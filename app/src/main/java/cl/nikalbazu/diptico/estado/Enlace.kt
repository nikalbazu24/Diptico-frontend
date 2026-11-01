package cl.nikalbazu.diptico.estado

import android.os.Handler
import android.os.Looper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

/**
 * Enlace con el teléfono: una pagina web chiquita que sirve la propia consola en tu Wi-Fi.
 * Escaneas el QR, en el teléfono sacas la clave (con un botón directo a la página oficial), la pegas ahí,
 * y llega sola a la consola. Solo funciona mientras el cuadro de la clave está abierto y con un código secreto.
 */
object Enlace {
    private var servidor: ServerSocket? = null
    private var hilo: Thread? = null
    private var codigo = ""
    private val principal = Handler(Looper.getMainLooper())

    /** IP de la consola en la red local (Wi-Fi), o null si no está conectada. */
    private fun ipLocal(): String? = try {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .sortedBy { if (it.name.startsWith("wlan")) 0 else 1 }
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }?.hostAddress
    } catch (_: Exception) {
        null
    }

    /**
     * Empieza a escuchar. [titulo]: qué clave es; [paginaOficial]: dónde se saca; [alRecibir] recibe la clave (en el hilo principal).
     * Devuelve la dirección para el QR, o null si no hay Wi-Fi.
     */
    fun iniciar(titulo: String, paginaOficial: String, alRecibir: (String) -> Unit): String? {
        detener()
        val ip = ipLocal() ?: return null
        val s = try { ServerSocket(0) } catch (_: Exception) { return null }
        servidor = s
        codigo = (1..6).map { "abcdefghjkmnpqrstuvwxyz23456789".random() }.joinToString("")
        hilo = Thread {
            while (!s.isClosed) {
                val c = try { s.accept() } catch (_: Exception) { break }
                try { atender(c, titulo, paginaOficial, alRecibir) } catch (_: Exception) {} finally { try { c.close() } catch (_: Exception) {} }
            }
        }.apply { isDaemon = true; start() }
        return "http://$ip:${s.localPort}/$codigo"
    }

    fun detener() {
        try { servidor?.close() } catch (_: Exception) {}
        servidor = null
        hilo = null
    }

    private fun atender(c: Socket, titulo: String, oficial: String, alRecibir: (String) -> Unit) {
        c.soTimeout = 15_000
        val lector = BufferedReader(InputStreamReader(c.getInputStream(), Charsets.UTF_8))
        val linea = lector.readLine() ?: return
        val (metodo, ruta) = linea.split(" ").let { (it.getOrNull(0) ?: "") to (it.getOrNull(1) ?: "") }
        var largo = 0
        while (true) {
            val h = lector.readLine() ?: break
            if (h.isEmpty()) break
            if (h.startsWith("Content-Length:", true)) largo = h.substringAfter(':').trim().toIntOrNull() ?: 0
        }
        if (ruta.trimStart('/').substringBefore('?') != codigo) {
            responder(c, 404, pagina(titulo, "<p>Este enlace ya no sirve. Vuelve a escanear el código en la consola.</p>"))
            return
        }
        if (metodo == "POST") {
            val cuerpo = CharArray(largo.coerceAtMost(4096)).let { buf -> var n = 0; while (n < buf.size) { val r = lector.read(buf, n, buf.size - n); if (r < 0) break; n += r }; String(buf, 0, n) }
            val clave = cuerpo.split("&").firstOrNull { it.startsWith("clave=") }?.substringAfter('=')?.let { URLDecoder.decode(it, "UTF-8") }?.trim().orEmpty()
            if (clave.length < 6) {
                responder(c, 200, pagina(titulo, formulario(oficial, Idioma.t("Esa clave parece incompleta. Cópiala de nuevo."))))
                return
            }
            principal.post { alRecibir(clave) }
            responder(c, 200, pagina(titulo, "<div class='ok'>✓</div><h2>${Idioma.t("¡Listo! La clave llegó a tu consola.")}</h2><p>${Idioma.t("Ya puedes cerrar esta página.")}</p>"))
            principal.postDelayed({ detener() }, 1500)
            return
        }
        responder(c, 200, pagina(titulo, formulario(oficial, null)))
    }

    private fun formulario(oficial: String, error: String?) = """
        <ol>
          <li>${Idioma.t("Toca el botón y crea tu clave en la página oficial (con tu cuenta).")}<br><a class='boton' href='$oficial' target='_blank'>${Idioma.t("Sacar mi clave")} ↗</a></li>
          <li>${Idioma.t("Copia la clave, vuelve a esta página y pégala aquí:")}</li>
        </ol>
        ${error?.let { "<p class='error'>$it</p>" } ?: ""}
        <form method='POST'><input name='clave' autocomplete='off' placeholder='${Idioma.t("Pega tu clave")}' autofocus>
        <button type='submit'>${Idioma.t("Enviar a mi consola")}</button></form>
        <p class='nota'>${Idioma.t("La clave va directo a tu consola por tu Wi-Fi. No pasa por ningún otro lado.")}</p>
    """.trimIndent()

    private fun pagina(titulo: String, contenido: String) = """
        <!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>
        <title>Díptico</title><style>
        body{margin:0;font-family:system-ui,sans-serif;background:#0f0d18;color:#f2f0ff;padding:24px}
        .tarjeta{max-width:460px;margin:0 auto;background:#1e1b2e;border-radius:22px;padding:22px}
        .marca{color:#ff7a59;font-weight:800;letter-spacing:2px;font-size:12px}
        h1{font-size:22px;margin:6px 0 14px}h2{text-align:center}
        ol{padding-left:20px;line-height:1.5}li{margin-bottom:14px}
        .boton,button{display:inline-block;margin-top:8px;background:#6b4eff;color:#fff;border:0;border-radius:999px;padding:12px 18px;font-weight:700;font-size:16px;text-decoration:none}
        input{width:100%;box-sizing:border-box;padding:14px;border-radius:14px;border:2px solid #3a3555;background:#0f0d18;color:#fff;font-size:16px;margin:6px 0 10px}
        button{width:100%}.nota{color:#b2aed0;font-size:13px}.error{color:#ff7a59}.ok{font-size:64px;text-align:center;color:#2ec4a6}
        </style></head><body><div class='tarjeta'><div class='marca'>DÍPTICO</div><h1>${Idioma.t(titulo)}</h1>$contenido</div></body></html>
    """.trimIndent()

    private fun responder(c: Socket, estado: Int, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        val cabecera = "HTTP/1.1 $estado OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\nCache-Control: no-store\r\n\r\n"
        c.getOutputStream().apply { write(cabecera.toByteArray()); write(bytes); flush() }
    }
}

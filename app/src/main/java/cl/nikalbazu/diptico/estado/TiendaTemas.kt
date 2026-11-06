package cl.nikalbazu.diptico.estado

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import org.json.JSONObject

data class TemaComunidad(
    val tema: Tema,
    val autor: String,
    val audio: String?,
    val categoria: String = "",
    val tituloPista: String = "",
    val pistaLocal: String? = null,
)

internal object TiendaTemas {
    const val WEB_APORTES = "https://diptico-temas.nickalbazu.chatgpt.site"
    private const val LIMITE_CATALOGO = 256 * 1024
    private const val LIMITE_AUDIO = 16 * 1024 * 1024
    private val adornosOficiales = mapOf(
        "chile" to "chupallas", "arcade" to "estrellas", "atardecer" to "petalos", "portatil" to "bloques",
        "cyber" to "fuegos", "menta" to "hojas", "monocromo" to "estrellas", "tinta" to "hojas",
        "brisamar" to "burbujas", "retro80" to "confeti", "ascua" to "fuegos",
    )
    private val nombreArchivo = Regex("com_[0-9a-f]{24}\\.(mp3|ogg|m4a|wav)")

    private fun url(texto: String): URL {
        val u = URL(texto.trim())
        require(u.protocol == "https" && u.host.isNotBlank() && u.userInfo == null) { "Usa un enlace HTTPS directo." }
        val partes = u.path.trim('/').split("/")
        return if (u.host.equals("github.com", true) && partes.size >= 5 && partes[2] == "blob") {
            URL("https://raw.githubusercontent.com/${partes.take(2).joinToString("/")}/${partes.drop(3).joinToString("/")}")
        } else u
    }

    private fun <T> leer(origen: URL, limite: Int, usar: (InputStream) -> T): T {
        var actual = origen
        repeat(5) {
            val c = actual.openConnection() as HttpsURLConnection
            try {
                c.instanceFollowRedirects = false
                c.connectTimeout = 8000
                c.readTimeout = 8000
                val codigo = c.responseCode
                if (codigo in setOf(301, 302, 303, 307, 308)) {
                    val destino = c.getHeaderField("Location") ?: error("El enlace está incompleto.")
                    actual = url(URL(actual, destino).toString())
                } else {
                    require(codigo == 200) { "El sitio respondió con HTTP $codigo." }
                    require(c.contentLength <= limite) { "El archivo es demasiado grande." }
                    return c.inputStream.use(usar)
                }
            } finally {
                c.disconnect()
            }
        }
        error("El enlace redirige demasiadas veces.")
    }

    private fun copiar(entrada: InputStream, salida: OutputStream, limite: Int) {
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val n = entrada.read(buffer)
            if (n < 0) break
            total += n
            require(total <= limite) { "El archivo es demasiado grande." }
            salida.write(buffer, 0, n)
        }
        require(total > 0) { "El archivo está vacío." }
    }

    fun incluidos(ctx: Context): List<TemaComunidad> = ctx.assets.open("tienda-temas.json").use {
        parsear(json(it), URL("$WEB_APORTES/catalogo.json"), local = true)
    }

    fun catalogo(enlace: String): List<TemaComunidad> {
        val origen = url(enlace)
        return leer(origen, LIMITE_CATALOGO) { parsear(json(it), origen) }
    }

    fun archivo(ctx: Context, uri: android.net.Uri): List<TemaComunidad> {
        require(uri.scheme == "content")
        val entrada = ctx.contentResolver.openInputStream(uri) ?: error("No pude abrir ese archivo.")
        return entrada.use { parsear(json(it), null) }
    }

    private fun json(entrada: InputStream): JSONObject = java.io.ByteArrayOutputStream().use { salida ->
        copiar(entrada, salida, LIMITE_CATALOGO)
        JSONObject(salida.toString("UTF-8"))
    }

    private fun parsear(json: JSONObject, origen: URL?, local: Boolean = false): List<TemaComunidad> {
        val lista = json.optJSONArray("temas")
        val entradas = if (lista == null) listOf(json) else {
            require(lista.length() in 1..50) { "El catálogo debe tener entre 1 y 50 temas." }
            (0 until lista.length()).mapNotNull(lista::optJSONObject)
        }
        return entradas.mapNotNull { o ->
            runCatching {
                val original = Tema.deJson(o)
                require(original.id.matches(Regex("[a-zA-Z0-9_-]{1,60}")) && original.nombre.isNotBlank())
                require(listOf(original.acento, original.acento2, original.acento3).all { it in 0xFF000000L..0xFFFFFFFFL })
                require(listOf(original.fondo, original.tinta).all { it == 0L || it in 0xFF000000L..0xFFFFFFFFL })
                val deDiptico = origen?.host == "diptico-temas.nickalbazu.chatgpt.site" &&
                    (origen.path.startsWith("/temas/") || origen.path == "/catalogo.json")
                val llave = (if (origen == null || deDiptico) o.toString() else "$origen|${original.id}").toByteArray(Charsets.UTF_8)
                val huella = java.security.MessageDigest.getInstance("SHA-256").digest(llave)
                    .take(12).joinToString("") { "%02x".format(it.toInt() and 255) }
                val t = original.copy(
                    id = "com_$huella", nombre = original.nombre.trim().take(36), propio = true, musica = "",
                    base = original.base.takeIf { it in setOf("auto", "claro", "oscuro", "oled", "gb") } ?: "auto",
                    decor = (if (deDiptico) adornosOficiales[original.id] else null)
                        ?: original.decor.takeIf { it in Temas.DECORACIONES } ?: "nada",
                    forma = original.forma.takeIf { it in Temas.FORMAS }.orEmpty(),
                    fuente = original.fuente.takeIf { it in Temas.FUENTES }.orEmpty(),
                    sonidos = original.sonidos.takeIf { it in setOf("bloques", "retro", "burbuja") }.orEmpty(),
                    mensaje = original.mensaje.take(80),
                    descripcion = original.descripcion.trim().take(320),
                    trama = original.trama.takeIf { it in Temas.TRAMAS }.orEmpty(),
                    acabado = original.acabado.takeIf { it in Temas.ACABADOS }.orEmpty(),
                    pistaBase = original.pistaBase.takeIf { id -> cl.nikalbazu.diptico.audio.Musica.pistas.any { it.id == id } }.orEmpty(),
                )
                val audio = o.optString("audio").trim().takeIf { it.isNotEmpty() }?.let { url(if (origen == null) it else URL(origen, it).toString()).toString() }
                TemaComunidad(t, o.optString("autor").trim().take(40), audio,
                    o.optString("categoria").trim().take(30), o.optString("tituloPista").trim().take(60),
                    if (local) "${original.id}.ogg" else null)
            }.getOrNull()
        }.distinctBy { it.tema.id }.also { require(it.isNotEmpty()) { "El enlace no contiene temas compatibles." } }
    }

    fun instalar(ctx: Context, entrada: TemaComunidad): Tema {
        val local = entrada.pistaLocal
        val origen = if (local == null) entrada.audio?.let(::url) ?: return entrada.tema else null
        val extension = origen?.path?.substringAfterLast('.')?.lowercase(java.util.Locale.ROOT) ?: "ogg"
        val nombre = "${entrada.tema.id}.$extension"
        require(nombre.matches(nombreArchivo)) { "La pista debe ser MP3, OGG, M4A o WAV." }
        val dir = File(ctx.filesDir, "temas-comunidad").apply { mkdirs() }
        val temporal = File.createTempFile("tema-", ".tmp", dir)
        try {
            temporal.outputStream().use { salida ->
                if (local != null) ctx.assets.open("temas-musica/$local").use { copiar(it, salida, LIMITE_AUDIO) }
                else leer(requireNotNull(origen), LIMITE_AUDIO) { copiar(it, salida, LIMITE_AUDIO) }
            }
            require(temporal.renameTo(File(dir, nombre))) { "No pude guardar la pista." }
        } finally {
            temporal.delete()
        }
        return entrada.tema.copy(musica = nombre)
    }

    fun pista(ctx: Context, tema: Tema): String? {
        if (!tema.musica.matches(nombreArchivo)) return null
        if (tema.id != "mio" && !tema.musica.startsWith("${tema.id}.")) return null
        return File(ctx.filesDir, "temas-comunidad/${tema.musica}").takeIf { it.isFile }?.let {
            "archivo:${android.net.Uri.fromFile(it)}"
        }
    }
}

package cl.nikalbazu.diptico.estado

import android.content.Context
import android.os.Build
import cl.nikalbazu.diptico.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reporte de fallas: si Díptico se cierra de golpe, guarda qué pasó; al abrirse de nuevo pregunta
 * si quieres compartirlo. El diagnóstico se depura antes de guardarlo o enviarlo.
 */
object Fallas {
    private const val ARCHIVO = "ultima-caida.txt"
    private const val ERRORES = "errores-recientes.txt"
    val configurado: Boolean get() = BuildConfig.REPORTES_URL.isNotBlank()

    data class Reporte(
        val detalle: String,
        val caida: Boolean,
        val nota: String = "",
        val id: String = UUID.randomUUID().toString(),
    )

    private val urlPrivada = Regex("""(?i)(?:https?|content|file)://[^\s<>"']+""")
    private val rutasLocales = Regex("""(?:/storage/|/sdcard/|/data/user/|/data/data/)[^\s<>"']+""")
    private val valorPrivado = Regex("""(?i)\b(?:authorization|api[_ -]?key|password|devpassword|token)\s*[:=]\s*(?:Bearer\s+)?[^\s,;&]+""")

    private fun depurar(texto: String): String = valorPrivado.replace(
        rutasLocales.replace(urlPrivada.replace(texto, "[enlace omitido]"), "[ruta omitida]"), "[credencial omitida]",
    )

    /** Se instala al arrancar la app: anota la caída y deja que Android cierre como siempre. */
    fun instalar(ctx: Context) {
        val app = ctx.applicationContext
        val previo = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { hilo, error ->
            try { File(app.filesDir, ARCHIVO).writeText(depurar(informe(app, "Díptico se cerró de golpe (hilo ${hilo.name})", error))) } catch (_: Throwable) {}
            previo?.uncaughtException(hilo, error)
        }
    }

    /** Un error que no cerró Díptico (una tarea de fondo que falló): se guardan los últimos para el reporte. */
    fun anotar(ctx: Context, error: Throwable) {
        try {
            val f = File(ctx.filesDir, ERRORES)
            val linea = "${hora()} · ${error.javaClass.simpleName}: ${error.message?.take(160) ?: ""} @ ${error.stackTrace.firstOrNull { it.className.startsWith("cl.nikalbazu") } ?: error.stackTrace.firstOrNull()}"
            val previas = if (f.exists()) f.readLines().takeLast(19) else emptyList()
            f.writeText(depurar((previas + linea).joinToString("\n")))
        } catch (_: Throwable) {}
    }

    /** El informe de la última caída, si hay uno sin mandar. */
    fun pendiente(ctx: Context): String? = try {
        File(ctx.filesDir, ARCHIVO).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
    } catch (_: Exception) { null }

    fun olvidar(ctx: Context) {
        try { File(ctx.filesDir, ARCHIVO).delete() } catch (_: Exception) {}
    }

    private fun hora(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

    private fun version(ctx: Context): String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
    } catch (_: Exception) { "?" }

    /** Versión, equipo, Android y el error, en texto simple. */
    fun informe(ctx: Context, titulo: String, error: Throwable?): String = buildString {
        appendLine(titulo)
        appendLine()
        appendLine("Versión: ${version(ctx)}${if (Actualizador.esMovil(ctx)) " (teléfono)" else ""}")
        appendLine("Equipo: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Hora: ${hora()}")
        if (error != null) {
            appendLine()
            val sw = StringWriter()
            error.printStackTrace(PrintWriter(sw))
            append(sw.toString().take(6000))
        }
        try {
            File(ctx.filesDir, ERRORES).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }?.let {
                appendLine()
                appendLine("Errores recientes que no cerraron Díptico:")
                append(it)
            }
        } catch (_: Exception) {}
    }

    fun textoReporte(reporte: Reporte): String = buildString {
        if (reporte.nota.isNotBlank()) {
            appendLine("Qué pasó:")
            appendLine(depurar(reporte.nota).take(1000))
            appendLine()
        }
        append(depurar(reporte.detalle).take(6000))
    }

    internal fun datosReporte(reporte: Reporte): JSONObject = JSONObject()
        .put("app", "diptico")
        .put("id", reporte.id)
        .put("tipo", if (reporte.caida) "caida" else "manual")
        .put("texto", textoReporte(reporte))

    /** Apps Script procesa el POST y devuelve el acuse desde un segundo host de Google. */
    fun enviar(reporte: Reporte): Boolean {
        if (!configurado) return false
        return try {
            val respuesta = solicitud(URL(BuildConfig.REPORTES_URL), datosReporte(reporte).toString())
            val acuse = JSONObject(respuesta)
            acuse.optBoolean("ok") && acuse.optString("id") == reporte.id
        } catch (_: Exception) {
            false
        }
    }

    private fun solicitud(url: URL, cuerpo: String? = null): String {
        val c = url.openConnection() as HttpURLConnection
        c.instanceFollowRedirects = false
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        return try {
            if (cuerpo != null) {
                val bytes = cuerpo.toByteArray(Charsets.UTF_8)
                c.requestMethod = "POST"
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                c.setFixedLengthStreamingMode(bytes.size)
                c.outputStream.use { it.write(bytes) }
            }
            when (c.responseCode) {
                HttpURLConnection.HTTP_OK -> c.inputStream.bufferedReader().use { it.readText() }
                HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_SEE_OTHER -> {
                    val destino = URL(c.getHeaderField("Location") ?: error("Falta el acuse"))
                    check(cuerpo != null && destino.protocol == "https" && destino.host == "script.googleusercontent.com" &&
                        destino.path == "/macros/echo" && destino.userInfo == null && destino.port in listOf(-1, 443))
                    solicitud(destino)
                }
                else -> error("No se pudo confirmar el reporte")
            }
        } finally {
            c.disconnect()
        }
    }
}

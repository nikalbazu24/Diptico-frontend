package cl.nikalbazu.diptico.estado

import android.content.Context
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reporte de fallas: si Díptico se cierra de golpe, guarda qué pasó; al abrirse de nuevo pregunta
 * si quieres mandarlo. El reporte llega como un aviso (issue) al repositorio de GitHub, donde
 * lo ven tanto quien hace Díptico como Claude. No lleva datos personales: versión, equipo y el error.
 */
object Fallas {
    private const val ARCHIVO = "ultima-caida.txt"
    private const val ERRORES = "errores-recientes.txt"
    const val REPO = "https://github.com/Nikalbazu24/diptico-frontend"

    /** Se instala al arrancar la app: anota la caída y deja que Android cierre como siempre. */
    fun instalar(ctx: Context) {
        val app = ctx.applicationContext
        val previo = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { hilo, error ->
            try { File(app.filesDir, ARCHIVO).writeText(informe(app, "Díptico se cerró de golpe (hilo ${hilo.name})", error)) } catch (_: Throwable) {}
            previo?.uncaughtException(hilo, error)
        }
    }

    /** Un error que no cerró Díptico (una tarea de fondo que falló): se guardan los últimos para el reporte. */
    fun anotar(ctx: Context, error: Throwable) {
        try {
            val f = File(ctx.filesDir, ERRORES)
            val linea = "${hora()} · ${error.javaClass.simpleName}: ${error.message?.take(160) ?: ""} @ ${error.stackTrace.firstOrNull { it.className.startsWith("cl.nikalbazu") } ?: error.stackTrace.firstOrNull()}"
            val previas = if (f.exists()) f.readLines().takeLast(19) else emptyList()
            f.writeText((previas + linea).joinToString("\n"))
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

    /** Un reporte nuevo en GitHub, ya escrito: solo falta tocar «Submit». */
    fun urlReporte(informe: String): String {
        // El título: la línea del error (por ejemplo «java.lang.NullPointerException: …»), o la primera del informe.
        val linea = informe.lineSequence().map { it.trim() }
            .firstOrNull { (it.contains("Exception") || it.contains("Error")) && !it.startsWith("at ") }
        val titulo = "Falla: " + (linea ?: informe.lineSequence().first()).take(90)
        // Las direcciones muy largas fallan: el texto va recortado (lo importante está al principio).
        val cuerpo = "**Qué estaba haciendo / What I was doing:**\n\n\n**Informe automático de Díptico:**\n```\n" + informe.take(3800) + "\n```"
        return "$REPO/issues/new?labels=bug&title=${Uri.encode(titulo)}&body=${Uri.encode(cuerpo)}"
    }

    /** Para reportar un problema sin caída (Menú › Acerca de). */
    fun urlProblema(ctx: Context): String = urlReporte(informe(ctx, "Reporte de un problema", null))
}

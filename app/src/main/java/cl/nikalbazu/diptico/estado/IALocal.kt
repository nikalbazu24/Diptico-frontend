package cl.nikalbazu.diptico.estado

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

/**
 * IA local: Bisa piensa dentro de la consola, sin internet ni créditos de nadie.
 * Usa un modelo de lenguaje chico (Qwen 2.5 de 1,5 mil millones de parámetros, libre y multilingüe)
 * que se baja una vez (unos 1,6 GB). Solo ocupa memoria mientras responde: después de un rato se descarga solo.
 */
object IALocal {
    const val NOMBRE_MODELO = "Qwen 2.5 · 1.5B"
    private const val ARCHIVO = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task"
    private const val URL_MODELO = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/$ARCHIVO"
    const val TAMANO = 1_625_493_432L

    /** Tokens que el modelo puede manejar entre pregunta y respuesta. */
    private const val MAX_TOKENS = 1280

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var motor: LlmInference? = null
    private var rutaCargada: String? = null
    private var liberar: Job? = null

    private fun carpeta(ctx: Context) = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "modelos").apply { mkdirs() }

    /** El modelo a usar: el bajado por Díptico, o uno propio que la persona eligió. */
    fun archivo(ctx: Context): File? {
        val dir = carpeta(ctx)
        for (n in listOf("propio-gemma.task", "propio.task")) {
            val propio = File(dir, n)
            if (propio.exists() && propio.length() > 1_000_000) return propio
        }
        val f = File(dir, ARCHIVO)
        // Se acepta si está completo según el tamaño que dio el servidor (o, si no se sabe, casi el esperado).
        val total = File(dir, "$ARCHIVO.total").takeIf { it.exists() }?.readText()?.trim()?.toLongOrNull() ?: TAMANO
        return if (f.exists() && f.length() >= total - 1024) f else null
    }

    fun listo(ctx: Context) = archivo(ctx) != null

    /** Lo descargado hasta ahora (para seguir donde quedó). */
    fun bajado(ctx: Context): Long = File(carpeta(ctx), "$ARCHIVO.parte").let { if (it.exists()) it.length() else 0L }

    /**
     * Baja el modelo (se puede cortar y seguir después). [avance] va de 0 a 1.
     * Devuelve null si salió bien, o el motivo si no.
     * Si el pedazo ya bajado no calza con el servidor (error 416), revisa si ya está completo o empieza de nuevo.
     */
    fun descargar(ctx: Context, reintento: Boolean = false, avance: (Float) -> Unit): String? {
        val dir = carpeta(ctx)
        val destino = File(dir, ARCHIVO)
        val parte = File(dir, "$ARCHIVO.parte")
        val marcaTotal = File(dir, "$ARCHIVO.total")
        val libre = dir.usableSpace
        if (libre < TAMANO - parte.length() + 200_000_000L) return "No hay espacio: se necesitan unos 1,8 GB libres."
        var desde = if (parte.exists()) parte.length() else 0L
        val c = URL(URL_MODELO).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 60_000
        c.instanceFollowRedirects = true
        if (desde > 0) c.setRequestProperty("Range", "bytes=$desde-")
        return try {
            val codigo = c.responseCode
            if (codigo == 416) {
                // "Rango imposible": lo bajado ya llega al final del archivo (o se pasó). Se revisa con el tamaño real.
                val total = c.getHeaderField("Content-Range")?.substringAfter('/')?.trim()?.toLongOrNull()
                c.disconnect()
                if (total != null && desde >= total - 1024 && desde <= total + 1024) {
                    marcaTotal.writeText(total.toString())
                    parte.renameTo(destino)
                    return null
                }
                parte.delete()
                return if (reintento) "El servidor del modelo no acepta la descarga ahora. Prueba en un rato." else descargar(ctx, true, avance)
            }
            if (codigo == 200) desde = 0L else if (codigo != 206) return "El servidor del modelo respondió con un error ($codigo)."
            // Tamaño real del archivo según el servidor.
            val total = when (codigo) {
                206 -> c.getHeaderField("Content-Range")?.substringAfter('/')?.trim()?.toLongOrNull()
                else -> c.contentLengthLong.takeIf { it > 0 }
            } ?: TAMANO
            marcaTotal.writeText(total.toString())
            c.inputStream.use { entrada ->
                java.io.FileOutputStream(parte, desde > 0).use { salida ->
                    val buf = ByteArray(256 * 1024)
                    var llevado = desde
                    var ultimo = 0L
                    while (true) {
                        val n = entrada.read(buf)
                        if (n < 0) break
                        salida.write(buf, 0, n)
                        llevado += n
                        if (llevado - ultimo > 8_000_000) { ultimo = llevado; avance(llevado / total.toFloat()) }
                    }
                }
            }
            if (parte.length() < total - 1024) return "La descarga quedó incompleta. Vuelve a intentarlo y sigue donde quedó."
            parte.renameTo(destino)
            null
        } catch (e: Exception) {
            "Se cortó la descarga. Vuelve a intentarlo y sigue donde quedó."
        } finally {
            c.disconnect()
        }
    }

    /** Copia un modelo .task elegido por la persona (por ejemplo Gemma). */
    fun usarPropio(ctx: Context, uri: android.net.Uri): String? = try {
        val nombre = uri.lastPathSegment.orEmpty()
        File(carpeta(ctx), "propio.task").delete(); File(carpeta(ctx), "propio-gemma.task").delete()
        val f = File(carpeta(ctx), if (nombre.contains("gemma", true)) "propio-gemma.task" else "propio.task")
        ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { i.copyTo(it, 1024 * 1024) } }
        if (f.length() < 1_000_000) { f.delete(); "Ese archivo no parece un modelo .task" } else { liberarAhora(); null }
    } catch (e: Exception) {
        "No pude copiar el modelo"
    }

    fun borrar(ctx: Context) {
        liberarAhora()
        carpeta(ctx).listFiles()?.forEach { it.delete() }
    }

    fun liberarAhora() {
        liberar?.cancel()
        try { motor?.close() } catch (_: Exception) {}
        motor = null
        rutaCargada = null
    }

    private fun cargar(ctx: Context): LlmInference {
        val f = archivo(ctx) ?: throw IllegalStateException("Primero baja la IA local en Menú › Bisa.")
        motor?.let { if (rutaCargada == f.absolutePath) return it }
        liberarAhora()
        val opciones = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(f.absolutePath)
            .setMaxTokens(MAX_TOKENS)
            .setMaxTopK(40)
            .build()
        val m = LlmInference.createFromOptions(ctx.applicationContext, opciones)
        motor = m
        rutaCargada = f.absolutePath
        return m
    }

    /** Arma la conversación con el formato que entiende el modelo (ChatML para Qwen, turnos para Gemma). */
    private fun formato(ctx: Context, sistema: String, mensajes: List<Pair<String, String>>): String {
        val gemma = archivo(ctx)?.name?.contains("gemma", true) == true
        return if (gemma) buildString {
            append("<start_of_turn>user\n").append(sistema).append("<end_of_turn>\n")
            for ((rol, t) in mensajes) append(if (rol == "user") "<start_of_turn>user\n" else "<start_of_turn>model\n").append(t).append("<end_of_turn>\n")
            append("<start_of_turn>model\n")
        } else buildString {
            append("<|im_start|>system\n").append(sistema).append("<|im_end|>\n")
            for ((rol, t) in mensajes) append("<|im_start|>").append(rol).append("\n").append(t).append("<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }
    }

    /**
     * Responde con el modelo local. [parcial] recibe el texto a medida que se escribe.
     * El historial se recorta para que todo quepa en lo que el modelo puede recordar.
     */
    suspend fun responder(ctx: Context, sistema: String, mensajes: List<Pair<String, String>>, parcial: (String) -> Unit): String {
        liberar?.cancel()
        val m = cargar(ctx)
        // ~3,5 letras por token: deja espacio para la respuesta (unos 350 tokens).
        val presupuesto = (MAX_TOKENS - 380) * 3
        var hist = mensajes.takeLast(6)
        var prompt = formato(ctx, sistema, hist)
        while (prompt.length > presupuesto && hist.size > 1) {
            hist = hist.drop(1)
            prompt = formato(ctx, sistema, hist)
        }
        if (prompt.length > presupuesto) prompt = formato(ctx, sistema.take(presupuesto / 2), hist.takeLast(1))
        val texto = StringBuilder()
        val r = suspendCancellableCoroutine { cont ->
            try {
                m.generateResponseAsync(prompt) { trozo, listo ->
                    if (trozo != null) { texto.append(trozo); parcial(texto.toString()) }
                    if (listo && cont.isActive) cont.resume(texto.toString())
                }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume("⚠ " + (e.message ?: "La IA local no pudo responder."))
            }
        }
        // Libera la memoria si no se usa en 3 minutos.
        liberar = alcance.launch { delay(180_000); liberarAhora() }
        return r.replace("<|im_end|>", "").replace("<end_of_turn>", "").trim()
    }
}

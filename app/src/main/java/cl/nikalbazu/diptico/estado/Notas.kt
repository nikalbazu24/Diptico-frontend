package cl.nikalbazu.diptico.estado

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** El mapa de un juego (una imagen tuya) y dónde quedaste marcado en él (de 0 a 1). */
data class MapaJuego(val uri: String, val x: Float = 0.5f, val y: Float = 0.5f)

/**
 * Notas de voz por juego: "¿dónde quedaste?" dicho en voz alta. Una nota por juego (la nueva reemplaza a la anterior),
 * guardada dentro de Díptico. Graba con el micrófono de la consola y se escucha desde la continuación.
 */
object Notas {
    const val MAX_MS = 60_000L

    private var grabadora: MediaRecorder? = null
    private var reproductor: MediaPlayer? = null
    private var grabandoClave: String? = null

    private val _sonando = MutableStateFlow<String?>(null)
    /** El juego cuya nota se está escuchando (o null). */
    val sonando: StateFlow<String?> = _sonando

    fun archivo(ctx: Context, clave: String): File =
        File(File(ctx.filesDir, "notas").apply { mkdirs() }, "${clave.hashCode().toUInt()}.m4a")

    fun existe(ctx: Context, clave: String): Boolean = archivo(ctx, clave).let { it.exists() && it.length() > 0 }

    fun puedeGrabar(ctx: Context): Boolean =
        ctx.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED

    /** Empieza a grabar (en un archivo aparte; solo reemplaza la nota anterior si se termina bien). */
    fun grabar(ctx: Context, clave: String): Boolean {
        detenerReproduccion()
        cancelar()
        val tmp = File(archivo(ctx, clave).path + ".tmp")
        return try {
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64_000)
            r.setAudioSamplingRate(44_100)
            r.setMaxDuration(MAX_MS.toInt())
            r.setOutputFile(tmp.absolutePath)
            r.prepare()
            r.start()
            grabadora = r
            grabandoClave = clave
            true
        } catch (_: Exception) {
            grabadora = null
            grabandoClave = null
            false
        }
    }

    /** Termina la grabación y la guarda como la nota del juego. */
    fun terminar(ctx: Context): Boolean {
        val r = grabadora ?: return false
        val clave = grabandoClave ?: return false
        grabadora = null
        grabandoClave = null
        val ok = try { r.stop(); true } catch (_: Exception) { false }
        try { r.release() } catch (_: Exception) {}
        val tmp = File(archivo(ctx, clave).path + ".tmp")
        if (!ok || !tmp.exists() || tmp.length() == 0L) { tmp.delete(); return false }
        val destino = archivo(ctx, clave)
        destino.delete()
        return tmp.renameTo(destino)
    }

    fun cancelar() {
        val r = grabadora ?: return
        grabadora = null
        grabandoClave = null
        try { r.stop() } catch (_: Exception) {}
        try { r.release() } catch (_: Exception) {}
    }

    fun reproducir(ctx: Context, clave: String) {
        if (_sonando.value == clave) { detenerReproduccion(); return }
        detenerReproduccion()
        val f = archivo(ctx, clave)
        if (!f.exists()) return
        try {
            // Pide el audio un momento: la música de Díptico baja mientras se escucha la nota.
            val am = ctx.getSystemService(android.media.AudioManager::class.java)
            val foco = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).build()
            am?.requestAudioFocus(foco)
            focoAm = am; focoNota = foco
            val m = MediaPlayer()
            m.setDataSource(f.absolutePath)
            m.setOnCompletionListener { detenerReproduccion() }
            m.prepare()
            m.start()
            reproductor = m
            _sonando.value = clave
        } catch (_: Exception) { detenerReproduccion() }
    }

    private var focoAm: android.media.AudioManager? = null
    private var focoNota: android.media.AudioFocusRequest? = null

    fun detenerReproduccion() {
        focoNota?.let { focoAm?.abandonAudioFocusRequest(it) }
        focoNota = null
        reproductor?.let { try { it.stop() } catch (_: Exception) {}; try { it.release() } catch (_: Exception) {} }
        reproductor = null
        _sonando.value = null
    }

    fun borrar(ctx: Context, clave: String) {
        if (_sonando.value == clave) detenerReproduccion()
        archivo(ctx, clave).delete()
    }
}

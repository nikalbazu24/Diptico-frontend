package cl.nikalbazu.diptico.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import cl.nikalbazu.diptico.R

/** Sonidos cortos al moverte, elegir, volver y abrir juegos. Se pueden apagar en Menú › Música. */
object Sonidos {
    private var pool: SoundPool? = null
    private val ids = HashMap<String, Int>()
    private var ultimoMover = 0L

    @Volatile
    var activo = true

    @Volatile
    var volumen = 0.6f

    fun iniciar(ctx: Context) {
        if (pool != null) return
        val p = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build()
        val app = ctx.applicationContext
        mapOf(
            "mover" to R.raw.sfx_mover, "consola" to R.raw.sfx_consola, "aceptar" to R.raw.sfx_aceptar,
            "atras" to R.raw.sfx_atras, "abrir" to R.raw.sfx_abrir, "jugar" to R.raw.sfx_jugar,
        ).forEach { (k, r) -> ids[k] = p.load(app, r, 1) }
        pool = p
    }

    private fun tocar(nombre: String, tono: Float = 1f) {
        if (!activo) return
        val id = ids[nombre] ?: return
        pool?.play(id, volumen, volumen, 1, 0, tono)
    }

    fun mover() {
        // Si mantienes la cruceta no se amontonan.
        val ahora = SystemClock.uptimeMillis()
        if (ahora - ultimoMover < 45) return
        ultimoMover = ahora
        tocar("mover", 0.96f + (ahora % 7) * 0.012f)
    }

    fun consola() = tocar("consola")
    fun aceptar() = tocar("aceptar")
    fun atras() = tocar("atras")
    fun abrir() = tocar("abrir")
    fun jugar() = tocar("jugar")
}

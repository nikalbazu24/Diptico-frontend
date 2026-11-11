package cl.nikalbazu.diptico.audio

import cl.nikalbazu.diptico.estado.*

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.InputDevice

/**
 * Vibración al moverse y elegir, con cuatro niveles: apagada, suave, media y fuerte.
 * Usa el motor de la consola o del teléfono; si no tiene, prueba con el del mando conectado.
 * Pulsos muy cortos para que acompañen sin cansar, y nunca mientras se juega.
 */
object Vibracion {
    val NIVELES = listOf("apagada", "suave", "media", "fuerte")
    fun nombre(n: String) = when (n) { "suave" -> "Suave"; "media" -> "Media"; "fuerte" -> "Fuerte"; else -> "Apagada" }

    @Volatile var nivel = "apagada"
    private var app: Context? = null
    private var ultimo = 0L

    fun iniciar(ctx: Context, nivelGuardado: String) {
        app = ctx.applicationContext
        nivel = nivelGuardado.takeIf { it in NIVELES } ?: "apagada"
    }

    private var cache: Vibrator? = null
    private var cacheEn = 0L

    /** El motor se busca como mucho cada 10 segundos (por si se conecta o desconecta un mando). */
    private fun motor(): Vibrator? {
        val ahora = SystemClock.uptimeMillis()
        if (cacheEn != 0L && ahora - cacheEn < 10_000) return cache
        cacheEn = ahora
        cache = buscarMotor()
        return cache
    }

    private fun buscarMotor(): Vibrator? {
        val ctx = app ?: return null
        val propio = try {
            if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
            else @Suppress("DEPRECATION") ctx.getSystemService(Vibrator::class.java)
        } catch (_: Exception) { null }
        if (propio?.hasVibrator() == true) return propio
        // Sin motor propio: el primer mando conectado que vibre.
        return try {
            InputDevice.getDeviceIds().asSequence().mapNotNull { InputDevice.getDevice(it) }
                .filter { it.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD }
                .mapNotNull { d ->
                    if (Build.VERSION.SDK_INT >= 31) d.vibratorManager.defaultVibrator else @Suppress("DEPRECATION") d.vibrator
                }.firstOrNull { it.hasVibrator() }
        } catch (_: Exception) { null }
    }

    /** [peso]: 1 = moverse, 2 = elegir, 3 = llegar al borde o un aviso. */
    private fun pulso(peso: Int) {
        if (nivel == "apagada" || cl.nikalbazu.diptico.estado.Diptico.estado.value.sesion != null) return
        val ahora = SystemClock.uptimeMillis()
        if (peso == 1 && ahora - ultimo < 60) return
        ultimo = ahora
        val (ms, fuerza) = when (nivel) {
            "suave" -> 8L to 50
            "fuerte" -> 22L to 230
            else -> 14L to 130
        }
        val v = motor() ?: return
        try {
            val amplitud = if (v.hasAmplitudeControl()) (fuerza * (0.6f + 0.2f * peso)).toInt().coerceIn(1, 255) else VibrationEffect.DEFAULT_AMPLITUDE
            v.vibrate(VibrationEffect.createOneShot(ms + (peso - 1) * 6L, amplitud))
        } catch (_: Exception) {}
    }

    fun mover() = pulso(1)
    fun elegir() = pulso(2)
    fun borde() = pulso(3)

    /** Para probar el nivel recién elegido. */
    fun probar() { ultimo = 0L; pulso(2) }
}

package cl.nikalbazu.diptico.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * La voz de DIPPO: cuando le hablas, te responde en voz alta con la voz del sistema (un poquito más aguda,
 * para que suene a pulpito). Si la consola no tiene voz instalada, simplemente no habla.
 */
object VozDippo {
    private var tts: TextToSpeech? = null
    private var listo = false
    private var pendiente: Pair<String, String>? = null

    fun decir(ctx: Context, texto: String, idioma: String) {
        val limpio = texto.replace(Regex("[*_#⚠•]"), "").replace(Regex("\\s+"), " ").trim().take(700)
        if (limpio.isEmpty()) return
        val voz = tts
        if (voz == null) {
            pendiente = limpio to idioma
            tts = try {
                TextToSpeech(ctx.applicationContext) { estado ->
                    listo = estado == TextToSpeech.SUCCESS
                    if (listo) pendiente?.let { (t, i) -> hablar(t, i) }
                    pendiente = null
                }
            } catch (_: Exception) { null }
            return
        }
        if (listo) hablar(limpio, idioma) else pendiente = limpio to idioma
    }

    private fun hablar(texto: String, idioma: String) {
        val voz = tts ?: return
        try {
            voz.language = when (idioma) {
                "en" -> Locale.US
                "pt" -> Locale.forLanguageTag("pt-BR")
                "zh" -> Locale.SIMPLIFIED_CHINESE
                "ja" -> Locale.JAPAN
                else -> Locale.forLanguageTag("es-419")
            }
            voz.setPitch(1.25f)
            voz.setSpeechRate(1.05f)
            voz.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "dippo")
        } catch (_: Exception) {}
    }

    fun callar() {
        try { tts?.stop() } catch (_: Exception) {}
    }
}

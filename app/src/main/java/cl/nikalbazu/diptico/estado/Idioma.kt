package cl.nikalbazu.diptico.estado

import android.content.Context
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.util.Locale

/**
 * Idiomas de Díptico. Todo está escrito en español; al mostrar un texto se busca su traducción
 * en assets/idiomas/<código>.json. Los textos con datos ("Listo: encontré 5 de 8 juegos") se traducen
 * con plantillas ("Listo: encontré {0} de {1} juegos").
 */
object Idioma {
    val IDIOMAS = listOf(
        "es" to "Español",
        "en" to "English",
        "pt" to "Português",
        "zh" to "中文（简体）",
        "ja" to "日本語",
    )

    /** Código del idioma en uso. Es estado de Compose: al cambiarlo, toda la pantalla se vuelve a dibujar. */
    var actual by mutableStateOf("es")
        private set

    val locale: Locale
        get() = when (actual) {
            "en" -> Locale.US
            "pt" -> Locale("pt", "BR")
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "ja" -> Locale.JAPANESE
            else -> Locale("es", "CL")
        }

    private var exactos: Map<String, String> = emptyMap()
    private var plantillas: List<Plantilla> = emptyList()
    private val cache = LruCache<String, String>(1500)

    private class Plantilla(val origen: String, val destino: String, val regex: Regex, val fijo: String)

    fun fijar(ctx: Context, codigo: String) {
        val cod = IDIOMAS.firstOrNull { it.first == codigo }?.first ?: "es"
        cache.evictAll()
        if (cod == "es") {
            exactos = emptyMap(); plantillas = emptyList()
        } else {
            try {
                val o = JSONObject(ctx.assets.open("idiomas/$cod.json").bufferedReader().use { it.readText() })
                val ex = HashMap<String, String>()
                val pl = ArrayList<Plantilla>()
                for (k in o.keys()) {
                    val v = o.getString(k)
                    if (Regex("""\{\d+\}""").containsMatchIn(k)) {
                        // "Encontré {0} de {1}" -> ^Encontré (.+?) de (.+?)$
                        val partes = k.split(Regex("""\{\d+\}"""))
                        val patron = partes.joinToString("(.+?)") { Regex.escape(it) }
                        val fijo = partes.maxByOrNull { it.length }.orEmpty()
                        pl += Plantilla(k, v, Regex("^$patron$", RegexOption.DOT_MATCHES_ALL), fijo)
                    } else {
                        ex[k] = v
                    }
                }
                exactos = ex
                // Las plantillas con más texto fijo primero (son más específicas).
                plantillas = pl.sortedByDescending { it.fijo.length }
            } catch (e: Exception) {
                exactos = emptyMap(); plantillas = emptyList()
            }
        }
        actual = cod
    }

    /** Traduce un texto ya armado. Si no hay traducción, lo devuelve igual. */
    fun t(texto: String): String {
        if (actual == "es" || texto.isBlank()) return texto
        cache.get(texto)?.let { return it }
        val r = traducir(texto)
        cache.put(texto, r)
        return r
    }

    private fun traducir(texto: String): String {
        exactos[texto]?.let { return it }
        val limpio = texto.trim()
        if (limpio != texto) exactos[limpio]?.let { return texto.replace(limpio, it) }
        for (p in plantillas) {
            if (p.fijo.isNotEmpty() && !texto.contains(p.fijo)) continue
            val m = p.regex.matchEntire(texto) ?: continue
            var salida = p.destino
            // Los números de las plantillas se respetan: {1} puede ir antes que {0} en otro idioma.
            val marcas = Regex("""\{(\d+)\}""").findAll(p.origen).map { it.groupValues[1].toInt() }.toList()
            m.groupValues.drop(1).forEachIndexed { i, v ->
                val n = marcas.getOrElse(i) { i }
                salida = salida.replace("{$n}", t(v))
            }
            return salida
        }
        // "Automática (alta)": se traduce lo de afuera y lo de adentro del paréntesis.
        Regex("""^(.+) \((.+)\)$""").matchEntire(texto)?.let { m ->
            val afuera = t(m.groupValues[1])
            val adentro = m.groupValues[2].let { x ->
                val directo = t(x)
                if (directo != x) directo
                else t(x.replaceFirstChar { it.uppercase() }).let { y -> if (y == x.replaceFirstChar { it.uppercase() }) x else y.lowercase(locale) }
            }
            if (afuera != m.groupValues[1] || adentro != m.groupValues[2]) return "$afuera ($adentro)"
        }
        // Un símbolo adelante ("⚠ ", "♪ ", "▶ "): se traduce el resto.
        Regex("""^([^\p{L}\p{N}"“¿¡(]+\s)(.+)$""", RegexOption.DOT_MATCHES_ALL).matchEntire(texto)?.let { m ->
            val resto = t(m.groupValues[2])
            if (resto != m.groupValues[2]) return m.groupValues[1] + resto
        }
        // Textos armados con " · " o saltos de línea: se traduce cada parte.
        for (sep in listOf("\n", " · ", " — ", ": ", " › ")) {
            if (texto.contains(sep)) {
                val partes = texto.split(sep)
                val trad = partes.map { t(it) }
                if (trad != partes) return trad.joinToString(sep)
            }
        }
        return texto
    }

    /** Fecha con el formato natural del idioma. [esqueleto] como "EEEEdMMMM" o "EEEdMMM". */
    fun fecha(esqueleto: String, d: java.util.Date = java.util.Date()): String = formato(esqueleto).format(d)

    fun formato(esqueleto: String): java.text.SimpleDateFormat {
        val patron = try { android.text.format.DateFormat.getBestDateTimePattern(locale, esqueleto) } catch (e: Exception) { esqueleto }
        return java.text.SimpleDateFormat(patron, locale)
    }

    /** Cómo se le pide a la IA que responda. */
    fun nombreParaIA(codigo: String) = when (codigo) {
        "en" -> "English"
        "pt" -> "Brazilian Portuguese"
        "zh" -> "Simplified Chinese"
        "ja" -> "Japanese"
        else -> "español de Chile"
    }

    fun ayuda(codigo: String) = when (codigo) {
        "en" -> "Use Díptico in English."
        "pt" -> "Usar o Díptico em português."
        "zh" -> "以简体中文使用 Díptico。"
        "ja" -> "Díptico を日本語で使います。"
        else -> "Usar Díptico en español."
    }

    /** Idioma de Android, para elegirlo solo la primera vez. */
    fun delSistema(): String = when (Locale.getDefault().language) {
        "en" -> "en"; "pt" -> "pt"; "zh" -> "zh"; "ja" -> "ja"; else -> "es"
    }
}

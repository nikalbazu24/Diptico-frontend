package cl.nikalbazu.diptico.estado

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * DIPPO busca en internet (sin claves): resultados de DuckDuckGo y artículos de Wikipedia.
 * Lo usa la IA de la consola, que no sabe buscar sola; las IA en la nube (Gemini, ChatGPT) buscan con su propia herramienta.
 */
object Web {
    data class Resultado(val titulo: String, val texto: String, val fuente: String)

    private fun leer(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) Diptico")
        c.setRequestProperty("Accept-Language", "es,en;q=0.8")
        return try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }

    private fun limpiar(html: String): String =
        html.replace(Regex("<[^>]+>"), "").replace("&quot;", "\"").replace("&#x27;", "'").replace("&#39;", "'")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ").replace(Regex("\\s+"), " ").trim()

    /** Resultados de DuckDuckGo (la versión liviana en HTML): título y resumen de cada página. */
    private fun duck(consulta: String): List<Resultado> = try {
        val html = leer("https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(consulta, "UTF-8") + "&kl=es-es")
        val titulos = Regex("""class="result__a"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL).findAll(html).map { limpiar(it.groupValues[1]) }.toList()
        val textos = Regex("""class="result__snippet"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL).findAll(html).map { limpiar(it.groupValues[1]) }.toList()
        val sitios = Regex("""class="result__url"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL).findAll(html).map { limpiar(it.groupValues[1]) }.toList()
        titulos.indices.mapNotNull { i ->
            val t = textos.getOrNull(i) ?: return@mapNotNull null
            Resultado(titulos[i], t, sitios.getOrNull(i) ?: "web")
        }.take(5)
    } catch (_: Exception) { emptyList() }

    /** El comienzo del artículo de Wikipedia que mejor calza (en el idioma de la persona). */
    private fun wikipedia(consulta: String, idioma: String): List<Resultado> = try {
        val lang = if (idioma in setOf("es", "en", "pt", "ja", "zh")) idioma else "es"
        val b = JSONObject(leer("https://$lang.wikipedia.org/w/api.php?action=query&list=search&format=json&srlimit=2&srsearch=" + URLEncoder.encode(consulta, "UTF-8")))
        val l = b.getJSONObject("query").getJSONArray("search")
        (0 until l.length()).mapNotNull { i ->
            val titulo = l.getJSONObject(i).getString("title")
            val r = JSONObject(leer("https://$lang.wikipedia.org/api/rest_v1/page/summary/" + URLEncoder.encode(titulo.replace(' ', '_'), "UTF-8")))
            r.optString("extract").takeIf { it.isNotBlank() }?.let { Resultado(titulo, it.take(600), "Wikipedia") }
        }
    } catch (_: Exception) { emptyList() }

    /** Busca y arma un resumen corto para dárselo a la IA (vacío si no hay internet). */
    fun buscar(consulta: String, idioma: String): List<Resultado> {
        val q = consulta.take(160)
        return (duck(q) + wikipedia(q, idioma)).distinctBy { it.titulo }.take(6)
    }

    fun comoContexto(r: List<Resultado>): String =
        if (r.isEmpty()) "" else "Información recién buscada en internet (úsala si sirve y menciona la fuente en pocas palabras):\n" +
            r.joinToString("\n") { "- ${it.titulo} (${it.fuente}): ${it.texto.take(400)}" }

    /** ¿La pregunta necesita datos de internet? (fechas, precios, novedades, guías concretas, "busca"…) */
    fun conviene(pregunta: String): Boolean {
        val t = pregunta.lowercase()
        val claves = listOf(
            "busca", "buscar", "internet", "google", "último", "ultimo", "nuevo", "nueva", "cuándo", "cuando sale", "precio", "cuesta",
            "lanzamiento", "noticia", "202", "dónde está", "donde esta", "cómo paso", "como paso", "cómo consigo", "como consigo",
            "guía", "guia", "walkthrough", "jefe", "boss", "mapa", "ubicación", "ubicacion", "qué es", "que es", "quién", "quien",
            "emulador", "bios", "configurar", "versión", "version",
        )
        return claves.any { it in t } || t.endsWith("?") && t.length > 25
    }
}

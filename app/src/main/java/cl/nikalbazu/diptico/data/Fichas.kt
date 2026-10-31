package cl.nikalbazu.diptico.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

/** Historia y datos de un juego, sacados de Wikipedia. */
data class FichaInfo(
    val titulo: String,
    val resumen: String,
    val historia: String,
    val juego: String,
    val recepcion: String,
    val enlace: String,
    val idioma: String,
) {
    val vacia: Boolean get() = resumen.isBlank() && historia.isBlank()
}

/**
 * Busca la ficha de un juego en Wikipedia (primero en español, si no en inglés).
 * Guarda lo encontrado para no pedirlo otra vez.
 */
object Fichas {
    private const val AGENTE = "Diptico/0.3 (launcher para AYN Thor; contacto en GitHub Nikalbazu24)"

    private fun texto(url: String): String? {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 12_000
        c.readTimeout = 20_000
        c.setRequestProperty("User-Agent", AGENTE)
        return try {
            if (c.responseCode !in 200..299) null else c.inputStream.use { it.bufferedReader().readText() }
        } catch (e: Exception) {
            null
        } finally {
            c.disconnect()
        }
    }

    private fun cod(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun hash(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)

    fun buscar(ctx: Context, titulo: String, consola: String): FichaInfo? {
        val cache = File(File(ctx.filesDir, "fichas").apply { mkdirs() }, hash("$titulo|$consola") + ".json")
        if (cache.exists()) {
            leer(cache.readText())?.let { return it }
        }
        val info = buscarEn("es", titulo, consola, "videojuego") ?: buscarEn("en", titulo, consola, "video game")
        if (info != null) cache.writeText(guardar(info))
        return info
    }

    private fun buscarEn(idioma: String, titulo: String, consola: String, palabra: String): FichaInfo? {
        val base = "https://$idioma.wikipedia.org/w/api.php"
        val busqueda = texto("$base?action=query&list=search&format=json&srlimit=3&srsearch=${cod("$titulo $palabra")}") ?: return null
        val resultados = try { JSONObject(busqueda).getJSONObject("query").getJSONArray("search") } catch (e: Exception) { return null }
        if (resultados.length() == 0) return null
        // Prefiere el resultado cuyo título se parece más al juego.
        val clave = titulo.lowercase().filter { it.isLetterOrDigit() }.take(12)
        var pagina = resultados.getJSONObject(0).getString("title")
        for (i in 0 until resultados.length()) {
            val t = resultados.getJSONObject(i).getString("title")
            if (t.lowercase().filter { it.isLetterOrDigit() }.startsWith(clave)) { pagina = t; break }
        }
        val json = texto("$base?action=query&prop=extracts&explaintext=1&exsectionformat=wiki&redirects=1&format=json&titles=${cod(pagina)}") ?: return null
        val paginas = try { JSONObject(json).getJSONObject("query").getJSONObject("pages") } catch (e: Exception) { return null }
        val p = paginas.getJSONObject(paginas.keys().next())
        val extracto = p.optString("extract", "")
        if (extracto.isBlank()) return null
        val secciones = separar(extracto)
        fun buscarSec(vararg nombres: String) = secciones.entries
            .filter { (k, _) -> nombres.any { k.contains(it, true) } }
            .joinToString("\n\n") { it.value.trim() }
            .trim()
        return FichaInfo(
            titulo = p.optString("title", pagina),
            resumen = secciones[""]?.trim().orEmpty().take(1600),
            historia = buscarSec("desarrollo", "development", "producción", "production", "historia", "history").take(3500),
            juego = buscarSec("jugabilidad", "sistema de juego", "gameplay", "argumento", "trama", "plot", "sinopsis").take(3000),
            recepcion = buscarSec("recepción", "reception", "legado", "legacy", "ventas", "sales").take(2000),
            enlace = "https://$idioma.wikipedia.org/wiki/${cod(pagina.replace(' ', '_'))}",
            idioma = idioma,
        )
    }

    /** Separa el texto por secciones ("== Desarrollo =="). La introducción queda con clave vacía. */
    private fun separar(t: String): Map<String, String> {
        val salida = LinkedHashMap<String, StringBuilder>()
        var actual = ""
        salida[actual] = StringBuilder()
        for (linea in t.lines()) {
            val m = Regex("^==+\\s*(.+?)\\s*==+$").find(linea.trim())
            if (m != null) {
                // Las subsecciones se suman a su sección principal.
                if (linea.trim().startsWith("===")) continue
                actual = m.groupValues[1]
                salida.getOrPut(actual) { StringBuilder() }
            } else if (linea.isNotBlank()) {
                salida[actual]!!.append(linea.trim()).append("\n")
            }
        }
        return salida.mapValues { it.value.toString() }
    }

    private fun guardar(f: FichaInfo) = JSONObject()
        .put("titulo", f.titulo).put("resumen", f.resumen).put("historia", f.historia).put("juego", f.juego)
        .put("recepcion", f.recepcion).put("enlace", f.enlace).put("idioma", f.idioma).toString()

    private fun leer(s: String): FichaInfo? = try {
        val o = JSONObject(s)
        FichaInfo(
            o.getString("titulo"), o.getString("resumen"), o.getString("historia"), o.getString("juego"),
            o.getString("recepcion"), o.getString("enlace"), o.getString("idioma"),
        )
    } catch (e: Exception) {
        null
    }

    /** Búsquedas de guías y trucos en internet, para abrir en el navegador. */
    fun enlacesGuias(titulo: String, consola: String): List<Pair<String, String>> {
        val q = cod(titulo)
        return listOf(
            "Guías en YouTube" to "https://www.youtube.com/results?search_query=$q+gu%C3%ADa+espa%C3%B1ol",
            "Guía completa (Google)" to "https://www.google.com/search?q=$q+gu%C3%ADa+completa",
            "Trucos y secretos" to "https://www.google.com/search?q=$q+${cod(consola)}+trucos+secretos",
            "Wiki de fans" to "https://www.google.com/search?q=$q+wiki+fandom",
            "GameFAQs" to "https://gamefaqs.gamespot.com/search?game=$q",
            "Logros y 100%" to "https://www.google.com/search?q=$q+100%25+completo+gu%C3%ADa",
        )
    }
}

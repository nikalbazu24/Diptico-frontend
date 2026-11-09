package cl.nikalbazu.diptico.estado

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cuánto dura cada juego, según HowLongToBeat: historia principal, con extras y al 100 %.
 * HowLongToBeat no tiene una conexión oficial para apps: se pregunta como lo hace su web (la misma forma que usa la
 * comunidad). Si su web cambia y eso falla, se busca el dato en internet como respaldo.
 */
object CuantoDura {
    /** Horas (0 = sin dato) y cuándo se buscó. */
    data class Duracion(val principal: Float, val extras: Float, val completo: Float, val buscado: Long) {
        val tiene get() = principal > 0f || extras > 0f || completo > 0f
    }

    private const val WEB = "https://howlongtobeat.com/"
    private const val AGENTE = "Mozilla/5.0 (Linux; Android 13; Thor) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    private fun abrir(url: String, metodo: String = "GET", cuerpo: String? = null, cabeceras: Map<String, String> = emptyMap()): String? {
        val c = URL(url).openConnection() as HttpURLConnection
        return try {
            c.connectTimeout = 7000
            c.readTimeout = 9000
            c.requestMethod = metodo
            c.setRequestProperty("User-Agent", AGENTE)
            c.setRequestProperty("Referer", WEB)
            c.setRequestProperty("Origin", WEB.trimEnd('/'))
            cabeceras.forEach { (k, v) -> c.setRequestProperty(k, v) }
            if (cuerpo != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(cuerpo.toByteArray()) }
            }
            if (c.responseCode !in 200..299) null else c.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) { null } finally { c.disconnect() }
    }

    /** La dirección de búsqueda que usa hoy la web (cambia de vez en cuando): está escrita en sus scripts. */
    @Volatile private var ruta: String? = null

    private fun rutaBusqueda(): String {
        ruta?.let { return it }
        val html = abrir(WEB) ?: return "api/s"
        val scripts = Regex("""<script[^>]+src="([^"]*/_next/static/chunks/[^"]+\.js)"""").findAll(html).map { it.groupValues[1] }.toList()
        val patron = Regex("""fetch\s*\(\s*["']/api/([a-zA-Z0-9_/]+)[^"']*["']\s*,\s*\{[^}]*method:\s*["']POST["']""", RegexOption.IGNORE_CASE)
        for (s in scripts.take(25)) {
            val js = abrir(if (s.startsWith("http")) s else WEB + s.trimStart('/')) ?: continue
            patron.find(js)?.let { m -> return ("api/" + m.groupValues[1].trimEnd('/')).also { ruta = it } }
        }
        return "api/s"
    }

    private fun normal(s: String) = Palabras.normal(s).replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

    /** Qué tanto se parecen dos nombres (0 a 1), por palabras en común. */
    private fun parecido(a: String, b: String): Float {
        val x = normal(a).split(' ').filter { it.isNotEmpty() }.toSet()
        val y = normal(b).split(' ').filter { it.isNotEmpty() }.toSet()
        if (x.isEmpty() || y.isEmpty()) return 0f
        return (x intersect y).size * 2f / (x.size + y.size)
    }

    /** Busca en HowLongToBeat directamente. */
    private fun desdeHltb(titulo: String): Duracion? {
        val r = rutaBusqueda()
        val init = abrir(WEB + r + "/init?t=" + System.currentTimeMillis())?.let { runCatching { JSONObject(it) }.getOrNull() }
        val token = init?.optString("token").orEmpty()
        var clave: String? = null
        var valor: String? = null
        init?.keys()?.forEach { k ->
            val l = k.lowercase()
            if ("key" in l) clave = init.optString(k) else if ("val" in l) valor = init.optString(k)
        }
        val cuerpo = JSONObject().apply {
            put("searchType", "games")
            put("searchTerms", JSONArray(titulo.split(' ').filter { it.isNotBlank() }))
            put("searchPage", 1)
            put("size", 10)
            put("searchOptions", JSONObject().apply {
                put("games", JSONObject().apply {
                    put("userId", 0); put("platform", ""); put("sortCategory", "popular"); put("rangeCategory", "main")
                    put("rangeTime", JSONObject().put("min", 0).put("max", 0))
                    put("gameplay", JSONObject().put("perspective", "").put("flow", "").put("genre", "").put("difficulty", ""))
                    put("rangeYear", JSONObject().put("max", "").put("min", ""))
                    put("modifier", "hide_dlc")
                })
                put("users", JSONObject().put("sortCategory", "postcount"))
                put("lists", JSONObject().put("sortCategory", "follows"))
                put("filter", ""); put("sort", 0); put("randomizer", 0)
            })
            put("useCache", true)
            val k = clave; val v = valor
            if (k != null && v != null) put(k, v)
        }.toString()
        val cab = buildMap {
            put("Accept", "*/*")
            if (token.isNotEmpty()) put("x-auth-token", token)
            clave?.let { put("x-hp-key", it) }
            valor?.let { put("x-hp-val", it) }
        }
        val resp = abrir(WEB + r, "POST", cuerpo, cab) ?: return null
        val datos = runCatching { JSONObject(resp).getJSONArray("data") }.getOrNull() ?: return null
        val mejor = (0 until datos.length()).map { datos.getJSONObject(it) }
            .maxByOrNull { parecido(titulo, it.optString("game_name")) } ?: return null
        if (parecido(titulo, mejor.optString("game_name")) < 0.5f) return null
        fun h(k: String) = (mejor.optLong(k, 0L) / 3600f)
        return Duracion(h("comp_main"), h("comp_plus"), h("comp_100"), System.currentTimeMillis()).takeIf { it.tiene }
    }

    /** Respaldo: busca «howlongtobeat <juego>» y lee las horas del resumen («Main Story 12½ Hours…»). */
    private fun desdeBusqueda(titulo: String): Duracion? {
        val html = abrir("https://html.duckduckgo.com/html/?q=" + URLEncoder.encode("howlongtobeat $titulo", "UTF-8")) ?: return null
        val texto = html.replace(Regex("<[^>]+>"), " ").replace("&frac12;", "½").replace("&#189;", "½")
        fun horas(etiqueta: String): Float {
            val m = Regex("""$etiqueta\D{0,12}?(\d+)(½|\.5)?\s*(Hours|Hrs|Horas|h)""", RegexOption.IGNORE_CASE).find(texto) ?: return 0f
            return m.groupValues[1].toFloat() + if (m.groupValues[2].isNotEmpty()) 0.5f else 0f
        }
        return Duracion(horas("Main Story"), horas("Main \\+ (Sides|Extras)"), horas("Completionist"), System.currentTimeMillis()).takeIf { it.tiene }
    }

    /** Busca cuánto dura [titulo] (sin internet o sin resultado: null). Se llama fuera del hilo principal. */
    fun buscar(titulo: String): Duracion? {
        val limpio = titulo.replace(Regex("\\(.*?\\)|\\[.*?]"), " ").replace(Regex("\\s+"), " ").trim()
        return desdeHltb(limpio) ?: desdeBusqueda(limpio)
    }

    fun horasBonitas(h: Float): String = when {
        h <= 0f -> "?"
        h < 1f -> "${(h * 60).toInt()} min"
        h % 1f >= 0.25f && h % 1f < 0.75f -> "${h.toInt()}½ h"
        else -> "${Math.round(h)} h"
    }

    /** «Te quedan ~8 h» según lo que llevas (en segundos) y lo que dura. */
    fun teQueda(d: Duracion, jugadoSeg: Long): String? {
        if (!d.tiene) return null
        val llevas = jugadoSeg / 3600f
        return when {
            d.principal > 0f && llevas < d.principal * 0.97f -> "Te quedan ~${horasBonitas(d.principal - llevas)} de historia"
            d.extras > 0f && llevas < d.extras -> "Historia lista · con extras ~${horasBonitas(d.extras - llevas)} más"
            d.completo > 0f && llevas < d.completo -> "Para el 100 %: ~${horasBonitas(d.completo - llevas)}"
            else -> "Ya pasaste lo que dura (${horasBonitas(maxOf(d.principal, d.extras, d.completo))})"
        }
    }

    fun leer(s: String): Map<String, Duracion> = s.split('\n').mapNotNull { l ->
        val p = l.split('|')
        if (p.size < 5) return@mapNotNull null
        p[0] to Duracion(p[1].toFloatOrNull() ?: 0f, p[2].toFloatOrNull() ?: 0f, p[3].toFloatOrNull() ?: 0f, p[4].toLongOrNull() ?: 0L)
    }.toMap()

    fun escribir(m: Map<String, Duracion>) = m.entries.joinToString("\n") { (k, d) -> "${k.replace("\n", " ").replace("|", "/")}|${d.principal}|${d.extras}|${d.completo}|${d.buscado}" }
}

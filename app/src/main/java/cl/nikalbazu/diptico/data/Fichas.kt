package cl.nikalbazu.diptico.data

import cl.nikalbazu.diptico.estado.*

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
    /** Imágenes del artículo (miniaturas grandes), para el carrusel. */
    val imagenes: List<ImagenWeb> = emptyList(),
    /** La imagen principal del artículo (casi siempre la portada). */
    val principal: ImagenWeb? = null,
) {
    val vacia: Boolean get() = resumen.isBlank() && historia.isBlank()
}

/** Una imagen de internet: [url] completa, [miniatura] para mostrar y de dónde viene. */
data class ImagenWeb(val url: String, val miniatura: String, val fuente: String, val nombre: String = "", val ancho: Int = 0, val alto: Int = 0)

/**
 * Busca la ficha de un juego en Wikipedia (primero en español, si no en inglés).
 * Guarda lo encontrado para no pedirlo otra vez.
 */
object Fichas {
    const val AGENTE = "Diptico/0.8 (launcher para AYN Thor; contacto en GitHub Nikalbazu24)"

    fun texto(url: String): String? {
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

    /** Palabra que se suma a la búsqueda para encontrar el artículo del juego, por idioma. */
    private fun palabra(idioma: String) = when (idioma) {
        "en" -> "video game"; "pt" -> "jogo eletrônico"; "zh" -> "电子游戏"; "ja" -> "ゲーム"; else -> "videojuego"
    }

    /** La ficha ya guardada en la consola (sin internet), o null si todavía no se buscó. */
    fun guardada(ctx: Context, titulo: String, consola: String, idioma: String = cl.nikalbazu.diptico.estado.Idioma.actual): FichaInfo? {
        val cache = File(File(ctx.filesDir, "fichas"), hash("v2|$idioma|$titulo|$consola") + ".json")
        return if (cache.exists()) try { leer(cache.readText()) } catch (_: Exception) { null } else null
    }

    fun buscar(ctx: Context, titulo: String, consola: String, idioma: String = cl.nikalbazu.diptico.estado.Idioma.actual): FichaInfo? {
        val cache = File(File(ctx.filesDir, "fichas").apply { mkdirs() }, hash("v2|$idioma|$titulo|$consola") + ".json")
        if (cache.exists()) {
            leer(cache.readText())?.let { return it }
        }
        // Primero en tu idioma; si no está, en inglés (y en español como último recurso).
        val orden = listOf(idioma, "en", "es").distinct()
        var info: FichaInfo? = null
        for (l in orden) {
            info = buscarEn(l, titulo, consola, palabra(l))
            if (info != null) break
        }
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
        val titReal = p.optString("title", pagina)
        val (imagenes, principal) = try { imagenesDe(idioma, titReal) } catch (e: Exception) { emptyList<ImagenWeb>() to null }
        return FichaInfo(
            imagenes = imagenes,
            principal = principal,
            titulo = titReal,
            resumen = secciones[""]?.trim().orEmpty().take(1600),
            historia = buscarSec(
                "desarrollo", "development", "producción", "production", "historia", "history",
                "desenvolvimento", "produção", "história", "开发", "制作", "历史", "開発", "制作", "歴史",
            ).take(3500),
            juego = buscarSec(
                "jugabilidad", "sistema de juego", "gameplay", "argumento", "trama", "plot", "sinopsis",
                "jogabilidade", "enredo", "玩法", "剧情", "故事", "ゲームシステム", "システム", "ストーリー", "あらすじ",
            ).take(3000),
            recepcion = buscarSec(
                "recepción", "reception", "legado", "legacy", "ventas", "sales",
                "recepção", "vendas", "评价", "反响", "销量", "評価", "反響", "売上", "販売",
            ).take(2000),
            enlace = "https://$idioma.wikipedia.org/wiki/${cod(pagina.replace(' ', '_'))}",
            idioma = idioma,
        )
    }

    /** Imágenes del artículo: fotos y capturas (sin íconos ni banderas) y la imagen principal. */
    fun imagenesDe(idioma: String, pagina: String): Pair<List<ImagenWeb>, ImagenWeb?> {
        val base = "https://$idioma.wikipedia.org/w/api.php"
        val json = texto(
            "$base?action=query&format=json&redirects=1&titles=${cod(pagina)}&generator=images&gimlimit=40" +
                "&prop=imageinfo&iiprop=url%7Cmime%7Csize&iiurlwidth=1280",
        )
        val lista = ArrayList<ImagenWeb>()
        if (json != null) {
            val paginas = JSONObject(json).optJSONObject("query")?.optJSONObject("pages")
            if (paginas != null) for (k in paginas.keys()) {
                val o = paginas.getJSONObject(k)
                val nombre = o.optString("title").substringAfter(':')
                val info = o.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                val mime = info.optString("mime")
                if (mime !in setOf("image/jpeg", "image/png", "image/webp")) continue
                if (info.optInt("width") < 240 || info.optInt("height") < 160) continue
                val n = nombre.lowercase()
                if (listOf("flag", "bandera", "commons-logo", "symbol", "icon", "question", "wiki", "edit-", "padlock", "portal", "stub").any { it in n }) continue
                val url = info.optString("url").takeIf { it.isNotEmpty() } ?: continue
                lista += ImagenWeb(url, info.optString("thumburl").ifEmpty { url }, "Wikipedia", nombre)
            }
        }
        // La imagen principal (la del cuadro de la derecha).
        val pj = texto("$base?action=query&format=json&redirects=1&titles=${cod(pagina)}&prop=pageimages&piprop=original%7Cthumbnail&pithumbsize=900")
        val principal = try {
            val ps = JSONObject(pj ?: "").getJSONObject("query").getJSONObject("pages")
            val o = ps.getJSONObject(ps.keys().next())
            val orig = o.optJSONObject("original")?.optString("source")
            val mini = o.optJSONObject("thumbnail")?.optString("source")
            if (orig.isNullOrEmpty() || orig.endsWith(".svg", true)) null else ImagenWeb(orig, mini ?: orig, "Wikipedia", "principal")
        } catch (e: Exception) {
            null
        }
        val ordenadas = lista.sortedBy { if (it.url == principal?.url) 0 else 1 }.take(16)
        return ordenadas to principal
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

    private fun imagenJson(i: ImagenWeb) = JSONObject().put("url", i.url).put("mini", i.miniatura).put("fuente", i.fuente).put("nombre", i.nombre)
    private fun imagenDe(o: JSONObject) = ImagenWeb(o.getString("url"), o.optString("mini", o.getString("url")), o.optString("fuente", "Wikipedia"), o.optString("nombre"))

    private fun guardar(f: FichaInfo) = JSONObject()
        .put("titulo", f.titulo).put("resumen", f.resumen).put("historia", f.historia).put("juego", f.juego)
        .put("recepcion", f.recepcion).put("enlace", f.enlace).put("idioma", f.idioma)
        .put("imagenes", org.json.JSONArray().apply { f.imagenes.forEach { put(imagenJson(it)) } })
        .apply { f.principal?.let { put("principal", imagenJson(it)) } }
        .toString()

    private fun leer(s: String): FichaInfo? = try {
        val o = JSONObject(s)
        val imgs = o.optJSONArray("imagenes")
        FichaInfo(
            o.getString("titulo"), o.getString("resumen"), o.getString("historia"), o.getString("juego"),
            o.getString("recepcion"), o.getString("enlace"), o.getString("idioma"),
            imagenes = if (imgs == null) emptyList() else (0 until imgs.length()).map { imagenDe(imgs.getJSONObject(it)) },
            principal = o.optJSONObject("principal")?.let { imagenDe(it) },
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

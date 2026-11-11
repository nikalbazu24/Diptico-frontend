package cl.nikalbazu.diptico.data

import cl.nikalbazu.diptico.estado.*

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Más fuentes de imágenes (además de libretro, SteamGridDB y Wikipedia): IGDB, ScreenScraper, RAWG y TheGamesDB.
 * Cada una pide su propia cuenta o clave gratis; Díptico solo usa las que la persona conectó.
 */
object Raspadores {
    data class Claves(
        val igdbId: String = "", val igdbSecreto: String = "",
        val ssUsuario: String = "", val ssClave: String = "", val ssDevId: String = "", val ssDevClave: String = "",
        val rawg: String = "", val tgdb: String = "",
    ) {
        val igdb: Boolean get() = igdbId.isNotBlank() && igdbSecreto.isNotBlank()
        val screenscraper: Boolean get() = ssDevId.isNotBlank() && ssDevClave.isNotBlank()
        val conRawg: Boolean get() = rawg.isNotBlank()
        val conTgdb: Boolean get() = tgdb.isNotBlank()
        val alguna: Boolean get() = igdb || screenscraper || conRawg || conTgdb
    }

    /** Lo encontrado en todas las fuentes para un juego. */
    data class Hallazgo(val portadas: List<ImagenWeb>, val artes: List<ImagenWeb>, val logos: List<ImagenWeb>, val resumen: String? = null)

    private fun pedir(url: String, metodo: String = "GET", cuerpo: String? = null, cabeceras: Map<String, String> = emptyMap()): String? {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        c.requestMethod = metodo
        c.setRequestProperty("User-Agent", Fichas.AGENTE)
        cabeceras.forEach { (k, v) -> c.setRequestProperty(k, v) }
        return try {
            if (cuerpo != null) { c.doOutput = true; c.outputStream.use { it.write(cuerpo.toByteArray()) } }
            if (c.responseCode !in 200..299) null else c.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) { null } finally { c.disconnect() }
    }

    private fun cod(s: String) = URLEncoder.encode(s, "UTF-8")

    // ---------- IGDB (con una app gratis de Twitch) ----------

    private var tokenIgdb: String? = null
    private var venceIgdb = 0L

    private fun token(c: Claves): String? {
        if (tokenIgdb != null && System.currentTimeMillis() < venceIgdb) return tokenIgdb
        val r = pedir("https://id.twitch.tv/oauth2/token?client_id=${cod(c.igdbId.trim())}&client_secret=${cod(c.igdbSecreto.trim())}&grant_type=client_credentials", "POST", "") ?: return null
        return try {
            val o = JSONObject(r)
            tokenIgdb = o.getString("access_token")
            venceIgdb = System.currentTimeMillis() + (o.optLong("expires_in", 3600) - 120) * 1000
            tokenIgdb
        } catch (_: Exception) { null }
    }

    /** Plataformas de IGDB por consola de Díptico. */
    private val PLATAFORMA_IGDB = mapOf(
        "snes" to 19, "sfc" to 19, "nes" to 18, "famicom" to 18, "n64" to 4, "gc" to 21, "wii" to 5, "wiiu" to 41, "switch" to 130,
        "gb" to 33, "gbc" to 22, "gba" to 24, "nds" to 20, "n3ds" to 37, "psx" to 7, "ps2" to 8, "ps3" to 9, "ps4" to 48, "psp" to 38,
        "psvita" to 46, "genesis" to 29, "megadrive" to 29, "mastersystem" to 64, "gamegear" to 35, "saturn" to 32, "dreamcast" to 23,
        "segacd" to 78, "sega32x" to 30, "pcengine" to 86, "tg16" to 86, "neogeo" to 80, "arcade" to 52, "mame" to 52, "xbox" to 11,
        "xbox360" to 12, "atari2600" to 59, "virtualboy" to 87, "ngp" to 119, "ngpc" to 120, "wonderswan" to 57, "wonderswancolor" to 123,
    )

    private fun imgIgdb(id: String, tam: String) = "https://images.igdb.com/igdb/image/upload/$tam/$id.jpg"

    fun igdb(c: Claves, titulo: String, consola: String): Hallazgo? {
        val t = token(c) ?: return null
        val cab = mapOf("Client-ID" to c.igdbId.trim(), "Authorization" to "Bearer $t", "Accept" to "application/json")
        val campos = "fields name,summary,cover.image_id,artworks.image_id,screenshots.image_id;"
        val buscado = titulo.replace("\"", "")
        val plataforma = PLATAFORMA_IGDB[consola]
        val r = (plataforma?.let { pedir("https://api.igdb.com/v4/games", "POST", "search \"$buscado\"; $campos where platforms = ($it); limit 3;", cab) }
            ?.takeIf { it.trim() != "[]" } ?: pedir("https://api.igdb.com/v4/games", "POST", "search \"$buscado\"; $campos limit 3;", cab)) ?: return null
        return try {
            val a = JSONArray(r)
            if (a.length() == 0) return null
            val portadas = ArrayList<ImagenWeb>(); val artes = ArrayList<ImagenWeb>()
            var resumen: String? = null
            for (i in 0 until a.length()) {
                val g = a.getJSONObject(i)
                if (resumen == null) resumen = g.optString("summary").takeIf { it.isNotBlank() }
                g.optJSONObject("cover")?.optString("image_id")?.takeIf { it.isNotBlank() }?.let { portadas += ImagenWeb(imgIgdb(it, "t_cover_big_2x"), imgIgdb(it, "t_cover_big"), "IGDB", g.optString("name"), 528, 748) }
                for (clave in listOf("artworks", "screenshots")) g.optJSONArray(clave)?.let { l ->
                    for (k in 0 until minOf(l.length(), 6)) l.getJSONObject(k).optString("image_id").takeIf { it.isNotBlank() }?.let {
                        artes += ImagenWeb(imgIgdb(it, "t_1080p"), imgIgdb(it, "t_screenshot_med"), "IGDB", if (clave == "artworks") "Arte" else "Captura", 1920, 1080)
                    }
                }
            }
            Hallazgo(portadas, artes, emptyList(), resumen)
        } catch (_: Exception) { null }
    }

    // ---------- ScreenScraper (cuenta de screenscraper.fr + credenciales de desarrollador) ----------

    private val SISTEMA_SS = mapOf(
        "megadrive" to 1, "genesis" to 1, "mastersystem" to 2, "nes" to 3, "famicom" to 3, "snes" to 4, "sfc" to 4, "gb" to 9, "gbc" to 10,
        "gba" to 12, "gc" to 13, "n64" to 14, "nds" to 15, "wii" to 16, "n3ds" to 17, "wiiu" to 18, "sega32x" to 19, "segacd" to 20,
        "gamegear" to 21, "saturn" to 22, "dreamcast" to 23, "atari2600" to 26, "pcengine" to 31, "tg16" to 31, "xbox" to 32, "xbox360" to 33,
        "psx" to 57, "ps2" to 58, "ps3" to 59, "psp" to 61, "psvita" to 62, "mame" to 75, "arcade" to 75, "neogeo" to 142, "switch" to 225,
        "ngp" to 25, "ngpc" to 82, "wonderswan" to 45, "wonderswancolor" to 46, "virtualboy" to 11, "atari7800" to 41, "atarilynx" to 28,
    )

    fun screenscraper(c: Claves, titulo: String, archivo: String, consola: String): Hallazgo? {
        val base = "devid=${cod(c.ssDevId.trim())}&devpassword=${cod(c.ssDevClave.trim())}&softname=Diptico&output=json" +
            (if (c.ssUsuario.isNotBlank()) "&ssid=${cod(c.ssUsuario.trim())}&sspassword=${cod(c.ssClave.trim())}" else "")
        val sistema = SISTEMA_SS[consola]?.let { "&systemeid=$it" } ?: ""
        // Primero por el nombre del archivo (lo más exacto); si no, buscando por el título.
        val jeu = pedir("https://api.screenscraper.fr/api2/jeuInfos.php?$base$sistema&romtype=rom&romnom=${cod(archivo)}")
            ?.let { try { JSONObject(it).getJSONObject("response").getJSONObject("jeu") } catch (_: Exception) { null } }
            ?: pedir("https://api.screenscraper.fr/api2/jeuRecherche.php?$base$sistema&recherche=${cod(titulo)}")
                ?.let { try { JSONObject(it).getJSONObject("response").getJSONArray("jeux").optJSONObject(0) } catch (_: Exception) { null } }
            ?: return null
        val medias = jeu.optJSONArray("medias") ?: return null
        val orden = listOf("wor", "us", "ss", "eu", "jp")
        fun de(tipos: Set<String>, nombre: String): List<ImagenWeb> = (0 until medias.length()).map { medias.getJSONObject(it) }
            .filter { it.optString("type") in tipos && it.optString("url").isNotBlank() }
            .sortedBy { orden.indexOf(it.optString("region")).let { p -> if (p < 0) 9 else p } }
            .map { ImagenWeb(it.getString("url"), it.getString("url"), "ScreenScraper", "$nombre ${it.optString("region").uppercase()}".trim()) }
            .take(6)
        val resumen = jeu.optJSONArray("synopsis")?.let { l -> (0 until l.length()).map { l.getJSONObject(it) }.firstOrNull { it.optString("langue") == "es" }?.optString("text") }
        return Hallazgo(de(setOf("box-2D"), "Caja"), de(setOf("fanart", "ss", "sstitle"), "Arte"), de(setOf("wheel-hd", "wheel"), "Logo"), resumen)
    }

    // ---------- RAWG (clave gratis en rawg.io/apidocs) ----------

    fun rawg(c: Claves, titulo: String): Hallazgo? {
        val r = pedir("https://api.rawg.io/api/games?key=${cod(c.rawg.trim())}&search=${cod(titulo)}&page_size=3") ?: return null
        return try {
            val l = JSONObject(r).getJSONArray("results")
            val artes = ArrayList<ImagenWeb>()
            for (i in 0 until l.length()) {
                val g = l.getJSONObject(i)
                g.optString("background_image").takeIf { it.isNotBlank() && it != "null" }?.let { artes += ImagenWeb(it, it, "RAWG", g.optString("name")) }
                g.optJSONArray("short_screenshots")?.let { s -> for (k in 0 until minOf(s.length(), 5)) s.getJSONObject(k).optString("image").takeIf { it.isNotBlank() }?.let { artes += ImagenWeb(it, it, "RAWG", "Captura") } }
            }
            Hallazgo(emptyList(), artes.distinctBy { it.url }, emptyList())
        } catch (_: Exception) { null }
    }

    // ---------- TheGamesDB (clave pedida en su foro) ----------

    fun tgdb(c: Claves, titulo: String): Hallazgo? {
        val k = cod(c.tgdb.trim())
        val r = pedir("https://api.thegamesdb.net/v1.1/Games/ByGameName?apikey=$k&name=${cod(titulo)}&include=boxart") ?: return null
        return try {
            val o = JSONObject(r)
            val juegos = o.getJSONObject("data").getJSONArray("games")
            if (juegos.length() == 0) return null
            val id = juegos.getJSONObject(0).getInt("id")
            val portadas = ArrayList<ImagenWeb>()
            o.optJSONObject("include")?.optJSONObject("boxart")?.let { b ->
                val base = b.getJSONObject("base_url").getString("original")
                b.optJSONObject("data")?.optJSONArray("$id")?.let { l ->
                    for (i in 0 until l.length()) l.getJSONObject(i).takeIf { it.optString("side") == "front" }?.let { portadas += ImagenWeb(base + it.getString("filename"), base + it.getString("filename"), "TheGamesDB", "Caja") }
                }
            }
            val artes = ArrayList<ImagenWeb>(); val logos = ArrayList<ImagenWeb>()
            pedir("https://api.thegamesdb.net/v1/Games/Images?apikey=$k&games_id=$id&filter%5Btype%5D=fanart,clearlogo,screenshot")?.let { s ->
                val d = JSONObject(s).getJSONObject("data")
                val base = d.getJSONObject("base_url").getString("original")
                d.getJSONObject("images").optJSONArray("$id")?.let { l ->
                    for (i in 0 until l.length()) {
                        val im = l.getJSONObject(i)
                        val u = base + im.getString("filename")
                        if (im.optString("type") == "clearlogo") logos += ImagenWeb(u, u, "TheGamesDB", "Logo") else artes += ImagenWeb(u, u, "TheGamesDB", if (im.optString("type") == "fanart") "Arte" else "Captura")
                    }
                }
            }
            Hallazgo(portadas, artes, logos)
        } catch (_: Exception) { null }
    }

    /** El reverso y el lomo reales de la caja (enlaces), si alguna fuente conectada los tiene. */
    data class Caras(val reverso: String?, val lomo: String?, val frente: String? = null)

    /** Plataformas de TheGamesDB por consola de Díptico (para no traer la caja de otra consola). */
    private val PLATAFORMA_TGDB = mapOf(
        "nes" to 7, "famicom" to 7, "snes" to 6, "sfc" to 6, "n64" to 3, "gc" to 2, "wii" to 9, "wiiu" to 38, "switch" to 4971,
        "gb" to 4, "gbc" to 41, "gba" to 5, "nds" to 8, "n3ds" to 4912, "virtualboy" to 4918,
        "psx" to 10, "ps2" to 11, "ps3" to 12, "ps4" to 4919, "psp" to 13, "psvita" to 39,
        "genesis" to 18, "megadrive" to 36, "mastersystem" to 35, "gamegear" to 20, "saturn" to 17, "dreamcast" to 16,
        "segacd" to 21, "sega32x" to 33, "pcengine" to 34, "tg16" to 34, "neogeo" to 24, "xbox" to 14, "xbox360" to 15,
        "atari2600" to 22, "atari7800" to 27, "atarilynx" to 4924, "ngp" to 4922, "ngpc" to 4923, "wonderswan" to 4925, "wonderswancolor" to 4926,
    )

    /**
     * Busca la caja completa: ScreenScraper trae reverso («box-2D-back») y lomo («box-2D-side»); TheGamesDB, el
     * reverso («back»). Si no hay ninguna fuente conectada o no aparece, Díptico arma el reverso con la ficha del juego.
     */
    fun caras(c: Claves, titulo: String, archivo: String, consola: String): Caras {
        var reverso: String? = null
        var lomo: String? = null
        var frente: String? = null
        if (c.screenscraper) try {
            val base = "devid=${cod(c.ssDevId.trim())}&devpassword=${cod(c.ssDevClave.trim())}&softname=Diptico&output=json" +
                (if (c.ssUsuario.isNotBlank()) "&ssid=${cod(c.ssUsuario.trim())}&sspassword=${cod(c.ssClave.trim())}" else "")
            val sistema = SISTEMA_SS[consola]?.let { "&systemeid=$it" } ?: ""
            val jeu = pedir("https://api.screenscraper.fr/api2/jeuInfos.php?$base$sistema&romtype=rom&romnom=${cod(archivo)}")
                ?.let { try { JSONObject(it).getJSONObject("response").getJSONObject("jeu") } catch (_: Exception) { null } }
                ?: pedir("https://api.screenscraper.fr/api2/jeuRecherche.php?$base$sistema&recherche=${cod(titulo)}")
                    ?.let { try { JSONObject(it).getJSONObject("response").getJSONArray("jeux").optJSONObject(0) } catch (_: Exception) { null } }
            jeu?.optJSONArray("medias")?.let { medias ->
                val orden = listOf("wor", "us", "ss", "eu", "jp")
                fun una(tipo: String) = (0 until medias.length()).map { medias.getJSONObject(it) }
                    .filter { it.optString("type") == tipo && it.optString("url").isNotBlank() }
                    .minByOrNull { orden.indexOf(it.optString("region")).let { p -> if (p < 0) 9 else p } }?.optString("url")
                reverso = una("box-2D-back")
                lomo = una("box-2D-side")
                frente = una("box-2D")
            }
        } catch (_: Exception) {}
        if ((reverso == null || frente == null) && c.conTgdb) try {
            val k = cod(c.tgdb.trim())
            // Primero en su consola (así no aparece la caja de otra versión); si no hay, en todas.
            val plataforma = PLATAFORMA_TGDB[consola]
            val r = (plataforma?.let { pedir("https://api.thegamesdb.net/v1.1/Games/ByGameName?apikey=$k&name=${cod(titulo)}&filter%5Bplatform%5D=$it&include=boxart") }
                ?.takeIf { try { JSONObject(it).getJSONObject("data").getJSONArray("games").length() > 0 } catch (_: Exception) { false } })
                ?: pedir("https://api.thegamesdb.net/v1.1/Games/ByGameName?apikey=$k&name=${cod(titulo)}&include=boxart")
            r?.let {
                val o = JSONObject(it)
                val juegos = o.getJSONObject("data").getJSONArray("games")
                if (juegos.length() > 0) {
                    val id = juegos.getJSONObject(0).getInt("id")
                    o.optJSONObject("include")?.optJSONObject("boxart")?.let { b ->
                        val urls = b.getJSONObject("base_url")
                        val base = urls.optString("large").ifBlank { urls.getString("original") }
                        b.optJSONObject("data")?.optJSONArray("$id")?.let { l ->
                            for (i in 0 until l.length()) {
                                val im = l.getJSONObject(i)
                                when (im.optString("side")) {
                                    "back" -> if (reverso == null) reverso = base + im.getString("filename")
                                    "front" -> if (frente == null) frente = base + im.getString("filename")
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return Caras(reverso, lomo, frente)
    }

    /** Busca en todas las fuentes conectadas. */
    fun buscar(c: Claves, titulo: String, archivo: String, consola: String): Hallazgo {
        val todos = listOfNotNull(
            if (c.screenscraper) screenscraper(c, titulo, archivo, consola) else null,
            if (c.igdb) igdb(c, titulo, consola) else null,
            if (c.conTgdb) tgdb(c, titulo) else null,
            if (c.conRawg) rawg(c, titulo) else null,
        )
        return Hallazgo(todos.flatMap { it.portadas }, todos.flatMap { it.artes }, todos.flatMap { it.logos }, todos.firstNotNullOfOrNull { it.resumen })
    }

    /** Prueba si las claves de una fuente funcionan. */
    fun probar(c: Claves, fuente: String): Boolean = when (fuente) {
        "igdb" -> token(c) != null
        "rawg" -> pedir("https://api.rawg.io/api/games?key=${cod(c.rawg.trim())}&page_size=1") != null
        "tgdb" -> pedir("https://api.thegamesdb.net/v1/Platforms?apikey=${cod(c.tgdb.trim())}") != null
        "ss" -> screenscraper(c, "Super Mario World", "Super Mario World (USA).sfc", "snes") != null
        else -> false
    }
}

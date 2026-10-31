package cl.nikalbazu.diptico.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Un logro de RetroAchievements. */
data class Logro(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val puntos: Int,
    val insignia: String,
    /** Fecha en que lo ganaste ("" si todavía no). */
    val ganado: String,
    val dificil: Boolean,
) {
    val conseguido: Boolean get() = ganado.isNotEmpty()
    val imagen: String get() = "${Logros.MEDIOS}/Badge/$insignia${if (conseguido) "" else "_lock"}.png"
}

/** Los logros de un juego y cuánto llevas. */
data class LogrosJuego(
    val idJuego: Int,
    val titulo: String,
    val icono: String,
    val logros: List<Logro>,
) {
    val total: Int get() = logros.size
    val ganados: Int get() = logros.count { it.conseguido }
    val puntos: Int get() = logros.sumOf { it.puntos }
    val puntosGanados: Int get() = logros.filter { it.conseguido }.sumOf { it.puntos }
}

/** Tu perfil de RetroAchievements. */
data class PerfilRA(
    val usuario: String,
    val puntos: Int,
    val puntosReales: Int,
    val foto: String,
    val desde: String,
    val lema: String,
    val recientes: List<Pair<String, Logro>> = emptyList(),
)

/**
 * RetroAchievements (retroachievements.org): logros para juegos retro.
 * Díptico solo los muestra; los logros se ganan jugando en RetroArch con tu sesión iniciada allá.
 * Se usa tu usuario y tu "Web API Key" (en retroachievements.org › Settings).
 */
object Logros {
    const val API = "https://retroachievements.org/API"
    const val MEDIOS = "https://media.retroachievements.org"

    /** Consola de Díptico (nombres de ES-DE) -> consola de RetroAchievements. */
    private val CONSOLAS = mapOf(
        "genesis" to 1, "megadrive" to 1, "megadrivejp" to 1,
        "n64" to 2, "snes" to 3, "sfc" to 3, "snesna" to 3, "satellaview" to 3, "sufami" to 3,
        "gb" to 4, "sgb" to 4, "gba" to 5, "gbc" to 6, "nes" to 7, "famicom" to 7, "fds" to 7,
        "pcengine" to 8, "tg16" to 8, "supergrafx" to 8, "segacd" to 9, "megacd" to 9, "megacdjp" to 9,
        "sega32x" to 10, "sega32xjp" to 10, "sega32xna" to 10, "mastersystem" to 11, "mark3" to 11,
        "psx" to 12, "atarilynx" to 13, "ngp" to 14, "ngpc" to 14, "gamegear" to 15, "gc" to 16,
        "atarijaguar" to 17, "nds" to 18, "wii" to 19, "ps2" to 21, "odyssey2" to 23, "videopac" to 23,
        "pokemini" to 24, "atari2600" to 25, "arcade" to 27, "fbneo" to 27, "mame" to 27, "neogeo" to 27, "cps1" to 27, "cps2" to 27, "cps3" to 27,
        "virtualboy" to 28, "msx" to 29, "msx1" to 29, "msx2" to 29, "sg-1000" to 33, "amstradcpc" to 37, "apple2" to 38,
        "saturn" to 39, "saturnjp" to 39, "dreamcast" to 40, "psp" to 41, "3do" to 43, "colecovision" to 44,
        "intellivision" to 45, "vectrex" to 46, "pc88" to 47, "pcfx" to 49, "atari7800" to 51,
        "wonderswan" to 53, "wonderswancolor" to 53, "neogeocd" to 56, "neogeocdjp" to 56, "channelf" to 57,
        "supervision" to 63, "megaduck" to 69, "arduboy" to 71, "wasm4" to 72, "arcadia" to 73,
        "pcenginecd" to 76, "tg-cd" to 76, "atarijaguarcd" to 77, "uzebox" to 80,
    )

    fun idConsola(consola: String): Int? = CONSOLAS[consola]
    fun tieneLogros(consola: String): Boolean = consola in CONSOLAS

    private fun cod(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun pedir(ruta: String): String {
        val c = URL("$API/$ruta").openConnection() as HttpURLConnection
        c.connectTimeout = 12_000
        c.readTimeout = 25_000
        c.setRequestProperty("User-Agent", Fichas.AGENTE)
        try {
            val codigo = c.responseCode
            if (codigo == 401 || codigo == 403) throw IllegalStateException("RetroAchievements no aceptó tu usuario o tu clave. Revísalos en Menú › Logros.")
            if (codigo == 429) throw IllegalStateException("RetroAchievements pide esperar un poco. Prueba en un minuto.")
            if (codigo !in 200..299) throw IllegalStateException("RetroAchievements respondió con un error ($codigo).")
            return c.inputStream.use { it.bufferedReader().readText() }
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("No pude conectar con RetroAchievements. Revisa tu internet.")
        } finally {
            c.disconnect()
        }
    }

    /** Revisa la cuenta y trae el perfil con los últimos logros. */
    fun perfil(usuario: String, clave: String): PerfilRA {
        val o = JSONObject(pedir("API_GetUserProfile.php?u=${cod(usuario)}&y=${cod(clave)}"))
        if (o.has("Error") || o.optString("User").isEmpty()) throw IllegalStateException("No encontré ese usuario en RetroAchievements.")
        val recientes = try {
            val a = JSONArray(pedir("API_GetUserRecentAchievements.php?u=${cod(usuario)}&y=${cod(clave)}&m=43200"))
            (0 until minOf(a.length(), 8)).map {
                val r = a.getJSONObject(it)
                r.optString("GameTitle") to Logro(
                    r.optInt("AchievementID"), r.optString("Title"), r.optString("Description"), r.optInt("Points"),
                    r.optString("BadgeName"), r.optString("Date"), r.optInt("HardcoreMode") == 1,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
        return PerfilRA(
            usuario = o.optString("User"),
            puntos = o.optInt("TotalPoints"),
            puntosReales = o.optInt("TotalTruePoints"),
            foto = MEDIOS + o.optString("UserPic"),
            desde = o.optString("MemberSince").take(10),
            lema = o.optString("Motto"),
            recientes = recientes,
        )
    }

    /** Lista de juegos con logros de una consola (se guarda una semana). */
    private fun listaJuegos(ctx: Context, consola: Int, clave: String): List<Pair<Int, String>> {
        val f = File(File(ctx.filesDir, "logros").apply { mkdirs() }, "consola-$consola.json")
        val texto = if (f.exists() && System.currentTimeMillis() - f.lastModified() < 7L * 24 * 3600 * 1000) f.readText()
        else pedir("API_GetGameList.php?i=$consola&f=1&y=${cod(clave)}").also { f.writeText(it) }
        val a = JSONArray(texto)
        return (0 until a.length()).map { a.getJSONObject(it).let { o -> o.optInt("ID") to o.optString("Title") } }
    }

    private fun normal(s: String) = Descargas.normalizar(s)

    /** Busca el juego en RetroAchievements por su nombre. */
    fun buscarJuego(ctx: Context, consola: String, titulos: List<String>, clave: String): Int? {
        val id = idConsola(consola) ?: return null
        val lista = listaJuegos(ctx, id, clave)
        val claves = titulos.map { normal(it) }.filter { it.isNotEmpty() }.distinct()
        for (k in claves) lista.firstOrNull { normal(it.second) == k }?.let { return it.first }
        // Nombres con subtítulo o "The" al final ("Legend of Zelda, The - A Link to the Past").
        for (k in claves) lista.firstOrNull { val n = normal(it.second); n.startsWith(k) || k.startsWith(n) && n.length > 6 }?.let { return it.first }
        return null
    }

    /** Los logros de un juego y cuáles ganaste. */
    fun deJuego(idJuego: Int, usuario: String, clave: String): LogrosJuego {
        val o = JSONObject(pedir("API_GetGameInfoAndUserProgress.php?g=$idJuego&u=${cod(usuario)}&y=${cod(clave)}"))
        val logros = ArrayList<Logro>()
        o.optJSONObject("Achievements")?.let { a ->
            for (k in a.keys()) {
                val l = a.getJSONObject(k)
                val duro = l.optString("DateEarnedHardcore").takeIf { it.isNotEmpty() && it != "null" }
                val normal = l.optString("DateEarned").takeIf { it.isNotEmpty() && it != "null" }
                logros += Logro(
                    l.optInt("ID"), l.optString("Title"), l.optString("Description"), l.optInt("Points"),
                    l.optString("BadgeName"), (duro ?: normal ?: "").take(10), duro != null,
                )
            }
        }
        // Primero los ganados (los más nuevos arriba), después los que faltan por puntos.
        val orden = logros.sortedWith(compareByDescending<Logro> { it.conseguido }.thenByDescending { it.ganado }.thenBy { it.puntos })
        return LogrosJuego(idJuego, o.optString("Title"), MEDIOS + o.optString("ImageIcon"), orden)
    }
}

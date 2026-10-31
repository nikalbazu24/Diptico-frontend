package cl.nikalbazu.diptico.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.Normalizer

/**
 * Descarga portadas, imágenes y títulos desde internet.
 *
 * - libretro-thumbnails: gratis y sin cuenta. Portadas (Named_Boxarts) e imágenes del juego (Named_Snaps).
 * - SteamGridDB: opcional, con una clave personal gratuita. Arte grande (heroes), logos y portadas.
 */
object Descargas {
    private const val AGENTE = "Diptico/0.2 (launcher para AYN Thor)"
    private const val LIBRETRO = "https://thumbnails.libretro.com"
    private const val SGDB = "https://www.steamgriddb.com/api/v2"

    data class Resultado(val medios: MediosJuego, val encontrado: Boolean)

    // ---------- Red ----------

    private fun abrir(url: String, clave: String? = null): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 12_000
        c.readTimeout = 20_000
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", AGENTE)
        if (clave != null) c.setRequestProperty("Authorization", "Bearer $clave")
        return c
    }

    private fun texto(url: String, clave: String? = null): String? {
        val c = abrir(url, clave)
        return try {
            if (c.responseCode !in 200..299) null else c.inputStream.use { it.bufferedReader().readText() }
        } catch (e: Exception) {
            null
        } finally {
            c.disconnect()
        }
    }

    private fun bajar(url: String, destino: File): Boolean {
        val c = abrir(url)
        return try {
            if (c.responseCode !in 200..299) return false
            val tmp = File(destino.parentFile, destino.name + ".tmp")
            c.inputStream.use { entrada -> tmp.outputStream().use { entrada.copyTo(it) } }
            if (tmp.length() < 512) {
                tmp.delete()
                false
            } else {
                tmp.renameTo(destino)
            }
        } catch (e: Exception) {
            false
        } finally {
            c.disconnect()
        }
    }

    /** Hay internet (probando con libretro). */
    fun hayInternet(): Boolean = try {
        val c = abrir("$LIBRETRO/")
        c.requestMethod = "HEAD"
        val ok = c.responseCode in 200..499
        c.disconnect()
        ok
    } catch (e: Exception) {
        false
    }

    private fun carpeta(ctx: Context, nombre: String): File = File(ctx.filesDir, nombre).apply { mkdirs() }

    private fun hash(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)

    private fun codificar(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    // ---------- Nombres ----------

    /** Deja un nombre comparable: sin región, sin tildes, sin "the", solo letras y números. */
    fun normalizar(nombre: String): String {
        var s = nombre.substringBeforeLast('.').let { if (it.length < nombre.length - 5) nombre else it }
        s = s.replace(Regex("[\\(\\[][^\\)\\]]*[\\)\\]]"), " ")
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
        s = s.lowercase().replace("&", " and ")
        s = s.replace(Regex("\\bthe\\b"), " ")
        s = s.replace(Regex("\\b(ii|iii|iv|v|vi|vii|viii|ix|x)\\b")) {
            when (it.value) {
                "ii" -> "2"; "iii" -> "3"; "iv" -> "4"; "v" -> "5"; "vi" -> "6"
                "vii" -> "7"; "viii" -> "8"; "ix" -> "9"; else -> "10"
            }
        }
        return s.replace(Regex("[^a-z0-9]"), "")
    }

    /** "Legend of Zelda, The - A Link to the Past (USA)" -> "The Legend of Zelda: A Link to the Past". */
    fun tituloBonito(nombre: String): String {
        var s = nombre.replace(Regex("\\s*[\\(\\[][^\\)\\]]*[\\)\\]]"), "").trim()
        val partes = s.split(" - ").toMutableList()
        partes[0] = partes[0].replace(Regex("^(.*), (The|A|An)$")) { "${it.groupValues[2]} ${it.groupValues[1]}" }
        s = partes.joinToString(": ")
        return s.ifEmpty { nombre }
    }

    /** Puntaje de región: preferimos USA, luego mundo, Europa y el resto. Menos es mejor. */
    private fun puntaje(nombre: String): Int {
        var p = when {
            nombre.contains("(USA", true) -> 0
            nombre.contains("(World", true) -> 1
            nombre.contains("(Europe", true) -> 2
            nombre.contains("(Japan", true) -> 4
            else -> 3
        }
        if (nombre.contains("(Beta", true) || nombre.contains("(Proto", true) || nombre.contains("(Demo", true)) p += 10
        if (nombre.contains("(Rev", true)) p += 1
        return p * 1000 + nombre.length
    }

    // ---------- libretro-thumbnails ----------

    private val indices = HashMap<String, Map<String, Map<String, String>>>()

    /**
     * Índice de una consola: carpeta (Named_Boxarts, Named_Snaps) -> nombre normalizado -> nombre real del archivo.
     * Se guarda en el teléfono para no pedirlo cada vez.
     */
    private fun indice(ctx: Context, sistema: String): Map<String, Map<String, String>> {
        indices[sistema]?.let { return it }
        val cache = File(carpeta(ctx, "indices"), hash(sistema) + ".json")
        val crudo: Map<String, List<String>> = if (cache.exists() && System.currentTimeMillis() - cache.lastModified() < 30L * 24 * 3600 * 1000) {
            leerIndice(cache.readText())
        } else {
            val nuevo = HashMap<String, List<String>>()
            for (dir in listOf("Named_Boxarts", "Named_Snaps", "Named_Titles")) {
                nuevo[dir] = listarLibretro(sistema, dir) ?: listarGithub(sistema, dir) ?: emptyList()
            }
            if (nuevo.values.any { it.isNotEmpty() }) {
                val o = JSONObject()
                for ((k, v) in nuevo) o.put(k, JSONArray(v))
                cache.writeText(o.toString())
            }
            nuevo
        }
        val salida = crudo.mapValues { (_, nombres) ->
            val m = HashMap<String, String>()
            for (n in nombres.sortedBy { puntaje(it) }) {
                val k = normalizar(n)
                if (k.isNotEmpty() && k !in m) m[k] = n
            }
            m as Map<String, String>
        }
        indices[sistema] = salida
        return salida
    }

    private fun leerIndice(s: String): Map<String, List<String>> = try {
        val o = JSONObject(s)
        o.keys().asSequence().associateWith { k ->
            val a = o.getJSONArray(k)
            (0 until a.length()).map { a.getString(it) }
        }
    } catch (e: Exception) {
        emptyMap()
    }

    /** Lista los .png de la página de libretro (es un índice de carpeta común). */
    private fun listarLibretro(sistema: String, dir: String): List<String>? {
        val html = texto("$LIBRETRO/${codificar(sistema)}/$dir/") ?: return null
        val nombres = Regex("href=\"([^\"]+\\.png)\"", RegexOption.IGNORE_CASE).findAll(html)
            .map { URLDecoder.decode(it.groupValues[1].replace("+", "%2B"), "UTF-8").substringAfterLast('/').removeSuffix(".png") }
            .toList()
        return nombres.ifEmpty { null }
    }

    /** Respaldo: el mismo índice desde el repositorio de GitHub de libretro-thumbnails. */
    private fun listarGithub(sistema: String, dir: String): List<String>? {
        val repo = sistema.replace(' ', '_')
        val json = texto("https://api.github.com/repos/libretro-thumbnails/$repo/git/trees/master?recursive=1") ?: return null
        return try {
            val arbol = JSONObject(json).getJSONArray("tree")
            (0 until arbol.length()).mapNotNull {
                val ruta = arbol.getJSONObject(it).getString("path")
                if (ruta.startsWith("$dir/") && ruta.endsWith(".png")) ruta.removePrefix("$dir/").removeSuffix(".png") else null
            }.ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    /** libretro reemplaza estos caracteres por "_" en el nombre del archivo. */
    private fun nombreArchivo(n: String) = n.replace(Regex("[&*/:`<>?\\\\|\"]"), "_")

    private fun bajarLibretro(sistema: String, dir: String, nombre: String, destino: File): Boolean {
        val archivo = codificar(nombreArchivo(nombre)) + ".png"
        if (bajar("$LIBRETRO/${codificar(sistema)}/$dir/$archivo", destino)) return true
        val repo = sistema.replace(' ', '_')
        return bajar("https://raw.githubusercontent.com/libretro-thumbnails/$repo/master/$dir/$archivo", destino)
    }

    // ---------- SteamGridDB ----------

    /** Revisa que la clave funcione. */
    fun probarSgdb(clave: String): Boolean = texto("$SGDB/search/autocomplete/${codificar("mario")}", clave)?.contains("\"success\":true") == true

    private fun idSgdb(titulo: String, clave: String): Int? {
        val json = texto("$SGDB/search/autocomplete/${codificar(titulo)}", clave) ?: return null
        return try {
            val d = JSONObject(json).optJSONArray("data") ?: return null
            if (d.length() == 0) null else d.getJSONObject(0).getInt("id")
        } catch (e: Exception) {
            null
        }
    }

    private fun urlSgdb(ruta: String, clave: String): String? {
        val json = texto("$SGDB$ruta", clave) ?: return null
        return try {
            val d = JSONObject(json).optJSONArray("data") ?: return null
            if (d.length() == 0) null else d.getJSONObject(0).optString("url").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    // ---------- Un juego ----------

    /**
     * Busca lo que falta de un juego. No toca lo que la persona ya tiene en su carpeta.
     * [previo] es lo ya descargado antes (para no repetir).
     */
    fun buscar(ctx: Context, juego: Juego, consola: Consola, previo: MediosJuego?, claveSgdb: String): Resultado {
        val dir = carpeta(ctx, "medios")
        val base = hash(juego.clave)
        var m = previo ?: MediosJuego()
        var encontrado = false

        fun existe(r: String?) = r != null && File(r).exists()
        // El ícono de un port no cuenta como portada: se puede mejorar con SteamGridDB.
        val tienePortada = juego.portada != null && juego.portada.scheme != "app"

        val sistema = consola.libretro
        if (sistema != null) {
            val idx = try { indice(ctx, sistema) } catch (e: Exception) { emptyMap() }
            val clave = normalizar(juego.archivo).ifEmpty { normalizar(juego.titulo) }
            val claveTitulo = normalizar(juego.titulo)
            fun encontrar(d: String): String? = idx[d]?.let { it[clave] ?: it[claveTitulo] }

            val caja = encontrar("Named_Boxarts")
            if (caja != null) {
                encontrado = true
                if (m.titulo == null) m = m.copy(titulo = tituloBonito(caja))
                if (!tienePortada && !existe(m.portada)) {
                    val f = File(dir, "$base-portada.png")
                    if (bajarLibretro(sistema, "Named_Boxarts", caja, f)) m = m.copy(portada = f.absolutePath)
                }
            }
            if (juego.arte == null && !existe(m.arte)) {
                val foto = encontrar("Named_Snaps") ?: encontrar("Named_Titles")
                if (foto != null) {
                    encontrado = true
                    if (m.titulo == null) m = m.copy(titulo = tituloBonito(foto))
                    val f = File(dir, "$base-arte.png")
                    val d = if (encontrar("Named_Snaps") != null) "Named_Snaps" else "Named_Titles"
                    if (bajarLibretro(sistema, d, foto, f)) m = m.copy(arte = f.absolutePath)
                }
            }
        }

        if (claveSgdb.isNotBlank()) {
            val titulo = m.titulo ?: juego.titulo
            val id = idSgdb(titulo, claveSgdb)
            if (id != null) {
                encontrado = true
                // El arte grande de SteamGridDB se ve mucho mejor en la pantalla de arriba que una captura.
                val arteEsCaptura = m.arte != null
                if (juego.arte == null && (!existe(m.arte) || arteEsCaptura)) {
                    urlSgdb("/heroes/game/$id", claveSgdb)?.let { u ->
                        val f = File(dir, "$base-hero.${extension(u)}")
                        if (bajar(u, f)) m = m.copy(arte = f.absolutePath)
                    }
                }
                if (!existe(m.logo)) {
                    urlSgdb("/logos/game/$id", claveSgdb)?.let { u ->
                        val f = File(dir, "$base-logo.${extension(u)}")
                        if (bajar(u, f)) m = m.copy(logo = f.absolutePath)
                    }
                }
                if (!tienePortada && !existe(m.portada)) {
                    urlSgdb("/grids/game/$id?dimensions=600x900", claveSgdb)?.let { u ->
                        val f = File(dir, "$base-portada.${extension(u)}")
                        if (bajar(u, f)) m = m.copy(portada = f.absolutePath)
                    }
                }
            }
        }
        return Resultado(m, encontrado)
    }

    private fun extension(url: String): String =
        url.substringAfterLast('.', "png").substringBefore('?').lowercase().takeIf { it.length in 3..4 } ?: "png"

    /** Aplica lo descargado a los juegos de la biblioteca (lo propio de la carpeta siempre manda). */
    fun aplicar(juegos: List<Juego>, medios: Map<String, MediosJuego>, titulos: Boolean): List<Juego> = juegos.map { j ->
        val m = medios[j.clave] ?: return@map j
        fun uri(r: String?) = r?.let { File(it) }?.takeIf { it.exists() }?.let { Uri.fromFile(it) }
        j.copy(
            portada = if (j.portada?.scheme == "app") (uri(m.portada) ?: j.portada) else (j.portada ?: uri(m.portada)),
            arte = j.arte ?: uri(m.arte),
            logo = j.logo ?: uri(m.logo),
            titulo = if (titulos && m.titulo != null) m.titulo else j.titulo,
        )
    }

    /** Borra todo lo descargado. */
    fun borrar(ctx: Context) {
        File(ctx.filesDir, "medios").deleteRecursively()
        File(ctx.filesDir, "indices").deleteRecursively()
        indices.clear()
    }

    fun espacioUsado(ctx: Context): Long = File(ctx.filesDir, "medios").walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

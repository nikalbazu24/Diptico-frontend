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
    private const val AGENTE = Fichas.AGENTE
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

    /** Reconoce el nombre oficial de un juego con la lista de libretro (sin bajar imágenes). */
    fun identificar(ctx: Context, juego: Juego, consola: Consola): String? {
        val sistema = consola.libretro ?: return null
        val idx = try { indice(ctx, sistema) } catch (e: Exception) { return null }
        val k1 = normalizar(juego.archivo)
        val k2 = normalizar(juego.titulo)
        for (d in listOf("Named_Boxarts", "Named_Titles", "Named_Snaps")) {
            val m = idx[d] ?: continue
            (m[k1] ?: m[k2])?.let { return tituloBonito(it) }
        }
        return null
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

    // ---------- Elegir una imagen entre varias ----------

    /** Varias opciones de SteamGridDB para un tipo (grids, heroes, logos, icons). */
    private fun urlsSgdb(ruta: String, clave: String, max: Int = 24): List<ImagenWeb> {
        val json = texto("$SGDB$ruta", clave) ?: return emptyList()
        return try {
            val d = JSONObject(json).optJSONArray("data") ?: return emptyList()
            (0 until minOf(d.length(), max)).mapNotNull {
                val o = d.getJSONObject(it)
                val u = o.optString("url").takeIf { u -> u.isNotEmpty() } ?: return@mapNotNull null
                ImagenWeb(u, o.optString("thumb").ifEmpty { u }, "SteamGridDB")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun urlLibretro(sistema: String, dir: String, nombre: String) =
        "$LIBRETRO/${codificar(sistema)}/$dir/${codificar(nombreArchivo(nombre))}.png"

    /** Capturas y pantallas de título de libretro (para el carrusel de la ficha). */
    fun fotosLibretro(ctx: Context, juego: Juego, consola: Consola): List<ImagenWeb> {
        val sistema = consola.libretro ?: return emptyList()
        val idx = try { indice(ctx, sistema) } catch (e: Exception) { return emptyList() }
        val k1 = normalizar(juego.archivo)
        val k2 = normalizar(juego.titulo)
        return listOf("Named_Titles", "Named_Snaps").mapNotNull { d ->
            val n = idx[d]?.let { it[k1] ?: it[k2] } ?: return@mapNotNull null
            val u = urlLibretro(sistema, d, n)
            ImagenWeb(u, u, "libretro", if (d == "Named_Titles") "Pantalla de título" else "Captura")
        }
    }

    /**
     * Todas las opciones de internet para una imagen de un juego.
     * [tipo]: "portada", "arte" (imagen grande / hero) o "logo".
     */
    fun candidatos(ctx: Context, juego: Juego, consola: Consola, tipo: String, titulo: String, claveSgdb: String, wiki: FichaInfo?): List<ImagenWeb> {
        val lista = ArrayList<ImagenWeb>()
        val sistema = consola.libretro
        if (sistema != null && tipo != "logo") {
            val idx = try { indice(ctx, sistema) } catch (e: Exception) { emptyMap() }
            val k1 = normalizar(juego.archivo)
            val k2 = normalizar(titulo)
            val dirs = if (tipo == "portada") listOf("Named_Boxarts") else listOf("Named_Snaps", "Named_Titles")
            for (d in dirs) idx[d]?.let { it[k1] ?: it[k2] }?.let { n ->
                val u = urlLibretro(sistema, d, n)
                lista += ImagenWeb(u, u, "libretro")
            }
        }
        if (claveSgdb.isNotBlank()) {
            val id = idSgdb(titulo, claveSgdb)
            if (id != null) lista += when (tipo) {
                "portada" -> urlsSgdb("/grids/game/$id?dimensions=600x900,342x482,660x930", claveSgdb)
                "logo" -> urlsSgdb("/logos/game/$id", claveSgdb)
                else -> urlsSgdb("/heroes/game/$id", claveSgdb)
            }
        }
        if (wiki != null) {
            when (tipo) {
                "portada" -> wiki.principal?.let { lista += it }
                "logo" -> lista += wiki.imagenes.filter { "logo" in it.nombre.lowercase() }
                else -> lista += wiki.imagenes.filter { it.url != wiki.principal?.url && "logo" !in it.nombre.lowercase() }
            }
        }
        return lista.distinctBy { it.url }
    }

    /**
     * Opciones de internet para las imágenes de una consola: SteamGridDB (por su nombre), Wikipedia (fotos de la consola)
     * y, para el ícono, los paquetes de RetroArch. [tipo]: "c_hero", "c_logo" o "c_icono".
     */
    fun candidatosConsola(consola: Consola, tipo: String, nombre: String, claveSgdb: String, wiki: FichaInfo?): List<ImagenWeb> {
        val lista = ArrayList<ImagenWeb>()
        if (tipo == "c_icono") {
            consola.icono?.let { ic ->
                for (p in PAQUETES) {
                    val u = "https://raw.githubusercontent.com/libretro/retroarch-assets/master/xmb/${p.id}/png/${codificar(ic)}.png"
                    lista += ImagenWeb(u, u, "RetroArch", p.nombre)
                }
            }
        }
        if (claveSgdb.isNotBlank()) {
            val id = idSgdb(nombre, claveSgdb)
            if (id != null) lista += when (tipo) {
                "c_logo" -> urlsSgdb("/logos/game/$id", claveSgdb)
                "c_icono" -> urlsSgdb("/icons/game/$id", claveSgdb)
                else -> urlsSgdb("/heroes/game/$id", claveSgdb)
            }
        }
        if (wiki != null && tipo != "c_icono") {
            lista += if (tipo == "c_logo") wiki.imagenes.filter { "logo" in it.nombre.lowercase() } else listOfNotNull(wiki.principal) + wiki.imagenes.filter { "logo" !in it.nombre.lowercase() }
        }
        return lista.distinctBy { it.url }
    }

    /** Baja una imagen elegida para una consola. */
    fun bajarParaConsola(ctx: Context, consola: String, tipo: String, url: String): File? {
        val f = File(carpeta(ctx, "medios"), "consola-$consola-$tipo-${System.currentTimeMillis() % 100000}.${extension(url)}")
        return if (bajar(url, f)) f else null
    }

    /** Baja una imagen elegida a la carpeta de Díptico. Devuelve el archivo o null. */
    fun bajarElegida(ctx: Context, juego: Juego, tipo: String, url: String): File? {
        val f = File(carpeta(ctx, "medios"), "${hash(juego.clave)}-propia-$tipo-${System.currentTimeMillis() % 100000}.${extension(url)}")
        return if (bajar(url, f)) f else null
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

    // ---------- Íconos de consola (paquetes de RetroArch) ----------

    /** Paquetes de íconos: id en retroarch-assets, nombre para mostrar y si son blancos (se tiñen con el tema). */
    data class PaqueteIconos(val id: String, val nombre: String, val blanco: Boolean)

    val PAQUETES = listOf(
        PaqueteIconos("systematic", "Consolas a color", false),
        PaqueteIconos("dot-art", "Pixel art", false),
        PaqueteIconos("daite", "Daite", false),
        PaqueteIconos("flatux", "Controles", false),
        PaqueteIconos("retrosystem", "Controles retro", false),
        PaqueteIconos("automatic", "Contornos", true),
        PaqueteIconos("monochrome", "Siluetas", true),
    )

    fun archivoIcono(ctx: Context, paquete: String, consola: String): File = File(File(ctx.filesDir, "iconos/$paquete"), "$consola.png")

    /**
     * Baja los íconos de un paquete para las consolas indicadas. Devuelve cuántos consiguió.
     * [avance] se llama con (hechos, total).
     */
    fun bajarIconos(ctx: Context, paquete: String, consolas: List<Consola>, avance: (Int, Int) -> Unit): Int {
        val con = consolas.filter { it.icono != null }
        var ok = 0
        con.forEachIndexed { i, c ->
            avance(i, con.size)
            val destino = archivoIcono(ctx, paquete, c.id)
            if (destino.exists()) { ok++; return@forEachIndexed }
            destino.parentFile?.mkdirs()
            val nombre = codificar(c.icono!!)
            if (bajar("https://raw.githubusercontent.com/libretro/retroarch-assets/master/xmb/$paquete/png/$nombre.png", destino)) ok++
        }
        return ok
    }

    /** Borra todo lo descargado. */
    fun borrar(ctx: Context) {
        File(ctx.filesDir, "medios").deleteRecursively()
        File(ctx.filesDir, "indices").deleteRecursively()
        indices.clear()
    }

    fun espacioUsado(ctx: Context): Long = File(ctx.filesDir, "medios").walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

package cl.nikalbazu.diptico.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document

/**
 * Busca juegos dentro de la carpeta elegida. Espera una carpeta por consola
 * (snes, n64, psx, gba, gc, nds, ps2, psp), igual que ES-DE.
 * Usa consultas directas al proveedor de documentos porque es mucho más rápido que DocumentFile.
 */
object Biblioteca {
    private val IMAGENES = setOf("png", "jpg", "jpeg", "webp")
    private val CARPETAS_PORTADAS = setOf("covers", "boxart", "box", "images", "imagenes", "portadas")

    private data class Doc(val id: String, val nombre: String, val esCarpeta: Boolean)

    private fun listar(ctx: Context, arbol: Uri, docId: String): List<Doc> {
        val hijos = DocumentsContract.buildChildDocumentsUriUsingTree(arbol, docId)
        val salida = mutableListOf<Doc>()
        ctx.contentResolver.query(
            hijos,
            arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val nombre = c.getString(1) ?: continue
                val mime = c.getString(2) ?: ""
                salida += Doc(id, nombre, mime == Document.MIME_TYPE_DIR)
            }
        }
        return salida
    }

    private fun nombreDe(ctx: Context, arbol: Uri, docId: String): String? {
        val uri = DocumentsContract.buildDocumentUriUsingTree(arbol, docId)
        return try {
            ctx.contentResolver.query(uri, arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun juntarImagenes(arbol: Uri, docs: List<Doc>, destino: MutableMap<String, Uri>) {
        for (d in docs) {
            if (d.esCarpeta) continue
            val ext = d.nombre.substringAfterLast('.', "").lowercase()
            if (ext in IMAGENES) {
                destino.putIfAbsent(d.nombre.substringBeforeLast('.').lowercase(), DocumentsContract.buildDocumentUriUsingTree(arbol, d.id))
            }
        }
    }

    /** Nombre corto de una carpeta para mostrar: "ROMs/switch" o "SD · Juegos". */
    fun nombreCarpeta(uri: Uri): String = try {
        val id = DocumentsContract.getTreeDocumentId(uri)
        val vol = id.substringBefore(':')
        val ruta = id.substringAfter(':', "").ifEmpty { "(raíz)" }
        if (vol.equals("primary", true)) ruta else "SD · $ruta"
    } catch (e: Exception) {
        uri.lastPathSegment ?: "Carpeta"
    }

    fun escanear(ctx: Context, arbol: Uri, media: Uri?, consolaFija: String? = null): List<Juego> {
        val raizId = DocumentsContract.getTreeDocumentId(arbol)
        val fija = consolaFija?.let { Consolas.porId(it) }
        if (fija != null) return escanearConsolas(ctx, arbol, media, listOf(Doc(raizId, nombreDe(ctx, arbol, raizId) ?: fija.id, true) to fija))
        val raiz = listar(ctx, arbol, raizId)

        // Carpetas de consola dentro de la carpeta elegida...
        val carpetasConsola = raiz.filter { it.esCarpeta }.mapNotNull { d -> Consolas.porCarpeta(d.nombre)?.let { d to it } }.toMutableList()
        // ...o la carpeta elegida ya es de una consola (por ejemplo, eligieron "psx" directamente).
        if (carpetasConsola.isEmpty()) {
            val nombreRaiz = nombreDe(ctx, arbol, raizId)
            val consola = nombreRaiz?.let { Consolas.porCarpeta(it) }
            if (consola != null) carpetasConsola += Doc(raizId, nombreRaiz, true) to consola
        }

        return escanearConsolas(ctx, arbol, media, carpetasConsola)
    }

    private fun escanearConsolas(ctx: Context, arbol: Uri, media: Uri?, carpetasConsola: List<Pair<Doc, Consola>>): List<Juego> {
        val mediaRaiz = media?.let { m -> listar(ctx, m, DocumentsContract.getTreeDocumentId(m)) }
        val juegos = mutableListOf<Juego>()

        for ((dir, consola) in carpetasConsola) {
            val hijos = listar(ctx, arbol, dir.id)
            val portadas = HashMap<String, Uri>()
            val artes = HashMap<String, Uri>()

            juntarImagenes(arbol, hijos, portadas)
            for (sub in hijos) {
                if (!sub.esCarpeta) continue
                val n = sub.nombre.lowercase()
                if (n in CARPETAS_PORTADAS) juntarImagenes(arbol, listar(ctx, arbol, sub.id), portadas)
                if (n == "media") {
                    listar(ctx, arbol, sub.id).firstOrNull { it.esCarpeta && it.nombre.equals("covers", true) }?.let {
                        juntarImagenes(arbol, listar(ctx, arbol, it.id), portadas)
                    }
                }
            }

            // Imágenes descargadas por ES-DE (downloaded_media/<consola>/covers, fanart o screenshots).
            if (media != null && mediaRaiz != null) {
                mediaRaiz.firstOrNull { it.esCarpeta && it.nombre.equals(dir.nombre, true) }?.let { sistema ->
                    val categorias = listar(ctx, media, sistema.id)
                    categorias.firstOrNull { it.esCarpeta && it.nombre.equals("covers", true) }?.let {
                        juntarImagenes(media, listar(ctx, media, it.id), portadas)
                    }
                    (categorias.firstOrNull { it.esCarpeta && it.nombre.equals("fanart", true) }
                        ?: categorias.firstOrNull { it.esCarpeta && it.nombre.equals("screenshots", true) })?.let {
                        juntarImagenes(media, listar(ctx, media, it.id), artes)
                    }
                }
            }

            val nombres = hijos.filter { !it.esCarpeta }.map { it.nombre.lowercase() }
            val hayIndices = nombres.any { it.endsWith(".cue") || it.endsWith(".m3u") }

            for (f in hijos) {
                // Fan games de RPG Maker: la carpeta es un juego solo si trae Game.ini (o Game.exe / mkxp.json).
                if (f.esCarpeta && consola.id == Consolas.RPGMAKER) {
                    val n = f.nombre.lowercase()
                    if (n in CARPETAS_PORTADAS || n == "media" || n.startsWith(".")) continue
                    rpgMaker(ctx, arbol, f, consola, portadas, artes)?.let { juegos += it }
                    continue
                }
                if (f.esCarpeta) {
                    // Juegos en carpeta: PS3/Wii U/Xbox 360, o una carpeta con extensión ("Juego.ps3"), como en ES-DE.
                    val n = f.nombre.lowercase()
                    val conExtension = n.substringAfterLast('.', "").let { it.isNotEmpty() && it in consola.extensiones }
                    if ((consola.carpetasSonJuegos || conExtension) && n !in CARPETAS_PORTADAS && n != "media" && !n.startsWith(".")) {
                        juegos += Juego(
                            clave = consola.id + "/" + f.nombre,
                            consolaId = consola.id,
                            titulo = limpiarTitulo(f.nombre),
                            archivo = f.nombre,
                            uri = DocumentsContract.buildDocumentUriUsingTree(arbol, f.id),
                            docId = f.id,
                            portada = portadas[n],
                            arte = artes[n],
                            esCarpeta = true,
                        )
                    }
                    continue
                }
                val ext = f.nombre.substringAfterLast('.', "").lowercase()
                if (ext !in consola.extensiones) continue
                // Los .bin de un juego con .cue son pistas: se muestra solo el .cue.
                if (hayIndices && (ext == "bin" || ext == "img") && "cue" in consola.extensiones) continue
                if (f.nombre.startsWith(".")) continue
                val base = f.nombre.substringBeforeLast('.')
                juegos += Juego(
                    clave = consola.id + "/" + f.nombre,
                    consolaId = consola.id,
                    titulo = limpiarTitulo(base),
                    archivo = f.nombre,
                    uri = DocumentsContract.buildDocumentUriUsingTree(arbol, f.id),
                    docId = f.id,
                    portada = portadas[base.lowercase()],
                    arte = artes[base.lowercase()],
                )
            }
        }
        return sinDuplicados(juegos).sortedWith(compareBy<Juego>({ Consolas.orden(it.consolaId) }, { it.titulo.lowercase() }))
    }

    private val ARCHIVOS_RPGMAKER = setOf("game.ini", "game.exe", "mkxp.json")

    /**
     * Lo ya leído de cada fan game (título y pantalla de título), guardado entre arranques:
     * así buscar juegos no vuelve a abrir cada Game.ini y cada carpeta de imágenes.
     */
    private var cacheRpg: MutableMap<String, Pair<String?, String?>>? = null

    private fun cacheRpg(ctx: Context): MutableMap<String, Pair<String?, String?>> = cacheRpg ?: run {
        val m = HashMap<String, Pair<String?, String?>>()
        try {
            val f = java.io.File(ctx.filesDir, "rpgmaker.json")
            if (f.exists()) {
                val o = org.json.JSONObject(f.readText())
                for (k in o.keys()) o.getJSONArray(k).let { a -> m[k] = a.optString(0).ifEmpty { null } to a.optString(1).ifEmpty { null } }
            }
        } catch (_: Exception) {}
        m.also { cacheRpg = it }
    }

    private fun guardarCacheRpg(ctx: Context) {
        val m = cacheRpg ?: return
        try {
            val o = org.json.JSONObject()
            for ((k, v) in m) o.put(k, org.json.JSONArray().put(v.first ?: "").put(v.second ?: ""))
            java.io.File(ctx.filesDir, "rpgmaker.json").writeText(o.toString())
        } catch (_: Exception) {}
    }

    /**
     * Un fan game de RPG Maker en su carpeta: el título sale del Game.ini y, si no hay imagen propia,
     * la pantalla de título del juego (Graphics/Titles) sirve de imagen grande.
     */
    private fun rpgMaker(ctx: Context, arbol: Uri, dir: Doc, consola: Consola, portadas: Map<String, Uri>, artes: Map<String, Uri>): Juego? {
        val n = dir.nombre.lowercase()
        val cache = cacheRpg(ctx)
        val guardado = cache[dir.id]
        val titulo: String?
        val pantallaTitulo: Uri?
        if (guardado != null) {
            // Ya se leyó antes: nada de abrir archivos (si se borró, Díptico lo nota al jugar o al desinstalar).
            titulo = guardado.first
            pantallaTitulo = guardado.second?.let { DocumentsContract.buildDocumentUriUsingTree(arbol, it) }
        } else {
            val dentro = try { listar(ctx, arbol, dir.id) } catch (e: Exception) { return null }
            if (dentro.none { !it.esCarpeta && it.nombre.lowercase() in ARCHIVOS_RPGMAKER }) return null
            val ini = dentro.firstOrNull { !it.esCarpeta && it.nombre.equals("Game.ini", true) }
            titulo = ini?.let { tituloIni(ctx, DocumentsContract.buildDocumentUriUsingTree(arbol, it.id)) }
            val docTitulo = try {
                dentro.firstOrNull { it.esCarpeta && it.nombre.equals("Graphics", true) }
                    ?.let { g -> listar(ctx, arbol, g.id).firstOrNull { it.esCarpeta && it.nombre.equals("Titles", true) } }
                    ?.let { t -> listar(ctx, arbol, t.id).firstOrNull { !it.esCarpeta && it.nombre.substringAfterLast('.', "").lowercase() in IMAGENES } }
                    ?.id
            } catch (e: Exception) { null }
            pantallaTitulo = docTitulo?.let { DocumentsContract.buildDocumentUriUsingTree(arbol, it) }
            cache[dir.id] = titulo to docTitulo
            guardarCacheRpg(ctx)
        }
        return Juego(
            clave = consola.id + "/" + dir.nombre,
            consolaId = consola.id,
            titulo = titulo ?: limpiarTitulo(dir.nombre),
            archivo = dir.nombre,
            uri = DocumentsContract.buildDocumentUriUsingTree(arbol, dir.id),
            docId = dir.id,
            portada = portadas[n],
            arte = artes[n] ?: pantallaTitulo,
            esCarpeta = true,
        )
    }

    /** Olvida lo leído de los fan games (Menú › Biblioteca › Volver a buscar juegos, manteniendo). */
    fun olvidarFanGames(ctx: Context) {
        cacheRpg = HashMap()
        try { java.io.File(ctx.filesDir, "rpgmaker.json").delete() } catch (_: Exception) {}
    }

    /** Lee "Title=" del Game.ini de un juego de RPG Maker. */
    private fun tituloIni(ctx: Context, uri: Uri): String? = try {
        ctx.contentResolver.openInputStream(uri)?.use { s ->
            val bytes = s.readBytes().let { if (it.size > 16_384) it.copyOf(16_384) else it }
            // Los Game.ini suelen venir en UTF-8; los antiguos, en Windows-1252.
            val texto = String(bytes, Charsets.UTF_8).let { if ('\uFFFD' in it) String(bytes, Charsets.ISO_8859_1) else it }
            texto.lineSequence().map { it.trim() }.firstOrNull { it.startsWith("Title=", true) }
                ?.substringAfter('=')?.trim()?.removePrefix("\uFEFF")?.takeIf { it.isNotEmpty() && it.length <= 80 }
        }
    } catch (e: Exception) { null }

    /** "Leyenda del Bosque (USA) [!]" -> "Leyenda del Bosque". */
    fun limpiarTitulo(base: String): String {
        var t = base
            .replace(Regex("\\s*[\\(\\[\\{][^\\)\\]\\}]*[\\)\\]\\}]"), " ") // (USA) [!] {v1.2}
            .replace('_', ' ')
            .replace(Regex("(?<=[a-z])(?=[A-Z][a-z])"), " ") // SuperMarioOdyssey -> Super Mario Odyssey
            .replace(Regex("\\b[0-9A-F]{16}\\b"), " ") // códigos de juego de Switch
            .replace(Regex("\\b[A-Z]{4}[0-9]{5}\\b"), " ") // códigos de PlayStation (BLUS30443)
            .replace(Regex("(?i)\\bv?\\d+(\\.\\d+){1,3}\\b"), " ") // versiones 1.0.2
            .replace(Regex("(?i)\\b(nsp|xci|nsz|usa|eur|jpn|pal|ntsc|multi\\d*|update|dlc|rev ?\\d)\\b"), " ")
        if (!t.contains(' ') && t.count { it == '.' } >= 2) t = t.replace('.', ' ')
        t = t.replace(Regex("\\s*-\\s*$"), "").replace(Regex("\\s{2,}"), " ").trim()
        return t.ifEmpty { base }
    }

    /** Formatos preferidos cuando hay varios archivos del mismo juego (menos es mejor). */
    private val PREFERENCIA = listOf("m3u", "chd", "rvz", "cue", "iso", "xci", "nsp", "nsz", "cso", "pbp", "zip", "7z")

    /**
     * Si el mismo juego está dos veces (por ejemplo .cue y .chd, o versión USA y Europa), deja uno solo:
     * el de mejor formato y, si empatan, el de región USA o mundial.
     */
    fun sinDuplicados(juegos: List<Juego>): List<Juego> {
        fun region(a: String): Int = when {
            a.contains("(USA", true) || a.contains("(World", true) -> 0
            a.contains("(Europe", true) || a.contains("(Spain", true) -> 1
            a.contains("(Japan", true) -> 3
            else -> 2
        }
        fun formato(j: Juego): Int = if (j.esCarpeta) 0 else PREFERENCIA.indexOf(j.tipo).let { if (it < 0) PREFERENCIA.size else it }
        // Los discos de un mismo juego (Disc 1, Disc 2) no son duplicados.
        fun disco(a: String) = Regex("[\\(\\[ ](Disc|Disk|CD|Disco) ?(\\d+)", RegexOption.IGNORE_CASE).find(a)?.groupValues?.get(2) ?: ""
        return juegos.groupBy { j -> j.consolaId + "|" + Descargas.normalizar(j.titulo).ifEmpty { j.archivo } + "|" + disco(j.archivo) }
            .values.map { grupo -> grupo.minWith(compareBy<Juego>({ formato(it) }, { region(it.archivo) }, { it.archivo.length })) }
    }
}

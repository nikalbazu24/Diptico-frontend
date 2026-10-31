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

    fun escanear(ctx: Context, arbol: Uri, media: Uri?): List<Juego> {
        val raizId = DocumentsContract.getTreeDocumentId(arbol)
        val raiz = listar(ctx, arbol, raizId)

        // Carpetas de consola dentro de la carpeta elegida...
        val carpetasConsola = raiz.filter { it.esCarpeta }.mapNotNull { d -> Consolas.porCarpeta(d.nombre)?.let { d to it } }.toMutableList()
        // ...o la carpeta elegida ya es de una consola (por ejemplo, eligieron "psx" directamente).
        if (carpetasConsola.isEmpty()) {
            val nombreRaiz = nombreDe(ctx, arbol, raizId)
            val consola = nombreRaiz?.let { Consolas.porCarpeta(it) }
            if (consola != null) carpetasConsola += Doc(raizId, nombreRaiz, true) to consola
        }

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
        return juegos.sortedWith(compareBy<Juego>({ Consolas.orden(it.consolaId) }, { it.titulo.lowercase() }))
    }

    /** "Leyenda del Bosque (USA) [!]" -> "Leyenda del Bosque". */
    fun limpiarTitulo(base: String): String {
        val limpio = base
            .replace(Regex("\\s*[\\(\\[][^\\)\\]]*[\\)\\]]"), "")
            .replace('_', ' ')
            .trim()
        return limpio.ifEmpty { base }
    }
}

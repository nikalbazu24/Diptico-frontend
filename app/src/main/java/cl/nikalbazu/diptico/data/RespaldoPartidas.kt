package cl.nikalbazu.diptico.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.security.MessageDigest

/**
 * Respaldo automático de partidas: al terminar de jugar, copia los guardados que cambiaron durante la partida
 * (en las carpetas de partidas que elegiste, por ejemplo RetroArch/saves) a una copia propia de Díptico.
 * Nunca toca los archivos del emulador salvo cuando tú pides restaurar una copia.
 *
 * Estructura: filesDir/respaldo-partidas/<juego>/<fecha>/<archivo> y, al lado, <archivo>.origen con su dirección original.
 */
internal object RespaldoPartidas {
    const val CARPETA = "respaldo-partidas"
    private const val MAX_ARCHIVO = 64L * 1024 * 1024
    private const val MAX_ARCHIVOS = 40
    private const val MAX_REVISADOS = 4_000
    private const val PROFUNDIDAD = 4
    /** Cuántas copias se guardan por juego (las más nuevas). */
    const val VERSIONES = 3

    data class Copia(val carpeta: File, val fecha: Long, val archivos: List<File>)

    private fun idJuego(clave: String) =
        MessageDigest.getInstance("SHA-256").digest(clave.toByteArray()).take(12).joinToString("") { "%02x".format(it) }

    private fun dirJuego(ctx: Context, clave: String) = File(ctx.filesDir, "$CARPETA/${idJuego(clave)}")

    private data class Doc(val uri: Uri, val nombre: String, val modificado: Long, val tam: Long)

    /** Los archivos (no carpetas) bajo [arbol], hasta [PROFUNDIDAD] niveles, modificados desde [desde]. */
    private fun cambiados(ctx: Context, arbol: Uri, desde: Long): List<Doc> {
        val cr = ctx.contentResolver
        val salida = mutableListOf<Doc>()
        var revisados = 0
        val cols = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        fun recorrer(docId: String, nivel: Int) {
            if (nivel > PROFUNDIDAD || revisados > MAX_REVISADOS || salida.size >= MAX_ARCHIVOS) return
            val hijos = DocumentsContract.buildChildDocumentsUriUsingTree(arbol, docId)
            val carpetas = mutableListOf<String>()
            try {
                cr.query(hijos, cols, null, null, null)?.use { c ->
                    while (c.moveToNext()) {
                        revisados++
                        val id = c.getString(0) ?: continue
                        val nombre = c.getString(1) ?: continue
                        if (nombre.startsWith(".")) continue
                        if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) { carpetas += id; continue }
                        val mod = if (c.isNull(3)) 0L else c.getLong(3)
                        val tam = if (c.isNull(4)) 0L else c.getLong(4)
                        if (mod >= desde && tam in 1..MAX_ARCHIVO) salida += Doc(DocumentsContract.buildDocumentUriUsingTree(arbol, id), nombre, mod, tam)
                    }
                }
            } catch (_: Exception) {
                return
            }
            for (id in carpetas) recorrer(id, nivel + 1)
        }
        try { recorrer(DocumentsContract.getTreeDocumentId(arbol), 0) } catch (_: Exception) {}
        return salida
    }

    /**
     * Copia lo que cambió en [carpetas] desde [desde] (el comienzo de la partida, con un minuto de margen).
     * Devuelve cuántos archivos se respaldaron (0 = nada cambió).
     */
    @Synchronized
    fun respaldar(ctx: Context, clave: String, carpetas: List<Uri>, desde: Long): Int {
        val docs = carpetas.flatMap { cambiados(ctx, it, desde - 60_000) }.sortedByDescending { it.modificado }.take(MAX_ARCHIVOS)
        if (docs.isEmpty()) return 0
        val ahora = System.currentTimeMillis()
        val dir = File(dirJuego(ctx, clave), ahora.toString()).apply { mkdirs() }
        var n = 0
        val usados = mutableSetOf<String>()
        for (d in docs) {
            // Mismo nombre en dos carpetas: se numera para no pisarse.
            var nombre = d.nombre.replace('/', '_').take(120)
            var k = 2
            while (!usados.add(nombre)) nombre = "${k++}-${d.nombre.replace('/', '_').take(110)}"
            val destino = File(dir, nombre)
            val ok = runCatching {
                ctx.contentResolver.openInputStream(d.uri)?.use { i -> destino.outputStream().use { o -> i.copyTo(o) } } ?: error("sin datos")
                File(dir, "$nombre.origen").writeText(d.uri.toString())
            }.isSuccess
            if (ok) n++ else { destino.delete(); File(dir, "$nombre.origen").delete() }
        }
        if (n == 0) dir.deleteRecursively()
        // Solo quedan las más nuevas.
        listar(ctx, clave).drop(VERSIONES).forEach { it.carpeta.deleteRecursively() }
        return n
    }

    /** Las copias del juego, de la más nueva a la más vieja. */
    fun listar(ctx: Context, clave: String): List<Copia> =
        dirJuego(ctx, clave).listFiles()?.filter { it.isDirectory && it.name.toLongOrNull() != null }
            ?.map { d -> Copia(d, d.name.toLong(), d.listFiles()?.filter { it.isFile && it.extension != "origen" }?.sortedBy { it.name }.orEmpty()) }
            ?.filter { it.archivos.isNotEmpty() }
            ?.sortedByDescending { it.fecha }.orEmpty()

    /**
     * Devuelve la copia a su lugar original (el emulador debe estar cerrado). Antes guarda lo que había ahora como una
     * copia más, para poder deshacer. Devuelve cuántos archivos se restauraron.
     */
    @Synchronized
    fun restaurar(ctx: Context, copia: Copia): Int {
        val cr = ctx.contentResolver
        var n = 0
        for (f in copia.archivos) {
            val origen = File(copia.carpeta, "${f.name}.origen").takeIf { it.isFile }?.readText()?.trim()?.let(Uri::parse) ?: continue
            val ok = runCatching {
                cr.openOutputStream(origen, "wt")?.use { o -> f.inputStream().use { it.copyTo(o) } } ?: error("sin destino")
            }.isSuccess
            if (ok) n++
        }
        return n
    }

    /** Para el respaldo a Drive: solo la copia más nueva de cada juego, sin pasar de [maximo] bytes en total. */
    fun paraRespaldo(ctx: Context, maximo: Long): List<File> {
        val raiz = File(ctx.filesDir, CARPETA)
        var total = 0L
        val salida = mutableListOf<File>()
        raiz.listFiles()?.filter { it.isDirectory }?.forEach { juego ->
            val ultima = juego.listFiles()?.filter { it.isDirectory && it.name.toLongOrNull() != null }?.maxByOrNull { it.name.toLong() } ?: return@forEach
            val archivos = ultima.listFiles()?.filter { it.isFile }.orEmpty()
            val tam = archivos.sumOf { it.length() }
            if (total + tam > maximo || salida.size + archivos.size > 1_200) return@forEach
            total += tam
            salida += archivos
        }
        return salida
    }
}

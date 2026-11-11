package cl.nikalbazu.diptico.estado

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.os.SystemClock
import android.view.Display
import cl.nikalbazu.diptico.audio.Aviso
import cl.nikalbazu.diptico.audio.Avisos
import cl.nikalbazu.diptico.audio.Medios
import cl.nikalbazu.diptico.audio.Musica
import cl.nikalbazu.diptico.audio.Sonidos
import cl.nikalbazu.diptico.audio.Vibracion
import cl.nikalbazu.diptico.data.Almacen
import cl.nikalbazu.diptico.data.Biblioteca
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.data.Descargas
import cl.nikalbazu.diptico.data.FichaInfo
import cl.nikalbazu.diptico.data.Fichas
import cl.nikalbazu.diptico.data.CarpetaJuegos
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.data.MediosJuego
import cl.nikalbazu.diptico.emu.App
import cl.nikalbazu.diptico.emu.Lanzador
import cl.nikalbazu.diptico.emu.Pantallas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Desinstalar un juego: funciones de Diptico movidas aquí desde Diptico.kt para que ese archivo no siga creciendo.
 * Se llaman igual que antes (Diptico.x()); fuera del paquete estado hay que importar cl.nikalbazu.diptico.estado.*.
 */

/** Y › Desinstalar: muestra qué se borra y cuánto espacio libera, y pide mantener A para confirmar. */
fun Diptico.desinstalarJuego() {
    val j = estado.value.seleccionado ?: return
    val consola = Consolas.porId(j.consolaId)
    cerrarPanel()
    // Ports y juegos de Android: los desinstala Android, con su propia ventana.
    if (j.portada?.scheme == "app" || consola?.sinEmulador == true) {
        try {
            app.startActivity(android.content.Intent(android.content.Intent.ACTION_DELETE, Uri.parse("package:${j.archivo}")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { avisar("No pude abrir la desinstalación de Android") }
        return
    }
    // Juegos de PC (Steam, Epic…): son accesos de Díptico; se quitan de la lista, no se borra nada del PC.
    if (j.uri.scheme == "file") {
        mostrarDialogo(Dialogo("quitar-pc", "¿Quitar ${j.titulo}?", "Se quita de tu estante. El juego sigue en GameNative o GameHub.", Icono.BORRAR, si = "Quitar", no = "Cancelar")) {
            quitarJuegoPC(j.clave)
        }
        return
    }
    avisar("Revisando los archivos de ${j.titulo}…")
    scope.launch {
        val lista = withContext(Dispatchers.IO) { try { archivosDelJuego(j) } catch (_: Exception) { emptyList() } }
        if (lista.isEmpty()) { avisar("No encontré los archivos de ${j.titulo}"); return@launch }
        val total = lista.sumOf { it.bytes }
        val espacio = if (total > 0) " y se liberarán ${tamanoBonito(total)}" else ""
        mostrarDialogo(
            Dialogo(
                "desinstalar-1", "¿Desinstalar ${j.titulo}?",
                "Se borrará de tu consola$espacio. También sus imágenes, notas y notas de voz de Díptico. Tus partidas guardadas del emulador se conservan.",
                Icono.BORRAR, lineas = lista.map { it.nombre }, si = "Seguir", no = "Cancelar", peligro = true,
            ),
        ) {
            mostrarDialogo(
                Dialogo(
                    "desinstalar-2", "Última confirmación",
                    "Mantén A hasta que se llene la barra para borrar ${j.titulo}. Esto no se puede deshacer.",
                    Icono.BORRAR, si = "Borrar", no = "Cancelar", mantener = true, peligro = true,
                ),
            ) { borrarJuego(j, lista) }
        }
    }
}


/** El juego y lo que va con él: los .bin de un .cue, los discos de un .m3u, las pistas de un .gdi, o su carpeta. */
internal fun Diptico.archivosDelJuego(j: Juego): List<PorBorrar> {
    val cr = app.contentResolver
    val arbolId = DocumentsContract.getTreeDocumentId(j.uri)
    val arbol = DocumentsContract.buildTreeDocumentUri(j.uri.authority, arbolId)
    fun info(docId: String): PorBorrar? {
        val u = DocumentsContract.buildDocumentUriUsingTree(arbol, docId)
        return try {
            cr.query(u, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { c ->
                if (!c.moveToFirst()) null else {
                    val carpeta = c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                    PorBorrar(u, c.getString(0) + if (carpeta) " (carpeta)" else "", if (carpeta) tamanoCarpeta(arbol, docId) else c.getLong(1))
                }
            }
        } catch (_: Exception) { null }
    }
    val principal = info(j.docId) ?: return emptyList()
    if (j.esCarpeta) return listOf(principal)
    val padre = j.docId.substringBeforeLast('/')
    val salida = linkedMapOf(j.docId to principal)
    // Archivos que nombra el juego por dentro (.cue, .m3u, .gdi), y los que nombran esos a su vez.
    val porRevisar = ArrayDeque(listOf(j.docId))
    while (porRevisar.isNotEmpty() && salida.size < 40) {
        val id = porRevisar.removeFirst()
        val ext = id.substringAfterLast('.', "").lowercase()
        if (ext !in setOf("cue", "m3u", "gdi")) continue
        val texto = try {
            cr.openInputStream(DocumentsContract.buildDocumentUriUsingTree(arbol, id))?.use { it.readBytes().take(65_536).toByteArray().toString(Charsets.UTF_8) }
        } catch (_: Exception) { null } ?: continue
        val nombres = when (ext) {
            "cue" -> Regex("FILE\\s+\"([^\"]+)\"", RegexOption.IGNORE_CASE).findAll(texto).map { it.groupValues[1] }.toList()
            "gdi" -> texto.lineSequence().drop(1).mapNotNull { l -> Regex("^\\s*\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+(\"[^\"]+\"|\\S+)").find(l)?.groupValues?.get(1)?.removeSurrounding("\"") }.toList()
            else -> texto.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
        }
        for (n in nombres) {
            // Solo archivos de la misma carpeta (o subcarpetas): nunca rutas hacia arriba.
            if (n.contains("..") || n.startsWith("/")) continue
            val hijo = "$padre/${n.replace('\\', '/')}"
            if (hijo in salida) continue
            info(hijo)?.let { salida[hijo] = it; porRevisar.add(hijo) }
        }
    }
    return salida.values.toList()
}


internal fun Diptico.tamanoCarpeta(arbol: Uri, docId: String, profundidad: Int = 0): Long {
    if (profundidad > 6) return 0L
    var total = 0L
    try {
        app.contentResolver.query(
            DocumentsContract.buildChildDocumentsUriUsingTree(arbol, docId),
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                total += if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) tamanoCarpeta(arbol, c.getString(0), profundidad + 1) else c.getLong(1)
            }
        }
    } catch (_: Exception) {}
    return total
}


internal fun Diptico.tamanoBonito(b: Long): String = when {
    b >= 1L shl 30 -> String.format(java.util.Locale.US, "%.1f GB", b / (1L shl 30).toDouble())
    b >= 1L shl 20 -> "${b / (1L shl 20)} MB"
    else -> "${(b / 1024).coerceAtLeast(1)} KB"
}


/** Borra los archivos y todo lo que Díptico guardaba del juego, y lo saca del estante. */
internal fun Diptico.borrarJuego(j: Juego, lista: List<PorBorrar>) {
    scope.launch {
        val fallidos = withContext(Dispatchers.IO) {
            lista.count { pb ->
                !(try { DocumentsContract.deleteDocument(app.contentResolver, pb.uri) } catch (_: Exception) { false })
            }
        }
        if (fallidos == lista.size) {
            avisar("No pude borrar ${j.titulo}. Elige otra vez tu carpeta de juegos (Menú › Biblioteca) para darle permiso a Díptico.")
            return@launch
        }
        // Lo de Díptico: imágenes descargadas, notas, nota de voz, mapa y su lugar en widgets y colecciones.
        withContext(Dispatchers.IO) {
            medios[j.clave]?.let { m -> listOfNotNull(m.portada, m.arte, m.logo).forEach { try { java.io.File(it).delete() } catch (_: Exception) {} } }
            medios = medios - j.clave
            almacen.guardarMedios(medios)
            try { Notas.borrar(app, j.clave) } catch (_: Exception) {}
            crudos = crudos.filter { it.clave != j.clave }
            almacen.guardarBiblioteca(crudos)
        }
        val datos = estado.value.datos - j.clave
        almacen.guardarDatos(datos)
        val widgets = estado.value.widgets.filter { it.tipo != "atajo-${j.clave}" }
        if (widgets.size != estado.value.widgets.size) guardarWidgets(widgets)
        val colecciones = estado.value.colecciones.mapValues { (_, c) -> c.copy(juegos = c.juegos - j.clave) }
        if (colecciones != estado.value.colecciones) guardarColecciones(colecciones)
        actualizar { it.copy(datos = datos, notasVoz = it.notasVoz - j.clave) }
        publicarJuegos()
        avisar(if (fallidos > 0) "${j.titulo} se borró, pero quedaron $fallidos archivos que no pude borrar" else "${j.titulo} desinstalado")
    }
}


/** Moverse en la ventana del juego: fila de botones grandes arriba, lista abajo. */
fun Diptico.moverRapido(dx: Int, dy: Int) {
    val e = estado.value
    val total = opcionesRapido().size
    if (total == 0) return
    val i = e.indice
    val n = when {
        i < PRIMARIAS_RAPIDO && dx != 0 -> (i + dx).coerceIn(0, PRIMARIAS_RAPIDO - 1)
        i < PRIMARIAS_RAPIDO && dy > 0 -> PRIMARIAS_RAPIDO.coerceAtMost(total - 1)
        i < PRIMARIAS_RAPIDO -> i
        dy < 0 -> if (i == PRIMARIAS_RAPIDO) 0 else i - 1
        dy > 0 -> (i + 1).coerceAtMost(total - 1)
        else -> i
    }
    if (n != i) actualizar { it.copy(indice = n) }
}


/** Emulador solo para este juego. */
internal fun Diptico.ciclarEmuladorJuego(clave: String, c: Consola) {
    val juego = estado.value.juegos.firstOrNull { it.clave == clave }
    val ops = Lanzador.opciones(app, c, juego?.tipo).distinctBy { it.id }
    if (ops.isEmpty()) {
        avisar("No hay emuladores de ${c.nombre} instalados. Te sugiero ${c.sugerido}.")
        return
    }
    val actual = estado.value.datos[clave]?.emulador
    val i = ops.indexOfFirst { it.id == actual }
    val siguiente = if (i == ops.size - 1) null else ops[i + 1].id
    cambiarDatos(clave) { it.copy(emulador = siguiente) }
}


internal fun Diptico.ciclarPantallaJuego(clave: String) {
    val datos = estado.value.datos.toMutableMap()
    val d = datos[clave] ?: DatosJuego()
    val siguiente = when (d.pantalla) { null -> "arriba"; "arriba" -> "abajo"; else -> null }
    datos[clave] = d.copy(pantalla = siguiente)
    almacen.guardarDatos(datos)
    actualizar { it.copy(datos = datos) }
}

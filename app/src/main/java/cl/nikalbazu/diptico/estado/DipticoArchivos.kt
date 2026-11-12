package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.estado.Diptico.Control
import cl.nikalbazu.diptico.estado.Diptico.InfoBateria
import cl.nikalbazu.diptico.estado.Diptico.ItemGaleria
import cl.nikalbazu.diptico.estado.Diptico.LanzadorPC
import cl.nikalbazu.diptico.estado.Diptico.OpcionBurbuja
import cl.nikalbazu.diptico.estado.Diptico.PorBorrar
import cl.nikalbazu.diptico.estado.Diptico.Regreso
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
 * Elegir archivos (imágenes y música): funciones de Diptico movidas aquí desde Diptico.kt para que ese archivo no siga creciendo.
 * Se llaman igual que antes (Diptico.x()); fuera del paquete estado hay que importar cl.nikalbazu.diptico.estado.*.
 */

internal fun Diptico.pedirImagen(para: String) = pedirArchivoPara(para, "image/*")


internal fun Diptico.pedirArchivoPara(para: String, tipo: String) {
    imagenVisual = null
    archivoPara = para
    pedirArchivo?.invoke(tipo) ?: avisar("No pude abrir el selector de archivos")
}


/** Al terminar de jugar: copia en segundo plano los guardados que cambiaron durante la partida. */
internal fun Diptico.respaldarPartida(clave: String, inicio: Long, duracion: Long) {
    val x = estado.value.extra
    if (!x.respaldoPartidas || x.carpetasPartidas.isEmpty() || duracion < 30) return
    val carpetas = x.carpetasPartidas.map(Uri::parse)
    scope.launch {
        val n = withContext(Dispatchers.IO) { runCatching { cl.nikalbazu.diptico.data.RespaldoPartidas.respaldar(app, clave, carpetas, inicio) }.getOrDefault(0) }
        if (n > 0) avisar(if (n == 1) "Partida respaldada" else "Partida respaldada ($n archivos)")
    }
}


internal fun Diptico.restaurarPartida(copia: cl.nikalbazu.diptico.data.RespaldoPartidas.Copia) {
    if (estado.value.sesion != null) { avisar("Cierra el juego antes de restaurar una partida"); return }
    scope.launch {
        val n = withContext(Dispatchers.IO) { runCatching { cl.nikalbazu.diptico.data.RespaldoPartidas.restaurar(app, copia) }.getOrDefault(0) }
        avisar(if (n > 0) "Partida restaurada ($n archivo${if (n == 1) "" else "s"})" else "No pude restaurar: revisa que la carpeta de partidas siga conectada")
    }
}


internal fun Diptico.abrirCopiasPartida(clave: String) {
    val j = estado.value.juegos.firstOrNull { it.clave == clave } ?: return
    scope.launch {
        val copias = withContext(Dispatchers.IO) { cl.nikalbazu.diptico.data.CopiasPartida.listar(app, clave) }
        val auto = withContext(Dispatchers.IO) { cl.nikalbazu.diptico.data.RespaldoPartidas.listar(app, clave) }
        val automaticas = auto.map { c ->
            val fecha = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(c.fecha))
            ItemGaleria("auto-${c.fecha}", "Respaldo automático · $fecha", "${c.archivos.size} archivo(s): " + c.archivos.take(3).joinToString(", ") { it.name }, false) {
                mostrarDialogo(Dialogo("restaurar-partida", "¿Volver a esta partida?",
                    "Los guardados de ese momento vuelven a su carpeta y reemplazan los de ahora. Hazlo con el juego cerrado.",
                    Icono.COPIAR, si = "Restaurar", no = "Cancelar")) { restaurarPartida(c) }
            }
        } + if (estado.value.extra.carpetasPartidas.isEmpty()) listOf(ItemGaleria("auto-carpeta", "Respaldar solo al terminar de jugar",
            "Elige la carpeta donde tu emulador guarda las partidas y Díptico las copia sola.", false) { pedirCarpeta?.invoke(Carpeta.PARTIDAS) }) else emptyList()
        opcionesCopiasPartida = automaticas + listOf(ItemGaleria("copia-nueva", "Guardar copia .srm",
            "Selecciona un guardado. Se conservan las últimas tres copias.", false) { pedirArchivoPara("partida-srm:$clave", "*/*") }) +
            copias.mapIndexed { i, archivo ->
                val fecha = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(archivo.lastModified()))
                ItemGaleria(archivo.name, "Recuperar copia ${i + 1}", fecha, false) {
                    mostrarDialogo(Dialogo("exportar-srm", "¿Recuperar esta copia?",
                        "Elige dónde guardar el archivo .srm recuperado. El guardado del emulador queda en su ubicación actual.",
                        Icono.COPIAR, si = "Elegir destino", no = "Cancelar")) {
                        copiaParaExportar = archivo
                        val base = archivoParaJugar(j, estado.value).archivo.substringBeforeLast('.').replace('/', '_')
                        pedirExportarPartida?.invoke("$base.srm")
                    }
                }
            }
        abrirGaleria("copias_srm")
    }
}


fun Diptico.exportarPartidaElegida(uri: Uri?) {
    val archivo = copiaParaExportar
    copiaParaExportar = null
    if (uri == null || archivo == null) return
    scope.launch {
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                requireNotNull(app.contentResolver.openOutputStream(uri, "wt")).use { salida ->
                    archivo.inputStream().use { it.copyTo(salida) }
                }
            }.isSuccess
        }
        avisar(if (ok) "Copia .srm recuperada" else "No pude guardar la copia")
    }
}


fun Diptico.imagenCaptura(e: Estado, id: String): Uri? {
    if (e.galeria == "captura_acciones") return capturaPara?.second?.let(Uri::parse)
    if (e.galeria != "capturas_juego") return null
    val clave = e.seleccionado?.clave ?: return null
    val i = id.removePrefix("captura-").toIntOrNull() ?: return null
    return e.datos[clave]?.capturas?.getOrNull(i)?.let(Uri::parse)
}


internal fun Diptico.accionesCaptura(clave: String, ruta: String) {
    capturaPara = clave to ruta
    abrirGaleria("captura_acciones")
}


internal fun Diptico.compartirCaptura(ruta: String) {
    val uri = Uri.parse(ruta)
    val enviar = Intent(Intent.ACTION_SEND).setType("image/*")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .also { it.clipData = android.content.ClipData.newRawUri("Captura", uri) }
    abrirIntent(Intent.createChooser(enviar, "Compartir captura"), "No pude compartir la captura")
}


internal fun Diptico.importarListaEsDe(uri: Uri) {
    val e = estado.value
    scope.launch {
        val previa = withContext(Dispatchers.IO) {
            runCatching {
                val entradas = app.contentResolver.openInputStream(uri)?.use(cl.nikalbazu.diptico.data.ImportarEsDe::leer)
                    ?: error("Archivo vacío")
                cl.nikalbazu.diptico.data.ImportarEsDe.comparar(entradas, e.juegos, e.datos)
            }.getOrNull()
        }
        if (previa == null) { avisar("No pude leer el gamelist.xml de ES-DE"); return@launch }
        if (previa.cambios.isEmpty()) {
            avisar("Sin cambios · ${previa.sinCoincidencia} juegos sin coincidencia única")
            return@launch
        }
        mostrarDialogo(Dialogo("importar-es-de", "Importar lista de ES-DE",
            "${previa.cambios.size} juegos se pueden actualizar. Se omiten ${previa.sinCoincidencia} sin coincidencia única.",
            Icono.DESCARGA, si = "Importar", no = "Cancelar",
            lineas = previa.cambios.take(6).map { it.titulo ?: it.juego.titulo })) {
            val actual = estado.value
            val claves = actual.juegos.mapTo(HashSet()) { it.clave }
            val datos = actual.datos.toMutableMap()
            for (cambio in previa.cambios) {
                val clave = cambio.juego.clave
                if (clave !in claves) continue
                val previo = datos[clave] ?: DatosJuego()
                datos[clave] = previo.copy(titulo = previo.titulo ?: cambio.titulo, favorito = previo.favorito || cambio.favorito)
            }
            almacen.guardarDatos(datos)
            actualizar { it.copy(datos = datos) }
            publicarJuegos()
            avisar("Lista de ES-DE importada")
        }
    }
}


fun Diptico.archivoElegido(uri: Uri) {
    if (archivoPara.startsWith("portada_carpeta:")) {
        val id = archivoPara.removePrefix("portada_carpeta:")
        archivoPara = ""
        scope.launch {
            val valida = withContext(Dispatchers.IO) {
                runCatching {
                    require(uri.scheme == "content")
                    requireNotNull(cl.nikalbazu.diptico.ui.Imagenes.cargar(app, uri, 512))
                    app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }.isSuccess
            }
            if (valida) {
                cambiarConsolaPropia(id) { it.copy(portadaCarpeta = uri.toString()) }
                avisar("Portada de carpeta guardada")
            } else avisar("No pude abrir o conservar esa imagen")
        }
        return
    }
    if (archivoPara == "temaJson") {
        archivoPara = ""
        importarTemas { TiendaTemas.archivo(app, uri) }
        return
    }
    if (archivoPara.startsWith("partida-srm:")) {
        val clave = archivoPara.removePrefix("partida-srm:")
        archivoPara = ""
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { cl.nikalbazu.diptico.data.CopiasPartida.guardar(app, clave, uri) }.isSuccess
            }
            if (ok) {
                avisar("Copia .srm guardada")
                abrirCopiasPartida(clave)
            } else avisar("Elige un guardado .srm legible de hasta 16 MB")
        }
        return
    }
    if (archivoPara.startsWith("captura:")) {
        val clave = archivoPara.removePrefix("captura:")
        archivoPara = ""
        try {
            require(uri.scheme == "content")
            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) { avisar("No pude conservar el acceso a esa captura"); return }
        val datos = estado.value.datos.toMutableMap()
        val d = datos[clave] ?: DatosJuego()
        if (d.capturas.size >= 24 && uri.toString() !in d.capturas) { avisar("La galería admite 24 capturas por juego"); return }
        datos[clave] = d.copy(capturas = (listOf(uri.toString()) + d.capturas).distinct())
        almacen.guardarDatos(datos)
        actualizar { it.copy(datos = datos) }
        avisar("Captura agregada")
        return
    }
    if (archivoPara == "es-de") {
        archivoPara = ""
        importarListaEsDe(uri)
        return
    }
    imagenVisual?.let { (id, tipo) ->
        imagenVisual = null
        try { app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        val v = estado.value.experiencia
        val c = v.colecciones[id] ?: PresentacionColeccion()
        val nuevo = when (tipo) { "musica" -> c.copy(musica = uri.toString()); "logo" -> c.copy(logo = uri.toString()); else -> c.copy(fondo = uri.toString()) }
        guardarExperiencia(v.copy(colecciones = v.colecciones + (id to nuevo)))
        return
    }

    if (archivoPara == "modeloIA") {
        archivoPara = ""
        trabajo("ialocal", "Copiando el modelo de IA")
        scope.launch {
            val error = withContext(Dispatchers.IO) { IALocal.usarPropio(app, uri) }
            terminarTrabajo("ialocal")
            avisar(error ?: "¡Listo! DIPPO usará tu modelo")
            actualizar { it.copy(iaLocalLista = IALocal.listo(app)) }
        }
        return
    }
    try {
        app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: Exception) {
    }
    val u = uri.toString()
    when {
        archivoPara == "perfilFoto" -> {
            cambiarPerfil { it.copy(foto = u) }
            avisar("¡Listo! Esta es tu foto")
        }
        archivoPara == "fondoInicio" -> {
            almacen.guardarTexto("fondoInicio", u)
            actualizar { it.copy(fondoInicio = u) }
            avisar("Fondo del inicio cambiado")
        }
        archivoPara == "widgetImagen" || archivoPara == "widgetVideo" -> {
            val video = archivoPara == "widgetVideo"
            val w = Widget((if (video) "video-" else "imagen-") + System.currentTimeMillis(), 0, 0, 2, 2, false, estado.value.paginaInicio, u, estilo = "sinfondo")
            guardarWidgets(estado.value.widgets + w)
            val k = estado.value.widgets.size - 1
            val celda = celdaPendiente
            celdaPendiente = null
            if (celda != null) ponerEnCelda(k, celda.first, celda.second) else alternarWidget(k)
            avisar(if (video) "Video agregado a tu inicio" else "Imagen agregada a tu inicio")
        }
        archivoPara.startsWith("widget:") -> {
            val tipo = archivoPara.removePrefix("widget:")
            guardarWidgets(estado.value.widgets.map { if (it.tipo == tipo) it.copy(uri = u) else it })
            avisar("Imagen cambiada")
        }
        archivoPara.startsWith("consola_") -> {
            val (que, id) = archivoPara.removePrefix("consola_").split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            cambiarConsolaPropia(id) {
                when (que) { "hero" -> it.copy(hero = u); "logo" -> it.copy(logo = u); "musica" -> it.copy(musica = u); else -> it }
            }
            avisar("¡Listo! Cambio guardado")
        }
        archivoPara.startsWith("mapa:") -> {
            val clave = archivoPara.removePrefix("mapa:")
            val m = estado.value.mapas + (clave to MapaJuego(u))
            guardarMapas(m)
            actualizar { it.copy(marcandoMapa = clave, pinX = 0.5f, pinY = 0.5f) }
            avisar("Ahora marca dónde quedaste")
        }
        archivoPara.startsWith("icono:") -> {
            val id = archivoPara.removePrefix("icono:")
            val m = estado.value.iconosPropios + (id to u)
            almacen.guardarTexto("iconosPropios", escribirMapa(m))
            actualizar { it.copy(iconosPropios = m) }
            avisar("Ícono cambiado")
        }
        else -> {
            val clave = estado.value.fichaDe ?: return
            cambiarDatos(clave) {
                when (archivoPara) {
                    "portada" -> it.copy(portada = u)
                    "arte" -> it.copy(arte = u)
                    "logo" -> it.copy(logo = u)
                    "musica" -> it.copy(musica = u)
                    else -> it
                }
            }
            avisar("¡Listo! Cambio guardado")
        }
    }
}

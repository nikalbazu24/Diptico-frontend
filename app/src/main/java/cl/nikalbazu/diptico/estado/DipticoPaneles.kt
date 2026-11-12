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
 * Paneles ----------: funciones de Diptico movidas aquí desde Diptico.kt para que ese archivo no siga creciendo.
 * Se llaman igual que antes (Diptico.x()); fuera del paquete estado hay que importar cl.nikalbazu.diptico.estado.*.
 */


fun Diptico.abrirRapido() {
    if (estado.value.seleccionado == null) return
    actualizar { it.copy(panel = Panel.RAPIDO, indice = 0) }
}


fun Diptico.abrirPaso() {
    val clave = estado.value.seleccionado?.clave ?: return
    actualizar { it.copy(panel = Panel.PASO, borrador = it.datos[clave]?.paso ?: "", pregunta = false, teclaFila = 2, teclaCol = 0, simbolos = false, cursor = -1, ancla = null, mayus = it.datos[clave]?.paso.isNullOrEmpty()) }
}


fun Diptico.abrirMenu(seccion: Seccion? = null) {
    if (estado.value.panel != Panel.MENU) Sonidos.abrir()
    actualizar {
        it.copy(
            panel = Panel.MENU, seccion = seccion ?: it.seccion.takeIf { s -> esGrupoAjustes(s) } ?: Seccion.G_ESTILO,
            enOpciones = seccion != null, indice = 0,
        )
    }
}


/** Lista de apps instaladas para marcar cuáles son ports. */
fun Diptico.abrirPorts(destino: String = Consolas.PORTS) {
    actualizar { it.copy(panel = Panel.PORTS, indice = 0, cargandoApps = true, appsDestino = destino) }
    scope.launch {
        val todas = withContext(Dispatchers.IO) { try { Lanzador.apps(app) } catch (e: Exception) { emptyList() } }
        // Para juegos de Android, primero los que Android marca como juego.
        val lista = if (destino == Consolas.ANDROID) todas.filter { it.esJuego } + todas.filter { !it.esJuego } else todas
        // Primero las ya elegidas, después el resto.
        val elegidas = (if (destino == Consolas.ANDROID) estado.value.android else estado.value.ports).toSet()
        val orden = lista.filter { it.paquete in elegidas } + lista.filter { it.paquete !in elegidas }
        actualizar { it.copy(apps = orden, cargandoApps = false) }
    }
}


fun Diptico.alternarPort(paquete: String) {
    val e = estado.value
    if (e.appsDestino == Consolas.ANDROID) {
        val nuevo = if (paquete in e.android) e.android - paquete else e.android + paquete
        almacen.android = nuevo
        actualizar { it.copy(android = nuevo) }
    } else {
        val nuevo = if (paquete in e.ports) e.ports - paquete else e.ports + paquete
        almacen.ports = nuevo
        actualizar { it.copy(ports = nuevo) }
    }
    publicarJuegos()
}


/** Agrega los juegos de Android que el sistema reconoce como juego. */
fun Diptico.detectarAndroid(silencioso: Boolean = false) {
    trabajo("android", "Buscando juegos de Android")
    scope.launch {
        val juegos = withContext(Dispatchers.IO) { try { Lanzador.juegosAndroid(app).map { it.paquete } } catch (e: Exception) { emptyList() } }
        val antes = estado.value.android
        val nuevo = (antes + juegos.filter { it !in antes && it !in estado.value.ports })
        almacen.android = nuevo
        actualizar { it.copy(android = nuevo) }
        publicarJuegos()
        terminarTrabajo("android")
        if (!silencioso) {
            val n = nuevo.size - antes.size
            avisar(if (n > 0) "Agregué $n juegos de Android" else "No encontré juegos de Android nuevos")
        }
    }
}


fun Diptico.abrirClave() = abrirTexto("sgdb", estado.value.claveSgdb)


/** Abre el cuadro de texto para [para] ("sgdb", "ia_clave_gemini", "titulo", "notas"). */
fun Diptico.abrirTexto(para: String, inicial: String = "") {
    if (estado.value.panel != Panel.CLAVE) claveDesde = regresoActual()
    actualizar { it.copy(panel = Panel.CLAVE, textoPara = para, borrador = inicial, teclaFila = 2, teclaCol = 0, simbolos = para == "perfil_nacimiento", cursor = -1, ancla = null, mayus = inicial.isEmpty() && para != "sgdb" && !para.startsWith("ia_clave") && !para.startsWith("ra_") && !para.startsWith("ras_") && para != "tema_enlace" && para != "discord_webhook" && para != "discord_app" && para != "discord_secreto" && para != "perfil_nacimiento") }
    // Para las claves: el teléfono puede mandarla directo (QR con la página de la consola en tu Wi-Fi).
    val oficial = urlClave(para)
    if (oficial != null && para != "ra_usuario") {
        val titulo = when (para) {
            "ia_clave_gemini" -> "Clave de Gemini"; "ia_clave_openai" -> "Clave de ChatGPT"
            "sgdb" -> "Clave de SteamGridDB"
            "discord_webhook" -> "Enlace de Discord"
            "discord_app" -> "Application ID de Discord"
            "discord_secreto" -> "Clave secreta de Discord"
            else -> tituloRaspador(para) ?: "Clave de RetroAchievements"
        }
        val url = Enlace.iniciar(titulo, oficial) { clave -> recibirClaveRemota(para, clave) }
        actualizar { it.copy(enlaceUrl = url) }
    } else if (estado.value.enlaceUrl != null) {
        Enlace.detener(); actualizar { it.copy(enlaceUrl = null) }
    }
}


/** Llegó una clave desde el teléfono: se guarda como si se hubiera escrito aquí. */
internal fun Diptico.recibirClaveRemota(para: String, clave: String) {
    val e = estado.value
    if (e.panel != Panel.CLAVE || e.textoPara != para) { Enlace.detener(); return }
    actualizar { it.copy(borrador = clave, enlaceUrl = null) }
    Sonidos.aviso()
    guardarClave()
    avisar("¡Clave recibida desde tu teléfono! Ya quedó activa")
}


fun Diptico.cerrarPanel() {
    if (estado.value.desplegable >= 0) actualizar { it.copy(extra = it.extra.copy(desplegable = -1)) }
    if (estado.value.extra.subOpcion >= 0) actualizar { it.copy(extra = it.extra.copy(subOpcion = -1, subSel = 0)) }
    if (estado.value.appsMenu != null || estado.value.appsMoviendo != null) { cerrarOpcionesApp(); return }
    if (estado.value.coleccionVisual != null) actualizar { it.copy(coleccionVisual = null) }
    val e = estado.value
    cadenaRaspador = emptyList()
    if (e.panel == Panel.CLAVE && e.enlaceUrl != null) { Enlace.detener(); actualizar { it.copy(enlaceUrl = null) } }
    // Lo abriste desde Ajustes: B vuelve a esa misma opción (y a su subventana), no al inicio ni al comienzo del menú.
    if (e.panel != Panel.MENU && !(e.panel == Panel.GALERIA && e.decoracionAbierta != null) && volverAlMenu()) return
    // Desde la clave, se vuelve a donde estabas (menú o tutorial).
    if (e.panel == Panel.CLAVE && (e.textoPara == "titulo" || e.textoPara == "notas")) {
        actualizar { it.copy(panel = Panel.FICHA) }
        return
    }
    if (e.panel == Panel.CLAVE && e.textoPara.startsWith("ia_clave") && claveVieneDeGaleria) {
        claveVieneDeGaleria = false
        abrirGaleria("ia")
        return
    }
    if (e.panel == Panel.CLAVE && (e.textoPara in setOf("asistente", "bisa_paso") || e.textoPara.startsWith("ia_clave") && claveParaAsistente)) {
        claveParaAsistente = false
        actualizar { it.copy(panel = Panel.ASISTENTE, indice = 0) }
        return
    }
    if (e.panel == Panel.CLAVE && e.textoPara == "consola_nombre") {
        actualizar { it.copy(panel = Panel.EDITAR_CONSOLA) }
        return
    }
    if (e.panel == Panel.EDITAR_CONSOLA) {
        actualizar { it.copy(panel = if (editarDesdeMenu) Panel.MENU else Panel.NINGUNO, seccion = Seccion.CONSOLAS, enOpciones = true, indice = 0) }
        return
    }
    if (e.panel == Panel.CLAVE && e.textoPara == "buscar_ajuste") {
        actualizar { it.copy(panel = Panel.MENU, seccion = Seccion.ESENCIAL, enOpciones = false) }
        return
    }
    if (e.panel == Panel.CLAVE && e.textoPara == "buscar_imagen") {
        actualizar { it.copy(panel = Panel.IMAGENES, indice = 0) }
        return
    }
    // Escribiendo un dato del perfil desde el editor: se vuelve al editor (no a los ajustes).
    if (e.panel == Panel.CLAVE && e.extra.edicionPerfil != null && (e.textoPara == "nombre" || e.textoPara.startsWith("perfil_"))) {
        claveDesde = null
        actualizar { it.copy(panel = Panel.NINGUNO) }
        return
    }
    if (e.panel == Panel.CLAVE && e.configurado) {
        if (volverA(claveDesde.also { claveDesde = null })) return
        val s = when {
            e.textoPara.startsWith("ra_") || e.textoPara.startsWith("ras_") || e.textoPara.startsWith("discord") || e.textoPara == "sgdb" -> Seccion.LOGROS
            e.textoPara == "nombre" || e.textoPara.startsWith("perfil_") -> { actualizar { it.copy(panel = Panel.NINGUNO) }; editarPerfil(); return }
            e.textoPara.startsWith("ia") && e.seccion == Seccion.ASISTENTE -> Seccion.ASISTENTE
            e.textoPara.startsWith("ia") -> Seccion.TEMAS
            else -> Seccion.PORTADAS
        }
        actualizar { it.copy(panel = Panel.MENU, seccion = s, enOpciones = true, indice = 0) }
        return
    }
    if (e.panel == Panel.WIDGET) {
        actualizar { it.copy(panel = Panel.NINGUNO, widgetAbierto = -1, indice = 0) }
        return
    }
    if (e.panel == Panel.ENCUADRE) {
        val i = opcionesFicha().indexOfFirst { it.titulo == if (e.encuadreDe.endsWith("|arte")) "Encuadrar la imagen grande" else "Encuadrar la portada" }
        actualizar { it.copy(panel = Panel.FICHA, indice = i.coerceAtLeast(0), encuadreDe = "") }
        return
    }
    if (e.panel == Panel.IMAGENES) {
        busquedaImagenes?.cancel()
        if (e.candidatoPara.startsWith("c_")) {
            val i = opcionesEditarConsola().indexOfFirst { it.titulo == when (e.candidatoPara) { "c_icono" -> "Ícono"; "c_portada" -> "Imagen del ícono de carpeta"; "c_logo" -> "Logo"; else -> "Imagen grande (hero)" } }
            actualizar { it.copy(panel = Panel.EDITAR_CONSOLA, indice = i.coerceAtLeast(0), candidatoPara = "") }
            return
        }
        actualizar { it.copy(panel = Panel.FICHA, indice = 0) }
        return
    }
    if (e.panel == Panel.ESTUDIO) {
        terminarEdicion()
        return
    }
    if (e.panel == Panel.GALERIA) {
        if (e.galeria == "decoraciones" && e.decoracionAbierta != null) {
            val indiceTema = temasDecoracion(e).indexOfFirst { it.id == e.decoracionAbierta }.coerceAtLeast(0)
            actualizar { it.copy(decoracionAbierta = null, indice = indiceTema) }
            return
        }
        // Se vuelve a donde se abrió (la ficha del juego, la ventana, la consola o la misma sección del menú).
        if (volverA(galeriaDesde.also { galeriaDesde = null })) return
        val sec = when (e.galeria) { "musica" -> Seccion.MUSICA; "ia" -> Seccion.ASISTENTE; else -> Seccion.TEMAS }
        actualizar { it.copy(panel = Panel.MENU, seccion = sec, enOpciones = true, indice = 0) }
        return
    }
    if (e.panel == Panel.BUSCAR && e.filtro == "ocultos") {
        salirDeOcultos(cerrarBusqueda = true)
        return
    }
    if (e.panel == Panel.CARPETAS && e.consolaPara >= 0) {
        actualizar { it.copy(consolaPara = -1, indice = e.consolaPara) }
        return
    }
    if (e.panel == Panel.CARPETAS && e.configurado) {
        actualizar { it.copy(panel = Panel.MENU, seccion = Seccion.BIBLIOTECA, enOpciones = true, indice = 0) }
        return
    }
    if (e.panel == Panel.PORTS && e.configurado) {
        actualizar { it.copy(panel = Panel.MENU, seccion = Seccion.BIBLIOTECA, enOpciones = true, indice = 0) }
        return
    }
    actualizar { it.copy(panel = Panel.NINGUNO, pregunta = false) }
}

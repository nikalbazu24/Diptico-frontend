package cl.nikalbazu.diptico.estado

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

/**
 * Traduce los botones y sticks del control a acciones de Díptico.
 *
 * Inicio: A juega (mantener = ventana del juego), Y ventana del juego, X ficha, B vuelve,
 * Select cambia inicio de carátulas/widgets (mantener = intercambiar pantallas), Start menú (mantener = centro de control),
 * L1/R1 consola, L2/R2 saltan de letra, L3 buscar, R3 cambia la vista. Stick izquierdo = cruceta, stick derecho = consola y desplazamiento.
 */
object Teclas {
    private var aLargo = false
    private var selectLargo = false
    private var startLargo = false

    private fun esA(code: Int) = code == KeyEvent.KEYCODE_BUTTON_A || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
    private fun esB(code: Int) = code == KeyEvent.KEYCODE_BUTTON_B || code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE

    private val botonesControl = setOf(
        KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT,
        KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR,
    )

    private val cruceta = setOf(
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
    )

    /** Dirección de la cruceta: (dx, dy). */
    private fun dir(code: Int): Pair<Int, Int>? = when (code) {
        KeyEvent.KEYCODE_DPAD_LEFT -> -1 to 0
        KeyEvent.KEYCODE_DPAD_RIGHT -> 1 to 0
        KeyEvent.KEYCODE_DPAD_UP -> 0 to -1
        KeyEvent.KEYCODE_DPAD_DOWN -> 0 to 1
        else -> null
    }

    fun manejar(ev: KeyEvent): Boolean {
        val code = ev.keyCode
        // El volumen siempre lo maneja Android (así baja también la música de Díptico).
        if (code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN || code == KeyEvent.KEYCODE_VOLUME_MUTE) return false
        val e = Diptico.estado.value
        val abajo = ev.action == KeyEvent.ACTION_DOWN
        val arriba = ev.action == KeyEvent.ACTION_UP

        // Reloj grande: cualquier botón lo cierra.
        if (e.relojGrande && (esA(code) || esB(code) || code in botonesControl)) {
            if (arriba) Diptico.alternarReloj()
            return true
        }

        // Animación de entrada: cualquier botón la salta.
        if (e.intro) {
            if (arriba) Diptico.terminarIntro()
            return true
        }

        // Bienvenida al encender: A sigue jugando, cualquier otro botón la cierra.
        if (e.bienvenida > 0L && !e.bienvenidaBreve && e.sesion == null) {
            if (arriba) { if (esA(code)) Diptico.seguirDesdeBienvenida() else Diptico.cerrarBienvenida() }
            return true
        }
        if (e.bienvenida > 0L && e.bienvenidaBreve && arriba) Diptico.cerrarBienvenida()

        // Sonidos de navegación.
        if (e.sesion == null && e.panel != Panel.PASO && e.panel != Panel.CLAVE) {
            if (abajo && code in cruceta) cl.nikalbazu.diptico.audio.Sonidos.mover()
            if (abajo && ev.repeatCount == 0 && (code == KeyEvent.KEYCODE_BUTTON_L1 || code == KeyEvent.KEYCODE_BUTTON_R1)) {
                if (e.panel == Panel.NINGUNO) cl.nikalbazu.diptico.audio.Sonidos.consola() else cl.nikalbazu.diptico.audio.Sonidos.pagina()
            }
            if (arriba) when {
                esA(code) && e.conTeclado -> cl.nikalbazu.diptico.audio.Sonidos.tecla()
                esA(code) && e.panel == Panel.MENU && e.enOpciones && Diptico.esInterruptor(e) != null ->
                    cl.nikalbazu.diptico.audio.Sonidos.interruptor(Diptico.esInterruptor(e) == false)
                esA(code) -> cl.nikalbazu.diptico.audio.Sonidos.aceptar()
                esB(code) -> cl.nikalbazu.diptico.audio.Sonidos.atras()
                code == KeyEvent.KEYCODE_BUTTON_X || code == KeyEvent.KEYCODE_BUTTON_Y || code == KeyEvent.KEYCODE_BUTTON_START -> cl.nikalbazu.diptico.audio.Sonidos.abrir()
            }
        }

        // Escribiendo (próximo paso o un texto): el teclado manda, salvo B (cerrar) y Start (guardar).
        if (e.conTeclado) {
            // Teclado de Díptico: cruceta elige, A escribe, B borra (o cierra si no hay nada), X espacio, Y mayúsculas,
            // L1/R1 mueven el cursor, L2 selecciona, R2 pega, Start listo.
            if (abajo) {
                val d = dir(code)
                when {
                    d != null -> { Diptico.moverTecla(d.first, d.second); cl.nikalbazu.diptico.audio.Sonidos.mover() }
                    code == KeyEvent.KEYCODE_BUTTON_L1 -> Diptico.moverCursor(-1)
                    code == KeyEvent.KEYCODE_BUTTON_R1 -> Diptico.moverCursor(1)
                }
                return true
            }
            if (arriba) when {
                esA(code) -> Diptico.pulsarTecla()
                code == KeyEvent.KEYCODE_BUTTON_B || code == KeyEvent.KEYCODE_BACK ->
                    if (e.borrador.isEmpty()) Diptico.cerrarPanel() else Diptico.borrarLetra()
                code == KeyEvent.KEYCODE_BUTTON_X -> Diptico.escribir(" ")
                code == KeyEvent.KEYCODE_BUTTON_Y -> Diptico.pulsarValor("⇧")
                code == KeyEvent.KEYCODE_BUTTON_L2 -> Diptico.alternarSeleccion()
                code == KeyEvent.KEYCODE_BUTTON_R2 -> Diptico.pegar()
                code == KeyEvent.KEYCODE_BUTTON_START -> Diptico.terminarTexto()
                code == KeyEvent.KEYCODE_BUTTON_SELECT -> Diptico.pulsarValor("#")
            }
            return true
        }
        if (e.panel == Panel.PASO || e.panel == Panel.CLAVE) {
            if (code == KeyEvent.KEYCODE_BUTTON_B || code == KeyEvent.KEYCODE_BACK) {
                if (arriba) Diptico.cerrarPanel()
                return true
            }
            if (code == KeyEvent.KEYCODE_BUTTON_START) {
                if (arriba) { if (e.panel == Panel.PASO) Diptico.guardarPaso() else Diptico.guardarClave() }
                return true
            }
            return false
        }

        // Tutorial guiado (sin paneles abiertos).
        if (!e.configurado && e.panel == Panel.NINGUNO) {
            if (abajo) {
                val d = dir(code)
                if (d != null) {
                    Diptico.moverIndice(if (d.first + d.second < 0) -1 else 1)
                    Diptico.marcarProbado("cruceta")
                    return true
                }
                return esA(code) || esB(code) || code in botonesControl
            }
            if (arriba) {
                when {
                    esA(code) -> Diptico.aceptar()
                    esB(code) -> Diptico.atras()
                    code == KeyEvent.KEYCODE_BUTTON_L1 || code == KeyEvent.KEYCODE_BUTTON_R1 -> Diptico.marcarProbado("gatillos")
                    code == KeyEvent.KEYCODE_BUTTON_Y -> Diptico.marcarProbado("Y")
                    code == KeyEvent.KEYCODE_BUTTON_START -> Diptico.marcarProbado("Start")
                    code in botonesControl -> Unit
                    else -> return false
                }
                return true
            }
            return false
        }

        // Select: en el inicio cambia entre carátulas y widgets; en menús y ventanas intercambia las pantallas.
        // Mantenerlo siempre intercambia las pantallas.
        if (code == KeyEvent.KEYCODE_BUTTON_SELECT && e.sesion == null) {
            if (abajo) {
                if (ev.repeatCount == 0) selectLargo = false
                else if (!selectLargo && ev.eventTime - ev.downTime > 550) {
                    selectLargo = true
                    Diptico.intercambiar()
                }
            }
            if (arriba) {
                if (!selectLargo) {
                    when {
                        e.panel == Panel.ESTUDIO -> Diptico.siguienteWidget(1)
                        e.panel == Panel.NINGUNO && !e.editandoWidgets -> Diptico.alternarInicio()
                        else -> Diptico.intercambiar()
                    }
                }
                selectLargo = false
            }
            return true
        }

        // Juego en curso y pantalla atenuada: cualquier botón la despierta.
        if (e.sesion != null && !e.despierta && e.panel == Panel.NINGUNO) {
            if (arriba) Diptico.despertar()
            return true
        }

        // Encuadrar una imagen: la cruceta la corre (se puede mantener), L1/R1 acercan, X centra, A guarda.
        if (e.panel == Panel.ENCUADRE) {
            if (abajo) {
                dir(code)?.let { (dx, dy) -> Diptico.moverEncuadre(-dx * 0.05f, -dy * 0.05f); return true }
                when (code) {
                    KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_L2 -> { Diptico.acercarEncuadre(-0.05f); return true }
                    KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_R2 -> { Diptico.acercarEncuadre(0.05f); return true }
                }
            }
            if (arriba) when {
                esA(code) || code == KeyEvent.KEYCODE_BUTTON_START -> Diptico.guardarEncuadre()
                esB(code) -> Diptico.cerrarPanel()
                code == KeyEvent.KEYCODE_BUTTON_X -> Diptico.centrarEncuadre()
            }
            return true
        }

        // Estudio de widgets (o el inicio en modo edición sin el panel abierto).
        if (e.panel == Panel.ESTUDIO || (e.editandoWidgets && e.panel == Panel.NINGUNO)) return estudio(ev, e)

        // Paneles con su propia forma de moverse.
        when (e.panel) {
            Panel.APPS, Panel.EDITAR_CONSOLA, Panel.FICHA, Panel.TRABAJOS, Panel.RAPIDO, Panel.BUSCAR, Panel.CENTRO, Panel.CARPETAS, Panel.PORTS, Panel.ESTADISTICAS, Panel.IMAGENES, Panel.ASISTENTE, Panel.GALERIA ->
                return panelEnGrilla(ev, e)
            Panel.MENU -> return menu(ev, e)
            else -> Unit
        }

        val hayPanel = e.panel != Panel.NINGUNO

        // Modo Sistemas, eligiendo consola: la cruceta y L1/R1 cambian de consola, A entra, Y elige un juego al azar.
        if (!hayPanel && e.sesion == null && e.vistaConsolas == "sistemas" && e.enSistemas) {
            if (abajo) {
                dir(code)?.let { (dx, dy) -> Diptico.cambiarConsola(if (dx + dy < 0) -1 else 1); return true }
                if ((code == KeyEvent.KEYCODE_BUTTON_L1 || code == KeyEvent.KEYCODE_BUTTON_R1) && ev.repeatCount == 0) {
                    Diptico.cambiarConsola(if (code == KeyEvent.KEYCODE_BUTTON_L1) -1 else 1); return true
                }
                if (esA(code) || esB(code) || code == KeyEvent.KEYCODE_BUTTON_Y || code == KeyEvent.KEYCODE_BUTTON_X) return true
            }
            if (arriba) {
                when {
                    esA(code) -> { Diptico.entrarSistema(); return true }
                    esB(code) -> return true
                    code == KeyEvent.KEYCODE_BUTTON_X -> { Diptico.abrirEditarConsola(e.consolaSel); return true }
                    code == KeyEvent.KEYCODE_BUTTON_Y -> { Diptico.sorprendeme(); Diptico.entrarSistema(); return true }
                }
            }
        }

        if (abajo) {
            when (code) {
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP -> {
                    if (!hayPanel) Diptico.moverJuego(if (code == KeyEvent.KEYCODE_DPAD_UP && (e.vista == "cuadricula" || e.vista == "lista")) -columnasEstante(e) else -1)
                    else Diptico.moverIndice(-1)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (!hayPanel) Diptico.moverJuego(if (code == KeyEvent.KEYCODE_DPAD_DOWN && (e.vista == "cuadricula" || e.vista == "lista")) columnasEstante(e) else 1)
                    else Diptico.moverIndice(1)
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_L1 -> { if (!hayPanel && ev.repeatCount == 0) Diptico.cambiarConsola(-1); return true }
                KeyEvent.KEYCODE_BUTTON_R1 -> { if (!hayPanel && ev.repeatCount == 0) Diptico.cambiarConsola(1); return true }
                KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2 -> {
                    val d = if (code == KeyEvent.KEYCODE_BUTTON_L2) -1 else 1
                    // En el inicio con varias páginas, L2/R2 cambian de página; si no, saltan de letra.
                    if (!hayPanel && ev.repeatCount == 0) { if (e.inicio == "widgets" && e.paginas > 1) Diptico.cambiarPagina(d) else Diptico.saltarLetra(d) }
                    return true
                }
            }
            if (esA(code)) {
                if (ev.repeatCount == 0) {
                    aLargo = false
                } else if (!aLargo && !hayPanel && ev.eventTime - ev.downTime > 550) {
                    aLargo = true
                    Diptico.abrirRapido()
                }
                return true
            }
            if (code == KeyEvent.KEYCODE_BUTTON_SELECT) {
                if (ev.repeatCount == 0) selectLargo = false
                else if (!selectLargo && ev.eventTime - ev.downTime > 550) {
                    selectLargo = true
                    Diptico.intercambiar()
                }
                return true
            }
            if (code == KeyEvent.KEYCODE_BUTTON_START) {
                if (ev.repeatCount == 0) startLargo = false
                else if (!startLargo && ev.eventTime - ev.downTime > 550) {
                    startLargo = true
                    Diptico.abrirCentro()
                }
                return true
            }
            if (esB(code) || code in botonesControl) return true
            return false
        }

        if (arriba) {
            if (esA(code)) {
                if (!aLargo) {
                    if (hayPanel) Diptico.aceptar()
                    else if (e.sesion != null) Diptico.atenuar()
                    else Diptico.jugar()
                }
                aLargo = false
                return true
            }
            if (esB(code)) {
                if (hayPanel) Diptico.atras()
                else if (e.vistaConsolas == "sistemas" && e.sesion == null) Diptico.volverASistemas()
                return true
            }
            when (code) {
                KeyEvent.KEYCODE_BUTTON_Y -> { if (!hayPanel) Diptico.abrirRapido(); return true }
                KeyEvent.KEYCODE_BUTTON_X -> { if (!hayPanel) Diptico.abrirFicha(); return true }
                KeyEvent.KEYCODE_BUTTON_START -> {
                    if (!startLargo) { if (hayPanel) Diptico.cerrarPanel() else Diptico.abrirMenu() }
                    startLargo = false
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_SELECT -> {
                    if (!selectLargo && !hayPanel) Diptico.alternarInicio()
                    selectLargo = false
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_THUMBL -> { if (!hayPanel) Diptico.abrirBuscar(); return true }
                KeyEvent.KEYCODE_BUTTON_THUMBR -> { if (!hayPanel) Diptico.cambiarVista(); return true }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2 -> return true
            }
        }
        return false
    }

    /** Columnas de la vista de cuadrícula/lista (la interfaz avisa cuántas hay). */
    @Volatile
    var columnasCuadricula = 4
    private fun columnasEstante(e: Estado) = if (e.vista == "lista") 1 else columnasCuadricula

    @Volatile
    var columnasBuscar = 4
    var columnasImagenes = 4
    var columnasGaleria = 3
    var columnasApps = 5

    @Volatile
    var columnasMenu = 3

    /** Paneles que se recorren en lista o grilla. */
    private fun panelEnGrilla(ev: KeyEvent, e: Estado): Boolean {
        val code = ev.keyCode
        val abajo = ev.action == KeyEvent.ACTION_DOWN
        val arriba = ev.action == KeyEvent.ACTION_UP
        val cols = when (e.panel) {
            Panel.RAPIDO -> Diptico.columnasVentana
            Panel.BUSCAR -> columnasBuscar
            Panel.IMAGENES -> columnasImagenes
            Panel.GALERIA -> columnasGaleria
            Panel.APPS -> columnasApps
            else -> 1
        }
        if (abajo) {
            when (code) {
                KeyEvent.KEYCODE_DPAD_UP -> when (e.panel) {
                    Panel.CENTRO -> Diptico.moverCentro(0, -1)
                    Panel.RAPIDO -> Diptico.moverRapido(0, -1)
                    else -> Diptico.moverIndice(-cols)
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> when (e.panel) {
                    Panel.CENTRO -> Diptico.moverCentro(0, 1)
                    Panel.RAPIDO -> Diptico.moverRapido(0, 1)
                    else -> Diptico.moverIndice(cols)
                }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    val d = if (code == KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1
                    when (e.panel) {
                        Panel.CENTRO -> Diptico.moverCentro(d, 0)
                        Panel.FICHA -> if (ev.repeatCount == 0) Diptico.cambiarPestanaFicha(d)
                        Panel.RAPIDO -> Diptico.moverRapido(d, 0)
                        Panel.EDITAR_CONSOLA -> Diptico.ladoEditarConsola(d)
                        Panel.BUSCAR, Panel.IMAGENES, Panel.GALERIA, Panel.APPS -> Diptico.moverIndice(d)
                        else -> Unit
                    }
                }
                KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1 -> if (ev.repeatCount == 0) {
                    val d = if (code == KeyEvent.KEYCODE_BUTTON_L1) -1 else 1
                    when (e.panel) {
                        Panel.FICHA -> Diptico.cambiarPestanaFicha(d)
                        Panel.BUSCAR -> Diptico.cambiarFiltro(d)
                        Panel.IMAGENES -> Diptico.cambiarFuenteImagen(d)
                        Panel.GALERIA -> Diptico.cambiarPestanaGaleria(d)
                        Panel.APPS -> Diptico.cambiarPestanaApps(d)
                        else -> Unit
                    }
                }
                KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2 -> if (ev.repeatCount == 0 && e.panel == Panel.BUSCAR) {
                    Diptico.cambiarOrden(if (code == KeyEvent.KEYCODE_BUTTON_L2) -1 else 1)
                }
                else -> return esA(code) || esB(code) || code in botonesControl
            }
            return true
        }
        if (arriba) {
            when {
                esA(code) -> Diptico.aceptar()
                // Si el centro se abrió manteniendo Start, al soltarlo se queda abierto.
                code == KeyEvent.KEYCODE_BUTTON_START && startLargo -> startLargo = false
                esB(code) || code == KeyEvent.KEYCODE_BUTTON_START -> Diptico.cerrarPanel()
                code == KeyEvent.KEYCODE_BUTTON_X -> when (e.panel) {
                    Panel.FICHA -> Diptico.cerrarPanel()
                    Panel.CENTRO -> Diptico.descartarAvisoSel()
                    Panel.ASISTENTE -> Diptico.escribirAlAsistente()
                    Panel.BUSCAR -> Diptico.limpiarBusqueda()
                    Panel.APPS -> Diptico.appsVisibles(e).getOrNull(e.indice)?.let { Diptico.infoApp(it.paquete) }
                    else -> Unit
                }
                code == KeyEvent.KEYCODE_BUTTON_Y -> when (e.panel) {
                    Panel.CARPETAS -> Diptico.quitarCarpeta()
                    Panel.CENTRO -> cl.nikalbazu.diptico.audio.Avisos.descartarTodas()
                    Panel.ASISTENTE -> Diptico.borrarChat()
                    Panel.IMAGENES -> Diptico.abrirTexto("buscar_imagen", e.nombreImagen)
                    Panel.GALERIA -> if (e.galeria == "ia") Diptico.itemsGaleria(e).getOrNull(e.indice)?.let { Diptico.abrirPaginaClave(it.id) }
                    Panel.RAPIDO -> Diptico.cerrarPanel()
                    Panel.APPS -> Diptico.appsVisibles(e).getOrNull(e.indice)?.let { Diptico.alternarFijada(it.paquete) }
                    else -> Unit
                }
                code in botonesControl || code in cruceta -> Unit
                else -> return false
            }
            return true
        }
        return false
    }

    /**
     * Estudio de widgets: ↑↓ elige un ajuste, ←→ lo cambia, A lo usa, L1/R1 cambian de pestaña,
     * Y mueve el widget con la cruceta (L1/R1 ancho, L2/R2 alto), X cambia su estilo, Select pasa al siguiente, B termina.
     */
    private fun estudio(ev: KeyEvent, e: Estado): Boolean {
        val code = ev.keyCode
        val abajo = ev.action == KeyEvent.ACTION_DOWN
        val arriba = ev.action == KeyEvent.ACTION_UP
        if (e.panel != Panel.ESTUDIO) {
            if (arriba && (esA(code) || code == KeyEvent.KEYCODE_BUTTON_Y)) Diptico.abrirAjustesWidget()
            else if (arriba && (esB(code) || code == KeyEvent.KEYCODE_BUTTON_START)) Diptico.terminarEdicion()
            return true
        }
        if (abajo) {
            val d = dir(code)
            if (e.estudioMover) {
                if (d != null) { Diptico.moverWidgetSel(d.first, d.second); return true }
                if (ev.repeatCount == 0) when (code) {
                    KeyEvent.KEYCODE_BUTTON_L1 -> Diptico.cambiarTamWidget(-1, 0)
                    KeyEvent.KEYCODE_BUTTON_R1 -> Diptico.cambiarTamWidget(1, 0)
                    KeyEvent.KEYCODE_BUTTON_L2 -> Diptico.cambiarTamWidget(0, -1)
                    KeyEvent.KEYCODE_BUTTON_R2 -> Diptico.cambiarTamWidget(0, 1)
                }
                return true
            }
            when {
                d != null && d.second != 0 -> Diptico.moverIndice(d.second)
                d != null -> Diptico.cambiarControl(d.first)
                code == KeyEvent.KEYCODE_BUTTON_L1 && ev.repeatCount == 0 -> Diptico.cambiarPestanaEstudio(-1)
                code == KeyEvent.KEYCODE_BUTTON_R1 && ev.repeatCount == 0 -> Diptico.cambiarPestanaEstudio(1)
                code == KeyEvent.KEYCODE_BUTTON_L2 && ev.repeatCount == 0 -> Diptico.cambiarPagina(-1)
                code == KeyEvent.KEYCODE_BUTTON_R2 && ev.repeatCount == 0 -> Diptico.cambiarPagina(1)
            }
            return true
        }
        if (arriba) {
            if (e.estudioMover) {
                if (esA(code) || esB(code) || code == KeyEvent.KEYCODE_BUTTON_Y) Diptico.alternarMoverWidget()
                return true
            }
            when {
                esA(code) -> Diptico.aceptarControl()
                esB(code) || code == KeyEvent.KEYCODE_BUTTON_START -> Diptico.terminarEdicion()
                code == KeyEvent.KEYCODE_BUTTON_Y -> { if (e.estudioPestana != 0) Diptico.elegirPestanaEstudio(0); Diptico.alternarMoverWidget() }
                code == KeyEvent.KEYCODE_BUTTON_X -> Diptico.cambiarEstiloWidget()
            }
            return true
        }
        return true
    }

    /** Menú principal: grilla de secciones y, adentro, la lista de opciones. */
    private fun menu(ev: KeyEvent, e: Estado): Boolean {
        val code = ev.keyCode
        val abajo = ev.action == KeyEvent.ACTION_DOWN
        val arriba = ev.action == KeyEvent.ACTION_UP
        if (abajo) {
            when (code) {
                // Columna de secciones a la izquierda y opciones a la derecha.
                KeyEvent.KEYCODE_DPAD_UP -> Diptico.moverIndice(-1)
                KeyEvent.KEYCODE_DPAD_DOWN -> Diptico.moverIndice(1)
                KeyEvent.KEYCODE_DPAD_LEFT -> if (e.enOpciones && ev.repeatCount == 0) Diptico.columnaMenu(false)
                KeyEvent.KEYCODE_DPAD_RIGHT -> if (!e.enOpciones && ev.repeatCount == 0) Diptico.columnaMenu(true)
                KeyEvent.KEYCODE_BUTTON_L1 -> if (ev.repeatCount == 0) {
                    val s = Seccion.values()
                    Diptico.elegirSeccion(s[(e.seccion.ordinal - 1 + s.size) % s.size])
                }
                KeyEvent.KEYCODE_BUTTON_R1 -> if (ev.repeatCount == 0) {
                    val s = Seccion.values()
                    Diptico.elegirSeccion(s[(e.seccion.ordinal + 1) % s.size])
                }
                else -> return esA(code) || esB(code) || code in botonesControl
            }
            return true
        }
        if (arriba) {
            when {
                esA(code) -> Diptico.aceptar()
                esB(code) -> Diptico.atras()
                code == KeyEvent.KEYCODE_BUTTON_START -> Diptico.cerrarPanel()
                code == KeyEvent.KEYCODE_BUTTON_Y -> Diptico.abrirTexto("buscar_ajuste", e.buscarAjuste)
                code in botonesControl || code in cruceta -> Unit
                else -> return false
            }
            return true
        }
        return false
    }

    // ---------- Sticks ----------

    private val manejador = Handler(Looper.getMainLooper())
    private var dirIzq: Int? = null
    private var dirDer: Int? = null
    private const val UMBRAL = 0.55f

    /** Convierte el stick en una dirección de cruceta (o null si está al centro). */
    private fun direccion(x: Float, y: Float): Int? = when {
        kotlin.math.abs(x) < UMBRAL && kotlin.math.abs(y) < UMBRAL -> null
        kotlin.math.abs(x) > kotlin.math.abs(y) -> if (x > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        else -> if (y > 0) KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP
    }

    private fun pulsar(code: Int, repeticion: Int) {
        val t = SystemClock.uptimeMillis()
        manejar(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, repeticion))
        if (repeticion == 0) manejar(KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0))
    }

    private val repetirIzq = object : Runnable {
        var veces = 0
        override fun run() {
            val d = dirIzq ?: return
            veces++
            pulsar(d, 0)
            manejador.postDelayed(this, if (veces < 3) 220 else 120)
        }
    }

    /**
     * Sticks analógicos: el izquierdo funciona como la cruceta (con repetición al mantenerlo),
     * el derecho cambia de consola (← →) o desplaza (↑ ↓).
     */
    fun movimiento(ev: MotionEvent): Boolean {
        if (ev.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK || ev.action != MotionEvent.ACTION_MOVE) return false
        val izq = direccion(ev.getAxisValue(MotionEvent.AXIS_X), ev.getAxisValue(MotionEvent.AXIS_Y))
        if (izq != dirIzq) {
            dirIzq = izq
            manejador.removeCallbacks(repetirIzq)
            if (izq != null) {
                repetirIzq.veces = 0
                pulsar(izq, 0)
                manejador.postDelayed(repetirIzq, 380)
            }
        }
        val der = direccion(ev.getAxisValue(MotionEvent.AXIS_Z), ev.getAxisValue(MotionEvent.AXIS_RZ))
        if (der != dirDer) {
            dirDer = der
            when (der) {
                KeyEvent.KEYCODE_DPAD_LEFT -> pulsar(KeyEvent.KEYCODE_BUTTON_L1, 0)
                KeyEvent.KEYCODE_DPAD_RIGHT -> pulsar(KeyEvent.KEYCODE_BUTTON_R1, 0)
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> pulsar(der, 0)
            }
        }
        // La cruceta de algunos controles llega como "hat".
        val hat = direccion(ev.getAxisValue(MotionEvent.AXIS_HAT_X), ev.getAxisValue(MotionEvent.AXIS_HAT_Y))
        if (hat != null && hat != dirHat) pulsar(hat, 0)
        dirHat = hat
        return true
    }

    private var dirHat: Int? = null
}

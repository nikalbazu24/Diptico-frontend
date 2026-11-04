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
 * Inicio: A juega (mantener = elegir la pantalla), Y ventana del juego, X ficha, B vuelve,
 * Select cambia inicio de carátulas/widgets (mantener = intercambiar pantallas), Start menú (mantener = centro de control),
 * L1/R1 consola, L2/R2 saltan de letra, L3 buscar, R3 cambia la vista. Sticks: ver [accionStick] (se eligen en Ajustes › Controles).
 */
object Teclas {
    private var aLargo = false
    private var selectLargo = false
    private var startLargo = false
    private var bLargo = false

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

        // Aprendiendo un botón extra: el primero que se apriete (que no sea A, B ni la cruceta) queda guardado.
        if (e.aprendiendoBoton >= 0) {
            if (arriba) {
                when {
                    esB(code) || code == KeyEvent.KEYCODE_BACK -> Diptico.cancelarAprender()
                    esA(code) || code in cruceta -> Unit
                    else -> Diptico.aprendido(code)
                }
            }
            return true
        }
        // Botones extra (L4, R4…): su acción, fuera de los juegos y del teclado.
        if (!e.conTeclado && e.panel != Panel.CLAVE && e.panel != Panel.PASO && e.botonesExtra.any { it.tecla == code && it.tecla != 0 && it.accion != "nada" }) {
            if (arriba) Diptico.botonExtra(code)
            return true
        }

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

        // Ventana de pregunta: A acepta (o se mantiene, si lo pide), B cancela. Nada más responde mientras está abierta.
        e.dialogo?.let { d ->
            if (esA(code)) {
                if (d.mantener) { if (abajo && ev.repeatCount == 0) Diptico.empezarMantener() else if (arriba) Diptico.soltarMantener() }
                else if (arriba) Diptico.responderDialogo(true)
            } else if (arriba && esB(code)) Diptico.responderDialogo(false)
            return true
        }

        // Tour de novedades: A o → sigue, ← vuelve, B o Start lo cierran.
        if (e.novedades >= 0) {
            if (abajo && ev.repeatCount == 0) when (code) {
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP -> Diptico.moverNovedades(-1)
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN -> Diptico.moverNovedades(1)
            }
            if (arriba) when {
                esA(code) -> Diptico.moverNovedades(1)
                esB(code) || code == KeyEvent.KEYCODE_BUTTON_START -> Diptico.cerrarNovedades()
            }
            return true
        }

        // Bienvenida al encender: A sigue jugando, cualquier otro botón la cierra.
        if (e.bienvenida > 0L && !e.bienvenidaBreve && e.sesion == null) {
            if (arriba) { if (esA(code)) Diptico.seguirDesdeBienvenida() else Diptico.cerrarBienvenida() }
            return true
        }
        if (e.bienvenida > 0L && e.bienvenidaBreve && arriba) Diptico.cerrarBienvenida()

        // Marcar en el mapa: la cruceta mueve el pin (se puede mantener), A guarda, Y otra imagen, X quita el mapa, B cancela.
        if (e.marcandoMapa != null) {
            if (abajo) dir(code)?.let { (dx, dy) -> Diptico.moverPin(dx * 0.02f, dy * 0.02f) }
            if (arriba) when {
                esA(code) -> Diptico.guardarPin()
                esB(code) -> Diptico.cerrarMapa()
                code == KeyEvent.KEYCODE_BUTTON_Y -> Diptico.cambiarImagenMapa()
                code == KeyEvent.KEYCODE_BUTTON_X -> Diptico.quitarMapa()
            }
            return true
        }

        // ¿Dónde quedaste?: ← → eligen la forma, A la usa (en la nota de voz, A otra vez termina), Y escucha la nota,
        // X no vuelve a preguntar, B pasa de largo.
        if (e.quedaste) {
            if (abajo && ev.repeatCount == 0) dir(code)?.let { (dx, dy) -> Diptico.moverQuedaste(if (dx + dy < 0) -1 else 1) }
            if (arriba) when {
                esA(code) -> Diptico.elegirQuedaste()
                esB(code) -> Diptico.cerrarQuedaste()
                code == KeyEvent.KEYCODE_BUTTON_Y -> e.seleccionado?.let { Diptico.escucharNota(it.clave) }
                code == KeyEvent.KEYCODE_BUTTON_X -> if (e.quedastePregunta) Diptico.noPreguntarQuedaste()
            }
            return true
        }

        // ¿En qué pantalla lo abro?: ↑ ↓ eligen, A abre, Y recuerda la pantalla para ese juego, X más opciones, B cancela.
        if (e.donde != null) {
            if (abajo && ev.repeatCount == 0) dir(code)?.let { (dx, dy) -> Diptico.moverDonde(if (dx + dy < 0) -1 else 1) }
            if (arriba) when {
                // El A que se soltó después de mantenerlo (lo que abrió esta ventana) no cuenta.
                esA(code) -> if (aLargo) aLargo = false else Diptico.confirmarDonde()
                esB(code) -> Diptico.cerrarDonde()
                code == KeyEvent.KEYCODE_BUTTON_Y -> Diptico.alternarRecordarDonde()
                code == KeyEvent.KEYCODE_BUTTON_X -> Diptico.confirmarDonde(2)
            }
            return true
        }

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
            Panel.WIDGET, Panel.APPS, Panel.EDITAR_CONSOLA, Panel.FICHA, Panel.TRABAJOS, Panel.RAPIDO, Panel.BUSCAR, Panel.CENTRO, Panel.CARPETAS, Panel.PORTS, Panel.ESTADISTICAS, Panel.IMAGENES, Panel.ASISTENTE, Panel.GALERIA ->
                return panelEnGrilla(ev, e)
            Panel.MENU -> return menu(ev, e)
            else -> Unit
        }

        val hayPanel = e.panel != Panel.NINGUNO

        // Moviendo un juego de lugar: la cruceta lo corre; A, B o Start terminan.
        if (e.ordenando && !hayPanel) {
            if (abajo) {
                when (code) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> Diptico.moverJuegoDeLugar(-1)
                    KeyEvent.KEYCODE_DPAD_RIGHT -> Diptico.moverJuegoDeLugar(1)
                    KeyEvent.KEYCODE_DPAD_UP -> Diptico.moverJuegoDeLugar(if (e.vista == "cuadricula" || e.vista == "lista") -columnasEstante(e) else -1)
                    KeyEvent.KEYCODE_DPAD_DOWN -> Diptico.moverJuegoDeLugar(if (e.vista == "cuadricula" || e.vista == "lista") columnasEstante(e) else 1)
                }
            }
            if (arriba && (esA(code) || esB(code) || code == KeyEvent.KEYCODE_BUTTON_START)) Diptico.terminarOrdenar()
            return true
        }

        // Inicio con widgets: ↑ desde el estante entra a los widgets; ahí la cruceta salta entre ellos, A abre y B vuelve.
        if (!hayPanel && e.sesion == null && e.inicio == "widgets" && !e.editandoWidgets) {
            if (e.widgetFoco >= 0) {
                if (abajo) {
                    dir(code)?.let { (dx, dy) -> Diptico.moverFocoWidget(dx, dy); return true }
                    if ((code == KeyEvent.KEYCODE_BUTTON_L2 || code == KeyEvent.KEYCODE_BUTTON_R2) && ev.repeatCount == 0) {
                        Diptico.paginaFocoWidget(if (code == KeyEvent.KEYCODE_BUTTON_L2) -1 else 1); return true
                    }
                    if (esA(code) || esB(code)) return true
                }
                if (arriba) when {
                    esA(code) -> { Diptico.abrirWidget(); return true }
                    esB(code) -> { Diptico.salirWidgets(); return true }
                    code in cruceta -> return true
                }
            } else if (abajo && ((e.unaPantalla && code in cruceta) || (code == KeyEvent.KEYCODE_DPAD_UP && e.vistaConsolas !in setOf("lado", "rueda", "sistemas", "carpetas") &&
                    ((e.vista != "cuadricula" && e.vista != "lista") || e.indiceSel < columnasEstante(e))))
            ) {
                if (Diptico.entrarWidgets()) return true
            }
        }

        // Modo Sistemas, eligiendo consola: la cruceta y L1/R1 cambian de consola, A entra, Y elige un juego al azar.
        if (!hayPanel && e.sesion == null && e.vistaConsolas in Diptico.MODOS_ELEGIR && e.enSistemas) {
            if (abajo) {
                // En Carpetas (una grilla) ↑ ↓ saltan de fila; en Sistemas todo avanza de a uno.
                dir(code)?.let { (dx, dy) -> Diptico.cambiarConsola(if (dy != 0 && e.vistaConsolas == "carpetas") dy * columnasCarpetas else if (dx + dy < 0) -1 else 1); return true }
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
                    // Mantener A: elegir en qué pantalla se abre (con una sola pantalla, la ventana del juego).
                    aLargo = true
                    Diptico.elegirPantalla("juego")
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
            // Mantener B en el estante: vuelve al primer juego de la lista.
            if (esB(code) && !hayPanel) {
                if (ev.repeatCount == 0) bLargo = false
                else if (!bLargo && ev.eventTime - ev.downTime > 550) { bLargo = true; Diptico.alPrincipio() }
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
                if (bLargo) { bLargo = false; return true }
                if (hayPanel) Diptico.atras()
                else if (e.vistaConsolas in Diptico.MODOS_ELEGIR && e.sesion == null) Diptico.volverASistemas()
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
    var columnasCarpetas = 3

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
                        Panel.WIDGET -> if (ev.repeatCount == 0 || ev.repeatCount % 3 == 0) Diptico.ladoWidget(d)
                        Panel.BUSCAR, Panel.IMAGENES, Panel.GALERIA, Panel.APPS, Panel.ASISTENTE -> Diptico.moverIndice(d)
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
                        Panel.CENTRO -> Diptico.cambiarCortina(if (d < 0) "avisos" else "ajustes")
                        else -> Unit
                    }
                }
                KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2 -> if (ev.repeatCount == 0 && e.panel == Panel.BUSCAR) {
                    Diptico.cambiarOrden(if (code == KeyEvent.KEYCODE_BUTTON_L2) -1 else 1)
                }
                else -> {
                    // En las apps: mantener A elige en qué pantalla abrirla.
                    if (esA(code) && e.panel == Panel.APPS) {
                        if (ev.repeatCount == 0) aLargo = false
                        else if (!aLargo && ev.eventTime - ev.downTime > 550) {
                            Diptico.appsVisibles(e).getOrNull(e.indice)?.let { aLargo = true; Diptico.elegirPantalla("app:${it.paquete}") }
                        }
                    }
                    return esA(code) || esB(code) || code in botonesControl
                }
            }
            return true
        }
        if (arriba) {
            when {
                esA(code) && aLargo -> aLargo = false
                esA(code) -> Diptico.aceptar()
                // Si el centro se abrió manteniendo Start, al soltarlo se queda abierto.
                code == KeyEvent.KEYCODE_BUTTON_START && startLargo -> startLargo = false
                esB(code) || code == KeyEvent.KEYCODE_BUTTON_START -> Diptico.cerrarPanel()
                code == KeyEvent.KEYCODE_BUTTON_X -> when (e.panel) {
                    Panel.FICHA -> Diptico.cerrarPanel()
                    Panel.CENTRO -> Diptico.descartarAvisoSel()
                    Panel.ASISTENTE -> Diptico.escribirAlAsistente()
                    Panel.BUSCAR -> if (e.tecladoPropio) Diptico.abrirTexto("buscar_juego", e.busqueda) else Diptico.limpiarBusqueda()
                    Panel.APPS -> Diptico.appsVisibles(e).getOrNull(e.indice)?.let { Diptico.infoApp(it.paquete) }
                    else -> Unit
                }
                code == KeyEvent.KEYCODE_BUTTON_Y -> when (e.panel) {
                    Panel.CARPETAS -> Diptico.quitarCarpeta()
                    Panel.CENTRO -> if (e.cortina == "avisos") cl.nikalbazu.diptico.audio.Avisos.descartarTodas()
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
    private var aLargoEstudio = false

    /**
     * Editor de widgets. Zona "grilla": la cruceta (o el stick) elige cualquier widget del inicio; A lo levanta para moverlo,
     * mantener A (o Y) abre la burbuja para agregar, X cambia el estilo y R1 pasa a las opciones del panel.
     * Zona "panel": la cruceta recorre las opciones; B vuelve a la grilla. Start o B en la grilla: listo.
     */
    private fun estudio(ev: KeyEvent, e: Estado): Boolean {
        val code = ev.keyCode
        val abajo = ev.action == KeyEvent.ACTION_DOWN
        val arriba = ev.action == KeyEvent.ACTION_UP
        // Burbuja para agregar: la cruceta elige, A agrega, B cierra.
        if (e.burbuja != null) {
            if (abajo) dir(code)?.let { (dx, dy) -> Diptico.moverBurbuja(if (dy != 0) dy * 3 else dx); return true }
            if (arriba) when {
                esA(code) -> Diptico.elegirBurbuja()
                esB(code) || code == KeyEvent.KEYCODE_BUTTON_Y -> Diptico.cerrarBurbuja()
            }
            return true
        }
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
            if (e.estudioZona == "grilla") {
                when {
                    d != null -> Diptico.elegirWidgetDir(d.first, d.second)
                    esA(code) -> {
                        if (ev.repeatCount == 0) aLargoEstudio = false
                        else if (!aLargoEstudio && ev.eventTime - ev.downTime > 550) { aLargoEstudio = true; Diptico.abrirBurbujaAqui() }
                    }
                    code == KeyEvent.KEYCODE_BUTTON_L2 && ev.repeatCount == 0 -> Diptico.cambiarPagina(-1)
                    code == KeyEvent.KEYCODE_BUTTON_R2 && ev.repeatCount == 0 -> Diptico.cambiarPagina(1)
                    (code == KeyEvent.KEYCODE_BUTTON_R1 || code == KeyEvent.KEYCODE_BUTTON_L1) && ev.repeatCount == 0 -> Diptico.zonaEstudio("panel")
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
            if (e.estudioZona == "grilla") {
                when {
                    esA(code) -> { if (!aLargoEstudio) Diptico.alternarMoverWidget(); aLargoEstudio = false }
                    code == KeyEvent.KEYCODE_BUTTON_Y -> Diptico.abrirBurbujaAqui()
                    code == KeyEvent.KEYCODE_BUTTON_X -> Diptico.cambiarEstiloWidget()
                    esB(code) || code == KeyEvent.KEYCODE_BUTTON_START -> Diptico.terminarEdicion()
                }
                return true
            }
            when {
                esA(code) -> Diptico.aceptarControl()
                esB(code) -> Diptico.zonaEstudio("grilla")
                code == KeyEvent.KEYCODE_BUTTON_START -> Diptico.terminarEdicion()
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

    /** Un stick que repite su dirección mientras se mantiene (rápido después de las primeras veces). */
    private class Repetir(val alMover: (Int, Boolean) -> Unit) : Runnable {
        var dir: Int? = null
        var veces = 0
        fun cambiar(d: Int?) {
            if (d == dir) return
            dir = d
            manejador.removeCallbacks(this)
            if (d != null) { veces = 0; alMover(d, false); manejador.postDelayed(this, 380) }
        }
        override fun run() {
            val d = dir ?: return
            veces++
            alMover(d, true)
            manejador.postDelayed(this, if (veces < 3) 220 else 120)
        }
    }

    private val stickIzq = Repetir { d, rep -> accionStick(true, d, rep) }
    private val stickDer = Repetir { d, rep -> accionStick(false, d, rep) }

    /**
     * Qué hace cada stick. Ajustes › Controles › Sticks:
     * "dividido" (lo normal): el izquierdo elige juegos y el derecho recorre los widgets;
     * "invertido": al revés; "clasico": el izquierdo es la cruceta y el derecho cambia de consola.
     * En el editor de widgets, el stick de los widgets desliza la grilla (↑ ↓) y cambia de página (← →).
     */
    private fun accionStick(izquierdo: Boolean, d: Int, repetido: Boolean) {
        val e = Diptico.estado.value
        val (dx, dy) = dir(d) ?: return
        val modo = e.sticks
        val deWidgets = when (modo) { "invertido" -> izquierdo; "clasico" -> false; else -> !izquierdo }
        val deJuegos = when (modo) { "invertido" -> !izquierdo; "clasico" -> false; else -> izquierdo }
        val libre = e.sesion == null && !e.conTeclado && e.configurado && !e.intro && e.bienvenida == 0L
        // Editor de widgets: un stick desliza para ver todo, el otro elige como la cruceta.
        if (libre && e.editandoWidgets && (e.panel == Panel.NINGUNO || e.panel == Panel.ESTUDIO) && e.burbuja == null) {
            val desliza = if (modo == "clasico") !izquierdo else deWidgets
            if (desliza) {
                if (dy != 0) Diptico.desplazarInicio(dy) else if (!repetido) Diptico.cambiarPagina(dx)
                return
            }
            if (e.panel == Panel.NINGUNO || e.estudioZona == "grilla") { pulsar(d, 0); return }
        }
        val enInicio = libre && e.inicio == "widgets" && e.panel == Panel.NINGUNO && !e.editandoWidgets && !e.ordenando &&
            !(e.vistaConsolas in Diptico.MODOS_ELEGIR && e.enSistemas)
        if (enInicio && deWidgets) {
            if (e.widgetFoco < 0) Diptico.entrarWidgets() else { Diptico.moverFocoWidget(dx, dy); cl.nikalbazu.diptico.audio.Sonidos.mover() }
            return
        }
        if (enInicio && deJuegos) {
            if (e.widgetFoco >= 0) Diptico.salirWidgets()
            val enGrilla = e.vista == "cuadricula" || e.vista == "lista"
            Diptico.moverJuego(if (dy != 0) (if (enGrilla) dy * columnasEstante(e) else dy) else dx)
            cl.nikalbazu.diptico.audio.Sonidos.mover()
            return
        }
        // En todo lo demás: el izquierdo (o el de juegos) es la cruceta; el otro cambia de consola (← →) o desplaza (↑ ↓).
        val comoCruceta = if (modo == "invertido") !izquierdo else izquierdo
        if (comoCruceta) { pulsar(d, 0); return }
        if (repetido) return
        when (d) {
            KeyEvent.KEYCODE_DPAD_LEFT -> pulsar(KeyEvent.KEYCODE_BUTTON_L1, 0)
            KeyEvent.KEYCODE_DPAD_RIGHT -> pulsar(KeyEvent.KEYCODE_BUTTON_R1, 0)
            else -> pulsar(d, 0)
        }
    }

    /** Sticks analógicos (ver [accionStick]) y la cruceta de los controles que llega como "hat". */
    fun movimiento(ev: MotionEvent): Boolean {
        if (ev.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK || ev.action != MotionEvent.ACTION_MOVE) return false
        stickIzq.cambiar(direccion(ev.getAxisValue(MotionEvent.AXIS_X), ev.getAxisValue(MotionEvent.AXIS_Y)))
        stickDer.cambiar(direccion(ev.getAxisValue(MotionEvent.AXIS_Z), ev.getAxisValue(MotionEvent.AXIS_RZ)))
        val hat = direccion(ev.getAxisValue(MotionEvent.AXIS_HAT_X), ev.getAxisValue(MotionEvent.AXIS_HAT_Y))
        if (hat != null && hat != dirHat) pulsar(hat, 0)
        dirHat = hat
        return true
    }

    private var dirHat: Int? = null
}

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
    private val presionadas = mutableSetOf<Int>()
    private val consumidas = mutableSetOf<Int>()
    private val atajosHandler = Handler(Looper.getMainLooper())
    private const val VENTANA_DOBLE = 300L
    private data class ToquePendiente(val tiempo: Long, val tarea: Runnable)
    private val toquesPendientes = mutableMapOf<Int, ToquePendiente>()

    /**
     * Olvida las teclas apretadas. Se llama al perder el foco (se abrió un juego o una ventana): Android no avisa
     * cuándo se sueltan esas teclas, y sin esto Start quedaba «apretado» y después R3 o L3 solos disparaban un atajo.
     */
    fun soltarTodo() {
        presionadas.clear()
        consumidas.clear()
        toquesPendientes.values.forEach { atajosHandler.removeCallbacks(it.tarea) }
        toquesPendientes.clear()
        aLargo = false; selectLargo = false; startLargo = false; bLargo = false
    }

    private fun cancelarToque(tecla: Int) {
        toquesPendientes.remove(tecla)?.let { atajosHandler.removeCallbacks(it.tarea) }
    }

    private fun atajosDisponibles(e: Estado): Boolean =
        e.configurado && e.sesion == null && !e.conTeclado && !e.intro && e.panel == Panel.NINGUNO &&
            e.dialogo == null && e.novedades < 0 && !e.relojGrande && !e.editandoWidgets &&
            !e.ordenando && !e.extra.galeria && e.bienvenida == 0L &&
            e.donde == null && !e.quedaste && e.marcandoMapa == null

    private fun atajo(ev: KeyEvent, e: Estado): Boolean {
        if (!atajosDisponibles(e)) return false
        val code = ev.keyCode
        val inicio = KeyEvent.KEYCODE_BUTTON_START
        val opciones = listOf(AtajoInicio.leer(e.atajoBisa, AtajoInicio.NO) to true,
            AtajoInicio.leer(e.atajoApps, AtajoInicio.NO) to false)
        fun abrir(bisa: Boolean) {
            if (bisa) Diptico.abrirAsistente() else Diptico.abrirApps()
        }
        if (ev.action == KeyEvent.ACTION_DOWN && ev.repeatCount == 0) {
            val combo = opciones.firstOrNull { (opcion, _) ->
                val pareja = opcion.tecla
                !opcion.doble && pareja != null &&
                    ((code == pareja && inicio in presionadas && !startLargo) ||
                        (code == inicio && pareja in presionadas &&
                            !(pareja == KeyEvent.KEYCODE_BUTTON_SELECT && selectLargo)))
            }
            toquesPendientes.keys.toList().filter { it != code || combo != null }.forEach(::cancelarToque)
            if (combo != null) {
                consumidas += inicio
                consumidas += checkNotNull(combo.first.tecla)
                startLargo = false
                selectLargo = false
                abrir(combo.second)
                return true
            }
        }
        if (ev.action != KeyEvent.ACTION_UP || code == inicio && startLargo) return false
        val doble = opciones.firstOrNull { (opcion, _) -> opcion.doble && opcion.tecla == code } ?: return false
        val anterior = toquesPendientes[code]
        cancelarToque(code)
        if (anterior != null && ev.eventTime - anterior.tiempo in 0L..VENTANA_DOBLE) {
            abrir(doble.second)
            return true
        }
        val tarea = Runnable {
            toquesPendientes.remove(code)
            val actual = Diptico.estado.value
            val id = if (doble.second) actual.atajoBisa else actual.atajoApps
            if (atajosDisponibles(actual) && code !in presionadas && id == doble.first.id) when (code) {
                inicio -> Diptico.abrirMenu()
                KeyEvent.KEYCODE_BUTTON_THUMBL -> Diptico.abrirBuscar()
                KeyEvent.KEYCODE_BUTTON_THUMBR -> Diptico.abrirGaleria("vistas_juegos")
            }
        }
        toquesPendientes[code] = ToquePendiente(ev.eventTime, tarea)
        atajosHandler.postDelayed(tarea, VENTANA_DOBLE)
        return true
    }

    private fun esA(code: Int) = code == KeyEvent.KEYCODE_BUTTON_A || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
    private fun esB(code: Int) = code == KeyEvent.KEYCODE_BUTTON_B || code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE
    private fun esY(code: Int) = code == KeyEvent.KEYCODE_BUTTON_Y

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

    /** Una capa/modal retiene el mando: nada debe filtrarse al Home que sigue dibujado detrás. */
    private fun modalVisible(e: Estado): Boolean =
        e.panel != Panel.NINGUNO || e.dialogo != null || e.novedades >= 0 ||
            e.conTeclado || e.donde != null || e.quedaste || e.marcandoMapa != null ||
            e.extra.nativosEn >= 0 || e.extra.perfilEn >= 0 ||
            e.extra.edicionPerfil != null || e.extra.anioEn >= 0 ||
            e.extra.amigosEn >= 0 || e.extra.edicionVitrina != null ||
            e.extra.cajaJuego.isNotEmpty() || e.extra.discordLogin != null ||
            // La Galería de Widgets ocupa su propia pantalla. Debe retener Select,
            // sticks y botones, no permitir a la ruta genérica alternarInicio().
            (e.inicio == "widgets" && e.extra.galeria) ||
            // Una carpeta de Colección viva es navegación principal, no un diálogo modal.
            // «carpetaEnVentana» también es true BAJO Widgets si el usuario abrió una
            // carpeta previamente. Bloquearla aquí filtraba Select y L1/R1 al Home.
            (e.carpetaEnVentana && !e.extra.coleccionViva && e.inicio != "widgets") ||
            e.relojGrande || e.extra.carga != null ||
            e.extra.emusDescarga != null

    fun manejar(ev: KeyEvent): Boolean {
        val code = ev.keyCode
        // El volumen siempre lo maneja Android (así baja también la música de Díptico).
        if (code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN || code == KeyEvent.KEYCODE_VOLUME_MUTE) return false
        val e = Diptico.estado.value
        val abajo = ev.action == KeyEvent.ACTION_DOWN
        val arriba = ev.action == KeyEvent.ACTION_UP
        if (abajo) presionadas += code else if (arriba) presionadas -= code
        if (abajo) cl.nikalbazu.diptico.ui.Vivo.toque()
        if (code in consumidas) { if (arriba) consumidas -= code; return true }
        // La colección captura Select antes del controlador genérico. Reiniciar aquí
        // el estado de pulsación larga permite mantener Select varias veces seguidas,
        // o pulsarlo corto después, sin arrastrar el estado anterior.
        if (code == KeyEvent.KEYCODE_BUTTON_SELECT && abajo && ev.repeatCount == 0) selectLargo = false
        // Durante la descarga/instalación de un emulador el instalador de Android es el único dueño
        // de los controles: no se abren carpetas ni cambia la selección en la pantalla de fondo.
        if (e.extra.emusDescarga != null) return true
        // El diálogo tiene prioridad incluso si la ventana de Emuladores al día sigue abierta.
        e.dialogo?.let { d ->
            if (esA(code)) {
                if (d.mantener) {
                    if (abajo && ev.repeatCount == 0) Diptico.empezarMantener()
                    else if (arriba) Diptico.soltarMantener()
                } else if (arriba) Diptico.responderDialogo(true)
            } else if (arriba && esB(code)) Diptico.responderDialogo(false)
            return true
        }
        // Mantener Select (en cualquier parte): intercambia las pantallas de arriba y abajo.
        if (code == KeyEvent.KEYCODE_BUTTON_SELECT && abajo && ev.repeatCount == 12 &&
            !selectLargo && !e.unaPantalla && !modalVisible(e)) {
            // La ruta genérica ya puede haber detectado una pulsación larga (>550 ms).
            // No intercambiar dos veces por el mismo Select mantenido.
            selectLargo = true
            consumidas += code
            Diptico.intercambiar()
            return true
        }
        // Abriendo un juego con la pantalla de carga: B la cancela; lo demás espera.
        // Iniciando sesión con Discord: B cierra el panel; A pide otro código si hubo un error.
        e.extra.discordLogin?.let { l ->
            if (arriba && esB(code)) Diptico.cancelarDiscord()
            else if (arriba && esA(code) && l.error != null) Diptico.iniciarDiscord()
            else if (arriba && esY(code) && SocialDiscord.disponible) Diptico.conectarDiscordAqui()
            return true
        }
        if (e.extra.carga != null) { if (arriba && esB(code)) Diptico.cancelarCarga(); return true }
        // Partida en curso con una pantalla (sin nada abierto encima): cruceta elige, A usa, Select o mantener B terminan.
        if (e.partidaEnPausa && e.panel == Panel.NINGUNO && !e.conTeclado && e.dialogo == null) {
            if (abajo) when (code) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT -> Diptico.moverPartidaUnica(-1)
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> Diptico.moverPartidaUnica(1)
                else -> if (esB(code) && ev.repeatCount == 8) { consumidas += code; Diptico.terminarPartidaUnica() }
            }
            if (arriba) when {
                esA(code) -> Diptico.usarPartidaUnica()
                code == KeyEvent.KEYCODE_BUTTON_SELECT -> Diptico.terminarPartidaUnica()
                esB(code) -> Diptico.volverAlJuegoUnica()
            }
            return true
        }
        // Tu espacio tiene prioridad absoluta: L1/R1 recorren sus páginas (no las carpetas ocultas).
        // En Tu espacio L1/R1 nunca deben llegar a cambiarConsola() del Home.
        // Capturar también con foco en widgets y en la galería; el recorrido tiene su propio controlador.
        if (e.inicio == "widgets" && e.sesion == null && !e.recorrido && !modalVisible(e) &&
            !e.editandoWidgets && (code == KeyEvent.KEYCODE_BUTTON_L1 || code == KeyEvent.KEYCODE_BUTTON_R1)) {
            if (abajo && ev.repeatCount == 0) {
                val delta = if (code == KeyEvent.KEYCODE_BUTTON_L1) -1 else 1
                when {
                    e.extra.galeria -> Diptico.categoriaEspacio(delta)
                    e.widgetFoco >= 0 -> Diptico.paginaFocoWidget(delta)
                    else -> Diptico.cambiarPagina(delta)
                }
            }
            return true
        }
        // Colección viva: R3 gira la caja (y abajo el carrete), Select cambia la vista; en las carpetas X opciones, Y seguir o ícono.
        if (cl.nikalbazu.diptico.ui.coleccionVivaActiva(e) && !modalVisible(e) &&
            e.extra.perfilEn < 0 && e.extra.anioEn < 0 && e.extra.amigosEn < 0 && e.novedades < 0 && e.inicio != "widgets") {
            val j = e.seleccionado
            if (e.extra.cajaGirada && !e.enSistemas) {
                val total = (if (j?.portada != null) 1 else 0) + (if (j?.arte != null) 1 else 0) + 6
                if (abajo) when (code) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> Diptico.moverCarrete(-1, total)
                    KeyEvent.KEYCODE_DPAD_RIGHT -> Diptico.moverCarrete(1, total)
                }
                if (arriba) when {
                    esB(code) || code == KeyEvent.KEYCODE_BUTTON_THUMBR -> Diptico.girarCaja(false)
                    // Select también cambia la vista si se dejó girada la caja: salimos
                    // del reverso primero para que la nueva vista sea visible de inmediato.
                    code == KeyEvent.KEYCODE_BUTTON_SELECT -> {
                        Diptico.girarCaja(false)
                        Diptico.siguienteVistaColeccion()
                    }
                    esY(code) -> j?.let { Diptico.abrirCajaJuego(it.clave) }
                    code == KeyEvent.KEYCODE_BUTTON_X -> Diptico.abrirFicha()
                    esA(code) -> Diptico.jugar()
                }
                if (code != KeyEvent.KEYCODE_BUTTON_START && code != KeyEvent.KEYCODE_BUTTON_MODE && code != KeyEvent.KEYCODE_HOME) return true
            } else if (!e.enSistemas) {
                when (code) {
                    KeyEvent.KEYCODE_BUTTON_THUMBR, KeyEvent.KEYCODE_BUTTON_X -> { if (arriba && j != null) Diptico.girarCaja(true); return true }
                    KeyEvent.KEYCODE_BUTTON_SELECT -> {
                        if (arriba && ev.repeatCount == 0) Diptico.siguienteVistaColeccion()
                        return true
                    }
                }
            } else {
                val id = e.consolaSel
                when {
                    code == KeyEvent.KEYCODE_BUTTON_SELECT -> { if (arriba) Diptico.siguienteVistaCarpetas(); return true }
                    code == KeyEvent.KEYCODE_BUTTON_X -> { if (arriba) Diptico.abrirEditarConsola(id); return true }
                    esY(code) -> { if (arriba) ultimoDeConsola(e, id)?.let { Diptico.seguirCon(it) } ?: Diptico.cambiarIconoCarpeta(id); return true }
                    esA(code) -> { if (arriba && id != "todo") Diptico.entrarConsolaCV(id); return id != "todo" }
                }
            }
        }
        // Lugar en la vitrina: cruceta mueve la pieza, L1/R1 cambian de pieza, L2/R2 tamaño, X caja, Y difuminado, A/B/Start listo.
        if (e.extra.edicionVitrina != null) {
            if (abajo) when (code) {
                KeyEvent.KEYCODE_DPAD_UP -> Diptico.moverPiezaVitrina(0, -1)
                KeyEvent.KEYCODE_DPAD_DOWN -> Diptico.moverPiezaVitrina(0, 1)
                KeyEvent.KEYCODE_DPAD_LEFT -> Diptico.moverPiezaVitrina(-1, 0)
                KeyEvent.KEYCODE_DPAD_RIGHT -> Diptico.moverPiezaVitrina(1, 0)
                KeyEvent.KEYCODE_BUTTON_L1 -> if (ev.repeatCount == 0) Diptico.siguientePiezaVitrina(-1)
                KeyEvent.KEYCODE_BUTTON_R1 -> if (ev.repeatCount == 0) Diptico.siguientePiezaVitrina(1)
                KeyEvent.KEYCODE_BUTTON_L2 -> Diptico.tamPiezaVitrina(-0.1f)
                KeyEvent.KEYCODE_BUTTON_R2 -> Diptico.tamPiezaVitrina(0.1f)
            }
            if (arriba) when {
                code == KeyEvent.KEYCODE_BUTTON_X -> Diptico.alternarCajaVitrina()
                esY(code) -> Diptico.alternarDifuminadoVitrina()
                code == KeyEvent.KEYCODE_BUTTON_SELECT -> Diptico.volverDisposicionOriginal()
                esA(code) || esB(code) || code == KeyEvent.KEYCODE_BUTTON_START -> Diptico.guardarEditorVitrina()
            }
            return true
        }
        // Estudio de Cajas: cruceta elige, L1/R1 pestaña, A usa, X abre la caja, Select cambia la vista, Y juega, B vuelve.
        if (e.extra.cajaJuego.isNotEmpty()) {
            if (abajo) when (code) {
                KeyEvent.KEYCODE_DPAD_UP -> Diptico.moverEstudio(0, -1)
                KeyEvent.KEYCODE_DPAD_DOWN -> Diptico.moverEstudio(0, 1)
                KeyEvent.KEYCODE_DPAD_LEFT -> Diptico.moverEstudio(-1, 0)
                KeyEvent.KEYCODE_DPAD_RIGHT -> Diptico.moverEstudio(1, 0)
                KeyEvent.KEYCODE_BUTTON_L1 -> if (ev.repeatCount == 0) Diptico.pestanaEstudio(-1)
                KeyEvent.KEYCODE_BUTTON_R1 -> if (ev.repeatCount == 0) Diptico.pestanaEstudio(1)
            }
            if (arriba) when {
                esB(code) || code == KeyEvent.KEYCODE_BACK -> Diptico.cerrarCajaJuego()
                esA(code) -> Diptico.usarEstudio()
                code == KeyEvent.KEYCODE_BUTTON_X -> Diptico.xEstudio()
                code == KeyEvent.KEYCODE_BUTTON_SELECT -> Diptico.siguienteVistaEstudio()
                esY(code) -> { val k = e.extra.cajaJuego; Diptico.cerrarCajaJuego(); Diptico.elegirJuego(k); Diptico.jugar() }
            }
            return true
        }
        // Centro de Switch 2 y ports: cruceta elige, L1/R1 (o ← →) cambian de pestaña, A usa, B cierra.
        if (e.extra.nativosEn >= 0 && !e.conTeclado) {
            if (abajo) when (code) {
                KeyEvent.KEYCODE_DPAD_UP -> Diptico.moverNativos(-1)
                KeyEvent.KEYCODE_DPAD_DOWN -> Diptico.moverNativos(1)
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_BUTTON_L1 -> if (ev.repeatCount == 0) Diptico.pestanaNativos(-1)
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_BUTTON_R1 -> if (ev.repeatCount == 0) Diptico.pestanaNativos(1)
            }
            if (arriba) when {
                esB(code) || code == KeyEvent.KEYCODE_BACK -> Diptico.cerrarNativos()
                esA(code) -> Diptico.usarNativo()
            }
            return true
        }
        // Tu año: B lo cierra.
        if (e.extra.anioEn >= 0) {
            if (arriba && (esB(code) || code == KeyEvent.KEYCODE_BACK)) Diptico.cerrarTuAno()
            return true
        }
        // Tu perfil: B lo cierra; A abre los ajustes para cambiar tus datos; Y abre «Tu año».
        // Editando el perfil (abajo): la cruceta elige, A usa lo elegido, Start guarda, B cancela.
        // Si se abrió el cuadro de texto (nombre, apodo, fecha), las teclas son del teclado.
        val ed = e.extra.edicionPerfil
        if (ed != null && e.panel == Panel.NINGUNO && !e.conTeclado) {
            if (abajo) when (code) {
                KeyEvent.KEYCODE_DPAD_UP -> Diptico.moverEdicionPerfil(-1, 0)
                KeyEvent.KEYCODE_DPAD_DOWN -> Diptico.moverEdicionPerfil(1, 0)
                KeyEvent.KEYCODE_DPAD_LEFT -> Diptico.moverEdicionPerfil(0, -1)
                KeyEvent.KEYCODE_DPAD_RIGHT -> Diptico.moverEdicionPerfil(0, 1)
            }
            if (arriba) when {
                esB(code) || code == KeyEvent.KEYCODE_BACK -> Diptico.cancelarEdicionPerfil()
                code == KeyEvent.KEYCODE_BUTTON_START -> Diptico.guardarEdicionPerfil()
                esA(code) -> Diptico.usarEdicionPerfil(ed.fila, ed.col)
            }
            return true
        }
        if (e.extra.perfilEn >= 0 && ed == null && !e.conTeclado) {
            if (arriba && (esB(code) || code == KeyEvent.KEYCODE_BACK)) Diptico.cerrarPerfil()
            else if (arriba && esA(code)) Diptico.editarPerfil()
            else if (arriba && esY(code)) Diptico.abrirTuAno(e.extra.perfilEn)
            return true
        }
        // Ventana de amigos de Discord: B la cierra; A conecta si todavía no hay cuenta.
        if (e.extra.amigosEn >= 0 && !e.conTeclado) {
            if (arriba && (esB(code) || code == KeyEvent.KEYCODE_BACK)) Diptico.cerrarAmigos()
            else if (arriba && esA(code) && e.extra.discordUsuario.isBlank()) { Diptico.cerrarAmigos(); Diptico.iniciarDiscord() }
            else if (arriba && esY(code) && e.extra.discordUsuario.isBlank() && SocialDiscord.disponible) Diptico.conectarDiscordAqui()
            return true
        }

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
        val start = KeyEvent.KEYCODE_BUTTON_START
        if (abajo && ev.repeatCount == 0 && start in presionadas && !startLargo && atajosDisponibles(e)
            && e.botonesExtra.any { it.conStart && it.tecla == code && code != start && it.accion != "nada" }) {
            cancelarToque(start)
            consumidas += start
            consumidas += code
            Diptico.botonExtra(code, conStart = true)
            return true
        }

        // Start + A en el inicio: abre (o cierra) tu perfil.
        if (abajo && ev.repeatCount == 0 && esA(code) && start in presionadas && e.configurado && !modalVisible(e) && !e.intro) {
            cancelarToque(start)
            consumidas += start
            consumidas += code
            if (e.extra.perfilEn >= 0) Diptico.cerrarPerfil() else Diptico.abrirPerfil()
            return true
        }

        // Botones extra (L4, R4…): su acción, fuera de los juegos y del teclado.
        if (!modalVisible(e) && e.botonesExtra.any { !it.conStart && it.tecla == code && it.tecla != 0 && it.accion != "nada" }) {
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

        // Los diálogos se despachan al comienzo, antes de cualquier control del Home o ventanas.

        // El recorrido ejecuta las acciones REALES, con prioridad sobre el Home.
        if (e.novedades >= 0) {
            if (e.recorrido) {
                // Cada paso escucha SOLO su botón y hace la acción real (la misma que el botón «Probar ahora»).
                // El paso se da por cumplido al ver el cambio en la interfaz (RecorridoDippo), no al apretar.
                val k = e.novedades
                val id = RecorridoVivo.idDe(k)
                val pide = when (id) {
                    "aJuego" -> code == KeyEvent.KEYCODE_BUTTON_R2 || code == KeyEvent.KEYCODE_BUTTON_L2
                    "aWidgets" -> code == KeyEvent.KEYCODE_BUTTON_L2 || code == KeyEvent.KEYCODE_BUTTON_R2
                    "consola" -> code == KeyEvent.KEYCODE_BUTTON_L1 || code == KeyEvent.KEYCODE_BUTTON_R1
                    "personalizar", "editar" -> code == KeyEvent.KEYCODE_BUTTON_X
                    "vistaCarpetas", "vistaJuegos" -> code == KeyEvent.KEYCODE_BUTTON_SELECT
                    "abrirConsola", "apps", "dippo" -> esA(code)
                    "opcionesJuego", "galeria", "buscar" -> code == KeyEvent.KEYCODE_BUTTON_Y
                    "volver" -> esB(code) || code == KeyEvent.KEYCODE_BACK
                    "ajustes" -> code == KeyEvent.KEYCODE_BUTTON_START
                    "perfil" -> esA(code) && KeyEvent.KEYCODE_BUTTON_START in presionadas
                    else -> false
                }
                val cumplible = !RecorridoVivo.puedeSeguir(k)
                if (abajo && ev.repeatCount == 0 && pide && cumplible) {
                    RecorridoVivo.probar(k, if (code == KeyEvent.KEYCODE_BUTTON_L1 || code == KeyEvent.KEYCODE_BUTTON_L2) -1 else 1)
                } else if (arriba && !pide && esA(code)) {
                    // A sigue solo cuando el paso ya está hecho; si no, DIPPO recuerda qué falta.
                    if (RecorridoVivo.puedeSeguir(k)) Diptico.moverNovedades(1) else RecorridoVivo.avisar("falta")
                } else if (arriba && !pide && (esB(code) || code == KeyEvent.KEYCODE_BACK) && cumplible) {
                    // El recorrido no se cierra con B: hay que completarlo (se puede repetir desde Ajustes).
                    RecorridoVivo.avisar("falta")
                }
                return true
            }
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

        if (e.panel == Panel.APPS && e.appsMenu != null) {
            if (abajo) dir(code)?.let { (_, dy) -> Diptico.moverMenuApp(if (dy != 0) dy else if (code == KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1) }
            if (arriba && esA(code)) { if (aLargo) aLargo = false else Diptico.aceptarMenuApp() }
            else if (arriba && (esB(code) || code == KeyEvent.KEYCODE_BUTTON_X)) Diptico.cerrarOpcionesApp()
            return true
        }
        if (e.panel == Panel.APPS && e.appsMoviendo != null) {
            if (abajo) dir(code)?.let { (dx, dy) -> Diptico.moverAppA(e.indice + if (dy != 0) dy * columnasApps else dx) }
            if (arriba && (esA(code) || esB(code))) Diptico.cerrarOpcionesApp()
            return true
        }
        if (atajo(ev, e)) return true

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
                code == KeyEvent.KEYCODE_BUTTON_X || code == KeyEvent.KEYCODE_BUTTON_Y -> cl.nikalbazu.diptico.audio.Sonidos.abrir()
            }
        }

        // Escribiendo (próximo paso o un texto): el teclado manda, salvo B (cerrar) y Start (guardar).
        if (e.conTeclado) {
            // Teclado de Díptico: cruceta elige, A escribe, B borra (o cierra si no hay nada), X espacio, Y tilde o número de la tecla (si no tiene, mayúscula),
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
                code == KeyEvent.KEYCODE_BUTTON_Y -> Diptico.varianteElegida()
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

        // Configuración de la primera vez: ↑↓ entre filas (la última es «Siguiente»), ◀▶ cambian la fila, A usa,
        // Start pasa al paso siguiente desde donde estés y B vuelve.
        if (!e.configurado && e.panel == Panel.NINGUNO) {
            if (abajo) {
                val d = dir(code)
                if (d != null) {
                    if (d.second != 0) Diptico.moverConfig(d.second) else Diptico.ladoConfig(d.first)
                    return true
                }
                return esA(code) || esB(code) || code in botonesControl
            }
            if (arriba) {
                when {
                    esA(code) -> Diptico.aceptar()
                    esB(code) -> Diptico.atras()
                    code == KeyEvent.KEYCODE_BUTTON_START || code == KeyEvent.KEYCODE_BUTTON_R1 -> Diptico.siguientePaso()
                    code == KeyEvent.KEYCODE_BUTTON_L1 -> Diptico.pasoAnterior()
                    code in botonesControl -> Unit
                    else -> return false
                }
                return true
            }
            return false
        }

        // Select: en el inicio cambia entre carátulas y widgets; en menús y ventanas intercambia las pantallas.
        // Mantenerlo siempre intercambia las pantallas.
        if (code == KeyEvent.KEYCODE_BUTTON_SELECT && e.sesion == null && !modalVisible(e)) {
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

        // Tu espacio · modo edición de abajo (boceto S221pt…): pestañas Mover · Tamaño · Páginas · Fondo, nada encima.
        if (e.extra.espacioEdita >= 0 && e.editandoWidgets && e.panel == Panel.NINGUNO) return espacioEdicion(ev, e)

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

        // Colección viva · Tu espacio (boceto 10): L2 y R2 pasan del inicio a tus páginas de widgets y de vuelta.
        val cvLibre = !hayPanel && e.sesion == null && !e.editandoWidgets && cl.nikalbazu.diptico.ui.coleccionVivaActiva(e) && !e.ordenando
        if (cvLibre && (code == KeyEvent.KEYCODE_BUTTON_L2 || code == KeyEvent.KEYCODE_BUTTON_R2)) {
            if (abajo && ev.repeatCount == 0) Diptico.pasoEspacio(if (code == KeyEvent.KEYCODE_BUTTON_L2) -1 else 1)
            return true
        }
        // En Tu espacio: la Galería de widgets (su propia pantalla) se maneja con el control; sin ella, Y la abre,
        // X entra a editar y B vuelve al inicio.
        if (cvLibre && e.inicio == "widgets" && (e.widgetFoco < 0 || e.extra.galeria)) {
            val d = dir(code)
            val una = ev.repeatCount == 0
            if (e.extra.galeria) {
                when {
                    d != null -> { if (abajo) Diptico.moverTiendaEspacio(d.first, d.second, if (e.unaPantalla) 1 else 2); return true }
                    code == KeyEvent.KEYCODE_BUTTON_L1 || code == KeyEvent.KEYCODE_BUTTON_R1 -> {
                        if (abajo && una) Diptico.categoriaEspacio(if (code == KeyEvent.KEYCODE_BUTTON_L1) -1 else 1); return true
                    }
                    code == KeyEvent.KEYCODE_BUTTON_X -> { if (abajo && una) Diptico.cambiarTamGaleria(1); return true }
                    // Select no abandona la Galería ni cambia carpetas/juegos a sus espaldas.
                    code == KeyEvent.KEYCODE_BUTTON_SELECT -> return true
                    esA(code) -> { if (abajo && una) Diptico.agregarDesdeGaleria(); return true }
                    esB(code) || code == KeyEvent.KEYCODE_BUTTON_Y -> { if (abajo && una) Diptico.cerrarGaleriaWidgets(); return true }
                }
            } else when {
                d != null -> { if (abajo) Diptico.entrarWidgets(); return true }
                code == KeyEvent.KEYCODE_BUTTON_Y -> { if (abajo && una) Diptico.abrirGaleriaWidgets(); return true }
                code == KeyEvent.KEYCODE_BUTTON_X -> { if (abajo && una) Diptico.editarEspacio(); return true }
                // A sobre Tu espacio NO ejecuta el juego escondido: primero enfoca el widget.
                esA(code) -> {
                    if (arriba && !e.espacioVacio()) Diptico.entrarWidgets()
                    else if (abajo && una && e.espacioVacio()) Diptico.agregarSugeridos()
                    return true
                }
                esB(code) -> { if (abajo && una) Diptico.pasoEspacio(-(e.paginaInicio + 1)); return true }
            }
        }

        // R3 es el selector de vistas de JUEGOS, no de widgets: al estar en Tu espacio
        // la única galería válida es la de widgets. No dejarlo pasar al Home.
        if (cvLibre && e.inicio == "widgets" && code == KeyEvent.KEYCODE_BUTTON_THUMBR) {
            if (abajo && ev.repeatCount == 0 && !e.extra.galeria) Diptico.abrirGaleriaWidgets()
            return true
        }

        // Con un widget enfocado, X abre sus opciones y Y lleva a la Galería;
        // la navegación del estante nunca debe recibir esos botones por debajo.
        if (cvLibre && e.inicio == "widgets" && e.widgetFoco >= 0 &&
            (code == KeyEvent.KEYCODE_BUTTON_X || code == KeyEvent.KEYCODE_BUTTON_Y)) {
            if (abajo && ev.repeatCount == 0) {
                if (code == KeyEvent.KEYCODE_BUTTON_X) Diptico.editarEsteWidget(e.widgets.getOrNull(e.widgetFoco)?.tipo)
                else Diptico.abrirGaleriaWidgets()
            }
            return true
        }

        // Moviendo un juego de lugar: la cruceta lo corre; A, B o Start terminan.
        if (e.ordenando && !hayPanel) {
            if (abajo) {
                when (code) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> Diptico.moverJuegoDeLugar(-1)
                    KeyEvent.KEYCODE_DPAD_RIGHT -> Diptico.moverJuegoDeLugar(1)
                    KeyEvent.KEYCODE_DPAD_UP -> Diptico.moverJuegoDeLugar(if (e.vista == "cuadricula" || e.vista == "iconos" || e.vista == "lista") -columnasEstante(e) else -1)
                    KeyEvent.KEYCODE_DPAD_DOWN -> Diptico.moverJuegoDeLugar(if (e.vista == "cuadricula" || e.vista == "iconos" || e.vista == "lista") columnasEstante(e) else 1)
                }
            }
            if (arriba && (esA(code) || esB(code) || code == KeyEvent.KEYCODE_BUTTON_START)) Diptico.terminarOrdenar()
            return true
        }

        // Inicio con widgets: ↑ desde el estante entra a los widgets; ahí la cruceta salta entre ellos, A abre y B vuelve.
        if (!hayPanel && e.sesion == null && e.inicio == "widgets" && !e.editandoWidgets && !e.eligiendoConsola) {
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
            } else if (abajo && ((e.unaPantalla && code in cruceta) || (code == KeyEvent.KEYCODE_DPAD_UP && e.vistaConsolas !in Diptico.MODOS_ELEGIR && e.vistaConsolas !in setOf("lado", "rueda") &&
                    (if (cl.nikalbazu.diptico.ui.esMosaico(e.vista)) e.indiceSel == 0 || (e.indiceSel - 1) % 2 == 0
                     else (e.vista != "cuadricula" && e.vista != "iconos" && e.vista != "lista") || e.indiceSel < columnasEstante(e))))
            ) {
                if (Diptico.entrarWidgets()) return true
            }
        }

        if (arriba && !hayPanel && e.sesion == null && e.filtro == "ocultos" && esB(code)) {
            if (!bLargo) Diptico.salirDeOcultos()
            bLargo = false
            return true
        }

        // Modo Sistemas, eligiendo consola: la cruceta y L1/R1 cambian de consola, A entra, Y elige un juego al azar.
        if (!hayPanel && e.sesion == null && e.eligiendoConsola) {
            // Ordenando carpetas (tiemblan): A toma o suelta, la cruceta mueve la tomada, X personaliza, B termina.
            if (e.extra.ordenandoCarpetas) {
                if (abajo) dir(code)?.let { (dx, dy) ->
                    if (e.extra.carpetaTomada != null) Diptico.moverCarpetaTomada(dx, dy, carrilesCarpetas)
                    else if (e.vistaConsolas in Diptico.VISTAS_CARPETAS) Diptico.moverCarpetas(dx, dy, carrilesCarpetas)
                    else Diptico.cambiarConsola(if (dx + dy < 0) -1 else 1)
                    return true
                }
                if (arriba) when {
                    esA(code) -> Diptico.tomarOSoltarCarpeta()
                    esB(code) || code == KeyEvent.KEYCODE_BACK -> Diptico.terminarOrdenCarpetas()
                    code == KeyEvent.KEYCODE_BUTTON_X -> Diptico.abrirEditarConsola(e.consolaSel)
                }
                return true
            }
            // Mantener A sobre una carpeta: entra al modo para ordenarlas (con la carpeta ya tomada).
            if (abajo && esA(code) && ev.repeatCount == 1 && e.vistaConsolas in Diptico.VISTAS_CARPETAS && e.consolaSel != "todo") {
                consumidas += code
                Diptico.empezarOrdenCarpetas(tomar = e.consolaSel)
                return true
            }
            if (abajo) {
                dir(code)?.let { (dx, dy) ->
                    if (e.vistaConsolas in Diptico.VISTAS_CARPETAS) {
                        Diptico.moverCarpetas(dx, dy, carrilesCarpetas)
                    } else Diptico.cambiarConsola(if (dx + dy < 0) -1 else 1)
                    return true
                }
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
                    if (!hayPanel && cl.nikalbazu.diptico.ui.esMosaico(e.vista)) Diptico.moverJuego(pasoMosaico(e, if (code == KeyEvent.KEYCODE_DPAD_LEFT) -1 else 0, if (code == KeyEvent.KEYCODE_DPAD_UP) -1 else 0))
                    else if (!hayPanel) Diptico.moverJuego(if (code == KeyEvent.KEYCODE_DPAD_UP && (e.vista == "cuadricula" || e.vista == "iconos" || e.vista == "lista")) -columnasEstante(e) else -1)
                    else Diptico.moverIndice(-1)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (!hayPanel && cl.nikalbazu.diptico.ui.esMosaico(e.vista)) Diptico.moverJuego(pasoMosaico(e, if (code == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else 0, if (code == KeyEvent.KEYCODE_DPAD_DOWN) 1 else 0))
                    else if (!hayPanel) Diptico.moverJuego(if (code == KeyEvent.KEYCODE_DPAD_DOWN && (e.vista == "cuadricula" || e.vista == "iconos" || e.vista == "lista")) columnasEstante(e) else 1)
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
                    if (!selectLargo && !hayPanel) {
                        // Defensa: Select no cambia a Widgets cuando se muestran los juegos
                        // o carpetas CV, incluso si antes no se consumió el evento.
                        if (cl.nikalbazu.diptico.ui.coleccionVivaActiva(e) && e.inicio != "widgets") {
                            if (e.enSistemas) Diptico.siguienteVistaCarpetas() else Diptico.siguienteVistaColeccion()
                        } else Diptico.alternarInicio()
                    }
                    selectLargo = false
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_THUMBL -> { if (!hayPanel) Diptico.abrirBuscar(); return true }
                KeyEvent.KEYCODE_BUTTON_THUMBR -> { if (!hayPanel) Diptico.abrirGaleria("vistas_juegos"); return true }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2 -> return true
            }
        }
        return false
    }

    /** Columnas de la vista de cuadrícula/lista (la interfaz avisa cuántas hay). */
    @Volatile
    var columnasCuadricula = 4

    /**
     * Vista Mosaicos: el juego 0 es el mosaico grande (ocupa dos filas); del 1 en adelante van de a dos por
     * columna (arriba el impar, abajo el par). Devuelve cuánto moverse en la lista para la dirección pedida.
     */
    @Volatile
    var mosaicosUnaFila = false

    fun pasoMosaico(e: Estado, dx: Int, dy: Int): Int {
        val i = e.indiceSel
        val n = e.visibles.size
        if (n <= 1) return 0
        // Con poco alto (una pantalla) los mosaicos van en una sola fila: izquierda y derecha nomás.
        if (mosaicosUnaFila) return if (dx != 0) (i + dx).coerceIn(0, n - 1) - i else 0
        if (i == 0) return if (dx > 0) 1 else 0
        val fila = (i - 1) % 2
        val destino = when {
            dx > 0 -> minOf(i + 2, n - 1)
            dx < 0 -> if (i <= 2) 0 else i - 2
            dy > 0 -> if (fila == 0 && i + 1 < n) i + 1 else i
            dy < 0 -> if (fila == 1) i - 1 else i
            else -> i
        }
        return destino - i
    }
    private fun columnasEstante(e: Estado) = if (e.vista == "lista") 1 else columnasCuadricula

    @Volatile
    var columnasBuscar = 4
    var columnasImagenes = 4
    var columnasGaleria = 3
    var columnasApps = 5
    @Volatile
    var carrilesCarpetas = 3

    @Volatile
    var columnasMenu = 3

    /** Paneles que se recorren en lista o grilla. */
    private fun panelEnGrilla(ev: KeyEvent, e: Estado): Boolean {
        val code = ev.keyCode
        val abajo = ev.action == KeyEvent.ACTION_DOWN
        val arriba = ev.action == KeyEvent.ACTION_UP
        // Modal de emulador por juego: captura TODA la entrada antes que el panel y el Home.
        if (e.panel == Panel.RAPIDO && e.desplegable >= 0) {
            if (abajo) when (code) {
                KeyEvent.KEYCODE_DPAD_UP -> Diptico.moverIndice(-1)
                KeyEvent.KEYCODE_DPAD_DOWN -> Diptico.moverIndice(1)
            }
            if (arriba) when {
                esA(code) -> Diptico.elegirDesplegable(e.desplegableSel)
                esB(code) || code == KeyEvent.KEYCODE_BUTTON_START -> Diptico.cerrarDesplegable()
            }
            return true
        }
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
                        Panel.ASISTENTE -> if (code == KeyEvent.KEYCODE_BUTTON_L1) Diptico.hablarConBisa()
                        Panel.FICHA -> Diptico.cambiarPestanaFicha(d)
                        Panel.BUSCAR -> Diptico.cambiarFiltro(d)
                        Panel.IMAGENES -> Diptico.cambiarFuenteImagen(d)
                        Panel.GALERIA -> Diptico.cambiarPestanaGaleria(d)
                        Panel.APPS -> Diptico.cambiarPestanaApps(d)
                        Panel.CENTRO -> Diptico.cambiarCortina(if (d < 0) "avisos" else "ajustes")
                        Panel.EDITAR_CONSOLA -> Diptico.seccionEditarConsola(d)
                        else -> Unit
                    }
                }
                KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2 -> if (ev.repeatCount == 0 && e.panel == Panel.BUSCAR) {
                    Diptico.cambiarOrden(if (code == KeyEvent.KEYCODE_BUTTON_L2) -1 else 1)
                } else if (ev.repeatCount == 0 && e.panel == Panel.GALERIA && e.galeria == "temas") {
                    Diptico.cambiarPreviaTema(if (code == KeyEvent.KEYCODE_BUTTON_L2) -1 else 1)
                }
                else -> {
                    // En las apps: mantener A elige en qué pantalla abrirla.
                    if (esA(code) && e.panel == Panel.APPS) {
                        if (ev.repeatCount == 0) aLargo = false
                        else if (!aLargo && ev.eventTime - ev.downTime > 550) {
                            Diptico.appsVisibles(e).getOrNull(e.indice)?.let { aLargo = true; Diptico.abrirOpcionesApp(it.paquete) }
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
                    Panel.APPS -> Diptico.appsVisibles(e).getOrNull(e.indice)?.let { Diptico.abrirOpcionesApp(it.paquete) }
                    Panel.GALERIA -> {
                        if (e.galeria == "accesos_cortina") Diptico.itemsGaleria(e).getOrNull(e.indice)?.takeIf { it.enUso }?.let { Diptico.subirAcceso(it.id) }
                        else if (e.galeria == "temas") Diptico.personalizarTemaElegido()
                    }
                    else -> Unit
                }
                code == KeyEvent.KEYCODE_BUTTON_Y -> when (e.panel) {
                    Panel.CARPETAS -> Diptico.quitarCarpeta()
                    Panel.CENTRO -> if (e.cortina == "avisos") cl.nikalbazu.diptico.audio.Avisos.descartarTodas()
                    Panel.ASISTENTE -> Diptico.borrarChat()
                    Panel.IMAGENES -> Diptico.abrirTexto("buscar_imagen", e.nombreImagen)
                    Panel.GALERIA -> {
                        if (e.galeria == "ia") Diptico.itemsGaleria(e).getOrNull(e.indice)?.let { Diptico.abrirPaginaClave(it.id) }
                        else if (e.galeria == "temas") Diptico.itemsGaleria(e).getOrNull(e.indice)?.tema?.let { Diptico.probarTema(it.id) }
                    }
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
    /**
     * Modo edición de Tu espacio. Cruceta = lo de la pestaña (mover el widget, tamaño de la página, ordenar páginas, elegir fondo),
     * L1/R1 = otro widget, L2/R2 = otra página, Y = siguiente pestaña, A = agrandar / página nueva / tu imagen,
     * X = achicar / quitar página, B o Start = listo.
     */
    private fun espacioEdicion(ev: KeyEvent, e: Estado): Boolean {
        val code = ev.keyCode
        if (ev.action != KeyEvent.ACTION_DOWN) return true
        val una = ev.repeatCount == 0
        dir(code)?.let { (dx, dy) -> Diptico.crucetaEspacio(dx, dy); return true }
        when {
            code == KeyEvent.KEYCODE_BUTTON_L1 || code == KeyEvent.KEYCODE_BUTTON_R1 -> if (una) {
                val d = if (code == KeyEvent.KEYCODE_BUTTON_L1) -1 else 1
                if (e.extra.espacioEdita == 0) Diptico.otroWidgetEspacio(d) else Diptico.pestanaEspacio(d)
            }
            code == KeyEvent.KEYCODE_BUTTON_L2 || code == KeyEvent.KEYCODE_BUTTON_R2 -> if (una) {
                Diptico.irAPagina(e.paginaInicio + if (code == KeyEvent.KEYCODE_BUTTON_L2) -1 else 1); cl.nikalbazu.diptico.audio.Sonidos.mover()
            }
            code == KeyEvent.KEYCODE_BUTTON_Y -> if (una) Diptico.pestanaEspacio(1)
            esA(code) -> if (una) Diptico.aEspacio()
            code == KeyEvent.KEYCODE_BUTTON_X -> if (una) Diptico.xEspacio()
            esB(code) || code == KeyEvent.KEYCODE_BUTTON_START -> if (una) Diptico.terminarEspacio()
        }
        return true
    }

    private fun estudio(ev: KeyEvent, e: Estado): Boolean {
        val tiendaCV = e.estudioPestana == 1 && e.extra.coleccionViva && !e.unaPantalla && !e.estudioMover
        val menuCV = e.estudioPestana == 0 && e.extra.coleccionViva && !e.estudioMover
        val code = ev.keyCode
        val abajo = ev.action == KeyEvent.ACTION_DOWN
        val arriba = ev.action == KeyEvent.ACTION_UP
        if (arriba && esA(code) && aLargoEstudio) {
            aLargoEstudio = false
            return true
        }
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
                // Tienda de widgets de Colección viva: la cruceta recorre la grilla de 2 y L1/R1 cambian de categoría.
                tiendaCV && d != null && e.indice > 0 -> Diptico.moverIndice(if (d.second != 0) d.second * 2 else d.first)
                d != null && d.second != 0 -> Diptico.moverIndice(d.second)
                d != null -> Diptico.cambiarControl(d.first)
                // Menú de un widget (Colección viva): L1/R1 pasan de pestaña (Qué muestra, Tamaño y lugar, Aspecto, Más).
                menuCV && (code == KeyEvent.KEYCODE_BUTTON_L1 || code == KeyEvent.KEYCODE_BUTTON_R1) && ev.repeatCount == 0 ->
                    Diptico.pasoPestanaWidget(if (code == KeyEvent.KEYCODE_BUTTON_L1) -1 else 1)
                tiendaCV && code == KeyEvent.KEYCODE_BUTTON_L1 && ev.repeatCount == 0 -> Diptico.elegirCategoriaWidgets((e.extra.tiendaCat - 1).mod(CATEGORIAS_WIDGETS.size))
                tiendaCV && code == KeyEvent.KEYCODE_BUTTON_R1 && ev.repeatCount == 0 -> Diptico.elegirCategoriaWidgets((e.extra.tiendaCat + 1).mod(CATEGORIAS_WIDGETS.size))
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
                // Con el menú de un widget, B es «Listo»: guarda y cierra el menú.
                esB(code) -> if (menuCV) Diptico.terminarEdicion() else Diptico.zonaEstudio("grilla")
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
                // ◀ ▶ cambian el valor de la opción marcada en su misma fila (lista o Sí/No); si no tiene, ◀ vuelve a las secciones.
                KeyEvent.KEYCODE_DPAD_LEFT -> if (e.desplegable >= 0) Unit else if (e.extra.subOpcion >= 0) Diptico.usarSubOpcion(menos = true)
                    else if (e.enOpciones && Diptico.cambiarEnFila(-1)) Unit else if (e.enOpciones && ev.repeatCount == 0) Diptico.columnaMenu(false)
                KeyEvent.KEYCODE_DPAD_RIGHT -> if (e.desplegable >= 0) Unit else if (e.extra.subOpcion >= 0) Diptico.usarSubOpcion()
                    else if (e.enOpciones) Diptico.cambiarEnFila(1) else if (ev.repeatCount == 0) Diptico.columnaMenu(true)
                KeyEvent.KEYCODE_BUTTON_L1 -> if (ev.repeatCount == 0 && e.extra.subOpcion < 0) Diptico.saltarSeccion(-1, envolver = true)
                KeyEvent.KEYCODE_BUTTON_R1 -> if (ev.repeatCount == 0 && e.extra.subOpcion < 0) Diptico.saltarSeccion(1, envolver = true)
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
        if (e.extra.emusDescarga != null) return
        // Si existe un panel, el stick actúa exclusivamente como cruceta DEL PANEL.
        if (modalVisible(e) && !e.editandoWidgets) {
            if (libre) pulsar(d, if (repetido) 1 else 0)
            return
        }
        // Prioridad absoluta de Tu espacio: ambos sticks se quedan en widgets, Galería
        // o pestañas de edición. No dejan pasar movimientos al estante de atrás.
        val enEspacioCV = e.extra.coleccionViva && e.inicio == "widgets" && e.sesion == null &&
            e.panel == Panel.NINGUNO && !e.conTeclado && !e.ordenando
        if (enEspacioCV && e.editandoWidgets && e.extra.espacioEdita >= 0) {
            if (libre) pulsar(d, 0) // Mover · Tamaño · Páginas · Fondo
            return
        }
        if (enEspacioCV && !e.editandoWidgets) {
            if (libre) {
                when {
                    e.extra.galeria -> pulsar(d, 0)
                    e.widgetFoco < 0 -> Diptico.entrarWidgets()
                    else -> {
                        Diptico.moverFocoWidget(dx, dy)
                        cl.nikalbazu.diptico.audio.Sonidos.mover()
                    }
                }
            }
            return
        }
        // En el menú de un widget, ambos sticks recorren sus opciones, no la grilla.
        if (libre && e.editandoWidgets && e.panel == Panel.ESTUDIO &&
            e.estudioZona == "panel" && e.burbuja == null) {
            pulsar(d, 0)
            return
        }
        // Editor tradicional: conserva las preferencias de sticks de cada usuario.
        if (libre && e.editandoWidgets && (e.panel == Panel.NINGUNO || e.panel == Panel.ESTUDIO) && e.burbuja == null) {
            val desliza = if (modo == "clasico") !izquierdo else deWidgets
            if (desliza) {
                if (dy != 0) Diptico.elegirWidgetDir(0, dy) else if (!repetido) Diptico.cambiarPagina(dx)
                return
            }
            if (e.panel == Panel.NINGUNO || e.estudioZona == "grilla") { pulsar(d, 0); return }
        }
        if (libre && e.panel == Panel.NINGUNO && e.eligiendoConsola && !e.editandoWidgets) {
            pulsar(d, 0)
            return
        }
        val enInicio = libre && e.inicio == "widgets" && e.panel == Panel.NINGUNO && !e.editandoWidgets && !e.ordenando
        if (enInicio && deWidgets) {
            if (e.widgetFoco < 0) Diptico.entrarWidgets() else { Diptico.moverFocoWidget(dx, dy); cl.nikalbazu.diptico.audio.Sonidos.mover() }
            return
        }
        if (enInicio && deJuegos) {
            // Ni el stick izquierdo ni el derecho deben navegar el estante oculto bajo Widgets.
            if (e.widgetFoco < 0) Diptico.entrarWidgets()
            else Diptico.moverFocoWidget(dx, dy)
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
        // En la vitrina de cajas y en el inicio de Colección viva el stick derecho SOLO mueve la caja o la consola
        // (de forma continua, como tenerla en la mano); no cambia de consola ni desplaza.
        if (cl.nikalbazu.diptico.ui.stickDerechoGira(Diptico.estado.value)) {
            val gx = ev.getAxisValue(MotionEvent.AXIS_Z)
            val gy = ev.getAxisValue(MotionEvent.AXIS_RZ)
            cl.nikalbazu.diptico.ui.GiroCaja.x = if (kotlin.math.abs(gx) > 0.15f) gx else 0f
            cl.nikalbazu.diptico.ui.GiroCaja.y = if (kotlin.math.abs(gy) > 0.15f) gy else 0f
            stickDer.cambiar(null)
        } else {
            if (cl.nikalbazu.diptico.ui.GiroCaja.x != 0f || cl.nikalbazu.diptico.ui.GiroCaja.y != 0f) { cl.nikalbazu.diptico.ui.GiroCaja.x = 0f; cl.nikalbazu.diptico.ui.GiroCaja.y = 0f }
            stickDer.cambiar(direccion(ev.getAxisValue(MotionEvent.AXIS_Z), ev.getAxisValue(MotionEvent.AXIS_RZ)))
        }
        val hat = direccion(ev.getAxisValue(MotionEvent.AXIS_HAT_X), ev.getAxisValue(MotionEvent.AXIS_HAT_Y))
        if (hat != null && hat != dirHat) pulsar(hat, 0)
        dirHat = hat
        return true
    }

    private var dirHat: Int? = null
}



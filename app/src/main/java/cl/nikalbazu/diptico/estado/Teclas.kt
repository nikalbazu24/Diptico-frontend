package cl.nikalbazu.diptico.estado

import android.view.KeyEvent

/**
 * Traduce los botones del control a acciones de Díptico.
 * A juega (mantener = menú rápido), B vuelve, Y próximo paso, Select intercambia pantallas,
 * L1/R1 cambian de consola, L2/R2 saltan de a 5, Start abre el menú.
 */
object Teclas {
    private var aLargo = false

    private fun esA(code: Int) = code == KeyEvent.KEYCODE_BUTTON_A || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
    private fun esB(code: Int) = code == KeyEvent.KEYCODE_BUTTON_B || code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE

    private val botonesControl = setOf(
        KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT,
    )

    private val cruceta = setOf(
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
    )

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

        // Escribiendo (próximo paso o clave): el teclado manda, salvo B (cerrar) y Start (guardar).
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

        // Tutorial guiado.
        if (!e.configurado) {
            if (abajo) {
                when (code) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT -> {
                        Diptico.moverIndice(-1)
                        Diptico.marcarProbado("cruceta")
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        Diptico.moverIndice(1)
                        Diptico.marcarProbado("cruceta")
                        return true
                    }
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

        // Juego en curso y pantalla atenuada: cualquier botón la despierta.
        if (e.sesion != null && !e.despierta && e.panel == Panel.NINGUNO) {
            if (arriba) Diptico.despertar()
            return true
        }

        // Ficha del juego: pestañas con L1/R1 o ← →, opciones con ↑ ↓.
        if (e.panel == Panel.FICHA || e.panel == Panel.WIDGETS || e.panel == Panel.TRABAJOS) {
            if (abajo) {
                when (code) {
                    KeyEvent.KEYCODE_DPAD_UP -> Diptico.moverIndice(-1)
                    KeyEvent.KEYCODE_DPAD_DOWN -> Diptico.moverIndice(1)
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_BUTTON_L1 -> if (ev.repeatCount == 0) {
                        if (e.panel == Panel.FICHA) Diptico.cambiarPestanaFicha(-1) else if (e.panel == Panel.WIDGETS) Diptico.moverWidget(-1)
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_BUTTON_R1 -> if (ev.repeatCount == 0) {
                        if (e.panel == Panel.FICHA) Diptico.cambiarPestanaFicha(1) else if (e.panel == Panel.WIDGETS) Diptico.moverWidget(1)
                    }
                    else -> return esA(code) || esB(code) || code in botonesControl
                }
                return true
            }
            if (arriba) {
                when {
                    esA(code) -> Diptico.aceptar()
                    esB(code) || code == KeyEvent.KEYCODE_BUTTON_X || code == KeyEvent.KEYCODE_BUTTON_START -> Diptico.cerrarPanel()
                    code in botonesControl || code in cruceta -> Unit
                    else -> return false
                }
                return true
            }
            return false
        }

        // Menú principal: dos columnas (secciones y opciones).
        if (e.panel == Panel.MENU) {
            if (abajo) {
                when (code) {
                    KeyEvent.KEYCODE_DPAD_UP -> Diptico.moverIndice(-1)
                    KeyEvent.KEYCODE_DPAD_DOWN -> Diptico.moverIndice(1)
                    KeyEvent.KEYCODE_DPAD_RIGHT -> if (!e.enOpciones) Diptico.columnaMenu(true)
                    KeyEvent.KEYCODE_DPAD_LEFT -> if (e.enOpciones) Diptico.columnaMenu(false)
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
                    code in botonesControl || code in cruceta -> Unit
                    else -> return false
                }
                return true
            }
            return false
        }

        val hayPanel = e.panel != Panel.NINGUNO

        if (abajo) {
            when (code) {
                KeyEvent.KEYCODE_DPAD_LEFT -> { if (!hayPanel) Diptico.moverJuego(-1); return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { if (!hayPanel) Diptico.moverJuego(1); return true }
                KeyEvent.KEYCODE_DPAD_UP -> { if (hayPanel) Diptico.moverIndice(-1); return true }
                KeyEvent.KEYCODE_DPAD_DOWN -> { if (hayPanel) Diptico.moverIndice(1); return true }
                KeyEvent.KEYCODE_BUTTON_L1 -> { if (!hayPanel && ev.repeatCount == 0) Diptico.cambiarConsola(-1); return true }
                KeyEvent.KEYCODE_BUTTON_R1 -> { if (!hayPanel && ev.repeatCount == 0) Diptico.cambiarConsola(1); return true }
                KeyEvent.KEYCODE_BUTTON_L2 -> { if (!hayPanel) Diptico.moverJuego(-5); return true }
                KeyEvent.KEYCODE_BUTTON_R2 -> { if (!hayPanel) Diptico.moverJuego(5); return true }
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
                return true
            }
            when (code) {
                KeyEvent.KEYCODE_BUTTON_Y -> { if (!hayPanel) Diptico.abrirPaso(); return true }
                KeyEvent.KEYCODE_BUTTON_X -> { if (!hayPanel) Diptico.abrirFicha(); return true }
                KeyEvent.KEYCODE_BUTTON_START -> {
                    if (hayPanel) Diptico.cerrarPanel() else Diptico.abrirMenu()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_SELECT -> { if (!hayPanel) Diptico.intercambiar(); return true }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2 -> return true
            }
        }
        return false
    }
}

package cl.nikalbazu.diptico.estado

import android.view.KeyEvent

/**
 * Traduce los botones de la Thor a acciones de Díptico.
 * A juega (mantener = menú rápido), B vuelve, Y próximo paso,
 * L1/R1 cambian de consola, L2/R2 saltan de a 5, Start abre ajustes.
 */
object Teclas {
    private var aLargo = false

    private fun esA(code: Int) = code == KeyEvent.KEYCODE_BUTTON_A || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
    private fun esB(code: Int) = code == KeyEvent.KEYCODE_BUTTON_B || code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE

    private val botonesControl = setOf(
        KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT,
    )

    fun manejar(ev: KeyEvent): Boolean {
        val e = Diptico.estado.value
        val code = ev.keyCode
        val abajo = ev.action == KeyEvent.ACTION_DOWN
        val arriba = ev.action == KeyEvent.ACTION_UP

        // Animación de entrada: cualquier botón la salta.
        if (e.intro) {
            if (arriba) Diptico.terminarIntro()
            return true
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
                    esB(code) -> Diptico.pasoAnterior()
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

        // Escribiendo el próximo paso: el teclado manda, salvo B (cerrar) y Start (guardar).
        if (e.panel == Panel.PASO) {
            if (code == KeyEvent.KEYCODE_BUTTON_B || code == KeyEvent.KEYCODE_BACK) {
                if (arriba) Diptico.cerrarPanel()
                return true
            }
            if (code == KeyEvent.KEYCODE_BUTTON_START) {
                if (arriba) Diptico.guardarPaso()
                return true
            }
            return false
        }

        // Juego en curso y pantalla atenuada: cualquier botón la despierta.
        if (e.sesion != null && !e.despierta && e.panel == Panel.NINGUNO) {
            if (arriba) Diptico.despertar()
            return true
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
                    Diptico.abrirMenu()
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
                if (hayPanel) Diptico.cerrarPanel()
                return true
            }
            when (code) {
                KeyEvent.KEYCODE_BUTTON_Y -> { if (!hayPanel) Diptico.abrirPaso(); return true }
                KeyEvent.KEYCODE_BUTTON_X -> { if (!hayPanel) Diptico.avisar("La ficha del juego llega en la próxima versión"); return true }
                KeyEvent.KEYCODE_BUTTON_START -> {
                    if (e.panel == Panel.AJUSTES) Diptico.cerrarPanel() else if (!hayPanel) Diptico.abrirAjustes()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_SELECT -> return true
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2 -> return true
            }
        }
        return false
    }
}

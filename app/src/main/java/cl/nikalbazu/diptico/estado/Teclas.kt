package cl.nikalbazu.diptico.estado

import android.view.KeyEvent

/**
 * Traduce los botones de la Thor a acciones de Díptico.
 * A juega (mantener = menú rápido), B vuelve, X detalles, Y próximo paso,
 * L1/R1 cambian de consola, L2/R2 saltan de a 5, Start abre ajustes.
 */
object Teclas {
    private var aLargo = false

    private fun esA(code: Int) = code == KeyEvent.KEYCODE_BUTTON_A || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
    private fun esB(code: Int) = code == KeyEvent.KEYCODE_BUTTON_B || code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE

    fun manejar(ev: KeyEvent): Boolean {
        val e = Diptico.estado.value
        val code = ev.keyCode
        val abajo = ev.action == KeyEvent.ACTION_DOWN
        val arriba = ev.action == KeyEvent.ACTION_UP

        // Primer arranque: A hace la acción principal.
        if (!e.configurado) {
            if (!e.modoElegido && (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                if (abajo) Diptico.moverOpcionInicial(if (code == KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1)
                return true
            }
            if (esA(code)) {
                if (arriba) Diptico.accionConfiguracion()
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
            if (esB(code) || code == KeyEvent.KEYCODE_BUTTON_X || code == KeyEvent.KEYCODE_BUTTON_Y ||
                code == KeyEvent.KEYCODE_BUTTON_START || code == KeyEvent.KEYCODE_BUTTON_SELECT
            ) return true
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

package cl.nikalbazu.diptico.estado

import android.view.KeyEvent

/** Atajos reservados para el inicio. Los botones A, B, X e Y siguen libres. */
enum class AtajoInicio(val id: String, val titulo: String, val tecla: Int? = null, val doble: Boolean = false) {
    START_DOBLE("start_doble", "Doble Start", KeyEvent.KEYCODE_BUTTON_START, true),
    START_R3("start_r3", "Start + R3", KeyEvent.KEYCODE_BUTTON_THUMBR),
    START_L3("start_l3", "Start + L3", KeyEvent.KEYCODE_BUTTON_THUMBL),
    START_SELECT("start_select", "Start + Select", KeyEvent.KEYCODE_BUTTON_SELECT),
    L3_DOBLE("l3_doble", "Doble L3", KeyEvent.KEYCODE_BUTTON_THUMBL, true),
    R3_DOBLE("r3_doble", "Doble R3", KeyEvent.KEYCODE_BUTTON_THUMBR, true),
    NO("no", "Desactivado");

    companion object {
        fun leer(id: String, defecto: AtajoInicio) = entries.firstOrNull { it.id == id } ?: defecto
    }
}

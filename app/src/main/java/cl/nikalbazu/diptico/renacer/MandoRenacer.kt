package cl.nikalbazu.diptico.renacer

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs

/** El stick usa el mismo foco de Android que la cruceta. Al salir de la pantalla se detiene. */
internal class MandoRenacer(private val enviar: (KeyEvent) -> Boolean) {
    private val handler = Handler(Looper.getMainLooper())
    private var tecla: Int? = null
    private val repetir = object : Runnable {
        override fun run() { pulsar(); if (tecla != null) handler.postDelayed(this, 140) }
    }
    private fun pulsar() {
        val codigo = tecla ?: return
        val ahora = SystemClock.uptimeMillis()
        enviar(KeyEvent(ahora, ahora, KeyEvent.ACTION_DOWN, codigo, 0))
        enviar(KeyEvent(ahora, ahora, KeyEvent.ACTION_UP, codigo, 0))
    }
    fun mover(evento: MotionEvent): Boolean {
        if (!evento.isFromSource(InputDevice.SOURCE_JOYSTICK) || evento.action != MotionEvent.ACTION_MOVE) return false
        val x = evento.getAxisValue(MotionEvent.AXIS_X)
        val y = evento.getAxisValue(MotionEvent.AXIS_Y)
        val nueva = when {
            maxOf(abs(x), abs(y)) < .5f -> null
            abs(x) > abs(y) -> if (x > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
            else -> if (y > 0) KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP
        }
        if (nueva != tecla) {
            detener()
            tecla = nueva
            if (nueva != null) { pulsar(); handler.postDelayed(repetir, 320) }
        }
        return nueva != null
    }
    fun detener() { tecla = null; handler.removeCallbacks(repetir) }
}

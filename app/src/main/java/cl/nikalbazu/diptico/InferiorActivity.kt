package cl.nikalbazu.diptico

import cl.nikalbazu.diptico.estado.*

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.ui.LocalEscala
import cl.nikalbazu.diptico.ui.Intercambio
import cl.nikalbazu.diptico.ui.Lienzo
import cl.nikalbazu.diptico.ui.Raiz
import cl.nikalbazu.diptico.ui.Rol
import cl.nikalbazu.diptico.ui.Texto
import kotlinx.coroutines.launch

/** La segunda pantalla (la de abajo en la Thor): consolas, estante y compañero de partida. */
class InferiorActivity : BaseActivity() {

    companion object {
        @Volatile
        var viva: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viva = true
        setContent {
            val e by Diptico.estado.collectAsState()
            val mi = miDisplay()
            val t = e.tamTexto
            val z = e.zoomAbajo
            when {
                !e.configurado -> Lienzo(true, Modifier.fillMaxSize(), t) { Raiz(e, Rol.INFERIOR, mi) }
                e.unaPantalla -> Lienzo(true, Modifier.fillMaxSize(), t) {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        Texto(
                            "Díptico está en modo una pantalla · cámbialo en el menú", 12f, color = Color(0xFF2A2838),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = LocalEscala.current.d(16)),
                        )
                    }
                }
                else -> Lienzo(e.oscuro, Modifier.fillMaxSize(), t, z) {
                    Intercambio(Diptico.intercambioEfectivo(e), esArriba = false) { inter ->
                        if (inter) Raiz(e, Rol.SUPERIOR, mi, rolIntro = Rol.INFERIOR) else Raiz(e, Rol.INFERIOR, mi)
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Diptico.estado.collect { e ->
                    if (e.modoPantallas == "una_libre" && e.modoElegido) finish() else aplicarBrillo(e)
                }
            }
        }
    }

    override fun onDestroy() {
        viva = false
        super.onDestroy()
    }

    /**
     * Mientras juegas en la otra pantalla, esta se atenúa (sin tocar el brillo general).
     * En modo "abajo en negro" queda al mínimo.
     */
    private fun aplicarBrillo(e: Estado) {
        val s = e.sesion
        val lupa = s != null && s.displayId != miDisplay() && !s.dosPantallas && e.lupaActiva && e.datos[s.clave]?.lupa == true
        // Con la lupa a la vista, esta pantalla no le quita el control al juego aunque la toques.
        val sinFoco = window.attributes.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0
        if (lupa != sinFoco) {
            if (lupa) window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        }
        val usandoBisa = e.panel == cl.nikalbazu.diptico.estado.Panel.ASISTENTE ||
            (e.panel == cl.nikalbazu.diptico.estado.Panel.CLAVE && e.textoPara in setOf("asistente", "bisa_paso"))
        val atenuarJuego = !lupa && s != null && s.displayId != miDisplay() && !s.dosPantallas &&
            !e.despierta && !Diptico.intercambioEfectivo(e) && !usandoBisa
        val valor = when {
            !e.configurado -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            e.unaPantalla -> 0.01f
            e.protector -> 0.06f
            atenuarJuego -> if (e.atenuacion >= 1f) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else e.atenuacion
            else -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        val lp = window.attributes
        if (lp.screenBrightness != valor) {
            lp.screenBrightness = valor
            window.attributes = lp
        }
    }
}



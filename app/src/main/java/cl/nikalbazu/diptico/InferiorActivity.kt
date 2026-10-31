package cl.nikalbazu.diptico

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.ui.Lienzo
import cl.nikalbazu.diptico.ui.PantallaInferior
import cl.nikalbazu.diptico.ui.PantallaSuperior
import kotlinx.coroutines.launch

/** Pantalla de abajo de la Thor: consolas, estante y compañero de partida. */
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
            val miPantalla = miDisplay()
            if (e.intercambiar) {
                Lienzo(960f, 540f, e.oscuro, Modifier.fillMaxSize()) { PantallaSuperior(e) }
            } else {
                Lienzo(620f, 540f, e.oscuro, Modifier.fillMaxSize()) { PantallaInferior(e, miPantalla) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Diptico.estado.collect { aplicarBrillo(it) }
            }
        }
    }

    override fun onDestroy() {
        viva = false
        super.onDestroy()
    }

    /** Mientras juegas en la pantalla de arriba, esta se atenúa (sin tocar el brillo general). */
    private fun aplicarBrillo(e: Estado) {
        val s = e.sesion
        val atenuar = s != null && s.displayId != miDisplay() && !s.dosPantallas && !e.despierta && !e.intercambiar
        val valor = if (atenuar) 0.04f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        val lp = window.attributes
        if (lp.screenBrightness != valor) {
            lp.screenBrightness = valor
            window.attributes = lp
        }
    }
}

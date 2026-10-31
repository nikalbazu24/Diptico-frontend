package cl.nikalbazu.diptico

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
import cl.nikalbazu.diptico.ui.Lienzo
import cl.nikalbazu.diptico.ui.Raiz
import cl.nikalbazu.diptico.ui.Rol
import cl.nikalbazu.diptico.ui.Texto
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
            when {
                e.modoPantallas == "una_negra" -> Lienzo(620f, 540f, true, Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        Texto(
                            "Díptico está en modo una pantalla · cámbialo en Ajustes", 11f, color = Color(0xFF2A2838),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = LocalEscala.current.d(16)),
                        )
                    }
                }
                e.intercambiar && e.configurado -> Lienzo(960f, 540f, e.oscuro, Modifier.fillMaxSize()) {
                    Raiz(e, Rol.SUPERIOR, miPantalla, rolIntro = Rol.INFERIOR)
                }
                else -> Lienzo(620f, 540f, e.oscuro, Modifier.fillMaxSize()) { Raiz(e, Rol.INFERIOR, miPantalla) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Diptico.estado.collect { e ->
                    if (e.modoPantallas == "una_libre") {
                        finish()
                    } else {
                        aplicarBrillo(e)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        viva = false
        super.onDestroy()
    }

    /**
     * Mientras juegas en la pantalla de arriba, esta se atenúa (sin tocar el brillo general).
     * En modo "abajo en negro" queda al mínimo.
     */
    private fun aplicarBrillo(e: Estado) {
        val s = e.sesion
        val atenuarJuego = s != null && s.displayId != miDisplay() && !s.dosPantallas && !e.despierta && !e.intercambiar
        val valor = when {
            e.modoPantallas == "una_negra" -> 0.01f
            atenuarJuego -> 0.04f
            else -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        val lp = window.attributes
        if (lp.screenBrightness != valor) {
            lp.screenBrightness = valor
            window.attributes = lp
        }
    }
}

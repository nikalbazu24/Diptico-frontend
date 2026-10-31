package cl.nikalbazu.diptico

import android.app.ActivityOptions
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cl.nikalbazu.diptico.emu.Pantallas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.ui.Lienzo
import cl.nikalbazu.diptico.ui.PantallaInferior
import cl.nikalbazu.diptico.ui.PantallaSuperior
import cl.nikalbazu.diptico.ui.PantallaUnica
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Pantalla de arriba de la Thor (la principal del sistema).
 * En modo una pantalla muestra todo aquí. En un teléfono vertical, las dos partes una sobre otra.
 */
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val e by Diptico.estado.collectAsState()
            val hayInferior = Pantallas.inferior(this) != null
            val vertical = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
            val miPantalla = miDisplay()
            when {
                !hayInferior && vertical -> Column(Modifier.fillMaxSize().background(Color.Black)) {
                    Lienzo(960f, 540f, e.oscuro, Modifier.fillMaxWidth().aspectRatio(16f / 9f)) { PantallaSuperior(e) }
                    Lienzo(620f, 540f, e.oscuro, Modifier.fillMaxWidth().weight(1f)) { PantallaInferior(e, miPantalla) }
                }
                !hayInferior || e.unaPantalla -> Lienzo(960f, 540f, e.oscuro, Modifier.fillMaxSize()) { PantallaUnica(e) }
                e.intercambiar -> Lienzo(620f, 540f, e.oscuro, Modifier.fillMaxSize()) { PantallaInferior(e, miPantalla) }
                else -> Lienzo(960f, 540f, e.oscuro, Modifier.fillMaxSize()) { PantallaSuperior(e) }
            }
        }
        // Si cambias a dos pantallas (o a "abajo en negro") desde Ajustes, se abre la de abajo.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Diptico.estado.map { it.modoPantallas }.distinctUntilChanged().collect { modo ->
                    if (modo != "una_libre") abrirPantallaInferior()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Diptico.estado.value.modoPantallas != "una_libre") abrirPantallaInferior()
    }

    private fun abrirPantallaInferior() {
        val inferior = Pantallas.inferior(this) ?: return
        if (InferiorActivity.viva) return
        val intent = Intent(this, InferiorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opciones = ActivityOptions.makeBasic().setLaunchDisplayId(inferior.displayId)
        try {
            startActivity(intent, opciones.toBundle())
        } catch (e: Exception) {
            Diptico.avisar("No pude abrir la pantalla de abajo")
        }
    }
}

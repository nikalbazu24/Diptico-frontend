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
import cl.nikalbazu.diptico.ui.Raiz
import cl.nikalbazu.diptico.ui.Rol
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
            val mi = miDisplay()
            when {
                !hayInferior && vertical -> Column(Modifier.fillMaxSize().background(Color.Black)) {
                    Lienzo(960f, 540f, e.oscuro, Modifier.fillMaxWidth().aspectRatio(16f / 9f)) { Raiz(e, Rol.SUPERIOR, mi) }
                    Lienzo(620f, 540f, e.oscuro, Modifier.fillMaxWidth().weight(1f)) { Raiz(e, Rol.INFERIOR, mi) }
                }
                !hayInferior || e.unaPantalla -> Lienzo(960f, 540f, e.oscuro, Modifier.fillMaxSize()) { Raiz(e, Rol.UNICA, mi) }
                e.intercambiar && e.configurado -> Lienzo(620f, 540f, e.oscuro, Modifier.fillMaxSize()) {
                    Raiz(e, Rol.INFERIOR, mi, rolIntro = Rol.SUPERIOR)
                }
                else -> Lienzo(960f, 540f, e.oscuro, Modifier.fillMaxSize()) { Raiz(e, Rol.SUPERIOR, mi) }
            }
        }
        // Si cambias a dos pantallas (o a "abajo en negro") se abre la de abajo.
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

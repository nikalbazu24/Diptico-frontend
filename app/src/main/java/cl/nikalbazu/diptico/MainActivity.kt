package cl.nikalbazu.diptico

import android.app.ActivityOptions
import android.content.Intent
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
import cl.nikalbazu.diptico.emu.Pantallas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.ui.Lienzo
import cl.nikalbazu.diptico.ui.PantallaInferior
import cl.nikalbazu.diptico.ui.PantallaSuperior

/**
 * Pantalla de arriba de la Thor (la principal del sistema).
 * Si el aparato tiene una sola pantalla (por ejemplo un teléfono), muestra las dos partes una sobre otra.
 */
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val e by Diptico.estado.collectAsState()
            val hayInferior = Pantallas.inferior(this) != null
            val miPantalla = miDisplay()
            when {
                !hayInferior -> Column(Modifier.fillMaxSize().background(Color.Black)) {
                    Lienzo(960f, 540f, e.oscuro, Modifier.fillMaxWidth().aspectRatio(16f / 9f)) { PantallaSuperior(e) }
                    Lienzo(620f, 540f, e.oscuro, Modifier.fillMaxWidth().weight(1f)) { PantallaInferior(e, miPantalla) }
                }
                e.intercambiar -> Lienzo(620f, 540f, e.oscuro, Modifier.fillMaxSize()) { PantallaInferior(e, miPantalla) }
                else -> Lienzo(960f, 540f, e.oscuro, Modifier.fillMaxSize()) { PantallaSuperior(e) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        abrirPantallaInferior()
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

package cl.nikalbazu.diptico

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle

/**
 * Inicio de la segunda pantalla: cuando Android abre "el inicio" en la pantalla de abajo (al prender o al apretar Inicio),
 * trae la pantalla de abajo de Díptico y se cierra. Así las dos pantallas siempre vuelven a Díptico.
 */
class InicioAbajo : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val id = display?.displayId ?: 0
            val i = Intent(this, InferiorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i, ActivityOptions.makeBasic().setLaunchDisplayId(id).toBundle())
        } catch (_: Exception) {
        }
        finish()
        overridePendingTransition(0, 0)
    }
}

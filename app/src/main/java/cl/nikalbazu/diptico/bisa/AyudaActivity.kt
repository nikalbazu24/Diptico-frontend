package cl.nikalbazu.diptico.bisa

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.Display
import cl.nikalbazu.diptico.InferiorActivity
import cl.nikalbazu.diptico.MainActivity
import cl.nikalbazu.diptico.emu.Pantallas
import cl.nikalbazu.diptico.estado.Diptico

/**
 * Al tocar el aviso de DIPPO mientras juegas: la abre en la pantalla libre si hay una,
 * o como ventana flotante encima del juego (DS, 3DS, modo una pantalla). No se ve: decide y se cierra.
 */
class AyudaActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Diptico.iniciar(this)
        val e = Diptico.estado.value
        val s = e.sesion
        val inferior = Pantallas.inferior(this)
        Diptico.prepararAyuda()
        when {
            s != null && !s.dosPantallas && !e.unaPantalla && inferior != null -> {
                // Pantalla libre: la que no tiene el juego.
                val libre = if (s.displayId == Display.DEFAULT_DISPLAY) inferior.displayId else Display.DEFAULT_DISPLAY
                val destino = if (libre == Display.DEFAULT_DISPLAY) MainActivity::class.java else InferiorActivity::class.java
                try {
                    startActivity(
                        Intent(this, destino).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                        ActivityOptions.makeBasic().setLaunchDisplayId(libre).toBundle(),
                    )
                } catch (_: Exception) {}
            }
            BurbujaBisa.puedeFlotar(this) -> BurbujaBisa.abrir(this)
            else -> {
                Diptico.avisar("Para ver a DIPPO encima del juego, activa su burbuja en Menú › Asistente")
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        finish()
    }
}

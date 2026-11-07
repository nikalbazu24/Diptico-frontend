package cl.nikalbazu.diptico

import android.app.Application
import cl.nikalbazu.diptico.estado.Diptico

class DipticoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Primero el reporte de fallas, para alcanzar a anotar cualquier caída desde el arranque.
        cl.nikalbazu.diptico.estado.Fallas.instalar(this)
        Diptico.iniciar(this)
    }
}

package cl.nikalbazu.diptico

import android.app.Application
import cl.nikalbazu.diptico.estado.Diptico

class DipticoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Diptico.iniciar(this)
    }
}

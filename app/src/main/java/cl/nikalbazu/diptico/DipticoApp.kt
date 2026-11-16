package cl.nikalbazu.diptico

import cl.nikalbazu.diptico.estado.*

import android.app.Application
import cl.nikalbazu.diptico.estado.Diptico

class DipticoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Primero el reporte de fallas, para alcanzar a anotar cualquier caída desde el arranque.
        cl.nikalbazu.diptico.estado.Fallas.instalar(this)
        // Modo seguro: si Díptico se cerró dos veces seguidas antes de los 30 s (también por caídas que no alcanzan
        // a dejar informe, como las del 3D), se apaga el 3D y se vuelve a la portada.
        val p = getSharedPreferences("arranque", 0)
        val seguidos = p.getInt("tempranos", 0) + 1
        p.edit().putInt("tempranos", seguidos).commit()
        if (seguidos >= 2) {
            cl.nikalbazu.diptico.ui.Motor3D.pausarPorSeguridad(this)
            getSharedPreferences("diptico", 0).edit().putString("inicio", "heroe").commit()
        }
        android.os.Handler(mainLooper).postDelayed({ p.edit().putInt("tempranos", 0).apply() }, 30_000L)
        Diptico.iniciar(this)
    }
}

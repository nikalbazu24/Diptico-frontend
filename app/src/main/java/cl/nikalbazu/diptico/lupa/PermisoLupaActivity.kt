package cl.nikalbazu.diptico.lupa

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import cl.nikalbazu.diptico.estado.Diptico

/** Pide a Android el permiso para copiar la pantalla de arriba (la lupa). No se ve: pregunta y se cierra. */
class PermisoLupaActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Diptico.iniciar(this)
        val mpm = getSystemService(MediaProjectionManager::class.java)
        // En Android 14+ se pide directamente la pantalla completa (no una sola app).
        val pedido = if (intent.getBooleanExtra("retoma", false)) mpm.createScreenCaptureIntent()
        else if (Build.VERSION.SDK_INT >= 34) mpm.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
        else mpm.createScreenCaptureIntent()
        try {
            @Suppress("DEPRECATION") startActivityForResult(pedido, 51)
        } catch (_: Exception) {
            if (intent.getBooleanExtra("retoma", false)) Diptico.retomaPermisoTerminado(intent.getBooleanExtra("luegoJugar", false))
            else Diptico.lupaRechazada(intent.getBooleanExtra("luegoJugar", false))
            finish()
        }
    }

    @Deprecated("API de Activity")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION") super.onActivityResult(requestCode, resultCode, data)
        if (intent.getBooleanExtra("retoma", false)) {
            if (resultCode == RESULT_OK && data != null) cl.nikalbazu.diptico.retoma.RetomaServicio.iniciar(this, resultCode, data, intent.getStringExtra("clave").orEmpty())
            Diptico.retomaPermisoTerminado(intent.getBooleanExtra("luegoJugar", false)); finish(); return
        }
        if (resultCode == RESULT_OK && data != null) {
            LupaServicio.iniciar(this, resultCode, data)
            Diptico.lupaPermitida(intent.getBooleanExtra("luegoJugar", false))
        } else {
            Diptico.lupaRechazada(intent.getBooleanExtra("luegoJugar", false))
        }
        finish()
    }
}


package cl.nikalbazu.diptico.bisa

import cl.nikalbazu.diptico.estado.*

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import cl.nikalbazu.diptico.R
import cl.nikalbazu.diptico.estado.Diptico

/**
 * DIPPO mientras juegas: un aviso fijo ("toca si te atascas") y, si el juego usa las dos pantallas
 * (DS, 3DS) o estás en modo una pantalla, una burbuja flotante encima del juego con su ventanita de ayuda.
 */
class BurbujaBisa : Service(), LifecycleOwner, SavedStateRegistryOwner {
    companion object {
        private const val CANAL_FIJO = "bisa_juego"
        private const val CANAL_AYUDA = "bisa_ayuda"
        private const val ID_FIJO = 7101
        private const val ID_AYUDA = 7102

        /** Empieza a acompañar un juego. [atascado]: se notó que reintenta mucho. [flotante]: no hay pantalla libre. */
        fun acompanar(ctx: Context, titulo: String, atascado: Boolean, flotante: Boolean) {
            val i = Intent(ctx, BurbujaBisa::class.java).setAction("acompanar")
                .putExtra("titulo", titulo).putExtra("atascado", atascado).putExtra("flotante", flotante)
            try { ctx.startForegroundService(i) } catch (_: Exception) {}
        }

        /** Abre la ventanita de ayuda encima del juego. */
        fun abrir(ctx: Context) {
            try { ctx.startService(Intent(ctx, BurbujaBisa::class.java).setAction("abrir")) } catch (_: Exception) {}
        }

        fun detener(ctx: Context) {
            try { ctx.stopService(Intent(ctx, BurbujaBisa::class.java)) } catch (_: Exception) {}
            ctx.getSystemService(NotificationManager::class.java)?.cancel(ID_AYUDA)
        }

        fun puedeFlotar(ctx: Context) = Settings.canDrawOverlays(ctx)

        fun pedirPermisoFlotante(ctx: Context) {
            try {
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (_: Exception) {}
        }

        private fun canales(ctx: Context) {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(NotificationChannel(CANAL_FIJO, "DIPPO mientras juegas", NotificationManager.IMPORTANCE_LOW))
            nm.createNotificationChannel(NotificationChannel(CANAL_AYUDA, "DIPPO: ofrecer ayuda", NotificationManager.IMPORTANCE_HIGH))
        }

        private fun alTocar(ctx: Context): PendingIntent = PendingIntent.getActivity(
            ctx, 1, Intent(ctx, AyudaActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private val ciclo = LifecycleRegistry(this)
    private val guardado = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = ciclo
    override val savedStateRegistry: SavedStateRegistry get() = guardado.savedStateRegistry

    private var vista: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    private var flotante = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        guardado.performRestore(null)
        ciclo.currentState = Lifecycle.State.CREATED
        canales(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "acompanar" -> {
                val titulo = intent.getStringExtra("titulo") ?: ""
                val atascado = intent.getBooleanExtra("atascado", false)
                flotante = intent.getBooleanExtra("flotante", false)
                val fija = Notification.Builder(this, CANAL_FIJO)
                    .setSmallIcon(R.drawable.ic_bisa)
                    .setContentTitle(cl.nikalbazu.diptico.estado.Idioma.t("DIPPO está contigo"))
                    .setContentText(cl.nikalbazu.diptico.estado.Idioma.t(if (titulo.isNotBlank()) "Toca si te atascas en $titulo" else "Toca si te atascas"))
                    .setContentIntent(alTocar(this))
                    .setOngoing(true)
                    .build()
                try {
                    if (Build.VERSION.SDK_INT >= 34) startForeground(ID_FIJO, fija, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    else startForeground(ID_FIJO, fija)
                } catch (_: Exception) {
                    stopSelf(); return START_NOT_STICKY
                }
                if (atascado) {
                    // Aviso que salta encima del juego.
                    val aviso = Notification.Builder(this, CANAL_AYUDA)
                        .setSmallIcon(R.drawable.ic_bisa)
                        .setContentTitle(cl.nikalbazu.diptico.estado.Idioma.t("¿Te está costando $titulo?"))
                        .setContentText(cl.nikalbazu.diptico.estado.Idioma.t("Llevas varios intentos seguidos. Toca y DIPPO te da una pista."))
                        .setContentIntent(alTocar(this))
                        .setAutoCancel(true)
                        .build()
                    try { getSystemService(NotificationManager::class.java)?.notify(ID_AYUDA, aviso) } catch (_: Exception) {}
                    if (flotante && puedeFlotar(this)) mostrar(expandida = false, alerta = true)
                }
            }
            "abrir" -> if (puedeFlotar(this)) mostrar(expandida = true, alerta = false)
        }
        return START_NOT_STICKY
    }

    /** Pone (o actualiza) la burbuja encima de todo. */
    private fun mostrar(expandida: Boolean, alerta: Boolean) {
        val wm = getSystemService(WindowManager::class.java) ?: return
        val dens = resources.displayMetrics.density
        val p = params ?: WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (16 * dens).toInt()
            y = (90 * dens).toInt()
        }
        params = p
        Diptico.burbujaExpandida(expandida, alerta)
        if (vista == null) {
            val v = ComposeView(this)
            v.setViewTreeLifecycleOwner(this)
            v.setViewTreeSavedStateRegistryOwner(this)
            v.setContent {
                VentanaBisa(
                    mover = { dx, dy ->
                        p.x += dx.toInt(); p.y += dy.toInt()
                        try { wm.updateViewLayout(v, p) } catch (_: Exception) {}
                    },
                    cerrar = { quitar() },
                )
            }
            ciclo.currentState = Lifecycle.State.RESUMED
            try { wm.addView(v, p) } catch (_: Exception) { return }
            vista = v
        }
    }

    private fun quitar() {
        val v = vista ?: return
        try { getSystemService(WindowManager::class.java)?.removeView(v) } catch (_: Exception) {}
        vista = null
        ciclo.currentState = Lifecycle.State.CREATED
    }

    override fun onDestroy() {
        quitar()
        ciclo.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }
}

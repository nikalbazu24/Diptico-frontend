package cl.nikalbazu.diptico.lupa

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Display
import android.view.Surface
import cl.nikalbazu.diptico.R
import cl.nikalbazu.diptico.estado.Diptico

/**
 * La lupa: copia en vivo la pantalla de arriba para mostrar ampliada una parte en la de abajo
 * (el minimapa de Genshin, la barra de vida, los subtítulos…).
 * Android pide permiso una vez; mientras la lupa esté encendida aparece el ícono de "compartiendo pantalla".
 */
class LupaServicio : Service() {
    companion object {
        private const val CANAL = "lupa"
        private const val ID = 7201

        @Volatile var activa = false
            private set
        /** Tamaño de la pantalla de arriba (lo que se copia). */
        @Volatile var ancho = 1920
            private set
        @Volatile var alto = 1080
            private set

        private var virtual: VirtualDisplay? = null
        private var proyeccion: MediaProjection? = null
        private var superficie: Surface? = null

        fun iniciar(ctx: Context, codigo: Int, datos: Intent) {
            val i = Intent(ctx, LupaServicio::class.java).putExtra("codigo", codigo).putExtra("datos", datos)
            try { ctx.startForegroundService(i) } catch (_: Exception) { Diptico.avisar("No pude encender la lupa") }
        }

        fun detener(ctx: Context) {
            try { ctx.stopService(Intent(ctx, LupaServicio::class.java)) } catch (_: Exception) {}
        }

        /** La vista de la pantalla de abajo entrega (o quita) dónde dibujar la copia. */
        fun ponerSuperficie(s: Surface?) {
            superficie = s
            try { virtual?.surface = s } catch (_: Exception) {}
        }

        fun tamanoArriba(ctx: Context): Point {
            val d = ctx.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
            val p = Point(1920, 1080)
            @Suppress("DEPRECATION") d?.getRealSize(p)
            return p
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(NotificationChannel(CANAL, "Lupa", NotificationManager.IMPORTANCE_LOW))
        val aviso = Notification.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_bisa)
            .setContentTitle("Lupa encendida")
            .setContentText("Ampliando una parte de la pantalla de arriba en la de abajo")
            .setOngoing(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID, aviso, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            else startForeground(ID, aviso)
        } catch (_: Exception) {
            stopSelf(); return START_NOT_STICKY
        }
        val codigo = intent?.getIntExtra("codigo", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        @Suppress("DEPRECATION") val datos: Intent? = intent?.getParcelableExtra("datos")
        if (codigo != Activity.RESULT_OK || datos == null) { stopSelf(); return START_NOT_STICKY }
        if (proyeccion != null) return START_NOT_STICKY
        try {
            val mpm = getSystemService(MediaProjectionManager::class.java)
            val p = mpm.getMediaProjection(codigo, datos) ?: run { stopSelf(); return START_NOT_STICKY }
            p.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { Handler(Looper.getMainLooper()).post { stopSelf() } }
            }, Handler(Looper.getMainLooper()))
            val t = tamanoArriba(this)
            ancho = t.x; alto = t.y
            val dpi = resources.displayMetrics.densityDpi
            virtual = p.createVirtualDisplay(
                "Diptico-lupa", ancho, alto, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, superficie, null, null,
            )
            proyeccion = p
            activa = true
            Diptico.lupaCambio(true)
        } catch (e: Exception) {
            Diptico.avisar("No pude encender la lupa")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        try { virtual?.release() } catch (_: Exception) {}
        try { proyeccion?.stop() } catch (_: Exception) {}
        virtual = null
        proyeccion = null
        activa = false
        Diptico.lupaCambio(false)
        super.onDestroy()
    }
}

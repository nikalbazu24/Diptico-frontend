package cl.nikalbazu.diptico.retoma

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.Surface
import cl.nikalbazu.diptico.R
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.lupa.LupaServicio
import java.io.File
import java.nio.ByteBuffer

/** Captura opcional de baja resolución. No accede al micrófono ni captura audio. */
class RetomaServicio : Service() {
    companion object {
        private const val CANAL = "retoma"
        @Volatile var activa = false
            private set
        fun iniciar(ctx: Context, codigo: Int, datos: Intent, clave: String) {
            val i = Intent(ctx, RetomaServicio::class.java).putExtra("codigo", codigo).putExtra("datos", datos).putExtra("clave", clave)
            try { ctx.startForegroundService(i) } catch (_: Exception) { Diptico.avisar("No pude preparar el clip de retoma") }
        }
        fun detener(ctx: Context) { try { ctx.stopService(Intent(ctx, RetomaServicio::class.java)) } catch (_: Exception) {} }
    }
    private val hilo = HandlerThread("Diptico-retoma")
    private lateinit var trabajo: Handler
    private var codec: MediaCodec? = null
    private var superficie: Surface? = null
    private var virtual: VirtualDisplay? = null
    private var proyeccion: MediaProjection? = null
    private var formato: MediaFormat? = null
    private val buffer = BufferRetoma()
    private var clave = ""
    private var cerrando = false
    @Volatile private var destruido = false
    private val pantalla = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { if (intent?.action == Intent.ACTION_SCREEN_OFF) stopSelf() }
    }
    override fun onCreate() {
        super.onCreate(); hilo.start(); trabajo = Handler(hilo.looper)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(pantalla, IntentFilter(Intent.ACTION_SCREEN_OFF), Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(pantalla, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android exige llamar a startForeground apenas arranca un servicio iniciado con startForegroundService,
        // aunque después se detenga: si no, cierra Díptico. Por eso la notificación va antes de cualquier revisión.
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CANAL, "Clip de retoma", NotificationManager.IMPORTANCE_LOW))
        val parar = PendingIntent.getService(this, 9281, Intent(this, RetomaServicio::class.java).setAction("detener"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val aviso = Notification.Builder(this, CANAL).setSmallIcon(R.drawable.ic_bisa)
            .setContentTitle("Díptico · clip de retoma activo")
            .setContentText("Conserva solo los últimos 6 segundos, sin sonido. Toca para detener.")
            .setContentIntent(parar).setOngoing(true).build()
        try { startForeground(9281, aviso, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) }
        catch (_: Exception) { stopSelf(); return START_NOT_STICKY }
        if (intent?.action == "detener") { stopSelf(); return START_NOT_STICKY }
        if (activa) return START_NOT_STICKY
        clave = intent?.getStringExtra("clave").orEmpty()
        val codigo = intent?.getIntExtra("codigo", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        @Suppress("DEPRECATION") val datos = intent?.getParcelableExtra<Intent>("datos")
        if (codigo != Activity.RESULT_OK || datos == null) { stopSelf(); return START_NOT_STICKY }
        // El permiso llega justo antes de abrir el juego: la sesión puede tardar un momento en existir.
        esperarSesion(codigo, datos, intentos = 6)
        return START_NOT_STICKY
    }

    private fun esperarSesion(codigo: Int, datos: Intent, intentos: Int) {
        val s = Diptico.estado.value.sesion
        val valida = s != null && s.clave == clave && !s.dosPantallas && s.displayId == 0 && !LupaServicio.activa
        if (!valida) {
            if (intentos > 0 && s == null && !LupaServicio.activa) {
                Handler(Looper.getMainLooper()).postDelayed({ if (!destruido && !activa) esperarSesion(codigo, datos, intentos - 1) }, 500)
            } else stopSelf()
            return
        }
        empezar(codigo, datos)
    }

    private fun empezar(codigo: Int, datos: Intent) {
        activa = true
        trabajo.post {
            try {
                val t = LupaServicio.tamanoArriba(this)
                val factor = minOf(640f / t.x.coerceAtLeast(1), 360f / t.y.coerceAtLeast(1), 1f)
                val w = ((t.x * factor).toInt() / 2 * 2).coerceAtLeast(2)
                val h = ((t.y * factor).toInt() / 2 * 2).coerceAtLeast(2)
                val f = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                    setInteger(MediaFormat.KEY_BIT_RATE, 800_000); setInteger(MediaFormat.KEY_FRAME_RATE, 15)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); setFloat(MediaFormat.KEY_MAX_FPS_TO_ENCODER, 15f)
                }
                val c = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC); codec = c
                c.configure(f, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                superficie = c.createInputSurface(); c.start()
                val p = getSystemService(MediaProjectionManager::class.java).getMediaProjection(codigo, datos) ?: error("Sin captura")
                proyeccion = p
                p.registerCallback(object : MediaProjection.Callback() { override fun onStop() { stopSelf() } }, Handler(Looper.getMainLooper()))
                virtual = p.createVirtualDisplay("Diptico-retoma", w, h, resources.displayMetrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, superficie, null, trabajo)
                vaciar()
            } catch (_: Exception) { Diptico.avisar("El clip de retoma no está disponible en este equipo"); stopSelf() }
        }
    }
    private fun vaciar() {
        if (cerrando) return
        val c = codec ?: return
        try {
            val info = MediaCodec.BufferInfo()
            while (true) {
                val n = c.dequeueOutputBuffer(info, 0)
                when {
                    n == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> formato = c.outputFormat
                    n >= 0 -> {
                        val b = c.getOutputBuffer(n)
                        if (b != null && info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            b.position(info.offset); b.limit(info.offset + info.size)
                            val datos = ByteArray(info.size); b.get(datos)
                            buffer.agregar(PaqueteRetoma(datos, info.presentationTimeUs, info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0, info.flags))
                        }
                        c.releaseOutputBuffer(n, false)
                    }
                    else -> break
                }
            }
            if (Diptico.estado.value.sesion?.clave != clave || LupaServicio.activa) { stopSelf(); return }
            trabajo.postDelayed({ vaciar() }, 50)
        } catch (_: Exception) { stopSelf() }
    }
    private fun guardar() {
        val paquetes = buffer.copia()
        val f = formato ?: return
        if (paquetes.size < 3 || !paquetes.first().clave) return
        val dir = File(cacheDir, "retoma").apply { mkdirs() }
        val destino = File(dir, "retoma.mp4")
        var mux: MediaMuxer? = null
        try {
            val m = MediaMuxer(destino.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4); mux = m
            val pista = m.addTrack(f); m.start()
            val inicio = paquetes.first().tiempoUs
            var previo = -1L
            paquetes.forEach { p ->
                val tiempo = (p.tiempoUs - inicio).coerceAtLeast(previo + 1)
                val info = MediaCodec.BufferInfo().apply { set(0, p.datos.size, tiempo, p.flags) }
                m.writeSampleData(pista, ByteBuffer.wrap(p.datos), info); previo = tiempo
            }
            m.stop(); m.release(); mux = null
            Diptico.clipRetomaListo(clave, destino.absolutePath)
        } catch (_: Exception) { try { mux?.release() } catch (_: Exception) {}; destino.delete() }
    }
    override fun onDestroy() {
        destruido = true
        activa = false
        try { unregisterReceiver(pantalla) } catch (_: Exception) {}
        trabajo.post {
            cerrando = true; trabajo.removeCallbacksAndMessages(null)
            try { virtual?.release() } catch (_: Exception) {}
            guardar()
            try { proyeccion?.stop() } catch (_: Exception) {}
            try { codec?.stop(); codec?.release() } catch (_: Exception) {}
            try { superficie?.release() } catch (_: Exception) {}
            buffer.limpiar(); hilo.quitSafely()
        }
        super.onDestroy()
    }
}

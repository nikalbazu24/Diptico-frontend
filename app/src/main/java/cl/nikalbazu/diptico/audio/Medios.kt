package cl.nikalbazu.diptico.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Servicio vacío: Android lo pide para dejar que Díptico vea qué música suena (Spotify, YouTube Music…). */
class OyenteMedios : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        Avisos.servicio = this
        Medios.revisar(this)
        try {
            Avisos.cargar(this, activeNotifications)
        } catch (_: Exception) {
        }
    }

    override fun onListenerDisconnected() {
        Avisos.servicio = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: android.service.notification.StatusBarNotification?) {
        if (sbn != null) Avisos.llego(this, sbn)
    }

    override fun onNotificationRemoved(sbn: android.service.notification.StatusBarNotification?) {
        if (sbn != null) Avisos.salio(sbn)
    }
}

/** Lo que suena ahora en otra app. */
data class Sonando(
    val app: String,
    val titulo: String,
    val artista: String,
    val portada: Bitmap?,
    val reproduciendo: Boolean,
    val paquete: String,
)

/** Widget de música: lee y controla la música de Spotify, YouTube Music y cualquier reproductor. */
object Medios {
    private val _sonando = MutableStateFlow<Sonando?>(null)
    val sonando: StateFlow<Sonando?> = _sonando

    private val _permiso = MutableStateFlow(false)
    val permiso: StateFlow<Boolean> = _permiso

    private var controlador: MediaController? = null
    private var gestor: MediaSessionManager? = null
    private var escuchando = false

    private val alCambiar = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publicar()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publicar()
        override fun onSessionDestroyed() {
            controlador = null
            publicar()
        }
    }

    private var permisoCache: Boolean? = null
    private var permisoHora = 0L

    /** ¿Díptico tiene acceso a notificaciones? (se recuerda unos segundos para no frenar el menú). */
    fun tienePermiso(ctx: Context): Boolean {
        val ahora = android.os.SystemClock.uptimeMillis()
        permisoCache?.let { if (ahora - permisoHora < 5000) return it }
        val v = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
        permisoCache = v
        permisoHora = ahora
        return v
    }

    /** Abre los ajustes de Android para dar el permiso de "acceso a notificaciones". */
    fun pedirPermiso(ctx: Context) {
        try {
            ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    fun revisar(ctx: Context) {
        val app = ctx.applicationContext
        val ok = tienePermiso(app)
        _permiso.value = ok
        if (!ok) return
        try {
            val g = gestor ?: app.getSystemService(MediaSessionManager::class.java).also { gestor = it } ?: return
            val componente = ComponentName(app, OyenteMedios::class.java)
            if (!escuchando) {
                g.addOnActiveSessionsChangedListener({ lista -> elegir(lista ?: emptyList(), app) }, componente)
                escuchando = true
            }
            elegir(g.getActiveSessions(componente), app)
        } catch (_: SecurityException) {
            _permiso.value = false
        } catch (_: Exception) {
        }
    }

    private fun elegir(lista: List<MediaController>, ctx: Context) {
        // Ignora la música de Díptico; prefiere lo que está sonando.
        val otras = lista.filter { it.packageName != ctx.packageName }
        val c = otras.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: otras.firstOrNull()
        if (c?.sessionToken != controlador?.sessionToken) {
            controlador?.unregisterCallback(alCambiar)
            controlador = c
            c?.registerCallback(alCambiar)
        }
        publicar(ctx)
    }

    private var nombres = HashMap<String, String>()
    private var ctxApp: Context? = null

    private fun publicar(ctx: Context? = null) {
        if (ctx != null) ctxApp = ctx.applicationContext
        val c = controlador
        if (c == null) {
            _sonando.value = null
            return
        }
        val m = c.metadata
        val nombreApp = nombres.getOrPut(c.packageName) {
            try {
                val pm = ctxApp?.packageManager
                pm?.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0))?.toString() ?: c.packageName
            } catch (e: Exception) {
                c.packageName
            }
        }
        _sonando.value = Sonando(
            app = nombreApp,
            titulo = m?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Sin título",
            artista = m?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: m?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
            portada = m?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            reproduciendo = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            paquete = c.packageName,
        )
    }

    fun alternar() {
        val c = controlador ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    fun siguiente() = controlador?.transportControls?.skipToNext()
    fun anterior() = controlador?.transportControls?.skipToPrevious()
}

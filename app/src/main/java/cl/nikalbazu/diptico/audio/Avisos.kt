package cl.nikalbazu.diptico.audio

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Una notificación de otra app, para el centro de notificaciones de Díptico. */
data class Aviso(
    val clave: String,
    val paquete: String,
    val app: String,
    val titulo: String,
    val texto: String,
    val tiempo: Long,
    val intent: PendingIntent?,
)

/**
 * Notificaciones del sistema dentro de Díptico, para usarlo como launcher principal.
 * Llegan por el mismo servicio del widget de música (acceso a notificaciones).
 */
object Avisos {
    private val _lista = MutableStateFlow<List<Aviso>>(emptyList())
    val lista: StateFlow<List<Aviso>> = _lista

    private val _nuevos = MutableSharedFlow<Aviso>(extraBufferCapacity = 8)
    val nuevos: SharedFlow<Aviso> = _nuevos

    internal var servicio: OyenteMedios? = null
    private val nombres = HashMap<String, String>()

    private fun nombreApp(ctx: Context, pkg: String): String = nombres.getOrPut(pkg) {
        try {
            val pm = ctx.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    private fun convertir(ctx: Context, sbn: StatusBarNotification): Aviso? {
        if (sbn.packageName == ctx.packageName) return null
        val n = sbn.notification
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return null
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        val ex = n.extras
        val titulo = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val texto = (ex.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: ex.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (titulo.isBlank() && texto.isBlank()) return null
        return Aviso(sbn.key, sbn.packageName, nombreApp(ctx, sbn.packageName), titulo, texto, sbn.postTime, n.contentIntent)
    }

    internal fun cargar(ctx: Context, todas: Array<StatusBarNotification>?) {
        _lista.value = (todas ?: emptyArray()).mapNotNull { convertir(ctx, it) }.sortedByDescending { it.tiempo }
    }

    internal fun llego(ctx: Context, sbn: StatusBarNotification) {
        val a = convertir(ctx, sbn) ?: return
        val nuevo = _lista.value.none { it.clave == a.clave }
        _lista.value = (listOf(a) + _lista.value.filter { it.clave != a.clave }).take(60)
        if (nuevo) _nuevos.tryEmit(a)
    }

    internal fun salio(sbn: StatusBarNotification) {
        _lista.value = _lista.value.filter { it.clave != sbn.key }
    }

    fun abrir(a: Aviso) {
        try {
            a.intent?.send()
        } catch (_: Exception) {
        }
        descartar(a)
    }

    fun descartar(a: Aviso) {
        try {
            servicio?.cancelNotification(a.clave)
        } catch (_: Exception) {
        }
        _lista.value = _lista.value.filter { it.clave != a.clave }
    }

    fun descartarTodas() {
        try {
            servicio?.cancelAllNotifications()
        } catch (_: Exception) {
        }
        _lista.value = emptyList()
    }
}

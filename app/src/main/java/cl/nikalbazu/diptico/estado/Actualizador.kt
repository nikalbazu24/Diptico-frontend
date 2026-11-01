package cl.nikalbazu.diptico.estado

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Una versión más nueva de Díptico publicada en GitHub. */
data class VersionNueva(val numero: Int, val nombre: String, val url: String, val bytes: Long, val notas: String)

/**
 * Actualizaciones desde la misma app: mira las versiones publicadas en GitHub, avisa cuando hay una nueva,
 * la descarga y le pide a Android que la instale encima (se conservan tus juegos y ajustes).
 */
object Actualizador {
    private const val REPO = "Nikalbazu24/diptico-frontend"
    private const val CANAL = "actualizaciones"
    private const val AVISO = 4471

    /** Número de esta versión (el mismo de "beta.N"). */
    fun numeroActual(ctx: Context): Int = try {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        (if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()).toInt()
    } catch (_: Exception) { 0 }

    /** Busca en GitHub la versión más nueva. null si no hay una más nueva que esta (o no hay internet). */
    fun buscar(ctx: Context): VersionNueva? {
        val c = URL("https://api.github.com/repos/$REPO/releases?per_page=6").openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "Diptico")
        val texto = try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
        val lista = JSONArray(texto)
        val actual = numeroActual(ctx)
        var mejor: VersionNueva? = null
        for (k in 0 until lista.length()) {
            val r = lista.getJSONObject(k)
            if (r.optBoolean("draft")) continue
            val tag = r.optString("tag_name")
            val n = Regex("(\\d+)$").find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: continue
            if (n <= actual || (mejor != null && n <= mejor.numero)) continue
            val assets = r.optJSONArray("assets") ?: continue
            val movil = esMovil(ctx)
            for (a in 0 until assets.length()) {
                val o = assets.getJSONObject(a)
                val archivoApk = o.optString("name")
                // Cada versión baja su mismo tipo: la de consolas o la de teléfonos ("-movil").
                if (archivoApk.endsWith(".apk") && archivoApk.contains("-movil") == movil) {
                    mejor = VersionNueva(
                        n, tag.removePrefix("v"), o.optString("browser_download_url"), o.optLong("size"),
                        r.optString("body").lineSequence().map { it.trim() }.filter { it.isNotBlank() }.take(4).joinToString("\n"),
                    )
                    break
                }
            }
        }
        return mejor
    }

    /** ¿Es la versión para teléfonos? (la que no trae el acceso a notificaciones). */
    fun esMovil(ctx: Context): Boolean = try {
        ctx.packageManager.getServiceInfo(android.content.ComponentName(ctx, "cl.nikalbazu.diptico.audio.OyenteMedios"), 0)
        false
    } catch (_: Exception) { true }

    private fun archivo(ctx: Context): File = File(ctx.cacheDir, "actualizacion").apply { mkdirs() }.let { File(it, "Diptico.apk") }

    /** Descarga el APK (con avance de 0 a 1). Devuelve el archivo. */
    fun descargar(ctx: Context, v: VersionNueva, avance: (Float) -> Unit): File {
        val destino = archivo(ctx)
        var url = URL(v.url)
        var c: HttpURLConnection
        // GitHub redirige a su servidor de archivos: se siguen las redirecciones a mano por si cambian de dominio.
        var saltos = 0
        while (true) {
            c = url.openConnection() as HttpURLConnection
            c.instanceFollowRedirects = false
            c.connectTimeout = 15_000
            c.readTimeout = 30_000
            c.setRequestProperty("User-Agent", "Diptico")
            val codigo = c.responseCode
            if (codigo in 300..399 && saltos < 6) {
                val sig = c.getHeaderField("Location") ?: break
                c.disconnect()
                url = URL(url, sig)
                saltos++
                continue
            }
            break
        }
        try {
            val total = c.contentLengthLong.takeIf { it > 0 } ?: v.bytes
            c.inputStream.use { entrada ->
                destino.outputStream().use { salida ->
                    val buf = ByteArray(64 * 1024)
                    var leidos = 0L
                    var ultimo = 0L
                    while (true) {
                        val n = entrada.read(buf)
                        if (n < 0) break
                        salida.write(buf, 0, n)
                        leidos += n
                        if (total > 0 && leidos - ultimo > 256 * 1024) { ultimo = leidos; avance((leidos.toFloat() / total).coerceIn(0f, 1f)) }
                    }
                }
            }
        } finally { c.disconnect() }
        avance(1f)
        return destino
    }

    /** ¿Android ya dejó que Díptico instale actualizaciones? */
    fun puedeInstalar(ctx: Context): Boolean = ctx.packageManager.canRequestPackageInstalls()

    fun pedirPermisoInstalar(ctx: Context) {
        try {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: Exception) {}
    }

    /** Le entrega el APK a Android para instalarlo encima. Android muestra su propia ventana para confirmar. */
    fun instalar(ctx: Context, apk: File) {
        val pi = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(ctx.packageName)
        val id = pi.createSession(params)
        pi.openSession(id).use { s ->
            s.openWrite("diptico", 0, apk.length()).use { salida ->
                apk.inputStream().use { it.copyTo(salida) }
                s.fsync(salida)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            val pend = PendingIntent.getBroadcast(ctx, id, Intent(ctx, ReceptorInstalacion::class.java), flags)
            s.commit(pend.intentSender)
        }
    }

    /** Notificación de Android: "Hay una versión nueva". Tocarla abre Díptico en la actualización. */
    fun notificar(ctx: Context, v: VersionNueva) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CANAL, "Actualizaciones de Díptico", NotificationManager.IMPORTANCE_DEFAULT))
        val abrir = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.putExtra("actualizar", true) ?: return
        val pend = PendingIntent.getActivity(ctx, AVISO, abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = android.app.Notification.Builder(ctx, CANAL)
            .setSmallIcon(cl.nikalbazu.diptico.R.mipmap.ic_launcher)
            .setContentTitle("Díptico ${v.nombre} ya está disponible")
            .setContentText("Tócala para descargarla e instalarla desde Díptico.")
            .setContentIntent(pend)
            .setAutoCancel(true)
            .build()
        try { nm.notify(AVISO, n) } catch (_: Exception) {}
    }
}

/** Respuesta de Android al instalar: si pide confirmación, se le muestra a la persona. */
class ReceptorInstalacion : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirmar = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java) else intent.getParcelableExtra(Intent.EXTRA_INTENT)
                confirmar?.let { try { ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {} }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            else -> Diptico.instalacionFallida(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
        }
    }
}

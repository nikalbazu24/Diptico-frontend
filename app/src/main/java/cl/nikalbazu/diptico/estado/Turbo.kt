package cl.nikalbazu.diptico.estado

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Settings

/**
 * Modo turbo: antes de abrir un juego, Díptico deja la consola lo más libre posible.
 * - Cierra las apps que quedaron en segundo plano (nunca el emulador, la música que suena, el teclado ni las del sistema).
 * - Mide cuánta memoria quedó libre, para mostrarlo.
 * - Opcional: "No molestar" mientras juegas (y lo devuelve como estaba al terminar).
 * Díptico mismo se pone en reposo mientras juegas (sin animaciones ni efectos), eso lo maneja Diptico.
 */
object Turbo {
    data class Resultado(val apps: Int, val liberadosMb: Long, val libresMb: Long)

    fun memoriaLibreMb(ctx: Context): Long {
        val am = ctx.getSystemService(ActivityManager::class.java) ?: return 0
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi.availMem / (1024 * 1024)
    }

    fun memoriaTotalMb(ctx: Context): Long {
        val am = ctx.getSystemService(ActivityManager::class.java) ?: return 0
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi.totalMem / (1024 * 1024)
    }

    /** Cierra las apps en segundo plano que no hacen falta. [protegidos]: paquetes que no se tocan. */
    fun optimizar(ctx: Context, protegidos: Set<String>, cerrarApps: Boolean): Resultado {
        val antes = memoriaLibreMb(ctx)
        var n = 0
        if (cerrarApps) {
            val am = ctx.getSystemService(ActivityManager::class.java)
            val pm = ctx.packageManager
            val teclado = try { Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)?.substringBefore('/') } catch (_: Exception) { null }
            val lanzables = try {
                pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                    .map { it.activityInfo.applicationInfo }.distinctBy { it.packageName }
            } catch (_: Exception) { emptyList() }
            for (ai in lanzables) {
                val pkg = ai.packageName
                val sistema = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 || (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                if (sistema || pkg == ctx.packageName || pkg == teclado || pkg in protegidos) continue
                try { am?.killBackgroundProcesses(pkg); n++ } catch (_: Exception) {}
            }
        }
        System.gc()
        // Android tarda un poquito en devolver la memoria.
        try { Thread.sleep(450) } catch (_: Exception) {}
        val despues = memoriaLibreMb(ctx)
        return Resultado(n, (despues - antes).coerceAtLeast(0), despues)
    }

    // ---------- No molestar ----------

    private var filtroAntes: Int? = null

    fun puedeNoMolestar(ctx: Context): Boolean =
        ctx.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true

    fun pedirNoMolestar(ctx: Context) {
        try { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
    }

    /** Activa "No molestar" al jugar (solo alarmas y prioritarias) y lo devuelve como estaba al terminar. */
    fun noMolestar(ctx: Context, activar: Boolean) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (!nm.isNotificationPolicyAccessGranted) return
        try {
            if (activar) {
                if (filtroAntes == null) filtroAntes = nm.currentInterruptionFilter
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } else {
                filtroAntes?.let { nm.setInterruptionFilter(it) }
                filtroAntes = null
            }
        } catch (_: Exception) {}
    }
}

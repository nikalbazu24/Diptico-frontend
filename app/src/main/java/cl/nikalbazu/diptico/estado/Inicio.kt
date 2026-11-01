package cl.nikalbazu.diptico.estado

import android.app.KeyguardManager
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock

/**
 * Díptico como inicio de Android: saber si lo es, y darse cuenta de cuándo se apaga y se prende la pantalla
 * (para la bienvenida y el mensaje de continuación).
 */
object Inicio {
    private var apagadaDesde = 0L
    private var jugandoAlApagar = false
    private var esperandoDesbloqueo = false
    private var vigilando = false

    /** ¿Díptico es la app de inicio? */
    fun esPredeterminado(ctx: Context): Boolean = try {
        if (Build.VERSION.SDK_INT >= 29) {
            ctx.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true
        } else {
            val r = ctx.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
            r?.activityInfo?.packageName == ctx.packageName
        }
    } catch (_: Exception) {
        false
    }

    /** El pedido de Android para que la persona elija a Díptico como inicio (o null si no se puede). */
    fun pedido(ctx: Context): Intent? = try {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = ctx.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) rm.createRequestRoleIntent(RoleManager.ROLE_HOME) else null
        } else null
    } catch (_: Exception) {
        null
    }

    /**
     * La pantalla de abajo también es de Díptico al apretar Inicio (Android la abre sola en la segunda pantalla),
     * salvo en el modo que deja libre la pantalla de abajo para otras apps.
     */
    fun inicioAbajo(ctx: Context, activo: Boolean) {
        try {
            val c = ComponentName(ctx, "cl.nikalbazu.diptico.InicioAbajo")
            val estado = if (activo) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            if (ctx.packageManager.getComponentEnabledSetting(c) != estado) {
                ctx.packageManager.setComponentEnabledSetting(c, estado, PackageManager.DONT_KILL_APP)
            }
        } catch (_: Exception) {
        }
    }

    /** Escucha cuándo se apaga y se prende la pantalla (mientras Díptico esté vivo, que siendo el inicio es siempre). */
    fun vigilarPantalla(ctx: Context) {
        if (vigilando) return
        vigilando = true
        val app = ctx.applicationContext
        val receptor = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                when (i.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        apagadaDesde = SystemClock.elapsedRealtime()
                        jugandoAlApagar = Diptico.estado.value.sesion != null
                        esperandoDesbloqueo = false
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        val bloqueada = c.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
                        if (bloqueada) esperandoDesbloqueo = true else avisarEncendido()
                    }
                    Intent.ACTION_USER_PRESENT -> if (esperandoDesbloqueo) { esperandoDesbloqueo = false; avisarEncendido() }
                }
            }
        }
        val filtro = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(receptor, filtro, Context.RECEIVER_NOT_EXPORTED)
            else app.registerReceiver(receptor, filtro)
        } catch (_: Exception) {
            vigilando = false
        }
    }

    private fun avisarEncendido() {
        if (apagadaDesde == 0L) return
        val ausencia = SystemClock.elapsedRealtime() - apagadaDesde
        apagadaDesde = 0L
        Diptico.alEncender(ausencia, jugandoAlApagar)
    }
}

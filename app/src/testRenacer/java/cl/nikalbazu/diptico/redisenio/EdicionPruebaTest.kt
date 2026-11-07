package cl.nikalbazu.diptico.redisenio

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import cl.nikalbazu.diptico.BuildConfig
import cl.nikalbazu.diptico.InferiorActivity
import cl.nikalbazu.diptico.MainActivity
import cl.nikalbazu.diptico.estado.RevisionActualizaciones
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EdicionPruebaTest {
    @Test fun seInstalaComoOtraAppConNombrePropioYTareaInferiorIndependiente() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals("cl.nikalbazu.diptico.prueba.renacer", ctx.packageName)
        assertNotEquals("cl.nikalbazu.diptico", ctx.packageName)
        assertNotEquals("cl.nikalbazu.diptico.renacer", ctx.packageName)
        assertEquals("Díptico · Prueba Renacer", ctx.applicationInfo.loadLabel(ctx.packageManager).toString())
        assertTrue(BuildConfig.RENACER)
        assertTrue(BuildConfig.VERSION_NAME.endsWith("-prueba-renacer"))
        val inicio = requireNotNull(ctx.packageManager.getLaunchIntentForPackage(ctx.packageName))
        assertEquals(MainActivity::class.java.name, inicio.component?.className)
        val inferior = ctx.packageManager.getActivityInfo(ComponentName(ctx, InferiorActivity::class.java), 0)
        assertEquals("${ctx.packageName}.inferior", inferior.taskAffinity)
        try {
            ctx.packageManager.getServiceInfo(ComponentName(ctx, RevisionActualizaciones::class.java), 0)
            fail("La prueba no debe registrar el actualizador de la beta")
        } catch (_: PackageManager.NameNotFoundException) { }
    }
}

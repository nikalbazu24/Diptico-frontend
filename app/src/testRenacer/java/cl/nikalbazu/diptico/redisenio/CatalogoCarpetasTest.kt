package cl.nikalbazu.diptico.redisenio

import androidx.test.core.app.ApplicationProvider
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Temas
import cl.nikalbazu.diptico.estado.TiendaTemas
import cl.nikalbazu.diptico.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatalogoCarpetasTest {
    @Test fun todasLasPlataformasDelCatalogoTienenUnaReferenciaIncluidosAndroidYRpgMaker() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        Consolas.cargar(ctx)
        assertTrue(Consolas.todas.size > 190)
        Consolas.todas.forEach { c ->
            val ref = ReferenciasCarpetas.para(c.id)
            assertNotNull("Falta ${c.id}",ref)
            assertFalse(ref!!.juego.isBlank())
            assertFalse(ref.motivo.isBlank())
        }
        assertEquals("Genshin Impact",ReferenciasCarpetas.para(Consolas.ANDROID)?.juego)
        assertEquals("To the Moon",ReferenciasCarpetas.para(Consolas.RPGMAKER)?.juego)
    }

    @Test fun glassPuedeActivarseYDesactivarseEnTodosLosTemasSinAlterarSusRecursos() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val temas = Temas.incluidos + Temas.fiestas.map { it.tema } + TiendaTemas.incluidos(ctx).map { it.tema }
        assertTrue(temas.size > 30)
        temas.forEach { t ->
            assertTrue("Activar ${t.id}", glassActivo(t,"glass"))
            assertFalse("Desactivar ${t.id}", glassActivo(t,"original"))
            assertEquals(t.forma == "cristal",glassActivo(t,"tema"))
        }
    }
}

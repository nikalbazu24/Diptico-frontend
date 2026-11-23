package cl.nikalbazu.diptico

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Integraciones
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** PokeDaisy, Peanuts Native y Biomeria Launcher están en el catálogo y Díptico sabe cómo abrirlas. */
@RunWith(RobolectricTestRunner::class)
class IntegracionesTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun catalogoConLasTresApps() {
        Consolas.cargar(ctx)
        assertTrue(Consolas.reglas["POKEDAISY"]!!.first().startsWith("com.pokedaisy.app/"))
        assertTrue(Consolas.reglas["PEANUTS-NATIVE"]!!.first().endsWith("/dev.peanuts.launch.LaunchActivity"))
        assertTrue(Consolas.reglas["BIOMERIA"]!!.first().startsWith(Integraciones.BIOMERIA + "/"))
        for (id in listOf("gb", "gbc", "gba", "nds")) {
            assertTrue("$id con PokeDaisy", Consolas.porId(id)!!.comandos.any { it.etiqueta == Integraciones.ORDEN_POKEDAISY })
        }
        assertTrue(Consolas.porId("switch")!!.comandos.any { it.etiqueta == Integraciones.ORDEN_PEANUTS })
        val mc = Consolas.porId(Integraciones.CONSOLA_BIOMERIA)
        assertNotNull(mc)
        assertEquals("BIOMERIA", mc!!.comandos.first().emulador)
        assertTrue(Integraciones.EXT_BIOMERIA in mc.extensiones)
        assertEquals("Minecraft", mc.nombre)
    }

    @Test
    fun reconocePokemonYArmaElEnlace() {
        assertTrue(Integraciones.esPokemon("Pokémon - Edición Esmeralda (S)"))
        assertTrue(Integraciones.esPokemon("POKEMON_FIRE_RED"))
        assertFalse(Integraciones.esPokemon("Zelda Minish Cap"))
        val enlace = Integraciones.enlaceDesdeDiptico(ctx, "prissma://launch?instance_id=abc-1")
        val u = Uri.parse(enlace)
        assertEquals("abc-1", u.getQueryParameter("instance_id"))
        assertEquals(ctx.packageName, u.getQueryParameter("source_frontend"))
        val i = Integraciones.Instancia("abc-1", "Survival", "java", "1.21.1", "fabric", 3600, enlace)
        assertEquals("Java 1.21.1 · Fabric", i.detalle)
        assertEquals("biomeria/abc-1", i.clave)
    }

    @Test
    fun sinBiomeriaNoHayCarpetaMinecraft() {
        assertTrue(Integraciones.juegosBiomeria(ctx).isEmpty())
    }
}

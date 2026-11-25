package cl.nikalbazu.diptico

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.emu.Lanzador
import cl.nikalbazu.diptico.estado.Diagnostico
import cl.nikalbazu.diptico.estado.EmuladoresAlDia
import cl.nikalbazu.diptico.estado.Integraciones
import cl.nikalbazu.diptico.estado.Nativos
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
    fun biomeriaSeReconoceConSuNombreNuevoYElAntiguo() {
        Consolas.cargar(ctx)
        assertTrue(Integraciones.esBiomeria("com.biomeria.launcher"))
        assertTrue(Integraciones.esBiomeria("com.prissma.launcher"))
        assertFalse(Integraciones.esBiomeria("com.otra.app"))
        assertFalse(Integraciones.esBiomeria(null))
        assertTrue(Consolas.reglas["BIOMERIA"]!!.any { it.startsWith("com.prissma.launcher/") })
    }

    @Test
    fun reconocePokemonYArmaElEnlace() {
        assertTrue(Integraciones.esPokemon("Pokémon - Edición Esmeralda (S)"))
        assertTrue(Integraciones.esPokemon("POKEMON_FIRE_RED"))
        assertFalse(Integraciones.esPokemon("Zelda Minish Cap"))
        val enlace = Integraciones.enlaceDesdeDiptico(ctx, "biomeria://launch?instance_id=abc-1")
        val u = Uri.parse(enlace)
        assertEquals("abc-1", u.getQueryParameter("instance_id"))
        assertEquals(ctx.packageName, u.getQueryParameter("source_frontend"))
        val i = Integraciones.Instancia("abc-1", "Survival", "java", "1.21.1", "fabric", 3600, enlace)
        assertEquals("Java 1.21.1 · Fabric", i.detalle)
        assertEquals("biomeria/abc-1", i.clave)
    }

    private fun juego(consola: String, archivo: String, doc: String = "primary:ROMs/$consola/$archivo",
                      uri: String = "content://com.android.externalstorage.documents/tree/primary%3AROMs/document/" + Uri.encode(doc)) =
        Juego("$consola/$archivo", consola, archivo.substringBeforeLast('.'), archivo, Uri.parse(uri), doc)

    @Test
    fun peanutsSoloParaLosNupDeSwitch1() {
        assertTrue(Integraciones.paraPeanuts(juego("switch", "Mario Odyssey.nup")))
        assertFalse(Integraciones.paraPeanuts(juego("switch", "Mario Odyssey.nsp")))
        // En la carpeta de Switch 2, solo si el nombre trae el código de un juego de Switch 1.
        assertTrue(Integraciones.paraPeanuts(juego("switch2", "Zelda BOTW [01007EF00011E000].nup")))
        assertFalse(Integraciones.paraPeanuts(juego("switch2", "Donkey Kong Bananza [040074A01BF12000].nup")))
        assertFalse(Integraciones.paraPeanuts(juego("switch2", "Mario Kart World.nup")))
        assertEquals("01007EF00011E000", Integraciones.codigoDe("zelda_01007ef00011e000_v0.nup"))
        assertNull(Integraciones.codigoDe("juego 2024.nup"))
    }

    @Test
    fun switch2SigueConNovaSYBananaPrimero() {
        Consolas.cargar(ctx)
        val etiquetas = Consolas.porId("switch2")!!.comandos.map { it.etiqueta }
        assertEquals(Nativos.ORDEN_NOVAS, etiquetas[0])
        assertEquals(Nativos.ORDEN_BANANA, etiquetas[1])
        assertEquals(Integraciones.ORDEN_PEANUTS, etiquetas.last())
        val peanuts = Consolas.porId("switch")!!.comandos.first { it.etiqueta == Integraciones.ORDEN_PEANUTS }
        assertEquals(Integraciones.REGLA_PEANUTS, peanuts.emulador)
        assertTrue("Peanuts recibe la ruta del .nup", "%EXTRA_game%=%ROM%" in peanuts.linea)
    }

    @Test
    fun peanutsRecibeUnaRutaQuePuedeAbrir() {
        assertEquals("/storage/emulated/0/ROMs/switch/a.nup", Lanzador.rutaReal("primary:ROMs/switch/a.nup"))
        assertEquals("/storage/1A2B-3C4D/ROMs/a.nup", Lanzador.rutaReal("1A2B-3C4D:ROMs/a.nup"))
        assertEquals("/storage/emulated/0/Documents/a.nup", Lanzador.rutaReal("home:a.nup"))
        assertNull(Lanzador.avisoPorRuta(juego("switch", "a.nup"), "Peanuts Native"))
        assertNull(Lanzador.avisoPorRuta(juego("switch", "a.nup", doc = "1A2B-3C4D:ROMs/a.nup"), "Peanuts Native"))
        // Mayúsculas: Peanuts solo lee «.nup».
        assertTrue(Lanzador.avisoPorRuta(juego("switch", "a.NUP"), "Peanuts Native")!!.contains("minúsculas"))
        // Un documento sin ruta (Descargas, la nube): se explica en vez de mandar una ruta falsa.
        val nube = juego("switch", "a.nup", doc = "msf:42", uri = "content://com.android.providers.downloads.documents/document/msf%3A42")
        assertTrue(Lanzador.avisoPorRuta(nube, "Peanuts Native")!!.contains("Acceso a todos los archivos"))
    }

    @Test
    fun diagnosticoSugierePeanuts() {
        Consolas.cargar(ctx)
        val sw = Consolas.porId("switch")!!
        val sin = Diagnostico.causas(juego("switch", "a.nup"), sw, "Eden", 4_000_000L, 1, peanutsInstalada = false)
        assertTrue(sin.first().contains("instala Peanuts Native"))
        val con = Diagnostico.causas(juego("switch", "a.nup"), sw, "Peanuts Native", 4_000_000L, 1, peanutsInstalada = true)
        assertTrue(con.any { "Acceso a todos los archivos" in it })
    }

    @Test
    fun peanutsNoPareceSiempreDesactualizada() {
        val f = EmuladoresAlDia.fuenteDe(Integraciones.PEANUTS)!!
        assertEquals("igawa6/peanuts-native", f.repo)
        assertEquals("v0.1.8", EmuladoresAlDia.etiquetaComparable(f, "v0.1.8-1"))
        assertFalse(EmuladoresAlDia.esMasNueva("0.1.8", EmuladoresAlDia.etiquetaComparable(f, "v0.1.8-1"), 0, 0))
        assertTrue(EmuladoresAlDia.esMasNueva("0.1.8", EmuladoresAlDia.etiquetaComparable(f, "v0.1.9-1"), 0, 0))
        assertEquals("PeanutsNative-0.1.8.apk", EmuladoresAlDia.elegirApk(listOf("PeanutsNative-0.1.8.apk"), Integraciones.PEANUTS))
    }

    @Test
    fun sinBiomeriaNoHayCarpetaMinecraft() {
        assertTrue(Integraciones.juegosBiomeria(ctx).isEmpty())
    }
}

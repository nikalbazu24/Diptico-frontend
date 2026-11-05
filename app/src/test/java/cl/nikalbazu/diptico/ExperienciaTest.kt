package cl.nikalbazu.diptico

import cl.nikalbazu.diptico.estado.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExperienciaTest {
    @Test fun perfilCompartidoNoExportaRecursosLocales() {
        val e = Experiencia(colecciones = mapOf("privado" to PresentacionColeccion(fondo = "content://privado/foto", musica = "content://privado/audio")))
        val texto = e.json().toString()
        assertFalse(texto.contains("content://"))
        assertFalse(texto.contains("privado"))
        assertEquals(e.copy(colecciones = emptyMap()), Experiencia.importar(texto))
        assertEquals(e, Experiencia.leer(e.json(local = true).toString()))
    }
    @Test fun importarValidaFormatoYVersion() {
        listOf("{}", "{\"formato\":\"diptico-visual\",\"version\":99}", "x".repeat(32769)).forEach { texto ->
            assertTrue(runCatching { Experiencia.importar(texto) }.isFailure)
        }
    }
    @Test fun perfilAjenoNoImportaRutasNiAccesosAJuegos() {
        val e = Experiencia(posiciones = listOf(PosicionVisual("atajo-rpgmaker/privado", 0, 0, 1, 1, 0, true)))
        assertFalse(e.json().toString().contains("privado"))
        assertTrue(Experiencia.importar(e.json(true).toString()).posiciones.isEmpty())
    }
    @Test fun geometriaAjenaSeAjustaALaGrilla() {
        val e = Experiencia.importar("""{"formato":"diptico-visual","version":1,"columnas":99,"filas":-1,"paginas":0,"posiciones":[{"tipo":"reloj","x":99,"y":-4,"an":99,"al":99,"pagina":-3}]}""")
        assertEquals(8, e.columnas); assertEquals(3, e.filas); assertEquals(1, e.paginas)
        val p = e.posiciones.single()
        assertEquals(0, p.x); assertEquals(0, p.y); assertEquals(8, p.an); assertEquals(3, p.al); assertEquals(0, p.pagina)
    }
    @Test fun configuracionDanadaUsaValoresSeguros() {
        assertEquals(Experiencia(), Experiencia.leer("incorrecto"))
        assertEquals("medianoche", Experiencia.leer("{\"pared\":\"desconocida\"}").pared)
    }
    @Test fun habitacionIncluyeColeccionesYRecuerdosEnLaNavegacion() {
        Diptico.iniciar(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        val j = cl.nikalbazu.diptico.data.Juego("snes/a", "snes", "Aventura", "a.zip", android.net.Uri.parse("content://test/a"), "a")
        val e = Estado(listo = true, configurado = true, juegos = listOf(j), juegoSel = j.clave,
            vistaConsolas = "habitacion", enSistemas = true,
            colecciones = mapOf("c" to Coleccion("Favoritos", listOf(j.clave))), datos = mapOf(j.clave to cl.nikalbazu.diptico.data.DatosJuego(tiempo = 120)))
        Diptico.fijarParaPrueba(e)
        assertEquals(listOf("consola:todo", "consola:snes", "coleccion:c", "juego:snes/a"), Diptico.objetosHabitacion(e))
        Diptico.moverHabitacion(2)
        assertEquals(2, Diptico.estado.value.habitacionSel)
        Diptico.moverHabitacion(1)
        assertEquals(3, Diptico.estado.value.habitacionSel)
        Diptico.moverHabitacion(1)
        assertEquals(0, Diptico.estado.value.habitacionSel)
    }

}

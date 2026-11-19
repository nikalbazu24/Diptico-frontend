package cl.nikalbazu.diptico

import android.view.KeyEvent
import android.net.Uri
import cl.nikalbazu.diptico.data.Juego
import org.junit.Assert.assertTrue
import androidx.test.core.app.ApplicationProvider
import cl.nikalbazu.diptico.estado.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** El recorrido debe ejecutar acciones reales, sin mover consolas cuando el usuario está en Widgets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TourInteractivoTest {
    @Before fun iniciar() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(
            listo = true, configurado = true, intro = false, inicio = "heroe",
            modoPantallas = "dos", modoElegido = true, dosDisponibles = true,
            enSistemas = true, vistaConsolas = "carpetas", consolaSel = "snes",
            paginas = 3, paginaInicio = 1,
        ).let { it.copy(extra = it.extra.copy(coleccionViva = true)) })
    }

    private fun pulsar(tecla: Int) {
        Teclas.manejar(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, tecla, 0))
        Teclas.manejar(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, tecla, 0))
    }

    @Test fun l2DelTourAbreLosWidgetsRealmente() {
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(novedades = 3, recorrido = true))
        pulsar(KeyEvent.KEYCODE_BUTTON_L2)
        assertEquals("widgets", Diptico.estado.value.inicio)
    }

    @Test fun selectDelTourCambiaLaVistaDeCarpetas() {
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(novedades = 5, recorrido = true))
        val anterior = Diptico.estado.value.extra.vistaCarpetasCV
        pulsar(KeyEvent.KEYCODE_BUTTON_SELECT)
        assertNotEquals(anterior, Diptico.estado.value.extra.vistaCarpetasCV)
    }

    @Test fun xDelTourAbrePersonalizacionReal() {
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(novedades = 2, recorrido = true))
        pulsar(KeyEvent.KEYCODE_BUTTON_X)
        assertEquals(Panel.EDITAR_CONSOLA, Diptico.estado.value.panel)
    }

    @Test fun cadaJuegoPuedeAbrirSuSelectorDeEmuladores() {
        val juego = Juego("ps2/prueba", "ps2", "Aventura de prueba", "prueba.iso",
            Uri.parse("content://prueba/ps2/prueba"), "primary:Juegos/ps2/prueba")
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(
            juegos = listOf(juego), juegoSel = juego.clave, consolaSel = "ps2", enSistemas = false,
        ))
        Diptico.abrirRapido()
        val grupo = Diptico.gruposJuego().indexOfFirst { it.first.titulo == "Emulador y versión" }
        assertTrue("Debe existir Emulador y versión en cualquier consola", grupo >= 0)
        Diptico.abrirGrupoRapido(grupo)
        val indice = Diptico.opcionesGrupoRapido(grupo).indexOfFirst { it.titulo == "Emulador" }
        assertTrue("El juego debe tener un selector individual", indice >= 0)
        Diptico.elegirSubRapido(indice)
        assertTrue("Al pulsar A debe abrirse el listado, no ejecutar una acción vacía",
            Diptico.estado.value.desplegable >= 0)
    }

    @Test fun tourIncluyeRetoDelEditorYEmuladorPorJuego() {
        assertEquals(23, Diptico.paginasTour(Diptico.estado.value.copy(recorrido = true)).size)
        assertEquals("pestanaEditor", RecorridoVivo.retoDe(15))
        assertEquals("emuladorJuego", RecorridoVivo.retoDe(20))
        assertEquals("ajustesEmuladores", RecorridoVivo.retoDe(21))
    }

    @Test fun probarTourCambiaVistaRealDeJuegos() {
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(novedades = 8, recorrido = true))
        val antes = Diptico.estado.value.extra.vistaColeccion
        RecorridoVivo.probar(8)
        assertNotEquals(antes, Diptico.estado.value.extra.vistaColeccion)
    }

    @Test fun l1r1EnWidgetsCambianPaginaPeroNoConsola() {
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(inicio = "widgets", paginaInicio = 1, novedades = -1))
        val consola = Diptico.estado.value.consolaSel
        pulsar(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals(2, Diptico.estado.value.paginaInicio)
        assertEquals(consola, Diptico.estado.value.consolaSel)
        pulsar(KeyEvent.KEYCODE_BUTTON_L1)
        assertEquals(1, Diptico.estado.value.paginaInicio)
        assertEquals(consola, Diptico.estado.value.consolaSel)
    }
}

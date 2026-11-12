package cl.nikalbazu.diptico

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.*
import cl.nikalbazu.diptico.ui.*
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = ABAJO)
class Revision153Test {
    @get:Rule val regla = createComposeRule()
    private val juegos = listOf("gb", "snes", "psx", "gba", "nds", "psp", "n64", "androidgames", "rpgmaker").flatMap { id ->
        (1..5).map { i -> Juego("$id/$i", id, "Aventura $i", "$i.zip", Uri.parse("content://prueba/$id/$i"), "$id/$i") }
    }
    private fun abrir(ventana: Boolean = false, oscuro: Boolean = true, widgets: Boolean = false, horizontal: Boolean = false) {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        val e = Estado(configurado = true, listo = true, intro = false, juegos = juegos,
            juegoSel = "gb/1", consolaSel = "gb", vistaConsolas = "carpetas", vista = "cuadricula",
            enSistemas = !ventana, modoCarpeta = "ventana", carpetasHorizontal = horizontal,
            oscuro = oscuro, experiencia = Experiencia(movimiento = false),
            inicio = if (widgets) "widgets" else "heroe", modoPantallas = "dos", dosDisponibles = true)
        Diptico.fijarParaPrueba(e)
        Estilo.tema = Temas.incluidos.first { it.id == "diptico_vidrio" }
        Estilo.materialInterfaz = "glass"
        Idioma.fijar(ApplicationProvider.getApplicationContext(), "es")
        Estilo.decoraciones = false
        Estilo.efectosCompletos = true
        regla.setContent {
            val actual by Diptico.estado.collectAsState()
            Lienzo(actual.oscuro, Modifier.fillMaxSize()) {
                Raiz(actual, if (widgets) Rol.SUPERIOR else Rol.INFERIOR, 1)
            }
        }
        regla.waitForIdle()
    }
    private fun foto(nombre: String) { regla.onRoot().captureRoboImage("capturas/153-$nombre.png") }
    @Test fun carpetas() { abrir(); foto("carpetas") }
    @Test fun ventanaOscura() { abrir(ventana = true); regla.onNodeWithTag("carpeta-abierta").assertExists(); foto("glass-oscuro") }
    @Test fun ventanaClara() { abrir(ventana = true, oscuro = false); foto("glass-claro") }
    @Test fun widgets() { abrir(widgets = true); foto("widgets") }
    @Test fun recorridoHorizontal() { abrir(horizontal = true); foto("horizontal") }
    @Test fun laHoraYBateriaSonUnicasYPermanecenAlAbrirPaneles() {
        abrir(ventana = true)
        for (panel in listOf(Panel.NINGUNO, Panel.MENU, Panel.ESTUDIO, Panel.APPS, Panel.GALERIA)) {
            regla.runOnIdle { Diptico.actualizar { it.copy(panel = panel) } }
            // Con el diseño nuevo en todas las vistas (dos pantallas) la barra vieja no se dibuja: la hora nunca sale repetida.
            assert(regla.onAllNodesWithTag("barra-sistema").fetchSemanticsNodes().size <= 1)
            assert(regla.onAllNodesWithTag("hora-sistema", useUnmergedTree = true).fetchSemanticsNodes().size <= 1)
            assert(regla.onAllNodesWithTag("bateria-sistema", useUnmergedTree = true).fetchSemanticsNodes().size <= 1)
        }
    }
    @Config(sdk = [29])
    @Test fun ventanaEnAndroid10() { abrir(ventana = true); foto("android10") }
    @Test fun pellizcoRealConservaVistaYSeleccionYGuardaUnaParada() {
        abrir(ventana = true)
        val zona = regla.onNodeWithTag("zona-juegos", useUnmergedTree = true)
        val antes = Diptico.estado.value
        zona.performTouchInput {
            down(0, center - Offset(50f, 0f)); down(1, center + Offset(50f, 0f))
            moveTo(0, center - Offset(64f, 0f)); moveTo(1, center + Offset(64f, 0f))
        }
        assertEquals(antes.tamJuegos, Diptico.estado.value.tamJuegos)
        zona.performTouchInput { up(1); up(0) }
        regla.waitForIdle()
        val e = Diptico.estado.value
        assertEquals(1.2f, e.tamJuegos)
        assertEquals(antes.vista, e.vista)
        assertEquals(antes.juegoSel, e.juegoSel)
        assertEquals("1.2", Diptico.almacen.texto("tamJuegos"))
        assertFalse(e.enSistemas)
    }
    @Test fun cancelarElPellizcoNoGuardaLaPrevia() {
        abrir(ventana = true)
        val zona = regla.onNodeWithTag("zona-juegos", useUnmergedTree = true)
        zona.performTouchInput {
            down(0, center - Offset(50f, 0f)); down(1, center + Offset(50f, 0f))
            moveTo(0, center - Offset(70f, 0f)); moveTo(1, center + Offset(70f, 0f))
            cancel()
        }
        assertEquals(1f, Diptico.estado.value.tamJuegos)
        assertEquals("gb/1", Diptico.estado.value.juegoSel)
    }
}

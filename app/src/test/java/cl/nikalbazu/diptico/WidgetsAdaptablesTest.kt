package cl.nikalbazu.diptico

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class WidgetsAdaptablesTest {
    @get:Rule val regla = createComposeRule()
    private fun preparar(): Estado {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Idioma.fijar(ApplicationProvider.getApplicationContext(), "es")
        Estilo.materialInterfaz = "glass"
        return Estado(configurado = true, listo = true, intro = false, inicio = "widgets", editandoWidgets = true,
            panel = Panel.ESTUDIO, estudioZona = "panel", experiencia = Experiencia(movimiento = false))
    }
    @Test fun losTresApartadosConservanTodaLaPersonalizacion() {
        val e = preparar()
        val titulos = (0..2).flatMap { grupo -> Diptico.controlesEstudio(e.copy(navegacion = e.navegacion.copy(grupoEditorWidget = grupo))) }.map { it.titulo }.toSet()
        listOf("Mover", "Forma", "Ancho", "Alto", "Página", "Estilo", "Color", "Fondo", "Esquinas", "Tamaño del contenido", "Copiar el estilo a todos", "Quitar este widget").forEach { assertTrue(it, it in titulos) }
        assertFalse(Diptico.controlesEstudio(e).any { it.titulo == "Copiar el estilo a todos" })
    }
    @Test fun elCatalogoSePuedeBuscarSinOcultarLosArchivosPropios() {
        val e = preparar().copy(estudioPestana = 1)
        val imagenes = Diptico.controlesEstudio(e.copy(navegacion = e.navegacion.copy(buscarWidgets = "GIF")))
        assertTrue(imagenes.any { it.extra == "imagen" })
        assertTrue(Diptico.controlesEstudio(e.copy(navegacion = e.navegacion.copy(buscarWidgets = "zzzzzz"))).isEmpty())
    }
    @Test fun elContenidoRespetaElEspacioRealYLaEscalaElegida() {
        val w = Widget("reloj", 0, 0, 3, 2, escala = 2f)
        for (ancho in listOf(55f, 90f, 180f, 300f)) for (alto in listOf(45f, 90f, 180f)) {
            val p = presentarWidget(w, ancho, alto)
            assertTrue(p.escala in .25f..2f)
            assertTrue(p.columnas in 1..3)
            assertTrue(p.filas in 1..2)
        }
        assertTrue(presentarWidget(w, 300f, 180f).escala > presentarWidget(w, 90f, 45f).escala)
    }
    @Test fun editorDeTamano() = capturaEditor(0, "tamano")
    @Test fun editorDeDiseno() = capturaEditor(1, "diseno")
    @Config(qualifiers = "w411dp-h800dp-land-mdpi")
    @Test fun editorEnMovilConPrevia() = capturaEditor(1, "movil", movil = true)
    private fun capturaEditor(grupo: Int, nombre: String, movil: Boolean = false) {
        val e = preparar().let { it.copy(navegacion = it.navegacion.copy(grupoEditorWidget = grupo), modoPantallas = if (movil) "una_negra" else "dos", dosDisponibles = !movil) }
        Diptico.fijarParaPrueba(e)
        regla.setContent { Lienzo(true, Modifier.fillMaxSize()) { PanelEstudio(e) } }
        regla.onRoot().captureRoboImage("capturas/153-editor-$nombre.png")
    }
    @Test fun unTextoLargoSePuedeLeerCompleto() {
        preparar()
        val texto = "Un texto muy largo con todos los detalles de la partida. ".repeat(40)
        regla.setContent {
            Lienzo(true) { CompositionLocalProvider(LocalTextoWidget provides true) { Box(Modifier.size(130.dp, 55.dp)) { Texto(texto, 16f, lineas = 2) } } }
        }
        regla.onNodeWithText("↗").assertExists()
        regla.onNode(hasClickAction() and hasAnyDescendant(hasText("↗")), useUnmergedTree = true).performClick()
        regla.onNodeWithText("Texto completo").assertExists()
        regla.onNode(hasText(texto) and hasAnyAncestor(isDialog())).assertExists()
    }
}

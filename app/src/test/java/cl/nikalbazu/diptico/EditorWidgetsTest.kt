package cl.nikalbazu.diptico

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.nikalbazu.diptico.data.Almacen
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Panel
import cl.nikalbazu.diptico.estado.Widget
import cl.nikalbazu.diptico.ui.Lienzo
import cl.nikalbazu.diptico.ui.PantallaInicio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = ARRIBA)
class EditorWidgetsTest {
    @get:Rule val regla = createComposeRule()
    private val reloj = Widget("cronometro", 0, 0, 2, 1)

    private fun abrir(widgets: List<Widget> = listOf(reloj)) {
        // El editor usa su estado y persistencia. La prueba de gestos no necesita los servicios de arranque.
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        Diptico.app = contexto
        Diptico.almacen = Almacen(contexto)
        Diptico.fijarParaPrueba(Estado(
            configurado = true, listo = true, intro = false, inicio = "widgets", panel = Panel.ESTUDIO,
            editandoWidgets = true, widgetSel = 0, paginas = 2, widgets = widgets,
        ))
        regla.setContent {
            val e by Diptico.estado.collectAsState()
            Lienzo(e.oscuro, Modifier.fillMaxSize()) { PantallaInicio(e) }
        }
    }

    @Test fun elWidgetSeEstiraAntesDeSoltar() {
        abrir()
        val grilla = regla.onNodeWithTag("paginas-widgets")
        val origen = grilla.fetchSemanticsNode().boundsInRoot.topLeft
        val widget = regla.onNodeWithContentDescription("Cronómetro", useUnmergedTree = true)
        val manija = regla.onNodeWithContentDescription("Cambiar tamaño", useUnmergedTree = true)
        val ancho = widget.fetchSemanticsNode().boundsInRoot.width
        val dedo = manija.fetchSemanticsNode().boundsInRoot.center - origen
        grilla.performTouchInput {
            down(dedo)
            moveTo(dedo + Offset(ancho * .45f, 0f))
        }
        assertTrue(widget.fetchSemanticsNode().boundsInRoot.width > ancho * 1.3f)
        assertEquals(2, Diptico.estado.value.widgets.first().an)
        grilla.performTouchInput { up() }
        assertEquals(3, Diptico.estado.value.widgets.first().an)
    }

    @Test fun unArrastreContinuaEnLaOtraPaginaYGuardaAlSoltar() {
        abrir()
        val grilla = regla.onNodeWithTag("paginas-widgets")
        val vista = grilla.fetchSemanticsNode().boundsInRoot
        val centro = regla.onNodeWithContentDescription("Cronómetro", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.center - vista.topLeft
        grilla.performTouchInput {
            down(centro)
            moveTo(Offset(vista.width - 5f, centro.y))
        }
        regla.waitUntil(4000) { Diptico.estado.value.paginaInicio == 1 }
        assertEquals(0, Diptico.estado.value.widgets.first().pagina)
        grilla.performTouchInput { moveTo(Offset(vista.width / 2, centro.y)); up() }
        assertEquals(1, Diptico.estado.value.widgets.first().pagina)
        assertTrue(Diptico.estado.value.editandoWidgets)
    }

    @Test fun unaPaginaLlenaConservaElWidgetEnOrigen() {
        val llena = Widget("juego", 0, 0, 6, 4, pagina = 1)
        val originales = listOf(reloj, llena)
        abrir(originales)
        regla.runOnIdle { Diptico.ponerWidget(0, 0, 0, 1) }
        assertEquals(originales, Diptico.estado.value.widgets)
    }
}

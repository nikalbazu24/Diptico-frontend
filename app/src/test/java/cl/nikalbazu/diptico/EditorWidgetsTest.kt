package cl.nikalbazu.diptico

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.nikalbazu.diptico.data.Almacen
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Panel
import cl.nikalbazu.diptico.estado.Widget
import cl.nikalbazu.diptico.estado.Widgets
import cl.nikalbazu.diptico.ui.ArrastreWidget
import cl.nikalbazu.diptico.ui.CarpetasConsolas
import cl.nikalbazu.diptico.ui.Escala
import cl.nikalbazu.diptico.ui.LocalEscala
import cl.nikalbazu.diptico.ui.MedidasEditorWidgets
import cl.nikalbazu.diptico.ui.editarConDedo
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

    private fun preparar(widgets: List<Widget>) {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        Diptico.app = contexto
        Diptico.almacen = Almacen(contexto)
        Diptico.fijarParaPrueba(Estado(
            configurado = true, listo = true, intro = false, inicio = "widgets", panel = Panel.ESTUDIO,
            editandoWidgets = true, widgetSel = 0, paginas = 2, widgets = widgets,
        ))
    }

    private fun abrir() {
        preparar(listOf(reloj))
        // La prueba de entrada monta el mismo detector de punteros que el inicio.
        // Las capturas cubren por separado la composición completa con el paginador.
        regla.setContent {
            val e by Diptico.estado.collectAsState()
            val densidad = LocalDensity.current
            val medidas = remember(densidad) {
                with(densidad) { MedidasEditorWidgets(280.dp.toPx(), 220.dp.toPx(), 8.dp.toPx(), 6.dp.toPx(), 6, 4, 24.dp.toPx()) }
            }
            var arrastre by remember { mutableStateOf<ArrastreWidget?>(null) }
            Box(Modifier.size(280.dp, 220.dp).testTag("paginas-widgets").editarConDedo(medidas, false) { arrastre = it }) {
                e.widgets.forEachIndexed { i, w ->
                    if (w.pagina == e.paginaInicio) {
                        val rect = medidas.rect(w)
                        val tam = arrastre?.takeIf { it.indice == i && it.estirando }?.tamano(medidas)
                            ?: Offset(rect.width, rect.height)
                        with(densidad) {
                            Box(Modifier.offset(rect.left.toDp(), rect.top.toDp()).size(tam.x.toDp(), tam.y.toDp())
                                .semantics { contentDescription = Widgets.nombre(w.tipo) }) {
                                if (e.widgetSel == i) Box(Modifier.align(Alignment.BottomEnd).size(20.dp)
                                    .semantics { contentDescription = "Cambiar tamaño" })
                            }
                        }
                    }
                }
            }
        }
    }

    @Test fun lasCarpetasSeAgrandanDuranteElPellizcoYSeDesplazanConUnDedo() {
        preparar(emptyList())
        val juegos = Consolas.todas.take(24).map { c ->
            Juego("${c.id}/juego", c.id, "Juego", "juego.zip", Uri.parse("content://prueba/${c.id}"), c.id)
        }
        Diptico.fijarParaPrueba(Estado(
            configurado = true, listo = true, intro = false, vistaConsolas = "carpetas",
            enSistemas = true, juegos = juegos,
        ))
        regla.setContent {
            val e by Diptico.estado.collectAsState()
            CompositionLocalProvider(LocalEscala provides Escala(.55f)) {
                CarpetasConsolas(e, Modifier.size(300.dp, 225.dp).testTag("cuadricula-carpetas"))
            }
        }
        val grilla = regla.onNodeWithTag("cuadricula-carpetas")
        val carpeta = regla.onNodeWithText("Todo")
        val original = carpeta.fetchSemanticsNode().boundsInRoot.width
        grilla.performTouchInput {
            val centro = center
            down(0, centro - Offset(width * .07f, 0f))
            down(1, centro + Offset(width * .07f, 0f))
            moveTo(0, centro - Offset(width * .11f, 0f))
            moveTo(1, centro + Offset(width * .11f, 0f))
        }
        assertTrue(carpeta.fetchSemanticsNode().boundsInRoot.width > original * 1.25f)
        assertEquals(1f, Diptico.estado.value.tamCarpetas, .001f)
        grilla.performTouchInput { up(1); up(0) }
        val guardado = Diptico.estado.value.tamCarpetas
        assertTrue(guardado > 1.25f)
        assertEquals(guardado, Diptico.almacen.texto("tamCarpetas").toFloat(), .001f)
        assertTrue(Diptico.estado.value.enSistemas)

        regla.runOnIdle { Diptico.alternarRecorridoCarpetas() }
        val horizontal = regla.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange))
        horizontal.performTouchInput { swipeLeft() }
        regla.waitForIdle()
        assertTrue(horizontal.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].value() > 0f)
        assertTrue(Diptico.almacen.bool("carpetasHorizontal", false))
        assertEquals(guardado, Diptico.estado.value.tamCarpetas, .001f)
        assertTrue(Diptico.estado.value.enSistemas)
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

    @Test fun soltarEnZonaDeQuitarOcultaElWidget() {
        abrir()
        val grilla = regla.onNodeWithTag("paginas-widgets")
        val vista = grilla.fetchSemanticsNode().boundsInRoot
        val centro = regla.onNodeWithContentDescription("Cronómetro", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.center - vista.topLeft
        grilla.performTouchInput {
            down(centro)
            moveTo(Offset(vista.width / 2f, vista.height * 0.9f))
        }
        assertTrue(Diptico.estado.value.widgets.first().visible)
        grilla.performTouchInput { up() }
        assertEquals(false, Diptico.estado.value.widgets.first().visible)
        assertTrue(Diptico.estado.value.editandoWidgets)
    }

    @Test fun agregarEnDosPantallasUsaElCatalogoSinBurbuja() {
        preparar(listOf(reloj))
        Diptico.actualizar { it.copy(dosDisponibles = true, modoPantallas = "dos") }
        Diptico.abrirBurbuja(2, 1)
        val e = Diptico.estado.value
        assertEquals(null, e.burbuja)
        assertEquals(2 to 1, e.celdaCatalogo)
        assertEquals(1, e.estudioPestana)
        assertEquals("panel", e.estudioZona)
    }

    @Test fun unaPaginaLlenaConservaElWidgetEnOrigen() {
        val llena = Widget("juego", 0, 0, 6, 4, pagina = 1)
        val originales = listOf(reloj, llena)
        preparar(originales)
        Diptico.ponerWidget(0, 0, 0, 1)
        assertEquals(originales, Diptico.estado.value.widgets)
    }
}

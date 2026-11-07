package cl.nikalbazu.diptico.redisenio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w500dp-h300dp-land-mdpi")
class FondoVentanaTest {
    @get:Rule val regla = createComposeRule()

    @Test fun laVentanaRecibeLosColoresDeLaEscenaRealEnSusCoordenadas() {
        val fondo = FondoVentanaCristal()
        regla.setContent {
            Row(Modifier.size(480.dp, 240.dp).fondoDeVentana(fondo)) {
                Box(Modifier.weight(1f).fillMaxHeight().background(Color.Red))
                Box(Modifier.weight(1f).fillMaxHeight().background(Color.Blue))
            }
        }
        regla.onRoot().captureRoboImage("capturas-renacer/31-fondo-escena.png")
        regla.runOnIdle {
            val imagen = requireNotNull(fondo.imagen)
            assertTrue(imagen.width < fondo.dimensiones.width)
            val pixeles = imagen.toPixelMap()
            val izquierda = pixeles[imagen.width / 8, imagen.height / 2]
            val derecha = pixeles[imagen.width * 7 / 8, imagen.height / 2]
            assertTrue(izquierda.red > .95f && izquierda.blue < .05f)
            assertTrue(derecha.blue > .95f && derecha.red < .05f)
            val centro = pixeles[imagen.width / 2, imagen.height / 2]
            assertTrue("La unión se difumina", centro.red > .1f && centro.blue > .1f)
        }
    }

    @Test fun redimensionarLaCarpetaRegeneraLaInstantaneaSinEstirarLaAnterior() {
        val fondo = FondoVentanaCristal()
        var ancho by mutableStateOf(240)
        regla.setContent { Box(Modifier.size(ancho.dp, 120.dp).fondoDeVentana(fondo).background(Color.Green)) }
        var anterior: androidx.compose.ui.graphics.ImageBitmap? = null
        regla.onRoot().captureRoboImage("capturas-renacer/32-fondo-antes.png")
        regla.runOnIdle { anterior = requireNotNull(fondo.imagen); ancho = 400 }
        regla.onRoot().captureRoboImage("capturas-renacer/33-fondo-despues.png")
        regla.runOnIdle {
            val actual = requireNotNull(fondo.imagen)
            assertNotSame(anterior, actual)
            assertTrue(actual.width > requireNotNull(anterior).width)
            assertEquals(400, fondo.dimensiones.width)
            assertTrue(actual.toPixelMap()[actual.width / 2, actual.height / 2].green > .95f)
        }
    }
}

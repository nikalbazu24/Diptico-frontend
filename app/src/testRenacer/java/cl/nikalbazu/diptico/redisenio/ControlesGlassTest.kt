package cl.nikalbazu.diptico.redisenio

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import cl.nikalbazu.diptico.data.Almacen
import cl.nikalbazu.diptico.estado.*
import cl.nikalbazu.diptico.ui.*
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w600dp-h340dp-land-xhdpi")
class ControlesGlassTest {
    @get:Rule val regla = createComposeRule()

    @Test(timeout = 45000) fun losDosDeslizadoresCambianSoloSuZonaYGuardanLosValores() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        Diptico.iniciar(ctx)
        Diptico.app = ctx
        Diptico.almacen = Almacen(ctx)
        Diptico.fijarParaPrueba(Estado(listo = true, configurado = true, materialInterfaz = "glass"))
        Estilo.tema = Temas.incluidos.first { it.id == "diptico_vidrio" }
        Estilo.materialInterfaz = "glass"
        regla.mainClock.autoAdvance = false
        regla.setContent {
            val e by Diptico.estado.collectAsState()
            Lienzo(true, Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Texto("Intensidad del Glass", 22f)
                    Spacer(Modifier.height(12.dp))
                    remember(e.nivelesGlass) { Diptico.opcionesSeccion(Seccion.APARIENCIA) }.filter { it.rango != null }.forEach { opcion ->
                        FilaOpcion(opcion, false, LocalPaleta.current.acentoTexto) { }
                        Spacer(Modifier.height(8.dp))
                    }
                    Detalle("0 % · Sólido        100 % · Máxima intensidad", 11f)
                }
            }
        }
        regla.mainClock.advanceTimeBy(200)
        regla.onNodeWithTag("Glass de carpetas").performSemanticsAction(SemanticsActions.SetProgress) { it(30f) }
        regla.mainClock.advanceTimeBy(100)
        regla.runOnIdle { assertEquals(NivelesGlass(30, 55), Diptico.estado.value.nivelesGlass) }
        regla.onNodeWithTag("Glass de widgets").performSemanticsAction(SemanticsActions.SetProgress) { it(80f) }
        regla.mainClock.advanceTimeBy(100)
        regla.runOnIdle { assertEquals(NivelesGlass(30, 80), Almacen(ctx).nivelesGlass) }
        regla.onRoot().captureRoboImage("capturas-renacer/35-glass-independiente-ajustes.png")
    }
}

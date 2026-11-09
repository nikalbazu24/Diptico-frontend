package cl.nikalbazu.diptico

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.ui.Lienzo
import cl.nikalbazu.diptico.ui.Raiz
import cl.nikalbazu.diptico.ui.Rol
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** En la pantalla de arriba, deslizar con el dedo pasa de la portada a los widgets y de vuelta. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = ARRIBA)
class DeslizarTest {
    @get:Rule val regla = createComposeRule()

    @Test fun deslizarArribaLlevaALosWidgetsYVuelve() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(
            listo = true, configurado = true, tieneCarpeta = true, modoPantallas = "dos", modoElegido = true,
            dosDisponibles = true, intro = false, vista = "mosaicos", inicio = "heroe",
        ))
        regla.setContent {
            val e by Diptico.estado.collectAsState()
            Lienzo(e.oscuro, Modifier.fillMaxSize(), e.tamTexto) { Raiz(e, Rol.SUPERIOR, 0) }
        }
        regla.onNodeWithTag("inicio-deslizable").performTouchInput { swipeLeft() }
        regla.waitForIdle()
        assertEquals("widgets", Diptico.estado.value.inicio)
        regla.onNodeWithTag("inicio-deslizable").performTouchInput { swipeRight() }
        regla.waitForIdle()
        assertEquals("heroe", Diptico.estado.value.inicio)
    }
}

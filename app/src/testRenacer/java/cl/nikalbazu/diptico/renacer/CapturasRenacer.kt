package cl.nikalbazu.diptico.renacer

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.TiendaTemas
import cl.nikalbazu.diptico.estado.Temas
import cl.nikalbazu.diptico.estado.TemaComunidad
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h600dp-land-xhdpi")
class CapturasRenacer {
    @get:Rule val regla = createComposeRule()
    private val juegos = listOf(
        Juego("snes/1", "snes", "Nebula Rift", "jardin.sfc", Uri.parse("content://prueba/1"), "1"),
        Juego("gba/2", "gba", "Circuit Breakers", "faro.gba", Uri.parse("content://prueba/2"), "2"),
        Juego("psx/3", "psx", "Temple of Echoes", "estacion.chd", Uri.parse("content://prueba/3"), "3"),
        Juego("gc/4", "gc", "Skyward Atlas", "isla.iso", Uri.parse("content://prueba/4"), "4"),
    )
    private val base = Salon(juegos = juegos, elegido = juegos[0].clave, cargando = false, calendario = false,
        datos = mapOf(juegos[0].clave to DatosJuego(ultimo = 2, favorito = true, paso = "Sector 04 · Recuperar el núcleo de energía."),
            juegos[1].clave to DatosJuego(ultimo = 1)))

    private val catalogo by lazy { TiendaTemas.incluidos(ApplicationProvider.getApplicationContext()) }

    private fun mostrar(estado: Salon, companera: Boolean = false, accion: (Accion) -> Unit = {}) {
        Consolas.cargar(ApplicationProvider.getApplicationContext())
        regla.setContent { SalonRenacer(estado.copy(temas = estado.temas.ifEmpty { catalogo }), accion, companera = companera) }
    }
    private fun foto(nombre: String, estado: Salon, companera: Boolean = false) {
        mostrar(estado.copy(movimiento = false), companera)
        regla.waitForIdle()
        regla.onRoot().captureRoboImage("capturas-renacer/$nombre.png")
    }
    @Test fun inicio() = foto("01-inicio", base)
    @Test fun biblioteca() = foto("02-biblioteca", base.copy(lugar = Lugar.BIBLIOTECA))
    @Test fun ambiente() = foto("03-ambiente", base.copy(lugar = Lugar.AMBIENTE))
    @Test fun temaTuberias() = foto("04-tuberias", base.copy(temaId = catalogo.first { it.pistaLocal == "tuberias.ogg" }.tema.id))
    @Test fun temaReliquias() = foto("09-reliquias", base.copy(temaId = catalogo.first { it.pistaLocal == "reliquias.ogg" }.tema.id))
    @Test fun temaChile() = foto("10-chile", base.copy(temaId = catalogo.first { it.pistaLocal == "chile.ogg" }.tema.id))
    @Test fun temaNavidad() {
        val navidad = Temas.fiestas.first { it.id == "navidad" }.tema
        foto("11-navidad", base.copy(temas = listOf(TemaComunidad(navidad, "Díptico", null)), temaId = navidad.id))
    }
    @Test fun vitrina() = foto("05-arriba", base, companera = true)
    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun pantallaInferior() = foto("06-abajo", base.copy(lugar = Lugar.BIBLIOTECA))
    @Config(qualifiers = "w390dp-h844dp-port-xhdpi")
    @Test fun telefono() = foto("07-telefono", base)
    @Test fun bienvenida() = foto("08-bienvenida", Salon(cargando = false, calendario = false))
    @Test fun ocultosTieneUnaSalidaVisible() {
        var ultima: Accion? = null
        mostrar(base.copy(lugar = Lugar.BIBLIOTECA, filtro = Filtro.OCULTOS)) { ultima = it }
        regla.onNodeWithText("Limpiar filtros").assertIsDisplayed().performClick()
        assertEquals(Accion.Limpiar, ultima)
    }
    @Test fun jugarSalePorLaAccionDeLanzamiento() {
        var ultima: Accion? = null
        mostrar(base) { ultima = it }
        regla.onNodeWithText("Continuar").assertIsDisplayed().performClick()
        assertEquals(Accion.Jugar(juegos[0].clave, 0), ultima)
    }
}

package cl.nikalbazu.diptico.redisenio

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Experiencia
import cl.nikalbazu.diptico.estado.Panel
import cl.nikalbazu.diptico.estado.Tema
import cl.nikalbazu.diptico.estado.Temas
import cl.nikalbazu.diptico.ui.Estilo
import cl.nikalbazu.diptico.ui.Imagenes
import cl.nikalbazu.diptico.ui.Lienzo
import cl.nikalbazu.diptico.ui.LocalRedisenio
import cl.nikalbazu.diptico.ui.Raiz
import cl.nikalbazu.diptico.ui.Rol
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w731dp-h411dp-land-xhdpi")
class CapturasRedisenio {
    @get:Rule val regla = createComposeRule()
    private fun imagen(nombre: String): Uri {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val archivo = File(ctx.cacheDir, nombre)
        requireNotNull(javaClass.getResourceAsStream("/renacer/$nombre")).use { archivo.writeBytes(it.readBytes()) }
        return Uri.fromFile(archivo).also { uri ->
            listOf(96, 256, 320, 350, 480, 700, 1100, 1600).forEach { Imagenes.cargar(ctx, uri, it) }
        }
    }
    private val juegos by lazy { listOf(
        Juego("snes/1", "snes", "Super Mario World", "mario.sfc", Uri.parse("content://prueba/1"), "1",
            portada = imagen("mario-portada.png"), arte = imagen("mario-captura.png")),
        Juego("gba/2", "gba", "Pokémon Emerald", "pokemon.gba", Uri.parse("content://prueba/2"), "2", portada = imagen("pokemon-portada.png")),
        Juego("psx/3", "psx", "Temple of Echoes", "temple.chd", Uri.parse("content://prueba/3"), "3"),
        Juego("gc/4", "gc", "Skyward Atlas", "skyward.iso", Uri.parse("content://prueba/4"), "4"),
        Juego("psp/5", "psp", "Neon Rally", "neon.iso", Uri.parse("content://prueba/5"), "5"),
        Juego("nds/6", "nds", "Star Route DS", "star.nds", Uri.parse("content://prueba/6"), "6"),
    ) }
    private val base by lazy { Estado(listo = true, configurado = true, tieneCarpeta = true, juegos = juegos,
        modoPantallas = "dos", modoElegido = true, dosDisponibles = true, juegoSel = juegos[0].clave, consolaSel = "snes",
        vistaConsolas = "carpetas", enSistemas = true, intro = false, novedades = -1, vista = "cuadricula",
        temaFestivo = false, zoomLento = false, experiencia = Experiencia(movimiento = false),
        datos = mapOf(juegos[0].clave to DatosJuego(ultimo = 2, favorito = true, tiempo = 12300,
            paso = "Buscar la salida secreta de la casa fantasma."))) }

    private fun mostrar(e: Estado, rol: Rol, tema: Tema = Temas.incluidos[0], nuevo: Boolean = true) {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Consolas.cargar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(e)
        Estilo.tema = tema
        Estilo.decoraciones = tema.decor != "nada"
        Estilo.efectosCompletos = false
        regla.mainClock.autoAdvance = false
        regla.setContent {
            val estado by Diptico.estado.collectAsState()
            CompositionLocalProvider(LocalRedisenio provides nuevo) {
                Lienzo(e.oscuro, Modifier.fillMaxSize(), e.tamTexto) { Raiz(estado, rol, 0) }
            }
        }
        regla.mainClock.advanceTimeBy(1600)
    }
    private fun foto(nombre: String, e: Estado = base, rol: Rol = Rol.SUPERIOR, tema: Tema = Temas.incluidos[0], nuevo: Boolean = true) {
        mostrar(e, rol, tema, nuevo)
        regla.onRoot().captureRoboImage("capturas-renacer/$nombre.png")
    }

    @Test fun anterior() = foto("00-diseno-anterior", base.copy(vistaConsolas = "arriba", enSistemas = false), nuevo = false)
    @Test fun escena() = foto("01-escena")
    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun carpetas() = foto("02-carpetas", rol = Rol.INFERIOR)
    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun carpetaAbierta() = foto("03-carpeta-abierta", base.copy(enSistemas = false), Rol.INFERIOR)
    @Test fun unaPantalla() = foto("04-una-pantalla", base.copy(modoPantallas = "una_negra"), Rol.UNICA)
    @Config(qualifiers = "w390dp-h844dp-port-xhdpi")
    @Test fun telefono() = foto("05-telefono", base.copy(modoPantallas = "una_negra"), Rol.UNICA)
    @Test fun widgets() = foto("06-widgets", base.copy(inicio = "widgets"))
    @Test fun chile() = foto("07-chile", tema = Temas.fiestas.first { it.id == "fiestaspatrias" }.tema)
    @Test fun navidad() = foto("08-navidad", tema = Temas.fiestas.first { it.id == "navidad" }.tema)

    @Test fun elDockAbreWidgetsYConservaLaSeleccion() {
        mostrar(base, Rol.SUPERIOR)
        regla.onNodeWithContentDescription("Widgets").assertIsDisplayed().performClick()
        assertEquals("widgets", Diptico.estado.value.inicio)
        assertEquals(base.juegoSel, Diptico.estado.value.juegoSel)
        assertEquals(base.juegos, Diptico.estado.value.juegos)
    }

    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun laBusquedaDeLaCabeceraAbreElPanelCompleto() {
        mostrar(base, Rol.INFERIOR)
        regla.onNodeWithContentDescription("Buscar juegos").assertIsDisplayed().performClick()
        assertEquals(Panel.BUSCAR, Diptico.estado.value.panel)
    }

    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun abrirCarpetaConservaElJuegoYVolverRecuperaLaCuadricula() {
        mostrar(base, Rol.INFERIOR)
        regla.onNodeWithTag("carpeta-gba").assertIsDisplayed().performClick()
        assertEquals("gba", Diptico.estado.value.consolaSel)
        assertEquals("gba/2", Diptico.estado.value.juegoSel)
        assertTrue(Diptico.estado.value.carpetaEnVentana)
        Diptico.volverASistemas()
        assertTrue(Diptico.estado.value.enSistemas)
        assertEquals("gba", Diptico.estado.value.consolaSel)
    }
}

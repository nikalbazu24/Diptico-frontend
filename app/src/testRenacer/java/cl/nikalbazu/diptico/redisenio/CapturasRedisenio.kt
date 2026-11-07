package cl.nikalbazu.diptico.redisenio

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
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
            listOf(96, 240, 256, 400, 480, 700, 1600).forEach {
                Imagenes.cargar(ctx, uri, it, memoriaGrafica = android.os.Build.VERSION.SDK_INT >= 31)
            }
            Imagenes.cargar(ctx, uri, 96, memoriaGrafica = false)
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
        Estilo.efectosCompletos = tema.forma == "cristal" && e.fluidez != "ahorro"
        regla.mainClock.autoAdvance = false
        regla.setContent {
            val estado by Diptico.estado.collectAsState()
            CompositionLocalProvider(LocalRedisenio provides nuevo) {
                Lienzo(e.oscuro, Modifier.fillMaxSize(), e.tamTexto) { Raiz(estado, rol, 0) }
            }
        }
        regla.mainClock.advanceTimeBy(1600)
        regla.runOnIdle {
            Diptico.fijarParaPrueba(e.copy(tema = tema.id))
            Diptico.aplicarTema()
            Estilo.tema = tema
            Estilo.efectosCompletos = tema.forma == "cristal" && e.fluidez != "ahorro"
            Estilo.decoraciones = tema.decor != "nada"
            cl.nikalbazu.diptico.estado.Idioma.fijar(ApplicationProvider.getApplicationContext(), "es")
        }
        regla.mainClock.advanceTimeBy(100)
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
    @Test fun ventanaEnUnaPantalla() = foto("09-ventana-una-pantalla", base.copy(modoPantallas = "una_negra", enSistemas = false), Rol.UNICA)
    @Config(sdk = [29])
    @Test fun ventanaEnAndroid10() = foto("11-ventana-android10", base.copy(modoPantallas = "una_negra", enSistemas = false), Rol.UNICA)
    @Test fun carpetaCompletaEnUnaPantalla() = foto("10-carpeta-completa", base.copy(modoPantallas = "una_negra", enSistemas = false, modoCarpeta = "completa"), Rol.UNICA)
    @Config(qualifiers = "w390dp-h844dp-port-xhdpi")
    @Test fun telefono() = foto("05-telefono", base.copy(modoPantallas = "una_negra"), Rol.UNICA)
    @Test fun widgets() = foto("06-widgets", base.copy(inicio = "widgets"))
    @Test fun chile() = foto("07-chile", tema = Temas.fiestas.first { it.id == "fiestaspatrias" }.tema)
    @Test fun navidad() = foto("08-navidad", tema = Temas.fiestas.first { it.id == "navidad" }.tema)
    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun seleccionLimpia() = foto("12-seleccion-limpia", base.copy(juegos = juegos.take(2), consolaSel = "todo", enSistemas = false), Rol.INFERIOR)
    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun opcionesDeJuegos() = foto("13-opciones-juegos", base.copy(enSistemas = false, panel = Panel.GALERIA, galeria = "biblioteca"), Rol.INFERIOR)
    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun filtroConSalida() = foto("14-filtro-activo", base.copy(enSistemas = false, filtro = "favoritos"), Rol.INFERIOR)

    private val cristal get() = Temas.incluidos.first { it.id == "diptico_vidrio" }
    @Test fun cristalEscena() = foto("15-cristal-escena", tema = cristal)
    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun cristalColeccion() = foto("16-cristal-carpetas", rol = Rol.INFERIOR, tema = cristal)
    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun cristalSeleccion() = foto("17-cristal-seleccion", base.copy(juegos = juegos.take(2), consolaSel = "todo", enSistemas = false), Rol.INFERIOR, cristal)
    @Test fun cristalWidgets() = foto("18-cristal-widgets", base.copy(inicio = "widgets"), tema = cristal)
    @Test fun cristalClaro() = foto("19-cristal-claro", base.copy(oscuro = false), tema = cristal)
    @Test fun cristalAjustes() = foto("20-cristal-ajustes", base.copy(panel = Panel.MENU, seccion = cl.nikalbazu.diptico.estado.Seccion.APARIENCIA, enOpciones = true), tema = cristal)
    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun cristalAhorro() = foto("21-cristal-ahorro", base.copy(enSistemas = false, fluidez = "ahorro"), Rol.INFERIOR, cristal)
    @Config(sdk = [29])
    @Test fun cristalAndroid10() = foto("22-cristal-android10", base.copy(modoPantallas = "una_negra", enSistemas = false), Rol.UNICA, cristal)
    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun cristalContraste() {
        val cr = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        android.provider.Settings.Secure.putInt(cr, "high_text_contrast_enabled", 1)
        try { foto("23-cristal-contraste", base.copy(enSistemas = false), Rol.INFERIOR, cristal) }
        finally { android.provider.Settings.Secure.putInt(cr, "high_text_contrast_enabled", 0) }
    }

    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun cristalPermiteAbrirOpcionesYVolverALaCarpeta() {
        mostrar(base.copy(enSistemas = false, experiencia = base.experiencia.copy(movimiento = true)), Rol.INFERIOR, cristal)
        regla.onNodeWithContentDescription("Opciones de juegos").assertIsDisplayed().performTouchInput { down(center); up() }
        regla.mainClock.advanceTimeBy(600)
        assertEquals("biblioteca", Diptico.estado.value.galeria)
        regla.runOnIdle { Diptico.atras() }
        regla.mainClock.advanceTimeBy(600)
        assertEquals(Panel.NINGUNO, Diptico.estado.value.panel)
        assertEquals(base.juegoSel, Diptico.estado.value.juegoSel)
    }

    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun lasOpcionesPermitenFiltrarYRegresarALaSeleccionSinPerderLaCarpeta() {
        mostrar(base.copy(enSistemas = false), Rol.INFERIOR)
        regla.onNodeWithContentDescription("Opciones de juegos").assertIsDisplayed().performClick()
        regla.mainClock.advanceTimeBy(600)
        regla.onNodeWithText("Filtrar").assertIsDisplayed().performClick()
        regla.mainClock.advanceTimeBy(600)
        regla.onNodeWithText("Favoritos").assertIsDisplayed().performClick()
        regla.mainClock.advanceTimeBy(600)
        assertEquals(Panel.NINGUNO, Diptico.estado.value.panel)
        assertEquals("favoritos", Diptico.estado.value.filtro)
        assertEquals("snes", Diptico.estado.value.consolaSel)
        assertFalse(Diptico.estado.value.enSistemas)
        regla.onNodeWithText("Mostrar todos").assertIsDisplayed().performClick()
        assertEquals("todos", Diptico.estado.value.filtro)
        assertEquals(base.juegoSel, Diptico.estado.value.juegoSel)
    }

    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun unaBusquedaDeOcultosSinResultadosTieneSalidaVisible() {
        mostrar(base.copy(enSistemas = false, filtro = "ocultos", busqueda = "nada"), Rol.INFERIOR)
        regla.onNodeWithText("Mostrar todos").assertIsDisplayed().performClick()
        assertEquals("todos", Diptico.estado.value.filtro)
        assertEquals("", Diptico.estado.value.busqueda)
        assertEquals("snes", Diptico.estado.value.consolaSel)
        assertEquals(1, Diptico.estado.value.visibles.size)
    }

    @Config(qualifiers = "w472dp-h411dp-land-xhdpi")
    @Test fun personalizarDesdeOpcionesConservaLaConsola() {
        mostrar(base.copy(enSistemas = false), Rol.INFERIOR)
        regla.onNodeWithContentDescription("Opciones de juegos").performClick()
        regla.mainClock.advanceTimeBy(600)
        regla.onNodeWithText("Personalizar carpeta").performClick()
        assertEquals(Panel.EDITAR_CONSOLA, Diptico.estado.value.panel)
        assertEquals("snes", Diptico.estado.value.consolaEditando)
    }

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

    @Test fun ampliarCarpetaUsaTodoElAnchoEnUnaPantalla() {
        mostrar(base.copy(modoPantallas = "una_negra", enSistemas = false), Rol.UNICA)
        regla.onNodeWithText("Ampliar").assertIsDisplayed().performClick()
        regla.mainClock.advanceTimeBy(600)
        assertEquals("completa", Diptico.estado.value.modoCarpeta)
        regla.onNodeWithTag("carpeta-completa").assertIsDisplayed().assertWidthIsAtLeast(700.dp)
        Diptico.volverASistemas()
        assertTrue(Diptico.estado.value.enSistemas)
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

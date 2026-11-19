package cl.nikalbazu.diptico

import android.view.KeyEvent
import android.net.Uri
import cl.nikalbazu.diptico.data.Juego
import org.junit.Assert.assertTrue
import androidx.test.core.app.ApplicationProvider
import cl.nikalbazu.diptico.estado.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** El recorrido debe ejecutar acciones reales, sin mover consolas cuando el usuario está en Widgets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TourInteractivoTest {
    @Before fun iniciar() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(
            listo = true, configurado = true, intro = false, inicio = "heroe",
            modoPantallas = "dos", modoElegido = true, dosDisponibles = true,
            enSistemas = true, vistaConsolas = "carpetas", consolaSel = "snes",
            paginas = 3, paginaInicio = 1,
        ).let { it.copy(extra = it.extra.copy(coleccionViva = true)) })
    }

    private fun pulsar(tecla: Int) {
        Teclas.manejar(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, tecla, 0))
        Teclas.manejar(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, tecla, 0))
    }

    @Test fun l2DelTourAbreLosWidgetsRealmente() {
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(novedades = 3, recorrido = true))
        pulsar(KeyEvent.KEYCODE_BUTTON_L2)
        assertEquals("widgets", Diptico.estado.value.inicio)
    }

    @Test fun selectDelTourCambiaLaVistaDeCarpetas() {
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(novedades = 5, recorrido = true))
        val anterior = Diptico.estado.value.extra.vistaCarpetasCV
        pulsar(KeyEvent.KEYCODE_BUTTON_SELECT)
        assertNotEquals(anterior, Diptico.estado.value.extra.vistaCarpetasCV)
    }

    @Test fun xDelTourAbrePersonalizacionReal() {
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(novedades = 2, recorrido = true))
        pulsar(KeyEvent.KEYCODE_BUTTON_X)
        assertEquals(Panel.EDITAR_CONSOLA, Diptico.estado.value.panel)
    }

    @Test fun cadaJuegoPuedeAbrirSuSelectorDeEmuladores() {
        val juego = Juego("ps2/prueba", "ps2", "Aventura de prueba", "prueba.iso",
            Uri.parse("content://prueba/ps2/prueba"), "primary:Juegos/ps2/prueba")
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(
            juegos = listOf(juego), juegoSel = juego.clave, consolaSel = "ps2", enSistemas = false,
        ))
        Diptico.abrirRapido()
        val grupo = Diptico.gruposJuego().indexOfFirst { it.first.titulo == "Emulador y versión" }
        assertTrue("Debe existir Emulador y versión en cualquier consola", grupo >= 0)
        Diptico.abrirGrupoRapido(grupo)
        val indice = Diptico.opcionesGrupoRapido(grupo).indexOfFirst { it.titulo == "Emulador" }
        assertTrue("El juego debe tener un selector individual", indice >= 0)
        Diptico.elegirSubRapido(indice)
        assertTrue("Al pulsar A debe abrirse el listado, no ejecutar una acción vacía",
            Diptico.estado.value.desplegable >= 0)
    }

    @Test fun tourIncluyeRetoDelEditorYEmuladorPorJuego() {
        assertEquals(23, Diptico.paginasTour(Diptico.estado.value.copy(recorrido = true)).size)
        assertEquals("pestanaEditor", RecorridoVivo.retoDe(15))
        assertEquals("emuladorJuego", RecorridoVivo.retoDe(20))
        assertEquals("ajustesEmuladores", RecorridoVivo.retoDe(21))
    }

    @Test fun probarTourCambiaVistaRealDeJuegos() {
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(novedades = 8, recorrido = true))
        val antes = Diptico.estado.value.extra.vistaColeccion
        RecorridoVivo.probar(8)
        assertNotEquals(antes, Diptico.estado.value.extra.vistaColeccion)
    }


    private fun prepararCarpetaCV(unaPantalla: Boolean = false) {
        val juegos = listOf(
            Juego("snes/celda", "snes", "Celda", "celda.sfc",
                Uri.parse("content://prueba/snes/celda"), "primary:Juegos/snes/celda"),
            Juego("ps2/ruta", "ps2", "Ruta", "ruta.iso",
                Uri.parse("content://prueba/ps2/ruta"), "primary:Juegos/ps2/ruta"),
        )
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(
            juegos = juegos, consolaSel = "snes", juegoSel = "snes/celda",
            enSistemas = false, vistaConsolas = "carpetas", modoCarpeta = "ventana",
            modoPantallas = if (unaPantalla) "una_negra" else "dos",
            dosDisponibles = !unaPantalla, inicio = "heroe",
            paginas = 3, paginaInicio = 1, novedades = -1, recorrido = false,
            extra = Diptico.estado.value.extra.copy(coleccionViva = true, vistaColeccion = "estante"),
        ))
        assertTrue("La carpeta debe ser una ventana para reproducir el error", Diptico.estado.value.carpetaEnVentana)
        assertTrue("Se necesitan varias consolas para detectar cambios ocultos",
            Diptico.estado.value.pestanas.size >= 2)
    }

    @Test fun selectDentroDeCarpetaAbiertaCambiaVistaSinIrAWidgets() {
        prepararCarpetaCV()
        val anteriorConsola = Diptico.estado.value.consolaSel
        val anteriorJuego = Diptico.estado.value.juegoSel
        for (vista in listOf("cuadricula", "lista", "estante")) {
            pulsar(KeyEvent.KEYCODE_BUTTON_SELECT)
            assertEquals("Select debe pasar a $vista", vista, Diptico.estado.value.extra.vistaColeccion)
            assertEquals("Select no debe abrir widgets", "heroe", Diptico.estado.value.inicio)
            assertEquals(anteriorConsola, Diptico.estado.value.consolaSel)
            assertEquals(anteriorJuego, Diptico.estado.value.juegoSel)
        }
    }

    @Test fun selectConCajaGiradaRegresaALosJuegosYCambiaVista() {
        prepararCarpetaCV()
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(
            extra = Diptico.estado.value.extra.copy(cajaGirada = true)
        ))
        pulsar(KeyEvent.KEYCODE_BUTTON_SELECT)
        assertEquals("cuadricula", Diptico.estado.value.extra.vistaColeccion)
        assertEquals(false, Diptico.estado.value.extra.cajaGirada)
        assertEquals("heroe", Diptico.estado.value.inicio)
    }

    @Test fun selectDentroDeCarpetaEnUnaPantallaCambiaVista() {
        prepararCarpetaCV(unaPantalla = true)
        pulsar(KeyEvent.KEYCODE_BUTTON_SELECT)
        assertEquals("cuadricula", Diptico.estado.value.extra.vistaColeccion)
        assertEquals("heroe", Diptico.estado.value.inicio)
    }

    @Test fun l1r1SobreWidgetsNoCambianLaConsolaOcultaEnCarpeta() {
        prepararCarpetaCV()
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(inicio = "widgets"))
        assertEquals("La carpeta guardada no debe registrarse como ventana VISIBLE bajo widgets", false, Diptico.estado.value.carpetaEnVentana)
        assertEquals("La biblioteca queda en juegos pero no puede recibir el mando", false, Diptico.estado.value.enSistemas)
        val consola = Diptico.estado.value.consolaSel
        val juego = Diptico.estado.value.juegoSel
        pulsar(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals(2, Diptico.estado.value.paginaInicio)
        pulsar(KeyEvent.KEYCODE_BUTTON_L1)
        assertEquals(1, Diptico.estado.value.paginaInicio)
        assertEquals("La consola del fondo no puede moverse", consola, Diptico.estado.value.consolaSel)
        assertEquals("El juego del fondo no puede cambiar", juego, Diptico.estado.value.juegoSel)
        assertEquals("widgets", Diptico.estado.value.inicio)
    }

    @Test fun selectEnGaleriaWidgetsNoAbandonaNiModificaLaBiblioteca() {
        prepararCarpetaCV()
        val e = Diptico.estado.value
        Diptico.fijarParaPrueba(e.copy(inicio = "widgets",
            extra = e.extra.copy(galeria = true)))
        val consola = Diptico.estado.value.consolaSel
        val vista = Diptico.estado.value.extra.vistaColeccion
        pulsar(KeyEvent.KEYCODE_BUTTON_SELECT)
        assertEquals(true, Diptico.estado.value.extra.galeria)
        assertEquals("widgets", Diptico.estado.value.inicio)
        assertEquals(vista, Diptico.estado.value.extra.vistaColeccion)
        assertEquals(consola, Diptico.estado.value.consolaSel)
    }

    @Test fun l1r1EnGaleriaDeWidgetsCambianCategoriaYNoLaConsola() {
        prepararCarpetaCV()
        val antes = Diptico.estado.value
        Diptico.fijarParaPrueba(antes.copy(inicio = "widgets", extra = antes.extra.copy(galeria = true)))
        val consola = Diptico.estado.value.consolaSel
        val categoria = Diptico.estado.value.extra.tiendaCat
        pulsar(KeyEvent.KEYCODE_BUTTON_R1)
        assertNotEquals("La categoría debe avanzar", categoria, Diptico.estado.value.extra.tiendaCat)
        assertEquals("La consola debe mantenerse", consola, Diptico.estado.value.consolaSel)
        assertEquals("widgets", Diptico.estado.value.inicio)
    }

    @Test fun crucetaEnWidgetsDeThorNoMueveConsolasDelFondo() {
        prepararCarpetaCV()
        val e = Diptico.estado.value
        Diptico.fijarParaPrueba(e.copy(
            enSistemas = true, inicio = "widgets", widgetFoco = 0,
        ))
        assertEquals("En dos pantallas Widgets debe tener la prioridad del foco",
            false, Diptico.estado.value.eligiendoConsola)
        val consola = Diptico.estado.value.consolaSel
        pulsar(KeyEvent.KEYCODE_DPAD_RIGHT)
        pulsar(KeyEvent.KEYCODE_DPAD_DOWN)
        assertEquals("La cruceta no debe mover consolas tras el widget enfocado",
            consola, Diptico.estado.value.consolaSel)
        assertEquals("widgets", Diptico.estado.value.inicio)
    }

    @Test fun l1r1EnWidgetsThorConCarpetasDeFondoNoCambianConsola() {
        prepararCarpetaCV()
        val e = Diptico.estado.value
        Diptico.fijarParaPrueba(e.copy(enSistemas = true, inicio = "widgets"))
        val consola = Diptico.estado.value.consolaSel
        pulsar(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals(2, Diptico.estado.value.paginaInicio)
        assertEquals(consola, Diptico.estado.value.consolaSel)
    }

    @Test fun conFocoWidgetL1R1TampocoMuevenLaConsola() {
        prepararCarpetaCV()
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(inicio = "widgets", widgetFoco = 0))
        val consola = Diptico.estado.value.consolaSel
        pulsar(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals(consola, Diptico.estado.value.consolaSel)
    }

    @Test fun mantenerSelectEnColeccionVivaIntercambiaSoloUnaVez() {
        prepararCarpetaCV()
        val anterior = Diptico.estado.value.intercambiar
        val vista = Diptico.estado.value.extra.vistaColeccion
        Teclas.manejar(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 0))
        Teclas.manejar(KeyEvent(0L, 620L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 12))
        Teclas.manejar(KeyEvent(0L, 700L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT, 0))
        assertNotEquals("Select largo intercambia pantallas una vez", anterior, Diptico.estado.value.intercambiar)
        assertEquals("Select largo no debe cambiar la vista de juegos", vista, Diptico.estado.value.extra.vistaColeccion)
        assertEquals("heroe", Diptico.estado.value.inicio)
    }

    @Test fun selectLargoSeguidoDeSelectCortoIntercambiaYCambiaVista() {
        prepararCarpetaCV()
        val antes = Diptico.estado.value.intercambiar
        Teclas.manejar(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 0))
        Teclas.manejar(KeyEvent(0L, 650L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 12))
        Teclas.manejar(KeyEvent(0L, 700L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT, 0))
        assertNotEquals(antes, Diptico.estado.value.intercambiar)
        assertEquals("estante", Diptico.estado.value.extra.vistaColeccion)
        pulsar(KeyEvent.KEYCODE_BUTTON_SELECT)
        assertEquals("cuadricula", Diptico.estado.value.extra.vistaColeccion)
    }

    @Test fun dosSelectLargosIntercambianPantallasDosVeces() {
        prepararCarpetaCV()
        val antes = Diptico.estado.value.intercambiar
        for (k in 0..1) {
            Teclas.manejar(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 0))
            Teclas.manejar(KeyEvent(0L, 650L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 12))
            Teclas.manejar(KeyEvent(0L, 700L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT, 0))
        }
        assertEquals(antes, Diptico.estado.value.intercambiar)
        assertEquals("estante", Diptico.estado.value.extra.vistaColeccion)
    }

    @Test fun mantenerSelectEnModoAnteriorNoIntercambiaDosVeces() {
        prepararCarpetaCV()
        val e = Diptico.estado.value
        Diptico.fijarParaPrueba(e.copy(modoCarpeta = "completa", extra = e.extra.copy(coleccionViva = false)))
        val anterior = Diptico.estado.value.intercambiar
        Teclas.manejar(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 0))
        Teclas.manejar(KeyEvent(0L, 620L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 11))
        Teclas.manejar(KeyEvent(0L, 670L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 12))
        Teclas.manejar(KeyEvent(0L, 700L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT, 0))
        assertNotEquals("Select largo no puede deshacer el intercambio con otro repeat", anterior,
            Diptico.estado.value.intercambiar)
    }

    @Test fun aEnWidgetsSinFocoNoEjecutaElJuegoOculto() {
        prepararCarpetaCV()
        val e = Diptico.estado.value
        Diptico.fijarParaPrueba(e.copy(inicio = "widgets", widgetFoco = -1,
            widgets = listOf(Widget("reloj", 0, 0, 2, 2))))
        val consola = Diptico.estado.value.consolaSel
        val juego = Diptico.estado.value.juegoSel
        pulsar(KeyEvent.KEYCODE_BUTTON_A)
        assertEquals("El Home no debe navegar al pulsar A en Widgets", consola, Diptico.estado.value.consolaSel)
        assertEquals(juego, Diptico.estado.value.juegoSel)
        assertEquals("widgets", Diptico.estado.value.inicio)
        assertEquals("Un A debe enfocar el widget", 0, Diptico.estado.value.widgetFoco)
        assertEquals(null, Diptico.estado.value.sesion)
    }

    @Test fun r3EnWidgetsAbreSuGaleriaSinCambiarVistaDeJuegos() {
        prepararCarpetaCV()
        val e = Diptico.estado.value
        Diptico.fijarParaPrueba(e.copy(inicio = "widgets", widgetFoco = -1))
        val vista = Diptico.estado.value.extra.vistaColeccion
        pulsar(KeyEvent.KEYCODE_BUTTON_THUMBR)
        assertEquals("R3 de Widgets abre su galería", true, Diptico.estado.value.extra.galeria)
        assertEquals("La vista de juegos queda intacta", vista, Diptico.estado.value.extra.vistaColeccion)
        assertEquals("widgets", Diptico.estado.value.inicio)
    }

    @Test fun unMenuAbiertoNuncaDejaCambiarLaConsolaDelFondo() {
        prepararCarpetaCV()
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(
            inicio = "widgets", panel = Panel.MENU,
        ))
        val consola = Diptico.estado.value.consolaSel
        pulsar(KeyEvent.KEYCODE_BUTTON_R1)
        pulsar(KeyEvent.KEYCODE_BUTTON_L1)
        assertEquals(consola, Diptico.estado.value.consolaSel)
    }

    @Test fun l1r1EnWidgetsCambianPaginaPeroNoConsola() {
        Diptico.fijarParaPrueba(Diptico.estado.value.copy(inicio = "widgets", paginaInicio = 1, novedades = -1))
        val consola = Diptico.estado.value.consolaSel
        pulsar(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals(2, Diptico.estado.value.paginaInicio)
        assertEquals(consola, Diptico.estado.value.consolaSel)
        pulsar(KeyEvent.KEYCODE_BUTTON_L1)
        assertEquals(1, Diptico.estado.value.paginaInicio)
        assertEquals(consola, Diptico.estado.value.consolaSel)
    }
}

package cl.nikalbazu.diptico

import android.net.Uri
import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.*
import cl.nikalbazu.diptico.ui.Estilo
import cl.nikalbazu.diptico.ui.Lienzo
import cl.nikalbazu.diptico.ui.Raiz
import cl.nikalbazu.diptico.ui.Rol
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.random.Random

/**
 * Prueba de recorrido: Díptico abierto en las dos pantallas (o en una) recibe cientos de botones, primero por caminos
 * pensados (carpetas, widgets, tienda, ajustes, estudio, ficha…) y después al azar, dibujando todo en cada paso.
 * Si algo se cae, la prueba falla y muestra los últimos botones para repetir el camino.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w1191dp-h411dp-land-xhdpi")
class RecorridoTest {
    @get:Rule val regla = createComposeRule()

    private fun juego(id: String, consola: String, titulo: String) =
        Juego("$consola/$id", consola, titulo, "$titulo.zip", Uri.parse("content://prueba/$consola/$id"), "primary:Juegos/$consola/$id")

    private val juegos = listOf(
        juego("1", "snes", "Leyenda del Bosque"), juego("2", "gba", "Cohete Rojo"), juego("3", "psx", "Bahía Neón"),
        juego("4", "nds", "Ruta Estelar"), juego("5", "ps2", "Caballero de Papel"), juego("6", "n64", "Pixel Kart"),
        juego("7", "snes", "Torre de Cristal"), juego("8", "gc", "Isla Tormenta"),
        // Una consola con muchos juegos (las vistas muestran hasta 5 junto al ícono) y consolas sin modelo 3D.
        juego("9", "snes", "Río Lunar"), juego("10", "snes", "Valle Eco"), juego("11", "snes", "Faro Norte"), juego("12", "snes", "Ciudad Musgo"),
        juego("13", "nds", "Dos Caminos"), juego("14", "rpgmaker", "Aldea Perdida"), juego("15", "megadrive", "Turbo Costa"),
    )

    private fun inicial(dos: Boolean) = Estado(
        listo = true, configurado = true, tieneCarpeta = true, juegos = juegos, juegoSel = "snes/1", consolaSel = "snes",
        modoPantallas = if (dos) "dos" else "una_negra", modoElegido = true, dosDisponibles = dos, intro = false,
        enSistemas = true, vistaConsolas = "carpetas", appsFijadas = listOf("com.android.settings"),
        datos = mapOf("snes/1" to DatosJuego(tiempo = 3600, paso = "Buscar la llave", ultimo = 1L)),
    ).let { it.copy(extra = it.extra.copy(coleccionViva = true)) }

    private val historial = ArrayDeque<String>()
    private var pasos = 0
    private val vistos = mutableSetOf<String>()

    private fun resumen(nombre: String) = println("RECORRIDO $nombre: $pasos botones sin caídas · pantallas visitadas: ${vistos.sorted().joinToString(", ")}")

    private fun tecla(code: Int, largo: Boolean = false) {
        historial.addLast(KeyEvent.keyCodeToString(code) + if (largo) " (mantener)" else "")
        if (historial.size > 40) historial.removeFirst()
        try {
            regla.runOnIdle {
                Teclas.manejar(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, code, 0))
                if (largo) for (r in 1..14) Teclas.manejar(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, code, r))
                Teclas.manejar(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, code, 0))
            }
            regla.mainClock.advanceTimeBy(260)
            regla.waitForIdle()
            pasos++
            Diptico.estado.value.let { e ->
                vistos += when {
                    e.editandoWidgets -> "editor de widgets"
                    e.panel != Panel.NINGUNO -> "panel " + e.panel.name.lowercase()
                    e.inicio == "widgets" -> "widgets"
                    e.enSistemas -> "carpetas"
                    else -> "portada de juegos"
                }
            }
        } catch (t: Throwable) {
            fail("Se cayó después de: ${historial.joinToString(" → ")}\n${t.stackTraceToString().take(2500)}")
        }
    }

    private fun abrir(dos: Boolean) {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(inicial(dos))
        Estilo.tema = Temas.incluidos[0]
        regla.mainClock.autoAdvance = false
        regla.setContent {
            val e by Diptico.estado.collectAsState()
            Lienzo(true, Modifier.fillMaxSize(), 1f) {
                if (dos) Row(Modifier.fillMaxSize()) {
                    Box(Modifier.width(731.dp).fillMaxHeight()) { Raiz(e, Rol.SUPERIOR, 0) }
                    Box(Modifier.width(460.dp).fillMaxHeight()) { Raiz(e, Rol.INFERIOR, 1) }
                } else Raiz(e, Rol.UNICA, 0)
            }
        }
        regla.mainClock.advanceTimeBy(1500)
    }

    private val A = KeyEvent.KEYCODE_BUTTON_A
    private val B = KeyEvent.KEYCODE_BUTTON_B
    private val X = KeyEvent.KEYCODE_BUTTON_X
    private val Y = KeyEvent.KEYCODE_BUTTON_Y
    private val L1 = KeyEvent.KEYCODE_BUTTON_L1
    private val R1 = KeyEvent.KEYCODE_BUTTON_R1
    private val L2 = KeyEvent.KEYCODE_BUTTON_L2
    private val R2 = KeyEvent.KEYCODE_BUTTON_R2
    private val SEL = KeyEvent.KEYCODE_BUTTON_SELECT
    private val START = KeyEvent.KEYCODE_BUTTON_START
    private val R3 = KeyEvent.KEYCODE_BUTTON_THUMBR
    private val ARR = KeyEvent.KEYCODE_DPAD_UP
    private val ABA = KeyEvent.KEYCODE_DPAD_DOWN
    private val IZQ = KeyEvent.KEYCODE_DPAD_LEFT
    private val DER = KeyEvent.KEYCODE_DPAD_RIGHT

    private fun volverAlInicio() {
        repeat(6) { if (Diptico.estado.value.panel != Panel.NINGUNO || Diptico.estado.value.editandoWidgets) tecla(B) }
    }

    /** Caminos pensados: lo que una persona hace al usar Díptico. */
    private fun caminos() {
        // Carpetas: cambiar de consola y de vista, entrar y salir.
        repeat(6) { tecla(R1) }; repeat(3) { tecla(L1) }
        repeat(4) { Diptico.elegirCuantosCarpeta(5); tecla(SEL); repeat(3) { tecla(R1) } }
        repeat(4) { tecla(DER) }; tecla(ABA); tecla(IZQ)
        tecla(A); repeat(3) { tecla(DER) }; tecla(SEL); tecla(SEL); tecla(R3); tecla(DER); tecla(R3); tecla(B)
        tecla(X); tecla(ABA); tecla(B); volverAlInicio()
        tecla(Y); volverAlInicio()
        // Widgets: ir, editar, abrir la tienda, cambiar categorías, agregar y configurar.
        Diptico.fijarInicio("widgets"); regla.mainClock.advanceTimeBy(600)
        tecla(A, largo = true)
        Diptico.editarWidgets(); regla.mainClock.advanceTimeBy(300)
        Diptico.abrirBurbuja(4, 2); regla.mainClock.advanceTimeBy(300)
        repeat(6) { tecla(R1) }
        repeat(3) { tecla(ABA) }; tecla(DER); tecla(A)
        repeat(4) { tecla(ABA) }; tecla(DER); tecla(IZQ)
        repeat(3) { tecla(L2) }; tecla(R2)
        tecla(START); volverAlInicio()
        Diptico.fijarInicio("heroe"); regla.mainClock.advanceTimeBy(600)
        // Ajustes: todas las secciones y algunas opciones.
        tecla(START)
        repeat(9) { tecla(ABA); tecla(A); tecla(ABA); tecla(DER); tecla(B) }
        volverAlInicio()
        // Portada de juegos y ficha.
        tecla(B); tecla(A); repeat(3) { tecla(DER) }; tecla(X); tecla(X); tecla(B); tecla(Y); tecla(B); volverAlInicio()
        // Intercambiar pantallas manteniendo Select.
        tecla(SEL, largo = true); tecla(SEL, largo = true)
    }

    private fun azar(semilla: Int, pasos: Int) {
        val botones = intArrayOf(A, B, X, Y, L1, R1, L2, R2, SEL, ARR, ABA, IZQ, DER, ARR, ABA, IZQ, DER, B, R3, START)
        val r = Random(semilla)
        repeat(pasos) {
            val b = botones[r.nextInt(botones.size)]
            // Sin lanzar juegos de verdad: A solo cuando no hay un juego elegido en la portada.
            tecla(b, largo = b == SEL && r.nextInt(12) == 0)
            if (it % 40 == 39) volverAlInicio()
        }
    }

    /** No basta con cambiar el estado: la pantalla debe dibujar la nueva vista. */
    private fun verificarVistas(dos: Boolean) {
        abrir(dos)
        regla.runOnIdle { Diptico.entrarConsolaCV("snes") }
        regla.mainClock.advanceTimeBy(600)
        regla.waitForIdle()
        regla.onNodeWithTag("cv-vista-estante", useUnmergedTree = true).assertExists()
        for ((tag, vista) in listOf(
            "cv-vista-cuadricula" to "cuadricula",
            "cv-vista-lista" to "lista",
            "cv-vista-estante" to "estante",
        )) {
            tecla(SEL)
            regla.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
            org.junit.Assert.assertEquals(vista, Diptico.estado.value.extra.vistaColeccion)
            org.junit.Assert.assertEquals("heroe", Diptico.estado.value.inicio)
        }
    }

    @Test fun selectCambiaVistaVisualDosPantallas() = verificarVistas(dos = true)

    @Config(qualifiers = "w731dp-h411dp-land-xhdpi")
    @Test fun selectCambiaVistaVisualUnaPantalla() = verificarVistas(dos = false)

    @Test fun dosPantallas() { abrir(dos = true); caminos(); azar(7, 400); resumen("dos pantallas") }

    @Config(qualifiers = "w731dp-h411dp-land-xhdpi")
    @Test fun unaPantalla() { abrir(dos = false); caminos(); azar(11, 400); resumen("una pantalla") }
}

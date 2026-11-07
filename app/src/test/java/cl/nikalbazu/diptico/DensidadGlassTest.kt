package cl.nikalbazu.diptico

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import cl.nikalbazu.diptico.data.Almacen
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.*
import cl.nikalbazu.diptico.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DensidadGlassTest {
    private fun iniciar(e: Estado) {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        cl.nikalbazu.diptico.data.Consolas.cargar(contexto)
        Diptico.app = contexto
        Diptico.almacen = Almacen(contexto)
        Diptico.fijarParaPrueba(e)
    }
    @Test fun elTamanoConservaVistaSeleccionYSeGuardaEnUnaParada() {
        for (vista in listOf("estante", "cuadricula", "carrusel", "abanico", "lomos", "lista")) {
            iniciar(Estado(vista = vista, juegoSel = "elegido"))
            Diptico.elegirTamJuegos(1.16f)
            assertEquals(1.2f, Diptico.estado.value.tamJuegos)
            assertEquals(vista, Diptico.estado.value.vista)
            assertEquals("elegido", Diptico.estado.value.juegoSel)
            repeat(20) { Diptico.pellizcar(true) }
            assertEquals(1.4f, Diptico.estado.value.tamJuegos)
            assertEquals(vista, Diptico.estado.value.vista)
            assertEquals("1.4", Diptico.almacen.texto("tamJuegos"))
        }
    }
    @Test fun carpetasSeAjustanALaParadaMasCercana() {
        iniciar(Estado())
        Diptico.guardarTamCarpetas(1.51f)
        assertEquals(1.6f, Diptico.estado.value.tamCarpetas)
        assertEquals("1.6", Diptico.almacen.texto("tamCarpetas"))
        listOf(Float.NaN, Float.POSITIVE_INFINITY).forEach { assertEquals(1f, Densidades.juegos.limitar(it)) }
    }
    @Test fun perfilesAnterioresYNuevosConservanLosNivelesIndependientes() {
        assertEquals(NivelesGlass(), Experiencia.leer("{}").nivelesGlass)
        val perfil = Experiencia(materialInterfaz = "original", nivelesGlass = NivelesGlass(25, 85))
        assertEquals(perfil, Experiencia.importar(perfil.json().toString()))
        iniciar(Estado(experiencia = perfil))
        val cambiar = opcionesGlass(Diptico.estado.value).first { it.titulo == "Glass de carpetas" }
        cambiar.rango!!.alCambiar(100)
        val guardado = Experiencia.leer(Diptico.almacen.texto("experiencia"))
        assertEquals(NivelesGlass(100, 85), guardado.nivelesGlass)
        assertEquals("original", guardado.materialInterfaz)
    }
    @Test fun elMaterialConservaTextoLegibleEnFondosExtremos() {
        for (oscuro in listOf(false, true)) for (t in Temas.incluidos) {
            val p = paletaDe(t, oscuro)
            for (nivel in listOf(0, 25, 55, 70, 100)) {
                val deseada = densidadCristal(CapaCristal.CARPETA, true, false, nivel = nivel)
                val alfa = opacidadLegibleCristal(p, CapaCristal.CARPETA, deseada)
                for (fondo in listOf(Color.Black, Color.White)) {
                    val material = lerp(fondo, colorMaterialCristal(p, CapaCristal.CARPETA), alfa)
                    val contraste = (maxOf(p.tinta.luminance(), material.luminance()) + .05f) / (minOf(p.tinta.luminance(), material.luminance()) + .05f)
                    assertTrue("${t.id}: $nivel ($contraste)", contraste >= 4.5f)
                }
                assertEquals(1f, densidadCristal(CapaCristal.CARPETA, false, false, nivel = nivel))
            }
        }
    }
    @Test fun ordenarLasCarpetasNoCambiaElJuegoElegidoNiLaBiblioteca() {
        val juegos = listOf("snes", "gb", "psx", "androidgames").map { id -> Juego("$id/1", id, "Juego", "j.zip", android.net.Uri.parse("content://prueba/$id"), id) }
        iniciar(Estado(juegos = juegos, consolaSel = "gb", juegoSel = "gb/1"))
        Diptico.ordenarCarpetas(true)
        val e = Diptico.estado.value
        assertEquals(listOf("androidgames", "gb", "psx", "snes"), e.ordenConsolas)
        assertEquals(juegos, e.juegos)
        assertEquals("gb/1", e.juegoSel)
        assertEquals("gb", e.consolaSel)
    }    @Test fun lasOpcionesDeBibliotecaVuelvenALosJuegos() {
        iniciar(Estado(vistaConsolas = "carpetas", enSistemas = false, panel = Panel.NINGUNO))
        Diptico.abrirGaleria("biblioteca")
        Diptico.cerrarPanel()
        assertEquals(Panel.NINGUNO, Diptico.estado.value.panel)
        assertFalse(Diptico.estado.value.enSistemas)
    }

}

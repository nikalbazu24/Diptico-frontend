package cl.nikalbazu.diptico.renacer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.test.core.app.ApplicationProvider
import cl.nikalbazu.diptico.data.Almacen
import cl.nikalbazu.diptico.data.CarpetaJuegos
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.data.Juego
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RenacerTest {
    private val a = Juego("snes/a", "snes", "Árbol", "a.sfc", Uri.parse("content://prueba/a"), "a")
    private val b = a.copy(clave = "gba/b", consolaId = "gba", titulo = "Íris")
    private val oculto = a.copy(clave = "snes/c", titulo = "Secreto")
    private val salon = Salon(juegos = listOf(a, b, oculto), cargando = false,
        datos = mapOf(a.clave to DatosJuego(favorito = true, ultimo = 12), oculto.clave to DatosJuego(oculto = true)))

    @Test fun volverDeOcultosLimpiaLaBusquedaYConservaLaConsola() {
        val filtrado = salon.abrirConsola("snes").copy(filtro = Filtro.OCULTOS, consulta = "Secreto", elegido = a.clave)
        assertEquals(listOf(oculto), filtrado.visibles)
        val regreso = filtrado.volver()
        assertEquals(Filtro.TODOS, regreso.filtro)
        assertEquals("", regreso.consulta)
        assertEquals("snes", regreso.consola)
        assertEquals(a.clave, regreso.elegido)
        assertEquals(listOf(a), regreso.visibles)
    }
    @Test fun cerrarUnaFichaConservaElLugarLaBusquedaYElFiltro() {
        val previo = salon.copy(lugar = Lugar.BIBLIOTECA, consulta = "arbol", filtro = Filtro.FAVORITOS)
        assertEquals(previo, previo.copy(ventana = Ventana.Juego(a.clave)).volver())
        assertEquals(listOf(a), previo.visibles)
    }
    @Test fun ocultosNoAparecenEnInicioNiEnCarpetas() {
        val e = salon.copy(elegido = oculto.clave)
        assertEquals(a, e.protagonista)
        assertEquals(listOf(a), e.recientes)
        assertEquals(listOf(a), e.porConsola["snes"])
    }
    @Test fun buscarNoDependeDelIdiomaDelEquipo() {
        val anterior = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals(listOf(b), salon.copy(consulta = "IRIS").visibles)
            assertEquals(listOf(a, b), salon.abrirConsola("*").visibles)
        } finally { Locale.setDefault(anterior) }
    }
    @Test fun losAcentosDelCalendarioSeLeenEnTodosLosAmbientes() {
        val acentos = Ambiente.entries.map { it.acento } + cl.nikalbazu.diptico.estado.Temas.fiestas.map { it.tema.acento }
        Ambiente.entries.forEach { ambiente ->
            val fondo = Color(ambiente.fondo)
            acentos.forEach { color ->
                val texto = acentoLegible(Color(color), fondo)
                val claros = maxOf(texto.luminance(), fondo.luminance())
                val oscuros = minOf(texto.luminance(), fondo.luminance())
                assertTrue("Acento de calendario ilegible sobre ${ambiente.nombre}", (claros + .05f) / (oscuros + .05f) >= 4.5f)
            }
        }
    }

    @Test fun quitarUnaCarpetaNoLaRecuperaDesdeLaCacheAlReiniciar() {
        val contexto: Context = ApplicationProvider.getApplicationContext()
        val almacen = Almacen(contexto)
        val raiz = Uri.parse("content://sinproveedor/tree/primary%3AJuegos")
        val juego = a.copy(uri = DocumentsContract.buildDocumentUriUsingTree(raiz, "primary:Juegos/snes/a.sfc"))
        almacen.guardarCarpetas(listOf(CarpetaJuegos(raiz, null)))
        almacen.guardarBiblioteca(listOf(juego))
        val biblioteca = BibliotecaRenacer(contexto)
        assertEquals(listOf(juego), biblioteca.cargar())
        val inaccesible = biblioteca.escanear(almacen.carpetas())
        assertEquals(listOf(juego), inaccesible.juegos)
        assertEquals(1, inaccesible.fallas.size)
        almacen.guardarCarpetas(emptyList())
        biblioteca.escanear(emptyList())
        assertTrue(BibliotecaRenacer(contexto).cargar().isEmpty())
        assertTrue(almacen.biblioteca().isEmpty())
    }
}

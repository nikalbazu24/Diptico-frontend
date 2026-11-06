package cl.nikalbazu.diptico

import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import cl.nikalbazu.diptico.estado.*
import cl.nikalbazu.diptico.retoma.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavegacionTest {
    @Test fun pellizcarNoPierdeWidgetsNiLosSuperpone() {
        val originales = (0 until 60).map { i -> Widget("w$i", i % 6, i % 4, 2, 2, visible = i != 10) }
        for (cols in 4..8) for (filas in 3..8) {
            val r = Rejilla.distribuir(originales, cols, filas, 1)
            assertEquals(originales.count { it.visible }, r.widgets.count { it.visible })
            assertEquals(originales.map { it.tipo }, r.widgets.map { it.tipo })
            val celdas = mutableSetOf<Triple<Int, Int, Int>>()
            r.widgets.filter { it.visible }.forEach { w ->
                assertTrue(w.x >= 0 && w.x + w.an <= cols && w.y >= 0 && w.y + w.al <= filas)
                assertTrue(w.pagina in 0 until r.paginas)
                for (x in w.x until w.x + w.an) for (y in w.y until w.y + w.al) assertTrue(celdas.add(Triple(w.pagina, x, y)))
            }
        }
    }
    @Test fun clipEmpiezaEnFotogramaClaveYAcotaMemoria() {
        val b = BufferRetoma(ventanaUs = 2_000_000, limiteBytes = 80)
        b.agregar(PaqueteRetoma(ByteArray(5), 0, false, 0))
        assertTrue(b.copia().isEmpty())
        for (i in 0..100) b.agregar(PaqueteRetoma(ByteArray(5), i * 100_000L, i % 10 == 0, if(i % 10 == 0) 1 else 0))
        val clip = b.copia(); assertTrue(clip.first().clave)
        assertTrue(clip.sumOf { it.datos.size } <= 80)
        assertTrue(clip.last().tiempoUs - clip.first().tiempoUs <= 3_000_000)
    }
    @Test fun clipNoConservaPaquetesQueExcedenElLimite() {
        val b = BufferRetoma(limiteBytes = 10)
        b.agregar(PaqueteRetoma(ByteArray(20), 0, true, 1))
        assertTrue(b.copia().isEmpty())
    }
    @Test fun acordeAbreCajonYNoAbreMenuAlSoltarStart() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(navegacion = NavegacionEstado(atajoApps = "start_r3"), listo = true, configurado = true, intro = false))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_THUMBR))
        assertEquals(Panel.APPS, Diptico.estado.value.panel)
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_THUMBR))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_START))
        assertEquals(Panel.APPS, Diptico.estado.value.panel)
    }
    @Test fun atajoAprendidoConStartNoActivaElBotonSolo() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        val start = KeyEvent.KEYCODE_BUTTON_START
        val extra = KeyEvent.KEYCODE_BUTTON_12
        Diptico.fijarParaPrueba(Estado(
            listo = true, configurado = true, intro = false,
            botonesExtra = listOf(BotonExtra("Extra", extra, "apps", conStart = true)),
        ))
        toque(extra, 1000L)
        assertEquals(Panel.NINGUNO, Diptico.estado.value.panel)
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_DOWN, start))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_DOWN, extra))
        assertEquals(Panel.APPS, Diptico.estado.value.panel)
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_UP, start))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_UP, extra))
        assertEquals(Panel.APPS, Diptico.estado.value.panel)
    }

    @Test fun novedadesNoRepitenIdentificadoresNiRevivenVersionesAntiguas() {
        val ids = Novedades.HISTORIAL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(listOf(Novedades.ID), Novedades.pendientes("centro-bisa-1").map { it.id })
    }

    @Test fun tiempoConConsolaCerradaNoCuentaComoTiempoJugado() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(listo = true, configurado = true, intro = false, bienvenidaModo = "no", sesion = Sesion("snes/a", 1000L, 0, false)))
        Diptico.alEncender(60_000L, true)
        assertEquals(61_000L, Diptico.estado.value.sesion!!.inicio)
    }

    private fun toque(tecla: Int, tiempo: Long) {
        Teclas.manejar(KeyEvent(tiempo, tiempo, KeyEvent.ACTION_DOWN, tecla, 0))
        Teclas.manejar(KeyEvent(tiempo, tiempo + 20, KeyEvent.ACTION_UP, tecla, 0))
    }

    @Test fun dobleStartAbreBisaSinActivarElMenu() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(listo = true, configurado = true, intro = false,
            navegacion = NavegacionEstado(atajoBisa = AtajoInicio.START_DOBLE.id)))
        toque(KeyEvent.KEYCODE_BUTTON_START, 1000L)
        assertEquals(Panel.NINGUNO, Diptico.estado.value.panel)
        toque(KeyEvent.KEYCODE_BUTTON_START, 1120L)
        assertEquals(Panel.ASISTENTE, Diptico.estado.value.panel)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
            .idleFor(java.time.Duration.ofMillis(350))
        assertEquals(Panel.ASISTENTE, Diptico.estado.value.panel)
    }

    @Test fun unaPulsacionStartSigueAbriendoElMenu() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(listo = true, configurado = true, intro = false))
        toque(KeyEvent.KEYCODE_BUTTON_START, 1000L)
        assertEquals(Panel.MENU, Diptico.estado.value.panel)
    }

    @Test fun mantenerStartAbreElCentroDeControl() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(listo = true, configurado = true, intro = false))
        val start = KeyEvent.KEYCODE_BUTTON_START
        Teclas.manejar(KeyEvent(1000L, 1000L, KeyEvent.ACTION_DOWN, start, 0))
        Teclas.manejar(KeyEvent(1000L, 1600L, KeyEvent.ACTION_DOWN, start, 1))
        Teclas.manejar(KeyEvent(1000L, 1620L, KeyEvent.ACTION_UP, start, 0))
        assertEquals(Panel.CENTRO, Diptico.estado.value.panel)
    }

    @Test fun permiteCambiarBisaAStartSelectSinUsarBotonesPrincipales() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(listo = true, configurado = true, intro = false,
            navegacion = NavegacionEstado(atajoBisa = "start_select")))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_START))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT))
        assertEquals(Panel.ASISTENTE, Diptico.estado.value.panel)
    }

}

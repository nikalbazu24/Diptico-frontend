package cl.nikalbazu.diptico

import cl.nikalbazu.diptico.estado.FilasEdicion
import cl.nikalbazu.diptico.estado.INICIAL
import cl.nikalbazu.diptico.estado.nombrePersonaje
import cl.nikalbazu.diptico.ui.Medicion
import cl.nikalbazu.diptico.ui.medir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** El medidor de fluidez y el orden del editor de perfil. */
class MedidorTest {
    @Test fun sesentaCuadrosParejosSon60fps() {
        assertEquals(Medicion(60, 16), medir(List(60) { 16.67f }))
    }

    @Test fun unTironSeVeComoPeorCuadro() {
        val m = medir(List(50) { 16.67f } + listOf(120f))
        assertEquals(120, m.peorMs)
        assertTrue(m.fps < 60)
    }

    @Test fun sinCuadrosNoHayMedicion() {
        assertEquals(Medicion(0, 0), medir(emptyList()))
    }

    @Test fun fotoTieneDiscordSoloSiHayCuenta() {
        assertEquals(listOf("personaje", "galeria", INICIAL), FilasEdicion.fuentes(false))
        assertEquals(listOf("personaje", "discord", "galeria", INICIAL), FilasEdicion.fuentes(true))
        assertEquals(4, FilasEdicion.ancho(FilasEdicion.PERSONAJES_2, false))
        assertEquals(2, FilasEdicion.ancho(FilasEdicion.BOTONES, false))
    }

    @Test fun sinKitDeDiscordNoSeVeLaPresencia() {
        assertFalse(FilasEdicion.visible(FilasEdicion.PRESENCIA, false))
        assertTrue(FilasEdicion.visible(FilasEdicion.SALUDO, false))
        assertEquals("Mi inicial", nombrePersonaje(INICIAL))
    }
}

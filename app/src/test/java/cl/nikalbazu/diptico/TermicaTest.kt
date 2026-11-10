package cl.nikalbazu.diptico

import cl.nikalbazu.diptico.estado.Termica
import cl.nikalbazu.diptico.estado.Termica.Animo
import cl.nikalbazu.diptico.estado.Termica.Lectura
import cl.nikalbazu.diptico.estado.Termica.Nivel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Los consejos de calor y batería: qué se dice y con qué ánimo, según la lectura. */
class TermicaTest {
    private fun lectura(nivel: Nivel, cargando: Boolean = false, bateria: Int = 60) =
        Lectura(bateria = 40f, procesador = 0f, estadoSistema = 0, margen = Float.NaN, cargando = cargando, nivelBateria = bateria, nivel = nivel)

    @Test fun sinLecturaNoHayConsejoUrgente() {
        assertNull(Termica.consejoUrgente(null))
    }

    @Test fun fresca_o_tibiaNoMolestan() {
        assertNull(Termica.consejoUrgente(lectura(Nivel.FRESCA)))
        assertNull(Termica.consejoUrgente(lectura(Nivel.TIBIA)))
    }

    @Test fun muyCalienteEsLoMasUrgenteYPreocupa() {
        val c = Termica.consejoUrgente(lectura(Nivel.MUY_CALIENTE, cargando = true, bateria = 10))!!
        assertEquals(Animo.PREOCUPADO, c.animo)
        assertEquals("Está muy caliente. Haz una pausa, baja el brillo y sácale la funda para que respire.", c.texto)
    }

    @Test fun calienteEnchufadoSugiereDesenchufar() {
        val enchufada = Termica.consejoUrgente(lectura(Nivel.CALIENTE, cargando = true))!!
        val sola = Termica.consejoUrgente(lectura(Nivel.CALIENTE, cargando = false))!!
        assertNotEquals(enchufada.texto, sola.texto)
        assertEquals(true, enchufada.texto.contains("Desenchúfala"))
    }

    @Test fun bateriaBajaSoloAvisaSiNoEstaCargando() {
        assertEquals(Animo.PREOCUPADO, Termica.consejoUrgente(lectura(Nivel.FRESCA, bateria = 12))!!.animo)
        assertNull(Termica.consejoUrgente(lectura(Nivel.FRESCA, cargando = true, bateria = 12)))
        assertNull(Termica.consejoUrgente(lectura(Nivel.FRESCA, bateria = 40)))
    }

    @Test fun elTurnoDeConsejosDaLaVueltaSinSalirseDeLaLista() {
        val vistos = (0..20).map { Termica.consejoDeTurno(it) }.toSet()
        assertEquals(5, vistos.size)
        // Un turno negativo tampoco debe romper.
        assertEquals(Termica.consejoDeTurno(-1), Termica.consejoDeTurno(4))
    }
}

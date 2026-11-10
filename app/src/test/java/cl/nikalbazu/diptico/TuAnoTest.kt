package cl.nikalbazu.diptico

import cl.nikalbazu.diptico.estado.TuAno
import cl.nikalbazu.diptico.estado.TuAno.Fila
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** «Tu año»: las cuentas del resumen, la racha y el aviso de diciembre. */
class TuAnoTest {
    private val filas = listOf(
        Fila("2026-01-10", "snes/1", 3600, 2),
        Fila("2026-01-11", "snes/1", 1800, 1),
        Fila("2026-01-12", "ps2/3", 7200, 1),
        Fila("2026-03-05", "ps2/3", 3600, 1),
        Fila("2025-12-31", "n64/7", 99_999, 9),
    )

    @Test fun sumaSoloLoDelAnioPedido() {
        val r = TuAno.resumenDe(filas, 2026)
        assertEquals(16_200L, r.segundos)
        assertEquals(5, r.partidas)
        assertEquals(4, r.dias)
        assertEquals(2, r.juegos)
        assertEquals("2026-01-10", r.desde)
    }

    @Test fun elTopVaDeMasAMenosYElMejorMesSeEncuentra() {
        val r = TuAno.resumenDe(filas, 2026) { if (it.startsWith("ps2")) "PlayStation 2" else "Super Nintendo" }
        assertEquals(listOf("ps2/3" to 10_800L, "snes/1" to 5_400L), r.top)
        assertEquals(1, r.mejorMes)
        assertEquals(14_400L, r.porMes[0])
        assertEquals(3_600L, r.porMes[2])
        assertEquals("PlayStation 2", r.consola)
    }

    @Test fun unAnioSinPartidasNoInventaNada() {
        val r = TuAno.resumenDe(filas, 2024)
        assertFalse(r.hayDatos)
        assertEquals(0, r.mejorMes)
        assertEquals("", r.desde)
        assertEquals("", r.consola)
        assertEquals(0, r.rachaMax)
    }

    @Test fun laRachaMasLargaCuentaDiasSeguidos() {
        assertEquals(3, TuAno.rachaMasLarga(setOf("2026-01-10", "2026-01-11", "2026-01-12", "2026-01-20")))
        assertEquals(1, TuAno.rachaMasLarga(setOf("2026-01-10", "2026-01-12")))
        // Cruza de mes y de año sin romperse.
        assertEquals(3, TuAno.rachaMasLarga(setOf("2025-12-31", "2026-01-01", "2026-01-02")))
        assertEquals(0, TuAno.rachaMasLarga(emptySet()))
    }

    @Test fun elAvisoSoloSaleEnDiciembreYUnaVezPorAnio() {
        assertTrue(TuAno.tocaAvisar(12, 2026, ""))
        assertTrue(TuAno.tocaAvisar(12, 2026, "2025"))
        assertFalse(TuAno.tocaAvisar(12, 2026, "2026"))
        assertFalse(TuAno.tocaAvisar(11, 2026, ""))
        assertFalse(TuAno.tocaAvisar(1, 2027, ""))
    }

    @Test fun elComentarioCambiaConLasHoras() {
        fun c(h: Long) = TuAno.comentario(TuAno.resumenDe(listOf(Fila("2026-02-01", "a", h * 3600, 1)), 2026))
        assertEquals(4, setOf(c(1), c(10), c(50), c(100)).size)
        assertTrue(TuAno.comentario(TuAno.resumenDe(emptyList(), 2026)).contains("Recién empecé"))
    }
}

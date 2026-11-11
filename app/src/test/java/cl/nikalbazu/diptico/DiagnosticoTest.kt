package cl.nikalbazu.diptico

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiagnosticoTest {
    private fun juego(archivo: String, consola: String) = Juego("$consola/$archivo", consola, archivo.substringBeforeLast('.'), archivo, Uri.parse("content://x/$archivo"), archivo)

    @Test fun psxPideBiosYSwitchPideLlaves() {
        Consolas.cargar(ApplicationProvider.getApplicationContext())
        val psx = Consolas.porId("psx")!!
        assertTrue(Diagnostico.causas(juego("crash.chd", "psx"), psx, "DuckStation", 400_000_000L, 1).any { "BIOS" in it })
        val sw = Consolas.porId("switch")!!
        assertTrue(Diagnostico.causas(juego("zelda.nsp", "switch"), sw, "Eden", 4_000_000_000L, 1).any { "llaves" in it })
    }

    @Test fun archivoVacioComprimidoYVariosCierres() {
        Consolas.cargar(ApplicationProvider.getApplicationContext())
        val ps2 = Consolas.porId("ps2")!!
        val c = Diagnostico.causas(juego("gow.zip", "ps2"), ps2, "NetherSX2", 10L, 3)
        assertTrue(c.first().contains("pesa casi nada"))
        assertTrue(c.any { ".zip" in it })
        assertTrue(c.size <= 5)
    }

    @Test fun nupConOtroEmuladorSugiereNovaS() {
        Consolas.cargar(ApplicationProvider.getApplicationContext())
        val s2 = Consolas.porId("switch2")!!
        assertTrue(Diagnostico.causas(juego("mkw.nup", "switch2"), s2, "Eden", 8_000_000_000L, 1).any { "NovaS" in it })
    }
}

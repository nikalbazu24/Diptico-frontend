package cl.nikalbazu.diptico

import cl.nikalbazu.diptico.estado.EmuRevisado
import cl.nikalbazu.diptico.estado.EmuladoresAlDia
import org.junit.Assert.*
import org.junit.Test

class EmuladoresAlDiaTest {
    @Test fun comparaVersiones() {
        assertTrue(EmuladoresAlDia.esMasNueva("0.8.2", "v0.9.0", 0, 0))
        assertFalse(EmuladoresAlDia.esMasNueva("0.9.0", "v0.9.0", 0, 0))
        assertFalse(EmuladoresAlDia.esMasNueva("2125.1", "2124.3", 0, 0))
        assertTrue(EmuladoresAlDia.esMasNueva("1.2", "1.2.1", 0, 0))
        // Sin números comparables manda la fecha (con 12 h de margen).
        val dia = 86_400_000L
        assertTrue(EmuladoresAlDia.esMasNueva("nightly", "nightly-abc", 10 * dia, 12 * dia))
        assertFalse(EmuladoresAlDia.esMasNueva("nightly", "nightly-abc", 10 * dia, 10 * dia + 3_600_000L))
        // Compilación por fecha contra versión normal: no se comparan por número.
        assertTrue(EmuladoresAlDia.esMasNueva("1.0", "20261105", 10 * dia, 12 * dia))
    }

    @Test fun eligeElApkDeLaThor() {
        val nombres = listOf("app-x86_64.apk", "app-armeabi-v7a.apk", "app-arm64-v8a.apk", "app-universal.apk", "notas.txt")
        assertEquals("app-arm64-v8a.apk", EmuladoresAlDia.elegirApk(nombres, "org.x.emu"))
        assertEquals("Eden-Legacy.apk", EmuladoresAlDia.elegirApk(listOf("Eden-Android.apk", "Eden-Legacy.apk"), "dev.legacy.eden_emulator"))
        assertEquals("Eden-Android.apk", EmuladoresAlDia.elegirApk(listOf("Eden-Android.apk", "Eden-Legacy.apk"), "dev.eden.eden_emulator"))
        assertNull(EmuladoresAlDia.elegirApk(listOf("source.zip"), "org.x.emu"))
    }

    @Test fun fuentesYComentario() {
        assertEquals("Eden", EmuladoresAlDia.fuenteDe("dev.eden.eden_emulator.nightly")?.nombre)
        assertNull(EmuladoresAlDia.fuenteDe("com.ejemplo.nada"))
        val l = listOf(
            EmuRevisado("Eden", "a", "0.8", 0, listOf("Switch"), 7200, ultima = "0.9", nueva = true),
            EmuRevisado("Azahar", "b", "2124", 0, ultima = "2124"),
        )
        val c = EmuladoresAlDia.comentario(l)
        assertTrue(c.contains("Eden tiene versión nueva"))
        assertTrue(c.contains("El que más usas es Eden"))
    }
}

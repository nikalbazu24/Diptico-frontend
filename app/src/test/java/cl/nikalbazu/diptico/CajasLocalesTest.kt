package cl.nikalbazu.diptico

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import cl.nikalbazu.diptico.data.Biblioteca
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.CajasLocales
import cl.nikalbazu.diptico.estado.CarasGuardadas
import cl.nikalbazu.diptico.estado.Informes
import cl.nikalbazu.diptico.estado.Respaldo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

/** Las cajas 3D se quedan en el equipo: no se pierden con una búsqueda fallida, ni sin internet, ni al restaurar. */
@RunWith(RobolectricTestRunner::class)
class CajasLocalesTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    private fun imagen(f: File): File {
        f.parentFile?.mkdirs()
        val b = Bitmap.createBitmap(64, 90, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF3366AA.toInt()) }
        f.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // Relleno para pasar el mínimo de tamaño (una cara de verdad pesa mucho más).
        if (f.length() < 1024) f.appendBytes(ByteArray(1024))
        return f
    }

    @Test fun unaBusquedaFallidaNoBorraLaCaja() {
        val antes = CarasGuardadas("file:///x/reverso.jpg", "file:///x/lomo.jpg", 10L, "file:///x/frente.jpg", origen = "internet")
        val nada = CarasGuardadas(null, null, 20L, null)
        val g = CajasLocales.combinar(antes, nada)
        assertEquals(antes.frente, g.frente)
        assertEquals(antes.reverso, g.reverso)
        assertEquals(antes.lomo, g.lomo)
        assertEquals("internet", g.origen)
        assertEquals(20L, g.buscado)
        // Lo nuevo sí completa lo que faltaba.
        val conLomo = CajasLocales.combinar(antes.copy(lomo = null), CarasGuardadas(null, "file:///y/lomo.jpg", 30L, null, origen = "tgdb"))
        assertEquals("file:///y/lomo.jpg", conLomo.lomo)
        assertEquals(antes.frente, conLomo.frente)
    }

    @Test fun losEnlacesPierdenSusClaves() {
        val url = "https://api.screenscraper.fr/api2/mediaJeu.php?devid=yo&devpassword=secreto&softname=Diptico&ssid=nico&sspassword=otra&systemeid=58&jeuid=1&media=box-2D"
        val limpio = CajasLocales.sinClaves(url)
        assertFalse("secreto" in limpio)
        assertFalse("otra" in limpio)
        assertTrue("jeuid=1" in limpio && "media=box-2D" in limpio)
    }

    @Test fun lasCarasGuardanOrigenYSeLeenIgual() {
        val m = mapOf("ps2/God of War.iso" to CarasGuardadas("r", "l", 5L, "f", origen = "propia", disco = "d"))
        val leido = CarasGuardadas.leer(CarasGuardadas.escribir(m))
        assertEquals(m, leido)
        assertTrue(leido.values.first().propia)
        // Lo guardado por versiones anteriores (sin origen) se sigue leyendo.
        val viejo = CarasGuardadas.leer("""{"snes/x.sfc":{"r":"","l":"","b":7,"f":"https://a/b.png"}}""")
        assertEquals("https://a/b.png", viejo["snes/x.sfc"]?.frente)
        assertNull(viejo["snes/x.sfc"]?.origen)
    }

    @Test fun rebasarArreglaRutasDelRespaldoYSueltaLasQueFaltan() {
        val f = imagen(File(ctx.filesDir, "cajas-guardadas/abc/frente-1.img"))
        val m = mapOf(
            "a" to CarasGuardadas(null, "file:///data/user/10/otra.app/files/cajas/xyz/no-existe.jpg", 1L, "local:cajas-guardadas/abc/frente-1.img"),
            "b" to CarasGuardadas(null, null, 99L, "local:cajas/nada/frente.jpg", origen = "propia"),
            "c" to CarasGuardadas(null, null, 3L, "https://ejemplo/f.png"),
        )
        val r = CajasLocales.rebasar(ctx, m)
        assertEquals(Uri.fromFile(f).toString(), r["a"]?.frente)
        assertNull(r["a"]?.lomo)
        // Sin ningún archivo, la caja vuelve a buscarse sola (ya no se considera propia ni recién buscada).
        assertFalse(r["b"]!!.tiene)
        assertNull(r["b"]?.origen)
        assertEquals(0L, r["b"]?.buscado)
        // Los enlaces quedan para bajarlos cuando haya internet.
        assertEquals("https://ejemplo/f.png", r["c"]?.frente)
    }

    @Test fun borrarSinUsoSoloTocaArchivosDeCajas() {
        val usado = imagen(File(ctx.filesDir, "cajas/juego-1/frente.jpg"))
        val viejo = imagen(File(ctx.filesDir, "cajas/juego-1/reverso.jpg"))
        val ajeno = imagen(File(ctx.filesDir, "medios/portada.png"))
        val antes = CarasGuardadas(Uri.fromFile(viejo).toString(), null, 1L, Uri.fromFile(usado).toString(), disco = Uri.fromFile(ajeno).toString())
        val siguen = listOf(CarasGuardadas(null, null, 2L, Uri.fromFile(usado).toString()))
        CajasLocales.borrarSinUso(ctx, antes, siguen)
        assertTrue(usado.exists())
        assertFalse(viejo.exists())
        assertTrue(ajeno.exists())
    }

    @Test fun elRespaldoLlevaLasCajasSinClaves() {
        val frente = imagen(File(ctx.filesDir, "cajas-guardadas/j1/frente-aa.img"))
        val propia = imagen(File(ctx.filesDir, "cajas/ps2-gow-1/frente.jpg"))
        val caras = mapOf(
            "ps2/gow.iso" to CarasGuardadas(null, null, 2L, Uri.fromFile(propia).toString(), origen = "propia"),
            "snes/x.sfc" to CarasGuardadas(
                "https://api.screenscraper.fr/x?devpassword=secreto&media=box-2D-back", null, 1L, Uri.fromFile(frente).toString(),
            ),
        )
        ctx.getSharedPreferences("diptico", Context.MODE_PRIVATE).edit()
            .putString("carasCaja2", CarasGuardadas.escribir(caras)).putString("tema", "noche").commit()
        val zip = ByteArrayOutputStream().also { Respaldo.escribir(ctx, it) }.toByteArray()
        val nombres = ArrayList<String>()
        var ajustes = ""
        ZipInputStream(ByteArrayInputStream(zip)).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                nombres += e.name
                if (e.name == "ajustes.json") ajustes = z.readBytes().decodeToString()
            }
        }
        assertTrue(nombres.contains("extras/cajas-guardadas/j1/frente-aa.img"))
        assertTrue(nombres.contains("extras/cajas/ps2-gow-1/frente.jpg"))
        assertFalse("secreto" in ajustes)
        assertFalse("screenscraper" in ajustes)
        val lista = org.json.JSONObject(ajustes).getJSONObject("carasCaja2").getString("v")
        assertTrue("local:cajas/ps2-gow-1/frente.jpg" in lista)

        // Restaurar en un equipo «limpio»: vuelven los archivos y las rutas apuntan a este equipo.
        File(ctx.filesDir, "cajas").deleteRecursively()
        File(ctx.filesDir, "cajas-guardadas").deleteRecursively()
        assertTrue(Respaldo.restaurar(ctx, ByteArrayInputStream(zip)) > 0)
        assertTrue(File(ctx.filesDir, "cajas/ps2-gow-1/frente.jpg").isFile)
        val leidas = CajasLocales.rebasar(ctx, CarasGuardadas.leer(
            ctx.getSharedPreferences("diptico", Context.MODE_PRIVATE).getString("carasCaja2", "")!!))
        assertEquals(Uri.fromFile(File(ctx.filesDir, "cajas/ps2-gow-1/frente.jpg")).toString(), leidas["ps2/gow.iso"]?.frente)
        assertTrue(leidas["ps2/gow.iso"]!!.propia)
        assertNull(leidas["snes/x.sfc"]?.reverso)
    }

    @Test fun carpetasDePs2ConOtrosNombres() {
        Consolas.cargar(ctx)
        assertEquals("ps2", Consolas.porCarpeta("PS2")?.id)
        assertEquals("ps2", Consolas.porCarpeta("PlayStation 2")?.id)
        assertEquals("ps2", Consolas.porCarpeta("Sony - PlayStation 2")?.id)
        assertEquals("ps2", Consolas.porCarpeta("PS2_ISO")?.id)
        assertEquals("psx", Consolas.porCarpeta("psx")?.id)
        assertNull(Consolas.porCarpeta("Descargas"))
    }

    @Test fun elInformeExplicaLoQueDejoFuera() {
        Consolas.cargar(ctx)
        val inf = Biblioteca.Informe().apply {
            porConsola["ps2"] = 12
            enSubcarpetas = 5
            sinConsola += "Juegos varios"
            omitir("Juego.rar", "el formato .rar no es de PlayStation 2")
        }
        val t = Informes.resumen(12, 5, 7, 0, inf, 0, 1)
        assertTrue("5 nuevos" in t)
        assertTrue("PlayStation 2 12" in t)
        assertTrue("Juegos varios" in t)
        assertTrue("Dejé fuera 1 archivo" in t)
        assertEquals(listOf("Juego.rar: el formato .rar no es de PlayStation 2"), Informes.lineas(inf))
    }
}

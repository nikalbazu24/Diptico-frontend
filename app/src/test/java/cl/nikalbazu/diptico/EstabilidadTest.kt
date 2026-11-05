package cl.nikalbazu.diptico

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import cl.nikalbazu.diptico.data.Almacen
import cl.nikalbazu.diptico.data.Biblioteca
import cl.nikalbazu.diptico.data.IndicesJuego
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.Respaldo
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EstabilidadTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val preferencias get() = ctx.getSharedPreferences("diptico", Context.MODE_PRIVATE)

    private fun zip(vararg archivos: Pair<String, String>): ByteArray = ByteArrayOutputStream().apply {
        ZipOutputStream(this).use { salida ->
            archivos.forEach { (nombre, texto) ->
                salida.putNextEntry(ZipEntry(nombre))
                salida.write(texto.toByteArray())
                salida.closeEntry()
            }
        }
    }.toByteArray()

    @Test fun clavesAntiguasSeConservanEnElEquipoYNoSalenEnElZip() {
        preferencias.edit().putString("claveGemini", "credencial-de-prueba")
            .putString("ras_igdbSecreto", "otro-secreto").putString("inicio", "widgets")
            .putString("carpeta", "content://documentos/local").commit()
        val almacen = Almacen(ctx)
        assertEquals("credencial-de-prueba", almacen.texto("claveGemini"))
        assertFalse(preferencias.contains("claveGemini"))
        assertFalse(preferencias.contains("ras_igdbSecreto"))
        val salida = ByteArrayOutputStream()
        Respaldo.escribir(ctx, salida)
        ZipInputStream(salida.toByteArray().inputStream()).use { entrada ->
            assertEquals("ajustes.json", entrada.nextEntry.name)
            val json = org.json.JSONObject(entrada.readBytes().decodeToString())
            assertEquals("widgets", json.getJSONObject("inicio").getString("v"))
            assertFalse(json.has("carpeta"))
            assertFalse(json.has("claveGemini"))
        }
    }

    @Test fun respaldoInvalidoNoModificaNotasNiPreferencias() {
        preferencias.edit().putString("inicio", "widgets").commit()
        val nota = File(ctx.filesDir, "notas/partida.m4a").apply { parentFile!!.mkdirs(); writeText("original") }
        val archivo = zip("notas/partida.m4a" to "nueva", "ajustes.json" to """{"paginas":{"t":"s","v":"0"}}""")
        assertEquals(-1, Respaldo.restaurar(ctx, archivo.inputStream()))
        assertEquals("original", nota.readText())
        assertEquals("widgets", preferencias.getString("inicio", null))
        val rutaInvalida = zip("notas/../partida.m4a" to "nueva", "ajustes.json" to """{"inicio":{"t":"s","v":"heroe"}}""")
        assertEquals(-1, Respaldo.restaurar(ctx, rutaInvalida.inputStream()))
    }

    @Test fun restaurarCopiaAntiguaNoPisaCredencialesNiPermisosLocales() {
        val almacen = Almacen(ctx)
        almacen.guardarTexto("claveOpenAI", "actual")
        preferencias.edit().putString("inicio", "widgets").putString("carpeta", "content://local").commit()
        val archivo = zip("ajustes.json" to """{"inicio":{"t":"s","v":"heroe"},"claveOpenAI":{"t":"s","v":"vieja"},"carpeta":{"t":"s","v":"content://ajena"}}""",
            "notas/partida.m4a" to "nota")
        assertEquals(1, Respaldo.restaurar(ctx, archivo.inputStream()))
        assertEquals("actual", almacen.texto("claveOpenAI"))
        assertEquals("content://local", preferencias.getString("carpeta", null))
        assertEquals("heroe", preferencias.getString("inicio", null))
        assertEquals("nota", File(ctx.filesDir, "notas/partida.m4a").readText())
        assertTrue(File(ctx.filesDir, "respaldo-antes-de-restaurar.zip").length() > 0)
    }

    @Test fun indicesDistinguenPistasDeArchivosSueltosYAcotanLaLectura() {
        assertEquals(listOf("pista 01.bin", "pista02.bin"), IndicesJuego.referencias(
            """FILE "pista 01.bin" BINARY
               FILE pista02.bin BINARY
               FILE "../otro.bin" BINARY""", "cue"))
        assertEquals(listOf("disco1.cue", "disco2.chd"), IndicesJuego.referencias(
            "# Comentario\n./disco1.cue\n/disco0.cue\ndisco2.chd\nC:\\disco.bin", "m3u"))
        assertNull(IndicesJuego.leer(ByteArray(65_537).inputStream()))
    }

    @Test fun bibliotecaGrandeConservaTitulosYDiscosDistintos() {
        val juegos = List(5_000) { i ->
            Juego("psx/$i", "psx", "Juego $i", "Juego $i.chd", Uri.parse("content://rom/$i"), "$i")
        }
        val disco1 = juegos.first().copy(archivo = "Juego 0 (Disc 1).chd")
        val disco2 = disco1.copy(archivo = "Juego 0 (Disc 2).chd")
        assertEquals(5_002, Biblioteca.sinDuplicados(juegos + disco1 + disco2).size)
    }

    @Test fun importacionSoloProponeCoincidenciasInequivocas() {
        val j = Juego("psx/prueba", "psx", "Sin nombre", "prueba.chd", Uri.parse("content://rom/prueba"), "prueba")
        val xml = """<gameList>
            <game><path>./prueba.chd</path><name>Mi juego</name><favorite>true</favorite></game>
            <game><path>./otro.chd</path><name>Otro</name></game>
        </gameList>"""
        val e = cl.nikalbazu.diptico.data.ImportarEsDe.leer(xml.byteInputStream())
        val previa = cl.nikalbazu.diptico.data.ImportarEsDe.comparar(e, listOf(j), emptyMap())
        assertEquals("Mi juego", previa.cambios.single().titulo)
        assertTrue(previa.cambios.single().favorito)
        assertEquals(1, previa.sinCoincidencia)
        val ambiguo = cl.nikalbazu.diptico.data.ImportarEsDe.comparar(e, listOf(j, j.copy(clave = "saturn/prueba")), emptyMap())
        assertTrue(ambiguo.cambios.isEmpty())
        assertEquals(2, ambiguo.sinCoincidencia)
    }

    @Test fun personalizacionDePartidaYColaSobreviveAlReinicio() {
        val d = cl.nikalbazu.diptico.data.DatosJuego(vistaPartida = "BITACORA", avance = "PENDIENTE", puestoCola = -1)
        Almacen(ctx).guardarDatos(mapOf("psx/prueba" to d))
        assertEquals(d, Almacen(ctx).datos().getValue("psx/prueba"))
    }

    @Test fun bibliotecaNoPierdeVariantesAlGuardarLaCache() {
        val usa = Juego("psx/game", "psx", "Game", "Game (USA).chd", Uri.parse("content://rom/usa"), "usa")
        val eu = usa.copy(clave = "psx/game-eu", archivo = "Game (Europe).chd", uri = Uri.parse("content://rom/eu"), docId = "eu")
        val reunidos = Biblioteca.sinDuplicados(listOf(eu, usa))
        assertEquals(usa.archivo, reunidos.single().archivo)
        assertEquals(eu.archivo, reunidos.single().variantes.single().archivo)
        val almacen = Almacen(ctx)
        almacen.guardarBiblioteca(reunidos)
        assertEquals(reunidos, almacen.biblioteca())
    }

    @Test fun perfilConservaWidgetsTransparentesYModoPartida() {
        val perfil = cl.nikalbazu.diptico.estado.Experiencia(
            vistaPartida = "BITACORA",
            posiciones = listOf(cl.nikalbazu.diptico.estado.PosicionVisual("juego", 0, 0, 4, 2, 0, true,
                estilo = "sinfondo", opacidad = 0f, escala = 1.3f)),
        )
        assertEquals(perfil, cl.nikalbazu.diptico.estado.Experiencia.importar(perfil.json().toString()))
    }
}

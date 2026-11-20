package cl.nikalbazu.diptico

import cl.nikalbazu.diptico.estado.Actualizador
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActualizadorCanalesTest {
    private fun release(tag: String, archivo: String, numero: Int, borrador: Boolean = false): JSONObject =
        JSONObject().put("tag_name", tag).put("draft", borrador)
            .put("body", "Cambios en ${tag}")
            .put("assets", JSONArray().put(
                JSONObject().put("name", archivo).put("state", "uploaded")
                    .put("browser_download_url",
                        "https://github.com/nikalbazu24/Diptico-frontend/releases/download/${tag}/${archivo}")
                    .put("size", numero * 1024L),
            ))

    @Test fun pruebasYOficialSoloDetectanSuPropioCanal() {
        val publicaciones = JSONArray()
            .put(release("v2.0.1-beta.920", "Diptico-v2.0.1-beta.920.apk", 920))
            .put(release("v2.0.0-pruebas.930", "Diptico-Pruebas-Completa-v2.0.0-pruebas.930.apk", 930))
            .put(release("v2.0.0-pruebas.931", "Diptico-Pruebas-Movil-v2.0.0-pruebas.931.apk", 931))
        val pruebas = Actualizador.seleccionarPublicacion(
            publicaciones, "cl.nikalbazu.diptico.pruebas", false, 801)
        val oficial = Actualizador.seleccionarPublicacion(
            publicaciones, "cl.nikalbazu.diptico", false, 801)
        val movil = Actualizador.seleccionarPublicacion(
            publicaciones, "cl.nikalbazu.diptico.pruebas", true, 801)
        assertEquals(930, pruebas?.numero)
        assertEquals("2.0.0-pruebas.930", pruebas?.nombre)
        assertEquals(920, oficial?.numero)
        assertEquals(931, movil?.numero)
        assertTrue(pruebas!!.url.contains("Pruebas-Completa"))
        assertTrue(movil!!.url.contains("Pruebas-Movil"))
    }

    @Test fun rechazaVersionViejaBorradorAssetErroneoYOtrosPaquetes() {
        val publicaciones = JSONArray()
            .put(release("v2.0.0-pruebas.803", "Diptico-Pruebas-Completa-v2.0.0-pruebas.803.apk", 803, borrador = true))
            .put(release("v2.0.0-pruebas.802", "Diptico-v2.0.0-pruebas.802.apk", 802))
            .put(release("v2.0.0-pruebas.801", "Diptico-Pruebas-Completa-v2.0.0-pruebas.801.apk", 801))
        assertNull(Actualizador.seleccionarPublicacion(
            publicaciones, "cl.nikalbazu.diptico.pruebas", false, 801))
        assertNull(Actualizador.seleccionarPublicacion(
            publicaciones, "cl.otra.aplicacion", false, 1))
        assertNull(Actualizador.seleccionarPublicacion(
            publicaciones, "cl.nikalbazu.diptico", false, 700))
    }

    @Test fun noOfreceVersionInstaladaYSigueAceptandoBetaOficial() {
        val publicaciones = JSONArray()
            .put(release("v2.0.0-pruebas.940", "Diptico-Pruebas-Completa-v2.0.0-pruebas.940.apk", 940))
            .put(release("v1.1-beta.941", "Diptico-v2.0.0.apk", 941))
        assertNull(Actualizador.seleccionarPublicacion(
            publicaciones, "cl.nikalbazu.diptico.pruebas", false, 940))
        assertEquals(941, Actualizador.seleccionarPublicacion(
            publicaciones, "cl.nikalbazu.diptico", false, 900)?.numero)
    }
}

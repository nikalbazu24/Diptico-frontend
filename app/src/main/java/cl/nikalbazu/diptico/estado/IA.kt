package cl.nikalbazu.diptico.estado

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Llamadas a Claude con la clave de la persona (temas y nombres de juegos). */
object IA {
    const val MODELO = "claude-haiku-5-5"

    /** Envía un pedido y devuelve el texto de la respuesta. Lanza una excepción con un mensaje en español si falla. */
    /** Modelos a probar, en orden (si uno no está disponible para la clave, se usa el siguiente). */
    private val MODELOS = listOf(MODELO, "claude-sonnet-5-5", "claude-haiku-4-5", "claude-sonnet-4-5")

    fun pedir(clave: String, prompt: String, maxTokens: Int = 800): String {
        var ultimo: Exception? = null
        for (m in MODELOS) {
            try {
                return pedirCon(m, clave, prompt, maxTokens)
            } catch (e: ModeloNoDisponible) {
                ultimo = e
            }
        }
        throw IllegalStateException(ultimo?.message ?: "No pude hablar con la IA.")
    }

    private class ModeloNoDisponible(m: String) : Exception(m)

    /** Explica en español el error que devolvió la API. */
    private fun explicar(codigo: Int, cuerpo: String): String {
        val detalle = try { JSONObject(cuerpo).getJSONObject("error").getString("message") } catch (e: Exception) { cuerpo.take(140) }
        return when {
            codigo == 401 -> "La clave de IA no es válida. Revísala en Menú › Temas › Clave de IA."
            codigo == 403 -> "La clave no tiene permiso para usar la IA ($detalle)."
            codigo == 429 -> "La IA está ocupada o llegaste al límite. Prueba en un rato."
            detalle.contains("credit", true) || detalle.contains("balance", true) -> "Tu cuenta de Anthropic no tiene saldo. Agrega créditos en console.anthropic.com › Billing."
            codigo == 529 || codigo >= 500 -> "Los servidores de la IA están saturados. Prueba en un rato."
            else -> "La IA respondió con un error ($codigo): $detalle"
        }
    }

    private fun pedirCon(modelo: String, clave: String, prompt: String, maxTokens: Int): String {
        val cuerpo = JSONObject()
            .put("model", modelo)
            .put("max_tokens", maxTokens)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        val c = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 15_000
        c.readTimeout = 60_000
        c.doOutput = true
        c.setRequestProperty("content-type", "application/json")
        c.setRequestProperty("x-api-key", clave.trim())
        c.setRequestProperty("anthropic-version", "2023-06-01")
        try {
            c.outputStream.use { it.write(cuerpo.toString().toByteArray()) }
            val codigo = c.responseCode
            if (codigo !in 200..299) {
                val error = try { c.errorStream?.use { it.bufferedReader().readText() } ?: "" } catch (e: Exception) { "" }
                if (codigo == 404 || (codigo == 400 && error.contains("model", true))) throw ModeloNoDisponible(explicar(codigo, error))
                throw IllegalStateException(explicar(codigo, error))
            }
            val respuesta = c.inputStream.use { it.bufferedReader().readText() }
            return JSONObject(respuesta).getJSONArray("content").getJSONObject(0).getString("text")
        } finally {
            c.disconnect()
        }
    }

    /** Saca el primer objeto JSON de un texto. */
    fun json(texto: String): JSONObject = JSONObject(texto.substring(texto.indexOf('{'), texto.lastIndexOf('}') + 1))

    /**
     * Reconoce los juegos a partir del nombre de sus archivos.
     * [archivos]: (consola, nombre de archivo). Devuelve nombre de archivo -> nombre oficial.
     */
    fun nombres(clave: String, archivos: List<Pair<String, String>>): Map<String, String> {
        val lista = archivos.joinToString("\n") { (c, a) -> "$c | $a" }
        val prompt = """
            Eres un experto en videojuegos. Cada línea es "consola | nombre de archivo" de un juego.
            Para cada una, di el nombre oficial y completo del juego tal como se conoce en Latinoamérica
            (normalmente el título en inglés), sin región, versión, idioma, códigos ni extensión.
            Si es un código de juego (por ejemplo de Switch o PS3) reconócelo si puedes. Si no lo reconoces, limpia el nombre lo mejor posible.
            Responde SOLO con un objeto JSON: {"nombre de archivo exacto": "Nombre oficial", ...}

            $lista
        """.trimIndent()
        val o = json(pedir(clave, prompt, 4000))
        return o.keys().asSequence().associateWith { o.getString(it).trim() }.filterValues { it.isNotEmpty() && it.length < 120 }
    }
}

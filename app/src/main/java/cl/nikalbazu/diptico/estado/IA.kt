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

    fun pedir(clave: String, prompt: String, maxTokens: Int = 800): String =
        conversar(clave, null, listOf("user" to prompt), maxTokens)

    /** Para el asistente: primero los modelos más capaces. */
    val MODELOS_LISTOS = listOf("claude-sonnet-5-5", "claude-opus-5-5", MODELO, "claude-sonnet-4-5", "claude-haiku-4-5")

    /**
     * Conversación con instrucciones de sistema e historial ("user"/"assistant", texto).
     * Con [buscarWeb], la IA puede buscar guías en internet (si la cuenta no lo permite, sigue sin buscar).
     */
    fun conversar(
        clave: String, sistema: String?, mensajes: List<Pair<String, String>>, maxTokens: Int = 800,
        modelos: List<String> = MODELOS, buscarWeb: Boolean = false,
    ): String {
        var ultimo: Exception? = null
        var web = buscarWeb
        for (m in modelos) {
            try {
                return pedirCon(m, clave, sistema, mensajes, maxTokens, web)
            } catch (e: SinBusqueda) {
                web = false
                try { return pedirCon(m, clave, sistema, mensajes, maxTokens, false) } catch (e2: ModeloNoDisponible) { ultimo = e2 }
            } catch (e: ModeloNoDisponible) {
                ultimo = e
            }
        }
        throw IllegalStateException(ultimo?.message ?: "No pude hablar con la IA.")
    }

    private class ModeloNoDisponible(m: String) : Exception(m)
    private class SinBusqueda : Exception()

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

    private fun pedirCon(modelo: String, clave: String, sistema: String?, mensajes: List<Pair<String, String>>, maxTokens: Int, web: Boolean = false): String {
        val lista = JSONArray()
        for ((rol, texto) in mensajes) lista.put(JSONObject().put("role", rol).put("content", texto))
        val cuerpo = JSONObject()
            .put("model", modelo)
            .put("max_tokens", maxTokens)
            .put("messages", lista)
        if (!sistema.isNullOrBlank()) cuerpo.put("system", sistema)
        if (web) cuerpo.put("tools", JSONArray().put(JSONObject().put("type", "web_search_20250305").put("name", "web_search").put("max_uses", 3)))
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
                if (web && codigo in 400..403 && (error.contains("web_search", true) || error.contains("tool", true))) throw SinBusqueda()
                if (codigo == 404 || (codigo == 400 && error.contains("model", true))) throw ModeloNoDisponible(explicar(codigo, error))
                throw IllegalStateException(explicar(codigo, error))
            }
            val respuesta = c.inputStream.use { it.bufferedReader().readText() }
            // Con búsqueda web la respuesta trae varios bloques: se juntan los de texto.
            val contenido = JSONObject(respuesta).getJSONArray("content")
            val texto = StringBuilder()
            for (i in 0 until contenido.length()) {
                val b = contenido.getJSONObject(i)
                if (b.optString("type") == "text") texto.append(b.optString("text"))
            }
            return texto.toString().trim()
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

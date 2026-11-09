package cl.nikalbazu.diptico.estado

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Una IA en la nube y la clave de la persona para usarla. [proveedor]: "gemini" u "openai". */
data class Llave(val proveedor: String, val clave: String)

/**
 * IA en la nube con la clave de cada persona: Gemini (Google, tiene nivel gratis) o ChatGPT (OpenAI).
 * Se usa para DIPPO, los temas con IA y reconocer los nombres de los juegos.
 */
object IA {
    /** Proveedores en la nube, en el orden en que se ofrecen. */
    val PROVEEDORES = listOf("gemini", "openai")

    fun nombre(p: String) = when (p) {
        "gemini" -> "Gemini"; "openai" -> "ChatGPT"; else -> p
    }

    fun empresa(p: String) = when (p) {
        "gemini" -> "Google"; "openai" -> "OpenAI"; else -> ""
    }

    /** Dónde se saca la clave de cada uno. */
    fun dondeClave(p: String) = when (p) {
        "gemini" -> "aistudio.google.com/apikey"
        else -> "platform.openai.com/api-keys"
    }

    // Modelos a probar en orden: si uno no existe para esa clave, se usa el siguiente.
    private val GEMINI = listOf("gemini-flash-latest", "gemini-3.8-flash", "gemini-3-flash", "gemini-2.5-flash", "gemini-flash-lite-latest")
    private val OPENAI = listOf("gpt-5.4-mini", "gpt-5-mini", "gpt-4.1-mini", "gpt-4o-mini")

    fun pedir(llave: Llave, prompt: String, maxTokens: Int = 800): String =
        conversar(llave, null, listOf("user" to prompt), maxTokens)

    /**
     * Conversación con instrucciones de sistema e historial ("user"/"assistant", texto).
     * [listos]: usar los modelos más capaces (para DIPPO). [buscarWeb]: dejar que la IA busque guías en internet
     * (si la cuenta no lo permite, sigue sin buscar).
     */
    fun conversar(
        llave: Llave, sistema: String?, mensajes: List<Pair<String, String>>, maxTokens: Int = 800,
        listos: Boolean = false, buscarWeb: Boolean = false,
    ): String {
        val modelos = if (llave.proveedor == "gemini") GEMINI else OPENAI
        var ultimo: Exception? = null
        var web = buscarWeb
        for (m in modelos) {
            try {
                return pedirCon(llave, m, sistema, mensajes, maxTokens, web)
            } catch (e: SinBusqueda) {
                web = false
                try { return pedirCon(llave, m, sistema, mensajes, maxTokens, false) } catch (e2: ModeloNoDisponible) { ultimo = e2 }
            } catch (e: ModeloNoDisponible) {
                ultimo = e
            }
        }
        throw IllegalStateException(ultimo?.message ?: "No pude hablar con la IA.")
    }

    private class ModeloNoDisponible(m: String) : Exception(m)
    private class SinBusqueda : Exception()

    /** Explica en español el error que devolvió la API. */
    private fun explicar(p: String, codigo: Int, cuerpo: String): String {
        val detalle = try {
            JSONObject(cuerpo).let { o -> o.optJSONObject("error")?.optString("message") ?: o.optString("message") }
        } catch (e: Exception) { cuerpo.take(140) }
        val n = nombre(p)
        return when {
            codigo == 401 || (codigo == 400 && detalle.contains("API key", true)) -> "La clave de $n no es válida. Revísala en Menú › DIPPO."
            codigo == 403 -> "La clave de $n no tiene permiso para esto ($detalle)."
            codigo == 429 && p == "gemini" -> "Llegaste al límite gratis de Gemini por ahora. Prueba en un rato."
            codigo == 429 && (detalle.contains("quota", true) || detalle.contains("billing", true)) -> "Tu cuenta de $n no tiene saldo. Agrega créditos en ${dondeClave(p)}."
            codigo == 429 -> "$n está ocupada o llegaste al límite. Prueba en un rato."
            detalle.contains("credit", true) || detalle.contains("balance", true) -> "Tu cuenta de $n no tiene saldo. Agrega créditos en ${dondeClave(p)}."
            codigo == 529 || codigo >= 500 -> "Los servidores de $n están saturados. Prueba en un rato."
            else -> "$n respondió con un error ($codigo): $detalle"
        }
    }

    private fun pedirCon(llave: Llave, modelo: String, sistema: String?, mensajes: List<Pair<String, String>>, maxTokens: Int, web: Boolean): String =
        when (llave.proveedor) {
            "gemini" -> gemini(llave.clave, modelo, sistema, mensajes, maxTokens, web)
            else -> openai(llave.clave, modelo, sistema, mensajes, maxTokens, web)
        }

    /** Hace el pedido HTTP y devuelve el cuerpo de la respuesta, o lanza el error ya explicado. */
    private fun enviar(p: String, url: String, cabeceras: Map<String, String>, cuerpo: JSONObject, web: Boolean): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 15_000
        c.readTimeout = 90_000
        c.doOutput = true
        c.setRequestProperty("content-type", "application/json")
        cabeceras.forEach { (k, v) -> c.setRequestProperty(k, v) }
        try {
            c.outputStream.use { it.write(cuerpo.toString().toByteArray()) }
            val codigo = c.responseCode
            if (codigo !in 200..299) {
                val error = try { c.errorStream?.use { it.bufferedReader().readText() } ?: "" } catch (e: Exception) { "" }
                if (web && codigo in 400..403 && (error.contains("search", true) || error.contains("tool", true) || error.contains("grounding", true))) throw SinBusqueda()
                if (codigo == 404 || (codigo == 400 && error.contains("model", true) && !error.contains("API key", true))) throw ModeloNoDisponible(explicar(p, codigo, error))
                throw IllegalStateException(explicar(p, codigo, error))
            }
            return c.inputStream.use { it.bufferedReader().readText() }
        } finally {
            c.disconnect()
        }
    }

    private fun gemini(clave: String, modelo: String, sistema: String?, mensajes: List<Pair<String, String>>, maxTokens: Int, web: Boolean): String {
        val lista = JSONArray()
        for ((rol, texto) in mensajes) {
            lista.put(JSONObject().put("role", if (rol == "assistant") "model" else "user").put("parts", JSONArray().put(JSONObject().put("text", texto))))
        }
        val cuerpo = JSONObject().put("contents", lista)
            // Los modelos que "piensan" gastan parte del límite en pensar: se deja espacio de sobra.
            .put("generationConfig", JSONObject().put("maxOutputTokens", maxOf(maxTokens * 4, 2048)))
        if (!sistema.isNullOrBlank()) cuerpo.put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", sistema))))
        if (web) cuerpo.put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
        val r = enviar(
            "gemini", "https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent",
            mapOf("x-goog-api-key" to clave.trim()), cuerpo, web,
        )
        val candidatos = JSONObject(r).optJSONArray("candidates")
        val partes = candidatos?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            ?: throw IllegalStateException("Gemini no quiso responder eso. Prueba preguntándolo de otra forma.")
        val texto = StringBuilder()
        for (i in 0 until partes.length()) {
            val p = partes.getJSONObject(i)
            if (!p.optBoolean("thought", false)) texto.append(p.optString("text"))
        }
        return texto.toString().trim()
    }

    private fun openai(clave: String, modelo: String, sistema: String?, mensajes: List<Pair<String, String>>, maxTokens: Int, web: Boolean): String {
        val entrada = JSONArray()
        for ((rol, texto) in mensajes) entrada.put(JSONObject().put("role", rol).put("content", texto))
        val cuerpo = JSONObject().put("model", modelo).put("input", entrada).put("max_output_tokens", maxOf(maxTokens * 3, 2000))
        if (!sistema.isNullOrBlank()) cuerpo.put("instructions", sistema)
        if (web) cuerpo.put("tools", JSONArray().put(JSONObject().put("type", "web_search")))
        val r = enviar("openai", "https://api.openai.com/v1/responses", mapOf("Authorization" to "Bearer ${clave.trim()}"), cuerpo, web)
        val o = JSONObject(r)
        val salida = o.optJSONArray("output") ?: JSONArray()
        val texto = StringBuilder()
        for (i in 0 until salida.length()) {
            val item = salida.getJSONObject(i)
            if (item.optString("type") != "message") continue
            val contenido = item.optJSONArray("content") ?: continue
            for (k in 0 until contenido.length()) {
                val c = contenido.getJSONObject(k)
                if (c.optString("type") == "output_text") texto.append(c.optString("text"))
            }
        }
        if (texto.isBlank()) throw IllegalStateException("ChatGPT no alcanzó a responder. Prueba de nuevo.")
        return texto.toString().trim()
    }

    /** Saca el primer objeto JSON de un texto. */
    fun json(texto: String): JSONObject = JSONObject(texto.substring(texto.indexOf('{'), texto.lastIndexOf('}') + 1))

    /**
     * Reconoce los juegos a partir del nombre de sus archivos.
     * [archivos]: (consola, nombre de archivo). Devuelve nombre de archivo -> nombre oficial.
     */
    fun nombres(llave: Llave, archivos: List<Pair<String, String>>): Map<String, String> {
        val lista = archivos.joinToString("\n") { (c, a) -> "$c | $a" }
        val prompt = """
            Eres un experto en videojuegos. Cada línea es "consola | nombre de archivo" de un juego.
            Para cada una, di el nombre oficial y completo del juego tal como se conoce en Latinoamérica
            (normalmente el título en inglés), sin región, versión, idioma, códigos ni extensión.
            Si es un código de juego (por ejemplo de Switch o PS3) reconócelo si puedes. Si no lo reconoces, limpia el nombre lo mejor posible.
            Responde SOLO con un objeto JSON: {"nombre de archivo exacto": "Nombre oficial", ...}

            $lista
        """.trimIndent()
        val o = json(pedir(llave, prompt, 4000))
        return o.keys().asSequence().associateWith { o.getString(it).trim() }.filterValues { it.isNotEmpty() && it.length < 120 }
    }
}

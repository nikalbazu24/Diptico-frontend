package cl.nikalbazu.diptico.estado

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Discord por webhook: Díptico publica en un canal de tu servidor cuando empiezas o terminas de jugar (con la portada
 * del juego y tu tiempo). No pide tu cuenta ni tu contraseña: solo el enlace del webhook que creas en el canal.
 * (El «Jugando a…» del perfil necesita el kit oficial de Discord para apps, que todavía no está en Díptico.)
 */
object Discord {
    private val PATRON = Regex("""^https://(canary\.|ptb\.)?discord(app)?\.com/api/webhooks/\d+/[\w-]+""")

    /** El enlace parece un webhook de Discord. */
    fun valido(url: String): Boolean = PATRON.containsMatchIn(url.trim())

    /** Dónde se explica cómo crear un webhook (para el código QR del teléfono). */
    const val AYUDA = "https://support.discord.com/hc/es/articles/228383668"

    /** Una tarjeta de Discord (embed) con título, detalle, color y, si hay, la portada adjunta. */
    fun tarjeta(titulo: String, detalle: String, color: Int, conPortada: Boolean, pie: String): JSONObject {
        val embed = JSONObject()
            .put("title", titulo.take(250))
            .put("description", detalle.take(1000))
            .put("color", color and 0xFFFFFF)
            .put("footer", JSONObject().put("text", pie.take(200)))
        if (conPortada) embed.put("thumbnail", JSONObject().put("url", "attachment://portada.jpg"))
        return JSONObject().put("username", "Díptico").put("embeds", JSONArray().put(embed))
    }

    /** La portada del juego achicada a JPEG (Discord la muestra al lado de la tarjeta). */
    fun portada(ctx: Context, uri: Uri?): ByteArray? {
        if (uri == null || uri.scheme == "app") return null
        return try {
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
            val k = 512f / maxOf(bmp.width, bmp.height)
            val chica = if (k < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt().coerceAtLeast(1), (bmp.height * k).toInt().coerceAtLeast(1), true) else bmp
            ByteArrayOutputStream().use { out -> chica.compress(Bitmap.CompressFormat.JPEG, 85, out); out.toByteArray() }
        } catch (_: Exception) { null }
    }

    /** Envía la tarjeta. Devuelve null si salió bien, o el motivo si falló. */
    suspend fun enviar(webhook: String, cuerpo: JSONObject, imagen: ByteArray?): String? = withContext(Dispatchers.IO) {
        if (!valido(webhook)) return@withContext "El enlace no es un webhook de Discord"
        try {
            val con = URL(webhook.trim()).openConnection() as HttpURLConnection
            con.requestMethod = "POST"
            con.doOutput = true
            con.connectTimeout = 10_000
            con.readTimeout = 15_000
            con.setRequestProperty("User-Agent", "Diptico (Android)")
            if (imagen == null) {
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                con.outputStream.use { it.write(cuerpo.toString().toByteArray()) }
            } else {
                // Con la portada: el formulario de varias partes que pide Discord (el JSON y el archivo).
                val limite = "diptico" + System.currentTimeMillis()
                con.setRequestProperty("Content-Type", "multipart/form-data; boundary=$limite")
                con.outputStream.use { o ->
                    fun linea(s: String) = o.write((s + "\r\n").toByteArray())
                    linea("--$limite")
                    linea("Content-Disposition: form-data; name=\"payload_json\"")
                    linea("Content-Type: application/json; charset=utf-8")
                    linea("")
                    linea(cuerpo.toString())
                    linea("--$limite")
                    linea("Content-Disposition: form-data; name=\"files[0]\"; filename=\"portada.jpg\"")
                    linea("Content-Type: image/jpeg")
                    linea("")
                    o.write(imagen)
                    linea("")
                    linea("--$limite--")
                }
            }
            val codigo = con.responseCode
            con.disconnect()
            when {
                codigo in 200..299 -> null
                codigo == 404 || codigo == 401 -> "Discord no encontró ese webhook (¿lo borraron?)"
                codigo == 429 -> "Discord pide esperar un poco antes de mandar otro"
                else -> "Discord respondió $codigo"
            }
        } catch (ex: Exception) {
            "No pude conectar con Discord"
        }
    }
}

/**
 * Iniciar sesión con Discord como en las consolas: Díptico pide un código, muestra un QR (y el código de 8 letras),
 * lo escaneas con el teléfono, apruebas en discord.com/activate y Díptico recibe el permiso solo. Sin escribir
 * contraseñas en la consola. Usa el flujo oficial «device authorization» de Discord con la app de Díptico.
 */
object DiscordCuenta {
    data class Codigo(val dispositivo: String, val usuario: String, val url: String, val urlCompleta: String, val expira: Long, val intervalo: Long)
    data class Token(val acceso: String, val refresco: String, val vence: Long)
    data class Usuario(val id: String, val nombre: String, val avatar: String?)

    /** Los permisos que se piden: saber quién eres y mostrar tu actividad (para el «Jugando a…»). */
    const val PERMISOS = "identify openid sdk.social_layer_presence"
    private const val API = "https://discord.com/api/v10"

    private fun formulario(url: String, campos: Map<String, String>): Pair<Int, String> {
        val con = URL(url).openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.doOutput = true
        con.connectTimeout = 10_000
        con.readTimeout = 15_000
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        con.setRequestProperty("User-Agent", "Diptico (Android)")
        val cuerpo = campos.entries.joinToString("&") { "${it.key}=${java.net.URLEncoder.encode(it.value, "UTF-8")}" }
        con.outputStream.use { it.write(cuerpo.toByteArray()) }
        val codigo = con.responseCode
        val texto = (if (codigo in 200..299) con.inputStream else con.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        con.disconnect()
        return codigo to texto
    }

    /** Paso 1: el código para el QR. Devuelve el código o el motivo del error. */
    suspend fun pedirCodigo(app: String): Pair<Codigo?, String?> = withContext(Dispatchers.IO) {
        try {
            val (n, t) = formulario("$API/oauth2/device/authorize", mapOf("client_id" to app, "scope" to PERMISOS))
            if (n !in 200..299) return@withContext null to motivo(t, n)
            val j = JSONObject(t)
            val url = j.optString("verification_uri", "https://discord.com/activate")
            Codigo(
                j.getString("device_code"), j.getString("user_code"), url,
                j.optString("verification_uri_complete", "$url?user_code=${j.getString("user_code")}"),
                System.currentTimeMillis() + j.optLong("expires_in", 300) * 1000, j.optLong("interval", 5).coerceAtLeast(3),
            ) to null
        } catch (_: Exception) { null to "No pude conectar con Discord" }
    }

    /** Paso 2: preguntar si ya aprobaste. null + null = todavía no; Token = listo; null + texto = falló. */
    suspend fun revisar(app: String, c: Codigo): Pair<Token?, String?> = withContext(Dispatchers.IO) {
        try {
            val (n, t) = formulario("$API/oauth2/token", mapOf(
                "client_id" to app, "grant_type" to "urn:ietf:params:oauth:grant-type:device_code", "device_code" to c.dispositivo,
            ))
            if (n in 200..299) return@withContext leerToken(t) to null
            val error = try { JSONObject(t).optString("error") } catch (_: Exception) { "" }
            when (error) {
                "authorization_pending", "slow_down", "" -> null to null
                "access_denied" -> null to "Cancelaste el permiso en Discord"
                "expired_token" -> null to "El código venció; pide uno nuevo"
                else -> null to motivo(t, n)
            }
        } catch (_: Exception) { null to null }
    }

    /** Renueva el permiso cuando vence (dura una semana). */
    suspend fun renovar(app: String, refresco: String): Token? = withContext(Dispatchers.IO) {
        try {
            val (n, t) = formulario("$API/oauth2/token", mapOf("client_id" to app, "grant_type" to "refresh_token", "refresh_token" to refresco))
            if (n in 200..299) leerToken(t) else null
        } catch (_: Exception) { null }
    }

    /** Tu nombre y tu foto de Discord. */
    suspend fun quienSoy(acceso: String): Usuario? = withContext(Dispatchers.IO) {
        try {
            val con = URL("$API/users/@me").openConnection() as HttpURLConnection
            con.setRequestProperty("Authorization", "Bearer $acceso")
            con.setRequestProperty("User-Agent", "Diptico (Android)")
            con.connectTimeout = 10_000
            if (con.responseCode !in 200..299) return@withContext null
            val j = JSONObject(con.inputStream.bufferedReader().use { it.readText() })
            con.disconnect()
            val id = j.getString("id")
            val av = j.optString("avatar").takeIf { it.isNotBlank() && it != "null" }?.let { "https://cdn.discordapp.com/avatars/$id/$it.png?size=128" }
            Usuario(id, j.optString("global_name").takeIf { it.isNotBlank() && it != "null" } ?: j.optString("username"), av)
        } catch (_: Exception) { null }
    }

    private fun leerToken(t: String): Token {
        val j = JSONObject(t)
        return Token(j.getString("access_token"), j.optString("refresh_token"), System.currentTimeMillis() + j.optLong("expires_in", 604800) * 1000)
    }

    private fun motivo(t: String, n: Int): String {
        val j = try { JSONObject(t) } catch (_: Exception) { null }
        val e = j?.optString("error").orEmpty()
        return when {
            e == "invalid_client" || n == 401 -> "Discord no reconoce la app de Díptico (revisa el Application ID y que tenga «Public Client»)"
            e == "invalid_scope" -> "Discord no aceptó los permisos pedidos"
            e.isNotBlank() -> "Discord respondió: " + (j?.optString("error_description")?.takeIf { it.isNotBlank() } ?: e)
            else -> "Discord respondió $n"
        }
    }
}

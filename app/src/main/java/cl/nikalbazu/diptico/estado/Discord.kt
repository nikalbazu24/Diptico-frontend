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

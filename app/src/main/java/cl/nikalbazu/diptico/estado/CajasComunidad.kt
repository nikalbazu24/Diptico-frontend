package cl.nikalbazu.diptico.estado

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import cl.nikalbazu.diptico.BuildConfig
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.UUID

/**
 * Cajas 3D de la comunidad: DIPPO arma la caja de un juego desde una sola imagen (el escaneo de la caja abierta:
 * reverso | lomo | frente, o solo el frente) y la puede compartir. Las cajas compartidas viven en una carpeta del Drive
 * de Díptico, detrás del mismo Apps Script de los reportes (tools/reportes-drive.gs): cualquiera las baja sin cuenta.
 */
object CajasComunidad {
    /** Las tres caras de una caja, como archivos locales (cualquiera puede faltar). */
    data class Caras(val frente: File?, val lomo: File?, val reverso: File?) {
        val vacia: Boolean get() = frente == null && lomo == null && reverso == null
    }

    val disponible: Boolean get() = BuildConfig.REPORTES_URL.isNotBlank()

    /** La clave de un juego en la base compartida: consola + título sin región, tildes ni signos («snes-super-mario-world»). */
    fun clave(titulo: String, consolaId: String): String {
        val limpio = titulo.replace(Regex("""\s*[\(\[].*?[\)\]]"""), " ")
        val sin = Normalizer.normalize(limpio, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        val slug = sin.lowercase().replace("&", " and ").replace(Regex("[^a-z0-9]+"), "-").trim('-').take(90)
        return "${consolaId.lowercase().replace(Regex("[^a-z0-9]+"), "")}-$slug"
    }

    fun carpeta(ctx: Context, clave: String): File = File(ctx.filesDir, "cajas/$clave").apply { mkdirs() }

    /**
     * DIPPO arma la caja desde una imagen. Si la imagen es ancha (la caja abierta, escaneada entera), se corta en
     * reverso, lomo y frente según la proporción del frente de la caja de esa consola; si no, es solo el frente.
     */
    fun armar(ctx: Context, uri: Uri, aspectoFrente: Float, destino: File): Caras? {
        val bmp = leer(ctx, uri, 2400) ?: return null
        val w = bmp.width; val h = bmp.height
        val proporcion = w.toFloat() / h
        // Una caja abierta mide (dos frentes + un lomo) de ancho: mucho más ancha que un frente solo.
        if (proporcion < aspectoFrente * 1.6f) {
            return Caras(guardar(bmp, File(destino, "frente.jpg")), null, null)
        }
        var anchoFrente = (h * aspectoFrente).toInt()
        var lomo = w - 2 * anchoFrente
        if (lomo < w * 0.025f || lomo > w * 0.3f) { lomo = (w * 0.07f).toInt(); anchoFrente = (w - lomo) / 2 }
        val reverso = Bitmap.createBitmap(bmp, 0, 0, anchoFrente, h)
        val canto = Bitmap.createBitmap(bmp, anchoFrente, 0, lomo.coerceAtLeast(1), h)
        val frente = Bitmap.createBitmap(bmp, (anchoFrente + lomo).coerceAtMost(w - 1), 0, (w - anchoFrente - lomo).coerceAtLeast(1), h)
        return Caras(guardar(frente, File(destino, "frente.jpg")), guardar(canto, File(destino, "lomo.jpg")), guardar(reverso, File(destino, "reverso.jpg")))
    }

    /** Sube una caja a la base compartida. null = quedó guardada; si no, el motivo en palabras simples. */
    fun subir(clave: String, titulo: String, consola: String, caras: Caras): String? {
        if (!disponible) return "Esta versión no tiene configurado el receptor de Drive"
        if (caras.vacia) return "La caja no tiene imágenes"
        val o = JSONObject().put("app", "diptico").put("tipo", "caja").put("id", UUID.randomUUID().toString())
            .put("clave", clave).put("titulo", titulo.take(120)).put("consola", consola.take(60))
        caras.frente?.let { o.put("frente", base64(it)) }
        caras.lomo?.let { o.put("lomo", base64(it)) }
        caras.reverso?.let { o.put("reverso", base64(it)) }
        return try {
            val r = JSONObject(pedir(URL(BuildConfig.REPORTES_URL), o.toString()))
            if (r.optBoolean("ok")) null
            else when (r.optString("motivo")) {
                "lleno" -> "Hoy ya se compartieron muchas cajas. Prueba mañana"
                "grande" -> "Las imágenes pesan demasiado"
                "ocupado" -> "El receptor estaba ocupado. Prueba de nuevo"
                "clave" -> "No pude armar el nombre de la caja para este juego"
                "" -> "El script de tu Drive todavía no sabe recibir cajas: hay que pegar la versión nueva de tools/reportes-drive.gs y publicar una versión nueva de la implementación"
                else -> "El receptor respondió: ${r.optString("motivo")}"
            }
        } catch (ex: java.io.IOException) {
            "No pude conectar con Drive (${ex.javaClass.simpleName}). Revisa internet"
        } catch (ex: org.json.JSONException) {
            "Drive respondió algo que no entiendo: el enlace configurado no es el del script, o pide iniciar sesión"
        } catch (ex: Exception) {
            "Falló al enviar: ${ex.message ?: ex.javaClass.simpleName}"
        }
    }

    /** Busca la caja de un juego en la base compartida y la guarda en [destino]. null = no está (o no hay conexión). */
    fun bajar(clave: String, destino: File): Caras? {
        if (!disponible) return null
        return try {
            val url = URL(BuildConfig.REPORTES_URL + "?caja=" + URLEncoder.encode(clave, "UTF-8"))
            val r = JSONObject(pedir(url, null))
            if (!r.optBoolean("ok")) return null
            fun cara(nombre: String): File? = r.optString(nombre).takeIf { it.isNotBlank() }?.let { b ->
                File(destino, "$nombre.jpg").apply { writeBytes(Base64.decode(b, Base64.DEFAULT)) }
            }
            Caras(cara("frente"), cara("lomo"), cara("reverso")).takeIf { !it.vacia }
        } catch (_: Exception) { null }
    }

    private fun base64(f: File): String = Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)

    private fun leer(ctx: Context, uri: Uri, maximo: Int): Bitmap? = try {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        var muestra = 1
        while (o.outWidth / muestra > maximo || o.outHeight / muestra > maximo) muestra *= 2
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra }) }
    } catch (_: Exception) { null }

    /** Guarda en JPEG, con el alto máximo de 900 px (así cada cara pesa ~100–200 KB al compartirla). */
    private fun guardar(b: Bitmap, f: File): File {
        val escala = (900f / b.height).coerceAtMost(1f)
        val final = if (escala < 1f) Bitmap.createScaledBitmap(b, (b.width * escala).toInt().coerceAtLeast(1), 900, true) else b
        val bytes = ByteArrayOutputStream().also { final.compress(Bitmap.CompressFormat.JPEG, 86, it) }.toByteArray()
        f.parentFile?.mkdirs()
        f.writeBytes(bytes)
        return f
    }

    /** POST (con cuerpo) o GET al Apps Script; sigue el acuse que Google devuelve desde script.googleusercontent.com. */
    private fun pedir(url: URL, cuerpo: String?): String {
        val c = url.openConnection() as HttpURLConnection
        c.instanceFollowRedirects = false
        c.connectTimeout = 12_000
        c.readTimeout = 25_000
        return try {
            if (cuerpo != null) {
                val bytes = cuerpo.toByteArray(Charsets.UTF_8)
                c.requestMethod = "POST"
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                c.setFixedLengthStreamingMode(bytes.size)
                c.outputStream.use { it.write(bytes) }
            }
            when (c.responseCode) {
                HttpURLConnection.HTTP_OK -> c.inputStream.bufferedReader().use { it.readText() }
                HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_SEE_OTHER -> {
                    val destino = URL(c.getHeaderField("Location") ?: error("Sin respuesta"))
                    check(destino.protocol == "https" && destino.host == "script.googleusercontent.com")
                    pedir(destino, null)
                }
                else -> error("Respuesta ${c.responseCode}")
            }
        } finally {
            c.disconnect()
        }
    }
}

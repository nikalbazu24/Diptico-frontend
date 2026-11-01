package cl.nikalbazu.diptico.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import cl.nikalbazu.diptico.data.Fichas
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Carga portadas y artes desde la carpeta del usuario, reducidas y en caché. */
object Imagenes {
    private val cache = LruCache<String, ImageBitmap>(120)

    fun cargar(ctx: Context, uri: Uri, maxLado: Int): ImageBitmap? {
        val clave = "$uri@$maxLado"
        cache.get(clave)?.let { return it }
        if (uri.scheme == "app") {
            // Ícono de una app instalada. "?capa=lleno": sin la máscara de Android (para darle la forma del tema);
            // "?capa=mono": el dibujo de un solo color de Android 13 (íconos temáticos). Si no existe, devuelve null.
            val d = ctx.packageManager.getApplicationIcon(uri.host ?: return null)
            val lado = maxLado.coerceAtMost(512)
            val capa = uri.getQueryParameter("capa")
            val bmp = when (capa) {
                "lleno", "mono" -> {
                    val a = d as? android.graphics.drawable.AdaptiveIconDrawable ?: return null
                    val capas = if (capa == "mono") {
                        if (android.os.Build.VERSION.SDK_INT >= 33) listOfNotNull(a.monochrome).ifEmpty { return null } else return null
                    } else listOfNotNull(a.background, a.foreground)
                    val b = android.graphics.Bitmap.createBitmap(lado, lado, android.graphics.Bitmap.Config.ARGB_8888)
                    val lienzo = android.graphics.Canvas(b)
                    // Las capas miden 108 y lo visible son los 72 del centro: se agrandan un cuarto por lado.
                    val extra = lado / 4
                    for (c in capas) { c.setBounds(-extra, -extra, lado + extra, lado + extra); c.draw(lienzo) }
                    b
                }
                else -> d.toBitmap(lado, lado)
            }
            val img = bmp.asImageBitmap()
            cache.put(clave, img)
            return img
        }
        if (uri.scheme == "http" || uri.scheme == "https") {
            // Imagen de internet (carrusel de la ficha, opciones para elegir): se guarda en caché del teléfono.
            val f = web(ctx, uri.toString()) ?: return null
            return cargar(ctx, Uri.fromFile(f), maxLado)?.also { cache.put(clave, it) }
        }
        val cr = ctx.contentResolver
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null
        var muestra = 1
        while (maxOf(limites.outWidth, limites.outHeight) / (muestra * 2) >= maxLado) muestra *= 2
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
        val bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) } ?: return null
        val img = bmp.asImageBitmap()
        cache.put(clave, img)
        return img
    }

    private fun web(ctx: Context, url: String): File? {
        val dir = File(ctx.cacheDir, "web").apply { mkdirs() }
        val f = File(dir, url.hashCode().toUInt().toString(16) + "-" + url.length)
        if (f.exists() && f.length() > 0) return f
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 20_000
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", Fichas.AGENTE)
        return try {
            if (c.responseCode !in 200..299) return null
            val tmp = File(dir, f.name + ".tmp")
            c.inputStream.use { i -> tmp.outputStream().use { i.copyTo(it) } }
            if (tmp.renameTo(f)) f else null
        } catch (e: Exception) {
            null
        } finally {
            c.disconnect()
        }
    }
}

@Composable
fun rememberImagen(uri: Uri?, maxLado: Int): ImageBitmap? {
    val ctx = LocalContext.current
    val img by produceState<ImageBitmap?>(initialValue = null, uri, maxLado) {
        value = if (uri == null) null else withContext(Dispatchers.IO) {
            try {
                Imagenes.cargar(ctx, uri, maxLado)
            } catch (e: Exception) {
                null
            }
        }
    }
    return img
}

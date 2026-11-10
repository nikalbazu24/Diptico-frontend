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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Carga portadas y artes desde la carpeta del usuario, reducidas y en caché. */
object Imagenes {
    /**
     * Caché medida en bytes (no en cantidad): una imagen grande pesa como 30 portadas chicas.
     * Usa una sexta parte de la memoria de la app, para que recorrer el estante no llene la memoria ni dé tirones.
     */
    private val cache = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 6).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = (value.width * value.height * 4).coerceAtLeast(1)
    }

    /** La imagen si ya está lista (sin esperar): así no parpadea al volver a dibujarse. */
    private fun clave(uri: Uri, maxLado: Int, memoriaGrafica: Boolean) = "$uri@$maxLado:$memoriaGrafica"

    fun enCache(uri: Uri?, maxLado: Int, memoriaGrafica: Boolean = true): ImageBitmap? =
        uri?.let { cache.get(clave(it, maxLado, memoriaGrafica)) }

    fun cargar(ctx: Context, uri: Uri, maxLado: Int, memoriaGrafica: Boolean = true): ImageBitmap? {
        val clave = clave(uri, maxLado, memoriaGrafica)
        cache.get(clave)?.let { return it }
        if (uri.scheme == "app") {
            // Ícono original de una app instalada.
            val d = ctx.packageManager.getApplicationIcon(uri.host ?: return null)
            val lado = maxLado.coerceAtMost(512)
            val bmp = d.toBitmap(lado, lado, if (memoriaGrafica) null else android.graphics.Bitmap.Config.ARGB_8888)
            val img = bmp.asImageBitmap()
            cache.put(clave, img)
            return img
        }
        if (uri.scheme == "http" || uri.scheme == "https") {
            // Imagen de internet (carrusel de la ficha, opciones para elegir): se guarda en caché del teléfono.
            val f = web(ctx, uri.toString()) ?: return null
            return cargar(ctx, Uri.fromFile(f), maxLado, memoriaGrafica)?.also { cache.put(clave, it) }
        }
        val cr = ctx.contentResolver
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null
        var muestra = 1
        while (maxOf(limites.outWidth, limites.outHeight) / (muestra * 2) >= maxLado) muestra *= 2
        // Las imágenes de la cuadrícula también se usan en el desenfoque sobre un bitmap.
        val opciones = BitmapFactory.Options().apply {
            inSampleSize = muestra
            inPreferredConfig = if (memoriaGrafica) android.graphics.Bitmap.Config.HARDWARE else android.graphics.Bitmap.Config.ARGB_8888
        }
        val bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) } ?: return null
        val img = bmp.asImageBitmap()
        cache.put(clave, img)
        return img
    }

    /**
     * La imagen desenfocada de verdad (para fondos): se carga mediana, se le pasa un desenfoque de caja tres veces
     * (queda como un desenfoque gaussiano, suave y sin píxeles) una sola vez, y queda guardada.
     */
    fun desenfocada(ctx: Context, uri: Uri, lado: Int = 240, radio: Int = 10): ImageBitmap? {
        val clave = "blur|$uri|$lado|$radio"
        cache.get(clave)?.let { return it }
        val base = cargar(ctx, uri, lado, memoriaGrafica = false) ?: return null
        val b = base.asAndroidBitmap().copy(android.graphics.Bitmap.Config.ARGB_8888, true) ?: return null
        val w = b.width; val h = b.height
        val px = IntArray(w * h)
        b.getPixels(px, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)
        repeat(3) {
            cajaBlur(px, tmp, w, h, radio, horizontal = true)
            cajaBlur(tmp, px, w, h, radio, horizontal = false)
        }
        b.setPixels(px, 0, w, 0, 0, w, h)
        val img = b.asImageBitmap()
        cache.put(clave, img)
        return img
    }

    /** Un paso de desenfoque de caja (promedio móvil) en una dirección. */
    private fun cajaBlur(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val largo = if (horizontal) w else h
        val lineas = if (horizontal) h else w
        val div = 2 * r + 1
        for (l in 0 until lineas) {
            var sa = 0; var sr = 0; var sg = 0; var sb = 0
            fun idx(k: Int): Int { val q = k.coerceIn(0, largo - 1); return if (horizontal) l * w + q else q * w + l }
            for (k in -r..r) { val c = src[idx(k)]; sa += c ushr 24; sr += (c shr 16) and 255; sg += (c shr 8) and 255; sb += c and 255 }
            for (k in 0 until largo) {
                dst[idx(k)] = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val sale = src[idx(k - r)]; val entra = src[idx(k + r + 1)]
                sa += (entra ushr 24) - (sale ushr 24)
                sr += ((entra shr 16) and 255) - ((sale shr 16) and 255)
                sg += ((entra shr 8) and 255) - ((sale shr 8) and 255)
                sb += (entra and 255) - (sale and 255)
            }
        }
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
fun rememberImagen(uri: Uri?, maxLado: Int, memoriaGrafica: Boolean = true): ImageBitmap? {
    val ctx = LocalContext.current
    val grafica = memoriaGrafica && !Estilo.glass && android.os.Build.VERSION.SDK_INT >= 31
    val img by produceState(initialValue = Imagenes.enCache(uri, maxLado, grafica), uri, maxLado, grafica) {
        if (value != null && value === Imagenes.enCache(uri, maxLado, grafica)) return@produceState
        value = if (uri == null) null else withContext(Dispatchers.IO) {
            try {
                Imagenes.cargar(ctx, uri, maxLado, grafica)
            } catch (e: Exception) {
                null
            }
        }
    }
    return img
}

/** La imagen desenfocada para fondos (se arma una vez, en segundo plano). */
@Composable
fun rememberDesenfocada(uri: Uri?): ImageBitmap? {
    val ctx = LocalContext.current
    val img by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = if (uri == null) null else withContext(Dispatchers.Default) { try { Imagenes.desenfocada(ctx, uri) } catch (_: Exception) { null } }
    }
    return img
}

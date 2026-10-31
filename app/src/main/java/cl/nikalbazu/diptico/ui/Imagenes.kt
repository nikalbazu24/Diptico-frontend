package cl.nikalbazu.diptico.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
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
            // Ícono de una app instalada (ports).
            val d = ctx.packageManager.getApplicationIcon(uri.host ?: return null)
            val lado = maxLado.coerceAtMost(512)
            val img = d.toBitmap(lado, lado).asImageBitmap()
            cache.put(clave, img)
            return img
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

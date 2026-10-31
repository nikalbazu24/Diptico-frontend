package cl.nikalbazu.diptico.ui

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Muestra una imagen propia: si es un GIF (o WebP animado) se mueve.
 * Usa el decodificador de Android, que no carga el archivo completo en memoria a la vez.
 */
@Composable
fun ImagenAnimada(uri: Uri, modifier: Modifier = Modifier, recortar: Boolean = true) {
    val ctx = LocalContext.current
    val dibujo by produceState<Drawable?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                ImageDecoder.decodeDrawable(ImageDecoder.createSource(ctx.contentResolver, uri)) { decoder, info, _ ->
                    // Imágenes muy grandes se achican para no gastar memoria.
                    val lado = maxOf(info.size.width, info.size.height)
                    if (lado > 1600) decoder.setTargetSampleSize((lado / 1600) + 1)
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    AndroidView(
        factory = { c -> ImageView(c).apply { scaleType = if (recortar) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER } },
        update = { v ->
            if (v.drawable !== dibujo) {
                v.setImageDrawable(dibujo)
                (dibujo as? AnimatedImageDrawable)?.start()
            }
        },
        modifier = modifier,
    )
}

package cl.nikalbazu.diptico.ui

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Tres filtros de caja separables, con bordes extendidos y alfa premultiplicado. */
internal class DesenfoqueBitmap(private val ancho: Int, private val alto: Int, private val radio: Int) {
    private val pixeles = IntArray(ancho * alto)
    private val temporal = IntArray(ancho * alto)

    init { require(ancho > 0 && alto > 0 && radio in 1..32) }

    fun aplicar(bitmap: Bitmap) {
        require(bitmap.width == ancho && bitmap.height == alto && bitmap.isMutable)
        bitmap.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto)
        for (i in pixeles.indices) {
            val c = pixeles[i]; val a = c ushr 24
            pixeles[i] = (a shl 24) or ((((c ushr 16) and 255) * a / 255) shl 16) or
                ((((c ushr 8) and 255) * a / 255) shl 8) or ((c and 255) * a / 255)
        }
        repeat(3) {
            barrer(pixeles, temporal, ancho, alto, 1, ancho)
            barrer(temporal, pixeles, alto, ancho, ancho, 1)
        }
        for (i in pixeles.indices) {
            val c = pixeles[i]; val a = c ushr 24
            pixeles[i] = if (a == 0) 0 else (a shl 24) or
                (((((c ushr 16) and 255) * 255 / a).coerceAtMost(255)) shl 16) or
                (((((c ushr 8) and 255) * 255 / a).coerceAtMost(255)) shl 8) or
                (((c and 255) * 255 / a).coerceAtMost(255))
        }
        bitmap.setPixels(pixeles, 0, ancho, 0, 0, ancho, alto)
    }

    private fun barrer(origen: IntArray, destino: IntArray, largo: Int, lineas: Int, paso: Int, salto: Int) {
        val n = radio * 2 + 1
        repeat(lineas) { linea ->
            val inicio = linea * salto
            var a = 0; var r = 0; var g = 0; var b = 0
            fun sumar(c: Int, signo: Int) {
                a += (c ushr 24) * signo; r += ((c ushr 16) and 255) * signo
                g += ((c ushr 8) and 255) * signo; b += (c and 255) * signo
            }
            for (k in -radio..radio) sumar(origen[inicio + k.coerceIn(0, largo - 1) * paso], 1)
            repeat(largo) { x ->
                destino[inicio + x * paso] = ((a / n) shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
                sumar(origen[inicio + (x - radio).coerceIn(0, largo - 1) * paso], -1)
                sumar(origen[inicio + (x + radio + 1).coerceIn(0, largo - 1) * paso], 1)
            }
        }
    }
}

/** Textura suavizada una sola vez por imagen, compartida por las dos pantallas y todos los controles. */
internal object FondosCristal {
    private val cache = object : LruCache<ImageBitmap, ImageBitmap>(256 * 1024) {
        override fun sizeOf(key: ImageBitmap, value: ImageBitmap) = value.width * value.height * 4
    }
    fun disponible(imagen: ImageBitmap) = cache.get(imagen)

    @Synchronized
    fun preparar(imagen: ImageBitmap): ImageBitmap {
        cache.get(imagen)?.let { return it }
        val escala = (96f / maxOf(imagen.width, imagen.height)).coerceAtMost(1f)
        val ancho = (imagen.width * escala).roundToInt().coerceAtLeast(1)
        val alto = (imagen.height * escala).roundToInt().coerceAtLeast(1)
        val reducida = Bitmap.createScaledBitmap(imagen.asAndroidBitmap(), ancho, alto, true)
        val copia = requireNotNull(reducida.copy(Bitmap.Config.ARGB_8888, true))
        if (reducida !== imagen.asAndroidBitmap()) reducida.recycle()
        DesenfoqueBitmap(ancho, alto, 5).aplicar(copia)
        return copia.asImageBitmap().also { cache.put(imagen, it) }
    }
}

@Composable
internal fun recordarFondoCristal(imagen: ImageBitmap?): ImageBitmap? = key(imagen) {
    if (imagen == null) return@key null
    val fondo by produceState(FondosCristal.disponible(imagen), imagen) {
        value = withContext(Dispatchers.Default) { FondosCristal.preparar(imagen) }
    }
    fondo
}

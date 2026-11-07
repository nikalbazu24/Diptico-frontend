package cl.nikalbazu.diptico.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Instantánea local de la escena que hay debajo de una carpeta modal, sin incluir la ventana. */
internal class FondoVentanaCristal {
    var imagen by mutableStateOf<ImageBitmap?>(null)
        private set
    var origen: Offset = Offset.Zero
        private set
    var dimensiones: IntSize = IntSize.Zero
        private set
    fun ubicar(origen: Offset, dimensiones: IntSize) {
        this.origen = origen
        this.dimensiones = dimensiones
    }
    fun publicar(imagen: ImageBitmap) { this.imagen = imagen }
}

internal val LocalFondoVentanaCristal = staticCompositionLocalOf<FondoVentanaCristal?> { null }

/**
 * Se captura una vez por apertura y tamaño: el fondo modal no recibe interacción.
 * Un único buffer pequeño sirve tanto para el exterior difuminado como para el interior del cristal.
 * La ventana nunca se vuelve a capturar a sí misma. Funciona también en Android 10 y 11.
 */
internal fun Modifier.fondoDeVentana(fondo: FondoVentanaCristal, nivel: Int = 70, mostrarDifuminado: Boolean = true): Modifier =
    onGloballyPositioned { fondo.ubicar(it.positionInRoot(), it.size) }.drawWithCache {
        val ancho = size.width.roundToInt().coerceAtLeast(1)
        val alto = size.height.roundToInt().coerceAtLeast(1)
        val paso = 4.dp.toPx()
        val imagen = ImageBitmap((ancho / paso).roundToInt().coerceAtLeast(1), (alto / paso).roundToInt().coerceAtLeast(1))
        val lienzo = Canvas(imagen)
        val filtro = DesenfoqueBitmap(imagen.width, imagen.height, (2f + 3f * nivel.coerceIn(0, 100) / 100f).roundToInt())
        var capturada = false
        onDrawWithContent {
            if (!capturada) {
                val contenido = this
                imagen.asAndroidBitmap().eraseColor(android.graphics.Color.TRANSPARENT)
                draw(this, layoutDirection, lienzo, size) {
                    lienzo.save()
                    try {
                        lienzo.scale(imagen.width.toFloat() / ancho, imagen.height.toFloat() / alto)
                        contenido.drawContent()
                    } finally { lienzo.restore() }
                }
                filtro.aplicar(imagen.asAndroidBitmap())
                capturada = true
                fondo.publicar(imagen)
            }
            if (mostrarDifuminado) drawImage(imagen, dstSize = IntSize(ancho, alto), filterQuality = FilterQuality.Medium) else drawContent()
        }
    }

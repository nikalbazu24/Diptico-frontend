package cl.nikalbazu.diptico.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape as Redondeada

/**
 * Esquinas de Díptico. Normalmente son redondeadas; con un tema de píxeles (Mundo de bloques)
 * se vuelven escalonadas, como en los juegos de antes. Todas las pantallas usan estas funciones.
 */
internal fun formaTema(dp: Dp?, porcentaje: Int, normal: Shape, forma: String = Estilo.tema.forma): Shape = when (forma) {
    "pixel" -> EsquinaPixel(dp, porcentaje)
    "bisel" -> EsquinaBisel(dp, porcentaje)
    "recta" -> Redondeada(2.dp)
    "capsula" -> if (dp != null) Redondeada((dp.value * 1.45f).dp) else Redondeada((porcentaje * 1.3f).toInt().coerceAtMost(50))
    // Recortes: un papel cortado a mano, con las esquinas apenas mordidas y los lados no del todo rectos.
    "recorte" -> EsquinaRecorte(dp, porcentaje)
    // Acuario: más redondo, como una gota.
    "burbuja" -> if (dp != null) Redondeada((dp.value * 1.8f).dp) else Redondeada((porcentaje * 1.6f).toInt().coerceAtMost(50))
    // Salón: casi recto, como el marco de una pantalla de salón.
    "neon" -> Redondeada(3.dp)
    // Constelación: esquinas suaves y parejas.
    "estelar" -> if (dp != null) Redondeada((dp.value * 0.8f).dp) else Redondeada((porcentaje * 0.8f).toInt())
    else -> normal
}

fun RoundedCornerShape(percent: Int): Shape = formaTema(null, percent, Redondeada(percent))

fun RoundedCornerShape(size: Dp): Shape = formaTema(size, 0, Redondeada(size))

fun RoundedCornerShape(topStart: Dp = 0.dp, topEnd: Dp = 0.dp, bottomEnd: Dp = 0.dp, bottomStart: Dp = 0.dp): Shape =
    formaTema(maxOf(topStart, topEnd, bottomEnd, bottomStart), 0, Redondeada(topStart, topEnd, bottomEnd, bottomStart))

private data class EsquinaBisel(val tam: Dp?, val porcentaje: Int) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val pedido = with(density) { tam?.toPx() } ?: (size.minDimension * porcentaje / 100f)
        val c = minOf(pedido, size.minDimension * 0.24f)
        return Outline.Generic(Path().apply {
            moveTo(c, 0f); lineTo(w, 0f); lineTo(w, h - c); lineTo(w - c, h)
            lineTo(0f, h); lineTo(0f, c); close()
        })
    }
}

/** Forma con las cuatro esquinas en dos escalones de píxel. [tam] o [porcentaje] dicen qué tan grande es la esquina. */
class EsquinaPixel(private val tam: Dp?, private val porcentaje: Int) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val pedido = with(density) { tam?.toPx() } ?: (minOf(w, h) * porcentaje / 100f)
        // Escalones chicos y parejos: como máximo 8 dp, y nunca más que un tercio del lado corto.
        val c = minOf(pedido * 0.55f, with(density) { 8.dp.toPx() }, minOf(w, h) / 3f)
        if (c < 1.5f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        val s = c / 2f
        val p = Path().apply {
            moveTo(c, 0f); lineTo(w - c, 0f); lineTo(w - c, s); lineTo(w - s, s); lineTo(w - s, c); lineTo(w, c)
            lineTo(w, h - c); lineTo(w - s, h - c); lineTo(w - s, h - s); lineTo(w - c, h - s); lineTo(w - c, h)
            lineTo(c, h); lineTo(c, h - s); lineTo(s, h - s); lineTo(s, h - c); lineTo(0f, h - c)
            lineTo(0f, c); lineTo(s, c); lineTo(s, s); lineTo(c, s); close()
        }
        return Outline.Generic(p)
    }

    override fun equals(other: Any?) = other is EsquinaPixel && other.tam == tam && other.porcentaje == porcentaje
    override fun hashCode() = (tam?.hashCode() ?: 0) * 31 + porcentaje
}


/** Papel recortado: esquinas cortadas en diagonal chica y bordes que se mueven un poquito, siempre igual para un mismo tamaño. */
private data class EsquinaRecorte(val tam: Dp?, val porcentaje: Int) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val pedido = with(density) { tam?.toPx() } ?: (size.minDimension * porcentaje / 100f)
        val c = minOf(pedido * 0.35f, with(density) { 5.dp.toPx() }, size.minDimension / 4f)
        if (c < 1f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        val j = minOf(with(density) { 1.2.dp.toPx() }, size.minDimension / 30f)
        return Outline.Generic(Path().apply {
            moveTo(c, j * 0.4f); lineTo(w * 0.5f, 0f); lineTo(w - c * 0.6f, j)
            lineTo(w, c); lineTo(w - j * 0.5f, h * 0.5f); lineTo(w - j, h - c * 0.7f)
            lineTo(w - c, h); lineTo(w * 0.45f, h - j * 0.6f); lineTo(c * 0.8f, h - j * 0.2f)
            lineTo(0f, h - c); lineTo(j * 0.6f, h * 0.48f); lineTo(j * 0.2f, c * 0.9f); close()
        })
    }
}

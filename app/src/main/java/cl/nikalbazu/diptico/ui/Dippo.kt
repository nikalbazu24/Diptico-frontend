package cl.nikalbazu.diptico.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Las caras de DIPPO: sus ojos son luz dentro del visor. */
enum class CaraDippo { NORMAL, FELIZ, PENSANDO, CELEBRANDO, SORPRENDIDO, PREOCUPADO, DURMIENDO }

/** Los colores de DIPPO: cálido de día, azulado de noche (de 20:00 a 7:00). */
data class ColoresDippo(val cuerpo: Color, val brillo: Color, val luz: Color, val ojos: Color, val boca: Color)

val DippoDia = ColoresDippo(Color(0xFFFF9A6B), Color(0xFFFFD2A8), Color(0xFFFFE08A), Color(0xFF7EF0D8), Color(0xFF7A2E1D))
val DippoNoche = ColoresDippo(Color(0xFF7AA8FF), Color(0xFFCFE6FF), Color(0xFFCFE6FF), Color(0xFFA9D4FF), Color(0xFF1D2B64))

fun coloresDippoAhora(): ColoresDippo {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return if (h >= 20 || h < 7) DippoNoche else DippoDia
}

/**
 * DIPPO, el pulpito de luz, dibujado en una grilla de 24×24 unidades ([k] = tamaño de una unidad).
 * [luces] (0..1) enciende las puntas de los tentáculos; [saludo] (0..1) levanta el tentáculo derecho.
 */
fun DrawScope.dibujarDippo(
    k: Float, col: ColoresDippo, cara: CaraDippo = CaraDippo.NORMAL,
    parpadeo: Float = 1f, mirar: Float = 0f, luces: Float = 1f, saludo: Float = 0f,
) {
    fun p(x: Float, y: Float) = Offset(x * k, y * k)
    val noche = Color(0xFF15121D)
    // Halo suave de luz.
    drawCircle(col.cuerpo.copy(alpha = 0.10f), 11.5f * k, p(12f, 11f))
    // Tentáculos (el derecho puede saludar).
    val trazo = Stroke(1.5f * k, cap = StrokeCap.Round)
    val puntas = mutableListOf<Offset>()
    fun tentaculo(x0: Float, x1: Float, y1: Float, cx: Float) {
        drawPath(Path().apply { moveTo(x0 * k, 15.4f * k); quadraticBezierTo(cx * k, 19f * k, x1 * k, y1 * k) }, col.cuerpo, style = trazo)
        puntas += p(x1, y1)
    }
    tentaculo(7.6f, 4.6f, 20.6f, 6.2f)
    tentaculo(10.2f, 9f, 21.6f, 9.4f)
    tentaculo(13.8f, 15f, 21.6f, 14.6f)
    val sy = 20.6f - 10.5f * saludo
    val sx = 19.4f + 1.4f * saludo
    drawPath(Path().apply { moveTo(16.4f * k, 15.2f * k); quadraticBezierTo((17.8f + 2f * saludo) * k, (19f - 4f * saludo) * k, sx * k, sy * k) }, col.cuerpo, style = trazo)
    puntas += p(sx, sy)
    // Lucecitas en las puntas.
    puntas.forEachIndexed { i, o ->
        val a = (luces * 4f - i).coerceIn(0f, 1f)
        if (a > 0f) {
            drawCircle(col.luz.copy(alpha = 0.28f * a), 1.5f * k, o)
            drawCircle(col.luz.copy(alpha = a), 0.8f * k, o)
        }
    }
    // Cabeza.
    drawRoundRect(col.cuerpo, p(5.2f, 3.6f), Size(13.6f * k, 12.4f * k), CornerRadius(6.8f * k, 6.2f * k))
    drawPath(Path().apply { moveTo(7f * k, 8.4f * k); quadraticBezierTo(8f * k, 5f * k, 11.6f * k, 4.6f * k) },
        col.brillo.copy(alpha = 0.8f), style = Stroke(0.8f * k, cap = StrokeCap.Round))
    // Visor.
    drawRoundRect(noche, p(6.2f, 8.4f), Size(11.6f * k, 4.2f * k), CornerRadius(2.1f * k))
    val ojos = listOf(9.3f, 14.7f)
    val y = 10.5f * k
    val dx = mirar.coerceIn(-1f, 1f) * 0.5f * k
    val fino = Stroke(0.6f * k, cap = StrokeCap.Round)
    when (cara) {
        CaraDippo.NORMAL -> ojos.forEach { x ->
            val alto = 2f * parpadeo.coerceIn(0.15f, 1f) * k
            drawRoundRect(col.ojos, Offset(x * k - 0.9f * k + dx, y - alto / 2), Size(1.8f * k, alto), CornerRadius(0.9f * k))
        }
        CaraDippo.FELIZ, CaraDippo.CELEBRANDO -> ojos.forEach { x ->
            drawPath(Path().apply { moveTo((x - 1f) * k, y + 0.5f * k); quadraticBezierTo(x * k, y - 1.1f * k, (x + 1f) * k, y + 0.5f * k) },
                if (cara == CaraDippo.CELEBRANDO) col.luz else col.ojos, style = fino)
        }
        CaraDippo.PENSANDO -> {
            drawLine(col.ojos, Offset((ojos[0] - 0.9f) * k, y), Offset((ojos[0] + 0.9f) * k, y), 0.6f * k, StrokeCap.Round)
            drawCircle(col.ojos, 0.85f * k, Offset(ojos[1] * k, y))
        }
        CaraDippo.SORPRENDIDO -> ojos.forEach { x -> drawCircle(col.ojos, 0.95f * k, Offset(x * k, y), style = Stroke(0.45f * k)) }
        CaraDippo.PREOCUPADO -> ojos.forEachIndexed { i, x ->
            val s = if (i == 0) 1f else -1f
            drawLine(Color(0xFFFF8F8F), Offset((x - 0.9f * s) * k, y - 0.9f * k), Offset((x + 0.5f * s) * k, y - 0.5f * k), 0.4f * k, StrokeCap.Round)
            drawCircle(Color(0xFFFF8F8F), 0.5f * k, Offset(x * k, y + 0.4f * k))
        }
        CaraDippo.DURMIENDO -> ojos.forEach { x ->
            drawPath(Path().apply { moveTo((x - 1f) * k, y - 0.2f * k); quadraticBezierTo(x * k, y + 0.7f * k, (x + 1f) * k, y - 0.2f * k) }, col.ojos, style = fino)
        }
    }
    // Boca y mejillas.
    if (cara != CaraDippo.DURMIENDO) {
        val triste = cara == CaraDippo.PREOCUPADO
        drawPath(Path().apply {
            moveTo(11.1f * k, 14.1f * k)
            quadraticBezierTo(12f * k, if (triste) 13.5f * k else 14.8f * k, 12.9f * k, 14.1f * k)
        }, col.boca, style = Stroke(0.45f * k, cap = StrokeCap.Round))
        drawCircle(Color(0xFFFF6F6F).copy(alpha = 0.4f), 0.6f * k, p(7.6f, 13.9f))
        drawCircle(Color(0xFFFF6F6F).copy(alpha = 0.4f), 0.6f * k, p(16.4f, 13.9f))
    }
}

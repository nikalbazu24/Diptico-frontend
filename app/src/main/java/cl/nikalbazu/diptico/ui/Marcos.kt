package cl.nikalbazu.diptico.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Los marcos de la foto de perfil: aros de luz que se mueven sin parar.
 * [t] es una vuelta completa (de 0 a 1) y [g] el grosor del aro. Todo se dibuja en una sola pasada, sin objetos nuevos por cuadro.
 */
internal fun DrawScope.dibujarMarco(id: String, color: Color, t: Float, g: Float) {
    val c = center
    val r = size.minDimension / 2f - g / 2f
    when (id) {
        "doble" -> {
            aroSuave(color, g)
            estela(c, r, g, color, t, 0.42f)
            estela(c, r, g, lerp(color, Color.White, 0.35f), t + 0.5f, 0.42f)
        }
        "chispas" -> {
            aroSuave(color, g * 0.6f)
            for (i in 0 until 10) {
                val a = (2 * PI * (i / 10.0 + t)).toFloat()
                val brillo = (0.35f + 0.65f * abs(sin(2 * PI * (t * 3 + i / 10.0)).toFloat()))
                val rr = g * (0.28f + 0.3f * brillo)
                drawCircle(lerp(color, Color.White, 0.55f).copy(alpha = brillo), rr, Offset(c.x + cos(a) * r, c.y + sin(a) * r))
            }
        }
        "corazon" -> {
            val latido = 0.5f + 0.5f * sin(2 * PI * t * 2).toFloat()
            val rosa = Color(0xFFFF5C8A)
            drawCircle(rosa.copy(alpha = 0.18f + 0.22f * latido), r, c, style = Stroke(g * (1.5f + 0.9f * latido)))
            drawCircle(rosa, r, c, style = Stroke(g * (0.7f + 0.35f * latido)))
        }
        "llama" -> {
            val f = (t * 2f) % 1f
            rotate(360f * f, c) {
                drawCircle(
                    Brush.sweepGradient(0f to Color(0x00FF3B00), 0.25f to Color(0xFFFF3B00), 0.6f to Color(0xFFFF9A1F), 0.92f to Color(0xFFFFE066), 1f to Color.White, center = c),
                    r, c, style = Stroke(g * 1.15f, cap = StrokeCap.Round),
                )
            }
            drawCircle(Color(0xFFFF6A1F).copy(alpha = 0.18f), r, c, style = Stroke(g * 0.6f))
        }
        "noche" -> {
            drawCircle(Color(0xFF1B1446), r, c, style = Stroke(g))
            rotate(360f * t, c) {
                drawCircle(Brush.sweepGradient(0f to Color(0x006C5CE7), 0.7f to Color(0x886C5CE7), 1f to Color(0xFFB8AEFF), center = c), r, c, style = Stroke(g * 0.85f))
            }
            for (i in 0 until 7) {
                val a = (2 * PI * (i / 7.0 + 0.05 * i)).toFloat()
                val br = 0.25f + 0.75f * abs(sin(2 * PI * (t * 2 + i * 0.31)).toFloat())
                drawCircle(Color.White.copy(alpha = br), g * (0.18f + 0.22f * br), Offset(c.x + cos(a) * r, c.y + sin(a) * r))
            }
        }
        "aurora" -> rotate(360f * t, c) {
            drawCircle(
                Brush.sweepGradient(listOf(Color(0xFF38E8E0), Color(0xFF3DDC84), Color(0xFF9B6BFF), Color(0xFF38BDF8), Color(0xFF38E8E0)), center = c),
                r, c, style = Stroke(g * 1.05f),
            )
        }
        "arcoiris" -> rotate(360f * t, c) {
            drawCircle(
                Brush.sweepGradient(listOf(Color(0xFFFF4D4D), Color(0xFFFFC94D), Color(0xFF7BE06A), Color(0xFF38BDF8), Color(0xFF9B6BFF), Color(0xFFE64FA0), Color(0xFFFF4D4D)), center = c),
                r, c, style = Stroke(g * 1.05f),
            )
        }
        "oro" -> {
            drawCircle(
                Brush.sweepGradient(listOf(Color(0xFF9A6A12), Color(0xFFFFD86B), Color(0xFFFFF1B8), Color(0xFFE0A526), Color(0xFF9A6A12)), center = c),
                r, c, style = Stroke(g * 1.05f),
            )
            rotate(360f * t, c) {
                drawCircle(Brush.sweepGradient(0f to Color.Transparent, 0.9f to Color.Transparent, 1f to Color.White.copy(alpha = 0.95f), center = c), r, c, style = Stroke(g * 1.05f))
            }
        }
        else -> { // «estela»: un cometa que da vueltas
            aroSuave(color, g)
            estela(c, r, g, color, t, 0.7f)
        }
    }
}

private fun DrawScope.aroSuave(color: Color, g: Float) {
    drawCircle(color.copy(alpha = 0.22f), size.minDimension / 2f - g / 2f, center, style = Stroke(g * 0.7f))
}

/** Un cometa: cola que se desvanece y una cabeza brillante, [largo] es lo que ocupa de la vuelta (0 a 1). */
private fun DrawScope.estela(c: Offset, r: Float, g: Float, color: Color, fase: Float, largo: Float) {
    val f = ((fase % 1f) + 1f) % 1f
    rotate(360f * f, c) {
        drawCircle(
            Brush.sweepGradient(0f to Color.Transparent, (1f - largo) to Color.Transparent, 0.97f to color, 1f to lerp(color, Color.White, 0.7f), center = c),
            r, c, style = Stroke(g * 1.1f, cap = StrokeCap.Round),
        )
        drawCircle(Color.White.copy(alpha = 0.9f), g * 0.62f, Offset(c.x + r, c.y))
        drawCircle(color.copy(alpha = 0.35f), g * 1.2f, Offset(c.x + r, c.y))
    }
}

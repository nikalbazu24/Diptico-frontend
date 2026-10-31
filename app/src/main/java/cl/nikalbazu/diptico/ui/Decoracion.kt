package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import cl.nikalbazu.diptico.estado.Tema
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** Una partícula: posición inicial (0..1), velocidad, tamaño y fase. Todo se calcula según el tiempo. */
private data class Particula(val x: Float, val y: Float, val v: Float, val t: Float, val fase: Float, val color: Int)

/**
 * Decoración animada del tema (nieve, hojas, murciélagos, confeti…).
 * Es suave y transparente, para no tapar nada; los toques pasan de largo.
 */
@Composable
fun Decoracion(tema: Tema) {
    var tiempo by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(tema.decor) {
        val inicio = withFrameMillis { it }
        while (true) {
            withFrameMillis { tiempo = (it - inicio) / 1000f }
        }
    }
    val particulas = remember(tema.decor) {
        val r = Random(tema.decor.hashCode())
        val n = when (tema.decor) { "estrellas" -> 26; "fuegos" -> 5; "faroles" -> 9; "murcielagos" -> 7; else -> 18 }
        List(n) { Particula(r.nextFloat(), r.nextFloat(), 0.4f + r.nextFloat() * 0.8f, 0.6f + r.nextFloat() * 0.8f, r.nextFloat() * 6.28f, r.nextInt(3)) }
    }
    val colores = listOf(Color(tema.acento), Color(tema.acento2), Color(tema.acento3))
    Canvas(Modifier.fillMaxSize()) {
        val u = size.minDimension / 400f
        for (p in particulas) {
            val c = colores[p.color]
            when (tema.decor) {
                "nieve" -> {
                    val y = ((p.y + tiempo * 0.05f * p.v) % 1.1f) - 0.05f
                    val x = p.x + sin(tiempo * 0.8f + p.fase) * 0.02f
                    drawCircle(Color.White.copy(alpha = 0.55f), (2f + 3f * p.t) * u, Offset(x * size.width, y * size.height))
                }
                "hojas", "petalos" -> {
                    val y = ((p.y + tiempo * 0.04f * p.v) % 1.15f) - 0.08f
                    val x = p.x + sin(tiempo * 0.6f + p.fase) * 0.04f
                    val col = if (tema.decor == "petalos") lerpColor(Color(0xFFFFD6E0), c, 0.35f) else c
                    translate(x * size.width, y * size.height) {
                        rotate(tiempo * 60f * p.v + p.fase * 57f, pivot = Offset.Zero) {
                            drawOval(col.copy(alpha = 0.5f), Offset(-6f * u * p.t, -3f * u * p.t), Size(12f * u * p.t, 6f * u * p.t))
                        }
                    }
                }
                "confeti" -> {
                    val y = ((p.y + tiempo * 0.07f * p.v) % 1.1f) - 0.05f
                    translate(p.x * size.width, y * size.height) {
                        rotate(tiempo * 140f * p.v + p.fase * 57f, pivot = Offset.Zero) {
                            drawRect(c.copy(alpha = 0.6f), Offset(-3f * u, -5f * u), Size(6f * u, 10f * u))
                        }
                    }
                }
                "corazones", "burbujas", "faroles" -> {
                    // Suben despacio.
                    val y = 1.05f - ((p.y + tiempo * 0.035f * p.v) % 1.15f)
                    val x = p.x + sin(tiempo * 0.7f + p.fase) * 0.03f
                    val centro = Offset(x * size.width, y * size.height)
                    when (tema.decor) {
                        "corazones" -> corazon(centro, 9f * u * p.t, c.copy(alpha = 0.5f))
                        "burbujas" -> drawCircle(c.copy(alpha = 0.45f), 7f * u * p.t, centro, style = Stroke(1.6f * u))
                        else -> farol(centro, 10f * u * p.t, c, tiempo + p.fase)
                    }
                }
                "estrellas" -> {
                    val brillo = (sin(tiempo * 2f * p.v + p.fase) + 1f) / 2f
                    estrella(Offset(p.x * size.width, p.y * size.height * 0.75f), (3f + 4f * p.t) * u, Color.White.copy(alpha = 0.15f + 0.5f * brillo))
                }
                "murcielagos" -> {
                    val x = ((p.x + tiempo * 0.04f * p.v) % 1.2f) - 0.1f
                    val y = p.y * 0.6f + sin(tiempo * 1.3f + p.fase) * 0.03f
                    murcielago(Offset(x * size.width, y * size.height), 10f * u * p.t, sin(tiempo * 12f + p.fase), Color(0xFF1A1026).copy(alpha = 0.7f))
                }
                "fuegos" -> {
                    // Cada fuego explota cada cierto tiempo en su lugar.
                    val ciclo = 3.2f + p.v
                    val k = ((tiempo + p.fase) % ciclo) / ciclo
                    if (k < 0.45f) {
                        val r = k / 0.45f
                        val centro = Offset(p.x * size.width, (0.12f + p.y * 0.4f) * size.height)
                        for (i in 0 until 12) {
                            val ang = i * (2 * PI.toFloat() / 12)
                            val d = r * 46f * u * p.t
                            drawCircle(c.copy(alpha = (1f - r) * 0.8f), 2.2f * u, centro + Offset(kotlin.math.cos(ang) * d, sin(ang) * d))
                        }
                    }
                }
            }
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)

private fun DrawScope.corazon(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y + r * 0.9f)
        cubicTo(c.x - r * 1.4f, c.y - r * 0.2f, c.x - r * 0.6f, c.y - r * 1.2f, c.x, c.y - r * 0.4f)
        cubicTo(c.x + r * 0.6f, c.y - r * 1.2f, c.x + r * 1.4f, c.y - r * 0.2f, c.x, c.y + r * 0.9f)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.estrella(c: Offset, r: Float, color: Color) {
    drawLine(color, Offset(c.x - r, c.y), Offset(c.x + r, c.y), r * 0.35f)
    drawLine(color, Offset(c.x, c.y - r), Offset(c.x, c.y + r), r * 0.35f)
    drawCircle(color, r * 0.3f, c)
}

private fun DrawScope.farol(c: Offset, r: Float, color: Color, t: Float) {
    val brillo = 0.35f + 0.25f * abs(sin(t * 1.5f))
    drawCircle(color.copy(alpha = brillo * 0.5f), r * 1.8f, c)
    drawRoundRect(color.copy(alpha = 0.75f), Offset(c.x - r * 0.7f, c.y - r), Size(r * 1.4f, r * 2f), CornerRadius(r * 0.6f))
    drawRect(Color(0xFF2A1A10).copy(alpha = 0.6f), Offset(c.x - r * 0.4f, c.y - r * 1.15f), Size(r * 0.8f, r * 0.2f))
}

private fun DrawScope.murcielago(c: Offset, r: Float, aleteo: Float, color: Color) {
    val ala = r * (0.5f + 0.5f * aleteo)
    val p = Path().apply {
        moveTo(c.x, c.y)
        quadraticBezierTo(c.x - r * 0.6f, c.y - ala, c.x - r * 1.4f, c.y - ala * 0.3f)
        quadraticBezierTo(c.x - r * 0.8f, c.y + r * 0.1f, c.x, c.y + r * 0.3f)
        quadraticBezierTo(c.x + r * 0.8f, c.y + r * 0.1f, c.x + r * 1.4f, c.y - ala * 0.3f)
        quadraticBezierTo(c.x + r * 0.6f, c.y - ala, c.x, c.y)
        close()
    }
    drawPath(p, color)
}

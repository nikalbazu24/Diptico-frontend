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
fun Decoracion(tema: Tema, animada: Boolean = true) {
    if (tema.decor == "nada") return
    var tiempo by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(tema.decor, animada) {
        if (!animada) return@LaunchedEffect
        val inicio = withFrameMillis { it }
        var ultimo = inicio - 33
        while (true) withFrameMillis {
            if (it - ultimo >= 32) {
                tiempo = (it - inicio) / 1000f
                ultimo = it
            }
        }
    }
    val particulas = remember(tema.decor, tema.intensidadDecoracion) {
        val r = Random(tema.decor.hashCode())
        val base = when (tema.decor) {
            "muerdago", "campanas", "chupallas", "volantines" -> 11
            in adornosDeJuego -> 14
            "estrellas" -> 26
            "bloques" -> 16
            "fuegos" -> 5
            "faroles" -> 9
            "murcielagos" -> 7
            else -> 18
        }
        val n = when (tema.intensidadDecoracion) {
            1 -> (base * 2 / 5).coerceAtLeast(2)
            3 -> base * 3 / 2
            else -> base
        }
        List(n) { Particula(r.nextFloat(), r.nextFloat(), 0.4f + r.nextFloat() * 0.8f, 0.6f + r.nextFloat() * 0.8f, r.nextFloat() * 6.28f, r.nextInt(3)) }
    }
    val colores = listOf(Color(tema.acento), Color(tema.acento2), Color(tema.acento3))
    Canvas(Modifier.fillMaxSize()) {
        val u = size.minDimension / 400f
        for (p in particulas) {
            val c = colores[p.color]
            when (tema.decor) {
                in adornosDeJuego -> {
                    val y = ((p.y + tiempo * 0.035f * p.v) % 1.1f) - 0.05f
                    val x = (p.x + if (tema.decor == "anillos") tiempo * 0.06f * p.v else sin(tiempo + p.fase) * 0.028f)
                    val r = (if (animada) 11f * u else size.minDimension * 0.039f) * p.t
                    translate((x - kotlin.math.floor(x)) * size.width, y * size.height) {
                        rotate(sin(tiempo * 0.8f + p.fase) * 25f, pivot = Offset.Zero) {
                            motivoJuego(tema.decor, r, c.copy(alpha = 0.68f))
                        }
                    }
                }
                "muerdago", "campanas", "chupallas", "volantines" -> {
                    val y = ((p.y + tiempo * 0.023f * p.v) % 1.08f) - 0.04f
                    val x = p.x + sin(tiempo * 0.55f + p.fase) * 0.023f
                    val r = (if (animada) 12f * u else size.minDimension * 0.037f) * p.t
                    translate(x * size.width, y * size.height) {
                        rotate(sin(tiempo * 0.65f + p.fase) * 18f, pivot = Offset.Zero) {
                            adornoFestivo(tema.decor, r, p.color)
                        }
                    }
                }
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
                "bloques" -> {
                    // Cubitos que bajan flotando despacio, y chispitas cuadradas que titilan.
                    if (p.t < 1.2f) {
                        val y = ((p.y + tiempo * 0.025f * p.v) % 1.2f) - 0.1f
                        val x = p.x + sin(tiempo * 0.4f + p.fase) * 0.015f
                        cubo(Offset(x * size.width, y * size.height), 9f * u * p.t, p.color)
                    } else {
                        val brillo = (sin(tiempo * 2.4f * p.v + p.fase) + 1f) / 2f
                        val l = 3f * u
                        drawRect(Color(0xFF5CE1E6).copy(alpha = 0.15f + 0.45f * brillo), Offset(p.x * size.width, p.y * size.height), Size(l, l))
                    }
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

/** Un cubito isométrico de píxeles: pasto con tierra, piedra o madera. */
private fun DrawScope.cubo(c: Offset, r: Float, tipo: Int) {
    val (arriba, lado) = when (tipo) {
        0 -> Color(0xFF6CC24A) to Color(0xFF8B5A2B)
        1 -> Color(0xFFA7A7A7) to Color(0xFF7A7A7A)
        else -> Color(0xFFC79A5B) to Color(0xFF8E6535)
    }
    val a = 0.5f
    val h = r * 0.58f
    val tapa = Path().apply { moveTo(c.x, c.y - h); lineTo(c.x + r, c.y - h / 2 + 0f); lineTo(c.x, c.y); lineTo(c.x - r, c.y - h / 2); close() }
    val izq = Path().apply { moveTo(c.x - r, c.y - h / 2); lineTo(c.x, c.y); lineTo(c.x, c.y + r); lineTo(c.x - r, c.y + r - h / 2); close() }
    val der = Path().apply { moveTo(c.x + r, c.y - h / 2); lineTo(c.x, c.y); lineTo(c.x, c.y + r); lineTo(c.x + r, c.y + r - h / 2); close() }
    drawPath(tapa, arriba.copy(alpha = a))
    drawPath(izq, lado.copy(alpha = a))
    drawPath(der, lerpColor(lado, Color.Black, 0.25f).copy(alpha = a))
    // Franja de pasto que cae por los lados.
    if (tipo == 0) {
        drawPath(Path().apply { moveTo(c.x - r, c.y - h / 2); lineTo(c.x, c.y); lineTo(c.x, c.y + r * 0.22f); lineTo(c.x - r, c.y - h / 2 + r * 0.22f); close() }, arriba.copy(alpha = a))
        drawPath(Path().apply { moveTo(c.x + r, c.y - h / 2); lineTo(c.x, c.y); lineTo(c.x, c.y + r * 0.22f); lineTo(c.x + r, c.y - h / 2 + r * 0.22f); close() }, lerpColor(arriba, Color.Black, 0.2f).copy(alpha = a))
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

private fun DrawScope.adornoFestivo(decor: String, r: Float, indice: Int) {
    when (decor) {
        "muerdago" -> {
            val hoja = Color(0xFF54B774).copy(alpha = 0.76f)
            for (angulo in listOf(-37f, 37f)) rotate(angulo, pivot = Offset.Zero) {
                drawOval(hoja, Offset(-r * .3f, -r), Size(r * .6f, r * 1.3f))
            }
            val baya = Color(0xFFD84954).copy(alpha = 0.9f)
            drawCircle(baya, r * .19f, Offset(-r * .21f, r * .24f))
            drawCircle(baya, r * .19f, Offset(r * .21f, r * .24f))
            drawCircle(baya, r * .19f, Offset(0f, r * .51f))
        }
        "campanas" -> {
            val dorado = Color(0xFFFFC85A).copy(alpha = 0.74f)
            drawPath(Path().apply {
                moveTo(-r * .85f, r * .35f)
                quadraticBezierTo(-r * .52f, 0f, -r * .52f, -r * .42f)
                quadraticBezierTo(0f, -r * 1.25f, r * .52f, -r * .42f)
                quadraticBezierTo(r * .52f, 0f, r * .85f, r * .35f)
                close()
            }, dorado)
            drawCircle(dorado, r * .19f, Offset(0f, r * .54f))
        }
        "chupallas" -> {
            val paja = Color(0xFFE9C487).copy(alpha = 0.82f)
            drawRoundRect(paja, Offset(-r * .57f, -r * .65f), Size(r * 1.14f, r * .83f), CornerRadius(r * .21f))
            drawRect(Color(0xFF92562F).copy(alpha = 0.8f), Offset(-r * .57f, -r * .18f), Size(r * 1.14f, r * .19f))
            drawOval(paja, Offset(-r, -r * .02f), Size(r * 2f, r * .42f))
        }
        "volantines" -> {
            val color = when (indice) {
                0 -> Color(0xFFE34352)
                1 -> Color(0xFF5484D5)
                else -> Color(0xFFF5F3EF)
            }.copy(alpha = 0.76f)
            drawPath(Path().apply {
                moveTo(0f, -r)
                lineTo(r * .68f, -r * .23f)
                lineTo(0f, r * .68f)
                lineTo(-r * .68f, -r * .23f)
                close()
            }, color)
            drawPath(Path().apply {
                moveTo(0f, r * .68f)
                quadraticBezierTo(-r * .45f, r, r * .18f, r * 1.34f)
            }, color, style = Stroke(r * .09f))
        }
    }
}

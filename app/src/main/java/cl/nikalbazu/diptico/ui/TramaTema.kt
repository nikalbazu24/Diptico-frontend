package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import cl.nikalbazu.diptico.estado.Tema

/** Las tramas son estáticas y quedan detrás de textos, controles y portadas. */
@Composable
internal fun TramaTema(tema: Tema = Estilo.tema) {
    if (tema.trama.isBlank()) return
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Canvas(Modifier.fillMaxSize()) {
        val tinta = p.tinta.copy(alpha = if (tema.trama == "cielo") 0.22f else 0.11f)
        val paso = esc.d(when (tema.trama) { "barrido" -> 6; "lcd" -> 12; "papel" -> 22; else -> 38 }).toPx().coerceAtLeast(3f)
        val fino = esc.d(1).toPx()
        val w = size.width
        val h = size.height
        when (tema.trama) {
            "ondas" -> {
                val centro = Offset(w * 1.05f, h * 1.18f)
                repeat(14) { i -> drawCircle(tinta, paso * (i + 1), centro, style = Stroke(fino)) }
            }
            "puntos", "cielo" -> {
                repeat((w / paso).toInt()) { x ->
                    repeat((h / paso).toInt()) { y ->
                        if (tema.trama != "cielo" || (x * 7 + y * 13) % 5 == 0) {
                            val dx = if (y % 2 == 0) 0.25f else 0.65f
                            drawCircle(tinta, fino * (1 + (x + y) % 2), Offset((x + dx) * paso, (y + 0.45f) * paso))
                        }
                    }
                }
            }
            "diagonal", "rombos" -> {
                repeat(((w + h) / paso).toInt() + 1) { i ->
                    val x = i * paso
                    drawLine(tinta, Offset(x, 0f), Offset(x - h, h), fino)
                    if (tema.trama == "rombos") drawLine(tinta, Offset(x - h, 0f), Offset(x, h), fino)
                }
            }
            else -> {
                repeat((h / paso).toInt()) { i -> drawLine(tinta, Offset(0f, i * paso), Offset(w, i * paso), fino) }
                if (tema.trama in setOf("lcd", "reticula")) repeat((w / paso).toInt()) { i ->
                    drawLine(tinta, Offset(i * paso, 0f), Offset(i * paso, h), fino)
                }
            }
        }
    }
}

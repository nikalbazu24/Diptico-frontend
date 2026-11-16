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
        val paso = esc.d(when (tema.trama) { "barrido" -> 6; "lcd" -> 12; "papel" -> 22; "carton" -> 30; "mar" -> 46; "constelacion" -> 60; else -> 38 }).toPx().coerceAtLeast(3f)
        val fino = esc.d(1).toPx()
        val w = size.width
        val h = size.height
        when (tema.trama) {
            "carton" -> {
                // Cartón: fibras cortas y pareja de líneas finas, como la cara de una caja.
                val fibra = p.tinta.copy(alpha = 0.07f)
                val r = java.util.Random(11)
                repeat(((w * h) / (paso * paso * 2.5f)).toInt().coerceAtMost(900)) {
                    val x = r.nextFloat() * w; val y = r.nextFloat() * h; val l = paso * (0.2f + r.nextFloat() * 0.5f)
                    drawLine(fibra, Offset(x, y), Offset(x + l, y + l * (r.nextFloat() - 0.5f) * 0.3f), fino)
                }
                var y = 0f
                while (y < h) { drawLine(p.tinta.copy(alpha = 0.035f), Offset(0f, y), Offset(w, y), fino * 2); y += paso * 0.4f }
            }
            "mar" -> {
                // Rayos de luz que bajan desde arriba y un brillo de olas en la superficie.
                for (i in 0 until 5) {
                    val x = w * (0.12f + i * 0.21f)
                    val ancho = w * (0.04f + (i % 3) * 0.025f)
                    drawPath(androidx.compose.ui.graphics.Path().apply {
                        moveTo(x, 0f); lineTo(x + ancho, 0f); lineTo(x + ancho * 3.2f - w * 0.12f, h); lineTo(x + ancho * 1.4f - w * 0.12f, h); close()
                    }, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f), androidx.compose.ui.graphics.Color.Transparent), 0f, h * 0.9f))
                }
                var x = 0f
                while (x < w) { drawArc(tinta, 180f, 180f, false, Offset(x, -paso * 0.2f), androidx.compose.ui.geometry.Size(paso, paso * 0.5f), style = Stroke(fino)); x += paso }
            }
            "marquesina" -> {
                // Líneas de pantalla de salón, muy juntas y suaves.
                val linea = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.035f)
                var y = 0f
                val l = esc.d(3).toPx().coerceAtLeast(2f)
                while (y < h) { drawLine(linea, Offset(0f, y), Offset(w, y), fino); y += l }
            }
            "constelacion" -> {
                // Cielo de estrellas fijas; algunas unidas por líneas finas como constelaciones.
                val r = java.util.Random(23)
                val estrellas = List(((w * h) / (paso * paso * 0.9f)).toInt().coerceIn(20, 260)) { Offset(r.nextFloat() * w, r.nextFloat() * h) to r.nextFloat() }
                estrellas.forEach { (c, b) -> drawCircle(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f + 0.35f * b), fino * (0.6f + b * 1.2f), c) }
                val oro = androidx.compose.ui.graphics.Color(0xFFFFE7A6).copy(alpha = 0.14f)
                estrellas.take(18).windowed(2).forEachIndexed { i, (a, b) ->
                    if (i % 3 != 2 && (a.first - b.first).getDistance() < paso * 5f) drawLine(oro, a.first, b.first, fino)
                }
            }
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

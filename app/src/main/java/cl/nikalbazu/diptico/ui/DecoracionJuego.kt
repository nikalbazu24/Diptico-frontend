package cl.nikalbazu.diptico.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal val adornosDeJuego = setOf("monedas", "esferas", "reliquias", "orbitas", "destellos", "anillos")

internal fun DrawScope.motivoJuego(tipo: String, radio: Float, color: Color) {
    val r = radio
    when (tipo) {
        "monedas" -> {
            val oro = Color(0xFFFFC858).copy(alpha = color.alpha)
            drawRoundRect(oro, Offset(-r * .56f, -r), Size(r * 1.12f, r * 2), CornerRadius(r * .34f))
            drawLine(Color(0xFF955526).copy(alpha = color.alpha), Offset(0f, -r * .5f), Offset(0f, r * .5f), r * .16f)
        }
        "esferas" -> {
            drawCircle(Color(0xFFFFF1D7).copy(alpha = color.alpha), r, Offset.Zero)
            drawArc(color, 180f, 180f, true, Offset(-r, -r), Size(r * 2, r * 2))
            drawLine(Color(0xFF302C39).copy(alpha = color.alpha), Offset(-r, 0f), Offset(r, 0f), r * .16f)
            drawCircle(Color(0xFFF7E8C8).copy(alpha = color.alpha), r * .24f, Offset.Zero)
        }
        "reliquias", "destellos" -> {
            val centro = if (tipo == "reliquias") r * .52f else r * .23f
            val forma = Path().apply {
                moveTo(0f, -r)
                if (tipo == "destellos") lineTo(centro, -centro)
                lineTo(r * .73f, 0f)
                if (tipo == "destellos") lineTo(centro, centro)
                lineTo(0f, r)
                if (tipo == "destellos") lineTo(-centro, centro)
                lineTo(-r * .73f, 0f)
                if (tipo == "destellos") lineTo(-centro, -centro)
                close()
            }
            drawPath(forma, color)
            if (tipo == "reliquias") drawLine(Color.White.copy(alpha = .35f), Offset(0f, -r * .8f), Offset(0f, r * .8f), r * .1f)
        }
        "orbitas" -> {
            drawOval(color, Offset(-r, -r * .49f), Size(r * 2, r * .98f), style = Stroke(r * .13f))
            drawCircle(color, r * .27f, Offset.Zero)
            drawCircle(color, r * .16f, Offset(r * .91f, -r * .15f))
        }
        "anillos" -> {
            val oro = Color(0xFFF5BF4A).copy(alpha = color.alpha)
            drawOval(oro, Offset(-r * .64f, -r), Size(r * 1.28f, r * 2), style = Stroke(r * .24f))
            drawLine(oro.copy(alpha = oro.alpha * .55f), Offset(-r * 1.5f, 0f), Offset(-r * .9f, 0f), r * .13f)
        }
    }
}

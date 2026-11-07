package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import cl.nikalbazu.diptico.data.Consola

@Composable
internal fun IconoHardware(consola: Consola, color: Color, fondo: Color, modifier: Modifier = Modifier, tam: Float) {
    val esc = LocalEscala.current
    Canvas(modifier.size(esc.d(tam))) { dibujarHardware(consola, color, fondo) }
}

internal fun DrawScope.dibujarHardware(consola: Consola, color: Color, fondo: Color) {
    val u = size.minDimension / 24f
    val inicio = Offset((size.width - 24 * u) / 2, (size.height - 24 * u) / 2)
    val hueco = if (fondo.alpha < 0.25f) Color(0xFF242034) else fondo
    fun p(x: Float, y: Float) = inicio + Offset(x * u, y * u)
    fun caja(x: Float, y: Float, w: Float, h: Float, radio: Float = 2f, tinta: Color = color) =
        drawRoundRect(tinta, p(x, y), Size(w * u, h * u), CornerRadius(radio * u))
    fun punto(x: Float, y: Float, r: Float = 1f, tinta: Color = hueco) = drawCircle(tinta, r * u, p(x, y))
    fun linea(x: Float, y: Float, xx: Float, yy: Float, tinta: Color = hueco) =
        drawLine(tinta, p(x, y), p(xx, yy), 1.2f * u, StrokeCap.Round)
    fun cruceta(x: Float, y: Float) {
        linea(x - 1.4f, y, x + 1.4f, y)
        linea(x, y - 1.4f, x, y + 1.4f)
    }
    when (consola.id) {
        "android", "androidapps", "androidgames" -> {
            linea(7f, 6f, 5.5f, 3.5f, color); linea(17f, 6f, 18.5f, 3.5f, color)
            val cabeza = Path().apply {
                moveTo(p(5f, 10f).x, p(5f, 10f).y)
                cubicTo(p(5f, 1.8f).x, p(5f, 1.8f).y, p(19f, 1.8f).x, p(19f, 1.8f).y, p(19f, 10f).x, p(19f, 10f).y)
                close()
            }
            drawPath(cabeza, color)
            punto(8.7f, 7.3f, .7f); punto(15.3f, 7.3f, .7f)
            caja(5f, 11.2f, 14f, 7.4f, 1.5f)
            caja(1.9f, 11.2f, 2f, 7f, 1f); caja(20.1f, 11.2f, 2f, 7f, 1f)
            caja(7.3f, 17f, 2.5f, 5.5f, 1.2f); caja(14.2f, 17f, 2.5f, 5.5f, 1.2f)
        }
        "rpgmaker", "easyrpg" -> {
            val gema = Path().apply {
                moveTo(p(12f, 2f).x, p(12f, 2f).y); lineTo(p(21f, 9f).x, p(21f, 9f).y)
                lineTo(p(12f, 22f).x, p(12f, 22f).y); lineTo(p(3f, 9f).x, p(3f, 9f).y); close()
            }
            drawPath(gema, color)
            linea(5f, 9f, 19f, 9f); linea(12f, 4f, 8f, 9f); linea(8f, 9f, 12f, 19f)
            linea(12f, 4f, 16f, 9f); linea(16f, 9f, 12f, 19f)
        }
        "switch", "switch2", "psp", "psvita", "gba", "gamegear", "atarilynx", "ngp", "ngpc", "wonderswan", "wonderswancolor" -> {
            val modular = consola.id.startsWith("switch")
            caja(1f, 6f, 22f, 12f, if (modular) 3f else 5f)
            caja(6.4f, 7.5f, 11.2f, 9f, if (modular) 1f else 2f, hueco)
            cruceta(3.8f, if (modular) 10.5f else 12f)
            punto(20.3f, 11f, 0.85f)
            punto(19.6f, 14.2f, 0.72f)
            if (modular) { linea(5f, 7f, 5f, 17f); linea(19f, 7f, 19f, 17f) }
        }
        "gb", "sgb", "gbc" -> {
            caja(5f, 2f, 14f, 20f, if (consola.id == "gb") 2f else 4f)
            caja(7f, 4.7f, 10f, 7.5f, 1.4f, hueco)
            cruceta(9.2f, 15.4f)
            punto(15.9f, 14.2f); punto(14.6f, 16.2f)
            linea(11.8f, 19f, 13.4f, 18.6f)
        }
        "nds", "n3ds", "wiiu" -> {
            caja(3.4f, 2f, 17.2f, 10f, 2.6f)
            caja(3.4f, 12.8f, 17.2f, 9.2f, 2.6f)
            caja(if (consola.id == "n3ds") 5.7f else 7f, 3.7f, if (consola.id == "n3ds") 12.6f else 10f, 6.5f, 0.9f, hueco)
            caja(8.5f, 14.3f, 7f, 5.8f, 0.8f, hueco)
            cruceta(6f, 17f); punto(18f, 16.8f)
        }
        "gc", "dreamcast", "saturn", "saturnjp", "psx", "3do" -> {
            val cubo = consola.id == "gc"
            caja(3f, if (cubo) 4.5f else 6.4f, 18f, if (cubo) 15.8f else 12f, if (cubo) 2.2f else 4.5f)
            punto(12f, 10.7f, if (cubo) 4.6f else 4f)
            punto(12f, 10.7f, 2.3f, color.copy(alpha = 0.78f))
            punto(7f, 16.1f, 0.8f); punto(10.3f, 16.1f, 0.8f); punto(14f, 16.1f, 0.8f)
        }
        "ps2", "ps3", "ps4", "xbox", "xbox360", "xboxone", "wii" -> {
            val vertical = consola.id in setOf("ps2", "wii", "xboxone")
            if (vertical) {
                caja(7f, 2f, 10f, 19.5f, 2f)
                linea(10f, 5.3f, 10f, 17.4f)
                punto(14.5f, 18.2f, 0.9f)
                caja(5f, 21f, 14f, 1.4f, 0.7f)
            } else {
                caja(2f, 7f, 20f, 10.5f, if (consola.id in setOf("ps3", "xbox360")) 4f else 1.5f)
                linea(5f, 12f, 13f, 12f)
                punto(18.3f, 12f, 1.45f)
            }
        }
        "nes", "famicom", "snes", "sfc", "snesna", "n64", "genesis", "megadrive", "megadrivejp", "mastersystem", "atari2600", "atari7800", "neogeo", "pcengine", "tg16" -> {
            caja(2.3f, 6f, 19.4f, 13f, if (consola.id in setOf("n64", "genesis", "megadrive", "megadrivejp")) 4f else 2f)
            caja(6.3f, 8.1f, 11.4f, 3.5f, 1f, hueco)
            linea(5.8f, 14.3f, 18.2f, 14.3f)
            punto(7.5f, 16.7f, 0.8f); punto(16.5f, 16.7f, 0.8f)
        }
        else -> dibujarMedio(consola.medio, color, fondo)
    }
}

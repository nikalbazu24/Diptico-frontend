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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import cl.nikalbazu.diptico.estado.Icono

/**
 * Íconos propios de Díptico: trazos redondeados y gruesos, dibujados en una grilla de 24.
 * [acento] colorea un detalle (el segundo panel, una nota, una flecha).
 */
@Composable
fun IconoDiptico(icono: Icono, color: Color, tam: Float = 24f, modifier: Modifier = Modifier, acento: Color = color) {
    val esc = LocalEscala.current
    Canvas(modifier.size(esc.d(tam))) { dibujarIcono(icono, color, acento) }
}

private fun DrawScope.dibujarIcono(icono: Icono, c: Color, a: Color) {
    val k = size.width / 24f
    val trazo = Stroke(width = 2.2f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun p(x: Float, y: Float) = Offset(x * k, y * k)
    fun linea(x1: Float, y1: Float, x2: Float, y2: Float, col: Color = c) = drawLine(col, p(x1, y1), p(x2, y2), trazo.width, StrokeCap.Round)
    fun caja(x: Float, y: Float, w: Float, h: Float, r: Float, col: Color = c, lleno: Boolean = false) =
        if (lleno) drawRoundRect(col, p(x, y), Size(w * k, h * k), CornerRadius(r * k))
        else drawRoundRect(col, p(x, y), Size(w * k, h * k), CornerRadius(r * k), style = trazo)
    fun circulo(x: Float, y: Float, r: Float, col: Color = c, lleno: Boolean = false) =
        if (lleno) drawCircle(col, r * k, p(x, y)) else drawCircle(col, r * k, p(x, y), style = trazo)
    fun camino(col: Color = c, lleno: Boolean = false, f: Path.() -> Unit) {
        val path = Path().apply(f)
        if (lleno) drawPath(path, col) else drawPath(path, col, style = trazo)
    }
    fun Path.m(x: Float, y: Float) = moveTo(x * k, y * k)
    fun Path.l(x: Float, y: Float) = lineTo(x * k, y * k)

    when (icono) {
        Icono.PANTALLAS -> {
            caja(4f, 3f, 16f, 8.5f, 2.2f)
            caja(6.5f, 13.5f, 11f, 7.5f, 2.2f, a)
        }
        Icono.UNA -> {
            caja(3f, 5f, 18f, 11f, 2.4f)
            linea(9f, 20f, 15f, 20f)
        }
        Icono.NEGRO -> {
            caja(4f, 3f, 16f, 8.5f, 2.2f)
            caja(6.5f, 13.5f, 11f, 7.5f, 2.2f, a, lleno = true)
        }
        Icono.LIBRE -> {
            caja(4f, 3f, 16f, 8.5f, 2.2f)
            caja(6.5f, 13.5f, 11f, 7.5f, 2.2f, a)
            circulo(12f, 17.25f, 1.2f, a, lleno = true)
        }
        Icono.INTERCAMBIAR -> {
            linea(5f, 8f, 18f, 8f); linea(15f, 5f, 18f, 8f); linea(15f, 11f, 18f, 8f)
            linea(19f, 16f, 6f, 16f, a); linea(9f, 13f, 6f, 16f, a); linea(9f, 19f, 6f, 16f, a)
        }
        Icono.TEXTO -> {
            camino { m(3f, 19f); l(8f, 5f); l(13f, 19f) }
            linea(5f, 14f, 11f, 14f)
            circulo(17.5f, 16f, 3f, a)
            linea(20.5f, 13f, 20.5f, 19f, a)
        }
        Icono.TEMA -> {
            circulo(12f, 12f, 8.5f)
            camino(lleno = true) {
                m(12f, 3.5f)
                arcTo(androidx.compose.ui.geometry.Rect(3.5f * k, 3.5f * k, 20.5f * k, 20.5f * k), -90f, 180f, false)
                close()
            }
        }
        Icono.INTRO -> {
            caja(4f, 5f, 16f, 6f, 3f, c, lleno = true)
            caja(6.5f, 13f, 11f, 6f, 3f, a, lleno = true)
        }
        Icono.MUSICA -> {
            linea(9f, 17f, 9f, 5f); linea(9f, 5f, 19f, 3f); linea(19f, 3f, 19f, 15f)
            circulo(6.5f, 17.5f, 2.6f, a, lleno = true)
            circulo(16.5f, 15.5f, 2.6f, a, lleno = true)
        }
        Icono.VOLUMEN -> {
            camino(lleno = true) { m(4f, 9f); l(8f, 9f); l(13f, 5f); l(13f, 19f); l(8f, 15f); l(4f, 15f); close() }
            camino(a) { m(16.5f, 8.5f); quadraticBezierTo(19f * k, 12f * k, 16.5f * k, 15.5f * k) }
            camino(a) { m(19f, 6f); quadraticBezierTo(23f * k, 12f * k, 19f * k, 18f * k) }
        }
        Icono.SILENCIO -> {
            camino(lleno = true) { m(4f, 9f); l(8f, 9f); l(13f, 5f); l(13f, 19f); l(8f, 15f); l(4f, 15f); close() }
            linea(16f, 9f, 21f, 15f, a); linea(21f, 9f, 16f, 15f, a)
        }
        Icono.CARPETA -> {
            camino { m(3f, 7f); l(3f, 18f); l(21f, 18f); l(21f, 9f); l(12f, 9f); l(10f, 6f); l(4f, 6f); close() }
            linea(3f, 11.5f, 21f, 11.5f, a)
        }
        Icono.EMULADOR -> {
            caja(2.5f, 7f, 19f, 11f, 5.5f)
            linea(6.5f, 12.5f, 10.5f, 12.5f); linea(8.5f, 10.5f, 8.5f, 14.5f)
            circulo(15.5f, 11f, 1.3f, a, lleno = true)
            circulo(18f, 13.8f, 1.3f, a, lleno = true)
        }
        Icono.PORTADA -> {
            caja(3.5f, 4f, 17f, 16f, 2.5f)
            camino(a) { m(5f, 18f); l(10f, 12f); l(13f, 15f); l(15.5f, 12.5f); l(19.5f, 17f) }
            circulo(15.5f, 8f, 1.6f, a, lleno = true)
        }
        Icono.DESCARGA -> {
            linea(12f, 3.5f, 12f, 14.5f, a); linea(7.5f, 10.5f, 12f, 15f, a); linea(16.5f, 10.5f, 12f, 15f, a)
            camino { m(4f, 15f); l(4f, 20f); l(20f, 20f); l(20f, 15f) }
        }
        Icono.CLAVE -> {
            circulo(8f, 12f, 4.5f)
            linea(12.5f, 12f, 21f, 12f, a); linea(18f, 12f, 18f, 15.5f, a); linea(21f, 12f, 21f, 15f, a)
        }
        Icono.TITULO -> {
            linea(4f, 6f, 20f, 6f); linea(12f, 6f, 12f, 19f)
            linea(16f, 19f, 20f, 19f, a)
        }
        Icono.BORRAR -> {
            linea(4f, 6.5f, 20f, 6.5f); linea(9.5f, 6.5f, 10f, 3.5f); linea(14.5f, 6.5f, 14f, 3.5f); linea(10f, 3.5f, 14f, 3.5f)
            camino { m(6f, 6.5f); l(7.2f, 20f); l(16.8f, 20f); l(18f, 6.5f) }
            linea(10.5f, 10.5f, 10.5f, 16f, a); linea(13.5f, 10.5f, 13.5f, 16f, a)
        }
        Icono.BIBLIOTECA -> {
            caja(3.5f, 6f, 4.5f, 13f, 1.2f)
            caja(9.5f, 4f, 4.5f, 15f, 1.2f, a)
            camino { m(15.6f, 7.3f); l(19.6f, 6.2f); l(22f, 18f); l(18f, 19.1f); close() }
            linea(2f, 21f, 22f, 21f)
        }
        Icono.BUSCAR -> {
            circulo(10.5f, 10.5f, 6f)
            linea(15f, 15f, 20.5f, 20.5f, a)
        }
        Icono.TUTORIAL -> {
            camino { m(3f, 5f); l(9f, 5f); quadraticBezierTo(12f * k, 5f * k, 12f * k, 8f * k); l(12f, 20f); quadraticBezierTo(12f * k, 18f * k, 9f * k, 18f * k); l(3f, 18f); close() }
            camino(a) { m(21f, 5f); l(15f, 5f); quadraticBezierTo(12f * k, 5f * k, 12f * k, 8f * k); l(12f, 20f); quadraticBezierTo(12f * k, 18f * k, 15f * k, 18f * k); l(21f, 18f); close() }
        }
        Icono.INFO -> {
            circulo(12f, 12f, 9f)
            linea(12f, 11f, 12f, 17f, a)
            circulo(12f, 7.5f, 1.3f, a, lleno = true)
        }
        Icono.CERRAR -> {
            linea(6f, 6f, 18f, 18f); linea(18f, 6f, 6f, 18f)
        }
        Icono.JUGAR -> {
            camino(lleno = true) { m(7f, 4.5f); l(19.5f, 12f); l(7f, 19.5f); close() }
        }
        Icono.DADO -> {
            caja(3.5f, 3.5f, 17f, 17f, 4f)
            circulo(8.3f, 8.3f, 1.5f, a, lleno = true)
            circulo(12f, 12f, 1.5f, a, lleno = true)
            circulo(15.7f, 15.7f, 1.5f, a, lleno = true)
        }
        Icono.LAPIZ -> {
            camino { m(4f, 20f); l(5f, 15.5f); l(15.5f, 5f); l(19f, 8.5f); l(8.5f, 19f); close() }
            linea(13f, 7.5f, 16.5f, 11f, a)
        }
        Icono.CONTINUAR -> {
            camino(lleno = true) { m(4f, 5f); l(13f, 12f); l(4f, 19f); close() }
            camino(a, lleno = true) { m(12f, 5f); l(21f, 12f); l(12f, 19f); close() }
        }
        Icono.MENU -> {
            linea(4f, 7f, 20f, 7f); linea(4f, 12f, 20f, 12f, a); linea(4f, 17f, 20f, 17f)
        }
        Icono.CHECK -> {
            camino { m(4.5f, 12.5f); l(9.5f, 17.5f); l(19.5f, 6.5f) }
        }
        Icono.ARRIBA -> {
            caja(4f, 3f, 16f, 8.5f, 2.2f, a, lleno = true)
            caja(6.5f, 13.5f, 11f, 7.5f, 2.2f)
        }
        Icono.ABAJO -> {
            caja(4f, 3f, 16f, 8.5f, 2.2f)
            caja(6.5f, 13.5f, 11f, 7.5f, 2.2f, a, lleno = true)
        }
        Icono.APPS -> {
            caja(3.5f, 3.5f, 7.5f, 7.5f, 2.2f)
            caja(13f, 3.5f, 7.5f, 7.5f, 2.2f)
            caja(3.5f, 13f, 7.5f, 7.5f, 2.2f)
            caja(13f, 13f, 7.5f, 7.5f, 2.2f, a, lleno = true)
        }
        Icono.ESTRELLA -> {
            camino(lleno = true) {
                m(12f, 3f); l(14.4f, 9f); l(21f, 9.4f); l(15.9f, 13.6f); l(17.6f, 20f)
                l(12f, 16.4f); l(6.4f, 20f); l(8.1f, 13.6f); l(3f, 9.4f); l(9.6f, 9f); close()
            }
        }
    }
}

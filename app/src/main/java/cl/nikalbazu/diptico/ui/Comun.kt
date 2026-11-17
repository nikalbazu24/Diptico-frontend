package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/** Qué pantalla dibuja una vista: la de arriba, la de abajo o la única (una pantalla). */
enum class Rol { SUPERIOR, INFERIOR, UNICA }

/** Un circulito que gira mientras algo carga. */
@Composable
fun Girador(tam: Float, color: Color = Uva) {
    val esc = LocalEscala.current
    val inf = rememberInfiniteTransition(label = "gira")
    val angulo by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "angulo")
    Canvas(Modifier.size(esc.d(tam))) {
        drawArc(color = color, startAngle = angulo, sweepAngle = 270f, useCenter = false, style = Stroke(width = size.width * 0.14f))
    }
}

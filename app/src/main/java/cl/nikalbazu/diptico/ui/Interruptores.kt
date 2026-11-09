package cl.nikalbazu.diptico.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs

/**
 * Interruptor «líquido»: la bolita se estira mientras viaja (como una gota) y se posa con un rebote suave;
 * al encenderse, un brillo recorre la pista. Con las animaciones apagadas, salta directo.
 */
@Composable
internal fun InterruptorLiquido(si: Boolean, color: Color) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val mov = ajusteMovimiento()
    val pos = remember { Animatable(if (si) 1f else 0f) }
    LaunchedEffect(si, mov.activo) {
        if (!mov.activo) pos.snapTo(if (si) 1f else 0f)
        else pos.animateTo(if (si) 1f else 0f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow / mov.duracion.coerceAtLeast(0.3f)))
    }
    val pista by animateColorAsState(if (si) color else p.linea, tween(mov.ms(220).coerceAtLeast(1)), label = "pista")
    Canvas(Modifier.size(esc.d(46), esc.d(26))) {
        val r = size.height / 2f
        drawRoundRect(pista, cornerRadius = CornerRadius(r))
        val f = pos.value
        // Brillo que acompaña a la bolita cuando enciende.
        if (f > 0.02f) drawRoundRect(
            Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.22f * f)), endX = size.width * f),
            cornerRadius = CornerRadius(r),
        )
        val m = 3f * density
        val diam = size.height - 2 * m
        // Estiramiento: máximo a mitad de camino (la velocidad de la gota).
        val estira = (1f - abs(f * 2f - 1f)) * diam * 0.45f * mov.intensidad.coerceAtMost(1.2f)
        val x = m + (size.width - 2 * m - diam) * f.coerceIn(-0.1f, 1.1f) - estira / 2f
        drawRoundRect(Color.Black.copy(alpha = 0.16f), Offset(x, m + 1.2f * density), Size(diam + estira, diam), CornerRadius(diam / 2f))
        drawRoundRect(Color.White, Offset(x, m), Size(diam + estira, diam), CornerRadius(diam / 2f))
    }
}

/**
 * Interruptor-escena para el modo claro/oscuro: de día, cielo celeste con un sol y nubes; de noche, cielo
 * profundo con estrellas y una luna. Al cambiar, el sol cruza y se vuelve luna mientras el cielo se funde.
 */
@Composable
internal fun InterruptorDiaNoche(noche: Boolean) {
    val esc = LocalEscala.current
    val mov = ajusteMovimiento()
    val f = remember { Animatable(if (noche) 1f else 0f) }
    LaunchedEffect(noche, mov.activo) {
        if (!mov.activo) f.snapTo(if (noche) 1f else 0f)
        else f.animateTo(if (noche) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow / mov.duracion.coerceAtLeast(0.3f)))
    }
    Canvas(Modifier.size(esc.d(58), esc.d(28))) { escenaDiaNoche(f.value.coerceIn(0f, 1f)) }
}

private fun DrawScope.escenaDiaNoche(n: Float) {
    val r = size.height / 2f
    val cielo = Brush.verticalGradient(listOf(
        lerp(Color(0xFF7CC8FF), Color(0xFF1B1B4B), n),
        lerp(Color(0xFFBFE6FF), Color(0xFF3A2D6E), n),
    ))
    drawRoundRect(cielo, cornerRadius = CornerRadius(r))
    // Estrellas que aparecen de a poco.
    val estrellas = listOf(0.18f to 0.3f, 0.3f to 0.65f, 0.42f to 0.25f, 0.12f to 0.7f, 0.5f to 0.55f)
    estrellas.forEachIndexed { i, (ex, ey) ->
        val a = ((n - i * 0.08f) / 0.6f).coerceIn(0f, 1f)
        if (a > 0f) drawCircle(Color.White.copy(alpha = a * 0.9f), (if (i % 2 == 0) 1.1f else 0.8f) * density, Offset(size.width * ex, size.height * ey))
    }
    // Nubes que se van al anochecer.
    val nube = (1f - n * 1.6f).coerceIn(0f, 1f)
    if (nube > 0f) {
        val c = Color.White.copy(alpha = 0.85f * nube)
        val y = size.height * 0.68f
        val x = size.width * 0.62f + n * size.width * 0.3f
        drawCircle(c, size.height * 0.16f, Offset(x, y)); drawCircle(c, size.height * 0.21f, Offset(x + size.height * 0.2f, y - size.height * 0.06f))
        drawCircle(c, size.height * 0.15f, Offset(x + size.height * 0.42f, y))
    }
    // El astro: sol a la izquierda, luna a la derecha.
    val m = 3f * density
    val d = size.height - 2 * m
    val cx = m + d / 2f + (size.width - 2 * m - d) * n
    val cy = size.height / 2f
    val astro = lerp(Color(0xFFFFD54A), Color(0xFFF1EEDC), n)
    drawCircle(astro.copy(alpha = 0.25f * (1f - n)), d / 2f + 3f * density, Offset(cx, cy))
    drawCircle(astro, d / 2f, Offset(cx, cy))
    // Cráteres de la luna.
    if (n > 0.4f) {
        val a = ((n - 0.4f) / 0.6f) * 0.35f
        val cr = Color(0xFFB8B3A0).copy(alpha = a)
        drawCircle(cr, d * 0.12f, Offset(cx - d * 0.15f, cy - d * 0.12f))
        drawCircle(cr, d * 0.08f, Offset(cx + d * 0.16f, cy + d * 0.1f))
        drawCircle(cr, d * 0.06f, Offset(cx - d * 0.05f, cy + d * 0.2f))
    }
}

package cl.nikalbazu.diptico.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalMovimiento = staticCompositionLocalOf { true }

/**
 * Cuánto y qué tan rápido se mueve la interfaz (Menú › Apariencia › Animaciones y Velocidad).
 * [intensidad]: 0 = sin animaciones, 0.6 suaves, 1 normales, 1.5 intensas. [duracion]: multiplica los tiempos
 * (más grande = más lento).
 */
internal data class AjusteMovimiento(val intensidad: Float = 1f, val duracion: Float = 1f) {
    val activo get() = intensidad > 0f
    fun ms(base: Int): Int = if (!activo) 0 else (base * duracion * RAPIDEZ).toInt()
}

/** Todas las transiciones van un 35 % más cortas: se sentían lentas en la Thor. */
private const val RAPIDEZ = 0.65f

internal val LocalAjusteMovimiento = staticCompositionLocalOf { AjusteMovimiento() }

/** Curva «Díptico»: arranca rápido y se posa suave, como algo que llega flotando. */
internal val CurvaPosarse = CubicBezierEasing(0.2f, 0.9f, 0.2f, 1f)

@Composable
internal fun duracionMovimiento(ms: Int): Int =
    if (LocalMovimiento.current) LocalAjusteMovimiento.current.ms(ms) else 0

/** El ajuste vigente, ya considerando si los efectos están apagados (ahorro o movimiento reducido). */
@Composable
internal fun ajusteMovimiento(): AjusteMovimiento =
    if (LocalMovimiento.current) LocalAjusteMovimiento.current else AjusteMovimiento(0f, 1f)

/** Entrada de una ventana o panel completo: sube un poco, crece apenas y aparece, con un leve rebote si es intenso. */
internal fun entradaVentana(m: AjusteMovimiento): EnterTransition {
    if (!m.activo) return EnterTransition.None
    val rebote = if (m.intensidad > 1.2f) Spring.DampingRatioLowBouncy else Spring.DampingRatioNoBouncy
    return fadeIn(tween(m.ms(220))) +
        slideInVertically(spring(dampingRatio = rebote, stiffness = Spring.StiffnessMediumLow / m.duracion.coerceAtLeast(0.3f))) { (it * 0.07f * m.intensidad).toInt() } +
        scaleIn(tween(m.ms(320), easing = CurvaPosarse), initialScale = 1f - 0.05f * m.intensidad)
}

internal fun salidaVentana(m: AjusteMovimiento): ExitTransition {
    if (!m.activo) return ExitTransition.None
    return fadeOut(tween(m.ms(160))) + scaleOut(tween(m.ms(200)), targetScale = 1f - 0.02f * m.intensidad)
}

/** Entrada de algo flotante (una ventana chica, un desplegable): crece desde casi su tamaño. */
internal fun entradaFlotante(m: AjusteMovimiento): EnterTransition {
    if (!m.activo) return EnterTransition.None
    val rebote = if (m.intensidad > 1.2f) Spring.DampingRatioMediumBouncy else Spring.DampingRatioLowBouncy
    return fadeIn(tween(m.ms(160))) + scaleIn(spring(dampingRatio = rebote, stiffness = Spring.StiffnessMedium / m.duracion.coerceAtLeast(0.3f)), initialScale = 1f - 0.08f * m.intensidad)
}

internal fun salidaFlotante(m: AjusteMovimiento): ExitTransition {
    if (!m.activo) return ExitTransition.None
    return fadeOut(tween(m.ms(130))) + scaleOut(tween(m.ms(150)), targetScale = 1f - 0.03f * m.intensidad)
}

/** Desplazamiento vertical de una fila que entra escalonada (la fila [i] de una lista). */
internal fun retrasoEscalonado(m: AjusteMovimiento, i: Int): Int = if (!m.activo || i > 9) 0 else m.ms(28 * i)

/** Una carpeta que se abre: crece desde más chica con un resorte, como si saliera de su ícono. */
internal fun entradaCarpeta(m: AjusteMovimiento): EnterTransition {
    if (!m.activo) return EnterTransition.None
    val rebote = if (m.intensidad > 1.2f) Spring.DampingRatioLowBouncy else 0.85f
    return fadeIn(tween(m.ms(200))) +
        scaleIn(spring(dampingRatio = rebote, stiffness = Spring.StiffnessMediumLow / m.duracion.coerceAtLeast(0.3f)), initialScale = 1f - 0.12f * m.intensidad.coerceAtMost(1.5f)) +
        slideInVertically(tween(m.ms(300), easing = CurvaPosarse)) { (it * 0.04f * m.intensidad).toInt() }
}

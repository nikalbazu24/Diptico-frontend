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
import kotlin.math.roundToInt

internal val LocalMovimiento = staticCompositionLocalOf { true }

/**
 * Política compartida para el movimiento de la interfaz (Ajustes › Apariencia › Animaciones y Velocidad).
 * «Suaves» mantiene el movimiento con menos desplazamiento; «Normales» usa el movimiento habitual;
 * «Intensas» exagera un poco la entrada. La duración se controla por separado.
 */
internal data class AjusteMovimiento(val intensidad: Float = 1f, val duracion: Float = 1f) {
    val activo get() = intensidad > 0f

    /** Duración consistente, sin convertir animaciones breves activas en duración cero. */
    fun ms(base: Int): Int = if (!activo) 0 else (base * duracion).roundToInt().coerceAtLeast(1)

    /** Desplazamiento sutil escalado con el ajuste general de intensidad. */
    fun distancia(dp: Float): Float = dp * intensidad.coerceIn(0f, 1.5f)
}

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

/** Entrada de una ventana o panel completo: movimiento corto y controlable desde Ajustes. */
internal fun entradaVentana(m: AjusteMovimiento): EnterTransition {
    if (!m.activo) return EnterTransition.None
    val rebote = if (m.intensidad > 1.2f) Spring.DampingRatioLowBouncy else Spring.DampingRatioNoBouncy
    val desplazamiento = m.distancia(0.07f)
    return fadeIn(tween(m.ms(220))) +
        slideInVertically(spring(dampingRatio = rebote, stiffness = Spring.StiffnessMediumLow / m.duracion.coerceAtLeast(0.3f))) { (it * desplazamiento).toInt() } +
        scaleIn(tween(m.ms(320), easing = CurvaPosarse), initialScale = 1f - 0.05f * m.intensidad)
}

internal fun salidaVentana(m: AjusteMovimiento): ExitTransition {
    if (!m.activo) return ExitTransition.None
    return fadeOut(tween(m.ms(160))) + scaleOut(tween(m.ms(200)), targetScale = 1f - 0.02f * m.intensidad)
}

/** Entrada sutil de un elemento flotante, también regulada por los ajustes globales. */
internal fun entradaFlotante(m: AjusteMovimiento): EnterTransition {
    if (!m.activo) return EnterTransition.None
    val rebote = if (m.intensidad > 1.2f) Spring.DampingRatioMediumBouncy else Spring.DampingRatioLowBouncy
    return fadeIn(tween(m.ms(160))) + scaleIn(
        spring(dampingRatio = rebote, stiffness = Spring.StiffnessMedium / m.duracion.coerceAtLeast(0.3f)),
        initialScale = 1f - 0.08f * m.intensidad,
    )
}

internal fun salidaFlotante(m: AjusteMovimiento): ExitTransition {
    if (!m.activo) return ExitTransition.None
    return fadeOut(tween(m.ms(130))) + scaleOut(tween(m.ms(150)), targetScale = 1f - 0.03f * m.intensidad)
}

/** Una carpeta que se abre: crece desde su ícono con el mismo ajuste global. */
internal fun entradaCarpeta(m: AjusteMovimiento): EnterTransition {
    if (!m.activo) return EnterTransition.None
    val rebote = if (m.intensidad > 1.2f) Spring.DampingRatioLowBouncy else 0.85f
    return fadeIn(tween(m.ms(200))) +
        scaleIn(spring(dampingRatio = rebote, stiffness = Spring.StiffnessMediumLow / m.duracion.coerceAtLeast(0.3f)), initialScale = 1f - 0.12f * m.intensidad.coerceAtMost(1.5f)) +
        slideInVertically(tween(m.ms(300), easing = CurvaPosarse)) { (it * m.distancia(0.04f)).toInt() }
}

internal fun salidaCarpeta(m: AjusteMovimiento): ExitTransition {
    if (!m.activo) return ExitTransition.None
    return fadeOut(tween(m.ms(160))) + scaleOut(tween(m.ms(220)), targetScale = 1f + 0.08f * m.intensidad)
}

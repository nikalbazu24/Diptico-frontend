package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Transición al intercambiar las pantallas: lo de arriba baja y cruza la bisagra,
 * y lo de abajo sube a ocupar su lugar. Cada pantalla anima su mitad del viaje.
 * [esArriba]: esta es la pantalla de arriba (las cosas salen y entran por su borde inferior).
 */
@Composable
fun Intercambio(intercambiado: Boolean, esArriba: Boolean, contenido: @Composable (Boolean) -> Unit) {
    val lado = if (esArriba) 1 else -1
    AnimatedContent(
        targetState = intercambiado,
        transitionSpec = {
            val entra = slideInVertically(tween(560, delayMillis = 120, easing = FastOutSlowInEasing)) { lado * it } +
                fadeIn(tween(380, delayMillis = 120)) +
                scaleIn(tween(560, delayMillis = 120, easing = FastOutSlowInEasing), initialScale = 0.94f)
            val sale = slideOutVertically(tween(480, easing = FastOutSlowInEasing)) { lado * it } +
                fadeOut(tween(420)) +
                scaleOut(tween(480), targetScale = 0.88f)
            (entra togetherWith sale).apply { targetContentZIndex = 1f }
        },
        label = "intercambio",
        modifier = Modifier.fillMaxSize(),
    ) { inter -> contenido(inter) }
}

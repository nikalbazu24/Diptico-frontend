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

/** Transición regulada por los ajustes globales al intercambiar las pantallas. */
@Composable
fun Intercambio(intercambiado: Boolean, esArriba: Boolean, contenido: @Composable (Boolean) -> Unit) {
    val lado = if (esArriba) 1 else -1
    val mov = ajusteMovimiento()
    AnimatedContent(
        targetState = intercambiado,
        transitionSpec = {
            if (!mov.activo) {
                androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
            } else {
                val distancia = mov.distancia(0.07f)
                val entra = slideInVertically(tween(mov.ms(300), delayMillis = mov.ms(80), easing = FastOutSlowInEasing)) { (lado * it * distancia).toInt() } +
                    fadeIn(tween(mov.ms(240), delayMillis = mov.ms(80))) +
                    scaleIn(tween(mov.ms(300), delayMillis = mov.ms(80), easing = FastOutSlowInEasing), initialScale = 1f - 0.035f * mov.intensidad)
                val sale = slideOutVertically(tween(mov.ms(250), easing = FastOutSlowInEasing)) { (lado * it * distancia).toInt() } +
                    fadeOut(tween(mov.ms(220))) +
                    scaleOut(tween(mov.ms(250)), targetScale = 1f - 0.05f * mov.intensidad)
                (entra togetherWith sale).apply { targetContentZIndex = 1f }
            }
        },
        label = "intercambio",
        modifier = Modifier.fillMaxSize(),
    ) { inter -> contenido(inter) }
}

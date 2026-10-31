package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import cl.nikalbazu.diptico.estado.Estado

/**
 * Lo que muestra una pantalla: el tutorial si falta configurar, o el inicio que le toca.
 * Encima van el reloj grande y la animación de entrada.
 */
@Composable
fun Raiz(e: Estado, rol: Rol, miPantalla: Int, rolIntro: Rol = rol) {
    CompositionLocalProvider(
        LocalIconos provides IconosCfg(e.estiloIconos, e.iconosPropios, e.iconosVersion),
        LocalFormas provides e.formas,
        LocalTamJuegos provides e.tamJuegos,
    ) {
        Box(Modifier.fillMaxSize()) {
            when {
                !e.configurado -> if (rol == Rol.INFERIOR) TutorialAbajo(e) else Tutorial(e)
                rol == Rol.UNICA -> PantallaUnica(e)
                rol == Rol.SUPERIOR -> PantallaSuperior(e)
                else -> PantallaInferior(e, miPantalla)
            }
            AnimatedVisibility(
                visible = e.relojGrande && rol != Rol.INFERIOR,
                enter = fadeIn(tween(250)) + scaleIn(tween(320), initialScale = 0.9f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 1.05f),
            ) { RelojGrande(e) }
            // En modo una pantalla, la de abajo no muestra la animación.
            val sinIntro = rol == Rol.INFERIOR && rolIntro == Rol.INFERIOR && e.unaPantalla
            if (e.intro && !sinIntro) Intro(rolIntro)
        }
    }
}

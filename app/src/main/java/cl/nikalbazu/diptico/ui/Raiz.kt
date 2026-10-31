package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cl.nikalbazu.diptico.estado.Estado

/**
 * Lo que muestra una pantalla: el tutorial si falta configurar, o el inicio que le toca.
 * La animación de entrada va encima de todo hasta que termina.
 */
@Composable
fun Raiz(e: Estado, rol: Rol, miPantalla: Int, rolIntro: Rol = rol) {
    Box(Modifier.fillMaxSize()) {
        when {
            !e.configurado -> if (rol == Rol.INFERIOR) TutorialAbajo(e) else Tutorial(e)
            rol == Rol.UNICA -> PantallaUnica(e)
            rol == Rol.SUPERIOR -> PantallaSuperior(e)
            else -> PantallaInferior(e, miPantalla)
        }
        // En modo una pantalla, la de abajo no muestra la animación.
        val sinIntro = rol == Rol.INFERIOR && rolIntro == Rol.INFERIOR && e.unaPantalla
        if (e.intro && !sinIntro) Intro(rolIntro)
    }
}

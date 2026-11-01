package cl.nikalbazu.diptico.ui

import cl.nikalbazu.diptico.estado.Panel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.blur
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateDpAsState
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
        LocalFormas provides remember(e.formas, e.formaTodos, e.consolaSel) { cl.nikalbazu.diptico.estado.Diptico.formasVisibles(e) },
        LocalHeroes provides (e.heroes to e.imagenHeroe),
        LocalEncuadres provides cl.nikalbazu.diptico.estado.Diptico.encuadresVisibles(e),
        LocalZoomLento provides e.zoomLento,
        LocalTamJuegos provides e.tamJuegos,
    ) {
        // El centro de control vive siempre en la pantalla de arriba (la principal), aunque se intercambien.
        val arribaFisica = miPantalla == android.view.Display.DEFAULT_DISPLAY &&
            (rol == Rol.UNICA || rol == (if (e.intercambiar) Rol.INFERIOR else Rol.SUPERIOR))
        val centroAqui = e.panel == Panel.CENTRO && arribaFisica
        // Desenfoque detrás de lo que se abre encima: el centro de control, la bienvenida, el reloj grande o la continuación.
        val encima = centroAqui || e.donde != null || e.quedaste || e.marcandoMapa != null || (e.novedades >= 0 && !e.intro) || (e.bienvenida > 0L && !e.intro) || e.relojGrande || (e.continuacion > 0L && e.sesion?.let { miPantalla != it.displayId } == true)
        val desenfoque by animateDpAsState(if (encima && Estilo.efectosCompletos) 18.dp else 0.dp, tween(300), label = "desenfoque")
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().then(if (desenfoque > 0.dp) Modifier.blur(desenfoque) else Modifier)) {
                when {
                    !e.configurado -> if (rol == Rol.INFERIOR) TutorialAbajo(e) else Tutorial(e)
                    rol == Rol.UNICA -> PantallaUnica(e)
                    rol == Rol.SUPERIOR -> PantallaSuperior(e)
                    else -> PantallaInferior(e, miPantalla)
                }
            }
            AnimatedVisibility(visible = centroAqui, enter = fadeIn(tween(160)), exit = fadeOut(tween(200))) { PanelCentro(e) }
            // Mientras juegas: Bisa se abre en la pantalla libre (la que no tiene el juego).
            val s = e.sesion
            if (s != null && e.panel == Panel.ASISTENTE && !s.dosPantallas && miPantalla != s.displayId) PanelAsistente(e)
            AnimatedVisibility(
                visible = e.relojGrande && rol != Rol.INFERIOR,
                enter = fadeIn(tween(250)) + scaleIn(tween(320), initialScale = 0.9f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 1.05f),
            ) { RelojGrande(e) }
            // Teclado de Díptico: en la pantalla contraria a la del texto.
            AnimatedVisibility(
                visible = e.conTeclado && (rol == Rol.SUPERIOR || rol == Rol.UNICA),
                enter = fadeIn(tween(200)) + androidx.compose.animation.slideInVertically(tween(300)) { it / 3 },
                exit = fadeOut(tween(160)),
            ) { TecladoDiptico(e) }
            if (e.protector && !(rol == Rol.INFERIOR && e.unaPantalla)) Protector(e, rol)
            // Bienvenida al encender (y el recordatorio si estabas jugando), encima de todo menos la animación de entrada.
            AnimatedVisibility(
                visible = e.bienvenida > 0L && !e.intro && !(rol == Rol.INFERIOR && e.unaPantalla),
                enter = fadeIn(tween(380)), exit = fadeOut(tween(420)),
            ) { Bienvenida(e, rol) }
            // ¿En qué pantalla lo abro?: la ventana va donde está el estante (abajo); arriba, un marco si se abrirá ahí.
            if (e.donde != null) {
                val fisicaArriba = miPantalla == android.view.Display.DEFAULT_DISPLAY
                if (rol != Rol.UNICA) MarcoDonde(if (fisicaArriba) e.dondeSel == 0 else e.dondeSel == 1)
                if (rol == Rol.UNICA || rol == Rol.INFERIOR) VentanaDonde(e)
            }
            // ¿Dónde quedaste? y el mapa: en la pantalla del estante (la de abajo, o la única).
            val delEstante = rol == Rol.UNICA || rol == Rol.INFERIOR
            if (delEstante && e.quedaste && e.marcandoMapa == null) TarjetaQuedaste(e)
            if (delEstante && e.marcandoMapa != null) MarcarMapa(e)
            // Tour de novedades (una vez por versión): en la pantalla principal; la otra acompaña.
            AnimatedVisibility(
                visible = e.novedades >= 0 && !e.intro,
                enter = fadeIn(tween(300)) + scaleIn(tween(360), initialScale = 0.95f), exit = fadeOut(tween(260)),
            ) { if (rol == Rol.INFERIOR) NovedadesCompania(e) else TourNovedades(e) }
            val sc = e.sesion
            AnimatedVisibility(
                visible = e.continuacion > 0L && sc != null && !sc.dosPantallas && miPantalla != sc.displayId && e.datos[sc.clave]?.lupa != true,
                enter = fadeIn(tween(300)) + scaleIn(tween(360), initialScale = 0.94f), exit = fadeOut(tween(400)),
            ) { Continuacion(e) }
            // En modo una pantalla, la de abajo no muestra la animación.
            val sinIntro = rol == Rol.INFERIOR && rolIntro == Rol.INFERIOR && e.unaPantalla
            if (e.intro && !sinIntro) Intro(rolIntro)
        }
    }
}

package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import kotlin.math.absoluteValue

/**
 * La pantalla de arriba como dos páginas seguidas: la portada del juego y, deslizando hacia el lado, tus widgets.
 * Ya no hace falta un botón para cambiar: se desliza con el dedo (y el botón o el atajo de siempre siguen sirviendo,
 * moviendo la página con la misma animación). Los widgets tienen sus propias páginas: al llegar a la primera,
 * seguir deslizando vuelve a la portada.
 */
@Composable
fun InicioDeslizable(e: Estado, modifier: Modifier = Modifier, portada: @Composable () -> Unit, widgets: @Composable () -> Unit) {
    val esc = LocalEscala.current
    val pager = rememberPagerState(initialPage = if (e.inicio == "widgets") 1 else 0) { 2 }
    // El botón, el atajo o DIPPO cambiaron el inicio: la página se mueve sola.
    LaunchedEffect(e.inicio) {
        val objetivo = if (e.inicio == "widgets") 1 else 0
        if (pager.currentPage != objetivo && !pager.isScrollInProgress) pager.animateScrollToPage(objetivo)
    }
    // Deslizaste con el dedo: se guarda en qué página quedaste.
    LaunchedEffect(pager.settledPage) {
        Diptico.fijarInicio(if (pager.settledPage == 1) "widgets" else "heroe")
    }
    Box(modifier.fillMaxSize().testTag("inicio-deslizable")) {
        HorizontalPager(
            state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 0,
            userScrollEnabled = !e.editandoWidgets && e.sesion == null,
        ) { pagina ->
            // Al deslizar, la página que se va se achica y oscurece apenas: se siente como pasar de una tarjeta a otra.
            Box(Modifier.fillMaxSize().graphicsLayer {
                val d = ((pager.currentPage - pagina) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                val s = 1f - 0.06f * d
                scaleX = s; scaleY = s
                alpha = 1f - 0.4f * d
            }) { if (pagina == 0) portada() else widgets() }
        }
        // Dos puntitos abajo: dónde estás (se tocan para cambiar).
        if (!e.editandoWidgets) Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(6)),
            horizontalArrangement = Arrangement.spacedBy(esc.d(6)), verticalAlignment = Alignment.CenterVertically,
        ) {
            for (k in 0..1) {
                val activa = (pager.currentPage + pager.currentPageOffsetFraction).let { if (k == 0) it < 0.5f else it >= 0.5f }
                Box(
                    Modifier.size(if (activa) esc.d(18) else esc.d(6), esc.d(6)).clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = if (activa) 0.85f else 0.35f))
                        .clickable { Diptico.fijarInicio(if (k == 1) "widgets" else "heroe") },
                )
            }
        }
    }
}

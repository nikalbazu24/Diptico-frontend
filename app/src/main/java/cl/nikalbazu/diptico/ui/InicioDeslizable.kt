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
        if (!e.editandoWidgets) {
            val enWidgets = pager.currentPage + pager.currentPageOffsetFraction >= 0.5f
            // Indicador con nombre abajo: «Portada · Widgets» (se toca para cambiar).
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(6)).clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = esc.d(4), vertical = esc.d(3)),
                horizontalArrangement = Arrangement.spacedBy(esc.d(2)), verticalAlignment = Alignment.CenterVertically,
            ) {
                for ((k, nombre) in listOf("Portada", "Widgets").withIndex()) {
                    val activa = if (k == 0) !enWidgets else enWidgets
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(if (activa) Color.White.copy(alpha = 0.9f) else Color.Transparent)
                            .clickable { Diptico.fijarInicio(if (k == 1) "widgets" else "heroe") }
                            .padding(horizontal = esc.d(10), vertical = esc.d(3)),
                    ) { Texto(nombre, 11f, color = if (activa) Color(0xFF14111D) else Color.White.copy(alpha = 0.8f), lineas = 1) }
                }
            }
            // Una pestaña al costado: tocarla (o deslizar desde el centro de la pantalla) lleva a la otra página.
            // Ojo: deslizar desde el mismo borde lo toma Android como «volver».
            Box(
                Modifier.align(if (enWidgets) Alignment.CenterStart else Alignment.CenterEnd)
                    .clip(if (enWidgets) RoundedCornerShape(topEnd = esc.d(14), bottomEnd = esc.d(14)) else RoundedCornerShape(topStart = esc.d(14), bottomStart = esc.d(14)))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable { Diptico.fijarInicio(if (enWidgets) "heroe" else "widgets") }
                    .padding(horizontal = esc.d(6), vertical = esc.d(14)),
            ) { Texto(if (enWidgets) "‹" else "›", 18f, color = Color.White.copy(alpha = 0.85f), lineas = 1, traducir = false) }
        }
    }
}

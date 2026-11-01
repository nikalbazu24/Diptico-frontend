package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado

/**
 * Vista "Lomos": tus juegos como una repisa de lomos (como libros o vinilos), cada uno con el color de su consola
 * y el título en vertical. El elegido se asoma y se abre mostrando su portada.
 * Es liviana: solo el juego elegido carga su imagen; los demás son color y texto.
 */
@Composable
fun VistaLomos(e: Estado, modifier: Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val lista = e.visibles
    val sel = e.indiceSel
    val estado = rememberLazyListState()
    LaunchedEffect(sel, lista.size) {
        if (lista.isEmpty()) return@LaunchedEffect
        // El elegido queda un poco a la izquierda del centro, para ver lo que viene.
        val vis = estado.layoutInfo.visibleItemsInfo
        if (vis.isEmpty()) estado.scrollToItem((sel - 3).coerceAtLeast(0))
        else estado.animateScrollToItem((sel - 3).coerceAtLeast(0))
    }
    BoxWithConstraints(modifier) {
        val altoU = maxHeight.value / esc.factor
        val altoLomo = (altoU - 92f).coerceIn(150f, 280f)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Box(Modifier.fillMaxWidth().height(esc.d(altoLomo + 10))) {
                LazyRow(
                    state = estado,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = esc.d(20)),
                    horizontalArrangement = Arrangement.spacedBy(esc.d(3)),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    itemsIndexed(lista, key = { _, j -> j.clave }) { i, j ->
                        Lomo(j, i == sel, altoLomo, alPulsar = { if (i == sel) Diptico.jugar() else Diptico.elegirJuego(j.clave) })
                    }
                }
            }
            // La repisa.
            Box(
                Modifier.fillMaxWidth().padding(horizontal = esc.d(10)).height(esc.d(10)).clip(androidx.compose.foundation.shape.RoundedCornerShape(esc.d(4)))
                    .background(Brush.verticalGradient(listOf(lerp(p.superficie, Color.White, 0.08f), p.superficie.copy(alpha = 0.5f)))),
            )
            Spacer(Modifier.height(esc.d(10)))
            // Lo del elegido, abajo de la repisa.
            val j = e.seleccionado
            Row(Modifier.fillMaxWidth().padding(horizontal = esc.d(22)), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Texto(j?.titulo ?: "", 17f, lineas = 1, traducir = false)
                    val c = j?.let { Consolas.porId(it.consolaId) }
                    val t = j?.let { e.datos[it.clave]?.tiempo } ?: 0L
                    Detalle(listOfNotNull(c?.bonito, if (t > 0) formatoTiempo(t) else null, "${sel + 1} de ${lista.size}").joinToString(" · "), 12f, lineas = 1)
                }
                Pista("A", "Jugar")
            }
        }
    }
}

@Composable
private fun Lomo(j: cl.nikalbazu.diptico.data.Juego, elegido: Boolean, altoLomo: Float, alPulsar: () -> Unit) {
    val esc = LocalEscala.current
    val c = Consolas.porId(j.consolaId)
    val color = c?.color ?: colorDe(j.clave)
    // Cada lomo tiene su propia altura (como libros reales), siempre la misma para el mismo juego.
    val alturaPropia = remember(j.clave) { 0.82f + (Math.floorMod(j.clave.hashCode(), 19) / 19f) * 0.18f }
    val ancho by animateDpAsState(esc.d(if (elegido) altoLomo * 0.72f else 40f), spring(dampingRatio = 0.72f, stiffness = 380f), label = "ancho")
    val alto by animateDpAsState(esc.d(if (elegido) altoLomo else altoLomo * alturaPropia), spring(dampingRatio = 0.72f, stiffness = 380f), label = "alto")
    val asoma by animateFloatAsState(if (elegido) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "asoma")
    val forma = androidx.compose.foundation.shape.RoundedCornerShape(esc.d(if (elegido) 10f else 5f))
    Box(
        Modifier.width(ancho).height(alto)
            .graphicsLayer { translationY = -asoma * 8f * density }
            .clip(forma)
            .background(Brush.horizontalGradient(listOf(lerp(color, Color.Black, 0.25f), color, lerp(color, Color.Black, 0.35f))))
            .then(if (elegido) Modifier.border(esc.d(2.5f), Color.White, forma) else Modifier)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { alPulsar() },
    ) {
        if (elegido) {
            // Se abre: la portada aparece al centro, encuadrada en la forma del lomo abierto.
            ArteJuego(j, preferirArte = false, maxLado = 420, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = asoma }, vivo = true)
        } else {
            // Bandas del lomo y el título en vertical.
            Box(Modifier.fillMaxWidth().height(esc.d(14)).align(Alignment.TopCenter).background(Color.White.copy(alpha = 0.18f)))
            Box(Modifier.fillMaxWidth().height(esc.d(4)).align(Alignment.BottomCenter).padding(bottom = esc.d(0)).background(Color.Black.copy(alpha = 0.25f)))
            Box(Modifier.fillMaxSize().padding(top = esc.d(18), bottom = esc.d(34)), contentAlignment = Alignment.Center) {
                Texto(
                    j.titulo, 12.5f, color = Color.White, lineas = 1, traducir = false,
                    modifier = Modifier.requiredWidth(esc.d(altoLomo * alturaPropia - 60f)).graphicsLayer { rotationZ = -90f },
                )
            }
            c?.let {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(8)).size(esc.d(22)), contentAlignment = Alignment.Center) {
                    IconoConsola(it, 20f, color)
                }
            }
        }
    }
}

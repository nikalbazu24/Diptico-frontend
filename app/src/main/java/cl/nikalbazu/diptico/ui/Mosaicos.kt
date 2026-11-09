package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Teclas

/**
 * Vista «Mosaicos» (estilo Xbox): el primer juego en un mosaico grande que ocupa dos filas y el resto en
 * mosaicos cuadrados de a dos por columna, que corren hacia el lado. El elegido lleva borde blanco y un halo
 * del color de su consola, para que siempre se note dónde estás. Liviana: sin vidrio ni sombras animadas.
 */
@Composable
fun Mosaicos(e: Estado, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val lista = e.visibles
    val estado = rememberLazyGridState()
    val margen = with(androidx.compose.ui.platform.LocalDensity.current) { esc.d(40).roundToPx() }
    SideEffect { Teclas.columnasCuadricula = 2 }
    LaunchedEffect(e.indiceSel, lista.size) {
        if (lista.isNotEmpty()) estado.animateScrollToItem(e.indiceSel.coerceIn(0, lista.lastIndex), -margen)
    }
    BoxWithConstraints(modifier.testTag("vista-mosaicos")) {
        if (lista.isEmpty()) { VacioEstante(e); return@BoxWithConstraints }
        val hueco = esc.d(10)
        val alto = maxHeight - esc.d(24)
        val lado: Dp = ((alto - hueco) / 2).coerceAtLeast(esc.d(60))
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2), state = estado, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = esc.d(18), vertical = esc.d(12)),
            horizontalArrangement = Arrangement.spacedBy(hueco), verticalArrangement = Arrangement.spacedBy(hueco),
        ) {
            items(lista.size, key = { lista[it].clave }, span = { i -> GridItemSpan(if (i == 0) maxLineSpan else 1) }) { i ->
                val grande = i == 0
                Mosaico(e, i, grande, if (grande) lado * 2 + hueco else lado)
            }
        }
    }
}

@Composable
private fun Mosaico(e: Estado, i: Int, grande: Boolean, lado: Dp) {
    val esc = LocalEscala.current
    val j = e.visibles[i]
    val elegido = i == e.indiceSel
    val color = Consolas.porId(j.consolaId)?.color ?: Uva
    val escala by animateFloatAsState(if (elegido) 1.04f else 1f, tween(duracionMovimiento(160)), label = "mosaico")
    val forma = RoundedCornerShape(esc.d(if (grande) 14 else 10))
    val jugado = (e.datos[j.clave]?.tiempo?.toLong() ?: 0L) > 0L
    Box(
        Modifier.size(lado).graphicsLayer { scaleX = escala; scaleY = escala }
            .then(if (elegido) Modifier.shadow(esc.d(18), forma, ambientColor = color, spotColor = color) else Modifier)
            .clip(forma)
            .background(color.copy(alpha = 0.25f))
            .then(if (elegido) Modifier.border(esc.d(3), Color.White, forma) else Modifier)
            .clickable { if (elegido) Diptico.jugar() else Diptico.elegirJuego(j.clave) },
    ) {
        ArteJuego(j, preferirArte = grande, maxLado = if (grande) 640 else 320, modifier = Modifier.fillMaxSize(), imagen = if (grande) "arte" else "portada")
        if (!elegido && !grande) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
                .padding(horizontal = esc.d(if (grande) 12 else 7), vertical = esc.d(if (grande) 10 else 5)),
        ) {
            if (grande && jugado) Texto("Continuar", 10.5f, color = Color.White.copy(alpha = 0.75f), lineas = 1)
            Texto(j.titulo, if (grande) 15f else 10.5f, color = Color.White, lineas = if (grande) 2 else 1, traducir = false)
        }
    }
}

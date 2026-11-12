package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/** Centra el conjunto y cada fila incompleta, conservando el desplazamiento al usar el mando. */
@Composable
internal fun RejillaCentrada(
    cantidad: Int, columnas: Int, seleccion: Int, separacionX: Dp, separacionY: Dp,
    relleno: PaddingValues, modifier: Modifier = Modifier, interactiva: Boolean = true,
    horizontal: Boolean = false, clave: (Int) -> String,
    estado: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    celda: @Composable (Int) -> Unit,
) {
    val carriles = columnas.coerceAtLeast(1)
    val filas = (0 until cantidad).toList().chunked(carriles)
    LaunchedEffect(seleccion, carriles, cantidad) {
        if (cantidad > 0) estado.mostrar(seleccion.coerceIn(0, cantidad - 1) / carriles)
    }
    if (horizontal) LazyRow(state = estado, modifier = modifier, userScrollEnabled = interactiva,
        contentPadding = relleno, horizontalArrangement = Arrangement.spacedBy(separacionX, Alignment.CenterHorizontally)) {
        items(filas, key = { clave(it.first()) }) { fila ->
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(separacionY, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally) { fila.forEach { i -> celda(i) } }
        }
    } else LazyColumn(state = estado, modifier = modifier, userScrollEnabled = interactiva,
        contentPadding = relleno, verticalArrangement = Arrangement.spacedBy(separacionY, Alignment.CenterVertically)) {
        items(filas, key = { clave(it.first()) }) { fila ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(separacionX, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically) {
                fila.forEach { i -> celda(i) }
            }
        }
    }
}

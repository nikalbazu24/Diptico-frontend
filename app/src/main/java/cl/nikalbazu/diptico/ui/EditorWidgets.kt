@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Widget
import cl.nikalbazu.diptico.estado.Widgets
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class MedidasEditorWidgets(
    val ancho: Float,
    val alto: Float,
    val margen: Float,
    val espacio: Float,
    val columnas: Int,
    val filas: Int,
    val manija: Float,
) {
    val hueco = espacio.coerceAtMost(minOf((ancho - 2 * margen) / columnas, (alto - 2 * margen) / filas).coerceAtLeast(0f) / 2f)
    val celdaAn = ((ancho - 2 * margen - hueco * (columnas - 1)) / columnas).coerceAtLeast(1f)
    val celdaAl = ((alto - 2 * margen - hueco * (filas - 1)) / filas).coerceAtLeast(1f)
    val pasoX = celdaAn + hueco
    val pasoY = celdaAl + hueco

    fun ancho(celdas: Int) = pasoX * celdas - hueco
    fun alto(celdas: Int) = pasoY * celdas - hueco

    fun rect(w: Widget): Rect {
        val x = margen + w.x * pasoX
        val y = margen + w.y * pasoY
        return Rect(x, y, x + ancho(w.an), y + alto(w.al))
    }

    fun celdas(tamano: Offset, w: Widget): Pair<Int, Int> =
        ((tamano.x + hueco) / pasoX).roundToInt().coerceIn(1, (columnas - w.x).coerceAtLeast(1)) to
            ((tamano.y + hueco) / pasoY).roundToInt().coerceIn(1, (filas - w.y).coerceAtLeast(1))
}

internal class ArrastreWidget(val indice: Int, val original: Widget, val estirando: Boolean) {
    var delta by mutableStateOf(Offset.Zero)

    fun tamano(m: MedidasEditorWidgets): Offset = Offset(
        (m.ancho(original.an) + delta.x).coerceIn(m.celdaAn, m.ancho((m.columnas - original.x).coerceAtLeast(1))),
        (m.alto(original.al) + delta.y).coerceIn(m.celdaAl, m.alto((m.filas - original.y).coerceAtLeast(1))),
    )
}

/** El gesto vive fuera del pager: sigue recibiendo el dedo al desmontar la página de origen. */
internal fun Modifier.editarConDedo(
    m: MedidasEditorWidgets,
    bloqueado: Boolean,
    mostrar: (ArrastreWidget?) -> Unit,
): Modifier = if (bloqueado) this else pointerInput(m) {
    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val inicio = down.position
            val primerCambio = awaitTouchSlopOrCancellation(down.id) { cambio, _ -> cambio.consume() }
                ?: return@awaitEachGesture

            val e = Diptico.estado.value
            val i = e.widgets.indices.firstOrNull { k ->
                val w = e.widgets[k]
                w.visible && w.pagina == e.paginaInicio && m.rect(w).contains(inicio)
            }
            val gesto = i?.let {
                val w = e.widgets[it]
                val r = m.rect(w)
                val estirando = it == e.widgetSel &&
                    inicio.x >= r.right - minOf(m.manija, r.width * .6f) &&
                    inicio.y >= r.bottom - minOf(m.manija, r.height * .75f)
                ArrastreWidget(it, w, estirando)
            }
            if (gesto != null) {
                Diptico.elegirWidget(gesto.indice)
                mostrar(gesto)
            }

            var delta = Offset.Zero
            var borde = 0
            var espera: Job? = null

            fun mover(posicion: Offset) {
                delta = posicion - inicio
                if (gesto == null) return
                gesto.delta = delta
                if (gesto.estirando) return

                val zona = m.manija.coerceAtMost(m.ancho / 6)
                val lado = when {
                    posicion.x < zona -> -1
                    posicion.x > m.ancho - zona -> 1
                    else -> 0
                }
                if (lado == borde) return
                espera?.cancel()
                borde = lado
                if (lado != 0) espera = launch {
                    delay(450)
                    Diptico.cambiarPagina(lado)
                }
            }

            try {
                mover(primerCambio.position)
                val completo = drag(primerCambio.id) { cambio ->
                    cambio.consume()
                    mover(cambio.position)
                }
                if (!completo) return@awaitEachGesture
                when {
                    gesto == null -> {
                        if (abs(delta.x) > m.pasoX / 2 && abs(delta.x) > abs(delta.y)) {
                            Diptico.cambiarPagina(if (delta.x < 0) 1 else -1)
                        }
                    }
                    gesto.estirando -> {
                        val (an, al) = m.celdas(gesto.tamano(m), gesto.original)
                        Diptico.redimensionarWidget(gesto.indice, an, al)
                    }
                    else -> {
                        val w = gesto.original
                        Diptico.ponerWidget(
                            gesto.indice, w.x + (delta.x / m.pasoX).roundToInt(),
                            w.y + (delta.y / m.pasoY).roundToInt(), Diptico.estado.value.paginaInicio,
                        )
                    }
                }
            } finally {
                espera?.cancel()
                mostrar(null)
            }
        }
    }
}

@Composable
internal fun FantasmaWidget(arrastre: ArrastreWidget, m: MedidasEditorWidgets) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val r = m.rect(arrastre.original)
    val forma = RoundedCornerShape(esc.d(16))
    val ancho = with(LocalDensity.current) { r.width.toDp() }
    val alto = with(LocalDensity.current) { r.height.toDp() }
    Box(
        Modifier.offset {
            val delta = arrastre.delta
            IntOffset(
                (r.left + delta.x).coerceIn(0f, (m.ancho - r.width).coerceAtLeast(0f)).roundToInt(),
                (r.top + delta.y).coerceIn(0f, (m.alto - r.height).coerceAtLeast(0f)).roundToInt(),
            )
        }.size(ancho, alto).clip(forma).background(p.superficie).border(esc.d(2), Coral, forma),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.padding(esc.d(6)), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(6)),
        ) {
            IconoDiptico(Widgets.icono(arrastre.original.tipo), Coral, 17f)
            Texto(Widgets.nombre(arrastre.original.tipo), 12f, lineas = 1)
        }
    }
}

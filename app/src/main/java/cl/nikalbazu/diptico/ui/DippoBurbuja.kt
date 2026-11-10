package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Los globitos de DIPPO: salen justo arriba de su cabeza, flotando sobre lo que haya detrás, y se sacan deslizándolos
 * (hacia arriba o a los lados) para que no molesten. Se dibujan en la capa de [Raiz], así no empujan ni tapan los accesos de abajo.
 */
object DippoBurbuja {
    /** Un globito: [tipo] 1 saludo, 2 idea, 3 logro. [alToque] se llama al tocarlo; [alCerrar] al deslizarlo para sacarlo. */
    data class Mensaje(val id: Long, val texto: String, val tipo: Int, val alToque: (() -> Unit)?, val alCerrar: () -> Unit)

    var actual by mutableStateOf<Mensaje?>(null)
        private set

    /** Dónde está la cabeza de DIPPO en su ventana (se actualiza solo desde donde esté dibujado). */
    @Volatile var pantalla: Int = 0
    @Volatile var cabeza: Rect = Rect.Zero

    fun mostrar(m: Mensaje) { actual = m }
    fun ocultar() { actual = null }
}

@Composable
fun BurbujaDippoCapa(miPantalla: Int) {
    val m = DippoBurbuja.actual
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val ahi = m != null && DippoBurbuja.pantalla == miPantalla
    // El último texto se conserva mientras el globito se va, para que no parpadee vacío.
    var ultimo by remember { mutableStateOf<DippoBurbuja.Mensaje?>(null) }
    if (m != null) ultimo = m
    val colaX = remember { floatArrayOf(0f) }
    val mostrado = ultimo
    Box(Modifier.fillMaxSize().testTag("burbuja-dippo")) {
        if (mostrado != null) Box(
            Modifier.layout { medible, c ->
                val pl = medible.measure(c.copy(minWidth = 0, minHeight = 0))
                val r = DippoBurbuja.cabeza
                val margen = 8.dp.roundToPx()
                val x = (r.center.x - pl.width / 2f).toInt().coerceIn(margen, (c.maxWidth - pl.width - margen).coerceAtLeast(margen))
                val arriba = r.top - pl.height - 9.dp.roundToPx()
                // Si arriba no hay lugar (DIPPO pegado al borde de arriba), el globito sale por debajo y sin colita.
                val y = if (arriba >= margen) arriba.toInt() else (r.bottom + 6.dp.roundToPx()).toInt()
                colaX[0] = if (arriba >= margen) r.center.x - x else -1f
                layout(c.maxWidth, c.maxHeight) { pl.place(x, y) }
            },
        ) {
            AnimatedVisibility(
                ahi,
                enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.7f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)),
                exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.8f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)),
            ) { Globito(mostrado, colaX, p, esc) }
        }
    }
}

@Composable
private fun Globito(m: DippoBurbuja.Mensaje, colaX: FloatArray, p: Paleta, esc: Escala) {
    val ox = remember(m.id) { Animatable(0f) }
    val oy = remember(m.id) { Animatable(0f) }
    val alcance = rememberCoroutineScope()
    val fondo = when (m.tipo) { 2 -> Coral.copy(alpha = 0.95f); 4 -> Uva.copy(alpha = 0.96f); 3 -> Sol.copy(alpha = 0.97f); else -> p.superficie.copy(alpha = 0.96f) }
    val tinta = when (m.tipo) { 2, 4 -> Color.White; 3 -> Color(0xFF2A1E05); else -> p.tinta }
    Column(
        Modifier
            .graphicsLayer { translationX = ox.value; translationY = oy.value; alpha = (1f - (abs(ox.value) / 260f + (-oy.value).coerceAtLeast(0f) / 160f)).coerceIn(0.1f, 1f) }
            .pointerInput(m.id) {
                detectDragGestures(
                    onDragEnd = {
                        alcance.launch {
                            if (abs(ox.value) > 50f || oy.value < -36f) {
                                // Se va hacia donde lo mandaste.
                                if (abs(ox.value) > -oy.value) ox.animateTo(if (ox.value > 0) 900f else -900f, tween(160))
                                else oy.animateTo(-600f, tween(160))
                                m.alCerrar()
                            } else { ox.animateTo(0f, tween(140)); oy.animateTo(0f, tween(140)) }
                        }
                    },
                    onDragCancel = { alcance.launch { ox.animateTo(0f); oy.animateTo(0f) } },
                ) { cambio, delta ->
                    cambio.consume()
                    alcance.launch { ox.snapTo(ox.value + delta.x); oy.snapTo((oy.value + delta.y).coerceAtMost(12f)) }
                }
            },
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            Modifier.widthIn(max = esc.d(230))
                // La colita apunta a la cabeza de DIPPO; se dibuja bajo el globito, fuera de sus bordes.
                .drawBehind {
                    if (colaX[0] < 0f) return@drawBehind
                    val cx = colaX[0].coerceIn(14.dp.toPx(), (size.width - 14.dp.toPx()).coerceAtLeast(14.dp.toPx()))
                    val w = 7.dp.toPx()
                    drawPath(Path().apply { moveTo(cx - w, size.height - 2f); lineTo(cx + w, size.height - 2f); lineTo(cx, size.height + 7.dp.toPx()); close() }, fondo)
                }
                .clip(RoundedCornerShape(esc.d(14))).background(fondo)
                .then(if (m.alToque != null) Modifier.clickable { m.alToque.invoke() } else Modifier)
                .padding(horizontal = esc.d(12), vertical = esc.d(7)),
        ) { Texto(m.texto, 12.5f, color = tinta, lineas = 3, traducir = m.tipo == 1) }
    }
}

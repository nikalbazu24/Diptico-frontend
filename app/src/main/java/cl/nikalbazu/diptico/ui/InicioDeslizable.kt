package cl.nikalbazu.diptico.ui

import cl.nikalbazu.diptico.estado.*

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.audio.Sonidos
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import kotlin.math.abs
import kotlinx.coroutines.launch

/** Las dos páginas del inicio: 0 = tus widgets (donde abre Díptico), 1 = la portada con tu juego. */
private const val WIDGETS = 0
private const val PORTADA = 1

private fun paginaDe(inicio: String) = if (inicio == "widgets") WIDGETS else PORTADA
private fun nombreDe(pagina: Int) = if (pagina == WIDGETS) "widgets" else "heroe"

/**
 * La pantalla del inicio como una cinta sin fin: tus widgets → tu juego → tus widgets → … hacia los dos lados.
 *
 * Son solo dos páginas de verdad (cada una se arma una sola vez): la que no se ve espera al lado hacia donde
 * deslizas. Así se puede seguir deslizando para siempre sin armar copias, y volver a una página ya visitada
 * no la rehace de cero. El botón, el atajo y DIPPO mueven la cinta con la misma animación.
 */
@Composable
fun InicioDeslizable(e: Estado, modifier: Modifier = Modifier, portada: @Composable () -> Unit, widgets: @Composable () -> Unit) {
    val esc = LocalEscala.current
    val alcance = rememberCoroutineScope()
    var actual by remember { mutableIntStateOf(paginaDe(e.inicio)) }
    // La página de al lado se arma la primera vez que se asoma y después queda lista.
    var visitada by remember { mutableStateOf(setOf(actual)) }
    // Corrimiento de la cinta en píxeles: negativo = la siguiente entra por la derecha.
    val corrido = remember { Animatable(0f) }
    var moviendo by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier.fillMaxSize().testTag("inicio-deslizable")) {
        val ancho = with(LocalDensity.current) { maxWidth.toPx() }.coerceAtLeast(1f)
        // Con los efectos de movimiento apagados, el cambio es inmediato.
        val conMovimiento = LocalMovimiento.current

        // Termina un cambio: la otra página pasa a ser la actual y la cinta vuelve a cero sin saltos.
        suspend fun cambiar(haciaIzquierda: Boolean) {
            if (moviendo) return
            moviendo = true
            try {
                visitada = visitada + (1 - actual)
                val destino = if (haciaIzquierda) -ancho else ancho
                if (conMovimiento) corrido.animateTo(destino, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 420f))
                actual = 1 - actual
                corrido.snapTo(0f)
                Diptico.fijarInicio(nombreDe(actual))
                Sonidos.deslizar()
            } finally { moviendo = false }
        }

        // El botón, el atajo o DIPPO pidieron la otra página: la cinta avanza sola hacia la izquierda.
        LaunchedEffect(e.inicio) {
            // En el alcance de la pantalla (no en el de este efecto): si el inicio cambia de nuevo a mitad de camino,
            // la animación termina igual y la cinta nunca queda a medias.
            if (paginaDe(e.inicio) != actual && !moviendo) alcance.launch { cambiar(haciaIzquierda = true) }
        }

        val arrastre = rememberDraggableState { d ->
            if (!moviendo) alcance.launch { corrido.snapTo((corrido.value + d).coerceIn(-ancho, ancho)) }
        }
        // Red de seguridad: si algo de la página se quedó con el arrastre sin usarlo, un deslizamiento largo igual cambia.
        val respaldo = Modifier.pointerInput(e.editandoWidgets, ancho) {
            if (e.editandoWidgets) return@pointerInput
            val umbral = 110.dp.toPx()
            awaitEachGesture {
                val inicio = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var ultimo = inicio.position
                var usado = false
                while (true) {
                    val ev = awaitPointerEvent(PointerEventPass.Final)
                    val c = ev.changes.firstOrNull { it.id == inicio.id } ?: break
                    if (c.isConsumed) usado = true
                    if (c.pressed) { ultimo = c.position; continue }
                    if (usado) break
                    val dx = ultimo.x - inicio.position.x
                    val dy = abs(ultimo.y - inicio.position.y)
                    if (!moviendo && corrido.value == 0f && abs(dx) > umbral && dy < abs(dx) / 2) {
                        alcance.launch { cambiar(haciaIzquierda = dx < 0) }
                    }
                    break
                }
            }
        }

        Box(
            Modifier.fillMaxSize().then(respaldo).draggable(
                arrastre, Orientation.Horizontal, enabled = !e.editandoWidgets,
                onDragStopped = { velocidad ->
                    val c = corrido.value
                    // Basta con arrastrar un cuarto de la pantalla, o soltar con impulso, para pasar a la otra.
                    val pasa = abs(c) > ancho * 0.25f || (abs(velocidad) > 900f && c != 0f && velocidad * c > 0)
                    if (pasa) cambiar(haciaIzquierda = c < 0)
                    else corrido.animateTo(0f, spring(stiffness = 600f))
                },
            ),
        ) {
            for (pagina in listOf(WIDGETS, PORTADA)) {
                if (pagina !in visitada && pagina != actual) continue
                val esActual = pagina == actual
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        val c = corrido.value
                        // La otra página espera pegada al lado hacia donde vas; quieta, no se dibuja.
                        translationX = if (esActual) c else c + if (c <= 0f) ancho else -ancho
                        val d = if (esActual) abs(c) / ancho else 1f - abs(c) / ancho
                        val s = 1f - 0.06f * d
                        scaleX = s; scaleY = s
                        alpha = if (!esActual && c == 0f) 0f else 1f - 0.4f * d
                    },
                ) { if (pagina == WIDGETS) widgets() else portada() }
            }
        }

        // Dos puntitos abajo: dónde estás (se tocan para cambiar).
        if (!e.editandoWidgets) Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(6)),
            horizontalArrangement = Arrangement.spacedBy(esc.d(6)), verticalAlignment = Alignment.CenterVertically,
        ) {
            for (k in listOf(WIDGETS, PORTADA)) {
                val activa = k == actual
                Box(
                    Modifier.size(if (activa) esc.d(18) else esc.d(6), esc.d(6)).clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = if (activa) 0.85f else 0.35f))
                        .clickable(onClickLabel = if (k == WIDGETS) "Ir a tus widgets" else "Ir a tu juego") {
                            if (k != actual) alcance.launch { cambiar(haciaIzquierda = true) }
                        },
                )
            }
        }
    }
}

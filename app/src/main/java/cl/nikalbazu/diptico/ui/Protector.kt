package cl.nikalbazu.diptico.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Estado
import kotlinx.coroutines.delay

/**
 * Protector de pantalla: tus juegos pasan uno tras otro, con un zoom lento.
 * Las dos pantallas muestran juegos distintos y quedan atenuadas. Cualquier botón o toque lo cierra.
 */
@Composable
fun Protector(e: Estado, rol: Rol) {
    val esc = LocalEscala.current
    val juegos = remember(e.juegos.size) { e.juegos.filter { it.arte != null || it.portada != null }.ifEmpty { e.juegos }.shuffled() }
    if (juegos.isEmpty()) return
    var i by remember { mutableIntStateOf(if (rol == Rol.INFERIOR) juegos.size / 2 else 0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(9000)
            i = (i + 1) % juegos.size
        }
    }
    val inf = rememberInfiniteTransition(label = "protector")
    val zoom by inf.animateFloat(1f, 1.12f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "zoom")
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(targetState = juegos[i], animationSpec = tween(1500), label = "juego") { j ->
            Box(Modifier.fillMaxSize()) {
                ArteJuego(j, preferirArte = true, maxLado = 1400, modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom; alpha = 0.75f })
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
                Column(Modifier.align(Alignment.BottomStart).padding(start = esc.d(28), bottom = esc.d(26))) {
                    Etiqueta(Consolas.porId(j.consolaId)?.bonito ?: "", Color.White.copy(alpha = 0.7f), 12f)
                    Texto(j.titulo, 30f, color = Color.White, lineas = 2, sombra = true)
                }
            }
        }
        if (rol != Rol.INFERIOR) {
            Texto(rememberHora(), 46f, color = Color.White.copy(alpha = 0.85f), sombra = true, modifier = Modifier.align(Alignment.TopEnd).padding(esc.d(24)))
        }
    }
}

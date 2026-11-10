package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.bisa.AvatarBisa
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Idioma

/**
 * El recorrido de la primera vez, contado por DIPPO: él grande al centro de la escena, con un globo de diálogo
 * que se va escribiendo letra a letra. Sus caras cambian según el paso (saluda, piensa, celebra al final).
 * Sirve igual con una pantalla o dos (en la de abajo quedan el avance y los botones).
 */
@Composable
fun RecorridoDippo(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val paginas = Diptico.paginasTour(e)
    val k = e.novedades.coerceIn(0, paginas.size - 1)
    val color = Color(paginas[k].color)
    val anima = LocalMovimiento.current
    val flota = rememberInfiniteTransition(label = "dippo-recorrido")
    val y = flota.animateFloat(-6f, 6f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "y")
    val cara = when {
        k == 0 -> CaraDippo.FELIZ
        k == paginas.size - 1 -> CaraDippo.CELEBRANDO
        k == 2 -> CaraDippo.PENSANDO
        else -> CaraDippo.NORMAL
    }
    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .background(Brush.radialGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent)))
            .clickable(interactionSource = null, indication = null) { Diptico.moverNovedades(1) }
            .testTag("recorrido-dippo"),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(esc.d(20)), contentAlignment = Alignment.Center) {
            val ancho = maxWidth > maxHeight * 1.15f
            val tamDippo = if (ancho) 168f else 120f
            val dippo: @Composable () -> Unit = {
                Box(Modifier.size(esc.d(tamDippo + 40)), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(esc.d(tamDippo + 40)).background(Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent))))
                    AvatarBisa(tamDippo, Modifier.graphicsLayer { translationY = if (anima) y.value * density else 0f }, cara = cara)
                }
            }
            if (ancho) Row(verticalAlignment = Alignment.CenterVertically) {
                dippo()
                Spacer(Modifier.width(esc.d(10)))
                Globo(e, k, paginas, color, colaIzquierda = true, modifier = Modifier.widthIn(max = esc.d(470)))
            } else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                dippo()
                Spacer(Modifier.height(esc.d(8)))
                Globo(e, k, paginas, color, colaIzquierda = false, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Globo(
    e: Estado, k: Int, paginas: List<cl.nikalbazu.diptico.estado.Novedades.Pagina>, color: Color, colaIzquierda: Boolean, modifier: Modifier,
) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val fondo = p.fondo2
    val forma = RoundedCornerShape(esc.d(26))
    Column(
        modifier
            .drawBehind {
                // La colita del globo apunta a DIPPO.
                val w = 14.dp.toPx()
                val path = if (colaIzquierda) Path().apply {
                    moveTo(1f, size.height * 0.42f - w); lineTo(-w, size.height * 0.42f); lineTo(1f, size.height * 0.42f + w); close()
                } else Path().apply {
                    moveTo(size.width / 2 - w, 1f); lineTo(size.width / 2, -w); lineTo(size.width / 2 + w, 1f); close()
                }
                drawPath(path, fondo)
            }
            .clip(forma).background(fondo).border(esc.d(1.5f), color.copy(alpha = 0.55f), forma),
    ) {
        AnimatedContent(
            targetState = k,
            transitionSpec = { (fadeIn(tween(220)) + scaleIn(spring(dampingRatio = 0.75f, stiffness = 500f), initialScale = 0.96f)) togetherWith fadeOut(tween(120)) },
            label = "globo",
        ) { i ->
            val pg = paginas[i]
            val c = Color(pg.color)
            Column(Modifier.fillMaxWidth().padding(horizontal = esc.d(22), vertical = esc.d(18))) {
                Etiqueta(if (i == 0) "DIPPO te cuenta" else "Paso $i de ${paginas.size - 1}", c, 11.5f)
                Spacer(Modifier.height(esc.d(4)))
                Texto(pg.titulo, 23f, lineas = 2)
                Spacer(Modifier.height(esc.d(6)))
                TextoQueEscribeDippo(pg.texto, 15f)
                if (pg.tecla.isNotBlank()) {
                    Spacer(Modifier.height(esc.d(10)))
                    Box(Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = 0.18f)).padding(horizontal = esc.d(12), vertical = esc.d(4))) {
                        Texto(pg.tecla, 12.5f, color = c, lineas = 1)
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(p.superficie.copy(alpha = 0.5f)).padding(horizontal = esc.d(18), vertical = esc.d(9)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PuntosAvance(k, paginas.size, color)
            Spacer(Modifier.weight(1f))
            Pistas("A" to if (k == paginas.size - 1) "¡A jugar!" else "Siguiente", "B" to "Saltar")
        }
    }
}

/** El texto de DIPPO aparece letra a letra, como en un juego (sin animación, sale entero). */
@Composable
private fun TextoQueEscribeDippo(texto: String, tam: Float) {
    val t = Idioma.t(texto)
    val anima = LocalMovimiento.current
    var n by remember(t) { mutableIntStateOf(if (anima) 0 else t.length) }
    LaunchedEffect(t) {
        while (n < t.length) { n = (n + 2).coerceAtMost(t.length); kotlinx.coroutines.delay(24) }
    }
    Box {
        // El texto completo invisible reserva el alto: el globo no cambia de tamaño mientras se escribe.
        Detalle(t, tam, color = Color.Transparent, lineas = 5)
        Detalle(t.take(n), tam, color = LocalPaleta.current.tinta.copy(alpha = 0.88f), lineas = 5)
    }
}

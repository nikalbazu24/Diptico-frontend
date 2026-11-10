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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import cl.nikalbazu.diptico.estado.RecorridoVivo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
/** Cómo se para DIPPO en cada paso: corrido (dx, dy), tamaño e inclinación, para que «mire» lo que explica. */
private data class Pose(val dx: Float, val dy: Float, val escala: Float, val giro: Float)

private fun poseDe(k: Int, total: Int): Pose = when {
    k == total - 1 -> Pose(0f, 0f, 1.06f, 0f)
    k == 1 -> Pose(0f, -34f, 1f, -9f)   // mira arriba: «arriba se luce tu juego»
    k == 2 -> Pose(14f, 30f, 0.98f, 7f) // mira abajo, al estante
    k == 3 -> Pose(0f, 0f, 1.12f, 0f)   // protagonista: «yo te ayudo»
    k == 4 -> Pose(-6f, -26f, 1f, -12f) // mira la esquina de tu foto
    else -> Pose(0f, 0f, 1f, 0f)
}

/** El «pruébalo» de cada paso: qué hay que hacer y qué responde DIPPO al lograrlo. */
private fun retoDe(k: Int): Pair<String, String>? = when (k) {
    2 -> "gatillo" to "¡Eso! Así saltas de consola en consola."
    3 -> "toque" to "¡Hola, hola! Cuando quieras, pregúntame."
    else -> null
}

private fun fraseDe(que: String): String = when (que) {
    "toque" -> "¡Jiji, cosquillas!"
    "gatillo" -> "L1 y R1 cambian de consola."
    "gatillo2" -> "L2 y R2 saltan de letra en letra."
    "x" -> "X abre la ficha de un juego."
    "y" -> "Y abre las opciones rápidas del juego."
    "stick" -> "Los sticks buscan y cambian la vista."
    else -> "¡Eso!"
}

@Composable
fun RecorridoDippo(e: Estado) {
    val esc = LocalEscala.current
    val paginas = Diptico.paginasTour(e)
    val k = e.novedades.coerceIn(0, paginas.size - 1)
    val color = Color(paginas[k].color)
    val anima = LocalMovimiento.current
    val alcance = rememberCoroutineScope()

    // Movimiento: entra desde el costado, flota, se acomoda en cada paso, salta al cambiar de paso y gira al tocarlo.
    val flota = rememberInfiniteTransition(label = "dippo-recorrido")
    val y = flota.animateFloat(-6f, 6f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "y")
    val vaiven = flota.animateFloat(-3f, 3f, infiniteRepeatable(tween(2300), RepeatMode.Reverse), label = "vaiven")
    val pose = poseDe(k, paginas.size)
    val resorte = spring<Float>(dampingRatio = 0.55f, stiffness = 180f)
    val dx by animateFloatAsState(if (anima) pose.dx else 0f, resorte, label = "dx")
    val dy by animateFloatAsState(if (anima) pose.dy else 0f, resorte, label = "dy")
    val escala by animateFloatAsState(pose.escala, resorte, label = "escala")
    val inclina by animateFloatAsState(if (anima) pose.giro else 0f, resorte, label = "inclina")
    val entrada = remember { Animatable(if (anima) 0f else 1f) }
    val salto = remember { Animatable(0f) }
    val vuelta = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 120f)) }
    LaunchedEffect(k) {
        if (!anima || k == 0) return@LaunchedEffect
        salto.animateTo(-30f, tween(150)); salto.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 380f))
    }

    // Lo que dice al reaccionar, y si ya se cumplió el «pruébalo» de este paso.
    var frase by remember { mutableStateOf<String?>(null) }
    var caraReaccion by remember { mutableStateOf<CaraDippo?>(null) }
    var retoHecho by remember(k) { mutableStateOf(false) }
    val reto = retoDe(k)
    fun reaccionar(que: String) {
        alcance.launch {
            val cumple = reto != null && reto.first == que && !retoHecho
            if (cumple) retoHecho = true
            frase = if (cumple) reto!!.second else fraseDe(que)
            caraReaccion = if (cumple) CaraDippo.CELEBRANDO else if (que == "toque") CaraDippo.SORPRENDIDO else CaraDippo.FELIZ
            if (anima) {
                launch { salto.animateTo(-36f, tween(140)); salto.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 360f)) }
                if (que == "toque" || cumple) launch { vuelta.snapTo(0f); vuelta.animateTo(360f, tween(620)); vuelta.snapTo(0f) }
            }
            delay(if (cumple) 1600 else 2200)
            caraReaccion = null
            frase = null
            if (cumple && Diptico.estado.value.novedades == k) Diptico.moverNovedades(1)
        }
    }
    val reaccion by RecorridoVivo.reaccion.collectAsState()
    LaunchedEffect(reaccion) { reaccion?.let { if (System.nanoTime() - it.vez < 2_000_000_000L) reaccionar(it.que) } }

    val cara = caraReaccion ?: when {
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
                    Box(Modifier.size(esc.d(tamDippo + 40)).graphicsLayer { scaleX = escala; scaleY = escala }
                        .background(Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent))))
                    AvatarBisa(
                        tamDippo,
                        Modifier
                            .graphicsLayer {
                                val d = density
                                translationX = (dx - (1f - entrada.value) * 260f) * d
                                translationY = (dy + salto.value + if (anima) y.value else 0f) * d
                                alpha = entrada.value.coerceIn(0f, 1f)
                                scaleX = escala; scaleY = escala
                                rotationZ = inclina + (if (anima) vaiven.value else 0f) + vuelta.value
                            }
                            // Tocarlo: gira, se sorprende y dice algo (en el paso «yo te ayudo», cumple el reto).
                            .clickable(interactionSource = null, indication = null) { reaccionar("toque") }
                            .testTag("dippo-recorrido"),
                        cara = cara,
                    )
                    // Lo que dice al reaccionar: un globito chico sobre su cabeza.
                    androidx.compose.animation.AnimatedVisibility(
                        frase != null,
                        Modifier.align(Alignment.TopCenter).graphicsLayer { translationX = dx * density; translationY = (dy - 18f) * density },
                        enter = fadeIn(tween(140)) + scaleIn(spring(dampingRatio = 0.5f), initialScale = 0.6f),
                        exit = fadeOut(tween(160)),
                    ) {
                        val f = frase ?: ""
                        Box(Modifier.widthIn(max = esc.d(220)).clip(RoundedCornerShape(esc.d(14))).background(Color.White).padding(horizontal = esc.d(12), vertical = esc.d(6))) {
                            Texto(f, 13f, color = Color(0xFF1B1630), lineas = 2)
                        }
                    }
                }
            }
            if (ancho) Row(verticalAlignment = Alignment.CenterVertically) {
                dippo()
                Spacer(Modifier.width(esc.d(10)))
                Globo(e, k, paginas, color, colaIzquierda = true, reto = reto?.let { if (retoHecho) "✓ ¡Bien hecho!" else pistaReto(k) },
                    modifier = Modifier.widthIn(max = esc.d(470)))
            } else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                dippo()
                Spacer(Modifier.height(esc.d(8)))
                Globo(e, k, paginas, color, colaIzquierda = false, reto = reto?.let { if (retoHecho) "✓ ¡Bien hecho!" else pistaReto(k) },
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun pistaReto(k: Int) = when (k) {
    2 -> "Pruébalo: aprieta L1 o R1"
    3 -> "Pruébalo: tócame"
    else -> ""
}

@Composable
private fun Globo(
    e: Estado, k: Int, paginas: List<cl.nikalbazu.diptico.estado.Novedades.Pagina>, color: Color, colaIzquierda: Boolean, reto: String?, modifier: Modifier,
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
                if (reto != null && i == k) {
                    Spacer(Modifier.height(esc.d(10)))
                    val pulso = rememberInfiniteTransition(label = "reto").animateFloat(0.55f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulso")
                    val hecho = reto.startsWith("✓")
                    Box(
                        Modifier.graphicsLayer { alpha = if (hecho) 1f else pulso.value }.clip(RoundedCornerShape(50))
                            .background(if (hecho) Color(0xFF2EC4A6) else c).padding(horizontal = esc.d(14), vertical = esc.d(6)),
                    ) { Texto(reto, 13.5f, color = Color.White, lineas = 1) }
                } else if (pg.tecla.isNotBlank()) {
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

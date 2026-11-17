package cl.nikalbazu.diptico.ui

import cl.nikalbazu.diptico.estado.*

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.bisa.AvatarBisa
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Idioma

/*
 * El recorrido de la primera vez (boceto 13c): DIPPO ilumina cada parte REAL del inicio y te hace probar los botones.
 * El resto de la pantalla se oscurece; el foco (con un halo celeste) se mueve de una parte a otra. DIPPO y su globo
 * van al lado de lo que explica, en la pantalla donde está. Funciona con una pantalla o con dos.
 */

/** Lo que se ilumina: en qué pantalla y un rectángulo en fracciones del lienzo (x, y, ancho, alto). */
private data class Foco(val abajo: Boolean, val x: Float, val y: Float, val w: Float, val h: Float)

/** Dónde está cada parte con dos pantallas (arriba 6a, abajo 6b) y con una (6c). null = sin foco (el cierre). */
private fun focoDe(k: Int, una: Boolean): Foco? = if (una) when (k) {
    0 -> Foco(false, .02f, .10f, .48f, .74f)
    1 -> Foco(false, .02f, .10f, .48f, .24f)
    2, 5 -> Foco(false, .50f, .50f, .49f, .42f)
    3 -> Foco(false, .38f, .91f, .24f, .09f)
    4 -> Foco(false, .01f, .00f, .40f, .11f)
    6 -> Foco(false, .01f, .90f, .52f, .10f)
    else -> null
} else when (k) {
    0 -> Foco(false, .02f, .11f, .47f, .66f)
    1 -> Foco(false, .02f, .11f, .47f, .24f)
    2, 5 -> Foco(true, .01f, .10f, .98f, .78f)
    3 -> Foco(false, .38f, .90f, .24f, .10f)
    4 -> Foco(true, .01f, .00f, .56f, .11f)
    6 -> Foco(true, .40f, .89f, .20f, .11f)
    else -> null
}

/** El «pruébalo» de cada paso: qué hay que hacer y qué responde DIPPO al lograrlo. */
private fun retoDe(k: Int): Pair<String, String>? = when (k) {
    1 -> "gatillo" to "¡Eso! Así saltas de consola en consola."
    3 -> "gatillo2" to "¡Bien! Ahí viven tus widgets."
    5 -> "select" to "¡Así se cambia la vista!"
    6 -> "toque" to "¡Hola, hola! Cuando quieras, pregúntame."
    else -> null
}

private fun pistaReto(k: Int) = when (k) { 1 -> "Aprieta L1 o R1"; 3 -> "Aprieta L2 o R2"; 5 -> "Aprieta Select"; 6 -> "Tócame"; else -> "" }

private fun fraseDe(que: String): String = when (que) {
    "toque" -> "¡Jiji, cosquillas!"
    "gatillo" -> "L1 y R1 cambian de consola."
    "gatillo2" -> "L2 y R2 te llevan a Tu espacio."
    "select" -> "Select cambia la vista."
    "x" -> "X personaliza la consola elegida."
    "y" -> "Y sigue tu última partida."
    "stick" -> "El stick derecho gira la consola."
    else -> "¡Eso!"
}

@Composable
fun RecorridoDippo(e: Estado, rol: Rol = Rol.UNICA) {
    val paginas = Diptico.paginasTour(e)
    val k = e.novedades.coerceIn(0, paginas.size - 1)
    val una = rol == Rol.UNICA || e.unaPantalla
    val abajo = rol == Rol.INFERIOR
    val foco = focoDe(k, una)
    // ¿Va DIPPO en esta pantalla? Donde está el foco; en el cierre (sin foco), en la de arriba.
    val aqui = una || (foco?.abajo ?: false) == abajo
    val alcance = rememberCoroutineScope()
    val anima = LocalMovimiento.current

    var frase by remember { mutableStateOf<String?>(null) }
    var caraReaccion by remember { mutableStateOf<CaraDippo?>(null) }
    var retoHecho by remember(k) { mutableStateOf(false) }
    val reto = retoDe(k)
    val salto = remember { Animatable(0f) }
    fun reaccionar(que: String) {
        alcance.launch {
            val cumple = reto != null && reto.first == que && !retoHecho
            if (cumple) retoHecho = true
            frase = if (cumple) reto!!.second else fraseDe(que)
            caraReaccion = if (cumple) CaraDippo.CELEBRANDO else if (que == "toque") CaraDippo.SORPRENDIDO else CaraDippo.FELIZ
            if (anima) launch { salto.animateTo(-36f, tween(140)); salto.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 360f)) }
            delay(if (cumple) 1500 else 2000)
            caraReaccion = null; frase = null
            if (cumple && Diptico.estado.value.novedades == k) Diptico.moverNovedades(1)
        }
    }
    val reaccion by RecorridoVivo.reaccion.collectAsState()
    LaunchedEffect(reaccion) { reaccion?.let { if (aqui && System.nanoTime() - it.vez < 2_000_000_000L) reaccionar(it.que) } }

    LienzoBoceto(if (abajo) 1240f else 1920f, 1080f, Modifier.testTag("recorrido-dippo"), zoom = if (una) ZOOM_UNA else if (abajo) ZOOM_ABAJO else ZOOM_ARRIBA) {
        val esc = LocalEscala.current
        val w = anchoLienzo(); val h = altoLienzo()
        val conFocoAqui = foco != null && (una || foco.abajo == abajo)
        // El foco se desliza de una parte a otra.
        val resorte = spring<Float>(dampingRatio = 0.8f, stiffness = 160f)
        val fx by animateFloatAsState(if (conFocoAqui) foco!!.x * w else w / 2, resorte, label = "fx")
        val fy by animateFloatAsState(if (conFocoAqui) foco!!.y * h else h / 2, resorte, label = "fy")
        val fw by animateFloatAsState(if (conFocoAqui) foco!!.w * w else 0f, resorte, label = "fw")
        val fh by animateFloatAsState(if (conFocoAqui) foco!!.h * h else 0f, resorte, label = "fh")
        val pulso = rememberInfiniteTransition(label = "halo").animateFloat(.55f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "p")
        Canvas(
            Modifier.fillMaxSize().graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                .clickable(interactionSource = null, indication = null) { if (reto == null || retoHecho) Diptico.moverNovedades(1) },
        ) {
            val u = size.width / w
            drawRect(Color(0xFF030308).copy(alpha = .74f))
            if (fw > 1f) {
                val o = androidx.compose.ui.geometry.Offset(fx * u, fy * u)
                val sz = androidx.compose.ui.geometry.Size(fw * u, fh * u)
                val r = androidx.compose.ui.geometry.CornerRadius(30f * u)
                drawRoundRect(Color.Black, o, sz, r, blendMode = androidx.compose.ui.graphics.BlendMode.Clear)
                drawRoundRect(Color(0xFF8FEAFF).copy(alpha = .45f * pulso.value), o, sz, r, style = androidx.compose.ui.graphics.drawscope.Stroke(14f * u))
                drawRoundRect(Color(0xFFF4F3FA), o, sz, r, style = androidx.compose.ui.graphics.drawscope.Stroke(4f * u))
            }
        }
        if (!aqui) {
            // La otra pantalla: solo una indicación de dónde mirar.
            Row(Modifier.align(Alignment.Center).clip(RoundedCornerShape(50)).background(Color(0xFFF4F3FA)).padding(horizontal = esc.d(30), vertical = esc.d(16)),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                IconoDiptico(if (abajo) Icono.ARRIBA else Icono.ABAJO, Color(0xFF12131F), 34f)
                TB("DIPPO te muestra en la otra pantalla", 26f, color = Color(0xFF12131F), peso = FontWeight.ExtraBold)
            }
            return@LienzoBoceto
        }
        // DIPPO y su globo, al lado de lo que explica (abajo del foco si hay lugar; si no, arriba).
        val pg = paginas[k]
        val anchoGlobo = minOf(680f, w * .6f)
        val debajo = !conFocoAqui || (fy + fh + 360f < h)
        val gx = (if (conFocoAqui) fx + 30f else w / 2 - anchoGlobo / 2).coerceIn(30f, w - anchoGlobo - 220f)
        val gy = if (!conFocoAqui) h / 2 - 160f else if (debajo) fy + fh + 30f else (fy - 330f).coerceAtLeast(20f)
        val cara = caraReaccion ?: when { k == 0 -> CaraDippo.FELIZ; k == paginas.size - 1 -> CaraDippo.CELEBRANDO; reto != null && !retoHecho -> CaraDippo.PENSANDO; else -> CaraDippo.NORMAL }
        val px = with(androidx.compose.ui.platform.LocalDensity.current) { esc.d(1f).toPx() }
        val flota = rememberInfiniteTransition(label = "flota").animateFloat(-6f, 6f, infiniteRepeatable(tween(1700), RepeatMode.Reverse), label = "y")
        val dxAnim by animateFloatAsState(gx + anchoGlobo + 20f, resorte, label = "dippoX")
        val dyAnim by animateFloatAsState(gy, resorte, label = "dippoY")
        Box(
            Modifier.offset(x = esc.d(dxAnim), y = esc.d(dyAnim)).size(esc.d(180))
                .graphicsLayer { translationY = (salto.value + if (anima) flota.value else 0f) * px; rotationZ = if (k % 2 == 0) 6f else -6f }
                .clickable(interactionSource = null, indication = null) { reaccionar("toque") }.testTag("dippo-recorrido"),
            contentAlignment = Alignment.Center,
        ) { AvatarBisa(160f * esc.factor, cara = cara) }
        androidx.compose.animation.AnimatedVisibility(frase != null, Modifier.offset(x = esc.d(dxAnim - 40f), y = esc.d(dyAnim - 70f)),
            enter = fadeIn(tween(140)) + scaleIn(spring(dampingRatio = 0.5f), initialScale = 0.6f), exit = fadeOut(tween(160))) {
            Box(Modifier.clip(RoundedCornerShape(esc.d(20))).background(Color.White).padding(horizontal = esc.d(18), vertical = esc.d(10))) {
                TB(frase ?: "", 20f, color = Color(0xFF1B1630), peso = FontWeight.ExtraBold, lineas = 2)
            }
        }
        Column(
            Modifier.offset(x = esc.d(gx), y = esc.d(gy)).width(esc.d(anchoGlobo))
                .clip(RoundedCornerShape(esc.d(32))).background(Color(0xFFF4F3FA)).padding(horizontal = esc.d(30), vertical = esc.d(24)),
            verticalArrangement = Arrangement.spacedBy(esc.d(10)),
        ) {
            TB("${k + 1} de ${paginas.size} · ${pg.titulo}", 18f, color = Color(pg.color), peso = FontWeight.ExtraBold)
            TextoQueEscribeDippo(pg.texto)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                if (reto != null) {
                    val p2 = rememberInfiniteTransition(label = "reto").animateFloat(.6f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "r")
                    Box(Modifier.graphicsLayer { alpha = if (retoHecho) 1f else p2.value }.clip(RoundedCornerShape(50))
                        .background(if (retoHecho) Color(0xFF2EC4A6) else Color(0xFF6B4EFF))
                        .clickable { if (!retoHecho) reaccionar(reto.first) }.padding(horizontal = esc.d(18), vertical = esc.d(8))) {
                        TB(if (retoHecho) "¡Eso! ✓" else pistaReto(k), 19f, color = Color.White, peso = FontWeight.ExtraBold)
                    }
                }
                Spacer(Modifier.weight(1f))
                TB(if (reto != null && !retoHecho) "B saltar" else if (k == paginas.size - 1) "A ¡A jugar!" else "A siguiente · B saltar", 17f, color = Color(0xFF4A4B63), peso = FontWeight.ExtraBold)
            }
        }
    }
}

/** El texto de DIPPO aparece letra a letra, como en un juego (sin animación, sale entero). */
@Composable
private fun TextoQueEscribeDippo(texto: String) {
    val t = Idioma.t(texto)
    val anima = LocalMovimiento.current
    var n by remember(t) { mutableIntStateOf(if (anima) 0 else t.length) }
    LaunchedEffect(t) { while (n < t.length) { n = (n + 2).coerceAtMost(t.length); delay(22) } }
    Box {
        // El texto completo invisible reserva el alto: el globo no cambia de tamaño mientras se escribe.
        TB(t, 26f, color = Color.Transparent, peso = FontWeight.ExtraBold, lineas = 4, traducir = false)
        TB(t.take(n), 26f, color = Color(0xFF12131F), peso = FontWeight.ExtraBold, lineas = 4, traducir = false)
    }
}

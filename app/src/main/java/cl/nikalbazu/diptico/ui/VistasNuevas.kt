package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

/**
 * Discos: cada consola es un vinilo en su funda, de pie en una caja como en una disquería.
 * La funda muestra cuatro de sus portadas; la elegida se para al frente y su disco sale girando por arriba,
 * con el ícono de la consola en la etiqueta. Las vecinas quedan inclinadas, como cuando hojeas una caja de discos.
 * Cruceta o deslizar para hojear, A (o tocar la del frente) para entrar a sus juegos.
 */
@Composable
fun DiscosConsolas(e: Estado, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val ids = e.pestanas
    val sel = ids.indexOf(e.consolaSel).coerceAtLeast(0)
    val pos by animateFloatAsState(sel.toFloat(), tween(360, easing = FastOutSlowInEasing), label = "discos")
    Column(modifier) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f).pointerInput(ids.size) {
                var acumulado = 0f
                detectHorizontalDragGestures(onDragEnd = { acumulado = 0f }) { cambio, d ->
                    cambio.consume()
                    acumulado += d
                    if (abs(acumulado) > 46f * density) { Diptico.cambiarConsola(if (acumulado > 0) -1 else 1); acumulado = 0f }
                }
            },
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (ids.isEmpty()) {
                Detalle(if (e.escaneando) "Buscando tus juegos…" else "Aquí aparecerán tus consolas", 15f, modifier = Modifier.align(Alignment.Center))
                return@BoxWithConstraints
            }
            // La funda cabe con su disco asomado por arriba (el disco sube hasta la mitad de la funda).
            val funda = minOf(maxHeight.value / 1.62f, maxWidth.value * 0.34f, 200f * esc.factor)
            val desde = (kotlin.math.floor(pos).toInt() - 4).coerceAtLeast(0)
            val hasta = (kotlin.math.ceil(pos).toInt() + 4).coerceAtMost(ids.size - 1)
            for (k in desde..hasta) {
                val id = ids[k]
                key(id) {
                    val off = k - pos
                    val cerca = off.coerceIn(-1f, 1f)
                    val frente = k == sel
                    Box(
                        Modifier.padding(bottom = (funda * 0.06f).dp).size((funda * 1.55f).dp, (funda * 1.55f).dp)
                            .zIndex(10f - abs(off))
                            .graphicsLayer {
                                // Al centro la del frente; las otras se juntan a los lados, como fundas en una caja.
                                val x = if (abs(off) <= 1f) off * funda * 0.74f else sign(off) * (funda * 0.74f + (abs(off) - 1f) * funda * 0.3f)
                                translationX = x * density
                                rotationY = -cerca * 34f
                                cameraDistance = 14f * density
                                val s = 1f - abs(cerca) * 0.2f
                                scaleX = s; scaleY = s
                                transformOrigin = TransformOrigin(0.5f, 1f)
                                alpha = ((4.2f - abs(off)) / 1.2f).coerceIn(0f, 1f)
                            }
                            .clickable { if (frente) Diptico.entrarSistema() else Diptico.elegirConsola(id) },
                        contentAlignment = Alignment.BottomCenter,
                    ) { DiscoConsola(e, id, funda, 1f - abs(cerca)) }
                }
            }
        }
        val id = ids.getOrNull(sel)
        Column(Modifier.fillMaxWidth().padding(bottom = esc.d(4)), horizontalAlignment = Alignment.CenterHorizontally) {
            Texto(Consolas.porId(id ?: "")?.nombreLargo ?: if (id == "todo") "Todos tus juegos" else "", 17f, lineas = 1, alinear = TextAlign.Center)
            Row { Pistas("✥" to "Hojear", "A" to "Abrir", "X" to "Personalizar", "Y" to "Al azar") }
        }
    }
}

/** Una funda con sus portadas y el disco que sale por arriba ([fuera] = 0 guardado, 1 afuera girando). */
@Composable
private fun DiscoConsola(e: Estado, id: String, funda: Float, fuera: Float) {
    val c = Consolas.porId(id)
    val color = c?.color ?: Uva
    val juegos = remember(e.juegos, id) { (if (id == "todo") e.juegos else e.juegos.filter { it.consolaId == id }).take(4) }
    val sale by animateFloatAsState(fuera, spring(dampingRatio = 0.7f, stiffness = 220f), label = "sale")
    // El disco gira solo cuando está afuera y los efectos están completos (en ahorro queda quieto).
    val giro = if (fuera > 0.95f && Estilo.efectosCompletos) {
        val t = rememberInfiniteTransition(label = "giro")
        t.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart), label = "giro").value
    } else 0f
    val lado = funda.dp
    Box(Modifier.size(lado, (funda * 1.55f).dp), contentAlignment = Alignment.BottomCenter) {
        // El disco, detrás de la funda: sube al salir.
        Box(
            Modifier.size(lado * 0.94f)
                .graphicsLayer { translationY = -(funda * (0.06f + 0.5f * sale)) * density; rotationZ = giro },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                // Vinilo con un borde que capta la luz: así se lee aunque el fondo sea oscuro.
                drawCircle(Brush.radialGradient(listOf(Color(0xFF2B2A33), Color(0xFF15151A)), radius = r), r)
                drawCircle(lerp(color, Color.White, 0.35f).copy(alpha = 0.55f), r - 1.5f, style = Stroke(3f))
                // Los surcos.
                for (i in 1..8) drawCircle(Color.White.copy(alpha = 0.07f), r * (0.40f + i * 0.068f), style = Stroke(1.4f))
                // Dos brillos que cruzan el disco (giran con él).
                for (inicio in listOf(-70f, 110f)) drawArc(Color.White.copy(alpha = 0.13f), inicio, 36f, false, topLeft = Offset(r * 0.14f, r * 0.14f),
                    size = androidx.compose.ui.geometry.Size(r * 1.72f, r * 1.72f), style = Stroke(r * 0.22f))
                drawCircle(Brush.radialGradient(listOf(lerp(color, Color.White, 0.15f), color)), r * 0.36f)
                drawCircle(Color(0xFF111114), r * 0.04f)
            }
            Box(Modifier.align(Alignment.Center).size(lado * 0.2f), contentAlignment = Alignment.Center) {
                IconoConsola(c, funda * 0.13f / LocalEscala.current.factor, Color.Transparent)
            }
        }
        // La funda: cuatro portadas (o el color de la consola si tiene menos) y su nombre abajo.
        val forma = RoundedCornerShape((funda * 0.04f).dp)
        Box(
            Modifier.size(lado).clip(forma)
                .background(Brush.linearGradient(listOf(lerp(color, Color.White, 0.1f), lerp(color, Color.Black, 0.35f))))
                .border((1.5f).dp, Color.White.copy(alpha = 0.16f + 0.5f * sale), forma),
        ) {
            if (juegos.size >= 4) Column(Modifier.fillMaxSize()) {
                for (fila in 0..1) Row(Modifier.fillMaxWidth().weight(1f)) {
                    for (col in 0..1) ArteJuego(juegos[fila * 2 + col], preferirArte = false, maxLado = 200, modifier = Modifier.weight(1f).fillMaxSize())
                }
            } else if (juegos.isNotEmpty()) {
                ArteJuego(juegos[0], preferirArte = false, maxLado = 320, modifier = Modifier.fillMaxSize())
            } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { IconoConsola(c, funda * 0.36f / LocalEscala.current.factor, Color.Transparent) }
            // Faja con el color de la consola, como el sello de un disco.
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha = 0.85f), lerp(color, Color.Black, 0.45f))))
                    .padding(start = (funda * 0.06f).dp, end = (funda * 0.06f).dp, top = (funda * 0.12f).dp, bottom = (funda * 0.05f).dp),
            ) {
                Texto(c?.nombre ?: "Todo", 14f, color = Color.White, lineas = 1, sombra = true)
                Detalle(juegosTexto(if (id == "todo") e.juegos.size else e.juegos.count { it.consolaId == id }), 11f, color = Color.White.copy(alpha = 0.85f), lineas = 1)
            }
        }
    }
}

/**
 * Abanico: tus juegos como un mazo de cartas abierto en la mano. La elegida sube al centro y las demás se abren
 * en arco hacia los lados. Cruceta o deslizar para pasar, A para jugar.
 */
@Composable
fun AbanicoJuegos(e: Estado, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val t = LocalTamJuegos.current
    val lista = e.visibles
    val sel = e.indiceSel
    val pos by animateFloatAsState(sel.toFloat(), tween(320, easing = FastOutSlowInEasing), label = "abanico")
    Column(modifier) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f).clipToBounds().pointerInput(lista.size) {
                var acumulado = 0f
                detectHorizontalDragGestures(onDragEnd = { acumulado = 0f }) { cambio, d ->
                    cambio.consume()
                    acumulado += d
                    if (abs(acumulado) > 40f * density) { Diptico.moverJuego(if (acumulado > 0) -1 else 1); acumulado = 0f }
                }
            },
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (lista.isEmpty()) {
                Detalle(if (e.escaneando) "Buscando tus juegos…" else "Aquí aparecerán tus juegos", 15f, modifier = Modifier.align(Alignment.Center))
                return@BoxWithConstraints
            }
            val alto = (maxHeight.value / esc.factor * 0.72f).coerceAtMost(200f * t)
            // El centro del arco queda muy por debajo de la pantalla: así el abanico se abre suave.
            val radio = alto * 2.6f * esc.factor
            val desde = (kotlin.math.floor(pos).toInt() - 6).coerceAtLeast(0)
            val hasta = (kotlin.math.ceil(pos).toInt() + 6).coerceAtMost(lista.size - 1)
            for (k in desde..hasta) {
                val j = lista[k]
                val c = Consolas.porId(j.consolaId) ?: continue
                key(j.clave) {
                    val off = k - pos
                    val frente = k == sel
                    val base = cajaDe(c)
                    val escala = alto / base.alto
                    val angulo = off * 9.5f
                    val rad = Math.toRadians(angulo.toDouble())
                    val sube = (1f - abs(off)).coerceIn(0f, 1f)
                    Box(
                        Modifier.padding(bottom = esc.d(10))
                            .zIndex(if (frente) 50f else 20f - abs(off))
                            .graphicsLayer {
                                translationX = (sin(rad) * radio).toFloat() * density
                                translationY = ((1 - cos(rad)) * radio).toFloat() * density - sube * alto * 0.12f * esc.factor * density
                                rotationZ = angulo
                                val s = 0.86f + 0.16f * sube
                                scaleX = s; scaleY = s
                                transformOrigin = TransformOrigin(0.5f, 1f)
                                alpha = ((7f - abs(off)) / 1.5f).coerceIn(0f, 1f)
                            }
                            .clickable { if (frente) Diptico.jugar() else Diptico.elegirJuego(j.clave) },
                    ) {
                        CajaJuego(j, c, escala = escala, vivo = frente)
                        // Las de atrás se oscurecen un poco: la del frente se lleva la mirada.
                        if (!frente) Box(Modifier.matchParentSize().clip(RoundedCornerShape(esc.d(base.radio * escala))).background(Color.Black.copy(alpha = 0.22f * (1f - sube))))
                    }
                }
            }
        }
        val j = e.seleccionado
        Column(Modifier.fillMaxWidth().padding(bottom = esc.d(4)), horizontalAlignment = Alignment.CenterHorizontally) {
            Texto(j?.titulo ?: "", 17f, lineas = 1, alinear = TextAlign.Center)
            if (j != null) Detalle("${Consolas.porId(j.consolaId)?.nombre ?: ""} · ${e.indiceSel + 1} de ${lista.size}", 11.5f, lineas = 1)
        }
    }
}

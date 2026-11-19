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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
private data class MarcoTour(val x: Float, val y: Float, val ancho: Float, val alto: Float)

/** Destinos del tour: la ubicación se obtiene del componente real, no de coordenadas fijas. */
private fun destinosDe(k: Int, una: Boolean, enWidgets: Boolean): List<String> = when (k) {
    0 -> listOf("consola")
    1 -> listOf("controles", "consola")
    2 -> listOf("carpetas")
    3 -> if (enWidgets) listOf("espacio") else listOf("widgets")
    4 -> listOf("apps")
    5 -> if (una) listOf("carpetas") else listOf("controles")
    6 -> listOf("dippo")
    else -> emptyList()
}

/** Conversión ventana -> lienzo, con recorte para no pintar fuera de los bordes físicos. */
private fun marcoDeAncla(ancla: Rect, ventana: Rect, unidad: Float, ancho: Float, alto: Float): MarcoTour? {
    if (unidad <= 0f || ancla.width <= 0f || ancla.height <= 0f) return null
    val margen = 10f
    val x1 = ((ancla.left - ventana.left) / unidad - margen).coerceIn(0f, ancho)
    val y1 = ((ancla.top - ventana.top) / unidad - margen).coerceIn(0f, alto)
    val x2 = ((ancla.right - ventana.left) / unidad + margen).coerceIn(0f, ancho)
    val y2 = ((ancla.bottom - ventana.top) / unidad + margen).coerceIn(0f, alto)
    return if (x2 - x1 > 1f && y2 - y1 > 1f) MarcoTour(x1, y1, x2 - x1, y2 - y1) else null
}

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

/** El «pruébalo» de cada paso (obligatorio): qué hay que hacer y qué responde DIPPO al lograrlo. */
private fun retoDe(k: Int): Pair<String, String>? = RecorridoVivo.retoDe(k)?.let { it to when (it) {
    "gatillo" -> "¡Eso! Así saltas de consola en consola."
    "x" -> "¡Así se abre! Ahí cambias su color, ícono y fondo."
    "gatillo2" -> "¡Bien! Ahí viven tus widgets."
    "select" -> "¡Así se cambia la vista!"
    else -> "¡Hola, hola! Cuando quieras, pregúntame."
} }

private fun pistaReto(k: Int) = when (k) { 1 -> "Aprieta L1 o R1"; 2 -> "Aprieta X"; 3 -> "Aprieta L2 o R2"; 5 -> "Aprieta Select"; 6 -> "Tócame"; else -> "" }

private fun fraseDe(que: String): String = when (que) {
    "toque" -> "¡Jiji, cosquillas!"
    "falta" -> "¡Primero pruébalo! Mira lo que te pido abajo."
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
    // Colección viva puede redistribuir sus componentes: usamos sus medidas reales.
    val conAnclas = coleccionVivaActiva(e)
    // Cada paso pertenece a una pantalla; en el cierre DIPPO queda arriba.
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
            if (cumple) { retoHecho = true; RecorridoVivo.hecho = k }
            frase = if (cumple) reto!!.second else fraseDe(que)
            caraReaccion = if (cumple) CaraDippo.CELEBRANDO else if (que == "toque") CaraDippo.SORPRENDIDO else CaraDippo.FELIZ
            if (anima) launch { salto.animateTo(-36f, tween(140)); salto.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 360f)) }
            // La acción real permanece visible antes de seguir con la explicación.
            delay(if (cumple) 2200 else 2000)
            caraReaccion = null; frase = null
            if (cumple && Diptico.estado.value.novedades == k) {
                // El paso de personalización abre su panel real, y lo cierra al continuar.
                if (k == 2 && Diptico.estado.value.panel == Panel.EDITAR_CONSOLA) Diptico.cerrarPanel()
                Diptico.moverNovedades(1)
            }
        }
    }
    val reaccion by RecorridoVivo.reaccion.collectAsState()
    LaunchedEffect(reaccion) { reaccion?.let { if (aqui && System.nanoTime() - it.vez < 2_000_000_000L) reaccionar(it.que) } }

    LienzoBoceto(if (abajo) 1240f else 1920f, 1080f, Modifier.testTag("recorrido-dippo"), zoom = if (una) ZOOM_UNA else if (abajo) ZOOM_ABAJO else ZOOM_ARRIBA) {
        val esc = LocalEscala.current
        val w = anchoLienzo(); val h = altoLienzo()
        val pantalla = pantallaActual()
        val ancla = if (conAnclas && aqui) destinosDe(k, una, e.inicio == "widgets").firstNotNullOfOrNull {
            AnclasRecorrido.buscar(pantalla, it)
        } else null
        var ventanaCanvas by remember { mutableStateOf<Rect?>(null) }
        val unidad = with(LocalDensity.current) { esc.d(1f).toPx() }
        val marco = if (conAnclas) {
            if (ancla != null && ventanaCanvas != null) marcoDeAncla(ancla, ventanaCanvas!!, unidad, w, h) else null
        } else if (foco != null && aqui) {
            MarcoTour(foco.x * w, foco.y * h, foco.w * w, foco.h * h)
        } else null
        val conFocoAqui = marco != null
        // Las dimensiones siguen el componente incluso al cambiar de pantalla, tema o escala.
        val resorte = spring<Float>(dampingRatio = 0.8f, stiffness = 160f)
        val fx by animateFloatAsState(marco?.x ?: w / 2f, resorte, label = "fx")
        val fy by animateFloatAsState(marco?.y ?: h / 2f, resorte, label = "fy")
        val fw by animateFloatAsState(marco?.ancho ?: 0f, resorte, label = "fw")
        val fh by animateFloatAsState(marco?.alto ?: 0f, resorte, label = "fh")
        val pulso = rememberInfiniteTransition(label = "halo").animateFloat(.55f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "p")
        Canvas(
            Modifier.fillMaxSize()
                .onGloballyPositioned { coordenadas ->
                    val actual = coordenadas.boundsInWindow()
                    if (ventanaCanvas != actual) ventanaCanvas = actual
                }
                .clickable(interactionSource = null, indication = null) { if (reto == null || retoHecho) Diptico.moverNovedades(1) else reaccionar("falta") },
        ) {
            val u = size.width / w
            val velo = Color(0xFF030308).copy(alpha = .74f)
            if (fw <= 1f) drawRect(velo)
            if (fw > 1f) {
                val o = androidx.compose.ui.geometry.Offset(fx * u, fy * u)
                val sz = androidx.compose.ui.geometry.Size(fw * u, fh * u)
                val r = androidx.compose.ui.geometry.CornerRadius(30f * u)
                // El velo con un hueco (par-impar): lo de adentro queda a la vista, sin capas aparte.
                val camino = Path().apply {
                    fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                    addRect(androidx.compose.ui.geometry.Rect(androidx.compose.ui.geometry.Offset.Zero, size))
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(androidx.compose.ui.geometry.Rect(o, sz), r))
                }
                drawPath(camino, velo)
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
        val anchoGlobo = minOf(680f, w * .56f)
        // La altura se mide según el texto real: una altura fija de 300 cortaba el espacio disponible
        // y hacía que el globo se superpusiera a la zona iluminada.
        var globoAltoMedido by remember(k) { mutableStateOf(210f) }
        val altoGlobo = maxOf(globoAltoMedido, 190f)
        val maxX = (w - anchoGlobo - 200f).coerceAtLeast(16f)
        val maxY = (h - altoGlobo - 16f).coerceAtLeast(16f)
        val xCerca = (fx + 20f).coerceIn(16f, maxX)
        val altoRequerido = maxOf(altoGlobo, 180f)
        val (gx, gy) = when {
            !conFocoAqui -> (w / 2f - anchoGlobo / 2f).coerceIn(16f, maxX) to (h / 2f - altoGlobo / 2f).coerceIn(16f, maxY)
            // En el caso de una tarjeta grande, preferimos debajo (fuera del marco).
            fy + fh + altoRequerido + 20f <= h - 16f -> xCerca to (fy + fh + 16f).coerceIn(16f, maxY)
            fy >= altoRequerido + 32f -> xCerca to (fy - altoRequerido - 16f).coerceIn(16f, maxY)
            w - (fx + fw) >= anchoGlobo + 212f -> (fx + fw + 16f).coerceIn(16f, maxX) to (fy + fh / 2f - altoRequerido / 2f).coerceIn(16f, maxY)
            fx >= anchoGlobo + 212f -> (fx - anchoGlobo - 212f).coerceIn(16f, maxX) to (fy + fh / 2f - altoRequerido / 2f).coerceIn(16f, maxY)
            // Si el componente ocupa casi toda la pantalla, la tarjeta se ubica en la zona libre mayor.
            h - fy - fh >= fy -> xCerca to maxY
            else -> xCerca to 16f
        }
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
                .onSizeChanged { tam ->
                    val medido = tam.height / unidad
                    if (kotlin.math.abs(medido - globoAltoMedido) > 1f) globoAltoMedido = medido
                }
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
.padding(horizontal = esc.d(18), vertical = esc.d(8))) {
                        TB(if (retoHecho) "¡Eso! ✓" else pistaReto(k), 19f, color = Color.White, peso = FontWeight.ExtraBold)
                    }
                }
                Spacer(Modifier.weight(1f))
                TB(if (reto != null && !retoHecho) "Hazlo para seguir" else if (k == paginas.size - 1) "A ¡A jugar!" else "A siguiente", 17f, color = Color(0xFF4A4B63), peso = FontWeight.ExtraBold)
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

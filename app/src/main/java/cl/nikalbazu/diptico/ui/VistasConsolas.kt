@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Teclas
import cl.nikalbazu.diptico.estado.*
import kotlin.math.abs

/*
 * Otras formas de ver las consolas (Menú › Estante › Consolas):
 *  - Al costado: una columna de consolas a la izquierda.
 *  - Rueda: las consolas giran en un arco, como un dial.
 *  - Sistemas: primero eliges la consola en un carrusel grande y luego entras a sus juegos (B vuelve).
 * Y una vista de juegos más: el Carrusel, con las carátulas en 3D.
 */

private fun colorConsola(id: String): Color = Consolas.porId(id)?.color ?: Uva
private fun nombreConsola(id: String): String = Consolas.porId(id)?.nombre ?: "Todo"
/** "1 juego" o "N juegos". */
fun juegosTexto(n: Int) = if (n == 1) "1 juego" else "$n juegos"
private fun cuantos(e: Estado, id: String) = if (id == "todo") e.juegos.size else e.juegosPorConsola[id].orEmpty().size

/** Distancia más corta entre dos posiciones de una lista circular (para que la rueda gire por el lado corto). */
private fun distancia(k: Int, pos: Float, n: Int): Float {
    var d = k - pos
    if (n > 1) {
        while (d > n / 2f) d -= n
        while (d < -n / 2f) d += n
    }
    return d
}

/** Consolas en una columna al costado. */
@Composable
fun RielConsolas(e: Estado, modifier: Modifier = Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val lista = rememberLazyListState()
    val sel = e.pestanas.indexOf(e.consolaSel).coerceAtLeast(0)
    LaunchedEffect(sel) { lista.animateScrollToItem((sel - 2).coerceAtLeast(0)) }
    LazyColumn(
        state = lista, modifier = modifier.width(esc.d(86)).fillMaxHeight(),
        contentPadding = PaddingValues(vertical = esc.d(8)), verticalArrangement = Arrangement.spacedBy(esc.d(4)),
    ) {
        itemsIndexed(e.pestanas, key = { _, id -> id }) { _, id ->
            val activa = id == e.consolaSel
            val escala by animateFloatAsState(if (activa) 1.12f else 1f, spring(dampingRatio = 0.5f), label = "riel")
            Row(
                Modifier.fillMaxWidth().padding(horizontal = esc.d(6)).clip(RoundedCornerShape(esc.d(14)))
                    .background(if (activa) colorConsola(id).copy(alpha = 0.22f) else Color.Transparent)
                    .combinedClickable(onLongClick = { Diptico.abrirEditarConsola(id) }) { Diptico.elegirConsola(id) }.padding(vertical = esc.d(6)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(esc.d(3)).height(esc.d(30)).clip(RoundedCornerShape(50)).background(if (activa) Coral else Color.Transparent))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconoConsola(Consolas.porId(id), 28f, p.fondo2, Modifier.graphicsLayer { scaleX = escala; scaleY = escala })
                    Spacer(Modifier.height(esc.d(3)))
                    Texto(nombreConsola(id), 11f, color = if (activa) p.tinta else p.tenue, familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1)
                }
            }
        }
    }
}

/** Las consolas en un arco que gira, como un dial: la elegida al centro y más grande, las demás se curvan hacia atrás. */
@Composable
fun RuedaConsolas(e: Estado, modifier: Modifier = Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val n = e.pestanas.size
    val sel = e.pestanas.indexOf(e.consolaSel).coerceAtLeast(0)
    // La rueda gira con suavidad hacia la consola elegida (por el lado más corto).
    val objetivo = remember { floatArrayOf(sel.toFloat()) }
    objetivo[0] = objetivo[0] + distancia(sel, objetivo[0], n)
    val pos by animateFloatAsState(objetivo[0], spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "rueda")
    val linea = p.linea
    BoxWithConstraints(
        modifier.width(esc.d(150)).fillMaxHeight()
            .pointerInput(Unit) {
                var acumulado = 0f
                detectVerticalDragGestures(onDragEnd = { acumulado = 0f }) { cambio, d ->
                    cambio.consume()
                    acumulado += d
                    if (abs(acumulado) > 46f * density) { Diptico.cambiarConsola(if (acumulado > 0) -1 else 1); acumulado = 0f }
                }
            },
    ) {
        val alto = maxHeight.value / esc.factor
        val paso = (alto / 6.2f).coerceIn(44f, 62f)
        fun x(off: Float) = 54f - 4.2f * off * off
        fun y(off: Float) = alto / 2f + off * paso
        // El arco por donde giran.
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val u = size.height / alto
            val camino = androidx.compose.ui.graphics.Path()
            var k = -3.6f
            camino.moveTo(x(k) * u, y(k) * u)
            while (k < 3.6f) { k += 0.1f; camino.lineTo(x(k) * u, y(k) * u) }
            drawPath(camino, linea, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f * u))
        }
        for (k in 0 until n) {
            val off = distancia(k, pos.mod(n.toFloat()), n)
            if (abs(off) > 3.4f) continue
            val cerca = (1f - abs(off) / 3.4f).coerceIn(0f, 1f)
            val tam = 24f + 18f * (1f - abs(off).coerceAtMost(1f))
            val id = e.pestanas[k]
            val caja = tam + 12f
            androidx.compose.runtime.key(id) { Box(
                Modifier.offset(x = esc.d(x(off) - caja / 2f), y = esc.d(y(off) - caja / 2f)).zIndex(cerca)
                    .graphicsLayer { alpha = 0.3f + 0.7f * cerca }
                    .size(esc.d(caja)).clip(CircleShape)
                    .background(if (k == sel) colorConsola(id).copy(alpha = 0.3f) else p.superficie.copy(alpha = 0.6f))
                    .then(if (k == sel) Modifier.border(esc.d(2), Coral, CircleShape) else Modifier)
                    .combinedClickable(onLongClick = { Diptico.abrirEditarConsola(id) }) { Diptico.elegirConsola(id) },
                contentAlignment = Alignment.Center,
            ) { IconoConsola(Consolas.porId(id), tam * 0.74f, p.fondo2) } }
        }
        // Nombre de la consola elegida, al lado de la rueda.
        Column(Modifier.align(Alignment.CenterStart).padding(start = esc.d(84), end = esc.d(4))) {
            Texto(nombreConsola(e.consolaSel), 14f, lineas = 1)
            Detalle(juegosTexto(cuantos(e, e.consolaSel)), 11f, lineas = 1)
        }
    }
}

/** Sistemas: carrusel grande de consolas, con la elegida al frente y las otras giradas hacia atrás. */
@Composable
fun SistemasCarrusel(e: Estado, modifier: Modifier = Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val n = e.pestanas.size
    val sel = e.pestanas.indexOf(e.consolaSel).coerceAtLeast(0)
    val objetivo = remember { floatArrayOf(sel.toFloat()) }
    objetivo[0] = objetivo[0] + distancia(sel, objetivo[0], n)
    val pos by animateFloatAsState(objetivo[0], tween(380, easing = FastOutSlowInEasing), label = "sistemas")
    Column(modifier.background(Brush.radialGradient(listOf(colorConsola(e.consolaSel).copy(alpha = 0.22f), Color.Transparent)))) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f).pointerInput(Unit) {
                var acumulado = 0f
                detectHorizontalDragGestures(onDragEnd = { acumulado = 0f }) { cambio, d ->
                    cambio.consume()
                    acumulado += d
                    if (abs(acumulado) > 60f * density) { Diptico.cambiarConsola(if (acumulado > 0) -1 else 1); acumulado = 0f }
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            val ancho = maxWidth.value / esc.factor
            val altoCaja = (maxHeight.value / esc.factor * 0.86f).coerceAtMost(250f)
            val anchoCaja = altoCaja * 0.78f
            for (k in 0 until n) {
                val off = distancia(k, pos.mod(n.toFloat()), n)
                if (abs(off) > 2.6f) continue
                val id = e.pestanas[k]
                val frente = k == sel
                val cerca = 1f - (abs(off) / 2.6f).coerceIn(0f, 1f)
                androidx.compose.runtime.key(id) { Box(
                    Modifier.zIndex(10f - abs(off))
                        .offset(x = esc.d(off * anchoCaja * 0.72f))
                        .graphicsLayer {
                            rotationY = (-off * 24f).coerceIn(-40f, 40f)
                            val s = 1f - abs(off).coerceAtMost(2f) * 0.14f
                            scaleX = s; scaleY = s
                            alpha = 0.35f + 0.65f * cerca
                        }
                        .size(esc.d(anchoCaja), esc.d(altoCaja))
                        .then(if (frente) Modifier.shadow(esc.d(18), RoundedCornerShape(esc.d(24)), ambientColor = colorConsola(id), spotColor = colorConsola(id)) else Modifier)
                        .clip(RoundedCornerShape(esc.d(24)))
                        .background(Brush.verticalGradient(listOf(lerp(colorConsola(id), Color.White, 0.08f), lerp(colorConsola(id), Color.Black, 0.55f))))
                        .then(if (frente) Modifier.border(esc.d(2.5f), Color.White.copy(alpha = 0.85f), RoundedCornerShape(esc.d(24))) else Modifier)
                        .clickable { if (frente) Diptico.entrarSistema() else Diptico.elegirConsola(id) },
                    contentAlignment = Alignment.Center,
                ) {
                    e.consolasPropias[id]?.hero?.let { h ->
                        ImagenAnimada(android.net.Uri.parse(h), Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(esc.d(12))) {
                        Box(Modifier.size(esc.d(anchoCaja * 0.5f)).clip(CircleShape).background(Color.Black.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                            IconoConsola(Consolas.porId(id), anchoCaja * 0.34f, Color.Transparent)
                        }
                        Spacer(Modifier.height(esc.d(10)))
                        Texto(nombreConsola(id), 20f, color = Color.White, lineas = 1, sombra = true)
                        Detalle(juegosTexto(cuantos(e, id)), 12.5f, color = Color.White.copy(alpha = 0.8f), lineas = 1)
                    }
                } }
            }
            if (ancho > 0f && n == 0) Detalle("Aquí aparecerán tus consolas", 15f)
        }
        Row(Modifier.fillMaxWidth().padding(bottom = esc.d(6)), horizontalArrangement = Arrangement.Center) {
            Pistas("← →" to "Consola", "A" to "Entrar", "X" to "Personalizar", "Y" to "Al azar")
        }
    }
}

/** Arriba de los juegos, en modo Sistemas: la consola en la que estás y cómo volver. */
@Composable
fun CabeceraSistema(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    if (e.vistaConsolas == "carpetas") {
        Row(Modifier.fillMaxWidth().padding(horizontal = esc.d(12), vertical = esc.d(7)), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(esc.d(28)).clip(CircleShape).clickable { Diptico.volverASistemas() }, contentAlignment = Alignment.Center) { Texto("‹", 24f) }
            Texto(Consolas.porId(e.consolaSel)?.nombreLargo ?: "Todos tus juegos", 15f, lineas = 1,
                alinear = TextAlign.Center, modifier = Modifier.weight(1f))
            if (esConsolaNativa(e.consolaSel)) AccesoNativos(e)
            Box(Modifier.size(esc.d(28)).clip(CircleShape).clickable { Diptico.alternarModoCarpeta() }, contentAlignment = Alignment.Center) { Texto(if (e.modoCarpeta == "completa") "↙" else "↗", 17f) }
        }
        return
    }
    Row(Modifier.fillMaxWidth().padding(start = esc.d(12), end = esc.d(16), top = esc.d(8), bottom = esc.d(4)), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(p.superficie).clickable { Diptico.volverASistemas() }
                .padding(start = esc.d(6), end = esc.d(12), top = esc.d(5), bottom = esc.d(5)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(esc.d(22)).clip(CircleShape).background(p.fondo), contentAlignment = Alignment.Center) { Texto("B", 11f, traducir = false) }
            Spacer(Modifier.width(esc.d(6)))
            Texto("Consolas", 12.5f, lineas = 1)
        }
        Spacer(Modifier.width(esc.d(12)))
        IconoConsola(Consolas.porId(e.consolaSel), 26f, p.fondo2)
        Spacer(Modifier.width(esc.d(8)))
        Column(Modifier.weight(1f)) {
            Texto(Consolas.porId(e.consolaSel)?.nombreLargo ?: "Todos tus juegos", 15f, lineas = 1)
            Detalle(juegosTexto(e.visibles.size), 11.5f, lineas = 1)
        }
        if (esConsolaNativa(e.consolaSel)) AccesoNativos(e)
        if (e.vistaConsolas == "carpetas") {
            val ventana = e.modoCarpeta != "completa"
            Box(Modifier.clip(RoundedCornerShape(50)).background(p.superficie)
                .clickable { Diptico.alternarModoCarpeta() }
                .padding(horizontal = esc.d(10), vertical = esc.d(6))) {
                Texto(if (ventana) "Ampliar" else "Abrir en ventana", 11.5f, lineas = 1)
            }
        }
    }
}

/** En la carpeta de Switch 2 o de Ports: abre el centro de Switch 2 y ports nativos. */
@Composable
private fun AccesoNativos(e: Estado) {
    val esc = LocalEscala.current
    val pantalla = pantallaActual()
    Box(
        Modifier.padding(horizontal = esc.d(6)).clip(RoundedCornerShape(50)).background(Color(0xFF00C3E3).copy(alpha = 0.18f))
            .clickable { Diptico.abrirNativos(pantalla, if (e.consolaSel == Consolas.PORTS) 1 else 0) }
            .padding(horizontal = esc.d(10), vertical = esc.d(6)).testTag("acceso-nativos"),
    ) { Texto(if (e.consolaSel == Consolas.PORTS) "Ports nativos" else "Centro Switch 2", 11.5f, lineas = 1) }
}

/** En la otra pantalla, en modo Sistemas: la consola elegida en grande, con lo último que jugaste en ella. */
@Composable
fun VistaSistema(e: Estado, completa: Boolean = true, conPistas: Boolean = true) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val id = e.consolaSel
    val c: Consola? = Consolas.porId(id)
    val color = colorConsola(id)
    val juegos = if (id == "todo") e.juegos else e.juegosPorConsola[id].orEmpty()
    val tiempo = juegos.sumOf { e.datos[it.clave]?.tiempo ?: 0L }
    val ultimo = juegos.maxByOrNull { e.datos[it.clave]?.ultimo ?: 0L }?.takeIf { (e.datos[it.clave]?.ultimo ?: 0L) > 0 }
    val propia = e.consolasPropias[id]
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(lerp(p.fondo, color, 0.35f), p.fondo, p.fondo)))) {
        // La imagen grande propia de la consola (si la tiene), con un degradado para leer bien el texto.
        if (propia?.hero != null) {
            ImagenAnimada(android.net.Uri.parse(propia.hero), Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to p.fondo.copy(alpha = 0.92f), 0.5f to p.fondo.copy(alpha = 0.55f), 1f to Color.Transparent)))
        }
        // Un ícono gigante de la consola, de fondo.
        if (completa && propia?.hero == null) Box(Modifier.align(Alignment.CenterEnd).padding(end = esc.d(30)).graphicsLayer { alpha = 0.16f; rotationZ = -8f }) {
            IconoConsola(c, 300f, Color.Transparent)
        }
        TramaTema()
        BarraSuperior(Modifier.align(Alignment.TopStart), inicio = 32f, trabajos = e.trabajos) {
            if (e.sesion == null) Box(Modifier.padding(end = LocalEscala.current.d(10))) { BotonPerfil(30f) }
        }
        Column(Modifier.align(Alignment.CenterStart).padding(start = esc.d(36), end = esc.d(36)).fillMaxWidth(if (completa) 0.62f else 0.46f)) {
            Etiqueta(if (id == "todo") "Toda tu colección" else "Consola", Coral, 12f)
            Spacer(Modifier.height(esc.d(4)))
            // El logo propio reemplaza al nombre.
            if (propia?.logo != null) ImagenAnimada(android.net.Uri.parse(propia.logo), Modifier.height(esc.d(90)).fillMaxWidth(), recortar = false)
            else Texto(c?.bonito ?: "Todos tus juegos", 38f, lineas = 2, interlineado = 42f, sombra = true)
            Spacer(Modifier.height(esc.d(10)))
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                Dato("${juegos.size}", if (juegos.size == 1) "juego" else "juegos")
                Dato(if (tiempo > 0) formatoTiempo(tiempo) else "0 min", "jugado")
                Dato("${juegos.count { e.datos[it.clave]?.favorito == true }}", "favoritos")
            }
            if (ultimo != null) {
                Spacer(Modifier.height(esc.d(16)))
                Row(
                    Modifier.clip(RoundedCornerShape(esc.d(16))).background(p.superficie.copy(alpha = 0.8f)).padding(esc.d(10)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Consolas.porId(ultimo.consolaId)?.let { CajaJuego(ultimo, it, escala = 0.5f) }
                    Spacer(Modifier.width(esc.d(12)))
                    Column {
                        Etiqueta("Lo último que jugaste aquí", p.tenue, 10.5f)
                        Texto(ultimo.titulo, 16f, lineas = 2)
                    }
                }
            }
        }
        if (completa && conPistas) Row(Modifier.align(Alignment.BottomStart).padding(start = esc.d(36), bottom = esc.d(22))) {
            Pistas("A" to "Entrar", "← →" to "Otra consola")
        }
    }
}

@Composable
private fun Dato(valor: String, texto: String) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column(
        Modifier.clip(RoundedCornerShape(esc.d(14))).background(p.superficie.copy(alpha = 0.75f)).padding(horizontal = esc.d(14), vertical = esc.d(8)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Texto(valor, 20f, lineas = 1)
        Detalle(texto, 11.5f, lineas = 1)
    }
}

/** Carrusel de juegos: la carátula elegida al frente y las vecinas giradas en 3D, como un tocadiscos de portadas. */
@Composable
fun CarruselJuegos(e: Estado, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val t = LocalTamJuegos.current
    val lista = e.visibles
    val sel = e.indiceSel
    val pos by animateFloatAsState(sel.toFloat(), tween(320, easing = FastOutSlowInEasing), label = "carrusel")
    Column(modifier) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f).pointerInput(lista.size) {
                var acumulado = 0f
                detectHorizontalDragGestures(onDragEnd = { acumulado = 0f }) { cambio, d ->
                    cambio.consume()
                    acumulado += d
                    if (abs(acumulado) > 50f * density) { Diptico.moverJuego(if (acumulado > 0) -1 else 1); acumulado = 0f }
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            if (lista.isEmpty()) {
                Detalle(if (e.escaneando) "Buscando tus juegos…" else "Aquí aparecerán tus juegos", 15f)
                return@BoxWithConstraints
            }
            val alto = (maxHeight.value / esc.factor * 0.82f).coerceAtMost(220f * t)
            // La ventana de cajas sigue a la posición animada (no a la elegida): así ninguna aparece o desaparece de golpe.
            val desde = (kotlin.math.floor(pos).toInt() - 5).coerceAtLeast(0)
            val hasta = (kotlin.math.ceil(pos).toInt() + 5).coerceAtMost(lista.size - 1)
            for (k in desde..hasta) {
                val j = lista[k]
                val c = Consolas.porId(j.consolaId) ?: continue
                // Con la clave del juego, cada caja conserva su imagen al moverse (antes se recargaban y parpadeaban).
                androidx.compose.runtime.key(j.clave) {
                val off = k - pos
                val base = cajaDe(c)
                val escala = alto / base.alto
                val frente = k == sel
                Box(
                    Modifier.zIndex(10f - abs(off))
                        .graphicsLayer {
                            translationX = (off.coerceIn(-1f, 1f) * base.ancho * escala * 0.78f + (off - off.coerceIn(-1f, 1f)) * base.ancho * escala * 0.34f) * esc.factor * density
                            rotationY = -off.coerceIn(-1f, 1f) * 42f
                            val s = if (abs(off) < 1f) 1f - abs(off) * 0.18f else 0.82f
                            scaleX = s; scaleY = s
                            // Se desvanecen suave en los bordes.
                            alpha = ((4.6f - abs(off)) / 1.2f).coerceIn(0f, 1f) * (1f - (abs(off) - 1f).coerceAtLeast(0f) * 0.18f).coerceIn(0f, 1f)
                        }
                        .clickable { if (frente) Diptico.jugar() else Diptico.elegirJuego(j.clave) },
                ) { CajaJuego(j, c, escala = escala, vivo = frente) }
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

/**
 * Cuadrícula de carpetas de consola. En Carpetas, cada celda es un ícono y A abre sus juegos.
 */
@Composable
fun CarpetasConsolas(e: Estado, modifier: Modifier = Modifier, interactiva: Boolean = true) {
    val esc = LocalEscala.current
    val densidad = LocalDensity.current
    val ids = e.pestanas
    val sel = ids.indexOf(e.consolaSel).coerceAtLeast(0)
    val conZoom = e.vistaConsolas == "carpetas"
    val horizontal = conZoom && e.carpetasHorizontal
    val grilla = rememberLazyGridState()
    var tam by remember(e.tamCarpetas) { mutableFloatStateOf(e.tamCarpetas) }
    var ajustando by remember { androidx.compose.runtime.mutableStateOf(false) }
    val animado by animateFloatAsState(tam, tween(duracionMovimiento(if (ajustando) 60 else 220)), label = "densidad carpetas")
    // Si se sale de las carpetas, el modo edición termina solo.
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { if (interactiva) Diptico.terminarOrdenCarpetas() } }
    Column(modifier) {
        if (e.extra.ordenandoCarpetas && interactiva) BarraOrdenar(e) else CabeceraCarpetas(e, interactiva)
        if (ajustando) ParadasDensidad(cl.nikalbazu.diptico.estado.Densidades.carpetas, tam, { Diptico.guardarTamCarpetas(it) })
        val zona = Modifier.fillMaxWidth().weight(1f)
        BoxWithConstraints(if (conZoom && interactiva) zona.ajustarDensidad(
            iniciar = { ajustando = true },
            ajustar = { tam = Diptico.limitarTamCarpetas(tam * it) },
            terminar = { guardar ->
                if (guardar) Diptico.guardarTamCarpetas(tam)
                tam = if (guardar) cl.nikalbazu.diptico.estado.Densidades.carpetas.masCercana(tam).escala else e.tamCarpetas
                ajustando = false
            },
        ) else zona) {
            val anchoU = maxWidth.value / esc.factor
            val columnas = when (e.vistaConsolas) {
                "mosaico" -> if (anchoU > 620f) 5 else if (anchoU > 300f) 3 else 2
                "vitrina" -> if (anchoU > 560f) 2 else 1
                else -> ((anchoU - 18f) / 83f).toInt().coerceIn(3, 7)
            }
            val separacionX = esc.d(18)
            val separacionY = esc.d(12)
            val ancho = with(densidad) { constraints.maxWidth - 2 * esc.d(14).roundToPx() }
            val alto = with(densidad) { constraints.maxHeight - esc.d(8).roundToPx() - esc.d(10).roundToPx() }
            val lado = with(densidad) {
                val base = (ancho - (columnas - 1) * separacionX.roundToPx()) / columnas
                (base.toDp() * animado).coerceIn(1.dp, minOf(ancho, (alto - esc.d(24).roundToPx()).coerceAtLeast(1)).toDp().coerceAtLeast(1.dp))
            }
            val celdas = if (conZoom) GridCells.FixedSize(lado) else GridCells.Fixed(columnas)
            val carriles = if (!conZoom) columnas else with(densidad) {
                val espacio = if (horizontal) (alto - esc.d(24).roundToPx()).coerceAtLeast(0) else ancho
                val separacion = (if (horizontal) separacionY + esc.d(24) else separacionX).roundToPx()
                with(celdas) { calculateCrossAxisCellSizes(espacio.coerceAtLeast(0), separacion).size }
            }
            SideEffect { Teclas.carrilesCarpetas = carriles }
            LaunchedEffect(sel, horizontal) { grilla.mostrar(sel, carriles) }
            val relleno = PaddingValues(start = esc.d(14), end = esc.d(14), top = esc.d(8), bottom = esc.d(10) + LocalAireAbajo.current)
            if (conZoom) {
                val ordenando = e.extra.ordenandoCarpetas && interactiva
                // Cuánto mide un paso entre carpetas (para saber dónde se soltó la que arrastras).
                val pasoX = with(densidad) { (lado + separacionX).toPx() }
                val pasoY = with(densidad) { (lado + separacionY + esc.d(22)).toPx() }
                RejillaCentrada(ids.size, carriles, sel, separacionX, separacionY, relleno,
                    Modifier.fillMaxSize(), interactiva && !ordenando, horizontal, { ids[it] }) { k ->
                    val id = ids[k]
                    var arrastre by remember(id) { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                    var arrastrando by remember(id) { androidx.compose.runtime.mutableStateOf(false) }
                    Box(
                        Modifier.zIndex(if (arrastrando) 5f else 0f)
                            .graphicsLayer { translationX = arrastre.x; translationY = arrastre.y }
                            .then(if (ordenando && id != "todo") Modifier.pointerInput(id, k, carriles, horizontal) {
                                detectDragGestures(
                                    onDragStart = { arrastrando = true; Diptico.elegirConsola(id) },
                                    onDragEnd = {
                                        val dx = kotlin.math.round(arrastre.x / pasoX).toInt()
                                        val dy = kotlin.math.round(arrastre.y / pasoY).toInt()
                                        val paso = if (horizontal) dy + dx * carriles else dx + dy * carriles
                                        arrastre = androidx.compose.ui.geometry.Offset.Zero
                                        arrastrando = false
                                        Diptico.soltarCarpetaEn(id, k + paso)
                                    },
                                    onDragCancel = { arrastre = androidx.compose.ui.geometry.Offset.Zero; arrastrando = false },
                                ) { cambio, d -> cambio.consume(); arrastre += d }
                            } else Modifier),
                    ) {
                        CarpetaBiblioteca(e, id, k == sel, interactiva, lado, tomada = arrastrando || (ordenando && e.extra.carpetaTomada == id))
                    }
                }
            } else LazyVerticalGrid(columns = celdas, state = grilla, userScrollEnabled = interactiva,
                modifier = Modifier.fillMaxSize(), contentPadding = relleno,
                verticalArrangement = Arrangement.spacedBy(separacionY), horizontalArrangement = Arrangement.spacedBy(separacionX)) {
                items(ids.size, key = { ids[it] }) { k -> CarpetaVisual(e, ids[k], k == sel, amplia = e.vistaConsolas == "vitrina") }
            }
        }
    }
}

@Composable
private fun CarpetaVisual(e: Estado, id: String, elegida: Boolean, amplia: Boolean) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val consola = Consolas.porId(id)
    val color = colorConsola(id)
    val juegos = remember(e.juegos, e.datos, id) {
        val deConsola = if (id == "todo") e.juegos else e.juegosPorConsola[id].orEmpty()
        deConsola.filter { e.datos[it.clave]?.oculto != true }
    }
    val forma = RoundedCornerShape(esc.d(16))
    Column(
        Modifier.fillMaxWidth()
            .then(if (elegida) Modifier.shadow(esc.d(14), forma, ambientColor = color, spotColor = color) else Modifier)
            .clip(forma).background(p.superficie)
            .border(esc.d(if (elegida) 3f else 1f), if (elegida) Color.White else p.linea, forma)
            .combinedClickable(onLongClick = { Diptico.abrirEditarConsola(id) }) {
                if (elegida) Diptico.entrarSistema() else Diptico.elegirConsola(id)
            },
    ) {
        Row(Modifier.fillMaxWidth().height(esc.d(if (amplia) 100f else 78f))) {
            PortadaCarpeta(e, id, Modifier.fillMaxSize())
        }
        Row(Modifier.padding(esc.d(8)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
            IconoConsola(consola, 22f, p.fondo2)
            Column(Modifier.weight(1f)) {
                Texto(nombreConsola(id), 14f, lineas = 1)
                Detalle(if (juegos.size == 1) "1 juego" else "${juegos.size} juegos", 11f, lineas = 1)
            }
        }
    }
}

/** Arriba de las carpetas mientras las ordenas: qué hacer y el botón «Listo». */
@Composable
private fun BarraOrdenar(e: Estado) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = esc.d(14), vertical = esc.d(6)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10)),
    ) {
        Column(Modifier.weight(1f)) {
            Texto("Ordenar carpetas", 17f, peso = FontWeight.Black, lineas = 1)
            Detalle(if (e.extra.carpetaTomada != null) "Mueve con la cruceta · A la suelta" else "Arrastra con el dedo, o A para tomar una · X personaliza", 11f, lineas = 1)
        }
        Box(
            Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(50)).background(Uva)
                .clickable { Diptico.terminarOrdenCarpetas() }.padding(horizontal = esc.d(14), vertical = esc.d(6)),
        ) { Texto("B · Listo", 13f, color = SobreUva, peso = FontWeight.Black, lineas = 1) }
    }
}

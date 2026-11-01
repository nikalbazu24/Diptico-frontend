@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.zIndex
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.blur
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.audio.Medios
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Temas
import cl.nikalbazu.diptico.estado.Widget
import cl.nikalbazu.diptico.estado.Widgets

/** Saludo según la hora del día. */
private fun saludo(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..11 -> "Buenos días"
        in 12..19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

/**
 * Inicio personalizado: widgets en una grilla de 6 x 4 que se mueven libremente y tienen 10 tamaños.
 * En modo edición se arrastran con el dedo o se mueven con la cruceta.
 */
@Composable
fun PantallaInicio(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(Modifier.fillMaxSize().background(p.fondo)) {
        // Fondo: el arte del juego elegido, difuminado, con un brillo del color del tema.
        if (e.fondoInicio != null) {
            ImagenAnimada(android.net.Uri.parse(e.fondoInicio), Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.fondo.copy(alpha = 0.15f), p.fondo.copy(alpha = 0.7f)))))
        } else {
            e.seleccionado?.let { j ->
                ArteJuego(j, preferirArte = true, maxLado = 900, modifier = Modifier.fillMaxSize().then(if (Estilo.efectosCompletos) Modifier.blur(28.dp) else Modifier).graphicsLayer { alpha = 0.32f })
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.fondo.copy(alpha = 0.45f), p.fondo.copy(alpha = 0.92f)))))
        }
        Box(Modifier.align(Alignment.TopEnd).size(esc.d(420)).background(Brush.radialGradient(listOf(Uva.copy(alpha = 0.18f), Color.Transparent))))
        BarraSuperior(Modifier.align(Alignment.TopStart), inicio = 20f, trabajos = e.trabajos)

        Column(Modifier.fillMaxSize().padding(start = esc.d(14), end = esc.d(14), top = esc.d(48), bottom = esc.d(8))) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    if (e.editandoWidgets || e.saludoInicio) Texto(if (e.editandoWidgets) "Acomodando tu inicio" else saludo(), 24f, lineas = 1)
                    if (!e.editandoWidgets && e.saludoInicio) Detalle( cl.nikalbazu.diptico.estado.Idioma.fecha("EEEEdMMMM").replaceFirstChar { it.uppercase() } +
                            (e.fiesta?.let { " · ${it.nombre}" } ?: ""),
                        13.5f, lineas = 1,
                    )
                }
                if (!e.editandoWidgets) {
                    BotonBisa(Modifier.padding(end = esc.d(10)))
                }
                if (!e.editandoWidgets) {
                    if (e.widgetFoco >= 0) {
                        Pista("A", "Abrir")
                        Spacer(Modifier.width(esc.d(8)))
                        Pista("B", "Salir")
                    } else Pista(if (e.unaPantalla) "✥" else "↑", "Widgets")
                    Spacer(Modifier.width(esc.d(10)))
                    Pista("Select", "Carátulas")
                    Spacer(Modifier.width(esc.d(10)))
                }
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(if (e.editandoWidgets) Uva else p.superficie.copy(alpha = 0.85f))
                        .clickable { if (e.editandoWidgets) Diptico.terminarEdicion() else Diptico.editarWidgets() }
                        .padding(horizontal = esc.d(12), vertical = esc.d(6)),
                ) { Texto(if (e.editandoWidgets) "Listo" else "Editar", 13.5f, color = if (e.editandoWidgets) SobreUva else p.tinta) }
            }
            if (e.editandoWidgets) {
                Spacer(Modifier.height(esc.d(2)))
                Detalle(
                    if (e.unaPantalla) "Arrastra los widgets con el dedo · todo lo demás en el estudio de la derecha"
                    else "Arrastra los widgets con el dedo · todo lo demás en el estudio, en la otra pantalla",
                    12f, lineas = 1,
                )
            }
            Spacer(Modifier.height(esc.d(10)))
            // Páginas: se deslizan a los lados (con el dedo, o L2/R2).
            val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = e.paginaInicio) { e.paginas }
            LaunchedEffect(e.paginaInicio) { if (pager.currentPage != e.paginaInicio) pager.animateScrollToPage(e.paginaInicio) }
            LaunchedEffect(pager.settledPage) { if (pager.settledPage != Diptico.estado.value.paginaInicio) Diptico.irAPagina(pager.settledPage) }
            androidx.compose.foundation.pager.HorizontalPager(
                state = pager, modifier = Modifier.fillMaxWidth().weight(1f),
                userScrollEnabled = !e.editandoWidgets, pageSpacing = esc.d(20), beyondViewportPageCount = 0,
            ) { pagina -> Grilla(e, pagina, Modifier.fillMaxSize()) }
            if (e.paginas > 1) {
                Row(Modifier.align(Alignment.CenterHorizontally).padding(top = esc.d(6)), horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    for (k in 0 until e.paginas) {
                        val activa = k == e.paginaInicio
                        Box(
                            Modifier.size(if (activa) esc.d(22) else esc.d(8), esc.d(8)).clip(RoundedCornerShape(50))
                                .background(if (activa) Uva else p.linea).clickable { Diptico.irAPagina(k) },
                        )
                    }
                }
            }
        }
    }
}

/** La grilla de widgets, con su modo de edición. */
@Composable
private fun Grilla(e: Estado, pagina: Int, modifier0: Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    // Margen de seguridad: el marco y el agrandado del widget elegido siempre se ven completos.
    val modifier = modifier0.padding(esc.d(8))
    val densidad = LocalDensity.current
    // Con más de 4 filas, la página se desplaza hacia abajo.
    val desplazar = rememberScrollState()
    BoxWithConstraints(modifier) {
        val hueco = e.huecoInicio.toFloat()
        val anchoU = maxWidth.value / esc.factor
        val visibles = minOf(Widgets.FILAS, 4)
        val altoU = maxHeight.value / esc.factor
        val celdaAn = (anchoU - hueco * (Widgets.COLUMNAS - 1)) / Widgets.COLUMNAS
        val celdaAl = (altoU - hueco * (visibles - 1)) / visibles
        val pasoX = with(densidad) { esc.d(celdaAn + hueco).toPx() }
        val pasoY = with(densidad) { esc.d(celdaAl + hueco).toPx() }
        Box(
            Modifier.fillMaxSize().verticalScroll(desplazar, enabled = Widgets.FILAS > 4 && !e.editandoWidgets)
                // Mantener apretado: fuera del editor entra a acomodar; dentro, abre la burbuja para agregar ahí.
                .pointerInput(e.editandoWidgets, pasoX, pasoY) {
                    if (!e.editandoWidgets) detectTapGestures(onLongPress = { Diptico.editarWidgets() })
                    else detectTapGestures(
                        onLongPress = { o -> Diptico.abrirBurbuja((o.x / pasoX).toInt(), (o.y / pasoY).toInt()) },
                        onTap = { Diptico.cerrarBurbuja() },
                    )
                },
        ) {
        // Alto total de la grilla (más alto que la pantalla si hay más de 4 filas).
        Box(Modifier.size(esc.d(anchoU), esc.d(Widgets.FILAS * celdaAl + (Widgets.FILAS - 1) * hueco)))

        if (e.editandoWidgets) {
            // Celdas de fondo para ver dónde cae cada widget.
            for (x in 0 until Widgets.COLUMNAS) for (y in 0 until Widgets.FILAS) {
                Box(
                    Modifier.offset(x = esc.d(x * (celdaAn + hueco)), y = esc.d(y * (celdaAl + hueco)))
                        .size(esc.d(celdaAn), esc.d(celdaAl)).clip(RoundedCornerShape(esc.d(14)))
                        .border(esc.d(1.5f), p.linea, RoundedCornerShape(esc.d(14))),
                )
            }
        }
        e.widgets.forEachIndexed { i, w ->
            if (!w.visible || w.pagina != pagina) return@forEachIndexed
            key(w.tipo) {
                val elegido = e.editandoWidgets && i == e.widgetSel
                // Con la cruceta en los widgets: el que tiene el foco se levanta y brilla.
                val enfocado = !e.editandoWidgets && i == e.widgetFoco && e.panel == cl.nikalbazu.diptico.estado.Panel.NINGUNO
                var arrastre by remember { mutableStateOf(Offset.Zero) }
                val x by animateDpAsState(esc.d(w.x * (celdaAn + hueco)), spring(dampingRatio = 0.75f, stiffness = 500f), label = "x")
                val y by animateDpAsState(esc.d(w.y * (celdaAl + hueco)), spring(dampingRatio = 0.75f, stiffness = 500f), label = "y")
                val an by animateDpAsState(esc.d(w.an * celdaAn + (w.an - 1) * hueco), spring(dampingRatio = 0.8f), label = "an")
                val al by animateDpAsState(esc.d(w.al * celdaAl + (w.al - 1) * hueco), spring(dampingRatio = 0.8f), label = "al")
                val tam by animateFloatAsState(if (elegido) 1.03f else if (enfocado) 1.045f else 1f, spring(dampingRatio = 0.5f), label = "tam")
                // En modo edición los widgets tiemblan un poquito (cada uno a su ritmo), como en los teléfonos.
                val tiemble = if (e.editandoWidgets) {
                    val t = rememberInfiniteTransition(label = "tiemble")
                    t.animateFloat(-1f, 1f, infiniteRepeatable(tween(260 + (i % 4) * 35), RepeatMode.Reverse), label = "rot").value
                } else 0f
                Box(
                    Modifier.offset(x = x, y = y).size(an, al)
                        .zIndex(if (elegido || enfocado) 2f else 1f)
                        .graphicsLayer {
                            translationX = arrastre.x; translationY = arrastre.y; scaleX = tam; scaleY = tam
                            rotationZ = if (elegido) tiemble * 0.4f else tiemble * 0.8f
                        }
                        .then(if (elegido) Modifier.border(esc.d(3), if (e.estudioMover) Coral else Uva, RoundedCornerShape(esc.d((w.radio + 2).toFloat()))) else Modifier)
                        .then(
                            if (enfocado) Modifier.shadow(esc.d(18), RoundedCornerShape(esc.d(w.radio.toFloat())), ambientColor = Uva, spotColor = Uva)
                                .border(esc.d(3), if (p == PaletaOscura) Color.White else Uva, RoundedCornerShape(esc.d((w.radio + 2).toFloat())))
                            else Modifier,
                        ),
                ) {
                    // Cada widget puede agrandar o achicar su contenido (Tamaño del contenido).
                    // El contenido crece un poco cuando el widget es más grande que su tamaño de siempre.
                    val auto = remember(w.tipo, w.an, w.al) { escalaAuto(w) }
                    CompositionLocalProvider(LocalWidget provides w, LocalEscala provides Escala(esc.factor * w.escala * auto, esc.ancho, esc.texto)) {
                        ContenidoWidget(e, w, w.an, w.al)
                    }
                    if (e.editandoWidgets) {
                        // Capa para arrastrar con el dedo (tapa los botones del widget mientras se edita).
                        Box(
                            Modifier.fillMaxSize().clip(RoundedCornerShape(esc.d(22)))
                                .background(if (elegido) Uva.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.18f))
                                .pointerInput(w.tipo, pasoX, pasoY) {
                                    detectDragGestures(
                                        onDragStart = { Diptico.elegirWidget(i) },
                                        onDragEnd = {
                                            val w0 = Diptico.estado.value.widgets[i]
                                            Diptico.ponerWidget(i, w0.x + (arrastre.x / pasoX).roundToInt(), w0.y + (arrastre.y / pasoY).roundToInt())
                                            arrastre = Offset.Zero
                                        },
                                        onDragCancel = { arrastre = Offset.Zero },
                                    ) { cambio, d ->
                                        cambio.consume()
                                        arrastre += d
                                    }
                                }
                                .clickable { Diptico.elegirWidget(i); Diptico.abrirAjustesWidget(i) },
                        ) {
                            if (elegido) {
                                // Manija para estirarlo libremente con el dedo (ancho y alto a la vez).
                                var estirar by remember { mutableStateOf(Offset.Zero) }
                                val nAn = (w.an + (estirar.x / pasoX).roundToInt()).coerceIn(1, Widgets.COLUMNAS - w.x)
                                val nAl = (w.al + (estirar.y / pasoY).roundToInt()).coerceIn(1, Widgets.FILAS - w.y)
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(esc.d(6)).clip(RoundedCornerShape(50))
                                        .background(if (e.estudioMover || estirar != Offset.Zero) Coral else Uva)
                                        .padding(horizontal = esc.d(8), vertical = esc.d(3)),
                                ) { Texto("$nAn × $nAl", 11.5f, color = SobreUva, lineas = 1, traducir = false) }
                                Box(
                                    Modifier.align(Alignment.BottomEnd).padding(esc.d(4)).size(esc.d(30))
                                        .graphicsLayer { translationX = estirar.x; translationY = estirar.y }
                                        .clip(CircleShape).background(if (e.estudioMover) Coral else Uva)
                                        .border(esc.d(2), Color.White, CircleShape)
                                        .pointerInput(w.tipo, pasoX, pasoY) {
                                            detectDragGestures(
                                                onDragEnd = {
                                                    val w0 = Diptico.estado.value.widgets[i]
                                                    Diptico.redimensionarWidget(i, w0.an + (estirar.x / pasoX).roundToInt(), w0.al + (estirar.y / pasoY).roundToInt())
                                                    estirar = Offset.Zero
                                                },
                                                onDragCancel = { estirar = Offset.Zero },
                                            ) { cambio, d -> cambio.consume(); estirar += d }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) { IconoDiptico(Icono.FORMA, Color.White, 14f) }
                            }
                        }
                    }
                }
            }
        }
    }
        // Burbuja para agregar un widget: sale desde la celda donde mantuviste el dedo (o el widget elegido).
        val b = e.burbuja
        if (e.editandoWidgets && b != null && pagina == e.paginaInicio) {
            BurbujaAgregar(e, b, celdaAn, celdaAl, hueco, anchoU, altoU)
        }
    }
}

/** La burbuja de "agregar": un globo que crece desde la celda tocada, con burbujas de cada widget disponible. */
@Composable
private fun BurbujaAgregar(e: Estado, celda: Pair<Int, Int>, celdaAn: Float, celdaAl: Float, hueco: Float, anchoU: Float, altoU: Float) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val opciones = remember(e.widgets) { Diptico.opcionesBurbuja(e) }
    val crecer = remember(celda) { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(celda) { crecer.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f)) }
    val cx = celda.first * (celdaAn + hueco) + celdaAn / 2
    val cy = celda.second * (celdaAl + hueco) + celdaAl / 2
    val cols = 4
    val anchoB = minOf(anchoU - 16f, 4 * 92f + 24f)
    val filas = (opciones.size + cols - 1) / cols
    val altoB = minOf(altoU - 10f, filas * 86f + 54f)
    // El globo se ubica para no salirse de la pantalla, y crece desde el punto tocado.
    val x0 = (cx - anchoB / 2).coerceIn(4f, anchoU - anchoB - 4f)
    val y0 = (if (cy > altoU / 2) cy - altoB - 20f else cy + 20f).coerceIn(2f, altoU - altoB - 2f)
    val origen = androidx.compose.ui.graphics.TransformOrigin(((cx - x0) / anchoB).coerceIn(0f, 1f), ((cy - y0) / altoB).coerceIn(0f, 1f))
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f * crecer.value.coerceIn(0f, 1f))).clickable(interactionSource = null, indication = null) { Diptico.cerrarBurbuja() })
    // El punto de origen.
    Box(Modifier.offset(x = esc.d(cx - 9), y = esc.d(cy - 9)).size(esc.d(18)).graphicsLayer { scaleX = crecer.value; scaleY = crecer.value }.clip(CircleShape).background(Coral))
    Column(
        Modifier.offset(x = esc.d(x0), y = esc.d(y0)).size(esc.d(anchoB), esc.d(altoB))
            .graphicsLayer { scaleX = crecer.value; scaleY = crecer.value; alpha = crecer.value.coerceIn(0f, 1f); transformOrigin = origen }
            .shadow(esc.d(18), RoundedCornerShape(esc.d(30)))
            .clip(RoundedCornerShape(esc.d(30))).background(p.fondo2.copy(alpha = 0.97f)).border(esc.d(1.5f), Uva.copy(alpha = 0.5f), RoundedCornerShape(esc.d(30)))
            .sinCerrar().padding(esc.d(12)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoDiptico(Icono.APPS, Coral, 16f)
            Spacer(Modifier.width(esc.d(6)))
            Texto("Agregar aquí", 14f, lineas = 1, modifier = Modifier.weight(1f))
            Pista("B", "Cerrar")
        }
        Spacer(Modifier.height(esc.d(8)))
        val grilla = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
        LaunchedEffect(e.burbujaSel) { grilla.mostrar(e.burbujaSel) }
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(cols), state = grilla,
            verticalArrangement = Arrangement.spacedBy(esc.d(6)), horizontalArrangement = Arrangement.spacedBy(esc.d(4)),
        ) {
            items(opciones.size) { k ->
                val o = opciones[k]
                val sel = k == e.burbujaSel
                val aparece = remember { androidx.compose.animation.core.Animatable(0f) }
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(40L * k.coerceAtMost(12)); aparece.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f)) }
                Column(
                    Modifier.graphicsLayer { scaleX = aparece.value; scaleY = aparece.value }.clip(RoundedCornerShape(esc.d(16)))
                        .clickable { Diptico.elegirBurbuja(k) }.padding(vertical = esc.d(4)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(esc.d(if (sel) 52f else 46f)).clip(CircleShape)
                            .then(if (sel) Modifier.fondoBoton(Uva) else Modifier.background(p.superficie))
                            .border(esc.d(if (sel) 2.5f else 1f), if (sel) Color.White else p.linea, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { IconoDiptico(o.icono, if (sel) SobreUva else p.tinta, 22f, acento = Coral) }
                    Spacer(Modifier.height(esc.d(3)))
                    Texto(o.nombre, 10.5f, lineas = 2, alinear = TextAlign.Center, color = if (sel) p.tinta else p.tenue, interlineado = 12f)
                }
            }
        }
    }
}

/** El contenido de cada widget según su tipo y su tamaño (1×1, angosto, una fila o grande). */
@Composable
private fun ContenidoWidget(e: Estado, w: cl.nikalbazu.diptico.estado.Widget, an: Int, al: Int) {
    when {
        w.tipo.startsWith("imagen") -> WidgetImagen(w)
        w.tipo.startsWith("video") -> WidgetVideo(w, e.sesion == null)
        an == 1 && w.tipo != "asistente" -> WidgetMini(e, w.tipo, al)
        w.tipo == "asistente" && an == 1 -> WidgetMini(e, w.tipo, al)
        else -> WidgetVista(e, w.tipo, an, al)
    }
}

/** Qué tanto crece el contenido según cuánto más grande es el widget que su tamaño de siempre. */
private fun escalaAuto(w: cl.nikalbazu.diptico.estado.Widget): Float {
    val base = cl.nikalbazu.diptico.estado.Widgets.porDefecto.firstOrNull { it.tipo == w.tipo } ?: return 1f
    val r = kotlin.math.sqrt((w.an * w.al).toFloat() / (base.an * base.al).toFloat())
    return if (r > 1f) r.coerceAtMost(1.35f) else 1f
}

/** El widget que se está dibujando (su estilo, color, fondo y esquinas). */
private val LocalWidget = androidx.compose.runtime.compositionLocalOf<cl.nikalbazu.diptico.estado.Widget?> { null }

@Composable
private fun Tarjeta(modifier: Modifier = Modifier, alPulsar: (() -> Unit)? = null, contenido: @Composable () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val w = LocalWidget.current
    val forma = RoundedCornerShape(esc.d((w?.radio ?: 22).toFloat()))
    val estilo = w?.estilo ?: "tarjeta"
    val op = w?.opacidad ?: 1f
    val propio = w?.color?.takeIf { it != 0L }?.let { Color(it) }
    val base = propio ?: p.superficie
    val acento = propio ?: Uva
    val fondo = when (estilo) {
        "vidrio" -> Modifier.background(Brush.linearGradient(listOf((propio ?: Color.White).copy(alpha = 0.16f * op), (propio ?: Color.White).copy(alpha = 0.05f * op))))
            .border(esc.d(1.2f), Color.White.copy(alpha = 0.22f), forma)
        "solido" -> Modifier.background(Brush.linearGradient(listOf(acento.copy(alpha = 0.9f * op), acento.copy(alpha = 0.65f * op))))
        "contorno" -> Modifier.border(esc.d(2), acento.copy(alpha = 0.8f), forma)
        "sinfondo" -> Modifier
        else -> Modifier.background(Brush.verticalGradient(listOf(base.copy(alpha = 0.9f * op), base.copy(alpha = 0.7f * op))))
            .border(esc.d(1), Color.White.copy(alpha = 0.06f), forma)
    }
    Box(
        modifier.fillMaxSize().clip(forma).then(fondo)
            .combinedClickable(onClick = { if (alPulsar != null) alPulsar() else w?.let { Diptico.abrirWidgetTipo(it.tipo) } }, onLongClick = { Diptico.editarWidgets() })
            .padding(esc.d(12)),
    ) { contenido() }
}

@Composable
private fun WidgetVista(e: Estado, tipo: String, an: Int, al: Int) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    // tam: 1 angosto, 2 mediano, 3 ancho. compacto: una sola fila de alto.
    val tam = when { an >= 4 -> 3; an >= 3 -> 2; else -> 1 }
    val compacto = al == 1
    if (compacto && tipo !in setOf("reloj", "musica", "bateria", "frase", "accesos", "notificaciones", "estadisticas", "apps")) {
        WidgetCompacto(e, tipo)
        return
    }
    val variante = LocalWidget.current?.variante ?: 0
    when (tipo) {
        "juego" -> {
            val j = e.seleccionado
            if (variante == 1 && j != null) {
                // Arte de fondo: la imagen del juego llena el widget y el título va encima.
                val forma = RoundedCornerShape(esc.d((LocalWidget.current?.radio ?: 22).toFloat()))
                Box(
                    Modifier.fillMaxSize().clip(forma).background(p.superficie)
                        .combinedClickable(onClick = { Diptico.jugar() }, onLongClick = { Diptico.editarWidgets() }),
                ) {
                    ArteJuego(j, preferirArte = true, maxLado = 700, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))
                    Column(Modifier.align(Alignment.BottomStart).padding(esc.d(14))) {
                        Etiqueta(Consolas.porId(j.consolaId)?.nombre ?: "", Sol, 11f)
                        Texto(j.titulo, if (tam >= 2) 22f else 17f, color = Color.White, lineas = 2, sombra = true)
                        Spacer(Modifier.height(esc.d(6)))
                        Box(Modifier.clip(RoundedCornerShape(50)).fondoBoton(Uva).padding(horizontal = esc.d(14), vertical = esc.d(6))) {
                            Texto("A · ¡Jugar!", 14f, color = SobreUva)
                        }
                    }
                }
                return
            }
            Tarjeta(alPulsar = { Diptico.jugar() }) {
                if (j == null) {
                    Detalle("Elige un juego en el estante", 15f)
                } else {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        val c = Consolas.porId(j.consolaId)
                        if (c != null) {
                            val base = cajaDe(c)
                            CajaJuego(j, c, escala = minOf((al * 64f) / base.alto, 1.1f))
                        }
                        Spacer(Modifier.width(esc.d(14)))
                        Column(Modifier.weight(1f)) {
                            Etiqueta(c?.nombre ?: "", Coral, 11f)
                            Texto(j.titulo, if (tam >= 2) 22f else 17f, lineas = 2, interlineado = if (tam >= 2) 25f else 20f)
                            Spacer(Modifier.height(esc.d(8)))
                            Box(Modifier.clip(RoundedCornerShape(50)).background(Uva).padding(horizontal = esc.d(14), vertical = esc.d(6))) {
                                Texto("A · ¡Jugar!", 14f, color = SobreUva)
                            }
                        }
                    }
                }
            }
        }
        "reloj" -> Tarjeta {
            // Si hay temporizador o cronómetro andando, se ven en el reloj.
            if (e.temporizadorFin > 0L || e.cronoInicio > 0L) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    Texto(rememberHora(), if (compacto) 22f else 34f, lineas = 1)
                    Spacer(Modifier.height(esc.d(4)))
                    if (e.temporizadorFin > 0L) FilaCuenta(Icono.RELOJ, "Temporizador", cuentaRegresiva(e.temporizadorFin), Coral)
                    if (e.cronoInicio > 0L && !compacto) FilaCuenta(Icono.JUGAR, "Cronómetro", cronometro(e), Uva)
                }
                return@Tarjeta
            }
            if (variante == 1 && !compacto) {
                // Analógico: esfera con manecillas y la fecha al lado (o abajo si es angosto).
                val ahora = rememberHora()
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    RelojAnalogico(ahora, Modifier.fillMaxHeight().aspectRatio(1f))
                    if (an >= 3) {
                        Spacer(Modifier.width(esc.d(14)))
                        Column {
                            Texto(ahora, 24f, lineas = 1)
                            Detalle(cl.nikalbazu.diptico.estado.Idioma.fecha("EEEEdMMMM").replaceFirstChar { it.uppercase() }, 13f, lineas = 2)
                        }
                    }
                }
                return@Tarjeta
            }
            if (variante == 2) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Texto(rememberHora(), if (compacto) 34f else if (al >= 3) 84f else 62f, peso = FontWeight.Medium, lineas = 1)
                }
                return@Tarjeta
            }
            if (compacto) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Texto(rememberHora(), 30f)
                    Spacer(Modifier.width(esc.d(10)))
                    Detalle(cl.nikalbazu.diptico.estado.Idioma.fecha("EEEdMMM"), 13f, lineas = 1)
                }
                return@Tarjeta
            }
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Texto(rememberHora(), if (al >= 3) 72f else if (tam >= 2) 58f else 44f, interlineado = if (al >= 3) 74f else if (tam >= 2) 60f else 46f)
                Detalle(cl.nikalbazu.diptico.estado.Idioma.fecha("EEEEdMMMM").replaceFirstChar { it.uppercase() }, 14f, lineas = 1)
            }
        }
        "bateria" -> Tarjeta {
            val b = rememberBateria()
            if (variante == 1 && !compacto) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val color = if (b in 0..19) Coral else Uva
                    val linea = p.linea
                    androidx.compose.foundation.Canvas(Modifier.fillMaxHeight(0.92f).aspectRatio(1f)) {
                        val g = size.minDimension * 0.1f
                        val st = androidx.compose.ui.graphics.drawscope.Stroke(g, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        val o = androidx.compose.ui.geometry.Offset(g / 2, g / 2)
                        val t = androidx.compose.ui.geometry.Size(size.width - g, size.height - g)
                        drawArc(linea, 0f, 360f, false, o, t, style = st)
                        drawArc(color, -90f, 360f * b.coerceIn(0, 100) / 100f, false, o, t, style = st)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Texto(if (b >= 0) "$b%" else "—", 22f, lineas = 1)
                        Detalle("Batería", 11f, lineas = 1)
                    }
                }
                return@Tarjeta
            }
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Texto(if (b >= 0) "$b%" else "—", if (compacto) 22f else 40f)
                Spacer(Modifier.height(esc.d(8)))
                Box(Modifier.fillMaxWidth().height(esc.d(10)).clip(RoundedCornerShape(50)).background(p.linea)) {
                    Box(Modifier.fillMaxWidth((b.coerceIn(0, 100)) / 100f).fillMaxHeight().clip(RoundedCornerShape(50)).background(if (b in 0..19) Coral else Uva))
                }
                Spacer(Modifier.height(esc.d(6)))
                Detalle("Batería", 13f)
            }
        }
        "musica" -> WidgetMusica(tam, compacto)
        "favoritos" -> Tarjeta {
            val favs = e.juegos.filter { e.datos[it.clave]?.favorito == true }.take(if (variante == 1) al * 2 else if (tam >= 3) 6 else if (tam == 2) 4 else 2)
            Column {
                Etiqueta("Favoritos", Coral, 11f)
                Spacer(Modifier.height(esc.d(8)))
                if (favs.isEmpty()) Detalle("Marca favoritos con Y sobre un juego", 14f)
                if (variante == 1) { ListaJuegosWidget(favs); return@Column }
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                    for (j in favs) {
                        val c = Consolas.porId(j.consolaId) ?: continue
                        val base = cajaDe(c)
                        Box(Modifier.clickable { Diptico.elegirConsola("todo"); Diptico.elegirJuego(j.clave) }) {
                            CajaJuego(j, c, escala = minOf((al * 52f) / base.alto, 0.8f))
                        }
                    }
                }
            }
        }
        "frase" -> Tarjeta {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.CHISPA, Sol, 22f, acento = Coral)
                Spacer(Modifier.width(esc.d(10)))
                Texto(fraseDelDia(e), 16f, lineas = if (compacto) 1 else 3)
            }
        }
        "paso" -> Tarjeta(alPulsar = { Diptico.abrirPaso() }) {
            val paso = e.seleccionado?.let { e.datos[it.clave]?.paso }.orEmpty()
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconoDiptico(Icono.LAPIZ, Coral, 18f)
                    Spacer(Modifier.width(esc.d(8)))
                    Texto("Tu próximo paso", 14f, color = p.acentoTexto)
                }
                Spacer(Modifier.height(esc.d(8)))
                Texto(paso.ifEmpty { "Aprieta Y para anotar dónde quedaste" }, 16f, lineas = 4, color = if (paso.isEmpty()) p.tenue else p.tinta, peso = FontWeight.SemiBold)
            }
        }
        "estadisticas" -> Tarjeta(alPulsar = { Diptico.abrirEstadisticas() }) {
            val total = e.datos.values.sumOf { it.tiempo }
            val porConsola = e.datos.entries.filter { it.value.tiempo > 0 }
                .groupBy { it.key.substringBefore('/') }.mapValues { (_, v) -> v.sumOf { it.value.tiempo } }
                .entries.sortedByDescending { it.value }.take(if (tam >= 2) 4 else 2)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconoDiptico(Icono.RELOJ, Uva, 18f, acento = Coral)
                    Spacer(Modifier.width(esc.d(6)))
                    Etiqueta("Estadísticas", p.tenue, 11f, Modifier.weight(1f))
                    Texto(formatoTiempo(total), 15f, lineas = 1)
                }
                Spacer(Modifier.height(esc.d(8)))
                val maximo = porConsola.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
                if (porConsola.isEmpty()) Detalle("Juega un rato y aquí verás tus horas por consola.", 13f)
                for ((c, t) in porConsola) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = esc.d(2))) {
                        Texto(Consolas.porId(c)?.nombre ?: c, 12f, modifier = Modifier.width(esc.d(54)), lineas = 1)
                        Box(Modifier.weight(1f).height(esc.d(9)).clip(RoundedCornerShape(50)).background(p.linea)) {
                            Box(Modifier.fillMaxWidth(t / maximo.toFloat()).fillMaxSize().clip(RoundedCornerShape(50)).background(Uva))
                        }
                    }
                }
            }
        }
        "sugerencia" -> {
            val j = remember(e.juegos.size) { e.juegos.filter { (e.datos[it.clave]?.tiempo ?: 0L) == 0L }.ifEmpty { e.juegos }.randomOrNull() }
            Tarjeta(alPulsar = { j?.let { Diptico.elegirConsola("todo"); Diptico.elegirJuego(it.clave) } }) {
                Column {
                    Etiqueta("¿Qué juego hoy?", Coral, 11f)
                    Spacer(Modifier.height(esc.d(6)))
                    if (j == null) Detalle("Agrega juegos para recibir sugerencias.", 13f)
                    else Row(verticalAlignment = Alignment.CenterVertically) {
                        Consolas.porId(j.consolaId)?.let { CajaJuego(j, it, escala = 0.6f) }
                        Spacer(Modifier.width(esc.d(8)))
                        Texto(j.titulo, 15f, lineas = 3)
                    }
                }
            }
        }
        "logros" -> Tarjeta(alPulsar = { if (e.usuarioRA.isBlank()) Diptico.abrirMenu(cl.nikalbazu.diptico.estado.Seccion.LOGROS) else Diptico.abrirEstadisticas() }) {
            val ra = e.perfilRA
            LaunchedEffect(e.usuarioRA) { if (ra == null) Diptico.cargarPerfilRA() }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconoDiptico(Icono.TROFEO, Sol, 18f, acento = Coral)
                    Spacer(Modifier.width(esc.d(8)))
                    Etiqueta("Logros", p.tenue, 11f, Modifier.weight(1f))
                    if (ra != null) Texto("${ra.puntos} pts", 15f, color = Sol)
                }
                Spacer(Modifier.height(esc.d(6)))
                when {
                    e.usuarioRA.isBlank() -> Detalle("Toca para conectar RetroAchievements", 13.5f, lineas = 2)
                    ra == null -> Detalle("Cargando tu perfil…", 13.5f)
                    ra.recientes.isEmpty() -> Detalle("Todavía no ganas logros este mes. ¡A jugar!", 13.5f, lineas = 2)
                    else -> ra.recientes.take(if (al >= 3) 4 else if (compacto) 1 else 2).forEach { (juego, l) ->
                        Row(Modifier.padding(vertical = esc.d(2)), verticalAlignment = Alignment.CenterVertically) {
                            val img = rememberImagen(android.net.Uri.parse(l.imagen), 96)
                            Box(Modifier.size(esc.d(28)).clip(RoundedCornerShape(esc.d(7))).background(p.fondo)) {
                                if (img != null) Image(img, null, Modifier.fillMaxSize())
                            }
                            Spacer(Modifier.width(esc.d(8)))
                            Column(Modifier.weight(1f)) {
                                Texto(l.titulo, 13f, lineas = 1)
                                if (!compacto) Detalle(juego, 11f, lineas = 1)
                            }
                        }
                    }
                }
            }
        }
        "asistente" -> WidgetBisa(e, compacto, al)
        "apps" -> Tarjeta { WidgetApps(e, an) }
        "accesos" -> Tarjeta {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Diptico.opcionesCentro().take(if (tam >= 2) 6 else 4).forEach { o ->
                    Column(Modifier.clip(RoundedCornerShape(esc.d(12))).clickable { o.accion() }.padding(esc.d(4)), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(esc.d(40)).clip(CircleShape).background(p.fondo.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                            o.icono?.let { IconoDiptico(it, p.tinta, 22f, acento = Coral) }
                        }
                        // Con una sola fila de alto no caben los nombres.
                        if (al >= 2) Texto(o.titulo, 10.5f, color = p.tenue, lineas = 1)
                    }
                }
            }
        }
        "notificaciones" -> {
            val avisos by cl.nikalbazu.diptico.audio.Avisos.lista.collectAsState()
            Tarjeta(alPulsar = { Diptico.abrirCentro() }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconoDiptico(Icono.CAMPANA, p.tinta, 18f, acento = Coral)
                        Spacer(Modifier.width(esc.d(6)))
                        Etiqueta("Notificaciones", p.tenue, 11f, Modifier.weight(1f))
                        if (avisos.isNotEmpty()) Texto("${avisos.size}", 14f, color = Coral)
                    }
                    Spacer(Modifier.height(esc.d(6)))
                    if (avisos.isEmpty()) Detalle("Todo tranquilo.", 13f)
                    avisos.take(if (al >= 3) 4 else 2).forEach { a ->
                        Texto("${a.app}: ${a.titulo.ifBlank { a.texto }}", 12.5f, lineas = 1, modifier = Modifier.padding(vertical = esc.d(2)))
                    }
                }
            }
        }
        "tiempo" -> Tarjeta(alPulsar = { Diptico.abrirEstadisticas() }) {
            val total = e.datos.values.sumOf { it.tiempo }
            val top = e.datos.maxByOrNull { it.value.tiempo }?.takeIf { it.value.tiempo > 0 }
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Etiqueta("Tiempo jugado", p.tenue, 11f)
                Texto(if (total > 0) formatoTiempo(total) else "0 min", 28f, lineas = 1)
                top?.let { (clave, d) ->
                    val t = e.juegos.firstOrNull { it.clave == clave }?.titulo ?: return@let
                    Detalle("Más jugado: $t (${formatoTiempo(d.tiempo)})", 13f, lineas = 2)
                }
            }
        }
        "recientes" -> Tarjeta {
            val recientes = e.datos.entries.filter { it.value.ultimo > 0 }.sortedByDescending { it.value.ultimo }
                .mapNotNull { (k, _) -> e.juegos.firstOrNull { it.clave == k } }.take(if (variante == 1) al * 2 else if (tam >= 3) 5 else if (tam == 2) 3 else 2)
            Column {
                Etiqueta("Jugados hace poco", p.tenue, 11f)
                Spacer(Modifier.height(esc.d(8)))
                if (recientes.isEmpty()) Detalle("Todavía no juegas nada con Díptico", 14f)
                if (variante == 1) { ListaJuegosWidget(recientes); return@Column }
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                    for (j in recientes) {
                        val c = Consolas.porId(j.consolaId) ?: continue
                        val base = cajaDe(c)
                        Box(Modifier.clickable { Diptico.elegirConsola("todo"); Diptico.elegirJuego(j.clave) }) {
                            CajaJuego(j, c, escala = minOf((al * 52f) / base.alto, 0.8f))
                        }
                    }
                }
            }
        }
        "fiesta" -> Tarjeta {
            val hoy = e.fiesta
            val prox = Temas.proxima()
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                IconoDiptico(Icono.MUNDO, Sol, 26f, acento = Coral)
                Spacer(Modifier.height(esc.d(6)))
                if (hoy != null) {
                    Texto(hoy.nombre, 18f, lineas = 2)
                    Detalle(hoy.tema.mensaje.ifBlank { hoy.donde }, 13f, lineas = 2)
                } else if (prox != null) {
                    Etiqueta("Próxima fiesta", p.tenue, 11f)
                    Texto(prox.first.nombre, 18f, lineas = 2)
                    Detalle("${prox.second.dayOfMonth}/${prox.second.monthValue} · ${prox.first.donde}", 13f, lineas = 2)
                }
            }
        }
        "consolas" -> Tarjeta {
            Column {
                Etiqueta("Tu colección", p.tenue, 11f)
                Texto("${e.juegos.size} juegos", 26f)
                Detalle("en ${e.consolasConJuegos.size} consolas", 13f)
                Spacer(Modifier.height(esc.d(6)))
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    e.consolasConJuegos.take(if (tam >= 2) 8 else 4).forEach { IconoConsola(it, 24f, p.superficie) }
                }
            }
        }
    }
}

/** Juegos en filas (diseño "Lista" de recientes y favoritos). */
@Composable
private fun ListaJuegosWidget(juegos: List<cl.nikalbazu.diptico.data.Juego>) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
        for (j in juegos) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(10))).background(p.fondo.copy(alpha = 0.35f))
                    .clickable { Diptico.elegirConsola("todo"); Diptico.elegirJuego(j.clave) }.padding(horizontal = esc.d(8), vertical = esc.d(5)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Consolas.porId(j.consolaId)?.let { IconoConsola(it, 18f, p.superficie) }
                Spacer(Modifier.width(esc.d(8)))
                Texto(j.titulo, 13f, lineas = 1, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Reloj de manecillas para el widget de reloj (diseño "Analógico"). [hora] como "HH:mm". */
@Composable
private fun RelojAnalogico(hora: String, modifier: Modifier) {
    val p = LocalPaleta.current
    val h = hora.substringBefore(':').toIntOrNull() ?: 0
    val m = hora.substringAfter(':').take(2).toIntOrNull() ?: 0
    val tinta = p.tinta
    val linea = p.linea
    val acento = Coral
    val uva = Uva
    androidx.compose.foundation.Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = center
        drawCircle(uva.copy(alpha = 0.12f), r, c)
        drawCircle(linea, r, c, style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.04f))
        for (k in 0 until 12) {
            val a = Math.toRadians(k * 30.0 - 90)
            val largo = if (k % 3 == 0) 0.16f else 0.08f
            val ini = androidx.compose.ui.geometry.Offset(c.x + (r * (0.9f - largo) * kotlin.math.cos(a)).toFloat(), c.y + (r * (0.9f - largo) * kotlin.math.sin(a)).toFloat())
            val fin = androidx.compose.ui.geometry.Offset(c.x + (r * 0.9f * kotlin.math.cos(a)).toFloat(), c.y + (r * 0.9f * kotlin.math.sin(a)).toFloat())
            drawLine(tinta.copy(alpha = if (k % 3 == 0) 0.9f else 0.4f), ini, fin, r * (if (k % 3 == 0) 0.045f else 0.03f), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        fun mano(angulo: Double, largo: Float, grosor: Float, color: Color) {
            val a = Math.toRadians(angulo - 90)
            drawLine(color, c, androidx.compose.ui.geometry.Offset(c.x + (r * largo * kotlin.math.cos(a)).toFloat(), c.y + (r * largo * kotlin.math.sin(a)).toFloat()), r * grosor, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        mano((h % 12) * 30.0 + m * 0.5, 0.5f, 0.08f, tinta)
        mano(m * 6.0, 0.74f, 0.055f, acento)
        drawCircle(acento, r * 0.07f, c)
    }
}

/** Lo que suena en Spotify, YouTube Music u otra app, con botones para controlarlo. */
@Composable
private fun WidgetMusica(tam: Int, compacto: Boolean = false) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val ctx = LocalContext.current
    val permiso by Medios.permiso.collectAsState()
    val s by Medios.sonando.collectAsState()
    if (!permiso) {
        Tarjeta(alPulsar = { Medios.pedirPermiso(ctx) }) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                IconoDiptico(Icono.MUSICA, Uva, 26f, acento = Coral)
                Spacer(Modifier.height(esc.d(6)))
                Texto("Widget de música", 17f)
                Detalle("Toca para dar permiso y ver lo que suena en Spotify o YouTube Music.", 13f, lineas = 3)
            }
        }
        return
    }
    Tarjeta {
        val musica = s
        if (compacto) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.MUSICA, Coral, 20f, acento = Uva)
                Spacer(Modifier.width(esc.d(8)))
                Texto(musica?.titulo ?: "Nada sonando", 14.5f, lineas = 1, modifier = Modifier.weight(1f))
                BotonCirculo(if (musica?.reproduciendo == true) Icono.MENU else Icono.JUGAR, grande = true, pausa = musica?.reproduciendo == true) { Medios.alternar() }
                Spacer(Modifier.width(esc.d(6)))
                BotonCirculo(Icono.CONTINUAR) { Medios.siguiente() }
            }
            return@Tarjeta
        }
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(esc.d(if (tam >= 2) 96f else 56f)).clip(RoundedCornerShape(esc.d(14))).background(Uva.copy(alpha = 0.25f))
                    .clickable { musica?.paquete?.let { Diptico.abrirPaquete(it) } },
                contentAlignment = Alignment.Center,
            ) {
                val img = musica?.portada
                if (img != null) Image(img.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else IconoDiptico(Icono.MUSICA, p.tinta, 28f, acento = Coral)
            }
            Spacer(Modifier.width(esc.d(12)))
            Column(Modifier.weight(1f)) {
                Etiqueta(musica?.app ?: "Música", Coral, 11f)
                Texto(musica?.titulo ?: "Nada sonando", 16f, lineas = 2)
                if (!musica?.artista.isNullOrBlank()) Detalle(musica!!.artista, 13f, lineas = 1)
                Spacer(Modifier.height(esc.d(6)))
                if (musica == null) {
                    // Nada sonando: accesos a Spotify y YouTube Music.
                    val apps = remember { Diptico.appsMusica().ifEmpty { listOf(cl.nikalbazu.diptico.emu.App("com.spotify.music", "Spotify"), cl.nikalbazu.diptico.emu.App("com.google.android.apps.youtube.music", "YT Music")) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        for (a in apps) {
                            Box(
                                Modifier.clip(RoundedCornerShape(50)).fondoBoton(if (a.paquete.contains("spotify")) Color(0xFF1DB954) else Color(0xFFFF0033))
                                    .clickable { Diptico.abrirPaquete(a.paquete) }.padding(horizontal = esc.d(10), vertical = esc.d(5)),
                            ) { Texto(a.nombre, 12.5f, color = Color.White, lineas = 1) }
                        }
                    }
                    return@Column
                }
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8)), verticalAlignment = Alignment.CenterVertically) {
                    BotonCirculo(Icono.CONTINUAR, invertido = true) { Medios.anterior() }
                    BotonCirculo(if (musica?.reproduciendo == true) Icono.MENU else Icono.JUGAR, grande = true, pausa = musica?.reproduciendo == true) { Medios.alternar() }
                    BotonCirculo(Icono.CONTINUAR) { Medios.siguiente() }
                }
            }
        }
    }
}

@Composable
private fun BotonCirculo(icono: Icono, grande: Boolean = false, invertido: Boolean = false, pausa: Boolean = false, accion: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val tam = if (grande) 38f else 30f
    Box(
        Modifier.size(esc.d(tam)).clip(CircleShape).background(if (grande) Uva else p.fondo).clickable { accion() },
        contentAlignment = Alignment.Center,
    ) {
        if (pausa) {
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(4))) {
                Box(Modifier.size(esc.d(4), esc.d(14)).clip(RoundedCornerShape(50)).background(SobreUva))
                Box(Modifier.size(esc.d(4), esc.d(14)).clip(RoundedCornerShape(50)).background(SobreUva))
            }
        } else {
            IconoDiptico(
                icono, if (grande) SobreUva else p.tinta, tam * 0.5f,
                modifier = Modifier.graphicsLayer { if (invertido) scaleX = -1f },
            )
        }
    }
}

/** Una fila corta para widgets de una sola fila de alto. */
@Composable
private fun WidgetCompacto(e: Estado, tipo: String) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val j = e.seleccionado
    val (icono, titulo, valor) = when (tipo) {
        "juego" -> Triple(Icono.JUGAR, j?.titulo ?: "Elige un juego", "A · ¡Jugar!")
        "paso" -> Triple(Icono.LAPIZ, "Tu próximo paso", j?.let { e.datos[it.clave]?.paso }.orEmpty().ifEmpty { "Sin anotar" })
        "tiempo" -> Triple(Icono.RELOJ, "Tiempo jugado", formatoTiempo(e.datos.values.sumOf { it.tiempo }))
        "recientes" -> Triple(Icono.CONTINUAR, "Último jugado", e.datos.entries.filter { it.value.ultimo > 0 }.maxByOrNull { it.value.ultimo }?.let { (k, _) -> e.juegos.firstOrNull { it.clave == k }?.titulo } ?: "Nada aún")
        "fiesta" -> Triple(Icono.MUNDO, e.fiesta?.nombre ?: (Temas.proxima()?.first?.nombre ?: "Fiestas"), e.fiesta?.donde ?: "Próxima fiesta")
        "consolas" -> Triple(Icono.BIBLIOTECA, "${e.juegos.size} juegos", "${e.consolasConJuegos.size} consolas")
        "favoritos" -> Triple(Icono.CORAZON, "Favoritos", "${e.datos.values.count { it.favorito }} juegos")
        else -> Triple(Icono.ESTRELLA, Widgets.nombre(tipo), "")
    }
    Tarjeta(alPulsar = { if (tipo == "juego") Diptico.jugar() else if (tipo == "paso") Diptico.abrirPaso() }) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(esc.d(34)).clip(CircleShape).background(Uva.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                IconoDiptico(icono, p.tinta, 18f, acento = Coral)
            }
            Spacer(Modifier.width(esc.d(10)))
            Column(Modifier.weight(1f)) {
                Texto(titulo, 15f, lineas = 1)
                if (valor.isNotEmpty()) Texto(valor, 12.5f, color = p.tenue, familia = Nunito, peso = FontWeight.Bold, lineas = 1)
            }
        }
    }
}

/** Una frase distinta cada día para el widget de saludo. */
private fun fraseDelDia(e: Estado): String {
    val frases = listOf(
        "Hoy es un buen día para terminar ese juego pendiente.",
        "Un nivel más y paras. (Seguro que no.)",
        "Los clásicos nunca pasan de moda.",
        "Guarda la partida antes del jefe final.",
        "¿Y si hoy pruebas un juego que nunca has jugado?",
        "La mejor consola es la que tienes a mano.",
        "Recuerda tomar agua entre partidas.",
        "Perder también es parte del juego: se aprende y se vuelve a intentar.",
        "Jugar con alguien más duplica la diversión.",
        "Pausa, respira, sigue. Así se gana.",
        "Ese juego que dejaste a medias te está esperando.",
        "Hoy cuenta cada pequeño avance.",
        "Mira a lo lejos un momento: tus ojos te lo agradecen.",
        "No hay apuro: el mejor ritmo es el tuyo.",
    )
    if (e.fraseExtra == 0) e.fiesta?.tema?.mensaje?.takeIf { it.isNotBlank() }?.let { return it }
    return frases[(java.time.LocalDate.now().dayOfYear + e.fraseExtra) % frases.size]
}

/** Un widget con tu propia imagen o GIF animado. */
@Composable
private fun WidgetImagen(w: cl.nikalbazu.diptico.estado.Widget) {
    val esc = LocalEscala.current
    // Sin fondo: los PNG y GIF con transparencia se ven tal cual. La imagen se ajusta entera al tamaño del widget.
    val forma = RoundedCornerShape(esc.d(w.radio.toFloat()))
    Box(
        Modifier.fillMaxSize().clip(forma)
            .then(if (w.estilo == "sinfondo") Modifier else Modifier.background(LocalPaleta.current.superficie.copy(alpha = 0.6f * w.opacidad)))
            .combinedClickable(onClick = { Diptico.abrirWidgetTipo(w.tipo) }, onLongClick = { Diptico.editarWidgets() }),
    ) {
        w.uri?.let { ImagenAnimada(android.net.Uri.parse(it), Modifier.fillMaxSize(), recortar = w.variante == 1) }
    }
}

/**
 * Un video corto como widget: se repite en silencio, decodificado por hardware en una TextureView
 * (no recarga la interfaz) y se pausa solo mientras juegas o cuando no se ve.
 */
@Composable
private fun WidgetVideo(w: cl.nikalbazu.diptico.estado.Widget, activo: Boolean) {
    val esc = LocalEscala.current
    val uri = w.uri ?: return
    val forma = RoundedCornerShape(esc.d(w.radio.toFloat()))
    val reproductor = remember(uri) { android.media.MediaPlayer() }
    var listo by remember(uri) { mutableStateOf(false) }
    androidx.compose.runtime.DisposableEffect(uri) {
        onDispose { try { reproductor.release() } catch (_: Exception) {} }
    }
    LaunchedEffect(activo, listo) {
        if (!listo) return@LaunchedEffect
        try { if (activo) reproductor.start() else reproductor.pause() } catch (_: Exception) {}
    }
    Box(Modifier.fillMaxSize().clip(forma).combinedClickable(onClick = { Diptico.abrirWidgetTipo(w.tipo) }, onLongClick = { Diptico.editarWidgets() })) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { c ->
                android.view.TextureView(c).apply {
                    isOpaque = false
                    surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: android.graphics.SurfaceTexture, ancho: Int, alto: Int) {
                            try {
                                reproductor.reset()
                                reproductor.setDataSource(c, android.net.Uri.parse(uri))
                                reproductor.setSurface(android.view.Surface(st))
                                reproductor.isLooping = true
                                reproductor.setVolume(0f, 0f)
                                reproductor.setOnPreparedListener { mp ->
                                    // Recorta al centro para llenar el widget sin deformarse.
                                    val vw = mp.videoWidth.toFloat(); val vh = mp.videoHeight.toFloat()
                                    if (vw > 0 && vh > 0 && width > 0 && height > 0) {
                                        val k = maxOf(width / vw, height / vh)
                                        val m = android.graphics.Matrix()
                                        m.setScale(vw * k / width, vh * k / height, width / 2f, height / 2f)
                                        setTransform(m)
                                    }
                                    listo = true
                                    if (activo) mp.start()
                                }
                                reproductor.prepareAsync()
                            } catch (_: Exception) {}
                        }
                        override fun onSurfaceTextureSizeChanged(st: android.graphics.SurfaceTexture, ancho: Int, alto: Int) {}
                        override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean { try { reproductor.pause() } catch (_: Exception) {}; return true }
                        override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Widgets de una columna de ancho (1×1, 1×2…): solo lo esencial, grande y al centro. */
@Composable
private fun WidgetMini(e: Estado, tipo: String, al: Int) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val j = e.seleccionado
    val m by cl.nikalbazu.diptico.audio.Medios.sonando.collectAsState()
    val avisos by cl.nikalbazu.diptico.audio.Avisos.lista.collectAsState()
    val bateria = rememberBateria()
    val (icono, valor) = when (tipo) {
        "reloj" -> null to rememberHora()
        "bateria" -> Icono.BATERIA to (if (bateria >= 0) "$bateria%" else "—")
        "tiempo", "estadisticas" -> Icono.RELOJ to "${e.datos.values.sumOf { it.tiempo } / 3600} h"
        "consolas" -> Icono.BIBLIOTECA to "${e.juegos.size}"
        "favoritos" -> Icono.CORAZON to "${e.datos.values.count { it.favorito }}"
        "notificaciones" -> Icono.CAMPANA to "${avisos.size}"
        "logros" -> Icono.TROFEO to (e.perfilRA?.puntos?.let { "$it" } ?: "—")
        "musica" -> Icono.MUSICA to (if (m?.reproduciendo == true) "▶" else "")
        else -> Widgets.icono(tipo) to ""
    }
    // Juego, recientes y sugerencia: la portada llena el widget.
    val portada = when (tipo) {
        "juego" -> j
        "recientes" -> e.datos.entries.filter { it.value.ultimo > 0 }.maxByOrNull { it.value.ultimo }?.let { (k, _) -> e.juegos.firstOrNull { it.clave == k } }
        "favoritos" -> e.juegos.firstOrNull { e.datos[it.clave]?.favorito == true }
        else -> null
    }
    if (portada != null) {
        val forma = RoundedCornerShape(esc.d((LocalWidget.current?.radio ?: 22).toFloat()))
        Box(Modifier.fillMaxSize().clip(forma).combinedClickable(onClick = { if (tipo == "juego") Diptico.jugar() else Diptico.abrirWidgetTipo(tipo) }, onLongClick = { Diptico.editarWidgets() })) {
            ArteJuego(portada, preferirArte = false, maxLado = 360, modifier = Modifier.fillMaxSize())
            if (tipo != "juego") Box(Modifier.align(Alignment.TopEnd).padding(esc.d(5)).size(esc.d(22)).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                IconoDiptico(Widgets.icono(tipo), Color.White, 12f)
            }
        }
        return
    }
    if (tipo == "asistente") {
        Tarjeta {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(minOf(56f, 30f * al)).value) }
        }
        return
    }
    Tarjeta(alPulsar = { if (tipo == "musica") cl.nikalbazu.diptico.audio.Medios.alternar() else Diptico.abrirWidgetTipo(tipo) }) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (icono != null) IconoDiptico(icono, p.tinta, if (valor.isEmpty()) 26f else 18f, acento = Coral)
            if (valor.isNotEmpty()) {
                if (icono != null) Spacer(Modifier.height(esc.d(2)))
                Texto(valor, if (tipo == "reloj") (if (al >= 2) 26f else 19f) else 18f, lineas = 1, traducir = false)
            }
        }
    }
}


// ---------- Widgets con vida ----------

/** "mm:ss" (o "h:mm:ss") que falta para [fin]. */
@Composable
private fun cuentaRegresiva(fin: Long): String {
    var ahora by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(fin) { while (true) { ahora = System.currentTimeMillis(); kotlinx.coroutines.delay(500) } }
    return formatoReloj(((fin - ahora).coerceAtLeast(0) + 999) / 1000)
}

@Composable
private fun cronometro(e: Estado): String {
    var ahora by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(e.cronoInicio) { while (e.cronoInicio > 0L) { ahora = System.currentTimeMillis(); kotlinx.coroutines.delay(250) } }
    val ms = e.cronoAcumulado + if (e.cronoInicio > 0L) (ahora - e.cronoInicio) else 0L
    return formatoReloj(ms / 1000)
}

private fun formatoReloj(seg: Long): String =
    if (seg >= 3600) "${seg / 3600}:${((seg % 3600) / 60).toString().padStart(2, '0')}:${(seg % 60).toString().padStart(2, '0')}"
    else "${seg / 60}:${(seg % 60).toString().padStart(2, '0')}"

@Composable
private fun FilaCuenta(icono: Icono, titulo: String, valor: String, color: Color) {
    val esc = LocalEscala.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = esc.d(2))) {
        IconoDiptico(icono, color, 14f)
        Spacer(Modifier.width(esc.d(6)))
        Detalle(titulo, 12f, lineas = 1, modifier = Modifier.weight(1f))
        Texto(valor, 15f, color = color, lineas = 1, traducir = false)
    }
}

/** Bisa con identidad propia: su morado de siempre, su cara, una burbuja que cambia y accesos directos. */
@Composable
private fun WidgetBisa(e: Estado, compacto: Boolean, al: Int) {
    val esc = LocalEscala.current
    val w = LocalWidget.current
    val forma = RoundedCornerShape(esc.d((w?.radio ?: 22).toFloat()))
    val j = e.seleccionado
    val frases = remember(j?.clave, e.chat.size) {
        buildList {
            add(saludo() + ". ¿En qué te ayudo?")
            if (j != null) add("¿Quieres un truco para ${j.titulo}?")
            j?.let { e.datos[it.clave]?.paso }?.takeIf { it.isNotBlank() }?.let { add("Quedaste en: $it") }
            add("¿No sabes qué jugar? Te recomiendo uno.")
            add("Puedo cambiarte el tema, la música o el inicio.")
            add("Mientras juegas, toca mi burbuja si te trabas.")
        }
    }
    var k by remember { mutableStateOf(0) }
    LaunchedEffect(frases) { while (true) { kotlinx.coroutines.delay(7000); k++ } }
    Box(
        Modifier.fillMaxSize().clip(forma)
            .background(Brush.linearGradient(listOf(Color(0xFF34295A), Color(0xFF17132A))))
            .border(esc.d(1.5f), Uva.copy(alpha = 0.55f), forma)
            .combinedClickable(onClick = { Diptico.abrirWidgetTipo("asistente") }, onLongClick = { Diptico.editarWidgets() }),
    ) {
        // Un brillo que respira detrás de Bisa.
        Box(Modifier.align(Alignment.CenterStart).size(esc.d(140)).background(Brush.radialGradient(listOf(Uva.copy(alpha = 0.35f), Color.Transparent))))
        Row(Modifier.fillMaxSize().padding(esc.d(10)), verticalAlignment = Alignment.CenterVertically) {
            cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(if (compacto) 40f else if (al >= 2) 64f else 48f).value)
            Spacer(Modifier.width(esc.d(10)))
            Column(Modifier.weight(1f)) {
                if (!compacto) Etiqueta("Bisa", Coral, 10.5f)
                androidx.compose.animation.AnimatedContent(
                    targetState = frases[k.mod(frases.size)],
                    transitionSpec = { (fadeIn(tween(400)) + androidx.compose.animation.slideInVertically(tween(400)) { it / 3 }) togetherWith fadeOut(tween(250)) },
                    label = "burbuja",
                ) { f ->
                    Box(
                        Modifier.clip(RoundedCornerShape(esc.d(4), esc.d(14), esc.d(14), esc.d(14))).background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = esc.d(10), vertical = esc.d(5)),
                    ) { Texto(f, 13f, color = Color.White, lineas = if (compacto) 1 else 2) }
                }
                if (!compacto && al >= 2) {
                    Spacer(Modifier.height(esc.d(7)))
                    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        ChipBisa("Hablar") { Diptico.abrirAsistente() }
                        if (j != null) ChipBisa("Un truco") { Diptico.abrirAsistente(); Diptico.preguntar("Dame un buen truco o consejo para ${j.titulo}.") }
                        ChipBisa("¿Qué juego?") { Diptico.abrirAsistente(); Diptico.preguntar("¿Qué juego de mi colección me recomiendas para hoy? Dame 3 opciones y por qué.") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipBisa(t: String, accion: () -> Unit) {
    val esc = LocalEscala.current
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(Uva.copy(alpha = 0.85f)).clickable { accion() }
            .padding(horizontal = esc.d(10), vertical = esc.d(4)),
    ) { Texto(t, 11.5f, color = SobreUva, lineas = 1) }
}

/** El contenido de un widget abierto (en la pantalla de los botones): su vista grande arriba y lo que se puede hacer. */
@Composable
fun PanelWidget(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val w = e.widgets.getOrNull(e.widgetAbierto) ?: return
    val opciones = remember(e) { Diptico.opcionesWidget(e) }
    val esBisa = w.tipo == "asistente"
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        if (esBisa) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF34295A).copy(alpha = 0.6f), Color.Transparent))))
        Column(Modifier.fillMaxSize().padding(start = esc.d(14), end = esc.d(14), top = esc.d(12), bottom = esc.d(10))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (esBisa) cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(38).value)
                else Box(Modifier.size(esc.d(36)).clip(CircleShape).background(Uva.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    IconoDiptico(Widgets.icono(w.tipo), p.tinta, 18f, acento = Coral)
                }
                Spacer(Modifier.width(esc.d(10)))
                Column(Modifier.weight(1f)) {
                    Texto(if (esBisa) "Bisa" else Widgets.nombre(w.tipo), 18f, lineas = 1)
                    Detalle(if (esBisa) "Tus accesos rápidos" else Widgets.descripcion(w.tipo), 12f, lineas = 1)
                }
                Box(
                    Modifier.size(esc.d(34)).clip(CircleShape).background(p.superficie).clickable { Diptico.cerrarPanel() },
                    contentAlignment = Alignment.Center,
                ) { IconoDiptico(Icono.CERRAR, p.tinta, 16f) }
            }
            // Cabeceras con vida: el reloj cuenta, la música suena, la frase se lee entera.
            when (w.tipo) {
                "reloj" -> if (e.temporizadorFin > 0L || e.cronoInicio > 0L || e.cronoAcumulado > 0L) {
                    Spacer(Modifier.height(esc.d(8)))
                    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                        if (e.temporizadorFin > 0L) CuadroCuenta("Temporizador", cuentaRegresiva(e.temporizadorFin), Coral, Modifier.weight(1f))
                        if (e.cronoInicio > 0L || e.cronoAcumulado > 0L) CuadroCuenta("Cronómetro", cronometro(e), Uva, Modifier.weight(1f))
                    }
                }
                "frase" -> {
                    Spacer(Modifier.height(esc.d(8)))
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(16))).background(p.superficie).padding(esc.d(14))) {
                        Texto(fraseDelDia(e), 17f, lineas = 4)
                    }
                }
            }
            Spacer(Modifier.height(esc.d(8)))
            val lista = rememberLazyListState()
            LaunchedEffect(e.indice) { lista.mostrar(e.indice) }
            androidx.compose.foundation.lazy.LazyColumn(state = lista, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                items(opciones.size) { i -> FilaOpcion(opciones[i], i == e.indice, Uva) { Diptico.elegirIndice(i) } }
            }
            opciones.getOrNull(e.indice)?.ayuda?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(esc.d(4)))
                Detalle(it, 12.5f, lineas = 2)
            }
            Spacer(Modifier.height(esc.d(4)))
            Pistas("↑ ↓" to "Moverte", "A" to "Elegir", "← →" to "Cambiar", "B" to "Volver")
        }
    }
}

@Composable
private fun CuadroCuenta(titulo: String, valor: String, color: Color, modifier: Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column(modifier.clip(RoundedCornerShape(esc.d(16))).background(p.superficie).padding(esc.d(10))) {
        Etiqueta(titulo, color, 10.5f)
        Texto(valor, 30f, color = color, lineas = 1, traducir = false)
    }
}

/** El widget abierto en la pantalla de arriba: a la izquierda el widget en grande, a la derecha lo que se puede hacer. */
@Composable
fun WidgetArriba(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val entra = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(e.widgetAbierto) { entra.snapTo(0f); entra.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 380f)) }
    Box(Modifier.fillMaxSize().background(p.fondo)) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight().graphicsLayer { alpha = entra.value; scaleX = 0.94f + 0.06f * entra.value; scaleY = 0.94f + 0.06f * entra.value }) {
                LecturaWidget(e)
            }
            Box(
                Modifier.width(esc.d(330)).fillMaxHeight()
                    .graphicsLayer { translationX = (1f - entra.value) * 120f * density; alpha = entra.value }
                    .border(esc.d(1), p.linea),
            ) { PanelWidget(e) }
        }
    }
}

/** La otra pantalla con un widget abierto: el widget en grande (el reloj, la música, el juego elegido de la lista…). */
@Composable
fun LecturaWidget(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val w = e.widgets.getOrNull(e.widgetAbierto) ?: return
    val opcion = Diptico.opcionesWidget(e).getOrNull(e.indice)
    // En las listas de juegos se ve el juego elegido de la lista.
    val juego = if (w.tipo in setOf("recientes", "favoritos", "sugerencia")) e.juegos.firstOrNull { it.titulo == opcion?.titulo } else null
    Box(Modifier.fillMaxSize().background(p.fondo)) {
        if (juego != null) {
            Heroe(juego, 1f)
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(p.fondo.copy(alpha = 0.92f), p.fondo.copy(alpha = 0.2f)), endX = 1200f)))
            Column(Modifier.align(Alignment.CenterStart).padding(esc.d(28)).widthIn(max = esc.d(380))) {
                Etiqueta(Consolas.porId(juego.consolaId)?.bonito ?: "", Coral, 12f)
                Texto(juego.titulo, 30f, lineas = 3, traducir = false)
                e.datos[juego.clave]?.paso?.takeIf { it.isNotBlank() }?.let { Spacer(Modifier.height(esc.d(8))); Detalle("Tu paso: $it", 14f, lineas = 3) }
                Spacer(Modifier.height(esc.d(10)))
                Pista("A", "¡Jugar!")
            }
            return@Box
        }
        Box(Modifier.align(Alignment.TopEnd).size(esc.d(480)).background(Brush.radialGradient(listOf(Uva.copy(alpha = 0.22f), Color.Transparent))))
        Box(Modifier.fillMaxSize().padding(esc.d(28)), contentAlignment = Alignment.Center) {
            when (w.tipo) {
                "reloj" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Texto(rememberHora(), 96f, peso = FontWeight.Medium, lineas = 1)
                    Detalle(cl.nikalbazu.diptico.estado.Idioma.fecha("EEEEdMMMM").replaceFirstChar { it.uppercase() }, 18f, lineas = 1)
                    if (e.temporizadorFin > 0L) { Spacer(Modifier.height(esc.d(14))); Texto("⏳ " + cuentaRegresiva(e.temporizadorFin), 34f, color = Coral, traducir = false) }
                    if (e.cronoInicio > 0L || e.cronoAcumulado > 0L) { Spacer(Modifier.height(esc.d(6))); Texto("⏱ " + cronometro(e), 34f, color = Uva, traducir = false) }
                }
                "asistente" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(130).value)
                    Spacer(Modifier.height(esc.d(14)))
                    Texto(opcion?.titulo ?: "¿En qué te ayudo?", 24f, lineas = 2, alinear = TextAlign.Center)
                    opcion?.ayuda?.takeIf { it.isNotBlank() }?.let { Detalle(it, 14f, lineas = 2) }
                }
                else -> {
                    // El widget mismo, en grande.
                    val k = 2.2f
                    Box(Modifier.size(esc.d(4 * 92f * k / 2), esc.d(3 * 80f * k / 2))) {
                        CompositionLocalProvider(LocalWidget provides w.copy(an = 4, al = 3)) {
                            if (w.tipo.startsWith("imagen")) WidgetImagen(w) else WidgetVista(e, w.tipo, 4, 3)
                        }
                    }
                }
            }
        }
    }
}

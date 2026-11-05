@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.zIndex
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.luminance
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
                ArteJuego(j, preferirArte = true, maxLado = 480, modifier = Modifier.fillMaxSize().then(if (Estilo.efectosCompletos) Modifier.blur(28.dp) else Modifier).graphicsLayer { alpha = 0.32f })
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.fondo.copy(alpha = 0.45f), p.fondo.copy(alpha = 0.92f)))))
        }
        // Brillo del tema arriba a la derecha (solo en temas oscuros: en los claros el degradado se ve "punteado" a través de las tarjetas).
        if (p.fondo.luminance() < 0.4f) Box(Modifier.align(Alignment.TopEnd).size(esc.d(420)).background(Brush.radialGradient(listOf(Uva.copy(alpha = 0.18f), Color.Transparent))))
        Column(Modifier.fillMaxSize().padding(start = esc.d(14), end = esc.d(14), top = esc.d(if (e.editandoWidgets) 18 else 10), bottom = esc.d(8))) {
            if (e.editandoWidgets) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Box(Modifier.weight(1f)) { Texto("Acomodando tu inicio", 24f, lineas = 1) }
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).fondoBoton(Uva).clickable { Diptico.terminarEdicion() }
                            .padding(horizontal = esc.d(14), vertical = esc.d(8)),
                    ) { Texto("Listo", 14f, color = SobreUva) }
                }
                Spacer(Modifier.height(esc.d(2)))
                Detalle(
                    "Arrastra al borde para cambiar de página · estira desde la esquina",
                    12f, lineas = 1,
                )
                Spacer(Modifier.height(esc.d(10)))
            } else if (e.saludoInicio || e.sesion == null) {
                EncabezadoWidgets(e)
                Spacer(Modifier.height(esc.d(8)))
            }
            PaginasWidgets(e, Modifier.fillMaxWidth().weight(1f))
        }
    }
}

@Composable
private fun EncabezadoWidgets(e: Estado) {
    val esc = LocalEscala.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val anchoBarra = if (e.saludoInicio) maxWidth * 0.66f else maxWidth
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (e.saludoInicio) {
                Column(Modifier.weight(1f).padding(end = esc.d(8))) {
                    Texto(saludo(), 22f, lineas = 1)
                    val formato = if (esc.angosta) "dMMM" else "EEEEdMMMM"
                    val fecha = cl.nikalbazu.diptico.estado.Idioma.fecha(formato).replaceFirstChar { it.uppercase() }
                    Detalle(fecha + (e.fiesta?.let { " · ${it.nombre}" } ?: ""), 13f, lineas = 1)
                }
            }
            if (e.sesion == null) BarraFlotante(e, null, Modifier.widthIn(max = anchoBarra), arriba = true)
        }
    }
}

@Composable
private fun PaginasWidgets(e: Estado, modifier: Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val densidad = LocalDensity.current
    val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = e.paginaInicio) { e.paginas }
    var arrastre by remember { mutableStateOf<ArrastreWidget?>(null) }
    LaunchedEffect(e.paginaInicio, e.editandoWidgets) {
        if (pager.currentPage == e.paginaInicio) return@LaunchedEffect
        if (e.editandoWidgets) pager.scrollToPage(e.paginaInicio)
        else pager.animateScrollToPage(e.paginaInicio)
    }
    LaunchedEffect(pager.settledPage) {
        if (!Diptico.estado.value.editandoWidgets) Diptico.irAPagina(pager.settledPage)
    }

    Column(modifier) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val medidas = with(densidad) {
                MedidasEditorWidgets(
                    maxWidth.toPx(), maxHeight.toPx(), esc.d(MARGEN_GRILLA).toPx(),
                    esc.d(e.huecoInicio.toFloat()).toPx(), e.gridCols, e.gridFilas, esc.d(38).toPx(),
                )
            }
            val gesto = if (e.editandoWidgets) Modifier.editarConDedo(medidas, e.burbuja != null) { arrastre = it }
            else Modifier.pellizcoWidgets()
            Box(Modifier.fillMaxSize().testTag("paginas-widgets").then(gesto)) {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pager, modifier = Modifier.fillMaxSize(), userScrollEnabled = !e.editandoWidgets,
                    pageSpacing = esc.d(20),
                ) { pagina -> Grilla(e, pagina, medidas, arrastre, Modifier.fillMaxSize()) }
                arrastre?.takeUnless { it.estirando }?.let { FantasmaWidget(it, medidas) }
            }
        }
        if (e.editandoWidgets) {
            Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                BotonPagina("‹", "Página anterior", e.paginaInicio > 0) { Diptico.cambiarPagina(-1) }
                Texto("Página ${e.paginaInicio + 1} de ${e.paginas}", 12f, lineas = 1)
                BotonPagina("›", "Página siguiente", e.paginaInicio < e.paginas - 1) { Diptico.cambiarPagina(1) }
                BotonPagina("+", "Agregar página", e.paginas < 12) { Diptico.agregarPaginaWidgets() }
            }
        } else if (e.paginas > 1) {
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

@Composable
private fun BotonPagina(simbolo: String, descripcion: String, habilitado: Boolean, accion: () -> Unit) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    Box(
        Modifier.size(esc.d(42), esc.d(36)).clip(RoundedCornerShape(50))
            .semantics { contentDescription = descripcion }
            .clickable(enabled = habilitado, onClick = accion),
        contentAlignment = Alignment.Center,
    ) { Texto(simbolo, 21f, color = if (habilitado) p.tinta else p.tenue, traducir = false) }
}

@Composable
private fun Grilla(e: Estado, pagina: Int, m: MedidasEditorWidgets, arrastre: ArrastreWidget?, modifier: Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val densidad = LocalDensity.current
    Box(
        modifier.padding(esc.d(MARGEN_GRILLA)).pointerInput(e.editandoWidgets, m) {
            if (!e.editandoWidgets) detectTapGestures(onLongPress = { Diptico.editarWidgets() })
            else detectTapGestures(
                onLongPress = { o -> Diptico.abrirBurbuja((o.x / m.pasoX).toInt(), (o.y / m.pasoY).toInt()) },
                onTap = { Diptico.cerrarBurbuja() },
            )
        },
    ) {
        if (e.editandoWidgets) {
            for (x in 0 until m.columnas) for (y in 0 until m.filas) {
                with(densidad) {
                    Box(Modifier.offset((x * m.pasoX).toDp(), (y * m.pasoY).toDp())
                        .size(m.celdaAn.toDp(), m.celdaAl.toDp())
                        .border(esc.d(1.5f), p.linea, RoundedCornerShape(esc.d(14))))
                }
            }
        }
        e.widgets.forEachIndexed { i, w ->
            if (!w.visible || w.pagina != pagina) return@forEachIndexed
            key(w.tipo) {
                val gesto = arrastre?.takeIf { it.indice == i }
                CeldaWidget(e, i, w, m, gesto)
            }
        }
        val b = e.burbuja
        if (e.editandoWidgets && b != null && pagina == e.paginaInicio) with(densidad) {
            BurbujaAgregar(e, b, m.celdaAn.toDp().value / esc.factor, m.celdaAl.toDp().value / esc.factor,
                m.hueco.toDp().value / esc.factor, (m.ancho - 2 * m.margen).toDp().value / esc.factor,
                (m.alto - 2 * m.margen).toDp().value / esc.factor)
        }
    }
}

@Composable
private fun CeldaWidget(e: Estado, i: Int, w: Widget, m: MedidasEditorWidgets, arrastre: ArrastreWidget?) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val densidad = LocalDensity.current
    val elegido = e.editandoWidgets && i == e.widgetSel
    val enfocado = !e.editandoWidgets && i == e.widgetFoco && e.panel == cl.nikalbazu.diptico.estado.Panel.NINGUNO
    val estirando = arrastre?.takeIf { it.estirando }
    val tam = estirando?.tamano(m) ?: Offset(m.ancho(w.an), m.alto(w.al))
    val ancho = with(densidad) { tam.x.toDp() }
    val alto = with(densidad) { tam.y.toDp() }
    val (an, al) = m.celdas(tam, w)
    val rect = m.rect(w)
    val x = with(densidad) { (rect.left - m.margen).toDp() }
    val y = with(densidad) { (rect.top - m.margen).toDp() }
    val forma = RoundedCornerShape(esc.d((w.radio + 2).toFloat()))
    val color = if (e.estudioMover || arrastre != null) Coral else Uva
    val crecer = minOf(0.045f, 8f / maxOf(ancho.value, alto.value))
    val escalaFoco by animateFloatAsState(if (enfocado) 1f + crecer else 1f, spring(dampingRatio = 0.75f), label = "foco widget")
    Box(
        Modifier.offset(x, y).size(ancho, alto).zIndex(if (elegido || enfocado) 2f else 1f)
            .graphicsLayer {
                alpha = if (arrastre != null && !arrastre.estirando) 0.3f else 1f
                scaleX = escalaFoco
                scaleY = escalaFoco
            }
            .then(if (e.editandoWidgets) Modifier.clip(forma).semantics { contentDescription = Widgets.nombre(w.tipo) } else Modifier)
            .then(if (elegido) Modifier.border(esc.d(3), color, forma) else Modifier)
            .then(if (enfocado) Modifier.anilloFoco(w.radio.toFloat(), if (p.fondo.luminance() < 0.4f) Color.White else Uva) else Modifier),
    ) {
        if (enfocado && !w.tipo.startsWith("imagen")) Box(Modifier.fillMaxSize().background(p.fondo))
        val auto = remember(w.tipo, an, al) { escalaAuto(w.copy(an = an, al = al)) }
        CompositionLocalProvider(LocalWidget provides w, LocalEscala provides Escala(esc.factor * w.escala * auto, esc.ancho, esc.texto)) {
            ContenidoWidget(e, w, an, al)
        }
        if (e.editandoWidgets) {
            Box(Modifier.fillMaxSize().background(if (elegido) Uva.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.18f))
                .clickable { Diptico.elegirWidget(i); Diptico.abrirAjustesWidget(i) }) {
                if (elegido) {
                    Box(Modifier.align(Alignment.TopEnd).padding(esc.d(6)).clip(CircleShape)
                        .background(color).padding(horizontal = esc.d(8), vertical = esc.d(3))) {
                        Texto("$an × $al", 11.5f, color = SobreUva, lineas = 1, traducir = false)
                    }
                    Box(Modifier.align(Alignment.BottomEnd).padding(esc.d(4)).size(esc.d(30))
                        .semantics { contentDescription = "Cambiar tamaño" }
                        .clip(CircleShape).background(color).border(esc.d(2), Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { IconoDiptico(Icono.FORMA, Color.White, 14f) }
                }
            }
        }
    }
}

private const val MARGEN_GRILLA = 12f

/** Marco del widget enfocado: una línea firme y un aro suave por fuera (sin sombras ni degradados, que en algunas pantallas se ven punteados). */
@Composable
private fun Modifier.anilloFoco(radio: Float, color: Color): Modifier {
    val esc = LocalEscala.current
    return this.drawBehind {
        val fuera = esc.d(5).toPx()
        val r = esc.d(radio + 2).toPx()
        drawRoundRect(
            color.copy(alpha = 0.28f), topLeft = androidx.compose.ui.geometry.Offset(-fuera, -fuera),
            size = androidx.compose.ui.geometry.Size(size.width + fuera * 2, size.height + fuera * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r + fuera), style = androidx.compose.ui.graphics.drawscope.Stroke(esc.d(3).toPx()),
        )
    }.border(esc.d(3), color, RoundedCornerShape(esc.d(radio + 2)))
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
        w.tipo.startsWith("atajo-") -> WidgetAtajo(e, w.tipo.removePrefix("atajo-"), al)
        w.tipo.startsWith("coleccion-") -> WidgetColeccion(e, w.tipo.removePrefix("coleccion-"), an, al)
        w.tipo.startsWith("imagen") -> WidgetImagen(w)
        w.tipo.startsWith("video") -> WidgetVideo(w, e.sesion == null)
        w.tipo in planWidgets -> WidgetPlanPartida(e, w.tipo, an, al)
        an == 1 && w.tipo !in herramientasWidgets -> WidgetMini(e, w.tipo, al)
        else -> WidgetVista(e, w.tipo, an, al)
    }
}

/** Qué tanto crece el contenido según cuánto más grande es el widget que su tamaño de siempre. */
private fun escalaAuto(w: cl.nikalbazu.diptico.estado.Widget): Float {
    val base = cl.nikalbazu.diptico.estado.Widgets.porDefecto.firstOrNull { it.tipo == w.tipo } ?: return 1f
    // Crece solo si el widget es más grande en las dos direcciones (si solo es más ancho o más alto, el texto no cabría).
    val area = kotlin.math.sqrt((w.an * w.al).toFloat() / (base.an * base.al).toFloat())
    val r = minOf(area, w.an.toFloat() / base.an, w.al.toFloat() / base.al)
    return if (r > 1f) r.coerceAtMost(1.25f) else 1f
}

/** El widget que se está dibujando (su estilo, color, fondo y esquinas). */
private val planWidgets = setOf("proximaPartida", "colaJuegos", "tengo20")
private val herramientasWidgets = setOf("temporizador", "cronometro", "buscar")

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
    if (tipo in planWidgets) {
        WidgetPlanPartida(e, tipo, an, al)
        return
    }
    if (tipo in herramientasWidgets) {
        WidgetHerramienta(e, tipo, an)
        return
    }
    if (compacto && tipo !in setOf("reloj", "musica", "bateria", "frase", "accesos", "notificaciones", "estadisticas", "apps")) {
        WidgetCompacto(e, tipo)
        return
    }
    val variante = LocalWidget.current?.variante ?: 0
    when (tipo) {
        "juego" -> {
            val j = e.seleccionado
            if ((variante == 1 || an <= 2) && j != null) {
                // Arte de fondo (también en los widgets angostos, donde la caja y el título no caben lado a lado): la imagen del juego llena el widget y el título va encima.
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
            if (compacto) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    IconoDiptico(Icono.BATERIA, if (b in 0..19) Coral else p.tinta, 20f, acento = Coral)
                    Spacer(Modifier.width(esc.d(8)))
                    Texto(if (b >= 0) "$b%" else "—", 20f, lineas = 1, traducir = false)
                    Spacer(Modifier.width(esc.d(10)))
                    Box(Modifier.weight(1f).height(esc.d(9)).clip(RoundedCornerShape(50)).background(p.linea)) {
                        Box(Modifier.fillMaxWidth((b.coerceIn(0, 100)) / 100f).fillMaxHeight().clip(RoundedCornerShape(50)).background(if (b in 0..19) Coral else Uva))
                    }
                }
                return@Tarjeta
            }
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Texto(if (b >= 0) "$b%" else "—", 40f)
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
        "paso" -> Tarjeta(alPulsar = { Diptico.abrirQuedaste() }) {
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
        "estadisticas" -> Tarjeta(alPulsar = { Diptico.abrirWidgetTipo("estadisticas") }) {
            val total = e.datos.values.sumOf { it.tiempo }
            if (compacto) {
                // Una fila: el total y la consola más jugada con su barra.
                val top = e.datos.entries.filter { it.value.tiempo > 0 }.groupBy { it.key.substringBefore('/') }
                    .mapValues { (_, v) -> v.sumOf { it.value.tiempo } }.maxByOrNull { it.value }
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    IconoDiptico(Icono.RELOJ, Uva, 20f, acento = Coral)
                    Spacer(Modifier.width(esc.d(8)))
                    Column(Modifier.weight(1f)) {
                        Etiqueta("Estadísticas", p.tenue, 10.5f)
                        Texto(formatoTiempo(total), 15f, lineas = 1)
                    }
                    if (top != null && tam >= 2) {
                        Spacer(Modifier.width(esc.d(8)))
                        Detalle(Consolas.porId(top.key)?.nombre ?: top.key, 12f, lineas = 1)
                    }
                }
                return@Tarjeta
            }
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
        "logros" -> Tarjeta(alPulsar = { Diptico.abrirWidgetTipo("logros") }) {
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
        "asistente" -> WidgetBisa(e, compacto, al, an)
        "apps" -> Tarjeta { WidgetApps(e, an) }
        "accesos" -> Tarjeta {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                // Solo los accesos que caben enteros (nunca uno cortado), y botones que caben en el alto.
                val anchoU = maxWidth.value / esc.factor
                val altoU = maxHeight.value / esc.factor
                val boton = minOf(40f, altoU - (if (al >= 2) 22f else 4f)).coerceAtLeast(24f)
                val cabe = ((anchoU + 6f) / (maxOf(boton, if (al >= 2) 56f else boton) + 6f)).toInt().coerceIn(1, 6)
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Diptico.opcionesCentro().take(cabe).forEach { o ->
                        Column(Modifier.clip(RoundedCornerShape(esc.d(12))).clickable { o.accion() }.padding(esc.d(2)), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(esc.d(boton)).clip(CircleShape).background(p.fondo.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                o.icono?.let { IconoDiptico(it, p.tinta, boton * 0.55f, acento = Coral) }
                            }
                            // Con una sola fila de alto no caben los nombres.
                            if (al >= 2) Texto(o.titulo, 10.5f, color = p.tenue, lineas = 1, modifier = Modifier.widthIn(max = esc.d(60)))
                        }
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
        "tiempo" -> Tarjeta(alPulsar = { Diptico.abrirWidgetTipo("tiempo") }) {
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
            if (compacto) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    IconoDiptico(Icono.MUSICA, Uva, 22f, acento = Coral)
                    Spacer(Modifier.width(esc.d(10)))
                    Column(Modifier.weight(1f)) {
                        Texto("Widget de música", 14.5f, lineas = 1)
                        Detalle("Toca para dar permiso", 11.5f, lineas = 1)
                    }
                }
                return@Tarjeta
            }
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

@Composable
private fun WidgetPlanPartida(e: Estado, tipo: String, an: Int, al: Int) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val juegos = remember(e.juegos, e.datos, tipo) {
        when (tipo) {
            "proximaPartida" -> listOfNotNull(Diptico.ultimoJugado(e))
            "colaJuegos" -> cl.nikalbazu.diptico.estado.PlanPartida.cola(e)
            else -> cl.nikalbazu.diptico.estado.PlanPartida.breves(e)
        }
    }
    val primero = juegos.firstOrNull()
    Tarjeta(alPulsar = { Diptico.abrirWidgetTipo(tipo) }) {
        if (an == 1) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                IconoDiptico(Widgets.icono(tipo), Coral, 22f)
            }
        } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Etiqueta(Widgets.nombre(tipo), p.acentoTexto, 11f)
            Texto(primero?.titulo ?: when (tipo) {
                "proximaPartida" -> "Aún no tienes una sesión"
                "colaJuegos" -> "Agrega un juego pendiente"
                else -> "Aún faltan sesiones"
            }, if (al >= 2) 18f else 13f, lineas = 1)
            if (al >= 2 && primero != null) {
                Spacer(Modifier.height(esc.d(4)))
                val paso = e.datos[primero.clave]?.paso.orEmpty()
                val mapa = e.mapas[primero.clave]
                if (tipo == "proximaPartida") {
                    Detalle(paso.ifBlank { "Anota dónde quedaste" }, 12f, lineas = 2)
                    if (mapa != null) Detalle("Mapa guardado", 11f, lineas = 1)
                } else Detalle(juegos.drop(1).take(2).joinToString(" · ") { it.titulo }
                    .ifBlank { if (tipo == "tengo20") "Según tus sesiones anteriores" else "Un juego en la cola" }, 12f, lineas = 2)
            }
        }
    }
}

@Composable
private fun WidgetHerramienta(e: Estado, tipo: String, an: Int) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val activo = when (tipo) {
        "temporizador" -> e.temporizadorFin > 0L
        "cronometro" -> e.cronoInicio > 0L
        else -> false
    }
    val valor = when (tipo) {
        "temporizador" -> if (activo) cuentaRegresiva(e.temporizadorFin) else "${e.temporizadorMin} min"
        "cronometro" -> cronometro(e)
        else -> ""
    }
    Tarjeta(alPulsar = { Diptico.abrirWidgetTipo(tipo) }) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (an >= 2 || valor.isEmpty()) {
                IconoDiptico(Widgets.icono(tipo), if (activo) Coral else p.tinta, 18f)
                if (an >= 2) Spacer(Modifier.width(esc.d(8)))
            }
            if (valor.isNotEmpty() || an >= 2) Column(Modifier.weight(1f)) {
                if (an >= 2) Detalle(Widgets.nombre(tipo), 12f, lineas = 1)
                if (valor.isNotEmpty()) Texto(valor, if (an == 1) 12f else 19f, lineas = 1)
            }
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
internal fun fraseDelDia(e: Estado): String {
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
internal fun WidgetImagen(w: cl.nikalbazu.diptico.estado.Widget) {
    val esc = LocalEscala.current
    // Sin fondo (siempre, salvo que elijas "Color", "Vidrio" o "Contorno" en el estudio): los PNG y GIF con transparencia
    // flotan sobre el inicio, sin tarjeta detrás. La imagen se ajusta entera al tamaño del widget.
    val forma = RoundedCornerShape(esc.d(w.radio.toFloat()))
    val conFondo = w.estilo == "solido" || w.estilo == "vidrio" || w.estilo == "contorno"
    val propio = w.color.takeIf { it != 0L }?.let { Color(it) } ?: Uva
    Box(
        Modifier.fillMaxSize()
            .then(if (conFondo || w.variante == 1) Modifier.clip(forma) else Modifier)
            .then(
                when (w.estilo) {
                    "solido" -> Modifier.background(propio.copy(alpha = 0.8f * w.opacidad))
                    "vidrio" -> Modifier.background(Color.White.copy(alpha = 0.14f * w.opacidad)).border(esc.d(1.2f), Color.White.copy(alpha = 0.22f), forma)
                    "contorno" -> Modifier.border(esc.d(2), propio.copy(alpha = 0.8f), forma)
                    else -> Modifier
                },
            )
            .combinedClickable(onClick = { Diptico.abrirWidgetTipo(w.tipo) }, onLongClick = { Diptico.editarWidgets() }),
    ) {
        if (w.uri == null) {
            Column(Modifier.fillMaxSize().clip(forma).background(LocalPaleta.current.superficie.copy(alpha = 0.6f)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                IconoDiptico(Icono.PORTADA, LocalPaleta.current.tenue, 22f, acento = Coral)
                Detalle("Toca para elegir", 11f, lineas = 1)
            }
        }
        w.uri?.let { ImagenAnimada(android.net.Uri.parse(it), Modifier.fillMaxSize(), recortar = w.variante == 1) }
    }
}

/**
 * Un video corto como widget: se repite en silencio, decodificado por hardware en una TextureView
 * (no recarga la interfaz) y se pausa solo mientras juegas o cuando no se ve.
 */
@Composable
internal fun WidgetVideo(w: cl.nikalbazu.diptico.estado.Widget, activo: Boolean) {
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
            if (icono != null) IconoDiptico(icono, p.tinta, if (valor.isEmpty()) 24f else if (al >= 2) 18f else 14f, acento = Coral)
            if (valor.isNotEmpty()) {
                if (icono != null && al >= 2) Spacer(Modifier.height(esc.d(2)))
                Texto(valor, if (tipo == "reloj") (if (al >= 2) 26f else 18f) else if (al >= 2) 18f else 15f, lineas = 1, traducir = false, interlineado = if (al >= 2) null else 17f)
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
    var ahora by remember(e.cronoInicio) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(e.cronoInicio) {
        while (e.cronoInicio > 0L) {
            ahora = System.currentTimeMillis()
            kotlinx.coroutines.delay(500)
        }
    }
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
private fun WidgetBisa(e: Estado, compacto: Boolean, al: Int, an: Int = 4) {
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
                        ChipBisa("Hablar") { Diptico.hablarConBisa() }
                        // Los accesos que caben: uno en los angostos, dos en los medianos y tres en los anchos.
                        if (j != null && an >= 4) ChipBisa("Un truco") { Diptico.abrirAsistente(); Diptico.preguntar("Dame un buen truco o consejo para ${j.titulo}.") }
                        if (an >= 3) ChipBisa("¿Qué juego?") { Diptico.abrirAsistente(); Diptico.preguntar("¿Qué juego de mi colección me recomiendas para hoy? Dame 3 opciones y por qué.") }
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
            // Solo las pistas que caben (← → solo si alguna opción se cambia hacia los lados).
            if (opciones.any { it.menos != null }) Pistas("↑ ↓" to "Moverte", "A" to "Elegir", "← →" to "Cambiar")
            else Pistas("↑ ↓" to "Moverte", "A" to "Elegir", "B" to "Volver")
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
        if (p.fondo.luminance() < 0.4f) Box(Modifier.align(Alignment.TopEnd).size(esc.d(480)).background(Brush.radialGradient(listOf(Uva.copy(alpha = 0.22f), Color.Transparent))))
        // Los widgets con diseño propio en grande (tiempo, colección, batería, música, frase, fiestas, logros, imágenes…).
        if (w.tipo != "reloj" && w.tipo != "asistente" && LecturaGrande(e, w, opcion)) return@Box
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

/**
 * Muestrario para revisar el diseño: todos los widgets con el mismo tamaño ([an] × [al] celdas), uno al lado del otro,
 * con el tamaño de celda real de la pantalla de arriba. Solo lo usan las capturas de prueba.
 */
@Composable
internal fun MuestrarioWidgets(e: Estado, an: Int, al: Int, foco: Int = -1) {
    // La misma escala de la pantalla de arriba de la Thor (411 dp de alto), aunque la foto sea más grande.
    val e0 = LocalEscala.current
    CompositionLocalProvider(LocalEscala provides Escala(411f / ALTO_DISENO, e0.ancho, e0.texto)) { MuestrarioWidgets0(e, an, al, foco) }
}

@Composable
private fun MuestrarioWidgets0(e: Estado, an: Int, al: Int, foco: Int) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val hueco = e.huecoInicio.toFloat()
    val celdaAn = 102f
    val celdaAl = 63f
    val ancho = an * celdaAn + (an - 1) * hueco
    val alto = al * celdaAl + (al - 1) * hueco
    val tipos = Widgets.porDefecto.map { it.tipo } + listOf("imagen1")
    BoxWithConstraints(Modifier.fillMaxSize().background(p.fondo).padding(esc.d(16))) {
        val porFila = ((maxWidth.value / esc.factor + 18f) / (ancho + 18f)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(esc.d(18))) {
            Texto("Widgets de $an × $al", 18f, lineas = 1, traducir = false)
            tipos.chunked(porFila).forEachIndexed { f, fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(18))) {
                    fila.forEachIndexed { c, tipo ->
                        val k = f * porFila + c
                        val w = (Widgets.porDefecto.firstOrNull { it.tipo == tipo } ?: Widget(tipo, 0, 0, an, al)).copy(an = an, al = al, visible = true)
                        Column {
                            Detalle(tipo, 10f, lineas = 1)
                            Box(Modifier.size(esc.d(ancho), esc.d(alto)).then(if (k == foco) Modifier.anilloFoco(w.radio.toFloat(), if (p.fondo.luminance() < 0.4f) Color.White else Uva) else Modifier)) {
                                val auto = remember(w.tipo, w.an, w.al) { escalaAuto(w) }
                                CompositionLocalProvider(LocalWidget provides w, LocalEscala provides Escala(esc.factor * w.escala * auto, esc.ancho, esc.texto)) {
                                    ContenidoWidget(e, w, an, al)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


/** Acceso directo a un juego: su portada llena el widget, con el título abajo. Un toque y a jugar. */
@Composable
private fun WidgetAtajo(e: Estado, clave: String, al: Int) {
    val esc = LocalEscala.current
    val j = e.juegos.firstOrNull { it.clave == clave }
    val forma = RoundedCornerShape(esc.d((LocalWidget.current?.radio ?: 22).toFloat()))
    Box(
        Modifier.fillMaxSize().clip(forma).background(LocalPaleta.current.superficie)
            .combinedClickable(onClick = { Diptico.abrirWidgetTipo("atajo-$clave") }, onLongClick = { Diptico.editarWidgets() }),
    ) {
        if (j == null) {
            Detalle("Juego no encontrado", 11f, modifier = Modifier.align(Alignment.Center).padding(esc.d(6)))
            return@Box
        }
        // Qué muestra (Diseño en los ajustes del widget): portada, imagen grande, logo o logo sobre la imagen grande.
        val variante = LocalWidget.current?.variante ?: 0
        when (variante) {
            1 -> ArteJuego(j, preferirArte = true, maxLado = 640, modifier = Modifier.fillMaxSize())
            2 -> ArteJuego(j, preferirArte = false, maxLado = 400, modifier = Modifier.fillMaxSize(), imagen = "logo")
            3 -> {
                ArteJuego(j, preferirArte = true, maxLado = 640, modifier = Modifier.fillMaxSize())
                val logo = cl.nikalbazu.diptico.ui.rememberImagen(j.logo, 400)
                if (logo != null) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
                    androidx.compose.foundation.Image(
                        logo, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier.align(Alignment.Center).fillMaxSize(0.72f),
                    )
                }
            }
            else -> ArteJuego(j, preferirArte = false, maxLado = 400, modifier = Modifier.fillMaxSize())
        }
        // El nombre solo hace falta cuando no se ve el logo.
        val conLogo = (variante == 2 || variante == 3) && j.logo != null
        if (!conLogo) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.8f))))
            Column(Modifier.align(Alignment.BottomStart).padding(esc.d(8))) {
                Texto(j.titulo, if (al >= 2) 13f else 11f, color = Color.White, lineas = 2, sombra = true, interlineado = if (al >= 2) 15f else 12.5f)
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(esc.d(6)).size(esc.d(24)).clip(CircleShape).background(Uva),
            contentAlignment = Alignment.Center,
        ) { IconoDiptico(Icono.JUGAR, SobreUva, 11f) }
    }
}

/** Una colección: su nombre y las portadas de sus juegos en mosaico (como una carpeta abierta). */
@Composable
private fun WidgetColeccion(e: Estado, id: String, an: Int, al: Int) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val c = e.colecciones[id]
    Tarjeta(alPulsar = { Diptico.abrirColeccionVisual(id) }) {
        if (c == null) { Detalle("Colección borrada", 12f); return@Tarjeta }
        val juegos = c.juegos.mapNotNull { k -> e.juegos.firstOrNull { it.clave == k } }
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.CARPETA, Coral, 16f)
                Spacer(Modifier.width(esc.d(6)))
                Texto(c.nombre, 14f, lineas = 1, traducir = false, modifier = Modifier.weight(1f))
                Detalle("${juegos.size}", 12f, lineas = 1)
            }
            if (al >= 2 || an >= 3) {
                Spacer(Modifier.height(esc.d(6)))
                if (juegos.isEmpty()) Detalle("Vacía: agrega juegos desde la ventana de cada juego (Y).", 11.5f, lineas = 3)
                else BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    // Tantas portadas como quepan, del mismo alto.
                    val altoU = maxHeight.value / esc.factor
                    val anchoU = maxWidth.value / esc.factor
                    val ancho = altoU * 0.72f
                    val caben = ((anchoU + 6f) / (ancho + 6f)).toInt().coerceAtLeast(1)
                    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        juegos.take(caben).forEach { j ->
                            Box(
                                Modifier.size(esc.d(ancho), esc.d(altoU)).clip(RoundedCornerShape(esc.d(8)))
                                    .clickable { Diptico.elegirConsola("todo"); Diptico.elegirJuego(j.clave) },
                            ) { ArteJuego(j, preferirArte = false, maxLado = 240, modifier = Modifier.fillMaxSize()) }
                        }
                    }
                }
            }
        }
    }
}



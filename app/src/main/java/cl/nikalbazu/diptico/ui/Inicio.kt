@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        Column(Modifier.fillMaxSize().padding(start = esc.d(20), end = esc.d(20), top = esc.d(48), bottom = esc.d(12))) {
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
                Spacer(Modifier.height(esc.d(6)))
                BarraEdicion(e)
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
private fun Grilla(e: Estado, pagina: Int, modifier: Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
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
        Box(
            Modifier.fillMaxSize().verticalScroll(desplazar, enabled = Widgets.FILAS > 4 && !e.editandoWidgets)
                // Mantener apretado en cualquier parte entra al modo de edición.
                .pointerInput(e.editandoWidgets) { if (!e.editandoWidgets) detectTapGestures(onLongPress = { Diptico.editarWidgets() }) },
        ) {
        // Alto total de la grilla (más alto que la pantalla si hay más de 4 filas).
        Box(Modifier.size(esc.d(anchoU), esc.d(Widgets.FILAS * celdaAl + (Widgets.FILAS - 1) * hueco)))
        val pasoX = with(densidad) { esc.d(celdaAn + hueco).toPx() }
        val pasoY = with(densidad) { esc.d(celdaAl + hueco).toPx() }

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
                var arrastre by remember { mutableStateOf(Offset.Zero) }
                val x by animateDpAsState(esc.d(w.x * (celdaAn + hueco)), spring(dampingRatio = 0.75f, stiffness = 500f), label = "x")
                val y by animateDpAsState(esc.d(w.y * (celdaAl + hueco)), spring(dampingRatio = 0.75f, stiffness = 500f), label = "y")
                val an by animateDpAsState(esc.d(w.an * celdaAn + (w.an - 1) * hueco), spring(dampingRatio = 0.8f), label = "an")
                val al by animateDpAsState(esc.d(w.al * celdaAl + (w.al - 1) * hueco), spring(dampingRatio = 0.8f), label = "al")
                val tam by animateFloatAsState(if (elegido) 1.03f else 1f, spring(dampingRatio = 0.5f), label = "tam")
                // En modo edición los widgets tiemblan un poquito (cada uno a su ritmo), como en los teléfonos.
                val tiemble = if (e.editandoWidgets) {
                    val t = rememberInfiniteTransition(label = "tiemble")
                    t.animateFloat(-1f, 1f, infiniteRepeatable(tween(260 + (i % 4) * 35), RepeatMode.Reverse), label = "rot").value
                } else 0f
                Box(
                    Modifier.offset(x = x, y = y).size(an, al)
                        .zIndex(if (elegido) 2f else 1f)
                        .graphicsLayer {
                            translationX = arrastre.x; translationY = arrastre.y; scaleX = tam; scaleY = tam
                            rotationZ = if (elegido) tiemble * 0.4f else tiemble * 0.8f
                        }
                        .then(if (elegido) Modifier.border(esc.d(3), Uva, RoundedCornerShape(esc.d(22))) else Modifier),
                ) {
                    CompositionLocalProvider(LocalWidget provides w) {
                        if (w.tipo.startsWith("imagen")) WidgetImagen(w) else WidgetVista(e, w.tipo, w.an, w.al)
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
                                .clickable { Diptico.elegirWidget(i) },
                        ) {
                            if (elegido) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(esc.d(6)).clip(RoundedCornerShape(50)).background(Uva)
                                        .padding(horizontal = esc.d(8), vertical = esc.d(3)),
                                ) { Texto(Widgets.nombreTamano(w.an, w.al), 11.5f, color = SobreUva, lineas = 1) }
                                // Botones táctiles: achicar/agrandar, estilo y quitar.
                                Row(
                                    Modifier.align(Alignment.BottomCenter).padding(esc.d(6)),
                                    horizontalArrangement = Arrangement.spacedBy(esc.d(5)),
                                ) {
                                    BotonMini("−") { Diptico.tamanoWidgetSel(-1) }
                                    BotonMini("+") { Diptico.tamanoWidgetSel(1) }
                                    BotonMini("⚙") { Diptico.abrirAjustesWidget(i) }
                                    BotonMini("✕") { Diptico.quitarWidget(i) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

/** El widget que se está dibujando (su estilo, color, fondo y esquinas). */
private val LocalWidget = androidx.compose.runtime.compositionLocalOf<cl.nikalbazu.diptico.estado.Widget?> { null }

@Composable
private fun BotonMini(texto: String, accion: () -> Unit) {
    val esc = LocalEscala.current
    Box(
        Modifier.size(esc.d(30)).clip(CircleShape).background(Uva).clickable { accion() },
        contentAlignment = Alignment.Center,
    ) { Texto(texto, 14f, color = SobreUva, lineas = 1, traducir = false) }
}

/** Herramientas al acomodar el inicio: agregar, grilla, páginas, fondo, ajustes y listo. */
@Composable
private fun BarraEdicion(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    @Composable
    fun Chip(texto: String, icono: Icono?, activo: Boolean = false, accion: () -> Unit) {
        Row(
            Modifier.clip(RoundedCornerShape(50)).then(if (activo) Modifier.fondoBoton(Uva) else Modifier.background(p.superficie.copy(alpha = 0.9f)))
                .clickable { accion() }.padding(horizontal = esc.d(11), vertical = esc.d(6)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icono != null) { IconoDiptico(icono, if (activo) SobreUva else p.tinta, 14f, acento = if (activo) SobreUva else Coral); Spacer(Modifier.width(esc.d(5))) }
            Texto(texto, 12.5f, color = if (activo) SobreUva else p.tinta, lineas = 1)
        }
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(esc.d(6)), verticalAlignment = Alignment.CenterVertically,
    ) {
        Chip("Agregar widget", Icono.APPS) { Diptico.actualizar { it.copy(panel = cl.nikalbazu.diptico.estado.Panel.WIDGETS, indice = 0) } }
        Chip("Columnas ${Widgets.COLUMNAS}", null) { Diptico.ciclarGrilla(columnas = true) }
        Chip("Filas ${Widgets.FILAS}", null) { Diptico.ciclarGrilla(columnas = false) }
        Chip("Páginas ${e.paginas}", null) { Diptico.ciclarPaginas() }
        Chip("Fondo", Icono.PORTADA) { Diptico.elegirFondoInicio() }
        e.widgets.getOrNull(e.widgetSel)?.let { w -> Chip("Estilo: ${Widgets.nombreEstilo(w.estilo)}", Icono.PALETA) { Diptico.cambiarEstiloWidget() } }
        Chip("Más ajustes", Icono.AJUSTES) { Diptico.abrirMenu(cl.nikalbazu.diptico.estado.Seccion.INICIO) }
        Chip("Listo", Icono.CHECK, activo = true) { Diptico.terminarEdicion() }
    }
    Spacer(Modifier.height(esc.d(4)))
    Detalle("Cruceta mueve · L1 R1 ancho · L2 R2 alto · A ajustes del widget · X estilo · Select otro · B listo", 11.5f, lineas = 1)
}

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
            .combinedClickable(onClick = { alPulsar?.invoke() }, onLongClick = { Diptico.editarWidgets() })
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
    if (compacto && tipo !in setOf("reloj", "musica", "bateria", "frase", "accesos", "notificaciones", "estadisticas")) {
        WidgetCompacto(e, tipo)
        return
    }
    when (tipo) {
        "juego" -> {
            val j = e.seleccionado
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
        "reloj" -> Tarjeta(alPulsar = { Diptico.alternarReloj() }) {
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
            val favs = e.juegos.filter { e.datos[it.clave]?.favorito == true }.take(if (tam >= 3) 6 else if (tam == 2) 4 else 2)
            Column {
                Etiqueta("Favoritos", Coral, 11f)
                Spacer(Modifier.height(esc.d(8)))
                if (favs.isEmpty()) Detalle("Marca favoritos con Y sobre un juego", 14f)
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
        "asistente" -> Tarjeta(alPulsar = { Diptico.abrirAsistente() }) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(esc.d(40)).clip(CircleShape).fondoBoton(Uva), contentAlignment = Alignment.Center) {
                    IconoDiptico(Icono.ASISTENTE, SobreUva, 22f, acento = SobreUva)
                }
                Spacer(Modifier.width(esc.d(10)))
                Column(Modifier.weight(1f)) {
                    Texto("Pregúntale al asistente", 15f, lineas = 1)
                    if (!compacto) Detalle("Qué jugar, trucos, o cambiar Díptico", 12.5f, lineas = 1)
                }
            }
        }
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
                .mapNotNull { (k, _) -> e.juegos.firstOrNull { it.clave == k } }.take(if (tam >= 3) 5 else if (tam == 2) 3 else 2)
            Column {
                Etiqueta("Jugados hace poco", p.tenue, 11f)
                Spacer(Modifier.height(esc.d(8)))
                if (recientes.isEmpty()) Detalle("Todavía no juegas nada con Díptico", 14f)
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
    )
    e.fiesta?.tema?.mensaje?.takeIf { it.isNotBlank() }?.let { return it }
    return frases[(java.time.LocalDate.now().dayOfYear) % frases.size]
}

/** Agregar o quitar widgets del inicio (A los muestra u oculta). */
@Composable
fun PanelWidgets(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val lista = rememberLazyListState()
    LaunchedEffect(e.indice) { lista.animateScrollToItem((e.indice - 2).coerceAtLeast(0)) }
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Column(Modifier.fillMaxSize().padding(start = esc.d(16), end = esc.d(16), top = esc.d(12), bottom = esc.d(10))) {
            Row(Modifier.fillMaxWidth().height(esc.d(40)), verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.APPS, p.tinta, 24f, acento = Coral)
                Spacer(Modifier.width(esc.d(10)))
                Texto("Agregar o quitar widgets", 20f, modifier = Modifier.weight(1f), lineas = 1)
                Detalle("${e.widgets.count { it.visible }} a la vista", 14f)
            }
            Spacer(Modifier.height(esc.d(8)))
            LazyColumn(state = lista, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                itemsIndexed(e.widgets, key = { _, w -> w.tipo }) { i, w ->
                    val activa = i == e.indice
                    Row(
                        Modifier.fillMaxWidth().height(esc.d(52)).clip(RoundedCornerShape(esc.d(16)))
                            .background(if (activa) Uva else p.superficie.copy(alpha = 0.75f))
                            .clickable { Diptico.elegirIndice(i) }
                            .padding(horizontal = esc.d(14)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Texto(Widgets.nombre(w.tipo), 15.5f, color = if (activa) SobreUva else p.tinta, lineas = 1, modifier = Modifier.weight(1f))
                        Texto(if (w.visible) Widgets.nombreTamano(w.an, w.al) else "Oculto", 12.5f, color = if (activa) SobreUva else p.tenue, modifier = Modifier.padding(end = esc.d(10)))
                        Box(
                            Modifier.size(esc.d(46), esc.d(26)).clip(RoundedCornerShape(50))
                                .background(if (w.visible) (if (activa) SobreUva else Uva) else p.linea),
                            contentAlignment = if (w.visible) Alignment.CenterEnd else Alignment.CenterStart,
                        ) {
                            Box(Modifier.padding(esc.d(3)).size(esc.d(20)).clip(CircleShape).background(if (w.visible && activa) Uva else Color.White))
                        }
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("A" to "Mostrar u ocultar", "B" to "Listo")
        }
    }
}

/** Un widget con tu propia imagen o GIF animado. */
@Composable
private fun WidgetImagen(w: cl.nikalbazu.diptico.estado.Widget) {
    val esc = LocalEscala.current
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(esc.d(22))).background(Color.Black)) {
        w.uri?.let { ImagenAnimada(android.net.Uri.parse(it), Modifier.fillMaxSize()) }
    }
}

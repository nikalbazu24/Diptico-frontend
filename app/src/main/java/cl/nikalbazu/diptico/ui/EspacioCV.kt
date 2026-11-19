package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.drawBehind
import cl.nikalbazu.diptico.estado.*

/*
 * Tu espacio (Widgets 3.0, boceto 10a–10d) con Colección viva:
 *  - arriba, tus páginas de widgets (empiezan vacías, DIPPO propone tres);
 *  - abajo, los accesos (Editar, Buscar, Fondo), las páginas y su tamaño, y la tienda por categorías;
 *  - con una pantalla, la tienda entra por el costado.
 * L2 y R2 pasan del inicio a Tu espacio y de vuelta (ya no hay botón grande de volver).
 */

/** Encabezado de Tu espacio (arriba): Inicio | Tu espacio, Editar, la tienda (solo con una pantalla), la hora y tu foto. */
@Composable
internal fun EncabezadoEspacioCV(e: Estado) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    @Composable
    fun Pildora(t: String, on: Boolean, desc: String? = null, accion: (() -> Unit)?) {
        Box(Modifier.clip(RoundedCornerShape(50)).background(if (on) p.tinta else p.tinta.copy(alpha = 0.08f))
            .border(esc.d(1), p.tinta.copy(alpha = if (on) 0f else 0.14f), RoundedCornerShape(50))
            .then(if (accion != null) Modifier.clickable(onClickLabel = desc, onClick = accion) else Modifier).padding(horizontal = esc.d(14), vertical = esc.d(7))) {
            Texto(t, 13.5f, color = if (on) p.fondo else p.tinta, lineas = 1)
        }
    }
    Row(Modifier.fillMaxWidth().anclaRecorrido("espacio").testTag("encabezado-espacio"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
        // Las dos páginas de la tira, con la pista de los gatillos.
        Row(Modifier.clip(RoundedCornerShape(50)).background(p.tinta.copy(alpha = 0.06f)).padding(esc.d(3)), horizontalArrangement = Arrangement.spacedBy(esc.d(2))) {
            Pildora("Inicio", false, "Volver al inicio") { Diptico.fijarInicio("heroe") }
            Pildora("Tu espacio", true, "Página siguiente", if (e.paginas > 1) ({ Diptico.irAPagina((e.paginaInicio + 1) % e.paginas) }) else null)
        }
        Texto("L2 · R2", 11.5f, color = p.tenue, lineas = 1, traducir = false, modifier = Modifier.padding(start = esc.d(4)))
        if (e.paginas > 1) Texto("${e.paginaInicio + 1}/${e.paginas}", 12f, color = p.tenue, lineas = 1, traducir = false)
        Spacer(Modifier.weight(1f))
        if (!e.espacioVacio()) Pildora("Editar", false) { Diptico.editarWidgets() }
        Pildora(if (e.extra.galeria) "Cerrar la Galería" else "Galería de widgets", e.extra.galeria) {
            if (e.extra.galeria) Diptico.cerrarGaleriaWidgets() else Diptico.abrirGaleriaWidgets()
        }
        PerfilArriba(50f, conReloj = true)
    }
}

/** Tu espacio vacío: DIPPO invita y propone tres widgets según cómo usas la consola. */
@Composable
internal fun EspacioVacioCV(e: Estado, modifier: Modifier = Modifier) {
    val sugeridos = Diptico.sugeridosEspacio(e)
    LienzoBoceto(1920f, 900f, modifier.testTag("espacio-vacio"), zoom = if (e.unaPantalla) ZOOM_UNA else ZOOM_ARRIBA) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        Column(
            Modifier.align(Alignment.Center).width(esc.d(1180)).vidrio(44f, fuerte = true).padding(horizontal = esc.d(48), vertical = esc.d(36)),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(18)),
        ) {
            cl.nikalbazu.diptico.bisa.AvatarBisa(118f * esc.factor)
            TB("Este espacio es tuyo", 46f, titulo = true)
            TB("Empieza vacío. Según cómo usas tu consola, te propongo estos tres:", 24f, color = col.sub, lineas = 2)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                sugeridos.forEach { (tipo, porque) ->
                    Column(
                        Modifier.weight(1f).height(esc.d(160)).clip(RoundedCornerShape(esc.d(28))).background(col.texto.copy(alpha = .06f))
                            .border(1.dp, col.borde, RoundedCornerShape(esc.d(28))).clickable { Diptico.ponerEnEspacio(tipo) }.padding(esc.d(18)),
                        verticalArrangement = Arrangement.spacedBy(esc.d(8)),
                    ) {
                        Box(Modifier.size(esc.d(52)).clip(RoundedCornerShape(esc.d(16))).background(col.acento.copy(alpha = .35f)), contentAlignment = Alignment.Center) {
                            IconoDiptico(Widgets.icono(tipo), col.texto, 34f)
                        }
                        TB(Widgets.nombre(tipo), 23f, peso = FontWeight.ExtraBold)
                        TB(porque, 18f, color = col.sub, lineas = 2, traducir = false)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                BotonTecla("A", "Agregar los 3", { Diptico.agregarSugeridos() }, alto = 72f, principal = true, letra = 24f)
                BotonTecla("Y", "Abrir la Galería", { Diptico.abrirGaleriaWidgets() }, alto = 72f, letra = 24f)
            }
        }
    }
}

/**
 * La pantalla de abajo mientras estás en Tu espacio (boceto S221ptEpUYaNtWFkc4kbQ9, a tamaño real de 1240 × 1080):
 *  - arriba, «Tu espacio» y la tira de páginas (L2/R2);
 *  - la Galería de widgets como un solo botón grande y dorado;
 *  - al medio, el mapa de la página que miras y el botón grande «Editar espacio» (X);
 *  - con Editar, el medio se transforma en pestañas Mover · Tamaño · Páginas · Fondo (nada se abre encima; B termina);
 *  - abajo, el dock de apps a la izquierda y DIPPO con su globito a la derecha. Sin pistas de botones.
 */
@Composable
fun EspacioAbajoCV(e: Estado) {
    if (e.extra.galeria && e.extra.espacioEdita < 0) { GaleriaAbajoCV(e); return }
    val editando = e.extra.espacioEdita >= 0
    LienzoBoceto(1240f, 1080f, Modifier.testTag("espacio-abajo"), fondo = LocalColeccion.current.fondo) {
        val esc = LocalEscala.current
        Box(Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.radialGradient(listOf(Color(0xFF1B1F4A).copy(alpha = .55f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width * .8f, 0f), radius = size.width * .9f))
        })
        Column(Modifier.fillMaxSize().padding(start = esc.d(44), end = esc.d(44), top = esc.d(40)), verticalArrangement = Arrangement.spacedBy(esc.d(26))) {
            CabeceraEspacio(e)
            if (editando) EdicionEspacio(e, Modifier.fillMaxWidth().weight(1f))
            else {
                BotonGaleria(Modifier.fillMaxWidth().height(esc.d(230)))
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(26))) {
                    TarjetaMapa(e, false, Modifier.weight(1f).fillMaxHeight())
                    BotonEditarEspacio(Modifier.width(esc.d(330)).fillMaxHeight())
                }
            }
            PieEspacio(e)
        }
    }
}

/** «Tu espacio» con la tira de páginas al lado: L2 ‹ Inicio · Pág 1 · Pág 2 · + › R2. */
@Composable
private fun CabeceraEspacio(e: Estado) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val editando = e.extra.espacioEdita >= 0
    Row(Modifier.fillMaxWidth().height(esc.d(84)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(20))) {
        TB("Tu espacio", 54f, titulo = true)
        Gatillo("L2") { if (editando) Diptico.irAPagina(e.paginaInicio - 1) else Diptico.pasoEspacio(-1) }
        val paginas = (if (editando) emptyList() else listOf("Inicio" to "tu colección")) + (0 until e.paginas).map { p ->
            val n = e.widgets.count { it.visible && it.pagina == p }
            "Pág ${p + 1}" to (if (n == 0) "vacía" else if (n == 1) "1 widget" else "$n widgets")
        }
        val desfase = if (editando) 0 else 1
        val conMas = e.paginas < 12
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.weight(1f)) {
            val hueco = 12f
            val total = paginas.size + if (conMas) .6f else 0f
            val ancho = ((maxWidth.value / esc.factor - hueco * (paginas.size)) / total).coerceIn(130f, 230f)
            val scroll = rememberScrollState()
            Row(Modifier.fillMaxWidth().horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(esc.d(hueco))) {
                paginas.forEachIndexed { k, (t, nota) ->
                    val pagina = k - desfase
                    val on = pagina == e.paginaInicio
                    Column(
                        Modifier.width(esc.d(ancho)).height(esc.d(76)).clip(RoundedCornerShape(esc.d(22)))
                            .background(if (on) col.elegido else col.texto.copy(alpha = .07f))
                            .border(esc.d(2), if (on) col.elegido else col.texto.copy(alpha = .11f), RoundedCornerShape(esc.d(22)))
                            .clickable(onClickLabel = t) { if (pagina < 0) Diptico.fijarInicio("heroe") else Diptico.irAPagina(pagina) }
                            .testTag("pagina-$k"),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        TB(t, 26f, peso = FontWeight.ExtraBold, color = if (on) col.enElegido else col.texto)
                        TB(nota, 17f, color = if (on) col.enElegido.copy(alpha = .65f) else col.sub)
                    }
                }
                if (conMas) Column(
                    Modifier.width(esc.d(ancho * .6f)).height(esc.d(76)).clip(RoundedCornerShape(esc.d(22)))
                        .border(esc.d(2), col.texto.copy(alpha = .22f), RoundedCornerShape(esc.d(22)))
                        .clickable(onClickLabel = "Agregar página") { Diptico.agregarPaginaEspacio() },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) { TB("+", 26f, peso = FontWeight.ExtraBold, traducir = false); TB("Página", 17f, color = col.sub) }
            }
            val elegida = e.paginaInicio + desfase
            val dens = androidx.compose.ui.platform.LocalDensity.current.density
            LaunchedEffect(elegida, ancho) { scroll.animateScrollTo(((ancho + hueco) * (elegida - 1).coerceAtLeast(0) * esc.factor * dens).toInt()) }
        }
        Gatillo("R2") { if (editando) Diptico.irAPagina(e.paginaInicio + 1) else Diptico.pasoEspacio(1) }
    }
}

/** El círculo de un gatillo (L2/R2), que también se toca. */
@Composable
private fun Gatillo(t: String, accion: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Box(Modifier.size(esc.d(56)).clip(CircleShape).border(esc.d(3), col.texto.copy(alpha = .55f), CircleShape).clickable(onClick = accion),
        contentAlignment = Alignment.Center) { TB(t, 18f, peso = FontWeight.ExtraBold, color = col.texto.copy(alpha = .8f), traducir = false) }
}

/** El botón grande y dorado de la Galería de widgets. */
@Composable
private fun BotonGaleria(modifier: Modifier) {
    val esc = LocalEscala.current
    val forma = RoundedCornerShape(esc.d(34))
    Row(
        modifier.clip(forma)
            .background(Brush.horizontalGradient(0f to Color(0xFFF1C757), .38f to Color(0xFFC9952F), .78f to Color(0xFF3A2F2C), 1f to Color(0xFF1A1C30)))
            .border(esc.d(4), Color.White, forma)
            .clickable(onClickLabel = "Abrir la Galería de widgets") { Diptico.abrirGaleriaWidgets() }
            .testTag("abrir-galeria").padding(horizontal = esc.d(46)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(40)),
    ) {
        MosaicoGaleria(Modifier.size(esc.d(200), esc.d(134)))
        Column(Modifier.weight(1f)) {
            TB("Galería de widgets", 50f, titulo = true, color = Color(0xFF1A1306))
            TB("${totalGaleria()} widgets. Lo que elijas va a la página que estás mirando.", 24f, color = Color(0xFF3A2A08),
                peso = FontWeight.Bold, lineas = 2, modifier = Modifier.padding(top = esc.d(4)))
        }
    }
}

/** El dibujito del botón: cuatro widgets de colores acomodados. */
@Composable
private fun MosaicoGaleria(modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val g = size.width * .05f
        val c1 = (size.width - g) * 1.3f / 2.3f; val c2 = size.width - g - c1
        val ch = (size.height - g) / 2f
        val r = androidx.compose.ui.geometry.CornerRadius(ch * .26f)
        fun caja(x: Float, y: Float, w: Float, c: Long) = drawRoundRect(Color(c), androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Size(w, ch), r)
        caja(0f, 0f, c1, 0xFFFFFBE9); caja(c1 + g, 0f, c2, 0xFF6B6BFF); caja(0f, ch + g, c1, 0xFF1FB59A); caja(c1 + g, ch + g, c2, 0xFFF6DCAE)
    }
}

/** El botón grande «Editar espacio» (X). */
@Composable
private fun BotonEditarEspacio(modifier: Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val forma = RoundedCornerShape(esc.d(30))
    Column(
        modifier.clip(forma).background(col.texto.copy(alpha = .08f)).border(esc.d(3), col.texto.copy(alpha = .25f), forma)
            .clickable(onClickLabel = "Editar espacio") { Diptico.editarEspacio() }.testTag("editar-espacio").padding(esc.d(30)),
        verticalArrangement = Arrangement.spacedBy(esc.d(16), Alignment.CenterVertically),
    ) {
        Box(Modifier.size(esc.d(66)).clip(CircleShape).border(esc.d(4), col.texto, CircleShape), contentAlignment = Alignment.Center) {
            TB("X", 28f, peso = FontWeight.ExtraBold, traducir = false)
        }
        TB("Editar espacio", 38f, titulo = true, lineas = 2)
        TB("Mover widgets, tamaño, páginas y fondo, todo aquí.", 21f, color = col.sub, lineas = 3)
    }
}

/** Colores de los widgets en el mapa (los del boceto), con letra oscura encima. */
private val COLORES_MAPA = listOf(0xFFFFFBE9, 0xFF6B6BFF, 0xFF1FB59A, 0xFFF6DCAE, 0xFFFF8A6B)

/** Fondo del mapa: el fondo elegido o el vidrio de siempre. */
private fun fondoMapa(e: Estado): Brush? = FONDOS_ESPACIO.getOrNull(e.extra.fondoEspacio)?.let { c ->
    Brush.linearGradient(c.map { Color(it) })
}

/** La tarjeta con el mapa de la página que estás mirando. */
@Composable
private fun TarjetaMapa(e: Estado, mover: Boolean, modifier: Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val forma = RoundedCornerShape(esc.d(30))
    Column(
        modifier.clip(forma).background(col.texto.copy(alpha = .04f)).border(esc.d(2), col.texto.copy(alpha = .10f), forma)
            .padding(horizontal = esc.d(28), vertical = esc.d(24)),
        verticalArrangement = Arrangement.spacedBy(esc.d(16)),
    ) {
        Row(Modifier.fillMaxWidth()) {
            val an = e.extra.paginaAncho; val al = e.extra.paginaAlto
            TB(if (mover || e.extra.espacioEdita >= 0) "Pág ${e.paginaInicio + 1} · $an × $al" else "Pág ${e.paginaInicio + 1}", 24f, peso = FontWeight.ExtraBold,
                color = col.sub, modifier = Modifier.weight(1f), traducir = false)
            TB(if (e.extra.espacioEdita >= 0) "B · Listo" else "$an × $al pantalla${if (an * al > 1) "s" else ""}", 24f, peso = FontWeight.ExtraBold, color = col.sub)
        }
        MapaEspacio(e, mover, Modifier.fillMaxWidth().weight(1f))
    }
}

/**
 * La cuadrícula de la página con sus widgets. En «Mover», el elegido lleva un anillo blanco y se arrastra con el dedo;
 * al soltar, cae en la celda más cercana (los vecinos se corren). Fuera del modo edición, tocar un widget entra a moverlo.
 */
@Composable
private fun MapaEspacio(e: Estado, mover: Boolean, modifier: Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val cols = Widgets.COLUMNAS.coerceAtLeast(1); val filas = Widgets.FILAS.coerceAtLeast(1)
    val enPagina = e.widgets.withIndex().filter { (_, w) -> w.visible && w.pagina == e.paginaInicio }
    val fondo = fondoMapa(e)
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.clip(RoundedCornerShape(esc.d(20))).then(if (fondo != null) Modifier.background(fondo) else Modifier)
        .testTag("mapa-espacio")) {
        val g = 12f
        val anchoU = maxWidth.value / esc.factor; val altoU = maxHeight.value / esc.factor
        val cw = (anchoU - g * (cols - 1)) / cols; val ch = (altoU - g * (filas - 1)) / filas
        // Las celdas punteadas.
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val f = size.width / anchoU
            val r = androidx.compose.ui.geometry.CornerRadius(14f * f)
            for (y in 0 until filas) for (x in 0 until cols) drawRoundRect(col.texto.copy(alpha = .09f),
                androidx.compose.ui.geometry.Offset(x * (cw + g) * f, y * (ch + g) * f), androidx.compose.ui.geometry.Size(cw * f, ch * f), r,
                style = androidx.compose.ui.graphics.drawscope.Stroke(2f * f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f * f, 8f * f))))
        }
        if (enPagina.isEmpty()) Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            TB("Página vacía", 24f, color = col.sub)
            TB("Abre la Galería y elige uno", 18f, color = col.sub)
        }
        enPagina.forEachIndexed { orden, (i, w) ->
            val an = w.an.coerceAtMost(cols - w.x).coerceAtLeast(1); val al = w.al.coerceAtMost(filas - w.y).coerceAtLeast(1)
            val elegido = mover && i == e.widgetSel
            var arrastre by androidx.compose.runtime.remember(i, w.x, w.y) { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
            // Un único contorno redondeado: mismo radio para el recorte, el fondo
            // y la selección. Antes había dos radios y un borde sin recorte exterior.
            val formaWidget = RoundedCornerShape(esc.d(20))
            Box(
                Modifier.offset(esc.d(w.x * (cw + g)), esc.d(w.y * (ch + g)))
                    .size(esc.d(an * cw + (an - 1) * g), esc.d(al * ch + (al - 1) * g))
                    .graphicsLayer { translationX = arrastre.x; translationY = arrastre.y }
                    .zIndex(if (elegido) 2f else 1f)
                    .clip(formaWidget).background(Color(COLORES_MAPA[orden % COLORES_MAPA.size]))
                    .then(if (elegido) Modifier.border(esc.d(4), Color.White, formaWidget) else Modifier)
                    .clickable(onClickLabel = Widgets.nombre(w.tipo)) {
                        if (e.extra.espacioEdita < 0) Diptico.editarEspacio(0)
                        if (Diptico.estado.value.extra.espacioEdita != 0) Diptico.elegirPestanaEspacio(0)
                        Diptico.elegirWidget(i)
                    }
                    .then(if (mover) Modifier.pointerInput(i, w.x, w.y, cw, ch) {
                        val celdaX = (cw + g) * esc.factor * density
                        val celdaY = (ch + g) * esc.factor * density
                        detectDragGestures(
                            onDragStart = { Diptico.elegirWidget(i) },
                            onDragCancel = { arrastre = androidx.compose.ui.geometry.Offset.Zero },
                            onDragEnd = {
                                val dx = kotlin.math.round(arrastre.x / celdaX).toInt(); val dy = kotlin.math.round(arrastre.y / celdaY).toInt()
                                arrastre = androidx.compose.ui.geometry.Offset.Zero
                                if (dx != 0 || dy != 0) Diptico.ponerWidget(i, w.x + dx, w.y + dy, w.pagina)
                            },
                        ) { cambio, d -> cambio.consume(); arrastre += d }
                    } else Modifier)
                    .padding(esc.d(12)),
                contentAlignment = Alignment.BottomStart,
            ) { TB(Widgets.nombre(w.tipo), 20f, peso = FontWeight.ExtraBold, color = Color(0xFF0B0C16), lineas = 2) }
        }
    }
}

/** Modo edición: las cuatro pestañas y, debajo, el mapa al lado de los controles de la pestaña. */
@Composable
private fun EdicionEspacio(e: Estado, modifier: Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val tab = e.extra.espacioEdita
    Column(modifier.testTag("espacio-edicion"), verticalArrangement = Arrangement.spacedBy(esc.d(18))) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
            PESTANAS_ESPACIO.forEachIndexed { i, t ->
                val on = i == tab
                Box(
                    Modifier.weight(1f).height(esc.d(74)).clip(RoundedCornerShape(esc.d(22)))
                        .background(if (on) col.elegido else col.texto.copy(alpha = .06f))
                        .border(esc.d(2), if (on) col.elegido else col.texto.copy(alpha = .16f), RoundedCornerShape(esc.d(22)))
                        .clickable(onClickLabel = t) { Diptico.elegirPestanaEspacio(i) }.testTag("pestana-espacio-$i"),
                    contentAlignment = Alignment.Center,
                ) { TB(t, 26f, peso = FontWeight.ExtraBold, color = if (on) col.enElegido else col.texto) }
            }
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(26))) {
            TarjetaMapa(e, tab == 0, Modifier.weight(1.3f).fillMaxHeight())
            val forma = RoundedCornerShape(esc.d(30))
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(forma).background(col.texto.copy(alpha = .05f)).border(esc.d(2), col.texto.copy(alpha = .10f), forma)
                    .verticalScroll(rememberScrollState()).padding(esc.d(28)),
                verticalArrangement = Arrangement.spacedBy(esc.d(20)),
            ) {
                when (tab) {
                    0 -> ControlesMover(e)
                    1 -> ControlesTamano(e)
                    2 -> ControlesPaginas(e)
                    else -> ControlesFondo(e)
                }
            }
        }
    }
}

/** Un botón redondo del panel de controles (claro = el principal). */
@Composable
private fun BotonEsp(t: String, modifier: Modifier = Modifier, claro: Boolean = false, activo: Boolean = true, traducir: Boolean = true, accion: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val forma = RoundedCornerShape(50)
    Box(
        modifier.height(esc.d(64)).widthIn(min = esc.d(64)).clip(forma)
            .background(if (claro) col.elegido else col.texto.copy(alpha = .09f))
            .border(esc.d(2), if (claro) col.elegido else col.texto.copy(alpha = .22f), forma)
            .clickable(enabled = activo, onClickLabel = t, onClick = accion).padding(horizontal = esc.d(22)),
        contentAlignment = Alignment.Center,
    ) { TB(t, 24f, peso = FontWeight.ExtraBold, color = (if (claro) col.enElegido else col.texto).copy(alpha = if (activo) 1f else .35f), traducir = traducir) }
}

/** Una fila «texto … − +». */
@Composable
private fun FilaMasMenos(t: String, menos: Boolean, mas: Boolean, alMenos: () -> Unit, alMas: () -> Unit) {
    val esc = LocalEscala.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
        TB(t, 24f, modifier = Modifier.weight(1f), lineas = 2)
        BotonEsp("−", activo = menos, traducir = false, accion = alMenos)
        BotonEsp("+", activo = mas, traducir = false, accion = alMas)
    }
}

@Composable
private fun ControlesMover(e: Estado) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val w = e.widgets.getOrNull(e.widgetSel)?.takeIf { it.visible && it.pagina == e.paginaInicio }
    if (w == null) {
        TB("No hay widgets aquí", 24f)
        TB("Vuelve y abre la Galería para poner el primero.", 21f, color = col.sub, lineas = 3)
        return
    }
    Row { TB("Moviendo: ", 24f); TB(Widgets.nombre(w.tipo), 24f, peso = FontWeight.ExtraBold) }
    TB("Cruceta o arrastra con el dedo. L1 / R1 eligen otro widget.", 21f, color = col.sub, lineas = 3)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        @Composable fun Celda(t: String, claro: Boolean = false, a: () -> Unit) = BotonEsp(t, Modifier.width(esc.d(72)), claro = claro, traducir = false, accion = a)
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) { Spacer(Modifier.width(esc.d(72))); Celda("▲") { Diptico.moverWidgetSel(0, -1) } }
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
            Celda("◀") { Diptico.moverWidgetSel(-1, 0) }; Celda("⇄", claro = true) { Diptico.otroWidgetEspacio(1) }; Celda("▶") { Diptico.moverWidgetSel(1, 0) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) { Spacer(Modifier.width(esc.d(72))); Celda("▼") { Diptico.moverWidgetSel(0, 1) } }
    }
    FilaMasMenos("Tamaño del widget · ${w.an} × ${w.al}", w.an > 1 || w.al > 1, w.x + w.an < Widgets.COLUMNAS || w.y + w.al < Widgets.FILAS,
        { Diptico.crecerWidgetEspacio(-1) }, { Diptico.crecerWidgetEspacio(1) })
}

@Composable
private fun ControlesTamano(e: Estado) {
    val col = LocalColeccion.current
    val an = e.extra.paginaAncho; val al = e.extra.paginaAlto
    TB("Tamaño de la página", 24f)
    TB("Para una gran corrida de espacio: agrándala y desliza para recorrerla.", 21f, color = col.sub, lineas = 3)
    FilaMasMenos("Ancho: $an pantalla${if (an > 1) "s" else ""}", an > 1, an < 3, { Diptico.cambiarTamPagina(-1, 0) }, { Diptico.cambiarTamPagina(1, 0) })
    FilaMasMenos("Alto: $al pantalla${if (al > 1) "s" else ""}", al > 1, al < 3, { Diptico.cambiarTamPagina(0, -1) }, { Diptico.cambiarTamPagina(0, 1) })
}

@Composable
private fun ControlesPaginas(e: Estado) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val vacia = e.widgets.none { it.visible && it.pagina == e.paginaInicio }
    TB(if (e.paginas == 1) "1 página" else "${e.paginas} páginas", 24f)
    TB("La de Inicio siempre va primero.", 21f, color = col.sub, lineas = 2)
    BotonEsp("+ Página nueva", Modifier.fillMaxWidth(), claro = true, activo = e.paginas < 12) { Diptico.agregarPaginaEspacio() }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
        BotonEsp("◀ Antes", Modifier.weight(1f), activo = e.paginaInicio > 0) { Diptico.moverPaginaEspacio(-1) }
        BotonEsp("Después ▶", Modifier.weight(1f), activo = e.paginaInicio < e.paginas - 1) { Diptico.moverPaginaEspacio(1) }
    }
    BotonEsp("Quitar esta página", Modifier.fillMaxWidth(), activo = e.paginas > 1 && vacia) { Diptico.quitarPaginaEspacio() }
    if (e.paginas > 1 && !vacia) TB("Para quitarla, primero saca sus widgets.", 18f, color = col.sub, lineas = 2)
}

@Composable
private fun ControlesFondo(e: Estado) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    TB("Fondo de Tu espacio", 24f)
    // «Como viene» + los fondos listos, en filas de tres.
    val opciones = listOf(-1) + FONDOS_ESPACIO.indices.toList()
    opciones.chunked(3).forEach { fila ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
            fila.forEach { i ->
                val on = i == e.extra.fondoEspacio && (i >= 0 || e.fondoInicio == null)
                val forma = RoundedCornerShape(esc.d(18))
                Box(
                    Modifier.weight(1f).height(esc.d(96)).clip(forma)
                        .then(FONDOS_ESPACIO.getOrNull(i)?.let { c -> Modifier.background(Brush.linearGradient(c.map { Color(it) })) }
                            ?: Modifier.background(col.texto.copy(alpha = .08f)))
                        .border(esc.d(3), if (on) Color.White else Color.Transparent, forma)
                        .clickable(onClickLabel = if (i < 0) "Como viene" else "Fondo ${i + 1}") { Diptico.elegirFondoEspacio(i) },
                    contentAlignment = Alignment.Center,
                ) { if (i < 0) TB("Como viene", 18f, peso = FontWeight.ExtraBold) }
            }
            // La última fila no se estira: deja los huecos que faltan.
            repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    BotonEsp(if (e.fondoInicio != null) "🖼 Cambiar tu imagen" else "🖼 Una imagen tuya", Modifier.fillMaxWidth()) { Diptico.pedirArchivoPara("fondoInicio", "image/*") }
    if (e.fondoInicio != null) BotonEsp("Quitar tu imagen", Modifier.fillMaxWidth()) { Diptico.quitarImagenEspacio() }
}

/** Lo que DIPPO dice abajo según lo que estás haciendo. */
private fun ideaEspacio(e: Estado): String {
    val vacia = e.widgets.none { it.visible && it.pagina == e.paginaInicio }
    return when (e.extra.espacioEdita) {
        0 -> if (vacia) "Esta página no tiene widgets. Pon uno desde la Galería." else "Mueve, agranda o cambia el fondo. B cuando termines."
        1 -> "Agrándala y después la recorres deslizando."
        2 -> "Puedes tener hasta 12 páginas."
        3 -> "Un fondo oscuro hace que tus widgets brillen."
        else -> when {
            e.espacioVacio() -> "¿Armamos tu espacio? Abre la Galería y elige un widget."
            vacia -> "Esta página está vacía. ¿Le ponemos un widget?"
            else -> "Toca «Editar espacio» para moverlos o cambiar su tamaño."
        }
    }
}

/** El pie: tus apps a la izquierda y DIPPO con su globito a la derecha. */
@Composable
private fun PieEspacio(e: Estado) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Row(
        Modifier.fillMaxWidth().height(esc.d(168)).padding(bottom = esc.d(18)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(26)),
    ) {
        if (e.extra.dock) Box(
            Modifier.anclaRecorrido("apps").clip(RoundedCornerShape(esc.d(34))).background(col.texto.copy(alpha = .06f))
                .border(esc.d(2), col.texto.copy(alpha = .11f), RoundedCornerShape(esc.d(34))).padding(horizontal = esc.d(18), vertical = esc.d(12)),
        ) { FilaDock(e, 92f) }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.widthIn(max = esc.d(380)).clip(RoundedCornerShape(esc.d(26), esc.d(26), esc.d(6), esc.d(26))).background(col.elegido)
                .padding(horizontal = esc.d(22), vertical = esc.d(16)),
        ) { TB(ideaEspacio(e), 23f, peso = FontWeight.ExtraBold, color = col.enElegido, lineas = 3) }
        cl.nikalbazu.diptico.bisa.AvatarBisa(116f * esc.factor)
    }
}

/** Galería de widgets abajo (boceto 12b): su propia pantalla, con categorías y los widgets en grande. */
@Composable
fun GaleriaAbajoCV(e: Estado) {
    LienzoBoceto(1240f, 1080f, Modifier.testTag("galeria-abajo"), fondo = LocalColeccion.current.fondo, zoom = ZOOM_ABAJO) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(40), vertical = esc.d(28)), verticalArrangement = Arrangement.spacedBy(esc.d(16))) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                BotonRedondo(Icono.CERRAR, 64f, { Diptico.cerrarGaleriaWidgets() }, descripcion = "Volver a Tu espacio")
                TB("Galería de widgets", 44f, titulo = true, modifier = Modifier.weight(1f))
                TB("Va a: Pág ${e.paginaInicio + 1}", 20f, color = col.sub, peso = FontWeight.ExtraBold)
            }
            ListaGaleria(e, Modifier.fillMaxWidth().weight(1f), columnas = if (e.unaPantalla) 1 else 2)
            PistasFinas("L1 R1" to "Categoría", "X" to "Tamaño", "A" to "Agregar", "B" to "Volver a Tu espacio")
        }
    }
}

/** Con una pantalla: la Galería entra por el costado de Tu espacio. */
@Composable
internal fun TiendaLadoCV(e: Estado, modifier: Modifier) {
    LienzoBoceto(760f, 1080f, modifier.testTag("tienda-lado"), zoom = ZOOM_UNA) {
        val esc = LocalEscala.current
        Column(Modifier.fillMaxSize().padding(esc.d(18)).vidrio(40f, fuerte = true).padding(esc.d(24)), verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                BotonRedondo(Icono.CERRAR, 56f, { Diptico.cerrarGaleriaWidgets() }, descripcion = "Cerrar la Galería")
                TB("Galería de widgets", 36f, titulo = true)
            }
            ListaGaleria(e, Modifier.fillMaxWidth().weight(1f), columnas = 1)
            TamanosGaleria(e)
        }
    }
}

/** Los tamaños para el widget elegido (X los pasa). */
@Composable
private fun TamanosGaleria(e: Estado) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val tipo = tiposTiendaEspacio(e.extra.tiendaCat).getOrNull(e.extra.espacioSel) ?: return
    val tams = tamanosGaleria(e, tipo)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
        TB("Tamaño", 20f, color = col.sub, peso = FontWeight.ExtraBold)
        tams.forEachIndexed { i, (an, al) ->
            val on = i == e.extra.galeriaTam.coerceIn(0, tams.size - 1)
            Box(Modifier.height(esc.d(54)).clip(CircleShape).background(if (on) col.elegido else col.texto.copy(alpha = .08f))
                .clickable { Diptico.elegirTamGaleria(i) }.padding(horizontal = esc.d(20)), contentAlignment = Alignment.Center) {
                TB("$an × $al", 20f, color = if (on) col.enElegido else col.texto, peso = FontWeight.ExtraBold, traducir = false)
            }
        }
    }
}

/**
 * Galería arriba (boceto 12a), con dos pantallas: el widget elegido en grande (de verdad, con tus datos) en el tamaño
 * que elegiste, y un mapa chico de la página que muestra dónde va a quedar.
 */
@Composable
fun GaleriaArribaCV(e: Estado, modifier: Modifier = Modifier) {
    val escApp = LocalEscala.current
    val tipos = tiposTiendaEspacio(e.extra.tiendaCat)
    val tipo = tipos.getOrNull(e.extra.espacioSel) ?: return
    val tams = tamanosGaleria(e, tipo)
    val (an, al) = tams.getOrNull(e.extra.galeriaTam.coerceIn(0, (tams.size - 1).coerceAtLeast(0))) ?: (2 to 2)
    val w = e.widgets.firstOrNull { it.tipo == tipo }
    LienzoBoceto(1920f, 900f, modifier.testTag("galeria-arriba"), zoom = ZOOM_ARRIBA) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        Row(Modifier.fillMaxSize().padding(horizontal = esc.d(80), vertical = esc.d(30)), horizontalArrangement = Arrangement.spacedBy(esc.d(60))) {
            Column(Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(18))) {
                TB("GALERÍA DE WIDGETS · ${CATEGORIAS_WIDGETS.getOrElse(e.extra.tiendaCat) { "" }.uppercase()}", 18f, color = col.sub, peso = FontWeight.ExtraBold, espaciado = 3f)
                TB(Widgets.nombre(tipo), 66f, titulo = true)
                TB(Widgets.descripcion(tipo), 23f, color = col.sub, lineas = 2)
                // El widget de verdad, en el tamaño elegido.
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    val unidad = 150f
                    Box(
                        Modifier.size(esc.d(unidad * an.coerceAtMost(4)), esc.d(unidad * al.coerceAtMost(3)))
                            .clip(RoundedCornerShape(esc.d(34))).vidrio(34f, fuerte = true),
                    ) {
                        if (w != null) androidx.compose.runtime.CompositionLocalProvider(LocalEscala provides escApp) {
                            ContenidoWidget(e, w.copy(an = an, al = al), an, al)
                        }
                    }
                }
                TamanosGaleria(e)
            }
            Column(Modifier.weight(.9f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(18))) {
                Column(Modifier.fillMaxWidth().weight(1f).vidrio(40f).padding(esc.d(28)), verticalArrangement = Arrangement.spacedBy(esc.d(16))) {
                    TB(if (w?.visible == true) "Ya está en la Pág ${w.pagina + 1}" else "Va a la Pág ${e.paginaInicio + 1}", 32f, titulo = true)
                    MapaPagina(e, tipo, an, al, Modifier.fillMaxWidth().weight(1f))
                    if (w?.visible != true && !cabeEnPagina(e, tipo, an, al)) TB("En esta página no cabe de ese tamaño: va a otra página.", 18f, color = Color(0xFFFFD29A), peso = FontWeight.ExtraBold, lineas = 2)
                    TB("Después lo mueves o estiras desde Editar.", 18f, color = col.sub)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    BotonTecla("A", if (w?.visible == true) "Ir a su página" else "Agregar a mi espacio", { Diptico.agregarDesdeGaleria() }, alto = 80f, principal = true, letra = 26f)
                }
            }
        }
    }
}

/** ¿Cabe el widget de [an]×[al] en algún hueco de la página que estás mirando? */
private fun cabeEnPagina(e: Estado, tipo: String, an: Int, al: Int): Boolean {
    val cols = Widgets.COLUMNAS.coerceAtLeast(1); val filas = Widgets.FILAS.coerceAtLeast(1)
    val enPagina = e.widgets.filter { it.visible && it.pagina == e.paginaInicio && it.tipo != tipo }
    for (y in 0..filas - al) for (x in 0..cols - an) {
        if (enPagina.none { o -> x < o.x + o.an && o.x < x + an && y < o.y + o.al && o.y < y + al }) return true
    }
    return false
}

/** El mapa de la página: los widgets que ya están y, punteado, dónde quedaría el nuevo. */
@Composable
private fun MapaPagina(e: Estado, tipo: String, an: Int, al: Int, modifier: Modifier) {
    val col = LocalColeccion.current
    val cols = Widgets.COLUMNAS.coerceAtLeast(1); val filas = Widgets.FILAS.coerceAtLeast(1)
    val enPagina = e.widgets.filter { it.visible && it.pagina == e.paginaInicio && it.tipo != tipo }
    // El primer hueco donde cabe (igual que al agregarlo).
    val hueco = run {
        for (y in 0..filas - al) for (x in 0..cols - an) {
            val libre = enPagina.none { o -> x < o.x + o.an && o.x < x + an && y < o.y + o.al && o.y < y + al }
            if (libre) return@run x to y
        }
        null
    }
    androidx.compose.foundation.Canvas(modifier) {
        val g = 8f
        val cw = (size.width - g * (cols - 1)) / cols; val ch = (size.height - g * (filas - 1)) / filas
        fun rect(x: Int, y: Int, w: Int, h: Int) = androidx.compose.ui.geometry.Offset(x * (cw + g), y * (ch + g)) to
            androidx.compose.ui.geometry.Size(w * cw + (w - 1) * g, h * ch + (h - 1) * g)
        val r = androidx.compose.ui.geometry.CornerRadius(14f)
        for (o in enPagina) { val (p, s) = rect(o.x, o.y, o.an.coerceAtMost(cols - o.x), o.al.coerceAtMost(filas - o.y)); drawRoundRect(col.texto.copy(alpha = .16f), p, s, r) }
        if (hueco != null) {
            val (p, s) = rect(hueco.first, hueco.second, an, al)
            drawRoundRect(col.acento.copy(alpha = .35f), p, s, r)
            drawRoundRect(col.texto, p, s, r, style = androidx.compose.ui.graphics.drawscope.Stroke(4f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
        }
    }
}

/** La lista de la Galería: categorías (L1/R1) y widgets (cruceta, A agrega). Sin entrar al editor. */
@Composable
private fun ListaGaleria(e: Estado, modifier: Modifier, columnas: Int) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val tipos = tiposTiendaEspacio(e.extra.tiendaCat)
    val sel = e.extra.espacioSel.coerceIn(0, (tipos.size - 1).coerceAtLeast(0))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(esc.d(16))) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
            CATEGORIAS_WIDGETS.forEachIndexed { i, n ->
                val on = i == e.extra.tiendaCat
                Box(Modifier.height(esc.d(54)).clip(CircleShape).background(if (on) col.elegido else col.texto.copy(alpha = .06f))
                    .border(1.dp, col.borde, CircleShape).clickable { Diptico.elegirCategoriaEspacio(i) }.padding(horizontal = esc.d(22)),
                    contentAlignment = Alignment.Center) { TB(n, 20f, color = if (on) col.enElegido else col.texto, peso = FontWeight.ExtraBold) }
            }
        }
        val grilla = rememberLazyGridState()
        LaunchedEffect(sel, e.extra.tiendaCat) { if (tipos.isNotEmpty()) grilla.animateScrollToItem((sel - columnas).coerceAtLeast(0)) }
        LazyVerticalGrid(GridCells.Fixed(columnas), Modifier.fillMaxWidth().weight(1f), state = grilla,
            verticalArrangement = Arrangement.spacedBy(esc.d(14)), horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
            items(tipos.size) { k ->
                val tipo = tipos[k]
                val on = k == sel
                val puesto = e.widgets.any { it.tipo == tipo && it.visible }
                val w = e.widgets.firstOrNull { it.tipo == tipo }
                Row(
                    Modifier.height(esc.d(170)).clip(RoundedCornerShape(esc.d(28)))
                        .background(if (on) col.texto.copy(alpha = .10f) else col.texto.copy(alpha = .04f))
                        .border(if (on) 3.dp else 1.dp, if (on) col.texto else col.borde, RoundedCornerShape(esc.d(28)))
                        .clickable { if (on) Diptico.agregarDesdeGaleria() else Diptico.actualizar { it.copy(extra = it.extra.copy(espacioSel = k, galeriaTam = 0)) } }
                        .padding(esc.d(16)),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16)),
                ) {
                    Box(Modifier.size(esc.d(120)).clip(RoundedCornerShape(esc.d(24))).background(col.acento.copy(alpha = .30f)), contentAlignment = Alignment.Center) {
                        IconoDiptico(Widgets.icono(tipo), col.texto, 56f, acento = Coral)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        TB(Widgets.nombre(tipo), 24f, peso = FontWeight.ExtraBold, lineas = 1)
                        TB(Widgets.descripcion(tipo), 17f, color = col.sub, lineas = 2)
                        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8)), verticalAlignment = Alignment.CenterVertically) {
                            if (w != null) Box(Modifier.clip(CircleShape).background(col.texto.copy(alpha = .08f)).padding(horizontal = esc.d(12), vertical = esc.d(4))) {
                                TB("${w.an} × ${w.al}", 15f, peso = FontWeight.ExtraBold, traducir = false)
                            }
                            if (puesto) TB("En Tu espacio ✓", 15f, color = Color(0xFF7FE0A8), peso = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

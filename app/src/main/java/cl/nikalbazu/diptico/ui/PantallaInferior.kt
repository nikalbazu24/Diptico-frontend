@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.zIndex
import androidx.compose.animation.togetherWith
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Panel
import kotlinx.coroutines.delay

/** Pantalla del estante: consolas, juegos y barra de acciones. Se adapta a pantalla angosta o ancha. */
@Composable
fun PantallaInferior(e: Estado, miDisplay: Int) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(Modifier.fillMaxSize().background(p.fondo2)) {
        val s = e.sesion
        // Con la lupa no se difumina nada encima (la copia de la pantalla de arriba se veía rara con el desenfoque).
        val conLupa = s != null && e.lupaActiva && e.datos[s.clave]?.lupa == true && s.displayId == android.view.Display.DEFAULT_DISPLAY && !s.dosPantallas
        Box(Modifier.fillMaxSize().then(if (conLupa) Modifier else Modifier.difuminar(e))) {
            when {
                s != null && s.displayId != miDisplay && !s.dosPantallas -> EnJuego(e)
                else -> Column(Modifier.fillMaxSize()) {
                    val escalaEstante = if (esc.angosta) 1.08f else 1.18f
                    when (e.vistaConsolas) {
                        "lado", "rueda" -> Row(Modifier.fillMaxWidth().weight(1f)) {
                            if (e.vistaConsolas == "lado") RielConsolas(e) else RuedaConsolas(e)
                            Box(Modifier.padding(vertical = esc.d(14)).width(esc.d(1)).fillMaxHeight().background(p.linea))
                            VistaJuegos(e, Modifier.weight(1f).fillMaxHeight(), escalaEstante * 0.9f)
                        }
                        "sistemas", "carpetas", "discos", "habitacion" -> AnimatedContent(
                            targetState = e.enSistemas,
                            transitionSpec = {
                                (fadeIn(tween(260)) + scaleIn(tween(320), initialScale = if (targetState) 1.06f else 0.94f)) togetherWith fadeOut(tween(160))
                            },
                            label = "sistemas", modifier = Modifier.fillMaxWidth().weight(1f),
                        ) { eligiendo ->
                            if (eligiendo) when (e.vistaConsolas) {
                                "habitacion" -> Habitacion(e, Modifier.fillMaxSize())
                                "carpetas" -> CarpetasConsolas(e, Modifier.fillMaxSize())
                                "discos" -> DiscosConsolas(e, Modifier.fillMaxSize())
                                else -> SistemasCarrusel(e, Modifier.fillMaxSize())
                            }
                            else Column(Modifier.fillMaxSize()) {
                                CabeceraSistema(e)
                                VistaJuegos(e, Modifier.fillMaxWidth().weight(1f), escalaEstante)
                            }
                        }
                        else -> {
                            Cinta(e)
                            Box(Modifier.padding(horizontal = esc.d(16)).fillMaxWidth().height(esc.d(1)).background(p.linea))
                            VistaJuegos(e, Modifier.fillMaxWidth().weight(1f), escalaEstante)
                        }
                    }
                }
            }
        }
        Paneles(e, conAyuda = false)
    }
}

/** Menús, próximo paso, clave y avisos: se dibujan encima de todo. */
@Composable
fun BoxScope.Paneles(e: Estado, conAyuda: Boolean) {
    // Moviendo un juego de lugar: un cartel arriba con lo que hace cada botón.
    androidx.compose.animation.AnimatedVisibility(
        e.ordenando && e.panel == Panel.NINGUNO, Modifier.align(Alignment.TopCenter).padding(top = LocalEscala.current.d(10)).zIndex(5f),
        enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it },
    ) {
        val esc = LocalEscala.current
        Row(
            Modifier.clip(RoundedCornerShape(50)).fondoBoton(Coral).padding(horizontal = esc.d(14), vertical = esc.d(7)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconoDiptico(Icono.VISTA, Color.White, 16f)
            Spacer(Modifier.width(esc.d(8)))
            Texto("Moviendo ${e.seleccionado?.titulo ?: ""}", 13.5f, color = Color.White, lineas = 1, modifier = Modifier.widthIn(max = esc.d(220)))
            Spacer(Modifier.width(esc.d(10)))
            Texto("✥ correr · A listo", 12f, color = Color.White.copy(alpha = 0.85f), lineas = 1)
        }
    }
    // Cada panel entra y sale con su animación: los de pantalla completa suben, los flotantes crecen.
    AnimatedContent(
        targetState = e.panel,
        transitionSpec = {
            val completo = targetState in setOf(Panel.MENU, Panel.ENCUADRE, Panel.APPS, Panel.EDITAR_CONSOLA, Panel.FICHA, Panel.PORTS, Panel.BUSCAR, Panel.CARPETAS, Panel.ESTADISTICAS, Panel.IMAGENES, Panel.ASISTENTE, Panel.GALERIA)
            val entra = if (completo) {
                fadeIn(tween(260)) + slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it / 8 }
            } else {
                fadeIn(tween(200)) + scaleIn(tween(260, easing = FastOutSlowInEasing), initialScale = 0.92f)
            }
            (entra togetherWith fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 0.97f)).using(SizeTransform(clip = false))
        },
        label = "panel",
        modifier = Modifier.fillMaxSize(),
    ) { panel ->
        when (panel) {
            // Con dos pantallas, el menú va arriba (la pantalla grande) y aquí la ayuda con su vista previa.
            Panel.MENU -> if (!conAyuda && !e.intercambiar) AyudaMenu(e) else PanelMenu(e, conAyuda)
            Panel.RAPIDO -> VentanaJuego(e)
            Panel.BUSCAR -> PanelBuscar(e)
            Panel.CENTRO -> Box(Modifier.fillMaxSize())
            Panel.CARPETAS -> PanelCarpetas(e)
            Panel.PASO -> PanelPaso(e)
            Panel.CLAVE -> PanelClave(e)
            Panel.PORTS -> PanelPorts(e)
            Panel.FICHA -> PanelFicha(e)
            Panel.ESTADISTICAS -> PanelEstadisticas(e)
            Panel.IMAGENES -> PanelImagenes(e)
            Panel.ENCUADRE -> PanelEncuadre(e)
            // Con dos pantallas, el widget abierto se muestra arriba (donde están los widgets).
            Panel.WIDGET -> if (!conAyuda && !e.intercambiar) Box(Modifier.fillMaxSize()) else PanelWidget(e)
            Panel.GALERIA -> PanelGaleria(e)
            Panel.ESTUDIO -> PanelEstudio(e)
            Panel.EDITAR_CONSOLA -> PanelEditarConsola(e)
            Panel.APPS -> PanelApps(e)
            Panel.ASISTENTE -> if (e.sesion == null) PanelAsistente(e) else Box(Modifier.fillMaxSize())
            Panel.TRABAJOS, Panel.NINGUNO -> Box(Modifier.fillMaxSize())
        }
    }
    Aviso(e.aviso, Modifier.align(Alignment.BottomCenter), abajo = if (e.panel == Panel.NINGUNO) 70f else 16f)
}

/** Con una ventana encima (opciones del juego, textos), lo de atrás se difumina. */
fun Modifier.difuminar(e: Estado): Modifier =
    if (Estilo.efectosCompletos && e.panel in setOf(Panel.RAPIDO, Panel.PASO, Panel.CLAVE, Panel.TRABAJOS, Panel.ENCUADRE)) this.blur(18.dp) else this

/** Los juegos según la vista elegida: estante (con la info del juego), cuadrícula o lista. */
@Composable
fun VistaJuegos(e: Estado, modifier: Modifier, escalaEstante: Float) {
    val esc = LocalEscala.current
    val t = LocalTamJuegos.current
    AnimatedContent(
        targetState = e.vista,
        transitionSpec = { (fadeIn(tween(260)) + scaleIn(tween(300), initialScale = 0.96f)) togetherWith fadeOut(tween(160)) },
        label = "vista",
        modifier = modifier.pellizco(),
    ) { vista ->
        when (vista) {
            "cuadricula" -> Cuadricula(e, Modifier.fillMaxSize())
            "lista" -> ListaJuegos(e, Modifier.fillMaxSize())
            "carrusel" -> CarruselJuegos(e, Modifier.fillMaxSize())
            "abanico" -> AbanicoJuegos(e, Modifier.fillMaxSize())
            "lomos" -> VistaLomos(e, Modifier.fillMaxSize())
            // El estante y su información quedan centrados en el espacio disponible.
            else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Estante(e, alto = (if (esc.angosta) 192f else 214f) * t.coerceAtMost(1.25f), escala = escalaEstante * t)
                Info(e)
                Spacer(Modifier.height(esc.d(8)))
            }
        }
    }
}

/** Dos dedos sobre los juegos: abrir agranda, cerrar achica (como en Cocoon o iiSU). Un dedo sigue desplazando. */
fun Modifier.pellizco(): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var acumulado = 1f
        while (true) {
            val ev = awaitPointerEvent(PointerEventPass.Initial)
            val dedos = ev.changes.count { it.pressed }
            if (dedos == 0) break
            if (dedos >= 2) {
                acumulado *= ev.calculateZoom()
                ev.changes.forEach { it.consume() }
                if (acumulado > 1.3f) { Diptico.pellizcar(true); acumulado = 1f }
                else if (acumulado < 0.77f) { Diptico.pellizcar(false); acumulado = 1f }
            }
        }
    }
}

/** Las consolas, como pestañas con el ícono de su formato. */
@Composable
fun Cinta(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val estado = rememberLazyListState()
    val sel = e.pestanas.indexOf(e.consolaSel).coerceAtLeast(0)
    LaunchedEffect(sel) { estado.animateScrollToItem((sel - 2).coerceAtLeast(0)) }
    LazyRow(
        state = estado,
        modifier = Modifier.fillMaxWidth().padding(top = esc.d(8)),
        contentPadding = PaddingValues(horizontal = esc.d(10)),
        horizontalArrangement = Arrangement.spacedBy(esc.d(2)),
    ) {
        itemsIndexed(e.pestanas, key = { _, id -> id }) { _, id ->
            val c = Consolas.porId(id)
            val activa = id == e.consolaSel
            val escala by animateFloatAsState(if (activa) 1.14f else 1f, spring(dampingRatio = 0.5f), label = "icono")
            Column(
                // Mantener apretada una consola abre su personalización.
                Modifier.width(esc.d(64)).combinedClickable(onLongClick = { Diptico.abrirEditarConsola(id) }) { Diptico.elegirConsola(id) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.height(esc.d(36)), contentAlignment = Alignment.Center) {
                    IconoConsola(c, 32f, p.fondo2, Modifier.graphicsLayer { scaleX = escala; scaleY = escala })
                }
                Texto(
                    c?.nombre ?: "Todo", 12f, color = if (activa) p.tinta else p.tenue,
                    familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1,
                )
                Spacer(Modifier.height(esc.d(4)))
                val ancho by animateDpAsState(if (activa) esc.d(22) else esc.d(0), label = "barra")
                Box(Modifier.width(ancho).height(esc.d(3)).clip(RoundedCornerShape(50)).background(Coral))
            }
        }
    }
}

/** El estante: cada juego con la forma real de su caja. */
@Composable
fun Estante(e: Estado, alto: Float = 184f, escala: Float = 1f) {
    // Al cambiar de consola, el estante se desliza hacia el lado que corresponde.
    AnimatedContent(
        targetState = e.consolaSel,
        transitionSpec = {
            val ida = e.pestanas.indexOf(targetState) >= e.pestanas.indexOf(initialState)
            val d = if (ida) 1 else -1
            (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { d * it / 3 } + fadeIn(tween(300)))
                .togetherWith(slideOutHorizontally(tween(320)) { -d * it / 3 } + fadeOut(tween(200)))
        },
        label = "estante",
    ) { consola ->
        EstanteDe(e, consola, alto, escala)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun EstanteDe(e: Estado, consola: String, alto: Float, escala: Float) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val lista = if (consola == e.consolaSel) e.visibles else if (consola == "todo") e.juegos else e.juegos.filter { it.consolaId == consola }
    val estado = rememberLazyListState()
    val sel = lista.indexOfFirst { it.clave == e.seleccionado?.clave }
    LaunchedEffect(sel, lista.size) {
        if (lista.isNotEmpty() && sel >= 0) estado.animateScrollToItem((sel - 1).coerceAtLeast(0))
    }
    Box(Modifier.fillMaxWidth().height(esc.d(alto))) {
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(30)).fillMaxWidth().height(esc.d(3)).background(p.estante),
        )
        if (lista.isEmpty()) {
            Detalle(
                if (e.escaneando) "Buscando tus juegos…" else "Aquí aparecerán tus juegos",
                15f, modifier = Modifier.align(Alignment.Center),
            )
        }
        LazyRow(
            state = estado,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(esc.d(14)),
            verticalAlignment = Alignment.Bottom,
            contentPadding = PaddingValues(horizontal = esc.d(18)),
        ) {
            itemsIndexed(lista, key = { _, j -> j.clave }) { i, j ->
                val c = Consolas.porId(j.consolaId) ?: return@itemsIndexed
                val elegido = i == sel
                val alza by animateFloatAsState(if (elegido) -12f else 0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow), label = "alza")
                val tam by animateFloatAsState(if (elegido) 1.06f else 1f, spring(dampingRatio = 0.5f), label = "tam")
                val u = with(LocalDensity.current) { esc.d(1f).toPx() }
                val color = colorDe(j.clave)
                val caja = cajaDe(c, escala)
                val disco = LocalFormas.current[c.id]?.forma == "disco"
                val forma = if (disco) CircleShape else RoundedCornerShape(esc.d(caja.radio))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        // Mantener presionado abre la ventana del juego (personalizar, ficha, favoritos…).
                        onLongClick = { Diptico.elegirJuego(j.clave); Diptico.elegirPantalla("juego") },
                    ) { if (elegido) Diptico.jugar() else Diptico.elegirJuego(j.clave) },
                ) {
                    Box(
                        Modifier
                            .graphicsLayer { translationY = alza * u; scaleX = tam; scaleY = tam }
                            .then(
                                if (elegido) Modifier
                                    .shadow(esc.d(16), forma, ambientColor = color, spotColor = color)
                                    .border(esc.d(3), if (p == PaletaOscura) Color.White else Uva, forma)
                                else Modifier,
                            ),
                    ) { CajaJuego(j, c, escala = escala, vivo = elegido) }
                    Box(
                        Modifier.padding(top = esc.d(7)).width(esc.d(caja.ancho)).height(esc.d(22)).clip(RoundedCornerShape(esc.d(caja.radio.coerceAtMost(12f))))
                            .background(Brush.verticalGradient(listOf(color.copy(alpha = if (elegido) 0.33f else 0.16f), Color.Transparent))),
                    )
                }
            }
        }
    }
}

@Composable
private fun Info(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val j = e.seleccionado ?: return
    val c = Consolas.porId(j.consolaId)
    Row(Modifier.fillMaxWidth().padding(start = esc.d(18), end = esc.d(18), top = esc.d(2)), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Texto(j.titulo, 20f, lineas = 1)
            Detalle("${c?.nombreLargo ?: ""} · ${e.indiceSel + 1} de ${e.visibles.size}", 12.5f, lineas = 1)
        }
        if (!esc.angosta) Pista("L1 R1", "Consola")
    }
}

/**
 * Compañero de partida: mientras juegas en la otra pantalla, aquí queda todo a mano en una sola vista.
 * Arriba la hora, la batería y el tiempo de la sesión; al centro el logo del juego (con movimiento y tamaño a gusto);
 * abajo los botones. A los 5 segundos se atenúa sola (el nivel se elige en Ajustes › Pantallas); tocar la despierta.
 */
@Composable
private fun EnJuego(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val s = e.sesion ?: return
    // Lupa: el juego está arriba y aquí se ve ampliada una parte de esa pantalla.
    if (e.lupaActiva && e.datos[s.clave]?.lupa == true && s.displayId == android.view.Display.DEFAULT_DISPLAY && !s.dosPantallas) {
        cl.nikalbazu.diptico.lupa.PanelLupa(e, s.clave)
        return
    }
    val juego = e.juegos.firstOrNull { it.clave == s.clave }
    val c = juego?.let { Consolas.porId(it.consolaId) }
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(s.inicio) {
        while (true) {
            ahora = System.currentTimeMillis()
            delay(1000)
        }
    }
    val seg = (ahora - s.inicio) / 1000
    val reloj = if (seg >= 3600) "${seg / 3600}:${((seg % 3600) / 60).toString().padStart(2, '0')}:${(seg % 60).toString().padStart(2, '0')}"
    else "${seg / 60}:${(seg % 60).toString().padStart(2, '0')}"
    val color = juego?.let { colorDe(it.clave) } ?: Uva
    val bateria = rememberBateria()
    val cargando = rememberCargando()
    val t = rememberInfiniteTransition(label = "compa")
    val flotar by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flotar")
    val respirar by t.animateFloat(0.97f, 1.03f, infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "respirar")
    val brillo by t.animateFloat(0.18f, 0.42f, infiniteRepeatable(tween(4200), RepeatMode.Reverse), label = "brillo")

    Box(
        Modifier.fillMaxSize().background(Color(0xFF07060C)).pointerInput(Unit) {
            detectTapGestures { Diptico.despertar() }
        },
    ) {
        // Brillo del color del juego que respira detrás del logo.
        Box(
            Modifier.align(Alignment.Center).size(esc.d(420)).graphicsLayer { scaleX = respirar; scaleY = respirar }
                .background(Brush.radialGradient(listOf(color.copy(alpha = brillo), Color.Transparent))),
        )
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(16), vertical = esc.d(12))) {
            // Barra de estado: hora, batería y sesión.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Texto(rememberHora(), 18f, color = Color.White)
                Spacer(Modifier.width(esc.d(12)))
                IconoDiptico(Icono.BATERIA, if (bateria in 0..15 && !cargando) Coral else Color(0xFFB9B5D6), 18f, acento = if (cargando) Sol else Color(0xFFB9B5D6))
                Spacer(Modifier.width(esc.d(4)))
                Texto(if (bateria >= 0) "$bateria%" + if (cargando) " ⚡" else "" else "", 14f, color = Color(0xFFB9B5D6), traducir = false)
                Spacer(Modifier.weight(1f))
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = esc.d(10), vertical = esc.d(4))) {
                    Texto("Sesión · $reloj", 13f, color = Color(0xFFB9B5D6))
                }
                Spacer(Modifier.width(esc.d(8)))
                cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(32).value, Modifier.clickable { Diptico.prepararAyuda() })
            }
            // Logo del juego con movimiento; su tamaño se ajusta con − y + (o pellizcando).
            Box(
                Modifier.weight(1f).fillMaxWidth().pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ -> if (zoom != 1f) Diptico.cambiarTamLogo(zoom) }
                },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        translationY = flotar * 6f * density
                        scaleX = e.tamLogo * respirar
                        scaleY = e.tamLogo * respirar
                    },
                ) {
                    Etiqueta(c?.nombreLargo ?: "", color, 11f)
                    Spacer(Modifier.height(esc.d(8)))
                    val logo = rememberImagen(juego?.logo, 800)
                    if (logo != null) {
                        Image(
                            logo, contentDescription = juego?.titulo, contentScale = ContentScale.Fit,
                            modifier = Modifier.heightIn(max = esc.d(120)).widthIn(max = esc.d(330)),
                        )
                    } else {
                        Texto(juego?.titulo ?: "Jugando", 32f, color = Color.White, lineas = 2, interlineado = 36f, alinear = TextAlign.Center, traducir = false)
                    }
                }
                Row(Modifier.align(Alignment.BottomEnd), horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    BotonRedondo("−") { Diptico.cambiarTamLogo(0.9f) }
                    BotonRedondo("+") { Diptico.cambiarTamLogo(1.1f) }
                }
            }
            // Si suena música en otra app (Spotify, YouTube Music…), se controla desde aquí sin salir del juego.
            MusicaEnJuego()
            // Paso anotado, si hay.
            val paso = e.datos[s.clave]?.paso.orEmpty()
            if (paso.isNotBlank()) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(14))).background(Color.White.copy(alpha = 0.06f)).padding(esc.d(10)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(esc.d(8)).clip(CircleShape).background(Coral))
                    Spacer(Modifier.width(esc.d(8)))
                    Detalle(paso, 13.5f, color = Color(0xFFE6E3FA), lineas = 2)
                }
                Spacer(Modifier.height(esc.d(8)))
            }
            // Botones: todos a la vista.
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                AccionCompa(Icono.LAPIZ, "Mi paso", Modifier.weight(1f)) { Diptico.abrirPaso() }
                AccionCompa(Icono.BISA, "Bisa", Modifier.weight(1f)) { Diptico.prepararAyuda() }
                if (s.displayId == android.view.Display.DEFAULT_DISPLAY) AccionCompa(Icono.BUSCAR, "Lupa", Modifier.weight(1f)) { Diptico.alternarLupaJuego(s.clave, true) }
                AccionCompa(Icono.TUTORIAL, "Guías", Modifier.weight(1f)) { Diptico.abrirFichaDe(s.clave, 2) }
                AccionCompa(Icono.NEGRO, "Atenuar", Modifier.weight(1f)) { Diptico.atenuar() }
                AccionCompa(Icono.CHECK, "Terminé", Modifier.weight(1f), Uva) { Diptico.cerrarSesion(preguntar = true) }
            }
        }
    }
}

/** Controles de la música que suena mientras juegas: portada, canción y anterior / pausa / siguiente. */
@Composable
private fun MusicaEnJuego() {
    val esc = LocalEscala.current
    val m by cl.nikalbazu.diptico.audio.Medios.sonando.collectAsState()
    val musica = m ?: return
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(16))).background(Color.White.copy(alpha = 0.07f))
            .border(esc.d(1), Color.White.copy(alpha = 0.08f), RoundedCornerShape(esc.d(16))).padding(esc.d(8)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(esc.d(40)).clip(RoundedCornerShape(esc.d(10))).background(Uva.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
            val img = musica.portada
            if (img != null) Image(img.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else IconoDiptico(Icono.MUSICA, Color.White, 20f, acento = Coral)
        }
        Spacer(Modifier.width(esc.d(10)))
        Column(Modifier.weight(1f)) {
            Texto(musica.titulo.ifBlank { musica.app }, 14f, color = Color.White, lineas = 1, traducir = false)
            Detalle(listOf(musica.artista, musica.app).filter { it.isNotBlank() }.joinToString(" · "), 11.5f, color = Color(0xFFB9B5D6), lineas = 1)
        }
        val boton = Modifier.size(esc.d(36)).clip(CircleShape)
        Box(boton.background(Color.White.copy(alpha = 0.1f)).clickable { cl.nikalbazu.diptico.audio.Medios.anterior() }, contentAlignment = Alignment.Center) {
            IconoDiptico(Icono.CONTINUAR, Color.White, 16f, modifier = Modifier.graphicsLayer { scaleX = -1f })
        }
        Spacer(Modifier.width(esc.d(6)))
        Box(boton.size(esc.d(42)).fondoBoton(Uva).clickable { cl.nikalbazu.diptico.audio.Medios.alternar() }, contentAlignment = Alignment.Center) {
            if (musica.reproduciendo) Row(horizontalArrangement = Arrangement.spacedBy(esc.d(4))) {
                Box(Modifier.size(esc.d(4), esc.d(14)).clip(RoundedCornerShape(50)).background(SobreUva))
                Box(Modifier.size(esc.d(4), esc.d(14)).clip(RoundedCornerShape(50)).background(SobreUva))
            } else IconoDiptico(Icono.JUGAR, SobreUva, 18f)
        }
        Spacer(Modifier.width(esc.d(6)))
        Box(boton.background(Color.White.copy(alpha = 0.1f)).clickable { cl.nikalbazu.diptico.audio.Medios.siguiente() }, contentAlignment = Alignment.Center) {
            IconoDiptico(Icono.CONTINUAR, Color.White, 16f)
        }
    }
    Spacer(Modifier.height(esc.d(8)))
}

@Composable
private fun BotonRedondo(texto: String, accion: () -> Unit) {
    val esc = LocalEscala.current
    Box(
        Modifier.size(esc.d(32)).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { accion() },
        contentAlignment = Alignment.Center,
    ) { Texto(texto, 16f, color = Color.White, traducir = false) }
}

@Composable
private fun AccionCompa(icono: Icono, texto: String, modifier: Modifier, fondo: Color = Color.White.copy(alpha = 0.08f), accion: () -> Unit) {
    val esc = LocalEscala.current
    Column(
        modifier.height(esc.d(62)).clip(RoundedCornerShape(esc.d(16))).background(fondo).clickable { accion() }.padding(esc.d(4)),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        IconoDiptico(icono, Color.White, 20f, acento = Coral)
        Spacer(Modifier.height(esc.d(4)))
        Texto(texto, 11.5f, color = Color.White, lineas = 1)
    }
}

/** Ancho de los paneles flotantes: cabe en la pantalla, con margen. */
@Composable
private fun anchoPanel(max: Float): Float = minOf(max, LocalEscala.current.ancho - 28f)

/** Menú rápido de un juego (mantener A). */
@Composable
private fun PanelRapido(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val opciones = Diptico.opcionesRapido()
    val lista = rememberLazyListState()
    LaunchedEffect(e.indice) { if (opciones.isNotEmpty()) lista.mostrar(e.indice) }
    Velo {
        Column(
            Modifier.width(esc.d(anchoPanel(440f))).clip(RoundedCornerShape(esc.d(24))).background(p.fondo2).sinCerrar()
                .padding(esc.d(12)),
        ) {
            Texto(e.seleccionado?.titulo ?: "", 19f, lineas = 1, modifier = Modifier.padding(start = esc.d(6), top = esc.d(4), bottom = esc.d(10)))
            LazyColumn(state = lista, modifier = Modifier.heightIn(max = esc.d(270)), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                itemsIndexed(opciones) { i, o -> TarjetaOpcion(o, i == e.indice, alto = 54f) { Diptico.elegirIndice(i) } }
            }
            Spacer(Modifier.height(esc.d(8)))
            Pistas("A" to "Elegir", "B" to "Cerrar", modifier = Modifier.padding(start = esc.d(6)))
        }
    }
}

/** Un cuadro de texto grande, para escribir con el teclado de la pantalla. */
@Composable
private fun CampoTexto(valor: String, ejemplo: String, alListo: () -> Unit, numerico: Boolean = false, alto: Float = 60f) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val est = Diptico.estado.collectAsState().value
    if (est.conTeclado) {
        // Se escribe con el teclado de Díptico, en la otra pantalla.
        Column(verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
            TextoEscrito(
                valor, ejemplo, 17f, alto - 24f,
                Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(16))).background(p.fondo)
                    .border(esc.d(2), Uva.copy(alpha = 0.6f), RoundedCornerShape(esc.d(16))).padding(esc.d(12)),
                cursor = est.cursor, seleccion = Diptico.seleccion(est), oculto = numerico,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.INTERCAMBIAR, Coral, 15f)
                Spacer(Modifier.width(esc.d(6)))
                Detalle("El teclado está en la otra pantalla", 12.5f, lineas = 1)
            }
        }
        return
    }
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        try { foco.requestFocus() } catch (_: Exception) {}
    }
    BasicTextField(
        value = valor,
        onValueChange = { Diptico.cambiarBorrador(it) },
        textStyle = TextStyle(color = p.tinta, fontSize = esc.s(17), fontFamily = Fredoka, fontWeight = FontWeight.Medium),
        cursorBrush = SolidColor(Uva),
        singleLine = numerico,
        keyboardOptions = KeyboardOptions(
            capitalization = if (numerico) KeyboardCapitalization.None else KeyboardCapitalization.Sentences,
            keyboardType = if (numerico) KeyboardType.Password else KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { alListo() }),
        modifier = Modifier.fillMaxWidth().focusRequester(foco)
            .clip(RoundedCornerShape(esc.d(16))).background(p.fondo)
            .border(esc.d(2), p.linea, RoundedCornerShape(esc.d(16)))
            .padding(esc.d(12)).heightIn(min = esc.d(alto - 24f)),
        decorationBox = { campo ->
            Box {
                if (valor.isEmpty()) Texto(ejemplo, 17f, color = p.tenue, peso = FontWeight.Medium)
                campo()
            }
        },
    )
}

@Composable
private fun PanelPaso(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Velo {
        Column(
            Modifier.width(esc.d(anchoPanel(520f))).clip(RoundedCornerShape(esc.d(24))).background(p.superficie).sinCerrar()
                .padding(esc.d(18)),
            verticalArrangement = Arrangement.spacedBy(esc.d(10)),
        ) {
            Texto(if (e.pregunta) "¿Dónde quedaste?" else "Tu próximo paso", 21f)
            Detalle(e.seleccionado?.titulo ?: "", 13.5f, lineas = 1)
            CampoTexto(e.borrador, "Ej: me falta la llave del jefe del templo", { Diptico.guardarPaso() }, alto = 76f)
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                BotonGrande("B · Cancelar", p.fondo2, p.tinta, Modifier.weight(1f)) { Diptico.cerrarPanel() }
                BotonGrande("Guardar", Uva, Color.White, Modifier.weight(1.4f)) { Diptico.guardarPaso() }
            }
        }
    }
}

/** Marcar qué apps instaladas son ports: aparecen en su propio estante. */
@Composable
private fun PanelPorts(e: Estado) {
    val esAndroid = e.appsDestino == Consolas.ANDROID
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val lista = rememberLazyListState()
    LaunchedEffect(e.indice) { if (e.apps.isNotEmpty()) lista.mostrar(e.indice) }
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Column(Modifier.fillMaxSize().padding(start = esc.d(16), end = esc.d(16), top = esc.d(12), bottom = esc.d(10))) {
            Row(Modifier.fillMaxWidth().height(esc.d(40)), verticalAlignment = Alignment.CenterVertically) {
                IconoConsola(Consolas.porId(e.appsDestino), 28f, p.fondo2)
                Spacer(Modifier.width(esc.d(10)))
                Column(Modifier.weight(1f)) {
                    Texto(if (esAndroid) "Juegos de Android" else "Tus ports", 21f, lineas = 1)
                }
                Detalle("${(if (esAndroid) e.android else e.ports).size} elegidos", 14f, lineas = 1)
            }
            Detalle(
                if (esAndroid) "Arriba están los que Android reconoce como juego. Marca los que quieras en tu estante."
                else "Marca los juegos hechos para Android. Puedes agregar cualquier app.",
                13.5f, lineas = 2,
            )
            Spacer(Modifier.height(esc.d(8)))
            if (e.cargandoApps) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Girador(22f)
                    Spacer(Modifier.width(esc.d(10)))
                    Texto("Buscando tus apps…", 16f)
                }
            }
            LazyColumn(state = lista, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                itemsIndexed(e.apps, key = { _, a -> a.paquete }) { i, a ->
                    val activa = i == e.indice
                    val marcada = a.paquete in (if (e.appsDestino == Consolas.ANDROID) e.android else e.ports)
                    Row(
                        Modifier.fillMaxWidth().height(esc.d(54)).clip(RoundedCornerShape(esc.d(18)))
                            .background(if (activa) Uva else p.superficie.copy(alpha = 0.75f))
                            .clickable { Diptico.elegirIndice(i) }
                            .padding(start = esc.d(10), end = esc.d(12)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconoApp(a.paquete, 36f)
                        Spacer(Modifier.width(esc.d(12)))
                        Texto(a.nombre, 16f, color = if (activa) Color.White else p.tinta, lineas = 1, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(esc.d(28)).clip(CircleShape)
                                .background(if (marcada) (if (activa) Color.White else Color(0xFF2EC4A6)) else Color.Transparent)
                                .border(esc.d(2), if (activa) Color.White else p.tenue, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (marcada) IconoDiptico(Icono.CHECK, if (activa) Uva else Color.White, 18f)
                        }
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("A" to "Marcar", "B" to "Listo")
        }
    }
}

/** Pegar o escribir la clave de SteamGridDB. */
@Composable
private fun PanelClave(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Velo {
        Column(
            Modifier.width(esc.d(anchoPanel(540f))).clip(RoundedCornerShape(esc.d(24))).background(p.superficie).sinCerrar()
                .padding(esc.d(18)),
            verticalArrangement = Arrangement.spacedBy(esc.d(10)),
        ) {
            val (icono, titulo, ayuda, ejemplo) = when (e.textoPara) {
                "nombre" -> listOf(Icono.TEXTO, "¿Cómo te llamas?", "Bisa te saluda por tu nombre cuando prendes la consola.", "Ej: Nico")
                "ia_clave_gemini" -> listOf(Icono.CLAVE, "Clave de Gemini (gratis)", "Entra a aistudio.google.com/apikey con tu cuenta de Google, toca \"Create API key\" y pégala aquí. Solo se guarda en tu consola.", "Pega tu clave")
                "ia_clave_openai" -> listOf(Icono.CLAVE, "Clave de ChatGPT", "Entra a platform.openai.com/api-keys, crea una clave y pégala aquí. Usa el saldo de tu cuenta de OpenAI. Solo se guarda en tu consola.", "Pega tu clave")
                "ra_usuario" -> listOf(Icono.TROFEO, "Usuario de RetroAchievements", "Tu nombre de usuario en retroachievements.org (no tu correo).", "Ej: Nikalbazu")
                "ra_clave" -> listOf(Icono.CLAVE, "Clave de RetroAchievements", "En retroachievements.org entra a Settings y copia tu \"Web API Key\". Solo se guarda en tu consola.", "Pega tu Web API Key")
                "buscar_imagen" -> listOf(Icono.BUSCAR, "Buscar con otro nombre", "Escribe el nombre del juego como se conoce en inglés o su nombre completo.", "Ej: The Legend of Zelda")
                "pc_steam" -> listOf(Icono.BUSCAR, "Agregar un juego de Steam", "Escribe su nombre como aparece en Steam. Lo abrirá GameNative o GameHub Lite.", "Ej: Hollow Knight")
                "asistente" -> listOf(Icono.ASISTENTE, "Pregúntale al asistente", "Lo que quieras: qué jugar, cómo pasar una parte, trucos, o que cambie algo de Díptico.", "Ej: ¿qué juego corto me recomiendas?")
                "ia_pedido" -> listOf(Icono.CHISPA, "Crear tema con IA", "Escribe tu idea, como \"Halloween japonés\" o \"Navidad en la playa\". Si lo dejas vacío, la IA elige la fiesta del mundo más cercana a hoy.", "Ej: Año Nuevo Lunar con faroles")
                "titulo" -> listOf(Icono.TITULO, "Título del juego", "Así se verá el nombre en todo Díptico.", "Escribe el título")
                "buscar_juego" -> listOf(Icono.BUSCAR, "Buscar un juego", "Escribe parte del nombre; también puedes dictarlo con la tecla Voz.", "Ej: zelda")
                "coleccion_nueva" -> listOf(Icono.CARPETA, "Nombre de la colección", "Por ejemplo \"Para jugar con mi hijo\" o \"RPG pendientes\". Aparecerá como widget en tu inicio.", "Ej: RPG pendientes")
                "consola_nombre" -> listOf(Icono.TEXTO, "Nombre de la consola", "Así se verá en las pestañas y en todo Díptico.", "Ej: Mi Super Nintendo")
                "ras_igdbId", "ras_igdbSecreto" -> listOf(Icono.CLAVE, Diptico.tituloRaspador(e.textoPara)!!, "Gratis: entra a dev.twitch.tv › Your Console › Register Your Application (categoría «Application Integration», URL http://localhost). Copia el Client ID y toca New Secret para el secreto.", "Pégalo aquí")
                "ras_ssUsuario", "ras_ssClave" -> listOf(Icono.CLAVE, Diptico.tituloRaspador(e.textoPara)!!, "Tu cuenta gratis de screenscraper.fr. Con cuenta hay más búsquedas por día y más rápidas.", if (e.textoPara == "ras_ssUsuario") "Ej: Nikalbazu" else "Tu contraseña")
                "ras_ssDevId", "ras_ssDevClave" -> listOf(Icono.CLAVE, Diptico.tituloRaspador(e.textoPara)!!, "ScreenScraper pide credenciales de desarrollador para apps como Díptico. Se piden gratis en su foro, sección «API». Te llegan un devid y un devpassword.", "Pégalo aquí")
                "ras_rawg" -> listOf(Icono.CLAVE, "Clave de RAWG", "Gratis: crea una cuenta en rawg.io, entra a rawg.io/apidocs, toca «Get API Key» y copia la clave.", "Pega tu clave")
                "ras_tgdb" -> listOf(Icono.CLAVE, "Clave de TheGamesDB", "Gratis: se pide en el foro de thegamesdb.net (sección «API Key Requests»). Te llega una clave pública.", "Pega tu clave")
                "notas" -> listOf(Icono.LAPIZ, "Mis trucos y notas", "Claves, trucos, coleccionables, lo que quieras recordar de este juego.", "Ej: el código del cofre es 4-2-1")
                else -> listOf(Icono.CLAVE, "Clave de SteamGridDB", "Es gratis: entra a steamgriddb.com, inicia sesión, ve a Preferencias › API y copia tu clave aquí.", "Pega tu clave")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(icono as Icono, p.tinta, 24f, acento = Coral)
                Spacer(Modifier.width(esc.d(10)))
                Texto(titulo as String, 20f, lineas = 1)
            }
            Detalle(ayuda as String, 14f)
            // Enlace directo a la página de la clave, y un QR por si prefieres sacarla desde el teléfono.
            Diptico.urlClave(e.textoPara)?.let { url ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(16))).background(p.fondo2).padding(esc.d(10)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Box(
                            Modifier.clip(RoundedCornerShape(50)).fondoBoton(Coral).clickable { Diptico.abrirPaginaClave(e.textoPara) }
                                .padding(horizontal = esc.d(14), vertical = esc.d(8)),
                        ) { Texto("Sacar mi clave (abre la página)", 13.5f, color = Color.White, lineas = 1) }
                        Spacer(Modifier.height(esc.d(6)))
                        if (e.enlaceUrl != null) {
                            Detalle("O escanea el código con tu teléfono (misma Wi-Fi): ahí sacas la clave, la pegas, y llega sola aquí.", 12f, lineas = 3)
                        } else {
                            Detalle("O escanea el código con tu teléfono.", 12f, lineas = 1)
                            Detalle(url.removePrefix("https://"), 11f, lineas = 1)
                        }
                    }
                    Spacer(Modifier.width(esc.d(10)))
                    CodigoQR(e.enlaceUrl ?: url, 72f)
                }
            }
            val esClave = e.textoPara == "sgdb" || e.textoPara.startsWith("ia_clave") || e.textoPara == "ra_clave" || (e.textoPara.startsWith("ras_") && e.textoPara != "ras_ssUsuario")
            CampoTexto(e.borrador, ejemplo as String, { Diptico.guardarClave() }, numerico = esClave, alto = if (e.textoPara == "notas") 130f else 60f)
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                BotonGrande("B · Cancelar", p.fondo2, p.tinta, Modifier.weight(1f)) { Diptico.cerrarPanel() }
                val quitar = esClave && e.borrador.isBlank() && (when (e.textoPara) { "sgdb" -> e.claveSgdb; "ra_clave" -> e.claveRA; "ia_clave_gemini" -> e.claveGemini; "ia_clave_openai" -> e.claveOpenAI; else -> if (e.textoPara.startsWith("ras_")) Diptico.almacen.texto(e.textoPara) else "" }).isNotBlank()
                BotonGrande(
                    when { quitar -> "Quitar clave"; e.textoPara == "ia_pedido" -> "Crear tema"; e.textoPara == "asistente" -> "Preguntar"; else -> "Guardar" },
                    Uva, SobreUva, Modifier.weight(1.4f),
                ) { Diptico.guardarClave() }
            }
        }
    }
}


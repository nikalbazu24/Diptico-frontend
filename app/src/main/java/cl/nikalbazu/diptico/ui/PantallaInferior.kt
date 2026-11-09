@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.testTag
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
import cl.nikalbazu.diptico.estado.VistaPartida
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
                        "carpetas" -> {
                            VistaCarpetas(e, Modifier.fillMaxWidth().weight(1f), escalaEstante)
                            if (e.vista == "mosaicos") FranjaAccesos(e)
                        }
                        "sistemas", "mosaico", "vitrina", "discos" -> {
                        AnimatedContent(
                            targetState = e.enSistemas,
                            transitionSpec = {
                                (fadeIn(tween(260)) + scaleIn(tween(320), initialScale = if (targetState) 1.06f else 0.94f)) togetherWith fadeOut(tween(160))
                            },
                            label = "sistemas", modifier = Modifier.fillMaxWidth().weight(1f),
                        ) { eligiendo ->
                            if (eligiendo) when (e.vistaConsolas) {
                                "mosaico", "vitrina" -> CarpetasConsolas(e, Modifier.fillMaxSize())
                                "discos" -> DiscosConsolas(e, Modifier.fillMaxSize())
                                else -> SistemasCarrusel(e, Modifier.fillMaxSize())
                            }
                            else Column(Modifier.fillMaxSize()) {
                                CabeceraSistema(e)
                                VistaJuegos(e, Modifier.fillMaxWidth().weight(1f), escalaEstante)
                            }
                        }
                        if (e.vista == "mosaicos") FranjaAccesos(e)
                        }
                        else -> {
                            Cinta(e)
                            Box(Modifier.padding(horizontal = esc.d(16)).fillMaxWidth().height(esc.d(1)).background(p.linea))
                            VistaJuegos(e, Modifier.fillMaxWidth().weight(1f), escalaEstante)
                            if (e.vista == "mosaicos") FranjaAccesos(e)
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
    val animar = LocalMovimiento.current
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
    val mov = if (animar) ajusteMovimiento() else AjusteMovimiento(0f)
    AnimatedContent(
        targetState = e.panel,
        transitionSpec = {
            // Los paneles completos suben y se posan; los flotantes crecen con un leve rebote (según Animaciones y Velocidad).
            val completo = targetState in setOf(Panel.MENU, Panel.ESTUDIO, Panel.ENCUADRE, Panel.APPS, Panel.EDITAR_CONSOLA, Panel.FICHA, Panel.PORTS, Panel.BUSCAR, Panel.CARPETAS, Panel.ESTADISTICAS, Panel.IMAGENES, Panel.ASISTENTE, Panel.GALERIA)
            val entra = if (completo) entradaVentana(mov) else entradaFlotante(mov)
            val sale = if (initialState == Panel.NINGUNO) salidaVentana(mov) else salidaFlotante(mov)
            (entra togetherWith sale).using(SizeTransform(clip = false))
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
            Panel.GALERIA -> PanelGaleria(e, conPrevia = conAyuda)
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
    val animar = LocalMovimiento.current
    var borrador by remember(e.tamJuegos) { androidx.compose.runtime.mutableFloatStateOf(e.tamJuegos) }
    var ajustando by remember { androidx.compose.runtime.mutableStateOf(false) }
    val t by animateFloatAsState(borrador, tween(duracionMovimiento(if (ajustando) 60 else 220)), label = "densidad juegos")
    Column(modifier) {
        ControlesBiblioteca(e)
        androidx.compose.animation.AnimatedVisibility(ajustando, enter = fadeIn(tween(duracionMovimiento(100))), exit = fadeOut(tween(duracionMovimiento(180)))) {
            ParadasDensidad(cl.nikalbazu.diptico.estado.Densidades.juegos, borrador, { Diptico.elegirTamJuegos(it) })
        }
        androidx.compose.runtime.CompositionLocalProvider(LocalTamJuegos provides t) {
    AnimatedContent(
        targetState = e.vista,
        transitionSpec = { (fadeIn(tween(if (animar) 200 else 0)) + scaleIn(tween(if (animar) 260 else 0), initialScale = 0.98f)) togetherWith fadeOut(tween(if (animar) 160 else 0)) },
        label = "vista",
        modifier = Modifier.fillMaxWidth().weight(1f).testTag("zona-juegos").ajustarDensidad(
            iniciar = { ajustando = true },
            ajustar = { borrador = cl.nikalbazu.diptico.estado.Densidades.juegos.limitar(borrador * it) },
            terminar = { guardar ->
                if (guardar) Diptico.elegirTamJuegos(borrador)
                borrador = if (guardar) cl.nikalbazu.diptico.estado.Densidades.juegos.masCercana(borrador).escala else e.tamJuegos
                ajustando = false
            }),
    ) { vista ->
        when (vista) {
            "mosaicos" -> Mosaicos(e, Modifier.fillMaxSize())
            "cuadricula" -> Cuadricula(e, Modifier.fillMaxSize())
            "lista" -> ListaJuegos(e, Modifier.fillMaxSize())
            "carrusel" -> CarruselJuegos(e, Modifier.fillMaxSize())
            // Vistas que ya no existen (estante, abanico, lomos…): se ven como mosaicos.
            else -> Mosaicos(e, Modifier.fillMaxSize())
        }
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
            val nuevo = e.vista == "mosaicos"
            val tono = c?.color ?: Coral
            val escala by animateFloatAsState(if (activa) (if (nuevo) 1.06f else 1.14f) else 1f, spring(dampingRatio = 0.5f), label = "icono")
            val forma = RoundedCornerShape(esc.d(14))
            Column(
                // Mantener apretada una consola abre su personalización.
                Modifier.width(esc.d(64))
                    .then(if (nuevo && activa) Modifier.shadow(esc.d(14), forma, ambientColor = tono, spotColor = tono).clip(forma)
                        .background(p.superficie).border(esc.d(1), Color.White.copy(alpha = 0.22f), forma) else Modifier)
                    .padding(vertical = if (nuevo) esc.d(4) else esc.d(0))
                    .combinedClickable(onLongClick = { Diptico.abrirEditarConsola(id) }) { Diptico.elegirConsola(id) },
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
                Box(Modifier.width(ancho).height(esc.d(3)).clip(RoundedCornerShape(50)).background(if (nuevo) tono else Coral))
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
 * Compañero de partida (rediseño 9 nov 2026): mientras juegas en la otra pantalla, aquí queda lo justo, agrupado.
 * Arriba una línea fina (juego, tiempo de sesión, hora); luego DIPPO con un consejo según lo que pasa, tu partida
 * (próximo paso y cuánto te queda) y la música si suena; abajo tu dock y cuatro botones grandes. Lo demás vive en «Más».
 * No hay botón «Terminé»: la partida se cierra sola cuando sales del juego y vuelves a Díptico en esa pantalla.
 * A los 5 segundos se atenúa sola (Ajustes › Pantallas); tocarla la despierta.
 */
@Composable
private fun EnJuego(e: Estado) {
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
    val color = if (e.experiencia.universo && e.colorUniverso != 0L) Color(e.colorUniverso) else juego?.let { colorDe(it.clave) } ?: Uva
    val tenue = Color(0xFFB9B5D6)
    var mas by remember(s.inicio) { mutableStateOf(false) }

    Box(
        Modifier.fillMaxSize().background(Color(0xFF07060C)).pointerInput(Unit) {
            detectTapGestures { Diptico.despertar() }
        },
    ) {
        // Un brillo quieto del color del juego (sin animación: no gasta batería mientras juegas).
        Box(
            Modifier.align(Alignment.TopStart).size(esc.d(460))
                .background(Brush.radialGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent))),
        )
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(16), vertical = esc.d(12))) {
            // 1 · Una línea fina: qué juegas, cuánto llevas y la hora.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (c != null) { IconoConsola(c, 18f, Color(0xFF07060C)); Spacer(Modifier.width(esc.d(8))) }
                Texto(juego?.titulo ?: "En partida", 14f, color = Color.White, lineas = 1, traducir = false, modifier = Modifier.weight(1f))
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = esc.d(10), vertical = esc.d(4))) {
                    Texto("⏱ $reloj", 13f, color = tenue, traducir = false)
                }
                if (!LocalBarraSistema.current) {
                    Spacer(Modifier.width(esc.d(10)))
                    val bat = rememberBateria()
                    Texto(rememberHora() + if (bat >= 0) " · $bat%" else "", 13f, color = tenue, traducir = false)
                }
            }
            Spacer(Modifier.height(esc.d(10)))
            if (mas) MasHerramientas(e, s, Modifier.weight(1f)) { mas = false }
            else Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
                // 2 · DIPPO: un consejo según lo que pasa, y la pista a un toque.
                ConsejoDippo(e, s, juego, (seg / 60).toInt())
                // 3 · Tu partida: el próximo paso y cuánto te queda.
                TuPartida(e, s.clave)
                // 4 · La música, solo si suena algo.
                MusicaEnJuego()
                // 5 · El logo del juego, como en Cocoon: flota suave en el espacio que queda.
                LogoEnPartida(juego, color, Modifier.weight(1f).fillMaxWidth())
                // 6 · Tus apps: se abren aquí y el juego sigue arriba.
                if (e.extra.dock && e.appsFijadas.isNotEmpty()) FilaDock(e, 34f, Modifier.align(Alignment.CenterHorizontally))
            }
            Spacer(Modifier.height(esc.d(8)))
            // 6 · Cuatro botones grandes (sin «Terminé»: la partida se cierra sola al salir del juego).
            if (!mas) Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                AccionCompa(Icono.BISA, "Pista", Modifier.weight(1f), Coral.copy(alpha = 0.22f)) { pedirPista(s.clave) }
                AccionCompa(Icono.LAPIZ, "Mi paso", Modifier.weight(1f)) { Diptico.abrirPaso() }
                AccionCompa(Icono.TUTORIAL, "Guías", Modifier.weight(1f)) { Diptico.abrirFichaDe(s.clave, 2) }
                AccionCompa(Icono.MENU, "Más", Modifier.weight(1f)) { mas = true }
            }
        }
    }
}

/** El logo del juego (o su nombre en grande) flotando despacio, con un brillo del color de la consola detrás. */
@Composable
private fun LogoEnPartida(juego: cl.nikalbazu.diptico.data.Juego?, color: Color, modifier: Modifier) {
    val esc = LocalEscala.current
    val anima = Diptico.estado.collectAsState().value.let { it.despierta && it.experiencia.movimiento && Estilo.efectosCompletos }
    val flotar = if (anima) {
        val t = rememberInfiniteTransition(label = "logo")
        val f by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flotar")
        f
    } else 0f
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.size(esc.d(260)).background(Brush.radialGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent))))
        val logo = rememberImagen(juego?.logo, 800)
        Box(Modifier.graphicsLayer { translationY = flotar * 5f * density }) {
            if (logo != null) Image(
                logo, contentDescription = juego?.titulo, contentScale = ContentScale.Fit,
                modifier = Modifier.heightIn(max = esc.d(110)).widthIn(max = esc.d(340)).padding(esc.d(6)),
            ) else Texto(juego?.titulo ?: "Jugando", 26f, color = Color.White, lineas = 2, interlineado = 30f, alinear = TextAlign.Center, traducir = false,
                modifier = Modifier.padding(horizontal = esc.d(20)))
        }
    }
}

/** Le pide a DIPPO una pista sin spoilers para el juego en curso. */
private fun pedirPista(clave: String) {
    Diptico.prepararAyuda()
    Diptico.ejecutarAccion(cl.nikalbazu.diptico.estado.AccionChat("bisa_pista", clave, ""))
}

/** DIPPO con un consejo que cambia según lo que pasa (reintentos, mucho rato, sin nota…). */
@Composable
private fun ConsejoDippo(e: Estado, s: cl.nikalbazu.diptico.estado.Sesion, juego: cl.nikalbazu.diptico.data.Juego?, min: Int) {
    val esc = LocalEscala.current
    val reintentos = remember(s.clave, min) { cl.nikalbazu.diptico.estado.Asistente.reintentos(s.clave) }
    val paso = e.datos[s.clave]?.paso.orEmpty()
    val (texto, cara) = when {
        reintentos >= 2 -> "Llevas $reintentos intentos seguidos. ¿Te tiro una pista sin spoilers?" to CaraDippo.PREOCUPADO
        e.temporizadorFin > 0L -> "Te aviso para la pausa en ${((e.temporizadorFin - System.currentTimeMillis()).coerceAtLeast(0L) + 59_999) / 60_000} min." to null
        min >= 90 -> "Llevas ${min / 60} h ${min % 60} min. ¿Una pausa pronto? Estírate un poquito." to CaraDippo.PENSANDO
        paso.isBlank() -> "¿Anotamos dónde vas? Toca «Mi paso» y la próxima vez te lo recuerdo." to null
        else -> "¡Aquí estoy! Si te atascas en ${juego?.titulo ?: "el juego"}, toca Pista." to CaraDippo.FELIZ
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(18))).background(Color.White.copy(alpha = 0.06f))
            .clickable { Diptico.prepararAyuda() }.padding(esc.d(10)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(52).value, cara = cara)
        Spacer(Modifier.width(esc.d(10)))
        Detalle(texto, 14f, color = Color.White, lineas = 3, modifier = Modifier.weight(1f))
    }
}

/** Tu próximo paso (se toca para cambiarlo) y cuánto te queda según HowLongToBeat. */
@Composable
private fun TuPartida(e: Estado, clave: String) {
    val esc = LocalEscala.current
    val paso = e.datos[clave]?.paso.orEmpty()
    val d = e.extra.duraciones[clave]?.takeIf { it.tiene }
    val jugado = e.datos[clave]?.tiempo ?: 0L
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(18))).background(Color.White.copy(alpha = 0.06f))
            .clickable { Diptico.abrirPaso() }.padding(esc.d(12)),
        verticalArrangement = Arrangement.spacedBy(esc.d(4)),
    ) {
        Etiqueta("Tu próximo paso", Color(0xFFC9B8FF), 10.5f)
        Texto(paso.ifBlank { "Toca para anotar dónde vas" }, 16f, color = if (paso.isBlank()) Color(0xFF8E89A8) else Color.White, lineas = 2)
        if (d != null && d.principal > 0f) {
            Spacer(Modifier.height(esc.d(4)))
            val avance = (jugado / 3600f / d.principal).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(esc.d(6)).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.12f))) {
                Box(Modifier.fillMaxWidth(avance).fillMaxHeight().clip(RoundedCornerShape(50)).background(Brush.horizontalGradient(listOf(Coral, Sol))))
            }
            cl.nikalbazu.diptico.estado.CuantoDura.teQueda(d, jugado)?.let { Detalle(it, 11.5f, color = Color(0xFFB9B5D6), lineas = 1) }
        }
    }
}

/** «Más»: las herramientas que antes estaban repartidas en pestañas, juntas en una grilla. */
@Composable
private fun MasHerramientas(e: Estado, s: cl.nikalbazu.diptico.estado.Sesion, modifier: Modifier, cerrar: () -> Unit) {
    val esc = LocalEscala.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Texto("Más herramientas", 17f, color = Color.White, modifier = Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.1f)).clickable(onClick = cerrar)
                .padding(horizontal = esc.d(12), vertical = esc.d(6))) { Texto("Volver", 13f, color = Color.White) }
        }
        val herramientas = buildList<Triple<Icono, String, () -> Unit>> {
            if (s.displayId == android.view.Display.DEFAULT_DISPLAY) add(Triple(Icono.BUSCAR, "Lupa", { Diptico.alternarLupaJuego(s.clave, true) }))
            add(Triple(Icono.MUNDO, "Mapa", { Diptico.abrirMapa(s.clave) }))
            add(Triple(Icono.RELOJ, if (e.temporizadorFin > 0L) "Quitar aviso" else "Aviso en ${e.temporizadorMin} min", { Diptico.alternarTemporizador() }))
            add(Triple(Icono.RELOJ, if (e.cronoInicio > 0L) "Pausar cronómetro" else "Cronómetro", { Diptico.alternarCrono() }))
            add(Triple(Icono.INFO, "Ficha del juego", { Diptico.abrirFichaDe(s.clave, 0) }))
            add(Triple(Icono.BISA, "Hablar con DIPPO", { Diptico.prepararAyuda() }))
            add(Triple(Icono.APPS, "Todas las apps", { Diptico.abrirApps() }))
            add(Triple(Icono.NEGRO, "Apagar esta pantalla", { Diptico.atenuar() }))
        }
        herramientas.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                fila.forEach { (icono, texto, accion) -> AccionCompa(icono, texto, Modifier.weight(1f)) { cerrar(); accion() } }
                if (fila.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.weight(1f))
        // Por si la consola no se dio cuenta (por ejemplo, abriste otra app encima del juego).
        Detalle("¿Ya no estás jugando? Díptico lo nota solo al volver al inicio en esa pantalla.", 11.5f, color = Color(0xFF8E89A8), lineas = 2)
        Box(Modifier.clip(RoundedCornerShape(50)).border(esc.d(1), Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
            .clickable { cerrar(); Diptico.cerrarSesion(preguntar = true) }.padding(horizontal = esc.d(12), vertical = esc.d(6))) {
            Texto("Marcar como terminada", 12f, color = Color(0xFFB9B5D6))
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
internal fun PanelClave(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Velo {
        Column(
            Modifier.width(esc.d(anchoPanel(540f))).clip(RoundedCornerShape(esc.d(24))).background(p.superficie).sinCerrar()
                .padding(esc.d(18)),
            verticalArrangement = Arrangement.spacedBy(esc.d(10)),
        ) {
            val (icono, titulo, ayuda, ejemplo) = when (e.textoPara) {
                "nombre" -> listOf(Icono.TEXTO, "¿Cómo te llamas?", "DIPPO te saluda por tu nombre cuando prendes la consola.", "Ej: Nico")
                "ia_clave_gemini" -> listOf(Icono.CLAVE, "Clave de Gemini (gratis)", "Entra a aistudio.google.com/apikey con tu cuenta de Google, toca \"Create API key\" y pégala aquí. Solo se guarda en tu consola.", "Pega tu clave")
                "ia_clave_openai" -> listOf(Icono.CLAVE, "Clave de ChatGPT", "Entra a platform.openai.com/api-keys, crea una clave y pégala aquí. Usa el saldo de tu cuenta de OpenAI. Solo se guarda en tu consola.", "Pega tu clave")
                "ra_usuario" -> listOf(Icono.TROFEO, "Usuario de RetroAchievements", "Tu nombre de usuario en retroachievements.org (no tu correo).", "Ej: Nikalbazu")
                "ra_clave" -> listOf(Icono.CLAVE, "Clave de RetroAchievements", "En retroachievements.org entra a Settings y copia tu \"Web API Key\". Solo se guarda en tu consola.", "Pega tu Web API Key")
                "buscar_imagen" -> listOf(Icono.BUSCAR, "Buscar con otro nombre", "Escribe el nombre del juego como se conoce en inglés o su nombre completo.", "Ej: The Legend of Zelda")
                "pc_steam" -> listOf(Icono.BUSCAR, "Agregar un juego de Steam", "Escribe su nombre como aparece en Steam. Lo abrirá GameNative o GameHub Lite.", "Ej: Hollow Knight")
                "reporte_fallas" -> listOf(Icono.INFO, "Reportar un problema", "Cuéntanos qué pasó. El reporte incluye la versión, el modelo y el diagnóstico técnico. Se comparte al confirmar.", "Ej: la pantalla inferior se quedó en negro")
                "bisa_paso" -> listOf(Icono.LAPIZ, "Mi próximo paso", "DIPPO lo recuerda para este juego.", "Ej: volver al puente")
                "asistente" -> listOf(Icono.ASISTENTE, "Pregúntale al asistente", "Lo que quieras: qué jugar, cómo pasar una parte, trucos, o que cambie algo de Díptico.", "Ej: ¿qué juego corto me recomiendas?")
                "tema_enlace" -> listOf(Icono.WEB, "Importar un tema por enlace", "Pega el enlace HTTPS al JSON que quieras instalar.", "https://…/temas.json")
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
            CampoTexto(e.borrador, ejemplo as String, { Diptico.guardarClave() }, numerico = esClave, alto = if (e.textoPara == "notas" || e.textoPara == "reporte_fallas") 130f else 60f)
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                BotonGrande("B · Cancelar", p.fondo2, p.tinta, Modifier.weight(1f)) { Diptico.cerrarPanel() }
                val quitar = esClave && e.borrador.isBlank() && (when (e.textoPara) { "sgdb" -> e.claveSgdb; "ra_clave" -> e.claveRA; "ia_clave_gemini" -> e.claveGemini; "ia_clave_openai" -> e.claveOpenAI; else -> if (e.textoPara.startsWith("ras_")) Diptico.almacen.texto(e.textoPara) else "" }).isNotBlank()
                BotonGrande(
                    when { quitar -> "Quitar clave"; e.textoPara == "tema_enlace" -> "Leer tema"; e.textoPara == "asistente" -> "Preguntar"; e.textoPara == "reporte_fallas" -> "Continuar"; else -> "Guardar" },
                    Uva, SobreUva, Modifier.weight(1.4f),
                ) { Diptico.guardarClave() }
            }
        }
    }
}



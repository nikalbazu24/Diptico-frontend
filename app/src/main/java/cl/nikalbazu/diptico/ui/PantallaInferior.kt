package cl.nikalbazu.diptico.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.animation.togetherWith
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Medio
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
        Box(Modifier.fillMaxSize().difuminar(e)) {
            when {
                s != null && s.displayId != miDisplay && !s.dosPantallas -> EnJuego(e)
                else -> Column(Modifier.fillMaxSize()) {
                    Cinta(e)
                    Box(Modifier.padding(horizontal = esc.d(16)).fillMaxWidth().height(esc.d(1)).background(p.linea))
                    VistaJuegos(e, Modifier.fillMaxWidth().weight(1f), if (esc.angosta) 1.08f else 1.18f)
                    Dock(e)
                }
            }
        }
        Paneles(e, conAyuda = false)
    }
}

/** Menús, próximo paso, clave y avisos: se dibujan encima de todo. */
@Composable
fun BoxScope.Paneles(e: Estado, conAyuda: Boolean) {
    // Cada panel entra y sale con su animación: los de pantalla completa suben, los flotantes crecen.
    AnimatedContent(
        targetState = e.panel,
        transitionSpec = {
            val completo = targetState in setOf(Panel.MENU, Panel.FICHA, Panel.WIDGETS, Panel.PORTS, Panel.BUSCAR, Panel.CARPETAS, Panel.ESTADISTICAS, Panel.IMAGENES, Panel.ASISTENTE)
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
            Panel.MENU -> PanelMenu(e, conAyuda)
            Panel.RAPIDO -> VentanaJuego(e)
            Panel.BUSCAR -> PanelBuscar(e)
            Panel.CENTRO -> PanelCentro(e)
            Panel.CARPETAS -> PanelCarpetas(e)
            Panel.PASO -> PanelPaso(e)
            Panel.CLAVE -> PanelClave(e)
            Panel.PORTS -> PanelPorts(e)
            Panel.FICHA -> PanelFicha(e)
            Panel.WIDGETS -> PanelWidgets(e)
            Panel.ESTADISTICAS -> PanelEstadisticas(e)
            Panel.IMAGENES -> PanelImagenes(e)
            Panel.ASISTENTE -> PanelAsistente(e)
            Panel.TRABAJOS, Panel.NINGUNO -> Box(Modifier.fillMaxSize())
        }
    }
    Aviso(e.aviso, Modifier.align(Alignment.BottomCenter), abajo = if (e.panel == Panel.NINGUNO) 70f else 16f)
}

/** Con una ventana encima (opciones del juego, textos), lo de atrás se difumina. */
fun Modifier.difuminar(e: Estado): Modifier =
    if (Estilo.efectosCompletos && e.panel in setOf(Panel.RAPIDO, Panel.PASO, Panel.CLAVE, Panel.CENTRO)) this.blur(16.dp) else this

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
            else -> Column(Modifier.fillMaxSize()) {
                Estante(e, alto = (if (esc.angosta) 192f else 214f) * t.coerceAtMost(1.25f), escala = escalaEstante * t)
                Info(e)
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
                Modifier.width(esc.d(64)).clickable { Diptico.elegirConsola(id) },
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
                        onLongClick = { Diptico.elegirJuego(j.clave); Diptico.abrirRapido() },
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
                    ) { CajaJuego(j, c, escala = escala) }
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

@Composable
private fun Dock(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column {
        Box(Modifier.fillMaxWidth().height(esc.d(1)).background(p.linea))
        Row(Modifier.fillMaxWidth().padding(horizontal = esc.d(8)).height(esc.d(62))) {
            ItemDock(Icono.CONTINUAR, "Continuar", p.acentoTexto) { Diptico.continuar() }
            ItemDock(Icono.BUSCAR, "Buscar") { Diptico.abrirBuscar() }
            ItemDock(Icono.VISTA, Diptico.nombreVista(e.vista)) { Diptico.cambiarVista() }
            ItemDock(Icono.APPS, if (e.inicio == "widgets") "Carátulas" else "Widgets") { Diptico.alternarInicio() }
            if (!e.unaPantalla) ItemDock(Icono.INTERCAMBIAR, "Intercambiar") { Diptico.intercambiar() }
            ItemDock(Icono.MENU, "Menú") { Diptico.abrirMenu() }
        }
    }
}

@Composable
private fun RowScope.ItemDock(icono: Icono, texto: String, color: Color = LocalPaleta.current.tinta, accion: () -> Unit) {
    val esc = LocalEscala.current
    Column(
        Modifier.weight(1f).fillMaxSize().clickable { accion() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconoDiptico(icono, color, 24f, acento = Coral)
        Spacer(Modifier.height(esc.d(4)))
        Texto(texto, 12f, color = color, familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1)
    }
}

/** Mientras juegas en la otra pantalla: esta se atenúa y muestra el logo o el nombre del juego. */
@Composable
private fun EnJuego(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val s = e.sesion ?: return
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
    val reloj = "${seg / 60}:${(seg % 60).toString().padStart(2, '0')}"
    val color = juego?.let { colorDe(it.clave) } ?: Uva

    Box(
        Modifier.fillMaxSize().background(Color(0xFF07060C)).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
        ) { if (!e.despierta) Diptico.despertar() },
    ) {
        if (!e.despierta) {
            Box(
                Modifier.align(Alignment.Center).size(esc.d(380)).background(
                    Brush.radialGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)),
                ),
            )
            Column(Modifier.align(Alignment.Center).padding(horizontal = esc.d(24)), horizontalAlignment = Alignment.CenterHorizontally) {
                Etiqueta(c?.nombreLargo ?: "", color, 12f)
                Spacer(Modifier.height(esc.d(12)))
                val logo = rememberImagen(juego?.logo, 800)
                if (logo != null) {
                    Image(
                        logo, contentDescription = juego?.titulo, contentScale = ContentScale.Fit,
                        modifier = Modifier.heightIn(max = esc.d(150)).widthIn(max = esc.d(380)),
                    )
                } else {
                    Texto(juego?.titulo ?: "Jugando", 40f, color = Color.White, lineas = 3, interlineado = 44f, alinear = TextAlign.Center)
                }
            }
            Texto(
                "Sesión · $reloj", 15f, color = Color(0xFF8E8AAE),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(46)),
            )
            Detalle(
                "Toca o aprieta un botón para abrir el compañero", 13f, color = Color(0xFF6E6A8E),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(20)),
            )
        } else {
            Column(Modifier.fillMaxSize().background(p.fondo2).padding(esc.d(18)), verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(esc.d(48)).clip(RoundedCornerShape(esc.d(14))).background(color), contentAlignment = Alignment.Center) {
                        IconoDiptico(Icono.JUGAR, Color.White, 24f)
                    }
                    Spacer(Modifier.width(esc.d(12)))
                    Column(Modifier.weight(1f)) {
                        Texto(juego?.titulo ?: "", 20f, lineas = 1)
                        Detalle("Sesión de $reloj", 13f)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                    BotonGrande("Anotar mi paso", Coral, Tinta, Modifier.weight(1f), alto = 64f) { Diptico.abrirPaso() }
                    BotonGrande("Atenuar", Sol, Tinta, Modifier.weight(1f), alto = 64f) { Diptico.atenuar() }
                }
                BotonGrande("Terminé de jugar", Uva, Color.White, Modifier.fillMaxWidth(), alto = 64f) { Diptico.cerrarSesion(preguntar = true) }
                Spacer(Modifier.weight(1f))
                Detalle("Se vuelve a atenuar sola en unos segundos.", 13f)
            }
        }
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
    LaunchedEffect(e.indice) { if (opciones.isNotEmpty()) lista.animateScrollToItem((e.indice - 2).coerceAtLeast(0)) }
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
    if (Diptico.estado.collectAsState().value.conTeclado) {
        // Se escribe con el teclado de Díptico, en la otra pantalla.
        Column(verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
            TextoEscrito(
                if (numerico && valor.length > 6) valor.take(4) + "•".repeat(minOf(valor.length - 4, 24)) else valor, ejemplo, 17f, alto - 24f,
                Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(16))).background(p.fondo)
                    .border(esc.d(2), Uva.copy(alpha = 0.6f), RoundedCornerShape(esc.d(16))).padding(esc.d(12)),
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
    LaunchedEffect(e.indice) { if (e.apps.isNotEmpty()) lista.animateScrollToItem((e.indice - 2).coerceAtLeast(0)) }
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
                        val icono = rememberImagen(android.net.Uri.parse("app://${a.paquete}"), 96)
                        Box(Modifier.size(esc.d(36)).clip(RoundedCornerShape(esc.d(10))).background(p.fondo), contentAlignment = Alignment.Center) {
                            if (icono != null) Image(icono, contentDescription = null, modifier = Modifier.fillMaxSize())
                        }
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
                "ia_clave" -> listOf(Icono.CLAVE, "Clave de IA (Claude)", "Entra a console.anthropic.com, ve a API Keys, crea una clave y pégala aquí. Solo se guarda en tu consola.", "Pega tu clave")
                "ra_usuario" -> listOf(Icono.TROFEO, "Usuario de RetroAchievements", "Tu nombre de usuario en retroachievements.org (no tu correo).", "Ej: Nikalbazu")
                "ra_clave" -> listOf(Icono.CLAVE, "Clave de RetroAchievements", "En retroachievements.org entra a Settings y copia tu \"Web API Key\". Solo se guarda en tu consola.", "Pega tu Web API Key")
                "asistente" -> listOf(Icono.ASISTENTE, "Pregúntale al asistente", "Lo que quieras: qué jugar, cómo pasar una parte, trucos, o que cambie algo de Díptico.", "Ej: ¿qué juego corto me recomiendas?")
                "ia_pedido" -> listOf(Icono.CHISPA, "Crear tema con IA", "Escribe tu idea, como \"Halloween japonés\" o \"Navidad en la playa\". Si lo dejas vacío, la IA elige la fiesta del mundo más cercana a hoy.", "Ej: Año Nuevo Lunar con faroles")
                "titulo" -> listOf(Icono.TITULO, "Título del juego", "Así se verá el nombre en todo Díptico.", "Escribe el título")
                "notas" -> listOf(Icono.LAPIZ, "Mis trucos y notas", "Claves, trucos, coleccionables, lo que quieras recordar de este juego.", "Ej: el código del cofre es 4-2-1")
                else -> listOf(Icono.CLAVE, "Clave de SteamGridDB", "Es gratis: entra a steamgriddb.com, inicia sesión, ve a Preferencias › API y copia tu clave aquí.", "Pega tu clave")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(icono as Icono, p.tinta, 24f, acento = Coral)
                Spacer(Modifier.width(esc.d(10)))
                Texto(titulo as String, 20f, lineas = 1)
            }
            Detalle(ayuda as String, 14f)
            val esClave = e.textoPara == "sgdb" || e.textoPara == "ia_clave" || e.textoPara == "ra_clave"
            CampoTexto(e.borrador, ejemplo as String, { Diptico.guardarClave() }, numerico = esClave, alto = if (e.textoPara == "notas") 130f else 60f)
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                BotonGrande("B · Cancelar", p.fondo2, p.tinta, Modifier.weight(1f)) { Diptico.cerrarPanel() }
                val quitar = esClave && e.borrador.isBlank() && (when (e.textoPara) { "sgdb" -> e.claveSgdb; "ra_clave" -> e.claveRA; else -> e.claveIa }).isNotBlank()
                BotonGrande(
                    when { quitar -> "Quitar clave"; e.textoPara == "ia_pedido" -> "Crear tema"; e.textoPara == "asistente" -> "Preguntar"; else -> "Guardar" },
                    Uva, SobreUva, Modifier.weight(1.4f),
                ) { Diptico.guardarClave() }
            }
        }
    }
}

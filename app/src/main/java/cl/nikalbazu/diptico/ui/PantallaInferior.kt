package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
        when {
            s != null && s.displayId != miDisplay && !s.dosPantallas -> EnJuego(e)
            else -> Column(Modifier.fillMaxSize()) {
                Cinta(e)
                Box(Modifier.padding(horizontal = esc.d(16)).fillMaxWidth().height(esc.d(1)).background(p.linea))
                Estante(e, alto = 184f, escala = 0.94f)
                Info(e)
                Spacer(Modifier.weight(1f))
                Dock(e)
            }
        }
        Paneles(e, conAyuda = false)
    }
}

/** Menús, próximo paso, clave y avisos: se dibujan encima de todo. */
@Composable
fun BoxScope.Paneles(e: Estado, conAyuda: Boolean) {
    when (e.panel) {
        Panel.MENU -> PanelMenu(e, conAyuda)
        Panel.RAPIDO -> PanelRapido(e)
        Panel.PASO -> PanelPaso(e)
        Panel.CLAVE -> PanelClave(e)
        Panel.NINGUNO -> Unit
    }
    Aviso(e.aviso, Modifier.align(Alignment.BottomCenter), abajo = if (e.panel == Panel.NINGUNO) 70f else 16f)
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
                    IconoMedio(
                        c?.medio ?: Medio.TODOS, c?.color ?: Uva, p.fondo2,
                        modifier = Modifier.graphicsLayer { scaleX = escala; scaleY = escala },
                        tam = 32f,
                    )
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
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val lista = e.visibles
    val estado = rememberLazyListState()
    LaunchedEffect(e.indiceSel, e.consolaSel, lista.size) {
        if (lista.isNotEmpty()) estado.animateScrollToItem((e.indiceSel - 1).coerceAtLeast(0))
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
                val elegido = i == e.indiceSel
                val alza by animateFloatAsState(if (elegido) -12f else 0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow), label = "alza")
                val tam by animateFloatAsState(if (elegido) 1.06f else 1f, spring(dampingRatio = 0.5f), label = "tam")
                val u = with(LocalDensity.current) { esc.d(1f).toPx() }
                val color = colorDe(j.clave)
                val forma = RoundedCornerShape(esc.d(c.caja.radio * escala))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { if (elegido) Diptico.jugar() else Diptico.elegirJuego(j.clave) },
                ) {
                    Box(
                        Modifier
                            .graphicsLayer { translationY = alza * u; scaleX = tam; scaleY = tam }
                            .then(
                                if (elegido) Modifier
                                    .shadow(esc.d(16), forma, ambientColor = color, spotColor = color)
                                    .border(esc.d(3), Color.White, forma)
                                else Modifier,
                            ),
                    ) { CajaJuego(j, c, escala = escala) }
                    Box(
                        Modifier.padding(top = esc.d(7)).width(esc.d(c.caja.ancho * escala)).height(esc.d(22)).clip(forma)
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
            ItemDock(Icono.DADO, "Sorpréndeme") { Diptico.sorprendeme() }
            ItemDock(Icono.LAPIZ, "Mi paso") { Diptico.abrirPaso() }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.CLAVE, p.tinta, 24f, acento = Coral)
                Spacer(Modifier.width(esc.d(10)))
                Texto("Clave de SteamGridDB", 20f, lineas = 1)
            }
            Detalle("Es gratis: entra a steamgriddb.com, inicia sesión, ve a Preferencias › API y copia tu clave aquí.", 14f)
            CampoTexto(e.borrador, "Pega tu clave", { Diptico.guardarClave() }, numerico = true)
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                BotonGrande("B · Cancelar", p.fondo2, p.tinta, Modifier.weight(1f)) { Diptico.cerrarPanel() }
                BotonGrande(if (e.borrador.isBlank() && e.claveSgdb.isNotBlank()) "Quitar clave" else "Guardar", Uva, Color.White, Modifier.weight(1.4f)) {
                    Diptico.guardarClave()
                }
            }
        }
    }
}

package cl.nikalbazu.diptico.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.Animatable
import cl.nikalbazu.diptico.estado.Opcion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import cl.nikalbazu.diptico.audio.Avisos
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Teclas
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------- Cuadrícula y lista de juegos ----------

/** Una carátula dentro de una celda del mismo tamaño para todas (sin importar la forma de la caja). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Celda(j: Juego, elegido: Boolean, anchoCelda: Float, altoCelda: Float, conTitulo: Boolean = true, alElegir: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val c = Consolas.porId(j.consolaId) ?: return
    val base = cajaDe(c)
    val k = minOf(anchoCelda / base.ancho, altoCelda / base.alto)
    val tam by animateFloatAsState(if (elegido) 1.07f else 1f, spring(dampingRatio = 0.55f), label = "celda")
    val color = colorDe(j.clave)
    Column(
        Modifier.width(esc.d(anchoCelda)).combinedClickable(
            interactionSource = null, indication = null,
            onLongClick = { Diptico.elegirJuego(j.clave); Diptico.elegirPantalla("juego") },
        ) { alElegir() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(esc.d(anchoCelda), esc.d(altoCelda)), contentAlignment = Alignment.BottomCenter) {
            val forma = RoundedCornerShape(esc.d(base.radio * k))
            Box(
                Modifier.graphicsLayer { scaleX = tam; scaleY = tam }
                    .then(if (elegido) Modifier.shadow(esc.d(14), forma, ambientColor = color, spotColor = color).border(esc.d(3), if (p == PaletaOscura) Color.White else Uva, forma) else Modifier),
            ) { CajaJuego(j, c, escala = k, vivo = elegido) }
        }
        if (conTitulo) {
            Spacer(Modifier.height(esc.d(6)))
            Texto(j.titulo, 12.5f, color = if (elegido) p.tinta else p.tenue, lineas = 2, interlineado = 14f, alinear = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

/** Todos los juegos en una cuadrícula de carátulas. */
@Composable
fun Cuadricula(e: Estado, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val t = LocalTamJuegos.current
    val anchoCelda = 96f * t
    val altoCelda = 112f * t
    val lista = e.visibles
    BoxWithConstraints(modifier) {
        val anchoUnidades = maxWidth.value / esc.factor
        val cols = ((anchoUnidades - 24f) / (anchoCelda + 12f)).toInt().coerceAtLeast(2)
        SideEffect { Teclas.columnasCuadricula = cols }
        val estado = rememberLazyGridState()
        LaunchedEffect(e.indiceSel, lista.size) { if (lista.isNotEmpty()) estado.animateScrollToItem((e.indiceSel - cols).coerceAtLeast(0)) }
        if (lista.isEmpty()) VacioEstante(e)
        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            state = estado,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = esc.d(12), end = esc.d(12), top = esc.d(14), bottom = esc.d(14)),
            horizontalArrangement = Arrangement.spacedBy(esc.d(12)),
            verticalArrangement = Arrangement.spacedBy(esc.d(14)),
        ) {
            itemsIndexed(lista, key = { _, j -> j.clave }) { i, j ->
                Box(contentAlignment = Alignment.Center) {
                    Celda(j, i == e.indiceSel, anchoCelda, altoCelda) {
                        if (i == e.indiceSel) Diptico.jugar() else Diptico.elegirJuego(j.clave)
                    }
                }
            }
        }
    }
}

/** Los juegos en una lista con su carátula chica, consola y tiempo jugado. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListaJuegos(e: Estado, modifier: Modifier = Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val t = LocalTamJuegos.current
    val lista = e.visibles
    val estado = rememberLazyListState()
    SideEffect { Teclas.columnasCuadricula = 1 }
    LaunchedEffect(e.indiceSel, lista.size) { if (lista.isNotEmpty()) estado.animateScrollToItem((e.indiceSel - 2).coerceAtLeast(0)) }
    Box(modifier) {
        if (lista.isEmpty()) VacioEstante(e)
        LazyColumn(
            state = estado, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = esc.d(12), vertical = esc.d(10)),
            verticalArrangement = Arrangement.spacedBy(esc.d(6)),
        ) {
            itemsIndexed(lista, key = { _, j -> j.clave }) { i, j ->
                val elegido = i == e.indiceSel
                val fondo by animateColorAsState(if (elegido) Uva else p.superficie.copy(alpha = 0.55f), label = "fila")
                val c = Consolas.porId(j.consolaId)
                val d = e.datos[j.clave]
                Row(
                    Modifier.fillMaxWidth().height(esc.d(58f * t)).clip(RoundedCornerShape(esc.d(16))).background(fondo)
                        .combinedClickable(
                            interactionSource = null, indication = null,
                            onLongClick = { Diptico.elegirJuego(j.clave); Diptico.elegirPantalla("juego") },
                        ) { if (elegido) Diptico.jugar() else Diptico.elegirJuego(j.clave) }
                        .padding(horizontal = esc.d(8)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (c != null) {
                        val base = cajaDe(c)
                        CajaJuego(j, c, escala = minOf(44f * t / base.alto, 70f / base.ancho))
                    }
                    Spacer(Modifier.width(esc.d(12)))
                    Column(Modifier.weight(1f)) {
                        Texto(j.titulo, 16f, color = if (elegido) SobreUva else p.tinta, lineas = 1)
                        Texto(
                            listOfNotNull(c?.nombre, d?.tiempo?.takeIf { it > 0 }?.let { formatoTiempo(it) }).joinToString(" · "),
                            12.5f, color = if (elegido) SobreUva.copy(alpha = 0.85f) else p.tenue, familia = Nunito, peso = FontWeight.Bold, lineas = 1,
                        )
                    }
                    if (d?.favorito == true) IconoDiptico(Icono.CORAZON, if (elegido) SobreUva else Coral, 16f, acento = if (elegido) SobreUva else Coral)
                }
            }
        }
    }
}

@Composable
private fun VacioEstante(e: Estado) {
    val esc = LocalEscala.current
    Column(Modifier.fillMaxSize().padding(esc.d(20)), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Texto(
            when {
                e.escaneando -> "Buscando tus juegos…"
                e.busqueda.isNotBlank() -> "Nada con \"${e.busqueda}\""
                e.filtro != "todos" -> "No hay juegos en \"${Diptico.nombreFiltro(e.filtro)}\""
                else -> "Aquí aparecerán tus juegos"
            },
            17f,
        )
        if (e.busqueda.isNotBlank() || e.filtro != "todos") Detalle("X quita la búsqueda y los filtros", 14f)
    }
}

// ---------- Buscar ----------

/** Buscar por nombre, con filtros (L1/R1) y orden (L2/R2). Los resultados se eligen con la cruceta. */
@Composable
fun PanelBuscar(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Column(Modifier.fillMaxSize().padding(start = esc.d(14), end = esc.d(14), top = esc.d(12), bottom = esc.d(10))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).height(esc.d(46)).clip(RoundedCornerShape(50)).background(p.superficie)
                        .border(esc.d(2), Uva.copy(alpha = 0.5f), RoundedCornerShape(50)).padding(horizontal = esc.d(14)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconoDiptico(Icono.BUSCAR, p.tenue, 20f, acento = Uva)
                    Spacer(Modifier.width(esc.d(10)))
                    Box(Modifier.weight(1f)) {
                        BasicTextField(
                            value = e.busqueda,
                            onValueChange = { Diptico.cambiarBusqueda(it) },
                            singleLine = true,
                            textStyle = TextStyle(color = p.tinta, fontSize = esc.s(17), fontFamily = Fredoka, fontWeight = FontWeight.Medium),
                            cursorBrush = SolidColor(Uva),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { }),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { campo ->
                                if (e.busqueda.isEmpty()) Texto("Toca aquí y escribe un nombre", 16f, color = p.tenue, peso = FontWeight.Medium)
                                campo()
                            },
                        )
                    }
                    if (e.busqueda.isNotEmpty()) {
                        Box(Modifier.size(esc.d(28)).clip(CircleShape).clickable { Diptico.cambiarBusqueda("") }, contentAlignment = Alignment.Center) {
                            IconoDiptico(Icono.CERRAR, p.tenue, 14f)
                        }
                    }
                }
                Spacer(Modifier.width(esc.d(10)))
                Detalle("${e.visibles.size}", 15f, color = p.tinta)
            }
            Spacer(Modifier.height(esc.d(10)))
            Chips(Diptico.FILTROS, e.filtro, { Diptico.nombreFiltro(it) }) { Diptico.elegirFiltro(it) }
            Spacer(Modifier.height(esc.d(6)))
            Chips(Diptico.ORDENES, e.orden, { Diptico.nombreOrden(it) }) { Diptico.elegirOrden(it) }
            Spacer(Modifier.height(esc.d(8)))
            val lista = e.visibles
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val anchoU = maxWidth.value / esc.factor
                val cols = ((anchoU - 8f) / 92f).toInt().coerceAtLeast(3)
                SideEffect { Teclas.columnasBuscar = cols }
                val estado = rememberLazyGridState()
                LaunchedEffect(e.indice) { if (lista.isNotEmpty()) estado.mostrar(e.indice) }
                if (lista.isEmpty()) VacioEstante(e)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols), state = estado, modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(esc.d(8)), verticalArrangement = Arrangement.spacedBy(esc.d(10)),
                ) {
                    itemsIndexed(lista, key = { _, j -> j.clave }) { i, j ->
                        Box(contentAlignment = Alignment.Center) {
                            Celda(j, i == e.indice, 80f, 92f) {
                                Diptico.actualizar { it.copy(indice = i) }
                                Diptico.aceptar()
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("L1 R1" to "Mostrar", "L2 R2" to "Orden", "X" to "Limpiar", "B" to "Cerrar")
        }
    }
}

@Composable
private fun Chips(opciones: List<String>, actual: String, nombre: (String) -> String, alElegir: (String) -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    LazyRow(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
        items(opciones) { o ->
            val activo = o == actual
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(if (activo) Uva else p.superficie).clickable { alElegir(o) }
                    .padding(horizontal = esc.d(12), vertical = esc.d(6)),
            ) { Texto(nombre(o), 13f, color = if (activo) SobreUva else p.tinta, lineas = 1) }
        }
    }
}

// ---------- Ventana del juego (Y o mantener A) ----------

/** Ventana flotante con las acciones del juego. El fondo se difumina detrás. */
@Composable
fun VentanaJuego(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val j = e.seleccionado ?: return
    val c = Consolas.porId(j.consolaId)
    val opciones = remember(e) { Diptico.opcionesRapido() }
    val n = Diptico.PRIMARIAS_RAPIDO
    Velo {
        val color = colorDe(j.clave)
        Column(
            Modifier.widthIn(max = esc.d(560)).fillMaxHeight(0.94f).clip(RoundedCornerShape(esc.d(26)))
                .background(Brush.verticalGradient(listOf(lerpColor(p.superficie, color, 0.22f), p.superficie)))
                .border(esc.d(1), p.linea, RoundedCornerShape(esc.d(26)))
                .sinCerrar().padding(esc.d(14)),
        ) {
            // Encabezado compacto
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (c != null) CajaJuego(j, c, escala = 58f / cajaDe(c).alto)
                Spacer(Modifier.width(esc.d(12)))
                Column(Modifier.weight(1f)) {
                    Etiqueta(c?.bonito ?: "", Coral, 11f)
                    Texto(j.titulo, 19f, lineas = 1)
                    val d = e.datos[j.clave]
                    Detalle(
                        listOfNotNull(
                            d?.tiempo?.takeIf { it > 0 }?.let { "${formatoTiempo(it)} jugadas" } ?: "Sin jugar",
                            if (d?.favorito == true) "♥ Favorito" else null,
                        ).joinToString(" · "),
                        12.5f, lineas = 1,
                    )
                }
            }
            Spacer(Modifier.height(esc.d(12)))
            // Fila de acciones grandes
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                opciones.take(n).forEachIndexed { i, o -> Mosaico(o, i == e.indice, Modifier.weight(1f)) { Diptico.elegirIndice(i) } }
            }
            Spacer(Modifier.height(esc.d(10)))
            Etiqueta("Ajustes de este juego", p.tenue, 11f)
            Spacer(Modifier.height(esc.d(6)))
            // Lista que se desliza sola siguiendo la selección (y con el dedo).
            val lista = rememberLazyListState()
            LaunchedEffect(e.indice) {
                if (e.indice >= n) {
                    val k = e.indice - n
                    val vis = lista.layoutInfo.visibleItemsInfo
                    if (vis.isEmpty() || k <= vis.first().index || k >= vis.last().index) lista.animateScrollToItem((k - 1).coerceAtLeast(0))
                } else lista.scrollToItem(0)
            }
            LazyColumn(state = lista, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
                itemsIndexed(opciones.drop(n)) { k, o ->
                    val i = k + n
                    TarjetaOpcion(o, i == e.indice, alto = 46f) { Diptico.elegirIndice(i) }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("A" to "Elegir", "Cruceta" to "Moverte", "B" to "Cerrar")
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)

/** Una acción en forma de mosaico: ícono grande arriba y nombre abajo. */
@Composable
private fun Mosaico(o: cl.nikalbazu.diptico.estado.Opcion, activo: Boolean, modifier: Modifier, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val fondo by animateColorAsState(if (activo) Uva else p.fondo2.copy(alpha = 0.7f), label = "mosaico")
    Column(
        modifier.height(esc.d(70)).clip(RoundedCornerShape(esc.d(18)))
            .then(if (activo) Modifier.fondoBoton(fondo) else Modifier.background(fondo))
            .clickable { alPulsar() }.padding(horizontal = esc.d(8), vertical = esc.d(8)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        o.icono?.let { IconoDiptico(it, if (activo) SobreUva else p.tinta, 24f, acento = if (activo) SobreUva else Coral) }
        Spacer(Modifier.height(esc.d(5)))
        Texto(o.titulo, 12.5f, color = if (activo) SobreUva else p.tinta, lineas = 1)
    }
}

// ---------- Centro de control ----------

/**
 * Centro de control: una cortina translúcida que baja desde arriba sobre lo de atrás difuminado.
 * Accesos de Android, las apps de la consola (AYN/Odin/Thor), tu música y tus notificaciones.
 */
@Composable
fun PanelCentro(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val avisos by Avisos.lista.collectAsState()
    val acciones = remember { Diptico.opcionesCentro() }
    val bajar = remember { Animatable(-1f) }
    // Baja con un rebote suave, como una cortina elástica.
    LaunchedEffect(Unit) { bajar.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 260f)) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { Diptico.cerrarPanel() }) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val alto = maxHeight
            Column(
                Modifier.fillMaxWidth().fillMaxHeight(0.94f)
                    .graphicsLayer { translationY = bajar.value * alto.toPx() }
                    .clip(RoundedCornerShape(bottomStart = esc.d(28), bottomEnd = esc.d(28)))
                    .background(Brush.verticalGradient(listOf(p.fondo2.copy(alpha = if (Estilo.efectosCompletos) 0.93f else 0.98f), p.fondo2.copy(alpha = if (Estilo.efectosCompletos) 0.84f else 0.96f))))
                    .sinCerrar()
                    .padding(start = esc.d(14), end = esc.d(14), top = esc.d(10), bottom = esc.d(8)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Texto(rememberHora(), 24f)
                    Spacer(Modifier.width(esc.d(10)))
                    Detalle(cl.nikalbazu.diptico.estado.Idioma.fecha("EEEEdMMMM"), 13.5f, lineas = 1, modifier = Modifier.weight(1f))
                    IconoDiptico(Icono.CAMPANA, p.tinta, 20f, acento = Coral)
                    Spacer(Modifier.width(esc.d(6)))
                    Texto(if (avisos.isEmpty()) "Sin avisos" else "${avisos.size}", 13f, color = p.tenue)
                }
                Spacer(Modifier.height(esc.d(8)))
                // Accesos en grilla
                val filas = acciones.chunked(Diptico.COLUMNAS_CENTRO)
                Column(verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    filas.forEachIndexed { f, fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                            for (c in 0 until Diptico.COLUMNAS_CENTRO) {
                                val i = f * Diptico.COLUMNAS_CENTRO + c
                                val o = fila.getOrNull(c)
                                if (o == null) { Spacer(Modifier.weight(1f)); continue }
                                // Cada acceso cae con su propio rebote, uno tras otro.
                                val rebote = remember { Animatable(0f) }
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(90L + i * 35L)
                                    rebote.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 380f))
                                }
                                AccesoCentro(
                                    o, i == e.indice,
                                    Modifier.weight(1f).graphicsLayer {
                                        val r = rebote.value
                                        alpha = r.coerceIn(0f, 1f)
                                        translationY = (1f - r) * -60f * density
                                        scaleX = 0.7f + 0.3f * r; scaleY = 0.7f + 0.3f * r
                                    },
                                ) { Diptico.elegirIndice(i) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(esc.d(10)))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Etiqueta("Notificaciones", p.tenue, 12f, Modifier.weight(1f))
                    if (avisos.isNotEmpty()) {
                        Box(Modifier.clip(RoundedCornerShape(50)).clickable { Avisos.descartarTodas() }.padding(horizontal = esc.d(8), vertical = esc.d(3))) {
                            Texto("Borrar todas", 12.5f, color = Coral)
                        }
                    }
                }
                Spacer(Modifier.height(esc.d(4)))
                if (!cl.nikalbazu.diptico.audio.Medios.tienePermiso(androidx.compose.ui.platform.LocalContext.current)) {
                    Detalle("Para ver tus notificaciones aquí, activa el permiso en Menú › Consola.", 14f)
                } else if (avisos.isEmpty()) {
                    Detalle("No tienes notificaciones.", 14f)
                }
                val lista = rememberLazyListState()
                LaunchedEffect(e.indice) { if (e.indice >= acciones.size) lista.animateScrollToItem((e.indice - acciones.size - 1).coerceAtLeast(0)) }
                LazyColumn(state = lista, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    itemsIndexed(avisos, key = { _, a -> a.clave }) { k, a ->
                        val activo = e.indice == acciones.size + k
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(16))).background(if (activo) Uva else p.superficie.copy(alpha = 0.7f))
                                .clickable { Diptico.abrirAviso(a) }.padding(esc.d(10)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconoApp(a.paquete, 34f)
                            Spacer(Modifier.width(esc.d(10)))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Texto(a.app, 11.5f, color = if (activo) SobreUva.copy(alpha = 0.85f) else p.tenue, familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1, modifier = Modifier.weight(1f))
                                    Texto(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(a.tiempo)), 11.5f, color = if (activo) SobreUva.copy(alpha = 0.85f) else p.tenue)
                                }
                                if (a.titulo.isNotBlank()) Texto(a.titulo, 14.5f, color = if (activo) SobreUva else p.tinta, lineas = 1)
                                if (a.texto.isNotBlank()) Texto(a.texto, 13f, color = if (activo) SobreUva.copy(alpha = 0.85f) else p.tenue, familia = Nunito, peso = FontWeight.Bold, lineas = 2)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(esc.d(4)))
                // La manija de la cortina
                Box(Modifier.align(Alignment.CenterHorizontally).size(esc.d(46), esc.d(5)).clip(RoundedCornerShape(50)).background(p.tenue.copy(alpha = 0.5f)))
                Spacer(Modifier.height(esc.d(4)))
                Pistas("A" to "Abrir", "X" to "Descartar", "Y" to "Borrar todas", "B" to "Cerrar")
            }
        }
    }
}

@Composable
private fun AccesoCentro(o: Opcion, activo: Boolean, modifier: Modifier, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val escala by animateFloatAsState(if (activo) 1.06f else 1f, spring(stiffness = 700f), label = "acceso")
    Column(
        modifier.height(esc.d(62)).graphicsLayer { scaleX = escala; scaleY = escala }.clip(RoundedCornerShape(esc.d(18)))
            .then(if (activo) Modifier.fondoBoton(Uva) else Modifier.background(p.superficie.copy(alpha = 0.75f)))
            .clickable { alPulsar() },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        if (o.paquete != null) IconoApp(o.paquete, 26f)
        else o.icono?.let { IconoDiptico(it, if (activo) SobreUva else p.tinta, 21f, acento = if (activo) SobreUva else Coral) }
        Spacer(Modifier.height(esc.d(4)))
        Texto(o.titulo, 11f, color = if (activo) SobreUva else p.tinta, lineas = 1)
    }
}

// ---------- Carpetas de juegos ----------

/** Todas tus carpetas de juegos: agregar, quitar y elegir la consola de cada una. */
@Composable
fun PanelCarpetas(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val lista = rememberLazyListState()
    LaunchedEffect(e.indice, e.consolaPara) { lista.mostrar(e.indice) }
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Column(Modifier.fillMaxSize().padding(start = esc.d(14), end = esc.d(14), top = esc.d(12), bottom = esc.d(10))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.CARPETA, p.tinta, 22f, acento = Coral)
                Spacer(Modifier.width(esc.d(10)))
                Texto(
                    if (e.consolaPara >= 0) "¿De qué consola es?" else "Carpetas de juegos", 21f,
                    lineas = 1, modifier = Modifier.weight(1f),
                )
            }
            Detalle(
                if (e.consolaPara >= 0) e.carpetas.getOrNull(e.consolaPara)?.let { Diptico.nombreCarpeta(it) } ?: ""
                else "Agrega todas las que quieras: memoria interna, SD… A cambia la consola, Y la quita.",
                13.5f, lineas = 2,
            )
            Spacer(Modifier.height(esc.d(8)))
            LazyColumn(state = lista, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                if (e.consolaPara >= 0) {
                    val opciones = listOf<Pair<String, cl.nikalbazu.diptico.data.Consola?>>("Reconocer sola (automático)" to null) + Diptico.consolasElegibles().map { it.bonito to it }
                    itemsIndexed(opciones) { i, (nombre, c) ->
                        val activo = i == e.indice
                        Row(
                            Modifier.fillMaxWidth().height(esc.d(48)).clip(RoundedCornerShape(esc.d(14))).background(if (activo) Uva else p.superficie.copy(alpha = 0.75f))
                                .clickable { Diptico.elegirIndice(i) }.padding(horizontal = esc.d(12)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconoConsola(c, 26f, p.superficie)
                            Spacer(Modifier.width(esc.d(10)))
                            Texto(nombre, 15f, color = if (activo) SobreUva else p.tinta, lineas = 1)
                        }
                    }
                } else {
                    itemsIndexed(e.carpetas) { i, carpeta ->
                        val activo = i == e.indice
                        val c = carpeta.consola?.let { Consolas.porId(it) }
                        Row(
                            Modifier.fillMaxWidth().height(esc.d(58)).clip(RoundedCornerShape(esc.d(16))).background(if (activo) Uva else p.superficie.copy(alpha = 0.75f))
                                .clickable { Diptico.elegirIndice(i) }.padding(horizontal = esc.d(12)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconoDiptico(Icono.CARPETA, if (activo) SobreUva else p.tinta, 24f, acento = if (activo) SobreUva else Coral)
                            Spacer(Modifier.width(esc.d(12)))
                            Column(Modifier.weight(1f)) {
                                Texto(Diptico.nombreCarpeta(carpeta), 15.5f, color = if (activo) SobreUva else p.tinta, lineas = 1)
                                Texto(c?.bonito ?: "Consola automática", 12.5f, color = if (activo) SobreUva.copy(alpha = 0.85f) else p.tenue, familia = Nunito, peso = FontWeight.Bold, lineas = 1)
                            }
                            Box(Modifier.size(esc.d(30)).clip(CircleShape).clickable { Diptico.actualizar { it.copy(indice = i) }; Diptico.quitarCarpeta() }, contentAlignment = Alignment.Center) {
                                IconoDiptico(Icono.BORRAR, if (activo) SobreUva else p.tenue, 18f)
                            }
                        }
                    }
                    item {
                        val activo = e.indice == e.carpetas.size
                        Row(
                            Modifier.fillMaxWidth().height(esc.d(52)).clip(RoundedCornerShape(esc.d(16)))
                                .background(if (activo) Uva else Color.Transparent)
                                .border(esc.d(2), if (activo) Uva else p.linea, RoundedCornerShape(esc.d(16)))
                                .clickable { Diptico.elegirIndice(e.carpetas.size) }.padding(horizontal = esc.d(12)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Texto("+", 22f, color = if (activo) SobreUva else Coral)
                            Spacer(Modifier.width(esc.d(12)))
                            Texto("Agregar carpeta", 15.5f, color = if (activo) SobreUva else p.tinta)
                        }
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("A" to if (e.consolaPara >= 0) "Elegir" else "Consola", "Y" to "Quitar", "B" to "Volver")
        }
    }
}

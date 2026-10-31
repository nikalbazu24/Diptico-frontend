package cl.nikalbazu.diptico.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
            onLongClick = { Diptico.elegirJuego(j.clave); Diptico.abrirRapido() },
        ) { alElegir() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(esc.d(anchoCelda), esc.d(altoCelda)), contentAlignment = Alignment.BottomCenter) {
            val forma = RoundedCornerShape(esc.d(base.radio * k))
            Box(
                Modifier.graphicsLayer { scaleX = tam; scaleY = tam }
                    .then(if (elegido) Modifier.shadow(esc.d(14), forma, ambientColor = color, spotColor = color).border(esc.d(3), if (p == PaletaOscura) Color.White else Uva, forma) else Modifier),
            ) { CajaJuego(j, c, escala = k) }
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
                            onLongClick = { Diptico.elegirJuego(j.clave); Diptico.abrirRapido() },
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
                LaunchedEffect(e.indice) { if (lista.isNotEmpty()) estado.animateScrollToItem((e.indice - cols).coerceAtLeast(0)) }
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
    val opciones = Diptico.opcionesRapido()
    Velo {
        BoxWithConstraints(Modifier.fillMaxSize().padding(esc.d(14)), contentAlignment = Alignment.Center) {
            val anchoU = maxWidth.value / esc.factor
            val cols = if (anchoU < 560f) 3 else 4
            SideEffect { Diptico.columnasVentana = cols }
            val color = colorDe(j.clave)
            Column(
                Modifier.widthIn(max = esc.d(640)).clip(RoundedCornerShape(esc.d(28)))
                    .background(Brush.verticalGradient(listOf(lerpColor(p.superficie, color, 0.25f), p.superficie)))
                    .border(esc.d(1), p.linea, RoundedCornerShape(esc.d(28)))
                    .sinCerrar().padding(esc.d(16)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (c != null) {
                        val base = cajaDe(c)
                        CajaJuego(j, c, escala = 76f / base.alto)
                    }
                    Spacer(Modifier.width(esc.d(14)))
                    Column(Modifier.weight(1f)) {
                        Etiqueta(c?.bonito ?: "", Coral, 11.5f)
                        Texto(j.titulo, 22f, lineas = 2, interlineado = 25f)
                        val d = e.datos[j.clave]
                        Detalle(
                            listOfNotNull(
                                d?.tiempo?.takeIf { it > 0 }?.let { "${formatoTiempo(it)} jugadas" } ?: "Sin jugar",
                                if (d?.favorito == true) "♥ Favorito" else null,
                            ).joinToString(" · "),
                            13.5f, lineas = 1,
                        )
                    }
                }
                Spacer(Modifier.height(esc.d(14)))
                opciones.chunked(cols).forEachIndexed { fila, grupo ->
                    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                        grupo.forEachIndexed { col, o ->
                            val i = fila * cols + col
                            Mosaico(o, i == e.indice, Modifier.weight(1f)) { Diptico.elegirIndice(i) }
                        }
                        repeat(cols - grupo.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(esc.d(8)))
                }
                Pistas("A" to "Elegir", "B" to "Cerrar")
            }
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
    val tam by animateFloatAsState(if (activo) 1.04f else 1f, spring(dampingRatio = 0.55f), label = "mosaicoTam")
    Column(
        modifier.height(esc.d(84)).graphicsLayer { scaleX = tam; scaleY = tam }.clip(RoundedCornerShape(esc.d(18))).background(fondo)
            .clickable { alPulsar() }.padding(horizontal = esc.d(8), vertical = esc.d(8)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        o.icono?.let { IconoDiptico(it, if (activo) SobreUva else p.tinta, 26f, acento = if (activo) SobreUva else Coral) }
        Spacer(Modifier.height(esc.d(6)))
        Texto(o.titulo, 13.5f, color = if (activo) SobreUva else p.tinta, lineas = 1)
        if (o.valor.isNotEmpty()) Texto(o.valor, 11f, color = if (activo) SobreUva.copy(alpha = 0.85f) else p.tenue, familia = Nunito, peso = FontWeight.Bold, lineas = 1)
    }
}

// ---------- Centro de control ----------

/** Accesos rápidos de Android y las notificaciones de tus apps. */
@Composable
fun PanelCentro(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val avisos by Avisos.lista.collectAsState()
    val acciones = Diptico.opcionesCentro()
    Box(Modifier.fillMaxSize().background(p.fondo2.copy(alpha = 0.97f)).sinCerrar()) {
        Column(Modifier.fillMaxSize().padding(start = esc.d(14), end = esc.d(14), top = esc.d(12), bottom = esc.d(10))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.CAMPANA, p.tinta, 22f, acento = Coral)
                Spacer(Modifier.width(esc.d(10)))
                Texto("Centro de control", 21f, modifier = Modifier.weight(1f))
                Texto(rememberHora(), 18f, color = p.tenue)
            }
            Spacer(Modifier.height(esc.d(10)))
            // Accesos rápidos
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                acciones.forEachIndexed { i, o ->
                    val activo = i == e.indice
                    Column(
                        Modifier.weight(1f).height(esc.d(66)).clip(RoundedCornerShape(esc.d(16))).background(if (activo) Uva else p.superficie)
                            .clickable { Diptico.elegirIndice(i) },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        o.icono?.let { IconoDiptico(it, if (activo) SobreUva else p.tinta, 22f, acento = if (activo) SobreUva else Coral) }
                        Spacer(Modifier.height(esc.d(4)))
                        Texto(o.titulo, 11.5f, color = if (activo) SobreUva else p.tinta, lineas = 1)
                    }
                }
            }
            Spacer(Modifier.height(esc.d(12)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Etiqueta("Notificaciones", p.tenue, 12f, Modifier.weight(1f))
                if (avisos.isNotEmpty()) {
                    Box(Modifier.clip(RoundedCornerShape(50)).clickable { Avisos.descartarTodas() }.padding(horizontal = esc.d(8), vertical = esc.d(3))) {
                        Texto("Borrar todas", 12.5f, color = Coral)
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
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
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(16))).background(if (activo) Uva else p.superficie.copy(alpha = 0.8f))
                            .clickable { Diptico.abrirAviso(a) }.padding(esc.d(10)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val icono = rememberImagen(android.net.Uri.parse("app://${a.paquete}"), 96)
                        Box(Modifier.size(esc.d(34)).clip(RoundedCornerShape(esc.d(10))).background(p.fondo), contentAlignment = Alignment.Center) {
                            if (icono != null) Image(icono, null, Modifier.fillMaxSize())
                        }
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
            Spacer(Modifier.height(esc.d(6)))
            Pistas("A" to "Abrir", "X" to "Descartar", "Y" to "Borrar todas", "B" to "Cerrar")
        }
    }
}

// ---------- Carpetas de juegos ----------

/** Todas tus carpetas de juegos: agregar, quitar y elegir la consola de cada una. */
@Composable
fun PanelCarpetas(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val lista = rememberLazyListState()
    LaunchedEffect(e.indice, e.consolaPara) { lista.animateScrollToItem((e.indice - 2).coerceAtLeast(0)) }
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

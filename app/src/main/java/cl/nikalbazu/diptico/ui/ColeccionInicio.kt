package cl.nikalbazu.diptico.ui

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.key
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/*
 * Colección viva · inicio dentro de una consola (bocetos 1, 2, 2b, 3 y 3b; docs/coleccion-viva.md).
 * Todas las medidas son píxeles del boceto: el lienzo las escala a cada pantalla.
 */

/** ¿Va el diseño nuevo en esta pantalla? Solo en el inicio de siempre (sin partida ni paneles que lo reemplacen). */
fun coleccionVivaActiva(e: Estado): Boolean = e.extra.coleccionViva && e.configurado && e.sesion == null && e.inicio != "widgets_viejo"

// ———————————————————————————— datos que comparten las pantallas ————————————————————————————

/** Un resumen corto del juego (de su ficha guardada), sin pedir nada a internet. */
@Composable
private fun resumenJuego(j: Juego, c: Consola?): String {
    val ctx = LocalContext.current
    val r by produceState("", j.clave) {
        value = withContext(Dispatchers.IO) {
            runCatching { cl.nikalbazu.diptico.data.Fichas.guardada(ctx, j.titulo, c?.bonito ?: "")?.resumen }.getOrNull().orEmpty()
        }
    }
    // Una o dos frases, para que no tape nada.
    return r.split(Regex("(?<=[.!?])\\s+")).take(2).joinToString(" ").take(200)
}

private fun avance(e: Estado, j: Juego): Int? {
    val d = e.extra.duraciones[j.clave]?.takeIf { it.tiene && it.principal > 0f } ?: return null
    val jugado = (e.datos[j.clave]?.tiempo ?: 0L) / 3600f
    return ((jugado / d.principal) * 100).toInt().coerceIn(0, 100).takeIf { it > 0 }
}

private fun chipsJuego(e: Estado, j: Juego): List<String> = buildList {
    val d = e.datos[j.clave]
    if ((d?.tiempo ?: 0L) > 0) add("${formatoTiempo(d!!.tiempo)} jugadas")
    avance(e, j)?.let { add("Vas en el $it %") }
    if ((d?.ultimo ?: 0L) > 1) add(haceCuanto(d!!.ultimo))
    if (d?.favorito == true) add("♥ Favorito")
}

/** Las imágenes del carrete: portada, imagen grande y las caras guardadas de la caja. */
private fun imagenesCarrete(e: Estado, j: Juego): List<Pair<String, Uri>> = buildList {
    j.portada?.let { add("Portada" to it) }
    j.arte?.let { add("Imagen grande" to it) }
    e.extra.caras[j.clave]?.let { g ->
        g.frente?.let { add("Frente de la caja" to Uri.parse(it)) }
        g.reverso?.let { add("Reverso" to Uri.parse(it)) }
        g.lomo?.let { add("Lomo" to Uri.parse(it)) }
    }
    j.logo?.let { add("Logo" to it) }
}.distinctBy { it.second }

private fun fraseDippo(e: Estado, j: Juego?): String {
    if (j == null) return "Elige un juego y te cuento lo que sé."
    val d = e.datos[j.clave]
    val dur = e.extra.duraciones[j.clave]
    return when {
        !d?.paso.isNullOrBlank() -> "Ibas en: ${d!!.paso}"
        dur?.tiene == true && (d?.tiempo ?: 0L) > 0 -> CuantoDura.teQueda(dur, d!!.tiempo) ?: "¿Seguimos donde quedaste?"
        (d?.tiempo ?: 0L) == 0L -> "Nunca lo has jugado conmigo. ¿Lo probamos?"
        else -> "¿Seguimos donde quedaste?"
    }
}

// ———————————————————————————— 1 · Thor arriba ————————————————————————————

@Composable
fun ColeccionArriba(e: Estado) {
    val j = e.seleccionado
    val c = j?.let { Consolas.porId(it.consolaId) }
    LienzoBoceto(1920f, 1080f, fondo = LocalColeccion.current.fondo) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val girada = e.extra.cajaGirada
        // Arte del juego a pantalla completa (si no hay, un degradado con el color de la consola).
        if (j != null) {
            androidx.compose.animation.Crossfade(j.clave, animationSpec = androidx.compose.animation.core.tween(320), label = "arte") { _ ->
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf((c?.color ?: col.acento).copy(alpha = .55f), col.fondo)))) {
                    ArteJuego(j, preferirArte = true, maxLado = 1280, modifier = Modifier.fillMaxSize())
                }
            }
        }
        Box(Modifier.fillMaxSize().veloColeccion(col.fondo))

        // Barra: consola a la izquierda; hora, batería y perfil a la derecha.
        Row(Modifier.fillMaxWidth().padding(start = esc.d(96), end = esc.d(96), top = esc.d(44)), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(18))) {
            PildoraVidrio(64f, onClick = { Diptico.volverASistemas() }) {
                Box(Modifier.size(esc.d(16)).clip(CircleShape).background(c?.color ?: col.acento))
                TB(c?.let { it.nombreLargo } ?: "Tus juegos", 28f, titulo = true, peso = FontWeight.SemiBold)
                if (e.visibles.isNotEmpty()) TB("${e.indiceSel + 1} de ${e.visibles.size}", 22f, color = col.sub, traducir = false)
            }
            Spacer(Modifier.weight(1f))
            HoraBateria()
            BotonPerfil(72f)
        }

        if (j == null) {
            Column(Modifier.align(Alignment.CenterStart).padding(start = esc.d(96))) {
                TB("Aún no hay juegos aquí", 72f, titulo = true)
                TB("Elige tu carpeta de juegos en Ajustes › Biblioteca.", 28f, color = col.sub)
            }
            return@LienzoBoceto
        }

        val info by animateFloatAsState(if (girada) 0f else 1f, tween(duracionMovimiento(320)), label = "info")
        // Información del juego.
        Column(Modifier.offset(esc.d(96), esc.d(210)).width(esc.d(820)).alpha(info), verticalArrangement = Arrangement.spacedBy(esc.d(22))) {
            TB((c?.bonito ?: "").uppercase(), 24f, color = lerpClaro(c?.color ?: col.acento), peso = FontWeight.ExtraBold, espaciado = 3.4f, traducir = false)
            val logo = rememberImagen(j.logo, 900)
            if (logo != null) Image(logo, j.titulo, contentScale = ContentScale.Fit, alignment = Alignment.CenterStart,
                modifier = Modifier.heightIn(max = esc.d(240)).widthIn(max = esc.d(760)))
            else TB(j.titulo, 118f, titulo = true, lineas = 2, interlineado = 116f, sombra = true, traducir = false)
            val resumen = resumenJuego(j, c)
            if (resumen.isNotBlank()) TB(resumen, 28f, color = Color(0xFFD3D5EA).siOscuro(col), peso = FontWeight.SemiBold, lineas = 3, interlineado = 39f, modifier = Modifier.widthIn(max = esc.d(720)), traducir = false)
            val chips = chipsJuego(e, j).ifEmpty { listOf("Aún sin jugar con Díptico") }
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(12))) { chips.take(3).forEach { ChipDato(it) } }
            val pct = avance(e, j)
            val paso = e.datos[j.clave]?.paso.orEmpty()
            if (pct != null || paso.isNotBlank()) Column(Modifier.widthIn(max = esc.d(640)), verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
                if (pct != null) Box(Modifier.fillMaxWidth().height(esc.d(8)).clip(RoundedCornerShape(50)).background(col.texto.copy(alpha = .14f))) {
                    Box(Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF6B4EFF), c?.color ?: Color(0xFF3B6BFF)))))
                }
                if (paso.isNotBlank()) TB("Tu próximo paso: $paso", 24f, color = Color(0xFFC7C9E0).siOscuro(col), peso = FontWeight.SemiBold, lineas = 2, traducir = false,
                    modifier = Modifier.clickable { Diptico.abrirPaso() })
            }
        }
        // Acciones.
        Row(Modifier.align(Alignment.BottomStart).padding(start = esc.d(96), bottom = esc.d(92)).alpha(info), horizontalArrangement = Arrangement.spacedBy(esc.d(18))) {
            BotonTecla("A", if ((e.datos[j.clave]?.tiempo ?: 0L) > 0) "Continuar" else "Jugar", { Diptico.jugar() }, principal = true, letra = 32f)
            BotonTecla("X", "Ficha", { Diptico.abrirFicha() })
            BotonTecla("Y", "Editar caja", { Diptico.abrirCajaJuego(j.clave) })
        }
        // La caja 3D: girada 24°, sin pedestal. Al tocarla viaja al centro, crece y muestra el reverso.
        CajaVitrinaCV(e, j, c ?: return@LienzoBoceto, x = 1240f, y = 210f, ancho = 400f, alto = 560f, centroX = 960f - 200f, girada = girada)
        // DIPPO: una línea discreta abajo a la derecha (no tapa nada).
        if (e.extra.dippoInicio && !girada) DippoFrase(fraseDippo(e, j), Modifier.align(Alignment.BottomEnd).padding(end = esc.d(96), bottom = esc.d(104)))
        if (girada) Row(Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(60)), horizontalArrangement = Arrangement.spacedBy(esc.d(40))) {
            PistaTecla("R3", "Volver a girar"); PistaTecla("B", "Volver")
        }
    }
}

/** Un color más claro del color de la consola, para el «GÉNERO · AÑO» (como #9DB6FF para el azul de PS2). */
private fun lerpClaro(c: Color): Color = androidx.compose.ui.graphics.lerp(c, Color.White, 0.55f)

private fun Color.siOscuro(col: ColoresColeccion): Color = if (col.texto.luminance() > 0.5f) this else col.sub


/**
 * La caja 3D del juego elegido en su lugar del boceto. Con [girada] viaja hacia [centroX], crece ×1.34 y gira 180°
 * (el reverso). Se puede girar con el dedo; al soltar vuelve a su pose.
 */
@Composable
fun CajaVitrinaCV(e: Estado, j: Juego, c: Consola, x: Float, y: Float, ancho: Float, alto: Float, centroX: Float, girada: Boolean, pose: Float = 24f) {
    val esc = LocalEscala.current
    val mov = LocalMovimiento.current
    val t by animateFloatAsState(if (girada) 1f else 0f, tween(if (mov) duracionMovimiento(700) else 0, easing = CurvaPosarse), label = "giro-caja")
    var dedo by remember { mutableStateOf(0f) }
    val dedoAnim by animateFloatAsState(dedo, if (dedo == 0f) androidx.compose.animation.core.spring(0.55f, 220f) else tween(0), label = "dedo")
    val xActual = x + (centroX - x) * t
    val escala = 1f + 0.34f * t
    // Sombra en el piso.
    Box(Modifier.offset(esc.d(xActual - 20f), esc.d(y + alto + 80f)).size(esc.d(ancho * 1.1f * escala), esc.d(60)).graphicsLayer { alpha = 1f - 0.4f * t }
        .background(Brush.radialGradient(listOf(Color.Black.copy(alpha = .6f), Color.Transparent))))
    Box(
        Modifier.offset(esc.d(xActual), esc.d(y)).size(esc.d(ancho), esc.d(alto + 40f))
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .testTag("caja-vitrina")
            .pointerInput(j.clave) {
                detectHorizontalDragGestures(onDragEnd = { dedo = 0f }, onDragCancel = { dedo = 0f }) { ch, dx -> ch.consume(); dedo += dx / 3f }
            }
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { Diptico.girarCaja() },
    ) {
        Caja3D(
            datosCaja(e, j, c), j, c, Modifier.fillMaxSize(),
            giroY = { pose * (1f - t) + 180f * t + dedoAnim },
            giroX = { -3f },
            caras = e.extra.caras[j.clave],
            detalle = true, sombraPiso = false,
        )
    }
}

// ———————————————————————————— 2 · Thor abajo ————————————————————————————

@Composable
fun ColeccionAbajo(e: Estado) {
    LienzoBoceto(1240f, 1080f, fondo = LocalColeccion.current.fondo2) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val c = e.seleccionado?.let { Consolas.porId(it.consolaId) } ?: Consolas.porId(e.consolaSel)
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf((c?.color ?: col.acento).copy(alpha = .22f), Color.Transparent), radius = 900f)))
        // Fila de arriba: DIPPO, buscar, widgets y apps.
        Row(Modifier.fillMaxWidth().padding(start = esc.d(40), end = esc.d(40), top = esc.d(32)).height(esc.d(76)),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
            Box(Modifier.size(esc.d(76)).clip(CircleShape).background(col.texto.copy(alpha = .07f)).border(1.dp, col.borde, CircleShape)
                .clickable { Diptico.abrirAsistente() }, contentAlignment = Alignment.Center) { cl.nikalbazu.diptico.bisa.AvatarBisa(48f * esc.factor) }
            Row(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50)).background(col.texto.copy(alpha = .07f)).border(1.dp, col.borde, RoundedCornerShape(50))
                .clickable { Diptico.abrirBuscar() }.padding(horizontal = esc.d(26)), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                IconoDiptico(Icono.BUSCAR, col.sub, 30f)
                TB("Buscar en tu colección", 26f, color = col.sub, peso = FontWeight.SemiBold)
            }
            BotonRedondo(Icono.APPS, 76f, { Diptico.fijarInicio("widgets") }, descripcion = "Widgets")
            BotonRedondo(Icono.MENU, 76f, { Diptico.abrirApps() }, descripcion = "Apps")
        }
        if (e.extra.cajaGirada && e.seleccionado != null) {
            CarreteCV(e, e.seleccionado!!, Modifier.padding(top = esc.d(136)))
            return@LienzoBoceto
        }
        // Consolas: L1 · píldoras · R1.
        FilaConsolasCV(e, Modifier.offset(esc.d(40), esc.d(136)).width(esc.d(anchoLienzo() - 80f)).height(esc.d(104)))
        // Los juegos de la consola, en la vista elegida.
        Box(Modifier.fillMaxWidth().offset(y = esc.d(260)).height(esc.d(680 - 0f))) {
            when (e.extra.vistaColeccion) {
                "cuadricula" -> RejillaCV(e, columnas = 5, Modifier.fillMaxSize().padding(horizontal = esc.d(40)))
                "lista" -> ListaCV(e, Modifier.fillMaxSize().padding(horizontal = esc.d(40)))
                else -> Column(Modifier.fillMaxSize()) {
                    EstanteCV(e, Modifier.fillMaxWidth().height(esc.d(500)))
                    val j = e.seleccionado
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        TB(j?.let { it.titulo } ?: "", 44f, titulo = true, traducir = false)
                        TB("${e.indiceSel + 1} de ${e.visibles.size} · ◀ ▶ para recorrer · A jugar", 22f, color = col.sub)
                    }
                }
            }
        }
        BarraAbajoCV(e, Modifier.align(Alignment.BottomCenter).padding(start = esc.d(40), end = esc.d(40), bottom = esc.d(36)))
    }
}

/** L1 · las consolas como píldoras con su punto de color (la elegida en blanco) · R1. */
@Composable
fun FilaConsolasCV(e: Estado, modifier: Modifier, alto: Float = 76f) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val ids = e.pestanas
    val lista = rememberLazyListState()
    val sel = ids.indexOf(e.consolaSel).coerceAtLeast(0)
    LaunchedEffect(sel) { lista.animateScrollToItem((sel - 1).coerceAtLeast(0)) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
        TeclaHombro("L1") { Diptico.cambiarConsola(-1) }
        androidx.compose.foundation.lazy.LazyRow(Modifier.weight(1f), state = lista, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
            itemsIndexed(ids) { i, id ->
                val k = Consolas.porId(id)
                val elegida = i == sel
                Row(
                    Modifier.height(esc.d(alto)).clip(RoundedCornerShape(50))
                        .background(if (elegida) col.elegido else col.texto.copy(alpha = .07f))
                        .then(if (elegida) Modifier else Modifier.border(1.dp, col.borde, RoundedCornerShape(50)))
                        .clickable { Diptico.elegirConsola(id) }.padding(horizontal = esc.d(22)),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(12)),
                ) {
                    Box(Modifier.size(esc.d(14)).clip(CircleShape).background(k?.color ?: col.acento))
                    TB(k?.let { it.nombreLargo } ?: "Todos", 23f, color = if (elegida) col.enElegido else col.sub, peso = FontWeight.ExtraBold)
                }
            }
        }
        TeclaHombro("R1") { Diptico.cambiarConsola(1) }
    }
}

@Composable
private fun TeclaHombro(t: String, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Box(Modifier.clip(RoundedCornerShape(esc.d(10))).border(2.dp, col.tenue.copy(alpha = .6f), RoundedCornerShape(esc.d(10))).clickable(onClick = onClick)
        .padding(horizontal = esc.d(10), vertical = esc.d(6))) { TB(t, 20f, color = col.tenue, peso = FontWeight.ExtraBold, traducir = false) }
}

/** El estante 3D: la elegida al centro con anillo blanco; las vecinas más chicas, giradas ±48° mostrando el lomo. */
@Composable
fun EstanteCV(e: Estado, modifier: Modifier, ancho: Float = 250f, alto: Float = 350f, paso: Float = 235f, vecinas: Int = 2) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val lista = e.visibles
    val sel = e.indiceSel
    val mov = LocalMovimiento.current
    val pos by animateFloatAsState(sel.toFloat(), if (mov) androidx.compose.animation.core.spring(0.85f, 300f) else tween(0), label = "estante")
    var arrastre by remember { mutableStateOf(0f) }
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.pointerInput(lista.size) {
        detectHorizontalDragGestures(onDragEnd = { arrastre = 0f }) { ch, dx ->
            ch.consume(); arrastre += dx
            val umbral = paso * esc.factor * density * 0.5f
            if (arrastre > umbral) { arrastre = 0f; Diptico.moverJuego(-1) }
            if (arrastre < -umbral) { arrastre = 0f; Diptico.moverJuego(1) }
        }
    }) {
        val anchoU = maxWidth.value / esc.factor
        val centro = anchoU / 2f
        // Sombra en el piso de la elegida.
        Box(Modifier.offset(esc.d(centro - 290f), esc.d(30f + alto + 112f)).size(esc.d(580), esc.d(40))
            .background(Brush.radialGradient(listOf(Color.Black.copy(alpha = .55f), Color.Transparent))))
        val desde = (sel - vecinas - 1).coerceAtLeast(0)
        val hasta = (sel + vecinas + 1).coerceAtMost(lista.size - 1)
        for (i in desde..hasta) {
            val j = lista[i]
            val c = Consolas.porId(j.consolaId) ?: continue
            val k = i - pos
            val cerca = (1f - abs(k)).coerceIn(0f, 1f)
            val w = 200f + (ancho - 200f) * cerca
            val h = 280f + (alto - 280f) * cerca
            val x = centro + k * paso - w / 2f
            val y = 30f + (95f - 30f) * (1f - cerca)
            key(j.clave) {
                Box(
                    Modifier.offset(esc.d(x), esc.d(y)).size(esc.d(w), esc.d(h)).zIndex(10f - abs(k))
                        .graphicsLayer { alpha = if (abs(k) > vecinas + 0.5f) 0f else 0.78f + 0.22f * cerca }
                        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {
                            if (i == sel) Diptico.jugar() else Diptico.elegirJuego(j.clave)
                        },
                ) {
                    Caja3D(
                        datosCaja(e, j, c), j, c, Modifier.fillMaxSize(),
                        giroY = { val kk = i - pos; if (abs(kk) < 1f) 14f * (1f - abs(kk)) + (if (kk < 0) 48f else -48f) * abs(kk) else if (kk < 0) 48f else -48f },
                        giroX = { -3f }, caras = e.extra.caras[j.clave], detalle = i == sel, sombraPiso = false,
                    )
                    if (i == sel) Box(Modifier.fillMaxSize().padding(esc.d(6)).border(esc.d(4), col.elegido, RoundedCornerShape(esc.d(8))))
                }
            }
        }
    }
}


/** Cuadrícula de portadas (vista «Cuadrícula»): la elegida con anillo blanco. */
@Composable
fun RejillaCV(e: Estado, columnas: Int, modifier: Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val estado = rememberLazyGridState()
    LaunchedEffect(e.indiceSel) { estado.animateScrollToItem((e.indiceSel - columnas).coerceAtLeast(0)) }
    androidx.compose.runtime.SideEffect { Teclas.columnasCuadricula = columnas }
    LazyVerticalGrid(GridCells.Fixed(columnas), modifier, estado, horizontalArrangement = Arrangement.spacedBy(esc.d(18)),
        verticalArrangement = Arrangement.spacedBy(esc.d(18))) {
        itemsIndexed(e.visibles, key = { _, j -> j.clave }) { i, j ->
            val elegido = i == e.indiceSel
            Box(Modifier.fillMaxWidth().height(esc.d(260)).clip(redondo(14f))
                .then(if (elegido) Modifier.border(esc.d(4), col.elegido, redondo(14f)) else Modifier)
                .clickable { if (elegido) Diptico.jugar() else Diptico.elegirJuego(j.clave) }) {
                ArteJuego(j, preferirArte = false, maxLado = 360, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** Lista con nombres claros (vista «Lista»). */
@Composable
fun ListaCV(e: Estado, modifier: Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val estado = rememberLazyListState()
    LaunchedEffect(e.indiceSel) { estado.animateScrollToItem((e.indiceSel - 2).coerceAtLeast(0)) }
    androidx.compose.runtime.SideEffect { Teclas.columnasCuadricula = 1 }
    LazyColumn(modifier, estado, verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
        itemsIndexed(e.visibles, key = { _, j -> j.clave }) { i, j ->
            val elegido = i == e.indiceSel
            Row(Modifier.fillMaxWidth().height(esc.d(96)).clip(redondo(24f))
                .background(if (elegido) col.elegido else col.vidrio)
                .clickable { if (elegido) Diptico.jugar() else Diptico.elegirJuego(j.clave) }.padding(horizontal = esc.d(14)),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(18))) {
                Box(Modifier.size(esc.d(68)).clip(RoundedCornerShape(esc.d(10)))) { ArteJuego(j, preferirArte = false, maxLado = 160, modifier = Modifier.fillMaxSize()) }
                TB(j.titulo, 28f, color = if (elegido) col.enElegido else col.texto, titulo = true, modifier = Modifier.weight(1f), traducir = false)
                val t = e.datos[j.clave]?.tiempo ?: 0L
                if (t > 0) TB(formatoTiempo(t), 22f, color = if (elegido) col.enElegido.copy(alpha = .7f) else col.sub, traducir = false)
            }
        }
    }
}

/** La barra de abajo: vistas · dock de apps · Colecciones · opciones del juego · ajustes. */
@Composable
fun BarraAbajoCV(e: Estado, modifier: Modifier, conDock: Boolean = true) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Row(modifier.fillMaxWidth().height(esc.d(96)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
        Row(Modifier.clip(RoundedCornerShape(50)).background(col.texto.copy(alpha = .07f)).border(1.dp, col.borde, RoundedCornerShape(50)).padding(esc.d(6)),
            horizontalArrangement = Arrangement.spacedBy(esc.d(4))) {
            listOf("estante" to "Estante 3D", "cuadricula" to "Cuadrícula", "lista" to "Lista").forEach { (v, n) ->
                val on = e.extra.vistaColeccion == v
                Box(Modifier.height(esc.d(76)).clip(RoundedCornerShape(50)).background(if (on) col.elegido else Color.Transparent)
                    .clickable { Diptico.elegirVistaColeccion(v) }.padding(horizontal = esc.d(22)), contentAlignment = Alignment.Center) {
                    TB(n, 22f, color = if (on) col.enElegido else col.sub, peso = FontWeight.ExtraBold)
                }
            }
        }
        if (conDock) FilaDock(e, 56f, Modifier.weight(1f), conCajon = false) else Spacer(Modifier.weight(1f))
        Row(Modifier.height(esc.d(88)).clip(RoundedCornerShape(50)).background(col.texto.copy(alpha = .07f)).border(1.dp, col.borde, RoundedCornerShape(50))
            .clickable { Diptico.elegirPestanaCarpetas(1); Diptico.volverASistemas() }.padding(horizontal = esc.d(24)),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
            IconoDiptico(Icono.CARPETA, col.texto, 28f)
            TB("Colecciones", 22f)
        }
        BotonRedondo(Icono.EDITAR, 88f, { Diptico.abrirRapido() }, descripcion = "Opciones del juego")
        BotonRedondo(Icono.AJUSTES, 88f, { Diptico.abrirMenu() }, descripcion = "Ajustes")
    }
}

/** 2b · el carrete: la imagen grande, de dónde sale y las miniaturas (con la caja girada arriba). */
@Composable
fun CarreteCV(e: Estado, j: Juego, modifier: Modifier, compacto: Boolean = false) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val imgs = imagenesCarrete(e, j)
    val sel = e.extra.carreteSel.coerceIn(0, (imgs.size - 1).coerceAtLeast(0))
    Column(modifier.fillMaxSize().padding(horizontal = esc.d(40)), verticalArrangement = Arrangement.spacedBy(esc.d(22))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                TB("IMÁGENES DE ${j.titulo.uppercase()}", 20f, color = col.sub, peso = FontWeight.ExtraBold, espaciado = 2.4f, traducir = false)
                TB(imgs.getOrNull(sel)?.first ?: "Sin imágenes todavía", 34f, titulo = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                listOf("ScreenScraper", "SteamGridDB", "TheGamesDB").forEach { f ->
                    Box(Modifier.height(esc.d(48)).vidrio(24f).clickable { Diptico.abrirCajaJuego(j.clave) }.padding(horizontal = esc.d(16)), contentAlignment = Alignment.Center) {
                        TB(f, 18f, peso = FontWeight.ExtraBold, traducir = false)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f).clip(redondo(28f)).background(col.vidrio), contentAlignment = Alignment.Center) {
            val img = rememberImagen(imgs.getOrNull(sel)?.second, 1200)
            if (img != null) Image(img, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            else TB("Busca imágenes con Y", 26f, color = col.sub)
        }
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
            itemsIndexed(imgs) { i, (_, uri) ->
                val img = rememberImagen(uri, 240)
                Box(Modifier.size(esc.d(if (compacto) 110f else 150f), esc.d(if (compacto) 80f else 110f)).clip(redondo(14f)).background(col.vidrio)
                    .then(if (i == sel) Modifier.border(esc.d(4), col.elegido, redondo(14f)) else Modifier)
                    .clickable { Diptico.elegirCarrete(i) }) {
                    if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = esc.d(36)), horizontalArrangement = Arrangement.SpaceBetween) {
            PistaTecla("◀ ▶", "Recorrer"); PistaTecla("Y", "Buscar imágenes"); PistaTecla("R3", "Girar la caja"); PistaTecla("B", "Volver")
        }
    }
}

// ———————————————————————————— 3 · una pantalla ————————————————————————————

@Composable
fun ColeccionUnica(e: Estado) {
    val j = e.seleccionado
    val c = j?.let { Consolas.porId(it.consolaId) }
    LienzoBoceto(1920f, 1080f, fondo = LocalColeccion.current.fondo) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val girada = e.extra.cajaGirada && j != null
        if (j != null) androidx.compose.animation.Crossfade(j.clave, animationSpec = tween(320), label = "arte") { _ ->
            ArteJuego(j, preferirArte = true, maxLado = 1280, modifier = Modifier.fillMaxSize())
        }
        Box(Modifier.fillMaxSize().veloColeccion(col.fondo))
        // Franja 1 · barra compacta (88): consola · búsqueda, apps, widgets, ajustes · DIPPO, hora y batería, perfil.
        Row(Modifier.fillMaxWidth().height(esc.d(88)).padding(horizontal = esc.d(48)).offset(y = esc.d(20)),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
            PildoraVidrio(64f, onClick = { Diptico.volverASistemas() }) {
                Box(Modifier.size(esc.d(16)).clip(CircleShape).background(c?.color ?: col.acento))
                TB(c?.let { it.nombreLargo } ?: "Tus juegos", 26f, titulo = true, peso = FontWeight.SemiBold)
                if (e.visibles.isNotEmpty()) TB("${e.indiceSel + 1} de ${e.visibles.size}", 20f, color = col.sub, traducir = false)
            }
            BotonRedondo(Icono.BUSCAR, 64f, { Diptico.abrirBuscar() }, descripcion = "Buscar")
            BotonRedondo(Icono.MENU, 64f, { Diptico.abrirApps() }, descripcion = "Apps")
            BotonRedondo(Icono.APPS, 64f, { Diptico.fijarInicio("widgets") }, descripcion = "Widgets")
            BotonRedondo(Icono.AJUSTES, 64f, { Diptico.abrirMenu() }, descripcion = "Ajustes")
            Spacer(Modifier.weight(1f))
            if (e.extra.dippoInicio) Box(Modifier.size(esc.d(64)).clickable { Diptico.abrirAsistente() }, contentAlignment = Alignment.Center) {
                cl.nikalbazu.diptico.bisa.AvatarBisa(56f * esc.factor)
            }
            HoraBateria(60f, 24f)
            BotonPerfil(64f)
        }
        if (j == null) {
            Column(Modifier.align(Alignment.CenterStart).padding(start = esc.d(96))) {
                TB("Aún no hay juegos aquí", 64f, titulo = true)
                TB("Elige tu carpeta de juegos en Ajustes › Biblioteca.", 26f, color = col.sub)
            }
            return@LienzoBoceto
        }
        val alto = LocalEscala.current.let { 1080f }
        if (girada) {
            // 3b · reverso con zoom a la izquierda y el carrete a la derecha, en la misma pantalla.
            CajaVitrinaCV(e, j, c ?: return@LienzoBoceto, x = 260f, y = 190f, ancho = 380f, alto = 530f, centroX = 260f, girada = true)
            CarreteCV(e, j, Modifier.padding(start = esc.d(900), top = esc.d(130)), compacto = true)
            return@LienzoBoceto
        }
        // Franja 2 · el juego: información a la izquierda, caja a la derecha.
        Column(Modifier.offset(esc.d(96), esc.d(150)).width(esc.d(1000)), verticalArrangement = Arrangement.spacedBy(esc.d(16))) {
            TB((c?.bonito ?: "").uppercase(), 22f, color = lerpClaro(c?.color ?: col.acento), peso = FontWeight.ExtraBold, espaciado = 3f, traducir = false)
            val logo = rememberImagen(j.logo, 800)
            if (logo != null) Image(logo, j.titulo, contentScale = ContentScale.Fit, alignment = Alignment.CenterStart,
                modifier = Modifier.heightIn(max = esc.d(170)).widthIn(max = esc.d(700)))
            else TB(j.titulo, 92f, titulo = true, lineas = 2, interlineado = 92f, sombra = true, traducir = false)
            val resumen = resumenJuego(j, c)
            if (resumen.isNotBlank()) TB(resumen, 24f, color = col.sub, peso = FontWeight.SemiBold, lineas = 2, interlineado = 33f, modifier = Modifier.widthIn(max = esc.d(860)), traducir = false)
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) { chipsJuego(e, j).ifEmpty { listOf("Aún sin jugar con Díptico") }.take(3).forEach { ChipDato(it, 21f) } }
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(14)), modifier = Modifier.padding(top = esc.d(6))) {
                BotonTecla("A", if ((e.datos[j.clave]?.tiempo ?: 0L) > 0) "Continuar" else "Jugar", { Diptico.jugar() }, alto = 76f, principal = true, letra = 26f)
                BotonTecla("X", "Ficha", { Diptico.abrirFicha() }, alto = 76f, letra = 24f)
                BotonTecla("Y", "Editar caja", { Diptico.abrirCajaJuego(j.clave) }, alto = 76f, letra = 24f)
            }
        }
        CajaVitrinaCV(e, j, c ?: return@LienzoBoceto, x = 1420f, y = 140f, ancho = 300f, alto = 420f, centroX = 1420f, girada = false)
        // Franja 3 · el estante: una fila de cajas, sin tapar nada de arriba.
        Box(Modifier.fillMaxWidth().offset(y = esc.d(alto - 400f)).height(esc.d(400))) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, col.fondo.copy(alpha = .9f)))))
            Row(Modifier.padding(start = esc.d(48), top = esc.d(18)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(18))) {
                TB("Tus juegos", 26f, titulo = true)
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    listOf("estante" to "Estante 3D", "cuadricula" to "Cuadrícula", "lista" to "Lista").forEach { (v, n) ->
                        val on = e.extra.vistaColeccion == v
                        Box(Modifier.height(esc.d(44)).clip(RoundedCornerShape(50)).background(if (on) col.elegido else col.vidrio)
                            .clickable { Diptico.elegirVistaColeccion(v) }.padding(horizontal = esc.d(16)), contentAlignment = Alignment.Center) {
                            TB(n, 18f, color = if (on) col.enElegido else col.texto, peso = FontWeight.ExtraBold)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                FilaDock(e, 48f, conCajon = false)
                Spacer(Modifier.width(esc.d(48)))
            }
            when (e.extra.vistaColeccion) {
                "cuadricula" -> RejillaCV(e, columnas = 8, Modifier.fillMaxSize().padding(start = esc.d(48), end = esc.d(48), top = esc.d(90)))
                "lista" -> ListaCV(e, Modifier.fillMaxSize().padding(start = esc.d(48), end = esc.d(48), top = esc.d(90)))
                else -> EstanteCV(e, Modifier.fillMaxSize().padding(top = esc.d(60)), ancho = 190f, alto = 260f, paso = 200f, vecinas = 4)
            }
        }
    }
}

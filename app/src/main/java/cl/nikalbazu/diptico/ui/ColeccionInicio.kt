package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.togetherWith
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

private fun lerpClaro(c: Color): Color = androidx.compose.ui.graphics.lerp(c, Color.White, 0.55f)

private fun Color.siOscuro(col: ColoresColeccion): Color = if (col.texto.luminance() > 0.5f) this else col.sub


// ———————————————————————————— 1 · Thor arriba ————————————————————————————

/** El fondo del tema en Colección viva: la decoración va DETRÁS de todo (encima tapaba los botones). */
@Composable
fun FondoTemaCV() {
    if (Estilo.decoraciones && Estilo.tema.decor != "nada") Decoracion(Estilo.tema, animada = !Estilo.decorQuieta)
}

@Composable
fun ColeccionArriba(e: Estado) {
    val j = e.seleccionado
    val c = j?.let { Consolas.porId(it.consolaId) }
    LienzoBoceto(1920f, 1080f, fondo = LocalColeccion.current.fondo, zoom = ZOOM_ARRIBA) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val girada = e.extra.cajaGirada && j != null
        val mov = LocalMovimiento.current
        val t by animateFloatAsState(if (girada) 1f else 0f, tween(if (mov) duracionMovimiento(650) else 0, easing = CurvaPosarse), label = "girada")
        // Arte del juego a pantalla completa (si no hay, el color de la consola); se funde al cambiar de juego.
        if (j != null) androidx.compose.animation.Crossfade(j.clave, animationSpec = tween(380), label = "arte") { clave ->
            val jj = e.visibles.firstOrNull { it.clave == clave } ?: j
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf((Consolas.porId(jj.consolaId)?.color ?: col.acento).copy(alpha = .55f), col.fondo)))) {
                ArteJuego(jj, preferirArte = true, maxLado = 1280, modifier = Modifier.fillMaxSize())
            }
        }
        Box(Modifier.fillMaxSize().veloColeccion(col.fondo))
        FondoTemaCV()
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(64), vertical = esc.d(30))) {
            // Franja 1 · barra: consola · hora, batería y perfil.
            Row(Modifier.fillMaxWidth().height(esc.d(72)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                PildoraVidrio(60f, onClick = { Diptico.volverASistemas() }) {
                    Box(Modifier.size(esc.d(16)).clip(CircleShape).background(c?.color ?: col.acento))
                    TB(c?.bonito ?: "Tus juegos", 26f, titulo = true, peso = FontWeight.SemiBold)
                    if (e.visibles.isNotEmpty()) TB("${e.indiceSel + 1} de ${e.visibles.size}", 20f, color = col.sub, traducir = false)
                }
                Spacer(Modifier.weight(1f))
                HoraBateria(60f, 24f)
                BotonPerfil(64f)
            }
            if (j == null) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    TB("Aún no hay juegos aquí", 64f, titulo = true)
                    TB("Elige tu carpeta de juegos en Ajustes › Biblioteca.", 24f, color = col.sub)
                }
                return@Column
            }
            // Franja 2 · la información a la izquierda y la caja a la derecha (al darse vuelta, la caja va al centro).
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val anchoU = maxWidth.value / esc.factor
                val altoU = maxHeight.value / esc.factor
                Column(Modifier.fillMaxHeight().width(esc.d(anchoU * 0.56f)).graphicsLayer { alpha = 1f - t; translationX = -60f * t },
                    verticalArrangement = Arrangement.spacedBy(esc.d(16), Alignment.CenterVertically)) {
                    InfoJuegoCV(e, j, c, tamTitulo = 92f)
                }
                // La caja: flota, se gira con el dedo y entra animada al cambiar de juego.
                // Marco casi cuadrado: así llenan su lugar tanto las cajas altas (PS2) como las anchas (SNES, N64).
                val ladoAlto = altoU * (0.92f + 0.08f * t)
                val ladoAncho = minOf(anchoU * (0.42f + 0.2f * t), ladoAlto * 1.05f)
                Box(Modifier.align(androidx.compose.ui.BiasAlignment(0.74f * (1f - t), 0f)).size(esc.d(ladoAncho), esc.d(ladoAlto))) {
                    CajaFlotanteCV(e, j, girada)
                }
            }
            // Franja 3 · acciones a la izquierda y DIPPO a la derecha, cada uno en su lugar.
            Row(Modifier.fillMaxWidth().height(esc.d(92)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                if (girada) {
                    PistaTecla("X", "Ver la ficha completa", 24f); PistaTecla("R3", "Volver a girar", 24f); PistaTecla("B", "Volver", 24f)
                    Spacer(Modifier.weight(1f))
                } else {
                    BotonTecla("A", if ((e.datos[j.clave]?.tiempo ?: 0L) > 0) "Continuar" else "Jugar", { Diptico.jugar() }, alto = 84f, principal = true, letra = 30f)
                    BotonTecla("X", "Ficha", { Diptico.girarCaja(true) }, alto = 84f, letra = 28f)
                    BotonTecla("Y", "Editar caja", { Diptico.abrirCajaJuego(j.clave) }, alto = 84f, letra = 28f)
                    Spacer(Modifier.weight(1f))
                    if (e.extra.dippoInicio) DippoFrase(fraseDippo(e, j), cara = 56f, letra = 22f, maxAncho = 520f)
                }
            }
        }
    }
}

/** Género/consola, título (o logo), resumen, chips y el próximo paso. */
@Composable
private fun InfoJuegoCV(e: Estado, j: Juego, c: Consola?, tamTitulo: Float, compacta: Boolean = false) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    TB((c?.bonito ?: "").uppercase(), 22f, color = lerpClaro(c?.color ?: col.acento), peso = FontWeight.ExtraBold, espaciado = 3.2f, traducir = false)
    val logo = rememberImagen(j.logo, 900)
    if (logo != null) Image(logo, j.titulo, contentScale = ContentScale.Fit, alignment = Alignment.CenterStart,
        modifier = Modifier.heightIn(max = esc.d(tamTitulo * 2f)).fillMaxWidth())
    else TB(j.titulo, tamTitulo, titulo = true, lineas = 2, interlineado = tamTitulo, sombra = true, traducir = false)
    val resumen = resumenJuego(j, c)
    if (resumen.isNotBlank() && !compacta) TB(resumen, 26f, color = Color(0xFFD3D5EA).siOscuro(col), peso = FontWeight.SemiBold, lineas = 3, interlineado = 36f, traducir = false)
    val chips = chipsJuego(e, j).ifEmpty { listOf("Aún sin jugar con Díptico") }
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(12))) { chips.take(3).forEach { ChipDato(it, 22f) } }
    val pct = avance(e, j)
    val paso = e.datos[j.clave]?.paso.orEmpty()
    if (pct != null) Box(Modifier.fillMaxWidth(0.85f).height(esc.d(10)).clip(RoundedCornerShape(50)).background(col.texto.copy(alpha = .14f))) {
        Box(Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().background(Brush.horizontalGradient(listOf(col.acento, c?.color ?: col.acento))))
    }
    if (paso.isNotBlank()) TB("Tu próximo paso: $paso", 24f, color = Color(0xFFC7C9E0).siOscuro(col), peso = FontWeight.SemiBold, lineas = 2, traducir = false,
        modifier = Modifier.clickable { Diptico.abrirPaso() })
}

/**
 * La caja del juego elegido: entra deslizándose al cambiar de juego, flota y se mece como antes, se gira con el dedo y,
 * con [girada], da la vuelta y muestra el reverso con la información del juego.
 */
@Composable
fun CajaFlotanteCV(e: Estado, j: Juego, girada: Boolean, pose: Float = 26f) {
    val mov = LocalMovimiento.current
    val indice = e.indiceSel
    androidx.compose.animation.AnimatedContent(
        targetState = j.clave,
        transitionSpec = {
            val adelante = e.visibles.indexOfFirst { it.clave == targetState } >= e.visibles.indexOfFirst { it.clave == initialState }
            if (!mov) androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
            else (androidx.compose.animation.slideInHorizontally(tween(420, easing = CurvaPosarse)) { if (adelante) it / 2 else -it / 2 } +
                androidx.compose.animation.fadeIn(tween(300)) + androidx.compose.animation.scaleIn(tween(420), initialScale = 0.9f)) togetherWith
                (androidx.compose.animation.slideOutHorizontally(tween(320)) { if (adelante) -it / 2 else it / 2 } + androidx.compose.animation.fadeOut(tween(220)))
        },
        label = "caja-${indice >= 0}", modifier = Modifier.fillMaxSize(),
    ) { clave ->
        val jj = e.visibles.firstOrNull { it.clave == clave } ?: j
        val cc = Consolas.porId(jj.consolaId) ?: return@AnimatedContent
        val t by animateFloatAsState(if (girada) 1f else 0f, tween(if (mov) duracionMovimiento(700) else 0, easing = CurvaPosarse), label = "giro")
        val ola = if (mov) androidx.compose.animation.core.rememberInfiniteTransition(label = "flotar").animateFloat(
            0f, (2 * Math.PI).toFloat(), androidx.compose.animation.core.infiniteRepeatable(tween(7000, easing = androidx.compose.animation.core.LinearEasing)), label = "ola",
        ) else null
        // Se gira con el dedo hacia cualquier lado; al soltarla vuelve a su pose con un resorte suave (sin saltos).
        var dedo by remember { mutableStateOf(0f) }
        var dedoV by remember { mutableStateOf(0f) }
        val dedoAnim by animateFloatAsState(dedo, if (dedo == 0f) androidx.compose.animation.core.spring(0.6f, 140f) else androidx.compose.animation.core.spring(1f, 2000f), label = "dedo")
        val dedoVAnim by animateFloatAsState(dedoV, if (dedoV == 0f) androidx.compose.animation.core.spring(0.6f, 140f) else androidx.compose.animation.core.spring(1f, 2000f), label = "dedoV")
        Box(
            Modifier.fillMaxSize().testTag("caja-vitrina")
                .graphicsLayer { translationY = (ola?.value?.let { kotlin.math.sin(it * 2f) * 10f } ?: 0f) * density }
                .pointerInput(clave) {
                    detectDragGestures(
                        onDragEnd = { dedo = 0f; dedoV = 0f }, onDragCancel = { dedo = 0f; dedoV = 0f },
                    ) { ch, d -> ch.consume(); dedo += d.x / (density * 1.2f); dedoV = (dedoV - d.y / (density * 1.6f)).coerceIn(-55f, 55f) }
                }
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { Diptico.girarCaja() },
        ) {
            Caja3D(
                datosCaja(e, jj, cc), jj, cc, Modifier.fillMaxSize(),
                giroY = { pose * (1f - t) + 180f * t + dedoAnim + (ola?.value?.let { kotlin.math.sin(it) * 6f } ?: 0f) * (1f - t) },
                giroX = { -5f + dedoVAnim + (ola?.value?.let { kotlin.math.cos(it) * 2f } ?: 0f) },
                caras = e.extra.caras[jj.clave], detalle = true, sombraPiso = true,
            )
        }
    }
}

// ———————————————————————————— 2 · Thor abajo ————————————————————————————

@Composable
fun ColeccionAbajo(e: Estado) {
    LienzoBoceto(1240f, 1080f, fondo = LocalColeccion.current.fondo2, zoom = ZOOM_ABAJO) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val c = e.seleccionado?.let { Consolas.porId(it.consolaId) } ?: Consolas.porId(e.consolaSel)
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf((c?.color ?: col.acento).copy(alpha = .22f), Color.Transparent), radius = 900f)))
        FondoTemaCV()
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(28), vertical = esc.d(22)), verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
            // Fila de arriba: DIPPO · buscar · widgets · apps.
            Row(Modifier.fillMaxWidth().height(esc.d(76)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                Box(Modifier.size(esc.d(76)).clip(CircleShape).background(col.texto.copy(alpha = .07f)).border(1.dp, col.borde, CircleShape)
                    .clickable { Diptico.abrirAsistente() }, contentAlignment = Alignment.Center) { cl.nikalbazu.diptico.bisa.AvatarBisa(54f * esc.factor) }
                Row(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50)).background(col.texto.copy(alpha = .07f)).border(1.dp, col.borde, RoundedCornerShape(50))
                    .clickable { Diptico.abrirBuscar() }.padding(horizontal = esc.d(24)), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                    IconoDiptico(Icono.BUSCAR, col.sub, 28f)
                    TB("Buscar en tu colección", 24f, color = col.sub, peso = FontWeight.SemiBold)
                }
                BotonRedondo(Icono.APPS, 76f, { Diptico.fijarInicio("widgets") }, descripcion = "Widgets")
                BotonRedondo(Icono.MENU, 76f, { Diptico.abrirApps() }, descripcion = "Todas las apps")
            }
            if (e.extra.cajaGirada && e.seleccionado != null) {
                CarreteCV(e, e.seleccionado!!, Modifier.weight(1f))
                return@Column
            }
            // Consolas: L1 · píldoras · R1.
            FilaConsolasCV(e, Modifier.fillMaxWidth().height(esc.d(80)), alto = 72f)
            // Los juegos de la consola, en la vista elegida.
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when (e.extra.vistaColeccion) {
                    "cuadricula" -> RejillaCV(e, columnas = 4, Modifier.fillMaxSize())
                    "lista" -> ListaCV(e, Modifier.fillMaxSize())
                    else -> Column(Modifier.fillMaxSize()) {
                        EstanteCV(e, Modifier.fillMaxWidth().weight(1f))
                        val j = e.seleccionado
                        Column(Modifier.fillMaxWidth().padding(top = esc.d(6)), horizontalAlignment = Alignment.CenterHorizontally) {
                            TB(j?.titulo ?: "", 40f, titulo = true, traducir = false)
                            TB("${e.indiceSel + 1} de ${e.visibles.size} · Select cambia la vista", 20f, color = col.sub)
                        }
                    }
                }
            }
            BarraAbajoCV(e, Modifier.fillMaxWidth())
        }
    }
}

/**
 * El estante 3D: la elegida al centro (grande), las vecinas giradas ±48° mostrando el lomo. Los tamaños salen del alto que
 * le toca, así llena su franja en cualquier pantalla.
 */
@Composable
fun EstanteCV(e: Estado, modifier: Modifier, vecinas: Int = 2) {
    val esc = LocalEscala.current
    val lista = e.visibles
    val sel = e.indiceSel
    val mov = LocalMovimiento.current
    val pos by animateFloatAsState(sel.toFloat(), if (mov) androidx.compose.animation.core.spring(0.85f, 300f) else tween(0), label = "estante")
    val ola = if (mov) androidx.compose.animation.core.rememberInfiniteTransition(label = "flotar-estante").animateFloat(
        0f, (2 * Math.PI).toFloat(), androidx.compose.animation.core.infiniteRepeatable(tween(6000, easing = androidx.compose.animation.core.LinearEasing)), label = "ola",
    ) else null
    var arrastre by remember { mutableStateOf(0f) }
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
        val anchoU = maxWidth.value / esc.factor
        val altoU = maxHeight.value / esc.factor
        val alto = altoU * 0.94f
        val ancho = alto * 1.0f
        val paso = ancho * 0.8f
        val umbral = paso * esc.factor * 0.5f
        Box(Modifier.fillMaxSize().pointerInput(lista.size) {
            detectHorizontalDragGestures(onDragEnd = { arrastre = 0f }) { ch, dx ->
                ch.consume(); arrastre += dx
                if (arrastre > umbral * density) { arrastre = 0f; Diptico.moverJuego(-1) }
                if (arrastre < -umbral * density) { arrastre = 0f; Diptico.moverJuego(1) }
            }
        }) {
            val centro = anchoU / 2f
            Box(Modifier.offset(esc.d(centro - ancho * 0.9f), esc.d(altoU - 30f)).size(esc.d(ancho * 1.8f), esc.d(30))
                .background(Brush.radialGradient(listOf(Color.Black.copy(alpha = .5f), Color.Transparent))))
            val desde = (sel - vecinas - 1).coerceAtLeast(0)
            val hasta = (sel + vecinas + 1).coerceAtMost(lista.size - 1)
            for (i in desde..hasta) {
                val j = lista[i]
                val c = Consolas.porId(j.consolaId) ?: continue
                val k = i - pos
                val cerca = (1f - abs(k)).coerceIn(0f, 1f)
                val escala = 0.78f + 0.22f * cerca
                val w = ancho * escala
                val h = alto * escala
                val x = centro + k * paso - w / 2f
                val y = (altoU - h) / 2f + (1f - cerca) * altoU * 0.04f
                key(j.clave) {
                    Box(
                        Modifier.offset(esc.d(x), esc.d(y)).size(esc.d(w), esc.d(h)).zIndex(10f - abs(k))
                            .graphicsLayer {
                                alpha = if (abs(k) > vecinas + 0.5f) 0f else 0.7f + 0.3f * cerca
                                translationY = if (i == sel) (ola?.value?.let { kotlin.math.sin(it * 2f) * 5f } ?: 0f) * density else 0f
                            }
                            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {
                                if (i == sel) Diptico.jugar() else Diptico.elegirJuego(j.clave)
                            },
                    ) {
                        Caja3D(
                            datosCaja(e, j, c), j, c, Modifier.fillMaxSize(),
                            giroY = {
                                val kk = i - pos
                                val base = if (abs(kk) < 1f) 14f * (1f - abs(kk)) + (if (kk < 0) 48f else -48f) * abs(kk) else if (kk < 0) 48f else -48f
                                base + if (i == sel) (ola?.value?.let { kotlin.math.sin(it) * 5f } ?: 0f) else 0f
                            },
                            giroX = { -4f }, caras = e.extra.caras[j.clave], detalle = i == sel, sombraPiso = false,
                        )
                    }
                }
            }
        }
    }
}

/** La barra de abajo: el dock de apps (5) · opciones del juego · ajustes. Las vistas se cambian con Select. */
@Composable
fun BarraAbajoCV(e: Estado, modifier: Modifier) {
    val esc = LocalEscala.current
    Row(modifier.height(esc.d(84)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
        DockCV(e, 58f)
        Spacer(Modifier.weight(1f))
        BotonRedondo(Icono.EDITAR, 76f, { Diptico.abrirRapido() }, descripcion = "Opciones del juego")
        BotonRedondo(Icono.AJUSTES, 76f, { Diptico.abrirMenu() }, descripcion = "Ajustes")
    }
}

// ———————————————————————————— 3 · una pantalla ————————————————————————————

@Composable
fun ColeccionUnica(e: Estado) {
    val j = e.seleccionado
    val c = j?.let { Consolas.porId(it.consolaId) }
    LienzoBoceto(1920f, 1080f, fondo = LocalColeccion.current.fondo, zoom = ZOOM_UNA) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val girada = e.extra.cajaGirada && j != null
        if (j != null) androidx.compose.animation.Crossfade(j.clave, animationSpec = tween(380), label = "arte") { clave ->
            val jj = e.visibles.firstOrNull { it.clave == clave } ?: j
            ArteJuego(jj, preferirArte = true, maxLado = 1280, modifier = Modifier.fillMaxSize())
        }
        Box(Modifier.fillMaxSize().veloColeccion(col.fondo))
        FondoTemaCV()
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(44), vertical = esc.d(20)), verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
            // Franja 1 · barra: consola · buscar, apps, widgets, ajustes · DIPPO, hora y batería, perfil.
            Row(Modifier.fillMaxWidth().height(esc.d(72)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                PildoraVidrio(62f, onClick = { Diptico.volverASistemas() }) {
                    Box(Modifier.size(esc.d(16)).clip(CircleShape).background(c?.color ?: col.acento))
                    TB(c?.bonito ?: "Tus juegos", 24f, titulo = true, peso = FontWeight.SemiBold)
                    if (e.visibles.isNotEmpty()) TB("${e.indiceSel + 1} de ${e.visibles.size}", 19f, color = col.sub, traducir = false)
                }
                BotonRedondo(Icono.BUSCAR, 64f, { Diptico.abrirBuscar() }, descripcion = "Buscar")
                BotonRedondo(Icono.MENU, 64f, { Diptico.abrirApps() }, descripcion = "Apps")
                BotonRedondo(Icono.APPS, 64f, { Diptico.fijarInicio("widgets") }, descripcion = "Widgets")
                BotonRedondo(Icono.AJUSTES, 64f, { Diptico.abrirMenu() }, descripcion = "Ajustes")
                Spacer(Modifier.weight(1f))
                if (e.extra.dippoInicio) Box(Modifier.size(esc.d(64)).clickable { Diptico.abrirAsistente() }, contentAlignment = Alignment.Center) {
                    cl.nikalbazu.diptico.bisa.AvatarBisa(56f * esc.factor)
                }
                HoraBateria(58f, 22f)
                BotonPerfil(60f)
            }
            if (j == null) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    TB("Aún no hay juegos aquí", 56f, titulo = true)
                    TB("Elige tu carpeta de juegos en Ajustes › Biblioteca.", 24f, color = col.sub)
                }
                return@Column
            }
            if (girada) {
                // 3b · el reverso grande a la izquierda y el carrete a la derecha, en la misma pantalla.
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(24))) {
                    Box(Modifier.weight(0.42f).fillMaxHeight()) { CajaFlotanteCV(e, j, girada = true) }
                    CarreteCV(e, j, Modifier.weight(0.58f).fillMaxHeight(), compacto = true)
                }
                return@Column
            }
            // Franja 2 · el juego: información y botones a la izquierda, la caja a la derecha.
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(24))) {
                Column(Modifier.weight(0.6f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(12), Alignment.CenterVertically)) {
                    InfoJuegoCV(e, j, c, tamTitulo = 72f, compacta = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                        BotonTecla("A", if ((e.datos[j.clave]?.tiempo ?: 0L) > 0) "Continuar" else "Jugar", { Diptico.jugar() }, alto = 72f, principal = true, letra = 26f)
                        BotonTecla("X", "Ficha", { Diptico.girarCaja(true) }, alto = 72f, letra = 24f)
                        BotonTecla("Y", "Editar caja", { Diptico.abrirCajaJuego(j.clave) }, alto = 72f, letra = 24f)
                    }
                }
                Box(Modifier.weight(0.4f).fillMaxHeight().padding(vertical = esc.d(4))) { CajaFlotanteCV(e, j, girada = false) }
            }
            // Franja 3 · el estante: título, vistas y dock arriba; las cajas debajo (sin tapar nada).
            Column(Modifier.fillMaxWidth().weight(1.25f).clip(redondo(28f)).background(col.fondo.copy(alpha = .55f)).padding(horizontal = esc.d(18), vertical = esc.d(10))) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                    TB("Tus juegos", 26f, titulo = true)
                    TB("Select cambia la vista", 18f, color = col.sub)
                    Spacer(Modifier.weight(1f))
                    DockCV(e, 50f)
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    when (e.extra.vistaColeccion) {
                        "cuadricula" -> RejillaCV(e, columnas = 7, Modifier.fillMaxSize().padding(top = esc.d(8)))
                        "lista" -> ListaCV(e, Modifier.fillMaxSize().padding(top = esc.d(8)))
                        else -> EstanteCV(e, Modifier.fillMaxSize(), vecinas = 4)
                    }
                }
            }
        }
    }
}


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
                    TB(k?.let { it.bonito } ?: "Todos", 23f, color = if (elegida) col.enElegido else col.sub, peso = FontWeight.ExtraBold)
                }
            }
        }
        TeclaHombro("R1") { Diptico.cambiarConsola(1) }
    }
}

@Composable
internal fun TeclaHombro(t: String, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Box(Modifier.clip(RoundedCornerShape(esc.d(10))).border(2.dp, col.tenue.copy(alpha = .6f), RoundedCornerShape(esc.d(10))).clickable(onClick = onClick)
        .padding(horizontal = esc.d(10), vertical = esc.d(6))) { TB(t, 20f, color = col.tenue, peso = FontWeight.ExtraBold, traducir = false) }
}

/** El estante 3D: la elegida al centro con anillo blanco; las vecinas más chicas, giradas ±48° mostrando el lomo. */
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
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(redondo(14f))
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
fun CarreteCV(e: Estado, j: Juego, modifier: Modifier, compacto: Boolean = false) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val imgs = imagenesCarrete(e, j)
    val sel = e.extra.carreteSel.coerceIn(0, (imgs.size - 1).coerceAtLeast(0))
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
        Column {
            Column {
                TB("IMÁGENES DE ${j.titulo.uppercase()}", 20f, color = col.sub, peso = FontWeight.ExtraBold, espaciado = 2.4f, traducir = false)
                TB(imgs.getOrNull(sel)?.first ?: "Sin imágenes todavía", 34f, titulo = true)
            }
            Row(Modifier.padding(top = esc.d(8)), horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
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
        Row(Modifier.fillMaxWidth().padding(bottom = esc.d(8)), horizontalArrangement = Arrangement.SpaceBetween) {
            PistaTecla("◀ ▶", "Recorrer", 20f); PistaTecla("Y", "Buscar", 20f); PistaTecla("B", "Volver", 20f)
        }
    }
}

// ———————————————————————————— 3 · una pantalla ————————————————————————————


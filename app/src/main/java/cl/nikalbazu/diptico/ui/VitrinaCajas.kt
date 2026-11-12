package cl.nikalbazu.diptico.ui

import cl.nikalbazu.diptico.estado.*

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Solo para las capturas de prueba: deja la caja del centro de frente, sin balanceo. */
internal var vitrinaSinGiro = false

/** Solo para las capturas de prueba: con cuántos grados parte girada la caja del centro. */
internal var vitrinaGiroPrueba = 0f

/** La portada de arriba: "cajas" (la vitrina con las cajas de los juegos) o "arte" (el arte del juego en grande). */
val Estado.portadaCajas: Boolean get() = extra.portada == "cajas"

/** Portada «PS5 con caja»: el arte del juego de fondo, su información a la izquierda y una sola caja 3D flotando a la derecha. */
val Estado.portadaPs5: Boolean get() = extra.portada == "ps5"

/** Arriba hay una caja 3D (vitrina de cajas o PS5 con caja). */
val Estado.vitrina3D: Boolean get() = portadaCajas || portadaPs5

/**
 * El stick derecho mientras se ve la vitrina: gira la caja del centro (lo escribe [cl.nikalbazu.diptico.estado.Teclas]).
 * [x] y [y] van de -1 a 1.
 */
object GiroCaja {
    var x by mutableFloatStateOf(0f)
    var y by mutableFloatStateOf(0f)
    /** Un pedido de vuelta completa (botón «Girar»): cuántas medias vueltas faltan dar. */
    var vueltas by mutableFloatStateOf(0f)
}

/** La vitrina está en pantalla y puede recibir el stick derecho. */
fun vitrinaActiva(e: Estado): Boolean =
    e.vitrina3D && e.sesion == null && nuevoDisenoDosPantallas(e) && e.panel == cl.nikalbazu.diptico.estado.Panel.NINGUNO &&
        e.inicio != "widgets" && e.extra.carga == null

/**
 * La vitrina de cajas: el arte del juego elegido de fondo, su caja en 3D al centro (se gira completa con el dedo o el
 * stick derecho: frente, lomo y reverso con su historia y tus datos) y las vecinas a los costados mostrando su lomo,
 * como en un estante. Tocar la caja del centro (o A) abre la pantalla de carga y el juego.
 */
@Composable
fun VitrinaCajas(e: Estado, juego: Juego) {
    androidx.compose.runtime.SideEffect { cl.nikalbazu.diptico.estado.Fluidez.contar("VitrinaCajas") }
    val esc = LocalEscala.current
    val consola = Consolas.porId(juego.consolaId)
    val color = consola?.color ?: Uva
    val ps5 = e.portadaPs5
    val lista = e.visibles
    val indice = lista.indexOfFirst { it.clave == juego.clave }.coerceAtLeast(0)
    // La posición del estante es continua: así las cajas se deslizan entre juego y juego en vez de saltar.
    val pos = remember { Animatable(indice.toFloat()) }
    LaunchedEffect(indice, lista.size) {
        if (abs(pos.value - indice) > 6f) pos.snapTo(indice.toFloat())
        pos.animateTo(indice.toFloat(), spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow))
    }
    // El giro de la caja del centro: libre (vuelta completa) en Y, limitado en X.
    val giroY = remember { Animatable(0f) }
    val giroX = remember { Animatable(0f) }
    val alcance = rememberCoroutineScope()
    LaunchedEffect(juego.clave) {
        giroY.snapTo(vitrinaGiroPrueba); giroX.snapTo(0f)
        // Si hay ScreenScraper o TheGamesDB, se busca el reverso real de la caja (una vez por juego).
        delay(700)
        Diptico.pedirCaras(juego.clave)
    }
    // Al soltar, la caja se acomoda en la cara más cercana (frente o reverso), con un resorte suave.
    suspend fun acomodar() {
        val destino = (giroY.value / 180f).roundToInt() * 180f
        alcance.launch { giroX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)) }
        giroY.animateTo(destino, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
        // Se vuelve a dejar entre -180 y 180 para que no crezca sin fin (se ve igual).
        val normal = ((destino % 360f) + 540f) % 360f - 180f
        giroY.snapTo(if (normal == -180f) 180f else normal)
    }
    // El stick derecho gira la caja mientras se mantiene; al soltarlo, se acomoda.
    LaunchedEffect(Unit) {
        var antesMovia = false
        snapshotFlow { abs(GiroCaja.x) > 0.15f || abs(GiroCaja.y) > 0.15f }.collect { moviendo ->
            // Solo se acomoda al soltar el stick (no al abrir la vitrina).
            if (!moviendo) { if (antesMovia) acomodar(); antesMovia = false; return@collect }
            antesMovia = true
            var antes = withFrameNanos { it }
            while (abs(GiroCaja.x) > 0.15f || abs(GiroCaja.y) > 0.15f) {
                val ahora = withFrameNanos { it }
                val dt = ((ahora - antes) / 1e9f).coerceAtMost(0.05f)
                antes = ahora
                giroY.snapTo(giroY.value + GiroCaja.x * 220f * dt)
                giroX.snapTo((giroX.value + (-GiroCaja.y * 34f - giroX.value) * (dt * 8f).coerceAtMost(1f)).coerceIn(-38f, 30f))
            }
        }
    }
    // El botón «Girar»: media vuelta (del frente al reverso y de vuelta).
    LaunchedEffect(Unit) {
        snapshotFlow { GiroCaja.vueltas }.collect { v ->
            if (v == 0f) return@collect
            GiroCaja.vueltas = 0f
            val destino = (giroY.value / 180f).roundToInt() * 180f + 180f * v
            giroY.animateTo(destino, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
            val normal = ((destino % 360f) + 540f) % 360f - 180f
            giroY.snapTo(if (normal == -180f) 180f else normal)
        }
    }
    val moverse = e.experiencia.movimiento && Estilo.efectosCompletos && !vitrinaSinGiro && LocalMovimiento.current
    // Un balanceo muy leve cuando nadie la toca: se siente viva, sin marear.
    val ola = if (moverse) androidx.compose.animation.core.rememberInfiniteTransition(label = "flotar").animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(tween(9000, easing = androidx.compose.animation.core.LinearEasing)), label = "ola",
    ) else null
    // La vitrina siempre es oscura (las cajas lucen sobre la penumbra), así que la letra va clara en cualquier tema.
    val paleta = LocalPaleta.current
    CompositionLocalProvider(LocalPaleta provides paleta.copy(tinta = Color.White, tenue = Color.White.copy(alpha = 0.66f))) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF09080D)).testTag("vitrina-cajas")) {
            val ancho = maxWidth.value
            val alto = maxHeight.value
            // 1. Fondo: la portada del juego elegido desenfocada (se arma una vez por juego, no en cada cuadro).
            Crossfade(juego, animationSpec = tween(if (Estilo.efectosCompletos) 520 else 0), label = "fondo-cajas") { j ->
                // PS5: el arte del juego nítido a pantalla completa, oscurecido hacia la izquierda donde va la información.
                if (ps5) {
                    ArteJuego(j, preferirArte = true, maxLado = 1280, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.9f }, imagen = "arte")
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color.Black.copy(alpha = 0.88f), 0.5f to Color.Black.copy(alpha = 0.5f), 1f to Color.Black.copy(alpha = 0.12f))))
                    return@Crossfade
                }
                // La portada del juego elegido (o su caja exacta), desenfocada de verdad y a pantalla completa.
                val fondo = rememberDesenfocada(e.extra.caras[j.clave]?.frente?.let { android.net.Uri.parse(it) } ?: j.portada ?: j.arte)
                if (fondo != null) androidx.compose.foundation.Image(
                    fondo, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.7f; scaleX = 1.08f; scaleY = 1.08f },
                )
            }
            if (!ps5) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to color.copy(alpha = 0.4f))))
            if (!ps5) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.55f), 0.3f to Color.Transparent, 0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.9f),
            )))

            // 2. El estante: la caja elegida al centro y hasta tres a cada lado.
            if (consola != null && lista.isNotEmpty()) CompositionLocalProvider(LocalFormas provides e.formas) {
                // PS5: una sola caja, flotando a la derecha (más liviano: se dibuja una caja, no siete).
                val desde = if (ps5) indice else (indice - 3).coerceAtLeast(0)
                val hasta = if (ps5) indice else (indice + 3).coerceAtMost(lista.lastIndex)
                val altoCaja = alto * (if (ps5) 0.64f else 0.66f)
                val anchoCaja = ancho * (if (ps5) 0.34f else 0.44f)
                val paso1 = ancho * 0.3f
                val paso2 = ancho * 0.13f
                Box(
                    Modifier.fillMaxSize().padding(bottom = (alto * (if (ps5) 0.04f else 0.16f)).dp, end = (if (ps5) ancho * 0.05f else 0f).dp),
                    contentAlignment = if (ps5) Alignment.CenterEnd else Alignment.Center,
                ) {
                    // De la más lejana a la más cercana: la del centro se dibuja al final, encima de todas.
                    val orden = (desde..hasta).sortedByDescending { abs(it - indice) }
                    for (i in orden) {
                        val j = lista[i]
                        val c = Consolas.porId(j.consolaId) ?: continue
                        key(j.clave) {
                            val esElegida = j.clave == juego.clave
                            fun r() = i - pos.value
                            Box(
                                Modifier.size(anchoCaja.dp, altoCaja.dp)
                                    .offset {
                                        val r = r()
                                        val ar = abs(r)
                                        val dx = (if (r < 0) -1f else 1f) * (ar.coerceAtMost(1f) * paso1 + (ar - 1f).coerceAtLeast(0f) * paso2)
                                        val dy = if (esElegida) (ola?.value?.let { kotlin.math.sin(it * 2f) * (if (ps5) 7f else 4f) } ?: 0f) else 0f
                                        IntOffset((dx * density).roundToInt(), (dy * density).roundToInt())
                                    }
                                    .graphicsLayer {
                                        val ar = abs(r())
                                        val s = 1f - 0.3f * ar.coerceAtMost(1f) - 0.08f * (ar - 1f).coerceAtLeast(0f)
                                        scaleX = s; scaleY = s
                                        alpha = if (ar > 3.4f) 0f else (1f - 0.35f * (ar - 2f).coerceAtLeast(0f)).coerceIn(0f, 1f)
                                    }
                                    .then(if (esElegida) Modifier.pointerInput(j.clave) {
                                        // Girar la caja con el dedo, como si la tuvieras en la mano: da la vuelta completa.
                                        detectDragGestures(
                                            onDragEnd = { alcance.launch { acomodar() } },
                                            onDragCancel = { alcance.launch { acomodar() } },
                                        ) { cambio, arrastre ->
                                            cambio.consume()
                                            alcance.launch {
                                                giroY.snapTo(giroY.value + arrastre.x / density * 0.6f)
                                                giroX.snapTo((giroX.value - arrastre.y / density * 0.35f).coerceIn(-38f, 30f))
                                            }
                                        }
                                    } else Modifier)
                                    .clickable(remember { MutableInteractionSource() }, indication = null) {
                                        if (esElegida) Diptico.jugar() else Diptico.elegirJuego(j.clave)
                                    },
                            ) {
                                Caja3D(
                                    datosCaja(e, j, c), j, c, Modifier.fillMaxSize(),
                                    // Las vecinas quedan giradas igual que en un estante de tienda: se les ve el lomo y parte
                                    // del frente. La del centro mira de frente y gira con lo que tú le hagas.
                                    giroY = {
                                        val k = (1f - abs(r())).coerceIn(0f, 1f)
                                        // Solo la caja del centro lee el balanceo: las vecinas no se redibujan en cada cuadro.
                                        val balanceo = if (esElegida) ola?.value?.let { kotlin.math.sin(it) * 5f } ?: 0f else 0f
                                        (1f - k) * 50f + k * (balanceo + if (esElegida) giroY.value else 0f)
                                    },
                                    giroX = { -7f + if (esElegida) (1f - abs(r()).coerceAtMost(1f)) * giroX.value else 0f },
                                    penumbra = { (abs(r()) * 0.45f).coerceAtMost(0.7f) },
                                    caras = e.extra.caras[j.clave],
                                    detalle = esElegida,
                                )
                            }
                        }
                    }
                }
            }

            // 3. Arriba: la consola a la izquierda; hora y batería a la derecha.
            // Hora y batería arriba (al otro lado de tu foto) y, debajo, la consola y en cuál juego vas.
            Column(Modifier.align(Alignment.TopStart).padding(start = esc.d(26), top = esc.d(14))) {
                if (e.sesion == null) { RelojBateria(); Spacer(Modifier.height(esc.d(8))) }
                Texto((consola?.nombre ?: "").uppercase(), 16f, color = Color.White, peso = FontWeight.ExtraBold, espaciado = 1f, lineas = 1, traducir = false)
                Texto("${indice + 1} de ${lista.size}", 10.5f, color = Color.White.copy(alpha = 0.6f), lineas = 1, traducir = false, espaciado = 1.2f)
            }
            // Hora y batería chicas, y tu perfil en grande (tipo iiSU).
            Row(Modifier.align(Alignment.TopEnd).padding(end = esc.d(22), top = esc.d(14)), verticalAlignment = Alignment.CenterVertically) {
                // Sin partida: hora y batería en su píldora, pegadas a tu foto y centradas con ella. Con partida no hay foto grande.
                if (e.sesion == null) PerfilArriba() else EstadoChico(conFoto = false)
            }

            // 4 (PS5). A la izquierda: logo (o nombre), datos y botones, como en la PS5.
            if (ps5) Column(
                Modifier.align(Alignment.CenterStart).padding(start = esc.d(30), top = esc.d(70)).fillMaxWidth(0.62f),
                verticalArrangement = Arrangement.spacedBy(esc.d(8)),
            ) {
                Crossfade(juego, animationSpec = tween(220), label = "info-ps5") { j ->
                    Column(verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
                        if (j.logo != null) ImagenAnimada(j.logo, Modifier.height(esc.d(76)).fillMaxWidth(0.8f), recortar = false)
                        Texto(j.titulo, if (j.logo != null) 18f else 26f, color = Color.White, lineas = 2, traducir = false, sombra = true)
                        val seg = e.datos[j.clave]?.tiempo?.toLong() ?: 0L
                        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                            listOfNotNull(
                                if (seg > 0) formatoTiempo(seg) + " " + cl.nikalbazu.diptico.estado.Idioma.t("jugadas") else cl.nikalbazu.diptico.estado.Idioma.t("Sin jugar"),
                                e.extra.duraciones[j.clave]?.takeIf { it.principal > 0f }?.let { cl.nikalbazu.diptico.estado.Idioma.t("Historia") + " ~" + it.principal.roundToInt() + " h" },
                                e.datos[j.clave]?.favorito?.takeIf { it }?.let { "♥" },
                            ).forEach { t ->
                                Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.14f)).padding(horizontal = esc.d(12), vertical = esc.d(4))) {
                                    Texto(t, 12.5f, color = Color.White, lineas = 1, traducir = false)
                                }
                            }
                        }
                    }
                }
            }
            // Los botones (mismo tamaño y orden) bajan al pie de la vitrina, a la izquierda, para aprovechar mejor el espacio.
            if (ps5) Row(
                Modifier.align(Alignment.BottomStart).padding(start = esc.d(30), bottom = esc.d(30)),
                horizontalArrangement = Arrangement.spacedBy(esc.d(10)), verticalAlignment = Alignment.CenterVertically,
            ) {
                val jugado = (e.datos[juego.clave]?.tiempo?.toLong() ?: 0L) > 0L
                BotonVitrina("A", if (jugado) "Continuar" else "Jugar", lleno = true) { Diptico.jugar() }
                BotonVitrina("X", "Ficha", lleno = false) { Diptico.abrirFicha() }
                BotonVitrina("Y", "DIPPO", lleno = false) { Diptico.abrirAsistente() }
            }
            // 4. Abajo: el nombre del juego, una línea de datos y los botones (también se tocan).
            if (!ps5) Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = esc.d(14), start = esc.d(24), end = esc.d(24)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(esc.d(4)),
            ) {
                Crossfade(juego, animationSpec = tween(220), label = "nombre-caja") { j ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Texto(j.titulo, 22f, color = Color.White, lineas = 1, traducir = false, alinear = TextAlign.Center)
                        val seg = e.datos[j.clave]?.tiempo?.toLong() ?: 0L
                        val linea = listOfNotNull(
                            consola?.bonito,
                            if (seg > 0) formatoTiempo(seg) + " " + cl.nikalbazu.diptico.estado.Idioma.t("jugadas") else null,
                            e.datos[j.clave]?.favorito?.takeIf { it }?.let { "♥" },
                        ).joinToString(" · ")
                        Texto(linea, 11.5f, color = Color.White.copy(alpha = 0.66f), lineas = 1, traducir = false, alinear = TextAlign.Center)
                    }
                }
                Row(Modifier.padding(top = esc.d(8)), horizontalArrangement = Arrangement.spacedBy(esc.d(10)), verticalAlignment = Alignment.CenterVertically) {
                    val jugado = (e.datos[juego.clave]?.tiempo?.toLong() ?: 0L) > 0L
                    BotonVitrina("A", if (jugado) "Continuar" else "Jugar", lleno = true) { Diptico.jugar() }
                    BotonVitrina("⟳", "Girar", lleno = false) { GiroCaja.vueltas = 1f }
                    BotonVitrina("X", "Ficha", lleno = false) { Diptico.abrirFicha() }
                    BotonVitrina("Y", "DIPPO", lleno = false) { Diptico.abrirAsistente() }
                }
            }
        }
    }
}

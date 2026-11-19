package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.luminance
import androidx.compose.animation.togetherWith
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.semantics.contentDescription
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/*
 * Colección viva · carpetas de consolas (bocetos 6a, 6b y 6c; docs/coleccion-viva.md).
 */

// ———————————————————————————— la consola ————————————————————————————

/**
 * Imágenes de las consolas: una foto de estudio de cada una (hecha en el computador a partir de los modelos 3D,
 * con fondo transparente) que va dentro de la app. Antes se dibujaban en 3D en vivo con Filament, pero ese motor
 * se caía en la Thor y usaba más de 1 GB de memoria; una imagen pesa ~25 KB y no se cae.
 * Se guardan las últimas en memoria para que cambiar de consola sea instantáneo.
 */
internal object ImagenesConsola {
    private val cache = android.util.LruCache<String, ImageBitmap>(6)
    private val fondos = android.util.LruCache<String, ImageBitmap>(6)
    fun hay(ctx: android.content.Context, id: String): Boolean =
        runCatching { ctx.assets.list("consolas")?.contains("$id.webp") == true }.getOrDefault(false)
    fun enMemoria(id: String): ImageBitmap? = cache.get(id)
    fun fondoEnMemoria(id: String): ImageBitmap? = fondos.get(id)
    /** La imagen entera (solo en un hilo de fondo). */
    fun cargar(ctx: android.content.Context, id: String): ImageBitmap? = cache.get(id) ?: runCatching {
        ctx.assets.open("consolas/$id.webp").use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
    }.getOrNull()?.also { cache.put(id, it) }
    /** Una versión diminuta (≈40 px): estirada a pantalla completa se ve como un difuminado suave, sin costo. */
    fun fondo(ctx: android.content.Context, id: String): ImageBitmap? = fondos.get(id) ?: runCatching {
        val o = BitmapFactory.Options().apply { inSampleSize = 16 }
        ctx.assets.open("consolas/$id.webp").use { BitmapFactory.decodeStream(it, null, o) }?.asImageBitmap()
    }.getOrNull()?.also { fondos.put(id, it) }
}

/**
 * La consola: su imagen flotando suave, con sombra en el piso. Con el dedo se inclina hacia los lados y vuelve sola.
 * (Se sigue llamando Consola3D para no cambiar las pantallas que la usan.)
 */
@Composable
fun Consola3D(c: Consola, modifier: Modifier = Modifier) {
    val id = Modelos3D.idDe(c.id)
    val mover = LocalMovimiento.current
    val ola = if (mover) {
        val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "flota")
        t.animateFloat(-1f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(3200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            androidx.compose.animation.core.RepeatMode.Reverse), label = "ola")
    } else remember { mutableStateOf(0f) }
    val giro = remember { Animatable(0f) }
    val giroX = remember { Animatable(0f) }
    // El stick derecho la inclina igual que el dedo; al soltarlo vuelve sola.
    val stickY by animateFloatAsState(GiroCaja.x * 30f, androidx.compose.animation.core.spring(0.7f, 180f), label = "stickY")
    val stickX by animateFloatAsState(-GiroCaja.y * 18f, androidx.compose.animation.core.spring(0.7f, 180f), label = "stickX")
    val alcance = rememberCoroutineScope()
    Box(modifier.testTag("consola-3d").pointerInput(Unit) {
        detectDragGestures(
            onDragEnd = {
                alcance.launch { giro.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = .45f, stiffness = 120f)) }
                alcance.launch { giroX.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = .45f, stiffness = 120f)) }
            },
        ) { cambio, d ->
            cambio.consume()
            alcance.launch { giro.snapTo((giro.value + d.x * 0.12f).coerceIn(-28f, 28f)) }
            alcance.launch { giroX.snapTo((giroX.value - d.y * 0.10f).coerceIn(-16f, 16f)) }
        }
    }, contentAlignment = Alignment.Center) {
        // Cada consola es su propio contenido: al cambiar de carpeta la de antes se va y la nueva entra.
        val t1 = duracionMovimiento(260); val t2 = duracionMovimiento(380); val t3 = duracionMovimiento(180)
        androidx.compose.animation.AnimatedContent(
            targetState = c, contentKey = { it.id },
            transitionSpec = {
                (androidx.compose.animation.fadeIn(tween(t1)) +
                    androidx.compose.animation.scaleIn(tween(t2, easing = CurvaPosarse), initialScale = 0.9f)) togetherWith
                    androidx.compose.animation.fadeOut(tween(t3))
            }, label = "consola-img", modifier = Modifier.fillMaxSize(),
        ) { k -> ImagenConsola(k, ola, { giro.value + stickY }, { giroX.value + stickX }) }
    }
}

@Composable
private fun ImagenConsola(c: Consola, ola: State<Float>, giroY: () -> Float, giroX: () -> Float) {
    val ctx = LocalContext.current
    val id = Modelos3D.idDe(c.id)
    val existe = remember(id) { ImagenesConsola.hay(ctx, id) }
    var img by remember(id) { mutableStateOf(ImagenesConsola.enMemoria(id)) }
    LaunchedEffect(id) {
        if (img == null && existe) img = withContext(Dispatchers.IO) { ImagenesConsola.cargar(ctx, id) }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val pintura = img
        if (pintura == null) {
            if (!existe) IconoConsola(c, 260f, c.color.copy(alpha = .22f))
        } else {
            // Sombra en el piso: un óvalo difuso que se achica cuando la consola sube.
            Canvas(Modifier.fillMaxSize()) {
                val k = 1f - ola.value * 0.06f
                val w = size.width * 0.62f * k
                val h = size.height * 0.07f * k
                val cy = size.height * 0.9f
                drawOval(Brush.radialGradient(listOf(Color.Black.copy(alpha = .45f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width / 2, cy), radius = w / 2),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width / 2 - w / 2, cy - h / 2),
                    size = androidx.compose.ui.geometry.Size(w, h))
            }
            androidx.compose.foundation.Image(pintura, contentDescription = c.nombre,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                modifier = Modifier.fillMaxSize(0.86f).graphicsLayer {
                    translationY = ola.value * size.height * 0.025f - size.height * 0.03f
                    rotationY = giroY()
                    rotationX = giroX()
                    cameraDistance = 14f * density
                })
        }
    }
}

/** El fondo de la pantalla de la consola: su propia imagen, enorme y difuminada, teñida con su color. */
@Composable
fun FondoConsolaCV(e: Estado, c: Consola?, modifier: Modifier = Modifier) {
    val fondo = rememberFondo(e, c)
    if (fondo.imagen != null) { FondoJuegoEmblema(fondo, modifier); return }
    val ctx = LocalContext.current
    val id = c?.let { Modelos3D.idDe(it.id) } ?: ""
    var img by remember(id) { mutableStateOf(ImagenesConsola.fondoEnMemoria(id)) }
    LaunchedEffect(id) {
        if (img == null && id.isNotEmpty() && ImagenesConsola.hay(ctx, id)) img = withContext(Dispatchers.IO) { ImagenesConsola.fondo(ctx, id) }
    }
    androidx.compose.animation.Crossfade(img, animationSpec = tween(duracionMovimiento(420)), label = "fondo", modifier = modifier) { pintura ->
        if (pintura != null) androidx.compose.foundation.Image(pintura, contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            filterQuality = androidx.compose.ui.graphics.FilterQuality.Low,
            alpha = .30f, modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.25f; scaleY = 1.25f })
    }
}

/** El fondo de la consola, calculado solo cuando cambia algo que lo afecta (no en cada cuadro). */
@Composable
fun rememberFondo(e: Estado, c: Consola?): FondoConsola =
    remember(c?.id, e.consolasPropias[c?.id ?: ""], e.extra.fondoConsolas, e.juegosPorConsola[c?.id ?: ""], e.datos) { fondoDeConsola(e, c) }

/**
 * El juego emblema de fondo: su imagen grande apenas difuminada (que se note cuál es) y oscurecida hacia el lado del
 * texto, con su nombre en una píldora chica abajo a la derecha. Cambia con un fundido al pasar de consola.
 */
@Composable
private fun FondoJuegoEmblema(fondo: FondoConsola, modifier: Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    androidx.compose.animation.Crossfade(fondo, animationSpec = tween(duracionMovimiento(480)), label = "emblema", modifier = modifier) { f ->
        Box(Modifier.fillMaxSize()) {
            val img = rememberImagen(f.imagen, 1280)
            if (img != null) androidx.compose.foundation.Image(img, contentDescription = f.juego,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop, alpha = .92f,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.04f; scaleY = 1.04f }
                    .then(if (android.os.Build.VERSION.SDK_INT >= 31) Modifier.blur(esc.d(3)) else Modifier))
            // Oscurece donde va el texto (izquierda y abajo) para que se lea, y deja el arte más limpio a la derecha.
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to col.fondo.copy(alpha = .82f), .45f to col.fondo.copy(alpha = .38f), 1f to Color.Transparent)))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(.6f to Color.Transparent, 1f to col.fondo.copy(alpha = .6f))))
            if (img != null && f.juego != null) Row(
                Modifier.align(Alignment.TopEnd).padding(end = esc.d(64), top = esc.d(120)).clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = .38f)).padding(horizontal = esc.d(18), vertical = esc.d(8)),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10)),
            ) {
                TB(when (f.modo) { "historico" -> "EMBLEMA"; "emblema" -> "TU MÁS JUGADO"; else -> "DE FONDO" }, 15f, color = Color.White.copy(alpha = .7f), peso = FontWeight.ExtraBold, espaciado = 2f)
                TB(f.juego, 20f, color = Color.White, peso = FontWeight.ExtraBold, traducir = false)
            }
        }
    }
}

// ———————————————————————————— 6a · Thor arriba ————————————————————————————

@Composable
fun CarpetasArribaCV(e: Estado) {
    val c = Consolas.porId(e.consolaSel) ?: e.consolasConJuegos.firstOrNull()
    LienzoBoceto(1920f, 1080f, fondo = LocalColeccion.current.fondo, zoom = ZOOM_ARRIBA) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val color = c?.color ?: col.acento
        FondoConsolaCV(e, c, Modifier.fillMaxSize())
        val conArte = rememberFondo(e, c).imagen != null
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(color.copy(alpha = if (conArte) .14f else .38f), Color.Transparent), radius = 1600f)))
        FondoTemaCV()
        val d1 = duracionMovimiento(280); val d2 = duracionMovimiento(360); val d3 = duracionMovimiento(180); val d4 = duracionMovimiento(220)
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(64), vertical = esc.d(28))) {
            Row(Modifier.fillMaxWidth().height(esc.d(92)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                TB("TUS CONSOLAS", 20f, color = col.sub, peso = FontWeight.ExtraBold, espaciado = 3f)
                Spacer(Modifier.weight(1f))
                Box(Modifier.anclaRecorrido("widgets")) { BotonWidgetsCV() }; HoraBateria(60f, 24f); BotonPerfil(82f)
            }
            if (c == null) { TB("Aún no hay consolas", 64f, titulo = true); return@Column }
            // En la pantalla angosta (con las pantallas intercambiadas) la consola va arriba y su información abajo,
            // a todo el ancho; en la ancha, lado a lado.
            val angosto = anchoLienzo() < 1350f
            Box(Modifier.fillMaxWidth().weight(1f)) {
            // Cambio de consola: la de antes se aleja y la nueva llega (zoom out / zoom in).
            androidx.compose.animation.AnimatedContent(
                targetState = c.id,
                transitionSpec = {
                    (androidx.compose.animation.fadeIn(tween(d1)) + androidx.compose.animation.scaleIn(tween(d2, easing = CurvaPosarse), initialScale = 0.86f)) togetherWith
                        (androidx.compose.animation.fadeOut(tween(d3)) + androidx.compose.animation.scaleOut(tween(d4), targetScale = 0.86f))
                }, label = "consola", modifier = Modifier.fillMaxSize(),
            ) { id ->
                val k = Consolas.porId(id) ?: return@AnimatedContent
                if (angosto) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
                        Spacer(Modifier.weight(1f))
                        InfoConsolaCV(e, k, tamNombre = 64f, lineasHistoria = 3)
                        PersonalizarConsolaCV(e, k)
                    }
                } else Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(esc.d(28))) {
                    Column(Modifier.weight(0.56f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(14), Alignment.CenterVertically)) {
                        InfoConsolaCV(e, k, tamNombre = 72f, lineasHistoria = 2)
                        PersonalizarConsolaCV(e, k)
                    }
                    Spacer(Modifier.weight(0.44f))
                }
            }
            // La consola va fuera de la animación: cambia de imagen con su propio fundido.
            if (angosto) {
                Column(Modifier.fillMaxWidth().fillMaxHeight(0.46f).align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                    Consola3D(c, Modifier.fillMaxWidth().weight(1f))
                }
            } else Column(Modifier.fillMaxWidth(0.44f).fillMaxHeight().align(Alignment.CenterEnd), horizontalAlignment = Alignment.CenterHorizontally) {
                Consola3D(c, Modifier.fillMaxWidth().weight(1f))
                Modelos3D.credito(c.id)?.let { TB(it, 16f, color = col.tenue, lineas = 1, traducir = false) }
            }
            }
            // Acciones a la izquierda; a la derecha cómo cambiar de consola y de vista (solo si cabe).
            Row(Modifier.fillMaxWidth().height(esc.d(90)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                AccionesConsolaCV(e, c, alto = 80f)
                Spacer(Modifier.weight(1f))
                if (!angosto) PistasFinas("L1 R1" to "Otra consola", "Select" to "Vista", "Mantén Select" to "Cambiar pantallas", modifier = Modifier.anclaRecorrido("controles"))
            }
        }
    }
}

@Composable
fun InfoConsolaCV(e: Estado, c: Consola, tamNombre: Float, lineasHistoria: Int = 4) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val f = HistoriaConsolas.de(c.id)
    val juegos = e.juegosPorConsola[c.id].orEmpty()
    val horas = juegos.sumOf { e.datos[it.clave]?.tiempo ?: 0L }
    Column(Modifier.anclaRecorrido("consola"), verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
        if (f != null) TB("${f.marca} · ${f.anio} · ${f.generacion}", 22f, color = androidx.compose.ui.graphics.lerp(c.color, Color.White, .55f), peso = FontWeight.ExtraBold, espaciado = 3f)
        val conLogo = c.id in e.extra.logoConsolas
        val logoUri = e.consolasPropias[c.id]?.logo?.let { android.net.Uri.parse(it) }
        val logo = if (conLogo) rememberImagen(logoUri, 900) else null
        if (conLogo && logo != null) androidx.compose.foundation.Image(logo, c.bonito, contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            alignment = Alignment.CenterStart, modifier = Modifier.height(esc.d(tamNombre * 1.3f)).fillMaxWidth())
        else if (conLogo) Box(Modifier.size(esc.d(620), esc.d(150)).border(3.dp, col.texto.copy(alpha = .3f), RoundedCornerShape(esc.d(24)))
            .clickable { Diptico.cambiarIconoCarpeta(c.id) }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TB("Logo de ${c.bonito}", 24f, peso = FontWeight.ExtraBold)
                TB("Elígelo en Ajustes › Tus consolas › Logo", 17f, color = col.tenue)
            }
        }
        else TB(e.consolasPropias[c.id]?.nombre ?: c.bonito, tamNombre, titulo = true, lineas = 2, interlineado = tamNombre * 0.95f, traducir = false)
        Box(Modifier.height(esc.d(48)).clip(RoundedCornerShape(50)).background(col.vidrio).border(1.dp, col.texto.copy(alpha = .18f), RoundedCornerShape(50))
            .clickable { Diptico.alternarLogoConsola(c.id) }.padding(horizontal = esc.d(20)), contentAlignment = Alignment.Center) {
            TB(if (conLogo) "Mostrar el nombre" else "Mostrar el logo", 19f, peso = FontWeight.ExtraBold)
        }
        if (f != null) TB(f.historia, 23f, color = Color(0xFFD3D5EA).let { if (col.texto.luminance() > .5f) it else col.sub }, peso = FontWeight.SemiBold, lineas = lineasHistoria, interlineado = 32f)
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
            ChipDato("${juegos.size} juegos", 21f)
            if (horas > 0) ChipDato("${formatoTiempo(horas)} jugadas", 21f)
        }
    }
}


/** Las 6 opciones de la consola (X): ícono, vista, color, emulador, fondo y música, nombre o logo. */
/**
 * Las opciones de la consola juntas en una sola tarjeta ordenada (antes eran seis casillas sueltas): un resumen de cómo
 * está cada cosa y un solo lugar para cambiarlas (X).
 */
@Composable
fun PersonalizarConsolaCV(e: Estado, c: Consola) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val cp = e.consolasPropias[c.id]
    val resumen = listOf(
        "Vista" to nombreVistaCarpetas(e.extra.vistaCarpetasCV),
        "Emulador" to Diptico.nombreEmulador(c),
        "Ícono" to if (cp?.portadaCarpeta != null) "Propio" else "Original",
    )
    Row(
        Modifier.fillMaxWidth().height(esc.d(132)).vidrio(30f, fuerte = true).clickable { Diptico.abrirEditarConsola(c.id) }
            .padding(horizontal = esc.d(22)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(20)),
    ) {
        Box(Modifier.size(esc.d(84)).clip(RoundedCornerShape(esc.d(24))).background(c.color.copy(alpha = .3f)), contentAlignment = Alignment.Center) {
            IconoDiptico(Icono.AJUSTES, col.texto, 44f, acento = Coral)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
            TB("Personalizar ${c.bonito}", 28f, titulo = true, lineas = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                resumen.forEach { (n, v) ->
                    Row(Modifier.clip(RoundedCornerShape(50)).background(col.texto.copy(alpha = .08f)).padding(horizontal = esc.d(12), vertical = esc.d(5)),
                        horizontalArrangement = Arrangement.spacedBy(esc.d(5))) {
                        TB(n, 16f, color = col.sub, lineas = 1)
                        TB(v, 16f, peso = FontWeight.ExtraBold, lineas = 1, traducir = false)
                    }
                }
            }
        }
        Box(Modifier.size(esc.d(54)).clip(CircleShape).border(2.dp, col.texto, CircleShape), contentAlignment = Alignment.Center) { TB("X", 22f, peso = FontWeight.ExtraBold, traducir = false) }
    }
}


@Composable
private fun AccionesConsolaCV(e: Estado, c: Consola, alto: Float = 84f) {
    val esc = LocalEscala.current
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
        BotonTecla("A", "Entrar", { Diptico.entrarConsolaCV(c.id) }, alto = alto, principal = true, letra = 28f)
        ultimoDeConsola(e, c.id)?.let { j -> BotonTecla("Y", "Seguir con ${j.titulo}", { Diptico.seguirCon(j) }, alto = alto, letra = 24f) }
            ?: BotonTecla("Y", "Cambiar ícono", { Diptico.cambiarIconoCarpeta(c.id) }, alto = alto, letra = 24f)
    }
}

// ———————————————————————————— 6b · Thor abajo ————————————————————————————

@Composable
fun CarpetasAbajoCV(e: Estado) {
    LienzoBoceto(1240f, 1080f, fondo = LocalColeccion.current.fondo2, zoom = ZOOM_ABAJO) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        FondoTemaCV()
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(24), vertical = esc.d(18)), verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
            // Arriba: el dock de apps a la izquierda; la hora, la batería y tu foto a la derecha.
            Row(Modifier.fillMaxWidth().height(esc.d(84)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                DockCV(e, 52f, Modifier.anclaRecorrido("apps"))
                Spacer(Modifier.weight(1f))
                HoraBateria(56f, 22f)
                BotonPerfil(70f)
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                if (e.extra.pestanaCarpetas == 1) ColeccionesCV(e, Modifier.fillMaxSize())
                else RejillaCarpetasCV(e, Modifier.fillMaxSize(), unaPantalla = false)
            }
            // Abajo, sin instrucciones: Consolas en la esquina izquierda, DIPPO al medio y Colecciones a la derecha.
            Box(Modifier.fillMaxWidth().height(esc.d(92))) {
                BotonPestanaCV(Icono.MANDO, "Consolas", e.extra.pestanaCarpetas == 0, Modifier.align(Alignment.CenterStart)) { Diptico.elegirPestanaCarpetas(0) }
                if (e.extra.dippoInicio) Box(Modifier.align(Alignment.Center).size(esc.d(88)).anclaRecorrido("dippo").clip(CircleShape).clickable { Diptico.abrirAsistente() },
                    contentAlignment = Alignment.Center) {
                    cl.nikalbazu.diptico.bisa.AvatarBisa(78f * esc.factor)
                }
                BotonPestanaCV(Icono.BIBLIOTECA, "Colecciones", e.extra.pestanaCarpetas == 1, Modifier.align(Alignment.CenterEnd)) { Diptico.elegirPestanaCarpetas(1) }
            }
        }
    }
}

/** Botón redondo de esquina con un ícono: lleno cuando es la pestaña que se ve. */
@Composable
private fun BotonPestanaCV(icono: Icono, nombre: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Box(
        modifier.size(esc.d(80)).clip(CircleShape).background(if (on) col.elegido else col.vidrio)
            .then(if (on) Modifier else Modifier.border(1.dp, col.borde, CircleShape))
            .clickable(onClick = onClick)
            .semantics { contentDescription = nombre }
            .testTag("pestana-$nombre"),
        contentAlignment = Alignment.Center,
    ) { IconoDiptico(icono, if (on) col.enElegido else col.texto, 40f) }
}

/** Colecciones: por ahora, tus favoritos y lo jugado hace poco como carpetas especiales. */
@Composable
private fun ColeccionesCV(e: Estado, modifier: Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val favoritos = e.juegos.filter { e.datos[it.clave]?.favorito == true }
    val recientes = e.juegos.filter { (e.datos[it.clave]?.ultimo ?: 0L) > 1 }.sortedByDescending { e.datos[it.clave]?.ultimo ?: 0L }.take(12)
    val sinJugar = e.juegos.filter { (e.datos[it.clave]?.tiempo ?: 0L) == 0L }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
        listOf(Triple("Favoritos", "♥", favoritos), Triple("Jugados hace poco", "⏱", recientes), Triple("Aún sin jugar", "✦", sinJugar)).forEach { (n, s, l) ->
            Row(Modifier.fillMaxWidth().height(esc.d(150)).vidrio(26f, fuerte = true).clickable {
                l.firstOrNull()?.let { Diptico.elegirJuego(it.clave); Diptico.entrarSistema() }
            }.padding(esc.d(16)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                Box(Modifier.size(esc.d(110)).clip(RoundedCornerShape(esc.d(26))).background(Brush.linearGradient(listOf(col.acento, Color(0xFF0D0F1C)))), contentAlignment = Alignment.Center) {
                    TB(s, 44f, color = Color.White, traducir = false)
                }
                Column(Modifier.weight(1f)) {
                    TB(n, 28f, titulo = true)
                    TB("${l.size} juegos", 20f, color = col.sub, traducir = false)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    l.take(3).forEach { j -> Box(Modifier.size(esc.d(64), esc.d(90)).clip(RoundedCornerShape(esc.d(6)))) { ArteJuego(j, preferirArte = false, maxLado = 200, modifier = Modifier.fillMaxSize()) } }
                }
            }
        }
    }
}



/** Las carpetas de las consolas en la vista elegida: Ícono, Díptico, Repisa o Carpeta. */
@Composable
fun RejillaCarpetasCV(e: Estado, modifier: Modifier, unaPantalla: Boolean) {
    val esc = LocalEscala.current
    val ids = e.pestanas.filter { it != "todo" }
    val sel = ids.indexOf(e.consolaSel).coerceAtLeast(0)
    val vista = e.extra.vistaCarpetasCV
    val columnas = columnasCarpetasCV(vista, unaPantalla).let {
        if (it < 0) e.extra.columnasIconoCV else if (vista == "grande" && e.extra.columnasGrandesCV > 0) e.extra.columnasGrandesCV else it
    }
    androidx.compose.runtime.SideEffect { Teclas.carrilesCarpetas = columnas }
    val estado = rememberLazyGridState()
    LaunchedEffect(sel, vista) { estado.animateScrollToItem((sel - columnas).coerceAtLeast(0)) }
    // Pellizcar con dos dedos (vistas Ícono y Grandes): abrir agranda las carpetas (menos por fila), cerrar las achica.
    val columnasAhora = androidx.compose.runtime.rememberUpdatedState(columnas)
    val pellizco = if (vista != "icono" && vista != "grande") Modifier else Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var acumulado = 1f
            while (true) {
                val ev = awaitPointerEvent()
                if (ev.changes.none { it.pressed }) break
                if (ev.changes.count { it.pressed } >= 2) {
                    acumulado *= ev.calculateZoom()
                    ev.changes.forEach { it.consume() }
                    val cambio = if (acumulado > 1.18f) -1 else if (acumulado < 0.85f) 1 else 0
                    if (cambio != 0) {
                        if (vista == "grande") Diptico.ajustarColumnasGrandes(columnasAhora.value + cambio) else Diptico.ajustarColumnasIcono(columnasAhora.value + cambio)
                        acumulado = 1f
                    }
                }
            }
        }
    }
    LazyVerticalGrid(GridCells.Fixed(columnas), modifier.then(pellizco), estado,
        horizontalArrangement = Arrangement.spacedBy(esc.d(if (vista == "icono") 10f else 24f)),
        verticalArrangement = Arrangement.spacedBy(esc.d(if (vista == "repisa") 12f else 22f)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = esc.d(10))) {
        itemsIndexed(ids, key = { _, id -> id }) { i, id ->
            val c = Consolas.porId(id) ?: return@itemsIndexed
            val elegida = i == sel
            val entrar = { if (elegida) Diptico.entrarConsolaCV(id) else Diptico.elegirConsola(id) }
            // En el recorrido iluminamos la carpeta elegida, no toda la rejilla.
            Box(Modifier.fillMaxWidth().then(if (elegida) Modifier.anclaRecorrido("carpetas") else Modifier)) {
                when (vista) {
                    "grande" -> CarpetaGrande(e, c, elegida, unaPantalla, entrar)
                    "icono" -> CarpetaIcono(e, c, elegida, unaPantalla, entrar)
                    "repisa" -> CarpetaRepisa(e, c, elegida, unaPantalla, entrar)
                    "carpeta" -> CarpetaClasica(e, c, elegida, unaPantalla, entrar)
                    else -> CarpetaDiptico(e, c, elegida, unaPantalla, entrar)
                }
            }
        }
    }
}

@Composable
private fun anilloCV(elegida: Boolean, forma: androidx.compose.ui.graphics.Shape): Modifier {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    return if (elegida) Modifier.border(esc.d(4), col.elegido, forma) else Modifier
}

/** El ícono de la carpeta: el propio (6d) o el degradado del color con la sigla. */
@Composable
private fun IconoCarpetaCV(e: Estado, c: Consola, modifier: Modifier, sigla: Float) {
    val propia = e.consolasPropias[c.id]?.portadaCarpeta
    // Con un ícono propio NO va el degradado del color de la consola detrás: el ícono reemplaza a la carpeta por completo.
    val fondo = if (propia != null) Modifier.background(Color(0xFF10121F)) else Modifier.background(Brush.linearGradient(listOf(c.color, Color(0xFF0D0F1C))))
    Box(modifier.then(fondo), contentAlignment = Alignment.Center) {
        if (propia != null) PortadaCarpetaRuta(propia, c.id, Modifier.fillMaxSize(), maxLado = 360)
        else IconoConsola(c, sigla, Color.Transparent)
    }
}

@Composable
private fun CarpetaIcono(e: Estado, c: Consola, elegida: Boolean, una: Boolean, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val forma = RoundedCornerShape(24)
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        IconoCarpetaCV(e, c, Modifier.fillMaxWidth(0.88f).aspectRatio(1f).graphicsLayer { scaleX = if (elegida) 1.05f else 1f; scaleY = scaleX }
            .clip(forma).then(anilloCV(elegida, forma)), 40f)
        TB(c.bonito, 20f, color = if (elegida) col.texto else col.sub)
    }
}

/**
 * Vista «Grandes»: una tarjeta del color de la consola con su foto de estudio, el nombre y cuántos juegos tiene.
 * Tres por fila con dos pantallas (cuatro con una): grandes, ordenadas y fáciles de apuntar con el dedo.
 */
@Composable
private fun CarpetaGrande(e: Estado, c: Consola, elegida: Boolean, una: Boolean, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val forma = RoundedCornerShape(esc.d(34))
    val ctx = LocalContext.current
    val id = Modelos3D.idDe(c.id)
    val propia = e.consolasPropias[c.id]?.portadaCarpeta
    var img by remember(id) { mutableStateOf(ImagenesConsola.enMemoria(id)) }
    LaunchedEffect(id) { if (img == null && propia == null && ImagenesConsola.hay(ctx, id)) img = withContext(Dispatchers.IO) { ImagenesConsola.cargar(ctx, id) } }
    val n = e.juegosPorConsola[c.id]?.size ?: 0
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        // La tarjeta es un cuadrado: o tu ícono tal cual (sin recortar ni color de la consola detrás), o la foto de la consola.
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .graphicsLayer { scaleX = if (elegida) 1.04f else 1f; scaleY = scaleX }
                .clip(forma).background(if (propia != null) Color(0xFF10121F) else Color.Transparent)
                .then(if (propia == null) Modifier.background(Brush.linearGradient(listOf(c.color, Color(0xFF141526)))) else Modifier)
                .then(anilloCV(elegida, forma)).testTag("carpeta-grande-${c.id}"),
        ) {
            val pintura = img
            when {
                propia != null -> PortadaCarpetaRuta(propia, c.id, Modifier.fillMaxSize(), maxLado = 512, escala = androidx.compose.ui.layout.ContentScale.Fit)
                pintura != null -> androidx.compose.foundation.Image(pintura, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.Low,
                    modifier = Modifier.fillMaxSize(0.86f).align(Alignment.Center))
                else -> Box(Modifier.align(Alignment.Center)) { IconoConsola(c, 70f, Color.Transparent) }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = esc.d(4))) {
            TB(c.bonito, 22f, color = if (elegida) col.texto else col.sub, peso = FontWeight.ExtraBold, lineas = 1)
            TB(cuantosJuegos(n), 16f, color = col.sub.copy(alpha = .8f), peso = FontWeight.Bold, traducir = false, lineas = 1)
        }
    }
}

/** Los mini-juegos al lado del ícono (los últimos, los favoritos o los elegidos; 1 a 3). */
@Composable
private fun MiniJuegos(e: Estado, c: Consola, ancho: Float, alto: Float) {
    val esc = LocalEscala.current
    val juegos = juegosJuntoAlIcono(e, c.id, e.extra.juegosCarpeta, e.extra.cuantosCarpeta)
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
        juegos.forEach { j ->
            Box(Modifier.size(esc.d(ancho), esc.d(alto)).clip(RoundedCornerShape(esc.d(5))).background(c.color.copy(alpha = .4f))) {
                ArteJuego(j, preferirArte = false, maxLado = 200, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun CarpetaDiptico(e: Estado, c: Consola, elegida: Boolean, una: Boolean, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val alto = if (una) 150f else 150f
    val forma = RoundedCornerShape(esc.d(26))
    Row(Modifier.fillMaxWidth().height(esc.d(alto)).clip(forma).background(col.vidrioFuerte).border(1.dp, col.borde, forma).then(anilloCV(elegida, forma)).clickable(onClick = onClick)) {
        IconoCarpetaCV(e, c, Modifier.width(esc.d(if (una) alto * 0.7f else alto)).fillMaxHeight(), 40f)
        Box(Modifier.width(esc.d(6)).fillMaxHeight().background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .35f), Color.White.copy(alpha = .08f)))))
        Column(Modifier.weight(1f).fillMaxHeight().padding(esc.d(16)), verticalArrangement = Arrangement.SpaceBetween) {
            MiniJuegos(e, c, 46f, 64f)
            Column {
                TB(c.bonito, 24f, titulo = true, peso = FontWeight.SemiBold)
                TB(cuantosJuegos(e.juegosPorConsola[c.id].orEmpty().size), 19f, color = col.sub)
            }
        }
    }
}

@Composable
private fun CarpetaRepisa(e: Estado, c: Consola, elegida: Boolean, una: Boolean, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val forma = RoundedCornerShape(esc.d(26))
    val juegos = e.juegosPorConsola[c.id].orEmpty()
    Row(Modifier.fillMaxWidth().height(esc.d(if (una) 160f else 190f)).clip(forma)
        .background(if (elegida) col.texto.copy(alpha = .08f) else Color.Transparent)
        .border(if (elegida) esc.d(3) else 1.dp, if (elegida) col.elegido else Color.Transparent, forma)
        .clickable(onClick = onClick).padding(horizontal = esc.d(24)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(20))) {
        Column(Modifier.width(esc.d(200)), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
            IconoCarpetaCV(e, c, Modifier.size(esc.d(56)).clip(RoundedCornerShape(esc.d(16))), 24f)
            TB(c.bonito, 24f, titulo = true, peso = FontWeight.SemiBold, lineas = 2, interlineado = 26f)
            TB(cuantosJuegos(juegos.size), 19f, color = col.sub)
        }
        // Los lomos de pie sobre la repisa (los primeros juegos de la consola, con su portada recortada).
        Box(Modifier.weight(1f).height(esc.d(if (una) 120f else 160f))) {
            Row(Modifier.fillMaxSize().padding(start = esc.d(10)), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                val lomos = juegosJuntoAlIcono(e, c.id, e.extra.juegosCarpeta, 3) + juegos.take(12)
                lomos.distinctBy { it.clave }.take(12).forEachIndexed { k, j ->
                    Box(Modifier.width(esc.d(30f + (k * 7) % 14)).fillMaxHeight(0.74f + ((k * 13) % 22) / 100f).clip(RoundedCornerShape(esc.d(4)))
                        .background(c.color.copy(alpha = .5f))) {
                        ArteJuego(j, preferirArte = false, maxLado = 120, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().offset(y = esc.d(10)).height(esc.d(10)).clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .22f), Color.White.copy(alpha = .04f)))))
        }
    }
}

@Composable
private fun CarpetaClasica(e: Estado, c: Consola, elegida: Boolean, una: Boolean, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val alto = if (una) 170f else 220f
    val juegos = juegosJuntoAlIcono(e, c.id, e.extra.juegosCarpeta, e.extra.cuantosCarpeta)
    Box(Modifier.fillMaxWidth().height(esc.d(alto)).clickable(onClick = onClick)) {
        // La carpeta de atrás, los juegos asomando y el frente de vidrio.
        Box(Modifier.fillMaxSize().padding(start = esc.d(10), end = esc.d(10), top = esc.d(22)).clip(RoundedCornerShape(esc.d(24))).background(c.color.copy(alpha = .53f)))
        juegos.forEachIndexed { k, j ->
            val pw = if (una) 70f else 86f
            Box(Modifier.offset(esc.d(30f + k * (pw - 6f)), esc.d(4f - k * 2f)).size(esc.d(pw), esc.d(pw * 1.38f))
                .graphicsLayer { rotationZ = listOf(-7f, 5f, -3f, 6f, -4f)[k % 5] }.clip(RoundedCornerShape(esc.d(6)))) {
                ArteJuego(j, preferirArte = false, maxLado = 200, modifier = Modifier.fillMaxSize())
            }
        }
        val forma = RoundedCornerShape(esc.d(24))
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(esc.d(alto * 0.6f)).clip(forma).background(col.vidrioFuerte)
            .border(1.dp, Color.White.copy(alpha = .14f), forma).then(anilloCV(elegida, forma)).padding(esc.d(18)),
            verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
            IconoCarpetaCV(e, c, Modifier.size(esc.d(if (una) 52f else 60f)).clip(RoundedCornerShape(esc.d(16))), 22f)
            Column {
                TB(c.bonito, if (una) 23f else 26f, titulo = true, peso = FontWeight.SemiBold)
                TB(cuantosJuegos(e.juegosPorConsola[c.id].orEmpty().size), 19f, color = col.sub)
            }
        }
    }
}

// ———————————————————————————— 6c · una pantalla ————————————————————————————

@Composable
fun CarpetasUnicaCV(e: Estado) {
    val c = Consolas.porId(e.consolaSel) ?: e.consolasConJuegos.firstOrNull()
    LienzoBoceto(1920f, 1080f, fondo = LocalColeccion.current.fondo, zoom = ZOOM_UNA) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val color = c?.color ?: col.acento
        FondoConsolaCV(e, c, Modifier.fillMaxSize())
        val conArte = rememberFondo(e, c).imagen != null
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(color.copy(alpha = if (conArte) .14f else .38f), Color.Transparent), radius = 1500f)))
        FondoTemaCV()
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(40), vertical = esc.d(18)), verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
            // Franja 1: vistas · hora, batería y perfil.
            Row(Modifier.fillMaxWidth().height(esc.d(84)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                DockCV(e, 52f, Modifier.anclaRecorrido("apps"))
                Spacer(Modifier.weight(1f))
                HoraBateria(56f, 22f); BotonPerfil(74f)
            }
            if (c == null) return@Column
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(26))) {
                // Izquierda: la consola, su historia, sus opciones y A/Y.
                Column(Modifier.weight(0.5f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(12), Alignment.CenterVertically)) {
                    InfoConsolaCV(e, c, tamNombre = 60f, lineasHistoria = 2)
                    PersonalizarConsolaCV(e, c)
                    Box(Modifier.anclaRecorrido("controles")) { AccionesConsolaCV(e, c, alto = 64f) }
                }
                // Derecha: la consola en 3D, qué juegos van junto al ícono y las carpetas.
                Column(Modifier.weight(0.5f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
                    Consola3D(c, Modifier.fillMaxWidth().weight(0.8f))
                    Modelos3D.credito(c.id)?.let { TB(it, 15f, color = col.tenue, lineas = 1, traducir = false) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        TB("JUNTO AL ÍCONO", 15f, color = Color(0xFF9A9CB8), peso = FontWeight.ExtraBold, espaciado = 1.4f)
                        listOf("ultimos" to "Últimos", "favoritos" to "Favoritos", "elegidos" to "Elegidos").forEach { (m, n) ->
                            ChipElegir(n, e.extra.juegosCarpeta == m) { Diptico.elegirJuegosCarpeta(m) }
                        }
                        Spacer(Modifier.weight(1f))
                        (1..5).forEach { q -> ChipElegir("$q", e.extra.cuantosCarpeta == q) { Diptico.elegirCuantosCarpeta(q) } }
                    }
                    RejillaCarpetasCV(e, Modifier.fillMaxWidth().weight(1.2f), unaPantalla = true)
                }
            }
            // Abajo: DIPPO y el dock de apps.
            Row(Modifier.fillMaxWidth().height(esc.d(84)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                if (e.extra.dippoInicio) DippoFrase("Elige una consola y aprieta A. Select cambia cómo se ven las carpetas; si lo mantienes, cambias de pantalla.", Modifier.weight(1f).anclaRecorrido("dippo"), cara = 58f, letra = 20f, maxAncho = 900f)
                else Spacer(Modifier.weight(1f))
                BotonRedondo(Icono.BUSCAR, 64f, { Diptico.abrirBuscar() }, descripcion = "Buscar")
                BotonRedondo(Icono.AJUSTES, 64f, { Diptico.abrirMenu() }, descripcion = "Ajustes")
            }
        }
    }
}


@Composable
private fun ChipElegir(texto: String, on: Boolean, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Box(Modifier.height(esc.d(44)).clip(RoundedCornerShape(50)).background(if (on) col.elegido else col.vidrioFuerte).border(1.dp, col.borde, RoundedCornerShape(50))
        .clickable(onClick = onClick).padding(horizontal = esc.d(16)), contentAlignment = Alignment.Center) {
        TB(texto, 18f, color = if (on) col.enElegido else col.texto, peso = FontWeight.ExtraBold)
    }
}

/** «1 juego», «3 juegos». */
internal fun cuantosJuegos(n: Int) = if (n == 1) "1 juego" else "$n juegos"

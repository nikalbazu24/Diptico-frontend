package cl.nikalbazu.diptico.ui

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

// ———————————————————————————— la consola en 3D ————————————————————————————

/** Una sola hoja de giro en memoria a la vez (pesa ~47 MB descomprimida): al cambiar de consola se suelta la anterior. */
private object HojasGiro {
    private var id: String? = null
    private var hoja: ImageBitmap? = null
    @Synchronized fun de(ctx: android.content.Context, consola: String): ImageBitmap? {
        if (consola == id) return hoja
        val bmp = runCatching { ctx.assets.open("consolas3d/$consola.webp").use { BitmapFactory.decodeStream(it) } }.getOrNull()
        id = consola; hoja = bmp?.asImageBitmap()
        return hoja
    }
    fun existe(ctx: android.content.Context, consola: String): Boolean =
        runCatching { ctx.assets.list("consolas3d")?.contains("$consola.webp") == true }.getOrDefault(false)
}

private const val CUADROS = 72
private const val COLUMNAS_HOJA = 8

/**
 * La consola en 3D: un giro de 72 cuadros hecho desde el modelo que mandó Nicolás (va en el equipo, sin internet).
 * Gira sola despacio; con el dedo se gira libre y sigue con inercia al soltar. Sin modelo: un recuadro punteado.
 */
@Composable
fun Consola3D(c: Consola, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val mov = LocalMovimiento.current
    val hoja by produceState<ImageBitmap?>(null, c.id) { value = withContext(Dispatchers.IO) { HojasGiro.de(ctx, c.id) } }
    var angulo by remember(c.id) { mutableStateOf(18f) }
    var vel by remember { mutableStateOf(0f) }
    var tocando by remember { mutableStateOf(false) }
    LaunchedEffect(c.id, mov) {
        while (true) {
            withFrameNanos { }
            if (!tocando) {
                angulo += (if (mov) 0.15f else 0f) + vel
                vel *= 0.93f
                if (abs(vel) < 0.01f) vel = 0f
            }
        }
    }
    Box(modifier.testTag("consola-3d").pointerInput(c.id) {
        detectHorizontalDragGestures(
            onDragStart = { tocando = true; vel = 0f },
            onDragEnd = { tocando = false }, onDragCancel = { tocando = false },
        ) { ch, dx -> ch.consume(); val d = dx / (5f * density / 2f); angulo += d; vel = d * 0.6f }
    }, contentAlignment = Alignment.Center) {
        val h = hoja
        if (h == null) {
            Box(Modifier.fillMaxSize(0.8f).border(3.dp, col.texto.copy(alpha = .25f), RoundedCornerShape(esc.d(32))), contentAlignment = Alignment.Center) {
                TB("Aquí va el modelo 3D de ${c.nombreLargo}", 24f, color = col.tenue, lineas = 2)
            }
            return@Box
        }
        val fw = h.width / COLUMNAS_HOJA
        val fh = h.height / ((CUADROS + COLUMNAS_HOJA - 1) / COLUMNAS_HOJA)
        Canvas(Modifier.fillMaxSize()) {
            val f = ((Math.round(angulo / 5f) % CUADROS) + CUADROS) % CUADROS
            val escala = minOf(size.width / fw, size.height / fh)
            val dw = (fw * escala).toInt(); val dh = (fh * escala).toInt()
            drawImage(
                h, srcOffset = IntOffset((f % COLUMNAS_HOJA) * fw, (f / COLUMNAS_HOJA) * fh), srcSize = IntSize(fw, fh),
                dstOffset = IntOffset(((size.width - dw) / 2).toInt(), ((size.height - dh) / 2).toInt()), dstSize = IntSize(dw, dh),
                filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium,
            )
        }
    }
}

// ———————————————————————————— 6a · Thor arriba ————————————————————————————

@Composable
fun CarpetasArribaCV(e: Estado) {
    val c = Consolas.porId(e.consolaSel) ?: e.consolasConJuegos.firstOrNull()
    LienzoBoceto(1920f, 1080f, fondo = LocalColeccion.current.fondo) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val color = c?.color ?: col.acento
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(color.copy(alpha = .4f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(1300f * esc.factor * 2f, 500f * esc.factor * 2f), radius = 1400f * esc.factor)))
        // Barra: hora, batería y perfil.
        Row(Modifier.align(Alignment.TopEnd).padding(end = esc.d(96), top = esc.d(44)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(18))) {
            HoraBateria(); BotonPerfil(72f)
        }
        if (c == null) {
            TB("Aún no hay consolas", 72f, titulo = true, modifier = Modifier.align(Alignment.CenterStart).padding(start = esc.d(96)))
            return@LienzoBoceto
        }
        // Cambio de consola: la de antes se aleja y la nueva llega (zoom out / zoom in).
        androidx.compose.animation.AnimatedContent(
            targetState = c.id,
            transitionSpec = {
                (androidx.compose.animation.fadeIn(tween(duracionMovimiento(280))) + androidx.compose.animation.scaleIn(tween(duracionMovimiento(360), easing = CurvaPosarse), initialScale = 0.86f)) togetherWith
                    (androidx.compose.animation.fadeOut(tween(duracionMovimiento(180))) + androidx.compose.animation.scaleOut(tween(duracionMovimiento(220)), targetScale = 0.86f))
            }, label = "consola", modifier = Modifier.fillMaxSize(),
        ) { id ->
            val k = Consolas.porId(id) ?: return@AnimatedContent
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.offset(esc.d(96), esc.d(190)).width(esc.d(860)), verticalArrangement = Arrangement.spacedBy(esc.d(20))) {
                    InfoConsolaCV(e, k, tamNombre = 116f)
                    OpcionesConsolaCV(e, k, columnas = 3)
                    AccionesConsolaCV(e, k)
                }
                Consola3D(k, Modifier.offset(esc.d(1080), esc.d(170)).size(esc.d(760), esc.d(560)))
                Modelos3D.credito(k.id)?.let { TB("$it · arrástrala con el dedo", 18f, color = col.tenue, modifier = Modifier.offset(esc.d(1080), esc.d(740)).width(esc.d(760)), traducir = false) }
            }
        }
        // L1 ◀ nombre ▶ R1 y el sonido de encendido.
        Row(Modifier.align(Alignment.BottomEnd).padding(end = esc.d(96), bottom = esc.d(92)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
            BotonTecla("L1", "◀", { Diptico.cambiarConsola(-1) }, alto = 72f, letra = 26f)
            TB(c.nombreLargo, 30f, titulo = true)
            BotonTecla("R1", "▶", { Diptico.cambiarConsola(1) }, alto = 72f, letra = 26f)
            Box(Modifier.height(esc.d(64)).vidrio(32f).clickable { cl.nikalbazu.diptico.audio.Sonidos.encendido() }.padding(horizontal = esc.d(22)), contentAlignment = Alignment.Center) {
                TB("♪ Sonido de encendido", 20f)
            }
        }
    }
}

/** «SONY · 2000 · SEXTA GENERACIÓN», el nombre (o el logo), la historia y los chips. */
@Composable
fun InfoConsolaCV(e: Estado, c: Consola, tamNombre: Float) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val f = HistoriaConsolas.de(c.id)
    val juegos = e.juegosPorConsola[c.id].orEmpty()
    val horas = juegos.sumOf { e.datos[it.clave]?.tiempo ?: 0L }
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(16))) {
        if (f != null) TB("${f.marca} · ${f.anio} · ${f.generacion}", 22f, color = androidx.compose.ui.graphics.lerp(c.color, Color.White, .55f), peso = FontWeight.ExtraBold, espaciado = 3f)
        val conLogo = c.id in e.extra.logoConsolas
        val logoUri = e.consolasPropias[c.id]?.logo?.let { android.net.Uri.parse(it) }
        val logo = if (conLogo) rememberImagen(logoUri, 900) else null
        if (conLogo && logo != null) androidx.compose.foundation.Image(logo, c.nombreLargo, contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            alignment = Alignment.CenterStart, modifier = Modifier.height(esc.d(tamNombre * 1.3f)).fillMaxWidth())
        else if (conLogo) Box(Modifier.size(esc.d(620), esc.d(150)).border(3.dp, col.texto.copy(alpha = .3f), RoundedCornerShape(esc.d(24)))
            .clickable { Diptico.cambiarIconoCarpeta(c.id) }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TB("Logo de ${c.nombreLargo}", 24f, peso = FontWeight.ExtraBold)
                TB("Elígelo en Ajustes › Tus consolas › Logo", 17f, color = col.tenue)
            }
        }
        else TB(e.consolasPropias[c.id]?.nombre ?: c.nombreLargo, tamNombre, titulo = true, lineas = 2, interlineado = tamNombre * 0.95f, traducir = false)
        Box(Modifier.height(esc.d(48)).clip(RoundedCornerShape(50)).background(col.vidrio).border(1.dp, col.texto.copy(alpha = .18f), RoundedCornerShape(50))
            .clickable { Diptico.alternarLogoConsola(c.id) }.padding(horizontal = esc.d(20)), contentAlignment = Alignment.Center) {
            TB(if (conLogo) "Mostrar el nombre" else "Mostrar el logo", 19f, peso = FontWeight.ExtraBold)
        }
        if (f != null) TB(f.historia, 24f, color = Color(0xFFD3D5EA).let { if (col.texto.luminance() > .5f) it else col.sub }, peso = FontWeight.SemiBold, lineas = 4, interlineado = 35f)
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
            ChipDato("${juegos.size} juegos", 22f)
            if (horas > 0) ChipDato("${formatoTiempo(horas)} jugadas", 22f)
            ultimoDeConsola(e, c.id)?.let { ChipDato("Lo último: ${it.titulo}", 22f) }
        }
    }
}


/** Las 6 opciones de la consola (X): ícono, vista, color, emulador, fondo y música, nombre o logo. */
@Composable
fun OpcionesConsolaCV(e: Estado, c: Consola, columnas: Int) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val opciones = listOf(
        Triple("Ícono de la carpeta", if (e.consolasPropias[c.id]?.portadaCarpeta != null) "Propio ›" else "Elegir ›", { Diptico.cambiarIconoCarpeta(c.id) }),
        Triple("Vista", nombreVistaCarpetas(e.extra.vistaCarpetasCV), { Diptico.siguienteVistaCarpetas() }),
        Triple("Color", if ((e.consolasPropias[c.id]?.color ?: 0L) != 0L) "Propio" else "De la consola", { Diptico.abrirEditarConsola(c.id) }),
        Triple("Emulador", Diptico.nombreEmulador(c), { Diptico.abrirEditarConsola(c.id) }),
        Triple("Fondo y música", if (e.consolasPropias[c.id]?.musica != null) "Propios" else "Del tema", { Diptico.abrirEditarConsola(c.id) }),
        Triple("Nombre o logo", if (c.id in e.extra.logoConsolas) "Logo" else "Nombre", { Diptico.alternarLogoConsola(c.id) }),
    )
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
        TB("OPCIONES DE ESTA CONSOLA · X", 19f, color = Color(0xFF9A9CB8), peso = FontWeight.ExtraBold, espaciado = 2.3f)
        opciones.chunked(columnas).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                fila.forEach { (n, v, accion) ->
                    Column(Modifier.weight(1f).height(esc.d(84)).vidrio(20f, fuerte = true).clickable { accion() }.padding(horizontal = esc.d(18)),
                        verticalArrangement = Arrangement.Center) {
                        TB(n, 18f, color = col.sub)
                        TB(v, 22f, peso = FontWeight.ExtraBold, traducir = false)
                    }
                }
                repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
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
    LienzoBoceto(1240f, 1080f, fondo = LocalColeccion.current.fondo2) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B0C16).takeIf { col.texto.luminance() > .5f } ?: col.fondo2, col.fondo))))
        // Arriba: pestañas · DIPPO con su frase · vistas.
        Row(Modifier.fillMaxWidth().padding(start = esc.d(40), end = esc.d(40), top = esc.d(28)).height(esc.d(72)), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                listOf("Consolas", "Colecciones").forEachIndexed { i, n ->
                    val on = e.extra.pestanaCarpetas == i
                    Box(Modifier.height(esc.d(60)).clip(RoundedCornerShape(50)).background(if (on) col.elegido else col.vidrio)
                        .then(if (on) Modifier else Modifier.border(1.dp, col.borde, RoundedCornerShape(50)))
                        .clickable { Diptico.elegirPestanaCarpetas(i) }.padding(horizontal = esc.d(26)), contentAlignment = Alignment.Center) {
                        TB(n, 24f, color = if (on) col.enElegido else col.texto, peso = if (on) FontWeight.ExtraBold else FontWeight.Bold)
                    }
                }
            }
            if (e.extra.dippoInicio) DippoFrase("Elige una consola y aprieta A para entrar.", Modifier.weight(1f).padding(horizontal = esc.d(18)), cara = 48f, letra = 18f)
            else Spacer(Modifier.weight(1f))
            SelectorVistasCarpetas(e)
        }
        if (e.extra.pestanaCarpetas == 1) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
                TB("Tus colecciones", 40f, titulo = true)
                TB("Agrupa juegos a tu gusto desde las opciones de cada juego.", 24f, color = col.sub)
            }
        } else RejillaCarpetasCV(e, Modifier.fillMaxSize().padding(start = esc.d(40), end = esc.d(40), top = esc.d(124), bottom = esc.d(124)), unaPantalla = false)
        // Pie: qué hace cada botón.
        Row(Modifier.align(Alignment.BottomCenter).padding(start = esc.d(40), end = esc.d(40), bottom = esc.d(28)).fillMaxWidth().height(esc.d(72)).vidrio(36f)
            .padding(horizontal = esc.d(24)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            PistaTecla("A", "Entrar"); PistaTecla("X", "Opciones"); PistaTecla("Y", "Cambiar ícono"); PistaTecla("Sel", "Vista: ${nombreVistaCarpetas(e.extra.vistaCarpetasCV)}")
        }
    }
}

@Composable
fun SelectorVistasCarpetas(e: Estado, alto: Float = 56f, letra: Float = 22f) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Row(Modifier.clip(RoundedCornerShape(50)).background(col.vidrio).border(1.dp, col.borde, RoundedCornerShape(50)).padding(esc.d(6)),
        horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
        VISTAS_CARPETAS_CV.forEach { v ->
            val on = e.extra.vistaCarpetasCV == v
            Box(Modifier.height(esc.d(alto)).clip(RoundedCornerShape(50)).background(if (on) col.elegido else Color.Transparent)
                .clickable { Diptico.elegirVistaCarpetas(v) }.padding(horizontal = esc.d(alto * 0.4f)), contentAlignment = Alignment.Center) {
                TB(nombreVistaCarpetas(v), letra, color = if (on) col.enElegido else col.texto, peso = FontWeight.ExtraBold)
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
    val columnas = columnasCarpetasCV(vista, unaPantalla)
    androidx.compose.runtime.SideEffect { Teclas.carrilesCarpetas = columnas }
    val estado = rememberLazyGridState()
    LaunchedEffect(sel, vista) { estado.animateScrollToItem((sel - columnas).coerceAtLeast(0)) }
    LazyVerticalGrid(GridCells.Fixed(columnas), modifier, estado,
        horizontalArrangement = Arrangement.spacedBy(esc.d(if (vista == "icono") 10f else 24f)),
        verticalArrangement = Arrangement.spacedBy(esc.d(if (vista == "repisa") 12f else 22f)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = esc.d(10))) {
        itemsIndexed(ids, key = { _, id -> id }) { i, id ->
            val c = Consolas.porId(id) ?: return@itemsIndexed
            val elegida = i == sel
            val entrar = { if (elegida) Diptico.entrarConsolaCV(id) else Diptico.elegirConsola(id) }
            when (vista) {
                "icono" -> CarpetaIcono(e, c, elegida, unaPantalla, entrar)
                "repisa" -> CarpetaRepisa(e, c, elegida, unaPantalla, entrar)
                "carpeta" -> CarpetaClasica(e, c, elegida, unaPantalla, entrar)
                else -> CarpetaDiptico(e, c, elegida, unaPantalla, entrar)
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
    Box(modifier.background(Brush.linearGradient(listOf(c.color, Color(0xFF0D0F1C)))), contentAlignment = Alignment.Center) {
        if (propia != null) PortadaCarpetaRuta(propia, c.id, Modifier.fillMaxSize(), maxLado = 360)
        else IconoConsola(c, sigla, Color.Transparent)
    }
}

@Composable
private fun CarpetaIcono(e: Estado, c: Consola, elegida: Boolean, una: Boolean, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val lado = if (una) 112f else 164f
    val forma = RoundedCornerShape(esc.d(lado * 0.25f))
    Column(Modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
        IconoCarpetaCV(e, c, Modifier.graphicsLayer { scaleX = if (elegida) 1.06f else 1f; scaleY = scaleX }.size(esc.d(lado)).clip(forma).then(anilloCV(elegida, forma)), lado * 0.4f)
        TB(c.nombreLargo, if (una) 20f else 22f, color = if (elegida) col.texto else col.sub)
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
    val alto = if (una) 160f else 176f
    val forma = RoundedCornerShape(esc.d(26))
    Row(Modifier.fillMaxWidth().height(esc.d(alto)).clip(forma).background(col.vidrioFuerte).border(1.dp, col.borde, forma).then(anilloCV(elegida, forma)).clickable(onClick = onClick)) {
        IconoCarpetaCV(e, c, Modifier.width(esc.d(alto)).fillMaxHeight(), 44f)
        Box(Modifier.width(esc.d(6)).fillMaxHeight().background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .35f), Color.White.copy(alpha = .08f)))))
        Column(Modifier.weight(1f).fillMaxHeight().padding(esc.d(16)), verticalArrangement = Arrangement.SpaceBetween) {
            MiniJuegos(e, c, if (una) 52f else 56f, if (una) 74f else 78f)
            Column {
                TB(c.nombreLargo, if (una) 25f else 28f, titulo = true, peso = FontWeight.SemiBold)
                TB("${e.juegosPorConsola[c.id].orEmpty().size} juegos", if (una) 18f else 20f, color = col.sub)
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
            TB(c.nombreLargo, 26f, titulo = true, peso = FontWeight.SemiBold)
            TB("${juegos.size} juegos", 19f, color = col.sub)
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
    val alto = if (una) 170f else 250f
    val juegos = juegosJuntoAlIcono(e, c.id, e.extra.juegosCarpeta, e.extra.cuantosCarpeta)
    Box(Modifier.fillMaxWidth().height(esc.d(alto)).clickable(onClick = onClick)) {
        // La carpeta de atrás, los juegos asomando y el frente de vidrio.
        Box(Modifier.fillMaxSize().padding(start = esc.d(10), end = esc.d(10), top = esc.d(22)).clip(RoundedCornerShape(esc.d(24))).background(c.color.copy(alpha = .53f)))
        juegos.forEachIndexed { k, j ->
            val pw = if (una) 70f else 86f
            Box(Modifier.offset(esc.d(30f + k * (pw - 6f)), esc.d(4f - k * 2f)).size(esc.d(pw), esc.d(pw * 1.38f))
                .graphicsLayer { rotationZ = listOf(-7f, 5f, -3f)[k] }.clip(RoundedCornerShape(esc.d(6)))) {
                ArteJuego(j, preferirArte = false, maxLado = 200, modifier = Modifier.fillMaxSize())
            }
        }
        val forma = RoundedCornerShape(esc.d(24))
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(esc.d(alto * 0.6f)).clip(forma).background(col.vidrioFuerte)
            .border(1.dp, Color.White.copy(alpha = .14f), forma).then(anilloCV(elegida, forma)).padding(esc.d(18)),
            verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
            IconoCarpetaCV(e, c, Modifier.size(esc.d(if (una) 52f else 60f)).clip(RoundedCornerShape(esc.d(16))), 22f)
            Column {
                TB(c.nombreLargo, if (una) 23f else 26f, titulo = true, peso = FontWeight.SemiBold)
                TB("${e.juegosPorConsola[c.id].orEmpty().size} juegos", 19f, color = col.sub)
            }
        }
    }
}

// ———————————————————————————— 6c · una pantalla ————————————————————————————

@Composable
fun CarpetasUnicaCV(e: Estado) {
    val c = Consolas.porId(e.consolaSel) ?: e.consolasConJuegos.firstOrNull()
    LienzoBoceto(1920f, 1080f, fondo = LocalColeccion.current.fondo) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val color = c?.color ?: col.acento
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(color.copy(alpha = .4f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(576f * esc.factor * 2.6f, 432f * esc.factor * 2.6f), radius = 1300f * esc.factor * 1.4f)))
        // Franja 1: vistas a la izquierda; hora, batería y perfil a la derecha.
        Row(Modifier.fillMaxWidth().padding(start = esc.d(48), end = esc.d(48), top = esc.d(24)).height(esc.d(72)), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
            SelectorVistasCarpetas(e)
            Spacer(Modifier.weight(1f))
            HoraBateria(60f, 24f); BotonPerfil(64f)
        }
        if (c == null) return@LienzoBoceto
        // Izquierda: la consola, su historia y sus opciones (centrado en su franja: sin hueco abajo).
        Column(Modifier.offset(esc.d(48), esc.d(124)).width(esc.d(820)).height(esc.d(916)), verticalArrangement = Arrangement.spacedBy(esc.d(16), Alignment.CenterVertically)) {
            InfoConsolaCV(e, c, tamNombre = 84f)
            OpcionesConsolaCV(e, c, columnas = 3)
            AccionesConsolaCV(e, c, alto = 76f)
        }
        // Derecha arriba: la consola en 3D (se arrastra).
        Consola3D(c, Modifier.offset(esc.d(908), esc.d(110)).size(esc.d(964), esc.d(380)))
        Modelos3D.credito(c.id)?.let { TB(it, 16f, color = col.tenue, modifier = Modifier.offset(esc.d(908), esc.d(486)).width(esc.d(964)), traducir = false) }
        // Qué juegos van junto al ícono.
        Row(Modifier.offset(esc.d(908), esc.d(514)).width(esc.d(964)).height(esc.d(52)), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
            TB("JUEGOS JUNTO AL ÍCONO", 17f, color = Color(0xFF9A9CB8), peso = FontWeight.ExtraBold, espaciado = 1.7f)
            listOf("ultimos" to "Últimos jugados", "favoritos" to "Favoritos", "elegidos" to "Elegidos por mí").forEach { (m, n) ->
                ChipElegir(n, e.extra.juegosCarpeta == m) { Diptico.elegirJuegosCarpeta(m) }
            }
            Spacer(Modifier.weight(1f))
            (1..3).forEach { q -> ChipElegir(if (q == 1) "1 juego" else "$q juegos", e.extra.cuantosCarpeta == q) { Diptico.elegirCuantosCarpeta(q) } }
        }
        RejillaCarpetasCV(e, Modifier.offset(esc.d(908), esc.d(578)).size(esc.d(964), esc.d(1080f - 578f - 134f)), unaPantalla = true)
        // Abajo a la derecha: DIPPO y el dock de apps.
        Row(Modifier.offset(esc.d(908), esc.d(1080f - 122f)).width(esc.d(964)).height(esc.d(96)), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(18))) {
            if (e.extra.dippoInicio) DippoFrase("Dime qué prefieres ver junto a cada carpeta: lo último que jugaste, tus favoritos o los que tú elijas.",
                Modifier.weight(1f), cara = 64f, letra = 19f, maxAncho = 600f) else Spacer(Modifier.weight(1f))
            Row(Modifier.vidrio(44f, fuerte = true).padding(esc.d(8)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                FilaDock(e, 52f, conCajon = false)
                BotonRedondo(Icono.BUSCAR, 58f, { Diptico.abrirBuscar() }, descripcion = "Buscar")
                BotonRedondo(Icono.AJUSTES, 58f, { Diptico.abrirMenu() }, descripcion = "Ajustes")
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

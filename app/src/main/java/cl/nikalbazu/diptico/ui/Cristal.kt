package cl.nikalbazu.diptico.ui

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import kotlin.math.max
import kotlin.math.roundToInt

internal enum class CapaCristal { CONTROL, PANEL, CONTENIDO }
internal data class EntornoCristal(
    val imagen: ImageBitmap? = null,
    val efectos: Boolean = true,
    val movimiento: Boolean = true,
    val contraste: Boolean = false,
    val colorAmbiente: Color = Color.Transparent,
    val detalle: Float = 0f,
)
internal val LocalCristal = staticCompositionLocalOf { EntornoCristal() }

@Composable
internal fun recordarCristal(e: Estado): EntornoCristal {
    if (!Estilo.glass) return EntornoCristal(efectos = false, movimiento = false)
    val ctx = LocalContext.current
    val config = LocalConfiguration.current
    var contraste by remember { mutableStateOf(false) }
    DisposableEffect(ctx) {
        fun leer() { contraste = runCatching {
            Settings.Secure.getInt(ctx.contentResolver, "high_text_contrast_enabled", 0) != 0
        }.getOrDefault(false) }
        leer()
        val observador = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { leer() }
        }
        ctx.contentResolver.registerContentObserver(Settings.Secure.getUriFor("high_text_contrast_enabled"), false, observador)
        onDispose { ctx.contentResolver.unregisterContentObserver(observador) }
    }
    val efectos = Estilo.efectosCompletos && Diptico.fluidezEfectiva(e) != "ahorro" && e.sesion == null
    // Textura pequeña y compartida: no se captura ni se vuelve a procesar toda la pantalla por control.
    val reducir = contraste || config.fontScale >= 1.4f
    val fuente = if (efectos && !reducir) rememberImagen(e.seleccionado?.arte ?: e.seleccionado?.portada, 96, memoriaGrafica = false) else null
    val imagen = recordarFondoCristal(fuente)
    val luz = remember(fuente) {
        if (fuente == null) Color.Transparent to 0f else {
            val pixeles = fuente.toPixelMap()
            var r = 0f; var g = 0f; var b = 0f; var media = 0f; var cuadrados = 0f
            repeat(8) { x -> repeat(8) { y ->
                val c = pixeles[x * (fuente.width - 1) / 7, y * (fuente.height - 1) / 7]
                r += c.red; g += c.green; b += c.blue
                val l = c.luminance(); media += l; cuadrados += l * l
            } }
            Color(r / 64, g / 64, b / 64) to ((cuadrados / 64 - (media / 64) * (media / 64)) * 8).coerceIn(0f, 1f)
        }
    }
    return EntornoCristal(imagen, efectos, efectos && e.experiencia.movimiento && ValueAnimator.areAnimatorsEnabled(), reducir, luz.first, luz.second)
}

/** Densidad por función. El texto extenso usa un material más sólido que la navegación flotante. */
internal fun densidadCristal(capa: CapaCristal, efectos: Boolean, contraste: Boolean, detalle: Float = 0f): Float = when {
    !efectos || contraste -> 1f
    capa == CapaCristal.CONTENIDO -> .97f
    capa == CapaCristal.PANEL -> .93f + .03f * detalle.coerceIn(0f, 1f)
    else -> .79f + .1f * detalle.coerceIn(0f, 1f)
}

/** El color del contenido permanece opaco y legible, incluso al reducir los efectos. */
internal fun colorMaterialCristal(p: Paleta, capa: CapaCristal): Color =
    lerp(p.fondo2, p.superficie, if (capa == CapaCristal.CONTROL) .78f else .55f).copy(alpha = 1f)

@Composable
internal fun Modifier.fondoPantalla(normal: Color): Modifier {
    if (!Estilo.glass) return background(normal)
    val claro = LocalPaleta.current.fondo.luminance() > .5f
    val a = Uva; val b = Coral; val c = Sol
    return drawWithCache {
        val w = size.width
        val h = size.height
        val radio = max(w, h).coerceAtLeast(1f)
        val azul = Brush.radialGradient(listOf(a.copy(alpha = if (claro) .19f else .18f), Color.Transparent), Offset(w * .12f, h * .12f), radio * .85f)
        val menta = Brush.radialGradient(listOf(b.copy(alpha = if (claro) .14f else .12f), Color.Transparent), Offset(w, h * .66f), radio * .64f)
        val rosa = Brush.radialGradient(listOf(c.copy(alpha = .09f), Color.Transparent), Offset(w * .6f, h), radio * .5f)
        onDrawBehind {
            drawRect(normal)
            drawRect(azul); drawRect(menta); drawRect(rosa)
        }
    }
}

/** Material de navegación: volumen, textura ambiental ampliada, velo y bordes ópticos. */
@Composable
internal fun Modifier.superficieCristal(forma: Shape, capa: CapaCristal = CapaCristal.CONTROL): Modifier {
    val p = LocalPaleta.current
    val entorno = LocalCristal.current
    val material = if (entorno.efectos && !entorno.contraste && entorno.colorAmbiente.alpha > 0f)
        lerp(colorMaterialCristal(p, capa), entorno.colorAmbiente, .07f) else colorMaterialCristal(p, capa)
    val opacidad = densidadCristal(capa, entorno.efectos, entorno.contraste, entorno.detalle)
    val claro = p.fondo.luminance() > .5f
    var posicion by remember { mutableStateOf(Offset.Zero) }
    var mundo by remember { mutableStateOf(IntSize.Zero) }
    var presionado by remember { mutableStateOf(false) }
    var punto by remember { mutableStateOf(Offset(.22f, .1f)) }
    val dinamico = entorno.movimiento && !entorno.contraste && capa == CapaCristal.CONTROL
    val luz by animateFloatAsState(if (presionado && dinamico) 1f else 0f, tween(if (dinamico) 140 else 0), label = "luzCristal")
    val gesto = if (!dinamico) Modifier else Modifier.pointerInput(Unit) {
        awaitEachGesture {
            try {
                val abajo = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                punto = Offset(abajo.position.x / size.width.coerceAtLeast(1), abajo.position.y / size.height.coerceAtLeast(1))
                presionado = true
                var pulsados = true
                while (pulsados) { pulsados = awaitPointerEvent(PointerEventPass.Initial).changes.any { it.pressed } }
            } finally { presionado = false }
        }
    }
    val elevacion = when (capa) { CapaCristal.PANEL -> 26; CapaCristal.CONTROL -> 12; CapaCristal.CONTENIDO -> 2 }
    return shadow(if (entorno.efectos && !entorno.contraste) elevacion.dp else 0.dp,
        forma, clip = false, ambientColor = Color.Black.copy(alpha = .28f), spotColor = Color.Black.copy(alpha = .45f))
        .clip(forma).then(gesto).onGloballyPositioned {
            posicion = it.positionInRoot()
            mundo = it.parentLayoutCoordinates?.let { parent ->
                var raiz = parent
                while (raiz.parentLayoutCoordinates != null) raiz = requireNotNull(raiz.parentLayoutCoordinates)
                raiz.size
            } ?: it.size
        }.drawWithCache {
            val contorno = forma.createOutline(size, layoutDirection, this)
            val borde = (if (capa == CapaCristal.PANEL) 1.6f else 1.1f).dp.toPx()
            val margen = (if (capa == CapaCristal.PANEL) 3f else 2f).dp.toPx().coerceAtMost(size.minDimension / 4)
            val interior = forma.createOutline(Size((size.width - margen * 2).coerceAtLeast(1f), (size.height - margen * 2).coerceAtLeast(1f)), layoutDirection, this)
            val iluminacion = Brush.linearGradient(listOf(Color.White.copy(alpha = if (claro) .98f else .78f), Color.White.copy(alpha = .08f), p.acentoTexto.copy(alpha = .45f)), Offset.Zero, Offset(size.width, size.height))
            val bisel = Brush.verticalGradient(listOf(Color.White.copy(alpha = if (claro) .45f else .2f), Color.Transparent, Color.Black.copy(alpha = .38f)))
            val reflejo = Brush.verticalGradient(listOf(Color.White.copy(alpha = if (claro) .31f else .12f), Color.Transparent, Color.Black.copy(alpha = if (claro) .035f else .11f)))
            onDrawWithContent {
                drawRect(material)
                val imagen = entorno.imagen
                if (entorno.efectos && !entorno.contraste && capa != CapaCristal.CONTENIDO && imagen != null && mundo.width > 0 && mundo.height > 0) {
                    // Ampliación local del arte ambiental: una aproximación óptica, no captura de los controles.
                    val escala = max(mundo.width.toFloat() / imagen.width, mundo.height.toFloat() / imagen.height) * 1.07f
                    val an = imagen.width * escala
                    val al = imagen.height * escala
                    drawImage(imagen, dstOffset = IntOffset(((mundo.width - an) / 2 - posicion.x).roundToInt(), ((mundo.height - al) / 2 - posicion.y).roundToInt()),
                        dstSize = IntSize(an.roundToInt().coerceAtLeast(1), al.roundToInt().coerceAtLeast(1)), filterQuality = FilterQuality.Low)
                    drawRect(material.copy(alpha = opacidad))
                }
                if (entorno.efectos && !entorno.contraste && capa != CapaCristal.CONTENIDO) {
                    drawRect(reflejo)
                    if (luz > 0f) drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = .17f * luz), Color.Transparent), Offset(punto.x * size.width, punto.y * size.height), size.width.coerceAtLeast(size.height)))
                }
                drawContent()
                if (entorno.contraste) drawOutline(contorno, p.tinta, style = Stroke(2.dp.toPx()))
                else if (entorno.efectos) {
                    drawOutline(contorno, Color.Black.copy(alpha = if (claro) .24f else .8f), style = Stroke(borde * 4))
                    if (capa != CapaCristal.CONTENIDO) withTransform({ translate(margen, margen) }) {
                        drawOutline(interior, bisel, style = Stroke(borde * .75f))
                    }
                    drawOutline(contorno, iluminacion, style = Stroke(if (capa == CapaCristal.CONTENIDO) borde * .5f else borde))
                }
            }
        }
}

/** Permite integrar el material sin modificar las formas ni los fondos de los otros temas. */
@Composable
internal fun Modifier.superficieApp(normal: Color, forma: Shape, capa: CapaCristal = CapaCristal.CONTROL): Modifier =
    if (Estilo.glass) superficieCristal(forma, capa) else clip(forma).background(normal)

/** El tinte de una acción comparte la capa del control: no agrega otro fondo desenfocado encima. */
@Composable
internal fun Modifier.tinteCristal(color: Color): Modifier {
    val efectos = LocalCristal.current.efectos
    return background(color).drawWithCache {
        onDrawWithContent {
            if (efectos) drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = .22f), Color.Transparent, Color.Black.copy(alpha = .08f))))
            drawContent()
        }
    }
}

package cl.nikalbazu.diptico.ui

import cl.nikalbazu.diptico.estado.*

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import cl.nikalbazu.diptico.data.Consolas
import kotlin.math.pow

/*
 * Intro de Colección viva (boceto 13a, unos 4 segundos):
 *  1. una línea de luz se enciende en la bisagra, como una consola que se abre;
 *  2. las dos pantallas se prenden y abajo entran tus consolas desde los lados;
 *  3. arriba caen las cajas y el logo se arma con sus dos mitades (un díptico);
 *  4. DIPPO se asoma detrás del logo y saluda;
 *  5. todo se funde hacia tu colección. Cualquier botón o toque la salta.
 * Las dos pantallas comparten el mismo reloj, así que van sincronizadas.
 */

private const val DURACION = 4300f
private val Noche = Color(0xFF030308)
private val Luz = Color(0xFFE8FBFF)
private val Celeste = Color(0xFF8FEAFF)

private fun tramo(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun suave(x: Float) = 1f - (1f - x).pow(3)
private fun rebote(x: Float): Float {
    val c1 = 1.70158f; val c3 = c1 + 1f
    return 1f + c3 * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
}

/** Colores de cinco consolas conocidas para las siluetas y las cajas (si no están, uno del tema). */
private val COLORES_INTRO: List<Color> by lazy {
    listOf("snes", "ps2", "gba", "switch", "psx", "n64", "nds").map { id -> Consolas.porId(id)?.color ?: Color(0xFF6B4EFF) }
}

@Composable
fun Intro(rol: Rol) {
    val inicio = remember { Diptico.inicioIntro() }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(inicio) {
        while (true) {
            withFrameMillis { }
            t = ((SystemClock.uptimeMillis() - inicio) / DURACION).coerceAtLeast(0f)
            if (t >= 1f) break
        }
        Diptico.terminarIntro()
    }
    val salida = tramo(t, 0.88f, 1f)
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = 1f - suave(salida); val z = 1f + 0.05f * salida; scaleX = z; scaleY = z }
            .background(Noche)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { Diptico.terminarIntro() },
    ) {
        when (rol) {
            Rol.INFERIOR -> LienzoBoceto(1240f, 1080f) { IntroAbajo(t) }
            Rol.SUPERIOR -> LienzoBoceto(1920f, 1080f) { IntroArriba(t, unica = false) }
            Rol.UNICA -> LienzoBoceto(1920f, 1080f) { IntroArriba(t, unica = true) }
        }
    }
}

/** El cielo que se prende: un violeta que sale desde la bisagra. */
@Composable
private fun BoxScope.Cielo(t: Float, desdeAbajo: Boolean) {
    val a = suave(tramo(t, 0.08f, 0.30f))
    val c = listOf(Color(0xFF3A2A8A), Color(0xFF120D2E), Noche)
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = a }.background(Brush.verticalGradient(if (desdeAbajo) c.reversed() else c)))
}

/** La línea de luz de la bisagra: crece y se apaga cuando se prende la pantalla. */
@Composable
private fun BoxScope.LineaBisagra(t: Float, abajo: Boolean, largo: Float) {
    val esc = LocalEscala.current
    val crece = suave(tramo(t, 0f, 0.07f))
    val apaga = 1f - tramo(t, 0.09f, 0.16f)
    if (apaga <= 0f) return
    Box(
        Modifier.align(if (abajo) Alignment.BottomCenter else Alignment.TopCenter)
            .width(esc.d(largo * crece)).height(esc.d(6))
            .graphicsLayer { alpha = apaga }
            .shadow(esc.d(24), RoundedCornerShape(50), ambientColor = Celeste, spotColor = Celeste)
            .clip(RoundedCornerShape(50)).background(Luz),
    )
}

@Composable
private fun BoxScope.IntroArriba(t: Float, unica: Boolean) {
    val esc = LocalEscala.current
    val px = with(LocalDensity.current) { esc.d(1f).toPx() }
    Cielo(t, desdeAbajo = !unica)
    LineaBisagra(t, abajo = !unica, largo = 900f)
    // Tus cajas caen y se acomodan en fila (con un rebote corto), escalonadas: las portadas de tus juegos.
    val portadas = remember { Diptico.estado.value.juegos.mapNotNull { it.portada?.takeIf { u -> u.scheme != "app" } }.distinct().take(7) }
    Row(
        Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(110)),
        horizontalArrangement = Arrangement.spacedBy(esc.d(26)),
    ) {
        COLORES_INTRO.forEachIndexed { i, c ->
            val p = tramo(t, 0.28f + i * 0.018f, 0.46f + i * 0.018f)
            val r = rebote(p)
            val atenua = 1f - 0.45f * tramo(t, 0.74f, 0.88f)
            Box(
                Modifier.size(esc.d(150), esc.d(210))
                    .graphicsLayer {
                        translationY = (1f - r) * -900f * px
                        rotationZ = (i - 3) * (1.5f + 12f * (1f - p))
                        alpha = (if (p > 0f) 1f else 0f) * atenua
                    }
                    .shadow(esc.d(20), RoundedCornerShape(esc.d(14)))
                    .clip(RoundedCornerShape(esc.d(14)))
                    .background(Brush.linearGradient(listOf(c, Color(0xFF141526)))),
            ) {
                val img = rememberImagen(portadas.getOrNull(i), 300)
                if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                // Brillo de plástico en el estuche.
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.White.copy(alpha = .22f), Color.Transparent, Color.Transparent))))
            }
        }
    }
    // El logo se arma con sus dos mitades (un díptico) y un destello de luz lo cruza; abajo, la frase.
    val junta = suave(tramo(t, 0.30f, 0.50f))
    val hueco = 420f * (1f - junta)
    Row(Modifier.align(Alignment.Center).offset(y = esc.d(-170)).graphicsLayer { alpha = junta }, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.graphicsLayer { translationX = -hueco / 2f * px; scaleX = .7f + .3f * junta; scaleY = .7f + .3f * junta }) { MarcaLogo(150f) }
        Box(Modifier.width(esc.d(34)))
        TB("Díp", 190f, titulo = true, traducir = false, modifier = Modifier.graphicsLayer { translationX = -hueco / 2f * px })
        TB("tico", 190f, titulo = true, color = Color(0xFFFFD29A), traducir = false, modifier = Modifier.graphicsLayer { translationX = hueco / 2f * px })
    }
    val destello = tramo(t, 0.50f, 0.64f)
    if (destello in 0.001f..0.999f) Box(
        Modifier.align(Alignment.Center).offset(y = esc.d(-170)).size(esc.d(1100), esc.d(260))
            .graphicsLayer { alpha = 1f - kotlin.math.abs(destello - .5f) * 2f; translationX = (destello - .5f) * 1400f * px }
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = .35f), Color.Transparent))),
    )
    TB("Tu colección, en dos pantallas", 40f, color = Color(0xFFB9BBD6), peso = FontWeight.ExtraBold,
        modifier = Modifier.align(Alignment.Center).offset(y = esc.d(-20)).graphicsLayer { alpha = suave(tramo(t, 0.52f, 0.64f)) * (1f - tramo(t, 0.8f, 0.88f)) })
    // DIPPO se asoma, salta y saluda.
    val d = tramo(t, 0.52f, 0.66f)
    if (d > 0f) {
        val r = rebote(d)
        Box(
            Modifier.align(Alignment.TopStart).offset(x = esc.d(1380), y = esc.d(250))
                .graphicsLayer { alpha = d; scaleX = 0.4f + 0.6f * r; scaleY = 0.4f + 0.6f * r; rotationZ = 8f - 28f * (1f - r); translationY = (1f - r) * 170f * px },
        ) { cl.nikalbazu.diptico.bisa.AvatarBisa(150f * esc.factor, cara = CaraDippo.FELIZ) }
        val g = suave(tramo(t, 0.60f, 0.70f))
        val e = Diptico.estado.value
        val saludo = if (e.configurado && e.nombre.isNotBlank()) "¡Hola, ${e.nombre}! Tu colección te espera."
            else "¡Hola! Soy DIPPO. Vamos a armar tu colección."
        Box(
            Modifier.align(Alignment.TopStart).offset(x = esc.d(1060), y = esc.d(120)).width(esc.d(560))
                .graphicsLayer { alpha = g; translationY = (1f - g) * 20f * px }
                .clip(RoundedCornerShape(esc.d(30))).background(Color(0xFFF4F3FA)).padding(horizontal = esc.d(28), vertical = esc.d(20)),
        ) { TB(saludo, 27f, color = Color(0xFF12131F), peso = FontWeight.ExtraBold, lineas = 2) }
    }
    if (unica) Siluetas(t, Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(20)), escala = 0.45f)
    TB("Cualquier botón para saltar", 20f, color = Color(0xFF8B8DA8), peso = FontWeight.ExtraBold,
        modifier = Modifier.align(Alignment.TopEnd).padding(esc.d(40)).graphicsLayer { alpha = tramo(t, 0.15f, 0.3f) * (1f - tramo(t, 0.8f, 0.9f)) })
}

@Composable
private fun BoxScope.IntroAbajo(t: Float) {
    val esc = LocalEscala.current
    Cielo(t, desdeAbajo = false)
    LineaBisagra(t, abajo = false, largo = 700f)
    Siluetas(t, Modifier.align(Alignment.Center), escala = 1f)
    TB("Cualquier botón para saltar", 22f, color = Color(0xFF8B8DA8), peso = FontWeight.ExtraBold,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(60)).graphicsLayer { alpha = tramo(t, 0.15f, 0.3f) * (1f - tramo(t, 0.8f, 0.9f)) })
}

/** Tus consolas (sus fotos de estudio) entran desde los lados; si no hay foto, una silueta de su color. */
@Composable
private fun Siluetas(t: Float, modifier: Modifier, escala: Float) {
    val esc = LocalEscala.current
    val px = with(LocalDensity.current) { esc.d(1f).toPx() }
    val ctx = LocalContext.current
    val consolas = remember { Diptico.estado.value.consolasConJuegos.take(5).ifEmpty { listOfNotNull("snes", "ps2", "gba", "switch", "psx").mapNotNull { Consolas.porId(it) } } }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(esc.d(26 * escala)), verticalAlignment = Alignment.CenterVertically) {
        consolas.forEachIndexed { i, c ->
            val p = suave(tramo(t, 0.14f + i * 0.025f, 0.36f + i * 0.025f))
            val id = Modelos3D.idDe(c.id)
            val img by produceState<ImageBitmap?>(ImagenesConsola.enMemoria(id), id) {
                if (value == null && ImagenesConsola.hay(ctx, id)) value = withContext(Dispatchers.IO) { ImagenesConsola.cargar(ctx, id) }
            }
            Box(
                Modifier.size(esc.d(210 * escala), esc.d(170 * escala))
                    .graphicsLayer { translationX = (1f - p) * (if (i < 2) -900f else 900f) * px; alpha = p; translationY = (1f - p) * 40f * px },
                contentAlignment = Alignment.Center,
            ) {
                val pintura = img
                if (pintura != null) Image(pintura, c.nombre, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else Box(Modifier.fillMaxSize().clip(RoundedCornerShape(esc.d(44 * escala))).background(Brush.linearGradient(listOf(c.color, Color(0xFF1A1B2E)))))
            }
        }
    }
}

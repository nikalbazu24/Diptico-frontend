package cl.nikalbazu.diptico.ui

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Diptico
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

private const val DURACION = 3400f
private val FondoIntro = Color(0xFF0F0D18)
private val TenueIntro = Color(0xFFA9A5C7)

/** Avance (0 a 1) de un tramo de la animación. */
private fun tramo(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun suave(x: Float) = 1f - (1f - x).pow(3)
private fun rebote(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
}
private fun vaiven(x: Float) = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).pow(3) / 2f

/**
 * Animación de entrada. En dos pantallas cuenta una pequeña historia: el panel uva cae arriba,
 * el panel coral sube desde la pantalla de abajo, cruza la bisagra y encaja bajo el uva.
 * Las dos pantallas comparten el mismo reloj, así que van sincronizadas.
 */
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
    val esc = LocalEscala.current
    val px = with(LocalDensity.current) { esc.d(1f).toPx() }
    val salida = tramo(t, 0.86f, 1f)
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - suave(salida)
                val z = 1f + 0.05f * salida
                scaleX = z
                scaleY = z
            }
            .background(FondoIntro)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { Diptico.terminarIntro() },
    ) {
        Brillos(t, px)
        when (rol) {
            Rol.INFERIOR -> IntroInferior(t, px)
            Rol.SUPERIOR -> IntroSuperior(t, px, unica = false)
            Rol.UNICA -> IntroSuperior(t, px, unica = true)
        }
        Texto(
            "Toca o aprieta un botón para saltar", 11f, color = TenueIntro.copy(alpha = 0.6f),
            familia = Nunito, peso = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = esc.d(24), bottom = esc.d(18))
                .graphicsLayer { alpha = tramo(t, 0.2f, 0.35f) },
        )
    }
}

@Composable
private fun BoxScope.Brillos(t: Float, px: Float) {
    val esc = LocalEscala.current
    val k = suave(tramo(t, 0.05f, 0.6f))
    val d = sin(t * PI.toFloat()) * 40f
    Box(
        Modifier.align(Alignment.TopEnd).size(esc.d(480))
            .graphicsLayer { alpha = k * 0.55f; translationX = -d * px; translationY = -120f * px }
            .background(Brush.radialGradient(listOf(Uva.copy(alpha = 0.6f), Color.Transparent))),
    )
    Box(
        Modifier.align(Alignment.BottomStart).size(esc.d(420))
            .graphicsLayer { alpha = k * 0.45f; translationX = d * px; translationY = 140f * px }
            .background(Brush.radialGradient(listOf(Coral.copy(alpha = 0.5f), Color.Transparent))),
    )
}

@Composable
private fun BoxScope.IntroSuperior(t: Float, px: Float, unica: Boolean) {
    val esc = LocalEscala.current
    val tam = 70f
    val inicioCoral = if (unica) 0.16f else 0.37f
    val entraUva = rebote(tramo(t, 0.03f, 0.27f))
    val entraCoral = rebote(tramo(t, inicioCoral, inicioCoral + if (unica) 0.24f else 0.19f))
    val clic = sin(PI.toFloat() * tramo(t, 0.56f, 0.66f)) * 0.09f
    val onda = tramo(t, 0.56f, 0.95f)

    Column(
        Modifier.align(Alignment.Center).graphicsLayer { translationY = (if (unica) -30f else -8f) * px },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                // Onda de luz cuando los dos paneles encajan.
                Box(
                    Modifier.size(esc.d(115))
                        .graphicsLayer {
                            val s = 0.4f + onda * 1.8f
                            scaleX = s
                            scaleY = s
                            alpha = if (onda <= 0f) 0f else (1f - onda) * 0.5f
                        }
                        .clip(CircleShape)
                        .background(Uva.copy(alpha = 0.5f)),
                )
                Column(
                    Modifier.graphicsLayer { scaleX = 1f + clic; scaleY = 1f - clic },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(esc.d(tam * 0.14f)),
                ) {
                    Box(
                        Modifier
                            .graphicsLayer { translationY = (1f - entraUva) * -340f * px; alpha = if (t > 0.03f) 1f else 0f }
                            .size(esc.d(tam * 1.25f), esc.d(tam * 0.5f))
                            .clip(RoundedCornerShape(esc.d(tam * 0.2f)))
                            .background(Uva),
                    )
                    Box(
                        Modifier
                            .graphicsLayer { translationY = (1f - entraCoral) * 340f * px; alpha = if (t > inicioCoral) 1f else 0f }
                            .size(esc.d(tam * 0.95f), esc.d(tam * 0.5f))
                            .clip(RoundedCornerShape(esc.d(tam * 0.2f)))
                            .background(Coral),
                    )
                }
            }
            Spacer(Modifier.width(esc.d(24)))
            Row {
                "Díptico".forEachIndexed { i, letra ->
                    val k = suave(tramo(t, 0.58f + i * 0.035f, 0.72f + i * 0.035f))
                    Texto(
                        letra.toString(), 76f, color = Color.White,
                        modifier = Modifier.graphicsLayer { alpha = k; translationY = (1f - k) * 26f * px },
                    )
                }
            }
        }
        if (unica) {
            Spacer(Modifier.height(esc.d(18)))
            Texto(
                "Tu colección de juegos, en una o dos pantallas", 18f, color = TenueIntro, peso = FontWeight.SemiBold,
                modifier = Modifier.graphicsLayer { alpha = suave(tramo(t, 0.7f, 0.82f)) },
            )
            Spacer(Modifier.height(esc.d(22)))
            FilaIconos(t, 0.72f)
        }
    }
    if (!unica) Bisagra(t, Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(10)))
}

@Composable
private fun BoxScope.IntroInferior(t: Float, px: Float) {
    val esc = LocalEscala.current
    val tam = 70f
    // El panel coral sube, cruza la pantalla y sale por la bisagra hacia la pantalla de arriba.
    val k = tramo(t, 0.08f, 0.38f)
    val y = 300f + (-360f - 300f) * vaiven(k)
    val visible = k > 0f && k < 1f
    Box(
        Modifier.align(Alignment.Center)
            .graphicsLayer { translationY = (y + 50f) * px; alpha = if (visible) 0.35f else 0f; scaleY = 1.6f }
            .size(esc.d(tam * 0.6f), esc.d(tam * 0.5f))
            .clip(RoundedCornerShape(esc.d(tam * 0.2f)))
            .background(Brush.verticalGradient(listOf(Coral, Color.Transparent))),
    )
    Box(
        Modifier.align(Alignment.Center)
            .graphicsLayer { translationY = y * px; alpha = if (visible) 1f else 0f }
            .size(esc.d(tam * 0.95f), esc.d(tam * 0.5f))
            .clip(RoundedCornerShape(esc.d(tam * 0.2f)))
            .background(Coral),
    )
    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
        Texto(
            "Tu colección de juegos,", 24f, color = Color.White,
            modifier = Modifier.graphicsLayer {
                val a = suave(tramo(t, 0.6f, 0.72f))
                alpha = a
                translationY = (1f - a) * 20f * px
            },
        )
        Texto(
            "en dos pantallas.", 24f, color = TenueIntro,
            modifier = Modifier.graphicsLayer {
                val a = suave(tramo(t, 0.65f, 0.77f))
                alpha = a
                translationY = (1f - a) * 20f * px
            },
        )
        Spacer(Modifier.height(esc.d(26)))
        FilaIconos(t, 0.68f)
    }
    Bisagra(t, Modifier.align(Alignment.TopCenter).padding(top = esc.d(10)))
}

/** Los íconos de cada consola aparecen uno tras otro. */
@Composable
private fun FilaIconos(t: Float, desde: Float) {
    val esc = LocalEscala.current
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
        Consolas.todas.forEachIndexed { i, c ->
            val k = rebote(tramo(t, desde + i * 0.016f, desde + 0.1f + i * 0.016f))
            IconoMedio(
                c.medio, c.color, FondoIntro, tam = 30f,
                modifier = Modifier.graphicsLayer { scaleX = k; scaleY = k; alpha = if (k > 0f) 1f else 0f },
            )
        }
    }
}

/** La bisagra punteada se dibuja desde el centro hacia los lados. */
@Composable
private fun Bisagra(t: Float, modifier: Modifier) {
    val esc = LocalEscala.current
    val k = suave(tramo(t, 0.6f, 0.86f))
    Canvas(modifier.fillMaxWidth().height(esc.d(8))) {
        val separacion = size.height * 18f / 8f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.height * 0.3f
        var d = 0f
        var i = 0
        while (k > 0f && d <= size.width / 2f * k) {
            val color = if (i % 2 == 0) Uva else Coral
            drawCircle(color, r, Offset(cx + d, cy))
            if (d > 0f) drawCircle(color, r, Offset(cx - d, cy))
            d += separacion
            i++
        }
    }
}

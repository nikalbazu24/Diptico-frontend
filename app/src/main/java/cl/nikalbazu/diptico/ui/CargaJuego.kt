package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.CARGA_MS
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado

/** Avance de [t] entre [desde] y [hasta], de 0 a 1 (para escalonar la animación con un solo reloj). */
private fun tramo(t: Float, desde: Float, hasta: Float) = ((t - desde) / (hasta - desde)).coerceIn(0f, 1f)

/** Curva suave para que lo que entra frene al llegar. */
private fun suave(x: Float) = FastOutSlowInEasing.transform(x)

/**
 * La pantalla de carga al abrir un juego: todo se tiñe del color de su consola, entra la caja con la forma de esa
 * consola, el nombre y una barrita fina; al final se funde a negro y recién ahí se abre el juego.
 * En la pantalla del juego va completa; en la otra, el mismo color con el logo, para que las dos se sientan una sola.
 */
@Composable
fun PantallaCarga(e: Estado, miPantalla: Int, unica: Boolean) {
    // La caja con la forma de su consola (no la forma «para todas» del estante).
    androidx.compose.runtime.CompositionLocalProvider(LocalFormas provides e.formas) { ContenidoCarga(e, miPantalla, unica) }
}

@Composable
private fun ContenidoCarga(e: Estado, miPantalla: Int, unica: Boolean) {
    val carga = e.extra.carga ?: return
    val juego = remember(carga.clave) { e.juegosPorClave[carga.clave] } ?: return
    val consola = Consolas.porId(juego.consolaId) ?: return
    val esc = LocalEscala.current
    val principal = unica || miPantalla == carga.displayId
    // Un solo reloj de 0 a 1 para toda la animación (el juego se abre al llegar a 1).
    val t = remember(carga.inicio) { Animatable(0f) }
    LaunchedEffect(carga.inicio) {
        val ya = (System.currentTimeMillis() - carga.inicio).coerceIn(0L, CARGA_MS)
        t.snapTo(ya.toFloat() / CARGA_MS)
        t.animateTo(1f, tween((CARGA_MS - ya).toInt().coerceAtLeast(1), easing = LinearEasing))
    }
    val color = consola.color
    val oscuro = lerp(color, Color.Black, 0.55f)
    val tinta = if (color.luminance() > 0.6f) Color(0xFF15121D) else Color.White
    Box(
        Modifier.fillMaxSize().testTag("pantalla-carga")
            // Se traga los toques: mientras carga no se toca nada de atrás.
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        val v = t.value
        // 1. El color de la consola entra (con un brillo arriba, como luz de vitrina).
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = tramo(v, 0f, 0.16f) }
                .background(Brush.verticalGradient(listOf(color, lerp(color, oscuro, 0.5f), oscuro))),
        )
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = tramo(v, 0f, 0.16f) * 0.5f }
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent))),
        )
        if (principal) BoxWithConstraints(Modifier.fillMaxSize()) {
            val ancho = maxWidth.value
            val alto = maxHeight.value
            val vertical = ancho / alto < 1.15f
            // La caja ocupa cerca de la mitad del alto (un poco menos si va arriba del texto).
            val base = cajaDe(consola)
            val objetivo = (alto * (if (vertical) 0.40f else 0.56f)) / esc.factor
            val escala = minOf(objetivo / base.alto, (ancho * 0.4f / esc.factor) / base.ancho)
            val caja = @Composable {
                val c = suave(tramo(v, 0.06f, 0.38f))
                Box(
                    Modifier.graphicsLayer {
                        alpha = c
                        translationX = (1f - c) * -60.dp.toPx()
                        val s = 0.9f + 0.1f * c
                        scaleX = s; scaleY = s
                        // Gira apenas al llegar: se siente como una caja de verdad que se acomoda.
                        rotationY = 22f * (1f - c) + 6f
                        cameraDistance = 14f * density
                    }.shadow(esc.d(24), RoundedCornerShape(esc.d(6)), ambientColor = Color.Black, spotColor = Color.Black),
                ) { CajaJuego(juego, consola, escala = escala) }
            }
            val textos = @Composable {
                val c = suave(tramo(v, 0.18f, 0.46f))
                Column(
                    Modifier.graphicsLayer { alpha = c; translationY = (1f - c) * 14.dp.toPx() },
                    verticalArrangement = Arrangement.spacedBy(esc.d(6)),
                    horizontalAlignment = if (vertical) Alignment.CenterHorizontally else Alignment.Start,
                ) {
                    Texto("INICIANDO", 10f, color = tinta.copy(alpha = 0.75f), peso = FontWeight.SemiBold, espaciado = 2.4f, lineas = 1)
                    Texto(consola.bonito, if (vertical) 30f else 40f, color = tinta, peso = FontWeight.ExtraBold, lineas = 1, traducir = false,
                        interlineado = if (vertical) 34f else 44f)
                    Texto(juego.titulo, if (vertical) 15f else 18f, color = tinta, lineas = 2, traducir = false,
                        alinear = if (vertical) androidx.compose.ui.text.style.TextAlign.Center else null)
                    // La barrita fina: se llena mientras la caja se acomoda.
                    Box(
                        Modifier.padding(top = esc.d(10)).width(esc.d(170)).height(esc.d(3)).clip(RoundedCornerShape(50))
                            .background(tinta.copy(alpha = 0.25f)),
                    ) {
                        Box(
                            Modifier.fillMaxHeight().fillMaxWidth(suave(tramo(v, 0.2f, 0.82f)).coerceAtLeast(0.001f))
                                .clip(RoundedCornerShape(50)).background(tinta),
                        )
                    }
                }
            }
            if (vertical) Column(
                Modifier.fillMaxSize().padding(esc.d(24)),
                verticalArrangement = Arrangement.spacedBy(esc.d(22), Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { caja(); textos() }
            else Row(
                Modifier.fillMaxSize().padding(horizontal = esc.d(56)),
                horizontalArrangement = Arrangement.spacedBy(esc.d(40), Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) { caja(); Box(Modifier.weight(1f, fill = false)) { textos() } }
        } else {
            // La pantalla compañera: el logo del juego (o su nombre) flotando en el mismo color.
            val c = suave(tramo(v, 0.12f, 0.45f))
            Box(
                Modifier.fillMaxSize().padding(esc.d(32)).graphicsLayer { alpha = c; val s = 0.94f + 0.06f * c; scaleX = s; scaleY = s },
                contentAlignment = Alignment.Center,
            ) {
                if (juego.logo != null) ArteJuego(juego, preferirArte = false, maxLado = 480, imagen = "logo",
                    modifier = Modifier.fillMaxWidth(0.62f).height(esc.d(120)))
                else Texto(juego.titulo, 26f, color = tinta, lineas = 2, traducir = false,
                    alinear = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        // 2. Al final todo se funde a negro: el juego aparece sobre negro, sin parpadeos.
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = suave(tramo(v, 0.8f, 1f)) }.background(Color.Black))
        if (principal) Texto(
            "B Cancelar", 11f, color = tinta.copy(alpha = 0.6f), peso = FontWeight.SemiBold, lineas = 1,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(14))
                .graphicsLayer { alpha = tramo(v, 0.3f, 0.45f) * (1f - tramo(v, 0.75f, 0.85f)) }
                .clickable { Diptico.cancelarCarga() },
        )
    }
}

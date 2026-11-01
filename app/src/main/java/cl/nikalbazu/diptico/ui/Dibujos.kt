package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.data.Banda
import cl.nikalbazu.diptico.data.FormaCaja
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.data.Medio
import kotlin.math.min

/** Dibujos de ejemplo para juegos que todavía no tienen portada. */
enum class Dibujo { MONTANAS, SOL, RAYAS, CUADROS, ONDAS, ESTRELLAS, FRANJAS, LUNA }

private val coloresPortada = listOf(
    0xFFFF8A5B, 0xFF3BA8FF, 0xFFFF5D8F, 0xFF6B4EFF, 0xFFE8573A, 0xFFFFB020,
    0xFF4B3F8F, 0xFF20B8A6, 0xFFC2453A, 0xFFC77DFF, 0xFF4A5BFF,
).map { Color(it) }

fun colorDe(clave: String): Color = coloresPortada[Math.floorMod(clave.hashCode(), coloresPortada.size)]
fun dibujoDe(clave: String): Dibujo = Dibujo.values()[Math.floorMod(clave.hashCode() / 7, Dibujo.values().size)]

fun DrawScope.portadaGenerada(d: Dibujo, color: Color, u: Float) {
    drawRect(color)
    val w = size.width
    val h = size.height
    val m = min(w, h)
    val claro = Color.White.copy(alpha = 0.22f)
    val crema = Color(0xF2FFF1C2)
    val sombra = Color(0xFF1C1A33)
    when (d) {
        Dibujo.MONTANAS -> {
            drawPath(Path().apply { moveTo(0f, h); lineTo(w * 0.42f, h * 0.42f); lineTo(w * 0.92f, h); close() }, sombra.copy(alpha = 0.38f))
            drawPath(Path().apply { moveTo(w * 0.45f, h); lineTo(w * 0.78f, h * 0.55f); lineTo(w * 1.1f, h); close() }, sombra.copy(alpha = 0.25f))
            drawCircle(crema, m * 0.1f, Offset(w * 0.72f, h * 0.26f))
        }
        Dibujo.SOL -> {
            drawCircle(crema, m * 0.24f, Offset(w * 0.5f, h * 0.6f))
            drawRect(sombra.copy(alpha = 0.35f), Offset(0f, h * 0.62f), Size(w, h * 0.38f))
        }
        Dibujo.RAYAS -> rotate(30f) {
            var x = -w * 2
            while (x < w * 2) {
                drawRect(claro, Offset(x, -h), Size(12 * u, h * 3))
                x += 26 * u
            }
        }
        Dibujo.CUADROS -> {
            val t = 11 * u
            var i = 0
            var y = 0f
            while (y < h) {
                var j = 0
                var x = 0f
                while (x < w) {
                    if ((i + j) % 2 == 0) drawRect(claro, Offset(x, y), Size(t, t))
                    x += t
                    j++
                }
                y += t
                i++
            }
        }
        Dibujo.ONDAS -> {
            var r = 7 * u
            val c = Offset(w * 0.3f, h * 0.85f)
            while (r < m * 2) {
                drawCircle(claro, r, c, style = Stroke(7 * u))
                r += 16 * u
            }
        }
        Dibujo.ESTRELLAS -> {
            drawCircle(sombra.copy(alpha = 0.45f), w * 0.62f, Offset(w * 0.5f, h * 1.2f))
            drawCircle(Color.White.copy(alpha = 0.95f), m * 0.05f, Offset(w * 0.5f, h * 0.42f))
            drawCircle(Color.White.copy(alpha = 0.8f), m * 0.018f, Offset(w * 0.22f, h * 0.22f))
            drawCircle(Color.White.copy(alpha = 0.8f), m * 0.018f, Offset(w * 0.78f, h * 0.68f))
            drawCircle(Color.White.copy(alpha = 0.7f), m * 0.014f, Offset(w * 0.65f, h * 0.18f))
        }
        Dibujo.FRANJAS -> {
            drawRect(Color.White.copy(alpha = 0.28f), Offset(0f, h * 0.38f), Size(w, h * 0.1f))
            drawRect(Color.White.copy(alpha = 0.28f), Offset(0f, h * 0.58f), Size(w, h * 0.1f))
        }
        Dibujo.LUNA -> {
            drawCircle(Color.White.copy(alpha = 0.9f), m * 0.12f, Offset(w * 0.64f, h * 0.3f))
            drawCircle(color, m * 0.11f, Offset(w * 0.70f, h * 0.26f))
            drawRect(Color.Black.copy(alpha = 0.35f), Offset(0f, h * 0.7f), Size(w, h * 0.3f))
        }
    }
}

/** Arte de un juego: su imagen real o, si no hay, el dibujo de ejemplo. */
@Composable
fun ArteJuego(juego: Juego, preferirArte: Boolean, maxLado: Int, modifier: Modifier = Modifier, imagen: String = "portada") {
    val uri = when {
        preferirArte -> juego.arte ?: juego.portada
        imagen == "arte" -> juego.arte ?: juego.portada
        imagen == "logo" -> juego.logo ?: juego.portada
        else -> juego.portada
    }
    val esIcono = uri?.scheme == "app"
    val esLogo = imagen == "logo" && juego.logo != null && !preferirArte
    val img = rememberImagen(uri, if (esIcono) 256 else maxLado)
    val u = with(LocalDensity.current) { LocalEscala.current.d(1f).toPx() }
    Box(modifier) {
        if (img != null && (esIcono || esLogo)) {
            // Ícono de app o logo: sobre un fondo de color, centrado y sin estirarse.
            val color = colorDe(juego.clave)
            Canvas(Modifier.fillMaxSize()) { portadaGenerada(dibujoDe(juego.clave), color, u) }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (esLogo) 0.45f else 0.25f)))
            Image(
                img, contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.Center).fillMaxSize(if (preferirArte) 0.42f else if (esLogo) 0.8f else 0.62f),
            )
        } else if (img != null) {
            Image(img, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            val color = colorDe(juego.clave)
            val dibujo = dibujoDe(juego.clave)
            Canvas(Modifier.fillMaxSize()) { portadaGenerada(dibujo, color, u) }
        }
    }
}

/** Forma de las cajas de cada consola, elegida en el menú (Estante). */
val LocalFormas = staticCompositionLocalOf { emptyMap<String, cl.nikalbazu.diptico.estado.FormaCfg>() }

/** Tamaño general de los juegos en el estante. */
val LocalTamJuegos = staticCompositionLocalOf { 1f }

/** La forma de caja que toca a una consola (la original o la elegida), ya multiplicada por [escala]. */
@Composable
fun cajaDe(consola: Consola, escala: Float = 1f): FormaCaja {
    val cfg = LocalFormas.current[consola.id]
    val f = when (cfg?.forma) {
        "cuadrada" -> FormaCaja(110f, 110f, 12f, Banda.NINGUNA)
        "poster" -> FormaCaja(92f, 138f, 8f, Banda.NINGUNA)
        "horizontal" -> FormaCaja(156f, 92f, 10f, Banda.NINGUNA)
        "disco" -> FormaCaja(116f, 116f, 58f, Banda.NINGUNA)
        "cartucho" -> FormaCaja(120f, 100f, 8f, Banda.ABAJO_CLARA)
        "tarjeta" -> FormaCaja(86f, 104f, 12f, Banda.ARRIBA_CLARA)
        else -> consola.caja
    }
    val k = escala * (cfg?.tam ?: 1f)
    return FormaCaja(f.ancho * k, f.alto * k, f.radio * k, f.banda)
}

/** La caja del juego con la forma de su consola (o la que elegiste para esa consola). */
@Composable
fun CajaJuego(juego: Juego, consola: Consola, modifier: Modifier = Modifier, escala: Float = 1f) {
    val esc = LocalEscala.current
    val f = cajaDe(consola, escala)
    val cfg = LocalFormas.current[consola.id]
    val k = f.ancho / 100f
    val imagen = cfg?.imagen ?: "portada"
    val tieneImagen = when (imagen) {
        "arte" -> juego.arte != null || (juego.portada != null && juego.portada.scheme != "app")
        "logo" -> juego.logo != null
        else -> juego.portada != null && juego.portada.scheme != "app"
    }
    val disco = cfg?.forma == "disco"
    Box(
        modifier
            .size(esc.d(f.ancho), esc.d(f.alto))
            .clip(if (disco) CircleShape else RoundedCornerShape(esc.d(f.radio))),
    ) {
        ArteJuego(juego, preferirArte = false, maxLado = 360, modifier = Modifier.fillMaxSize(), imagen = imagen)
        when (f.banda) {
            Banda.NINGUNA -> Unit
            Banda.BISAGRA -> Box(Modifier.fillMaxHeight().width(esc.d(11 * k)).background(Color.Black.copy(alpha = 0.3f)))
            Banda.LATERAL_CLARA -> Box(Modifier.fillMaxHeight().width(esc.d(8 * k)).background(Color.White.copy(alpha = 0.35f)))
            Banda.ARRIBA_CLARA -> Box(Modifier.fillMaxWidth().height(esc.d(11 * k)).background(Color.White.copy(alpha = 0.45f)))
            Banda.ARRIBA_OSCURA -> Box(Modifier.fillMaxWidth().height(esc.d(15 * k)).background(Color.Black.copy(alpha = 0.45f)))
            Banda.ARRIBA_ROJA -> Box(Modifier.fillMaxWidth().height(esc.d(14 * k)).background(Color(0xFFE60012)))
            Banda.ABAJO_CLARA -> Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(esc.d(10 * k)).background(Color.White.copy(alpha = 0.3f)),
            )
        }
        if (disco) {
            // Brillo y agujero del disco.
            Box(Modifier.fillMaxSize().background(Brush.sweepGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.18f), Color.Transparent, Color.White.copy(alpha = 0.12f), Color.Transparent))))
            Box(Modifier.align(Alignment.Center).size(esc.d(f.ancho * 0.22f)).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)))
            Box(Modifier.align(Alignment.Center).size(esc.d(f.ancho * 0.1f)).clip(CircleShape).background(LocalPaleta.current.fondo2))
        }
        if (!tieneImagen && !disco) {
            Texto(
                juego.titulo, if (f.ancho < 70) 9.5f else if (f.ancho < 90) 10.5f else if (f.ancho < 130) 11.5f else 13f,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = esc.d(8), end = esc.d(8), bottom = esc.d(7)),
                color = Color.White, lineas = 3, interlineado = if (f.ancho < 130) 13f else 15f, sombra = true,
            )
        }
    }
}

/** Ícono del formato físico de cada consola (cartucho, CD, DVD, tarjeta...). */
@Composable
fun IconoMedio(medio: Medio, color: Color, fondo: Color, modifier: Modifier = Modifier, tam: Float = 44f) {
    val esc = LocalEscala.current
    Canvas(modifier.size(esc.d(tam))) {
        val k = size.width / 44f
        val cx = size.width / 2
        val cy = size.height / 2
        fun rr(x: Float, y: Float, w: Float, h: Float, r: Float, c: Color) =
            drawRoundRect(c, Offset(cx + (x - 22) * k, cy + (y - 22) * k), Size(w * k, h * k), CornerRadius(r * k))
        fun estrias(x: Float, y: Float, w: Float, h: Float) {
            var px = x + 1
            while (px < x + w - 1) {
                drawRect(Color.White.copy(alpha = 0.45f), Offset(cx + (px - 22) * k, cy + (y - 22) * k), Size(2 * k, h * k))
                px += 5
            }
        }
        val etiqueta = Color.White.copy(alpha = 0.75f)
        when (medio) {
            Medio.CARTUCHO_ANCHO -> {
                rr(2f, 7f, 40f, 30f, 6f, color)
                estrias(4f, 7f, 36f, 7f)
                rr(9f, 20f, 26f, 12f, 2f, etiqueta)
            }
            Medio.CARTUCHO_N64 -> {
                val p = Path().apply {
                    moveTo(cx - 20 * k, cy - 16 * k); lineTo(cx + 20 * k, cy - 16 * k)
                    lineTo(cx + 15 * k, cy + 16 * k); lineTo(cx - 15 * k, cy + 16 * k); close()
                }
                drawPath(p, color)
                estrias(4f, 6f, 36f, 7f)
                rr(11f, 19f, 22f, 12f, 2f, etiqueta)
            }
            Medio.CD, Medio.DVD, Medio.BLURAY -> {
                val radio = 19 * k
                when (medio) {
                    Medio.CD -> drawCircle(Brush.sweepGradient(listOf(color, Color(0xFFE9E5FF), color, Color.White, color), Offset(cx, cy)), radio, Offset(cx, cy))
                    Medio.BLURAY -> {
                        drawCircle(Brush.sweepGradient(listOf(color, Color(0xFF6FA8FF), color, Color(0xFFB9D3FF), color), Offset(cx, cy)), radio, Offset(cx, cy))
                        drawCircle(Color(0xFF6FA8FF).copy(alpha = 0.6f), 13f * k, Offset(cx, cy), style = Stroke(1.5f * k))
                    }
                    else -> {
                        drawCircle(color, radio, Offset(cx, cy))
                        drawCircle(Color.White.copy(alpha = 0.35f), 16.5f * k, Offset(cx, cy), style = Stroke(1.5f * k))
                    }
                }
                drawCircle(Color.White.copy(alpha = 0.75f), 8 * k, Offset(cx, cy))
                drawCircle(fondo, 4 * k, Offset(cx, cy))
            }
            Medio.CARTUCHO_CHICO -> {
                drawRoundRect(color, Offset(cx - 18 * k, cy - 13 * k), Size(36 * k, 26 * k), CornerRadius(5 * k))
                drawCircle(fondo, 5 * k, Offset(cx, cy - 13 * k))
                rr(11f, 20f, 22f, 11f, 2f, etiqueta)
            }
            Medio.MINIDISCO -> {
                rr(5f, 5f, 34f, 34f, 9f, color)
                drawCircle(Color.White.copy(alpha = 0.35f), 12.5f * k, Offset(cx, cy))
                drawCircle(Color.White.copy(alpha = 0.85f), 6 * k, Offset(cx, cy))
                drawCircle(fondo, 3 * k, Offset(cx, cy))
            }
            Medio.TARJETA -> {
                rr(7f, 4.5f, 30f, 35f, 5f, color)
                estrias(8f, 4.5f, 28f, 6f)
                rr(12f, 19f, 20f, 16f, 2f, etiqueta)
            }
            Medio.UMD -> {
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = cx - 16 * k, top = cy - 19 * k, right = cx + 16 * k, bottom = cy + 19 * k,
                            topLeftCornerRadius = CornerRadius(16 * k), topRightCornerRadius = CornerRadius(16 * k),
                            bottomRightCornerRadius = CornerRadius(6 * k), bottomLeftCornerRadius = CornerRadius(6 * k),
                        ),
                    )
                }
                drawPath(path, color)
                drawCircle(Color.White.copy(alpha = 0.8f), 9 * k, Offset(cx, cy - 3 * k))
            }
            Medio.APP -> {
                rr(5f, 5f, 34f, 34f, 10f, color)
                drawCircle(Color.White.copy(alpha = 0.9f), 4.2f * k, Offset(cx - 6.5f * k, cy - 6.5f * k))
                drawCircle(Color.White.copy(alpha = 0.9f), 4.2f * k, Offset(cx + 6.5f * k, cy - 6.5f * k))
                drawCircle(Color.White.copy(alpha = 0.9f), 4.2f * k, Offset(cx - 6.5f * k, cy + 6.5f * k))
                drawCircle(Sol, 4.2f * k, Offset(cx + 6.5f * k, cy + 6.5f * k))
            }
            Medio.MOVIL -> {
                rr(11f, 3f, 22f, 38f, 5f, color)
                rr(13.5f, 7f, 17f, 26f, 2f, Color.White.copy(alpha = 0.85f))
                val tri = Path().apply {
                    moveTo(cx - 3.5f * k, cy - 5f * k); lineTo(cx + 5f * k, cy - 1f * k); lineTo(cx - 3.5f * k, cy + 3f * k); close()
                }
                drawPath(tri, color)
                drawCircle(Color.White.copy(alpha = 0.85f), 1.6f * k, Offset(cx, cy + 16.5f * k))
            }
            Medio.ARCADE -> {
                rr(10f, 3f, 24f, 38f, 3f, color)
                rr(13f, 7f, 18f, 13f, 2f, Color.White.copy(alpha = 0.85f))
                rr(8f, 23f, 28f, 6f, 2f, color)
                drawCircle(Sol, 2.6f * k, Offset(cx - 5f * k, cy + 1.5f * k))
                drawCircle(Coral, 2f * k, Offset(cx + 3f * k, cy + 1.5f * k))
                drawCircle(Coral, 2f * k, Offset(cx + 8f * k, cy + 1.5f * k))
            }
            Medio.DISQUETE -> {
                rr(6f, 6f, 32f, 32f, 4f, color)
                rr(13f, 6f, 18f, 11f, 1.5f, Color.White.copy(alpha = 0.75f))
                rr(25f, 8f, 4f, 7f, 1f, color)
                rr(11f, 23f, 22f, 13f, 2f, Color.White.copy(alpha = 0.85f))
            }
            Medio.TODOS -> {
                rr(18f, 4f, 22f, 28f, 5f, Sol)
                rr(12f, 8f, 22f, 28f, 5f, Coral)
                rr(6f, 12f, 22f, 28f, 5f, color)
            }
        }
    }
}

/** La marca de Díptico: dos paneles como la Thor, el de arriba más ancho que el de abajo. */
@Composable
fun MarcaLogo(tam: Float, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    androidx.compose.foundation.layout.Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(esc.d(tam * 0.14f)),
    ) {
        Box(Modifier.size(esc.d(tam * 1.25f), esc.d(tam * 0.5f)).clip(RoundedCornerShape(esc.d(tam * 0.2f))).background(Uva))
        Box(Modifier.size(esc.d(tam * 0.95f), esc.d(tam * 0.5f)).clip(RoundedCornerShape(esc.d(tam * 0.2f))).background(Coral))
    }
}

/** Logo de Díptico: la marca y el nombre. */
@Composable
fun Logo(tam: Float = 20f, color: Color = LocalPaleta.current.tinta) {
    val esc = LocalEscala.current
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        MarcaLogo(tam)
        Box(Modifier.width(esc.d(tam * 0.5f)))
        Texto("Díptico", tam, color = color)
    }
}

val SinEspacio = 0.dp

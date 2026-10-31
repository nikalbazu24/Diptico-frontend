package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.nikalbazu.diptico.R

data class Paleta(
    val fondo: Color,
    val fondo2: Color,
    val superficie: Color,
    val tinta: Color,
    val tenue: Color,
    val linea: Color,
    val acentoTexto: Color,
    val estante: Color,
)

val PaletaOscura = Paleta(
    fondo = Color(0xFF0F0D18),
    fondo2 = Color(0xFF14121F),
    superficie = Color(0xFF25223A),
    tinta = Color(0xFFF2F0FF),
    tenue = Color(0xFFA9A5C7),
    linea = Color(0xFF2E2B47),
    acentoTexto = Color(0xFFB3A4FF),
    estante = Color(0x1AFFFFFF),
)

val PaletaClara = Paleta(
    fondo = Color(0xFFF4F1FF),
    fondo2 = Color(0xFFFBFAFF),
    superficie = Color(0xFFFFFFFF),
    tinta = Color(0xFF1C1A33),
    tenue = Color(0xFF5D5A7A),
    linea = Color(0xFFDCD5F3),
    acentoTexto = Color(0xFF5B3FD9),
    estante = Color(0x241C1A33),
)

val Uva = Color(0xFF6B4EFF)
val UvaSombra = Color(0xFF4F35D6)
val Coral = Color(0xFFFF7A59)
val Sol = Color(0xFFFFC93C)
val Tinta = Color(0xFF1C1A33)

val Fredoka = FontFamily(
    Font(R.font.fredoka_medium, FontWeight.Medium),
    Font(R.font.fredoka_semibold, FontWeight.SemiBold),
    Font(R.font.fredoka_bold, FontWeight.Bold),
)

val Nunito = FontFamily(
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

/** Convierte las medidas del diseño (en px del boceto) a medidas reales de la pantalla. */
class Escala(val factor: Float) {
    fun d(x: Float): Dp = (x * factor).dp
    fun d(x: Int): Dp = d(x.toFloat())
    fun s(x: Float): TextUnit = (x * factor).sp
    fun s(x: Int): TextUnit = s(x.toFloat())
}

val LocalEscala = staticCompositionLocalOf { Escala(1f) }
val LocalPaleta = staticCompositionLocalOf { PaletaOscura }

/**
 * Lienzo de diseño: escala el contenido para que un diseño de [ancho]x[alto]
 * llene la pantalla real sin deformarse.
 */
@Composable
fun Lienzo(ancho: Float, alto: Float, oscuro: Boolean, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    val paleta = if (oscuro) PaletaOscura else PaletaClara
    BoxWithConstraints(modifier.background(paleta.fondo)) {
        val f = minOf(maxWidth.value / ancho, maxHeight.value / alto)
        CompositionLocalProvider(LocalEscala provides Escala(f), LocalPaleta provides paleta) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(Modifier.size((ancho * f).dp, (alto * f).dp)) { contenido() }
            }
        }
    }
}

@Composable
fun Texto(
    texto: String,
    tam: Float,
    modifier: Modifier = Modifier,
    color: Color = LocalPaleta.current.tinta,
    peso: FontWeight = FontWeight.Bold,
    familia: FontFamily = Fredoka,
    lineas: Int = Int.MAX_VALUE,
    interlineado: Float? = null,
    espaciado: Float? = null,
    sombra: Boolean = false,
    alinear: TextAlign? = null,
) {
    val esc = LocalEscala.current
    Text(
        text = texto,
        modifier = modifier,
        color = color,
        fontSize = esc.s(tam),
        fontWeight = peso,
        fontFamily = familia,
        maxLines = lineas,
        overflow = TextOverflow.Ellipsis,
        lineHeight = interlineado?.let { esc.s(it) } ?: TextUnit.Unspecified,
        letterSpacing = espaciado?.let { esc.s(it) } ?: TextUnit.Unspecified,
        textAlign = alinear,
        style = if (sombra) TextStyle(shadow = Shadow(Color(0x66000000), Offset(0f, 2f), 10f)) else TextStyle.Default,
    )
}

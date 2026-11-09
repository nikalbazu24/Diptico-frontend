package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cl.nikalbazu.diptico.estado.Tema
import cl.nikalbazu.diptico.estado.Temas
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
    tenue = Color(0xFFB2AED0),
    linea = Color(0xFF2E2B47),
    acentoTexto = Color(0xFFB3A4FF),
    estante = Color(0x1AFFFFFF),
)

val PaletaClara = Paleta(
    fondo = Color(0xFFF4F1FF),
    fondo2 = Color(0xFFFBFAFF),
    superficie = Color(0xFFFFFFFF),
    tinta = Color(0xFF1C1A33),
    tenue = Color(0xFF55526F),
    linea = Color(0xFFDCD5F3),
    acentoTexto = Color(0xFF5B3FD9),
    estante = Color(0x241C1A33),
)

/** Tema elegido y si se dibujan sus decoraciones. Al cambiarlos, toda la app se repinta sola. */
object Estilo {
    var tema by mutableStateOf(Temas.incluidos[0])
    var materialInterfaz by mutableStateOf("glass")
    /** Liquid Glass quedó apagado mientras se rediseña junto con el nuevo Díptico (9 nov 2026). */
    val glass: Boolean get() = false
    var decoraciones by mutableStateOf(true)
    /** Fondo de los botones: "liso", "degradado", "brillo" o "vidrio". */
    var efecto by mutableStateOf("degradado")
    /** Animaciones y difuminados completos (se apagan en modo ahorro). */
    var efectosCompletos by mutableStateOf(true)
}

val Uva: Color get() = Color(Estilo.tema.acento)
val UvaSombra: Color get() = lerp(Uva, Color.Black, 0.22f)
val Coral: Color get() = Color(Estilo.tema.acento2)
val Sol: Color get() = Color(Estilo.tema.acento3)
val Tinta = Color(0xFF1C1A33)
val Cielo = Color(0xFF3BA8FF)

/** Color de letra que se lee bien sobre el color principal (blanco, o tinta si el color es muy claro). */
val SobreUva: Color get() = if (Uva.luminance() > 0.55f) Tinta else Color.White

/** Arma la paleta de fondo, superficies y letras a partir del tema y del modo claro/oscuro. */
fun paletaDe(t: Tema, oscuroPersona: Boolean): Paleta {
    val base = paletaBase(t, oscuroPersona)
    if (t.forma != "cristal") return base
    val claro = base.fondo.luminance() > 0.45f
    val fondo = if (t.id == "diptico_vidrio") {
        if (claro) Color(0xFFE9EDF4) else Color(0xFF10131B)
    } else base.fondo
    val velo = if (claro) Color.White else Color(0xFFBDC8E1)
    return base.copy(
        fondo = fondo,
        fondo2 = lerp(fondo, velo, if (claro) 0.72f else 0.045f).copy(alpha = 0.94f),
        superficie = lerp(fondo, velo, if (claro) 0.92f else 0.11f).copy(alpha = 0.95f),
        linea = lerp(fondo, base.tinta, if (claro) 0.16f else 0.22f),
    )
}

private fun paletaBase(t: Tema, oscuroPersona: Boolean): Paleta {
    if (t.base == "gb") {
        return Paleta(
            fondo = Color(0xFF0F380F), fondo2 = Color(0xFF16461A), superficie = Color(0xFF306230), tinta = Color(0xFFE3F5C8),
            tenue = Color(0xFF9BBC0F), linea = Color(0xFF306230), acentoTexto = Color(0xFF9BBC0F), estante = Color(0x339BBC0F),
        )
    }
    val oscuro = when (t.base) {
        "oscuro", "oled" -> true
        "claro" -> false
        else -> oscuroPersona
    }
    if (t.id == "diptico" && t.acento == Temas.incluidos[0].acento && t.fondo == 0L && t.tinta == 0L) return if (oscuro) PaletaOscura else PaletaClara
    val a = Color(t.acento)
    // Fondo y letras propios (Mi tema › Colores).
    if (t.fondo != 0L || t.tinta != 0L) {
        val f = if (t.fondo != 0L) Color(t.fondo) else if (oscuro) lerp(Color(0xFF0B0A12), a, 0.07f) else lerp(Color(0xFFF7F6FB), a, 0.07f)
        val claro = f.luminance() > 0.45f
        val tinta = if (t.tinta != 0L) Color(t.tinta) else if (claro) Color(0xFF1C1A2E) else Color(0xFFF4F2FF)
        val hacia = if (claro) Color.Black else Color.White
        return Paleta(
            fondo = f,
            fondo2 = lerp(f, hacia, 0.03f),
            superficie = lerp(f, hacia, if (claro) 0.06f else 0.1f),
            tinta = tinta,
            tenue = lerp(tinta, f, 0.35f),
            linea = lerp(f, hacia, 0.16f),
            acentoTexto = lerp(a, if (claro) Color.Black else Color.White, if (claro) 0.28f else 0.38f),
            estante = if (claro) Color(0x241C1A33) else Color(0x1AFFFFFF),
        )
    }
    return if (oscuro) {
        val negro = t.base == "oled"
        Paleta(
            fondo = if (negro) Color.Black else lerp(Color(0xFF0B0A12), a, 0.07f),
            fondo2 = if (negro) Color(0xFF050507) else lerp(Color(0xFF100F19), a, 0.09f),
            superficie = lerp(Color(0xFF1E1C2C), a, 0.16f),
            tinta = Color(0xFFF4F2FF),
            tenue = lerp(Color(0xFFAEAAC6), a, 0.15f),
            linea = lerp(Color(0xFF2A2840), a, 0.2f),
            acentoTexto = lerp(a, Color.White, 0.38f),
            estante = Color(0x1AFFFFFF),
        )
    } else {
        Paleta(
            fondo = lerp(Color(0xFFF7F6FB), a, 0.07f),
            fondo2 = lerp(Color(0xFFFFFFFF), a, 0.035f),
            superficie = Color.White,
            tinta = lerp(Color(0xFF1C1A2E), a, 0.1f),
            tenue = lerp(Color(0xFF5C5A72), a, 0.12f),
            linea = lerp(Color(0xFFE2E0EE), a, 0.25f),
            acentoTexto = lerp(a, Color.Black, 0.28f),
            estante = Color(0x241C1A33),
        )
    }
}

private val FredokaBase = FontFamily(
    Font(R.font.fredoka_medium, FontWeight.Medium),
    Font(R.font.fredoka_semibold, FontWeight.SemiBold),
    Font(R.font.fredoka_bold, FontWeight.Bold),
)

private val NunitoBase = FontFamily(
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

/** Letra de píxeles (Tiny5, licencia OFL) para el tema Mundo de bloques. Un solo grosor: así los píxeles quedan nítidos. */
val Pixel = FontFamily(
    Font(R.font.tiny5, FontWeight.Normal),
    Font(R.font.tiny5, FontWeight.Medium),
    Font(R.font.tiny5, FontWeight.SemiBold),
    Font(R.font.tiny5, FontWeight.Bold),
    Font(R.font.tiny5, FontWeight.ExtraBold),
)

internal fun fuenteTema(tipo: String, base: FontFamily = FredokaBase): FontFamily = when (tipo) {
    "pixel" -> Pixel
    "sans" -> FontFamily.SansSerif
    "serif" -> FontFamily.Serif
    "mono" -> FontFamily.Monospace
    "redonda" -> FredokaBase
    "suave" -> NunitoBase
    else -> base
}

val Fredoka: FontFamily get() = fuenteTema(Estilo.tema.fuente, FredokaBase)
val Nunito: FontFamily get() = fuenteTema(Estilo.tema.fuente, NunitoBase)

/** Alto del lienzo de diseño, en unidades. Toda pantalla mide 400 de alto; el ancho depende de su forma. */
const val ALTO_DISENO = 400f

/**
 * Convierte las medidas del diseño a medidas reales de la pantalla.
 * [ancho] es el ancho disponible en unidades de diseño (unas 711 en la pantalla de arriba de la Thor, unas 460 en la de abajo).
 * [texto] agranda solo las letras, según el ajuste de tamaño de letra.
 */
class Escala(val factor: Float, val ancho: Float = 711f, val texto: Float = 1f) {
    fun d(x: Float): Dp = (x * factor).dp
    fun d(x: Int): Dp = d(x.toFloat())
    fun s(x: Float): TextUnit = (x * factor * texto).sp
    fun s(x: Int): TextUnit = s(x.toFloat())

    /** Pantalla angosta (la de abajo de la Thor, o un teléfono). */
    val angosta: Boolean get() = ancho < 600f
}

val LocalEscala = staticCompositionLocalOf { Escala(1f) }

/**
 * Cambio de ambiente suave: al pasar de claro a oscuro (o cambiar de tema) los colores se funden en vez de saltar,
 * como cuando cae la tarde. Sin efectos completos o con las animaciones del sistema apagadas, cambia al instante.
 */
@Composable
private fun paletaSuave(destino: Paleta): Paleta {
    if (!Estilo.efectosCompletos || !android.animation.ValueAnimator.areAnimatorsEnabled()) return destino
    val t = androidx.compose.animation.core.tween<Color>(650, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    @Composable fun c(x: Color) = androidx.compose.animation.animateColorAsState(x, t, label = "ambiente").value
    return Paleta(c(destino.fondo), c(destino.fondo2), c(destino.superficie), c(destino.tinta), c(destino.tenue),
        c(destino.linea), c(destino.acentoTexto), c(destino.estante))
}
val LocalPaleta = staticCompositionLocalOf { PaletaOscura }

/**
 * Lienzo de diseño: siempre 400 unidades de alto y tan ancho como la forma de la pantalla.
 * Así el diseño llena cualquier pantalla (arriba, abajo, intercambiadas o un teléfono) sin deformarse ni achicarse.
 */
@Composable
fun Lienzo(oscuro: Boolean, modifier: Modifier = Modifier, tamTexto: Float = 1f, zoom: Float = 1f, contenido: @Composable () -> Unit) {
    val paleta = paletaDe(Estilo.tema, oscuro)
    BoxWithConstraints(modifier.background(paleta.fondo)) {
        val alto = if (maxHeight.value > 0f) maxHeight.value else 1f
        // En pantallas verticales (un teléfono) manda el ancho, para que nada quede fuera.
        val corto = if (maxWidth.value > 0f) minOf(alto, maxWidth.value) else alto
        // Según la forma de la pantalla: en las casi cuadradas (como la de abajo de la Thor) todo se achica un poco
        // para que quepa más; en las anchas (16:9) se ve como fue diseñado.
        val forma = if (alto > 0f) maxWidth.value / alto else 1.78f
        val base = when {
            forma >= 1.6f -> ALTO_DISENO
            forma >= 1.3f -> ALTO_DISENO * 1.06f
            forma >= 0.9f -> ALTO_DISENO * 1.12f
            else -> ALTO_DISENO
        }
        val f = corto / (base / zoom.coerceIn(0.6f, 1.6f))
        val ancho = maxWidth.value / f
        CompositionLocalProvider(LocalEscala provides Escala(f, ancho, tamTexto), LocalPaleta provides paleta) {
            Box(Modifier.fillMaxSize()) {
                contenido()
                if (Estilo.decoraciones && Estilo.tema.decor != "nada") Decoracion(Estilo.tema)
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
    traducir: Boolean = true,
) {
    val esc = LocalEscala.current
    // Lee el idioma (estado de Compose) para que todo se redibuje al cambiarlo.
    val idioma = cl.nikalbazu.diptico.estado.Idioma.actual
    if (LocalTextoWidget.current && lineas <= 4) {
        TextoWidget(if (traducir && idioma != "es") cl.nikalbazu.diptico.estado.Idioma.t(texto) else texto,
            modifier, TextStyle(color = color, fontSize = esc.s(tam), fontWeight = peso, fontFamily = familia,
                lineHeight = interlineado?.let { esc.s(it) } ?: TextUnit.Unspecified,
                letterSpacing = espaciado?.let { esc.s(it) } ?: TextUnit.Unspecified,
                shadow = if (sombra) Shadow(Color(0x88000000), Offset(0f, 2f), 12f) else null), lineas, alinear)
        return
    }
    Text(
        text = if (traducir && idioma != "es") cl.nikalbazu.diptico.estado.Idioma.t(texto) else texto,
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
        style = if (sombra) TextStyle(shadow = Shadow(Color(0x88000000), Offset(0f, 2f), 12f)) else TextStyle.Default,
    )
}


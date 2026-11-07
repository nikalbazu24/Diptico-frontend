package cl.nikalbazu.diptico.renacer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.Temas
import cl.nikalbazu.diptico.ui.rememberImagen

@Composable
fun TemaRenacer(e: Salon, contenido: @Composable () -> Unit) {
    val fecha = remember(java.time.LocalDate.now(), e.calendario) { if (e.calendario) Temas.deHoy() else null }
    val fondo = Color(e.ambiente.fondo)
    val claro = fondo.luminance() > .5f
    val tinta = if (claro) Color(0xFF292B2A) else Color(0xFFF1ECE0)
    val acento = acentoLegible(fecha?.tema?.acento?.let(::Color) ?: Color(e.ambiente.acento), fondo)
    val esquema = if (claro) lightColorScheme(
        primary = acento, onPrimary = if (acento.luminance() > .179f) Color.Black else Color.White,
        background = fondo, onBackground = tinta, surface = lerp(fondo, Color.White, .48f),
        onSurface = tinta, onSurfaceVariant = Color(0xFF62635D), outline = Color(0xFFCBC5B8),
        secondaryContainer = Color(0xFFE6E0D4), onSecondaryContainer = tinta,
    ) else darkColorScheme(
        primary = acento, onPrimary = if (acento.luminance() > .179f) Color.Black else Color.White, background = fondo, onBackground = tinta,
        surface = lerp(fondo, Color.White, .045f), onSurface = tinta, onSurfaceVariant = Color(0xFFC0C3B8),
        outline = lerp(fondo, Color.White, .23f), secondaryContainer = lerp(fondo, Color.White, .1f), onSecondaryContainer = tinta,
    )
    MaterialTheme(colorScheme = esquema, typography = Typography(
        headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 40.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 32.sp),
        titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
        titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = .8.sp),
    ), content = contenido)
}

/** El acento también se usa como texto. Las fechas especiales deben conservar su legibilidad. */
internal fun acentoLegible(acento: Color, fondo: Color): Color {
    val destino = if (fondo.luminance() > .5f) Color.Black else Color.White
    var ajustado = acento
    repeat(12) {
        val luminosidades = listOf(ajustado.luminance(), fondo.luminance())
        val contraste = (luminosidades.max() + .05f) / (luminosidades.min() + .05f)
        if (contraste >= 4.5f) return ajustado
        ajustado = lerp(ajustado, destino, .12f)
    }
    return destino
}

/** La bisagra y sus dos hojas forman la marca. Se mantiene legible a tamaño de ícono. */
@Composable
fun Marca(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(color, Offset(0f, h * .12f), Size(w * .43f, h * .82f), androidx.compose.ui.geometry.CornerRadius(w * .08f))
        val hoja = Path().apply {
            moveTo(w * .57f, h * .12f); lineTo(w, 0f); lineTo(w, h * .82f)
            lineTo(w * .57f, h * .94f); close()
        }
        drawPath(hoja, color)
    }
}

/** Arte vectorial propio mientras la biblioteca no tenga una imagen para ese juego. */
@Composable
fun Paisaje(juego: Juego?, modifier: Modifier = Modifier) {
    val color = remember(juego?.consolaId) {
        val tonos = listOf(Color(0xFF44685D), Color(0xFF9C604B), Color(0xFF52677D), Color(0xFF6E637C))
        tonos[(juego?.consolaId.orEmpty().hashCode() and Int.MAX_VALUE) % tonos.size]
    }
    val imagen = rememberImagen(juego?.arte ?: juego?.portada, 1000)
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(color)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(lerp(color, Color.White, .23f), color)))
            drawCircle(Color(0xFFE7C996), size.minDimension * .18f, Offset(size.width * .72f, size.height * .28f))
            repeat(3) { plano ->
                val y = size.height * (.46f + plano * .16f)
                val camino = Path().apply {
                    moveTo(0f, size.height); lineTo(0f, y)
                    cubicTo(size.width * .28f, y - size.height * .18f, size.width * .46f, y + size.height * .3f, size.width, y - size.height * .04f)
                    lineTo(size.width, size.height); close()
                }
                drawPath(camino, lerp(color, Color(0xFF142D2A), .22f + plano * .23f))
            }
            drawLine(Color.White.copy(alpha = .13f), Offset(size.width * .4f, 0f), Offset(size.width * .4f, size.height), 1.dp.toPx())
        }
        if (imagen != null) Image(imagen, juego?.titulo, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
fun TarjetaEnfocable(
    modifier: Modifier = Modifier,
    elegido: Boolean = false,
    seleccionar: () -> Unit = {},
    activar: () -> Unit,
    contenido: @Composable () -> Unit,
) {
    var foco by remember { mutableStateOf(false) }
    val fuente = remember { MutableInteractionSource() }
    val colores = MaterialTheme.colorScheme
    Box(modifier.clip(RoundedCornerShape(18.dp))
        .background(if (elegido) colores.secondaryContainer else colores.surface)
        .border(if (foco) 3.dp else 1.dp, if (foco) colores.primary else colores.outline.copy(alpha = .45f), RoundedCornerShape(18.dp))
        .onFocusChanged { foco = it.isFocused; if (it.isFocused) seleccionar() }
        .clickable(interactionSource = fuente, indication = null) { seleccionar(); activar() }
) { contenido() }
}

@Composable
fun Eyebrow(texto: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(texto, modifier, style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
fun MensajeVacio(titulo: String, texto: String, modifier: Modifier = Modifier, accion: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(modifier.padding(24.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)) {
        Text(titulo, style = MaterialTheme.typography.headlineMedium)
        Text(texto, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        accion()
    }
}

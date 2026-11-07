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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.ui.LocalPaleta
import cl.nikalbazu.diptico.ui.dibujarHardware
import cl.nikalbazu.diptico.ui.fuenteTema
import cl.nikalbazu.diptico.ui.paletaDe
import cl.nikalbazu.diptico.ui.rememberImagen

@Composable
fun TemaRenacer(e: Salon, contenido: @Composable () -> Unit) {
    val hoy = java.time.LocalDate.now()
    val tema = remember(e.temas, e.temaId, e.calendario, hoy) { IdentidadRenacer.tema(e, hoy) }
    val paleta = remember(tema) { paletaDe(tema, true) }
    val acento = acentoLegible(Color(tema.acento), paleta.fondo)
    val claro = paleta.fondo.luminance() > .5f
    val tintaBoton = if (acento.luminance() > .179f) Color.Black else Color.White
    val esquema = if (claro) lightColorScheme(
        primary = acento, onPrimary = tintaBoton, secondary = Color(tema.acento2),
        background = paleta.fondo, onBackground = paleta.tinta, surface = paleta.superficie,
        onSurface = paleta.tinta, onSurfaceVariant = paleta.tenue, outline = paleta.linea,
        secondaryContainer = lerp(paleta.fondo, acento, .1f), onSecondaryContainer = paleta.tinta,
    ) else darkColorScheme(
        primary = acento, onPrimary = tintaBoton, secondary = Color(tema.acento2),
        background = paleta.fondo, onBackground = paleta.tinta, surface = paleta.superficie,
        onSurface = paleta.tinta, onSurfaceVariant = paleta.tenue, outline = paleta.linea,
        secondaryContainer = lerp(paleta.fondo, acento, .15f), onSecondaryContainer = paleta.tinta,
    )
    val familia = fuenteTema(tema.fuente, FontFamily.SansSerif)
    CompositionLocalProvider(LocalTemaRenacer provides tema, LocalPaleta provides paleta) {
        MaterialTheme(colorScheme = esquema, typography = Typography(
            headlineLarge = TextStyle(fontFamily = familia, fontWeight = FontWeight.ExtraBold, fontSize = 35.sp, lineHeight = 38.sp),
            headlineMedium = TextStyle(fontFamily = familia, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp),
            titleLarge = TextStyle(fontFamily = familia, fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
            titleMedium = TextStyle(fontFamily = familia, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
            bodyLarge = TextStyle(fontFamily = familia, fontSize = 16.sp, lineHeight = 23.sp),
            bodyMedium = TextStyle(fontFamily = familia, fontSize = 14.sp, lineHeight = 20.sp),
            labelLarge = TextStyle(fontFamily = familia, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold),
            labelSmall = TextStyle(fontFamily = familia, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = .8.sp),
        ), content = contenido)
    }
}

/** Dos pantallas y los botones del mando forman el monograma. */
@Composable
fun Marca(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val borde = size.minDimension * .07f
        drawRoundRect(color, Offset(w * .04f, h * .06f), Size(w * .66f, h * .4f),
            androidx.compose.ui.geometry.CornerRadius(borde), style = Stroke(borde))
        drawRoundRect(color, Offset(w * .17f, h * .55f), Size(w * .66f, h * .38f),
            androidx.compose.ui.geometry.CornerRadius(borde), style = Stroke(borde))
        drawCircle(color, borde * .7f, Offset(w * .91f, h * .65f))
        drawCircle(color, borde * .7f, Offset(w * .91f, h * .86f))
    }
}

/** Fondo de reserva con perspectiva y luz. Las imágenes del usuario tienen prioridad. */
@Composable
fun Paisaje(juego: Juego?, modifier: Modifier = Modifier) {
    val tema = LocalTemaRenacer.current
    val fondo = MaterialTheme.colorScheme.background
    val acento = Color(tema.acento)
    val secundario = Color(tema.acento2)
    val imagen = rememberImagen(juego?.arte ?: juego?.portada, 1000)
    Box(modifier.clip(marcoRenacer()).background(fondo)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(Brush.linearGradient(listOf(lerp(fondo, secundario, .28f), fondo), Offset(w, 0f), Offset(0f, h)))
            val centro = Offset(w * .76f, h * .36f)
            drawCircle(Brush.radialGradient(listOf(acento.copy(alpha = .27f), Color.Transparent), centro, h * .7f), h * .7f, centro)
            drawCircle(secundario.copy(alpha = .7f), h * .2f, centro, style = Stroke(2.dp.toPx()))
            drawCircle(acento.copy(alpha = .4f), h * .24f, centro, style = Stroke(1.dp.toPx()))
            val horizonte = Offset(w * .68f, h * .53f)
            repeat(15) { i ->
                val fin = Offset((i - 5) * w / 5, h)
                drawLine(acento.copy(alpha = .18f), horizonte, fin, 1.dp.toPx())
            }
            repeat(7) { i ->
                val avance = (i + 1) / 7f
                val y = horizonte.y + (h - horizonte.y) * avance * avance
                drawLine(secundario.copy(alpha = .24f), Offset(0f, y), Offset(w, y), 1.dp.toPx())
            }
            repeat(24) { i ->
                val x = ((i * 71 + 17) % 307) / 307f * w
                val y = ((i * 41 + 11) % 181) / 181f * h * .48f
                drawRect(acento.copy(alpha = .35f), Offset(x, y), Size(2.dp.toPx(), 2.dp.toPx()))
            }
        }
        if (imagen != null) Image(imagen, juego?.titulo, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
internal fun EmblemaConsola(id: String, modifier: Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val consola = Consolas.porId(id)
    val fondo = MaterialTheme.colorScheme.background
    if (consola == null) Marca(modifier, color)
    else Canvas(modifier) { dibujarHardware(consola, color, fondo) }
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
    val forma = marcoRenacer()
    Box(modifier.clip(forma)
        .background(if (elegido || foco) colores.secondaryContainer else colores.surface)
        .border(if (foco || elegido) 2.dp else 1.dp, if (foco || elegido) colores.primary else colores.outline.copy(alpha = .7f), forma)
        .onFocusChanged { foco = it.isFocused; if (it.isFocused) seleccionar() }
        .clickable(interactionSource = fuente, indication = null) { seleccionar(); activar() }) { contenido() }
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

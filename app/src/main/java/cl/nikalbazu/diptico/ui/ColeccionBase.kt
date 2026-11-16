package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.estado.*

/*
 * Colección viva: la base común de los bocetos aprobados (docs/coleccion-viva.md).
 *
 * [LienzoBoceto] arma un lienzo del tamaño del boceto (1920×1080 o 1240×1080) y entrega un [Escala] donde 1 unidad es
 * 1 píxel del boceto. Así todas las medidas se copian tal cual del boceto (esc.d(96) = los 96 px de margen) y también
 * los componentes de siempre (Texto, IconoDiptico, FilaDock…) quedan del tamaño correcto.
 */

/** Los colores de Colección viva: con un tema oscuro son los del boceto; con otro, salen de su paleta. */
@Immutable
data class ColoresColeccion(
    val fondo: Color,
    val fondo2: Color,
    val vidrio: Color,
    val vidrioFuerte: Color,
    val borde: Color,
    val texto: Color,
    val sub: Color,
    val tenue: Color,
    val elegido: Color,
    val enElegido: Color,
    val acento: Color,
    /** Cuánto se redondean las esquinas según la forma del tema (1 = como el boceto). */
    val radio: Float,
)

val LocalColeccion = staticCompositionLocalOf {
    ColoresColeccion(
        Color(0xFF07080F), Color(0xFF0B0C16), Color(0x8C121424), Color(0x99121424), Color(0x1FFFFFFF),
        Color(0xFFF4F3FA), Color(0xFFB9BBD6), Color(0xFF8E90AE), Color(0xFFF4F3FA), Color(0xFF12131F), Color(0xFF6B8CFF), 1f,
    )
}

@Composable
fun coloresColeccion(): ColoresColeccion {
    val p = LocalPaleta.current
    val tema = Estilo.tema
    val radio = when (tema.forma) { "pixel", "recorte" -> 0.25f; "recta" -> 0.4f; "burbuja" -> 1.3f; "neon" -> 0.8f; else -> 1f }
    val acento = Uva
    val oscura = p.fondo.luminance() < 0.3f
    // Todo sale del tema: así cada tema cambia de verdad el diseño nuevo (antes los oscuros quedaban todos iguales).
    val vidrio = androidx.compose.ui.graphics.lerp(p.fondo, p.tinta, if (oscura) 0.07f else 0.0f).copy(alpha = if (oscura) 0.62f else 0.82f)
    return ColoresColeccion(
        fondo = p.fondo, fondo2 = androidx.compose.ui.graphics.lerp(p.fondo, acento, if (oscura) 0.06f else 0.04f),
        vidrio = if (oscura) vidrio else Color.White.copy(alpha = 0.72f),
        vidrioFuerte = if (oscura) vidrio.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.86f),
        borde = p.tinta.copy(alpha = if (oscura) 0.12f else 0.16f),
        texto = p.tinta, sub = p.tenue, tenue = p.tenue.copy(alpha = 0.8f),
        elegido = if (oscura) p.tinta else acento, enElegido = if (oscura) p.fondo else Color.White, acento = acento, radio = radio,
    )
}

/**
 * Un lienzo del tamaño del boceto, escalado para llenar la pantalla. [zoom] agranda todo (las pantallas de la Thor son chicas:
 * sin zoom, 1 px del boceto queda en ~0,38 dp y la letra sale de 8–10 sp). El lienzo lógico queda de (ancho/zoom × alto/zoom) y
 * crece hacia el lado que sobre; por eso los diseños se arman en franjas relativas, no con posiciones fijas.
 */
@Composable
fun LienzoBoceto(ancho: Float, alto: Float, modifier: Modifier = Modifier, fondo: Color? = null, zoom: Float = 1f, contenido: @Composable BoxScope.() -> Unit) {
    val base = LocalEscala.current
    val colores = coloresColeccion()
    BoxWithConstraints(modifier.fillMaxSize().then(if (fondo != null) Modifier.background(fondo) else Modifier), contentAlignment = Alignment.Center) {
        val s = (minOf(maxWidth.value / ancho, maxHeight.value / alto) * zoom * base.texto.coerceIn(0.85f, 1.3f)).coerceAtLeast(0.01f)
        val w = maxWidth.value / s
        val h = maxHeight.value / s
        CompositionLocalProvider(
            LocalEscala provides Escala(s, w, 1f),
            LocalColeccion provides colores,
            LocalAltoLienzo provides h,
        ) {
            Box(Modifier.requiredSize((w * s).dp, (h * s).dp), content = contenido)
        }
    }
}

/** Zoom de cada pantalla: la de abajo de la Thor es la más chica. */
const val ZOOM_ARRIBA = 1.45f
const val ZOOM_ABAJO = 1.65f
const val ZOOM_UNA = 1.35f

val LocalAltoLienzo = staticCompositionLocalOf { 1080f }

/** El ancho y alto del lienzo en píxeles del boceto (con zoom, menos que el boceto). */
@Composable
fun anchoLienzo(): Float = LocalEscala.current.ancho

@Composable
fun altoLienzo(): Float = LocalAltoLienzo.current

/** Forma redondeada que respeta la forma del tema. */
@Composable
fun redondo(px: Float): Shape {
    val esc = LocalEscala.current
    val c = LocalColeccion.current
    return RoundedCornerShape(esc.d(px * c.radio))
}

/** Pone algo en una posición fija del boceto (left/top en px). */
fun Modifier.en(esc: Escala, x: Float, y: Float): Modifier = this.offset(esc.d(x), esc.d(y))

/** Vidrio: el fondo translúcido con su borde fino. */
@Composable
fun Modifier.vidrio(radioPx: Float, fuerte: Boolean = false): Modifier {
    val c = LocalColeccion.current
    val forma = redondo(radioPx)
    return this.clip(forma).background(if (fuerte) c.vidrioFuerte else c.vidrio).border(BorderStroke(1.dp, c.borde), forma)
}

/** Texto del boceto: tamaño en px del boceto. */
@Composable
fun TB(
    texto: String, px: Float, modifier: Modifier = Modifier, color: Color = LocalColeccion.current.texto,
    peso: FontWeight = FontWeight.Bold, titulo: Boolean = false, lineas: Int = 1, interlineado: Float? = null,
    espaciado: Float? = null, sombra: Boolean = false, traducir: Boolean = true,
) = Texto(texto, px, modifier, color, peso, if (titulo) Fredoka else Nunito, lineas, interlineado, espaciado, sombra, traducir = traducir)

/** Botón de acción del boceto: la tecla en un círculo y su texto. Principal = blanco (A); si no, vidrio. */
@Composable
fun BotonTecla(
    tecla: String, texto: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    alto: Float = 92f, principal: Boolean = false, letra: Float = 30f,
) {
    val esc = LocalEscala.current
    val c = LocalColeccion.current
    val forma = RoundedCornerShape(50)
    Row(
        modifier.height(esc.d(alto)).clip(forma)
            .then(if (principal) Modifier.background(c.elegido) else Modifier.background(c.vidrio).border(1.dp, c.texto.copy(alpha = 0.18f), forma))
            .clickable(onClick = onClick)
            .padding(start = esc.d(alto * 0.2f), end = esc.d(alto * if (principal) 0.48f else 0.4f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(esc.d(alto * 0.18f)),
    ) {
        val d = alto * 0.62f
        Box(
            Modifier.size(esc.d(d)).clip(CircleShape)
                .then(if (principal) Modifier.background(c.enElegido) else Modifier.border(2.dp, c.texto, CircleShape)),
            contentAlignment = Alignment.Center,
        ) { TB(tecla, d * 0.45f, color = if (principal) c.elegido else c.texto, peso = FontWeight.ExtraBold, traducir = false) }
        TB(texto, letra, color = if (principal) c.enElegido else c.texto, peso = FontWeight.ExtraBold)
    }
}

/** Una pista de botón: la tecla en un óvalo con borde y lo que hace. */
@Composable
fun PistaTecla(tecla: String, texto: String, px: Float = 22f) {
    val esc = LocalEscala.current
    val c = LocalColeccion.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
        Box(
            Modifier.height(esc.d(px * 1.9f)).border(2.dp, c.texto, RoundedCornerShape(50)).padding(horizontal = esc.d(px * 0.45f)),
            contentAlignment = Alignment.Center,
        ) { TB(tecla, px * 0.78f, peso = FontWeight.ExtraBold, traducir = false) }
        TB(texto, px)
    }
}

/** Chip de dato («12 h jugadas»): blanco al 10 %. */
@Composable
fun ChipDato(texto: String, px: Float = 23f) {
    val esc = LocalEscala.current
    val c = LocalColeccion.current
    Box(Modifier.clip(RoundedCornerShape(50)).background(c.texto.copy(alpha = 0.1f)).padding(horizontal = esc.d(px * 0.87f), vertical = esc.d(px * 0.43f))) {
        TB(texto, px)
    }
}

/** Píldora de vidrio con contenido en fila (consola, hora y batería…). */
@Composable
fun PildoraVidrio(alto: Float, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, contenido: @Composable RowScope.() -> Unit) {
    val esc = LocalEscala.current
    Row(
        modifier.height(esc.d(alto)).vidrio(alto / 2).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = esc.d(alto * 0.4f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(esc.d(alto * 0.22f)),
        content = contenido,
    )
}

/** Botón redondo de vidrio con un ícono (buscar, widgets, apps, ajustes…). */
@Composable
fun BotonRedondo(icono: Icono, tam: Float, onClick: () -> Unit, modifier: Modifier = Modifier, descripcion: String? = null) {
    val esc = LocalEscala.current
    val c = LocalColeccion.current
    Box(
        modifier.size(esc.d(tam)).clip(CircleShape).background(c.texto.copy(alpha = 0.07f)).border(1.dp, c.borde, CircleShape)
            .clickable(onClick = onClick)
            .then(if (descripcion != null) Modifier.semantics { contentDescription = descripcion } else Modifier),
        contentAlignment = Alignment.Center,
    ) { IconoDiptico(icono, c.texto, tam * 0.42f) }
}

/** Hora · batería · %, como en la barra del boceto. */
@Composable
fun HoraBateria(alto: Float = 64f, letra: Float = 26f) {
    val esc = LocalEscala.current
    val c = LocalColeccion.current
    val hora = rememberHora()
    val bateria = rememberBateria()
    PildoraVidrio(alto) {
        TB(hora, letra, peso = FontWeight.ExtraBold, traducir = false)
        Box(Modifier.width(esc.d(2)).height(esc.d(letra)).background(c.texto.copy(alpha = 0.2f)))
        if (bateria in 0..100) {
            PilaDibujo(bateria, letra)
            TB("$bateria %", letra * 0.92f, traducir = false)
        }
    }
}

/** La pila del boceto: contorno, relleno según la carga y el botoncito. */
@Composable
fun PilaDibujo(carga: Int, alto: Float) {
    val esc = LocalEscala.current
    val c = LocalColeccion.current
    androidx.compose.foundation.Canvas(Modifier.size(esc.d(alto * 1.55f), esc.d(alto * 0.85f))) {
        val w = size.width; val h = size.height
        val k = w / 40f
        drawRoundRect(c.texto, androidx.compose.ui.geometry.Offset(1 * k, 1 * k), androidx.compose.ui.geometry.Size(34 * k, h - 2 * k),
            androidx.compose.ui.geometry.CornerRadius(5 * k), style = androidx.compose.ui.graphics.drawscope.Stroke(2 * k))
        val lleno = (28f * (carga.coerceIn(0, 100) / 100f)).coerceAtLeast(2f)
        drawRoundRect(if (carga <= 15) Color(0xFFFF6B6B) else c.texto, androidx.compose.ui.geometry.Offset(4 * k, 4 * k),
            androidx.compose.ui.geometry.Size(lleno * k, h - 8 * k), androidx.compose.ui.geometry.CornerRadius(3 * k))
        drawRoundRect(c.texto, androidx.compose.ui.geometry.Offset(36 * k, h * 0.32f), androidx.compose.ui.geometry.Size(3 * k, h * 0.36f),
            androidx.compose.ui.geometry.CornerRadius(1.5f * k))
    }
}

/** DIPPO en una píldora con su frase (sin tapar nada: va en su propio lugar). */
@Composable
fun DippoFrase(frase: String, modifier: Modifier = Modifier, cara: Float = 52f, letra: Float = 23f, maxAncho: Float = 560f) {
    val esc = LocalEscala.current
    val c = LocalColeccion.current
    Row(
        modifier.widthIn(max = esc.d(maxAncho)).clip(RoundedCornerShape(50)).background(c.vidrio).border(1.dp, c.borde, RoundedCornerShape(50))
            .clickable { cl.nikalbazu.diptico.estado.Diptico.abrirAsistente() }
            .padding(start = esc.d(12), end = esc.d(22), top = esc.d(12), bottom = esc.d(12)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(esc.d(14)),
    ) {
        cl.nikalbazu.diptico.bisa.AvatarBisa(cara * esc.factor)
        TB(frase, letra, color = Color(0xFFE4E5F4).takeIf { c.texto.luminance() > 0.5f } ?: c.texto, lineas = 2, interlineado = letra * 1.3f)
    }
}

/** Velo para leer sobre el arte: oscuro a la izquierda y abajo (como el boceto 1). */
fun Modifier.veloColeccion(fondo: Color): Modifier = this
    .background(Brush.horizontalGradient(0f to fondo.copy(alpha = .92f), .34f to fondo.copy(alpha = .72f), .62f to fondo.copy(alpha = .1f), 1f to Color.Transparent))
    .background(Brush.verticalGradient(0f to Color.Transparent, .66f to Color.Transparent, 1f to fondo.copy(alpha = .85f)))

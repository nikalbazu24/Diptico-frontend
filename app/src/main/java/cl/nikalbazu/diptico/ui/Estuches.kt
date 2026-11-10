package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Medio

/**
 * El estuche de cada consola: lo que hace que una caja se reconozca de lejos aunque no tenga logo. Son detalles
 * genéricos inspirados en el material de cada época (cartón con rejilla, estuche de CD con bisagra, DVD con franja,
 * tarjetas, UMD, marquesina de arcade, caja de PC), nunca logos ni marcas.
 */
enum class Estuche {
    CARTON_REJILLA, CARTON_FRANJA, CARTON_LOMO, CARTON_PORTATIL, CLAMSHELL_REJILLA, REJILLA_BLANCA,
    JEWEL, JEWEL_BLANCO, DVD, BLURAY, TARJETA, UMD, MARQUESINA, CAJA_PC, NINGUNO,
}

fun estucheDe(c: Consola): Estuche = when (c.id) {
    "nes", "famicom", "atari2600", "atari7800", "atarijaguar", "msx", "sega32x" -> Estuche.CARTON_REJILLA
    "snes", "sfc", "snesna" -> Estuche.CARTON_FRANJA
    "n64" -> Estuche.CARTON_LOMO
    "genesis", "megadrive", "megadrivejp" -> Estuche.CLAMSHELL_REJILLA
    "mastersystem" -> Estuche.REJILLA_BLANCA
    "dreamcast", "pcenginecd" -> Estuche.JEWEL_BLANCO
    "ps2", "xbox", "xbox360", "wii", "gc" -> Estuche.DVD
    "ps3", "ps4", "wiiu", "xboxone" -> Estuche.BLURAY
    "switch", "switch2", "nds", "n3ds", "psvita", "pcengine", "tg16" -> Estuche.TARJETA
    "psp" -> Estuche.UMD
    "fds" -> Estuche.CARTON_PORTATIL
    else -> when (c.medio) {
        Medio.CARTUCHO_CHICO -> Estuche.CARTON_PORTATIL
        Medio.CD -> Estuche.JEWEL
        Medio.ARCADE -> Estuche.MARQUESINA
        Medio.DISQUETE -> Estuche.CAJA_PC
        Medio.CARTUCHO_ANCHO, Medio.CARTUCHO_N64 -> Estuche.CARTON_REJILLA
        Medio.DVD, Medio.MINIDISCO -> Estuche.DVD
        Medio.BLURAY -> Estuche.BLURAY
        Medio.TARJETA -> Estuche.TARJETA
        Medio.UMD -> Estuche.UMD
        else -> Estuche.NINGUNO
    }
}

/**
 * Cuánto ocupa el estuche en cada borde, como fracción del tamaño final de la caja. El arte va completo en el
 * hueco que queda (la caja crece para hacerle espacio): así ninguna franja tapa la portada.
 */
data class MargenesEstuche(val arriba: Float = 0f, val izq: Float = 0f, val abajo: Float = 0f, val der: Float = 0f) {
    val ancho get() = 1f - izq - der
    val alto get() = 1f - arriba - abajo
}

fun margenesDe(t: Estuche): MargenesEstuche = when (t) {
    Estuche.CARTON_REJILLA -> MargenesEstuche(arriba = 0.15f, izq = 0.1f)
    Estuche.CARTON_FRANJA -> MargenesEstuche(abajo = 0.19f)
    Estuche.CARTON_LOMO -> MargenesEstuche(izq = 0.15f)
    Estuche.CARTON_PORTATIL -> MargenesEstuche(arriba = 0.15f)
    Estuche.CLAMSHELL_REJILLA -> MargenesEstuche(arriba = 0.19f)
    Estuche.REJILLA_BLANCA -> MargenesEstuche(arriba = 0.06f, izq = 0.07f, abajo = 0.12f, der = 0.07f)
    Estuche.JEWEL, Estuche.JEWEL_BLANCO -> MargenesEstuche(izq = 0.11f)
    Estuche.DVD -> MargenesEstuche(arriba = 0.145f)
    Estuche.BLURAY -> MargenesEstuche(arriba = 0.11f)
    Estuche.TARJETA -> MargenesEstuche(arriba = 0.11f)
    Estuche.UMD -> MargenesEstuche(arriba = 0.11f, izq = 0.03f, abajo = 0.03f, der = 0.05f)
    Estuche.MARQUESINA -> MargenesEstuche(arriba = 0.17f)
    Estuche.CAJA_PC -> MargenesEstuche(izq = 0.09f, abajo = 0.11f)
    Estuche.NINGUNO -> MargenesEstuche()
}

/** Color de la franja de cada consola (algunas usan el blanco o el negro de sus cajas en vez de su color). */
private fun franja(c: Consola): Color = when (c.id) {
    "nds", "wii", "dreamcast", "pcengine" -> Color(0xFFF2F3F5)
    "n3ds" -> Color(0xFFF2F3F5)
    "ps2", "ps3", "psp" -> Color(0xFF111116)
    "xbox360" -> Color(0xFFEDEFF2)
    else -> c.color
}

/** El estuche dibujado encima del arte de la caja. Se arma una vez por tamaño (no en cada cuadro). */
@Composable
fun CapaEstuche(c: Consola, modifier: Modifier = Modifier) {
    val tipo = estucheDe(c)
    if (tipo == Estuche.NINGUNO) return
    val medidor = rememberTextMeasurer()
    val banda = franja(c)
    val tinta = if (banda.luminance() > 0.55f) Color(0xFF1B1A22) else Color.White
    Box(modifier.fillMaxSize().drawWithCache {
        val u = size.minDimension / 100f
        val tam = (u * 6.2f / density / fontScale).coerceIn(5f, 30f)
        val nombre = medidor.measure(c.nombre.uppercase(), TextStyle(color = tinta, fontSize = tam.sp, fontWeight = FontWeight.Bold, letterSpacing = (tam * 0.12f).sp))
        onDrawBehind { dibujarEstuche(tipo, c.color, banda, tinta, nombre) }
    })
}

private fun DrawScope.dibujarEstuche(tipo: Estuche, color: Color, banda: Color, tinta: Color, nombre: TextLayoutResult) {
    val w = size.width
    val h = size.height
    val u = size.minDimension / 100f
    val oscuro = lerp(color, Color.Black, 0.55f)
    fun etiqueta(x: Float, y: Float) = drawText(nombre, topLeft = Offset(x, y))
    fun bandaArriba(alto: Float, c: Color = banda): Float {
        drawRect(c, Offset.Zero, Size(w, alto))
        drawRect(Color.White.copy(alpha = 0.10f), Offset.Zero, Size(w, alto * 0.35f))
        etiqueta(u * 5f, (alto - nombre.size.height) / 2f)
        return alto
    }
    fun rejilla(x: Float, y: Float, ancho: Float, alto: Float, paso: Float, c: Color) {
        var px = x - alto
        while (px < x + ancho) {
            drawLine(c, Offset(px, y + alto), Offset(px + alto, y), strokeWidth = u * 0.5f)
            px += paso
        }
    }
    when (tipo) {
        // Cartón con rejilla diagonal arriba y al costado (cajas negras de los 80).
        Estuche.CARTON_REJILLA -> {
            val a = h * 0.14f
            val l = w * 0.1f
            drawRect(Color(0xFF0E0E12), Offset.Zero, Size(w, a))
            drawRect(Color(0xFF0E0E12), Offset(0f, a), Size(l, h - a))
            rejilla(0f, 0f, w, a, u * 3f, Color.White.copy(alpha = 0.16f))
            rejilla(0f, a, l, h - a, u * 3f, Color.White.copy(alpha = 0.12f))
            drawRect(color, Offset(l, a - u * 1.4f), Size(w - l, u * 1.4f))
            drawRect(color, Offset(u * 2.5f, h - u * 2.5f - l * 0.8f), Size(l * 0.8f - u * 2.5f, l * 0.8f - u * 2.5f))
            drawText(nombre, color = Color.White, topLeft = Offset(l + u * 2f, (a - nombre.size.height) / 2f))
        }
        // Cartón con banda clara abajo y cuatro puntos de colores (los botones de su mando).
        Estuche.CARTON_FRANJA -> {
            val a = h * 0.17f
            drawRect(Color(0xFFD9D9E0), Offset(0f, h - a), Size(w, a))
            drawRect(color, Offset(0f, h - a - u * 1.6f), Size(w, u * 1.6f))
            val r = a * 0.13f
            listOf(0xFFE5484D, 0xFFF5C518, 0xFF2FB35A, 0xFF3B6FE0).forEachIndexed { i, c ->
                drawCircle(Color(c), r, Offset(w - u * 5f - r - i * r * 2.8f, h - a / 2f))
            }
            drawText(nombre, color = Color(0xFF2A2833), topLeft = Offset(u * 4f, h - a + (a - nombre.size.height) / 2f))
        }
        // Lomo negro a la izquierda con un cubito de cuatro colores.
        Estuche.CARTON_LOMO -> {
            val l = w * 0.15f
            drawRect(Color(0xFF0E0E12), Offset.Zero, Size(l, h))
            drawRect(Color.White.copy(alpha = 0.08f), Offset(l - u, 0f), Size(u, h))
            val c = l * 0.5f
            val x = (l - c) / 2f
            val y = u * 5f
            drawRect(Color(0xFFE5484D), Offset(x, y), Size(c / 2, c / 2))
            drawRect(Color(0xFF2FB35A), Offset(x + c / 2, y), Size(c / 2, c / 2))
            drawRect(Color(0xFFF5C518), Offset(x, y + c / 2), Size(c / 2, c / 2))
            drawRect(Color(0xFF3B6FE0), Offset(x + c / 2, y + c / 2), Size(c / 2, c / 2))
            drawRect(color, Offset(l - u * 1.6f, 0f), Size(u * 1.6f, h))
            rotate(-90f, Offset(l / 2f, h - u * 6f)) { drawText(nombre, color = Color.White, topLeft = Offset(l / 2f, h - u * 6f - nombre.size.height / 2f)) }
        }
        // Portátiles: franja de color arriba y una esquina del mismo color.
        Estuche.CARTON_PORTATIL -> {
            val a = bandaArriba(h * 0.14f, color)
            drawRect(Color.White.copy(alpha = 0.35f), Offset(0f, a - u * 0.8f), Size(w, u * 0.8f))
        }
        // Estuche de plástico negro con cuadritos arriba y una línea roja.
        Estuche.CLAMSHELL_REJILLA -> {
            val a = h * 0.18f
            drawRect(Color(0xFF111114), Offset.Zero, Size(w, a))
            val paso = u * 4.2f
            var y = u * 1.5f
            while (y < a - paso * 0.6f) {
                var x = u * 1.5f
                while (x < w - paso * 0.6f) { drawRect(Color.White.copy(alpha = 0.07f), Offset(x, y), Size(paso * 0.7f, paso * 0.7f)); x += paso }
                y += paso
            }
            drawRect(Color(0xFFE23B3B), Offset(0f, h * 0.19f - u * 1.2f), Size(w, u * 1.2f))
            drawText(nombre, color = Color.White, topLeft = Offset(u * 4f, (a - nombre.size.height) / 2f))
        }
        // Marco blanco con cuadrícula fina.
        Estuche.REJILLA_BLANCA -> {
            val blanco = Color(0xFFF4F4F6)
            val bl = w * 0.07f
            val ba = h * 0.06f
            val bb = h * 0.12f
            drawRect(blanco, Offset.Zero, Size(w, ba)); drawRect(blanco, Offset(0f, h - bb), Size(w, bb))
            drawRect(blanco, Offset.Zero, Size(bl, h)); drawRect(blanco, Offset(w - bl, 0f), Size(bl, h))
            // La cuadrícula fina va solo en el marco blanco.
            var y = u * 2f; while (y < h) { drawLine(Color.Black.copy(alpha = 0.07f), Offset(0f, y), Offset(bl, y), u * 0.3f); drawLine(Color.Black.copy(alpha = 0.07f), Offset(w - bl, y), Offset(w, y), u * 0.3f); y += u * 2.5f }
            drawText(nombre, color = Color(0xFF222228), topLeft = Offset(bl + u * 2f, h - bb + (bb - nombre.size.height) / 2f))
        }
        // Estuche de CD: bisagra con estrías a la izquierda y borde de plástico transparente.
        Estuche.JEWEL, Estuche.JEWEL_BLANCO -> {
            val blanco = tipo == Estuche.JEWEL_BLANCO
            val l = w * 0.11f
            drawRect(if (blanco) Color(0xFFF2F3F5) else Color(0xFF0D0D10), Offset.Zero, Size(l, h))
            for (k in 0..3) drawRect((if (blanco) Color.Black else Color.White).copy(alpha = 0.12f), Offset(l * 0.18f + k * l * 0.2f, 0f), Size(u * 0.5f, h))
            if (blanco) drawRect(color, Offset(l, 0f), Size(u * 1.2f, h))
            // El canto del plástico transparente, fino para no tapar la portada.
            drawRect(Color.White.copy(alpha = 0.22f), Offset(l, 0f), Size(w - l, h), style = Stroke(u * 0.9f))
            rotate(-90f, Offset(l / 2f, h - u * 5f)) {
                drawText(nombre, color = if (blanco) color else Color.White, topLeft = Offset(l / 2f, h - u * 5f - nombre.size.height / 2f))
            }
        }
        // DVD: franja arriba con el color de la consola y dos broches al costado.
        Estuche.DVD -> {
            val a = bandaArriba(h * 0.13f)
            drawRect(lerp(banda, color, 0.5f), Offset(0f, h * 0.145f - u * 1.2f), Size(w, u * 1.2f))
            drawRect(Color.White.copy(alpha = 0.16f), Offset.Zero, size, style = Stroke(u * 0.9f))
        }
        // Blu-ray: franja más fina y borde azulado.
        Estuche.BLURAY -> {
            val a = bandaArriba(h * 0.11f)
            drawRect(Color.White.copy(alpha = 0.25f), Offset(0f, a - u * 0.6f), Size(w, u * 0.6f))
            drawRect(Color(0xFF8EC5FF).copy(alpha = 0.3f), Offset.Zero, size, style = Stroke(u * 1.4f))
        }
        // Tarjeta: franja de color arriba (blanca en las portátiles de dos pantallas, con una línea de color).
        Estuche.TARJETA -> {
            val a = bandaArriba(h * 0.11f)
            if (banda != color) drawRect(color, Offset(0f, a - u * 1.4f), Size(w, u * 1.4f))
            drawRect(Color.White.copy(alpha = 0.18f), Offset.Zero, size, style = Stroke(u * 0.9f))
        }
        // UMD: marco oscuro translúcido y canto con muescas.
        Estuche.UMD -> {
            val a = bandaArriba(h * 0.11f, Color(0xFF15151B))
            drawRect(Color(0xFF15151B), Offset(0f, a), Size(w * 0.03f, h - a))
            drawRect(Color(0xFF15151B), Offset(0f, h - h * 0.03f), Size(w, h * 0.03f))
            drawRect(Color(0xFF15151B), Offset(w - w * 0.05f, a), Size(w * 0.05f, h - a))
            var y = a + u * 4f
            while (y < h - u * 4f) { drawRect(Color.White.copy(alpha = 0.18f), Offset(w - w * 0.038f, y), Size(w * 0.024f, u * 0.8f)); y += u * 3f }
        }
        // Arcade: marquesina con luz de neón y foquitos.
        Estuche.MARQUESINA -> {
            val a = h * 0.15f
            drawRect(Color(0xFF09090D), Offset.Zero, Size(w, a))
            drawRect(Brush.horizontalGradient(listOf(color, Color(0xFFFF4FA3), color)), Offset(0f, h * 0.17f - u * 1.6f), Size(w, u * 1.6f))
            val n = 7
            for (k in 0 until n) drawCircle(Color(0xFFFFE08A).copy(alpha = 0.9f), u * 0.9f, Offset(w * (k + 0.5f) / n, a * 0.18f))
            drawText(nombre, color = lerp(color, Color.White, 0.35f), topLeft = Offset(u * 4f, a * 0.32f + (a * 0.68f - nombre.size.height) / 2f))
        }
        // Caja de PC: lomo de color y franja negra abajo con un disquete chico.
        Estuche.CAJA_PC -> {
            val l = w * 0.09f
            drawRect(oscuro, Offset.Zero, Size(l, h))
            val a = h * 0.11f
            drawRect(Color(0xFF111114), Offset(0f, h - a), Size(w, a))
            val d = a * 0.55f
            drawRect(color, Offset(w - u * 4f - d, h - a + (a - d) / 2f), Size(d, d))
            drawRect(Color(0xFF111114), Offset(w - u * 4f - d * 0.75f, h - a + (a - d) / 2f), Size(d * 0.5f, d * 0.32f))
            drawText(nombre, color = Color.White, topLeft = Offset(l + u * 3f, h - a + (a - nombre.size.height) / 2f))
        }
        Estuche.NINGUNO -> Unit
    }
    // Brillo de plástico en diagonal, igual para todas.
    drawRect(Brush.linearGradient(
        0f to Color.White.copy(alpha = 0.11f), 0.35f to Color.White.copy(alpha = 0.03f), 0.36f to Color.Transparent, 1f to Color.Transparent,
        start = Offset.Zero, end = Offset(w, h),
    ))
}

package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.lerp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Perfil
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Los personajes de Díptico: ocho amigos originales dibujados con degradados, brillos y luz.
 * Cada uno se dibuja en una grilla de 64×64 unidades ([k] = tamaño de una unidad).
 */
private class Pincel(val s: DrawScope, val k: Float) {
    fun o(x: Float, y: Float) = Offset(x * k, y * k)

    /** Una esfera con la luz arriba a la izquierda. */
    fun esfera(cx: Float, cy: Float, r: Float, luz: Color, sombra: Color) =
        s.drawCircle(Brush.radialGradient(listOf(luz, sombra), center = o(cx - r * 0.35f, cy - r * 0.4f), radius = r * k * 1.55f), r * k, o(cx, cy))

    fun ovalo(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) =
        s.drawOval(color, o(cx - rx, cy - ry), Size(rx * 2 * k, ry * 2 * k))

    fun ovaloBrush(cx: Float, cy: Float, rx: Float, ry: Float, brush: Brush) =
        s.drawOval(brush, o(cx - rx, cy - ry), Size(rx * 2 * k, ry * 2 * k))

    /** Brillo de plástico: una mancha blanca suave que da volumen. */
    fun brillo(cx: Float, cy: Float, rx: Float, ry: Float, a: Float = 0.38f) = ovalo(cx, cy, rx, ry, Color.White.copy(alpha = a))

    fun suelo(cx: Float, cy: Float, rx: Float) = ovalo(cx, cy, rx, rx * 0.2f, Color.Black.copy(alpha = 0.22f))

    fun mejilla(cx: Float, cy: Float, r: Float, color: Color) =
        s.drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.75f), color.copy(alpha = 0f)), center = o(cx, cy), radius = r * k), r * k, o(cx, cy))

    /** Un ojo oscuro con dos reflejos blancos. */
    fun ojo(cx: Float, cy: Float, r: Float, color: Color = Color(0xFF1A1424)) {
        ovalo(cx, cy, r, r * 1.2f, color)
        s.drawCircle(Color.White, r * 0.38f * k, o(cx + r * 0.3f, cy - r * 0.45f))
        s.drawCircle(Color.White.copy(alpha = 0.7f), r * 0.17f * k, o(cx - r * 0.35f, cy + r * 0.5f))
    }

    fun poligono(brush: Brush, vararg p: Pair<Float, Float>) {
        val t = Path().apply { moveTo(p[0].first * k, p[0].second * k); for (i in 1 until p.size) lineTo(p[i].first * k, p[i].second * k); close() }
        s.drawPath(t, brush)
    }

    fun poligono(color: Color, vararg p: Pair<Float, Float>) = poligono(Brush.linearGradient(listOf(color, color), o(0f, 0f), o(1f, 1f)), *p)

    fun trazo(path: Path, color: Color, ancho: Float) = s.drawPath(path, color, style = Stroke(ancho * k, cap = StrokeCap.Round))

    fun linea(x0: Float, y0: Float, x1: Float, y1: Float, color: Color, ancho: Float) =
        s.drawLine(color, o(x0, y0), o(x1, y1), ancho * k, StrokeCap.Round)

    fun vertical(a: Color, b: Color, y0: Float, y1: Float) = Brush.verticalGradient(listOf(a, b), y0 * k, y1 * k)
}

private val TINTA = Color(0xFF1A1424)

fun DrawScope.dibujarPersonaje(id: String, lado: Float = size.minDimension) {
    val p = Pincel(this, lado / 64f)
    when (id) {
        "robot" -> robotin(p)
        "bicho" -> moquito(p)
        "dragon" -> dragoncito(p)
        "fantasma" -> fantasmita(p)
        "caballero" -> caballerito(p)
        "zorro" -> zorrito(p)
        "pixel" -> pixelito(p)
        else -> misu(p)
    }
}

/** Misu, el gato: blanco perlado con orejas rosas, rayitas en la frente y bigotes. */
private fun misu(p: Pincel) = with(p) {
    suelo(32f, 56f, 18f)
    val oreja = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD9D3EC)), o(14f, 8f), o(30f, 28f))
    val rosa = Brush.linearGradient(listOf(Color(0xFFFFB3CB), Color(0xFFF07BA3)), o(16f, 12f), o(26f, 26f))
    poligono(oreja, 12f to 31f, 15f to 7f, 32f to 18f)
    poligono(oreja, 52f to 31f, 49f to 7f, 32f to 18f)
    poligono(rosa, 17f to 25f, 18f to 13f, 27f to 19f)
    poligono(rosa, 47f to 25f, 46f to 13f, 37f to 19f)
    esfera(32f, 36f, 21f, Color(0xFFFFFFFF), Color(0xFFCFC8E6))
    // Rayitas de la frente.
    for (dx in listOf(-5f, 0f, 5f)) linea(32f + dx, 17.5f, 32f + dx * 0.9f, 22.5f - (if (dx == 0f) -1.5f else 0f), Color(0xFFB7A9E0), 1.7f)
    mejilla(20f, 42f, 5.5f, Color(0xFFFF8FB3)); mejilla(44f, 42f, 5.5f, Color(0xFFFF8FB3))
    ojo(24f, 35f, 3.2f); ojo(40f, 35f, 3.2f)
    poligono(Color(0xFFF07BA3), 29.8f to 41f, 34.2f to 41f, 32f to 43.6f)
    trazo(Path().apply { moveTo(32f * k, 43.6f * k); quadraticBezierTo(29f * k, 47.5f * k, 26.5f * k, 45.5f * k) }, TINTA, 1.4f)
    trazo(Path().apply { moveTo(32f * k, 43.6f * k); quadraticBezierTo(35f * k, 47.5f * k, 37.5f * k, 45.5f * k) }, TINTA, 1.4f)
    for (dy in listOf(0f, 3.4f)) {
        linea(11f, 41f + dy, 19.5f, 42f + dy * 0.6f, Color(0xFF8E80B8), 1.1f)
        linea(53f, 41f + dy, 44.5f, 42f + dy * 0.6f, Color(0xFF8E80B8), 1.1f)
    }
    brillo(24f, 22f, 7f, 3.4f, 0.55f)
}

/** Robotín: cabeza metálica con visor oscuro, ojos de luz y una antena que brilla. */
private fun robotin(p: Pincel) = with(p) {
    suelo(32f, 56f, 19f)
    linea(32f, 20f, 32f, 10f, Color(0xFF9AA6C0), 2f)
    s.drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD58A).copy(alpha = 0.6f), Color(0x00FFB347)), o(32f, 8f), 8f * k), 8f * k, o(32f, 8f))
    esfera(32f, 8f, 3.6f, Color(0xFFFFE9B0), Color(0xFFFF9A3C))
    // Tornillos laterales.
    esfera(7.5f, 37f, 4.2f, Color(0xFFDDE4F5), Color(0xFF6F7C9E)); esfera(56.5f, 37f, 4.2f, Color(0xFFDDE4F5), Color(0xFF6F7C9E))
    // Cabeza.
    s.drawRoundRect(vertical(Color(0xFFEFF3FF), Color(0xFF8A97BA), 19f, 56f), o(10f, 19f), Size(44f * k, 37f * k), CornerRadius(12f * k))
    s.drawRoundRect(Color.White.copy(alpha = 0.5f), o(14f, 21.5f), Size(36f * k, 3.2f * k), CornerRadius(1.6f * k))
    // Visor.
    s.drawRoundRect(vertical(Color(0xFF22305F), Color(0xFF0B1230), 25f, 44f), o(15f, 25f), Size(34f * k, 19f * k), CornerRadius(8f * k))
    for (cx in listOf(25.5f, 38.5f)) {
        s.drawCircle(Brush.radialGradient(listOf(Color(0xFF7BF3FF).copy(alpha = 0.55f), Color(0x0035D6FF)), o(cx, 34.5f), 8f * k), 8f * k, o(cx, 34.5f))
        ovaloBrush(cx, 34.5f, 3.4f, 4.4f, Brush.verticalGradient(listOf(Color(0xFFD9FDFF), Color(0xFF2FD0F0)), 30f * k, 39f * k))
    }
    brillo(24f, 28.4f, 6f, 1.7f, 0.22f)
    // Boquita y remaches.
    trazo(Path().apply { moveTo(27f * k, 49.5f * k); quadraticBezierTo(32f * k, 52.5f * k, 37f * k, 49.5f * k) }, Color(0xFF45507A), 1.6f)
    for (x in listOf(14f, 50f)) esfera(x, 51f, 1.5f, Color(0xFFFFFFFF), Color(0xFF7E8BAF))
}

/** Moquito: un bichito verde de dos antenas con luz, ojos grandes y mucha sonrisa. */
private fun moquito(p: Pincel) = with(p) {
    suelo(32f, 56f, 19f)
    for (sx in listOf(-1f, 1f)) {
        val base = 32f + sx * 9f
        trazo(Path().apply { moveTo(base * k, 19f * k); quadraticBezierTo((32f + sx * 14f) * k, 8f * k, (32f + sx * 15f) * k, 10f * k) }, Color(0xFF3D9A3B), 2f)
        esfera(32f + sx * 15f, 10f, 3.6f, Color(0xFFFFE08A), Color(0xFFFF9A3C))
    }
    esfera(32f, 37f, 22f, Color(0xFFD8FF8A), Color(0xFF39A845))
    // Manchitas.
    for ((x, y, r) in listOf(Triple(21f, 24f, 2.6f), Triple(43f, 23f, 2.1f), Triple(32f, 20f, 1.7f))) s.drawCircle(Color(0xFF2E8C3C).copy(alpha = 0.35f), r * k, o(x, y))
    // Ojos grandes.
    for (cx in listOf(24f, 40f)) {
        esfera(cx, 33f, 7.2f, Color.White, Color(0xFFDCE7F2))
        ojo(cx + 0.8f, 34f, 3.6f)
    }
    mejilla(16f, 44f, 5f, Color(0xFFFF8A8A)); mejilla(48f, 44f, 5f, Color(0xFFFF8A8A))
    val sonrisa = Path().apply { moveTo(23f * k, 44.5f * k); quadraticBezierTo(32f * k, 54f * k, 41f * k, 44.5f * k); quadraticBezierTo(32f * k, 47.5f * k, 23f * k, 44.5f * k) }
    s.drawPath(sonrisa, Color(0xFF3B1E2E))
    ovalo(32f, 49.2f, 3.2f, 1.6f, Color(0xFFFF7A8A))
    brillo(24f, 21f, 8f, 3.2f, 0.5f)
}

/** Dragoncito: verde azulado, con cuernitos, cresta de fuego y ojos dorados. */
private fun dragoncito(p: Pincel) = with(p) {
    suelo(32f, 57f, 18f)
    val cuerno = Brush.linearGradient(listOf(Color(0xFFFFF1C9), Color(0xFFE9B96B)), o(20f, 8f), o(24f, 24f))
    poligono(cuerno, 17f to 24f, 19f to 7f, 27f to 20f)
    poligono(cuerno, 47f to 24f, 45f to 7f, 37f to 20f)
    // Cresta.
    for ((x, h) in listOf(27f to 4.5f, 32f to 6.5f, 37f to 4.5f)) poligono(vertical(Color(0xFFFFB347), Color(0xFFFF5A3C), 14f, 22f), (x - 2.6f) to 18f, x to (17f - h * 0.6f), (x + 2.6f) to 18f)
    esfera(32f, 37f, 21f, Color(0xFF9CF0C4), Color(0xFF1F9C8F))
    // Hocico.
    ovaloBrush(32f, 46f, 11.5f, 8f, Brush.verticalGradient(listOf(Color(0xFFE8FFF1), Color(0xFF9DDDC4)), 38f * k, 54f * k))
    ovalo(28f, 45f, 1.1f, 1.6f, Color(0xFF1F6F69)); ovalo(36f, 45f, 1.1f, 1.6f, Color(0xFF1F6F69))
    trazo(Path().apply { moveTo(26f * k, 49.5f * k); quadraticBezierTo(32f * k, 53f * k, 38f * k, 49.5f * k) }, Color(0xFF1F6F69), 1.4f)
    poligono(Color.White, 29.5f to 50.6f, 31.2f to 50.9f, 30.2f to 53.2f)
    // Ojos dorados con pupila de rendija.
    for (cx in listOf(23.5f, 40.5f)) {
        esfera(cx, 33.5f, 5.2f, Color(0xFFFFF3B0), Color(0xFFF2B21C))
        ovalo(cx + 0.4f, 33.5f, 1.4f, 3.7f, TINTA)
        s.drawCircle(Color.White, 1.1f * k, o(cx + 1.6f, 31.2f))
    }
    mejilla(14f, 42f, 4.6f, Color(0xFFFF9C7A)); mejilla(50f, 42f, 4.6f, Color(0xFFFF9C7A))
    brillo(24f, 22f, 7f, 3f, 0.45f)
}

/** Fantasmita: un cuerpo de luz suave, con la orilla ondulada y cara de dulce. */
private fun fantasmita(p: Pincel) = with(p) {
    s.drawCircle(Brush.radialGradient(listOf(Color(0xFFB9A8FF).copy(alpha = 0.45f), Color(0x00B9A8FF)), o(32f, 34f), 30f * k), 30f * k, o(32f, 34f))
    suelo(32f, 58f, 16f)
    val cuerpo = Path().apply {
        moveTo(12f * k, 52f * k); lineTo(12f * k, 32f * k)
        cubicTo(12f * k, 10f * k, 52f * k, 10f * k, 52f * k, 32f * k)
        lineTo(52f * k, 52f * k)
        quadraticBezierTo(47f * k, 59f * k, 42f * k, 52f * k); quadraticBezierTo(37f * k, 45f * k, 32f * k, 52f * k)
        quadraticBezierTo(27f * k, 59f * k, 22f * k, 52f * k); quadraticBezierTo(17f * k, 45f * k, 12f * k, 52f * k)
        close()
    }
    s.drawPath(cuerpo, vertical(Color(0xFFFFFFFF), Color(0xFFC3BBF2), 12f, 58f))
    s.drawPath(cuerpo, Color(0xFFFFFFFF).copy(alpha = 0.7f), style = Stroke(1.2f * k))
    mejilla(20f, 40f, 5f, Color(0xFFFF9CC0)); mejilla(44f, 40f, 5f, Color(0xFFFF9CC0))
    ojo(25f, 32f, 3.4f, Color(0xFF2B2150)); ojo(39f, 32f, 3.4f, Color(0xFF2B2150))
    ovalo(32f, 41.5f, 2.4f, 2.9f, Color(0xFF2B2150))
    ovalo(32f, 42.6f, 1.5f, 1.2f, Color(0xFFFF8FB3))
    brillo(23f, 20f, 7f, 3.4f, 0.7f)
    for ((x, y) in listOf(10f to 22f, 55f to 18f, 50f to 8f)) {
        linea(x - 2f, y, x + 2f, y, Color.White.copy(alpha = 0.9f), 1.2f); linea(x, y - 2f, x, y + 2f, Color.White.copy(alpha = 0.9f), 1.2f)
    }
}

/** Caballerito: yelmo de acero con penacho rojo y un visor que deja ver dos lucecitas. */
private fun caballerito(p: Pincel) = with(p) {
    suelo(32f, 57f, 18f)
    val penacho = Path().apply {
        moveTo(31f * k, 17f * k); cubicTo(28f * k, 3f * k, 47f * k, 0f * k, 54f * k, 9f * k)
        cubicTo(46f * k, 8f * k, 41f * k, 13f * k, 37f * k, 18f * k); close()
    }
    s.drawPath(penacho, Brush.linearGradient(listOf(Color(0xFFFF7A6B), Color(0xFFC41E3A)), o(30f, 2f), o(50f, 18f)))
    val yelmo = Path().apply {
        moveTo(13f * k, 54f * k); lineTo(13f * k, 32f * k); cubicTo(13f * k, 11f * k, 51f * k, 11f * k, 51f * k, 32f * k)
        lineTo(51f * k, 54f * k); close()
    }
    s.drawPath(yelmo, Brush.horizontalGradient(listOf(Color(0xFF7E8BA8), Color(0xFFF3F6FF), Color(0xFFA9B4CC), Color(0xFF66738F)), 13f * k, 51f * k))
    // Cresta central.
    s.drawRoundRect(Color.White.copy(alpha = 0.45f), o(30.6f, 14f), Size(2.8f * k, 17f * k), CornerRadius(1.4f * k))
    // Visor.
    s.drawRoundRect(vertical(Color(0xFF30384F), Color(0xFF0D111F), 30f, 42f), o(17f, 30f), Size(30f * k, 11f * k), CornerRadius(5.5f * k))
    for (cx in listOf(25.5f, 38.5f)) {
        s.drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE08A).copy(alpha = 0.6f), Color(0x00FFD24C)), o(cx, 35.5f), 6f * k), 6f * k, o(cx, 35.5f))
        ovalo(cx, 35.5f, 2.2f, 2.8f, Color(0xFFFFF0B8))
    }
    // Rejilla del mentón y remaches.
    for (x in listOf(26f, 32f, 38f)) linea(x, 45f, x, 51f, Color(0xFF5B6784), 1.7f)
    for (x in listOf(16f, 48f)) esfera(x, 46f, 1.6f, Color.White, Color(0xFF6C7894))
    brillo(22f, 21f, 6f, 3.4f, 0.55f)
}

/** Zorrito: naranja con orejas grandes, mejillas blancas y una mirada pícara. */
private fun zorrito(p: Pincel) = with(p) {
    suelo(32f, 57f, 19f)
    val cara = Path().apply {
        moveTo(32f * k, 55f * k); lineTo(8f * k, 39f * k); lineTo(9f * k, 10f * k); lineTo(25f * k, 22f * k)
        quadraticBezierTo(32f * k, 20.5f * k, 39f * k, 22f * k); lineTo(55f * k, 10f * k); lineTo(56f * k, 39f * k); close()
    }
    s.drawPath(cara, Brush.radialGradient(listOf(Color(0xFFFFB166), Color(0xFFE5611A)), o(26f, 26f), 38f * k))
    // Orejas por dentro.
    poligono(Color(0xFF5A2A1C), 12f to 17f, 12.5f to 27f, 20f to 22f)
    poligono(Color(0xFF5A2A1C), 52f to 17f, 51.5f to 27f, 44f to 22f)
    // Mejillas blancas.
    val mejillas = Path().apply {
        moveTo(32f * k, 55f * k); lineTo(10f * k, 40.5f * k); quadraticBezierTo(22f * k, 37f * k, 32f * k, 44f * k)
        quadraticBezierTo(42f * k, 37f * k, 54f * k, 40.5f * k); close()
    }
    s.drawPath(mejillas, vertical(Color(0xFFFFFFFF), Color(0xFFF1E4D8), 38f, 55f))
    mejilla(17f, 37f, 4f, Color(0xFFFF8A6B)); mejilla(47f, 37f, 4f, Color(0xFFFF8A6B))
    ojo(23.5f, 33.5f, 3f); ojo(40.5f, 33.5f, 3f)
    ovalo(32f, 47.5f, 2.6f, 2f, TINTA)
    s.drawCircle(Color.White.copy(alpha = 0.8f), 0.7f * k, o(31.2f, 46.8f))
    trazo(Path().apply { moveTo(32f * k, 49.5f * k); quadraticBezierTo(29f * k, 52.5f * k, 26.5f * k, 50.5f * k) }, TINTA, 1.2f)
    trazo(Path().apply { moveTo(32f * k, 49.5f * k); quadraticBezierTo(35f * k, 52.5f * k, 37.5f * k, 50.5f * k) }, TINTA, 1.2f)
    brillo(25f, 26f, 6f, 2.6f, 0.35f)
}

/** Pixelito: un bichito hecho de cuadritos de luz, con antenas y piecitos. */
private fun pixelito(p: Pincel) = with(p) {
    val filas = listOf(
        "#......#",
        ".#....#.",
        ".######.",
        "##wk#wk#",
        "########",
        "#.#kk#.#",
        "#.#..#.#",
        "...##...",
    )
    val c = 5.4f
    val x0 = 32f - 4f * c
    val y0 = 33f - 4f * c + 2f
    s.drawCircle(Brush.radialGradient(listOf(Color(0xFF5EF0D0).copy(alpha = 0.42f), Color(0x005EF0D0)), o(32f, 34f), 30f * k), 30f * k, o(32f, 34f))
    suelo(32f, 57f, 16f)
    filas.forEachIndexed { f, fila ->
        fila.forEachIndexed { col, ch ->
            if (ch == '.') return@forEachIndexed
            val x = x0 + col * c
            val y = y0 + f * c
            val cuerpo = vertical(Color(0xFF8FFFE0), Color(0xFF1FA6A0), y, y + c)
            val fondo = when (ch) { 'w' -> Brush.verticalGradient(listOf(Color.White, Color(0xFFD6F4F0)), y * k, (y + c) * k); 'k' -> Brush.verticalGradient(listOf(Color(0xFF2B2150), TINTA), y * k, (y + c) * k); else -> cuerpo }
            s.drawRoundRect(fondo, o(x + 0.25f, y + 0.25f), Size((c - 0.5f) * k, (c - 0.5f) * k), CornerRadius(1.1f * k))
            if (ch == '#') s.drawRoundRect(Color.White.copy(alpha = 0.32f), o(x + 0.9f, y + 0.8f), Size((c - 1.8f) * k, 1.1f * k), CornerRadius(0.5f * k))
        }
    }
    // Reflejo de los ojos.
    s.drawCircle(Color.White, 0.8f * k, o(x0 + 3f * c + 1.6f, y0 + 3f * c + 1.6f))
    s.drawCircle(Color.White, 0.8f * k, o(x0 + 6f * c + 1.6f, y0 + 3f * c + 1.6f))
    // Antenas con luz.
    for (x in listOf(x0 + c / 2, x0 + 7.5f * c)) s.drawCircle(Color(0xFFFFE08A), 1.6f * k, o(x, y0 - 1.6f))
}

/**
 * Tu foto de perfil: tu imagen (o la de tu Discord) si elegiste una, o el personaje sobre un fondo del color de tu perfil.
 * Con [anillo] lleva el marco elegido (por defecto una estela de luz que gira sin parar).
 */
@Composable
fun FotoPerfil(
    perfil: Perfil, tam: Float, modifier: Modifier = Modifier, anillo: Boolean = true, personaje: String = perfil.personaje,
    marco: String = perfil.marco,
) {
    val esc = LocalEscala.current
    val color = Color(perfil.color)
    val avatar by remember { Diptico.estado.map { it.extra.discordAvatar }.distinctUntilChanged() }.collectAsState(Diptico.estado.value.extra.discordAvatar)
    val fuente = when {
        perfil.fotoDiscord && avatar.isNotBlank() -> avatar
        perfil.foto.isNotBlank() -> perfil.foto
        else -> ""
    }
    val img = if (fuente.isNotBlank()) rememberImagen(android.net.Uri.parse(fuente), (tam * 3).toInt().coerceIn(96, 512)) else null
    val g = if (anillo) esc.d((tam * 0.07f).coerceIn(2.5f, 8f)) else 0.dp
    val anima = anillo && LocalMovimiento.current
    val vuelta = if (anima) rememberInfiniteTransition(label = "marco").animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "vuelta") else null
    Box(modifier.size(esc.d(tam))) {
        Box(
            Modifier.fillMaxSize().padding(g * 1.35f).clip(CircleShape)
                .drawBehind {
                    drawRect(Brush.radialGradient(listOf(lerp(color, Color.White, 0.28f), color, lerp(color, Color.Black, 0.45f)), center = Offset(size.width * 0.3f, size.height * 0.25f), radius = size.maxDimension * 1.05f))
                },
        ) {
            if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Canvas(Modifier.fillMaxSize().padding(esc.d(tam * 0.1f))) { dibujarPersonaje(personaje, size.minDimension) }
        }
        if (anillo) Canvas(Modifier.fillMaxSize()) { dibujarMarco(marco, color, vuelta?.value ?: 0.12f, g.toPx()) }
    }
}

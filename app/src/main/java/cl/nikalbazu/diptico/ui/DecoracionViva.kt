package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Tema
import cl.nikalbazu.diptico.estado.Termica
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/**
 * Lo que las decoraciones vivas saben del mundo. [ultimoToque] lo actualizan las teclas y el dedo
 * (sin consumir nada), así la nieve o los pétalos saben si estás quieto.
 */
object Vivo {
    @Volatile var ultimoToque: Long = System.currentTimeMillis()
        private set

    fun toque() { ultimoToque = System.currentTimeMillis() }

    /** Solo para capturas y pruebas: segundos de quietud fingidos, la hora y la lectura del equipo. */
    @Volatile var pruebaQuieto: Float? = null
    @Volatile var pruebaHora: Float? = null
    @Volatile var pruebaRacha: Int? = null

    /** Fase de la luna (0 = nueva, 0,5 = llena) para una fecha, con el mes sinódico medio. */
    fun faseLuna(fecha: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)): Float {
        val referencia = LocalDateTime.of(2000, 1, 6, 18, 14).toEpochSecond(ZoneOffset.UTC)
        val dias = (fecha.toEpochSecond(ZoneOffset.UTC) - referencia) / 86400.0
        val f = (dias / 29.530588853) % 1.0
        return (if (f < 0) f + 1 else f).toFloat()
    }

    /** Hora del día como número (14,5 = 14:30). */
    fun horaAhora(): Float = pruebaHora ?: LocalDateTime.now().let { it.hour + it.minute / 60f }
}

private data class Mota(val x: Float, val y: Float, val v: Float, val t: Float, val fase: Float)

/** Memoria de dibujo: sirve para que la nieve o los pétalos acumulados «caigan» cuando vuelves a tocar. */
private class Sacudida {
    var visto = 0L
    var acumuladoAlTocar = 0f
    var momento = -10f
}

@Composable
internal fun DecoracionViva(tema: Tema, animada: Boolean) {
    var tiempo by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(tema.decor, animada) {
        if (!animada) return@LaunchedEffect
        val inicio = withFrameMillis { it }
        var ultimo = inicio - 33
        while (true) withFrameMillis {
            if (it - ultimo >= 32) {
                tiempo = (it - inicio) / 1000f
                ultimo = it
            }
        }
    }
    // Lo que se lee del equipo (batería, calor) cambia lento: una lectura cada 20 s fuera del hilo principal basta.
    val ctx = LocalContext.current
    var lectura by remember { mutableStateOf(Termica.pruebaLectura) }
    var racha by remember { mutableIntStateOf(Vivo.pruebaRacha ?: 0) }
    val necesitaEquipo = tema.decor in setOf("lava_viva", "marea_viva", "lcd_viva")
    LaunchedEffect(tema.decor) {
        while (true) {
            if (necesitaEquipo) lectura = withContext(Dispatchers.IO) { runCatching { Termica.leer(ctx) }.getOrNull() }
            if (tema.decor == "fogata") racha = Vivo.pruebaRacha ?: withContext(Dispatchers.IO) { runCatching { Diptico.rachaDias() }.getOrDefault(0) }
            delay(20_000)
        }
    }
    val motas = remember(tema.decor, tema.intensidadDecoracion) {
        val r = Random(tema.decor.hashCode())
        val base = when (tema.decor) {
            "lcd_viva", "cielo_vivo" -> 2
            "fogata" -> 22
            "bosque_vivo" -> 16
            "luna_viva" -> 26
            else -> 20
        }
        val n = when (tema.intensidadDecoracion) { 1 -> (base * 2 / 5).coerceAtLeast(2); 3 -> base * 3 / 2; else -> base }
        List(n) { Mota(r.nextFloat(), r.nextFloat(), 0.4f + r.nextFloat() * 0.8f, 0.6f + r.nextFloat() * 0.8f, r.nextFloat() * 6.28f) }
    }
    val sacudida = remember { Sacudida() }
    val colores = listOf(Color(tema.acento), Color(tema.acento2), Color(tema.acento3))
    Canvas(Modifier.fillMaxSize()) {
        val u = size.minDimension / 400f
        val ahora = System.currentTimeMillis()
        val quieto = Vivo.pruebaQuieto ?: ((ahora - Vivo.ultimoToque) / 1000f).coerceAtLeast(0f)
        // Si alguien tocó desde el último cuadro, lo acumulado «se cae» y empieza de nuevo.
        if (Vivo.ultimoToque != sacudida.visto) {
            if (sacudida.visto != 0L) {
                sacudida.acumuladoAlTocar = acumulado(sacudida.visto, Vivo.ultimoToque)
                sacudida.momento = tiempo
            }
            sacudida.visto = Vivo.ultimoToque
        }
        val trasToque = tiempo - sacudida.momento
        when (tema.decor) {
            "nieve_viva" -> nieveViva(motas, tiempo, u, quieto, sacudida.acumuladoAlTocar, trasToque)
            "lava_viva" -> lavaViva(motas, tiempo, u, lectura?.nivel ?: Termica.Nivel.FRESCA)
            "bosque_vivo" -> bosqueVivo(motas, tiempo, u, Vivo.horaAhora(), colores)
            "luna_viva" -> lunaViva(motas, tiempo, u)
            "marea_viva" -> mareaViva(motas, tiempo, u, (lectura?.nivelBateria ?: 80).coerceIn(0, 100) / 100f, lectura?.cargando == true)
            "sakura_viva" -> sakuraViva(motas, tiempo, u, quieto, sacudida.acumuladoAlTocar, trasToque)
            "lcd_viva" -> lcdViva(u, (lectura?.nivelBateria ?: 80).coerceIn(0, 100) / 100f)
            "cielo_vivo" -> cieloVivo(tiempo, u, Vivo.horaAhora())
            "fogata" -> fogata(motas, tiempo, u, racha)
        }
    }
}

/** Cuánto se juntó (0..1) entre dos toques: llega al máximo con 90 s de quietud. */
private fun acumulado(desde: Long, hasta: Long) = ((hasta - desde) / 90_000f).coerceIn(0f, 1f)

// ---------- Nieve: cae, se junta en los bordes y se sacude ----------

private fun DrawScope.nieveViva(motas: List<Mota>, t: Float, u: Float, quieto: Float, alTocar: Float, trasToque: Float) {
    for (p in motas) {
        val y = ((p.y + t * 0.05f * p.v) % 1.1f) - 0.05f
        val x = p.x + sin(t * 0.8f + p.fase) * 0.02f
        drawCircle(Color.White.copy(alpha = 0.6f), (2f + 3f * p.t) * u, Offset(x * size.width, y * size.height))
    }
    val junta = (quieto / 90f).coerceIn(0f, 1f)
    if (junta > 0.02f) {
        // Abajo: un montoncito ondulado que crece; arriba: escarcha que cuelga del borde.
        val alto = (6f + 22f * junta) * u
        val abajo = Path().apply {
            moveTo(0f, size.height)
            var x = 0f
            while (x <= size.width) {
                lineTo(x, size.height - alto * (0.75f + 0.25f * sin(x / (37f * u))))
                x += 8f * u
            }
            lineTo(size.width, size.height); close()
        }
        drawPath(abajo, Color.White.copy(alpha = 0.78f))
        val arriba = Path().apply {
            moveTo(0f, 0f)
            var x = 0f
            while (x <= size.width) {
                lineTo(x, alto * 0.35f * (0.6f + 0.4f * abs(sin(x / (23f * u)))))
                x += 10f * u
            }
            lineTo(size.width, 0f); close()
        }
        drawPath(arriba, Color.White.copy(alpha = 0.6f))
    }
    // La sacudida: lo que se había juntado cae en copos grandes durante 1,2 s.
    if (alTocar > 0.05f && trasToque in 0f..1.2f) {
        val k = trasToque / 1.2f
        for (i in 0 until 26) {
            val x = (i * 0.618f % 1f) * size.width
            val y = size.height * 0.04f + k * k * size.height * 0.5f + (i % 5) * 6f * u
            drawCircle(Color.White.copy(alpha = (1f - k) * 0.8f * alTocar), (3f + (i % 3) * 2f) * u, Offset(x, y))
        }
    }
    // Si estás quieto más de un minuto, el vidrio se empaña desde los bordes.
    val vaho = ((quieto - 60f) / 60f).coerceIn(0f, 1f)
    if (vaho > 0f) {
        drawRect(Brush.radialGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.32f * vaho)),
            center = center, radius = size.maxDimension * 0.62f,
        ))
    }
}

// ---------- Lava: sube y brilla con el calor real ----------

private fun DrawScope.lavaViva(motas: List<Mota>, t: Float, u: Float, nivel: Termica.Nivel) {
    val n = nivel.ordinal // 0 fresca … 3 muy caliente
    val alto = size.height * (0.10f + 0.07f * n)
    val pulso = if (n == 3) (sin(t * 3f) + 1f) / 2f else 0f
    val brillo = Color(0xFFFF5A1F).copy(alpha = 0.18f + 0.07f * n)
    val rojo = Color(0xFFFF1F1F).copy(alpha = 0.10f + 0.16f * pulso)
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, brillo), startY = size.height - alto * 1.8f, endY = size.height),
        topLeft = Offset(0f, size.height - alto * 1.8f), size = Size(size.width, alto * 1.8f))
    // Borde ondulado de la lava.
    val borde = Path().apply {
        moveTo(0f, size.height)
        var x = 0f
        while (x <= size.width) {
            lineTo(x, size.height - alto * (0.55f + 0.18f * sin(x / (40f * u) + t * 0.8f)))
            x += 8f * u
        }
        lineTo(size.width, size.height); close()
    }
    drawPath(borde, Color(0xFFFF6A1F).copy(alpha = 0.22f + 0.05f * n))
    if (pulso > 0f) drawRect(rojo)
    // Brasas que suben: más rápidas y más con el calor; con «muy caliente» se calman (para no gastar).
    val cuantas = if (n == 3) motas.size / 2 else motas.size * (2 + n) / 5
    for ((i, p) in motas.withIndex()) {
        if (i >= cuantas) break
        val y = 1.02f - ((p.y + t * (0.04f + 0.02f * n) * p.v) % 0.6f)
        val x = p.x + sin(t * 1.3f + p.fase) * 0.015f
        val a = ((1.02f - y) / 0.6f).coerceIn(0f, 1f)
        drawCircle(Color(0xFFFFB347).copy(alpha = (1f - a) * 0.75f), (1.6f + 1.6f * p.t) * u, Offset(x * size.width, y * size.height))
    }
}

// ---------- Bosque: según la hora real ----------

private fun DrawScope.bosqueVivo(motas: List<Mota>, t: Float, u: Float, hora: Float, colores: List<Color>) {
    val dia = hora in 6.5f..17.5f
    val tarde = hora in 17.5f..20.5f
    when {
        dia -> {
            // Rayos de luz que entran en diagonal.
            for (i in 0 until 3) {
                val x0 = size.width * (0.2f + i * 0.28f) + sin(t * 0.2f + i) * 10f * u
                val rayo = Path().apply {
                    moveTo(x0, 0f); lineTo(x0 + 40f * u, 0f)
                    lineTo(x0 + 160f * u, size.height); lineTo(x0 + 90f * u, size.height); close()
                }
                drawPath(rayo, Color(0xFFFFF4C8).copy(alpha = 0.07f + 0.03f * sin(t * 0.5f + i)))
            }
            hojasCayendo(motas, t, u, colores)
        }
        tarde -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFFF8A3D).copy(alpha = 0.20f), Color.Transparent), endY = size.height * 0.6f))
            hojasCayendo(motas.take(motas.size / 2), t, u, listOf(Color(0xFFFF9F43), Color(0xFFE8632B), Color(0xFFFFC857)))
        }
        else -> {
            drawRect(Color(0xFF0A1A3A).copy(alpha = 0.14f))
            for (p in motas) {
                // Luciérnagas: vagan despacio y se encienden y apagan.
                val x = p.x + sin(t * 0.3f * p.v + p.fase) * 0.06f
                val y = 0.25f + p.y * 0.7f + cos(t * 0.25f * p.v + p.fase) * 0.05f
                val luz = ((sin(t * 1.4f * p.v + p.fase) + 1f) / 2f).let { it * it }
                val c = Offset(x * size.width, y * size.height)
                drawCircle(Color(0xFFD9FF7A).copy(alpha = 0.18f * luz), 9f * u, c)
                drawCircle(Color(0xFFF2FFC0).copy(alpha = 0.85f * luz), 2.2f * u, c)
            }
        }
    }
}

private fun DrawScope.hojasCayendo(motas: List<Mota>, t: Float, u: Float, colores: List<Color>) {
    for ((i, p) in motas.withIndex()) {
        val y = ((p.y + t * 0.04f * p.v) % 1.15f) - 0.08f
        val x = p.x + sin(t * 0.6f + p.fase) * 0.04f
        translate(x * size.width, y * size.height) {
            rotate(t * 60f * p.v + p.fase * 57f, pivot = Offset.Zero) {
                drawOval(colores[i % colores.size].copy(alpha = 0.55f), Offset(-6f * u * p.t, -3f * u * p.t), Size(12f * u * p.t, 6f * u * p.t))
            }
        }
    }
}

// ---------- Medianoche: la luna de hoy ----------

private fun DrawScope.lunaViva(motas: List<Mota>, t: Float, u: Float) {
    for (p in motas) {
        val brillo = (sin(t * 2f * p.v + p.fase) + 1f) / 2f
        drawCircle(Color.White.copy(alpha = 0.12f + 0.45f * brillo), (1.2f + 1.6f * p.t) * u, Offset(p.x * size.width, p.y * size.height * 0.7f))
    }
    val r = 26f * u
    val c = Offset(size.width * 0.82f, size.height * 0.16f)
    val fase = Vivo.faseLuna()
    // La parte en sombra es otro círculo corrido: entra por la derecha al menguar y se va por la izquierda al crecer.
    val k = fase * 2f
    val dx = if (k <= 1f) -2f * r * k else 2f * r * (2f - k)
    drawCircle(Color(0xFFF1ECD4).copy(alpha = 0.10f), r, c)
    val sombra = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c + Offset(dx, 0f), r * 1.02f)) }
    clipPath(sombra, clipOp = ClipOp.Difference) {
        drawCircle(Color(0xFFFFF7DA).copy(alpha = 0.9f), r, c)
        drawCircle(Color(0xFFE9DFB8).copy(alpha = 0.5f), r * 0.18f, c + Offset(-r * 0.3f, -r * 0.2f))
        drawCircle(Color(0xFFE9DFB8).copy(alpha = 0.4f), r * 0.12f, c + Offset(r * 0.25f, r * 0.3f))
    }
    drawCircle(Color(0xFFFFF7DA).copy(alpha = 0.10f), r * 1.8f, c)
    // Estrella fugaz cada ~14 s.
    val ciclo = (t % 14f) / 1.1f
    if (ciclo < 1f) {
        val inicio = Offset(size.width * 0.25f, size.height * 0.08f)
        val fin = inicio + Offset(size.width * 0.35f * ciclo, size.height * 0.18f * ciclo)
        drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.8f * (1f - ciclo))), inicio, fin), inicio, fin, 1.6f * u)
    }
}

// ---------- Océano: la marea es la batería ----------

private fun DrawScope.mareaViva(motas: List<Mota>, t: Float, u: Float, bateria: Float, cargando: Boolean) {
    val nivel = size.height * (0.04f + 0.16f * bateria)
    if (bateria < 0.25f) {
        // Con poca batería se asoma la arena.
        drawRect(Color(0xFFE9D3A0).copy(alpha = 0.28f), Offset(0f, size.height - 10f * u), Size(size.width, 10f * u))
    }
    for (capa in 0..1) {
        val ola = Path().apply {
            moveTo(0f, size.height)
            var x = 0f
            while (x <= size.width) {
                val y = size.height - nivel - (if (capa == 0) 6f else 3f) * u * sin(x / ((52f + capa * 30f) * u) + t * (0.9f - capa * 0.35f))
                lineTo(x, y + capa * 5f * u)
                x += 8f * u
            }
            lineTo(size.width, size.height); close()
        }
        drawPath(ola, (if (capa == 0) Color(0xFF7FD3F0) else Color(0xFF2A8FC0)).copy(alpha = if (capa == 0) 0.16f else 0.20f))
    }
    drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, size.height - nivel), Offset(size.width, size.height - nivel), 1f * u)
    if (cargando) {
        for (p in motas) {
            val y = 1f - ((p.y + t * 0.05f * p.v) % 0.5f)
            val x = p.x + sin(t + p.fase) * 0.015f
            drawCircle(Color(0xFFC8F5FF).copy(alpha = 0.5f), (2f + 3f * p.t) * u, Offset(x * size.width, y * size.height),
                style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f * u))
        }
    }
}

// ---------- Sakura: pétalos que se juntan y el viento que los barre ----------

private fun DrawScope.sakuraViva(motas: List<Mota>, t: Float, u: Float, quieto: Float, alTocar: Float, trasToque: Float) {
    val viento = if (trasToque in 0f..1.6f) (1f - trasToque / 1.6f) else 0f
    val rosa = Color(0xFFFFC2D6)
    for (p in motas) {
        val y = ((p.y + t * 0.04f * p.v) % 1.15f) - 0.08f
        val x = p.x + sin(t * 0.6f + p.fase) * 0.04f + viento * 0.25f * p.v
        translate((x - floor(x)) * size.width, y * size.height) {
            rotate(t * 60f * p.v + p.fase * 57f + viento * 90f, pivot = Offset.Zero) {
                drawOval(rosa.copy(alpha = 0.6f), Offset(-6f * u * p.t, -3.5f * u * p.t), Size(12f * u * p.t, 7f * u * p.t))
            }
        }
    }
    // El montoncito de abajo: crece mientras no tocas; al tocar, el viento se lo lleva hacia la derecha.
    val junta = if (viento > 0f) alTocar else (quieto / 90f).coerceIn(0f, 1f)
    val cuantos = (60 * junta).toInt()
    val r = Random(7)
    for (i in 0 until cuantos) {
        val x0 = r.nextFloat() * size.width
        val y0 = size.height - r.nextFloat() * 14f * u * (0.4f + junta)
        val corre = if (viento > 0f) (1f - viento) * size.width * 0.6f else 0f
        val a = if (viento > 0f) viento * 0.7f else 0.7f
        drawOval((if (i % 3 == 0) Color(0xFFFFB0C9) else rosa).copy(alpha = a), Offset(x0 + corre - 5f * u, y0 - 3f * u), Size(10f * u, 6f * u))
    }
}

// ---------- Portátil: la pantalla verde se pone pálida con poca batería ----------

private fun DrawScope.lcdViva(u: Float, bateria: Float) {
    drawRect(Color(0xFFC4CFA1).copy(alpha = 0.05f + (1f - bateria) * 0.22f))
    // Rejilla de puntos de las pantallas de antes.
    val paso = 4f * u
    val linea = Color(0xFF0F380F).copy(alpha = 0.05f)
    var x = 0f
    while (x < size.width) { drawLine(linea, Offset(x, 0f), Offset(x, size.height), 0.6f * u); x += paso }
    var y = 0f
    while (y < size.height) { drawLine(linea, Offset(0f, y), Offset(size.width, y), 0.6f * u); y += paso }
}

// ---------- Atardecer: el cielo de la hora real ----------

private fun DrawScope.cieloVivo(t: Float, u: Float, hora: Float) {
    val (tinte, astro) = when {
        hora < 5.5f || hora >= 21f -> Color(0xFF1B2A6B) to Color(0xFFF1ECD4)
        hora < 8f -> Color(0xFFFF8FB5) to Color(0xFFFFD59A)
        hora < 17.5f -> Color(0xFF8ED8FF) to Color(0xFFFFF2B0)
        else -> Color(0xFFFF7A3D) to Color(0xFFFFB25C)
    }
    drawRect(Brush.verticalGradient(listOf(tinte.copy(alpha = 0.24f), Color.Transparent), endY = size.height * 0.45f))
    // El sol (de día) o la luna (de noche) avanzan por un arco según la hora.
    val deDia = hora in 6f..20f
    val avance = if (deDia) (hora - 6f) / 14f else ((hora + 24f - 20f) % 24f) / 10f
    val x = size.width * (0.1f + 0.8f * avance.coerceIn(0f, 1f))
    val y = size.height * (0.30f - 0.22f * sin(avance.coerceIn(0f, 1f) * PI.toFloat()))
    drawCircle(astro.copy(alpha = 0.15f + 0.04f * sin(t)), 34f * u, Offset(x, y))
    drawCircle(astro.copy(alpha = 0.65f), 14f * u, Offset(x, y))
}

// ---------- Fogata: crece con tu racha ----------

private fun DrawScope.fogata(motas: List<Mota>, t: Float, u: Float, racha: Int) {
    val fuerza = (racha.coerceIn(0, 14) / 14f)
    val base = Offset(size.width * 0.5f, size.height - 6f * u)
    // Leños.
    drawLine(Color(0xFF5A3420).copy(alpha = 0.8f), base + Offset(-26f * u, 2f * u), base + Offset(26f * u, -4f * u), 6f * u)
    drawLine(Color(0xFF4A2A18).copy(alpha = 0.8f), base + Offset(-24f * u, -4f * u), base + Offset(24f * u, 2f * u), 6f * u)
    if (racha <= 0) {
        // Sin racha quedan brasas que respiran.
        for (i in 0 until 6) {
            val c = base + Offset((i - 2.5f) * 7f * u, -3f * u)
            drawCircle(Color(0xFFFF6A1F).copy(alpha = 0.35f + 0.3f * (sin(t * 2f + i) + 1f) / 2f), 3f * u, c)
        }
        return
    }
    val alto = (30f + 70f * fuerza) * u
    drawCircle(Color(0xFFFF8A3D).copy(alpha = 0.10f + 0.08f * fuerza), alto * 1.3f, base + Offset(0f, -alto * 0.3f))
    val llamas = listOf(Color(0xFFFF5A1F) to 1f, Color(0xFFFFA83D) to 0.72f, Color(0xFFFFE08A) to 0.45f)
    for ((col, esc) in llamas) {
        val h = alto * esc * (0.92f + 0.08f * sin(t * 7f + esc * 10f))
        val w = h * 0.42f
        val vaiven = sin(t * 3.1f + esc * 4f) * w * 0.25f
        val llama = Path().apply {
            moveTo(base.x - w, base.y)
            quadraticTo(base.x - w * 1.05f, base.y - h * 0.55f, base.x + vaiven, base.y - h)
            quadraticTo(base.x + w * 1.05f, base.y - h * 0.55f, base.x + w, base.y)
            close()
        }
        drawPath(llama, col.copy(alpha = 0.78f))
    }
    // Chispas.
    for ((i, p) in motas.withIndex()) {
        if (i > 6 + (16 * fuerza).toInt()) break
        val k = (p.y + t * 0.35f * p.v) % 1f
        val c = base + Offset(sin(t * 2f + p.fase) * 18f * u * k + (p.x - 0.5f) * 30f * u, -alto * 0.6f - k * alto * 1.4f)
        drawCircle(Color(0xFFFFD36B).copy(alpha = (1f - k) * 0.9f), 1.5f * u, c)
    }
}

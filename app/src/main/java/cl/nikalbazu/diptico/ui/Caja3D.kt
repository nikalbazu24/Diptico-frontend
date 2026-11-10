package cl.nikalbazu.diptico.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.CarasGuardadas
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Idioma
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Grosor de la caja de cada consola, como fracción de su alto (medidas de las cajas reales). */
fun grosorDe(t: Estuche): Float = when (t) {
    Estuche.CARTON_REJILLA, Estuche.CARTON_FRANJA, Estuche.CARTON_LOMO -> 0.2f
    Estuche.CARTON_PORTATIL -> 0.22f
    Estuche.CLAMSHELL_REJILLA, Estuche.REJILLA_BLANCA -> 0.13f
    Estuche.JEWEL, Estuche.JEWEL_BLANCO -> 0.08f
    Estuche.DVD -> 0.075f
    Estuche.BLURAY -> 0.08f
    Estuche.TARJETA -> 0.075f
    Estuche.UMD -> 0.085f
    Estuche.MARQUESINA -> 0.15f
    Estuche.CAJA_PC -> 0.2f
    Estuche.NINGUNO -> 0.1f
}

/** Alto de la franja de la consola arriba del frente (fracción del alto total), como en las cajas reales. */
fun franjaDe(t: Estuche): Float = when (t) {
    Estuche.NINGUNO, Estuche.MARQUESINA, Estuche.CAJA_PC -> 0f
    Estuche.CARTON_PORTATIL, Estuche.TARJETA -> 0.1f
    else -> 0.085f
}

/** Lo que va en el frente: la portada (o una armada) con la franja de la consola y su logo arriba. */
data class InfoFrente(
    val titulo: String,
    val consola: String,
    val color: Int,
    val estuche: Estuche,
    val portada: Uri?,
    val logo: Uri?,
    val aspecto: Float,
    val ancho: Int,
    /** Alto de la franja (0 = sin franja, si la persona la apagó). */
    val franja: Float = franjaDe(estuche),
)

/** Cajas de plástico blanco (Dreamcast, Master System): el reverso y el lomo van claros. */
private fun estucheClaro(t: Estuche) = t == Estuche.JEWEL_BLANCO || t == Estuche.REJILLA_BLANCA

/** Cajas de cartón: los cantos son del mismo cartón impreso; las de plástico tienen cantos de plástico. */
private fun esCarton(t: Estuche) = t in setOf(
    Estuche.CARTON_REJILLA, Estuche.CARTON_FRANJA, Estuche.CARTON_LOMO, Estuche.CARTON_PORTATIL, Estuche.CAJA_PC, Estuche.MARQUESINA,
)

/** Lo que va impreso en el reverso y el lomo cuando no hay una foto real de la caja. */
data class InfoReverso(
    val titulo: String,
    val consola: String,
    val consolaCorta: String,
    val color: Int,
    val estuche: Estuche,
    val resumen: String,
    val datos: List<String>,
    val arte: Uri?,
    val aspecto: Float,
)

/**
 * Arma el reverso, el lomo (y el frente si falta la portada) de una caja como una imagen, la primera vez que se
 * necesitan, en segundo plano. Quedan guardadas en memoria: girar la caja después no cuesta nada.
 */
internal object FabricaCaras {
    private val cache = object : LruCache<String, Bitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private fun claveDe(tipo: String, i: InfoReverso) = "$tipo|${i.hashCode()}"
    fun enCache(tipo: String, i: InfoReverso): Bitmap? = cache.get(claveDe(tipo, i))

    fun hacer(ctx: Context, tipo: String, i: InfoReverso): Bitmap {
        val k = claveDe(tipo, i)
        cache.get(k)?.let { return it }
        val b = when (tipo) {
            "lomo" -> lomo(i)
            "frente" -> frente(i)
            else -> reverso(ctx, i)
        }
        cache.put(k, b)
        return b
    }

    fun enCacheFrente(f: InfoFrente): Bitmap? = cache.get("f|${f.hashCode()}")

    /** El frente completo: franja de la consola (con su logo o su nombre) y debajo la portada entera, sin recortes. */
    fun hacerFrente(ctx: Context, f: InfoFrente): Bitmap {
        val k = "f|${f.hashCode()}"
        cache.get(k)?.let { return it }
        val w = f.ancho
        val h = (w / f.aspecto).roundToInt().coerceIn(200, 1600)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val franja = (h * f.franja).roundToInt()
        val base = Color(f.color)
        val claro = estucheClaro(f.estuche)
        val fondoFranja = when {
            claro -> Color(0xFFF4F4F6)
            f.estuche == Estuche.DVD || f.estuche == Estuche.UMD || f.estuche == Estuche.JEWEL -> lerp(Color(0xFF0E0E12), base, 0.18f)
            esCarton(f.estuche) -> lerp(base, Color.Black, 0.5f)
            else -> base
        }
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        // La portada (o una armada) ocupa todo lo de abajo de la franja.
        val dst = RectF(0f, franja.toFloat(), w.toFloat(), h.toFloat())
        val arte = f.portada?.let { u -> try { Imagenes.cargar(ctx, u, w, memoriaGrafica = false)?.asAndroidBitmap() } catch (_: Exception) { null } }
        if (arte != null) c.drawBitmap(arte, null, dst, p)
        else {
            val generado = frente(InfoReverso(f.titulo, f.consola, f.consola, f.color, f.estuche, "", emptyList(), null, dst.width() / dst.height()))
            c.drawBitmap(generado, null, dst, p)
        }
        if (franja > 0) {
            p.color = fondoFranja.toArgb()
            c.drawRect(0f, 0f, w.toFloat(), franja.toFloat(), p)
            // Un filo de luz bajo la franja, como el borde del inserto de papel.
            p.color = 0x33FFFFFF
            c.drawRect(0f, franja - 2f, w.toFloat(), franja.toFloat(), p)
            val tinta = if (fondoFranja.luminanceSimple() > 0.6f) 0xFF15121D.toInt() else 0xFFFFFFFF.toInt()
            val pad = w * 0.05f
            val centrado = f.estuche == Estuche.TARJETA || f.estuche == Estuche.CARTON_PORTATIL
            val logo = f.logo?.let { u -> try { Imagenes.cargar(ctx, u, 512, memoriaGrafica = false)?.asAndroidBitmap() } catch (_: Exception) { null } }
            if (logo != null && logo.height > 0) {
                val altoLogo = franja * 0.66f
                val anchoLogo = min(altoLogo * logo.width / logo.height, w * 0.7f)
                val alto2 = anchoLogo * logo.height / logo.width
                val x = if (centrado) (w - anchoLogo) / 2f else pad
                c.drawBitmap(logo, null, RectF(x, (franja - alto2) / 2f, x + anchoLogo, (franja + alto2) / 2f), p)
            } else {
                // Sin logo propio: el nombre de la consola como marca, en letras anchas.
                val t = texto(franja * 0.44f, tinta, negrita = true).apply { letterSpacing = 0.08f; textAlign = if (centrado) Paint.Align.CENTER else Paint.Align.LEFT }
                val nombre = TextUtils.ellipsize(f.consola.uppercase(), t, w - 2 * pad, TextUtils.TruncateAt.END).toString()
                c.drawText(nombre, if (centrado) w / 2f else pad, franja * 0.66f, t)
            }
        }
        cache.put(k, bmp)
        return bmp
    }

    private fun texto(tam: Float, color: Int, negrita: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = tam
        this.color = color
        typeface = Typeface.create(Typeface.SANS_SERIF, if (negrita) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun bloque(t: String, p: TextPaint, ancho: Int, lineas: Int, centrado: Boolean = false): StaticLayout =
        StaticLayout.Builder.obtain(t, 0, t.length, p, ancho.coerceAtLeast(1))
            .setAlignment(if (centrado) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.08f).setMaxLines(lineas).setEllipsize(TextUtils.TruncateAt.END).build()

    private fun colores(i: InfoReverso): Triple<Int, Int, Int> {
        val base = Color(i.color)
        val claro = estucheClaro(i.estuche)
        val arriba = if (claro) lerp(Color.White, base, 0.06f) else lerp(base, Color.Black, 0.38f)
        val abajo = if (claro) lerp(Color.White, base, 0.16f) else lerp(base, Color.Black, 0.74f)
        val tinta = if (claro) 0xFF1A1A22.toInt() else 0xFFFFFFFF.toInt()
        return Triple(arriba.toArgb(), abajo.toArgb(), tinta)
    }

    private fun reverso(ctx: Context, i: InfoReverso): Bitmap {
        val w = 480
        val h = (w / i.aspecto).roundToInt().coerceIn(380, 920)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val (arriba, abajo, tinta) = colores(i)
        val tenue = (tinta and 0x00FFFFFF) or (0xB4 shl 24)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), arriba, abajo, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null
        val pad = w * 0.07f
        // Franja de arriba con el nombre de la consola, como en los reversos de verdad.
        val franja = h * 0.055f
        p.color = i.color
        c.drawRect(0f, 0f, w.toFloat(), franja, p)
        val tf = texto(franja * 0.5f, if (Color(i.color).luminanceSimple() > 0.62f) 0xFF15121D.toInt() else 0xFFFFFFFF.toInt(), negrita = true)
        tf.letterSpacing = 0.12f
        c.drawText(i.consola.uppercase(), pad, franja * 0.68f, tf)
        var y = franja + pad * 0.8f
        // El título.
        val tt = bloque(i.titulo, texto(w * 0.062f, tinta, negrita = true), (w - 2 * pad).toInt(), 2)
        c.save(); c.translate(pad, y); tt.draw(c); c.restore()
        y += tt.height + pad * 0.55f
        // Una imagen del juego (la captura o el arte), con esquinas redondeadas.
        val arte = i.arte?.let { u -> try { Imagenes.cargar(ctx, u, 640, memoriaGrafica = false)?.asAndroidBitmap() } catch (_: Exception) { null } }
        val altoArte = (h - y) * 0.36f
        if (arte != null && altoArte > 60f) {
            val dst = RectF(pad, y, w - pad, y + altoArte)
            val ra = dst.width() / dst.height()
            val rb = arte.width.toFloat() / arte.height
            val src = if (rb > ra) {
                val ancho = arte.height * ra
                android.graphics.Rect(((arte.width - ancho) / 2).toInt(), 0, ((arte.width + ancho) / 2).toInt(), arte.height)
            } else {
                val alto = arte.width / ra
                android.graphics.Rect(0, ((arte.height - alto) / 2).toInt(), arte.width, ((arte.height + alto) / 2).toInt())
            }
            c.save()
            val ruta = android.graphics.Path().apply { addRoundRect(dst, w * 0.02f, w * 0.02f, android.graphics.Path.Direction.CW) }
            c.clipPath(ruta)
            c.drawBitmap(arte, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            c.restore()
            p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = (tinta and 0x00FFFFFF) or (0x40 shl 24)
            c.drawRoundRect(dst, w * 0.02f, w * 0.02f, p)
            p.style = Paint.Style.FILL
            y += altoArte + pad * 0.6f
        }
        // Abajo la banda con la consola y un código de barras; lo que sobra entre medio es para la historia y tus datos.
        val banda = h * 0.11f
        val finTexto = h - banda - pad * 0.5f
        val datos = i.datos.joinToString("\n")
        val td = texto(w * 0.034f, tinta, negrita = true)
        val ld = if (datos.isNotBlank()) bloque(datos, td, (w - 2 * pad).toInt(), 3) else null
        val altoDatos = (ld?.height ?: 0) + if (ld != null) pad * 0.5f else 0f
        if (i.resumen.isNotBlank()) {
            val tr = texto(w * 0.034f, tenue)
            val linea = tr.fontSpacing * 1.08f
            val caben = ((finTexto - altoDatos - y) / linea).toInt()
            if (caben >= 2) {
                val lr = bloque(i.resumen, tr, (w - 2 * pad).toInt(), caben)
                c.save(); c.translate(pad, y); lr.draw(c); c.restore()
                y += lr.height + pad * 0.5f
            }
        }
        if (ld != null) {
            val yd = max(y, finTexto - ld.height)
            p.color = (tinta and 0x00FFFFFF) or (0x18 shl 24)
            c.drawRoundRect(RectF(pad * 0.6f, yd - pad * 0.3f, w - pad * 0.6f, yd + ld.height + pad * 0.3f), w * 0.02f, w * 0.02f, p)
            c.save(); c.translate(pad, yd); ld.draw(c); c.restore()
        }
        p.color = (0x30 shl 24)
        c.drawRect(0f, h - banda, w.toFloat(), h.toFloat(), p)
        val tb = texto(banda * 0.26f, tinta, negrita = true)
        c.drawText(i.consola, pad, h - banda * 0.55f, tb)
        c.drawText("Díptico", pad, h - banda * 0.22f, texto(banda * 0.2f, tenue))
        // El código de barras: rayas al azar pero siempre las mismas para el mismo juego.
        val r = kotlin.random.Random(i.titulo.hashCode())
        val bw = w * 0.26f
        var x = w - pad - bw
        p.color = 0xFFFFFFFF.toInt()
        c.drawRect(x - 6f, h - banda * 0.86f, w - pad + 6f, h - banda * 0.14f, p)
        p.color = 0xFF111111.toInt()
        while (x < w - pad) {
            val g = 1f + r.nextInt(3)
            c.drawRect(x, h - banda * 0.8f, min(x + g, w - pad), h - banda * 0.28f, p)
            x += g + 1f + r.nextInt(3)
        }
        return bmp
    }

    private fun lomo(i: InfoReverso): Bitmap {
        val alto = 720
        val ancho = (alto * grosorDe(i.estuche)).roundToInt().coerceAtLeast(40)
        val bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val base = Color(i.color)
        val claro = estucheClaro(i.estuche)
        val fondo = when {
            claro -> Color(0xFFF2F2F4)
            i.estuche == Estuche.JEWEL -> Color(0xFF121216)
            else -> lerp(base, Color.Black, 0.25f)
        }
        val tinta = if (claro) 0xFF1A1A22.toInt() else 0xFFFFFFFF.toInt()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, ancho.toFloat(), 0f,
            intArrayOf(lerp(fondo, Color.Black, 0.25f).toArgb(), lerp(fondo, Color.White, 0.08f).toArgb(), lerp(fondo, Color.Black, 0.3f).toArgb()),
            null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, ancho.toFloat(), alto.toFloat(), p)
        p.shader = null
        // Arriba el bloque de la consola, con su nombre corto girado.
        val bloque = alto * 0.17f
        p.color = base.toArgb()
        c.drawRect(0f, 0f, ancho.toFloat(), bloque, p)
        val sobreBase = if (base.luminanceSimple() > 0.62f) 0xFF15121D.toInt() else 0xFFFFFFFF.toInt()
        val tamCorto = min(ancho * 0.5f, bloque / max(3, i.consolaCorta.length) * 1.5f)
        val tc = texto(tamCorto, sobreBase, negrita = true)
        c.save(); c.translate(ancho / 2f, bloque * 0.08f); c.rotate(90f)
        c.drawText(TextUtils.ellipsize(i.consolaCorta.uppercase(), tc, bloque * 0.84f, TextUtils.TruncateAt.END).toString(), 0f, tamCorto * 0.36f, tc)
        c.restore()
        // El título a lo largo del lomo, de arriba hacia abajo (como los lomos de verdad).
        val tam = ancho * 0.46f
        val tt = texto(tam, tinta, negrita = true)
        val largo = alto * 0.72f
        val titulo = TextUtils.ellipsize(i.titulo, tt, largo, TextUtils.TruncateAt.END).toString()
        c.save(); c.translate(ancho / 2f, bloque + alto * 0.04f); c.rotate(90f)
        c.drawText(titulo, 0f, tam * 0.36f, tt)
        c.restore()
        // Abajo un rombo chico: la marca de Díptico.
        p.color = (tinta and 0x00FFFFFF) or (0x90 shl 24)
        val cy = alto - ancho * 0.7f
        val r = ancho * 0.18f
        c.drawPath(android.graphics.Path().apply { moveTo(ancho / 2f, cy - r); lineTo(ancho / 2f + r, cy); lineTo(ancho / 2f, cy + r); lineTo(ancho / 2f - r, cy); close() }, p)
        return bmp
    }

    private fun frente(i: InfoReverso): Bitmap {
        val w = 480
        val h = (w / i.aspecto).roundToInt().coerceIn(380, 920)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val base = Color(i.color)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), lerp(base, Color.White, 0.12f).toArgb(), lerp(base, Color.Black, 0.55f).toArgb(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null
        val tinta = if (base.luminanceSimple() > 0.62f) 0xFF15121D.toInt() else 0xFFFFFFFF.toInt()
        val lt = bloque(i.titulo, texto(w * 0.09f, tinta, negrita = true), (w * 0.84f).toInt(), 4, centrado = true)
        c.save(); c.translate(w * 0.08f, (h - lt.height) / 2f); lt.draw(c); c.restore()
        val tc = texto(w * 0.04f, (tinta and 0x00FFFFFF) or (0xB0 shl 24), negrita = true).apply { letterSpacing = 0.15f; textAlign = Paint.Align.CENTER }
        c.drawText(i.consola.uppercase(), w / 2f, h * 0.9f, tc)
        return bmp
    }
}

private fun Color.luminanceSimple(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

/** Lo que va impreso en el reverso de [j]: su historia (si ya se bajó la ficha), tu tiempo y cuánto dura. */
@Composable
fun infoReverso(e: Estado, j: Juego, c: Consola, aspecto: Float): InfoReverso {
    val ctx = LocalContext.current
    val inicial = if (e.fichaDe == j.clave) e.ficha?.resumen.orEmpty() else ""
    val resumen by produceState(initialValue = inicial, j.clave, inicial) {
        if (value.isBlank()) value = withContext(Dispatchers.IO) {
            runCatching { cl.nikalbazu.diptico.data.Fichas.guardada(ctx, j.titulo, c.bonito)?.resumen }.getOrNull().orEmpty()
        }
    }
    val seg = e.datos[j.clave]?.tiempo?.toLong() ?: 0L
    val d = e.extra.duraciones[j.clave]
    fun horas(x: Float) = if (x >= 10f) "${x.roundToInt()} h" else "%.1f h".format(x)
    val datos = listOfNotNull(
        if (seg > 0) Idioma.t("Tu tiempo") + ": " + formatoTiempo(seg) else Idioma.t("Todavía sin jugar"),
        d?.takeIf { it.principal > 0f }?.let { Idioma.t("Historia") + ": " + horas(it.principal) + (if (it.completo > 0f) " · " + Idioma.t("Completo") + ": " + horas(it.completo) else "") },
    )
    return InfoReverso(j.titulo, c.bonito, c.nombre, c.color.toArgb(), estucheDe(c), resumen.take(700), datos, j.arte, aspecto)
}

/** Proporción del frente de la caja: la de la portada real si está, si no la de la caja de su consola. */
@Composable
fun aspectoCaja(frente: ImageBitmap?, c: Consola): Float {
    val f = cajaDe(c)
    val a = if (frente != null && frente.height > 0) frente.width.toFloat() / frente.height else f.ancho / f.alto
    return a.coerceIn(0.55f, 1.5f)
}

/**
 * Una caja de juego en 3D de verdad: frente (la portada), lomo, reverso y cantos, dibujados con perspectiva real.
 * Se puede girar completa: [giroY] (grados, sin límite) la da vuelta y [giroX] la inclina. Todo se lee al dibujar,
 * así girarla no vuelve a armar la pantalla. [penumbra] (0..1) la oscurece (las vecinas de la vitrina).
 * Usa el reverso y el lomo reales si se encontraron ([caras]); si no, los arma con la ficha del juego.
 */
@Composable
fun Caja3D(
    e: Estado, juego: Juego, consola: Consola, modifier: Modifier = Modifier,
    giroY: () -> Float, giroX: () -> Float, penumbra: () -> Float = { 0f },
    caras: CarasGuardadas? = null, detalle: Boolean = true, sombraPiso: Boolean = true,
) {
    val ctx = LocalContext.current
    val frenteReal = rememberImagen(juego.portada, if (detalle) 720 else 420)
    val estuche = estucheDe(consola)
    val grosor = grosorDe(estuche)
    // La franja de la consola va arriba de la portada: la caja crece un poco para que la portada se vea entera.
    val franja = if (e.extra.bandaCaja) franjaDe(estuche) else 0f
    val aspecto = aspectoCaja(frenteReal, consola) * (1f - franja)
    val info = infoReverso(e, juego, consola, aspecto)
    val logo = e.consolasPropias[consola.id]?.logo?.let { Uri.parse(it) }
    val datosFrente = InfoFrente(juego.titulo, consola.bonito, consola.color.toArgb(), estuche, juego.portada, logo, aspecto, if (detalle) 640 else 360, franja)
    // Las caras reales (si aparecieron) o las armadas por Díptico.
    val reversoReal = if (detalle) rememberImagen(caras?.reverso?.let { Uri.parse(it) }, 900) else null
    val lomoReal = rememberImagen(caras?.lomo?.let { Uri.parse(it) }, 720)
    val reversoHecho by produceState(FabricaCaras.enCache("reverso", info), info, detalle) {
        if (value == null && detalle) value = withContext(Dispatchers.Default) { runCatching { FabricaCaras.hacer(ctx, "reverso", info) }.getOrNull() }
    }
    val lomoHecho by produceState(FabricaCaras.enCache("lomo", info), info) {
        if (value == null) value = withContext(Dispatchers.Default) { runCatching { FabricaCaras.hacer(ctx, "lomo", info) }.getOrNull() }
    }
    val frenteHecho by produceState(FabricaCaras.enCacheFrente(datosFrente), datosFrente) {
        if (value == null) value = withContext(Dispatchers.IO) { runCatching { FabricaCaras.hacerFrente(ctx, datosFrente) }.getOrNull() }
    }
    // Mientras se arma el frente grande, se usa el chico de la vecina (si ya está) para que nunca se vea en blanco.
    val frente = frenteHecho ?: FabricaCaras.enCacheFrente(datosFrente.copy(ancho = 360)) ?: frenteReal?.asAndroidBitmap()
    val reverso = reversoReal?.asAndroidBitmap() ?: reversoHecho
    val lomo = lomoReal?.asAndroidBitmap() ?: lomoHecho
    // El color de los cantos: cartón impreso (el de la consola) o plástico.
    val base = consola.color
    val canto = when {
        estucheClaro(estuche) -> Color(0xFFE8E8EC)
        esCarton(estuche) -> lerp(base, Color.Black, 0.2f)
        estuche == Estuche.JEWEL -> Color(0xFF26262C)
        else -> lerp(base, Color.Black, 0.35f)
    }.toArgb()

    // Todo lo que se reutiliza en cada cuadro (sin crear objetos nuevos mientras gira).
    val h = remember { Herramientas3D() }
    Canvas(modifier) {
        val ancho = size.width
        val alto = size.height
        // El tamaño se calcula para que la caja quepa entera aunque esté girada.
        val diag = sqrt(aspecto * aspecto + grosor * grosor)
        val H = min(alto * 0.8f, ancho * 0.86f / diag)
        val W = H * aspecto
        val D = H * grosor
        val cx = ancho / 2f
        val cy = alto * 0.46f
        val cam = H * 3.4f
        val a = Math.toRadians(giroY().toDouble())
        val b = Math.toRadians(giroX().toDouble())
        val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
        val cb = cos(b).toFloat(); val sb = sin(b).toFloat()
        // Las 8 esquinas, giradas y con perspectiva.
        for (i in 0 until 8) {
            val x = if (i and 1 == 0) -W / 2f else W / 2f
            val y = if (i and 2 == 0) -H / 2f else H / 2f
            val z = if (i and 4 == 0) -D / 2f else D / 2f
            val x1 = x * ca + z * sa
            val z1 = -x * sa + z * ca
            val y2 = y * cb - z1 * sb
            val z2 = y * sb + z1 * cb
            val f = cam / (cam - z2)
            h.px[i] = cx + x1 * f
            h.py[i] = cy + y2 * f
        }
        val oscuro = penumbra().coerceIn(0f, 1f)
        drawIntoCanvas { lienzo ->
            val nc = lienzo.nativeCanvas
            // La sombra en el piso.
            if (sombraPiso) {
                var minX = h.px[0]; var maxX = h.px[0]; var piso = h.py[0]
                for (i in 1 until 8) { minX = min(minX, h.px[i]); maxX = max(maxX, h.px[i]); piso = max(piso, h.py[i]) }
                val rx = (maxX - minX) * 0.62f
                h.plano.shader = RadialGradient(cx, piso, rx.coerceAtLeast(1f), intArrayOf(0x88000000.toInt(), 0x00000000), null, Shader.TileMode.CLAMP)
                nc.save(); nc.scale(1f, 0.16f, cx, piso)
                nc.drawCircle(cx, piso, rx, h.plano)
                nc.restore()
                h.plano.shader = null
            }
            // Las seis caras (de una caja convexa nunca se tapan entre sí: no hace falta ordenarlas).
            for (k in 0 until 6) {
                val q = CARAS[k]
                for (n in 0 until 4) { h.dst[n * 2] = h.px[q[n]]; h.dst[n * 2 + 1] = h.py[q[n]] }
                // Se ve si sus esquinas quedan en sentido horario en la pantalla.
                var area = 0f
                for (n in 0 until 4) {
                    val m = (n + 1) % 4
                    area += h.dst[n * 2] * h.dst[m * 2 + 1] - h.dst[m * 2] * h.dst[n * 2 + 1]
                }
                if (area <= 0f) continue
                // Luz desde arriba a la izquierda, por delante.
                val nx = NORMALES[k * 3]; val ny = NORMALES[k * 3 + 1]; val nz = NORMALES[k * 3 + 2]
                val nx1 = nx * ca + nz * sa
                val nz1 = -nx * sa + nz * ca
                val ny2 = ny * cb - nz1 * sb
                val nz2 = ny * sb + nz1 * cb
                val luz = (nx1 * -0.36f + ny2 * -0.46f + nz2 * 0.81f).coerceIn(0f, 1f)
                h.ruta.reset()
                h.ruta.moveTo(h.dst[0], h.dst[1]); h.ruta.lineTo(h.dst[2], h.dst[3]); h.ruta.lineTo(h.dst[4], h.dst[5]); h.ruta.lineTo(h.dst[6], h.dst[7]); h.ruta.close()
                val bmp = when (k) { 0 -> frente; 1 -> reverso; 2 -> lomo; else -> null }
                if (bmp != null) {
                    h.src[0] = 0f; h.src[1] = 0f; h.src[2] = bmp.width.toFloat(); h.src[3] = 0f
                    h.src[4] = bmp.width.toFloat(); h.src[5] = bmp.height.toFloat(); h.src[6] = 0f; h.src[7] = bmp.height.toFloat()
                    h.matriz.setPolyToPoly(h.src, 0, h.dst, 0, 4)
                    nc.drawBitmap(bmp, h.matriz, h.imagen)
                } else {
                    h.plano.color = if (k == 1 || k == 2) lerp(Color(canto), Color.Black, 0.3f).toArgb() else canto
                    nc.drawPath(h.ruta, h.plano)
                }
                // Sombra según la luz, más la penumbra de las cajas de los costados.
                val sombra = ((0.78f - luz).coerceAtLeast(0f) * 0.75f + oscuro * 0.6f).coerceIn(0f, 0.9f)
                if (sombra > 0.01f) {
                    h.plano.color = ((sombra * 255).roundToInt() shl 24)
                    nc.drawPath(h.ruta, h.plano)
                }
                // Brillo del plástico que cruza el frente y el reverso al girar.
                if ((k == 0 || k == 1) && detalle && oscuro < 0.5f) {
                    val minX = min(min(h.dst[0], h.dst[2]), min(h.dst[4], h.dst[6]))
                    val maxX = max(max(h.dst[0], h.dst[2]), max(h.dst[4], h.dst[6]))
                    val t = ((nx1 + 1f) / 2f)
                    val x0 = minX + (maxX - minX) * (1.1f - t * 1.2f)
                    val anchoBrillo = (maxX - minX) * 0.45f
                    h.plano.shader = LinearGradient(x0 - anchoBrillo, 0f, x0 + anchoBrillo, H * 0.3f,
                        intArrayOf(0x00FFFFFF, 0x2EFFFFFF, 0x00FFFFFF), null, Shader.TileMode.CLAMP)
                    nc.drawPath(h.ruta, h.plano)
                    h.plano.shader = null
                }
                // Un filo de luz en los bordes: la caja se ve nítida contra el fondo.
                h.borde.color = if (abs(luz) > 0.5f) 0x30FFFFFF else 0x18FFFFFF
                nc.drawPath(h.ruta, h.borde)
            }
        }
    }
}

/** Objetos que la caja reutiliza en cada cuadro. */
private class Herramientas3D {
    val px = FloatArray(8)
    val py = FloatArray(8)
    val src = FloatArray(8)
    val dst = FloatArray(8)
    val matriz = Matrix()
    val ruta = android.graphics.Path()
    val imagen = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val plano = Paint(Paint.ANTI_ALIAS_FLAG)
    val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f }
}

/**
 * Las caras de la caja, cada una con sus esquinas (arriba izq., arriba der., abajo der., abajo izq.) vistas desde
 * afuera. Las esquinas son 0..7: bit 1 = derecha, bit 2 = abajo, bit 4 = adelante.
 * Orden: frente, reverso, lomo (izquierda), canto derecho, tapa de arriba, tapa de abajo.
 */
private val CARAS = arrayOf(
    intArrayOf(4, 5, 7, 6),
    intArrayOf(1, 0, 2, 3),
    intArrayOf(0, 4, 6, 2),
    intArrayOf(5, 1, 3, 7),
    intArrayOf(0, 1, 5, 4),
    intArrayOf(6, 7, 3, 2),
)

private val NORMALES = floatArrayOf(
    0f, 0f, 1f,
    0f, 0f, -1f,
    -1f, 0f, 0f,
    1f, 0f, 0f,
    0f, -1f, 0f,
    0f, 1f, 0f,
)

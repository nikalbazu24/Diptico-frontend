package cl.nikalbazu.diptico.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.estado.*

/** Muestras locales pequeñas: sin nuevas descargas ni capturas de pantalla. */
object ColorUniverso {
    private val cache = LruCache<String, Long>(64)
    fun leer(ctx: Context, uri: Uri?): Long {
        if (uri == null || uri.scheme !in listOf("content", "file", "android.resource")) return 0L
        cache.get(uri.toString())?.let { return it }
        val color = try {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
            var muestra = 1
            while (maxOf(limites.outWidth, limites.outHeight) / muestra > 48) muestra *= 2
            val opciones = BitmapFactory.Options().apply { inSampleSize = muestra; inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888 }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
            var mejor = 0L; var puntuacion = 0f
            if (bmp != null) {
                val hsv = FloatArray(3)
                for (y in 0 until bmp.height step 2) for (x in 0 until bmp.width step 2) {
                    val px = bmp.getPixel(x, y)
                    if (android.graphics.Color.alpha(px) < 200) continue
                    android.graphics.Color.colorToHSV(px, hsv)
                    val p = hsv[1] * hsv[2]
                    if (p > puntuacion && hsv[2] > .25f) {
                        puntuacion = p
                        hsv[1] = hsv[1].coerceIn(.35f, .65f); hsv[2] = .85f
                        mejor = android.graphics.Color.HSVToColor(hsv).toLong() and 0xFFFFFFFFL
                    }
                }
                bmp.recycle()
            }
            mejor
        } catch (_: Exception) { 0L }
        cache.put(uri.toString(), color)
        return color
    }
}

@Composable
fun PresentacionColeccion(e: Estado, modifier: Modifier = Modifier) {
    val c = e.coleccionVisual?.let { e.experiencia.colecciones[it] } ?: return
    val fondo = rememberImagen(c.fondo.takeIf { it.isNotBlank() }?.let(Uri::parse), 1280)
    val logo = rememberImagen(c.logo.takeIf { it.isNotBlank() }?.let(Uri::parse), 512)
    Box(modifier.fillMaxSize().background(Color(c.color).copy(alpha = .13f))) {
        fondo?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .18f) }
        logo?.let { Image(it, "Logo de colección", Modifier.align(Alignment.TopEnd).padding(16.dp).size(70.dp), contentScale = ContentScale.Fit) }
    }
}

@Composable
fun ColeccionUniverso(e: Estado) {
    val id = e.coleccionVisual ?: return
    val c = e.colecciones[id] ?: return
    val estilo = e.experiencia.colecciones[id] ?: PresentacionColeccion()
    val esc = LocalEscala.current
    val juegos = remember(c.juegos, e.juegos) { c.juegos.mapNotNull { k -> e.juegos.firstOrNull { it.clave == k } } }
    Box(Modifier.fillMaxSize().background(LocalPaleta.current.fondo)) {
        PresentacionColeccion(e)
        Column(Modifier.fillMaxSize().padding(esc.d(24))) {
            Texto(c.nombre, 30f, modifier = Modifier.fillMaxWidth(.8f))
            Texto("${juegos.size} juegos · B volver", 12f, color = LocalPaleta.current.tenue)
            Spacer(Modifier.height(esc.d(14)))
            if (juegos.isEmpty()) Texto("Agrega juegos desde sus opciones (Y › Colecciones).", 16f)
            LazyVerticalGrid(columns = if (estilo.vista == "galeria") GridCells.Fixed(1) else GridCells.Adaptive(esc.d(130)), modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(12)), horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                items(juegos, key = { it.clave }) { j ->
                    Column(Modifier.clip(RoundedCornerShape(esc.d(16))).background(LocalPaleta.current.superficie.copy(alpha = .75f)).clickable { Diptico.jugarDesdeWidget(j.clave) }.padding(esc.d(8))) {
                        ArteJuego(j, preferirArte = estilo.vista == "galeria", maxLado = 640, modifier = Modifier.fillMaxWidth().height(esc.d(if (estilo.vista == "galeria") 140 else 100)))
                        Texto(j.titulo, 14f, lineas = 2)
                    }
                }
            }
        }
    }
}

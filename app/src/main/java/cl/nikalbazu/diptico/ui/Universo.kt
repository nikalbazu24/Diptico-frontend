package cl.nikalbazu.diptico.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.data.Consolas
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
fun Habitacion(e: Estado, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val fondo = when (e.experiencia.pared) {
        "bosque" -> Color(0xFF153D35); "atardecer" -> Color(0xFF503043)
        "papel" -> Color(0xFF414658); else -> Color(0xFF121D35)
    }
    val ids = remember(e.consolasConJuegos) { listOf("todo") + e.consolasConJuegos.map { it.id } }
    val lista = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    LaunchedEffect(e.consolaSel) { lista.animateScrollToItem(ids.indexOf(e.consolaSel).coerceAtLeast(0)) }
    Column(modifier.background(Brush.verticalGradient(listOf(fondo, Color(0xFF090F1D)))).padding(esc.d(16))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Texto("Tu habitación", 25f, color = Color.White)
                Texto("${e.juegos.size} juegos · ${e.experiencia.adorno}", 11f, color = Color(0xFFCED8EF))
            }
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .12f)).clickable { Diptico.abrirMenu(); Diptico.elegirSeccion(Seccion.EXPERIENCIA) }.padding(esc.d(10))) {
                Texto("Decorar", 12f, color = Color.White)
            }
        }
        Spacer(Modifier.height(esc.d(12)))
        LazyVerticalGrid(columns = GridCells.Adaptive(esc.d(125)), state = lista, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(16)), horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
            items(ids, key = { it }) { id ->
                val c = Consolas.porId(id)
                val elegida = e.consolaSel == id
                val escala by animateFloatAsState(if (elegida) 1f else .94f, tween(if (e.experiencia.movimiento) 180 else 0), label = "objeto")
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(14))).background(if (elegida) Uva.copy(alpha = .45f) else Color.White.copy(alpha = .08f)).border(if (elegida) 2.dp else 1.dp, if (elegida) Uva else Color.White.copy(alpha = .15f), RoundedCornerShape(esc.d(14))).clickable { Diptico.elegirConsola(id); Diptico.entrarSistema() }.padding(esc.d(12)), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconoDiptico(if (id == "todo") Icono.BIBLIOTECA else Icono.MANDO, Color.White, 30f * escala)
                    Texto(c?.nombre ?: "Todos tus juegos", 13f, color = Color.White, lineas = 2)
                    Box(Modifier.padding(top = esc.d(8)).height(esc.d(4)).fillMaxWidth().background(Color(0xFF85728B)))
                }
            }
            items(e.colecciones.entries.toList(), key = { "coleccion-${it.key}" }) { (id, c) ->
                val p = e.experiencia.colecciones[id] ?: PresentacionColeccion()
                Column(Modifier.clip(RoundedCornerShape(esc.d(14))).background(Color(p.color).copy(alpha = .25f)).clickable { Diptico.abrirColeccionVisual(id) }.padding(esc.d(12))) {
                    IconoDiptico(Icono.CARPETA, Color(p.color), 26f)
                    Texto(c.nombre, 14f, color = Color.White, lineas = 2)
                    Texto("${c.juegos.size} juegos", 11f, color = Color(0xFFCED8EF))
                }
            }
            // Los recuerdos usan juegos reales de la biblioteca; no crean entradas falsas.
            items(e.juegos.filter { (e.datos[it.clave]?.tiempo ?: 0L) > 0L }.sortedByDescending { e.datos[it.clave]?.tiempo }.take(3), key = { "recuerdo-${it.clave}" }) { j ->
                Column(Modifier.clickable { Diptico.elegirConsola("todo"); Diptico.elegirJuego(j.clave); Diptico.entrarSistema() }) {
                    ArteJuego(j, preferirArte = false, maxLado = 240, modifier = Modifier.fillMaxWidth().height(esc.d(70)).clip(RoundedCornerShape(esc.d(10))))
                    Texto(j.titulo, 11f, color = Color.White, lineas = 1)
                }
            }
        }
        if (e.experiencia.adorno != "ninguno") {
            val objeto = when (e.experiencia.adorno) { "plantas" -> "✿"; "discos" -> "◉"; else -> "✦" }
            Texto("$objeto     $objeto     $objeto", 16f, color = Color(0xFFB8CBE8), modifier = Modifier.align(Alignment.End))
        }
        Texto("✥ elegir consola · A entrar · B volver", 10f, color = Color(0xFFCED8EF))
    }
}

/** Una superficie explícita para el gesto: no intercepta el estante ni la cortina de Android. */
@Composable
fun EnviarFicha(e: Estado, modifier: Modifier = Modifier) {
    var recorrido by remember { mutableFloatStateOf(0f) }
    Box(modifier.clip(RoundedCornerShape(50)).background(LocalPaleta.current.superficie.copy(alpha = .94f))
        .pointerInput(e.fichaFijada, e.juegoSel) {
            detectVerticalDragGestures(onDragStart = { recorrido = 0f }, onVerticalDrag = { c, dy -> c.consume(); recorrido += dy }, onDragEnd = { if (recorrido < -32.dp.toPx()) Diptico.fijarFicha(); recorrido = 0f }, onDragCancel = { recorrido = 0f })
        }.clickable { Diptico.fijarFicha() }.padding(horizontal = 14.dp, vertical = 8.dp)) {
        Texto(if (e.fichaFijada == null) "↑ Enviar ficha" else "↓ Soltar ficha", 11f)
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

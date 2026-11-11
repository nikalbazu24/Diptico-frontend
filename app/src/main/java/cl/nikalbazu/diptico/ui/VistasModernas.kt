@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.*

@Composable
fun TarjetaCaratula(j: Juego, elegida: Boolean, ancho: Float, alto: Float, titulo: Boolean, elegir: () -> Unit) {
    val esc = LocalEscala.current; val p = LocalPaleta.current
    val c = Consolas.porId(j.consolaId) ?: return
    val caja = cajaDe(c); val k = minOf((ancho - 12) / caja.ancho, (alto - 8) / caja.alto)
    Column(Modifier.width(esc.d(ancho)).clip(RoundedCornerShape(esc.d(18)))
        .background(if (elegida) Uva.copy(alpha = .2f) else p.superficie.copy(alpha = .7f))
        .border(esc.d(if (elegida) 2 else 1), if (elegida) Uva else p.linea, RoundedCornerShape(esc.d(18)))
        .combinedClickable(onLongClick = { Diptico.elegirJuego(j.clave); Diptico.abrirRapido() }, onClick = elegir)
        .padding(esc.d(6)), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(esc.d(alto)), contentAlignment = Alignment.Center) { CajaJuego(j, c, escala = k, vivo = elegida) }
        if (titulo) {
            Spacer(Modifier.height(esc.d(5))); Texto(j.titulo, 12f, lineas = 2, modifier = Modifier.fillMaxWidth().padding(horizontal = esc.d(4)))
            Spacer(Modifier.height(esc.d(4)))
        }
    }
}

@Composable
fun FilaBiblioteca(e: Estado, j: Juego, elegida: Boolean) {
    val p = LocalPaleta.current; val esc = LocalEscala.current
    val c = Consolas.porId(j.consolaId); val d = e.datos[j.clave]
    Row(Modifier.fillMaxWidth().height(esc.d(68)).clip(RoundedCornerShape(esc.d(18)))
        .background(if (elegida) Uva.copy(alpha = .18f) else p.superficie.copy(alpha = .7f))
        .border(esc.d(if (elegida) 2 else 1), if (elegida) Uva else p.linea, RoundedCornerShape(esc.d(18)))
        .combinedClickable(onLongClick = { Diptico.elegirJuego(j.clave); Diptico.abrirRapido() }) { if (elegida) Diptico.jugar() else Diptico.elegirJuego(j.clave) }
        .padding(esc.d(8)), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(esc.d(54)).clip(RoundedCornerShape(esc.d(10)))) { ArteJuego(j, false, 180, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(esc.d(12)))
        Column(Modifier.weight(1f)) {
            Texto(j.titulo, 15f, lineas = 1)
            Detalle(listOfNotNull(c?.nombre, d?.tiempo?.takeIf { it > 0 }?.let { formatoTiempo(it) }).joinToString(" · "), 11.5f, lineas = 1)
        }
        if (d?.favorito == true) IconoDiptico(Icono.CORAZON, Coral, 16f)
        if (elegida) Box(Modifier.padding(start = esc.d(6)).size(esc.d(30)).clip(RoundedCornerShape(50)).fondoBoton(Uva).clickable { Diptico.jugar() }, contentAlignment = Alignment.Center) { IconoDiptico(Icono.JUGAR, SobreUva, 15f) }
    }
}

@Composable
fun CarpetaBiblioteca(e: Estado, id: String, elegida: Boolean, interactiva: Boolean, lado: Dp, tomada: Boolean = false) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val consola = Consolas.porId(id)
    val color = consola?.color ?: Uva
    val forma = androidx.compose.foundation.shape.RoundedCornerShape(esc.d(19))
    val ordenando = e.extra.ordenandoCarpetas && interactiva
    val foco by androidx.compose.animation.core.animateFloatAsState(if (elegida && interactiva) 1f else 0f,
        androidx.compose.animation.core.tween(duracionMovimiento(180)), label = "foco carpeta")
    val alzada by androidx.compose.animation.core.animateFloatAsState(if (tomada) 1f else 0f,
        androidx.compose.animation.core.spring(dampingRatio = .6f, stiffness = 500f), label = "carpeta tomada")
    // Modo edición: todas tiemblan un poquito (cada una a su ritmo); la tomada flota quieta y más grande.
    val tiembla = ordenando && !tomada && LocalMovimiento.current
    val giro = if (tiembla) {
        val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "tiembla")
        t.animateFloat(-1.4f, 1.4f, androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(150 + (id.hashCode() and 63), easing = androidx.compose.animation.core.LinearEasing),
            androidx.compose.animation.core.RepeatMode.Reverse), label = "giro").value
    } else 0f
    Column(Modifier.width(lado), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(lado).graphicsLayer {
                val s = (1f - .035f * (1f - foco)) * (1f + .1f * alzada)
                scaleX = s; scaleY = s; rotationZ = giro - 4f * alzada
            }
            // La elegida lleva un halo del color de su consola y borde blanco, como los mosaicos.
            .then(if ((elegida || tomada) && interactiva) Modifier.shadow(esc.d(16 + 14 * alzada), forma, clip = false, ambientColor = color, spotColor = color) else Modifier.shadow(esc.d(3), forma, clip = false))
            .clip(forma)
            .combinedClickable(enabled = interactiva, onLongClick = {
                if (!ordenando && id != "todo") { Diptico.elegirConsola(id); Diptico.empezarOrdenCarpetas() }
            }) {
                if (ordenando) Diptico.elegirConsola(id)
                else {
                    if (!elegida) Diptico.elegirConsola(id)
                    Diptico.entrarSistema()
                }
            }, contentAlignment = Alignment.Center) {
            when (e.extra.estiloCarpetas) {
                "consola" -> Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(
                    androidx.compose.ui.graphics.lerp(color, Color.White, .12f), androidx.compose.ui.graphics.lerp(color, Color.Black, .45f)))),
                    contentAlignment = Alignment.Center) { IconoConsola(consola, (lado.value / esc.factor) * .62f, Color.Transparent, memoriaGrafica = false) }
                "abanico" -> AbanicoCarpeta(e, id, color)
                "logo" -> Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.White.copy(alpha = .12f), Color.White.copy(alpha = .03f))))
                    .background(p.superficie.copy(alpha = .6f)), contentAlignment = Alignment.Center) {
                    Texto(consola?.nombre ?: "Todo", (lado.value / esc.factor) * .2f, color = p.tinta, peso = androidx.compose.ui.text.font.FontWeight.Black,
                        lineas = 2, alinear = androidx.compose.ui.text.style.TextAlign.Center, traducir = consola == null, modifier = Modifier.padding(esc.d(6)))
                }
                else -> PortadaCarpeta(e, id, Modifier.fillMaxSize(), maxLado = 256, memoriaGrafica = false)
            }
            Box(Modifier.fillMaxSize().border(esc.d(.8f + 2.2f * maxOf(foco, alzada)),
                if (elegida || tomada) Color.White else Color.White.copy(alpha = .3f), forma))
        }
        Spacer(Modifier.height(esc.d(6)))
        Texto(consola?.nombre ?: "Todo", 11f, color = if (elegida) p.tinta else p.tenue,
            lineas = 1, alinear = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/** Estilo «Abanico de portadas»: los tres juegos más jugados de la consola asomando sobre su color. */
@Composable
private fun AbanicoCarpeta(e: Estado, id: String, color: Color) {
    val top = remember(e.juegosPorConsola, e.datos, id) {
        e.juegosPorConsola[id].orEmpty().filter { e.datos[it.clave]?.oculto != true }
            .sortedByDescending { e.datos[it.clave]?.tiempo ?: 0L }.take(3)
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(
        androidx.compose.ui.graphics.lerp(color, Color.Black, .25f), androidx.compose.ui.graphics.lerp(color, Color.Black, .65f))))) {
        if (top.isEmpty()) { ArteCarpeta(Consolas.porId(id), Modifier.fillMaxSize()); return@BoxWithConstraints }
        val w = maxWidth * .46f
        val giros = listOf(-12f, 0f, 12f)
        val xs = listOf(.08f, .27f, .46f)
        top.reversed().forEachIndexed { k, j ->
            val i = top.size - 1 - k
            Box(Modifier.offset(x = maxWidth * xs[i], y = maxHeight * (.12f + .04f * kotlin.math.abs(i - 1)))
                .size(w, w * 1.36f).graphicsLayer { rotationZ = giros[i] }
                .shadow(LocalEscala.current.d(5), androidx.compose.foundation.shape.RoundedCornerShape(LocalEscala.current.d(7)))
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(LocalEscala.current.d(7)))) {
                ArteJuego(j, preferirArte = false, maxLado = 180, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
internal fun PortadaCarpeta(
    e: Estado, id: String, modifier: Modifier, maxLado: Int = 512,
    memoriaGrafica: Boolean = true,
) {
    val ruta = e.consolasPropias[id]?.portadaCarpeta
    if (ruta == null) {
        ArteCarpeta(Consolas.porId(id), modifier)
        return
    }
    val portada = key(ruta) { rememberImagen(android.net.Uri.parse(ruta), maxLado, memoriaGrafica) }
    if (portada == null) ArteCarpeta(Consolas.porId(id), modifier)
    else Image(portada, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
}

@Composable
fun ControlesBiblioteca(e: Estado) {
    val esc = LocalEscala.current; val p = LocalPaleta.current
    Row(Modifier.fillMaxWidth().padding(horizontal = esc.d(14), vertical = esc.d(3)),
        horizontalArrangement = Arrangement.spacedBy(esc.d(10)), verticalAlignment = Alignment.CenterVertically) {
        Texto(juegosTexto(e.visibles.size), 10.5f, color = p.tenue, lineas = 1)
        Spacer(Modifier.weight(1f))
        Texto(Diptico.nombreVista(e.vista), 10.5f, color = p.acentoTexto, lineas = 1,
            modifier = Modifier.clickable { Diptico.abrirGaleria("vistas_juegos") }.padding(esc.d(6)))
        Texto("•••", 16f, lineas = 1, modifier = Modifier.clickable(onClickLabel = "Opciones de biblioteca") { Diptico.abrirGaleria("biblioteca") }.padding(esc.d(6)))
    }
}

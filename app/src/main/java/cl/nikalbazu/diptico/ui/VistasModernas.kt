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
fun CarpetaBiblioteca(e: Estado, id: String, elegida: Boolean, interactiva: Boolean, lado: Dp) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val consola = Consolas.porId(id)
    val color = consola?.color ?: Uva
    val forma = androidx.compose.foundation.shape.RoundedCornerShape(esc.d(19))
    val foco by androidx.compose.animation.core.animateFloatAsState(if (elegida && interactiva) 1f else 0f,
        androidx.compose.animation.core.tween(duracionMovimiento(180)), label = "foco carpeta")
    Column(Modifier.width(lado), horizontalAlignment = Alignment.CenterHorizontally) {
        // La elegida crece un poco, con borde blanco y un halo del color de la consola; las demás se apagan apenas.
        Box(Modifier.size(lado).graphicsLayer { scaleX = 1f + .06f * foco - .035f * (1f - foco); scaleY = scaleX }
            .shadow(esc.d(3 + 14 * foco), forma, clip = false, ambientColor = color, spotColor = color).clip(forma)
            .combinedClickable(enabled = interactiva, onLongClick = { Diptico.abrirEditarConsola(id) }) {
                if (!elegida) Diptico.elegirConsola(id)
                Diptico.entrarSistema()
            }, contentAlignment = Alignment.Center) {
            PortadaCarpeta(e, id, Modifier.fillMaxSize(), maxLado = 256, memoriaGrafica = false)
            if (!elegida && interactiva) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .22f)))
            Box(Modifier.fillMaxSize().border(esc.d(.8f + 2.2f * foco),
                if (elegida) Color.White else Color.White.copy(alpha = .3f), forma))
        }
        Spacer(Modifier.height(esc.d(6)))
        Texto(consola?.nombre ?: "Todo", 11f, color = if (elegida) p.tinta else p.tenue,
            lineas = 1, alinear = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
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

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono

/** La identidad por consola acompaña al tema. Las celebraciones mantienen sus propios colores. */
internal fun colorColeccion(id: String): Color {
    Diptico.estado.value.consolasPropias[id]?.color?.takeIf { it != 0L }?.let { return Color(it) }
    if (Estilo.tema.id !in setOf("diptico", "diptico_vidrio")) return Uva
    return when (id) {
        "snes", "nes" -> Color(0xFFAA85FF)
        "gba", "gb", "gbc" -> Color(0xFFFFC45C)
        "gc", "wii", "wiiu" -> Color(0xFF778DFF)
        "nds", "3ds" -> Color(0xFFFF85BC)
        "psx", "ps2", "ps3" -> Color(0xFF58DDCF)
        "psp", "psvita" -> Color(0xFF67B9FF)
        "switch", "switch2" -> Color(0xFFFF856E)
        "megadrive", "mastersystem", "dreamcast", "saturn" -> Color(0xFF63CEFF)
        else -> Consolas.porId(id)?.color ?: Uva
    }
}

@Composable
internal fun CabeceraJuegosLimpia(e: Estado) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val c = Consolas.porId(e.consolaSel)
    Box(Modifier.fillMaxWidth().padding(horizontal = esc.d(13), vertical = esc.d(10))) {
        Column(Modifier.fillMaxWidth().padding(horizontal = esc.d(46)).testTag("titulo-carpeta"),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(1))) {
            Texto(c?.nombre ?: "Todos tus juegos", 19f, familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8), Alignment.CenterHorizontally)) {
                Texto(juegosTexto(e.visibles.size), 10.5f, color = p.tenue, familia = Nunito, lineas = 1)
                if (e.vistaConsolas == "carpetas") Box(Modifier.clip(RoundedCornerShape(esc.d(11)))
                    .clickable { Diptico.alternarModoCarpeta() }.padding(horizontal = esc.d(7), vertical = esc.d(5))) {
                    Texto(if (e.modoCarpeta != "completa") "Ampliar" else "En ventana", 10.5f, familia = Nunito, color = p.acentoTexto, lineas = 1)
                }
            }
        }
        Box(Modifier.align(Alignment.CenterStart).size(esc.d(38)).clip(RoundedCornerShape(esc.d(13)))
            .background(p.superficie.copy(alpha = if (Estilo.glass) .32f else .68f)).semantics { contentDescription = "Volver a consolas" }
            .clickable { Diptico.volverASistemas() }, contentAlignment = Alignment.Center) {
            Texto("‹", 28f, familia = Nunito, traducir = false)
        }
        Box(Modifier.align(Alignment.CenterEnd).size(esc.d(38)).clip(RoundedCornerShape(esc.d(13)))
            .background(p.superficie.copy(alpha = if (Estilo.glass) .32f else .68f)).semantics { contentDescription = "Opciones de juegos" }
            .clickable { Diptico.abrirGaleria("biblioteca") }, contentAlignment = Alignment.Center) {
            Texto("•••", 14f, familia = Nunito, espaciado = 1f, traducir = false)
        }
    }
}

/** Solo aparece cuando hay una selección restringida, con una salida que siempre queda a la vista. */
@Composable
internal fun FiltroActivoLimpio(e: Estado) {
    if (e.filtro == "todos" && e.busqueda.isBlank()) return
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    Row(Modifier.fillMaxWidth().padding(horizontal = esc.d(15), vertical = esc.d(3)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
        Texto(if (e.busqueda.isNotBlank()) "Buscar: ${e.busqueda}" else Diptico.nombreFiltro(e.filtro),
            10.5f, familia = Nunito, color = p.acentoTexto, lineas = 1, modifier = Modifier.weight(1f))
        Box(Modifier.clip(RoundedCornerShape(50)).background(Uva.copy(alpha = .12f))
            .clickable { Diptico.limpiarBusqueda() }.padding(horizontal = esc.d(12), vertical = esc.d(7))) {
            Texto("Mostrar todos", 10.5f, familia = Nunito, color = p.acentoTexto, lineas = 1)
        }
    }
}

@Composable
internal fun PieSeleccion(e: Estado) {
    val j = e.seleccionado ?: return
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    Row(Modifier.fillMaxWidth().padding(horizontal = esc.d(15), vertical = esc.d(8)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Row(Modifier.height(esc.d(42)).testTag("accion-jugar").clip(RoundedCornerShape(esc.d(14))).fondoBoton(Uva)
            .semantics { contentDescription = "Jugar ${j.titulo}" }.clickable { Diptico.jugar() }
            .padding(horizontal = esc.d(18)), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(7))) {
            IconoDiptico(Icono.JUGAR, SobreUva, 16f)
            Texto("Jugar", 13f, familia = Nunito, color = SobreUva, lineas = 1)
        }
    }
}

/** Una portada y su nombre. El detalle y las acciones del juego viven en su ficha y menú contextual. */
@Composable
internal fun PortadaSeleccion(j: Juego, elegida: Boolean, ancho: Float, alto: Float, titulo: Boolean, elegir: () -> Unit) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val color = colorColeccion(j.consolaId)
    val forma = RoundedCornerShape(esc.d(13))
    val imagen = rememberImagen(j.portada, 400)
    Column(Modifier.width(esc.d(ancho)).combinedClickable(
        onClickLabel = "Seleccionar ${j.titulo}",
        onLongClickLabel = "Opciones de ${j.titulo}",
        onLongClick = { Diptico.elegirJuego(j.clave); Diptico.abrirRapido() }, onClick = elegir),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(esc.d(alto)).shadow(esc.d(if (elegida) 7 else 2), forma, spotColor = color)
            .clip(forma).background(Brush.linearGradient(listOf(lerp(p.fondo2, color, .22f), p.fondo2)))
            .border(esc.d(if (elegida) 2 else 1), if (elegida) color else color.copy(alpha = .23f), forma),
            contentAlignment = Alignment.Center) {
            if (imagen != null) Image(imagen, j.titulo, Modifier.fillMaxSize().padding(esc.d(5)), contentScale = ContentScale.Fit)
            else ArteJuego(j, false, 400, Modifier.fillMaxSize())
            if (elegida) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth(.42f).height(esc.d(3)).background(color))
        }
        if (titulo) Texto(j.titulo, 11f, familia = Nunito,
            color = if (elegida) p.tinta else p.tenue, peso = if (elegida) FontWeight.ExtraBold else FontWeight.SemiBold,
            lineas = 2, alinear = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().height(esc.d(38f * esc.texto)).wrapContentHeight(Alignment.CenterVertically).padding(top = esc.d(7), start = esc.d(2), end = esc.d(2)))
    }
}

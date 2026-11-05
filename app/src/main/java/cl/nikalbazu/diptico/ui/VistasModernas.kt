@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
            if (elegida) Texto("A · Jugar   Y · Opciones", 9.5f, color = p.acentoTexto, lineas = 1, modifier = Modifier.padding(vertical = esc.d(4)))
            else Spacer(Modifier.height(esc.d(8)))
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
fun CarpetaBiblioteca(e: Estado, id: String, elegida: Boolean) {
    val esc = LocalEscala.current; val p = LocalPaleta.current
    val c = Consolas.porId(id)
    val juegos = remember(e.juegos, id) { if (id == "todo") e.juegos else e.juegos.filter { it.consolaId == id } }
    val color = c?.color ?: Uva
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(20)))
        .background(Brush.verticalGradient(listOf(color.copy(alpha = .22f), p.superficie)))
        .border(esc.d(if (elegida) 2.5f else 1f), if (elegida) color else p.linea, RoundedCornerShape(esc.d(20)))
        .combinedClickable(onLongClick = { if (id != "todo") Diptico.abrirEditarConsola(id) }) { if (elegida) Diptico.entrarSistema() else Diptico.elegirConsola(id) }
        .padding(esc.d(10))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoConsola(c, 22f, p.fondo)
            Spacer(Modifier.width(esc.d(6)))
            Texto(c?.nombre ?: "Todos", 13f, lineas = 1, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(esc.d(10)))
        Row(Modifier.fillMaxWidth().height(esc.d(70)), horizontalArrangement = Arrangement.spacedBy(esc.d(4))) {
            juegos.take(3).forEach { j -> Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(esc.d(6)))) { ArteJuego(j, false, 180, Modifier.fillMaxSize()) } }
        }
        Spacer(Modifier.height(esc.d(9)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Detalle("${juegos.size} juegos", 10.5f, lineas = 1)
            Texto(if (elegida) "A · Abrir" else "Explorar", 10.5f, color = if (elegida) p.acentoTexto else p.tenue, lineas = 1)
        }
    }
}

@Composable
fun ControlesBiblioteca(e: Estado) {
    val esc = LocalEscala.current; val p = LocalPaleta.current
    Row(Modifier.fillMaxWidth().padding(horizontal = esc.d(12), vertical = esc.d(6)), horizontalArrangement = Arrangement.spacedBy(esc.d(6)), verticalAlignment = Alignment.CenterVertically) {
        Texto("${e.visibles.size}", 11f, color = p.tenue)
        listOf("vistas_juegos" to Diptico.nombreVista(e.vista), "filtros" to Diptico.nombreFiltro(e.filtro), "orden" to Diptico.nombreOrden(e.orden)).forEach { (tipo, nombre) ->
            Box(Modifier.weight(1f).clip(RoundedCornerShape(50)).background(p.superficie.copy(alpha = .8f)).clickable { Diptico.abrirGaleria(tipo) }.padding(horizontal = esc.d(8), vertical = esc.d(7)), contentAlignment = Alignment.Center) {
                Texto(nombre, 10.5f, lineas = 1)
            }
        }
    }
}

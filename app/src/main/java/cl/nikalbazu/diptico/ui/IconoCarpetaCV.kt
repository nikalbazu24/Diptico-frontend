package cl.nikalbazu.diptico.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.ImagenWeb
import cl.nikalbazu.diptico.estado.*

/**
 * Ícono de la carpeta de una consola (boceto 6d): las fuentes arriba, a la izquierda «Así queda» en cada vista
 * (Ícono, Díptico, Repisa) con el candidato marcado, y a la derecha los candidatos de la fuente elegida.
 */
@Composable
internal fun IconoCarpetaCV(e: Estado, lista: List<ImagenWeb>) {
    val c = Consolas.porId(e.consolaEditando)
    val fuentes = Diptico.fuentesImagen(e)
    SideEffect { Teclas.columnasImagenes = 3 }
    LienzoBoceto(1240f, 1080f, fondo = LocalColeccion.current.fondo, zoom = ZOOM_ABAJO) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        Column(Modifier.fillMaxSize().testTag("icono-carpeta").padding(horizontal = esc.d(32), vertical = esc.d(22)), verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
            Column {
                TB("${(c?.bonito ?: "Consola").uppercase()} · ÍCONO DE LA CARPETA", 18f, color = col.tenue, peso = FontWeight.ExtraBold, espaciado = 2f)
                TB("Elige cómo se ve", 42f, titulo = true)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                items(fuentes.size) { i ->
                    val f = fuentes[i]
                    val n = if (f == "todas") e.candidatos.size else e.candidatos.count { it.fuente == f }
                    val activa = f == e.fuenteImagen
                    Row(Modifier.height(esc.d(56)).clip(CircleShape).background(if (activa) col.texto else col.vidrio).border(1.dp, col.borde, CircleShape)
                        .clickable { Diptico.actualizar { it.copy(fuenteImagen = f, indice = 0) } }.padding(horizontal = esc.d(20)),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                        TB(Diptico.nombreFuente(f), 20f, color = if (activa) col.fondo else col.texto, peso = FontWeight.ExtraBold)
                        TB("$n", 17f, color = if (activa) col.fondo.copy(alpha = .7f) else col.sub, traducir = false)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(22))) {
                // Así queda.
                val elegido = lista.getOrNull(e.indice)?.takeUnless { it.url.startsWith("accion:") }
                val img = rememberImagen(elegido?.let { Uri.parse(it.miniatura) }, 420)
                Column(Modifier.width(esc.d(360)).fillMaxHeight().vidrio(28f).padding(esc.d(22)), verticalArrangement = Arrangement.spacedBy(esc.d(16))) {
                    TB("ASÍ QUEDA", 18f, color = col.tenue, peso = FontWeight.ExtraBold, espaciado = 2f)
                    Box(Modifier.align(Alignment.CenterHorizontally)) { Muestra(img, 190f, 46f, c?.color ?: col.acento) }
                    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(14)), verticalAlignment = Alignment.Bottom) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Muestra(img, 66f, 16f, c?.color ?: col.acento); TB("Ícono", 16f, color = col.sub) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(Modifier.size(esc.d(104), esc.d(66)).clip(RoundedCornerShape(esc.d(12))).background(col.vidrioFuerte)) {
                                Box(Modifier.fillMaxHeight().aspectRatio(1f)) { Muestra(img, 66f, 12f, c?.color ?: col.acento) }
                            }
                            TB("Díptico", 16f, color = col.sub)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(esc.d(32), esc.d(86))) { Muestra(img, 32f, 5f, c?.color ?: col.acento, recortar = true) }; TB("Repisa", 16f, color = col.sub) }
                    }
                    TB(elegido?.let { "De ${it.fuente}. Se guarda en el equipo y va en el respaldo." } ?: "Elige una imagen a la derecha, o usa una tuya.", 18f, color = col.sub, lineas = 3)
                }
                // Candidatos.
                val grilla = rememberLazyGridState()
                LaunchedEffect(e.indice) { if (lista.isNotEmpty()) grilla.animateScrollToItem(e.indice.coerceIn(0, lista.size - 1)) }
                if (e.buscandoCandidatos && lista.isEmpty()) Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { TB("Buscando en las fuentes…", 22f, color = col.sub) }
                else LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1f).fillMaxHeight(), state = grilla,
                    verticalArrangement = Arrangement.spacedBy(esc.d(16)), horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                    itemsIndexed(lista, key = { _, x -> x.url }) { i, x ->
                        val activa = i == e.indice
                        val accion = x.url.startsWith("accion:")
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(esc.d(30)))
                            .background(if (accion) col.acento.copy(alpha = .18f) else Color(0xFF1B1D2E))
                            .border(if (activa) 4.dp else 1.dp, if (activa) col.texto else Color.White.copy(alpha = .12f), RoundedCornerShape(esc.d(30)))
                            .clickable { if (activa) Diptico.usarCandidato(i) else Diptico.elegirIndice(i) }, contentAlignment = Alignment.Center) {
                            if (accion) Column(Modifier.padding(esc.d(14)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
                                IconoDiptico(when (x.url) { "accion:archivos" -> Icono.CARPETA; "accion:quitar" -> Icono.BORRAR; else -> Icono.CLAVE }, col.texto, 40f, acento = Coral)
                                TB(x.fuente, 18f, lineas = 2)
                            } else {
                                val m = rememberImagen(Uri.parse(x.miniatura), 360)
                                if (m != null) Image(m, null, Modifier.fillMaxSize().padding(esc.d(10)), contentScale = ContentScale.Fit)
                                else TB("…", 30f, color = col.tenue)
                                Box(Modifier.align(Alignment.BottomStart).padding(esc.d(10)).clip(RoundedCornerShape(esc.d(12))).background(Color(0xB307080F)).padding(horizontal = esc.d(10), vertical = esc.d(3))) {
                                    TB(x.fuente, 15f, peso = FontWeight.ExtraBold, traducir = false)
                                }
                            }
                        }
                    }
                }
            }
            PistasFinas("A" to "Usar", "L1 R1" to "Fuente", "Y" to "Otro nombre", "B" to "Volver")
        }
    }
}

@Composable
private fun Muestra(img: ImageBitmap?, lado: Float, radio: Float, color: Color, recortar: Boolean = false) {
    val esc = LocalEscala.current
    Box(Modifier.then(if (recortar) Modifier.fillMaxSize() else Modifier.size(esc.d(lado))).clip(RoundedCornerShape(esc.d(radio)))
        .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(color, color.copy(alpha = .35f)))), contentAlignment = Alignment.Center) {
        if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = if (recortar) ContentScale.Crop else ContentScale.Fit)
    }
}

package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado

/** Home compacto exclusivo de una pantalla. Reutiliza el estado y acciones existentes. */
@Composable
fun HomeUnaPantallaRedisenado(e: Estado) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val recientes = remember(e.juegos, e.datos) {
        e.juegos.filter { (e.datos[it.clave]?.ultimo ?: 0L) > 0L }
            .sortedByDescending { e.datos[it.clave]?.ultimo ?: 0L }.take(6)
    }
    Column(
        Modifier.fillMaxSize().background(p.fondo).testTag("home-una-redisenado"),
    ) {
        Row(
            Modifier.fillMaxWidth().height(esc.d(48)).padding(horizontal = esc.d(14)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(8)),
        ) {
            Logo(24f)
            Texto("DÍPTICO", 14f, peso = FontWeight.Black, lineas = 1)
            Spacer(Modifier.width(esc.d(4)))
            HomeAcceso(Icono.PORTADA, "Inicio") { Diptico.alternarInicio() }
            HomeAcceso(Icono.BIBLIOTECA, "Biblioteca") { Diptico.entrarSistema() }
            HomeAcceso(Icono.APPS, "Apps") { Diptico.abrirApps() }
            HomeAcceso(Icono.BUSCAR, "Buscar") { Diptico.abrirBuscar() }
            Spacer(Modifier.weight(1f))
            RelojBateria()
            BotonPerfil(28f)
        }
        Box(Modifier.fillMaxWidth().height(esc.d(1)).background(p.linea.copy(alpha = .45f)))
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = esc.d(14), vertical = esc.d(8))) {
            Texto("Tus consolas", 13f, color = p.tenue, peso = FontWeight.Bold, lineas = 1)
            Spacer(Modifier.height(esc.d(4)))
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(esc.d(20))).background(p.superficie.copy(alpha = .34f)).padding(esc.d(6))) {
                DiscosConsolas(e, Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(esc.d(8)))
            if (recientes.isNotEmpty()) {
                Texto("Últimos juegos", 13f, color = p.tenue, peso = FontWeight.Bold, lineas = 1)
                Spacer(Modifier.height(esc.d(4)))
                LazyRow(
                    Modifier.fillMaxWidth().height(esc.d(96)).testTag("home-ultimos"),
                    horizontalArrangement = Arrangement.spacedBy(esc.d(8)),
                ) {
                    items(recientes, key = { it.clave }) { j -> UltimoJuegoCompacto(e, j) }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().height(esc.d(42)).padding(horizontal = esc.d(14)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Pistas("A" to "Abrir", "Select" to "Vista", "Start" to "Menú")
            Spacer(Modifier.weight(1f))
            if (e.extra.dock && e.appsFijadas.isNotEmpty()) FilaDock(e, 22f, conCajon = false)
            Spacer(Modifier.width(esc.d(8)))
            HomeAcceso(Icono.AJUSTES, "Ajustes") { Diptico.abrirMenu() }
        }
    }
}

@Composable
private fun HomeAcceso(icono: Icono, texto: String, accion: () -> Unit) {
    val esc = LocalEscala.current
    Row(
        Modifier.clip(RoundedCornerShape(esc.d(12))).clickable(onClickLabel = texto, onClick = accion)
            .padding(horizontal = esc.d(7), vertical = esc.d(5)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconoDiptico(icono, LocalPaleta.current.tinta, 15f)
    }
}

@Composable
private fun UltimoJuegoCompacto(e: Estado, j: Juego) {
    val esc = LocalEscala.current
    val seleccionado = e.seleccionado?.clave == j.clave
    val forma = RoundedCornerShape(esc.d(12))
    Box(
        Modifier.width(esc.d(if (seleccionado) 112f else 92f)).fillMaxHeight().clip(forma)
            .then(if (seleccionado) Modifier.border(esc.d(2), Coral, forma) else Modifier)
            .clickable { Diptico.elegirConsola(j.consolaId); Diptico.elegirJuego(j.clave) },
    ) {
        ArteJuego(j, preferirArte = false, maxLado = 320, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .82f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(esc.d(7))) {
            Texto(j.titulo, 11f, color = Color.White, peso = FontWeight.Bold, lineas = 1, traducir = false)
            Texto(Consolas.porId(j.consolaId)?.nombre ?: j.consolaId, 9f, color = Color.White.copy(alpha = .75f), lineas = 1, traducir = false)
        }
    }
}

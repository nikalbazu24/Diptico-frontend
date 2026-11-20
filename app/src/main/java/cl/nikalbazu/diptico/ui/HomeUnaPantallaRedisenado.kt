package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.*

/**
 * Inicio exclusivo para una pantalla. Todas las selecciones dependen del mismo Estado
 * que usa el mando: la cruceta recorre los juegos, L1/R1 las consolas y Select los widgets.
 * No modifica la navegación, el tema ni los widgets del modo de dos pantallas.
 */
@Composable
fun HomeUnaPantallaRedisenado(e: Estado) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val recientes = remember(e.juegos, e.datos) {
        e.juegos.filter { (e.datos[it.clave]?.ultimo ?: 0L) > 0L && e.datos[it.clave]?.oculto != true }
            .sortedByDescending { e.datos[it.clave]?.ultimo ?: 0L }
            .take(6)
    }

    // La lista de juegos es una sola fila: arriba/abajo no deben mover una
    // selección invisible como sucede al reutilizar el cálculo de mosaicos.
    DisposableEffect(Unit) {
        val anterior = Teclas.mosaicosUnaFila
        Teclas.mosaicosUnaFila = true
        onDispose { Teclas.mosaicosUnaFila = anterior }
    }

    Column(Modifier.fillMaxSize().background(p.fondo).testTag("home-una-redisenado")) {
        Row(
            Modifier.fillMaxWidth().height(esc.d(48)).padding(horizontal = esc.d(14)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(7)),
        ) {
            Logo(24f)
            Texto("DÍPTICO", 14f, peso = FontWeight.Black, lineas = 1, traducir = false)
            Spacer(Modifier.width(esc.d(3)))
            HomeAcceso(Icono.VISTA, "Widgets") { Diptico.alternarInicio() }
            HomeAcceso(Icono.BIBLIOTECA, "Carpetas") { Diptico.elegirVistaConsolas("carpetas") }
            HomeAcceso(Icono.APPS, "Apps") { Diptico.abrirApps() }
            HomeAcceso(Icono.BUSCAR, "Buscar") { Diptico.abrirBuscar() }
            Spacer(Modifier.weight(1f))
            RelojBateria()
            BotonPerfil(28f)
        }
        Box(Modifier.fillMaxWidth().height(esc.d(1)).background(p.linea.copy(alpha = 0.45f)))

        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val compacto = maxHeight < 420.dp
            Column(
                Modifier.fillMaxSize().padding(horizontal = esc.d(14), vertical = esc.d(8)),
                verticalArrangement = Arrangement.spacedBy(esc.d(6)),
            ) {
            Texto("Tus consolas", 13f, color = p.tenue, peso = FontWeight.Bold, lineas = 1)
            LazyRow(
                Modifier.fillMaxWidth().height(esc.d(if (compacto) 62f else 80f)).testTag("home-consolas"),
                horizontalArrangement = Arrangement.spacedBy(esc.d(8)),
            ) {
                items(e.pestanas, key = { it }) { id -> ConsolaHome(e, id) }
            }

            Texto("Juego seleccionado", 13f, color = p.tenue, peso = FontWeight.Bold, lineas = 1)
            val seleccionado = e.seleccionado
            val forma = RoundedCornerShape(esc.d(17))
            Row(
                Modifier.fillMaxWidth().weight(1f).clip(forma).background(p.superficie.copy(alpha = 0.7f))
                    .testTag("home-destacado"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (seleccionado != null) {
                    Box(Modifier.width(esc.d(108)).fillMaxHeight().clip(forma)) {
                        ArteJuego(seleccionado, preferirArte = false, maxLado = 320, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Transparent, p.fondo.copy(alpha = .15f)))))
                    }
                    Column(
                        Modifier.weight(1f).padding(horizontal = esc.d(14), vertical = esc.d(7)),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Texto(seleccionado.titulo, 20f, peso = FontWeight.Black, lineas = 2, traducir = false)
                        Texto(
                            Consolas.porId(seleccionado.consolaId)?.nombre ?: seleccionado.consolaId,
                            11f, color = p.tenue, lineas = 1, traducir = false,
                        )
                        Spacer(Modifier.height(esc.d(6)))
                        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                            HomeAcceso(Icono.JUGAR, "Jugar") { Diptico.jugar() }
                            HomeAcceso(Icono.INFO, "Ficha") { Diptico.abrirFicha() }
                        }
                    }
                } else {
                    Texto(
                        if (e.escaneando) "Buscando tus juegos…" else "Elige una consola o añade juegos a tu biblioteca",
                        15f, modifier = Modifier.padding(esc.d(14)),
                    )
                }
            }

            Texto(
                "Juegos en la consola · " + e.visibles.size,
                13f, color = p.tenue, peso = FontWeight.Bold, lineas = 1,
            )
            val estadoJuegos = rememberLazyListState()
            LaunchedEffect(e.consolaSel, e.indiceSel, e.visibles.size) {
                if (e.visibles.isNotEmpty()) {
                    estadoJuegos.animateScrollToItem(e.indiceSel.coerceIn(0, e.visibles.lastIndex))
                }
            }
            LazyRow(
                state = estadoJuegos,
                modifier = Modifier.fillMaxWidth().height(esc.d(if (compacto) 72f else 92f)).testTag("home-juegos"),
                horizontalArrangement = Arrangement.spacedBy(esc.d(8)),
            ) {
                items(e.visibles, key = { it.clave }) { j -> UltimoJuegoCompacto(e, j) }
            }

            if (recientes.isNotEmpty() && !compacto) {
                Texto("Últimos juegos", 13f, color = p.tenue, peso = FontWeight.Bold, lineas = 1)
                LazyRow(
                    Modifier.fillMaxWidth().height(esc.d(74)).testTag("home-ultimos"),
                    horizontalArrangement = Arrangement.spacedBy(esc.d(8)),
                ) {
                    items(recientes, key = { it.clave }) { j -> UltimoJuegoCompacto(e, j) }
                }
            }
            }
        }

        Row(
            Modifier.fillMaxWidth().height(esc.d(42)).padding(horizontal = esc.d(14)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (esc.angosta) Pistas("A" to "Jugar", "Select" to "Widgets")
            else Pistas("A" to "Jugar", "L1/R1" to "Consola", "Select" to "Widgets")
            Spacer(Modifier.weight(1f))
            if (e.extra.dock && e.extra.dockVisible && e.appsFijadas.isNotEmpty())
                FilaDock(e, 22f, conCajon = false)
            Spacer(Modifier.width(esc.d(6)))
            HomeAcceso(Icono.AJUSTES, "Ajustes") { Diptico.abrirMenu() }
        }
    }
}

/** Carpeta visual: seleccionar aquí cambia el mismo estado que cambia L1/R1. */
@Composable
private fun ConsolaHome(e: Estado, id: String) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val c = remember(id) { Consolas.porId(id) }
    val seleccionada = id == e.consolaSel
    val forma = RoundedCornerShape(esc.d(13))
    val cantidad = if (id == "todo") e.juegos.size else e.juegosPorConsola[id].orEmpty().size
    Column(
        Modifier.width(esc.d(110)).fillMaxHeight().clip(forma)
            .background(if (seleccionada) p.superficie else p.superficie.copy(alpha = .44f))
            .then(if (seleccionada) Modifier.border(esc.d(2), Coral, forma) else Modifier)
            .clickable(onClickLabel = "Elegir consola") { Diptico.elegirConsola(id) }
            .padding(horizontal = esc.d(7), vertical = esc.d(6)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconoConsola(c, 22f, p.fondo)
        Spacer(Modifier.height(esc.d(3)))
        Texto(c?.nombre ?: if (id == "todo") "Todos" else id, 11f, peso = FontWeight.Bold, lineas = 1, traducir = false)
        Texto(juegosTexto(cantidad), 9f, color = p.tenue, lineas = 1)
    }
}

/** El primer toque selecciona; tocar otra vez el juego seleccionado lo abre. */
@Composable
private fun UltimoJuegoCompacto(e: Estado, j: Juego) {
    val esc = LocalEscala.current
    val seleccionado = e.seleccionado?.clave == j.clave
    val forma = RoundedCornerShape(esc.d(12))
    Box(
        Modifier.width(esc.d(if (seleccionado) 112f else 92f)).fillMaxHeight().clip(forma)
            .then(if (seleccionado) Modifier.border(esc.d(2), Coral, forma) else Modifier)
            .clickable(onClickLabel = if (seleccionado) "Jugar" else "Seleccionar juego") {
                if (seleccionado) Diptico.jugar()
                else {
                    Diptico.elegirConsola(j.consolaId)
                    Diptico.elegirJuego(j.clave)
                }
            },
    ) {
        ArteJuego(j, preferirArte = false, maxLado = 320, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .82f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(esc.d(7))) {
            Texto(j.titulo, 11f, color = Color.White, peso = FontWeight.Bold, lineas = 1, traducir = false)
            Texto(Consolas.porId(j.consolaId)?.nombre ?: j.consolaId, 9f, color = Color.White.copy(alpha = .75f), lineas = 1, traducir = false)
        }
    }
}

@Composable
private fun HomeAcceso(icono: Icono, texto: String, accion: () -> Unit) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    Row(
        Modifier.clip(RoundedCornerShape(esc.d(11))).clickable(onClickLabel = texto, onClick = accion)
            .background(p.superficie.copy(alpha = .35f))
            .padding(horizontal = esc.d(8), vertical = esc.d(5)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(esc.d(5)),
    ) {
        IconoDiptico(icono, p.tinta, 15f)
        if (!esc.angosta) Texto(texto, 11f, color = p.tinta, peso = FontWeight.Bold, lineas = 1)
    }
}

package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.*

/*
 * Inicio en una pantalla (idea A del boceto) y modo sala para la tele. Regla: nada encima de nada. Cada parte
 * tiene su franja y entre todas llenan la pantalla de borde a borde. Solo DIPPO flota, porque se mueve con el dedo.
 */

private val FondoRiel = Color(0xFF0C0E1B)
private val Linea = Color(0xFF1F2340)
private val Chip = Color(0xFF171B34)

/** Idea A: riel de consolas a la izquierda; a la derecha barra, vitrina, filtros, estante de juegos y pie. */
@Composable
fun InicioRiel(e: Estado) {
    val esc = LocalEscala.current
    Row(Modifier.fillMaxSize().background(Color(0xFF090B14)).testTag("inicio-riel")) {
        RielConsolasNuevo(e, Modifier.width(esc.d(92)).fillMaxHeight())
        Column(Modifier.weight(1f).fillMaxHeight()) {
            BarraRiel(e)
            val j = e.seleccionado
            Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = esc.d(12))) {
                if (j != null) VitrinaRiel(e, j, Modifier.fillMaxSize(), grande = false)
                else Texto(if (e.escaneando) "Buscando tus juegos…" else "No encontré juegos en esta consola", 20f, Modifier.align(Alignment.Center))
            }
            FiltrosRiel(e)
            EstanteRiel(e, Modifier.fillMaxWidth().height(esc.d(160)))
            PieRiel(e, tele = false)
        }
    }
}

/** Modo sala (tele): vitrina grande con amigos, «Seguir jugando», consolas con sus íconos y pistas grandes. */
@Composable
fun InicioSala(e: Estado) {
    val esc = LocalEscala.current
    val margen = esc.d(40) // margen de seguridad de la tele, dentro de cada franja
    Column(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF1D2350), Color(0xFF090B14)), radius = 1800f)).testTag("inicio-sala")) {
        Row(Modifier.fillMaxWidth().height(esc.d(56)).padding(horizontal = margen), verticalAlignment = Alignment.CenterVertically) {
            PerfilArriba(40f)
            Spacer(Modifier.weight(1f))
            DippoInicio(e, 40f, lugar = "sala")
            Spacer(Modifier.width(esc.d(14)))
            RelojBateria()
        }
        val j = e.seleccionado
        Row(Modifier.fillMaxWidth().weight(1f).padding(horizontal = margen), horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
            if (j != null) VitrinaRiel(e, j, Modifier.weight(1f).fillMaxHeight(), grande = true)
            AmigosAhora(Modifier.width(esc.d(210)).fillMaxHeight())
        }
        SeguirJugando(e, Modifier.fillMaxWidth().height(esc.d(132)).padding(horizontal = margen))
        ConsolasSala(e, Modifier.fillMaxWidth().height(esc.d(84)).padding(horizontal = margen))
        PieRiel(e, tele = true)
    }
}

// ---------- Piezas ----------

@Composable
private fun RielConsolasNuevo(e: Estado, modifier: Modifier) {
    val esc = LocalEscala.current
    val estado = rememberLazyListState()
    val sel = e.pestanas.indexOf(e.consolaSel).coerceAtLeast(0)
    LaunchedEffect(sel) { estado.animateScrollToItem((sel - 2).coerceAtLeast(0)) }
    Column(modifier.background(Brush.verticalGradient(listOf(Color(0xFF121527), FondoRiel))), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(esc.d(10)))
        BotonPerfil(40f)
        Spacer(Modifier.height(esc.d(8)))
        Box(Modifier.width(esc.d(56)).height(esc.d(1)).background(Linea))
        LazyColumn(
            state = estado, modifier = Modifier.fillMaxWidth().weight(1f).testTag("riel-consolas"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(esc.d(8)),
            contentPadding = PaddingValues(vertical = esc.d(10)),
        ) {
            itemsIndexed(e.pestanas, key = { _, id -> id }) { _, id -> IconoRiel(id, id == e.consolaSel) }
        }
    }
}

@Composable
private fun IconoRiel(id: String, elegida: Boolean) {
    val esc = LocalEscala.current
    val c = remember(id) { Consolas.porId(id) }
    val tono = c?.color ?: Uva
    val lado by animateDpAsState(if (elegida) esc.d(64) else esc.d(50), label = "riel")
    val forma = RoundedCornerShape(if (elegida) esc.d(20) else esc.d(16))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(lado)
                .then(if (elegida) Modifier.shadow(esc.d(14), forma, ambientColor = tono, spotColor = tono) else Modifier)
                .clip(forma)
                .background(Brush.linearGradient(listOf(tono, tono.copy(alpha = 0.35f))))
                .then(if (elegida) Modifier.border(esc.d(3), Color.White, forma) else Modifier)
                .clickable { Diptico.elegirConsola(id) },
            contentAlignment = Alignment.Center,
        ) { IconoConsola(c, if (elegida) 36f else 28f, Color.Transparent) }
        if (elegida) {
            Spacer(Modifier.height(esc.d(3)))
            Texto(c?.nombre ?: "Todo", 11f, color = Color.White, peso = FontWeight.ExtraBold, familia = Nunito, lineas = 1)
        }
    }
}

@Composable
private fun BarraRiel(e: Estado) {
    val esc = LocalEscala.current
    val c = Consolas.porId(e.consolaSel)
    Row(Modifier.fillMaxWidth().height(esc.d(44)).padding(horizontal = esc.d(16)), verticalAlignment = Alignment.CenterVertically) {
        Texto(((c?.nombreLargo ?: "Todos tus juegos") + " · " + e.visibles.size + " juegos").uppercase(), 12f,
            color = Color(0xFFFFD2BF), familia = Nunito, peso = FontWeight.Black, lineas = 1, espaciado = 1.6f, traducir = false)
        Spacer(Modifier.weight(1f))
        AmigosJugando()
        Spacer(Modifier.width(esc.d(10)))
        DippoInicio(e, 36f, lugar = "riel")
        Spacer(Modifier.width(esc.d(10)))
        RelojBateria()
    }
}

/** «N amigos jugando», solo si Discord está conectado y hay alguien. */
@Composable
private fun AmigosJugando() {
    val esc = LocalEscala.current
    val amigos by SocialDiscord.amigos.collectAsState()
    val pantalla = pantallaActual()
    val n = amigos.count { it.estado == "jugando" }
    if (n == 0) return
    Row(Modifier.clip(RoundedCornerShape(50)).clickable { Diptico.abrirAmigos(pantalla) }.padding(horizontal = esc.d(6), vertical = esc.d(4)),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(esc.d(8)).clip(CircleShape).background(Color(0xFF3EE08F)))
        Spacer(Modifier.width(esc.d(6)))
        Texto(if (n == 1) "1 amigo jugando" else "$n amigos jugando", 12f, color = Color(0xFF9FE8C4), familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1, traducir = false)
    }
}

/** La vitrina: el arte del juego de fondo; a la izquierda la información y los botones, a la derecha la caja 3D. */
@Composable
private fun VitrinaRiel(e: Estado, j: Juego, modifier: Modifier, grande: Boolean) {
    val esc = LocalEscala.current
    val c = Consolas.porId(j.consolaId)
    val forma = RoundedCornerShape(esc.d(22))
    val datos = e.datos[j.clave]
    val tiempo = (datos?.tiempo ?: 0L).toLong()
    Box(modifier.clip(forma).background(Color(0xFF120D1C)).border(esc.d(1), Color.White.copy(alpha = 0.08f), forma).testTag("vitrina-riel")) {
        ArteJuego(j, preferirArte = true, maxLado = 1280, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color(0xEB0C0916), 0.5f to Color(0x8C0C0916), 1f to Color(0x1A0C0916))))
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = esc.d(22), vertical = esc.d(14)), verticalArrangement = Arrangement.Center) {
                if (grande && c != null) {
                    Texto(c.nombreLargo.uppercase(), 13f, color = Color(0xFFCFE0FF), familia = Nunito, peso = FontWeight.Black, lineas = 1, espaciado = 1.8f, traducir = false)
                    Spacer(Modifier.height(esc.d(6)))
                }
                Texto(j.titulo, if (grande) 40f else 34f, color = Color.White, lineas = 2, interlineado = if (grande) 42f else 36f, sombra = true, traducir = false)
                Spacer(Modifier.height(esc.d(8)))
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    if (tiempo > 0) ChipVitrina(formatoTiempo(tiempo))
                    if (datos?.favorito == true) ChipVitrina("Favorito")
                }
                Spacer(Modifier.height(esc.d(6)))
                LineaDuracion(e, j)
                val paso = datos?.paso.orEmpty()
                if (paso.isNotBlank()) {
                    Spacer(Modifier.height(esc.d(6)))
                    Texto("Próximo paso: $paso", 13f, color = Color(0xFFE8E9F3), familia = Nunito, peso = FontWeight.Bold, lineas = 1, traducir = false)
                }
                Spacer(Modifier.height(esc.d(10)))
                BotonesVitrina(e, j)
            }
            if (c != null && e.extra.disposiciones[j.clave]?.conCaja != false) {
                Box(Modifier.width(if (grande) esc.d(200) else esc.d(230)).fillMaxHeight().padding(vertical = esc.d(12)), contentAlignment = Alignment.Center) {
                    Caja3D(datosCaja(e, j, c), j, c, Modifier.fillMaxSize(0.86f), giroY = { -26f }, giroX = { -6f }, caras = e.extra.caras[j.clave], detalle = false)
                }
            }
        }
    }
}

@Composable
private fun ChipVitrina(texto: String) {
    val esc = LocalEscala.current
    Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.13f)).padding(horizontal = esc.d(10), vertical = esc.d(3))) {
        Texto(texto, 12f, color = Color.White, familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1, traducir = false)
    }
}

/** Filtros de la biblioteca (los mismos de Ajustes), tocando. */
@Composable
private fun FiltrosRiel(e: Estado) {
    val esc = LocalEscala.current
    val filtros = listOf("todos", "recientes", "favoritos", "sinjugar", "mas")
    Row(Modifier.fillMaxWidth().height(esc.d(34)).padding(horizontal = esc.d(16)), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
        filtros.forEach { f ->
            val act = e.filtro == f
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(if (act) Color.White else Chip)
                    .clickable { Diptico.elegirFiltro(f) }.padding(horizontal = esc.d(12), vertical = esc.d(4)),
            ) { Texto(Diptico.nombreFiltro(f), 12f, color = if (act) Color(0xFF14141C) else Color(0xFFCFD2E6), familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1) }
        }
    }
}

/** El estante: cajas en fila; la elegida sube y crece. Debajo, cuánto llevas (si se sabe cuánto dura). */
@Composable
private fun EstanteRiel(e: Estado, modifier: Modifier) {
    val esc = LocalEscala.current
    val lista = e.visibles
    val estado = rememberLazyListState()
    SideEffect { Teclas.columnasCuadricula = lista.size.coerceAtLeast(1) }
    LaunchedEffect(e.indiceSel, lista.size) { if (lista.isNotEmpty()) estado.animateScrollToItem((e.indiceSel - 2).coerceAtLeast(0)) }
    LazyRow(
        state = estado, modifier = modifier.testTag("estante-riel"),
        contentPadding = PaddingValues(horizontal = esc.d(16), vertical = esc.d(8)),
        horizontalArrangement = Arrangement.spacedBy(esc.d(12)),
        verticalAlignment = Alignment.Bottom,
    ) {
        itemsIndexed(lista, key = { _, j -> j.clave }) { i, j -> CajaEstante(e, j, i == e.indiceSel) }
    }
}

@Composable
private fun CajaEstante(e: Estado, j: Juego, elegido: Boolean) {
    val esc = LocalEscala.current
    val ancho by animateDpAsState(if (elegido) esc.d(100) else esc.d(80), label = "caja-ancho")
    val alto by animateDpAsState(if (elegido) esc.d(130) else esc.d(104), label = "caja-alto")
    val forma = RoundedCornerShape(esc.d(8))
    val avance = avanceDe(e, j)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.width(ancho).height(alto)
                .then(if (elegido) Modifier.shadow(esc.d(16), forma, ambientColor = Color(0xFFFF8C5A), spotColor = Color(0xFFFF8C5A)) else Modifier)
                .clip(forma).background(Chip)
                .then(if (elegido) Modifier.border(esc.d(3), Color.White, forma) else Modifier)
                .clickable { if (elegido) Diptico.jugar() else Diptico.elegirJuego(j.clave) },
        ) { ArteJuego(j, preferirArte = false, maxLado = 300, modifier = Modifier.fillMaxSize()) }
        Spacer(Modifier.height(esc.d(4)))
        Box(Modifier.width(ancho).height(esc.d(4)).clip(RoundedCornerShape(50)).background(if (avance != null) Color(0xFF262A47) else Color.Transparent)) {
            if (avance != null) Box(Modifier.fillMaxWidth(avance).fillMaxHeight().background(if (avance >= 1f) Color(0xFF3EE08F) else Color(0xFFFFB454)))
        }
    }
}

/** Cuánto llevas de la historia (tu tiempo / lo que dura según HowLongToBeat), o null si no se sabe. */
private fun avanceDe(e: Estado, j: Juego): Float? {
    val d = e.extra.duraciones[j.clave]?.takeIf { it.tiene && it.principal > 0f } ?: return null
    val seg = (e.datos[j.clave]?.tiempo ?: 0L).toLong()
    if (seg <= 0) return null
    return (seg / 3600f / d.principal).coerceIn(0.03f, 1f)
}

/** Tele: amigos de Discord conectados (solo si hay). */
@Composable
private fun AmigosAhora(modifier: Modifier) {
    val esc = LocalEscala.current
    val amigos by SocialDiscord.amigos.collectAsState()
    val lista = amigos.filter { it.estado != "desconectado" }.sortedBy { if (it.estado == "jugando") 0 else 1 }.take(4)
    Column(modifier.clip(RoundedCornerShape(esc.d(20))).background(Color.White.copy(alpha = 0.05f)).padding(esc.d(12)),
        verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        Texto("AMIGOS AHORA", 11f, color = Color(0xFF9FE8C4), familia = Nunito, peso = FontWeight.Black, lineas = 1, espaciado = 1.4f)
        if (lista.isEmpty()) Detalle("Conecta Discord en Ajustes › Cuentas para ver a tus amigos aquí.", 12f, lineas = 4)
        lista.forEach { a ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(12))).background(Color.Black.copy(alpha = 0.3f)).padding(esc.d(8)),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(esc.d(10)).clip(CircleShape).background(if (a.estado == "jugando") Color(0xFF3EE08F) else Color(0xFF6FA8FF)))
                Spacer(Modifier.width(esc.d(8)))
                Column(Modifier.weight(1f)) {
                    Texto(a.nombre, 13f, color = Color.White, familia = Nunito, peso = FontWeight.Black, lineas = 1, traducir = false)
                    Texto(a.actividad ?: "Conectado", 11f, color = Color(0xFFCFD2E6), familia = Nunito, peso = FontWeight.Bold, lineas = 1, traducir = a.actividad == null)
                }
            }
        }
    }
}

/** Tele: los últimos juegos que jugaste, de cualquier consola, con su avance. */
@Composable
private fun SeguirJugando(e: Estado, modifier: Modifier) {
    val esc = LocalEscala.current
    val recientes = remember(e.juegos, e.datos) {
        e.juegos.filter { (e.datos[it.clave]?.ultimo ?: 0L) > 0L }.sortedByDescending { e.datos[it.clave]?.ultimo ?: 0L }.take(5)
    }
    Column(modifier) {
        Spacer(Modifier.height(esc.d(6)))
        Texto("Seguir jugando", 15f, color = Color(0xFFCFD2E6), familia = Nunito, peso = FontWeight.Black, lineas = 1)
        Spacer(Modifier.height(esc.d(6)))
        if (recientes.isEmpty()) { Detalle("Aquí aparecerán los juegos que vayas jugando.", 13f); return@Column }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
            recientes.forEach { j ->
                val elegido = j.clave == e.seleccionado?.clave
                val forma = RoundedCornerShape(esc.d(14))
                Box(
                    Modifier.weight(if (elegido) 1.25f else 1f).fillMaxHeight().clip(forma)
                        .then(if (elegido) Modifier.border(esc.d(4), Color.White, forma) else Modifier)
                        .clickable { Diptico.elegirConsola(j.consolaId); Diptico.elegirJuego(j.clave) },
                ) {
                    ArteJuego(j, preferirArte = true, maxLado = 480, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color(0xCC000000))))
                    Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(esc.d(10))) {
                        Texto(j.titulo, 13f, color = Color.White, familia = Nunito, peso = FontWeight.Black, lineas = 1, traducir = false)
                        val a = avanceDe(e, j)
                        if (a != null) {
                            Spacer(Modifier.height(esc.d(4)))
                            Box(Modifier.fillMaxWidth().height(esc.d(4)).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.4f))) {
                                Box(Modifier.fillMaxWidth(a).fillMaxHeight().background(Color.White))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tele: las consolas con sus íconos, repartidas a lo ancho. */
@Composable
private fun ConsolasSala(e: Estado, modifier: Modifier) {
    val esc = LocalEscala.current
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(esc.d(22)), verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(vertical = esc.d(6))) {
        itemsIndexed(e.pestanas, key = { _, id -> id }) { _, id -> IconoRiel(id, id == e.consolaSel) }
    }
}

/** Pie: pistas de botones, apps fijadas, Buscar y Ajustes. En la tele, solo pistas (todo se hace con el mando). */
@Composable
private fun PieRiel(e: Estado, tele: Boolean) {
    val esc = LocalEscala.current
    Row(
        Modifier.fillMaxWidth().height(if (tele) esc.d(38) else esc.d(36)).background(Color.Black.copy(alpha = 0.25f))
            .padding(horizontal = if (tele) esc.d(40) else esc.d(16)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tele) {
            Spacer(Modifier.weight(1f))
            Pistas("A" to "Jugar", "L1/R1" to "Consola", "Y" to "Opciones", "Start" to "Ajustes")
            Spacer(Modifier.weight(1f))
        } else {
            Pistas("A" to "Jugar", "L1/R1" to "Consola", "Y" to "Opciones", "Start" to "Menú")
            Spacer(Modifier.weight(1f))
            if (e.extra.dock) FilaDock(e, 22f, conCajon = false)
            Spacer(Modifier.width(esc.d(12)))
            BotonPie(Icono.BUSCAR, "Buscar") { Diptico.abrirBuscar() }
            Spacer(Modifier.width(esc.d(8)))
            BotonPie(Icono.AJUSTES, "Ajustes") { Diptico.abrirMenu() }
        }
    }
}

@Composable
private fun BotonPie(icono: Icono, texto: String, alPulsar: () -> Unit) {
    val esc = LocalEscala.current
    Row(Modifier.clip(RoundedCornerShape(50)).background(Chip).clickable(onClick = alPulsar).padding(horizontal = esc.d(10), vertical = esc.d(4)),
        verticalAlignment = Alignment.CenterVertically) {
        IconoDiptico(icono, Color.White, 14f)
        Spacer(Modifier.width(esc.d(6)))
        Texto(texto, 12f, color = Color.White, familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1)
    }
}

package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Opcion
import cl.nikalbazu.diptico.estado.Seccion
import cl.nikalbazu.diptico.estado.Teclas

fun colorSeccion(s: Seccion): Color = when (s) {
    Seccion.PANTALLAS -> Color(0xFF6B4EFF)
    Seccion.INICIO -> Color(0xFF00ACC1)
    Seccion.ESTANTE -> Color(0xFFFF8A5B)
    Seccion.TEMAS -> Color(0xFFB388FF)
    Seccion.APARIENCIA -> Color(0xFFFF5D8F)
    Seccion.MUSICA -> Color(0xFFFF7A59)
    Seccion.EMULADORES -> Color(0xFF3BA8FF)
    Seccion.PORTADAS -> Color(0xFFFFC93C)
    Seccion.BIBLIOTECA -> Color(0xFF2EC4A6)
    Seccion.CONSOLA -> Color(0xFF8BC34A)
    Seccion.SISTEMA -> Color(0xFF8E8AAE)
}

private fun tintaSobre(c: Color) = if (c.luminance() > 0.55f) Tinta else Color.White

/**
 * El menú: primero una grilla de secciones con su color e ícono; al entrar, la página de la sección
 * con sus opciones (los Sí/No se ven como interruptores).
 */
@Composable
fun PanelMenu(e: Estado, conAyuda: Boolean) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Box(
            Modifier.align(Alignment.TopEnd).size(esc.d(360))
                .background(Brush.radialGradient(listOf(colorSeccion(e.seccion).copy(alpha = 0.2f), Color.Transparent))),
        )
        AnimatedContent(
            targetState = e.enOpciones,
            transitionSpec = {
                if (targetState) (fadeIn(tween(240)) + slideInHorizontally(tween(300)) { it / 6 }) togetherWith fadeOut(tween(140))
                else (fadeIn(tween(240)) + scaleIn(tween(300), initialScale = 0.96f)) togetherWith fadeOut(tween(140))
            },
            label = "menu",
            modifier = Modifier.fillMaxSize(),
        ) { adentro ->
            if (adentro) PaginaSeccion(e, conAyuda) else GrillaSecciones(e)
        }
    }
}

@Composable
private fun GrillaSecciones(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column(Modifier.fillMaxSize().padding(start = esc.d(14), end = esc.d(14), top = esc.d(12), bottom = esc.d(10))) {
        Row(Modifier.fillMaxWidth().height(esc.d(40)), verticalAlignment = Alignment.CenterVertically) {
            MarcaLogo(14f)
            Spacer(Modifier.width(esc.d(10)))
            Texto("Menú", 22f)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(esc.d(36)).clip(CircleShape).background(p.superficie).clickable { Diptico.cerrarPanel() },
                contentAlignment = Alignment.Center,
            ) { IconoDiptico(Icono.CERRAR, p.tinta, 18f) }
        }
        Spacer(Modifier.height(esc.d(8)))
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val anchoU = maxWidth.value / esc.factor
            val cols = if (anchoU < 560f) 3 else 4
            SideEffect { Teclas.columnasMenu = cols }
            val estado = rememberLazyGridState()
            LaunchedEffect(e.seccion) { estado.animateScrollToItem((e.seccion.ordinal - cols).coerceAtLeast(0)) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols), state = estado, modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(esc.d(8)), verticalArrangement = Arrangement.spacedBy(esc.d(8)),
                contentPadding = PaddingValues(bottom = esc.d(4)),
            ) {
                itemsIndexed(Diptico.seccionesMenu()) { _, s ->
                    val activa = s == e.seccion
                    val color = colorSeccion(s)
                    val tam by animateFloatAsState(if (activa) 1.04f else 1f, spring(dampingRatio = 0.55f), label = "tile")
                    Column(
                        Modifier.height(esc.d(92)).graphicsLayer { scaleX = tam; scaleY = tam }
                            .clip(RoundedCornerShape(esc.d(20)))
                            .then(
                                if (activa) Modifier.fondoBoton(color)
                                else Modifier.background(Brush.linearGradient(listOf(p.superficie, p.superficie.copy(alpha = 0.7f)))),
                            )
                            .then(if (activa) Modifier.border(esc.d(2), Color.White.copy(alpha = 0.5f), RoundedCornerShape(esc.d(20))) else Modifier)
                            .clickable { Diptico.elegirSeccion(s) }
                            .padding(esc.d(10)),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(
                            Modifier.size(esc.d(36)).clip(CircleShape).background(if (activa) Color.White.copy(alpha = 0.22f) else color.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) { IconoDiptico(s.icono, if (activa) tintaSobre(color) else color, 20f, acento = if (activa) tintaSobre(color) else color) }
                        Texto(s.titulo, 14.5f, color = if (activa) tintaSobre(color) else p.tinta, lineas = 1)
                    }
                }
            }
        }
        Spacer(Modifier.height(esc.d(6)))
        Pistas("A" to "Entrar", "B" to "Cerrar", "L1 R1" to "Sección")
    }
}

/** ¿Es una opción de Sí/No? Entonces se dibuja como interruptor. */
private fun esInterruptor(o: Opcion) = o.valor == "Sí" || o.valor == "No"

@Composable
private fun PaginaSeccion(e: Estado, conAyuda: Boolean) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val s = e.seccion
    val color = colorSeccion(s)
    val opciones = Diptico.opcionesSeccion(s)
    Column(Modifier.fillMaxSize()) {
        // Encabezado con el color de la sección.
        Row(
            Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
                .padding(start = esc.d(10), end = esc.d(14), top = esc.d(10), bottom = esc.d(10)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(esc.d(36)).clip(CircleShape).clickable { Diptico.columnaMenu(false) }, contentAlignment = Alignment.Center) {
                Texto("‹", 26f, color = p.tinta)
            }
            Box(Modifier.size(esc.d(40)).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                IconoDiptico(s.icono, tintaSobre(color), 22f, acento = tintaSobre(color))
            }
            Spacer(Modifier.width(esc.d(10)))
            Column(Modifier.weight(1f)) {
                Etiqueta("Menú", p.tenue, 11f)
                Texto(s.titulo, 21f, lineas = 1)
            }
        }
        val lista = rememberLazyListState()
        LaunchedEffect(e.indice) { if (opciones.isNotEmpty()) lista.animateScrollToItem((e.indice - 2).coerceAtLeast(0)) }
        LazyColumn(
            state = lista, modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = esc.d(12), end = esc.d(12), top = esc.d(6), bottom = esc.d(6)),
            verticalArrangement = Arrangement.spacedBy(esc.d(6)),
        ) {
            itemsIndexed(opciones) { i, o -> FilaOpcion(o, i == e.indice, color) { Diptico.elegirIndice(i) } }
        }
        if (conAyuda) {
            val o = opciones.getOrNull(e.indice)
            Detalle(o?.ayuda?.ifEmpty { null } ?: s.ayuda, 13.5f, lineas = 2, modifier = Modifier.padding(horizontal = esc.d(14)))
        }
        Pistas("↑ ↓" to "Moverte", "A" to "Cambiar", "B" to "Secciones", modifier = Modifier.padding(start = esc.d(14), top = esc.d(4), bottom = esc.d(10)))
    }
}

/** Una opción del menú: ícono, título, y a la derecha su valor o un interruptor. */
@Composable
private fun FilaOpcion(o: Opcion, activa: Boolean, color: Color, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val fondo by animateColorAsState(if (activa) p.superficie else p.superficie.copy(alpha = 0.45f), label = "fila")
    Row(
        Modifier.fillMaxWidth().height(esc.d(56)).clip(RoundedCornerShape(esc.d(16))).background(fondo)
            .then(if (activa) Modifier.border(esc.d(2), color, RoundedCornerShape(esc.d(16))) else Modifier)
            .clickable { alPulsar() }
            .graphicsLayer { alpha = if (o.apagada) 0.5f else 1f }
            .padding(horizontal = esc.d(12)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        o.icono?.let {
            Box(Modifier.size(esc.d(34)).clip(CircleShape).background(color.copy(alpha = if (activa) 0.3f else 0.14f)), contentAlignment = Alignment.Center) {
                IconoDiptico(it, p.tinta, 18f, acento = color)
            }
            Spacer(Modifier.width(esc.d(12)))
        }
        Texto(o.titulo, 15.5f, lineas = 1, modifier = Modifier.weight(1f))
        when {
            esInterruptor(o) -> {
                val si = o.valor == "Sí"
                Box(
                    Modifier.size(esc.d(46), esc.d(26)).clip(RoundedCornerShape(50)).background(if (si) color else p.linea),
                    contentAlignment = if (si) Alignment.CenterEnd else Alignment.CenterStart,
                ) { Box(Modifier.padding(esc.d(3)).size(esc.d(20)).clip(CircleShape).background(Color.White)) }
            }
            o.valor.isNotEmpty() -> Box(
                Modifier.clip(RoundedCornerShape(50)).background(if (activa) color.copy(alpha = 0.22f) else p.fondo2.copy(alpha = 0.6f))
                    .padding(horizontal = esc.d(10), vertical = esc.d(4)),
            ) { Texto(o.valor, 12.5f, color = p.tinta, familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1, modifier = Modifier.widthIn(max = esc.d(170))) }
            else -> Texto("›", 20f, color = p.tenue)
        }
    }
}

/** En la otra pantalla, mientras el menú está abierto: explica la sección y la opción elegida, en grande. */
@Composable
fun AyudaMenu(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val s = e.seccion
    val o = if (e.enOpciones) Diptico.opcionesSeccion(s).getOrNull(e.indice) else null
    val color = colorSeccion(s)
    Box(Modifier.fillMaxSize().background(p.fondo)) {
        Box(
            Modifier.align(Alignment.CenterEnd).size(esc.d(520))
                .background(Brush.radialGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent))),
        )
        BarraSuperior(Modifier.align(Alignment.TopStart), trabajos = e.trabajos)
        AnimatedContent(
            targetState = Triple(s, o?.titulo, o?.valor),
            transitionSpec = { (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 14 }) togetherWith fadeOut(tween(140)) },
            label = "ayuda",
            modifier = Modifier.fillMaxSize().padding(start = esc.d(36), end = esc.d(36), top = esc.d(60), bottom = esc.d(54)),
        ) { (sec, titulo, valor) ->
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
                    Etiqueta("Menú · ${sec.titulo}", color, 13f)
                    Texto(titulo ?: sec.titulo, if (esc.angosta) 30f else 38f, lineas = 2, interlineado = if (esc.angosta) 34f else 42f)
                    if (!valor.isNullOrEmpty()) {
                        Box(
                            Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.18f))
                                .padding(horizontal = esc.d(14), vertical = esc.d(6)),
                        ) { Texto(valor, 15f, color = p.tinta, lineas = 1) }
                    }
                    Detalle(
                        (if (titulo != null) Diptico.opcionesSeccion(sec).firstOrNull { it.titulo == titulo }?.ayuda else null)
                            ?.ifEmpty { null } ?: sec.ayuda,
                        17f, lineas = 4,
                    )
                }
                if (!esc.angosta) {
                    Spacer(Modifier.width(esc.d(28)))
                    Box(
                        Modifier.size(esc.d(170)).clip(CircleShape).background(color.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(esc.d(118)).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                            IconoDiptico(sec.icono, if (sec == Seccion.PORTADAS) Tinta else Color.White, 64f)
                        }
                    }
                }
            }
        }
        Pistas(
            "A" to "Cambiar", "B" to "Volver", "Start" to "Cerrar",
            modifier = Modifier.align(Alignment.BottomStart).padding(start = esc.d(36), bottom = esc.d(20)),
        )
    }
}

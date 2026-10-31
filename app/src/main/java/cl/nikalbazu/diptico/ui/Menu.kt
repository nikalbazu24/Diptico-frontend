package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Seccion

private fun colorSeccion(s: Seccion): Color = when (s) {
    Seccion.PANTALLAS -> Uva
    Seccion.APARIENCIA -> Color(0xFFFF5D8F)
    Seccion.MUSICA -> Coral
    Seccion.EMULADORES -> Cielo
    Seccion.PORTADAS -> Sol
    Seccion.BIBLIOTECA -> Color(0xFF2EC4A6)
    Seccion.SISTEMA -> Color(0xFF8E8AAE)
    Seccion.INICIO -> Color(0xFF00ACC1)
    Seccion.TEMAS -> Color(0xFFB388FF)
}

/**
 * El menú principal, en su propia pestaña de pantalla completa.
 * A la izquierda las secciones con su ícono; a la derecha las opciones de la sección.
 * Con [conAyuda] muestra también la explicación (cuando no hay otra pantalla para mostrarla).
 */
@Composable
fun PanelMenu(e: Estado, conAyuda: Boolean) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val angosta = esc.angosta
    val anchoSecciones = if (angosta) 150f else 196f
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Box(
            Modifier.align(Alignment.TopEnd).size(esc.d(320))
                .background(Brush.radialGradient(listOf(colorSeccion(e.seccion).copy(alpha = 0.16f), Color.Transparent))),
        )
        Column(Modifier.fillMaxSize().padding(start = esc.d(14), end = esc.d(14), top = esc.d(12), bottom = esc.d(10))) {
            Row(Modifier.fillMaxWidth().height(esc.d(40)), verticalAlignment = Alignment.CenterVertically) {
                MarcaLogo(14f)
                Spacer(Modifier.width(esc.d(10)))
                Texto("Menú", 22f)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(esc.d(36)).clip(CircleShape).background(p.superficie).clickable { Diptico.cerrarPanel() },
                    contentAlignment = Alignment.Center,
                ) { IconoDiptico(cl.nikalbazu.diptico.estado.Icono.CERRAR, p.tinta, 18f) }
            }
            Spacer(Modifier.height(esc.d(8)))
            Row(Modifier.fillMaxWidth().weight(1f)) {
                // Secciones
                val listaS = rememberLazyListState()
                LaunchedEffect(e.seccion) { listaS.animateScrollToItem((e.seccion.ordinal - 2).coerceAtLeast(0)) }
                LazyColumn(
                    state = listaS,
                    modifier = Modifier.width(esc.d(anchoSecciones)).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(esc.d(5)),
                ) {
                    itemsIndexed(Diptico.seccionesMenu()) { _, s ->
                        val activa = s == e.seccion
                        val conFoco = activa && !e.enOpciones
                        val fondo by animateColorAsState(
                            when {
                                conFoco -> colorSeccion(s)
                                activa -> p.superficie
                                else -> Color.Transparent
                            },
                            label = "seccion",
                        )
                        val tinta = if (conFoco) (if (s == Seccion.PORTADAS) Tinta else Color.White) else p.tinta
                        Row(
                            Modifier.fillMaxWidth().height(esc.d(44)).clip(RoundedCornerShape(esc.d(14))).background(fondo)
                                .clickable { Diptico.elegirSeccion(s) }
                                .padding(horizontal = esc.d(10)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconoDiptico(s.icono, tinta, 22f, acento = if (conFoco) tinta else colorSeccion(s))
                            Spacer(Modifier.width(esc.d(10)))
                            Texto(s.titulo, 15.5f, color = tinta, lineas = 1)
                        }
                    }
                }
                Spacer(Modifier.width(esc.d(12)))
                // Opciones
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(
                        targetState = e.seccion,
                        transitionSpec = { (fadeIn(tween(260)) + slideInHorizontally(tween(260)) { it / 12 }) togetherWith fadeOut(tween(120)) },
                        label = "opciones",
                        modifier = Modifier.weight(1f),
                    ) { s ->
                        val opciones = Diptico.opcionesSeccion(s)
                        val lista = rememberLazyListState()
                        LaunchedEffect(e.indice, e.enOpciones) {
                            if (e.enOpciones && opciones.isNotEmpty()) lista.animateScrollToItem((e.indice - 2).coerceAtLeast(0))
                        }
                        LazyColumn(
                            state = lista,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(esc.d(6)),
                            contentPadding = PaddingValues(bottom = esc.d(4)),
                        ) {
                            itemsIndexed(opciones) { i, o ->
                                TarjetaOpcion(o, activa = e.enOpciones && i == e.indice, alto = 56f) { Diptico.elegirIndice(i) }
                            }
                        }
                    }
                    if (conAyuda) {
                        val o = if (e.enOpciones) Diptico.opcionesSeccion(e.seccion).getOrNull(e.indice) else null
                        Spacer(Modifier.height(esc.d(6)))
                        Detalle(o?.ayuda?.ifEmpty { null } ?: e.seccion.ayuda, 13.5f, lineas = 2)
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            if (angosta) {
                Pistas("A" to "Elegir", "B" to "Volver", "L1 R1" to "Sección")
            } else {
                Pistas("↑ ↓" to "Moverte", "← →" to "Columna", "A" to "Elegir", "B" to "Volver", "L1 R1" to "Sección")
            }
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

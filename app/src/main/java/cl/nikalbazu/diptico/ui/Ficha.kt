package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Las ↑ ↓ desplazan el texto largo. */
@Composable
private fun DesplazarCon(indice: Int, scroll: ScrollState) {
    val px = with(LocalDensity.current) { LocalEscala.current.d(90f).toPx() }
    LaunchedEffect(indice) { scroll.animateScrollTo((indice * px).toInt().coerceAtMost(scroll.maxValue)) }
}

/** Textos de la pestaña Resumen o Historia, por secciones. */
@Composable
private fun TextoFicha(e: Estado, pestana: Int, tamTexto: Float) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val f = e.ficha
    when {
        e.cargandoFicha -> Row(verticalAlignment = Alignment.CenterVertically) {
            Girador(20f)
            Spacer(Modifier.width(esc.d(10)))
            Texto("Buscando en Wikipedia…", 16f)
        }
        f == null || f.vacia -> Column(verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
            Texto("No encontré la historia de este juego", 18f)
            Detalle("Necesito internet, y que el título se parezca al nombre real del juego. Puedes cambiar el título en Personalizar.", 14.5f)
        }
        pestana == 0 -> Column(verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
            Detalle(f.resumen, tamTexto, color = p.tinta)
            Detalle("Fuente: Wikipedia (${if (f.idioma == "es") "español" else "inglés"})", 12f)
        }
        else -> Column(verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
            val partes = listOf("Cómo se creó" to f.historia, "El juego" to f.juego, "Cómo le fue" to f.recepcion).filter { it.second.isNotBlank() }
            if (partes.isEmpty()) Detalle("Wikipedia no tiene la historia de su creación. Mira el Resumen o busca en Guías.", tamTexto)
            for ((t, texto) in partes) {
                Column(verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
                    Etiqueta(t, Coral, 12.5f)
                    Detalle(texto, tamTexto, color = p.tinta)
                }
            }
        }
    }
}

/** Datos rápidos del juego: tiempo jugado, última vez, favorito. */
@Composable
private fun Datos(d: DatosJuego?) {
    val esc = LocalEscala.current
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
        Chip(if ((d?.tiempo ?: 0) > 0) formatoTiempo(d!!.tiempo) else "Sin jugar")
        if ((d?.ultimo ?: 0) > 1) Chip("Última vez: " + SimpleDateFormat("d MMM", Locale("es")).format(Date(d!!.ultimo)))
        if (d?.favorito == true) Chip("♥ Favorito", Coral)
    }
}

@Composable
private fun Chip(texto: String, color: Color = LocalPaleta.current.tenue) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(Modifier.clip(RoundedCornerShape(50)).background(p.superficie).padding(horizontal = esc.d(10), vertical = esc.d(4))) {
        Texto(texto, 12.5f, color = color, familia = Nunito, lineas = 1)
    }
}

/** Ficha del juego: resumen, historia, guías y trucos, y personalización. Pestañas con L1/R1 o ← →. */
@Composable
fun PanelFicha(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val j = e.juegos.firstOrNull { it.clave == e.fichaDe } ?: return
    val c = Consolas.porId(j.consolaId)
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Box(Modifier.fillMaxWidth().height(esc.d(150)).background(Brush.verticalGradient(listOf(colorDe(j.clave).copy(alpha = 0.35f), Color.Transparent))))
        Column(Modifier.fillMaxSize().padding(start = esc.d(16), end = esc.d(16), top = esc.d(12), bottom = esc.d(10))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (c != null) CajaJuego(j, c, escala = 0.5f)
                Spacer(Modifier.width(esc.d(12)))
                Column(Modifier.weight(1f)) {
                    Etiqueta(c?.bonito ?: "", Coral, 11.5f)
                    Texto(j.titulo, 21f, lineas = 2, interlineado = 24f)
                    Spacer(Modifier.height(esc.d(4)))
                    Datos(e.datos[j.clave])
                }
            }
            Spacer(Modifier.height(esc.d(10)))
            // Pestañas
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                Diptico.PESTANAS_FICHA.forEachIndexed { i, t ->
                    val activa = i == e.pestanaFicha
                    val fondo by animateColorAsState(if (activa) Uva else p.superficie, label = "pestana")
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(fondo).clickable { Diptico.elegirPestanaFicha(i) }
                            .padding(horizontal = esc.d(if (esc.angosta) 9f else 14f), vertical = esc.d(7)),
                    ) {
                        Texto(if (esc.angosta && i == 2) "Guías" else t, 13.5f, color = if (activa) SobreUva else p.tinta, lineas = 1)
                    }
                }
            }
            Spacer(Modifier.height(esc.d(10)))
            AnimatedContent(
                targetState = e.pestanaFicha,
                transitionSpec = { (fadeIn(tween(250)) + slideInHorizontally(tween(280)) { if (targetState > initialState) it / 10 else -it / 10 }) togetherWith fadeOut(tween(120)) },
                label = "pestanaFicha",
                modifier = Modifier.weight(1f),
            ) { pestana ->
                if (pestana >= 2) {
                    val opciones = Diptico.opcionesFicha()
                    val lista = rememberLazyListState()
                    LaunchedEffect(e.indice) { if (opciones.isNotEmpty()) lista.animateScrollToItem((e.indice - 2).coerceAtLeast(0)) }
                    LazyColumn(state = lista, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        if (pestana == 2 && (e.datos[j.clave]?.notas ?: "").isNotBlank()) {
                            item {
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(16))).background(Sol.copy(alpha = 0.14f)).padding(esc.d(12))) {
                                    Detalle(e.datos[j.clave]!!.notas, 14.5f, color = p.tinta, lineas = 6)
                                }
                            }
                        }
                        itemsIndexed(opciones) { i, o -> TarjetaOpcion(o, i == e.indice, alto = 54f) { Diptico.elegirIndice(i) } }
                    }
                } else {
                    val scroll = rememberScrollState()
                    DesplazarCon(e.indice, scroll)
                    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                        TextoFicha(e, pestana, 15.5f)
                        Spacer(Modifier.height(esc.d(20)))
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("L1 R1" to "Pestaña", "↑ ↓" to "Moverte", "B" to "Cerrar")
        }
    }
}

/**
 * En la otra pantalla, mientras la ficha está abierta: el arte del juego y el texto en grande para leer cómodo.
 */
@Composable
fun LecturaFicha(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val j = e.juegos.firstOrNull { it.clave == e.fichaDe } ?: return
    Box(Modifier.fillMaxSize().background(p.fondo)) {
        Heroe(j, if (esc.angosta) 1f else 0.55f)
        Box(
            Modifier.fillMaxSize().background(
                if (esc.angosta) Brush.verticalGradient(listOf(p.fondo.copy(alpha = 0.75f), p.fondo))
                else Brush.horizontalGradient(0f to p.fondo, 0.5f to p.fondo, 0.85f to p.fondo.copy(alpha = 0.3f)),
            ),
        )
        BarraSuperior(Modifier.align(Alignment.TopStart), trabajos = e.trabajos)
        Column(
            Modifier.fillMaxHeight().widthIn(max = esc.d(470)).padding(start = esc.d(30), end = esc.d(20), top = esc.d(54), bottom = esc.d(16)),
        ) {
            Etiqueta("Ficha · ${Diptico.PESTANAS_FICHA[e.pestanaFicha]}", Coral, 12.5f)
            Texto(j.titulo, 30f, lineas = 2, interlineado = 33f)
            Spacer(Modifier.height(esc.d(10)))
            if (e.pestanaFicha <= 1) {
                val scroll = rememberScrollState()
                DesplazarCon(e.indice, scroll)
                Column(Modifier.weight(1f).verticalScroll(scroll)) { TextoFicha(e, e.pestanaFicha, 18f) }
            } else {
                val o = Diptico.opcionesFicha().getOrNull(e.indice)
                Detalle(
                    if (e.pestanaFicha == 2) "Tus notas y las mejores guías de internet. Elige una búsqueda y se abre en el navegador."
                    else "Cambia el título, las imágenes, el logo y hasta la música que suena con este juego.",
                    17f,
                )
                if (o != null) {
                    Spacer(Modifier.height(esc.d(14)))
                    Texto(o.titulo, 24f, color = p.acentoTexto, lineas = 2)
                    if (o.valor.isNotBlank()) Detalle(o.valor, 16f)
                }
                val notas = e.datos[j.clave]?.notas.orEmpty()
                if (e.pestanaFicha == 2 && notas.isNotBlank()) {
                    Spacer(Modifier.height(esc.d(14)))
                    Etiqueta("Mis trucos y notas", Sol, 12f)
                    Detalle(notas, 16f, color = p.tinta, lineas = 8)
                }
            }
        }
    }
}

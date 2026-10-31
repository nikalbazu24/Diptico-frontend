package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.MensajeChat

/** Tres puntitos mientras la IA piensa. */
@Composable
private fun Pensando() {
    val esc = LocalEscala.current
    val t = rememberInfiniteTransition(label = "pensando")
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(5)), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 3) {
            val a by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(450, delayMillis = i * 150), RepeatMode.Reverse), label = "p$i")
            Box(Modifier.size(esc.d(8)).alpha(a).clip(CircleShape).background(Uva))
        }
    }
}

@Composable
private fun Burbuja(m: MensajeChat) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val mia = m.rol == "yo"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mia) Arrangement.End else Arrangement.Start) {
        if (!mia) {
            Box(Modifier.size(esc.d(28)).clip(CircleShape).fondoBoton(Uva), contentAlignment = Alignment.Center) {
                IconoDiptico(Icono.ASISTENTE, SobreUva, 15f, acento = SobreUva)
            }
            Spacer(Modifier.width(esc.d(6)))
        }
        Box(
            Modifier.widthIn(max = esc.d(420)).clip(
                RoundedCornerShape(
                    topStart = esc.d(18), topEnd = esc.d(18),
                    bottomStart = esc.d(if (mia) 18f else 4f), bottomEnd = esc.d(if (mia) 4f else 18f),
                ),
            ).background(if (mia) Uva else p.superficie).padding(horizontal = esc.d(12), vertical = esc.d(8)),
        ) {
            Detalle(m.texto, 14.5f, color = if (mia) SobreUva else p.tinta)
        }
    }
}

/** Chat con el asistente: la conversación, sus botones y sugerencias. */
@Composable
fun PanelAsistente(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val elementos = remember(e) { Diptico.elementosAsistente() }
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Box(Modifier.align(Alignment.TopEnd).size(esc.d(320)).background(Brush.radialGradient(listOf(Uva.copy(alpha = 0.22f), Color.Transparent))))
        Column(Modifier.fillMaxSize().padding(start = esc.d(14), end = esc.d(14), top = esc.d(12), bottom = esc.d(10))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(esc.d(36)).clip(CircleShape).fondoBoton(Uva), contentAlignment = Alignment.Center) {
                    IconoDiptico(Icono.ASISTENTE, SobreUva, 20f, acento = SobreUva)
                }
                Spacer(Modifier.width(esc.d(10)))
                Column(Modifier.weight(1f)) {
                    Texto("Asistente", 20f, lineas = 1)
                    Detalle(if (e.pensando) "Pensando…" else "Con IA en la nube: no gasta memoria de tu consola", 12f, lineas = 1)
                }
            }
            Spacer(Modifier.height(esc.d(8)))
            val lista = rememberLazyListState()
            LaunchedEffect(e.chat.size, e.pensando) { if (e.chat.isNotEmpty()) lista.animateScrollToItem(e.chat.size) }
            LazyColumn(state = lista, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
                if (e.chat.isEmpty()) item {
                    Column(Modifier.padding(vertical = esc.d(6)), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        Texto("¡Hola! ¿En qué te ayudo?", 18f)
                        Detalle(
                            if (e.claveIa.isBlank()) "Para usarme necesitas una clave de IA (Claude). Elige \"Escribir una pregunta\" y te pido la clave."
                            else "Conozco tus juegos. Pídeme recomendaciones, trucos, o que cambie el tema o la vista.",
                            13.5f,
                        )
                    }
                }
                itemsIndexed(e.chat) { _, m -> Burbuja(m) }
                if (e.pensando) item { Row(Modifier.padding(start = esc.d(34), top = esc.d(4))) { Pensando() } }
                // Botones y sugerencias
                item {
                    Column(Modifier.padding(top = esc.d(4)), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        elementos.forEachIndexed { i, o ->
                            val activo = i == e.indice
                            val esAccion = o.valor != "sugerencia" && i > 0
                            Row(
                                Modifier.clip(RoundedCornerShape(50))
                                    .then(if (activo) Modifier.fondoBoton(Uva) else Modifier.background(if (esAccion) Coral.copy(alpha = 0.18f) else p.superficie.copy(alpha = 0.7f)))
                                    .border(esc.d(if (esAccion && !activo) 1.5f else 0f), if (esAccion && !activo) Coral else Color.Transparent, RoundedCornerShape(50))
                                    .clickable { if (activo) o.accion() else Diptico.elegirIndice(i) }
                                    .padding(horizontal = esc.d(12), vertical = esc.d(7)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                o.icono?.let { IconoDiptico(it, if (activo) SobreUva else p.tinta, 15f, acento = if (activo) SobreUva else Coral) }
                                Spacer(Modifier.width(esc.d(6)))
                                Texto(o.titulo, 13.5f, color = if (activo) SobreUva else p.tinta, lineas = 1)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("A" to "Elegir", "X" to "Escribir", "Y" to "Borrar chat", "B" to "Cerrar")
        }
    }
}

/** En la otra pantalla: la última respuesta, en grande para leer cómodo. */
@Composable
fun LecturaAsistente(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val ultima = e.chat.lastOrNull { it.rol == "ia" }
    val pregunta = e.chat.lastOrNull { it.rol == "yo" }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(p.fondo, Uva.copy(alpha = 0.18f), p.fondo)))) {
        BarraSuperior(Modifier.align(Alignment.TopStart), trabajos = e.trabajos)
        Column(
            Modifier.fillMaxSize().padding(start = esc.d(36), end = esc.d(36), top = esc.d(62), bottom = esc.d(24)),
            verticalArrangement = Arrangement.spacedBy(esc.d(10)),
        ) {
            Etiqueta("Asistente", Coral, 13f)
            if (pregunta != null) Detalle("“${pregunta.texto}”", 15f, lineas = 2)
            when {
                e.pensando -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Pensando()
                    Spacer(Modifier.width(esc.d(10)))
                    Texto("Pensando…", 20f, color = p.tenue)
                }
                ultima != null -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Texto(ultima.texto, if (ultima.texto.length > 260) 19f else 23f, interlineado = if (ultima.texto.length > 260) 25f else 30f)
                }
                else -> {
                    Texto("Pregúntame lo que quieras", 32f, lineas = 2)
                    Detalle("“¿Qué juego me recomiendas hoy?”, “Dame un truco para Zelda”, “Ponme un tema de Navidad”…", 17f)
                }
            }
        }
    }
}

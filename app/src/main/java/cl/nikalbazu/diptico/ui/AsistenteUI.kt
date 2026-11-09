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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import cl.nikalbazu.diptico.estado.CentroBisa
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Estado
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
private fun Burbuja(m: MensajeChat, e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val mia = m.rol == "yo"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mia) Arrangement.End else Arrangement.Start) {
        if (!mia) {
            cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(28).value)
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
            Column {
                Detalle(m.texto, 14.5f, color = if (mia) SobreUva else p.tinta)
                // Los juegos que propone DIPPO van en una carpetita: tócala y se abre con todos a mano.
                val juegos = m.acciones.filter { it.tipo in setOf("jugar", "ver", "ficha") }.mapNotNull { e.juegosPorClave[it.valor] }.distinctBy { it.clave }
                if (!mia && juegos.size >= 2) {
                    Spacer(Modifier.height(esc.d(8)))
                    CarpetaJuegosChat(juegos)
                }
            }
        }
    }
}

/**
 * La carpetita de juegos del chat: cerrada muestra las portadas apiladas; abierta, una grilla donde cada juego
 * se elige (queda marcado en tu estante) o se juega directo con su botón.
 */
@Composable
private fun CarpetaJuegosChat(juegos: List<cl.nikalbazu.diptico.data.Juego>) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    var abierta by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.animation.AnimatedContent(abierta, label = "carpeta chat") { abrir ->
        if (!abrir) Row(
            Modifier.clip(RoundedCornerShape(esc.d(14))).background(Uva.copy(alpha = 0.18f))
                .border(esc.d(1), Uva.copy(alpha = 0.4f), RoundedCornerShape(esc.d(14)))
                .clickable { abierta = true }.padding(esc.d(8)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(esc.d(58), esc.d(52))) {
                juegos.take(3).forEachIndexed { i, j ->
                    Box(Modifier.padding(start = esc.d(i * 9f), top = esc.d(i * 3f)).size(esc.d(36), esc.d(46))
                        .clip(RoundedCornerShape(esc.d(6))).border(esc.d(1), Color.White.copy(alpha = 0.3f), RoundedCornerShape(esc.d(6)))) {
                        ArteJuego(j, preferirArte = false, maxLado = 160, modifier = Modifier.fillMaxSize(), imagen = "portada")
                    }
                }
            }
            Spacer(Modifier.width(esc.d(10)))
            Column {
                Texto("${juegos.size} juegos para ti", 13.5f, color = p.tinta, lineas = 1)
                Detalle("Toca para abrir la carpeta", 11.5f, lineas = 1)
            }
        }
        else Column(
            Modifier.clip(RoundedCornerShape(esc.d(16))).background(Color.Black.copy(alpha = 0.25f)).padding(esc.d(8)),
            verticalArrangement = Arrangement.spacedBy(esc.d(6)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Texto("Juegos que te propongo", 13f, color = p.tinta, lineas = 1, modifier = Modifier.weight(1f))
                Texto("Cerrar", 12f, color = p.acentoTexto, lineas = 1, modifier = Modifier.clickable { abierta = false }.padding(esc.d(4)))
            }
            juegos.chunked(3).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    fila.forEach { j ->
                        Column(Modifier.width(esc.d(104))) {
                            Box(Modifier.size(esc.d(104), esc.d(132)).clip(RoundedCornerShape(esc.d(10)))
                                .clickable { Diptico.ejecutarAccion(cl.nikalbazu.diptico.estado.AccionChat("ver", j.clave, j.titulo)) }) {
                                ArteJuego(j, preferirArte = false, maxLado = 320, modifier = Modifier.fillMaxSize(), imagen = "portada")
                                // Jugar directo.
                                Box(Modifier.align(Alignment.BottomEnd).padding(esc.d(6)).size(esc.d(32)).clip(CircleShape).fondoBoton(Uva)
                                    .clickable { Diptico.ejecutarAccion(cl.nikalbazu.diptico.estado.AccionChat("jugar", j.clave, j.titulo)) },
                                    contentAlignment = Alignment.Center) { IconoDiptico(Icono.JUGAR, SobreUva, 14f) }
                            }
                            Texto(j.titulo, 11.5f, color = p.tinta, lineas = 2, traducir = false)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chat con DIPPO, pensado para escribir poco: abajo dos botones grandes (Hablar y Escribir)
 * y debajo respuestas y preguntas listas para tocar (o elegir con la cruceta).
 */
@Composable
fun PanelAsistente(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val elementos = remember(e) { Diptico.elementosAsistente(e) }
    val contexto = remember(e.sesion, e.juegos, e.seleccionado, e.dosDisponibles, e.unaPantalla) { CentroBisa.contexto(e) }
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Box(Modifier.align(Alignment.TopEnd).size(esc.d(320)).background(Brush.radialGradient(listOf(Uva.copy(alpha = 0.18f), Color.Transparent))))
        Column(Modifier.fillMaxSize().padding(start = esc.d(14), end = esc.d(14), top = esc.d(12), bottom = esc.d(8))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(42).value, cara = if (e.pensando) CaraDippo.PENSANDO else null)
                Spacer(Modifier.width(esc.d(10)))
                Column(Modifier.weight(1f)) {
                    Texto("DIPPO", 20f, lineas = 1)
                    Detalle(
                        if (e.pensando) e.pensandoQue.ifBlank { "Pensando…" } else when (Diptico.motorEfectivo(e)) {
                            "local" -> "IA de tu consola"
                            "gemini" -> "Gemini"
                            "openai" -> "ChatGPT"
                            else -> "Ayuda local"
                        },
                        12f, lineas = 1,
                    )
                }
                Box(
                    Modifier.size(esc.d(38)).clip(CircleShape).background(p.superficie)
                        .clickable(onClickLabel = "Cerrar DIPPO") { Diptico.cerrarPanel() },
                    contentAlignment = Alignment.Center,
                ) { IconoDiptico(Icono.CERRAR, p.tinta, 18f) }
            }
            Spacer(Modifier.height(esc.d(8)))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(14)))
                    .background(Uva.copy(alpha = .14f)).padding(horizontal = esc.d(11), vertical = esc.d(7)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconoDiptico(if (contexto.jugando) Icono.MANDO else Icono.BIBLIOTECA, Coral, 15f)
                Spacer(Modifier.width(esc.d(8)))
                Column(Modifier.weight(1f)) {
                    Detalle(if (contexto.jugando) "En partida" else "Contexto", 10.5f, lineas = 1)
                    Texto(contexto.titulo, 13f, lineas = 1, traducir = false)
                }
                if (contexto.pantallaLibre) IconoDiptico(Icono.PANTALLAS, p.acentoTexto, 16f)
            }
            Spacer(Modifier.height(esc.d(7)))
            val lista = rememberLazyListState()
            LaunchedEffect(e.chat.size, e.pensando) { if (e.chat.isNotEmpty()) lista.animateScrollToItem((e.chat.size - 1 + if (e.pensando) 1 else 0).coerceAtLeast(0)) }
            LazyColumn(state = lista, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
                if (e.chat.isEmpty()) item {
                    Column(Modifier.padding(vertical = esc.d(6)), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        Texto(if (contexto.jugando) "Aquí estoy, copiloto." else "Soy DIPPO. ¿Qué jugamos?", 20f)
                        Detalle(
                            if (Diptico.motorEfectivo(e) == "ninguno") "Puedo recuperar tus notas, sugerirte qué jugar y revisar cómo abre un juego. Elige una acción, habla o escribe."
                            else "Habla, escribe o elige una acción. Puedo ayudarte con el juego que ves en la tarjeta de contexto.",
                            13.5f,
                        )
                    }
                }
                itemsIndexed(e.chat) { _, m -> Burbuja(m, e) }
                if (e.pensando) item {
                    Row(Modifier.padding(start = esc.d(34), top = esc.d(4)), verticalAlignment = Alignment.CenterVertically) {
                        Pensando()
                        Spacer(Modifier.width(esc.d(8)))
                        Detalle(e.pensandoQue.ifBlank { "Pensando…" }, 12.5f, lineas = 1)
                    }
                }
            }
            Spacer(Modifier.height(esc.d(8)))
            // Hablar y Escribir, grandes.
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                elementos.take(2).forEachIndexed { i, o ->
                    val activo = i == e.indice
                    val voz = i == 0
                    Row(
                        Modifier.weight(if (voz) 1.3f else 1f).clip(RoundedCornerShape(esc.d(18)))
                            .then(if (activo) Modifier.fondoBoton(if (voz) Coral else Uva) else Modifier.background(if (voz) Coral.copy(alpha = 0.2f) else p.superficie))
                            .border(esc.d(if (activo) 2f else 0f), if (activo) Color.White else Color.Transparent, RoundedCornerShape(esc.d(18)))
                            .clickable { o.accion() }.padding(vertical = esc.d(10)),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        o.icono?.let { IconoDiptico(it, if (activo) Color.White else p.tinta, 18f, acento = if (activo) Color.White else Coral) }
                        Spacer(Modifier.width(esc.d(8)))
                        Texto(o.titulo, 15.5f, color = if (activo) Color.White else p.tinta, lineas = 1)
                    }
                }
            }
            // Respuestas y preguntas listas para tocar.
            val resto = elementos.drop(2)
            if (resto.isNotEmpty()) {
                Spacer(Modifier.height(esc.d(6)))
                val chips = rememberLazyListState()
                LaunchedEffect(e.indice) { if (e.indice >= 2) chips.mostrar(e.indice - 2) }
                androidx.compose.foundation.lazy.LazyRow(state = chips, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    items(resto.size) { k ->
                        val o = resto[k]
                        val activo = k + 2 == e.indice
                        val esAccion = o.valor != "sugerencia"
                        Row(
                            Modifier.widthIn(max = esc.d(220)).clip(RoundedCornerShape(esc.d(14)))
                                .then(if (activo) Modifier.fondoBoton(Uva) else Modifier.background(if (esAccion) Coral.copy(alpha = 0.18f) else p.superficie.copy(alpha = 0.8f)))
                                .clickable { Diptico.elegirIndice(k + 2) }
                                .padding(horizontal = esc.d(11), vertical = esc.d(8)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            o.icono?.let { IconoDiptico(it, if (activo) SobreUva else p.tinta, 14f, acento = if (activo) SobreUva else Coral) }
                            Spacer(Modifier.width(esc.d(6)))
                            Texto(o.titulo, 12.5f, color = if (activo) SobreUva else p.tinta, lineas = 2, interlineado = 15f)
                        }
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("A" to "Elegir", "L1" to "Hablar", "X" to "Escribir", "B" to "Cerrar")
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(44).value)
                Spacer(Modifier.width(esc.d(10)))
                Etiqueta("DIPPO · tu asistente", Coral, 13f)
            }
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


/** Acceso a DIPPO para el inicio: su cara animada y su nombre. */
@Composable
fun BotonBisa(modifier: Modifier = Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Row(
        modifier.clip(RoundedCornerShape(50)).background(p.superficie.copy(alpha = 0.9f))
            .border(esc.d(1.5f), Uva.copy(alpha = 0.5f), RoundedCornerShape(50))
            .clickable { Diptico.abrirAsistente() }.padding(start = esc.d(4), end = esc.d(12), top = esc.d(4), bottom = esc.d(4)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(30).value)
        Spacer(Modifier.width(esc.d(7)))
        Texto("DIPPO", 14f, lineas = 1)
    }
}


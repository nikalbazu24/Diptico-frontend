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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.*
import kotlinx.coroutines.delay

/*
 * «Partida en curso» con una sola pantalla (boceto «Jugando»): barra · volver al juego grande a la izquierda con tu
 * paso, y a la derecha DIPPO y las herramientas que en una consola de dos pantallas van en la de abajo · pie con pistas.
 * Cada parte en su franja; nada encima de nada.
 */

@Composable
fun PartidaEnCurso(e: Estado) {
    val esc = LocalEscala.current
    val s = e.sesion ?: return
    val juego = e.juegosPorClave[s.clave]
    val c = juego?.let { Consolas.porId(it.consolaId) }
    val color = c?.color ?: juego?.let { colorDe(it.clave) } ?: Uva
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var termica by remember { mutableStateOf(Termica.pruebaLectura) }
    LaunchedEffect(s.inicio, e.extra.termica) {
        if (!e.extra.termica) { termica = null; return@LaunchedEffect }
        while (true) {
            termica = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { Termica.leer(ctx) }.getOrNull() }
            delay(15_000)
        }
    }
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { ahora = System.currentTimeMillis(); delay(30_000) } }
    // Lo jugado hasta que volviste a Díptico (la pausa no cuenta).
    val hasta = if (e.extra.partidaPausa > 0L) e.extra.partidaPausa else ahora
    val min = ((hasta - s.inicio) / 60_000).toInt().coerceAtLeast(0)
    val sel = e.extra.partidaSel

    Column(Modifier.fillMaxSize().background(Color(0xFF0B0D17)).testTag("partida-en-curso")) {
        // 1 · Barra: la partida, cuánto llevas, temperatura, hora y batería.
        Row(
            Modifier.fillMaxWidth().height(esc.d(44)).background(Color(0xFF11131F)).padding(horizontal = esc.d(16)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF3EE08F)).padding(horizontal = esc.d(10), vertical = esc.d(3))) {
                Texto("PARTIDA EN CURSO", 11f, color = Color(0xFF0B0D17), peso = FontWeight.Black, familia = Nunito, lineas = 1)
            }
            Spacer(Modifier.width(esc.d(10)))
            Texto(juego?.titulo ?: "Tu juego", 14f, color = Color.White, peso = FontWeight.Black, lineas = 1, traducir = false, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(esc.d(8)))
            Texto(listOfNotNull(c?.nombre, if (min > 0) "$min min" else null).joinToString(" · "), 12.5f, color = Color(0xFFA9ADC4), lineas = 1, traducir = false)
            Spacer(Modifier.weight(1f))
            termica?.takeIf { it.grados > 0f }?.let { t ->
                Texto("${"%.0f".format(t.grados)} °C", 13f, color = if (t.nivel >= Termica.Nivel.CALIENTE) Color(0xFFFF7A59) else Color(0xFFFFB454), traducir = false)
                Spacer(Modifier.width(esc.d(12)))
            }
            RelojBateria()
        }
        // 2 · Cuerpo en dos columnas.
        Row(Modifier.fillMaxWidth().weight(1f).padding(esc.d(12)), horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
            Column(Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
                val forma = RoundedCornerShape(esc.d(22))
                Box(
                    Modifier.fillMaxWidth().weight(1f).clip(forma)
                        .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.35f), Color(0xFF1A1230))))
                        .then(if (sel == 0) Modifier.border(esc.d(3), Color.White, forma) else Modifier)
                        .clickable { Diptico.volverAlJuegoUnica() }
                        .testTag("partida-volver"),
                ) {
                    if (juego != null) {
                        ArteJuego(juego, preferirArte = true, maxLado = 960, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.2f to Color.Transparent, 1f to Color(0xE60B0D17))))
                    }
                    Column(Modifier.align(Alignment.BottomStart).padding(esc.d(20))) {
                        Texto("A", 13f, color = Color(0xFFFFD2BF), peso = FontWeight.Black, familia = Nunito, lineas = 1, traducir = false)
                        Texto("Volver al juego", 30f, color = Color.White, peso = FontWeight.Black, lineas = 1, sombra = true)
                        Texto("Sigue donde quedaste", 14f, color = Color(0xFFFFE2D6), lineas = 1)
                    }
                }
                TuPartida(e, s.clave)
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
                ConsejoDippo(e, s, juego, min, termica, (min / 2), null)
                HERRAMIENTAS_PARTIDA.chunked(2).forEachIndexed { f, fila ->
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                        fila.forEachIndexed { k, t ->
                            val i = 1 + f * 2 + k
                            val forma = RoundedCornerShape(esc.d(16))
                            Box(
                                Modifier.weight(1f).fillMaxHeight().clip(forma).background(Color(0xFF171B34))
                                    .then(if (sel == i) Modifier.border(esc.d(3), Color.White, forma) else Modifier)
                                    .clickable { Diptico.usarPartidaUnica(i) },
                                contentAlignment = Alignment.Center,
                            ) { Texto(t, 15f, color = Color.White, peso = FontWeight.Black, lineas = 1) }
                        }
                    }
                }
            }
        }
        // 3 · Pie con las pistas y «Terminar partida» a la vista.
        Row(
            Modifier.fillMaxWidth().height(esc.d(40)).background(Color(0xFF11131F)).padding(horizontal = esc.d(16)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Pistas("A" to "Usar", "Cruceta" to "Elegir", "B" to "Volver al juego", "Select" to "Terminar partida")
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.clip(RoundedCornerShape(50)).border(esc.d(1), Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                    .clickable { Diptico.terminarPartidaUnica() }.padding(horizontal = esc.d(12), vertical = esc.d(5)).testTag("partida-terminar"),
            ) { Texto("Terminar partida", 12.5f, color = Color(0xFFCFD2E6), peso = FontWeight.ExtraBold, lineas = 1) }
        }
    }
}

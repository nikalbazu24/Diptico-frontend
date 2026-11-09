package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import cl.nikalbazu.diptico.bisa.AvatarBisa
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import kotlinx.coroutines.delay

/** El saludo de DIPPO según la hora. */
internal fun saludoDippo(hora: Int): String = when (hora) {
    in 6..11 -> "¡Buenos días!"
    in 12..19 -> "¡Buenas tardes!"
    else -> "¡Buenas noches!"
}

/** La cara de DIPPO en el inicio según lo que pasa: contento al saludar, preocupado con poca batería, dormido de madrugada. */
internal fun caraDippoInicio(saludando: Boolean, bateria: Int, cargando: Boolean, hora: Int): CaraDippo? = when {
    saludando -> CaraDippo.FELIZ
    bateria in 0..15 && !cargando -> CaraDippo.PREOCUPADO
    hora in 1..5 -> CaraDippo.DURMIENDO
    else -> null
}

/**
 * DIPPO siempre a la vista en el inicio. Al llegar saluda con un globito, después propone una idea que se hace
 * tocándola, y se queda flotando. Tocarlo abre su conversación; mantenerlo apretado, el dictado.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DippoInicio(e: Estado, tam: Float, modifier: Modifier = Modifier) {
    if (!e.extra.dippoInicio || !e.configurado) return
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val bateria = rememberBateria()
    val cargando = rememberCargando()
    val hora = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    // Primero saluda, después propone una idea (seguir tu partida, volver a un juego olvidado…). Tocar el globito la hace.
    var fase by remember { mutableStateOf(0) }
    val idea = remember(e.juegos.size, e.datos.size) { cl.nikalbazu.diptico.estado.CentroBisa.ideaInicio(e) }
    LaunchedEffect(e.extra.dippoSaludo) {
        if (!e.extra.dippoSaludo) { fase = 0; return@LaunchedEffect }
        delay(600)
        fase = 1
        delay(2800)
        fase = if (idea != null) 2 else 0
        delay(7000)
        fase = 0
    }
    // Al ganar un logro, el globito lo anuncia (fase 3) unos segundos.
    LaunchedEffect(e.extra.dippoCelebra) {
        if (System.currentTimeMillis() - e.extra.dippoCelebra < 8000) { fase = 3; delay(7000); fase = 0 }
    }
    val saludando = fase == 1
    Row(modifier.testTag("dippo-inicio"), verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(
            fase != 0,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
        ) {
            Box(
                Modifier.padding(end = esc.d(6)).clip(RoundedCornerShape(esc.d(12)))
                    .background(when (fase) { 2 -> Coral.copy(alpha = 0.92f); 3 -> Sol.copy(alpha = 0.95f); else -> p.superficie.copy(alpha = 0.92f) })
                    .then(if (fase == 3) Modifier.clickable { fase = 0; Diptico.abrirGaleria("logros_dippo") } else if (fase == 2 && idea != null) Modifier.clickable { fase = 0; if (idea.second.tipo.startsWith("bisa_")) Diptico.abrirAsistente(); Diptico.ejecutarAccion(idea.second) } else Modifier)
                    .padding(horizontal = esc.d(10), vertical = esc.d(5)),
            ) {
                if (fase == 3) Texto("🏆 " + e.extra.dippoLogro, 12f, color = Color(0xFF2A1E05), lineas = 1, traducir = false)
                else if (fase == 2 && idea != null) Texto(idea.first, 12f, color = Color.White, lineas = 1, traducir = false)
                else Texto(saludoDippo(hora), 12f, color = p.tinta, lineas = 1)
            }
        }
        AvatarBisa(
            tam,
            Modifier.combinedClickable(onLongClick = { Diptico.hablarConBisa() }) { Diptico.abrirAsistente() },
            cara = if (e.pensando) CaraDippo.PENSANDO else caraDippoInicio(saludando || fase == 2, bateria, cargando, hora),
        )
    }
}

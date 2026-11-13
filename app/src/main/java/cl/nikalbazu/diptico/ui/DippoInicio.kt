package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt

import cl.nikalbazu.diptico.estado.*

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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
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
fun DippoInicio(e: Estado, tam: Float, modifier: Modifier = Modifier, lugar: String = "") {
    // Durante el recorrido o las novedades, DIPPO está al centro de la escena: aquí no se duplica.
    if (!e.extra.dippoInicio || !e.configurado || e.novedades >= 0) return
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
        // Si recién ganó un logro, primero lo celebra.
        if (System.currentTimeMillis() - e.extra.dippoCelebra < 8000) delay(7200)
        delay(600)
        fase = 1
        delay(2800)
        fase = if (idea != null) 2 else 0
        delay(7000)
        fase = 0
    }
    // Cada tanto, si está tranquilo, cuenta un truco escondido de Díptico (fase 4).
    var dato by remember { mutableStateOf("") }
    LaunchedEffect(e.extra.dippoDatos) {
        if (!e.extra.dippoDatos) return@LaunchedEffect
        delay(45_000)
        while (true) {
            if (fase == 0) {
                dato = Diptico.siguienteDatoDippo()
                fase = 4
                delay(11_000)
                if (fase == 4) fase = 0
            }
            delay(4 * 60_000L)
        }
    }
    // Al ganar un logro, el globito lo anuncia (fase 3) unos segundos.
    LaunchedEffect(e.extra.dippoCelebra) {
        if (System.currentTimeMillis() - e.extra.dippoCelebra < 8000) { fase = 3; delay(7000); fase = 0 }
    }
    val saludando = fase == 1
    val pantalla = pantallaActual()
    // El globito sale arriba de su cabeza, en la capa de Raiz (así no empuja ni tapa los accesos de abajo).
    val texto = when (fase) {
        4 -> "💡 " + cl.nikalbazu.diptico.estado.Idioma.t(dato)
        3 -> "🏆 " + e.extra.dippoLogro
        2 -> idea?.first
        1 -> saludoDippo(hora)
        else -> null
    }
    LaunchedEffect(fase, texto, pantalla) {
        if (texto == null) { DippoBurbuja.ocultar(); return@LaunchedEffect }
        val alToque: (() -> Unit)? = when {
            fase == 3 -> { { fase = 0; Diptico.abrirGaleria("logros_dippo") } }
            fase == 2 && idea != null -> { { fase = 0; if (idea.second.tipo.startsWith("bisa_")) Diptico.abrirAsistente(); Diptico.ejecutarAccion(idea.second) } }
            else -> null
        }
        DippoBurbuja.pantalla = pantalla
        DippoBurbuja.mostrar(DippoBurbuja.Mensaje(System.nanoTime(), texto, fase, alToque) { fase = 0 })
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { DippoBurbuja.ocultar() } }
    // DIPPO se mueve con el dedo a donde quieras y se queda ahí (por pantalla y lugar). Doble toque: vuelve a su sitio.
    val clave = "$pantalla-" + lugar.ifBlank { tam.toInt().toString() }
    var corrido by remember(clave) { mutableStateOf(if (lugar == "fijo") androidx.compose.ui.geometry.Offset.Zero else Diptico.dippoCorrido(clave).let { androidx.compose.ui.geometry.Offset(it.first, it.second) }) }
    var base by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var medida by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val vista = androidx.compose.ui.platform.LocalView.current
    Row(modifier.testTag("dippo-inicio"), verticalAlignment = Alignment.CenterVertically) {
        AvatarBisa(
            tam,
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset(corrido.x.roundToInt(), corrido.y.roundToInt()) }
                .then(if (lugar == "fijo") Modifier else Modifier.pointerInput(clave) {
                    detectDragGestures(onDragEnd = { Diptico.guardarDippoCorrido(clave, corrido.x, corrido.y) }) { cambio, d ->
                        cambio.consume()
                        val maxX = (vista.width - medida.width).toFloat()
                        val maxY = (vista.height - medida.height).toFloat()
                        corrido = androidx.compose.ui.geometry.Offset(
                            (corrido.x + d.x).coerceIn(-base.x, (maxX - base.x).coerceAtLeast(-base.x)),
                            (corrido.y + d.y).coerceIn(-base.y, (maxY - base.y).coerceAtLeast(-base.y)),
                        )
                    }
                })
                .onGloballyPositioned { c ->
                    val pos = c.positionInWindow()
                    base = pos - corrido
                    medida = c.size
                    DippoBurbuja.cabeza = androidx.compose.ui.geometry.Rect(pos.x, pos.y, pos.x + c.size.width, pos.y + c.size.height)
                }
                .combinedClickable(
                    onLongClick = { Diptico.hablarConBisa() },
                    onDoubleClick = { corrido = androidx.compose.ui.geometry.Offset.Zero; Diptico.guardarDippoCorrido(clave, 0f, 0f) },
                ) { Diptico.abrirAsistente() },
            cara = if (e.pensando) CaraDippo.PENSANDO else caraDippoInicio(saludando || fase == 2, bateria, cargando, hora),
        )
    }
}

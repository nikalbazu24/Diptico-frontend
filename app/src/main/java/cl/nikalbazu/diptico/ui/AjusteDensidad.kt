package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import cl.nikalbazu.diptico.estado.EscalaDensidades

/** Un dedo sigue desplazando; desde el segundo se consumen también los toques al soltar. */
@Composable
internal fun Modifier.ajustarDensidad(iniciar: () -> Unit, ajustar: (Float) -> Unit, terminar: (Boolean) -> Unit): Modifier {
    val alIniciar by rememberUpdatedState(iniciar)
    val alAjustar by rememberUpdatedState(ajustar)
    val alTerminar by rememberUpdatedState(terminar)
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var multiple = false
            var completo = false
            var cancelado = false
            try {
                do {
                    val evento = awaitPointerEvent(PointerEventPass.Initial)
                    val dedos = evento.changes.count { it.pressed }
                    // Compose expresa ACTION_CANCEL como liberaciones ya consumidas.
                    // Leerlo antes de consumir nosotros evita confundirlo con un cierre normal.
                    if (dedos == 0 && evento.changes.any { it.isConsumed }) cancelado = true
                    if (dedos >= 2) {
                        if (!multiple) { multiple = true; alIniciar() }
                        val zoom = evento.calculateZoom()
                        if (zoom.isFinite() && zoom > 0f) alAjustar(zoom)
                    }
                    if (multiple) evento.changes.forEach { it.consume() }
                } while (dedos > 0)
                completo = !cancelado
            } finally { if (multiple) alTerminar(completo) }
        }
    }
}

@Composable
internal fun Modifier.pellizcoCarpetas(ajustar: (Float) -> Unit, guardar: () -> Unit): Modifier =
    ajustarDensidad({}, ajustar) { completo -> if (completo) guardar() }

@Composable
internal fun ParadasDensidad(escala: EscalaDensidades, valor: Float, elegir: (Float) -> Unit, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val actual = escala.masCercana(valor)
    Row(modifier.fillMaxWidth().padding(horizontal = esc.d(8), vertical = esc.d(4)),
        horizontalArrangement = Arrangement.spacedBy(esc.d(5), Alignment.CenterHorizontally)) {
        escala.valores.forEach { parada ->
            val activa = parada == actual
            Box(Modifier.weight(1f).clip(RoundedCornerShape(50))
                .background(if (activa) p.acentoTexto else p.superficie)
                .testTag("densidad-${parada.nombre}").clickable { elegir(parada.escala) }
                .padding(horizontal = esc.d(4), vertical = esc.d(6)), contentAlignment = Alignment.Center) {
                Texto(parada.nombre, 10f, color = if (activa) p.fondo else p.tinta, lineas = 1)
            }
        }
    }
}

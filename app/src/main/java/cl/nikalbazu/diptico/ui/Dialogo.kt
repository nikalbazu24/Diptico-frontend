package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado

/**
 * Ventana de pregunta importante (enviar un reporte, desinstalar un juego…).
 * A acepta, B cancela. Si la pregunta pide [Dialogo.mantener], hay que mantener A (o el dedo)
 * hasta que se llene la barra: así lo que no se puede deshacer nunca pasa por un toque sin querer.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VentanaDialogo(e: Estado) {
    val d = e.dialogo ?: return
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val acento = if (d.peligro) Color(0xFFE5484D) else Uva
    val animar = LocalMovimiento.current
    val entra = remember(d.id) { Animatable(if (animar) .955f else 1f) }
    LaunchedEffect(d.id, animar) { entra.animateTo(1f, tween(if (animar) 240 else 0)) }
    // Avance de "mantener A": se llena mientras se mantiene, y vuelve a cero al soltar.
    val avance = remember(d.id) { Animatable(0f) }
    LaunchedEffect(e.dialogoMantener) {
        if (e.dialogoMantener > 0L) avance.animateTo(1f, tween(Diptico.MANTENER_MS.toInt(), easing = LinearEasing))
        else avance.animateTo(0f, tween(180))
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).sinCerrar(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .graphicsLayer { scaleX = entra.value; scaleY = entra.value; alpha = ((entra.value - .955f) / .045f).coerceIn(0f, 1f) }
                .widthIn(max = esc.d(520)).fillMaxWidth(0.9f)
                .superficieApp(p.fondo2, RoundedCornerShape(esc.d(28)), CapaCristal.PANEL)
                .padding(esc.d(22)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(esc.d(46)).clip(CircleShape).background(acento.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    IconoDiptico(d.icono, acento, 24f, acento = acento)
                }
                Spacer(Modifier.width(esc.d(14)))
                Texto(d.titulo, 20f, lineas = 2, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(esc.d(12)))
            Detalle(d.texto, 14f, lineas = 6)
            if (d.lineas.isNotEmpty()) {
                Spacer(Modifier.height(esc.d(12)))
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(14))).background(p.fondo).padding(esc.d(12)),
                    verticalArrangement = Arrangement.spacedBy(esc.d(4)),
                ) {
                    d.lineas.take(7).forEach { Detalle("·  $it", 12.5f, lineas = 1) }
                    if (d.lineas.size > 7) Detalle("…y ${d.lineas.size - 7} más", 12.5f, lineas = 1)
                }
            }
            Spacer(Modifier.height(esc.d(18)))
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                Box(
                    Modifier.weight(1f).height(esc.d(52)).clip(RoundedCornerShape(esc.d(16))).background(p.fondo)
                        .combinedClickable(onClick = { Diptico.responderDialogo(false) }),
                    contentAlignment = Alignment.Center,
                ) { Texto("B · ${d.no}", 15f, lineas = 1) }
                Box(
                    Modifier.weight(1.4f).height(esc.d(52)).clip(RoundedCornerShape(esc.d(16))).background(acento.copy(alpha = if (d.mantener) 0.35f else 1f))
                        .combinedClickable(
                            onClick = { if (!d.mantener) Diptico.responderDialogo(true) },
                            // Con el dedo: mantener presionado también confirma.
                            onLongClick = { if (d.mantener) Diptico.responderDialogo(true) },
                        ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (d.mantener) Box(Modifier.fillMaxHeight().fillMaxWidth(avance.value).background(acento))
                    Texto(
                        if (d.mantener) "Mantén A · ${d.si}" else "A · ${d.si}", 15f, color = Color.White, lineas = 1,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import cl.nikalbazu.diptico.estado.Opcion
import kotlin.math.roundToInt

@Composable
internal fun ControlGlass(o: Opcion, color: Color) {
    val rango = o.rango ?: return
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(16))).background(p.superficie).padding(esc.d(10))) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Texto(o.titulo, 14f, modifier = Modifier.weight(1f), lineas = 1)
            Texto(o.valor, 12f, color = color)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(esc.d(40)).semantics { contentDescription = "Reducir ${o.titulo}" }.clickable { o.menos?.invoke() }, contentAlignment = Alignment.Center) { Texto("−", 20f) }
            Slider(rango.valor.toFloat(), { rango.alCambiar(it.roundToInt()) }, valueRange = 0f..100f,
                modifier = Modifier.weight(1f).testTag(o.titulo).semantics { contentDescription = o.titulo },
                colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color))
            Box(Modifier.size(esc.d(40)).semantics { contentDescription = "Aumentar ${o.titulo}" }.clickable { o.accion() }, contentAlignment = Alignment.Center) { Texto("+", 20f) }
        }
    }
}

package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight

/** Lo que mide el medidor en un segundo: cuadros dibujados y el cuadro más lento (en milisegundos). */
data class Medicion(val fps: Int, val peorMs: Int)

/** Resume un segundo de cuadros (sus duraciones en ms) en una [Medicion]. */
fun medir(duracionesMs: List<Float>): Medicion {
    if (duracionesMs.isEmpty()) return Medicion(0, 0)
    val total = duracionesMs.sum().coerceAtLeast(1f)
    return Medicion((duracionesMs.size * 1000f / total).toInt(), duracionesMs.max().toInt())
}

/**
 * Medidor de fluidez: una pastilla chica en la esquina de abajo a la derecha con los cuadros por segundo de esta
 * pantalla y el cuadro más lento del último segundo. Verde = fluido, amarillo = se nota, rojo = tirones.
 */
@Composable
fun MedidorFps(visible: Boolean) {
    if (!visible) return
    val esc = LocalEscala.current
    var m by remember { mutableStateOf(Medicion(0, 0)) }
    LaunchedEffect(Unit) {
        val cuadros = ArrayList<Float>(240)
        var anterior = 0L
        var desde = 0L
        while (true) {
            withFrameNanos { t ->
                if (anterior != 0L) cuadros += (t - anterior) / 1_000_000f
                if (desde == 0L) desde = t
                anterior = t
                if (t - desde >= 1_000_000_000L) {
                    m = medir(cuadros)
                    cuadros.clear()
                    desde = t
                }
            }
        }
    }
    val color = when {
        m.fps == 0 -> Color(0xFFA5A8BD)
        m.fps >= 55 && m.peorMs <= 34 -> Color(0xFF23C486)
        m.fps >= 40 -> Color(0xFFFFC94D)
        else -> Color(0xFFFF5A5F)
    }
    Box(Modifier.fillMaxSize().padding(esc.d(6)), contentAlignment = Alignment.BottomEnd) {
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(Color(0xCC0B0C16)).padding(horizontal = esc.d(8), vertical = esc.d(3)).testTag("medidor-fps"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(5)),
        ) {
            Box(Modifier.size(esc.d(7)).clip(CircleShape).background(color))
            Texto("${m.fps} fps · ${m.peorMs} ms", 10.5f, color = Color.White, peso = FontWeight.ExtraBold, lineas = 1, traducir = false)
        }
    }
}

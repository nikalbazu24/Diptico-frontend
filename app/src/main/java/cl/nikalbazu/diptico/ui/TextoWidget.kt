package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Ajusta antes de dibujar: sin fotogramas de texto cortado ni ciclos de recomposición por onTextLayout. */
@Composable
internal fun TextoWidget(texto: String, modifier: Modifier, estilo: TextStyle, lineas: Int, alinear: TextAlign?) {
    val medidor = rememberTextMeasurer()
    var abierto by remember(texto) { mutableStateOf(false) }
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    BoxWithConstraints(modifier) {
        val ancho = constraints.maxWidth
        val alto = constraints.maxHeight
        val ajustado = remember(texto, estilo, lineas, ancho, alto, medidor) {
            var resultado = estilo
            var escala = 1f
            // Las etiquetas largas pueden ocupar dos líneas; los párrafos conservan su distribución.
            val cantidad = if (lineas == 1 && texto.contains(" ")) 2 else lineas
            while (escala >= .66f) {
                resultado = estilo.copy(fontSize = estilo.fontSize * escala,
                    lineHeight = if (estilo.lineHeight == TextUnit.Unspecified) TextUnit.Unspecified else estilo.lineHeight * escala)
                val medida = medidor.measure(texto, resultado, maxLines = cantidad,
                    constraints = Constraints(maxWidth = ancho, maxHeight = alto))
                if (!medida.hasVisualOverflow) break
                escala -= .05f
            }
            val final = medidor.measure(texto, resultado, maxLines = if (lineas == 1 && texto.contains(" ")) 2 else lineas,
                constraints = Constraints(maxWidth = ancho, maxHeight = alto))
            resultado to final.hasVisualOverflow
        }
        Row(Modifier.then(if (ajustado.second) Modifier.clickable(onClickLabel = "Leer texto completo") { abierto = true } else Modifier),
            verticalAlignment = Alignment.CenterVertically) {
            Text(texto, style = ajustado.first, maxLines = if (lineas == 1 && texto.contains(" ")) 2 else lineas,
                modifier = if (ajustado.second) Modifier.weight(1f, fill = false) else Modifier,
                textAlign = alinear, overflow = TextOverflow.Ellipsis)
            if (ajustado.second) Text("↗", color = p.acentoTexto, fontSize = esc.s(11))
        }
    }
    if (abierto) Dialog(onDismissRequest = { abierto = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth(.90f).widthIn(max = esc.d(520))
            .background(p.fondo2, RoundedCornerShape(esc.d(24))).padding(esc.d(20))) {
            Text("Texto completo", color = p.tinta, fontSize = esc.s(18))
            Spacer(Modifier.height(esc.d(12)))
            Text(texto, color = p.tinta, fontSize = esc.s(16),
                modifier = Modifier.fillMaxWidth().heightIn(max = esc.d(280)).verticalScroll(rememberScrollState()))
            Spacer(Modifier.height(esc.d(14)))
            Text("Cerrar", color = p.acentoTexto, fontSize = esc.s(15), modifier = Modifier.align(Alignment.End).clickable { abierto = false }.padding(esc.d(8)))
        }
    }
}

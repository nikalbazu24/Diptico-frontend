package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Idioma

/**
 * Acceso pequeño de recarga manual para los distintos inicios.
 * El escaneo real ya se realiza en segundo plano y se comparte entre pantallas.
 */
@Composable
fun BotonRecargarJuegos(e: Estado, tam: Float = 34f, estiloColeccion: Boolean = false) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val col = LocalColeccion.current
    val colorIcono = if (estiloColeccion) col.texto else p.tenue
    val descripcion = Idioma.t(if (e.escaneando) "Actualizando juegos" else "Actualizar juegos")
    Box(
        Modifier.size(esc.d(tam))
            .clip(CircleShape)
            .then(
                if (estiloColeccion) Modifier
                    .background(col.texto.copy(alpha = .07f))
                    .border(1.dp, col.borde, CircleShape)
                else Modifier.background(p.superficie.copy(alpha = .55f))
            )
            .semantics { contentDescription = descripcion }
            .clickable(enabled = !e.escaneando, onClickLabel = Idioma.t("Actualizar juegos")) {
                Diptico.recargarJuegosDesdeInicio()
            }
            .testTag("recargar-juegos"),
        contentAlignment = Alignment.Center,
    ) {
        if (e.escaneando) {
            Girador(tam * .49f, colorIcono)
        } else {
            // La línea base del glifo ↻ lo desplazaba visualmente hacia abajo.
            // Un dibujo vectorial mantiene la flecha centrada en cualquier fuente/escala.
            Canvas(Modifier.size(esc.d(tam * .52f)).testTag("recargar-icono")) {
                val ancho = size.width
                val alto = size.height
                val radio = minOf(ancho, alto) * .31f
                val centro = Offset(ancho / 2f, alto / 2f)
                val trazo = minOf(ancho, alto) * .105f
                drawArc(
                    color = colorIcono,
                    startAngle = 65f,
                    sweepAngle = 290f,
                    useCenter = false,
                    topLeft = Offset(centro.x - radio, centro.y - radio),
                    size = Size(radio * 2, radio * 2),
                    style = Stroke(width = trazo, cap = StrokeCap.Round),
                )
                val punta = Offset(centro.x + radio * .996f, centro.y - radio * .087f)
                drawLine(
                    colorIcono,
                    Offset(punta.x - ancho * .17f, punta.y - alto * .01f),
                    punta, strokeWidth = trazo, cap = StrokeCap.Round,
                )
                drawLine(
                    colorIcono,
                    Offset(punta.x + ancho * .005f, punta.y - alto * .17f),
                    punta, strokeWidth = trazo, cap = StrokeCap.Round,
                )
            }
        }
    }
}

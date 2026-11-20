package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado

/**
 * Acceso pequeño de recarga manual para los distintos inicios.
 * El escaneo real ya se realiza en segundo plano y se comparte entre pantallas.
 */
@Composable
fun BotonRecargarJuegos(e: Estado, tam: Float = 34f) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val descripcion = if (e.escaneando) "Actualizando juegos" else "Actualizar juegos"
    Box(
        Modifier.size(esc.d(tam))
            .clip(CircleShape)
            .background(p.superficie.copy(alpha = .55f))
            .semantics { contentDescription = descripcion }
            .clickable(enabled = !e.escaneando, onClickLabel = "Actualizar juegos") {
                Diptico.recargarJuegosDesdeInicio()
            }
            .testTag("recargar-juegos"),
        contentAlignment = Alignment.Center,
    ) {
        if (e.escaneando) Girador(tam * .49f, p.tenue)
        else Texto("↻", tam * .66f, color = p.tenue, lineas = 1, traducir = false)
    }
}

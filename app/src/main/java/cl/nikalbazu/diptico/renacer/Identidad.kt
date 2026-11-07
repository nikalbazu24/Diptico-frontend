package cl.nikalbazu.diptico.renacer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.estado.Tema
import cl.nikalbazu.diptico.estado.TemaComunidad
import cl.nikalbazu.diptico.estado.Temas
import cl.nikalbazu.diptico.ui.formaTema
import java.time.LocalDate

/** La elección sigue siendo válida aunque cambie la descripción o la música del catálogo. */
internal val TemaComunidad.claveVisual: String get() = pistaLocal?.substringBeforeLast('.') ?: tema.id

/** Los temas de Díptico son la fuente de colores, formas, letra, tramas y adornos. */
internal object IdentidadRenacer {
    val base = Tema(
        id = "renacer", nombre = "Díptico", acento = 0xFF72F5D0, acento2 = 0xFF7B9DFF, acento3 = 0xFFFF8CAF,
        base = "oscuro", fondo = 0xFF0B1020, fuente = "sans", forma = "bisel", trama = "reticula",
        descripcion = "Arte de tus juegos, consolas y controles a la vista.",
    )

    fun tema(e: Salon, hoy: LocalDate = LocalDate.now()): Tema {
        if (e.calendario) Temas.deHoy(hoy)?.let { return it.tema }
        return e.temas.firstOrNull { it.claveVisual == e.temaId || it.tema.id == e.temaId }?.tema ?: base
    }
}

internal val LocalTemaRenacer = staticCompositionLocalOf { IdentidadRenacer.base }

@Composable
internal fun marcoRenacer(radio: Int = 16): Shape {
    val dp = radio.dp
    return formaTema(dp, 0, RoundedCornerShape(dp), LocalTemaRenacer.current.forma)
}

/** También se usa el acento como texto y debe distinguirse del fondo. */
internal fun acentoLegible(acento: Color, fondo: Color): Color {
    val destino = if (fondo.luminance() > .179f) Color.Black else Color.White
    var ajustado = acento
    repeat(16) {
        val claro = maxOf(ajustado.luminance(), fondo.luminance())
        val oscuro = minOf(ajustado.luminance(), fondo.luminance())
        if ((claro + .05f) / (oscuro + .05f) >= 4.5f) return ajustado
        ajustado = lerp(ajustado, destino, .12f)
    }
    return destino
}

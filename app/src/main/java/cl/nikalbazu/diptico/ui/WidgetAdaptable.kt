package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cl.nikalbazu.diptico.estado.Widget

internal val LocalTextoWidget = staticCompositionLocalOf { false }

internal data class PresentacionWidget(val columnas: Int, val filas: Int, val escala: Float)

/** Usa el espacio real, además de las casillas: la misma grilla puede vivir en un móvil o una Thor. */
internal fun presentarWidget(w: Widget, ancho: Float, alto: Float): PresentacionWidget {
    val columnas = minOf(w.an, when { ancho < 100f -> 1; ancho < 210f -> 2; ancho < 300f -> 3; else -> 4 })
    val filas = minOf(w.al, if (alto < 100f) 1 else if (alto < 185f) 2 else 3)
    val minimoAncho = if (columnas == 1) 52f else if (columnas == 2) 135f else 205f
    val minimoAlto = if (filas == 1) 50f else 105f
    val escala = minOf(w.escala.coerceIn(.5f, 2f), ancho / minimoAncho, alto / minimoAlto).coerceAtLeast(.25f)
    return PresentacionWidget(columnas.coerceAtLeast(1), filas.coerceAtLeast(1), escala)
}

@Composable
internal fun WidgetAdaptable(w: Widget, contenido: @Composable (Int, Int) -> Unit) {
    val esc = LocalEscala.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val presentacion = presentarWidget(w, maxWidth.value / esc.factor, maxHeight.value / esc.factor)
        CompositionLocalProvider(LocalTextoWidget provides true,
            LocalEscala provides Escala(esc.factor * presentacion.escala, esc.ancho, esc.texto)) {
            contenido(presentacion.columnas, presentacion.filas)
        }
    }
}

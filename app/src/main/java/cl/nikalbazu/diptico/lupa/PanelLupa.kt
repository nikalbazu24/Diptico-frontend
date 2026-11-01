package cl.nikalbazu.diptico.lupa

import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.onSizeChanged
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.ui.Coral
import cl.nikalbazu.diptico.ui.Detalle
import cl.nikalbazu.diptico.ui.LocalEscala
import cl.nikalbazu.diptico.ui.SobreUva
import cl.nikalbazu.diptico.ui.Texto
import cl.nikalbazu.diptico.ui.Uva
import cl.nikalbazu.diptico.ui.fondoBoton
import kotlinx.coroutines.delay

/** Zonas típicas (centro x, centro y, ancho): dónde suelen estar el minimapa, la vida o los subtítulos. */
val ZONAS_LUPA = listOf(
    "Arriba izq." to floatArrayOf(0.11f, 0.19f, 0.2f),
    "Arriba der." to floatArrayOf(0.89f, 0.19f, 0.2f),
    "Abajo izq." to floatArrayOf(0.11f, 0.81f, 0.2f),
    "Abajo der." to floatArrayOf(0.89f, 0.81f, 0.2f),
    "Centro" to floatArrayOf(0.5f, 0.5f, 0.3f),
    "Subtítulos" to floatArrayOf(0.5f, 0.88f, 0.7f),
)

/**
 * Arma la zona con la misma forma que la pantalla de abajo, así llena la vista sin bordes ni deformarse.
 * [cx], [cy] centro y [w] ancho, en fracciones de la pantalla de arriba; [aspecto] = ancho/alto de la vista de la lupa.
 */
fun zonaConForma(cx: Float, cy: Float, w: Float, aspecto: Float): RectF {
    val W = LupaServicio.ancho.toFloat()
    val H = LupaServicio.alto.toFloat()
    var ancho = w.coerceIn(0.04f, 1f)
    var alto = ancho * W / aspecto / H
    if (alto > 1f) { ancho /= alto; alto = 1f }
    val x = (cx - ancho / 2).coerceIn(0f, 1f - ancho)
    val y = (cy - alto / 2).coerceIn(0f, 1f - alto)
    return RectF(x, y, x + ancho, y + alto)
}

/**
 * La lupa en la pantalla de abajo: muestra ampliada la zona elegida de la de arriba, llenando la pantalla sin deformarse.
 * Tocar muestra los controles; − y + cambian el zoom; "Ajustar zona" muestra la pantalla entera para mover el cuadro.
 */
@Composable
fun PanelLupa(e: Estado, clave: String) {
    val esc = LocalEscala.current
    val guardada = remember(clave, e.datos[clave]?.zonaLupa) { Diptico.zonaLupa(clave) }
    var aspecto by remember { mutableStateOf(1.15f) }
    var zona by remember(clave) { mutableStateOf(guardada) }
    var editando by remember { mutableStateOf(false) }
    var controles by remember { mutableStateOf(true) }
    var vista by remember { mutableStateOf<VistaLupa?>(null) }
    LaunchedEffect(controles, editando) { if (controles && !editando) { delay(3500); controles = false } }
    // Cuando se conoce la forma de la vista (o el tamaño real de arriba), la zona se ajusta a esa forma.
    LaunchedEffect(aspecto, e.lupaActiva) { zona = zonaConForma(zona.centerX(), zona.centerY(), zona.width(), aspecto) }
    fun zoom(f: Float) {
        zona = zonaConForma(zona.centerX(), zona.centerY(), zona.width() / f, aspecto)
        Diptico.guardarZonaLupa(clave, zona)
        controles = true
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { VistaLupa(it).also { v -> vista = v } },
            update = { v -> v.refrescar(); v.zona = if (editando) null else zona },
            modifier = Modifier.fillMaxSize().onSizeChanged { if (it.height > 0) aspecto = it.width.toFloat() / it.height },
        )
        if (editando) {
            // Cuadro de la zona encima de la pantalla entera: un dedo lo mueve, dos dedos cambian el zoom.
            Canvas(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTransformGestures { _, mov, z, _ ->
                        val v = vista ?: return@detectTransformGestures
                        val r = v.rectoCompleto()
                        if (r.width() <= 0f) return@detectTransformGestures
                        val actual = zona
                        zona = zonaConForma(actual.centerX() + mov.x / r.width(), actual.centerY() + mov.y / r.height(), actual.width() / z, aspecto)
                    }
                },
            ) {
                val v = vista ?: return@Canvas
                val r = v.rectoCompleto()
                val x = r.left + zona.left * r.width()
                val y = r.top + zona.top * r.height()
                val w = zona.width() * r.width()
                val h = zona.height() * r.height()
                drawRect(Color.Black.copy(alpha = 0.45f), Offset(0f, 0f), Size(size.width, y))
                drawRect(Color.Black.copy(alpha = 0.45f), Offset(0f, y + h), Size(size.width, size.height - y - h))
                drawRect(Color.Black.copy(alpha = 0.45f), Offset(0f, y), Size(x, h))
                drawRect(Color.Black.copy(alpha = 0.45f), Offset(x + w, y), Size(size.width - x - w, h))
                drawRoundRect(Coral, Offset(x, y), Size(w, h), CornerRadius(10f), style = Stroke(5f))
            }
        } else {
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { controles = !controles } })
        }

        AnimatedVisibility(controles || editando, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
            Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(esc.d(10))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Texto(
                        if (editando) "Mueve el cuadro con un dedo y agrándalo con dos" else "Lupa · zoom ${"%.1f".format(1f / zona.width())}×",
                        13f, color = Color.White, lineas = 1, modifier = Modifier.weight(1f),
                    )
                    BotonLupa("−") { zoom(0.8f) }
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(start = esc.d(6)))
                    BotonLupa("+") { zoom(1.25f) }
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = esc.d(6)),
                    horizontalArrangement = Arrangement.spacedBy(esc.d(6)),
                ) {
                    BotonLupa(if (editando) "Listo" else "Ajustar zona", activo = editando) {
                        if (editando) Diptico.guardarZonaLupa(clave, zona)
                        editando = !editando
                    }
                    ZONAS_LUPA.forEach { (n, z) -> BotonLupa(n) { zona = zonaConForma(z[0], z[1], z[2], aspecto); Diptico.guardarZonaLupa(clave, zona) } }
                    BotonLupa("Apagar lupa") { Diptico.alternarLupaJuego(clave, false) }
                }
            }
        }
        if (!LupaServicio.activa) {
            Detalle("Encendiendo la lupa…", 14f, color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun BotonLupa(texto: String, activo: Boolean = false, accion: () -> Unit) {
    val esc = LocalEscala.current
    Box(
        Modifier.clip(RoundedCornerShape(50)).then(if (activo) Modifier.fondoBoton(Uva) else Modifier.background(Color.White.copy(alpha = 0.16f)))
            .clickable { accion() }.padding(horizontal = esc.d(12), vertical = esc.d(7)),
    ) { Texto(texto, 12.5f, color = if (activo) SobreUva else Color.White, lineas = 1) }
}

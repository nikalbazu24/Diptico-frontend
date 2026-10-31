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

/** Zonas típicas: dónde suelen estar el minimapa, la vida o los subtítulos. */
val ZONAS_LUPA = listOf(
    "Arriba izq." to RectF(0f, 0f, 0.25f, 0.33f),
    "Arriba der." to RectF(0.75f, 0f, 1f, 0.33f),
    "Abajo izq." to RectF(0f, 0.67f, 0.25f, 1f),
    "Abajo der." to RectF(0.75f, 0.67f, 1f, 1f),
    "Centro" to RectF(0.3f, 0.3f, 0.7f, 0.7f),
    "Subtítulos" to RectF(0.1f, 0.78f, 0.9f, 1f),
)

/**
 * La lupa en la pantalla de abajo: muestra ampliada la zona elegida de la de arriba.
 * Tocar muestra los controles; "Ajustar zona" muestra la pantalla entera para mover y achicar el cuadro con los dedos.
 */
@Composable
fun PanelLupa(e: Estado, clave: String) {
    val esc = LocalEscala.current
    val guardada = remember(clave, e.datos[clave]?.zonaLupa) { Diptico.zonaLupa(clave) }
    var zona by remember(clave) { mutableStateOf(guardada) }
    var editando by remember { mutableStateOf(false) }
    var controles by remember { mutableStateOf(true) }
    var vista by remember { mutableStateOf<VistaLupa?>(null) }
    LaunchedEffect(controles, editando) { if (controles && !editando) { delay(3500); controles = false } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { VistaLupa(it).also { v -> vista = v } },
            update = { v -> v.zona = if (editando) null else zona },
            modifier = Modifier.fillMaxSize(),
        )
        if (editando) {
            // Cuadro de la zona encima de la pantalla entera: un dedo lo mueve, dos dedos lo agrandan o achican.
            Canvas(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTransformGestures { _, mov, z, _ ->
                        val v = vista ?: return@detectTransformGestures
                        val r = v.rectoCompleto()
                        if (r.width() <= 0f) return@detectTransformGestures
                        val dx = mov.x / r.width()
                        val dy = mov.y / r.height()
                        val actual = zona
                        val w = (actual.width() / z).coerceIn(0.06f, 1f)
                        val h = (actual.height() / z).coerceIn(0.06f, 1f)
                        val cx = (actual.centerX() + dx).coerceIn(w / 2, 1 - w / 2)
                        val cy = (actual.centerY() + dy).coerceIn(h / 2, 1 - h / 2)
                        zona = RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
                    }
                },
            ) {
                val v = vista ?: return@Canvas
                val r = v.rectoCompleto()
                val x = r.left + zona.left * r.width()
                val y = r.top + zona.top * r.height()
                val w = zona.width() * r.width()
                val h = zona.height() * r.height()
                // Oscurece lo de afuera.
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
            Column(
                Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(esc.d(10)),
            ) {
                Texto(if (editando) "Mueve el cuadro con un dedo y agrándalo con dos" else "Lupa · toca para ocultar estos botones", 13f, color = Color.White, lineas = 1)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = esc.d(6)),
                    horizontalArrangement = Arrangement.spacedBy(esc.d(6)),
                ) {
                    BotonLupa(if (editando) "Listo" else "Ajustar zona", activo = editando) {
                        if (editando) Diptico.guardarZonaLupa(clave, zona)
                        editando = !editando
                    }
                    ZONAS_LUPA.forEach { (n, r) -> BotonLupa(n) { zona = RectF(r); Diptico.guardarZonaLupa(clave, zona) } }
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

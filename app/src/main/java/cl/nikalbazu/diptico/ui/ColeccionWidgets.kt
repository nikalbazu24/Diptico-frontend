package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/*
 * Colección viva · widgets (bocetos 7a, 7b y 7c). Cada widget toma los colores, el radio y la letra del tema.
 */

/** Los widgets de Colección viva, en el orden del boceto. (id, nombre, tamaño, qué muestra) */
val WIDGETS_CV = listOf(
    WidgetCV("continuar", "Continuar", 2, 2, "El juego que dejaste a medias"),
    WidgetCV("semana", "Tu semana", 2, 1, "Horas jugadas por día"),
    WidgetCV("dippo", "DIPPO", 2, 1, "Una idea para hoy y preguntas rápidas"),
    WidgetCV("hoy", "¿Qué juego hoy?", 1, 1, "Elige por ti según tu ánimo"),
    WidgetCV("logro", "Logro más cercano", 1, 1, "El que te falta poco"),
    WidgetCV("equipo", "Equipo", 1, 1, "Temperatura, batería y espacio"),
    WidgetCV("reloj", "Reloj", 1, 1, "La hora y la fecha en grande"),
    WidgetCV("favoritos", "Tus favoritos", 3, 1, "Repisa con tus cajas"),
    WidgetCV("extrana", "Te extraña", 1, 1, "Juegos sin tocar hace semanas"),
    WidgetCV("recuerdo", "Hace un año", 2, 1, "Lo que hacías hace un tiempo"),
)

data class WidgetCV(val id: String, val nombre: String, val an: Int, val al: Int, val detalle: String)


/** Acomoda los widgets en la cuadrícula de 6×3, de izquierda a derecha y de arriba abajo, donde quepan. */
private fun acomodar(lista: List<WidgetCV>): List<Triple<WidgetCV, Int, Int>> {
    val ocupado = Array(3) { BooleanArray(6) }
    val r = mutableListOf<Triple<WidgetCV, Int, Int>>()
    for (w in lista) {
        loop@ for (y in 0..(3 - w.al)) for (x in 0..(6 - w.an)) {
            if ((y until y + w.al).all { yy -> (x until x + w.an).none { xx -> ocupado[yy][xx] } }) {
                for (yy in y until y + w.al) for (xx in x until x + w.an) ocupado[yy][xx] = true
                r += Triple(w, x, y); break@loop
            }
        }
    }
    return r
}


/** El marco de cada widget: vidrio del tema; editando, borde punteado y ✕ para quitarlo. */
@Composable
private fun MarcoWidget(e: Estado, w: WidgetCV, fondo: Brush? = null, onClick: (() -> Unit)? = null, contenido: @Composable ColumnScope.() -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val forma = redondo(28f)
    val editando = e.extra.editandoWidgetsCV
    Box(Modifier.fillMaxSize().clip(forma).background(col.vidrioFuerte).then(if (fondo != null) Modifier.background(fondo) else Modifier)
        .border(1.dp, col.borde, forma)
        .then(if (editando) Modifier.drawBehind {
            drawRoundRect(col.acento, style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f * col.radio * esc.factor * density))
        } else Modifier)
        .then(if (onClick != null && !editando) Modifier.clickable(onClick = onClick) else Modifier)) {
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(22), vertical = esc.d(20)), verticalArrangement = Arrangement.spacedBy(esc.d(8)), content = contenido)
        if (editando) Box(Modifier.align(Alignment.TopEnd).padding(esc.d(10)).size(esc.d(34)).clip(CircleShape).background(col.acento)
            .clickable { Diptico.quitarWidgetCV(w.id) }, contentAlignment = Alignment.Center) { TB("✕", 16f, color = Color.White, peso = FontWeight.ExtraBold, traducir = false) }
    }
}


@Composable
private fun ChipWidget(t: String, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Box(Modifier.height(esc.d(38)).clip(RoundedCornerShape(50)).border(1.dp, col.borde, RoundedCornerShape(50)).clickable(onClick = onClick)
        .padding(horizontal = esc.d(14)), contentAlignment = Alignment.Center) { TB(t, 16f, peso = FontWeight.ExtraBold) }
}

@Composable
private fun BarraDato(t: String, frac: Float) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    TB(t, 16f, color = col.sub, traducir = false)
    Box(Modifier.fillMaxWidth().height(esc.d(9)).clip(RoundedCornerShape(50)).background(col.texto.copy(alpha = .3f))) {
        Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).fillMaxHeight().background(col.acento))
    }
}

private fun juegoAlAzar(e: Estado): Juego? = e.juegos.filter { e.datos[it.clave]?.oculto != true }.randomOrNull()

private fun ideaDippo(e: Estado): String {
    val j = e.juegos.filter { (e.datos[it.clave]?.ultimo ?: 0L) > 1 }.maxByOrNull { e.datos[it.clave]!!.ultimo }
    val h = java.time.LocalTime.now().hour
    return when {
        j == null -> "¿Probamos algo nuevo hoy? Toca «Sorpréndeme»."
        h >= 22 || h < 6 -> "Es tarde: algo corto antes de dormir, ¿te parece?"
        else -> "Buen momento para seguir con ${j.titulo}."
    }
}

// ———————————————————————————— 7b · la tienda de widgets (Thor abajo) ————————————————————————————



package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.estado.*
import cl.nikalbazu.diptico.estado.Diptico.Control

/**
 * La tienda de widgets en la pantalla de abajo (boceto 8b): pestañas del editor, categorías con L1/R1 (o tocándolas),
 * los widgets de la categoría y, al lado, el elegido en vivo con su tamaño real y para qué sirve. A lo agrega
 * (en la celda que mantuviste presionada, si venías de ahí) y abre sus ajustes para dejarlo listo.
 */
@Composable
internal fun TiendaWidgetsAbajo(e: Estado, controles: List<Control>) {
    val base = LocalEscala.current
    val items = controles.drop(1)
    val cat = controles.firstOrNull()
    val foco = (e.indice - 1).coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val elegido = items.getOrNull(foco)
    LienzoBoceto(1240f, 1080f, fondo = LocalColeccion.current.fondo, zoom = ZOOM_ABAJO) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        Column(Modifier.fillMaxSize().testTag("tienda-widgets").padding(horizontal = esc.d(32), vertical = esc.d(22)), verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
            // Encabezado: el editor tiene tres pestañas; aquí se ve Agregar.
            Row(Modifier.fillMaxWidth().height(esc.d(64)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                TB("Tienda de widgets", 38f, titulo = true)
                Spacer(Modifier.weight(1f))
                Diptico.PESTANAS_ESTUDIO.forEachIndexed { i, t ->
                    val activa = i == 1
                    Box(Modifier.height(esc.d(52)).clip(CircleShape).background(if (activa) col.texto else col.vidrio)
                        .border(1.dp, col.borde, CircleShape).clickable { Diptico.elegirPestanaEstudio(i) }.padding(horizontal = esc.d(22)),
                        contentAlignment = Alignment.Center) { TB(t, 20f, color = if (activa) col.fondo else col.texto, peso = FontWeight.ExtraBold) }
                }
                Box(Modifier.height(esc.d(52)).clip(CircleShape).background(col.vidrio).border(1.dp, col.borde, CircleShape)
                    .clickable { Diptico.terminarEdicion() }.padding(horizontal = esc.d(22)), contentAlignment = Alignment.Center) {
                    TB("Listo", 20f, peso = FontWeight.ExtraBold)
                }
            }
            // Categorías.
            if (cat != null) Row(Modifier.fillMaxWidth().height(esc.d(68)).vidrio(34f).padding(esc.d(6)), horizontalArrangement = Arrangement.spacedBy(esc.d(4))) {
                cat.elecciones.forEachIndexed { i, n ->
                    val activa = i == cat.elegida
                    val foco0 = e.indice == 0 && activa
                    Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(if (activa) col.texto else Color.Transparent)
                        .then(if (foco0) Modifier.border(3.dp, col.acento, CircleShape) else Modifier)
                        .clickable { cat.elegir?.invoke(i) }, contentAlignment = Alignment.Center) {
                        TB(n, 19f, color = if (activa) col.fondo else col.sub, peso = FontWeight.ExtraBold)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(18))) {
                // Los widgets de la categoría.
                val grilla = rememberLazyGridState()
                LaunchedEffect(foco) { if (items.isNotEmpty()) grilla.animateScrollToItem((foco - 2).coerceAtLeast(0)) }
                LazyVerticalGrid(GridCells.Fixed(2), Modifier.weight(0.58f).fillMaxHeight(), state = grilla,
                    verticalArrangement = Arrangement.spacedBy(esc.d(12)), horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                    items(items.size) { k ->
                        val c = items[k]
                        val sel = k == foco && e.indice > 0
                        val enInicio = c.valor.endsWith("✓")
                        Column(
                            Modifier.height(esc.d(190)).clip(RoundedCornerShape(esc.d(24)))
                                .background(if (sel) col.acento.copy(alpha = .22f) else Color.White.copy(alpha = .06f))
                                .border(if (sel) 3.dp else 1.dp, if (sel) col.texto else Color.White.copy(alpha = .12f), RoundedCornerShape(esc.d(24)))
                                .clickable { if (sel) c.mas() else Diptico.actualizar { it.copy(indice = k + 1) } }.padding(esc.d(16)),
                            verticalArrangement = Arrangement.spacedBy(esc.d(6)),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(esc.d(58)).clip(RoundedCornerShape(esc.d(18))).background(col.acento.copy(alpha = .3f)), contentAlignment = Alignment.Center) {
                                    c.icono?.let { IconoDiptico(it, col.texto, 30f, acento = Coral) }
                                }
                                Spacer(Modifier.weight(1f))
                                if (enInicio) TB("En tu inicio", 15f, color = Color(0xFF7FE0A8), peso = FontWeight.ExtraBold)
                            }
                            TB(c.titulo, 22f, titulo = true, lineas = 1)
                            TB(c.ayuda, 16f, color = col.sub, lineas = 2)
                        }
                    }
                }
                // El elegido, en vivo y a su tamaño, con lo que hace A.
                Column(Modifier.weight(0.42f).fillMaxHeight().vidrio(28f).padding(esc.d(20)), verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
                    TB("ASÍ SE VE", 16f, color = col.tenue, peso = FontWeight.ExtraBold, espaciado = 2f)
                    val tipo = elegido?.extra.orEmpty()
                    val w = e.widgets.firstOrNull { it.tipo == tipo }
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        if (w != null) {
                            // Vuelve a la escala de siempre: el widget se dibuja igual que en el inicio.
                            CompositionLocalProvider(LocalEscala provides base, LocalWidget provides w) {
                                Box(Modifier.fillMaxWidth().aspectRatio((w.an.toFloat() / w.al).coerceIn(0.6f, 3f))) {
                                    WidgetAdaptable(w) { c, f -> ContenidoWidget(e, w, c, f) }
                                }
                            }
                        } else TB(if (tipo == "imagen" || tipo == "video") "Eliges el archivo al agregarlo" else "Elige un widget", 18f, color = col.tenue, lineas = 2)
                    }
                    if (w != null) TB("${w.an} × ${w.al} · se estira desde su esquina", 16f, color = col.sub, lineas = 1)
                    elegido?.let { TB(it.ayuda, 18f, lineas = 3) }
                    if (e.celdaCatalogo != null) TB("Va en el lugar que mantuviste presionado", 16f, color = Color(0xFFFFB48F), lineas = 2)
                }
            }
            PistasFinas("L1 R1" to "Categoría", "A" to (if (elegido?.valor?.endsWith("✓") == true) "Ir a él" else "Agregar"), "L2 R2" to "Página", "Start" to "Listo")
        }
    }
}

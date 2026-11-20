package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.estado.*
import cl.nikalbazu.diptico.estado.Diptico.Control

/**
 * El menú de un widget (boceto 11b/11c): se abre al mantener presionado un widget y se cierra con «Listo» (o B).
 * Con dos pantallas ocupa la de abajo; con una, va al costado. Usa los mismos controles del editor de siempre
 * ([controles] = controlesEstudio: 0 = el widget, 1 = las pestañas, el resto = lo de la pestaña), así el control
 * se mueve igual: ↕ elige, ◀ ▶ cambia, A usa, L1/R1 pasan de pestaña.
 */
@Composable
fun MenuWidgetCV(e: Estado, controles: List<Control>, lateral: Boolean) {
    val elegido = controles.getOrNull(0)
    val pestanas = controles.getOrNull(1)?.takeIf { it.tipo == "chips" && it.grupo.isEmpty() }
    val items = if (pestanas != null) controles.drop(2) else controles.drop(1)
    val base = if (pestanas != null) 2 else 1
    LienzoBoceto(if (lateral) 760f else 1240f, 1080f, Modifier.testTag("menu-widget").background(LocalColeccion.current.fondo), zoom = if (lateral) 1f else ZOOM_ABAJO) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        Column(
            Modifier.fillMaxSize().padding(esc.d(if (lateral) 18f else 26f)).vidrio(44f, fuerte = true).padding(horizontal = esc.d(30), vertical = esc.d(26)),
            verticalArrangement = Arrangement.spacedBy(esc.d(18)),
        ) {
            // Qué widget estás editando (‹ › pasa al de al lado sin cerrar el menú).
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                val tipo = e.widgets.getOrNull(e.widgetSel)?.tipo.orEmpty()
                Box(Modifier.size(esc.d(84)).clip(RoundedCornerShape(esc.d(26))).background(col.acento.copy(alpha = .45f)), contentAlignment = Alignment.Center) {
                    IconoDiptico(Widgets.icono(tipo), col.texto, 40f, acento = Coral)
                }
                Column(Modifier.weight(1f)) {
                    TB("EDITANDO", 17f, color = col.sub, peso = FontWeight.ExtraBold, espaciado = 2.4f)
                    TB(elegido?.valor ?: "", 38f, titulo = true)
                }
                if (elegido?.menos != null) {
                    Redondo("‹", "Widget anterior") { elegido.menos?.invoke() }
                    Redondo("›", "Widget siguiente") { elegido.mas() }
                }
            }
            // Pestañas
            if (pestanas != null) Row(Modifier.fillMaxWidth().clip(CircleShape).background(col.texto.copy(alpha = .05f)).padding(esc.d(6)),
                horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                pestanas.elecciones.forEachIndexed { k, n ->
                    val on = k == pestanas.elegida
                    Box(Modifier.weight(1f).height(esc.d(62)).clip(CircleShape).background(if (on) col.elegido else Color.Transparent)
                        .clickable { pestanas.elegir?.invoke(k) }, contentAlignment = Alignment.Center) {
                        TB(n, 20f, color = if (on) col.enElegido else col.texto, peso = FontWeight.ExtraBold)
                    }
                }
            }
            // Lo de la pestaña
            val lista = rememberLazyListState()
            LaunchedEffect(e.indice) { (e.indice - base).takeIf { it >= 0 && it < items.size }?.let { lista.animateScrollToItem(it) } }
            LazyColumn(state = lista, modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
                itemsIndexed(items) { k, c -> FilaControl(c, base + k == e.indice) { Diptico.actualizar { it.copy(indice = base + k) } } }
                if (items.isEmpty()) item { TB("Este widget no tiene más que elegir aquí.", 21f, color = col.sub, lineas = 2) }
            }
            // Pie: «Listo» guarda y cierra el menú.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                TB(if (lateral) "Se ve al instante a la izquierda." else "Los cambios se ven arriba al instante.", 19f, color = col.sub, lineas = 2, modifier = Modifier.weight(1f))
                BotonTecla("A", "Listo", { Diptico.terminarEdicion() }, alto = 76f, principal = true, letra = 26f)
            }
            PistasFinas("↕" to "Opción", "◀ ▶" to "Cambiar", "L1 R1" to "Pestaña", "B" to "Listo")
        }
    }
}

@Composable
private fun Redondo(t: String, desc: String, accion: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Box(Modifier.size(esc.d(60)).clip(CircleShape).background(col.texto.copy(alpha = .08f)).clickable(onClickLabel = desc, onClick = accion),
        contentAlignment = Alignment.Center) { TB(t, 28f, peso = FontWeight.ExtraBold, traducir = false) }
}

/** Una fila del menú según el tipo de control (pasos, opciones, colores, formas, interruptor o acción). */
@Composable
private fun FilaControl(c: Control, foco: Boolean, elegir: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val forma = RoundedCornerShape(esc.d(26))
    val marco = Modifier.fillMaxWidth().clip(forma)
        .background(if (foco) col.texto.copy(alpha = .10f) else col.texto.copy(alpha = .04f))
        .then(if (foco) Modifier.border(3.dp, col.texto, forma) else Modifier.border(1.dp, col.borde, forma))
    @Composable
    fun Chip(n: String, sel: Boolean, accion: () -> Unit) {
        Box(Modifier.height(esc.d(54)).clip(CircleShape).background(if (sel) col.elegido else col.texto.copy(alpha = .06f))
            .border(1.dp, col.borde, CircleShape).clickable { elegir(); accion() }.padding(horizontal = esc.d(20)), contentAlignment = Alignment.Center) {
            TB(n, 19f, color = if (sel) col.enElegido else col.texto, peso = FontWeight.ExtraBold)
        }
    }
    when (c.tipo) {
        "interruptor" -> {
            val on = c.valor == "Sí"
            Row(marco.height(esc.d(88)).clickable { elegir(); c.mas() }.padding(horizontal = esc.d(24)), verticalAlignment = Alignment.CenterVertically) {
                TB(c.titulo, 23f, peso = FontWeight.ExtraBold, modifier = Modifier.weight(1f), lineas = 2)
                Box(Modifier.size(esc.d(74), esc.d(42)).clip(CircleShape).background(if (on) Color(0xFF2EC4A6) else col.texto.copy(alpha = .18f)).padding(esc.d(5)),
                    contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart) {
                    Box(Modifier.size(esc.d(32)).clip(CircleShape).background(Color.White))
                }
            }
        }
        "pasos" -> Row(marco.height(esc.d(88)).clickable { elegir() }.padding(horizontal = esc.d(24)), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
            TB(c.titulo, 23f, peso = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Box(Modifier.size(esc.d(52)).clip(CircleShape).background(col.texto.copy(alpha = .08f)).clickable { elegir(); c.menos?.invoke() }, contentAlignment = Alignment.Center) { TB("‹", 24f, traducir = false) }
            TB(c.valor, 21f, peso = FontWeight.ExtraBold, traducir = false)
            Box(Modifier.size(esc.d(52)).clip(CircleShape).background(col.texto.copy(alpha = .08f)).clickable { elegir(); c.mas() }, contentAlignment = Alignment.Center) { TB("›", 24f, traducir = false) }
        }
        "accion", "catalogo" -> Row(marco.height(esc.d(88)).clickable { elegir(); c.mas() }.padding(horizontal = esc.d(24)), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
            c.icono?.let { IconoDiptico(it, col.texto, 30f, acento = Coral) }
            TB(c.titulo, 23f, peso = FontWeight.ExtraBold, modifier = Modifier.weight(1f), lineas = 2,
                color = if (c.icono == Icono.BORRAR) Color(0xFFFFB4B4) else col.texto)
            if (c.valor.isNotEmpty() && c.valor != "A") Box(Modifier.height(esc.d(48)).clip(CircleShape).background(col.texto.copy(alpha = .08f)).padding(horizontal = esc.d(18)), contentAlignment = Alignment.Center) {
                TB(c.valor, 19f, peso = FontWeight.ExtraBold, traducir = false)
            }
        }
        else -> Column(marco.clickable { elegir() }.padding(esc.d(20)), verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
            TB(c.titulo.uppercase(), 17f, color = col.sub, peso = FontWeight.ExtraBold, espaciado = 1.8f)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(esc.d(10)), verticalAlignment = Alignment.Bottom) {
                when (c.tipo) {
                    "colores" -> c.colores.forEachIndexed { k, v ->
                        val sel = k == c.elegida
                        Box(Modifier.size(esc.d(58)).clip(CircleShape)
                            .background(if (v == 0L) Brush.linearGradient(listOf(Uva, Coral)) else Brush.linearGradient(listOf(Color(v), Color(v))))
                            .border(if (sel) 4.dp else 1.dp, if (sel) col.texto else col.borde, CircleShape)
                            .clickable { elegir(); c.elegir?.invoke(k) }, contentAlignment = Alignment.Center) {
                            if (v == 0L) TB("Tema", 12f, color = Color.White, peso = FontWeight.ExtraBold)
                        }
                    }
                    "formas" -> c.formas.forEachIndexed { k, (an, al) ->
                        val sel = k == c.elegida
                        Column(Modifier.width(esc.d(116)).clip(RoundedCornerShape(esc.d(22)))
                            .background(if (sel) col.texto.copy(alpha = .12f) else col.texto.copy(alpha = .04f))
                            .then(if (sel) Modifier.border(3.dp, col.texto, RoundedCornerShape(esc.d(22))) else Modifier)
                            .clickable { elegir(); c.elegir?.invoke(k) }.padding(vertical = esc.d(14)),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
                            val u = 60f / maxOf(an.toFloat(), al * 1.3f)
                            Box(Modifier.height(esc.d(64)), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(esc.d(an * u), esc.d(al * u * 0.8f)).clip(RoundedCornerShape(esc.d(8))).background(if (sel) col.texto else col.texto.copy(alpha = .3f)))
                            }
                            TB(c.elecciones.getOrNull(k) ?: "$an × $al", 16f, peso = FontWeight.ExtraBold, traducir = false)
                        }
                    }
                    else -> c.elecciones.forEachIndexed { k, n -> Chip(n, k == c.elegida) { c.elegir?.invoke(k) } }
                }
            }
        }
    }
}

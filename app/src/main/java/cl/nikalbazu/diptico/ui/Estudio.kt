package cl.nikalbazu.diptico.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Widgets

/**
 * Estudio de widgets: todo lo del inicio en un solo panel ordenado.
 * Con dos pantallas ocupa la de abajo y el inicio se ve en vivo arriba; con una sola pantalla va a un costado.
 * Pestañas: Widget (el elegido), Agregar (catálogo) y Página (grilla, fondo y encabezado).
 */
@Composable
fun PanelEstudio(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val lateral = e.unaPantalla
    val controles = remember(e) { Diptico.controlesEstudio(e) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        Column(
            (if (lateral && !esc.angosta) Modifier.fillMaxHeight().fillMaxWidth(0.5f)
            else Modifier.fillMaxSize()).background(p.fondo2.copy(alpha = 1f))
                .sinCerrar().padding(start = esc.d(14), end = esc.d(14), top = esc.d(12), bottom = esc.d(10)),
        ) {
            // El estado de la página y el botón de salida se mantienen visibles durante toda la edición.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = esc.d(4), vertical = esc.d(5)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(esc.d(30)).clip(RoundedCornerShape(esc.d(9))).background(p.superficie), contentAlignment = Alignment.Center) {
                    IconoDiptico(Icono.EDITAR, p.acentoTexto, 17f)
                }
                Spacer(Modifier.width(esc.d(10)))
                Column(Modifier.weight(1f)) {
                    Texto("Editar widgets", 18f, lineas = 1)
                    val cantidad = e.widgets.count { it.visible && it.pagina == e.paginaInicio }
                    Detalle("Página ${e.paginaInicio + 1}/${e.paginas} · $cantidad widgets", 12f, lineas = 1)
                }
                Box(
                    Modifier.clip(RoundedCornerShape(50)).fondoBoton(Uva).clickable { Diptico.terminarEdicion() }
                        .padding(horizontal = esc.d(14), vertical = esc.d(7)),
                ) { Texto("Listo", 13.5f, color = SobreUva, lineas = 1) }
            }
            Spacer(Modifier.height(esc.d(10)))
            // Pestañas, como un selector segmentado.
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(p.superficie.copy(alpha = 0.8f)).padding(esc.d(3)),
                horizontalArrangement = Arrangement.spacedBy(esc.d(3)),
            ) {
                Diptico.PESTANAS_ESTUDIO.forEachIndexed { i, t ->
                    val activa = i == e.estudioPestana
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(50)).then(if (activa) Modifier.fondoBoton(Uva) else Modifier)
                            .clickable { Diptico.elegirPestanaEstudio(i) }.padding(vertical = esc.d(6)),
                        contentAlignment = Alignment.Center,
                    ) { Texto(t, 13f, color = if (activa) SobreUva else p.tinta, lineas = 1) }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            val guia = when (e.estudioPestana) {
                1 -> e.celdaCatalogo?.let { (x, y) -> "Agregar en columna ${x + 1} · fila ${y + 1}" }
                    ?: "Elige un widget para añadirlo a esta página."
                2 -> "Ajusta el espacio, el fondo y la distribución."
                else -> "Elige un widget para cambiar su forma y estilo."
            }
            Detalle(guia, 12f, lineas = 1, modifier = Modifier.padding(horizontal = esc.d(4)))
            Spacer(Modifier.height(esc.d(6)))
            // En la zona "grilla" la cruceta elige widgets directo en el inicio: aquí se avisa, sin tapar nada.
            val enGrilla = e.estudioZona == "grilla" && !e.estudioMover
            androidx.compose.animation.AnimatedVisibility(enGrilla) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = esc.d(8)).clip(RoundedCornerShape(esc.d(14))).background(Uva.copy(alpha = 0.16f))
                        .border(esc.d(1), Uva.copy(alpha = 0.4f), RoundedCornerShape(esc.d(14)))
                        .clickable { Diptico.zonaEstudio("panel") }.padding(horizontal = esc.d(12), vertical = esc.d(8)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconoDiptico(Icono.MANDO, p.tinta, 18f, acento = Coral)
                    Spacer(Modifier.width(esc.d(10)))
                    Column(Modifier.weight(1f)) {
                        Texto("Elige un widget en tu inicio", 13.5f, lineas = 1)
                        Detalle("A lo mueve · mantén A para agregar · R1 para sus opciones", 11.5f, lineas = 1)
                    }
                }
            }
            // Controles agrupados: cada grupo es una tarjeta, con aire entre grupos.
            val filas = remember(controles) {
                buildList {
                    var grupo = ""
                    controles.forEachIndexed { i, c ->
                        if (c.grupo.isNotEmpty() && c.grupo != grupo) { add(-1 to c.grupo); grupo = c.grupo }
                        add(i to "")
                    }
                }
            }
            val lista = rememberLazyListState()
            // Solo se desplaza si el ajuste elegido quedó fuera de la vista (incluye su título de grupo).
            LaunchedEffect(e.indice, e.estudioPestana) {
                val pos = filas.indexOfFirst { it.first == e.indice }
                if (pos < 0) return@LaunchedEffect
                val vistos = lista.layoutInfo.visibleItemsInfo
                if (vistos.isEmpty()) {
                    if (pos > 5) lista.scrollToItem((pos - 2).coerceAtLeast(0))
                    return@LaunchedEffect
                }
                val primero = vistos.firstOrNull()?.index ?: 0
                val ultimo = vistos.lastOrNull()?.index ?: 0
                val conTitulo = if (pos > 0 && filas[pos - 1].first < 0) pos - 1 else pos
                when {
                    e.indice == 0 -> lista.animateScrollToItem(0)
                    conTitulo < primero || (conTitulo == primero && (vistos.firstOrNull()?.offset ?: 0) < 0) -> lista.animateScrollToItem(conTitulo)
                    pos >= ultimo -> lista.animateScrollToItem((conTitulo - 1).coerceAtLeast(0))
                }
            }
            LazyColumn(
                state = lista,
                modifier = Modifier.weight(1f).graphicsLayer { alpha = if (enGrilla) 0.6f else 1f },
            ) {
                items(filas.size) { k ->
                    val (i, grupo) = filas[k]
                    if (i < 0) {
                        Etiqueta(grupo.uppercase(), Coral, 10.5f, Modifier.padding(start = esc.d(6), top = esc.d(if (k == 0) 2f else 16f), bottom = esc.d(6)))
                    } else {
                        // Posición dentro de su grupo: la tarjeta se redondea arriba en el primero y abajo en el último.
                        val primero = k == 0 || filas[k - 1].first < 0
                        val ultimo = k == filas.size - 1 || filas[k + 1].first < 0
                        ControlEstudio(
                            controles[i], activo = i == e.indice && !e.estudioMover && !enGrilla, moviendo = e.estudioMover && e.estudioPestana == 0 && i == 1,
                            primero = primero, ultimo = ultimo,
                        ) {
                            Diptico.actualizar { it.copy(indice = i, estudioZona = "panel") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            if (e.estudioMover) Pistas("✥" to "Mover", "L1 R1" to "Ancho", "L2 R2" to "Alto", "A" to "Soltar")
            else if (enGrilla) Pistas("✥" to "Elegir", "A" to "Mover", "Y" to "Agregar", "R1" to "Opciones", "B" to "Listo")
            else Pistas("↑ ↓" to "Elegir", "← →" to "Cambiar", "L1 R1" to "Pestaña", "B" to "Inicio")
        }
    }
}

@Composable
private fun ControlEstudio(c: Diptico.Control, activo: Boolean, moviendo: Boolean, primero: Boolean = true, ultimo: Boolean = true, elegir: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val fondo by animateColorAsState(if (activo || moviendo) p.superficie else p.superficie.copy(alpha = 0.55f), label = "fondo")
    val r = esc.d(16)
    val forma = if (activo || moviendo) RoundedCornerShape(esc.d(14))
    else RoundedCornerShape(topStart = if (primero) r else esc.d(0), topEnd = if (primero) r else esc.d(0), bottomStart = if (ultimo) r else esc.d(0), bottomEnd = if (ultimo) r else esc.d(0))
    if (!primero) Box(Modifier.fillMaxWidth().height(esc.d(1)).background(p.fondo2))
    Column(
        Modifier.fillMaxWidth().clip(forma).background(fondo)
            .border(esc.d(if (activo || moviendo) 2f else 0f), if (moviendo) Coral else if (activo) Uva else Color.Transparent, forma)
            .clickable { elegir() }.padding(horizontal = esc.d(12), vertical = esc.d(9)),
    ) {
        when (c.tipo) {
            "pasos" -> Row(verticalAlignment = Alignment.CenterVertically) {
                c.icono?.let { IconoDiptico(it, p.tinta, 16f, acento = Coral); Spacer(Modifier.width(esc.d(8))) }
                Texto(c.titulo, 13.5f, lineas = 1, modifier = Modifier.weight(1f))
                BotonPaso("‹") { elegir(); c.menos?.invoke() }
                Box(Modifier.width(esc.d(80)), contentAlignment = Alignment.Center) { Texto(c.valor, 13f, color = p.acentoTexto, lineas = 1) }
                BotonPaso("›") { elegir(); c.mas() }
            }
            "accion", "interruptor" -> Row(verticalAlignment = Alignment.CenterVertically) {
                c.icono?.let {
                    Box(Modifier.size(esc.d(30)).clip(CircleShape).background(p.fondo.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                        IconoDiptico(it, if (moviendo) Coral else p.tinta, 16f, acento = Coral)
                    }
                    Spacer(Modifier.width(esc.d(10)))
                }
                Column(Modifier.weight(1f)) {
                    Texto(c.titulo, 13.5f, lineas = 1, color = if (moviendo) Coral else p.tinta)
                    if (activo && c.ayuda.isNotEmpty()) Detalle(c.ayuda, 11.5f, lineas = 2)
                }
                Spacer(Modifier.width(esc.d(8)))
                if (c.tipo == "interruptor") {
                    val si = c.valor == "Sí"
                    Box(
                        Modifier.size(esc.d(44), esc.d(24)).clip(RoundedCornerShape(50)).background(if (si) Uva else p.linea)
                            .clickable { elegir(); c.mas() },
                        contentAlignment = if (si) Alignment.CenterEnd else Alignment.CenterStart,
                    ) { Box(Modifier.padding(esc.d(3)).size(esc.d(18)).clip(CircleShape).background(Color.White)) }
                } else if (c.valor.isNotEmpty() && c.valor != "—") {
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).then(if (activo || moviendo) Modifier.fondoBoton(if (moviendo) Coral else Uva) else Modifier.background(p.fondo))
                            .clickable { elegir(); c.mas() }.padding(horizontal = esc.d(11), vertical = esc.d(5)),
                    ) { Texto(c.valor, 12f, color = if (activo || moviendo) SobreUva else p.tinta, lineas = 1) }
                }
            }
            "catalogo" -> Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(esc.d(40)).clip(RoundedCornerShape(esc.d(12))).background(Uva.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    IconoDiptico(c.icono ?: Icono.APPS, p.tinta, 20f, acento = Coral)
                }
                Spacer(Modifier.width(esc.d(10)))
                Column(Modifier.weight(1f)) {
                    Texto(c.titulo, 14f, lineas = 1)
                    Detalle(c.ayuda, 11.5f, lineas = if (activo) 2 else 1)
                }
                Spacer(Modifier.width(esc.d(8)))
                val agregar = c.valor == "Agregar" || c.valor == "Elegir"
                Box(
                    Modifier.clip(RoundedCornerShape(50)).then(if (agregar) Modifier.fondoBoton(Uva) else Modifier.background(p.fondo))
                        .clickable { elegir(); c.mas() }.padding(horizontal = esc.d(11), vertical = esc.d(5)),
                ) { Texto(if (agregar) "+ ${c.valor}" else c.valor, 12f, color = if (agregar) SobreUva else p.tenue, lineas = 1) }
            }
            else -> {
                // Elecciones a la vista: chips, estilos, colores, formas o widgets.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Texto(c.titulo, 13.5f, lineas = 1, modifier = Modifier.weight(1f))
                    if (c.valor.isNotEmpty() && c.tipo != "widgets") Texto(c.valor, 12f, color = p.acentoTexto, lineas = 1)
                }
                Spacer(Modifier.height(esc.d(7)))
                val desplazar = rememberScrollState()
                val paso = with(LocalDensity.current) { esc.d(if (c.tipo == "colores") 32f else 76f).toPx() }
                LaunchedEffect(c.elegida) { if (c.elegida > 1) desplazar.animateScrollTo(((c.elegida - 1) * paso).toInt()) }
                Row(Modifier.fillMaxWidth().horizontalScroll(desplazar), horizontalArrangement = Arrangement.spacedBy(esc.d(6)), verticalAlignment = Alignment.CenterVertically) {
                    when (c.tipo) {
                        "colores" -> c.colores.forEachIndexed { k, col ->
                            val sel = k == c.elegida
                            Box(
                                Modifier.size(esc.d(26)).clip(CircleShape)
                                    .background(if (col == 0L) Brush.linearGradient(listOf(Uva, Coral)) else Brush.linearGradient(listOf(Color(col), Color(col))))
                                    .border(esc.d(if (sel) 3f else 1f), if (sel) p.tinta else p.linea, CircleShape)
                                    .clickable { elegir(); c.elegir?.invoke(k) },
                                contentAlignment = Alignment.Center,
                            ) { if (col == 0L) Texto("A", 10f, color = Color.White, lineas = 1, traducir = false) }
                        }
                        "formas" -> c.formas.forEachIndexed { k, (an, al) ->
                            val sel = k == c.elegida
                            Column(
                                Modifier.width(esc.d(70)).clip(RoundedCornerShape(esc.d(10))).background(if (sel) Uva.copy(alpha = 0.22f) else p.fondo.copy(alpha = 0.5f))
                                    .border(esc.d(if (sel) 2f else 0f), if (sel) Uva else Color.Transparent, RoundedCornerShape(esc.d(10)))
                                    .clickable { elegir(); c.elegir?.invoke(k) }.padding(vertical = esc.d(6)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val u = 36f / maxOf(an.toFloat(), al * 1.4f)
                                Box(Modifier.height(esc.d(28)), contentAlignment = Alignment.Center) {
                                    Box(Modifier.size(esc.d(an * u), esc.d(al * u * 0.75f)).clip(RoundedCornerShape(esc.d(3))).background(if (sel) Uva else p.tenue.copy(alpha = 0.6f)))
                                }
                                Texto(c.elecciones[k], 10.5f, color = if (sel) p.tinta else p.tenue, lineas = 1)
                            }
                        }
                        "estilos" -> c.elecciones.forEachIndexed { k, nombre ->
                            val sel = k == c.elegida
                            Column(
                                Modifier.width(esc.d(70)).clickable { elegir(); c.elegir?.invoke(k) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                MuestraEstilo(Widgets.ESTILOS[k], sel)
                                Spacer(Modifier.height(esc.d(3)))
                                Texto(nombre, 10.5f, color = if (sel) p.tinta else p.tenue, lineas = 1)
                            }
                        }
                        "widgets" -> c.elecciones.forEachIndexed { k, nombre ->
                            val sel = k == c.elegida
                            Box(
                                Modifier.clip(RoundedCornerShape(50)).then(if (sel) Modifier.fondoBoton(Uva) else Modifier.background(p.fondo.copy(alpha = 0.6f)))
                                    .clickable { elegir(); c.elegir?.invoke(k) }.padding(horizontal = esc.d(11), vertical = esc.d(6)),
                            ) { Texto(nombre, 12f, color = if (sel) SobreUva else p.tinta, lineas = 1) }
                        }
                        else -> c.elecciones.forEachIndexed { k, nombre ->
                            val sel = k == c.elegida
                            Box(
                                Modifier.clip(RoundedCornerShape(50)).then(if (sel) Modifier.fondoBoton(Uva) else Modifier.background(p.fondo.copy(alpha = 0.6f)))
                                    .clickable { elegir(); c.elegir?.invoke(k) }.padding(horizontal = esc.d(11), vertical = esc.d(5)),
                            ) { Texto(nombre, 12f, color = if (sel) SobreUva else p.tinta, lineas = 1) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonPaso(t: String, accion: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(Modifier.size(esc.d(28)).clip(CircleShape).background(p.fondo).clickable { accion() }, contentAlignment = Alignment.Center) {
        Texto(t, 16f, lineas = 1, traducir = false)
    }
}

/** Una tarjetita que muestra cómo se ve cada estilo de widget. */
@Composable
private fun MuestraEstilo(estilo: String, sel: Boolean) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val forma = RoundedCornerShape(esc.d(9))
    Box(
        Modifier.size(esc.d(62), esc.d(38)).clip(RoundedCornerShape(esc.d(11)))
            .background(Brush.linearGradient(listOf(Uva.copy(alpha = 0.35f), Coral.copy(alpha = 0.25f))))
            .border(esc.d(if (sel) 2.5f else 0f), if (sel) p.tinta else Color.Transparent, RoundedCornerShape(esc.d(11)))
            .padding(esc.d(6)),
    ) {
        val m = Modifier.fillMaxSize().clip(forma)
        Box(
            when (estilo) {
                "vidrio" -> m.background(Color.White.copy(alpha = 0.22f)).border(esc.d(1), Color.White.copy(alpha = 0.4f), forma)
                "solido" -> m.background(Uva)
                "contorno" -> m.border(esc.d(1.5f), Uva, forma)
                "sinfondo" -> m
                else -> m.background(p.superficie)
            },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.padding(start = esc.d(5)).size(esc.d(18), esc.d(4)).clip(RoundedCornerShape(50)).background(if (estilo == "solido") SobreUva else p.tinta))
        }
    }
}

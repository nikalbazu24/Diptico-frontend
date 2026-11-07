package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado

/**
 * Personalizar una consola: nombre, color, ícono, imagen grande, logo, música, cajas y emulador.
 * La otra pantalla (o la parte de arriba, con una sola) muestra cómo queda, en vivo.
 */
@Composable
fun PanelEditarConsola(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val c = Consolas.porId(e.consolaEditando)
    val color = c?.color ?: Uva
    val opciones = remember(e) { Diptico.opcionesEditarConsola(e) }
    Box(Modifier.fillMaxSize().fondoPantalla(p.fondo2).sinCerrar()) {
        // Un poco del color (o la imagen) de la consola de fondo.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), Color.Transparent))))
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).padding(start = esc.d(14), end = esc.d(14), top = esc.d(12), bottom = esc.d(10))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconoConsola(c, 34f, p.fondo2)
                    Spacer(Modifier.width(esc.d(10)))
                    Column(Modifier.weight(1f)) {
                        Etiqueta(if (c == null) "Personalizar carpeta" else "Personalizar consola", Coral, 10.5f)
                        Texto(c?.bonito ?: "Todo", 19f, lineas = 1)
                    }
                    Box(
                        Modifier.size(esc.d(34)).clip(CircleShape).background(p.superficie).clickable { Diptico.cerrarPanel() },
                        contentAlignment = Alignment.Center,
                    ) { IconoDiptico(cl.nikalbazu.diptico.estado.Icono.CERRAR, p.tinta, 16f) }
                }
                Spacer(Modifier.height(esc.d(10)))
                val lista = rememberLazyListState()
                LaunchedEffect(e.indice) {
                    lista.mostrar(e.indice)
                }
                LazyColumn(state = lista, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
                    itemsIndexed(opciones) { i, o ->
                        val activa = i == e.indice
                        val forma = RoundedCornerShape(esc.d(14))
                        Row(
                            Modifier.fillMaxWidth().height(esc.d(50)).clip(forma)
                                .background(if (activa) p.superficie else p.superficie.copy(alpha = 0.55f))
                                .then(if (activa) Modifier.border(esc.d(2), Uva, forma) else Modifier)
                                .clickable { Diptico.elegirIndice(i) }.padding(horizontal = esc.d(10)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            o.icono?.let {
                                Box(Modifier.size(esc.d(30)).clip(CircleShape).background(color.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                                    IconoDiptico(it, p.tinta, 16f, acento = Coral)
                                }
                                Spacer(Modifier.width(esc.d(10)))
                            }
                            Texto(o.titulo, 14.5f, lineas = 1, modifier = Modifier.weight(1f))
                            if (o.menos != null) {
                                Box(Modifier.size(esc.d(26)).clip(CircleShape).background(p.fondo).clickable { Diptico.actualizar { it.copy(indice = i) }; o.menos.invoke() }, contentAlignment = Alignment.Center) {
                                    Texto("‹", 15f, traducir = false)
                                }
                                if (o.titulo == "Color") {
                                    Spacer(Modifier.width(esc.d(6)))
                                    Box(Modifier.size(esc.d(20)).clip(CircleShape).background(color).border(esc.d(1.5f), Color.White.copy(alpha = 0.6f), CircleShape))
                                }
                                Box(Modifier.widthIn(min = esc.d(64)).padding(horizontal = esc.d(6)), contentAlignment = Alignment.Center) {
                                    Texto(o.valor, 12f, color = p.acentoTexto, lineas = 1)
                                }
                                Box(Modifier.size(esc.d(26)).clip(CircleShape).background(p.fondo).clickable { Diptico.actualizar { it.copy(indice = i) }; o.accion() }, contentAlignment = Alignment.Center) {
                                    Texto("›", 15f, traducir = false)
                                }
                            } else if (o.valor.isNotEmpty()) {
                                Box(
                                    Modifier.clip(RoundedCornerShape(50)).background(if (activa) Uva.copy(alpha = 0.25f) else p.fondo.copy(alpha = 0.7f))
                                        .padding(horizontal = esc.d(10), vertical = esc.d(4)),
                                ) { Texto(o.valor, 12f, lineas = 1, modifier = Modifier.widthIn(max = esc.d(150))) }
                            } else Texto("›", 18f, color = p.tenue, traducir = false)
                        }
                    }
                }
                Spacer(Modifier.height(esc.d(6)))
                Pistas("↑ ↓" to "Moverte", "A" to "Cambiar", "← →" to "Color", "B" to "Listo")
            }
            // Con una sola pantalla, la vista previa va al lado.
            if (e.unaPantalla && !esc.angosta) {
                Box(Modifier.weight(0.9f).padding(end = esc.d(12), top = esc.d(14), bottom = esc.d(14))) {
                    MiniPantalla(711f, 400f, Modifier.fillMaxSize()) { PreviaConsolaEditada(e) }
                }
            }
        }
    }
}

@Composable
internal fun PreviaConsolaEditada(e: Estado) {
    val id = e.consolaEditando
    val opcion = Diptico.opcionesEditarConsola(e).getOrNull(e.indice)?.titulo
    if (id == "todo" || opcion == "Imagen del ícono de carpeta" || opcion == "Restaurar portada de carpeta") {
        val esc = LocalEscala.current
        Box(Modifier.fillMaxSize().padding(esc.d(20)), contentAlignment = Alignment.Center) {
            PortadaCarpeta(e, id, Modifier.fillMaxSize().clip(RoundedCornerShape(esc.d(20)))) {
                IconoConsola(Consolas.porId(id), 64f, LocalPaleta.current.fondo2)
            }
        }
    } else {
        VistaSistema(e.copy(consolaSel = id, panel = cl.nikalbazu.diptico.estado.Panel.NINGUNO))
    }
}

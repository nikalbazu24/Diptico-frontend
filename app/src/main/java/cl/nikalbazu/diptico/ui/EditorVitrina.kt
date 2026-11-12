package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import cl.nikalbazu.diptico.estado.*

/** Colores de las tres piezas (los mismos que en el boceto): logo ámbar, caja coral, imagen grande celeste. */
private val COLORES_PIEZA = listOf(Color(0xFFFFB454), Color(0xFFFF7A59), Color(0xFF7FD4FF))
private val MARCAS = listOf("L", "C", "F")
private val FondoEditor = Color(0xFF0B0D17)
private val Celda = Color(0xFF171B34)
private val CeldaElegida = Color(0xFF2A2F55)
private val Borde = Color(0xFF3A3F66)

/**
 * Lugar en la vitrina: con dos pantallas, arriba se ve la vitrina con los cambios en vivo y abajo la cuadrícula;
 * con una, la vista previa va a la izquierda y la cuadrícula a la derecha.
 */
@Composable
fun EditorVitrina(e: Estado, rol: Rol) {
    val ed = e.extra.edicionVitrina ?: return
    val j = e.juegosPorClave[ed.clave] ?: return
    val dividido = !e.unaPantalla && e.dosDisponibles
    if (rol == Rol.INFERIOR && e.unaPantalla) return
    val tapar = Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, indication = null) { }
    if (rol == Rol.SUPERIOR && dividido) {
        Box(tapar.testTag("editor-vitrina-previa")) { VitrinaCajas(e, j, forzarPs5 = true) }
        return
    }
    if (rol == Rol.SUPERIOR) return
    val esc = LocalEscala.current
    Box(tapar.background(FondoEditor).testTag("editor-vitrina")) {
        if (dividido) ControlesVitrina(e, ed, j, Modifier.fillMaxSize().padding(esc.d(16)))
        else Row(Modifier.fillMaxSize().padding(esc.d(14)), horizontalArrangement = Arrangement.spacedBy(esc.d(14)), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1.05f).aspectRatio(16f / 9f).clip(RoundedCornerShape(esc.d(16))).border(esc.d(1), Borde, RoundedCornerShape(esc.d(16)))) {
                VitrinaCajas(e, j, forzarPs5 = true)
            }
            ControlesVitrina(e, ed, j, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun ControlesVitrina(e: Estado, ed: EdicionVitrina, j: cl.nikalbazu.diptico.data.Juego, modifier: Modifier) {
    val esc = LocalEscala.current
    val blanco = Color.White
    val tenue = Color(0xFFA9ADC4)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        Etiqueta("Personalizar › Lugar en la vitrina", tenue, 11f)
        Texto(j.titulo, 17f, color = blanco, lineas = 1, traducir = false)
        if (!e.portadaPs5) Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(12))).background(Color(0xFF2A2140))
                .clickable { Diptico.usarPortadaPs5() }.padding(horizontal = esc.d(12), vertical = esc.d(6)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Detalle("Esto se ve con la portada «PS5 con caja».", 11.5f, color = blanco, lineas = 2, modifier = Modifier.weight(1f))
            Texto("Usarla", 12.5f, color = COLORES_PIEZA[0], lineas = 1)
        }
        // Las tres piezas: se elige cuál mover (L1/R1 con el mando).
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
            Disposicion.NOMBRES.forEachIndexed { k, n ->
                val act = ed.sel == k
                Box(
                    Modifier.weight(1f).height(esc.d(40)).clip(RoundedCornerShape(esc.d(12)))
                        .background(if (act) CeldaElegida else Celda)
                        .border(esc.d(2), if (act) COLORES_PIEZA[k] else Borde, RoundedCornerShape(esc.d(12)))
                        .clickable { Diptico.elegirPiezaVitrina(k) }.testTag("pieza-$k"),
                    contentAlignment = Alignment.Center,
                ) { Texto(n, 13f, color = blanco, lineas = 1) }
            }
        }
        // La cuadrícula: tocar una celda lleva ahí la pieza elegida.
        Column(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(esc.d(14))).background(Color(0xFF12152A))
                .border(esc.d(1), Borde, RoundedCornerShape(esc.d(14))).padding(esc.d(6)),
            verticalArrangement = Arrangement.spacedBy(esc.d(4)),
        ) {
            for (f in 0 until Disposicion.FILAS) Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(4))) {
                for (c in 0 until Disposicion.COLUMNAS) {
                    val aqui = (0..2).filter { k -> ed.disp.pieza(k).let { it.c == c && it.f == f } }
                    val elegida = ed.sel in aqui
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(esc.d(8)))
                            .background(if (elegida) CeldaElegida else Celda)
                            .border(if (elegida) esc.d(2) else esc.d(1), if (elegida) COLORES_PIEZA[ed.sel] else Color(0xFF262A47), RoundedCornerShape(esc.d(8)))
                            .clickable { Diptico.ponerPiezaVitrina(c, f) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (aqui.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(esc.d(2))) {
                            aqui.forEach { k -> Texto(MARCAS[k], 12f, color = COLORES_PIEZA[k], lineas = 1, traducir = false) }
                        }
                    }
                }
            }
        }
        // Tamaño de la pieza elegida.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
            Texto("Tamaño de «${Disposicion.NOMBRES[ed.sel]}»", 12.5f, color = Color(0xFFCFD2E6), lineas = 1, modifier = Modifier.weight(1f))
            BotonChico("−") { Diptico.tamPiezaVitrina(-0.1f) }
            Texto("${Math.round(ed.disp.pieza(ed.sel).t * 100)} %", 13f, color = blanco, lineas = 1, traducir = false,
                alinear = TextAlign.Center, modifier = Modifier.widthIn(min = esc.d(52)))
            BotonChico("+") { Diptico.tamPiezaVitrina(0.1f) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
            Interruptor("Caja 3D", ed.disp.conCaja, "X") { Diptico.alternarCajaVitrina() }
            Interruptor("Fondo difuminado", ed.disp.difuminado, "Y") { Diptico.alternarDifuminadoVitrina() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
            BotonAncho("Volver a como estaba", lleno = false, Modifier.weight(1f)) { Diptico.volverDisposicionOriginal() }
            BotonAncho("Listo", lleno = true, Modifier.weight(1f)) { Diptico.guardarEditorVitrina() }
        }
        Pistas("Cruceta" to "Mover", "L1/R1" to "Pieza", "L2/R2" to "Tamaño", "B" to "Listo")
    }
}

@Composable
private fun BotonChico(t: String, alPulsar: () -> Unit) {
    val esc = LocalEscala.current
    Box(
        Modifier.size(esc.d(40)).clip(RoundedCornerShape(esc.d(12))).background(Color(0xFF1A1E38))
            .border(esc.d(1), Borde, RoundedCornerShape(esc.d(12))).clickable { alPulsar() },
        contentAlignment = Alignment.Center,
    ) { Texto(t, 18f, color = Color.White, lineas = 1, traducir = false) }
}

@Composable
private fun RowScope.Interruptor(t: String, si: Boolean, boton: String, alPulsar: () -> Unit) {
    val esc = LocalEscala.current
    Row(
        Modifier.weight(1f).height(esc.d(40)).clip(RoundedCornerShape(esc.d(12))).background(Color(0xFF1A1E38))
            .border(esc.d(1), Borde, RoundedCornerShape(esc.d(12))).clickable { alPulsar() }.padding(horizontal = esc.d(10)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Texto(t, 12.5f, color = Color.White, lineas = 1, modifier = Modifier.weight(1f))
        Texto((if (si) "Sí" else "No") + " · $boton", 12f, color = COLORES_PIEZA[0], lineas = 1)
    }
}

@Composable
private fun BotonAncho(t: String, lleno: Boolean, modifier: Modifier, alPulsar: () -> Unit) {
    val esc = LocalEscala.current
    Box(
        modifier.height(esc.d(42)).clip(RoundedCornerShape(esc.d(12)))
            .then(if (lleno) Modifier.background(COLORES_PIEZA[1]) else Modifier.border(esc.d(1), Borde, RoundedCornerShape(esc.d(12))))
            .clickable { alPulsar() },
        contentAlignment = Alignment.Center,
    ) { Texto(t, 13f, color = if (lleno) Color(0xFF1A0A05) else Color(0xFFCFD2E6), lineas = 1) }
}

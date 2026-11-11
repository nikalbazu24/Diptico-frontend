package cl.nikalbazu.diptico.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.estado.*

private val Claro = Color(0xFFF4F5FB)
private val Suave = Color(0xFFA5A8BD)
private val Verde = Color(0xFF23C486)
/** Color del centro: el celeste de Switch 2. */
private val Acento = Color(0xFF00C3E3)
private val Naranjo = Color(0xFFFF9F43)

/** El centro está abierto en esta pantalla: con dos pantallas, abajo la lista y arriba el detalle; con una, todo junto. */
fun nativosAqui(e: Estado, rol: Rol): Boolean = e.extra.nativosEn >= 0 && (rol != Rol.INFERIOR || !e.unaPantalla)

/**
 * Centro de Switch 2 y ports nativos. Con dos pantallas, la de abajo (la táctil) tiene la lista para elegir y la de
 * arriba muestra en grande lo elegido, con sus consejos. Con una sola pantalla, la lista y el detalle van lado a lado.
 */
@Composable
fun CentroNativos(e: Estado, rol: Rol) {
    val abierto = nativosAqui(e, rol)
    val animar = LocalMovimiento.current
    val t by animateFloatAsState(if (abierto) 1f else 0f, if (!animar) snap() else tween(if (abierto) 280 else 180), label = "nativos")
    if (!abierto && t < 0.01f) return
    val esc = LocalEscala.current
    // Las filas se arman solo al cambiar lo que muestran (no en cada cuadro de la animación).
    val filas = remember(e.extra.nativosPestana, e.juegos, e.emuladores, e.extra.nativosEn) { Diptico.entradasNativos(e) }
    val sel = e.extra.nativosSel.coerceIn(0, (filas.size - 1).coerceAtLeast(0))
    val dividido = !e.unaPantalla && e.dosDisponibles
    Box(Modifier.fillMaxSize().testTag("centro-nativos")) {
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = t }.background(Color(0x99080912))
                .clickable(remember { MutableInteractionSource() }, indication = null) { Diptico.cerrarNativos() },
        )
        Box(
            Modifier.fillMaxSize().graphicsLayer { val s = 0.94f + 0.06f * t; scaleX = s; scaleY = s; alpha = (t * 2f).coerceIn(0f, 1f) }
                .padding(horizontal = esc.d(if (esc.angosta) 12 else 34), vertical = esc.d(if (esc.angosta) 12 else 22)),
        ) {
            val forma = RoundedCornerShape(esc.d(26))
            Box(
                Modifier.fillMaxSize().clip(forma)
                    .background(Brush.linearGradient(listOf(Color(0xF2121A2C), Color(0xF20A0C16))))
                    .background(Brush.radialGradient(listOf(Acento.copy(alpha = 0.20f), Color.Transparent), center = Offset(0f, 0f), radius = 1000f))
                    .border(esc.d(1.5f), Acento.copy(alpha = 0.45f), forma)
                    .clickable(remember { MutableInteractionSource() }, indication = null) {},
            ) {
                when {
                    // Dos pantallas: arriba el detalle grande, abajo la lista.
                    dividido && rol == Rol.SUPERIOR -> Detalle(filas.getOrNull(sel), e.extra.nativosPestana, grande = true)
                    dividido -> Lista(e, filas, sel)
                    esc.angosta -> Lista(e, filas, sel)
                    else -> Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1.1f).fillMaxHeight()) { Lista(e, filas, sel) }
                        Box(Modifier.width(esc.d(1)).fillMaxHeight().background(Color.White.copy(alpha = 0.08f)))
                        Box(Modifier.weight(1f).fillMaxHeight()) { Detalle(filas.getOrNull(sel), e.extra.nativosPestana, grande = false) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Lista(e: Estado, filas: List<EntradaNativa>, sel: Int) {
    val esc = LocalEscala.current
    val estado = rememberLazyListState()
    LaunchedEffect(sel, e.extra.nativosPestana) { if (filas.isNotEmpty()) estado.animateScrollToItem((sel - 1).coerceAtLeast(0)) }
    Column(Modifier.fillMaxSize().padding(esc.d(if (esc.angosta) 14 else 18))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Texto("SWITCH 2 Y PORTS", 11f, color = Acento, peso = FontWeight.ExtraBold, espaciado = 1.4f, lineas = 1)
                Texto("Juega lo nuevo, nativo", if (esc.angosta) 19f else 22f, color = Claro, peso = FontWeight.ExtraBold, lineas = 1)
            }
            Pastilla("B · Cerrar") { Diptico.cerrarNativos() }
        }
        Spacer(Modifier.height(esc.d(10)))
        // Pestañas: L1 / R1 (o ← →) o con el dedo.
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8)), verticalAlignment = Alignment.CenterVertically) {
            Texto("L1", 10f, color = Suave, lineas = 1, traducir = false)
            Pestana("Switch 2", e.extra.nativosPestana == 0) { if (e.extra.nativosPestana != 0) Diptico.pestanaNativos(-1) }
            Pestana("Ports nativos", e.extra.nativosPestana == 1) { if (e.extra.nativosPestana != 1) Diptico.pestanaNativos(1) }
            Texto("R1", 10f, color = Suave, lineas = 1, traducir = false)
        }
        Spacer(Modifier.height(esc.d(10)))
        Crossfade(e.extra.nativosPestana, animationSpec = tween(160), label = "pestana-nativos", modifier = Modifier.weight(1f)) { _ ->
            LazyColumn(state = estado, verticalArrangement = Arrangement.spacedBy(esc.d(6)), modifier = Modifier.fillMaxSize()) {
                itemsIndexed(filas, key = { _, f -> f.id }) { i, f ->
                    val anteriorGrupo = filas.getOrNull(i - 1)?.grupo
                    if (f.grupo != anteriorGrupo) {
                        Texto(f.grupo.uppercase(), 10.5f, color = Suave, peso = FontWeight.Bold, espaciado = 1.2f, lineas = 1,
                            modifier = Modifier.padding(top = esc.d(if (i == 0) 0 else 8), bottom = esc.d(2), start = esc.d(4)))
                    }
                    Fila(f, i == sel) { Diptico.elegirNativo(i) }
                }
            }
        }
        // Lo que hace A sobre lo elegido.
        filas.getOrNull(sel)?.accion?.takeIf { it.isNotEmpty() }?.let { a ->
            Spacer(Modifier.height(esc.d(8)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pastilla("A · $a", lleno = true) { Diptico.usarNativo(sel) }
            }
        }
    }
}

@Composable
private fun Fila(f: EntradaNativa, elegida: Boolean, alPulsar: () -> Unit) {
    val esc = LocalEscala.current
    val forma = RoundedCornerShape(esc.d(14))
    Row(
        Modifier.fillMaxWidth().clip(forma)
            .background(if (elegida) Acento.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f))
            .then(if (elegida) Modifier.border(esc.d(2), Acento, forma) else Modifier)
            .clickable(onClick = alPulsar).padding(horizontal = esc.d(10), vertical = esc.d(8)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(esc.d(38)).clip(RoundedCornerShape(esc.d(10))).background(Color.White.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
            when {
                f.paquete != null && f.instalada -> IconoApp(f.paquete, 34f, desnudo = true)
                f.id == "equipo" -> IconoDiptico(cl.nikalbazu.diptico.estado.Icono.CHISPA, Claro, 18f)
                else -> Texto(f.titulo.take(1), 16f, color = Suave, peso = FontWeight.ExtraBold, lineas = 1, traducir = false)
            }
        }
        Spacer(Modifier.width(esc.d(10)))
        Column(Modifier.weight(1f)) {
            Texto(f.titulo, 14.5f, color = Claro, peso = FontWeight.Bold, lineas = 1, traducir = false)
            Texto(f.detalle, 11.5f, color = Suave, lineas = 1)
        }
        Spacer(Modifier.width(esc.d(8)))
        Box(Modifier.size(esc.d(9)).clip(CircleShape).background(if (f.lista) Verde else if (f.id == "equipo") Naranjo else Color.White.copy(alpha = 0.18f)))
    }
}

@Composable
private fun Detalle(f: EntradaNativa?, pestana: Int, grande: Boolean) {
    val esc = LocalEscala.current
    Column(
        Modifier.fillMaxSize().padding(esc.d(if (grande) 28 else 18)),
        verticalArrangement = Arrangement.spacedBy(esc.d(if (grande) 10 else 8)),
    ) {
        Texto(if (pestana == 0) "NINTENDO SWITCH 2" else "PORTS NATIVOS", 11f, color = Acento, peso = FontWeight.ExtraBold, espaciado = 1.4f, lineas = 1)
        if (f == null) {
            Texto("Nada que mostrar", 20f, color = Claro, lineas = 1)
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (f.paquete != null && f.instalada) {
                IconoApp(f.paquete, if (grande) 64f else 46f, desnudo = true)
                Spacer(Modifier.width(esc.d(14)))
            }
            Column(Modifier.weight(1f)) {
                Texto(f.titulo, if (grande) 30f else 22f, color = Claro, peso = FontWeight.ExtraBold, lineas = 2, traducir = false)
                Texto(f.detalle, if (grande) 14f else 12.5f, color = if (f.lista) Verde else Suave, lineas = 2)
            }
        }
        if (f.ayuda.isNotEmpty()) Texto(f.ayuda, if (grande) 15.5f else 13.5f, color = Claro.copy(alpha = 0.86f), lineas = 5)
        if (f.consejos.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
            f.consejos.forEach { c ->
                Row(verticalAlignment = Alignment.Top) {
                    Texto("•", 13f, color = Acento, lineas = 1, traducir = false)
                    Spacer(Modifier.width(esc.d(8)))
                    Texto(c, if (grande) 13.5f else 12.5f, color = Claro.copy(alpha = 0.8f), lineas = 3)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (grande) Texto(
            if (pestana == 0) "Los juegos de Switch 2 que Banana Native convierte se abren solos como port nativo; los demás, con NovaS."
            else "Los ports que instales aparecen solos en tu carpeta Ports.",
            12f, color = Suave, lineas = 2,
        )
    }
}

@Composable
private fun Pestana(texto: String, activa: Boolean, alPulsar: () -> Unit) {
    val esc = LocalEscala.current
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(if (activa) Acento else Color.White.copy(alpha = 0.07f))
            .clickable(onClick = alPulsar).padding(horizontal = esc.d(14), vertical = esc.d(6)),
    ) { Texto(texto, 13f, color = if (activa) Color(0xFF06222A) else Claro, peso = FontWeight.Bold, lineas = 1) }
}

@Composable
private fun Pastilla(texto: String, lleno: Boolean = false, alPulsar: () -> Unit) {
    val esc = LocalEscala.current
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(if (lleno) Claro else Color.White.copy(alpha = 0.1f))
            .clickable(onClick = alPulsar).padding(horizontal = esc.d(16), vertical = esc.d(7)),
    ) { Texto(texto, 13.5f, color = if (lleno) Color(0xFF15121D) else Claro, peso = FontWeight.Bold, lineas = 1) }
}

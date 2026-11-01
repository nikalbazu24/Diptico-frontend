package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono

private fun juegoEncuadre(e: Estado): Juego? = e.juegos.firstOrNull { it.clave == e.encuadreDe.substringBeforeLast('|') }

/**
 * Encuadrar una portada o una imagen grande: se corre con la cruceta o el dedo y se acerca con L1/R1 (o − +).
 * Todo se ve en vivo, aquí y en la otra pantalla, antes de guardar.
 */
@Composable
fun PanelEncuadre(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val j = juegoEncuadre(e) ?: return
    val c = Consolas.porId(j.consolaId)
    val esArte = e.encuadreDe.endsWith("|arte")
    val b = e.encuadreBorrador
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Column(Modifier.fillMaxSize().padding(start = esc.d(14), end = esc.d(14), top = esc.d(12), bottom = esc.d(10))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.PORTADA, p.tinta, 22f, acento = Coral)
                Spacer(Modifier.width(esc.d(10)))
                Column(Modifier.weight(1f)) {
                    Texto(if (esArte) "Encuadrar la imagen grande" else "Encuadrar la portada", 18f, lineas = 1)
                    Detalle(j.titulo, 12.5f, lineas = 1)
                }
            }
            Spacer(Modifier.height(esc.d(8)))
            Row(Modifier.weight(1f).fillMaxWidth()) {
                // La foto en su forma: se arrastra con el dedo.
                BoxWithConstraints(
                    Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(esc.d(16))).background(p.fondo.copy(alpha = 0.6f))
                        .pointerInput(Unit) {
                            detectDragGestures { cambio, d ->
                                cambio.consume()
                                Diptico.moverEncuadre(-d.x / size.width * 2.4f, -d.y / size.height * 2.4f)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val anchoU = maxWidth.value / esc.factor
                    val altoU = maxHeight.value / esc.factor
                    if (esArte || c == null) {
                        Box(Modifier.padding(esc.d(10)).fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(esc.d(12)))) {
                            ArteJuego(j, preferirArte = true, maxLado = 1200, modifier = Modifier.fillMaxSize())
                            Tercios()
                        }
                    } else {
                        val base = cajaDe(c)
                        val k = minOf((altoU - 24f) / base.alto, (anchoU - 24f) / base.ancho)
                        Box {
                            CajaJuego(j, c, escala = k)
                            Box(Modifier.matchParentSize()) { Tercios() }
                        }
                    }
                }
                Spacer(Modifier.width(esc.d(10)))
                // Controles táctiles (lo mismo que la cruceta y los gatillos).
                Column(Modifier.width(esc.d(148)).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    BotonRedondo("↑") { Diptico.moverEncuadre(0f, 0.1f) }
                    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(30))) {
                        BotonRedondo("←") { Diptico.moverEncuadre(0.1f, 0f) }
                        BotonRedondo("→") { Diptico.moverEncuadre(-0.1f, 0f) }
                    }
                    BotonRedondo("↓") { Diptico.moverEncuadre(0f, -0.1f) }
                    Spacer(Modifier.height(esc.d(10)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BotonRedondo("−") { Diptico.acercarEncuadre(-0.1f) }
                        Texto("${(b.zoom * 100).toInt()}%", 13f, lineas = 1, traducir = false, modifier = Modifier.padding(horizontal = esc.d(8)))
                        BotonRedondo("+") { Diptico.acercarEncuadre(0.1f) }
                    }
                    Spacer(Modifier.height(esc.d(10)))
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(p.superficie).clickable { Diptico.centrarEncuadre() }
                            .padding(horizontal = esc.d(14), vertical = esc.d(6)),
                    ) { Texto("Centrar", 12.5f, lineas = 1) }
                    Spacer(Modifier.height(esc.d(6)))
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).fondoBoton(Uva).clickable { Diptico.guardarEncuadre() }
                            .padding(horizontal = esc.d(18), vertical = esc.d(7)),
                    ) { Texto("Guardar", 13.5f, color = SobreUva, lineas = 1) }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("✥" to "Mover", "L1 R1" to "Acercar", "X" to "Centrar", "A" to "Guardar", "B" to "Volver")
        }
    }
}

/** Líneas de tercios, para ayudar a centrar. */
@Composable
private fun Tercios() {
    Canvas(Modifier.fillMaxSize()) {
        val c = Color.White.copy(alpha = 0.35f)
        val g = 1.dp.toPx()
        for (i in 1..2) {
            drawLine(c, Offset(size.width * i / 3f, 0f), Offset(size.width * i / 3f, size.height), g)
            drawLine(c, Offset(0f, size.height * i / 3f), Offset(size.width, size.height * i / 3f), g)
        }
    }
}

@Composable
private fun BotonRedondo(t: String, accion: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(
        Modifier.size(esc.d(34)).clip(CircleShape).background(p.superficie).border(esc.d(1.5f), Uva.copy(alpha = 0.4f), CircleShape).clickable { accion() },
        contentAlignment = Alignment.Center,
    ) { Texto(t, 15f, lineas = 1, traducir = false) }
}

/** La otra pantalla mientras se encuadra: cómo va a quedar de verdad. */
@Composable
fun VistaEncuadre(e: Estado) {
    val esc = LocalEscala.current
    val j = juegoEncuadre(e) ?: return
    val c = Consolas.porId(j.consolaId)
    val esArte = e.encuadreDe.endsWith("|arte")
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (esArte || c == null) {
            Heroe(j, 1f)
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent), endX = 900f)))
        } else {
            ArteJuego(j, preferirArte = true, maxLado = 900, modifier = Modifier.fillMaxSize().blur(28.dp).alpha(0.45f))
            val base = cajaDe(c)
            Box(Modifier.align(Alignment.Center)) { CajaJuego(j, c, escala = minOf(300f / base.alto, 380f / base.ancho)) }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(esc.d(18))) {
            Etiqueta("Vista previa", Coral, 11f)
            Texto("Así se verá", 18f, color = Color.White, lineas = 1)
        }
    }
}

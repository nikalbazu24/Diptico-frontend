package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono

/**
 * Modo una pantalla (como en una tele, una Odin o un teléfono): el arte del juego de fondo,
 * la información arriba y las consolas con el estante abajo.
 */
@Composable
fun PantallaUnica(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    if (e.inicio == "widgets") {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            // En el estudio de widgets, el inicio completo se ve en miniatura a la izquierda (en vivo) y el panel a la derecha.
            val estudio = e.panel == cl.nikalbazu.diptico.estado.Panel.ESTUDIO && !esc.angosta
            if (estudio) {
                val anchoPx = constraints.maxWidth
                val altoPx = constraints.maxHeight
                val escala = 0.46f
                Column(
                    Modifier.fillMaxHeight().fillMaxWidth(0.5f).background(p.fondo2),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Etiqueta("Vista previa en vivo", p.tenue, 11f)
                    Spacer(Modifier.height(esc.d(8)))
                    Box(
                        Modifier.layout { medible, _ ->
                            val pl = medible.measure(androidx.compose.ui.unit.Constraints.fixed(anchoPx, altoPx))
                            layout((anchoPx * escala).toInt(), (altoPx * escala).toInt()) {
                                pl.placeWithLayer(0, 0) { scaleX = escala; scaleY = escala; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f) }
                            }
                        }.clip(RoundedCornerShape(esc.d(14))).border(esc.d(2), p.linea, RoundedCornerShape(esc.d(14))),
                    ) { PantallaInicio(e) }
                    Spacer(Modifier.height(esc.d(8)))
                    Detalle("Toca un widget para elegirlo, o arrástralo", 11.5f, lineas = 1)
                }
            } else {
                Box(Modifier.fillMaxSize().difuminar(e)) { PantallaInicio(e) }
            }
            Paneles(e, conAyuda = true)
        }
        return
    }
    if (e.vistaConsolas == "sistemas" && e.enSistemas) {
        Box(Modifier.fillMaxSize().background(p.fondo)) {
            Box(Modifier.fillMaxSize().difuminar(e)) {
                VistaSistema(e, completa = false)
                SistemasCarrusel(e, Modifier.align(Alignment.BottomEnd).fillMaxWidth(0.5f).fillMaxHeight(0.8f).clipToBounds())
            }
            Paneles(e, conAyuda = true)
        }
        return
    }
    Box(Modifier.fillMaxSize().background(p.fondo)) {
      Box(Modifier.fillMaxSize().difuminar(e)) {
        val juego = e.seleccionado
        if (juego != null) Heroe(juego, 0.78f)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to p.fondo, 0.22f to p.fondo, 0.7f to Color.Transparent)))
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(esc.d(270))
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.4f to p.fondo.copy(alpha = 0.92f), 1f to p.fondo)),
        )

        BarraSuperior(Modifier.align(Alignment.TopStart), trabajos = e.trabajos) {
            Box(
                Modifier.size(esc.d(32)).clip(CircleShape).background(p.superficie.copy(alpha = 0.8f)).clickable { Diptico.abrirMenu() },
                contentAlignment = Alignment.Center,
            ) { IconoDiptico(Icono.MENU, p.tinta, 18f, acento = Coral) }
            Spacer(Modifier.width(esc.d(14)))
        }

        if (juego == null) {
            Column(Modifier.align(Alignment.TopStart).padding(start = esc.d(28), top = esc.d(70))) {
                Texto(if (e.escaneando) "Buscando tus juegos…" else "No encontré juegos en tu carpeta", 24f)
                if (!e.escaneando) Detalle("Abre el menú con Start › Biblioteca para elegirla.", 15f)
            }
        } else {
            AnimatedContent(
                targetState = juego,
                transitionSpec = { (fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 10 }) togetherWith fadeOut(tween(180)) },
                label = "info",
                modifier = Modifier.padding(start = esc.d(28), top = esc.d(50)).widthIn(max = esc.d(430)),
            ) { j -> InfoUnica(e, j) }
        }

        if (e.vista == "estante") {
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                Cinta(e)
                Estante(e, alto = 140f * LocalTamJuegos.current.coerceAtMost(1.2f), escala = 0.74f * LocalTamJuegos.current)
                Row(
                    Modifier.fillMaxWidth().padding(start = esc.d(22), end = esc.d(22), bottom = esc.d(8)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))
                    Pistas("A" to "Jugar", "Y" to "Opciones", "L1 R1" to "Consola", "Select" to "Widgets", "Start" to "Menú")
                }
            }
        } else {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.6f)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, p.fondo.copy(alpha = 0.95f), p.fondo))),
            ) {
                Cinta(e)
                VistaJuegos(e, Modifier.fillMaxWidth().weight(1f), 0.74f)
            }
        }
      }

        Paneles(e, conAyuda = true)
    }
}

@Composable
private fun InfoUnica(e: Estado, j: cl.nikalbazu.diptico.data.Juego) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val c = cl.nikalbazu.diptico.data.Consolas.porId(j.consolaId)
    val paso = e.datos[j.clave]?.paso.orEmpty()
    Column {
        if (c != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoConsola(c, 20f, p.fondo)
                Spacer(Modifier.width(esc.d(8)))
                Etiqueta(c.nombreLargo, p.tenue, 12f)
            }
            Spacer(Modifier.height(esc.d(6)))
        }
        Texto(j.titulo, 30f, lineas = 2, interlineado = 33f, sombra = true)
        Spacer(Modifier.height(esc.d(6)))
        Row {
            Box(Modifier.padding(top = esc.d(6)).size(esc.d(8)).clip(CircleShape).background(Coral))
            Spacer(Modifier.width(esc.d(8)))
            Texto(
                if (paso.isEmpty()) "Aprieta Y para anotar dónde quedaste" else paso,
                16f, lineas = 1, color = if (paso.isEmpty()) p.tenue else p.tinta,
                peso = androidx.compose.ui.text.font.FontWeight.SemiBold,
            )
        }
    }
}

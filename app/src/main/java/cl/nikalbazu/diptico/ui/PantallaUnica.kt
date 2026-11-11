@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package cl.nikalbazu.diptico.ui

import cl.nikalbazu.diptico.estado.*

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import cl.nikalbazu.diptico.estado.Panel

/**
 * Modo una pantalla (como en una tele, una Odin o un teléfono): el arte del juego de fondo,
 * la información arriba y las consolas con el estante abajo.
 */
@Composable
fun PantallaUnica(e: Estado) {
    if (e.editandoWidgets || !Estilo.efectosCompletos) {
        PantallaUnicaContenido(e)
        return
    }
    // Carátulas ↔ widgets con transición suave, igual que con dos pantallas.
    androidx.compose.animation.AnimatedContent(
        targetState = e.inicio == "widgets",
        transitionSpec = {
            // Igual que con dos pantallas: los widgets quedan «al lado» de la portada y entran deslizándose.
            val ida = if (targetState) 1 else -1
            (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) +
                androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(380)) { ida * it / 3 }) togetherWith
                (androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220)) +
                    androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(320)) { -ida * it / 4 })
        },
        label = "inicioUnica",
    ) { w -> PantallaUnicaContenido(e.copy(inicio = if (w) "widgets" else "heroe")) }
}

@Composable
private fun PantallaUnicaContenido(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    if (e.panel == Panel.WIDGET && e.coleccionVisual != null) { ColeccionUniverso(e); return }
    if (e.inicio == "widgets") {
        Box(Modifier.fillMaxSize()) {
            val dividido = e.panel == Panel.ESTUDIO && !esc.angosta
            if (dividido) {
                Lienzo(e.oscuro, Modifier.fillMaxWidth(0.5f).fillMaxHeight(), e.tamTexto) {
                    PantallaInicio(e)
                }
            } else {
                Box(Modifier.fillMaxSize().difuminar(e)) {
                    PantallaInicio(e)
                }
            }
            Paneles(e, conAyuda = true)
        }
        return
    }
    if (e.vistaConsolas in Diptico.MODOS_ELEGIR) {
        Box(Modifier.fillMaxSize().background(p.fondo)) {
            Column(Modifier.fillMaxSize().difuminar(e)) {
                Box(Modifier.fillMaxWidth().weight(1f).padding(top = esc.d(12))) {
                    when {
                        e.vistaConsolas == "carpetas" -> VistaCarpetas(e, Modifier.fillMaxSize(), 1.05f)
                        !e.enSistemas -> Column(Modifier.fillMaxSize()) {
                            CabeceraSistema(e)
                            VistaJuegos(e, Modifier.fillMaxWidth().weight(1f), 1.05f)
                        }
                        e.vistaConsolas in Diptico.VISTAS_CARPETAS -> CarpetasConsolas(e, Modifier.fillMaxSize())
                        e.vistaConsolas == "discos" -> {
                            VistaSistema(e, completa = false)
                            DiscosConsolas(e, Modifier.align(Alignment.BottomEnd).fillMaxWidth(0.55f).fillMaxHeight(0.82f).clipToBounds())
                        }
                        else -> {
                            VistaSistema(e, completa = false)
                            SistemasCarrusel(e, Modifier.align(Alignment.BottomEnd).fillMaxWidth(0.5f).fillMaxHeight(0.8f).clipToBounds())
                        }
                    }
                }
                if (e.sesion == null && !e.carpetaEnVentana) BarraFlotante(e, if (e.enSistemas) null else e.seleccionado, consola = e.enSistemas)
            }
            if (e.sesion == null && e.panel == Panel.NINGUNO)
                DippoInicio(e, 48f, Modifier.align(Alignment.TopEnd).padding(top = esc.d(ALTO_BARRA_SISTEMA + 10f), end = esc.d(20)))
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
            // El turbo queda junto al estado del equipo.
            Box(
                Modifier.size(esc.d(32)).clip(CircleShape).background(p.superficie.copy(alpha = 0.8f))
                    .combinedClickable(onLongClick = { Diptico.alternarTurbo() }) { Diptico.turboAhora() },
                contentAlignment = Alignment.Center,
            ) { IconoDiptico(Icono.CHISPA, if (e.turbo) Color(0xFFFF5252) else p.tenue, 17f) }
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

        val lateral = e.vistaConsolas == "lado" || e.vistaConsolas == "rueda"
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .then(if (e.vista == "estante" && !lateral) Modifier else Modifier.fillMaxHeight(if (e.vista == "mosaicos") 0.65f else 0.62f)),
        ) {
            when {
                lateral -> Row(Modifier.fillMaxWidth().weight(1f)) {
                    if (e.vistaConsolas == "lado") RielConsolas(e) else RuedaConsolas(e)
                    VistaJuegos(e, Modifier.fillMaxHeight().weight(1f), 0.74f)
                }
                e.vista == "estante" -> {
                    Cinta(e)
                    Estante(e, alto = 140f * LocalTamJuegos.current.coerceAtMost(1.2f), escala = 0.74f * LocalTamJuegos.current)
                }
                else -> {
                    Cinta(e)
                    VistaJuegos(e, Modifier.fillMaxWidth().weight(1f), 0.74f)
                }
            }
            if (e.sesion == null) BarraFlotante(e, juego)
        }
      }

        if (e.sesion == null && e.panel == Panel.NINGUNO)
            DippoInicio(e, 52f, Modifier.align(Alignment.TopEnd).padding(top = esc.d(ALTO_BARRA_SISTEMA + 14f), end = esc.d(22)))
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



package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado

/**
 * Modo una pantalla (como en una tele): el arte del juego de fondo, la información arriba
 * y las consolas con el estante abajo, todo en la pantalla de arriba.
 */
@Composable
fun PantallaUnica(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(Modifier.fillMaxSize().background(p.fondo)) {
        val juego = e.seleccionado
        if (juego != null) {
            Crossfade(targetState = juego, animationSpec = tween(700), label = "arte", modifier = Modifier.fillMaxSize()) { j ->
                Box(Modifier.fillMaxSize()) {
                    ArteJuego(
                        j, preferirArte = true, maxLado = 1600,
                        modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().fillMaxWidth(0.78f),
                    )
                }
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to p.fondo, 0.22f to p.fondo, 0.7f to Color.Transparent)))
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(esc.d(340))
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.45f to p.fondo.copy(alpha = 0.92f), 1f to p.fondo)),
        )

        BarraSuperior(Modifier.align(Alignment.TopStart)) {
            Spacer(Modifier.width(esc.d(16)))
            Icon(
                Icons.Filled.Settings, contentDescription = "Ajustes", tint = p.tinta,
                modifier = Modifier.size(esc.d(22)).clickable { Diptico.abrirAjustes() },
            )
        }

        if (juego == null) {
            Texto(
                if (e.escaneando) "Buscando tus juegos…" else "No encontré juegos en tu carpeta", 20f, color = p.tenue,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            AnimatedContent(
                targetState = juego,
                transitionSpec = { (fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 10 }) togetherWith fadeOut(tween(200)) },
                label = "info",
                modifier = Modifier.padding(start = esc.d(44), top = esc.d(72)).width(esc.d(520)),
            ) { j ->
                val c = Consolas.porId(j.consolaId)
                val datos = e.datos[j.clave]
                Column(verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
                    if (c != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconoMedio(c.medio, c.color, p.fondo, tam = 22f)
                            Spacer(Modifier.width(esc.d(8)))
                            Texto(c.nombreLargo, 11f, color = p.tenue, familia = Nunito, peso = FontWeight.ExtraBold, espaciado = 1.6f)
                        }
                    }
                    Texto(j.titulo, 38f, lineas = 2, interlineado = 40f)
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.padding(top = esc.d(5)).size(esc.d(9)).clip(CircleShape).background(Coral))
                        Spacer(Modifier.width(esc.d(8)))
                        val paso = datos?.paso.orEmpty()
                        Texto(
                            if (paso.isEmpty()) "Aprieta Y para anotar dónde quedaste" else "Tu próximo paso: $paso",
                            16f, peso = FontWeight.SemiBold, lineas = 2, interlineado = 21f,
                            color = if (paso.isEmpty()) p.tenue else p.tinta,
                        )
                    }
                }
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Cinta(e)
            Estante(e, alto = 196f)
            Row(
                Modifier.fillMaxWidth().padding(start = esc.d(24), end = esc.d(24), bottom = esc.d(10)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Texto(juego?.titulo ?: "", 15f, lineas = 1, modifier = Modifier.weight(1f))
                Texto(
                    "A jugar · Y próximo paso · L1 R1 consola · Start ajustes", 11f,
                    color = p.tenue, familia = Nunito, peso = FontWeight.ExtraBold,
                )
            }
        }

        Paneles(e)
    }
}

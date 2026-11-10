package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado

/** La biblioteca y sus posiciones siguen montadas durante la apertura y el cierre. */
@Composable
internal fun VistaCarpetas(e: Estado, modifier: Modifier, escalaEstante: Float) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    if (!e.enSistemas && !e.carpetaEnVentana) {
        Column(modifier) {
            CabeceraSistema(e)
            VistaJuegos(e, Modifier.fillMaxWidth().weight(1f), escalaEstante)
        }
        return
    }
    val transicion = remember { MutableTransitionState(false) }
    transicion.targetState = e.carpetaEnVentana
    val abierta = transicion.currentState || transicion.targetState
    val fondo = remember(abierta, e.consolaSel, e.nivelesGlass.carpetas) { FondoVentanaCristal() }
    val captura = abierta && Estilo.glass && LocalCristal.current.efectos
    val entrada = duracionMovimiento(260)
    val salida = duracionMovimiento(180)
    Box(modifier) {
        // Detrás de la carpeta abierta, las demás quedan difuminadas y oscuras: así no se confunden con los juegos.
        val desenfoque by animateDpAsState(if (abierta && Estilo.efectosCompletos) 16.dp else 0.dp, tween(entrada), label = "desenfoque carpetas")
        CarpetasConsolas(e, Modifier.fillMaxSize().desenfoqueAnimado { desenfoque }
            .then(if (captura) Modifier.fondoDeVentana(fondo, e.nivelesGlass.carpetas) else Modifier), interactiva = !abierta)
        AnimatedVisibility(transicion, enter = fadeIn(tween(entrada)), exit = fadeOut(tween(salida))) {
            Box(Modifier.fillMaxSize().background(p.fondo.copy(alpha = .55f)).clickable { Diptico.volverASistemas() })
        }
        AnimatedVisibility(transicion, Modifier.align(Alignment.Center),
            enter = entradaCarpeta(ajusteMovimiento()), exit = salidaVentana(ajusteMovimiento())) {
            CompositionLocalProvider(LocalFondoVentanaCristal provides fondo,
                LocalPaleta provides paletaLecturaCristal(p, e.nivelesGlass.carpetas)) {
                val forma = androidx.compose.foundation.shape.RoundedCornerShape(esc.d(26))
                Column(Modifier.fillMaxWidth(.94f).fillMaxHeight(.90f).testTag("carpeta-abierta")
                    .superficieApp(p.fondo2, forma, CapaCristal.CARPETA).sinCerrar()) {
                    CabeceraSistema(e)
                    VistaJuegos(e, Modifier.fillMaxWidth().weight(1f), escalaEstante)
                }
            }
        }
    }
}

@Composable
internal fun CabeceraCarpetas(e: Estado, interactiva: Boolean) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    Row(Modifier.fillMaxWidth().padding(horizontal = esc.d(16), vertical = esc.d(5)),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Texto("Biblioteca", 16f, lineas = 1)
        Texto("${e.pestanas.size - 1} sistemas", 11f, color = p.tenue, lineas = 1)
        Box(Modifier.superficieApp(p.superficie, RoundedCornerShape(50))
            .clickable(enabled = interactiva) { Diptico.abrirGaleria("orden_carpetas") }
            .padding(horizontal = esc.d(12), vertical = esc.d(6))) { Texto("Ordenar", 11f, lineas = 1) }
    }
}

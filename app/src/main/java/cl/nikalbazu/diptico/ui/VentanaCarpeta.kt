package cl.nikalbazu.diptico.ui

import cl.nikalbazu.diptico.estado.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlin.math.roundToInt
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
    // Entrar a una carpeta (y volver): los juegos crecen desde el centro y las carpetas se alejan, como abrir una caja.
    val mov = ajusteMovimiento()
    androidx.compose.animation.AnimatedContent(
        targetState = !e.enSistemas && !e.carpetaEnVentana,
        transitionSpec = {
            if (!mov.activo) androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
            else if (targetState) (fadeIn(tween(mov.ms(240))) + androidx.compose.animation.scaleIn(tween(mov.ms(320), easing = CurvaPosarse), initialScale = 0.9f)) togetherWith
                (fadeOut(tween(mov.ms(160))) + androidx.compose.animation.scaleOut(tween(mov.ms(220)), targetScale = 1.08f))
            else (fadeIn(tween(mov.ms(240))) + androidx.compose.animation.scaleIn(tween(mov.ms(300), easing = CurvaPosarse), initialScale = 1.08f)) togetherWith
                (fadeOut(tween(mov.ms(160))) + androidx.compose.animation.scaleOut(tween(mov.ms(220)), targetScale = 0.9f))
        },
        label = "entrar-carpeta", modifier = modifier,
    ) { dentro ->
        if (dentro) Column(Modifier.fillMaxSize()) {
            CabeceraSistema(e)
            VistaJuegos(e, Modifier.fillMaxWidth().weight(1f), escalaEstante)
        } else ListaCarpetas(e, Modifier.fillMaxSize(), escalaEstante)
    }
}

@Composable
private fun ListaCarpetas(e: Estado, modifier: Modifier, escalaEstante: Float) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
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
        // La barra de apps flota abajo: la ventana termina justo encima de ella (antes la tapaba y se perdían los nombres de la última fila).
        val aire = LocalAireAbajo.current
        val vidrio = e.extra.carpetasTono == "vidrio"
        val colorConsola = remember(e.consolaSel) { cl.nikalbazu.diptico.data.Consolas.porId(e.consolaSel)?.color ?: Uva }
        AnimatedVisibility(transicion, Modifier.align(Alignment.Center).padding(bottom = aire),
            enter = entradaCarpeta(ajusteMovimiento()), exit = salidaVentana(ajusteMovimiento())) {
            CompositionLocalProvider(LocalFondoVentanaCristal provides fondo,
                LocalPaleta provides paletaLecturaCristal(p, e.nivelesGlass.carpetas),
                LocalAireAbajo provides 0.dp) {
                val forma = androidx.compose.foundation.shape.RoundedCornerShape(esc.d(26))
                // Dos niveles de desenfoque: afuera (la biblioteca, 16 dp) y adentro de la ventana, donde se ve otra capa
                // de la biblioteca desenfocada un poco menos, con el color de la consola encima.
                var origenVentana by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                val capa = fondo.imagen
                Box(Modifier.fillMaxWidth(.94f).fillMaxHeight(.94f).testTag("carpeta-abierta")
                    .clip(forma)
                    .onGloballyPositioned { origenVentana = it.positionInRoot() }
                    .then(if (vidrio) Modifier.border(esc.d(1), androidx.compose.ui.graphics.Color.White.copy(alpha = .22f), forma) else Modifier.superficieApp(p.fondo2, forma, CapaCristal.CARPETA))
                ) {
                    if (vidrio && captura && capa != null) {
                        val extra = with(androidx.compose.ui.platform.LocalDensity.current) { 6.dp.toPx() }
                        Box(Modifier.matchParentSize()
                            .graphicsLayer { if (extra > 0.5f) renderEffect = androidx.compose.ui.graphics.BlurEffect(extra, extra, androidx.compose.ui.graphics.TileMode.Clamp) }
                            .drawBehind {
                                drawImage(capa,
                                    dstOffset = androidx.compose.ui.unit.IntOffset((fondo.origen.x - origenVentana.x).roundToInt(), (fondo.origen.y - origenVentana.y).roundToInt()),
                                    dstSize = fondo.dimensiones)
                            })
                    }
                    if (vidrio) Box(Modifier.matchParentSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(
                        colorConsola.copy(alpha = .30f), androidx.compose.ui.graphics.lerp(p.fondo, colorConsola, .12f).copy(alpha = .55f)))))
                    Column(Modifier.fillMaxSize().sinCerrar()) {
                        CabeceraSistema(e)
                    VistaJuegos(e, Modifier.fillMaxWidth().weight(1f), escalaEstante)
                    }
                }
            }
        }
    }
}

@Composable
internal fun CabeceraCarpetas(e: Estado, interactiva: Boolean) = EsquinasArriba(e)

package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Novedades

/**
 * Tour de novedades (una vez por versión): una tarjeta por novedad, con su ícono que flota,
 * el botón que la usa y puntos de avance. A o tocar sigue, B salta.
 */
@Composable
fun TourNovedades(e: Estado) {
    // La primera vez el recorrido lo cuenta DIPPO.
    if (e.recorrido) { RecorridoDippo(e); return }
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val paginas = Diptico.paginasTour(e)
    val k = e.novedades.coerceIn(0, paginas.size - 1)
    val flota = rememberInfiniteTransition(label = "flota")
    val y by flota.animateFloat(-4f, 4f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "y")
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).sinCerrar(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = esc.d(560)).fillMaxWidth(0.86f).clip(RoundedCornerShape(esc.d(30)))
                .background(p.fondo2).border(esc.d(1.5f), Color(paginas[k].color).copy(alpha = 0.5f), RoundedCornerShape(esc.d(30)))
                .clickable { Diptico.moverNovedades(1) },
        ) {
            AnimatedContent(
                targetState = k,
                transitionSpec = {
                    val d = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(260)) + slideInHorizontally(spring(dampingRatio = 0.8f, stiffness = 420f)) { it / 4 * d }) togetherWith
                        (fadeOut(tween(160)) + slideOutHorizontally(tween(200)) { -it / 5 * d })
                },
                label = "novedad",
            ) { i ->
                val pg = paginas[i]
                val c = Color(pg.color)
                Row(Modifier.fillMaxWidth().padding(esc.d(24)), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(esc.d(112)), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(esc.d(112)).background(Brush.radialGradient(listOf(c.copy(alpha = 0.35f), Color.Transparent))))
                        Box(
                            Modifier.size(esc.d(78)).graphicsLayer { translationY = y * density }.clip(CircleShape)
                                .background(Brush.linearGradient(listOf(c, androidx.compose.ui.graphics.lerp(c, Color.Black, 0.25f)))),
                            contentAlignment = Alignment.Center,
                        ) { IconoDiptico(pg.icono, Color.White, 38f, acento = Color.White) }
                    }
                    Spacer(Modifier.width(esc.d(20)))
                    Column(Modifier.weight(1f)) {
                        Etiqueta(
                            when {
                                e.recorrido -> if (i == 0) "Recorrido por Díptico" else "Paso $i de ${paginas.size - 1}"
                                i == 0 -> "Díptico ${Novedades.NOMBRE}"
                                paginas.map { it.version }.filter { it.isNotBlank() }.distinct().size > 1 -> "${pg.version} · Novedad $i de ${paginas.size - 1}"
                                else -> "Novedad $i de ${paginas.size - 1}"
                            },
                            c, 11.5f,
                        )
                        Spacer(Modifier.height(esc.d(4)))
                        Texto(pg.titulo, 24f, lineas = 2)
                        Spacer(Modifier.height(esc.d(6)))
                        Detalle(pg.texto, 14.5f, lineas = 5)
                        if (pg.tecla.isNotBlank()) {
                            Spacer(Modifier.height(esc.d(10)))
                            Box(Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = 0.18f)).padding(horizontal = esc.d(12), vertical = esc.d(4))) {
                                Texto(pg.tecla, 12.5f, color = c, lineas = 1)
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().background(p.superficie.copy(alpha = 0.5f)).padding(horizontal = esc.d(20), vertical = esc.d(10)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PuntosAvance(k, paginas.size, Color(paginas[k].color))
                Spacer(Modifier.weight(1f))
                Pistas("A" to if (k == paginas.size - 1) "¡A jugar!" else "Siguiente", "B" to "Saltar")
            }
        }
    }
}

@Composable
internal fun PuntosAvance(k: Int, n: Int, color: Color) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6)), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until n) {
            val ancho by androidx.compose.animation.core.animateDpAsState(esc.d(if (i == k) 22f else 8f), spring(dampingRatio = 0.7f), label = "punto")
            Box(Modifier.size(ancho, esc.d(8)).clip(RoundedCornerShape(50)).background(if (i <= k) color else p.linea))
        }
    }
}

/** En la otra pantalla durante el tour: solo el avance y los botones. */
@Composable
fun NovedadesCompania(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val paginas = Diptico.paginasTour(e)
    val k = e.novedades.coerceIn(0, paginas.size - 1)
    Box(Modifier.fillMaxSize().background(p.fondo.copy(alpha = 0.92f)).sinCerrar(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconoDiptico(Icono.ARRIBA, Color(paginas[k].color), 30f)
            Spacer(Modifier.height(esc.d(8)))
            Texto(if (e.recorrido) "DIPPO te cuenta arriba" else "Mira las novedades arriba", 20f, lineas = 1)
            Spacer(Modifier.height(esc.d(12)))
            PuntosAvance(k, paginas.size, Color(paginas[k].color))
            Spacer(Modifier.height(esc.d(14)))
            Pistas("A" to "Siguiente", "B" to "Saltar")
        }
    }
}

/**
 * ¿En qué pantalla lo abro?: la consola dibujada con sus dos pantallas; la elegida se ilumina.
 * Abajo, "Más opciones" (la ventana del juego, o fijar la app) y "Recordar para este juego".
 */
@Composable
fun VentanaDonde(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val d = e.donde ?: return
    val esJuego = d == "juego"
    val nombre = if (esJuego) e.seleccionado?.titulo ?: "" else e.apps.firstOrNull { it.paquete == d.removePrefix("app:") }?.nombre
        ?: cl.nikalbazu.diptico.emu.Lanzador.nombreApp(androidx.compose.ui.platform.LocalContext.current, d.removePrefix("app:")) ?: ""
    val crece = remember(d) { androidx.compose.animation.core.Animatable(0.85f) }
    LaunchedEffect(d) { crece.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 420f)) }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(interactionSource = null, indication = null) { Diptico.cerrarDonde() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.graphicsLayer { scaleX = crece.value; scaleY = crece.value; alpha = ((crece.value - 0.85f) / 0.15f).coerceIn(0f, 1f) }
                .widthIn(max = esc.d(420)).fillMaxWidth(0.92f).clip(RoundedCornerShape(esc.d(26))).background(p.fondo2)
                .border(esc.d(1.5f), Uva.copy(alpha = 0.45f), RoundedCornerShape(esc.d(26))).sinCerrar().padding(esc.d(16)),
        ) {
            Etiqueta("¿Dónde lo abro?", Coral, 11.5f)
            Texto(nombre, 19f, lineas = 1, traducir = false)
            Spacer(Modifier.height(esc.d(12)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // La consola: dos pantallas, una arriba de la otra.
                Column(Modifier.width(esc.d(150)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    PantallaMini(e.dondeSel == 0, 150f, 84f, "Arriba") { Diptico.confirmarDonde(0) }
                    PantallaMini(e.dondeSel == 1, 112f, 92f, "Abajo") { Diptico.confirmarDonde(1) }
                }
                Spacer(Modifier.width(esc.d(14)))
                Column(Modifier.weight(1f)) {
                    Texto(if (e.dondeSel == 1) "En la pantalla de abajo" else if (e.dondeSel == 0) "En la pantalla de arriba" else if (esJuego) "Ventana del juego" else "Fijar o quitar la app", 15.5f, lineas = 2)
                    Detalle(
                        when (e.dondeSel) {
                            0 -> "La grande, la de siempre para jugar."
                            1 -> "Ideal para apps, guías, videos o un juego mientras arriba sigues con otra cosa."
                            else -> if (esJuego) "Ficha, favoritos, emulador, imágenes y más." else "Las fijadas aparecen primero."
                        },
                        12.5f, lineas = 3,
                    )
                    Spacer(Modifier.height(esc.d(10)))
                    val masSel = e.dondeSel == 2
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(if (masSel) Uva else p.superficie).clickable { Diptico.confirmarDonde(2) }
                            .padding(horizontal = esc.d(12), vertical = esc.d(6)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconoDiptico(if (esJuego) Icono.MENU else Icono.ESTRELLA, if (masSel) SobreUva else p.tinta, 14f, acento = Coral)
                        Spacer(Modifier.width(esc.d(6)))
                        Texto(if (esJuego) "Más opciones" else "Fijar", 12.5f, color = if (masSel) SobreUva else p.tinta, lineas = 1)
                    }
                    if (esJuego) {
                        Spacer(Modifier.height(esc.d(8)))
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).clickable { Diptico.alternarRecordarDonde() }.padding(vertical = esc.d(3)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(esc.d(18)).clip(RoundedCornerShape(esc.d(5)))
                                    .background(if (e.dondeRecordar) Coral else Color.Transparent).border(esc.d(1.5f), if (e.dondeRecordar) Coral else p.tenue, RoundedCornerShape(esc.d(5))),
                                contentAlignment = Alignment.Center,
                            ) { if (e.dondeRecordar) IconoDiptico(Icono.CHECK, Color.White, 12f) }
                            Spacer(Modifier.width(esc.d(6)))
                            Detalle("Recordar para este juego", 12.5f, lineas = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.height(esc.d(12)))
            Pistas("↑ ↓" to "Elegir", "A" to "Abrir", *(if (esJuego) arrayOf("Y" to "Recordar") else emptyArray<Pair<String, String>>()), "B" to "Cancelar")
        }
    }
}

@Composable
private fun PantallaMini(elegida: Boolean, ancho: Float, alto: Float, texto: String, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val brillo by androidx.compose.animation.core.animateFloatAsState(if (elegida) 1f else 0f, spring(dampingRatio = 0.6f), label = "brillo")
    Box(
        Modifier.size(esc.d(ancho * (0.92f + 0.08f * brillo)), esc.d(alto * (0.92f + 0.08f * brillo))).clip(RoundedCornerShape(esc.d(12)))
            .background(Color(0xFF15131F)).border(esc.d(2f + 1.5f * brillo), if (elegida) Uva else p.linea, RoundedCornerShape(esc.d(12)))
            .clickable { alPulsar() }.padding(esc.d(5)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxSize().clip(RoundedCornerShape(esc.d(8)))
                .background(Brush.linearGradient(listOf(Uva.copy(alpha = 0.25f + 0.55f * brillo), Coral.copy(alpha = 0.15f + 0.45f * brillo)))),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (elegida) IconoDiptico(Icono.JUGAR, Color.White, 18f)
                Texto(texto, 12f, color = Color.White.copy(alpha = 0.6f + 0.4f * brillo), lineas = 1, alinear = TextAlign.Center)
            }
        }
    }
}

/** En la pantalla de arriba, mientras se elige: un marco que dice "aquí" si es la elegida. */
@Composable
fun MarcoDonde(aqui: Boolean) {
    val esc = LocalEscala.current
    val a by androidx.compose.animation.core.animateFloatAsState(if (aqui) 1f else 0f, tween(220), label = "marco")
    if (a <= 0.01f) return
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = a }.padding(esc.d(6)).border(esc.d(4), Uva, RoundedCornerShape(esc.d(18)))) {
        Box(Modifier.align(Alignment.TopCenter).padding(top = esc.d(10)).clip(RoundedCornerShape(50)).background(Uva).padding(horizontal = esc.d(14), vertical = esc.d(5))) {
            Texto("Se abrirá aquí", 13f, color = SobreUva, lineas = 1)
        }
    }
}


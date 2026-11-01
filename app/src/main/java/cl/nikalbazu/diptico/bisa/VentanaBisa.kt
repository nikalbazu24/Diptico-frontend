package cl.nikalbazu.diptico.bisa

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import cl.nikalbazu.diptico.ui.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.ui.Coral
import cl.nikalbazu.diptico.ui.Detalle
import cl.nikalbazu.diptico.ui.LocalEscala
import cl.nikalbazu.diptico.ui.LocalPaleta
import cl.nikalbazu.diptico.ui.Lienzo
import cl.nikalbazu.diptico.ui.SobreUva
import cl.nikalbazu.diptico.ui.Texto
import cl.nikalbazu.diptico.ui.Uva
import cl.nikalbazu.diptico.ui.dibujarBisa
import cl.nikalbazu.diptico.ui.fondoBoton

/** Bisa animada: parpadea y flota suave. Es su cara en todo Díptico. */
@Composable
fun AvatarBisa(tamDp: Float, modifier: Modifier = Modifier, alerta: Boolean = false) {
    val t = rememberInfiniteTransition(label = "bisa")
    val parpadeo by t.animateFloat(
        1f, 1f,
        infiniteRepeatable(keyframes { durationMillis = 3800; 1f at 3500; 0.1f at 3620; 1f at 3760 }),
        label = "parpadeo",
    )
    val flotar by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "flotar")
    val pulso by t.animateFloat(1f, 1.35f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulso")
    Box(modifier.size(tamDp.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize().graphicsLayer { translationY = flotar * tamDp * 0.04f * density }
                .shadow(6.dp, CircleShape).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF2A2146), Color(0xFF14111F)))),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size((tamDp * 0.86f).dp)) {
                dibujarBisa(size.width / 24f, Uva, Coral, parpadeo = parpadeo, mirar = flotar * 0.4f)
            }
        }
        if (alerta) {
            Box(
                Modifier.align(Alignment.TopEnd).size((tamDp * 0.26f).dp).graphicsLayer { scaleX = pulso; scaleY = pulso }
                    .clip(CircleShape).background(Coral).border(2.dp, Color.White, CircleShape),
            )
        }
    }
}

/** La burbuja flotante y su ventanita de ayuda, encima del juego. */
@Composable
fun VentanaBisa(mover: (Float, Float) -> Unit, cerrar: () -> Unit) {
    val e by Diptico.estado.collectAsState()
    if (!e.burbujaAbierta) {
        AvatarBisa(
            60f,
            Modifier.padding(6.dp)
                .pointerInput(Unit) { detectDragGestures { cambio, d -> cambio.consume(); mover(d.x, d.y) } }
                .clickable { Diptico.burbujaExpandida(true, false) },
            alerta = e.burbujaAlerta,
        )
        return
    }
    Lienzo(true, Modifier.size(340.dp, 430.dp).padding(6.dp).shadow(12.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))) {
        val p = LocalPaleta.current
        val esc = LocalEscala.current
        val ultima = e.chat.lastOrNull { it.rol == "ia" }
        val juego = e.sesion?.let { s -> e.juegos.firstOrNull { it.clave == s.clave } }
        Column(Modifier.fillMaxSize().background(p.fondo2).padding(esc.d(12))) {
            Row(
                Modifier.fillMaxWidth().pointerInput(Unit) { detectDragGestures { cambio, d -> cambio.consume(); mover(d.x, d.y) } },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarBisa(34f)
                Spacer(Modifier.width(esc.d(8)))
                Column(Modifier.weight(1f)) {
                    Texto("Bisa", 17f, lineas = 1)
                    Detalle(juego?.titulo ?: "Tu asistente", 11.5f, lineas = 1)
                }
                Box(Modifier.size(esc.d(30)).clip(CircleShape).background(p.superficie).clickable { cerrar() }, contentAlignment = Alignment.Center) {
                    Texto("✕", 13f, traducir = false)
                }
            }
            Spacer(Modifier.height(esc.d(8)))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when {
                    e.pensando -> Detalle("Pensando…", 14f)
                    ultima != null -> Detalle(ultima.texto, 14f, color = p.tinta)
                    else -> Detalle("¿En qué parte vas? Te ayudo con una pista suave, sin arruinarte la sorpresa.", 14f, color = p.tinta)
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            val botones = buildList {
                ultima?.acciones?.forEach { a -> add(a.etiqueta to { Diptico.ejecutarAccion(a) }) }
                if (isEmpty()) {
                    add("Estoy atascado" to { Diptico.preguntar("Estoy atascado en ${juego?.titulo ?: "este juego"}. Dame una pista suave.") })
                    add("Me matan siempre en lo mismo" to { Diptico.preguntar("En ${juego?.titulo ?: "este juego"} me matan una y otra vez en la misma parte. ¿Qué hago?") })
                    add("Un truco para este juego" to { Diptico.preguntar("Dame un buen truco o consejo para ${juego?.titulo ?: "este juego"}.") })
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
                botones.take(3).forEach { (t, f) ->
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(p.superficie).clickable { f() }
                            .padding(horizontal = esc.d(12), vertical = esc.d(7)),
                    ) { Texto(t, 13f, lineas = 1) }
                }
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).fondoBoton(Uva).clickable { Diptico.burbujaExpandida(false, false) }
                        .padding(horizontal = esc.d(12), vertical = esc.d(7)),
                    contentAlignment = Alignment.Center,
                ) { Texto("‹  Volver al juego", 13f, color = SobreUva, lineas = 1) }
            }
        }
    }
}

package cl.nikalbazu.diptico.ui

import android.content.Context
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Opcion
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatoTiempo(segundos: Long): String {
    if (segundos < 60) return "menos de 1 min"
    val h = segundos / 3600
    val m = (segundos % 3600) / 60
    return if (h > 0) "$h h ${m.toString().padStart(2, '0')} min" else "$m min"
}

@Composable
fun rememberHora(): String {
    fun ahora() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    var hora by remember { mutableStateOf(ahora()) }
    LaunchedEffect(Unit) {
        while (true) {
            hora = ahora()
            delay(10_000)
        }
    }
    return hora
}

private fun bateria(ctx: Context): Int =
    ctx.getSystemService(BatteryManager::class.java)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

@Composable
fun rememberBateria(): Int {
    val ctx = LocalContext.current
    var nivel by remember { mutableIntStateOf(bateria(ctx)) }
    LaunchedEffect(Unit) {
        while (true) {
            nivel = bateria(ctx)
            delay(60_000)
        }
    }
    return nivel
}

/** Letra chica en mayúsculas, para etiquetas. */
@Composable
fun Etiqueta(texto: String, color: Color = LocalPaleta.current.tenue, tam: Float = 12f, modifier: Modifier = Modifier) {
    Texto(texto.uppercase(), tam, modifier, color = color, familia = Nunito, peso = FontWeight.ExtraBold, espaciado = 1.4f, lineas = 1)
}

/** Texto secundario en Nunito. */
@Composable
fun Detalle(texto: String, tam: Float = 14f, color: Color = LocalPaleta.current.tenue, lineas: Int = Int.MAX_VALUE, modifier: Modifier = Modifier) {
    Texto(texto, tam, modifier, color = color, familia = Nunito, peso = FontWeight.Bold, lineas = lineas, interlineado = tam * 1.38f)
}

/** Una tecla del control dibujada como píldora, con lo que hace al lado. */
@Composable
fun Pista(tecla: String, texto: String, color: Color = LocalPaleta.current.tinta) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.height(esc.d(22)).border(esc.d(1.8f), p.tenue, RoundedCornerShape(50)).padding(horizontal = esc.d(7)),
            contentAlignment = Alignment.Center,
        ) { Texto(tecla, 11f, color = color) }
        Spacer(Modifier.width(esc.d(6)))
        Texto(texto, 13f, color = color, familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1)
    }
}

@Composable
fun Pistas(vararg pares: Pair<String, String>, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(esc.d(14)), verticalAlignment = Alignment.CenterVertically) {
        pares.forEach { (t, x) -> Pista(t, x) }
    }
}

/** Botón grande, cómodo para el dedo. */
@Composable
fun BotonGrande(texto: String, fondo: Color, tinta: Color, modifier: Modifier = Modifier, alto: Float = 56f, accion: () -> Unit) {
    val esc = LocalEscala.current
    Box(
        modifier.height(esc.d(alto)).clip(RoundedCornerShape(esc.d(18))).background(fondo).clickable { accion() },
        contentAlignment = Alignment.Center,
    ) { Texto(texto, 16f, color = tinta, lineas = 1) }
}

/**
 * Una opción grande: ícono en un círculo, título y detalle. Se usa en el tutorial y en los menús.
 * La elegida se pinta uva y muestra la "A".
 */
@Composable
fun TarjetaOpcion(o: Opcion, activa: Boolean, modifier: Modifier = Modifier, alto: Float = 60f, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val fondo by animateColorAsState(if (activa) Uva else p.superficie.copy(alpha = if (p == PaletaOscura) 0.75f else 0.95f), label = "fondo")
    val alza by animateFloatAsState(if (activa) 1.015f else 1f, spring(dampingRatio = 0.55f), label = "alza")
    Row(
        modifier.fillMaxWidth().height(esc.d(alto))
            .graphicsLayer { scaleX = alza; scaleY = alza; alpha = if (o.apagada) 0.5f else 1f }
            .clip(RoundedCornerShape(esc.d(alto * 0.32f))).background(fondo)
            .clickable { alPulsar() }
            .padding(start = esc.d(10), end = esc.d(12)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (o.icono != null) {
            Box(
                Modifier.size(esc.d(alto - 20f)).clip(CircleShape).background(if (activa) Color.White else p.fondo),
                contentAlignment = Alignment.Center,
            ) {
                IconoDiptico(o.icono, if (activa) Uva else p.tinta, tam = (alto - 20f) * 0.56f, acento = Coral)
            }
            Spacer(Modifier.width(esc.d(12)))
        } else {
            Spacer(Modifier.width(esc.d(8)))
        }
        Column(Modifier.weight(1f)) {
            Texto(o.titulo, 16.5f, color = if (activa) Color.White else p.tinta, lineas = 1)
            if (o.valor.isNotEmpty()) {
                Texto(
                    o.valor, 13f, color = if (activa) Color.White.copy(alpha = 0.86f) else p.tenue,
                    familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1,
                )
            }
        }
        if (activa) {
            Spacer(Modifier.width(esc.d(8)))
            Box(Modifier.size(esc.d(28)).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Texto("A", 13f, color = Uva)
            }
        }
    }
}

/** Fondo oscuro detrás de un panel; tocarlo cierra el panel. */
@Composable
fun Velo(contenido: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xA6000000)).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
        ) { Diptico.cerrarPanel() },
        contentAlignment = Alignment.Center,
    ) { contenido() }
}

/** Evita que un toque dentro del panel lo cierre. */
fun Modifier.sinCerrar(): Modifier = this.clickable(interactionSource = MutableInteractionSource(), indication = null) {}

@Composable
fun Aviso(texto: String?, modifier: Modifier, abajo: Float = 70f) {
    val esc = LocalEscala.current
    AnimatedVisibility(
        visible = texto != null,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier.padding(bottom = esc.d(abajo)),
    ) {
        Box(
            Modifier.padding(horizontal = esc.d(20)).clip(RoundedCornerShape(esc.d(16))).background(Tinta.copy(alpha = 0.95f))
                .padding(horizontal = esc.d(16), vertical = esc.d(11)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(esc.d(9)).clip(CircleShape).background(Coral))
                Spacer(Modifier.width(esc.d(10)))
                Texto(texto ?: "", 14.5f, color = Color.White, familia = Nunito, peso = FontWeight.ExtraBold, lineas = 3)
            }
        }
    }
}

/** Logo, hora, batería y lo que está sonando. */
@Composable
fun BarraSuperior(modifier: Modifier, inicio: Float = 28f, extra: @Composable () -> Unit = {}) {
    val esc = LocalEscala.current
    val hora = rememberHora()
    val bat = rememberBateria()
    Row(
        modifier.fillMaxWidth().padding(start = esc.d(inicio), end = esc.d(24), top = esc.d(16)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Logo(15f)
        Spacer(Modifier.weight(1f))
        extra()
        Texto(hora, 15f, sombra = true)
        if (bat >= 0) {
            Spacer(Modifier.width(esc.d(14)))
            Texto("$bat%", 15f, sombra = true)
        }
    }
}

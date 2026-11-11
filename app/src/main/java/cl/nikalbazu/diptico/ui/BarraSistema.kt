package cl.nikalbazu.diptico.ui

import cl.nikalbazu.diptico.estado.*

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import kotlinx.coroutines.delay

/**
 * Fondo de una barra según el estilo elegido para su pantalla: sólida (color del tema), difusa (material
 * esmerilado sobre el arte) o transparente (casi sin fondo, solo una sombra suave para que se lea).
 */
@Composable
internal fun Modifier.fondoBarra(estilo: String, forma: Shape = RectangleShape): Modifier {
    val p = LocalPaleta.current
    val plana = forma == RectangleShape
    return when (estilo) {
        "transparente" -> if (plana) background(Brush.verticalGradient(listOf(p.fondo.copy(alpha = 0.35f), Color.Transparent)))
            else clip(forma).background(p.fondo.copy(alpha = 0.28f))
        "difusa" -> superficieCristal(forma, CapaCristal.PANEL, opacidadFondo = 0.8f)
        else -> clip(forma).background(p.fondo2.copy(alpha = if (plana) 1f else 0.94f))
    }
}

/** Un único propietario por pantalla: los paneles y sus transiciones quedan debajo. */
internal val LocalBarraSistema = staticCompositionLocalOf { false }
internal const val ALTO_BARRA_SISTEMA = 26f

/** ¿La barra de la hora ocupa su propio espacio aquí? La flotante solo lo reserva con un panel abierto. */
internal fun barraOcupaEspacio(e: Estado, arriba: Boolean = true): Boolean =
    e.extra.formaBarra == "completa" || e.panel != cl.nikalbazu.diptico.estado.Panel.NINGUNO || !arriba

/** Colores de los íconos de la barra: cada uno con el suyo (batería, avisos, wifi, hora). */
internal data class ColoresBarra(val bateria: (Int, Boolean) -> Color, val avisos: Color, val wifi: Color, val hora: Color)

@Composable
internal fun coloresBarra(estilo: String): ColoresBarra {
    val p = LocalPaleta.current
    fun nivel(alto: Color, medio: Color, bajo: Color, carga: Color) = { n: Int, c: Boolean ->
        when { c -> carga; n in 0..15 -> bajo; n in 16..40 -> medio; else -> alto }
    }
    return when (estilo) {
        "tema" -> ColoresBarra({ n, c -> if (c) Sol else if (n in 0..15) Coral else p.tinta }, p.tenue, p.tenue, p.tinta)
        "pastel" -> ColoresBarra(nivel(Color(0xFF9EE6B8), Color(0xFFFFD99A), Color(0xFFFFA8A8), Color(0xFFB8F0FF)),
            Color(0xFFFFB3C7), Color(0xFFA9D4FF), p.tinta)
        "neon" -> ColoresBarra(nivel(Color(0xFF39FF88), Color(0xFFFFE53B), Color(0xFFFF3B6B), Color(0xFF3BF5FF)),
            Color(0xFFFF4FD8), Color(0xFF4FB8FF), Color(0xFFE9FFFE))
        else -> ColoresBarra(nivel(Color(0xFF4ADE80), Color(0xFFFBBF24), Color(0xFFF87171), Color(0xFF38BDF8)),
            Color(0xFFFF7A9C), Color(0xFF60A5FA), p.tinta)
    }
}

/** ¿Hay red, y es wifi? Se revisa cada 20 s (no pide permisos nuevos). */
private fun red(ctx: Context): Int {
    val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return 0
    val cap = runCatching { cm.getNetworkCapabilities(cm.activeNetwork) }.getOrNull() ?: return 0
    return when {
        cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 2
        cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> 1
        else -> 0
    }
}

@Composable
private fun rememberRed(): Int {
    val ctx = LocalContext.current
    var r by remember { mutableIntStateOf(red(ctx)) }
    LaunchedEffect(Unit) { while (true) { r = red(ctx); delay(20_000) } }
    return r
}

@Composable
internal fun BarraSistema(e: Estado, modifier: Modifier = Modifier, estilo: String = "solida") {
    val hora = rememberHora()
    val bateria = rememberBateria()
    val cargando = rememberCargando()
    val red = rememberRed()
    val avisos by cl.nikalbazu.diptico.audio.Avisos.lista.collectAsState()
    val c = coloresBarra(e.extra.coloresBarra)
    BarraCompleta(e, modifier, estilo, hora, bateria, cargando, red, avisos.size, c)
}

/** La barra de siempre: una franja de lado a lado con su propio espacio. */
@Composable
private fun BarraCompleta(e: Estado, modifier: Modifier, estilo: String, hora: String, bateria: Int, cargando: Boolean, red: Int, avisos: Int, c: ColoresBarra) {
    val esc = LocalEscala.current
    Row(modifier.fillMaxWidth().height(esc.d(ALTO_BARRA_SISTEMA)).fondoBarra(estilo)
        .padding(horizontal = esc.d(14)).testTag("barra-sistema"), verticalAlignment = Alignment.CenterVertically) {
        Logo(10f)
        Spacer(Modifier.width(esc.d(10)))
        PildoraTrabajos(e.trabajos)
        Spacer(Modifier.weight(1f))
        IconosEstado(hora, bateria, cargando, red, avisos, c)
    }
}

@Composable
private fun IconosEstado(hora: String, bateria: Int, cargando: Boolean, red: Int, avisos: Int, c: ColoresBarra) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    // Avisos: la campana toma su color cuando hay algo nuevo, con el número al lado.
    Row(Modifier.clip(RoundedCornerShape(50)).clickable(onClickLabel = "Abrir notificaciones") { Diptico.abrirCentro("avisos") }
        .padding(horizontal = esc.d(3), vertical = esc.d(2)), verticalAlignment = Alignment.CenterVertically) {
        IconoDiptico(Icono.CAMPANA, if (avisos > 0) c.avisos else p.tenue, 12f)
        if (avisos > 0) {
            Spacer(Modifier.width(esc.d(2)))
            Box(Modifier.clip(CircleShape).background(c.avisos).padding(horizontal = esc.d(4)), contentAlignment = Alignment.Center) {
                Texto(if (avisos > 9) "9+" else "$avisos", 9f, color = Color(0xFF14121C), traducir = false, lineas = 1)
            }
        }
    }
    Spacer(Modifier.width(esc.d(7)))
    Box(Modifier.semantics { contentDescription = when (red) { 2 -> "Wi-Fi conectado"; 1 -> "Conectado"; else -> "Sin conexión" } }) {
        IconoDiptico(Icono.WIFI, if (red > 0) c.wifi else p.tenue.copy(alpha = 0.5f), 12f)
    }
    Spacer(Modifier.width(esc.d(8)))
    Texto(hora, 11.5f, color = c.hora, traducir = false, lineas = 1,
        modifier = Modifier.testTag("hora-sistema").clickable(onClickLabel = "Abrir reloj") { Diptico.alternarReloj() })
    Spacer(Modifier.width(esc.d(8)))
    Row(Modifier.testTag("bateria-sistema").semantics {
        contentDescription = if (bateria >= 0) "Batería $bateria por ciento" else "Batería no disponible"
    }, verticalAlignment = Alignment.CenterVertically) {
        PilaBateria(bateria, cargando, c.bateria(bateria, cargando))
        Spacer(Modifier.width(esc.d(4)))
        Texto(if (bateria >= 0) "$bateria%" else "—", 11f, color = p.tinta, traducir = false, lineas = 1)
    }
}

/** Pila dibujada: se llena según la carga, cambia de color por nivel y late suave mientras carga. */
@Composable
private fun PilaBateria(nivel: Int, cargando: Boolean, color: Color) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val tono by animateColorAsState(color, tween(400), label = "pila")
    val latido = if (cargando && LocalMovimiento.current) {
        val t = rememberInfiniteTransition(label = "carga")
        t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "latido").value
    } else 1f
    Canvas(Modifier.size(esc.d(20), esc.d(10))) {
        val trazo = 1.2f * density
        val cuerpo = Size(size.width - 2.5f * density, size.height)
        drawRoundRect(p.tinta.copy(alpha = 0.55f), Offset.Zero, cuerpo, CornerRadius(3f * density), style = Stroke(trazo))
        drawRoundRect(p.tinta.copy(alpha = 0.55f), Offset(cuerpo.width + 0.6f * density, size.height * 0.3f),
            Size(1.9f * density, size.height * 0.4f), CornerRadius(1f * density))
        val f = (if (nivel < 0) 0.5f else nivel / 100f).coerceIn(0.06f, 1f)
        val m = 2.2f * density
        drawRoundRect(tono.copy(alpha = latido), Offset(m, m), Size((cuerpo.width - 2 * m) * f, cuerpo.height - 2 * m), CornerRadius(1.6f * density))
    }
}

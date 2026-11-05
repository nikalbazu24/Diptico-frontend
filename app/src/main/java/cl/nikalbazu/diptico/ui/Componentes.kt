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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Descargas
import cl.nikalbazu.diptico.data.Medio
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Opcion
import cl.nikalbazu.diptico.estado.Trabajo
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

/** ¿Está cargando? */
fun cargando(ctx: Context): Boolean =
    ctx.getSystemService(BatteryManager::class.java)?.isCharging ?: false

@Composable
fun rememberCargando(): Boolean {
    val ctx = LocalContext.current
    var c by remember { mutableStateOf(cargando(ctx)) }
    LaunchedEffect(Unit) {
        while (true) {
            c = cargando(ctx)
            delay(30_000)
        }
    }
    return c
}

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
    val idioma = cl.nikalbazu.diptico.estado.Idioma.actual
    val t = if (idioma == "es") texto else cl.nikalbazu.diptico.estado.Idioma.t(texto)
    Texto(t.uppercase(cl.nikalbazu.diptico.estado.Idioma.locale), tam, modifier, color = color, familia = Nunito, peso = FontWeight.ExtraBold, espaciado = if (idioma == "zh" || idioma == "ja") 0.6f else 1.4f, lineas = 1, traducir = false)
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
    // Un poquito de aire arriba y abajo: así el contorno del botón nunca queda cortado por el borde de la pantalla o del panel.
    Row(Modifier.padding(vertical = esc.d(2)), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.heightIn(min = esc.d(22)).widthIn(min = esc.d(22)).padding(esc.d(1)).border(esc.d(1.6f), p.tenue, RoundedCornerShape(50)).padding(horizontal = esc.d(6)),
            contentAlignment = Alignment.Center,
        ) { Texto(tecla, 11f, color = color, lineas = 1) }
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
 * Fondo de un botón elegido según el efecto del tema: liso, degradado, con un brillo que pasa, o de vidrio.
 * Va después de clip(forma).
 */
@Composable
fun Modifier.fondoBoton(color: Color): Modifier = if (Estilo.tema.forma == "pixel") this.background(color).drawWithContent {
    // Botón de bloque: color parejo con borde claro arriba-izquierda y oscuro abajo-derecha.
    drawContent()
    val b = 2.5f * density
    drawRect(Color.White.copy(alpha = 0.28f), size = androidx.compose.ui.geometry.Size(size.width, b))
    drawRect(Color.White.copy(alpha = 0.18f), size = androidx.compose.ui.geometry.Size(b, size.height))
    drawRect(Color.Black.copy(alpha = 0.3f), topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - b), size = androidx.compose.ui.geometry.Size(size.width, b))
    drawRect(Color.Black.copy(alpha = 0.22f), topLeft = androidx.compose.ui.geometry.Offset(size.width - b, 0f), size = androidx.compose.ui.geometry.Size(b, size.height))
} else if (Estilo.tema.forma == "cristal") {
    this.background(Brush.verticalGradient(listOf(
        androidx.compose.ui.graphics.lerp(color, Color.White, 0.29f).copy(alpha = 0.87f),
        color.copy(alpha = 0.72f),
        androidx.compose.ui.graphics.lerp(color, Color.Black, 0.18f).copy(alpha = 0.86f),
    ))).drawWithContent {
        drawContent()
        drawLine(Color.White.copy(alpha = 0.52f), androidx.compose.ui.geometry.Offset.Zero,
            androidx.compose.ui.geometry.Offset(size.width, 0f), 2f * density)
    }
} else when (Estilo.efecto) {
    "degradado" -> this.background(
        Brush.linearGradient(listOf(androidx.compose.ui.graphics.lerp(color, Color.White, 0.2f), color, androidx.compose.ui.graphics.lerp(color, Color.Black, 0.28f))),
    )
    "brillo" -> {
        val inf = androidx.compose.animation.core.rememberInfiniteTransition(label = "brillo")
        val x by inf.animateFloat(
            -0.6f, 1.6f,
            androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2400, delayMillis = 600)),
            label = "x",
        )
        this.background(color).drawWithContent {
            drawContent()
            val w = size.width
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.3f), Color.Transparent),
                    start = androidx.compose.ui.geometry.Offset(w * x - w * 0.25f, 0f),
                    end = androidx.compose.ui.geometry.Offset(w * x + w * 0.05f, size.height),
                ),
            )
        }
    }
    "vidrio" -> this.background(color.copy(alpha = 0.55f))
        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent, Color.Black.copy(alpha = 0.12f))))
    else -> this.background(color)
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
            .clip(RoundedCornerShape(esc.d(alto * 0.32f)))
            .then(if (activa) Modifier.fondoBoton(fondo) else Modifier.background(fondo))
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
            Texto(o.titulo, 16.5f, color = if (activa) SobreUva else p.tinta, lineas = 1)
            if (o.valor.isNotEmpty()) {
                Texto(
                    o.valor, 13f, color = if (activa) SobreUva.copy(alpha = 0.86f) else p.tenue,
                    familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1,
                )
            }
        }
        if (activa) {
            Spacer(Modifier.width(esc.d(8)))
            Box(Modifier.size(esc.d(28)).clip(CircleShape).background(SobreUva), contentAlignment = Alignment.Center) {
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

/** Logo, trabajos de fondo, hora y batería. Tocar la hora agranda el reloj. */
@Composable
fun BarraSuperior(modifier: Modifier, inicio: Float = 28f, trabajos: List<Trabajo> = emptyList(), extra: @Composable () -> Unit = {}) {
    val esc = LocalEscala.current
    val hora = rememberHora()
    val bat = rememberBateria()
    Row(
        modifier.fillMaxWidth().padding(start = esc.d(inicio), end = esc.d(24), top = esc.d(16)),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.padding(top = esc.d(4))) { Logo(15f) }
        Spacer(Modifier.width(esc.d(14)))
        PildoraTrabajos(trabajos)
        Spacer(Modifier.weight(1f))
        extra()
        Campana()
        Row(
            Modifier.clip(RoundedCornerShape(50)).clickable { Diptico.alternarReloj() }.padding(horizontal = esc.d(6), vertical = esc.d(2)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Texto(hora, 15f, sombra = true)
            if (bat >= 0) {
                Spacer(Modifier.width(esc.d(12)))
                Texto("$bat%", 15f, sombra = true)
            }
        }
    }
}

/** Campana de notificaciones con cuántas hay. Abre el centro de control. */
@Composable
private fun Campana() {
    val esc = LocalEscala.current
    val avisos by cl.nikalbazu.diptico.audio.Avisos.lista.collectAsState()
    Box(
        Modifier.padding(end = esc.d(8)).size(esc.d(30)).clip(CircleShape).clickable { Diptico.abrirCentro("avisos") },
        contentAlignment = Alignment.Center,
    ) {
        IconoDiptico(cl.nikalbazu.diptico.estado.Icono.CAMPANA, LocalPaleta.current.tinta, 18f, acento = Coral)
        if (avisos.isNotEmpty()) {
            Box(
                Modifier.align(Alignment.TopEnd).size(esc.d(15)).clip(CircleShape).background(Coral),
                contentAlignment = Alignment.Center,
            ) { Texto(if (avisos.size > 9) "9+" else "${avisos.size}", 9f, color = Color.White, lineas = 1) }
        }
    }
}

/** Aviso de lo que se hace de fondo: buscar juegos, bajar portadas… Tocándolo se ve el detalle. */
@Composable
fun PildoraTrabajos(trabajos: List<Trabajo>) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    var abierta by remember { mutableStateOf(false) }
    AnimatedVisibility(
        visible = trabajos.isNotEmpty(),
        enter = fadeIn() + androidx.compose.animation.expandHorizontally(),
        exit = fadeOut() + androidx.compose.animation.shrinkHorizontally(),
    ) {
        val primero = trabajos.firstOrNull() ?: return@AnimatedVisibility
        Column(
            Modifier.clip(RoundedCornerShape(esc.d(16))).background(p.superficie.copy(alpha = 0.92f))
                .border(esc.d(1), p.linea, RoundedCornerShape(esc.d(16)))
                .clickable { abierta = !abierta }
                .padding(horizontal = esc.d(10), vertical = esc.d(6))
                .animateContentSize(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Girador(13f)
                Spacer(Modifier.width(esc.d(8)))
                Texto(
                    primero.texto + if (trabajos.size > 1) "  +${trabajos.size - 1}" else "", 12.5f,
                    familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1,
                    modifier = Modifier.widthIn(max = esc.d(240)),
                )
            }
            if (abierta) {
                for (t in trabajos) {
                    Spacer(Modifier.height(esc.d(6)))
                    Texto(t.texto, 12.5f, color = p.tenue, familia = Nunito, peso = FontWeight.Bold, lineas = 1, modifier = Modifier.widthIn(max = esc.d(260)))
                    if (t.avance != null) {
                        Spacer(Modifier.height(esc.d(3)))
                        Box(Modifier.width(esc.d(200)).height(esc.d(4)).clip(RoundedCornerShape(50)).background(p.linea)) {
                            Box(Modifier.fillMaxWidth(t.avance.coerceIn(0f, 1f)).height(esc.d(4)).clip(RoundedCornerShape(50)).background(Uva))
                        }
                    }
                }
            } else if (primero.avance != null) {
                Spacer(Modifier.height(esc.d(4)))
                Box(Modifier.width(esc.d(120)).height(esc.d(3)).clip(RoundedCornerShape(50)).background(p.linea)) {
                    Box(Modifier.fillMaxWidth(primero.avance.coerceIn(0f, 1f)).height(esc.d(3)).clip(RoundedCornerShape(50)).background(Uva))
                }
            }
        }
    }
}

/** Reloj a pantalla completa: se abre tocando la hora y se cierra con un toque o cualquier botón. */
@Composable
fun RelojGrande(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val hora = rememberHora()
    val bat = rememberBateria()
    var segundos by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            segundos = (System.currentTimeMillis() / 1000 % 60).toInt()
            delay(1000)
        }
    }
    Box(
        Modifier.fillMaxSize().background(p.fondo).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
        ) { Diptico.alternarReloj() },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(esc.d(520)).background(Brush.radialGradient(listOf(Uva.copy(alpha = 0.25f), Color.Transparent))))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Texto(hora, if (esc.angosta) 110f else 150f, interlineado = if (esc.angosta) 112f else 150f)
                Spacer(Modifier.width(esc.d(8)))
                Texto(segundos.toString().padStart(2, '0'), 34f, color = Coral, modifier = Modifier.padding(bottom = esc.d(22)))
            }
            Detalle(
                cl.nikalbazu.diptico.estado.Idioma.fecha("EEEEdMMMM").replaceFirstChar { it.uppercase() },
                22f, color = p.tinta, lineas = 1,
            )
            Spacer(Modifier.height(esc.d(10)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (bat >= 0) {
                    Box(Modifier.width(esc.d(46)).height(esc.d(20)).clip(RoundedCornerShape(esc.d(6))).border(esc.d(2), p.tenue, RoundedCornerShape(esc.d(6))).padding(esc.d(3))) {
                        Box(Modifier.fillMaxWidth(bat.coerceIn(0, 100) / 100f).fillMaxSize().clip(RoundedCornerShape(esc.d(3))).background(if (bat < 20) Coral else Uva))
                    }
                    Spacer(Modifier.width(esc.d(8)))
                    Texto("$bat%", 18f, color = p.tenue)
                }
                e.fiesta?.let { f ->
                    Spacer(Modifier.width(esc.d(18)))
                    Texto(f.tema.mensaje.ifBlank { f.nombre }, 18f, color = Sol, lineas = 1)
                }
            }
        }
        Detalle("Toca o aprieta un botón para cerrar", 13f, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(18)))
    }
}

private val SIGLAS = mapOf(
    "switch" to "NSW", "xbox360" to "360", "xboxone" to "ONE", "psvita" to "Vita", "wiiu" to "WiiU", "n3ds" to "3DS",
    "megadrive" to "MD", "genesis" to "GEN", "dreamcast" to "DC", "saturn" to "SAT", "androidgames" to "APK", "ports" to "PRT",
)

/** Ajustes de íconos de consola, para que cualquier ícono los lea sin pasar el estado. */
data class IconosCfg(val estilo: String = "diptico", val propios: Map<String, String> = emptyMap(), val version: Int = 0)

val LocalIconos = staticCompositionLocalOf { IconosCfg() }

/**
 * Ícono de una consola según el estilo elegido: imagen propia, paquete de RetroArch, siglas o el dibujo de Díptico.
 */
@Composable
fun IconoConsola(c: Consola?, tam: Float, fondo: Color, modifier: Modifier = Modifier) {
    val cfg = LocalIconos.current
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    if (c == null) {
        IconoMedio(Medio.TODOS, Uva, fondo, modifier, tam)
        return
    }
    val ctx = LocalContext.current
    val propio = cfg.propios[c.id]?.let { android.net.Uri.parse(it) }
    val paquete = Descargas.PAQUETES.firstOrNull { it.id == cfg.estilo }
    val archivo = remember(cfg.estilo, cfg.version, c.id) {
        paquete?.let { Descargas.archivoIcono(ctx, it.id, c.id).takeIf { f -> f.exists() }?.let { f -> android.net.Uri.fromFile(f) } }
    }
    val uri = propio ?: archivo
    val img = rememberImagen(uri, 192)
    // Si un paquete no trae el ícono de esta consola, se dibuja uno con la misma línea (a color o en contorno).
    val dibujo = if (paquete != null) (if (paquete.blanco) "contorno" else "insignia") else cfg.estilo
    when {
        img != null -> Image(
            img, contentDescription = c.nombre, contentScale = ContentScale.Fit,
            colorFilter = if (propio == null && paquete?.blanco == true) ColorFilter.tint(p.tinta) else null,
            modifier = modifier.size(esc.d(tam)),
        )
        dibujo == "siglas" -> Box(
            modifier.size(esc.d(tam)).clip(RoundedCornerShape(esc.d(tam * 0.28f))).background(c.color),
            contentAlignment = Alignment.Center,
        ) {
            val siglas = SIGLAS[c.id] ?: c.nombre.replace(" ", "").let { if (it.length <= 4) it else it.take(3).uppercase() }
            Texto(siglas, tam * (if (siglas.length > 3) 0.3f else 0.38f), color = if (c.color.luminance() > 0.6f) Tinta else Color.White, lineas = 1)
        }
        // Estilos dibujados por Díptico: todas las consolas con la misma línea.
        dibujo == "insignia" -> Box(
            modifier.size(esc.d(tam)).clip(CircleShape)
                .background(Brush.linearGradient(listOf(androidx.compose.ui.graphics.lerp(c.color, Color.White, 0.18f), androidx.compose.ui.graphics.lerp(c.color, Color.Black, 0.3f)))),
            contentAlignment = Alignment.Center,
        ) { IconoHardware(c, Color.White, c.color, tam = tam * 0.64f) }
        dibujo == "neon" -> Box(
            modifier.size(esc.d(tam)).clip(RoundedCornerShape(esc.d(tam * 0.28f))).background(Color(0xFF0E0C16))
                .border(esc.d(tam * 0.045f), c.color.copy(alpha = 0.9f), RoundedCornerShape(esc.d(tam * 0.28f))),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(esc.d(tam * 0.7f)).background(Brush.radialGradient(listOf(c.color.copy(alpha = 0.35f), Color.Transparent))))
            IconoHardware(c, androidx.compose.ui.graphics.lerp(c.color, Color.White, 0.35f), Color(0xFF0E0C16), tam = tam * 0.6f)
        }
        dibujo == "solido" -> Box(
            modifier.size(esc.d(tam)).clip(RoundedCornerShape(esc.d(tam * 0.28f))).background(c.color),
            contentAlignment = Alignment.Center,
        ) { IconoHardware(c, if (c.color.luminance() > 0.6f) Tinta else Color.White, c.color, tam = tam * 0.62f) }
        dibujo == "contorno" -> Box(
            modifier.size(esc.d(tam)).clip(RoundedCornerShape(esc.d(tam * 0.28f))).background(c.color.copy(alpha = 0.14f))
                .border(esc.d(tam * 0.06f), c.color, RoundedCornerShape(esc.d(tam * 0.28f))),
            contentAlignment = Alignment.Center,
        ) { IconoHardware(c, c.color, fondo, tam = tam * 0.62f) }
        dibujo == "pixel" -> ConsolaPixel(c, c.color, fondo, modifier, tam)
        else -> IconoHardware(c, c.color, fondo, modifier, tam)
    }
}

/** El formato de la consola dibujado en píxeles grandes (estilo "Píxeles"). */
@Composable
fun MedioPixel(medio: Medio, color: Color, fondo: Color, modifier: Modifier = Modifier, tam: Float = 44f) {
    val esc = LocalEscala.current
    val img = remember(medio, color, fondo) {
        val n = 16
        val bmp = androidx.compose.ui.graphics.ImageBitmap(n, n)
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
            androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr,
            androidx.compose.ui.graphics.Canvas(bmp), androidx.compose.ui.geometry.Size(n.toFloat(), n.toFloat()),
        ) { dibujarMedio(medio, color, fondo) }
        bmp
    }
    Image(img, null, modifier.size(esc.d(tam)), contentScale = ContentScale.Fit, filterQuality = androidx.compose.ui.graphics.FilterQuality.None)
}

/** El formato de la consola dibujado en píxeles grandes (estilo "Píxeles"). */
@Composable
private fun ConsolaPixel(consola: Consola, color: Color, fondo: Color, modifier: Modifier = Modifier, tam: Float = 44f) {
    val esc = LocalEscala.current
    val img = remember(consola.id, consola.medio, color, fondo) {
        val n = 16
        val bmp = androidx.compose.ui.graphics.ImageBitmap(n, n)
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
            androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr,
            androidx.compose.ui.graphics.Canvas(bmp), androidx.compose.ui.geometry.Size(n.toFloat(), n.toFloat()),
        ) { dibujarHardware(consola, color, fondo) }
        bmp
    }
    Image(img, null, modifier.size(esc.d(tam)), contentScale = ContentScale.Fit, filterQuality = androidx.compose.ui.graphics.FilterQuality.None)
}

/**
 * Desplaza la lista solo cuando el elegido no se ve entero: si ya se ve, no se mueve nada (las opciones quedan quietas).
 * Se cuenta en elementos, no en píxeles, para que nunca se quede pegada aunque mantengas la cruceta:
 * bajando, el elegido queda como penúltimo a la vista; subiendo, como segundo.
 */
suspend fun androidx.compose.foundation.lazy.LazyListState.mostrar(i: Int) {
    if (i < 0) return
    val info = layoutInfo
    val vis = info.visibleItemsInfo
    if (vis.isEmpty()) { if (i > 1) scrollToItem(i - 1); return }
    val fin = info.viewportEndOffset - info.afterContentPadding
    val completos = vis.filter { it.offset >= 0 && it.offset + it.size <= fin }
    if (completos.any { it.index == i }) return
    val primero = completos.firstOrNull()?.index ?: vis.first().index
    val n = completos.size.coerceAtLeast(2)
    if (i <= primero) animateScrollToItem((i - 1).coerceAtLeast(0))
    else animateScrollToItem((i - n + 2).coerceIn(0, i))
}

/** Lo mismo para grillas (por filas). */
suspend fun androidx.compose.foundation.lazy.grid.LazyGridState.mostrar(i: Int) {
    if (i < 0) return
    val info = layoutInfo
    val vis = info.visibleItemsInfo
    if (vis.isEmpty()) { if (i > 0) scrollToItem(i); return }
    val fin = info.viewportEndOffset - info.afterContentPadding
    val cols = vis.count { it.row == vis.first().row }.coerceAtLeast(1)
    val completos = vis.filter { it.offset.y >= 0 && it.offset.y + it.size.height <= fin }
    if (completos.any { it.index == i }) return
    val filaI = i / cols
    val filas = completos.map { it.row }.distinct()
    val primera = filas.minOrNull() ?: vis.first().row
    val n = filas.size.coerceAtLeast(2)
    val destino = if (filaI <= primera) (filaI - 1).coerceAtLeast(0) else (filaI - n + 2).coerceIn(0, filaI)
    animateScrollToItem(destino * cols)
}

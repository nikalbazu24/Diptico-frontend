package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Perfil
import cl.nikalbazu.diptico.estado.ResumenPerfil
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val TextoClaro = Color(0xFFF4F5FB)
private val TextoSuave = Color(0xFFA5A8BD)

/**
 * Tu foto en una esquina: al tocarla se abre tu perfil, y la tarjeta crece desde ahí mismo.
 * Guarda dónde está (como fracción de la pantalla) para que la animación salga de este punto.
 */
@Composable
fun BotonPerfil(tam: Float = 26f) {
    val vista = LocalView.current
    val pf by remember { Diptico.estado.map { it.extra.perfil }.distinctUntilChanged() }.collectAsState(Diptico.estado.value.extra.perfil)
    // Centro de la foto en la ventana (fracción del ancho y del alto); no se lee al dibujar, así que no recompone nada.
    val centro = remember { floatArrayOf(0.92f, 0.07f) }
    Box(
        Modifier
            .onGloballyPositioned { c ->
                val p = c.positionInWindow()
                val w = vista.rootView.width.coerceAtLeast(1)
                val h = vista.rootView.height.coerceAtLeast(1)
                centro[0] = (p.x + c.size.width / 2f) / w
                centro[1] = (p.y + c.size.height / 2f) / h
            }
            .clip(CircleShape)
            .clickable { Diptico.abrirPerfil(vista.display?.displayId ?: 0, centro[0], centro[1]) }
            .testTag("boton-perfil"),
    ) { FotoPerfil(pf, tam) }
}

/**
 * La tarjeta de tu perfil. Crece desde tu foto con un resorte suave, sobre el fondo difuminado (lo desenfoca [Raiz]),
 * y se cierra encogiéndose de vuelta hacia ella. B o tocar afuera la cierra; A abre los ajustes del perfil.
 */
@Composable
fun PanelPerfil(e: Estado, miPantalla: Int) {
    val abierto = e.extra.perfilEn == miPantalla
    val animar = LocalMovimiento.current
    val t by animateFloatAsState(
        if (abierto) 1f else 0f,
        if (!animar) snap() else if (abierto) spring(dampingRatio = 0.8f, stiffness = 300f) else tween(210, easing = FastOutLinearInEasing),
        label = "perfil",
    )
    if (!abierto && t < 0.01f) return
    val esc = LocalEscala.current
    val ox = e.extra.perfilOx
    val oy = e.extra.perfilOy
    val color = Color(e.extra.perfil.color)
    Box(Modifier.fillMaxSize()) {
        // Oscurece un poco más lo de atrás (el desenfoque lo pone Raiz).
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = t.coerceIn(0f, 1f) }.background(Color(0x66080912))
                .clickable(remember { MutableInteractionSource() }, indication = null) { Diptico.cerrarPerfil() },
        )
        // Todo el lienzo se escala desde el punto de la foto: así la tarjeta sale exactamente de ahí.
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    val s = 0.04f + 0.96f * t
                    scaleX = s; scaleY = s
                    alpha = (t * 2.2f).coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(ox, oy)
                }
                .padding(horizontal = esc.d(if (esc.angosta) 12 else 40), vertical = esc.d(if (esc.angosta) 12 else 26)),
        ) {
            val forma = RoundedCornerShape(esc.d(28))
            Box(
                Modifier.fillMaxSize().clip(forma)
                    .background(Brush.linearGradient(listOf(Color(0xF0171A32), Color(0xF00C0D1A))))
                    .background(Brush.radialGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent), center = Offset(0f, 0f), radius = 1100f))
                    .border(esc.d(1.5f), color.copy(alpha = 0.55f), forma)
                    .clickable(remember { MutableInteractionSource() }, indication = null) {}
                    .testTag("panel-perfil"),
            ) { ContenidoPerfil(e) }
        }
    }
}

@Composable
private fun ContenidoPerfil(e: Estado) {
    val esc = LocalEscala.current
    val pf = e.extra.perfil
    val color = Color(pf.color)
    val r = remember(e.datos, e.juegos, pf, e.consolasConJuegos) { Diptico.resumenPerfil(e) }
    val nombre = e.nombre.ifBlank { pf.apodo.ifBlank { "Jugador" } }
    val angosta = esc.angosta
    Column(Modifier.fillMaxSize().padding(esc.d(if (angosta) 16 else 22))) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (angosta) {
                // Pantalla casi cuadrada: arriba tu foto y tu nombre, y abajo las pastillas y los números (sin favoritos, para que quepa todo).
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                    FotoPerfil(pf, 78f)
                    Column(Modifier.weight(1f)) { Encabezado(e, nombre, 26f) }
                }
                Spacer(Modifier.height(esc.d(8)))
                Fichas(e, r, color)
                Spacer(Modifier.height(esc.d(10)))
                Numeros(e, r, color, conFavoritos = false)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(26))) {
                    Column(Modifier.width(esc.d(196)), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        FotoPerfil(pf, 104f)
                        Encabezado(e, nombre, 30f)
                        Fichas(e, r, color)
                    }
                    Column(Modifier.weight(1f)) { Numeros(e, r, color, conFavoritos = true) }
                }
            }
        }
        Spacer(Modifier.height(esc.d(10)))
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(color).clickable { Diptico.editarPerfil() }
                    .padding(horizontal = esc.d(16), vertical = esc.d(7)),
            ) { Texto("A · Editar perfil", 13.5f, color = if (color.luminance2() > 0.55f) Color(0xFF14101A) else Color.White, lineas = 1) }
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.1f)).clickable { Diptico.cerrarPerfil() }
                    .padding(horizontal = esc.d(16), vertical = esc.d(7)),
            ) { Texto("B · Cerrar", 13.5f, color = TextoClaro, lineas = 1) }
        }
    }
}

private fun Color.luminance2(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

@Composable
private fun Encabezado(e: Estado, nombre: String, tam: Float) {
    val pf = e.extra.perfil
    val desde = remember(pf.desde) { java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(pf.desde)) }
    Column(verticalArrangement = Arrangement.spacedBy(LocalEscala.current.d(2))) {
        Texto(nombre, tam, color = TextoClaro, peso = FontWeight.Black, lineas = 1, traducir = false)
        Texto(
            (if (pf.apodo.isNotBlank()) "@${pf.apodo} · " else "") + "desde $desde",
            14f, color = TextoSuave, lineas = 1, traducir = false,
        )
    }
}

/** Las pastillas bajo tu nombre: Discord, tu consola favorita y tu edad o cumpleaños. */
@Composable
private fun Fichas(e: Estado, r: ResumenPerfil, color: Color) {
    val esc = LocalEscala.current
    val pf = e.extra.perfil
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
        if (pf.cumpleHoy()) Pastilla(color.copy(alpha = 0.3f), color) { Texto("🎂 ¡Hoy es tu cumpleaños${pf.edad()?.let { " · $it" } ?: ""}!", 12.5f, color = TextoClaro, lineas = 2, traducir = false) }
        if (e.extra.discordUsuario.isNotBlank()) Pastilla(Color(0x385865F2), Color(0xFF23A55A)) { Texto("Discord: ${e.extra.discordUsuario}", 12.5f, color = TextoClaro, lineas = 1, traducir = false) }
        if (r.consola.isNotBlank()) Pastilla(Color.White.copy(alpha = 0.08f), color) { Texto("Consola favorita: ${r.consola}", 12.5f, color = TextoClaro, lineas = 1, traducir = false) }
        pf.edad()?.takeIf { !pf.cumpleHoy() }?.let { Pastilla(Color.White.copy(alpha = 0.08f), color) { Texto("$it años", 12.5f, color = TextoClaro, lineas = 1, traducir = false) } }
    }
}

@Composable
private fun Pastilla(fondo: Color, punto: Color, contenido: @Composable () -> Unit) {
    val esc = LocalEscala.current
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(fondo).padding(horizontal = esc.d(11), vertical = esc.d(4)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(7)),
    ) {
        Box(Modifier.size(esc.d(8)).clip(CircleShape).background(punto))
        contenido()
    }
}

/** Tus números (horas, juego más jugado, racha, logros) y tus juegos favoritos. */
@Composable
private fun Numeros(e: Estado, r: ResumenPerfil, color: Color, conFavoritos: Boolean) {
    val esc = LocalEscala.current
    val pf = e.extra.perfil
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        Texto("TU ACTIVIDAD", 12f, color = TextoSuave, espaciado = 1.6f, lineas = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
            Dato("Horas jugadas", if (pf.mostrarHoras) horas(r.segundos) else "Oculto", if (pf.mostrarHoras) "${r.juegosJugados} juegos distintos" else "Se cambia en tu perfil", color, Modifier.weight(1f))
            Dato("Más jugado", r.masJugado?.titulo ?: "Aún nada", r.masJugado?.let { if (pf.mostrarHoras) horas(r.masJugadoSeg) else "" }.orEmpty(), color, Modifier.weight(1f), traducirValor = r.masJugado == null)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
            Dato("Racha", if (r.racha == 1) "1 día" else "${r.racha} días", if (r.racha > 0) "jugando seguido" else "juega hoy para empezar", color, Modifier.weight(1f))
            Dato("Logros de DIPPO", "${r.logros} de ${r.logrosTotal}", "", color, Modifier.weight(1f))
        }
        if (conFavoritos) {
            Texto("MIS FAVORITOS", 12f, color = TextoSuave, espaciado = 1.6f, lineas = 1)
            if (r.favoritos.isEmpty()) Detalle("Marca juegos como favoritos y aparecen aquí.", 12.5f, color = TextoSuave, lineas = 2)
            else Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                for (j in r.favoritos) Box(
                    Modifier.height(esc.d(62)).aspectRatio(0.78f).clip(RoundedCornerShape(esc.d(10))).background(Color.White.copy(alpha = 0.06f)),
                ) { ArteJuego(j, preferirArte = false, maxLado = 200, modifier = Modifier.fillMaxSize()) }
            }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, sub: String, color: Color, modifier: Modifier, traducirValor: Boolean = false) {
    val esc = LocalEscala.current
    Column(
        modifier.clip(RoundedCornerShape(esc.d(16))).background(Color.White.copy(alpha = 0.07f)).padding(horizontal = esc.d(12), vertical = esc.d(7)),
        verticalArrangement = Arrangement.spacedBy(esc.d(0)),
    ) {
        Texto(etiqueta, 11.5f, color = TextoSuave, lineas = 1)
        Texto(valor, 19f, color = TextoClaro, peso = FontWeight.Black, lineas = 1, traducir = traducirValor || valor == "Oculto")
        if (sub.isNotBlank()) Texto(sub, 11.5f, color = color, lineas = 1, traducir = sub == "jugando seguido" || sub == "juega hoy para empezar" || sub == "Se cambia en tu perfil")
    }
}

private fun horas(seg: Long): String = when {
    seg >= 36_000 -> "${seg / 3600} h"
    seg >= 3600 -> "%.1f h".format(java.util.Locale.ROOT, seg / 3600.0)
    else -> "${seg / 60} min"
}

/** Una vista chica de tu perfil, para el panel de la derecha de los ajustes. */
@Composable
internal fun PreviaPerfil(e: Estado) {
    val esc = LocalEscala.current
    val pf: Perfil = e.extra.perfil
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(pf.color).copy(alpha = 0.3f), Color.Transparent), radius = 700f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
            FotoPerfil(pf, 110f)
            Texto(e.nombre.ifBlank { "Tu nombre" }, 24f, lineas = 1, traducir = e.nombre.isBlank())
            if (pf.apodo.isNotBlank()) Detalle("@${pf.apodo}", 14f, lineas = 1)
            if (pf.nacimientoBonito().isNotBlank()) Detalle(pf.nacimientoBonito(), 13f, lineas = 1)
        }
    }
}

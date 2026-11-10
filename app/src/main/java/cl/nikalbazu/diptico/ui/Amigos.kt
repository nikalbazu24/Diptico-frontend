package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.estado.AmigoDiscord
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.SocialDiscord

internal val ColorDiscord = Color(0xFF5865F2)
private val VerdeConectado = Color(0xFF23A55A)
private val AmarilloAusente = Color(0xFFF0B232)
private val RojoOcupado = Color(0xFFF23F43)

/** El color del puntito de estado de un amigo. */
private fun colorEstado(estado: String) = when (estado) {
    "jugando", "conectado" -> VerdeConectado
    "ausente" -> AmarilloAusente
    "ocupado" -> RojoOcupado
    else -> Color(0xFF80848E)
}

/**
 * El dibujo de Díptico para Discord: una burbuja de chat con carita de mando (no es el logo de Discord), en su azul.
 * Con [conectado] lleva el puntito verde; sin conexión queda gris.
 */
@Composable
fun MarcaDiscord(tam: Float, conectado: Boolean, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    Canvas(modifier.size(esc.d(tam))) {
        val w = size.width
        val fondo = if (conectado) ColorDiscord else Color(0xFF4E5058)
        drawCircle(fondo, w / 2f)
        // La burbuja: un mando redondeado con dos ojitos.
        val bw = w * 0.58f; val bh = w * 0.38f
        val x = (w - bw) / 2f; val y = w * 0.33f
        drawRoundRect(Color.White, Offset(x, y), Size(bw, bh), CornerRadius(bh * 0.5f))
        drawCircle(fondo, w * 0.055f, Offset(w * 0.4f, y + bh * 0.5f))
        drawCircle(fondo, w * 0.055f, Offset(w * 0.6f, y + bh * 0.5f))
        if (conectado) {
            drawCircle(Color(0xFF111214), w * 0.17f, Offset(w * 0.82f, w * 0.82f))
            drawCircle(VerdeConectado, w * 0.12f, Offset(w * 0.82f, w * 0.82f))
        }
    }
}

/**
 * El botón de Discord para las barras (arriba junto a la hora, abajo en la franja). Muestra si estás conectado y
 * cuántos amigos juegan; al tocarlo se abre la ventana de amigos desde el costado de esa misma pantalla.
 */
@Composable
fun BotonDiscord(tam: Float = 22f) {
    val esc = LocalEscala.current
    val vista = LocalView.current
    val e by Diptico.estado.collectAsState()
    val amigos by SocialDiscord.amigos.collectAsState()
    val conexion by SocialDiscord.conexion.collectAsState()
    // Con el kit, el puntito verde es la conexión real; sin él, basta con la cuenta guardada.
    val conectado = e.extra.discordUsuario.isNotBlank() && (!SocialDiscord.disponible || conexion == "listo")
    val jugando = amigos.count { it.estado == "jugando" }
    Row(
        Modifier.clip(RoundedCornerShape(50)).clickable { Diptico.abrirAmigos(vista.display?.displayId ?: 0) }
            .padding(horizontal = esc.d(4), vertical = esc.d(2)).testTag("boton-discord"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(esc.d(4)),
    ) {
        MarcaDiscord(tam, conectado)
        if (jugando > 0) Texto("$jugando", 11f, color = LocalPaleta.current.tinta, traducir = false, lineas = 1)
    }
}

/**
 * La ventana de amigos: se desliza desde el costado derecho de la pantalla donde la abriste, sin tapar todo.
 * Arriba tu cuenta; luego los amigos que juegan (y a qué), los conectados y los desconectados. B o tocar afuera la cierra.
 */
@Composable
fun PanelAmigos(e: Estado, miPantalla: Int) {
    val abierto = e.extra.amigosEn == miPantalla
    AnimatedVisibility(abierto, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
            .clickable(remember { MutableInteractionSource() }, indication = null) { Diptico.cerrarAmigos() })
    }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        val ancho = (maxWidth * 0.46f).coerceIn(LocalEscala.current.d(260), LocalEscala.current.d(380))
        AnimatedVisibility(
            abierto,
            enter = slideInHorizontally(tween(260, easing = CurvaPosarse)) { it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(200)) { it } + fadeOut(tween(160)),
        ) {
            ContenidoAmigos(e, Modifier.width(ancho).fillMaxHeight())
        }
    }
}

@Composable
private fun ContenidoAmigos(e: Estado, modifier: Modifier) {
    val esc = LocalEscala.current
    val amigos by SocialDiscord.amigos.collectAsState()
    val conexion by SocialDiscord.conexion.collectAsState()
    val x = e.extra
    val conectado = x.discordUsuario.isNotBlank()
    val enLineaYo = !SocialDiscord.disponible || conexion == "listo"
    Column(
        modifier.padding(vertical = esc.d(10)).padding(end = esc.d(10))
            .clip(RoundedCornerShape(esc.d(22))).background(Color(0xF21E1F24))
            .border(esc.d(1), ColorDiscord.copy(alpha = 0.45f), RoundedCornerShape(esc.d(22)))
            .clickable(remember { MutableInteractionSource() }, indication = null) {}
            .testTag("panel-amigos"),
    ) {
        // Cabecera: tu cuenta.
        Row(
            Modifier.fillMaxWidth().background(ColorDiscord.copy(alpha = 0.22f)).padding(horizontal = esc.d(14), vertical = esc.d(12)),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10)),
        ) {
            Avatar(x.discordAvatar.takeIf { it.isNotBlank() }, x.discordUsuario.ifBlank { "?" }, 38f,
                if (conectado && enLineaYo) "conectado" else if (conectado && conexion == "conectando") "ausente" else "desconectado")
            Column(Modifier.weight(1f)) {
                Texto(if (conectado) x.discordUsuario else "Discord", 15f, color = Color.White, peso = FontWeight.Bold, lineas = 1, traducir = !conectado)
                Texto(when {
                    !conectado -> "Sin conectar"
                    enLineaYo -> "Conectado"
                    conexion == "conectando" -> "Conectando…"
                    else -> "Sin conexión con Discord"
                }, 11.5f, color = Color.White.copy(alpha = 0.65f), lineas = 1)
            }
            Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.12f)).clickable { Diptico.cerrarAmigos() }
                .padding(horizontal = esc.d(10), vertical = esc.d(5))) { Texto("B", 11f, color = Color.White, traducir = false, lineas = 1) }
        }
        when {
            !conectado -> Column(Modifier.fillMaxWidth().padding(esc.d(16)), verticalArrangement = Arrangement.spacedBy(esc.d(12)),
                horizontalAlignment = Alignment.CenterHorizontally) {
                MarcaDiscord(64f, false)
                Texto("Conecta tu Discord para ver a tus amigos, quién está conectado y a qué juega.", 13.5f,
                    color = Color.White, alinear = androidx.compose.ui.text.style.TextAlign.Center)
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(ColorDiscord).clickable { Diptico.iniciarDiscord() }
                    .padding(vertical = esc.d(11)), contentAlignment = Alignment.Center) {
                    Texto("A  Conectar con Discord", 14f, color = Color.White, peso = FontWeight.Bold, lineas = 1)
                }
                Texto("Aparece un código: lo escaneas con tu teléfono y listo.", 11.5f, color = Color.White.copy(alpha = 0.6f),
                    alinear = androidx.compose.ui.text.style.TextAlign.Center)
                if (SocialDiscord.disponible) Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.1f))
                    .clickable { Diptico.conectarDiscordAqui() }.padding(vertical = esc.d(9)), contentAlignment = Alignment.Center) {
                    Texto("Y  Abrir Discord en esta consola", 12.5f, color = Color.White, lineas = 1)
                }
            }
            !SocialDiscord.disponible -> Column(Modifier.fillMaxWidth().padding(esc.d(16)), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
                Texto("Tus amigos", 13f, color = Color.White.copy(alpha = 0.7f), peso = FontWeight.Bold, lineas = 1)
                Texto("La lista de amigos llega con el kit oficial de Discord, que estamos sumando a Díptico.", 12.5f, color = Color.White.copy(alpha = 0.75f))
            }
            amigos.isEmpty() -> Column(Modifier.fillMaxWidth().padding(esc.d(16)), verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
                Texto(when (conexion) {
                    "conectando" -> "Conectando con Discord…"
                    "listo" -> "Todavía no tienes amigos en Discord."
                    else -> "No pude conectar con Discord."
                }, 13f, color = Color.White.copy(alpha = 0.75f))
                if (conexion != "conectando" && conexion != "listo") Box(Modifier.clip(RoundedCornerShape(50)).background(ColorDiscord)
                    .clickable { Diptico.reconectarDiscord() }.padding(horizontal = esc.d(16), vertical = esc.d(8))) {
                    Texto("Reintentar", 13f, color = Color.White, peso = FontWeight.Bold, lineas = 1)
                }
            }
            else -> {
                val lista = remember(amigos) { SocialDiscord.ordenar(amigos) }
                val jugando = lista.filter { it.estado == "jugando" }
                val enLinea = lista.filter { it.estado in setOf("conectado", "ausente", "ocupado") }
                val fuera = lista.filter { it.estado == "desconectado" }
                LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(horizontal = esc.d(10)), verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
                    if (jugando.isNotEmpty()) { item { Seccion("Jugando ahora · ${jugando.size}") }; items(jugando, key = { it.id }) { FilaAmigo(it) } }
                    if (enLinea.isNotEmpty()) { item { Seccion("Conectados · ${enLinea.size}") }; items(enLinea, key = { it.id }) { FilaAmigo(it) } }
                    if (fuera.isNotEmpty()) { item { Seccion("Desconectados · ${fuera.size}") }; items(fuera, key = { it.id }) { FilaAmigo(it) } }
                    item { Spacer(Modifier.height(esc.d(10))) }
                }
            }
        }
    }
}

@Composable
private fun Seccion(t: String) {
    val esc = LocalEscala.current
    Texto(t.uppercase(), 10.5f, color = Color.White.copy(alpha = 0.55f), peso = FontWeight.Bold, espaciado = 1f, lineas = 1, traducir = false,
        modifier = Modifier.padding(top = esc.d(10), bottom = esc.d(2), start = esc.d(4)))
}

@Composable
private fun FilaAmigo(a: AmigoDiscord) {
    val esc = LocalEscala.current
    val jugando = a.estado == "jugando"
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(12)))
            .background(if (jugando) ColorDiscord.copy(alpha = 0.14f) else Color.Transparent)
            .padding(horizontal = esc.d(8), vertical = esc.d(6)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10)),
    ) {
        Avatar(a.avatar, a.nombre, 32f, a.estado)
        Column(Modifier.weight(1f)) {
            Texto(a.nombre, 13.5f, color = if (a.estado == "desconectado") Color.White.copy(alpha = 0.5f) else Color.White, lineas = 1, traducir = false)
            if (a.actividad != null) Texto(
                (if (a.enDiptico) "🎮 " else "") + a.actividad + (a.detalle?.let { " · $it" } ?: ""), 11.5f,
                color = if (jugando) Color(0xFFB5BAFF) else Color.White.copy(alpha = 0.55f), lineas = 1, traducir = false,
            )
        }
    }
}

/** La foto del amigo (o su inicial) con el puntito de su estado. */
@Composable
private fun Avatar(url: String?, nombre: String, tam: Float, estado: String) {
    val esc = LocalEscala.current
    val img = rememberImagen(url?.let { android.net.Uri.parse(it) }, 128)
    Box(Modifier.size(esc.d(tam))) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(ColorDiscord.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
            if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Texto(nombre.take(1).uppercase(), tam * 0.42f, color = Color.White, peso = FontWeight.Bold, traducir = false, lineas = 1)
        }
        Box(Modifier.align(Alignment.BottomEnd).size(esc.d(tam * 0.34f)).clip(CircleShape).background(Color(0xFF1E1F24)).padding(esc.d(tam * 0.06f))) {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(colorEstado(estado)))
        }
    }
}

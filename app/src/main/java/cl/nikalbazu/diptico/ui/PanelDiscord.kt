package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import kotlinx.coroutines.delay

private val Blurple = Color(0xFF5865F2)

/**
 * Iniciar sesión con Discord, como en las consolas: un QR para escanear con el teléfono y, por si acaso, el código
 * para escribir en discord.com/activate. Díptico se entera solo cuando apruebas. B cierra, A pide otro código si falló.
 */
@Composable
fun PanelDiscord(e: Estado) {
    val l = e.extra.discordLogin ?: return
    val esc = LocalEscala.current
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(l.expira) { while (true) { ahora = System.currentTimeMillis(); delay(1000) } }
    Box(
        Modifier.fillMaxSize().background(Color(0xE60A0A12)).testTag("panel-discord")
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.padding(esc.d(18))) {
            val ancho = maxWidth.value > 560f
            Column(
                Modifier.widthIn(max = esc.d(720)).clip(RoundedCornerShape(esc.d(22))).background(Color(0xFF1E1F29)),
            ) {
                // Cabecera con el color de Discord.
                Row(
                    Modifier.background(Blurple).padding(horizontal = esc.d(20), vertical = esc.d(14)),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10)),
                ) {
                    IconoDiptico(cl.nikalbazu.diptico.estado.Icono.MUNDO, Color.White, 18f)
                    Texto("Iniciar sesión con Discord", 17f, color = Color.White, peso = FontWeight.ExtraBold, lineas = 1, modifier = Modifier.weight(1f))
                    Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f)).clickable { Diptico.cancelarDiscord() }
                        .padding(horizontal = esc.d(12), vertical = esc.d(5))) { Texto("B Cerrar", 12f, color = Color.White, lineas = 1) }
                }
                Box(Modifier.padding(esc.d(20))) {
                    when {
                        l.error != null -> Column(verticalArrangement = Arrangement.spacedBy(esc.d(12)), horizontalAlignment = Alignment.CenterHorizontally) {
                            Texto(l.error, 15f, color = Color.White, alinear = TextAlign.Center, traducir = false)
                            Box(Modifier.clip(RoundedCornerShape(50)).background(Blurple).clickable { Diptico.iniciarDiscord() }
                                .padding(horizontal = esc.d(18), vertical = esc.d(9))) { Texto("A Pedir otro código", 14f, color = Color.White, peso = FontWeight.Bold, lineas = 1) }
                        }
                        l.cargando -> Texto("Pidiendo un código a Discord…", 15f, color = Color.White.copy(alpha = 0.8f))
                        else -> {
                            val qr = @Composable { CodigoQR(l.urlCompleta, if (ancho) 170f else 140f) }
                            val pasos = @Composable {
                                Column(verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
                                    Texto("1. Escanea el código con la cámara de tu teléfono.", 14f, color = Color.White)
                                    Texto("2. Inicia sesión en Discord si te lo pide y toca «Autorizar».", 14f, color = Color.White)
                                    Texto("3. Listo: Díptico se conecta solo.", 14f, color = Color.White)
                                    Texto("¿Sin cámara? Entra a discord.com/activate y escribe:", 12.5f, color = Color.White.copy(alpha = 0.7f))
                                    Box(Modifier.clip(RoundedCornerShape(esc.d(10))).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = esc.d(14), vertical = esc.d(8))) {
                                        Texto(l.codigo, 26f, color = Color.White, peso = FontWeight.ExtraBold, espaciado = 3f, lineas = 1, traducir = false)
                                    }
                                    val quedan = ((l.expira - ahora) / 1000).coerceAtLeast(0)
                                    Texto("Esperando que apruebes… (el código dura ${quedan / 60}:${"%02d".format(quedan % 60)})", 11.5f,
                                        color = Color.White.copy(alpha = 0.6f), traducir = false)
                                    if (cl.nikalbazu.diptico.estado.SocialDiscord.disponible) Box(Modifier.clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = 0.1f)).clickable { Diptico.conectarDiscordAqui() }
                                        .padding(horizontal = esc.d(14), vertical = esc.d(7))) {
                                        Texto("Y  O ábrelo en esta consola", 12.5f, color = Color.White, lineas = 1)
                                    }
                                }
                            }
                            if (ancho) Row(horizontalArrangement = Arrangement.spacedBy(esc.d(22)), verticalAlignment = Alignment.CenterVertically) { qr(); pasos() }
                            else Column(verticalArrangement = Arrangement.spacedBy(esc.d(14)), horizontalAlignment = Alignment.CenterHorizontally) { qr(); pasos() }
                        }
                    }
                }
            }
        }
    }
}

package cl.nikalbazu.diptico.ui

import android.content.Context
import android.os.BatteryManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Estado
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

/** Pantalla de arriba: el juego elegido ocupa todo, con la información encima. */
@Composable
fun PantallaSuperior(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(Modifier.fillMaxSize().background(p.fondo)) {
        val juego = e.seleccionado
        when {
            !e.configurado -> Bienvenida(e)
            juego == null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Logo(30f)
                Spacer(Modifier.height(esc.d(14)))
                Texto(if (e.escaneando) "Buscando tus juegos…" else "No encontré juegos en tu carpeta", 20f, color = p.tenue)
            }
            else -> {
                Crossfade(targetState = juego, animationSpec = tween(700), label = "arte", modifier = Modifier.fillMaxSize()) { j ->
                    Box(Modifier.fillMaxSize()) {
                        ArteJuego(
                            j, preferirArte = true, maxLado = 1600,
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.8f),
                        )
                    }
                }
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to p.fondo, 0.24f to p.fondo, 0.72f to Color.Transparent)))
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(esc.d(260))
                        .background(Brush.verticalGradient(listOf(Color.Transparent, p.fondo))),
                )

                BarraSuperior(Modifier.align(Alignment.TopStart))

                AnimatedContent(
                    targetState = juego,
                    transitionSpec = { (fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 10 }) togetherWith fadeOut(tween(200)) },
                    label = "info",
                    modifier = Modifier.padding(start = esc.d(44), top = esc.d(108)).width(esc.d(500)),
                ) { j ->
                    val c = Consolas.porId(j.consolaId)
                    val datos = e.datos[j.clave]
                    Column(verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
                        if (c != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconoMedio(c.medio, c.color, p.fondo, tam = 24f)
                                Spacer(Modifier.width(esc.d(10)))
                                Texto(c.nombreLargo, 12f, color = p.tenue, familia = Nunito, peso = FontWeight.ExtraBold, espaciado = 1.6f)
                            }
                        }
                        Texto(j.titulo, 54f, lineas = 3, interlineado = 56f)
                        Texto(
                            if ((datos?.tiempo ?: 0L) > 0) "${formatoTiempo(datos!!.tiempo)} jugadas" else "Aún sin jugar con Díptico",
                            14f, color = p.tenue, familia = Nunito, peso = FontWeight.ExtraBold,
                        )
                        Spacer(Modifier.height(esc.d(6)))
                        Row {
                            Box(Modifier.padding(top = esc.d(6)).size(esc.d(10)).clip(CircleShape).background(Coral))
                            Spacer(Modifier.width(esc.d(10)))
                            val paso = datos?.paso.orEmpty()
                            Column {
                                Texto("Tu próximo paso", 14f, color = p.acentoTexto)
                                Texto(
                                    paso.ifEmpty { "Aún no anotas dónde quedaste. Aprieta Y para hacerlo." },
                                    20f, peso = FontWeight.SemiBold, lineas = 3, interlineado = 26f,
                                    color = if (paso.isEmpty()) p.tenue else p.tinta,
                                )
                            }
                        }
                    }
                }

                Row(
                    Modifier.align(Alignment.BottomStart).padding(start = esc.d(44), bottom = esc.d(32)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(esc.d(22)),
                ) {
                    Row(
                        Modifier
                            .shadow(esc.d(6), RoundedCornerShape(50), spotColor = Uva)
                            .clip(RoundedCornerShape(50))
                            .background(Uva)
                            .padding(start = esc.d(7), end = esc.d(24), top = esc.d(7), bottom = esc.d(7)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(esc.d(40)).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                            Texto("A", 18f, color = Uva)
                        }
                        Spacer(Modifier.width(esc.d(12)))
                        Texto("¡Jugar!", 20f, color = Color.White)
                    }
                    PistaBoton("Y", "Próximo paso")
                    PistaBoton("Start", "Ajustes")
                }
                if (e.sesion != null) {
                    Texto(
                        "Jugando ahora", 13f, color = Coral, familia = Nunito, peso = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = esc.d(40), bottom = esc.d(44)),
                    )
                }
            }
        }
    }
}

@Composable
private fun PistaBoton(tecla: String, texto: String) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.height(esc.d(26)).border(esc.d(2), p.tenue, RoundedCornerShape(50)).padding(horizontal = esc.d(8)),
            contentAlignment = Alignment.Center,
        ) { Texto(tecla, 12f) }
        Spacer(Modifier.width(esc.d(8)))
        Texto(texto, 16f)
    }
}

@Composable
internal fun BarraSuperior(modifier: Modifier, extra: @Composable () -> Unit = {}) {
    val esc = LocalEscala.current
    val hora = rememberHora()
    val bat = rememberBateria()
    Row(
        modifier.fillMaxWidth().padding(start = esc.d(44), end = esc.d(40), top = esc.d(24)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Logo(20f)
        Spacer(Modifier.weight(1f))
        Texto(hora, 16f, sombra = true)
        if (bat >= 0) {
            Spacer(Modifier.width(esc.d(18)))
            Texto("$bat%", 16f, sombra = true)
        }
        extra()
    }
}

@Composable
private fun Bienvenida(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.align(Alignment.TopEnd).size(esc.d(520)).background(
                Brush.radialGradient(listOf(Uva.copy(alpha = 0.45f), Color.Transparent)),
            ),
        )
        Box(
            Modifier.align(Alignment.BottomCenter).size(esc.d(420)).background(
                Brush.radialGradient(listOf(Coral.copy(alpha = 0.3f), Color.Transparent)),
            ),
        )
        Column(Modifier.padding(start = esc.d(56), top = esc.d(48), end = esc.d(56)).fillMaxSize()) {
            Logo(48f)
            Spacer(Modifier.height(esc.d(20)))
            Texto("Tu colección de juegos, en dos pantallas.", 28f, peso = FontWeight.SemiBold, color = p.tenue, modifier = Modifier.width(esc.d(560)))
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                val paso = when {
                    !e.tieneCarpeta -> 0
                    e.escaneando -> 1
                    else -> 2
                }
                Paso("1", "Tu carpeta de juegos", "Una carpeta por consola: snes, psx, nds…", paso > 0, paso == 0)
                Paso("2", "Buscar juegos", if (e.escaneando) "Buscando…" else "${e.juegos.size} encontrados", paso > 1, paso == 1)
                Paso("3", "Emuladores", "Reviso cuáles tienes instalados", false, paso == 2)
            }
            Spacer(Modifier.height(esc.d(40)))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Paso(n: String, titulo: String, detalle: String, listo: Boolean, actual: Boolean) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column(
        Modifier.weight(1f)
            .clip(RoundedCornerShape(esc.d(22)))
            .background(if (actual) Uva.copy(alpha = 0.22f) else p.tinta.copy(alpha = 0.05f))
            .border(esc.d(2), if (actual) Uva else p.tinta.copy(alpha = 0.08f), RoundedCornerShape(esc.d(22)))
            .padding(esc.d(16)),
        verticalArrangement = Arrangement.spacedBy(esc.d(8)),
    ) {
        Box(
            Modifier.size(esc.d(32)).clip(CircleShape).background(if (listo) Sol else if (actual) Uva else p.tinta.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) { Texto(if (listo) "✓" else n, 15f, color = if (listo) Tinta else Color.White) }
        Texto(titulo, 17f)
        Texto(detalle, 12f, color = p.tenue, familia = Nunito)
    }
}

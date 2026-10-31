package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Resumen de tu tiempo de juego: totales, por consola, tus juegos más jugados y los últimos. */
@Composable
fun PanelEstadisticas(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val jugados = e.datos.filter { it.value.tiempo > 0 }
    val total = jugados.values.sumOf { it.tiempo }
    val sesiones = e.datos.values.sumOf { it.sesiones }
    val porConsola = jugados.entries.groupBy { it.key.substringBefore('/') }
        .mapValues { (_, v) -> v.sumOf { it.value.tiempo } }.entries.sortedByDescending { it.value }
    val top = jugados.entries.sortedByDescending { it.value.tiempo }.take(5)
    val recientes = e.datos.entries.filter { it.value.ultimo > 1 }.sortedByDescending { it.value.ultimo }.take(5)
    fun titulo(clave: String) = e.juegos.firstOrNull { it.clave == clave }?.titulo ?: clave.substringAfter('/')
    // Las barras crecen al abrir.
    val crecer = remember { Animatable(0f) }
    LaunchedEffect(Unit) { crecer.animateTo(1f, tween(700)) }

    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Column(Modifier.fillMaxSize().padding(start = esc.d(16), end = esc.d(16), top = esc.d(12), bottom = esc.d(10))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.RELOJ, p.tinta, 22f, acento = Coral)
                Spacer(Modifier.width(esc.d(10)))
                Texto("Tus estadísticas", 21f, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(esc.d(10)))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                    Cifra(formatoTiempo(total), "jugadas", Modifier.weight(1f))
                    Cifra("${jugados.size}", "juegos jugados", Modifier.weight(1f))
                    Cifra("$sesiones", "partidas", Modifier.weight(1f))
                }
                if (jugados.isEmpty()) Detalle("Todavía no juegas con Díptico. Cuando lo hagas, aquí verás tus horas, tus juegos favoritos y más.", 15f)
                if (porConsola.isNotEmpty()) {
                    Etiqueta("Horas por consola", Coral, 12f)
                    val maximo = porConsola.first().value.coerceAtLeast(1)
                    for ((c, t) in porConsola) Barra(Consolas.porId(c)?.nombre ?: c, formatoTiempo(t), t / maximo.toFloat() * crecer.value)
                }
                if (top.isNotEmpty()) {
                    Etiqueta("Tus más jugados", Coral, 12f)
                    val maximo = top.first().value.tiempo.coerceAtLeast(1)
                    for ((k, d) in top) Barra(titulo(k), formatoTiempo(d.tiempo) + if (d.sesiones > 0) " · ${d.sesiones} partidas" else "", d.tiempo / maximo.toFloat() * crecer.value)
                }
                if (recientes.isNotEmpty()) {
                    Etiqueta("Últimas partidas", Coral, 12f)
                    val fecha = cl.nikalbazu.diptico.estado.Idioma.formato("EEEdMMMHHmm")
                    for ((k, d) in recientes) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Texto(titulo(k), 14.5f, lineas = 1, modifier = Modifier.weight(1f))
                            Detalle(fecha.format(Date(d.ultimo)), 12.5f, lineas = 1)
                        }
                    }
                }
                // RetroAchievements
                val ra = e.perfilRA
                if (ra != null) {
                    Etiqueta("RetroAchievements", Sol, 12f)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(16))).background(p.superficie).padding(esc.d(10)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val foto = rememberImagen(android.net.Uri.parse(ra.foto), 128)
                        Box(Modifier.size(esc.d(46)).clip(CircleShape).background(p.fondo)) {
                            if (foto != null) Image(foto, null, Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.width(esc.d(10)))
                        Column(Modifier.weight(1f)) {
                            Texto(ra.usuario, 16f, lineas = 1)
                            Detalle(if (ra.desde.isNotEmpty()) "Desde ${ra.desde}" else "", 12f, lineas = 1)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Texto("${ra.puntos}", 18f, color = Sol)
                            Detalle("puntos", 12f)
                        }
                    }
                    for ((juego, l) in ra.recientes.take(5)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val img = rememberImagen(android.net.Uri.parse(l.imagen), 96)
                            Box(Modifier.size(esc.d(30)).clip(RoundedCornerShape(esc.d(8))).background(p.fondo)) {
                                if (img != null) Image(img, null, Modifier.fillMaxSize())
                            }
                            Spacer(Modifier.width(esc.d(8)))
                            Column(Modifier.weight(1f)) {
                                Texto(l.titulo, 13.5f, lineas = 1)
                                Detalle(juego, 11.5f, lineas = 1)
                            }
                            Texto("+${l.puntos}", 13f, color = Sol)
                        }
                    }
                } else if (e.usuarioRA.isBlank()) {
                    Detalle("Conecta RetroAchievements en Menú › Logros para ver aquí tus puntos y tus últimos logros.", 12.5f)
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("B" to "Cerrar")
        }
    }
}

@Composable
private fun Cifra(valor: String, texto: String, modifier: Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column(modifier.clip(RoundedCornerShape(esc.d(16))).background(p.superficie).padding(esc.d(10))) {
        Texto(valor, 18f, color = p.acentoTexto, lineas = 1)
        Detalle(texto, 12f, lineas = 1)
    }
}

@Composable
private fun Barra(nombre: String, valor: String, fraccion: Float) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Texto(nombre, 14f, lineas = 1, modifier = Modifier.weight(1f))
            Detalle(valor, 12.5f, lineas = 1)
        }
        Spacer(Modifier.height(esc.d(3)))
        Box(Modifier.fillMaxWidth().height(esc.d(10)).clip(RoundedCornerShape(50)).background(p.linea)) {
            Box(Modifier.fillMaxWidth(fraccion.coerceIn(0.02f, 1f)).height(esc.d(10)).clip(RoundedCornerShape(50)).fondoBoton(Uva))
        }
    }
}

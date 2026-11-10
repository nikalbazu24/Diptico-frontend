package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.TuAno

private val TextoClaro = Color(0xFFF4F5FB)
private val TextoSuave = Color(0xFFA5A8BD)

/**
 * «Tu año»: una tarjeta con lo que jugaste en el año (horas, días, juegos, tu mejor mes, tus más jugados) y un comentario de DIPPO.
 * Sale sobre el fondo difuminado (lo desenfoca [Raiz]); B o tocar afuera la cierra. Se abre desde tu perfil (Y) o desde Ajustes.
 */
@Composable
fun PanelTuAno(e: Estado, miPantalla: Int) {
    val abierto = e.extra.anioEn == miPantalla
    val animar = LocalMovimiento.current
    val t by animateFloatAsState(
        if (abierto) 1f else 0f,
        if (!animar) snap() else tween(if (abierto) 320 else 200),
        label = "tuAno",
    )
    if (!abierto && t < 0.01f) return
    val esc = LocalEscala.current
    val color = Color(e.extra.perfil.color)
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = t.coerceIn(0f, 1f) }.background(Color(0x77080912))
                .clickable(remember { MutableInteractionSource() }, indication = null) { Diptico.cerrarTuAno() },
        )
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    val s = 0.92f + 0.08f * t
                    scaleX = s; scaleY = s
                    alpha = (t * 2f).coerceIn(0f, 1f)
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
                    .testTag("panel-tu-ano"),
            ) { ContenidoTuAno(e, color) }
        }
    }
}

@Composable
private fun ContenidoTuAno(e: Estado, color: Color) {
    val esc = LocalEscala.current
    val anio = remember { java.time.LocalDate.now().year }
    val r = remember(e.datos, e.juegos) { TuAno.resumen(e, anio) }
    val total = remember(e.datos, e.juegos, e.consolasConJuegos) { Diptico.resumenPerfil(e) }
    val partidasTotal = remember(e.datos) { e.datos.values.sumOf { it.sesiones } }
    Column(Modifier.fillMaxSize().padding(esc.d(if (esc.angosta) 16 else 22))) {
        if (esc.angosta) {
            // Pantalla casi cuadrada: todo en una columna que se desliza con el dedo.
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(esc.d(10)),
            ) {
                Cabecera(r, anio, color)
                Tarjetas(e, r, color)
                Grafico(e, r, color)
                Totales(total.segundos, partidasTotal, total.juegosJugados, color)
                Nota(r)
            }
        } else {
            // Pantalla ancha: a la izquierda tus números y a la derecha el mes a mes y tus más jugados, para verlo todo sin deslizar.
            Box(Modifier.weight(1f).fillMaxWidth()) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(esc.d(22))) {
                Column(Modifier.weight(1.3f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
                    Cabecera(r, anio, color)
                    Tarjetas(e, r, color)
                    Totales(total.segundos, partidasTotal, total.juegosJugados, color)
                }
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
                    Grafico(e, r, color)
                    Nota(r)
                }
            }
            // En la pantalla ancha el botón va arriba a la derecha, para dejarle todo el alto al contenido.
            BotonCerrar(Modifier.align(Alignment.TopEnd))
            }
        }
        if (esc.angosta) {
            Spacer(Modifier.height(esc.d(10)))
            BotonCerrar(Modifier)
        }
    }
}

@Composable
private fun BotonCerrar(modifier: Modifier) {
    val esc = LocalEscala.current
    Box(
        modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.1f)).clickable { Diptico.cerrarTuAno() }
            .padding(horizontal = esc.d(16), vertical = esc.d(7)),
    ) { Texto("B · Cerrar", 13.5f, color = TextoClaro, lineas = 1) }
}

/** DIPPO celebra y comenta el año. */
@Composable
private fun Cabecera(r: TuAno.Resumen, anio: Int, color: Color) {
    val esc = LocalEscala.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
        cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(if (esc.angosta) 56 else 64).value, cara = CaraDippo.CELEBRANDO)
        Column(Modifier.weight(1f)) {
            Etiqueta("Tu año", color, 12f)
            Texto("$anio", 34f, color = TextoClaro, peso = FontWeight.Black, lineas = 1, traducir = false)
            Detalle(TuAno.comentario(r), 14f, color = TextoClaro, lineas = 3)
        }
    }
}

/** Las seis tarjetas del año: horas, días, juegos, juego, consola y mejor mes. */
@Composable
private fun Tarjetas(e: Estado, r: TuAno.Resumen, color: Color) {
    val esc = LocalEscala.current
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
            Dato("Horas jugadas", horas(r.segundos), color, Modifier.weight(1f)) { Conteo(r.partidas, if (r.partidas == 1) "partida" else "partidas", color) }
            Dato("Días jugados", "${r.dias}", color, Modifier.weight(1f)) { Conteo(r.rachaMax, "Racha máxima", color, numeroAlFinal = true) }
            Dato("Juegos distintos", "${r.juegos}", color, Modifier.weight(1f)) {}
        }
        if (r.hayDatos) {
            val primero = r.top.firstOrNull()
            val juego = primero?.let { e.juegosPorClave[it.first] }
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                Dato("Juego del año", juego?.titulo ?: "—", color, Modifier.weight(1f), tam = 14f) {
                    primero?.let { Texto(horas(it.second), 11.5f, color = color, lineas = 1, traducir = false) }
                }
                Dato("Consola del año", r.consola.ifBlank { "—" }, color, Modifier.weight(1f), tam = 14f) {}
                Dato("Mejor mes", if (r.mejorMes > 0) TuAno.nombreMes(r.mejorMes) else "—", color, Modifier.weight(1f), tam = 14f) {
                    if (r.mejorMes > 0) Texto(horas(r.porMes[r.mejorMes - 1]), 11.5f, color = color, lineas = 1, traducir = false)
                }
            }
        }
    }
}

/** El mes a mes y los cinco más jugados (solo si hay partidas del año). */
@Composable
private fun Grafico(e: Estado, r: TuAno.Resumen, color: Color) {
    val esc = LocalEscala.current
    if (!r.hayDatos) return
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
        Etiqueta("Mes a mes", TextoSuave, 11.5f)
        Meses(r, color)
        Etiqueta("Tus más jugados", TextoSuave, 11.5f)
        Top(e, r, color)
    }
}

/** Lo de siempre: horas, partidas y juegos en total. */
@Composable
private fun Totales(segundos: Long, partidas: Int, juegos: Int, color: Color) {
    val esc = LocalEscala.current
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
        Etiqueta("En total contigo", TextoSuave, 11.5f)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
            Texto(horas(segundos), 15f, color = TextoClaro, peso = FontWeight.Black, lineas = 1, traducir = false)
            Texto("·", 15f, color = TextoSuave, lineas = 1, traducir = false)
            Conteo(partidas, if (partidas == 1) "partida" else "partidas", color)
            Texto("·", 15f, color = TextoSuave, lineas = 1, traducir = false)
            Conteo(juegos, "juegos", color)
        }
    }
}

/** Desde cuándo se cuentan las partidas del año (o que todavía no hay). */
@Composable
private fun Nota(r: TuAno.Resumen) {
    val esc = LocalEscala.current
    if (r.desde.isNotBlank()) {
        val fecha = remember(r.desde) {
            try {
                java.time.LocalDate.parse(r.desde).format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
            } catch (_: Exception) { r.desde }
        }
        Column {
            Detalle("Cuento tus partidas del año desde", 12f, color = TextoSuave, lineas = 2)
            Texto(fecha, 12f, color = TextoSuave, lineas = 1, traducir = false)
        }
    } else Detalle("Todavía no hay partidas anotadas este año. Desde ahora Díptico las va contando.", 12f, color = TextoSuave, lineas = 2)
}

/** Una tarjetita con un título, un valor grande y, si hace falta, una línea de abajo. */
@Composable
private fun Dato(etiqueta: String, valor: String, color: Color, modifier: Modifier, traducirValor: Boolean = false, tam: Float = 18f, sub: @Composable () -> Unit) {
    val esc = LocalEscala.current
    Column(
        modifier.clip(RoundedCornerShape(esc.d(16))).background(Color.White.copy(alpha = 0.07f)).padding(horizontal = esc.d(12), vertical = esc.d(7)),
    ) {
        Texto(etiqueta, 10.5f, color = TextoSuave, lineas = 1)
        Texto(valor, tam, color = TextoClaro, peso = FontWeight.Black, lineas = if (tam < 18f) 2 else 1, traducir = traducirValor)
        sub()
    }
}

/** «12 partidas»: el número aparte para que el texto se pueda traducir. */
@Composable
private fun Conteo(n: Int, etiqueta: String, color: Color, numeroAlFinal: Boolean = false) {
    val esc = LocalEscala.current
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(4))) {
        if (numeroAlFinal) {
            Texto(etiqueta, 11.5f, color = color, lineas = 1)
            Texto("$n", 11.5f, color = color, peso = FontWeight.Black, lineas = 1, traducir = false)
        } else {
            Texto("$n", 11.5f, color = color, peso = FontWeight.Black, lineas = 1, traducir = false)
            Texto(etiqueta, 11.5f, color = color, lineas = 1)
        }
    }
}

/** Doce barritas, una por mes; el mejor mes va resaltado. */
@Composable
private fun Meses(r: TuAno.Resumen, color: Color) {
    val esc = LocalEscala.current
    val max = (r.porMes.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Row(Modifier.fillMaxWidth().height(esc.d(70)), horizontalArrangement = Arrangement.spacedBy(esc.d(5))) {
        for (m in 1..12) {
            val v = r.porMes[m - 1]
            val fraccion = if (v > 0) (0.08f + 0.92f * v / max).coerceIn(0.08f, 1f) else 0.04f
            val tono = if (m == r.mejorMes) color else Color.White.copy(alpha = if (v > 0) 0.35f else 0.12f)
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth().fillMaxHeight(fraccion).clip(RoundedCornerShape(esc.d(5))).background(tono))
                }
                Spacer(Modifier.height(esc.d(3)))
                Texto(TuAno.nombreMes(m).take(1), 10.5f, color = if (m == r.mejorMes) TextoClaro else TextoSuave, lineas = 1, traducir = false)
            }
        }
    }
}

/** Los cinco juegos con más tiempo en el año, con su portada y una barrita. */
@Composable
private fun Top(e: Estado, r: TuAno.Resumen, color: Color) {
    val esc = LocalEscala.current
    val max = (r.top.firstOrNull()?.second ?: 1L).coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
        r.top.forEachIndexed { i, (clave, seg) ->
            val juego = e.juegosPorClave[clave]
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                Texto("${i + 1}", 14f, color = color, peso = FontWeight.Black, lineas = 1, traducir = false, modifier = Modifier.width(esc.d(16)))
                Box(Modifier.size(esc.d(30)).clip(RoundedCornerShape(esc.d(7))).background(Color.White.copy(alpha = 0.06f))) {
                    if (juego != null) ArteJuego(juego, preferirArte = false, maxLado = 120, modifier = Modifier.fillMaxSize())
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(3))) {
                    Texto(juego?.titulo ?: clave, 13.5f, color = TextoClaro, lineas = 1, traducir = false)
                    Box(Modifier.fillMaxWidth().height(esc.d(4)).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.1f))) {
                        Box(Modifier.fillMaxWidth((seg.toFloat() / max).coerceIn(0.03f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
                    }
                }
                Texto(horas(seg), 13f, color = TextoSuave, lineas = 1, traducir = false)
            }
        }
    }
}

private fun horas(seg: Long): String = when {
    seg >= 36_000 -> "${seg / 3600} h"
    seg >= 3600 -> "%.1f h".format(java.util.Locale.ROOT, seg / 3600.0)
    else -> "${seg / 60} min"
}

package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import cl.nikalbazu.diptico.audio.Sonidos
import cl.nikalbazu.diptico.estado.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Los widgets útiles (deportes, contadores, tiempo y fechas). Todos se dibujan según su tamaño en celdas
 * ([an] × [al]): al estirarlos en el editor se acomodan al tiro, sin esperar a soltar.
 */
@Composable
internal fun WidgetUtil(e: Estado, tipo: String, an: Int, al: Int) {
    when (tipo) {
        "marcador" -> WMarcador(e, an, al)
        "proximoPartido" -> WProximoPartido(e, an, al)
        "tabla" -> WTabla(e, an, al)
        "contador" -> WContador(e, an, al)
        "marcadorMesa" -> WMarcadorMesa(e, an, al)
        "dados" -> WDados(e, an, al)
        "regresiva" -> WRegresiva(e, an, al)
        "nota" -> WNota(e, al)
        "clima" -> WClima(e, an, al)
        "feriados" -> WFeriados(an, al)
        "horaMundo" -> WHoraMundo(e, an, al)
    }
}

/** Lee algo de internet al aparecer y lo vuelve a leer cada [cada] ms (según lo último leído). */
@Composable
private fun <T> lectura(vararg claves: Any?, cada: (T?) -> Long, leer: () -> T?): Pair<T?, Boolean> {
    val r by produceState<Pair<T?, Boolean>>(null to true, *claves) {
        while (true) {
            val v = withContext(Dispatchers.IO) { runCatching { leer() }.getOrNull() }
            value = (v ?: value.first) to false
            delay(cada(v))
        }
    }
    return r
}

private fun abrirAjustes(tipo: String) {
    val i = Diptico.estado.value.widgets.indexOfFirst { it.tipo == tipo }
    if (i >= 0) Diptico.abrirAjustesWidget(i)
}

/** Un número grande que crece con el alto del widget. */
private fun grande(al: Int, an: Int) = when { al >= 3 && an >= 2 -> 52f; al >= 2 -> 38f; else -> 24f }

// ———————————————————————————— Deportes ————————————————————————————

@Composable
private fun Escudo(eq: Deportes.Equipo, tam: Float) {
    val esc = LocalEscala.current
    val color = runCatching { Color(android.graphics.Color.parseColor("#" + eq.color.ifEmpty { "6B4EFF" })) }.getOrDefault(Uva)
    Box(Modifier.size(esc.d(tam)).clip(CircleShape).background(color).border(esc.d(1), Color.White.copy(alpha = .4f), CircleShape), contentAlignment = Alignment.Center) {
        Texto(eq.sigla.take(3), tam * 0.32f, color = if (color.luminance() > .6f) Color(0xFF12131F) else Color.White, lineas = 1, traducir = false)
    }
}

private fun Color.luminance() = 0.2126f * red + 0.7152f * green + 0.0722f * blue

private fun horaCorta(ms: Long): String {
    val z = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    val dias = ChronoUnit.DAYS.between(LocalDate.now(), z.toLocalDate())
    val dia = when (dias) { 0L -> "Hoy"; 1L -> "Mañana"; else -> z.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es")).replaceFirstChar { it.uppercase() } + " " + z.dayOfMonth }
    return "$dia · " + z.format(DateTimeFormatter.ofPattern("HH:mm"))
}

@Composable
private fun EnVivo(p: Deportes.Partido) {
    val color = when { p.enVivo -> Color(0xFFFF5D5D); p.terminado -> LocalPaleta.current.tenue; else -> Sol }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(LocalEscala.current.d(7)).clip(CircleShape).background(color))
        Spacer(Modifier.width(LocalEscala.current.d(5)))
        Etiqueta(when { p.enVivo -> "En vivo · ${p.detalle}"; p.terminado -> "Final"; else -> horaCorta(p.fecha) }, color, 10f)
    }
}

@Composable
private fun WMarcador(e: Estado, an: Int, al: Int) {
    val esc = LocalEscala.current
    val liga = Deportes.liga(e.conf("marcador", "liga", Deportes.LIGAS[0].id))
    val equipo = e.conf("marcador", "equipo")
    val (partidos, cargando) = lectura<List<Deportes.Partido>>(liga.id, equipo, cada = { l -> if (l.orEmpty().any { it.enVivo }) 60_000L else 600_000L }) {
        if (equipo.isNotEmpty()) listOfNotNull(Deportes.deEquipo(liga, equipo)) else Deportes.hoy(liga)
    }
    Tarjeta(alPulsar = { abrirAjustes("marcador") }) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
            Etiqueta(liga.nombre, Coral, 10f)
            val l = partidos.orEmpty()
            when {
                l.isEmpty() -> Detalle(if (cargando) "Buscando partidos…" else if (partidos == null) "Sin conexión con los resultados." else "Hoy no hay partidos. Toca para elegir liga o equipo.", 11.5f, lineas = 2)
                al == 1 || l.size == 1 -> FilaPartidoGrande(l[0], al, an)
                else -> l.take(al * 2 - 1).forEach { FilaPartido(it) }
            }
        }
    }
}

@Composable
private fun FilaPartidoGrande(p: Deportes.Partido, al: Int, an: Int) {
    val esc = LocalEscala.current
    if (al == 1) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
            Escudo(p.local, 26f)
            Texto("${p.local.sigla} ${p.local.marcador.ifEmpty { "–" }}  ·  ${p.visita.marcador.ifEmpty { "–" }} ${p.visita.sigla}", 15f, lineas = 1, traducir = false, modifier = Modifier.weight(1f))
            Escudo(p.visita, 26f)
        }
        EnVivo(p)
        return
    }
    val tamE = if (al >= 3) 52f else 38f
    Row(Modifier.fillMaxWidth().padding(top = esc.d(4)), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Escudo(p.local, tamE); Detalle(p.local.nombre, 10.5f, lineas = 1)
        }
        Texto(if (p.estado == "pre") "vs" else "${p.local.marcador} – ${p.visita.marcador}", grande(al, an), lineas = 1, traducir = false)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Escudo(p.visita, tamE); Detalle(p.visita.nombre, 10.5f, lineas = 1)
        }
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { EnVivo(p) }
}

@Composable
private fun FilaPartido(p: Deportes.Partido) {
    val esc = LocalEscala.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(5))) {
        Escudo(p.local, 20f)
        Texto("${p.local.sigla} ${p.local.marcador}", 12f, lineas = 1, traducir = false)
        Texto("–", 12f, traducir = false)
        Texto("${p.visita.marcador} ${p.visita.sigla}", 12f, lineas = 1, traducir = false)
        Escudo(p.visita, 20f)
        Spacer(Modifier.weight(1f))
        Detalle(when { p.enVivo -> p.detalle; p.terminado -> "Final"; else -> horaCorta(p.fecha).substringAfter("· ") }, 10f, lineas = 1,
            color = if (p.enVivo) Color(0xFFFF5D5D) else LocalPaleta.current.tenue)
    }
}

@Composable
private fun WProximoPartido(e: Estado, an: Int, al: Int) {
    val esc = LocalEscala.current
    val liga = Deportes.liga(e.conf("proximoPartido", "liga", Deportes.LIGAS[0].id))
    val equipo = e.conf("proximoPartido", "equipo")
    val (p, cargando) = lectura<Deportes.Partido>(liga.id, equipo, cada = { if (it?.enVivo == true) 60_000L else 1_800_000L }) {
        if (equipo.isEmpty()) null else Deportes.deEquipo(liga, equipo)
    }
    Tarjeta(alPulsar = { abrirAjustes("proximoPartido") }) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
            Etiqueta("Próximo partido", Coral, 10f)
            when {
                equipo.isEmpty() -> Detalle("Toca para elegir tu equipo.", 12f, lineas = 2)
                p == null -> Detalle(if (cargando) "Buscando…" else "No encontré su próximo partido.", 12f, lineas = 2)
                else -> {
                    val propio = if (p.local.id == equipo) p.local else p.visita
                    val rival = if (p.local.id == equipo) p.visita else p.local
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        Escudo(propio, if (al >= 2) 34f else 24f); Texto("vs", 12f, traducir = false); Escudo(rival, if (al >= 2) 34f else 24f)
                    }
                    Texto(rival.nombre, if (al >= 2) 16f else 13f, lineas = 1, traducir = false)
                    if (p.enVivo || p.terminado) EnVivo(p) else {
                        Detalle(horaCorta(p.fecha), 11.5f, lineas = 1)
                        if (al >= 2) {
                            val falta = p.fecha - System.currentTimeMillis()
                            val txt = when { falta < 3_600_000L -> "¡Ya casi!"; falta < 86_400_000L -> "Faltan ${falta / 3_600_000L} h"; else -> "Faltan ${falta / 86_400_000L} días" }
                            Texto(txt, 13f, color = Sol, lineas = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WTabla(e: Estado, an: Int, al: Int) {
    val esc = LocalEscala.current
    val liga = Deportes.liga(e.conf("tabla", "liga", Deportes.LIGAS[0].id))
    val equipo = e.conf("tabla", "equipo")
    val (filas, cargando) = lectura<List<Deportes.Fila>>(liga.id, cada = { 3_600_000L }) { Deportes.tabla(liga) }
    Tarjeta(alPulsar = { abrirAjustes("tabla") }) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(3))) {
            Etiqueta(liga.nombre, Coral, 10f)
            val l = filas.orEmpty()
            if (l.isEmpty()) { Detalle(if (cargando) "Cargando la tabla…" else "No pude leer la tabla.", 11.5f, lineas = 2); return@Column }
            val cuantas = (al * 3 - 1).coerceAtLeast(2)
            // Si tu equipo no entra en los primeros, se muestra al final.
            val mia = l.indexOfFirst { it.equipo.id == equipo }
            val vista = if (mia >= cuantas) l.take(cuantas - 1) + l[mia] else l.take(cuantas)
            vista.forEach { f ->
                val propia = f.equipo.id == equipo
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(6))).background(if (propia) Uva.copy(alpha = .28f) else Color.Transparent).padding(horizontal = esc.d(4)),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    Texto("${f.pos}", 11.5f, lineas = 1, traducir = false, modifier = Modifier.width(esc.d(18)))
                    Escudo(f.equipo, 16f)
                    Texto(if (an >= 3) f.equipo.nombre else f.equipo.sigla, 11.5f, lineas = 1, traducir = false, modifier = Modifier.weight(1f))
                    Detalle(f.valor, 11f, lineas = 1)
                }
            }
        }
    }
}

// ———————————————————————————— Contadores ————————————————————————————

@Composable
private fun BotonRedondoW(t: String, principal: Boolean, tam: Float, accion: () -> Unit) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    Box(Modifier.size(esc.d(tam)).clip(CircleShape).background(if (principal) p.tinta else p.tinta.copy(alpha = .1f))
        .border(esc.d(1), p.tinta.copy(alpha = .2f), CircleShape).clickable { Sonidos.mover(); accion() }, contentAlignment = Alignment.Center) {
        Texto(t, tam * 0.5f, color = if (principal) p.fondo else p.tinta, traducir = false)
    }
}

@Composable
private fun WContador(e: Estado, an: Int, al: Int) {
    val esc = LocalEscala.current
    val nombre = e.conf("contador", "nombre", "Contador")
    val paso = e.conf("contador", "paso", "1").toIntOrNull() ?: 1
    val reinicio = e.conf("contador", "reinicio", "nunca")
    // Vuelve a cero solo cuando cambia el día (o la semana).
    LaunchedEffect(reinicio) {
        val hoy = LocalDate.now()
        val marca = when (reinicio) { "dia" -> hoy.toString(); "semana" -> hoy.minusDays(hoy.dayOfWeek.value - 1L).toString(); else -> "" }
        if (marca.isNotEmpty() && e.conf("contador", "marca") != marca) Diptico.confWidget("contador", mapOf("valor" to "0", "marca" to marca))
    }
    val valor = e.conf("contador", "valor", "0").toIntOrNull() ?: 0
    fun cambiar(d: Int) = Diptico.confWidget("contador", "valor", (valor + d).coerceAtLeast(0).toString())
    Tarjeta {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
            Texto(nombre, 12f, lineas = if (al >= 2) 2 else 1, alinear = TextAlign.Center, modifier = Modifier.clickable { abrirAjustes("contador") })
            if (al == 1) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                BotonRedondoW("−", false, 28f) { cambiar(-paso) }
                Texto("$valor", 26f, lineas = 1, traducir = false)
                BotonRedondoW("+", true, 28f) { cambiar(paso) }
            } else {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Texto("$valor", grande(al, an) * 1.3f, lineas = 1, traducir = false) }
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                    BotonRedondoW("−", false, 36f) { cambiar(-paso) }
                    BotonRedondoW("+", true, 36f) { cambiar(paso) }
                }
            }
        }
    }
}

@Composable
private fun WMarcadorMesa(e: Estado, an: Int, al: Int) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val a = e.conf("marcadorMesa", "a", "Equipo 1"); val b = e.conf("marcadorMesa", "b", "Equipo 2")
    val pa = e.conf("marcadorMesa", "pa", "0").toIntOrNull() ?: 0; val pb = e.conf("marcadorMesa", "pb", "0").toIntOrNull() ?: 0
    Tarjeta {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
            listOf(Triple(a, pa, "pa"), Triple(b, pb, "pb")).forEachIndexed { k, (n, v, clave) ->
                if (k == 1) Box(Modifier.width(esc.d(1)).fillMaxHeight().background(p.tinta.copy(alpha = .15f)))
                Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(esc.d(12)))
                    .clickable { Sonidos.mover(); Diptico.confWidget("marcadorMesa", clave, (v + 1).toString()) },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Texto(n, 12f, lineas = 1, color = if (k == 0) Coral else Cielo, traducir = false)
                    Texto("$v", grande(al, an) * if (al >= 2) 1.2f else 1f, lineas = 1, traducir = false)
                    if (al >= 2) BotonRedondoW("−", false, 26f) { Diptico.confWidget("marcadorMesa", clave, (v - 1).coerceAtLeast(0).toString()) }
                }
            }
        }
    }
}

@Composable
private fun WDados(e: Estado, an: Int, al: Int) {
    val esc = LocalEscala.current
    val cuantos = e.conf("dados", "cantidad", "2").toIntOrNull()?.coerceIn(1, 3) ?: 2
    val caras = e.conf("dados", "caras", "6").toIntOrNull() ?: 6
    var valores by remember(cuantos, caras) { mutableStateOf(List(cuantos) { (1..caras).random() }) }
    var tirando by remember { mutableStateOf(0) }
    LaunchedEffect(tirando) {
        if (tirando == 0) return@LaunchedEffect
        repeat(8) { valores = List(cuantos) { (1..caras).random() }; delay(70L + it * 12L) }
        Sonidos.aceptar()
    }
    Tarjeta(alPulsar = { tirando++ }) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(8), Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                valores.forEach { v -> Dado(v, caras, Modifier.fillMaxHeight(if (al >= 2) 0.85f else 0.95f).aspectRatio(1f)) }
            }
            if (al >= 2) Detalle(if (cuantos > 1) "Suma ${valores.sum()} · toca para tirar" else "Toca para tirar", 11f, lineas = 1)
        }
    }
}

@Composable
private fun Dado(v: Int, caras: Int, modifier: Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(modifier.clip(RoundedCornerShape(22)).background(Color(0xFFF4F3FA)).border(esc.d(1), Color.White, RoundedCornerShape(22)), contentAlignment = Alignment.Center) {
        if (caras != 6) { Texto("$v", 22f, color = Color(0xFF12131F), lineas = 1, traducir = false); return@Box }
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension * 0.09f
            val a = 0.27f; val b = 0.5f; val c = 0.73f
            val pos = when (v) {
                1 -> listOf(b to b); 2 -> listOf(a to a, c to c); 3 -> listOf(a to a, b to b, c to c)
                4 -> listOf(a to a, c to a, a to c, c to c); 5 -> listOf(a to a, c to a, b to b, a to c, c to c)
                else -> listOf(a to a, c to a, a to b, c to b, a to c, c to c)
            }
            pos.forEach { (x, y) -> drawCircle(Color(0xFF12131F), r, Offset(size.width * x, size.height * y)) }
        }
    }
}

// ———————————————————————————— Tiempo y fechas ————————————————————————————

@Composable
private fun WRegresiva(e: Estado, an: Int, al: Int) {
    val esc = LocalEscala.current
    val (texto, fecha) = e.regresiva()
    val dias = ChronoUnit.DAYS.between(LocalDate.now(), fecha)
    Tarjeta(alPulsar = { abrirAjustes("regresiva") }) {
        if (al == 1) Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
            Texto(if (dias <= 0) "¡Hoy!" else "$dias", 26f, lineas = 1, traducir = false)
            Column { Etiqueta(if (dias == 1L) "Falta 1 día" else "Faltan", Coral, 10f); Detalle(texto, 11f, lineas = 1) }
        } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(2))) {
            Etiqueta("Faltan", Coral, 10f)
            Texto(if (dias <= 0) "¡Hoy!" else "$dias", grande(al, an) * 1.3f, lineas = 1, traducir = false)
            Texto(if (dias == 1L) "día" else "días", 14f, lineas = 1)
            Detalle(texto, 11.5f, lineas = 2)
            if (al >= 3) Detalle(fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es"))), 11f, lineas = 1)
        }
    }
}

@Composable
private fun WNota(e: Estado, al: Int) {
    val texto = e.conf("nota", "texto")
    Tarjeta(alPulsar = { Diptico.abrirTexto("wconf:nota:texto", texto) }) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(LocalEscala.current.d(4))) {
            Etiqueta("Nota", Sol, 10f)
            if (texto.isEmpty()) Detalle("Toca para escribir algo que no quieras olvidar.", 12f, lineas = 3)
            else Texto(texto, if (al >= 2) 16f else 13f, lineas = al * 3, traducir = false)
        }
    }
}

@Composable
private fun WClima(e: Estado, an: Int, al: Int) {
    val esc = LocalEscala.current
    val ciudad = e.conf("clima", "ciudad", "Santiago")
    val (c, cargando) = lectura<Clima.Hoy>(ciudad, cada = { 900_000L }) { Clima.de(ciudad) }
    Tarjeta(alPulsar = { abrirAjustes("clima") }) {
        if (c == null) { Detalle(if (cargando) "Mirando el cielo de $ciudad…" else "No pude leer el clima.", 12f, lineas = 2); return@Tarjeta }
        if (al == 1) Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
            IconoClima(c.codigo, c.dia, Modifier.size(esc.d(30)))
            Texto("${c.grados}°", 24f, lineas = 1, traducir = false)
            Column(Modifier.weight(1f)) { Texto(c.ciudad, 11.5f, lineas = 1, traducir = false); Detalle(Clima.texto(c.codigo), 10.5f, lineas = 1) }
        } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(3))) {
            Etiqueta(c.ciudad, Cielo, 10f)
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                IconoClima(c.codigo, c.dia, Modifier.fillMaxHeight(0.8f).aspectRatio(1f))
                Texto("${c.grados}°", grande(al, an) * 1.2f, lineas = 1, traducir = false)
            }
            Texto(Clima.texto(c.codigo), 13f, lineas = 1)
            Detalle("Máx ${c.max}° · Mín ${c.min}°" + if (c.lluvia > 0) " · lluvia ${c.lluvia} %" else "", 11f, lineas = 1)
        }
    }
}

/** Sol, nubes, lluvia o nieve dibujados (sin emojis). */
@Composable
private fun IconoClima(codigo: Int, dia: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val nube = codigo >= 2
        if (codigo <= 2) {
            if (dia) {
                drawCircle(Color(0xFFFFC93C), w * 0.2f, Offset(w * 0.42f, h * 0.4f))
                for (k in 0 until 8) {
                    val ang = Math.toRadians(k * 45.0)
                    val o = Offset(w * 0.42f + (w * 0.3f * Math.cos(ang)).toFloat(), h * 0.4f + (h * 0.3f * Math.sin(ang)).toFloat())
                    drawCircle(Color(0xFFFFC93C), w * 0.035f, o)
                }
            } else drawCircle(Color(0xFFE8E6FF), w * 0.2f, Offset(w * 0.42f, h * 0.4f))
        }
        if (nube) {
            val c = if (codigo in 95..99 || codigo >= 61) Color(0xFF9AA3C7) else Color(0xFFE6E8F5)
            drawCircle(c, w * 0.16f, Offset(w * 0.38f, h * 0.55f))
            drawCircle(c, w * 0.21f, Offset(w * 0.58f, h * 0.48f))
            drawRoundRect(c, Offset(w * 0.22f, h * 0.55f), Size(w * 0.6f, h * 0.17f), androidx.compose.ui.geometry.CornerRadius(h * 0.08f))
        }
        if (codigo in 51..67 || codigo in 80..82 || codigo in 95..99) for (k in 0..2)
            drawLine(Color(0xFF7FD1FF), Offset(w * (0.36f + k * 0.15f), h * 0.78f), Offset(w * (0.32f + k * 0.15f), h * 0.92f), w * 0.04f)
        if (codigo in 71..77 || codigo in 85..86) for (k in 0..2) drawCircle(Color.White, w * 0.035f, Offset(w * (0.35f + k * 0.15f), h * 0.85f))
        if (codigo in 45..48) for (k in 0..1) drawLine(Color(0xFFB9BBD6), Offset(w * 0.2f, h * (0.8f + k * 0.1f)), Offset(w * 0.8f, h * (0.8f + k * 0.1f)), w * 0.04f)
    }
}

@Composable
private fun WFeriados(an: Int, al: Int) {
    val esc = LocalEscala.current
    val (lista, cargando) = lectura<List<Feriados.Feriado>>("feriados", cada = { 21_600_000L }) { Feriados.proximos(8) }
    val es = Locale.forLanguageTag("es")
    Tarjeta {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(3))) {
            Etiqueta("Feriados", Coral, 10f)
            val l = lista.orEmpty()
            if (l.isEmpty()) { Detalle(if (cargando) "Revisando el calendario…" else "No pude leer los feriados.", 11.5f, lineas = 2); return@Column }
            val f = l[0]
            val dias = ChronoUnit.DAYS.between(LocalDate.now(), f.fecha)
            Texto(f.nombre, if (al >= 2) 15f else 13f, lineas = 1, traducir = false)
            Detalle(f.fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", es)) + " · " + when (dias) { 0L -> "¡hoy!"; 1L -> "mañana"; else -> "en $dias días" }, 11f, lineas = 1)
            if (al >= 2) l.drop(1).take(al * 2 - 1).forEach { x ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                    Detalle(x.fecha.format(DateTimeFormatter.ofPattern("d MMM", es)), 10.5f, lineas = 1, modifier = Modifier.width(esc.d(46)))
                    Texto(x.nombre, 11f, lineas = 1, traducir = false)
                }
            }
        }
    }
}

@Composable
private fun WHoraMundo(e: Estado, an: Int, al: Int) {
    val esc = LocalEscala.current
    var ahora by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(20_000); ahora = ZonedDateTime.now() } }
    val zonas: List<Pair<String, String>> = listOf("Aquí" to ZoneId.systemDefault().id) +
        listOf(e.conf("horaMundo", "zona1", "Asia/Tokyo"), e.conf("horaMundo", "zona2", "America/New_York"))
            .map { z -> (ZONAS.firstOrNull { it.second == z }?.first ?: z) to z }
    Tarjeta(alPulsar = { abrirAjustes("horaMundo") }) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(esc.d(8)), verticalAlignment = Alignment.CenterVertically) {
            zonas.forEach { (n, z) ->
                val t = ahora.withZoneSameInstant(ZoneId.of(z))
                val dif = (t.offset.totalSeconds - ahora.offset.totalSeconds) / 3600
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Detalle(n, 10.5f, lineas = 1)
                    Texto(t.format(DateTimeFormatter.ofPattern("HH:mm")), if (al >= 2) 24f else 18f, lineas = 1, traducir = false)
                    if (al >= 2 || an >= 3) Detalle(if (n == "Aquí") "" else (if (dif >= 0) "+$dif h" else "$dif h") + if (t.toLocalDate() != ahora.toLocalDate()) " · otro día" else "", 10f, lineas = 1)
                }
            }
        }
    }
}

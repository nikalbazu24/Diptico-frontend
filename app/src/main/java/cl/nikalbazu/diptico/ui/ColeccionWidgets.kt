package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/*
 * Colección viva · widgets (bocetos 7a, 7b y 7c). Cada widget toma los colores, el radio y la letra del tema.
 */

/** Los widgets de Colección viva, en el orden del boceto. (id, nombre, tamaño, qué muestra) */
val WIDGETS_CV = listOf(
    WidgetCV("continuar", "Continuar", 2, 2, "El juego que dejaste a medias"),
    WidgetCV("semana", "Tu semana", 2, 1, "Horas jugadas por día"),
    WidgetCV("dippo", "DIPPO", 2, 1, "Una idea para hoy y preguntas rápidas"),
    WidgetCV("hoy", "¿Qué juego hoy?", 1, 1, "Elige por ti según tu ánimo"),
    WidgetCV("logro", "Logro más cercano", 1, 1, "El que te falta poco"),
    WidgetCV("equipo", "Equipo", 1, 1, "Temperatura, batería y espacio"),
    WidgetCV("reloj", "Reloj", 1, 1, "La hora y la fecha en grande"),
    WidgetCV("favoritos", "Tus favoritos", 3, 1, "Repisa con tus cajas"),
    WidgetCV("extrana", "Te extraña", 1, 1, "Juegos sin tocar hace semanas"),
    WidgetCV("recuerdo", "Hace un año", 2, 1, "Lo que hacías hace un tiempo"),
)

data class WidgetCV(val id: String, val nombre: String, val an: Int, val al: Int, val detalle: String)

@Composable
fun WidgetsCV(e: Estado, unaPantalla: Boolean) {
    LienzoBoceto(1920f, 1080f, fondo = LocalColeccion.current.fondo) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(col.acento.copy(alpha = .16f), col.fondo))))
        // Barra: Portada | Widgets · Editar widgets · (perfil) hora y batería, nombre y título, foto.
        Row(Modifier.fillMaxWidth().padding(start = esc.d(48), end = esc.d(48), top = esc.d(24)).height(esc.d(72)), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
            PildoraTexto("Portada", false) { Diptico.fijarInicio("heroe") }
            PildoraTexto("Widgets", true) {}
            PildoraTexto(if (e.extra.editandoWidgetsCV) "Listo" else "Editar widgets", false) { Diptico.alternarEditarWidgetsCV() }
            if (e.extra.editandoWidgetsCV) PildoraTexto("Tienda de widgets", false) { Diptico.abrirGaleria("temas", 2) }
            Spacer(Modifier.weight(1f))
            HoraBateria(56f, 22f)
            BotonPerfil(80f)
        }
        val abajo = if (unaPantalla) 150f else 40f
        val visibles = WIDGETS_CV.filter { it.id !in e.extra.widgetsCVOcultos }
        // Cuadrícula 6×3 con separación 20 (los que se quitan dejan su lugar a los que siguen).
        Box(Modifier.fillMaxSize().padding(start = esc.d(48), end = esc.d(48), top = esc.d(124), bottom = esc.d(abajo))) {
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                val cw = (maxWidth.value / esc.factor - 5 * 20f) / 6f
                val ch = (maxHeight.value / esc.factor - 2 * 20f) / 3f
                val lugares = acomodar(visibles)
                lugares.forEach { (w, x, y) ->
                    Box(Modifier.offset(esc.d(x * (cw + 20f)), esc.d(y * (ch + 20f))).size(esc.d(w.an * cw + (w.an - 1) * 20f), esc.d(w.al * ch + (w.al - 1) * 20f))) {
                        TarjetaCV(e, w)
                    }
                }
            }
        }
        if (unaPantalla) Row(Modifier.align(Alignment.BottomCenter).padding(start = esc.d(48), end = esc.d(48), bottom = esc.d(26)).fillMaxWidth().height(esc.d(96)),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(18))) {
            if (e.extra.dippoInicio) DippoFrase("Cada widget toma los colores, la forma y la letra de tu tema.", Modifier.weight(1f), cara = 76f, letra = 19f, maxAncho = 1200f)
            else Spacer(Modifier.weight(1f))
            Row(Modifier.vidrio(44f, fuerte = true).padding(esc.d(8)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                FilaDock(e, 52f, conCajon = false)
                BotonRedondo(Icono.BUSCAR, 58f, { Diptico.abrirBuscar() }, descripcion = "Buscar")
                BotonRedondo(Icono.AJUSTES, 58f, { Diptico.abrirMenu() }, descripcion = "Ajustes")
            }
        }
    }
}

/** Acomoda los widgets en la cuadrícula de 6×3, de izquierda a derecha y de arriba abajo, donde quepan. */
private fun acomodar(lista: List<WidgetCV>): List<Triple<WidgetCV, Int, Int>> {
    val ocupado = Array(3) { BooleanArray(6) }
    val r = mutableListOf<Triple<WidgetCV, Int, Int>>()
    for (w in lista) {
        loop@ for (y in 0..(3 - w.al)) for (x in 0..(6 - w.an)) {
            if ((y until y + w.al).all { yy -> (x until x + w.an).none { xx -> ocupado[yy][xx] } }) {
                for (yy in y until y + w.al) for (xx in x until x + w.an) ocupado[yy][xx] = true
                r += Triple(w, x, y); break@loop
            }
        }
    }
    return r
}

@Composable
private fun PildoraTexto(t: String, on: Boolean, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Box(Modifier.height(esc.d(52)).clip(RoundedCornerShape(50)).background(if (on) col.acento else col.vidrio).border(1.dp, if (on) Color.Transparent else col.borde, RoundedCornerShape(50))
        .clickable(onClick = onClick).padding(horizontal = esc.d(18)), contentAlignment = Alignment.Center) {
        TB(t, 18f, color = if (on) Color.White else col.texto, peso = FontWeight.ExtraBold)
    }
}

/** El marco de cada widget: vidrio del tema; editando, borde punteado y ✕ para quitarlo. */
@Composable
private fun MarcoWidget(e: Estado, w: WidgetCV, fondo: Brush? = null, onClick: (() -> Unit)? = null, contenido: @Composable ColumnScope.() -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val forma = redondo(28f)
    val editando = e.extra.editandoWidgetsCV
    Box(Modifier.fillMaxSize().clip(forma).background(col.vidrioFuerte).then(if (fondo != null) Modifier.background(fondo) else Modifier)
        .border(1.dp, col.borde, forma)
        .then(if (editando) Modifier.drawBehind {
            drawRoundRect(col.acento, style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f * col.radio * esc.factor * density))
        } else Modifier)
        .then(if (onClick != null && !editando) Modifier.clickable(onClick = onClick) else Modifier)) {
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(22), vertical = esc.d(20)), verticalArrangement = Arrangement.spacedBy(esc.d(8)), content = contenido)
        if (editando) Box(Modifier.align(Alignment.TopEnd).padding(esc.d(10)).size(esc.d(34)).clip(CircleShape).background(col.acento)
            .clickable { Diptico.quitarWidgetCV(w.id) }, contentAlignment = Alignment.Center) { TB("✕", 16f, color = Color.White, peso = FontWeight.ExtraBold, traducir = false) }
    }
}

@Composable
private fun TarjetaCV(e: Estado, w: WidgetCV) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    when (w.id) {
        "continuar" -> {
            val j = e.juegos.filter { (e.datos[it.clave]?.ultimo ?: 0L) > 1 }.maxByOrNull { e.datos[it.clave]!!.ultimo } ?: e.seleccionado
            MarcoWidget(e, w, Brush.linearGradient(listOf(col.acento.copy(alpha = .45f), Color.Transparent)), onClick = j?.let { { Diptico.jugarDesdeWidget(it.clave) } }) {
                TB("CONTINUAR", 18f, color = col.sub, espaciado = 2.5f)
                if (j == null) { TB("Aún no juegas nada", 36f, titulo = true); return@MarcoWidget }
                TB(j.titulo, 44f, titulo = true, lineas = 2, interlineado = 44f, traducir = false)
                val c = Consolas.porId(j.consolaId)
                TB("${c?.bonito ?: ""} · ${haceCuanto(e.datos[j.clave]?.ultimo ?: 0L).lowercase()}", 21f, color = col.sub, traducir = false)
                Box(Modifier.weight(1f).fillMaxWidth().clip(redondo(16f))) { ArteJuego(j, preferirArte = true, maxLado = 640, modifier = Modifier.fillMaxSize()) }
                val d = e.extra.duraciones[j.clave]
                val jugado = e.datos[j.clave]?.tiempo ?: 0L
                if (d?.tiene == true && d.principal > 0f) {
                    val pct = ((jugado / 3600f) / d.principal).coerceIn(0f, 1f)
                    TB("Vas en el ${(pct * 100).toInt()} %" + (CuantoDura.teQueda(d, jugado)?.let { " · $it" } ?: ""), 19f, color = col.sub, traducir = false)
                    Box(Modifier.fillMaxWidth().height(esc.d(12)).clip(RoundedCornerShape(50)).background(col.texto.copy(alpha = .2f))) {
                        Box(Modifier.fillMaxWidth(pct).fillMaxHeight().background(col.acento))
                    }
                }
                Box(Modifier.height(esc.d(56)).clip(RoundedCornerShape(50)).background(col.acento).padding(horizontal = esc.d(26)), contentAlignment = Alignment.Center) {
                    TB("A · Continuar", 23f, color = Color.White, peso = FontWeight.ExtraBold)
                }
            }
        }
        "semana" -> MarcoWidget(e, w) {
            val dias = remember { runCatching { TuAno.ultimosDias(7) }.getOrDefault(emptyList()) }
            val total = dias.sumOf { it.second }
            TB("Tu semana", 26f, titulo = true)
            TB(if (total > 0) "${formatoTiempo(total)} en total" else "Aún no juegas esta semana", 18f, color = col.sub, traducir = false)
            val max = (dias.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(12)), verticalAlignment = Alignment.Bottom) {
                dias.forEachIndexed { i, (fecha, seg) ->
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(Modifier.fillMaxWidth(0.7f).fillMaxHeight((seg.toFloat() / max * 0.8f).coerceAtLeast(0.03f)).clip(redondo(7f))
                            .background(col.acento.copy(alpha = if (i == dias.size - 1) 1f else .6f)))
                        TB("LMXJVSD"[fecha.dayOfWeek.value - 1].toString(), 16f, color = col.sub, traducir = false)
                    }
                }
            }
        }
        "dippo" -> MarcoWidget(e, w) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                cl.nikalbazu.diptico.bisa.AvatarBisa(104f * esc.factor)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
                    TB("DIPPO", 22f, titulo = true)
                    TB(ideaDippo(e), 19f, lineas = 3, interlineado = 24f, traducir = false)
                    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                        ChipWidget("Sorpréndeme") { juegoAlAzar(e)?.let { Diptico.jugarDesdeWidget(it.clave) } }
                        ChipWidget("¿Qué me falta?") { Diptico.abrirAsistente() }
                    }
                }
            }
        }
        "hoy" -> MarcoWidget(e, w, onClick = { juegoAlAzar(e)?.let { Diptico.elegirJuego(it.clave); Diptico.fijarInicio("heroe") } }) {
            TB("¿Qué juego hoy?", 22f, titulo = true)
            val tres = remember(e.juegos.size) { e.juegos.shuffled(java.util.Random(java.time.LocalDate.now().toEpochDay())).take(3) }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(8)), verticalAlignment = Alignment.CenterVertically) {
                tres.forEach { j -> Box(Modifier.weight(1f).fillMaxHeight(0.8f).clip(RoundedCornerShape(esc.d(12)))) { ArteJuego(j, preferirArte = false, maxLado = 200, modifier = Modifier.fillMaxSize()) } }
            }
            TB("Toca para elegir al azar", 16f, color = col.sub)
        }
        "logro" -> MarcoWidget(e, w, onClick = { Diptico.abrirGaleria("logros_dippo") }) {
            val l = remember(e.datos.size) { runCatching { Diptico.proximoLogroDippo(e) }.getOrNull() }
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(6), Alignment.CenterVertically)) {
                val frac = l?.let { it.second.toFloat() / it.third.coerceAtLeast(1) } ?: 1f
                Box(Modifier.size(esc.d(104)).drawBehind {
                    drawArc(col.texto.copy(alpha = .3f), 0f, 360f, false, style = Stroke(13.dp.toPx() * esc.factor * 1.6f))
                    drawArc(col.acento, -90f, 360f * frac, false, style = Stroke(13.dp.toPx() * esc.factor * 1.6f, cap = StrokeCap.Round))
                }, contentAlignment = Alignment.Center) { TB(l?.let { "${it.second}/${it.third}" } ?: "✓", 22f, titulo = true, traducir = false) }
                TB(l?.first ?: "¡Todos!", 19f, titulo = true, lineas = 1, traducir = false)
                TB("tu logro más cercano", 15f, color = col.sub)
            }
        }
        "equipo" -> MarcoWidget(e, w) {
            val ctx = LocalContext.current
            var lectura by remember { mutableStateOf(Termica.pruebaLectura) }
            var libres by remember { mutableStateOf(-1L to -1L) }
            LaunchedEffect(Unit) {
                while (true) {
                    withContext(Dispatchers.IO) {
                        lectura = runCatching { Termica.leer(ctx) }.getOrNull()
                        libres = runCatching { android.os.StatFs(android.os.Environment.getExternalStorageDirectory().path).let { it.availableBytes to it.totalBytes } }.getOrDefault(-1L to -1L)
                    }
                    delay(30_000)
                }
            }
            val bat = rememberBateria()
            TB("Equipo", 22f, titulo = true)
            val l = lectura
            val (texto, color) = when (l?.nivel) {
                Termica.Nivel.FRESCA -> "fresca" to Color(0xFF5FBF5A); Termica.Nivel.TIBIA -> "tibia" to Color(0xFFF5C542)
                Termica.Nivel.CALIENTE -> "caliente" to Color(0xFFFF8A3D); Termica.Nivel.MUY_CALIENTE -> "muy caliente" to Color(0xFFFF4D4D)
                null -> "leyendo…" to col.sub
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                Box(Modifier.size(esc.d(14)).clip(CircleShape).background(color))
                TB((l?.let { "${maxOf(it.bateria, it.procesador).toInt()} °C · " } ?: "") + texto, 20f, peso = FontWeight.ExtraBold, traducir = false)
            }
            if (bat in 0..100) BarraDato("Batería $bat %", bat / 100f)
            if (libres.second > 0) BarraDato("Espacio ${(libres.second - libres.first) / 1_000_000_000} / ${libres.second / 1_000_000_000} GB",
                1f - libres.first.toFloat() / libres.second)
        }
        "reloj" -> MarcoWidget(e, w) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                TB(rememberHora(), 64f, titulo = true, traducir = false)
                TB(java.time.format.DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", java.util.Locale(Idioma.actual.ifEmpty { "es" })).format(java.time.LocalDate.now()), 21f, color = col.sub, traducir = false)
            }
        }
        "favoritos" -> MarcoWidget(e, w) {
            TB("Tus favoritos", 22f, titulo = true)
            val favs = e.juegos.filter { e.datos[it.clave]?.favorito == true }.ifEmpty { e.juegos.sortedByDescending { e.datos[it.clave]?.tiempo ?: 0L } }.take(9)
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                favs.forEachIndexed { i, j ->
                    Box(Modifier.width(esc.d(48f + (i * 5) % 10)).fillMaxHeight(0.82f + ((i * 11) % 18) / 100f).clip(redondo(5f))
                        .clickable { Diptico.elegirJuego(j.clave); Diptico.fijarInicio("heroe") }) {
                        ArteJuego(j, preferirArte = false, maxLado = 160, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        "extrana" -> {
            val j = remember(e.juegos.size) {
                val limite = System.currentTimeMillis() - 14L * 86_400_000L
                e.juegos.filter { val u = e.datos[it.clave]?.ultimo ?: 0L; u in 2 until limite }.maxByOrNull { e.datos[it.clave]?.tiempo ?: 0L }
            }
            MarcoWidget(e, w, onClick = j?.let { { Diptico.elegirJuego(it.clave); Diptico.fijarInicio("heroe") } }) {
                TB("Te extraña", 22f, titulo = true)
                TB(j?.let { "Hace ${((System.currentTimeMillis() - (e.datos[it.clave]?.ultimo ?: 0L)) / (7 * 86_400_000L)).coerceAtLeast(2)} semanas que no juegas ${it.titulo}." }
                    ?: "Has jugado todo hace poco. ¡Bien!", 18f, lineas = 3, interlineado = 23f, traducir = false)
                if (j != null) Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(esc.d(14)))) { ArteJuego(j, preferirArte = true, maxLado = 320, modifier = Modifier.fillMaxSize()) }
            }
        }
        "recuerdo" -> MarcoWidget(e, w) {
            val r = remember { runCatching { TuAno.recuerdo() }.getOrNull() }
            val j = r?.first?.let { f -> e.juegos.firstOrNull { it.clave == f.clave } }
            TB(r?.takeIf { j != null }?.let { "EL ${it.first.fecha.split("-").reversed().joinToString("/")}" } ?: "HACE UN TIEMPO", 17f, color = col.sub, espaciado = 2.4f, traducir = false)
            TB(j?.let { "Jugabas ${it.titulo}" } ?: "Aquí aparecerán tus recuerdos", 30f, titulo = true, lineas = 2, traducir = false)
            if (r != null && j != null) TB("${formatoTiempo(r.second)} ese día · ${j?.let { Consolas.porId(it.consolaId)?.bonito } ?: ""}", 20f, color = col.sub, traducir = false)
            Spacer(Modifier.weight(1f))
            if (j != null) ChipWidget("Ver la ficha") { Diptico.abrirFichaDe(j.clave, 0) }
        }
    }
}

@Composable
private fun ChipWidget(t: String, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Box(Modifier.height(esc.d(38)).clip(RoundedCornerShape(50)).border(1.dp, col.borde, RoundedCornerShape(50)).clickable(onClick = onClick)
        .padding(horizontal = esc.d(14)), contentAlignment = Alignment.Center) { TB(t, 16f, peso = FontWeight.ExtraBold) }
}

@Composable
private fun BarraDato(t: String, frac: Float) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    TB(t, 16f, color = col.sub, traducir = false)
    Box(Modifier.fillMaxWidth().height(esc.d(9)).clip(RoundedCornerShape(50)).background(col.texto.copy(alpha = .3f))) {
        Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).fillMaxHeight().background(col.acento))
    }
}

private fun juegoAlAzar(e: Estado): Juego? = e.juegos.filter { e.datos[it.clave]?.oculto != true }.randomOrNull()

private fun ideaDippo(e: Estado): String {
    val j = e.juegos.filter { (e.datos[it.clave]?.ultimo ?: 0L) > 1 }.maxByOrNull { e.datos[it.clave]!!.ultimo }
    val h = java.time.LocalTime.now().hour
    return when {
        j == null -> "¿Probamos algo nuevo hoy? Toca «Sorpréndeme»."
        h >= 22 || h < 6 -> "Es tarde: algo corto antes de dormir, ¿te parece?"
        else -> "Buen momento para seguir con ${j.titulo}."
    }
}

// ———————————————————————————— 7b · la tienda de widgets (Thor abajo) ————————————————————————————

@Composable
fun TiendaWidgetsCV(e: Estado) {
    LienzoBoceto(1240f, 1080f, fondo = LocalColeccion.current.fondo2) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val usados = WIDGETS_CV.filter { it.id !in e.extra.widgetsCVOcultos }.sumOf { it.an * it.al }
        Row(Modifier.fillMaxWidth().padding(start = esc.d(40), end = esc.d(40), top = esc.d(28)).height(esc.d(72)), verticalAlignment = Alignment.CenterVertically) {
            TB("Tienda de widgets", 36f, titulo = true, modifier = Modifier.weight(1f))
            Box(Modifier.height(esc.d(52)).vidrio(26f).padding(horizontal = esc.d(18)), contentAlignment = Alignment.Center) { TB("$usados de 18 espacios", 18f, peso = FontWeight.ExtraBold, traducir = false) }
        }
        LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize().padding(start = esc.d(40), end = esc.d(40), top = esc.d(124), bottom = esc.d(124)),
            horizontalArrangement = Arrangement.spacedBy(esc.d(18)), verticalArrangement = Arrangement.spacedBy(esc.d(18))) {
            itemsIndexed(WIDGETS_CV, key = { _, w -> w.id }) { _, w ->
                val puesto = w.id !in e.extra.widgetsCVOcultos
                Row(Modifier.fillMaxWidth().height(esc.d(132)).vidrio(28f, fuerte = true).padding(horizontal = esc.d(20)), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                    Box(Modifier.size(esc.d(96), esc.d(84)).clip(redondo(14f)).background(Brush.linearGradient(listOf(col.acento, Color(0xFF141628)))))
                    Column(Modifier.weight(1f)) {
                        TB(w.nombre, 22f, titulo = true)
                        TB(w.detalle, 17f, color = col.sub, lineas = 2)
                        TB("Tamaño ${w.an}×${w.al}", 15f, color = col.sub, traducir = false)
                    }
                    Box(Modifier.height(esc.d(46)).clip(RoundedCornerShape(50)).background(if (puesto) Color.Transparent else col.acento)
                        .border(1.dp, if (puesto) col.borde else Color.Transparent, RoundedCornerShape(50))
                        .clickable { if (puesto) Diptico.quitarWidgetCV(w.id) else Diptico.ponerWidgetCV(w.id) }.padding(horizontal = esc.d(18)), contentAlignment = Alignment.Center) {
                        TB(if (puesto) "Puesto ✓" else "Agregar", 18f, color = if (puesto) col.texto else Color.White, peso = FontWeight.ExtraBold)
                    }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(start = esc.d(40), end = esc.d(40), bottom = esc.d(28)).fillMaxWidth().height(esc.d(72)).vidrio(36f)
            .padding(horizontal = esc.d(24)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            PistaTecla("A", "Agregar o quitar"); PistaTecla("X", "Editar widgets"); PistaTecla("B", "Volver")
        }
    }
}

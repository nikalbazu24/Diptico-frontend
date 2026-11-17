package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import cl.nikalbazu.diptico.audio.Musica
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/** Los widgets nuevos de la tienda (16 nov 2026). */
val WIDGETS_VIVOS = setOf("dippoPropone", "teExtrana", "tuSemana", "equipo", "recuerdo", "radio")

@Composable
internal fun WidgetVivo(e: Estado, tipo: String, an: Int, al: Int) {
    when (tipo) {
        "dippoPropone" -> DippoPropone(e, al)
        "teExtrana" -> TeExtrana(e, an, al)
        "tuSemana" -> TuSemana(e, al)
        "equipo" -> TuEquipo(al)
        "recuerdo" -> UnRecuerdo(e, an, al)
        "radio" -> RadioTema(e, al)
    }
}

/** Un juego que se juega en ratos cortos (para proponerlo de noche). */
private fun cortoDe(e: Estado): Juego? = e.juegos.asSequence().mapNotNull { j ->
    val d = e.datos[j.clave] ?: return@mapNotNull null
    if (d.oculto || d.sesiones < 2 || d.avance == AvanceJuego.TERMINADO.name) return@mapNotNull null
    val media = d.tiempo / d.sesiones
    if (media > 25 * 60) null else j to d.ultimo
}.maxByOrNull { it.second }?.first

@Composable
private fun DippoPropone(e: Estado, al: Int) {
    val esc = LocalEscala.current
    val hora = remember { LocalTime.now().hour }
    val noche = hora >= 22 || hora < 6
    val (juego, texto) = remember(e.datos, e.juegos.size, noche) {
        val ultimo = CentroBisa.ultimo(e)?.takeIf { e.datos[it.clave]?.avance != AvanceJuego.TERMINADO.name }
        val corto = if (noche) cortoDe(e) else null
        when {
            corto != null -> {
                val d = e.datos[corto.clave]
                val min = ((d?.tiempo ?: 0L) / (d?.sesiones?.coerceAtLeast(1) ?: 1) / 60).coerceAtLeast(5)
                corto to "Ya es tarde: alcanza para una partida corta de ${corto.titulo}. Sueles jugarlo unos $min minutos."
            }
            ultimo != null -> {
                val paso = e.datos[ultimo.clave]?.paso?.takeIf { it.isNotBlank() }
                ultimo to (if (paso != null) "¿Seguimos con ${ultimo.titulo}? Tu paso: «$paso»" else "¿Seguimos con ${ultimo.titulo}? Te espero donde lo dejaste.")
            }
            else -> CentroBisa.olvidado(e, null)?.let { it to "¿Y si vuelves a ${it.titulo}? Hace rato que no lo juegas." }
                ?: (null to "Elige un juego y te ayudo a seguirlo paso a paso.")
        }
    }
    Tarjeta(alPulsar = { juego?.let { Diptico.jugarJuegoMostrado(it.clave) } ?: Diptico.abrirWidgetTipo("asistente") }) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(if (al >= 2) 46 else 30).value)
            Spacer(Modifier.width(esc.d(10)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
                Etiqueta(if (noche) "DIPPO · de noche" else "DIPPO te propone", Coral, 10.5f)
                Texto(texto, if (al >= 2) 13.5f else 12f, lineas = if (al >= 2) 4 else 2, traducir = false)
                if (al >= 2 && juego != null) Chip("A · Jugar") { Diptico.jugarJuegoMostrado(juego.clave) }
            }
        }
    }
}

@Composable
private fun TeExtrana(e: Estado, an: Int, al: Int) {
    val esc = LocalEscala.current
    val j = remember(e.datos, e.juegos.size) { CentroBisa.olvidado(e, CentroBisa.ultimo(e)?.clave) }
    if (j == null) {
        Tarjeta {
            Column(verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
                Etiqueta("Te extraña", Coral, 10.5f)
                Detalle("Cuando dejes un juego que te gustaba por más de dos semanas, aparecerá aquí.", 12f, lineas = 3)
            }
        }
        return
    }
    val d = e.datos[j.clave]
    val dias = ((System.currentTimeMillis() - (d?.ultimo ?: 0L)) / 86_400_000L).coerceAtLeast(1)
    val forma = RoundedCornerShape(esc.d((LocalWidget.current?.radio ?: 22).toFloat()))
    Box(Modifier.fillMaxSize().clip(forma).clickable { Diptico.jugarJuegoMostrado(j.clave) }) {
        ArteJuego(j, preferirArte = true, maxLado = 600, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.2f to Color.Black.copy(alpha = 0.15f), 1f to Color.Black.copy(alpha = 0.88f))))
        Column(Modifier.align(Alignment.BottomStart).padding(esc.d(12)), verticalArrangement = Arrangement.spacedBy(esc.d(3))) {
            Etiqueta("Te extraña · hace $dias días", Sol, 10.5f)
            Texto(j.titulo, if (an >= 3) 18f else 15f, color = Color.White, lineas = 1, sombra = true, traducir = false)
            val nota = d?.paso?.takeIf { it.isNotBlank() }?.let { "Tu paso: «$it»" } ?: "Lo dejaste con ${formatoTiempo(d?.tiempo ?: 0L)} jugadas."
            if (al >= 2) Texto(nota, 12f, color = Color.White.copy(alpha = 0.85f), lineas = 2, peso = androidx.compose.ui.text.font.FontWeight.Medium, traducir = false)
        }
    }
}

@Composable
private fun TuSemana(e: Estado, al: Int) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val dias = remember(e.datos) { TuAno.ultimosDias(7) }
    val total = dias.sumOf { it.second }
    val maximo = dias.maxOf { it.second }.coerceAtLeast(1L)
    val mejor = dias.maxByOrNull { it.second }?.takeIf { it.second > 0 }
    val racha = remember(e.datos) { Diptico.rachaDias() }
    Tarjeta(alPulsar = { Diptico.abrirTuAno() }) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Etiqueta("Tu semana", Coral, 10.5f)
                Spacer(Modifier.weight(1f))
                if (e.ver("tuSemana", "total")) Texto(formatoTiempo(total), 13f, lineas = 1, traducir = false)
            }
            if (al >= 2 && e.ver("tuSemana", "barras")) Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(6)), verticalAlignment = Alignment.Bottom) {
                for ((dia, seg) in dias) {
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        val hoy = dia == LocalDate.now()
                        Box(Modifier.fillMaxWidth(0.7f).weight(1f, fill = false)
                            .fillMaxHeight((seg.toFloat() / maximo).coerceIn(0.06f, 1f))
                            .clip(RoundedCornerShape(esc.d(5)))
                            .background(if (seg > 0) (if (hoy) Sol else Uva) else p.tinta.copy(alpha = 0.12f)))
                        Spacer(Modifier.height(esc.d(3)))
                        Texto(dia.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.forLanguageTag(Idioma.actual)), 10f, color = if (hoy) p.tinta else p.tenue, lineas = 1, traducir = false)
                    }
                }
            }
            if (e.ver("tuSemana", "frase")) Detalle(when {
                mejor == null -> "Esta semana todavía no juegas."
                racha > 1 -> "Llevas $racha días seguidos. Tu mejor día: ${mejor.first.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag(Idioma.actual))}."
                else -> "Tu mejor día: ${mejor.first.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag(Idioma.actual))}."
            }, 11f, lineas = 1)
        }
    }
}

@Composable
private fun TuEquipo(al: Int) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val ctx = LocalContext.current
    var lectura by remember { mutableStateOf(Termica.pruebaLectura) }
    var libres by remember { mutableStateOf(-1L) }
    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) {
                lectura = runCatching { Termica.leer(ctx) }.getOrNull()
                libres = runCatching { android.os.StatFs(android.os.Environment.getExternalStorageDirectory().path).availableBytes }.getOrDefault(-1L)
            }
            delay(30_000)
        }
    }
    val l = lectura
    Tarjeta {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(if (al >= 2) 6 else 2))) {
            Etiqueta("Tu equipo", Coral, 10.5f)
            if (l == null) { Detalle("Leyendo…", 12f); return@Column }
            val (calor, color) = when (l.nivel) {
                Termica.Nivel.FRESCA -> "Fresca" to Color(0xFF3FD68A)
                Termica.Nivel.TIBIA -> "Tibia" to Color(0xFFF5C542)
                Termica.Nivel.CALIENTE -> "Caliente" to Color(0xFFFF8A3D)
                Termica.Nivel.MUY_CALIENTE -> "Muy caliente" to Color(0xFFFF4D4D)
            }
            FilaEquipo("Batería", if (l.nivelBateria >= 0) "${l.nivelBateria} %" + if (l.cargando) " ⚡" else "" else "—", if (l.nivelBateria in 0..15) Color(0xFFFF4D4D) else p.tinta)
            FilaEquipo("Temperatura", if (l.grados > 0f) "${Termica.texto(l.grados)} · $calor" else calor, color)
            if (al >= 2 && libres >= 0) FilaEquipo("Espacio libre", "${libres / 1_000_000_000L} GB", p.tinta)
        }
    }
}

@Composable
private fun FilaEquipo(titulo: String, valor: String, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Detalle(titulo, 11.5f, lineas = 1, modifier = Modifier.weight(1f))
        Texto(valor, 12.5f, color = color, lineas = 1, traducir = false)
    }
}

@Composable
private fun UnRecuerdo(e: Estado, an: Int, al: Int) {
    val esc = LocalEscala.current
    val r = remember(e.datos) { TuAno.recuerdo() }
    val j = r?.first?.clave?.let { e.juegosPorClave[it] }
    if (r == null || j == null) {
        Tarjeta {
            Column(verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
                Etiqueta("Un recuerdo", Coral, 10.5f)
                Detalle("Cuando lleves una semana jugando, aquí te recordaré qué jugabas antes.", 12f, lineas = 3)
            }
        }
        return
    }
    val cuando = when (r.second) {
        in 360..370 -> "Hace un año"
        in 28..31 -> "Hace un mes"
        7L -> "Hace una semana"
        else -> "Hace ${r.second} días"
    }
    val forma = RoundedCornerShape(esc.d((LocalWidget.current?.radio ?: 22).toFloat()))
    Box(Modifier.fillMaxSize().clip(forma).clickable { Diptico.jugarJuegoMostrado(j.clave) }) {
        ArteJuego(j, preferirArte = true, maxLado = 600, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))
        Column(Modifier.align(Alignment.BottomStart).padding(esc.d(12))) {
            Etiqueta(cuando, Sol, 10.5f)
            Texto("Jugabas ${j.titulo}", if (an >= 3) 16f else 13.5f, color = Color.White, lineas = 2, sombra = true, traducir = false)
            if (al >= 2) Detalle("${formatoTiempo(r.first.segundos)} ese día", 11.5f, color = Color.White.copy(alpha = 0.75f), lineas = 1)
        }
    }
}

@Composable
private fun RadioTema(e: Estado, al: Int) {
    val esc = LocalEscala.current
    var titulo by remember { mutableStateOf(Musica.titulo) }
    LaunchedEffect(Unit) { while (true) { titulo = Musica.titulo; delay(2000) } }
    val callada = e.extra.musicaCalla > System.currentTimeMillis()
    Tarjeta {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
            Column(Modifier.weight(1f)) {
                Etiqueta(if (callada) "En silencio" else if (e.musica) "Sonando" else "Música apagada", Coral, 10.5f)
                Texto(titulo.ifBlank { "Música de Díptico" }, 13f, lineas = 1, traducir = false)
            }
            Chip(if (e.musica && !callada) "⏸" else "▶") {
                if (callada) Diptico.actualizar { it.copy(extra = it.extra.copy(musicaCalla = 0L)) } else Diptico.fijarMusica(!e.musica)
            }
            if (Musica.esViva) Chip("⏭") { Musica.saltarParte() }
            if (!callada) Chip("1 h") { Diptico.callarMusicaUnaHora() }
        }
    }
}

@Composable
private fun Chip(texto: String, accion: () -> Unit) {
    val esc = LocalEscala.current
    Box(Modifier.clip(RoundedCornerShape(50)).background(Uva.copy(alpha = 0.9f)).clickable(onClick = accion)
        .padding(horizontal = esc.d(12), vertical = esc.d(6))) {
        Texto(texto, 12.5f, color = SobreUva, lineas = 1)
    }
}

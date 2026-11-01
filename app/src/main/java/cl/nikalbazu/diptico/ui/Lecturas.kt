package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Opcion
import cl.nikalbazu.diptico.estado.Widget

/**
 * Cada widget abierto, en grande, en la pantalla de arriba: un diseño propio para leerlo de lejos
 * (como el reloj), con números grandes, barras y lo más importante primero.
 * Devuelve false si el tipo no tiene diseño propio (entonces se muestra el widget agrandado).
 */
@Composable
internal fun LecturaGrande(e: Estado, w: Widget, opcion: Opcion?): Boolean {
    when {
        w.tipo == "tiempo" || w.tipo == "estadisticas" -> LecturaTiempo(e, w.tipo == "estadisticas")
        w.tipo == "consolas" -> LecturaColeccion(e)
        w.tipo == "bateria" -> LecturaBateria(e)
        w.tipo == "notificaciones" -> LecturaAvisos(opcion)
        w.tipo == "musica" -> LecturaMusica()
        w.tipo == "frase" -> LecturaFrase(e)
        w.tipo == "fiesta" -> LecturaFiesta(e)
        w.tipo == "logros" -> LecturaLogros(e)
        w.tipo.startsWith("imagen") -> Box(Modifier.fillMaxSize().padding(LocalEscala.current.d(10)), contentAlignment = Alignment.Center) { WidgetImagen(w.copy(estilo = "sinfondo")) }
        w.tipo.startsWith("video") -> Box(Modifier.fillMaxSize().clip(RoundedCornerShape(LocalEscala.current.d(24)))) { WidgetVideo(w, e.sesion == null) }
        else -> return false
    }
    return true
}

/** Sube de 0 al valor final cuando se abre (los números y barras "se llenan"). */
@Composable
private fun rememberEntrada(clave: Any?): Float {
    val a = remember(clave) { Animatable(0f) }
    LaunchedEffect(clave) { a.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    return a.value
}

@Composable
private fun CifraGrande(valor: String, unidad: String, color: Color = LocalPaleta.current.tinta, tam: Float = 84f) {
    val p = LocalPaleta.current
    Row(verticalAlignment = Alignment.Bottom) {
        Texto(valor, tam, color = color, peso = FontWeight.Bold, lineas = 1, traducir = false, interlineado = tam * 1.02f)
        if (unidad.isNotEmpty()) {
            Spacer(Modifier.width(LocalEscala.current.d(6)))
            Texto(unidad, tam * 0.34f, color = p.tenue, lineas = 1, modifier = Modifier.padding(bottom = LocalEscala.current.d(tam * 0.14f)))
        }
    }
}

@Composable
private fun BarraLectura(icono: @Composable () -> Unit, nombre: String, valor: String, fraccion: Float, color: Color) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = esc.d(3))) {
        Box(Modifier.size(esc.d(26)), contentAlignment = Alignment.Center) { icono() }
        Spacer(Modifier.width(esc.d(8)))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Texto(nombre, 13f, lineas = 1, modifier = Modifier.weight(1f), traducir = false)
                Detalle(valor, 12f, lineas = 1)
            }
            Spacer(Modifier.height(esc.d(3)))
            Box(Modifier.fillMaxWidth().height(esc.d(7)).clip(RoundedCornerShape(50)).background(p.linea)) {
                Box(Modifier.fillMaxWidth(fraccion.coerceIn(0.02f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(Brush.horizontalGradient(listOf(color, lerp(color, Color.White, 0.25f)))))
            }
        }
    }
}

/** Tiempo jugado / Estadísticas: las horas en grande, cuántos juegos, y las consolas en barras. */
@Composable
private fun LecturaTiempo(e: Estado, estadisticas: Boolean) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val jugados = e.datos.filterValues { it.tiempo > 0 }
    val total = jugados.values.sumOf { it.tiempo }
    val sesiones = e.datos.values.sumOf { it.sesiones }
    val porConsola = remember(e.datos) {
        jugados.entries.groupBy { it.key.substringBefore('/') }.mapValues { (_, v) -> v.sumOf { it.value.tiempo } }.entries.sortedByDescending { it.value }
    }
    val top = jugados.maxByOrNull { it.value.tiempo }?.let { (k, d) -> e.juegos.firstOrNull { it.clave == k }?.let { it to d.tiempo } }
    val a = rememberEntrada(total)
    Column(Modifier.fillMaxSize().padding(horizontal = esc.d(26), vertical = esc.d(20)), verticalArrangement = Arrangement.Center) {
        Etiqueta(if (estadisticas) "Tus estadísticas" else "Tiempo jugado", Coral, 12f)
        Row(verticalAlignment = Alignment.Bottom) {
            CifraGrande("${((total / 3600) * a).toInt()}", "h")
            Spacer(Modifier.width(esc.d(10)))
            CifraGrande("${(((total % 3600) / 60) * a).toInt()}", "min", p.tenue, 40f)
        }
        Detalle("en ${jugados.size} juegos · $sesiones partidas", 14f, lineas = 1)
        Spacer(Modifier.height(esc.d(14)))
        if (porConsola.isEmpty()) {
            Detalle("Cuando juegues con Díptico, aquí verás tus horas por consola.", 14f)
        } else {
            val max = porConsola.first().value.coerceAtLeast(1L)
            porConsola.take(if (estadisticas) 5 else 4).forEach { (id, t) ->
                val c = Consolas.porId(id)
                BarraLectura({ IconoConsola(c, 22f, c?.color ?: Uva) }, c?.nombre ?: id, formatoTiempo(t), t.toFloat() / max * a, c?.color ?: Uva)
            }
        }
        if (top != null) {
            Spacer(Modifier.height(esc.d(12)))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(16))).background(p.superficie.copy(alpha = 0.7f)).padding(esc.d(8)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(esc.d(40)).clip(RoundedCornerShape(esc.d(8)))) { ArteJuego(top.first, preferirArte = false, maxLado = 160, modifier = Modifier.fillMaxSize()) }
                Spacer(Modifier.width(esc.d(10)))
                Column(Modifier.weight(1f)) {
                    Etiqueta("El que más juegas", Sol, 10.5f)
                    Texto(top.first.titulo, 15f, lineas = 1, traducir = false)
                }
                Texto(formatoTiempo(top.second), 14f, color = Coral, lineas = 1, traducir = false)
            }
        }
    }
}

/** Tu colección: cuántos juegos en grande y cada consola con su ícono y su número. */
@Composable
private fun LecturaColeccion(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val consolas = e.consolasConJuegos
    val a = rememberEntrada(e.juegos.size)
    Column(Modifier.fillMaxSize().padding(horizontal = esc.d(26), vertical = esc.d(20)), verticalArrangement = Arrangement.Center) {
        Etiqueta("Tu colección", Coral, 12f)
        CifraGrande("${(e.juegos.size * a).toInt()}", "juegos")
        Detalle("en ${consolas.size} consolas · ${e.datos.values.count { it.favorito }} favoritos", 14f, lineas = 1)
        Spacer(Modifier.height(esc.d(16)))
        val max = consolas.maxOfOrNull { c -> e.juegos.count { it.consolaId == c.id } }?.coerceAtLeast(1) ?: 1
        consolas.sortedByDescending { c -> e.juegos.count { it.consolaId == c.id } }.take(5).forEach { c ->
            val n = e.juegos.count { it.consolaId == c.id }
            BarraLectura({ IconoConsola(c, 22f, c.color) }, c.bonito, "$n", n.toFloat() / max * a, c.color)
        }
        if (consolas.size > 5) Detalle("y ${consolas.size - 5} consolas más", 12f, lineas = 1, modifier = Modifier.padding(top = esc.d(4)))
    }
}

/** Batería: un anillo grande que se llena, con el estado y la temperatura abajo. */
@Composable
private fun LecturaBateria(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val b = rememberBateria()
    val info = remember(b) { Diptico.infoBateria() }
    val baja = b in 0..19
    val color = if (baja) Coral else if (info.estado == "Cargando") Color(0xFF2EC4A6) else Uva
    val a = rememberEntrada(b)
    Row(Modifier.fillMaxSize().padding(esc.d(26)), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.fillMaxHeight(0.82f).aspectRatio(1f), contentAlignment = Alignment.Center) {
            val linea = p.linea
            Canvas(Modifier.fillMaxSize()) {
                val g = size.minDimension * 0.09f
                val st = Stroke(g, cap = StrokeCap.Round)
                val o = Offset(g / 2, g / 2)
                val t = Size(size.width - g, size.height - g)
                drawArc(linea, 0f, 360f, false, o, t, style = st)
                drawArc(color, -90f, 360f * b.coerceIn(0, 100) / 100f * a, false, o, t, style = st)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconoDiptico(Icono.BATERIA, color, 26f, acento = color)
                Texto(if (b >= 0) "${(b * a).toInt()}%" else "—", 54f, lineas = 1, traducir = false)
            }
        }
        Spacer(Modifier.width(esc.d(22)))
        Column(Modifier.weight(1f)) {
            Etiqueta("Batería", Coral, 12f)
            Texto(info.estado, 22f, lineas = 2)
            Spacer(Modifier.height(esc.d(10)))
            if (info.temperatura > 0f) DatoLectura("Temperatura", "${"%.1f".format(info.temperatura)} °C")
            if (info.voltaje > 0) DatoLectura("Voltaje", "${"%.2f".format(info.voltaje / 1000f)} V")
            DatoLectura("Salud", info.salud)
            if (baja) { Spacer(Modifier.height(esc.d(8))); Detalle("Queda poca: conéctala pronto.", 13f, color = Coral) }
        }
    }
}

@Composable
private fun DatoLectura(t: String, v: String) {
    val esc = LocalEscala.current
    Row(Modifier.fillMaxWidth().padding(vertical = esc.d(3)), verticalAlignment = Alignment.CenterVertically) {
        Detalle(t, 13f, lineas = 1, modifier = Modifier.weight(1f))
        Texto(v, 15f, lineas = 1, traducir = false)
    }
}

/** Notificaciones: cuántas hay y las últimas como tarjetas (la elegida en la lista, marcada). */
@Composable
private fun LecturaAvisos(opcion: Opcion?) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val avisos by cl.nikalbazu.diptico.audio.Avisos.lista.collectAsState()
    Column(Modifier.fillMaxSize().padding(horizontal = esc.d(24), vertical = esc.d(20)), verticalArrangement = Arrangement.Center) {
        Etiqueta("Notificaciones", Coral, 12f)
        CifraGrande("${avisos.size}", if (avisos.size == 1) "nueva" else "nuevas", tam = 64f)
        Spacer(Modifier.height(esc.d(10)))
        if (avisos.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.CHECK, Color(0xFF2EC4A6), 22f)
                Spacer(Modifier.width(esc.d(8)))
                Detalle("Todo tranquilo por aquí.", 15f)
            }
        }
        avisos.take(4).forEach { a ->
            val sel = opcion?.titulo == a.titulo.ifBlank { a.app }
            Row(
                Modifier.fillMaxWidth().padding(vertical = esc.d(3)).clip(RoundedCornerShape(esc.d(14)))
                    .background(if (sel) Uva.copy(alpha = 0.22f) else p.superficie.copy(alpha = 0.7f))
                    .then(if (sel) Modifier.border(esc.d(1.5f), Uva, RoundedCornerShape(esc.d(14))) else Modifier)
                    .padding(horizontal = esc.d(10), vertical = esc.d(7)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(esc.d(28)).clip(CircleShape).background(Uva.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    IconoDiptico(Icono.CAMPANA, p.tinta, 14f, acento = Coral)
                }
                Spacer(Modifier.width(esc.d(8)))
                Column(Modifier.weight(1f)) {
                    Texto(a.titulo.ifBlank { a.app }, 13.5f, lineas = 1, traducir = false)
                    Detalle(a.texto.ifBlank { a.app }, 12f, lineas = 1)
                }
            }
        }
    }
}

/** Música: la portada grande, la canción y el artista, y si está sonando. */
@Composable
private fun LecturaMusica() {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val m by cl.nikalbazu.diptico.audio.Medios.sonando.collectAsState()
    Row(Modifier.fillMaxSize().padding(esc.d(26)), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.fillMaxHeight(0.78f).aspectRatio(1f).clip(RoundedCornerShape(esc.d(22)))
                .background(Brush.linearGradient(listOf(Uva, Coral))),
            contentAlignment = Alignment.Center,
        ) {
            val img = m?.portada
            if (img != null) Image(img.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else IconoDiptico(Icono.MUSICA, Color.White, 64f)
        }
        Spacer(Modifier.width(esc.d(22)))
        Column(Modifier.weight(1f)) {
            Etiqueta(m?.app ?: "Música", Coral, 12f)
            Texto(m?.titulo ?: "No suena nada", 26f, lineas = 3, traducir = m == null)
            m?.artista?.takeIf { it.isNotBlank() }?.let { Detalle(it, 16f, lineas = 1) }
            Spacer(Modifier.height(esc.d(14)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(esc.d(10)).clip(CircleShape).background(if (m?.reproduciendo == true) Color(0xFF2EC4A6) else p.tenue))
                Spacer(Modifier.width(esc.d(8)))
                Detalle(if (m?.reproduciendo == true) "Sonando" else if (m != null) "En pausa" else "Abre Spotify o YouTube Music desde la lista", 13.5f, lineas = 2)
            }
        }
    }
}

/** Saludo del día: la frase en grande, entre comillas. */
@Composable
private fun LecturaFrase(e: Estado) {
    val esc = LocalEscala.current
    Column(Modifier.fillMaxSize().padding(esc.d(30)), verticalArrangement = Arrangement.Center) {
        Texto("“", 90f, color = Coral, lineas = 1, traducir = false, interlineado = 60f)
        Texto(fraseDelDia(e), 30f, lineas = 5, interlineado = 38f)
        Spacer(Modifier.height(esc.d(14)))
        Detalle(cl.nikalbazu.diptico.estado.Idioma.fecha("EEEEdMMMM").replaceFirstChar { it.uppercase() }, 15f, lineas = 1)
    }
}

/** Fiestas del mundo: la de hoy (o la próxima) en grande y las que vienen. */
@Composable
private fun LecturaFiesta(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val proximas = remember { cl.nikalbazu.diptico.estado.Temas.proximas(4) }
    val hoy = e.fiesta
    val principal = hoy ?: proximas.firstOrNull()?.first
    Column(Modifier.fillMaxSize().padding(horizontal = esc.d(26), vertical = esc.d(20)), verticalArrangement = Arrangement.Center) {
        Etiqueta(if (hoy != null) "Hoy se celebra" else "Se viene", Coral, 12f)
        Texto(principal?.nombre ?: "Sin fiestas cerca", 34f, lineas = 2, interlineado = 38f)
        principal?.let { Detalle(it.donde, 15f, lineas = 1) }
        hoy?.tema?.mensaje?.takeIf { it.isNotBlank() }?.let { Spacer(Modifier.height(esc.d(6))); Detalle(it, 14f, lineas = 2) }
        Spacer(Modifier.height(esc.d(16)))
        proximas.drop(if (hoy == null) 1 else 0).take(3).forEach { (f, d) ->
            Row(Modifier.fillMaxWidth().padding(vertical = esc.d(4)), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.size(esc.d(44)).clip(RoundedCornerShape(esc.d(12))).background(p.superficie),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Texto("${d.dayOfMonth}", 17f, lineas = 1, traducir = false, interlineado = 18f)
                    Detalle("${d.monthValue}", 10f, lineas = 1)
                }
                Spacer(Modifier.width(esc.d(10)))
                Column(Modifier.weight(1f)) {
                    Texto(f.nombre, 15f, lineas = 1)
                    Detalle(f.donde, 12f, lineas = 1)
                }
            }
        }
    }
}

/** Logros: tus puntos de RetroAchievements en grande y los últimos que ganaste. */
@Composable
private fun LecturaLogros(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val perfil = e.perfilRA
    Column(Modifier.fillMaxSize().padding(horizontal = esc.d(26), vertical = esc.d(20)), verticalArrangement = Arrangement.Center) {
        Etiqueta("RetroAchievements", Coral, 12f)
        if (perfil == null) {
            Texto("Conecta tu cuenta", 30f, lineas = 2)
            Detalle("En Menú › Logros pones tu usuario y tu clave, y aquí verás tus puntos.", 14f, lineas = 3)
            return@Column
        }
        Texto(perfil.usuario, 20f, lineas = 1, traducir = false)
        CifraGrande("${(perfil.puntos * rememberEntrada(perfil.puntos)).toInt()}", "puntos", Sol)
        if (perfil.desde.isNotBlank()) Detalle("Jugando desde ${perfil.desde.take(10)}", 13f, lineas = 1)
        Spacer(Modifier.height(esc.d(12)))
        perfil.recientes.take(3).forEach { (juego, l) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = esc.d(3)).clip(RoundedCornerShape(esc.d(14))).background(p.superficie.copy(alpha = 0.7f)).padding(esc.d(8)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconoDiptico(Icono.TROFEO, Sol, 18f)
                Spacer(Modifier.width(esc.d(8)))
                Column(Modifier.weight(1f)) {
                    Texto(l.titulo, 13.5f, lineas = 1, traducir = false)
                    Detalle(juego, 11.5f, lineas = 1)
                }
                Texto("+${l.puntos}", 13f, color = Sol, lineas = 1, traducir = false)
            }
        }
    }
}

package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.audio.Medios
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Temas
import cl.nikalbazu.diptico.estado.Widget
import cl.nikalbazu.diptico.estado.Widgets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Inicio personalizado: una grilla de widgets en la pantalla principal.
 * Cada widget mide 1, 2 o 3 columnas; se acomodan en filas solas.
 */
@Composable
fun PantallaInicio(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(Modifier.fillMaxSize().background(p.fondo)) {
        // Fondo: el arte del juego elegido, muy suave.
        e.seleccionado?.let { j ->
            ArteJuego(j, preferirArte = true, maxLado = 1200, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.22f })
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.fondo.copy(alpha = 0.6f), p.fondo))))
        BarraSuperior(Modifier.align(Alignment.TopStart), trabajos = e.trabajos)

        val columnas = if (esc.angosta) 2 else 3
        val filas = remember(e.widgets, columnas) { acomodar(e.widgets.filter { it.tam > 0 }, columnas) }
        val scroll = rememberScrollState()
        Column(
            Modifier.fillMaxSize().padding(start = esc.d(20), end = esc.d(20), top = esc.d(52), bottom = esc.d(14)).verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(esc.d(12)),
        ) {
            if (filas.isEmpty()) {
                Detalle("No hay widgets a la vista. Abre el menú › Inicio › Editar widgets.", 16f)
            }
            for (fila in filas) {
                Row(Modifier.fillMaxWidth().height(esc.d(150)), horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                    for (w in fila) {
                        Box(Modifier.weight(minOf(w.tam, columnas).toFloat()).fillMaxHeight()) { WidgetVista(e, w.tipo, minOf(w.tam, columnas)) }
                    }
                    val usado = fila.sumOf { minOf(it.tam, columnas) }
                    if (usado < columnas) Spacer(Modifier.weight((columnas - usado).toFloat()))
                }
            }
        }
    }
}

/** Reparte los widgets en filas de [columnas] columnas. */
private fun acomodar(ws: List<Widget>, columnas: Int): List<List<Widget>> {
    val filas = mutableListOf<MutableList<Widget>>()
    var actual = mutableListOf<Widget>()
    var usado = 0
    for (w in ws) {
        val t = minOf(w.tam, columnas)
        if (usado + t > columnas) {
            filas += actual
            actual = mutableListOf()
            usado = 0
        }
        actual += w
        usado += t
    }
    if (actual.isNotEmpty()) filas += actual
    return filas
}

@Composable
private fun Tarjeta(modifier: Modifier = Modifier, alPulsar: (() -> Unit)? = null, contenido: @Composable () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(
        modifier.fillMaxSize().clip(RoundedCornerShape(esc.d(22))).background(p.superficie.copy(alpha = 0.82f))
            .border(esc.d(1), p.linea, RoundedCornerShape(esc.d(22)))
            .then(if (alPulsar != null) Modifier.clickable { alPulsar() } else Modifier)
            .padding(esc.d(14)),
    ) { contenido() }
}

@Composable
private fun WidgetVista(e: Estado, tipo: String, tam: Int) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    when (tipo) {
        "juego" -> {
            val j = e.seleccionado
            Tarjeta(alPulsar = { Diptico.jugar() }) {
                if (j == null) {
                    Detalle("Elige un juego en el estante", 15f)
                } else {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        val c = Consolas.porId(j.consolaId)
                        if (c != null) CajaJuego(j, c, escala = 0.95f)
                        Spacer(Modifier.width(esc.d(14)))
                        Column(Modifier.weight(1f)) {
                            Etiqueta(c?.nombre ?: "", Coral, 11f)
                            Texto(j.titulo, if (tam >= 2) 22f else 17f, lineas = 2, interlineado = if (tam >= 2) 25f else 20f)
                            Spacer(Modifier.height(esc.d(8)))
                            Box(Modifier.clip(RoundedCornerShape(50)).background(Uva).padding(horizontal = esc.d(14), vertical = esc.d(6))) {
                                Texto("A · ¡Jugar!", 14f, color = SobreUva)
                            }
                        }
                    }
                }
            }
        }
        "reloj" -> Tarjeta(alPulsar = { Diptico.alternarReloj() }) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Texto(rememberHora(), if (tam >= 2) 58f else 44f, interlineado = if (tam >= 2) 60f else 46f)
                Detalle(SimpleDateFormat("EEEE d 'de' MMMM", Locale("es")).format(Date()).replaceFirstChar { it.uppercase() }, 14f, lineas = 1)
            }
        }
        "bateria" -> Tarjeta {
            val b = rememberBateria()
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Texto(if (b >= 0) "$b%" else "—", 40f)
                Spacer(Modifier.height(esc.d(8)))
                Box(Modifier.fillMaxWidth().height(esc.d(10)).clip(RoundedCornerShape(50)).background(p.linea)) {
                    Box(Modifier.fillMaxWidth((b.coerceIn(0, 100)) / 100f).fillMaxHeight().clip(RoundedCornerShape(50)).background(if (b in 0..19) Coral else Uva))
                }
                Spacer(Modifier.height(esc.d(6)))
                Detalle("Batería", 13f)
            }
        }
        "musica" -> WidgetMusica(tam)
        "paso" -> Tarjeta(alPulsar = { Diptico.abrirPaso() }) {
            val paso = e.seleccionado?.let { e.datos[it.clave]?.paso }.orEmpty()
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconoDiptico(Icono.LAPIZ, Coral, 18f)
                    Spacer(Modifier.width(esc.d(8)))
                    Texto("Tu próximo paso", 14f, color = p.acentoTexto)
                }
                Spacer(Modifier.height(esc.d(8)))
                Texto(paso.ifEmpty { "Aprieta Y para anotar dónde quedaste" }, 16f, lineas = 4, color = if (paso.isEmpty()) p.tenue else p.tinta, peso = FontWeight.SemiBold)
            }
        }
        "tiempo" -> Tarjeta {
            val total = e.datos.values.sumOf { it.tiempo }
            val top = e.datos.maxByOrNull { it.value.tiempo }?.takeIf { it.value.tiempo > 0 }
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Etiqueta("Tiempo jugado", p.tenue, 11f)
                Texto(if (total > 0) formatoTiempo(total) else "0 min", 28f, lineas = 1)
                top?.let { (clave, d) ->
                    val t = e.juegos.firstOrNull { it.clave == clave }?.titulo ?: return@let
                    Detalle("Más jugado: $t (${formatoTiempo(d.tiempo)})", 13f, lineas = 2)
                }
            }
        }
        "recientes" -> Tarjeta {
            val recientes = e.datos.entries.filter { it.value.ultimo > 0 }.sortedByDescending { it.value.ultimo }
                .mapNotNull { (k, _) -> e.juegos.firstOrNull { it.clave == k } }.take(if (tam >= 3) 5 else if (tam == 2) 3 else 2)
            Column {
                Etiqueta("Jugados hace poco", p.tenue, 11f)
                Spacer(Modifier.height(esc.d(8)))
                if (recientes.isEmpty()) Detalle("Todavía no juegas nada con Díptico", 14f)
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                    for (j in recientes) {
                        val c = Consolas.porId(j.consolaId) ?: continue
                        Box(Modifier.clickable { Diptico.elegirConsola("todo"); Diptico.elegirJuego(j.clave) }) {
                            CajaJuego(j, c, escala = 0.72f)
                        }
                    }
                }
            }
        }
        "fiesta" -> Tarjeta {
            val hoy = e.fiesta
            val prox = Temas.proxima()
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                IconoDiptico(Icono.MUNDO, Sol, 26f, acento = Coral)
                Spacer(Modifier.height(esc.d(6)))
                if (hoy != null) {
                    Texto(hoy.nombre, 18f, lineas = 2)
                    Detalle(hoy.tema.mensaje.ifBlank { hoy.donde }, 13f, lineas = 2)
                } else if (prox != null) {
                    Etiqueta("Próxima fiesta", p.tenue, 11f)
                    Texto(prox.first.nombre, 18f, lineas = 2)
                    Detalle("${prox.second.dayOfMonth}/${prox.second.monthValue} · ${prox.first.donde}", 13f, lineas = 2)
                }
            }
        }
        "consolas" -> Tarjeta {
            Column {
                Etiqueta("Tu colección", p.tenue, 11f)
                Texto("${e.juegos.size} juegos", 26f)
                Detalle("en ${e.consolasConJuegos.size} consolas", 13f)
                Spacer(Modifier.height(esc.d(6)))
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    e.consolasConJuegos.take(if (tam >= 2) 8 else 4).forEach { IconoConsola(it, 24f, p.superficie) }
                }
            }
        }
    }
}

/** Lo que suena en Spotify, YouTube Music u otra app, con botones para controlarlo. */
@Composable
private fun WidgetMusica(tam: Int) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val ctx = LocalContext.current
    val permiso by Medios.permiso.collectAsState()
    val s by Medios.sonando.collectAsState()
    if (!permiso) {
        Tarjeta(alPulsar = { Medios.pedirPermiso(ctx) }) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                IconoDiptico(Icono.MUSICA, Uva, 26f, acento = Coral)
                Spacer(Modifier.height(esc.d(6)))
                Texto("Widget de música", 17f)
                Detalle("Toca para dar permiso y ver lo que suena en Spotify o YouTube Music.", 13f, lineas = 3)
            }
        }
        return
    }
    Tarjeta {
        val musica = s
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(esc.d(if (tam >= 2) 96f else 56f)).clip(RoundedCornerShape(esc.d(14))).background(Uva.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                val img = musica?.portada
                if (img != null) Image(img.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else IconoDiptico(Icono.MUSICA, p.tinta, 28f, acento = Coral)
            }
            Spacer(Modifier.width(esc.d(12)))
            Column(Modifier.weight(1f)) {
                Etiqueta(musica?.app ?: "Música", Coral, 11f)
                Texto(musica?.titulo ?: "Nada sonando", 16f, lineas = 2)
                if (!musica?.artista.isNullOrBlank()) Detalle(musica!!.artista, 13f, lineas = 1)
                Spacer(Modifier.height(esc.d(6)))
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8)), verticalAlignment = Alignment.CenterVertically) {
                    BotonCirculo(Icono.CONTINUAR, invertido = true) { Medios.anterior() }
                    BotonCirculo(if (musica?.reproduciendo == true) Icono.MENU else Icono.JUGAR, grande = true, pausa = musica?.reproduciendo == true) { Medios.alternar() }
                    BotonCirculo(Icono.CONTINUAR) { Medios.siguiente() }
                }
            }
        }
    }
}

@Composable
private fun BotonCirculo(icono: Icono, grande: Boolean = false, invertido: Boolean = false, pausa: Boolean = false, accion: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val tam = if (grande) 38f else 30f
    Box(
        Modifier.size(esc.d(tam)).clip(CircleShape).background(if (grande) Uva else p.fondo).clickable { accion() },
        contentAlignment = Alignment.Center,
    ) {
        if (pausa) {
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(4))) {
                Box(Modifier.size(esc.d(4), esc.d(14)).clip(RoundedCornerShape(50)).background(SobreUva))
                Box(Modifier.size(esc.d(4), esc.d(14)).clip(RoundedCornerShape(50)).background(SobreUva))
            }
        } else {
            IconoDiptico(
                icono, if (grande) SobreUva else p.tinta, tam * 0.5f,
                modifier = Modifier.graphicsLayer { if (invertido) scaleX = -1f },
            )
        }
    }
}

/** Editor de widgets: A cambia el tamaño, L1/R1 los mueve. */
@Composable
fun PanelWidgets(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val lista = rememberLazyListState()
    LaunchedEffect(e.indice) { lista.animateScrollToItem((e.indice - 2).coerceAtLeast(0)) }
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Column(Modifier.fillMaxSize().padding(start = esc.d(16), end = esc.d(16), top = esc.d(12), bottom = esc.d(10))) {
            Row(Modifier.fillMaxWidth().height(esc.d(40)), verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.APPS, p.tinta, 24f, acento = Coral)
                Spacer(Modifier.width(esc.d(10)))
                Texto("Tus widgets", 21f, modifier = Modifier.weight(1f))
                Detalle("${e.widgets.count { it.tam > 0 }} a la vista", 14f)
            }
            Detalle("A cambia el tamaño · L1 R1 lo mueven arriba o abajo", 13.5f, lineas = 1)
            Spacer(Modifier.height(esc.d(8)))
            LazyColumn(state = lista, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                itemsIndexed(e.widgets, key = { _, w -> w.tipo }) { i, w ->
                    val activa = i == e.indice
                    val alza by animateFloatAsState(if (activa) 1.02f else 1f, spring(dampingRatio = 0.6f), label = "w")
                    Row(
                        Modifier.fillMaxWidth().height(esc.d(52)).graphicsLayer { scaleX = alza; scaleY = alza }
                            .clip(RoundedCornerShape(esc.d(16)))
                            .background(if (activa) Uva else p.superficie.copy(alpha = 0.75f))
                            .clickable { Diptico.elegirIndice(i) }
                            .padding(horizontal = esc.d(14)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Texto("${i + 1}", 14f, color = if (activa) SobreUva else p.tenue, modifier = Modifier.width(esc.d(24)))
                        Texto(Widgets.nombre(w.tipo), 15.5f, color = if (activa) SobreUva else p.tinta, lineas = 1, modifier = Modifier.weight(1f))
                        // Puntitos del tamaño.
                        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(3))) {
                            for (k in 1..3) {
                                Box(
                                    Modifier.size(esc.d(14), esc.d(10)).clip(RoundedCornerShape(esc.d(3)))
                                        .background(if (k <= w.tam) (if (activa) SobreUva else Coral) else (if (activa) SobreUva.copy(alpha = 0.3f) else p.linea)),
                                )
                            }
                        }
                        Spacer(Modifier.width(esc.d(8)))
                        Texto(Widgets.nombreTam(w.tam), 12.5f, color = if (activa) SobreUva else p.tenue, modifier = Modifier.width(esc.d(62)))
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("A" to "Tamaño", "L1 R1" to "Mover", "B" to "Listo")
        }
    }
}

package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import cl.nikalbazu.diptico.emu.App
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Teclas

/**
 * El ícono de una app, ajustado a Díptico según Apariencia › Íconos de apps:
 * "tema" (la forma del tema, como las demás tarjetas), "mono" (un solo color, el del tema) u "original" (tal cual).
 */
@Composable
fun IconoApp(paquete: String, tam: Float, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val estilo = Estilo.iconosApps
    val forma = RoundedCornerShape(esc.d(tam * 0.28f))
    val base = rememberImagen(android.net.Uri.parse("app://$paquete"), 144)
    when (estilo) {
        "original" -> Box(modifier.size(esc.d(tam)), contentAlignment = Alignment.Center) {
            if (base != null) Image(base, null, Modifier.fillMaxSize()) else IconoDiptico(Icono.APPS, p.tenue, tam * 0.5f)
        }
        "mono" -> {
            val mono = rememberImagen(android.net.Uri.parse("app://$paquete?capa=mono"), 144)
            Box(
                modifier.size(esc.d(tam)).clip(forma)
                    .background(Brush.linearGradient(listOf(androidx.compose.ui.graphics.lerp(p.superficie, Uva, 0.28f), p.superficie)))
                    .border(esc.d(1), Uva.copy(alpha = 0.35f), forma),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    mono != null -> Image(mono, null, Modifier.fillMaxSize(), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(p.tinta))
                    base != null -> Image(base, null, Modifier.fillMaxSize(0.7f), colorFilter = filtroDuotono(Uva, p.tinta))
                    else -> IconoDiptico(Icono.APPS, p.tenue, tam * 0.5f)
                }
            }
        }
        else -> {
            val lleno = rememberImagen(android.net.Uri.parse("app://$paquete?capa=lleno"), 144)
            Box(
                modifier.size(esc.d(tam)).clip(forma).background(p.superficie).border(esc.d(1), Color.White.copy(alpha = 0.08f), forma),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    // Íconos modernos: ocupan toda la forma del tema.
                    lleno != null -> Image(lleno, null, Modifier.fillMaxSize())
                    // Íconos antiguos: centrados sobre una tarjeta del color del tema.
                    base != null -> Image(base, null, Modifier.fillMaxSize(0.78f))
                    else -> IconoDiptico(Icono.APPS, p.tenue, tam * 0.5f)
                }
            }
        }
    }
}

/** Pasa una imagen a dos tonos: lo oscuro toma [oscuro] y lo claro toma [claro]. */
private fun filtroDuotono(oscuro: Color, claro: Color): androidx.compose.ui.graphics.ColorFilter {
    // Luminancia de cada punto (0..1) y de ahí una mezcla lineal entre los dos colores.
    val l = floatArrayOf(0.299f, 0.587f, 0.114f)
    fun fila(o: Float, c: Float) = floatArrayOf(l[0] * (c - o), l[1] * (c - o), l[2] * (c - o), 0f, o * 255f)
    val m = fila(oscuro.red, claro.red) + fila(oscuro.green, claro.green) + fila(oscuro.blue, claro.blue) + floatArrayOf(0f, 0f, 0f, 1f, 0f)
    return androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(m))
}

/**
 * Cajón de apps: todas las apps de la consola en una cuadrícula, con pestañas (Todas, Fijadas, Juegos).
 * A abre, Y fija o suelta, X muestra su información en Android (para desinstalarla o ver permisos).
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PanelApps(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val apps = remember(e.appsTodas, e.appsFijadas, e.appsPestana) { Diptico.appsVisibles(e) }
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Box(Modifier.fillMaxWidth().height(esc.d(160)).background(Brush.verticalGradient(listOf(Cielo.copy(alpha = 0.16f), Color.Transparent))))
        Column(Modifier.fillMaxSize().padding(start = esc.d(14), end = esc.d(14), top = esc.d(12), bottom = esc.d(10))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(esc.d(36)).clip(RoundedCornerShape(esc.d(11))).fondoBoton(Cielo), contentAlignment = Alignment.Center) {
                    IconoDiptico(Icono.APPS, Color.White, 18f, acento = Color.White)
                }
                Spacer(Modifier.width(esc.d(10)))
                Column(Modifier.weight(1f)) {
                    Texto("Apps de la consola", 18f, lineas = 1)
                    Detalle(if (e.cargandoTodas) "Buscando tus apps…" else "${e.appsTodas.size} apps · ${e.appsFijadas.size} fijadas", 12f, lineas = 1)
                }
                Box(
                    Modifier.size(esc.d(34)).clip(CircleShape).background(p.superficie).clickable { Diptico.cerrarPanel() },
                    contentAlignment = Alignment.Center,
                ) { IconoDiptico(Icono.CERRAR, p.tinta, 16f) }
            }
            Spacer(Modifier.height(esc.d(10)))
            // Pestañas segmentadas.
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(p.superficie.copy(alpha = 0.8f)).padding(esc.d(3)),
                horizontalArrangement = Arrangement.spacedBy(esc.d(3)),
            ) {
                Diptico.PESTANAS_APPS.forEachIndexed { i, t ->
                    val activa = i == e.appsPestana
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(50)).then(if (activa) Modifier.fondoBoton(Cielo) else Modifier)
                            .clickable { Diptico.elegirPestanaApps(i) }.padding(vertical = esc.d(6)),
                        contentAlignment = Alignment.Center,
                    ) { Texto(t, 13f, color = if (activa) Color.White else p.tinta, lineas = 1) }
                }
            }
            Spacer(Modifier.height(esc.d(10)))
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val anchoU = maxWidth.value / esc.factor
                val cols = (anchoU / 86f).toInt().coerceIn(3, 8)
                SideEffect { Teclas.columnasApps = cols }
                val grilla = rememberLazyGridState()
                LaunchedEffect(e.indice, e.appsPestana) { if (apps.isNotEmpty()) grilla.animateScrollToItem((e.indice - cols).coerceAtLeast(0)) }
                if (apps.isEmpty() && !e.cargandoTodas) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        IconoDiptico(Icono.APPS, p.tenue, 36f)
                        Spacer(Modifier.height(esc.d(8)))
                        Detalle(if (e.appsPestana == 1) "Fija tus apps favoritas con Y y aparecen aquí." else "No hay apps en esta pestaña.", 14f, lineas = 2)
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols), state = grilla, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = esc.d(4)),
                    verticalArrangement = Arrangement.spacedBy(esc.d(8)), horizontalArrangement = Arrangement.spacedBy(esc.d(6)),
                ) {
                    itemsIndexed(apps, key = { _, a -> a.paquete }) { i, a ->
                        CeldaApp(a, i == e.indice, a.paquete in e.appsFijadas, onLongClick = { Diptico.alternarFijada(a.paquete) }) {
                            if (i == e.indice) Diptico.abrirPaquete(a.paquete) else Diptico.elegirIndice(i)
                        }
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("A" to "Abrir", "Y" to "Fijar", "X" to "Información", "L1 R1" to "Pestaña", "B" to "Cerrar")
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CeldaApp(a: App, elegida: Boolean, fijada: Boolean, onLongClick: () -> Unit, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val tam by animateFloatAsState(if (elegida) 1.08f else 1f, spring(dampingRatio = 0.5f), label = "app")
    Column(
        Modifier.clip(RoundedCornerShape(esc.d(16)))
            .background(if (elegida) Cielo.copy(alpha = 0.18f) else Color.Transparent)
            .border(esc.d(if (elegida) 2f else 0f), if (elegida) Cielo else Color.Transparent, RoundedCornerShape(esc.d(16)))
            .combinedClickable(onLongClick = onLongClick) { alPulsar() }
            .padding(vertical = esc.d(8), horizontal = esc.d(4)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            IconoApp(a.paquete, 46f, Modifier.graphicsLayer { scaleX = tam; scaleY = tam })
            if (fijada) Box(
                Modifier.align(Alignment.TopEnd).size(esc.d(14)).clip(CircleShape).background(Sol).border(esc.d(1.5f), p.fondo2, CircleShape),
            )
        }
        Spacer(Modifier.height(esc.d(5)))
        Texto(a.nombre, 11.5f, color = if (elegida) p.tinta else p.tenue, lineas = 2, interlineado = 13.5f, alinear = TextAlign.Center, traducir = false)
    }
}

/** En la otra pantalla, con el cajón abierto: la app elegida en grande. */
@Composable
fun LecturaApps(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val a = remember(e) { Diptico.appsVisibles(e).getOrNull(e.indice) }
    Box(Modifier.fillMaxSize().background(p.fondo)) {
        Box(Modifier.align(Alignment.CenterEnd).size(esc.d(520)).background(Brush.radialGradient(listOf(Cielo.copy(alpha = 0.22f), Color.Transparent))))
        BarraSuperior(Modifier.align(Alignment.TopStart), trabajos = e.trabajos)
        if (a == null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                IconoDiptico(Icono.APPS, p.tenue, 60f)
                Spacer(Modifier.height(esc.d(10)))
                Texto("Apps de la consola", 24f)
            }
            return@Box
        }
        Row(Modifier.align(Alignment.CenterStart).padding(horizontal = esc.d(48)), verticalAlignment = Alignment.CenterVertically) {
            IconoApp(a.paquete, 120f)
            Spacer(Modifier.width(esc.d(28)))
            Column {
                Etiqueta(if (a.esJuego) "Juego de Android" else "App", Coral, 12f)
                Texto(a.nombre, 36f, lineas = 2, interlineado = 40f, traducir = false)
                Detalle(a.paquete, 13f, lineas = 1)
                Spacer(Modifier.height(esc.d(14)))
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                    Box(Modifier.clip(RoundedCornerShape(50)).fondoBoton(Cielo).clickable { Diptico.abrirPaquete(a.paquete) }.padding(horizontal = esc.d(18), vertical = esc.d(8))) {
                        Texto("A · Abrir", 15f, color = Color.White, lineas = 1)
                    }
                    Box(Modifier.clip(RoundedCornerShape(50)).background(p.superficie).clickable { Diptico.alternarFijada(a.paquete) }.padding(horizontal = esc.d(18), vertical = esc.d(8))) {
                        Texto(if (a.paquete in e.appsFijadas) "Y · Soltar" else "Y · Fijar", 15f, lineas = 1)
                    }
                }
            }
        }
    }
}

/** Widget de apps: las fijadas a un toque, y un botón para el cajón completo. */
@Composable
fun WidgetApps(e: Estado, columnas: Int) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val fijadas = e.appsFijadas.take((columnas * 1.6f).toInt().coerceAtLeast(2))
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        fijadas.forEach { paq ->
            Box(Modifier.clip(RoundedCornerShape(esc.d(12))).clickable { Diptico.abrirPaquete(paq) }.padding(esc.d(3))) { IconoApp(paq, 38f) }
        }
        Column(
            Modifier.clip(RoundedCornerShape(esc.d(12))).clickable { Diptico.abrirApps() }.padding(esc.d(3)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(esc.d(38)).clip(RoundedCornerShape(esc.d(10))).background(p.fondo.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                IconoDiptico(Icono.APPS, p.tinta, 20f, acento = Cielo)
            }
            if (fijadas.isEmpty()) Texto("Todas las apps", 10.5f, color = p.tenue, lineas = 1)
        }
    }
}

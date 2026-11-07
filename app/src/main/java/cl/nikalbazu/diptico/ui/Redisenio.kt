@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.BuildConfig
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono

/** La revisión usa los recorridos completos de Díptico. También permite comparar ambas vistas. */
val LocalRedisenio = staticCompositionLocalOf { BuildConfig.RENACER }

@Composable
internal fun FondoColeccion(e: Estado, modifier: Modifier = Modifier) {
    val p = LocalPaleta.current
    val color = colorColeccion(e.consolaSel)
    Box(modifier.background(p.fondo2)) {
        e.seleccionado?.let { juego ->
            val imagen = rememberImagen(juego.arte ?: juego.portada, 96)
            if (imagen != null) Image(imagen, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .10f)
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.fondo2.copy(alpha = .5f), p.fondo2))))
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(color.copy(alpha = .18f), Color.Transparent, Coral.copy(alpha = .08f)))))
    }
}

@Composable
internal fun CabeceraColeccion(e: Estado, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    Row(modifier.fillMaxWidth().padding(horizontal = esc.d(17), vertical = esc.d(9)), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Texto("Tu colección", 21f, familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1)
            Detalle("${e.juegos.count { e.datos[it.clave]?.oculto != true }} juegos · ${e.pestanas.count { it != "todo" }} consolas", 10.5f, lineas = 1)
        }
        AccesoColeccion(Icono.BUSCAR, "Buscar juegos") { Diptico.abrirBuscar() }
        Spacer(Modifier.width(esc.d(6)))
        AccesoColeccion(Icono.MENU, "Opciones de biblioteca") { Diptico.abrirGaleria("biblioteca") }
    }
}

@Composable
private fun AccesoColeccion(icono: Icono, nombre: String, accion: () -> Unit) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    Box(Modifier.size(esc.d(35)).semantics { contentDescription = nombre }.clip(RoundedCornerShape(esc.d(12)))
        .background(p.superficie.copy(alpha = .78f)).border(esc.d(.7f), p.linea, RoundedCornerShape(esc.d(12)))
        .clickable(onClickLabel = nombre, onClick = accion), contentAlignment = Alignment.Center) {
        IconoDiptico(icono, p.tinta, 17f)
    }
}

/** Un dock compacto mantiene los accesos anteriores y deja que el juego ocupe la escena. */
@Composable
internal fun DockDiptico(e: Estado, juego: Juego?, modifier: Modifier, arriba: Boolean, consola: Boolean) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val widgets = e.inicio == "widgets"
    val forma = RoundedCornerShape(esc.d(21))
    Box(modifier.then(if (arriba) Modifier else Modifier.fillMaxWidth()).padding(
        horizontal = esc.d(if (arriba) 0 else 14), vertical = esc.d(if (arriba) 0 else 9)), contentAlignment = Alignment.Center) {
        Row(Modifier.widthIn(max = esc.d(740)).shadow(esc.d(12), forma).clip(forma)
            .background(p.fondo2.copy(alpha = .96f)).border(esc.d(.8f), p.linea, forma)
            .horizontalScroll(rememberScrollState()).padding(esc.d(5)),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(3))) {
            if (consola) AccesoDock(Icono.CARPETA, "Abrir carpeta", destacado = true) { Diptico.entrarSistema() }
            else if (juego != null) AccesoDock(Icono.JUGAR, "Jugar", destacado = true) { Diptico.jugar() }
            if (juego != null && !consola) AccesoDock(Icono.INFO, "Ficha del juego") { Diptico.abrirFicha() }
            AccesoDock(Icono.BUSCAR, "Buscar") { Diptico.abrirBuscar() }
            AccesoDock(Icono.APPS, "Aplicaciones") { Diptico.abrirApps() }
            AccesoDock(if (widgets) Icono.PORTADA else Icono.VISTA, if (widgets) "Carátulas" else "Widgets") { Diptico.alternarInicio() }
            if (widgets) AccesoDock(Icono.EDITAR, "Editar widgets") { Diptico.editarWidgets() }
            AccesoDock(Icono.BISA, "Bisa", mantener = { Diptico.hablarConBisa() }) { Diptico.abrirAsistente() }
            AccesoDock(Icono.AJUSTES, "Ajustes") { Diptico.abrirMenu() }
        }
    }
}

@Composable
private fun AccesoDock(icono: Icono, nombre: String, destacado: Boolean = false, mantener: (() -> Unit)? = null, accion: () -> Unit) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val forma = RoundedCornerShape(esc.d(16))
    val tinta = if (destacado) SobreUva else p.tenue
    Row(Modifier.height(esc.d(38)).semantics { contentDescription = nombre }.clip(forma)
        .then(if (destacado) Modifier.fondoBoton(Uva) else Modifier)
        .combinedClickable(onClickLabel = nombre, onLongClick = mantener, onClick = accion)
        .padding(horizontal = esc.d(if (destacado) 15 else 11)),
        horizontalArrangement = Arrangement.spacedBy(esc.d(7)), verticalAlignment = Alignment.CenterVertically) {
        IconoDiptico(icono, tinta, 18f, acento = tinta)
        if (destacado) Texto(nombre, 12f, familia = Nunito, color = tinta, lineas = 1)
    }
}

/** Vuelve el arte a todo el lienzo, con una ficha breve y la portada completa como segundo plano. */
@Composable
internal fun EscenaDiptico(e: Estado, modifier: Modifier = Modifier, controles: Boolean = true, reducida: Boolean = false) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val juego = e.seleccionado
    Box(modifier.fillMaxSize().testTag("escena-diptico").background(p.fondo)) {
        if (juego != null) Heroe(juego, 1f)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
            0f to p.fondo.copy(alpha = .98f), .35f to p.fondo.copy(alpha = .84f), 1f to p.fondo.copy(alpha = .08f))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to p.fondo.copy(alpha = .15f), .6f to Color.Transparent, 1f to p.fondo.copy(alpha = .94f))))
        if (!reducida) BarraSuperior(Modifier.align(Alignment.TopStart), inicio = 25f, trabajos = e.trabajos)
        if (juego == null) Column(Modifier.align(Alignment.CenterStart).padding(esc.d(26))) {
            Logo(26f)
            Spacer(Modifier.height(esc.d(12)))
            Texto("Aquí empieza tu próxima partida", 25f, familia = Nunito, lineas = 2)
            Detalle("Agrega tus juegos desde Ajustes › Biblioteca", 13f)
        } else {
            Row(Modifier.fillMaxSize().padding(start = esc.d(26), end = esc.d(30),
                top = esc.d(if (reducida) 16 else 66), bottom = esc.d(if (controles) 73 else 18)),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
                    val c = Consolas.porId(juego.consolaId)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(7))) {
                        IconoConsola(c, 20f, p.fondo)
                        Texto(c?.nombreLargo ?: "Tu biblioteca", 10.5f, color = p.acentoTexto, familia = Nunito, lineas = 1)
                    }
                    val logo = rememberImagen(juego.logo, 700)
                    if (logo != null) Image(logo, juego.titulo, Modifier.fillMaxWidth().heightIn(max = esc.d(if (reducida) 65 else 95)), contentScale = ContentScale.Fit, alignment = Alignment.CenterStart)
                    else Texto(juego.titulo, if (reducida) 26f else 35f, familia = Nunito, peso = FontWeight.ExtraBold,
                        lineas = if (reducida) 2 else 3, interlineado = if (reducida) 29f else 37f, sombra = true)
                    val d = e.datos[juego.clave]
                    val tiempo = d?.tiempo?.takeIf { it > 0 }?.let { "${formatoTiempo(it)} jugadas" } ?: "Listo para jugar"
                    Texto(tiempo + if (d?.favorito == true) "  ·  ♥ Favorito" else "", 11.5f, color = p.tenue, familia = Nunito, lineas = 1)
                    if (!reducida && !d?.paso.isNullOrBlank()) {
                        Column(Modifier.widthIn(max = esc.d(315)).clip(RoundedCornerShape(esc.d(14)))
                            .background(p.fondo2.copy(alpha = .68f)).border(esc.d(.8f), p.linea, RoundedCornerShape(esc.d(14)))
                            .clickable { Diptico.abrirPaso() }.padding(esc.d(12)), verticalArrangement = Arrangement.spacedBy(esc.d(3))) {
                            Texto("Donde lo dejaste", 10f, familia = Nunito, color = p.acentoTexto)
                            Texto(d?.paso.orEmpty(), 12f, familia = Nunito, peso = FontWeight.SemiBold, lineas = 2)
                        }
                    }
                }
                if (!reducida && esc.ancho >= 600f && juego.portada != null) {
                    Spacer(Modifier.width(esc.d(28)))
                    PortadaEscena(juego, Modifier.widthIn(max = esc.d(190)).heightIn(max = esc.d(220)))
                }
            }
        }
        if (controles && e.sesion == null) DockDiptico(e, juego, Modifier.align(Alignment.BottomCenter), arriba = false, consola = false)
    }
}

@Composable
private fun PortadaEscena(juego: Juego, modifier: Modifier) {
    val esc = LocalEscala.current
    val portada = rememberImagen(juego.portada, 700) ?: return
    val proporcion = portada.width.toFloat() / portada.height.coerceAtLeast(1)
    val forma = RoundedCornerShape(esc.d(14))
    Box(modifier.aspectRatio(proporcion).shadow(esc.d(22), forma).clip(forma).background(LocalPaleta.current.superficie)
        .border(esc.d(1), Color.White.copy(alpha = .25f), forma)) {
        Image(portada, juego.titulo, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

/** Una pantalla combina el escenario y la biblioteca sin quitar el contexto al abrir una carpeta. */
@Composable
internal fun InicioUnificado(e: Estado) {
    val esc = LocalEscala.current
    val ventana = e.carpetaEnVentana
    val completa = e.vistaConsolas == "carpetas" && !e.enSistemas && e.modoCarpeta == "completa"
    val fondo = if (ventana) e.copy(enSistemas = true) else e
    BoxWithConstraints(Modifier.fillMaxSize().background(LocalPaleta.current.fondo)) {
        val vertical = maxHeight > maxWidth
        Box(Modifier.fillMaxSize().difuminar(e).then(if (ventana) Modifier.desenfocarCuadricula() else Modifier)) {
            Column(Modifier.fillMaxSize()) {
                if (completa) {
                    Box(Modifier.fillMaxWidth().weight(1f).testTag("carpeta-completa")) { ColeccionUnificada(e) }
                } else if (vertical) {
                    BarraSuperior(Modifier, inicio = 24f, trabajos = e.trabajos)
                    Box(Modifier.fillMaxWidth().weight(.43f)) { EscenaDiptico(e, controles = false, reducida = true) }
                    Box(Modifier.fillMaxWidth().weight(.57f)) { ColeccionUnificada(fondo) }
                } else {
                    BarraSuperior(Modifier, inicio = 24f, trabajos = e.trabajos)
                    Row(Modifier.weight(1f).padding(top = esc.d(12))) {
                        Box(Modifier.weight(.43f).fillMaxHeight()) { EscenaDiptico(e, controles = false, reducida = true) }
                        Box(Modifier.weight(.57f).fillMaxHeight().clip(RoundedCornerShape(esc.d(22)))) { ColeccionUnificada(fondo) }
                        Spacer(Modifier.width(esc.d(14)))
                    }
                }
                if (e.sesion == null && !completa) DockDiptico(e, e.seleccionado.takeIf { e.enSistemas }, Modifier, arriba = false, consola = false)
            }
        }
        if (ventana) {
            Box(Modifier.fillMaxSize().background(LocalPaleta.current.fondo.copy(alpha = .18f)).clickable { Diptico.volverASistemas() })
            VentanaCarpeta(e, Modifier.align(Alignment.Center).fillMaxWidth(.94f).fillMaxHeight(.90f), .95f)
        }
        Paneles(e, conAyuda = true)
    }
}

@Composable
internal fun VentanaCarpeta(e: Estado, modifier: Modifier, escala: Float) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val forma = RoundedCornerShape(esc.d(19))
    Column(modifier.shadow(esc.d(12), forma, clip = false).clip(forma)
        .background(p.fondo2.copy(alpha = if (LocalRedisenio.current) .92f else .80f)).border(esc.d(1), p.linea, forma).sinCerrar()) {
        if (LocalRedisenio.current) Box(Modifier.fillMaxWidth().height(esc.d(3))
            .background(Brush.horizontalGradient(listOf(colorColeccion(e.consolaSel), Uva, Coral))))
        CabeceraSistema(e)
        VistaJuegos(e, Modifier.fillMaxWidth().weight(1f), escala)
    }
}

@Composable
private fun ColeccionUnificada(e: Estado) {
    val esc = LocalEscala.current
    Box(Modifier.fillMaxSize()) {
        FondoColeccion(e, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            if (!(e.vistaConsolas in Diptico.MODOS_ELEGIR && !e.enSistemas)) CabeceraColeccion(e)
            // El reparto de columnas depende del ancho del panel, no del ancho del dispositivo entero.
            BoxWithConstraints(Modifier.weight(1f)) {
                CompositionLocalProvider(LocalEscala provides Escala(esc.factor, maxWidth.value / esc.factor, esc.texto)) {
                    when {
                        e.vistaConsolas == "carpetas" -> VistaCarpetas(e, Modifier.fillMaxSize(), .8f)
                        e.vistaConsolas == "lado" || e.vistaConsolas == "rueda" -> Row(Modifier.fillMaxSize()) {
                            if (e.vistaConsolas == "lado") RielConsolas(e) else RuedaConsolas(e)
                            VistaJuegos(e, Modifier.weight(1f).fillMaxHeight(), .8f)
                        }
                        e.vistaConsolas in Diptico.MODOS_ELEGIR && e.enSistemas -> when (e.vistaConsolas) {
                            "discos" -> DiscosConsolas(e, Modifier.fillMaxSize())
                            "sistemas" -> SistemasCarrusel(e, Modifier.fillMaxSize())
                            else -> CarpetasConsolas(e, Modifier.fillMaxSize())
                        }
                        else -> Column(Modifier.fillMaxSize()) {
                            if (e.vistaConsolas in Diptico.MODOS_ELEGIR) CabeceraSistema(e) else Cinta(e)
                            VistaJuegos(e, Modifier.fillMaxWidth().weight(1f), .8f)
                        }
                    }
                }
            }
        }
    }
}

/** La carátula descargada tiene prioridad. Cada consola conserva su color e identidad. */
@Composable
internal fun IconoCarpetaDiptico(e: Estado, id: String, elegida: Boolean, interactiva: Boolean, lado: Dp) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val c = Consolas.porId(id)
    val color = colorColeccion(id)
    val forma = RoundedCornerShape(lado * .22f)
    val juegos = if (id == "todo") e.juegos else e.juegosPorConsola[id].orEmpty()
    val cantidad = juegos.count { e.datos[it.clave]?.oculto != true }
    Column(Modifier.size(lado), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).aspectRatio(1f).testTag("carpeta-$id").shadow(if (elegida) esc.d(7) else esc.d(2), forma, spotColor = color)
            .clip(forma).background(Brush.linearGradient(listOf(color.copy(alpha = .48f), p.fondo)))
            .combinedClickable(enabled = interactiva, onClickLabel = "Abrir ${c?.nombre ?: "todos los juegos"}",
                onLongClickLabel = "Personalizar carpeta", onLongClick = { Diptico.abrirEditarConsola(id) }) {
                if (!elegida) Diptico.elegirConsola(id)
                Diptico.entrarSistema()
            }, contentAlignment = Alignment.Center) {
            PortadaCarpeta(e, id, Modifier.fillMaxSize(), maxLado = 320, memoriaGrafica = false) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(color.copy(alpha = .12f), size.width * .65f, Offset(size.width * .82f, size.height * .3f))
                    drawCircle(color.copy(alpha = .20f), size.width * .46f, Offset(size.width * .8f, size.height * .25f), style = Stroke(size.width * .013f))
                    drawLine(color.copy(alpha = .16f), Offset(0f, size.height * .8f), Offset(size.width, size.height * .34f), size.width * .06f)
                }
                IconoConsola(c?.copy(color = color), (lado.value / esc.factor * .47f).coerceIn(18f, 76f), p.fondo2, memoriaGrafica = false)
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .52f)))))
            Box(Modifier.fillMaxSize().border(esc.d(if (elegida) 2.2f else .8f), if (elegida) p.tinta else color.copy(alpha = .55f), forma))
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth(.38f).height(esc.d(if (elegida) 3f else 1.5f))
                .background(if (elegida) p.tinta else color))
            if (lado / esc.d(1) >= 66f) Texto("$cantidad", 8.5f, familia = Nunito, color = Color.White.copy(alpha = .8f),
                modifier = Modifier.align(Alignment.TopEnd).padding(esc.d(8)))
        }
        Spacer(Modifier.height(esc.d(6)))
        Texto(c?.nombre ?: "Todo", 10.5f, familia = Nunito, peso = FontWeight.ExtraBold,
            color = if (elegida) p.tinta else p.tenue, lineas = 1)
    }
}

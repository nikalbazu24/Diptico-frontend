package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.horizontalScroll
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Panel
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import java.util.Date

/** Las ↑ ↓ desplazan el texto largo. */
@Composable
private fun DesplazarCon(indice: Int, scroll: ScrollState) {
    val px = with(LocalDensity.current) { LocalEscala.current.d(90f).toPx() }
    LaunchedEffect(indice) { scroll.animateScrollTo((indice * px).toInt().coerceAtMost(scroll.maxValue)) }
}

/** Textos de la pestaña Resumen o Historia, por secciones. */
@Composable
private fun TextoFicha(e: Estado, pestana: Int, tamTexto: Float) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val f = e.ficha
    when {
        e.cargandoFicha -> Row(verticalAlignment = Alignment.CenterVertically) {
            Girador(20f)
            Spacer(Modifier.width(esc.d(10)))
            Texto("Buscando en Wikipedia…", 16f)
        }
        f == null || f.vacia -> Column(verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
            Texto("No encontré la historia de este juego", 18f)
            Detalle("Necesito internet, y que el título se parezca al nombre real del juego. Puedes cambiar el título en Personalizar.", 14.5f)
        }
        pestana == 0 -> Column(verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
            Detalle(f.resumen, tamTexto, color = p.tinta)
            Detalle("Fuente: Wikipedia (${cl.nikalbazu.diptico.estado.Idioma.IDIOMAS.firstOrNull { it.first == f.idioma }?.second ?: f.idioma})", 12f)
        }
        else -> Column(verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
            val partes = listOf("Cómo se creó" to f.historia, "El juego" to f.juego, "Cómo le fue" to f.recepcion).filter { it.second.isNotBlank() }
            if (partes.isEmpty()) Detalle("Wikipedia no tiene la historia de su creación. Mira el Resumen o busca en Guías.", tamTexto)
            for ((t, texto) in partes) {
                Column(verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
                    Etiqueta(t, Coral, 12.5f)
                    Detalle(texto, tamTexto, color = p.tinta)
                }
            }
        }
    }
}

/** Datos rápidos del juego: tiempo jugado, última vez, favorito. */
@Composable
private fun Datos(d: DatosJuego?) {
    val esc = LocalEscala.current
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
        Chip(if ((d?.tiempo ?: 0) > 0) formatoTiempo(d!!.tiempo) else "Sin jugar")
        if ((d?.ultimo ?: 0) > 1) Chip("Última vez: " + cl.nikalbazu.diptico.estado.Idioma.fecha("dMMM", Date(d!!.ultimo)))
        if (d?.favorito == true) Chip("♥ Favorito", Coral)
    }
}

@Composable
private fun Chip(texto: String, color: Color = LocalPaleta.current.tenue) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(Modifier.clip(RoundedCornerShape(50)).background(p.superficie).padding(horizontal = esc.d(10), vertical = esc.d(4))) {
        Texto(texto, 12.5f, color = color, familia = Nunito, lineas = 1)
    }
}

/** Ficha del juego: resumen, historia, guías y trucos, y personalización. Pestañas con L1/R1 o ← →. */
@Composable
fun PanelFicha(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val j = e.juegos.firstOrNull { it.clave == e.fichaDe } ?: return
    val c = Consolas.porId(j.consolaId)
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Box(Modifier.fillMaxWidth().height(esc.d(150)).background(Brush.verticalGradient(listOf(colorDe(j.clave).copy(alpha = 0.35f), Color.Transparent))))
        Column(Modifier.fillMaxSize().padding(start = esc.d(16), end = esc.d(16), top = esc.d(12), bottom = esc.d(10))) {
            // Al bajar por la lista, la cabecera y las pestañas se van hacia arriba y dejan todo el espacio a las opciones;
            // al volver al comienzo reaparecen. Arriba queda una línea fina con el juego y la pestaña.
            val arriba = e.indice == 0
            AnimatedVisibility(!arriba, enter = expandVertically(tween(220)) + fadeIn(tween(220)), exit = shrinkVertically(tween(200)) + fadeOut(tween(150))) {
                Row(Modifier.fillMaxWidth().padding(bottom = esc.d(8)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                    Texto(j.titulo, 14f, lineas = 1, traducir = false, modifier = Modifier.weight(1f))
                    Box(Modifier.clip(RoundedCornerShape(50)).background(Uva).padding(horizontal = esc.d(10), vertical = esc.d(4))) {
                        Texto(Diptico.PESTANAS_FICHA.getOrElse(e.pestanaFicha) { "" }, 12f, color = SobreUva, lineas = 1)
                    }
                    Texto("L1 R1", 11f, color = p.tenue, lineas = 1, traducir = false)
                }
            }
            AnimatedVisibility(arriba, enter = expandVertically(tween(240)) + fadeIn(tween(240)), exit = shrinkVertically(tween(220)) + fadeOut(tween(150))) {
            Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (c != null) CajaJuego(j, c, escala = 0.5f)
                Spacer(Modifier.width(esc.d(12)))
                Column(Modifier.weight(1f)) {
                    Etiqueta(c?.bonito ?: "", Coral, 11.5f)
                    Texto(j.titulo, 21f, lineas = 2, interlineado = 24f)
                    Spacer(Modifier.height(esc.d(4)))
                    Datos(e.datos[j.clave])
                }
            }
            Spacer(Modifier.height(esc.d(10)))
            // Pestañas
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                Diptico.PESTANAS_FICHA.forEachIndexed { i, t ->
                    val activa = i == e.pestanaFicha
                    val fondo by animateColorAsState(if (activa) Uva else p.superficie, label = "pestana")
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(fondo).clickable { Diptico.elegirPestanaFicha(i) }
                            .padding(horizontal = esc.d(if (esc.angosta) 9f else 14f), vertical = esc.d(7)),
                    ) {
                        Texto(if (esc.angosta && i == 2) "Guías" else t, 13.5f, color = if (activa) SobreUva else p.tinta, lineas = 1)
                    }
                }
            }
            Spacer(Modifier.height(esc.d(10)))
            }
            }
            AnimatedContent(
                targetState = e.pestanaFicha,
                transitionSpec = { (fadeIn(tween(250)) + slideInHorizontally(tween(280)) { if (targetState > initialState) it / 10 else -it / 10 }) togetherWith fadeOut(tween(120)) },
                label = "pestanaFicha",
                modifier = Modifier.weight(1f),
            ) { pestana ->
                if (pestana == 4) {
                    ListaLogros(e)
                } else if (pestana >= 2) {
                    val opciones = Diptico.opcionesFicha()
                    val lista = rememberLazyListState()
                    LaunchedEffect(e.indice) { if (opciones.isNotEmpty()) lista.mostrar(e.indice) }
                    LazyColumn(state = lista, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        if (pestana == 2 && (e.datos[j.clave]?.notas ?: "").isNotBlank()) {
                            item {
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(16))).background(Sol.copy(alpha = 0.14f)).padding(esc.d(12))) {
                                    Detalle(e.datos[j.clave]!!.notas, 14.5f, color = p.tinta, lineas = 6)
                                }
                            }
                        }
                        itemsIndexed(opciones) { i, o -> TarjetaOpcion(o, i == e.indice, alto = 54f) { Diptico.elegirIndice(i) } }
                    }
                } else {
                    val scroll = rememberScrollState()
                    DesplazarCon(e.indice, scroll)
                    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                        TextoFicha(e, pestana, 15.5f)
                        Spacer(Modifier.height(esc.d(20)))
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("L1 R1" to "Pestaña", "↑ ↓" to "Moverte", "B" to "Cerrar")
        }
    }
}

/**
 * En la otra pantalla, mientras la ficha está abierta: el arte del juego y el texto en grande para leer cómodo.
 */
@Composable
fun LecturaFicha(e: Estado) {
    if (e.panel == Panel.IMAGENES) { VistaCandidato(e); return }
    val j = e.juegos.firstOrNull { it.clave == e.fichaDe } ?: return
    // Logros: el logro elegido en grande.
    if (e.pestanaFicha == 4) { LogroGrande(e, j.titulo); return }
    // La ficha (texto y opciones) vive en la otra pantalla. Aquí no se repite: aquí va la vitrina del juego.
    VitrinaFicha(e, j)
}

/**
 * Vitrina de la ficha: complementa lo que lees abajo sin repetirlo. Pasan tus capturas y las fotos del juego
 * (o su arte, si no hay), y DIPPO va comentando datos que no están en la ficha: tus horas, cuánto te queda,
 * tu última partida, tus capturas. En «Personalizar» se ve el arte y el logo para que notes los cambios al tiro.
 */
@Composable
private fun VitrinaFicha(e: Estado, j: cl.nikalbazu.diptico.data.Juego) {
    val esc = LocalEscala.current
    val d = e.datos[j.clave]
    // Primero tus capturas (son tuyas), después las fotos de internet.
    val fotos = remember(j.clave, d?.capturas, e.fichaImagenes) {
        (d?.capturas.orEmpty().map { it to "Tu captura" } + e.fichaImagenes.map { it.miniatura to it.fuente }).take(16)
    }
    val personalizar = e.pestanaFicha == 3
    var actual by remember(fotos) { mutableIntStateOf(0) }
    if (!personalizar && fotos.size > 1) LaunchedEffect(fotos) {
        while (true) { delay(6000); actual = (actual + 1) % fotos.size }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (personalizar || fotos.isEmpty()) {
            // El arte del juego, grande y quieto, con su logo: lo que personalizas se ve aquí.
            ArteJuego(j, preferirArte = true, maxLado = 1280, modifier = Modifier.fillMaxSize(), imagen = "arte")
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
            val logo = rememberImagen(j.logo, 900)
            if (logo != null) Image(logo, j.titulo, Modifier.align(Alignment.Center).heightIn(max = esc.d(150)).widthIn(max = esc.d(460)), contentScale = ContentScale.Fit)
        } else Crossfade(targetState = actual, animationSpec = tween(900), label = "vitrina") { i ->
            val (ruta, _) = fotos.getOrNull(i) ?: return@Crossfade
            val img = rememberImagen(Uri.parse(ruta), 1280)
            val zoom = remember { Animatable(1f) }
            LaunchedEffect(i) { zoom.snapTo(1f); zoom.animateTo(1.1f, tween(7000)) }
            Box(Modifier.fillMaxSize()) {
                if (img != null) {
                    Image(img, null, Modifier.fillMaxSize().blur(24.dp).alpha(0.5f), contentScale = ContentScale.Crop)
                    Image(img, null, Modifier.fillMaxSize().graphicsLayer { scaleX = zoom.value; scaleY = zoom.value }, contentScale = ContentScale.Fit)
                } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Girador(26f) }
            }
        }
        Box(Modifier.fillMaxWidth().height(esc.d(170)).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
        // DIPPO comenta abajo a la izquierda; a la derecha, de dónde es la foto y los puntitos.
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = esc.d(18), end = esc.d(18), bottom = esc.d(14)),
            verticalAlignment = Alignment.Bottom) {
            ComentarioDippo(e, j, Modifier.weight(1f))
            if (!personalizar && fotos.isNotEmpty()) Column(horizontalAlignment = Alignment.End) {
                Detalle(fotos.getOrNull(actual)?.second ?: "", 11.5f, color = Color.White.copy(alpha = 0.7f), lineas = 1)
                Spacer(Modifier.height(esc.d(4)))
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(5))) {
                    fotos.indices.take(10).forEach { k ->
                        Box(Modifier.size(esc.d(if (k == actual) 16f else 6f), esc.d(6)).clip(RoundedCornerShape(50))
                            .background(if (k == actual) Uva else Color.White.copy(alpha = 0.5f)))
                    }
                }
            }
        }
        BarraSuperior(Modifier.align(Alignment.TopStart), trabajos = e.trabajos)
    }
}

/** DIPPO con un dato del juego que cambia cada pocos segundos (nada de lo que ya dice la ficha de abajo). */
@Composable
private fun ComentarioDippo(e: Estado, j: cl.nikalbazu.diptico.data.Juego, modifier: Modifier) {
    val esc = LocalEscala.current
    val d = e.datos[j.clave]
    val datos = remember(j.clave, d, e.extra.duraciones[j.clave], e.pestanaFicha) {
        buildList {
            if (e.pestanaFicha == 3) add("Lo que cambies abajo se ve aquí al tiro.")
            if (e.pestanaFicha == 2) add("Si una guía te sirve, anótala en tus notas y te la recuerdo.")
            val horas = (d?.tiempo ?: 0L)
            if (horas > 0) add("Llevas ${formatoTiempo(horas)} en ${d?.sesiones ?: 0} partidas.") else add("Todavía no lo juegas conmigo. ¿Le damos?")
            e.extra.duraciones[j.clave]?.takeIf { it.tiene }?.let { h ->
                cl.nikalbazu.diptico.estado.CuantoDura.teQueda(h, horas)?.let { add("$it, según HowLongToBeat.") }
            }
            if ((d?.ultimo ?: 0L) > 1) add("Tu última partida fue ${haceCuanto(d!!.ultimo).lowercase()}.")
            val capturas = d?.capturas?.size ?: 0
            if (capturas > 0) add("Tienes $capturas ${if (capturas == 1) "captura guardada" else "capturas guardadas"} de este juego.")
            if (d?.favorito == true) add("Es uno de tus favoritos. ♥")
        }
    }
    var k by remember(datos) { mutableIntStateOf(0) }
    if (datos.size > 1) LaunchedEffect(datos) { while (true) { delay(7000); k = (k + 1) % datos.size } }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(46).value)
        Spacer(Modifier.width(esc.d(10)))
        Crossfade(targetState = datos.getOrNull(k) ?: "", animationSpec = tween(500), label = "dato") { t ->
            Box(Modifier.clip(RoundedCornerShape(esc.d(14))).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = esc.d(12), vertical = esc.d(8))) {
                Detalle(t, 14f, color = Color.White, lineas = 2)
            }
        }
    }
}

/** Carrusel de fotos del juego: cada una entra con un fundido y se acerca despacio. */
@Composable
private fun Carrusel(e: Estado, titulo: String) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val fotos = e.fichaImagenes
    var actual by remember(fotos) { mutableIntStateOf(0) }
    LaunchedEffect(fotos) {
        while (true) {
            delay(6000)
            actual = (actual + 1) % fotos.size
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(targetState = actual, animationSpec = tween(900), label = "carrusel") { i ->
            val f = fotos.getOrNull(i) ?: return@Crossfade
            val img = rememberImagen(Uri.parse(f.miniatura), 1280)
            val zoom = remember { Animatable(1f) }
            LaunchedEffect(i) { zoom.snapTo(1f); zoom.animateTo(1.12f, tween(7000)) }
            Box(Modifier.fillMaxSize()) {
                if (img != null) {
                    // Fondo difuminado para rellenar, y la foto completa encima.
                    Image(img, null, Modifier.fillMaxSize().blur(24.dp).alpha(0.55f), contentScale = ContentScale.Crop)
                    Image(img, null, Modifier.fillMaxSize().graphicsLayer { scaleX = zoom.value; scaleY = zoom.value }, contentScale = ContentScale.Fit)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Girador(26f) }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(esc.d(130)).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(start = esc.d(24), bottom = esc.d(16), end = esc.d(24))) {
            Etiqueta("Ficha · ${Diptico.PESTANAS_FICHA[e.pestanaFicha]}", Coral, 12f)
            Texto(titulo, 24f, color = Color.White, lineas = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Detalle(fotos.getOrNull(actual)?.fuente ?: "", 12.5f, color = Color.White.copy(alpha = 0.75f), lineas = 1, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(5))) {
                    fotos.indices.take(12).forEach { k ->
                        Box(Modifier.size(esc.d(if (k == actual) 16f else 6f), esc.d(6)).clip(RoundedCornerShape(50)).background(if (k == actual) Uva else Color.White.copy(alpha = 0.5f)))
                    }
                }
            }
        }
        BarraSuperior(Modifier.align(Alignment.TopStart), trabajos = e.trabajos)
    }
}

/** La opción elegida, en grande, en la otra pantalla. */
@Composable
private fun VistaCandidato(e: Estado) {
    val esc = LocalEscala.current
    val c = Diptico.candidatosVisibles(e).getOrNull(e.indice)?.takeIf { !it.url.startsWith("accion:") }
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (c != null) {
            val img = rememberImagen(Uri.parse(c.url), 1400)
            val mini = rememberImagen(Uri.parse(c.miniatura), 600)
            (img ?: mini)?.let {
                Image(it, null, Modifier.fillMaxSize().blur(30.dp).alpha(0.4f), contentScale = ContentScale.Crop)
                Image(it, null, Modifier.fillMaxSize().padding(esc.d(20)), contentScale = ContentScale.Fit)
            } ?: Girador(26f)
            Column(Modifier.align(Alignment.BottomStart).padding(esc.d(18))) {
                Texto(c.fuente, 16f, color = Color.White)
                Detalle("A para usarla", 12.5f, color = Color.White.copy(alpha = 0.75f))
            }
        } else {
            Detalle(if (e.buscandoCandidatos) "Buscando opciones…" else "Sin opciones", 16f, color = Color.White)
        }
    }
}

/** La tienda de imágenes: portadas, heroes y logos de libretro, SteamGridDB y Wikipedia, más tus archivos. */
@Composable
fun PanelImagenes(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val j = e.juegos.firstOrNull { it.clave == e.fichaDe }
    val lista = remember(e.candidatos, e.fuenteImagen, e.candidatoPara, e.datos, e.claveSgdb) { Diptico.candidatosVisibles(e) }
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Column(Modifier.fillMaxSize().padding(start = esc.d(14), end = esc.d(14), top = esc.d(12), bottom = esc.d(10))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.DESCARGA, p.tinta, 22f, acento = Coral)
                Spacer(Modifier.width(esc.d(10)))
                Column(Modifier.weight(1f)) {
                    Texto("Tienda de imágenes · ${Diptico.nombreCandidato(e.candidatoPara)}", 18f, lineas = 1)
                    Detalle(if (e.candidatoPara.startsWith("c_")) "Buscando: ${e.nombreImagen}" else if (e.nombreImagen.isNotBlank() && e.nombreImagen != j?.titulo) "Buscando: ${e.nombreImagen}" else j?.titulo ?: "", 12.5f, lineas = 1)
                }
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(p.superficie).clickable { Diptico.abrirTexto("buscar_imagen", e.nombreImagen) }
                        .padding(horizontal = esc.d(10), vertical = esc.d(5)),
                ) { Row(verticalAlignment = Alignment.CenterVertically) { IconoDiptico(Icono.BUSCAR, p.tinta, 14f, acento = Coral); Spacer(Modifier.width(esc.d(5))); Texto("Otro nombre", 12f, lineas = 1) } }
            }
            Spacer(Modifier.height(esc.d(8)))
            // Pestañas de fuentes (L1/R1), con cuántas opciones trae cada una.
            val fuentes = Diptico.fuentesImagen(e)
            val filaFuentes = rememberLazyListState()
            LaunchedEffect(e.fuenteImagen, fuentes) { filaFuentes.mostrar(fuentes.indexOf(e.fuenteImagen)) }
            LazyRow(state = filaFuentes, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                items(fuentes.size) { i ->
                    val f = fuentes[i]
                    val n = if (f == "todas") e.candidatos.size else e.candidatos.count { it.fuente == f }
                    val activa = f == e.fuenteImagen
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).then(if (activa) Modifier.fondoBoton(Uva) else Modifier.background(p.superficie))
                            .clickable { Diptico.actualizar { it.copy(fuenteImagen = f, indice = 0) } }
                            .padding(horizontal = esc.d(12), vertical = esc.d(6)),
                    ) { Texto("${Diptico.nombreFuente(f)} · $n", 12.5f, color = if (activa) SobreUva else p.tinta, lineas = 1) }
                }
            }
            Spacer(Modifier.height(esc.d(8)))
            if (e.buscandoCandidatos) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Girador(18f)
                    Spacer(Modifier.width(esc.d(8)))
                    Detalle("Buscando en las fuentes disponibles…", 13f, lineas = 1)
                }
                Spacer(Modifier.height(esc.d(6)))
            } else if (e.candidatos.isEmpty()) {
                Detalle("No encontré opciones con ese nombre. Prueba \"Otro nombre\" o elige una de tus imágenes.", 13f, lineas = 2)
                Spacer(Modifier.height(esc.d(6)))
            }
            val cols = when (e.candidatoPara) { "portada", "c_icono", "c_portada" -> if (esc.angosta) 4 else 6; "logo", "c_logo" -> 3; else -> if (esc.angosta) 2 else 3 }
            SideEffect { cl.nikalbazu.diptico.estado.Teclas.columnasImagenes = cols }
            val grilla = rememberLazyGridState()
            LaunchedEffect(e.indice) { if (lista.isNotEmpty()) grilla.mostrar(e.indice.coerceIn(0, lista.size - 1)) }
            val proporcion = when (e.candidatoPara) { "portada" -> 0.68f; "c_icono", "c_portada" -> 1f; "logo", "c_logo" -> 2f; else -> 2.6f }
            val ajustar = e.candidatoPara in setOf("logo", "c_logo", "c_icono", "c_portada")
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols), state = grilla, modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(esc.d(8)), verticalArrangement = Arrangement.spacedBy(esc.d(8)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(esc.d(3)),
            ) {
                itemsIndexed(lista, key = { _, c -> c.url }) { i, c ->
                    val activa = i == e.indice
                    val atajo = c.url.startsWith("accion:")
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(proporcion).clip(RoundedCornerShape(esc.d(14)))
                            .background(if (atajo) Uva.copy(alpha = 0.16f) else if (ajustar) Color(0xFF2A2633) else p.superficie)
                            .border(esc.d(if (activa) 3f else if (atajo) 1.5f else 0f), if (activa) Uva else if (atajo) Uva.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(esc.d(14)))
                            .clickable { if (activa) Diptico.usarCandidato(i) else Diptico.elegirIndice(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (atajo) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(esc.d(6))) {
                                IconoDiptico(
                                    when (c.url) { "accion:archivos" -> Icono.CARPETA; "accion:quitar" -> Icono.BORRAR; else -> Icono.CLAVE },
                                    p.tinta, 24f, acento = Coral,
                                )
                                Spacer(Modifier.height(esc.d(4)))
                                Texto(c.fuente, 11.5f, lineas = 2, alinear = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        } else {
                            val img = rememberImagen(Uri.parse(c.miniatura), 420)
                            // Las cuadradas (o de otra forma) se ven enteras, sin recortarse, con su etiqueta.
                            val otraForma = e.candidatoPara == "portada" && c.ancho > 0 && c.alto > 0 && kotlin.math.abs(c.ancho.toFloat() / c.alto - 0.68f) > 0.15f
                            if (img != null) Image(img, null, Modifier.fillMaxSize().padding(esc.d(if (ajustar || otraForma) 6f else 0f)), contentScale = if (ajustar || otraForma) ContentScale.Fit else ContentScale.Crop)
                            else Girador(18f)
                            Box(Modifier.align(Alignment.BottomStart).padding(esc.d(5)).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = esc.d(6), vertical = esc.d(2))) {
                                Texto(c.fuente, 9.5f, color = Color.White, lineas = 1, traducir = false)
                            }
                            if (c.ancho > 0 && c.alto > 0 && e.candidatoPara == "portada") {
                                val r = c.ancho.toFloat() / c.alto
                                val etiqueta = when { r > 1.2f -> "Horizontal"; r > 0.85f -> "Cuadrada"; else -> null }
                                if (etiqueta != null) Box(Modifier.align(Alignment.TopEnd).padding(esc.d(5)).clip(RoundedCornerShape(50)).background(Uva).padding(horizontal = esc.d(6), vertical = esc.d(2))) {
                                    Texto(etiqueta, 9.5f, color = SobreUva, lineas = 1)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("A" to "Usar", "L1 R1" to "Fuente", "Y" to "Otro nombre", "B" to "Volver")
        }
    }
}


// ---------- Logros (RetroAchievements) ----------

/** Lista de logros del juego: los ganados a color, los que faltan en gris. */
@Composable
private fun ListaLogros(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val l = e.logrosFicha
    when {
        e.usuarioRA.isBlank() || e.claveRA.isBlank() -> Column(verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
            Texto("Conecta RetroAchievements", 18f)
            Detalle("Escribe tu usuario y tu clave en Menú › Logros para ver aquí los logros de cada juego, cuáles tienes y cuántos puntos llevas.", 14.5f)
            Box(Modifier.clip(RoundedCornerShape(50)).fondoBoton(Uva).clickable { Diptico.abrirMenu(cl.nikalbazu.diptico.estado.Seccion.LOGROS) }.padding(horizontal = esc.d(14), vertical = esc.d(8))) {
                Texto("Conectar mi cuenta", 14f, color = SobreUva)
            }
        }
        e.cargandoLogros -> Row(verticalAlignment = Alignment.CenterVertically) {
            Girador(20f)
            Spacer(Modifier.width(esc.d(10)))
            Texto("Buscando tus logros…", 16f)
        }
        l == null -> Detalle(e.errorLogros ?: "No encontré logros para este juego.", 15f)
        l.total == 0 -> Detalle("Este juego todavía no tiene logros en RetroAchievements.", 15f)
        else -> {
            val lista = rememberLazyListState()
            LaunchedEffect(e.indice) { lista.mostrar(e.indice) }
            Column {
                // Avance
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Texto("${l.ganados} de ${l.total}", 17f, color = p.acentoTexto)
                    Spacer(Modifier.width(esc.d(8)))
                    Detalle("${l.puntosGanados} / ${l.puntos} puntos", 13f, lineas = 1, modifier = Modifier.weight(1f))
                    Detalle("${(l.ganados * 100) / l.total.coerceAtLeast(1)}%", 13f)
                }
                Spacer(Modifier.height(esc.d(5)))
                Box(Modifier.fillMaxWidth().height(esc.d(8)).clip(RoundedCornerShape(50)).background(p.linea)) {
                    Box(Modifier.fillMaxWidth(l.ganados / l.total.toFloat().coerceAtLeast(1f)).height(esc.d(8)).clip(RoundedCornerShape(50)).fondoBoton(Sol))
                }
                Spacer(Modifier.height(esc.d(8)))
                LazyColumn(state = lista, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    itemsIndexed(l.logros, key = { _, x -> x.id }) { i, x -> FilaLogro(x, i == e.indice) { Diptico.elegirIndice(i) } }
                }
            }
        }
    }
}

@Composable
private fun FilaLogro(x: cl.nikalbazu.diptico.data.Logro, activo: Boolean, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val img = rememberImagen(Uri.parse(x.imagen), 128)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(14)))
            .background(if (activo) Uva.copy(alpha = 0.25f) else p.superficie.copy(alpha = 0.7f))
            .border(esc.d(if (activo) 2f else 0f), if (activo) Uva else Color.Transparent, RoundedCornerShape(esc.d(14)))
            .clickable { alPulsar() }.padding(esc.d(8)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(esc.d(44)).clip(RoundedCornerShape(esc.d(10))).background(p.fondo)) {
            if (img != null) Image(img, null, Modifier.fillMaxSize().alpha(if (x.conseguido) 1f else 0.6f))
        }
        Spacer(Modifier.width(esc.d(10)))
        Column(Modifier.weight(1f)) {
            Texto(x.titulo, 14.5f, lineas = 1, color = if (x.conseguido) p.tinta else p.tenue)
            Detalle(x.descripcion, 12.5f, lineas = 2)
        }
        Spacer(Modifier.width(esc.d(8)))
        Column(horizontalAlignment = Alignment.End) {
            Texto("${x.puntos}", 15f, color = if (x.conseguido) Sol else p.tenue)
            if (x.conseguido) Detalle(if (x.dificil) "★ difícil" else "✓", 11f, color = Sol)
        }
    }
}

/** En la otra pantalla: el logro elegido, en grande. */
@Composable
private fun LogroGrande(e: Estado, titulo: String) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val l = e.logrosFicha
    val x = l?.logros?.getOrNull(e.indice)
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Sol.copy(alpha = 0.18f), p.fondo), radius = 1400f))) {
        BarraSuperior(Modifier.align(Alignment.TopStart), trabajos = e.trabajos)
        Row(
            Modifier.fillMaxSize().padding(start = esc.d(36), end = esc.d(36), top = esc.d(60), bottom = esc.d(30)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (x != null) {
                val img = rememberImagen(Uri.parse(x.imagen), 256)
                Box(Modifier.size(esc.d(150)).clip(RoundedCornerShape(esc.d(28))).background(p.superficie), contentAlignment = Alignment.Center) {
                    if (img != null) Image(img, null, Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(esc.d(28)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
                    Etiqueta("Logros · $titulo", Sol, 12.5f)
                    Texto(x.titulo, 30f, lineas = 2, interlineado = 34f)
                    Detalle(x.descripcion, 17f, lineas = 4)
                    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                        Chip("${x.puntos} puntos", Sol)
                        Chip(if (x.conseguido) "Ganado el ${x.ganado}" else "Te falta", if (x.conseguido) Uva else p.tenue)
                    }
                    if (l.total > 0) Detalle("Llevas ${l.ganados} de ${l.total} logros en este juego.", 14f)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
                    Etiqueta("Logros · $titulo", Sol, 12.5f)
                    Texto("RetroAchievements", 32f)
                    Detalle(
                        if (e.usuarioRA.isBlank()) "Conecta tu cuenta en Menú › Logros para ver tus logros de cada juego."
                        else e.errorLogros ?: if (e.cargandoLogros) "Buscando tus logros…" else "Elige un logro para verlo en grande.",
                        17f,
                    )
                }
            }
        }
    }
}

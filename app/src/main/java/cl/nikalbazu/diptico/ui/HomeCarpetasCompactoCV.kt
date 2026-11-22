package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Home real de Colección viva para UNA pantalla, inspirado en el boceto aprobado.
 * No modifica CarpetasArribaCV ni CarpetasAbajoCV de las consolas de dos pantallas.
 */
@Composable
fun CarpetasUnicaCompactaCV(e: Estado) {
    val seleccionada = Consolas.porId(e.consolaSel) ?: e.consolasConJuegos.firstOrNull()
    LienzoBoceto(1920f, 1080f, fondo = LocalColeccion.current.fondo, zoom = ZOOM_UNA) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val color = seleccionada?.color ?: col.acento
        // El rótulo de emblema del fondo se reubica encima de la consola principal.
        FondoConsolaCV(e, seleccionada, Modifier.fillMaxSize(), mostrarEtiqueta = false)
        val conArte = rememberFondo(e, seleccionada).imagen != null
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(color.copy(alpha = if (conArte) .14f else .34f), Color.Transparent),
                    radius = 1550f,
                ),
            ),
        )
        FondoTemaCV()

        // Reserva lateral para el dock fijo: el carrusel y el arte no quedan debajo de las apps.
        Column(
            Modifier.fillMaxSize().padding(start = esc.d(50), end = esc.d(112), top = esc.d(18), bottom = esc.d(28)),
            verticalArrangement = Arrangement.spacedBy(esc.d(14)),
        ) {
            Row(
                Modifier.fillMaxWidth().height(esc.d(106)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DippoConsejosEsquinaCV(e, Modifier.anclaRecorrido("dippo"))
            }

            if (seleccionada == null) {
                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    TB("Tu biblioteca está lista para empezar", 54f, titulo = true)
                    TB("Agrega una carpeta de juegos desde los ajustes de Biblioteca.", 25f, color = col.sub)
                    BotonTecla("A", "Abrir biblioteca", { Diptico.abrirMenu() }, alto = 70f)
                }
            } else {
                // El texto vive a la izquierda; el modelo tiene su columna propia y centrada.
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(esc.d(34)),
                ) {
                    Column(
                        Modifier.weight(.47f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(esc.d(16), Alignment.CenterVertically),
                    ) {
                        FichaConsolaPortadaCV(e, seleccionada)
                    }
                    Column(
                        Modifier.weight(.53f).fillMaxHeight().testTag("consola-destacada-cv"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(esc.d(8)),
                    ) {
                        MasJugadoArribaCV(e, seleccionada)
                        Box(Modifier.fillMaxWidth().weight(1f)) {
                            Consola3D(seleccionada, Modifier.fillMaxSize().padding(horizontal = esc.d(12)))
                        }
                        Modelos3D.credito(seleccionada.id)?.let {
                            TB(it, 15f, color = col.tenue, lineas = 1, traducir = false)
                        }
                        // Las acciones se trasladan a una fila común sobre "CONSOLAS".

                    }
                }

                // Acción principal inmediatamente encima del encabezado del selector.
                Row(
                    Modifier.fillMaxWidth().height(esc.d(72))
                        .anclaRecorrido("controles").testTag("acciones-consola-cv"),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // A · Entrar queda al costado izquierdo, justo encima de «CONSOLAS».
                    Box(Modifier.testTag("entrar-izquierda-cv"), contentAlignment = Alignment.CenterStart) {
                        BotonTecla(
                            "A", "Entrar", { Diptico.entrarConsolaCV(seleccionada.id) },
                            alto = 70f, principal = true, letra = 25f,
                        )
                    }
                    ultimoDeConsola(e, seleccionada.id)?.let { ultimo ->
                        Spacer(Modifier.width(esc.d(14)))
                        BotonTecla(
                            "Y", "Seguir con ${ultimo.titulo}", { Diptico.seguirCon(ultimo) },
                            alto = 70f, letra = 20f,
                        )
                    }
                }
                // Selector centrado con énfasis en la tarjeta activa.
                SelectorCarpetasCompactoCV(e, seleccionada.id, Modifier.fillMaxWidth().height(esc.d(184)))
            }
        }
        // Dock permanente en el costado derecho, independiente de la navegación por consolas.
        DockLateralCompactoCV(
            e,
            Modifier.align(Alignment.CenterEnd).padding(end = esc.d(14)).anclaRecorrido("apps"),
        )
        // Una sola fila flotante: utilidades pegadas a la foto de perfil sin hueco intermedio.
        Row(
            Modifier.align(Alignment.TopEnd)
                .padding(top = esc.d(13), end = esc.d(8))
                .height(esc.d(88))
                .testTag("utilidades-perfil-cv"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(12)),
        ) {
            Box(
                Modifier.size(esc.d(64)).testTag("recargar-alineado-cv"),
                contentAlignment = Alignment.Center,
            ) { BotonRecargarJuegos(e, 56f) }
            Box(Modifier.height(esc.d(68)), contentAlignment = Alignment.Center) {
                HoraBateria(58f, 23f)
            }
            Box(Modifier.size(esc.d(68)), contentAlignment = Alignment.Center) {
                BotonRedondo(Icono.AJUSTES, 60f, { Diptico.abrirMenu() }, descripcion = "Ajustes")
            }
            Box(
                Modifier.anclaRecorrido("perfil").testTag("perfil-esquina-cv"),
                contentAlignment = Alignment.Center,
            ) { BotonPerfil(76f) }
        }
    }
}

/** La ficha compacta deja todo el espacio libre bajo el título y mantiene la personalización en un engranaje. */
@Composable
private fun FichaConsolaPortadaCV(e: Estado, c: Consola) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val historia = HistoriaConsolas.de(c.id)
    val juegos = e.juegosPorConsola[c.id].orEmpty()
    val tiempo = juegos.sumOf { e.datos[it.clave]?.tiempo ?: 0L }
    val nombre = e.consolasPropias[c.id]?.nombre ?: c.bonito
    val conLogo = c.id in e.extra.logoConsolas
    val uri = e.consolasPropias[c.id]?.logo?.let { android.net.Uri.parse(it) }
    val logo = if (conLogo) rememberImagen(uri, 800) else null

    Column(
        Modifier.fillMaxWidth().anclaRecorrido("consola").testTag("ficha-consola-cv"),
        verticalArrangement = Arrangement.spacedBy(esc.d(18)),
    ) {
        if (historia != null) {
            TB("${historia.marca} · ${historia.anio} · ${historia.generacion}", 21f,
                color = androidx.compose.ui.graphics.lerp(c.color, Color.White, .55f),
                peso = FontWeight.ExtraBold, espaciado = 2.1f, lineas = 1)
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(16)),
        ) {
            if (logo != null) {
                Image(logo, contentDescription = nombre,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.weight(1f).height(esc.d(95)))
            } else {
                TB(nombre, 65f, modifier = Modifier.weight(1f), titulo = true,
                    lineas = 2, interlineado = 64f, traducir = false)
            }
            Box(
                Modifier.size(esc.d(86)).clip(CircleShape)
                    .background(c.color.copy(alpha = .27f))
                    .border(esc.d(3), col.acento.copy(alpha = .95f), CircleShape)
                    .testTag("personalizar-consola-cv"),
                contentAlignment = Alignment.Center,
            ) {
                BotonRedondo(
                    Icono.AJUSTES, 72f,
                    { Diptico.abrirEditarConsola(c.id) },
                    descripcion = "Personalizar ${c.bonito}",
                )
            }
        }
        if (historia != null) {
            TB(historia.historia, 22f, color = col.sub,
                peso = FontWeight.SemiBold, lineas = 3, interlineado = 31f)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
            ChipDato("${juegos.size} ${if (juegos.size == 1) "juego" else "juegos"}", 20f)
            if (tiempo > 0) ChipDato("${formatoTiempo(tiempo)} jugadas", 20f)
        }
    }
}

/** DIPPO es el pulpo; solo él puede originar sus consejos efímeros. */
@Composable
private fun DippoConsejosEsquinaCV(e: Estado, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val coloresDippo = remember(e.extra.dippoColor) { coloresDippoAhora(e.extra.dippoColor) }
    var visible by remember { mutableStateOf(false) }
    var indice by remember { mutableStateOf(0) }
    val consejos = listOf(
        "Pulsa A para entrar a la carpeta seleccionada.",
        "Usa L1 y R1 para cambiar de consola.",
        "Desliza las consolas para recorrer tus carpetas.",
        "Toca una carpeta dos veces para entrar directamente.",
        "Mantén Select para intercambiar pantallas, si tienes dos.",
    )
    LaunchedEffect(e.extra.dippoInicio, e.consolaSel) {
        if (!e.extra.dippoInicio) { visible = false; return@LaunchedEffect }
        delay(550)
        while (true) {
            visible = true
            delay(5_500)
            visible = false
            indice = (indice + 1) % consejos.size
            delay(72_000)
        }
    }
    if (!e.extra.dippoInicio) {
        Box(modifier.testTag("dippo-oculto"))
        return
    }
    Row(
        modifier.testTag("dippo-esquina-cv"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(esc.d(2)),
    ) {
        Box(
            Modifier.size(esc.d(82)).clip(CircleShape)
                .clickable(onClickLabel = "Hablar con Dippo") { Diptico.abrirAsistente() }
                .testTag("dippo-pulpo-cv"),
            contentAlignment = Alignment.Center,
        ) {
            // Sin placa ni burbuja alrededor: el pulpo flota libre, tal como el diseño.
            Canvas(Modifier.fillMaxSize()) {
                dibujarDippo(
                    size.width / 24f,
                    coloresDippo,
                    if (visible) CaraDippo.FELIZ else CaraDippo.NORMAL,
                    saludo = if (visible) 0.8f else 0f,
                )
            }
        }
        AnimatedVisibility(
            visible,
            enter = fadeIn(tween(230)) + expandHorizontally(tween(230)),
            exit = fadeOut(tween(210)) + shrinkHorizontally(tween(210)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TB("◀", 24f, color = col.acento, traducir = false)
                Row(
                    Modifier.width(esc.d(590)).height(esc.d(72))
                        .vidrio(30f, fuerte = true)
                        .border(1.dp, col.acento.copy(alpha = .7f), RoundedCornerShape(esc.d(30)))
                        .clickable(onClickLabel = "Abrir consejo de Dippo") { Diptico.abrirAsistente() }
                        .padding(horizontal = esc.d(20)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(esc.d(10)),
                ) {
                    TB("Dippo:", 19f, color = col.acento, peso = FontWeight.ExtraBold)
                    TB(consejos[indice], 20f, modifier = Modifier.weight(1f), lineas = 2, traducir = false)
                }
            }
        }
    }
}

/** Rótulo contextual real: aparece sobre el modelo, jamás encima de la descripción. */
@Composable
private fun MasJugadoArribaCV(e: Estado, c: Consola) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val mejor = remember(c.id, e.datos, e.juegosPorConsola[c.id]) {
        e.juegosPorConsola[c.id].orEmpty()
            .maxByOrNull { e.datos[it.clave]?.tiempo ?: 0L }
            ?.takeIf { (e.datos[it.clave]?.tiempo ?: 0L) > 0L }
    }
    if (mejor == null) return
    Row(
        Modifier.height(esc.d(48)).fillMaxWidth(.85f)
            .vidrio(30f, fuerte = true).testTag("tu-mas-jugado-cv")
            .padding(horizontal = esc.d(20)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(esc.d(15)),
    ) {
        TB("TU MÁS JUGADO", 18f, color = col.acento, peso = FontWeight.ExtraBold)
        TB(mejor.titulo, 19f, modifier = Modifier.weight(1f), lineas = 1, traducir = false)
    }
}

/** Solo imágenes locales de las consolas, cargadas fuera del hilo principal. */
@Composable
private fun IconoCarpetaCompactoCV(e: Estado, c: Consola) {
    val ctx = LocalContext.current
    val id = remember(c.id) { Modelos3D.idDe(c.id) }
    var imagen by remember(id) { mutableStateOf(ImagenesConsola.enMemoria(id)) }
    LaunchedEffect(id) {
        if (imagen == null && ImagenesConsola.hay(ctx, id)) {
            imagen = withContext(Dispatchers.IO) { ImagenesConsola.cargar(ctx, id) }
        }
    }
    val portadaPropia = e.consolasPropias[c.id]?.portadaCarpeta
    if (portadaPropia != null) {
        PortadaCarpetaRuta(portadaPropia, c.id, Modifier.fillMaxSize(.9f), maxLado = 360)
    } else if (imagen != null) {
        Image(imagen!!, contentDescription = c.bonito, modifier = Modifier.fillMaxSize(.91f), contentScale = ContentScale.Fit)
    } else {
        IconoConsola(c, 64f, c.color.copy(alpha = .2f))
    }
}

/** Carrusel compacto: desplazar con dedo, elegir con toque, entrar con un segundo toque o A. */
@Composable
private fun SelectorCarpetasCompactoCV(e: Estado, seleccionadaId: String, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val ids = remember(e.pestanas) { e.pestanas.filter { it != "todo" && Consolas.porId(it) != null } }
    val indice = ids.indexOf(seleccionadaId).coerceAtLeast(0)
    SideEffect { Teclas.carrilesCarpetas = 1 }

    Column(
        modifier.testTag("selector-carpetas-compacto-cv"),
        verticalArrangement = Arrangement.spacedBy(esc.d(9)),
    ) {
        Row(
            Modifier.fillMaxWidth().height(esc.d(35)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(12)),
        ) {
            IconoDiptico(Icono.MANDO, col.texto, 25f)
            TB("CONSOLAS", 25f, peso = FontWeight.ExtraBold, espaciado = 2f)
            Spacer(Modifier.weight(1f))
            TB("L1 / R1 para cambiar", 18f, color = col.sub)
            BotonRedondo(Icono.BUSCAR, 38f, { Diptico.abrirBuscar() }, descripcion = "Buscar juegos")
        }
        Row(
            Modifier.fillMaxWidth().weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(9)),
        ) {
            FlechaSelectorCV("‹", {
                if (ids.isNotEmpty()) Diptico.elegirConsola(ids[(indice - 1 + ids.size) % ids.size])
            }, descripcion = "Consola anterior")
            // Cinco posiciones estables (-2…+2); la elegida ocupa SIEMPRE el centro.
            // A diferencia de desplazar una LazyRow al final, esto nunca deja la
            // consola seleccionada pegada al borde cuando hay pocas/muchas carpetas.
            Row(
                Modifier.weight(1f).fillMaxHeight()
                    .testTag("carrusel-carpetas-cv")
                    .pointerInput(ids, seleccionadaId) {
                        var arrastre = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { arrastre = 0f },
                            onHorizontalDrag = { change, dx ->
                                arrastre += dx
                                change.consume()
                            },
                            onDragEnd = {
                                if (ids.isNotEmpty() && kotlin.math.abs(arrastre) >= size.width * .055f) {
                                    val movimiento = if (arrastre < 0f) 1 else -1
                                    Diptico.elegirConsola(ids[(indice + movimiento + ids.size) % ids.size])
                                }
                                arrastre = 0f
                            },
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(esc.d(14), Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val anchoTarjeta = esc.d(148f)
                val visibles = when (ids.size) {
                    0 -> emptySet<Int>()
                    1 -> setOf(0)
                    2 -> setOf(0, 1)
                    3 -> setOf(-1, 0, 1)
                    4 -> setOf(-2, -1, 0, 1)
                    else -> setOf(-2, -1, 0, 1, 2)
                }
                for (posicion in -2..2) {
                    if (posicion !in visibles || ids.isEmpty()) {
                        Spacer(Modifier.width(anchoTarjeta))
                    } else {
                        val id = ids[(indice + posicion + ids.size) % ids.size]
                        val c = Consolas.porId(id)
                        if (c == null) {
                            Spacer(Modifier.width(anchoTarjeta))
                        } else {
                            val elegida = posicion == 0
                            val forma = RoundedCornerShape(esc.d(23))
                            val nombreCorto = e.consolasPropias[id]?.nombre?.takeIf { it.isNotBlank() } ?: when (id.lowercase()) {
                                "switch" -> "Switch"
                                "ps2" -> "PS2"
                                "gba" -> "GBA"
                                "snes" -> "SNES"
                                "android" -> "Android"
                                else -> c.bonito
                            }
                            val escala by animateFloatAsState(
                                targetValue = if (elegida) 1.23f else .86f,
                                animationSpec = tween(220),
                                label = "Destacar consola seleccionada",
                            )
                            Box(
                                Modifier.width(anchoTarjeta).fillMaxHeight(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().fillMaxHeight(.86f)
                                        .graphicsLayer {
                                            scaleX = escala
                                            scaleY = escala
                                        }
                                        .clip(forma)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    c.color.copy(alpha = if (elegida) .85f else .50f),
                                                    col.fondo.copy(alpha = .85f),
                                                ),
                                            ),
                                        )
                                        .then(
                                            if (elegida) Modifier.border(esc.d(3), col.elegido, forma)
                                            else Modifier.border(1.dp, col.borde, forma),
                                        )
                                        .clickable(onClickLabel = if (elegida) "Entrar a ${c.bonito}" else "Elegir ${c.bonito}") {
                                            if (elegida) Diptico.entrarConsolaCV(id) else Diptico.elegirConsola(id)
                                        }
                                        .testTag("carpeta-cv-$id")
                                        .padding(esc.d(8)),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(esc.d(4)),
                                ) {
                                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                        IconoCarpetaCompactoCV(e, c)
                                    }
                                    TB(nombreCorto, 17f, peso = FontWeight.ExtraBold, lineas = 1, traducir = false)
                                }
                            }
                        }
                    }
                }
            }
            FlechaSelectorCV("›", {
                if (ids.isNotEmpty()) Diptico.elegirConsola(ids[(indice + 1) % ids.size])
            }, descripcion = "Consola siguiente")

        }
    }
}

/** Flechas para cambiar de carpeta sin alterar el mando (L1/R1 y A siguen activos). */
@Composable
private fun FlechaSelectorCV(simbolo: String, accion: () -> Unit, descripcion: String) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Box(
        Modifier.size(esc.d(52)).clip(CircleShape)
            .background(col.vidrioFuerte).border(1.dp, col.borde, CircleShape)
            .clickable(onClickLabel = descripcion, onClick = accion),
        contentAlignment = Alignment.Center,
    ) { TB(simbolo, 46f, color = col.texto, traducir = false) }
}

/** Dock fijo: mantiene las aplicaciones visibles en el lateral derecho sin pestaña que se cierre. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DockLateralCompactoCV(e: Estado, modifier: Modifier = Modifier) {
    if (!e.extra.dock) return
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val vista = LocalView.current
    Column(
        modifier.width(esc.d(84)).vidrio(35f, fuerte = true)
            .padding(horizontal = esc.d(8), vertical = esc.d(13))
            .testTag("dock-lateral-cv"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(esc.d(12)),
    ) {
        e.appsFijadas.take(MAX_DOCK).forEach { pkg ->
            Box(
                Modifier.size(esc.d(60)).clip(RoundedCornerShape(esc.d(14)))
                    .combinedClickable(
                        onClickLabel = "Abrir app",
                        onLongClickLabel = "Quitar del dock",
                        onLongClick = { Diptico.alternarFijada(pkg) },
                    ) { Diptico.abrirPaqueteAqui(pkg, vista.display?.displayId) },
                contentAlignment = Alignment.Center,
            ) { IconoApp(pkg, 58f, desnudo = true) }
        }
        if (e.appsFijadas.size < MAX_DOCK) {
            Box(
                Modifier.size(esc.d(58)).clip(CircleShape)
                    .border(1.dp, col.borde, CircleShape)
                    .clickable(onClickLabel = "Agregar app al dock") { Diptico.abrirAppsParaFijar() },
                contentAlignment = Alignment.Center,
            ) { TB("+", 40f, traducir = false) }
        }
        BotonRedondo(Icono.APPS, 58f, { Diptico.abrirApps() }, descripcion = "Todas las apps")
    }
}

package cl.nikalbazu.diptico.ui

import cl.nikalbazu.diptico.estado.*

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Teclas

/**
 * Vista «Mosaicos» (estilo Xbox): el primer juego en un mosaico grande que ocupa dos filas y el resto en
 * mosaicos cuadrados de a dos por columna, que corren hacia el lado. El elegido lleva borde blanco y un halo
 * del color de su consola, para que siempre se note dónde estás. Liviana: sin vidrio ni sombras animadas.
 */
@Composable
fun Mosaicos(e: Estado, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val lista = e.visibles
    val estado = rememberLazyGridState()
    val margen = with(androidx.compose.ui.platform.LocalDensity.current) { esc.d(40).roundToPx() }
    SideEffect { Teclas.columnasCuadricula = 2 }
    LaunchedEffect(e.indiceSel, lista.size) {
        if (lista.isNotEmpty()) estado.animateScrollToItem(e.indiceSel.coerceIn(0, lista.lastIndex), -margen)
    }
    // Al cambiar de consola, los mosaicos se deslizan y funden hacia el lado de la consola nueva.
    val mov = ajusteMovimiento()
    androidx.compose.animation.AnimatedContent(
        targetState = e.consolaSel,
        transitionSpec = {
            val ida = if (e.pestanas.indexOf(targetState) >= e.pestanas.indexOf(initialState)) 1 else -1
            if (!mov.activo) androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
            else (androidx.compose.animation.fadeIn(tween(mov.ms(220))) + androidx.compose.animation.slideInHorizontally(tween(mov.ms(300), easing = CurvaPosarse)) { ida * it / 8 }) togetherWith
                (androidx.compose.animation.fadeOut(tween(mov.ms(140))) + androidx.compose.animation.slideOutHorizontally(tween(mov.ms(200))) { -ida * it / 12 })
        },
        label = "mosaicos-consola", modifier = modifier,
    ) { _ ->
    BoxWithConstraints(Modifier.fillMaxSize().testTag("vista-mosaicos")) {
        if (lista.isEmpty()) { VacioEstante(e); return@BoxWithConstraints }
        val hueco = esc.d(10)
        // La zona ya descuenta la barra de abajo (padding en VistaJuegos): maxHeight es lo que queda a la vista.
        val alto = maxHeight - esc.d(24)
        // Con poco alto (una pantalla, con la vitrina arriba) va una sola fila de mosaicos más grandes.
        // Con la vitrina de cajas arriba, abajo va una sola fila en el mismo orden que el estante: avanzar es +1, sin saltos.
        val estante = e.portadaCajas && nuevoDisenoDosPantallas(e)
        val unaFila = alto < esc.d(200) || estante
        SideEffect { Teclas.mosaicosUnaFila = unaFila }
        val lado: Dp = when {
            estante -> alto.coerceAtMost(esc.d(170)).coerceAtLeast(esc.d(60))
            unaFila -> alto.coerceAtLeast(esc.d(60))
            else -> ((alto - hueco) / 2).coerceAtLeast(esc.d(60))
        }
        LazyHorizontalGrid(
            rows = GridCells.Fixed(if (unaFila) 1 else 2), state = estado, modifier = Modifier.fillMaxSize(),
            contentPadding = run {
                // La zona ya viene con el aire de la barra de abajo (VistaJuegos): aquí solo se centra la fila.
                val arriba = if (estante) ((maxHeight - lado) / 2).coerceAtLeast(esc.d(12)) else esc.d(12)
                PaddingValues(start = esc.d(18), end = esc.d(18), top = arriba, bottom = arriba)
            },
            horizontalArrangement = Arrangement.spacedBy(hueco), verticalArrangement = Arrangement.spacedBy(hueco),
        ) {
            items(lista.size, key = { lista[it].clave }, span = { i -> GridItemSpan(if (i == 0) maxLineSpan else 1) }) { i ->
                val grande = i == 0
                val juego = lista[i]
                val elegido = i == e.indiceSel
                val jugado = (e.datos[juego.clave]?.tiempo?.toLong() ?: 0L) > 0L
                when {
                    estante -> Mosaico(juego, elegido, jugado, false, lado, ancho = lado * 0.78f)
                    unaFila -> Mosaico(juego, elegido, jugado, grande, lado, ancho = if (grande) lado * 1.7f else lado)
                    grande -> Mosaico(juego, elegido, jugado, true, lado * 2 + hueco)
                    else -> Mosaico(juego, elegido, jugado, false, lado)
                }
            }
        }
    }
    }
}

@Composable
private fun Mosaico(j: cl.nikalbazu.diptico.data.Juego, elegido: Boolean, jugado: Boolean, grande: Boolean, lado: Dp, ancho: Dp = lado) {
    val esc = LocalEscala.current
    val color = remember(j.consolaId) { Consolas.porId(j.consolaId)?.color ?: Uva }
    // El elegido crece con un resorte corto (sin rebote largo) y vuelve suave: se siente como consola.
    val mov = ajusteMovimiento()
    val escala by animateFloatAsState(
        if (elegido) 1.05f else 1f,
        if (mov.activo) androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow / mov.duracion.coerceAtLeast(0.3f))
        else androidx.compose.animation.core.snap(),
        label = "mosaico",
    )
    val forma = RoundedCornerShape(esc.d(if (grande) 14 else 10))
    Box(
        Modifier.size(ancho, lado).graphicsLayer { scaleX = escala; scaleY = escala }
            .then(if (elegido) Modifier.shadow(esc.d(18), forma, ambientColor = color, spotColor = color) else Modifier)
            .clip(forma)
            .background(color.copy(alpha = 0.25f))
            .then(if (elegido) Modifier.border(esc.d(3), Color.White, forma) else Modifier)
            .clickable { if (elegido) Diptico.jugar() else Diptico.elegirJuego(j.clave) },
    ) {
        ArteJuego(j, preferirArte = grande, maxLado = if (grande) 640 else 320, modifier = Modifier.fillMaxSize(), imagen = if (grande) "arte" else "portada")
        if (!elegido && !grande) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
                .padding(horizontal = esc.d(if (grande) 12 else 7), vertical = esc.d(if (grande) 10 else 5)),
        ) {
            if (grande && jugado) Texto("Continuar", 10.5f, color = Color.White.copy(alpha = 0.75f), lineas = 1)
            Texto(j.titulo, if (grande) 15f else 10.5f, color = Color.White, lineas = if (grande) 2 else 1, traducir = false)
        }
    }
}

/**
 * Franja de abajo del nuevo diseño: accesos fijos (Buscar, Apps, Ajustes) a la izquierda y la hora con la batería a
 * la derecha. Reemplaza la barra de la hora en la pantalla de abajo, así no se repite ni tapa nada.
 */
/** Alto de la barra de abajo abierta (barrita + accesos) y cerrada (solo la barrita), en dp de Díptico. */
const val ALTO_FRANJA_ABIERTA = 56f
const val ALTO_FRANJA_CERRADA = 22f

/** Cuánto tapa la barra flotante de abajo: las rejillas lo suman a su relleno para poder correr el último elemento por encima. */
val LocalAireAbajo = androidx.compose.runtime.compositionLocalOf { 0.dp }

@Composable
fun FranjaAccesos(e: Estado, modifier: Modifier = Modifier) {
    if (!e.extra.franjaAbajo) return
    val esc = LocalEscala.current
    // Solo la lupa y los ajustes, pegados a las esquinas de abajo, cada uno en una pestaña semiredonda para que se note.
    // Las apps fijadas (dock) ahora viven arriba, en EsquinasArriba.
    androidx.compose.foundation.layout.Row(
        modifier.fillMaxWidth().testTag("franja-accesos"),
        verticalAlignment = Alignment.Bottom,
    ) {
        CirculoFranja(cl.nikalbazu.diptico.estado.Icono.BUSCAR, izquierda = true) { Diptico.abrirBuscar() }
        if (dockArriba(e)) Spacer(Modifier.weight(1f))
        else Box(Modifier.weight(1f).padding(horizontal = esc.d(8), vertical = esc.d(4)).testTag("franja-deslizable"), contentAlignment = Alignment.Center) {
            // En las vistas sin cabecera arriba (juegos de una consola, mosaicos…) las apps fijadas siguen abajo, entre la lupa y los ajustes.
            androidx.compose.foundation.layout.Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                DockOApps(e)
            }
        }
        CirculoFranja(cl.nikalbazu.diptico.estado.Icono.AJUSTES, izquierda = false) { Diptico.abrirMenu() }
    }
}

/** Acceso de las esquinas de la franja (lupa, ajustes): solo el ícono, flotando, con una sombra suave para leerse sobre cualquier fondo. */
@Composable
private fun CirculoFranja(icono: cl.nikalbazu.diptico.estado.Icono, izquierda: Boolean, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    // Pestaña de esquina: el lado de afuera queda recto contra el borde de la pantalla y el de adentro, redondeado.
    val forma = if (izquierda) RoundedCornerShape(topEnd = esc.d(26)) else RoundedCornerShape(topStart = esc.d(26))
    Box(
        Modifier.size(esc.d(64), esc.d(52))
            .clip(forma)
            .background(p.superficie.copy(alpha = 0.86f))
            .border(esc.d(1), p.linea, forma)
            .clickable(onClick = alPulsar),
        contentAlignment = if (izquierda) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Box(Modifier.padding(horizontal = esc.d(14)), contentAlignment = Alignment.Center) { IconoDiptico(icono, p.tinta, 22f) }
    }
}

/** Arriba de la pantalla de abajo: DIPPO en una esquina, tu perfil en la otra y la hora y la batería al centro. */
@Composable
fun EsquinasArriba(e: Estado) {
    val esc = LocalEscala.current
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(start = esc.d(10), end = esc.d(14), top = esc.d(4), bottom = esc.d(2)).testTag("esquinas-arriba"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // DIPPO grande en su esquina; a la derecha (de afuera hacia adentro): batería, hora y tu foto.
        Box(Modifier.size(esc.d(56)), contentAlignment = Alignment.Center) { DippoInicio(e, 52f) }
        // Tus apps fijadas, arriba al medio (antes iban abajo, tapando los juegos).
        Box(Modifier.weight(1f).padding(horizontal = esc.d(8)).testTag("franja-deslizable"), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Row(
                Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockOApps(e)
            }
        }
        // El título de tu perfil solo va en la pantalla de arriba.
        BotonPerfil(38f)
        Spacer(Modifier.width(esc.d(12)))
        HoraBateria()
    }
}

@Composable
private fun HoraBateria() {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val hora = rememberHora()
    val bateria = rememberBateria()
    val cargando = rememberCargando()
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Texto(hora, 13f, color = p.tinta, traducir = false, lineas = 1)
        androidx.compose.foundation.layout.Spacer(Modifier.size(esc.d(6)))
        val tono = when { cargando -> Color(0xFF38BDF8); bateria in 0..15 -> Color(0xFFF87171); else -> p.tinta }
        Box(Modifier.size(esc.d(22), esc.d(11)).border(esc.d(1.5f), p.tinta.copy(alpha = 0.7f), RoundedCornerShape(esc.d(3))).padding(esc.d(2))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(((if (bateria < 0) 50 else bateria) / 100f).coerceIn(0.08f, 1f))
                .clip(RoundedCornerShape(esc.d(1.5f))).background(tono))
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(esc.d(3)))
        Texto(if (bateria >= 0) "$bateria%" else "—", 13f, color = p.tinta, traducir = false, lineas = 1)
    }
}

/** Nuevo diseño (vista Mosaicos) con las dos pantallas: arriba solo la vitrina, sin barras repetidas. */
internal fun nuevoDisenoDosPantallas(e: Estado): Boolean = e.vista == "mosaicos" && !e.unaPantalla && e.dosDisponibles

/** Los dos botones de la vitrina de arriba: A para jugar (o continuar) y X para la ficha del juego. */
@Composable
fun BotonesVitrina(e: Estado, juego: cl.nikalbazu.diptico.data.Juego, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val jugado = (e.datos[juego.clave]?.tiempo?.toLong() ?: 0L) > 0L
    androidx.compose.foundation.layout.Row(modifier.testTag("botones-vitrina"), horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
        BotonVitrina("A", if (jugado) "Continuar" else "Jugar", lleno = true) { Diptico.jugar() }
        BotonVitrina("X", "Ficha del juego", lleno = false) { Diptico.abrirFicha() }
    }
}

@Composable
internal fun BotonVitrina(letra: String, texto: String, lleno: Boolean, alPulsar: () -> Unit) {
    val esc = LocalEscala.current
    val forma = RoundedCornerShape(50)
    val tinta = if (lleno) Color(0xFF15121D) else Color.White
    androidx.compose.foundation.layout.Row(
        Modifier.height(esc.d(46)).clip(forma)
            .background(if (lleno) Color.White else Color.White.copy(alpha = 0.12f))
            .then(if (lleno) Modifier else Modifier.border(esc.d(1), Color.White.copy(alpha = 0.25f), forma))
            .clickable(onClick = alPulsar).padding(horizontal = esc.d(20)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(esc.d(22)).clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (lleno) tinta else Color.Transparent)
            .then(if (lleno) Modifier else Modifier.border(esc.d(1.5f), tinta, androidx.compose.foundation.shape.CircleShape)),
            contentAlignment = Alignment.Center) {
            Texto(letra, 11f, color = if (lleno) Color.White else tinta, traducir = false, lineas = 1)
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(esc.d(10)))
        Texto(texto, 15f, color = tinta, lineas = 1)
    }
}

/** La lista de carpetas de consolas lleva el dock arriba (en su cabecera); en las demás vistas queda abajo. */
internal fun dockArriba(e: Estado): Boolean = e.vistaConsolas == "carpetas" && e.enSistemas && !e.carpetaEnVentana && !e.extra.ordenandoCarpetas

/** Apps fijadas (o el botón «Apps» si el dock está apagado). */
@Composable
private fun DockOApps(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    if (e.extra.dock) FilaDock(e, 32f, flotante = true)
    else androidx.compose.foundation.layout.Row(
        Modifier.clip(RoundedCornerShape(50)).clickable { Diptico.abrirApps() }.padding(horizontal = esc.d(8), vertical = esc.d(8)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconoDiptico(cl.nikalbazu.diptico.estado.Icono.APPS, p.tinta, 15f)
        Spacer(Modifier.width(esc.d(8)))
        Texto("Apps", 13f, color = p.tinta, lineas = 1)
    }
}

package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
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
        val alto = maxHeight - esc.d(24)
        // Con poco alto (una pantalla, con la vitrina arriba) va una sola fila de mosaicos más grandes.
        val unaFila = alto < esc.d(200)
        SideEffect { Teclas.mosaicosUnaFila = unaFila }
        val lado: Dp = if (unaFila) alto.coerceAtLeast(esc.d(60)) else ((alto - hueco) / 2).coerceAtLeast(esc.d(60))
        LazyHorizontalGrid(
            rows = GridCells.Fixed(if (unaFila) 1 else 2), state = estado, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = esc.d(18), vertical = esc.d(12)),
            horizontalArrangement = Arrangement.spacedBy(hueco), verticalArrangement = Arrangement.spacedBy(hueco),
        ) {
            items(lista.size, key = { lista[it].clave }, span = { i -> GridItemSpan(if (i == 0) maxLineSpan else 1) }) { i ->
                val grande = i == 0
                when {
                    unaFila -> Mosaico(e, i, grande, lado, ancho = if (grande) lado * 1.7f else lado)
                    grande -> Mosaico(e, i, true, lado * 2 + hueco)
                    else -> Mosaico(e, i, false, lado)
                }
            }
        }
    }
    }
}

@Composable
private fun Mosaico(e: Estado, i: Int, grande: Boolean, lado: Dp, ancho: Dp = lado) {
    val esc = LocalEscala.current
    val j = e.visibles[i]
    val elegido = i == e.indiceSel
    val color = Consolas.porId(j.consolaId)?.color ?: Uva
    // El elegido crece con un resorte corto (sin rebote largo) y vuelve suave: se siente como consola.
    val mov = ajusteMovimiento()
    val escala by animateFloatAsState(
        if (elegido) 1.05f else 1f,
        if (mov.activo) androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow / mov.duracion.coerceAtLeast(0.3f))
        else androidx.compose.animation.core.snap(),
        label = "mosaico",
    )
    val forma = RoundedCornerShape(esc.d(if (grande) 14 else 10))
    val jugado = (e.datos[j.clave]?.tiempo?.toLong() ?: 0L) > 0L
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
@Composable
fun FranjaAccesos(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val hora = rememberHora()
    val bateria = rememberBateria()
    val cargando = rememberCargando()
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().background(p.superficie.copy(alpha = 0.6f)).padding(horizontal = esc.d(12), vertical = esc.d(8)).testTag("franja-accesos"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(esc.d(6)),
    ) {
        // Con el dock, Buscar y Ajustes quedan como íconos para dejarle espacio a tus apps.
        val conDock = e.extra.dock
        BotonFranja(cl.nikalbazu.diptico.estado.Icono.BUSCAR, if (conDock) null else "Buscar") { Diptico.abrirBuscar() }
        BotonFranja(cl.nikalbazu.diptico.estado.Icono.AJUSTES, if (conDock) null else "Ajustes") { Diptico.abrirMenu() }
        if (conDock) {
            Box(Modifier.size(esc.d(1), esc.d(26)).background(p.tinta.copy(alpha = 0.15f)))
            // Las apps se esconden y se sacan con un botoncito: la franja queda limpia cuando no las usas.
            androidx.compose.animation.AnimatedVisibility(
                e.extra.dockVisible,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(expandFrom = Alignment.Start),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally(shrinkTowards = Alignment.Start),
            ) { FilaDock(e, 30f) }
            BotonRepliegue(e.extra.dockVisible) { Diptico.alternarDockVisible() }
        } else BotonFranja(cl.nikalbazu.diptico.estado.Icono.APPS, "Apps") { Diptico.abrirApps() }
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        DippoInicio(e, 36f)
        androidx.compose.foundation.layout.Spacer(Modifier.size(esc.d(10)))
        Texto(hora, 13f, color = p.tinta, traducir = false, lineas = 1)
        androidx.compose.foundation.layout.Spacer(Modifier.size(esc.d(4)))
        val tono = when { cargando -> Color(0xFF38BDF8); bateria in 0..15 -> Color(0xFFF87171); else -> p.tinta }
        Box(Modifier.size(esc.d(22), esc.d(11)).border(esc.d(1.5f), p.tinta.copy(alpha = 0.7f), RoundedCornerShape(esc.d(3))).padding(esc.d(2))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(((if (bateria < 0) 50 else bateria) / 100f).coerceIn(0.08f, 1f))
                .clip(RoundedCornerShape(esc.d(1.5f))).background(tono))
        }
        Texto(if (bateria >= 0) "$bateria%" else "—", 13f, color = p.tinta, traducir = false, lineas = 1)
    }
}

/** El botón que esconde (‹) o saca (las apps) el dock de la franja. */
@Composable
private fun BotonRepliegue(abierto: Boolean, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val giro by androidx.compose.animation.core.animateFloatAsState(if (abierto) 0f else 180f, label = "repliegue")
    Box(
        Modifier.size(esc.d(28), esc.d(40)).clip(RoundedCornerShape(esc.d(10))).background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = alPulsar).testTag("dock-repliegue"),
        contentAlignment = Alignment.Center,
    ) {
        if (abierto) Texto("‹", 18f, color = p.tinta.copy(alpha = 0.8f), traducir = false, lineas = 1,
            modifier = Modifier.graphicsLayer { rotationZ = giro })
        else IconoDiptico(cl.nikalbazu.diptico.estado.Icono.APPS, p.tinta, 14f)
    }
}

@Composable
private fun BotonFranja(icono: cl.nikalbazu.diptico.estado.Icono, nombre: String?, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    androidx.compose.foundation.layout.Row(
        Modifier.height(esc.d(40)).clip(RoundedCornerShape(esc.d(12))).background(Color.White.copy(alpha = 0.07f))
            .clickable(onClick = alPulsar).padding(horizontal = esc.d(12)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconoDiptico(icono, p.tinta, 15f)
        if (nombre != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(esc.d(7)))
            Texto(nombre, 13f, color = p.tinta, lineas = 1)
        }
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

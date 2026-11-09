package cl.nikalbazu.diptico.ui

import cl.nikalbazu.diptico.estado.Icono
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Panel

/** Pantalla del juego: el juego elegido ocupa todo, con su información encima. Se adapta a pantalla ancha o angosta. */
@Composable
fun PantallaSuperior(e: Estado) {
    if (e.panel == Panel.MENU) {
        // El menú siempre en la pantalla de arriba; la ayuda queda abajo.
        if (!e.intercambiar) Box(Modifier.fillMaxSize().background(LocalPaleta.current.fondo2)) { PanelMenu(e, conAyuda = false) }
        else AyudaMenu(e)
        return
    }
    if (e.panel == Panel.WIDGET && e.coleccionVisual != null) { ColeccionUniverso(e); return }
    if (e.panel == Panel.WIDGET) {
        // Lo que abre el widget aparece aquí arriba, junto al widget en grande; abajo sigue tu estante.
        if (!e.intercambiar) WidgetArriba(e) else LecturaWidget(e)
        return
    }
    if (e.panel == Panel.ENCUADRE) {
        VistaEncuadre(e)
        return
    }
    if (e.panel == Panel.FICHA || e.panel == Panel.IMAGENES) {
        LecturaFicha(e)
        return
    }
    if (e.panel == Panel.APPS) {
        LecturaApps(e)
        return
    }
    if (e.panel == Panel.EDITAR_CONSOLA) {
        PreviaConsolaEditada(e)
        return
    }
    if (e.panel == Panel.GALERIA) {
        VistaGaleria(e)
        return
    }
    if (e.panel == Panel.ASISTENTE) {
        LecturaAsistente(e)
        return
    }
    if (e.editandoWidgets) {
        PantallaInicio(e)
        return
    }
    // Portada ↔ widgets: dos páginas seguidas que se deslizan con el dedo (el botón y el atajo también las mueven).
    InicioDeslizable(
        e,
        // Cualquier toque en esta pantalla vuelve a sacar la barra flotante.
        Modifier.pointerInput(Unit) {
            awaitEachGesture { awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial); Diptico.mostrarBarra() }
        },
        portada = { Caratulas(e) },
        widgets = { PantallaInicio(e) },
    )
}

/** El inicio con el juego elegido en grande (carátulas). */
@Composable
private fun Caratulas(e: Estado) {
    // Modo Sistemas: mientras eliges consola, aquí se ve la consola en grande.
    if (e.vistaConsolas in Diptico.MODOS_ELEGIR && e.enSistemas && e.sesion == null) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f)) { VistaSistema(e, conPistas = false) }
            BarraFlotante(e, null, consola = true)
        }
        return
    }
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val angosta = esc.angosta
    val margen = if (angosta) 22f else 32f
    Box(Modifier.fillMaxSize().background(p.fondo)) {
        val juego = e.seleccionado
        if (juego == null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Logo(26f)
                Spacer(Modifier.height(esc.d(14)))
                Texto(if (e.escaneando) "Buscando tus juegos…" else "No encontré juegos en tu carpeta", 19f, color = p.tenue)
                if (!e.escaneando) {
                    Spacer(Modifier.height(esc.d(6)))
                    Detalle("Abre el menú con Start › Biblioteca para elegirla.", 15f)
                }
            }
            if (e.sesion == null) BarraFlotante(e, null, Modifier.align(Alignment.BottomCenter))
            return@Box
        }
        Heroe(juego, if (angosta) 1f else 0.86f)
        Box(
            Modifier.fillMaxSize().background(
                if (angosta) Brush.verticalGradient(0f to p.fondo.copy(alpha = 0.35f), 0.45f to p.fondo.copy(alpha = 0.7f), 1f to p.fondo)
                else Brush.horizontalGradient(0f to p.fondo, 0.26f to p.fondo, 0.74f to Color.Transparent),
            ),
        )
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(esc.d(180))
                .background(Brush.verticalGradient(listOf(Color.Transparent, p.fondo))),
        )

        TramaTema()
        BarraSuperior(Modifier.align(Alignment.TopStart), inicio = margen, trabajos = e.trabajos)

        AnimatedContent(
            targetState = juego,
            transitionSpec = { (fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 10 }) togetherWith fadeOut(tween(180)) },
            label = "info",
            modifier = Modifier.padding(start = esc.d(margen), end = esc.d(margen), top = esc.d(58))
                .widthIn(max = esc.d(if (angosta) 460f else 400f)),
        ) { j -> InfoJuego(e, j, if (angosta) 32f else 38f) }

        // Todo lo de abajo va en una barra flotante que se esconde sola: así luce el juego.
        if (e.sesion == null) BarraFlotante(e, juego, Modifier.align(Alignment.BottomCenter))
        // Nuevo diseño: la vitrina lleva sus dos botones grandes, estilo consola.
        if (e.sesion == null && juego != null && nuevoDisenoDosPantallas(e)) BotonesVitrina(e, juego,
            Modifier.align(Alignment.BottomStart).padding(start = esc.d(margen), bottom = esc.d(34)))
        if (e.sesion != null) {
            Etiqueta(
                "Jugando ahora", Coral, 13f,
                Modifier.align(Alignment.BottomEnd).padding(end = esc.d(margen), bottom = esc.d(34)),
            )
        }
    }
}

/** Un recordatorio pequeño y pasajero al entrar a los widgets: qué botones usar. No tapa nada ni bloquea. */
@Composable
fun RecordatorioWidgets(e: Estado, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(e.avisoInicio) {
        if (e.avisoInicio == 0L) return@LaunchedEffect
        visible = true
        kotlinx.coroutines.delay(3200)
        visible = false
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = visible && !e.editandoWidgets && e.panel == Panel.NINGUNO, modifier = modifier.padding(bottom = esc.d(16)),
        enter = fadeIn(tween(300)) + androidx.compose.animation.slideInVertically(tween(360)) { it / 2 },
        exit = fadeOut(tween(400)),
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(p.fondo2.copy(alpha = 0.82f)).border(esc.d(1), p.linea, RoundedCornerShape(50))
                .padding(horizontal = esc.d(14), vertical = esc.d(7)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(12)),
        ) {
            Pista(if (e.unaPantalla) "✥" else "↑", "Moverte por los widgets")
            Pista("Select", "Carátulas")
            Pista("Mantén", "Acomodar")
        }
    }
}

/** El arte del juego, a la derecha (o de fondo en pantallas angostas), con un cruce suave al cambiar. */
@Composable
fun Heroe(juego: Juego, ancho: Float) {
    // Un acercamiento lento y suave sobre el arte (se apaga en modo ahorro).
    val zoom = zoomLento(true, 1.06f, 14000)
    val (porJuego, todos) = LocalHeroes.current
    Crossfade(targetState = juego, animationSpec = tween(if (Estilo.efectosCompletos) 360 else 0), label = "arte", modifier = Modifier.fillMaxSize()) { j ->
        Box(Modifier.fillMaxSize()) {
            val tipo = porJuego[j.clave] ?: todos
            if (tipo == "portada" && j.portada != null) {
                // La portada entera (sin recortarla), sobre ella misma difuminada para llenar el espacio.
                Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(ancho)) {
                    if (Estilo.efectosCompletos) ArteJuego(
                        j, preferirArte = false, maxLado = 500,
                        modifier = Modifier.fillMaxSize().blur(36.dp).graphicsLayer { alpha = 0.55f; scaleX = 1.15f; scaleY = 1.15f },
                    )
                    ArteJuego(
                        j, preferirArte = false, maxLado = 1100,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = LocalEscala.current.d(40), top = LocalEscala.current.d(54), bottom = LocalEscala.current.d(30))
                            .fillMaxHeight().aspectRatio(0.72f).graphicsLayer { scaleX = zoom; scaleY = zoom }
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(LocalEscala.current.d(14))),
                    )
                }
            } else {
                ArteJuego(
                    j, preferirArte = true, maxLado = 1600,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(ancho).graphicsLayer { scaleX = zoom; scaleY = zoom },
                )
            }
        }
    }
}

/** Qué imagen va en grande en el inicio: la elegida por juego y la de todos ("arte" o "portada"). */
val LocalHeroes = androidx.compose.runtime.staticCompositionLocalOf { emptyMap<String, String>() to "arte" }

/** Los accesos comparten una superficie con la escena o se alinean con el saludo de widgets. */
@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun BarraFlotante(e: Estado, juego: Juego?, modifier: Modifier = Modifier, arriba: Boolean = false, consola: Boolean = false) {
    // Nuevo diseño con dos pantallas: los accesos viven en la franja de abajo; arriba no se repiten.
    if (nuevoDisenoDosPantallas(e)) return
    val visible = e.barra || e.barraFija
    // Escondida: solo una manija discreta que recuerda que se puede sacar (tocarla también la saca).
    androidx.compose.animation.AnimatedVisibility(
        visible = !visible, modifier = modifier,
        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(500, delayMillis = 250)),
        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)),
    ) {
        val esc = LocalEscala.current
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.clip(RoundedCornerShape(50)).clickable { Diptico.mostrarBarra() }.padding(horizontal = esc.d(20), vertical = esc.d(10))) {
                Box(Modifier.size(esc.d(46), esc.d(5)).clip(RoundedCornerShape(50)).background(LocalPaleta.current.tinta.copy(alpha = 0.28f)))
            }
        }
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = visible, modifier = modifier,
        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) +
            androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(260)) { if (arriba) -it / 2 else it / 2 },
        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(380)) +
            androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(380)) { if (arriba) -it / 3 else it / 3 },
    ) { ContenidoBarra(e, juego, Modifier, arriba, consola) }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun ContenidoBarra(e: Estado, juego: Juego?, modifier: Modifier, arriba: Boolean, consola: Boolean) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val compacta = arriba || esc.angosta
    val widgets = e.inicio == "widgets"
    // El estilo de la barra de la pantalla de arriba: sólida (como antes), difusa o transparente.
    val base = if (arriba) modifier else when (e.extra.estiloBarraArriba) {
        "solida" -> modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(p.fondo.copy(alpha = 0.60f), p.fondo)))
        else -> modifier.fillMaxWidth().fondoBarra(e.extra.estiloBarraArriba)
    }
    Column(base) {
        if (!arriba) Spacer(Modifier.fillMaxWidth().height(esc.d(1)).background(p.linea.copy(alpha = 0.62f)))
        Row(
            Modifier.then(if (arriba) Modifier else Modifier.fillMaxWidth())
                .horizontalScroll(rememberScrollState()).padding(
                    horizontal = esc.d(if (arriba) 0 else if (compacta) 10 else 24),
                    // Arriba también lleva aire: antes el botón marcado se cortaba contra el borde.
                    vertical = esc.d(if (arriba) 4 else 6),
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(if (compacta) 2 else 8)),
        ) {
            if (consola) {
                BotonAcceso(Icono.CONTINUAR, "Entrar", compacta = false, principal = true) { Diptico.entrarSistema() }
                BotonAcceso(Icono.INTERCAMBIAR, "Otra consola", compacta) { Diptico.cambiarConsola(1) }
                Spacer(Modifier.padding(horizontal = esc.d(4)).size(esc.d(1), esc.d(22)).background(p.linea))
            } else if (juego != null) {
                BotonAcceso(Icono.JUGAR, "Jugar", compacta = false, principal = true) { Diptico.jugar() }
                BotonAcceso(Icono.INFO, "Ficha", compacta) { Diptico.abrirFicha() }
                BotonAcceso(Icono.MENU, "Opciones", compacta) { Diptico.abrirRapido() }
                Spacer(Modifier.padding(horizontal = esc.d(4)).size(esc.d(1), esc.d(22)).background(p.linea))
            }
            BotonAcceso(Icono.BUSCAR, "Buscar", compacta) { Diptico.abrirBuscar() }
            BotonAcceso(Icono.APPS, "Apps", compacta) { Diptico.abrirApps() }
            BotonAcceso(if (widgets) Icono.PORTADA else Icono.VISTA, if (widgets) "Carátulas" else "Widgets", compacta) {
                Diptico.alternarInicio()
            }
            if (widgets) BotonAcceso(Icono.EDITAR, "Editar widgets", compacta) { Diptico.editarWidgets() }
            BotonAcceso(Icono.BISA, "DIPPO", compacta, alMantener = { Diptico.hablarConBisa() }) { Diptico.abrirAsistente() }
            BotonAcceso(Icono.AJUSTES, "Ajustes", compacta) { Diptico.abrirMenu() }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BotonAcceso(
    icono: Icono,
    texto: String,
    compacta: Boolean,
    principal: Boolean = false,
    alMantener: (() -> Unit)? = null,
    accion: () -> Unit,
) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val forma = RoundedCornerShape(esc.d(11))
    val pulsar = if (alMantener == null) Modifier.clickable(onClickLabel = texto, onClick = accion)
    else Modifier.combinedClickable(onClickLabel = texto, onLongClickLabel = "Hablar con DIPPO", onLongClick = alMantener, onClick = accion)
    Row(
        Modifier.clip(forma).then(if (principal) Modifier.background(p.superficie).border(esc.d(1.5f), p.acentoTexto.copy(alpha = 0.6f), forma) else Modifier)
            .then(pulsar).height(esc.d(36)).padding(horizontal = esc.d(if (compacta) 7 else 8)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(esc.d(5)),
    ) {
        val tinta = if (principal) p.acentoTexto else p.tinta
        IconoDiptico(icono, tinta, 17f, acento = tinta)
        if (!compacta) Texto(texto, 12f, color = tinta, familia = Nunito, lineas = 1)
    }
}

@Composable
fun InfoJuego(e: Estado, j: Juego, tamTitulo: Float, conPaso: Boolean = true) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val c = Consolas.porId(j.consolaId)
    val datos = e.datos[j.clave]
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        if (c != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoConsola(c, 22f, p.fondo)
                Spacer(Modifier.width(esc.d(8)))
                Etiqueta(c.nombreLargo, p.tenue, 12.5f)
            }
        }
        val logo = rememberImagen(j.logo, 700)
        if (logo != null) {
            Image(
                logo, contentDescription = j.titulo, contentScale = ContentScale.Fit, alignment = Alignment.CenterStart,
                modifier = Modifier.heightIn(max = esc.d(tamTitulo * 3f)).widthIn(max = esc.d(380)),
            )
        } else {
            Texto(j.titulo, tamTitulo, lineas = 3, interlineado = tamTitulo * 1.06f, sombra = true)
        }
        Detalle(
            if ((datos?.tiempo ?: 0L) > 0) "${formatoTiempo(datos!!.tiempo)} jugadas" else "Aún sin jugar con Díptico",
            14f,
        )
        LineaDuracion(e, j)
        // Datos rápidos del juego.
        val chips = buildList {
            if ((datos?.sesiones ?: 0) > 0) add("${datos!!.sesiones} partidas")
            if ((datos?.ultimo ?: 0L) > 1) add(haceCuanto(datos!!.ultimo))
            if (datos?.favorito == true) add("♥ Favorito")
            if ((datos?.notas ?: "").isNotBlank()) add("Tiene notas")
        }
        if (chips.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
            chips.forEach { c ->
                Box(Modifier.clip(RoundedCornerShape(50)).background(p.superficie.copy(alpha = 0.75f)).padding(horizontal = esc.d(9), vertical = esc.d(3))) {
                    Texto(c, 12f, color = p.tenue, familia = Nunito, lineas = 1)
                }
            }
        }
        if (conPaso && !datos?.paso.isNullOrBlank()) {
            Spacer(Modifier.height(esc.d(2)))
            Row {
                Box(Modifier.padding(top = esc.d(6)).size(esc.d(9)).clip(CircleShape).background(Coral))
                Spacer(Modifier.width(esc.d(9)))
                val paso = datos?.paso.orEmpty()
                Column(Modifier.clickable { Diptico.abrirPaso() }) {
                    Texto("Tu próximo paso", 14f, color = p.acentoTexto)
                    Texto(
                        paso.ifEmpty { "Aún no anotas dónde quedaste. Aprieta Y para hacerlo." },
                        17f, peso = FontWeight.SemiBold, lineas = 3, interlineado = 22f,
                        color = if (paso.isEmpty()) p.tenue else p.tinta,
                    )
                }
            }
        }
    }
}

@Composable
fun BotonJugar() {
    val esc = LocalEscala.current
    Row(
        Modifier
            .shadow(esc.d(6), RoundedCornerShape(50), spotColor = Uva)
            .clip(RoundedCornerShape(50))
            .fondoBoton(Uva)
            .clickable { Diptico.jugar() }
            .padding(start = esc.d(4), end = esc.d(14), top = esc.d(4), bottom = esc.d(4)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Más chico que antes: así luce el logo y la imagen grande del juego.
        Box(Modifier.size(esc.d(25)).clip(CircleShape).background(SobreUva), contentAlignment = Alignment.Center) {
            Texto("A", 12f, color = Uva)
        }
        Spacer(Modifier.width(esc.d(7)))
        Texto("¡Jugar!", 14.5f, color = SobreUva)
    }
}


/** "Hoy", "Ayer", "Hace 3 días"… */
fun haceCuanto(ms: Long): String {
    val dias = ((System.currentTimeMillis() - ms) / 86_400_000L).toInt()
    return when {
        dias <= 0 -> "Jugado hoy"
        dias == 1 -> "Jugado ayer"
        dias < 30 -> "Hace $dias días"
        else -> "Hace ${dias / 30} meses"
    }
}




/** Cuánto dura el juego según HowLongToBeat y cuánto te queda (lo busca solo si el juego se queda elegido un rato). */
@Composable
fun LineaDuracion(e: Estado, j: Juego) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    androidx.compose.runtime.LaunchedEffect(j.clave) {
        kotlinx.coroutines.delay(1500)
        Diptico.pedirDuracion(j.clave)
    }
    val d = e.extra.duraciones[j.clave]?.takeIf { it.tiene } ?: return
    val jugado = (e.datos[j.clave]?.tiempo ?: 0L).toLong()
    val texto = cl.nikalbazu.diptico.estado.CuantoDura.teQueda(d, jugado) ?: return
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconoDiptico(Icono.RELOJ, p.acentoTexto, 13f)
        Spacer(Modifier.width(esc.d(6)))
        Texto(if (jugado > 0 || d.principal <= 0f) texto else "Dura ~${cl.nikalbazu.diptico.estado.CuantoDura.horasBonitas(d.principal)} la historia", 13f, color = p.tinta, lineas = 1, traducir = false)
        Spacer(Modifier.width(esc.d(6)))
        Texto("HowLongToBeat", 10f, color = p.tenue, lineas = 1, traducir = false)
    }
}

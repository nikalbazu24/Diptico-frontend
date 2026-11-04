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
        VistaSistema(e.copy(consolaSel = e.consolaEditando))
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
    // Carátulas ↔ widgets con una transición suave (y un recordatorio discreto al llegar a los widgets).
    AnimatedContent(
        targetState = e.inicio == "widgets",
        transitionSpec = {
            val entra = fadeIn(tween(420)) + scaleIn(tween(480, easing = FastOutSlowInEasing), initialScale = if (targetState) 0.94f else 1.04f)
            val sale = fadeOut(tween(260)) + scaleOut(tween(320), targetScale = if (targetState) 1.04f else 0.96f)
            entra togetherWith sale
        },
        label = "inicio",
        // Cualquier toque en esta pantalla vuelve a sacar la barra flotante.
        modifier = Modifier.pointerInput(Unit) {
            awaitEachGesture { awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial); Diptico.mostrarBarra() }
        },
    ) { widgets ->
        if (widgets) Box(Modifier.fillMaxSize()) {
            PantallaInicio(e)
            RecordatorioWidgets(e, Modifier.align(Alignment.TopCenter).padding(top = LocalEscala.current.d(64)))
            if (!e.editandoWidgets && e.sesion == null) BarraFlotante(e, null, Modifier.align(Alignment.BottomCenter))
        } else Caratulas(e)
    }
}

/** El inicio con el juego elegido en grande (carátulas). */
@Composable
private fun Caratulas(e: Estado) {
    // Modo Sistemas: mientras eliges consola, aquí se ve la consola en grande.
    if (e.vistaConsolas in Diptico.MODOS_ELEGIR && e.enSistemas && e.sesion == null) {
        Box(Modifier.fillMaxSize()) {
            VistaSistema(e)
            BarraFlotante(e, null, Modifier.align(Alignment.BottomCenter))
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
    Crossfade(targetState = juego, animationSpec = tween(650), label = "arte", modifier = Modifier.fillMaxSize()) { j ->
        Box(Modifier.fillMaxSize()) {
            val tipo = porJuego[j.clave] ?: todos
            if (tipo == "portada" && j.portada != null) {
                // La portada entera (sin recortarla), sobre ella misma difuminada para llenar el espacio.
                Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(ancho)) {
                    ArteJuego(
                        j, preferirArte = false, maxLado = 500,
                        modifier = Modifier.fillMaxSize().then(if (Estilo.efectosCompletos) Modifier.blur(36.dp) else Modifier).graphicsLayer { alpha = 0.55f; scaleX = 1.15f; scaleY = 1.15f },
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

/** Un botón con la tecla del control que lo hace: se toca o se aprieta. */
@Composable
private fun AccionRapida(tecla: String, texto: String, accion: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(p.superficie.copy(alpha = 0.85f)).clickable { accion() }
            .padding(start = esc.d(4), end = esc.d(11), top = esc.d(4), bottom = esc.d(4)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(esc.d(21)).clip(CircleShape).background(p.fondo), contentAlignment = Alignment.Center) { Texto(tecla, 10.5f, traducir = false) }
        Spacer(Modifier.width(esc.d(6)))
        Texto(texto, 12.5f, lineas = 1)
    }
}

/**
 * La barra flotante del inicio: jugar, ficha y opciones del juego, buscar, apps, turbo, menú y Bisa, en una sola
 * píldora. Se esconde sola a los 5 segundos (o queda fija, en Menú › Inicio) y vuelve deslizando hacia arriba desde
 * el borde de abajo, tocando la pantalla o con B. Escondida, solo queda una manija discreta.
 */
@Composable
fun BarraFlotante(e: Estado, juego: Juego?, modifier: Modifier = Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val visible = e.barra || e.barraFija
    Box(modifier.fillMaxWidth().padding(bottom = esc.d(14), start = esc.d(16), end = esc.d(16)), contentAlignment = Alignment.BottomCenter) {
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(240)) + slideInVertically(androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 420f)) { it / 2 },
            exit = fadeOut(tween(420)) + androidx.compose.animation.slideOutVertically(tween(420)) { it / 3 },
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (juego != null && !esc.angosta) {
                    ConsejoBisa(e, juego)
                    Spacer(Modifier.height(esc.d(8)))
                }
                Row(
                    Modifier.shadow(esc.d(14), RoundedCornerShape(50)).clip(RoundedCornerShape(50))
                        .background(p.fondo2.copy(alpha = 0.9f)).border(esc.d(1), Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                        .padding(horizontal = esc.d(6), vertical = esc.d(6)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(esc.d(4)),
                ) {
                    if (juego != null) {
                        BotonJugar()
                        AccionRapida("X", "Ficha") { Diptico.abrirFicha() }
                        if (!esc.angosta) AccionRapida("Y", "Opciones") { Diptico.abrirRapido() }
                        Box(Modifier.padding(horizontal = esc.d(4)).size(esc.d(1), esc.d(26)).background(p.linea))
                    }
                    IconoBarra(Icono.BUSCAR, "Buscar") { Diptico.abrirBuscar() }
                    IconoBarra(Icono.APPS, "Apps") { Diptico.abrirApps() }
                    // Turbo: tocar libera memoria ahora; mantener lo enciende o apaga al jugar.
                    IconoBarra(Icono.CHISPA, "Turbo", if (e.turbo) Color(0xFFFF5252) else null, alMantener = { Diptico.alternarTurbo() }) { Diptico.turboAhora() }
                    IconoBarra(if (e.inicio == "widgets") Icono.PORTADA else Icono.EDITAR, if (e.inicio == "widgets") "Carátulas" else "Widgets") { Diptico.alternarInicio() }
                    IconoBarra(Icono.MENU, "Menú") { Diptico.abrirMenu() }
                    Box(
                        Modifier.size(esc.d(40)).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Uva, Coral)))
                            .clickable { Diptico.abrirAsistente() },
                        contentAlignment = Alignment.Center,
                    ) { cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(30).value) }
                }
            }
        }
        // Escondida: una manija discreta que recuerda que se puede sacar.
        androidx.compose.animation.AnimatedVisibility(
            visible = !visible,
            enter = fadeIn(tween(600, delayMillis = 300)), exit = fadeOut(tween(150)),
        ) {
            Box(
                Modifier.clip(RoundedCornerShape(50)).clickable { Diptico.mostrarBarra() }.padding(horizontal = esc.d(18), vertical = esc.d(8)),
            ) { Box(Modifier.size(esc.d(44), esc.d(5)).clip(RoundedCornerShape(50)).background(p.tinta.copy(alpha = 0.28f))) }
        }
    }
}

/** Un acceso redondo de la barra flotante, con su nombre chiquito debajo. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun IconoBarra(icono: Icono, texto: String, color: Color? = null, alMantener: (() -> Unit)? = null, accion: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column(
        Modifier.clip(RoundedCornerShape(esc.d(14))).combinedClickable(onLongClick = alMantener) { accion() }
            .padding(horizontal = esc.d(8), vertical = esc.d(2)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconoDiptico(icono, color ?: p.tinta, 19f, acento = color ?: Coral)
        Texto(texto, 9.5f, color = color ?: p.tenue, familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1)
    }
}

/** Una burbuja de diálogo de Bisa con algo útil sobre el juego elegido. Al tocarla, Bisa responde. */
@Composable
private fun ConsejoBisa(e: Estado, j: Juego) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val d = e.datos[j.clave]
    val (texto, pregunta) = when {
        d == null || d.tiempo == 0L -> "¿Quieres saber de qué se trata ${j.titulo}?" to "Cuéntame de qué se trata ${j.titulo} y si me va a gustar, sin spoilers."
        d.paso.isNotBlank() -> "¿Te ayudo con tu próximo paso?" to "En ${j.titulo} mi próximo paso es: \"${d.paso}\". Dame una pista suave."
        d.tiempo > 10 * 3600 -> "Llevas harto en este juego. ¿Te digo qué te falta?" to "Llevo muchas horas en ${j.titulo}. ¿Qué cosas importantes o secretos me podría estar perdiendo?"
        else -> "¿Un truco para ${j.titulo}?" to "Dame un buen truco o consejo para ${j.titulo}."
    }
    Box(
        Modifier.widthIn(max = esc.d(250)).clip(RoundedCornerShape(topStart = esc.d(16), topEnd = esc.d(16), bottomStart = esc.d(16), bottomEnd = esc.d(4)))
            .background(p.superficie.copy(alpha = 0.92f)).clickable { Diptico.abrirAsistente(); Diptico.preguntar(pregunta) }
            .padding(horizontal = esc.d(12), vertical = esc.d(8)),
    ) { Detalle(texto, 13f, color = p.tinta, lineas = 2) }
}

/** Consola, título (o logo), tiempo jugado y el próximo paso. */
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

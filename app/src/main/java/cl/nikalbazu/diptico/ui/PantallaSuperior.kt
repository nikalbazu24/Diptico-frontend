package cl.nikalbazu.diptico.ui

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
    ) { widgets ->
        if (widgets) Box(Modifier.fillMaxSize()) {
            PantallaInicio(e)
            RecordatorioWidgets(e, Modifier.align(Alignment.BottomCenter))
        } else Caratulas(e)
    }
}

/** El inicio con el juego elegido en grande (carátulas). */
@Composable
private fun Caratulas(e: Estado) {
    // Modo Sistemas: mientras eliges consola, aquí se ve la consola en grande.
    if (e.vistaConsolas == "sistemas" && e.enSistemas && e.sesion == null) {
        VistaSistema(e)
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
        Heroe(juego, if (angosta) 1f else 0.8f)
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

        Row(
            Modifier.align(Alignment.BottomStart).padding(start = esc.d(margen), bottom = esc.d(22)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(16)),
        ) {
            BotonJugar()
            AccionRapida("X", "Ficha") { Diptico.abrirFicha() }
            if (!angosta) AccionRapida("Y", "Opciones") { Diptico.abrirRapido() }
        }
        // Bisa, abajo a la derecha, con un consejo según el juego.
        if (e.sesion == null) {
            Column(
                Modifier.align(Alignment.BottomEnd).padding(end = esc.d(margen), bottom = esc.d(20)),
                horizontalAlignment = Alignment.End,
            ) {
                ConsejoBisa(e, juego)
                Spacer(Modifier.height(esc.d(8)))
                BotonBisa()
            }
        }
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
    Crossfade(targetState = juego, animationSpec = tween(650), label = "arte", modifier = Modifier.fillMaxSize()) { j ->
        Box(Modifier.fillMaxSize()) {
            ArteJuego(
                j, preferirArte = true, maxLado = 1600,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(ancho).graphicsLayer { scaleX = zoom; scaleY = zoom },
            )
        }
    }
}

/** Un botón con la tecla del control que lo hace: se toca o se aprieta. */
@Composable
private fun AccionRapida(tecla: String, texto: String, accion: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(p.superficie.copy(alpha = 0.85f)).clickable { accion() }
            .padding(start = esc.d(5), end = esc.d(14), top = esc.d(5), bottom = esc.d(5)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(esc.d(26)).clip(CircleShape).background(p.fondo), contentAlignment = Alignment.Center) { Texto(tecla, 12.5f, traducir = false) }
        Spacer(Modifier.width(esc.d(7)))
        Texto(texto, 14f, lineas = 1)
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
                modifier = Modifier.heightIn(max = esc.d(tamTitulo * 2.4f)).widthIn(max = esc.d(320)),
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
        if (conPaso) {
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
            .padding(start = esc.d(6), end = esc.d(20), top = esc.d(6), bottom = esc.d(6)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(esc.d(34)).clip(CircleShape).background(SobreUva), contentAlignment = Alignment.Center) {
            Texto("A", 15f, color = Uva)
        }
        Spacer(Modifier.width(esc.d(10)))
        Texto("¡Jugar!", 18f, color = SobreUva)
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

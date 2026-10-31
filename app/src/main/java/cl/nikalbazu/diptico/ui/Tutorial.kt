package cl.nikalbazu.diptico.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import cl.nikalbazu.diptico.audio.Musica
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.PasoTuto

/** Qué parte de Díptico muestra cada pantalla. */
enum class Rol { SUPERIOR, INFERIOR, UNICA }

private val Cuerpo = Color(0xFF2A2838)
private val Apagado = Color(0xFF3A3850)
private val Noche = Color(0xFF14121F)

private data class InfoPaso(val etiqueta: String, val titulo: String, val texto: String)

private fun nombreModoLargo(e: Estado) = when {
    !e.dosDisponibles -> "una pantalla"
    e.modoPantallas == "una_negra" -> "una pantalla, con la de abajo en negro"
    e.modoPantallas == "una_libre" -> "una pantalla, con la de abajo libre"
    else -> "las dos pantallas"
}

private fun infoPaso(e: Estado, paso: PasoTuto): InfoPaso = when (paso) {
    PasoTuto.BIENVENIDA -> InfoPaso(
        "Bienvenida", "¡Hola! Soy Díptico",
        "Te ayudo a dejar todo listo. Usa la cruceta y la A, o toca la pantalla.",
    )
    PasoTuto.PANTALLAS -> InfoPaso(
        "Pantallas", "¿Una pantalla o dos?",
        if (e.dosDisponibles) "Con dos, arriba ves el juego y abajo eliges. Con una, todo pasa aquí, como en una tele."
        else "Tu equipo tiene una pantalla, así que todo pasará aquí. Si conectas otra, podrás cambiarlo en el menú.",
    )
    PasoTuto.TEXTO -> InfoPaso(
        "Letra", "¿Cómo se lee mejor?",
        "Muévete entre las opciones y mira cómo cambia todo al tiro.",
    )
    PasoTuto.DISPOSICION -> InfoPaso(
        "Disposición", "¿Qué va en cada pantalla?",
        "Te recomiendo el juego arriba, en la pantalla grande, y el estante abajo para elegir con el dedo.",
    )
    PasoTuto.JUEGOS -> InfoPaso(
        "Al jugar", "¿Dónde se abren los juegos?",
        "Es lo que pasa al apretar A. Para un juego en especial lo puedes cambiar manteniendo A.",
    )
    PasoTuto.CARPETA -> when {
        !e.tieneCarpeta -> InfoPaso(
            "Tus juegos", "¿Dónde están tus juegos?",
            "Elige la carpeta que tiene una carpeta por consola: snes, gba, psx, nds, ps2… igual que en ES-DE.",
        )
        e.escaneando -> InfoPaso("Tus juegos", "Buscando tus juegos…", "La primera vez puede tardar un poco.")
        e.juegos.isEmpty() -> InfoPaso(
            "Tus juegos", "No encontré juegos",
            "Revisa que adentro haya carpetas como snes, gba o psx con tus juegos, y elige la carpeta de nuevo.",
        )
        else -> InfoPaso(
            "Tus juegos", "¡Encontré ${e.juegos.size} juegos!",
            "Están en ${e.consolasConJuegos.size} consolas. Si falta alguna, revisa el nombre de su carpeta.",
        )
    }
    PasoTuto.EMULADORES -> {
        val faltan = e.consolasConJuegos.count { !Diptico.emuladorListo(it) }
        when {
            e.consolasConJuegos.isEmpty() -> InfoPaso(
                "Emuladores", "Emuladores",
                "Cuando agregues juegos, aquí eliges con qué emulador o núcleo de RetroArch se abre cada consola.",
            )
            faltan == 0 -> InfoPaso(
                "Emuladores", "Tus emuladores están listos",
                "Toca una consola para cambiar su emulador. Con RetroArch eliges el núcleo (snes9x, mgba…).",
            )
            else -> InfoPaso(
                "Emuladores", if (faltan == 1) "Falta 1 emulador" else "Faltan $faltan emuladores",
                "Puedes instalarlos después. RetroArch sirve para SNES, GBA, N64, PSX y más.",
            )
        }
    }
    PasoTuto.PORTS -> InfoPaso(
        "Ports", if (e.ports.isEmpty()) "¿Tienes ports?" else "¡${e.ports.size} ports en tu estante!",
        "Son juegos hechos para Android, como Ship of Harkinian, los de Doom o Mario 64. Márcalos y tendrán su propio estante.",
    )
    PasoTuto.PORTADAS -> InfoPaso(
        "Portadas", "Portadas, imágenes y títulos",
        "Las bajo gratis de libretro. Con una clave de SteamGridDB también traigo logos y arte grande.",
    )
    PasoTuto.MUSICA -> InfoPaso(
        "Música", "Elige la música de fondo",
        "Escúchala mientras te mueves. Se calla sola al jugar o si pones Spotify o YouTube Music.",
    )
    PasoTuto.APARIENCIA -> InfoPaso(
        "Apariencia", "¿Claro u oscuro?",
        "Mira cómo cambia mientras eliges. El oscuro ahorra batería en pantallas OLED.",
    )
    PasoTuto.CONTROLES -> InfoPaso(
        "Controles", "Probemos el control",
        "Aprieta cada botón para ver qué hace. Cuando quieras, elige Continuar.",
    )
    PasoTuto.LISTO -> InfoPaso(
        "¡Listo!", "¡Todo listo!",
        "Usarás ${nombreModoLargo(e)}, con ${e.juegos.size} juegos. Todo se cambia después en el menú, con Start.",
    )
}

/** El tutorial siempre va en una sola pantalla: así sirve igual en la Thor, una Odin o un teléfono. */
@Composable
fun Tutorial(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(Modifier.fillMaxSize().background(p.fondo)) {
        Box(
            Modifier.align(Alignment.TopEnd).size(esc.d(460))
                .background(Brush.radialGradient(listOf(Uva.copy(alpha = 0.2f), Color.Transparent))),
        )
        Box(
            Modifier.align(Alignment.BottomStart).size(esc.d(380))
                .background(Brush.radialGradient(listOf(Coral.copy(alpha = 0.12f), Color.Transparent))),
        )
        Row(
            Modifier.fillMaxWidth().padding(start = esc.d(28), end = esc.d(24), top = esc.d(16)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Logo(15f)
            Spacer(Modifier.weight(1f))
            Progreso(e)
        }
        val info = infoPaso(e, e.pasoTuto)
        Entrada(e.pasoTuto, Modifier.fillMaxSize().padding(start = esc.d(28), end = esc.d(24), top = esc.d(52), bottom = esc.d(14))) {
            if (esc.angosta) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
                    Encabezado(info, 26f, 15f)
                    if (e.pasoTuto == PasoTuto.CONTROLES) ChecklistControles(e)
                    ListaOpciones(e, Diptico.opcionesTuto(), Modifier.weight(1f))
                    Pistas("A" to "Elegir", "B" to "Atrás")
                }
            } else {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.width(esc.d(minOf(320f, esc.ancho * 0.45f))).fillMaxHeight()) {
                        Encabezado(info, 29f, 15.5f)
                        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            val s = minOf(maxWidth.value / esc.d(420).value, maxHeight.value / esc.d(390).value)
                            if (s > 0.28f) Ilustracion(e, e.pasoTuto, s * 0.96f)
                        }
                    }
                    Spacer(Modifier.width(esc.d(24)))
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
                        if (e.pasoTuto == PasoTuto.CONTROLES) ChecklistControles(e)
                        ListaOpciones(e, Diptico.opcionesTuto(), Modifier.weight(1f))
                        Pistas("↑ ↓" to "Moverte", "A" to "Elegir", "B" to "Atrás")
                    }
                }
            }
        }
        Paneles(e, conAyuda = true)
    }
}

/**
 * La otra pantalla durante el tutorial: casi apagada mientras eliges,
 * y un saludo cuando eliges usar las dos.
 */
@Composable
fun TutorialAbajo(e: Estado) {
    val esc = LocalEscala.current
    val dos = e.modoElegido && e.modoPantallas == "dos"
    Box(Modifier.fillMaxSize().background(if (dos) LocalPaleta.current.fondo2 else Color.Black), contentAlignment = Alignment.Center) {
        if (dos) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
                MiniEstante()
                Texto("¡Hola desde abajo!", 26f)
                Detalle("Aquí vivirá tu estante de juegos.\nSigue el tutorial en la pantalla de arriba.", 15f)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
                Texto("↑", 30f, color = Color(0xFF3A3850))
                Texto("El tutorial está en la otra pantalla", 15f, color = Color(0xFF3A3850))
            }
        }
    }
}

/** Cada paso entra deslizándose suavemente. */
@Composable
private fun Entrada(clave: Any, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    val a = remember(clave) { Animatable(0f) }
    LaunchedEffect(clave) { a.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
    val px = with(LocalDensity.current) { LocalEscala.current.d(1f).toPx() }
    Box(modifier.graphicsLayer { alpha = a.value; translationX = (1f - a.value) * 40f * px }) { contenido() }
}

@Composable
private fun Encabezado(info: InfoPaso, tamTitulo: Float, tamTexto: Float) {
    val esc = LocalEscala.current
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        Etiqueta(info.etiqueta, Coral, 12.5f)
        Texto(info.titulo, tamTitulo, interlineado = tamTitulo * 1.1f, lineas = 3)
        Detalle(info.texto, tamTexto, lineas = 4)
    }
}

@Composable
private fun Progreso(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val pasos = Diptico.pasosTuto(e)
    val actual = pasos.indexOf(e.pasoTuto).coerceAtLeast(0)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(5))) {
        pasos.forEachIndexed { i, paso ->
            key(paso) {
                val ancho by animateDpAsState(if (i == actual) esc.d(22) else esc.d(7), label = "punto")
                Box(Modifier.width(ancho).height(esc.d(7)).clip(RoundedCornerShape(50)).background(if (i <= actual) Uva else p.linea))
            }
        }
        Spacer(Modifier.width(esc.d(8)))
        Texto("${actual + 1} de ${pasos.size}", 13f, color = p.tenue, familia = Nunito, peso = FontWeight.ExtraBold)
    }
}

@Composable
private fun ListaOpciones(e: Estado, opciones: List<cl.nikalbazu.diptico.estado.Opcion>, modifier: Modifier = Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    if (opciones.isEmpty()) {
        Column(modifier) {
            Row(
                Modifier.fillMaxWidth().height(esc.d(60)).clip(RoundedCornerShape(esc.d(20)))
                    .background(p.superficie.copy(alpha = 0.6f)).padding(horizontal = esc.d(18)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Girador(22f)
                Spacer(Modifier.width(esc.d(12)))
                Texto("Buscando…", 17f)
            }
        }
        return
    }
    val lista = rememberLazyListState()
    LaunchedEffect(e.indice, e.pasoTuto) { lista.animateScrollToItem((e.indice - 2).coerceAtLeast(0)) }
    LazyColumn(state = lista, modifier = modifier, verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        itemsIndexed(opciones) { i, o ->
            TarjetaOpcion(o, activa = i == e.indice, alto = 60f) { Diptico.elegirIndice(i) }
        }
    }
}

@Composable
fun Girador(tam: Float, color: Color = Uva) {
    val esc = LocalEscala.current
    val inf = rememberInfiniteTransition(label = "gira")
    val angulo by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "angulo")
    Canvas(Modifier.size(esc.d(tam))) {
        drawArc(color = color, startAngle = angulo, sweepAngle = 270f, useCenter = false, style = Stroke(width = size.width * 0.14f))
    }
}

// ---------- Ilustraciones ----------

/** Dibujo de cada paso, pensado en 420 x 390 y achicado con [s] para que quepa. */
@Composable
private fun Ilustracion(e: Estado, paso: PasoTuto, s: Float) {
    val esc = LocalEscala.current
    val px = with(LocalDensity.current) { esc.d(1f).toPx() }
    val inf = rememberInfiniteTransition(label = "flota")
    val y by inf.animateFloat(-6f, 6f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
    Box(
        Modifier.requiredSize(esc.d(420), esc.d(390)).graphicsLayer { scaleX = s; scaleY = s; translationY = y * px },
        contentAlignment = Alignment.Center,
    ) {
        when (paso) {
            PasoTuto.TEXTO -> MuestraTexto(e.tamTexto)
            PasoTuto.PORTS -> EstantePorts()
            PasoTuto.MUSICA -> Ecualizador(e)
            PasoTuto.BIENVENIDA -> MiniThor(Noche, Noche, arriba = { Logo(24f, Color.White) }, abajo = { MiniEstante() })
            PasoTuto.PANTALLAS -> {
                val i = e.indice
                MiniThor(
                    Uva,
                    when (i) { 0 -> Coral; 1 -> Color.Black; else -> Apagado },
                    arriba = { EtiquetaThor(if (i == 0) "Juego" else "Juego y estante") },
                    abajo = { EtiquetaThor(when (i) { 0 -> "Estante"; 1 -> "Apagada"; else -> "Otra app" }) },
                )
            }
            PasoTuto.DISPOSICION -> {
                val inv = e.indice == 1
                MiniThor(
                    if (inv) Coral else Uva, if (inv) Uva else Coral,
                    arriba = { EtiquetaThor(if (inv) "Estante" else "Juego") },
                    abajo = { EtiquetaThor(if (inv) "Juego" else "Estante") },
                )
            }
            PasoTuto.JUEGOS -> {
                val abajoJ = e.indice == 1
                MiniThor(
                    if (abajoJ) Noche else Cielo, if (abajoJ) Cielo else Noche,
                    arriba = { EtiquetaThor(if (abajoJ) "Díptico" else "▶  Juego") },
                    abajo = { EtiquetaThor(if (abajoJ) "▶  Juego" else "Díptico") },
                )
            }
            PasoTuto.CARPETA -> if (e.juegos.isNotEmpty()) RejillaConsolas(e, conEstado = false) else ArbolCarpetas()
            PasoTuto.EMULADORES -> RejillaConsolas(e, conEstado = true)
            PasoTuto.PORTADAS -> EstanteEjemplo(e)
            PasoTuto.APARIENCIA -> MiniInicio()
            PasoTuto.CONTROLES -> ControlIlustrado(e.probados)
            PasoTuto.LISTO -> MiniThor(Uva, Coral, arriba = { EtiquetaThor("¡A jugar!") }, abajo = { EtiquetaThor("✓") })
        }
    }
}

@Composable
private fun EtiquetaThor(texto: String) {
    Texto(texto, 22f, color = Color.White)
}

/** Una Thor en miniatura, con sus dos pantallas encendidas del color que se pida. */
@Composable
private fun MiniThor(
    luzArriba: Color,
    luzAbajo: Color,
    arriba: @Composable BoxScope.() -> Unit = {},
    abajo: @Composable BoxScope.() -> Unit = {},
) {
    val esc = LocalEscala.current
    val la by animateColorAsState(luzArriba, tween(400), label = "arriba")
    val lb by animateColorAsState(luzAbajo, tween(400), label = "abajo")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(esc.d(340), esc.d(196)).clip(RoundedCornerShape(esc.d(26))).background(Cuerpo).padding(esc.d(14))) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(esc.d(12))).background(la), contentAlignment = Alignment.Center, content = arriba)
        }
        Box(Modifier.size(esc.d(290), esc.d(12)).background(Color(0xFF1C1A28)))
        Box(Modifier.size(esc.d(340), esc.d(170)).clip(RoundedCornerShape(esc.d(26))).background(Cuerpo)) {
            Box(
                Modifier.align(Alignment.Center).size(esc.d(178), esc.d(146)).clip(RoundedCornerShape(esc.d(10))).background(lb),
                contentAlignment = Alignment.Center, content = abajo,
            )
            Box(Modifier.align(Alignment.CenterStart).padding(start = esc.d(28)).size(esc.d(40)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(esc.d(40), esc.d(12)).clip(RoundedCornerShape(esc.d(3))).background(Apagado))
                Box(Modifier.size(esc.d(12), esc.d(40)).clip(RoundedCornerShape(esc.d(3))).background(Apagado))
            }
            Box(Modifier.align(Alignment.CenterEnd).padding(end = esc.d(26)).size(esc.d(42))) {
                listOf(Alignment.TopCenter, Alignment.CenterStart, Alignment.CenterEnd, Alignment.BottomCenter).forEach { a ->
                    Box(Modifier.align(a).size(esc.d(12)).clip(CircleShape).background(Apagado))
                }
            }
        }
    }
}

@Composable
private fun MiniEstante() {
    val esc = LocalEscala.current
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8)), verticalAlignment = Alignment.Bottom) {
        listOf(Color(0xFFFF8A5B) to 46f, Cielo to 52f, Color(0xFFFF5D8F) to 58f, Uva to 50f).forEach { (c, alto) ->
            Box(Modifier.size(esc.d(30), esc.d(alto)).clip(RoundedCornerShape(esc.d(5))).background(c))
        }
    }
}

@Composable
private fun IconoCarpeta(color: Color, tam: Float) {
    val esc = LocalEscala.current
    Canvas(Modifier.size(esc.d(tam), esc.d(tam * 0.8f))) {
        val w = size.width
        val h = size.height
        drawRoundRect(color, Offset(0f, 0f), Size(w * 0.45f, h * 0.32f), CornerRadius(w * 0.08f))
        drawRoundRect(color, Offset(0f, h * 0.16f), Size(w, h * 0.84f), CornerRadius(w * 0.1f))
    }
}

/** Cómo ordenar la carpeta de juegos. */
@Composable
private fun ArbolCarpetas() {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCarpeta(Coral, 40f)
            Spacer(Modifier.width(esc.d(12)))
            Texto("Juegos", 24f)
        }
        listOf("snes", "psx", "nds", "ps2", "ps3").forEach { nombre ->
            val c = Consolas.porCarpeta(nombre)
            Row(Modifier.padding(start = esc.d(30)), verticalAlignment = Alignment.CenterVertically) {
                Texto("└", 20f, color = p.tenue)
                Spacer(Modifier.width(esc.d(10)))
                IconoCarpeta(Uva, 30f)
                Spacer(Modifier.width(esc.d(10)))
                Texto(nombre, 20f, modifier = Modifier.width(esc.d(70)))
                if (c != null) IconoMedio(c.medio, c.color, p.fondo, tam = 30f)
            }
        }
        Texto("…y así con cada consola", 15f, color = p.tenue, familia = Nunito, peso = FontWeight.Bold, modifier = Modifier.padding(start = esc.d(30)))
    }
}

/** Las consolas encontradas, con cuántos juegos tienen o si su emulador está listo. */
@Composable
private fun RejillaConsolas(e: Estado, conEstado: Boolean) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val hay = e.consolasEmu.isNotEmpty()
    val lista = if (hay) e.consolasEmu else Consolas.todas.filter { !it.esPorts }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(18))) {
        lista.chunked(4).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                fila.forEach { c ->
                    Column(Modifier.width(esc.d(88)), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(esc.d(66)), contentAlignment = Alignment.Center) {
                            IconoMedio(c.medio, c.color, p.fondo, tam = 56f, modifier = Modifier.graphicsLayer { alpha = if (hay) 1f else 0.35f })
                            if (conEstado && hay) {
                                val listo = Diptico.emuladorListo(c)
                                Box(
                                    Modifier.align(Alignment.TopEnd).size(esc.d(22)).clip(CircleShape).background(if (listo) Sol else Coral),
                                    contentAlignment = Alignment.Center,
                                ) { Texto(if (listo) "✓" else "!", 12f, color = Tinta) }
                            }
                        }
                        Spacer(Modifier.height(esc.d(4)))
                        Texto(c.nombre, 14f, lineas = 1)
                        if (!conEstado && hay) {
                            val n = e.juegos.count { it.consolaId == c.id }
                            Texto(if (n == 1) "1 juego" else "$n juegos", 12f, color = p.tenue, familia = Nunito, peso = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

/** Ejemplo de estante con portadas, para el paso de portadas. */
@Composable
private fun EstanteEjemplo(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val u = with(LocalDensity.current) { esc.d(1f).toPx() }
    val ejemplos = listOf(
        Triple("snes", Dibujo.MONTANAS, Color(0xFFFF8A5B)),
        Triple("nds", Dibujo.ESTRELLAS, Cielo),
        Triple("ps2", Dibujo.RAYAS, Color(0xFFFF5D8F)),
    )
    Box(Modifier.size(esc.d(420), esc.d(300))) {
        if (e.tieneMedia) {
            Box(
                Modifier.align(Alignment.TopCenter).clip(RoundedCornerShape(50)).background(Sol.copy(alpha = 0.18f))
                    .padding(horizontal = esc.d(14), vertical = esc.d(6)),
            ) { Texto("Imágenes de ES-DE conectadas ✓", 14f, color = Sol, familia = Nunito, peso = FontWeight.ExtraBold) }
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(50)).fillMaxWidth().height(esc.d(3)).background(p.estante))
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(53)),
            horizontalArrangement = Arrangement.spacedBy(esc.d(18)),
            verticalAlignment = Alignment.Bottom,
        ) {
            ejemplos.forEach { (id, dibujo, color) ->
                val f = Consolas.porId(id)?.caja ?: return@forEach
                Canvas(Modifier.size(esc.d(f.ancho * 1.15f), esc.d(f.alto * 1.15f)).clip(RoundedCornerShape(esc.d(f.radio)))) {
                    portadaGenerada(dibujo, color, u)
                }
            }
        }
    }
}

/** Mini inicio que cambia de tema en vivo. */
@Composable
private fun MiniInicio() {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val u = with(LocalDensity.current) { esc.d(1f).toPx() }
    Box(
        Modifier.size(esc.d(400), esc.d(240)).clip(RoundedCornerShape(esc.d(22))).background(p.fondo)
            .border(esc.d(2), p.linea, RoundedCornerShape(esc.d(22))),
    ) {
        Canvas(Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.7f)) {
            portadaGenerada(Dibujo.MONTANAS, Color(0xFFFF8A5B), u)
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to p.fondo, 0.3f to p.fondo, 0.85f to Color.Transparent)))
        Column(Modifier.padding(esc.d(22)), verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
            Logo(12f)
            Spacer(Modifier.height(esc.d(6)))
            Texto("Leyenda del Bosque", 26f, lineas = 2, interlineado = 28f, modifier = Modifier.width(esc.d(200)))
            Box(
                Modifier.size(esc.d(110), esc.d(36)).clip(RoundedCornerShape(50)).background(Uva),
                contentAlignment = Alignment.Center,
            ) { Texto("¡Jugar!", 15f, color = Color.White) }
        }
    }
}

/** Un control que se ilumina a medida que pruebas cada botón. */
@Composable
private fun ControlIlustrado(probados: Set<String>) {
    val esc = LocalEscala.current
    val inf = rememberInfiniteTransition(label = "a")
    val pulso by inf.animateFloat(0.88f, 1.12f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulso")
    val gatillos by animateColorAsState(if ("gatillos" in probados) Coral else Apagado, label = "g")
    val cruceta by animateColorAsState(if ("cruceta" in probados) Coral else Apagado, label = "c")
    val y by animateColorAsState(if ("Y" in probados) Coral else Apagado, label = "y")
    val start by animateColorAsState(if ("Start" in probados) Coral else Apagado, label = "s")
    Box(Modifier.size(esc.d(400), esc.d(290))) {
        Box(
            Modifier.align(Alignment.TopStart).padding(start = esc.d(34)).size(esc.d(110), esc.d(36))
                .clip(RoundedCornerShape(esc.d(14))).background(gatillos),
            contentAlignment = Alignment.Center,
        ) { Texto("L1", 15f, color = Color.White) }
        Box(
            Modifier.align(Alignment.TopEnd).padding(end = esc.d(34)).size(esc.d(110), esc.d(36))
                .clip(RoundedCornerShape(esc.d(14))).background(gatillos),
            contentAlignment = Alignment.Center,
        ) { Texto("R1", 15f, color = Color.White) }
        Box(Modifier.align(Alignment.BottomCenter).size(esc.d(400), esc.d(236)).clip(RoundedCornerShape(esc.d(64))).background(Cuerpo)) {
            Box(Modifier.align(Alignment.CenterStart).padding(start = esc.d(52)).size(esc.d(96)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(esc.d(96), esc.d(30)).clip(RoundedCornerShape(esc.d(6))).background(cruceta))
                Box(Modifier.size(esc.d(30), esc.d(96)).clip(RoundedCornerShape(esc.d(6))).background(cruceta))
            }
            Box(Modifier.align(Alignment.CenterEnd).padding(end = esc.d(48)).size(esc.d(112))) {
                BotonRedondo("X", Alignment.TopCenter, Apagado)
                BotonRedondo("Y", Alignment.CenterStart, y)
                BotonRedondo("A", Alignment.CenterEnd, Uva, pulso)
                BotonRedondo("B", Alignment.BottomCenter, Apagado)
            }
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(30)).size(esc.d(74), esc.d(26))
                    .clip(RoundedCornerShape(50)).background(start),
                contentAlignment = Alignment.Center,
            ) { Texto("Start", 12f, color = Color.White) }
        }
    }
}

@Composable
private fun BoxScope.BotonRedondo(letra: String, lugar: Alignment, color: Color, escala: Float = 1f) {
    val esc = LocalEscala.current
    Box(
        Modifier.align(lugar).size(esc.d(38)).graphicsLayer { scaleX = escala; scaleY = escala }.clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) { Texto(letra, 15f, color = Color.White) }
}

/** Lista de botones por probar en el paso de controles. */
@Composable
private fun ChecklistControles(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val items = listOf(
        Triple("cruceta", "← →", "Moverte por el estante"),
        Triple("gatillos", "L1 R1", "Cambiar de consola"),
        Triple("Y", "Y", "Anotar tu próximo paso"),
        Triple("Start", "Start", "Abrir el menú"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        items.forEach { (clave, tecla, texto) ->
            val ok = clave in e.probados
            val fondo by animateColorAsState(if (ok) Sol.copy(alpha = 0.18f) else p.superficie.copy(alpha = 0.6f), label = "item")
            Row(
                Modifier.fillMaxWidth().height(esc.d(48)).clip(RoundedCornerShape(esc.d(16))).background(fondo).padding(horizontal = esc.d(14)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.width(esc.d(64)).height(esc.d(30)).clip(RoundedCornerShape(esc.d(10))).border(esc.d(2), p.linea, RoundedCornerShape(esc.d(10))),
                    contentAlignment = Alignment.Center,
                ) { Texto(tecla, 13f) }
                Spacer(Modifier.width(esc.d(12)))
                Texto(texto, 15f, modifier = Modifier.weight(1f), lineas = 1)
                Texto(if (ok) "✓" else "Pruébalo", 13f, color = if (ok) Sol else p.tenue, familia = Nunito, peso = FontWeight.ExtraBold)
            }
        }
    }
}

/** Íconos de apps sobre un estante, para el paso de ports. */
@Composable
private fun EstantePorts() {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(18)), verticalAlignment = Alignment.Bottom) {
            listOf(Color(0xFF2EC4A6) to Icono.JUGAR, Uva to Icono.ESTRELLA, Coral to Icono.EMULADOR, Sol to Icono.DADO)
                .forEachIndexed { i, (c, ic) ->
                    Box(
                        Modifier.size(esc.d(if (i == 1) 96f else 80f)).clip(RoundedCornerShape(esc.d(24))).background(c),
                        contentAlignment = Alignment.Center,
                    ) { IconoDiptico(ic, if (c == Sol) Tinta else Color.White, if (i == 1) 46f else 38f) }
                }
        }
        Box(Modifier.padding(top = esc.d(10)).size(esc.d(400), esc.d(4)).clip(RoundedCornerShape(50)).background(p.estante))
        Spacer(Modifier.height(esc.d(18)))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoMedio(cl.nikalbazu.diptico.data.Medio.APP, Color(0xFF2EC4A6), p.fondo, tam = 40f)
            Spacer(Modifier.width(esc.d(12)))
            Texto("Ports", 32f)
        }
    }
}

/** Muestra cómo se ve la letra con cada tamaño. */
@Composable
private fun MuestraTexto(tam: Float) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column(
        Modifier.size(esc.d(380), esc.d(300)).clip(RoundedCornerShape(esc.d(26))).background(p.superficie).padding(esc.d(26)),
        verticalArrangement = Arrangement.spacedBy(esc.d(12)),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Texto("Aa", 72f, color = Uva)
            Spacer(Modifier.width(esc.d(14)))
            Texto("Aa", 46f, color = Coral, modifier = Modifier.padding(bottom = esc.d(8)))
        }
        Texto("Leyenda del Bosque", 26f, lineas = 1)
        Texto("Tu próximo paso: buscar la llave del templo", 17f, color = p.tenue, lineas = 2)
    }
}

/** Barras que bailan con la pista elegida. */
@Composable
private fun Ecualizador(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val inf = rememberInfiniteTransition(label = "eq")
    val colores = listOf(Uva, Coral, Sol, Cielo, Color(0xFFFF5D8F), Uva, Coral)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.height(esc.d(200)), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
            colores.forEachIndexed { i, c ->
                val dur = 380 + (i * 97) % 300
                val h by inf.animateFloat(
                    if (e.musica) 40f else 14f, if (e.musica) 190f - (i % 3) * 30f else 16f,
                    infiniteRepeatable(tween(dur, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b$i",
                )
                Box(Modifier.width(esc.d(30)).height(esc.d(h)).clip(RoundedCornerShape(esc.d(10))).background(if (e.musica) c else Apagado))
            }
        }
        Spacer(Modifier.height(esc.d(22)))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoDiptico(if (e.musica) Icono.MUSICA else Icono.SILENCIO, p.tinta, 34f, acento = Coral)
            Spacer(Modifier.width(esc.d(12)))
            Texto(
                if (!e.musica) "Sin música" else when (e.pista) {
                    "mezcla" -> "Mezcla"; "mia" -> "Tu música"
                    else -> Musica.pistas.firstOrNull { it.id == e.pista }?.nombre ?: ""
                },
                30f,
            )
        }
    }
}

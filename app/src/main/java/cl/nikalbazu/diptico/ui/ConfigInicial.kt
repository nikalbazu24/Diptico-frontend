package cl.nikalbazu.diptico.ui

import cl.nikalbazu.diptico.estado.*

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/*
 * Configuración básica de la primera vez (boceto 13b): cinco pasos con DIPPO.
 *  - Arriba: la fila de pasos, DIPPO diciendo qué toca y una vista previa que se va armando (tu tarjeta, el estante
 *    de consolas, qué falta para jugar, cómo se verá tu inicio, el resumen).
 *  - Abajo: los controles del paso (las mismas opciones que se recorren con la cruceta y A).
 *  - Con una pantalla: las dos mitades lado a lado.
 */

private data class DichoPaso(val texto: String, val nota: String, val cara: CaraDippo)

private fun dichoDe(e: Estado): DichoPaso = when (e.pasoTuto) {
    PasoTuto.PANTALLAS -> DichoPaso("Veo que tu consola tiene dos pantallas. ¿Cómo quieres usar Díptico?",
        "Si hoy eliges dos y mañana prefieres una sola, se cambia en Ajustes › Pantallas › Modo de pantallas.", CaraDippo.NORMAL)
    PasoTuto.BIENVENIDA -> DichoPaso("¡Hola! Soy DIPPO, el que vive en tu Díptico. ¿Cómo te llamo?",
        "Tu nombre y tu personaje salen en tu perfil y en el saludo de cada día.", CaraDippo.FELIZ)
    PasoTuto.CARPETA -> when {
        e.escaneando -> DichoPaso("Buscando tus juegos… ya casi.", "Reconozco cada consola por el nombre de su carpeta.", CaraDippo.PENSANDO)
        e.tieneCarpeta && e.juegos.isNotEmpty() -> DichoPaso("¡Encontré ${e.juegos.size} juegos en ${e.consolasConJuegos.size} consolas!",
            "Las portadas, cajas e imágenes se bajan solas mientras sigues. No hace falta esperar.", CaraDippo.CELEBRANDO)
        e.tieneCarpeta -> DichoPaso("Esa carpeta no tiene juegos que yo reconozca.", "Elige la que tiene una carpeta por consola (snes, ps2, gba…).", CaraDippo.PREOCUPADO)
        else -> DichoPaso("Ahora tus juegos. Muéstrame dónde los guardas y yo armo la colección.",
            "La carpeta que tiene una carpeta por consola: snes, ps2, gba…", CaraDippo.NORMAL)
    }
    PasoTuto.EMULADORES -> if (e.consolasEmu.all { Diptico.emuladorListo(it) })
        DichoPaso("Todas tus consolas tienen con qué jugar. ¡Bien!", "Cada consola puede usar otro emulador cuando quieras.", CaraDippo.CELEBRANDO)
        else DichoPaso("Para jugar hacen falta emuladores. Te marqué los que faltan.",
            "Toca una consola para elegir su emulador. Los que faltan se instalan después en Ajustes › Emuladores al día.", CaraDippo.PENSANDO)
    PasoTuto.CUENTAS -> DichoPaso("Si tienes cuentas, conéctalas ahora: así traigo portadas, cajas y logros de verdad.",
        "Todo es opcional y se puede hacer después en Ajustes › Cuentas y conexiones.", CaraDippo.PENSANDO)
    PasoTuto.APARIENCIA -> DichoPaso("¿Cómo quieres que se vea? Detrás de cada consola puedo poner el juego que la hizo famosa.",
        "Lo ves cambiar al instante.", CaraDippo.FELIZ)
    else -> DichoPaso("¡Listo${if (e.nombre.isNotBlank()) ", ${e.nombre}" else ""}! Tu colección ya está armada.",
        "¿Te muestro dónde está cada cosa? Toma dos minutos.", CaraDippo.CELEBRANDO)
}

/**
 * La configuración completa vive en la pantalla principal (arriba, o la única): la fila de pasos con su nombre,
 * DIPPO diciendo qué toca, el título del paso con sus opciones grandes y el pie fijo con Atrás y Siguiente.
 * Con dos pantallas, la de abajo solo muestra el logo de Díptico respirando (ver [ConfigAbajoCV]).
 * Con una sola, el logo animado va chico al pie de la columna de DIPPO.
 */
@Composable
fun ConfigArribaCV(e: Estado, modifier: Modifier = Modifier, logoChico: Boolean = false) {
    LienzoBoceto(1920f, 1080f, modifier.testTag("config-arriba"), fondo = LocalColeccion.current.fondo, zoom = ZOOM_ARRIBA) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val opciones = Diptico.opcionesTuto()
        val pasos = Diptico.pasosTuto(e)
        val enSiguiente = e.indice >= opciones.size
        val ancho = anchoLienzo()
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(col.acento.copy(alpha = .30f), Color.Transparent), radius = 1400f)))
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(56), vertical = esc.d(26)), verticalArrangement = Arrangement.spacedBy(esc.d(20))) {
            // Encabezado: el saludo y los pasos, cada uno con su nombre (los ya hechos se pueden tocar para volver).
            Column(verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
                TB("¡Bienvenido!", 40f, titulo = true)
                FilaPasos(e, pasos, Modifier.fillMaxWidth())
            }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(44))) {
                val d = dichoDe(e)
                Column(Modifier.width(esc.d(minOf(380f, ancho * .28f))).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(18))) {
                    cl.nikalbazu.diptico.bisa.AvatarBisa(150f * esc.factor, cara = d.cara)
                    Box(Modifier.clip(RoundedCornerShape(topStart = esc.d(8), topEnd = esc.d(36), bottomEnd = esc.d(36), bottomStart = esc.d(36)))
                        .background(Color(0xFFF4F3FA)).padding(horizontal = esc.d(26), vertical = esc.d(22))) {
                        TB(d.texto, 30f, color = Color(0xFF12131F), peso = FontWeight.ExtraBold, lineas = 6)
                    }
                    TB(d.nota, 23f, color = col.sub, lineas = 4)
                    if (logoChico) {
                        Spacer(Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                            LogoAnimado(30f)
                            TB("Díptico", 32f, titulo = true, traducir = false)
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(16))) {
                    Column(verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
                        TB(tituloPaso(e.pasoTuto), 52f, titulo = true, lineas = 2)
                        TB(subtituloPaso(e), 24f, color = col.sub, lineas = 3)
                    }
                    if (e.pasoTuto == PasoTuto.CARPETA) BotonCarpeta(e)
                    if (e.pasoTuto == PasoTuto.LISTO) ResumenListo(e, Modifier.fillMaxWidth().weight(1f))
                    else {
                        if (opciones.isEmpty() && e.escaneando) Row(Modifier.fillMaxWidth().height(esc.d(84)).vidrio(26f).padding(horizontal = esc.d(24)),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) { Girador(30f); TB("Buscando tus juegos…", 26f) }
                        val lista = rememberLazyListState()
                        LaunchedEffect(e.indice, e.pasoTuto) { if (opciones.isNotEmpty() && !enSiguiente) lista.animateScrollToItem((e.indice - 1).coerceAtLeast(0)) }
                        LazyColumn(Modifier.fillMaxWidth().weight(1f), state = lista, verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
                            if (e.pasoTuto != PasoTuto.CARPETA) itemsIndexed(opciones) { i, o ->
                                FilaConfig(e, o, i == e.indice) { Diptico.elegirIndice(i) }
                            }
                        }
                    }
                }
            }
            PieConfig(e, pasos, enSiguiente)
        }
    }
}

/** Los pasos de la configuración: una barra por paso con su nombre debajo, para saber siempre cuánto falta. */
@Composable
private fun FilaPasos(e: Estado, pasos: List<PasoTuto>, modifier: Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val actual = pasos.indexOf(e.pasoTuto).coerceAtLeast(0)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
        pasos.forEachIndexed { i, p ->
            val on = i == actual
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(esc.d(12))).clickable(enabled = i < actual) { Diptico.irAPaso(p) }.padding(vertical = esc.d(4)),
                verticalArrangement = Arrangement.spacedBy(esc.d(8)),
            ) {
                Box(Modifier.fillMaxWidth().height(esc.d(9)).clip(CircleShape).background(
                    when { i < actual -> col.acento; on -> col.texto; else -> col.texto.copy(alpha = .14f) }))
                TB(nombrePasoTuto(p), 22f, color = if (on) col.texto else if (i < actual) col.sub else col.tenue, peso = FontWeight.ExtraBold, lineas = 1)
            }
        }
    }
}

/** El pie fijo: Atrás y Siguiente (Start o R1 aprietan Siguiente desde cualquier fila). */
@Composable
private fun PieConfig(e: Estado, pasos: List<PasoTuto>, enSiguiente: Boolean) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val n = pasos.indexOf(e.pasoTuto)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(18))) {
        if (n > 0) BotonTecla("B", "Atrás", { Diptico.pasoAnterior() }, alto = 76f, letra = 26f)
        TB("Paso ${n + 1} de ${pasos.size} · cruceta para elegir, A para marcar", 22f, color = col.tenue, peso = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f), lineas = 2)
        if (!enSiguiente) TB("Start", 20f, color = col.tenue, peso = FontWeight.ExtraBold, traducir = false)
        Row(
            Modifier.height(esc.d(84)).clip(CircleShape)
                .background(if (enSiguiente) col.elegido else col.texto)
                .border(if (enSiguiente) 4.dp else 0.dp, col.texto, CircleShape)
                .clickable { Diptico.siguientePaso() }.testTag("config-siguiente").padding(horizontal = esc.d(40)),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(14)),
        ) {
            val tinta = if (enSiguiente) col.enElegido else col.fondo
            TB(Diptico.textoSiguiente(e), 30f, color = tinta, titulo = true)
            TB("›", 38f, color = tinta, titulo = true, traducir = false)
        }
    }
}

/**
 * La pantalla de abajo durante la configuración: solo el logo de Díptico, con las dos piezas meciéndose en sentidos
 * contrarios, un brillo que las cruza y un halo que respira. Todo lo que hay que elegir está arriba.
 */
@Composable
fun ConfigAbajoCV(e: Estado, modifier: Modifier = Modifier) {
    LienzoBoceto(1240f, 1080f, modifier.testTag("config-abajo"), fondo = LocalColeccion.current.fondo, zoom = ZOOM_ABAJO) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val mov = LocalMovimiento.current
        val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "halo")
        val halo = t.animateFloat(.35f, .8f, androidx.compose.animation.core.infiniteRepeatable(tween(2000), androidx.compose.animation.core.RepeatMode.Reverse), label = "respira")
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (mov) halo.value else .6f }
            .background(Brush.radialGradient(listOf(col.acento.copy(alpha = .32f), Color.Transparent), radius = 620f)))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(40))) {
            LogoAnimado(110f)
            TB("Díptico", 84f, titulo = true, traducir = false)
        }
    }
}

/** La marca de Díptico viva: el panel de arriba sube cuando el de abajo baja, y un brillo pasa por el de arriba. */
@Composable
fun LogoAnimado(tam: Float, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val mov = LocalMovimiento.current
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "logo")
    val vaiven = t.animateFloat(-1f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(1500), androidx.compose.animation.core.RepeatMode.Reverse), label = "vaiven")
    val brillo = t.animateFloat(-0.5f, 1.6f, androidx.compose.animation.core.infiniteRepeatable(tween(3000)), label = "brillo")
    val mecer = esc.d(tam * 0.16f)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(tam * 0.22f))) {
        Box(
            Modifier.size(esc.d(tam * 1.25f), esc.d(tam * 0.5f))
                .graphicsLayer { translationY = if (mov) -vaiven.value * mecer.toPx() else 0f }
                .clip(RoundedCornerShape(esc.d(tam * 0.2f))).background(Uva)
                .drawWithContent {
                    drawContent()
                    if (mov) {
                        val x = brillo.value * size.width
                        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = .45f), Color.Transparent), startX = x - size.width * .3f, endX = x + size.width * .3f))
                    }
                },
        )
        Box(
            Modifier.size(esc.d(tam * 0.95f), esc.d(tam * 0.5f))
                .graphicsLayer { translationY = if (mov) vaiven.value * mecer.toPx() else 0f }
                .clip(RoundedCornerShape(esc.d(tam * 0.2f))).background(Coral),
        )
    }
}

private fun tituloPaso(p: PasoTuto) = when (p) {
    PasoTuto.PANTALLAS -> "Tus pantallas"; PasoTuto.BIENVENIDA -> "Tú"; PasoTuto.CARPETA -> "Tus juegos"; PasoTuto.EMULADORES -> "Para jugar"; PasoTuto.CUENTAS -> "Tus cuentas"
    PasoTuto.APARIENCIA -> "Tu estilo"; else -> "¡Todo listo!"
}

private fun subtituloPaso(e: Estado) = when (e.pasoTuto) {
    PasoTuto.PANTALLAS -> "Dos pantallas o una sola. Después se puede cambiar."
    PasoTuto.BIENVENIDA -> "Cómo te llamas y cómo te ves. Todo se cambia después."
    PasoTuto.CARPETA -> "Elige la carpeta donde guardas tus juegos."
    PasoTuto.EMULADORES -> "Con qué se abre cada consola. ◀ ▶ cambia el emulador."
    PasoTuto.CUENTAS -> "Discord, RetroAchievements, SteamGridDB, TheGamesDB, ScreenScraper… Todo opcional."
    PasoTuto.APARIENCIA -> "El tema, lo que va detrás de cada consola y el sonido."
    else -> "Ahora DIPPO te muestra cómo se usa. Toma dos minutos."
}

/** Una fila de la configuración. Personaje, color y tema traen su propia tira que se desliza sola hacia lo elegido. */
@Composable
private fun FilaConfig(e: Estado, o: Opcion, elegida: Boolean, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(30)))
            .background(if (elegida) col.texto.copy(alpha = .10f) else col.texto.copy(alpha = .04f))
            .border(if (elegida) 3.dp else 1.dp, if (elegida) col.texto else col.borde, RoundedCornerShape(esc.d(30)))
            .clickable(onClick = onClick).padding(horizontal = esc.d(28), vertical = esc.d(20)),
        verticalArrangement = Arrangement.spacedBy(esc.d(12)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
            o.activa?.let { Box(Modifier.size(esc.d(18)).clip(CircleShape).background(if (it) Color(0xFF2EC4A6) else Color(0xFFFFD29A))) }
            Column(Modifier.weight(1f)) {
                TB(o.titulo, 31f, peso = FontWeight.ExtraBold, lineas = 2)
                if (o.valor.isNotBlank()) TB(o.valor, 23f, color = col.sub, peso = FontWeight.Bold, traducir = false, lineas = 2)
            }
            if (o.menos != null) {
                BotonFlecha("‹") { o.menos.invoke() }
                BotonFlecha("›") { o.accion() }
            }
        }
        when (o.titulo) {
            "Tu personaje" -> TiraElegible(PERSONAJES.map { it.first } + INICIAL, e.extra.perfil.personaje) { id, on ->
                Box(Modifier.size(esc.d(96)).clip(CircleShape).border(if (on) 4.dp else 0.dp, col.texto, CircleShape)
                    .clickable { Diptico.cambiarPerfil { it.copy(personaje = id, foto = "", fotoDiscord = false) } }, contentAlignment = Alignment.Center) {
                    FotoPerfil(e.extra.perfil.copy(fotoDiscord = false), 84f, anillo = false, personaje = id)
                }
            }
            "Tu color" -> TiraElegible(Perfil.COLORES.map { it.second }, e.extra.perfil.color) { c, on ->
                Box(Modifier.size(esc.d(64)).clip(CircleShape).background(Color(c)).border(if (on) 5.dp else 0.dp, col.texto, CircleShape)
                    .clickable { Diptico.cambiarPerfil { it.copy(color = c) } })
            }
            "Tema" -> TiraElegible(TEMAS_INICIALES, TEMAS_INICIALES.firstOrNull { it.id == e.tema }) { t, on ->
                Column(Modifier.width(esc.d(150)).clip(RoundedCornerShape(esc.d(20))).border(if (on) 4.dp else 1.dp, if (on) col.texto else col.borde, RoundedCornerShape(esc.d(20)))
                    .clickable { Diptico.elegirTema(t.id) }) {
                    Box(Modifier.fillMaxWidth().height(esc.d(64)).background(Brush.linearGradient(listOf(Color(t.acento), Color(if (t.fondo != 0L) t.fondo else 0xFF07080F)))))
                    TB(t.nombre, 17f, peso = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = esc.d(12), vertical = esc.d(8)))
                }
            }
            else -> Unit
        }
    }
}

/** Una tira horizontal que se desliza sola para mostrar lo elegido (y se puede deslizar con el dedo). */
@Composable
private fun <T> TiraElegible(items: List<T>, elegido: T?, item: @Composable (T, Boolean) -> Unit) {
    val esc = LocalEscala.current
    val estado = androidx.compose.foundation.lazy.rememberLazyListState()
    val i = items.indexOf(elegido).coerceAtLeast(0)
    LaunchedEffect(i) { estado.animateScrollToItem((i - 2).coerceAtLeast(0)) }
    androidx.compose.foundation.lazy.LazyRow(state = estado, horizontalArrangement = Arrangement.spacedBy(esc.d(14)),
        verticalAlignment = Alignment.CenterVertically) {
        items(items.size) { k -> item(items[k], k == i && elegido != null) }
    }
}

@Composable
private fun BotonFlecha(t: String, accion: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Box(Modifier.size(esc.d(60)).clip(CircleShape).background(col.texto.copy(alpha = .1f)).clickable(onClick = accion), contentAlignment = Alignment.Center) {
        TB(t, 30f, peso = FontWeight.ExtraBold, traducir = false)
    }
}

/** Paso «Tus juegos»: el botón grande para elegir la carpeta. */
@Composable
private fun BotonCarpeta(e: Estado) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val on = e.indice == 0
    Row(
        Modifier.fillMaxWidth().height(esc.d(170)).clip(RoundedCornerShape(esc.d(40))).background(if (on) col.elegido else col.texto.copy(alpha = .08f))
            .clickable { Diptico.pedirCarpeta?.invoke(Carpeta.JUEGOS) }.padding(horizontal = esc.d(34)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(24)),
    ) {
        val tinta = if (on) col.enElegido else col.texto
        Box(Modifier.size(esc.d(100)).clip(RoundedCornerShape(esc.d(28))).background(tinta), contentAlignment = Alignment.Center) {
            IconoDiptico(Icono.CARPETA, if (on) col.elegido else col.fondo, 54f)
        }
        Column(Modifier.weight(1f)) {
            TB(if (e.tieneCarpeta) "Carpeta elegida ✓" else "Elegir la carpeta de juegos", 38f, color = tinta, titulo = true, lineas = 2)
            TB(if (e.escaneando) "Buscando…" else if (e.tieneCarpeta) "${e.juegos.size} juegos en ${e.consolasConJuegos.size} consolas · A elige otra" else "La que tiene una carpeta por consola: snes, ps2, gba…",
                24f, color = tinta.copy(alpha = .75f), peso = FontWeight.ExtraBold, lineas = 3)
        }
    }
}

/** Paso «Listo»: lo que quedó armado. */
@Composable
private fun ResumenListo(e: Estado, modifier: Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val listas = e.consolasEmu.count { Diptico.emuladorListo(it) }
    val datos = listOf(
        "${e.juegos.size}" to "juegos", "${e.consolasConJuegos.size}" to "consolas",
        "$listas/${e.consolasEmu.size}" to "listas para jugar", (Diptico.todosLosTemas(e).firstOrNull { it.id == e.tema }?.nombre ?: "") to "tu tema",
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(esc.d(16))) {
        datos.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                fila.forEach { (n, t) ->
                    Column(Modifier.weight(1f).vidrio(30f).padding(horizontal = esc.d(26), vertical = esc.d(20))) {
                        TB(n, 50f, titulo = true, traducir = false, lineas = 2); TB(t, 24f, color = col.sub, peso = FontWeight.ExtraBold, lineas = 2)
                    }
                }
            }
        }
    }
}

/** Con una pantalla: todo en la misma, con el logo animado chico al pie de la columna de DIPPO. */
@Composable
fun ConfigUnicaCV(e: Estado) {
    ConfigArribaCV(e, logoChico = true)
}

/** Lo que Raiz dibuja mientras no estás configurado: la mitad que toca a esta pantalla y, encima, los paneles (teclado). */
@Composable
fun ConfigPantalla(e: Estado, rol: Rol) {
    // Con dos pantallas, la configuración ya las usa a las dos (se puede cambiar después en Ajustes).
    LaunchedEffect(e.dosDisponibles) { if (e.dosDisponibles && !e.modoElegido) Diptico.elegirModo("dos") }
    Box(Modifier.fillMaxSize()) {
        // Con dos pantallas, arriba se configura todo y abajo solo respira el logo, aunque esta pantalla haya nacido
        // como «única» antes de elegir el modo: así nada se repite.
        val dos = e.dosDisponibles && e.modoPantallas == "dos"
        when {
            rol == Rol.INFERIOR -> ConfigAbajoCV(e)
            dos -> ConfigArribaCV(e)
            else -> ConfigUnicaCV(e)
        }
        if (rol != Rol.INFERIOR) Paneles(e, conAyuda = true)
    }
}

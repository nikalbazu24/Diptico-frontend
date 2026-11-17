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

/** La pantalla de arriba (o la mitad izquierda con una pantalla). */
@Composable
fun ConfigArribaCV(e: Estado, modifier: Modifier = Modifier) {
    LienzoBoceto(1920f, 1080f, modifier.testTag("config-arriba"), fondo = LocalColeccion.current.fondo, zoom = if (e.unaPantalla || !e.dosDisponibles) 1f else ZOOM_ARRIBA) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(col.acento.copy(alpha = .35f), Color.Transparent), radius = 1500f)))
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(80), vertical = esc.d(40)), verticalArrangement = Arrangement.spacedBy(esc.d(30))) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(30))) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                    MarcaLogo(46f); TB("Díptico", 40f, titulo = true, traducir = false)
                }
                Spacer(Modifier.weight(1f))
                FilaPasos(e)
            }
            BarraProgreso(e)
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(60))) {
                val d = dichoDe(e)
                Column(Modifier.width(esc.d(minOf(720f, anchoLienzo() * .4f))).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(26), Alignment.CenterVertically)) {
                    cl.nikalbazu.diptico.bisa.AvatarBisa(260f * esc.factor, cara = d.cara)
                    Box(Modifier.clip(RoundedCornerShape(topStart = esc.d(10), topEnd = esc.d(44), bottomEnd = esc.d(44), bottomStart = esc.d(44)))
                        .background(Color(0xFFF4F3FA)).padding(horizontal = esc.d(36), vertical = esc.d(30))) {
                        TB(d.texto, 40f, color = Color(0xFF12131F), peso = FontWeight.ExtraBold, lineas = 4)
                    }
                    TB(d.nota, 27f, color = col.sub, lineas = 3)
                }
                val dEntra = duracionMovimiento(320); val dSale = duracionMovimiento(200)
                AnimatedContent(e.pasoTuto, Modifier.weight(1f).fillMaxHeight(), label = "previa",
                    transitionSpec = { (fadeIn(tween(dEntra)) + slideInHorizontally { it / 8 }) togetherWith fadeOut(tween(dSale)) },
                ) { paso -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PreviaPaso(e, paso) } }
            }
        }
    }
}

@Composable
private fun FilaPasos(e: Estado) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val pasos = Diptico.pasosTuto(e)
    val actual = pasos.indexOf(e.pasoTuto).coerceAtLeast(0)
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
        pasos.forEachIndexed { i, p ->
            val on = i == actual
            Row(
                Modifier.height(esc.d(72)).clip(CircleShape).background(if (on) col.elegido else col.texto.copy(alpha = .06f))
                    .clickable(enabled = i < actual) { Diptico.irAPaso(p) }.padding(start = esc.d(8), end = esc.d(22)),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(12)),
            ) {
                Box(Modifier.size(esc.d(54)).clip(CircleShape).background(if (on) col.enElegido else col.texto.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
                    TB(if (i < actual) "✓" else "${i + 1}", 25f, color = if (on) col.elegido else col.texto, peso = FontWeight.ExtraBold, traducir = false)
                }
                TB(nombrePasoTuto(p), 27f, color = if (on) col.enElegido else if (i < actual) col.texto else col.tenue, peso = FontWeight.ExtraBold)
            }
        }
    }
}

/** Una barra fina con el avance de la configuración. */
@Composable
private fun BarraProgreso(e: Estado) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val pasos = Diptico.pasosTuto(e)
    val f by androidx.compose.animation.core.animateFloatAsState((pasos.indexOf(e.pasoTuto) + 1f) / pasos.size, tween(500), label = "avance")
    Box(Modifier.fillMaxWidth().height(esc.d(8)).clip(CircleShape).background(col.texto.copy(alpha = .08f))) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(col.acento, Color(0xFFFF9B54)))))
    }
}

/** Qué servicios ya están conectados (una pastilla por servicio). */
@Composable
private fun PreviaCuentas(e: Estado) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val c = e.raspadores
    val servicios = listOf(
        "Discord" to e.extra.discordUsuario.isNotBlank(),
        "RetroAchievements" to (e.usuarioRA.isNotBlank() && e.claveRA.isNotBlank()),
        "SteamGridDB" to e.claveSgdb.isNotBlank(),
        "ScreenScraper" to c.screenscraper,
        "TheGamesDB" to c.conTgdb,
        "IGDB" to c.igdb,
        "RAWG" to c.conRawg,
    )
    Column(Modifier.fillMaxWidth(.95f).vidrio(56f, fuerte = true).padding(esc.d(48)), verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
        TB("${servicios.count { it.second }} de ${servicios.size} conectados", 44f, titulo = true, traducir = false)
        servicios.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                fila.forEach { (n, ok) ->
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(esc.d(30))).background(if (ok) col.acento.copy(alpha = .25f) else Color.White.copy(alpha = .06f)).padding(horizontal = esc.d(24), vertical = esc.d(14))) {
                        TB((if (ok) "✓ " else "") + n, 26f, color = if (ok) col.texto else col.sub, peso = FontWeight.ExtraBold, traducir = false)
                    }
                }
            }
        }
    }
}

/** La vista previa de cada paso: lo que vas armando. */
@Composable
private fun PreviaPaso(e: Estado, paso: PasoTuto) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    when (paso) {
        PasoTuto.BIENVENIDA -> Row(
            Modifier.fillMaxWidth(.95f).vidrio(56f, fuerte = true).border(4.dp, Color(e.extra.perfil.color), RoundedCornerShape(esc.d(56))).padding(esc.d(56)),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(40)),
        ) {
            FotoPerfil(e.extra.perfil, 230f)
            Column(verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
                TB(e.nombre.ifBlank { "Tu nombre" }, 84f, titulo = true, color = if (e.nombre.isBlank()) col.tenue else col.texto, traducir = false)
                TB("Coleccionista desde hoy", 30f, color = col.sub, peso = FontWeight.ExtraBold)
            }
        }
        PasoTuto.CARPETA, PasoTuto.EMULADORES -> Estante(e, conEmulador = paso == PasoTuto.EMULADORES)
        PasoTuto.CUENTAS -> PreviaCuentas(e)
        PasoTuto.APARIENCIA -> {
            val c = e.consolasConJuegos.firstOrNull()
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(esc.d(40))).border(4.dp, col.acento, RoundedCornerShape(esc.d(40)))) {
                FondoConsolaCV(e, c, Modifier.fillMaxSize())
                Column(Modifier.align(Alignment.CenterStart).padding(start = esc.d(44)), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    TB("CONSOLA 1 DE ${e.consolasConJuegos.size.coerceAtLeast(1)}", 18f, color = col.sub, peso = FontWeight.ExtraBold, espaciado = 3f)
                    TB(c?.nombre ?: "Tu consola", 58f, titulo = true)
                }
                if (c != null) Consola3D(c, Modifier.align(Alignment.CenterEnd).padding(end = esc.d(40)).fillMaxHeight(.7f).aspectRatio(1.4f))
            }
        }
        else -> Estante(e, conEmulador = false)
    }
}

/** El estante de consolas que se arma al buscar tus juegos (con su estado de emulador en el paso «Para jugar»). */
@Composable
private fun Estante(e: Estado, conEmulador: Boolean) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val consolas = e.consolasConJuegos.take(9)
    if (consolas.isEmpty()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(16))) {
            if (e.escaneando) Girador(60f) else IconoDiptico(Icono.CARPETA, col.tenue, 120f)
            TB(if (e.escaneando) "Buscando…" else "Aquí aparecerán tus consolas", 30f, color = col.sub, titulo = true)
        }
        return
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(esc.d(20))) {
        consolas.chunked(3).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(20))) {
                fila.forEach { c ->
                    val lista = Diptico.emuladorListo(c)
                    Column(
                        Modifier.weight(1f).height(esc.d(230)).clip(RoundedCornerShape(esc.d(36)))
                            .background(Brush.linearGradient(listOf(c.color, Color(0xFF141526)))).padding(esc.d(26)),
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        val n = e.juegosPorConsola[c.id]?.size ?: 0
                        TB(c.nombre, 32f, color = Color.White, peso = FontWeight.ExtraBold)
                        TB(if (n == 1) "1 juego" else "$n juegos", 23f, color = Color.White.copy(alpha = .8f), peso = FontWeight.ExtraBold)
                        if (conEmulador) Box(Modifier.padding(top = esc.d(8)).clip(CircleShape)
                            .background(if (lista) Color(0xFF2EC4A6) else Color(0xFFFFD29A)).padding(horizontal = esc.d(16), vertical = esc.d(6))) {
                            TB(if (lista) "✓ Lista" else "Falta emulador", 20f, color = Color(0xFF12131F), peso = FontWeight.ExtraBold)
                        }
                    }
                }
                repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (e.consolasConJuegos.size > 9) TB("y ${e.consolasConJuegos.size - 9} consolas más", 20f, color = col.sub, peso = FontWeight.ExtraBold)
    }
}

/**
 * La pantalla de abajo (o la mitad derecha con una pantalla): los controles del paso, cada uno en su fila con su propio
 * desplazamiento (◀▶ cambia dentro de la fila, ↑↓ pasa de fila). «Siguiente» queda fijo abajo: Start o R1 lo aprietan desde
 * cualquier fila, así nunca hay que subir a buscarlo.
 */
@Composable
fun ConfigAbajoCV(e: Estado, modifier: Modifier = Modifier) {
    LienzoBoceto(1240f, 1080f, modifier.testTag("config-abajo"), fondo = LocalColeccion.current.fondo, zoom = if (e.unaPantalla || !e.dosDisponibles) 1f else ZOOM_ABAJO) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val opciones = Diptico.opcionesTuto()
        val pasos = Diptico.pasosTuto(e)
        val enSiguiente = e.indice >= opciones.size
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(44), vertical = esc.d(34)), verticalArrangement = Arrangement.spacedBy(esc.d(20))) {
            Column(verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
                TB("PASO ${pasos.indexOf(e.pasoTuto) + 1} DE ${pasos.size}", 18f, color = col.acento, peso = FontWeight.ExtraBold, espaciado = 3f)
                TB(tituloPaso(e.pasoTuto), 50f, titulo = true)
                TB(subtituloPaso(e), 22f, color = col.sub, lineas = 2)
            }
            if (e.pasoTuto == PasoTuto.CARPETA) BotonCarpeta(e)
            if (e.pasoTuto == PasoTuto.LISTO) ResumenListo(e, Modifier.fillMaxWidth().weight(1f))
            else {
                if (opciones.isEmpty() && e.escaneando) Row(Modifier.fillMaxWidth().height(esc.d(90)).vidrio(28f).padding(horizontal = esc.d(26)), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(esc.d(16))) { Girador(30f); TB("Buscando tus juegos…", 26f) }
                val lista = rememberLazyListState()
                LaunchedEffect(e.indice, e.pasoTuto) { if (opciones.isNotEmpty() && !enSiguiente) lista.animateScrollToItem((e.indice - 1).coerceAtLeast(0)) }
                LazyColumn(Modifier.fillMaxWidth().weight(1f), state = lista, verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
                    itemsIndexed(opciones) { i, o ->
                        if (e.pasoTuto == PasoTuto.CARPETA) return@itemsIndexed
                        FilaConfig(e, o, i == e.indice) { Diptico.elegirIndice(i) }
                    }
                }
            }
            // El pie fijo: Atrás y Siguiente.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(18))) {
                if (pasos.indexOf(e.pasoTuto) > 0) BotonTecla("B", "Atrás", { Diptico.pasoAnterior() }, alto = 84f, letra = 26f)
                Spacer(Modifier.weight(1f))
                if (!enSiguiente) TB("Start", 18f, color = col.tenue, peso = FontWeight.ExtraBold, traducir = false)
                val brillo = if (enSiguiente) 1f else .0f
                Row(
                    Modifier.height(esc.d(96)).clip(CircleShape)
                        .background(if (enSiguiente) col.elegido else col.texto.copy(alpha = .12f))
                        .border(if (enSiguiente) 4.dp else 1.dp, if (enSiguiente) col.texto.copy(alpha = brillo) else col.borde, CircleShape)
                        .clickable { Diptico.siguientePaso() }.testTag("config-siguiente").padding(horizontal = esc.d(40)),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(14)),
                ) {
                    TB(Diptico.textoSiguiente(e), 30f, color = if (enSiguiente) col.enElegido else col.texto, titulo = true)
                    TB("›", 38f, color = if (enSiguiente) col.enElegido else col.texto, titulo = true, traducir = false)
                }
            }
        }
    }
}

private fun tituloPaso(p: PasoTuto) = when (p) {
    PasoTuto.BIENVENIDA -> "Tú"; PasoTuto.CARPETA -> "Tus juegos"; PasoTuto.EMULADORES -> "Para jugar"; PasoTuto.CUENTAS -> "Tus cuentas"
    PasoTuto.APARIENCIA -> "Tu estilo"; else -> "¡Todo listo!"
}

private fun subtituloPaso(e: Estado) = when (e.pasoTuto) {
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
            .clickable(onClick = onClick).padding(horizontal = esc.d(26), vertical = esc.d(18)),
        verticalArrangement = Arrangement.spacedBy(esc.d(12)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
            o.activa?.let { Box(Modifier.size(esc.d(18)).clip(CircleShape).background(if (it) Color(0xFF2EC4A6) else Color(0xFFFFD29A))) }
            Column(Modifier.weight(1f)) {
                TB(o.titulo, 27f, peso = FontWeight.ExtraBold)
                if (o.valor.isNotBlank()) TB(o.valor, 19f, color = col.sub, peso = FontWeight.Bold, traducir = false)
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
            TB(if (e.tieneCarpeta) "Carpeta elegida ✓" else "Elegir la carpeta de juegos", 36f, color = tinta, titulo = true)
            TB(if (e.escaneando) "Buscando…" else if (e.tieneCarpeta) "${e.juegos.size} juegos en ${e.consolasConJuegos.size} consolas · A elige otra" else "La que tiene una carpeta por consola: snes, ps2, gba…",
                20f, color = tinta.copy(alpha = .75f), peso = FontWeight.ExtraBold, lineas = 2)
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
                        TB(n, 50f, titulo = true, traducir = false, lineas = 1); TB(t, 21f, color = col.sub, peso = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

/** Con una pantalla: las dos mitades lado a lado. */
@Composable
fun ConfigUnicaCV(e: Estado) {
    Row(Modifier.fillMaxSize().background(LocalColeccion.current.fondo)) {
        ConfigArribaCV(e, Modifier.weight(1.1f))
        ConfigAbajoCV(e, Modifier.weight(.9f))
    }
}

/** Lo que Raiz dibuja mientras no estás configurado: la mitad que toca a esta pantalla y, encima, los paneles (teclado). */
@Composable
fun ConfigPantalla(e: Estado, rol: Rol) {
    // Con dos pantallas, la configuración ya las usa a las dos (se puede cambiar después en Ajustes).
    LaunchedEffect(e.dosDisponibles) { if (e.dosDisponibles && !e.modoElegido) Diptico.elegirModo("dos") }
    Box(Modifier.fillMaxSize()) {
        // Con dos pantallas cada una muestra SU mitad (arriba DIPPO y la vista previa, abajo los controles), aunque esta
        // pantalla haya nacido como «única» antes de elegir el modo: así nada se repite.
        val dos = e.dosDisponibles && e.modoPantallas == "dos"
        when {
            rol == Rol.INFERIOR -> ConfigAbajoCV(e)
            dos -> ConfigArribaCV(e)
            else -> ConfigUnicaCV(e)
        }
        if (rol != Rol.INFERIOR) Paneles(e, conAyuda = true)
    }
}

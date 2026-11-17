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
    PasoTuto.APARIENCIA -> DichoPaso("¿Cómo quieres que se vea? Detrás de cada consola puedo poner su juego más jugado.",
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
            FilaPasos(e)
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

/** La vista previa de cada paso: lo que vas armando. */
@Composable
private fun PreviaPaso(e: Estado, paso: PasoTuto) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    when (paso) {
        PasoTuto.BIENVENIDA -> Row(
            Modifier.fillMaxWidth(.95f).vidrio(56f, fuerte = true).padding(esc.d(56)),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(40)),
        ) {
            FotoPerfil(e.extra.perfil, 230f)
            Column(verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
                TB(e.nombre.ifBlank { "Tu nombre" }, 84f, titulo = true, color = if (e.nombre.isBlank()) col.tenue else col.texto, traducir = false)
                TB("Coleccionista desde hoy", 30f, color = col.sub, peso = FontWeight.ExtraBold)
            }
        }
        PasoTuto.CARPETA, PasoTuto.EMULADORES -> Estante(e, conEmulador = paso == PasoTuto.EMULADORES)
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
        else -> {
            val listas = e.consolasEmu.count { Diptico.emuladorListo(it) }
            val datos = listOf(
                "${e.juegos.size}" to "juegos en tu colección", "${e.consolasConJuegos.size}" to "consolas",
                "$listas/${e.consolasEmu.size}" to "listas para jugar", (Diptico.todosLosTemas(e).firstOrNull { it.id == e.tema }?.nombre ?: "") to "tu tema",
            )
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(esc.d(22))) {
                datos.chunked(if (anchoLienzo() < 1500f) 1 else 2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(22))) {
                        fila.forEach { (n, t) ->
                            Column(Modifier.weight(1f).vidrio(36f).padding(horizontal = esc.d(30), vertical = esc.d(26))) {
                                TB(n, 64f, titulo = true, traducir = false, lineas = 1); TB(t, 26f, color = col.sub, peso = FontWeight.ExtraBold, lineas = 2)
                            }
                        }
                    }
                }
            }
        }
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

/** La pantalla de abajo (o la mitad derecha con una pantalla): los controles del paso. */
@Composable
fun ConfigAbajoCV(e: Estado, modifier: Modifier = Modifier) {
    LienzoBoceto(1240f, 1080f, modifier.testTag("config-abajo"), fondo = LocalColeccion.current.fondo, zoom = if (e.unaPantalla || !e.dosDisponibles) 1f else ZOOM_ABAJO) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val opciones = Diptico.opcionesTuto()
        val pasos = Diptico.pasosTuto(e)
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(40), vertical = esc.d(32)), verticalArrangement = Arrangement.spacedBy(esc.d(18))) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                TB(nombrePasoTuto(e.pasoTuto), 46f, titulo = true)
                TB("Paso ${pasos.indexOf(e.pasoTuto) + 1} de ${pasos.size}", 20f, color = col.sub, peso = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = esc.d(8)))
            }
            when (e.pasoTuto) {
                PasoTuto.BIENVENIDA -> Personajes(e)
                PasoTuto.CARPETA -> BotonCarpeta(e)
                PasoTuto.APARIENCIA -> MuestrasTemas(e)
                else -> Unit
            }
            if (opciones.isEmpty()) Row(Modifier.fillMaxWidth().height(esc.d(90)).vidrio(28f).padding(horizontal = esc.d(26)), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(esc.d(16))) { Girador(30f); TB("Buscando…", 26f) }
            val lista = rememberLazyListState()
            LaunchedEffect(e.indice, e.pasoTuto) { if (opciones.isNotEmpty()) lista.animateScrollToItem((e.indice.coerceIn(0, opciones.size - 1) - 1).coerceAtLeast(0)) }
            LazyColumn(Modifier.fillMaxWidth().weight(1f), state = lista, verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
                itemsIndexed(opciones) { i, o -> FilaConfig(o, i == e.indice, principal = i == 0) { Diptico.elegirIndice(i) } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (pasos.indexOf(e.pasoTuto) > 0) BotonTecla("B", "Atrás", { Diptico.pasoAnterior() }, alto = 72f, letra = 24f)
                Spacer(Modifier.weight(1f))
                PistasFinas("✥" to "Elegir", "A" to "Usar")
            }
        }
    }
}

/** Lo que Raiz dibuja mientras no estás configurado: la mitad que toca a esta pantalla y, encima, los paneles (teclado). */
@Composable
fun ConfigPantalla(e: Estado, rol: Rol) {
    // Con dos pantallas, la configuración ya las usa a las dos (se puede cambiar después en Ajustes).
    LaunchedEffect(e.dosDisponibles) { if (e.dosDisponibles && !e.modoElegido) Diptico.elegirModo("dos") }
    Box(Modifier.fillMaxSize()) {
        when {
            rol == Rol.UNICA || !e.dosDisponibles -> ConfigUnicaCV(e)
            rol == Rol.INFERIOR -> ConfigAbajoCV(e)
            else -> ConfigArribaCV(e)
        }
        if (rol != Rol.INFERIOR || !e.dosDisponibles) Paneles(e, conAyuda = true)
    }
}

/** Una opción grande; si se puede cambiar hacia los lados, con ‹ › a mano. */
@Composable
private fun FilaConfig(o: Opcion, elegida: Boolean, principal: Boolean, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val fondo = when { principal && elegida -> col.elegido; elegida -> col.texto.copy(alpha = .12f); else -> col.texto.copy(alpha = .05f) }
    val tinta = if (principal && elegida) col.enElegido else col.texto
    Row(
        Modifier.fillMaxWidth().height(esc.d(100)).clip(RoundedCornerShape(esc.d(30))).background(fondo)
            .border(if (elegida && !principal) 3.dp else 1.dp, if (elegida && !principal) col.texto else col.borde, RoundedCornerShape(esc.d(30)))
            .clickable(onClick = onClick).padding(horizontal = esc.d(26)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(18)),
    ) {
        o.activa?.let { Box(Modifier.size(esc.d(20)).clip(CircleShape).background(if (it) Color(0xFF2EC4A6) else Color(0xFFFFD29A))) }
        Column(Modifier.weight(1f)) {
            TB(o.titulo, 27f, color = tinta, peso = FontWeight.ExtraBold)
            if (o.valor.isNotBlank()) TB(o.valor, 18f, color = tinta.copy(alpha = .7f), peso = FontWeight.Bold, traducir = false)
        }
        if (o.menos != null) {
            BotonFlecha("‹") { o.menos.invoke() }
            BotonFlecha("›") { o.accion() }
        }
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

/** Paso «Tú»: los personajes para elegir con el dedo. */
@Composable
private fun Personajes(e: Estado) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val ids = PERSONAJES.map { it.first }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
        ids.forEach { id ->
            val on = e.extra.perfil.personaje == id
            Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(esc.d(24))).background(col.texto.copy(alpha = if (on) .14f else .05f))
                .border(if (on) 3.dp else 1.dp, if (on) col.texto else col.borde, RoundedCornerShape(esc.d(24)))
                .clickable { Diptico.cambiarPerfil { it.copy(personaje = id, foto = "") } }, contentAlignment = Alignment.Center) {
                FotoPerfil(e.extra.perfil, 92f, anillo = false, personaje = id)
            }
        }
    }
}

/** Paso «Tus juegos»: el botón grande para elegir la carpeta. */
@Composable
private fun BotonCarpeta(e: Estado) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Row(
        Modifier.fillMaxWidth().height(esc.d(150)).clip(RoundedCornerShape(esc.d(40))).background(col.elegido)
            .clickable { Diptico.pedirCarpeta?.invoke(Carpeta.JUEGOS) }.padding(horizontal = esc.d(34)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(24)),
    ) {
        Box(Modifier.size(esc.d(90)).clip(RoundedCornerShape(esc.d(26))).background(col.enElegido), contentAlignment = Alignment.Center) {
            IconoDiptico(Icono.CARPETA, col.elegido, 50f)
        }
        Column {
            TB(if (e.tieneCarpeta) "Carpeta elegida ✓" else "Elegir la carpeta de juegos", 34f, color = col.enElegido, titulo = true)
            TB(if (e.tieneCarpeta) "Toca para elegir otra" else "La que tiene snes, ps2, gba…", 19f, color = col.enElegido.copy(alpha = .7f), peso = FontWeight.ExtraBold)
        }
    }
}

/** Paso «Tu estilo»: los temas en muestras para tocar. */
@Composable
private fun MuestrasTemas(e: Estado) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
        TEMAS_INICIALES.forEach { t ->
            val on = t.id == e.tema
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(esc.d(26))).background(col.texto.copy(alpha = if (on) .1f else .04f))
                    .border(if (on) 3.dp else 1.dp, if (on) col.texto else col.borde, RoundedCornerShape(esc.d(26)))
                    .clickable { Diptico.elegirTema(t.id) }.padding(esc.d(12)),
                verticalArrangement = Arrangement.spacedBy(esc.d(8)),
            ) {
                Box(Modifier.fillMaxWidth().height(esc.d(70)).clip(RoundedCornerShape(esc.d(18)))
                    .background(Brush.linearGradient(listOf(Color(t.acento), Color(if (t.fondo != 0L) t.fondo else 0xFF07080F)))))
                TB(t.nombre, 19f, peso = FontWeight.ExtraBold)
            }
        }
    }
}

/** Con una pantalla: las dos mitades lado a lado. */
@Composable
fun ConfigUnicaCV(e: Estado) {
    Row(Modifier.fillMaxSize().background(LocalColeccion.current.fondo)) {
        ConfigArribaCV(e, Modifier.weight(1.15f))
        ConfigAbajoCV(e, Modifier.weight(.85f))
    }
}

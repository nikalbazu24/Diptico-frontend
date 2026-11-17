package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.*

/**
 * Personalizar una consola con Colección viva (boceto 9a–9c): todas las opciones en una pantalla, ordenadas en
 * secciones (Carpeta, Imágenes, Cajas, Juego y sonido, Restaurar), y en la otra un panel vivo con la consola,
 * tu historia con ella y una vista previa de lo que estás cambiando.
 */

private fun gruposDe(l: List<Opcion>): List<String> = l.map { it.grupo ?: "Carpeta" }.distinct()

private val NOTAS_SECCION = mapOf(
    "Carpeta" to "cómo se ve en la biblioteca", "Fondo" to "lo que va detrás en el inicio", "Imágenes" to "su logo", "Cajas" to "forma de los estuches",
    "Juego y sonido" to "emulador y música", "Restaurar" to "dejarla como venía",
)

/** Las opciones, arriba con dos pantallas ([angosto] = false) o a la izquierda con una (secciones en fila). */
@Composable
fun ConsolaOpcionesCV(e: Estado, angosto: Boolean = false) {
    val opciones = remember(e) { Diptico.opcionesEditarConsola(e) }
    val c = Consolas.porId(e.consolaEditando)
    val grupos = gruposDe(opciones)
    val actual = opciones.getOrNull(e.indice)
    val grupo = actual?.grupo ?: grupos.firstOrNull() ?: "Carpeta"
    LienzoBoceto(if (angosto) 1240f else 1920f, 1080f, Modifier.testTag("consola-opciones-cv"), fondo = LocalColeccion.current.fondo, zoom = if (angosto) 1f else ZOOM_ARRIBA) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf((c?.color ?: col.acento).copy(alpha = .30f), Color.Transparent))))
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(64), vertical = esc.d(36)), verticalArrangement = Arrangement.spacedBy(esc.d(24))) {
            // Encabezado
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(20))) {
                if (c != null) Box(Modifier.size(esc.d(84)).clip(RoundedCornerShape(esc.d(26))).background(c.color.copy(alpha = .35f)), contentAlignment = Alignment.Center) {
                    IconoConsola(c, 56f, Color.Transparent)
                }
                Column(Modifier.weight(1f)) {
                    TB("PERSONALIZAR", 19f, color = col.sub, peso = FontWeight.ExtraBold, espaciado = 2.6f)
                    TB(c?.bonito ?: "Carpeta", 48f, titulo = true)
                }
                if (!angosto) HoraBateria(58f, 23f)
            }
            if (angosto) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                grupos.forEach { g ->
                    val on = g == grupo
                    Box(Modifier.height(esc.d(58)).clip(CircleShape).background(if (on) col.elegido else col.vidrio).border(1.dp, col.borde, CircleShape)
                        .clickable { irASeccion(opciones, g) }.padding(horizontal = esc.d(22)), contentAlignment = Alignment.Center) {
                        TB(g, 21f, color = if (on) col.enElegido else col.texto, peso = FontWeight.ExtraBold)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(36))) {
                if (!angosto) Column(Modifier.width(esc.d(360)).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
                    grupos.forEach { g ->
                        val on = g == grupo
                        Row(
                            Modifier.fillMaxWidth().height(esc.d(96)).clip(RoundedCornerShape(esc.d(30)))
                                .background(if (on) col.elegido else col.vidrio).border(1.dp, col.borde, RoundedCornerShape(esc.d(30)))
                                .clickable { irASeccion(opciones, g) }.padding(horizontal = esc.d(24)),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(18)),
                        ) {
                            Box(Modifier.size(esc.d(14)).clip(CircleShape).background(if (on) (c?.color ?: col.acento) else col.texto.copy(alpha = .25f)))
                            Column {
                                TB(g, 29f, titulo = true, color = if (on) col.enElegido else col.texto)
                                TB(NOTAS_SECCION[g] ?: "", 18f, color = if (on) col.enElegido.copy(alpha = .7f) else col.sub)
                            }
                        }
                    }
                }
                // Las opciones de la sección
                Column(Modifier.weight(1f).fillMaxHeight().vidrio(36f).padding(esc.d(26)), verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
                    val deGrupo = opciones.withIndex().filter { (it.value.grupo ?: "Carpeta") == grupo }
                    TB("${grupo.uppercase()} · ${deGrupo.size} OPCIONES", 19f, color = col.sub, peso = FontWeight.ExtraBold, espaciado = 2.6f, modifier = Modifier.padding(start = esc.d(8)))
                    val lista = rememberLazyListState()
                    LaunchedEffect(e.indice, grupo) {
                        val k = deGrupo.indexOfFirst { it.index == e.indice }
                        val vistos = lista.layoutInfo.visibleItemsInfo.filter { it.offset >= 0 && it.offset + it.size <= lista.layoutInfo.viewportEndOffset }.map { it.index }
                        if (k >= 0 && k !in vistos) lista.animateScrollToItem(k)
                    }
                    LazyColumn(state = lista, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
                        itemsIndexed(deGrupo) { _, (i, o) ->
                            val on = i == e.indice
                            Row(
                                Modifier.fillMaxWidth().height(esc.d(96)).clip(RoundedCornerShape(esc.d(28)))
                                    .background(if (on) col.texto.copy(alpha = .10f) else col.texto.copy(alpha = .03f))
                                    .then(if (on) Modifier.border(3.dp, col.texto, RoundedCornerShape(esc.d(28))) else Modifier)
                                    .clickable { if (on) o.accion() else Diptico.elegirIndice(i) }.padding(horizontal = esc.d(28)),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16)),
                            ) {
                                TB(o.titulo, 27f, peso = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                                Row(Modifier.height(esc.d(52)).clip(CircleShape).background(if (on) col.elegido else col.texto.copy(alpha = .08f)),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    if (o.menos != null && on) Box(Modifier.fillMaxHeight().clickable { o.menos.invoke() }.padding(horizontal = esc.d(14)), contentAlignment = Alignment.Center) { TB("◀", 17f, color = col.enElegido, traducir = false) }
                                    TB(o.valor.ifEmpty { "›" }, 21f, color = if (on) col.enElegido else col.texto, peso = FontWeight.ExtraBold, traducir = o.valor.isEmpty(),
                                        modifier = Modifier.padding(horizontal = if (o.menos != null && on) 0.dp else esc.d(20)))
                                    if (o.menos != null && on) Box(Modifier.fillMaxHeight().clickable { o.accion() }.padding(horizontal = esc.d(14)), contentAlignment = Alignment.Center) { TB("▶", 17f, color = col.enElegido, traducir = false) }
                                }
                            }
                        }
                    }
                    if (angosto && actual != null) TB(actual.ayuda, 21f, color = col.sub, lineas = 2, modifier = Modifier.padding(horizontal = esc.d(10)))
                }
                // Qué hace (solo con el ancho de arriba)
                if (!angosto) Column(Modifier.width(esc.d(470)).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(20))) {
                    Column(Modifier.fillMaxWidth().vidrio(36f).padding(esc.d(32)), verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
                        TB("QUÉ HACE", 18f, color = col.sub, peso = FontWeight.ExtraBold, espaciado = 2.6f)
                        TB(actual?.titulo ?: "", 34f, titulo = true, lineas = 2)
                        TB(actual?.ayuda?.ifBlank { "Aprieta A para cambiarlo." } ?: "", 23f, color = col.sub, lineas = 6, interlineado = 32f)
                    }
                    if (!e.unaPantalla) Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(36))).border(2.dp, col.borde, RoundedCornerShape(esc.d(36))).padding(esc.d(26)),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(18)),
                    ) {
                        Box(Modifier.size(esc.d(56)).clip(CircleShape).background(col.elegido), contentAlignment = Alignment.Center) { TB(if (e.intercambiar) "↑" else "↓", 26f, color = col.enElegido, titulo = true, traducir = false) }
                        TB("Lo ves cambiar en vivo en la otra pantalla.", 22f, color = col.sub, lineas = 2, modifier = Modifier.weight(1f))
                    }
                }
            }
            PistasFinas("L1 R1" to "Sección", "↕" to "Opción", "◀ ▶" to "Cambiar sin abrir", "A" to "Abrir", "B" to "Volver a las carpetas")
        }
    }
}

private fun irASeccion(l: List<Opcion>, g: String) {
    val i = l.indexOfFirst { (it.grupo ?: "Carpeta") == g }
    if (i >= 0) Diptico.elegirIndice(i)
}

/** El panel vivo: la consola grande, tu historia con ella, cómo se verá lo que cambias, tu más jugado y un dato de DIPPO. */
@Composable
fun ConsolaVivaCV(e: Estado) {
    val c = Consolas.porId(e.consolaEditando)
    val opciones = remember(e) { Diptico.opcionesEditarConsola(e) }
    val grupo = opciones.getOrNull(e.indice)?.grupo ?: "Carpeta"
    LienzoBoceto(1240f, 1080f, Modifier.testTag("consola-viva-cv"), fondo = LocalColeccion.current.fondo, zoom = if (e.unaPantalla) 1f else ZOOM_ABAJO) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        if (c == null) {
            Box(Modifier.fillMaxSize().padding(esc.d(48)), contentAlignment = Alignment.Center) { PortadaCarpeta(e, e.consolaEditando, Modifier.fillMaxSize().clip(RoundedCornerShape(esc.d(36)))) }
            return@LienzoBoceto
        }
        val juegos = e.juegosPorConsola[c.id].orEmpty()
        val seg = juegos.sumOf { e.datos[it.clave]?.tiempo ?: 0L }
        val terminados = juegos.count { e.datos[it.clave]?.avance == AvanceJuego.TERMINADO.name }
        val favorito = juegos.maxByOrNull { e.datos[it.clave]?.tiempo ?: 0L }?.takeIf { (e.datos[it.clave]?.tiempo ?: 0L) > 0L }
        val ficha = HistoriaConsolas.de(c.id)
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(c.color.copy(alpha = .5f), Color.Transparent))))
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(44), vertical = esc.d(30)), verticalArrangement = Arrangement.spacedBy(esc.d(18))) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                PildoraVidrio(58f, onClick = { Diptico.cerrarPanel() }) {
                    Box(Modifier.size(esc.d(40), esc.d(36)).clip(CircleShape).border(2.dp, col.texto, CircleShape), contentAlignment = Alignment.Center) { TB("B", 15f, peso = FontWeight.ExtraBold, traducir = false) }
                    TB("Carpetas", 21f, peso = FontWeight.ExtraBold)
                }
                Spacer(Modifier.weight(1f))
                TB("TU ${c.bonito.uppercase()}", 19f, color = col.sub, peso = FontWeight.ExtraBold, espaciado = 2.2f, traducir = false)
            }
            // Consola + datos
            Row(Modifier.fillMaxWidth().weight(1.05f), horizontalArrangement = Arrangement.spacedBy(esc.d(24))) {
                Box(Modifier.weight(1f).fillMaxHeight()) { Consola3D(c, Modifier.fillMaxSize()) }
                Column(Modifier.weight(0.95f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(10), Alignment.CenterVertically)) {
                    TB(c.bonito, 48f, titulo = true, lineas = 2)
                    ficha?.let { TB("${it.marca.lowercase().replaceFirstChar { ch -> ch.uppercase() }} · ${it.anio}", 22f, color = col.sub, traducir = false) }
                    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                        ChipCifra(if (seg >= 3600) "${seg / 3600} h" else "${seg / 60} min", "jugadas")
                        ChipCifra("${juegos.size}", "juegos")
                        ChipCifra("$terminados", "terminados")
                    }
                    if (!c.sinEmulador) Row(
                        Modifier.clip(CircleShape).background(Color(0x2E2EC4A6)).border(1.dp, Color(0x802EC4A6), CircleShape).clickable { Diptico.elegirIndice(opciones.indexOfFirst { it.titulo == "Emulador" }.coerceAtLeast(0)) }
                            .padding(horizontal = esc.d(20), vertical = esc.d(12)),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10)),
                    ) {
                        Box(Modifier.size(esc.d(12)).clip(CircleShape).background(Color(0xFF2EC4A6)))
                        TB("Emulador: ${Diptico.nombreEmulador(c)}", 20f, peso = FontWeight.ExtraBold, traducir = false)
                    }
                }
            }
            // Así se verá · tu más jugado · DIPPO
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(24))) {
                AsiSeVera(e, c, grupo, favorito ?: juegos.firstOrNull(), Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(0.95f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(16))) {
                    Row(
                        Modifier.fillMaxWidth().weight(1f).vidrio(32f).padding(esc.d(18)),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16)),
                    ) {
                        if (favorito != null) {
                            ArteJuego(favorito, preferirArte = false, maxLado = 256, modifier = Modifier.size(esc.d(84)).clip(RoundedCornerShape(esc.d(18))))
                            Column(Modifier.weight(1f)) {
                                TB("MÁS JUGADO", 14f, color = col.sub, peso = FontWeight.ExtraBold, espaciado = 1.5f)
                                TB(favorito.titulo, 25f, titulo = true, lineas = 2, traducir = false)
                                val t = e.datos[favorito.clave]?.tiempo ?: 0L
                                TB(if (t >= 3600) "${t / 3600} h" else "${t / 60} min", 18f, color = col.sub, traducir = false)
                            }
                            Box(Modifier.height(esc.d(56)).clip(CircleShape).background(col.elegido).clickable { Diptico.jugarDesdeWidget(favorito.clave) }.padding(horizontal = esc.d(20)), contentAlignment = Alignment.Center) {
                                TB("Jugar", 20f, color = col.enElegido, peso = FontWeight.ExtraBold)
                            }
                        } else TB("Todavía no juegas nada de esta consola. ¡Prueba uno al azar!", 21f, color = col.sub, lineas = 3)
                    }
                    Row(
                        Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(esc.d(32))).background(Coral.copy(alpha = .12f))
                            .border(1.dp, Coral.copy(alpha = .35f), RoundedCornerShape(esc.d(32))).padding(horizontal = esc.d(22), vertical = esc.d(16)),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16)),
                    ) {
                        cl.nikalbazu.diptico.bisa.AvatarBisa(64f * esc.factor)
                        TB(ficha?.historia ?: "Cada consola guarda su historia. ¡Esta es parte de la tuya!", 19f, lineas = 4, interlineado = 25f, modifier = Modifier.weight(1f))
                    }
                }
            }
            // Accesos de la consola (cada uno hace algo)
            Row(Modifier.fillMaxWidth().height(esc.d(84)), horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                AccesoConsola("Ver sus juegos", Modifier.weight(1f)) { Diptico.verJuegosConsola(c.id) }
                AccesoConsola("Uno al azar", Modifier.weight(1f)) { Diptico.azarDeConsola(c.id) }
                if (!e.unaPantalla) AccesoConsola("Intercambiar pantallas", Modifier.weight(1f)) { Diptico.intercambiar() }
            }
        }
    }
}

@Composable
private fun ChipCifra(cifra: String, texto: String) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Row(Modifier.vidrio(24f).padding(horizontal = esc.d(14), vertical = esc.d(9)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
        TB(cifra, 25f, titulo = true, traducir = false)
        TB(texto, 16f, color = col.sub)
    }
}

@Composable
private fun AccesoConsola(texto: String, modifier: Modifier, accion: () -> Unit) {
    val esc = LocalEscala.current
    Box(modifier.fillMaxHeight().vidrio(30f).clickable(onClick = accion), contentAlignment = Alignment.Center) { TB(texto, 21f, peso = FontWeight.ExtraBold) }
}

/** «Así se verá»: la carpeta, la caja o el fondo, según la sección que estás cambiando (se puede elegir a mano). */
@Composable
private fun AsiSeVera(e: Estado, c: Consola, grupo: String, juego: cl.nikalbazu.diptico.data.Juego?, modifier: Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val auto = when (grupo) { "Cajas" -> 1; "Fondo", "Imágenes", "Juego y sonido" -> 2; else -> 0 }
    var elegida by remember(grupo) { mutableIntStateOf(auto) }
    Column(modifier.vidrio(36f).padding(esc.d(22)), verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
        TB("ASÍ SE VERÁ", 17f, color = col.sub, peso = FontWeight.ExtraBold, espaciado = 2f)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
            listOf("Carpeta", "Caja", "Fondo").forEachIndexed { i, n ->
                val on = i == elegida
                Box(Modifier.height(esc.d(42)).clip(CircleShape).background(if (on) col.elegido else col.texto.copy(alpha = .08f)).clickable { elegida = i }.padding(horizontal = esc.d(16)), contentAlignment = Alignment.Center) {
                    TB(n, 17f, color = if (on) col.enElegido else col.texto, peso = FontWeight.ExtraBold)
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when (elegida) {
                0 -> PortadaCarpeta(e, c.id, Modifier.fillMaxHeight().aspectoCuadrado().clip(RoundedCornerShape(esc.d(30))).border(4.dp, col.texto, RoundedCornerShape(esc.d(30))))
                1 -> if (juego != null) CajaJuego(juego, c, Modifier.fillMaxHeight(0.92f), estuche = true) else TB("Sin juegos todavía", 19f, color = col.sub)
                else -> Box(Modifier.fillMaxHeight().aspectRatio(16f / 9f).clip(RoundedCornerShape(esc.d(26))).border(3.dp, col.texto.copy(alpha = .5f), RoundedCornerShape(esc.d(26)))) {
                    FondoConsolaCV(e, c, Modifier.fillMaxSize())
                    Column(Modifier.align(Alignment.CenterStart).padding(start = esc.d(24))) {
                        TB(c.nombre, 34f, titulo = true)
                        TB(nombreModoFondo(modoFondo(e, c.id)), 15f, color = col.sub, peso = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

private fun Modifier.aspectoCuadrado(): Modifier = this.aspectRatio(1f)

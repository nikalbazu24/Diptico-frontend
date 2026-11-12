package cl.nikalbazu.diptico.ui

import cl.nikalbazu.diptico.estado.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.EdicionPerfil
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.FilasEdicion
import cl.nikalbazu.diptico.estado.INICIAL
import cl.nikalbazu.diptico.estado.PERSONAJES
import cl.nikalbazu.diptico.estado.Panel
import cl.nikalbazu.diptico.estado.Perfil
import cl.nikalbazu.diptico.estado.SocialDiscord

private val Claro = Color(0xFFF4F5FB)
private val Suave = Color(0xFFA5A8BD)
private val Casilla = Color.White.copy(alpha = 0.06f)
private val TintaEd = Color(0xFF14101A)

/** El fondo propio de cada personaje en la cuadrícula (como en el boceto). */
private val FONDO_PERSONAJE = mapOf(
    "gato" to 0xFFE64FA0, "robot" to 0xFF4B63D6, "bicho" to 0xFF23A874, "dragon" to 0xFFD9582F,
    "fantasma" to 0xFF7C4FC4, "caballero" to 0xFF5A6A8A, "zorro" to 0xFFC98A12, "pixel" to 0xFF178C87,
)

/** Dónde se dibuja el editor: abajo si hay dos pantallas; si no, en la pantalla del perfil. */
internal fun editorAqui(e: Estado, rol: Rol, miPantalla: Int): Boolean {
    if (e.extra.edicionPerfil == null || e.panel != Panel.NINGUNO) return false
    if (rol == Rol.INFERIOR && e.unaPantalla) return false
    return if (Diptico.edicionDividida(e)) rol == Rol.INFERIOR else rol == Rol.UNICA || e.extra.perfilEn == miPantalla
}

/**
 * Editar perfil (boceto «Abajo · Editar perfil», acomodado en horizontal): a la izquierda tu foto y los personajes;
 * a la derecha tus datos, el color y la privacidad. Arriba se ve la tarjeta tal como va quedando.
 * Nada se guarda hasta A · Guardar (o Start); B cancela.
 */
@Composable
fun EditorPerfilPantalla(e: Estado, rol: Rol, miPantalla: Int) {
    val visible = editorAqui(e, rol, miPantalla)
    // Para que la salida se anime con lo último que se vio.
    val ultimo = remember { mutableStateOf<EdicionPerfil?>(null) }
    e.extra.edicionPerfil?.let { ultimo.value = it }
    val animar = LocalMovimiento.current
    AnimatedVisibility(
        visible = visible,
        enter = if (animar) fadeIn(tween(220)) + slideInVertically(tween(300)) { it / 6 } else fadeIn(tween(0)),
        exit = if (animar) fadeOut(tween(180)) + slideOutVertically(tween(220)) { it / 8 } else fadeOut(tween(0)),
    ) {
        val ed = ultimo.value ?: return@AnimatedVisibility
        Editor(e, ed)
    }
}

@Composable
private fun Editor(e: Estado, ed: EdicionPerfil) {
    val esc = LocalEscala.current
    val acento = Color(ed.perfil.color)
    val conDiscord = e.extra.discordAvatar.isNotBlank()
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(if (Estilo.efectosCompletos) listOf(Color(0xD914152A), Color(0xE60B0C16)) else listOf(Color(0xF214152A), Color(0xF70B0C16))))
            .clickable(remember { MutableInteractionSource() }, indication = null) {}
            .testTag("editor-perfil"),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(16), vertical = esc.d(12)), verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
            // Encabezado: título y «B Volver».
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Texto("Editar perfil", 22f, color = Claro, peso = FontWeight.Black, familia = Nunito, lineas = 1, modifier = Modifier.weight(1f))
                Pildora("B  Volver", Color.White.copy(alpha = 0.1f), Claro, enfocada = false) { Diptico.cancelarEdicionPerfil() }
            }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                // Izquierda: foto y personajes.
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
                    TarjetaFoto(ed, acento, conDiscord)
                    EtiquetaEd("PERSONAJES")
                    Personajes(ed, acento)
                    if (!esc.angosta) Texto("Personajes originales de Díptico. Pronto habrá más.", 10.5f, color = Suave, familia = Nunito, lineas = 2)
                }
                // Derecha: datos, color y privacidad (se desliza sola hasta lo que eliges).
                Derecha(e, ed, acento, Modifier.weight(1.08f))
            }
            // Pie: A Guardar y B Cancelar.
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10)), verticalAlignment = Alignment.CenterVertically) {
                val enBotones = ed.fila == FilasEdicion.BOTONES
                Pildora("A  Guardar", acento, if (acento.luz() > 0.55f) TintaEd else Color.White, enfocada = enBotones && ed.col == 0, grande = true) {
                    Diptico.usarEdicionPerfil(FilasEdicion.BOTONES, 0)
                }
                Pildora("B  Cancelar", Color.White.copy(alpha = 0.1f), Claro, enfocada = enBotones && ed.col == 1, grande = true) {
                    Diptico.usarEdicionPerfil(FilasEdicion.BOTONES, 1)
                }
                Spacer(Modifier.weight(1f))
                Texto("Start también guarda", 10.5f, color = Suave, familia = Nunito, lineas = 1)
            }
        }
    }
}

/** La foto grande con el borde de tu color, y de dónde sale: un personaje, Discord, la galería o tu inicial. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun TarjetaFoto(ed: EdicionPerfil, acento: Color, conDiscord: Boolean) {
    val esc = LocalEscala.current
    val p = ed.perfil
    val actual = when {
        p.fotoDiscord && conDiscord -> "discord"
        p.foto.isNotBlank() -> "galeria"
        p.personaje == INICIAL -> INICIAL
        else -> "personaje"
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(18))).background(Casilla).padding(esc.d(12)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(12)),
    ) {
        FotoPerfil(p, if (esc.angosta) 58f else 70f)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
            EtiquetaEd("FOTO")
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(esc.d(6)), verticalArrangement = Arrangement.spacedBy(esc.d(6)),
            ) {
                FilasEdicion.fuentes(conDiscord).forEachIndexed { i, f ->
                    val elegida = f == actual
                    val texto = when (f) { "personaje" -> "Un personaje"; "discord" -> "La de Discord"; "galeria" -> "De la galería"; else -> "Mi inicial" }
                    Pildora(
                        texto, if (elegida) acento else Color.White.copy(alpha = 0.1f), if (elegida && acento.luz() > 0.55f) TintaEd else Claro,
                        enfocada = ed.fila == FilasEdicion.FOTO && ed.col == i,
                    ) { Diptico.usarEdicionPerfil(FilasEdicion.FOTO, i) }
                }
            }
        }
    }
}

/** Los 8 personajes en dos filas de 4: el elegido con borde blanco y aro de tu color. */
@Composable
private fun Personajes(ed: EdicionPerfil, acento: Color) {
    val esc = LocalEscala.current
    val p = ed.perfil
    val usandoPersonaje = p.foto.isBlank() && !p.fotoDiscord
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(16))).background(Casilla).padding(vertical = esc.d(8), horizontal = esc.d(4)),
        verticalArrangement = Arrangement.spacedBy(esc.d(6)),
    ) {
        for (fila in 0 until 2) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(2))) {
            for (c in 0 until 4) {
                val (id, nombre) = PERSONAJES[fila * 4 + c]
                val elegido = usandoPersonaje && p.personaje == id
                val enfocado = ed.fila == FilasEdicion.PERSONAJES_1 + fila && ed.col == c
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(esc.d(12)))
                        .then(if (enfocado) Modifier.background(acento.copy(alpha = 0.22f)).border(esc.d(1.5f), acento, RoundedCornerShape(esc.d(12))) else Modifier)
                        .clickable { Diptico.usarEdicionPerfil(FilasEdicion.PERSONAJES_1 + fila, c) }
                        .padding(horizontal = esc.d(1), vertical = esc.d(3))
                        .testTag("personaje-$id"),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(2)),
                ) {
                    val aro = if (elegido) Modifier.border(esc.d(2.5f), Color.White, CircleShape).padding(esc.d(2.5f)) else Modifier.padding(esc.d(2.5f))
                    Box(
                        Modifier.then(if (elegido) Modifier.border(esc.d(2f), acento, CircleShape).padding(esc.d(2f)) else Modifier.padding(esc.d(2f))).then(aro),
                    ) {
                        FotoPerfil(Perfil(color = FONDO_PERSONAJE[id] ?: p.color), if (esc.angosta) 34f else 40f, anillo = false, personaje = id)
                    }
                    Texto(nombre, if (esc.angosta) 9f else 10.5f, color = Claro, peso = if (elegido) FontWeight.Black else FontWeight.Bold, familia = Nunito, lineas = 1, traducir = false)
                }
            }
        }
    }
}

@Composable
private fun Derecha(e: Estado, ed: EdicionPerfil, acento: Color, modifier: Modifier) {
    val esc = LocalEscala.current
    val p = ed.perfil
    val kit = SocialDiscord.disponible
    val lista = rememberLazyListState()
    // La fila de la derecha que corresponde a lo elegido con el mando (para deslizar hasta ahí).
    val indice = when (ed.fila) {
        FilasEdicion.NOMBRE -> 1; FilasEdicion.APODO -> 2; FilasEdicion.FECHA -> 3; FilasEdicion.CONSOLA -> 4
        FilasEdicion.COLOR -> 5; FilasEdicion.MARCO -> 6
        FilasEdicion.PRESENCIA -> 8; FilasEdicion.SALUDO -> if (kit) 9 else 8; FilasEdicion.HORAS -> if (kit) 10 else 9
        else -> -1
    }
    LaunchedEffect(indice) { if (indice >= 0) lista.animateScrollToItem((indice - 1).coerceAtLeast(0)) }
    val consola = remember(p.consolaFav, e.consolasConJuegos) {
        if (p.consolaFav.isBlank()) "La más jugada" else e.consolasConJuegos.firstOrNull { it.id == p.consolaFav }?.bonito ?: p.consolaFav
    }
    LazyColumn(modifier, state = lista, verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
        item { EtiquetaEd("DATOS") }
        item { FilaDato("Nombre", "Cómo te saluda DIPPO", ed.nombre.ifBlank { "Sin nombre" }, acento, ed.fila == FilasEdicion.NOMBRE, traducirValor = ed.nombre.isBlank()) { Diptico.usarEdicionPerfil(FilasEdicion.NOMBRE) } }
        item { FilaDato("Apodo", "Lo ven tus amigos", if (p.apodo.isBlank()) "Sin apodo" else "@${p.apodo}", acento, ed.fila == FilasEdicion.APODO, traducirValor = p.apodo.isBlank()) { Diptico.usarEdicionPerfil(FilasEdicion.APODO) } }
        item {
            val f = p.nacimiento.split("-").takeIf { it.size == 3 }?.reversed()?.joinToString(" / ") ?: "Sin fecha"
            FilaDato("Fecha de nacimiento", "Para saber tu edad (Discord la pide). No se muestra a nadie.", f, acento, ed.fila == FilasEdicion.FECHA, traducirValor = p.nacimiento.isBlank()) { Diptico.usarEdicionPerfil(FilasEdicion.FECHA) }
        }
        item {
            FilaDato("Consola favorita", "Sale en tu tarjeta · ← → para cambiarla", "‹ $consola ›", acento, ed.fila == FilasEdicion.CONSOLA, traducirValor = p.consolaFav.isBlank()) {
                Diptico.usarEdicionPerfil(FilasEdicion.CONSOLA)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
                Spacer(Modifier.height(esc.d(2)))
                EtiquetaEd("COLOR DE MI PERFIL")
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(14))).background(Casilla)
                        .then(if (ed.fila == FilasEdicion.COLOR) Modifier.border(esc.d(1.5f), acento.copy(alpha = 0.7f), RoundedCornerShape(esc.d(14))) else Modifier)
                        .padding(horizontal = esc.d(10), vertical = esc.d(8)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Perfil.COLORES.forEachIndexed { i, (nombre, c) ->
                        val col = Color(c)
                        val elegido = p.color == c
                        val enfocado = ed.fila == FilasEdicion.COLOR && ed.col == i
                        Box(
                            Modifier.size(esc.d(26))
                                .then(if (enfocado) Modifier.offset(y = esc.d(-2)) else Modifier)
                                .then(if (elegido) Modifier.border(esc.d(2f), col, CircleShape).padding(esc.d(2f)) else Modifier)
                                .clip(CircleShape).background(col)
                                .then(if (elegido) Modifier.border(esc.d(2.5f), Color.White, CircleShape) else Modifier)
                                .then(if (enfocado && !elegido) Modifier.border(esc.d(2f), Color.White.copy(alpha = 0.6f), CircleShape) else Modifier)
                                .clickable { Diptico.usarEdicionPerfil(FilasEdicion.COLOR, i) }
                                .testTag("color-$nombre"),
                        )
                    }
                }
                Texto("Es el acento de tu tarjeta y de los botones de tu perfil.", 10.5f, color = Suave, familia = Nunito, lineas = 2)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
                Spacer(Modifier.height(esc.d(2)))
                EtiquetaEd("BORDE DE MI FOTO")
                val enMarco = ed.fila == FilasEdicion.MARCO
                val scroll = androidx.compose.foundation.rememberScrollState()
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(14))).background(Casilla)
                        .then(if (enMarco) Modifier.border(esc.d(1.5f), acento.copy(alpha = 0.7f), RoundedCornerShape(esc.d(14))) else Modifier)
                        .horizontalScroll(scroll).padding(horizontal = esc.d(10), vertical = esc.d(10)),
                    horizontalArrangement = Arrangement.spacedBy(esc.d(12)),
                ) {
                    MARCOS.forEachIndexed { i, m ->
                        val libre = Diptico.marcoDesbloqueado(m)
                        val elegido = p.marco == m.id
                        val enfocado = enMarco && ed.col == i
                        Box(
                            Modifier.size(esc.d(50))
                                .graphicsLayer { alpha = if (libre) 1f else 0.38f }
                                .then(if (enfocado) Modifier.border(esc.d(2f), Color.White.copy(alpha = 0.7f), CircleShape) else Modifier)
                                .then(if (elegido) Modifier.border(esc.d(2.5f), Color.White, CircleShape) else Modifier)
                                .clickable { Diptico.usarEdicionPerfil(FilasEdicion.MARCO, i) }
                                .testTag("marco-${m.id}"),
                            contentAlignment = Alignment.Center,
                        ) { FotoPerfil(p, 36f, marco = m.id) }
                    }
                }
                // Qué marco está mirando el mando: su nombre y cómo se gana (o que ya es tuyo).
                val mirado = MARCOS.getOrNull(if (enMarco) ed.col else MARCOS.indexOfFirst { it.id == p.marco }.coerceAtLeast(0)) ?: MARCOS.first()
                val libreMirado = Diptico.marcoDesbloqueado(mirado)
                Texto(mirado.nombre + (if (libreMirado) "" else " · bloqueado"), 12f, color = Claro, peso = FontWeight.Bold, lineas = 1)
                Texto(if (libreMirado) mirado.detalle else "Se gana con un logro de DIPPO. ${mirado.detalle}", 10.5f, color = Suave, familia = Nunito, lineas = 2)
            }
        }
        item { Spacer(Modifier.height(esc.d(2))); EtiquetaEd("PRIVACIDAD Y PERSONALIZACIÓN") }
        if (kit) item { Interruptor("Mostrar «Jugando a…» en Discord", ed.presencia, acento, ed.fila == FilasEdicion.PRESENCIA) { Diptico.usarEdicionPerfil(FilasEdicion.PRESENCIA) } }
        item { Interruptor("DIPPO me saluda al llegar", ed.saludo, acento, ed.fila == FilasEdicion.SALUDO) { Diptico.usarEdicionPerfil(FilasEdicion.SALUDO) } }
        item { Interruptor("Mostrar mis horas en la tarjeta", p.mostrarHoras, acento, ed.fila == FilasEdicion.HORAS) { Diptico.usarEdicionPerfil(FilasEdicion.HORAS) } }
        item { Spacer(Modifier.height(esc.d(4))) }
    }
}

@Composable
private fun FilaDato(titulo: String, ayuda: String, valor: String, acento: Color, enfocada: Boolean, traducirValor: Boolean = false, alTocar: () -> Unit) {
    val esc = LocalEscala.current
    val forma = RoundedCornerShape(esc.d(14))
    Row(
        Modifier.fillMaxWidth().clip(forma).background(if (enfocada) acento.copy(alpha = 0.2f) else Casilla)
            .then(if (enfocada) Modifier.border(esc.d(1.5f), acento, forma) else Modifier)
            .clickable(onClick = alTocar).padding(horizontal = esc.d(12), vertical = esc.d(8)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10)),
    ) {
        Column(Modifier.weight(1f)) {
            Texto(titulo, 13f, color = Claro, peso = FontWeight.ExtraBold, familia = Nunito, lineas = 1)
            Texto(ayuda, 10.5f, color = Suave, familia = Nunito, lineas = 2)
        }
        Texto(valor, 13f, color = acento, peso = FontWeight.ExtraBold, familia = Nunito, lineas = 1, traducir = traducirValor, alinear = TextAlign.End,
            modifier = Modifier.widthIn(max = esc.d(118)))
    }
}

@Composable
private fun Interruptor(titulo: String, encendido: Boolean, acento: Color, enfocada: Boolean, alTocar: () -> Unit) {
    val esc = LocalEscala.current
    val forma = RoundedCornerShape(esc.d(14))
    Row(
        Modifier.fillMaxWidth().clip(forma).background(if (enfocada) acento.copy(alpha = 0.2f) else Casilla)
            .then(if (enfocada) Modifier.border(esc.d(1.5f), acento, forma) else Modifier)
            .clickable(onClick = alTocar).padding(horizontal = esc.d(12), vertical = esc.d(8)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10)),
    ) {
        Texto(titulo, 12.5f, color = Claro, peso = FontWeight.ExtraBold, familia = Nunito, lineas = 2, modifier = Modifier.weight(1f))
        Box(Modifier.width(esc.d(40)).height(esc.d(23)).clip(RoundedCornerShape(50)).background(if (encendido) acento else Color.White.copy(alpha = 0.22f))) {
            Box(
                Modifier.padding(esc.d(3)).size(esc.d(17)).align(if (encendido) Alignment.CenterEnd else Alignment.CenterStart)
                    .clip(CircleShape).background(Color.White),
            )
        }
    }
}

@Composable
private fun EtiquetaEd(texto: String) =
    Texto(texto, 10.5f, color = Suave, peso = FontWeight.ExtraBold, familia = Nunito, espaciado = 1.4f, lineas = 1)

/** Un botón en forma de píldora; elegido con el mando se marca con un borde blanco. */
@Composable
private fun Pildora(texto: String, fondo: Color, tinta: Color, enfocada: Boolean, grande: Boolean = false, alTocar: () -> Unit) {
    val esc = LocalEscala.current
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(fondo)
            .then(if (enfocada) Modifier.border(esc.d(2f), Color.White, RoundedCornerShape(50)) else Modifier)
            .clickable(onClick = alTocar)
            .padding(horizontal = esc.d(if (grande) 16 else 10), vertical = esc.d(if (grande) 7 else 5)),
    ) { Texto(texto, if (grande) 13.5f else 11.5f, color = tinta, peso = FontWeight.ExtraBold, familia = Nunito, lineas = 1) }
}

private fun Color.luz(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

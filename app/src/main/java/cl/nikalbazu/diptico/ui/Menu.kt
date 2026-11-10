package cl.nikalbazu.diptico.ui

import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Opcion
import cl.nikalbazu.diptico.estado.Seccion

fun colorSeccion(s: Seccion): Color = when (s) {
    Seccion.ESENCIAL -> Color(0xFFFF6E8A)
    Seccion.PANTALLAS -> Color(0xFF6B4EFF)
    Seccion.INICIO -> Color(0xFF00ACC1)
    Seccion.ESTANTE -> Color(0xFFFF8A5B)
    Seccion.TEMAS -> Color(0xFFB388FF)
    Seccion.DECORACIONES -> Color(0xFFF4AB61)
    Seccion.APARIENCIA -> Color(0xFFFF5D8F)
    Seccion.MUSICA -> Color(0xFFFF7A59)
    Seccion.EMULADORES -> Color(0xFF3BA8FF)
    Seccion.PORTADAS -> Color(0xFFFFC93C)
    Seccion.LOGROS -> Color(0xFFE0A030)
    Seccion.ASISTENTE -> Color(0xFF7C4DFF)
    Seccion.IDIOMA -> Color(0xFF29B6F6)
    Seccion.BIBLIOTECA -> Color(0xFF2EC4A6)
    Seccion.CONSOLA -> Color(0xFF8BC34A)
    Seccion.SISTEMA -> Color(0xFF8E8AAE)
    Seccion.CONSOLAS -> Color(0xFFFF5D5D)
    Seccion.SOBREMESA -> Color(0xFF26A69A)
    Seccion.CONTROLES -> Color(0xFFFFA726)
    Seccion.RENDIMIENTO -> Color(0xFFFF5252)
    Seccion.MAS -> Color(0xFF9AA4B8)
    Seccion.G_ESTILO -> Color(0xFFFF5D8F)
    Seccion.G_WIDGETS -> Color(0xFF00ACC1)
    Seccion.G_TEMAS -> Color(0xFFB388FF)
    Seccion.G_BIBLIOTECA -> Color(0xFF2EC4A6)
    Seccion.G_JUEGOS -> Color(0xFF3BA8FF)
    Seccion.G_CONTROLES -> Color(0xFFFFA726)
    Seccion.G_SONIDO -> Color(0xFFFF7A59)
    Seccion.G_DIPPO -> Color(0xFFFF9A6B)
    Seccion.G_SISTEMA -> Color(0xFF8E8AAE)
    Seccion.G_CUENTAS -> Color(0xFF5865F2)
    Seccion.G_PERFIL, Seccion.PERFIL -> Color(0xFFFF7A59)
}

private fun tintaSobre(c: Color) = if (c.luminance() > 0.55f) Tinta else Color.White

/**
 * El menú de ajustes: a la izquierda todas las secciones (siempre a la vista), a la derecha sus opciones.
 * Arriba de las secciones, "Buscar un ajuste" (Y). Así nada queda escondido.
 */
@Composable
fun PanelMenu(e: Estado, conAyuda: Boolean) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    conAyudaAngosta = conAyuda && !esc.angosta
    // El menú llega flotando: sube un poco, crece apenas y se posa (según Animaciones y Velocidad).
    val mov = ajusteMovimiento()
    val entrada = remember { androidx.compose.animation.core.Animatable(if (mov.activo) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (mov.activo) entrada.animateTo(1f, tween(mov.ms(340), easing = CurvaPosarse)) else entrada.snapTo(1f)
    }
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Row(Modifier.fillMaxSize().graphicsLayer {
            alpha = entrada.value
            translationY = esc.d(28).toPx() * mov.intensidad * (1f - entrada.value)
            val s = 1f - 0.03f * mov.intensidad * (1f - entrada.value)
            scaleX = s; scaleY = s
        }) {
            // Con una sola pantalla la columna de secciones es más angosta, para dejar lugar a la vista previa.
            ColumnaSecciones(e, Modifier.width(esc.d(if (esc.angosta || conAyuda) 156f else 196f)).fillMaxHeight())
            Box(Modifier.fillMaxHeight().width(esc.d(1)).background(p.linea))
            PaginaSeccion(e, conAyuda, Modifier.weight(1f).fillMaxHeight())
            // Con una sola pantalla, la vista previa va a la derecha del menú.
            if (conAyuda && !esc.angosta) {
                val o = if (e.enOpciones) Diptico.opcionesSeccion(e.seccion).getOrNull(e.indice) else null
                PreviaAjuste(e, e.seccion, o, Modifier.width(esc.d(220)).fillMaxHeight().padding(start = esc.d(4), end = esc.d(12), top = esc.d(14), bottom = esc.d(14)))
            }
        }
        // Menú desplegable de una opción con varias alternativas, encima de todo el menú.
        androidx.compose.animation.AnimatedVisibility(
            visible = e.extra.desplegable >= 0 && e.enOpciones,
            enter = entradaFlotante(mov), exit = salidaFlotante(mov),
        ) { VentanaDesplegable(e) }
    }
}

/** La lista de alternativas de una opción: se mueve con la cruceta, A elige y B vuelve sin cambiar nada. */
@Composable
private fun VentanaDesplegable(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val o = Diptico.opcionesSeccion(e.seccion).getOrNull(e.extra.desplegable) ?: return
    val nombres = o.alternativas ?: return
    val color = colorSeccion(e.seccion)
    val lista = rememberLazyListState()
    LaunchedEffect(e.extra.desplegableSel) { lista.mostrar(e.extra.desplegableSel) }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null,
        ) { Diptico.cerrarDesplegable() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = esc.d(460)).fillMaxWidth(0.86f).heightIn(max = esc.d(520))
                .shadow(esc.d(18), RoundedCornerShape(esc.d(24))).clip(RoundedCornerShape(esc.d(24))).background(p.fondo2)
                .border(esc.d(1.5f), color.copy(alpha = 0.5f), RoundedCornerShape(esc.d(24)))
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
                .padding(vertical = esc.d(14)),
        ) {
            Row(Modifier.padding(horizontal = esc.d(18)), verticalAlignment = Alignment.CenterVertically) {
                o.icono?.let { IconoDiptico(it, color, 20f, acento = color); Spacer(Modifier.width(esc.d(10))) }
                Texto(o.titulo, 18f, lineas = 1, modifier = Modifier.weight(1f))
            }
            if (o.ayuda.isNotBlank()) Detalle(o.ayuda, 12f, lineas = 2, modifier = Modifier.padding(horizontal = esc.d(18), vertical = esc.d(6)))
            LazyColumn(
                state = lista, modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = esc.d(10), vertical = esc.d(6)),
                verticalArrangement = Arrangement.spacedBy(esc.d(4)),
            ) {
                itemsIndexed(nombres) { i, nombre ->
                    val marcada = i == e.extra.desplegableSel
                    val puesta = i == o.elegida
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = esc.d(48)).clip(RoundedCornerShape(esc.d(14)))
                            .background(if (marcada) color.copy(alpha = 0.22f) else Color.Transparent)
                            .then(if (marcada) Modifier.border(esc.d(1.5f), color, RoundedCornerShape(esc.d(14))) else Modifier)
                            .clickable { Diptico.elegirDesplegable(i) }
                            .padding(horizontal = esc.d(14)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        o.muestrasIconos?.getOrNull(i)?.let { estilo ->
                            // Una muestra del estilo: tres consolas dibujadas con él.
                            androidx.compose.runtime.CompositionLocalProvider(LocalIconos provides IconosCfg(estilo, emptyMap(), e.iconosVersion)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(4))) {
                                    listOf("snes", "psx", "gba").forEach { id -> IconoConsola(cl.nikalbazu.diptico.data.Consolas.porId(id), 22f, LocalPaleta.current.fondo2) }
                                }
                            }
                            Spacer(Modifier.width(esc.d(12)))
                        }
                        Texto(nombre, 15f, lineas = 2, modifier = Modifier.weight(1f))
                        if (puesta) IconoDiptico(Icono.CHECK, color, 18f, acento = color)
                    }
                }
            }
            Pistas("↑ ↓" to "Moverte", "A" to "Elegir", "B" to "Volver", modifier = Modifier.padding(start = esc.d(18), top = esc.d(6)))
        }
    }
}

@Composable
private fun ColumnaSecciones(e: Estado, modifier: Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val secciones = remember(e.menuSimple) { Diptico.seccionesMenu(e.menuSimple) }
    // Secciones con el título de su grupo encima (Empieza aquí, Cómo se ve, Tus juegos, Asistente, Consola).
    val filas = remember(secciones) { secciones.flatMap { s -> if (s.grupo.isNotEmpty()) listOf<Any>(s.grupo, s) else listOf<Any>(s) } }
    val lista = rememberLazyListState()
    LaunchedEffect(e.seccion) {
        // Una sección avanzada abierta desde «Más ajustes» marca «Más ajustes».
        val i = filas.indexOf(e.seccion).takeIf { it >= 0 } ?: filas.indexOf(Seccion.MAS)
        if (i < 0) return@LaunchedEffect
        // Solo se desplaza si la sección elegida quedó fuera de la vista (así no salta en cada paso).
        if (i > 0 && filas[i - 1] is String) lista.mostrar(i - 1)
        lista.mostrar(i)
    }
    Column(modifier.background(p.fondo.copy(alpha = 0.55f)).padding(top = esc.d(10), bottom = esc.d(8))) {
        Row(Modifier.padding(horizontal = esc.d(12)), verticalAlignment = Alignment.CenterVertically) {
            MarcaLogo(12f)
            Spacer(Modifier.width(esc.d(8)))
            Texto("Ajustes", 18f, lineas = 1)
        }
        Spacer(Modifier.height(esc.d(8)))
        // Buscar un ajuste
        Row(
            Modifier.padding(horizontal = esc.d(8)).fillMaxWidth().clip(RoundedCornerShape(50)).background(p.superficie)
                .clickable { Diptico.abrirTexto("buscar_ajuste", e.buscarAjuste) }.padding(horizontal = esc.d(10), vertical = esc.d(7)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconoDiptico(Icono.BUSCAR, p.tenue, 15f, acento = Coral)
            Spacer(Modifier.width(esc.d(6)))
            Texto(e.buscarAjuste.ifBlank { "Buscar" }, 12.5f, color = p.tenue, lineas = 1, modifier = Modifier.weight(1f))
            Texto("Y", 11f, color = p.tenue, traducir = false)
        }
        Spacer(Modifier.height(esc.d(6)))
        LazyColumn(
            state = lista, modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = esc.d(8), vertical = esc.d(4)),
            verticalArrangement = Arrangement.spacedBy(esc.d(3)),
        ) {
            itemsIndexed(filas, key = { _, f -> if (f is Seccion) f.name else "g_$f" }) { k, f ->
                if (f is String) {
                    Etiqueta(f, p.tenue.copy(alpha = 0.8f), 10f, Modifier.padding(start = esc.d(8), top = esc.d(if (k == 0) 2f else 10f), bottom = esc.d(2)))
                    return@itemsIndexed
                }
                val s = f as Seccion
                val activa = s == e.seccion || (s == Seccion.MAS && e.seccion !in secciones)
                val color = colorSeccion(s)
                val enfoque = activa && !e.enOpciones
                val fondo by animateColorAsState(
                    when { enfoque -> color.copy(alpha = 0.32f); activa -> color.copy(alpha = 0.16f); else -> Color.Transparent },
                    tween(140), label = "seccion",
                )
                Row(
                    Modifier.fillMaxWidth().height(esc.d(42)).clip(RoundedCornerShape(esc.d(14))).background(fondo)
                        .then(if (enfoque) Modifier.border(esc.d(2), color, RoundedCornerShape(esc.d(14))) else Modifier)
                        .clickable { Diptico.elegirSeccion(s) }.padding(horizontal = esc.d(8)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(esc.d(28)).clip(CircleShape).background(if (activa) color else color.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) { IconoDiptico(s.icono, if (activa) tintaSobre(color) else color, 16f, acento = if (activa) tintaSobre(color) else color) }
                    Spacer(Modifier.width(esc.d(8)))
                    Texto(s.titulo, 13.5f, color = if (activa) p.tinta else p.tenue, lineas = 1)
                }
            }
        }
        Pistas("L1 R1" to "Sección", modifier = Modifier.padding(start = esc.d(12), top = esc.d(4)))
    }
}

/** ¿Es una opción de Sí/No? Entonces se dibuja como interruptor. */
private fun esInterruptor(o: Opcion) = o.valor == "Sí" || o.valor == "No"

@Composable
private fun PaginaSeccion(e: Estado, conAyuda: Boolean, modifier: Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val s = e.seccion
    val color = colorSeccion(s)
    // Las opciones se arman solo cuando cambia algo (no en cada cuadro de una animación).
    val opciones = remember(s, e) { Diptico.opcionesSeccion(s) }
    Column(modifier.graphicsLayer { alpha = if (e.enOpciones) 1f else 0.82f }) {
        // Encabezado con el color de la sección.
        Row(
            Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.3f), Color.Transparent)))
                .padding(start = esc.d(14), end = esc.d(10), top = esc.d(10), bottom = esc.d(10)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Texto(s.titulo, 20f, lineas = 1)
                Detalle(s.ayuda, 12f, lineas = 1)
            }
            // Sin barra de la hora arriba (diseño nuevo): la hora, la batería y los avisos van aquí, chiquitos.
            if (!LocalBarraSistema.current) {
                EstadoChico()
                Spacer(Modifier.width(esc.d(10)))
            }
            Box(
                Modifier.size(esc.d(34)).clip(CircleShape).background(p.superficie).clickable { Diptico.cerrarPanel() },
                contentAlignment = Alignment.Center,
            ) { IconoDiptico(Icono.CERRAR, p.tinta, 16f) }
        }
        val lista = rememberLazyListState()
        LaunchedEffect(e.indice, e.enOpciones) {
            if (!e.enOpciones || opciones.isEmpty()) { lista.scrollToItem(0); return@LaunchedEffect }
            lista.mostrar(e.indice)
        }
        LazyColumn(
            state = lista, modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = esc.d(12), end = esc.d(12), top = esc.d(8), bottom = esc.d(8)),
            verticalArrangement = Arrangement.spacedBy(esc.d(6)),
        ) {
            itemsIndexed(opciones) { i, o ->
                // Título de grupo cuando empieza uno nuevo.
                val g = Diptico.grupoOpcion(s, o)
                Column {
                    if (g.isNotEmpty() && (i == 0 || Diptico.grupoOpcion(s, opciones[i - 1]) != g)) {
                        Etiqueta(g, color, 10.5f, Modifier.padding(start = esc.d(6), top = esc.d(if (i == 0) 0f else 6f), bottom = esc.d(5)))
                    }
                    // Sin animación por fila (era lo que más pesaba al abrir una sección): la sección entera se funde.
                    FilaOpcion(o, e.enOpciones && i == e.indice, color) { Diptico.elegirIndice(i) }
                }
            }
        }
        if (conAyuda && e.enOpciones) {
            val o = opciones.getOrNull(e.indice)
            Detalle(o?.ayuda?.ifEmpty { null } ?: s.ayuda, 13.5f, lineas = 2, modifier = Modifier.padding(horizontal = esc.d(14)))
        }
        Pistas(
            *(if (e.enOpciones) arrayOf("↑ ↓" to "Moverte", "A" to "Cambiar", "B" to "Secciones") else arrayOf("↑ ↓" to "Sección", "A" to "Entrar", "B" to "Cerrar")),
            modifier = Modifier.padding(start = esc.d(14), top = esc.d(4), bottom = esc.d(10)),
        )
    }
}

/** Con una sola pantalla y vista previa al lado, los valores se acortan para que el título no se corte. */
private var conAyudaAngosta = false

/**
 * Una opción del menú: ícono, título, y a la derecha su valor o un interruptor.
 * El título nunca se corta: si la fila es angosta o el valor es largo, el valor pasa a una segunda línea
 * debajo del título en vez de ir en la píldora de la derecha.
 */
@Composable
fun FilaOpcion(o: Opcion, activa: Boolean, color: Color, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val fondo by animateColorAsState(if (activa) p.superficie else p.superficie.copy(alpha = 0.45f), label = "fila")
    if (o.rango != null) { ControlGlass(o, color); return }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val interruptor = esInterruptor(o)
        val ancho = maxWidth
        val angosta = ancho < esc.d(400) || conAyudaAngosta
        val valorAbajo = o.valor.isNotEmpty() && !interruptor && (o.valor.length + o.titulo.length > (if (angosta) 24 else 40))
        Row(
            Modifier.fillMaxWidth().heightIn(min = esc.d(54)).clip(RoundedCornerShape(esc.d(16))).background(fondo)
                .then(if (activa) Modifier.border(esc.d(2), color, RoundedCornerShape(esc.d(16))) else Modifier)
                .clickable { alPulsar() }
                .graphicsLayer { alpha = if (o.apagada) 0.5f else 1f }
                .padding(horizontal = esc.d(12), vertical = esc.d(7)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            o.icono?.let {
                Box(Modifier.size(esc.d(34)).clip(CircleShape).background(color.copy(alpha = if (activa) 0.3f else 0.14f)), contentAlignment = Alignment.Center) {
                    IconoDiptico(it, p.tinta, 18f, acento = color)
                }
                Spacer(Modifier.width(esc.d(12)))
            }
            val largo = o.titulo.length > 22
            Column(Modifier.weight(1f)) {
                Texto(o.titulo, if (largo) 14.5f else 15.5f, lineas = 2, interlineado = if (largo) 16.5f else 18f)
                if (valorAbajo) Texto(o.valor, 12.5f, color = if (activa) color else p.tenue, familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1)
            }
            Spacer(Modifier.width(esc.d(8)))
            when {
                interruptor -> InterruptorLiquido(o.valor == "Sí", color)
                o.titulo == "Modo" && (o.valor == "Oscuro" || o.valor == "Claro") -> InterruptorDiaNoche(o.valor == "Oscuro")
                o.valor.isNotEmpty() && !valorAbajo -> Box(
                    Modifier.clip(RoundedCornerShape(50)).background(if (activa) color.copy(alpha = 0.22f) else p.fondo2.copy(alpha = 0.6f))
                        .padding(horizontal = esc.d(10), vertical = esc.d(4)),
                ) {
                    // ▾ avisa que al elegirla se abre una lista con todas las alternativas.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Texto(o.valor, 12.5f, color = p.tinta, familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1, modifier = Modifier.widthIn(max = ancho * 0.33f))
                        if (o.alternativas != null) Texto(" ▾", 12.5f, color = p.tenue, lineas = 1, traducir = false)
                    }
                }
                valorAbajo -> if (o.alternativas != null) Texto("▾", 16f, color = p.tenue, traducir = false)
                else -> Texto("›", 20f, color = p.tenue)
            }
        }
    }
}

/** En la otra pantalla, mientras el menú está abierto: explica la sección y la opción elegida, en grande. */
@Composable
fun AyudaMenu(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val s = e.seccion
    val opciones = remember(s, e) { Diptico.opcionesSeccion(s) }
    val o = if (e.enOpciones) opciones.getOrNull(e.indice) else null
    val color = colorSeccion(s)
    Box(Modifier.fillMaxSize().background(p.fondo)) {
        Box(
            Modifier.align(Alignment.CenterEnd).size(esc.d(520))
                .background(Brush.radialGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent))),
        )
        val angosta = esc.angosta
        if (!angosta) BarraSuperior(Modifier.align(Alignment.TopStart), trabajos = e.trabajos)
        AnimatedContent(
            targetState = s to (if (e.enOpciones) e.indice else -1),
            // Transición corta: así recorrer opciones con la cruceta se siente fluido.
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(90)) },
            label = "ayuda",
            modifier = Modifier.fillMaxSize().padding(
                start = esc.d(if (angosta) 20 else 36), end = esc.d(if (angosta) 20 else 36),
                top = esc.d(if (angosta) 18 else 60), bottom = esc.d(if (angosta) 46 else 54),
            ),
        ) { (sec, indice) ->
            val opcion = if (sec == s && indice >= 0) opciones.getOrNull(indice) else null
            val titulo = opcion?.titulo
            val valor = opcion?.valor
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(0.9f).then(if (angosta) Modifier.fillMaxHeight() else Modifier), verticalArrangement = Arrangement.spacedBy(esc.d(if (angosta) 7 else 10))) {
                    Etiqueta("Menú · ${sec.titulo}", color, 13f)
                    Texto(titulo ?: sec.titulo, if (angosta) 25f else 38f, lineas = 2, interlineado = if (angosta) 29f else 42f)
                    if (!valor.isNullOrEmpty()) {
                        Box(
                            Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.18f))
                                .padding(horizontal = esc.d(14), vertical = esc.d(6)),
                        ) { Texto(valor, 15f, color = p.tinta, lineas = 1) }
                    }
                    Detalle(
                        (if (titulo != null && sec == s) o?.ayuda else null)?.ifEmpty { null } ?: sec.ayuda,
                        if (angosta) 14.5f else 17f, lineas = if (angosta) 3 else 4,
                    )
                    // En pantallas angostas (la de abajo) la vista previa va debajo del texto, con lo que queda de alto.
                    if (angosta) PreviaAjuste(e, sec, if (sec == s) o else null, Modifier.weight(1f).fillMaxWidth())
                }
                if (!esc.angosta) {
                    Spacer(Modifier.width(esc.d(24)))
                    PreviaAjuste(e, sec, if (sec == s) o else null, Modifier.weight(1.1f).fillMaxHeight())
                }
            }
        }
        Pistas(
            "A" to "Cambiar", "B" to "Volver", "Start" to "Cerrar",
            modifier = Modifier.align(Alignment.BottomStart).padding(start = esc.d(if (angosta) 20 else 36), bottom = esc.d(if (angosta) 12 else 20)),
        )
    }
}


// ---------- Vistas previas del menú ----------

/** Un inicio en miniatura pintado con los colores del tema. */
@Composable
fun PreviaTema(t: cl.nikalbazu.diptico.estado.Tema, oscuro: Boolean, animar: Boolean = false) {
    val esc = LocalEscala.current
    val pal = remember(t, oscuro) { paletaDe(t, oscuro) }
    val a = Color(t.acento)
    val a2 = Color(t.acento2)
    val a3 = Color(t.acento3)
    val fuente = fuenteTema(t.fuente)
    Box(Modifier.fillMaxSize().background(pal.fondo)) {
        androidx.compose.runtime.CompositionLocalProvider(LocalPaleta provides pal) { TramaTema(t) }
        if (t.decor != "nada") Decoracion(t, animada = animar)
        if (t.trama.isBlank()) Box(Modifier.align(Alignment.TopEnd).size(esc.d(160)).background(Brush.radialGradient(listOf(a.copy(alpha = 0.23f), Color.Transparent))))
        Column(Modifier.fillMaxSize().padding(esc.d(12)), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(esc.d(14)).clip(CircleShape).background(a))
                Spacer(Modifier.width(esc.d(6)))
                Texto(t.nombre, 13f, color = pal.tinta, lineas = 1, modifier = Modifier.weight(1f), familia = fuente)
                Texto("12:30", 11f, color = pal.tenue, familia = fuente)
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(6)), verticalAlignment = Alignment.Bottom) {
                listOf(a, a2, a3, a.copy(alpha = 0.6f)).forEachIndexed { i, c ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight(if (i == 0) 1f else 0.82f)
                            .clip(formaTema(esc.d(8), 0, androidx.compose.foundation.shape.RoundedCornerShape(esc.d(8)), t.forma))
                            .background(Brush.verticalGradient(listOf(c, lerpColor(c, pal.fondo, 0.45f)))),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(formaTema(null, 50, androidx.compose.foundation.shape.RoundedCornerShape(50), t.forma))
                        .background(if (t.acabado == "liso") Brush.linearGradient(listOf(a, a)) else Brush.linearGradient(listOf(a, a2)))
                        .padding(horizontal = esc.d(12), vertical = esc.d(5)),
                ) { Texto("▶ Jugar", 11.5f, color = if (a.luminance() > 0.55f) Tinta else Color.White, familia = fuente) }
                Spacer(Modifier.width(esc.d(8)))
                Box(Modifier.weight(1f).height(esc.d(8)).clip(RoundedCornerShape(50)).background(pal.superficie))
            }
        }
    }
}

private fun lerpColor(a: Color, b: Color, f: Float) = androidx.compose.ui.graphics.lerp(a, b, f)

/** Barras que bailan con la canción elegida. */
@Composable
fun PreviaMusica(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val pista = cl.nikalbazu.diptico.audio.Musica.pistas.firstOrNull { it.id == e.pista }
    val tr = androidx.compose.animation.core.rememberInfiniteTransition(label = "eq")
    val fases = (0 until 9).map { i ->
        tr.animateFloat(
            0.2f, 1f,
            androidx.compose.animation.core.infiniteRepeatable(tween(380 + i * 70), androidx.compose.animation.core.RepeatMode.Reverse),
            label = "eq$i",
        )
    }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Uva.copy(alpha = 0.25f), p.fondo2))).padding(esc.d(14))) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(5)), verticalAlignment = Alignment.Bottom) {
            fases.forEachIndexed { i, f ->
                Box(
                    Modifier.weight(1f).fillMaxHeight(if (e.musica) f.value else 0.12f).clip(RoundedCornerShape(esc.d(4)))
                        .background(if (i % 3 == 0) Coral else Uva),
                )
            }
        }
        Spacer(Modifier.height(esc.d(8)))
        Texto(if (e.pista == "mia") "Tu canción" else pista?.nombre ?: "Mezcla", 15f, lineas = 1)
        Detalle(if (!e.musica) "Música apagada" else pista?.estilo ?: "", 12f, lineas = 1)
    }
}



/** Hora, batería y avisos en una línea chica, para los paneles de la pantalla de arriba sin barra. */
@Composable
fun EstadoChico() {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val hora = rememberHora()
    val bateria = rememberBateria()
    val cargando = rememberCargando()
    val avisos by cl.nikalbazu.diptico.audio.Avisos.lista.collectAsState()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
        if (avisos.isNotEmpty()) Row(
            Modifier.clip(RoundedCornerShape(50)).clickable { Diptico.abrirCentro("avisos") }.padding(horizontal = esc.d(4), vertical = esc.d(2)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconoDiptico(Icono.CAMPANA, Coral, 13f)
            Texto("${avisos.size}", 11f, color = p.tinta, traducir = false, lineas = 1)
        }
        BotonPerfil(32f)
        Texto(hora, 13f, color = p.tinta, traducir = false, lineas = 1, modifier = Modifier.clickable { Diptico.alternarReloj() })
        val tono = when { cargando -> Color(0xFF38BDF8); bateria in 0..15 -> Color(0xFFF87171); else -> p.tinta }
        Box(Modifier.size(esc.d(20), esc.d(10)).border(esc.d(1.5f), p.tinta.copy(alpha = 0.7f), RoundedCornerShape(esc.d(3))).padding(esc.d(2))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(((if (bateria < 0) 50 else bateria) / 100f).coerceIn(0.08f, 1f))
                .clip(RoundedCornerShape(esc.d(1.5f))).background(tono))
        }
        Texto(if (bateria >= 0) "$bateria%" else "—", 12f, color = p.tinta, traducir = false, lineas = 1)
    }
}

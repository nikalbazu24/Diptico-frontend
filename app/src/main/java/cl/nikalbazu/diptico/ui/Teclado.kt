package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxHeight
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Panel
import cl.nikalbazu.diptico.estado.Palabras
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex

/**
 * Lo que se está escribiendo, con el cursor donde va y lo seleccionado resaltado
 * (sin abrir el teclado de Android).
 */
@Composable
fun TextoEscrito(
    valor: String, ejemplo: String, tam: Float = 17f, minAlto: Float = 36f, modifier: Modifier = Modifier,
    cursor: Int = -1, seleccion: IntRange = IntRange.EMPTY, oculto: Boolean = false,
) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val t = rememberInfiniteTransition(label = "cursor")
    val brillo by t.animateFloat(1f, 0f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "cursor")
    val c = if (cursor < 0 || cursor > valor.length) valor.length else cursor
    val mostrar = if (oculto && valor.length > 6) valor.take(4) + "•".repeat(valor.length - 4) else valor
    Box(modifier.heightIn(min = esc.d(minAlto))) {
        if (valor.isEmpty()) Texto(ejemplo, tam, color = p.tenue, peso = FontWeight.Medium, modifier = Modifier.padding(start = esc.d(4)))
        val texto = buildAnnotatedString {
            for (i in 0..mostrar.length) {
                if (i == c && seleccion.isEmpty()) withStyle(SpanStyle(color = Uva.copy(alpha = brillo))) { append("▏") }
                if (i == mostrar.length) break
                val ch = mostrar[i].toString()
                if (i in seleccion) withStyle(SpanStyle(background = Uva.copy(alpha = 0.45f))) { append(ch) } else append(ch)
            }
        }
        BasicText(
            texto,
            style = TextStyle(color = p.tinta, fontSize = esc.s(tam), fontFamily = Fredoka, fontWeight = FontWeight.Medium, lineHeight = esc.s(tam * 1.35f)),
        )
    }
}

/**
 * Teclado de Díptico, a la manera de Gboard: arriba la franja con palabras sugeridas (⋯ cambia a las herramientas
 * de cursor, copiar y pegar; el micrófono dicta), las letras con ⇧ y ⌫ a los lados, y abajo ?123, coma, espacio,
 * punto y Listo. Al tocar una letra se ve en grande encima del dedo; manteniéndola sale su tilde o su número.
 * Con mando: cruceta elige, A escribe, Y pone la tilde, X espacio, B borra.
 */
@Composable
fun TecladoDiptico(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val filas = Diptico.filasTeclado(e)
    val titulo = when {
        e.panel == Panel.PASO -> if (e.pregunta) "¿Dónde quedaste?" else "Tu próximo paso"
        e.textoPara == "titulo" -> "Nombre del juego"
        e.textoPara == "notas" -> "Notas y trucos"
        e.textoPara == "reporte_fallas" -> "Describe qué pasó"
        e.textoPara == "ia_pedido" -> "Pídele un tema a la IA"
        e.textoPara == "sgdb" || e.textoPara.startsWith("ia_clave") || e.textoPara == "ra_clave" -> "Pega o escribe tu clave"
        e.textoPara == "ra_usuario" -> "Tu usuario de RetroAchievements"
        e.textoPara.startsWith("ras_") -> Diptico.tituloRaspador(e.textoPara) ?: "Escribe"
        e.textoPara == "nombre" -> "¿Cómo te llamas?"
        e.textoPara == "asistente" -> "Pregúntale a DIPPO"
        e.textoPara == "buscar_ajuste" -> "Buscar un ajuste"
        e.textoPara == "buscar_imagen" -> "Buscar con otro nombre"
        e.textoPara == "buscar_juego" -> "Buscar un juego"
        e.textoPara == "coleccion_nueva" -> "Nombre de la colección"
        e.textoPara == "pc_steam" -> "Nombre del juego de Steam"
        else -> "Escribe"
    }
    val oculto = e.panel == Panel.CLAVE && (e.textoPara == "sgdb" || e.textoPara.startsWith("ia_clave") || e.textoPara == "ra_clave")
    val sel = Diptico.seleccion(e)
    Box(Modifier.fillMaxSize()) {
        // Arriba se sigue viendo la pantalla, apenas oscurecida.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.35f to Color.Black.copy(alpha = 0.25f), 0.45f to p.fondo.copy(alpha = 0.9f))))
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.78f)
                .clip(RoundedCornerShape(topStart = esc.d(22), topEnd = esc.d(22))).background(p.fondo2.copy(alpha = 0.98f)).sinCerrar()
                .padding(horizontal = esc.d(8), vertical = esc.d(6)),
        ) {
            // Lo que se escribe, con su título al lado.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = esc.d(4))) {
                Texto(titulo, 12.5f, color = p.tenue, lineas = 1, modifier = Modifier.weight(1f))
                if (!sel.isEmpty()) Detalle("${sel.last - sel.first + 1} seleccionadas", 11f, color = Uva, lineas = 1)
            }
            Spacer(Modifier.height(esc.d(3)))
            TextoEscrito(
                e.borrador.takeLast(300), "Toca las letras, usa la cruceta o el micrófono para dictar", 17f, 30f,
                Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(20))).background(p.superficie)
                    .padding(horizontal = esc.d(14), vertical = esc.d(5)),
                cursor = e.cursor - maxOf(0, e.borrador.length - 300).let { if (e.cursor < 0) 0 else it },
                seleccion = sel, oculto = oculto,
            )
            Spacer(Modifier.height(esc.d(4)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
                filas.forEachIndexed { f, fila ->
                    if (f == 0) FranjaTeclado(e, fila, Modifier.weight(0.8f))
                    else Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(5))) {
                        fila.forEachIndexed { c, t -> Tecla(e, t, f, c) }
                    }
                }
            }
            Spacer(Modifier.height(esc.d(3)))
            Pistas("A" to "Escribir", "Y" to "Tilde", "X" to "Espacio", "B" to "Borrar", "Start" to "Listo")
        }
    }
}

/** La franja de arriba: palabras sugeridas como en Gboard, o las herramientas de edición. */
@Composable
private fun FranjaTeclado(e: Estado, fila: List<String>, modifier: Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        fila.forEachIndexed { c, t ->
            val activa = e.teclaFila == 0 && c == e.teclaCol
            when {
                t == "⋯" || t == "Voz" -> Box(
                    Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true).clip(CircleShape)
                        .then(if (activa) Modifier.fondoBoton(Uva) else if (t == "⋯" && e.extra.tecladoEdicion) Modifier.background(Uva.copy(alpha = 0.3f)) else Modifier)
                        .clickable { Diptico.pulsarTecla(0, c) },
                    contentAlignment = Alignment.Center,
                ) {
                    IconoDiptico(if (t == "Voz") Icono.MICROFONO else Icono.MENU, if (activa) SobreUva else p.tinta, 17f, acento = if (activa) SobreUva else Coral)
                }
                t.startsWith("»") -> {
                    if (c > 1) Box(Modifier.width(esc.d(1)).fillMaxHeight(0.5f).background(p.linea))
                    Box(
                        Modifier.weight(1f).fillMaxHeight().padding(horizontal = esc.d(2)).clip(RoundedCornerShape(esc.d(10)))
                            .then(if (activa) Modifier.fondoBoton(Uva) else Modifier)
                            .clickable { Diptico.pulsarTecla(0, c) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Texto(t.drop(1), 15f, color = if (activa) SobreUva else p.tinta, lineas = 1, traducir = false,
                            peso = if (c == 2) FontWeight.Bold else FontWeight.Medium)
                    }
                }
                else -> {
                    val icono = when (t) { "Copiar", "Cortar" -> Icono.COPIAR; "Pegar" -> Icono.PEGAR; else -> null }
                    val marcada = t == "Selec." && e.ancla != null
                    Row(
                        Modifier.weight(1f).fillMaxHeight().padding(horizontal = esc.d(2)).clip(RoundedCornerShape(esc.d(10)))
                            .then(if (activa) Modifier.fondoBoton(Uva) else Modifier.background(if (marcada) Sol.copy(alpha = 0.5f) else p.superficie.copy(alpha = 0.6f)))
                            .clickable { Diptico.pulsarTecla(0, c) },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (icono != null && !esc.angosta) {
                            IconoDiptico(icono, if (activa) SobreUva else p.tinta, 12f, acento = if (activa) SobreUva else Coral)
                            Spacer(Modifier.width(esc.d(3)))
                        }
                        Texto(t, if (t.length == 1) 17f else 12f, color = if (activa) SobreUva else p.tinta, lineas = 1)
                    }
                }
            }
        }
        // Sin sugerencias, la franja queda limpia (el micrófono sigue a la derecha).
        if (!e.extra.tecladoEdicion && fila.none { it.startsWith("»") }) Spacer(Modifier.weight(1f))
    }
}

/** Una tecla: se agranda en un globito al tocarla y, manteniéndola, escribe su tilde o su número. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.Tecla(e: Estado, t: String, f: Int, c: Int) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val activa = f == e.teclaFila && c == e.teclaCol
    val funcion = t in setOf("⇧", "⌫", "?123", "abc", "✓", "␣") || t.length > 1
    val peso = when (t) { "␣" -> 4.6f; "✓", "?123", "abc" -> 1.5f; "⇧", "⌫" -> 1.45f; else -> 1f }
    val toque = remember { MutableInteractionSource() }
    val apretada by toque.collectIsPressedAsState()
    val fondo = when {
        t == "✓" -> Uva
        t == "⇧" && e.extra.mayusFija -> Sol.copy(alpha = 0.75f)
        t == "⇧" && e.mayus -> Sol.copy(alpha = 0.4f)
        funcion -> p.fondo
        else -> p.superficie
    }
    BoxWithConstraints(Modifier.weight(peso).fillMaxHeight().zIndex(if (apretada) 2f else 0f)) {
        val alto = maxHeight
        Box(
            Modifier.fillMaxSize().shadow(esc.d(1), RoundedCornerShape(esc.d(8))).clip(RoundedCornerShape(esc.d(8)))
                .then(if (activa) Modifier.fondoBoton(Uva) else Modifier.background(fondo))
                .then(if (activa) Modifier.border(esc.d(2), Color.White, RoundedCornerShape(esc.d(8))) else Modifier)
                .combinedClickable(
                    interactionSource = toque, indication = null,
                    onLongClick = {
                        when {
                            t == "⌫" -> Diptico.borrarPalabra()
                            Palabras.variante(t) != null -> Diptico.escribirLetra(Palabras.variante(t)!!)
                            else -> Diptico.pulsarTecla(f, c)
                        }
                    },
                ) { Diptico.pulsarTecla(f, c) },
            contentAlignment = Alignment.Center,
        ) {
            val color = if (activa || t == "✓") SobreUva else p.tinta
            when (t) {
                "✓" -> IconoDiptico(Icono.CHECK, color, 18f, acento = color)
                "␣" -> Texto("espacio", 11.5f, color = color.copy(alpha = 0.55f), lineas = 1)
                "⇧" -> Texto(if (e.extra.mayusFija) "⇪" else "⇧", 20f, color = color, lineas = 1, traducir = false)
                else -> Texto(t, if (t.length == 1) 21f else 13f, color = color, lineas = 1, traducir = t.length > 1)
            }
            Palabras.pista(t)?.takeIf { !e.simbolos }?.let { h ->
                Texto(h, 9.5f, color = color.copy(alpha = 0.5f), lineas = 1, traducir = false,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = esc.d(4), top = esc.d(1)))
            }
        }
        // El globito con la letra en grande, encima del dedo (solo letras y signos).
        if (apretada && !funcion) {
            Box(
                Modifier.align(Alignment.TopCenter).graphicsLayer { translationY = -alto.toPx() * 1.05f; scaleX = 1.15f; scaleY = 1.15f }
                    .fillMaxWidth().height(alto).shadow(esc.d(6), RoundedCornerShape(esc.d(10)))
                    .clip(RoundedCornerShape(esc.d(10))).background(p.superficie),
                contentAlignment = Alignment.Center,
            ) { Texto(t, 26f, color = p.tinta, lineas = 1, traducir = false) }
        }
    }
}

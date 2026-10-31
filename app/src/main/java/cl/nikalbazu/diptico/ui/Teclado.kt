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
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Teclado de Díptico: aparece en la mitad de abajo de la pantalla contraria a la del texto,
 * así se sigue viendo lo de arriba. Cruceta + A, o tocando. Tiene cursor, seleccionar, copiar, cortar y pegar.
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
        e.textoPara == "ia_pedido" -> "Pídele un tema a la IA"
        e.textoPara == "sgdb" || e.textoPara == "ia_clave" || e.textoPara == "ra_clave" -> "Pega o escribe tu clave"
        e.textoPara == "ra_usuario" -> "Tu usuario de RetroAchievements"
        e.textoPara == "asistente" -> "Pregúntale a Bisa"
        e.textoPara == "buscar_ajuste" -> "Buscar un ajuste"
        e.textoPara == "buscar_imagen" -> "Buscar con otro nombre"
        else -> "Escribe"
    }
    val oculto = e.panel == Panel.CLAVE && (e.textoPara == "sgdb" || e.textoPara == "ia_clave" || e.textoPara == "ra_clave")
    val sel = Diptico.seleccion(e)
    Box(Modifier.fillMaxSize()) {
        // Arriba se sigue viendo la pantalla, apenas oscurecida.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.35f to Color.Black.copy(alpha = 0.25f), 0.45f to p.fondo.copy(alpha = 0.9f))))
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.6f)
                .clip(RoundedCornerShape(topStart = esc.d(24), topEnd = esc.d(24))).background(p.fondo2.copy(alpha = 0.97f)).sinCerrar()
                .padding(horizontal = esc.d(12), vertical = esc.d(8)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.TEXTO, p.tinta, 16f, acento = Coral)
                Spacer(Modifier.width(esc.d(6)))
                Texto(titulo, 14f, lineas = 1, modifier = Modifier.weight(1f))
                if (!sel.isEmpty()) Detalle("${sel.last - sel.first + 1} seleccionadas", 11f, color = Uva, lineas = 1)
                Spacer(Modifier.width(esc.d(8)))
                Detalle("${e.borrador.length} letras", 11f, lineas = 1)
            }
            Spacer(Modifier.height(esc.d(4)))
            TextoEscrito(
                e.borrador.takeLast(300), "Escribe con la cruceta y A, o tocando las teclas", 15f, 26f,
                Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(12))).background(p.superficie)
                    .border(esc.d(1.5f), Uva.copy(alpha = 0.6f), RoundedCornerShape(esc.d(12))).padding(horizontal = esc.d(10), vertical = esc.d(5)),
                cursor = e.cursor - maxOf(0, e.borrador.length - 300).let { if (e.cursor < 0) 0 else it },
                seleccion = sel, oculto = oculto,
            )
            Spacer(Modifier.height(esc.d(5)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
                filas.forEachIndexed { f, fila ->
                    val edicion = f == 0
                    Row(Modifier.weight(if (edicion) 0.8f else 1f), horizontalArrangement = Arrangement.spacedBy(esc.d(4))) {
                        fila.forEachIndexed { c, t ->
                            val activa = f == e.teclaFila && c == e.teclaCol
                            val peso = when (t) { "␣" -> 3.6f; "✓" -> 1.8f; "⇧", "#", "abc", "⌫" -> 1.4f; else -> 1f }
                            val especial = f == filas.size - 1 || edicion
                            val marcada = (t == "⇧" && e.mayus) || (t == "Selec." && e.ancla != null)
                            val fondo = when {
                                activa -> Uva
                                t == "✓" -> Coral.copy(alpha = 0.85f)
                                marcada -> Sol.copy(alpha = 0.6f)
                                especial -> p.fondo
                                else -> p.superficie
                            }
                            Box(
                                Modifier.weight(peso).fillMaxSize().clip(RoundedCornerShape(esc.d(9)))
                                    .then(if (activa) Modifier.fondoBoton(Uva) else Modifier.background(fondo))
                                    .clickable { Diptico.pulsarTecla(f, c) },
                                contentAlignment = Alignment.Center,
                            ) {
                                val icono = when (t) { "Copiar", "Cortar" -> Icono.COPIAR; "Pegar" -> Icono.PEGAR; else -> null }
                                val etiqueta = when (t) { "␣" -> "espacio"; "✓" -> "Listo"; "⌫" -> "⌫"; else -> t }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (icono != null && !esc.angosta) {
                                        IconoDiptico(icono, if (activa) SobreUva else p.tinta, 12f, acento = if (activa) SobreUva else Coral)
                                        Spacer(Modifier.width(esc.d(3)))
                                    }
                                    Texto(etiqueta, if (t.length == 1) 15f else 11.5f, color = if (activa || t == "✓") SobreUva else p.tinta, lineas = 1)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(esc.d(4)))
            Pistas("A" to "Escribir", "B" to "Borrar", "L1 R1" to "Cursor", "L2" to "Seleccionar", "R2" to "Pegar", "Start" to "Listo")
        }
    }
}

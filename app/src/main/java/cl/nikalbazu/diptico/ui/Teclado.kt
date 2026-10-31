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
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Panel

/** Lo que se está escribiendo, con un cursor que parpadea (sin abrir el teclado de Android). */
@Composable
fun TextoEscrito(valor: String, ejemplo: String, tam: Float = 17f, minAlto: Float = 36f, modifier: Modifier = Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val t = rememberInfiniteTransition(label = "cursor")
    val brillo by t.animateFloat(1f, 0f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "cursor")
    Box(modifier.heightIn(min = esc.d(minAlto))) {
        if (valor.isEmpty()) Texto(ejemplo, tam, color = p.tenue, peso = FontWeight.Medium)
        Row(verticalAlignment = Alignment.Bottom) {
            if (valor.isNotEmpty()) Texto(valor, tam, peso = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
            Box(Modifier.padding(start = esc.d(1), bottom = esc.d(3)).size(esc.d(2.5f), esc.d(tam * 1.15f)).alpha(brillo).background(Uva))
        }
    }
}

/** Teclado de Díptico: aparece en la pantalla contraria a la del texto. Cruceta + A, o tocando. */
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
        e.textoPara == "asistente" -> "Pregúntale al asistente"
        else -> "Escribe"
    }
    val oculto = e.panel == Panel.CLAVE && (e.textoPara == "sgdb" || e.textoPara == "ia_clave" || e.textoPara == "ra_clave")
    Box(Modifier.fillMaxSize().background(p.fondo.copy(alpha = 0.94f)).sinCerrar()) {
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(14), vertical = esc.d(10))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.TEXTO, p.tinta, 20f, acento = Coral)
                Spacer(Modifier.width(esc.d(8)))
                Texto(titulo, 18f, lineas = 1, modifier = Modifier.weight(1f))
                Detalle("${e.borrador.length} letras", 12f, lineas = 1)
            }
            Spacer(Modifier.height(esc.d(6)))
            val mostrado = if (oculto && e.borrador.length > 6) e.borrador.take(4) + "•".repeat(minOf(e.borrador.length - 4, 24)) else e.borrador
            TextoEscrito(
                mostrado.takeLast(140), "Escribe con la cruceta y A, o tocando las teclas", 17f, 34f,
                Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(14))).background(p.superficie)
                    .border(esc.d(2), Uva.copy(alpha = 0.6f), RoundedCornerShape(esc.d(14))).padding(horizontal = esc.d(12), vertical = esc.d(8)),
            )
            Spacer(Modifier.height(esc.d(8)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
                filas.forEachIndexed { f, fila ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(5))) {
                        fila.forEachIndexed { c, t ->
                            val activa = f == e.teclaFila && c == e.teclaCol
                            val peso = when (t) { "␣" -> 3.6f; "✓" -> 1.8f; "⇧", "#", "abc", "⌫" -> 1.4f; else -> 1f }
                            val especial = f == filas.size - 1
                            val fondo = when {
                                activa -> Uva
                                t == "✓" -> Coral.copy(alpha = 0.85f)
                                t == "⇧" && e.mayus -> Sol.copy(alpha = 0.6f)
                                especial -> p.fondo2
                                else -> p.superficie
                            }
                            Box(
                                Modifier.weight(peso).fillMaxSize().clip(RoundedCornerShape(esc.d(11)))
                                    .then(if (activa) Modifier.fondoBoton(Uva) else Modifier.background(fondo))
                                    .clickable { Diptico.pulsarTecla(f, c) },
                                contentAlignment = Alignment.Center,
                            ) {
                                val etiqueta = when (t) { "␣" -> "espacio"; "✓" -> "Listo"; "⌫" -> "⌫ borrar"; "⇧" -> "⇧"; else -> t }
                                Texto(etiqueta, if (t.length == 1) 17f else 13f, color = if (activa || t == "✓") SobreUva else p.tinta, lineas = 1)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("A" to "Escribir", "B" to "Borrar", "X" to "Espacio", "Y" to "Mayús", "Start" to "Listo")
        }
    }
}

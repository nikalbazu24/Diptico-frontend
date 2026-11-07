package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import cl.nikalbazu.diptico.data.Consola
import kotlin.math.min

/** Arte alrededor del hardware, con referencias adicionales al catálogo de cada familia. */
internal fun motivosCarpeta(id: String): List<String> {
    val principal = ReferenciasCarpetas.para(id)?.motivo ?: "coleccion"
    val secundarios = when (id) {
        "nes", "famicom", "fds" -> listOf("estrella", "trifuerza", "robot", "barril")
        "snes", "snesna", "sfc" -> listOf("gorra", "trifuerza", "robot", "bola")
        "n64", "n64dd" -> listOf("gorra", "estrella", "bola", "barril")
        "gb", "sgb", "gbc", "gba", "pokemini" -> listOf("bola", "bloques", "estrella", "trifuerza")
        "gc", "wii", "wiiu", "switch", "switch2" -> listOf("estrella", "trifuerza", "hoja", "calamar")
        "nds", "n3ds" -> listOf("bola", "hoja", "estrella", "trifuerza")
        "psx", "ps2", "ps3", "ps4", "psp", "psvita" -> listOf("espadon", "garras", "omega", "brujula")
        "xbox", "xbox360", "xboxone" -> listOf("casco", "carreras", "espada", "nave")
        "android", "androidapps", "androidgames" -> listOf("viento", "pajaro", "montana", "bloques")
        "rpgmaker", "easyrpg" -> listOf("luna", "espada", "castillo", "gema")
        else -> when (principal) {
            "anillo" -> listOf("estrella", "carreras", "luna", "anillo")
            "pacman", "garras", "tanque" -> listOf("pacman", "invasor", "tanque", "carreras")
            "calavera", "lambda", "portal" -> listOf("portal", "lambda", "calavera", "espada")
            "nave", "invasor" -> listOf("nave", "invasor", "bloques", "estrella")
            else -> listOf(principal, "estrella", "gema", "nave")
        }
    }
    return (listOf(principal) + secundarios).distinct()
}

/** Portadas propias y vectoriales para todo el catálogo, también Game Boy. */
@Composable
internal fun ArteCarpeta(consola: Consola?, modifier: Modifier = Modifier) {
    val id = consola?.id ?: "todo"
    val color = consola?.color ?: Uva
    Box(modifier.drawWithCache {
        val s = min(size.width, size.height)
        val papel = lerp(color, Color.White, .88f)
        val tinta = lerp(color, Color.Black, .63f)
        val medio = lerp(color, Color.Black, .14f)
        val motivos = motivosCarpeta(id).map(::construirEmblema)
        // Dibujos en la periferia. El centro se reserva para la consola, a cualquier tamaño.
        val posiciones = listOf(
            Triple(.01f, .02f, .31f), Triple(.67f, -.02f, .33f),
            Triple(-.06f, .35f, .30f), Triple(.79f, .35f, .27f),
            Triple(.00f, .70f, .32f), Triple(.34f, .79f, .24f), Triple(.68f, .70f, .34f),
        )
        val fondo = Brush.linearGradient(listOf(papel, lerp(papel, color, .12f)), Offset.Zero, Offset(s, s))
        val placa = Rect(s * .245f, s * .245f, s * .755f, s * .755f)
        val radio = CornerRadius(s * .125f)
        val luz = Brush.linearGradient(listOf(lerp(medio, Color.White, .2f), medio, lerp(medio, Color.Black, .18f)))
        val clip = Path().apply { addRoundRect(RoundRect(placa, radio)) }
        onDrawBehind {
            drawRect(fondo)
            // Trama muy discreta, dibujada una vez en la caché, sin texturas descargadas.
            for (y in 0..23) for (x in 0..23) {
                val centro = Offset(s * (x + .5f) / 24, s * (y + .5f) / 24)
                drawCircle(tinta.copy(alpha = .08f), s * .0024f, centro)
            }
            posiciones.forEachIndexed { i, (x, y, escala) ->
                withTransform({ translate(s * x, s * y); scale(s * escala / 100, s * escala / 100, Offset.Zero); rotate(if (i % 2 == 0) -10f else 9f, Offset(50f, 50f)) }) {
                    motivos[i % motivos.size].forEach { trazo ->
                        val tono = lerp(papel, tinta, (.15f + (1f - trazo.color.luminance()) * .72f) * trazo.color.alpha)
                        drawPath(trazo.forma, tono, style = if (trazo.grosor == 0f) androidx.compose.ui.graphics.drawscope.Fill else Stroke(trazo.grosor))
                        if (trazo.grosor == 0f) drawPath(trazo.forma, tinta.copy(alpha = .38f), style = Stroke(.65f))
                    }
                }
            }
            // Sombra difusa escalonada, sin marco grueso ni varios contornos concéntricos.
            for (i in 8 downTo 1) {
                val margen = s * i * .003f
                drawRoundRect(tinta.copy(alpha = .018f), placa.topLeft + Offset(-margen, s * .014f - margen),
                    Size(placa.width + 2 * margen, placa.height + 2 * margen), CornerRadius(radio.x + margen))
            }
            drawRoundRect(luz, placa.topLeft, placa.size, radio)
            clipPath(clip) {
                drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = .23f), Color.Transparent), placa.topLeft, s * .63f))
                withTransform({ translate(placa.left + placa.width * .12f, placa.top + placa.height * .12f); scale(placa.width * .76f / size.width, placa.height * .76f / size.height, Offset.Zero) }) {
                    if (consola != null) dibujarHardware(consola, Color.White, medio)
                    else dibujarMedio(cl.nikalbazu.diptico.data.Medio.TODOS, Color.White, medio)
                }
            }
            drawRoundRect(Color.White.copy(alpha = .34f), placa.topLeft, placa.size, radio, style = Stroke(s * .002f))
        }
    })
}

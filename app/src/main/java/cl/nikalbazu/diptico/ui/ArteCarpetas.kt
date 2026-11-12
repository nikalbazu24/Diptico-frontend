package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
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

/**
 * Dibujos alrededor del hardware: motivos propios y genéricos según el tipo de consola.
 * Nunca personajes, objetos ni símbolos de franquicias (Díptico es público).
 */
internal fun motivosCarpeta(id: String): List<String> = when (id) {
    "gb", "sgb", "gbc", "gba", "nds", "n3ds", "pokemini", "gamegear", "ngp", "ngpc", "lynx", "wonderswan",
    "wonderswancolor", "psp", "psvita", "virtualboy", "gameandwatch", "lcdgames" -> listOf("bloques", "estrella", "hoja", "tablero")
    "switch", "switch2", "wii", "wiiu" -> listOf("hoja", "carreras", "estrella", "montana")
    "nes", "famicom", "fds", "snes", "snesna", "sfc", "n64", "n64dd", "satellaview", "sufami",
    "genesis", "megadrive", "megadrivejp", "mastersystem", "mark3", "sg-1000", "sega32x", "sega32xjp", "sega32xna",
    "atari2600", "atari5200", "atari7800", "colecovision", "intellivision", "pcengine", "tg16" -> listOf("estrella", "castillo", "nave", "carreras")
    "psx", "ps2", "ps3", "ps4", "segacd", "megacd", "megacdjp", "saturn", "saturnjp", "dreamcast", "gc",
    "xbox", "xbox360", "xboxone", "3do", "pcfx", "jaguar" -> listOf("luna", "montana", "brujula", "espada")
    "arcade", "mame", "mame-advmame", "mess", "fba", "fbneo", "cps", "cps1", "cps2", "cps3", "neogeo", "neogeocd",
    "neogeocdjp", "naomi", "naomi2", "naomigd", "atomiswave", "model2", "model3", "triforce", "stv",
    "consolearcade", "pcarcade", "type-x" -> listOf("pinball", "malabares", "tanque", "nave")
    "android", "androidapps", "androidgames", "ports" -> listOf("viento", "pajaro", "montana", "bloques")
    "rpgmaker", "easyrpg" -> listOf("luna", "espada", "castillo", "vela")
    "pc", "dos", "windows", "scummvm", "steam", "amiga", "c64", "msx", "zxspectrum" -> listOf("television", "robot", "tablero", "brujula")
    else -> listOf("coleccion", "estrella", "hoja", "gema")
}

/** Portadas propias y vectoriales para todo el catálogo, también Game Boy. */
@Composable
internal fun ArteCarpeta(consola: Consola?, modifier: Modifier = Modifier, oscuro: Boolean = false) {
    val id = consola?.id ?: "todo"
    val color = consola?.color ?: Uva
    Box(modifier.drawWithCache {
        val s = min(size.width, size.height)
        // Modo oscuro: el «papel» es un fondo profundo teñido del color de la consola y los dibujos pasan a ser claros.
        val papel = if (oscuro) lerp(color, Color.Black, .8f) else lerp(color, Color.White, .88f)
        val tinta = if (oscuro) lerp(color, Color.White, .62f) else lerp(color, Color.Black, .63f)
        val medio = lerp(color, Color.Black, if (oscuro) .32f else .14f)
        val sombra = if (oscuro) Color.Black else tinta
        val motivos = motivosCarpeta(id).map(::construirEmblema)
        // Dibujos en la periferia. El centro se reserva para la consola, a cualquier tamaño.
        // Solo las cuatro esquinas: menos dibujos y más aire alrededor de la consola.
        val posiciones = listOf(
            Triple(.02f, .02f, .28f), Triple(.70f, .02f, .28f),
            Triple(.02f, .70f, .28f), Triple(.70f, .70f, .28f),
        )
        val fondo = Brush.linearGradient(listOf(papel, lerp(papel, color, .12f)), Offset.Zero, Offset(s, s))
        val placa = Rect(s * .245f, s * .245f, s * .755f, s * .755f)
        val radio = CornerRadius(s * .125f)
        val luz = Brush.linearGradient(listOf(lerp(medio, Color.White, .2f), medio, lerp(medio, Color.Black, .18f)))
        val clip = Path().apply { addRoundRect(RoundRect(placa, radio)) }
        // Trama muy discreta: los 576 puntitos se calculan una vez y se dibujan en una sola llamada (antes eran 576).
        val trama = List(24 * 24) { k -> Offset(s * (k % 24 + .5f) / 24, s * (k / 24 + .5f) / 24) }
        val tramaColor = tinta.copy(alpha = .08f)
        onDrawBehind {
            drawRect(fondo)
            drawPoints(trama, androidx.compose.ui.graphics.PointMode.Points, tramaColor, strokeWidth = s * .0048f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
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
                drawRoundRect(sombra.copy(alpha = if (oscuro) .05f else .018f), placa.topLeft + Offset(-margen, s * .014f - margen),
                    Size(placa.width + 2 * margen, placa.height + 2 * margen), CornerRadius(radio.x + margen))
            }
            drawRoundRect(luz, placa.topLeft, placa.size, radio)
            clipPath(clip) {
                drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = if (oscuro) .14f else .23f), Color.Transparent), placa.topLeft, s * .63f))
                withTransform({ translate(placa.left + placa.width * .12f, placa.top + placa.height * .12f); scale(placa.width * .76f / size.width, placa.height * .76f / size.height, Offset.Zero) }) {
                    if (consola != null) dibujarHardware(consola, Color.White, medio)
                    else dibujarMedio(cl.nikalbazu.diptico.data.Medio.TODOS, Color.White, medio)
                }
            }
            drawRoundRect(Color.White.copy(alpha = if (oscuro) .26f else .34f), placa.topLeft, placa.size, radio, style = Stroke(s * .002f))
        }
    })
}

/** ¿Las carpetas van oscuras? Con «auto» siguen al tema: oscuras si el fondo de la app lo es. */
@Composable
internal fun carpetasOscuras(tono: String): Boolean = when (tono) {
    "oscuro" -> true
    "claro" -> false
    else -> LocalPaleta.current.fondo.luminance() < 0.45f
}

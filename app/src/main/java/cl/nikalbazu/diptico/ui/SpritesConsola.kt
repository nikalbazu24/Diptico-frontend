package cl.nikalbazu.diptico.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp

/*
 * Íconos pixel hechos a mano (16×16) para las consolas que merecen uno propio en el estilo «Píxeles»:
 * Switch, Switch 2 y Gahvoria (los fan games de RPG Maker). Cada letra es un color:
 *   M principal · D oscuro · L claro (del color de la consola)   B azul · E azul oscuro · F azul claro
 *   K contorno · N pantalla · G brillo de pantalla · W blanco · Y amarillo · X gris de los mandos · C acento (color de la consola)
 *   . transparente
 * Los dibujos son originales de Díptico: formas genéricas, sin logos ni marcas.
 */
internal object SpritesConsola {
    private val SWITCH = listOf(
        "................",
        "................",
        "................",
        "MMMMKKKKKKKKBBBB",
        "MLLMKNNNNNNKBBFB",
        "MKKMKNGGNNNKBFBF",
        "MKKMKNGNNNNKBBFB",
        "MMMMKNNNNNNKBBBB",
        "MMMMKNNNNNNKBKKB",
        "MMDMKNNNNNNKBKKB",
        "DMMDKNNNNNNKEBBE",
        "DDDDKKKKKKKKEEEE",
        "................",
        "................",
        "................",
        "................",
    )
    private val SWITCH2 = listOf(
        "................",
        "................",
        "................",
        "..XXXKKKKKKKKXXX",
        "..XXXKNNNNNNKXXX",
        "..XCXKNWWWNNKXCX",
        "..XCXKNNNWNNKXCX",
        "..XXXKNNWWNNKXXX",
        "..XXXKNWNNNNKXXX",
        "..XCXKNWWWWNKXCX",
        "..XXXKNNNNNNKXXX",
        "..XXXKKKKKKKKXXX",
        "................",
        "................",
        "................",
        "................",
    )
    /** Gahvoria: una gema de cristal con destellos (RPG Maker). */
    private val GAHVORIA = listOf(
        "................",
        "...Y.......W....",
        "....LLLLLLLL....",
        "...LLMMLLMMLD...",
        "..LLMMMLLMMMLD..",
        "..LLLLLLLLLLLD..",
        "...MMMMLLMMMD...",
        "...DMMMLLMMMD...",
        "....DMMLLMMD....",
        ".....DMLLMD.....",
        "......DLLD......",
        ".......DD.......",
        "................",
        "................",
        "................",
        "................",
    )

    fun de(id: String): List<String>? = when (id) {
        "switch" -> SWITCH
        "switch2" -> SWITCH2
        "rpgmaker", "easyrpg" -> GAHVORIA
        else -> null
    }
}

/** Pinta un sprite en la cuadrícula del lienzo (un cuadro por letra). */
internal fun DrawScope.dibujarSprite(sprite: List<String>, color: Color) {
    val u = size.minDimension / 16f
    val azul = Color(0xFF1FA2E8)
    fun tono(c: Char): Color? = when (c) {
        'M', 'C' -> color
        'D' -> lerp(color, Color.Black, 0.35f)
        'L' -> lerp(color, Color.White, 0.4f)
        'B' -> azul
        'E' -> lerp(azul, Color.Black, 0.3f)
        'F' -> lerp(azul, Color.White, 0.4f)
        'K' -> Color(0xFF15171F)
        'N' -> Color(0xFF22304F)
        'G' -> Color(0xFF8FB4FF)
        'W' -> Color.White
        'Y' -> Color(0xFFFFD54F)
        'X' -> Color(0xFF3A3F4E)
        else -> null
    }
    sprite.forEachIndexed { y, fila ->
        fila.forEachIndexed { x, c -> tono(c)?.let { drawRect(it, Offset(x * u, y * u), Size(u, u)) } }
    }
}

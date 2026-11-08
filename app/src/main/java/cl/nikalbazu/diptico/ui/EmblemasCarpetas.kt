package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

internal data class TrazoEmblema(val forma: Path, val color: Color, val grosor: Float = 0f)

/** Emblemas vectoriales propios (dibujos genéricos, sin franquicias): nítidos a cualquier tamaño y sin red. */
internal fun construirEmblema(motivo: String): List<TrazoEmblema> = buildList {
    val blanco = Color(0xFFFFF5DE); val oscuro = Color(0xFF101B32)
    val oro = Color(0xFFFFCC57); val azul = Color(0xFF5CA9FF)
    val rojo = Color(0xFFFF6176); val menta = Color(0xFF6BE1B6)
    fun forma(color: Color, grosor: Float = 0f, crear: Path.() -> Unit) {
        add(TrazoEmblema(Path().apply(crear), color, grosor))
    }
    fun pol(color: Color, vararg puntos: Float) = forma(color) {
        moveTo(puntos[0], puntos[1]); for (i in 2 until puntos.size step 2) lineTo(puntos[i], puntos[i + 1]); close()
    }
    fun oval(color: Color, x: Float, y: Float, w: Float, h: Float, grosor: Float = 0f) = forma(color, grosor) { addOval(Rect(x, y, x + w, y + h)) }
    fun caja(color: Color, x: Float, y: Float, w: Float, h: Float, radio: Float = 3f) = forma(color) {
        addRoundRect(RoundRect(Rect(x, y, x + w, y + h), CornerRadius(radio)))
    }
    fun linea(color: Color, ancho: Float, vararg puntos: Float) = forma(color, ancho) {
        moveTo(puntos[0], puntos[1]); for (i in 2 until puntos.size step 2) lineTo(puntos[i], puntos[i + 1])
    }
    fun estrella(color: Color, cx: Float = 50f, cy: Float = 45f, radio: Float = 35f) = forma(color) {
        repeat(10) { i ->
            val angulo = -Math.PI / 2 + i * Math.PI / 5
            val r = radio * if (i % 2 == 0) 1f else .47f
            val x = cx + kotlin.math.cos(angulo).toFloat() * r
            val y = cy + kotlin.math.sin(angulo).toFloat() * r
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }; close()
    }
    // Una sombra corta separa el sello del fondo sin animaciones ni desenfoques por ícono.
    oval(oscuro.copy(alpha = .3f), 17f, 66f, 68f, 14f)
    when (motivo) {
        "estrella" -> {
            estrella(oro); estrella(Color(0xFFFFE68C), 43f, 38f, 20f)
        }
        "vela" -> {
            linea(blanco, 3f, 50f, 12f, 50f, 76f)
            forma(blanco) { moveTo(46f, 15f); cubicTo(29f, 28f, 19f, 47f, 17f, 58f); lineTo(46f, 58f); close() }
            pol(rojo, 55f, 19f, 81f, 52f, 55f, 54f)
            pol(oro, 18f, 65f, 82f, 65f, 69f, 78f, 32f, 78f)
            pol(rojo, 50f, 12f, 65f, 17f, 50f, 21f)
        }
        "gema" -> {
            // Una gema facetada simple (dibujo propio).
            pol(menta, 30f, 18f, 70f, 18f, 86f, 38f, 50f, 82f, 14f, 38f)
            pol(blanco.copy(alpha = .55f), 30f, 18f, 50f, 38f, 14f, 38f)
            pol(blanco.copy(alpha = .3f), 70f, 18f, 86f, 38f, 50f, 38f)
            linea(oscuro.copy(alpha = .4f), 2f, 14f, 38f, 86f, 38f)
        }
        "espada" -> {
            oval(azul.copy(alpha = .15f), 13f, 6f, 76f, 76f)
            pol(blanco, 38f, 59f, 68f, 12f, 76f, 8f, 78f, 21f, 48f, 66f)
            linea(Color(0xFF8DC0D0), 2f, 37f, 60f, 74f, 18f)
            pol(oro, 22f, 57f, 52f, 76f, 57f, 70f, 26f, 50f)
            linea(Color(0xFF6F74CD), 8f, 23f, 78f, 37f, 61f)
        }
        "luna" -> {
            val luna = Path().apply { addOval(Rect(17f, 9f, 81f, 74f)) }
            val corte = Path().apply { addOval(Rect(37f, 0f, 91f, 56f)) }
            add(TrazoEmblema(Path.combine(PathOperation.Difference, luna, corte), blanco))
            estrella(oro, 75f, 22f, 10f); estrella(azul, 58f, 43f, 6f)
            pol(blanco, 45f, 75f, 68f, 60f, 61f, 82f, 56f, 73f)
        }
        "hoja" -> {
            forma(menta) { moveTo(26f, 70f); cubicTo(-2f, 30f, 37f, 7f, 82f, 14f); cubicTo(88f, 63f, 57f, 87f, 26f, 70f); close() }
            linea(Color(0xFF238B78), 3f, 20f, 82f, 66f, 31f)
            oval(oscuro, 68f, 37f, 17f, 17f)
        }
        "fantasma" -> {
            forma(rojo) { moveTo(18f, 78f); lineTo(18f, 43f); cubicTo(18f, 1f, 82f, 1f, 82f, 43f); lineTo(82f, 78f); lineTo(66f, 69f); lineTo(50f, 80f); lineTo(34f, 69f); close() }
            oval(blanco, 29f, 38f, 20f, 24f); oval(blanco, 51f, 38f, 20f, 24f)
            oval(oscuro, 38f, 44f, 8f, 13f); oval(oscuro, 54f, 44f, 8f, 13f)
        }
        "bloques" -> {
            val bloques = listOf(Triple(19f, 15f, azul), Triple(39f, 15f, azul), Triple(39f, 35f, azul), Triple(39f, 55f, azul), Triple(59f, 35f, rojo), Triple(59f, 55f, rojo), Triple(79f, 55f, rojo), Triple(19f, 55f, oro))
            bloques.forEach { (x,y,c) -> caja(c,x - 7f,y,18f,18f); caja(blanco.copy(alpha = .3f),x - 4f,y + 2f,12f,3f,1f) }
        }
        "nave", "viento" -> {
            if (motivo == "viento") {
                pol(menta, 50f, 7f, 60f, 35f, 88f, 45f, 60f, 55f, 50f, 83f, 40f, 55f, 12f, 45f, 40f, 35f)
                pol(blanco, 50f, 23f, 55f, 40f, 72f, 45f, 55f, 50f, 50f, 67f, 45f, 50f, 28f, 45f, 45f, 40f)
            } else {
                pol(azul, 50f, 9f, 69f, 47f, 85f, 69f, 58f, 62f, 50f, 77f, 42f, 62f, 15f, 69f, 31f, 47f)
                pol(blanco, 50f, 9f, 57f, 52f, 50f, 62f, 43f, 52f)
                pol(rojo, 42f, 68f, 50f, 89f, 58f, 68f)
            }
        }
        "carreras" -> {
            linea(blanco, 4f, 23f, 82f, 23f, 12f)
            for(y in 0..3) for(x in 0..3) caja(if((x+y)%2==0) blanco else oscuro, 26f+x*14, 14f+y*13,14f,13f,0f)
            linea(oro, 3f, 28f, 71f, 81f, 71f)
        }
        "robot" -> {
            caja(azul, 20f, 15f, 60f, 58f, 20f); caja(blanco, 30f, 34f, 40f, 29f, 12f)
            caja(Color(0xFF2681D8), 43f, 12f, 15f, 32f, 4f)
            caja(oscuro, 35f, 42f, 6f, 13f); caja(oscuro, 60f, 42f, 6f, 13f)
            caja(azul, 13f, 33f, 12f, 28f); caja(azul, 75f, 33f, 12f, 28f)
        }
        "montana" -> {
            pol(Color(0xFF7774D6), 9f, 76f, 41f, 18f, 67f, 76f)
            pol(azul, 40f, 76f, 68f, 32f, 93f, 76f)
            pol(blanco, 30f, 38f, 41f, 18f, 52f, 39f, 42f, 33f)
            pol(rojo, 48f, 53f, 61f, 48f, 65f, 59f, 52f, 65f)
        }
        "castillo", "ciudad" -> {
            oval(oro, 61f, 9f, 22f, 22f)
            caja(Color(0xFF8E88E8), 19f, 36f, 64f, 42f, 2f)
            for(x in listOf(19f, 42f, 66f)) { caja(azul,x,27f,17f,51f,2f); pol(blanco, x-3f,28f,x+8f,11f,x+20f,28f) }
            caja(oscuro,43f,56f,16f,22f,7f)
            for(x in listOf(24f, 71f)) caja(oro,x,41f,6f,11f,2f)
        }
        "tanque" -> {
            caja(oscuro, 15f, 51f, 72f, 25f, 12f); caja(menta, 13f, 41f, 72f, 19f, 6f)
            caja(Color(0xFF419E83), 34f, 22f, 33f, 23f, 7f); caja(menta, 52f, 26f, 37f, 8f, 2f)
            for(x in listOf(25f,43f,61f,79f)) oval(oro,x-5,58f,10f,10f)
            estrella(rojo,42f,48f,6f)
        }
        "calavera", "pirata", "dragon" -> {
            if(motivo=="dragon") { pol(oro,20f,32f,13f,7f,40f,25f); pol(oro,60f,25f,87f,7f,80f,32f) }
            else { linea(blanco,6f,21f,72f,79f,29f); linea(blanco,6f,21f,29f,79f,72f) }
            oval(if(motivo=="dragon") menta else blanco,25f,16f,52f,47f)
            caja(if(motivo=="dragon") menta else blanco,34f,48f,34f,23f,7f)
            oval(oscuro,33f,33f,13f,14f); oval(oscuro,56f,33f,13f,14f)
            if(motivo=="pirata") pol(oscuro,20f,28f,32f,8f,49f,17f,68f,8f,82f,28f)
        }
        "television" -> {
            caja(oro,14f,22f,72f,53f,11f); caja(oscuro,22f,29f,47f,37f,7f)
            linea(blanco,3f,31f,8f,48f,22f,65f,8f); oval(rojo,74f,37f,6f,6f); oval(blanco,74f,51f,6f,6f)
        }
        "brujula" -> {
            oval(oro,16f,10f,69f,69f,4f); pol(rojo,65f,21f,57f,50f,36f,68f,44f,40f)
            pol(blanco,65f,21f,57f,50f,44f,40f); oval(oro,47f,41f,8f,8f)
        }
        "pajaro" -> { oval(oro,20f,23f,56f,47f); pol(rojo,70f,43f,91f,50f,70f,56f); oval(blanco,52f,30f,20f,20f); oval(oscuro,63f,36f,7f,8f); oval(Color(0xFFFFA14D),15f,43f,35f,20f) }
        "serpiente" -> { linea(menta,14f,22f,70f,22f,39f,50f,39f,50f,68f,78f,68f,78f,27f); oval(menta,67f,16f,23f,23f); oval(oscuro,79f,20f,4f,4f); oval(rojo,26f,12f,13f,13f) }
        "miel" -> { caja(oro,24f,25f,54f,52f,15f); caja(Color(0xFFC88A3C),24f,19f,54f,12f); forma(blanco) { moveTo(28f,32f); lineTo(72f,32f); lineTo(72f,45f); cubicTo(62f,50f,56f,37f,50f,48f); cubicTo(45f,57f,39f,37f,28f,44f); close() } }
        "pinball" -> { pol(Color(0xFF7B75CC),15f,79f,26f,12f,75f,12f,86f,79f); oval(blanco,46f,20f,23f,23f); linea(rojo,9f,27f,65f,43f,72f); linea(rojo,9f,73f,65f,57f,72f) }
        "malabares" -> { oval(oro,16f,16f,15f,15f); oval(rojo,69f,16f,15f,15f); oval(blanco,42f,37f,17f,17f); linea(blanco,5f,50f,51f,50f,69f,34f,82f); linea(blanco,5f,50f,69f,66f,82f); linea(blanco,5f,25f,41f,37f,60f,50f,55f,63f,60f,75f,41f) }
        "tablero" -> { for(i in 0..2) for(j in 0..2) caja(azul.copy(alpha=.35f),18f+i*23f,13f+j*23f,20f,20f); oval(oro,23f,18f,10f,10f,3f); linea(rojo,4f,44f,39f,56f,51f); linea(rojo,4f,56f,39f,44f,51f); oval(oro,69f,64f,10f,10f,3f) }
        "coleccion" -> { for(i in 0..1) for(j in 0..1) caja(listOf(azul,oro,rojo,menta)[i*2+j],19f+i*34f,14f+j*34f,28f,28f,8f) }
        else -> error("Motivo sin dibujo: $motivo")
    }
}

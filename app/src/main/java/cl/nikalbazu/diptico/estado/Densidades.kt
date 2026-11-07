package cl.nikalbazu.diptico.estado

import kotlin.math.abs
import kotlin.math.ln

data class Densidad(val nombre: String, val escala: Float)

/** Las mismas paradas para el gesto, los accesos táctiles y el mando. */
class EscalaDensidades(val valores: List<Densidad>) {
    init { require(valores.isNotEmpty() && valores.all { it.escala.isFinite() && it.escala > 0 }) }
    fun limitar(valor: Float): Float = if (valor.isFinite()) valor.coerceIn(valores.first().escala, valores.last().escala) else 1f
    fun masCercana(valor: Float): Densidad {
        val v = limitar(valor)
        return valores.minBy { abs(ln(v / it.escala)) }
    }
    fun siguiente(valor: Float, delta: Int): Densidad =
        valores[(valores.indexOf(masCercana(valor)) + delta).coerceIn(valores.indices)]
}

object Densidades {
    val juegos = EscalaDensidades(listOf(Densidad("Compacto", .8f), Densidad("Equilibrado", 1f),
        Densidad("Amplio", 1.2f), Densidad("Detalle", 1.4f)))
    val carpetas = EscalaDensidades(listOf(Densidad("Compacto", .65f), Densidad("Equilibrado", 1f),
        Densidad("Amplio", 1.3f), Densidad("Grande", 1.6f), Densidad("Detalle", 1.9f)))
}

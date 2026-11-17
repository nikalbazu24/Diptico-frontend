package cl.nikalbazu.diptico.estado

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Lo que hace la persona durante el recorrido de DIPPO (botones que aprieta, toques a DIPPO), para que él reaccione.
 * [vez] cambia en cada reacción para que se repita aunque sea el mismo botón.
 */
object RecorridoVivo {
    data class Reaccion(val que: String, val vez: Long = System.nanoTime())

    val reaccion = MutableStateFlow<Reaccion?>(null)

    fun avisar(que: String) { reaccion.value = Reaccion(que) }

    /** Lo que hay que hacer sí o sí en cada paso del recorrido (null = basta con A). */
    fun retoDe(k: Int): String? = when (k) { 1 -> "gatillo"; 2 -> "x"; 3 -> "gatillo2"; 5 -> "select"; 6 -> "toque"; else -> null }

    /** El último paso cuyo reto ya se cumplió (para dejar seguir con A). */
    @Volatile var hecho: Int = -1

    /** ¿Se puede pasar del paso [k]? Solo si no tiene reto o ya se cumplió. */
    fun puedeSeguir(k: Int) = retoDe(k) == null || hecho == k
}

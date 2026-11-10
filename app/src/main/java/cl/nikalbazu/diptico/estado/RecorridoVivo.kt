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
}

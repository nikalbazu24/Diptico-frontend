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

    /** Cada desafío está ligado a una transición comprobada de la interfaz, nunca a una tecla vacía. */
    fun retoDe(k: Int): String? = when (k) {
        1 -> "gatillo"
        2 -> "x"
        3, 11 -> "gatillo2"
        5 -> "select"
        6 -> "toque"
        7 -> "abrirCarpeta"
        8 -> "vistaJuego"
        9 -> "opcionesJuego"
        10 -> "volverCarpetas"
        12 -> "paginaWidget"
        13 -> "galeriaWidget"
        14 -> "editarWidget"
        16 -> "apps"
        17 -> "buscar"
        18 -> "ajustes"
        19 -> "perfil"
        else -> null
    }

    @Volatile var hecho: Int = -1

    /** Los desafíos dependientes de una biblioteca vacía o de páginas inexistentes son opcionales. */
    fun puedeSeguir(k: Int): Boolean {
        if (retoDe(k) == null || hecho == k) return true
        val e = Diptico.estado.value
        return when (k) {
            7, 8, 9 -> e.juegos.isEmpty()
            10 -> e.enSistemas
            12 -> e.paginas < 2
            else -> false
        }
    }
}

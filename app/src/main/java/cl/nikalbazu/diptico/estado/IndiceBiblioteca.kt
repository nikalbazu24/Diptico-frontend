package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.data.Juego

/** Índices y vistas que se reutilizan mientras el catálogo siga siendo el mismo. */
internal class IndiceBiblioteca private constructor(private val juegos: List<Juego>) {
    val porConsola by lazy { juegos.groupBy { it.consolaId } }
    val porClave by lazy { buildMap { juegos.forEach { getOrPut(it.clave) { it } } } }
    val sinPortada by lazy { juegos.count { it.portada == null } }

    private val titulosNormalizados = HashMap<String, String>()
    private val vistas = ArrayDeque<Vista>()

    private class Vista(
        val datos: Map<String, DatosJuego>,
        val consola: String,
        val filtro: String,
        val consulta: String,
        val orden: String,
        val ordenManual: List<String>,
        val juegos: List<Juego>,
    )

    @Synchronized
    fun visibles(e: Estado): List<Juego> {
        val manual = if (e.orden == "propio") e.ordenPropio[e.consolaSel].orEmpty() else emptyList()
        val i = vistas.indexOfFirst {
            it.datos === e.datos && it.consola == e.consolaSel && it.filtro == e.filtro &&
                it.consulta == e.busqueda && it.orden == e.orden && it.ordenManual === manual
        }
        if (i >= 0) {
            val vista = vistas.removeAt(i)
            vistas.addFirst(vista)
            return vista.juegos
        }

        val origen = if (e.consolaSel == "todo") juegos else porConsola[e.consolaSel].orEmpty()
        val consulta = if (e.busqueda.isNotBlank()) Busqueda.normalizar(e.busqueda) else null
        val filtrados = origen.filter { juego ->
            val dato = e.datos[juego.clave]
            if ((dato?.oculto == true) != (e.filtro == "ocultos")) return@filter false
            val coincide = when (e.filtro) {
                "favoritos" -> dato?.favorito == true
                "recientes" -> (dato?.ultimo ?: 0L) > 0L
                "mas" -> (dato?.tiempo ?: 0L) > 0L
                "sinjugar" -> (dato?.tiempo ?: 0L) == 0L
                else -> true
            }
            coincide && (consulta == null ||
                titulosNormalizados.getOrPut(juego.titulo) { Busqueda.normalizar(juego.titulo) }.contains(consulta))
        }
        val resultado = when (e.orden) {
            "propio" -> {
                val posiciones = buildMap { manual.forEachIndexed { i, clave -> getOrPut(clave) { i } } }
                filtrados.sortedWith(compareBy({ posiciones[it.clave] ?: Int.MAX_VALUE }, { it.titulo.lowercase() }))
            }
            "recientes" -> filtrados.sortedByDescending { e.datos[it.clave]?.ultimo ?: 0L }
            "tiempo" -> filtrados.sortedByDescending { e.datos[it.clave]?.tiempo ?: 0L }
            else -> if (e.consolaSel == "todo" && e.filtro == "todos" && consulta == null) filtrados
            else filtrados.sortedBy { it.titulo.lowercase() }
        }
        if (vistas.size == 4) vistas.removeLast()
        vistas.addFirst(Vista(e.datos, e.consolaSel, e.filtro, e.busqueda, e.orden, manual, resultado))
        return resultado
    }

    companion object {
        private var ultimo: IndiceBiblioteca? = null

        @Synchronized
        fun de(juegos: List<Juego>): IndiceBiblioteca =
            ultimo?.takeIf { it.juegos === juegos } ?: IndiceBiblioteca(juegos).also { ultimo = it }
    }
}

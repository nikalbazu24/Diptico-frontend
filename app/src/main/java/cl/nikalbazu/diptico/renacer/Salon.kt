package cl.nikalbazu.diptico.renacer

import cl.nikalbazu.diptico.data.CarpetaJuegos
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.data.Juego
import java.text.Normalizer
import java.util.Locale

/** Navegación del nuevo Díptico. Cada destino tiene una única función. */
enum class Lugar(val nombre: String) { JUGAR("Jugar"), BIBLIOTECA("Biblioteca"), AMBIENTE("Ambiente") }
enum class Filtro(val nombre: String) { TODOS("Todos"), FAVORITOS("Favoritos"), RECIENTES("Recientes"), OCULTOS("Ocultos") }
enum class Ambiente(val nombre: String, val descripcion: String, val acento: Long, val fondo: Long) {
    PAPEL("Papel", "Luz cálida, tinta y terracota.", 0xFFBD5638, 0xFFF4EFE5),
    BOSQUE("Bosque", "Verdes profundos y un poco de calma.", 0xFFB7D6AD, 0xFF182B28),
    NOCHE("Noche", "Tinta azul y luz de sobremesa.", 0xFFDFB975, 0xFF161D2B),
}

sealed interface Ventana {
    data class Juego(val clave: String) : Ventana
    data object Fuentes : Ventana
    data object Apps : Ventana
}

data class Partida(val clave: String, val pantalla: Int, val inicio: Long, val salio: Boolean = false)

data class Salon(
    val juegos: List<Juego> = emptyList(),
    val datos: Map<String, DatosJuego> = emptyMap(),
    val carpetas: List<CarpetaJuegos> = emptyList(),
    val lugar: Lugar = Lugar.JUGAR,
    val consola: String? = null,
    val consulta: String = "",
    val filtro: Filtro = Filtro.TODOS,
    val elegido: String? = null,
    val ventana: Ventana? = null,
    val cargando: Boolean = true,
    val aviso: String? = null,
    val error: String? = null,
    val ambiente: Ambiente = Ambiente.PAPEL,
    val calendario: Boolean = true,
    val movimiento: Boolean = true,
    val dosPantallas: Boolean = false,
    val usarDos: Boolean = true,
    val partida: Partida? = null,
    val lista: Boolean = false,
) {
    val porClave by lazy { juegos.associateBy { it.clave } }
    val porConsola by lazy { juegos.filterNot { datos[it.clave]?.oculto == true }.groupBy { it.consolaId } }
    val recientes by lazy { juegos.filter { datos[it.clave]?.oculto != true && (datos[it.clave]?.ultimo ?: 0L) > 0L }
        .sortedByDescending { datos[it.clave]?.ultimo }.take(8) }
    val visibles by lazy {
        val texto = normalizar(consulta.trim())
        juegos.filter { juego ->
            val dato = datos[juego.clave]
            (consola == null || consola == "*" || juego.consolaId == consola) &&
                (dato?.oculto == true) == (filtro == Filtro.OCULTOS) &&
                when (filtro) {
                    Filtro.FAVORITOS -> dato?.favorito == true
                    Filtro.RECIENTES -> (dato?.ultimo ?: 0L) > 0L
                    else -> true
                } && (texto.isEmpty() || normalizar(juego.titulo).contains(texto))
        }.let { if (filtro == Filtro.RECIENTES) it.sortedByDescending { j -> datos[j.clave]?.ultimo } else it }
    }
    val protagonista: Juego? get() = porClave[elegido]?.takeIf { datos[it.clave]?.oculto != true }
        ?: recientes.firstOrNull() ?: juegos.firstOrNull { datos[it.clave]?.oculto != true }

    fun ir(lugar: Lugar) = copy(lugar = lugar, ventana = null, aviso = null)
    fun abrirConsola(id: String?) = copy(lugar = Lugar.BIBLIOTECA, consola = id, consulta = "", filtro = Filtro.TODOS, ventana = null)
    fun limpiar() = copy(consulta = "", filtro = Filtro.TODOS)
    fun volver(): Salon = when {
        ventana != null -> copy(ventana = null)
        consulta.isNotEmpty() || filtro != Filtro.TODOS -> limpiar()
        consola != null -> abrirConsola(null)
        else -> ir(Lugar.JUGAR)
    }

    companion object {
        private val marcas = Regex("\\p{M}")
        fun normalizar(texto: String) = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(marcas, "").lowercase(Locale.ROOT)
    }
}

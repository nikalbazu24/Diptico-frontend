package cl.nikalbazu.diptico.estado

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Un amigo de Discord, como lo entrega el kit oficial de Discord (Social SDK). */
data class AmigoDiscord(
    val id: String,
    val nombre: String,
    val avatar: String? = null,
    /** "jugando", "conectado", "ausente", "ocupado" o "desconectado". */
    val estado: String = "desconectado",
    /** A qué juega, si está jugando (el nombre que muestra Discord). */
    val actividad: String? = null,
    /** Detalle de la actividad (nivel, modo, «en el menú»…). */
    val detalle: String? = null,
    /** Juega desde Díptico. */
    val enDiptico: Boolean = false,
)

/**
 * Lo social de Discord: tus amigos y quién juega. Lo llena el kit oficial de Discord (Social SDK) cuando está dentro
 * de la app ([disponible]); sin el kit la ventana de amigos explica qué falta en vez de mostrar una lista falsa.
 */
object SocialDiscord {
    private val _amigos = MutableStateFlow<List<AmigoDiscord>>(emptyList())
    val amigos: StateFlow<List<AmigoDiscord>> = _amigos

    /** El kit de Discord está dentro de esta versión de Díptico. */
    @Volatile var disponible: Boolean = false

    fun publicar(lista: List<AmigoDiscord>) { _amigos.value = lista }

    /** Orden de la lista: primero los que juegan, luego conectados, ausentes y al final los desconectados. */
    fun ordenar(l: List<AmigoDiscord>): List<AmigoDiscord> = l.sortedWith(
        compareBy<AmigoDiscord> { when (it.estado) { "jugando" -> 0; "conectado" -> 1; "ocupado" -> 2; "ausente" -> 3; else -> 4 } }
            .thenBy { it.nombre.lowercase() },
    )
}

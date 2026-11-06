package cl.nikalbazu.diptico.estado

/** Estado del cajón y de la retoma, separado del estado general de la biblioteca. */
data class NavegacionEstado(
    val atajoApps: String = AtajoInicio.START_R3.id,
    val atajoBisa: String = AtajoInicio.START_L3.id,
    val ordenApps: List<String> = emptyList(),
    val appsMenu: String? = null,
    val appsMenuIndice: Int = 0,
    val appsMoviendo: String? = null,
    val estanteAvanzado: Boolean = false,
    val clipRetoma: Boolean = false,
    val esperaRetoma: Int = 30,
    val retomaVideo: String = "",
    val retomaVideoClave: String = "",
    val retomaVideoFecha: Long = 0L,
    val retomaApagadaEn: Long = 0L,
)

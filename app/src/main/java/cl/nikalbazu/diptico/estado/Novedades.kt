package cl.nikalbazu.diptico.estado



/**
 * Tour de novedades: se muestra una vez al abrir Díptico después de actualizar.
 * Cada versión cambia [ID] y sus páginas; el tour sale solo si la persona todavía no vio este [ID].
 */
object Novedades {
    data class Pagina(val titulo: String, val texto: String, val icono: Icono, val color: Long, val tecla: String = "")

    const val ID = "1.0-beta.6"
    const val NOMBRE = "1.0 beta.6"

    val paginas = listOf(
        Pagina(
            "Novedades de Díptico", "Un recorrido cortito por lo nuevo. A para seguir, B para saltarlo cuando quieras.",
            Icono.CHISPA, 0xFF6B4EFF,
        ),
        Pagina(
            "Más fuentes de imágenes", "Ahora Díptico también busca portadas, arte y logos en ScreenScraper, IGDB, TheGamesDB y RAWG. Conéctalas en Menú › Portadas (son gratis).",
            Icono.PORTADA, 0xFF3BA8FF, "Menú › Portadas",
        ),
        Pagina(
            "Tienda con más pestañas", "Al cambiar una imagen (Y › Imágenes), cada fuente conectada tiene su propia pestaña para elegir.",
            Icono.BUSCAR, 0xFF2EC4A6, "Y · Imágenes",
        ),
        Pagina(
            "Versión para teléfonos", "Si instalas Díptico en un teléfono, usa el archivo «-movil»: no pide acceso a las notificaciones y Play Protect no lo bloquea. Se actualiza sola a su mismo tipo.",
            Icono.MANDO, 0xFFFFA726,
        ),
    )
}

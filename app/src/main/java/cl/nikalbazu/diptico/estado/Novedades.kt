package cl.nikalbazu.diptico.estado



/**
 * Tour de novedades: se muestra una vez al abrir Díptico después de actualizar.
 * Cada versión cambia [ID] y sus páginas; el tour sale solo si la persona todavía no vio este [ID].
 */
object Novedades {
    data class Pagina(val titulo: String, val texto: String, val icono: Icono, val color: Long, val tecla: String = "")

    const val ID = "1.0-beta.4"
    const val NOMBRE = "1.0 beta.4"

    val paginas = listOf(
        Pagina(
            "Novedades de Díptico", "Un recorrido cortito por lo nuevo. A para seguir, B para saltarlo cuando quieras.",
            Icono.CHISPA, 0xFF6B4EFF,
        ),
        Pagina(
            "¿Arriba o abajo?", "Mantén A sobre un juego o una app y elige en qué pantalla se abre. Puedes dejarlo recordado para ese juego.",
            Icono.PANTALLAS, 0xFF3BA8FF, "Mantén A",
        ),
        Pagina(
            "¿Dónde quedaste?", "Al terminar de jugar: escríbelo, déjalo en una nota de voz o márcalo en el mapa del juego. O pasa de largo. Se apaga en Menú › Inicio.",
            Icono.LAPIZ, 0xFFFF7A59, "Nota de voz",
        ),
        Pagina(
            "Turbo a un toque", "El botón Turbo de la barra libera memoria al instante. Mantenlo para encender o apagar el turbo al jugar.",
            Icono.CHISPA, 0xFFFF5252, "Turbo",
        ),
        Pagina(
            "Un stick para cada cosa", "El izquierdo elige tus juegos y el derecho recorre los widgets. En el editor, el stick desliza la pantalla. Se cambia en Menú › Controles.",
            Icono.MANDO, 0xFFFFA726, "Sticks",
        ),
        Pagina(
            "Widgets pulidos", "Marcos que siempre se ven completos, GIF sin fondo que flotan sobre tu inicio y cada widget abierto en grande en la pantalla de arriba.",
            Icono.APPS, 0xFF2EC4A6, "Widgets",
        ),
        Pagina(
            "Todos parejos", "En «Todos» las cajas se ven iguales (póster o cuadradas). Y eliges qué imagen va en grande en el inicio desde la ventana de cada juego (Y).",
            Icono.FORMA, 0xFFB388FF, "Y · Imagen en el inicio",
        ),
        Pagina(
            "Actualiza desde aquí", "Cuando salga una versión nueva te aviso, y la descargas e instalas desde Menú › Acerca de, sin buscar en GitHub.",
            Icono.DESCARGA, 0xFFFFC93C, "Acerca de",
        ),
    )
}

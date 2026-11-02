package cl.nikalbazu.diptico.estado



/**
 * Tour de novedades: se muestra una vez al abrir Díptico después de actualizar.
 * Cada versión cambia [ID] y sus páginas; el tour sale solo si la persona todavía no vio este [ID].
 */
object Novedades {
    data class Pagina(val titulo: String, val texto: String, val icono: Icono, val color: Long, val tecla: String = "")

    const val ID = "1.0-beta.6"
    const val NOMBRE = "1.0 beta.6"

    /** Páginas del tour que toca: el recorrido de bienvenida (primera vez) o las novedades de esta versión. */
    fun paginasDe(recorrido: Boolean) = if (recorrido) RECORRIDO else paginas

    /** Recorrido por las opciones y funciones: se muestra la primera vez, al terminar de configurar Díptico. */
    val RECORRIDO = listOf(
        Pagina(
            "¡Bienvenido a Díptico!", "Te muestro en un minuto cómo se usa. A o → para seguir, ← para volver, B para saltarlo. Puedes verlo otra vez en Menú › Acerca de.",
            Icono.CHISPA, 0xFF6B4EFF,
        ),
        Pagina(
            "Dos pantallas, un díptico", "Arriba se luce el juego elegido, con su arte grande y su logo. Abajo eliges consola y juego. Mantén Select para intercambiarlas.",
            Icono.PANTALLAS, 0xFF3BA8FF, "Mantener Select",
        ),
        Pagina(
            "Elegir y jugar", "Muévete con la cruceta o el stick, cambia de consola con L1 y R1, y salta de letra con L2 y R2. A para jugar.",
            Icono.JUGAR, 0xFF2EC4A6, "L1 · R1 · A",
        ),
        Pagina(
            "¿En qué pantalla?", "Mantén A sobre un juego o app para elegir en qué pantalla se abre, y si quieres, que lo recuerde.",
            Icono.INTERCAMBIAR, 0xFF00BFA5, "Mantener A",
        ),
        Pagina(
            "La ventana del juego", "Y abre las opciones rápidas del juego: imágenes, colecciones, acceso directo y más. X abre su ficha, con su historia, tus notas y trucos.",
            Icono.EDITAR, 0xFFFFA726, "Y · X",
        ),
        Pagina(
            "Buscar y cambiar la vista", "Presiona el stick izquierdo para buscar un juego. El stick derecho cambia la forma de ver los juegos: estante, grilla, carrusel y más.",
            Icono.BUSCAR, 0xFF8E7CFF, "L3 · R3",
        ),
        Pagina(
            "Tu inicio de widgets", "Select cambia entre tus juegos y el inicio de widgets: reloj, música, batería, tiempo de juego, colecciones y más. Ahí puedes acomodarlos a tu gusto.",
            Icono.FORMA, 0xFFFF7A59, "Select",
        ),
        Pagina(
            "Centro de control", "Mantén Start para el Wi-Fi, el Bluetooth, el brillo, el turbo y No molestar. Lo que está encendido se pinta de verde.",
            Icono.WIFI, 0xFF2EC4A6, "Mantener Start",
        ),
        Pagina(
            "Bisa, tu ayudante", "Pregúntale qué jugar, cómo pasar una parte o que cambie algo de Díptico. Puedes hablarle en vez de escribir, y busca en internet por ti.",
            Icono.BISA, 0xFFFF7A59, "Hablar",
        ),
        Pagina(
            "Continúa donde lo dejaste", "Al salir de un juego, Díptico te pregunta dónde quedaste: puedes escribirlo, grabar una nota de voz o marcarlo en el mapa. La próxima vez te lo recuerda.",
            Icono.CONTINUAR, 0xFFB388FF,
        ),
        Pagina(
            "El menú", "Start abre el menú: temas, portadas, pantallas, música, Bisa y mucho más. Todo se puede cambiar. ¡Que lo disfrutes!",
            Icono.MENU, 0xFF6B4EFF, "Start",
        ),
    )

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

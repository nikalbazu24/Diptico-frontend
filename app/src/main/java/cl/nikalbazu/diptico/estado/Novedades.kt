package cl.nikalbazu.diptico.estado



/**
 * Tour de novedades: se muestra una vez al abrir Díptico después de actualizar.
 * Cada versión cambia [ID] y sus páginas; el tour sale solo si la persona todavía no vio este [ID].
 */
object Novedades {
    data class Pagina(val titulo: String, val texto: String, val icono: Icono, val color: Long, val tecla: String = "")

    const val ID = "1.0-beta.5"
    const val NOMBRE = "1.0 beta.5"

    val paginas = listOf(
        Pagina(
            "Novedades de Díptico", "Un recorrido cortito por lo nuevo. A para seguir, B para saltarlo cuando quieras.",
            Icono.CHISPA, 0xFF6B4EFF,
        ),
        Pagina(
            "Bisa, más lista", "Háblale con el botón Hablar en vez de escribir, busca en internet por ti y te ofrece respuestas listas para tocar. Y estrena cara.",
            Icono.BISA, 0xFFFF7A59, "Hablar",
        ),
        Pagina(
            "Teclado grande", "Teclas más grandes, una tecla Voz para dictar, y el teclado de Android ya no aparece encima.",
            Icono.TEXTO, 0xFF3BA8FF, "Voz",
        ),
        Pagina(
            "Colecciones y accesos", "Junta juegos en tus propias carpetas o deja un juego a un toque en tu inicio de widgets. En la ventana de cada juego (Y).",
            Icono.CARPETA, 0xFF2EC4A6, "Y · Colecciones",
        ),
        Pagina(
            "Vista Carpetas", "Cada consola como una carpeta con sus portadas asomándose. Menú › Estante › Consolas.",
            Icono.VISTA, 0xFFFFA726, "Carpetas",
        ),
        Pagina(
            "Lo encendido se nota", "En el centro de control, lo que está activo (Wi-Fi, Bluetooth, turbo, No molestar) se pinta de verde.",
            Icono.WIFI, 0xFF2EC4A6, "Centro de control",
        ),
        Pagina(
            "Díptico conoce tu equipo", "Reconoce si es una Thor, una Odin, una Retroid, un teléfono o una tablet y se acomoda solo. Menú › Pantallas › Tu equipo.",
            Icono.MANDO, 0xFFB388FF, "Tu equipo",
        ),
        Pagina(
            "Switch 2 y más", "Consola Switch 2 lista para cuando haya emulador, y más nombres de carpetas reconocidos. Además: bienvenida animada, carrusel sin parpadeo y la portada más limpia.",
            Icono.EMULADOR, 0xFFE60012, "Más consolas",
        ),
    )
}

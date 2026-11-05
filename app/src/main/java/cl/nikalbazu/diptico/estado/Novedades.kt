package cl.nikalbazu.diptico.estado



/**
 * Tour de novedades: se muestra una vez al abrir Díptico después de actualizar.
 * Cada versión cambia [ID] y sus páginas; el tour sale solo si la persona todavía no vio este [ID].
 */
object Novedades {
    data class Pagina(val titulo: String, val texto: String, val icono: Icono, val color: Long, val tecla: String = "", val version: String = "")

    /** Las novedades de una versión. */
    data class Version(val id: String, val nombre: String, val paginas: List<Pagina>)

    const val ID = "mesa-partida-1"
    const val NOMBRE = "Tu partida, a tu manera"

    /**
     * Todas las versiones con novedades, de la más vieja a la más nueva.
     * Para una versión nueva: se agrega al final y se cambian [ID] y [NOMBRE].
     */
    val HISTORIAL = listOf(
        Version("1.0-beta.4", "1.0 beta.4", listOf(
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
        )
        )),
        Version("1.0-beta.5", "1.0 beta.5", listOf(
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
        )
        )),
        Version("1.0-beta.6", "1.0 beta.6", listOf(
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
        )
        )),
        Version("1.0-beta.7", "1.0 beta.7", listOf(
            Pagina(
                "Tus fan games, directo", "Nueva consola RPG Maker: los fan games de Pokémon y otros se abren al tiro con Essentia. Pon cada juego en su carpeta dentro de «rpgmaker» o «fangames».",
                Icono.EMULADOR, 0xFFE0452B, "Consola RPG Maker",
            ),
            Pagina(
                "Notificaciones y ajustes, por separado", "Desliza hacia abajo desde la izquierda de arriba: tus notificaciones. Desde la derecha: los ajustes rápidos. Con el control, mantén Start y pasa de uno a otro con L1 y R1.",
                Icono.CAMPANA, 0xFF3BA8FF, "L1 · R1",
            ),
            Pagina(
                "Respaldo en Google Drive", "Guarda tus ajustes, colecciones, tiempos y notas en tu Drive, y Díptico lo actualiza solo cada día. Menú › Acerca de › Respaldo en Google Drive.",
                Icono.DESCARGA, 0xFF2EC4A6, "Menú › Acerca de",
            ),
            Pagina(
                "Una cosa a la vez", "Al abrir, los avisos y las tarjetas salen de a uno, nunca encimados. Y eliges con qué parte Díptico: Menú › Inicio › Al abrir Díptico.",
                Icono.CHECK, 0xFF8E7CFF, "Al abrir Díptico",
            ),
            Pagina(
                "Accesos directos a tu gusto", "El widget de un juego puede mostrar la portada, la imagen grande, el logo o el logo sobre la imagen. En sus ajustes, Diseño.",
                Icono.CONTINUAR, 0xFFFFA726, "Diseño",
            ),
            Pagina(
                "Desinstalar desde Díptico", "Y › Desinstalar borra un juego y todo lo suyo sin entrar a los archivos. Te muestra qué se borra y pide mantener A para confirmar. Tus partidas se conservan.",
                Icono.BORRAR, 0xFFE5484D, "Y · Desinstalar",
            ),
            Pagina(
                "Más fluido y más firme", "Arranca y se mueve más suave, los menús ya no cortan los textos, y si algo falla te ofrece mandar el reporte con un toque. Bisa ahora usa la IA de tu consola, Gemini o ChatGPT.",
                Icono.CHISPA, 0xFFFF7A59,
            ),
        )),
        Version("1.0-beta.8", "1.0 beta.8", listOf(
            Pagina(
                "Un inicio más despejado", "La barra de abajo ahora flota arriba y se esconde sola a los 5 segundos. Vuelve deslizando hacia arriba desde el borde de abajo, tocando o con B. Para dejarla fija: Menú › Inicio.",
                Icono.APPS, 0xFF3BA8FF, "Desliza ↑ o B",
            ),
            Pagina(
                "Menú simple", "El menú muestra solo lo de todos los días y lo avanzado queda junto en «Más ajustes», a un toque. Cada opción explica qué hace y las parecidas van juntas.",
                Icono.VISTA, 0xFF8E7CFF, "Más ajustes",
            ),
            Pagina(
                "Vista Discos", "Cada consola es un vinilo en su funda con cuatro de sus portadas. La elegida saca su disco girando. Menú › Estante › Consolas › Discos.",
                Icono.MUSICA, 0xFFFF7A59, "Discos",
            ),
            Pagina(
                "Vista Abanico", "Tus juegos como cartas abiertas en la mano: la elegida sube al centro. Cámbiala con R3 o en Menú › Estante › Juegos.",
                Icono.PORTADA, 0xFFFFA726, "R3",
            ),
            Pagina(
                "Más rápido con tus fan games", "Díptico recuerda tus fan games en vez de leerlos cada vez, las imágenes ocupan menos memoria y la música vuelve sola al regresar de Essentia u otra app.",
                Icono.CHISPA, 0xFF2EC4A6,
            ),
        )),
        Version("estudio-visual-1", "Estudio visual", listOf(
            Pagina("Tu Díptico, a tu manera", "Menú › Estudio visual: Universo del juego toma colores de tus imágenes; tarjetas de vidrio y movimiento ambiental a elección.", Icono.PALETA, 0xFF57DCC7),
            Pagina("Tu habitación", "Consolas, colecciones y recuerdos en un espacio propio. Cambia la pared y los objetos en Estudio visual. Cruceta elige y A entra.", Icono.BIBLIOTECA, 0xFF8EAFFF),
            Pagina("Colecciones con identidad", "Elige color, fondo, logo, música y disposición para cada colección. Comparte y guarda perfiles visuales con la composición de tus widgets.", Icono.CARPETA, 0xFFFFB46A),
            Pagina("Una ficha en la otra pantalla", "Desliza hacia arriba la tarjeta «Enviar ficha» del estante para dejar el juego fijado arriba mientras recorres otros títulos. Tócala para soltarlo.", Icono.PANTALLAS, 0xFFBCA4FF),
            Pagina("Actualizador más tranquilo", "La ayuda conserva su panel al cambiar el porcentaje de descarga y la pantalla inferior evita reaplicar opciones de ventana sin cambios.", Icono.DESCARGA, 0xFF57DCC7),
        )),
        Version("navegacion-retoma-1", "Navegación y retoma", listOf(
            Pagina("Una barra, en su lugar", "La barra queda fija sobre la imagen del juego. Al pasar a Widgets sube, y reúne los accesos en un solo lugar.", Icono.APPS, 0xFF57DCC7),
            Pagina("Tu cajón de apps", "Start + Y abre las apps. Mantén una app o pulsa X para moverla, fijarla, abrir sus opciones o pedir a Android que la desinstale. El atajo se cambia en Controles.", Icono.APPS, 0xFF73ABFF),
            Pagina("Widgets con dos dedos", "Abre los dedos para ver menos casillas y widgets más grandes; júntalos para ver más. Si hace falta espacio se agregan páginas, sin ocultar widgets.", Icono.FORMA, 0xFFFFB46A),
            Pagina("Elige la vista directamente", "R3 muestra tus vistas, con previa en la otra pantalla. Vista, filtro y orden quedan a mano en el estante; las opciones finas están agrupadas.", Icono.VISTA, 0xFFBCA4FF),
            Pagina("Retoma donde lo dejaste", "En Inicio eliges cuánto debe durar la pausa y puedes activar un clip opcional de los últimos 6 segundos, sin sonido. Android pide permiso. Con lupa o sin clip se usa la imagen del juego.", Icono.CONTINUAR, 0xFF57DCC7),
        )),
        Version("editor-widgets-1", "Edición de widgets", listOf(
            Pagina("Pasa widgets entre páginas", "En el editor, usa las flechas para cambiar de página o + para crear otra. Arrastra un widget hacia un borde y espera un instante para cruzar sin soltarlo.", Icono.APPS, 0xFF57DCC7),
            Pagina("Estira y mira el cambio", "El widget sigue tu dedo desde la manija de la esquina. Al soltar se ajusta a la grilla. Agrega Temporizador, Cronómetro o Buscar juegos desde el catálogo.", Icono.FORMA, 0xFFBCA4FF),
            Pagina("Avisos de actualización", "Díptico comprueba nuevas versiones al volver a la app y en segundo plano. Permite las notificaciones en Android para recibir el aviso.", Icono.CAMPANA, 0xFF73ABFF),
        )),
        Version(ID, NOMBRE, listOf(
            Pagina("Tu pantalla de partida", "Toca Portada, Bitácora, Herramientas o Ligera en la pantalla libre. La vista se recuerda para cada juego. Desde sus opciones también la puedes cambiar con el mando.", Icono.PANTALLAS, 0xFF57DCC7),
            Pagina("Un inicio para cada momento", "En Estudio visual prueba Jugar, Explorar o Ligero. Puedes ajustar los widgets, guardar el perfil y volver a tu distribución anterior.", Icono.PALETA, 0xFFBCA4FF),
            Pagina("Planea tu próxima partida", "Agrega Mi próxima partida, Cola de juegos o Tengo 20 minutos al inicio. Desde las opciones de un juego puedes marcarlo pendiente, jugando o terminado.", Icono.CONTINUAR, 0xFFFFB46A),
            Pagina("Trae tu biblioteca", "En Biblioteca puedes importar títulos y favoritos de un gamelist.xml de ES-DE. Revisa las coincidencias antes de aplicar los cambios.", Icono.BIBLIOTECA, 0xFF73ABFF),
        )),
    )

    /**
     * Las versiones que la persona todavía no vio, de la más nueva a la más vieja.
     * [vista] es la última versión cuyas novedades vio; si no está en el historial (venía de antes), se muestran todas.
     */
    fun pendientes(vista: String): List<Version> {
        val k = HISTORIAL.indexOfFirst { it.id == vista }
        return HISTORIAL.drop(if (k >= 0) k + 1 else 0).reversed().ifEmpty { listOf(HISTORIAL.last()) }
    }

    /** Páginas del tour: el recorrido de bienvenida, o las novedades juntas de todas las versiones que te saltaste. */
    fun paginasDe(recorrido: Boolean, vista: String = HISTORIAL.getOrNull(HISTORIAL.size - 2)?.id ?: ""): List<Pagina> {
        if (recorrido) return RECORRIDO
        val vs = pendientes(vista)
        val intro = if (vs.size == 1) Pagina(
            "Novedades de Díptico", "Un recorrido cortito por lo nuevo. A para seguir, B para saltarlo cuando quieras.",
            Icono.CHISPA, 0xFF6B4EFF,
        ) else Pagina(
            "Te pusiste al día", "Saltaste ${vs.size} versiones, así que aquí va todo lo nuevo junto, empezando por lo más reciente. A para seguir, B para saltarlo.",
            Icono.CHISPA, 0xFF6B4EFF,
        )
        return listOf(intro) + vs.flatMap { v -> v.paginas.map { it.copy(version = v.nombre) } }
    }

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
            "La barra que se esconde", "Arriba flota una barra con jugar, buscar, apps, turbo, el menú y Bisa. A los 5 segundos se esconde para que luzca el juego; vuelve deslizando hacia arriba desde el borde de abajo, tocando o con B.",
            Icono.APPS, 0xFF3BA8FF, "Desliza ↑ · B",
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
            "El menú", "Start abre el menú: lo de todos los días a la vista y lo avanzado junto en «Más ajustes». Cada opción explica qué hace, y con Y buscas cualquier ajuste por su nombre. ¡Que lo disfrutes!",
            Icono.MENU, 0xFF6B4EFF, "Start",
        ),
    )
}



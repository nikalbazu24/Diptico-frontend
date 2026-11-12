package cl.nikalbazu.diptico.estado

/**
 * Tour de novedades: se muestra una vez al abrir Díptico después de actualizar.
 * Cada versión cambia [ID] y sus páginas; el tour sale solo si la persona todavía no vio este [ID].
 */
object Novedades {
    data class Pagina(val titulo: String, val texto: String, val icono: Icono, val color: Long, val tecla: String = "", val version: String = "")

    /** Las novedades de una versión. */
    data class Version(val id: String, val nombre: String, val paginas: List<Pagina>)

    const val ID = "ajustes-vistas-2.0-62"
    const val NOMBRE = "Ajustes ordenados"

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
            Pagina("Tu Díptico, a tu manera", "Menú › Apariencia: Universo del juego toma colores de tus imágenes; tarjetas de vidrio y movimiento ambiental a elección.", Icono.PALETA, 0xFF57DCC7),
            Pagina("Tu habitación", "Consolas, colecciones y recuerdos en un espacio propio. Cambia la pared y los objetos en Apariencia. Cruceta elige y A entra.", Icono.BIBLIOTECA, 0xFF8EAFFF),
            Pagina("Colecciones con identidad", "Elige color, fondo, logo, música y disposición para cada colección. Comparte y guarda perfiles visuales con la composición de tus widgets.", Icono.CARPETA, 0xFFFFB46A),
            Pagina("Una ficha en la otra pantalla", "Usa «Enviar ficha a la otra pantalla» en el menú de personalización. Ahí mismo puedes soltarla cuando quieras.", Icono.PANTALLAS, 0xFFBCA4FF),
            Pagina("Actualizador más tranquilo", "La ayuda conserva su panel al cambiar el porcentaje de descarga y la pantalla inferior evita reaplicar opciones de ventana sin cambios.", Icono.DESCARGA, 0xFF57DCC7),
        )),
        Version("navegacion-retoma-1", "Navegación y retoma", listOf(
            Pagina("Una barra, en su lugar", "La barra queda fija sobre la imagen del juego. Al pasar a Widgets sube, y reúne los accesos en un solo lugar.", Icono.APPS, 0xFF57DCC7),
            Pagina("Tu cajón de apps", "En Controles puedes elegir cómo abrir las apps. Mantén una app o pulsa X para moverla, fijarla, abrir sus opciones o pedir a Android que la desinstale. El atajo se cambia en Controles.", Icono.APPS, 0xFF73ABFF),
            Pagina("Widgets con dos dedos", "Abre los dedos para ver menos casillas y widgets más grandes; júntalos para ver más. Si hace falta espacio se agregan páginas, sin ocultar widgets.", Icono.FORMA, 0xFFFFB46A),
            Pagina("Elige la vista directamente", "R3 muestra tus vistas, con previa en la otra pantalla. Vista, filtro y orden quedan a mano en el estante; las opciones finas están agrupadas.", Icono.VISTA, 0xFFBCA4FF),
            Pagina("Retoma donde lo dejaste", "En Inicio eliges cuánto debe durar la pausa y puedes activar un clip opcional de los últimos 6 segundos, sin sonido. Android pide permiso. Con lupa o sin clip se usa la imagen del juego.", Icono.CONTINUAR, 0xFF57DCC7),
        )),
        Version("editor-widgets-1", "Edición de widgets", listOf(
            Pagina("Pasa widgets entre páginas", "En el editor, usa las flechas para cambiar de página o + para crear otra. Arrastra un widget hacia un borde y espera un instante para cruzar sin soltarlo.", Icono.APPS, 0xFF57DCC7),
            Pagina("Estira y mira el cambio", "El widget sigue tu dedo desde la manija de la esquina. Al soltar se ajusta a la grilla. Agrega Temporizador, Cronómetro o Buscar juegos desde el catálogo.", Icono.FORMA, 0xFFBCA4FF),
            Pagina("Avisos de actualización", "Díptico comprueba nuevas versiones al volver a la app y en segundo plano. Permite las notificaciones en Android para recibir el aviso.", Icono.CAMPANA, 0xFF73ABFF),
        )),
        Version("mesa-partida-1", "Tu partida, a tu manera", listOf(
            Pagina("Tu pantalla de partida", "Toca Portada, Bitácora, Herramientas o Ligera en la pantalla libre. La vista se recuerda para cada juego. Desde sus opciones también la puedes cambiar con el mando.", Icono.PANTALLAS, 0xFF57DCC7),
            Pagina("Un inicio para cada momento", "En Apariencia prueba Jugar, Explorar o Ligero. Puedes ajustar los widgets, guardar el perfil y volver a tu distribución anterior.", Icono.PALETA, 0xFFBCA4FF),
            Pagina("Planea tu próxima partida", "Agrega Mi próxima partida, Cola de juegos o Tengo 20 minutos al inicio. Desde las opciones de un juego puedes marcarlo pendiente, jugando o terminado.", Icono.CONTINUAR, 0xFFFFB46A),
            Pagina("Capturas y copias", "Desde las opciones del juego puedes abrir su galería, compartir una captura o usarla como mapa. En sistemas con RetroArch puedes guardar y exportar hasta tres copias .srm por juego.", Icono.PORTADA, 0xFFBCA4FF),
            Pagina("Trae tu biblioteca", "En Biblioteca puedes importar títulos y favoritos de un gamelist.xml de ES-DE. Revisa las coincidencias antes de aplicar los cambios.", Icono.BIBLIOTECA, 0xFF73ABFF),
        )),
        Version("centro-bisa-1", "Centro de Bisa", listOf(
            Pagina("Bisa sabe dónde estás", "Te acompaña con acciones para el juego activo y su sistema. Consulta tu próximo paso y revisa la biblioteca sin configurar un motor. Con un motor puedes pedir pistas.", Icono.BISA, 0xFF57DCC7),
            Pagina("Habla con un gesto", "En Inicio pulsa Start + L3 o toca a Bisa. Start + R3 abre las apps. Puedes cambiar ambos atajos en Controles. Mantén su cara para dictar. En el chat puedes usar X para escribir o L1 para hablar.", Icono.MICROFONO, 0xFFBCA4FF),
            Pagina("Bisa durante una partida", "Con una pantalla libre tienes el chat y sus accesos. Cuando el juego usa toda la consola, usa la burbuja y sus acciones rápidas si Android le dio permiso.", Icono.PANTALLAS, 0xFF73ABFF),
        )),
        Version("cristal-accesos-2", "Cristal y accesos rápidos", listOf(
            Pagina("Díptico Cristal", "Prueba el tema en Ajustes › Temas. La luz y el arte del fondo se difuminan detrás de la interfaz.", Icono.PALETA, 0xFFBCA4FF),
            Pagina("Agregado · Carpetas y cajas", "En Estante prueba Mosaico, Vitrina y las cajas Panorámica, Instantánea y Casete. Edita los widgets desde su propio inicio.", Icono.FORMA, 0xFFFFB46A),
            Pagina("Actualizado · Barra de arriba", "Buscar, Apps, Widgets y Bisa ahora se distinguen mejor. Ajustes tiene su propio botón y Start lo abre al soltarlo con los atajos predeterminados.", Icono.AJUSTES, 0xFFBCA4FF),
            Pagina("Retirado · Estudio visual e IA para temas", "Quitamos la entrada Estudio visual y la creación de temas con IA. Busca los perfiles en Apariencia y los temas de la comunidad en Temas.", Icono.PALETA, 0xFFFF8A83),
            Pagina("Corregido · Biblioteca", "Se filtran accesos de Android que ya no tienen una app disponible y se evitan duplicados entre Android, Ports y accesos importados.", Icono.BIBLIOTECA, 0xFF73ABFF),
            Pagina("Apps y atajos a tu gusto", "En dos pantallas, las opciones aparecen al elegir una app. En Controles puedes aprender un botón y combinarlo con Start.", Icono.MANDO, 0xFF73ABFF),
            Pagina("Bisa, tu copiloto", "Pídele una pista sin spoilers y cuéntale el punto en que estás. Si ya dejaste una nota, Bisa puede usarla.", Icono.BISA, 0xFF57DCC7),
        )),
        Version("cristal-editor-3", "Tienda y edición de widgets", listOf(
            Pagina("Agregado · Quitar al arrastrar", "Mueve el widget hasta la zona Quitar widget al pie de la pantalla. Cuando se destaque, suéltalo para quitarlo del inicio.", Icono.BORRAR, 0xFFDB7187),
            Pagina("Corregido · Edición en dos pantallas", "Arriba queda la cuadrícula y abajo el catálogo. Mantén una celda o pulsa Y en la cuadrícula para agregar en ese lugar.", Icono.EDITAR, 0xFF8F89DF),
            Pagina("Actualizado · Cristal y accesos", "Cristal usa tonos neutros, con desenfoque suave. Los accesos se alinean con el fondo del juego y con el encabezado de widgets.", Icono.PALETA, 0xFFA8B2D2),
            Pagina("Agregado · Tienda visible", "Abre Ajustes › Temas › Tienda de temas. Ya hay temas para previsualizar e instalar, y puedes importar más por enlace.", Icono.WEB, 0xFF77BDB8),
            Pagina("Corregido · Navegación por carpetas", "La cruceta y el joystick suben de fila en Carpetas, Mosaico y Vitrina. El editor de widgets ocupa el espacio completo, sin el marco del modo anterior.", Icono.MANDO, 0xFFA8B2D2),
            Pagina("Retirado · Habitación", "La vista Habitación sale de la biblioteca. Si la usabas, Díptico abre tus consolas en Carpetas.", Icono.CARPETA, 0xFFDBB386),
        )),
        Version("temas-web-4", "Temas fuera de Díptico", listOf(
            Pagina("Agregado · Catálogo web", "Hay 24 temas descargables fuera de Díptico. Cada uno explica su intención y muestra sus formas y texturas.", Icono.WEB, 0xFFBCA4FF),
            Pagina("Actualizado · Cinco temas incluidos", "Los demás estilos se instalan desde un JSON o enlace. Ajustes › Temas › Mis temas reúne tus descargas.", Icono.PALETA, 0xFF77BDB8),
            Pagina("Corregido · Accesos al elegir consola", "Entrar y Otra consola comparten la franja con Buscar, Apps y el resto de accesos. Así tienen espacio propio.", Icono.AJUSTES, 0xFFDBB386),
        )),
        Version("tienda-integrada-5", "Tienda integrada y biblioteca personal", listOf(
            Pagina("Tienda dentro de Díptico", "Explora e instala 24 temas en Ajustes › Temas › Tienda de temas. En dos pantallas, el catálogo se abre arriba y puedes recorrerlo desde abajo.", Icono.PALETA, 0xFFBCA4FF),
            Pagina("Combina tus temas", "En Mezclar mi tema elige de dónde vienen los colores, la tipografía, las formas, el fondo, la decoración, los efectos y la música.", Icono.EDITAR, 0xFF77BDB8),
            Pagina("Portadas y juegos que se lucen", "Pulsa X o mantén una carpeta para elegir su portada. Mientras recorres Carpetas, Mosaico o Vitrina, el widget Juego elegido muestra títulos de la carpeta enfocada.", Icono.CARPETA, 0xFFDBB386),
            Pagina("Navidad y Fiestas Patrias", "Adornos festivos deja elegir muérdago, campanas, chupallas o volantines. Los temas de las fechas especiales siguen su calendario.", Icono.ESTRELLA, 0xFFFFB46A),
        )),
        Version("carpetas-y-decoraciones-6", "Carpetas y decoraciones por tema", listOf(
            Pagina("Salir de Ocultos", "En la búsqueda de juegos ocultos toca Salir de Ocultos o pulsa B para recuperar tu vista anterior.", Icono.OCULTAR, 0xFF77BDB8),
            Pagina("Carpetas como íconos", "Cada consola abre sus juegos en una ventana con la cuadrícula desenfocada. Elige Pantalla completa en Estante › Abrir carpetas.", Icono.CARPETA, 0xFFDBB386),
            Pagina("Busca un ícono de carpeta", "Mantén un ícono o pulsa X. Puedes elegir arte de las fuentes conectadas o una imagen de tus archivos.", Icono.PORTADA, 0xFFBCA4FF),
            Pagina("Videojuegos en la tienda", "Hay seis temas nuevos con música y efectos propios. Pruébalos en la pestaña Videojuegos de la tienda.", Icono.MANDO, 0xFF77BDB8),
            Pagina("Decoraciones por tema", "Abre Ajustes › Decoraciones y despliega un tema para cambiar su adorno y su intensidad.", Icono.ESTRELLA, 0xFFFFB46A),
        )),
        Version("desenfoque-carpetas-7", "Ventanas de carpeta", listOf(
            Pagina("Carpetas con fondo desenfocado", "En la vista Carpetas, la ventana deja ver la cuadrícula desenfocada a través del panel y alrededor. Toca fuera o pulsa B para volver.", Icono.CARPETA, 0xFFDBB386),
        )),
        Version("zoom-y-recorrido-carpetas-8", "Carpetas a tu medida", listOf(
            Pagina("Cambia el tamaño con dos dedos", "Junta o separa dos dedos sobre la vista Carpetas. También puedes elegir un tamaño con el mando en Menú › Estante › Tamaño de carpetas.", Icono.FORMA, 0xFFDBB386),
            Pagina("Recorre las carpetas a tu manera", "En Menú › Estante › Recorrido de carpetas elige si quieres deslizar hacia los lados o hacia arriba y abajo.", Icono.CARPETA, 0xFF77BDB8),
        )),
        Version("rendimiento-y-una-pantalla-9", "Díptico en una pantalla", listOf(
            Pagina("Carpetas y accesos en una pantalla", "En consolas de una pantalla, abre Carpetas y usa la barra para buscar, pasar a Widgets, abrir Bisa o entrar a Ajustes. La cabecera te lleva de vuelta a tus consolas.", Icono.CARPETA, 0xFFDBB386),
            Pagina("Temas con vista previa", "Mira el tema o adorno que estás eligiendo y usa la acción de la galería para instalarlo o aplicarlo desde la misma pantalla.", Icono.PALETA, 0xFF77BDB8),
            Pagina("Biblioteca más ligera", "Díptico reutiliza la búsqueda y el orden mientras recorres tus juegos. El modo Ahorro conserva las decoraciones y efectos desactivados mientras está en uso.", Icono.CHISPA, 0xFFBCA4FF),
        )),
        Version("mantenimiento-y-estabilidad-10", "Mantenimiento y estabilidad", listOf(
            Pagina("Biblioteca más estable", "Si cambias las carpetas durante un escaneo, Díptico vuelve a leerlas con los ajustes nuevos. Si una carpeta no responde, conserva los juegos que ya conocía de ella.", Icono.CARPETA, 0xFFDBB386),
            Pagina("Guía actualizada", "El README de GitHub reúne la instalación y las funciones de Díptico en español e inglés, con enlaces a los temas de la comunidad y a las pruebas de la beta.", Icono.INFO, 0xFF77BDB8),
        )),
        Version("glass-y-densidades-153-11", "Glass y carpetas", listOf(
            Pagina("Liquid Glass", "Actívalo en Apariencia. Las carpetas usan el fondo real desenfocado, con bordes finos y luz suave. Regula por separado el nivel de carpetas y widgets.", Icono.ESTRELLA, 0xFF8ECFD4),
            Pagina("Carpetas ordenadas", "Los iconos propios de cada sistema tienen su nombre debajo. Ordena por nombre o por familias desde la biblioteca; tus portadas personales siguen disponibles.", Icono.CARPETA, 0xFFBCA4FF),
            Pagina("Editor de widgets más simple", "Tamaño, Diseño y Más ajustes conservan toda la personalización. Busca widgets en el catálogo. El contenido se adapta al espacio; toca ↗ para leer un texto largo completo.", Icono.EDITAR, 0xFF8ECFD4),
            Pagina("Espacios predeterminados", "Ajusta los juegos con dos dedos: Compacto, Equilibrado, Amplio o Detalle. Al soltar se ajustan a una parada fija y conservan tu vista y tu selección.", Icono.FORMA, 0xFFDBB386),
        )),
        Version("arreglos-1.2-12", "Arreglos importantes", listOf(
            Pagina("Tu partida sigue ahí", "Si eliges un juego que ya está abierto, o que dejaste en pausa hace poco, Díptico vuelve a tu partida en vez de empezarla de cero.", Icono.CONTINUAR, 0xFF2EC4A6),
            Pagina("Pantallas intercambiadas", "Con las pantallas intercambiadas, las herramientas de la partida aparecen siempre en la pantalla que deja libre el juego.", Icono.PANTALLAS, 0xFF3BA8FF),
            Pagina("Más estable", "Arreglos para que Díptico no se cierre con el clip de retoma, para que el aviso de cierre no vuelva siempre y para que los atajos del mando no se activen solos.", Icono.CHECK, 0xFF8E7CFF),
            Pagina("Respaldo completo y carpetas propias", "El respaldo ahora guarda tus copias de partidas y la música de los temas. Los íconos de las carpetas tienen dibujos propios, con más aire.", Icono.CARPETA, 0xFFFF7A59),
        )),
        Version("orden-y-base-1.2-13", "Orden y base", listOf(
            Pagina("Elige de una lista", "Las opciones con varias alternativas (fluidez, protector, orden, filtros, tamaños, Glass…) abren una lista para elegir directo, sin dar la vuelta completa. Las marcan con ▾.", Icono.MENU, 0xFF8E7CFF, "A · B"),
            Pagina("Tu barra, a tu manera", "La barra de accesos puede quedar fija o esconderse sola. Y las barras de cada pantalla pueden ser sólidas, difusas o transparentes, por separado. Menú › Inicio y Menú › Pantallas.", Icono.APPS, 0xFF3BA8FF),
            Pagina("Accesos a tu gusto", "Elige qué accesos aparecen en los ajustes rápidos y en qué orden: toca el lápiz al final de la cortina. A muestra u oculta; X sube un acceso.", Icono.EDITAR, 0xFF2EC4A6, "X"),
            Pagina("Vibración", "Un toque de vibración al moverte y elegir, suave, media o fuerte. Nunca vibra mientras juegas. Menú › Música › Vibración.", Icono.CHISPA, 0xFFFF7A59),
        )),
        Version("movimiento-y-vistas-1.3-15", "Movimiento y vistas", listOf(
            Pagina("Todo se mueve suave", "El menú, las ventanas y las listas llegan flotando y se posan. Elige Animaciones (apagadas, suaves, normales o intensas) y su velocidad en Menú › Apariencia.", Icono.CHISPA, 0xFF8E7CFF),
            Pagina("Vidrio con lente", "Con Liquid Glass, el vidrio se comporta como una lupa suave: curva lo de atrás en los bordes, brilla en el canto y una luz sigue tu dedo. Menú › Apariencia › Efecto lente.", Icono.BRILLO, 0xFF8ECFD4),
            Pagina("Dos vistas nuevas", "Solo portadas: una cuadrícula sin textos para ver muchos juegos. Cartuchos 3D: cada juego de pie en su cartucho o caja. Cámbialas con R3.", Icono.VISTA, 0xFFFFA726, "R3"),
            Pagina("Barra nueva", "La hora, el wifi, los avisos y la batería flotan en una isla en la esquina, cada uno con su color, y la pantalla queda entera. Menú › Pantallas › Forma de la barra.", Icono.ARRIBA, 0xFF60A5FA),
            Pagina("Interruptores vivos", "Los interruptores se estiran como una gota al cambiar, y el de modo claro u oscuro es un pequeño cielo: sale el sol o la luna, y los colores de toda la app se funden suave.", Icono.TEMA, 0xFFFFD54A),
            Pagina("Galería de artes y carpetas que se abren", "Menú › Estante › Galería de artes: mira los artes y portadas de la carpeta, en grande en la otra pantalla. Y las carpetas ahora se abren con un resorte suave.", Icono.PORTADA, 0xFFFF7A9C),
            Pagina("Dock de apps", "Activa el dock en Menú › Inicio: una manija abajo despliega tus apps fijadas y el cajón con todas. Eliges el lado, el tamaño y cuántas apps.", Icono.APPS, 0xFF2EC4A6),
        )),
        Version("mosaicos-2.0-16", "Nuevo Díptico: mosaicos", listOf(
            Pagina("Mosaicos, estilo consola", "Tus juegos ahora se ven como en una consola: el primero en un mosaico grande y el resto en cuadrados. El elegido brilla con el color de su consola. Cambia de vista con R3.", Icono.VISTA, 0xFF8B7CF6, "R3"),
        )),
        Version("franja-2.0-18", "Nuevo Díptico: la pantalla de abajo", listOf(
            Pagina("Todo a mano abajo", "Abajo tienes una franja fija con Buscar, Apps y Ajustes, y la hora con la batería. La consola elegida se ilumina con su color.", Icono.APPS, 0xFF2EC4A6),
        )),
        Version("vitrina-ajustes-2.0-19", "Nuevo Díptico: vitrina y ajustes", listOf(
            Pagina("La vitrina", "Arriba, el juego elegido luce a pantalla completa, sin barras encima, con dos botones: A para jugar o continuar y X para su ficha.", Icono.PORTADA, 0xFFFF7A59, "A"),
            Pagina("Ajustes más simples", "Los ajustes ahora son seis secciones: Pantalla y estilo, Biblioteca, Juegos y emuladores, Controles, Sonido y Sistema. Lo de antes sigue adentro, ordenado por bloques.", Icono.AJUSTES, 0xFF8E8AAE),
        )),
        Version("ajustes-orden-2.0-20", "Nuevo Díptico: ajustes en orden", listOf(
            Pagina("Jugar sin preguntas", "Abrir un juego ya no pide grabar la pantalla. El clip para retomar es opcional y viene apagado.", Icono.MANDO, 0xFF4ADE80),
            Pagina("Temas, todo en la tienda", "Importar un tema desde un archivo o un enlace, y aportar uno, ahora están arriba en la Tienda de temas.", Icono.PALETA, 0xFFB388FF),
            Pagina("Inicio y widgets aparte", "Los widgets y el inicio tienen su propia sección en Ajustes. Y los íconos de las consolas se eligen de una lista con una muestra de cada estilo.", Icono.APPS, 0xFF00ACC1),
        )),
        Version("temas-aparte-2.0-21", "Temas aparte y movimiento pulido", listOf(
            Pagina("Temas y decoraciones aparte", "Los temas y los adornos tienen su propia sección en Ajustes. Y al cambiar de consola, los mosaicos se deslizan hacia ese lado.", Icono.PALETA, 0xFFB388FF),
        )),
        Version("dippo-2.0-22", "Llegó DIPPO", listOf(
            Pagina("¡Hola, soy DIPPO!", "Bisa ahora es DIPPO, un pulpito de luz que vive en tu consola. Curioso, servicial y un poquito despistado: conoce tus juegos y celebra tus logros contigo.", Icono.BISA, 0xFFFF9A6B),
            Pagina("De día y de noche", "Las puntas de sus tentáculos son lucecitas que respiran. De día es cálido; de noche se pone azulado. Y cada tanto te saluda.", Icono.ESTRELLA, 0xFF7AA8FF),
        )),
        Version("dippo-casa-2.0-23", "DIPPO se queda contigo", listOf(
            Pagina("DIPPO te espera en el inicio", "Ahora DIPPO vive a la vista: abajo junto a la hora, o en la esquina si usas una pantalla. Te saluda al llegar y pone cara de preocupado si queda poca batería. Tócalo para hablarle.", Icono.BISA, 0xFFFF9A6B),
            Pagina("Su propio espacio", "DIPPO tiene su sección en Ajustes: si aparece en el inicio, su saludo, sus colores, su IA y cómo te ayuda mientras juegas. Y ahora se asoma en la animación de entrada.", Icono.AJUSTES, 0xFF7AA8FF),
        )),
        Version("dippo-listo-2.0-24", "DIPPO más listo y teclado nuevo", listOf(
            Pagina("DIPPO habla primero", "Al abrirlo te cuenta qué notó: lo último que jugaste, lo que anotaste y algún juego olvidado. Todo con botones listos: casi no tendrás que escribir.", Icono.BISA, 0xFFFF9A6B),
            Pagina("Preguntas con un toque", "¿De qué trata?, trucos, cuánto dura, juegos parecidos… Y después de cada respuesta, DIPPO te deja la siguiente pregunta lista. Si le hablas, te responde con su voz.", Icono.MICROFONO, 0xFF7AA8FF),
            Pagina("Teclado nuevo", "Como el de Google: palabras sugeridas arriba, tildes manteniendo la letra (o con Y), dos espacios ponen punto y ⋯ trae copiar y pegar.", Icono.TEXTO, 0xFF00ACC1),
        )),
        Version("logros-dippo-2.0-25", "Logros de DIPPO y cuánto te queda", listOf(
            Pagina("Los logros de DIPPO", "DIPPO tiene sus propios logros: por jugar, por terminar juegos, por rachas de días y por conversar con él. Cuando ganas uno, lo celebra. Míralos en Ajustes › DIPPO.", Icono.TROFEO, 0xFFFFC94D),
            Pagina("¿Cuánto te queda?", "Díptico consulta HowLongToBeat y te dice cuánto dura cada juego y cuánto te falta según lo que llevas. Pregúntaselo también a DIPPO con un toque.", Icono.RELOJ, 0xFF7AA8FF),
            Pagina("Más ordenado", "Quedaron las cuatro vistas de juegos que más se usan, la barra de la hora es una sola y «Pantalla y estilo» tiene solo lo esencial.", Icono.AJUSTES, 0xFF00ACC1),
        )),
        Version("deslizar-dock-2.0-26", "Desliza a tus widgets y dock de apps", listOf(
            Pagina("Desliza a tus widgets", "Tus widgets están al lado de la portada: desliza la pantalla de arriba con el dedo y pasas de una a otra. Los puntitos de abajo te dicen dónde estás.", Icono.APPS, 0xFF00ACC1),
            Pagina("Tu dock de apps", "Hasta 5 apps fijas en la pantalla de abajo, también mientras juegas: se abren en esa pantalla y el juego sigue arriba. Fíjalas en el cajón con Y.", Icono.ESTRELLA, 0xFFFF9A6B),
            Pagina("Widget «Tu progreso»", "Cuánto te queda en tu juego, tu racha de días y el próximo logro de DIPPO, todo junto. Prueba el inicio nuevo en Ajustes › Inicio y widgets.", Icono.TROFEO, 0xFFFFC94D),
        )),
        Version("jugando-2.0-27", "Mientras juegas, más simple", listOf(
            Pagina("Mientras juegas, más simple", "La pantalla libre quedó ordenada: DIPPO con un consejo, tu próximo paso con cuánto te queda, la música, tus apps y cuatro botones: Pista, Mi paso, Guías y Más.", Icono.MANDO, 0xFFFF9A6B),
            Pagina("Sin botón «Terminé»", "Ya no hace falta: cuando sales del juego y vuelves a Díptico, la partida se cierra sola y se guarda tu tiempo.", Icono.CHECK, 0xFF7AA8FF),
        )),
        Version("ficha-carpetas-2.0-28", "Ficha viva y carpetas claras", listOf(
            Pagina("Ficha sin repetir", "Abajo lees y eliges; arriba pasan tus capturas y fotos del juego, y DIPPO te cuenta datos tuyos.", Icono.PORTADA, 0xFFFF9A6B),
            Pagina("Juegos en una carpeta", "Cuando DIPPO te propone juegos, vienen en una carpetita: tócala, se abre y juegas al tiro.", Icono.BISA, 0xFF7AA8FF),
        )),
        Version("cajas-carga-2.0-29", "Cajas y pantalla de carga", listOf(
            Pagina("Pantalla de carga", "Al abrir un juego, todo se tiñe del color de su consola, entra su caja y se funde a negro antes de empezar. B la cancela. Se apaga en Ajustes › Pantalla y estilo.", Icono.JUGAR, 0xFFE60012),
            Pagina("Vitrina de cajas", "Arriba, la caja del juego elegido luce al centro con las de al lado asomándose inclinadas. Toca una caja de al lado para elegirla, o la del centro para jugar.", Icono.PORTADA, 0xFFFF9A6B),
        )),
        Version("estuches-discord-2.0-30", "Estuches, Discord y tu logo al jugar", listOf(
            Pagina("Cada consola, su estuche", "Las cajas de la vitrina y de la carga ahora parecen de su consola: el cartón con rejilla de los 8 bits, la franja de 16 bits, el estuche de CD con bisagra, el DVD, la tarjeta, el UMD y la marquesina de arcade.", Icono.PORTADA, 0xFFFF9A6B),
            Pagina("Discord", "Conecta un canal de tu servidor en Ajustes › Juegos › Discord: Díptico publica qué juegas, con la portada, y cuánto jugaste al terminar.", Icono.MUNDO, 0xFF5865F2),
            Pagina("Tu logo al jugar", "Mientras juegas, el logo del juego luce en grande abajo y lo que te queda cabe en una línea. En la franja, las apps se esconden con su botoncito.", Icono.MANDO, 0xFF7AA8FF),
        )),
        Version("cajas-3d-2.0-31", "Cajas en 3D y tus cuentas", listOf(
            Pagina("Cajas en 3D", "La caja del juego flota en la vitrina y la puedes girar con el dedo: se le ve el lomo y el brillo del plástico. Al soltarla vuelve sola. El estuche ahora va alrededor de la portada, sin taparla.", Icono.PORTADA, 0xFFFF9A6B),
            Pagina("Inicia sesión con Discord", "En Ajustes › Cuentas y conexiones: aparece un código QR, lo escaneas con tu teléfono y apruebas. Sin escribir contraseñas en la consola.", Icono.MUNDO, 0xFF5865F2),
            Pagina("Tus cuentas, juntas", "Discord, RetroAchievements, SteamGridDB, ScreenScraper y las demás fuentes ahora viven en una sola sección: Cuentas y conexiones. Y las decoraciones del tema se ven en toda la consola, siempre.", Icono.CLAVE, 0xFF7AA8FF),
        )),
        Version("cajas-reales-2.0-32", "Cajas de verdad y más rapidez", listOf(
            Pagina("Cajas de verdad", "Cada caja tiene frente, lomo y reverso, con su grosor real. Gírala completa con el dedo, el stick derecho o el botón Girar: atrás está la historia del juego, tu tiempo y cuánto dura. Con ScreenScraper o TheGamesDB conectados, trae el reverso original.", Icono.PORTADA, 0xFFFF9A6B),
            Pagina("Del estante al juego", "Las cajas de los costados muestran su lomo, como en un estante. Toca la del centro (o A) y se abre la pantalla de carga con la misma caja, y luego el juego.", Icono.JUGAR, 0xFF6B4EFF),
            Pagina("Más rápido", "Menos trabajo en cada cuadro: los difuminados, el zoom de las portadas y el logo al jugar ya no rearman la pantalla entera. Mientras juegas, las decoraciones se quedan quietas para no quitarle fluidez al juego.", Icono.CHISPA, 0xFF3BD18A),
        )),
        Version("cajas-exactas-2.0-33", "Cajas exactas y estante en línea", listOf(
            Pagina("Cajas exactas", "Con TheGamesDB o ScreenScraper conectados, el frente y el reverso son el escaneo de la caja original. En DS y 3DS la franja del logo va al costado, como en sus cajas.", Icono.PORTADA, 0xFFFF9A6B),
            Pagina("Estante en línea", "Con la vitrina de cajas, abajo los juegos van en una sola fila en el mismo orden que arriba: avanzas uno a uno, sin saltos. El fondo es la portada desenfocada de verdad.", Icono.JUGAR, 0xFF6B4EFF),
            Pagina("Más espacio en las opciones", "En las opciones del juego y en su ficha, todo se desliza junto: lo de arriba se va al bajar y las opciones usan toda la ventana.", Icono.MENU, 0xFF3BD18A),
        )),
        Version("discord-amigos-2.0-34", "Discord con tus amigos", listOf(
            Pagina("Conéctate en un toque", "Toca el botón de Discord de la barra: escanea el código con tu teléfono, o ábrelo en esta consola y toca «Autorizar». Sin claves ni contraseñas.", Icono.MUNDO, 0xFF5865F2),
            Pagina("Tus amigos al lado", "El botón de Discord, arriba y abajo, muestra si estás conectado y cuántos amigos juegan. Tócalo y se desliza una ventana con quién está conectado y quién juega en Díptico.", Icono.CORAZON, 0xFF23A55A),
            Pagina("Jugando a…", "Al abrir un juego, tu perfil de Discord muestra cuál es, en qué consola y cuánto llevas. Se apaga en Ajustes › Cuentas y conexiones.", Icono.JUGAR, 0xFFFF9A6B),
        )),
        Version("perfil-2.0-35", "Tu perfil", listOf(
            Pagina("Tu perfil", "Toca tu foto, arriba a la derecha (o abajo, junto a la hora): se abre tu tarjeta con tus horas, tu racha, tus logros de DIPPO y tus juegos favoritos. Crece desde tu foto sobre el fondo difuminado.", Icono.PERFIL, 0xFFFF7A59),
            Pagina("Foto o personaje", "Pon una foto de tu consola o elige uno de ocho personajes de Díptico: Misu, Robotín, Moquito, Dragoncito, Fantasmita, Caballerito, Zorrito y Pixelito. Todos son originales.", Icono.BISA, 0xFFE64FA0),
            Pagina("Tus datos", "En Ajustes › Mi perfil: tu nombre, un apodo, tu fecha de nacimiento (DIPPO te felicita ese día), tu color y tu consola favorita. Si quieres, puedes ocultar tus horas.", Icono.EDITAR, 0xFF23C486),
        )),
        Version("perfil-mando-2.0-36", "Perfil con el mando", listOf(
            Pagina("Perfil con el mando", "Mantén Start y aprieta A en el inicio: se abre tu perfil. Con el mismo gesto se cierra, y B también.", Icono.PERFIL, 0xFFFF7A59),
            Pagina("Tu foto en el inicio", "Tu foto también está junto a la hora en el inicio con widgets, para abrir tu perfil con un toque.", Icono.BISA, 0xFFE64FA0),
        )),
        Version("marcos-dippo-2.0-37", "Marcos, Discord y DIPPO", listOf(
            Pagina("Un marco que se mueve", "Tu foto lleva un aro de luz en movimiento: una estela que da vueltas sin parar. Se cambia en Ajustes › Mi perfil › Marco de la foto. Si apagas los efectos, queda quieto.", Icono.ESTRELLA, 0xFF9B6BFF),
            Pagina("Marcos que se ganan", "Con los logros de DIPPO se desbloquean marcos nuevos: chispas, latido, llama, noche estrellada, aurora, arcoíris y oro. Ahora piden logros difíciles (250 o 500 horas, 25 finales, 14 días seguidos…). Los que faltan se ven con un candado y te dicen qué logro piden.", Icono.TROFEO, 0xFFFFC94D),
            Pagina("Títulos y más logros", "Hay 12 logros difíciles nuevos y títulos que se muestran bajo tu nombre en la tarjeta: Maratonista, Coleccionista, Constante, Leyenda y más. Se eligen en Mi perfil › Título.", Icono.TROFEO, 0xFFFFC94D),
            Pagina("Tu foto de Discord", "Con Discord conectado puedes usar su foto como tu foto de perfil (Mi perfil › Usar la foto de Discord). Ahora es un poco más grande y se ve en el inicio, en las portadas y abajo.", Icono.MUNDO, 0xFF5865F2),
            Pagina("Discord en tu foto", "Una insignia de Discord en la esquina de tu foto abre la ventana de amigos, con atajos a tu perfil y a los ajustes. Todos los ajustes de Discord ahora están en Mi perfil.", Icono.MUNDO, 0xFF5865F2),
            Pagina("Logros en tu tarjeta", "Tu tarjeta muestra tus logros de DIPPO (con los últimos trofeos) y los de RetroAchievements (usuario, puntos e insignias recientes).", Icono.TROFEO, 0xFFFF7A59),
            Pagina("DIPPO sobre su cabeza", "Los mensajes de DIPPO salen como globitos justo arriba de su cabeza. Deslízalos hacia arriba o a un lado y se van, sin molestar la vista.", Icono.BISA, 0xFFFF7A59),
            Pagina("Barra de abajo deslizable", "La franja de accesos de abajo se desliza hacia el lado cuando hay muchas apps. Tu foto, DIPPO, la hora y la batería quedan fijos a la derecha.", Icono.APPS, 0xFF38BDF8),
        )),
        Version("primera-vez-2.0-38", "Empezar es más fácil", listOf(
            Pagina("Primera vez más corta", "La configuración ahora pide solo lo justo: pantallas, tus juegos, emuladores si faltan y el estilo. Las portadas se bajan solas.", Icono.CHECK, 0xFF2EC4A6),
            Pagina("Recorrido en 5 pasos", "El recorrido de bienvenida ahora es corto y al día: dos pantallas, jugar, DIPPO, tu perfil y Ajustes.", Icono.CHISPA, 0xFF6B4EFF),
            Pagina("Sin cierres de sorpresa", "Al elegir Díptico como inicio, te avisa antes que Android lo reinicia un segundo, y al volver te confirma que quedó listo.", Icono.APPS, 0xFF3BA8FF),
            Pagina("Directo a tus juegos", "Al abrir Díptico vas directo al inicio. El recordatorio de tu partida sale solo si dejaste un juego en pausa.", Icono.JUGAR, 0xFFFF7A59),
            Pagina("Tema Díptico por defecto", "Ahora parte con el tema de Díptico. Los temas de fechas especiales se activan en Temas si los quieres.", Icono.PALETA, 0xFF9B6BFF),
            Pagina("Fondo difuminado", "Toda ventana que se abre (opciones, galerías, ficha, búsqueda, amigos) deja el fondo difuminado detrás.", Icono.FORMA, 0xFF38BDF8),
        )),
        Version("dippo-guia-2.0-39", "DIPPO te guía", listOf(
            Pagina("DIPPO te muestra Díptico", "El recorrido de bienvenida ahora lo cuenta DIPPO, grande y al centro, con un globo que se escribe letra a letra. Puedes verlo de nuevo en Ajustes.", Icono.BISA, 0xFFFF7A59),
            Pagina("Trucos escondidos", "De vez en cuando DIPPO te cuenta en su globito una función que quizás no conocías. Se apaga en Ajustes › DIPPO › Trucos y datos escondidos.", Icono.CHISPA, 0xFF9B6BFF),
        )),
        Version("dippo-vivo-2.0-40", "DIPPO se mueve", listOf(
            Pagina("DIPPO cobra vida", "En el recorrido DIPPO entra saltando, mira hacia lo que explica, flota y da un saltito en cada paso. Tócalo y gira de sorpresa.", Icono.BISA, 0xFFFF7A59),
            Pagina("Pruébalo con DIPPO", "Algunos pasos te piden probar: aprieta L1 o R1, o toca a DIPPO. Él celebra y sigue solo. Si aprietas otros botones, te cuenta para qué sirven.", Icono.MANDO, 0xFF2EC4A6),
        )),
        Version("temperatura-2.0-41", "Temperatura y consejos", listOf(
            Pagina("Cuánto se calienta", "Mientras juegas, la pantalla compañera muestra la temperatura de la consola: verde si está fresca, amarilla si está tibia y roja si se calienta mucho.", Icono.BATERIA, 0xFFFF7A59),
            Pagina("Consejos de DIPPO", "DIPPO te avisa si se calienta o queda poca batería, y entre partidas te recuerda cuidar tus ojos y tus manos, y tomar agua. Se apaga en Ajustes › DIPPO.", Icono.BISA, 0xFF2EC4A6),
            Pagina("DIPPO conoce tus juegos", "Si un juego suele calentarla, DIPPO te avisa al empezar, y con emuladores pesados te dice qué bajar. Si la consola ya viene caliente, te avisa en el inicio.", Icono.MANDO, 0xFFFFC93C),
        )),
        Version("tu-ano-2.0-42", "Tu año", listOf(
            Pagina("Tu año con Díptico", "Un resumen de lo que jugaste: horas, días, tus juegos más jugados, tu mejor mes y tu racha, con un comentario de DIPPO. Ábrelo desde tu perfil (Y) o en Ajustes › Tu perfil. En diciembre te aviso solo.", Icono.ESTRELLA, 0xFFFFC93C),
            Pagina("Logros en plena partida", "Si ganas un logro de DIPPO mientras juegas, se celebra en la pantalla de abajo en el momento, sin esperar a que termines.", Icono.TROFEO, 0xFFFF7A59),
            Pagina("Desde ahora, cada partida cuenta", "Díptico empezó a anotar tus partidas con su fecha, para que «Tu año» sea cada vez más completo. Se guarda solo en tu consola.", Icono.RELOJ, 0xFF2EC4A6),
        )),
        Version("tu-ano-2.0-43", "Tu año completo", listOf(
            Pagina("«Tu año» se ve completo", "En la pantalla ancha, tus números van a la izquierda y el mes a mes con tus cinco juegos más jugados a la derecha, todo a la vista sin deslizar. Y los plurales salen bien («1 partida»).", Icono.ESTRELLA, 0xFFFFC93C),
        )),
        Version("tu-ano-2.0-44", "Tu año, bien ajustado", listOf(
            Pagina("Todo a la vista", "«Tu año» ahora cabe entero en la pantalla de arriba: el botón de cerrar pasó a la esquina y las tarjetas son más compactas.", Icono.ESTRELLA, 0xFFFFC93C),
        )),
        Version("biblioteca-ordenada-2.0-45", "Biblioteca más ordenada", listOf(
            Pagina("Sin «Todos los juegos»", "La biblioteca muestra una carpeta por consola y nada más. Si quieres ver todos tus juegos juntos, actívala en Ajustes (búscala por su nombre: Carpeta «Todos los juegos»).", Icono.CARPETA, 0xFF3BA8FF),
            Pagina("Controles en la cabecera", "La barrita de abajo con los botones desapareció: ahora esa guía está arriba, entre «Biblioteca · sistemas» y «Ordenar».", Icono.MANDO, 0xFFFFC93C),
            Pagina("Tu perfil se edita ahí mismo", "En tu tarjeta de perfil, A abre tus datos dentro de la misma ventana: nombre, foto, personaje, marco y más. Ya no te manda a Ajustes.", Icono.PERFIL, 0xFFFF7A59),
        )),
        Version("mas-rapido-2.0-46", "Más rápido", listOf(
            Pagina("Ajustes y transiciones más ágiles", "Moverte por los ajustes ya no vuelve a armar toda la lista en cada paso, y las animaciones son más cortas.", Icono.CHISPA, 0xFF2EC4A6),
        )),
        Version("esquinas-2.0-47", "Esquinas ordenadas", listOf(
            Pagina("Barra de abajo más limpia", "Abajo: la lupa en una esquina, los ajustes en la otra y tus apps fijas al centro (se esconden con el botoncito). En la pantalla de arriba: DIPPO en una esquina y tu perfil en la otra. Se fue la guía de botones.", Icono.APPS, 0xFF3BA8FF),
        )),
        Version("esquinas-2.0-48", "DIPPO y tu foto, abajo", listOf(
            Pagina("Arriba de la pantalla de abajo", "Donde decía «Biblioteca» y «Ordenar» ahora están DIPPO, la hora y la batería, y tu foto de perfil. Tus apps fijas van en una píldora con los colores de tu tema.", Icono.PERFIL, 0xFFFF7A59),
        )),
        Version("franja-nueva-2.0-49", "Barra de apps nueva", listOf(
            Pagina("Todo combina", "La barra de abajo ya no es una franja: la lupa y los ajustes son círculos y tus apps van en una píldora, todo con la superficie de tu tema. Arriba, DIPPO más grande en su esquina y en la otra tu foto, la hora y la batería.", Icono.APPS, 0xFF3BA8FF),
        )),
        Version("franja-ajuste-2.0-50", "Barra de abajo a tu gusto", listOf(
            Pagina("Fija o apagada", "La barra de abajo queda siempre a mano. Si prefieres más espacio, apágala en Ajustes buscando «Barra de abajo».", Icono.APPS, 0xFF2EC4A6),
        )),
        Version("franja-burbuja-2.0-51", "Barra con burbuja", listOf(
            Pagina("Sin marcos y escondida abajo", "Los íconos de tus apps van sin casillas ni marcos. La barra se esconde hacia abajo y deja solo una barrita: tócala y la barra sale como una burbuja (y con otro toque vuelve a esconderse).", Icono.APPS, 0xFF3BA8FF),
        )),
        Version("editar-perfil-2.0-52", "Editar tu perfil", listOf(
            Pagina("Tu perfil, en dos pantallas", "Toca tu foto y elige «Editar perfil»: abajo cambias tu foto, tu personaje, tus datos, tu color y lo que muestras; arriba ves tu tarjeta tal como va quedando. Nada se guarda hasta que aprietas A · Guardar (o Start). B cancela.", Icono.PERFIL, 0xFFFF7A59, "A · Guardar"),
            Pagina("Tu inicial como foto", "Si no quieres foto ni personaje, elige «Mi inicial»: tu perfil muestra la primera letra de tu nombre sobre tu color.", Icono.TEXTO, 0xFF9B6BFF),
        )),
        Version("partidas-a-salvo-2.0-53", "Partidas a salvo", listOf(
            Pagina("Tus partidas, respaldadas solas", "En Ajustes › Sistema › Respaldo de partidas elige la carpeta donde tu emulador guarda (por ejemplo RetroArch/saves). Al terminar de jugar, Díptico copia lo que cambió: guarda las últimas 3 por juego y la más nueva viaja en tu respaldo de Drive. Para volver a una, abre las opciones del juego › Copias de partida.", Icono.COPIAR, 0xFF23C486),
            Pagina("¿Va fluido?", "Activa el Medidor de fluidez en Ajustes: cada pantalla muestra sus cuadros por segundo y el cuadro más lento. Verde es fluido; amarillo o rojo te dicen dónde se pone lento.", Icono.CHISPA, 0xFFFFC94D),
            Pagina("Alto contraste", "Junto al tamaño de letra hay una opción de alto contraste: las letras chicas y los bordes se marcan más para leer mejor.", Icono.TEMA, 0xFF38BDF8),
            Pagina("Editor con fondo difuminado", "El editor del perfil deja ver tu inicio difuminado detrás, como todas las ventanas.", Icono.PERFIL, 0xFFFF7A59),
            Pagina("Ordena tus carpetas", "Mantén presionada una carpeta (con el dedo o con A): todas tiemblan y puedes arrastrarlas a su nuevo lugar, o tomarlas con A y moverlas con la cruceta. B o «Listo» termina.", Icono.CARPETA, 0xFFE64FA0, "Mantén A"),
            Pagina("Carpetas con estilo", "En Ajustes › Biblioteca › Estilo de las carpetas elige cómo se ven: Tarjeta, Consola, Abanico de portadas o Logo grande.", Icono.CARPETA, 0xFF9B6BFF),
            Pagina("Ajustes más ordenados", "Las secciones largas muestran lo principal; el resto está en «Más opciones…», que se abre en una ventana. Arriba sigue la explicación con su vista previa de cada opción.", Icono.MENU, 0xFF5865F2),
        )),
        Version("fluido-flotante-2.0-54", "Más fluido y flotante", listOf(
            Pagina("Barra de abajo flotante", "La barra de abajo ya no tiene fondo: tus apps flotan con una sombra suave y la lupa y los ajustes son burbujas casi transparentes. Si la escondes con la barrita, queda solo esa píldora.", Icono.APPS, 0xFF3BA8FF),
            Pagina("Más fluido", "La vitrina de cajas mueve solo la del centro, los mosaicos ya no se redibujan enteros en cada paso y las carpetas tiemblan sin trabar. Prende el Medidor de fluidez para comparar.", Icono.CHISPA, 0xFFFFC94D),
            Pagina("Tu título junto a tu foto", "El título que ganas con los logros de DIPPO aparece en una burbuja chica al lado de tu foto, abajo.", Icono.PERFIL, 0xFFFF7A59),
        )),
        Version("switch2-ports-2.0-55", "Switch 2 y ports nativos", listOf(
            Pagina("Switch 2 con NovaS", "Díptico ya abre tus juegos de Switch 2 con NovaS (beta), también los archivos .nup. Si lo tienes instalado, queda listo solo.", Icono.MANDO, 0xFF00C3E3),
            Pagina("Ports nativos de Switch 2", "Mario Kart World, Donkey Kong Bananza, Splatoon Raiders, Pokémon Pokopia y Zelda: Ocarina of Time se abren solos con Banana Native como port nativo, mucho más fluidos. Los demás siguen con NovaS.", Icono.CHISPA, 0xFFFFC94D),
            Pagina("Un centro propio", "En Ajustes › Biblioteca › «Switch 2 y ports nativos» (o desde la carpeta de Switch 2 o de Ports): abajo eliges, arriba ves el detalle. Revisa si tu consola aguanta, elige NovaS y consigue ports como Ship of Harkinian, Ghostship o Dusklight. Los que instales aparecen solos en tu carpeta Ports.", Icono.APPS, 0xFF23C486, "L1 · R1"),
            Pagina("Barra flotante de verdad", "La barra de abajo ya no ocupa espacio: tus carpetas corren por detrás y los accesos flotan sin círculos. Escondida, queda solo la píldora.", Icono.APPS, 0xFF3BA8FF),
            Pagina("DIPPO no interrumpe", "Con Ajustes o cualquier ventana abierta, DIPPO guarda sus globitos para después.", Icono.PERFIL, 0xFFFF7A59),
        )),
        Version("temas-propios-2.0-56", "Temas propios", listOf(
            Pagina("Calendario de temas", "En Ajustes › Temas › pestaña Calendario están todas las fiestas, de la más cercana a la más lejana, y cada una es un tema completo: su fondo, su trama, sus botones y su decoración. Con «Fechas especiales» se pone sola el día que toca; o úsala cuando quieras.", Icono.MUNDO, 0xFFFFC93C),
            Pagina("Más fechas de Chile", "Se suman el Día de la Mamá, el Día del Papá, el Día del Profesor, el Día del Libro y el Día del Orgullo Friki.", Icono.ESTRELLA, 0xFFFF5D8F),
            Pagina("Temas exclusivos", "Ocho temas que no se venden ni se importan: Aurora austral, Eclipse, Turbo, Bosque profundo, Leyenda, Créditos finales, Cartógrafo y Museo de consolas. Cada uno se gana con un logro difícil de DIPPO; antes se ven con candado.", Icono.TROFEO, 0xFF8B5CF6),
        )),
        Version("por-que-fallo-2.0-57", "Por qué falló un juego", listOf(
            Pagina("Si un juego no abre, DIPPO te dice por qué", "Si el emulador se cierra a los pocos segundos, Díptico no lo cuenta como partida y te muestra lo más probable: falta la BIOS, faltan las llaves de Switch, el archivo está comprimido o incompleto, o el emulador no tiene permiso para tu carpeta.", Icono.EMULADOR, 0xFFFF7A59),
            Pagina("Probar con otro emulador", "Desde el mismo aviso, un toque prueba el juego con el siguiente emulador que tengas instalado, y queda guardado solo para ese juego.", Icono.MANDO, 0xFF23C486, "A"),
        )),
        Version("emuladores-al-dia-2.0-58", "Emuladores al día", listOf(
            Pagina("DIPPO revisa tus emuladores", "Una vez al día, DIPPO mira qué emuladores tienes, cuáles usas y cuánto, y busca si hay versión nueva en su página. Si hay, te avisa.", Icono.EMULADOR, 0xFF00C3E3),
            Pagina("Actualiza desde Díptico", "En Ajustes › Juegos y emuladores › «Emuladores al día» (o en el centro de Switch 2 y ports, pestaña Emuladores): A baja la versión nueva y Android la instala encima, como Obtainium. Los ports nativos también se actualizan.", Icono.DESCARGA, 0xFF23C486, "A"),
            Pagina("Tu perfil, en grande arriba", "Tu foto, tu nombre y tu título se ven grandes en la pantalla de arriba, como en iiSU. Tu perfil se edita desde tu tarjeta (ya no está en Ajustes) y Discord quedó en «Cuentas y conexiones».", Icono.PERFIL, 0xFFFF7A59),
            Pagina("Fan games con Gahvoria", "Los fan games de RPG Maker y Pokémon Essentials ahora se abren con Gahvoria (Essentia ya no se usa). DIPPO también revisa si Gahvoria tiene versión nueva.", Icono.EMULADOR, 0xFFE0452B),
            Pagina("Los premios de los logros", "En Logros de DIPPO cada logro dice qué regala, y la pestaña Premios junta todos los marcos, títulos y temas: los ganados se usan con un toque.", Icono.TROFEO, 0xFFFFC94D),
        )),
        Version("hora-iconos-2.0-59", "Carpetas y avisos", listOf(
            Pagina("Hora y batería, bien puestas", "Arriba, junto a tu foto, la hora y la batería van en una píldora oscura y chica, alineada con tu nombre. Tocarla cambia entre 12 y 24 horas. No ocupa más pantalla que antes.", Icono.PERFIL, 0xFF3BA8FF),
            Pagina("Juegos al medio", "En la pantalla de abajo la fila de juegos queda justo en el centro de lo que se ve, sin contar la barra flotante.", Icono.VISTA, 0xFF23C486),
            Pagina("Íconos pixel", "Switch, Switch 2 y Gahvoria tienen su ícono pixel propio en la vista de íconos.", Icono.EMULADOR, 0xFFFF7A59),
            Pagina("Mueve carpetas por toda la pantalla", "Al ordenar carpetas, arrastra una hacia el borde de arriba o de abajo y la vista se desliza sola hasta que la dejes donde quieras. Se suelta en la carpeta que quede debajo del dedo.", Icono.CARPETA, 0xFFFFC94D, "Arrastra"),
            Pagina("DIPPO te avisa en la campana", "La temperatura alta, las actualizaciones de Díptico y de tus emuladores aparecen también en la campana de arriba, con la carita de DIPPO. Se apaga en Ajustes › «Avisos de DIPPO en la campana».", Icono.CAMPANA, 0xFF8B5CF6),
            Pagina("Carpetas en modo oscuro", "Nueva opción «Tono de las carpetas»: Según el tema, Oscuras o Claras. Con un tema oscuro las carpetas pasan a fondo profundo con el dibujo en claro.", Icono.CARPETA, 0xFF2EC4A6),
        )),
        Version("dock-arriba-2.0-60", "Apps arriba", listOf(
            Pagina("Tus apps, arriba", "Las apps que fijaste ahora van arriba al medio de la pantalla de abajo, entre DIPPO y tu foto. Ya no tapan los juegos.", Icono.APPS, 0xFF3BA8FF),
            Pagina("Lupa y ajustes con fondo", "La lupa y los ajustes siguen en las esquinas de abajo, ahora sobre una pestaña semiredonda para que se noten.", Icono.AJUSTES, 0xFF8B5CF6),
            Pagina("Más íconos pixel", "Wii U y Android tienen su ícono pixel propio, y el estilo «Píxeles» también se ve dentro de las carpetas de las consolas.", Icono.EMULADOR, 0xFFFF7A59),
        )),
        Version("pixel-arriba-2.0-61", "Pixel y orden", listOf(
            Pagina("Pixel art completo", "Con el estilo «Pixel art», las consolas que ese paquete no trae (Switch, Switch 2, Wii U, Android, fan games) ya no salen como insignia: se dibujan en pixeles, igual que el resto.", Icono.EMULADOR, 0xFFFF7A59),
            Pagina("Hora y batería a la izquierda", "En la vitrina de arriba, la hora y la batería pasaron junto al nombre de la consola, para que tu foto y tu nombre queden solos a la derecha.", Icono.VISTA, 0xFF3BA8FF),
            Pagina("Juegos al centro", "En la pantalla de abajo el juego elegido queda en el medio, de lado a lado, también el primero y el último.", Icono.VISTA, 0xFF23C486),
            Pagina("Apps sin sombras y título solo arriba", "La barra de apps ya no lleva sombras ni contornos, y tu título («Recién llegado») se ve solo en la pantalla de arriba.", Icono.APPS, 0xFF8B5CF6),
        )),
        Version(ID, "Ajustes ordenados", listOf(
            Pagina("Ajustes en ocho secciones", "Pantalla y estilo, Biblioteca, Inicio y widgets, Juegos y emuladores, DIPPO, Controles y sonido, Cuentas y Sistema. Dentro de cada una, las opciones parecidas van juntas en un submenú.", Icono.AJUSTES, 0xFF8B5CF6),
            Pagina("Cada opción se explica", "Al marcar una opción, abajo ves qué hace, cómo se cambia y una vista previa de cómo queda. Ahora todas tienen explicación.", Icono.TUTORIAL, 0xFF3BA8FF),
            Pagina("B vuelve donde estabas", "Si una opción de Ajustes abre otra ventana (una galería, la tienda, una consola…), con B vuelves a esa misma opción y a su submenú, no al comienzo.", Icono.CONTINUAR, 0xFF23C486),
            Pagina("El diseño nuevo en todas las vistas", "Galería, Lista y Carrusel ahora usan el mismo diseño de arriba y abajo que los Mosaicos: ya no vuelve la distribución antigua al cambiar de vista.", Icono.VISTA, 0xFFFF7A59),
            Pagina("Vuelve la vista Xbox", "«Mosaicos Xbox»: un juego grande y el resto en dos filas. Se elige en Ajustes › Biblioteca › Cómo se ven los juegos, o con R3.", Icono.VISTA, 0xFF23C486),
            Pagina("DIPPO a la vista", "En Ajustes › DIPPO, hablar con él, sus logros y los premios están apenas entras, sin buscarlos.", Icono.TROFEO, 0xFFFFB547),
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
    /** El recorrido de la primera vez, contado por DIPPO: seis pasos cortos. Se puede ver de nuevo en Ajustes. */
    val RECORRIDO = listOf(
        Pagina(
            "¡Hola! Soy DIPPO", "Soy el pulpito que vive en tu Díptico. Te muestro lo importante en un ratito. A para seguir, B para saltar.",
            Icono.BISA, 0xFFFF7A59, "A · B",
        ),
        Pagina(
            "Una consola, dos pantallas", "Arriba se luce tu juego, con su arte grande. Abajo eliges la consola y el juego.",
            Icono.PANTALLAS, 0xFF6B4EFF,
        ),
        Pagina(
            "Elige y juega", "Muévete con la cruceta o el stick. L1 y R1 cambian de consola, A juega y X abre la ficha del juego.",
            Icono.JUGAR, 0xFF2EC4A6, "L1 · R1 · A",
        ),
        Pagina(
            "Yo te ayudo", "Tócame para preguntarme qué jugar o cómo pasar una parte. Mis mensajes salen sobre mi cabeza: deslízalos si molestan.",
            Icono.BISA, 0xFFFF7A59, "Tócame",
        ),
        Pagina(
            "Tu perfil", "Tu foto está en la esquina: ahí ves tus horas y logros. Con Start + A la abres con el mando.",
            Icono.PERFIL, 0xFFE64FA0, "Start + A",
        ),
        Pagina(
            "¡A jugar!", "Start abre Ajustes y cada opción explica qué hace. De vez en cuando te contaré trucos escondidos. ¡Nos vemos!",
            Icono.AJUSTES, 0xFF3BA8FF, "Start",
        ),
    )
}



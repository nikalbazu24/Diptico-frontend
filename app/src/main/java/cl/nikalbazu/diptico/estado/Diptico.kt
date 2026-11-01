package cl.nikalbazu.diptico.estado

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.Display
import cl.nikalbazu.diptico.audio.Aviso
import cl.nikalbazu.diptico.audio.Avisos
import cl.nikalbazu.diptico.audio.Medios
import cl.nikalbazu.diptico.audio.Musica
import cl.nikalbazu.diptico.audio.Sonidos
import cl.nikalbazu.diptico.data.Almacen
import cl.nikalbazu.diptico.data.Biblioteca
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.data.Descargas
import cl.nikalbazu.diptico.data.FichaInfo
import cl.nikalbazu.diptico.data.Fichas
import cl.nikalbazu.diptico.data.CarpetaJuegos
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.data.MediosJuego
import cl.nikalbazu.diptico.emu.App
import cl.nikalbazu.diptico.emu.Lanzador
import cl.nikalbazu.diptico.emu.Pantallas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Panel { APPS, EDITAR_CONSOLA, NINGUNO, RAPIDO, PASO, MENU, CLAVE, PORTS, FICHA, TRABAJOS, BUSCAR, CENTRO, CARPETAS, ESTADISTICAS, IMAGENES, ASISTENTE, GALERIA, ESTUDIO }

/** Carpetas que Díptico puede pedir. */
enum class Carpeta { JUEGOS, MEDIA, MUSICA }

/** Pasos del tutorial guiado del primer arranque. */
enum class PasoTuto { BIENVENIDA, PANTALLAS, TEXTO, DISPOSICION, JUEGOS, CARPETA, EMULADORES, PORTS, PORTADAS, MUSICA, APARIENCIA, CONTROLES, LISTO }

/** Íconos propios de Díptico (se dibujan en ui/Iconos.kt). */
enum class Icono {
    PANTALLAS, UNA, NEGRO, LIBRE, INTERCAMBIAR, TEXTO, TEMA, INTRO, MUSICA, VOLUMEN, SILENCIO, CARPETA, EMULADOR,
    PORTADA, DESCARGA, CLAVE, TITULO, BORRAR, BIBLIOTECA, BUSCAR, TUTORIAL, INFO, CERRAR, JUGAR, DADO, LAPIZ,
    CONTINUAR, MENU, CHECK, ARRIBA, ABAJO, ESTRELLA, APPS, RELOJ, PALETA, MOVIL, CHISPA, CORAZON, MUNDO, WEB,
    CAMPANA, VISTA, WIFI, BLUETOOTH, BRILLO, AJUSTES, FORMA, MANDO, BATERIA, TROFEO, ASISTENTE, IDIOMA, OCULTAR,
    COPIAR, PEGAR, BISA, EDITAR,
}

/**
 * Secciones del menú, ordenadas por grupos: lo que más se cambia arriba, lo técnico abajo.
 * [grupo] es el título que aparece sobre la primera sección de cada grupo.
 */
enum class Seccion(val titulo: String, val icono: Icono, val ayuda: String, val grupo: String = "") {
    ESENCIAL("Lo esencial", Icono.ESTRELLA, "Lo que más se cambia, todo junto. Y para buscar cualquier ajuste por su nombre.", "Empieza aquí"),
    TEMAS("Temas", Icono.PALETA, "Colores, temas de fiestas del mundo, tus propios colores y temas creados con IA.", "Cómo se ve"),
    APARIENCIA("Apariencia", Icono.TEMA, "Claro u oscuro, efecto de los botones, decoraciones, íconos de las consolas y la animación de entrada."),
    INICIO("Inicio", Icono.APPS, "La pantalla principal: el juego en grande o tu inicio con widgets, y la bienvenida al encender."),
    ESTANTE("Estante", Icono.VISTA, "Cómo se ven tus consolas y tus juegos: vistas, tamaño, filtros y orden."),
    MUSICA("Música", Icono.MUSICA, "La música de fondo y los sonidos. Se calla sola al abrir un juego o si pones Spotify o YouTube Music."),
    CONSOLAS("Tus consolas", Icono.MANDO, "Personaliza cada consola: nombre, color, ícono, imagen grande, logo, música y cajas.", "Tus juegos"),
    BIBLIOTECA("Biblioteca", Icono.BIBLIOTECA, "Tus carpetas de juegos, ports, juegos de Android y las imágenes que ya tengas de ES-DE."),
    PORTADAS("Portadas", Icono.PORTADA, "Descarga portadas, imágenes, logos y títulos de internet."),
    EMULADORES("Emuladores", Icono.EMULADOR, "Con qué emulador, o con qué núcleo de RetroArch, se abre cada consola."),
    LOGROS("Logros", Icono.TROFEO, "Conecta tu cuenta de RetroAchievements para ver tus logros y puntos en cada juego."),
    ASISTENTE("Bisa", Icono.BISA, "Bisa, tu asistente con IA: conoce tus juegos, te ayuda cuando te atascas y cambia cosas de Díptico por ti.", "Asistente"),
    PANTALLAS("Pantallas", Icono.PANTALLAS, "Cuántas pantallas usa Díptico, qué va en cada una, el tamaño de la letra y la atenuación al jugar.", "Consola"),
    CONSOLA("Android", Icono.CAMPANA, "Díptico como inicio, notificaciones y accesos rápidos de Android."),
    IDIOMA("Idioma", Icono.IDIOMA, "El idioma de Díptico: español, inglés, portugués, chino o japonés."),
    SISTEMA("Acerca de", Icono.INFO, "Repite el tutorial o mira la versión de Díptico."),
}

/** Un juego abierto ahora mismo. */
data class Sesion(val clave: String, val inicio: Long, val displayId: Int, val dosPantallas: Boolean)

data class Opcion(
    val titulo: String,
    val valor: String = "",
    val icono: Icono? = null,
    val ayuda: String = "",
    val apagada: Boolean = false,
    /** Si es una app, su paquete (para mostrar su ícono). */
    val paquete: String? = null,
    /** Para opciones con valor: qué hace ← (la acción normal es →). */
    val menos: (() -> Unit)? = null,
    val accion: () -> Unit,
)

/** Algo que Díptico hace de fondo (buscar juegos, bajar portadas…). [avance] va de 0 a 1, o null si no se sabe. */
data class Trabajo(val id: String, val texto: String, val avance: Float? = null)

/** Un widget del inicio personalizado. [tam]: 0 oculto, 1 chico, 2 mediano, 3 ancho completo. */
/**
 * Un widget del inicio, en una grilla de [Widgets.COLUMNAS] x [Widgets.FILAS].
 * [x], [y]: celda de arriba a la izquierda; [an], [al]: cuántas celdas ocupa.
 */
data class Widget(
    val tipo: String, val x: Int, val y: Int, val an: Int, val al: Int, val visible: Boolean = true,
    /** Página del inicio donde está. */
    val pagina: Int = 0,
    /** Imagen o GIF propio (widgets de tipo "imagen-…"). */
    val uri: String? = null,
    /** Estilo: tarjeta, vidrio, solido, contorno o sinfondo. */
    val estilo: String = "tarjeta",
    /** Color propio del widget (0 = el del tema), qué tan opaco es su fondo y qué tan redondas sus esquinas. */
    val color: Long = 0L,
    val opacidad: Float = 1f,
    val radio: Int = 22,
    /** Diseño del contenido (por ejemplo reloj digital, analógico o mínimo) y tamaño de sus letras. */
    val variante: Int = 0,
    val escala: Float = 1f,
)

/** Lo que la persona cambió de una consola: nombre, color, imagen grande (hero), logo y música. */
data class ConsolaPropia(
    val nombre: String? = null,
    val color: Long = 0L,
    val hero: String? = null,
    val logo: String? = null,
    val musica: String? = null,
) {
    val vacia: Boolean get() = nombre.isNullOrBlank() && color == 0L && hero == null && logo == null && musica == null
}

/** Forma de las cajas de una consola en el estante. */
data class FormaCfg(val forma: String = "original", val tam: Float = 1f, val imagen: String = "portada")

object Widgets {
    /** Tamaño de la grilla (se puede cambiar en Menú › Inicio). */
    var COLUMNAS = 6
    var FILAS = 4

    /** Tamaños y formas posibles (ancho x alto en celdas): todos los que caben en la grilla. */
    val TAMANOS: List<Pair<Int, Int>>
        get() = (1..COLUMNAS).flatMap { an -> (1..FILAS).map { al -> an to al } }.sortedWith(compareBy({ it.first * it.second }, { it.first }))

    val ESTILOS = listOf("tarjeta", "vidrio", "solido", "contorno", "sinfondo")
    fun nombreEstilo(e: String) = when (e) { "vidrio" -> "Vidrio"; "solido" -> "Color"; "contorno" -> "Contorno"; "sinfondo" -> "Sin fondo"; else -> "Tarjeta" }

    fun nombreTamano(an: Int, al: Int) = "$an × $al · " + when (an to al) {
        2 to 1 -> "Chico"; 2 to 2 -> "Cuadrado"; 3 to 2 -> "Mediano"; 4 to 2 -> "Ancho"; 6 to 1 -> "Franja"
        2 to 3 -> "Alto"; 3 to 3 -> "Grande"; 4 to 3 -> "Muy grande"; 6 to 2 -> "Panorámico"; 3 to 1 -> "Barra"
        else -> when { an == al -> "Cuadrado"; an > al -> "Ancho"; else -> "Alto" }
    }

    val porDefecto = listOf(
        Widget("juego", 0, 0, 4, 2), Widget("reloj", 4, 0, 2, 2), Widget("musica", 0, 2, 3, 2),
        Widget("recientes", 3, 2, 3, 2), Widget("paso", 0, 0, 2, 2, false), Widget("fiesta", 0, 0, 2, 2, false),
        Widget("tiempo", 0, 0, 2, 1, false), Widget("bateria", 0, 0, 2, 1, false), Widget("consolas", 0, 0, 3, 2, false),
        Widget("favoritos", 0, 0, 3, 2, false), Widget("frase", 0, 0, 6, 1, false),
        Widget("estadisticas", 0, 0, 3, 2, false), Widget("sugerencia", 0, 0, 2, 2, false),
        Widget("accesos", 0, 0, 3, 1, false), Widget("notificaciones", 0, 0, 3, 2, false),
        Widget("logros", 0, 0, 3, 2, false), Widget("asistente", 0, 0, 3, 1, false),
        Widget("apps", 0, 0, 3, 1, false),
    )

    /** Para qué sirve cada widget (en el catálogo del estudio). */
    fun descripcion(tipo: String) = when (tipo) {
        "juego" -> "El juego elegido en grande, listo para jugar"; "reloj" -> "La hora y la fecha (digital, analógico o mínimo)"
        "musica" -> "Lo que suena en Spotify o YouTube Music, con botones"; "paso" -> "Lo que anotaste para seguir tu juego"
        "recientes" -> "Tus últimos juegos, a un toque"; "fiesta" -> "La fiesta del mundo de hoy o la próxima"
        "tiempo" -> "Cuántas horas llevas jugando"; "bateria" -> "La carga de la consola"; "consolas" -> "Cuántos juegos y consolas tienes"
        "favoritos" -> "Tus juegos favoritos"; "frase" -> "Una frase distinta cada día"; "estadisticas" -> "Tus horas por consola"
        "sugerencia" -> "Un juego al azar para descubrir"; "accesos" -> "Wi-Fi, Bluetooth, brillo y más"
        "notificaciones" -> "Lo último que llegó a tu consola"; "logros" -> "Tus logros de RetroAchievements"
        "asistente" -> "Un acceso directo para hablar con Bisa"
        "apps" -> "Tus apps fijadas y el cajón con todas"
        else -> "Tu imagen o GIF animado"
    }

    fun icono(tipo: String): Icono = when (tipo) {
        "juego" -> Icono.JUGAR; "reloj" -> Icono.RELOJ; "musica" -> Icono.MUSICA; "paso" -> Icono.LAPIZ; "recientes" -> Icono.CONTINUAR
        "fiesta" -> Icono.MUNDO; "tiempo" -> Icono.RELOJ; "bateria" -> Icono.BATERIA; "consolas" -> Icono.BIBLIOTECA
        "favoritos" -> Icono.CORAZON; "frase" -> Icono.CHISPA; "estadisticas" -> Icono.TROFEO; "sugerencia" -> Icono.DADO
        "accesos" -> Icono.AJUSTES; "notificaciones" -> Icono.CAMPANA; "logros" -> Icono.TROFEO; "asistente" -> Icono.BISA; "apps" -> Icono.APPS
        else -> Icono.PORTADA
    }

    /** Diseños posibles del contenido de cada widget. */
    fun variantes(tipo: String): List<String> = when (tipo) {
        "reloj" -> listOf("Digital", "Analógico", "Mínimo")
        "juego" -> listOf("Con caja", "Arte de fondo")
        "bateria" -> listOf("Barra", "Anillo")
        "recientes", "favoritos" -> listOf("Cajas", "Lista")
        else -> emptyList()
    }

    /** Formas listas para elegir (ancho x alto en celdas). */
    val FORMAS = listOf(
        "Chico" to (2 to 1), "Cuadrado" to (2 to 2), "Mediano" to (3 to 2), "Ancho" to (4 to 2),
        "Alto" to (2 to 3), "Grande" to (3 to 3), "Franja" to (6 to 1), "Panorámico" to (6 to 2),
    )

    fun nombre(tipo: String) = when (tipo) {
        "juego" -> "Juego elegido"; "reloj" -> "Reloj y fecha"; "musica" -> "Música (Spotify, YouTube Music…)"
        "paso" -> "Tu próximo paso"; "recientes" -> "Jugados hace poco"; "fiesta" -> "Fiestas del mundo"
        "tiempo" -> "Tiempo jugado"; "bateria" -> "Batería"; "consolas" -> "Tu colección"
        "favoritos" -> "Favoritos"; "frase" -> "Saludo del día"; "estadisticas" -> "Estadísticas"
        "sugerencia" -> "¿Qué juego hoy?"; "accesos" -> "Accesos rápidos"; "notificaciones" -> "Notificaciones"
        "logros" -> "Logros (RetroAchievements)"; "asistente" -> "Asistente con IA"; "apps" -> "Apps"
        else -> if (tipo.startsWith("imagen")) "Imagen o GIF" else tipo
    }
}

/** Descarga de portadas en curso. */
data class Descarga(val hechos: Int, val total: Int, val actual: String, val encontrados: Int)

data class Estado(
    val listo: Boolean = false,
    val configurado: Boolean = false,
    val tieneCarpeta: Boolean = false,
    val tieneMedia: Boolean = false,
    val escaneando: Boolean = false,
    val juegos: List<Juego> = emptyList(),
    val consolaSel: String = "todo",
    val juegoSel: String? = null,
    val oscuro: Boolean = true,
    val intercambiar: Boolean = false,
    val pantallaJuegos: String = "arriba",
    val modoPantallas: String = "una_negra",
    val modoElegido: Boolean = false,
    val dosDisponibles: Boolean = true,
    val sesion: Sesion? = null,
    val despierta: Boolean = false,
    val panel: Panel = Panel.NINGUNO,
    val indice: Int = 0,
    val seccion: Seccion = Seccion.PANTALLAS,
    val enOpciones: Boolean = false,
    val borrador: String = "",
    val pregunta: Boolean = false,
    val aviso: String? = null,
    val datos: Map<String, DatosJuego> = emptyMap(),
    val emuladores: Map<String, String> = emptyMap(),
    val intro: Boolean = true,
    val introVez: Int = 0,
    val mostrarIntro: Boolean = true,
    val pasoTuto: PasoTuto = PasoTuto.BIENVENIDA,
    val probados: Set<String> = emptySet(),
    val tamTexto: Float = 1f,
    val musica: Boolean = true,
    val pista: String = "bisagra",
    val volumen: Float = 0.6f,
    val carpetaMusica: Uri? = null,
    val claveSgdb: String = "",
    val titulosBonitos: Boolean = true,
    val descarga: Descarga? = null,
    val aLaVista: Int = 0,
    val ports: List<String> = emptyList(),
    val apps: List<App> = emptyList(),
    val cargandoApps: Boolean = false,
    val android: List<String> = emptyList(),
    /** A qué estante se agregan las apps del panel: "ports" o "androidgames". */
    val appsDestino: String = "ports",
    /** Para qué es el cuadro de texto: "sgdb", "ia_clave", "ia_pedido", "titulo" o "notas". */
    val textoPara: String = "sgdb",
    val tema: String = "diptico",
    val temaFestivo: Boolean = true,
    val decoraciones: Boolean = true,
    val temasPropios: List<Tema> = emptyList(),
    val claveIa: String = "",
    /** Claves de otras IA en la nube: Gemini (Google) y ChatGPT (OpenAI). */
    val claveGemini: String = "",
    val claveOpenAI: String = "",
    /** Cuenta de RetroAchievements. */
    val usuarioRA: String = "",
    val claveRA: String = "",
    val perfilRA: cl.nikalbazu.diptico.data.PerfilRA? = null,
    val logrosFicha: cl.nikalbazu.diptico.data.LogrosJuego? = null,
    val cargandoLogros: Boolean = false,
    val errorLogros: String? = null,
    /** Conversación con el asistente. */
    val chat: List<MensajeChat> = emptyList(),
    val pensando: Boolean = false,
    /** Bisa puede buscar guías en internet (cuesta un poco más de la clave). */
    val bisaWeb: Boolean = true,
    /** Motor de Bisa: auto, local, gemini, openai o claude. */
    val motorIA: String = "auto",
    val iaLocalLista: Boolean = false,
    /** Bisa ofrece ayuda mientras juegas (aviso y burbuja). */
    val bisaEnJuego: Boolean = true,
    /** Burbuja flotante de Bisa: abierta (ventanita) y con aviso. */
    val burbujaAbierta: Boolean = false,
    val burbujaAlerta: Boolean = false,
    /** La lupa está copiando la pantalla de arriba. */
    val lupaActiva: Boolean = false,
    /** Tamaño del logo en el compañero de partida (0.6 a 1.8). */
    val tamLogo: Float = 1f,
    /** Brillo de la pantalla atenuada mientras juegas (0.01 a 0.4; 1 = no atenuar). */
    val atenuacion: Float = 0.04f,
    /** Idioma de Díptico: es, en, pt, zh, ja. */
    val idioma: String = "es",
    /** Lo que se escribió para buscar un ajuste (en "Lo esencial"). */
    val buscarAjuste: String = "",
    /** Galería abierta: temas, musica o colores; y su pestaña. */
    val galeria: String = "temas",
    val galeriaPestana: Int = 0,
    /** Inicio con widgets: separación entre widgets y si se ve el saludo. */
    val huecoInicio: Int = 10,
    val saludoInicio: Boolean = true,
    val fiesta: Fiesta? = null,
    val ficha: FichaInfo? = null,
    val fichaDe: String? = null,
    val pestanaFicha: Int = 0,
    val cargandoFicha: Boolean = false,
    /** Fotos del juego para el carrusel de la ficha (arte propio, Wikipedia y libretro). */
    val fichaImagenes: List<cl.nikalbazu.diptico.data.ImagenWeb> = emptyList(),
    /** Opciones de internet para cambiar una imagen del juego. */
    val candidatos: List<cl.nikalbazu.diptico.data.ImagenWeb> = emptyList(),
    val candidatoPara: String = "portada",
    val buscandoCandidatos: Boolean = false,
    /** Pestaña de la tienda de imágenes y el nombre con que se buscó. */
    val fuenteImagen: String = "todas",
    val nombreImagen: String = "",
    val inicio: String = "heroe",
    val widgets: List<Widget> = Widgets.porDefecto,
    val estiloIconos: String = "diptico",
    val iconosPropios: Map<String, String> = emptyMap(),
    val iconosVersion: Int = 0,
    val trabajos: List<Trabajo> = emptyList(),
    val relojGrande: Boolean = false,
    /** "estante", "cuadricula", "lista" o "carrusel". */
    val vista: String = "estante",
    /** Todas las apps de la consola (se cargan al abrir el cajón de apps), las fijadas y la pestaña elegida. */
    val appsTodas: List<App> = emptyList(),
    val appsFijadas: List<String> = emptyList(),
    val appsPestana: Int = 0,
    val cargandoTodas: Boolean = false,
    /** Personalización de cada consola (id -> lo que se cambió) y la consola que se está editando. */
    val consolasPropias: Map<String, ConsolaPropia> = emptyMap(),
    val consolaEditando: String = "",
    /** Cómo se ven las consolas: "arriba" (pestañas), "lado" (columna), "rueda" (dial) o "sistemas" (carrusel y luego juegos). */
    val vistaConsolas: String = "arriba",
    /** En modo sistemas: true mientras se elige la consola (antes de entrar a sus juegos). */
    val enSistemas: Boolean = false,
    val tamJuegos: Float = 1f,
    /** "todos", "favoritos", "recientes", "mas" (más jugados) o "sinjugar". */
    val filtro: String = "todos",
    /** "az", "recientes" o "tiempo". */
    val orden: String = "az",
    val busqueda: String = "",
    val formas: Map<String, FormaCfg> = emptyMap(),
    val editandoWidgets: Boolean = false,
    val widgetSel: Int = 0,
    /** Estudio de widgets: pestaña (0 widget, 1 agregar, 2 página) y si la cruceta está moviendo el widget. */
    val estudioPestana: Int = 0,
    val estudioMover: Boolean = false,
    /** Zoom de cada pantalla (1 = automático según su forma). */
    val zoomArriba: Float = 1f,
    val zoomAbajo: Float = 1f,
    val musicaTema: Boolean = true,
    val avisosEmergentes: Boolean = true,
    val carpetas: List<CarpetaJuegos> = emptyList(),
    /** En el panel de carpetas: índice de la carpeta a la que se le elige consola (-1 = ninguna). */
    val consolaPara: Int = -1,
    /** Pista que corresponde al tema en uso. */
    val pistaTema: String = "bisagra",
    /** Fondo de los botones: "liso", "degradado", "brillo" o "vidrio". */
    val efecto: String = "degradado",
    /** "auto", "alta", "equilibrada" o "ahorro". */
    val fluidez: String = "auto",
    /** Lo que eligió "auto" según la consola. */
    val fluidezDetectada: String = "alta",
    val sonidos: Boolean = true,
    /** Escribir con el teclado de Díptico, que aparece en la otra pantalla. */
    val tecladoPropio: Boolean = true,
    val teclaFila: Int = 2,
    /** Posición del cursor en el texto (-1 = al final) y el otro extremo de la selección (null = sin selección). */
    val cursor: Int = -1,
    val ancla: Int? = null,
    val teclaCol: Int = 0,
    val mayus: Boolean = true,
    val simbolos: Boolean = false,
    /** Minutos sin usar la consola antes del protector de pantalla (0 = nunca). */
    val protectorMin: Int = 5,
    val protector: Boolean = false,
    /** Grilla del inicio: columnas, filas y páginas. */
    val gridCols: Int = 6,
    val gridFilas: Int = 4,
    val paginas: Int = 1,
    val paginaInicio: Int = 0,
    /** Imagen o GIF de fondo del inicio. */
    val fondoInicio: String? = null,
    /** Bienvenida al encender: cuándo se mostró (0 = no está a la vista), y si es la versión breve. */
    val bienvenida: Long = 0L,
    val bienvenidaBreve: Boolean = false,
    /** Cuánto rato estuvo apagada la consola antes de esta bienvenida (milisegundos). */
    val ausencia: Long = 0L,
    /** Mensaje de continuación al encender en medio de un juego (0 = no está a la vista). */
    val continuacion: Long = 0L,
    /** "completa", "breve" o "no". */
    val bienvenidaModo: String = "completa",
    /** Cómo te llama Bisa (vacío = sin nombre). */
    val nombre: String = "",
    /** Díptico es la app de inicio de Android (el botón Inicio trae aquí). */
    val esInicio: Boolean = true,
) {
    /** Todo Díptico vive en una sola pantalla (siempre durante el tutorial). */
    val unaPantalla: Boolean
        get() = !configurado || modoPantallas != "dos" || !dosDisponibles

    /** Se está escribiendo con el teclado de Díptico (en la pantalla contraria a la del texto). */
    val conTeclado: Boolean
        get() = tecladoPropio && !unaPantalla && (panel == Panel.PASO || panel == Panel.CLAVE)

    val consolasConJuegos: List<Consola>
        get() = Consolas.todas.filter { c -> juegos.any { it.consolaId == c.id } }

    /** Consolas que usan emulador (todas menos Ports). */
    val consolasEmu: List<Consola>
        get() = consolasConJuegos.filter { !it.sinEmulador }

    val pestanas: List<String>
        get() = listOf("todo") + consolasConJuegos.map { it.id }

    /** Los juegos del estante: consola elegida, filtro, búsqueda y orden. Se calcula una vez por estado. */
    val visibles: List<Juego> by lazy {
        var l = if (consolaSel == "todo") juegos else juegos.filter { it.consolaId == consolaSel }
        fun d(j: Juego) = datos[j.clave]
        l = when (filtro) {
            "ocultos" -> l.filter { d(it)?.oculto == true }
            else -> l.filter { d(it)?.oculto != true }
        }
        l = when (filtro) {
            "favoritos" -> l.filter { d(it)?.favorito == true }
            "recientes" -> l.filter { (d(it)?.ultimo ?: 0L) > 0 }
            "mas" -> l.filter { (d(it)?.tiempo ?: 0L) > 0 }
            "sinjugar" -> l.filter { (d(it)?.tiempo ?: 0L) == 0L }
            else -> l
        }
        if (busqueda.isNotBlank()) {
            val q = Busqueda.normalizar(busqueda)
            l = l.filter { Busqueda.normalizar(it.titulo).contains(q) }
        }
        when (orden) {
            "recientes" -> l.sortedByDescending { d(it)?.ultimo ?: 0L }
            "tiempo" -> l.sortedByDescending { d(it)?.tiempo ?: 0L }
            else -> if (consolaSel == "todo" && filtro == "todos" && busqueda.isBlank()) l else l.sortedBy { it.titulo.lowercase() }
        }
    }

    val seleccionado: Juego? by lazy {
        val v = visibles
        v.firstOrNull { it.clave == juegoSel } ?: v.firstOrNull()
    }

    val indiceSel: Int by lazy {
        val clave = seleccionado?.clave
        if (clave == null) 0 else visibles.indexOfFirst { it.clave == clave }.coerceAtLeast(0)
    }

    val sinPortada: Int
        get() = juegos.count { it.portada == null }
}

/** Para buscar sin importar tildes ni mayúsculas. */
object Busqueda {
    fun normalizar(s: String): String =
        java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()
}

/** Estado compartido por las dos pantallas y todas las acciones de Díptico. */
object Diptico {
    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado

    internal lateinit var app: Context
    internal lateinit var almacen: Almacen
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var avisoJob: Job? = null
    private var despiertaJob: Job? = null
    private var descargaJob: Job? = null
    private var introInicio = 0L
    private var crudos: List<Juego> = emptyList()
    private var medios: Map<String, MediosJuego> = emptyMap()

    const val VERSION = "1.0 beta"

    /** La pantalla activa registra aquí cómo pedir una carpeta. */
    var pedirCarpeta: ((Carpeta) -> Unit)? = null

    fun iniciar(ctx: Context) {
        if (::app.isInitialized) return
        app = ctx.applicationContext
        almacen = Almacen(app)
        Consolas.cargar(app)
        Musica.iniciar(app)
        val carpeta = almacen.carpeta
        medios = almacen.medios()
        crudos = almacen.biblioteca()
        val idioma = almacen.texto("idioma").ifEmpty { Idioma.delSistema() }
        Idioma.fijar(app, idioma)
        _estado.value = Estado(
            listo = true,
            idioma = idioma,
            configurado = almacen.configurado && carpeta != null,
            tieneCarpeta = carpeta != null,
            tieneMedia = almacen.media != null,
            juegos = Descargas.aplicar(crudos, medios, almacen.titulosBonitos),
            oscuro = almacen.oscuro,
            intercambiar = almacen.intercambiar,
            pantallaJuegos = almacen.pantallaJuegos,
            modoPantallas = if (almacen.modoElegido) almacen.modoPantallas else "una_negra",
            modoElegido = almacen.modoElegido,
            dosDisponibles = Pantallas.inferior(app) != null,
            datos = almacen.datos(),
            emuladores = almacen.emuladores(),
            intro = almacen.mostrarIntro,
            mostrarIntro = almacen.mostrarIntro,
            tamTexto = almacen.tamTexto,
            musica = almacen.musica,
            pista = almacen.pista,
            volumen = almacen.volumen,
            carpetaMusica = almacen.carpetaMusica,
            claveSgdb = almacen.claveSgdb,
            titulosBonitos = almacen.titulosBonitos,
            ports = almacen.ports,
            android = almacen.android ?: emptyList(),
            tema = almacen.texto("tema", "diptico"),
            temaFestivo = almacen.bool("temaFestivo", true),
            decoraciones = almacen.bool("decoraciones", true),
            temasPropios = leerTemas(almacen.texto("temasPropios", "[]")),
            claveIa = almacen.texto("claveIa"),
            claveGemini = almacen.texto("claveGemini"),
            claveOpenAI = almacen.texto("claveOpenAI"),
            usuarioRA = almacen.texto("usuarioRA"),
            bisaWeb = almacen.bool("bisaWeb", true),
            motorIA = almacen.texto("motorIA", "auto"),
            iaLocalLista = IALocal.listo(app),
            tamLogo = almacen.texto("tamLogo", "1").toFloatOrNull() ?: 1f,
            atenuacion = almacen.texto("atenuacion", "0.04").toFloatOrNull() ?: 0.04f,
            bisaEnJuego = almacen.bool("bisaEnJuego", true),
            claveRA = almacen.texto("claveRA"),
            inicio = almacen.texto("inicio", "heroe"),
            widgets = leerWidgets(almacen.texto("widgets")),
            estiloIconos = almacen.texto("estiloIconos", "diptico"),
            iconosPropios = leerMapa(almacen.texto("iconosPropios", "{}")),
            vista = almacen.texto("vista", "estante"),
            vistaConsolas = almacen.texto("vistaConsolas", "arriba"),
            consolasPropias = leerConsolasPropias(almacen.texto("consolasPropias", "{}")),
            appsFijadas = almacen.texto("appsFijadas").split(",").filter { it.isNotBlank() },
            enSistemas = almacen.texto("vistaConsolas", "arriba") == "sistemas",
            tamJuegos = almacen.texto("tamJuegos", "1").toFloatOrNull() ?: 1f,
            orden = almacen.texto("orden", "az"),
            formas = leerFormas(almacen.texto("formas", "{}")),
            zoomArriba = almacen.texto("zoomArriba", "1").toFloatOrNull() ?: 1f,
            zoomAbajo = almacen.texto("zoomAbajo", "1").toFloatOrNull() ?: 1f,
            musicaTema = almacen.bool("musicaTema", true),
            avisosEmergentes = almacen.bool("avisosEmergentes", true),
            carpetas = almacen.carpetas(),
            efecto = almacen.texto("efecto", "degradado"),
            fluidez = almacen.texto("fluidez", "auto"),
            fluidezDetectada = detectarFluidez(),
            sonidos = almacen.bool("sonidos", true),
            tecladoPropio = almacen.bool("tecladoPropio", true),
            protectorMin = almacen.texto("protectorMin", "5").toIntOrNull() ?: 5,
            gridCols = almacen.texto("gridCols", "6").toIntOrNull() ?: 6,
            huecoInicio = almacen.texto("huecoInicio", "10").toIntOrNull() ?: 10,
            saludoInicio = almacen.bool("saludoInicio", true),
            gridFilas = almacen.texto("gridFilas", "4").toIntOrNull() ?: 4,
            paginas = almacen.texto("paginas", "1").toIntOrNull() ?: 1,
            fondoInicio = almacen.texto("fondoInicio").ifEmpty { null },
            bienvenidaModo = almacen.texto("bienvenidaModo", "completa"),
            nombre = almacen.texto("nombre"),
            esInicio = Inicio.esPredeterminado(app),
        )
        Consolas.personalizar(estado.value.consolasPropias.mapValues { (_, v) -> v.nombre to v.color })
        Inicio.vigilarPantalla(app)
        Inicio.inicioAbajo(app, !(almacen.modoElegido && almacen.modoPantallas == "una_libre"))
        bienvenidaPendiente = true
        Widgets.COLUMNAS = estado.value.gridCols
        Widgets.FILAS = estado.value.gridFilas
        Sonidos.iniciar(app)
        Sonidos.activo = estado.value.sonidos
        aplicarFluidez()
        vigilarProtector()
        // Avisos emergentes de las notificaciones (nunca mientras juegas).
        scope.launch {
            Avisos.nuevos.collect { a ->
                val e = estado.value
                if (e.avisosEmergentes && e.sesion == null && e.aLaVista > 0) avisar("${a.app}: ${a.titulo.ifBlank { a.texto }}".take(90))
            }
        }
        aplicarTema()
        publicarJuegos()
        Medios.revisar(app)
        if (almacen.android == null) detectarAndroid(silencioso = true)
        if (carpeta != null) escanear()
        // La música sigue al estado: suena con Díptico a la vista y sin juego abierto.
        scope.launch {
            estado.map { e ->
                // Si el juego elegido tiene su propia canción, suena esa.
                val propia = if (e.configurado && e.panel != Panel.MENU) {
                    e.seleccionado?.let { e.datos[it.clave]?.musica } ?: e.consolasPropias[e.consolaSel]?.musica
                } else null
                val pista = when {
                    propia != null -> "archivo:$propia"
                    e.configurado && e.musicaTema && e.pista != "mia" -> e.pistaTema
                    else -> e.pista
                }
                listOf(e.listo && e.musica && e.aLaVista > 0 && e.sesion == null, pista, e.volumen, e.carpetaMusica)
            }.distinctUntilChanged().collect { (sonar, pista, vol, carpetaM) ->
                Musica.sincronizar(sonar as Boolean, pista as String, vol as Float, carpetaM as Uri?)
            }
        }
    }

    /** Solo para las capturas automáticas: fija un estado de ejemplo. */
    fun fijarParaPrueba(e: Estado) {
        _estado.value = e
    }

    internal fun actualizar(f: (Estado) -> Estado) {
        _estado.value = f(_estado.value)
    }

    /** Una pantalla de Díptico aparece (+1) o se va (-1). */
    fun aLaVista(delta: Int) {
        actualizar { it.copy(aLaVista = (it.aLaVista + delta).coerceAtLeast(0)) }
    }

    /** La pantalla de abajo existe ahora (por ejemplo, si se conecta o desconecta una pantalla). */
    fun revisarPantallas() {
        val hay = Pantallas.inferior(app) != null
        if (hay != estado.value.dosDisponibles) actualizar { it.copy(dosDisponibles = hay) }
    }

    // ---------- Animación de entrada ----------

    /** Momento en que empezó la animación; las dos pantallas lo comparten para ir sincronizadas. */
    fun inicioIntro(): Long {
        if (introInicio == 0L) introInicio = SystemClock.uptimeMillis()
        return introInicio
    }

    fun terminarIntro() {
        if (estado.value.intro) actualizar { it.copy(intro = false) }
        // Al prender la consola de cero: después de la animación, la bienvenida.
        if (bienvenidaPendiente) {
            bienvenidaPendiente = false
            scope.launch { delay(350); mostrarBienvenida(0L) }
        }
    }

    // ---------- Bienvenida y continuación ----------

    private var bienvenidaPendiente = false
    private var bienvenidaJob: Job? = null

    /** El último juego que se jugó (para "Continuar"). */
    fun ultimoJugado(e: Estado = estado.value): cl.nikalbazu.diptico.data.Juego? =
        e.datos.entries.filter { it.value.ultimo > 0 }.sortedByDescending { it.value.ultimo }
            .firstNotNullOfOrNull { (k, _) -> e.juegos.firstOrNull { it.clave == k } }

    /** La pantalla se encendió después de [ausencia] ms apagada. Se llama desde [Inicio]. */
    fun alEncender(ausencia: Long, estabaJugando: Boolean) {
        val e = estado.value
        if (!e.configurado || e.intro) return
        if (e.sesion != null) {
            if (estabaJugando && ausencia > 8_000) mostrarContinuacion()
            return
        }
        // Solo si al apagarla estabas en Díptico (no en otra app).
        if (ausencia > 20_000 && e.aLaVista > 0) mostrarBienvenida(ausencia)
    }

    fun mostrarBienvenida(ausencia: Long) {
        val e = estado.value
        if (e.bienvenidaModo == "no" || !e.configurado || e.sesion != null) return
        val breve = e.bienvenidaModo == "breve" || ultimoJugado(e) == null && e.juegos.isEmpty()
        actualizar { it.copy(bienvenida = System.currentTimeMillis(), bienvenidaBreve = breve, ausencia = ausencia, protector = false, relojGrande = false) }
        actividad()
        bienvenidaJob?.cancel()
        bienvenidaJob = scope.launch {
            delay(if (breve) 4_000 else 10_000)
            cerrarBienvenida()
        }
    }

    fun cerrarBienvenida() {
        bienvenidaJob?.cancel()
        if (estado.value.bienvenida != 0L) actualizar { it.copy(bienvenida = 0L) }
    }

    /** "Seguir jugando" desde la bienvenida: abre el último juego. */
    fun seguirDesdeBienvenida() {
        val j = ultimoJugado()
        cerrarBienvenida()
        if (j == null) return
        actualizar { it.copy(consolaSel = "todo", juegoSel = j.clave, panel = Panel.NINGUNO, enSistemas = false) }
        jugar()
    }

    /** Encendiste la consola en medio de un juego: un recordatorio de dónde ibas. */
    fun mostrarContinuacion() {
        val e = estado.value
        val s = e.sesion ?: return
        if (e.bienvenidaModo == "no") return
        val j = e.juegos.firstOrNull { it.clave == s.clave } ?: return
        val minutos = (System.currentTimeMillis() - s.inicio) / 60_000
        val paso = e.datos[s.clave]?.paso.orEmpty()
        actualizar { it.copy(continuacion = System.currentTimeMillis()) }
        despertar()
        cl.nikalbazu.diptico.bisa.Cartel.mostrar(
            app, s.displayId, "Sigues jugando", j.titulo,
            listOfNotNull(if (minutos > 0) "$minutos min en esta partida" else null, paso.takeIf { it.isNotBlank() }?.let { "Tu paso: $it" }).joinToString(" · "),
        )
        scope.launch {
            delay(7_000)
            actualizar { it.copy(continuacion = 0L) }
        }
    }

    fun cambiarBienvenidaModo() {
        val l = listOf("completa", "breve", "no")
        val v = l[(l.indexOf(estado.value.bienvenidaModo) + 1).mod(l.size)]
        almacen.guardarTexto("bienvenidaModo", v)
        actualizar { it.copy(bienvenidaModo = v) }
        if (v != "no") mostrarBienvenida(3 * 3_600_000L)
    }

    fun nombreBienvenida(v: String) = when (v) { "breve" -> "Breve"; "no" -> "No"; else -> "Completa" }

    // ---------- Díptico como inicio de Android ----------

    /** La pantalla activa registra aquí cómo pedir a Android que Díptico sea la app de inicio. */
    var pedirSerInicio: (() -> Unit)? = null

    fun hacerInicio() {
        if (Inicio.esPredeterminado(app)) { actualizar { it.copy(esInicio = true) }; avisar("Díptico ya es tu pantalla de inicio ✓"); return }
        pedirSerInicio?.invoke() ?: abrirIntent(Intent(android.provider.Settings.ACTION_HOME_SETTINGS), "No pude abrir los ajustes de inicio")
    }

    fun revisarInicio() {
        val v = Inicio.esPredeterminado(app)
        if (v != estado.value.esInicio) actualizar { it.copy(esInicio = v) }
    }

    private fun verIntro() {
        introInicio = 0L
        actualizar { it.copy(intro = true, introVez = it.introVez + 1, panel = Panel.NINGUNO) }
    }

    // ---------- Carpeta y biblioteca ----------

    fun escanear() {
        val carpetas = almacen.carpetas()
        if (carpetas.isEmpty()) return
        actualizar { it.copy(escaneando = true) }
        trabajo("escaneo", "Buscando tus juegos")
        scope.launch {
            var fallas = 0
            val lista = withContext(Dispatchers.IO) {
                carpetas.flatMap { c ->
                    try {
                        Biblioteca.escanear(app, c.uri, almacen.media, c.consola)
                    } catch (e: Exception) {
                        fallas++
                        emptyList()
                    }
                }.distinctBy { it.clave }.sortedWith(compareBy<Juego>({ Consolas.orden(it.consolaId) }, { it.titulo.lowercase() }))
            }.takeIf { fallas < carpetas.size }
            if (fallas > 0) avisar(if (lista == null) "No pude leer tus carpetas de juegos. Elígelas otra vez." else "No pude leer $fallas de tus carpetas")
            if (lista != null) {
                crudos = lista
                withContext(Dispatchers.IO) { almacen.guardarBiblioteca(lista) }
                publicarJuegos()
                actualizar { it.copy(escaneando = false, indice = if (it.configurado) it.indice else it.indice.coerceAtMost(1)) }
                bajarIconosFaltantes()
                identificarNombres()
            } else {
                actualizar { it.copy(escaneando = false) }
            }
            terminarTrabajo("escaneo")
        }
    }

    /** Los ports elegidos, como juegos de la consola "Ports". */
    private fun juegosApps(lista: List<String>, consola: String): List<Juego> = lista.mapNotNull { pkg ->
        val nombre = Lanzador.nombreApp(app, pkg) ?: return@mapNotNull null
        val icono = Uri.parse("app://$pkg")
        Juego("$consola/$pkg", consola, nombre, pkg, icono, pkg, portada = icono)
    }.sortedBy { it.titulo.lowercase() }

    internal fun todosCrudos(): List<Juego> =
        crudos + juegosApps(estado.value.ports, Consolas.PORTS) + juegosApps(estado.value.android, Consolas.ANDROID)

    /** Junta lo escaneado, los ports y lo descargado, y lo muestra. */
    internal fun publicarJuegos() {
        val datos = estado.value.datos
        val lista = Descargas.aplicar(todosCrudos(), medios, estado.value.titulosBonitos).map { j ->
            // Lo que la persona personalizó en la ficha manda sobre todo lo demás.
            val d = datos[j.clave] ?: return@map j
            j.copy(
                titulo = d.titulo ?: j.titulo,
                portada = d.portada?.let(Uri::parse) ?: j.portada,
                arte = d.arte?.let(Uri::parse) ?: j.arte,
                logo = d.logo?.let(Uri::parse) ?: j.logo,
            )
        }
        actualizar { e ->
            val sigue = lista.any { it.clave == e.juegoSel }
            val consolaSigue = e.consolaSel == "todo" || lista.any { it.consolaId == e.consolaSel }
            e.copy(
                juegos = lista,
                juegoSel = if (sigue) e.juegoSel else null,
                consolaSel = if (consolaSigue) e.consolaSel else "todo",
            )
        }
    }

    fun carpetaElegida(uri: Uri, tipo: Carpeta) {
        val cr = app.contentResolver
        try {
            cr.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } catch (e: SecurityException) {
            try {
                cr.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
            }
        }
        when (tipo) {
            Carpeta.JUEGOS -> {
                // Se suma a las demás carpetas (sirve para tener juegos en la memoria y en la SD).
                val antes = almacen.carpetas()
                if (antes.none { it.uri == uri }) almacen.guardarCarpetas(antes + CarpetaJuegos(uri, null))
                actualizar { it.copy(tieneCarpeta = true, indice = if (it.panel == Panel.CARPETAS) almacen.carpetas().size - 1 else 0, carpetas = almacen.carpetas()) }
                escanear()
                revisarCarpetaNueva(uri)
            }
            Carpeta.MEDIA -> {
                almacen.media = uri
                actualizar { it.copy(tieneMedia = true) }
                avisar("Imágenes de ES-DE conectadas")
                escanear()
            }
            Carpeta.MUSICA -> {
                almacen.carpetaMusica = uri
                almacen.pista = "mia"
                actualizar { it.copy(carpetaMusica = uri, pista = "mia", musica = true) }
                almacen.musica = true
                scope.launch {
                    val n = withContext(Dispatchers.IO) { Musica.listarCanciones(uri).size }
                    avisar(if (n == 0) "No encontré canciones en esa carpeta" else "Tu música: $n canciones")
                }
            }
        }
    }

    fun elegirModo(modo: String) {
        almacen.modoPantallas = modo
        almacen.modoElegido = true
        actualizar { it.copy(modoPantallas = modo, modoElegido = true) }
        Inicio.inicioAbajo(app, modo != "una_libre")
    }

    private fun nombreModo(m: String) = when (m) {
        "una_negra" -> "Una · abajo en negro"
        "una_libre" -> "Una · abajo libre"
        else -> "Dos pantallas"
    }

    fun terminarConfiguracion() {
        if (!estado.value.modoElegido) elegirModo(if (estado.value.dosDisponibles) "dos" else "una_negra")
        almacen.configurado = true
        actualizar { it.copy(configurado = true, panel = Panel.NINGUNO, indice = 0) }
        // Para que Díptico sea siempre lo primero: se ofrece como app de inicio de Android.
        scope.launch {
            delay(1800)
            if (!Inicio.esPredeterminado(app)) { avisar("Elige Díptico como tu inicio para que siempre vuelvas aquí"); hacerInicio() }
        }
    }

    fun avisar(texto: String) {
        actualizar { it.copy(aviso = texto) }
        avisoJob?.cancel()
        avisoJob = scope.launch {
            delay(3800)
            actualizar { it.copy(aviso = null) }
        }
    }

    // ---------- Ajustes sueltos ----------

    private fun fijarTema(oscuro: Boolean) {
        almacen.oscuro = oscuro
        actualizar { it.copy(oscuro = oscuro) }
    }

    fun alternarTema() = fijarTema(!estado.value.oscuro)

    private val tamanos = listOf(1f, 1.15f, 1.3f)
    private fun nombreTam(t: Float) = when {
        t >= 1.29f -> "Muy grande"
        t >= 1.14f -> "Grande"
        else -> "Normal"
    }

    private fun fijarTam(t: Float) {
        almacen.tamTexto = t
        actualizar { it.copy(tamTexto = t) }
    }

    fun intercambiar() {
        val e = estado.value
        if (e.unaPantalla) {
            avisar("Intercambiar sirve cuando usas las dos pantallas")
            return
        }
        val v = !e.intercambiar
        almacen.intercambiar = v
        actualizar { it.copy(intercambiar = v) }
        avisar(if (v) "Pantallas intercambiadas" else "Pantallas en su lugar")
    }

    private fun fijarMusica(si: Boolean) {
        almacen.musica = si
        actualizar { it.copy(musica = si) }
    }

    private fun fijarPista(p: String) {
        if (p == "mia" && estado.value.carpetaMusica == null) {
            pedirCarpeta?.invoke(Carpeta.MUSICA)
            return
        }
        almacen.pista = p
        // Elegir una pista a mano apaga la música según el tema.
        almacen.guardarBool("musicaTema", false)
        actualizar { it.copy(pista = p, musica = true, musicaTema = false) }
        almacen.musica = true
    }

    private fun nombrePista(p: String) = when (p) {
        "mezcla" -> "Mezcla de las tres"
        "mia" -> "Tu música"
        else -> Musica.pistas.firstOrNull { it.id == p }?.nombre ?: "Bisagra"
    }

    private val volumenes = listOf(0.35f, 0.6f, 0.9f)
    private fun nombreVolumen(v: Float) = when {
        v >= 0.85f -> "Alto"
        v >= 0.55f -> "Medio"
        else -> "Bajo"
    }

    // ---------- Tutorial guiado ----------

    fun pasosTuto(e: Estado = estado.value): List<PasoTuto> = buildList {
        add(PasoTuto.BIENVENIDA)
        add(PasoTuto.PANTALLAS)
        add(PasoTuto.TEXTO)
        if (e.dosDisponibles && e.modoPantallas == "dos") {
            add(PasoTuto.DISPOSICION)
            add(PasoTuto.JUEGOS)
        }
        add(PasoTuto.CARPETA)
        add(PasoTuto.EMULADORES)
        add(PasoTuto.PORTS)
        add(PasoTuto.PORTADAS)
        add(PasoTuto.MUSICA)
        add(PasoTuto.APARIENCIA)
        add(PasoTuto.CONTROLES)
        add(PasoTuto.LISTO)
    }

    private val ordenPistas = listOf("bisagra", "cartucho", "medianoche", "mezcla")

    private fun indiceInicial(p: PasoTuto, e: Estado): Int = when (p) {
        PasoTuto.PANTALLAS -> if (!e.modoElegido) (if (e.dosDisponibles) 0 else 1)
            else listOf("dos", "una_negra", "una_libre").indexOf(e.modoPantallas).coerceAtLeast(0)
        PasoTuto.TEXTO -> tamanos.indexOfFirst { kotlin.math.abs(it - e.tamTexto) < 0.01f }.coerceAtLeast(0)
        PasoTuto.DISPOSICION -> if (e.intercambiar) 1 else 0
        PasoTuto.JUEGOS -> if (e.pantallaJuegos == "abajo") 1 else 0
        PasoTuto.MUSICA -> if (!e.musica) 4 else ordenPistas.indexOf(e.pista).let { if (it < 0) 5 else it }
        PasoTuto.APARIENCIA -> if (e.oscuro) 0 else 1
        else -> 0
    }

    private fun irAPaso(p: PasoTuto) {
        actualizar { it.copy(pasoTuto = p, indice = indiceInicial(p, it), panel = Panel.NINGUNO) }
    }

    fun siguientePaso() {
        val pasos = pasosTuto()
        val i = pasos.indexOf(estado.value.pasoTuto)
        if (i < 0 || i >= pasos.size - 1) terminarConfiguracion() else irAPaso(pasos[i + 1])
    }

    fun pasoAnterior() {
        val pasos = pasosTuto()
        val i = pasos.indexOf(estado.value.pasoTuto)
        if (i > 0) irAPaso(pasos[i - 1])
    }

    fun marcarProbado(boton: String) {
        if (estado.value.pasoTuto == PasoTuto.CONTROLES) actualizar { it.copy(probados = it.probados + boton) }
    }

    fun reiniciarTutorial() {
        almacen.configurado = false
        actualizar {
            it.copy(
                configurado = false, pasoTuto = PasoTuto.BIENVENIDA, indice = 0, panel = Panel.NINGUNO,
                probados = emptySet(), intercambiar = it.intercambiar,
            )
        }
    }

    fun opcionesTuto(): List<Opcion> {
        val e = estado.value
        return when (e.pasoTuto) {
            PasoTuto.BIENVENIDA -> listOf(
                Opcion("¡Comencemos!", "Toma unos 2 minutos", Icono.JUGAR) { siguientePaso() },
                Opcion("Idioma", Idioma.IDIOMAS.first { it.first == e.idioma }.second, Icono.IDIOMA) {
                    val l = Idioma.IDIOMAS.map { it.first }
                    cambiarIdioma(l[(l.indexOf(estado.value.idioma) + 1).mod(l.size)])
                },
            )
            PasoTuto.PANTALLAS -> listOf(
                Opcion(
                    "Dos pantallas",
                    if (e.dosDisponibles) "Arriba el juego, abajo eliges" else "No detecto una segunda pantalla",
                    Icono.PANTALLAS, apagada = !e.dosDisponibles,
                ) {
                    if (estado.value.dosDisponibles) {
                        elegirModo("dos"); siguientePaso()
                    } else {
                        avisar("Este equipo tiene una sola pantalla. Elige \"Una pantalla\".")
                    }
                },
                Opcion("Una pantalla", if (e.dosDisponibles) "La de abajo queda en negro" else "Todo en tu pantalla", Icono.UNA) {
                    elegirModo("una_negra"); siguientePaso()
                },
                Opcion("Una pantalla, abajo libre", "Para usar otra app abajo", Icono.LIBRE, apagada = !e.dosDisponibles) {
                    if (estado.value.dosDisponibles) {
                        elegirModo("una_libre"); siguientePaso()
                    } else {
                        avisar("Este equipo tiene una sola pantalla. Elige \"Una pantalla\".")
                    }
                },
            )
            PasoTuto.TEXTO -> listOf(
                Opcion("Normal", "Cabe más en la pantalla", Icono.TEXTO) { fijarTam(1f); siguientePaso() },
                Opcion("Grande", "Más fácil de leer", Icono.TEXTO) { fijarTam(1.15f); siguientePaso() },
                Opcion("Muy grande", "Para leer de lejos", Icono.TEXTO) { fijarTam(1.3f); siguientePaso() },
            )
            PasoTuto.DISPOSICION -> listOf(
                Opcion("Juego arriba, estante abajo", "Recomendado", Icono.ARRIBA) {
                    almacen.intercambiar = false
                    actualizar { it.copy(intercambiar = false) }
                    siguientePaso()
                },
                Opcion("Estante arriba, juego abajo", "Lo puedes cambiar cuando quieras", Icono.INTERCAMBIAR) {
                    almacen.intercambiar = true
                    actualizar { it.copy(intercambiar = true) }
                    siguientePaso()
                },
            )
            PasoTuto.JUEGOS -> listOf(
                Opcion("En la pantalla de arriba", "Recomendado: es la grande", Icono.ARRIBA) {
                    almacen.pantallaJuegos = "arriba"
                    actualizar { it.copy(pantallaJuegos = "arriba") }
                    siguientePaso()
                },
                Opcion("En la pantalla de abajo", "Útil para juegos de una pantalla chica", Icono.ABAJO) {
                    almacen.pantallaJuegos = "abajo"
                    actualizar { it.copy(pantallaJuegos = "abajo") }
                    siguientePaso()
                },
            )
            PasoTuto.CARPETA -> when {
                !e.tieneCarpeta -> listOf(
                    Opcion("Elegir carpeta de juegos", "La que tiene snes, psx, nds…", Icono.CARPETA) { pedirCarpeta?.invoke(Carpeta.JUEGOS) },
                    Opcion("Hacerlo después", "Puedes elegirla desde el menú", Icono.CONTINUAR) { siguientePaso() },
                )
                e.escaneando -> emptyList()
                e.juegos.isEmpty() -> listOf(
                    Opcion("Elegir otra carpeta", icono = Icono.CARPETA) { pedirCarpeta?.invoke(Carpeta.JUEGOS) },
                    Opcion("Continuar sin juegos", icono = Icono.CONTINUAR) { siguientePaso() },
                )
                else -> listOf(
                    Opcion("Continuar", "${e.juegos.size} juegos encontrados", Icono.CHECK) { siguientePaso() },
                    Opcion("Elegir otra carpeta", icono = Icono.CARPETA) { pedirCarpeta?.invoke(Carpeta.JUEGOS) },
                )
            }
            PasoTuto.EMULADORES -> listOf(Opcion("Continuar", icono = Icono.CHECK) { siguientePaso() }) + e.consolasEmu.map { c ->
                Opcion(
                    c.bonito,
                    if (emuladorListo(c)) nombreEmulador(c) else "Falta: ${c.sugerido}",
                    Icono.EMULADOR,
                ) { ciclarEmulador(c) }
            }
            PasoTuto.PORTS -> listOf(
                Opcion(
                    "Elegir mis ports",
                    if (e.ports.isEmpty()) "Ship of Harkinian, Doom, Mario 64…" else "${e.ports.size} elegidos ✓",
                    Icono.APPS,
                ) { abrirPorts() },
                Opcion(
                    "Juegos de Android",
                    if (e.android.isEmpty()) "Genshin, Minecraft y más" else "${e.android.size} encontrados ✓",
                    Icono.MOVIL,
                ) { abrirPorts(Consolas.ANDROID) },
                Opcion(if (e.ports.isEmpty()) "Ahora no" else "Continuar", "Lo encuentras en el menú, en Biblioteca", Icono.CONTINUAR) {
                    siguientePaso()
                },
            )
            PasoTuto.PORTADAS -> listOf(
                Opcion(
                    if (e.descarga != null) "Descargando…" else "Descargar portadas ahora",
                    when {
                        e.descarga != null -> "${e.descarga.hechos} de ${e.descarga.total} · sigue sola de fondo"
                        e.juegos.isEmpty() -> "Primero necesitas juegos"
                        else -> "Gratis, desde libretro · ${e.sinPortada} sin portada"
                    },
                    Icono.DESCARGA,
                ) {
                    if (estado.value.juegos.isNotEmpty()) {
                        if (estado.value.descarga == null) descargarPortadas(todo = false)
                        siguientePaso()
                    } else {
                        avisar("Primero elige tu carpeta de juegos")
                    }
                },
                Opcion(
                    "Clave de SteamGridDB",
                    if (e.claveSgdb.isNotBlank()) "Conectada ✓ · logos y arte grande" else "Opcional · logos y arte grande",
                    Icono.CLAVE,
                ) { abrirClave() },
                Opcion("Imágenes de ES-DE", if (e.tieneMedia) "Conectadas ✓" else "Si ya usas ES-DE", Icono.CARPETA) {
                    pedirCarpeta?.invoke(Carpeta.MEDIA)
                },
                Opcion("Después", "Lo encuentras en el menú, en Portadas", Icono.CONTINUAR) { siguientePaso() },
            )
            PasoTuto.MUSICA -> Musica.pistas.take(3).map { p ->
                Opcion(p.nombre, p.estilo, Icono.MUSICA) { fijarPista(p.id); siguientePaso() }
            } + listOf(
                Opcion("Mezcla", "Las tres, una tras otra", Icono.ESTRELLA) { fijarPista("mezcla"); siguientePaso() },
                Opcion("Sin música", "Silencio total", Icono.SILENCIO) { fijarMusica(false); siguientePaso() },
                Opcion(
                    "Mi propia música",
                    if (e.carpetaMusica != null) "Carpeta elegida ✓" else "Elige una carpeta con canciones",
                    Icono.CARPETA,
                ) {
                    if (estado.value.carpetaMusica != null) {
                        fijarPista("mia"); siguientePaso()
                    } else {
                        pedirCarpeta?.invoke(Carpeta.MUSICA)
                    }
                },
            )
            PasoTuto.APARIENCIA -> listOf(
                Opcion("Modo oscuro", "Ahorra batería en pantallas OLED", Icono.TEMA) { fijarTema(true); siguientePaso() },
                Opcion("Modo claro", "Más luminoso", Icono.TEMA) { fijarTema(false); siguientePaso() },
            )
            PasoTuto.CONTROLES -> listOf(Opcion("Continuar", "Cuando quieras", Icono.CHECK) { siguientePaso() })
            PasoTuto.LISTO -> listOf(Opcion("¡Empezar a jugar!", icono = Icono.JUGAR) { terminarConfiguracion() })
        }
    }

    /** En algunos pasos el cambio se ve (y se oye) en vivo mientras eliges. */
    private fun vistaPrevia(paso: PasoTuto, i: Int) {
        when (paso) {
            PasoTuto.APARIENCIA -> fijarTema(i == 0)
            PasoTuto.TEXTO -> tamanos.getOrNull(i)?.let { fijarTam(it) }
            PasoTuto.MUSICA -> when (i) {
                in 0..3 -> {
                    val p = ordenPistas[i]
                    almacen.pista = p
                    almacen.musica = true
                    actualizar { it.copy(pista = p, musica = true) }
                }
                4 -> fijarMusica(false)
                else -> if (estado.value.carpetaMusica != null) {
                    almacen.pista = "mia"
                    actualizar { it.copy(pista = "mia", musica = true) }
                }
            }
            else -> Unit
        }
    }

    // ---------- Navegación ----------

    fun moverJuego(delta: Int) {
        val e = estado.value
        val v = e.visibles
        if (v.isEmpty()) return
        val i = (e.indiceSel + delta).coerceIn(0, v.size - 1)
        actualizar { it.copy(juegoSel = v[i].clave) }
    }

    fun elegirJuego(clave: String) {
        actualizar { it.copy(juegoSel = clave) }
    }

    fun cambiarConsola(delta: Int) {
        val e = estado.value
        val tabs = e.pestanas
        if (tabs.isEmpty()) return
        val i = tabs.indexOf(e.consolaSel).coerceAtLeast(0)
        elegirConsola(tabs[(i + delta + tabs.size) % tabs.size])
    }

    fun elegirConsola(id: String) {
        actualizar { e ->
            val vis = if (id == "todo") e.juegos else e.juegos.filter { it.consolaId == id }
            val sel = vis.firstOrNull { it.clave == e.juegoSel } ?: vis.firstOrNull()
            e.copy(consolaSel = id, juegoSel = sel?.clave)
        }
    }

    fun sorprendeme() {
        val e = estado.value
        val otros = e.visibles.filter { it.clave != e.seleccionado?.clave }
        if (otros.isEmpty()) return
        elegirJuego(otros.random().clave)
    }

    fun continuar() {
        val e = estado.value
        val ultimo = e.datos.entries
            .filter { it.value.ultimo > 0 && e.juegos.any { j -> j.clave == it.key } }
            .maxByOrNull { it.value.ultimo }?.key
        if (ultimo == null) {
            avisar("Todavía no has jugado nada con Díptico")
            return
        }
        actualizar { it.copy(consolaSel = "todo", juegoSel = ultimo) }
    }

    // ---------- Jugar ----------

    fun jugar(pantalla: String? = null, conLupaLista: Boolean = false) {
        val e = estado.value
        val juego = e.seleccionado ?: return
        val consola = Consolas.porId(juego.consolaId) ?: return
        val destino = pantalla ?: e.datos[juego.clave]?.pantalla ?: if (e.unaPantalla) "arriba" else e.pantallaJuegos
        val inferior = Pantallas.inferior(app)
        // Lupa: antes de abrir el juego se pide el permiso para copiar la pantalla de arriba (una vez).
        if (!conLupaLista && e.datos[juego.clave]?.lupa == true && destino == "arriba" && inferior != null && !e.unaPantalla &&
            !cl.nikalbazu.diptico.lupa.LupaServicio.activa
        ) {
            pantallaPendiente = pantalla
            pedirPermisoLupa(luegoJugar = true)
            return
        }
        val displayId = if (destino == "abajo" && inferior != null) inferior.displayId else Display.DEFAULT_DISPLAY
        val error = if (juego.uri.scheme == "app") Lanzador.lanzarApp(app, juego.archivo, juego.titulo, displayId)
        else Lanzador.lanzar(app, juego, consola, e.datos[juego.clave]?.emulador ?: e.emuladores[consola.id], displayId, raizDe(juego))
        if (error != null) {
            avisar(error)
            return
        }
        actualizar {
            it.copy(
                sesion = Sesion(juego.clave, System.currentTimeMillis(), displayId, consola.dosPantallas),
                panel = Panel.NINGUNO,
                despierta = false,
                burbujaAbierta = false,
                burbujaAlerta = false,
            )
        }
        // La pantalla del compañero queda encendida 5 segundos y después se atenúa.
        despertar()
        // Bisa acompaña el juego. Si se nota que lo reintenta mucho, ofrece ayuda.
        if (e.bisaEnJuego) {
            val atascado = Asistente.reintentos(juego.clave) >= 2
            val sinPantallaLibre = consola.dosPantallas || estado.value.unaPantalla || inferior == null
            cl.nikalbazu.diptico.bisa.BurbujaBisa.acompanar(app, juego.titulo, atascado, sinPantallaLibre)
            if (atascado && !sinPantallaLibre) prepararAyuda()
        }
    }

    // ---------- Lupa ----------

    private var pantallaPendiente: String? = null

    fun pedirPermisoLupa(luegoJugar: Boolean) {
        try {
            app.startActivity(
                Intent(app, cl.nikalbazu.diptico.lupa.PermisoLupaActivity::class.java)
                    .putExtra("luegoJugar", luegoJugar).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: Exception) {
            avisar("No pude pedir el permiso de la lupa")
        }
    }

    /** Android dio permiso: si estaba por abrir un juego, ahora sí. */
    fun lupaPermitida(luegoJugar: Boolean) {
        if (luegoJugar) scope.launch { delay(350); jugar(pantallaPendiente, conLupaLista = true) }
    }

    fun lupaCambio(activa: Boolean) = actualizar { it.copy(lupaActiva = activa) }

    fun zonaLupa(clave: String): android.graphics.RectF {
        val s = estado.value.datos[clave]?.zonaLupa?.split(",")?.mapNotNull { it.toFloatOrNull() }
        return if (s != null && s.size == 4) android.graphics.RectF(s[0], s[1], s[0] + s[2], s[1] + s[3])
        else android.graphics.RectF(0.79f, 0.02f, 0.99f, 0.36f)
    }

    fun guardarZonaLupa(clave: String, r: android.graphics.RectF) {
        cambiarDatos(clave) { it.copy(zonaLupa = "${r.left},${r.top},${r.width()},${r.height()}") }
    }

    /** Enciende o apaga la lupa para un juego (si se está jugando, se pide el permiso al tiro). */
    fun alternarLupaJuego(clave: String, encender: Boolean = estado.value.datos[clave]?.lupa != true) {
        cambiarDatos(clave) { it.copy(lupa = encender) }
        if (encender) {
            if (estado.value.sesion != null && !cl.nikalbazu.diptico.lupa.LupaServicio.activa) pedirPermisoLupa(false)
            avisar("Lupa encendida: al jugar, la pantalla de abajo amplía una parte de la de arriba")
        } else {
            cl.nikalbazu.diptico.lupa.LupaServicio.detener(app)
            avisar("Lupa apagada")
        }
    }

    // ---------- Galerías: temas, música y colores ----------

    /** Un elemento de una galería. [color] es la muestra (en colores) y [tema] la vista previa (en temas). */
    data class ItemGaleria(val id: String, val nombre: String, val detalle: String, val enUso: Boolean, val tema: Tema? = null, val color: Long = 0L, val accion: () -> Unit)

    fun abrirGaleria(tipo: String) {
        actualizar { it.copy(panel = Panel.GALERIA, galeria = tipo, galeriaPestana = 0, indice = 0) }
        val i = itemsGaleria().indexOfFirst { it.enUso }
        if (i > 0) actualizar { it.copy(indice = i) }
    }

    fun pestanasGaleria(tipo: String = estado.value.galeria): List<String> = when (tipo) {
        "temas" -> listOf("Colores", "Fiestas del mundo", "Tus temas")
        "colores" -> listOf("Principal", "Secundario", "Detalles", "Fondo", "Letras")
        "ia" -> listOf("El cerebro de Bisa")
        else -> listOf("Canciones")
    }

    fun cambiarPestanaGaleria(d: Int) {
        val n = pestanasGaleria().size
        actualizar { it.copy(galeriaPestana = (it.galeriaPestana + d).mod(n), indice = 0) }
        val i = itemsGaleria().indexOfFirst { it.enUso }
        if (i > 0) actualizar { it.copy(indice = i) }
    }

    /** Muestras para "Mis colores": 12 tonos en 5 luces, más grises. */
    val MUESTRAS_COLOR: List<Long> = buildList {
        val tonos = listOf(0f, 18f, 35f, 50f, 90f, 140f, 170f, 195f, 215f, 245f, 270f, 300f, 330f)
        for (v in listOf(0.95f to 0.55f, 0.85f to 0.85f, 0.75f to 1f, 0.55f to 0.65f, 0.35f to 0.35f)) {
            for (h in tonos) add(android.graphics.Color.HSVToColor(floatArrayOf(h, v.first, v.second)).toLong() and 0xFFFFFFFFL)
        }
        listOf(0xFFFFFFFF, 0xFFE5E5EA, 0xFFB0B0B8, 0xFF6E6E78, 0xFF3A3A44, 0xFF1C1C24, 0xFF0B0A12, 0xFF000000).forEach { add(it) }
    }

    fun itemsGaleria(e: Estado = estado.value): List<ItemGaleria> {
        val actual = cl.nikalbazu.diptico.ui.Estilo.tema
        return when (e.galeria) {
            "temas" -> when (e.galeriaPestana) {
                0 -> Temas.incluidos.map { t -> ItemGaleria(t.id, t.nombre, if (t.decor != "nada") Temas.nombreDecor(t.decor) else "", actual.id == t.id, t) { elegirTema(t.id) } }
                1 -> Temas.fiestas.map { f -> ItemGaleria(f.tema.id, f.nombre, f.donde, actual.id == f.tema.id, f.tema) { elegirTema(f.tema.id) } }
                else -> e.temasPropios.map { t -> ItemGaleria(t.id, t.nombre, "Creado por ti", actual.id == t.id, t) { elegirTema(t.id) } } +
                    ItemGaleria("_ia", "Crear con IA", if (llaveNube(e) == null) "Necesita una IA en la nube" else "Escribe tu idea", false) {
                        if (llaveNube() == null) pedirIANube("Crear temas") else abrirTexto("ia_pedido")
                    } +
                    ItemGaleria("_colores", "Mis colores", "Toda la interfaz", false) { abrirGaleria("colores") }
            }
            "colores" -> {
                val mio = e.temasPropios.firstOrNull { it.id == "mio" } ?: actual
                val valor = when (e.galeriaPestana) { 0 -> mio.acento; 1 -> mio.acento2; 2 -> mio.acento3; 3 -> mio.fondo; else -> mio.tinta }
                val auto = if (e.galeriaPestana >= 3) listOf(ItemGaleria("_auto", "Automático", "", valor == 0L, color = 0L) { ponerColor(0L) }) else emptyList()
                auto + MUESTRAS_COLOR.map { c -> ItemGaleria(c.toString(), "", "", c == valor, color = c) { ponerColor(c) } }
            }
            "ia" -> {
                fun estadoNube(p: String) = if (claveDe(p, e).isBlank()) "Falta tu clave" else "Clave guardada ✓"
                listOf(
                    ItemGaleria("auto", "Automático", "Usa la de tu consola si está; si no, la primera con clave", e.motorIA == "auto") { elegirIA("auto") },
                    ItemGaleria("local", "En tu consola", if (e.iaLocalLista) "Lista ✓ · gratis y sin internet" else "Bajar 1,6 GB · gratis y sin internet", e.motorIA == "local") { elegirIA("local") },
                    ItemGaleria("gemini", "Gemini", "Google · gratis con límite diario · " + estadoNube("gemini"), e.motorIA == "gemini") { elegirIA("gemini") },
                    ItemGaleria("openai", "ChatGPT", "OpenAI · pagado por uso · " + estadoNube("openai"), e.motorIA == "openai") { elegirIA("openai") },
                    ItemGaleria("claude", "Claude", "Anthropic · pagado por uso · " + estadoNube("claude"), e.motorIA == "claude") { elegirIA("claude") },
                )
            }
            else -> {
                val sonando = if (e.musicaTema && e.pista != "mia") e.pistaTema else e.pista
                Musica.pistas.map { p -> ItemGaleria(p.id, p.nombre, p.estilo, e.musica && sonando == p.id) { fijarPista(p.id) } } +
                    ItemGaleria("mezcla", "Mezcla", "Todas, una tras otra", e.musica && sonando == "mezcla") { fijarPista("mezcla") } +
                    ItemGaleria("_tema", "Según el tema", "Cada tema trae su música", e.musica && e.musicaTema) {
                        almacen.guardarBool("musicaTema", true); actualizar { it.copy(musicaTema = true) }
                        if (!estado.value.musica) fijarMusica(true)
                    } +
                    ItemGaleria("_mia", "Mi propia música", if (e.carpetaMusica != null) "Tu carpeta" else "Elegir carpeta", e.musica && e.pista == "mia") { pedirCarpeta?.invoke(Carpeta.MUSICA) } +
                    ItemGaleria("_no", "Sin música", "Silencio total", !e.musica) { fijarMusica(false) }
            }
        }
    }

    /** Pone un color en el espacio elegido de "Mi tema" y lo aplica a toda la interfaz. */
    private fun ponerColor(c: Long) {
        val p = estado.value.galeriaPestana
        editarMiTema {
            when (p) {
                0 -> it.copy(acento = c); 1 -> it.copy(acento2 = c); 2 -> it.copy(acento3 = c)
                3 -> it.copy(fondo = c); else -> it.copy(tinta = c)
            }
        }
    }

    /** Deja a Bisa lista para ayudar con el juego en curso (en la pantalla libre o en la burbuja). */
    fun prepararAyuda() {
        val e = estado.value
        val s = e.sesion
        val juego = s?.let { ses -> e.juegos.firstOrNull { it.clave == ses.clave } } ?: e.seleccionado
        val titulo = juego?.titulo ?: "tu juego"
        val r = juego?.let { Asistente.reintentos(it.clave) } ?: 0
        val saludo = MensajeChat(
            "ia",
            if (r >= 2) "Veo que llevas varios intentos seguidos en $titulo. ¡Tranqui, nos pasa a todos! ¿Te doy una pista suave o prefieres la solución?"
            else "¡Hola! Estoy aquí mientras juegas $titulo. Si algo te cuesta, dime en qué parte vas.",
            listOf(
                AccionChat("preguntar", "Estoy atascado en $titulo. Dame una pista suave.", "Dame una pista suave"),
                AccionChat("preguntar", "En $titulo me matan una y otra vez en la misma parte. ¿Qué hago?", "Me matan siempre en lo mismo"),
                AccionChat("preguntar", "Dame un buen truco o consejo para $titulo.", "Un truco para este juego"),
            ),
        )
        actualizar { it.copy(panel = Panel.ASISTENTE, chat = if (it.chat.lastOrNull()?.texto == saludo.texto) it.chat else it.chat + saludo, indice = 1, despierta = true) }
    }

    fun burbujaExpandida(abierta: Boolean, alerta: Boolean) {
        if (abierta && estado.value.chat.isEmpty()) prepararAyuda()
        actualizar { it.copy(burbujaAbierta = abierta, burbujaAlerta = alerta) }
    }

    /** Una pantalla de Díptico volvió al frente: si ahí corría un juego, la sesión terminó. */
    fun alVolver(displayId: Int) {
        val s = estado.value.sesion ?: return
        if (s.displayId != displayId) return
        // Recién lanzado: la pantalla todavía no se tapó. Lo ignoramos.
        if (System.currentTimeMillis() - s.inicio < 4000) return
        cerrarSesion(preguntar = true)
    }

    fun cerrarSesion(preguntar: Boolean) {
        val s = estado.value.sesion ?: return
        val duracion = (System.currentTimeMillis() - s.inicio) / 1000
        Asistente.intentos += Triple(s.clave, s.inicio, System.currentTimeMillis())
        if (Asistente.intentos.size > 40) Asistente.intentos.removeAt(0)
        cl.nikalbazu.diptico.bisa.BurbujaBisa.detener(app)
        cl.nikalbazu.diptico.lupa.LupaServicio.detener(app)
        val datos = estado.value.datos.toMutableMap()
        val d = datos[s.clave] ?: DatosJuego()
        datos[s.clave] = d.copy(
            tiempo = d.tiempo + if (duracion >= 15) duracion else 0,
            ultimo = System.currentTimeMillis(),
            sesiones = d.sesiones + if (duracion >= 15) 1 else 0,
        )
        almacen.guardarDatos(datos)
        val hacerPregunta = preguntar && duracion >= 60
        actualizar {
            it.copy(
                sesion = null,
                despierta = false,
                datos = datos,
                juegoSel = s.clave,
                panel = if (hacerPregunta) Panel.PASO else it.panel,
                borrador = if (hacerPregunta) d.paso else it.borrador,
                pregunta = hacerPregunta,
                teclaFila = if (hacerPregunta) 2 else it.teclaFila,
                cursor = if (hacerPregunta) -1 else it.cursor,
                ancla = if (hacerPregunta) null else it.ancla,
                teclaCol = if (hacerPregunta) 0 else it.teclaCol,
                mayus = if (hacerPregunta) d.paso.isEmpty() else it.mayus,
            )
        }
    }

    fun cambiarTamLogo(factor: Float) {
        val v = (estado.value.tamLogo * factor).coerceIn(0.6f, 1.8f)
        actualizar { it.copy(tamLogo = v) }
        almacen.guardarTexto("tamLogo", v.toString())
    }

    /** Abre la ficha de un juego en una pestaña (0 resumen, 1 historia, 2 guías, 3 personalizar, 4 logros). */
    fun abrirFichaDe(clave: String, pestana: Int) {
        actualizar { it.copy(juegoSel = clave, consolaSel = "todo") }
        abrirFicha()
        elegirPestanaFicha(pestana)
    }

    val NIVELES_ATENUACION = listOf(0.01f, 0.04f, 0.12f, 0.25f, 1f)
    fun nombreAtenuacion(v: Float) = when {
        v >= 1f -> "Sin atenuar"; v >= 0.2f -> "Suave"; v >= 0.1f -> "Media"; v >= 0.03f -> "Oscura"; else -> "Muy oscura"
    }

    fun despertar() {
        actualizar { it.copy(despierta = true) }
        despiertaJob?.cancel()
        despiertaJob = scope.launch {
            delay(5000)
            actualizar { it.copy(despierta = false) }
        }
    }

    fun atenuar() {
        despiertaJob?.cancel()
        actualizar { it.copy(despierta = false) }
    }

    // ---------- Paneles ----------

    fun abrirRapido() {
        if (estado.value.seleccionado == null) return
        actualizar { it.copy(panel = Panel.RAPIDO, indice = 0) }
    }

    fun abrirPaso() {
        val clave = estado.value.seleccionado?.clave ?: return
        actualizar { it.copy(panel = Panel.PASO, borrador = it.datos[clave]?.paso ?: "", pregunta = false, teclaFila = 2, teclaCol = 0, simbolos = false, cursor = -1, ancla = null, mayus = it.datos[clave]?.paso.isNullOrEmpty()) }
    }

    fun abrirMenu(seccion: Seccion? = null) {
        actualizar {
            it.copy(
                panel = Panel.MENU, seccion = seccion ?: it.seccion,
                enOpciones = seccion != null, indice = 0,
            )
        }
    }

    /** Lista de apps instaladas para marcar cuáles son ports. */
    fun abrirPorts(destino: String = Consolas.PORTS) {
        actualizar { it.copy(panel = Panel.PORTS, indice = 0, cargandoApps = true, appsDestino = destino) }
        scope.launch {
            val todas = withContext(Dispatchers.IO) { try { Lanzador.apps(app) } catch (e: Exception) { emptyList() } }
            // Para juegos de Android, primero los que Android marca como juego.
            val lista = if (destino == Consolas.ANDROID) todas.filter { it.esJuego } + todas.filter { !it.esJuego } else todas
            // Primero las ya elegidas, después el resto.
            val elegidas = (if (destino == Consolas.ANDROID) estado.value.android else estado.value.ports).toSet()
            val orden = lista.filter { it.paquete in elegidas } + lista.filter { it.paquete !in elegidas }
            actualizar { it.copy(apps = orden, cargandoApps = false) }
        }
    }

    fun alternarPort(paquete: String) {
        val e = estado.value
        if (e.appsDestino == Consolas.ANDROID) {
            val nuevo = if (paquete in e.android) e.android - paquete else e.android + paquete
            almacen.android = nuevo
            actualizar { it.copy(android = nuevo) }
        } else {
            val nuevo = if (paquete in e.ports) e.ports - paquete else e.ports + paquete
            almacen.ports = nuevo
            actualizar { it.copy(ports = nuevo) }
        }
        publicarJuegos()
    }

    /** Agrega los juegos de Android que el sistema reconoce como juego. */
    fun detectarAndroid(silencioso: Boolean = false) {
        trabajo("android", "Buscando juegos de Android")
        scope.launch {
            val juegos = withContext(Dispatchers.IO) { try { Lanzador.juegosAndroid(app).map { it.paquete } } catch (e: Exception) { emptyList() } }
            val antes = estado.value.android
            val nuevo = (antes + juegos.filter { it !in antes && it !in estado.value.ports })
            almacen.android = nuevo
            actualizar { it.copy(android = nuevo) }
            publicarJuegos()
            terminarTrabajo("android")
            if (!silencioso) {
                val n = nuevo.size - antes.size
                avisar(if (n > 0) "Agregué $n juegos de Android" else "No encontré juegos de Android nuevos")
            }
        }
    }

    fun abrirClave() = abrirTexto("sgdb", estado.value.claveSgdb)

    /** Abre el cuadro de texto para [para] ("sgdb", "ia_clave", "ia_pedido", "titulo", "notas"). */
    fun abrirTexto(para: String, inicial: String = "") {
        actualizar { it.copy(panel = Panel.CLAVE, textoPara = para, borrador = inicial, teclaFila = 2, teclaCol = 0, simbolos = false, cursor = -1, ancla = null, mayus = inicial.isEmpty() && para != "sgdb" && !para.startsWith("ia_clave") && !para.startsWith("ra_")) }
    }

    fun cerrarPanel() {
        val e = estado.value
        // Desde la clave, se vuelve a donde estabas (menú o tutorial).
        if (e.panel == Panel.CLAVE && (e.textoPara == "titulo" || e.textoPara == "notas")) {
            actualizar { it.copy(panel = Panel.FICHA) }
            return
        }
        if (e.panel == Panel.CLAVE && e.textoPara.startsWith("ia_clave") && claveVieneDeGaleria) {
            claveVieneDeGaleria = false
            abrirGaleria("ia")
            return
        }
        if (e.panel == Panel.CLAVE && (e.textoPara == "asistente" || e.textoPara.startsWith("ia_clave") && claveParaAsistente)) {
            claveParaAsistente = false
            actualizar { it.copy(panel = Panel.ASISTENTE, indice = 0) }
            return
        }
        if (e.panel == Panel.CLAVE && e.textoPara == "consola_nombre") {
            actualizar { it.copy(panel = Panel.EDITAR_CONSOLA) }
            return
        }
        if (e.panel == Panel.EDITAR_CONSOLA) {
            actualizar { it.copy(panel = if (editarDesdeMenu) Panel.MENU else Panel.NINGUNO, seccion = Seccion.CONSOLAS, enOpciones = true, indice = 0) }
            return
        }
        if (e.panel == Panel.CLAVE && e.textoPara == "buscar_ajuste") {
            actualizar { it.copy(panel = Panel.MENU, seccion = Seccion.ESENCIAL, enOpciones = false) }
            return
        }
        if (e.panel == Panel.CLAVE && e.textoPara == "buscar_imagen") {
            actualizar { it.copy(panel = Panel.IMAGENES, indice = 0) }
            return
        }
        if (e.panel == Panel.CLAVE && e.configurado) {
            val s = when {
                e.textoPara.startsWith("ra_") -> Seccion.LOGROS
                e.textoPara == "nombre" -> Seccion.INICIO
                e.textoPara.startsWith("ia") && e.seccion == Seccion.ASISTENTE -> Seccion.ASISTENTE
                e.textoPara.startsWith("ia") -> Seccion.TEMAS
                else -> Seccion.PORTADAS
            }
            actualizar { it.copy(panel = Panel.MENU, seccion = s, enOpciones = true, indice = 0) }
            return
        }
        if (e.panel == Panel.IMAGENES) {
            actualizar { it.copy(panel = Panel.FICHA, indice = 0) }
            return
        }
        if (e.panel == Panel.ESTUDIO) {
            terminarEdicion()
            return
        }
        if (e.panel == Panel.GALERIA) {
            val sec = when (e.galeria) { "musica" -> Seccion.MUSICA; "ia" -> Seccion.ASISTENTE; else -> Seccion.TEMAS }
            actualizar { it.copy(panel = Panel.MENU, seccion = sec, enOpciones = true, indice = 0) }
            return
        }
        if (e.panel == Panel.CARPETAS && e.consolaPara >= 0) {
            actualizar { it.copy(consolaPara = -1, indice = e.consolaPara) }
            return
        }
        if (e.panel == Panel.CARPETAS && e.configurado) {
            actualizar { it.copy(panel = Panel.MENU, seccion = Seccion.BIBLIOTECA, enOpciones = true, indice = 0) }
            return
        }
        if (e.panel == Panel.PORTS && e.configurado) {
            actualizar { it.copy(panel = Panel.MENU, seccion = Seccion.BIBLIOTECA, enOpciones = true, indice = 0) }
            return
        }
        actualizar { it.copy(panel = Panel.NINGUNO, pregunta = false) }
    }

    // ---------- Teclado de Díptico ----------

    fun filasTeclado(e: Estado): List<List<String>> {
        val filas = if (e.simbolos) listOf(
            "1234567890", "áéíóúüñ@#&", "-_:;/()\"'!", "+=*%\$¿¡?,.",
        ) else listOf(
            "1234567890", "qwertyuiop", "asdfghjklñ", "zxcvbnm,.?",
        )
        val letras = filas.map { f -> f.map { c -> if (e.mayus && !e.simbolos) c.uppercase() else c.toString() } }
        return listOf(EDICION) + letras + listOf(listOf("⇧", if (e.simbolos) "abc" else "#", "␣", "⌫", "✓"))
    }

    /** Fila de arriba del teclado: mover el cursor, seleccionar, copiar, cortar y pegar. */
    val EDICION = listOf("◀", "▶", "Selec.", "Todo", "Copiar", "Cortar", "Pegar")

    fun moverTecla(dx: Int, dy: Int) {
        val e = estado.value
        val filas = filasTeclado(e)
        var f = e.teclaFila.coerceIn(0, filas.size - 1)
        var c = e.teclaCol
        if (dy != 0) {
            val nf = (f + dy).coerceIn(0, filas.size - 1)
            if (nf != f) {
                val a = filas[f].size - 1
                val b = filas[nf].size - 1
                c = if (a <= 0) 0 else Math.round(c * b / a.toFloat())
                f = nf
            }
        }
        if (dx != 0) c = (c + dx).mod(filas[f].size)
        actualizar { it.copy(teclaFila = f, teclaCol = c.coerceIn(0, filas[f].size - 1)) }
    }

    /** Aprieta una tecla (con A sobre la elegida, o tocándola). */
    fun pulsarTecla(fila: Int = estado.value.teclaFila, col: Int = estado.value.teclaCol) {
        val e = estado.value
        val t = filasTeclado(e).getOrNull(fila)?.getOrNull(col) ?: return
        if (fila != e.teclaFila || col != e.teclaCol) actualizar { it.copy(teclaFila = fila, teclaCol = col) }
        pulsarValor(t)
        Sonidos.mover()
    }

    /** Hace lo de una tecla según su etiqueta. */
    fun pulsarValor(t: String) {
        val e = estado.value
        when (t) {
            "⇧" -> actualizar { it.copy(mayus = !it.mayus) }
            "#", "abc" -> actualizar { it.copy(simbolos = !it.simbolos) }
            "␣" -> escribir(" ")
            "⌫" -> borrarLetra()
            "✓" -> terminarTexto()
            "◀" -> moverCursor(-1)
            "▶" -> moverCursor(1)
            "Selec." -> alternarSeleccion()
            "Todo" -> actualizar { it.copy(ancla = 0, cursor = it.borrador.length) }
            "Copiar" -> copiar(false)
            "Cortar" -> copiar(true)
            "Pegar" -> pegar()
            else -> {
                escribir(t)
                if (e.mayus && !e.simbolos) actualizar { it.copy(mayus = false) }
            }
        }
    }

    private fun cursorDe(e: Estado) = if (e.cursor < 0 || e.cursor > e.borrador.length) e.borrador.length else e.cursor

    /** Lo seleccionado, como rango (vacío si no hay selección). */
    fun seleccion(e: Estado): IntRange {
        val c = cursorDe(e)
        val a = e.ancla?.coerceIn(0, e.borrador.length) ?: return IntRange.EMPTY
        return minOf(a, c) until maxOf(a, c)
    }

    fun escribir(t: String) {
        val e = estado.value
        val sel = seleccion(e)
        val c = cursorDe(e)
        val (antes, despues) = if (!sel.isEmpty()) e.borrador.substring(0, sel.first) to e.borrador.substring(sel.last + 1)
        else e.borrador.substring(0, c) to e.borrador.substring(c)
        cambiarBorrador(antes + t + despues)
        val nuevo = (antes.length + t.length).coerceAtMost(estado.value.borrador.length)
        actualizar { it.copy(cursor = if (despues.isEmpty()) -1 else nuevo, ancla = null) }
        // Después de un punto, la siguiente va con mayúscula.
        if (t == "." || t == "?" || t == "!") actualizar { it.copy(mayus = true) }
    }

    fun borrarLetra() {
        val e = estado.value
        val sel = seleccion(e)
        if (!sel.isEmpty()) {
            cambiarBorrador(e.borrador.removeRange(sel.first, sel.last + 1))
            actualizar { it.copy(cursor = sel.first, ancla = null) }
        } else {
            val c = cursorDe(e)
            if (c > 0) {
                cambiarBorrador(e.borrador.removeRange(c - 1, c))
                actualizar { it.copy(cursor = if (c >= e.borrador.length) -1 else c - 1) }
            }
        }
        if (estado.value.borrador.isEmpty()) actualizar { it.copy(mayus = true, cursor = -1, ancla = null) }
    }

    fun moverCursor(d: Int) {
        val e = estado.value
        val c = (cursorDe(e) + d).coerceIn(0, e.borrador.length)
        actualizar { it.copy(cursor = c) }
    }

    /** Empieza (o termina) a seleccionar desde donde está el cursor. */
    fun alternarSeleccion() {
        val e = estado.value
        actualizar { it.copy(ancla = if (e.ancla == null) cursorDe(e) else null) }
        if (e.ancla == null) avisar("Mueve el cursor para seleccionar")
    }

    private fun portapapeles() = app.getSystemService(android.content.ClipboardManager::class.java)

    /** Copia lo seleccionado (o todo si no hay selección). Con [cortar], además lo borra. */
    fun copiar(cortar: Boolean) {
        val e = estado.value
        val sel = seleccion(e)
        val texto = if (sel.isEmpty()) e.borrador else e.borrador.substring(sel.first, sel.last + 1)
        if (texto.isEmpty()) { avisar("No hay nada que copiar"); return }
        try { portapapeles()?.setPrimaryClip(android.content.ClipData.newPlainText("Díptico", texto)) } catch (_: Exception) {}
        if (cortar) {
            if (sel.isEmpty()) { cambiarBorrador(""); actualizar { it.copy(cursor = -1, ancla = null) } } else borrarLetra()
        } else {
            actualizar { it.copy(ancla = null) }
        }
        avisar(if (cortar) "Cortado" else "Copiado")
    }

    fun pegar() {
        val t = try { portapapeles()?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(app)?.toString() } catch (_: Exception) { null }
        if (t.isNullOrEmpty()) { avisar("No hay nada para pegar"); return }
        escribir(t.trim())
    }

    fun terminarTexto() {
        if (estado.value.panel == Panel.PASO) guardarPaso() else guardarClave()
    }

    fun cambiarBorrador(texto: String) {
        val max = when {
            estado.value.panel != Panel.CLAVE -> 160
            estado.value.textoPara == "notas" -> 2000
            estado.value.textoPara == "ia_pedido" -> 200
            estado.value.textoPara == "titulo" -> 80
            else -> 200
        }
        actualizar { it.copy(borrador = texto.take(max)) }
    }

    fun guardarPaso() {
        val e = estado.value
        val clave = e.seleccionado?.clave ?: return
        val datos = e.datos.toMutableMap()
        datos[clave] = (datos[clave] ?: DatosJuego()).copy(paso = e.borrador.trim())
        almacen.guardarDatos(datos)
        actualizar { it.copy(datos = datos, panel = Panel.NINGUNO, pregunta = false) }
        avisar("Próximo paso guardado")
    }

    fun guardarClave() {
        when (estado.value.textoPara) {
            "sgdb" -> Unit
            else -> {
                guardarTextoPanel()
                return
            }
        }
        val clave = estado.value.borrador.trim()
        if (clave.isEmpty()) {
            almacen.claveSgdb = ""
            actualizar { it.copy(claveSgdb = "") }
            cerrarPanel()
            avisar("Clave de SteamGridDB quitada")
            return
        }
        avisar("Probando la clave…")
        scope.launch {
            val ok = withContext(Dispatchers.IO) { try { Descargas.probarSgdb(clave) } catch (e: Exception) { false } }
            if (ok) {
                almacen.claveSgdb = clave
                actualizar { it.copy(claveSgdb = clave) }
                cerrarPanel()
                avisar("¡Clave conectada! Ya puedo bajar logos y arte grande")
            } else {
                avisar("La clave no funcionó. Revísala o prueba tu conexión.")
            }
        }
    }

    /** Arriba/abajo: en el menú se mueve dentro de la columna que tiene el foco. */
    fun moverIndice(delta: Int) {
        val e = estado.value
        if (!e.configurado && e.panel == Panel.NINGUNO) {
            val total = opcionesTuto().size
            if (total == 0) return
            val n = (e.indice + delta).coerceIn(0, total - 1)
            if (n == e.indice) return
            actualizar { it.copy(indice = n) }
            vistaPrevia(e.pasoTuto, n)
            return
        }
        when (e.panel) {
            Panel.MENU -> if (!e.enOpciones) {
                val s = Seccion.values()
                val n = (e.seccion.ordinal + delta).coerceIn(0, s.size - 1)
                actualizar { it.copy(seccion = s[n], indice = 0) }
            } else {
                val total = opcionesSeccion(e.seccion).size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta + total) % total) }
            }
            Panel.RAPIDO -> {
                val total = opcionesRapido().size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
            }
            Panel.ASISTENTE -> {
                val total = elementosAsistente().size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
            }
            Panel.APPS -> {
                val total = appsVisibles(e).size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
            }
            Panel.EDITAR_CONSOLA -> {
                val total = opcionesEditarConsola().size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
            }
            Panel.ESTUDIO -> {
                val total = controlesEstudio(e).size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
            }
            Panel.GALERIA -> {
                val total = itemsGaleria(e).size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
            }
            Panel.IMAGENES -> {
                val total = candidatosVisibles(e).size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
            }
            Panel.BUSCAR -> {
                val total = e.visibles.size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
            }
            Panel.CENTRO -> {
                val total = opcionesCentro().size + Avisos.lista.value.size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
            }
            Panel.CARPETAS -> {
                val total = if (e.consolaPara >= 0) consolasElegibles().size + 1 else filasCarpetas()
                actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
            }
            Panel.PORTS -> {
                val total = e.apps.size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
            }
            Panel.FICHA -> {
                val total = when {
                    e.pestanaFicha == 4 -> e.logrosFicha?.logros?.size ?: 0
                    e.pestanaFicha >= 2 -> opcionesFicha().size
                    else -> 40
                }
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
            }
            else -> Unit
        }
    }

    /** Izquierda/derecha en el menú: cambia de columna. */
    fun columnaMenu(aOpciones: Boolean) {
        actualizar { it.copy(enOpciones = aOpciones, indice = if (aOpciones) 0 else it.indice) }
    }

    fun elegirSeccion(s: Seccion) {
        actualizar { it.copy(seccion = s, enOpciones = true, indice = 0) }
    }

    fun elegirIndice(i: Int) {
        val e = estado.value
        if (!e.configurado && e.panel == Panel.NINGUNO && i != e.indice) vistaPrevia(e.pasoTuto, i)
        actualizar { it.copy(indice = i, enOpciones = if (it.panel == Panel.MENU) true else it.enOpciones) }
        aceptar()
    }

    fun aceptar() {
        val e = estado.value
        if (!e.configurado && e.panel == Panel.NINGUNO) {
            val op = opcionesTuto().getOrNull(e.indice) ?: return
            op.accion()
            return
        }
        when (e.panel) {
            Panel.MENU -> if (!e.enOpciones) columnaMenu(true) else opcionesSeccion(e.seccion).getOrNull(e.indice)?.accion?.invoke()
            Panel.RAPIDO -> opcionesRapido().getOrNull(e.indice)?.accion?.invoke()
            Panel.PORTS -> e.apps.getOrNull(e.indice)?.let { alternarPort(it.paquete) }
            Panel.FICHA -> if (e.pestanaFicha >= 2) opcionesFicha().getOrNull(e.indice)?.accion?.invoke()
            Panel.BUSCAR -> e.visibles.getOrNull(e.indice)?.let { j ->
                actualizar { it.copy(juegoSel = j.clave, panel = Panel.NINGUNO) }
            }
            Panel.CARPETAS -> aceptarCarpetas()
            Panel.IMAGENES -> usarCandidato()
            Panel.GALERIA -> itemsGaleria(e).getOrNull(e.indice)?.accion?.invoke()
            Panel.ESTUDIO -> aceptarControl()
            Panel.EDITAR_CONSOLA -> opcionesEditarConsola().getOrNull(e.indice)?.accion?.invoke()
            Panel.APPS -> appsVisibles(e).getOrNull(e.indice)?.let { abrirPaquete(it.paquete) }
            Panel.ASISTENTE -> elementosAsistente().getOrNull(e.indice)?.accion?.invoke()
            Panel.CENTRO -> {
                val acciones = opcionesCentro()
                if (e.indice < acciones.size) acciones[e.indice].accion() else Avisos.lista.value.getOrNull(e.indice - acciones.size)?.let { abrirAviso(it) }
            }
            Panel.TRABAJOS -> cerrarPanel()
            else -> Unit
        }
    }

    /** B: retrocede un nivel. */
    fun atras() {
        val e = estado.value
        when {
            !e.configurado && e.panel == Panel.NINGUNO -> pasoAnterior()
            e.panel == Panel.MENU && e.enOpciones -> columnaMenu(false)
            e.panel != Panel.NINGUNO -> cerrarPanel()
        }
    }

    // ---------- Menú rápido de un juego (mantener A) ----------

    /** La ventana de un juego (Y o mantener A): acciones en mosaico. */
    /** Las primeras opciones de la ventana del juego van grandes, en una fila. */
    const val PRIMARIAS_RAPIDO = 5

    fun opcionesRapido(): List<Opcion> {
        val e = estado.value
        val juego = e.seleccionado ?: return emptyList()
        val d = e.datos[juego.clave]
        val consola = Consolas.porId(juego.consolaId)
        val lista = mutableListOf<Opcion>()
        // Fila de arriba: lo más usado.
        lista += Opcion("Jugar", icono = Icono.JUGAR) { cerrarPanel(); jugar() }
        lista += Opcion("Ficha", icono = Icono.TUTORIAL) { abrirFicha() }
        lista += Opcion("Mi paso", d?.paso?.take(30) ?: "", Icono.LAPIZ) { abrirPaso() }
        lista += Opcion(if (d?.favorito == true) "Favorito ✓" else "Favorito", icono = Icono.CORAZON) { alternarFavorito(juego.clave) }
        lista += Opcion("Bisa", icono = Icono.BISA) { abrirAsistente(); preguntar("Dame un buen truco o consejo para ${juego.titulo}.") }
        // Lista de abajo: ajustes de este juego.
        lista += Opcion("Personalizar", "Título, imágenes, música", Icono.PALETA) { abrirFicha(); elegirPestanaFicha(3) }
        lista += Opcion("Imágenes", "Tienda de imágenes", Icono.DESCARGA) { abrirFicha(); buscarCandidatos("portada") }
        if (consola != null && !consola.sinEmulador) {
            lista += Opcion("Emulador", d?.emulador?.let { id -> Lanzador.opciones(app, consola).firstOrNull { it.id == id }?.nombre } ?: "El de la consola", Icono.EMULADOR) {
                ciclarEmuladorJuego(juego.clave, consola)
            }
        }
        if (!e.unaPantalla) {
            lista += Opcion(
                "Abre en",
                when (d?.pantalla) { "arriba" -> "Arriba"; "abajo" -> "Abajo"; else -> "Según el menú" },
                Icono.PANTALLAS,
            ) { ciclarPantallaJuego(juego.clave) }
            lista += Opcion(
                "Lupa en la pantalla de abajo", if (d?.lupa == true) "Sí" else "No", Icono.BUSCAR,
                "Mientras juegas arriba, la pantalla de abajo muestra ampliada una parte de la de arriba: el minimapa, la vida, los subtítulos. La zona se elige en la misma lupa.",
            ) { alternarLupaJuego(juego.clave) }
        }
        if (consola != null) {
            lista += Opcion("Cajas de ${consola.nombre}", nombreForma(e.formas[consola.id]?.forma ?: "original"), Icono.FORMA) {
                ciclarForma(consola.id)
            }
        }
        lista += Opcion(
            if (d?.oculto == true) "Mostrar este juego" else "Ocultar este juego",
            if (d?.oculto == true) "Vuelve al estante" else "Se ve con el filtro Ocultos", Icono.OCULTAR,
        ) { alternarOculto(juego.clave) }
        return lista
    }

    /** Moverse en la ventana del juego: fila de botones grandes arriba, lista abajo. */
    fun moverRapido(dx: Int, dy: Int) {
        val e = estado.value
        val total = opcionesRapido().size
        if (total == 0) return
        val i = e.indice
        val n = when {
            i < PRIMARIAS_RAPIDO && dx != 0 -> (i + dx).coerceIn(0, PRIMARIAS_RAPIDO - 1)
            i < PRIMARIAS_RAPIDO && dy > 0 -> PRIMARIAS_RAPIDO.coerceAtMost(total - 1)
            i < PRIMARIAS_RAPIDO -> i
            dy < 0 -> if (i == PRIMARIAS_RAPIDO) 0 else i - 1
            dy > 0 -> (i + 1).coerceAtMost(total - 1)
            else -> i
        }
        if (n != i) actualizar { it.copy(indice = n) }
    }

    /** Columnas de la ventana del juego (la interfaz avisa cuántas caben). */
    var columnasVentana = 4

    /** Emulador solo para este juego. */
    private fun ciclarEmuladorJuego(clave: String, c: Consola) {
        val juego = estado.value.juegos.firstOrNull { it.clave == clave }
        val ops = Lanzador.opciones(app, c, juego?.tipo).distinctBy { it.id }
        if (ops.isEmpty()) {
            avisar("No hay emuladores de ${c.nombre} instalados. Te sugiero ${c.sugerido}.")
            return
        }
        val actual = estado.value.datos[clave]?.emulador
        val i = ops.indexOfFirst { it.id == actual }
        val siguiente = if (i == ops.size - 1) null else ops[i + 1].id
        cambiarDatos(clave) { it.copy(emulador = siguiente) }
    }

    private fun ciclarPantallaJuego(clave: String) {
        val datos = estado.value.datos.toMutableMap()
        val d = datos[clave] ?: DatosJuego()
        val siguiente = when (d.pantalla) { null -> "arriba"; "arriba" -> "abajo"; else -> null }
        datos[clave] = d.copy(pantalla = siguiente)
        almacen.guardarDatos(datos)
        actualizar { it.copy(datos = datos) }
    }

    // ---------- Menú principal ----------

    fun seccionesMenu(): List<Seccion> = Seccion.values().toList()

    private var cacheOpciones: Pair<Estado, HashMap<Seccion, List<Opcion>>>? = null

    /** Las opciones de una sección. Se arman una vez por cada cambio de estado (las dos pantallas las comparten). */
    /** Subtítulo que agrupa las opciones largas de algunas secciones (así es más fácil encontrar algo). "" = sin grupo. */
    fun grupoOpcion(s: Seccion, t: String): String = when (s) {
        Seccion.ASISTENTE -> when {
            t == "Hablar con Bisa" || t == "Bisa mientras juegas" || t == "Burbuja flotante" -> "Bisa"
            t.startsWith("Clave de") -> "Claves de IA en la nube"
            t == "Borrar la conversación" -> "Conversación"
            else -> "Su cerebro"
        }
        Seccion.TEMAS -> when {
            t.startsWith("Mi tema") -> "Mi tema"
            t.contains("IA") -> "Con IA"
            else -> "Elegir"
        }
        Seccion.INICIO -> when {
            t.startsWith("Bienvenida") || t == "Tu nombre" -> "Al encender"
            t == "Estilo del inicio" || t.contains("widget", true) && !t.startsWith("Permiso") -> "Pantalla principal"
            else -> "Más"
        }
        Seccion.PANTALLAS -> when {
            t.startsWith("Tamaño") || t.contains("letra", true) -> "Tamaño"
            t.startsWith("Teclado") -> "Escribir"
            else -> "Pantallas"
        }
        else -> ""
    }

    fun opcionesSeccion(s: Seccion): List<Opcion> {
        val e = estado.value
        val c = cacheOpciones?.takeIf { it.first === e }?.second ?: HashMap<Seccion, List<Opcion>>().also { cacheOpciones = e to it }
        return c.getOrPut(s) { armarOpciones(s) }
    }

    /** Ajustes que más se usan, sacados de todas las secciones. O, si se buscó algo, lo que coincide. */
    private fun opcionesEsenciales(e: Estado): List<Opcion> {
        val todas = Seccion.values().filter { it != Seccion.ESENCIAL }.flatMap { s -> armarOpciones(s).map { s to it } }
        if (e.buscarAjuste.isNotBlank()) {
            val q = Busqueda.normalizar(e.buscarAjuste)
            val res = todas.filter { (s, o) ->
                listOf(o.titulo, o.ayuda, s.titulo, Idioma.t(o.titulo), Idioma.t(s.titulo)).any { Busqueda.normalizar(it).contains(q) }
            }.map { (s, o) -> o.copy(valor = o.valor.ifEmpty { s.titulo }) }
            return listOf(Opcion("Quitar la búsqueda", "\"${e.buscarAjuste}\" · ${res.size}", Icono.CERRAR) { actualizar { it.copy(buscarAjuste = "", indice = 0) } }) + res
        }
        val elegidos = listOf(
            "Modo", "Tamaño de letra", "Modo de pantallas", "Intercambiar pantallas", "Teclado", "Música de fondo",
            "Sonidos al moverte", "Fechas especiales", "Efecto de los botones", "Fluidez", "Bisa mientras juegas",
        )
        val lista = mutableListOf(
            Opcion("Buscar un ajuste", "Y", Icono.BUSCAR, "Escribe una palabra (música, tema, teclado…) y te muestro dónde está.") { abrirTexto("buscar_ajuste") },
            Opcion("Idioma", Idioma.IDIOMAS.first { it.first == e.idioma }.second, Icono.IDIOMA) { abrirMenu(Seccion.IDIOMA) },
            Opcion("Acomodar el inicio", icono = Icono.EDITAR, ayuda = "Mueve, agranda y cambia el estilo de tus widgets. También manteniendo apretado el inicio.") { cerrarPanel(); editarWidgets() },
            Opcion("Hablar con Bisa", icono = Icono.BISA) { abrirAsistente() },
        )
        for (t in elegidos) todas.firstOrNull { it.second.titulo == t }?.let { lista += it.second }
        return lista
    }

    private fun armarOpciones(s: Seccion): List<Opcion> {
        val e = estado.value
        val lista = mutableListOf<Opcion>()
        when (s) {
            Seccion.ESTANTE -> lista += opcionesEstante(e)
            Seccion.CONSOLA -> lista += opcionesConsola(e)
            Seccion.CONSOLAS -> {
                for (c in e.consolasConJuegos) {
                    val n = e.juegos.count { it.consolaId == c.id }
                    val propia = e.consolasPropias[c.id]?.vacia == false || e.iconosPropios.containsKey(c.id)
                    lista += Opcion(
                        c.bonito, if (propia) "Personalizada ✓" else if (n == 1) "1 juego" else "$n juegos", Icono.MANDO,
                        "Cambia su nombre, color, ícono, imagen grande, logo, música, cajas y emulador.",
                    ) { abrirEditarConsola(c.id, desdeMenu = true) }
                }
                if (lista.isEmpty()) lista += Opcion("Aún no hay consolas", "Elige tu carpeta de juegos", Icono.CARPETA) { abrirMenu(Seccion.BIBLIOTECA) }
            }
            Seccion.PANTALLAS -> {
                lista += Opcion(
                    "Tamaño en la pantalla de arriba", nombreZoom(e.zoomArriba), Icono.ARRIBA,
                    "Automático ajusta todo según la forma de la pantalla. Compacto muestra más cosas; grande, todo más grande.",
                ) { ciclarZoom(true) }
                if (e.dosDisponibles) lista += Opcion(
                    "Tamaño en la pantalla de abajo", nombreZoom(e.zoomAbajo), Icono.ABAJO,
                    "La pantalla de abajo de la Thor es casi cuadrada: en automático todo se acomoda a esa forma.",
                ) { ciclarZoom(false) }
                lista += Opcion(
                    "Modo de pantallas", if (e.dosDisponibles) nombreModo(e.modoPantallas) else "Una (no hay otra)", Icono.PANTALLAS,
                    "Dos pantallas: arriba el juego y abajo el estante. Una: todo en la pantalla principal, como en una tele o una Odin.",
                    apagada = !e.dosDisponibles,
                ) {
                    if (!estado.value.dosDisponibles) {
                        avisar("Este equipo tiene una sola pantalla")
                    } else {
                        val siguiente = when (estado.value.modoPantallas) { "dos" -> "una_negra"; "una_negra" -> "una_libre"; else -> "dos" }
                        elegirModo(siguiente)
                    }
                }
                lista += Opcion(
                    "Atenuación mientras juegas", nombreAtenuacion(e.atenuacion), Icono.NEGRO,
                    "Qué tan oscura queda la otra pantalla mientras juegas. Se atenúa sola 5 segundos después de abrir el juego; tocarla la despierta.",
                    apagada = e.unaPantalla,
                ) {
                    val l = NIVELES_ATENUACION
                    val v = l[(l.indexOfFirst { kotlin.math.abs(it - estado.value.atenuacion) < 0.005f } + 1).mod(l.size)]
                    almacen.guardarTexto("atenuacion", v.toString())
                    actualizar { it.copy(atenuacion = v) }
                }
                lista += Opcion(
                    "Teclado", if (e.tecladoPropio) "Díptico, en la otra pantalla" else "El de Android", Icono.TEXTO,
                    "Con dos pantallas, el teclado de Díptico aparece en la pantalla contraria para que veas lo que escribes. Se usa con la cruceta o tocando.",
                    apagada = e.unaPantalla,
                ) {
                    val v = !estado.value.tecladoPropio
                    almacen.guardarBool("tecladoPropio", v)
                    actualizar { it.copy(tecladoPropio = v) }
                }
                lista += Opcion(
                    "Intercambiar pantallas", if (e.intercambiar) "Sí" else "No", Icono.INTERCAMBIAR,
                    "Pone el estante arriba y el juego elegido abajo. También está en la barra de abajo.",
                    apagada = e.unaPantalla,
                ) { intercambiar() }
                lista += Opcion(
                    "Los juegos se abren en", if (e.pantallaJuegos == "abajo") "Abajo" else "Arriba", Icono.JUGAR,
                    "La pantalla donde se abre el emulador al apretar A. Para un juego en especial, mantén A sobre él.",
                    apagada = e.unaPantalla,
                ) {
                    if (estado.value.unaPantalla) {
                        avisar("Con una pantalla, los juegos se abren en la principal")
                    } else {
                        val v = if (estado.value.pantallaJuegos == "abajo") "arriba" else "abajo"
                        almacen.pantallaJuegos = v
                        actualizar { it.copy(pantallaJuegos = v) }
                    }
                }
                lista += Opcion(
                    "Tamaño de letra", nombreTam(e.tamTexto), Icono.TEXTO,
                    "Agranda todas las letras de Díptico. Se nota al instante.",
                ) {
                    val i = tamanos.indexOfFirst { kotlin.math.abs(it - estado.value.tamTexto) < 0.01f }
                    fijarTam(tamanos[(i + 1) % tamanos.size])
                }
            }
            Seccion.INICIO -> lista += opcionesInicio(e)
            Seccion.TEMAS -> lista += opcionesTemas(e)
            Seccion.LOGROS -> lista += opcionesLogros(e)
            Seccion.ESENCIAL -> lista += opcionesEsenciales(e)
            Seccion.ASISTENTE -> {
                lista += Opcion("Hablar con Bisa", icono = Icono.BISA, ayuda = "Bisa es tu asistente: te recomienda juegos de tu biblioteca, te ayuda cuando te atascas y puede cambiar el tema o la vista por ti.") { abrirAsistente() }
                lista += Opcion(
                    "Bisa mientras juegas", if (e.bisaEnJuego) "Sí" else "No", Icono.MANDO,
                    "Mientras juegas deja un aviso para pedir ayuda, y si reintentas varias veces seguidas la misma partida te ofrece una pista. Se abre en la pantalla libre.",
                ) {
                    val v = !estado.value.bisaEnJuego
                    almacen.guardarBool("bisaEnJuego", v)
                    actualizar { it.copy(bisaEnJuego = v) }
                }
                lista += Opcion(
                    "Burbuja flotante", if (cl.nikalbazu.diptico.bisa.BurbujaBisa.puedeFlotar(app)) "Activada ✓" else "Dar permiso", Icono.BISA,
                    "Para juegos que usan las dos pantallas (DS, 3DS) o el modo una pantalla: Bisa aparece como una burbuja encima del juego, con su botón para volver.",
                ) { if (cl.nikalbazu.diptico.bisa.BurbujaBisa.puedeFlotar(app)) avisar("La burbuja ya tiene permiso") else cl.nikalbazu.diptico.bisa.BurbujaBisa.pedirPermisoFlotante(app) }
                lista += Opcion(
                    "IA de Bisa", textoMotor(e), Icono.CHISPA,
                    "Su cerebro: la IA de tu consola (gratis, sin internet), Gemini (Google, con nivel gratis), ChatGPT o Claude. Cada persona usa su propia clave.",
                ) { abrirGaleria("ia") }
                lista += Opcion(
                    "Buscar guías en internet", if (e.bisaWeb) "Sí" else "No", Icono.WEB,
                    "Con Gemini, ChatGPT o Claude, Bisa busca en internet para darte datos exactos de cada juego. La IA de tu consola no busca.",
                ) {
                    val v = !estado.value.bisaWeb
                    almacen.guardarBool("bisaWeb", v)
                    actualizar { it.copy(bisaWeb = v) }
                }
                lista += Opcion(
                    "IA en tu consola",
                    when { e.iaLocalLista -> "Lista ✓ · ${IALocal.NOMBRE_MODELO}"; trabajosMapa.containsKey("ialocal") -> "Bajando…"; else -> "Bajar (1,6 GB)" },
                    Icono.DESCARGA,
                    "Un modelo de IA libre (${IALocal.NOMBRE_MODELO}) que corre en tu consola. Se baja una vez; después Bisa funciona sin internet y sin gastar créditos, aunque compartas Díptico.",
                ) { if (estado.value.iaLocalLista) avisar("La IA local ya está lista") else descargarIALocal() }
                for (prov in IA.PROVEEDORES) {
                    val clave = claveDe(prov, e)
                    lista += Opcion(
                        "Clave de ${IA.nombre(prov)}" + if (prov == "gemini") " (gratis)" else "",
                        if (clave.isBlank()) "Pegar" else "Guardada ✓", Icono.CLAVE,
                        "Se saca en ${IA.dondeClave(prov)}. Solo se guarda en tu consola." + if (prov == "gemini") " Con tu cuenta de Google es gratis (con un límite diario)." else " Usa el saldo de tu cuenta.",
                    ) { abrirTexto(claveTexto(prov), clave) }
                }
                lista += Opcion(
                    "Usar un modelo propio", icono = Icono.CARPETA,
                    ayuda = "Si tienes otro modelo en formato .task de MediaPipe (por ejemplo Gemma), elígelo y Bisa lo usará.",
                ) { pedirArchivoPara("modeloIA", "*/*") }
                if (e.iaLocalLista) lista += Opcion("Borrar la IA local", "Libera 1,6 GB", Icono.BORRAR) {
                    IALocal.borrar(app)
                    actualizar { it.copy(iaLocalLista = false) }
                    avisar("IA local borrada")
                }
                lista += Opcion("Borrar la conversación", if (e.chat.isEmpty()) "Vacía" else "${e.chat.size} mensajes", Icono.BORRAR) { borrarChat(); avisar("Conversación borrada") }
            }
            Seccion.IDIOMA -> Idioma.IDIOMAS.forEach { (cod, nombre) ->
                lista += Opcion(nombre, if (e.idioma == cod) "En uso ✓" else "", Icono.IDIOMA, Idioma.ayuda(cod)) { cambiarIdioma(cod) }
            }
            Seccion.APARIENCIA -> {
                lista += Opcion("Modo", if (e.oscuro) "Oscuro" else "Claro", Icono.TEMA, "El oscuro ahorra batería en pantallas OLED. Algunos temas usan siempre el suyo.") { alternarTema() }
                lista += Opcion(
                    "Efecto de los botones", nombreEfecto(e.efecto), Icono.ESTRELLA,
                    "Liso, degradado, con un brillo que pasa, o de vidrio. Se ve en el botón Jugar, el menú y las opciones elegidas.",
                ) {
                    val l = listOf("liso", "degradado", "brillo", "vidrio")
                    val v = l[(l.indexOf(estado.value.efecto) + 1).mod(l.size)]
                    almacen.guardarTexto("efecto", v)
                    actualizar { it.copy(efecto = v) }
                    cl.nikalbazu.diptico.ui.Estilo.efecto = v
                }
                lista += Opcion(
                    "Fluidez", nombreFluidez(e.fluidez) + if (e.fluidez == "auto") " (${nombreFluidez(e.fluidezDetectada).lowercase()})" else "", Icono.CHISPA,
                    "Automática elige según tu consola. Alta usa la máxima tasa de refresco y todos los efectos; ahorro apaga difuminados y decoraciones para ir más liviano.",
                ) {
                    val l = listOf("auto", "alta", "equilibrada", "ahorro")
                    val v = l[(l.indexOf(estado.value.fluidez) + 1).mod(l.size)]
                    almacen.guardarTexto("fluidez", v)
                    actualizar { it.copy(fluidez = v) }
                    aplicarFluidez()
                }
                lista += Opcion(
                    "Protector de pantalla", if (e.protectorMin == 0) "Nunca" else "A los ${e.protectorMin} min", Icono.RELOJ,
                    "Si no usas la consola, muestra tus juegos con las dos pantallas atenuadas. Cualquier botón lo cierra.",
                ) {
                    val l = listOf(2, 5, 10, 20, 0)
                    val v = l[(l.indexOf(estado.value.protectorMin) + 1).mod(l.size)]
                    almacen.guardarTexto("protectorMin", v.toString())
                    actualizar { it.copy(protectorMin = v) }
                }
                lista += Opcion("Ver el protector ahora", icono = Icono.RELOJ) { cerrarPanel(); actualizar { it.copy(protector = true) } }
                lista += Opcion(
                    "Decoraciones", if (e.decoraciones) "Sí" else "No", Icono.ESTRELLA,
                    "Nieve en Navidad, murciélagos en Halloween, pétalos en primavera… según el tema.",
                ) {
                    val v = !estado.value.decoraciones
                    almacen.guardarBool("decoraciones", v)
                    actualizar { it.copy(decoraciones = v) }
                    aplicarTema()
                }
                lista += opcionesIconos(e)
                lista += Opcion(
                    "Animación de entrada", if (e.mostrarIntro) "Sí" else "No", Icono.INTRO,
                    "La animación con el logo de Díptico al encender.",
                ) {
                    val v = !estado.value.mostrarIntro
                    almacen.mostrarIntro = v
                    actualizar { it.copy(mostrarIntro = v) }
                }
                lista += Opcion("Ver la animación ahora", icono = Icono.ESTRELLA, ayuda = "Para mostrársela a alguien.") { verIntro() }
            }
            Seccion.MUSICA -> {
                lista += Opcion(
                    "Música de fondo", if (e.musica) "Sí" else "No", Icono.MUSICA,
                    "Suena mientras eliges. Se calla al abrir un juego o si otra app pone música.",
                ) { fijarMusica(!estado.value.musica) }
                lista += Opcion(
                    "Música según el tema", if (e.musicaTema) "Sí" else "No", Icono.PALETA,
                    "Cada tema trae su música: Halloween suena a fantasmas, Navidad a cascabeles, Sakura a flauta japonesa… Ahora: ${nombrePista(e.pistaTema)}.",
                ) {
                    val v = !estado.value.musicaTema
                    almacen.guardarBool("musicaTema", v)
                    actualizar { it.copy(musicaTema = v) }
                }
                val sonando = if (e.musicaTema && e.pista != "mia") e.pistaTema else e.pista
                lista.add(
                    0,
                    Opcion(
                        "Elegir canción", if (e.musica) nombrePista(sonando) else "Música apagada", Icono.MUSICA,
                        "Todas las canciones de Díptico en una lista, para escucharlas y elegir.",
                    ) { abrirGaleria("musica") },
                )
                lista += Opcion(
                    "Sonidos al moverte", if (e.sonidos) "Sí" else "No", Icono.VOLUMEN,
                    "Un sonido suave al moverte, elegir, volver y abrir un juego.",
                ) {
                    val v = !estado.value.sonidos
                    almacen.guardarBool("sonidos", v)
                    Sonidos.activo = v
                    actualizar { it.copy(sonidos = v) }
                    if (v) Sonidos.aceptar()
                }
                lista += Opcion("Volumen", nombreVolumen(e.volumen), Icono.VOLUMEN, "Qué tan fuerte suena la música de Díptico. Es independiente del volumen de tus juegos.") {
                    val i = volumenes.indexOfFirst { kotlin.math.abs(it - estado.value.volumen) < 0.01f }
                    val v = volumenes[(i + 1) % volumenes.size]
                    almacen.volumen = v
                    actualizar { it.copy(volumen = v) }
                }
                lista += Opcion(
                    "Mi propia música", if (e.carpetaMusica != null) "Cambiar carpeta" else "Elegir carpeta", Icono.CARPETA,
                    "Elige una carpeta con canciones (mp3, ogg, m4a, flac). Suenan en orden aleatorio.",
                ) { pedirCarpeta?.invoke(Carpeta.MUSICA) }
            }
            Seccion.EMULADORES -> {
                val consolas = e.consolasEmu.ifEmpty { Consolas.todas.filter { !it.sinEmulador }.take(20) }
                for (c in consolas) {
                    lista += Opcion(
                        c.bonito,
                        if (emuladorListo(c)) nombreEmulador(c) else "Falta: ${c.sugerido}",
                        Icono.EMULADOR,
                        "Aprieta A para pasar al siguiente emulador instalado. Con RetroArch puedes elegir el núcleo; " +
                            "el núcleo debe estar descargado dentro de RetroArch.",
                    ) { ciclarEmulador(c) }
                }
            }
            Seccion.PORTADAS -> {
                lista += Opcion(
                    "Reconocer nombres de juegos", icono = Icono.TITULO,
                    ayuda = "Busca el nombre oficial de cada juego en libretro (gratis). Se hace solo al buscar juegos nuevos.",
                ) { identificarNombres(avisarAlFinal = true) }
                lista += Opcion(
                    "Reconocer nombres con IA", if (llaveNube(e) == null) "Necesita IA en la nube" else "Para los que faltan", Icono.CHISPA,
                    "La IA reconoce los juegos que libretro no conoce (Switch, PS3, archivos con nombres raros) y les pone su nombre real.",
                ) { identificarConIA() }
                val d = e.descarga
                lista += Opcion(
                    if (d != null) "Descargando…" else "Descargar lo que falta",
                    if (d != null) "${d.hechos} de ${d.total}" else "${e.sinPortada} sin portada",
                    Icono.DESCARGA,
                    "Busca en libretro (gratis) y, si conectaste tu clave, en SteamGridDB. No toca tus propias imágenes.",
                ) { if (estado.value.descarga == null) descargarPortadas(todo = false) else cancelarDescarga() }
                lista += Opcion(
                    "Buscar de nuevo todo", "Incluye los que ya tienen", Icono.BUSCAR,
                    "Vuelve a buscar títulos, arte y logos para todos los juegos.",
                ) { if (estado.value.descarga == null) descargarPortadas(todo = true) }
                lista += Opcion(
                    "Clave de SteamGridDB", if (e.claveSgdb.isNotBlank()) "Conectada ✓" else "Opcional", Icono.CLAVE,
                    "Gratis en steamgriddb.com: entra, ve a Preferencias › API y copia tu clave. Trae arte grande y logos.",
                ) { abrirClave() }
                lista += Opcion(
                    "Títulos de libretro", if (e.titulosBonitos) "Sí" else "No", Icono.TITULO,
                    "Muestra el nombre oficial del juego en vez del nombre del archivo.",
                ) {
                    val v = !estado.value.titulosBonitos
                    almacen.titulosBonitos = v
                    actualizar { it.copy(titulosBonitos = v) }
                    publicarJuegos()
                }
                lista += Opcion(
                    "Imágenes de ES-DE", if (e.tieneMedia) "Conectadas ✓" else "Opcional", Icono.CARPETA,
                    "Si ya usas ES-DE, elige su carpeta downloaded_media y uso esas portadas.",
                ) { pedirCarpeta?.invoke(Carpeta.MEDIA) }
                lista += Opcion("Borrar lo descargado", icono = Icono.BORRAR, ayuda = "Libera espacio. Tus imágenes propias no se tocan.") {
                    cancelarDescarga()
                    scope.launch {
                        withContext(Dispatchers.IO) { Descargas.borrar(app) }
                        medios = emptyMap()
                        almacen.guardarMedios(medios)
                        publicarJuegos()
                        avisar("Listo, borré las imágenes descargadas")
                    }
                }
            }
            Seccion.BIBLIOTECA -> {
                lista += Opcion(
                    "Volver a buscar juegos", if (e.escaneando) "Buscando…" else "${e.juegos.size} juegos", Icono.BUSCAR,
                    "Úsalo cuando agregues juegos nuevos a tu carpeta.",
                ) { escanear() }
                val ocultos = e.datos.count { it.value.oculto }
                lista += Opcion(
                    "Juegos ocultos", if (ocultos == 0) "Ninguno" else "$ocultos", Icono.OCULTAR,
                    "Los juegos que ocultaste desde sus opciones (Y). Desde aquí los ves y los vuelves a mostrar.",
                ) { actualizar { it.copy(panel = Panel.BUSCAR, filtro = "ocultos", busqueda = "", indice = 0) } }
                lista += Opcion(
                    "Carpetas de juegos", "${almacen.carpetas().size} carpetas", Icono.CARPETA,
                    "Agrega todas las que quieras (memoria interna, SD…). Si una carpeta no tiene el nombre de una consola, le eliges la consola.",
                ) { abrirCarpetas() }
                lista += Opcion(
                    "Ports y apps", if (e.ports.isEmpty()) "Elegir" else "${e.ports.size} elegidos", Icono.APPS,
                    "Los ports son juegos hechos para Android (Ship of Harkinian, los de Doom, Mario 64…). " +
                        "Márcalos y aparecen en su propio estante, con tiempo jugado y próximo paso.",
                ) { abrirPorts() }
                lista += Opcion(
                    "Juegos de Android", if (e.android.isEmpty()) "Elegir" else "${e.android.size} juegos", Icono.MOVIL,
                    "Genshin, Minecraft y cualquier juego instalado. Tienen su propio estante, ficha, guías y personalización.",
                ) { abrirPorts(Consolas.ANDROID) }
                lista += Opcion(
                    "Buscar juegos de Android nuevos", icono = Icono.BUSCAR,
                    ayuda = "Agrega solos los juegos que Android reconoce como juego.",
                ) { detectarAndroid() }
                lista += Opcion("Ir a lo último que jugaste", icono = Icono.CONTINUAR, ayuda = "Salta directo a tu último juego.") {
                    cerrarPanel(); continuar()
                }
            }
            Seccion.SISTEMA -> {
                lista += Opcion("Repetir el tutorial", icono = Icono.TUTORIAL, ayuda = "Vuelve a configurar todo paso a paso.") { reiniciarTutorial() }
                lista += Opcion(
                    "Acerca de Díptico", "Versión $VERSION", Icono.INFO,
                    "Hecho para la AYN Thor y consolas de una pantalla. Abre los emuladores con las reglas de ES-DE.",
                ) { avisar("Díptico $VERSION · hecho con cariño para jugar") }
                lista += Opcion("Cerrar el menú", icono = Icono.CERRAR, ayuda = "También con B o Start.") { cerrarPanel() }
            }
        }
        return lista
    }

    // ---------- Emuladores ----------

    fun nombreEmulador(c: Consola): String =
        Lanzador.elegida(app, c, estado.value.emuladores[c.id])?.nombre ?: "Elegir al abrir"

    fun emuladorListo(c: Consola): Boolean = Lanzador.elegida(app, c, estado.value.emuladores[c.id]) != null

    private fun ciclarEmulador(c: Consola) {
        val ops = Lanzador.opciones(app, c).distinctBy { it.id }
        if (ops.isEmpty()) {
            avisar("No hay emuladores de ${c.nombre} instalados. Te sugiero ${c.sugerido}.")
            return
        }
        val actual = Lanzador.elegida(app, c, estado.value.emuladores[c.id])?.id
        val i = ops.indexOfFirst { it.id == actual }
        val siguiente = ops[(i + 1) % ops.size]
        val m = estado.value.emuladores + (c.id to siguiente.id)
        almacen.guardarEmuladores(m)
        actualizar { it.copy(emuladores = m) }
    }

    // ---------- Descargas ----------

    fun descargarPortadas(todo: Boolean) {
        if (descargaJob?.isActive == true) return
        val e = estado.value
        val pendientes = todosCrudos().filter { j ->
            val m = medios[j.clave]
            todo || (j.portada == null && m?.portada == null) || (j.arte == null && m?.arte == null) ||
                (e.claveSgdb.isNotBlank() && m?.logo == null)
        }
        if (pendientes.isEmpty()) {
            avisar("Todos tus juegos ya tienen portada")
            return
        }
        descargaJob = scope.launch {
            val hay = withContext(Dispatchers.IO) { Descargas.hayInternet() }
            if (!hay) {
                actualizar { it.copy(descarga = null) }
                avisar("No tengo internet. Conéctate a una red Wi-Fi y prueba de nuevo.")
                return@launch
            }
            var encontrados = 0
            val clave = estado.value.claveSgdb
            pendientes.forEachIndexed { i, j ->
                actualizar { it.copy(descarga = Descarga(i, pendientes.size, j.titulo, encontrados)) }
                trabajo("portadas", "Portadas · ${j.titulo}", i / pendientes.size.toFloat())
                val c = Consolas.porId(j.consolaId) ?: return@forEachIndexed
                if (c.esPorts && clave.isBlank()) return@forEachIndexed
                val previo = if (todo) null else medios[j.clave]
                val r = withContext(Dispatchers.IO) {
                    try { Descargas.buscar(app, j, c, previo, clave) } catch (ex: Exception) { null }
                }
                if (r != null) {
                    if (r.encontrado) encontrados++
                    medios = medios + (j.clave to r.medios)
                    if (i % 4 == 3) {
                        withContext(Dispatchers.IO) { almacen.guardarMedios(medios) }
                        publicarJuegos()
                    }
                }
            }
            withContext(Dispatchers.IO) { almacen.guardarMedios(medios) }
            publicarJuegos()
            actualizar { it.copy(descarga = null) }
            terminarTrabajo("portadas")
            avisar("Listo: encontré $encontrados de ${pendientes.size} juegos")
        }
    }

    private fun cancelarDescarga() {
        descargaJob?.cancel()
        descargaJob = null
        terminarTrabajo("portadas")
        actualizar { it.copy(descarga = null) }
        almacen.guardarMedios(medios)
        publicarJuegos()
    }

    private fun descargarUno(j: Juego) {
        val crudo = todosCrudos().firstOrNull { it.clave == j.clave } ?: return
        val c = Consolas.porId(crudo.consolaId) ?: return
        avisar("Buscando ${j.titulo}…")
        trabajo("uno", "Imágenes de ${j.titulo}")
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                try { Descargas.buscar(app, crudo, c, null, estado.value.claveSgdb) } catch (ex: Exception) { null }
            }
            terminarTrabajo("uno")
            if (r == null || !r.encontrado) {
                avisar("No encontré imágenes para ${j.titulo}")
                return@launch
            }
            medios = medios + (crudo.clave to r.medios)
            withContext(Dispatchers.IO) { almacen.guardarMedios(medios) }
            publicarJuegos()
            avisar("¡Listo! Imágenes nuevas para ${r.medios.titulo ?: j.titulo}")
        }
    }
    // ---------- Trabajos en segundo plano ----------

    private val trabajosMapa = LinkedHashMap<String, Trabajo>()

    /** Anuncia (o actualiza) algo que se hace de fondo. Aparece arriba en la pantalla principal. */
    fun trabajo(id: String, texto: String, avance: Float? = null) {
        trabajosMapa[id] = Trabajo(id, texto, avance)
        actualizar { it.copy(trabajos = trabajosMapa.values.toList()) }
    }

    fun terminarTrabajo(id: String) {
        if (trabajosMapa.remove(id) != null) actualizar { it.copy(trabajos = trabajosMapa.values.toList()) }
    }

    fun abrirTrabajos() {
        actualizar { it.copy(panel = if (it.panel == Panel.TRABAJOS) Panel.NINGUNO else Panel.TRABAJOS, indice = 0) }
    }

    // ---------- Reloj grande ----------

    fun alternarReloj() = actualizar { it.copy(relojGrande = !it.relojGrande) }

    /** El botón Inicio de Android con Díptico como launcher: cierra todo y vuelve al inicio. */
    fun alInicio() = actualizar { it.copy(panel = Panel.NINGUNO, relojGrande = false, editandoWidgets = false, pregunta = false) }

    // ---------- Guardar listas en JSON ----------

    private fun leerTemas(s: String): List<Tema> = try {
        val a = org.json.JSONArray(s)
        (0 until a.length()).map { Tema.deJson(a.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    private fun escribirTemas(l: List<Tema>): String = org.json.JSONArray().apply { l.forEach { put(it.json()) } }.toString()

    private fun leerWidgets(s: String): List<Widget> = try {
        val a = org.json.JSONArray(s)
        if (a.length() > 0 && a.getJSONObject(0).has("tam")) throw IllegalStateException("formato antiguo")
        val leidos = (0 until a.length()).map {
            a.getJSONObject(it).let { o ->
                Widget(
                    o.getString("tipo"), o.getInt("x"), o.getInt("y"), o.getInt("an"), o.getInt("al"), o.optBoolean("visible", true),
                    o.optInt("pagina", 0), o.optString("uri", "").ifEmpty { null }, o.optString("estilo", "tarjeta"),
                    o.optLong("color", 0L), o.optDouble("opacidad", 1.0).toFloat(), o.optInt("radio", 22),
                    o.optInt("variante", 0), o.optDouble("escala", 1.0).toFloat(),
                )
            }
        }
        // Si una versión nueva trae widgets nuevos, se agregan ocultos.
        leidos + Widgets.porDefecto.filter { d -> leidos.none { it.tipo == d.tipo } }.map { it.copy(visible = false) }
    } catch (e: Exception) {
        Widgets.porDefecto
    }

    private fun escribirWidgets(l: List<Widget>): String = org.json.JSONArray().apply {
        l.forEach {
            val o = org.json.JSONObject().put("tipo", it.tipo).put("x", it.x).put("y", it.y).put("an", it.an).put("al", it.al)
                .put("visible", it.visible).put("pagina", it.pagina).put("estilo", it.estilo)
                .put("color", it.color).put("opacidad", it.opacidad.toDouble()).put("radio", it.radio)
                .put("variante", it.variante).put("escala", it.escala.toDouble())
            it.uri?.let { u -> o.put("uri", u) }
            put(o)
        }
    }.toString()

    private fun leerMapa(s: String): Map<String, String> = try {
        val o = org.json.JSONObject(s)
        o.keys().asSequence().associateWith { o.getString(it) }
    } catch (e: Exception) {
        emptyMap()
    }

    private fun escribirMapa(m: Map<String, String>): String = org.json.JSONObject(m as Map<*, *>).toString()

    // ---------- Temas ----------

    fun todosLosTemas(e: Estado = estado.value): List<Tema> = Temas.incluidos + e.temasPropios

    /** Aplica el tema elegido, o el de la fiesta de hoy si está activado. */
    fun aplicarTema() {
        val e = estado.value
        val fiesta = if (e.temaFestivo) Temas.deHoy() else null
        val elegido = todosLosTemas(e).firstOrNull { it.id == e.tema }
            ?: Temas.fiestas.firstOrNull { it.tema.id == e.tema }?.tema
            ?: Temas.incluidos[0]
        val activo = fiesta?.tema ?: elegido
        cl.nikalbazu.diptico.ui.Estilo.tema = activo
        cl.nikalbazu.diptico.ui.Estilo.decoraciones = e.decoraciones
        Sonidos.juego = activo.sonidos
        val pista = musicaDeTema(activo)
        if (fiesta?.id != e.fiesta?.id || pista != e.pistaTema) actualizar { it.copy(fiesta = fiesta, pistaTema = pista) }
    }

    fun elegirTema(id: String) {
        almacen.guardarTexto("tema", id)
        actualizar { it.copy(tema = id) }
        aplicarTema()
        val e = estado.value
        if (e.fiesta != null) avisar("Hoy manda el tema de ${e.fiesta.nombre}. Apaga \"Fechas especiales\" para usar este.")
    }

    private fun guardarTemasPropios(l: List<Tema>) {
        almacen.guardarTexto("temasPropios", escribirTemas(l))
        actualizar { it.copy(temasPropios = l) }
    }

    /** Edita "Mi tema" (lo crea a partir del tema actual si no existe). */
    private fun editarMiTema(f: (Tema) -> Tema) {
        val e = estado.value
        val actual = cl.nikalbazu.diptico.ui.Estilo.tema
        val mio = e.temasPropios.firstOrNull { it.id == "mio" }
            ?: actual.copy(id = "mio", nombre = "Mi tema", propio = true)
        val nuevo = f(mio)
        guardarTemasPropios(listOf(nuevo) + e.temasPropios.filter { it.id != "mio" })
        if (e.tema != "mio") elegirTema("mio") else aplicarTema()
    }

    private fun siguienteColor(c: Long): Long {
        val m = Temas.MUESTRAS
        return m[(m.indexOf(c) + 1).mod(m.size)]
    }

    fun crearTemaIA(pedido: String) {
        val clave = llaveNube() ?: return pedirIANube("Crear temas")
        trabajo("tema", "Creando tu tema con IA")
        avisar("Pidiendo un tema a la IA…")
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                try {
                    Result.success(Temas.conIA(clave, pedido))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            terminarTrabajo("tema")
            r.onSuccess { t ->
                guardarTemasPropios(listOf(t) + estado.value.temasPropios)
                elegirTema(t.id)
                avisar(if (t.mensaje.isNotBlank()) "${t.nombre}: ${t.mensaje}" else "¡Listo! Tema \"${t.nombre}\" creado")
            }.onFailure { avisar(it.message ?: "No pude crear el tema. Revisa tu conexión.") }
        }
    }

    private fun opcionesTemas(e: Estado): List<Opcion> {
        val lista = mutableListOf<Opcion>()
        val prox = Temas.proxima()
        lista += Opcion(
            "Fechas especiales", if (e.temaFestivo) "Sí" else "No", Icono.MUNDO,
            "Cambia el tema solo en más de 20 fiestas del mundo: Año Nuevo Lunar, Holi, Diwali, Halloween, Día de Muertos, " +
                "Fiestas Patrias, Navidad y más." + (prox?.let { " La próxima: ${it.first.nombre} (${it.first.donde})." } ?: ""),
        ) {
            val v = !estado.value.temaFestivo
            almacen.guardarBool("temaFestivo", v)
            actualizar { it.copy(temaFestivo = v) }
            aplicarTema()
            estado.value.fiesta?.let { f -> avisar(f.tema.mensaje.ifBlank { "¡Hoy es ${f.nombre}!" }) }
        }
        e.fiesta?.let { f ->
            lista += Opcion("Hoy: ${f.nombre}", f.donde, Icono.ESTRELLA, f.tema.mensaje) { avisar(f.tema.mensaje.ifBlank { f.nombre }) }
        }
        lista += Opcion(
            "Crear tema con IA", if (llaveNube(e) == null) "Necesita una IA en la nube" else "Escribe tu idea", Icono.CHISPA,
            "Escribe algo como \"Halloween japonés\" o \"Navidad en la playa\", o déjalo vacío y la IA elige la fiesta del mundo más cercana a hoy.",
        ) { if (llaveNube() == null) pedirIANube("Crear temas") else abrirTexto("ia_pedido") }
        lista.add(
            0,
            Opcion(
                "Elegir tema", cl.nikalbazu.diptico.ui.Estilo.tema.nombre, Icono.PALETA,
                "Una galería con todos los temas, separados en pestañas: colores, fiestas del mundo y los tuyos. Se ve cómo queda antes de elegir.",
            ) { abrirGaleria("temas") },
        )
        lista.add(
            1,
            Opcion(
                "Mis colores", "Toda la interfaz", Icono.PALETA,
                "Elige tú el color principal, el secundario, los detalles, el fondo y las letras de todo Díptico.",
            ) { abrirGaleria("colores") },
        )
        val mio = e.temasPropios.firstOrNull { it.id == "mio" }
        lista += Opcion("Mi tema · fondo", when (mio?.base) { "oscuro" -> "Oscuro"; "claro" -> "Claro"; "oled" -> "Negro puro"; else -> "Según el modo" }, Icono.TEMA) {
            editarMiTema { it.copy(base = when (it.base) { "auto" -> "oscuro"; "oscuro" -> "oled"; "oled" -> "claro"; else -> "auto" }) }
        }
        lista += Opcion("Mi tema · decoración", Temas.nombreDecor(mio?.decor ?: "nada"), Icono.ESTRELLA) {
            editarMiTema { t -> t.copy(decor = Temas.DECORACIONES[(Temas.DECORACIONES.indexOf(t.decor) + 1).mod(Temas.DECORACIONES.size)]) }
        }
        lista += Opcion(
            "IA para crear temas", llaveNube(e)?.let { IA.nombre(it.proveedor) + " ✓" } ?: "Elegir", Icono.CLAVE,
            "Los temas con IA usan una IA en la nube: Gemini (gratis con tu cuenta de Google), ChatGPT o Claude.",
        ) { abrirGaleria("ia") }
        if (e.temasPropios.any { it.id != "mio" }) {
            lista += Opcion("Borrar temas creados con IA", icono = Icono.BORRAR) {
                guardarTemasPropios(estado.value.temasPropios.filter { it.id == "mio" })
                if (estado.value.tema.startsWith("ia_")) elegirTema("diptico")
            }
        }
        return lista
    }

    // ---------- Íconos de consola ----------

    private fun estilosIconos(): List<String> = listOf("diptico", "siglas") + Descargas.PAQUETES.map { it.id }

    fun nombreEstiloIconos(s: String) = when (s) {
        "diptico" -> "Formatos de Díptico"
        "siglas" -> "Siglas"
        else -> Descargas.PAQUETES.firstOrNull { it.id == s }?.nombre ?: s
    }

    private fun opcionesIconos(e: Estado): List<Opcion> {
        val c = Consolas.porId(e.consolaSel)
        val lista = mutableListOf(

            Opcion(
                "Íconos de consola", nombreEstiloIconos(e.estiloIconos), Icono.EMULADOR,
                "Los dibujos de Díptico, siglas, o paquetes de íconos de RetroArch (se descargan una vez).",
            ) {
                val l = estilosIconos()
                val s = l[(l.indexOf(estado.value.estiloIconos) + 1).mod(l.size)]
                almacen.guardarTexto("estiloIconos", s)
                actualizar { it.copy(estiloIconos = s) }
                bajarIconosFaltantes()
            },
        )
        if (c != null) {
            lista += Opcion(
                "Ícono propio para ${c.nombre}", if (e.iconosPropios.containsKey(c.id)) "Cambiar imagen" else "Elegir imagen", Icono.PORTADA,
                "Usa cualquier imagen de tu teléfono como ícono de esta consola. Elige la consola en el estante antes de abrir el menú.",
            ) { pedirImagen("icono:${c.id}") }
            if (e.iconosPropios.containsKey(c.id)) {
                lista += Opcion("Quitar ícono propio de ${c.nombre}", icono = Icono.BORRAR) {
                    val m = estado.value.iconosPropios - c.id
                    almacen.guardarTexto("iconosPropios", escribirMapa(m))
                    actualizar { it.copy(iconosPropios = m) }
                }
            }
        }
        return lista
    }

    /** Si el estilo es un paquete de RetroArch, baja los íconos que falten de tus consolas. */
    private fun bajarIconosFaltantes() {
        val e = estado.value
        val paquete = Descargas.PAQUETES.firstOrNull { it.id == e.estiloIconos } ?: return
        val consolas = e.consolasConJuegos.ifEmpty { Consolas.todas.take(24) }
            .filter { !Descargas.archivoIcono(app, paquete.id, it.id).exists() }
        if (consolas.isEmpty()) return
        trabajo("iconos", "Bajando íconos")
        scope.launch {
            val n = withContext(Dispatchers.IO) {
                try {
                    Descargas.bajarIconos(app, paquete.id, consolas) { h, t ->
                        scope.launch { trabajo("iconos", "Bajando íconos", if (t == 0) null else h / t.toFloat()) }
                    }
                } catch (ex: Exception) {
                    -1
                }
            }
            terminarTrabajo("iconos")
            actualizar { it.copy(iconosVersion = it.iconosVersion + 1) }
            if (n < 0) avisar("No pude bajar los íconos. Revisa tu conexión.")
        }
    }

    // ---------- Inicio personalizado ----------

    private fun opcionesInicio(e: Estado): List<Opcion> = listOf(
        Opcion(
            "Bienvenida al encender", nombreBienvenida(e.bienvenidaModo), Icono.CHISPA,
            "Al prender la consola: un saludo con la hora, tu último juego para seguir y Bisa. Si estabas jugando, un recordatorio de dónde ibas. Nada se detiene mientras tanto.",
        ) { cambiarBienvenidaModo() },
        Opcion("Tu nombre", e.nombre.ifBlank { "Sin nombre" }, Icono.TEXTO, "Para que Bisa te salude por tu nombre.") { abrirTexto("nombre", estado.value.nombre) },
        Opcion(
            "Estilo del inicio", if (e.inicio == "widgets") "Con widgets" else "Carátulas", Icono.APPS,
            "Carátulas: el arte del juego ocupa la pantalla. Con widgets: tu inicio con reloj, música, recientes y más. " +
                "Se cambia rápido con Select.",
        ) { alternarInicio() },
        Opcion(
            "Estudio de widgets", "${e.widgets.count { it.visible }} a la vista", Icono.LAPIZ,
            "Todo en un lugar: tamaño, forma, estilo, color, diseño y página de cada widget. También manteniendo apretado el inicio.",
        ) { editarWidgets() },
        Opcion("Agregar widgets", icono = Icono.APPS, ayuda = "El catálogo de widgets: reloj, música, recientes, Bisa y más.") { abrirCatalogoWidgets() },
        Opcion("Volver al orden original", icono = Icono.BORRAR, ayuda = "Deja los widgets como vienen de fábrica.") {
            guardarWidgets(Widgets.porDefecto)
            avisar("Widgets como al principio")
        },
    ) + opcionesGrilla(e) + listOf(
        Opcion(
            "Permiso para el widget de música", if (Medios.tienePermiso(app)) "Listo ✓" else "Dar permiso", Icono.MUSICA,
            "Para mostrar y controlar lo que suena en Spotify, YouTube Music u otra app, Android pide activar el \"acceso a notificaciones\" de Díptico.",
        ) { if (!Medios.tienePermiso(app)) Medios.pedirPermiso(app) else avisar("El widget de música ya tiene permiso") },
    )

    /** Select: cambia entre el inicio con widgets y el de carátulas. */
    fun alternarInicio() {
        val v = if (estado.value.inicio == "widgets") "heroe" else "widgets"
        almacen.guardarTexto("inicio", v)
        actualizar { it.copy(inicio = v, editandoWidgets = false) }
    }

    private fun guardarWidgets(l: List<Widget>) {
        almacen.guardarTexto("widgets", escribirWidgets(l))
        actualizar { it.copy(widgets = l) }
    }

    /** Entra al modo de acomodar widgets (en la pantalla del inicio). */
    fun editarWidgets() {
        almacen.guardarTexto("inicio", "widgets")
        val primero = estado.value.widgets.indexOfFirst { it.visible }.coerceAtLeast(0)
        actualizar { it.copy(inicio = "widgets", editandoWidgets = true, widgetSel = primero, panel = Panel.ESTUDIO, estudioPestana = 0, estudioMover = false, indice = 0) }
    }

    fun terminarEdicion() {
        almacen.guardarTexto("widgets", escribirWidgets(estado.value.widgets))
        actualizar { it.copy(editandoWidgets = false, estudioMover = false, panel = if (it.panel == Panel.ESTUDIO) Panel.NINGUNO else it.panel) }
        avisar("Inicio guardado")
    }

    private fun cambiarWidget(i: Int, f: (Widget) -> Widget) {
        val l = estado.value.widgets.toMutableList()
        val w = l.getOrNull(i) ?: return
        l[i] = f(w)
        actualizar { it.copy(widgets = l) }
    }

    /** ¿El widget [w] (en la posición [i] de la lista) se monta sobre otro de su página? */
    private fun choca(w: Widget, i: Int, l: List<Widget> = estado.value.widgets): Boolean = l.withIndex().any { (k, o) ->
        k != i && o.visible && o.pagina == w.pagina &&
            w.x < o.x + o.an && o.x < w.x + w.an && w.y < o.y + o.al && o.y < w.y + w.al
    }

    private fun encajar(w: Widget): Widget {
        val an = w.an.coerceIn(1, Widgets.COLUMNAS)
        val al = w.al.coerceIn(1, Widgets.FILAS)
        return w.copy(an = an, al = al, x = w.x.coerceIn(0, Widgets.COLUMNAS - an), y = w.y.coerceIn(0, Widgets.FILAS - al))
    }

    fun moverWidgetSel(dx: Int, dy: Int) {
        val e = estado.value
        val i = e.widgetSel
        cambiarWidget(i) { w ->
            val nx = w.x + dx
            when {
                // Al salir por un costado pasa a la página de al lado (si allá hay espacio).
                nx < 0 && w.pagina > 0 -> encajar(w.copy(pagina = w.pagina - 1, x = Widgets.COLUMNAS - w.an)).takeIf { !choca(it, i) } ?: w
                nx + w.an > Widgets.COLUMNAS && w.pagina < e.paginas - 1 -> encajar(w.copy(pagina = w.pagina + 1, x = 0)).takeIf { !choca(it, i) } ?: w
                else -> {
                    // Salta por encima de los otros widgets hasta el primer lugar libre en esa dirección.
                    var paso = 1
                    var r: Widget? = null
                    while (paso <= maxOf(Widgets.COLUMNAS, Widgets.FILAS)) {
                        val c = encajar(w.copy(x = w.x + dx * paso, y = w.y + dy * paso))
                        if (c.x == w.x && c.y == w.y) break
                        if (!choca(c, i)) { r = c; break }
                        paso++
                    }
                    r ?: w
                }
            }
        }
        estado.value.widgets.getOrNull(e.widgetSel)?.let { w -> if (w.pagina != e.paginaInicio) actualizar { it.copy(paginaInicio = w.pagina) } }
    }

    /** Para arrastrar con el dedo: deja el widget en la celda indicada. */
    fun ponerWidget(i: Int, x: Int, y: Int) = cambiarWidget(i) { w ->
        val c = encajar(w.copy(x = x, y = y))
        if (choca(c, i)) { avisar("Ahí hay otro widget"); w } else c
    }

    /** Cambia el ancho (L1/R1) o el alto (L2/R2) del widget elegido, sin montarse sobre otros. */
    fun cambiarTamWidget(dAn: Int, dAl: Int) {
        val i = estado.value.widgetSel
        cambiarWidget(i) { w ->
            val an = (w.an + dAn).coerceIn(1, Widgets.COLUMNAS)
            val al = (w.al + dAl).coerceIn(1, Widgets.FILAS)
            // Si no cabe a la derecha o abajo, se corre hacia la izquierda o arriba.
            val c = w.copy(an = an, al = al, x = w.x.coerceAtMost(Widgets.COLUMNAS - an), y = w.y.coerceAtMost(Widgets.FILAS - al))
            if (choca(c, i)) { avisar("No cabe: hay otro widget al lado"); w } else c
        }
    }

    fun tamanoWidgetSel(delta: Int) = cambiarWidget(estado.value.widgetSel) { w ->
        val t = Widgets.TAMANOS
        val i = estado.value.widgetSel
        val actual = t.indexOfFirst { it.first == w.an && it.second == w.al }
        var k = actual
        var r = w
        repeat(t.size) {
            k = (k + delta).mod(t.size)
            val (an, al) = t[k]
            val c = encajar(w.copy(an = an, al = al))
            if (!choca(c, i)) { r = c; return@cambiarWidget r }
        }
        r
    }

    /** Cambia el estilo del widget elegido: tarjeta, vidrio, color, contorno o sin fondo. */
    fun cambiarEstiloWidget(i: Int = estado.value.widgetSel) {
        cambiarWidget(i) { w -> w.copy(estilo = Widgets.ESTILOS[(Widgets.ESTILOS.indexOf(w.estilo) + 1).mod(Widgets.ESTILOS.size)]) }
        estado.value.widgets.getOrNull(i)?.let { avisar("Estilo: ${Widgets.nombreEstilo(it.estilo)}") }
    }

    fun siguienteWidget(delta: Int) {
        val l = estado.value.widgets
        val visibles = l.indices.filter { l[it].visible }
        if (visibles.isEmpty()) return
        val pos = visibles.indexOf(estado.value.widgetSel)
        actualizar { it.copy(widgetSel = visibles[(pos + delta).mod(visibles.size)]) }
    }

    fun elegirWidget(i: Int) = actualizar { it.copy(widgetSel = i) }

    /** Muestra u oculta un widget. Al mostrarlo, lo pone en el primer hueco libre. */
    private fun alternarWidget(i: Int) {
        val l = estado.value.widgets
        val w = l.getOrNull(i) ?: return
        if (w.visible) {
            cambiarWidget(i) { it.copy(visible = false) }
        } else {
            val ocupadas = HashSet<Pair<Int, Int>>()
            l.filter { it.visible && it.pagina == estado.value.paginaInicio }.forEach { o -> for (x in o.x until o.x + o.an) for (y in o.y until o.y + o.al) ocupadas += x to y }
            // Busca un hueco en la página actual (achicándolo si hace falta) y si no, en las otras páginas.
            val e = estado.value
            var puesto: Widget? = null
            val paginas = listOf(e.paginaInicio) + (0 until e.paginas).filter { it != e.paginaInicio }
            busqueda@ for (pag in paginas) for ((an, al) in listOf(w.an to w.al, 2 to 2, 2 to 1, 1 to 1)) {
                for (y in 0..Widgets.FILAS - al) for (x in 0..Widgets.COLUMNAS - an) {
                    val c = w.copy(visible = true, x = x, y = y, an = an, al = al, pagina = pag)
                    if (!choca(c, i, l)) { puesto = c; break@busqueda }
                }
            }
            if (puesto == null) { avisar("No hay espacio. Agrega una página o achica otro widget."); return }
            cambiarWidget(i) { puesto }
            if (puesto.pagina != e.paginaInicio) actualizar { it.copy(paginaInicio = puesto.pagina) }
        }
        almacen.guardarTexto("widgets", escribirWidgets(estado.value.widgets))
    }

    // ---------- Ficha del juego ----------

    val PESTANAS_FICHA = listOf("Resumen", "Historia", "Guías y trucos", "Personalizar", "Logros")

    fun abrirFicha() {
        val e = estado.value
        val j = e.seleccionado ?: return
        val misma = e.fichaDe == j.clave
        actualizar {
            it.copy(
                panel = Panel.FICHA, pestanaFicha = if (misma) it.pestanaFicha else 0, indice = 0, fichaDe = j.clave,
                ficha = if (misma) it.ficha else null,
                fichaImagenes = if (misma) it.fichaImagenes else emptyList(),
                logrosFicha = if (misma) it.logrosFicha else null,
                errorLogros = if (misma) it.errorLogros else null,
            )
        }
        if (misma && e.ficha != null) return
        val consola = Consolas.porId(j.consolaId)?.bonito ?: ""
        actualizar { it.copy(cargandoFicha = true) }
        trabajo("ficha", "Buscando la historia de ${j.titulo}")
        scope.launch {
            val info = withContext(Dispatchers.IO) { try { Fichas.buscar(app, j.titulo, if (consola == "Juegos de Android") "" else consola) } catch (ex: Exception) { null } }
            terminarTrabajo("ficha")
            if (estado.value.fichaDe == j.clave) actualizar { it.copy(ficha = info, cargandoFicha = false) }
            // Fotos para el carrusel: lo propio primero, después internet.
            val fotos = withContext(Dispatchers.IO) {
                val propias = listOfNotNull(j.arte, j.portada?.takeIf { it.scheme != "app" }).map { u ->
                    cl.nikalbazu.diptico.data.ImagenWeb(u.toString(), u.toString(), "Tu biblioteca")
                }
                val crudo = todosCrudos().firstOrNull { it.clave == j.clave }
                val c = Consolas.porId(j.consolaId)
                val retro = if (crudo != null && c != null) try { Descargas.fotosLibretro(app, crudo, c) } catch (ex: Exception) { emptyList() } else emptyList()
                (propias + retro + (info?.imagenes ?: emptyList())).distinctBy { it.url }
            }
            if (estado.value.fichaDe == j.clave) actualizar { it.copy(fichaImagenes = fotos) }
        }
    }

    fun cambiarPestanaFicha(delta: Int) {
        actualizar { it.copy(pestanaFicha = (it.pestanaFicha + delta).mod(PESTANAS_FICHA.size), indice = 0) }
        if (estado.value.pestanaFicha == 4) cargarLogros()
    }

    fun elegirPestanaFicha(i: Int) {
        actualizar { it.copy(pestanaFicha = i, indice = 0) }
        if (i == 4) cargarLogros()
    }

    // ---------- Asistente ----------

    fun cambiarIdioma(cod: String) {
        almacen.guardarTexto("idioma", cod)
        Idioma.fijar(app, cod)
        actualizar { it.copy(idioma = cod, fichaDe = null, ficha = null) }
        avisar(Idioma.IDIOMAS.first { it.first == cod }.second)
    }

    fun abrirAsistente() {
        actualizar { it.copy(panel = Panel.ASISTENTE, indice = 0) }
    }

    private var claveParaAsistente = false

    fun escribirAlAsistente() {
        if (motorEfectivo() == "ninguno") { abrirGaleria("ia"); avisar("Primero elige el cerebro de Bisa"); return }
        abrirTexto("asistente")
    }

    fun borrarChat() {
        actualizar { it.copy(chat = emptyList(), indice = 0) }
    }

    /** Lo que se puede elegir con la cruceta en el asistente: escribir, los botones de la última respuesta o las sugerencias. */
    fun elementosAsistente(): List<Opcion> {
        val e = estado.value
        val lista = mutableListOf(Opcion("Escribir una pregunta", icono = Icono.TEXTO) { escribirAlAsistente() })
        val ultima = e.chat.lastOrNull()
        if (ultima?.rol == "ia") ultima.acciones.forEach { a -> lista += Opcion(a.etiqueta, a.tipo, Icono.CHISPA) { ejecutarAccion(a) } }
        if (e.chat.isEmpty() || !e.pensando && ultima?.acciones.isNullOrEmpty()) {
            val sug = if (e.chat.isEmpty()) Asistente.sugerencias else Asistente.sugerencias.shuffled(java.util.Random(e.chat.size.toLong())).take(3)
            sug.forEach { s -> lista += Opcion(s, "sugerencia", Icono.ASISTENTE) { preguntar(s) } }
        }
        return lista
    }

    /** La clave guardada para una IA en la nube. */
    fun claveDe(p: String, e: Estado = estado.value) = when (p) { "gemini" -> e.claveGemini; "openai" -> e.claveOpenAI; else -> e.claveIa }

    /** Qué IA usa Bisa ahora: local (en la consola), gemini, openai, claude o "ninguno". */
    fun motorEfectivo(e: Estado = estado.value): String = when (e.motorIA) {
        "local", "gemini", "openai", "claude" -> e.motorIA
        else -> when {
            IALocal.listo(app) -> "local"
            else -> IA.PROVEEDORES.firstOrNull { claveDe(it, e).isNotBlank() } ?: "ninguno"
        }
    }

    fun nombreMotor(m: String) = when (m) {
        "local" -> "En tu consola"; "gemini" -> "Gemini"; "openai" -> "ChatGPT"; "claude" -> "Claude"; "ninguno" -> "Sin IA aún"; else -> "Automático"
    }

    /** Valor que se muestra en el menú: el motor elegido (y, en automático, cuál está usando). */
    fun textoMotor(e: Estado = estado.value): String = when {
        e.motorIA != "auto" -> nombreMotor(e.motorIA)
        motorEfectivo(e) == "ninguno" -> "Sin elegir"
        else -> "Auto · " + nombreMotor(motorEfectivo(e))
    }

    /** Una IA en la nube con clave, para temas y nombres: la de Bisa si es de la nube, si no la primera con clave. */
    fun llaveNube(e: Estado = estado.value): Llave? {
        val m = motorEfectivo(e)
        if (m in IA.PROVEEDORES && claveDe(m, e).isNotBlank()) return Llave(m, claveDe(m, e))
        return IA.PROVEEDORES.firstOrNull { claveDe(it, e).isNotBlank() }?.let { Llave(it, claveDe(it, e)) }
    }

    /** Para algo que necesita IA en la nube y no hay ninguna clave: abre el selector de IA con una explicación. */
    private fun pedirIANube(para: String) {
        abrirGaleria("ia")
        avisar("$para necesita una IA en la nube. Gemini tiene un nivel gratis: elígela y pega tu clave.")
    }

    fun elegirMotor(m: String) {
        almacen.guardarTexto("motorIA", m)
        actualizar { it.copy(motorIA = m) }
    }

    /** Elegir una IA en el selector: si le falta algo (bajarla o la clave), lo pide. */
    fun elegirIA(m: String) {
        val e = estado.value
        when {
            m == "local" && !IALocal.listo(app) -> { elegirMotor("local"); descargarIALocal() }
            m in IA.PROVEEDORES && claveDe(m, e).isBlank() -> { elegirMotor(m); claveVieneDeGaleria = true; abrirTexto(claveTexto(m)) }
            else -> { elegirMotor(m); avisar("Bisa ahora usa: ${nombreMotor(m)}") }
        }
    }

    private var claveVieneDeGaleria = false

    fun claveTexto(p: String) = when (p) { "gemini" -> "ia_clave_gemini"; "openai" -> "ia_clave_openai"; else -> "ia_clave" }

    fun preguntar(texto: String) {
        val e = estado.value
        if (e.pensando) return
        val motor = motorEfectivo(e)
        if (motor == "ninguno" || (motor == "local" && !IALocal.listo(app))) {
            abrirGaleria("ia")
            avisar("Elige el cerebro de Bisa: la IA de tu consola (gratis, sin internet) o una en la nube como Gemini")
            return
        }
        if (motor in IA.PROVEEDORES && claveDe(motor, e).isBlank()) {
            claveParaAsistente = true; abrirTexto(claveTexto(motor))
            avisar("Para usar ${nombreMotor(motor)} pega tu clave, o elige otra IA en Menú › Bisa.")
            return
        }
        val chat = e.chat + MensajeChat("yo", texto)
        // La respuesta aparece mientras se escribe (con la IA local).
        actualizar { it.copy(chat = chat, pensando = true, indice = 0) }
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                try {
                    val historia = chat.takeLast(12).filter { it.texto.isNotBlank() }.map { (if (it.rol == "yo") "user" to it.texto else "assistant" to it.texto) }
                    if (motor == "local") {
                        val sistema = Asistente.sistemaLocal(estado.value)
                        var ultimo = 0L
                        Result.success(
                            IALocal.responder(app, sistema, historia) { parcial ->
                                val ahora = System.currentTimeMillis()
                                if (ahora - ultimo > 120) {
                                    ultimo = ahora
                                    val visible = parcial.substringBefore("[[").trim()
                                    actualizar { it.copy(chat = chat + MensajeChat("ia", visible)) }
                                }
                            },
                        )
                    } else {
                        val sistema = Asistente.sistema(estado.value)
                        Result.success(IA.conversar(Llave(motor, claveDe(motor)), sistema, historia, 1200, listos = true, buscarWeb = estado.value.bisaWeb))
                    }
                } catch (ex: Exception) {
                    Result.failure(ex)
                }
            }
            val msg = r.fold({ Asistente.interpretar(it) }, { MensajeChat("ia", "⚠ " + (it.message ?: "No pude hablar con la IA.")) })
            actualizar { it.copy(chat = chat + msg, pensando = false, indice = if (msg.acciones.isNotEmpty()) 1 else 0) }
        }
    }

    /** Baja la IA local (unos 1,6 GB). Se puede cortar y seguir después. */
    fun descargarIALocal() {
        if (IALocal.listo(app)) { avisar("La IA local ya está lista"); return }
        if (trabajosMapa.containsKey("ialocal")) return
        trabajo("ialocal", "Bajando la IA de Bisa", IALocal.bajado(app) / IALocal.TAMANO.toFloat())
        avisar("Bajando la IA de Bisa (1,6 GB). Puedes seguir usando Díptico.")
        scope.launch {
            val error = withContext(Dispatchers.IO) {
                IALocal.descargar(app) { a -> scope.launch { trabajo("ialocal", "Bajando la IA de Bisa", a) } }
            }
            terminarTrabajo("ialocal")
            if (error != null) avisar(error) else {
                avisar("¡Listo! Bisa ya piensa dentro de tu consola, sin internet")
                if (estado.value.motorIA != "local") elegirMotor("auto")
            }
            actualizar { it.copy(iaLocalLista = IALocal.listo(app)) }
        }
    }

    /** Hace lo que propuso el asistente. */
    fun ejecutarAccion(a: AccionChat) {
        when (a.tipo) {
            "preguntar" -> preguntar(a.valor)
            "pista" -> preguntar(if (a.valor == "solucion") "Dime la solución completa, paso a paso." else "Dame una pista más directa.")
            "jugar", "ver", "ficha" -> {
                if (estado.value.juegos.none { it.clave == a.valor }) { avisar("No encontré ese juego"); return }
                actualizar { it.copy(consolaSel = "todo", juegoSel = a.valor, panel = Panel.NINGUNO) }
                when (a.tipo) { "jugar" -> jugar(); "ficha" -> abrirFicha() }
            }
            "tema" -> if (todosLosTemas().any { it.id == a.valor }) { elegirTema(a.valor); avisar("Tema: ${todosLosTemas().first { it.id == a.valor }.nombre}") } else avisar("No encontré ese tema")
            "vista" -> if (a.valor in VISTAS) {
                almacen.guardarTexto("vista", a.valor)
                actualizar { it.copy(vista = a.valor, panel = Panel.NINGUNO) }
            }
            "buscar" -> actualizar { it.copy(panel = Panel.BUSCAR, busqueda = a.valor.take(40), indice = 0) }
            "menu" -> Seccion.values().firstOrNull { it.name.equals(a.valor, true) }?.let { abrirMenu(it) }
        }
    }

    // ---------- RetroAchievements ----------

    private val idsRA = HashMap<String, Int?>()

    /** Trae los logros del juego de la ficha (si tienes cuenta conectada). */
    fun cargarLogros(forzar: Boolean = false) {
        val e = estado.value
        val j = juegoFicha() ?: return
        if (e.usuarioRA.isBlank() || e.claveRA.isBlank()) { actualizar { it.copy(logrosFicha = null, errorLogros = null) }; return }
        if (!forzar && e.logrosFicha != null && e.logrosFicha.titulo.isNotEmpty() && idsRA[j.clave] == e.logrosFicha.idJuego) return
        if (!cl.nikalbazu.diptico.data.Logros.tieneLogros(j.consolaId)) {
            actualizar { it.copy(logrosFicha = null, errorLogros = "RetroAchievements no tiene logros para esta consola.") }
            return
        }
        actualizar { it.copy(cargandoLogros = true, errorLogros = null, logrosFicha = null) }
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                try {
                    val crudo = todosCrudos().firstOrNull { it.clave == j.clave }
                    val id = idsRA.getOrPut(j.clave) {
                        cl.nikalbazu.diptico.data.Logros.buscarJuego(app, j.consolaId, listOfNotNull(j.titulo, crudo?.titulo, crudo?.archivo?.substringBeforeLast('.')), e.claveRA)
                    } ?: return@withContext Result.failure<cl.nikalbazu.diptico.data.LogrosJuego>(IllegalStateException("No encontré \"${j.titulo}\" en RetroAchievements. Prueba cambiando el título en Personalizar."))
                    Result.success(cl.nikalbazu.diptico.data.Logros.deJuego(id, e.usuarioRA, e.claveRA))
                } catch (ex: Exception) {
                    Result.failure(ex)
                }
            }
            if (estado.value.fichaDe != j.clave) return@launch
            actualizar {
                it.copy(cargandoLogros = false, logrosFicha = r.getOrNull(), errorLogros = r.exceptionOrNull()?.message)
            }
        }
    }

    /** Revisa la cuenta y trae tu perfil y tus últimos logros. */
    fun cargarPerfilRA(avisarSiOk: Boolean = false) {
        val e = estado.value
        if (e.usuarioRA.isBlank() || e.claveRA.isBlank()) return
        scope.launch {
            val r = withContext(Dispatchers.IO) { try { Result.success(cl.nikalbazu.diptico.data.Logros.perfil(e.usuarioRA, e.claveRA)) } catch (ex: Exception) { Result.failure(ex) } }
            r.onSuccess { p ->
                actualizar { it.copy(perfilRA = p) }
                if (avisarSiOk) avisar("¡Conectado! ${p.usuario} · ${p.puntos} puntos")
            }.onFailure { if (avisarSiOk) avisar(it.message ?: "No pude conectar con RetroAchievements") }
        }
    }

    private fun opcionesLogros(e: Estado): List<Opcion> = listOf(
        Opcion(
            "Tu usuario", e.usuarioRA.ifBlank { "Escribir" }, Icono.TITULO,
            "Tu nombre de usuario de retroachievements.org. Si no tienes cuenta, créala gratis en la web.",
        ) { abrirTexto("ra_usuario", estado.value.usuarioRA) },
        Opcion(
            "Tu clave (Web API Key)", if (e.claveRA.isBlank()) "Pegar" else "Guardada ✓", Icono.CLAVE,
            "En retroachievements.org entra a Settings (Ajustes) y copia tu \"Web API Key\". Solo se guarda en tu consola.",
        ) { abrirTexto("ra_clave") },
        Opcion(
            "Probar la conexión", e.perfilRA?.let { "${it.puntos} puntos" } ?: if (e.usuarioRA.isBlank()) "Falta tu cuenta" else "Probar", Icono.TROFEO,
            "Trae tu perfil y tus últimos logros.",
        ) { if (estado.value.usuarioRA.isBlank() || estado.value.claveRA.isBlank()) avisar("Primero escribe tu usuario y tu clave") else cargarPerfilRA(true) },
        Opcion("Conseguir mi clave", "Abre la web", Icono.WEB, "Abre retroachievements.org en el navegador para crear tu cuenta o copiar tu clave.") {
            abrirWeb("https://retroachievements.org/settings")
        },
        Opcion(
            "Cómo se ganan los logros", icono = Icono.INFO,
            ayuda = "Los logros se ganan jugando en RetroArch (o en otros emuladores compatibles) con tu cuenta iniciada allá: en RetroArch ve a Ajustes › Logros. Díptico te muestra cuáles tienes y cuáles faltan en la ficha de cada juego.",
        ) { avisar("En RetroArch: Ajustes › Logros, inicia sesión con tu misma cuenta") },
        Opcion("Desconectar la cuenta", icono = Icono.BORRAR, apagada = e.usuarioRA.isBlank()) {
            almacen.guardarTexto("usuarioRA", ""); almacen.guardarTexto("claveRA", "")
            actualizar { it.copy(usuarioRA = "", claveRA = "", perfilRA = null, logrosFicha = null) }
            avisar("Cuenta de RetroAchievements desconectada")
        },
    )

    private fun juegoFicha(): Juego? = estado.value.let { e -> e.juegos.firstOrNull { it.clave == e.fichaDe } }

    fun opcionesFicha(): List<Opcion> {
        val e = estado.value
        val j = juegoFicha() ?: return emptyList()
        val d = e.datos[j.clave] ?: DatosJuego()
        val consola = Consolas.porId(j.consolaId)
        return when (e.pestanaFicha) {
            2 -> buildList {
                add(
                    Opcion(
                        "Mis trucos y notas", d.notas.lineSequence().firstOrNull()?.take(40)?.ifEmpty { null } ?: "Escribir", Icono.LAPIZ,
                    ) { abrirTexto("notas", d.notas) },
                )
                Fichas.enlacesGuias(j.titulo, consola?.bonito ?: "").forEach { (t, url) ->
                    add(Opcion(t, "Abre el navegador", Icono.WEB) { abrirWeb(url) })
                }
                e.ficha?.enlace?.let { url -> add(Opcion("Artículo completo en Wikipedia", icono = Icono.WEB) { abrirWeb(url) }) }
            }
            3 -> listOf(
                Opcion("Título", d.titulo ?: j.titulo, Icono.TITULO) { abrirTexto("titulo", d.titulo ?: j.titulo) },
                Opcion("Portada", if (d.portada != null) "Propia ✓" else "Elegir en la tienda", Icono.PORTADA) { buscarCandidatos("portada") },
                Opcion("Imagen grande (hero)", if (d.arte != null) "Propia ✓" else "Elegir en la tienda", Icono.PORTADA) { buscarCandidatos("arte") },
                Opcion("Logo", if (d.logo != null) "Propio ✓" else "Elegir en la tienda", Icono.ESTRELLA) { buscarCandidatos("logo") },
                Opcion("Música del juego", if (d.musica != null) "Elegida ✓" else "Elegir canción", Icono.MUSICA) { pedirArchivoPara("musica", "audio/*") },
                Opcion(if (d.favorito) "Quitar de favoritos" else "Marcar como favorito", icono = Icono.CORAZON) { alternarFavorito(j.clave) },
                Opcion("Buscar todo automático", icono = Icono.DESCARGA) { descargarUno(j) },
                Opcion("Volver a lo original", "Quita lo personalizado", Icono.BORRAR) {
                    cambiarDatos(j.clave) { it.copy(titulo = null, portada = null, arte = null, logo = null, musica = null) }
                    avisar("Listo, ${j.titulo} volvió a como era")
                },
            )
            else -> emptyList()
        }
    }

    // ---------- Elegir una imagen de internet ----------

    fun nombreCandidato(t: String) = when (t) { "arte" -> "imagen grande"; "logo" -> "logo"; else -> "portada" }

    /** Busca varias opciones (libretro, SteamGridDB y Wikipedia) para la portada, el hero o el logo. */
    /** Fuentes de la tienda de imágenes (pestañas). */
    val FUENTES_IMAGEN = listOf("todas", "SteamGridDB", "libretro", "Wikipedia")
    fun nombreFuente(f: String) = if (f == "todas") "Todas" else f

    /** Lo que se ve en la tienda: atajos (mis archivos, quitar, conectar SteamGridDB) y las imágenes de la fuente elegida. */
    fun candidatosVisibles(e: Estado = estado.value): List<cl.nikalbazu.diptico.data.ImagenWeb> {
        val j = juegoFicha()
        val d = j?.let { e.datos[it.clave] }
        val propia = when (e.candidatoPara) { "arte" -> d?.arte; "logo" -> d?.logo; else -> d?.portada }
        val atajos = buildList {
            add(cl.nikalbazu.diptico.data.ImagenWeb("accion:archivos", "", "Mis archivos"))
            if (propia != null) add(cl.nikalbazu.diptico.data.ImagenWeb("accion:quitar", propia, "Quitar la propia"))
            if (e.claveSgdb.isBlank()) add(cl.nikalbazu.diptico.data.ImagenWeb("accion:sgdb", "", "Conectar SteamGridDB"))
        }
        val lista = if (e.fuenteImagen == "todas") e.candidatos else e.candidatos.filter { it.fuente == e.fuenteImagen }
        return atajos + lista
    }

    fun cambiarFuenteImagen(d: Int) {
        val l = FUENTES_IMAGEN
        actualizar { it.copy(fuenteImagen = l[(l.indexOf(it.fuenteImagen) + d).mod(l.size)], indice = 0) }
    }

    /** Abre la tienda de imágenes (libretro, SteamGridDB y Wikipedia) para la portada, el hero o el logo. */
    fun buscarCandidatos(tipo: String, nombre: String? = null) {
        val e = estado.value
        val j = juegoFicha() ?: return
        val crudo = todosCrudos().firstOrNull { it.clave == j.clave } ?: return
        val c = Consolas.porId(crudo.consolaId) ?: return
        val titulo = nombre?.takeIf { it.isNotBlank() } ?: j.titulo
        actualizar { it.copy(panel = Panel.IMAGENES, candidatos = emptyList(), candidatoPara = tipo, buscandoCandidatos = true, indice = 0, nombreImagen = titulo) }
        scope.launch {
            val lista = withContext(Dispatchers.IO) {
                try {
                    val wiki = if (nombre == null) e.ficha ?: Fichas.buscar(app, titulo, c.bonito) else Fichas.buscar(app, titulo, c.bonito)
                    Descargas.candidatos(app, crudo, c, tipo, titulo, e.claveSgdb, wiki)
                } catch (ex: Exception) {
                    emptyList()
                }
            }
            if (estado.value.panel == Panel.IMAGENES) actualizar { it.copy(candidatos = lista, buscandoCandidatos = false) }
        }
    }

    /** Usa la opción elegida de la tienda: la baja y la deja como imagen propia del juego (o hace el atajo). */
    fun usarCandidato(i: Int = estado.value.indice) {
        val e = estado.value
        val img = candidatosVisibles(e).getOrNull(i) ?: return
        val j = juegoFicha() ?: return
        val tipo = e.candidatoPara
        when (img.url) {
            "accion:archivos" -> { pedirImagen(tipo); return }
            "accion:sgdb" -> { abrirTexto("sgdb", e.claveSgdb); return }
            "accion:quitar" -> {
                cambiarDatos(j.clave) { when (tipo) { "arte" -> it.copy(arte = null); "logo" -> it.copy(logo = null); else -> it.copy(portada = null) } }
                avisar("Listo, volvió la ${nombreCandidato(tipo)} original")
                actualizar { it.copy(indice = 0) }
                return
            }
        }
        avisar("Bajando la ${nombreCandidato(tipo)}…")
        scope.launch {
            val f = withContext(Dispatchers.IO) {
                if (img.url.startsWith("http")) try { Descargas.bajarElegida(app, j, tipo, img.url) } catch (ex: Exception) { null } else null
            }
            if (f == null) { avisar("No pude bajar esa imagen. Prueba otra."); return@launch }
            val u = Uri.fromFile(f).toString()
            cambiarDatos(j.clave) {
                when (tipo) { "arte" -> it.copy(arte = u); "logo" -> it.copy(logo = u); else -> it.copy(portada = u) }
            }
            avisar("¡Listo! Nueva ${nombreCandidato(tipo)} para ${j.titulo}")
        }
    }

    private fun cambiarDatos(clave: String, f: (DatosJuego) -> DatosJuego) {
        val datos = estado.value.datos.toMutableMap()
        datos[clave] = f(datos[clave] ?: DatosJuego())
        almacen.guardarDatos(datos)
        actualizar { it.copy(datos = datos) }
        publicarJuegos()
    }

    /** Oculta un juego del estante (o lo vuelve a mostrar). Los ocultos se ven en Buscar › filtro Ocultos. */
    fun alternarOculto(clave: String) {
        val oculto = estado.value.datos[clave]?.oculto != true
        val e = estado.value
        // Al ocultarlo, la selección pasa al juego vecino.
        val v = e.visibles
        val i = v.indexOfFirst { it.clave == clave }
        val vecino = if (oculto && e.filtro != "ocultos") (v.getOrNull(i + 1) ?: v.getOrNull(i - 1))?.clave else e.juegoSel
        cambiarDatos(clave) { it.copy(oculto = oculto) }
        actualizar { it.copy(juegoSel = vecino, panel = Panel.NINGUNO) }
        avisar(if (oculto) "Juego oculto. Lo ves en Buscar › Ocultos" else "El juego volvió al estante")
    }

    fun alternarFavorito(clave: String) {
        cambiarDatos(clave) { it.copy(favorito = !it.favorito) }
        avisar(if (estado.value.datos[clave]?.favorito == true) "Agregado a favoritos" else "Quitado de favoritos")
    }

    /** La página donde se saca cada clave (para el botón "Sacar mi clave" y su código QR). */
    fun urlClave(para: String): String? = when (para) {
        "ia_clave_gemini", "gemini" -> "https://aistudio.google.com/apikey"
        "ia_clave_openai", "openai" -> "https://platform.openai.com/api-keys"
        "ia_clave", "claude" -> "https://console.anthropic.com/settings/keys"
        "sgdb" -> "https://www.steamgriddb.com/profile/preferences/api"
        "ra_clave", "ra_usuario" -> "https://retroachievements.org/settings"
        else -> null
    }

    /** Abre en el navegador la página para sacar la clave de [para]. */
    fun abrirPaginaClave(para: String) {
        val u = urlClave(para) ?: return
        avisar("Abriendo la página. Copia la clave y vuelve a Díptico para pegarla")
        abrirWeb(u)
    }

    fun abrirWeb(url: String) {
        try {
            app.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            avisar("No encontré un navegador para abrir el enlace")
        }
    }

    /** Guarda lo escrito en el cuadro de texto según para qué era. */
    private fun guardarTextoPanel() {
        val e = estado.value
        val t = e.borrador.trim()
        when (e.textoPara) {
            "titulo" -> e.fichaDe?.let { c -> cambiarDatos(c) { it.copy(titulo = t.ifEmpty { null }) } }
            "notas" -> e.fichaDe?.let { c -> cambiarDatos(c) { it.copy(notas = e.borrador) } }
            "buscar_ajuste" -> {
                actualizar { it.copy(buscarAjuste = t, panel = Panel.MENU, seccion = Seccion.ESENCIAL, enOpciones = true, indice = 0) }
                return
            }
            "buscar_imagen" -> {
                buscarCandidatos(e.candidatoPara, t)
                return
            }
            "asistente" -> {
                actualizar { it.copy(panel = Panel.ASISTENTE, indice = 0) }
                if (t.isNotEmpty()) preguntar(t)
                return
            }
            "consola_nombre" -> {
                cambiarConsolaPropia(e.consolaEditando) { it.copy(nombre = t.take(30).ifBlank { null }) }
                actualizar { it.copy(panel = Panel.EDITAR_CONSOLA) }
                return
            }
            "nombre" -> {
                almacen.guardarTexto("nombre", t.take(24))
                actualizar { it.copy(nombre = t.take(24)) }
                if (t.isNotBlank()) avisar("¡Hola, ${t.take(24)}!")
            }
            "ra_usuario" -> {
                almacen.guardarTexto("usuarioRA", t)
                actualizar { it.copy(usuarioRA = t, perfilRA = null) }
                if (t.isNotEmpty() && estado.value.claveRA.isBlank()) {
                    abrirTexto("ra_clave")
                    avisar("Ahora pega tu Web API Key")
                    return
                }
                cargarPerfilRA(true)
            }
            "ra_clave" -> {
                almacen.guardarTexto("claveRA", t)
                actualizar { it.copy(claveRA = t, perfilRA = null) }
                cargarPerfilRA(true)
            }
            "ia_clave", "ia_clave_gemini", "ia_clave_openai" -> {
                when (e.textoPara) {
                    "ia_clave_gemini" -> { almacen.guardarTexto("claveGemini", t); actualizar { it.copy(claveGemini = t) } }
                    "ia_clave_openai" -> { almacen.guardarTexto("claveOpenAI", t); actualizar { it.copy(claveOpenAI = t) } }
                    else -> { almacen.guardarTexto("claveIa", t); actualizar { it.copy(claveIa = t) } }
                }
                if (claveVieneDeGaleria) {
                    claveVieneDeGaleria = false
                    abrirGaleria("ia")
                    if (t.isNotEmpty()) avisar("Clave guardada. Bisa ya puede pensar con ${nombreMotor(estado.value.motorIA)}.")
                    return
                }
                if (t.isNotEmpty() && claveParaAsistente) {
                    claveParaAsistente = false
                    abrirTexto("asistente")
                    avisar("Clave guardada. Ahora escribe tu pregunta.")
                    return
                }
            }
            "ia_pedido" -> {
                cerrarPanel()
                crearTemaIA(t)
                return
            }
        }
        cerrarPanel()
    }

    // ---------- Elegir archivos (imágenes y música) ----------

    /** La pantalla activa registra aquí cómo pedir un archivo de un tipo (imagen o audio). */
    var pedirArchivo: ((String) -> Unit)? = null
    private var archivoPara = ""

    private fun pedirImagen(para: String) = pedirArchivoPara(para, "image/*")

    private fun pedirArchivoPara(para: String, tipo: String) {
        archivoPara = para
        pedirArchivo?.invoke(tipo) ?: avisar("No pude abrir el selector de archivos")
    }

    fun archivoElegido(uri: Uri) {
        if (archivoPara == "modeloIA") {
            archivoPara = ""
            trabajo("ialocal", "Copiando el modelo de IA")
            scope.launch {
                val error = withContext(Dispatchers.IO) { IALocal.usarPropio(app, uri) }
                terminarTrabajo("ialocal")
                avisar(error ?: "¡Listo! Bisa usará tu modelo")
                actualizar { it.copy(iaLocalLista = IALocal.listo(app)) }
            }
            return
        }
        try {
            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
        val u = uri.toString()
        when {
            archivoPara == "fondoInicio" -> {
                almacen.guardarTexto("fondoInicio", u)
                actualizar { it.copy(fondoInicio = u) }
                avisar("Fondo del inicio cambiado")
            }
            archivoPara == "widgetImagen" -> {
                val w = Widget("imagen-${System.currentTimeMillis()}", 0, 0, 2, 2, false, estado.value.paginaInicio, u)
                guardarWidgets(estado.value.widgets + w)
                alternarWidget(estado.value.widgets.size - 1)
                avisar("Imagen agregada a tu inicio")
            }
            archivoPara.startsWith("consola_") -> {
                val (que, id) = archivoPara.removePrefix("consola_").split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                cambiarConsolaPropia(id) {
                    when (que) { "hero" -> it.copy(hero = u); "logo" -> it.copy(logo = u); "musica" -> it.copy(musica = u); else -> it }
                }
                avisar("¡Listo! Cambio guardado")
            }
            archivoPara.startsWith("icono:") -> {
                val id = archivoPara.removePrefix("icono:")
                val m = estado.value.iconosPropios + (id to u)
                almacen.guardarTexto("iconosPropios", escribirMapa(m))
                actualizar { it.copy(iconosPropios = m) }
                avisar("Ícono cambiado")
            }
            else -> {
                val clave = estado.value.fichaDe ?: return
                cambiarDatos(clave) {
                    when (archivoPara) {
                        "portada" -> it.copy(portada = u)
                        "arte" -> it.copy(arte = u)
                        "logo" -> it.copy(logo = u)
                        "musica" -> it.copy(musica = u)
                        else -> it
                    }
                }
                avisar("¡Listo! Cambio guardado")
            }
        }
    }
    // ---------- Estante: vista, tamaño, filtros, búsqueda ----------

    private val VISTAS = listOf("estante", "carrusel", "cuadricula", "lista")
    fun nombreVista(v: String) = when (v) { "cuadricula" -> "Cuadrícula"; "lista" -> "Lista"; "carrusel" -> "Carrusel"; else -> "Estante" }

    val VISTAS_CONSOLAS = listOf("arriba", "lado", "rueda", "sistemas")
    fun nombreVistaConsolas(v: String) = when (v) { "lado" -> "Al costado"; "rueda" -> "Rueda"; "sistemas" -> "Sistemas"; else -> "Arriba" }

    /** Cambia cómo se ven las consolas (arriba, al costado, rueda o sistemas). */
    fun elegirVistaConsolas(v: String) {
        almacen.guardarTexto("vistaConsolas", v)
        actualizar { it.copy(vistaConsolas = v, enSistemas = v == "sistemas") }
        avisar("Consolas: ${nombreVistaConsolas(v)}")
    }

    fun cambiarVistaConsolas(d: Int = 1) = elegirVistaConsolas(VISTAS_CONSOLAS[(VISTAS_CONSOLAS.indexOf(estado.value.vistaConsolas) + d).mod(VISTAS_CONSOLAS.size)])

    /** Modo sistemas: entra a los juegos de la consola elegida. */
    fun entrarSistema() {
        cl.nikalbazu.diptico.audio.Sonidos.abrir()
        actualizar { it.copy(enSistemas = false) }
    }

    /** Modo sistemas: vuelve al carrusel de consolas. */
    fun volverASistemas() {
        if (estado.value.vistaConsolas == "sistemas") actualizar { it.copy(enSistemas = true) }
    }

    /** R3 o el botón Vista: estante, cuadrícula o lista. */
    fun cambiarVista() {
        val v = VISTAS[(VISTAS.indexOf(estado.value.vista) + 1).mod(VISTAS.size)]
        almacen.guardarTexto("vista", v)
        actualizar { it.copy(vista = v) }
        avisar("Vista: ${nombreVista(v)}")
    }

    private val TAMANOS_JUEGO = listOf(0.8f, 1f, 1.2f, 1.4f)
    fun nombreTamJuegos(t: Float) = when { t < 0.9f -> "Chico"; t < 1.1f -> "Mediano"; t < 1.3f -> "Grande"; else -> "Enorme" }

    fun cambiarTamJuegos(delta: Int = 1) {
        val i = TAMANOS_JUEGO.indexOfFirst { kotlin.math.abs(it - estado.value.tamJuegos) < 0.01f }
        val t = TAMANOS_JUEGO[(i + delta).coerceIn(0, TAMANOS_JUEGO.size - 1)]
        almacen.guardarTexto("tamJuegos", t.toString())
        actualizar { it.copy(tamJuegos = t) }
    }

    /** Escalera del pellizco: de lo más chico (lista) a lo más grande (estante enorme). */
    private val ESCALERA = listOf(
        "lista" to 1f, "cuadricula" to 0.8f, "cuadricula" to 1f, "cuadricula" to 1.2f, "cuadricula" to 1.4f,
        "estante" to 0.8f, "estante" to 1f, "estante" to 1.2f, "estante" to 1.4f,
    )

    /** Pellizco con dos dedos: abrir agranda los juegos, cerrar los achica (y cambia de vista en los extremos). */
    fun pellizcar(agrandar: Boolean) {
        val e = estado.value
        val actual = ESCALERA.indices.minByOrNull { i ->
            val (v, t) = ESCALERA[i]
            (if (v == e.vista) 0f else 10f) + kotlin.math.abs(t - e.tamJuegos)
        } ?: 0
        val n = (actual + if (agrandar) 1 else -1).coerceIn(0, ESCALERA.size - 1)
        if (n == actual) return
        val (v, t) = ESCALERA[n]
        almacen.guardarTexto("vista", v)
        almacen.guardarTexto("tamJuegos", t.toString())
        actualizar { it.copy(vista = v, tamJuegos = t) }
        avisar("${nombreVista(v)} · ${nombreTamJuegos(t)}")
    }

    val FILTROS = listOf("todos", "favoritos", "recientes", "mas", "sinjugar", "ocultos")
    fun nombreFiltro(f: String) = when (f) {
        "favoritos" -> "Favoritos"; "recientes" -> "Jugados hace poco"; "mas" -> "Más jugados"; "sinjugar" -> "Sin jugar"; "ocultos" -> "Ocultos"; else -> "Todos"
    }

    val ORDENES = listOf("az", "recientes", "tiempo")
    fun nombreOrden(o: String) = when (o) { "recientes" -> "Últimos jugados"; "tiempo" -> "Más tiempo"; else -> "A → Z" }

    fun cambiarFiltro(delta: Int = 1) {
        val f = FILTROS[(FILTROS.indexOf(estado.value.filtro) + delta).mod(FILTROS.size)]
        actualizar { it.copy(filtro = f, indice = 0) }
    }

    fun elegirFiltro(f: String) = actualizar { it.copy(filtro = f, indice = 0) }

    fun cambiarOrden(delta: Int = 1) {
        val o = ORDENES[(ORDENES.indexOf(estado.value.orden) + delta).mod(ORDENES.size)]
        almacen.guardarTexto("orden", o)
        actualizar { it.copy(orden = o, indice = 0) }
    }

    fun elegirOrden(o: String) {
        almacen.guardarTexto("orden", o)
        actualizar { it.copy(orden = o, indice = 0) }
    }

    /** L3 o el botón Buscar. */
    fun abrirBuscar() = actualizar { it.copy(panel = Panel.BUSCAR, indice = 0) }

    fun cambiarBusqueda(t: String) = actualizar { it.copy(busqueda = t.take(40), indice = 0) }

    fun limpiarBusqueda() = actualizar { it.copy(busqueda = "", filtro = "todos", indice = 0) }

    /** L2/R2: salta a la letra anterior o siguiente. */
    fun saltarLetra(delta: Int) {
        val e = estado.value
        val v = e.visibles
        if (v.isEmpty()) return
        fun letra(i: Int) = v[i].titulo.firstOrNull()?.uppercaseChar() ?: ' '
        val actual = letra(e.indiceSel)
        var i = e.indiceSel
        if (delta > 0) {
            while (i < v.size - 1 && letra(i) == actual) i++
        } else {
            while (i > 0 && letra(i) == actual) i--
            val l = letra(i)
            while (i > 0 && letra(i - 1) == l) i--
        }
        actualizar { it.copy(juegoSel = v[i].clave) }
        avisar("${letra(i)}")
    }

    // ---------- Forma de las cajas por consola ----------

    val FORMAS = listOf("original", "cuadrada", "poster", "horizontal", "disco", "cartucho", "tarjeta")
    fun nombreForma(f: String) = when (f) {
        "cuadrada" -> "Cuadrada"; "poster" -> "Póster"; "horizontal" -> "Horizontal"; "disco" -> "Disco"
        "cartucho" -> "Cartucho"; "tarjeta" -> "Tarjeta"; else -> "Caja original"
    }

    private val IMAGENES_CAJA = listOf("portada", "arte", "logo")
    fun nombreImagenCaja(i: String) = when (i) { "arte" -> "Captura del juego"; "logo" -> "Logo"; else -> "Portada" }

    private fun guardarFormas(m: Map<String, FormaCfg>) {
        val o = org.json.JSONObject()
        m.forEach { (k, f) -> o.put(k, org.json.JSONObject().put("forma", f.forma).put("tam", f.tam.toDouble()).put("imagen", f.imagen)) }
        almacen.guardarTexto("formas", o.toString())
        actualizar { it.copy(formas = m) }
    }

    private fun leerFormas(s: String): Map<String, FormaCfg> = try {
        val o = org.json.JSONObject(s)
        o.keys().asSequence().associateWith { k ->
            o.getJSONObject(k).let { f -> FormaCfg(f.optString("forma", "original"), f.optDouble("tam", 1.0).toFloat(), f.optString("imagen", "portada")) }
        }
    } catch (e: Exception) {
        emptyMap()
    }

    private fun cambiarForma(consola: String, f: (FormaCfg) -> FormaCfg) {
        val m = estado.value.formas
        guardarFormas(m + (consola to f(m[consola] ?: FormaCfg())))
    }

    fun ciclarForma(consola: String) = cambiarForma(consola) { it.copy(forma = FORMAS[(FORMAS.indexOf(it.forma) + 1).mod(FORMAS.size)]) }

    private fun ciclarTamForma(consola: String) = cambiarForma(consola) {
        val l = listOf(0.8f, 1f, 1.2f)
        it.copy(tam = l[(l.indexOfFirst { t -> kotlin.math.abs(t - it.tam) < 0.01f } + 1).mod(l.size)])
    }

    private fun ciclarImagenCaja(consola: String) = cambiarForma(consola) {
        it.copy(imagen = IMAGENES_CAJA[(IMAGENES_CAJA.indexOf(it.imagen) + 1).mod(IMAGENES_CAJA.size)])
    }

    private fun opcionesEstante(e: Estado): List<Opcion> {
        val c = Consolas.porId(e.consolaSel)
        val lista = mutableListOf(
            Opcion(
                "Consolas", nombreVistaConsolas(e.vistaConsolas), Icono.MANDO,
                "Arriba como pestañas, en una columna al costado, en una rueda que gira, o Sistemas: primero eliges la consola en un carrusel grande y después entras a sus juegos.",
                menos = { cambiarVistaConsolas(-1) },
            ) { cambiarVistaConsolas(1) },
            Opcion("Juegos", nombreVista(e.vista), Icono.VISTA, "Estante con cajas, carrusel en 3D, cuadrícula de carátulas o lista. Se cambia rápido con R3 (apretar el stick derecho).") { cambiarVista() },
            Opcion("Tamaño de los juegos", nombreTamJuegos(e.tamJuegos), Icono.FORMA, "Más chico para ver más juegos, o más grande para verlos mejor.") { cambiarTamJuegos(if (e.tamJuegos >= 1.39f) -3 else 1) },
            Opcion("Mostrar", nombreFiltro(e.filtro), Icono.CORAZON, "Todos, solo favoritos, jugados hace poco, los más jugados o los que no has jugado.") { cambiarFiltro() },
            Opcion("Ordenar", nombreOrden(e.orden), Icono.MENU, "A → Z, últimos jugados o más tiempo jugado.") { cambiarOrden() },
            Opcion("Buscar un juego", icono = Icono.BUSCAR, ayuda = "También con L3 (apretar el stick izquierdo).") { abrirBuscar() },
        )
        if (c != null) {
            val f = e.formas[c.id] ?: FormaCfg()
            lista += Opcion("Cajas de ${c.nombre}: forma", nombreForma(f.forma), Icono.FORMA, "Elige la forma de las cajas de esta consola. Para otra consola, elígela en el estante antes de abrir el menú.") { ciclarForma(c.id) }
            lista += Opcion("Cajas de ${c.nombre}: tamaño", nombreTamJuegos(f.tam), Icono.FORMA) { ciclarTamForma(c.id) }
            lista += Opcion("Cajas de ${c.nombre}: imagen", nombreImagenCaja(f.imagen), Icono.PORTADA, "Muestra la portada, una captura del juego o su logo.") { ciclarImagenCaja(c.id) }
        } else {
            lista += Opcion("Forma de las cajas", "Elige una consola", Icono.FORMA, "En el estante, elige una consola (L1 R1) y vuelve aquí para cambiar la forma de sus cajas.") {
                avisar("Primero elige una consola en el estante con L1 o R1")
            }
        }
        return lista
    }

    // ---------- Tamaño de cada pantalla ----------

    private val ZOOMS = listOf(1f, 0.85f, 0.92f, 1.08f, 1.16f)
    fun nombreZoom(z: Float) = when {
        kotlin.math.abs(z - 1f) < 0.01f -> "Automático"
        z < 0.9f -> "Muy compacto"; z < 1f -> "Compacto"; z < 1.12f -> "Grande"; else -> "Muy grande"
    }

    private fun ciclarZoom(arriba: Boolean) {
        val actual = if (arriba) estado.value.zoomArriba else estado.value.zoomAbajo
        val z = ZOOMS[(ZOOMS.indexOfFirst { kotlin.math.abs(it - actual) < 0.01f } + 1).mod(ZOOMS.size)]
        almacen.guardarTexto(if (arriba) "zoomArriba" else "zoomAbajo", z.toString())
        actualizar { if (arriba) it.copy(zoomArriba = z) else it.copy(zoomAbajo = z) }
    }

    // ---------- Centro de control y notificaciones ----------

    fun abrirCentro() {
        Medios.revisar(app)
        actualizar { it.copy(panel = if (it.panel == Panel.CENTRO) Panel.NINGUNO else Panel.CENTRO, indice = 0) }
    }

    private fun abrirIntent(i: Intent, error: String) {
        try {
            app.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            avisar(error)
        }
    }

    /** Abre el administrador de archivos de Android. */
    fun abrirArchivos() {
        val intentos = listOf(
            Intent(Intent.ACTION_VIEW).setDataAndType(
                android.provider.DocumentsContract.buildRootUri("com.android.externalstorage.documents", "primary"),
                "vnd.android.document/root",
            ),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_FILES),
            app.packageManager.getLaunchIntentForPackage("com.google.android.documentsui"),
            app.packageManager.getLaunchIntentForPackage("com.android.documentsui"),
        ).filterNotNull()
        for (i in intentos) {
            try {
                app.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: Exception) {
            }
        }
        avisar("No encontré un administrador de archivos")
    }

    /** Columnas de los accesos del centro de control. */
    const val COLUMNAS_CENTRO = 6

    /** Apps de la consola (AYN, Odin, Thor) y herramientas de la comunidad, encontradas una vez. */
    @Volatile private var appsConsola: List<App>? = null

    private val PISTAS_CONSOLA = listOf("ayn", "odin", "thor", "mjolnir", "wayfinder", "handheld", "gamecontroller", "padsettings")
    private val MUSICA_APPS = listOf(
        "com.spotify.music" to "Spotify",
        "com.google.android.apps.youtube.music" to "YT Music",
    )

    fun appsDeConsola(): List<App> = appsConsola ?: run {
        val pm = app.packageManager
        val lista = try {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .mapNotNull { r ->
                    val paquete = r.activityInfo?.packageName ?: return@mapNotNull null
                    if (paquete == app.packageName) return@mapNotNull null
                    val nombre = r.loadLabel(pm)?.toString() ?: paquete
                    val texto = (paquete + " " + nombre).lowercase()
                    if (PISTAS_CONSOLA.any { it in texto }) App(paquete, nombre) else null
                }.distinctBy { it.paquete }.sortedBy { it.nombre.lowercase() }.take(6)
        } catch (_: Exception) {
            emptyList()
        }
        appsConsola = lista
        lista
    }

    /** Spotify y YouTube Music, si están instalados. */
    fun appsMusica(): List<App> = MUSICA_APPS.filter { (p, _) -> Lanzador.instalado(app, p) }.map { (p, n) -> App(p, n) }

    fun abrirPaquete(paquete: String) {
        val i = app.packageManager.getLaunchIntentForPackage(paquete)
        if (i == null) avisar("Esa app no está instalada") else {
            cerrarPanel()
            abrirIntent(i, "No pude abrir la app")
        }
    }

    /** Mueve la selección del centro: grilla de accesos arriba, lista de notificaciones abajo. */
    fun moverCentro(dx: Int, dy: Int) {
        val e = estado.value
        val n = opcionesCentro().size
        val total = n + Avisos.lista.value.size
        if (total == 0) return
        val i = e.indice
        val nuevo = if (i < n) {
            when {
                dx != 0 -> (i + dx).coerceIn(0, n - 1)
                dy < 0 -> if (i - COLUMNAS_CENTRO >= 0) i - COLUMNAS_CENTRO else i
                else -> if (i + COLUMNAS_CENTRO < n) i + COLUMNAS_CENTRO else if (total > n) n else i
            }
        } else {
            when {
                dy < 0 -> if (i == n) ((n - 1) / COLUMNAS_CENTRO) * COLUMNAS_CENTRO else i - 1
                dy > 0 -> (i + 1).coerceAtMost(total - 1)
                else -> i
            }
        }
        if (nuevo != i) actualizar { it.copy(indice = nuevo) }
    }

    fun opcionesCentro(): List<Opcion> = opcionesSistema() +
        appsDeConsola().map { a -> Opcion(a.nombre, "Consola", Icono.MANDO, paquete = a.paquete) { abrirPaquete(a.paquete) } } +
        appsMusica().map { a -> Opcion(a.nombre, "Música", Icono.MUSICA, paquete = a.paquete) { abrirPaquete(a.paquete) } }

    private fun opcionesSistema(): List<Opcion> = listOf(
        Opcion("Wi-Fi", icono = Icono.WIFI) {
            abrirIntent(Intent(android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY), "No pude abrir el Wi-Fi")
        },
        Opcion("Bluetooth", icono = Icono.BLUETOOTH) { abrirIntent(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS), "No pude abrir el Bluetooth") },
        Opcion("Volumen", icono = Icono.VOLUMEN) { abrirIntent(Intent(android.provider.Settings.Panel.ACTION_VOLUME), "No pude abrir el volumen") },
        Opcion("Pantalla", icono = Icono.BRILLO) { abrirIntent(Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS), "No pude abrir la pantalla") },
        Opcion("Todas las apps", icono = Icono.APPS) { abrirApps() },
        Opcion("Archivos", icono = Icono.CARPETA) { abrirArchivos() },
        Opcion("Batería", icono = Icono.BATERIA) { abrirIntent(Intent(Intent.ACTION_POWER_USAGE_SUMMARY), "No pude abrir la batería") },
        Opcion("Bisa", icono = Icono.BISA) { abrirAsistente() },
        Opcion("Estadísticas", icono = Icono.RELOJ) { abrirEstadisticas() },
        Opcion("Protector", icono = Icono.ESTRELLA) { cerrarPanel(); actualizar { it.copy(protector = true) } },
        Opcion("Ajustes", icono = Icono.AJUSTES) { abrirIntent(Intent(android.provider.Settings.ACTION_SETTINGS), "No pude abrir los ajustes") },
    ) + (if (!estado.value.unaPantalla) listOf(Opcion("Intercambiar", icono = Icono.INTERCAMBIAR) { cerrarPanel(); intercambiar() }) else emptyList())

    fun abrirAviso(a: Aviso) {
        cerrarPanel()
        Avisos.abrir(a)
    }

    fun descartarAvisoSel() {
        val e = estado.value
        val n = opcionesCentro().size
        Avisos.lista.value.getOrNull(e.indice - n)?.let { Avisos.descartar(it) }
    }

    private fun opcionesConsola(e: Estado): List<Opcion> = listOf(
        Opcion("Centro de control", "Notificaciones y accesos", Icono.CAMPANA, "Wi-Fi, Bluetooth, volumen, archivos y tus notificaciones. También manteniendo Start o tocando la campana.") { abrirCentro() },
        Opcion(
            "Notificaciones en Díptico", if (Medios.tienePermiso(app)) "Activadas ✓" else "Dar permiso", Icono.CAMPANA,
            "Para ver las notificaciones de tus apps dentro de Díptico, Android pide activar el \"acceso a notificaciones\".",
        ) { if (!Medios.tienePermiso(app)) Medios.pedirPermiso(app) else avisar("Las notificaciones ya están activadas") },
        Opcion(
            "Avisos emergentes", if (e.avisosEmergentes) "Sí" else "No", Icono.CAMPANA,
            "Muestra un aviso chico cuando llega una notificación (nunca mientras juegas).",
        ) {
            val v = !estado.value.avisosEmergentes
            almacen.guardarBool("avisosEmergentes", v)
            actualizar { it.copy(avisosEmergentes = v) }
        },
        Opcion(
            "Díptico como inicio", if (e.esInicio) "Listo ✓" else "Hacerlo ahora", Icono.ESTRELLA,
            "Así el botón Inicio, el encendido y la salida de cada juego te traen siempre a Díptico, en las dos pantallas.",
        ) { hacerInicio() },
        Opcion("Ajustes de inicio de Android", icono = Icono.AJUSTES, ayuda = "Para cambiar la app de inicio a mano.") {
            abrirIntent(Intent(android.provider.Settings.ACTION_HOME_SETTINGS), "No pude abrir los ajustes de inicio")
        },
        Opcion("Archivos", icono = Icono.CARPETA, ayuda = "Abre el administrador de archivos de Android.") { abrirArchivos() },
        Opcion("Ajustes de Android", icono = Icono.AJUSTES) { abrirIntent(Intent(android.provider.Settings.ACTION_SETTINGS), "No pude abrir los ajustes") },
    )

    // ---------- Reconocer los nombres de los juegos ----------

    /** Busca el nombre oficial de los juegos en la lista de libretro (sin bajar imágenes). */
    fun identificarNombres(avisarAlFinal: Boolean = false) {
        if (!estado.value.titulosBonitos) return
        val pendientes = crudos.filter { medios[it.clave]?.titulo == null }
        if (pendientes.isEmpty()) {
            if (avisarAlFinal) avisar("Todos tus juegos ya tienen nombre")
            return
        }
        trabajo("nombres", "Reconociendo nombres")
        scope.launch {
            val hay = withContext(Dispatchers.IO) { Descargas.hayInternet() }
            if (!hay) {
                terminarTrabajo("nombres")
                if (avisarAlFinal) avisar("Necesito internet para reconocer los nombres")
                return@launch
            }
            var n = 0
            pendientes.forEachIndexed { i, j ->
                val c = Consolas.porId(j.consolaId) ?: return@forEachIndexed
                if (i % 10 == 0) trabajo("nombres", "Reconociendo nombres", i / pendientes.size.toFloat())
                val t = withContext(Dispatchers.IO) { try { Descargas.identificar(app, j, c) } catch (ex: Exception) { null } }
                if (t != null) {
                    medios = medios + (j.clave to (medios[j.clave] ?: MediosJuego()).copy(titulo = t))
                    n++
                }
            }
            withContext(Dispatchers.IO) { almacen.guardarMedios(medios) }
            publicarJuegos()
            terminarTrabajo("nombres")
            if (avisarAlFinal || n > 0) avisar("Reconocí el nombre de $n juegos")
        }
    }

    /** Pide a la IA el nombre de los juegos que no se reconocieron (Switch, PS3, archivos con nombres raros…). */
    fun identificarConIA() {
        val clave = llaveNube() ?: return pedirIANube("Reconocer nombres")
        val pendientes = crudos.filter { medios[it.clave]?.titulo == null }
        if (pendientes.isEmpty()) {
            avisar("Todos tus juegos ya tienen nombre")
            return
        }
        trabajo("ia_nombres", "La IA está reconociendo tus juegos")
        scope.launch {
            var n = 0
            var error: String? = null
            pendientes.chunked(40).forEachIndexed { i, grupo ->
                trabajo("ia_nombres", "La IA está reconociendo tus juegos", i * 40f / pendientes.size)
                val r = withContext(Dispatchers.IO) {
                    try {
                        IA.nombres(clave, grupo.map { (Consolas.porId(it.consolaId)?.bonito ?: it.consolaId) to it.archivo })
                    } catch (ex: Exception) {
                        error = ex.message
                        emptyMap()
                    }
                }
                for (j in grupo) {
                    val t = r[j.archivo] ?: continue
                    medios = medios + (j.clave to (medios[j.clave] ?: MediosJuego()).copy(titulo = t))
                    n++
                }
            }
            withContext(Dispatchers.IO) { almacen.guardarMedios(medios) }
            publicarJuegos()
            terminarTrabajo("ia_nombres")
            avisar(error?.let { "IA: $it" } ?: "La IA reconoció $n juegos")
        }
    }

    // ---------- Música según el tema ----------

    /** Qué pista va con cada tema y cada fiesta. */
    fun musicaDeTema(t: Tema): String = when (t.id.removePrefix("fiesta_")) {
        "fiestaspatrias", "chile" -> "cueca"
        "glorias", "sanpedro", "patagonia" -> "oceano"
        "patrimonio", "dosmundos", "menta" -> "bosque"
        "tirana", "nino", "trabajador", "musica" -> "fiesta"
        "cyber", "retro80", "fuego" -> "neon"
        "lavanda", "cereza" -> "sakura"
        "dorado", "desierto", "monocromo" -> "medianoche"
        "hielo" -> "nieve"
        "medianoche", "oled", "papel" -> "medianoche"
        "sakura", "hanami", "pascua", "lunar", "eid", "mujer" -> "sakura"
        "arcade", "lava", "gamer" -> "neon"
        "oceano", "songkran" -> "oceano"
        "bosque", "tierra", "sanpatricio", "gracias", "andino" -> "bosque"
        "portatil" -> "portatil"
        "bloques" -> "bloques"
        "halloween", "muertos" -> "fantasma"
        "navidad", "anionuevo", "reyes", "janucá", "diwali" -> "nieve"
        "carnaval", "holi", "orgullo", "independencia", "oktoberfest", "sanvalentin" -> "fiesta"
        "atardecer" -> "cartucho"
        else -> if (t.propio) when (t.decor) {
            "nieve" -> "nieve"; "murcielagos" -> "fantasma"; "petalos", "faroles" -> "sakura"; "confeti", "fuegos" -> "fiesta"
            "hojas" -> "bosque"; "burbujas" -> "oceano"; "estrellas" -> "medianoche"; else -> "bisagra"
        } else "bisagra"
    }

    // ---------- Varias carpetas de juegos ----------

    /** Carpeta elegida (árbol) que contiene a un juego, para rutas y archivos vecinos. */
    private fun raizDe(j: Juego): Uri? = try {
        android.provider.DocumentsContract.buildTreeDocumentUri(j.uri.authority, android.provider.DocumentsContract.getTreeDocumentId(j.uri))
    } catch (e: Exception) {
        almacen.carpeta
    }

    /** Lista de consolas para elegir a mano: las más usadas primero. */
    fun consolasElegibles(): List<Consola> = Consolas.todas

    fun abrirCarpetas() = actualizar { it.copy(panel = Panel.CARPETAS, indice = 0, consolaPara = -1, carpetas = almacen.carpetas()) }

    /** Si una carpeta nueva no tiene juegos reconocibles, pregunta de qué consola es. */
    private fun revisarCarpetaNueva(uri: Uri) {
        scope.launch {
            val juegos = withContext(Dispatchers.IO) { try { Biblioteca.escanear(app, uri, null) } catch (e: Exception) { emptyList() } }
            if (juegos.isEmpty()) {
                val l = almacen.carpetas()
                val i = l.indexOfFirst { it.uri == uri }
                if (i >= 0) {
                    actualizar { it.copy(panel = Panel.CARPETAS, carpetas = l, consolaPara = i, indice = 0) }
                    avisar("No reconocí la consola de esa carpeta. Elígela tú.")
                }
            }
        }
    }

    /** Filas del panel de carpetas: cada carpeta y "Agregar carpeta". */
    fun filasCarpetas(): Int = estado.value.carpetas.size + 1

    private fun aceptarCarpetas() {
        val e = estado.value
        if (e.consolaPara >= 0) {
            // Elegir consola: fila 0 = automático, luego todas las consolas.
            val consola = if (e.indice == 0) null else consolasElegibles().getOrNull(e.indice - 1)?.id
            val l = e.carpetas.toMutableList()
            l[e.consolaPara] = l[e.consolaPara].copy(consola = consola)
            almacen.guardarCarpetas(l)
            actualizar { it.copy(carpetas = l, consolaPara = -1, indice = e.consolaPara) }
            escanear()
            return
        }
        if (e.indice >= e.carpetas.size) pedirCarpeta?.invoke(Carpeta.JUEGOS)
        else actualizar { it.copy(consolaPara = e.indice, indice = 0) }
    }

    /** Y en el panel de carpetas: quita la carpeta elegida. */
    fun quitarCarpeta() {
        val e = estado.value
        if (e.consolaPara >= 0 || e.indice >= e.carpetas.size) return
        val l = e.carpetas.toMutableList()
        val quitada = l.removeAt(e.indice)
        try {
            app.contentResolver.releasePersistableUriPermission(quitada.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
        almacen.guardarCarpetas(l)
        actualizar { it.copy(carpetas = l, indice = it.indice.coerceAtMost(l.size), tieneCarpeta = l.isNotEmpty()) }
        avisar("Carpeta quitada")
        escanear()
        if (l.isEmpty()) {
            crudos = emptyList()
            publicarJuegos()
        }
    }

    fun nombreCarpeta(c: CarpetaJuegos): String = Biblioteca.nombreCarpeta(c.uri)

    // ---------- Efecto, fluidez y protector de pantalla ----------

    fun nombreEfecto(v: String) = when (v) { "liso" -> "Liso"; "brillo" -> "Brillo"; "vidrio" -> "Vidrio"; else -> "Degradado" }
    fun nombreFluidez(v: String) = when (v) { "alta" -> "Alta"; "equilibrada" -> "Equilibrada"; "ahorro" -> "Ahorro"; else -> "Automática" }

    /** Mira la memoria y los núcleos de la consola para elegir cuántos efectos usar. */
    private fun detectarFluidez(): String {
        val am = app.getSystemService(android.app.ActivityManager::class.java) ?: return "equilibrada"
        val mi = android.app.ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val gb = mi.totalMem / (1024.0 * 1024 * 1024)
        val nucleos = Runtime.getRuntime().availableProcessors()
        return when {
            am.isLowRamDevice || gb < 3.5 -> "ahorro"
            gb >= 7 && nucleos >= 8 -> "alta"
            else -> "equilibrada"
        }
    }

    /** Fluidez en uso (resuelve "auto"). */
    fun fluidezEfectiva(e: Estado = estado.value) = if (e.fluidez == "auto") e.fluidezDetectada else e.fluidez

    private fun aplicarFluidez() {
        val f = fluidezEfectiva()
        cl.nikalbazu.diptico.ui.Estilo.efectosCompletos = f != "ahorro"
        cl.nikalbazu.diptico.ui.Estilo.efecto = estado.value.efecto
        cl.nikalbazu.diptico.ui.Estilo.decoraciones = estado.value.decoraciones && f != "ahorro"
    }

    private var ultimaActividad = SystemClock.uptimeMillis()

    /** Se llama con cada botón o toque. Devuelve true si cerró el protector (y el toque no debe hacer nada más). */
    fun actividad(): Boolean {
        ultimaActividad = SystemClock.uptimeMillis()
        if (estado.value.protector) {
            actualizar { it.copy(protector = false) }
            return true
        }
        return false
    }

    private fun vigilarProtector() {
        scope.launch {
            while (true) {
                delay(10_000)
                val e = estado.value
                if (e.protector || e.protectorMin <= 0 || !e.configurado || e.sesion != null || e.aLaVista == 0 || e.juegos.isEmpty()) continue
                if (SystemClock.uptimeMillis() - ultimaActividad > e.protectorMin * 60_000L) {
                    actualizar { it.copy(protector = true, panel = Panel.NINGUNO, relojGrande = false) }
                }
            }
        }
    }

    // ---------- Estadísticas ----------

    fun abrirEstadisticas() {
        actualizar { it.copy(panel = Panel.ESTADISTICAS, indice = 0) }
        if (estado.value.perfilRA == null) cargarPerfilRA()
    }

    // ---------- Grilla y páginas del inicio ----------

    private fun guardarGrilla(cols: Int, filas: Int, paginas: Int) {
        almacen.guardarTexto("gridCols", cols.toString())
        almacen.guardarTexto("gridFilas", filas.toString())
        almacen.guardarTexto("paginas", paginas.toString())
        Widgets.COLUMNAS = cols
        Widgets.FILAS = filas
        // Los widgets que quedaron fuera se acomodan dentro, sin montarse unos sobre otros.
        val l = reacomodar(estado.value.widgets.map { encajar(it.copy(pagina = it.pagina.coerceIn(0, paginas - 1))) }, paginas)
        almacen.guardarTexto("widgets", escribirWidgets(l))
        actualizar { it.copy(gridCols = cols, gridFilas = filas, paginas = paginas, widgets = l, paginaInicio = it.paginaInicio.coerceIn(0, paginas - 1)) }
    }

    /** Si dos widgets quedaron encima, el segundo se mueve al primer hueco libre (o se oculta si no hay). */
    private fun reacomodar(lista: List<Widget>, paginas: Int): List<Widget> {
        val l = lista.toMutableList()
        for (i in l.indices) {
            val w = l[i]
            if (!w.visible || !choca(w, i, l)) continue
            var puesto: Widget? = null
            busqueda@ for (pag in (0 until paginas)) for ((an, al) in listOf(w.an to w.al, 2 to 2, 2 to 1, 1 to 1)) {
                for (y in 0..Widgets.FILAS - al) for (x in 0..Widgets.COLUMNAS - an) {
                    val c = w.copy(x = x, y = y, an = minOf(an, Widgets.COLUMNAS), al = minOf(al, Widgets.FILAS), pagina = pag)
                    if (!choca(c, i, l)) { puesto = c; break@busqueda }
                }
            }
            l[i] = puesto ?: w.copy(visible = false)
        }
        return l
    }

    // ---------- Estudio de widgets ----------

    /**
     * Un control del estudio de widgets. [tipo]: "pasos" (‹ valor ›), "chips" (elegir entre varios), "colores",
     * "accion", "interruptor", "catalogo" (un widget para agregar) o "formas" (tamaños listos).
     */
    data class Control(
        val titulo: String,
        val tipo: String,
        val valor: String = "",
        val grupo: String = "",
        val icono: Icono? = null,
        val ayuda: String = "",
        val elecciones: List<String> = emptyList(),
        val elegida: Int = -1,
        val colores: List<Long> = emptyList(),
        /** Para el catálogo: el tipo de widget. Para formas: (ancho, alto) de cada elección. */
        val extra: String = "",
        val formas: List<Pair<Int, Int>> = emptyList(),
        val menos: (() -> Unit)? = null,
        val elegir: ((Int) -> Unit)? = null,
        val mas: () -> Unit = {},
    )

    val PESTANAS_ESTUDIO = listOf("Widget", "Agregar", "Página")

    fun abrirAjustesWidget(i: Int = estado.value.widgetSel) {
        if (!estado.value.editandoWidgets) editarWidgets()
        actualizar { it.copy(widgetSel = i, panel = Panel.ESTUDIO, estudioPestana = 0, indice = 0) }
    }

    fun abrirCatalogoWidgets() {
        if (!estado.value.editandoWidgets) editarWidgets()
        actualizar { it.copy(panel = Panel.ESTUDIO, estudioPestana = 1, indice = 0) }
    }

    fun cambiarPestanaEstudio(d: Int) = actualizar {
        it.copy(estudioPestana = (it.estudioPestana + d).mod(PESTANAS_ESTUDIO.size), indice = 0, estudioMover = false)
    }

    fun elegirPestanaEstudio(i: Int) = actualizar { it.copy(estudioPestana = i, indice = 0, estudioMover = false) }

    fun alternarMoverWidget() {
        val v = !estado.value.estudioMover
        actualizar { it.copy(estudioMover = v) }
        if (v) avisar("Cruceta mueve el widget · L1 R1 ancho · L2 R2 alto · A o B para terminar")
    }

    private val OPACIDADES = listOf(1f, 0.85f, 0.7f, 0.5f, 0.3f, 0f)
    private val RADIOS = listOf(6, 14, 22, 34)
    private val ESCALAS = listOf(0.85f, 1f, 1.15f, 1.3f)
    private fun <T> ciclar(l: List<T>, v: T, d: Int): T = l[(l.indexOf(v).coerceAtLeast(0) + d).mod(l.size)]

    /** Pone el widget elegido en una forma lista (si choca, busca dónde cabe en su página). */
    private fun ponerForma(i: Int, an: Int, al: Int) {
        val e = estado.value
        val w = e.widgets.getOrNull(i) ?: return
        if (an > Widgets.COLUMNAS || al > Widgets.FILAS) { avisar("No cabe en la grilla: agrega columnas o filas en la pestaña Página"); return }
        val c = encajar(w.copy(an = an, al = al))
        if (!choca(c, i)) { cambiarWidget(i) { c }; return }
        for (y in 0..Widgets.FILAS - al) for (x in 0..Widgets.COLUMNAS - an) {
            val d = c.copy(x = x, y = y)
            if (!choca(d, i)) { cambiarWidget(i) { d }; return }
        }
        avisar("No cabe en esta página: achica otro widget o cámbialo de página")
    }

    /** Los controles de la pestaña abierta del estudio. */
    fun controlesEstudio(e: Estado = estado.value): List<Control> = when (e.estudioPestana) {
        1 -> controlesAgregar(e)
        2 -> controlesPagina(e)
        else -> controlesWidget(e)
    }

    private fun controlesWidget(e: Estado): List<Control> {
        val i = e.widgetSel
        val w = e.widgets.getOrNull(i)?.takeIf { it.visible } ?: return listOf(
            Control("No hay widgets a la vista", "accion", "Agregar", icono = Icono.APPS) { elegirPestanaEstudio(1) },
        )
        val visibles = e.widgets.indices.filter { e.widgets[it].visible }
        val formas = Widgets.FORMAS.filter { (_, f) -> f.first <= Widgets.COLUMNAS && f.second <= Widgets.FILAS }
        val colores = listOf(0L) + Temas.MUESTRAS
        val l = mutableListOf<Control>()
        l += Control(
            "Widget elegido", "widgets", Widgets.nombre(w.tipo), "Elegido",
            elecciones = visibles.map { Widgets.nombre(e.widgets[it].tipo) }, elegida = visibles.indexOf(i), extra = w.tipo,
            menos = { siguienteWidget(-1) }, elegir = { k -> visibles.getOrNull(k)?.let { elegirWidget(it) } },
        ) { siguienteWidget(1) }
        l += Control(
            if (e.estudioMover) "Moviendo: usa la cruceta" else "Mover con la cruceta", "accion",
            if (e.estudioMover) "Terminar" else "Y", "Elegido", Icono.MANDO,
            ayuda = "También puedes arrastrarlo con el dedo en tu inicio.",
        ) { alternarMoverWidget() }
        l += Control(
            "Forma", "formas", Widgets.nombreTamano(w.an, w.al), "Tamaño",
            elecciones = formas.map { it.first }, formas = formas.map { it.second },
            elegida = formas.indexOfFirst { it.second == (w.an to w.al) },
            menos = { tamanoWidgetSel(-1) }, elegir = { k -> formas.getOrNull(k)?.second?.let { (an, al) -> ponerForma(i, an, al) } },
        ) { tamanoWidgetSel(1) }
        l += Control("Ancho", "pasos", "${w.an} de ${Widgets.COLUMNAS}", "Tamaño", Icono.FORMA, menos = { cambiarTamWidget(-1, 0) }) { cambiarTamWidget(1, 0) }
        l += Control("Alto", "pasos", "${w.al} de ${Widgets.FILAS}", "Tamaño", Icono.FORMA, menos = { cambiarTamWidget(0, -1) }) { cambiarTamWidget(0, 1) }
        val variantes = Widgets.variantes(w.tipo)
        if (variantes.size > 1) l += Control(
            "Diseño", "chips", variantes.getOrElse(w.variante) { variantes[0] }, "Aspecto", elecciones = variantes, elegida = w.variante.coerceIn(0, variantes.size - 1),
            elegir = { k -> cambiarWidget(i) { it.copy(variante = k.coerceIn(0, variantes.size - 1)) } },
        ) { cambiarWidget(i) { it.copy(variante = (it.variante + 1).mod(variantes.size)) } }
        l += Control(
            "Estilo", "estilos", Widgets.nombreEstilo(w.estilo), "Aspecto",
            elecciones = Widgets.ESTILOS.map { Widgets.nombreEstilo(it) }, elegida = Widgets.ESTILOS.indexOf(w.estilo),
            elegir = { k -> cambiarWidget(i) { it.copy(estilo = Widgets.ESTILOS[k.coerceIn(0, Widgets.ESTILOS.size - 1)]) } },
        ) { cambiarWidget(i) { it.copy(estilo = ciclar(Widgets.ESTILOS, it.estilo, 1)) } }
        l += Control(
            "Color", "colores", if (w.color == 0L) "Del tema" else "Propio", "Aspecto", colores = colores, elegida = colores.indexOf(w.color).coerceAtLeast(0),
            elegir = { k -> cambiarWidget(i) { it.copy(color = colores[k.coerceIn(0, colores.size - 1)]) } },
        ) { cambiarWidget(i) { it.copy(color = ciclar(colores, it.color, 1)) } }
        l += Control(
            "Fondo", "chips", "${(w.opacidad * 100).toInt()}%", "Aspecto",
            elecciones = OPACIDADES.map { if (it == 0f) "Sin fondo" else "${(it * 100).toInt()}%" }, elegida = OPACIDADES.indexOf(w.opacidad),
            elegir = { k -> cambiarWidget(i) { it.copy(opacidad = OPACIDADES[k.coerceIn(0, OPACIDADES.size - 1)]) } },
        ) { cambiarWidget(i) { it.copy(opacidad = ciclar(OPACIDADES, it.opacidad, 1)) } }
        l += Control(
            "Esquinas", "chips", "", "Aspecto", elecciones = listOf("Rectas", "Suaves", "Redondas", "Muy redondas"), elegida = RADIOS.indexOf(w.radio),
            elegir = { k -> cambiarWidget(i) { it.copy(radio = RADIOS[k.coerceIn(0, RADIOS.size - 1)]) } },
        ) { cambiarWidget(i) { it.copy(radio = ciclar(RADIOS, it.radio, 1)) } }
        l += Control(
            "Tamaño del contenido", "chips", "", "Aspecto", elecciones = listOf("Chico", "Normal", "Grande", "Enorme"), elegida = ESCALAS.indexOf(w.escala),
            elegir = { k -> cambiarWidget(i) { it.copy(escala = ESCALAS[k.coerceIn(0, ESCALAS.size - 1)]) } },
        ) { cambiarWidget(i) { it.copy(escala = ciclar(ESCALAS, it.escala, 1)) } }
        l += Control("Página", "pasos", "${w.pagina + 1} de ${e.paginas}", "Lugar", Icono.VISTA, menos = { moverWidgetPagina(i, -1) }) { moverWidgetPagina(i, 1) }
        l += Control("Copiar el estilo a todos", "accion", "Aplicar", "Lugar", Icono.PALETA, "Todos los widgets quedan con este estilo, color, fondo y esquinas.") {
            val base = estado.value.widgets[i]
            guardarWidgets(estado.value.widgets.map { it.copy(estilo = base.estilo, color = base.color, opacidad = base.opacidad, radio = base.radio) })
            avisar("Estilo copiado a todos los widgets")
        }
        l += Control("Quitar este widget", "accion", "Quitar", "Lugar", Icono.BORRAR) { quitarWidget(i) }
        return l
    }

    private fun controlesAgregar(e: Estado): List<Control> {
        val tipos = e.widgets.withIndex().filter { !it.value.tipo.startsWith("imagen") || it.value.visible }
        return tipos.map { (k, w) ->
            Control(
                Widgets.nombre(w.tipo), "catalogo", if (w.visible) "En tu inicio ✓" else "Agregar", if (w.visible) "En tu inicio" else "Para agregar",
                Widgets.icono(w.tipo), ayuda = Widgets.descripcion(w.tipo), extra = w.tipo,
            ) {
                if (w.visible) {
                    actualizar { it.copy(widgetSel = k, estudioPestana = 0, indice = 0, paginaInicio = w.pagina) }
                } else {
                    alternarWidget(k)
                    if (estado.value.widgets[k].visible) {
                        actualizar { it.copy(widgetSel = k) }
                        avisar("${Widgets.nombre(w.tipo)} agregado")
                    }
                }
            }
        }.sortedBy { if (it.valor == "Agregar") 0 else 1 } + Control(
            "Imagen o GIF propio", "catalogo", "Elegir", "Para agregar", Icono.PORTADA,
            ayuda = "Una foto o un GIF animado de tu consola como widget.", extra = "imagen",
        ) { pedirArchivoPara("widgetImagen", "image/*") }
    }

    private fun controlesPagina(e: Estado): List<Control> = listOf(
        Control("Columnas", "pasos", "${e.gridCols}", "Grilla", Icono.APPS, "De 3 a 10.", menos = { cambiarGrilla(-1, 0, 0) }) { cambiarGrilla(1, 0, 0) },
        Control("Filas", "pasos", "${e.gridFilas}", "Grilla", Icono.APPS, "De 2 a 8. Con más de 4, el inicio se desplaza hacia abajo.", menos = { cambiarGrilla(0, -1, 0) }) { cambiarGrilla(0, 1, 0) },
        Control("Páginas", "pasos", "${e.paginas}", "Grilla", Icono.VISTA, "Cambia de página deslizando o con L2 y R2.", menos = { cambiarGrilla(0, 0, -1) }) { cambiarGrilla(0, 0, 1) },
        Control(
            "Separación", "chips", "", "Grilla", elecciones = listOf("Juntos", "Normal", "Amplia"), elegida = listOf(4, 10, 16).indexOf(e.huecoInicio),
            elegir = { k -> listOf(4, 10, 16).getOrNull(k)?.let { v -> almacen.guardarTexto("huecoInicio", v.toString()); actualizar { it.copy(huecoInicio = v) } } },
        ) { cambiarHueco(1) },
        Control("Saludo y fecha arriba", "interruptor", if (e.saludoInicio) "Sí" else "No", "Encabezado", Icono.TEXTO) {
            val v = !estado.value.saludoInicio
            almacen.guardarBool("saludoInicio", v)
            actualizar { it.copy(saludoInicio = v) }
        },
        Control("Fondo del inicio", "accion", if (e.fondoInicio != null) "Propio ✓" else "Arte del juego", "Fondo", Icono.PORTADA, "Una imagen o un GIF animado detrás de tus widgets.") {
            pedirArchivoPara("fondoInicio", "image/*")
        },
        Control("Quitar fondo propio", "accion", if (e.fondoInicio != null) "Quitar" else "—", "Fondo", Icono.BORRAR) {
            almacen.guardarTexto("fondoInicio", "")
            actualizar { it.copy(fondoInicio = null) }
        },
        Control("Volver a como venía", "accion", "Restablecer", "Fondo", Icono.BORRAR, "Deja los widgets y la grilla como vienen de fábrica.") {
            guardarGrilla(6, 4, 1)
            guardarWidgets(Widgets.porDefecto)
            actualizar { it.copy(widgetSel = 0, huecoInicio = 10) }
            almacen.guardarTexto("huecoInicio", "10")
            avisar("Inicio como al principio")
        },
    )

    private fun cambiarGrilla(dc: Int, df: Int, dp: Int) {
        val e = estado.value
        val c = (e.gridCols + dc).coerceIn(3, 10)
        val f = (e.gridFilas + df).coerceIn(2, 8)
        val pg = (e.paginas + dp).coerceIn(1, 6)
        if (c == e.gridCols && f == e.gridFilas && pg == e.paginas) return
        guardarGrilla(c, f, pg)
    }

    /** ← y → en el estudio. */
    fun cambiarControl(d: Int) {
        val c = controlesEstudio().getOrNull(estado.value.indice) ?: return
        when {
            d < 0 && c.menos != null -> c.menos.invoke()
            c.elegir != null && c.tipo != "widgets" && c.tipo != "formas" -> {
                val n = if (c.tipo == "colores") c.colores.size else c.elecciones.size
                if (n > 0) c.elegir.invoke((c.elegida + d).coerceIn(0, n - 1))
            }
            d > 0 -> c.mas()
        }
    }

    /** A en el estudio. */
    fun aceptarControl() {
        val c = controlesEstudio().getOrNull(estado.value.indice) ?: return
        when (c.tipo) {
            "accion", "interruptor", "catalogo" -> c.mas()
            else -> cambiarControl(1)
        }
    }

    /** ← en un panel de valores. */
    fun menosEnPanel() {
        val e = estado.value
        if (e.panel == Panel.ESTUDIO) cambiarControl(-1)
        if (e.panel == Panel.EDITAR_CONSOLA) opcionesEditarConsola().getOrNull(e.indice)?.let { o -> (o.menos ?: o.accion).invoke() }
    }

    // ---------- Cajón de apps: el resto de las apps de la consola ----------

    val PESTANAS_APPS = listOf("Todas", "Fijadas", "Juegos")

    /** Abre el cajón con todas las apps de la consola (las carga la primera vez, sin trabar nada). */
    fun abrirApps() {
        actualizar { it.copy(panel = Panel.APPS, indice = 0, appsPestana = if (it.appsFijadas.isNotEmpty()) 1 else 0) }
        if (estado.value.appsTodas.isEmpty()) cargarApps()
    }

    fun cargarApps() {
        if (estado.value.cargandoTodas) return
        actualizar { it.copy(cargandoTodas = true) }
        scope.launch {
            val lista = withContext(Dispatchers.IO) {
                try {
                    val pm = app.packageManager
                    pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0).mapNotNull { r ->
                        val paquete = r.activityInfo?.packageName ?: return@mapNotNull null
                        if (paquete == app.packageName) return@mapNotNull null
                        val info = r.activityInfo.applicationInfo
                        @Suppress("DEPRECATION")
                        val juego = info.category == android.content.pm.ApplicationInfo.CATEGORY_GAME || (info.flags and android.content.pm.ApplicationInfo.FLAG_IS_GAME) != 0
                        App(paquete, r.loadLabel(pm)?.toString() ?: paquete, juego)
                    }.distinctBy { it.paquete }.sortedBy { Busqueda.normalizar(it.nombre) }
                } catch (_: Exception) {
                    emptyList()
                }
            }
            actualizar { it.copy(appsTodas = lista, cargandoTodas = false) }
        }
    }

    /** Las apps de la pestaña elegida. */
    fun appsVisibles(e: Estado = estado.value): List<App> = when (e.appsPestana) {
        1 -> e.appsFijadas.mapNotNull { p -> e.appsTodas.firstOrNull { it.paquete == p } }
        2 -> e.appsTodas.filter { it.esJuego }
        else -> e.appsTodas
    }

    fun cambiarPestanaApps(d: Int) = actualizar { it.copy(appsPestana = (it.appsPestana + d).mod(PESTANAS_APPS.size), indice = 0) }

    fun elegirPestanaApps(i: Int) = actualizar { it.copy(appsPestana = i, indice = 0) }

    /** Fija (o suelta) una app: las fijadas tienen su pestaña y su widget en el inicio. */
    fun alternarFijada(paquete: String) {
        val l = estado.value.appsFijadas
        val n = if (paquete in l) l - paquete else l + paquete
        almacen.guardarTexto("appsFijadas", n.joinToString(","))
        actualizar { it.copy(appsFijadas = n) }
        avisar(if (paquete in n) "App fijada" else "App soltada")
    }

    /** Abre la información de la app en los ajustes de Android (para desinstalar, permisos…). */
    fun infoApp(paquete: String) {
        abrirIntent(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$paquete")), "No pude abrir la información de la app")
    }

    // ---------- Personalizar consolas ----------

    /** ← y → al personalizar una consola: cambian los valores que tienen ‹ › (como el color). */
    fun ladoEditarConsola(d: Int) {
        val o = opcionesEditarConsola().getOrNull(estado.value.indice) ?: return
        if (o.menos == null) return
        if (d < 0) o.menos.invoke() else o.accion()
    }

    private var editarDesdeMenu = false

    private fun leerConsolasPropias(s: String): Map<String, ConsolaPropia> = try {
        val o = org.json.JSONObject(s)
        o.keys().asSequence().associateWith { k ->
            val c = o.getJSONObject(k)
            fun txt(n: String) = c.optString(n, "").ifEmpty { null }
            ConsolaPropia(txt("nombre"), c.optLong("color", 0L), txt("hero"), txt("logo"), txt("musica"))
        }
    } catch (e: Exception) {
        emptyMap()
    }

    private fun escribirConsolasPropias(m: Map<String, ConsolaPropia>): String = org.json.JSONObject().apply {
        m.forEach { (k, c) ->
            put(k, org.json.JSONObject().put("nombre", c.nombre ?: "").put("color", c.color).put("hero", c.hero ?: "").put("logo", c.logo ?: "").put("musica", c.musica ?: ""))
        }
    }.toString()

    private fun cambiarConsolaPropia(id: String, f: (ConsolaPropia) -> ConsolaPropia) {
        if (id.isBlank()) return
        val actual = estado.value.consolasPropias[id] ?: ConsolaPropia()
        val nueva = f(actual)
        val m = if (nueva.vacia) estado.value.consolasPropias - id else estado.value.consolasPropias + (id to nueva)
        almacen.guardarTexto("consolasPropias", escribirConsolasPropias(m))
        Consolas.personalizar(m.mapValues { (_, v) -> v.nombre to v.color })
        actualizar { it.copy(consolasPropias = m, iconosVersion = it.iconosVersion + 1) }
    }

    /** Abre la personalización de una consola (desde el menú, una pestaña o el carrusel). */
    fun abrirEditarConsola(id: String, desdeMenu: Boolean = false) {
        if (id == "todo" || Consolas.porId(id) == null) { avisar("Elige una consola (no \"Todo\") para personalizarla"); return }
        editarDesdeMenu = desdeMenu
        actualizar { it.copy(panel = Panel.EDITAR_CONSOLA, consolaEditando = id, indice = 0) }
    }

    private val COLORES_CONSOLA = listOf(0L) + Temas.MUESTRAS

    /** Todo lo que se puede cambiar de la consola que se está editando. */
    fun opcionesEditarConsola(): List<Opcion> {
        val e = estado.value
        val id = e.consolaEditando
        val c = Consolas.porId(id) ?: return emptyList()
        val p = e.consolasPropias[id] ?: ConsolaPropia()
        val f = e.formas[id] ?: FormaCfg()
        fun archivo(que: String, tipo: String = "image/*") = pedirArchivoPara("consola_$que:$id", tipo)
        return listOf(
            Opcion("Nombre", p.nombre ?: "Original", Icono.TITULO, "Cómo se llama esta consola en todo Díptico.") { abrirTexto("consola_nombre", p.nombre ?: "") },
            Opcion(
                "Color", if (p.color == 0L) "Original" else "Propio", Icono.PALETA, "El color de sus tarjetas, del carrusel y del fondo de su vista.",
                menos = { cambiarConsolaPropia(id) { it.copy(color = COLORES_CONSOLA[(COLORES_CONSOLA.indexOf(it.color).coerceAtLeast(0) - 1).mod(COLORES_CONSOLA.size)]) } },
            ) { cambiarConsolaPropia(id) { it.copy(color = COLORES_CONSOLA[(COLORES_CONSOLA.indexOf(it.color).coerceAtLeast(0) + 1).mod(COLORES_CONSOLA.size)]) } },
            Opcion("Ícono", if (e.iconosPropios.containsKey(id)) "Propio ✓" else "Elegir imagen", Icono.EMULADOR, "Cualquier imagen de tu consola como ícono.") { pedirImagen("icono:$id") },
            Opcion("Imagen grande (hero)", if (p.hero != null) "Propia ✓" else "Elegir imagen", Icono.PORTADA, "El fondo de esta consola en la vista grande y en su tarjeta del carrusel.") { archivo("hero") },
            Opcion("Logo", if (p.logo != null) "Propio ✓" else "Elegir imagen", Icono.ESTRELLA, "Un logo (mejor con fondo transparente) que reemplaza al nombre en la vista grande.") { archivo("logo") },
            Opcion("Música", if (p.musica != null) "Propia ✓" else "Elegir canción", Icono.MUSICA, "Suena mientras recorres esta consola (si el juego no tiene su propia canción).") { archivo("musica", "audio/*") },
            Opcion("Forma de las cajas", nombreForma(f.forma), Icono.FORMA) { ciclarForma(id) },
            Opcion("Tamaño de las cajas", nombreTamJuegos(f.tam), Icono.FORMA) { ciclarTamForma(id) },
            Opcion("Imagen de las cajas", nombreImagenCaja(f.imagen), Icono.PORTADA, "Portada, captura del juego o su logo.") { ciclarImagenCaja(id) },
            Opcion("Emulador", if (c.sinEmulador) "No necesita" else nombreEmulador(c), Icono.MANDO) { if (!c.sinEmulador) ciclarEmulador(c) },
            Opcion("Quitar imágenes y música propias", icono = Icono.BORRAR) {
                cambiarConsolaPropia(id) { it.copy(hero = null, logo = null, musica = null) }
                if (estado.value.iconosPropios.containsKey(id)) {
                    val m = estado.value.iconosPropios - id
                    almacen.guardarTexto("iconosPropios", escribirMapa(m))
                    actualizar { it.copy(iconosPropios = m) }
                }
            },
            Opcion("Volver a como venía", icono = Icono.BORRAR, ayuda = "Nombre, color, imágenes y música originales.") {
                cambiarConsolaPropia(id) { ConsolaPropia() }
                avisar("${Consolas.porId(id)?.nombre ?: ""} como al principio")
            },
        )
    }

    private fun moverWidgetPagina(i: Int, d: Int) {
        val e = estado.value
        val w = e.widgets.getOrNull(i) ?: return
        val p = (w.pagina + d).coerceIn(0, e.paginas - 1)
        if (p == w.pagina) { avisar("Agrega páginas en la pestaña Página"); return }
        val c = w.copy(pagina = p)
        if (choca(c, i)) {
            val l = reacomodar(e.widgets.toMutableList().also { it[i] = c.copy(x = 0, y = 0) }, e.paginas)
            actualizar { it.copy(widgets = l, paginaInicio = p) }
        } else {
            cambiarWidget(i) { c }
            actualizar { it.copy(paginaInicio = p) }
        }
    }

    private fun cambiarHueco(d: Int) {
        val l = listOf(4, 10, 16)
        val v = ciclar(l, estado.value.huecoInicio, d)
        almacen.guardarTexto("huecoInicio", v.toString())
        actualizar { it.copy(huecoInicio = v) }
    }

    fun ciclarGrilla(columnas: Boolean) {
        val e = estado.value
        if (columnas) guardarGrilla(if (e.gridCols >= 10) 3 else e.gridCols + 1, e.gridFilas, e.paginas)
        else guardarGrilla(e.gridCols, if (e.gridFilas >= 8) 2 else e.gridFilas + 1, e.paginas)
    }

    fun ciclarPaginas() {
        val e = estado.value
        guardarGrilla(e.gridCols, e.gridFilas, if (e.paginas >= 6) 1 else e.paginas + 1)
    }

    fun elegirFondoInicio() = pedirArchivoPara("fondoInicio", "image/*")

    fun quitarWidget(i: Int) {
        cambiarWidget(i) { it.copy(visible = false) }
        almacen.guardarTexto("widgets", escribirWidgets(estado.value.widgets))
        siguienteWidget(1)
    }

    /** L2/R2 o deslizar: otra página del inicio. */
    fun cambiarPagina(delta: Int) {
        val e = estado.value
        val p = (e.paginaInicio + delta).coerceIn(0, e.paginas - 1)
        if (p != e.paginaInicio) actualizar { it.copy(paginaInicio = p) }
    }

    fun irAPagina(p: Int) = actualizar { it.copy(paginaInicio = p.coerceIn(0, it.paginas - 1)) }

    private fun opcionesGrilla(e: Estado): List<Opcion> = listOf(
        Opcion("Columnas de la grilla", "${e.gridCols}", Icono.APPS, "Cuántas columnas tiene tu inicio (de 3 a 10).") { ciclarGrilla(true) },
        Opcion("Filas de la grilla", "${e.gridFilas}", Icono.APPS, "De 2 a 8 filas. Con más de 4, el inicio se desplaza hacia abajo.") { ciclarGrilla(false) },
        Opcion("Páginas", "${e.paginas}", Icono.VISTA, "Desliza a los lados con el dedo o con L2 y R2 para cambiar de página.") { ciclarPaginas() },
        Opcion("Agregar imagen o GIF", icono = Icono.PORTADA, ayuda = "Elige una imagen o un GIF animado de tu consola; aparece como widget en la página actual.") {
            pedirArchivoPara("widgetImagen", "image/*")
        },
        Opcion(
            "Fondo del inicio", if (e.fondoInicio != null) "Propio ✓" else "Arte del juego", Icono.PORTADA,
            "Usa una imagen o un GIF animado como fondo de tu inicio con widgets.",
        ) { pedirArchivoPara("fondoInicio", "image/*") },
        Opcion("Quitar fondo propio", icono = Icono.BORRAR) {
            almacen.guardarTexto("fondoInicio", "")
            actualizar { it.copy(fondoInicio = null) }
        },
    )
}

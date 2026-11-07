package cl.nikalbazu.diptico.estado

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Panel { WIDGET, ENCUADRE, APPS, EDITAR_CONSOLA, NINGUNO, RAPIDO, PASO, MENU, CLAVE, PORTS, FICHA, TRABAJOS, BUSCAR, CENTRO, CARPETAS, ESTADISTICAS, IMAGENES, ASISTENTE, GALERIA, ESTUDIO }

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
    COPIAR, PEGAR, BISA, EDITAR, MICROFONO,
}

/**
 * Secciones del menú, ordenadas por grupos: lo que más se cambia arriba, lo técnico abajo.
 * [grupo] es el título que aparece sobre la primera sección de cada grupo.
 */
enum class Seccion(val titulo: String, val icono: Icono, val ayuda: String, val grupo: String = "") {
    ESENCIAL("Lo esencial", Icono.ESTRELLA, "Lo que más se cambia, todo junto. Y para buscar cualquier ajuste por su nombre.", "Empieza aquí"),
    TEMAS("Temas", Icono.PALETA, "La tienda, los temas del calendario y tus mezclas.", "Cómo se ve"),
    DECORACIONES("Decoraciones", Icono.ESTRELLA, "Elige el adorno y la cantidad de elementos animados de cada tema."),
    APARIENCIA("Apariencia", Icono.TEMA, "Claro u oscuro, efecto de los botones, íconos de las consolas y la animación de entrada."),
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
    SOBREMESA("Sobremesa", Icono.VISTA, "Cuando conectas la consola a la tele: una sola pantalla, todo más grande, margen para que nada se corte y más."),
    RENDIMIENTO("Rendimiento", Icono.CHISPA, "Modo turbo al jugar: libera memoria cerrando apps de fondo, pone a Díptico en reposo y, si quieres, activa No molestar."),
    CONTROLES("Controles", Icono.MANDO, "Qué hace cada stick y los botones de atrás (L4 y R4 de la Odin 2 y otras): Bisa, apps, música, un juego al azar…"),
    CONSOLA("Android", Icono.CAMPANA, "Díptico como inicio, notificaciones y accesos rápidos de Android."),
    IDIOMA("Idioma", Icono.IDIOMA, "El idioma de Díptico: español, inglés, portugués, chino o japonés."),
    SISTEMA("Acerca de", Icono.INFO, "Respaldo en Google Drive, actualizaciones, reportar un problema, el tutorial y la versión de Díptico."),
    /** Solo en el modo simple: la puerta a las secciones avanzadas (nada queda escondido). */
    MAS("Más ajustes", Icono.AJUSTES, "Las secciones avanzadas, a un toque. Y para volver al menú completo.", "Avanzado"),
}

/** Un juego abierto ahora mismo. */
data class Sesion(val clave: String, val inicio: Long, val displayId: Int, val dosPantallas: Boolean)

data class BibliotecaAntesDeOcultos(
    val filtro: String, val busqueda: String, val consola: String, val juego: String?,
    val enSistemas: Boolean, val inicio: String,
)

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
    /** Para los accesos que se encienden y apagan (Wi-Fi, Bluetooth, turbo…): si está encendido ahora. null = no aplica. */
    val activa: Boolean? = null,
    val accion: () -> Unit,
)

/** Algo que Díptico hace de fondo (buscar juegos, bajar portadas…). [avance] va de 0 a 1, o null si no se sabe. */
data class Trabajo(val id: String, val texto: String, val avance: Float? = null)

/** Una colección propia: una carpeta con los juegos que tú elijas (de cualquier consola). */
data class Coleccion(val nombre: String, val juegos: List<String>)

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

/**
 * Una pregunta importante en una ventana (enviar un reporte, desinstalar un juego…).
 * [mantener]: para confirmar hay que mantener A apretado (lo que no se puede deshacer).
 */
data class Dialogo(
    val id: String,
    val titulo: String,
    val texto: String,
    val icono: Icono,
    val lineas: List<String> = emptyList(),
    val si: String = "Sí",
    val no: String = "No",
    val mantener: Boolean = false,
    val peligro: Boolean = false,
)

/** Personalización de una consola o de la carpeta Todo. */
data class ConsolaPropia(
    val nombre: String? = null,
    val color: Long = 0L,
    val hero: String? = null,
    val logo: String? = null,
    val musica: String? = null,
    val portadaCarpeta: String? = null,
) {
    val vacia: Boolean get() = nombre.isNullOrBlank() && color == 0L && hero == null && logo == null && musica == null && portadaCarpeta == null
}

/** Forma de las cajas de una consola en el estante. */
/** Un botón extra del control: su tecla de Android y la acción que le toca. */
data class BotonExtra(val nombre: String, val tecla: Int, val accion: String, val conStart: Boolean = false) {
    companion object {
        /** L4 y R4 suelen llegar como BUTTON_C y BUTTON_Z; si los tuyos son otros, Díptico los aprende. */
        val PORDEFECTO = listOf(
            BotonExtra("L4", android.view.KeyEvent.KEYCODE_BUTTON_C, "bisa"),
            BotonExtra("R4", android.view.KeyEvent.KEYCODE_BUTTON_Z, "musica"),
        )
        val ACCIONES = listOf("nada", "bisa", "musica", "siguiente", "apps", "buscar", "azar", "widgets", "vista", "centro", "favorito", "ficha", "intercambiar", "menu")
        fun nombreAccion(a: String) = when (a) {
            "bisa" -> "Hablar con Bisa"; "musica" -> "Pausar o seguir la música"; "siguiente" -> "Siguiente canción"; "apps" -> "Abrir las apps"
            "buscar" -> "Buscar un juego"; "azar" -> "Un juego al azar"; "widgets" -> "Widgets ↔ carátulas"; "vista" -> "Cambiar la vista"
            "centro" -> "Centro de control"; "favorito" -> "Favorito sí / no"; "ficha" -> "Ficha del juego"; "intercambiar" -> "Intercambiar pantallas"
            "menu" -> "Abrir el menú"; else -> "Nada"
        }
    }
}

/** Un juego de PC que Díptico abre con GameNative, GameHub Lite o el que elijas: su tienda y su número. */
data class JuegoPC(val titulo: String, val id: Int, val tienda: String = "STEAM")

data class FormaCfg(val forma: String = "original", val tam: Float = 1f, val imagen: String = "portada")

/** Cómo se encuadra una imagen dentro de su caja: hacia dónde se corre (-1 a 1) y cuánto se acerca. */
data class Encuadre(val x: Float = 0f, val y: Float = 0f, val zoom: Float = 1f)

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
        Widget("temporizador", 0, 0, 2, 1, false), Widget("cronometro", 0, 0, 2, 1, false),
        Widget("buscar", 0, 0, 2, 1, false),
        Widget("proximaPartida", 0, 0, 3, 2, false),
        Widget("colaJuegos", 0, 0, 3, 2, false),
        Widget("tengo20", 0, 0, 3, 1, false),
    )

    /** Para qué sirve cada widget (en el catálogo del estudio). */
    fun descripcion(tipo: String) = if (tipo.startsWith("atajo-")) "Un juego a un toque" else if (tipo.startsWith("coleccion-")) "Tu colección de juegos"
        else if (tipo.startsWith("video")) "Un video corto que se repite, sin sonido" else when (tipo) {
        "juego" -> "El juego elegido en grande, listo para jugar"; "reloj" -> "La hora y la fecha (digital, analógico o mínimo)"
        "musica" -> "Lo que suena en Spotify o YouTube Music, con botones"; "paso" -> "Lo que anotaste para seguir tu juego"
        "recientes" -> "Tus últimos juegos, a un toque"; "fiesta" -> "La fiesta del mundo de hoy o la próxima"
        "tiempo" -> "Cuántas horas llevas jugando"; "bateria" -> "La carga de la consola"; "consolas" -> "Cuántos juegos y consolas tienes"
        "favoritos" -> "Tus juegos favoritos"; "frase" -> "Una frase distinta cada día"; "estadisticas" -> "Tus horas por consola"
        "sugerencia" -> "Un juego al azar para descubrir"; "accesos" -> "Wi-Fi, Bluetooth, brillo y más"
        "notificaciones" -> "Lo último que llegó a tu consola"; "logros" -> "Tus logros de RetroAchievements"
        "asistente" -> "Un acceso directo para hablar con Bisa"
        "apps" -> "Tus apps fijadas y el cajón con todas"
        "temporizador" -> "Cuenta atrás y aviso mientras juegas"
        "cronometro" -> "Mide una partida y pausa cuando quieras"
        "buscar" -> "Abre la búsqueda de juegos con un toque"
        "proximaPartida" -> "Recuerda qué jugaste, tu próximo paso y tu mapa"
        "colaJuegos" -> "Ordena los juegos que quieres empezar o terminar"
        "tengo20" -> "Sugiere juegos que sueles jugar en sesiones breves"
        else -> "Tu imagen o GIF animado"
    }

    fun icono(tipo: String): Icono = if (tipo.startsWith("atajo-")) Icono.JUGAR else if (tipo.startsWith("coleccion-")) Icono.CARPETA
        else if (tipo.startsWith("video")) Icono.JUGAR else when (tipo) {
        "juego" -> Icono.JUGAR; "reloj" -> Icono.RELOJ; "musica" -> Icono.MUSICA; "paso" -> Icono.LAPIZ; "recientes" -> Icono.CONTINUAR
        "fiesta" -> Icono.MUNDO; "tiempo" -> Icono.RELOJ; "bateria" -> Icono.BATERIA; "consolas" -> Icono.BIBLIOTECA
        "favoritos" -> Icono.CORAZON; "frase" -> Icono.CHISPA; "estadisticas" -> Icono.TROFEO; "sugerencia" -> Icono.DADO
        "accesos" -> Icono.AJUSTES; "notificaciones" -> Icono.CAMPANA; "logros" -> Icono.TROFEO; "asistente" -> Icono.BISA; "apps" -> Icono.APPS
        "temporizador", "cronometro" -> Icono.RELOJ
        "buscar" -> Icono.BUSCAR
        "proximaPartida" -> Icono.CONTINUAR
        "colaJuegos" -> Icono.BIBLIOTECA
        "tengo20" -> Icono.RELOJ
        else -> Icono.PORTADA
    }

    /** Diseños posibles del contenido de cada widget. */
    fun variantes(tipo: String): List<String> = if (tipo.startsWith("imagen")) listOf("Ajustada", "Llenar")
        else if (tipo.startsWith("atajo-")) listOf("Portada", "Imagen grande", "Logo", "Logo sobre imagen") else when (tipo) {
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

    fun nombre(tipo: String) = if (tipo.startsWith("atajo-")) "Acceso directo" else if (tipo.startsWith("coleccion-")) "Colección"
        else if (tipo.startsWith("video")) "Video corto" else when (tipo) {
        "juego" -> "Juego elegido"; "reloj" -> "Reloj y fecha"; "musica" -> "Música (Spotify, YouTube Music…)"
        "paso" -> "Tu próximo paso"; "recientes" -> "Jugados hace poco"; "fiesta" -> "Fiestas del mundo"
        "tiempo" -> "Tiempo jugado"; "bateria" -> "Batería"; "consolas" -> "Tu colección"
        "favoritos" -> "Favoritos"; "frase" -> "Saludo del día"; "estadisticas" -> "Estadísticas"
        "sugerencia" -> "¿Qué juego hoy?"; "accesos" -> "Accesos rápidos"; "notificaciones" -> "Notificaciones"
        "logros" -> "Logros (RetroAchievements)"; "asistente" -> "Asistente con IA"; "apps" -> "Apps"
        "temporizador" -> "Temporizador"; "cronometro" -> "Cronómetro"; "buscar" -> "Buscar juegos"
        "proximaPartida" -> "Mi próxima partida"; "colaJuegos" -> "Cola de juegos"; "tengo20" -> "Tengo 20 minutos"
        else -> if (tipo.startsWith("imagen")) "Imagen o GIF" else tipo
    }
}

/** Descarga de portadas en curso. */
data class Descarga(val hechos: Int, val total: Int, val actual: String, val encontrados: Int)

data class Estado(
    val navegacion: NavegacionEstado = NavegacionEstado(),
    val experiencia: Experiencia = Experiencia(),
    val perfilesVisuales: Map<String, Experiencia> = emptyMap(),
    val colorUniverso: Long = 0L,
    val coleccionVisual: String? = null,
    val fichaFijada: String? = null,
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
    /** Para qué es el cuadro de texto: "sgdb", "ia_clave_gemini", "titulo" o "notas". */
    val textoPara: String = "sgdb",
    val tema: String = "diptico",
    val temaFestivo: Boolean = true,
    val decoraciones: Boolean = true,
    val temasPropios: List<Tema> = emptyList(),
    val adornosPorTema: Map<String, String> = emptyMap(),
    val intensidadPorTema: Map<String, String> = emptyMap(),
    val decoracionAbierta: String? = null,
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
    /** Motor de Bisa: auto, local, gemini u openai. */
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
    /** Cómo abre Díptico: "igual", "caratulas", "widgets", "consolas" o "ultimo". */
    val arranque: String = "igual",
    /** Ventana con una pregunta importante (null = ninguna) y desde cuándo se mantiene A para confirmarla. */
    val dialogo: Dialogo? = null,
    /** La cortina de arriba: "avisos" (notificaciones, bajando por la izquierda) o "ajustes" (rápidos, por la derecha). */
    val cortina: String = "ajustes",
    /** Respaldo en Drive: dónde se guarda (vacío = sin elegir) y cuándo fue el último. */
    val respaldoNombre: String = "",
    val respaldoUltimo: Long = 0L,
    val dialogoMantener: Long = 0L,
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
    /** Dirección de la página de la consola para mandar una clave desde el teléfono (null = no está abierta). */
    val enlaceUrl: String? = null,
    /** Personalización de cada consola (id -> lo que se cambió) y la consola que se está editando. */
    val consolasPropias: Map<String, ConsolaPropia> = emptyMap(),
    val consolaEditando: String = "",
    /** Cómo se ven las consolas: "arriba" (pestañas), "lado" (columna), "rueda" (dial) o "sistemas" (carrusel y luego juegos). */
    val vistaConsolas: String = "arriba",
    val modoCarpeta: String = "ventana",
    val carpetasHorizontal: Boolean = false,
    val tamCarpetas: Float = 1f,
    /** En modo sistemas: true mientras se elige la consola (antes de entrar a sus juegos). */
    val enSistemas: Boolean = false,
    val tamJuegos: Float = 1f,
    /** "todos", "favoritos", "recientes", "mas" (más jugados) o "sinjugar". */
    val filtro: String = "todos",
    val antesDeOcultos: BibliotecaAntesDeOcultos? = null,
    /** "az", "recientes" o "tiempo". */
    val orden: String = "az",
    /** Tu orden de las consolas y, por consola ("todo" = todos), tu orden de los juegos. */
    val ordenConsolas: List<String> = emptyList(),
    val ordenPropio: Map<String, List<String>> = emptyMap(),
    /** Moviendo un juego de lugar con la cruceta. */
    val ordenando: Boolean = false,
    val busqueda: String = "",
    val formas: Map<String, FormaCfg> = emptyMap(),
    /** Encuadres propios de las imágenes ("clave|portada" o "clave|arte"). */
    val encuadres: Map<String, Encuadre> = emptyMap(),
    /** Encuadrando: qué imagen y cómo va quedando (se ve en vivo antes de guardar). */
    val encuadreDe: String = "",
    val encuadreBorrador: Encuadre = Encuadre(),
    /** Acercamiento lento en las portadas elegidas y en la imagen grande. */
    val zoomLento: Boolean = true,
    val paqueteSonidos: String = "tema",
    /** Juegos de PC agregados desde Díptico (Steam por su número) y lo último que se buscó en Steam. */
    val juegosPC: List<JuegoPC> = emptyList(),
    val resultadosSteam: List<JuegoPC> = emptyList(),
    /** Modo sobremesa (tele): "auto" (al conectar la tele), "si" o "no"; y la tele que se ve conectada. */
    val sobremesaModo: String = "auto",
    val teleConectada: String? = null,
    val sobremesaTam: Float = 1.2f,
    val sobremesaMargen: Int = 3,
    val sobremesaUna: Boolean = true,
    val sobremesaEfectos: Boolean = true,
    val sobremesaSonidos: Boolean = true,
    /** Botones extra (L4, R4…): qué tecla es cada uno y qué hace. Y si se está esperando que aprietes uno para aprenderlo. */
    val botonesExtra: List<BotonExtra> = BotonExtra.PORDEFECTO,
    val aprendiendoBoton: Int = -1,
    /** Navegar los widgets con la cruceta: cuál tiene el foco (-1 = ninguno, el control maneja el estante). */
    val widgetFoco: Int = -1,
    /** Cuándo se llegó a los widgets (para el recordatorio discreto de botones). */
    val avisoInicio: Long = 0L,
    /** Galería de cajas: para qué consola se está eligiendo la forma. */
    val galeriaConsola: String = "",
    /** Rendimiento al jugar: modo turbo, cerrar apps de fondo, no molestar y Díptico en reposo. */
    val turbo: Boolean = true,
    /** La barra flotante del inicio (arriba) está a la vista; se esconde sola salvo que [barraFija]. */
    val barra: Boolean = true,
    val barraFija: Boolean = true,
    /** Menú de ajustes simple: solo las secciones de siempre; las avanzadas viven en «Más ajustes». */
    val menuSimple: Boolean = true,
    val turboCerrarApps: Boolean = true,
    val turboNoMolestar: Boolean = false,
    val turboReposo: Boolean = true,
    val turboUltimo: String = "",
    /** Sticks: "dividido" (izquierdo juegos, derecho widgets), "invertido" o "clasico" (izquierdo = cruceta). */
    val sticks: String = "dividido",
    /** ¿En qué pantalla lo abro?: "juego" o "app:paquete" mientras se elige; la opción marcada y si se recuerda. */
    val donde: String? = null,
    val dondeSel: Int = 0,
    val dondeRecordar: Boolean = false,
    /** Tour de novedades de esta versión: la página que se ve (-1 = cerrado). */
    val novedades: Int = -1,
    /** true: el tour que se ve es el recorrido de la primera vez (no las novedades de la versión). */
    val recorrido: Boolean = false,
    /** Última versión cuyas novedades ya viste: el tour junta las de todas las versiones posteriores. */
    val novedadesDesde: String = "",
    /** Actualizaciones: la versión nueva encontrada, el avance de la descarga (-1 = sin descargar) y si se buscan solas. */
    val actualizacion: VersionNueva? = null,
    val descargaAct: Float = -1f,
    val actualizarSolo: Boolean = true,
    val buscandoAct: Boolean = false,
    /** Lo que Bisa está haciendo mientras piensa ("Buscando en internet…"), para mostrarlo. */
    val pensandoQue: String = "",
    /** Cambia cuando un acceso del centro de control se enciende o apaga (para redibujarlo). */
    val centroVersion: Int = 0,
    /** Colecciones propias (id -> colección); se ven como widgets en el inicio. */
    val colecciones: Map<String, Coleccion> = emptyMap(),
    /** Cuentas y claves de más fuentes de imágenes (IGDB, ScreenScraper, RAWG, TheGamesDB). */
    val raspadores: cl.nikalbazu.diptico.data.Raspadores.Claves = cl.nikalbazu.diptico.data.Raspadores.Claves(),
    /** "¿Dónde quedaste?" al terminar de jugar: si se pregunta, la tarjeta abierta, la opción marcada y si se está grabando (desde cuándo). */
    val preguntarQuedaste: Boolean = true,
    val quedaste: Boolean = false,
    val quedastePregunta: Boolean = false,
    val quedasteSel: Int = 0,
    val grabando: Long = 0L,
    /** Mapas de los juegos (una imagen y dónde quedaste) y el que se está marcando. */
    val mapas: Map<String, MapaJuego> = emptyMap(),
    val marcandoMapa: String? = null,
    val pinX: Float = 0.5f,
    val pinY: Float = 0.5f,
    /** Para que la continuación sepa qué notas de voz existen sin leer el disco a cada rato. */
    val notasVoz: Set<String> = emptySet(),
    /** Qué imagen del juego se ve en grande en el inicio: "arte" (imagen grande) o "portada"; y la elegida por juego. */
    val imagenHeroe: String = "arte",
    val heroes: Map<String, String> = emptyMap(),
    /** En la consola "Todo": todas las cajas con la misma forma ("poster", "cuadrada") o cada consola la suya ("propia"). */
    val formaTodos: String = "poster",
    /** El widget abierto (su contenido) en Panel.WIDGET. */
    val widgetAbierto: Int = -1,
    /** Reloj: temporizador (cuándo termina y cuánto dura), cronómetro y cada cuánto recordar una pausa al jugar. */
    val temporizadorFin: Long = 0L,
    val temporizadorMin: Int = 10,
    val cronoInicio: Long = 0L,
    val cronoAcumulado: Long = 0L,
    val pausaCada: Int = 0,
    /** Sugerencias del widget "¿Qué juego hoy?" (cambian al pedir otras) y la frase extra elegida. */
    val sugeridas: List<String> = emptyList(),
    val fraseExtra: Int = 0,
    val editandoWidgets: Boolean = false,
    val widgetSel: Int = 0,
    /** Estudio de widgets: pestaña (0 widget, 1 agregar, 2 página) y si la cruceta está moviendo el widget. */
    val estudioPestana: Int = 0,
    val catalogoTemas: List<TemaComunidad> = emptyList(),
    val estudioMover: Boolean = false,
    /** En el editor de widgets: la cruceta elige widgets en el inicio ("grilla") o recorre el panel ("panel"). */
    val estudioZona: String = "grilla",
    /** Burbuja para agregar un widget: en qué celda se abrió (x, y) y cuál se está eligiendo. */
    val burbuja: Pair<Int, Int>? = null,
    val burbujaSel: Int = 0,
    val celdaCatalogo: Pair<Int, Int>? = null,
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
    val atajoApps get() = navegacion.atajoApps
    val atajoBisa get() = navegacion.atajoBisa
    val ordenApps get() = navegacion.ordenApps
    val appsMenu get() = navegacion.appsMenu
    val appsMenuIndice get() = navegacion.appsMenuIndice
    val appsMoviendo get() = navegacion.appsMoviendo
    val estanteAvanzado get() = navegacion.estanteAvanzado
    val clipRetoma get() = navegacion.clipRetoma
    val esperaRetoma get() = navegacion.esperaRetoma
    val retomaVideo get() = navegacion.retomaVideo
    val retomaVideoClave get() = navegacion.retomaVideoClave
    val retomaVideoFecha get() = navegacion.retomaVideoFecha
    val retomaApagadaEn get() = navegacion.retomaApagadaEn

    /** Conectada a la tele (o forzado en los ajustes): se usa el perfil de sobremesa. */
    val sobremesa: Boolean
        get() = configurado && (sobremesaModo == "si" || (sobremesaModo == "auto" && teleConectada != null))

    /** Todo Díptico vive en una sola pantalla (siempre durante el tutorial, y en la tele si así se eligió). */
    val unaPantalla: Boolean
        get() = !configurado || modoPantallas != "dos" || !dosDisponibles || (sobremesa && sobremesaUna)

    val eligiendoConsola: Boolean
        get() = enSistemas && vistaConsolas in Diptico.MODOS_ELEGIR && (!unaPantalla || inicio != "widgets")

    val carpetaEnVentana: Boolean
        get() = vistaConsolas == "carpetas" && !enSistemas && modoCarpeta != "completa" && sesion == null

    /** Se está escribiendo con el teclado de Díptico (en la pantalla contraria a la del texto). */
    val conTeclado: Boolean
        get() = tecladoPropio && (panel == Panel.PASO || panel == Panel.CLAVE)

    private val biblioteca by lazy { IndiceBiblioteca.de(juegos) }
    val juegosPorConsola: Map<String, List<Juego>> get() = biblioteca.porConsola
    val juegosPorClave: Map<String, Juego> get() = biblioteca.porClave

    val consolasConJuegos: List<Consola> by lazy {
        val lista = Consolas.todas.filter { it.id in juegosPorConsola }
        val orden = ordenConsolas.withIndex().associate { (i, id) -> id to i }
        if (orden.isEmpty()) lista else lista.sortedBy { orden[it.id] ?: Int.MAX_VALUE }
    }

    /** Consolas que usan emulador (todas menos Ports). */
    val consolasEmu: List<Consola>
        get() = consolasConJuegos.filter { !it.sinEmulador }

    val pestanas: List<String> by lazy { listOf("todo") + consolasConJuegos.map { it.id } }

    /** La selección y los paneles pueden cambiar sin volver a filtrar ni ordenar la biblioteca. */
    val visibles: List<Juego> by lazy { biblioteca.visibles(this) }

    val indiceSel: Int by lazy { visibles.indexOfFirst { it.clave == juegoSel }.coerceAtLeast(0) }
    val seleccionado: Juego? get() = visibles.getOrNull(indiceSel)
    val sinPortada: Int get() = biblioteca.sinPortada
}

/** Para buscar sin importar tildes ni mayúsculas. */
object Busqueda {
    private val marcas = Regex("\\p{M}")

    fun normalizar(s: String): String =
        java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replace(marcas, "").lowercase()
}

/** Estado compartido por las dos pantallas y todas las acciones de Díptico. */
object Diptico {
    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado

    internal lateinit var app: Context
    internal lateinit var almacen: Almacen
    /** Una tarea de fondo que falla no cierra Díptico: se anota para el reporte de fallas y se sigue. */
    private val sinCaidas = kotlinx.coroutines.CoroutineExceptionHandler { _, error ->
        if (::app.isInitialized) Fallas.anotar(app, error)
    }
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + sinCaidas)
    private var avisoJob: Job? = null
    private var despiertaJob: Job? = null
    private var descargaJob: Job? = null
    private var introInicio = 0L
    private var crudos: List<Juego> = emptyList()
    private var accesosAndroid: Map<Uri, String> = emptyMap()

    private fun esAccesoAndroid(j: Juego): Boolean =
        j.uri.scheme != "app" && Consolas.porId(j.consolaId)?.comandos?.any { it.abreApp } == true

    private suspend fun leerAccesosAndroid(juegos: List<Juego>): Map<Uri, String> = withContext(Dispatchers.IO) {
        juegos.filter(::esAccesoAndroid).mapNotNull { j ->
            Lanzador.paqueteDeAcceso(app, j.uri)?.let { j.uri to it }
        }.toMap()
    }
    private var medios: Map<String, MediosJuego> = emptyMap()

    val VERSION: String = cl.nikalbazu.diptico.BuildConfig.VERSION_NAME.substringBefore("-beta.") + " beta"

    /** La pantalla activa registra aquí cómo pedir una carpeta. */
    var pedirCarpeta: ((Carpeta) -> Unit)? = null

    fun iniciar(ctx: Context) {
        if (::app.isInitialized) return
        app = ctx.applicationContext
        almacen = Almacen(app)
        Consolas.cargar(app)
        Musica.iniciar(app)
        val carpetas = almacen.carpetas()
        val raices = carpetas.mapTo(HashSet()) { it.uri }
        medios = almacen.medios()
        crudos = almacen.biblioteca().filter { raizDe(it) in raices }
        val idioma = almacen.texto("idioma").ifEmpty { Idioma.delSistema() }
        Idioma.fijar(app, idioma)
        val apps = AtajoInicio.leer(almacen.texto("atajoApps"), AtajoInicio.START_R3)
        val bisa = AtajoInicio.leer(almacen.texto("atajoBisa"), AtajoInicio.START_L3)
            .let { if (it == apps && it != AtajoInicio.NO) AtajoInicio.NO else it }
        val vistaGuardada = almacen.texto("vistaConsolas", "arriba")
        val vistaConsolas = vistaConsolasCompatible(vistaGuardada)
        if (vistaConsolas != vistaGuardada) almacen.guardarTexto("vistaConsolas", vistaConsolas)
        _estado.value = Estado(
            navegacion = NavegacionEstado(atajoApps = apps.id, atajoBisa = bisa.id, ordenApps = almacen.texto("ordenApps", "").split(",").filter { it.isNotBlank() }, clipRetoma = almacen.bool("clipRetoma", false), esperaRetoma = almacen.texto("esperaRetoma", "30").toIntOrNull()?.coerceIn(15, 300) ?: 30),
            listo = true,
            idioma = idioma,
            configurado = almacen.configurado && carpetas.isNotEmpty(),
            tieneCarpeta = carpetas.isNotEmpty(),
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
            modoCarpeta = almacen.texto("modoCarpeta", "ventana"),
            carpetasHorizontal = almacen.bool("carpetasHorizontal", false),
            tamCarpetas = limitarTamCarpetas(almacen.texto("tamCarpetas").toFloatOrNull() ?: 1f),
            experiencia = Experiencia.leer(almacen.texto("experiencia", "{}")),
            perfilesVisuales = Experiencia.perfiles(almacen.texto("perfilesVisuales", "{}")),
            temasPropios = leerTemas(almacen.texto("temasPropios", "[]")),
            adornosPorTema = leerMapa(almacen.texto("adornosPorTema", "{}")),
            intensidadPorTema = leerMapa(almacen.texto("intensidadPorTema", "{}")),
            // La clave de Claude ya no se usa: se borra de la consola (cada persona usa Gemini, ChatGPT o la IA local).
                        claveGemini = almacen.texto("claveGemini").also { if (almacen.texto("claveIa").isNotEmpty()) almacen.guardarTexto("claveIa", "") },
            claveOpenAI = almacen.texto("claveOpenAI"),
            usuarioRA = almacen.texto("usuarioRA"),
            bisaWeb = almacen.bool("bisaWeb", true),
            // Claude ya no es una opción: quien la tenía elegida pasa a automático.
                        motorIA = almacen.texto("motorIA", "auto").let { if (it == "claude") "auto" else it },
            iaLocalLista = IALocal.listo(app),
            tamLogo = almacen.texto("tamLogo", "1").toFloatOrNull() ?: 1f,
            atenuacion = almacen.texto("atenuacion", "0.04").toFloatOrNull() ?: 0.04f,
            bisaEnJuego = almacen.bool("bisaEnJuego", true),
            claveRA = almacen.texto("claveRA"),
            inicio = almacen.texto("inicio", "heroe"),
            arranque = almacen.texto("arranque", "igual"),
            respaldoNombre = almacen.texto("respaldoNombre"),
            respaldoUltimo = almacen.texto("respaldoUltimo").toLongOrNull() ?: 0L,
            widgets = leerWidgets(almacen.texto("widgets")),
            estiloIconos = almacen.texto("estiloIconos", "diptico"),
            iconosPropios = leerMapa(almacen.texto("iconosPropios", "{}")),
            vista = almacen.texto("vista", "estante"),
            vistaConsolas = vistaConsolas,
            consolasPropias = leerConsolasPropias(almacen.texto("consolasPropias", "{}")),
            appsFijadas = almacen.texto("appsFijadas").split(",").filter { it.isNotBlank() },
            enSistemas = vistaConsolas in MODOS_ELEGIR,
            tamJuegos = almacen.texto("tamJuegos", "1").toFloatOrNull() ?: 1f,
            orden = almacen.texto("orden", "az"),
            ordenConsolas = almacen.texto("ordenConsolas").split(",").filter { it.isNotBlank() },
            ordenPropio = leerOrdenPropio(almacen.texto("ordenPropio", "{}")),
            formas = leerFormas(almacen.texto("formas", "{}")),
            encuadres = leerEncuadres(almacen.texto("encuadres", "{}")),
            zoomLento = almacen.bool("zoomLento", true),
            paqueteSonidos = almacen.texto("paqueteSonidos", "tema"),
            pausaCada = almacen.texto("pausaCada", "0").toIntOrNull() ?: 0,
            turbo = almacen.bool("turbo", true),
            barraFija = true,
            menuSimple = almacen.bool("menuSimple", true),
            turboCerrarApps = almacen.bool("turboCerrarApps", true),
            turboNoMolestar = almacen.bool("turboNoMolestar", false),
            turboReposo = almacen.bool("turboReposo", true),
            sticks = almacen.texto("sticks", "dividido"),
            preguntarQuedaste = almacen.bool("preguntarQuedaste", true),
            mapas = leerMapas(almacen.texto("mapas", "{}")),
            notasVoz = leerNotasVoz(),
            imagenHeroe = almacen.texto("imagenHeroe", "arte"),
            heroes = leerMapa(almacen.texto("heroes", "{}")),
            formaTodos = almacen.texto("formaTodos", "poster"),
            colecciones = leerColecciones(almacen.texto("colecciones", "{}")),
            raspadores = leerRaspadores(),
            actualizarSolo = almacen.bool("actualizarSolo", true),
            sobremesaModo = almacen.texto("sobremesaModo", "auto"),
            sobremesaTam = almacen.texto("sobremesaTam", "1.2").toFloatOrNull() ?: 1.2f,
            sobremesaMargen = almacen.texto("sobremesaMargen", "3").toIntOrNull() ?: 3,
            sobremesaUna = almacen.bool("sobremesaUna", true),
            sobremesaEfectos = almacen.bool("sobremesaEfectos", true),
            sobremesaSonidos = almacen.bool("sobremesaSonidos", true),
            botonesExtra = leerBotonesExtra(almacen.texto("botonesExtra", "")),
            juegosPC = leerJuegosPC(almacen.texto("juegosPC", "")),
            temporizadorMin = almacen.texto("temporizadorMin", "10").toIntOrNull() ?: 10,
            zoomArriba = almacen.texto("zoomArriba", "1").toFloatOrNull() ?: 1f,
            zoomAbajo = almacen.texto("zoomAbajo", "1").toFloatOrNull() ?: 1f,
            musicaTema = almacen.bool("musicaTema", true),
            avisosEmergentes = almacen.bool("avisosEmergentes", true),
            carpetas = carpetas,
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
        Sonidos.paquete = almacen.texto("paqueteSonidos", "tema")
        aplicarFluidez()
        vigilarProtector()
        vigilarPausas()
        vigilarTele()
        vigilarReposo()
        vigilarBateria()
        vigilarActualizaciones()
        // Tour de novedades: una vez por versión (en una instalación nueva lo reemplaza el tutorial).
        if (!estado.value.configurado) almacen.guardarTexto("novedadesVistas", Novedades.ID)
        // Recién actualizado: el tour sale al tiro, sin esperar la animación de entrada.
        else if (almacen.texto("novedadesVistas") != Novedades.ID) {
            val desde = almacen.texto("novedadesVistas")
            actualizar { it.copy(novedades = 0, recorrido = false, novedadesDesde = desde, intro = false) }
        }
        aplicarArranque()
        vigilarFila()
        revisarCaida()
        respaldoDiario()
        // Avisos emergentes de las notificaciones (nunca mientras juegas, y de a uno: esperan si hay algo encima).
        scope.launch {
            Avisos.nuevos.collect { a ->
                val e = estado.value
                if (e.avisosEmergentes && e.sesion == null && e.aLaVista > 0) {
                    val texto = "${a.app}: ${a.titulo.ifBlank { a.texto }}".take(90)
                    cuandoHayaLugar("aviso-${a.app}") { avisar(texto); Sonidos.aviso() }
                }
            }
        }
        vigilarUniverso()
        aplicarTema()
        publicarJuegos()
        vigilarAppsInstaladas()
        Medios.revisar(app)
        if (almacen.android == null) detectarAndroid(silencioso = true)
        if (carpetas.isNotEmpty()) escanear()
        // La música sigue al estado: suena con Díptico a la vista y sin juego abierto.
        scope.launch {
            estado.map { e ->
                // Si el juego elegido tiene su propia canción, suena esa.
                val propia = if (e.configurado && e.panel != Panel.MENU) {
                    e.coleccionVisual?.let { e.experiencia.colecciones[it]?.musica }?.takeIf { it.isNotBlank() } ?: e.seleccionado?.let { e.datos[it.clave]?.musica } ?: e.consolasPropias[e.consolaSel]?.musica
                } else null
                val pista = when {
                    propia != null -> "archivo:$propia"
                    e.configurado && e.musicaTema && e.pista != "mia" -> e.pistaTema
                    else -> e.pista
                }
                // Se calla mientras grabas una nota de voz o escuchas una.
                listOf(e.listo && e.musica && e.aLaVista > 0 && e.sesion == null && e.grabando == 0L, pista, e.volumen, e.carpetaMusica)
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
        // Volviste a Díptico (por ejemplo desde un juego): la música de fondo vuelve si se había cortado.
        if (delta > 0 && estado.value.sesion == null) { Musica.alVolver(); mostrarBarra() }
    }

    // ---------- Barra flotante ----------

    /** Vuelve a mostrar la barra de inicio al tocar la pantalla o volver del juego. */
    fun mostrarBarra() { if (!estado.value.barra) actualizar { it.copy(barra = true) } }

    fun cambiarBarraFija(v: Boolean) {
        almacen.guardarBool("barraFija", v)
        actualizar { it.copy(barraFija = v, barra = true) }
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
            .firstNotNullOfOrNull { (k, _) -> e.juegosPorClave[k] }

    /** La pantalla se encendió después de [ausencia] ms apagada. Se llama desde [Inicio]. */
    fun alEncender(ausencia: Long, estabaJugando: Boolean) {
        val e = estado.value
        if (!e.configurado || e.intro) return
        if (e.sesion != null) {
            if (estabaJugando && ausencia > 0L) actualizar { it.copy(sesion = it.sesion?.let { s -> s.copy(inicio = s.inicio + ausencia) }) }
            if (estabaJugando && ausencia >= e.esperaRetoma * 1000L) mostrarContinuacion()
            return
        }
        // Solo si al apagarla estabas en Díptico (no en otra app).
        if (ausencia > 20_000 && e.aLaVista > 0) mostrarBienvenida(ausencia)
    }

    // ---------- Una cosa a la vez ----------

    /** Lo que espera su turno para aparecer: «¿dónde quedaste?», avisos de notificaciones… (una entrada por clave). */
    private val enFila = java.util.concurrent.ConcurrentLinkedQueue<Pair<String, () -> Unit>>()

    /** ¿Hay algo grande encima de la pantalla ahora mismo (tour, bienvenida, tarjetas)? */
    fun hayAlgoEncima(e: Estado = estado.value): Boolean =
        e.intro || e.dialogo != null || e.novedades >= 0 || e.bienvenida > 0L || e.quedaste || e.donde != null || e.marcandoMapa != null ||
            e.continuacion > 0L || e.relojGrande || !e.configurado

    /** Hace [accion] ahora si la pantalla está libre; si no, la deja en la fila para cuando se libere. */
    private fun cuandoHayaLugar(clave: String, accion: () -> Unit) {
        if (!hayAlgoEncima() && enFila.isEmpty()) { accion(); return }
        enFila.removeIf { it.first == clave }
        enFila.add(clave to accion)
        // Los avisos viejos no sirven: la fila guarda como mucho 6 cosas.
        while (enFila.size > 6) enFila.poll()
    }

    /** Saca de la fila lo que espera, de a uno, con una pausa entre cosa y cosa. */
    private fun vigilarFila() {
        scope.launch {
            while (true) {
                delay(700)
                if (enFila.isEmpty() || hayAlgoEncima()) continue
                delay(600)
                if (hayAlgoEncima()) continue
                val sig = enFila.poll() ?: continue
                try { sig.second() } catch (_: Exception) {}
            }
        }
    }

    /** Cómo abre Díptico: como lo dejaste, carátulas, widgets, tus consolas o el último juego (Menú › Inicio). */
    private fun aplicarArranque() {
        when (estado.value.arranque) {
            "caratulas" -> actualizar { it.copy(inicio = "heroe") }
            "widgets" -> actualizar { it.copy(inicio = "widgets") }
            "consolas" -> actualizar {
                it.copy(inicio = "heroe", enSistemas = true, vistaConsolas = if (it.vistaConsolas in MODOS_ELEGIR) it.vistaConsolas else "carpetas")
            }
            "ultimo" -> scope.launch {
                actualizar { it.copy(inicio = "heroe") }
                // Los juegos pueden tardar un poco en cargarse.
                repeat(40) {
                    val j = ultimoJugado()
                    if (j != null) { actualizar { it.copy(consolaSel = "todo", juegoSel = j.clave, enSistemas = false) }; return@launch }
                    delay(250)
                }
            }
        }
    }

    // ---------- Ventanas de pregunta ----------

    private var dialogoSi: (() -> Unit)? = null
    private var dialogoNo: (() -> Unit)? = null
    private var mantenerJob: kotlinx.coroutines.Job? = null

    /** Tiempo que hay que mantener A para confirmar algo que no se puede deshacer. */
    const val MANTENER_MS = 1600L

    fun mostrarDialogo(d: Dialogo, no: () -> Unit = {}, si: () -> Unit) {
        dialogoSi = si; dialogoNo = no
        actualizar { it.copy(dialogo = d, dialogoMantener = 0L) }
        Sonidos.abrir()
    }

    fun responderDialogo(si: Boolean) {
        val accion = if (si) dialogoSi else dialogoNo
        dialogoSi = null; dialogoNo = null
        mantenerJob?.cancel()
        actualizar { it.copy(dialogo = null, dialogoMantener = 0L) }
        if (si) Sonidos.aceptar() else Sonidos.atras()
        try { accion?.invoke() } catch (ex: Exception) { Fallas.anotar(app, ex); avisar("No se pudo completar") }
    }

    /** Se empieza a mantener A (o el dedo) sobre una pregunta que lo pide. */
    fun empezarMantener() {
        val d = estado.value.dialogo ?: return
        if (!d.mantener) return
        if (estado.value.dialogoMantener > 0L) return
        actualizar { it.copy(dialogoMantener = System.currentTimeMillis()) }
        mantenerJob?.cancel()
        mantenerJob = scope.launch {
            delay(MANTENER_MS)
            if (estado.value.dialogo?.id == d.id && estado.value.dialogoMantener > 0L) responderDialogo(true)
        }
    }

    /** Se soltó antes de tiempo: no pasa nada. */
    fun soltarMantener() {
        if (estado.value.dialogoMantener == 0L) return
        mantenerJob?.cancel()
        actualizar { it.copy(dialogoMantener = 0L) }
    }

    /** Si Díptico se cerró de golpe la vez anterior, ofrece mandar el reporte (cuando la pantalla esté libre). */
    private fun revisarCaida() {
        val informe = Fallas.pendiente(app) ?: return
        cuandoHayaLugar("falla") {
            mostrarDialogo(
                Dialogo(
                    "falla", "Díptico se cerró de golpe",
                    "La última vez Díptico se cerró solo. Puedes contar qué pasó y enviar el reporte al equipo de Díptico.",
                    Icono.INFO, si = "Preparar reporte", no = "Ahora no",
                ),
            ) { prepararReporte(informe) }
        }
    }

    private var reportePendiente: Fallas.Reporte? = null
    private var enviandoReporte = false

    private fun prepararReporte(caida: String? = null) {
        if (enviandoReporte) { avisar("El reporte se está enviando"); return }
        val detalle = caida ?: Fallas.informe(app, "Reporte de un problema", null)
        val reporte = reportePendiente?.takeIf { it.caida == (caida != null) }
            ?: Fallas.Reporte(detalle, caida != null)
        reportePendiente = reporte
        abrirTexto("reporte_fallas", reporte.nota)
    }

    private fun confirmarReporte(nota: String) {
        val reporte = reportePendiente?.copy(nota = nota.take(1000)) ?: return
        if (enviandoReporte) return
        if (!reporte.caida && reporte.nota.isBlank()) { avisar("Escribe una descripción breve"); return }
        reportePendiente = reporte
        if (!Fallas.configurado) {
            ofrecerCopiarReporte(reporte, "El envío directo aún no está configurado.")
            return
        }
        mostrarDialogo(Dialogo(
            "enviar_reporte", "¿Enviar el reporte a Díptico?",
            "Se enviarán tu descripción, la versión, el equipo y el diagnóstico técnico al equipo de Díptico.",
            Icono.INFO, si = "Enviar", no = "Volver",
        )) {
            enviandoReporte = true
            avisar("Enviando el reporte…")
            scope.launch {
                try {
                    if (withContext(Dispatchers.IO) { Fallas.enviar(reporte) }) {
                        reportePendiente = null
                        if (reporte.caida) Fallas.olvidar(app)
                        if (estado.value.panel == Panel.CLAVE && estado.value.textoPara == "reporte_fallas") cerrarPanel()
                        avisar("Reporte enviado · ${reporte.id.take(8)}")
                    } else if (estado.value.panel == Panel.CLAVE && estado.value.textoPara == "reporte_fallas") {
                        ofrecerCopiarReporte(reporte, "No se pudo confirmar el envío. Puedes volver y reintentar.")
                    } else avisar("No se pudo confirmar el envío del reporte")
                } finally {
                    enviandoReporte = false
                }
            }
        }
    }

    private fun ofrecerCopiarReporte(reporte: Fallas.Reporte, motivo: String) {
        mostrarDialogo(Dialogo(
            "copiar_reporte", "Guardar el texto del reporte",
            "$motivo Puedes copiar el diagnóstico y guardarlo para compartirlo después.",
            Icono.INFO, si = "Copiar", no = "Volver",
        )) {
            portapapeles()?.setPrimaryClip(android.content.ClipData.newPlainText("Reporte de Díptico", Fallas.textoReporte(reporte)))
            avisar("Reporte copiado")
        }
    }

    // ---------- Respaldo en Google Drive ----------

    /** Selectores de archivos de Android (los pone la pantalla activa). */
    var pedirCrearRespaldo: ((String) -> Unit)? = null
    var pedirTraerRespaldo: (() -> Unit)? = null

    private fun opcionesRespaldo(e: Estado): List<Opcion> {
        val conDrive = e.respaldoNombre.isNotBlank()
        val cuando = cuandoFue(e.respaldoUltimo)
        return listOfNotNull(
            Opcion(
                "Respaldo en Google Drive", if (conDrive) "Cada día ✓" else "Elegir dónde", Icono.DESCARGA,
                "Guarda todos tus ajustes, colecciones, tiempo de juego, notas y notas de voz en un archivo en tu Google Drive (elige Drive en la ventana que se abre). " +
                    "Después Díptico lo actualiza solo una vez al día. Además, Android guarda una copia en tu cuenta de Google y la devuelve si reinstalas Díptico.",
            ) { if (conDrive) respaldarAhora() else elegirDondeRespaldar() },
            if (conDrive) Opcion("Respaldar ahora", "Último: $cuando", Icono.CHECK, "Guarda el respaldo en ${e.respaldoNombre} en este momento.") { respaldarAhora() } else null,
            if (conDrive) Opcion("Cambiar dónde se guarda", e.respaldoNombre.take(28), Icono.CARPETA, "Elige otro archivo u otra carpeta para el respaldo.") { elegirDondeRespaldar() } else null,
            Opcion(
                "Traer un respaldo", icono = Icono.PEGAR,
                ayuda = "Elige un respaldo de Díptico (de Drive o de donde lo tengas) y recupera todo. Sirve para pasar tus cosas a otra consola. Reemplaza tus ajustes actuales.",
            ) { pedirTraerRespaldo?.invoke() ?: avisar("No pude abrir el selector de archivos") },
        )
    }

    /** "hoy 14:05", "ayer 09:30" o "3 nov" (en el idioma de Díptico). */
    private fun cuandoFue(t: Long): String {
        if (t <= 0L) return Idioma.t("nunca")
        val c = java.util.Calendar.getInstance(); val hoy = c.get(java.util.Calendar.DAY_OF_YEAR) to c.get(java.util.Calendar.YEAR)
        c.timeInMillis = t
        val dia = c.get(java.util.Calendar.DAY_OF_YEAR) to c.get(java.util.Calendar.YEAR)
        val hora = String.format(java.util.Locale.US, "%02d:%02d", c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE))
        return when {
            dia == hoy -> Idioma.t("hoy") + " " + hora
            dia.second == hoy.second && dia.first == hoy.first - 1 -> Idioma.t("ayer") + " " + hora
            else -> Idioma.fecha("dMMM", java.util.Date(t))
        }
    }

    private fun elegirDondeRespaldar() {
        val fecha = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        pedirCrearRespaldo?.invoke("Diptico-respaldo-$fecha.zip") ?: avisar("No pude abrir el selector de archivos")
    }

    /** Volvió el selector con el archivo nuevo (por ejemplo en Drive): se guarda el permiso y se respalda al tiro. */
    fun respaldoCreado(uri: Uri) {
        try {
            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } catch (_: Exception) {}
        val nombre = try {
            app.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (_: Exception) { null } ?: "tu respaldo"
        almacen.guardarTexto("respaldoUri", uri.toString())
        almacen.guardarTexto("respaldoNombre", nombre)
        actualizar { it.copy(respaldoNombre = nombre) }
        respaldarAhora()
    }

    fun respaldarAhora(silencioso: Boolean = false) {
        val uri = almacen.texto("respaldoUri").takeIf { it.isNotBlank() }?.let(Uri::parse) ?: return elegirDondeRespaldar()
        if (!silencioso) avisar("Guardando el respaldo…")
        scope.launch {
            val ok = withContext(Dispatchers.IO) { Respaldo.escribirEn(app, uri) }
            if (ok) {
                val ahora = System.currentTimeMillis()
                almacen.guardarTexto("respaldoUltimo", ahora.toString())
                actualizar { it.copy(respaldoUltimo = ahora) }
                if (!silencioso) avisar("Respaldo guardado en ${estado.value.respaldoNombre}")
            } else if (!silencioso) avisar("No pude guardar el respaldo. Elige otra vez dónde guardarlo.")
        }
    }

    /** Una vez al día, al abrir Díptico, el respaldo se actualiza solo (si hay uno elegido). */
    private fun respaldoDiario() {
        if (almacen.texto("respaldoUri").isBlank()) return
        if (System.currentTimeMillis() - estado.value.respaldoUltimo < 20 * 3_600_000L) return
        scope.launch { delay(15_000); respaldarAhora(silencioso = true) }
    }

    /** Volvió el selector con un respaldo para traer: se confirma, se recupera y Díptico se reinicia. */
    fun respaldoElegido(uri: Uri) {
        mostrarDialogo(
            Dialogo(
                "traer-respaldo", "¿Traer este respaldo?",
                "Tus ajustes, colecciones, tiempos y notas actuales se reemplazan por los del respaldo. Después Díptico se reinicia. Si cambiaste de consola, vuelve a elegir tu carpeta de juegos.",
                Icono.PEGAR, si = "Traer", no = "Cancelar",
            ),
        ) {
            scope.launch {
                val n = withContext(Dispatchers.IO) {
                    try { app.contentResolver.openInputStream(uri)?.use { Respaldo.restaurar(app, it) } ?: -1 } catch (_: Exception) { -1 }
                }
                if (n < 0) { avisar("No pude validar o restaurar el respaldo de Díptico"); return@launch }
                avisar("Respaldo recuperado: Díptico se reinicia")
                delay(1500)
                reiniciarDiptico()
            }
        }
    }

    /** Cierra Díptico y lo vuelve a abrir (para cargar un respaldo). */
    private fun reiniciarDiptico() {
        try {
            val i = app.packageManager.getLaunchIntentForPackage(app.packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            if (i != null) {
                val pi = android.app.PendingIntent.getActivity(app, 7771, i, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_CANCEL_CURRENT)
                (app.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager).set(android.app.AlarmManager.ELAPSED_REALTIME, android.os.SystemClock.elapsedRealtime() + 700, pi)
            }
        } catch (_: Exception) {}
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private val ARRANQUES = listOf("igual", "caratulas", "widgets", "consolas", "ultimo")
    fun nombreArranque(v: String) = when (v) {
        "caratulas" -> "Carátulas"; "widgets" -> "Widgets"; "consolas" -> "Tus consolas"; "ultimo" -> "Tu último juego"; else -> "Como lo dejaste"
    }

    fun mostrarBienvenida(ausencia: Long) {
        val e = estado.value
        // La bienvenida no hace fila: si hay otra cosa encima, simplemente no sale (ya no sería «al encender»).
        if (e.bienvenidaModo == "no" || !e.configurado || e.sesion != null || e.novedades >= 0 || e.quedaste || e.donde != null) return
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
        // Sin otra pantalla libre (una pantalla, o un juego que usa las dos): la tarjeta grande va encima del juego.
        if (e.unaPantalla || s.dosPantallas) {
            cl.nikalbazu.diptico.bisa.Cartel.continuacion(
                app, s.displayId, j.titulo, if (minutos > 0) "$minutos min en esta partida" else "Recién empezaste esta partida", paso, j.portada,
            )
        } else cl.nikalbazu.diptico.bisa.Cartel.mostrar(
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
        if (estado.value.escaneando) return
        actualizar { it.copy(escaneando = true) }
        trabajo("escaneo", "Buscando tus juegos")
        scope.launch {
            try {
                while (true) {
                    val carpetas = almacen.carpetas()
                    val media = almacen.media
                    val anterior = crudos
                    val (lista, fallas) = withContext(Dispatchers.IO) {
                        val anteriores = anterior.groupBy(::raizDe)
                        var fallas = 0
                        val juegos = carpetas.flatMap { carpeta ->
                            try {
                                Biblioteca.escanear(app, carpeta.uri, media, carpeta.consola)
                            } catch (cancelado: CancellationException) {
                                throw cancelado
                            } catch (_: Exception) {
                                fallas++
                                anteriores[carpeta.uri].orEmpty()
                            }
                        }.distinctBy { it.clave }.sortedWith(compareBy({ Consolas.orden(it.consolaId) }, { it.titulo.lowercase() }))
                        juegos to fallas
                    }
                    val accesos = leerAccesosAndroid(lista)
                    // Si se cambió una carpeta durante el escaneo, vuelve a leer la configuración antes de mostrar resultados.
                    if (carpetas != almacen.carpetas() || media != almacen.media) continue
                    withContext(Dispatchers.IO) { almacen.guardarBiblioteca(lista) }
                    if (carpetas != almacen.carpetas() || media != almacen.media) continue
                    crudos = lista
                    accesosAndroid = accesos
                    publicarJuegos()
                    if (fallas > 0) avisar(
                        if (fallas == carpetas.size) "No pude leer tus carpetas de juegos. Elígelas otra vez."
                        else "No pude leer $fallas de tus carpetas",
                    )
                    if (fallas < carpetas.size) {
                        actualizar { it.copy(indice = if (it.configurado) it.indice else it.indice.coerceAtMost(1)) }
                        bajarIconosFaltantes()
                        identificarNombres()
                    }
                    break
                }
            } finally {
                actualizar { it.copy(escaneando = false) }
                terminarTrabajo("escaneo")
            }
        }
    }

    /** Los ports elegidos, como juegos de la consola "Ports". */
    private fun juegosApps(lista: List<String>, consola: String): List<Juego> = lista.distinct().mapNotNull { pkg ->
        if (!Lanzador.appDisponible(app, pkg)) return@mapNotNull null
        val nombre = Lanzador.nombreApp(app, pkg) ?: return@mapNotNull null
        val icono = Uri.parse("app://$pkg")
        Juego("$consola/$pkg", consola, nombre, pkg, icono, pkg, portada = icono)
    }.sortedBy { it.titulo.lowercase() }

    internal fun todosCrudos(): List<Juego> {
        val paquetes = mutableSetOf<String>()
        val archivos = crudos.distinctBy { it.uri }.filter { j ->
            if (!esAccesoAndroid(j)) true
            else accesosAndroid[j.uri]?.let { Lanzador.appDisponible(app, it) && paquetes.add(it) } == true
        }
        val e = estado.value
        val ports = juegosApps(e.ports.filterNot { it in paquetes }, Consolas.PORTS)
        paquetes.addAll(ports.map { it.archivo })
        val android = juegosApps(e.android.filterNot { it in paquetes }, Consolas.ANDROID)
        return (archivos + ports + android + juegosDePC()).distinctBy { it.clave }
    }

    private fun vigilarAppsInstaladas() {
        val filtro = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        androidx.core.content.ContextCompat.registerReceiver(app, object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_PACKAGE_REMOVED && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
                publicarJuegos()
                if (estado.value.panel == Panel.APPS) cargarApps()
            }
        }, filtro, androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
    }

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
        // La distribución que mejor le queda a este equipo (consola de dos pantallas, portátil, teléfono, tablet o tele).
        optimizarParaEquipo(silencioso = true)
        almacen.configurado = true
        // Primera vez: recorrido por las opciones y funciones. Al cerrarlo se ofrece Díptico como inicio de Android.
        almacen.guardarTexto("novedadesVistas", Novedades.ID)
        actualizar { it.copy(configurado = true, panel = Panel.NINGUNO, indice = 0, novedades = 0, recorrido = true) }
    }

    /** Para que Díptico sea siempre lo primero: se ofrece como app de inicio de Android. */
    private fun ofrecerInicio() {
        scope.launch {
            delay(900)
            if (!Inicio.esPredeterminado(app)) { avisar("Elige Díptico como tu inicio para que siempre vuelvas aquí"); hacerInicio() }
        }
    }

    /** Vuelve a mostrar el recorrido de la primera vez. */
    fun verRecorrido() {
        cerrarPanel()
        actualizar { it.copy(novedades = 0, recorrido = true) }
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
        PasoTuto.APARIENCIA -> if (e.tema == "diptico_vidrio") 2 else if (e.oscuro) 0 else 1
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
                Opcion("Idioma", (Idioma.IDIOMAS.firstOrNull { it.first == e.idioma }?.second ?: e.idioma), Icono.IDIOMA) {
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
                Opcion("Díptico de noche", "Estante y tonos oscuros", Icono.TEMA) { elegirAparienciaInicial(0); siguientePaso() },
                Opcion("Díptico de día", "Estante y tonos claros", Icono.TEMA) { elegirAparienciaInicial(1); siguientePaso() },
                Opcion("Díptico Cristal", "Vidrio y un inicio con widgets", Icono.PALETA) { elegirAparienciaInicial(2); siguientePaso() },
            )
            PasoTuto.CONTROLES -> listOf(Opcion("Continuar", "Cuando quieras", Icono.CHECK) { siguientePaso() })
            PasoTuto.LISTO -> listOf(
                Opcion("¡Empezar a jugar!", icono = Icono.JUGAR) { terminarConfiguracion() },
                Opcion("¿Cómo te llama Bisa?", e.nombre.ifBlank { "Opcional" }, Icono.BISA) { abrirTexto("nombre", e.nombre) },
            )
        }
    }

    /** En algunos pasos el cambio se ve (y se oye) en vivo mientras eliges. */
    private fun elegirAparienciaInicial(i: Int) {
        fijarTema(i != 1)
        elegirTema(if (i == 2) "diptico_vidrio" else "diptico")
        val inicio = if (i == 2) "widgets" else "heroe"
        almacen.guardarTexto("inicio", inicio)
        actualizar { it.copy(inicio = inicio) }
    }

    private fun vistaPrevia(paso: PasoTuto, i: Int) {
        when (paso) {
            PasoTuto.APARIENCIA -> {
                fijarTema(i != 1)
                elegirTema(if (i == 2) "diptico_vidrio" else "diptico")
            }
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
        if (i == e.indiceSel && delta != 0) Sonidos.borde()
        actualizar { it.copy(juegoSel = v[i].clave) }
    }

    /** Vuelve al primer juego de la lista (manteniendo B). */
    fun alPrincipio() {
        val v = estado.value.visibles
        if (v.isEmpty()) return
        actualizar { it.copy(juegoSel = v.first().clave) }
        Sonidos.deslizar()
    }

    fun elegirJuego(clave: String) {
        actualizar { it.copy(juegoSel = clave) }
    }

    fun cambiarConsola(delta: Int) {
        val e = estado.value
        val tabs = e.pestanas
        if (tabs.isEmpty()) return
        val i = tabs.indexOf(e.consolaSel).coerceAtLeast(0)
        elegirConsola(tabs[(i + delta).mod(tabs.size)])
    }

    fun moverCarpetas(dx: Int, dy: Int, carriles: Int) {
        val e = estado.value
        val horizontal = e.vistaConsolas == "carpetas" && e.carpetasHorizontal
        val salto = if (horizontal) dx else dy
        if (!horizontal && salto == 0) {
            cambiarConsola(dx)
            return
        }
        val carpetas = e.pestanas
        val i = carpetas.indexOf(e.consolaSel).coerceAtLeast(0)
        val carril = i % carriles
        val inicioLinea = (i / carriles + salto) * carriles
        if (inicioLinea !in carpetas.indices) return
        val destino = if (salto == 0) {
            val fila = carril + dy
            if (fila !in 0 until carriles || inicioLinea + fila !in carpetas.indices) return
            inicioLinea + fila
        } else (inicioLinea + carril).coerceAtMost(carpetas.lastIndex)
        elegirConsola(carpetas[destino])
    }

    fun elegirConsola(id: String) {
        actualizar { e ->
            val vis = if (id == "todo") e.juegos else e.juegosPorConsola[id].orEmpty()
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

    private fun archivoParaJugar(juego: Juego, e: Estado): Juego {
        val archivo = e.datos[juego.clave]?.archivoVariante ?: return juego
        val variante = juego.variantes.singleOrNull { it.archivo == archivo } ?: return juego
        return juego.copy(archivo = variante.archivo, uri = variante.uri, docId = variante.docId, esCarpeta = variante.esCarpeta)
    }

    private var revisarLanzamiento: Job? = null

    fun jugarJuegoMostrado(clave: String?) {
        if (clave == null || estado.value.visibles.none { it.clave == clave }) return
        elegirJuego(clave)
        jugar()
    }

    fun jugar(pantalla: String? = null, conLupaLista: Boolean = false, conRetomaLista: Boolean = false) {
        val e = estado.value
        val juego = e.seleccionado ?: return
        if (revisarLanzamiento?.isActive == true) return
        if (juego.uri.scheme != "content") {
            jugarComprobado(pantalla, conLupaLista, conRetomaLista)
            return
        }
        revisarLanzamiento = scope.launch {
            val problema = withContext(Dispatchers.IO) { Lanzador.revisarArchivo(app, archivoParaJugar(juego, e)) }
            val actual = estado.value
            if (actual.seleccionado?.clave != juego.clave ||
                actual.datos[juego.clave]?.archivoVariante != e.datos[juego.clave]?.archivoVariante) return@launch
            if (problema == null) jugarComprobado(pantalla, conLupaLista, conRetomaLista)
            else {
                if (conLupaLista) cl.nikalbazu.diptico.lupa.LupaServicio.detener(app)
                if (conRetomaLista) cl.nikalbazu.diptico.retoma.RetomaServicio.detener(app)
                mostrarDialogo(Dialogo("archivo-no-accesible", "No se pudo abrir ${juego.titulo}", problema,
                    Icono.CARPETA, si = "Revisar carpetas", no = "Cerrar")) { abrirCarpetas() }
            }
        }
    }

    private fun jugarComprobado(pantalla: String?, conLupaLista: Boolean, conRetomaLista: Boolean) {
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
        if (!conRetomaLista && e.clipRetoma && !e.unaPantalla && e.datos[juego.clave]?.lupa != true && destino == "arriba" && !consola.dosPantallas) {
            pedirPermisoRetoma(true, pantalla); return
        }
        val displayId = if (destino == "abajo" && inferior != null) inferior.displayId else Display.DEFAULT_DISPLAY
        val error = if (juego.uri.scheme == "app") Lanzador.lanzarApp(app, juego.archivo, juego.titulo, displayId)
        else {
            val archivo = archivoParaJugar(juego, e)
            Lanzador.lanzar(app, archivo, consola, e.datos[juego.clave]?.emulador ?: e.emuladores[consola.id], displayId, raizDe(archivo))
        }
        if (error != null) {
            if (conLupaLista) cl.nikalbazu.diptico.lupa.LupaServicio.detener(app)
            if (conRetomaLista) cl.nikalbazu.diptico.retoma.RetomaServicio.detener(app)
            if (juego.uri.scheme == "app") avisar(error)
            else mostrarDialogo(Dialogo("lanzamiento-fallido", "No se pudo abrir ${juego.titulo}", error,
                Icono.EMULADOR, si = "Emuladores", no = "Cerrar")) { abrirMenu(Seccion.EMULADORES) }
            return
        }
        if (e.turbo) turboAlJugar(juego, displayId)
        actualizar {
            it.copy(navegacion = it.navegacion.copy(retomaVideo = "", retomaVideoClave = ""), sesion = Sesion(juego.clave, System.currentTimeMillis(), displayId, consola.dosPantallas), panel = Panel.NINGUNO, despierta = false, burbujaAbierta = false, burbujaAlerta = false)
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
            // Mientras juegas, el permiso se pide en la pantalla de abajo: así no tapa ni pausa el juego de arriba.
            cl.nikalbazu.diptico.retoma.RetomaServicio.detener(app)
            val i = Intent(app, cl.nikalbazu.diptico.lupa.PermisoLupaActivity::class.java)
                .putExtra("luegoJugar", luegoJugar).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val abajo = Pantallas.inferior(app)
            if (!luegoJugar && abajo != null) app.startActivity(i, android.app.ActivityOptions.makeBasic().setLaunchDisplayId(abajo.displayId).toBundle())
            else app.startActivity(i)
        } catch (_: Exception) {
            lupaRechazada(luegoJugar)
        }
    }

    /** Android dio permiso: si estaba por abrir un juego, ahora sí. */
    fun lupaPermitida(luegoJugar: Boolean) {
        if (luegoJugar) {
            val destino = pantallaPendiente
            pantallaPendiente = null
            scope.launch { delay(350); jugar(destino, conLupaLista = true) }
        }
    }

    /** Si se rechaza la captura, apaga la lupa de este juego y lo abre de todos modos. */
    fun lupaRechazada(luegoJugar: Boolean) {
        val clave = if (luegoJugar) estado.value.seleccionado?.clave else estado.value.sesion?.clave
        if (clave != null) cambiarDatos(clave) { it.copy(lupa = false) }
        avisar("Lupa apagada para este juego")
        if (luegoJugar) {
            val destino = pantallaPendiente
            pantallaPendiente = null
            scope.launch { delay(350); jugar(destino, conLupaLista = true) }
        }
    }

    fun lupaCambio(activa: Boolean) = actualizar { it.copy(lupaActiva = activa) }

    fun zonaLupa(clave: String): android.graphics.RectF {
        val s = estado.value.datos[clave]?.zonaLupa?.split(",")?.mapNotNull { it.toFloatOrNull() }
        return if (s != null && s.size == 4) android.graphics.RectF(s[0], s[1], s[0] + s[2], s[1] + s[3])
        else android.graphics.RectF(0.79f, 0.02f, 0.99f, 0.36f)
    }

    /** Guarda la zona de la lupa sin rearmar la biblioteca (antes cada toque hacía ese trabajo pesado y la lupa se trababa). */
    fun guardarZonaLupa(clave: String, r: android.graphics.RectF) {
        val datos = estado.value.datos.toMutableMap()
        datos[clave] = (datos[clave] ?: DatosJuego()).copy(zonaLupa = "${r.left},${r.top},${r.width()},${r.height()}")
        actualizar { it.copy(datos = datos) }
        zonaJob?.cancel()
        zonaJob = scope.launch { delay(600); almacen.guardarDatos(estado.value.datos) }
    }

    private var zonaJob: Job? = null

    /** Enciende o apaga la lupa para un juego (si se está jugando, se pide el permiso al tiro). */
    fun alternarLupaJuego(clave: String, encender: Boolean = estado.value.datos[clave]?.lupa != true) {
        cambiarDatos(clave) { it.copy(lupa = encender) }
        if (encender) {
            if (estado.value.sesion != null && !cl.nikalbazu.diptico.lupa.LupaServicio.activa) pedirPermisoLupa(false)
            avisar("Lupa encendida: al jugar, la pantalla de abajo amplía una parte de la de arriba")
        } else {
            cl.nikalbazu.diptico.retoma.RetomaServicio.detener(app)
        cl.nikalbazu.diptico.lupa.LupaServicio.detener(app)
            avisar("Lupa apagada")
        }
    }

    // ---------- Galerías: temas, música y colores ----------

    /** Un elemento de una galería. [color] es la muestra (en colores) y [tema] la vista previa (en temas). */
    data class ItemGaleria(val id: String, val nombre: String, val detalle: String, val enUso: Boolean, val tema: Tema? = null, val color: Long = 0L, val accion: () -> Unit)

    /** De dónde se abrió una galería o un cuadro de texto: al volver (B) se regresa justo ahí, no a otra sección. */
    private data class Regreso(val panel: Panel, val seccion: Seccion, val enOpciones: Boolean, val indice: Int, val fichaPestana: Int)
    private var galeriaDesde: Regreso? = null
    private var claveDesde: Regreso? = null

    private fun regresoActual(): Regreso? {
        val e = estado.value
        return if (e.panel == Panel.NINGUNO || e.panel == Panel.GALERIA || e.panel == Panel.CLAVE) null
        else Regreso(e.panel, e.seccion, e.enOpciones, e.indice, e.pestanaFicha)
    }

    /** Vuelve a [r]: true si había a dónde volver. */
    private fun volverA(r: Regreso?): Boolean {
        if (r == null) return false
        actualizar { it.copy(panel = r.panel, seccion = r.seccion, enOpciones = r.enOpciones, indice = r.indice, pestanaFicha = r.fichaPestana) }
        return true
    }

    private val categoriasTienda = listOf("Todos", "Videojuegos", "Paisajes", "Fantasía", "Retro", "Calma")
    private var catalogoOficial: List<TemaComunidad>? = null

    private fun cargarCatalogoOficial(): List<TemaComunidad> =
        catalogoOficial ?: TiendaTemas.incluidos(app).also { catalogoOficial = it }

    fun abrirDecoraciones() {
        if (::app.isInitialized) runCatching { cargarCatalogoOficial() }
        abrirGaleria("decoraciones")
    }

    fun abrirTiendaTemas() {
        runCatching { cargarCatalogoOficial() }
            .onSuccess { temas ->
                val oficiales = temas.associateBy { it.tema.id }
                val previos = estado.value.temasPropios
                val migrados = previos.map { tema ->
                    oficiales[tema.id]?.tema?.copy(musica = tema.musica) ?: tema
                }
                if (migrados != previos) {
                    guardarTemasPropios(migrados)
                    aplicarTema()
                }
                actualizar { it.copy(catalogoTemas = temas) }
                abrirGaleria("tienda_temas")
            }
            .onFailure { avisar("No pude abrir la tienda de temas") }
    }

    fun abrirAportesTemas() = abrirWeb(TiendaTemas.WEB_APORTES)

    private fun temasEnTienda(e: Estado): List<TemaComunidad> {
        if (e.galeriaPestana == 0) return e.catalogoTemas
        val categoria = categoriasTienda.getOrNull(e.galeriaPestana)
        return e.catalogoTemas.filter { it.categoria == categoria }
    }

    fun temaEnTienda(e: Estado): TemaComunidad? = temasEnTienda(e).getOrNull(e.indice)

    fun abrirGaleria(tipo: String, pestana: Int = 0) {
        if (estado.value.panel != Panel.GALERIA) galeriaDesde = regresoActual()
        actualizar { it.copy(panel = Panel.GALERIA, galeria = tipo, galeriaPestana = pestana, indice = 0, decoracionAbierta = null) }
        val i = itemsGaleria().indexOfFirst { it.enUso }
        if (i > 0) actualizar { it.copy(indice = i) }
    }

    fun pestanasGaleria(tipo: String = estado.value.galeria): List<String> = if (tipo == "colecciones") listOf("Colecciones") else when (tipo) {
        "variantes_juego" -> listOf("Archivos del juego")
        "capturas_juego", "captura_acciones" -> listOf("Capturas guardadas")
        "copias_srm" -> listOf("Copias de RetroArch")
        "filtros" -> listOf("Filtrar juegos")
        "orden" -> listOf("Ordenar juegos")
        "vistas_consolas" -> listOf("Tus consolas")
        "vistas_juegos" -> listOf("Tus juegos")
        "temas" -> listOf("Incluidos", "Mis temas")
        "importar_temas" -> listOf("Importar temas")
        "tienda_temas" -> categoriasTienda
        "decoraciones" -> listOf("Instalados", "Tienda de temas", "Calendario")
        "mezclar_tema" -> ComponenteTema.entries.map { it.titulo }
        "colores" -> listOf("Principal", "Secundario", "Detalles", "Fondo", "Letras")
        "ia" -> listOf("El cerebro de Bisa")
        "cajas" -> listOf("${Consolas.porId(estado.value.galeriaConsola)?.nombre ?: "Esta consola"}", "Todas las consolas")
        else -> listOf("Canciones")
    }

    fun cambiarPestanaGaleria(d: Int) {
        val n = pestanasGaleria().size
        actualizar { it.copy(galeriaPestana = (it.galeriaPestana + d).mod(n), indice = 0, decoracionAbierta = null) }
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
        if (e.galeria == "copias_srm") return opcionesCopiasPartida
        if (e.galeria == "captura_acciones") {
            val (clave, ruta) = capturaPara ?: return emptyList()
            return listOf(
                ItemGaleria("mapa", "Usar como mapa", "Marca el punto donde quedaste", false) {
                    guardarMapas(estado.value.mapas + (clave to MapaJuego(ruta, .5f, .5f)))
                    cerrarPanel()
                    abrirMapa(clave)
                },
                ItemGaleria("compartir", "Compartir", "Abre el selector de Android", false) { compartirCaptura(ruta) },
                ItemGaleria("quitar", "Quitar de la galería", "El archivo original se conserva", false) {
                    cambiarDatos(clave) { it.copy(capturas = it.capturas - ruta) }
                    abrirGaleria("capturas_juego")
                },
                ItemGaleria("volver", "Volver a capturas", "Mira las demás imágenes", false) { abrirGaleria("capturas_juego") },
            )
        }
        if (e.galeria == "capturas_juego") {
            val j = e.seleccionado ?: return emptyList()
            val capturas = e.datos[j.clave]?.capturas.orEmpty()
            return listOf(ItemGaleria("agregar", "Agregar captura", "Elige una imagen guardada en Android", false) {
                pedirArchivoPara("captura:${j.clave}", "image/*")
            }) + capturas.mapIndexed { i, ruta ->
                ItemGaleria("captura-$i", "Captura ${i + 1}", "Compartir o usar como mapa", false) {
                    accionesCaptura(j.clave, ruta)
                }
            }
        }
        if (e.galeria == "variantes_juego") {
            val j = e.seleccionado ?: return emptyList()
            val actual = e.datos[j.clave]?.archivoVariante
            val opciones = listOf(null to j.archivo) + j.variantes.groupBy { it.archivo }.values
                .mapNotNull { it.singleOrNull() }.map { it.archivo to it.archivo }
            return opciones.mapIndexed { i, (archivo, nombre) ->
                ItemGaleria("archivo-$i", nombre, if (archivo == null) "Archivo preferido" else if (j.tipo == "m3u") "Disco o versión alternativa" else "Versión alternativa", actual == archivo) {
                    cambiarDatos(j.clave) { it.copy(archivoVariante = archivo) }
                    cerrarPanel()
                }
            }
        }
        if (e.galeria == "filtros") return FILTROS.map { f -> ItemGaleria(f, nombreFiltro(f), "Mostrar esta selección", e.filtro == f) { elegirFiltro(f); cerrarPanel() } }
        if (e.galeria == "orden") return ORDENES.map { o -> ItemGaleria(o, nombreOrden(o), "Orden de tu biblioteca", e.orden == o) { elegirOrden(o); cerrarPanel() } }
        if (e.galeria == "vistas_juegos") return listOf("cuadricula", "estante", "lista", "carrusel", "abanico", "lomos").map { v ->
            val detalle = when(v) { "cuadricula" -> "Todas las portadas a la vista"; "lista" -> "Nombres claros y navegación rápida"; "estante" -> "Cajas con información del juego"; "carrusel" -> "Una portada protagonista"; "abanico" -> "Portadas como cartas"; else -> "Lomos de las cajas" }
            ItemGaleria(v, nombreVista(v), detalle, e.vista == v) { almacen.guardarTexto("vista", v); actualizar { it.copy(vista = v) }; cerrarPanel() }
        }
        if (e.galeria == "vistas_consolas") return VISTAS_CONSOLAS.map { v ->
            val detalle = when (v) {
                "mosaico" -> "Más consolas a la vista, con una portada y el total de juegos"
                "vitrina" -> "Carpetas anchas con tres portadas"
                "carpetas" -> "Íconos de consola que abren una ventana con sus juegos"
                else -> "Vista previa en la otra pantalla"
            }
            ItemGaleria(v, nombreVistaConsolas(v), detalle, e.vistaConsolas == v) { elegirVistaConsolas(v); cerrarPanel() }
        }
        if (e.galeria == "colecciones") {
            val j = e.seleccionado
            return e.colecciones.map { (id, c) ->
                val dentro = j != null && j.clave in c.juegos
                ItemGaleria(id, c.nombre, (if (c.juegos.size == 1) "1 juego" else "${c.juegos.size} juegos") + if (dentro) " · este juego está aquí" else "", dentro) { j?.let { alternarEnColeccion(id, it.clave) } }
            } + ItemGaleria("_nueva", "Nueva colección", if (j != null) "Con ${j.titulo}" else "Vacía", false) { abrirTexto("coleccion_nueva") }
        }
        return when (e.galeria) {
            "temas" -> when (e.galeriaPestana) {
                0 -> Temas.iniciales.map { t -> ItemGaleria(t.id, t.nombre, t.descripcion, actual.id == t.id, t) { elegirTema(t.id) } }
                else -> temasPersonales(e).map { t ->
                    ItemGaleria(t.id, t.nombre, t.descripcion.ifBlank { "Tema guardado en tu consola" }, actual.id == t.id, t) {
                        elegirTema(t.id)
                    }
                } + ItemGaleria("_colores", "Mis colores", "Personaliza la interfaz", false) { abrirGaleria("colores") }
            }
            "mezclar_tema" -> {
                val componente = ComponenteTema.entries.getOrNull(e.galeriaPestana) ?: ComponenteTema.COLORES
                val mio = temaParaMezclar(e)
                todosLosTemas(e).filterNot { it.id == "mio" }.map { fuente ->
                    val previa = componente.combinar(mio, conAdorno(e, fuente))
                    ItemGaleria(fuente.id, fuente.nombre, fuente.descripcion, previa == mio, previa) {
                        if (componente == ComponenteTema.DECORACION) guardarDecoracion("mio", null, null)
                        editarMiTema { componente.combinar(it, conAdorno(estado.value, fuente)) }
                    }
                }
            }
            "tienda_temas" -> temasEnTienda(e).map { entrada ->
                val t = entrada.tema
                val instalado = e.temasPropios.any { it.id == t.id }
                ItemGaleria(t.id, t.nombre, t.descripcion, instalado, t) {
                    val guardado = estado.value.temasPropios.firstOrNull { it.id == t.id }
                    if (guardado != null && TiendaTemas.pista(app, guardado) != null) elegirTema(t.id)
                    else confirmarTemaImportado(entrada)
                }
            }
            "decoraciones" -> itemsDecoraciones(e)
            "importar_temas" -> e.catalogoTemas.map { entrada ->
                val t = entrada.tema
                ItemGaleria(t.id, t.nombre, t.descripcion.ifBlank { entrada.autor }, false, t) { confirmarTemaImportado(entrada) }
            }
            "colores" -> {
                val mio = e.temasPropios.firstOrNull { it.id == "mio" } ?: actual
                val valor = when (e.galeriaPestana) { 0 -> mio.acento; 1 -> mio.acento2; 2 -> mio.acento3; 3 -> mio.fondo; else -> mio.tinta }
                val auto = if (e.galeriaPestana >= 3) listOf(ItemGaleria("_auto", "Automático", "", valor == 0L, color = 0L) { ponerColor(0L) }) else emptyList()
                auto + MUESTRAS_COLOR.map { c -> ItemGaleria(c.toString(), "", "", c == valor, color = c) { ponerColor(c) } }
            }
            "cajas" -> {
                val actualF = e.formas[e.galeriaConsola]?.forma ?: "original"
                FORMAS.map { f ->
                    ItemGaleria(f, nombreForma(f), "", e.galeriaPestana == 0 && actualF == f) {
                        if (estado.value.galeriaPestana == 0) cambiarForma(estado.value.galeriaConsola) { it.copy(forma = f) }
                        else {
                            val m = estado.value.formas.toMutableMap()
                            for (c in estado.value.consolasConJuegos) m[c.id] = (m[c.id] ?: FormaCfg()).copy(forma = f)
                            guardarFormas(m)
                        }
                        avisar("Cajas: ${nombreForma(f)}")
                    }
                }
            }
            "ia" -> {
                fun estadoNube(p: String) = if (claveDe(p, e).isBlank()) "Falta tu clave" else "Clave guardada ✓"
                listOf(
                    ItemGaleria("auto", "Automático", "Usa la de tu consola si está; si no, la primera con clave", e.motorIA == "auto") { elegirIA("auto") },
                    ItemGaleria("local", "En tu consola", if (e.iaLocalLista) "Lista ✓ · gratis y sin internet" else "Bajar 1,6 GB · gratis y sin internet", e.motorIA == "local") { elegirIA("local") },
                    ItemGaleria("gemini", "Gemini", "Google · gratis con límite diario · " + estadoNube("gemini"), e.motorIA == "gemini") { elegirIA("gemini") },
                    ItemGaleria("openai", "ChatGPT", "OpenAI · pagado por uso · " + estadoNube("openai"), e.motorIA == "openai") { elegirIA("openai") },
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
        val c = CentroBisa.contexto(estado.value)
        val saludo = MensajeChat("ia", if (c.juego == null) "Estoy contigo. ¿En qué te ayudo?"
            else "Estás en ${c.titulo}. Puedes consultar tu próximo paso, pedir una pista o revisar las opciones de este sistema.")
        actualizar {
            val chat = if (it.pensando || it.chat.lastOrNull()?.texto == saludo.texto) it.chat else it.chat.takeLast(38) + saludo
            it.copy(panel = Panel.ASISTENTE, chat = chat, indice = 0, despierta = true)
        }
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
        cl.nikalbazu.diptico.retoma.RetomaServicio.detener(app)
        cl.nikalbazu.diptico.lupa.LupaServicio.detener(app)
        val datos = estado.value.datos.toMutableMap()
        val d = datos[s.clave] ?: DatosJuego()
        datos[s.clave] = d.copy(
            tiempo = d.tiempo + if (duracion >= 15) duracion else 0,
            ultimo = System.currentTimeMillis(),
            sesiones = d.sesiones + if (duracion >= 15) 1 else 0,
        )
        almacen.guardarDatos(datos)
        // ¿Dónde quedaste?: una tarjeta con formas rápidas de anotarlo (escribir, nota de voz, mapa) o pasar de largo.
        val hacerPregunta = preguntar && duracion >= 60 && estado.value.preguntarQuedaste
        actualizar { it.copy(sesion = null, despierta = false, datos = datos, juegoSel = s.clave) }
        // Sale cuando no haya otra cosa encima (una cosa a la vez).
        if (hacerPregunta) cuandoHayaLugar("quedaste") {
            if (estado.value.sesion == null) actualizar { it.copy(juegoSel = s.clave, quedaste = true, quedastePregunta = true, quedasteSel = 0) }
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
        if (estado.value.panel != Panel.MENU) Sonidos.abrir()
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

    /** Abre el cuadro de texto para [para] ("sgdb", "ia_clave_gemini", "titulo", "notas"). */
    fun abrirTexto(para: String, inicial: String = "") {
        if (estado.value.panel != Panel.CLAVE) claveDesde = regresoActual()
        actualizar { it.copy(panel = Panel.CLAVE, textoPara = para, borrador = inicial, teclaFila = 2, teclaCol = 0, simbolos = false, cursor = -1, ancla = null, mayus = inicial.isEmpty() && para != "sgdb" && !para.startsWith("ia_clave") && !para.startsWith("ra_") && !para.startsWith("ras_") && para != "tema_enlace") }
        // Para las claves: el teléfono puede mandarla directo (QR con la página de la consola en tu Wi-Fi).
        val oficial = urlClave(para)
        if (oficial != null && para != "ra_usuario") {
            val titulo = when (para) {
                "ia_clave_gemini" -> "Clave de Gemini"; "ia_clave_openai" -> "Clave de ChatGPT"
                "sgdb" -> "Clave de SteamGridDB"
                else -> tituloRaspador(para) ?: "Clave de RetroAchievements"
            }
            val url = Enlace.iniciar(titulo, oficial) { clave -> recibirClaveRemota(para, clave) }
            actualizar { it.copy(enlaceUrl = url) }
        } else if (estado.value.enlaceUrl != null) {
            Enlace.detener(); actualizar { it.copy(enlaceUrl = null) }
        }
    }

    /** Llegó una clave desde el teléfono: se guarda como si se hubiera escrito aquí. */
    private fun recibirClaveRemota(para: String, clave: String) {
        val e = estado.value
        if (e.panel != Panel.CLAVE || e.textoPara != para) { Enlace.detener(); return }
        actualizar { it.copy(borrador = clave, enlaceUrl = null) }
        Sonidos.aviso()
        guardarClave()
        avisar("¡Clave recibida desde tu teléfono! Ya quedó activa")
    }

    fun cerrarPanel() {
        if (estado.value.appsMenu != null || estado.value.appsMoviendo != null) { cerrarOpcionesApp(); return }
        if (estado.value.coleccionVisual != null) actualizar { it.copy(coleccionVisual = null) }
        val e = estado.value
        cadenaRaspador = emptyList()
        if (e.panel == Panel.CLAVE && e.enlaceUrl != null) { Enlace.detener(); actualizar { it.copy(enlaceUrl = null) } }
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
        if (e.panel == Panel.CLAVE && (e.textoPara in setOf("asistente", "bisa_paso") || e.textoPara.startsWith("ia_clave") && claveParaAsistente)) {
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
            if (volverA(claveDesde.also { claveDesde = null })) return
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
        if (e.panel == Panel.WIDGET) {
            actualizar { it.copy(panel = Panel.NINGUNO, widgetAbierto = -1, indice = 0) }
            return
        }
        if (e.panel == Panel.ENCUADRE) {
            val i = opcionesFicha().indexOfFirst { it.titulo == if (e.encuadreDe.endsWith("|arte")) "Encuadrar la imagen grande" else "Encuadrar la portada" }
            actualizar { it.copy(panel = Panel.FICHA, indice = i.coerceAtLeast(0), encuadreDe = "") }
            return
        }
        if (e.panel == Panel.IMAGENES) {
            busquedaImagenes?.cancel()
            if (e.candidatoPara.startsWith("c_")) {
                val i = opcionesEditarConsola().indexOfFirst { it.titulo == when (e.candidatoPara) { "c_icono" -> "Ícono"; "c_portada" -> "Imagen del ícono de carpeta"; "c_logo" -> "Logo"; else -> "Imagen grande (hero)" } }
                actualizar { it.copy(panel = Panel.EDITAR_CONSOLA, indice = i.coerceAtLeast(0), candidatoPara = "") }
                return
            }
            actualizar { it.copy(panel = Panel.FICHA, indice = 0) }
            return
        }
        if (e.panel == Panel.ESTUDIO) {
            terminarEdicion()
            return
        }
        if (e.panel == Panel.GALERIA) {
            if (e.galeria == "decoraciones" && e.decoracionAbierta != null) {
                val indiceTema = temasDecoracion(e).indexOfFirst { it.id == e.decoracionAbierta }.coerceAtLeast(0)
                actualizar { it.copy(decoracionAbierta = null, indice = indiceTema) }
                return
            }
            // Se vuelve a donde se abrió (la ficha del juego, la ventana, la consola o la misma sección del menú).
            if (volverA(galeriaDesde.also { galeriaDesde = null })) return
            val sec = when (e.galeria) { "musica" -> Seccion.MUSICA; "ia" -> Seccion.ASISTENTE; else -> Seccion.TEMAS }
            actualizar { it.copy(panel = Panel.MENU, seccion = sec, enOpciones = true, indice = 0) }
            return
        }
        if (e.panel == Panel.BUSCAR && e.filtro == "ocultos") {
            salirDeOcultos(cerrarBusqueda = true)
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
        return listOf(EDICION) + letras + listOf(listOf("⇧", if (e.simbolos) "abc" else "#", "Voz", "␣", "⌫", "✓"))
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
            "Voz" -> dictar("teclado")
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
            estado.value.textoPara == "tema_enlace" -> 600
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
        val antes = estado.value
        moverIndiceSolo(delta)
        val ahora = estado.value
        // Si no se movió nada, llegaste al borde: suena distinto.
        if (delta != 0 && ahora.indice == antes.indice && ahora.seccion == antes.seccion && ahora.panel == antes.panel && ahora.widgetSel == antes.widgetSel) Sonidos.borde()
    }

    private fun moverIndiceSolo(delta: Int) {
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
                saltarSeccion(delta, envolver = false)
            } else {
                val total = opcionesSeccion(e.seccion).size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta + total) % total) }
            }
            Panel.WIDGET -> {
                val total = opcionesWidget(e).size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
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
                val total = if (e.cortina == "avisos") Avisos.lista.value.size else opcionesCentro().size
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
            Panel.WIDGET -> opcionesWidget(e).getOrNull(e.indice)?.takeIf { !it.apagada }?.accion?.invoke()
            Panel.PORTS -> e.apps.getOrNull(e.indice)?.let { alternarPort(it.paquete) }
            Panel.FICHA -> if (e.pestanaFicha >= 2) opcionesFicha().getOrNull(e.indice)?.accion?.invoke()
            Panel.BUSCAR -> e.visibles.getOrNull(e.indice)?.let { j ->
                actualizar { it.copy(juegoSel = j.clave, panel = Panel.NINGUNO, enSistemas = false,
                    inicio = if (it.unaPantalla) "heroe" else it.inicio) }
            }
            Panel.CARPETAS -> aceptarCarpetas()
            Panel.IMAGENES -> usarCandidato()
            Panel.GALERIA -> itemsGaleria(e).getOrNull(e.indice)?.accion?.invoke()
            Panel.ESTUDIO -> aceptarControl()
            Panel.EDITAR_CONSOLA -> opcionesEditarConsola().getOrNull(e.indice)?.accion?.invoke()
            Panel.APPS -> appsVisibles(e).getOrNull(e.indice)?.let { abrirPaquete(it.paquete) }
            Panel.ASISTENTE -> elementosAsistente().getOrNull(e.indice)?.accion?.invoke()
            Panel.CENTRO -> {
                if (e.cortina == "avisos") Avisos.lista.value.getOrNull(e.indice)?.let { abrirAviso(it) }
                else opcionesCentro().getOrNull(e.indice)?.accion?.invoke()
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
            // Una sección avanzada abierta desde «Más ajustes»: B vuelve ahí, con esa sección marcada.
            e.panel == Panel.MENU && e.enOpciones && e.seccion !in seccionesMenu() -> {
                val i = opcionesMas(e).indexOfFirst { it.titulo == e.seccion.titulo }.coerceAtLeast(0)
                actualizar { it.copy(seccion = Seccion.MAS, indice = i) }
            }
            e.panel == Panel.MENU && e.enOpciones -> columnaMenu(false)
            e.panel != Panel.NINGUNO -> cerrarPanel()
            // En el inicio, B saca la barra flotante (y la esconde si ya estaba a la vista).
            e.configurado && e.sesion == null && !e.barraFija -> actualizar { it.copy(barra = !it.barra) }
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
        lista += Opcion("Jugar", icono = Icono.JUGAR, ayuda = "Abre el juego con su emulador, en la pantalla que elegiste. También con A.") { cerrarPanel(); jugar() }
        lista += Opcion("Ficha", icono = Icono.TUTORIAL, ayuda = "Todo del juego: de qué trata, tu tiempo, logros, trucos y notas, y para cambiar sus imágenes. También con X.") { abrirFicha() }
        lista += Opcion("Mi paso", d?.paso?.take(30) ?: "", Icono.LAPIZ, ayuda = "Anota dónde quedaste (texto, voz o un punto en el mapa) para que Díptico te lo recuerde la próxima vez.") { cerrarPanel(); abrirQuedaste() }
        lista += Opcion(if (d?.favorito == true) "Favorito ✓" else "Favorito", icono = Icono.CORAZON, ayuda = "Los favoritos se ven primero con el filtro Favoritos y en el widget de favoritos.") { alternarFavorito(juego.clave) }
        lista += Opcion("Bisa", icono = Icono.BISA, ayuda = "Le pregunta a Bisa por un buen truco o consejo para este juego.") { abrirAsistente(); preguntar("Dame un buen truco o consejo para ${juego.titulo}.") }
        // Lista de abajo: ajustes de este juego.
        lista += Opcion("Personalizar", "Título, imágenes, música", Icono.PALETA, ayuda = "Cambia el título, la portada, la imagen grande, el logo y la música de este juego.") { abrirFicha(); elegirPestanaFicha(3) }
        lista += Opcion("Imágenes", "Tienda de imágenes", Icono.DESCARGA, ayuda = "Abre la tienda de imágenes para elegir otra portada de internet.") { abrirFicha(); buscarCandidatos("portada") }
        lista += Opcion("Mover de lugar", if (e.orden == "propio") "A tu gusto" else "Ordénalos a tu gusto", Icono.VISTA, "Corre este juego con la cruceta y deja tus juegos en el orden que quieras.") { empezarOrdenar() }
        if (consola != null && !consola.sinEmulador) {
            lista += Opcion("Emulador", d?.emulador?.let { id -> Lanzador.opciones(app, consola).firstOrNull { it.id == id }?.nombre } ?: "El de la consola", Icono.EMULADOR, ayuda = "Con qué emulador o núcleo se abre solo este juego. «El de la consola» usa el de Menú › Emuladores.") {
                ciclarEmuladorJuego(juego.clave, consola)
            }
        }
        if (consola != null && consola.comandos.any { it.linea.contains("RETROARCH", ignoreCase = true) }) {
            lista += Opcion("Copias de RetroArch", ".srm", Icono.COPIAR,
                "Guarda o recupera una copia manual del archivo SRAM que selecciones.") { abrirCopiasPartida(juego.clave) }
        }
        lista += Opcion("Capturas del juego", d?.capturas?.size?.toString() ?: "0", Icono.PORTADA,
            "Junta tus capturas guardadas para verlas, compartirlas o marcar un mapa.") { abrirGaleria("capturas_juego") }
        if (juego.variantes.isNotEmpty()) lista += Opcion("Versión del juego", d?.archivoVariante ?: juego.archivo,
            Icono.FORMA, "Elige otra región o formato. El emulador gestiona el guardado de cada archivo.") { abrirGaleria("variantes_juego") }
        lista += Opcion("Estado en mi cola", AvanceJuego.leer(d?.avance).titulo, Icono.BIBLIOTECA,
            "Marca pendiente, jugando o terminado para ordenar la cola.") { cambiarAvance(juego.clave) }
        if (AvanceJuego.leer(d?.avance) in setOf(AvanceJuego.PENDIENTE, AvanceJuego.JUGANDO)) {
            lista += Opcion("Priorizar en la cola", icono = Icono.ARRIBA) { priorizarJuego(juego.clave) }
        }
        if (!e.unaPantalla) lista += Opcion("Pantalla compañera", VistaPartida.leer(d?.vistaPartida ?: e.experiencia.vistaPartida).titulo,
            Icono.PANTALLAS, "Elige portada, bitácora, herramientas o una vista ligera para este juego.") {
            val actual = VistaPartida.leer(estado.value.datos[juego.clave]?.vistaPartida ?: estado.value.experiencia.vistaPartida)
            elegirVistaPartida(juego.clave, VistaPartida.entries[(actual.ordinal + 1) % VistaPartida.entries.size])
        }
        if (!e.unaPantalla) {
            lista += Opcion(
                "Abre en",
                when (d?.pantalla) { "arriba" -> "Arriba"; "abajo" -> "Abajo"; else -> "Según el menú" },
                Icono.PANTALLAS, ayuda = "En qué pantalla se abre este juego. «Según el menú» sigue lo que elegiste en Menú › Pantallas.",
            ) { ciclarPantallaJuego(juego.clave) }
            lista += Opcion(
                "Lupa en este juego", if (d?.lupa == true) "Sí" else "No", Icono.BUSCAR,
                ayuda = "Amplía una parte de la pantalla de arriba en la de abajo. Puedes apagarla aquí si afecta al juego.",
            ) { alternarLupaJuego(juego.clave) }
        }
        lista += Opcion("Acceso directo en el inicio", if (e.widgets.any { it.tipo == "atajo-${juego.clave}" && it.visible }) "Ya está ✓" else "Agregar", Icono.CONTINUAR,
            "Un widget con este juego en tu inicio de widgets: un toque y a jugar.") { crearAtajo(juego.clave) }
        lista += Opcion("Colecciones", e.colecciones.values.count { juego.clave in it.juegos }.let { if (it == 0) "Agregar a una" else "En $it" }, Icono.CARPETA,
            "Junta tus juegos en carpetas propias (\"Para jugar con mi hijo\", \"RPG pendientes\"…), que se ven como widgets en el inicio.") { abrirGaleria("colecciones") }
        lista += Opcion("Imagen en el inicio", nombreHeroe(e.heroes[juego.clave]), Icono.PORTADA,
            "Qué imagen de este juego se ve en grande en el inicio: la imagen grande, la portada, o la que elijan todos (Menú › Inicio).") { ciclarHeroe(juego.clave) }
        if (consola != null) {
            lista += Opcion("Cajas de ${consola.nombre}", nombreForma(e.formas[consola.id]?.forma ?: "original"), Icono.FORMA, ayuda = "La forma de las cajas de esta consola en el estante: cuadrada, póster, horizontal, disco, cartucho o tarjeta.") {
                abrirCajas(consola.id)
            }
            lista += Opcion("Imagen en las cajas de ${consola.nombre}", nombreImagenCaja(e.formas[consola.id]?.imagen ?: "portada"), Icono.PORTADA,
                "Portada, captura del juego o logo, para todas las cajas de esta consola.") { ciclarImagenCaja(consola.id) }
        }
        lista += Opcion(
            if (d?.oculto == true) "Mostrar este juego" else "Ocultar este juego",
            if (d?.oculto == true) "Vuelve al estante" else "Se ve con el filtro Ocultos", Icono.OCULTAR, ayuda = "Lo saca del estante sin borrar nada. Para verlo de nuevo: filtro Ocultos en Menú › Estante.",
        ) { alternarOculto(juego.clave) }
        lista += Opcion(
            "Desinstalar", if (juego.uri.scheme == "file") "Quitar de Díptico" else "Borrar de la consola", Icono.BORRAR,
            "Borra el juego y todo lo suyo (otros discos, carpeta, imágenes y notas de Díptico) sin entrar a los archivos. Tus partidas guardadas se conservan. Pide confirmar dos veces.",
        ) { desinstalarJuego() }
        return lista
    }

    // ---------- Desinstalar un juego ----------

    /** Un archivo o carpeta del juego que se va a borrar. */
    private class PorBorrar(val uri: Uri, val nombre: String, val bytes: Long)

    /** Y › Desinstalar: muestra qué se borra y cuánto espacio libera, y pide mantener A para confirmar. */
    fun desinstalarJuego() {
        val j = estado.value.seleccionado ?: return
        val consola = Consolas.porId(j.consolaId)
        cerrarPanel()
        // Ports y juegos de Android: los desinstala Android, con su propia ventana.
        if (j.portada?.scheme == "app" || consola?.sinEmulador == true) {
            try {
                app.startActivity(android.content.Intent(android.content.Intent.ACTION_DELETE, Uri.parse("package:${j.archivo}")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) { avisar("No pude abrir la desinstalación de Android") }
            return
        }
        // Juegos de PC (Steam, Epic…): son accesos de Díptico; se quitan de la lista, no se borra nada del PC.
        if (j.uri.scheme == "file") {
            mostrarDialogo(Dialogo("quitar-pc", "¿Quitar ${j.titulo}?", "Se quita de tu estante. El juego sigue en GameNative o GameHub.", Icono.BORRAR, si = "Quitar", no = "Cancelar")) {
                quitarJuegoPC(j.clave)
            }
            return
        }
        avisar("Revisando los archivos de ${j.titulo}…")
        scope.launch {
            val lista = withContext(Dispatchers.IO) { try { archivosDelJuego(j) } catch (_: Exception) { emptyList() } }
            if (lista.isEmpty()) { avisar("No encontré los archivos de ${j.titulo}"); return@launch }
            val total = lista.sumOf { it.bytes }
            val espacio = if (total > 0) " y se liberarán ${tamanoBonito(total)}" else ""
            mostrarDialogo(
                Dialogo(
                    "desinstalar-1", "¿Desinstalar ${j.titulo}?",
                    "Se borrará de tu consola$espacio. También sus imágenes, notas y notas de voz de Díptico. Tus partidas guardadas del emulador se conservan.",
                    Icono.BORRAR, lineas = lista.map { it.nombre }, si = "Seguir", no = "Cancelar", peligro = true,
                ),
            ) {
                mostrarDialogo(
                    Dialogo(
                        "desinstalar-2", "Última confirmación",
                        "Mantén A hasta que se llene la barra para borrar ${j.titulo}. Esto no se puede deshacer.",
                        Icono.BORRAR, si = "Borrar", no = "Cancelar", mantener = true, peligro = true,
                    ),
                ) { borrarJuego(j, lista) }
            }
        }
    }

    /** El juego y lo que va con él: los .bin de un .cue, los discos de un .m3u, las pistas de un .gdi, o su carpeta. */
    private fun archivosDelJuego(j: Juego): List<PorBorrar> {
        val cr = app.contentResolver
        val arbolId = DocumentsContract.getTreeDocumentId(j.uri)
        val arbol = DocumentsContract.buildTreeDocumentUri(j.uri.authority, arbolId)
        fun info(docId: String): PorBorrar? {
            val u = DocumentsContract.buildDocumentUriUsingTree(arbol, docId)
            return try {
                cr.query(u, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { c ->
                    if (!c.moveToFirst()) null else {
                        val carpeta = c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                        PorBorrar(u, c.getString(0) + if (carpeta) " (carpeta)" else "", if (carpeta) tamanoCarpeta(arbol, docId) else c.getLong(1))
                    }
                }
            } catch (_: Exception) { null }
        }
        val principal = info(j.docId) ?: return emptyList()
        if (j.esCarpeta) return listOf(principal)
        val padre = j.docId.substringBeforeLast('/')
        val salida = linkedMapOf(j.docId to principal)
        // Archivos que nombra el juego por dentro (.cue, .m3u, .gdi), y los que nombran esos a su vez.
        val porRevisar = ArrayDeque(listOf(j.docId))
        while (porRevisar.isNotEmpty() && salida.size < 40) {
            val id = porRevisar.removeFirst()
            val ext = id.substringAfterLast('.', "").lowercase()
            if (ext !in setOf("cue", "m3u", "gdi")) continue
            val texto = try {
                cr.openInputStream(DocumentsContract.buildDocumentUriUsingTree(arbol, id))?.use { it.readBytes().take(65_536).toByteArray().toString(Charsets.UTF_8) }
            } catch (_: Exception) { null } ?: continue
            val nombres = when (ext) {
                "cue" -> Regex("FILE\\s+\"([^\"]+)\"", RegexOption.IGNORE_CASE).findAll(texto).map { it.groupValues[1] }.toList()
                "gdi" -> texto.lineSequence().drop(1).mapNotNull { l -> Regex("^\\s*\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+(\"[^\"]+\"|\\S+)").find(l)?.groupValues?.get(1)?.removeSurrounding("\"") }.toList()
                else -> texto.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
            }
            for (n in nombres) {
                // Solo archivos de la misma carpeta (o subcarpetas): nunca rutas hacia arriba.
                if (n.contains("..") || n.startsWith("/")) continue
                val hijo = "$padre/${n.replace('\\', '/')}"
                if (hijo in salida) continue
                info(hijo)?.let { salida[hijo] = it; porRevisar.add(hijo) }
            }
        }
        return salida.values.toList()
    }

    private fun tamanoCarpeta(arbol: Uri, docId: String, profundidad: Int = 0): Long {
        if (profundidad > 6) return 0L
        var total = 0L
        try {
            app.contentResolver.query(
                DocumentsContract.buildChildDocumentsUriUsingTree(arbol, docId),
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    total += if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) tamanoCarpeta(arbol, c.getString(0), profundidad + 1) else c.getLong(1)
                }
            }
        } catch (_: Exception) {}
        return total
    }

    private fun tamanoBonito(b: Long): String = when {
        b >= 1L shl 30 -> String.format(java.util.Locale.US, "%.1f GB", b / (1L shl 30).toDouble())
        b >= 1L shl 20 -> "${b / (1L shl 20)} MB"
        else -> "${(b / 1024).coerceAtLeast(1)} KB"
    }

    /** Borra los archivos y todo lo que Díptico guardaba del juego, y lo saca del estante. */
    private fun borrarJuego(j: Juego, lista: List<PorBorrar>) {
        scope.launch {
            val fallidos = withContext(Dispatchers.IO) {
                lista.count { pb ->
                    !(try { DocumentsContract.deleteDocument(app.contentResolver, pb.uri) } catch (_: Exception) { false })
                }
            }
            if (fallidos == lista.size) {
                avisar("No pude borrar ${j.titulo}. Elige otra vez tu carpeta de juegos (Menú › Biblioteca) para darle permiso a Díptico.")
                return@launch
            }
            // Lo de Díptico: imágenes descargadas, notas, nota de voz, mapa y su lugar en widgets y colecciones.
            withContext(Dispatchers.IO) {
                medios[j.clave]?.let { m -> listOfNotNull(m.portada, m.arte, m.logo).forEach { try { java.io.File(it).delete() } catch (_: Exception) {} } }
                medios = medios - j.clave
                almacen.guardarMedios(medios)
                try { Notas.borrar(app, j.clave) } catch (_: Exception) {}
                crudos = crudos.filter { it.clave != j.clave }
                almacen.guardarBiblioteca(crudos)
            }
            val datos = estado.value.datos - j.clave
            almacen.guardarDatos(datos)
            val widgets = estado.value.widgets.filter { it.tipo != "atajo-${j.clave}" }
            if (widgets.size != estado.value.widgets.size) guardarWidgets(widgets)
            val colecciones = estado.value.colecciones.mapValues { (_, c) -> c.copy(juegos = c.juegos - j.clave) }
            if (colecciones != estado.value.colecciones) guardarColecciones(colecciones)
            actualizar { it.copy(datos = datos, notasVoz = it.notasVoz - j.clave) }
            publicarJuegos()
            avisar(if (fallidos > 0) "${j.titulo} se borró, pero quedaron $fallidos archivos que no pude borrar" else "${j.titulo} desinstalado")
        }
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

    /** Las secciones del modo simple: lo de todos los días. Las demás quedan a un toque en «Más ajustes». */
    val SECCIONES_SIMPLES = listOf(
        Seccion.ESENCIAL, Seccion.TEMAS, Seccion.DECORACIONES, Seccion.INICIO, Seccion.ESTANTE, Seccion.MUSICA,
        Seccion.BIBLIOTECA, Seccion.EMULADORES, Seccion.ASISTENTE, Seccion.PANTALLAS, Seccion.SISTEMA,
    )

    fun seccionesMenu(simple: Boolean = estado.value.menuSimple): List<Seccion> =
        if (simple) SECCIONES_SIMPLES + Seccion.MAS else Seccion.values().filter { it != Seccion.MAS }

    /** L1/R1 y la cruceta en la columna de secciones: la anterior o la siguiente de las que se ven. */
    fun saltarSeccion(delta: Int, envolver: Boolean) {
        val e = estado.value
        val s = seccionesMenu()
        // Una sección avanzada abierta desde «Más ajustes» cuenta como «Más ajustes».
        val i = s.indexOf(e.seccion).takeIf { it >= 0 } ?: s.indexOf(Seccion.MAS).coerceAtLeast(0)
        val n = if (envolver) (i + delta + s.size) % s.size else (i + delta).coerceIn(0, s.size - 1)
        if (envolver) elegirSeccion(s[n]) else actualizar { it.copy(seccion = s[n], indice = 0) }
    }

    fun cambiarMenuSimple(v: Boolean) {
        almacen.guardarBool("menuSimple", v)
        val visibles = seccionesMenu(v)
        actualizar { it.copy(menuSimple = v, seccion = if (it.seccion in visibles) it.seccion else Seccion.ESENCIAL, indice = 0) }
        avisar(if (v) "Menú simple: lo avanzado está en «Más ajustes»" else "Menú completo: todas las secciones a la vista")
    }

    private fun opcionMenuSimple(e: Estado) = Opcion(
        "Menú simple", if (e.menuSimple) "Sí" else "No", Icono.VISTA,
        "Sí: solo las secciones de todos los días y las avanzadas juntas en «Más ajustes». No: todas las secciones en la columna. Buscar encuentra cualquier ajuste igual.",
    ) { cambiarMenuSimple(!estado.value.menuSimple) }

    /** «Más ajustes»: cada sección avanzada con lo que trae, para abrirla de un toque. */
    private fun opcionesMas(e: Estado): List<Opcion> {
        val lista = mutableListOf(opcionMenuSimple(e))
        for (s in Seccion.values()) {
            if (s in SECCIONES_SIMPLES || s == Seccion.MAS) continue
            lista += Opcion(s.titulo, "", s.icono, s.ayuda) { actualizar { it.copy(seccion = s, enOpciones = true, indice = 0) } }
        }
        return lista
    }

    private var cacheOpciones: Pair<Estado, HashMap<Seccion, List<Opcion>>>? = null

    /** Las opciones de una sección. Se arman una vez por cada cambio de estado (las dos pantallas las comparten). */
    /** Subtítulo que agrupa las opciones largas de algunas secciones (así es más fácil encontrar algo). "" = sin grupo. */
    fun grupoOpcion(s: Seccion, t: String): String = when (s) {
        Seccion.BIBLIOTECA -> when {
            t == "Agregar un juego de Steam" || t.startsWith("Agregar: ") || estado.value.juegosPC.any { it.titulo == t } ||
                LANZADORES_PC.any { it.nombre == t } -> "Juegos de PC"
            else -> "Tus juegos"
        }
        Seccion.RENDIMIENTO -> if (t == "Optimizar ahora" || t == "Memoria disponible") "Ahora" else "Al jugar"
        Seccion.CONTROLES -> if (t == "Sticks") "Sticks" else if (t.startsWith("Botón") || t.startsWith("Aprender")) "Botones extra" else "Más"
        Seccion.SOBREMESA -> if (t == "Cuándo se activa" || t == "Tele conectada") "Cuándo" else "En la tele"
        Seccion.ASISTENTE -> when {
            t == "Hablar con Bisa" || t == "Bisa mientras juegas" || t == "Burbuja flotante" -> "Bisa"
            t.startsWith("Clave de") -> "Claves de IA en la nube"
            t == "Borrar la conversación" -> "Conversación"
            else -> "Su cerebro"
        }
        Seccion.TEMAS -> when {
            t.startsWith("Mi tema") || t == "Mezclar mi tema" -> "Mi tema"
            t.contains("IA") -> "Con IA"
            else -> "Elegir"
        }
        Seccion.INICIO -> when {
            t == "Al abrir Díptico" || t.startsWith("Bienvenida") || t.startsWith("Continuación") || t == "Tu nombre" -> "Al encender"
            t.startsWith("Preguntar dónde") -> "Al terminar de jugar"
            t.startsWith("Imagen grande") || t == "Barra de accesos" -> "Pantalla principal"
            t == "Estilo del inicio" || t.contains("widget", true) && !t.startsWith("Permiso") -> "Pantalla principal"
            else -> "Más"
        }
        Seccion.APARIENCIA -> when {
            t == "Modo" || t.startsWith("Efecto") || t == "Fluidez" -> "Estilo"
            t.contains("protector", true) -> "Protector de pantalla"
            t.startsWith("Zoom") || t.contains("nimación") -> "Detalles"
            else -> "Íconos de las consolas"
        }
        Seccion.MUSICA -> if (t.startsWith("Sonidos") || t.startsWith("Juego de sonidos")) "Sonidos" else "Música de fondo"
        Seccion.ESTANTE -> when {
            t.endsWith("carpetas") -> "Carpetas"
            t.startsWith("Cajas") || t.startsWith("Forma") || t.startsWith("Tamaño") || t.startsWith("Imagen") -> "Cajas"
            else -> "Qué ves y en qué orden"
        }
        Seccion.PORTADAS -> when {
            t.startsWith("Reconocer") || t == "Títulos de libretro" -> "Nombres de tus juegos"
            t.startsWith("Descarga") || t.startsWith("Buscar de nuevo") || t.startsWith("Imágenes de") || t.startsWith("Borrar") -> "Imágenes"
            else -> "Cuentas para más imágenes"
        }
        Seccion.SISTEMA -> when {
            t.startsWith("Respald") || t.contains("respaldo", true) || t.startsWith("Cambiar dónde") || t.startsWith("Elegir dónde") -> "Respaldo"
            t.startsWith("Descargar e instalar") || t.contains("actualizaciones") || t.startsWith("Avisarme") -> "Actualizaciones"
            t.startsWith("Reportar") || t.startsWith("Ver las novedades") || t.startsWith("Recorrido") || t.startsWith("Repetir") -> "Ayuda"
            else -> "Díptico"
        }
        Seccion.PANTALLAS -> when {
            t == "Tu equipo" -> "Tu equipo"
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
        val todas = Seccion.values().filter { it != Seccion.ESENCIAL && it != Seccion.MAS }.flatMap { s -> armarOpciones(s).map { s to it } }
        if (e.buscarAjuste.isNotBlank()) {
            val q = Busqueda.normalizar(e.buscarAjuste)
            val res = todas.filter { (s, o) ->
                listOf(o.titulo, o.ayuda, s.titulo, Idioma.t(o.titulo), Idioma.t(s.titulo)).any { Busqueda.normalizar(it).contains(q) }
            }.map { (s, o) -> o.copy(valor = o.valor.ifEmpty { s.titulo }) }
            return listOf(Opcion("Quitar la búsqueda", "\"${e.buscarAjuste}\" · ${res.size}", Icono.CERRAR) { actualizar { it.copy(buscarAjuste = "", indice = 0) } }) + res
        }
        val elegidos = listOf(
            "Tienda de temas", "Modo", "Tamaño de letra", "Modo de pantallas", "Intercambiar pantallas", "Teclado", "Música de fondo",
            "Sonidos al moverte", "Fechas especiales", "Efecto de los botones", "Fluidez", "Bisa mientras juegas",
        )
        val lista = mutableListOf(
            Opcion("Buscar un ajuste", "Y", Icono.BUSCAR, "Escribe una palabra (música, tema, teclado…) y te muestro dónde está, aunque esté en una sección avanzada.") { abrirTexto("buscar_ajuste") },
            opcionMenuSimple(e),
            Opcion("Idioma", (Idioma.IDIOMAS.firstOrNull { it.first == e.idioma }?.second ?: e.idioma), Icono.IDIOMA) { abrirMenu(Seccion.IDIOMA) },
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
            Seccion.SOBREMESA -> lista += opcionesSobremesa(e)
            Seccion.CONTROLES -> lista += opcionesBotones(e)
            Seccion.RENDIMIENTO -> lista += opcionesTurbo(e)
            Seccion.CONSOLAS -> {
                lista += opcionesIconos(e).first()
                for (c in e.consolasConJuegos) {
                    val n = e.juegosPorConsola[c.id].orEmpty().size
                    val propia = e.consolasPropias[c.id]?.vacia == false || e.iconosPropios.containsKey(c.id)
                    lista += Opcion(
                        c.bonito, if (propia) "Personalizada ✓" else if (n == 1) "1 juego" else "$n juegos", Icono.MANDO,
                        "Cambia su nombre, color, ícono, imagen grande, logo, música, cajas y emulador.",
                    ) { abrirEditarConsola(c.id, desdeMenu = true) }
                }
                if (lista.isEmpty()) lista += Opcion("Aún no hay consolas", "Elige tu carpeta de juegos", Icono.CARPETA, ayuda = "Primero elige la carpeta donde guardas tus juegos y aquí aparecerán tus consolas.") { abrirMenu(Seccion.BIBLIOTECA) }
            }
            Seccion.PANTALLAS -> {
                val q = equipo()
                lista += Opcion(
                    "Tu equipo", q.nombre, Icono.MANDO,
                    "${q.tipoBonito} · ${q.ramGb} GB de memoria · potencia ${q.gama}. Elige esto para que Díptico acomode solo la distribución, las vistas y los efectos para este equipo.",
                ) { optimizarParaEquipo() }
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
            Seccion.DECORACIONES -> {
                lista += Opcion("Configurar por tema", icono = Icono.PALETA,
                    ayuda = "Despliega un tema para elegir su adorno y la intensidad.") { abrirDecoraciones() }
                lista += Opcion("Animar decoraciones", if (e.decoraciones) "Sí" else "No", Icono.ESTRELLA,
                    ayuda = "Enciende o apaga los adornos animados de todos los temas.") {
                    val activar = !estado.value.decoraciones
                    almacen.guardarBool("decoraciones", activar)
                    actualizar { it.copy(decoraciones = activar) }
                    aplicarTema()
                }
            }
            Seccion.LOGROS -> lista += opcionesLogros(e)
            Seccion.ESENCIAL -> lista += opcionesEsenciales(e)
            Seccion.MAS -> lista += opcionesMas(e)
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
                    "Su cerebro: la IA de tu consola (gratis, sin internet), Gemini (Google, con nivel gratis) o ChatGPT. Cada persona usa su propia clave.",
                ) { abrirGaleria("ia") }
                lista += Opcion(
                    "Buscar guías en internet", if (e.bisaWeb) "Sí" else "No", Icono.WEB,
                    "Con Gemini o ChatGPT, Bisa busca en internet para darte datos exactos de cada juego. La IA de tu consola no busca.",
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
                if (e.iaLocalLista) lista += Opcion("Borrar la IA local", "Libera 1,6 GB", Icono.BORRAR, ayuda = "Borra el modelo de IA que se descargó a la consola. Bisa sigue funcionando con lo básico o con la nube.") {
                    IALocal.borrar(app)
                    actualizar { it.copy(iaLocalLista = false) }
                    avisar("IA local borrada")
                }
                lista += Opcion("Borrar la conversación", if (e.chat.isEmpty()) "Vacía" else "${e.chat.size} mensajes", Icono.BORRAR, ayuda = "Borra lo que hablaste con Bisa. No toca tus ajustes ni tus juegos.") { borrarChat(); avisar("Conversación borrada") }
            }
            Seccion.IDIOMA -> Idioma.IDIOMAS.forEach { (cod, nombre) ->
                lista += Opcion(nombre, if (e.idioma == cod) "En uso ✓" else "", Icono.IDIOMA, Idioma.ayuda(cod)) { cambiarIdioma(cod) }
            }
            Seccion.APARIENCIA -> {
                lista += Opcion("Modo", if (e.oscuro) "Oscuro" else "Claro", Icono.TEMA, "El oscuro ahorra batería en pantallas OLED. Algunos temas usan siempre el suyo.") { alternarTema() }
                lista += Opcion(
                    "Efecto de los botones", nombreEfecto(if (e.experiencia.vidrio) "vidrio" else e.efecto), Icono.ESTRELLA,
                    "Liso, degradado, con un brillo que pasa, o de vidrio. Se ve en el botón Jugar, el menú y las opciones elegidas.",
                ) {
                    val l = listOf("liso", "degradado", "brillo", "vidrio")
                    val experiencia = estado.value.experiencia
                    val actual = if (experiencia.vidrio) "vidrio" else estado.value.efecto
                    val v = l[(l.indexOf(actual) + 1).mod(l.size)]
                    almacen.guardarTexto("efecto", v)
                    actualizar { it.copy(efecto = v) }
                    if (experiencia.vidrio) guardarExperiencia(experiencia.copy(vidrio = false)) else aplicarTema()
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
                lista += Opcion("Ver el protector ahora", icono = Icono.RELOJ, ayuda = "Muestra el protector de pantalla ya mismo, para ver cómo queda.") { cerrarPanel(); actualizar { it.copy(protector = true) } }
                lista += Opcion(
                    "Zoom lento en las portadas", if (e.zoomLento) "Sí" else "No", Icono.PORTADA,
                    "La portada elegida y la imagen grande se acercan muy despacio, como en un documental. Apágalo si prefieres todo quieto.",
                ) {
                    val v = !estado.value.zoomLento
                    almacen.guardarBool("zoomLento", v)
                    actualizar { it.copy(zoomLento = v) }
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
                lista += opcionesPersonalizacion(e)
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
                lista += Opcion(
                    "Juego de sonidos", nombrePaqueteSonidos(e.paqueteSonidos), Icono.VOLUMEN,
                    "Del tema: cada tema trae los suyos. Clásicos: campanitas suaves. Bloques: madera y pops. Retro: 8 bits. Burbuja: gotitas.",
                    menos = { cambiarPaqueteSonidos(-1) },
                ) { cambiarPaqueteSonidos(1) }
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
                if (e.seleccionado != null) lista += Opcion("Buscar imágenes del juego", "Portadas por fuente", Icono.PORTADA,
                    "Elige imágenes para el juego seleccionado. Libretro, Wikipedia y Steam para PC funcionan sin clave.") {
                    abrirFicha(); buscarCandidatos("portada")
                }
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
                lista += opcionesRaspadores(e)
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
                lista += Opcion("Importar lista de ES-DE", icono = Icono.DESCARGA,
                    ayuda = "Elige gamelist.xml tras escanear tus juegos. Revisa los títulos y favoritos antes de importar.") {
                    pedirArchivoPara("es-de", "*/*")
                }
                lista += Opcion(
                    "Volver a buscar juegos", if (e.escaneando) "Buscando…" else "${e.juegos.size} juegos", Icono.BUSCAR,
                    "Úsalo cuando agregues juegos nuevos a tu carpeta.",
                ) { escanear() }
                val ocultos = e.datos.count { it.value.oculto }
                lista += Opcion(
                    "Juegos ocultos", if (ocultos == 0) "Ninguno" else "$ocultos", Icono.OCULTAR,
                    "Los juegos que ocultaste desde sus opciones (Y). Desde aquí los ves y los vuelves a mostrar.",
                ) { abrirJuegosOcultos() }
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
                lista += opcionesPC(e)
            }
            Seccion.SISTEMA -> {
                lista += opcionesRespaldo(e)
                lista += opcionesActualizar(e)
                lista += Opcion(
                    "Reportar un problema", icono = Icono.INFO,
                    ayuda = "Describe qué pasó y revisa el envío del diagnóstico al equipo de Díptico.",
                ) { prepararReporte() }
                lista += Opcion("Ver las novedades de esta versión", icono = Icono.CHISPA, ayuda = "El tour corto de lo nuevo, otra vez.") { cerrarPanel(); actualizar { it.copy(novedades = 0, recorrido = false, novedadesDesde = "") } }
                lista += Opcion("Recorrido por Díptico", icono = Icono.TUTORIAL, ayuda = "El paseo de la primera vez por todas las opciones y botones.") { verRecorrido() }
                lista += Opcion("Repetir el tutorial", icono = Icono.TUTORIAL, ayuda = "Vuelve a configurar todo paso a paso.") { reiniciarTutorial() }
                lista += Opcion(
                    "Acerca de Díptico", "Versión $VERSION.${Actualizador.numeroActual(app)}", Icono.INFO,
                    "Hecho para la AYN Thor y consolas de una pantalla. Abre los emuladores con las reglas de ES-DE.",
                ) { avisar("Díptico $VERSION · hecho con cariño para jugar") }
                lista += Opcion("Cerrar el menú", icono = Icono.CERRAR, ayuda = "También con B o Start.") { cerrarPanel() }
            }
        }
        // Las opciones de un mismo grupo quedan juntas, bajo un solo subtítulo (en el orden en que aparece cada grupo).
        if (s == Seccion.ESENCIAL || s == Seccion.MAS) return lista
        return lista.groupBy { grupoOpcion(s, it.titulo) }.values.flatten()
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
                if (c.esPorts && clave.isBlank() && !estado.value.raspadores.alguna) return@forEachIndexed
                val previo = if (todo) null else medios[j.clave]
                val r = withContext(Dispatchers.IO) {
                    try { Descargas.buscar(app, j, c, previo, clave, estado.value.raspadores) } catch (ex: Exception) { null }
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
                try { Descargas.buscar(app, crudo, c, null, estado.value.claveSgdb, estado.value.raspadores) } catch (ex: Exception) { null }
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

    // ---------- Personalización y perfiles visuales ----------
    var exportarVisual: ((String) -> Unit)? = null
    var importarVisual: (() -> Unit)? = null
    private var imagenVisual: Pair<String, String>? = null
    fun cancelarArchivoVisual() { imagenVisual = null }

    fun guardarExperiencia(v: Experiencia) {
        almacen.guardarTexto("experiencia", v.json(local = true).toString())
        actualizar { it.copy(experiencia = v) }
        cl.nikalbazu.diptico.ui.Estilo.efecto = if (v.vidrio) "vidrio" else estado.value.efecto
        aplicarTema()
    }

    private fun capturarPerfil(): Experiencia = estado.value.let { e -> e.experiencia.copy(
        posiciones = e.widgets.map { w -> PosicionVisual(w.tipo, w.x, w.y, w.an, w.al, w.pagina, w.visible,
            w.estilo, w.color, w.radio, w.opacidad, w.variante, w.escala) },
        columnas = e.gridCols, filas = e.gridFilas, paginas = e.paginas,
        inicioPerfil = e.inicio, vistaPerfil = e.vista, sistemasPerfil = e.vistaConsolas,
    ) }

    private fun aplicarPerfil(v: Experiencia) {
        guardarExperiencia(v.copy(posiciones = emptyList()))
        if (v.posiciones.isNotEmpty()) {
            val posiciones = v.posiciones.associateBy { it.tipo }
            val widgets = estado.value.widgets.map { w ->
                val p = posiciones[w.tipo] ?: return@map w
                w.copy(x = p.x, y = p.y, an = p.an, al = p.al, pagina = p.pagina, visible = p.visible,
                    estilo = p.estilo ?: w.estilo, color = p.color ?: w.color, radio = p.radio ?: w.radio,
                    opacidad = p.opacidad ?: w.opacidad, variante = p.variante ?: w.variante, escala = p.escala ?: w.escala)
            }
            val cols = v.columnas.coerceIn(4, 8)
            val filas = v.filas.coerceIn(3, 8)
            val r = Rejilla.distribuir(widgets, cols, filas, v.paginas)
            almacen.guardarTexto("gridCols", cols.toString())
            almacen.guardarTexto("gridFilas", filas.toString())
            almacen.guardarTexto("paginas", r.paginas.toString())
            Widgets.COLUMNAS = cols
            Widgets.FILAS = filas
            guardarWidgets(r.widgets)
            actualizar { it.copy(gridCols = cols, gridFilas = filas, paginas = r.paginas, paginaInicio = 0) }
        }
        v.inicioPerfil?.let { inicio ->
            almacen.guardarTexto("inicio", inicio)
            actualizar { it.copy(inicio = inicio) }
        }
        v.vistaPerfil?.let { vista ->
            almacen.guardarTexto("vista", vista)
            actualizar { it.copy(vista = vista) }
        }
        v.sistemasPerfil?.let { elegirVistaConsolas(it) }
    }

    private fun plantillaVisual(nombre: String) {
        val e = estado.value
        val elegidos = when (nombre) {
            "Jugar" -> setOf("juego", "reloj", "proximaPartida", "tengo20")
            "Explorar" -> setOf("buscar", "recientes", "colaJuegos", "favoritos")
            else -> setOf("juego", "reloj")
        }
        val widgets = Widgets.porDefecto.map { w -> w.copy(visible = w.tipo in elegidos) }
        val r = Rejilla.distribuir(widgets, 6, 4, 1)
        val plantilla = e.experiencia.copy(
            vidrio = nombre != "Ligero", movimiento = nombre != "Ligero",
            posiciones = r.widgets.map { PosicionVisual(it.tipo, it.x, it.y, it.an, it.al, it.pagina, it.visible) },
            columnas = 6, filas = 4, paginas = r.paginas, inicioPerfil = "widgets",
            vistaPerfil = if (nombre == "Ligero") "lista" else "cuadricula",
            sistemasPerfil = if (nombre == "Explorar") "carpetas" else "arriba",
            vistaPartida = when (nombre) {
                "Jugar" -> VistaPartida.BITACORA.name
                "Ligero" -> VistaPartida.LIGERA.name
                else -> VistaPartida.CLASICA.name
            },
        )
        mostrarDialogo(Dialogo("plantilla-visual", "Aplicar $nombre", "Cambia la distribución del inicio y el estilo. Conserva las personalizaciones de cada juego.",
            Icono.PALETA, si = "Aplicar", no = "Cancelar")) {
            almacen.guardarTexto("perfilVisualAnterior", capturarPerfil().json(local = true).toString())
            aplicarPerfil(plantilla)
        }
    }

    fun recibirPerfilVisual(texto: String) {
        try {
            val v = Experiencia.importar(texto)
            mostrarDialogo(Dialogo("perfil_visual", "Importar perfil visual", "Universo: ${if (v.universo) "sí" else "no"} · Vidrio: ${if (v.vidrio) "sí" else "no"}. Tus colecciones y juegos se conservan.", Icono.PALETA)) {
                aplicarPerfil(v.copy(colecciones = estado.value.experiencia.colecciones))
            }
        } catch (_: Exception) { avisar("Ese archivo no es un perfil visual compatible") }
    }

    fun abrirColeccionVisual(id: String) {
        if (id !in estado.value.colecciones) return
        abrirWidgetTipo("coleccion-$id")
        actualizar { it.copy(coleccionVisual = id) }
    }

    fun fijarFicha() {
        val e = estado.value
        if (e.unaPantalla || e.sesion != null) return
        actualizar { it.copy(fichaFijada = if (it.fichaFijada != null) null else it.seleccionado?.clave) }
    }

    private fun vigilarUniverso() {
        scope.launch {
            estado.map { e ->
                if (!e.experiencia.universo) return@map null
                val c = e.coleccionVisual?.let { e.experiencia.colecciones[it] }
                val j = e.sesion?.let { e.juegosPorClave[it.clave] } ?: e.seleccionado
                c?.color to (j?.arte ?: j?.portada)
            }.distinctUntilChanged().collectLatest { fuente ->
                // Ignora el color de una portada anterior si ya te moviste a otro juego.
                val color = if (fuente == null) 0L else fuente.first ?: withContext(Dispatchers.IO) {
                    cl.nikalbazu.diptico.ui.ColorUniverso.leer(app, fuente.second)
                }
                if (color != estado.value.colorUniverso) {
                    actualizar { it.copy(colorUniverso = color) }
                    aplicarTema()
                }
            }
        }
    }

    private fun opcionesPersonalizacion(e: Estado): List<Opcion> = buildList {
        val v = e.experiencia
        add(Opcion("Colores del juego · Universo", if (v.universo) "Sí" else "No", Icono.MUNDO, "Colores tomados de la imagen del juego, compartidos por ambas pantallas. Sin imagen se conserva el tema.") { guardarExperiencia(v.copy(universo = !v.universo)) })
        add(Opcion("Movimiento ambiental", if (v.movimiento) "Suave" else "Reducido", Icono.ESTRELLA, "Apaga el movimiento ambiental y el zoom lento; en partida se mantiene estático.") {
            guardarExperiencia(v.copy(movimiento = !v.movimiento))
        })
        for (nombre in listOf("Jugar", "Explorar", "Ligero")) {
            add(Opcion("Plantilla $nombre", "Personalizable", Icono.PALETA) { plantillaVisual(nombre) })
        }
        if (almacen.texto("perfilVisualAnterior").isNotBlank()) {
            add(Opcion("Volver a mi distribución anterior", icono = Icono.CONTINUAR) {
                aplicarPerfil(Experiencia.leer(almacen.texto("perfilVisualAnterior")))
                almacen.guardarTexto("perfilVisualAnterior", "")
            })
        }
        for (n in 1..3) {
            val id = "Perfil $n"
            add(Opcion("Guardar $id", if (id in e.perfilesVisuales) "Reemplazar" else "Vacío", Icono.COPIAR) {
                val perfiles = estado.value.perfilesVisuales + (id to capturarPerfil())
                almacen.guardarTexto("perfilesVisuales", org.json.JSONObject().apply { perfiles.forEach { (k, p) -> put(k, p.json(true)) } }.toString())
                actualizar { it.copy(perfilesVisuales = perfiles) }; avisar("$id guardado")
            })
            if (id in e.perfilesVisuales) add(Opcion("Aplicar $id", icono = Icono.CHECK) { aplicarPerfil(e.perfilesVisuales.getValue(id)) })
        }
        add(Opcion("Compartir perfil visual", "Archivo JSON", Icono.WEB, "Comparte la composición, el estilo de widgets y las vistas. Los archivos locales, las claves y la biblioteca quedan en tu equipo.") { exportarVisual?.invoke(capturarPerfil().json().toString(2)) })
        add(Opcion("Importar perfil visual", "Con vista previa y confirmación", Icono.DESCARGA) { importarVisual?.invoke() })
        e.colecciones.forEach { (id, c) ->
            val p = v.colecciones[id] ?: PresentacionColeccion()
            fun guardar(nuevo: PresentacionColeccion) = guardarExperiencia(estado.value.experiencia.let { it.copy(colecciones = it.colecciones + (id to nuevo)) })
            add(Opcion("${c.nombre} · abrir", "Presentación", Icono.CARPETA) { abrirColeccionVisual(id) })
            add(Opcion("${c.nombre} · color", "Cambiar", Icono.PALETA) { val l = Temas.MUESTRAS; guardar(p.copy(color = l[(l.indexOf(p.color) + 1).mod(l.size)])) })
            add(Opcion("${c.nombre} · disposición", p.vista, Icono.VISTA) { guardar(p.copy(vista = if (p.vista == "mosaico") "galeria" else "mosaico")) })
            listOf("fondo", "logo", "musica").forEach { tipo ->
                add(Opcion("${c.nombre} · $tipo", "Elegir archivo", if (tipo == "musica") Icono.MUSICA else Icono.PORTADA) { imagenVisual = id to tipo; pedirArchivo?.invoke(if (tipo == "musica") "audio/*" else "image/*") })
            }
            add(Opcion("${c.nombre} · restaurar diseño", icono = Icono.CONTINUAR) { guardarExperiencia(v.copy(colecciones = v.colecciones - id)) })
        }
    }

    // ---------- Temas ----------

    private fun temasPersonales(e: Estado): List<Tema> {
        val temaAnterior = Temas.incluidos.firstOrNull { it.id == e.tema && Temas.iniciales.none { t -> t.id == it.id } }
        return (e.temasPropios + listOfNotNull(temaAnterior)).distinctBy { it.id }
    }

    fun todosLosTemas(e: Estado = estado.value): List<Tema> = Temas.iniciales + temasPersonales(e)

    private fun conAdorno(e: Estado, tema: Tema): Tema {
        val decor = e.adornosPorTema[tema.id]?.takeIf { it in Temas.DECORACIONES } ?: tema.decor
        val intensidad = e.intensidadPorTema[tema.id]?.toIntOrNull()?.takeIf { it in 1..3 } ?: tema.intensidadDecoracion
        return if (decor == tema.decor && intensidad == tema.intensidadDecoracion) tema
        else tema.copy(decor = decor, intensidadDecoracion = intensidad)
    }

    private fun temasDecoracion(e: Estado): List<Tema> = when (e.galeriaPestana) {
        1 -> catalogoOficial.orEmpty().map { it.tema }
        2 -> Temas.fiestas.map { it.tema }
        else -> todosLosTemas(e)
    }

    private fun itemsDecoraciones(e: Estado): List<ItemGaleria> = buildList {
        temasDecoracion(e).forEachIndexed { i, tema ->
            val abierto = e.decoracionAbierta == tema.id
            val activo = conAdorno(e, tema)
            val detalle = if (activo.decor == "nada") Temas.nombreDecor(activo.decor)
            else "${Temas.nombreDecor(activo.decor)} · ${Temas.intensidadNombre(activo.intensidadDecoracion)}"
            add(ItemGaleria("decor-tema:${tema.id}", tema.nombre, detalle, abierto, activo) {
                actualizar { it.copy(decoracionAbierta = if (abierto) null else tema.id, indice = i) }
            })
            if (abierto) {
                val estilos = (Temas.decoracionesPara(tema) + activo.decor).distinct()
                estilos.forEach { decor ->
                    add(ItemGaleria("decor-tipo:$decor", Temas.nombreDecor(decor), "Tipo de decoración",
                        activo.decor == decor, activo.copy(decor = decor)) {
                        guardarDecoracion(tema.id, decor, e.intensidadPorTema[tema.id]?.toIntOrNull())
                    })
                }
                for (nivel in 1..3) {
                    add(ItemGaleria("decor-intensidad:$nivel", Temas.intensidadNombre(nivel), "Intensidad",
                        activo.intensidadDecoracion == nivel, activo.copy(intensidadDecoracion = nivel)) {
                        guardarDecoracion(tema.id, e.adornosPorTema[tema.id], nivel)
                    })
                }
                add(ItemGaleria("decor-restaurar", "Restaurar decoración", "Valores originales del tema", false, tema) {
                    guardarDecoracion(tema.id, null, null)
                })
            }
        }
    }

    private fun guardarDecoracion(id: String, decor: String?, nivel: Int?) {
        val e = estado.value
        val estilos = if (decor == null) e.adornosPorTema - id else e.adornosPorTema + (id to decor)
        val intensidades = if (nivel == null) e.intensidadPorTema - id else e.intensidadPorTema + (id to nivel.toString())
        if (estilos == e.adornosPorTema && intensidades == e.intensidadPorTema) return
        almacen.guardarTexto("adornosPorTema", escribirMapa(estilos))
        almacen.guardarTexto("intensidadPorTema", escribirMapa(intensidades))
        actualizar { it.copy(adornosPorTema = estilos, intensidadPorTema = intensidades) }
        aplicarTema()
    }

    /** Aplica el tema elegido, o el de la fiesta de hoy si está activado. */
    fun aplicarTema() {
        val e = estado.value
        val fiesta = if (e.temaFestivo) Temas.deHoy() else null
        val elegido = todosLosTemas(e).firstOrNull { it.id == e.tema }
            ?: Temas.fiestas.firstOrNull { it.tema.id == e.tema }?.tema
            ?: Temas.incluidos[0]
        val baseVisual = conAdorno(e, fiesta?.tema ?: elegido)
        val activo = if (e.experiencia.universo && e.colorUniverso != 0L) baseVisual.copy(acento = e.colorUniverso, acento2 = e.colorUniverso, decor = "nada") else baseVisual
        cl.nikalbazu.diptico.ui.Estilo.tema = activo
        cl.nikalbazu.diptico.ui.Estilo.efecto = activo.acabado.ifBlank { if (e.experiencia.vidrio) "vidrio" else e.efecto }
        aplicarFluidez()
        Sonidos.cambiarJuego(activo.sonidos, activo.tonoSonidos)
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

    private fun temaParaMezclar(e: Estado): Tema {
        e.temasPropios.firstOrNull { it.id == "mio" }?.let { return it }
        val elegido = todosLosTemas(e).firstOrNull { it.id == e.tema } ?: Temas.iniciales.first()
        return ComponenteTema.MUSICA.combinar(
            conAdorno(e, elegido).copy(id = "mio", nombre = "Mi tema", propio = true, descripcion = ""),
            elegido,
        )
    }

    private fun editarMiTema(f: (Tema) -> Tema) {
        val e = estado.value
        val nuevo = f(temaParaMezclar(e))
        guardarTemasPropios(listOf(nuevo) + e.temasPropios.filter { it.id != "mio" })
        if (e.tema != "mio") elegirTema("mio") else aplicarTema()
    }

    private fun importarTemas(leer: () -> List<TemaComunidad>) {
        if (trabajosMapa.containsKey("catalogo_temas")) return
        trabajo("catalogo_temas", "Leyendo el tema")
        scope.launch {
            val resultado = withContext(Dispatchers.IO) { runCatching(leer) }
            terminarTrabajo("catalogo_temas")
            resultado.onSuccess { temas ->
                if (temas.size == 1) confirmarTemaImportado(temas.first())
                else {
                    actualizar { it.copy(catalogoTemas = temas) }
                    abrirGaleria("importar_temas")
                }
            }.onFailure { avisar(it.message ?: "No pude leer el tema.") }
        }
    }

    private fun confirmarTemaImportado(entrada: TemaComunidad) {
        val tema = entrada.tema
        val detalle = tema.descripcion.ifBlank { "Tema de ${entrada.autor.ifBlank { "la comunidad" }}" }
        mostrarDialogo(Dialogo("instalar_tema", "Instalar ${tema.nombre}", detalle, Icono.PALETA,
            si = "Instalar y usar", no = "Cancelar")) { instalarTemaComunidad(entrada) }
    }

    private fun instalarTemaComunidad(entrada: TemaComunidad) {
        if (trabajosMapa.containsKey("instalar_tema")) return
        trabajo("instalar_tema", "Instalando ${entrada.tema.nombre}")
        scope.launch {
            val resultado = withContext(Dispatchers.IO) { runCatching { TiendaTemas.instalar(app, entrada) } }
            terminarTrabajo("instalar_tema")
            resultado.onSuccess { tema ->
                guardarTemasPropios(listOf(tema) + estado.value.temasPropios.filterNot { it.id == tema.id })
                elegirTema(tema.id)
                val actual = estado.value
                if (actual.panel == Panel.GALERIA && actual.galeria == "importar_temas") abrirGaleria("temas", pestana = 1)
                avisar(if (actual.fiesta != null) "${tema.nombre} instalado" else "${tema.nombre} instalado y aplicado")
            }.onFailure { avisar(it.message ?: "No pude instalar este tema.") }
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
        lista.add(
            0,
            Opcion(
                "Elegir tema", cl.nikalbazu.diptico.ui.Estilo.tema.nombre, Icono.PALETA,
                "Cinco temas incluidos y los que hayas instalado. Cada tema trae una explicación y vista previa.",
            ) { abrirGaleria("temas") },
        )
        lista.add(
            1,
            Opcion(
                "Mis colores", "Toda la interfaz", Icono.PALETA,
                "Elige tú el color principal, el secundario, los detalles, el fondo y las letras de todo Díptico.",
            ) { abrirGaleria("colores") },
        )
        lista.add(0, Opcion("Tienda de temas", "${TiendaTemas.TOTAL_OFICIAL} temas", Icono.PALETA,
            "Explora e instala temas dentro de Díptico.") { abrirTiendaTemas() })
        lista.add(3, Opcion("Importar tema", "Archivo JSON", Icono.DESCARGA,
            "Selecciona un tema que hayas descargado.") { pedirArchivoPara("temaJson", "*/*") })
        lista.add(4, Opcion("Importar por enlace", "Tema o catálogo", Icono.WEB,
            "Pega el enlace HTTPS directo al archivo JSON.") { abrirTexto("tema_enlace") })
        lista += Opcion("Aportar un tema", "Abre el editor web", Icono.WEB,
            "Crea el archivo de un tema y aprende cómo compartirlo con la comunidad.") { abrirAportesTemas() }
        lista.add(2, Opcion("Mezclar mi tema", "Temas instalados", Icono.PALETA,
            "Elige de qué tema viene cada elemento. Cada cambio se guarda en Mi tema.") { abrirGaleria("mezclar_tema") })
        val mio = e.temasPropios.firstOrNull { it.id == "mio" }
        lista += Opcion("Mi tema · fondo", when (mio?.base) { "oscuro" -> "Oscuro"; "claro" -> "Claro"; "oled" -> "Negro puro"; else -> "Según el modo" }, Icono.TEMA, ayuda = "El fondo de tu tema propio: oscuro, claro, negro puro (ideal para OLED) o según el modo.") {
            editarMiTema { it.copy(base = when (it.base) { "auto" -> "oscuro"; "oscuro" -> "oled"; "oled" -> "claro"; else -> "auto" }) }
        }
        return lista
    }

    // ---------- Íconos de consola ----------

    private fun estilosIconos(): List<String> = listOf("diptico", "insignia", "solido", "contorno", "neon", "pixel", "siglas") + Descargas.PAQUETES.map { it.id }

    fun nombreEstiloIconos(s: String) = when (s) {
        "diptico" -> "Formatos de Díptico"
        "siglas" -> "Siglas"
        "insignia" -> "Insignias"
        "contorno" -> "Contornos"
        "pixel" -> "Píxeles"
        "solido" -> "Sellos de color"
        "neon" -> "Neón"
        else -> Descargas.PAQUETES.firstOrNull { it.id == s }?.nombre ?: s
    }

    private fun opcionesIconos(e: Estado): List<Opcion> {
        val c = Consolas.porId(e.consolaSel)
        val lista = mutableListOf(

            Opcion(
                "Íconos de consola", nombreEstiloIconos(e.estiloIconos), Icono.EMULADOR,
                "Estilos de Díptico (formatos, insignias, contornos, píxeles o siglas: todas las consolas con la misma línea), o paquetes de RetroArch (se descargan una vez). Para una sola consola: Menú › Tus consolas.",
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
                lista += Opcion("Quitar ícono propio de ${c.nombre}", icono = Icono.BORRAR, ayuda = "Vuelve al ícono del estilo elegido para esta consola.") {
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
        Opcion("Clip al retomar", if (e.clipRetoma) "Activado" else "Desactivado", Icono.CONTINUAR, "En dos pantallas: pide el permiso de grabación de Android al abrir un juego y guarda solo los últimos 6 segundos, sin audio. La lupa tiene prioridad. Puede afectar al rendimiento; se desactiva aquí.") { alternarClipRetoma() },
        Opcion("Preparar clip de esta partida", "Pedir permiso de Android", Icono.CONTINUAR, "Después de bloquear la consola Android puede finalizar la captura. Este botón prepara una nueva para la siguiente retoma.", apagada = e.sesion == null || !e.clipRetoma) { pedirPermisoRetoma(false) },
        Opcion("Retomar después de", "${e.esperaRetoma} segundos", Icono.RELOJ, "Tiempo mínimo con la pantalla apagada para mostrar el recordatorio al abrir la consola.") { cambiarEsperaRetoma() },
        Opcion(
            "Al abrir Díptico", nombreArranque(e.arranque), Icono.INTRO,
            "Con qué parte Díptico cada vez que se abre: como lo dejaste, las carátulas, tus widgets, tus consolas o directo en tu último juego.",
            menos = { cambiarArranque(-1) },
        ) { cambiarArranque(1) },
        Opcion(
            "Bienvenida al encender", nombreBienvenida(e.bienvenidaModo), Icono.CHISPA,
            "Al prender la consola: un saludo con la hora, tu último juego para seguir y Bisa. Si estabas jugando, un recordatorio de dónde ibas. Nada se detiene mientras tanto.",
        ) { cambiarBienvenidaModo() },
        Opcion(
            "Continuación sobre el juego", if (cl.nikalbazu.diptico.bisa.Cartel.puede(app)) "Lista ✓" else "Dar permiso", Icono.CONTINUAR,
            "Con una pantalla, al prender la consola en medio de un juego aparece encima una tarjeta con el juego, tu tiempo y tu próximo paso. Android pide permiso para mostrarse sobre otras apps.",
        ) {
            if (cl.nikalbazu.diptico.bisa.Cartel.puede(app)) avisar("La continuación ya está lista")
            else try {
                app.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${app.packageName}")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) { avisar("No pude abrir el permiso") }
        },
        Opcion(
            "Barra de accesos", "Fija · arriba en Widgets", Icono.APPS,
            "En la imagen del juego queda abajo; al pasar a Widgets sube y reúne los accesos en un solo lugar."
        ) { avisar("La barra está siempre disponible") },
        Opcion("Tu nombre", e.nombre.ifBlank { "Sin nombre" }, Icono.TEXTO, "Para que Bisa te salude por tu nombre.") { abrirTexto("nombre", estado.value.nombre) },
        Opcion("Preguntar dónde quedaste", if (e.preguntarQuedaste) "Sí" else "No", Icono.LAPIZ,
            "Al terminar de jugar, una tarjeta para anotar dónde quedaste: escribiendo, con una nota de voz o marcando en el mapa del juego. Siempre se puede pasar de largo.") {
            val v = !estado.value.preguntarQuedaste
            almacen.guardarBool("preguntarQuedaste", v)
            actualizar { it.copy(preguntarQuedaste = v) }
        },
        Opcion("Imagen grande de los juegos", nombreHeroe(e.imagenHeroe), Icono.PORTADA,
            "Qué imagen se ve en grande en el inicio: la imagen grande (hero) o la portada. Cada juego puede tener la suya (ventana del juego, Y).") {
            val v = if (estado.value.imagenHeroe == "portada") "arte" else "portada"
            almacen.guardarTexto("imagenHeroe", v)
            actualizar { it.copy(imagenHeroe = v) }
        },
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

    private fun cambiarArranque(d: Int) {
        val i = ARRANQUES.indexOf(estado.value.arranque).coerceAtLeast(0)
        val v = ARRANQUES[Math.floorMod(i + d, ARRANQUES.size)]
        almacen.guardarTexto("arranque", v)
        actualizar { it.copy(arranque = v) }
        avisar("Al abrir: ${nombreArranque(v)}")
    }

    /** Select: cambia entre el inicio con widgets y el de carátulas. */
    /** Abre un widget por su tipo (al tocarlo). */
    fun abrirWidgetTipo(tipo: String) {
        val i = estado.value.widgets.indexOfFirst { it.tipo == tipo }
        if (i >= 0) abrirWidget(i)
    }

    fun alternarInicio() {
        actualizar { it.copy(widgetFoco = -1) }
        val v = if (estado.value.inicio == "widgets") "heroe" else "widgets"
        almacen.guardarTexto("inicio", v)
        actualizar { it.copy(inicio = v, editandoWidgets = false, avisoInicio = if (v == "widgets") System.currentTimeMillis() else 0L) }
        Sonidos.deslizar()
    }

    private fun guardarWidgets(l: List<Widget>) {
        almacen.guardarTexto("widgets", escribirWidgets(l))
        actualizar { it.copy(widgets = l) }
    }

    /** Entra al modo de acomodar widgets (en la pantalla del inicio). */
    fun editarWidgets() {
        almacen.guardarTexto("inicio", "widgets")
        val primero = widgetsDePagina().firstOrNull() ?: -1
        actualizar { it.copy(inicio = "widgets", editandoWidgets = true, widgetSel = primero, panel = Panel.ESTUDIO, estudioPestana = 0, estudioMover = false, indice = 0, widgetFoco = -1, estudioZona = "grilla", burbuja = null, celdaCatalogo = null) }
    }

    fun terminarEdicion() {
        almacen.guardarTexto("widgets", escribirWidgets(estado.value.widgets))
        actualizar { it.copy(editandoWidgets = false, estudioMover = false, burbuja = null, celdaCatalogo = null,
            panel = if (it.panel == Panel.ESTUDIO) Panel.NINGUNO else it.panel) }
        avisar("Inicio guardado")
    }

    private fun cambiarWidgetYEmpujar(i: Int, f: (Widget) -> Widget) {
        cambiarWidget(i, f)
        aplicarEmpuje()
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
                    // Avanza una celda; si ahí hay otro widget, ese se corre para dejarle espacio.
                    val c = encajar(w.copy(x = w.x + dx, y = w.y + dy))
                    if (c.x == w.x && c.y == w.y) w
                    else if (!choca(c, i)) c
                    else {
                        val l = empujando(i, c)
                        if (l != null) { pendienteEmpuje = l; c } else {
                            // Si no hay cómo correrlo, salta por encima hasta el primer lugar libre.
                            var paso = 2
                            var r: Widget? = null
                            while (paso <= maxOf(Widgets.COLUMNAS, Widgets.FILAS)) {
                                val c2 = encajar(w.copy(x = w.x + dx * paso, y = w.y + dy * paso))
                                if (c2.x == w.x && c2.y == w.y) break
                                if (!choca(c2, i)) { r = c2; break }
                                paso++
                            }
                            r ?: w
                        }
                    }
                }
            }
        }
        aplicarEmpuje()
        estado.value.widgets.getOrNull(e.widgetSel)?.let { w -> if (w.pagina != e.paginaInicio) actualizar { it.copy(paginaInicio = w.pagina) } }
    }

    /** Un solo cambio de estado para mover o estirar y reubicar a los vecinos. */
    private fun ubicarWidget(i: Int, destino: Widget): Boolean {
        val e = estado.value
        val antes = e.widgets.getOrNull(i) ?: return false
        val c = encajar(destino)
        if (c == antes) return true
        val nueva = empujando(i, c) ?: return false
        almacen.guardarTexto("widgets", escribirWidgets(nueva))
        actualizar { it.copy(widgets = nueva, widgetSel = i, paginaInicio = c.pagina) }
        return true
    }

    fun ponerWidget(i: Int, x: Int, y: Int, pagina: Int) {
        val e = estado.value
        val w = e.widgets.getOrNull(i)?.takeIf { it.visible } ?: return
        val destino = encajar(w.copy(x = x, y = y, pagina = pagina.coerceIn(0, e.paginas - 1)))
        if (ubicarWidget(i, destino)) return
        // Si el punto de caída está ocupado, busca el hueco más cercano de la página destino.
        if (destino.pagina != w.pagina) {
            val libre = (0..Widgets.FILAS - w.al).asSequence().flatMap { fila ->
                (0..Widgets.COLUMNAS - w.an).asSequence().map { columna -> destino.copy(x = columna, y = fila) }
            }.filterNot { choca(it, i, e.widgets) }
                .minByOrNull { kotlin.math.abs(it.x - destino.x) + kotlin.math.abs(it.y - destino.y) }
            if (libre != null && ubicarWidget(i, libre)) return
        }
        avisar("No hay espacio. El widget sigue en su lugar.")
    }

    /** Los widgets corridos por el último movimiento (se aplican después de mover el elegido). */
    private var pendienteEmpuje: List<Widget>? = null

    private fun aplicarEmpuje() {
        val l = pendienteEmpuje ?: return
        pendienteEmpuje = null
        val i = estado.value.widgetSel
        val actual = estado.value.widgets
        // El elegido queda como quedó; los demás toman su lugar nuevo.
        actualizar { it.copy(widgets = l.mapIndexed { k, w -> if (k == i) actual.getOrElse(k) { w } else w }) }
        Sonidos.soltar()
    }

    /**
     * Pone el widget [i] en [destino] y corre a los que tapa: primero al lugar que dejó libre (así se intercambian),
     * y si no cabe ahí, al hueco libre más cercano de la misma página. Devuelve la lista nueva o null si no hay espacio.
     */
    private fun empujando(i: Int, destino: Widget): List<Widget>? {
        val l = estado.value.widgets.toMutableList()
        val antes = l.getOrNull(i) ?: return null
        l[i] = destino
        val tapados = l.indices.filter { k -> k != i && l[k].visible && l[k].pagina == destino.pagina && solapan(l[k], destino) }
        for (k in tapados) {
            val o = l[k]
            val candidatos = buildList {
                if (antes.pagina == destino.pagina) {
                    add(o.copy(x = antes.x, y = antes.y))
                    add(o.copy(x = antes.x + antes.an - o.an, y = antes.y))
                    add(o.copy(x = antes.x, y = antes.y + antes.al - o.al))
                }
                for (yy in 0..Widgets.FILAS - o.al) for (xx in 0..Widgets.COLUMNAS - o.an) add(o.copy(x = xx, y = yy))
            }.map { encajar(it) }
            val inicioCandidatos = if (antes.pagina == destino.pagina) 3 else 0
            val elegido = candidatos.take(inicioCandidatos).firstOrNull { !choca(it, k, l) }
                ?: candidatos.drop(inicioCandidatos).sortedBy { kotlin.math.abs(it.x - o.x) + kotlin.math.abs(it.y - o.y) }.firstOrNull { !choca(it, k, l) }
                ?: return null
            l[k] = elegido
        }
        return l
    }

    private fun solapan(a: Widget, b: Widget) = a.x < b.x + b.an && b.x < a.x + a.an && a.y < b.y + b.al && b.y < a.y + a.al

    /** Cambia el ancho (L1/R1) o el alto (L2/R2) del widget elegido; si choca, corre a los otros. */
    fun cambiarTamWidget(dAn: Int, dAl: Int) {
        val i = estado.value.widgetSel
        cambiarWidgetYEmpujar(i) { w ->
            val an = (w.an + dAn).coerceIn(1, Widgets.COLUMNAS)
            val al = (w.al + dAl).coerceIn(1, Widgets.FILAS)
            // Si no cabe a la derecha o abajo, se corre hacia la izquierda o arriba.
            val c = w.copy(an = an, al = al, x = w.x.coerceAtMost(Widgets.COLUMNAS - an), y = w.y.coerceAtMost(Widgets.FILAS - al))
            if (!choca(c, i)) c
            else empujando(i, c)?.let { l -> pendienteEmpuje = l; c } ?: run { avisar("No cabe: no hay cómo correr los otros widgets"); w }
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
        avisar(Idioma.IDIOMAS.firstOrNull { it.first == cod }?.second ?: cod)
    }

    fun abrirAsistente() {
        actualizar { it.copy(panel = Panel.ASISTENTE, indice = 0) }
    }

    private var claveParaAsistente = false

    fun escribirAlAsistente() {
        abrirTexto("asistente")
    }

    fun borrarChat() {
        actualizar { it.copy(chat = emptyList(), indice = 0) }
    }

    /** Lo que se puede elegir con la cruceta en el asistente: escribir, los botones de la última respuesta o las sugerencias. */
    fun elementosAsistente(e: Estado = estado.value): List<Opcion> = buildList {
        add(Opcion("Hablar", "voz", Icono.MICROFONO) { hablarConBisa() })
        add(Opcion("Escribir", "texto", Icono.TEXTO) { escribirAlAsistente() })
        val ultima = e.chat.lastOrNull()
        val acciones = if (ultima?.rol == "ia") ultima.acciones else emptyList()
        (acciones + CentroBisa.acciones(e)).distinctBy { it.tipo to it.valor }.forEach { a ->
            add(Opcion(a.etiqueta, a.tipo, Icono.CHISPA) { ejecutarAccion(a) })
        }
        if (!e.pensando && e.chat.any { it.rol == "yo" } && motorEfectivo(e) != "ninguno") {
            add(Opcion("Más detalles", "sugerencia", Icono.INFO) { preguntar("Dame más detalles, paso a paso.") })
            e.chat.lastOrNull { it.rol == "yo" }?.texto?.let { pregunta ->
                add(Opcion("Búscalo en internet", "sugerencia", Icono.WEB) { preguntar(pregunta, forzarWeb = true) })
            }
        }
    }

    fun hablarConBisa() {
        if (estado.value.panel != Panel.ASISTENTE) abrirAsistente()
        dictar("bisa")
    }

    private fun respuestaBisa(mensaje: MensajeChat, pregunta: String? = null) {
        actualizar {
            val nuevo = if (pregunta == null) listOf(mensaje) else listOf(MensajeChat("yo", pregunta), mensaje)
            it.copy(chat = it.chat.takeLast(38) + nuevo, indice = if (mensaje.acciones.isEmpty()) 0 else 2)
        }
    }

    /** La clave guardada para una IA en la nube. */
    fun claveDe(p: String, e: Estado = estado.value) = when (p) { "gemini" -> e.claveGemini; "openai" -> e.claveOpenAI; else -> "" }

    /** Qué IA usa Bisa ahora: local (en la consola), gemini, openai o "ninguno". */
    fun motorEfectivo(e: Estado = estado.value): String = when (e.motorIA) {
        "local", "gemini", "openai" -> e.motorIA
        else -> when {
            IALocal.listo(app) -> "local"
            else -> IA.PROVEEDORES.firstOrNull { claveDe(it, e).isNotBlank() } ?: "ninguno"
        }
    }

    fun nombreMotor(m: String) = when (m) {
        "local" -> "En tu consola"; "gemini" -> "Gemini"; "openai" -> "ChatGPT"; "ninguno" -> "Sin IA aún"; else -> "Automático"
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

    fun claveTexto(p: String) = if (p == "gemini") "ia_clave_gemini" else "ia_clave_openai"

    fun preguntar(texto: String, forzarWeb: Boolean = false) {
        val e = estado.value
        if (e.pensando) return
        if (!forzarWeb) {
            val local = CentroBisa.responder(e, texto) ?: AyudaLocal.responder(texto)?.let { MensajeChat("ia", it) }
            if (local != null) { respuestaBisa(local, texto); return }
        }
        val motor = motorEfectivo(e)
        if (motor == "ninguno" || (motor == "local" && !IALocal.listo(app))) {
            respuestaBisa(MensajeChat("ia", "Puedo ayudarte con tu biblioteca, tus notas y las funciones de Díptico. Para preguntas abiertas sobre guías o trucos, configura un motor.",
                if (e.sesion == null) listOf(AccionChat("bisa_motor", "", "Configurar motor")) else emptyList()), texto)
            return
        }
        if (motor in IA.PROVEEDORES && claveDe(motor, e).isBlank()) {
            claveParaAsistente = true; abrirTexto(claveTexto(motor))
            avisar("Para usar ${nombreMotor(motor)} pega tu clave, o elige otra IA en Menú › Bisa.")
            return
        }
        val chat = e.chat.takeLast(38) + MensajeChat("yo", texto)
        // La respuesta aparece mientras se escribe (con la IA local).
        actualizar { it.copy(chat = chat, pensando = true, indice = 0) }
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                try {
                    val historia = chat.takeLast(12).filter { it.texto.isNotBlank() }.map { (if (it.rol == "yo") "user" to it.texto else "assistant" to it.texto) }
                    if (motor == "local") {
                        // La IA de la consola no sabe buscar: Díptico busca por ella y le pasa lo encontrado.
                        val web = if (forzarWeb || (estado.value.bisaWeb && Web.conviene(texto))) {
                            actualizar { it.copy(pensandoQue = "Buscando en internet…") }
                            Web.comoContexto(Web.buscar(texto, estado.value.idioma)).also { actualizar { it.copy(pensandoQue = "Pensando…") } }
                        } else ""
                        val sistema = Asistente.sistemaLocal(e) + if (web.isNotBlank()) "\n\n$web" else ""
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
                        val sistema = Asistente.sistema(e)
                        if (forzarWeb || estado.value.bisaWeb) actualizar { it.copy(pensandoQue = "Pensando y buscando…") }
                        Result.success(IA.conversar(Llave(motor, claveDe(motor)), sistema, historia, 1200, listos = true, buscarWeb = forzarWeb || estado.value.bisaWeb))
                    }
                } catch (ex: Exception) {
                    Result.failure(ex)
                }
            }
            val msg = r.fold({ Asistente.interpretar(it) }, { MensajeChat("ia", "⚠ " + (it.message ?: "No pude hablar con la IA.")) })
            actualizar { it.copy(chat = chat + msg, pensando = false, pensandoQue = "", indice = if (msg.acciones.isNotEmpty()) 2 else 0) }
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

    private var pasoDeBisa: String? = null

    /** Las acciones sensibles al juego comprueban de nuevo el contexto al pulsarlas. */
    fun ejecutarAccion(a: AccionChat) {
        val e = estado.value
        val c = CentroBisa.contexto(e)
        if (e.pensando) return
        if (c.jugando && a.tipo in setOf("jugar", "ver", "ficha", "tema", "vista", "buscar", "menu",
                "bisa_biblioteca", "bisa_retomar", "bisa_sugerir", "bisa_logros", "bisa_motor", "bisa_apps")) {
            avisar("Esta acción está disponible al volver al inicio")
            return
        }
        when (a.tipo) {
            "bisa_biblioteca" -> abrirMenu(Seccion.BIBLIOTECA)
            "bisa_motor" -> abrirGaleria("ia")
            "bisa_apps" -> abrirApps()
            "bisa_sugerir" -> respuestaBisa(CentroBisa.sugerir(e))
            "bisa_retomar" -> ultimoJugado(e)?.takeUnless { e.datos[it.clave]?.oculto == true }
                ?.let { jugarDesdeWidget(it.clave) } ?: avisar("Aún no hay una partida reciente")
            "bisa_sistema" -> {
                val ayuda = CentroBisa.ayudaSistema(e, a.valor) ?: return
                val j = c.juego
                val consola = c.consola
                if (j == null || consola == null || consola.sinEmulador) respuestaBisa(ayuda)
                else scope.launch {
                    val elegido = withContext(Dispatchers.IO) {
                        runCatching { Lanzador.elegida(app, consola,
                            e.datos[j.clave]?.emulador ?: e.emuladores[consola.id], archivoParaJugar(j, e).tipo)?.nombre }.getOrNull()
                    }
                    val actual = estado.value
                    if (CentroBisa.contexto(actual).juego?.clave == j.clave && !actual.pensando) {
                        val detalle = elegido?.let { " Emulador seleccionado para abrirlo: $it." }
                            ?: " No encontré un emulador reconocido para ese archivo."
                        respuestaBisa(ayuda.copy(texto = ayuda.texto + detalle))
                    }
                }
            }
            "bisa_logros" -> if (c.juego?.clave == a.valor) abrirFichaDe(a.valor, 4)
            "bisa_pista" -> CentroBisa.prepararPista(e, a.valor)?.let { respuestaBisa(it) }
            "bisa_escribir" -> if (c.juego?.clave == a.valor) escribirAlAsistente()
            "bisa_pausa" -> if (c.jugando && c.juego?.clave == a.valor) {
                if (e.temporizadorFin > 0L) {
                    val minutos = ((e.temporizadorFin - System.currentTimeMillis()).coerceAtLeast(0L) + 59_999) / 60_000
                    respuestaBisa(MensajeChat("ia", "Tu aviso de pausa está activo. Faltan unos $minutos min."))
                } else alternarTemporizador()
            }
            "bisa_paso", "bisa_mapa" -> if (c.juego?.clave == a.valor && (!c.jugando || c.pantallaLibre)) {
                if (a.tipo == "bisa_mapa") abrirMapa(a.valor)
                else {
                    pasoDeBisa = a.valor
                    abrirTexto("bisa_paso", e.datos[a.valor]?.paso.orEmpty())
                }
            }
            "preguntar" -> preguntar(a.valor)
            "pista" -> preguntar(if (a.valor == "solucion") "Dime la solución completa, paso a paso." else "Dame una pista más directa.")
            "jugar", "ver", "ficha" -> {
                if (e.juegos.none { it.clave == a.valor } || e.datos[a.valor]?.oculto == true) { avisar("No encontré ese juego"); return }
                if (a.tipo == "jugar") jugarDesdeWidget(a.valor)
                else {
                    actualizar { it.copy(consolaSel = "todo", juegoSel = a.valor, panel = Panel.NINGUNO) }
                    if (a.tipo == "ficha") abrirFicha()
                }
            }
            "tema" -> if (todosLosTemas().any { it.id == a.valor }) elegirTema(a.valor) else avisar("No encontré ese tema")
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
        Opcion("Desconectar la cuenta", icono = Icono.BORRAR, apagada = e.usuarioRA.isBlank(), ayuda = "Saca tu cuenta de RetroAchievements de Díptico. Tus logros siguen en la página de RetroAchievements.") {
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
                        "Mis trucos y notas", d.notas.lineSequence().firstOrNull()?.take(40)?.ifEmpty { null } ?: "Escribir", Icono.LAPIZ, ayuda = "Tus apuntes de este juego: claves, trucos, lo que no quieres olvidar. Bisa también los lee.",
                    ) { abrirTexto("notas", d.notas) },
                )
                Fichas.enlacesGuias(j.titulo, consola?.bonito ?: "").forEach { (t, url) ->
                    add(Opcion(t, "Abre el navegador", Icono.WEB, ayuda = "Abre esta guía en el navegador.") { abrirWeb(url) })
                }
                e.ficha?.enlace?.let { url -> add(Opcion("Artículo completo en Wikipedia", icono = Icono.WEB, ayuda = "Abre el artículo de Wikipedia de este juego en el navegador.") { abrirWeb(url) }) }
            }
            3 -> listOf(
                Opcion("Título", d.titulo ?: j.titulo, Icono.TITULO, ayuda = "El nombre que se muestra en el estante y arriba. El archivo no se cambia.") { abrirTexto("titulo", d.titulo ?: j.titulo) },
                Opcion("Portada", if (d.portada != null) "Propia ✓" else "Elegir en la tienda", Icono.PORTADA, ayuda = "Elige otra portada en la tienda de imágenes o desde tus archivos.") { buscarCandidatos("portada") },
                Opcion("Imagen grande (hero)", if (d.arte != null) "Propia ✓" else "Elegir en la tienda", Icono.PORTADA, ayuda = "La imagen grande que se ve de fondo en la pantalla de arriba.") { buscarCandidatos("arte") },
                Opcion("Logo", if (d.logo != null) "Propio ✓" else "Elegir en la tienda", Icono.ESTRELLA, ayuda = "El logo que aparece sobre la imagen grande en vez del título escrito.") { buscarCandidatos("logo") },
                Opcion("Imagen en el inicio", nombreHeroe(e.heroes[j.clave]), Icono.PORTADA, "Qué imagen se ve en grande en el inicio para este juego.") { ciclarHeroe(j.clave) },
                Opcion("Encuadrar la portada", if (e.encuadres.containsKey("${j.clave}|portada")) "Ajustado ✓" else "Mover y acercar", Icono.PORTADA,
                    "Corre y acerca la foto para que quede centrada en la forma de la caja.") { abrirEncuadre("portada") },
                Opcion("Encuadrar la imagen grande", if (e.encuadres.containsKey("${j.clave}|arte")) "Ajustado ✓" else "Mover y acercar", Icono.PORTADA,
                    "Corre y acerca el hero para que se vea lo importante.") { abrirEncuadre("arte") },
                Opcion("Música del juego", if (d.musica != null) "Elegida ✓" else "Elegir canción", Icono.MUSICA, ayuda = "Una canción que suena en el estante cuando este juego está elegido.") { pedirArchivoPara("musica", "audio/*") },
                Opcion(if (d.favorito) "Quitar de favoritos" else "Marcar como favorito", icono = Icono.CORAZON, ayuda = "Los favoritos se ven primero con el filtro Favoritos.") { alternarFavorito(j.clave) },
                Opcion("Buscar todo automático", icono = Icono.DESCARGA, ayuda = "Busca en internet portada, imagen grande y logo para este juego, de una vez.") { descargarUno(j) },
                Opcion("Volver a lo original", "Quita lo personalizado", Icono.BORRAR, ayuda = "Quita el título, las imágenes, la música y los encuadres propios de este juego.") {
                    cambiarDatos(j.clave) { it.copy(titulo = null, portada = null, arte = null, logo = null, musica = null) }
                    guardarEncuadres(estado.value.encuadres - "${j.clave}|portada" - "${j.clave}|arte")
                    avisar("Listo, ${j.titulo} volvió a como era")
                },
            )
            else -> emptyList()
        }
    }

    /** Si la opción elegida del menú es un interruptor: su estado actual (true = encendido), o null si no lo es. */
    fun esInterruptor(e: Estado): Boolean? = when (opcionesSeccion(e.seccion).getOrNull(e.indice)?.valor) {
        "Sí", "Activado", "Encendido" -> true
        "No", "Desactivado", "Apagado" -> false
        else -> null
    }

    fun nombrePaqueteSonidos(p: String) = when (p) {
        "clasico" -> "Clásicos"; "bloques" -> "Bloques"; "retro" -> "Retro (8 bits)"; "burbuja" -> "Burbuja"; else -> "Los del tema"
    }

    private fun cambiarPaqueteSonidos(d: Int) {
        val l = Sonidos.PAQUETES
        val n = l[(l.indexOf(Sonidos.paquete) + d).mod(l.size)]
        Sonidos.paquete = n
        almacen.guardarTexto("paqueteSonidos", n)
        actualizar { it.copy(paqueteSonidos = n) }
        Sonidos.aceptar()
    }

    // ---------- Juegos de PC: GameHub, Winlator, GameNative, WinNative, DroidDeck… ----------

    /** Apps para juegos de PC en Android: nombre, paquetes conocidos, cómo se reconocen por su nombre y para qué sirve. */
    private data class LanzadorPC(val nombre: String, val paquetes: List<String>, val etiqueta: Regex, val ayuda: String)

    private val LANZADORES_PC = listOf(
        LanzadorPC("GameHub Lite", listOf("gamehub.lite", "emuready.gamehub.lite"), Regex("gamehub ?lite", RegexOption.IGNORE_CASE),
            "Juegos de Steam y locales. Díptico los abre directo con su número de Steam."),
        LanzadorPC("GameHub", listOf("com.xiaoji.egggame"), Regex("^gamehub$", RegexOption.IGNORE_CASE),
            "La app de GameSir para juegos de PC. Agrégala a tus Ports para abrirla desde el estante."),
        LanzadorPC("GameNative", listOf("app.gamenative"), Regex("gamenative", RegexOption.IGNORE_CASE),
            "Steam, Epic, GOG y Amazon. Díptico abre tus juegos de Steam directo con su número."),
        LanzadorPC("Winlator", listOf("com.winlator.cmod", "com.winlator", "com.cmodded.winlator", "com.winlator.vanilla"), Regex("winlator", RegexOption.IGNORE_CASE),
            "Juegos de Windows. En Winlator usa \"Exportar para frontend\" y deja los accesos en la carpeta \"windows\" de tus juegos."),
        LanzadorPC("WinNative", listOf("com.winnative.cmod"), Regex("winnative", RegexOption.IGNORE_CASE),
            "Accesos de Windows, Steam y Epic: déjalos en las carpetas \"windows\", \"steam\" o \"epic\" de tus juegos."),
        LanzadorPC("DroidDeck", emptyList(), Regex("droid ?deck", RegexOption.IGNORE_CASE),
            "Agrégala a tus Ports para abrirla desde el estante, con su ficha y tiempo jugado."),
        LanzadorPC("Steam Link", listOf("com.valvesoftware.steamlink"), Regex("steam ?link", RegexOption.IGNORE_CASE),
            "Juega lo de tu PC por Wi-Fi. Agrégala a tus Ports."),
        LanzadorPC("Moonlight", listOf("com.limelight", "com.limelight.root"), Regex("moonlight", RegexOption.IGNORE_CASE),
            "Juegos de tu PC por streaming. Agrégala a tus Ports."),
    )

    /** El paquete instalado de un lanzador de PC (por sus paquetes conocidos o por su nombre). */
    private fun paqueteLanzador(l: LanzadorPC, apps: List<App>): String? =
        l.paquetes.firstOrNull { Lanzador.instalado(app, it) } ?: apps.firstOrNull { l.etiqueta.containsMatchIn(it.nombre) }?.paquete

    private var appsCache: Pair<Long, List<App>>? = null
    private fun appsInstaladasRapido(): List<App> {
        val c = appsCache
        if (c != null && SystemClock.uptimeMillis() - c.first < 60_000) return c.second
        val l = try { Lanzador.apps(app) } catch (_: Exception) { emptyList() }
        appsCache = SystemClock.uptimeMillis() to l
        return l
    }

    private fun leerJuegosPC(t: String): List<JuegoPC> = t.split("\n").mapNotNull { linea ->
        val c = linea.split("\t")
        if (c.size < 3) null else c[1].toIntOrNull()?.let { JuegoPC(c[0], it, c[2]) }
    }

    private fun guardarJuegosPC(l: List<JuegoPC>) {
        almacen.guardarTexto("juegosPC", l.joinToString("\n") { "${it.titulo.replace("\t", " ")}\t${it.id}\t${it.tienda}" })
        actualizar { it.copy(juegosPC = l) }
        publicarJuegos()
    }

    /** Quita un juego de PC del estante (clave "steam/pc-<id>" o "windows/pc-<id>"). */
    private fun quitarJuegoPC(clave: String) {
        val id = clave.substringAfter("/pc-").toIntOrNull() ?: return
        guardarJuegosPC(estado.value.juegosPC.filter { it.id != id })
        avisar("Quitado de tu estante")
    }

    /** Cada juego de PC agregado es un archivito con su número (como los que exporta ES-DE), en la carpeta de Díptico. */
    private fun juegosDePC(): List<Juego> = estado.value.juegosPC.mapNotNull { g ->
        try {
            val consola = if (g.tienda == "STEAM") "steam" else "windows"
            val dir = java.io.File(app.filesDir, "pc/$consola").apply { mkdirs() }
            val nombre = g.titulo.replace(Regex("[\\\\/:*?\"<>|]"), "").trim().ifEmpty { "juego${g.id}" } + ".steam"
            val f = java.io.File(dir, nombre)
            if (!f.exists() || f.readText().trim() != g.id.toString()) f.writeText(g.id.toString())
            Juego("$consola/pc-${g.id}", consola, g.titulo, nombre, Uri.fromFile(f), "", null)
        } catch (_: Exception) { null }
    }

    /** Busca en la tienda de Steam (por nombre) para agregar un juego que abrirán GameNative o GameHub Lite. */
    fun buscarEnSteam(texto: String) {
        val q = texto.trim()
        if (q.isEmpty()) return
        avisar("Buscando \"$q\" en Steam…")
        scope.launch {
            val l = withContext(Dispatchers.IO) {
                try {
                    Descargas.buscarSteam(q).map { JuegoPC(it.nombre, it.id, "STEAM") }
                } catch (_: Exception) { null }
            }
            if (l == null) { avisar("No pude conectarme con Steam. Revisa el Wi-Fi."); return@launch }
            actualizar { it.copy(resultadosSteam = l) }
            if (l.isEmpty()) avisar("No encontré \"$q\" en Steam") else {
                abrirMenu(Seccion.BIBLIOTECA)
                val i = opcionesSeccion(Seccion.BIBLIOTECA).indexOfFirst { it.titulo.startsWith("Agregar: ") }
                actualizar { it.copy(enOpciones = true, indice = i.coerceAtLeast(0)) }
            }
        }
    }

    private fun opcionesPC(e: Estado): List<Opcion> = buildList {
        val apps = appsInstaladasRapido()
        add(Opcion("Agregar un juego de Steam", icono = Icono.BUSCAR, ayuda = "Escribe su nombre: Díptico lo busca en Steam y lo abre con GameNative o GameHub Lite (elige cuál en Emuladores › Valve Steam).") { abrirTexto("pc_steam") })
        for (g in e.resultadosSteam) add(Opcion("Agregar: ${g.titulo}", "Steam ${g.id}", Icono.DESCARGA, "Queda en tu estante de Steam, con portada, ficha y tiempo jugado.") {
            if (estado.value.juegosPC.none { it.id == g.id }) guardarJuegosPC(estado.value.juegosPC + g)
            actualizar { it.copy(resultadosSteam = emptyList()) }
            avisar("${g.titulo} agregado a tu estante de Steam")
            descargarFaltantesLuego()
        })
        for (g in e.juegosPC) add(Opcion(g.titulo, "Quitar", Icono.BORRAR, "Juego de PC agregado desde Díptico (Steam ${g.id}). Elige para quitarlo de tu estante.") {
            guardarJuegosPC(estado.value.juegosPC.filter { it.id != g.id })
            avisar("${g.titulo} quitado")
        })
        for (l in LANZADORES_PC) {
            val pkg = paqueteLanzador(l, apps)
            if (pkg == null) {
                add(Opcion(l.nombre, "No instalado", Icono.MOVIL, l.ayuda, apagada = false) { avisar("${l.nombre} no está instalado") })
                continue
            }
            val enPorts = pkg in e.ports
            add(Opcion(l.nombre, if (enPorts) "En tus Ports ✓" else "Instalado", Icono.MOVIL, l.ayuda, paquete = pkg) {
                if (!enPorts) {
                    val nuevo = estado.value.ports + pkg
                    almacen.ports = nuevo
                    actualizar { it.copy(ports = nuevo) }
                    publicarJuegos()
                    avisar("${l.nombre} agregado a tus Ports")
                } else abrirPaquete(pkg)
            })
        }
    }

    /** Después de agregar juegos de PC, busca sus portadas (si hay conexión). */
    private fun descargarFaltantesLuego() {
        scope.launch {
            delay(600)
            estado.value.juegos.filter { it.clave.contains("/pc-") && it.portada == null }.forEach { descargarUno(it) }
        }
    }

    // ---------- Rendimiento: modo turbo ----------

    /** Paquetes que el turbo nunca cierra: emuladores, la música que suena y el juego mismo (si es una app). */
    private fun protegidosTurbo(juego: Juego?): Set<String> = buildSet {
        addAll(Consolas.reglas.values.flatten().map { it.substringBefore('/') })
        cl.nikalbazu.diptico.audio.Medios.sonando.value?.paquete?.let { add(it) }
        addAll(estado.value.ports); addAll(estado.value.android)
        if (juego?.uri?.scheme == "app") add(juego.archivo)
    }

    private fun turboAlJugar(juego: Juego, displayId: Int) {
        val e = estado.value
        scope.launch {
            val r = withContext(Dispatchers.IO) { try { Turbo.optimizar(app, protegidosTurbo(juego), e.turboCerrarApps) } catch (_: Exception) { null } } ?: return@launch
            val resumen = "${r.libresMb} MB libres" + if (r.liberadosMb > 0) " (+${r.liberadosMb} MB)" else ""
            actualizar { it.copy(turboUltimo = resumen) }
            delay(1200)
            if (estado.value.sesion != null) cl.nikalbazu.diptico.bisa.Cartel.mostrar(app, displayId, "Modo turbo", juego.titulo, resumen + if (r.apps > 0) " · ${r.apps} apps en reposo" else "")
        }
    }

    /** Mientras juegas, Díptico se queda quieto (sin animaciones ni difuminados) y, si lo pediste, No molestar. */
    private fun vigilarReposo() {
        scope.launch {
            estado.map { it.sesion != null }.distinctUntilChanged().collect { jugando ->
                val e = estado.value
                aplicarFluidez()
                if (e.turbo && e.turboNoMolestar) Turbo.noMolestar(app, jugando)
            }
        }
    }

    fun optimizarAhora() {
        avisar("Optimizando…")
        scope.launch {
            val r = withContext(Dispatchers.IO) { try { Turbo.optimizar(app, protegidosTurbo(null), estado.value.turboCerrarApps) } catch (_: Exception) { null } }
            if (r == null) { avisar("No pude optimizar ahora"); return@launch }
            val resumen = "${r.libresMb} MB libres" + if (r.liberadosMb > 0) " (+${r.liberadosMb} MB)" else ""
            actualizar { it.copy(turboUltimo = resumen) }
            Sonidos.aviso()
            avisar("Listo: $resumen" + if (r.apps > 0) " · ${r.apps} apps en reposo" else "")
        }
    }

    private fun opcionesTurbo(e: Estado): List<Opcion> {
        fun alternar(clave: String, f: (Estado) -> Boolean, g: (Estado, Boolean) -> Estado) {
            val v = !f(estado.value)
            almacen.guardarBool(clave, v)
            actualizar { g(it, v) }
            Sonidos.interruptor(v)
        }
        val libres = Turbo.memoriaLibreMb(app)
        val total = Turbo.memoriaTotalMb(app)
        return listOf(
            Opcion("Modo turbo al jugar", if (e.turbo) "Sí" else "No", Icono.CHISPA,
                "Antes de abrir cada juego, Díptico libera memoria y se pone en reposo para que el juego tenga toda la consola.") {
                alternar("turbo", { it.turbo }) { s0, v -> s0.copy(turbo = v) }
            },
            Opcion("Cerrar apps en segundo plano", if (e.turboCerrarApps) "Sí" else "No", Icono.APPS,
                "Duerme las apps que quedaron abiertas de fondo. Nunca toca emuladores, la música que suena, el teclado ni las apps del sistema.",
                apagada = !e.turbo) { alternar("turboCerrarApps", { it.turboCerrarApps }) { s0, v -> s0.copy(turboCerrarApps = v) } },
            Opcion("Díptico en reposo mientras juegas", if (e.turboReposo) "Sí" else "No", Icono.NEGRO,
                "Sin animaciones, decoraciones ni difuminados mientras el juego está abierto: menos trabajo para la consola.",
                apagada = !e.turbo) { alternar("turboReposo", { it.turboReposo }) { s0, v -> s0.copy(turboReposo = v) } },
            Opcion("No molestar mientras juegas", if (!Turbo.puedeNoMolestar(app) && e.turboNoMolestar) "Falta permiso" else if (e.turboNoMolestar) "Sí" else "No", Icono.CAMPANA,
                "Silencia las notificaciones (deja pasar alarmas y prioritarias) y las devuelve como estaban al terminar. Android pide un permiso.",
                apagada = !e.turbo) {
                if (!Turbo.puedeNoMolestar(app)) { Turbo.pedirNoMolestar(app); avisar("Activa Díptico en la lista y vuelve") }
                alternar("turboNoMolestar", { it.turboNoMolestar }) { s0, v -> s0.copy(turboNoMolestar = v) }
            },
            Opcion("Optimizar ahora", e.turboUltimo.ifBlank { "Liberar memoria" }, Icono.DESCARGA, "Hace lo mismo que el turbo, ahora mismo.") { optimizarAhora() },
            Opcion("Memoria disponible", "$libres de $total MB", Icono.INFO, "Cuánta memoria tiene libre la consola en este momento.") {},
        )
    }

    // ---------- Modo sobremesa (tele) ----------

    private val NOMBRE_TELE = Regex("hdmi|displayport|\\bdp\\b|external|externa|usb|\\btv\\b|wireless|cast|miracast", RegexOption.IGNORE_CASE)

    /** Batería baja (15 % o menos, sin cargar) mientras navegas: un aviso una sola vez por carga. */
    private fun vigilarBateria() {
        scope.launch {
            var avisado = false
            while (true) {
                delay(60_000)
                val b = try { infoBateria() } catch (_: Exception) { continue }
                val cargando = b.estado == "Cargando" || b.estado == "Llena"
                if (cargando || b.nivel > 20) avisado = false
                if (!avisado && !cargando && b.nivel in 1..15 && estado.value.sesion == null && estado.value.aLaVista > 0) {
                    avisado = true
                    avisar("Te queda ${b.nivel}% de batería: buen momento para guardar y enchufar")
                    Sonidos.aviso()
                }
            }
        }
    }

    /** Mira si hay una tele conectada (HDMI, USB-C, inalámbrica) y avisa al conectar o desconectar. */
    private fun vigilarTele() {
        val dm = app.getSystemService(android.hardware.display.DisplayManager::class.java) ?: return
        fun revisar(avisarCambio: Boolean) {
            revisarPantallas()
            val tele = buscarTele(dm)
            val antes = estado.value.teleConectada
            if (tele == antes) return
            actualizar { it.copy(teleConectada = tele) }
            aplicarSobremesa()
            if (avisarCambio && estado.value.sobremesaModo == "auto") {
                if (tele != null) { avisar("Tele conectada: modo sobremesa activado"); Sonidos.aviso() } else avisar("Volviste a la consola: modo portátil")
            }
        }
        revisar(false)
        dm.registerDisplayListener(object : android.hardware.display.DisplayManager.DisplayListener {
            override fun onDisplayAdded(id: Int) = revisar(true)
            override fun onDisplayRemoved(id: Int) = revisar(true)
            override fun onDisplayChanged(id: Int) = revisar(true)
        }, android.os.Handler(android.os.Looper.getMainLooper()))
    }

    /**
     * La tele, si hay: una pantalla que no es la principal y que por su nombre es externa (HDMI, DP, USB, inalámbrica),
     * o una tercera pantalla (la Thor ya trae dos).
     */
    private fun buscarTele(dm: android.hardware.display.DisplayManager): String? {
        val otras = dm.displays.filter { it.displayId != android.view.Display.DEFAULT_DISPLAY && it.state != android.view.Display.STATE_OFF }
        if (otras.isEmpty()) return null
        // Con una sola pantalla extra en una consola de dos pantallas (la Thor), esa es la de abajo, no una tele.
        val e = estado.value
        if (otras.size == 1 && e.modoPantallas == "dos" && e.dosDisponibles) return null
        otras.firstOrNull { NOMBRE_TELE.containsMatchIn(it.name ?: "") }?.let { return it.name }
        return if (otras.size >= 2) otras.last().name else null
    }

    /** Todas las pantallas que ve Android (para el ajuste, por si la tele no se detecta sola). */
    fun pantallasVistas(): String = try {
        app.getSystemService(android.hardware.display.DisplayManager::class.java)?.displays
            ?.joinToString(" · ") { "${it.displayId}: ${it.name}" } ?: ""
    } catch (_: Exception) { "" }

    /** Aplica lo que cambia en la tele: efectos y volumen de los sonidos. */
    private fun aplicarSobremesa() {
        val e = estado.value
        Sonidos.volumen = if (e.sobremesa && e.sobremesaSonidos) 0.9f else 0.6f
        aplicarFluidez()
    }

    private fun guardarSobremesa(clave: String, valor: String, f: (Estado) -> Estado) {
        almacen.guardarTexto(clave, valor)
        actualizar(f)
        aplicarSobremesa()
    }

    private fun opcionesSobremesa(e: Estado): List<Opcion> {
        val modos = listOf("auto", "si", "no")
        fun nombreModo(m: String) = when (m) { "si" -> "Siempre"; "no" -> "Nunca"; else -> "Automático" }
        val tams = listOf(1f, 1.1f, 1.2f, 1.35f, 1.5f)
        fun nombreTam(t: Float) = when { t <= 1f -> "Normal"; t <= 1.1f -> "Un poco más grande"; t <= 1.2f -> "Grande"; t <= 1.35f -> "Muy grande"; else -> "Enorme" }
        val margenes = listOf(0, 2, 3, 5, 8)
        return listOf(
            Opcion(
                "Cuándo se activa", nombreModo(e.sobremesaModo), Icono.VISTA,
                "Al conectar la tele, Díptico se acomoda solo. Si tu tele no se detecta, elige \"Siempre\" mientras juegas en ella.",
                menos = { val v = modos[(modos.indexOf(estado.value.sobremesaModo) - 1).mod(modos.size)]; guardarSobremesa("sobremesaModo", v) { it.copy(sobremesaModo = v) } },
            ) { val v = modos[(modos.indexOf(estado.value.sobremesaModo) + 1).mod(modos.size)]; guardarSobremesa("sobremesaModo", v) { it.copy(sobremesaModo = v) } },
            Opcion(
                "Tele conectada", e.teleConectada ?: "Ninguna", Icono.INFO,
                Idioma.t("Pantallas que veo: {0}").replace("{0}", pantallasVistas().ifBlank { Idioma.t("solo la principal") }) + (if (e.sobremesa) " · " + Idioma.t("Activo ahora") else ""),
            ) {},
            Opcion(
                "Tamaño en la tele", nombreTam(e.sobremesaTam), Icono.TEXTO, "Todo más grande, para verlo cómodo desde el sillón.",
                menos = { val v = tams[(tams.indexOfFirst { it >= estado.value.sobremesaTam - 0.01f }.coerceAtLeast(0) - 1).coerceAtLeast(0)]; guardarSobremesa("sobremesaTam", v.toString()) { it.copy(sobremesaTam = v) } },
            ) { val v = tams[(tams.indexOfFirst { it >= estado.value.sobremesaTam - 0.01f }.coerceAtLeast(0) + 1).mod(tams.size)]; guardarSobremesa("sobremesaTam", v.toString()) { it.copy(sobremesaTam = v) } },
            Opcion(
                "Margen de seguridad", if (e.sobremesaMargen == 0) "Sin margen" else "${e.sobremesaMargen}%", Icono.FORMA,
                "Algunas teles cortan los bordes de la imagen. Sube el margen si no ves la hora o los botones de las orillas.",
                menos = { val v = margenes[(margenes.indexOf(estado.value.sobremesaMargen).coerceAtLeast(0) - 1).coerceAtLeast(0)]; guardarSobremesa("sobremesaMargen", v.toString()) { it.copy(sobremesaMargen = v) } },
            ) { val v = margenes[(margenes.indexOf(estado.value.sobremesaMargen).coerceAtLeast(0) + 1).mod(margenes.size)]; guardarSobremesa("sobremesaMargen", v.toString()) { it.copy(sobremesaMargen = v) } },
            Opcion(
                "Una sola pantalla en la tele", if (e.sobremesaUna) "Sí" else "No", Icono.UNA,
                "Todo Díptico en la tele (como en una Odin), y la pantalla de abajo de la consola se apaga.",
            ) { val v = !estado.value.sobremesaUna; almacen.guardarBool("sobremesaUna", v); actualizar { it.copy(sobremesaUna = v) } },
            Opcion(
                "Todos los efectos en la tele", if (e.sobremesaEfectos) "Sí" else "No", Icono.CHISPA,
                "Enchufada a la corriente, la consola puede con los difuminados y animaciones aunque uses el modo ahorro.",
            ) { val v = !estado.value.sobremesaEfectos; almacen.guardarBool("sobremesaEfectos", v); actualizar { it.copy(sobremesaEfectos = v) }; aplicarSobremesa() },
            Opcion(
                "Sonidos más fuertes", if (e.sobremesaSonidos) "Sí" else "No", Icono.VOLUMEN,
                "Los sonidos al moverte suenan más, porque la tele queda lejos.",
            ) { val v = !estado.value.sobremesaSonidos; almacen.guardarBool("sobremesaSonidos", v); actualizar { it.copy(sobremesaSonidos = v) }; aplicarSobremesa(); Sonidos.aceptar() },
        )
    }

    // ---------- Botones extra (L4, R4…) ----------

    private fun leerBotonesExtra(t: String): List<BotonExtra> {
        if (t.isBlank()) return BotonExtra.PORDEFECTO
        return t.split(";").mapNotNull { p ->
            val c = p.split(",")
            if (c.size < 3) null else BotonExtra(c[0], c[1].toIntOrNull() ?: return@mapNotNull null, c[2], c.getOrNull(3) == "start")
        }.ifEmpty { BotonExtra.PORDEFECTO }
    }

    private fun guardarBotonesExtra(l: List<BotonExtra>) {
        almacen.guardarTexto("botonesExtra", l.joinToString(";") { "${it.nombre},${it.tecla},${it.accion},${if (it.conStart) "start" else "solo"}" })
        actualizar { it.copy(botonesExtra = l) }
    }

    private fun cambiarAccionBoton(i: Int, d: Int) {
        val l = estado.value.botonesExtra.toMutableList()
        val b = l.getOrNull(i) ?: return
        val a = BotonExtra.ACCIONES
        l[i] = b.copy(accion = a[(a.indexOf(b.accion).coerceAtLeast(0) + d).mod(a.size)])
        guardarBotonesExtra(l)
    }

    /** Espera a que aprietes el botón para aprender cuál es (sirve para cualquier control). */
    fun aprenderBoton(i: Int) {
        actualizar { it.copy(aprendiendoBoton = i) }
        avisar("Aprieta el botón ${estado.value.botonesExtra.getOrNull(i)?.nombre ?: ""} ahora")
    }

    /** Lo llama el control mientras se aprende un botón: guarda la tecla y vuelve. */
    fun aprendido(tecla: Int) {
        val e = estado.value
        val i = e.aprendiendoBoton
        if (i < 0) return
        val l = e.botonesExtra.toMutableList()
        if (i >= l.size) return
        if (tecla == android.view.KeyEvent.KEYCODE_BUTTON_START || (l[i].conStart && tecla in teclasCombinadasReservadas(e))) {
            avisar("Esa tecla ya está reservada. Prueba otra o cambia el atajo que la usa.")
            return
        }
        for (k in l.indices) {
            if (k != i && l[k].tecla == tecla && l[k].conStart == l[i].conStart) l[k] = l[k].copy(tecla = 0)
        }
        l[i] = l[i].copy(tecla = tecla)
        actualizar { it.copy(aprendiendoBoton = -1) }
        guardarBotonesExtra(l)
        Sonidos.interruptor(true)
        avisar("Listo: ${l[i].nombre} aprendido (${android.view.KeyEvent.keyCodeToString(tecla).removePrefix("KEYCODE_")})")
    }

    fun cancelarAprender() = actualizar { it.copy(aprendiendoBoton = -1) }

    private fun teclasCombinadasReservadas(e: Estado) =
        listOf(e.atajoBisa, e.atajoApps).mapNotNull { AtajoInicio.leer(it, AtajoInicio.NO).takeUnless { a -> a.doble }?.tecla }

    private fun alternarModoBoton(i: Int) {
        val e = estado.value
        val botones = e.botonesExtra.toMutableList()
        val b = botones.getOrNull(i) ?: return
        val conStart = !b.conStart
        val ocupada = botones.withIndex().any { (k, otro) -> k != i && otro.tecla == b.tecla && otro.conStart == conStart }
        val tecla = b.tecla.takeUnless { ocupada || (conStart && it in teclasCombinadasReservadas(e)) } ?: 0
        botones[i] = b.copy(conStart = conStart, tecla = tecla)
        guardarBotonesExtra(botones)
        if (b.tecla != 0 && tecla == 0) avisar("La combinación ya está en uso. Elige otro botón.")
    }

    /** Si [tecla] es un botón extra con acción, la ejecuta. */
    fun botonExtra(tecla: Int, conStart: Boolean = false): Boolean {
        val b = estado.value.botonesExtra.firstOrNull { it.tecla == tecla && it.tecla != 0 && it.conStart == conStart } ?: return false
        if (b.accion == "nada") return false
        val e = estado.value
        when (b.accion) {
            "bisa" -> if (e.sesion != null) prepararAyuda() else abrirAsistente()
            "musica" -> if (cl.nikalbazu.diptico.audio.Medios.sonando.value != null) cl.nikalbazu.diptico.audio.Medios.alternar() else fijarMusica(!e.musica)
            "siguiente" -> cl.nikalbazu.diptico.audio.Medios.siguiente()
            "apps" -> abrirApps()
            "buscar" -> abrirBuscar()
            "azar" -> sorprendeme()
            "widgets" -> alternarInicio()
            "vista" -> cambiarVista()
            "centro" -> abrirCentro()
            "favorito" -> e.seleccionado?.let { alternarFavorito(it.clave) }
            "ficha" -> abrirFicha()
            "intercambiar" -> intercambiar()
            "menu" -> abrirMenu()
        }
        return true
    }

    private fun opcionesBotones(e: Estado): List<Opcion> = buildList {
        fun opcionAtajo(bisa: Boolean): Opcion {
            val clave = if (bisa) "atajoBisa" else "atajoApps"
            val titulo = if (bisa) "Atajo de Bisa" else "Atajo del cajón de apps"
            val actual = if (bisa) e.atajoBisa else e.atajoApps
            fun cambiar(delta: Int) {
                val actualEstado = estado.value
                val otro = if (bisa) actualEstado.atajoApps else actualEstado.atajoBisa
                val ahora = if (bisa) actualEstado.atajoBisa else actualEstado.atajoApps
                val personalizadas = actualEstado.botonesExtra.filter { it.conStart }.map { it.tecla }.toSet()
                val opciones = AtajoInicio.entries.filter {
                    it == AtajoInicio.NO || (it.id != otro && (it.doble || it.tecla !in personalizadas))
                }
                val i = opciones.indexOfFirst { it.id == ahora }
                val nuevo = opciones[(i + delta).mod(opciones.size)]
                almacen.guardarTexto(clave, nuevo.id)
                actualizar { it.copy(navegacion = if (bisa) it.navegacion.copy(atajoBisa = nuevo.id)
                    else it.navegacion.copy(atajoApps = nuevo.id)) }
            }
            return Opcion(titulo, AtajoInicio.leer(actual, AtajoInicio.NO).titulo,
                if (bisa) Icono.BISA else Icono.APPS,
                "Elige con A o ← →. Funciona en Inicio. Si eliges Doble Start, Ajustes espera un instante para distinguir los dos toques.",
                menos = { cambiar(-1) }) { cambiar(1) }
        }
        add(opcionAtajo(true))
        add(opcionAtajo(false))
        val modos = listOf("dividido", "invertido", "clasico")
        fun cambiarSticks(d: Int) {
            val v = modos[(modos.indexOf(estado.value.sticks).coerceAtLeast(0) + d).mod(modos.size)]
            almacen.guardarTexto("sticks", v)
            actualizar { it.copy(sticks = v) }
        }
        add(Opcion("Sticks", nombreSticks(e.sticks), Icono.MANDO,
            "Dividido: el stick izquierdo elige juegos y el derecho recorre los widgets. Invertido: al revés. Clásico: el izquierdo es la cruceta y el derecho cambia de consola. En el editor de widgets, el stick de los widgets desliza la pantalla.",
            menos = { cambiarSticks(-1) }) { cambiarSticks(1) })
        e.botonesExtra.forEachIndexed { i, b ->
            add(
                Opcion(
                    "Botón ${b.nombre}", BotonExtra.nombreAccion(b.accion), Icono.MANDO,
                    "Qué hace ${b.nombre} en Díptico (← → para cambiar). Dentro de un juego el botón es del juego.",
                    menos = { cambiarAccionBoton(i, -1) },
                ) { cambiarAccionBoton(i, 1) },
            )
            add(Opcion("Cómo activar ${b.nombre}", if (b.conStart) "Start + botón" else "Un botón", Icono.MANDO,
                "Con Start, mantén Start y pulsa el botón aprendido. Las combinaciones funcionan en Inicio.") { alternarModoBoton(i) })
            add(
                Opcion(
                    "Aprender el botón ${b.nombre}", if (e.aprendiendoBoton == i) "Aprieta el botón…" else if (b.tecla == 0) "Sin aprender" else android.view.KeyEvent.keyCodeToString(b.tecla).removePrefix("KEYCODE_").lowercase(),
                    Icono.LAPIZ, "Elige esto y aprieta el botón en tu control: así funciona con la Odin 2, la Retroid, la Thor o el control que sea.",
                ) { aprenderBoton(i) },
            )
        }
        if (e.botonesExtra.size < 4) add(Opcion("Agregar un acceso personalizado", icono = Icono.MANDO, ayuda = "Elige un botón, asígnale una acción y decide si quieres combinarlo con Start.") {
            val l = estado.value.botonesExtra + BotonExtra("Extra ${estado.value.botonesExtra.size - 1}", 0, "nada")
            guardarBotonesExtra(l)
            aprenderBoton(l.size - 1)
        })
        add(Opcion("Volver a lo original", icono = Icono.BORRAR, ayuda = "L4 = Bisa y R4 = música, con las teclas de siempre.") { guardarBotonesExtra(BotonExtra.PORDEFECTO) })
    }

    // ---------- Widgets con vida: foco con la cruceta y contenido al abrirlos ----------

    /** Widgets visibles de la página actual del inicio (índices de la lista). */
    private fun widgetsDePagina(e: Estado = estado.value) = e.widgets.indices.filter { e.widgets[it].visible && e.widgets[it].pagina == e.paginaInicio }

    /** Entra a los widgets con la cruceta (desde el estante, con ↑): el foco va al de más abajo a la izquierda. */
    fun entrarWidgets(): Boolean {
        val e = estado.value
        if (e.inicio != "widgets" || e.editandoWidgets || e.eligiendoConsola) return false
        val l = widgetsDePagina(e)
        if (l.isEmpty()) return false
        val i = l.maxWithOrNull(compareBy<Int>({ e.widgets[it].y + e.widgets[it].al }, { -e.widgets[it].x })) ?: return false
        actualizar { it.copy(widgetFoco = i) }
        Sonidos.levantar()
        return true
    }

    fun salirWidgets() {
        if (estado.value.widgetFoco < 0) return
        actualizar { it.copy(widgetFoco = -1) }
        Sonidos.soltar()
    }

    /** Mueve el foco al widget más cercano en esa dirección. Abajo del todo vuelve al estante; a los lados cambia de página. */
    fun moverFocoWidget(dx: Int, dy: Int) {
        val e = estado.value
        val w = e.widgets.getOrNull(e.widgetFoco) ?: return
        val siguiente = vecinoWidget(e, e.widgetFoco, dx, dy)
        when {
            siguiente != null -> actualizar { it.copy(widgetFoco = siguiente) }
            dy > 0 -> salirWidgets()
            dx != 0 && e.paginas > 1 && e.paginaInicio + dx in 0 until e.paginas -> {
                irAPagina(e.paginaInicio + dx)
                val l = widgetsDePagina()
                val f = (if (dx > 0) l.minByOrNull { estado.value.widgets[it].x * 10 + kotlin.math.abs(estado.value.widgets[it].y - w.y) }
                else l.maxByOrNull { estado.value.widgets[it].x * 10 - kotlin.math.abs(estado.value.widgets[it].y - w.y) })
                actualizar { it.copy(widgetFoco = f ?: -1) }
                Sonidos.pagina()
            }
            else -> Sonidos.borde()
        }
    }

    /** Con el foco en los widgets, L2/R2 cambian de página y el foco pasa al primero de la nueva. */
    fun paginaFocoWidget(d: Int) {
        val e = estado.value
        val n = e.paginaInicio + d
        if (n !in 0 until e.paginas) { Sonidos.borde(); return }
        irAPagina(n)
        val l = widgetsDePagina()
        actualizar { it.copy(widgetFoco = l.minByOrNull { k -> it.widgets[k].y * 100 + it.widgets[k].x } ?: -1) }
        Sonidos.pagina()
    }

    /** Abre el widget: unos van directo a su lugar (ficha, estadísticas, Bisa…) y el resto muestra su propio contenido. */
    fun abrirWidget(i: Int = estado.value.widgetFoco) {
        val e = estado.value
        val w = e.widgets.getOrNull(i) ?: return
        when (w.tipo) {
            "juego" -> { abrirFicha(); return }
            "paso" -> { abrirQuedaste(); return }
            "accesos" -> { abrirCentro(); return }
            "apps" -> { abrirApps(); return }
            "buscar" -> { abrirBuscar(); return }
        }
        // Acceso directo: juega al tiro.
        if (w.tipo.startsWith("atajo-")) {
            val clave = w.tipo.removePrefix("atajo-")
            if (e.juegos.none { it.clave == clave }) { avisar("Ese juego ya no está en tu biblioteca"); return }
            elegirConsola("todo"); elegirJuego(clave); jugar(); return
        }
        if (w.tipo == "sugerencia" && e.sugeridas.isEmpty()) nuevasSugerencias()
        actualizar { it.copy(panel = Panel.WIDGET, widgetAbierto = i, widgetFoco = i, indice = 0) }
    }

    /** ← → dentro de un widget abierto: cambia las opciones con valor (minutos, pausas…). */
    fun ladoWidget(d: Int) {
        val o = opcionesWidget().getOrNull(estado.value.indice) ?: return
        if (o.menos == null) return
        if (d < 0) o.menos.invoke() else o.accion()
    }

    /** Tres juegos para descubrir (primero los que nunca has jugado). */
    fun nuevasSugerencias() {
        val e = estado.value
        val sinJugar = e.juegos.filter { (e.datos[it.clave]?.tiempo ?: 0L) == 0L }
        val l = (sinJugar.shuffled() + e.juegos.shuffled()).distinctBy { it.clave }.filter { it.clave !in e.sugeridas }.take(3)
            .ifEmpty { e.juegos.shuffled().take(3) }
        actualizar { it.copy(sugeridas = l.map { j -> j.clave }) }
    }

    fun jugarDesdeWidget(clave: String) {
        cerrarPanel()
        actualizar { it.copy(widgetFoco = -1) }
        elegirConsola("todo")
        elegirJuego(clave)
        jugar()
    }

    private val MINUTOS_TEMPORIZADOR = listOf(1, 3, 5, 10, 15, 20, 25, 30, 45, 60, 90, 120)
    private val PAUSAS = listOf(0, 30, 45, 60, 90, 120)

    private fun cambiarTemporizador(d: Int) {
        val l = MINUTOS_TEMPORIZADOR
        val i = l.indexOfFirst { it >= estado.value.temporizadorMin }.coerceAtLeast(0)
        val v = l[(i + d).coerceIn(0, l.size - 1)]
        almacen.guardarTexto("temporizadorMin", v.toString())
        actualizar { it.copy(temporizadorMin = v) }
    }

    fun alternarTemporizador() {
        val e = estado.value
        if (e.temporizadorFin > 0L) {
            actualizar { it.copy(temporizadorFin = 0L) }
            Sonidos.interruptor(false)
            return
        }
        val fin = System.currentTimeMillis() + e.temporizadorMin * 60_000L
        actualizar { it.copy(temporizadorFin = fin) }
        Sonidos.interruptor(true)
        avisar("Temporizador de ${e.temporizadorMin} min. Te aviso aunque estés jugando.")
        scope.launch {
            while (estado.value.temporizadorFin == fin && System.currentTimeMillis() < fin) delay(1000)
            if (estado.value.temporizadorFin != fin) return@launch
            actualizar { it.copy(temporizadorFin = 0L) }
            Sonidos.aviso()
            val s = estado.value.sesion
            if (s != null) cl.nikalbazu.diptico.bisa.Cartel.mostrar(app, s.displayId, "Temporizador", "¡Se acabó el tiempo!", "${e.temporizadorMin} min")
            else avisar("¡Se acabó el tiempo! (${e.temporizadorMin} min)")
        }
    }

    fun alternarCrono() {
        val e = estado.value
        val ahora = System.currentTimeMillis()
        if (e.cronoInicio > 0L) actualizar { it.copy(cronoInicio = 0L, cronoAcumulado = it.cronoAcumulado + (ahora - it.cronoInicio)) }
        else actualizar { it.copy(cronoInicio = ahora) }
        Sonidos.interruptor(e.cronoInicio == 0L)
    }

    fun reiniciarCrono() = actualizar { it.copy(cronoInicio = 0L, cronoAcumulado = 0L) }

    private fun cambiarPausa(d: Int) {
        val l = PAUSAS
        val v = l[(l.indexOf(estado.value.pausaCada).coerceAtLeast(0) + d).mod(l.size)]
        almacen.guardarTexto("pausaCada", v.toString())
        actualizar { it.copy(pausaCada = v) }
    }

    /** Mientras juegas: cada [Estado.pausaCada] minutos un cartel amable para descansar la vista y estirarse. */
    private fun vigilarPausas() {
        scope.launch {
            var avisadas = 0L
            var sesion = 0L
            while (true) {
                delay(30_000)
                val e = estado.value
                val s = e.sesion
                if (s == null || e.pausaCada <= 0) { avisadas = 0; continue }
                if (s.inicio != sesion) { sesion = s.inicio; avisadas = 0 }
                val minutos = (System.currentTimeMillis() - s.inicio) / 60_000
                val toca = minutos / e.pausaCada
                if (toca > avisadas) {
                    avisadas = toca
                    cl.nikalbazu.diptico.bisa.Cartel.mostrar(
                        app, s.displayId, "Bisa", "Llevas $minutos min jugando",
                        listOf("¿Una pausa? Mira a lo lejos unos segundos.", "Estira las manos y toma agua.", "Buen momento para guardar la partida.").random(),
                    )
                }
            }
        }
    }

    fun elegirVistaPartida(clave: String, vista: VistaPartida) {
        val datos = estado.value.datos.toMutableMap()
        datos[clave] = (datos[clave] ?: DatosJuego()).copy(vistaPartida = vista.name)
        almacen.guardarDatos(datos)
        actualizar { it.copy(datos = datos) }
        if (estado.value.sesion != null) despertar()
    }

    fun cambiarAvance(clave: String) {
        cambiarDatos(clave) { d ->
            val actual = AvanceJuego.leer(d.avance)
            val siguiente = AvanceJuego.entries[(actual.ordinal + 1) % AvanceJuego.entries.size]
            d.copy(avance = siguiente.name, puestoCola = d.puestoCola ?: System.currentTimeMillis())
        }
    }

    fun priorizarJuego(clave: String) {
        val primero = estado.value.datos.values.mapNotNull { it.puestoCola }.minOrNull() ?: 0L
        cambiarDatos(clave) { it.copy(puestoCola = (primero.coerceAtLeast(Long.MIN_VALUE + 1)) - 1) }
        avisar("Juego al comienzo de la cola")
    }

    private fun opcionesTemporizador(e: Estado) = listOf(
        Opcion("Temporizador", "${e.temporizadorMin} min", Icono.RELOJ, "Elige los minutos con ← →. Te aviso con un cartel aunque estés jugando.", menos = { cambiarTemporizador(-1) }) { cambiarTemporizador(1) },
        Opcion(if (e.temporizadorFin > 0L) "Detener el temporizador" else "Empezar el temporizador", icono = if (e.temporizadorFin > 0L) Icono.CERRAR else Icono.JUGAR, ayuda = "Un temporizador para avisarte cuando sea hora de dejar de jugar.") { alternarTemporizador() },
    )

    private fun opcionesCronometro(e: Estado) = listOf(
        Opcion(if (e.cronoInicio > 0L) "Pausar el cronómetro" else if (e.cronoAcumulado > 0L) "Seguir el cronómetro" else "Empezar el cronómetro", icono = Icono.RELOJ, ayuda = "Para medir una partida, un speedrun o lo que quieras.") { alternarCrono() },
        Opcion("Reiniciar el cronómetro", icono = Icono.BORRAR, apagada = e.cronoInicio == 0L && e.cronoAcumulado == 0L, ayuda = "Vuelve el cronómetro a cero.") { reiniciarCrono() },
    )

    /** Lo que se puede hacer dentro de cada widget abierto (la lista del panel). */
    fun opcionesWidget(e: Estado = estado.value): List<Opcion> {
        val w = e.widgets.getOrNull(e.widgetAbierto) ?: return emptyList()
        fun juegosComo(l: List<cl.nikalbazu.diptico.data.Juego>) = l.map { j ->
            val d = e.datos[j.clave]
            Opcion(j.titulo, (Consolas.porId(j.consolaId)?.nombre ?: "") + (d?.tiempo?.takeIf { it >= 60 }?.let { " · ${formatoTiempoCorto(it)}" } ?: ""), Icono.JUGAR, d?.paso.orEmpty()) { jugarDesdeWidget(j.clave) }
        }
        return when (w.tipo) {
            "proximaPartida" -> ultimoJugado(e)?.let { j ->
                juegosComo(listOf(j)) + Opcion("Ver o marcar el mapa", icono = Icono.MUNDO) { abrirMapa(j.clave) }
            } ?: listOf(Opcion("Tu próxima partida aparecerá aquí", icono = Icono.INFO, apagada = true) {})
            "colaJuegos" -> juegosComo(PlanPartida.cola(e)).ifEmpty {
                listOf(Opcion("Agrega juegos desde Opciones del juego", icono = Icono.INFO, apagada = true) {})
            }
            "tengo20" -> juegosComo(PlanPartida.breves(e)).ifEmpty {
                listOf(Opcion("Las sugerencias se basan en tus sesiones anteriores", icono = Icono.INFO, apagada = true) {})
            }
            "temporizador" -> opcionesTemporizador(e)
            "cronometro" -> opcionesCronometro(e)
            "reloj" -> opcionesTemporizador(e) + opcionesCronometro(e) + listOf(
                Opcion("Recordarme una pausa al jugar", if (e.pausaCada == 0) "No" else "Cada ${e.pausaCada} min", Icono.CORAZON, "Un cartel amable mientras juegas para descansar la vista y estirarte.", menos = { cambiarPausa(-1) }) { cambiarPausa(1) },
                Opcion("Reloj a pantalla completa", icono = Icono.RELOJ, ayuda = "Muestra la hora en grande en la pantalla de arriba.") { cerrarPanel(); alternarReloj() },
            )
            "musica" -> buildList {
                val m = cl.nikalbazu.diptico.audio.Medios.sonando.value
                if (!cl.nikalbazu.diptico.audio.Medios.tienePermiso(app)) add(Opcion("Dar permiso para ver la música", icono = Icono.CLAVE, ayuda = "Android pide permiso para que Díptico vea y controle lo que suena.") { cl.nikalbazu.diptico.audio.Medios.pedirPermiso(app) })
                if (m != null) {
                    add(Opcion(if (m.reproduciendo) "Pausar" else "Reproducir", m.titulo, Icono.JUGAR) { cl.nikalbazu.diptico.audio.Medios.alternar() })
                    add(Opcion("Siguiente canción", icono = Icono.CONTINUAR) { cl.nikalbazu.diptico.audio.Medios.siguiente() })
                    add(Opcion("Canción anterior", icono = Icono.CONTINUAR) { cl.nikalbazu.diptico.audio.Medios.anterior() })
                    add(Opcion("Abrir ${m.app}", icono = Icono.APPS) { cerrarPanel(); abrirPaquete(m.paquete) })
                }
                appsMusica().filter { it.paquete != m?.paquete }.forEach { a -> add(Opcion("Abrir ${a.nombre}", icono = Icono.MUSICA, paquete = a.paquete) { cerrarPanel(); abrirPaquete(a.paquete) }) }
                add(Opcion("Música de Díptico", if (e.musica) "Sí" else "No", Icono.MUSICA, "La música propia de cada tema.") { fijarMusica(!estado.value.musica) })
                add(Opcion("Elegir canción de Díptico", icono = Icono.MUSICA, ayuda = "Elige qué canción de fondo suena en Díptico.") { abrirGaleria("musica") })
            }
            "recientes" -> juegosComo(e.datos.entries.filter { it.value.ultimo > 0 }.sortedByDescending { it.value.ultimo }.mapNotNull { (k, _) -> e.juegosPorClave[k] }.take(12))
                .ifEmpty { listOf(Opcion("Todavía no juegas nada con Díptico", icono = Icono.INFO, apagada = true) {}) }
            "favoritos" -> juegosComo(e.juegos.filter { e.datos[it.clave]?.favorito == true })
                .ifEmpty { listOf(Opcion("Marca favoritos con Y sobre un juego", icono = Icono.CORAZON, apagada = true) {}) }
            "sugerencia" -> juegosComo(e.sugeridas.mapNotNull { k -> e.juegosPorClave[k] }) + listOf(
                Opcion("Otras sugerencias", icono = Icono.DADO, ayuda = "Te muestra otros juegos de tu biblioteca para jugar ahora.") { nuevasSugerencias() },
                Opcion("Pídele una recomendación a Bisa", icono = Icono.BISA, ayuda = "Bisa elige un juego para ti según lo que te gusta.") { abrirAsistente(); preguntar("¿Qué juego de mi colección me recomiendas para hoy? Dame 3 opciones y por qué.") },
            )
            "fiesta" -> buildList {
                e.fiesta?.let { f -> add(Opcion("Hoy: ${f.nombre}", f.donde, Icono.MUNDO, f.tema.mensaje) {}) }
                Temas.proximas(6).forEach { (f, d) -> add(Opcion(f.nombre, "${d.dayOfMonth}/${d.monthValue}", Icono.MUNDO, f.donde) { avisar("${f.nombre}: ${f.donde}") }) }
                add(Opcion("Temas de fiestas", if (e.temaFestivo) "Sí" else "No", Icono.TEMA, "Díptico se viste solo para cada celebración.") {
                    val v = !estado.value.temaFestivo
                    almacen.guardarBool("temaFestivo", v)
                    actualizar { it.copy(temaFestivo = v) }
                    aplicarTema()
                })
            }
            "bateria" -> {
                val b = infoBateria()
                listOf(
                    Opcion("Carga", if (b.nivel >= 0) "${b.nivel}%" else "—", Icono.BATERIA) {},
                    Opcion("Estado", b.estado, Icono.BATERIA) {},
                    Opcion("Temperatura", if (b.temperatura > 0f) "${"%.1f".format(b.temperatura)} °C" else "—", Icono.INFO, if (b.temperatura >= 42f) "Está tibia: una pausa le viene bien." else "") {},
                    Opcion("Voltaje", if (b.voltaje > 0) "${"%.2f".format(b.voltaje / 1000f)} V" else "—", Icono.INFO) {},
                    Opcion("Salud", b.salud, Icono.CORAZON) {},
                    Opcion("Modo de efectos", nombreFluidez(e.fluidez), Icono.CHISPA, "En ahorro Díptico apaga difuminados y decoraciones para gastar menos.") {
                        val l = listOf("auto", "alta", "equilibrada", "ahorro")
                        val v = l[(l.indexOf(estado.value.fluidez) + 1).mod(l.size)]
                        almacen.guardarTexto("fluidez", v)
                        actualizar { it.copy(fluidez = v) }
                        aplicarFluidez()
                    },
                    Opcion("Ajustes de batería de Android", icono = Icono.AJUSTES, ayuda = "Abre los ajustes de batería de Android.") { cerrarPanel(); abrirAjusteAndroid(android.content.Intent.ACTION_POWER_USAGE_SUMMARY) },
                )
            }
            "tiempo", "estadisticas" -> {
                val top = e.datos.entries.filter { it.value.tiempo > 0 }.sortedByDescending { it.value.tiempo }.mapNotNull { (k, _) -> e.juegosPorClave[k] }.take(8)
                listOf(Opcion("Ver todas las estadísticas", icono = Icono.TROFEO, ayuda = "Horas por juego y por consola, partidas y rachas.") { abrirEstadisticas() }) + juegosComo(top)
            }
            "logros" -> if (e.usuarioRA.isBlank()) listOf(Opcion("Conectar RetroAchievements", icono = Icono.CLAVE, ayuda = "Tu usuario y tu clave web de retroachievements.org.") { abrirMenu(Seccion.LOGROS) })
            else listOf(
                Opcion("Ver mis logros", icono = Icono.TROFEO, ayuda = "Tus puntos, los últimos logros y los de cada juego.") { abrirEstadisticas() },
                Opcion("Ajustes de logros", icono = Icono.AJUSTES, ayuda = "Tu cuenta de RetroAchievements y cómo se ven tus logros.") { abrirMenu(Seccion.LOGROS) },
            )
            "consolas" -> e.consolasConJuegos.map { c ->
                Opcion(c.bonito, "${e.juegosPorConsola[c.id].orEmpty().size} juegos", Icono.BIBLIOTECA, ayuda = "Va a esta consola en el estante.") { cerrarPanel(); actualizar { it.copy(widgetFoco = -1) }; elegirConsola(c.id) }
            }
            "frase" -> listOf(
                Opcion("Otra frase", icono = Icono.CHISPA, ayuda = "Muestra otra frase en el widget.") { actualizar { it.copy(fraseExtra = it.fraseExtra + 1) } },
                Opcion("Pídele una a Bisa", icono = Icono.BISA, ayuda = "Bisa escribe una frase nueva para el widget.") { abrirAsistente(); preguntar("Dame una frase corta y simpática para empezar a jugar hoy.") },
            )
            "notificaciones" -> Avisos.lista.value.map { a -> Opcion(a.titulo.ifBlank { a.app }, a.app, Icono.CAMPANA, a.texto, paquete = a.paquete) { abrirAviso(a) } }
                .ifEmpty { listOf(Opcion("Todo tranquilo: no hay notificaciones", icono = Icono.CHECK, apagada = true) {}) }
            "asistente" -> buildList {
                val j = e.seleccionado
                add(Opcion("Conversar con Bisa", icono = Icono.BISA, ayuda = "Pregúntale lo que quieras: trucos, qué jugar, o cambiar Díptico.") { abrirAsistente() })
                if (j != null) {
                    add(Opcion("Un truco para ${j.titulo}", icono = Icono.CHISPA, ayuda = "Le pregunta a Bisa por un truco para este juego.") { abrirAsistente(); preguntar("Dame un buen truco o consejo para ${j.titulo}.") })
                    add(Opcion("¿De qué trata ${j.titulo}?", icono = Icono.TUTORIAL, ayuda = "Bisa te cuenta de qué trata este juego, sin spoilers.") { abrirAsistente(); preguntar("Cuéntame en pocas líneas de qué trata ${j.titulo} y por qué vale la pena.") })
                    e.datos[j.clave]?.paso?.takeIf { it.isNotBlank() }?.let { paso -> add(Opcion("Ayúdame a seguir donde quedé", paso.take(30), Icono.LAPIZ, ayuda = "Bisa te ayuda con lo que anotaste como tu próximo paso.") { abrirAsistente(); preguntar("En ${j.titulo} anoté esto: \"$paso\". ¿Qué me conviene hacer ahora?") }) }
                }
                add(Opcion("¿Qué juego hoy?", icono = Icono.DADO, ayuda = "Bisa te recomienda qué jugar hoy.") { abrirAsistente(); preguntar("¿Qué juego de mi colección me recomiendas para hoy? Dame 3 opciones y por qué.") })
                add(Opcion("Cámbiame el tema", icono = Icono.TEMA, ayuda = "Bisa te cambia el tema de Díptico.") { abrirAsistente(); preguntar("Cámbiame el tema de Díptico por uno que me sorprenda.") })
                add(Opcion("Ayuda mientras juego", icono = Icono.BISA, ayuda = "Bisa mira tu partida y te ayuda (con el botón de Bisa en el juego).") { prepararAyuda() })
            }
            else -> if (w.tipo.startsWith("coleccion-")) {
                val id = w.tipo.removePrefix("coleccion-")
                val c = e.colecciones[id] ?: return emptyList()
                val j = e.seleccionado
                juegosComo(c.juegos.mapNotNull { k -> e.juegosPorClave[k] }) + listOfNotNull(
                    j?.let { Opcion(if (it.clave in c.juegos) "Quitar ${it.titulo}" else "Agregar ${it.titulo}", icono = Icono.CARPETA, ayuda = "El juego elegido en el estante.") { alternarEnColeccion(id, it.clave) } },
                    Opcion("Borrar esta colección", icono = Icono.BORRAR, ayuda = "Los juegos no se borran, solo la carpeta.") { borrarColeccion(id) },
                )
            } else if (w.tipo.startsWith("imagen")) listOf(
                Opcion("Cambiar la imagen o GIF", icono = Icono.PORTADA, ayuda = "Elige una imagen o GIF de tus archivos para este widget.") { pedirArchivoPara("widget:${w.tipo}", "image/*") },
                Opcion("Acomodar en el estudio", icono = Icono.APPS, ayuda = "Abre el estudio para mover, agrandar o cambiar el estilo de este widget.") { cerrarPanel(); editarWidgets(); elegirWidget(e.widgetAbierto) },
            ) else emptyList()
        }
    }

    data class InfoBateria(val nivel: Int, val estado: String, val temperatura: Float, val voltaje: Int, val salud: String)

    fun infoBateria(): InfoBateria {
        val i = app.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val nivel = i?.let { val l = it.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1); val s = it.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100); if (l >= 0) l * 100 / s.coerceAtLeast(1) else -1 } ?: -1
        val est = when (i?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)) {
            android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "Cargando"
            android.os.BatteryManager.BATTERY_STATUS_FULL -> "Llena"
            android.os.BatteryManager.BATTERY_STATUS_DISCHARGING, android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Usando la batería"
            else -> "—"
        }
        val salud = when (i?.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, -1)) {
            android.os.BatteryManager.BATTERY_HEALTH_GOOD -> "Buena"
            android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Muy caliente"
            android.os.BatteryManager.BATTERY_HEALTH_COLD -> "Muy fría"
            android.os.BatteryManager.BATTERY_HEALTH_DEAD -> "Gastada"
            else -> "—"
        }
        return InfoBateria(nivel, est, (i?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f, i?.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0) ?: 0, salud)
    }

    // ---------- Imagen grande del inicio y cajas en "Todos" ----------

    // ---------- Dictado por voz (para Bisa y el teclado) ----------

    /** Abre el reconocimiento de voz de Android (lo registra la pantalla activa). */
    var pedirVoz: (() -> Boolean)? = null
    private var vozPara = ""

    fun dictar(para: String) {
        vozPara = para
        if (pedirVoz?.invoke() != true) avisar("Esta consola no tiene dictado por voz. Instala la app de Google para usarlo.")
    }

    fun vozRecibida(texto: String) {
        val t = texto.trim()
        if (t.isEmpty()) return
        when (vozPara) {
            "bisa" -> { if (estado.value.panel != Panel.ASISTENTE) abrirAsistente(); preguntar(t.replaceFirstChar { it.uppercase() }) }
            else -> escribir((if (estado.value.borrador.isNotEmpty() && !estado.value.borrador.endsWith(" ")) " " else "") + t)
        }
    }

    // ---------- Accesos directos y colecciones (widgets) ----------

    /** Pone un widget con un juego en el inicio de widgets. */
    fun crearAtajo(clave: String) {
        val tipo = "atajo-$clave"
        val l = estado.value.widgets
        val i = l.indexOfFirst { it.tipo == tipo }
        if (i >= 0 && l[i].visible) { avisar("Ese acceso ya está en tu inicio"); return }
        if (i < 0) guardarWidgets(l + Widget(tipo, 0, 0, 1, 2, false, estado.value.paginaInicio, estilo = "sinfondo"))
        val k = estado.value.widgets.indexOfFirst { it.tipo == tipo }
        val celda = celdaPendiente
        celdaPendiente = null
        if (celda != null) ponerEnCelda(k, celda.first, celda.second) else alternarWidget(k)
        avisar("Acceso directo agregado a tu inicio de widgets")
    }

    fun crearColeccion(nombre: String, conJuego: String?) {
        val id = System.currentTimeMillis().toString(36)
        val m = estado.value.colecciones + (id to Coleccion(nombre.take(30), listOfNotNull(conJuego)))
        guardarColecciones(m)
        guardarWidgets(estado.value.widgets + Widget("coleccion-$id", 0, 0, 2, 2, false, estado.value.paginaInicio))
        val k = estado.value.widgets.indexOfFirst { it.tipo == "coleccion-$id" }
        val celda = celdaPendiente
        celdaPendiente = null
        if (celda != null) ponerEnCelda(k, celda.first, celda.second) else alternarWidget(k)
        cerrarPanel()
        avisar("Colección «${nombre.take(30)}» creada en tu inicio")
    }

    fun alternarEnColeccion(id: String, clave: String) {
        val c = estado.value.colecciones[id] ?: return
        val nueva = if (clave in c.juegos) c.copy(juegos = c.juegos - clave) else c.copy(juegos = c.juegos + clave)
        guardarColecciones(estado.value.colecciones + (id to nueva))
        avisar(if (clave in nueva.juegos) "Agregado a «${c.nombre}»" else "Quitado de «${c.nombre}»")
    }

    fun borrarColeccion(id: String) {
        guardarColecciones(estado.value.colecciones - id)
        guardarWidgets(estado.value.widgets.filter { it.tipo != "coleccion-$id" })
        cerrarPanel()
        avisar("Colección borrada")
    }

    private fun guardarColecciones(m: Map<String, Coleccion>) {
        val o = org.json.JSONObject()
        m.forEach { (k, c) -> o.put(k, org.json.JSONObject().put("nombre", c.nombre).put("juegos", org.json.JSONArray(c.juegos))) }
        almacen.guardarTexto("colecciones", o.toString())
        actualizar { it.copy(colecciones = m) }
    }

    private fun leerColecciones(s: String): Map<String, Coleccion> = try {
        val o = org.json.JSONObject(s)
        o.keys().asSequence().associateWith { k ->
            val c = o.getJSONObject(k)
            val a = c.optJSONArray("juegos") ?: org.json.JSONArray()
            Coleccion(c.optString("nombre", "Colección"), (0 until a.length()).map { a.getString(it) })
        }
    } catch (_: Exception) { emptyMap() }

    // ---------- Tu equipo: detectarlo y acomodar Díptico ----------

    fun equipo(): Equipo = Equipos.detectar(app, Pantallas.inferior(app) != null)

    /** Acomoda la distribución y los efectos al equipo: vistas, pantallas y fluidez. */
    fun optimizarParaEquipo(silencioso: Boolean = false) {
        val q = equipo()
        val vista = when (q.tipo) { "telefono", "tablet" -> "cuadricula"; else -> null }
        if (vista != null) { almacen.guardarTexto("vista", vista); actualizar { it.copy(vista = vista) } }
        if (q.tipo == "dual" && estado.value.modoPantallas != "dos") elegirModo("dos")
        if (q.tipo == "tele") guardarSobremesa("sobremesaModo", "si") { it.copy(sobremesaModo = "si") }
        // Los efectos según la potencia (la gama media usa los efectos, pero sin los más pesados).
        val f = when (q.gama) { "alta" -> "alta"; "baja" -> "ahorro"; else -> "equilibrada" }
        almacen.guardarTexto("fluidez", f)
        actualizar { it.copy(fluidez = f) }
        aplicarFluidez()
        if (!silencioso) avisar("Listo: Díptico acomodado para tu ${q.nombre}")
    }

    // ---------- Más fuentes de imágenes (IGDB, ScreenScraper, RAWG, TheGamesDB) ----------

    /** Título corto de cada dato de las fuentes de imágenes (para el panel, el teclado y el QR). */
    fun tituloRaspador(para: String): String? = when (para) {
        "ras_igdbId" -> "IGDB · Client ID"; "ras_igdbSecreto" -> "IGDB · Client Secret"
        "ras_ssUsuario" -> "Usuario de ScreenScraper"; "ras_ssClave" -> "Contraseña de ScreenScraper"
        "ras_ssDevId" -> "ScreenScraper · devid"; "ras_ssDevClave" -> "ScreenScraper · devpassword"
        "ras_rawg" -> "Clave de RAWG"; "ras_tgdb" -> "Clave de TheGamesDB"
        else -> null
    }

    private fun leerRaspadores() = cl.nikalbazu.diptico.data.Raspadores.Claves(
        almacen.texto("ras_igdbId"), almacen.texto("ras_igdbSecreto"), almacen.texto("ras_ssUsuario"), almacen.texto("ras_ssClave"),
        almacen.texto("ras_ssDevId"), almacen.texto("ras_ssDevClave"), almacen.texto("ras_rawg"), almacen.texto("ras_tgdb"),
    )

    private fun guardarRaspador(campo: String, valor: String) {
        almacen.guardarTexto("ras_$campo", valor)
        val c = leerRaspadores()
        actualizar { it.copy(raspadores = c) }
        // ¿Quedan datos por pedir de esta fuente? Se pide el siguiente.
        if (cadenaRaspador.isNotEmpty()) {
            val sig = cadenaRaspador.first()
            cadenaRaspador = cadenaRaspador.drop(1)
            abrirTexto("ras_$sig", valorRaspador(c, sig))
            return
        }
        cerrarPanel()
        // Si la fuente ya tiene todo lo que pide, se prueba al tiro.
        val fuente = when (campo) { "igdbId", "igdbSecreto" -> "igdb"; "rawg" -> "rawg"; "tgdb" -> "tgdb"; else -> "ss" }
        val lista = when (fuente) { "igdb" -> c.igdb; "rawg" -> c.conRawg; "tgdb" -> c.conTgdb; else -> c.screenscraper }
        if (valor.isBlank()) { avisar("Dato quitado"); return }
        if (!lista) { avisar("Guardado. Falta el otro dato de esta fuente"); return }
        avisar("Probando la conexión…")
        scope.launch {
            val ok = withContext(Dispatchers.IO) { try { cl.nikalbazu.diptico.data.Raspadores.probar(c, fuente) } catch (_: Exception) { false } }
            avisar(if (ok) "¡Conectado! Ya puedo buscar imágenes ahí" else "No funcionó. Revisa los datos o tu conexión")
        }
    }

    /** Los datos que faltan por pedir de la fuente que se está conectando (se piden uno tras otro). */
    private var cadenaRaspador: List<String> = emptyList()

    /** Conecta una fuente pidiendo sus datos uno tras otro (ScreenScraper pide 4; IGDB, 2). */
    private fun conectarFuente(campos: List<String>) {
        val c = estado.value.raspadores
        cadenaRaspador = campos.drop(1)
        abrirTexto("ras_${campos.first()}", valorRaspador(c, campos.first()))
    }

    private fun valorRaspador(c: cl.nikalbazu.diptico.data.Raspadores.Claves, campo: String) = when (campo) {
        "igdbId" -> c.igdbId; "igdbSecreto" -> c.igdbSecreto; "ssUsuario" -> c.ssUsuario; "ssClave" -> c.ssClave
        "ssDevId" -> c.ssDevId; "ssDevClave" -> c.ssDevClave; "rawg" -> c.rawg; else -> c.tgdb
    }

    private fun opcionesRaspadores(e: Estado): List<Opcion> {
        val c = e.raspadores
        // Valor corto para que el nombre de la fuente se lea completo.
        fun estado(listo: Boolean, alguno: Boolean) = if (listo) "✓" else if (alguno) "Falta" else "Añadir"
        return listOf(
            Opcion("ScreenScraper", estado(c.screenscraper, listOf(c.ssUsuario, c.ssClave, c.ssDevId, c.ssDevClave).any { it.isNotBlank() }), Icono.CLAVE,
                "Cajas, fanart, capturas y logos de casi todas las consolas. Te pide 4 datos: tu usuario y contraseña de screenscraper.fr, y el devid y devpassword de desarrollador que se piden gratis en su foro (sección API).") {
                conectarFuente(listOf("ssUsuario", "ssClave", "ssDevId", "ssDevClave"))
            },
            Opcion("IGDB", estado(c.igdb, c.igdbId.isNotBlank() || c.igdbSecreto.isNotBlank()), Icono.CLAVE,
                "Portadas, artes y capturas en alta calidad (de Twitch). Gratis: crea una app en dev.twitch.tv y copia su Client ID y su Client Secret.") {
                conectarFuente(listOf("igdbId", "igdbSecreto"))
            },
            Opcion("TheGamesDB", estado(c.conTgdb, false), Icono.CLAVE,
                "Cajas, fanart y logos. Clave gratis que se pide en el foro de thegamesdb.net.") { conectarFuente(listOf("tgdb")) },
            Opcion("RAWG", estado(c.conRawg, false), Icono.CLAVE,
                "Imágenes grandes y capturas, muy buena para juegos de PC y consolas nuevas. Clave gratis en rawg.io/apidocs.") { conectarFuente(listOf("rawg")) },
        )
    }

    fun nombreHeroe(v: String?) = when (v) { null -> "Como todos"; "portada" -> "Portada"; else -> "Imagen grande" }

    private fun ciclarHeroe(clave: String) {
        val l = listOf(null, "arte", "portada")
        val v = l[(l.indexOf(estado.value.heroes[clave]) + 1).mod(l.size)]
        val m = if (v == null) estado.value.heroes - clave else estado.value.heroes + (clave to v)
        almacen.guardarTexto("heroes", escribirMapa(m))
        actualizar { it.copy(heroes = m) }
    }

    fun nombreFormaTodos(v: String) = when (v) { "cuadrada" -> "Todas cuadradas"; "propia" -> "Cada consola la suya"; else -> "Todas en póster" }

    private fun ciclarFormaTodos() {
        val l = listOf("poster", "cuadrada", "propia")
        val v = l[(l.indexOf(estado.value.formaTodos) + 1).mod(l.size)]
        almacen.guardarTexto("formaTodos", v)
        actualizar { it.copy(formaTodos = v) }
    }

    private fun imagenTodasLasCajas() {
        val e = estado.value
        val actual = e.formas.values.firstOrNull()?.imagen ?: "portada"
        val v = IMAGENES_CAJA[(IMAGENES_CAJA.indexOf(actual) + 1).mod(IMAGENES_CAJA.size)]
        guardarFormas(Consolas.todas.associate { c -> c.id to (e.formas[c.id] ?: FormaCfg()).copy(imagen = v) })
        avisar("Todas las cajas: ${nombreImagenCaja(v)}")
    }

    /** Las formas que se dibujan: en "Todo" (si así se eligió) todas las consolas con la misma forma. */
    fun formasVisibles(e: Estado): Map<String, FormaCfg> =
        if (e.consolaSel != "todo" || e.formaTodos == "propia") e.formas
        else Consolas.todas.associate { c -> c.id to (e.formas[c.id] ?: FormaCfg()).copy(forma = e.formaTodos) }

    // ---------- ¿Dónde quedaste? (al terminar de jugar, o desde la ventana del juego) ----------

    /** Las formas de anotar, en orden: escribir, nota de voz, mapa y pasar de largo. */
    val FORMAS_QUEDASTE = listOf("escribir", "voz", "mapa", "nada")

    fun abrirQuedaste() {
        if (estado.value.seleccionado == null) return
        actualizar { it.copy(quedaste = true, quedastePregunta = false, quedasteSel = 0) }
    }

    fun moverQuedaste(d: Int) {
        if (estado.value.grabando > 0L) return
        actualizar { it.copy(quedasteSel = (it.quedasteSel + d).coerceIn(0, FORMAS_QUEDASTE.size - 1)) }
        Sonidos.mover()
    }

    fun cerrarQuedaste() {
        if (estado.value.grabando > 0L) { Notas.cancelar(); actualizar { it.copy(grabando = 0L) } }
        actualizar { it.copy(quedaste = false, quedastePregunta = false) }
    }

    /** No volver a preguntar al terminar de jugar (se vuelve a activar en Menú › Inicio). */
    fun noPreguntarQuedaste() {
        almacen.guardarBool("preguntarQuedaste", false)
        actualizar { it.copy(preguntarQuedaste = false) }
        cerrarQuedaste()
        avisar("Listo, no te pregunto más. Se activa en Menú › Inicio")
    }

    fun elegirQuedaste(i: Int = estado.value.quedasteSel) {
        val e = estado.value
        val clave = e.seleccionado?.clave ?: return
        actualizar { it.copy(quedasteSel = i) }
        when (FORMAS_QUEDASTE[i]) {
            "escribir" -> {
                val pregunta = e.quedastePregunta
                actualizar { it.copy(quedaste = false) }
                abrirPaso()
                actualizar { it.copy(pregunta = pregunta) }
            }
            "voz" -> alternarGrabacion(clave)
            "mapa" -> {
                actualizar { it.copy(quedaste = false) }
                abrirMapa(clave)
            }
            else -> cerrarQuedaste()
        }
    }

    /** Pedirle a Android el micrófono (lo registra la pantalla activa). */
    var pedirMicrofono: (() -> Unit)? = null

    private var grabacionJob: Job? = null

    private fun alternarGrabacion(clave: String) {
        if (estado.value.grabando > 0L) { terminarGrabacion(clave); return }
        if (!Notas.puedeGrabar(app)) {
            pedirMicrofono?.invoke()
            avisar("Dale permiso al micrófono y vuelve a elegir Nota de voz")
            return
        }
        if (!Notas.grabar(app, clave)) { avisar("No pude usar el micrófono"); return }
        Sonidos.abrir()
        actualizar { it.copy(grabando = System.currentTimeMillis()) }
        grabacionJob?.cancel()
        grabacionJob = scope.launch {
            delay(Notas.MAX_MS)
            if (estado.value.grabando > 0L) terminarGrabacion(clave)
        }
    }

    private fun terminarGrabacion(clave: String) {
        grabacionJob?.cancel()
        val ok = Notas.terminar(app)
        actualizar { it.copy(grabando = 0L, quedaste = !ok && it.quedaste, notasVoz = if (ok) it.notasVoz + clave else it.notasVoz) }
        if (ok) { Sonidos.aviso(); avisar("Nota de voz guardada") } else avisar("No se grabó nada")
    }

    fun escucharNota(clave: String) = Notas.reproducir(app, clave)

    fun borrarNota(clave: String) {
        Notas.borrar(app, clave)
        actualizar { it.copy(notasVoz = it.notasVoz - clave) }
        avisar("Nota de voz borrada")
    }

    private fun leerNotasVoz(): Set<String> = try {
        val hay = java.io.File(app.filesDir, "notas").list()?.toSet() ?: emptySet()
        crudos.map { it.clave }.filter { "${it.hashCode().toUInt()}.m4a" in hay }.toSet()
    } catch (_: Exception) { emptySet() }

    // ---------- Mapa del juego: una imagen tuya con un pin donde quedaste ----------

    fun abrirMapa(clave: String) {
        val m = estado.value.mapas[clave]
        if (m == null) {
            avisar("Elige una imagen del mapa del juego (una captura o una descargada)")
            pedirArchivoPara("mapa:$clave", "image/*")
            return
        }
        actualizar { it.copy(marcandoMapa = clave, pinX = m.x, pinY = m.y) }
    }

    fun moverPin(dx: Float, dy: Float) = actualizar { it.copy(pinX = (it.pinX + dx).coerceIn(0f, 1f), pinY = (it.pinY + dy).coerceIn(0f, 1f)) }
    fun ponerPin(x: Float, y: Float) = actualizar { it.copy(pinX = x.coerceIn(0f, 1f), pinY = y.coerceIn(0f, 1f)) }

    fun guardarPin() {
        val e = estado.value
        val clave = e.marcandoMapa ?: return
        val m = e.mapas[clave] ?: return
        guardarMapas(e.mapas + (clave to m.copy(x = e.pinX, y = e.pinY)))
        actualizar { it.copy(marcandoMapa = null) }
        Sonidos.aviso()
        avisar("Marcado en el mapa")
    }

    fun cerrarMapa() = actualizar { it.copy(marcandoMapa = null) }

    fun cambiarImagenMapa() {
        val clave = estado.value.marcandoMapa ?: return
        actualizar { it.copy(marcandoMapa = null) }
        pedirArchivoPara("mapa:$clave", "image/*")
    }

    fun quitarMapa() {
        val clave = estado.value.marcandoMapa ?: return
        guardarMapas(estado.value.mapas - clave)
        actualizar { it.copy(marcandoMapa = null) }
        avisar("Mapa quitado")
    }

    private fun guardarMapas(m: Map<String, MapaJuego>) {
        val o = org.json.JSONObject()
        m.forEach { (k, v) -> o.put(k, org.json.JSONObject().put("uri", v.uri).put("x", v.x.toDouble()).put("y", v.y.toDouble())) }
        almacen.guardarTexto("mapas", o.toString())
        actualizar { it.copy(mapas = m) }
    }

    private fun leerMapas(s: String): Map<String, MapaJuego> = try {
        val o = org.json.JSONObject(s)
        o.keys().asSequence().associateWith { k -> o.getJSONObject(k).let { MapaJuego(it.getString("uri"), it.optDouble("x", 0.5).toFloat(), it.optDouble("y", 0.5).toFloat()) } }
    } catch (_: Exception) { emptyMap() }

    fun nombreSticks(m: String) = when (m) { "invertido" -> "Izquierdo widgets · derecho juegos"; "clasico" -> "Clásico (izquierdo = cruceta)"; else -> "Izquierdo juegos · derecho widgets" }

    // ---------- ¿En qué pantalla lo abro? (mantener A sobre un juego o una app) ----------

    fun puedeElegirPantalla(e: Estado = estado.value) = !e.unaPantalla && Pantallas.inferior(app) != null

    /** [destino]: "juego" (el elegido) o "app:paquete". Con una sola pantalla abre lo de siempre. */
    fun elegirPantalla(destino: String) {
        val e = estado.value
        if (!puedeElegirPantalla(e)) {
            if (destino == "juego") abrirRapido() else alternarFijada(destino.removePrefix("app:"))
            return
        }
        if (destino == "juego" && e.seleccionado == null) return
        val actual = if (destino == "juego") e.seleccionado?.let { e.datos[it.clave]?.pantalla } ?: e.pantallaJuegos else "arriba"
        actualizar { it.copy(donde = destino, dondeSel = if (actual == "abajo") 1 else 0, dondeRecordar = false) }
        Sonidos.abrir()
    }

    fun moverDonde(d: Int) { actualizar { it.copy(dondeSel = (it.dondeSel + d).coerceIn(0, 2)) }; Sonidos.mover() }
    fun alternarRecordarDonde() = actualizar { it.copy(dondeRecordar = !it.dondeRecordar) }
    fun cerrarDonde() { actualizar { it.copy(donde = null) }; Sonidos.atras() }

    /** 0 = arriba, 1 = abajo, 2 = más opciones (la ventana del juego, o fijar la app). */
    fun confirmarDonde(sel: Int = estado.value.dondeSel) {
        val e = estado.value
        val d = e.donde ?: return
        actualizar { it.copy(donde = null) }
        if (sel == 2) {
            if (d == "juego") abrirRapido() else alternarFijada(d.removePrefix("app:"))
            return
        }
        val pantalla = if (sel == 1) "abajo" else "arriba"
        if (d == "juego") {
            val j = e.seleccionado ?: return
            if (e.dondeRecordar) cambiarDatos(j.clave) { it.copy(pantalla = pantalla) }
            jugar(pantalla)
        } else {
            val pkg = d.removePrefix("app:")
            val inferior = Pantallas.inferior(app)
            val id = if (pantalla == "abajo" && inferior != null) inferior.displayId else Display.DEFAULT_DISPLAY
            cerrarPanel()
            Lanzador.lanzarApp(app, pkg, Lanzador.nombreApp(app, pkg) ?: "La app", id)?.let { avisar(it) }
        }
    }

    // ---------- Botón de turbo en el inicio ----------

    /** Tocar: libera memoria ahora (y deja el turbo al jugar encendido). Mantener: enciende o apaga el turbo al jugar. */
    fun turboAhora() {
        if (!estado.value.turbo) { almacen.guardarBool("turbo", true); actualizar { it.copy(turbo = true) } }
        optimizarAhora()
    }

    fun alternarTurbo() {
        val v = !estado.value.turbo
        almacen.guardarBool("turbo", v)
        actualizar { it.copy(turbo = v) }
        Sonidos.interruptor(v)
        avisar(if (v) "Turbo al jugar: encendido" else "Turbo al jugar: apagado")
    }

    // ---------- Actualizaciones desde la app ----------

    private var ultimaConsultaAct: Long? = null

    private fun vigilarActualizaciones() {
        Actualizador.programar(app, estado.value.actualizarSolo)
        scope.launch {
            while (true) {
                delay(5 * 60_000L)
                if (estado.value.aLaVista > 0) revisarActualizacionAlVolver()
            }
        }
    }

    fun revisarActualizacionAlVolver() {
        if (!estado.value.listo || !estado.value.actualizarSolo) return
        val ultima = ultimaConsultaAct
        if (ultima == null || android.os.SystemClock.elapsedRealtime() - ultima >= 60_000L) {
            buscarActualizacion(silencioso = true)
        } else {
            estado.value.actualizacion?.let { Actualizador.avisarSiNueva(app, it) }
        }
    }

    fun buscarActualizacion(silencioso: Boolean = false) {
        if (estado.value.buscandoAct) return
        ultimaConsultaAct = android.os.SystemClock.elapsedRealtime()
        actualizar { it.copy(buscandoAct = true) }
        if (!silencioso) avisar("Buscando una versión nueva…")
        scope.launch {
            val resultado = withContext(Dispatchers.IO) { runCatching { Actualizador.buscar(app) } }
            val v = resultado.getOrNull()
            val nuevaParaLaApp = v != null && v.numero != estado.value.actualizacion?.numero
            actualizar { it.copy(buscandoAct = false, actualizacion = v ?: it.actualizacion) }
            when {
                v != null -> {
                    if (Actualizador.avisarSiNueva(app, v)) Sonidos.aviso()
                    if (nuevaParaLaApp || !silencioso) avisar("¡Díptico ${v.nombre} ya está disponible! Menú › Acerca de")
                }
                !silencioso && resultado.isFailure -> avisar("No pude consultar GitHub. Revisa tu conexión.")
                !silencioso -> avisar("Ya tienes la versión más nueva")
            }
        }
    }

    /** Desde la notificación: abre Acerca de con la actualización lista para descargar. */
    fun abrirActualizacion() {
        abrirMenu(Seccion.SISTEMA)
        if (estado.value.actualizacion == null) buscarActualizacion()
    }

    fun descargarActualizacion() {
        val v = estado.value.actualizacion ?: run { buscarActualizacion(); return }
        if (estado.value.descargaAct in 0f..0.999f) return
        if (!Actualizador.puedeInstalar(app)) {
            Actualizador.pedirPermisoInstalar(app)
            avisar("Permite a Díptico instalar apps y vuelve a elegir Descargar")
            return
        }
        actualizar { it.copy(descargaAct = 0f) }
        scope.launch {
            val apk = withContext(Dispatchers.IO) {
                try { Actualizador.descargar(app, v) { a -> actualizar { it.copy(descargaAct = a.coerceAtMost(0.99f)) } } } catch (_: Exception) { null }
            }
            if (apk == null) { actualizar { it.copy(descargaAct = -1f) }; avisar("No pude descargar la versión nueva. Revisa el internet."); return@launch }
            actualizar { it.copy(descargaAct = 1f) }
            avisar("Descargada: Android te pedirá confirmar la instalación")
            withContext(Dispatchers.IO) { try { Actualizador.instalar(app, apk) } catch (_: Exception) { null } }
        }
    }

    fun instalacionFallida(motivo: String?) {
        actualizar { it.copy(descargaAct = -1f) }
        avisar("No se pudo instalar la actualización" + (motivo?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""))
    }

    private fun opcionesActualizar(e: Estado): List<Opcion> = buildList {
        val v = e.actualizacion
        if (v != null) {
            val valor = when {
                e.descargaAct >= 1f -> "Instalando…"
                e.descargaAct >= 0f -> "Descargando ${(e.descargaAct * 100).toInt()} %"
                else -> "${v.bytes / (1024 * 1024)} MB"
            }
            add(Opcion("Descargar e instalar ${v.nombre}", valor, Icono.DESCARGA,
                "La versión nueva se instala encima: tus juegos, portadas y ajustes se quedan. " + v.notas.replace("\n", " · ").take(160)) { descargarActualizacion() })
        } else add(Opcion("Buscar actualizaciones", if (e.buscandoAct) "Buscando…" else "Versión ${Actualizador.numeroActual(app)}", Icono.DESCARGA,
            "Mira si hay una versión más nueva de Díptico en GitHub.") { buscarActualizacion() })
        add(Opcion("Avisarme de versiones nuevas", if (e.actualizarSolo) "Sí" else "No", Icono.CAMPANA,
            "Revisa al volver a Díptico y en segundo plano. Android decide cuándo corre la revisión y debe permitir las notificaciones.") {
            val n = !estado.value.actualizarSolo
            almacen.guardarBool("actualizarSolo", n)
            actualizar { it.copy(actualizarSolo = n) }
            Actualizador.programar(app, n)
            if (n) revisarActualizacionAlVolver()
        })
    }

    // ---------- Tour de novedades ----------

    fun moverNovedades(d: Int) {
        val n = estado.value.novedades
        if (n < 0) return
        val sig = n + d
        if (sig >= paginasTour().size) { cerrarNovedades(); return }
        actualizar { it.copy(novedades = sig.coerceAtLeast(0)) }
        Sonidos.pagina()
    }

    /** Las páginas del tour que se está viendo. */
    fun paginasTour(e: Estado = estado.value) =
        if (e.recorrido || e.novedadesDesde.isBlank()) Novedades.paginasDe(e.recorrido) else Novedades.paginasDe(false, e.novedadesDesde)

    fun cerrarNovedades() {
        almacen.guardarTexto("novedadesVistas", Novedades.ID)
        val primeraVez = estado.value.recorrido && !almacen.texto("recorridoVisto").isNotEmpty()
        if (estado.value.recorrido) almacen.guardarTexto("recorridoVisto", "si")
        actualizar { it.copy(novedades = -1, recorrido = false) }
        if (primeraVez) ofrecerInicio()
    }

    private fun formatoTiempoCorto(seg: Long): String {
        val min = seg / 60
        return if (min >= 60) "${min / 60} h ${min % 60} min" else "$min min"
    }

    private fun abrirAjusteAndroid(accion: String) {
        try { app.startActivity(android.content.Intent(accion).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { avisar("No pude abrir ese ajuste") }
    }

    // ---------- Encuadrar portadas y heroes ----------

    private fun leerEncuadres(t: String): Map<String, Encuadre> = try {
        val o = org.json.JSONObject(t)
        o.keys().asSequence().associateWith { k ->
            val p = o.getString(k).split(",").map { it.toFloatOrNull() ?: 0f }
            Encuadre(p.getOrElse(0) { 0f }, p.getOrElse(1) { 0f }, p.getOrElse(2) { 1f })
        }
    } catch (_: Exception) { emptyMap() }

    private fun guardarEncuadres(m: Map<String, Encuadre>) {
        val o = org.json.JSONObject()
        m.forEach { (k, v) -> o.put(k, "${v.x},${v.y},${v.zoom}") }
        almacen.guardarTexto("encuadres", o.toString())
        actualizar { it.copy(encuadres = m) }
    }

    /** Abre el encuadre de la portada ("portada") o de la imagen grande ("arte") del juego de la ficha. */
    fun abrirEncuadre(tipo: String) {
        val j = juegoFicha() ?: return
        val k = "${j.clave}|$tipo"
        actualizar { it.copy(panel = Panel.ENCUADRE, encuadreDe = k, encuadreBorrador = it.encuadres[k] ?: Encuadre()) }
    }

    /** Corre la foto (la cruceta o el dedo): [dx] y [dy] en la escala de -1 a 1. */
    fun moverEncuadre(dx: Float, dy: Float) {
        actualizar { it.copy(encuadreBorrador = it.encuadreBorrador.let { b -> b.copy(x = (b.x + dx).coerceIn(-1f, 1f), y = (b.y + dy).coerceIn(-1f, 1f)) }) }
    }

    fun acercarEncuadre(d: Float) {
        actualizar { it.copy(encuadreBorrador = it.encuadreBorrador.let { b -> b.copy(zoom = kotlin.math.round((b.zoom + d) * 20f) / 20f).let { n -> n.copy(zoom = n.zoom.coerceIn(1f, 3f)) } }) }
    }

    fun centrarEncuadre() = actualizar { it.copy(encuadreBorrador = Encuadre()) }

    fun guardarEncuadre() {
        val e = estado.value
        val b = e.encuadreBorrador
        guardarEncuadres(if (b == Encuadre()) e.encuadres - e.encuadreDe else e.encuadres + (e.encuadreDe to b))
        avisar("Listo, quedó encuadrada")
        cerrarPanel()
    }

    /** Lo que se ve: mientras se encuadra, el borrador ya se nota en todas partes. */
    fun encuadresVisibles(e: Estado): Map<String, Encuadre> =
        if (e.panel == Panel.ENCUADRE && e.encuadreDe.isNotEmpty()) e.encuadres + (e.encuadreDe to e.encuadreBorrador) else e.encuadres

    // ---------- Elegir una imagen de internet ----------

    fun nombreCandidato(t: String) = when (t) { "arte", "c_hero" -> "imagen grande"; "logo", "c_logo" -> "logo"; "c_icono", "c_portada" -> "ícono"; else -> "portada" }

    /** Pestañas de la tienda según qué se busca (las consolas usan RetroArch en vez de libretro). */
    fun fuentesImagen(e: Estado = estado.value): List<String> {
        val carpeta = e.candidatoPara == "c_portada"
        if (!carpeta && e.candidatoPara.startsWith("c_")) return listOf("todas", "RetroArch", "SteamGridDB", "Wikipedia")
        val c = e.raspadores
        val consola = if (carpeta) juegoDeCarpetaParaArte(e, e.nombreImagen)?.consolaId else juegoFicha()?.consolaId
        val fuentes = if (carpeta) listOf("todas", "RetroArch") + FUENTES_IMAGEN.drop(1) else FUENTES_IMAGEN
        return fuentes.filter { f -> when (f) {
            "Steam" -> consola in setOf("steam", "windows", "pc")
            "ScreenScraper" -> c.screenscraper
            "IGDB" -> c.igdb
            "TheGamesDB" -> c.conTgdb
            "RAWG" -> c.conRawg
            else -> true
        } }
    }

    /** Busca varias opciones (libretro, SteamGridDB y Wikipedia) para la portada, el hero o el logo. */
    /** Fuentes de la tienda de imágenes (pestañas). */
    val FUENTES_IMAGEN = listOf("todas", "Steam", "SteamGridDB", "libretro", "ScreenScraper", "IGDB", "TheGamesDB", "RAWG", "Wikipedia")
    fun nombreFuente(f: String) = if (f == "todas") "Todas" else f

    /** Lo que se ve en la tienda: atajos (mis archivos, quitar, conectar SteamGridDB) y las imágenes de la fuente elegida. */
    fun candidatosVisibles(e: Estado = estado.value): List<cl.nikalbazu.diptico.data.ImagenWeb> {
        val j = juegoFicha()
        val d = j?.let { e.datos[it.clave] }
        val cp = e.consolasPropias[e.consolaEditando]
        val propia = when (e.candidatoPara) {
            "arte" -> d?.arte; "logo" -> d?.logo
            "c_hero" -> cp?.hero; "c_logo" -> cp?.logo; "c_icono" -> e.iconosPropios[e.consolaEditando]
            "c_portada" -> cp?.portadaCarpeta
            else -> d?.portada
        }
        val atajos = buildList {
            add(cl.nikalbazu.diptico.data.ImagenWeb("accion:archivos", "", "Mis archivos"))
            if (propia != null) add(cl.nikalbazu.diptico.data.ImagenWeb("accion:quitar", propia, "Quitar la propia"))
            if (e.claveSgdb.isBlank()) add(cl.nikalbazu.diptico.data.ImagenWeb("accion:sgdb", "", "Conectar SteamGridDB"))
            if (!e.candidatoPara.startsWith("c_") || e.candidatoPara == "c_portada") add(cl.nikalbazu.diptico.data.ImagenWeb("accion:fuentes", "", "Configurar fuentes"))
        }
        val lista = if (e.fuenteImagen == "todas") e.candidatos else e.candidatos.filter { it.fuente == e.fuenteImagen }
        return atajos + lista
    }

    fun cambiarFuenteImagen(d: Int) {
        val l = fuentesImagen()
        actualizar { it.copy(fuenteImagen = l[(l.indexOf(it.fuenteImagen) + d).mod(l.size)], indice = 0) }
    }

    private var busquedaImagenes: Job? = null

    private fun juegoDeCarpetaParaArte(e: Estado, nombre: String?): Juego? {
        val juegos = if (e.consolaEditando == "todo") e.juegos else e.juegosPorConsola[e.consolaEditando].orEmpty()
        val consulta = nombre?.let(Busqueda::normalizar)
        if (!consulta.isNullOrEmpty()) {
            juegos.firstOrNull { Busqueda.normalizar(it.titulo) == consulta }?.let { return it }
        }
        return juegos.firstOrNull { e.datos[it.clave]?.oculto != true }
    }

    private fun lanzarBusquedaImagenes(
        tipo: String,
        titulo: String,
        iniciales: List<cl.nikalbazu.diptico.data.ImagenWeb> = emptyList(),
        buscar: () -> List<cl.nikalbazu.diptico.data.ImagenWeb>,
    ) {
        busquedaImagenes?.cancel()
        actualizar { it.copy(panel = Panel.IMAGENES, candidatos = iniciales, candidatoPara = tipo,
            buscandoCandidatos = true, indice = 0, nombreImagen = titulo, fuenteImagen = "todas") }
        busquedaImagenes = scope.launch {
            val lista = withContext(Dispatchers.IO) { runCatching(buscar).getOrDefault(emptyList()) }
            if (estado.value.panel == Panel.IMAGENES) {
                actualizar { it.copy(candidatos = (iniciales + lista).distinctBy { it.url }, buscandoCandidatos = false) }
            }
        }
    }

    fun buscarCandidatos(tipo: String, nombre: String? = null) {
        val e = estado.value
        val consulta = nombre?.takeIf { it.isNotBlank() }
        if (tipo.startsWith("c_")) {
            val consola = Consolas.porId(e.consolaEditando)
            if (consola == null && tipo != "c_portada") return
            val titulo = consulta ?: consola?.bonito ?: "Todo"
            val juego = if (tipo == "c_portada") juegoDeCarpetaParaArte(e, consulta) else null
            val iniciales = if (tipo == "c_portada" && consola != null) {
                Descargas.candidatosConsola(consola, "c_icono", titulo, "", null)
            } else emptyList()
            lanzarBusquedaImagenes(tipo, titulo, iniciales) {
                val wiki = if (tipo == "c_icono" || consola == null && consulta == null) null
                else runCatching { Fichas.buscar(app, titulo, "") }.getOrNull()
                val plataforma = consola?.let { Descargas.candidatosConsola(it, tipo, titulo, e.claveSgdb, wiki) }
                    ?: listOfNotNull(wiki?.principal)
                val sistema = juego?.let { Consolas.porId(it.consolaId) }
                if (juego == null || sistema == null) plataforma
                else {
                    val nombreJuego = consulta ?: juego.titulo
                    val muestra = if (Busqueda.normalizar(nombreJuego) == Busqueda.normalizar(juego.titulo)) juego
                    else juego.copy(clave = "${juego.consolaId}/busqueda-icono", archivo = nombreJuego, titulo = nombreJuego)
                    plataforma + runCatching {
                        Descargas.candidatos(app, muestra, sistema, "portada", nombreJuego, e.claveSgdb, null, e.raspadores)
                    }.getOrDefault(emptyList())
                }
            }
            return
        }
        val j = juegoFicha() ?: return
        val crudo = todosCrudos().firstOrNull { it.clave == j.clave } ?: return
        val c = Consolas.porId(crudo.consolaId) ?: return
        val titulo = consulta ?: j.titulo
        lanzarBusquedaImagenes(tipo, titulo) {
            val wiki = if (nombre == null) e.ficha ?: Fichas.buscar(app, titulo, c.bonito) else Fichas.buscar(app, titulo, c.bonito)
            Descargas.candidatos(app, crudo, c, tipo, titulo, e.claveSgdb, wiki, e.raspadores)
        }
    }

    /** Usa la opción elegida de la tienda: la baja y la deja como imagen propia del juego (o hace el atajo). */
    fun usarCandidato(i: Int = estado.value.indice) {
        val e = estado.value
        val img = candidatosVisibles(e).getOrNull(i) ?: return
        if (e.candidatoPara.startsWith("c_")) { usarCandidatoConsola(img); return }
        val j = juegoFicha() ?: return
        val tipo = e.candidatoPara
        when (img.url) {
            "accion:archivos" -> { pedirImagen(tipo); return }
            "accion:fuentes" -> { abrirMenu(Seccion.PORTADAS); return }
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
        "ras_igdbId", "ras_igdbSecreto" -> "https://dev.twitch.tv/console/apps/create"
        "ras_ssUsuario", "ras_ssClave" -> "https://www.screenscraper.fr/membreinscription.php"
        "ras_ssDevId", "ras_ssDevClave" -> "https://www.screenscraper.fr/forumsujets.php?frub=12&numpage=0"
        "ras_rawg" -> "https://rawg.io/apidocs"
        "ras_tgdb" -> "https://forums.thegamesdb.net/viewforum.php?f=10"
        "ia_clave_gemini", "gemini" -> "https://aistudio.google.com/apikey"
        "ia_clave_openai", "openai" -> "https://platform.openai.com/api-keys"
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
            "reporte_fallas" -> { confirmarReporte(t); return }
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
            "coleccion_nueva" -> {
                crearColeccion(t.ifBlank { "Mi colección" }, e.seleccionado?.clave)
                return
            }
            "ras_igdbId", "ras_igdbSecreto", "ras_ssUsuario", "ras_ssClave", "ras_ssDevId", "ras_ssDevClave", "ras_rawg", "ras_tgdb" -> {
                guardarRaspador(e.textoPara.removePrefix("ras_"), e.borrador.trim())
                return
            }
            "buscar_juego" -> {
                actualizar { it.copy(panel = Panel.BUSCAR, busqueda = t.take(40), indice = 0) }
                return
            }
            "pc_steam" -> {
                actualizar { it.copy(panel = Panel.MENU, seccion = Seccion.BIBLIOTECA, enOpciones = true) }
                buscarEnSteam(t)
                return
            }
            "bisa_paso" -> {
                val clave = pasoDeBisa
                pasoDeBisa = null
                actualizar { it.copy(panel = Panel.ASISTENTE, indice = 0) }
                if (clave != null && CentroBisa.contexto(e).juego?.clave == clave) {
                    val datos = estado.value.datos.toMutableMap()
                    datos[clave] = (datos[clave] ?: DatosJuego()).copy(paso = t)
                    almacen.guardarDatos(datos)
                    actualizar { it.copy(datos = datos) }
                    respuestaBisa(MensajeChat("ia", if (t.isBlank()) "Quité tu próximo paso." else "Guardé tu próximo paso: $t"))
                }
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
            "ia_clave_gemini", "ia_clave_openai" -> {
                if (e.textoPara == "ia_clave_gemini") { almacen.guardarTexto("claveGemini", t); actualizar { it.copy(claveGemini = t) } }
                else { almacen.guardarTexto("claveOpenAI", t); actualizar { it.copy(claveOpenAI = t) } }
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
            "tema_enlace" -> {
                cerrarPanel()
                if (t.isNotBlank()) importarTemas { TiendaTemas.catalogo(t) }
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
        imagenVisual = null
        archivoPara = para
        pedirArchivo?.invoke(tipo) ?: avisar("No pude abrir el selector de archivos")
    }

    var pedirExportarPartida: ((String) -> Unit)? = null
    private var copiaParaExportar: java.io.File? = null
    private var opcionesCopiasPartida: List<ItemGaleria> = emptyList()

    private fun abrirCopiasPartida(clave: String) {
        val j = estado.value.juegos.firstOrNull { it.clave == clave } ?: return
        scope.launch {
            val copias = withContext(Dispatchers.IO) { cl.nikalbazu.diptico.data.CopiasPartida.listar(app, clave) }
            opcionesCopiasPartida = listOf(ItemGaleria("copia-nueva", "Guardar copia .srm",
                "Selecciona un guardado. Se conservan las últimas tres copias.", false) { pedirArchivoPara("partida-srm:$clave", "*/*") }) +
                copias.mapIndexed { i, archivo ->
                    val fecha = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(archivo.lastModified()))
                    ItemGaleria(archivo.name, "Recuperar copia ${i + 1}", fecha, false) {
                        mostrarDialogo(Dialogo("exportar-srm", "¿Recuperar esta copia?",
                            "Elige dónde guardar el archivo .srm recuperado. El guardado del emulador queda en su ubicación actual.",
                            Icono.COPIAR, si = "Elegir destino", no = "Cancelar")) {
                            copiaParaExportar = archivo
                            val base = archivoParaJugar(j, estado.value).archivo.substringBeforeLast('.').replace('/', '_')
                            pedirExportarPartida?.invoke("$base.srm")
                        }
                    }
                }
            abrirGaleria("copias_srm")
        }
    }

    fun exportarPartidaElegida(uri: Uri?) {
        val archivo = copiaParaExportar
        copiaParaExportar = null
        if (uri == null || archivo == null) return
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    requireNotNull(app.contentResolver.openOutputStream(uri, "wt")).use { salida ->
                        archivo.inputStream().use { it.copyTo(salida) }
                    }
                }.isSuccess
            }
            avisar(if (ok) "Copia .srm recuperada" else "No pude guardar la copia")
        }
    }

    private var capturaPara: Pair<String, String>? = null

    fun imagenCaptura(e: Estado, id: String): Uri? {
        if (e.galeria == "captura_acciones") return capturaPara?.second?.let(Uri::parse)
        if (e.galeria != "capturas_juego") return null
        val clave = e.seleccionado?.clave ?: return null
        val i = id.removePrefix("captura-").toIntOrNull() ?: return null
        return e.datos[clave]?.capturas?.getOrNull(i)?.let(Uri::parse)
    }

    private fun accionesCaptura(clave: String, ruta: String) {
        capturaPara = clave to ruta
        abrirGaleria("captura_acciones")
    }

    private fun compartirCaptura(ruta: String) {
        val uri = Uri.parse(ruta)
        val enviar = Intent(Intent.ACTION_SEND).setType("image/*")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .also { it.clipData = android.content.ClipData.newRawUri("Captura", uri) }
        abrirIntent(Intent.createChooser(enviar, "Compartir captura"), "No pude compartir la captura")
    }

    private fun importarListaEsDe(uri: Uri) {
        val e = estado.value
        scope.launch {
            val previa = withContext(Dispatchers.IO) {
                runCatching {
                    val entradas = app.contentResolver.openInputStream(uri)?.use(cl.nikalbazu.diptico.data.ImportarEsDe::leer)
                        ?: error("Archivo vacío")
                    cl.nikalbazu.diptico.data.ImportarEsDe.comparar(entradas, e.juegos, e.datos)
                }.getOrNull()
            }
            if (previa == null) { avisar("No pude leer el gamelist.xml de ES-DE"); return@launch }
            if (previa.cambios.isEmpty()) {
                avisar("Sin cambios · ${previa.sinCoincidencia} juegos sin coincidencia única")
                return@launch
            }
            mostrarDialogo(Dialogo("importar-es-de", "Importar lista de ES-DE",
                "${previa.cambios.size} juegos se pueden actualizar. Se omiten ${previa.sinCoincidencia} sin coincidencia única.",
                Icono.DESCARGA, si = "Importar", no = "Cancelar",
                lineas = previa.cambios.take(6).map { it.titulo ?: it.juego.titulo })) {
                val actual = estado.value
                val claves = actual.juegos.mapTo(HashSet()) { it.clave }
                val datos = actual.datos.toMutableMap()
                for (cambio in previa.cambios) {
                    val clave = cambio.juego.clave
                    if (clave !in claves) continue
                    val previo = datos[clave] ?: DatosJuego()
                    datos[clave] = previo.copy(titulo = previo.titulo ?: cambio.titulo, favorito = previo.favorito || cambio.favorito)
                }
                almacen.guardarDatos(datos)
                actualizar { it.copy(datos = datos) }
                publicarJuegos()
                avisar("Lista de ES-DE importada")
            }
        }
    }

    fun archivoElegido(uri: Uri) {
        if (archivoPara.startsWith("portada_carpeta:")) {
            val id = archivoPara.removePrefix("portada_carpeta:")
            archivoPara = ""
            scope.launch {
                val valida = withContext(Dispatchers.IO) {
                    runCatching {
                        require(uri.scheme == "content")
                        requireNotNull(cl.nikalbazu.diptico.ui.Imagenes.cargar(app, uri, 512))
                        app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }.isSuccess
                }
                if (valida) {
                    cambiarConsolaPropia(id) { it.copy(portadaCarpeta = uri.toString()) }
                    avisar("Portada de carpeta guardada")
                } else avisar("No pude abrir o conservar esa imagen")
            }
            return
        }
        if (archivoPara == "temaJson") {
            archivoPara = ""
            importarTemas { TiendaTemas.archivo(app, uri) }
            return
        }
        if (archivoPara.startsWith("partida-srm:")) {
            val clave = archivoPara.removePrefix("partida-srm:")
            archivoPara = ""
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching { cl.nikalbazu.diptico.data.CopiasPartida.guardar(app, clave, uri) }.isSuccess
                }
                if (ok) {
                    avisar("Copia .srm guardada")
                    abrirCopiasPartida(clave)
                } else avisar("Elige un guardado .srm legible de hasta 16 MB")
            }
            return
        }
        if (archivoPara.startsWith("captura:")) {
            val clave = archivoPara.removePrefix("captura:")
            archivoPara = ""
            try {
                require(uri.scheme == "content")
                app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { avisar("No pude conservar el acceso a esa captura"); return }
            val datos = estado.value.datos.toMutableMap()
            val d = datos[clave] ?: DatosJuego()
            if (d.capturas.size >= 24 && uri.toString() !in d.capturas) { avisar("La galería admite 24 capturas por juego"); return }
            datos[clave] = d.copy(capturas = (listOf(uri.toString()) + d.capturas).distinct())
            almacen.guardarDatos(datos)
            actualizar { it.copy(datos = datos) }
            avisar("Captura agregada")
            return
        }
        if (archivoPara == "es-de") {
            archivoPara = ""
            importarListaEsDe(uri)
            return
        }
        imagenVisual?.let { (id, tipo) ->
            imagenVisual = null
            try { app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            val v = estado.value.experiencia
            val c = v.colecciones[id] ?: PresentacionColeccion()
            val nuevo = when (tipo) { "musica" -> c.copy(musica = uri.toString()); "logo" -> c.copy(logo = uri.toString()); else -> c.copy(fondo = uri.toString()) }
            guardarExperiencia(v.copy(colecciones = v.colecciones + (id to nuevo)))
            return
        }

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
            archivoPara == "widgetImagen" || archivoPara == "widgetVideo" -> {
                val video = archivoPara == "widgetVideo"
                val w = Widget((if (video) "video-" else "imagen-") + System.currentTimeMillis(), 0, 0, 2, 2, false, estado.value.paginaInicio, u, estilo = "sinfondo")
                guardarWidgets(estado.value.widgets + w)
                val k = estado.value.widgets.size - 1
                val celda = celdaPendiente
                celdaPendiente = null
                if (celda != null) ponerEnCelda(k, celda.first, celda.second) else alternarWidget(k)
                avisar(if (video) "Video agregado a tu inicio" else "Imagen agregada a tu inicio")
            }
            archivoPara.startsWith("widget:") -> {
                val tipo = archivoPara.removePrefix("widget:")
                guardarWidgets(estado.value.widgets.map { if (it.tipo == tipo) it.copy(uri = u) else it })
                avisar("Imagen cambiada")
            }
            archivoPara.startsWith("consola_") -> {
                val (que, id) = archivoPara.removePrefix("consola_").split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                cambiarConsolaPropia(id) {
                    when (que) { "hero" -> it.copy(hero = u); "logo" -> it.copy(logo = u); "musica" -> it.copy(musica = u); else -> it }
                }
                avisar("¡Listo! Cambio guardado")
            }
            archivoPara.startsWith("mapa:") -> {
                val clave = archivoPara.removePrefix("mapa:")
                val m = estado.value.mapas + (clave to MapaJuego(u))
                guardarMapas(m)
                actualizar { it.copy(marcandoMapa = clave, pinX = 0.5f, pinY = 0.5f) }
                avisar("Ahora marca dónde quedaste")
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

    private val VISTAS = listOf("estante", "carrusel", "abanico", "lomos", "cuadricula", "lista")
    fun nombreVista(v: String) = when (v) { "cuadricula" -> "Galería"; "lista" -> "Lista"; "carrusel" -> "Carrusel"; "abanico" -> "Abanico"; "lomos" -> "Lomos"; else -> "Estante" }

    val VISTAS_CONSOLAS = listOf("carpetas", "mosaico", "vitrina", "arriba", "sistemas", "lado", "rueda", "discos")
    /** Vistas donde primero se elige la consola (en grande) y después se entra a sus juegos. */
    val VISTAS_CARPETAS = setOf("carpetas", "mosaico", "vitrina")
    val MODOS_ELEGIR = VISTAS_CARPETAS + setOf("sistemas", "discos")
    fun nombreVistaConsolas(v: String) = when (v) {
        "lado" -> "Al costado"; "rueda" -> "Rueda"; "sistemas" -> "Sistemas"; "carpetas" -> "Carpetas"
        "mosaico" -> "Mosaico"; "vitrina" -> "Vitrina"; "discos" -> "Discos"; else -> "Arriba"
    }

    /** Cambia cómo se ven las consolas (arriba, al costado, rueda o sistemas). */
    internal fun vistaConsolasCompatible(vista: String): String = when {
        vista == "habitacion" -> "carpetas"
        vista in VISTAS_CONSOLAS -> vista
        else -> "arriba"
    }

    fun elegirVistaConsolas(v: String) {
        val vista = vistaConsolasCompatible(v)
        almacen.guardarTexto("vistaConsolas", vista)
        actualizar { it.copy(vistaConsolas = vista, enSistemas = vista in MODOS_ELEGIR, widgetFoco = -1) }
        avisar("Consolas: ${nombreVistaConsolas(vista)}")
    }

    fun cambiarVistaConsolas(d: Int = 1) = elegirVistaConsolas(VISTAS_CONSOLAS[(VISTAS_CONSOLAS.indexOf(estado.value.vistaConsolas) + d).mod(VISTAS_CONSOLAS.size)])

    fun alternarModoCarpeta() {
        val nuevo = if (estado.value.modoCarpeta == "completa") "ventana" else "completa"
        almacen.guardarTexto("modoCarpeta", nuevo)
        actualizar { it.copy(modoCarpeta = nuevo) }
    }

    fun alternarRecorridoCarpetas() {
        val horizontal = !estado.value.carpetasHorizontal
        almacen.guardarBool("carpetasHorizontal", horizontal)
        actualizar { it.copy(carpetasHorizontal = horizontal) }
    }

    private val TAMANOS_CARPETAS = listOf(.65f, 1f, 1.3f, 1.6f, 1.9f)

    internal fun limitarTamCarpetas(tam: Float): Float =
        if (tam.isFinite()) tam.coerceIn(TAMANOS_CARPETAS.first(), TAMANOS_CARPETAS.last()) else 1f

    fun guardarTamCarpetas(tam: Float) {
        val nuevo = limitarTamCarpetas(tam)
        if (nuevo == estado.value.tamCarpetas) return
        almacen.guardarTexto("tamCarpetas", nuevo.toString())
        actualizar { it.copy(tamCarpetas = nuevo) }
    }

    private fun cambiarTamCarpetas(agrandar: Boolean) {
        val actual = estado.value.tamCarpetas
        val tam = if (agrandar) TAMANOS_CARPETAS.firstOrNull { it > actual + .01f } ?: TAMANOS_CARPETAS.first()
        else TAMANOS_CARPETAS.lastOrNull { it < actual - .01f } ?: TAMANOS_CARPETAS.last()
        guardarTamCarpetas(tam)
    }

    /** Modo sistemas: entra a los juegos de la consola elegida. */
    fun entrarSistema() {
        cl.nikalbazu.diptico.audio.Sonidos.abrir()
        actualizar { it.copy(enSistemas = false) }
    }

    /** Modo sistemas: vuelve al carrusel de consolas. */
    fun volverASistemas() {
        if (estado.value.vistaConsolas in MODOS_ELEGIR) actualizar { it.copy(enSistemas = true, widgetFoco = -1) }
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

    val ORDENES = listOf("az", "recientes", "tiempo", "propio")
    fun nombreOrden(o: String) = when (o) { "recientes" -> "Últimos jugados"; "tiempo" -> "Más tiempo"; "propio" -> "A tu gusto"; else -> "A → Z" }

    // ---------- Tu propio orden ----------

    private fun leerOrdenPropio(t: String): Map<String, List<String>> = try {
        val o = org.json.JSONObject(t)
        o.keys().asSequence().associateWith { k -> o.getString(k).split("\u0001").filter { it.isNotEmpty() } }
    } catch (_: Exception) { emptyMap() }

    private fun guardarOrdenPropio(m: Map<String, List<String>>) {
        val o = org.json.JSONObject()
        m.forEach { (k, v) -> o.put(k, v.joinToString("\u0001")) }
        almacen.guardarTexto("ordenPropio", o.toString())
    }

    /** Empieza a mover el juego elegido de lugar (la cruceta lo corre, A o B terminan). Pasa al orden "A tu gusto". */
    fun empezarOrdenar() {
        val e = estado.value
        if (e.orden != "propio") {
            // Se parte del orden que se ve ahora, para que nada salte.
            val actual = e.visibles.map { it.clave }
            val m = e.ordenPropio + (e.consolaSel to actual)
            guardarOrdenPropio(m)
            almacen.guardarTexto("orden", "propio")
            actualizar { it.copy(orden = "propio", ordenPropio = m) }
        }
        actualizar { it.copy(ordenando = true, panel = Panel.NINGUNO, indice = 0) }
        Sonidos.levantar()
    }

    fun terminarOrdenar() {
        actualizar { it.copy(ordenando = false) }
        Sonidos.soltar()
        avisar("Orden guardado")
    }

    /** Corre el juego elegido [d] lugares (en la lista que se ve). */
    fun moverJuegoDeLugar(d: Int) {
        val e = estado.value
        val sel = e.seleccionado ?: return
        val visto = e.visibles.map { it.clave }.toMutableList()
        val i = visto.indexOf(sel.clave)
        val n = (i + d).coerceIn(0, visto.size - 1)
        if (i < 0 || n == i) { Sonidos.borde(); return }
        visto.removeAt(i)
        visto.add(n, sel.clave)
        // Los que no se ven (por un filtro) conservan su lugar al final.
        val resto = e.ordenPropio[e.consolaSel].orEmpty().filter { it !in visto }
        val m = e.ordenPropio + (e.consolaSel to (visto + resto))
        guardarOrdenPropio(m)
        actualizar { it.copy(ordenPropio = m) }
    }

    /** Cambia el lugar de una consola en la lista (para "Tus consolas" y las vistas de consolas). */
    fun moverConsola(id: String, d: Int) {
        val e = estado.value
        val l = e.consolasConJuegos.map { it.id }.toMutableList()
        val i = l.indexOf(id)
        val n = (i + d).coerceIn(0, l.size - 1)
        if (i < 0 || n == i) { Sonidos.borde(); return }
        l.removeAt(i)
        l.add(n, id)
        almacen.guardarTexto("ordenConsolas", l.joinToString(","))
        actualizar { it.copy(ordenConsolas = l) }
    }

    fun cambiarFiltro(delta: Int = 1) {
        val f = FILTROS[(FILTROS.indexOf(estado.value.filtro) + delta).mod(FILTROS.size)]
        elegirFiltro(f)
    }

    fun elegirFiltro(f: String) = actualizar { e ->
        val anterior = if (f == "ocultos") {
            e.antesDeOcultos ?: e.takeIf { it.filtro != "ocultos" }?.antesDeFiltrarOcultos()
        } else null
        e.copy(filtro = f, antesDeOcultos = anterior, indice = 0)
    }

    private fun Estado.antesDeFiltrarOcultos() = BibliotecaAntesDeOcultos(filtro, busqueda, consolaSel, juegoSel, enSistemas, inicio)

    fun abrirJuegosOcultos() = actualizar { e ->
        val anterior = e.antesDeOcultos ?: e.takeIf { it.filtro != "ocultos" }?.antesDeFiltrarOcultos()
        e.copy(panel = Panel.BUSCAR, filtro = "ocultos", antesDeOcultos = anterior, busqueda = "", consolaSel = "todo", indice = 0)
    }

    fun salirDeOcultos(cerrarBusqueda: Boolean = false) = actualizar { e ->
        if (e.filtro != "ocultos") return@actualizar e
        val anterior = e.antesDeOcultos
        e.copy(
            panel = if (cerrarBusqueda) Panel.NINGUNO else e.panel,
            filtro = anterior?.filtro ?: "todos",
            busqueda = anterior?.busqueda.orEmpty(),
            consolaSel = anterior?.consola ?: e.consolaSel,
            juegoSel = anterior?.juego,
            enSistemas = anterior?.enSistemas ?: e.enSistemas,
            inicio = anterior?.inicio ?: e.inicio,
            antesDeOcultos = null,
            indice = 0,
        )
    }

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

    fun limpiarBusqueda() = actualizar { it.copy(busqueda = "", filtro = "todos", antesDeOcultos = null, indice = 0) }

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

    val FORMAS = listOf("original", "cuadrada", "poster", "horizontal", "disco", "cartucho", "tarjeta", "panoramica", "instantanea", "cassette")
    fun nombreForma(f: String) = when (f) {
        "cuadrada" -> "Cuadrada"; "poster" -> "Póster"; "horizontal" -> "Horizontal"; "disco" -> "Disco"
        "cartucho" -> "Cartucho"; "tarjeta" -> "Tarjeta"; "panoramica" -> "Panorámica"
        "instantanea" -> "Instantánea"; "cassette" -> "Casete"; else -> "Caja original"
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

    /** La consola a la que se le cambian las cajas: la elegida, o la del juego elegido si estás en "Todo". */
    fun consolaParaCajas(e: Estado = estado.value): String =
        e.consolaSel.takeIf { it != "todo" } ?: e.seleccionado?.consolaId ?: e.consolasConJuegos.firstOrNull()?.id ?: ""

    /** Galería visual de las formas de caja: cada forma con tus portadas, para esta consola o todas. */
    fun abrirCajas(consola: String? = null) {
        actualizar { it.copy(galeriaConsola = consola ?: consolaParaCajas(it)) }
        abrirGaleria("cajas")
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
                "Vista de consolas", nombreVistaConsolas(e.vistaConsolas), Icono.MANDO,
                "Arriba como pestañas, en una columna al costado, en una rueda que gira, Sistemas (un carrusel grande), Carpetas (cuadrícula de íconos que abren sus juegos) o Discos (cada consola es un vinilo en su funda, y la elegida saca su disco girando). En Sistemas, Carpetas y Discos primero eliges la consola y después entras a sus juegos.",
                menos = { cambiarVistaConsolas(-1) },
            ) { abrirGaleria("vistas_consolas") },
            Opcion("Abrir carpetas", if (e.modoCarpeta == "completa") "Pantalla completa" else "Ventana con desenfoque", Icono.CARPETA,
                "Elige cómo se abren los juegos en la vista Carpetas.") { alternarModoCarpeta() },
            Opcion("Recorrido de carpetas", if (e.carpetasHorizontal) "Horizontal" else "Vertical", Icono.VISTA,
                "Desplaza la cuadrícula hacia los lados o hacia arriba y abajo.") { alternarRecorridoCarpetas() },
            Opcion("Tamaño de carpetas", "${(e.tamCarpetas * 100 + .5f).toInt()} %", Icono.FORMA,
                "También puedes juntar o separar dos dedos sobre la cuadrícula.",
                menos = { cambiarTamCarpetas(false) }) { cambiarTamCarpetas(true) },
            Opcion("Vista de juegos", nombreVista(e.vista), Icono.VISTA, "Estante con cajas, carrusel en 3D, abanico (como cartas abiertas en la mano: la elegida sube al centro), lomos (como una repisa de libros: el elegido se abre), cuadrícula de carátulas o lista. Se cambia rápido con R3 (apretar el stick derecho).") { abrirGaleria("vistas_juegos") },
            Opcion("Forma de las cajas", nombreForma(e.formas[consolaParaCajas()]?.forma ?: "original"), Icono.FORMA,
                "Elige cómo se ven las cajas (cuadrada, póster, horizontal, disco, cartucho o tarjeta) viendo tus portadas en cada forma. Para una consola o para todas.") { abrirCajas() },
            Opcion("Tamaño de los juegos", nombreTamJuegos(e.tamJuegos), Icono.FORMA, "Más chico para ver más juegos, o más grande para verlos mejor.") { cambiarTamJuegos(if (e.tamJuegos >= 1.39f) -3 else 1) },
            Opcion("Mostrar", nombreFiltro(e.filtro), Icono.CORAZON, "Todos, solo favoritos, jugados hace poco, los más jugados o los que no has jugado.") { cambiarFiltro() },
            Opcion("Ordenar", nombreOrden(e.orden), Icono.MENU, "A → Z, últimos jugados o más tiempo jugado.") { cambiarOrden() },
            Opcion("Buscar un juego", icono = Icono.BUSCAR, ayuda = "También con L3 (apretar el stick izquierdo).") { abrirBuscar() },
        )
        if (c != null) {
            val f = e.formas[c.id] ?: FormaCfg()
            lista += Opcion("Cajas de ${c.nombre}: forma", nombreForma(f.forma), Icono.FORMA, "Elige la forma de las cajas de esta consola, viendo tus portadas en cada una.") { abrirCajas(c.id) }
            lista += Opcion("Cajas de ${c.nombre}: tamaño", nombreTamJuegos(f.tam), Icono.FORMA, ayuda = "Más chicas para ver más juegos de esta consola, o más grandes para verlos mejor.") { ciclarTamForma(c.id) }
            lista += Opcion("Cajas de ${c.nombre}: imagen", nombreImagenCaja(f.imagen), Icono.PORTADA, "Muestra la portada, una captura del juego o su logo.") { ciclarImagenCaja(c.id) }
        }
        lista += Opcion("Cajas en «Todos»", nombreFormaTodos(e.formaTodos), Icono.FORMA,
            "Cuando ves todos tus juegos juntos: todas las cajas iguales (póster o cuadradas) para que se vea parejo, o cada consola con su forma.") { ciclarFormaTodos() }
        lista += Opcion("Imagen en todas las cajas", nombreImagenCaja(e.formas.values.firstOrNull()?.imagen ?: "portada"), Icono.PORTADA,
            "Portada, captura del juego o logo, para las cajas de todas tus consolas a la vez.") { imagenTodasLasCajas() }
        val mas = Opcion(if (e.estanteAvanzado) "Ver menos opciones" else "Más opciones de cajas", icono = Icono.AJUSTES) { actualizar { it.copy(navegacion = it.navegacion.copy(estanteAvanzado = !it.estanteAvanzado)) } }
        return (if (e.estanteAvanzado) lista else lista.take(9)) + mas
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

    /**
     * Abre la cortina de arriba: [cual] "avisos" (notificaciones) o "ajustes" (ajustes rápidos), como en los
     * teléfonos Samsung o Xiaomi. Sin [cual], la de ajustes. Si ya estaba abierta en esa misma, se cierra.
     */
    fun abrirCentro(cual: String = "ajustes") {
        Medios.revisar(app)
        val e = estado.value
        if (e.panel == Panel.CENTRO && e.cortina == cual) { cerrarPanel(); return }
        actualizar { it.copy(panel = Panel.CENTRO, cortina = cual, indice = 0) }
        Sonidos.deslizar()
    }

    /** L1/R1 (o tocar la pestaña): pasa de notificaciones a ajustes y al revés. */
    fun cambiarCortina(cual: String? = null) {
        val e = estado.value
        val v = cual ?: if (e.cortina == "avisos") "ajustes" else "avisos"
        if (v == e.cortina) return
        actualizar { it.copy(cortina = v, indice = 0) }
        Sonidos.pagina()
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
        val i = e.indice
        if (e.cortina == "avisos") {
            // Notificaciones: una lista; ← → pasan a los ajustes.
            if (dx != 0) { cambiarCortina("ajustes"); return }
            val total = Avisos.lista.value.size
            if (total == 0) return
            val nuevo = (i + dy).coerceIn(0, total - 1)
            if (nuevo != i) actualizar { it.copy(indice = nuevo) }
            return
        }
        val n = opcionesCentro().size
        if (n == 0) return
        val nuevo = when {
            dx != 0 -> (i + dx).coerceIn(0, n - 1)
            dy < 0 -> if (i - COLUMNAS_CENTRO >= 0) i - COLUMNAS_CENTRO else i
            else -> if (i + COLUMNAS_CENTRO < n) i + COLUMNAS_CENTRO else i
        }
        if (nuevo != i) actualizar { it.copy(indice = nuevo) }
    }

    fun opcionesCentro(): List<Opcion> = opcionesSistema() +
        appsDeConsola().map { a -> Opcion(a.nombre, "Consola", Icono.MANDO, paquete = a.paquete) { abrirPaquete(a.paquete) } } +
        appsMusica().map { a -> Opcion(a.nombre, "Música", Icono.MUSICA, paquete = a.paquete) { abrirPaquete(a.paquete) } }

    /** Cómo están ahora los accesos que se encienden y apagan (para que se note cuáles están activos). */
    private fun wifiEncendido(): Boolean? = try { (app.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager)?.isWifiEnabled } catch (_: Exception) { null }
    private fun bluetoothEncendido(): Boolean? = try { app.getSystemService(android.bluetooth.BluetoothManager::class.java)?.adapter?.isEnabled } catch (_: Exception) { null }
    private fun conSonido(): Boolean? = try { (app.getSystemService(android.media.AudioManager::class.java)?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 0) > 0 } catch (_: Exception) { null }
    private fun brilloAuto(): Boolean? = try { android.provider.Settings.System.getInt(app.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE) == 1 } catch (_: Exception) { null }
    private fun noMolestarActivo(): Boolean = try {
        (app.getSystemService(android.app.NotificationManager::class.java)?.currentInterruptionFilter ?: 1) > android.app.NotificationManager.INTERRUPTION_FILTER_ALL
    } catch (_: Exception) { false }

    fun alternarNoMolestar() {
        if (!Turbo.puedeNoMolestar(app)) { Turbo.pedirNoMolestar(app); avisar("Activa Díptico en la lista y vuelve"); return }
        val nm = app.getSystemService(android.app.NotificationManager::class.java) ?: return
        val activar = !noMolestarActivo()
        try { nm.setInterruptionFilter(if (activar) android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY else android.app.NotificationManager.INTERRUPTION_FILTER_ALL) } catch (_: Exception) {}
        avisar(if (activar) "No molestar: encendido" else "No molestar: apagado")
        actualizar { it.copy(centroVersion = it.centroVersion + 1) }
    }

    private fun opcionesSistema(): List<Opcion> = listOf(
        Opcion("Wi-Fi", icono = Icono.WIFI, activa = wifiEncendido()) {
            abrirIntent(Intent(android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY), "No pude abrir el Wi-Fi")
        },
        Opcion("Bluetooth", icono = Icono.BLUETOOTH, activa = bluetoothEncendido()) { abrirIntent(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS), "No pude abrir el Bluetooth") },
        Opcion("Volumen", icono = Icono.VOLUMEN, activa = conSonido()) { abrirIntent(Intent(android.provider.Settings.Panel.ACTION_VOLUME), "No pude abrir el volumen") },
        Opcion("Pantalla", icono = Icono.BRILLO, activa = brilloAuto()) { abrirIntent(Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS), "No pude abrir la pantalla") },
        Opcion("Turbo", icono = Icono.CHISPA, activa = estado.value.turbo) { alternarTurbo(); actualizar { it.copy(centroVersion = it.centroVersion + 1) } },
        Opcion("No molestar", icono = Icono.SILENCIO, activa = noMolestarActivo()) { alternarNoMolestar() },
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
        if (e.cortina != "avisos") return
        Avisos.lista.value.getOrNull(e.indice)?.let { Avisos.descartar(it) }
        val quedan = Avisos.lista.value.size
        if (e.indice >= quedan && quedan > 0) actualizar { it.copy(indice = quedan - 1) }
    }

    private fun opcionesConsola(e: Estado): List<Opcion> = listOf(
        Opcion("Ajustes rápidos", "Wi-Fi, Bluetooth, brillo…", Icono.AJUSTES, "Wi-Fi, Bluetooth, volumen, brillo, turbo y apps de la consola. Desliza hacia abajo desde la esquina derecha de arriba, o mantén Start.") { abrirCentro("ajustes") },
        Opcion("Notificaciones", "${Avisos.lista.value.size}", Icono.CAMPANA, "Tus notificaciones. Desliza hacia abajo desde la esquina izquierda de arriba, o toca la campana.") { abrirCentro("avisos") },
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
        Opcion("Ajustes de Android", icono = Icono.AJUSTES, ayuda = "Abre los ajustes de Android.") { abrirIntent(Intent(android.provider.Settings.ACTION_SETTINGS), "No pude abrir los ajustes") },
    )

    // ---------- Reconocer los nombres de los juegos ----------

    /** Busca el nombre oficial de los juegos en la lista de libretro (sin bajar imágenes). */
    fun identificarNombres(avisarAlFinal: Boolean = false) {
        if (!estado.value.titulosBonitos) return
        // Solo las consolas que libretro conoce (los fan games y los ports ya traen su nombre): así no se reintenta en cada arranque.
        val pendientes = crudos.filter { medios[it.clave]?.titulo == null && Consolas.porId(it.consolaId)?.libretro != null }
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
    fun musicaDeTema(t: Tema): String = (if (::app.isInitialized) TiendaTemas.pista(app, t) else null) ?: pistaIncluidaDeTema(t)

    internal fun pistaIncluidaDeTema(t: Tema): String = t.pistaBase.takeIf { id -> Musica.pistas.any { it.id == id } } ?: when (t.id.removePrefix("fiesta_")) {
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
        if (l.isEmpty()) {
            crudos = emptyList()
            accesosAndroid = emptyMap()
            publicarJuegos()
        }
        avisar("Carpeta quitada")
        escanear()
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
        val e = estado.value
        // El perfil de sobremesa puede activar efectos. El reposo durante el juego tiene prioridad.
        val ahorro = fluidezEfectiva(e) == "ahorro" && !(e.sobremesa && e.sobremesaEfectos)
        val efectos = !ahorro && !(e.sesion != null && e.turbo && e.turboReposo)
        cl.nikalbazu.diptico.ui.Estilo.efectosCompletos = efectos
        cl.nikalbazu.diptico.ui.Estilo.decoraciones = efectos && e.decoraciones && e.experiencia.movimiento && e.sesion == null
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
        actualizar { it.copy(widgetSel = i, panel = Panel.ESTUDIO, estudioPestana = 0, indice = 0, estudioZona = "panel", celdaCatalogo = null) }
    }

    /** En el editor: pasar de elegir widgets en el inicio a las opciones del panel (y de vuelta). */
    fun zonaEstudio(z: String) {
        if (estado.value.estudioZona == z) return
        actualizar { it.copy(estudioZona = z, estudioMover = false, indice = if (z == "panel") 0 else it.indice) }
        Sonidos.pagina()
    }

    /** En el editor: elegir con la cruceta el widget que está hacia ese lado (libre, sin un orden fijo). */
    fun elegirWidgetDir(dx: Int, dy: Int) {
        val e = estado.value
        if (e.widgets.getOrNull(e.widgetSel)?.let { it.visible && it.pagina == e.paginaInicio } != true) {
            widgetsDePagina(e).firstOrNull()?.let { elegirWidget(it) } ?: Sonidos.borde()
            return
        }
        val k = vecinoWidget(e, e.widgetSel, dx, dy)
        when {
            k != null -> actualizar { it.copy(widgetSel = k) }
            dx != 0 && e.paginaInicio + dx in 0 until e.paginas -> {
                irAPagina(e.paginaInicio + dx)
                val l = widgetsDePagina()
                (if (dx > 0) l.minByOrNull { estado.value.widgets[it].x } else l.maxByOrNull { estado.value.widgets[it].x })?.let { elegirWidget(it) }
                Sonidos.pagina()
            }
            else -> Sonidos.borde()
        }
    }

    /** El widget más cercano hacia (dx, dy) desde el widget [desde], en la página actual. */
    private fun vecinoWidget(e: Estado, desde: Int, dx: Int, dy: Int): Int? {
        val w = e.widgets.getOrNull(desde) ?: return null
        val cx = w.x + w.an / 2f
        val cy = w.y + w.al / 2f
        return widgetsDePagina(e).filter { it != desde }.mapNotNull { k ->
            val o = e.widgets[k]
            val ox = o.x + o.an / 2f
            val oy = o.y + o.al / 2f
            val principal = if (dx != 0) (ox - cx) * dx else (oy - cy) * dy
            val haciaAlla = if (dx != 0) (if (dx > 0) o.x >= w.x + w.an - 0.01f else o.x + o.an <= w.x + 0.01f) else (if (dy > 0) o.y >= w.y + w.al - 0.01f else o.y + o.al <= w.y + 0.01f)
            if (principal <= 0f || !haciaAlla) null
            else k to (principal + 2f * (if (dx != 0) kotlin.math.abs(oy - cy) else kotlin.math.abs(ox - cx)))
        }.minByOrNull { it.second }?.first
    }

    // ---------- Burbuja para agregar widgets ----------

    data class OpcionBurbuja(val nombre: String, val icono: Icono, val indice: Int, val especial: String = "")

    /** Lo que se puede agregar: los widgets que no están a la vista, más imagen/GIF y video. */
    fun opcionesBurbuja(e: Estado = estado.value): List<OpcionBurbuja> =
        e.widgets.withIndex().filter { !it.value.visible && !it.value.tipo.startsWith("imagen") && !it.value.tipo.startsWith("video") }
            .map { (k, w) -> OpcionBurbuja(Widgets.nombre(w.tipo), Widgets.icono(w.tipo), k) } +
            OpcionBurbuja("Imagen o GIF", Icono.PORTADA, -1, "imagen") + OpcionBurbuja("Video corto", Icono.JUGAR, -1, "video") +
            OpcionBurbuja("Acceso a un juego", Icono.CONTINUAR, -1, "atajo") + OpcionBurbuja("Colección nueva", Icono.CARPETA, -1, "coleccion")

    /** Abre la burbuja en la celda (x, y): con el dedo (manteniendo) o con el control. */
    fun abrirBurbuja(x: Int, y: Int) {
        val e = estado.value
        if (!e.editandoWidgets) return
        val celda = x.coerceIn(0, Widgets.COLUMNAS - 1) to y.coerceIn(0, Widgets.FILAS - 1)
        if (!e.unaPantalla) abrirCatalogoWidgets(celda)
        else actualizar { it.copy(burbuja = celda, burbujaSel = 0, estudioMover = false) }
        Sonidos.abrir()
    }

    /** La burbuja junto al widget elegido (con el control). */
    fun abrirBurbujaAqui() {
        val e = estado.value
        val w = e.widgets.getOrNull(e.widgetSel)?.takeIf { it.visible && it.pagina == e.paginaInicio }
        abrirBurbuja(w?.x ?: 0, w?.y ?: 0)
    }

    fun cerrarBurbuja() {
        if (estado.value.burbuja == null) return
        actualizar { it.copy(burbuja = null) }
        Sonidos.atras()
    }

    fun moverBurbuja(d: Int) {
        val n = opcionesBurbuja().size
        if (n == 0) return
        actualizar { it.copy(burbujaSel = (it.burbujaSel + d).mod(n)) }
    }

    /** Agrega lo elegido de la burbuja en su celda; si ahí hay otro widget, se corre para dejarle espacio. */
    fun elegirBurbuja(k: Int = estado.value.burbujaSel) {
        val e = estado.value
        val (cx, cy) = e.burbuja ?: return
        val o = opcionesBurbuja(e).getOrNull(k) ?: return
        actualizar { it.copy(burbuja = null) }
        when (o.especial) {
            "imagen" -> { celdaPendiente = cx to cy; pedirArchivoPara("widgetImagen", "image/*"); return }
            "video" -> { celdaPendiente = cx to cy; pedirArchivoPara("widgetVideo", "video/*"); return }
            "atajo" -> {
                val j = e.seleccionado ?: run { avisar("Primero elige un juego en el estante"); return }
                celdaPendiente = cx to cy; crearAtajo(j.clave); return
            }
            "coleccion" -> { celdaPendiente = cx to cy; abrirTexto("coleccion_nueva"); return }
        }
        ponerEnCelda(o.indice, cx, cy)
    }

    /** Dónde poner la próxima imagen o video que se elija (si se abrió desde la burbuja). */
    private var celdaPendiente: Pair<Int, Int>? = null

    private fun ponerEnCelda(i: Int, cx: Int, cy: Int) {
        val e = estado.value
        val w = e.widgets.getOrNull(i) ?: return
        val destino = encajar(w.copy(visible = true, x = cx, y = cy, pagina = e.paginaInicio))
        val l = e.widgets.toMutableList()
        when {
            !choca(destino, i) -> { l[i] = destino; guardarWidgets(l) }
            else -> {
                val antes = estado.value.widgetSel
                actualizar { it.copy(widgetSel = i) }
                val empujada = empujando(i, destino)
                if (empujada != null) guardarWidgets(empujada.mapIndexed { k, x -> if (k == i) destino else x })
                else { actualizar { it.copy(widgetSel = antes) }; alternarWidget(i) }
            }
        }
        if (estado.value.widgets.getOrNull(i)?.visible == true) {
            actualizar { it.copy(widgetSel = i) }
            Sonidos.soltar()
            avisar("${Widgets.nombre(w.tipo)} agregado")
        }
    }

    /** Guarda la vista previa del tamaño al soltar la manija. */
    fun redimensionarWidget(i: Int, an: Int, al: Int) {
        val w = estado.value.widgets.getOrNull(i)?.takeIf { it.visible } ?: return
        val c = w.copy(an = an.coerceIn(1, (Widgets.COLUMNAS - w.x).coerceAtLeast(1)),
            al = al.coerceIn(1, (Widgets.FILAS - w.y).coerceAtLeast(1)))
        if (!ubicarWidget(i, c)) avisar("No hay espacio para estirarlo tanto")
    }

    fun abrirCatalogoWidgets(celda: Pair<Int, Int>? = null) {
        if (!estado.value.editandoWidgets) editarWidgets()
        actualizar { it.copy(panel = Panel.ESTUDIO, estudioPestana = 1, indice = 0,
            estudioZona = "panel", estudioMover = false, burbuja = null, celdaCatalogo = celda) }
    }

    fun cambiarPestanaEstudio(d: Int) = actualizar {
        it.copy(estudioPestana = (it.estudioPestana + d).mod(PESTANAS_ESTUDIO.size), indice = 0, estudioMover = false, celdaCatalogo = null)
    }

    fun elegirPestanaEstudio(i: Int) = actualizar { it.copy(estudioPestana = i, indice = 0, estudioMover = false, celdaCatalogo = if (i == 1) it.celdaCatalogo else null) }

    fun alternarMoverWidget() {
        val v = !estado.value.estudioMover
        actualizar { it.copy(estudioMover = v) }
        if (v) Sonidos.levantar() else Sonidos.soltar()
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
        val formas = Widgets.FORMAS.filter { (_, f) -> f.first <= Widgets.COLUMNAS && f.second <= Widgets.FILAS }
        val colores = listOf(0L) + Temas.MUESTRAS
        val l = mutableListOf<Control>()
        l += Control(
            "Widget elegido", "pasos", Widgets.nombre(w.tipo), "Elegido", Widgets.icono(w.tipo),
            ayuda = "También lo eliges con la cruceta directo en tu inicio (B para volver allá).",
            menos = { siguienteWidget(-1) },
        ) { siguienteWidget(1) }
        l += Control(
            if (e.estudioMover) "Moviendo: usa la cruceta" else "Mover", "accion",
            if (e.estudioMover) "Terminar" else "A", "Posición y tamaño", Icono.MANDO,
            ayuda = "Si lo pones sobre otro, el otro se corre. Con el dedo: arrástralo, y estíralo desde su esquina.",
        ) { alternarMoverWidget() }
        l += Control(
            "Forma", "formas", Widgets.nombreTamano(w.an, w.al), "Posición y tamaño",
            elecciones = formas.map { it.first }, formas = formas.map { it.second },
            elegida = formas.indexOfFirst { it.second == (w.an to w.al) },
            menos = { tamanoWidgetSel(-1) }, elegir = { k -> formas.getOrNull(k)?.second?.let { (an, al) -> ponerForma(i, an, al) } },
        ) { tamanoWidgetSel(1) }
        l += Control("Ancho", "pasos", "${w.an} de ${Widgets.COLUMNAS}", "Posición y tamaño", Icono.FORMA, menos = { cambiarTamWidget(-1, 0) }) { cambiarTamWidget(1, 0) }
        l += Control("Alto", "pasos", "${w.al} de ${Widgets.FILAS}", "Posición y tamaño", Icono.FORMA, menos = { cambiarTamWidget(0, -1) }) { cambiarTamWidget(0, 1) }
        l += Control("Página", "pasos", "${w.pagina + 1} de ${e.paginas}", "Posición y tamaño", Icono.VISTA, menos = { moverWidgetPagina(i, -1) }) { moverWidgetPagina(i, 1) }
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
        l += Control("Copiar el estilo a todos", "accion", "Aplicar", "Más", Icono.PALETA, "Todos los widgets quedan con este estilo, color, fondo y esquinas.") {
            val base = estado.value.widgets[i]
            guardarWidgets(estado.value.widgets.map { it.copy(estilo = base.estilo, color = base.color, opacidad = base.opacidad, radio = base.radio) })
            avisar("Estilo copiado a todos los widgets")
        }
        l += Control("Quitar este widget", "accion", "Quitar", "Más", Icono.BORRAR) { quitarWidget(i) }
        return l
    }

    private fun controlesAgregar(e: Estado): List<Control> {
        val tipos = e.widgets.withIndex().filter { (!it.value.tipo.startsWith("imagen") && !it.value.tipo.startsWith("video")) || it.value.visible }
        return tipos.map { (k, w) ->
            Control(
                Widgets.nombre(w.tipo), "catalogo", if (w.visible) "En tu inicio ✓" else "Agregar", if (w.visible) "En tu inicio" else "Para agregar",
                Widgets.icono(w.tipo), ayuda = Widgets.descripcion(w.tipo), extra = w.tipo,
            ) {
                if (w.visible) {
                    actualizar { it.copy(widgetSel = k, estudioPestana = 0, indice = 0, paginaInicio = w.pagina, celdaCatalogo = null) }
                } else if (e.celdaCatalogo != null) {
                    val (columna, fila) = e.celdaCatalogo
                    actualizar { it.copy(celdaCatalogo = null) }
                    ponerEnCelda(k, columna, fila)
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
        ) { celdaPendiente = e.celdaCatalogo; pedirArchivoPara("widgetImagen", "image/*") } + Control(
            "Video corto", "catalogo", "Elegir", "Para agregar", Icono.JUGAR,
            ayuda = "Un clip corto (mp4) que se repite en silencio. Se pausa solo mientras juegas.", extra = "video",
        ) { celdaPendiente = e.celdaCatalogo; pedirArchivoPara("widgetVideo", "video/*") } + listOf(
            Control("Acceso a un juego", "catalogo", "Agregar", "Para agregar", Icono.CONTINUAR,
                ayuda = "Usa el juego elegido en el estante.", extra = "atajo") {
                val juego = estado.value.seleccionado
                if (juego == null) avisar("Primero elige un juego en el estante") else {
                    celdaPendiente = e.celdaCatalogo
                    crearAtajo(juego.clave)
                    celdaPendiente = null
                }
            },
            Control("Colección nueva", "catalogo", "Crear", "Para agregar", Icono.CARPETA,
                ayuda = "Crea una colección y añádela a tu inicio.", extra = "coleccion") {
                celdaPendiente = e.celdaCatalogo
                abrirTexto("coleccion_nueva")
            },
        )
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

    // ---------- Navegación directa, cajón y retoma ----------
    fun cambiarDensidadWidgets(agrandar: Boolean) {
        val e = estado.value
        val cols = (e.gridCols + if (agrandar) -1 else 1).coerceIn(4, 8)
        val filas = (e.gridFilas + if (agrandar) -1 else 1).coerceIn(3, 8)
        if (cols == e.gridCols && filas == e.gridFilas) return
        val r = Rejilla.distribuir(e.widgets, cols, filas, e.paginas)
        almacen.guardarTexto("gridCols", cols.toString()); almacen.guardarTexto("gridFilas", filas.toString())
        almacen.guardarTexto("paginas", r.paginas.toString()); almacen.guardarTexto("widgets", escribirWidgets(r.widgets))
        Widgets.COLUMNAS = cols; Widgets.FILAS = filas
        actualizar { it.copy(gridCols = cols, gridFilas = filas, paginas = r.paginas, widgets = r.widgets, estudioMover = false) }
        avisar("Grilla $cols × $filas · ${r.paginas} páginas")
    }

    fun abrirOpcionesApp(paquete: String) = actualizar { it.copy(navegacion = it.navegacion.copy(appsMenu = paquete, appsMenuIndice = 0, appsMoviendo = null)) }
    fun cerrarOpcionesApp() = actualizar { it.copy(navegacion = it.navegacion.copy(appsMenu = null, appsMoviendo = null)) }
    fun opcionesApp(e: Estado = estado.value, paquete: String? = e.appsMenu): List<Opcion> {
        val pkg = paquete ?: return emptyList()
        return listOf(
            Opcion("Abrir", icono = Icono.JUGAR) { cerrarOpcionesApp(); abrirPaquete(pkg) },
            Opcion(if (pkg in e.appsFijadas) "Quitar de fijadas" else "Fijar app", icono = Icono.ESTRELLA) { alternarFijada(pkg); cerrarOpcionesApp() },
            Opcion("Mover de lugar", icono = Icono.INTERCAMBIAR, ayuda = "Elige otra casilla con el dedo o muévela con la cruceta; A termina.") {
                actualizar { it.copy(navegacion = it.navegacion.copy(appsMoviendo = pkg, appsMenu = null)) }; avisar("Toca su nuevo lugar o usa la cruceta · A listo")
            },
            Opcion("Abrir en otra pantalla", icono = Icono.PANTALLAS) { cerrarOpcionesApp(); elegirPantalla("app:$pkg") },
            Opcion("Opciones de Android", icono = Icono.AJUSTES) { cerrarOpcionesApp(); infoApp(pkg) },
            Opcion("Desinstalar", icono = Icono.BORRAR, ayuda = "Android te pedirá confirmar. Algunas apps del sistema no se pueden desinstalar.") {
                cerrarOpcionesApp()
                abrirIntent(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")), "Android no permite desinstalar esta app")
            },
        )
    }
    fun moverMenuApp(d: Int) { val n = opcionesApp().size; if (n > 0) actualizar { it.copy(navegacion = it.navegacion.copy(appsMenuIndice = (it.appsMenuIndice + d).mod(n))) } }
    fun aceptarMenuApp() { opcionesApp().getOrNull(estado.value.appsMenuIndice)?.accion?.invoke() }
    fun moverAppA(destino: Int) {
        val e = estado.value; val pkg = e.appsMoviendo ?: return
        val visibles = appsVisibles(e).map { it.paquete }.toMutableList()
        val actual = visibles.indexOf(pkg); if (actual < 0 || visibles.isEmpty()) return
        val nuevo = destino.coerceIn(0, visibles.lastIndex)
        visibles.removeAt(actual); visibles.add(nuevo, pkg)
        if (e.appsPestana == 1) {
            almacen.guardarTexto("appsFijadas", visibles.joinToString(",")); actualizar { it.copy(appsFijadas = visibles, indice = nuevo) }
        } else {
            val orden = visibles + e.ordenApps.filter { it !in visibles }
            almacen.guardarTexto("ordenApps", orden.joinToString(",")); actualizar { it.copy(navegacion = it.navegacion.copy(ordenApps = orden), indice = nuevo) }
        }
    }
    fun tocarApp(i: Int) {
        val e = estado.value
        if (e.appsMoviendo != null) { moverAppA(i); cerrarOpcionesApp() }
        else {
            actualizar { it.copy(indice = i) }
            if (e.unaPantalla || i == e.indice) appsVisibles().getOrNull(i)?.let { abrirPaquete(it.paquete) }
        }
    }

    fun alternarClipRetoma() {
        val v = !estado.value.clipRetoma
        almacen.guardarBool("clipRetoma", v); actualizar { it.copy(navegacion = it.navegacion.copy(clipRetoma = v)) }
        if (!v) { cl.nikalbazu.diptico.retoma.RetomaServicio.detener(app); actualizar { it.copy(navegacion = it.navegacion.copy(retomaVideo = "", retomaVideoClave = "")) } }
    }
    fun cambiarEsperaRetoma() {
        val l = listOf(15, 30, 60, 300); val n = l[(l.indexOf(estado.value.esperaRetoma) + 1).mod(l.size)]
        almacen.guardarTexto("esperaRetoma", n.toString()); actualizar { it.copy(navegacion = it.navegacion.copy(esperaRetoma = n)) }
    }
    fun alSuspender() {
        actualizar { it.copy(navegacion = it.navegacion.copy(retomaApagadaEn = System.currentTimeMillis(), retomaVideo = "", retomaVideoClave = "")) }
        cl.nikalbazu.diptico.retoma.RetomaServicio.detener(app)
    }
    fun clipRetomaListo(clave: String, archivo: String) {
        if (!estado.value.clipRetoma || estado.value.sesion?.clave != clave) return
        actualizar { it.copy(navegacion = it.navegacion.copy(retomaVideo = archivo, retomaVideoClave = clave, retomaVideoFecha = System.currentTimeMillis())) }
    }
    fun pedirPermisoRetoma(luegoJugar: Boolean, pantalla: String? = null) {
        val e = estado.value
        val clave = (if (luegoJugar) e.seleccionado?.clave else e.sesion?.clave) ?: return
        if (e.datos[clave]?.lupa == true || cl.nikalbazu.diptico.lupa.LupaServicio.activa) {
            avisar("La lupa tiene prioridad: la retoma usará la imagen del juego")
            if (luegoJugar) jugar(pantalla, conRetomaLista = true)
            return
        }
        pantallaRetomaPendiente = pantalla
        claveRetomaPendiente = if (luegoJugar) clave else null
        cl.nikalbazu.diptico.retoma.RetomaServicio.detener(app)
        try { app.startActivity(Intent(app, cl.nikalbazu.diptico.lupa.PermisoLupaActivity::class.java)
            .putExtra("retoma", true).putExtra("clave", clave).putExtra("luegoJugar", luegoJugar).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { retomaPermisoTerminado(luegoJugar) }
    }
    private var pantallaRetomaPendiente: String? = null
    private var claveRetomaPendiente: String? = null
    fun retomaPermisoTerminado(luegoJugar: Boolean) {
        if (luegoJugar) {
            claveRetomaPendiente?.let { elegirConsola("todo"); elegirJuego(it) }
            jugar(pantallaRetomaPendiente, conRetomaLista = true)
        }
        pantallaRetomaPendiente = null; claveRetomaPendiente = null
    }

    // ---------- Cajón de apps: el resto de las apps de la consola ----------

    val PESTANAS_APPS = listOf("Todas", "Fijadas", "Juegos")

    /** Abre el cajón con todas las apps de la consola (las carga la primera vez, sin trabar nada). */
    fun abrirApps() {
        actualizar { it.copy(panel = Panel.APPS, indice = 0, appsPestana = if (it.appsFijadas.isNotEmpty()) 1 else 0) }
        cargarApps()
    }

    fun cargarApps() {
        if (estado.value.cargandoTodas) return
        actualizar { it.copy(cargandoTodas = true) }
        scope.launch {
            val lista = withContext(Dispatchers.IO) {
                try {
                    Lanzador.apps(app).sortedBy { Busqueda.normalizar(it.nombre) }
                } catch (_: Exception) {
                    emptyList()
                }
            }
            actualizar { e ->
                val anterior = appsVisibles(e).getOrNull(e.indice)?.paquete
                val nuevo = e.copy(appsTodas = lista, cargandoTodas = false)
                if (e.panel != Panel.APPS) nuevo else {
                    val visibles = appsVisibles(nuevo)
                    val indice = visibles.indexOfFirst { it.paquete == anterior }.takeIf { it >= 0 }
                        ?: e.indice.coerceIn(0, visibles.lastIndex.coerceAtLeast(0))
                    nuevo.copy(indice = indice, navegacion = e.navegacion.copy(
                        appsMenu = e.appsMenu?.takeIf { p -> lista.any { it.paquete == p } },
                        appsMoviendo = e.appsMoviendo?.takeIf { p -> lista.any { it.paquete == p } },
                    ))
                }
            }
        }
    }

    /** Las apps de la pestaña elegida. */
    fun appsVisibles(e: Estado = estado.value): List<App> {
        val orden = e.ordenApps.withIndex().associate { it.value to it.index }
        val apps = e.appsTodas.withIndex().sortedBy { orden[it.value.paquete] ?: (e.ordenApps.size + it.index) }.map { it.value }
        return when (e.appsPestana) {
            1 -> e.appsFijadas.mapNotNull { p -> e.appsTodas.firstOrNull { it.paquete == p } }
            2 -> apps.filter { it.esJuego }
            else -> apps
        }
    }

    fun cambiarPestanaApps(d: Int) = actualizar { it.copy(navegacion = it.navegacion.copy(appsMenu = null, appsMoviendo = null), appsPestana = (it.appsPestana + d).mod(PESTANAS_APPS.size), indice = 0) }

    fun elegirPestanaApps(i: Int) = actualizar { it.copy(navegacion = it.navegacion.copy(appsMenu = null, appsMoviendo = null), appsPestana = i, indice = 0) }

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

    /** Usa una imagen de la tienda para la consola que se está editando. */
    private fun usarCandidatoConsola(img: cl.nikalbazu.diptico.data.ImagenWeb) {
        val e = estado.value
        val id = e.consolaEditando
        val que = e.candidatoPara.removePrefix("c_")
        when (img.url) {
            "accion:archivos" -> {
                when (que) {
                    "icono" -> pedirImagen("icono:$id")
                    "portada" -> pedirImagen("portada_carpeta:$id")
                    else -> pedirImagen("consola_$que:$id")
                }
                return
            }
            "accion:fuentes" -> { abrirMenu(Seccion.PORTADAS); return }
            "accion:sgdb" -> { abrirTexto("sgdb", e.claveSgdb); return }
            "accion:quitar" -> {
                if (que == "icono") {
                    val m = e.iconosPropios - id
                    almacen.guardarTexto("iconosPropios", escribirMapa(m)); actualizar { it.copy(iconosPropios = m) }
                } else cambiarConsolaPropia(id) {
                    when (que) {
                        "logo" -> it.copy(logo = null)
                        "portada" -> it.copy(portadaCarpeta = null)
                        else -> it.copy(hero = null)
                    }
                }
                actualizar { it.copy(indice = 0) }
                avisar("Listo, volvió la imagen original")
                return
            }
        }
        avisar("Bajando la imagen…")
        scope.launch {
            val f = withContext(Dispatchers.IO) { try { Descargas.bajarParaConsola(app, id, que, img.url) } catch (_: Exception) { null } }
            if (f == null) { avisar("No pude bajar esa imagen. Prueba otra."); return@launch }
            val u = Uri.fromFile(f).toString()
            when (que) {
                "icono" -> {
                    val m = estado.value.iconosPropios + (id to u)
                    almacen.guardarTexto("iconosPropios", escribirMapa(m)); actualizar { it.copy(iconosPropios = m, iconosVersion = it.iconosVersion + 1) }
                }
                "logo" -> cambiarConsolaPropia(id) { it.copy(logo = u) }
                "portada" -> cambiarConsolaPropia(id) { it.copy(portadaCarpeta = u) }
                else -> cambiarConsolaPropia(id) { it.copy(hero = u) }
            }
            avisar("¡Listo! Imagen nueva para ${Consolas.porId(id)?.nombre ?: ""}")
        }
    }

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
            ConsolaPropia(txt("nombre"), c.optLong("color", 0L), txt("hero"), txt("logo"), txt("musica"), txt("portadaCarpeta"))
        }
    } catch (e: Exception) {
        emptyMap()
    }

    private fun escribirConsolasPropias(m: Map<String, ConsolaPropia>): String = org.json.JSONObject().apply {
        m.forEach { (k, c) ->
            put(k, org.json.JSONObject().put("nombre", c.nombre ?: "").put("color", c.color).put("hero", c.hero ?: "").put("logo", c.logo ?: "").put("musica", c.musica ?: "").put("portadaCarpeta", c.portadaCarpeta ?: ""))
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
        if (id != "todo" && Consolas.porId(id) == null) return
        editarDesdeMenu = desdeMenu
        actualizar { it.copy(panel = Panel.EDITAR_CONSOLA, consolaEditando = id, indice = 0) }
    }

    private val COLORES_CONSOLA = listOf(0L) + Temas.MUESTRAS

    /** Todo lo que se puede cambiar de la consola que se está editando. */
    fun opcionesEditarConsola(e: Estado = estado.value): List<Opcion> {
        val id = e.consolaEditando
        val p = e.consolasPropias[id] ?: ConsolaPropia()
        val portada = listOf(
            Opcion("Imagen del ícono de carpeta", if (p.portadaCarpeta == null) "Buscar imagen" else "Propia ✓", Icono.PORTADA,
                "Busca arte de la consola o de un juego en las fuentes disponibles, o elige una imagen de tus archivos.") { buscarCandidatos("c_portada") },
        ) + listOfNotNull(p.portadaCarpeta?.let {
            Opcion("Restaurar portada de carpeta", icono = Icono.BORRAR) {
                cambiarConsolaPropia(id) { it.copy(portadaCarpeta = null) }
                actualizar { it.copy(indice = 0) }
            }
        })
        val c = Consolas.porId(id) ?: return portada
        val f = e.formas[id] ?: FormaCfg()
        fun archivo(que: String, tipo: String = "image/*") = pedirArchivoPara("consola_$que:$id", tipo)
        return portada + listOf(
            Opcion("Nombre", p.nombre ?: "Original", Icono.TITULO, "Cómo se llama esta consola en todo Díptico.") { abrirTexto("consola_nombre", p.nombre ?: "") },
            Opcion(
                "Lugar en la lista", "${e.consolasConJuegos.indexOfFirst { it.id == id } + 1} de ${e.consolasConJuegos.size}", Icono.VISTA,
                "Corre esta consola antes o después (← →). Así quedan en el orden que tú quieras.",
                menos = { moverConsola(id, -1) },
            ) { moverConsola(id, 1) },
            Opcion(
                "Color", if (p.color == 0L) "Original" else "Propio", Icono.PALETA, "El color de sus tarjetas, del carrusel y del fondo de su vista.",
                menos = { cambiarConsolaPropia(id) { it.copy(color = COLORES_CONSOLA[(COLORES_CONSOLA.indexOf(it.color).coerceAtLeast(0) - 1).mod(COLORES_CONSOLA.size)]) } },
            ) { cambiarConsolaPropia(id) { it.copy(color = COLORES_CONSOLA[(COLORES_CONSOLA.indexOf(it.color).coerceAtLeast(0) + 1).mod(COLORES_CONSOLA.size)]) } },
            Opcion("Ícono", if (e.iconosPropios.containsKey(id)) "Propio ✓" else "Buscar", Icono.EMULADOR, "Elige entre los paquetes de RetroArch, SteamGridDB o tus archivos.") { buscarCandidatos("c_icono") },
            Opcion("Imagen grande (hero)", if (p.hero != null) "Propia ✓" else "Buscar", Icono.PORTADA, "El fondo de esta consola: de SteamGridDB, fotos de Wikipedia o tus archivos.") { buscarCandidatos("c_hero") },
            Opcion("Logo", if (p.logo != null) "Propio ✓" else "Buscar", Icono.ESTRELLA, "Un logo que reemplaza al nombre en la vista grande: de SteamGridDB, Wikipedia o tus archivos.") { buscarCandidatos("c_logo") },
            Opcion("Música", if (p.musica != null) "Propia ✓" else "Elegir canción", Icono.MUSICA, "Suena mientras recorres esta consola (si el juego no tiene su propia canción).") { archivo("musica", "audio/*") },
            Opcion("Forma de las cajas", nombreForma(f.forma), Icono.FORMA, "Cuadrada, póster, horizontal, disco (CD), cartucho o tarjeta: la portada se acomoda al centro.") { abrirCajas(id) },
            Opcion("Tamaño de las cajas", nombreTamJuegos(f.tam), Icono.FORMA, ayuda = "Más chicas para ver más juegos de esta consola, o más grandes para verlos mejor.") { ciclarTamForma(id) },
            Opcion("Imagen de las cajas", nombreImagenCaja(f.imagen), Icono.PORTADA, "Portada, captura del juego o su logo.") { ciclarImagenCaja(id) },
            Opcion("Emulador", if (c.sinEmulador) "No necesita" else nombreEmulador(c), Icono.MANDO, ayuda = "Con qué emulador o núcleo se abren los juegos de esta consola.") { if (!c.sinEmulador) ciclarEmulador(c) },
            Opcion("Quitar imágenes y música propias", icono = Icono.BORRAR, ayuda = "Quita las imágenes y la música que elegiste para esta consola.") {
                cambiarConsolaPropia(id) { it.copy(hero = null, logo = null, musica = null, portadaCarpeta = null) }
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
        ponerWidget(i, w.x, w.y, p)
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
    fun cambiarPagina(delta: Int) = irAPagina(estado.value.paginaInicio + delta)

    fun irAPagina(p: Int) {
        val e = estado.value
        val pagina = p.coerceIn(0, e.paginas - 1)
        if (pagina == e.paginaInicio) return
        actualizar {
            if (!it.editandoWidgets) it.copy(paginaInicio = pagina)
            else it.copy(
                paginaInicio = pagina, burbuja = null, estudioMover = false, celdaCatalogo = null,
                widgetSel = it.widgets.indexOfFirst { w -> w.visible && w.pagina == pagina },
            )
        }
    }

    fun agregarPaginaWidgets() {
        val e = estado.value
        if (!e.editandoWidgets || e.paginas >= 12) return
        val paginas = e.paginas + 1
        almacen.guardarTexto("paginas", paginas.toString())
        actualizar { it.copy(paginas = paginas, paginaInicio = paginas - 1, widgetSel = -1, estudioMover = false, burbuja = null, celdaCatalogo = null) }
    }

    private fun opcionesGrilla(e: Estado): List<Opcion> = listOf(
        Opcion("Columnas de la grilla", "${e.gridCols}", Icono.APPS, "Cuántas columnas tiene tu inicio (de 3 a 10).") { ciclarGrilla(true) },
        Opcion("Filas de la grilla", "${e.gridFilas}", Icono.APPS, "De 2 a 8 filas. Cuantas más filas, más pequeñas son las casillas.") { ciclarGrilla(false) },
        Opcion("Páginas", "${e.paginas}", Icono.VISTA, "Desliza a los lados con el dedo o con L2 y R2 para cambiar de página.") { ciclarPaginas() },
        Opcion("Agregar imagen o GIF", icono = Icono.PORTADA, ayuda = "Elige una imagen o un GIF animado de tu consola; aparece como widget en la página actual.") {
            pedirArchivoPara("widgetImagen", "image/*")
        },
        Opcion(
            "Fondo del inicio", if (e.fondoInicio != null) "Propio ✓" else "Arte del juego", Icono.PORTADA,
            "Usa una imagen o un GIF animado como fondo de tu inicio con widgets.",
        ) { pedirArchivoPara("fondoInicio", "image/*") },
        Opcion("Quitar fondo propio", icono = Icono.BORRAR, ayuda = "Vuelve a la imagen de fondo original de esta consola.") {
            almacen.guardarTexto("fondoInicio", "")
            actualizar { it.copy(fondoInicio = null) }
        },
    )
}



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
import cl.nikalbazu.diptico.audio.Vibracion
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
enum class Carpeta { JUEGOS, MEDIA, MUSICA, PARTIDAS }

/** Pasos del tutorial guiado del primer arranque. */
enum class PasoTuto { BIENVENIDA, PANTALLAS, TEXTO, DISPOSICION, JUEGOS, CARPETA, EMULADORES, PORTS, PORTADAS, MUSICA, APARIENCIA, CONTROLES, LISTO }

/** Íconos propios de Díptico (se dibujan en ui/Iconos.kt). */
enum class Icono {
    PANTALLAS, UNA, NEGRO, LIBRE, INTERCAMBIAR, TEXTO, TEMA, INTRO, MUSICA, VOLUMEN, SILENCIO, CARPETA, EMULADOR,
    PORTADA, DESCARGA, CLAVE, TITULO, BORRAR, BIBLIOTECA, BUSCAR, TUTORIAL, INFO, CERRAR, JUGAR, DADO, LAPIZ,
    CONTINUAR, MENU, CHECK, ARRIBA, ABAJO, ESTRELLA, APPS, RELOJ, PALETA, MOVIL, CHISPA, CORAZON, MUNDO, WEB,
    CAMPANA, VISTA, WIFI, BLUETOOTH, BRILLO, AJUSTES, FORMA, MANDO, BATERIA, TROFEO, ASISTENTE, IDIOMA, OCULTAR,
    COPIAR, PEGAR, BISA, EDITAR, MICROFONO, PERFIL,
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
    LOGROS("Cuentas y conexiones", Icono.MUNDO, "Discord, RetroAchievements y las fuentes de portadas e imágenes: todo lo que se conecta a una cuenta."),
    ASISTENTE("DIPPO", Icono.BISA, "DIPPO, tu asistente con IA: conoce tus juegos, te ayuda cuando te atascas y cambia cosas de Díptico por ti.", "Asistente"),
    PANTALLAS("Pantallas", Icono.PANTALLAS, "Cuántas pantallas usa Díptico, qué va en cada una, el tamaño de la letra y la atenuación al jugar.", "Consola"),
    SOBREMESA("Sobremesa", Icono.VISTA, "Cuando conectas la consola a la tele: una sola pantalla, todo más grande, margen para que nada se corte y más."),
    RENDIMIENTO("Rendimiento", Icono.CHISPA, "Modo turbo al jugar: libera memoria cerrando apps de fondo, pone a Díptico en reposo y, si quieres, activa No molestar."),
    CONTROLES("Controles", Icono.MANDO, "Qué hace cada stick y los botones de atrás (L4 y R4 de la Odin 2 y otras): DIPPO, apps, música, un juego al azar…"),
    CONSOLA("Android", Icono.CAMPANA, "Díptico como inicio, notificaciones y accesos rápidos de Android."),
    IDIOMA("Idioma", Icono.IDIOMA, "El idioma de Díptico: español, inglés, portugués, chino o japonés."),
    SISTEMA("Acerca de", Icono.INFO, "Respaldo en Google Drive, actualizaciones, reportar un problema, el tutorial y la versión de Díptico."),
    // Ajustes nuevos (9 nov 2026): seis secciones claras, cada una junta varias de las de antes.
    G_PERFIL("Mi perfil", Icono.PERFIL, "Tu foto o personaje, tu nombre, tu color y tus datos: para que Díptico sea más tuyo.", "Ajustes"),
    G_ESTILO("Pantalla y estilo", Icono.TEMA, "Claro u oscuro, animaciones, íconos y las pantallas."),
    G_TEMAS("Temas y decoraciones", Icono.PALETA, "La tienda de temas, tus colores, las fechas especiales y los adornos animados."),
    G_WIDGETS("Inicio y widgets", Icono.APPS, "La pantalla de inicio, tus widgets y el dock de apps."),
    G_BIBLIOTECA("Biblioteca", Icono.BIBLIOTECA, "Cómo se ven tus juegos, tus consolas, tus carpetas y las portadas."),
    G_JUEGOS("Juegos y emuladores", Icono.EMULADOR, "Con qué se abre cada consola y el modo turbo al jugar."),
    G_CUENTAS("Cuentas y conexiones", Icono.MUNDO, "Discord, RetroAchievements, SteamGridDB, ScreenScraper y las demás fuentes: todo lo que se conecta, en un solo lugar."),
    G_CONTROLES("Controles", Icono.MANDO, "Qué hace cada botón y stick."),
    G_SONIDO("Sonido", Icono.MUSICA, "La música de fondo, los sonidos y la vibración."),
    G_DIPPO("DIPPO", Icono.BISA, "Tu compañero: dónde aparece, sus colores, su IA y cómo te ayuda mientras juegas."),
    G_SISTEMA("Sistema", Icono.INFO, "Respaldo, idioma, Android, la tele y la versión de Díptico."),
    /** Las opciones de tu perfil (dentro de «Mi perfil»). */
    PERFIL("Mi perfil", Icono.PERFIL, "Tu foto o personaje, tu nombre, tu color y tus datos."),
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
    val rango: RangoOpcion? = null,
    /** Subtítulo bajo el que se muestra en su sección. null = se deduce del título (forma antigua). */
    val grupo: String? = null,
    /** Si tiene varias alternativas: sus nombres. Al elegirla se abre un menú desplegable en vez de pasar de una en una. */
    val alternativas: List<String>? = null,
    /** La alternativa que está puesta ahora (índice en [alternativas]). */
    val elegida: Int = -1,
    /** Qué hacer al elegir la alternativa número n. */
    val elegir: ((Int) -> Unit)? = null,
    /** Si las alternativas son estilos de íconos: sus identificadores, para mostrar una muestra en cada fila. */
    val muestrasIconos: List<String>? = null,
    /** Ajustes condensados: las opciones que esta fila guarda y muestra en una ventana emergente. */
    val sub: List<Opcion>? = null,
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
            "bisa" -> "Hablar con DIPPO"; "musica" -> "Pausar o seguir la música"; "siguiente" -> "Siguiente canción"; "apps" -> "Abrir las apps"
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
        Widget("progreso", 0, 0, 3, 2, false),
    )

    /** El inicio nuevo (9 nov 2026): seguir jugando, tu progreso, música y lo último que jugaste. */
    val inicioNuevo = listOf(
        Widget("proximaPartida", 0, 0, 3, 2), Widget("progreso", 3, 0, 3, 2),
        Widget("musica", 0, 2, 3, 2), Widget("recientes", 3, 2, 3, 2),
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
        "asistente" -> "Un acceso directo para hablar con DIPPO"
        "apps" -> "Tus apps fijadas y el cajón con todas"
        "temporizador" -> "Cuenta atrás y aviso mientras juegas"
        "cronometro" -> "Mide una partida y pausa cuando quieras"
        "buscar" -> "Abre la búsqueda de juegos con un toque"
        "proximaPartida" -> "Recuerda qué jugaste, tu próximo paso y tu mapa"
        "colaJuegos" -> "Ordena los juegos que quieres empezar o terminar"
        "tengo20" -> "Sugiere juegos que sueles jugar en sesiones breves"
        "progreso" -> "Cuánto te queda en tu juego, tu racha de días y el próximo logro de DIPPO"
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
        "progreso" -> Icono.TROFEO
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
        "progreso" -> "Tu progreso"
        else -> if (tipo.startsWith("imagen")) "Imagen o GIF" else tipo
    }
}

/** Descarga de portadas en curso. */
data class Descarga(val hechos: Int, val total: Int, val actual: String, val encontrados: Int)

/**
 * Lo que se agregó desde la v1.2. Va en su propia clase porque [Estado] ya llegó al máximo de datos que acepta Java;
 * se lee igual que antes (e.desplegable, e.vibracion…) gracias a las propiedades de abajo.
 */
data class EstadoExtra(
    /** Estilo de las barras de cada pantalla: "solida", "difusa" o "transparente". */
    val estiloBarraArriba: String = "solida",
    val estiloBarraAbajo: String = "solida",
    /** Accesos de la cortina elegidos, en orden (vacío = todos). */
    val accesosCentro: List<String> = emptyList(),
    /** Vibración al moverse y elegir: "apagada", "suave", "media" o "fuerte". */
    val vibracion: String = "apagada",
    /** Menú desplegable abierto sobre la opción número [desplegable] del menú (-1 = cerrado) y la alternativa marcada. */
    val desplegable: Int = -1,
    val desplegableSel: Int = 0,
    /** Animaciones: "apagadas", "suaves", "normales" o "intensas"; y su velocidad: "lenta", "normal" o "rapida". */
    val animaciones: String = "normales",
    val velocidadAnim: String = "normal",
    /** Vidrio con lente (refracción y luz), en Android 13 o más nuevo. */
    val lente: Boolean = false,
    /** Dock de apps fijadas en la pantalla de abajo: si está, dónde, de qué tamaño, cuántas apps y si está desplegado. */
    val dock: Boolean = true,
    val dockLado: String = "abajo",
    val dockTam: String = "medio",
    val dockCantidad: Int = 5,
    val dockAbierto: Boolean = false,
    /** Barra de la hora: "flotante" (islas en las esquinas, sin espacio propio) o "completa" (la franja de siempre). */
    val formaBarra: String = "completa",
    /** Colores de los íconos de la barra: "vivos", "pastel", "neon" o "tema". */
    val coloresBarra: String = "vivos",
    /** DIPPO se queda siempre a la vista en el inicio (franja de abajo o esquina en una pantalla). */
    val dippoInicio: Boolean = true,
    /** DIPPO saluda con un globito al llegar al inicio. */
    val dippoSaludo: Boolean = true,
    /** DIPPO cuenta trucos y funciones escondidas de vez en cuando. */
    val dippoDatos: Boolean = true,
    /** Si la biblioteca muestra la carpeta «Todos los juegos» junto a las de cada consola. */
    val carpetaTodos: Boolean = false,
    /** La barra de abajo (lupa, apps y ajustes): fija, pero se puede apagar. */
    val franjaAbajo: Boolean = true,
    /** Editando el perfil: el borrador (null = solo mirando). Con dos pantallas va abajo y la tarjeta arriba. */
    val edicionPerfil: EdicionPerfil? = null,
    /** Carpetas donde los emuladores guardan las partidas (direcciones de Android); al terminar de jugar se respalda lo que cambió. */
    val carpetasPartidas: List<String> = emptyList(),
    /** Respaldar solo las partidas al terminar de jugar. */
    val respaldoPartidas: Boolean = true,
    /** Accesibilidad: letras secundarias y bordes más marcados. */
    val altoContraste: Boolean = false,
    /** Medidor de fluidez: cuadros por segundo en una esquina de cada pantalla. */
    val medidorFps: Boolean = false,
    /** Ajustes condensados: la fila «Más opciones…» abierta en ventana (índice; -1 = ninguna) y la opción marcada adentro. */
    val subOpcion: Int = -1,
    val subSel: Int = 0,
    /** Carpetas en modo edición (tiemblan y se pueden mover) y la que está tomada para moverla con la cruceta. */
    val ordenandoCarpetas: Boolean = false,
    val carpetaTomada: String? = null,
    /** Cómo se ven las carpetas: "tarjeta", "consola", "abanico" o "logo" (ver ESTILOS_CARPETA). */
    val estiloCarpetas: String = "tarjeta",
    /** Mientras juegas: muestra la temperatura y DIPPO da consejos de calor, batería y descansos. */
    val termica: Boolean = true,
    /** Avisos de DIPPO (calor, batería, actualizaciones) también en la campana de notificaciones de arriba. */
    val dippoAvisos: Boolean = true,
    /** Tono del dibujo de las carpetas: "auto" (según el tema), "oscuro" o "claro". */
    val carpetasTono: String = "auto",
    /** Colores de DIPPO: "hora" (cálido de día, azulado de noche), "dia" o "noche". */
    val dippoColor: String = "hora",
    /** Cuando le hablas a DIPPO, te responde en voz alta. */
    val dippoVoz: Boolean = true,
    /** Teclado: la franja de arriba muestra las herramientas (cursor, copiar, pegar) en vez de las sugerencias. */
    val tecladoEdicion: Boolean = false,
    /** Teclado: mayúsculas fijas (dos toques a ⇧). */
    val mayusFija: Boolean = false,
    /** Cuánto dura cada juego según HowLongToBeat (por clave de juego). */
    val duraciones: Map<String, CuantoDura.Duracion> = emptyMap(),
    /** Reverso y lomo reales de cada caja (enlaces de ScreenScraper o TheGamesDB), por clave de juego. */
    val caras: Map<String, CarasGuardadas> = emptyMap(),
    /** Las cajas llevan arriba la franja con el logo (o el nombre) de su consola, como las cajas de verdad. */
    val bandaCaja: Boolean = true,
    /** La ventana de amigos de Discord está abierta en esta pantalla (id de la pantalla; -1 = cerrada). */
    val amigosEn: Int = -1,
    /** Cuándo DIPPO ganó un logro (para que celebre unos segundos) y cuál. */
    val dippoCelebra: Long = 0L,
    val dippoLogro: String = "",
    /** Pantalla de carga al abrir un juego: el color de la consola, la caja y un fundido a negro. */
    val cargaJuego: Boolean = true,
    /** El juego que se está abriendo con la pantalla de carga (null = ninguno). */
    val carga: CargaJuego? = null,
    /** La portada de arriba con el diseño nuevo: "cajas" (vitrina de cajas) o "arte" (el arte del juego en grande). */
    val portada: String = "cajas",
    /** Las apps del dock a la vista en la franja de abajo (se esconden con su botón para dejar la franja limpia). */
    val dockVisible: Boolean = true,
    /** Discord: el enlace del webhook de tu canal y cuándo avisar (al empezar y al terminar de jugar). */
    val discordWebhook: String = "",
    val discordEmpezar: Boolean = true,
    val discordTerminar: Boolean = true,
    /** Discord con tu cuenta: el Application ID de Díptico (si cambias el de fábrica), tu nombre y foto, y el inicio de sesión en curso. */
    val discordApp: String = "",
    val discordUsuario: String = "",
    val discordAvatar: String = "",
    val discordLogin: LoginDiscord? = null,
    /** Con el kit de Discord: tu perfil muestra «Jugando a…» con el juego que abres. */
    val discordPresencia: Boolean = true,
    /** Tu perfil, y si su tarjeta está abierta (id de la pantalla; -1 = cerrada) con el punto de donde crece (fracción del ancho y del alto). */
    val perfil: Perfil = Perfil(),
    val perfilEn: Int = -1,
    /** «Tu año» abierto en esta pantalla (-1 = cerrado). */
    val anioEn: Int = -1,
    /** Centro de Switch 2 y ports nativos: en qué pantalla está abierto (-1 = cerrado), su pestaña y la fila elegida. */
    val nativosEn: Int = -1,
    val nativosPestana: Int = 0,
    val nativosSel: Int = 0,
    /** Emuladores al día: lo que DIPPO revisó, si está revisando, qué se está bajando y cuánto va, y si revisa solo cada día. */
    val emus: List<EmuRevisado> = emptyList(),
    val emusRevisando: Boolean = false,
    val emusRevisado: Long = 0L,
    val emusDescarga: String? = null,
    val emusAvance: Float = 0f,
    val emusAuto: Boolean = true,
    val perfilOx: Float = 0.92f,
    val perfilOy: Float = 0.07f,
)

/** El inicio de sesión con Discord en curso: el código y el QR para el teléfono, o el error. */
data class LoginDiscord(
    val codigo: String = "",
    val url: String = "",
    val urlCompleta: String = "",
    val expira: Long = 0L,
    val error: String? = null,
) {
    val cargando get() = codigo.isBlank() && error == null
}

/** Un juego que se está abriendo: cuál, en qué pantalla y desde cuándo (para la animación de carga). */
data class CargaJuego(val clave: String, val displayId: Int, val inicio: Long)

/** Cuánto dura la pantalla de carga antes de abrir el juego (el fundido a negro termina justo ahí). */
const val CARGA_MS = 1250L

val Estado.estiloBarraArriba get() = extra.estiloBarraArriba
val Estado.estiloBarraAbajo get() = extra.estiloBarraAbajo
val Estado.accesosCentro get() = extra.accesosCentro
val Estado.vibracion get() = extra.vibracion
val Estado.desplegable get() = extra.desplegable
val Estado.desplegableSel get() = extra.desplegableSel

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
    val temaFestivo: Boolean = false,
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
    /** DIPPO puede buscar guías en internet (cuesta un poco más de la clave). */
    val bisaWeb: Boolean = true,
    /** Motor de DIPPO: auto, local, gemini u openai. */
    val motorIA: String = "auto",
    val iaLocalLista: Boolean = false,
    /** DIPPO ofrece ayuda mientras juegas (aviso y burbuja). */
    val bisaEnJuego: Boolean = true,
    /** Burbuja flotante de DIPPO: abierta (ventanita) y con aviso. */
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
    /** Ajustes y estados nuevos agrupados aparte (Java no permite más de 255 datos en un solo constructor). */
    val extra: EstadoExtra = EstadoExtra(),
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
    /** Lo que DIPPO está haciendo mientras piensa ("Buscando en internet…"), para mostrarlo. */
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
    val bienvenidaModo: String = "no",
    /** Cómo te llama DIPPO (vacío = sin nombre). */
    val nombre: String = "",
    /** Díptico es la app de inicio de Android (el botón Inicio trae aquí). */
    val esInicio: Boolean = true,
) {
    val nivelesGlass get() = experiencia.nivelesGlass
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

    internal val biblioteca by lazy { IndiceBiblioteca.de(juegos) }
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

    val pestanas: List<String> by lazy { (if (extra.carpetaTodos || consolasConJuegos.isEmpty()) listOf("todo") else emptyList()) + consolasConJuegos.map { it.id } }

    /** La selección y los paneles pueden cambiar sin volver a filtrar ni ordenar la biblioteca. */
    val visibles: List<Juego> by lazy { biblioteca.visibles(this) }

    val indiceSel: Int by lazy { visibles.indexOfFirst { it.clave == juegoSel }.coerceAtLeast(0) }
    val seleccionado: Juego? get() = visibles.getOrNull(indiceSel)
    val sinPortada: Int get() = biblioteca.sinPortada
}

/** Para buscar sin importar tildes ni mayúsculas. */
object Busqueda {
    internal val marcas = Regex("\\p{M}")

    fun normalizar(s: String): String =
        java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replace(marcas, "").lowercase()
}

/** Estado compartido por las dos pantallas y todas las acciones de Díptico. */
object Diptico {
    internal val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado

    internal lateinit var app: Context
    internal lateinit var almacen: Almacen
    /** Una tarea de fondo que falla no cierra Díptico: se anota para el reporte de fallas y se sigue. */
    internal val sinCaidas = kotlinx.coroutines.CoroutineExceptionHandler { _, error ->
        if (::app.isInitialized) Fallas.anotar(app, error)
    }
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + sinCaidas)
    internal var avisoJob: Job? = null
    internal var despiertaJob: Job? = null
    internal var descargaJob: Job? = null
    internal var introInicio = 0L
    internal var crudos: List<Juego> = emptyList()
    internal var accesosAndroid: Map<Uri, String> = emptyMap()

    internal fun esAccesoAndroid(j: Juego): Boolean =
        j.uri.scheme != "app" && Consolas.porId(j.consolaId)?.comandos?.any { it.abreApp } == true

    internal suspend fun leerAccesosAndroid(juegos: List<Juego>): Map<Uri, String> = withContext(Dispatchers.IO) {
        juegos.filter(::esAccesoAndroid).mapNotNull { j ->
            Lanzador.paqueteDeAcceso(app, j.uri)?.let { j.uri to it }
        }.toMap()
    }
    internal var medios: Map<String, MediosJuego> = emptyMap()

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
            navegacion = NavegacionEstado(atajoApps = apps.id, atajoBisa = bisa.id, ordenApps = almacen.texto("ordenApps", "").split(",").filter { it.isNotBlank() }, clipRetoma = almacen.bool("clipRetoma2", false), esperaRetoma = almacen.texto("esperaRetoma", "30").toIntOrNull()?.coerceIn(15, 300) ?: 30),
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
            temaFestivo = almacen.bool("temaFestivo2", false),
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
            vista = almacen.texto("vista", "mosaicos").let { if (it in setOf("mosaicos", "xbox", "cuadricula", "lista", "carrusel")) it else "mosaicos" },
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
            barraFija = almacen.bool("barraFija", true),
            extra = EstadoExtra(
                estiloBarraArriba = almacen.texto("estiloBarraArriba", "solida"),
                estiloBarraAbajo = almacen.texto("estiloBarraAbajo", "solida"),
                accesosCentro = almacen.texto("accesosCentro", "").split("|").filter { it.isNotBlank() }.map { if (it == "Bisa") "DIPPO" else it },
                vibracion = almacen.texto("vibracion", "apagada").also { cl.nikalbazu.diptico.audio.Vibracion.iniciar(app, it) },
                animaciones = almacen.texto("animaciones", "normales"),
                velocidadAnim = almacen.texto("velocidadAnim", "normal"),
                lente = almacen.bool("lenteVidrio", false),
                formaBarra = "completa",
                coloresBarra = almacen.texto("coloresBarra", "vivos"),
                dippoInicio = almacen.bool("dippoInicio", true),
                dippoSaludo = almacen.bool("dippoSaludo", true),
                dippoDatos = almacen.bool("dippoDatos", true),
                carpetaTodos = almacen.bool("carpetaTodos2", false),
                emusAuto = almacen.bool("emusAuto", true),
                emusRevisado = almacen.texto("emusRevisado").toLongOrNull() ?: 0L,
                franjaAbajo = almacen.bool("franjaAbajo", true),
                carpetasPartidas = almacen.texto("carpetasPartidas").lines().filter { it.isNotBlank() },
                respaldoPartidas = almacen.bool("respaldoPartidas", true),
                altoContraste = almacen.bool("altoContraste", false),
                medidorFps = almacen.bool("medidorFps", false),
                estiloCarpetas = almacen.texto("estiloCarpetas", "tarjeta"),
                termica = almacen.bool("termica", true),
                dippoAvisos = almacen.bool("dippoAvisos", true),
                carpetasTono = almacen.texto("carpetasTono", "auto"),
                dippoColor = almacen.texto("dippoColor", "hora"),
                dippoVoz = almacen.bool("dippoVoz", true),
                cargaJuego = almacen.bool("cargaJuego", true),
                portada = almacen.texto("portadaArriba", "cajas"),
                dockVisible = almacen.bool("dockVisible", true),
                discordWebhook = almacen.texto("discordWebhook", ""),
                discordEmpezar = almacen.bool("discordEmpezar", true),
                discordTerminar = almacen.bool("discordTerminar", true),
                discordApp = almacen.texto("discordApp", ""),
                discordUsuario = almacen.texto("discordUsuario", ""),
                discordPresencia = almacen.bool("discordPresencia", true),
                perfil = cargarPerfil(),
                discordAvatar = almacen.texto("discordAvatar", ""),
                duraciones = CuantoDura.leer(almacen.texto("duraciones")),
                caras = CarasGuardadas.leer(almacen.texto("carasCaja2")),
                bandaCaja = almacen.bool("bandaCaja", true),
                dock = almacen.bool("dock2", true),
                dockLado = almacen.texto("dockLado", "abajo"),
                dockTam = almacen.texto("dockTam", "medio"),
                dockCantidad = almacen.texto("dockCantidad", "5").toIntOrNull()?.coerceIn(3, 6) ?: 5,
            ),
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
            bienvenidaModo = almacen.texto("bienvenidaModo2", "no"),
            nombre = almacen.texto("nombre"),
            esInicio = Inicio.esPredeterminado(app),
        )
        Consolas.personalizar(estado.value.consolasPropias.mapValues { (_, v) -> v.nombre to v.color })
        Inicio.vigilarPantalla(app)
        Inicio.inicioAbajo(app, !(almacen.modoElegido && almacen.modoPantallas == "una_libre"))
        bienvenidaPendiente = true
        revisarPedidoInicio()
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
        vigilarTuAno()
        vigilarEmuladores()
        vigilarActualizaciones()
        vigilarLogrosDippo()
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
        val nuevo = f(_estado.value)
        // De vuelta en el inicio: ya no hay a qué ventana de Ajustes volver.
        if (nuevo.panel == Panel.NINGUNO && pilaMenu.isNotEmpty()) pilaMenu.clear()
        _estado.value = nuevo
    }

    /** Dónde estabas en Ajustes (sección, opción y subventana) cuando una opción abrió otra ventana; B vuelve justo ahí. */
    internal data class EnMenu(val seccion: Seccion, val enOpciones: Boolean, val indice: Int, val subOpcion: Int, val subSel: Int, val abierto: Panel)
    internal val pilaMenu = ArrayDeque<EnMenu>()

    /** Corre [accion] desde Ajustes: si abre otra ventana, se recuerda dónde estabas para volver con B. */
    internal fun desdeMenu(accion: () -> Unit) {
        val e = estado.value
        accion()
        val d = estado.value
        if (e.panel == Panel.MENU && d.panel != Panel.MENU && d.panel != Panel.NINGUNO) {
            pilaMenu.addLast(EnMenu(e.seccion, e.enOpciones, e.indice, e.extra.subOpcion, e.extra.subSel, d.panel))
            while (pilaMenu.size > 6) pilaMenu.removeFirst()
        }
    }

    /** Si la ventana que se cierra se abrió desde Ajustes, vuelve a esa opción (con su subventana abierta). */
    internal fun volverAlMenu(): Boolean {
        val e = estado.value
        val r = pilaMenu.lastOrNull() ?: return false
        if (r.abierto != e.panel) return false
        pilaMenu.removeLast()
        actualizar { it.copy(panel = Panel.MENU, seccion = r.seccion, enOpciones = r.enOpciones, indice = r.indice,
            extra = it.extra.copy(subOpcion = r.subOpcion, subSel = r.subSel, desplegable = -1)) }
        return true
    }

    /** Una pantalla de Díptico aparece (+1) o se va (-1). */
    fun aLaVista(delta: Int) {
        actualizar { it.copy(aLaVista = (it.aLaVista + delta).coerceAtLeast(0)) }
        // Volviste a Díptico (por ejemplo desde un juego): la música de fondo vuelve si se había cortado.
        if (delta > 0 && estado.value.sesion == null) { Musica.alVolver(); mostrarBarra() }
    }

    // ---------- Barra flotante ----------

    /** Vuelve a mostrar la barra de inicio al tocar la pantalla o volver del juego. */
    internal var trabajoBarra: Job? = null
    const val BARRA_MS = 5_000L

    /** Saca la barra de accesos; si no está fija, se vuelve a esconder a los 5 segundos sin tocar nada. */
    fun mostrarBarra() {
        if (!estado.value.barra) actualizar { it.copy(barra = true) }
        trabajoBarra?.cancel()
        if (estado.value.barraFija) return
        trabajoBarra = scope.launch {
            delay(BARRA_MS)
            if (!estado.value.barraFija && estado.value.panel == Panel.NINGUNO) actualizar { it.copy(barra = false) }
        }
    }

    val ESTILOS_BARRA = listOf("solida", "difusa", "transparente")
    fun nombreEstiloBarra(v: String) = when (v) { "difusa" -> "Difusa"; "transparente" -> "Transparente"; else -> "Sólida" }

    val COLORES_DIPPO = listOf("hora", "dia", "noche")
    fun nombreColorDippo(v: String) = when (v) { "dia" -> "Siempre de día"; "noche" -> "Siempre de noche"; else -> "Según la hora" }

    val FORMAS_BARRA = listOf("flotante", "completa")
    val COLORES_BARRA = listOf("vivos", "pastel", "neon", "tema")
    fun fijarEstiloBarra(arriba: Boolean, v: String) {
        almacen.guardarTexto(if (arriba) "estiloBarraArriba" else "estiloBarraAbajo", v)
        actualizar { it.copy(extra = if (arriba) it.extra.copy(estiloBarraArriba = v) else it.extra.copy(estiloBarraAbajo = v)) }
    }

    fun cambiarBarraFija(v: Boolean) {
        almacen.guardarBool("barraFija", v)
        actualizar { it.copy(barraFija = v, barra = true) }
        if (!v) mostrarBarra()
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

    internal var bienvenidaPendiente = false
    internal var bienvenidaJob: Job? = null

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
    internal val enFila = java.util.concurrent.ConcurrentLinkedQueue<Pair<String, () -> Unit>>()

    /** ¿Hay algo grande encima de la pantalla ahora mismo (tour, bienvenida, tarjetas)? */
    fun hayAlgoEncima(e: Estado = estado.value): Boolean =
        e.intro || e.dialogo != null || e.novedades >= 0 || e.bienvenida > 0L || e.quedaste || e.donde != null || e.marcandoMapa != null ||
            e.continuacion > 0L || e.relojGrande || !e.configurado

    /** Hace [accion] ahora si la pantalla está libre; si no, la deja en la fila para cuando se libere. */
    internal fun cuandoHayaLugar(clave: String, accion: () -> Unit) {
        if (!hayAlgoEncima() && enFila.isEmpty()) { accion(); return }
        enFila.removeIf { it.first == clave }
        enFila.add(clave to accion)
        // Los avisos viejos no sirven: la fila guarda como mucho 6 cosas.
        while (enFila.size > 6) enFila.poll()
    }

    /** Saca de la fila lo que espera, de a uno, con una pausa entre cosa y cosa. */
    internal fun vigilarFila() {
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
    internal fun aplicarArranque() {
        when (estado.value.arranque) {
            "caratulas" -> actualizar { it.copy(inicio = "heroe") }
            "widgets" -> actualizar { it.copy(inicio = "widgets") }
            "consolas" -> actualizar {
                it.copy(inicio = "heroe", enSistemas = true, consolaSel = consolaDeCarpeta(it), vistaConsolas = if (it.vistaConsolas in MODOS_ELEGIR) it.vistaConsolas else "carpetas")
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

    internal var dialogoSi: (() -> Unit)? = null
    internal var dialogoNo: (() -> Unit)? = null
    internal var mantenerJob: kotlinx.coroutines.Job? = null

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
    internal fun revisarCaida() {
        val informe = Fallas.pendiente(app) ?: return
        cuandoHayaLugar("falla") {
            mostrarDialogo(
                Dialogo(
                    "falla", "Díptico se cerró de golpe",
                    "La última vez Díptico se cerró solo. Puedes contar qué pasó y enviar el reporte al equipo de Díptico.",
                    Icono.INFO, si = "Preparar reporte", no = "Ahora no",
                ),
                // «Ahora no» descarta el aviso: no vuelve a salir en cada arranque por la misma caída.
                no = { Fallas.olvidar(app) },
            ) { prepararReporte(informe) }
        }
    }

    internal var reportePendiente: Fallas.Reporte? = null
    internal var enviandoReporte = false

    internal fun prepararReporte(caida: String? = null) {
        if (enviandoReporte) { avisar("El reporte se está enviando"); return }
        val detalle = caida ?: Fallas.informe(app, "Reporte de un problema", null)
        val reporte = reportePendiente?.takeIf { it.caida == (caida != null) }
            ?: Fallas.Reporte(detalle, caida != null)
        reportePendiente = reporte
        abrirTexto("reporte_fallas", reporte.nota)
    }

    internal fun confirmarReporte(nota: String) {
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

    internal fun ofrecerCopiarReporte(reporte: Fallas.Reporte, motivo: String) {
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

    internal val ARRANQUES = listOf("igual", "caratulas", "widgets", "consolas", "ultimo")
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
    internal fun juegosApps(lista: List<String>, consola: String): List<Juego> = lista.distinct().mapNotNull { pkg ->
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
        val ports = juegosApps((e.ports + Nativos.paquetesPorts.filter { Lanzador.instalado(app, it) }).filterNot { it in paquetes }, Consolas.PORTS)
        paquetes.addAll(ports.map { it.archivo })
        val android = juegosApps(e.android.filterNot { it in paquetes }, Consolas.ANDROID)
        return (archivos + ports + android + juegosDePC()).distinctBy { it.clave }
    }

    internal fun vigilarAppsInstaladas() {
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
            Carpeta.PARTIDAS -> {
                val antes = estado.value.extra.carpetasPartidas
                if (uri.toString() !in antes) {
                    val nuevas = antes + uri.toString()
                    almacen.guardarTexto("carpetasPartidas", nuevas.joinToString("\n"))
                    actualizar { it.copy(extra = it.extra.copy(carpetasPartidas = nuevas)) }
                }
                avisar("Listo: al terminar de jugar respaldo las partidas que cambien en «${Biblioteca.nombreCarpeta(uri)}»")
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

    internal fun nombreModo(m: String) = when (m) {
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
    internal fun ofrecerInicio() {
        scope.launch {
            delay(600)
            if (Inicio.esPredeterminado(app)) return@launch
            // Android cierra y vuelve a abrir la app al cambiar el inicio: se avisa antes para que no parezca una caída.
            mostrarDialogo(
                Dialogo(
                    "ser-inicio", "¿Díptico como tu inicio?",
                    "Así el botón Inicio siempre te trae aquí. Al aceptarlo, Android cierra Díptico un segundo y lo vuelve a abrir solo: no es una falla.",
                    Icono.APPS, si = "Sí, hacerlo", no = "Ahora no",
                ),
            ) {
                almacen.guardarTexto("pidioInicio", System.currentTimeMillis().toString())
                hacerInicio()
            }
        }
    }

    /** Al abrir: si se acaba de pedir ser el inicio (y Android reinició la app), se confirma con calma. */
    internal fun revisarPedidoInicio() {
        val t = almacen.texto("pidioInicio").toLongOrNull() ?: return
        almacen.guardarTexto("pidioInicio", "")
        if (System.currentTimeMillis() - t > 10 * 60_000L) return
        scope.launch {
            delay(1200)
            if (Inicio.esPredeterminado(app)) avisar("¡Listo! Díptico es tu inicio: el botón Inicio siempre te trae aquí")
        }
    }

    /** El siguiente truco escondido que cuenta DIPPO (da la vuelta a la lista sin repetir). */
    fun siguienteDatoDippo(): String {
        val lista = DatosDippo.LISTA
        val i = ((almacen.texto("dippoDato").toIntOrNull() ?: -1) + 1).mod(lista.size)
        almacen.guardarTexto("dippoDato", i.toString())
        return lista[i]
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

    internal fun fijarTema(oscuro: Boolean) {
        almacen.oscuro = oscuro
        actualizar { it.copy(oscuro = oscuro) }
    }

    fun alternarTema() = fijarTema(!estado.value.oscuro)

    internal val tamanos = listOf(1f, 1.15f, 1.3f)
    internal fun nombreTam(t: Float) = when {
        t >= 1.29f -> "Muy grande"
        t >= 1.14f -> "Grande"
        else -> "Normal"
    }

    internal fun fijarTam(t: Float) {
        almacen.tamTexto = t
        actualizar { it.copy(tamTexto = t) }
    }

    /**
     * Si las pantallas se ven intercambiadas ahora. Mientras juegas, manda dónde está el juego: la pantalla libre
     * siempre muestra el estante con las herramientas de la partida, estén o no intercambiadas.
     */
    fun intercambioEfectivo(e: Estado): Boolean {
        val s = e.sesion
        if (s == null || s.dosPantallas || e.unaPantalla) return e.intercambiar
        return s.displayId != Display.DEFAULT_DISPLAY
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

    internal fun fijarMusica(si: Boolean) {
        almacen.musica = si
        actualizar { it.copy(musica = si) }
    }

    internal fun fijarPista(p: String) {
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

    internal fun nombrePista(p: String) = when (p) {
        "mezcla" -> "Mezcla de las tres"
        "mia" -> "Tu música"
        else -> Musica.pistas.firstOrNull { it.id == p }?.nombre ?: "Bisagra"
    }

    internal val volumenes = listOf(0.35f, 0.6f, 0.9f)
    internal fun nombreVolumen(v: Float) = when {
        v >= 0.85f -> "Alto"
        v >= 0.55f -> "Medio"
        else -> "Bajo"
    }

    // ---------- Tutorial guiado ----------
    internal val ordenPistas = listOf("bisagra", "cartucho", "medianoche", "mezcla")

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

    internal fun archivoParaJugar(juego: Juego, e: Estado): Juego {
        val archivo = e.datos[juego.clave]?.archivoVariante ?: return juego
        val variante = juego.variantes.singleOrNull { it.archivo == archivo } ?: return juego
        return juego.copy(archivo = variante.archivo, uri = variante.uri, docId = variante.docId, esCarpeta = variante.esCarpeta)
    }

    internal var revisarLanzamiento: Job? = null

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

    internal fun jugarComprobado(pantalla: String?, conLupaLista: Boolean, conRetomaLista: Boolean, conCarga: Boolean = false) {
        val e = estado.value
        val juego = e.seleccionado ?: return
        // Durante la carga cambiaste de juego o la cancelaste: no se abre nada.
        if (conCarga && e.extra.carga?.clave != juego.clave) return
        val consola = Consolas.porId(juego.consolaId) ?: return
        // El juego ya está abierto: volver a la partida, nunca reiniciarla (muchos emuladores se abren «limpiando la tarea»).
        e.sesion?.takeIf { it.clave == juego.clave }?.let { s -> volverALaPartida(juego, consola, s); return }
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
        // Pantalla de carga: el color de la consola y la caja; el juego se abre recién cuando termina el fundido a negro.
        if (!conCarga && e.extra.cargaJuego && e.extra.animaciones != "apagadas" && e.configurado) {
            actualizar { it.copy(extra = it.extra.copy(carga = CargaJuego(juego.clave, displayId, System.currentTimeMillis())), panel = Panel.NINGUNO) }
            scope.launch {
                delay(CARGA_MS)
                if (estado.value.extra.carga?.clave == juego.clave) jugarComprobado(pantalla, conLupaLista, conRetomaLista, conCarga = true)
            }
            return
        }
        val error = if (juego.uri.scheme == "app") Lanzador.lanzarApp(app, juego.archivo, juego.titulo, displayId)
        else {
            val archivo = archivoParaJugar(juego, e)
            // Si es el mismo juego que se dejó hace poco (volviste a Díptico con la partida en pausa), el emulador
            // puede tenerla guardada en memoria: se abre sin «limpiar la tarea» para no reiniciarla.
            val reciente = ultimaPartida?.let { (clave, cuando) -> clave == juego.clave && System.currentTimeMillis() - cuando < RETOMAR_MS } == true
            Lanzador.lanzar(app, archivo, consola, e.datos[juego.clave]?.emulador ?: Nativos.recomendado(app, juego) ?: e.emuladores[consola.id], displayId, raizDe(archivo), retomar = reciente)
        }
        if (conCarga) terminarCarga(juego.clave, if (error != null) 0L else 1200L)
        if (error != null) {
            if (conLupaLista) cl.nikalbazu.diptico.lupa.LupaServicio.detener(app)
            if (conRetomaLista) cl.nikalbazu.diptico.retoma.RetomaServicio.detener(app)
            if (juego.uri.scheme == "app") avisar(error)
            else mostrarDialogo(Dialogo("lanzamiento-fallido", "No se pudo abrir ${juego.titulo}", error,
                Icono.EMULADOR, si = "Emuladores", no = "Cerrar")) { abrirMenu(Seccion.EMULADORES) }
            return
        }
        if (e.turbo) turboAlJugar(juego, displayId)
        if (e.extra.discordEmpezar) avisarDiscord(juego, consola, inicio = true, sesionSeg = 0L)
        presenciaDiscord(juego, consola, System.currentTimeMillis())
        actualizar {
            it.copy(navegacion = it.navegacion.copy(retomaVideo = "", retomaVideoClave = ""), sesion = Sesion(juego.clave, System.currentTimeMillis(), displayId, consola.dosPantallas), panel = Panel.NINGUNO, despierta = false, burbujaAbierta = false, burbujaAlerta = false)
        }
        // La pantalla del compañero queda encendida 5 segundos y después se atenúa.
        despertar()
        // DIPPO acompaña el juego. Si se nota que lo reintenta mucho, ofrece ayuda.
        if (e.bisaEnJuego) {
            val atascado = Asistente.reintentos(juego.clave) >= 2
            val sinPantallaLibre = consola.dosPantallas || estado.value.unaPantalla || inferior == null
            cl.nikalbazu.diptico.bisa.BurbujaBisa.acompanar(app, juego.titulo, atascado, sinPantallaLibre)
            if (atascado && !sinPantallaLibre) prepararAyuda()
        }
    }

    /** Quita la pantalla de carga de [clave] después de [espera] ms (el juego ya la tapó). */
    internal fun terminarCarga(clave: String, espera: Long) {
        scope.launch {
            if (espera > 0) delay(espera)
            if (estado.value.extra.carga?.clave == clave) actualizar { it.copy(extra = it.extra.copy(carga = null)) }
        }
    }

    /** B durante la carga: no se abre el juego. */
    fun cancelarCarga() {
        if (estado.value.extra.carga == null) return
        actualizar { it.copy(extra = it.extra.copy(carga = null)) }
    }

    /** «Hace X h Y min» para las tarjetas de Discord. */
    internal fun duracionDiscord(seg: Long): String {
        val h = seg / 3600
        val m = (seg % 3600) / 60
        return if (h > 0) "$h h $m min" else "$m min"
    }

    /** Publica en el canal de Discord que empezaste (o terminaste) de jugar, con la portada. No hace nada sin webhook. */
    internal fun avisarDiscord(juego: Juego, consola: Consola, inicio: Boolean, sesionSeg: Long, totalSeg: Long = 0L, prueba: Boolean = false) {
        val e = estado.value
        val url = e.extra.discordWebhook
        if (url.isBlank()) return
        val quien = e.nombre.ifBlank { "Alguien" }
        val titulo = when {
            prueba -> "Díptico está conectado a este canal"
            inicio -> "🎮 $quien está jugando ${juego.titulo}"
            else -> "✅ $quien jugó ${juego.titulo}"
        }
        val total = totalSeg.takeIf { it > 0 } ?: (e.datos[juego.clave]?.tiempo?.toLong() ?: 0L)
        val detalle = buildString {
            append(consola.bonito)
            if (!inicio && sesionSeg > 0) append(" · esta vez ${duracionDiscord(sesionSeg)}")
            if (total > 0) append(" · en total ${duracionDiscord(total)}")
            e.datos[juego.clave]?.paso?.takeIf { it.isNotBlank() && inicio }?.let { append("\nVa por: $it") }
        }
        val color = consola.color.let { (((it.red * 255).toInt() and 0xFF) shl 16) or (((it.green * 255).toInt() and 0xFF) shl 8) or ((it.blue * 255).toInt() and 0xFF) }
        scope.launch {
            val imagen = withContext(Dispatchers.IO) { Discord.portada(app, juego.portada) }
            val error = Discord.enviar(url, Discord.tarjeta(titulo, detalle, color, imagen != null, "Desde Díptico en ${android.os.Build.MODEL}"), imagen)
            if (prueba) avisar(error ?: "Listo: mira tu canal de Discord")
        }
    }

    /** Manda una tarjeta de prueba con el juego elegido (o el primero). */
    fun probarDiscord() {
        val e = estado.value
        if (e.extra.discordWebhook.isBlank()) { abrirTexto("discord_webhook"); return }
        val j = e.seleccionado ?: e.juegos.firstOrNull() ?: run { avisar("Primero agrega juegos"); return }
        val c = Consolas.porId(j.consolaId) ?: return
        avisarDiscord(j, c, inicio = true, sesionSeg = 0L, prueba = true)
        avisar("Enviando a Discord…")
    }

    internal fun opcionesDiscord(e: Estado): List<Opcion> = buildList {
        val x = e.extra
        if (x.discordUsuario.isNotBlank()) {
            add(Opcion("Tu cuenta de Discord", x.discordUsuario + " ✓", Icono.MUNDO,
                "Díptico está conectado a tu cuenta: tus amigos aparecen en el botón de Discord de la barra.",
                grupo = "Discord") { abrirAmigos(0) })
            if (SocialDiscord.disponible) add(Opcion("Mostrar «Jugando a…» en Discord", if (x.discordPresencia) "Sí" else "No", Icono.JUGAR,
                "Tu perfil de Discord muestra el juego que abriste en Díptico, la consola y cuánto llevas.", grupo = "Discord") {
                val v = !estado.value.extra.discordPresencia
                almacen.guardarBool("discordPresencia", v); actualizar { it.copy(extra = it.extra.copy(discordPresencia = v)) }
                if (!v) SocialDiscord.sinJuego()
            })
            add(Opcion("Cerrar sesión de Discord", "", Icono.BORRAR, "Díptico olvida tu cuenta de Discord en esta consola.", grupo = "Discord") { salirDiscord() })
        } else {
            add(Opcion("Iniciar sesión con Discord", "Con un código QR", Icono.MUNDO,
                "Aparece un código QR: escanéalo con tu teléfono y aprueba en Discord. O entra a discord.com/activate y escribe el código. Nunca escribes tu contraseña en la consola.",
                grupo = "Discord") { iniciarDiscord() })
            if (SocialDiscord.disponible) add(Opcion("Iniciar sesión en esta consola", "Abre Discord", Icono.MUNDO,
                "Se abre la app de Discord (o el navegador) en esta pantalla: tocas «Autorizar» y vuelves solo a Díptico.",
                grupo = "Discord") { conectarDiscordAqui() })
        }
        // Ya no hace falta con «Public Client»; queda a mano solo para quien la había puesto.
        val conSecreto = almacen.texto("discordSecreto", "").isNotBlank()
        if (conSecreto) add(Opcion("Clave secreta de la app (Client Secret)", if (conSecreto) "Puesta ✓" else "Falta", Icono.CLAVE,
            "Discord la pide para iniciar sesión desde una consola. En discord.com/developers › tu app › OAuth2 › Client Secret › Reset Secret, cópiala y pégala aquí (o mándala con el QR). Solo se guarda en tu consola.",
            grupo = "Discord") { abrirTexto("discord_secreto", "") })
        // Viene lista con la app; solo se muestra si falta o si alguien puso la suya.
        if (cl.nikalbazu.diptico.BuildConfig.DISCORD_APP.isBlank() || x.discordApp.isNotBlank())
        add(Opcion("App de Díptico en Discord", appDiscord().ifBlank { "Falta" }, Icono.CLAVE,
            "El Application ID de Díptico en discord.com/developers (con «Public Client» activado). Viene listo; cámbialo solo si usas tu propia app.",
            grupo = "Discord") { abrirTexto("discord_app", estado.value.extra.discordApp) })
        val conectado = x.discordWebhook.isNotBlank()
        add(Opcion(
            "Publicar en un canal", if (conectado) "Conectado ✓" else "Opcional", Icono.CAMPANA,
            "Además puedes avisar a tus amigos en un canal de tu servidor: Editar canal › Integraciones › Webhooks › Nuevo webhook › Copiar URL, y pégala aquí (o mándala con el QR).",
            grupo = "Discord · canal",
        ) { abrirTexto("discord_webhook", estado.value.extra.discordWebhook) })
        if (!conectado) return@buildList
        add(Opcion("Avisar cuando empiezo a jugar", if (x.discordEmpezar) "Sí" else "No", Icono.JUGAR,
            "Publica en el canal qué juego abriste, con su portada y dónde vas.", grupo = "Discord · canal") {
            val v = !estado.value.extra.discordEmpezar
            almacen.guardarBool("discordEmpezar", v); actualizar { it.copy(extra = it.extra.copy(discordEmpezar = v)) }
        })
        add(Opcion("Avisar cuando termino", if (x.discordTerminar) "Sí" else "No", Icono.RELOJ,
            "Al salir del juego publica cuánto jugaste esta vez y en total (solo si fue más de un minuto).", grupo = "Discord · canal") {
            val v = !estado.value.extra.discordTerminar
            almacen.guardarBool("discordTerminar", v); actualizar { it.copy(extra = it.extra.copy(discordTerminar = v)) }
        })
        add(Opcion("Mandar una prueba", "", Icono.CHISPA, "Publica una tarjeta de prueba en tu canal para ver que todo funciona.", grupo = "Discord · canal") { probarDiscord() })
        add(Opcion("Quitar el canal", "", Icono.BORRAR, "Olvida el enlace: Díptico deja de publicar en tu canal.", grupo = "Discord · canal") {
            almacen.guardarTexto("discordWebhook", ""); actualizar { it.copy(extra = it.extra.copy(discordWebhook = "")) }
            avisar("Canal de Discord quitado")
        })
    }

    /** El Application ID de Díptico en Discord: el que pegaste o el que viene con la app. */
    fun appDiscord(): String = estado.value.extra.discordApp.ifBlank { cl.nikalbazu.diptico.BuildConfig.DISCORD_APP }

    internal var loginDiscordJob: Job? = null

    /** Inicia sesión con Discord: pide el código, lo muestra con su QR y espera a que apruebes en el teléfono. */
    fun iniciarDiscord() {
        val app = appDiscord()
        if (app.isBlank()) {
            abrirTexto("discord_app", "")
            avisar("Primero pega el Application ID de la app de Díptico en Discord")
            return
        }
        // Con «Public Client» activado no hace falta la clave secreta; si alguien la puso, se usa.
        val secreto = almacen.texto("discordSecreto", "")
        val permisos = if (SocialDiscord.disponible) SocialDiscord.permisos()?.let { "identify $it" } ?: DiscordCuenta.PERMISOS_KIT else DiscordCuenta.PERMISOS
        loginDiscordJob?.cancel()
        actualizar { it.copy(extra = it.extra.copy(discordLogin = LoginDiscord())) }
        loginDiscordJob = scope.launch {
            val (c, error) = DiscordCuenta.pedirCodigo(app, secreto, permisos)
            if (c == null) { actualizar { it.copy(extra = it.extra.copy(discordLogin = LoginDiscord(error = error ?: "No pude pedir el código"))) }; return@launch }
            actualizar { it.copy(extra = it.extra.copy(discordLogin = LoginDiscord(c.usuario, c.url, c.urlCompleta, c.expira))) }
            while (System.currentTimeMillis() < c.expira) {
                delay(c.intervalo * 1000)
                if (estado.value.extra.discordLogin == null) return@launch
                val (token, falla) = DiscordCuenta.revisar(app, c, secreto)
                if (token != null) {
                    almacen.guardarTexto("discordToken", token.acceso)
                    almacen.guardarTexto("discordRefresh", token.refresco)
                    almacen.guardarTexto("discordVence", token.vence.toString())
                    SocialDiscord.usarToken(token.acceso)
                    val yo = DiscordCuenta.quienSoy(token.acceso)
                    val nombre = yo?.nombre ?: "tu cuenta"
                    almacen.guardarTexto("discordUsuario", nombre)
                    almacen.guardarTexto("discordAvatar", yo?.avatar.orEmpty())
                    actualizar { it.copy(extra = it.extra.copy(discordLogin = null, discordUsuario = nombre, discordAvatar = yo?.avatar.orEmpty())) }
                    avisar("¡Listo! Conectado a Discord como $nombre")
                    return@launch
                }
                if (falla != null) { actualizar { it.copy(extra = it.extra.copy(discordLogin = LoginDiscord(error = falla))) }; return@launch }
            }
            actualizar { it.copy(extra = it.extra.copy(discordLogin = LoginDiscord(error = "El código venció; pide uno nuevo"))) }
        }
    }

    /** Abre (o cierra, si ya estaba) la ventana de amigos de Discord en la pantalla [pantalla]. */
    fun abrirAmigos(pantalla: Int) {
        actualizar { it.copy(extra = it.extra.copy(amigosEn = if (it.extra.amigosEn == pantalla) -1 else pantalla)) }
    }

    fun cerrarAmigos() {
        if (estado.value.extra.amigosEn >= 0) actualizar { it.copy(extra = it.extra.copy(amigosEn = -1)) }
    }

    /** Cierra el panel del QR sin conectar. */
    fun cancelarDiscord() {
        loginDiscordJob?.cancel()
        if (SocialDiscord.conexion.value == "conectando" && estado.value.extra.discordUsuario.isBlank()) SocialDiscord.cancelar()
        actualizar { it.copy(extra = it.extra.copy(discordLogin = null)) }
    }

    fun salirDiscord() {
        SocialDiscord.salir()
        for (k in listOf("discordToken", "discordRefresh", "discordVence", "discordUsuario", "discordAvatar")) almacen.guardarTexto(k, "")
        actualizar { it.copy(extra = it.extra.copy(discordUsuario = "", discordAvatar = "")) }
        avisar("Cerraste la sesión de Discord")
    }

    /** Inicia sesión en esta misma consola con el kit: se abre Discord (o el navegador), tocas «Autorizar» y vuelves. */
    fun conectarDiscordAqui() {
        if (!SocialDiscord.disponible) { iniciarDiscord(); return }
        loginDiscordJob?.cancel()
        actualizar { it.copy(extra = it.extra.copy(discordLogin = null, amigosEn = -1)) }
        avisar("Se abre Discord: toca «Autorizar» y vuelves solo")
        SocialDiscord.conectarAqui()
    }

    /** Al abrir Díptico (o cuando el permiso vence): conecta el kit con la cuenta guardada, renovando si hace falta. */
    fun reconectarDiscord(forzarRenovar: Boolean = false) {
        val acceso = almacen.texto("discordToken", "")
        val refresco = almacen.texto("discordRefresh", "")
        if (acceso.isBlank() && refresco.isBlank()) return
        val vence = almacen.texto("discordVence", "").toLongOrNull() ?: 0L
        val pronto = vence - System.currentTimeMillis() < 24 * 60 * 60 * 1000L
        if ((forzarRenovar || pronto || acceso.isBlank()) && refresco.isNotBlank()) SocialDiscord.renovar(refresco)
        else if (acceso.isNotBlank()) SocialDiscord.usarToken(acceso)
    }

    /** El kit entregó un permiso nuevo (al autorizar o al renovar): se guarda para la próxima vez. */
    fun tokenDiscord(acceso: String, refresco: String, venceSeg: Long) {
        if (acceso.isBlank()) return
        almacen.guardarTexto("discordToken", acceso)
        if (refresco.isNotBlank()) almacen.guardarTexto("discordRefresh", refresco)
        almacen.guardarTexto("discordVence", (System.currentTimeMillis() + (if (venceSeg > 0) venceSeg else 604800L) * 1000).toString())
    }

    /** El kit dice quién eres: nombre y foto para la barra y la ventana de amigos. */
    fun cuentaDiscord(nombre: String, avatar: String) {
        if (nombre.isBlank()) return
        val antes = estado.value.extra.discordUsuario
        almacen.guardarTexto("discordUsuario", nombre)
        almacen.guardarTexto("discordAvatar", avatar)
        actualizar { it.copy(extra = it.extra.copy(discordUsuario = nombre, discordAvatar = avatar, discordLogin = null)) }
        if (antes.isBlank()) avisar("¡Listo! Conectado a Discord como $nombre")
        // Si estabas jugando cuando se conectó, el «Jugando a…» aparece igual.
        estado.value.sesion?.let { s -> estado.value.juegosPorClave[s.clave]?.let { j -> Consolas.porId(j.consolaId)?.let { c -> presenciaDiscord(j, c, s.inicio) } } }
    }

    fun errorDiscord(motivo: String) {
        // Si el permiso guardado ya no sirve, se olvida para no reintentar en cada arranque.
        if ("venció" in motivo) for (k in listOf("discordToken", "discordRefresh", "discordVence")) almacen.guardarTexto(k, "")
        avisar(motivo)
    }

    /** «Jugando a…» en tu perfil de Discord (si lo tienes encendido). */
    internal fun presenciaDiscord(juego: Juego, consola: Consola, inicio: Long) {
        if (!estado.value.extra.discordPresencia) return
        SocialDiscord.jugando(juego.titulo, consola.bonito, null, inicio)
    }

    /** El último juego cuya sesión se cerró y cuándo: si se vuelve a abrir pronto, se intenta retomar la partida. */
    internal var ultimaPartida: Pair<String, Long>? = null
    internal const val RETOMAR_MS = 6 * 60 * 60 * 1000L

    /** Trae al frente la partida que sigue abierta, en su misma pantalla y sin tocar la sesión. */
    internal fun volverALaPartida(juego: Juego, consola: Consola, s: Sesion) {
        val e = estado.value
        val error = if (juego.uri.scheme == "app") Lanzador.lanzarApp(app, juego.archivo, juego.titulo, s.displayId)
        else {
            val archivo = archivoParaJugar(juego, e)
            Lanzador.lanzar(app, archivo, consola, e.datos[juego.clave]?.emulador ?: Nativos.recomendado(app, juego) ?: e.emuladores[consola.id], s.displayId, raizDe(archivo), retomar = true)
        }
        if (error != null) { avisar(error); return }
        actualizar { it.copy(panel = Panel.NINGUNO, continuacion = 0L) }
        avisar("Volviendo a tu partida")
    }

    // ---------- Lupa ----------

    internal var pantallaPendiente: String? = null

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

    internal var zonaJob: Job? = null

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
    internal data class Regreso(val panel: Panel, val seccion: Seccion, val enOpciones: Boolean, val indice: Int, val fichaPestana: Int)
    internal var galeriaDesde: Regreso? = null
    internal var claveDesde: Regreso? = null

    internal fun regresoActual(): Regreso? {
        val e = estado.value
        return if (e.panel == Panel.NINGUNO || e.panel == Panel.GALERIA || e.panel == Panel.CLAVE) null
        else Regreso(e.panel, e.seccion, e.enOpciones, e.indice, e.pestanaFicha)
    }

    /** Vuelve a [r]: true si había a dónde volver. */
    internal fun volverA(r: Regreso?): Boolean {
        if (r == null) return false
        actualizar { it.copy(panel = r.panel, seccion = r.seccion, enOpciones = r.enOpciones, indice = r.indice, pestanaFicha = r.fichaPestana) }
        return true
    }

    internal val categoriasTienda = listOf("Todos", "Videojuegos", "Paisajes", "Fantasía", "Retro", "Calma")
    internal var catalogoOficial: List<TemaComunidad>? = null

    internal fun cargarCatalogoOficial(): List<TemaComunidad> =
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

    /** Importar un tema descargado (archivo JSON), desde la tienda. */
    fun importarTemaArchivo() = pedirArchivoPara("temaJson", "*/*")

    internal fun temasEnTienda(e: Estado): List<TemaComunidad> {
        if (e.galeriaPestana == 0) return e.catalogoTemas
        val categoria = categoriasTienda.getOrNull(e.galeriaPestana)
        return e.catalogoTemas.filter { it.categoria == categoria }
    }

    fun temaEnTienda(e: Estado): TemaComunidad? = temasEnTienda(e).getOrNull(e.indice)

    fun abrirGaleria(tipo: String, pestana: Int = 0) {
        if (estado.value.panel != Panel.GALERIA) {
            val e = estado.value
            galeriaDesde = regresoActual() ?: e.takeIf { it.panel == Panel.NINGUNO }?.let {
                Regreso(it.panel, it.seccion, it.enOpciones, it.indice, it.pestanaFicha)
            }
        }
        actualizar { it.copy(panel = Panel.GALERIA, galeria = tipo, galeriaPestana = pestana, indice = 0, decoracionAbierta = null) }
        val i = itemsGaleria().indexOfFirst { it.enUso }
        if (i > 0) actualizar { it.copy(indice = i) }
    }

    fun pestanasGaleria(tipo: String = estado.value.galeria): List<String> = if (tipo == "colecciones") listOf("Colecciones") else when (tipo) {
        "orden_carpetas" -> listOf("Orden de carpetas")
        "biblioteca" -> listOf("Opciones de biblioteca")
        "variantes_juego" -> listOf("Archivos del juego")
        "capturas_juego", "captura_acciones" -> listOf("Capturas guardadas")
        "copias_srm" -> listOf("Copias de partida")
        "filtros" -> listOf("Filtrar juegos")
        "orden" -> listOf("Ordenar juegos")
        "vistas_consolas" -> listOf("Tus consolas")
        "vistas_juegos" -> listOf("Tus juegos")
        "logros_dippo" -> listOf("Logros de DIPPO", "Premios")
        "personajes" -> listOf("Personajes")
        "marcos" -> listOf("Marcos")
        "titulos" -> listOf("Títulos")
        "artes" -> listOf("Artes", "Portadas")
        "temas" -> listOf("Incluidos", "Calendario", "Exclusivos", "Mis temas")
        "importar_temas" -> listOf("Importar temas")
        "tienda_temas" -> categoriasTienda
        "decoraciones" -> listOf("Instalados", "Tienda de temas", "Calendario")
        "mezclar_tema" -> ComponenteTema.entries.map { it.titulo }
        "colores" -> listOf("Principal", "Secundario", "Detalles", "Fondo", "Letras")
        "ia" -> listOf("El cerebro de DIPPO")
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
        if (e.galeria == "biblioteca") return listOf(
            ItemGaleria("buscar", "Buscar un juego", "Por nombre", false) { abrirBuscar() },
            ItemGaleria("filtros", "Filtrar", nombreFiltro(e.filtro), false) { abrirGaleria("filtros") },
            ItemGaleria("orden", "Ordenar juegos", nombreOrden(e.orden), false) { abrirGaleria("orden") },
            ItemGaleria("vista", "Vista de juegos", nombreVista(e.vista), false) { abrirGaleria("vistas_juegos") },
        )
        if (e.galeria == "personajes") return PERSONAJES.map { (id, nombre) ->
            ItemGaleria(id, nombre, "Un amigo de Díptico", id == e.extra.perfil.personaje && e.extra.perfil.foto.isBlank()) {
                cambiarPerfil { it.copy(personaje = id, foto = "") }
                avisar("¡Ahora eres $nombre!")
                cerrarPanel()
            }
        }
        if (e.galeria == "marcos") return MARCOS.map { m ->
            val libre = marcoDesbloqueado(m)
            val logro = LogrosDippo.LISTA.firstOrNull { it.id == m.logro }
            ItemGaleria(m.id, m.nombre, if (libre) m.detalle else "🔒 Se gana con «${logro?.titulo ?: "un logro"}»: ${logro?.descripcion.orEmpty()}",
                m.id == e.extra.perfil.marco) {
                if (!libre) avisar("🔒 Gana el logro «${logro?.titulo}» para usar este marco")
                else { cambiarPerfil { it.copy(marco = m.id) }; avisar("Marco «${m.nombre}» listo"); cerrarPanel() }
            }
        }
        if (e.galeria == "titulos") return TITULOS.map { t ->
            val libre = tituloDesbloqueado(t)
            val logro = LogrosDippo.LISTA.firstOrNull { it.id == t.logro }
            ItemGaleria(t.id.ifEmpty { "ninguno" }, t.nombre, if (libre) "Un título bajo tu nombre" else "🔒 Se gana con «${logro?.titulo ?: "un logro"}»: ${logro?.descripcion.orEmpty()}",
                t.id == e.extra.perfil.titulo) {
                if (!libre) avisar("🔒 Gana el logro «${logro?.titulo}» para usar este título")
                else { cambiarPerfil { it.copy(titulo = t.id) }; avisar(if (t.id.isEmpty()) "Sin título" else "Título «${t.nombre}» listo"); cerrarPanel() }
            }
        }
        if (e.galeria == "logros_dippo" && e.galeriaPestana == 1) return premiosDippo(e)
        if (e.galeria == "logros_dippo") return progresoLogrosDippo(e).map { (l, v) ->
            val hecho = l.id in hechosDippo()
            // Lo que regala cada logro (marco, título o tema), para que se sepa por qué vale la pena.
            val premio = premiosDeLogro(l.id).joinToString(" · ")
            val detalle = (if (hecho) "✓ " + l.descripcion else "${l.descripcion} · $v de ${l.meta}") + if (premio.isNotEmpty()) "\n🎁 $premio" else ""
            ItemGaleria(l.id, l.titulo, detalle, hecho, color = if (hecho) 0xFFFFC94DL else 0L) {
                if (premio.isEmpty()) avisar(l.descripcion)
                else if (hecho) actualizar { it.copy(galeriaPestana = 1, indice = 0) }
                else avisar("🎁 Al ganarlo: $premio")
            }
        }
        if (e.galeria == "orden_carpetas") return listOf(
            ItemGaleria("alfabetico", "Nombre · A → Z", "Orden alfabético", false) { ordenarCarpetas(true); cerrarPanel() },
            ItemGaleria("sistemas", "Familias de consolas", "Orden del catálogo", false) { ordenarCarpetas(false); cerrarPanel() },
        )
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
        if (e.galeria == "accesos_cortina") {
            val todos = opcionesSistema()
            val elegidos = e.accesosCentro.ifEmpty { todos.map { it.titulo } }
            val ordenados = elegidos.mapNotNull { t -> todos.firstOrNull { it.titulo == t } } + todos.filter { it.titulo !in elegidos }
            return ordenados.map { o ->
                val visible = o.titulo in elegidos
                ItemGaleria(o.titulo, o.titulo, if (visible) "Se muestra · X para subirlo" else "Oculto", visible) { alternarAcceso(o.titulo) }
            }
        }
        if (e.galeria == "artes") {
            val conArte = e.galeriaPestana == 0
            // Portadas: todos (los que no tienen, muestran la de Díptico); Artes: solo los que tienen uno.
            return e.visibles.filter { !conArte || it.arte != null }.map { j ->
                ItemGaleria(j.clave, j.titulo, Consolas.porId(j.consolaId)?.nombre ?: "", e.juegoSel == j.clave) {
                    elegirJuego(j.clave); cerrarPanel()
                }
            }
        }
        if (e.galeria == "filtros") return FILTROS.map { f -> ItemGaleria(f, nombreFiltro(f), "Mostrar esta selección", e.filtro == f) { elegirFiltro(f); cerrarPanel() } }
        if (e.galeria == "orden") return ORDENES.map { o -> ItemGaleria(o, nombreOrden(o), "Orden de tu biblioteca", e.orden == o) { elegirOrden(o); cerrarPanel() } }
        if (e.galeria == "vistas_juegos") return VISTAS.map { v ->
            val detalle = when(v) { "mosaicos" -> "Una fila de juegos, en el mismo orden que la vitrina de arriba"; "xbox" -> "Estilo Xbox: un mosaico grande y el resto en dos filas"; "cuadricula" -> "Todas las portadas a la vista"; "iconos" -> "Solo las portadas, sin textos: más juegos a la vista"; "cartuchos" -> "Cada juego en su cartucho o caja en 3D"; "lista" -> "Nombres claros y navegación rápida"; "estante" -> "Cajas con información del juego"; "carrusel" -> "Una portada protagonista"; "abanico" -> "Portadas como cartas"; else -> "Lomos de las cajas" }
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
                // Calendario: cada fiesta es un tema completo; se ve sola en su fecha (con «Fechas especiales») o se usa cuando quieras.
                1 -> Temas.calendario().map { (fi, dia) ->
                    val hoy = dia == java.time.LocalDate.now()
                    ItemGaleria(fi.tema.id, fi.nombre, (if (hoy) "Hoy · " else "${dia.dayOfMonth}/${dia.monthValue} · ") + fi.donde, actual.id == fi.tema.id, fi.tema) {
                        elegirTema(fi.tema.id)
                    }
                }
                // Exclusivos: se ganan con los logros difíciles de DIPPO; antes se ven con candado.
                2 -> Temas.exclusivos.map { x ->
                    val libre = x.logro in hechosDippo()
                    val logro = LogrosDippo.LISTA.firstOrNull { it.id == x.logro }
                    ItemGaleria(x.tema.id, x.tema.nombre,
                        if (libre) x.tema.descripcion else "🔒 Se gana con «${logro?.titulo ?: "un logro"}»: ${logro?.descripcion.orEmpty()}",
                        actual.id == x.tema.id, x.tema) {
                        if (libre) elegirTema(x.tema.id) else avisar("🔒 Gana el logro «${logro?.titulo}» para usar este tema")
                    }
                }
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
    internal fun ponerColor(c: Long) {
        val p = estado.value.galeriaPestana
        editarMiTema {
            when (p) {
                0 -> it.copy(acento = c); 1 -> it.copy(acento2 = c); 2 -> it.copy(acento3 = c)
                3 -> it.copy(fondo = c); else -> it.copy(tinta = c)
            }
        }
    }

    /** Deja a DIPPO listo para ayudar con el juego en curso (en la pantalla libre o en la burbuja). */
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
        ultimaPartida = s.clave to System.currentTimeMillis()
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
        TuAno.registrar(s.clave, duracion)
        respaldarPartida(s.clave, s.inicio, duracion)
        SocialDiscord.sinJuego()
        // Si el emulador se cerró a los pocos segundos, el juego no llegó a abrir: Díptico explica por qué y ofrece otro emulador.
        if (preguntar) revisarCierreRapido(s.clave, duracion)
        if (duracion >= 60 && estado.value.extra.discordTerminar) {
            val j = estado.value.juegosPorClave[s.clave]
            val c = j?.let { Consolas.porId(it.consolaId) }
            if (j != null && c != null) avisarDiscord(j, c, inicio = false, sesionSeg = duracion, totalSeg = datos[s.clave]?.tiempo?.toLong() ?: 0L)
        }
        // ¿Dónde quedaste?: una tarjeta con formas rápidas de anotarlo (escribir, nota de voz, mapa) o pasar de largo.
        val hacerPregunta = preguntar && duracion >= 60 && estado.value.preguntarQuedaste
        actualizar { it.copy(sesion = null, despierta = false, datos = datos, juegoSel = s.clave) }
        // Sale cuando no haya otra cosa encima (una cosa a la vez).
        if (hacerPregunta) cuandoHayaLugar("quedaste") {
            if (estado.value.sesion == null) actualizar { it.copy(juegoSel = s.clave, quedaste = true, quedastePregunta = true, quedasteSel = 0) }
        }
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
    // ---------- Teclado de Díptico ----------
    /** Fila de herramientas del teclado: mover el cursor, seleccionar, copiar, cortar y pegar. */
    val EDICION = listOf("◀", "▶", "Selec.", "Todo", "Copiar", "Cortar", "Pegar")

    internal var ultimoShift = 0L

    // ---------- Menús desplegables ----------

    /** Abre la lista de alternativas de la opción [i] del menú, con la puesta ahora marcada. */
    fun abrirDesplegable(i: Int, elegida: Int) {
        actualizar { it.copy(extra = it.extra.copy(desplegable = i, desplegableSel = elegida.coerceAtLeast(0)), indice = i) }
        Sonidos.abrir()
    }

    fun cerrarDesplegable() {
        if (estado.value.desplegable >= 0) actualizar { it.copy(extra = it.extra.copy(desplegable = -1)) }
    }

    /** Pone la alternativa [n] de la opción abierta y cierra la lista. */
    fun elegirDesplegable(n: Int) {
        val e = estado.value
        val o = opcionesSeccion(e.seccion).getOrNull(e.desplegable)
        actualizar { it.copy(extra = it.extra.copy(desplegable = -1)) }
        if (o?.elegir != null && n in o.alternativas.orEmpty().indices) o.elegir.invoke(n)
    }

    /**
     * Una opción con varias alternativas: muestra la puesta ahora y, al elegirla, abre un menú desplegable con todas.
     * [accion] sigue pasando a la siguiente (para atajos y para DIPPO).
     */
    fun opcionLista(
        titulo: String, icono: Icono?, ayuda: String, ids: List<String>, nombres: List<String>, actual: String,
        grupo: String? = null, fijar: (String) -> Unit,
    ): Opcion {
        val i = ids.indexOf(actual)
        return Opcion(
            titulo, nombres.getOrElse(i) { actual }, icono, ayuda, grupo = grupo,
            alternativas = nombres, elegida = i, elegir = { n -> fijar(ids[n]) },
        ) { fijar(ids[(i + 1).mod(ids.size)]) }
    }

    /** Le agrega a una opción la lista de alternativas para el menú desplegable, sin cambiar lo que ya hacía. */
    fun Opcion.conLista(nombres: List<String>, elegida: Int, elegir: (Int) -> Unit): Opcion =
        copy(alternativas = nombres, elegida = elegida, elegir = elegir)

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
            Panel.MENU -> if (!e.enOpciones) columnaMenu(true) else {
                val o = opcionesSeccion(e.seccion).getOrNull(e.indice)
                when {
                    e.extra.subOpcion >= 0 -> usarSubOpcion()
                    e.desplegable >= 0 -> elegirDesplegable(e.desplegableSel)
                    o?.alternativas != null && o.elegir != null -> abrirDesplegable(e.indice, o.elegida)
                    else -> o?.accion?.let { desdeMenu(it) }
                }
            }
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
            e.panel == Panel.MENU && e.extra.subOpcion >= 0 -> cerrarSubOpciones()
            e.panel == Panel.MENU && e.desplegable >= 0 -> cerrarDesplegable()
            // Una sección avanzada abierta desde «Más ajustes»: B vuelve ahí, con esa sección marcada.
            e.panel == Panel.MENU && e.enOpciones && e.seccion !in seccionesMenu() -> {
                val i = opcionesMas(e).indexOfFirst { it.titulo == e.seccion.titulo }.coerceAtLeast(0)
                actualizar { it.copy(seccion = Seccion.MAS, indice = i) }
            }
            e.panel == Panel.MENU && e.enOpciones -> columnaMenu(false)
            e.panel != Panel.NINGUNO -> cerrarPanel()
            // En el inicio, B saca la barra flotante (y la esconde si ya estaba a la vista).
            e.configurado && e.sesion == null && !e.barraFija -> if (e.barra) { trabajoBarra?.cancel(); actualizar { it.copy(barra = false) } } else mostrarBarra()
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
        lista += Opcion("DIPPO", icono = Icono.BISA, ayuda = "Le pregunta a DIPPO por un buen truco o consejo para este juego.") { abrirAsistente(); preguntar("Dame un buen truco o consejo para ${juego.titulo}.") }
        // Lista de abajo: ajustes de este juego.
        lista += Opcion("Personalizar", "Título, imágenes, música", Icono.PALETA, ayuda = "Cambia el título, la portada, la imagen grande, el logo y la música de este juego.") { abrirFicha(); elegirPestanaFicha(3) }
        lista += Opcion("Imágenes", "Tienda de imágenes", Icono.DESCARGA, ayuda = "Abre la tienda de imágenes para elegir otra portada de internet.") { abrirFicha(); buscarCandidatos("portada") }
        lista += Opcion("Mover de lugar", if (e.orden == "propio") "A tu gusto" else "Ordénalos a tu gusto", Icono.VISTA, "Corre este juego con la cruceta y deja tus juegos en el orden que quieras.") { empezarOrdenar() }
        if (consola != null && !consola.sinEmulador) {
            lista += Opcion("Emulador", d?.emulador?.let { id -> Lanzador.opciones(app, consola).firstOrNull { it.id == id }?.nombre } ?: "El de la consola", Icono.EMULADOR, ayuda = "Con qué emulador o núcleo se abre solo este juego. «El de la consola» usa el de Menú › Emuladores.") {
                ciclarEmuladorJuego(juego.clave, consola)
            }
        }
        if (estado.value.extra.carpetasPartidas.isNotEmpty() || consola != null && consola.comandos.any { it.linea.contains("RETROARCH", ignoreCase = true) }) {
            lista += Opcion("Copias de partida", if (estado.value.extra.carpetasPartidas.isNotEmpty()) "Automáticas" else ".srm", Icono.COPIAR,
                "Vuelve a una partida respaldada sola al terminar de jugar, o guarda y recupera una copia manual de RetroArch (.srm).") { abrirCopiasPartida(juego.clave) }
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
    internal class PorBorrar(val uri: Uri, val nombre: String, val bytes: Long)

    /** Columnas de la ventana del juego (la interfaz avisa cuántas caben). */
    var columnasVentana = 4

    // ---------- Menú principal ----------

    /** Las secciones del modo simple: lo de todos los días. Las demás quedan a un toque en «Más ajustes». */
    val SECCIONES_SIMPLES = listOf(
        Seccion.ESENCIAL, Seccion.TEMAS, Seccion.DECORACIONES, Seccion.INICIO, Seccion.ESTANTE, Seccion.MUSICA,
        Seccion.BIBLIOTECA, Seccion.EMULADORES, Seccion.ASISTENTE, Seccion.PANTALLAS, Seccion.SISTEMA,
    )

    /** Qué secciones de antes junta cada una de las seis nuevas. */
    val GRUPOS_AJUSTES: Map<Seccion, List<Seccion>> = linkedMapOf(
        // «Mi perfil» ya no está en Ajustes: se edita desde tu tarjeta (A sobre la foto) y Discord vive en Cuentas.
        Seccion.G_ESTILO to listOf(Seccion.APARIENCIA, Seccion.PANTALLAS),
        Seccion.G_TEMAS to listOf(Seccion.TEMAS, Seccion.DECORACIONES),
        Seccion.G_WIDGETS to listOf(Seccion.INICIO),
        Seccion.G_BIBLIOTECA to listOf(Seccion.ESTANTE, Seccion.CONSOLAS, Seccion.BIBLIOTECA, Seccion.PORTADAS),
        Seccion.G_JUEGOS to listOf(Seccion.EMULADORES, Seccion.RENDIMIENTO),
        Seccion.G_CUENTAS to listOf(Seccion.LOGROS),
        Seccion.G_CONTROLES to listOf(Seccion.CONTROLES),
        Seccion.G_SONIDO to listOf(Seccion.MUSICA),
        Seccion.G_DIPPO to listOf(Seccion.ASISTENTE),
        Seccion.G_SISTEMA to listOf(Seccion.SISTEMA, Seccion.IDIOMA, Seccion.CONSOLA, Seccion.SOBREMESA),
    )

    internal var cacheOpciones: Pair<Estado, HashMap<Seccion, List<Opcion>>>? = null

    // ---------- Emuladores ----------

    fun nombreEmulador(c: Consola): String =
        Lanzador.elegida(app, c, estado.value.emuladores[c.id])?.nombre ?: "Elegir al abrir"

    fun emuladorListo(c: Consola): Boolean = Lanzador.elegida(app, c, estado.value.emuladores[c.id]) != null

    internal fun ciclarEmulador(c: Consola) {
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

    internal fun cancelarDescarga() {
        descargaJob?.cancel()
        descargaJob = null
        terminarTrabajo("portadas")
        actualizar { it.copy(descarga = null) }
        almacen.guardarMedios(medios)
        publicarJuegos()
    }

    internal fun descargarUno(j: Juego) {
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

    internal val trabajosMapa = LinkedHashMap<String, Trabajo>()

    /** Anuncia (o actualiza) algo que se hace de fondo. Aparece arriba en la pantalla principal. */
    fun trabajo(id: String, texto: String, avance: Float? = null) {
        trabajosMapa[id] = Trabajo(id, texto, avance)
        actualizar { it.copy(trabajos = trabajosMapa.values.toList()) }
    }

    fun terminarTrabajo(id: String) {
        if (trabajosMapa.remove(id) != null) actualizar { it.copy(trabajos = trabajosMapa.values.toList()) }
    }

    // ---------- Reloj grande ----------

    fun alternarReloj() = actualizar { it.copy(relojGrande = !it.relojGrande) }

    /** El botón Inicio de Android con Díptico como launcher: cierra todo y vuelve al inicio. */
    fun alInicio() = actualizar { it.copy(panel = Panel.NINGUNO, relojGrande = false, editandoWidgets = false, pregunta = false) }

    // ---------- Guardar listas en JSON ----------

    internal fun leerTemas(s: String): List<Tema> = try {
        val a = org.json.JSONArray(s)
        (0 until a.length()).map { Tema.deJson(a.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    internal fun escribirTemas(l: List<Tema>): String = org.json.JSONArray().apply { l.forEach { put(it.json()) } }.toString()

    internal fun leerWidgets(s: String): List<Widget> = try {
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

    internal fun escribirWidgets(l: List<Widget>): String = org.json.JSONArray().apply {
        l.forEach {
            val o = org.json.JSONObject().put("tipo", it.tipo).put("x", it.x).put("y", it.y).put("an", it.an).put("al", it.al)
                .put("visible", it.visible).put("pagina", it.pagina).put("estilo", it.estilo)
                .put("color", it.color).put("opacidad", it.opacidad.toDouble()).put("radio", it.radio)
                .put("variante", it.variante).put("escala", it.escala.toDouble())
            it.uri?.let { u -> o.put("uri", u) }
            put(o)
        }
    }.toString()

    internal fun leerMapa(s: String): Map<String, String> = try {
        val o = org.json.JSONObject(s)
        o.keys().asSequence().associateWith { o.getString(it) }
    } catch (e: Exception) {
        emptyMap()
    }

    internal fun escribirMapa(m: Map<String, String>): String = org.json.JSONObject(m as Map<*, *>).toString()

    // ---------- Personalización y perfiles visuales ----------
    var exportarVisual: ((String) -> Unit)? = null
    var importarVisual: (() -> Unit)? = null
    internal var imagenVisual: Pair<String, String>? = null
    // ---------- Temas ----------
    // ---------- Íconos de consola ----------

    internal fun estilosIconos(): List<String> = listOf("diptico", "insignia", "solido", "contorno", "neon", "pixel", "siglas") + Descargas.PAQUETES.map { it.id }

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

    internal fun opcionesIconos(e: Estado): List<Opcion> {
        val c = Consolas.porId(e.consolaSel)
        val lista = mutableListOf(

            estilosIconos().let { l ->
                opcionLista(
                    "Íconos de consola", Icono.EMULADOR,
                    "Estilos de Díptico (formatos, insignias, contornos, píxeles o siglas: todas las consolas con la misma línea), o paquetes de RetroArch (se descargan una vez). Para una sola consola: Menú › Tus consolas.",
                    l, l.map(::nombreEstiloIconos), e.estiloIconos,
                ) { s ->
                    almacen.guardarTexto("estiloIconos", s)
                    actualizar { it.copy(estiloIconos = s) }
                    bajarIconosFaltantes()
                }.copy(muestrasIconos = l)
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
    internal fun bajarIconosFaltantes() {
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
    /** Los widgets corridos por el último movimiento (se aplican después de mover el elegido). */
    internal var pendienteEmpuje: List<Widget>? = null

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

    // ---------- Logros de DIPPO ----------

    internal var contadoresDippo: LogrosDippo.Contadores? = null
    internal var logrosDippoHechos: MutableSet<String>? = null

    internal fun contadores(): LogrosDippo.Contadores = contadoresDippo ?: if (!::almacen.isInitialized) LogrosDippo.Contadores() else LogrosDippo.Contadores(
        LogrosDippo.leerNumeros(almacen.texto("dippoContadores")),
        almacen.texto("dippoDias").split(',').filter { it.isNotBlank() }.toSet(),
    ).also { contadoresDippo = it }

    internal fun hechosDippo(): MutableSet<String> = logrosDippoHechos
        ?: if (!::almacen.isInitialized) mutableSetOf() else almacen.texto("dippoLogros").split(',').filter { it.isNotBlank() }.toMutableSet().also { logrosDippoHechos = it }

    /** Suma a un contador de DIPPO (preguntas, voz, pistas, ideas, temas…) y revisa si ganó algún logro. */
    fun sumarDippo(k: String, n: Int = 1) {
        if (!::almacen.isInitialized) return
        val c = contadores()
        val m = c.numeros + (k to c.n(k) + n)
        contadoresDippo = c.copy(numeros = m)
        almacen.guardarTexto("dippoContadores", LogrosDippo.escribirNumeros(m))
        revisarLogrosDippo()
    }

    /** Si se cumplió la meta de algún logro, lo guarda y DIPPO lo celebra. */
    fun revisarLogrosDippo() {
        val base = estado.value
        if (!base.configurado) return
        // Mientras juegas, el tiempo y la partida en curso ya cuentan: así los logros de horas y partidas se celebran en vivo.
        val e = base.sesion?.let { s ->
            val seg = (System.currentTimeMillis() - s.inicio) / 1000
            if (seg < 15) base else {
                val d = base.datos[s.clave] ?: DatosJuego()
                base.copy(datos = base.datos + (s.clave to d.copy(tiempo = d.tiempo + seg, sesiones = d.sesiones + 1)))
            }
        } ?: base
        val c = contadores()
        val hechos = hechosDippo()
        val nuevos = LogrosDippo.LISTA.filter { it.id !in hechos && it.valor(e, c) >= it.meta }
        if (nuevos.isEmpty()) return
        hechos += nuevos.map { it.id }
        almacen.guardarTexto("dippoLogros", hechos.joinToString(","))
        val titulo = if (nuevos.size == 1) nuevos.first().titulo else "${nuevos.size} logros nuevos"
        actualizar { it.copy(extra = it.extra.copy(dippoCelebra = System.currentTimeMillis(), dippoLogro = titulo)) }
        if (e.sesion == null) cuandoHayaLugar("logro-dippo") { avisar("🏆 Logro de DIPPO: $titulo"); Sonidos.aviso() }
    }

    /** Cada medio minuto: anota los días jugados (para las rachas) y si juegas de noche o de madrugada. */
    internal fun vigilarLogrosDippo() {
        scope.launch {
            delay(8000)
            while (true) {
                val e = estado.value
                if (e.sesion != null) {
                    val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())
                    val c = contadores()
                    if (hoy !in c.dias) {
                        val dias = (c.dias + hoy).sorted().takeLast(60).toSet()
                        contadoresDippo = c.copy(dias = dias)
                        almacen.guardarTexto("dippoDias", dias.joinToString(","))
                    }
                    when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
                        in 1..4 -> if (contadores().n("noche") == 0) sumarDippo("noche")
                        in 5..6 -> if (contadores().n("madrugada") == 0) sumarDippo("madrugada")
                    }
                }
                revisarLogrosDippo()
                delay(30_000)
            }
        }
    }

    /** Los logros de DIPPO con su avance, para la galería y para que DIPPO los cuente. */
    fun progresoLogrosDippo(e: Estado = estado.value): List<Pair<LogrosDippo.Logro, Int>> = LogrosDippo.progreso(e, contadores())

    fun logrosDippoGanados(): Int = hechosDippo().size

    /** Los marcos de la foto que ya puedes usar (los libres y los de logros ganados). */
    fun marcoDesbloqueado(m: MarcoPerfil): Boolean = m.logro == null || m.logro in hechosDippo()
    /** Los ids de los logros de DIPPO ya ganados. */
    fun idsLogrosDippo(): Set<String> = hechosDippo().toSet()
    fun tituloDesbloqueado(t: TituloPerfil): Boolean = t.logro == null || t.logro in hechosDippo()
    fun marcosDesbloqueados(): Int = MARCOS.count { marcoDesbloqueado(it) }

    /** Días seguidos jugando (para el widget de progreso). */
    fun rachaDias(): Int = LogrosDippo.racha(contadores().dias)

    /** El logro de DIPPO más cerca de ganarse: título, avance y meta. */
    fun proximoLogroDippo(e: Estado = estado.value): Triple<String, Int, Int>? =
        progresoLogrosDippo(e).filter { (l, v) -> v < l.meta && l.id !in hechosDippo() }
            .maxByOrNull { (l, v) -> v.toFloat() / l.meta }?.let { (l, v) -> Triple(l.titulo, v, l.meta) }

    /** Ordena los widgets con el diseño nuevo; la distribución de antes queda guardada para volver a ella. */
    fun aplicarInicioNuevo() {
        almacen.guardarTexto("perfilVisualAnterior", capturarPerfil().json(local = true).toString())
        val nuevos = Widgets.inicioNuevo
        val resto = estado.value.widgets.filter { w -> nuevos.none { it.tipo == w.tipo } }.map { it.copy(visible = false) }
        guardarWidgets(nuevos.map { it.copy(pagina = 0) } + resto)
        almacen.guardarTexto("paginas", "1")
        actualizar { it.copy(paginaInicio = 0, paginas = 1) }
        fijarInicio("widgets")
        avisar("Listo: tu inicio nuevo. Puedes volver al de antes en Temas › Estilos guardados")
    }

    // ---------- Cuánto dura cada juego (HowLongToBeat) ----------

    internal val buscandoDuracion = HashSet<String>()

    internal val buscandoCaras = HashSet<String>()

    internal var ultimaCharla = 0L

    /** Si la última pregunta fue con la voz, DIPPO responde hablando. */
    internal var porVoz = false

    internal var claveParaAsistente = false

    internal var claveVieneDeGaleria = false

    internal var pasoDeBisa: String? = null

    // ---------- RetroAchievements ----------

    internal val idsRA = HashMap<String, Int?>()

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

    internal fun opcionesLogros(e: Estado): List<Opcion> = listOf(
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

    internal fun juegoFicha(): Juego? = estado.value.let { e -> e.juegos.firstOrNull { it.clave == e.fichaDe } }

    fun opcionesFicha(): List<Opcion> {
        val e = estado.value
        val j = juegoFicha() ?: return emptyList()
        val d = e.datos[j.clave] ?: DatosJuego()
        val consola = Consolas.porId(j.consolaId)
        return when (e.pestanaFicha) {
            2 -> buildList {
                add(
                    Opcion(
                        "Mis trucos y notas", d.notas.lineSequence().firstOrNull()?.take(40)?.ifEmpty { null } ?: "Escribir", Icono.LAPIZ, ayuda = "Tus apuntes de este juego: claves, trucos, lo que no quieres olvidar. DIPPO también los lee.",
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

    internal fun cambiarPaqueteSonidos(d: Int) {
        val l = Sonidos.PAQUETES
        val n = l[(l.indexOf(Sonidos.paquete) + d).mod(l.size)]
        Sonidos.paquete = n
        almacen.guardarTexto("paqueteSonidos", n)
        actualizar { it.copy(paqueteSonidos = n) }
        Sonidos.aceptar()
    }

    // ---------- Juegos de PC: GameHub, Winlator, GameNative, WinNative, DroidDeck… ----------

    /** Apps para juegos de PC en Android: nombre, paquetes conocidos, cómo se reconocen por su nombre y para qué sirve. */
    internal data class LanzadorPC(val nombre: String, val paquetes: List<String>, val etiqueta: Regex, val ayuda: String)

    internal val LANZADORES_PC = listOf(
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
    internal fun paqueteLanzador(l: LanzadorPC, apps: List<App>): String? =
        l.paquetes.firstOrNull { Lanzador.instalado(app, it) } ?: apps.firstOrNull { l.etiqueta.containsMatchIn(it.nombre) }?.paquete

    internal var appsCache: Pair<Long, List<App>>? = null
    internal fun appsInstaladasRapido(): List<App> {
        val c = appsCache
        if (c != null && SystemClock.uptimeMillis() - c.first < 60_000) return c.second
        val l = try { Lanzador.apps(app) } catch (_: Exception) { emptyList() }
        appsCache = SystemClock.uptimeMillis() to l
        return l
    }

    internal fun leerJuegosPC(t: String): List<JuegoPC> = t.split("\n").mapNotNull { linea ->
        val c = linea.split("\t")
        if (c.size < 3) null else c[1].toIntOrNull()?.let { JuegoPC(c[0], it, c[2]) }
    }

    internal fun guardarJuegosPC(l: List<JuegoPC>) {
        almacen.guardarTexto("juegosPC", l.joinToString("\n") { "${it.titulo.replace("\t", " ")}\t${it.id}\t${it.tienda}" })
        actualizar { it.copy(juegosPC = l) }
        publicarJuegos()
    }

    /** Quita un juego de PC del estante (clave "steam/pc-<id>" o "windows/pc-<id>"). */
    internal fun quitarJuegoPC(clave: String) {
        val id = clave.substringAfter("/pc-").toIntOrNull() ?: return
        guardarJuegosPC(estado.value.juegosPC.filter { it.id != id })
        avisar("Quitado de tu estante")
    }

    /** Cada juego de PC agregado es un archivito con su número (como los que exporta ES-DE), en la carpeta de Díptico. */
    internal fun juegosDePC(): List<Juego> = estado.value.juegosPC.mapNotNull { g ->
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

    internal fun opcionesPC(e: Estado): List<Opcion> = buildList {
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
    internal fun descargarFaltantesLuego() {
        scope.launch {
            delay(600)
            estado.value.juegos.filter { it.clave.contains("/pc-") && it.portada == null }.forEach { descargarUno(it) }
        }
    }

    // ---------- Rendimiento: modo turbo ----------

    /** Paquetes que el turbo nunca cierra: emuladores, la música que suena y el juego mismo (si es una app). */
    internal fun protegidosTurbo(juego: Juego?): Set<String> = buildSet {
        addAll(Consolas.reglas.values.flatten().map { it.substringBefore('/') })
        cl.nikalbazu.diptico.audio.Medios.sonando.value?.paquete?.let { add(it) }
        addAll(estado.value.ports); addAll(estado.value.android)
        if (juego?.uri?.scheme == "app") add(juego.archivo)
    }

    internal fun turboAlJugar(juego: Juego, displayId: Int) {
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
    internal fun vigilarReposo() {
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

    internal fun opcionesTurbo(e: Estado): List<Opcion> {
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

    internal val NOMBRE_TELE = Regex("hdmi|displayport|\\bdp\\b|external|externa|usb|\\btv\\b|wireless|cast|miracast", RegexOption.IGNORE_CASE)

    /** Batería baja (15 % o menos, sin cargar) mientras navegas: un aviso una sola vez por carga. */
    internal fun vigilarBateria() {
        scope.launch {
            var avisado = false
            var calorAvisado = false
            while (true) {
                delay(60_000)
                // Calor fuera de partida: si ya viene caliente (por cargar o por el último juego), un aviso hasta que se enfríe.
                if (estado.value.extra.termica && estado.value.sesion == null) {
                    val t = try { withContext(Dispatchers.IO) { Termica.leer(app) } } catch (_: Exception) { null }
                    if (t != null) {
                        if (t.nivel <= Termica.Nivel.TIBIA) calorAvisado = false
                        else if (!calorAvisado && estado.value.aLaVista > 0) {
                            calorAvisado = true
                            avisar("La consola está caliente: déjala descansar un ratito antes de jugar algo pesado")
                        }
                        if (t.nivel >= Termica.Nivel.CALIENTE) avisoCalor(t) else quitarAvisoDippo("calor")
                    }
                }
                val b = try { infoBateria() } catch (_: Exception) { continue }
                val cargando = b.estado == "Cargando" || b.estado == "Llena"
                if (cargando || b.nivel > 20) { avisado = false; quitarAvisoDippo("bateria") }
                if (!avisado && !cargando && b.nivel in 1..15 && estado.value.sesion == null && estado.value.aLaVista > 0) {
                    avisado = true
                    avisar("Te queda ${b.nivel}% de batería: buen momento para guardar y enchufar")
                    avisoDippo("bateria", "Te queda ${b.nivel}% de batería", "Buen momento para guardar tu partida y enchufar la consola.")
                    Sonidos.aviso()
                }
            }
        }
    }

    /** Mira si hay una tele conectada (HDMI, USB-C, inalámbrica) y avisa al conectar o desconectar. */
    internal fun vigilarTele() {
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
    internal fun buscarTele(dm: android.hardware.display.DisplayManager): String? {
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
    internal fun aplicarSobremesa() {
        val e = estado.value
        Sonidos.volumen = if (e.sobremesa && e.sobremesaSonidos) 0.9f else 0.6f
        aplicarFluidez()
    }

    internal fun guardarSobremesa(clave: String, valor: String, f: (Estado) -> Estado) {
        almacen.guardarTexto(clave, valor)
        actualizar(f)
        aplicarSobremesa()
    }

    internal fun opcionesSobremesa(e: Estado): List<Opcion> {
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

    internal fun leerBotonesExtra(t: String): List<BotonExtra> {
        if (t.isBlank()) return BotonExtra.PORDEFECTO
        return t.split(";").mapNotNull { p ->
            val c = p.split(",")
            if (c.size < 3) null else BotonExtra(c[0], c[1].toIntOrNull() ?: return@mapNotNull null, c[2], c.getOrNull(3) == "start")
        }.ifEmpty { BotonExtra.PORDEFECTO }
    }

    internal fun guardarBotonesExtra(l: List<BotonExtra>) {
        almacen.guardarTexto("botonesExtra", l.joinToString(";") { "${it.nombre},${it.tecla},${it.accion},${if (it.conStart) "start" else "solo"}" })
        actualizar { it.copy(botonesExtra = l) }
    }

    internal fun cambiarAccionBoton(i: Int, d: Int) {
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

    internal fun teclasCombinadasReservadas(e: Estado) =
        listOf(e.atajoBisa, e.atajoApps).mapNotNull { AtajoInicio.leer(it, AtajoInicio.NO).takeUnless { a -> a.doble }?.tecla }

    internal fun alternarModoBoton(i: Int) {
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

    internal fun opcionesBotones(e: Estado): List<Opcion> = buildList {
        fun opcionAtajo(bisa: Boolean): Opcion {
            val clave = if (bisa) "atajoBisa" else "atajoApps"
            val titulo = if (bisa) "Atajo de DIPPO" else "Atajo del cajón de apps"
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
        add(Opcion("Volver a lo original", icono = Icono.BORRAR, ayuda = "L4 = DIPPO y R4 = música, con las teclas de siempre.") { guardarBotonesExtra(BotonExtra.PORDEFECTO) })
    }

    // ---------- Widgets con vida: foco con la cruceta y contenido al abrirlos ----------
    internal val MINUTOS_TEMPORIZADOR = listOf(1, 3, 5, 10, 15, 20, 25, 30, 45, 60, 90, 120)
    internal val PAUSAS = listOf(0, 30, 45, 60, 90, 120)

    data class InfoBateria(val nivel: Int, val estado: String, val temperatura: Float, val voltaje: Int, val salud: String)

    // ---------- Imagen grande del inicio y cajas en "Todos" ----------

    // ---------- Dictado por voz (para DIPPO y el teclado) ----------

    /** Abre el reconocimiento de voz de Android (lo registra la pantalla activa). */
    var pedirVoz: (() -> Boolean)? = null
    internal var vozPara = ""

    fun dictar(para: String) {
        vozPara = para
        cl.nikalbazu.diptico.audio.VozDippo.callar()
        if (pedirVoz?.invoke() != true) avisar("Esta consola no tiene dictado por voz. Instala la app de Google para usarlo.")
    }

    fun vozRecibida(texto: String) {
        val t = texto.trim()
        if (t.isEmpty()) return
        when (vozPara) {
            "bisa" -> {
                if (estado.value.panel != Panel.ASISTENTE) abrirAsistente()
                porVoz = true
                sumarDippo("voz")
                preguntar(t.replaceFirstChar { it.uppercase() })
            }
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

    internal fun guardarColecciones(m: Map<String, Coleccion>) {
        val o = org.json.JSONObject()
        m.forEach { (k, c) -> o.put(k, org.json.JSONObject().put("nombre", c.nombre).put("juegos", org.json.JSONArray(c.juegos))) }
        almacen.guardarTexto("colecciones", o.toString())
        actualizar { it.copy(colecciones = m) }
    }

    internal fun leerColecciones(s: String): Map<String, Coleccion> = try {
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

    internal fun leerRaspadores() = cl.nikalbazu.diptico.data.Raspadores.Claves(
        almacen.texto("ras_igdbId"), almacen.texto("ras_igdbSecreto"), almacen.texto("ras_ssUsuario"), almacen.texto("ras_ssClave"),
        almacen.texto("ras_ssDevId"), almacen.texto("ras_ssDevClave"), almacen.texto("ras_rawg"), almacen.texto("ras_tgdb"),
    )

    internal fun guardarRaspador(campo: String, valor: String) {
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
    internal var cadenaRaspador: List<String> = emptyList()

    /** Conecta una fuente pidiendo sus datos uno tras otro (ScreenScraper pide 4; IGDB, 2). */
    internal fun conectarFuente(campos: List<String>) {
        val c = estado.value.raspadores
        cadenaRaspador = campos.drop(1)
        abrirTexto("ras_${campos.first()}", valorRaspador(c, campos.first()))
    }

    internal fun valorRaspador(c: cl.nikalbazu.diptico.data.Raspadores.Claves, campo: String) = when (campo) {
        "igdbId" -> c.igdbId; "igdbSecreto" -> c.igdbSecreto; "ssUsuario" -> c.ssUsuario; "ssClave" -> c.ssClave
        "ssDevId" -> c.ssDevId; "ssDevClave" -> c.ssDevClave; "rawg" -> c.rawg; else -> c.tgdb
    }

    internal fun opcionesRaspadores(e: Estado): List<Opcion> {
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

    internal fun ciclarHeroe(clave: String) {
        val l = listOf(null, "arte", "portada")
        val v = l[(l.indexOf(estado.value.heroes[clave]) + 1).mod(l.size)]
        val m = if (v == null) estado.value.heroes - clave else estado.value.heroes + (clave to v)
        almacen.guardarTexto("heroes", escribirMapa(m))
        actualizar { it.copy(heroes = m) }
    }

    fun nombreFormaTodos(v: String) = when (v) { "cuadrada" -> "Todas cuadradas"; "propia" -> "Cada consola la suya"; else -> "Todas en póster" }

    internal fun ciclarFormaTodos() {
        val l = listOf("poster", "cuadrada", "propia")
        val v = l[(l.indexOf(estado.value.formaTodos) + 1).mod(l.size)]
        almacen.guardarTexto("formaTodos", v)
        actualizar { it.copy(formaTodos = v) }
    }

    internal fun imagenTodasLasCajas() {
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

    internal var grabacionJob: Job? = null

    internal fun alternarGrabacion(clave: String) {
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

    internal fun terminarGrabacion(clave: String) {
        grabacionJob?.cancel()
        val ok = Notas.terminar(app)
        actualizar { it.copy(grabando = 0L, quedaste = !ok && it.quedaste, notasVoz = if (ok) it.notasVoz + clave else it.notasVoz) }
        if (ok) { Sonidos.aviso(); avisar("Nota de voz guardada") } else avisar("No se grabó nada")
    }

    fun escucharNota(clave: String) = Notas.reproducir(app, clave)

    internal fun leerNotasVoz(): Set<String> = try {
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

    internal fun guardarMapas(m: Map<String, MapaJuego>) {
        val o = org.json.JSONObject()
        m.forEach { (k, v) -> o.put(k, org.json.JSONObject().put("uri", v.uri).put("x", v.x.toDouble()).put("y", v.y.toDouble())) }
        almacen.guardarTexto("mapas", o.toString())
        actualizar { it.copy(mapas = m) }
    }

    internal fun leerMapas(s: String): Map<String, MapaJuego> = try {
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

    internal var ultimaConsultaAct: Long? = null

    internal fun vigilarActualizaciones() {
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
                    avisoDippo("diptico", "Díptico ${v.nombre} ya está disponible", "Toca para verla y descargarla desde Acerca de.") { abrirActualizacion() }
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

    internal fun opcionesActualizar(e: Estado): List<Opcion> = buildList {
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

    internal fun formatoTiempoCorto(seg: Long): String {
        val min = seg / 60
        return if (min >= 60) "${min / 60} h ${min % 60} min" else "$min min"
    }

    internal fun abrirAjusteAndroid(accion: String) {
        try { app.startActivity(android.content.Intent(accion).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { avisar("No pude abrir ese ajuste") }
    }

    // ---------- Encuadrar portadas y heroes ----------

    internal fun leerEncuadres(t: String): Map<String, Encuadre> = try {
        val o = org.json.JSONObject(t)
        o.keys().asSequence().associateWith { k ->
            val p = o.getString(k).split(",").map { it.toFloatOrNull() ?: 0f }
            Encuadre(p.getOrElse(0) { 0f }, p.getOrElse(1) { 0f }, p.getOrElse(2) { 1f })
        }
    } catch (_: Exception) { emptyMap() }

    internal fun guardarEncuadres(m: Map<String, Encuadre>) {
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
    /** Busca varias opciones (libretro, SteamGridDB y Wikipedia) para la portada, el hero o el logo. */
    /** Fuentes de la tienda de imágenes (pestañas). */
    val FUENTES_IMAGEN = listOf("todas", "Steam", "SteamGridDB", "libretro", "ScreenScraper", "IGDB", "TheGamesDB", "RAWG", "Wikipedia")
    internal var busquedaImagenes: Job? = null

    // ---------- Elegir archivos (imágenes y música) ----------

    /** La pantalla activa registra aquí cómo pedir un archivo de un tipo (imagen o audio). */
    var pedirArchivo: ((String) -> Unit)? = null
    internal var archivoPara = ""

    var pedirExportarPartida: ((String) -> Unit)? = null
    internal var copiaParaExportar: java.io.File? = null
    internal var opcionesCopiasPartida: List<ItemGaleria> = emptyList()

    internal var capturaPara: Pair<String, String>? = null

    // ---------- Estante: vista, tamaño, filtros, búsqueda ----------

    /** Las vistas de juegos (9 nov 2026: de nueve a cuatro, las que de verdad se usan). */
    internal val VISTAS = listOf("mosaicos", "xbox", "cuadricula", "lista", "carrusel")
    fun nombreVista(v: String) = when (v) { "xbox" -> "Mosaicos Xbox"; "cuadricula" -> "Galería"; "lista" -> "Lista"; "carrusel" -> "Carrusel"; else -> "Mosaicos" }

    /** Cómo se eligen las consolas: la fila de logos (el diseño nuevo), carpetas o sistemas en grande. */
    val VISTAS_CONSOLAS = listOf("arriba", "carpetas", "sistemas")
    /** Vistas donde primero se elige la consola (en grande) y después se entra a sus juegos. */
    val VISTAS_CARPETAS = setOf("carpetas", "mosaico", "vitrina")
    val MODOS_ELEGIR = VISTAS_CARPETAS + setOf("sistemas", "discos")
    fun nombreVistaConsolas(v: String) = when (v) {
        "lado" -> "Al costado"; "rueda" -> "Rueda"; "sistemas" -> "Sistemas"; "carpetas" -> "Carpetas"
        "mosaico" -> "Mosaico"; "vitrina" -> "Vitrina"; "discos" -> "Discos"; else -> "Arriba"
    }

    /** Cambia cómo se ven las consolas (arriba, al costado, rueda o sistemas). */
    internal fun vistaConsolasCompatible(vista: String): String = when {
        vista == "habitacion" || vista == "mosaico" || vista == "vitrina" -> "carpetas"
        vista == "discos" -> "sistemas"
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

    internal val TAMANOS_CARPETAS = listOf(.65f, 1f, 1.3f, 1.6f, 1.9f)

    internal fun limitarTamCarpetas(tam: Float): Float =
        if (tam.isFinite()) tam.coerceIn(TAMANOS_CARPETAS.first(), TAMANOS_CARPETAS.last()) else 1f

    fun guardarTamCarpetas(tam: Float) {
        val nuevo = Densidades.carpetas.masCercana(tam).escala
        if (nuevo == estado.value.tamCarpetas) return
        almacen.guardarTexto("tamCarpetas", nuevo.toString())
        actualizar { it.copy(tamCarpetas = nuevo) }
    }

    internal fun cambiarTamCarpetas(agrandar: Boolean) {
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
        if (estado.value.vistaConsolas in MODOS_ELEGIR) actualizar { it.copy(enSistemas = true, widgetFoco = -1, consolaSel = consolaDeCarpeta(it)) }
    }

    /** Sin la carpeta «Todos los juegos», la selección de carpetas nunca queda en «todo»: cae en la consola del juego o en la primera. */
    internal fun consolaDeCarpeta(e: Estado): String {
        if (e.consolaSel != "todo" || e.extra.carpetaTodos) return e.consolaSel
        return e.seleccionado?.consolaId?.takeIf { it in e.pestanas } ?: e.pestanas.firstOrNull() ?: "todo"
    }

    /** R3 o el botón Vista: estante, cuadrícula o lista. */
    fun cambiarVista() {
        val v = VISTAS[(VISTAS.indexOf(estado.value.vista) + 1).mod(VISTAS.size)]
        almacen.guardarTexto("vista", v)
        actualizar { it.copy(vista = v) }
        avisar("Vista: ${nombreVista(v)}")
    }

    fun nombreTamJuegos(t: Float) = Densidades.juegos.masCercana(t).nombre

    fun elegirTamJuegos(tam: Float) {
        val t = Densidades.juegos.masCercana(tam).escala
        if (t == estado.value.tamJuegos) return
        almacen.guardarTexto("tamJuegos", t.toString())
        actualizar { it.copy(tamJuegos = t) }
    }

    fun cambiarTamJuegos(delta: Int = 1) = elegirTamJuegos(Densidades.juegos.siguiente(estado.value.tamJuegos, delta).escala)

    /** Conserva la vista y la selección. El gesto y el mando usan los mismos tamaños fijos. */
    fun pellizcar(agrandar: Boolean) = cambiarTamJuegos(if (agrandar) 1 else -1)

    val FILTROS = listOf("todos", "favoritos", "recientes", "mas", "sinjugar", "ocultos")
    fun nombreFiltro(f: String) = when (f) {
        "favoritos" -> "Favoritos"; "recientes" -> "Jugados hace poco"; "mas" -> "Más jugados"; "sinjugar" -> "Sin jugar"; "ocultos" -> "Ocultos"; else -> "Todos"
    }

    val ORDENES = listOf("az", "recientes", "tiempo", "propio")
    fun nombreOrden(o: String) = when (o) { "recientes" -> "Últimos jugados"; "tiempo" -> "Más tiempo"; "propio" -> "A tu gusto"; else -> "A → Z" }

    // ---------- Tu propio orden ----------

    internal fun leerOrdenPropio(t: String): Map<String, List<String>> = try {
        val o = org.json.JSONObject(t)
        o.keys().asSequence().associateWith { k -> o.getString(k).split("\u0001").filter { it.isNotEmpty() } }
    } catch (_: Exception) { emptyMap() }

    internal fun guardarOrdenPropio(m: Map<String, List<String>>) {
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
    fun ordenarCarpetas(alfabetico: Boolean) {
        val lista = estado.value.consolasConJuegos
        val ids = (if (alfabetico) lista.sortedBy { it.nombre.lowercase(java.util.Locale.ROOT) }
            else Consolas.todas.filter { c -> lista.any { it.id == c.id } }).map { it.id }
        almacen.guardarTexto("ordenConsolas", ids.joinToString(","))
        actualizar { it.copy(ordenConsolas = ids) }
    }

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

    internal fun Estado.antesDeFiltrarOcultos() = BibliotecaAntesDeOcultos(filtro, busqueda, consolaSel, juegoSel, enSistemas, inicio)

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

    internal val IMAGENES_CAJA = listOf("portada", "arte", "logo")
    fun nombreImagenCaja(i: String) = when (i) { "arte" -> "Captura del juego"; "logo" -> "Logo"; else -> "Portada" }

    internal fun guardarFormas(m: Map<String, FormaCfg>) {
        val o = org.json.JSONObject()
        m.forEach { (k, f) -> o.put(k, org.json.JSONObject().put("forma", f.forma).put("tam", f.tam.toDouble()).put("imagen", f.imagen)) }
        almacen.guardarTexto("formas", o.toString())
        actualizar { it.copy(formas = m) }
    }

    internal fun leerFormas(s: String): Map<String, FormaCfg> = try {
        val o = org.json.JSONObject(s)
        o.keys().asSequence().associateWith { k ->
            o.getJSONObject(k).let { f -> FormaCfg(f.optString("forma", "original"), f.optDouble("tam", 1.0).toFloat(), f.optString("imagen", "portada")) }
        }
    } catch (e: Exception) {
        emptyMap()
    }

    internal fun cambiarForma(consola: String, f: (FormaCfg) -> FormaCfg) {
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

    internal fun ciclarTamForma(consola: String) = cambiarForma(consola) {
        val l = listOf(0.8f, 1f, 1.2f)
        it.copy(tam = l[(l.indexOfFirst { t -> kotlin.math.abs(t - it.tam) < 0.01f } + 1).mod(l.size)])
    }

    internal fun ciclarImagenCaja(consola: String) = cambiarForma(consola) {
        it.copy(imagen = IMAGENES_CAJA[(IMAGENES_CAJA.indexOf(it.imagen) + 1).mod(IMAGENES_CAJA.size)])
    }

    internal fun opcionesEstante(e: Estado): List<Opcion> {
        val c = Consolas.porId(e.consolaSel)
        val lista = mutableListOf(
            Opcion("Switch 2 y ports nativos", "NovaS · Banana Native · ports", Icono.MANDO,
                "Un centro propio para lo nuevo: revisa si tu consola aguanta Switch 2, elige NovaS como emulador, juega como port nativo los juegos que convierte Banana Native y consigue los ports nativos (Zelda, Mario 64, Crash…), que aparecen solos en tu carpeta Ports.",
            ) { cerrarPanel(); abrirNativos(pestana = if (estado.value.consolaSel == Consolas.PORTS) 1 else 0) },
            Opcion("Galería de artes", "${e.visibles.count { it.arte != null }} artes", Icono.PORTADA,
                "Los artes y portadas de los juegos de esta carpeta, en grande en la otra pantalla. Elige uno con A para ir a ese juego.",
            ) { abrirGaleria("artes") },
            Opcion(
                "Vista de consolas", nombreVistaConsolas(e.vistaConsolas), Icono.MANDO,
                "Arriba como pestañas, en una columna al costado, en una rueda que gira, Sistemas (un carrusel grande), Carpetas (cuadrícula de íconos que abren sus juegos) o Discos (cada consola es un vinilo en su funda, y la elegida saca su disco girando). En Sistemas, Carpetas y Discos primero eliges la consola y después entras a sus juegos.",
                menos = { cambiarVistaConsolas(-1) },
            ) { abrirGaleria("vistas_consolas") },
            Opcion("Barra de abajo", if (e.extra.franjaAbajo) "Sí" else "No", Icono.APPS,
                "La barra fija de la pantalla de abajo con la lupa, tus apps y los ajustes. Apagada, los abres con los botones del mando o desde el menú.") {
                val v = !estado.value.extra.franjaAbajo
                almacen.guardarBool("franjaAbajo", v)
                actualizar { it.copy(extra = it.extra.copy(franjaAbajo = v)) }
            },
                        Opcion("Carpeta «Todos los juegos»", if (e.extra.carpetaTodos) "Sí" else "No", Icono.CARPETA,
                "Agrega a la biblioteca una carpeta con todos tus juegos juntos, además de la de cada consola. Apagada, solo ves una carpeta por consola.") {
                val v = !estado.value.extra.carpetaTodos
                almacen.guardarBool("carpetaTodos2", v)
                actualizar { it.copy(extra = it.extra.copy(carpetaTodos = v)) }
                actualizar { it.copy(consolaSel = if (v) it.consolaSel else consolaDeCarpeta(it)) }
            },
            opcionLista("Estilo de las carpetas", Icono.CARPETA,
                "Cómo se ven las carpetas de tus consolas: tarjeta, el dibujo de la consola, un abanico con tus juegos más jugados o el logo grande.",
                ESTILOS_CARPETA.map { it.first }, ESTILOS_CARPETA.map { it.second }, e.extra.estiloCarpetas) { elegirEstiloCarpetas(it) },
            opcionLista("Tono de las carpetas", Icono.CARPETA,
                "Los dibujos de las carpetas pueden ser oscuros (cómodos de noche y con temas oscuros) o claros, como papel. «Según el tema» los pone oscuros cuando tu tema lo es.",
                listOf("auto", "oscuro", "claro"), listOf("Según el tema", "Oscuras", "Claras"), e.extra.carpetasTono) { elegirTonoCarpetas(it) },
            Opcion("Ordenar carpetas", "A tu gusto", Icono.VISTA,
                "Las carpetas tiemblan y las mueves arrastrando con el dedo, o con A y la cruceta. También se entra manteniendo una carpeta presionada. B o «Listo» termina.") {
                cerrarPanel(); empezarOrdenCarpetas()
            },
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
            Opcion("Tamaño de los juegos", nombreTamJuegos(e.tamJuegos), Icono.FORMA, "Cuatro espacios predeterminados. Ajusta con dos dedos o con izquierda y derecha; conserva la vista y el juego elegido.", menos = { cambiarTamJuegos(-1) }) { cambiarTamJuegos(1) },
            Opcion("Mostrar", nombreFiltro(e.filtro), Icono.CORAZON, "Todos, solo favoritos, jugados hace poco, los más jugados, los que no has jugado o los ocultos.") { cambiarFiltro() }
                .conLista(FILTROS.map(::nombreFiltro), FILTROS.indexOf(e.filtro)) { elegirFiltro(FILTROS[it]) },
            Opcion("Ordenar", nombreOrden(e.orden), Icono.MENU, "A → Z, últimos jugados, más tiempo jugado o en el orden que tú elijas.") { cambiarOrden() }
                .conLista(ORDENES.map(::nombreOrden), ORDENES.indexOf(e.orden)) { elegirOrden(ORDENES[it]) },
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
            .conLista(listOf("poster", "cuadrada", "propia").map(::nombreFormaTodos), listOf("poster", "cuadrada", "propia").indexOf(e.formaTodos)) { n ->
                val v = listOf("poster", "cuadrada", "propia")[n]
                almacen.guardarTexto("formaTodos", v)
                actualizar { it.copy(formaTodos = v) }
            }
        lista += Opcion("Imagen en todas las cajas", nombreImagenCaja(e.formas.values.firstOrNull()?.imagen ?: "portada"), Icono.PORTADA,
            "Portada, captura del juego o logo, para las cajas de todas tus consolas a la vez.") { imagenTodasLasCajas() }
        val mas = Opcion(if (e.estanteAvanzado) "Ver menos opciones" else "Más opciones de cajas", icono = Icono.AJUSTES) { actualizar { it.copy(navegacion = it.navegacion.copy(estanteAvanzado = !it.estanteAvanzado)) } }
        return (if (e.estanteAvanzado) lista else lista.take(9)) + mas
    }

    // ---------- Tamaño de cada pantalla ----------

    internal val ZOOMS = listOf(1f, 0.85f, 0.92f, 1.08f, 1.16f)
    fun nombreZoom(z: Float) = when {
        kotlin.math.abs(z - 1f) < 0.01f -> "Automático"
        z < 0.9f -> "Muy compacto"; z < 1f -> "Compacto"; z < 1.12f -> "Grande"; else -> "Muy grande"
    }

    internal fun fijarZoom(arriba: Boolean, z: Float) {
        almacen.guardarTexto(if (arriba) "zoomArriba" else "zoomAbajo", z.toString())
        actualizar { if (arriba) it.copy(zoomArriba = z) else it.copy(zoomAbajo = z) }
    }

    internal fun ciclarZoom(arriba: Boolean) {
        val actual = if (arriba) estado.value.zoomArriba else estado.value.zoomAbajo
        val z = ZOOMS[(ZOOMS.indexOfFirst { kotlin.math.abs(it - actual) < 0.01f } + 1).mod(ZOOMS.size)]
        almacen.guardarTexto(if (arriba) "zoomArriba" else "zoomAbajo", z.toString())
        actualizar { if (arriba) it.copy(zoomArriba = z) else it.copy(zoomAbajo = z) }
    }

    // ---------- Centro de control y notificaciones ----------
    /** Columnas de los accesos del centro de control. */
    const val COLUMNAS_CENTRO = 6

    /** Apps de la consola (AYN, Odin, Thor) y herramientas de la comunidad, encontradas una vez. */
    @Volatile internal var appsConsola: List<App>? = null

    internal val PISTAS_CONSOLA = listOf("ayn", "odin", "thor", "mjolnir", "wayfinder", "handheld", "gamecontroller", "padsettings")
    internal val MUSICA_APPS = listOf(
        "com.spotify.music" to "Spotify",
        "com.google.android.apps.youtube.music" to "YT Music",
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
    internal fun raizDe(j: Juego): Uri? = try {
        android.provider.DocumentsContract.buildTreeDocumentUri(j.uri.authority, android.provider.DocumentsContract.getTreeDocumentId(j.uri))
    } catch (e: Exception) {
        almacen.carpeta
    }

    /** Lista de consolas para elegir a mano: las más usadas primero. */
    fun consolasElegibles(): List<Consola> = Consolas.todas

    fun abrirCarpetas() = actualizar { it.copy(panel = Panel.CARPETAS, indice = 0, consolaPara = -1, carpetas = almacen.carpetas()) }

    /** Si una carpeta nueva no tiene juegos reconocibles, pregunta de qué consola es. */
    internal fun revisarCarpetaNueva(uri: Uri) {
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

    internal fun aceptarCarpetas() {
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
    internal fun detectarFluidez(): String {
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

    internal fun aplicarFluidez() {
        val e = estado.value
        // El perfil de sobremesa puede activar efectos. El reposo durante el juego tiene prioridad.
        val ahorro = fluidezEfectiva(e) == "ahorro" && !(e.sobremesa && e.sobremesaEfectos)
        val efectos = !ahorro && !(e.sesion != null && e.turbo && e.turboReposo)
        cl.nikalbazu.diptico.ui.Estilo.efectosCompletos = efectos
        // Las decoraciones se ven en todo momento, también mientras juegas (salvo con el reposo del modo turbo, que apaga los efectos).
        cl.nikalbazu.diptico.ui.Estilo.decoraciones = efectos && e.decoraciones && e.experiencia.movimiento
        // Con un juego abierto se dibujan quietas: ninguna animación le roba cuadros al emulador.
        cl.nikalbazu.diptico.ui.Estilo.decorQuieta = e.sesion != null
        cl.nikalbazu.diptico.ui.Estilo.contraste = e.extra.altoContraste
    }

    internal var ultimaActividad = SystemClock.uptimeMillis()

    /** Se llama con cada botón o toque. Devuelve true si cerró el protector (y el toque no debe hacer nada más). */
    fun actividad(): Boolean {
        ultimaActividad = SystemClock.uptimeMillis()
        if (estado.value.protector) {
            actualizar { it.copy(protector = false) }
            return true
        }
        return false
    }

    internal fun vigilarProtector() {
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

    internal fun guardarGrilla(cols: Int, filas: Int, paginas: Int) {
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
    internal fun reacomodar(lista: List<Widget>, paginas: Int): List<Widget> {
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
        actualizar { it.copy(widgetSel = i, panel = Panel.ESTUDIO, estudioPestana = 0, indice = 0, estudioZona = "panel", celdaCatalogo = null,
            navegacion = it.navegacion.copy(grupoEditorWidget = 0)) }
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
    internal fun vecinoWidget(e: Estado, desde: Int, dx: Int, dy: Int): Int? {
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

    /** Dónde poner la próxima imagen o video que se elija (si se abrió desde la burbuja). */
    internal var celdaPendiente: Pair<Int, Int>? = null

    internal val OPACIDADES = listOf(1f, 0.85f, 0.7f, 0.5f, 0.3f, 0f)
    internal val RADIOS = listOf(6, 14, 22, 34)
    internal val ESCALAS = listOf(0.85f, 1f, 1.15f, 1.3f)
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
        almacen.guardarBool("clipRetoma2", v); actualizar { it.copy(navegacion = it.navegacion.copy(clipRetoma = v)) }
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
    internal var pantallaRetomaPendiente: String? = null
    internal var claveRetomaPendiente: String? = null
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
    /** Esconde o vuelve a mostrar las apps del dock en la franja de abajo (queda guardado). */
    fun alternarDockVisible() {
        val v = !estado.value.extra.dockVisible
        almacen.guardarBool("dockVisible", v)
        actualizar { it.copy(extra = it.extra.copy(dockVisible = v)) }
    }

    internal fun opcionesDock(e: Estado): List<Opcion> = listOf(
        Opcion(
            "Dock de apps", if (e.extra.dock) "Sí" else "No", Icono.APPS,
            "Hasta 5 de tus apps fijadas, siempre a la vista en la pantalla de abajo, también mientras juegas: la app se abre en esa pantalla y el juego sigue en la otra. Fija apps en el cajón con Y.",
            grupo = "Dock de apps",
        ) {
            val v = !estado.value.extra.dock
            almacen.guardarBool("dock2", v)
            actualizar { it.copy(extra = it.extra.copy(dock = v)) }
        },
        Opcion("Elegir las apps del dock", "${e.appsFijadas.size.coerceAtMost(cl.nikalbazu.diptico.ui.MAX_DOCK)} de ${cl.nikalbazu.diptico.ui.MAX_DOCK}", Icono.ESTRELLA,
            "Abre el cajón en tus apps fijadas: con Y fijas o sueltas una, y puedes ordenarlas. Las primeras 5 van al dock.",
            grupo = "Dock de apps") { abrirApps() },
    )

    fun alternarFijada(paquete: String) {
        val l = estado.value.appsFijadas
        val n = if (paquete in l) l - paquete else l + paquete
        almacen.guardarTexto("appsFijadas", n.joinToString(","))
        actualizar { it.copy(appsFijadas = n) }
        avisar(when {
            paquete !in n -> "App soltada"
            n.size > cl.nikalbazu.diptico.ui.MAX_DOCK -> "App fijada · el dock muestra las primeras ${cl.nikalbazu.diptico.ui.MAX_DOCK}"
            else -> "App fijada en tu dock"
        })
    }

    /** Abre la información de la app en los ajustes de Android (para desinstalar, permisos…). */
    fun infoApp(paquete: String) {
        abrirIntent(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$paquete")), "No pude abrir la información de la app")
    }

    // ---------- Personalizar consolas ----------

    /** Usa una imagen de la tienda para la consola que se está editando. */
    internal fun usarCandidatoConsola(img: cl.nikalbazu.diptico.data.ImagenWeb) {
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

    internal var editarDesdeMenu = false

    internal fun leerConsolasPropias(s: String): Map<String, ConsolaPropia> = try {
        val o = org.json.JSONObject(s)
        o.keys().asSequence().associateWith { k ->
            val c = o.getJSONObject(k)
            fun txt(n: String) = c.optString(n, "").ifEmpty { null }
            ConsolaPropia(txt("nombre"), c.optLong("color", 0L), txt("hero"), txt("logo"), txt("musica"), txt("portadaCarpeta"))
        }
    } catch (e: Exception) {
        emptyMap()
    }

    internal fun escribirConsolasPropias(m: Map<String, ConsolaPropia>): String = org.json.JSONObject().apply {
        m.forEach { (k, c) ->
            put(k, org.json.JSONObject().put("nombre", c.nombre ?: "").put("color", c.color).put("hero", c.hero ?: "").put("logo", c.logo ?: "").put("musica", c.musica ?: "").put("portadaCarpeta", c.portadaCarpeta ?: ""))
        }
    }.toString()

    internal fun cambiarConsolaPropia(id: String, f: (ConsolaPropia) -> ConsolaPropia) {
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

    internal val COLORES_CONSOLA = listOf(0L) + Temas.MUESTRAS

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

    internal fun moverWidgetPagina(i: Int, d: Int) {
        val e = estado.value
        val w = e.widgets.getOrNull(i) ?: return
        val p = (w.pagina + d).coerceIn(0, e.paginas - 1)
        if (p == w.pagina) { avisar("Agrega páginas en la pestaña Página"); return }
        ponerWidget(i, w.x, w.y, p)
    }

    internal fun cambiarHueco(d: Int) {
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

    internal fun opcionesGrilla(e: Estado): List<Opcion> = listOf(
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




/** Las caras reales de una caja que se encontraron en internet (enlaces) y cuándo se buscaron. */
data class CarasGuardadas(val reverso: String?, val lomo: String?, val buscado: Long, val frente: String? = null) {
    companion object {
        fun leer(t: String): Map<String, CarasGuardadas> = try {
            val o = org.json.JSONObject(t.ifBlank { "{}" })
            o.keys().asSequence().associateWith { k ->
                val v = o.getJSONObject(k)
                CarasGuardadas(v.optString("r").ifBlank { null }, v.optString("l").ifBlank { null }, v.optLong("b"), v.optString("f").ifBlank { null })
            }
        } catch (_: Exception) { emptyMap() }

        fun escribir(m: Map<String, CarasGuardadas>): String {
            val o = org.json.JSONObject()
            for ((k, v) in m) o.put(k, org.json.JSONObject().put("r", v.reverso.orEmpty()).put("l", v.lomo.orEmpty()).put("b", v.buscado).put("f", v.frente.orEmpty()))
            return o.toString()
        }
    }
}

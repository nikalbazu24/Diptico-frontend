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

enum class Panel { NINGUNO, RAPIDO, PASO, MENU, CLAVE, PORTS, FICHA, WIDGETS, TRABAJOS, BUSCAR, CENTRO, CARPETAS, ESTADISTICAS, IMAGENES, ASISTENTE }

/** Carpetas que Díptico puede pedir. */
enum class Carpeta { JUEGOS, MEDIA, MUSICA }

/** Pasos del tutorial guiado del primer arranque. */
enum class PasoTuto { BIENVENIDA, PANTALLAS, TEXTO, DISPOSICION, JUEGOS, CARPETA, EMULADORES, PORTS, PORTADAS, MUSICA, APARIENCIA, CONTROLES, LISTO }

/** Íconos propios de Díptico (se dibujan en ui/Iconos.kt). */
enum class Icono {
    PANTALLAS, UNA, NEGRO, LIBRE, INTERCAMBIAR, TEXTO, TEMA, INTRO, MUSICA, VOLUMEN, SILENCIO, CARPETA, EMULADOR,
    PORTADA, DESCARGA, CLAVE, TITULO, BORRAR, BIBLIOTECA, BUSCAR, TUTORIAL, INFO, CERRAR, JUGAR, DADO, LAPIZ,
    CONTINUAR, MENU, CHECK, ARRIBA, ABAJO, ESTRELLA, APPS, RELOJ, PALETA, MOVIL, CHISPA, CORAZON, MUNDO, WEB,
    CAMPANA, VISTA, WIFI, BLUETOOTH, BRILLO, AJUSTES, FORMA, MANDO, BATERIA, TROFEO, ASISTENTE, IDIOMA,
}

/** Secciones del menú principal. */
enum class Seccion(val titulo: String, val icono: Icono, val ayuda: String) {
    PANTALLAS("Pantallas", Icono.PANTALLAS, "Cuántas pantallas usa Díptico, qué va en cada una y el tamaño de la letra."),
    INICIO("Inicio", Icono.APPS, "Cómo se ve la pantalla principal: el juego en grande o tu inicio con widgets."),
    ESTANTE("Estante", Icono.VISTA, "Cómo se ven tus juegos: vista, tamaño, orden y la forma de las cajas de cada consola."),
    TEMAS("Temas", Icono.PALETA, "Colores, temas de fiestas del mundo y temas creados con IA."),
    APARIENCIA("Apariencia", Icono.TEMA, "Claro u oscuro, decoraciones, íconos de las consolas y la animación de entrada."),
    MUSICA("Música", Icono.MUSICA, "La música de fondo. Se calla sola al abrir un juego o si pones Spotify o YouTube Music."),
    EMULADORES("Emuladores", Icono.EMULADOR, "Con qué emulador, o con qué núcleo de RetroArch, se abre cada consola."),
    PORTADAS("Portadas", Icono.PORTADA, "Descarga portadas, imágenes, logos y títulos de internet."),
    ASISTENTE("Asistente", Icono.ASISTENTE, "Un asistente con IA que conoce tus juegos: recomienda, explica y cambia cosas de Díptico por ti."),
    IDIOMA("Idioma", Icono.IDIOMA, "El idioma de Díptico: español, inglés, portugués, chino o japonés."),
    LOGROS("Logros", Icono.TROFEO, "Conecta tu cuenta de RetroAchievements para ver tus logros y puntos en cada juego."),
    BIBLIOTECA("Biblioteca", Icono.BIBLIOTECA, "Tu carpeta de juegos y las imágenes que ya tengas de ES-DE."),
    CONSOLA("Consola", Icono.CAMPANA, "Notificaciones, Díptico como launcher principal y accesos rápidos de Android."),
    SISTEMA("Sistema", Icono.INFO, "Repite el tutorial o mira la versión de Díptico."),
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
)

/** Forma de las cajas de una consola en el estante. */
data class FormaCfg(val forma: String = "original", val tam: Float = 1f, val imagen: String = "portada")

object Widgets {
    /** Tamaño de la grilla (se puede cambiar en Menú › Inicio). */
    var COLUMNAS = 6
    var FILAS = 4

    /** Tamaños y formas posibles (ancho x alto en celdas). */
    val TAMANOS = listOf(
        2 to 1, 2 to 2, 3 to 2, 4 to 2, 6 to 1, 2 to 3, 3 to 3, 4 to 3, 6 to 2, 3 to 1,
    )

    fun nombreTamano(an: Int, al: Int) = when (an to al) {
        2 to 1 -> "Chico"; 2 to 2 -> "Cuadrado"; 3 to 2 -> "Mediano"; 4 to 2 -> "Ancho"; 6 to 1 -> "Franja"
        2 to 3 -> "Alto"; 3 to 3 -> "Grande"; 4 to 3 -> "Muy grande"; 6 to 2 -> "Panorámico"; 3 to 1 -> "Barra"
        else -> "${an}x$al"
    }

    val porDefecto = listOf(
        Widget("juego", 0, 0, 4, 2), Widget("reloj", 4, 0, 2, 2), Widget("musica", 0, 2, 3, 2),
        Widget("recientes", 3, 2, 3, 2), Widget("paso", 0, 0, 2, 2, false), Widget("fiesta", 0, 0, 2, 2, false),
        Widget("tiempo", 0, 0, 2, 1, false), Widget("bateria", 0, 0, 2, 1, false), Widget("consolas", 0, 0, 3, 2, false),
        Widget("favoritos", 0, 0, 3, 2, false), Widget("frase", 0, 0, 6, 1, false),
        Widget("estadisticas", 0, 0, 3, 2, false), Widget("sugerencia", 0, 0, 2, 2, false),
        Widget("accesos", 0, 0, 3, 1, false), Widget("notificaciones", 0, 0, 3, 2, false),
        Widget("logros", 0, 0, 3, 2, false), Widget("asistente", 0, 0, 3, 1, false),
    )

    fun nombre(tipo: String) = when (tipo) {
        "juego" -> "Juego elegido"; "reloj" -> "Reloj y fecha"; "musica" -> "Música (Spotify, YouTube Music…)"
        "paso" -> "Tu próximo paso"; "recientes" -> "Jugados hace poco"; "fiesta" -> "Fiestas del mundo"
        "tiempo" -> "Tiempo jugado"; "bateria" -> "Batería"; "consolas" -> "Tu colección"
        "favoritos" -> "Favoritos"; "frase" -> "Saludo del día"; "estadisticas" -> "Estadísticas"
        "sugerencia" -> "¿Qué juego hoy?"; "accesos" -> "Accesos rápidos"; "notificaciones" -> "Notificaciones"
        "logros" -> "Logros (RetroAchievements)"; "asistente" -> "Asistente con IA"
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
    /** Idioma de Díptico: es, en, pt, zh, ja. */
    val idioma: String = "es",
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
    val inicio: String = "heroe",
    val widgets: List<Widget> = Widgets.porDefecto,
    val estiloIconos: String = "diptico",
    val iconosPropios: Map<String, String> = emptyMap(),
    val iconosVersion: Int = 0,
    val trabajos: List<Trabajo> = emptyList(),
    val relojGrande: Boolean = false,
    /** "estante", "cuadricula" o "lista". */
    val vista: String = "estante",
    val tamJuegos: Float = 1f,
    /** "todos", "favoritos", "recientes", "mas" (más jugados) o "sinjugar". */
    val filtro: String = "todos",
    /** "az", "recientes" o "tiempo". */
    val orden: String = "az",
    val busqueda: String = "",
    val formas: Map<String, FormaCfg> = emptyMap(),
    val editandoWidgets: Boolean = false,
    val widgetSel: Int = 0,
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
    val teclaFila: Int = 1,
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

    const val VERSION = "0.7"

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
            usuarioRA = almacen.texto("usuarioRA"),
            claveRA = almacen.texto("claveRA"),
            inicio = almacen.texto("inicio", "heroe"),
            widgets = leerWidgets(almacen.texto("widgets")),
            estiloIconos = almacen.texto("estiloIconos", "diptico"),
            iconosPropios = leerMapa(almacen.texto("iconosPropios", "{}")),
            vista = almacen.texto("vista", "estante"),
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
            gridFilas = almacen.texto("gridFilas", "4").toIntOrNull() ?: 4,
            paginas = almacen.texto("paginas", "1").toIntOrNull() ?: 1,
            fondoInicio = almacen.texto("fondoInicio").ifEmpty { null },
        )
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
                val propia = if (e.configurado && e.panel != Panel.MENU) e.seleccionado?.let { e.datos[it.clave]?.musica } else null
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

    fun jugar(pantalla: String? = null) {
        val e = estado.value
        val juego = e.seleccionado ?: return
        val consola = Consolas.porId(juego.consolaId) ?: return
        val destino = pantalla ?: e.datos[juego.clave]?.pantalla ?: if (e.unaPantalla) "arriba" else e.pantallaJuegos
        val inferior = Pantallas.inferior(app)
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
            )
        }
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
                teclaFila = if (hacerPregunta) 1 else it.teclaFila,
                teclaCol = if (hacerPregunta) 0 else it.teclaCol,
                mayus = if (hacerPregunta) d.paso.isEmpty() else it.mayus,
            )
        }
    }

    fun despertar() {
        actualizar { it.copy(despierta = true) }
        despiertaJob?.cancel()
        despiertaJob = scope.launch {
            delay(8000)
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
        actualizar { it.copy(panel = Panel.PASO, borrador = it.datos[clave]?.paso ?: "", pregunta = false, teclaFila = 1, teclaCol = 0, simbolos = false, mayus = it.datos[clave]?.paso.isNullOrEmpty()) }
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
        actualizar { it.copy(panel = Panel.CLAVE, textoPara = para, borrador = inicial, teclaFila = 1, teclaCol = 0, simbolos = false, mayus = inicial.isEmpty() && para != "sgdb" && para != "ia_clave" && !para.startsWith("ra_")) }
    }

    fun cerrarPanel() {
        val e = estado.value
        // Desde la clave, se vuelve a donde estabas (menú o tutorial).
        if (e.panel == Panel.CLAVE && (e.textoPara == "titulo" || e.textoPara == "notas")) {
            actualizar { it.copy(panel = Panel.FICHA) }
            return
        }
        if (e.panel == Panel.CLAVE && (e.textoPara == "asistente" || e.textoPara == "ia_clave" && claveParaAsistente)) {
            claveParaAsistente = false
            actualizar { it.copy(panel = Panel.ASISTENTE, indice = 0) }
            return
        }
        if (e.panel == Panel.CLAVE && e.configurado) {
            val s = when {
                e.textoPara.startsWith("ra_") -> Seccion.LOGROS
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
        if (e.panel == Panel.WIDGETS) {
            almacen.guardarTexto("widgets", escribirWidgets(e.widgets))
            actualizar { it.copy(panel = if (e.editandoWidgets) Panel.NINGUNO else Panel.MENU, seccion = Seccion.INICIO, enOpciones = true, indice = 0) }
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
        return letras + listOf(listOf("⇧", if (e.simbolos) "abc" else "#", "␣", "⌫", "✓"))
    }

    fun moverTecla(dx: Int, dy: Int) {
        val e = estado.value
        val filas = filasTeclado(e)
        var f = e.teclaFila
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
        when (t) {
            "⇧" -> actualizar { it.copy(mayus = !it.mayus) }
            "#", "abc" -> actualizar { it.copy(simbolos = !it.simbolos) }
            "␣" -> escribir(" ")
            "⌫" -> borrarLetra()
            "✓" -> terminarTexto()
            else -> {
                escribir(t)
                if (e.mayus && !e.simbolos) actualizar { it.copy(mayus = false) }
            }
        }
        Sonidos.mover()
    }

    fun escribir(t: String) {
        val e = estado.value
        cambiarBorrador(e.borrador + t)
        // Después de un punto, la siguiente va con mayúscula.
        if (t == "." || t == "?" || t == "!") actualizar { it.copy(mayus = true) }
    }

    fun borrarLetra() {
        val b = estado.value.borrador
        if (b.isNotEmpty()) cambiarBorrador(b.dropLast(1))
        if (b.length <= 1) actualizar { it.copy(mayus = true) }
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
            Panel.IMAGENES -> {
                val total = e.candidatos.size
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
            Panel.WIDGETS -> {
                val total = e.widgets.size
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
            Panel.WIDGETS -> alternarWidget(e.indice)
            Panel.BUSCAR -> e.visibles.getOrNull(e.indice)?.let { j ->
                actualizar { it.copy(juegoSel = j.clave, panel = Panel.NINGUNO) }
            }
            Panel.CARPETAS -> aceptarCarpetas()
            Panel.IMAGENES -> usarCandidato()
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
    fun opcionesRapido(): List<Opcion> {
        val e = estado.value
        val juego = e.seleccionado ?: return emptyList()
        val d = e.datos[juego.clave]
        val consola = Consolas.porId(juego.consolaId)
        val lista = mutableListOf<Opcion>()
        lista += Opcion("Jugar", icono = Icono.JUGAR) { cerrarPanel(); jugar() }
        lista += Opcion("Personalizar", "Título, imágenes, música", Icono.PALETA) { abrirFicha(); elegirPestanaFicha(3) }
        lista += Opcion("Ficha", "Historia y guías", Icono.TUTORIAL) { abrirFicha() }
        lista += Opcion("Mi próximo paso", d?.paso?.take(30) ?: "", Icono.LAPIZ) { abrirPaso() }
        lista += Opcion(if (d?.favorito == true) "Favorito ✓" else "Favorito", icono = Icono.CORAZON) { alternarFavorito(juego.clave) }
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
        }
        lista += Opcion("Imágenes", if (juego.portada != null) "Buscar de nuevo" else "Buscar en internet", Icono.DESCARGA) {
            cerrarPanel(); descargarUno(juego)
        }
        if (consola != null) {
            lista += Opcion("Cajas de ${consola.nombre}", nombreForma(e.formas[consola.id]?.forma ?: "original"), Icono.FORMA) {
                ciclarForma(consola.id)
            }
        }
        return lista
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

    fun opcionesSeccion(s: Seccion): List<Opcion> {
        val e = estado.value
        val lista = mutableListOf<Opcion>()
        when (s) {
            Seccion.ESTANTE -> lista += opcionesEstante(e)
            Seccion.CONSOLA -> lista += opcionesConsola(e)
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
            Seccion.ASISTENTE -> {
                lista += Opcion("Abrir el asistente", icono = Icono.ASISTENTE, ayuda = "Chatea con la IA: te recomienda juegos de tu biblioteca, te da trucos y puede cambiar el tema o la vista por ti.") { abrirAsistente() }
                lista += Opcion(
                    "Clave de IA (Claude)", if (e.claveIa.isBlank()) "Pegar" else "Guardada ✓", Icono.CLAVE,
                    "El asistente y los temas con IA usan tu clave de console.anthropic.com. Corre en la nube: no ocupa memoria ni hace más lenta tu consola.",
                ) { abrirTexto("ia_clave", estado.value.claveIa) }
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
                // Todas las pistas a la vista: elegir una la pone a sonar.
                val sonando = if (e.musicaTema && e.pista != "mia") e.pistaTema else e.pista
                for (p in Musica.pistas) {
                    lista += Opcion(
                        "♪ ${p.nombre}", if (e.musica && sonando == p.id) "Sonando ✓" else p.estilo, Icono.MUSICA,
                        "${p.estilo}. Elegirla apaga \"Música según el tema\".",
                    ) { fijarPista(p.id) }
                }
                lista += Opcion("♪ Mezcla", if (e.musica && sonando == "mezcla") "Sonando ✓" else "Todas, una tras otra", Icono.ESTRELLA) { fijarPista("mezcla") }
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
                    "Reconocer nombres con IA", if (e.claveIa.isBlank()) "Necesita clave" else "Para los que faltan", Icono.CHISPA,
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
                    o.optInt("pagina", 0), o.optString("uri", "").ifEmpty { null },
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
                .put("visible", it.visible).put("pagina", it.pagina)
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
        val clave = estado.value.claveIa
        if (clave.isBlank()) {
            abrirTexto("ia_clave")
            return
        }
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
            "Crear tema con IA", if (e.claveIa.isBlank()) "Necesita una clave" else "Escribe tu idea", Icono.CHISPA,
            "Escribe algo como \"Halloween japonés\" o \"Navidad en la playa\", o déjalo vacío y la IA elige la fiesta del mundo más cercana a hoy.",
        ) { if (estado.value.claveIa.isBlank()) abrirTexto("ia_clave") else abrirTexto("ia_pedido") }
        for (t in todosLosTemas(e)) {
            val enUso = cl.nikalbazu.diptico.ui.Estilo.tema.id == t.id
            lista += Opcion(
                t.nombre, if (enUso) "En uso ✓" else if (t.propio) "Creado por ti" else "", Icono.PALETA,
                if (t.decor != "nada") "Con ${Temas.nombreDecor(t.decor).lowercase()}." else "",
            ) { elegirTema(t.id) }
        }
        val fiestaIdx = Temas.fiestas.indexOfFirst { it.tema.id == e.tema }
        lista += Opcion(
            "Probar temas de fiestas", if (fiestaIdx >= 0) Temas.fiestas[fiestaIdx].nombre else "Elegir", Icono.MUNDO,
            "Recorre los temas de las fiestas del mundo cuando quieras.",
        ) { elegirTema(Temas.fiestas[(fiestaIdx + 1).mod(Temas.fiestas.size)].tema.id) }
        val mio = e.temasPropios.firstOrNull { it.id == "mio" }
        lista += Opcion("Mi tema · color principal", "Cambiar", Icono.PALETA, "Crea tu propio tema eligiendo sus colores.") {
            editarMiTema { it.copy(acento = siguienteColor(it.acento)) }
        }
        lista += Opcion("Mi tema · color 2", "Cambiar", Icono.PALETA) { editarMiTema { it.copy(acento2 = siguienteColor(it.acento2)) } }
        lista += Opcion("Mi tema · color 3", "Cambiar", Icono.PALETA) { editarMiTema { it.copy(acento3 = siguienteColor(it.acento3)) } }
        lista += Opcion("Mi tema · fondo", when (mio?.base) { "oscuro" -> "Oscuro"; "claro" -> "Claro"; "oled" -> "Negro puro"; else -> "Según el modo" }, Icono.TEMA) {
            editarMiTema { it.copy(base = when (it.base) { "auto" -> "oscuro"; "oscuro" -> "oled"; "oled" -> "claro"; else -> "auto" }) }
        }
        lista += Opcion("Mi tema · decoración", Temas.nombreDecor(mio?.decor ?: "nada"), Icono.ESTRELLA) {
            editarMiTema { t -> t.copy(decor = Temas.DECORACIONES[(Temas.DECORACIONES.indexOf(t.decor) + 1).mod(Temas.DECORACIONES.size)]) }
        }
        lista += Opcion(
            "Clave de IA (Claude)", if (e.claveIa.isNotBlank()) "Conectada ✓" else "Opcional", Icono.CLAVE,
            "Para crear temas con IA. Se saca en console.anthropic.com › API Keys. Cada tema cuesta muy poquito.",
        ) { abrirTexto("ia_clave", estado.value.claveIa) }
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
            "Estilo del inicio", if (e.inicio == "widgets") "Con widgets" else "Carátulas", Icono.APPS,
            "Carátulas: el arte del juego ocupa la pantalla. Con widgets: tu inicio con reloj, música, recientes y más. " +
                "Se cambia rápido con Select.",
        ) { alternarInicio() },
        Opcion(
            "Acomodar widgets", "${e.widgets.count { it.visible }} a la vista", Icono.LAPIZ,
            "Mueve los widgets con la cruceta o el dedo, cambia su tamaño con L1 y R1 (10 tamaños y formas).",
        ) { editarWidgets() },
        Opcion("Agregar o quitar widgets", icono = Icono.APPS, ayuda = "Elige qué widgets se ven en tu inicio.") {
            actualizar { it.copy(panel = Panel.WIDGETS, indice = 0) }
        },
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
        actualizar { it.copy(inicio = "widgets", editandoWidgets = true, widgetSel = primero, panel = Panel.NINGUNO) }
        avisar("Cruceta mueve · L1 R1 tamaño · L2 R2 otro widget · Y agregar · B listo")
    }

    fun terminarEdicion() {
        almacen.guardarTexto("widgets", escribirWidgets(estado.value.widgets))
        actualizar { it.copy(editandoWidgets = false) }
        avisar("Inicio guardado")
    }

    private fun cambiarWidget(i: Int, f: (Widget) -> Widget) {
        val l = estado.value.widgets.toMutableList()
        val w = l.getOrNull(i) ?: return
        l[i] = f(w)
        actualizar { it.copy(widgets = l) }
    }

    private fun encajar(w: Widget): Widget {
        val an = w.an.coerceIn(1, Widgets.COLUMNAS)
        val al = w.al.coerceIn(1, Widgets.FILAS)
        return w.copy(an = an, al = al, x = w.x.coerceIn(0, Widgets.COLUMNAS - an), y = w.y.coerceIn(0, Widgets.FILAS - al))
    }

    fun moverWidgetSel(dx: Int, dy: Int) {
        val e = estado.value
        cambiarWidget(e.widgetSel) { w ->
            val nx = w.x + dx
            when {
                // Al salir por un costado pasa a la página de al lado.
                nx < 0 && w.pagina > 0 -> encajar(w.copy(pagina = w.pagina - 1, x = Widgets.COLUMNAS - w.an))
                nx + w.an > Widgets.COLUMNAS && w.pagina < e.paginas - 1 -> encajar(w.copy(pagina = w.pagina + 1, x = 0))
                else -> encajar(w.copy(x = nx, y = w.y + dy))
            }
        }
        estado.value.widgets.getOrNull(e.widgetSel)?.let { w -> if (w.pagina != e.paginaInicio) actualizar { it.copy(paginaInicio = w.pagina) } }
    }

    /** Para arrastrar con el dedo: deja el widget en la celda indicada. */
    fun ponerWidget(i: Int, x: Int, y: Int) = cambiarWidget(i) { encajar(it.copy(x = x, y = y)) }

    fun tamanoWidgetSel(delta: Int) = cambiarWidget(estado.value.widgetSel) { w ->
        val t = Widgets.TAMANOS
        val i = t.indexOfFirst { it.first == w.an && it.second == w.al }
        val (an, al) = t[(i + delta).mod(t.size)]
        encajar(w.copy(an = an, al = al))
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
            var lugar = 0 to 0
            buscar@ for (y in 0..Widgets.FILAS - w.al) for (x in 0..Widgets.COLUMNAS - w.an) {
                val libre = (x until x + w.an).all { cx -> (y until y + w.al).all { cy -> (cx to cy) !in ocupadas } }
                if (libre) { lugar = x to y; break@buscar }
            }
            cambiarWidget(i) { it.copy(visible = true, x = lugar.first, y = lugar.second, pagina = estado.value.paginaInicio) }
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
        if (estado.value.claveIa.isBlank()) { claveParaAsistente = true; abrirTexto("ia_clave"); avisar("El asistente usa tu clave de IA (Claude). Pégala primero."); return }
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

    fun preguntar(texto: String) {
        val e = estado.value
        if (e.pensando) return
        if (e.claveIa.isBlank()) { claveParaAsistente = true; abrirTexto("ia_clave"); avisar("El asistente usa tu clave de IA (Claude). Pégala primero."); return }
        val chat = e.chat + MensajeChat("yo", texto)
        actualizar { it.copy(chat = chat, pensando = true, indice = 0) }
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                try {
                    val sistema = Asistente.sistema(estado.value)
                    val historia = chat.takeLast(12).map { (if (it.rol == "yo") "user" to it.texto else "assistant" to it.texto) }
                    Result.success(IA.conversar(estado.value.claveIa, sistema, historia, 700))
                } catch (ex: Exception) {
                    Result.failure(ex)
                }
            }
            val msg = r.fold({ Asistente.interpretar(it) }, { MensajeChat("ia", "⚠ " + (it.message ?: "No pude hablar con la IA.")) })
            actualizar { it.copy(chat = it.chat + msg, pensando = false, indice = if (msg.acciones.isNotEmpty()) 1 else 0) }
        }
    }

    /** Hace lo que propuso el asistente. */
    fun ejecutarAccion(a: AccionChat) {
        when (a.tipo) {
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
                Opcion("Portada", if (d.portada != null) "Propia ✓" else "Elegir imagen", Icono.PORTADA) { pedirImagen("portada") },
                Opcion("Imagen grande (arriba)", if (d.arte != null) "Propia ✓" else "Elegir imagen", Icono.PORTADA) { pedirImagen("arte") },
                Opcion("Logo", if (d.logo != null) "Propio ✓" else "Elegir imagen", Icono.ESTRELLA) { pedirImagen("logo") },
                Opcion("Música del juego", if (d.musica != null) "Elegida ✓" else "Elegir canción", Icono.MUSICA) { pedirArchivoPara("musica", "audio/*") },
                Opcion(if (d.favorito) "Quitar de favoritos" else "Marcar como favorito", icono = Icono.CORAZON) { alternarFavorito(j.clave) },
                Opcion("Elegir portada de internet", "Varias opciones", Icono.DESCARGA) { buscarCandidatos("portada") },
                Opcion("Elegir imagen grande (hero)", "Varias opciones", Icono.DESCARGA) { buscarCandidatos("arte") },
                Opcion("Elegir logo de internet", "Varias opciones", Icono.DESCARGA) { buscarCandidatos("logo") },
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
    fun buscarCandidatos(tipo: String) {
        val e = estado.value
        val j = juegoFicha() ?: return
        val crudo = todosCrudos().firstOrNull { it.clave == j.clave } ?: return
        val c = Consolas.porId(crudo.consolaId) ?: return
        actualizar { it.copy(panel = Panel.IMAGENES, candidatos = emptyList(), candidatoPara = tipo, buscandoCandidatos = true, indice = 0) }
        scope.launch {
            val lista = withContext(Dispatchers.IO) {
                try {
                    val wiki = e.ficha ?: Fichas.buscar(app, j.titulo, c.bonito)
                    Descargas.candidatos(app, crudo, c, tipo, j.titulo, e.claveSgdb, wiki)
                } catch (ex: Exception) {
                    emptyList()
                }
            }
            if (estado.value.panel == Panel.IMAGENES) actualizar { it.copy(candidatos = lista, buscandoCandidatos = false) }
        }
    }

    /** Baja la opción elegida y la deja como imagen propia del juego. */
    fun usarCandidato(i: Int = estado.value.indice) {
        val e = estado.value
        val img = e.candidatos.getOrNull(i) ?: return
        val j = juegoFicha() ?: return
        val tipo = e.candidatoPara
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
            actualizar { it.copy(panel = Panel.FICHA) }
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

    fun alternarFavorito(clave: String) {
        cambiarDatos(clave) { it.copy(favorito = !it.favorito) }
        avisar(if (estado.value.datos[clave]?.favorito == true) "Agregado a favoritos" else "Quitado de favoritos")
    }

    private fun abrirWeb(url: String) {
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
            "asistente" -> {
                actualizar { it.copy(panel = Panel.ASISTENTE, indice = 0) }
                if (t.isNotEmpty()) preguntar(t)
                return
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
            "ia_clave" -> {
                almacen.guardarTexto("claveIa", t)
                actualizar { it.copy(claveIa = t) }
                if (t.isNotEmpty() && claveParaAsistente) {
                    claveParaAsistente = false
                    abrirTexto("asistente")
                    avisar("Clave guardada. Ahora escribe tu pregunta.")
                    return
                }
                if (t.isNotEmpty() && estado.value.seccion != Seccion.ASISTENTE) {
                    abrirTexto("ia_pedido")
                    avisar("Clave guardada. Ahora escribe tu idea (o déjalo vacío).")
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

    private val VISTAS = listOf("estante", "cuadricula", "lista")
    fun nombreVista(v: String) = when (v) { "cuadricula" -> "Cuadrícula"; "lista" -> "Lista"; else -> "Estante" }

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

    val FILTROS = listOf("todos", "favoritos", "recientes", "mas", "sinjugar")
    fun nombreFiltro(f: String) = when (f) {
        "favoritos" -> "Favoritos"; "recientes" -> "Jugados hace poco"; "mas" -> "Más jugados"; "sinjugar" -> "Sin jugar"; else -> "Todos"
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
            Opcion("Vista", nombreVista(e.vista), Icono.VISTA, "Estante con cajas, cuadrícula de carátulas o lista. Se cambia rápido con R3 (apretar el stick derecho).") { cambiarVista() },
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
        Opcion("Archivos", icono = Icono.CARPETA) { abrirArchivos() },
        Opcion("Batería", icono = Icono.BATERIA) { abrirIntent(Intent(Intent.ACTION_POWER_USAGE_SUMMARY), "No pude abrir la batería") },
        Opcion("Asistente", icono = Icono.ASISTENTE) { abrirAsistente() },
        Opcion("Estadísticas", icono = Icono.RELOJ) { abrirEstadisticas() },
        Opcion("Protector", icono = Icono.ESTRELLA) { cerrarPanel(); actualizar { it.copy(protector = true) } },
        Opcion("Ajustes", icono = Icono.AJUSTES) { abrirIntent(Intent(android.provider.Settings.ACTION_SETTINGS), "No pude abrir los ajustes") },
    )

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
            "Díptico como launcher principal", icono = Icono.ESTRELLA,
            ayuda = "Abre los ajustes de Android para elegir Díptico como pantalla de inicio. Así el botón Inicio te trae aquí.",
        ) { abrirIntent(Intent(android.provider.Settings.ACTION_HOME_SETTINGS), "No pude abrir los ajustes de inicio") },
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
        val clave = estado.value.claveIa
        if (clave.isBlank()) {
            abrirTexto("ia_clave")
            avisar("Para reconocer nombres con IA necesito tu clave de Claude")
            return
        }
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
        // Los widgets que quedaron fuera se acomodan dentro.
        val l = estado.value.widgets.map { encajar(it.copy(pagina = it.pagina.coerceIn(0, paginas - 1))) }
        almacen.guardarTexto("widgets", escribirWidgets(l))
        actualizar { it.copy(gridCols = cols, gridFilas = filas, paginas = paginas, widgets = l, paginaInicio = it.paginaInicio.coerceIn(0, paginas - 1)) }
    }

    /** L2/R2 o deslizar: otra página del inicio. */
    fun cambiarPagina(delta: Int) {
        val e = estado.value
        val p = (e.paginaInicio + delta).coerceIn(0, e.paginas - 1)
        if (p != e.paginaInicio) actualizar { it.copy(paginaInicio = p) }
    }

    fun irAPagina(p: Int) = actualizar { it.copy(paginaInicio = p.coerceIn(0, it.paginas - 1)) }

    private fun opcionesGrilla(e: Estado): List<Opcion> = listOf(
        Opcion("Columnas de la grilla", "${e.gridCols}", Icono.APPS, "Cuántas columnas tiene tu inicio (de 4 a 8).") {
            val v = if (e.gridCols >= 8) 4 else e.gridCols + 1
            guardarGrilla(v, e.gridFilas, e.paginas)
        },
        Opcion("Filas de la grilla", "${e.gridFilas}", Icono.APPS, "Con más de 4 filas, el inicio se desplaza hacia abajo.") {
            val v = if (e.gridFilas >= 8) 3 else e.gridFilas + 1
            guardarGrilla(e.gridCols, v, e.paginas)
        },
        Opcion("Páginas", "${e.paginas}", Icono.VISTA, "Desliza a los lados con el dedo o con L2 y R2 para cambiar de página.") {
            val v = if (e.paginas >= 5) 1 else e.paginas + 1
            guardarGrilla(e.gridCols, e.gridFilas, v)
        },
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

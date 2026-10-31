package cl.nikalbazu.diptico.estado

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.Display
import cl.nikalbazu.diptico.audio.Musica
import cl.nikalbazu.diptico.data.Almacen
import cl.nikalbazu.diptico.data.Biblioteca
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.data.Descargas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.data.MediosJuego
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

enum class Panel { NINGUNO, RAPIDO, PASO, MENU, CLAVE }

/** Carpetas que Díptico puede pedir. */
enum class Carpeta { JUEGOS, MEDIA, MUSICA }

/** Pasos del tutorial guiado del primer arranque. */
enum class PasoTuto { BIENVENIDA, PANTALLAS, TEXTO, DISPOSICION, JUEGOS, CARPETA, EMULADORES, PORTADAS, MUSICA, APARIENCIA, CONTROLES, LISTO }

/** Íconos propios de Díptico (se dibujan en ui/Iconos.kt). */
enum class Icono {
    PANTALLAS, UNA, NEGRO, LIBRE, INTERCAMBIAR, TEXTO, TEMA, INTRO, MUSICA, VOLUMEN, SILENCIO, CARPETA, EMULADOR,
    PORTADA, DESCARGA, CLAVE, TITULO, BORRAR, BIBLIOTECA, BUSCAR, TUTORIAL, INFO, CERRAR, JUGAR, DADO, LAPIZ,
    CONTINUAR, MENU, CHECK, ARRIBA, ABAJO, ESTRELLA,
}

/** Secciones del menú principal. */
enum class Seccion(val titulo: String, val icono: Icono, val ayuda: String) {
    PANTALLAS("Pantallas", Icono.PANTALLAS, "Cuántas pantallas usa Díptico, qué va en cada una y el tamaño de la letra."),
    APARIENCIA("Apariencia", Icono.TEMA, "Los colores de Díptico y su animación de entrada."),
    MUSICA("Música", Icono.MUSICA, "La música de fondo. Se calla sola al abrir un juego o si pones Spotify o YouTube Music."),
    EMULADORES("Emuladores", Icono.EMULADOR, "Con qué emulador, o con qué núcleo de RetroArch, se abre cada consola."),
    PORTADAS("Portadas", Icono.PORTADA, "Descarga portadas, imágenes, logos y títulos de internet."),
    BIBLIOTECA("Biblioteca", Icono.BIBLIOTECA, "Tu carpeta de juegos y las imágenes que ya tengas de ES-DE."),
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
    val accion: () -> Unit,
)

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
) {
    /** Todo Díptico vive en una sola pantalla (siempre durante el tutorial). */
    val unaPantalla: Boolean
        get() = !configurado || modoPantallas != "dos" || !dosDisponibles

    val consolasConJuegos: List<Consola>
        get() = Consolas.todas.filter { c -> juegos.any { it.consolaId == c.id } }

    val pestanas: List<String>
        get() = listOf("todo") + consolasConJuegos.map { it.id }

    val visibles: List<Juego>
        get() = if (consolaSel == "todo") juegos else juegos.filter { it.consolaId == consolaSel }

    val seleccionado: Juego?
        get() {
            val v = visibles
            return v.firstOrNull { it.clave == juegoSel } ?: v.firstOrNull()
        }

    val indiceSel: Int
        get() {
            val clave = seleccionado?.clave ?: return 0
            return visibles.indexOfFirst { it.clave == clave }.coerceAtLeast(0)
        }

    val sinPortada: Int
        get() = juegos.count { it.portada == null }
}

/** Estado compartido por las dos pantallas y todas las acciones de Díptico. */
object Diptico {
    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado

    private lateinit var app: Context
    private lateinit var almacen: Almacen
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var avisoJob: Job? = null
    private var despiertaJob: Job? = null
    private var descargaJob: Job? = null
    private var introInicio = 0L
    private var crudos: List<Juego> = emptyList()
    private var medios: Map<String, MediosJuego> = emptyMap()

    const val VERSION = "0.2"

    /** La pantalla activa registra aquí cómo pedir una carpeta. */
    var pedirCarpeta: ((Carpeta) -> Unit)? = null

    fun iniciar(ctx: Context) {
        if (::app.isInitialized) return
        app = ctx.applicationContext
        almacen = Almacen(app)
        Musica.iniciar(app)
        val carpeta = almacen.carpeta
        medios = almacen.medios()
        crudos = almacen.biblioteca()
        _estado.value = Estado(
            listo = true,
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
        )
        if (carpeta != null) escanear()
        // La música sigue al estado: suena con Díptico a la vista y sin juego abierto.
        scope.launch {
            estado.map { e ->
                listOf(e.listo && e.musica && e.aLaVista > 0 && e.sesion == null, e.pista, e.volumen, e.carpetaMusica)
            }.distinctUntilChanged().collect { (sonar, pista, vol, carpetaM) ->
                Musica.sincronizar(sonar as Boolean, pista as String, vol as Float, carpetaM as Uri?)
            }
        }
    }

    /** Solo para las capturas automáticas: fija un estado de ejemplo. */
    fun fijarParaPrueba(e: Estado) {
        _estado.value = e
    }

    private inline fun actualizar(f: (Estado) -> Estado) {
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
        val carpeta = almacen.carpeta ?: return
        actualizar { it.copy(escaneando = true) }
        scope.launch {
            val lista = try {
                withContext(Dispatchers.IO) { Biblioteca.escanear(app, carpeta, almacen.media) }
            } catch (e: Exception) {
                avisar("No pude leer la carpeta de juegos. Elígela otra vez.")
                null
            }
            if (lista != null) {
                crudos = lista
                withContext(Dispatchers.IO) { almacen.guardarBiblioteca(lista) }
                publicarJuegos()
                actualizar { it.copy(escaneando = false, indice = if (it.configurado) it.indice else it.indice.coerceAtMost(1)) }
            } else {
                actualizar { it.copy(escaneando = false) }
            }
        }
    }

    /** Junta lo escaneado con lo descargado y lo muestra. */
    private fun publicarJuegos() {
        val lista = Descargas.aplicar(crudos, medios, estado.value.titulosBonitos)
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
                almacen.carpeta = uri
                actualizar { it.copy(tieneCarpeta = true, indice = 0) }
                escanear()
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
        actualizar { it.copy(pista = p, musica = true) }
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
            PasoTuto.BIENVENIDA -> listOf(Opcion("¡Comencemos!", "Toma unos 2 minutos", Icono.JUGAR) { siguientePaso() })
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
            PasoTuto.EMULADORES -> listOf(Opcion("Continuar", icono = Icono.CHECK) { siguientePaso() }) + e.consolasConJuegos.map { c ->
                Opcion(
                    c.nombreLargo.lowercase().replaceFirstChar { it.uppercase() },
                    if (emuladorListo(c)) nombreEmulador(c) else "Falta: ${c.emuladores.first().nombre}",
                    Icono.EMULADOR,
                ) { ciclarEmulador(c) }
            }
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
            PasoTuto.MUSICA -> Musica.pistas.map { p ->
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
        val error = Lanzador.lanzar(app, juego, consola, e.emuladores[consola.id], displayId)
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
        actualizar { it.copy(panel = Panel.PASO, borrador = it.datos[clave]?.paso ?: "", pregunta = false) }
    }

    fun abrirMenu(seccion: Seccion? = null) {
        actualizar {
            it.copy(
                panel = Panel.MENU, seccion = seccion ?: it.seccion,
                enOpciones = seccion != null, indice = 0,
            )
        }
    }

    fun abrirClave() {
        actualizar { it.copy(panel = Panel.CLAVE, borrador = it.claveSgdb) }
    }

    fun cerrarPanel() {
        val e = estado.value
        // Desde la clave, se vuelve a donde estabas (menú o tutorial).
        if (e.panel == Panel.CLAVE && e.configurado) {
            actualizar { it.copy(panel = Panel.MENU, seccion = Seccion.PORTADAS, enOpciones = true) }
            return
        }
        actualizar { it.copy(panel = Panel.NINGUNO, pregunta = false) }
    }

    fun cambiarBorrador(texto: String) {
        val max = if (estado.value.panel == Panel.CLAVE) 80 else 160
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
                val n = (e.seccion.ordinal + delta + s.size) % s.size
                actualizar { it.copy(seccion = s[n], indice = 0) }
            } else {
                val total = opcionesSeccion(e.seccion).size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta + total) % total) }
            }
            Panel.RAPIDO -> {
                val total = opcionesRapido().size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta + total) % total) }
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

    fun opcionesRapido(): List<Opcion> {
        val e = estado.value
        val juego = e.seleccionado ?: return emptyList()
        val recordada = e.datos[juego.clave]?.pantalla
        val lista = mutableListOf<Opcion>()
        if (!e.unaPantalla) {
            lista += Opcion("Abrir en la pantalla de arriba", icono = Icono.ARRIBA) { cerrarPanel(); jugar("arriba") }
            lista += Opcion("Abrir en la pantalla de abajo", icono = Icono.ABAJO) { cerrarPanel(); jugar("abajo") }
            lista += Opcion(
                "Este juego abre siempre",
                when (recordada) { "arriba" -> "Arriba"; "abajo" -> "Abajo"; else -> "Según el menú" },
                Icono.PANTALLAS,
            ) { ciclarPantallaJuego(juego.clave) }
        } else {
            lista += Opcion("Jugar", icono = Icono.JUGAR) { cerrarPanel(); jugar() }
        }
        lista += Opcion("Anotar mi próximo paso", icono = Icono.LAPIZ) { abrirPaso() }
        lista += Opcion("Buscar portada de este juego", if (juego.portada != null) "Ya tiene" else "En internet", Icono.DESCARGA) {
            cerrarPanel(); descargarUno(juego)
        }
        return lista
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
            Seccion.PANTALLAS -> {
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
            Seccion.APARIENCIA -> {
                lista += Opcion("Tema", if (e.oscuro) "Oscuro" else "Claro", Icono.TEMA, "El oscuro ahorra batería en pantallas OLED.") { alternarTema() }
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
                    "Pista", nombrePista(e.pista), Icono.ESTRELLA,
                    "Bisagra es lofi tranquilo, Cartucho es arcade alegre y Medianoche es ambiente suave.",
                ) {
                    val orden = ordenPistas + if (estado.value.carpetaMusica != null) listOf("mia") else emptyList()
                    val i = orden.indexOf(estado.value.pista)
                    fijarPista(orden[(i + 1) % orden.size])
                }
                lista += Opcion("Volumen", nombreVolumen(e.volumen), Icono.VOLUMEN, "Qué tan fuerte suena la música de Díptico.") {
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
                val consolas = e.consolasConJuegos.ifEmpty { Consolas.todas }
                for (c in consolas) {
                    lista += Opcion(
                        c.nombreLargo.lowercase().replaceFirstChar { it.uppercase() },
                        if (emuladorListo(c)) nombreEmulador(c) else "Falta: ${c.emuladores.first().nombre}",
                        Icono.EMULADOR,
                        "Aprieta A para pasar al siguiente emulador instalado. Con RetroArch puedes elegir el núcleo; " +
                            "el núcleo debe estar descargado dentro de RetroArch.",
                    ) { ciclarEmulador(c) }
                }
            }
            Seccion.PORTADAS -> {
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
                    "Cambiar carpeta de juegos", icono = Icono.CARPETA,
                    ayuda = "La carpeta que tiene una carpeta por consola: snes, psx, nds, ps2…",
                ) { pedirCarpeta?.invoke(Carpeta.JUEGOS) }
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
        Lanzador.elegida(app, c, estado.value.emuladores[c.id])?.emulador?.nombre ?: "Elegir al abrir"

    fun emuladorListo(c: Consola): Boolean = Lanzador.elegida(app, c, estado.value.emuladores[c.id]) != null

    private fun ciclarEmulador(c: Consola) {
        val ops = Lanzador.opciones(app, c).distinctBy { it.id }
        if (ops.isEmpty()) {
            avisar("No hay emuladores de ${c.nombre} instalados. Te sugiero ${c.emuladores.first().nombre}.")
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
        val pendientes = crudos.filter { j ->
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
                avisar("No tengo internet. Conéctate a una red Wi-Fi y prueba de nuevo.")
                return@launch
            }
            var encontrados = 0
            val clave = estado.value.claveSgdb
            pendientes.forEachIndexed { i, j ->
                actualizar { it.copy(descarga = Descarga(i, pendientes.size, j.titulo, encontrados)) }
                val c = Consolas.porId(j.consolaId) ?: return@forEachIndexed
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
            avisar("Listo: encontré $encontrados de ${pendientes.size} juegos")
        }
    }

    private fun cancelarDescarga() {
        descargaJob?.cancel()
        descargaJob = null
        actualizar { it.copy(descarga = null) }
        almacen.guardarMedios(medios)
        publicarJuegos()
    }

    private fun descargarUno(j: Juego) {
        val crudo = crudos.firstOrNull { it.clave == j.clave } ?: return
        val c = Consolas.porId(crudo.consolaId) ?: return
        avisar("Buscando ${j.titulo}…")
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                try { Descargas.buscar(app, crudo, c, null, estado.value.claveSgdb) } catch (ex: Exception) { null }
            }
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
}

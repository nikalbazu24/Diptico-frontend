package cl.nikalbazu.diptico.estado

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Display
import cl.nikalbazu.diptico.data.Almacen
import cl.nikalbazu.diptico.data.Biblioteca
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.emu.Lanzador
import cl.nikalbazu.diptico.emu.Pantallas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Panel { NINGUNO, MENU, PASO, AJUSTES }

/** Un juego abierto ahora mismo. */
data class Sesion(val clave: String, val inicio: Long, val displayId: Int, val dosPantallas: Boolean)

data class Opcion(val titulo: String, val valor: String = "", val accion: () -> Unit)

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
    val sesion: Sesion? = null,
    val despierta: Boolean = false,
    val panel: Panel = Panel.NINGUNO,
    val indice: Int = 0,
    val borrador: String = "",
    val pregunta: Boolean = false,
    val aviso: String? = null,
    val datos: Map<String, DatosJuego> = emptyMap(),
    val emuladores: Map<String, String> = emptyMap(),
) {
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

    /** La pantalla activa registra aquí cómo pedir una carpeta (true = juegos, false = imágenes ES-DE). */
    var pedirCarpeta: ((Boolean) -> Unit)? = null

    fun iniciar(ctx: Context) {
        if (::app.isInitialized) return
        app = ctx.applicationContext
        almacen = Almacen(app)
        val carpeta = almacen.carpeta
        _estado.value = Estado(
            listo = true,
            configurado = almacen.configurado && carpeta != null,
            tieneCarpeta = carpeta != null,
            tieneMedia = almacen.media != null,
            juegos = almacen.biblioteca(),
            oscuro = almacen.oscuro,
            intercambiar = almacen.intercambiar,
            pantallaJuegos = almacen.pantallaJuegos,
            datos = almacen.datos(),
            emuladores = almacen.emuladores(),
        )
        if (carpeta != null) escanear()
    }

    private inline fun actualizar(f: (Estado) -> Estado) {
        _estado.value = f(_estado.value)
    }

    // ---------- Carpeta y biblioteca ----------

    fun escanear() {
        val carpeta = almacen.carpeta ?: return
        actualizar { it.copy(escaneando = true) }
        scope.launch {
            val lista = try {
                withContext(Dispatchers.IO) { Biblioteca.escanear(app, carpeta, almacen.media) }
            } catch (e: Exception) {
                avisar("No pude leer la carpeta de juegos. Elígela otra vez en Ajustes.")
                null
            }
            if (lista != null) {
                withContext(Dispatchers.IO) { almacen.guardarBiblioteca(lista) }
                actualizar { e ->
                    val sigue = lista.any { it.clave == e.juegoSel }
                    val consolaSigue = e.consolaSel == "todo" || lista.any { it.consolaId == e.consolaSel }
                    e.copy(
                        juegos = lista,
                        escaneando = false,
                        juegoSel = if (sigue) e.juegoSel else null,
                        consolaSel = if (consolaSigue) e.consolaSel else "todo",
                    )
                }
            } else {
                actualizar { it.copy(escaneando = false) }
            }
        }
    }

    fun carpetaElegida(uri: Uri, esDeJuegos: Boolean) {
        val cr = app.contentResolver
        try {
            cr.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } catch (e: SecurityException) {
            try {
                cr.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
            }
        }
        if (esDeJuegos) {
            almacen.carpeta = uri
            actualizar { it.copy(tieneCarpeta = true) }
        } else {
            almacen.media = uri
            actualizar { it.copy(tieneMedia = true) }
            avisar("Imágenes de ES-DE conectadas")
        }
        escanear()
    }

    fun accionConfiguracion() {
        val e = estado.value
        when {
            !e.tieneCarpeta -> pedirCarpeta?.invoke(true)
            e.escaneando -> Unit
            e.juegos.isEmpty() -> pedirCarpeta?.invoke(true)
            else -> terminarConfiguracion()
        }
    }

    fun terminarConfiguracion() {
        almacen.configurado = true
        actualizar { it.copy(configurado = true) }
    }

    fun avisar(texto: String) {
        actualizar { it.copy(aviso = texto) }
        avisoJob?.cancel()
        avisoJob = scope.launch {
            delay(3500)
            actualizar { it.copy(aviso = null) }
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

    fun alternarTema() {
        val v = !estado.value.oscuro
        almacen.oscuro = v
        actualizar { it.copy(oscuro = v) }
    }

    // ---------- Jugar ----------

    fun jugar(pantalla: String? = null) {
        val e = estado.value
        val juego = e.seleccionado ?: return
        val consola = Consolas.porId(juego.consolaId) ?: return
        val destino = pantalla ?: e.datos[juego.clave]?.pantalla ?: e.pantallaJuegos
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

    fun abrirMenu() {
        if (estado.value.seleccionado == null) return
        actualizar { it.copy(panel = Panel.MENU, indice = 0) }
    }

    fun abrirPaso() {
        val clave = estado.value.seleccionado?.clave ?: return
        actualizar { it.copy(panel = Panel.PASO, borrador = it.datos[clave]?.paso ?: "", pregunta = false) }
    }

    fun abrirAjustes() {
        actualizar { it.copy(panel = Panel.AJUSTES, indice = 0) }
    }

    fun cerrarPanel() {
        actualizar { it.copy(panel = Panel.NINGUNO, pregunta = false) }
    }

    fun cambiarBorrador(texto: String) {
        actualizar { it.copy(borrador = texto.take(160)) }
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

    fun moverIndice(delta: Int) {
        val total = when (estado.value.panel) {
            Panel.MENU -> opcionesMenu().size
            Panel.AJUSTES -> opcionesAjustes().size
            else -> 0
        }
        if (total == 0) return
        actualizar { it.copy(indice = (it.indice + delta + total) % total) }
    }

    fun elegirIndice(i: Int) {
        actualizar { it.copy(indice = i) }
        aceptar()
    }

    fun aceptar() {
        val e = estado.value
        val lista = when (e.panel) {
            Panel.MENU -> opcionesMenu()
            Panel.AJUSTES -> opcionesAjustes()
            else -> return
        }
        lista.getOrNull(e.indice)?.accion?.invoke()
    }

    fun opcionesMenu(): List<Opcion> {
        val e = estado.value
        val juego = e.seleccionado ?: return emptyList()
        val recordada = e.datos[juego.clave]?.pantalla
        return listOf(
            Opcion("Abrir en la pantalla de arriba") { cerrarPanel(); jugar("arriba") },
            Opcion("Abrir en la pantalla de abajo") { cerrarPanel(); jugar("abajo") },
            Opcion(
                "Este juego abre siempre",
                when (recordada) { "arriba" -> "Arriba"; "abajo" -> "Abajo"; else -> "Según ajustes" },
            ) { ciclarPantallaJuego(juego.clave) },
            Opcion("Anotar mi próximo paso") { abrirPaso() },
        )
    }

    private fun ciclarPantallaJuego(clave: String) {
        val datos = estado.value.datos.toMutableMap()
        val d = datos[clave] ?: DatosJuego()
        val siguiente = when (d.pantalla) { null -> "arriba"; "arriba" -> "abajo"; else -> null }
        datos[clave] = d.copy(pantalla = siguiente)
        almacen.guardarDatos(datos)
        actualizar { it.copy(datos = datos) }
    }

    fun opcionesAjustes(): List<Opcion> {
        val e = estado.value
        val lista = mutableListOf(
            Opcion("Tema", if (e.oscuro) "Oscuro" else "Claro") { alternarTema() },
            Opcion("Abrir juegos en", if (e.pantallaJuegos == "abajo") "Pantalla de abajo" else "Pantalla de arriba") {
                val v = if (estado.value.pantallaJuegos == "abajo") "arriba" else "abajo"
                almacen.pantallaJuegos = v
                actualizar { it.copy(pantallaJuegos = v) }
            },
            Opcion("Intercambiar pantallas", if (e.intercambiar) "Sí" else "No") {
                val v = !estado.value.intercambiar
                almacen.intercambiar = v
                actualizar { it.copy(intercambiar = v) }
            },
        )
        for (c in e.consolasConJuegos) {
            lista += Opcion("Emulador de ${c.nombre}", nombreEmulador(c)) { ciclarEmulador(c) }
        }
        lista += Opcion("Volver a buscar juegos", if (e.escaneando) "Buscando…" else "${e.juegos.size} juegos") { escanear() }
        lista += Opcion("Cambiar carpeta de juegos") { pedirCarpeta?.invoke(true) }
        lista += Opcion("Imágenes de ES-DE", if (e.tieneMedia) "Conectadas" else "Opcional") { pedirCarpeta?.invoke(false) }
        lista += Opcion("Cerrar ajustes") { cerrarPanel() }
        return lista
    }

    fun nombreEmulador(c: Consola): String =
        Lanzador.elegida(app, c, estado.value.emuladores[c.id])?.emulador?.nombre ?: "Elegir al abrir"

    fun emuladorListo(c: Consola): Boolean = Lanzador.elegida(app, c, estado.value.emuladores[c.id]) != null

    private fun ciclarEmulador(c: Consola) {
        val ops = Lanzador.opciones(app, c).distinctBy { it.paquete }
        if (ops.isEmpty()) {
            avisar("No hay emuladores de ${c.nombre} instalados. Te sugiero ${c.emuladores.first().nombre}.")
            return
        }
        val actual = Lanzador.elegida(app, c, estado.value.emuladores[c.id])?.paquete
        val i = ops.indexOfFirst { it.paquete == actual }
        val siguiente = ops[(i + 1) % ops.size]
        val m = estado.value.emuladores + (c.id to siguiente.paquete)
        almacen.guardarEmuladores(m)
        actualizar { it.copy(emuladores = m) }
    }
}

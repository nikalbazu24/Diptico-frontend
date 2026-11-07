package cl.nikalbazu.diptico.renacer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import cl.nikalbazu.diptico.data.CarpetaJuegos
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.data.Descargas
import cl.nikalbazu.diptico.emu.App
import cl.nikalbazu.diptico.emu.Lanzador
import cl.nikalbazu.diptico.estado.Respaldo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** El coordinador comparte una sesión entre las dos pantallas. La UI solo envía acciones. */
class RenacerControl(contexto: Context) {
    private val app = contexto.applicationContext
    private val biblioteca = BibliotecaRenacer(app)
    private val almacen = biblioteca.almacen
    private val errores = CoroutineExceptionHandler { _, error ->
        cl.nikalbazu.diptico.estado.Fallas.anotar(app, error)
        _estado.update { it.copy(cargando = false, aviso = "No pude completar esta acción. Puedes volver a intentarlo.") }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + errores)
    private val disco = Mutex()
    private val escaneos = Channel<Unit>(Channel.CONFLATED)
    private val guardados = Channel<Unit>(Channel.CONFLATED)
    private val _estado = MutableStateFlow(Salon())
    val estado: StateFlow<Salon> = _estado.asStateFlow()
    private val _apps = MutableStateFlow<List<App>>(emptyList())
    val apps: StateFlow<List<App>> = _apps.asStateFlow()
    private val _arte = MutableStateFlow<String?>(null)
    val arte: StateFlow<String?> = _arte.asStateFlow()
    private var listo = false

    init {
        scope.launch {
            try {
                val inicial = withContext(Dispatchers.IO) { disco.withLock {
                    val juegos = biblioteca.cargar()
                    Salon(juegos = juegos, datos = almacen.datos(), carpetas = almacen.carpetas(),
                        ambiente = Ambiente.entries.firstOrNull { it.name == almacen.texto("renacer_ambiente") } ?: Ambiente.PAPEL,
                        calendario = almacen.bool("renacer_calendario", true),
                        movimiento = almacen.bool("renacer_movimiento", true),
                        usarDos = almacen.bool("renacer_dos", true), cargando = false)
                } }
                _estado.update { inicial.copy(dosPantallas = it.dosPantallas) }
                listo = true
                escanear()
            } catch (cancelado: CancellationException) { throw cancelado }
            catch (_: Exception) {
                listo = true
                _estado.update { it.copy(cargando = false, error = "No pude cargar la biblioteca. Puedes volver a intentarlo.") }
            }
        }
        scope.launch {
            for (peticion in escaneos) leerBiblioteca()
        }
        scope.launch {
            for (peticion in guardados) {
                try {
                    withContext(Dispatchers.IO) { disco.withLock { guardarAhora() } }
                } catch (cancelado: CancellationException) { throw cancelado }
                catch (_: Exception) { avisar("No pude guardar tus cambios. Inténtalo otra vez.") }
            }
        }
    }

    fun ir(lugar: Lugar) = _estado.update { it.ir(lugar) }
    fun consola(id: String?) = _estado.update { it.abrirConsola(id) }
    fun buscar(texto: String) = _estado.update { it.copy(consulta = texto, lugar = Lugar.BIBLIOTECA) }
    fun filtrar(filtro: Filtro) = _estado.update { it.copy(filtro = filtro) }
    fun limpiar() = _estado.update(Salon::limpiar)
    fun volver() = _estado.update(Salon::volver)
    fun seleccionar(clave: String) = _estado.update { if (it.elegido == clave) it else it.copy(elegido = clave) }
    fun abrir(ventana: Ventana?) = _estado.update { it.copy(ventana = ventana) }
    fun avisar(texto: String?) = _estado.update { it.copy(aviso = texto) }
    fun lista() = _estado.update { it.copy(lista = !it.lista) }
    fun pantallas(disponibles: Boolean) = _estado.update { it.copy(dosPantallas = disponibles) }
    fun escanear() { if (listo) escaneos.trySend(Unit) }
    fun ambiente(ambiente: Ambiente) { _estado.update { it.copy(ambiente = ambiente) }; guardar() }
    fun calendario(valor: Boolean) { _estado.update { it.copy(calendario = valor) }; guardar() }
    fun movimiento(valor: Boolean) { _estado.update { it.copy(movimiento = valor) }; guardar() }
    fun usarDos(valor: Boolean) { _estado.update { it.copy(usarDos = valor) }; guardar() }

    private suspend fun leerBiblioteca() {
        _estado.update { it.copy(cargando = true, error = null) }
        try {
            val carpetas = estado.value.carpetas
            val lectura = withContext(Dispatchers.IO) { disco.withLock { biblioteca.escanear(carpetas) } }
            if (carpetas != estado.value.carpetas) { escaneos.trySend(Unit); return }
            _estado.update { actual ->
                val juegos = lectura.juegos.map { juego ->
                    actual.datos[juego.clave]?.portada?.let { juego.copy(portada = Uri.parse(it)) } ?: juego
                }
                actual.copy(juegos = juegos, error = lectura.fallas.takeIf { it.isNotEmpty() }
                    ?.joinToString(prefix = "Revisa el acceso a ", separator = ", "),
                    consola = actual.consola?.takeIf { id -> id == "*" || lectura.juegos.any { it.consolaId == id } })
            }
        } catch (cancelado: CancellationException) { throw cancelado }
        catch (_: Exception) { _estado.update { it.copy(error = "No pude leer tus juegos. Revisa la carpeta e inténtalo de nuevo.") } }
        finally { _estado.update { it.copy(cargando = false) } }
    }

    fun carpeta(uri: Uri) {
        try {
            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) { avisar("Android no permitió guardar el acceso a esta carpeta."); return }
        _estado.update { it.copy(carpetas = (it.carpetas + CarpetaJuegos(uri, null)).distinctBy { c -> c.uri }) }
        guardar()
        escanear()
    }

    fun quitarCarpeta(uri: Uri) {
        _estado.update { it.copy(carpetas = it.carpetas.filterNot { c -> c.uri == uri }) }
        guardar()
        escanear()
    }

    fun asignarConsola(uri: Uri, consola: String?) {
        _estado.update { it.copy(carpetas = it.carpetas.map { c -> if (c.uri == uri) c.copy(consola = consola) else c }) }
        guardar()
        escanear()
    }

    private fun dato(clave: String, cambio: (DatosJuego) -> DatosJuego) {
        _estado.update { it.copy(datos = it.datos + (clave to cambio(it.datos[clave] ?: DatosJuego()))) }
        guardar()
    }
    fun favorito(clave: String) = dato(clave) { it.copy(favorito = !it.favorito) }
    fun ocultar(clave: String) = dato(clave) { it.copy(oculto = !it.oculto) }
    fun paso(clave: String, texto: String) = dato(clave) { it.copy(paso = texto.take(2000)) }
    fun emulador(clave: String, id: String?) = dato(clave) { it.copy(emulador = id) }

    fun jugar(clave: String, pantalla: Int) {
        if (estado.value.partida != null) return
        val juego = estado.value.porClave[clave] ?: return
        val consola = Consolas.porId(juego.consolaId) ?: return
        val error = if (juego.uri.scheme == "app") Lanzador.lanzarApp(app, juego.archivo, juego.titulo, pantalla)
        else Lanzador.lanzar(app, juego, consola, estado.value.datos[clave]?.emulador, pantalla, BibliotecaRenacer.raiz(juego))
        if (error != null) { avisar(error); return }
        _estado.update { it.copy(partida = Partida(clave, pantalla, SystemClock.uptimeMillis()), ventana = null) }
        dato(clave) { it.copy(ultimo = System.currentTimeMillis()) }
    }

    fun pausa(pantalla: Int) = _estado.update { e ->
        e.copy(partida = e.partida?.let { if (it.pantalla == pantalla) it.copy(salio = true) else it })
    }

    fun volverDePartida(pantalla: Int) {
        Lanzador.olvidar()
        val partida = estado.value.partida?.takeIf { it.pantalla == pantalla && it.salio } ?: return
        val segundos = ((SystemClock.uptimeMillis() - partida.inicio) / 1000).coerceAtLeast(0)
        dato(partida.clave) { it.copy(tiempo = it.tiempo + segundos, sesiones = it.sesiones + if (segundos >= 15) 1 else 0) }
        _estado.update { it.copy(partida = null, elegido = partida.clave, lugar = Lugar.JUGAR) }
    }

    fun cargarApps() {
        abrir(Ventana.Apps)
        scope.launch {
            _apps.value = withContext(Dispatchers.IO) { Lanzador.apps(app) }
        }
    }

    fun agregarApp(paquete: String) {
        scope.launch {
            withContext(Dispatchers.IO) { disco.withLock { almacen.android = (almacen.android.orEmpty() + paquete).distinct() } }
            escanear()
            abrir(null)
        }
    }

    fun buscarArte(clave: String) {
        if (_arte.value != null) return
        val juego = estado.value.porClave[clave] ?: return
        val consola = Consolas.porId(juego.consolaId) ?: return
        _arte.value = clave
        scope.launch {
            try {
                val encontrado = withContext(Dispatchers.IO) {
                    val resultado = Descargas.buscar(app, juego, consola, null, almacen.claveSgdb)
                    disco.withLock { almacen.guardarMedios(almacen.medios() + (clave to resultado.medios)) }
                    resultado.encontrado
                }
                escanear()
                avisar(if (encontrado) "Tu juego ya tiene una nueva portada." else "No encontré arte para este juego. Puedes elegir una imagen tuya.")
            } catch (cancelado: CancellationException) { throw cancelado }
            catch (_: Exception) { avisar("No pude buscar la portada. Revisa la conexión e inténtalo otra vez.") }
            finally { _arte.value = null }
        }
    }

    fun imagen(clave: String, uri: Uri) {
        try { app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        catch (_: SecurityException) { avisar("No pude conservar el acceso a esa imagen."); return }
        dato(clave) { it.copy(portada = uri.toString()) }
        _estado.update { e -> e.copy(juegos = e.juegos.map { if (it.clave == clave) it.copy(portada = uri) else it }) }
    }

    fun exportar(uri: Uri) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { disco.withLock {
                guardarAhora()
                Respaldo.escribirEn(app, uri)
            } }
            avisar(if (ok) "Tu biblioteca personal quedó respaldada." else "No pude escribir el respaldo.")
        }
    }

    fun importar(uri: Uri) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { disco.withLock {
                    app.contentResolver.openInputStream(uri)?.use { Respaldo.restaurar(app, it) } ?: error("Sin acceso")
                    val datos = almacen.datos()
                    _estado.update { it.copy(datos = datos) }
                } }
                escanear()
                avisar("Recuperé tus favoritos, tiempos y notas. Agrega las carpetas de este equipo para ver sus juegos.")
            } catch (cancelado: CancellationException) { throw cancelado }
            catch (_: Exception) { avisar("No pude recuperar ese respaldo. Revisa el archivo.") }
        }
    }

    private fun guardar() { guardados.trySend(Unit) }
    private fun guardarAhora() {
        val e = estado.value
        almacen.guardarDatos(e.datos)
        almacen.guardarCarpetas(e.carpetas)
        almacen.guardarTexto("renacer_ambiente", e.ambiente.name)
        almacen.guardarBool("renacer_calendario", e.calendario)
        almacen.guardarBool("renacer_movimiento", e.movimiento)
        almacen.guardarBool("renacer_dos", e.usarDos)
    }
}

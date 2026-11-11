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

/*
 * Centro de control y notificaciones: funciones de Diptico movidas aquí desde Diptico.kt para que ese archivo no siga creciendo.
 * Se llaman igual que antes (Diptico.x()); fuera del paquete estado hay que importar cl.nikalbazu.diptico.estado.*.
 */


/**
 * Abre la cortina de arriba: [cual] "avisos" (notificaciones) o "ajustes" (ajustes rápidos), como en los
 * teléfonos Samsung o Xiaomi. Sin [cual], la de ajustes. Si ya estaba abierta en esa misma, se cierra.
 */
fun Diptico.abrirCentro(cual: String = "ajustes") {
    Medios.revisar(app)
    val e = estado.value
    if (e.panel == Panel.CENTRO && e.cortina == cual) { cerrarPanel(); return }
    actualizar { it.copy(panel = Panel.CENTRO, cortina = cual, indice = 0) }
    Sonidos.deslizar()
}


/** L1/R1 (o tocar la pestaña): pasa de notificaciones a ajustes y al revés. */
fun Diptico.cambiarCortina(cual: String? = null) {
    val e = estado.value
    val v = cual ?: if (e.cortina == "avisos") "ajustes" else "avisos"
    if (v == e.cortina) return
    actualizar { it.copy(cortina = v, indice = 0) }
    Sonidos.pagina()
}


internal fun Diptico.abrirIntent(i: Intent, error: String) {
    try {
        app.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        avisar(error)
    }
}


/** Abre el administrador de archivos de Android. */
fun Diptico.abrirArchivos() {
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


fun Diptico.appsDeConsola(): List<App> = appsConsola ?: run {
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
fun Diptico.appsMusica(): List<App> = MUSICA_APPS.filter { (p, _) -> Lanzador.instalado(app, p) }.map { (p, n) -> App(p, n) }


/** Abre una app en la pantalla [displayId] (la del dock que tocaste); si no se puede, como siempre. */
fun Diptico.abrirPaqueteAqui(paquete: String, displayId: Int?) {
    val i = app.packageManager.getLaunchIntentForPackage(paquete) ?: run { avisar("Esa app no está instalada"); return }
    if (displayId == null) { abrirPaquete(paquete); return }
    cerrarPanel()
    try {
        app.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            android.app.ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle())
    } catch (_: Exception) { abrirPaquete(paquete) }
}


fun Diptico.abrirPaquete(paquete: String) {
    val i = app.packageManager.getLaunchIntentForPackage(paquete)
    if (i == null) avisar("Esa app no está instalada") else {
        cerrarPanel()
        abrirIntent(i, "No pude abrir la app")
    }
}


/** Mueve la selección del centro: grilla de accesos arriba, lista de notificaciones abajo. */
fun Diptico.moverCentro(dx: Int, dy: Int) {
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


/** Los accesos de la cortina en el orden y la selección de la persona (vacío = todos, como vienen). */
internal fun Diptico.accesosElegidos(): List<Opcion> {
    val todos = opcionesSistema()
    val orden = estado.value.accesosCentro
    if (orden.isEmpty()) return todos
    return orden.mapNotNull { t -> todos.firstOrNull { it.titulo == t } }
}


internal fun Diptico.guardarAccesos(lista: List<String>) {
    almacen.guardarTexto("accesosCentro", lista.joinToString("|"))
    actualizar { it.copy(extra = it.extra.copy(accesosCentro = lista), centroVersion = it.centroVersion + 1) }
}


/** Muestra u oculta un acceso de la cortina. */
fun Diptico.alternarAcceso(titulo: String) {
    val todos = opcionesSistema().map { it.titulo }
    val actual = estado.value.accesosCentro.ifEmpty { todos }
    val nueva = if (titulo in actual) actual - titulo else actual + titulo
    if (nueva.isEmpty()) { avisar("Deja al menos un acceso"); return }
    guardarAccesos(nueva)
}


/** Sube un acceso un lugar en la cortina (X en la lista de accesos). */
fun Diptico.subirAcceso(titulo: String) {
    val actual = estado.value.accesosCentro.ifEmpty { opcionesSistema().map { it.titulo } }.toMutableList()
    val i = actual.indexOf(titulo)
    if (i <= 0) return
    actual.removeAt(i); actual.add(i - 1, titulo)
    guardarAccesos(actual)
    actualizar { it.copy(indice = (it.indice - 1).coerceAtLeast(0)) }
}


fun Diptico.opcionesCentro(): List<Opcion> = accesosElegidos() +
    appsDeConsola().map { a -> Opcion(a.nombre, "Consola", Icono.MANDO, paquete = a.paquete) { abrirPaquete(a.paquete) } } +
    appsMusica().map { a -> Opcion(a.nombre, "Música", Icono.MUSICA, paquete = a.paquete) { abrirPaquete(a.paquete) } } +
    // Siempre al final: el lápiz para elegir y ordenar los accesos, como en los teléfonos.
    Opcion("Editar accesos", icono = Icono.EDITAR) { abrirGaleria("accesos_cortina") }


/** Cómo están ahora los accesos que se encienden y apagan (para que se note cuáles están activos). */
internal fun Diptico.wifiEncendido(): Boolean? = try { (app.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager)?.isWifiEnabled } catch (_: Exception) { null }

internal fun Diptico.bluetoothEncendido(): Boolean? = try { app.getSystemService(android.bluetooth.BluetoothManager::class.java)?.adapter?.isEnabled } catch (_: Exception) { null }

internal fun Diptico.conSonido(): Boolean? = try { (app.getSystemService(android.media.AudioManager::class.java)?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 0) > 0 } catch (_: Exception) { null }

internal fun Diptico.brilloAuto(): Boolean? = try { android.provider.Settings.System.getInt(app.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE) == 1 } catch (_: Exception) { null }

internal fun Diptico.noMolestarActivo(): Boolean = try {
    (app.getSystemService(android.app.NotificationManager::class.java)?.currentInterruptionFilter ?: 1) > android.app.NotificationManager.INTERRUPTION_FILTER_ALL
} catch (_: Exception) { false }


fun Diptico.alternarNoMolestar() {
    if (!Turbo.puedeNoMolestar(app)) { Turbo.pedirNoMolestar(app); avisar("Activa Díptico en la lista y vuelve"); return }
    val nm = app.getSystemService(android.app.NotificationManager::class.java) ?: return
    val activar = !noMolestarActivo()
    try { nm.setInterruptionFilter(if (activar) android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY else android.app.NotificationManager.INTERRUPTION_FILTER_ALL) } catch (_: Exception) {}
    avisar(if (activar) "No molestar: encendido" else "No molestar: apagado")
    actualizar { it.copy(centroVersion = it.centroVersion + 1) }
}


internal fun Diptico.opcionesSistema(): List<Opcion> = listOf(
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
    Opcion("DIPPO", icono = Icono.BISA) { abrirAsistente() },
    Opcion("Estadísticas", icono = Icono.RELOJ) { abrirEstadisticas() },
    Opcion("Protector", icono = Icono.ESTRELLA) { cerrarPanel(); actualizar { it.copy(protector = true) } },
    Opcion("Ajustes", icono = Icono.AJUSTES) { abrirIntent(Intent(android.provider.Settings.ACTION_SETTINGS), "No pude abrir los ajustes") },
) + (if (!estado.value.unaPantalla) listOf(Opcion("Intercambiar", icono = Icono.INTERCAMBIAR) { cerrarPanel(); intercambiar() }) else emptyList())


fun Diptico.abrirAviso(a: Aviso) {
    cerrarPanel()
    Avisos.abrir(a)
}


fun Diptico.descartarAvisoSel() {
    val e = estado.value
    if (e.cortina != "avisos") return
    Avisos.lista.value.getOrNull(e.indice)?.let { Avisos.descartar(it) }
    val quedan = Avisos.lista.value.size
    if (e.indice >= quedan && quedan > 0) actualizar { it.copy(indice = quedan - 1) }
}


internal fun Diptico.opcionesConsola(e: Estado): List<Opcion> = listOf(
    Opcion(
        "Accesos de la cortina", if (e.accesosCentro.isEmpty()) "Todos" else "${e.accesosCentro.size} elegidos", Icono.EDITAR,
        "Elige qué accesos aparecen en los ajustes rápidos (Wi-Fi, Bluetooth, turbo…) y en qué orden. A muestra u oculta; X sube el acceso un lugar.",
    ) { abrirGaleria("accesos_cortina") },
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

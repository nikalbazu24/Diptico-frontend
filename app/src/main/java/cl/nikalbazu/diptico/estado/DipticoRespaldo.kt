package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.estado.Diptico.Control
import cl.nikalbazu.diptico.estado.Diptico.InfoBateria
import cl.nikalbazu.diptico.estado.Diptico.ItemGaleria
import cl.nikalbazu.diptico.estado.Diptico.LanzadorPC
import cl.nikalbazu.diptico.estado.Diptico.OpcionBurbuja
import cl.nikalbazu.diptico.estado.Diptico.PorBorrar
import cl.nikalbazu.diptico.estado.Diptico.Regreso
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
 * Respaldo en Google Drive: funciones de Diptico movidas aquí desde Diptico.kt para que ese archivo no siga creciendo.
 * Se llaman igual que antes (Diptico.x()); fuera del paquete estado hay que importar cl.nikalbazu.diptico.estado.*.
 */

internal fun Diptico.opcionesRespaldo(e: Estado): List<Opcion> {
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
            "Respaldo de partidas",
            when { e.extra.carpetasPartidas.isEmpty() -> "Elegir carpeta"; e.extra.respaldoPartidas -> "Sí · ${e.extra.carpetasPartidas.size} carpeta(s)"; else -> "No" },
            Icono.COPIAR,
            "Al terminar de jugar, Díptico copia los guardados que cambiaron en tus carpetas de partidas (por ejemplo RetroArch/saves). " +
                "Guarda las últimas ${cl.nikalbazu.diptico.data.RespaldoPartidas.VERSIONES} por juego y la más nueva viaja en tu respaldo de Drive. " +
                "Se recuperan desde las opciones de cada juego › Copias de partida.",
            grupo = "Partidas",
        ) {
            if (estado.value.extra.carpetasPartidas.isEmpty()) pedirCarpeta?.invoke(Carpeta.PARTIDAS) ?: avisar("No pude abrir el selector de carpetas")
            else {
                val v = !estado.value.extra.respaldoPartidas
                almacen.guardarBool("respaldoPartidas", v)
                actualizar { it.copy(extra = it.extra.copy(respaldoPartidas = v)) }
            }
        },
        Opcion("Agregar carpeta de partidas", "Otra carpeta", Icono.CARPETA,
            "Suma otra carpeta donde un emulador guarde sus partidas (cada emulador usa la suya).", grupo = "Partidas") {
            pedirCarpeta?.invoke(Carpeta.PARTIDAS) ?: avisar("No pude abrir el selector de carpetas")
        },
        if (e.extra.carpetasPartidas.isNotEmpty()) Opcion("Quitar las carpetas de partidas", "${e.extra.carpetasPartidas.size}", Icono.BORRAR,
            "Díptico deja de mirar esas carpetas. Las copias que ya hizo se quedan.", grupo = "Partidas") {
            almacen.guardarTexto("carpetasPartidas", "")
            actualizar { it.copy(extra = it.extra.copy(carpetasPartidas = emptyList())) }
            avisar("Carpetas de partidas quitadas")
        } else null,
        Opcion(
            "Traer un respaldo", icono = Icono.PEGAR,
            ayuda = "Elige un respaldo de Díptico (de Drive o de donde lo tengas) y recupera todo. Sirve para pasar tus cosas a otra consola. Reemplaza tus ajustes actuales.",
        ) { pedirTraerRespaldo?.invoke() ?: avisar("No pude abrir el selector de archivos") },
    )
}


/** "hoy 14:05", "ayer 09:30" o "3 nov" (en el idioma de Díptico). */
internal fun Diptico.cuandoFue(t: Long): String {
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


internal fun Diptico.elegirDondeRespaldar() {
    val fecha = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    pedirCrearRespaldo?.invoke("Diptico-respaldo-$fecha.zip") ?: avisar("No pude abrir el selector de archivos")
}


/** Volvió el selector con el archivo nuevo (por ejemplo en Drive): se guarda el permiso y se respalda al tiro. */
fun Diptico.respaldoCreado(uri: Uri) {
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


fun Diptico.respaldarAhora(silencioso: Boolean = false) {
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
internal fun Diptico.respaldoDiario() {
    if (almacen.texto("respaldoUri").isBlank()) return
    if (System.currentTimeMillis() - estado.value.respaldoUltimo < 20 * 3_600_000L) return
    scope.launch { delay(15_000); respaldarAhora(silencioso = true) }
}


/** Volvió el selector con un respaldo para traer: se confirma, se recupera y Díptico se reinicia. */
fun Diptico.respaldoElegido(uri: Uri) {
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
internal fun Diptico.reiniciarDiptico() {
    try {
        val i = app.packageManager.getLaunchIntentForPackage(app.packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (i != null) {
            val pi = android.app.PendingIntent.getActivity(app, 7771, i, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_CANCEL_CURRENT)
            (app.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager).set(android.app.AlarmManager.ELAPSED_REALTIME, android.os.SystemClock.elapsedRealtime() + 700, pi)
        }
    } catch (_: Exception) {}
    android.os.Process.killProcess(android.os.Process.myPid())
}


fun Diptico.nombreArranque(v: String) = when (v) {
    "caratulas" -> "Carátulas"; "widgets" -> "Widgets"; "consolas" -> "Tus consolas"; "ultimo" -> "Tu último juego"; else -> "Como lo dejaste"
}


fun Diptico.mostrarBienvenida(ausencia: Long) {
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


fun Diptico.cerrarBienvenida() {
    bienvenidaJob?.cancel()
    if (estado.value.bienvenida != 0L) actualizar { it.copy(bienvenida = 0L) }
}


/** "Seguir jugando" desde la bienvenida: abre el último juego. */
fun Diptico.seguirDesdeBienvenida() {
    val j = ultimoJugado()
    cerrarBienvenida()
    if (j == null) return
    actualizar { it.copy(consolaSel = "todo", juegoSel = j.clave, panel = Panel.NINGUNO, enSistemas = false) }
    jugar()
}


/** Encendiste la consola en medio de un juego: un recordatorio de dónde ibas. */
fun Diptico.mostrarContinuacion() {
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


fun Diptico.cambiarBienvenidaModo() {
    val l = listOf("completa", "breve", "no")
    val v = l[(l.indexOf(estado.value.bienvenidaModo) + 1).mod(l.size)]
    almacen.guardarTexto("bienvenidaModo2", v)
    actualizar { it.copy(bienvenidaModo = v) }
    if (v != "no") mostrarBienvenida(3 * 3_600_000L)
}


fun Diptico.nombreBienvenida(v: String) = when (v) { "breve" -> "Breve"; "no" -> "No"; else -> "Completa" }

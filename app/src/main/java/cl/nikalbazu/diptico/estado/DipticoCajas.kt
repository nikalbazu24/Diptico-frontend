package cl.nikalbazu.diptico.estado

import android.net.Uri
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Raspadores
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Guarda las caras de un juego (archivos locales) como su caja 3D. [origen] "propia" (armada por la persona) reemplaza
 * la caja entera; "comunidad" solo completa lo que falte. Los archivos de la caja anterior que quedan sin uso se borran.
 */
private fun Diptico.ponerCaras(clave: String, c: CajasComunidad.Caras, origen: String) {
    val antes = estado.value.extra.caras[clave]
    val nuevo = CarasGuardadas(
        c.reverso?.let { Uri.fromFile(it).toString() },
        c.lomo?.let { Uri.fromFile(it).toString() },
        System.currentTimeMillis(),
        c.frente?.let { Uri.fromFile(it).toString() },
        origen = origen,
    )
    // Una caja propia nueva reemplaza todo (si solo trae el frente, el reverso se arma con la ficha, no con la caja vieja).
    val g = if (origen == "propia") nuevo.conEstiloDe(antes) else CajasLocales.combinar(antes, nuevo)
    guardarCaras(clave, g)
    scope.launch(Dispatchers.IO) { CajasLocales.borrarSinUso(app, antes, estado.value.extra.caras.values) }
}

/** Deja guardada la caja de un juego (en el estado y en el almacén). */
internal fun Diptico.guardarCaras(clave: String, g: CarasGuardadas?) {
    actualizar { it.copy(extra = it.extra.copy(caras = if (g == null) it.extra.caras - clave else it.extra.caras + (clave to g))) }
    almacen.guardarTexto("carasCaja2", CarasGuardadas.escribir(estado.value.extra.caras))
}

/**
 * Al abrir Díptico (en segundo plano): revisa que las cajas guardadas sigan en el equipo, arregla rutas que vienen de
 * un respaldo y baja a archivos propios las caras que todavía eran enlaces (de versiones anteriores). Sin internet no
 * pasa nada: lo ya guardado se ve igual y lo pendiente se baja en la próxima vuelta.
 */
internal fun Diptico.revisarCajasGuardadas() {
    scope.launch {
        val antes = estado.value.extra.caras
        val revisadas = withContext(Dispatchers.IO) { runCatching { CajasLocales.rebasar(app, antes) }.getOrDefault(antes) }
        if (revisadas != antes) {
            actualizar { e -> e.copy(extra = e.extra.copy(caras = e.extra.caras + revisadas.filterKeys { e.extra.caras[it] == antes[it] })) }
            almacen.guardarTexto("carasCaja2", CarasGuardadas.escribir(estado.value.extra.caras))
        }
        bajarCarasPendientes()
    }
}

/** Baja a archivos locales las caras que siguen siendo enlaces (de a una, sin apurar nada). */
internal suspend fun Diptico.bajarCarasPendientes() {
    val pendientes = estado.value.extra.caras.filterValues(CajasLocales::pendiente).keys.toList()
    var cambios = 0
    for (clave in pendientes) {
        val g = estado.value.extra.caras[clave] ?: continue
        val local = withContext(Dispatchers.IO) { runCatching { CajasLocales.localizar(app, clave, g) }.getOrNull() } ?: continue
        if (local == g) continue
        // Si mientras tanto cambió la caja (la persona armó otra), no se pisa.
        actualizar { e -> if (e.extra.caras[clave] == g) e.copy(extra = e.extra.copy(caras = e.extra.caras + (clave to local))) else e }
        if (++cambios % 10 == 0) almacen.guardarTexto("carasCaja2", CarasGuardadas.escribir(estado.value.extra.caras))
    }
    if (cambios > 0) almacen.guardarTexto("carasCaja2", CarasGuardadas.escribir(estado.value.extra.caras))
}

/** Pide la imagen de la caja (abierta y escaneada entera, o solo el frente) para que DIPPO arme la caja 3D. */
fun Diptico.armarCajaConImagen(clave: String? = null) {
    val clave = clave ?: estado.value.fichaDe ?: estado.value.juegoSel ?: return
    avisar("Elige una foto o escaneo de la caja: abierta (reverso, lomo y frente) o solo el frente")
    pedirImagen("caja:$clave")
}

/** DIPPO arma la caja desde la imagen elegida y ofrece compartirla con la comunidad. */
internal fun Diptico.cajaDesdeImagen(clave: String, uri: Uri) {
    val j = estado.value.juegosPorClave[clave] ?: return
    val consola = Consolas.porId(j.consolaId) ?: return
    val llave = CajasComunidad.clave(j.titulo, j.consolaId)
    // Las cajas se guardan por juego: si la armas de nuevo, reemplaza a la anterior (otro nombre para que no quede la vieja en caché).
    val destino = CajasComunidad.carpeta(app, "$llave-${System.currentTimeMillis() % 100000}")
    actualizar { it.copy(pensando = true, pensandoQue = "DIPPO está armando la caja…") }
    scope.launch {
        val caras = withContext(Dispatchers.IO) {
            runCatching { CajasComunidad.armar(app, uri, consola.caja.ancho / consola.caja.alto, destino) }.getOrNull()
        }
        actualizar { it.copy(pensando = false, pensandoQue = "") }
        if (caras == null || caras.vacia) { avisar("No pude leer esa imagen. Prueba con otra"); return@launch }
        ponerCaras(clave, caras, "propia")
        sumarDippo("caja")
        val queArmo = if (caras.reverso != null) "frente, lomo y reverso" else "el frente (el reverso lo arma con la ficha)"
        if (!CajasComunidad.disponible) { avisar("¡Caja lista! DIPPO usó $queArmo"); return@launch }
        mostrarDialogo(Dialogo(
            "compartir-caja", "¡Caja de ${j.titulo} lista!",
            "DIPPO usó $queArmo. ¿La compartes con la comunidad de Díptico? Así a quien tenga este juego le aparece sola.",
            Icono.PORTADA, si = "Compartir", no = "Solo para mí",
        )) {
            scope.launch {
                val motivo = withContext(Dispatchers.IO) { CajasComunidad.subir(llave, j.titulo, consola.bonito, caras) }
                if (motivo == null) avisar("¡Gracias! La caja quedó en la comunidad")
                else mostrarDialogo(Dialogo("caja-no-compartida", "No se pudo compartir", "$motivo.\n\nTu caja igual quedó guardada en este equipo.", Icono.INFO, si = "Entendido", no = "Cerrar")) {}
            }
        }
    }
}

/** Busca la caja del juego en la comunidad (antes que en ScreenScraper o TheGamesDB). true = la encontró. */
internal suspend fun Diptico.cajaDeLaComunidad(clave: String, respetarPropia: Boolean = true): Boolean {
    val j = estado.value.juegosPorClave[clave] ?: return false
    val llave = CajasComunidad.clave(j.titulo, j.consolaId)
    // La caja que armó la persona manda: la de la comunidad no la reemplaza sola.
    if (respetarPropia && estado.value.extra.caras[clave]?.propia == true) return false
    val caras = withContext(Dispatchers.IO) { CajasComunidad.bajar(llave, CajasComunidad.carpeta(app, llave)) } ?: return false
    ponerCaras(clave, caras, "comunidad")
    return true
}

/** Quita la caja propia (o de la comunidad) de un juego: vuelve a la portada y al reverso armado. */
fun Diptico.quitarCajaPropia(clave: String? = null) {
    val clave = clave ?: estado.value.fichaDe ?: return
    val antes = estado.value.extra.caras[clave]
    guardarCaras(clave, null)
    // La persona la quitó a propósito: sus archivos se borran (nada más que esos).
    scope.launch(Dispatchers.IO) { CajasLocales.borrarSinUso(app, antes, estado.value.extra.caras.values) }
    avisar("Listo, la caja volvió a la portada")
}

/** Vuelve a buscar la caja original: primero en la comunidad, después en ScreenScraper y TheGamesDB. */
fun Diptico.buscarCajaOriginal() {
    val clave = estado.value.fichaDe ?: return
    // La caja de ahora se conserva hasta que aparezca otra: si no hay internet o no se encuentra, no se pierde nada.
    avisar("Buscando la caja original…")
    pedirCaras(clave, forzar = true)
}

/**
 * Busca la caja en ScreenScraper o TheGamesDB, baja sus caras al equipo y las guarda. true = encontró algo.
 * Sin internet o sin resultado, lo que ya había queda igual (solo se anota cuándo se buscó).
 * [explicito] = la persona lo pidió: puede reemplazar una caja armada por ella; si no, esa nunca se toca.
 */
internal suspend fun Diptico.cajaDeInternet(clave: String, explicito: Boolean): Boolean {
    val e = estado.value
    val j = e.juegosPorClave[clave] ?: return false
    val c = e.raspadores
    if (!c.screenscraper && !c.conTgdb) return false
    if (!explicito && e.extra.caras[clave]?.propia == true) return false
    val r = withContext(Dispatchers.IO) { runCatching { Raspadores.caras(c, j.titulo, j.archivo, j.consolaId) }.getOrNull() }
    val ahora = System.currentTimeMillis()
    if (r == null || (r.frente == null && r.reverso == null && r.lomo == null)) {
        guardarCaras(clave, (estado.value.extra.caras[clave] ?: CarasGuardadas(null, null, ahora)).copy(buscado = ahora))
        return false
    }
    val nuevo = withContext(Dispatchers.IO) {
        CajasLocales.localizar(app, clave, CarasGuardadas(r.reverso, r.lomo, ahora, r.frente, origen = "internet"))
    }
    val actual = estado.value.extra.caras[clave]
    // Si la persona armó una caja mientras se buscaba, se queda la suya.
    if (!explicito && actual?.propia == true) return false
    guardarCaras(clave, if (actual?.propia == true) nuevo.conEstiloDe(actual) else CajasLocales.combinar(actual, nuevo))
    withContext(Dispatchers.IO) { CajasLocales.borrarSinUso(app, actual, estado.value.extra.caras.values) }
    return true
}

/** Las imágenes nuevas con la forma, el color y el material que la persona ya había elegido en el Estudio. */
internal fun CarasGuardadas.conEstiloDe(antes: CarasGuardadas?): CarasGuardadas =
    if (antes == null) this else copy(plantilla = antes.plantilla, color = antes.color, material = antes.material)

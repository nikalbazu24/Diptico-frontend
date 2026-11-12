package cl.nikalbazu.diptico.estado

import android.net.Uri
import cl.nikalbazu.diptico.data.Consolas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Guarda las caras de un juego (archivos locales) como su caja 3D. */
private fun Diptico.ponerCaras(clave: String, c: CajasComunidad.Caras) {
    val antes = estado.value.extra.caras[clave]
    val g = CarasGuardadas(
        c.reverso?.let { Uri.fromFile(it).toString() } ?: antes?.reverso,
        c.lomo?.let { Uri.fromFile(it).toString() } ?: antes?.lomo,
        System.currentTimeMillis(),
        c.frente?.let { Uri.fromFile(it).toString() } ?: antes?.frente,
    )
    actualizar { it.copy(extra = it.extra.copy(caras = it.extra.caras + (clave to g))) }
    almacen.guardarTexto("carasCaja2", CarasGuardadas.escribir(estado.value.extra.caras))
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
        ponerCaras(clave, caras)
        sumarDippo("caja")
        val queArmo = if (caras.reverso != null) "frente, lomo y reverso" else "el frente (el reverso lo arma con la ficha)"
        if (!CajasComunidad.disponible) { avisar("¡Caja lista! DIPPO usó $queArmo"); return@launch }
        mostrarDialogo(Dialogo(
            "compartir-caja", "¡Caja de ${j.titulo} lista!",
            "DIPPO usó $queArmo. ¿La compartes con la comunidad de Díptico? Así a quien tenga este juego le aparece sola.",
            Icono.PORTADA, si = "Compartir", no = "Solo para mí",
        )) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { CajasComunidad.subir(llave, j.titulo, consola.bonito, caras) }
                avisar(if (ok) "¡Gracias! La caja quedó en la comunidad" else "No se pudo compartir ahora. Tu caja igual quedó guardada")
            }
        }
    }
}

/** Busca la caja del juego en la comunidad (antes que en ScreenScraper o TheGamesDB). true = la encontró. */
internal suspend fun Diptico.cajaDeLaComunidad(clave: String): Boolean {
    val j = estado.value.juegosPorClave[clave] ?: return false
    val llave = CajasComunidad.clave(j.titulo, j.consolaId)
    val caras = withContext(Dispatchers.IO) { CajasComunidad.bajar(llave, CajasComunidad.carpeta(app, llave)) } ?: return false
    ponerCaras(clave, caras)
    return true
}

/** Quita la caja propia (o de la comunidad) de un juego: vuelve a la portada y al reverso armado. */
fun Diptico.quitarCajaPropia(clave: String? = null) {
    val clave = clave ?: estado.value.fichaDe ?: return
    actualizar { it.copy(extra = it.extra.copy(caras = it.extra.caras - clave)) }
    almacen.guardarTexto("carasCaja2", CarasGuardadas.escribir(estado.value.extra.caras))
    avisar("Listo, la caja volvió a la portada")
}

/** Vuelve a buscar la caja original: primero en la comunidad, después en ScreenScraper y TheGamesDB. */
fun Diptico.buscarCajaOriginal() {
    val clave = estado.value.fichaDe ?: return
    actualizar { it.copy(extra = it.extra.copy(caras = it.extra.caras - clave)) }
    avisar("Buscando la caja original…")
    pedirCaras(clave, forzar = true)
}

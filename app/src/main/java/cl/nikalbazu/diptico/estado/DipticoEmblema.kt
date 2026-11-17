package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.audio.Sonidos
import cl.nikalbazu.diptico.data.Juego

/**
 * El juego emblema de cada consola: el que va detrás de ella, grande y difuminado. Por defecto es tu más jugado;
 * si todavía no juegas ninguno, el primero que tenga imagen. Se puede cambiar por otro (‹ ›) o volver a la consola.
 */

/** Candidatos a emblema de una consola: con imagen, del más jugado al menos jugado. */
fun Estado.candidatosEmblema(consolaId: String): List<Juego> =
    juegosPorConsola[consolaId].orEmpty().filter { it.arte != null || it.portada != null }
        .sortedWith(compareByDescending<Juego> { datos[it.clave]?.tiempo ?: 0L }.thenByDescending { it.arte != null })
        .take(12)

fun Estado.juegoEmblema(consolaId: String): Juego? {
    val lista = juegosPorConsola[consolaId].orEmpty()
    extra.emblemas[consolaId]?.let { k -> lista.firstOrNull { it.clave == k }?.let { return it } }
    return candidatosEmblema(consolaId).firstOrNull()
}

/** ¿El emblema es el que se eligió solo (tu más jugado) o uno que elegiste tú? */
fun Estado.emblemaElegido(consolaId: String): Boolean = extra.emblemas[consolaId] != null

fun Diptico.elegirFondoConsolas(v: String) {
    if (v !in setOf("emblema", "consola")) return
    almacen.guardarTexto("fondoConsolas", v)
    actualizar { it.copy(extra = it.extra.copy(fondoConsolas = v)) }
}

fun Diptico.alternarFondoConsolas() = elegirFondoConsolas(if (estado.value.extra.fondoConsolas == "emblema") "consola" else "emblema")

/** Pasa al juego emblema siguiente o anterior de la consola elegida (y deja el fondo en «emblema»). */
fun Diptico.cambiarEmblema(d: Int, consolaId: String = estado.value.consolaSel) {
    val e = estado.value
    val l = e.candidatosEmblema(consolaId)
    if (l.isEmpty()) return
    val i = l.indexOfFirst { it.clave == e.juegoEmblema(consolaId)?.clave }.coerceAtLeast(0)
    val nuevo = l[(i + d).mod(l.size)]
    val mapa = e.extra.emblemas + (consolaId to nuevo.clave)
    almacen.guardarTexto("emblemas", mapa.entries.joinToString("|") { "${it.key}=${it.value}" })
    if (e.extra.fondoConsolas != "emblema") almacen.guardarTexto("fondoConsolas", "emblema")
    actualizar { it.copy(extra = it.extra.copy(emblemas = mapa, fondoConsolas = "emblema")) }
    Sonidos.mover()
}

/** Vuelve a «tu más jugado» en esa consola. */
fun Diptico.emblemaAutomatico(consolaId: String = estado.value.consolaSel) {
    val mapa = estado.value.extra.emblemas - consolaId
    almacen.guardarTexto("emblemas", mapa.entries.joinToString("|") { "${it.key}=${it.value}" })
    actualizar { it.copy(extra = it.extra.copy(emblemas = mapa)) }
}

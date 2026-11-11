package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.audio.Sonidos
import cl.nikalbazu.diptico.audio.Vibracion

/*
 * Carpetas de consolas: ordenarlas a gusto (modo edición) y elegir su estilo.
 * Se llaman como cualquier función de Diptico: Diptico.empezarOrdenCarpetas(), etc.
 */

/** Los estilos de carpeta (id, nombre, descripción). */
val ESTILOS_CARPETA: List<Triple<String, String, String>> = listOf(
    Triple("tarjeta", "Tarjeta", "El arte de la consola en una tarjeta con su nombre."),
    Triple("consola", "Consola", "El dibujo de la consola al centro, sobre su color."),
    Triple("abanico", "Abanico de portadas", "Tus tres juegos más jugados asomando."),
    Triple("logo", "Logo grande", "Solo el nombre de la consola, grande y sobrio."),
)

fun nombreEstiloCarpeta(id: String) = ESTILOS_CARPETA.firstOrNull { it.first == id }?.second ?: "Tarjeta"

/** Modo edición de las carpetas: todas tiemblan y se pueden mover (con el dedo o con A + cruceta). */
fun Diptico.empezarOrdenCarpetas(tomar: String? = null) {
    if (estado.value.consolasConJuegos.size < 2) { avisar("Necesitas al menos dos consolas para ordenarlas"); return }
    actualizar { it.copy(extra = it.extra.copy(ordenandoCarpetas = true, carpetaTomada = tomar)) }
    Vibracion.elegir()
}

fun Diptico.terminarOrdenCarpetas() {
    if (!estado.value.extra.ordenandoCarpetas) return
    actualizar { it.copy(extra = it.extra.copy(ordenandoCarpetas = false, carpetaTomada = null)) }
    Sonidos.aviso()
}

/** A: toma la carpeta elegida (flota) o la suelta donde está. */
fun Diptico.tomarOSoltarCarpeta() {
    val e = estado.value
    if (e.consolaSel == "todo") { Sonidos.borde(); return }
    val nueva = if (e.extra.carpetaTomada == null) e.consolaSel else null
    actualizar { it.copy(extra = it.extra.copy(carpetaTomada = nueva)) }
    Vibracion.elegir()
}

/** Pone la consola [id] en el lugar [posicion] de las carpetas que se ven (la de «Todos los juegos», si está, no se mueve). */
fun Diptico.soltarCarpetaEn(id: String, posicion: Int, sigueTomada: Boolean = false) {
    val e = estado.value
    if (id == "todo") return
    val desfase = if (e.pestanas.firstOrNull() == "todo") 1 else 0
    val l = e.consolasConJuegos.map { it.id }.toMutableList()
    val i = l.indexOf(id)
    if (i < 0) return
    val n = (posicion - desfase).coerceIn(0, l.size - 1)
    if (n != i) {
        l.removeAt(i)
        l.add(n, id)
        almacen.guardarTexto("ordenConsolas", l.joinToString(","))
        Vibracion.mover()
    } else if (sigueTomada) Sonidos.borde()
    actualizar { it.copy(ordenConsolas = l, consolaSel = id, extra = it.extra.copy(carpetaTomada = if (sigueTomada) id else null)) }
}

/** Con una carpeta tomada, la cruceta la mueve (← → un lugar; ↑ ↓ una fila). */
fun Diptico.moverCarpetaTomada(dx: Int, dy: Int, carriles: Int) {
    val e = estado.value
    val id = e.extra.carpetaTomada ?: return
    val horizontal = e.vistaConsolas == "carpetas" && e.carpetasHorizontal
    val paso = if (horizontal) dy + dx * carriles else dx + dy * carriles
    val i = e.pestanas.indexOf(id)
    if (i < 0) return
    soltarCarpetaEn(id, i + paso, sigueTomada = true)
}

fun Diptico.elegirEstiloCarpetas(id: String) {
    almacen.guardarTexto("estiloCarpetas", id)
    actualizar { it.copy(extra = it.extra.copy(estiloCarpetas = id)) }
}

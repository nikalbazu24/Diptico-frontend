package cl.nikalbazu.diptico.estado

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Informe de fluidez: con el medidor encendido, cada segundo se anota cuántos cuadros hubo, el más lento, en qué
 * pantalla y qué se estaba viendo, y cuántas veces se rearmó cada parte grande de la interfaz. Sirve para saber
 * DÓNDE se pone lenta Díptico en la Thor (no se puede medir desde fuera) y se manda con el reporte de fallas.
 */
object Fluidez {
    data class Muestra(val hora: Long, val pantalla: String, val fps: Int, val peorMs: Int, val contexto: String, val rearmados: Map<String, Int>)

    private const val MAX = 600
    private val muestras = ArrayDeque<Muestra>()
    private val contadores = ConcurrentHashMap<String, AtomicInteger>()

    /** Marca que la parte [nombre] de la interfaz se volvió a armar (se llama desde un SideEffect). */
    fun contar(nombre: String) {
        contadores.getOrPut(nombre) { AtomicInteger() }.incrementAndGet()
    }

    /** Qué se estaba viendo: panel, vistas, carpeta abierta, partida… (sin datos personales). */
    fun contexto(e: Estado): String = buildList {
        add(if (e.panel == Panel.NINGUNO) (if (e.enSistemas) "carpetas" else "juegos") else e.panel.name.lowercase())
        if (e.panel == Panel.MENU) add(e.seccion.name.lowercase())
        add("vista=${e.vista}")
        add("consolas=${e.vistaConsolas}")
        if (e.carpetaEnVentana) add("carpeta-abierta")
        if (e.portadaCajas) add("cajas3d")
        if (e.sesion != null) add("jugando")
        if (e.unaPantalla) add("una-pantalla")
    }.joinToString(" ")

    @Synchronized
    fun registrar(pantalla: String, fps: Int, peorMs: Int, contexto: String) {
        // Los contadores son de toda la app: se reparten en la muestra de la pantalla que pide primero el segundo.
        val rearmados = contadores.mapValues { it.value.getAndSet(0) }.filterValues { it > 0 }
        muestras.addLast(Muestra(System.currentTimeMillis(), pantalla, fps, peorMs, contexto, rearmados))
        while (muestras.size > MAX) muestras.removeFirst()
    }

    @Synchronized
    fun cuantas(): Int = muestras.size

    /** El informe en texto: resumen por lo que se estaba viendo (de lo más lento a lo más fluido) y los últimos segundos. */
    @Synchronized
    fun informe(equipo: String): String = buildString {
        appendLine("INFORME DE FLUIDEZ · $equipo · ${muestras.size} s medidos")
        if (muestras.isEmpty()) { appendLine("Sin datos: enciende el medidor y usa Díptico un rato."); return@buildString }
        appendLine()
        appendLine("Por pantalla y contexto (fps promedio · fps mínimo · cuadro más lento · segundos):")
        muestras.filter { it.fps > 0 }.groupBy { it.pantalla + " · " + it.contexto }
            .map { (k, l) -> Triple(k, l, l.map { it.fps }.average()) }
            .sortedBy { it.third }
            .forEach { (k, l, prom) ->
                appendLine("- $k: ${prom.toInt()} · ${l.minOf { it.fps }} · ${l.maxOf { it.peorMs }} ms · ${l.size} s")
            }
        appendLine()
        appendLine("Partes que más se rearman (veces por segundo, promedio):")
        val total = HashMap<String, Int>()
        muestras.forEach { m -> m.rearmados.forEach { (k, v) -> total[k] = (total[k] ?: 0) + v } }
        total.entries.sortedByDescending { it.value }.take(15).forEach { (k, v) -> appendLine("- $k: ${"%.1f".format(Locale.US, v.toFloat() / muestras.size)}") }
        appendLine()
        appendLine("Segundos con tirones (fps < 45), los últimos 40:")
        val f = SimpleDateFormat("HH:mm:ss", Locale.US)
        muestras.filter { it.fps in 1..44 }.takeLast(40).forEach { m ->
            appendLine("${f.format(Date(m.hora))} ${m.pantalla} ${m.fps} fps (peor ${m.peorMs} ms) · ${m.contexto} · " +
                m.rearmados.entries.sortedByDescending { it.value }.take(5).joinToString { "${it.key}×${it.value}" })
        }
    }

    @Synchronized
    fun borrar() { muestras.clear(); contadores.clear() }
}

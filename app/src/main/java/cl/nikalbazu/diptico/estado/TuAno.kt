package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.data.Consolas
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

/**
 * «Tu año»: el resumen de lo que jugaste en el año, para verlo en diciembre (o cuando quieras).
 *
 * Díptico antes solo guardaba el total de horas de cada juego, sin fechas. Desde esta versión anota cada partida
 * (día, juego, segundos y cuántas fueron) en un historial chico, y de ahí salen los números del año. El resumen
 * se muestra junto a los totales de siempre, y dice desde cuándo se está contando.
 */
object TuAno {
    private const val CLAVE = "historialAnio"
    private const val MAX_FILAS = 4000

    /** Un día de un juego: cuántos segundos y cuántas partidas. */
    data class Fila(val fecha: String, val clave: String, val segundos: Long, val partidas: Int)

    private var cache: MutableList<Fila>? = null

    /** Solo para pruebas y capturas: si trae filas, el resumen usa estas en vez del historial real. */
    @Volatile internal var pruebaFilas: List<Fila>? = null

    private fun hoy(t: Long = System.currentTimeMillis()): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(t))

    private fun cargar(): MutableList<Fila> {
        cache?.let { return it }
        val l = ArrayList<Fila>()
        try {
            Diptico.almacen.texto(CLAVE).split('\n').forEach { linea ->
                val p = linea.split('\t')
                if (p.size == 4) {
                    val s = p[2].toLongOrNull()
                    val n = p[3].toIntOrNull()
                    if (s != null && n != null) l += Fila(p[0], p[1], s, n)
                }
            }
        } catch (_: Exception) {
        }
        cache = l
        return l
    }

    private fun guardar(l: List<Fila>) {
        try {
            Diptico.almacen.guardarTexto(CLAVE, l.joinToString("\n") { "${it.fecha}\t${it.clave}\t${it.segundos}\t${it.partidas}" })
        } catch (_: Exception) {
        }
    }

    /** Anota una partida terminada. Las de menos de 15 segundos no cuentan (igual que en el tiempo jugado). */
    fun registrar(clave: String, segundos: Long, cuando: Long = System.currentTimeMillis()) {
        if (segundos < 15) return
        val l = cargar()
        val f = hoy(cuando)
        // Si ya hay una fila de ese día y ese juego (cerca del final), se suma en vez de crear otra.
        val i = (l.size - 1 downTo maxOf(0, l.size - 60)).firstOrNull { l[it].fecha == f && l[it].clave == clave }
        if (i != null) l[i] = l[i].copy(segundos = l[i].segundos + segundos, partidas = l[i].partidas + 1)
        else l += Fila(f, clave, segundos, 1)
        while (l.size > MAX_FILAS) l.removeAt(0)
        guardar(l)
    }

    data class Resumen(
        val anio: Int,
        val segundos: Long,
        val partidas: Int,
        val dias: Int,
        val rachaMax: Int,
        val juegos: Int,
        /** Los juegos más jugados del año: (clave, segundos). */
        val top: List<Pair<String, Long>>,
        /** Segundos de cada mes (12 valores, enero primero). */
        val porMes: List<Long>,
        /** Mes con más tiempo (1 a 12), o 0 si no hay datos. */
        val mejorMes: Int,
        /** La consola con más tiempo en el año (nombre ya bonito), o vacío. */
        val consola: String,
        /** Desde qué día se tienen datos (yyyy-MM-dd), o vacío. */
        val desde: String,
    ) {
        val hayDatos: Boolean get() = segundos > 0
    }

    /** Arma el resumen del año [anio] con el historial. */
    fun resumen(e: Estado, anio: Int = LocalDate.now().year): Resumen = resumenDe(pruebaFilas ?: cargar(), anio) { clave ->
        e.juegosPorClave[clave]?.consolaId?.let { id -> Consolas.porId(id)?.bonito }.orEmpty()
    }

    /** La cuenta en sí, separada para poder probarla sin la app. */
    fun resumenDe(filas: List<Fila>, anio: Int, consolaDe: (String) -> String = { "" }): Resumen {
        val delAnio = filas.filter { it.fecha.startsWith("$anio-") }
        val porJuego = delAnio.groupBy { it.clave }.mapValues { (_, v) -> v.sumOf { it.segundos } }
        val top = porJuego.entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
        val meses = MutableList(12) { 0L }
        delAnio.forEach { f -> f.fecha.substring(5, 7).toIntOrNull()?.takeIf { it in 1..12 }?.let { meses[it - 1] += f.segundos } }
        val mejor = meses.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index?.plus(1) ?: 0
        val fechas = delAnio.map { it.fecha }.toSortedSet()
        val porConsola = HashMap<String, Long>()
        porJuego.forEach { (clave, seg) -> consolaDe(clave).takeIf { it.isNotBlank() }?.let { porConsola[it] = (porConsola[it] ?: 0L) + seg } }
        return Resumen(
            anio = anio,
            segundos = delAnio.sumOf { it.segundos },
            partidas = delAnio.sumOf { it.partidas },
            dias = fechas.size,
            rachaMax = rachaMasLarga(fechas),
            juegos = porJuego.size,
            top = top,
            porMes = meses,
            mejorMes = mejor,
            consola = porConsola.maxByOrNull { it.value }?.key.orEmpty(),
            desde = fechas.firstOrNull().orEmpty(),
        )
    }

    /** La racha de días seguidos más larga entre estas fechas (yyyy-MM-dd). */
    fun rachaMasLarga(fechas: Set<String>): Int {
        var mejor = 0
        var actual = 0
        var anterior: LocalDate? = null
        for (f in fechas.sorted()) {
            val d = try { LocalDate.parse(f) } catch (_: Exception) { continue }
            actual = if (anterior != null && anterior.plusDays(1) == d) actual + 1 else 1
            if (actual > mejor) mejor = actual
            anterior = d
        }
        return mejor
    }

    /** Lo que DIPPO comenta del año (sin números, así se traduce). */
    fun comentario(r: Resumen): String {
        val h = r.segundos / 3600
        return when {
            !r.hayDatos -> "Recién empecé a contar tus partidas. Juega un ratito y vuelve a mirar."
            h >= 100 -> "¡Más de 100 horas! Eres de los que no sueltan el mando."
            h >= 50 -> "¡Qué año de maratones! Se nota que te gusta jugar."
            h >= 10 -> "Buen ritmo: te hiciste tus ratitos para jugar."
            else -> "Un comienzo tranquilo. ¡Todavía queda harto por jugar!"
        }
    }

    /** ¿Ya es hora de avisar que «Tu año» está listo? (de diciembre en adelante, una vez por año). */
    fun tocaAvisar(mes: Int, anio: Int, yaAvisado: String): Boolean = mes == 12 && yaAvisado != anio.toString()

    /** Nombre del mes en el idioma del equipo (1 a 12). */
    fun nombreMes(m: Int): String = java.text.DateFormatSymbols(Locale.getDefault()).months.getOrNull(m - 1).orEmpty()
        .replaceFirstChar { it.uppercase() }
}

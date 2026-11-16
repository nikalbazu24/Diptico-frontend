package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.estado.Diptico.Control
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

/**
 * Widgets útiles (16 nov 2026): deportes en vivo, contadores, tiempo y fechas, y herramientas para jugar con otros.
 * Cada uno guarda sus ajustes en [EstadoExtra.confWidgets] (tipo → clave → valor), que va al almacén «confWidgets».
 */
val WIDGETS_UTILES: List<Pair<String, Pair<Int, Int>>> = listOf(
    "marcador" to (3 to 1), "proximoPartido" to (2 to 2), "tabla" to (3 to 2),
    "contador" to (2 to 2), "marcadorMesa" to (3 to 2), "dados" to (2 to 2),
    "regresiva" to (2 to 2), "nota" to (2 to 2),
    "clima" to (2 to 2), "feriados" to (3 to 2), "horaMundo" to (3 to 1),
)
val TIPOS_UTILES: Set<String> = WIDGETS_UTILES.map { it.first }.toSet()

val UTILES_NOMBRE = mapOf(
    "marcador" to "Marcador en vivo", "proximoPartido" to "Próximo partido", "tabla" to "Tabla de posiciones",
    "contador" to "Contador", "marcadorMesa" to "Marcador para jugar con otros", "dados" to "Dados",
    "regresiva" to "Cuenta regresiva", "nota" to "Nota rápida",
    "clima" to "Clima", "feriados" to "Feriados de Chile", "horaMundo" to "Hora en el mundo",
)
val UTILES_DESCRIPCION = mapOf(
    "marcador" to "El partido de hoy de tu equipo (o de la liga) con el marcador al minuto",
    "proximoPartido" to "Cuándo juega tu equipo, contra quién y cuánto falta",
    "tabla" to "Los primeros de la liga y dónde va tu equipo",
    "contador" to "Suma o resta con un toque: victorias, intentos, vasos de agua…",
    "marcadorMesa" to "Puntos de dos equipos o jugadores para cualquier juego",
    "dados" to "Tira uno, dos o tres dados de 6 o de 20 caras",
    "regresiva" to "Los días que faltan para algo que esperas",
    "nota" to "Una línea a la vista, escrita con el teclado de Díptico",
    "clima" to "Temperatura, lluvia, máxima y mínima de hoy en tu ciudad",
    "feriados" to "El próximo feriado y los que vienen",
    "horaMundo" to "La hora aquí y en otras dos ciudades (para jugar en línea o esperar un estreno)",
)
val UTILES_ICONO = mapOf(
    "marcador" to Icono.TROFEO, "proximoPartido" to Icono.RELOJ, "tabla" to Icono.TROFEO,
    "contador" to Icono.CHECK, "marcadorMesa" to Icono.MANDO, "dados" to Icono.DADO,
    "regresiva" to Icono.RELOJ, "nota" to Icono.LAPIZ,
    "clima" to Icono.BRILLO, "feriados" to Icono.ESTRELLA, "horaMundo" to Icono.MUNDO,
)

/** Categorías de la tienda: cada widget en la suya. */
val CATEGORIAS_WIDGETS = listOf("Para ti", "Deportes", "Contadores", "Tiempo y fechas", "Díptico", "Más")
fun categoriaWidget(tipo: String): String = when {
    tipo in setOf("marcador", "proximoPartido", "tabla") -> "Deportes"
    tipo in setOf("contador", "marcadorMesa", "dados", "temporizador", "cronometro") -> "Contadores"
    tipo in setOf("regresiva", "clima", "feriados", "horaMundo", "reloj", "fiesta") -> "Tiempo y fechas"
    tipo in setOf("nota", "musica", "radio", "accesos", "notificaciones", "bateria", "equipo", "apps", "frase", "buscar") ||
        tipo == "imagen" || tipo == "video" -> "Más"
    else -> "Díptico"
}
/** Para ti: los nuevos y los más usados. */
val PARA_TI = listOf("marcador", "contador", "clima", "regresiva", "proximaPartida", "tuSemana", "dippoPropone", "musica")

// ———————————————————————————— Ajustes de cada widget ————————————————————————————

object ConfWidgets {
    fun leer(s: String): Map<String, Map<String, String>> = runCatching {
        val o = JSONObject(s)
        o.keys().asSequence().associateWith { t -> o.getJSONObject(t).let { c -> c.keys().asSequence().associateWith { c.getString(it) } } }
    }.getOrDefault(emptyMap())
    fun escribir(m: Map<String, Map<String, String>>): String =
        JSONObject().apply { m.forEach { (t, c) -> put(t, JSONObject(c)) } }.toString()
}

fun Estado.conf(tipo: String, clave: String, def: String = ""): String = extra.confWidgets[tipo]?.get(clave)?.takeIf { it.isNotEmpty() } ?: def

fun Diptico.confWidget(tipo: String, cambios: Map<String, String>) {
    val actual = estado.value.extra.confWidgets
    val nuevo = actual + (tipo to (actual[tipo].orEmpty() + cambios))
    almacen.guardarTexto("confWidgets", ConfWidgets.escribir(nuevo))
    actualizar { it.copy(extra = it.extra.copy(confWidgets = nuevo)) }
}
fun Diptico.confWidget(tipo: String, clave: String, valor: String) = confWidget(tipo, mapOf(clave to valor))

/** Lo que se escribe con el teclado para un widget: «wconf:<tipo>:<clave>». */
internal fun Diptico.guardarTextoWidget(para: String, texto: String): Boolean {
    if (!para.startsWith("wconf:")) return false
    val (_, tipo, clave) = para.split(":", limit = 3).let { Triple(it[0], it.getOrElse(1) { "" }, it.getOrElse(2) { "" }) }
    when (clave) {
        "fecha" -> {
            val f = leerFecha(texto) ?: run { avisar("Escribe la fecha así: 18/09/2027"); return true }
            confWidget(tipo, "fecha", f.toString())
        }
        "ciudad" -> confWidget(tipo, mapOf("ciudad" to texto.trim().ifEmpty { "Santiago" }, "lat" to "", "lon" to ""))
        else -> confWidget(tipo, clave, texto.trim())
    }
    return true
}

private fun leerFecha(t: String): LocalDate? = runCatching {
    val p = t.trim().split('/', '-', '.', ' ').filter { it.isNotBlank() }.map { it.toInt() }
    val hoy = LocalDate.now()
    when (p.size) {
        2 -> LocalDate.of(hoy.year, p[1], p[0]).let { if (it.isBefore(hoy)) it.plusYears(1) else it }
        3 -> LocalDate.of(if (p[2] < 100) 2000 + p[2] else p[2], p[1], p[0])
        else -> null
    }
}.getOrNull()

/** Fechas listas para la cuenta regresiva. */
private fun fechasListas(): List<Pair<String, LocalDate>> {
    val hoy = LocalDate.now()
    fun prox(m: Int, d: Int) = LocalDate.of(hoy.year, m, d).let { if (it.isBefore(hoy)) it.plusYears(1) else it }
    return listOf(
        "Fiestas Patrias" to prox(9, 18), "Navidad" to prox(12, 25), "Año Nuevo" to prox(1, 1),
        "Halloween" to prox(10, 31), "Día del Niño" to prox(8, 9),
    )
}

/** Las ciudades listas para el clima y la hora. */
val CIUDADES = listOf("Santiago", "Valparaíso", "Concepción", "La Serena", "Antofagasta", "Temuco", "Puerto Montt", "Punta Arenas", "Arica")
val ZONAS = listOf(
    "Tokio" to "Asia/Tokyo", "Nueva York" to "America/New_York", "Los Ángeles" to "America/Los_Angeles",
    "Madrid" to "Europe/Madrid", "Londres" to "Europe/London", "Ciudad de México" to "America/Mexico_City",
    "Buenos Aires" to "America/Argentina/Buenos_Aires", "Seúl" to "Asia/Seoul", "São Paulo" to "America/Sao_Paulo",
)

/** Los ajustes de cada widget útil, como controles del editor (grupo «Contenido»). */
internal fun Diptico.controlesUtil(e: Estado, tipo: String): List<Control> {
    val g = "Contenido"
    fun chips(titulo: String, clave: String, opciones: List<Pair<String, String>>, def: String): Control {
        val actual = e.conf(tipo, clave, def)
        val k = opciones.indexOfFirst { it.second == actual }.coerceAtLeast(0)
        return Control(titulo, "chips", opciones[k].first, g, elecciones = opciones.map { it.first }, elegida = k,
            elegir = { i -> opciones.getOrNull(i)?.let { confWidget(tipo, clave, it.second) } },
            menos = { confWidget(tipo, clave, opciones[(k - 1).mod(opciones.size)].second) },
        ) { confWidget(tipo, clave, opciones[(k + 1).mod(opciones.size)].second) }
    }
    fun escribir(titulo: String, clave: String, valor: String) = Control(titulo, "accion", valor.ifEmpty { "Escribir" }, g, Icono.LAPIZ,
        ayuda = "Se escribe con el teclado de Díptico.") { abrirTexto("wconf:$tipo:$clave", e.conf(tipo, clave)) }
    return when (tipo) {
        "marcador", "proximoPartido", "tabla" -> {
            val liga = Deportes.liga(e.conf(tipo, "liga", Deportes.LIGAS[0].id))
            val equipos = Deportes.equiposEnCache(liga)
            val l = mutableListOf(chips("Liga", "liga", Deportes.LIGAS.map { it.nombre to it.id }, Deportes.LIGAS[0].id))
            l += Control("Tu equipo", "pasos", e.conf(tipo, "equipoNombre", if (tipo == "proximoPartido") "Elige uno" else "Toda la liga"), g, Icono.CORAZON,
                ayuda = if (equipos.isEmpty()) "Cargando los equipos de la liga…" else "${equipos.size} equipos en la liga.",
                menos = { cambiarEquipo(tipo, liga, -1) }) { cambiarEquipo(tipo, liga, 1) }
            if (equipos.isEmpty()) Deportes.cargarEquiposLuego(liga)
            l
        }
        "contador" -> listOf(
            escribir("Nombre", "nombre", e.conf(tipo, "nombre", "Contador")),
            chips("Suma de a", "paso", listOf("1" to "1", "5" to "5", "10" to "10"), "1"),
            chips("Vuelve a cero", "reinicio", listOf("Nunca" to "nunca", "Cada día" to "dia", "Cada semana" to "semana"), "nunca"),
            Control("Poner en cero", "accion", "A", g, Icono.BORRAR) { confWidget(tipo, "valor", "0"); avisar("Contador en cero") },
        )
        "marcadorMesa" -> listOf(
            escribir("Lado izquierdo", "a", e.conf(tipo, "a", "Equipo 1")),
            escribir("Lado derecho", "b", e.conf(tipo, "b", "Equipo 2")),
            Control("Poner en cero", "accion", "A", g, Icono.BORRAR) { confWidget(tipo, mapOf("pa" to "0", "pb" to "0")) },
        )
        "dados" -> listOf(
            chips("Cuántos", "cantidad", listOf("Uno" to "1", "Dos" to "2", "Tres" to "3"), "2"),
            chips("Caras", "caras", listOf("6 caras" to "6", "20 caras" to "20"), "6"),
        )
        "regresiva" -> {
            val listas = fechasListas()
            listOf(
                chips("Para", "lista", listas.map { it.first to it.first } + ("Mi fecha" to "propia"), "Fiestas Patrias"),
                Control("Mi fecha", "accion", e.conf(tipo, "fecha").ifEmpty { "Escribir" }, g, Icono.RELOJ,
                    ayuda = "Por ejemplo 14/12 o 14/12/2027.") { confWidget(tipo, "lista", "propia"); abrirTexto("wconf:$tipo:fecha") },
                escribir("Texto", "texto", e.conf(tipo, "texto")),
            )
        }
        "nota" -> listOf(escribir("Tu nota", "texto", e.conf(tipo, "texto")))
        "clima" -> listOf(
            chips("Ciudad", "ciudad", (CIUDADES + listOf(e.conf(tipo, "ciudad", "Santiago")).filter { it !in CIUDADES }).map { it to it }, "Santiago"),
            escribir("Escribir otra ciudad", "ciudad", ""),
        )
        "horaMundo" -> listOf(
            chips("Ciudad 1", "zona1", ZONAS.map { it.first to it.second }, "Asia/Tokyo"),
            chips("Ciudad 2", "zona2", ZONAS.map { it.first to it.second }, "America/New_York"),
        )
        else -> emptyList()
    }
}

private fun Diptico.cambiarEquipo(tipo: String, liga: Deportes.Liga, d: Int) {
    val equipos = Deportes.equiposEnCache(liga)
    if (equipos.isEmpty()) { Deportes.cargarEquiposLuego(liga); avisar("Cargando los equipos…"); return }
    val conTodos = if (tipo == "proximoPartido") equipos else listOf(Deportes.Equipo("", "Toda la liga", "", "")) + equipos
    val actual = estado.value.conf(tipo, "equipo")
    val k = (conTodos.indexOfFirst { it.id == actual }.coerceAtLeast(0) + d).mod(conTodos.size)
    confWidget(tipo, mapOf("equipo" to conTodos[k].id, "equipoNombre" to conTodos[k].nombre))
}

/** Fecha objetivo de la cuenta regresiva y su texto. */
fun Estado.regresiva(): Pair<String, LocalDate> {
    val lista = conf("regresiva", "lista", "Fiestas Patrias")
    val texto = conf("regresiva", "texto")
    if (lista == "propia") {
        val f = runCatching { LocalDate.parse(conf("regresiva", "fecha")) }.getOrNull() ?: LocalDate.now().plusDays(30)
        return texto.ifEmpty { "para tu fecha" } to f
    }
    val (n, f) = fechasListas().firstOrNull { it.first == lista } ?: fechasListas()[0]
    return texto.ifEmpty { "para $n" } to f
}

// ———————————————————————————— Lecturas de internet ————————————————————————————

/** Una lectura de internet con un pequeño recuerdo (así el widget y su vista previa no piden lo mismo dos veces). */
internal object Red {
    private val recuerdo = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, String>>()
    fun texto(url: String, vigencia: Long = 45_000L): String? {
        recuerdo[url]?.let { (t, s) -> if (System.currentTimeMillis() - t < vigencia) return s }
        return runCatching {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 8000; c.readTimeout = 10000
            c.setRequestProperty("User-Agent", "Diptico/1.1 (Android)")
            c.inputStream.bufferedReader().use { it.readText() }.also { recuerdo[url] = System.currentTimeMillis() to it }
        }.getOrNull()
    }
}

/** Deportes: los datos públicos que muestra ESPN (sin cuenta). Pueden cambiar sin aviso: si fallan, el widget lo dice. */
object Deportes {
    data class Liga(val id: String, val deporte: String, val nombre: String)
    data class Equipo(val id: String, val nombre: String, val sigla: String, val color: String, val marcador: String = "")
    data class Partido(val local: Equipo, val visita: Equipo, val estado: String, val detalle: String, val fecha: Long) {
        val enVivo get() = estado == "in"
        val terminado get() = estado == "post"
    }
    data class Fila(val pos: Int, val equipo: Equipo, val valor: String)

    val LIGAS = listOf(
        Liga("chi.1", "soccer", "Primera División (Chile)"), Liga("conmebol.libertadores", "soccer", "Copa Libertadores"),
        Liga("conmebol.sudamericana", "soccer", "Copa Sudamericana"), Liga("uefa.champions", "soccer", "Champions League"),
        Liga("eng.1", "soccer", "Premier League"), Liga("esp.1", "soccer", "LaLiga"), Liga("arg.1", "soccer", "Liga Argentina"),
        Liga("nba", "basketball", "NBA"), Liga("nfl", "football", "NFL"), Liga("mlb", "baseball", "MLB"),
    )
    fun liga(id: String) = LIGAS.firstOrNull { it.id == id } ?: LIGAS[0]
    private fun base(l: Liga) = "https://site.api.espn.com/apis/site/v2/sports/${l.deporte}/${l.id}"

    private val equipos = java.util.concurrent.ConcurrentHashMap<String, List<Equipo>>()
    fun equiposEnCache(l: Liga): List<Equipo> = equipos[l.id].orEmpty()
    fun cargarEquiposLuego(l: Liga) {
        if (equipos.containsKey(l.id)) return
        Thread {
            val lista = runCatching {
                val o = JSONObject(Red.texto("${base(l)}/teams", 86_400_000L) ?: return@Thread)
                val t = o.getJSONArray("sports").getJSONObject(0).getJSONArray("leagues").getJSONObject(0).getJSONArray("teams")
                (0 until t.length()).map { equipo(t.getJSONObject(it).getJSONObject("team")) }.sortedBy { it.nombre }
            }.getOrNull() ?: return@Thread
            equipos[l.id] = lista
            Diptico.actualizar { it.copy(extra = it.extra.copy(confWidgets = it.extra.confWidgets.toMap())) }
        }.apply { isDaemon = true }.start()
    }

    private fun equipo(t: JSONObject, marcador: String = "") = Equipo(
        t.optString("id"), t.optString("shortDisplayName").ifEmpty { t.optString("displayName") },
        t.optString("abbreviation"), t.optString("color"), marcador,
    )

    private fun partido(ev: JSONObject): Partido? = runCatching {
        val comp = ev.getJSONArray("competitions").getJSONObject(0)
        val cs = comp.getJSONArray("competitors")
        fun lado(k: Int): Equipo {
            val c = cs.getJSONObject(k)
            val s = c.opt("score")
            val m = when (s) { is JSONObject -> s.optString("displayValue"); null -> ""; else -> s.toString() }
            return equipo(c.getJSONObject("team"), m)
        }
        val (iL, iV) = if (cs.getJSONObject(0).optString("homeAway") == "home") 0 to 1 else 1 to 0
        val st = (comp.optJSONObject("status") ?: ev.getJSONObject("status")).getJSONObject("type")
        Partido(lado(iL), lado(iV), st.optString("state"), st.optString("shortDetail"),
            runCatching { Instant.parse(ev.getString("date").let { if (it.length == 17) it.dropLast(1) + ":00Z" else it }).toEpochMilli() }.getOrDefault(0L))
    }.getOrNull()

    /** Partidos de hoy en la liga (los en vivo primero). */
    fun hoy(l: Liga): List<Partido>? {
        val o = JSONObject(Red.texto("${base(l)}/scoreboard") ?: return null)
        val evs = o.optJSONArray("events") ?: JSONArray()
        return (0 until evs.length()).mapNotNull { partido(evs.getJSONObject(it)) }
            .sortedWith(compareBy({ if (it.enVivo) 0 else if (it.terminado) 2 else 1 }, { it.fecha }))
    }

    /** El calendario de un equipo (partidos jugados y por jugar). */
    fun calendario(l: Liga, equipo: String): List<Partido>? {
        val o = JSONObject(Red.texto("${base(l)}/teams/$equipo/schedule", 600_000L) ?: return null)
        val evs = o.optJSONArray("events") ?: JSONArray()
        return (0 until evs.length()).mapNotNull { partido(evs.getJSONObject(it)) }.sortedBy { it.fecha }
    }

    /** El partido que importa del equipo: el de hoy si hay; si no, el próximo. */
    fun deEquipo(l: Liga, equipo: String): Partido? {
        hoy(l)?.firstOrNull { it.local.id == equipo || it.visita.id == equipo }?.let { return it }
        val ahora = System.currentTimeMillis()
        return calendario(l, equipo)?.firstOrNull { it.fecha > ahora - 3 * 3_600_000L && !it.terminado }
    }

    fun tabla(l: Liga): List<Fila>? {
        val o = JSONObject(Red.texto("https://site.api.espn.com/apis/v2/sports/${l.deporte}/${l.id}/standings", 3_600_000L) ?: return null)
        val grupos = o.optJSONArray("children")?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } } ?: listOf(o)
        return grupos.flatMap { g ->
            val en = g.optJSONObject("standings")?.optJSONArray("entries") ?: return@flatMap emptyList()
            (0 until en.length()).map { k ->
                val x = en.getJSONObject(k)
                val stats = x.optJSONArray("stats") ?: JSONArray()
                fun stat(n: String) = (0 until stats.length()).map { stats.getJSONObject(it) }.firstOrNull { it.optString("name") == n }?.optString("displayValue")
                val valor = stat("points")?.let { "$it pts" } ?: listOfNotNull(stat("wins"), stat("losses")).joinToString("-")
                Fila(stat("rank")?.toIntOrNull() ?: (k + 1), equipo(x.getJSONObject("team")), valor)
            }.sortedBy { it.pos }
        }
    }
}

/** El clima de Open-Meteo (gratis, sin cuenta). */
object Clima {
    data class Hoy(val ciudad: String, val grados: Int, val codigo: Int, val dia: Boolean, val max: Int, val min: Int, val lluvia: Int)

    fun de(ciudad: String): Hoy? {
        val geo = JSONObject(Red.texto("https://geocoding-api.open-meteo.com/v1/search?count=1&language=es&name=" +
            URLEncoder.encode(ciudad, "UTF-8"), 86_400_000L) ?: return null)
        val r = geo.optJSONArray("results")?.optJSONObject(0) ?: return null
        val lat = r.getDouble("latitude"); val lon = r.getDouble("longitude")
        val o = JSONObject(Red.texto("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max&forecast_days=1&timezone=auto", 900_000L) ?: return null)
        val c = o.getJSONObject("current"); val d = o.getJSONObject("daily")
        return Hoy(r.optString("name", ciudad), c.getDouble("temperature_2m").toInt(), c.getInt("weather_code"), c.optInt("is_day", 1) == 1,
            d.getJSONArray("temperature_2m_max").getDouble(0).toInt(), d.getJSONArray("temperature_2m_min").getDouble(0).toInt(),
            d.getJSONArray("precipitation_probability_max").optInt(0, 0))
    }

    fun texto(codigo: Int): String = when (codigo) {
        0 -> "Despejado"; 1, 2 -> "Parcial"; 3 -> "Nublado"; 45, 48 -> "Neblina"
        in 51..57 -> "Llovizna"; in 61..67, in 80..82 -> "Lluvia"; in 71..77, 85, 86 -> "Nieve"; in 95..99 -> "Tormenta"
        else -> "—"
    }
}

/** Feriados de Chile: la lista oficial del Gobierno Digital; sin internet, los de fecha fija. */
object Feriados {
    data class Feriado(val fecha: LocalDate, val nombre: String)

    private val FIJOS = listOf(
        1 to 1 to "Año Nuevo", 5 to 1 to "Día del Trabajo", 5 to 21 to "Glorias Navales", 7 to 16 to "Virgen del Carmen",
        8 to 15 to "Asunción de la Virgen", 9 to 18 to "Independencia Nacional", 9 to 19 to "Glorias del Ejército",
        11 to 1 to "Todos los Santos", 12 to 8 to "Inmaculada Concepción", 12 to 25 to "Navidad",
    )

    fun proximos(n: Int = 6): List<Feriado> {
        val hoy = LocalDate.now(ZoneId.systemDefault())
        val todos = (hoy.year..hoy.year + 1).flatMap { anio ->
            val web = runCatching {
                val a = JSONArray(Red.texto("https://apis.digital.gob.cl/fl/feriados/$anio", 86_400_000L) ?: error("sin red"))
                (0 until a.length()).map { a.getJSONObject(it) }.map { Feriado(LocalDate.parse(it.getString("fecha")), it.getString("nombre")) }
            }.getOrNull()
            web ?: FIJOS.map { (md, nombre) -> Feriado(LocalDate.of(anio, md.first, md.second), nombre) }
        }
        return todos.filter { !it.fecha.isBefore(hoy) }.distinctBy { it.fecha }.sortedBy { it.fecha }.take(n)
    }
}

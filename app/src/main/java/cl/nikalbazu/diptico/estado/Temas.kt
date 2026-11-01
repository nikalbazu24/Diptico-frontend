package cl.nikalbazu.diptico.estado

import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Un tema de colores. Los colores van como números (0xFFRRGGBB) para no depender de la interfaz.
 * [base]: "auto" (sigue claro/oscuro), "oscuro", "claro" u "oled" (negro puro).
 * [decor]: decoración animada encima de la pantalla.
 */
data class Tema(
    val id: String,
    val nombre: String,
    val acento: Long,
    val acento2: Long,
    val acento3: Long,
    val base: String = "auto",
    val decor: String = "nada",
    val mensaje: String = "",
    val propio: Boolean = false,
    /** Color del fondo y de las letras (0 = el automático, según el color principal). */
    val fondo: Long = 0L,
    val tinta: Long = 0L,
    /** Estilo completo: "pixel" usa letra de píxeles; forma "pixel" hace las esquinas escalonadas; sonidos "bloques". */
    val fuente: String = "",
    val forma: String = "",
    val sonidos: String = "",
) {
    fun json(): JSONObject = JSONObject()
        .put("id", id).put("nombre", nombre).put("acento", acento).put("acento2", acento2).put("acento3", acento3)
        .put("base", base).put("decor", decor).put("mensaje", mensaje).put("propio", propio)
        .put("fondo", fondo).put("tinta", tinta).put("fuente", fuente).put("forma", forma).put("sonidos", sonidos)

    companion object {
        fun deJson(o: JSONObject) = Tema(
            o.getString("id"), o.getString("nombre"), o.getLong("acento"), o.getLong("acento2"), o.getLong("acento3"),
            o.optString("base", "auto"), o.optString("decor", "nada"), o.optString("mensaje", ""), o.optBoolean("propio", true),
            o.optLong("fondo", 0L), o.optLong("tinta", 0L),
            o.optString("fuente", ""), o.optString("forma", ""), o.optString("sonidos", ""),
        )
    }
}

/** Una celebración del mundo, con su tema y las fechas en que se activa. */
data class Fiesta(val id: String, val nombre: String, val donde: String, val tema: Tema, val paises: Set<String> = emptySet())

object Temas {
    val DECORACIONES = listOf("nada", "nieve", "hojas", "murcielagos", "petalos", "confeti", "corazones", "estrellas", "faroles", "burbujas", "fuegos", "bloques")

    fun nombreDecor(d: String) = when (d) {
        "nieve" -> "Nieve"; "hojas" -> "Hojas"; "murcielagos" -> "Murciélagos"; "petalos" -> "Pétalos"
        "confeti" -> "Confeti"; "corazones" -> "Corazones"; "estrellas" -> "Estrellas"; "faroles" -> "Faroles"
        "burbujas" -> "Burbujas"; "fuegos" -> "Fuegos artificiales"; "bloques" -> "Bloques que caen"; else -> "Sin decoración"
    }

    /** Colores para armar temas propios. */
    val MUESTRAS = listOf(
        0xFF6B4EFF, 0xFFFF7A59, 0xFFFFC93C, 0xFF3BA8FF, 0xFFFF5D8F, 0xFF2EC4A6, 0xFFE63946, 0xFF8BC34A,
        0xFF00E5FF, 0xFFFF2E97, 0xFFB388FF, 0xFFF97316, 0xFF14B8A6, 0xFFFACC15, 0xFF94A3B8, 0xFFFFFFFF,
    )

    val incluidos = listOf(
        Tema("diptico", "Díptico", 0xFF6B4EFF, 0xFFFF7A59, 0xFFFFC93C),
        // Mundo de bloques: pasto, tierra y diamante; letra de píxeles, esquinas escalonadas, sonidos de madera y piano tranquilo.
        Tema(
            "bloques", "Mundo de bloques", 0xFF62B33F, 0xFFA0703F, 0xFF5CE1E6, "oscuro", "bloques",
            fondo = 0xFF1A1714, tinta = 0xFFF4ECDA, fuente = "pixel", forma = "pixel", sonidos = "bloques",
        ),
        Tema("medianoche", "Medianoche", 0xFF3B82F6, 0xFF22D3EE, 0xFFA78BFA, "oscuro", "estrellas"),
        Tema("atardecer", "Atardecer", 0xFFFF7A59, 0xFFFF5D8F, 0xFFFFC93C),
        Tema("sakura", "Sakura", 0xFFFF8FB1, 0xFFB388FF, 0xFFFFD6E0, decor = "petalos"),
        Tema("arcade", "Arcade neón", 0xFFFF2E97, 0xFF00E5FF, 0xFFFFE600, "oscuro"),
        Tema("oceano", "Océano", 0xFF0EA5E9, 0xFF14B8A6, 0xFF38BDF8, decor = "burbujas"),
        Tema("bosque", "Bosque", 0xFF2EC4A6, 0xFFFFB020, 0xFF8BC34A, decor = "hojas"),
        Tema("lava", "Lava", 0xFFEF4444, 0xFFF97316, 0xFFFACC15, "oscuro"),
        Tema("portatil", "Portátil clásica", 0xFF8BAC0F, 0xFF306230, 0xFF9BBC0F, "gb"),
        Tema("papel", "Papel", 0xFF3D5A80, 0xFFE07A5F, 0xFFF2CC8F, "claro"),
        Tema("oled", "Negro puro", 0xFF6B4EFF, 0xFFFF7A59, 0xFFFFC93C, "oled"),
        Tema("chile", "Chile", 0xFFD52B1E, 0xFF0039A6, 0xFFFFFFFF, "oscuro", "estrellas"),
        Tema("cyber", "Cyberpunk", 0xFFF000FF, 0xFF00F0FF, 0xFFFFF200, "oled"),
        Tema("menta", "Menta", 0xFF10B981, 0xFF34D399, 0xFFA7F3D0),
        Tema("lavanda", "Lavanda", 0xFFA78BFA, 0xFFF0ABFC, 0xFFC4B5FD, decor = "petalos"),
        Tema("dorado", "Dorado", 0xFFD4AF37, 0xFFF59E0B, 0xFFFDE68A, "oscuro", "estrellas"),
        Tema("hielo", "Hielo", 0xFF7DD3FC, 0xFFBAE6FD, 0xFFE0F2FE, decor = "nieve"),
        Tema("fuego", "Fuego", 0xFFFF4500, 0xFFFF8C00, 0xFFFFD700, "oscuro"),
        Tema("monocromo", "Monocromo", 0xFFE5E5E5, 0xFF9CA3AF, 0xFFFFFFFF, "oled"),
        Tema("cereza", "Cereza", 0xFFBE123C, 0xFFF43F5E, 0xFFFDA4AF),
        Tema("desierto", "Desierto de Atacama", 0xFFD97706, 0xFFB45309, 0xFF7DD3FC, "oscuro", "estrellas"),
        Tema("patagonia", "Patagonia", 0xFF0EA5E9, 0xFF64748B, 0xFFE0F2FE, decor = "nieve"),
        Tema("retro80", "Retro 80", 0xFFFF6EC7, 0xFF7B61FF, 0xFF00E5FF, "oscuro"),
    )

    // ---------- Celebraciones del mundo ----------

    private fun f(id: String, nombre: String, donde: String, a: Long, b: Long, c: Long, decor: String, mensaje: String, base: String = "auto", paises: Set<String> = emptySet()) =
        Fiesta(id, nombre, donde, Tema("fiesta_$id", nombre, a, b, c, base, decor, mensaje), paises)

    val fiestas = listOf(
        f("anionuevo", "Año Nuevo", "Todo el mundo", 0xFFFFC93C, 0xFFFF5D8F, 0xFF3BA8FF, "fuegos", "¡Feliz año nuevo!", "oscuro"),
        f("reyes", "Día de Reyes", "España y Latinoamérica", 0xFFFFC93C, 0xFF8E6CF0, 0xFFFF7A59, "estrellas", "¡Feliz Día de Reyes!"),
        f("lunar", "Año Nuevo Lunar", "China, Corea, Vietnam y más", 0xFFE63946, 0xFFFFC93C, 0xFFFF7A59, "faroles", "¡Feliz Año Nuevo Lunar!", "oscuro"),
        f("sanvalentin", "San Valentín", "Todo el mundo", 0xFFFF5D8F, 0xFFE63946, 0xFFFFD6E0, "corazones", "¡Feliz día del amor y la amistad!"),
        f("carnaval", "Carnaval", "Brasil, Bolivia y el mundo", 0xFF2EC4A6, 0xFFFFC93C, 0xFF8E6CF0, "confeti", "¡Feliz Carnaval!"),
        f("holi", "Holi", "India y Nepal", 0xFFFF2E97, 0xFFFFE600, 0xFF00E5FF, "confeti", "¡Feliz Holi, fiesta de los colores!"),
        f("mujer", "Día de la Mujer", "Todo el mundo", 0xFF8E6CF0, 0xFFFF5D8F, 0xFFFFC93C, "petalos", "Día Internacional de la Mujer"),
        f("sanpatricio", "San Patricio", "Irlanda y el mundo", 0xFF16A34A, 0xFFFFC93C, 0xFF8BC34A, "hojas", "¡Feliz San Patricio!"),
        f("eid", "Eid al-Fitr", "Países musulmanes", 0xFF14B8A6, 0xFFFFC93C, 0xFF3BA8FF, "faroles", "¡Eid Mubarak!", "oscuro"),
        f("hanami", "Hanami", "Japón", 0xFFFF8FB1, 0xFFB388FF, 0xFFFFD6E0, "petalos", "Temporada de flores de cerezo"),
        f("pascua", "Pascua", "Todo el mundo", 0xFFB388FF, 0xFF8BC34A, 0xFFFFD6E0, "petalos", "¡Felices Pascuas!"),
        f("songkran", "Songkran", "Tailandia", 0xFF0EA5E9, 0xFF22D3EE, 0xFFFFC93C, "burbujas", "¡Feliz Songkran!"),
        f("tierra", "Día de la Tierra", "Todo el mundo", 0xFF2EC4A6, 0xFF3BA8FF, 0xFF8BC34A, "hojas", "Día de la Tierra"),
        f("orgullo", "Mes del Orgullo", "Todo el mundo", 0xFFFF2E97, 0xFF3BA8FF, 0xFFFFE600, "confeti", "Mes del Orgullo"),
        f("andino", "We Tripantu e Inti Raymi", "Chile, Perú y Bolivia", 0xFFFFB020, 0xFFE63946, 0xFF3BA8FF, "estrellas", "¡Feliz año nuevo de los pueblos originarios!", "oscuro"),
        f("gamer", "Día del Gamer", "Latinoamérica", 0xFF00E5FF, 0xFFFF2E97, 0xFFFFE600, "estrellas", "¡Feliz Día del Gamer!", "oscuro"),
        f("fiestaspatrias", "Fiestas Patrias", "Chile", 0xFFD52B1E, 0xFF0039A6, 0xFFFFFFFF, "confeti", "¡Felices Fiestas Patrias!", paises = setOf("CL")),
        f("independencia", "Fiestas de Independencia", "México y Centroamérica", 0xFF006847, 0xFFCE1126, 0xFFFFFFFF, "confeti", "¡Viva la independencia!"),
        f("oktoberfest", "Oktoberfest", "Alemania", 0xFFF59E0B, 0xFF3BA8FF, 0xFFFFFFFF, "burbujas", "¡Prost!"),
        f("diwali", "Diwali", "India y el mundo", 0xFFF97316, 0xFFFFC93C, 0xFFE63946, "faroles", "¡Feliz Diwali, fiesta de las luces!", "oscuro"),
        f("halloween", "Halloween", "Todo el mundo", 0xFFFF7A1A, 0xFF7B2CBF, 0xFF8BC34A, "murcielagos", "¡Feliz Halloween!", "oscuro"),
        f("muertos", "Día de Muertos", "México y Latinoamérica", 0xFFFF9F1C, 0xFFFF5D8F, 0xFF8E6CF0, "petalos", "Día de Muertos", "oscuro"),
        f("gracias", "Acción de Gracias", "Estados Unidos y Canadá", 0xFFD97706, 0xFF92400E, 0xFFFFC93C, "hojas", "¡Feliz Día de Acción de Gracias!"),
        f("janucá", "Janucá", "Comunidades judías", 0xFF3B82F6, 0xFF94A3B8, 0xFFFFC93C, "estrellas", "¡Jag Janucá Sameaj!", "oscuro"),
        f("navidad", "Navidad", "Todo el mundo", 0xFFE63946, 0xFF2A9D8F, 0xFFFFC93C, "nieve", "¡Feliz Navidad!", "oscuro"),
        // Calendario de Chile
        f("trabajador", "Día del Trabajador", "Chile y el mundo", 0xFFE63946, 0xFFFFC93C, 0xFF3BA8FF, "confeti", "¡Feliz Día del Trabajador!"),
        f("glorias", "Glorias Navales", "Chile", 0xFF0B3D91, 0xFFFFFFFF, 0xFFD52B1E, "burbujas", "21 de mayo · Glorias Navales", "oscuro", setOf("CL")),
        f("patrimonio", "Día del Patrimonio", "Chile", 0xFFB45309, 0xFF92400E, 0xFFFDE68A, "hojas", "¡Feliz Día del Patrimonio!", paises = setOf("CL")),
        f("sanpedro", "San Pedro y San Pablo", "Chile y Latinoamérica", 0xFF0EA5E9, 0xFF1E3A8A, 0xFFFFC93C, "burbujas", "¡Feliz San Pedro y San Pablo!"),
        f("tirana", "La Tirana", "Norte de Chile", 0xFFE11D48, 0xFFFFC93C, 0xFF7C3AED, "confeti", "¡Viva La Tirana!", "oscuro", setOf("CL")),
        f("nino", "Día del Niño", "Chile", 0xFF3BA8FF, 0xFFFFC93C, 0xFFFF5D8F, "burbujas", "¡Feliz Día del Niño!", paises = setOf("CL")),
        f("dosmundos", "Encuentro de Dos Mundos", "Chile y América", 0xFFB45309, 0xFF2EC4A6, 0xFFFFC93C, "hojas", "12 de octubre", paises = setOf("CL")),
        f("musica", "Día de la Música", "Chile y el mundo", 0xFF8E6CF0, 0xFFFF5D8F, 0xFFFFC93C, "estrellas", "¡Feliz Día de la Música!"),
    )

    /** Domingo de Pascua (algoritmo de Gauss/Meeus). */
    private fun pascua(anio: Int): LocalDate {
        val a = anio % 19; val b = anio / 100; val c = anio % 100; val d = b / 4; val e = b % 4
        val f = (b + 8) / 25; val g = (b - f + 1) / 3; val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4; val k = c % 4; val l = (32 + 2 * e + 2 * i - h - k) % 7; val m = (a + 11 * h + 22 * l) / 451
        val mes = (h + l - 7 * m + 114) / 31; val dia = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(anio, mes, dia)
    }

    // Fiestas con calendario lunar (fechas aproximadas).
    private val LUNAR = mapOf(2025 to "01-29", 2026 to "02-17", 2027 to "02-06", 2028 to "01-26", 2029 to "02-13", 2030 to "02-03")
    private val HOLI = mapOf(2025 to "03-14", 2026 to "03-04", 2027 to "03-22", 2028 to "03-11", 2029 to "03-01", 2030 to "03-20")
    private val EID = mapOf(2025 to "03-30", 2026 to "03-20", 2027 to "03-10", 2028 to "02-27", 2029 to "02-14", 2030 to "02-04")
    private val DIWALI = mapOf(2025 to "10-20", 2026 to "11-08", 2027 to "10-29", 2028 to "10-17", 2029 to "11-05", 2030 to "10-26")
    private val JANUCA = mapOf(2025 to "12-14", 2026 to "12-04", 2027 to "12-24", 2028 to "12-12", 2029 to "12-01", 2030 to "12-20")

    private fun tabla(t: Map<Int, String>, anio: Int): LocalDate? = t[anio]?.let { LocalDate.parse("$anio-$it") }
    private fun d(anio: Int, mes: Int, dia: Int) = LocalDate.of(anio, mes, dia)

    /** Días en que se activa cada fiesta, para un año. */
    private fun rangos(anio: Int): List<Pair<String, ClosedRange<LocalDate>>> {
        val p = pascua(anio)
        val gracias = d(anio, 11, 1).with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY))
        val lista = mutableListOf(
            "anionuevo" to (d(anio, 1, 1)..d(anio, 1, 2)),
            "anionuevo" to (d(anio, 12, 30)..d(anio, 12, 31)),
            "reyes" to (d(anio, 1, 5)..d(anio, 1, 6)),
            "sanvalentin" to (d(anio, 2, 10)..d(anio, 2, 14)),
            "carnaval" to (p.minusDays(51)..p.minusDays(46)),
            "mujer" to (d(anio, 3, 8)..d(anio, 3, 8)),
            "sanpatricio" to (d(anio, 3, 16)..d(anio, 3, 17)),
            "hanami" to (d(anio, 3, 25)..d(anio, 4, 8)),
            "pascua" to (p.minusDays(3)..p.plusDays(1)),
            "songkran" to (d(anio, 4, 13)..d(anio, 4, 15)),
            "tierra" to (d(anio, 4, 22)..d(anio, 4, 22)),
            "orgullo" to (d(anio, 6, 1)..d(anio, 6, 20)),
            "andino" to (d(anio, 6, 21)..d(anio, 6, 24)),
            "gamer" to (d(anio, 8, 29)..d(anio, 8, 29)),
            "fiestaspatrias" to (d(anio, 9, 14)..d(anio, 9, 20)),
            "independencia" to (d(anio, 9, 14)..d(anio, 9, 16)),
            "oktoberfest" to (d(anio, 9, 21)..d(anio, 10, 5)),
            "halloween" to (d(anio, 10, 24)..d(anio, 10, 31)),
            "muertos" to (d(anio, 11, 1)..d(anio, 11, 2)),
            "gracias" to (gracias.minusDays(1)..gracias.plusDays(1)),
            "navidad" to (d(anio, 12, 15)..d(anio, 12, 26)),
            "trabajador" to (d(anio, 5, 1)..d(anio, 5, 1)),
            "glorias" to (d(anio, 5, 21)..d(anio, 5, 21)),
            "sanpedro" to (d(anio, 6, 29)..d(anio, 6, 29)),
            "tirana" to (d(anio, 7, 14)..d(anio, 7, 16)),
            "dosmundos" to (d(anio, 10, 12)..d(anio, 10, 12)),
            "musica" to (d(anio, 11, 22)..d(anio, 11, 22)),
        )
        // Día del Patrimonio: último fin de semana de mayo. Día del Niño: segundo domingo de agosto.
        val finMayo = d(anio, 5, 31).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        lista += "patrimonio" to (finMayo.minusDays(1)..finMayo)
        val nino = d(anio, 8, 1).with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.SUNDAY))
        lista += "nino" to (nino.minusDays(1)..nino)
        tabla(LUNAR, anio)?.let { lista += "lunar" to (it.minusDays(2)..it.plusDays(6)) }
        tabla(HOLI, anio)?.let { lista += "holi" to (it.minusDays(1)..it.plusDays(1)) }
        tabla(EID, anio)?.let { lista += "eid" to (it.minusDays(1)..it.plusDays(2)) }
        tabla(DIWALI, anio)?.let { lista += "diwali" to (it.minusDays(2)..it.plusDays(2)) }
        tabla(JANUCA, anio)?.let { lista += "janucá" to (it..it.plusDays(7)) }
        return lista
    }

    /** La celebración de hoy (si hay varias, prefiere la del país de la persona). */
    fun deHoy(hoy: LocalDate = LocalDate.now()): Fiesta? {
        val pais = Locale.getDefault().country
        val ids = (rangos(hoy.year) + rangos(hoy.year - 1).filter { it.second.endInclusive.year == hoy.year })
            .filter { hoy in it.second }.map { it.first }.distinct()
        val candidatas = ids.mapNotNull { id -> fiestas.firstOrNull { it.id == id } }
        return candidatas.firstOrNull { pais in it.paises } ?: candidatas.firstOrNull { it.paises.isEmpty() } ?: candidatas.firstOrNull()
    }

    /** La próxima celebración y cuándo empieza. */
    fun proxima(hoy: LocalDate = LocalDate.now()): Pair<Fiesta, LocalDate>? =
        (rangos(hoy.year) + rangos(hoy.year + 1))
            .filter { it.second.start.isAfter(hoy) }
            .minByOrNull { it.second.start }
            ?.let { (id, r) -> fiestas.firstOrNull { it.id == id }?.let { it to r.start } }

    /** Las próximas [n] celebraciones (sin repetir) y cuándo empiezan. */
    fun proximas(n: Int, hoy: LocalDate = LocalDate.now()): List<Pair<Fiesta, LocalDate>> =
        (rangos(hoy.year) + rangos(hoy.year + 1))
            .filter { it.second.start.isAfter(hoy) }
            .sortedBy { it.second.start }
            .distinctBy { it.first }
            .mapNotNull { (id, r) -> fiestas.firstOrNull { it.id == id }?.let { it to r.start } }
            .take(n)

    // ---------- Temas creados con IA ----------

    /**
     * Pide a la IA un tema nuevo. [pedido] puede ser "Halloween en Japón" o vacío (elige la celebración más cercana del mundo).
     * Usa la IA en la nube que la persona tenga conectada (Gemini, ChatGPT o Claude).
     */
    fun conIA(llave: Llave, pedido: String, hoy: LocalDate = LocalDate.now()): Tema {
        val tarea = pedido.ifBlank {
            "la celebración más importante del mundo que ocurra cerca de la fecha $hoy (piensa en todos los países, no solo en uno)"
        }
        val prompt = """
            Diseña un tema de colores para Díptico, un launcher de videojuegos con fondo oscuro, inspirado en: $tarea.
            Responde SOLO con un objeto JSON, sin texto extra, con estas claves:
            "nombre" (máximo 24 caracteres, en español), "acento", "acento2", "acento3" (colores hex #RRGGBB vivos y que se lean bien sobre fondo oscuro),
            "base" ("oscuro" o "claro"), "decor" (una de: ${DECORACIONES.joinToString(", ")}),
            "mensaje" (un saludo corto en español, máximo 50 caracteres).
        """.trimIndent()
        val texto = IA.pedir(llave, prompt, 500)
        run {
            val j = IA.json(texto)
            fun color(k: String, defecto: Long): Long = try {
                0xFF000000 or j.getString(k).removePrefix("#").take(6).toLong(16)
            } catch (e: Exception) {
                defecto
            }
            val decor = j.optString("decor", "estrellas").let { if (it in DECORACIONES) it else "estrellas" }
            return Tema(
                id = "ia_${System.currentTimeMillis()}",
                nombre = j.optString("nombre", "Tema IA").take(24),
                acento = color("acento", 0xFF6B4EFF),
                acento2 = color("acento2", 0xFFFF7A59),
                acento3 = color("acento3", 0xFFFFC93C),
                base = if (j.optString("base") == "claro") "claro" else "oscuro",
                decor = decor,
                mensaje = j.optString("mensaje", "").take(60),
                propio = true,
            )
        }
    }
}

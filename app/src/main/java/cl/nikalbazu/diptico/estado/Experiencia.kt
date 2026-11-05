package cl.nikalbazu.diptico.estado

import org.json.JSONObject

/** Perfiles visuales portables: nunca contienen claves, rutas, juegos ni datos personales. */
data class PresentacionColeccion(val color: Long = 0xFF57DCC7, val fondo: String = "", val logo: String = "", val musica: String = "", val vista: String = "mosaico")
data class PosicionVisual(val tipo: String, val x: Int, val y: Int, val an: Int, val al: Int, val pagina: Int, val visible: Boolean)
data class Experiencia(
    val universo: Boolean = true,
    val vidrio: Boolean = true,
    val movimiento: Boolean = true,
    val pared: String = "medianoche",
    val adorno: String = "estrellas",
    val colecciones: Map<String, PresentacionColeccion> = emptyMap(),
    val posiciones: List<PosicionVisual> = emptyList(),
    val columnas: Int = 6,
    val filas: Int = 4,
    val paginas: Int = 1,
) {
    fun json(local: Boolean = false): JSONObject = JSONObject().put("formato", "diptico-visual").put("version", 1)
        .put("universo", universo).put("vidrio", vidrio).put("movimiento", movimiento)
        .put("pared", pared).put("adorno", adorno).put("columnas", columnas).put("filas", filas).put("paginas", paginas).apply {
            put("posiciones", org.json.JSONArray().apply { posiciones.filter { local || it.tipo in Widgets.porDefecto.map { w -> w.tipo } }.forEach { p -> put(JSONObject().put("tipo", p.tipo).put("x", p.x).put("y", p.y).put("an", p.an).put("al", p.al).put("pagina", p.pagina).put("visible", p.visible)) } })
            if (local) put("colecciones", JSONObject().apply { colecciones.forEach { (id, c) ->
                put(id, JSONObject().put("color", c.color).put("fondo", c.fondo).put("logo", c.logo).put("musica", c.musica).put("vista", c.vista))
            } })
        }
    companion object {
        val paredes = listOf("medianoche", "bosque", "atardecer", "papel")
        val adornos = listOf("estrellas", "plantas", "discos", "ninguno")
        fun leer(texto: String): Experiencia = try { deJson(JSONObject(texto)) } catch (_: Exception) { Experiencia() }
        fun importar(texto: String): Experiencia {
            require(texto.length <= 32768) { "El perfil es demasiado grande" }
            val o = JSONObject(texto)
            require(o.optString("formato") == "diptico-visual" && o.optInt("version") == 1) { "No es un perfil visual compatible" }
            return deJson(o).let { it.copy(colecciones = emptyMap(), posiciones = it.posiciones.filter { p -> p.tipo in Widgets.porDefecto.map { w -> w.tipo } }) }
        }
        private fun deJson(o: JSONObject): Experiencia = Experiencia(
            o.optBoolean("universo", true), o.optBoolean("vidrio", true), o.optBoolean("movimiento", true),
            o.optString("pared").takeIf { it in paredes } ?: "medianoche",
            o.optString("adorno").takeIf { it in adornos } ?: "estrellas",
            o.optJSONObject("colecciones")?.let { cs -> cs.keys().asSequence().take(500).associateWith { id ->
                val c = cs.getJSONObject(id)
                PresentacionColeccion(c.optLong("color", 0xFF57DCC7) or 0xFF000000, c.optString("fondo"), c.optString("logo"), c.optString("musica"), c.optString("vista").takeIf { it in listOf("mosaico", "galeria") } ?: "mosaico")
            } } ?: emptyMap(),
            o.optJSONArray("posiciones")?.let { a -> (0 until minOf(a.length(), 100)).map { i ->
                val p = a.getJSONObject(i)
                val cols = o.optInt("columnas", 6).coerceIn(4, 8)
                val filas = o.optInt("filas", 4).coerceIn(3, 8)
                val an = p.optInt("an", 1).coerceIn(1, cols)
                val al = p.optInt("al", 1).coerceIn(1, filas)
                PosicionVisual(p.getString("tipo").take(200), p.optInt("x").coerceIn(0, cols - an), p.optInt("y").coerceIn(0, filas - al), an, al, p.optInt("pagina").coerceIn(0, o.optInt("paginas", 1).coerceIn(1, 10) - 1), p.optBoolean("visible", true))
            }.distinctBy { it.tipo } } ?: emptyList(),
            o.optInt("columnas", 6).coerceIn(4, 8), o.optInt("filas", 4).coerceIn(3, 8), o.optInt("paginas", 1).coerceIn(1, 10),
        )
        fun perfiles(texto: String): Map<String, Experiencia> = try {
            val o = JSONObject(texto)
            o.keys().asSequence().take(3).associateWith { leer(o.getJSONObject(it).toString()) }
        } catch (_: Exception) { emptyMap() }
    }
}

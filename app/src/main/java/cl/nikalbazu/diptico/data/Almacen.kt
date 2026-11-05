package cl.nikalbazu.diptico.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** Guarda las preferencias y la memoria de Díptico en el teléfono. */
class Almacen(ctx: Context) {
    companion object {
        private val credencialesLocales = setOf(
            "claveSgdb", "claveGemini", "claveOpenAI", "claveIa", "claveRA", "usuarioRA",
            "ras_igdbId", "ras_igdbSecreto", "ras_ssUsuario", "ras_ssClave",
            "ras_ssDevId", "ras_ssDevClave", "ras_rawg", "ras_tgdb",
        )

        internal fun esCredencial(clave: String) = clave in credencialesLocales || clave.startsWith("ras_")
    }

    private val p = ctx.getSharedPreferences("diptico", Context.MODE_PRIVATE)
    private val credenciales = ctx.getSharedPreferences("diptico_credenciales", Context.MODE_PRIVATE)

    init {
        val antiguas = if (credencialesLocales.any(p::contains)) p.all.filterKeys(::esCredencial) else emptyMap()
        if (antiguas.isNotEmpty()) {
            val destino = credenciales.edit()
            for ((clave, valor) in antiguas) {
                if (valor is String && !credenciales.contains(clave)) destino.putString(clave, valor)
            }
            if (destino.commit()) {
                val origen = p.edit()
                antiguas.keys.forEach(origen::remove)
                origen.commit()
            }
        }
    }

    private fun preferencias(clave: String) = if (esCredencial(clave)) credenciales else p

    var carpeta: Uri?
        get() = p.getString("carpeta", null)?.let(Uri::parse)
        set(v) = p.edit().putString("carpeta", v?.toString()).apply()

    var media: Uri?
        get() = p.getString("media", null)?.let(Uri::parse)
        set(v) = p.edit().putString("media", v?.toString()).apply()

    var configurado: Boolean
        get() = p.getBoolean("configurado", false)
        set(v) = p.edit().putBoolean("configurado", v).apply()

    var oscuro: Boolean
        get() = p.getBoolean("oscuro", true)
        set(v) = p.edit().putBoolean("oscuro", v).apply()

    var intercambiar: Boolean
        get() = p.getBoolean("intercambiar", false)
        set(v) = p.edit().putBoolean("intercambiar", v).apply()

    /** "dos", "una_negra" (abajo en negro) o "una_libre" (abajo sin Díptico). */
    var modoPantallas: String
        get() = p.getString("modoPantallas", "dos") ?: "dos"
        set(v) = p.edit().putString("modoPantallas", v).apply()

    var mostrarIntro: Boolean
        get() = p.getBoolean("mostrarIntro", true)
        set(v) = p.edit().putBoolean("mostrarIntro", v).apply()

    var modoElegido: Boolean
        get() = p.getBoolean("modoElegido", false)
        set(v) = p.edit().putBoolean("modoElegido", v).apply()

    var pantallaJuegos: String
        get() = p.getString("pantallaJuegos", "arriba") ?: "arriba"
        set(v) = p.edit().putString("pantallaJuegos", v).apply()

    /** Tamaño de letra: 1.0 normal, 1.15 grande, 1.3 muy grande. */
    var tamTexto: Float
        get() = p.getFloat("tamTexto", 1f)
        set(v) = p.edit().putFloat("tamTexto", v).apply()

    var musica: Boolean
        get() = p.getBoolean("musica", true)
        set(v) = p.edit().putBoolean("musica", v).apply()

    /** "bisagra", "cartucho", "medianoche", "mezcla" o "mia" (carpeta propia). */
    var pista: String
        get() = p.getString("pista", "bisagra") ?: "bisagra"
        set(v) = p.edit().putString("pista", v).apply()

    var volumen: Float
        get() = p.getFloat("volumen", 0.6f)
        set(v) = p.edit().putFloat("volumen", v).apply()

    var carpetaMusica: Uri?
        get() = p.getString("carpetaMusica", null)?.let(Uri::parse)
        set(v) = p.edit().putString("carpetaMusica", v?.toString()).apply()

    /** Clave personal de SteamGridDB (opcional). */
    var claveSgdb: String
        get() = texto("claveSgdb")
        set(v) = guardarTexto("claveSgdb", v)

    /** Usar los títulos de libretro en vez del nombre del archivo. */
    var titulosBonitos: Boolean
        get() = p.getBoolean("titulosBonitos", true)
        set(v) = p.edit().putBoolean("titulosBonitos", v).apply()

    /** Paquetes de las apps elegidas como ports. */
    var ports: List<String>
        get() = (p.getString("ports", "") ?: "").split('\n').filter { it.isNotBlank() }
        set(v) = p.edit().putString("ports", v.joinToString("\n")).apply()

    /** Juegos de Android elegidos. null = todavía no se eligió (se detectan solos). */
    var android: List<String>?
        get() = p.getString("android", null)?.split('\n')?.filter { it.isNotBlank() }
        set(v) = p.edit().putString("android", v?.joinToString("\n")).apply()

    /** Texto guardado con un nombre (temas propios, widgets, íconos...), en JSON. */
    fun texto(clave: String, defecto: String = ""): String = preferencias(clave).getString(clave, defecto) ?: defecto
    fun guardarTexto(clave: String, valor: String) = preferencias(clave).edit().putString(clave, valor).apply()
    fun bool(clave: String, defecto: Boolean): Boolean = p.getBoolean(clave, defecto)
    fun guardarBool(clave: String, valor: Boolean) = p.edit().putBoolean(clave, valor).apply()

    /**
     * Todas las carpetas de juegos. Cada una puede tener una consola fija (si su nombre no es el de una consola).
     * Si la persona venía de una versión con una sola carpeta, esa queda como la primera.
     */
    fun carpetas(): List<CarpetaJuegos> {
        val s = p.getString("carpetas", null)
        if (s == null) return carpeta?.let { listOf(CarpetaJuegos(it, null)) } ?: emptyList()
        return try {
            val a = JSONArray(s)
            (0 until a.length()).map { i ->
                a.getJSONObject(i).let { o -> CarpetaJuegos(Uri.parse(o.getString("uri")), o.optString("consola", "").ifEmpty { null }) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun guardarCarpetas(l: List<CarpetaJuegos>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("uri", it.uri.toString()).put("consola", it.consola ?: "")) }
        p.edit().putString("carpetas", a.toString()).apply()
        carpeta = l.firstOrNull()?.uri
    }

    fun medios(): Map<String, MediosJuego> {
        val o = try { JSONObject(p.getString("medios", "{}") ?: "{}") } catch (e: Exception) { JSONObject() }
        val salida = HashMap<String, MediosJuego>()
        for (k in o.keys()) {
            val d = o.optJSONObject(k) ?: continue
            fun t(n: String) = d.optString(n, "").takeIf { it.isNotEmpty() }
            salida[k] = MediosJuego(t("portada"), t("arte"), t("logo"), t("titulo"))
        }
        return salida
    }

    fun guardarMedios(m: Map<String, MediosJuego>) {
        val o = JSONObject()
        for ((k, d) in m) {
            val j = JSONObject()
            d.portada?.let { j.put("portada", it) }
            d.arte?.let { j.put("arte", it) }
            d.logo?.let { j.put("logo", it) }
            d.titulo?.let { j.put("titulo", it) }
            o.put(k, j)
        }
        p.edit().putString("medios", o.toString()).apply()
    }

    fun emuladores(): Map<String, String> {
        val o = JSONObject(p.getString("emuladores", "{}") ?: "{}")
        return o.keys().asSequence().associateWith { o.getString(it) }
    }

    fun guardarEmuladores(m: Map<String, String>) {
        p.edit().putString("emuladores", JSONObject(m).toString()).apply()
    }

    fun datos(): Map<String, DatosJuego> {
        val o = try { JSONObject(p.getString("datos", "{}") ?: "{}") } catch (e: Exception) { JSONObject() }
        val salida = HashMap<String, DatosJuego>()
        for (k in o.keys()) {
            val d = o.optJSONObject(k) ?: continue
            salida[k] = DatosJuego(
                tiempo = d.optLong("tiempo", 0L),
                paso = d.optString("paso", ""),
                ultimo = d.optLong("ultimo", 0L),
                pantalla = if (d.has("pantalla")) d.optString("pantalla") else null,
                titulo = d.optString("titulo", "").ifEmpty { null },
                portada = d.optString("portada", "").ifEmpty { null },
                arte = d.optString("arte", "").ifEmpty { null },
                logo = d.optString("logo", "").ifEmpty { null },
                musica = d.optString("musica", "").ifEmpty { null },
                notas = d.optString("notas", ""),
                favorito = d.optBoolean("favorito", false),
                emulador = d.optString("emulador", "").ifEmpty { null },
                sesiones = d.optInt("sesiones", 0),
                oculto = d.optBoolean("oculto", false),
                lupa = d.optBoolean("lupa", false),
                zonaLupa = d.optString("zonaLupa", "").ifEmpty { null },
                vistaPartida = d.optString("vistaPartida", "").ifEmpty { null },
                avance = d.optString("avance", "").ifEmpty { null },
                puestoCola = d.optLong("puestoCola").takeIf { d.has("puestoCola") },
            )
        }
        return salida
    }

    fun guardarDatos(m: Map<String, DatosJuego>) {
        val o = JSONObject()
        for ((k, d) in m) {
            val j = JSONObject()
            j.put("tiempo", d.tiempo)
            j.put("paso", d.paso)
            j.put("ultimo", d.ultimo)
            d.pantalla?.let { j.put("pantalla", it) }
            d.titulo?.let { j.put("titulo", it) }
            d.portada?.let { j.put("portada", it) }
            d.arte?.let { j.put("arte", it) }
            d.logo?.let { j.put("logo", it) }
            d.musica?.let { j.put("musica", it) }
            if (d.notas.isNotEmpty()) j.put("notas", d.notas)
            if (d.favorito) j.put("favorito", true)
            d.emulador?.let { j.put("emulador", it) }
            if (d.sesiones > 0) j.put("sesiones", d.sesiones)
            if (d.oculto) j.put("oculto", true)
            if (d.lupa) j.put("lupa", true)
            d.zonaLupa?.let { j.put("zonaLupa", it) }
            d.vistaPartida?.let { j.put("vistaPartida", it) }
            d.avance?.let { j.put("avance", it) }
            d.puestoCola?.let { j.put("puestoCola", it) }
            o.put(k, j)
        }
        p.edit().putString("datos", o.toString()).apply()
    }

    fun biblioteca(): List<Juego> {
        val a = try { JSONArray(p.getString("biblioteca", "[]") ?: "[]") } catch (e: Exception) { JSONArray() }
        val salida = ArrayList<Juego>(a.length())
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            salida += Juego(
                clave = o.getString("clave"),
                consolaId = o.getString("consola"),
                titulo = o.getString("titulo"),
                archivo = o.getString("archivo"),
                uri = Uri.parse(o.getString("uri")),
                docId = o.getString("doc"),
                portada = o.optString("portada", "").takeIf { it.isNotEmpty() }?.let(Uri::parse),
                arte = o.optString("arte", "").takeIf { it.isNotEmpty() }?.let(Uri::parse),
                esCarpeta = o.optBoolean("carpeta", false),
            )
        }
        return salida
    }

    fun guardarBiblioteca(lista: List<Juego>) {
        val a = JSONArray()
        for (j in lista) {
            val o = JSONObject()
            o.put("clave", j.clave)
            o.put("consola", j.consolaId)
            o.put("titulo", j.titulo)
            o.put("archivo", j.archivo)
            o.put("uri", j.uri.toString())
            o.put("doc", j.docId)
            o.put("portada", j.portada?.toString() ?: "")
            o.put("arte", j.arte?.toString() ?: "")
            o.put("carpeta", j.esCarpeta)
            a.put(o)
        }
        p.edit().putString("biblioteca", a.toString()).apply()
    }
}


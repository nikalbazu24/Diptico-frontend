package cl.nikalbazu.diptico.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** Guarda las preferencias y la memoria de Díptico en el teléfono. */
class Almacen(ctx: Context) {
    private val p = ctx.getSharedPreferences("diptico", Context.MODE_PRIVATE)

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

    var modoElegido: Boolean
        get() = p.getBoolean("modoElegido", false)
        set(v) = p.edit().putBoolean("modoElegido", v).apply()

    var pantallaJuegos: String
        get() = p.getString("pantallaJuegos", "arriba") ?: "arriba"
        set(v) = p.edit().putString("pantallaJuegos", v).apply()

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

package cl.nikalbazu.diptico.estado

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Respaldo de Díptico en un solo archivo .zip: todos los ajustes (temas, widgets, colecciones, tiempo de juego,
 * notas, «¿dónde quedaste?», mapas…) y las notas de voz. Se guarda donde la persona elija, por ejemplo una
 * carpeta de Google Drive, y Díptico lo vuelve a escribir solo una vez al día.
 * Las imágenes descargadas no van (se vuelven a bajar solas) para que el respaldo sea liviano.
 */
object Respaldo {
    private const val PREFS = "diptico"
    /** Claves que no tienen sentido en otro equipo (permisos de archivos que Android da a cada instalación). */
    private val SIN_COPIAR = setOf("respaldoUri", "respaldoUltimo")

    /** Escribe el respaldo en [salida]. Devuelve cuántos ajustes guardó. */
    fun escribir(ctx: Context, salida: OutputStream): Int {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all
        val json = JSONObject()
        for ((k, v) in prefs) {
            if (k in SIN_COPIAR || v == null) continue
            val o = JSONObject()
            when (v) {
                is String -> o.put("t", "s").put("v", v)
                is Boolean -> o.put("t", "b").put("v", v)
                is Int -> o.put("t", "i").put("v", v)
                is Long -> o.put("t", "l").put("v", v)
                is Float -> o.put("t", "f").put("v", v.toDouble())
                is Set<*> -> o.put("t", "set").put("v", JSONArray(v.map { it.toString() }))
                else -> continue
            }
            json.put(k, o)
        }
        ZipOutputStream(salida).use { zip ->
            zip.putNextEntry(ZipEntry("ajustes.json"))
            zip.write(json.toString().toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("info.txt"))
            zip.write("Respaldo de Díptico · ${java.util.Date()}\n".toByteArray())
            zip.closeEntry()
            File(ctx.filesDir, "notas").takeIf { it.isDirectory }?.listFiles()?.filter { it.isFile }?.forEach { f ->
                zip.putNextEntry(ZipEntry("notas/${f.name}"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return json.length()
    }

    /** Reemplaza los ajustes con los del respaldo. Devuelve cuántos trajo, o -1 si el archivo no es un respaldo. */
    fun restaurar(ctx: Context, entrada: InputStream): Int {
        var ajustes: JSONObject? = null
        val notas = File(ctx.filesDir, "notas").apply { mkdirs() }
        ZipInputStream(entrada).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                when {
                    e.name == "ajustes.json" -> ajustes = JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                    e.name.startsWith("notas/") && !e.isDirectory -> {
                        // Solo el nombre del archivo: nunca rutas que salgan de la carpeta de notas.
                        val nombre = e.name.substringAfterLast('/')
                        if (nombre.isNotBlank() && !nombre.startsWith(".")) File(notas, nombre).outputStream().use { zip.copyTo(it) }
                    }
                }
                zip.closeEntry()
            }
        }
        val a = ajustes ?: return -1
        val ed = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        // Se conservan los permisos de este equipo (el respaldo puede venir de otro).
        val actuales = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all
        ed.clear()
        for (k in SIN_COPIAR) when (val v = actuales[k]) { is String -> ed.putString(k, v); is Long -> ed.putLong(k, v) }
        for (k in a.keys()) {
            val o = a.getJSONObject(k)
            when (o.getString("t")) {
                "s" -> ed.putString(k, o.getString("v"))
                "b" -> ed.putBoolean(k, o.getBoolean("v"))
                "i" -> ed.putInt(k, o.getInt("v"))
                "l" -> ed.putLong(k, o.getLong("v"))
                "f" -> ed.putFloat(k, o.getDouble("v").toFloat())
                "set" -> ed.putStringSet(k, o.getJSONArray("v").let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() })
            }
        }
        // commit (no apply): queda escrito antes de reiniciar Díptico.
        ed.commit()
        return a.length()
    }

    /** Escribe el respaldo en el archivo de Drive (o donde sea) que la persona eligió. */
    fun escribirEn(ctx: Context, uri: Uri): Boolean = try {
        ctx.contentResolver.openOutputStream(uri, "wt")?.use { escribir(ctx, it); true } ?: false
    } catch (_: Exception) {
        // Algunos proveedores no aceptan "wt": se prueba con "w".
        try { ctx.contentResolver.openOutputStream(uri, "w")?.use { escribir(ctx, it); true } ?: false } catch (_: Exception) { false }
    }
}

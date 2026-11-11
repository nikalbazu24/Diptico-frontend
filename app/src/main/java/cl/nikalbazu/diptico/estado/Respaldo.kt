package cl.nikalbazu.diptico.estado

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.AtomicFile
import cl.nikalbazu.diptico.data.Almacen
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Copia portable de las preferencias y notas de voz, con restauración validada antes de escribir. */
object Respaldo {
    private const val PREFS = "diptico"
    private const val LIMITE_ZIP = 128L * 1024 * 1024
    private const val LIMITE_AJUSTES = 8L * 1024 * 1024
    private const val LIMITE_ENTRADAS = 2_048
    private val descartar = object : OutputStream() {
        override fun write(b: Int) = Unit
        override fun write(b: ByteArray, off: Int, len: Int) = Unit
    }
    private val soloEquipo = setOf(
        "respaldoUri", "respaldoUltimo", "respaldoNombre",
        "carpeta", "carpetas", "media", "carpetaMusica", "biblioteca", "medios", "actAvisada",
    )
    private val tiposFijos = mapOf(
        "configurado" to "b", "oscuro" to "b", "intercambiar" to "b", "modoElegido" to "b",
        "mostrarIntro" to "b", "musica" to "b", "titulosBonitos" to "b",
        "tamTexto" to "f", "volumen" to "f",
        "modoPantallas" to "s", "pantallaJuegos" to "s", "pista" to "s",
        "datos" to "s", "widgets" to "s", "colecciones" to "s", "gridCols" to "s",
        "gridFilas" to "s", "paginas" to "s",
    )

    /** Carpetas que también viajan en el respaldo: copias manuales de partidas y la música de los temas instalados. */
    private val CARPETAS_EXTRA = listOf("copias-partida", "temas-comunidad", cl.nikalbazu.diptico.data.RespaldoPartidas.CARPETA)

    /** Ruta relativa segura (sin «..», sin ocultos, hasta 3 niveles). */
    private fun rutaSegura(ruta: String): Boolean {
        val partes = ruta.split('/')
        return ruta.length <= 300 && partes.size in 1..3 && partes.all { it.isNotBlank() && it != ".." && !it.startsWith(".") && '\\' !in it }
    }

    private fun portable(clave: String) = clave !in soloEquipo && !Almacen.esCredencial(clave)

    private fun tipo(valor: Any?) = when (valor) {
        is String -> "s"
        is Boolean -> "b"
        is Int -> "i"
        is Long -> "l"
        is Float -> "f"
        is Set<*> -> "set"
        else -> null
    }

    private fun codificar(valores: Map<String, *>): JSONObject = JSONObject().apply {
        valores.filterKeys(::portable).forEach { (clave, valor) ->
            val tipo = tipo(valor) ?: return@forEach
            val dato = when (valor) {
                is Set<*> -> JSONArray(valor.toList())
                is Float -> valor.toDouble()
                else -> valor
            }
            put(clave, JSONObject().put("t", tipo).put("v", dato))
        }
    }

    @Synchronized
    fun escribir(ctx: Context, salida: OutputStream): Int {
        val ajustes = codificar(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all)
        ZipOutputStream(salida).use { zip ->
            zip.putNextEntry(ZipEntry("ajustes.json"))
            zip.write(ajustes.toString().toByteArray())
            zip.closeEntry()
            File(ctx.filesDir, "notas").listFiles()?.filter { it.isFile }?.forEach { f ->
                zip.putNextEntry(ZipEntry("notas/${f.name}"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            for (nombre in CARPETAS_EXTRA) {
                val raiz = File(ctx.filesDir, nombre)
                // Partidas respaldadas: solo la copia más nueva de cada juego, y sin pasar de 48 MB (el respaldo tiene un tope).
                val archivos = if (nombre == cl.nikalbazu.diptico.data.RespaldoPartidas.CARPETA)
                    cl.nikalbazu.diptico.data.RespaldoPartidas.paraRespaldo(ctx, 48L * 1024 * 1024).asSequence()
                else raiz.walkTopDown().filter { it.isFile }
                archivos.forEach { f ->
                    val rel = f.relativeTo(raiz).invariantSeparatorsPath
                    if (!rutaSegura(rel)) return@forEach
                    zip.putNextEntry(ZipEntry("extras/$nombre/$rel"))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        return ajustes.length()
    }

    private fun decodificar(json: JSONObject, previos: Map<String, *>): Map<String, Any> = buildMap {
        require(json.length() in 1..4096)
        for (clave in json.keys()) {
            if (!portable(clave)) continue
            require(clave.length in 1..160)
            val dato = json.getJSONObject(clave)
            val clase = dato.getString("t")
            val esperado = tiposFijos[clave] ?: tipo(previos[clave])
            require(esperado == null || esperado == clase)
            val valor: Any = when (clase) {
                "s" -> dato.get("v") as String
                "b" -> dato.get("v") as Boolean
                "i" -> (dato.get("v") as Number).toLong().also { require(it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) }.toInt()
                "l" -> (dato.get("v") as Number).toLong()
                "f" -> (dato.get("v") as Number).toFloat().also { require(it.isFinite()) }
                "set" -> dato.getJSONArray("v").let { a -> (0 until a.length()).map { a.get(it) as String }.toSet() }
                else -> error("Tipo de ajuste desconocido")
            }
            when (clave) {
                "gridCols" -> require((valor as String).toIntOrNull()?.let { it in 3..10 } == true)
                "gridFilas" -> require((valor as String).toIntOrNull()?.let { it in 2..8 } == true)
                "paginas" -> require((valor as String).toIntOrNull()?.let { it in 1..256 } == true)
            }
            put(clave, valor)
        }
        require(isNotEmpty())
    }

    private fun copiarLimitado(entrada: InputStream, salida: OutputStream, limite: Long): Long {
        var total = 0L
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val leidos = entrada.read(buffer)
            if (leidos < 0) return total
            total += leidos
            require(total <= limite) { "El respaldo excede el tamaño permitido" }
            salida.write(buffer, 0, leidos)
        }
    }

    private fun cargarZip(entrada: InputStream, temporal: File, extras: File): JSONObject {
        var total = 0L
        var ajustes: JSONObject? = null
        val vistos = HashSet<String>()
        ZipInputStream(entrada).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                require(vistos.add(e.name) && vistos.size <= LIMITE_ENTRADAS)
                val limite = LIMITE_ZIP - total
                val leidos = when {
                    e.name == "ajustes.json" && !e.isDirectory -> {
                        val bytes = java.io.ByteArrayOutputStream()
                        val n = copiarLimitado(zip, bytes, minOf(limite, LIMITE_AJUSTES))
                        ajustes = JSONObject(bytes.toString("UTF-8"))
                        n
                    }
                    e.name == "info.txt" -> copiarLimitado(zip, descartar, minOf(limite, 16_384L))
                    e.name == "notas/" && e.isDirectory -> copiarLimitado(zip, descartar, 0)
                    e.name.startsWith("notas/") && !e.isDirectory -> {
                        val nombre = e.name.removePrefix("notas/")
                        require(nombre.isNotBlank() && nombre.length <= 200 && !nombre.startsWith(".") && '/' !in nombre && '\\' !in nombre)
                        File(temporal, nombre).outputStream().use { copiarLimitado(zip, it, limite) }
                    }
                    e.name.startsWith("extras/") -> {
                        val resto = e.name.removePrefix("extras/")
                        val carpeta = resto.substringBefore('/')
                        val rel = resto.substringAfter('/', "")
                        if (e.isDirectory || rel.isBlank()) copiarLimitado(zip, descartar, 0)
                        else {
                            require(carpeta in CARPETAS_EXTRA && rutaSegura(rel))
                            val destino = File(extras, "$carpeta/$rel").apply { parentFile?.mkdirs() }
                            destino.outputStream().use { copiarLimitado(zip, it, limite) }
                        }
                    }
                    else -> error("Entrada de respaldo desconocida")
                }
                total += leidos
                zip.closeEntry()
            }
        }
        return requireNotNull(ajustes)
    }

    private fun reemplazar(prefs: SharedPreferences, valores: Map<String, *>): Boolean {
        val ed = prefs.edit().clear()
        valores.forEach { (clave, valor) ->
            when (valor) {
                is String -> ed.putString(clave, valor)
                is Boolean -> ed.putBoolean(clave, valor)
                is Int -> ed.putInt(clave, valor)
                is Long -> ed.putLong(clave, valor)
                is Float -> ed.putFloat(clave, valor)
                is Set<*> -> ed.putStringSet(clave, valor.filterIsInstance<String>().toSet())
            }
        }
        return ed.commit()
    }

    private fun guardarCopiaPrevia(ctx: Context) {
        val archivo = AtomicFile(File(ctx.filesDir, "respaldo-antes-de-restaurar.zip"))
        val salida = archivo.startWrite()
        try {
            // El ZIP debe finalizar, pero AtomicFile conserva el control del descriptor hasta sincronizarlo.
            escribir(ctx, object : java.io.FilterOutputStream(salida) {
                override fun write(b: ByteArray, off: Int, len: Int) = salida.write(b, off, len)
                override fun close() = flush()
            })
            archivo.finishWrite(salida)
        } catch (error: Exception) {
            archivo.failWrite(salida)
            throw error
        }
    }

    @Synchronized
    fun restaurar(ctx: Context, entrada: InputStream): Int {
        val temporal = java.nio.file.Files.createTempDirectory(ctx.filesDir.toPath(), "restaurar-").toFile()
        val nuevas = File(temporal, "nuevas").apply { mkdir() }
        val anteriores = File(temporal, "anteriores")
        val extras = File(temporal, "extras").apply { mkdir() }
        val notas = File(ctx.filesDir, "notas")
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previos = prefs.all
        var movioNotas = false
        var pusoNotas = false
        var intentoAjustes = false
        var conservarTemporal = false
        try {
            val valores = decodificar(cargarZip(entrada, nuevas, extras), previos)
            guardarCopiaPrevia(ctx)
            // Conserva notas locales que no estaban incluidas en la copia.
            notas.listFiles()?.filter { it.isFile && !File(nuevas, it.name).exists() }?.forEach {
                it.copyTo(File(nuevas, it.name))
            }
            if (notas.exists()) {
                check(notas.renameTo(anteriores))
                movioNotas = true
            }
            check(nuevas.renameTo(notas))
            pusoNotas = true
            intentoAjustes = true
            check(reemplazar(prefs, previos.filterKeys { !portable(it) } + valores))
            // Copias de partida y música de temas: se suman a lo que ya hay (nunca se borra nada local).
            try { extras.copyRecursively(ctx.filesDir, overwrite = true) } catch (_: Exception) {}
            return valores.size
        } catch (_: Exception) {
            if (intentoAjustes) reemplazar(prefs, previos)
            if (pusoNotas) notas.deleteRecursively()
            if (movioNotas && !anteriores.renameTo(notas)) conservarTemporal = true
            return -1
        } finally {
            // Si el sistema no dejó revertir la carpeta, conserva la copia anterior para recuperación.
            if (!conservarTemporal) temporal.deleteRecursively()
        }
    }

    fun escribirEn(ctx: Context, uri: Uri): Boolean {
        val temporal = try { File.createTempFile("respaldo-", ".zip", ctx.cacheDir) }
        catch (_: Exception) { return false }
        return try {
            temporal.outputStream().use { escribir(ctx, it) }
            val salida = try { ctx.contentResolver.openOutputStream(uri, "wt") }
            catch (_: Exception) { ctx.contentResolver.openOutputStream(uri, "w") }
            salida?.use { temporal.inputStream().use { origen -> origen.copyTo(it) }; true } ?: false
        } catch (_: Exception) {
            false
        } finally {
            temporal.delete()
        }
    }
}

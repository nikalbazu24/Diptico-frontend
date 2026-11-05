package cl.nikalbazu.diptico.emu

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Display
import cl.nikalbazu.diptico.data.Comando
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego

/** Encuentra las dos pantallas. En la Thor, la de arriba es la principal. */
object Pantallas {
    fun inferior(ctx: Context): Display? {
        val dm = ctx.getSystemService(DisplayManager::class.java) ?: return null
        // La pantalla de abajo de la consola, nunca la tele (HDMI, USB-C, inalámbrica) si hay una conectada.
        val tele = Regex("hdmi|displayport|\\bdp\\b|external|externa|usb|\\btv\\b|wireless|cast|miracast", RegexOption.IGNORE_CASE)
        // La proyección de la lupa también aparece como pantalla, pero no es la pantalla física de abajo.
        val otras = dm.displays.filter { it.displayId != Display.DEFAULT_DISPLAY && it.name !in setOf("Diptico-lupa", "Diptico-retoma") }
        // Solo se descarta por el nombre si hay más de una (por si la pantalla propia tuviera un nombre raro).
        val propias = if (otras.size >= 2) otras.filter { !tele.containsMatchIn(it.name ?: "") }.ifEmpty { otras.take(1) } else otras
        val presentacion = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .firstOrNull { d -> d.displayId != Display.DEFAULT_DISPLAY && propias.any { it.displayId == d.displayId } }
        return presentacion ?: propias.firstOrNull()
    }
}

/** Una app instalada que se puede abrir (para elegir ports y juegos de Android). */
data class App(val paquete: String, val nombre: String, val esJuego: Boolean = false)

/**
 * Abre juegos con su emulador, en la pantalla elegida.
 * Entiende los mismos comandos que ES-DE para Android, así que sirve para todas sus consolas.
 */
object Lanzador {
    /** Un emulador instalado para una consola, con la orden que lo abre. */
    data class Opcion(val comando: Comando, val paquete: String?, val actividad: String?) {
        /** Lo que se guarda en los ajustes. */
        val id: String get() = comando.etiqueta
        val nombre: String get() = comando.nombre
    }

    private val instalados = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    /** Olvida lo que se sabía de apps instaladas (al volver a Díptico, por si instalaste algo). */
    fun olvidar() = instalados.clear()

    fun instalado(ctx: Context, paquete: String): Boolean = instalados.getOrPut(paquete) {
        try {
            ctx.packageManager.getPackageInfo(paquete, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** Primer "paquete/actividad" instalado de una regla de ES-DE. */
    private fun resolver(ctx: Context, emulador: String): Pair<String, String>? {
        for (e in Consolas.reglas[emulador] ?: return null) {
            val pkg = e.substringBefore('/')
            if (!instalado(ctx, pkg)) continue
            val act = e.substringAfter('/', "")
            return pkg to when {
                act.isEmpty() -> ""
                act.startsWith(".") -> pkg + act
                else -> act
            }
        }
        return null
    }

    /** Extensión que pide un comando que lee un archivo vecino (ej. "%INJECT%=%BASENAME%.ps3" -> "ps3"). */
    private fun extensionInyectada(c: Comando): String? =
        Regex("%INJECT%=%BASENAME%\\.([a-z0-9]+)").find(c.linea)?.groupValues?.get(1)

    private fun sirve(c: Comando, tipo: String?): Boolean {
        val ext = extensionInyectada(c) ?: return true
        return tipo == null || tipo == ext
    }

    /** Emuladores instalados para la consola. Si se indica [tipo], primero los que sirven para ese archivo. */
    fun opciones(ctx: Context, consola: Consola, tipo: String? = null): List<Opcion> {
        val cache = HashMap<String, Pair<String, String>?>()
        val todas = consola.comandos.mapNotNull { c ->
            when {
                c.abreApp -> Opcion(c, null, null)
                c.emulador != null -> cache.getOrPut(c.emulador) { resolver(ctx, c.emulador) }?.let { (p, a) -> Opcion(c, p, a) }
                else -> null
            }
        }
        if (tipo == null) return todas
        return todas.filter { sirve(it.comando, tipo) } + todas.filter { !sirve(it.comando, tipo) }
    }

    /**
     * El emulador elegido para la consola. Entiende lo guardado por versiones anteriores
     * ("paquete" o "paquete#núcleo") y, si ya no está instalado, usa el primero que sirva.
     */
    fun elegida(ctx: Context, consola: Consola, preferido: String?, tipo: String? = null): Opcion? {
        val ops = opciones(ctx, consola, tipo)
        if (ops.isEmpty()) return null
        val compatibles = ops.filter { sirve(it.comando, tipo) }
        if (preferido != null) {
            val p = ops.firstOrNull { it.id == preferido }
                ?: preferido.substringAfter('#', "").takeIf { it.isNotEmpty() }?.let { core -> ops.firstOrNull { it.comando.nucleo == core } }
                ?: ops.firstOrNull { it.paquete == preferido && it.comando.nucleo == null }
            if (p != null && (tipo == null || sirve(p.comando, tipo))) return p
        }
        return compatibles.firstOrNull() ?: ops.first()
    }

    /** Convierte un documento de la memoria interna o SD en ruta real: primary:ROMs/x -> /storage/emulated/0/ROMs/x. */
    fun rutaReal(docId: String): String? {
        if (docId.startsWith("raw:")) return docId.removePrefix("raw:")
        val dos = docId.indexOf(':')
        if (dos <= 0) return null
        val volumen = docId.substring(0, dos)
        val resto = docId.substring(dos + 1)
        return if (volumen.equals("primary", true)) "/storage/emulated/0/$resto" else "/storage/$volumen/$resto"
    }

    private fun leerTexto(ctx: Context, uri: Uri): String? = try {
        ctx.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            ?.lineSequence()?.map { it.trim() }?.firstOrNull { it.isNotEmpty() }
    } catch (e: Exception) {
        null
    }

    /** Separa la orden en partes, respetando lo que va entre comillas. */
    private fun partes(linea: String): List<String> {
        val salida = mutableListOf<String>()
        val sb = StringBuilder()
        var comillas = false
        for (ch in linea) {
            when {
                ch == '"' -> { comillas = !comillas; sb.append(ch) }
                ch == ' ' && !comillas -> if (sb.isNotEmpty()) { salida += sb.toString(); sb.clear() }
                else -> sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) salida += sb.toString()
        return salida
    }

    private class Falla(val mensaje: String) : Exception(mensaje)

    /** Arma el Intent de ES-DE para un juego. */
    private fun construir(ctx: Context, op: Opcion, juego: Juego, consola: Consola, rom: Uri, raiz: Uri?): Intent {
        val linea = op.comando.linea
        // Accesos creados por Díptico (juegos de PC de Steam, Epic, GOG…) viven en su carpeta interna.
        val ruta = if (juego.uri.scheme == "file") juego.uri.path else rutaReal(juego.docId)
        val raizRuta = raiz?.let { r -> try { rutaReal(DocumentsContract.getTreeDocumentId(r)) } catch (e: Exception) { null } }
        val base = if (juego.esCarpeta) juego.archivo else juego.archivo.substringBeforeLast('.')

        fun necesitaRuta(): String = ruta ?: throw Falla("No pude encontrar la ruta de ${juego.titulo}. Usa una carpeta de la memoria interna o la SD.")

        fun basico(v: String): String {
            var x = v
            if ("%ROMPATHRAW%" in x || "%ROMPATH%" in x) {
                val rp = raizRuta ?: necesitaRuta().substringBeforeLast('/').substringBeforeLast('/')
                x = x.replace("%ROMPATHRAW%", rp).replace("%ROMPATH%", rp)
            }
            if ("%GAMEDIRRAW%" in x) x = x.replace("%GAMEDIRRAW%", necesitaRuta().substringBeforeLast('/'))
            if ("%ROMRAW%" in x) x = x.replace("%ROMRAW%", necesitaRuta())
            if ("%ROM%" in x) x = x.replace("%ROM%", necesitaRuta())
            x = x.replace("%ROMSAF%", rom.toString()).replace("%ROMPROVIDER%", rom.toString())
                .replace("%BASENAME%", base)
                .replace("%ANDROIDPACKAGE%", op.paquete ?: "")
                .replace("%EXTERNALDATA%", "/storage/emulated/0")
                .replace("%INTERNALDATA%", "/data/data")
            return x
        }

        /** Lee el contenido de un archivo junto al juego (o del juego mismo). */
        fun inyectar(nombre: String): String {
            val n = nombre.substringAfterLast('/')
            val uri = if (n.equals(juego.archivo, true) || raiz == null) juego.uri
            else DocumentsContract.buildDocumentUriUsingTree(raiz, juego.docId.substringBeforeLast('/') + "/" + n)
            return leerTexto(ctx, uri) ?: throw Falla("El archivo $n debe tener adentro el código del juego (por ejemplo CUSA00900).")
        }

        fun valor(v0: String): String {
            val v = v0.removeSurrounding("\"")
            if (v == "%FILEINJECT%") return inyectar(juego.archivo)
            val i = v.indexOf("%INJECT%=")
            if (i >= 0) return basico(v.substring(0, i)) + inyectar(basico(v.substring(i + "%INJECT%=".length)))
            return basico(v)
        }

        val intent = Intent()
        var data: Uri? = null
        var mime: String? = null
        for (p in partes(linea)) {
            if (p.startsWith("%EMULATOR_")) {
                if (op.paquete != null) {
                    if (op.actividad.isNullOrEmpty()) {
                        ctx.packageManager.getLaunchIntentForPackage(op.paquete)?.component?.let { intent.component = it }
                            ?: intent.setPackage(op.paquete)
                    } else {
                        intent.component = ComponentName(op.paquete, op.actividad)
                    }
                }
                continue
            }
            when (p) {
                "%ACTIVITY_CLEAR_TASK%" -> { intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); continue }
                "%ACTIVITY_CLEAR_TOP%" -> { intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); continue }
                "%ACTIVITY_NO_HISTORY%" -> { intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY); continue }
            }
            val corte = p.indexOf("%=", 1)
            if (!p.startsWith("%") || corte < 0) continue
            val clave = p.substring(0, corte + 1)
            val v = valor(p.substring(corte + 2))
            when {
                clave == "%ACTION%" -> intent.action = v
                clave == "%CATEGORY%" -> intent.addCategory(v)
                clave == "%MIMETYPE%" -> mime = v
                clave == "%DATA%" -> data = Uri.parse(v)
                clave.startsWith("%EXTRABOOL_") -> intent.putExtra(clave.removePrefix("%EXTRABOOL_").removeSuffix("%"), v.equals("true", true))
                clave.startsWith("%EXTRAINTEGER_") -> intent.putExtra(
                    clave.removePrefix("%EXTRAINTEGER_").removeSuffix("%"),
                    v.toIntOrNull() ?: throw Falla("El archivo de ${juego.titulo} debe tener adentro un número."),
                )
                clave.startsWith("%EXTRAARRAY_") -> intent.putExtra(clave.removePrefix("%EXTRAARRAY_").removeSuffix("%"), v.split(',').toTypedArray())
                clave.startsWith("%EXTRA_") -> intent.putExtra(clave.removePrefix("%EXTRA_").removeSuffix("%"), v)
            }
        }
        when {
            data != null && mime != null -> intent.setDataAndType(data, mime)
            data != null -> intent.data = data
            mime != null -> intent.type = mime
        }
        return intent
    }

    /** Verifica el documento sin leer la ROM. Algunos proveedores no exponen metadatos. */
    fun revisarArchivo(ctx: Context, juego: Juego): String? = try {
        ctx.contentResolver.query(juego.uri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)
            ?.use { if (it.moveToFirst()) null else "El archivo no aparece. Comprueba la tarjeta SD y vuelve a escanear su carpeta." }
    } catch (_: SecurityException) {
        "Díptico perdió el permiso de lectura. Vuelve a elegir la carpeta donde está este juego."
    } catch (_: java.io.FileNotFoundException) {
        "El archivo no está disponible. Comprueba la tarjeta SD o si moviste el juego."
    } catch (_: IllegalArgumentException) {
        "La ubicación del juego ya no está disponible. Vuelve a elegir su carpeta."
    } catch (_: Exception) {
        null
    }

    /**
     * Abre el juego. Devuelve null si salió bien, o un mensaje para mostrar si falló.
     * [raiz] es la carpeta de juegos elegida (para rutas y archivos vecinos).
     */
    fun lanzar(ctx: Context, juego: Juego, consola: Consola, preferido: String?, displayId: Int?, raiz: Uri?): String? {
        // URI del juego dentro de la carpeta elegida: es la que Díptico puede compartir con el emulador.
        // (La versión sin "tree" no se puede compartir y por eso Eden decía que no había juego.)
        val rom = juego.uri
        val op = elegida(ctx, consola, preferido, juego.tipo)

        val base: Intent = try {
            when {
                op != null && op.comando.abreApp -> {
                    // Ports y juegos de Android de ES-DE: el archivo trae adentro el nombre de la app.
                    val texto = leerTexto(ctx, juego.uri) ?: return "El archivo ${juego.archivo} debe tener adentro el nombre de la app."
                    val pkg = texto.substringBefore('/').trim()
                    return lanzarApp(ctx, pkg, juego.titulo, displayId)
                }
                op != null -> construir(ctx, op, juego, consola, rom, raiz)
                consola.id == Consolas.RPGMAKER ->
                    return "Para abrir ${juego.titulo} necesitas instalar Essentia. Descárgala desde github.com/Nikalbazu24/essentia"
                consola.comandos.isEmpty() || juego.esCarpeta || juego.tipo in setOf("ps3", "ps4", "psvita") ->
                    return "Para abrir ${juego.titulo} necesitas instalar ${consola.sugerido}."
                else -> {
                    val ver = Intent(Intent.ACTION_VIEW).setDataAndType(rom, "*/*")
                    ver.clipData = ClipData.newRawUri("rom", rom)
                    ver.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    Intent.createChooser(ver, "Abrir ${juego.titulo} con… (te sugiero ${consola.sugerido})")
                }
            }
        } catch (f: Falla) {
            return f.mensaje
        } catch (e: Exception) {
            return "No pude preparar ${juego.titulo}: ${e.message ?: "error desconocido"}"
        }
        base.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Los accesos internos de Díptico (file://) no se comparten: el emulador recibe solo el código del juego.
        val conPermiso = if (juego.uri.scheme == "file") base else Intent(base).apply {
            clipData = ClipData.newRawUri("rom", rom).apply {
                addItem(ClipData.Item(juego.uri))
                raiz?.let { addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        val bundle = opcionesPantalla(displayId)
        return try {
            ctx.startActivity(conPermiso, bundle)
            null
        } catch (se: SecurityException) {
            try {
                ctx.startActivity(base, bundle)
                null
            } catch (e: Exception) {
                "No pude abrir ${juego.titulo}: ${e.message ?: "error desconocido"}"
            }
        } catch (nf: ActivityNotFoundException) {
            "No encontré con qué abrir ${consola.bonito}. Instala ${consola.sugerido}."
        } catch (e: Exception) {
            "No pude abrir ${juego.titulo}: ${e.message ?: "error desconocido"}"
        }
    }

    private fun opcionesPantalla(displayId: Int?): Bundle {
        val o = ActivityOptions.makeBasic()
        if (displayId != null) o.launchDisplayId = displayId
        return o.toBundle()
    }

    // ---------- Apps (ports y juegos de Android) ----------

    /** Todas las apps que se pueden abrir, ordenadas por nombre (sin Díptico). */
    fun apps(ctx: Context): List<App> {
        val pm = ctx.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(i, 0)
            .map { r ->
                val ai = r.activityInfo.applicationInfo
                App(r.activityInfo.packageName, r.loadLabel(pm).toString(), esJuego(ai))
            }
            .filter { it.paquete != ctx.packageName }
            .distinctBy { it.paquete }
            .sortedBy { it.nombre.lowercase() }
    }

    /** Paquetes de emuladores conocidos (para no confundirlos con juegos de Android). */
    private val emuladores: Set<String> by lazy {
        Consolas.reglas.values.flatten().map { it.substringBefore('/') }.toSet()
    }

    @Suppress("DEPRECATION")
    private fun esJuego(ai: ApplicationInfo): Boolean =
        ai.category == ApplicationInfo.CATEGORY_GAME || (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0

    /** Juegos de Android instalados (los que el sistema marca como juego), sin contar emuladores. */
    fun juegosAndroid(ctx: Context): List<App> = apps(ctx).filter { it.esJuego && it.paquete !in emuladores }

    fun nombreApp(ctx: Context, paquete: String): String? = try {
        val pm = ctx.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(paquete, 0)).toString()
    } catch (e: Exception) {
        null
    }

    /** Abre una app (un port o un juego de Android) en la pantalla elegida. */
    fun lanzarApp(ctx: Context, paquete: String, nombre: String, displayId: Int?): String? {
        val i = ctx.packageManager.getLaunchIntentForPackage(paquete) ?: return "$nombre ya no está instalado."
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(i, opcionesPantalla(displayId))
            null
        } catch (e: Exception) {
            try {
                ctx.startActivity(i)
                null
            } catch (e2: Exception) {
                "No pude abrir $nombre: ${e2.message ?: "error desconocido"}"
            }
        }
    }
}


package cl.nikalbazu.diptico.emu

import cl.nikalbazu.diptico.estado.*

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
    data class Opcion(val comando: Comando, val paquete: String?, val actividad: String?, val etiquetaApp: String? = null) {
        /** Lo que se guarda en los ajustes. */
        val id: String get() = comando.etiqueta
        val nombre: String get() = etiquetaApp ?: comando.nombre
    }

    /**
     * Cualquier app instalada usada como emulador (por si Díptico no conoce la tuya): se abre con el juego
     * como «ver archivo». Se guarda como "app:paquete".
     */
    fun appComoEmulador(ctx: Context, paquete: String, nombre: String? = null): Opcion? {
        if (!instalado(ctx, paquete)) return null
        val etiqueta = nombre ?: try {
            ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(paquete, 0)).toString()
        } catch (_: Exception) { paquete }
        return Opcion(Comando("app:$paquete", "%EMULATOR_APP% %ACTION%=android.intent.action.VIEW %DATA%=%ROMSAF%"), paquete, null, etiqueta)
    }

    private val instalados = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    /** Lo que se sabe de cada «paquete/actividad»: si existe y otra app la puede abrir. */
    private val pantallas = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    /** Olvida lo que se sabía de apps instaladas (al volver a Díptico, por si instalaste algo). */
    fun olvidar() {
        instalados.clear()
        pantallas.clear()
    }

    fun instalado(ctx: Context, paquete: String): Boolean = instalados.getOrPut(paquete) {
        try {
            ctx.packageManager.getPackageInfo(paquete, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** «paquete/actividad» de una regla → (paquete, actividad completa); la actividad relativa («.Main») se completa con el paquete. */
    internal fun separar(entrada: String): Pair<String, String> {
        val pkg = entrada.substringBefore('/')
        val act = entrada.substringAfter('/', "")
        return pkg to when {
            act.isEmpty() -> ""
            act.startsWith(".") -> pkg + act
            else -> act
        }
    }

    /**
     * Primer "paquete/actividad" de una regla de ES-DE que está instalado Y tiene esa pantalla abierta a otras apps.
     * Si el paquete está pero la pantalla no (otra versión, o una app que solo comparte el nombre del paquete), se
     * prueba el siguiente candidato.
     */
    private fun resolver(ctx: Context, emulador: String): Pair<String, String>? {
        for (e in Consolas.reglas[emulador] ?: return null) {
            val (pkg, act) = separar(e)
            if (!instalado(ctx, pkg)) continue
            // Gahvoria sin su entrada para lanzadores se sigue ofreciendo: `lanzar` abre su biblioteca.
            if (pkg != Consolas.PAQUETE_GAHVORIA && !actividadExiste(ctx, pkg, act)) continue
            return pkg to act
        }
        return null
    }

    /**
     * Un emulador de [consola] que está instalado pero sin la pantalla que Díptico usa para abrir juegos
     * (casi siempre una versión vieja o distinta). Devuelve su nombre, o null si no hay ninguno así.
     */
    fun instaladoSinPantalla(ctx: Context, consola: Consola): String? {
        for (c in consola.comandos) {
            val regla = c.emulador ?: continue
            for (e in Consolas.reglas[regla] ?: continue) {
                val (pkg, act) = separar(e)
                if (esCamuflado(pkg) || !instalado(ctx, pkg)) continue
                if (!actividadExiste(ctx, pkg, act)) return nombreApp(ctx, pkg) ?: c.nombre
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
        // Project Lemon (Switch 1): contrato oficial de AndroidManifest.xml,
        // dev.lemon.lemon_emulator + EmulationActivity exportada (ACTION_VIEW, content://, octet-stream).
        // El catálogo es general: cualquier juego puede elegir Project Lemon.
        // La compatibilidad real se valida al lanzar (Lemon emula Switch).
        val lemon = if (instalado(ctx, "dev.lemon.lemon_emulator")) listOf(
            Opcion(
                Comando("Project Lemon", "%EMULATOR_LEMON% %ACTION%=android.intent.action.VIEW %DATA%=%ROMSAF% %MIMETYPE%=application/octet-stream"),
                "dev.lemon.lemon_emulator",
                "dev.lemon.lemon_emu.activities.EmulationActivity",
                "Project Lemon",
            ),
        ) else emptyList()
        val todas = lemon + consola.comandos.mapNotNull { c ->
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
        // Project Lemon es seleccionable en cualquier juego, pero no debe pasar a ser
        // el emulador predeterminado de PS2, GBA, etc. por estar instalado.
        val compatibles = ops.filter {
            sirve(it.comando, tipo) && (consola.id == "switch" || it.paquete != "dev.lemon.lemon_emulator")
        }
        if (preferido != null && preferido.startsWith("app:")) appComoEmulador(ctx, preferido.removePrefix("app:"))?.let { return it }
        if (ops.isEmpty()) return null
        if (preferido != null) {
            val p = ops.firstOrNull { it.id == preferido }
                ?: preferido.substringAfter('#', "").takeIf { it.isNotEmpty() }?.let { core -> ops.firstOrNull { it.comando.nucleo == core } }
                ?: ops.firstOrNull { it.paquete == preferido && it.comando.nucleo == null }
            if (p != null && (tipo == null || sirve(p.comando, tipo))) return p
        }
        return compatibles.firstOrNull() ?: ops.firstOrNull { it.paquete != "dev.lemon.lemon_emulator" }
    }

    /** Convierte un documento de la memoria interna o SD en ruta real: primary:ROMs/x -> /storage/emulated/0/ROMs/x. */
    fun rutaReal(docId: String): String? {
        if (docId.startsWith("raw:")) return docId.removePrefix("raw:")
        val dos = docId.indexOf(':')
        if (dos <= 0) return null
        val volumen = docId.substring(0, dos)
        val resto = docId.substring(dos + 1)
        return when {
            volumen.equals("primary", true) -> "/storage/emulated/0/$resto"
            // La raíz «Documentos» del selector de Android (ExternalStorageProvider la llama "home").
            volumen.equals("home", true) -> "/storage/emulated/0/Documents/$resto"
            else -> "/storage/$volumen/$resto"
        }
    }

    /**
     * ¿El juego tiene una ruta real que otra app pueda abrir? Sí en la memoria interna, la tarjeta SD y las rutas directas;
     * no en Descargas recientes, la nube u otras apps (esos documentos no tienen ruta, solo permiso de Díptico).
     */
    fun rutaConfiable(uri: Uri, docId: String): Boolean {
        if (uri.scheme == "file" || docId.startsWith("raw:/")) return true
        if (uri.authority != "com.android.externalstorage.documents") return false
        val volumen = docId.substringBefore(':', "")
        return volumen.equals("primary", true) || volumen.equals("home", true) ||
            Regex("[0-9A-Fa-f]+(-[0-9A-Fa-f]+)+").matches(volumen) // tarjeta SD o USB: «1A2B-3C4D»
    }

    /**
     * Apps hechas con el motor nxnative (Banana Native y Peanuts Native): reciben la RUTA del .nup y lo abren ellas mismas
     * con «Acceso a todos los archivos», sin el permiso de carpeta que Díptico comparte con los emuladores.
     */
    private val REGLAS_POR_RUTA = setOf("BANANA-NATIVE", Integraciones.REGLA_PEANUTS)

    /** Lo que impide que una app nxnative abra [juego] (o null si se puede intentar), dicho para la persona. */
    fun avisoPorRuta(juego: Juego, app: String): String? {
        val ext = juego.archivo.substringAfterLast('.', "")
        if (!juego.esCarpeta && ext.equals("nup", true) && ext != "nup")
            return "$app solo lee archivos que terminan en «.nup» con minúsculas. Cambia el nombre a «${juego.archivo.substringBeforeLast('.')}.nup», " +
                "vuelve a buscar juegos en Díptico y ábrelo otra vez."
        if (!rutaConfiable(juego.uri, juego.docId))
            return "$app abre los juegos por su ruta, y ${juego.titulo} está en un lugar sin ruta (Descargas, la nube u otra app). " +
                "Mueve el archivo a una carpeta de la memoria interna o de la tarjeta SD, elige esa carpeta en Díptico " +
                "y dale a $app «Acceso a todos los archivos» (Ajustes de Android › Apps › $app › Permisos)."
        return null
    }

    private fun leerTexto(ctx: Context, uri: Uri): String? = try {
        ctx.contentResolver.openInputStream(uri)?.use { stream ->
            val lector = stream.bufferedReader()
            val buffer = CharArray(4096)
            val leidos = lector.read(buffer)
            if (leidos <= 0) null else String(buffer, 0, leidos).lineSequence().map(String::trim).firstOrNull(String::isNotEmpty)
        }
    } catch (e: Exception) {
        null
    }

    internal fun paqueteDeAcceso(ctx: Context, uri: Uri): String? =
        leerTexto(ctx, uri)?.removePrefix("\uFEFF")?.substringBefore('/')?.trim()?.takeIf { it.isNotEmpty() }

    internal fun appDisponible(ctx: Context, paquete: String): Boolean =
        ctx.packageManager.getLaunchIntentForPackage(paquete) != null

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
        // Solo se arma una ruta si el documento la tiene de verdad (memoria interna, SD, USB): en Descargas o la nube
        // rutaReal inventaría algo como «/storage/msf/123». No se usa File.exists(): sin «acceso a todos los archivos»
        // Díptico no ve esas rutas aunque el emulador sí.
        val ruta = if (juego.uri.scheme == "file") juego.uri.path
        else rutaReal(juego.docId)?.takeIf { rutaConfiable(juego.uri, juego.docId) }
        val raizRuta = raiz?.let { r ->
            try {
                val id = DocumentsContract.getTreeDocumentId(r)
                rutaReal(id)?.takeIf { r.authority == "com.android.externalstorage.documents" || id.startsWith("raw:/") }
            } catch (e: Exception) { null }
        }
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
            // Sin ruta real se manda el documento (content://): el emulador recibe el permiso de lectura y muchos lo abren igual.
            if ("%ROM%" in x) x = x.replace("%ROM%", ruta ?: rom.toString())
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
                    // App elegida a mano: Android busca dentro de ella la pantalla que abre el archivo.
                    if (op.comando.emulador == "APP") { intent.setPackage(op.paquete); continue }
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
    /**
     * Abre [juego]. Con [retomar] (el juego ya está abierto) se usa la misma orden pero sin «limpiar la tarea»:
     * Android trae la partida al frente tal como estaba, en vez de reiniciar el emulador.
     */
    fun lanzar(ctx: Context, juego: Juego, consola: Consola, preferido: String?, displayId: Int?, raiz: Uri?, retomar: Boolean = false): String? {
        // URI del juego dentro de la carpeta elegida: es la que Díptico puede compartir con el emulador.
        // (La versión sin "tree" no se puede compartir y por eso Eden decía que no había juego.)
        val rom = juego.uri
        val op = elegida(ctx, consola, preferido, juego.tipo)
        if (op != null && op.comando.emulador?.let { it in REGLAS_POR_RUTA } == true) avisoPorRuta(juego, op.nombre.substringBefore(" ("))?.let { return it }
        if (op?.paquete == "dev.lemon.lemon_emulator" && consola.id != "switch") {
            return "Project Lemon está seleccionado para ${juego.titulo}, pero únicamente ejecuta juegos compatibles de Nintendo Switch. Puedes elegir otro emulador en Ajustes del juego."
        }

        val base: Intent = try {
            when {
                op != null && op.comando.abreApp -> {
                    // Ports y juegos de Android de ES-DE: el archivo trae adentro el nombre de la app.
                    val pkg = paqueteDeAcceso(ctx, juego.uri) ?: return "El archivo ${juego.archivo} debe tener adentro el nombre de la app."
                    return lanzarApp(ctx, pkg, juego.titulo, displayId)
                }
                // Gahvoria sin su entrada para lanzadores (versiones antiguas): se abre su biblioteca y eliges el juego ahí.
                op != null && op.paquete == Consolas.PAQUETE_GAHVORIA && !actividadExiste(ctx, op.paquete, op.actividad) ->
                    return lanzarApp(ctx, op.paquete, "Gahvoria", displayId)
                op != null -> construir(ctx, op, juego, consola, rom, raiz)
                consola.id == Consolas.RPGMAKER ->
                    return "Para abrir ${juego.titulo} necesitas instalar Gahvoria. Descárgala desde github.com/Nikalbazu24/gahvoria"
                consola.comandos.isEmpty() || juego.esCarpeta || juego.tipo in setOf("ps3", "ps4", "psvita") ->
                    return instaladoSinPantalla(ctx, consola)?.let { app ->
                        "Tienes $app, pero esa versión no tiene la pantalla que Díptico usa para abrir juegos. Actualiza $app."
                    } ?: "Para abrir ${juego.titulo} necesitas instalar ${consola.sugerido}."
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
        if (retomar) base.flags = base.flags and (Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP).inv()

        // Los accesos internos de Díptico (file://) no se comparten: el emulador recibe solo el código del juego.
        fun compartido(permisos: Int): Intent = if (juego.uri.scheme == "file") base else Intent(base).apply {
            clipData = ClipData.newRawUri("rom", rom).apply {
                addItem(ClipData.Item(juego.uri))
                raiz?.let { addItem(ClipData.Item(it)) }
            }
            addFlags(permisos)
        }

        val bundle = opcionesPantalla(displayId)
        // Primero lectura y escritura (partidas junto al juego); si la carpeta es de solo lectura Android lo rechaza,
        // entonces solo lectura; y recién al final sin permisos.
        val intentos = listOf(
            compartido(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
            compartido(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            base,
        )
        for ((n, intento) in intentos.withIndex()) {
            try {
                ctx.startActivity(intento, bundle)
                return null
            } catch (se: SecurityException) {
                if (n == intentos.lastIndex) return "No pude abrir ${juego.titulo}: ${se.message ?: "Android no dio permiso"}"
            } catch (nf: ActivityNotFoundException) {
                val pkg = op?.paquete
                return if (pkg != null && instalado(ctx, pkg)) {
                    val app = nombreApp(ctx, pkg) ?: op?.nombre ?: pkg
                    "$app está instalado, pero su versión no tiene la pantalla que Díptico usa para abrir juegos. Actualiza $app o elige otro emulador."
                } else "No encontré con qué abrir ${consola.bonito}. Instala ${consola.sugerido}."
            } catch (e: Exception) {
                return "No pude abrir ${juego.titulo}: ${e.message ?: "error desconocido"}"
            }
        }
        return "No pude abrir ${juego.titulo}"
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

    /**
     * Prefijos de paquetes «camuflados»: algunos emuladores se publican con el nombre de un juego o benchmark famoso
     * (Genshin, PUBG, AnTuTu…) para que el equipo les dé más rendimiento. En las reglas van siempre al final.
     */
    private val CAMUFLADOS = listOf("com.miHoYo.", "com.antutu.", "com.tencent.", "com.ludashi.")

    internal fun esCamuflado(paquete: String): Boolean = CAMUFLADOS.any { paquete.startsWith(it) }

    /** Paquetes de emuladores conocidos (para no confundirlos con juegos de Android), sin los camuflados. */
    private val emuladores: Set<String> by lazy {
        Consolas.reglas.values.flatten().map { it.substringBefore('/') }.filterNot { esCamuflado(it) }.toSet()
    }

    /** Pantallas de emulador de cada paquete camuflado («com.tencent.ig» → la de Winlator, la de GameHub…). */
    private val pantallasCamufladas: Map<String, List<String>> by lazy {
        Consolas.reglas.values.flatten().map { separar(it) }.filter { esCamuflado(it.first) }
            .groupBy({ it.first }, { it.second })
    }

    /** ¿[paquete] es un emulador? Un camuflado solo lo es si de verdad trae la pantalla del emulador (si no, es el juego real). */
    private fun esEmulador(ctx: Context, paquete: String): Boolean =
        paquete in emuladores || pantallasCamufladas[paquete]?.any { actividadExiste(ctx, paquete, it) } == true

    @Suppress("DEPRECATION")
    private fun esJuego(ai: ApplicationInfo): Boolean =
        ai.category == ApplicationInfo.CATEGORY_GAME || (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0

    /** Juegos de Android instalados (los que el sistema marca como juego), sin contar emuladores. */
    fun juegosAndroid(ctx: Context): List<App> = apps(ctx).filter { it.esJuego && !esEmulador(ctx, it.paquete) }

    fun nombreApp(ctx: Context, paquete: String): String? = try {
        val pm = ctx.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(paquete, 0)).toString()
    } catch (e: Exception) {
        null
    }

    /** ¿La app [paquete] tiene la pantalla [actividad] y se puede abrir desde otra app? */
    private fun actividadExiste(ctx: Context, paquete: String?, actividad: String?): Boolean {
        if (paquete == null || actividad.isNullOrEmpty()) return true
        return pantallas.getOrPut("$paquete/$actividad") {
            try {
                ctx.packageManager.getActivityInfo(ComponentName(paquete, actividad), 0).exported
            } catch (_: Exception) { false }
        }
    }

    /** Abre una app (un port o un juego de Android) en la pantalla elegida. */
    fun lanzarApp(ctx: Context, paquete: String, nombre: String, displayId: Int?): String? {
        val i = ctx.packageManager.getLaunchIntentForPackage(paquete) ?: return try {
            ctx.packageManager.getPackageInfo(paquete, 0)
            "$nombre está instalado, pero no tiene ícono para abrirlo desde un lanzador."
        } catch (_: PackageManager.NameNotFoundException) {
            "$nombre ya no está instalado."
        }
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


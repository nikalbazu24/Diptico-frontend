package cl.nikalbazu.diptico.emu

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.net.Uri
import android.provider.DocumentsContract
import android.view.Display
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Emulador
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.data.ModoRom

/** Encuentra las dos pantallas de la Thor. En la Thor, la de arriba es la principal. */
object Pantallas {
    fun inferior(ctx: Context): Display? {
        val dm = ctx.getSystemService(DisplayManager::class.java) ?: return null
        val presentacion = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        return presentacion ?: dm.displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
    }
}

/** Una app instalada que se puede abrir (para elegir ports). */
data class App(val paquete: String, val nombre: String)

/** Abre juegos con su emulador, en la pantalla elegida. */
object Lanzador {
    /** Todas las apps que se pueden abrir, ordenadas por nombre (sin Díptico). */
    fun apps(ctx: Context): List<App> {
        val pm = ctx.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(i, 0)
            .map { App(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.paquete != ctx.packageName }
            .distinctBy { it.paquete }
            .sortedBy { it.nombre.lowercase() }
    }

    fun nombreApp(ctx: Context, paquete: String): String? = try {
        val pm = ctx.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(paquete, 0)).toString()
    } catch (e: Exception) {
        null
    }

    /** Abre una app (un port) en la pantalla elegida. */
    fun lanzarApp(ctx: Context, paquete: String, nombre: String, displayId: Int?): String? {
        val i = ctx.packageManager.getLaunchIntentForPackage(paquete) ?: return "$nombre ya no está instalado."
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opciones = ActivityOptions.makeBasic()
        if (displayId != null) opciones.launchDisplayId = displayId
        return try {
            ctx.startActivity(i, opciones.toBundle())
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

    data class Opcion(val emulador: Emulador, val paquete: String) {
        /** Lo que se guarda en los ajustes: "paquete" o "paquete#núcleo". */
        val id: String get() = emulador.id(paquete)
    }

    fun instalado(ctx: Context, paquete: String): Boolean = try {
        ctx.packageManager.getPackageInfo(paquete, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** Emuladores instalados para la consola. Si se indica [tipo], solo los que sirven para ese tipo de archivo. */
    fun opciones(ctx: Context, consola: Consola, tipo: String? = null): List<Opcion> =
        consola.emuladores
            .filter { tipo == null || it.sirvePara(tipo) }
            .flatMap { e -> e.paquetes.filter { instalado(ctx, it) }.map { Opcion(e, it) } }

    fun elegida(ctx: Context, consola: Consola, preferido: String?, tipo: String? = null): Opcion? {
        val ops = opciones(ctx, consola, tipo)
        return ops.firstOrNull { it.id == preferido }
            ?: ops.firstOrNull { it.paquete == preferido && it.emulador.nucleo == null }
            ?: ops.firstOrNull()
    }

    /** Convierte un documento de la memoria interna o SD en ruta real: primary:ROMs/x -> /storage/emulated/0/ROMs/x. */
    private fun rutaReal(docId: String): String? {
        if (docId.startsWith("raw:")) return docId.removePrefix("raw:")
        val dos = docId.indexOf(':')
        if (dos <= 0) return null
        val volumen = docId.substring(0, dos)
        val resto = docId.substring(dos + 1)
        return if (volumen.equals("primary", true)) "/storage/emulated/0/$resto" else "/storage/$volumen/$resto"
    }

    /** Lee el código guardado dentro de un archivo .ps3 / .ps4 (por ejemplo "CUSA00900"). */
    private fun leerCodigo(ctx: Context, uri: Uri): String? = try {
        ctx.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            ?.lineSequence()?.map { it.trim() }?.firstOrNull { it.isNotEmpty() }
    } catch (e: Exception) {
        null
    }

    private fun construir(ctx: Context, op: Opcion, juego: Juego, rom: Uri): Intent? {
        val e = op.emulador
        val i = Intent()
        i.component = ComponentName(op.paquete, e.componente(op.paquete))
        e.accion?.let { i.action = it }
        e.categoria?.let { i.addCategory(it) }
        when (e.modo) {
            ModoRom.DATA -> i.data = rom
            ModoRom.EXTRA -> i.putExtra(e.extraRom, rom.toString())
            ModoRom.RUTA -> i.putExtra(e.extraRom, rutaReal(juego.docId) ?: return null)
            ModoRom.INYECTAR -> i.putExtra(e.extraRom, leerCodigo(ctx, juego.uri) ?: return null)
        }
        for ((k, v) in e.extrasBool) i.putExtra(k, v)
        for ((k, v) in e.extrasTexto) i.putExtra(k, v.replace("{pkg}", op.paquete).replace("{core}", e.nucleo ?: ""))
        if (e.limpiarTarea) i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return i
    }

    /**
     * Abre el juego. Devuelve null si salió bien, o un mensaje para mostrar si falló.
     * Si no hay un emulador conocido instalado, ofrece elegir una app.
     */
    fun lanzar(ctx: Context, juego: Juego, consola: Consola, preferido: String?, displayId: Int?): String? {
        val rom = DocumentsContract.buildDocumentUri(juego.uri.authority, juego.docId)
        val op = elegida(ctx, consola, preferido, juego.tipo)

        if (op == null && (juego.tipo == "ps3" || juego.tipo == "ps4" || juego.esCarpeta)) {
            val sugerido = consola.emuladores.firstOrNull()?.nombre ?: "un emulador"
            return "Para abrir ${juego.titulo} necesitas $sugerido instalado."
        }

        val base = if (op != null) {
            construir(ctx, op, juego, rom) ?: return when (op.emulador.modo) {
                ModoRom.INYECTAR -> "El archivo ${juego.archivo} debe tener adentro el código del juego (por ejemplo CUSA00900)."
                else -> "No pude encontrar la ruta de ${juego.titulo}. Usa una carpeta de la memoria interna o la SD."
            }
        } else {
            val ver = Intent(Intent.ACTION_VIEW).setDataAndType(rom, "*/*")
            ver.clipData = ClipData.newRawUri("rom", rom)
            ver.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            Intent.createChooser(ver, "Abrir ${juego.titulo} con…")
        }
        base.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val conPermiso = Intent(base).apply {
            clipData = ClipData.newRawUri("rom", rom).apply { addItem(ClipData.Item(juego.uri)) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        val opciones = ActivityOptions.makeBasic()
        if (displayId != null) opciones.launchDisplayId = displayId
        val bundle = opciones.toBundle()

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
            val sugerido = consola.emuladores.firstOrNull()?.nombre ?: "un emulador"
            "No encontré con qué abrir ${consola.nombre}. Instala $sugerido."
        } catch (e: Exception) {
            "No pude abrir ${juego.titulo}: ${e.message ?: "error desconocido"}"
        }
    }
}

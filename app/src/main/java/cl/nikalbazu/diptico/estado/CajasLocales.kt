package cl.nikalbazu.diptico.estado

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import cl.nikalbazu.diptico.data.Fichas
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Las cajas 3D viven en el equipo para siempre (hasta que la persona las quite): cada cara bajada de internet se guarda
 * como archivo propio de Díptico en filesDir/cajas-guardadas/<juego>/ (nunca en la caché, que Android puede vaciar),
 * con escritura atómica y revisando que sea una imagen de verdad. Así la caja se ve sin internet, tras reiniciar y
 * tras actualizar Díptico. Las cajas armadas por la persona y las de la comunidad siguen en filesDir/cajas/.
 */
object CajasLocales {
    const val CARPETA = "cajas-guardadas"
    /** Carpetas de filesDir con arte de cajas (las dos viajan en el respaldo). */
    val CARPETAS = listOf(CARPETA, "cajas")
    private const val MINIMO = 512L
    private const val MAXIMO = 25L * 1024 * 1024
    /** En el respaldo, las rutas locales van así (relativas), para que sirvan en otro equipo. */
    private const val LOCAL = "local:"

    fun esRemota(s: String?): Boolean = s != null && (s.startsWith("https://") || s.startsWith("http://"))

    private fun sha(t: String): String = MessageDigest.getInstance("SHA-256").digest(t.toByteArray())
        .joinToString("") { "%02x".format(it) }

    /** El enlace sin claves de ScreenScraper/TheGamesDB (para nombrar el archivo y no guardar claves en ningún lado). */
    fun sinClaves(url: String): String {
        val q = url.substringAfter('?', "")
        if (q.isEmpty()) return url
        val privados = setOf("devid", "devpassword", "ssid", "sspassword", "apikey", "softname", "key", "token")
        val limpio = q.split('&').filter { it.substringBefore('=').lowercase() !in privados }.joinToString("&")
        return url.substringBefore('?') + if (limpio.isEmpty()) "" else "?$limpio"
    }

    fun carpetaJuego(ctx: Context, clave: String): File = File(ctx.filesDir, "$CARPETA/${sha(clave).take(20)}")

    /** El archivo de una cara si es local y existe. */
    fun archivo(s: String?): File? {
        if (s == null || !s.startsWith("file:")) return null
        return Uri.parse(s).path?.let(::File)?.takeIf { it.isFile && it.length() > 0 }
    }

    /** ¿Los bytes del archivo son una imagen que Android sabe leer? */
    fun esImagen(f: File): Boolean {
        if (f.length() !in MINIMO..MAXIMO) return false
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        return try { BitmapFactory.decodeFile(f.path, o); o.outWidth > 0 && o.outHeight > 0 } catch (_: Exception) { false }
    }

    /**
     * Baja una cara a los archivos de Díptico. Si ya estaba (mismo enlace), no la vuelve a bajar.
     * Devuelve el archivo, o null si no hay red o lo que llegó no era una imagen.
     */
    fun bajarCara(ctx: Context, url: String, clave: String, cara: String): File? {
        val dir = carpetaJuego(ctx, clave).apply { mkdirs() }
        val destino = File(dir, "$cara-${sha(sinClaves(url)).take(12)}.img")
        if (destino.isFile && esImagen(destino)) return destino
        val tmp = File(dir, destino.name + ".tmp")
        val c = try { URL(url).openConnection() as HttpURLConnection } catch (_: Exception) { return null }
        return try {
            c.connectTimeout = 12_000
            c.readTimeout = 25_000
            c.instanceFollowRedirects = true
            c.setRequestProperty("User-Agent", Fichas.AGENTE)
            if (c.responseCode !in 200..299) return null
            if (c.contentLengthLong > MAXIMO) return null
            c.inputStream.use { i -> tmp.outputStream().use { o -> copiarLimitado(i, o) } }
            if (!esImagen(tmp)) { tmp.delete(); return null }
            if (destino.exists()) destino.delete()
            if (tmp.renameTo(destino)) destino else { tmp.delete(); null }
        } catch (_: Exception) {
            tmp.delete(); null
        } finally {
            c.disconnect()
        }
    }

    private fun copiarLimitado(i: java.io.InputStream, o: java.io.OutputStream) {
        val b = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val n = i.read(b)
            if (n < 0) return
            total += n
            require(total <= MAXIMO)
            o.write(b, 0, n)
        }
    }

    /** Pasa a archivos locales las caras que todavía son enlaces (las que no se pueden bajar ahora quedan para después). */
    fun localizar(ctx: Context, clave: String, g: CarasGuardadas): CarasGuardadas {
        fun una(s: String?, cara: String): String? =
            if (!esRemota(s)) s else bajarCara(ctx, s!!, clave, cara)?.let { Uri.fromFile(it).toString() } ?: s
        return g.copy(frente = una(g.frente, "frente"), lomo = una(g.lomo, "lomo"), reverso = una(g.reverso, "reverso"), disco = una(g.disco, "disco"))
    }

    /** Lo nuevo se suma a lo de antes: una búsqueda que no encontró una cara nunca borra la que ya había. */
    fun combinar(antes: CarasGuardadas?, nuevo: CarasGuardadas): CarasGuardadas = if (antes == null) nuevo else CarasGuardadas(
        reverso = nuevo.reverso ?: antes.reverso, lomo = nuevo.lomo ?: antes.lomo, buscado = maxOf(nuevo.buscado, antes.buscado),
        frente = nuevo.frente ?: antes.frente, origen = if (nuevo.tiene) nuevo.origen ?: antes.origen else antes.origen,
        disco = nuevo.disco ?: antes.disco,
        // Lo que eligió la persona en el Estudio (forma, color, material) se queda aunque cambien las imágenes.
        plantilla = nuevo.plantilla ?: antes.plantilla, color = nuevo.color ?: antes.color, material = nuevo.material ?: antes.material,
        nombre = nuevo.nombre ?: antes.nombre,
    )

    /** ¿Quedan enlaces por bajar? */
    fun pendiente(g: CarasGuardadas): Boolean = g.caras().any(::esRemota)

    /**
     * Al abrir Díptico: las rutas del respaldo («local:…») o de otra instalación pasan a la carpeta de este equipo, y las
     * caras cuyo archivo ya no existe se sueltan (así la caja se vuelve a buscar sola en vez de quedar vacía).
     */
    fun rebasar(ctx: Context, m: Map<String, CarasGuardadas>): Map<String, CarasGuardadas> {
        val base = ctx.filesDir
        fun una(s: String?): String? {
            if (s == null || esRemota(s)) return s
            val rel = when {
                s.startsWith(LOCAL) -> s.removePrefix(LOCAL)
                s.startsWith("file:") -> Uri.parse(s).path?.let { p -> CARPETAS.firstNotNullOfOrNull { c -> p.indexOf("/$c/").takeIf { it >= 0 }?.let { p.substring(it + 1) } } }
                else -> null
            } ?: return s.takeIf { !it.startsWith("file:") || archivo(it) != null }
            if (".." in rel) return null
            val f = File(base, rel)
            return if (f.isFile && f.length() > 0) Uri.fromFile(f).toString() else null
        }
        return m.mapValues { (_, g) ->
            val n = g.copy(frente = una(g.frente), lomo = una(g.lomo), reverso = una(g.reverso), disco = una(g.disco))
            if (!n.tiene && g.tiene) n.copy(origen = if (g.propia && n.conEstilo) "propia" else null, buscado = 0L) else n
        }
    }

    /**
     * Versión para el respaldo: sin enlaces (pueden llevar claves de ScreenScraper) y con rutas relativas.
     * [incluidos] = archivos que sí van en el zip; las caras cuyo archivo no viaja se dejan fuera (se buscarán de nuevo).
     */
    fun paraRespaldo(ctx: Context, texto: String, incluidos: Set<String>): String {
        val base = ctx.filesDir.path.trimEnd('/') + "/"
        fun una(s: String?): String? {
            val f = archivo(s) ?: return null
            val rel = f.path.removePrefix(base).takeIf { it != f.path } ?: return null
            return if (rel in incluidos) LOCAL + rel else null
        }
        val m = CarasGuardadas.leer(texto).mapValues { (_, g) ->
            g.copy(frente = una(g.frente), lomo = una(g.lomo), reverso = una(g.reverso), disco = una(g.disco))
        }.filterValues { it.tiene || it.conEstilo }
        return CarasGuardadas.escribir(m)
    }

    /**
     * Qué archivos de cajas viajan en el respaldo, sin pasar de [tope] bytes: primero las armadas por la persona
     * (no se pueden volver a bajar), después el resto, del más nuevo al más viejo. Rutas relativas a filesDir.
     */
    fun archivosParaRespaldo(ctx: Context, texto: String, tope: Long): List<File> {
        val m = CarasGuardadas.leer(texto)
        val orden = m.values.sortedWith(compareBy<CarasGuardadas>({ if (it.propia) 0 else 1 }, { -it.buscado }))
        val salida = ArrayList<File>()
        var total = 0L
        val vistos = HashSet<String>()
        for (g in orden) for (s in g.caras()) {
            val f = archivo(s) ?: continue
            if (!vistos.add(f.path)) continue
            if (CARPETAS.none { f.path.startsWith(File(ctx.filesDir, it).path + "/") }) continue
            if (total + f.length() > tope) continue
            total += f.length()
            salida += f
        }
        return salida
    }

    /** Borra los archivos de [g] que ya no usa nadie (solo dentro de las carpetas de cajas de Díptico). */
    fun borrarSinUso(ctx: Context, g: CarasGuardadas?, siguen: Collection<CarasGuardadas>) {
        if (g == null) return
        // Rutas canónicas en los dos lados (en Android /data/user/0 es un enlace a /data/data: compararlas tal cual fallaría).
        fun canonica(f: File): String? = try { f.canonicalPath } catch (_: Exception) { null }
        val enUso = siguen.flatMap { it.caras() }.mapNotNull { archivo(it)?.let(::canonica) }.toSet()
        val raices = CARPETAS.map { File(ctx.filesDir, it).canonicalPath + "/" }
        for (s in g.caras()) {
            val f = archivo(s) ?: continue
            val ruta = canonica(f) ?: continue
            if (ruta in enUso || raices.none { ruta.startsWith(it) }) continue
            f.delete()
            f.parentFile?.takeIf { it.list()?.isEmpty() == true }?.delete()
        }
    }
}

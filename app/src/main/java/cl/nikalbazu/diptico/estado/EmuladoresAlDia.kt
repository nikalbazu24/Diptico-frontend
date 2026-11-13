package cl.nikalbazu.diptico.estado

import android.content.Context
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.emu.Lanzador
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/*
 * «Emuladores al día»: un actualizador tipo Obtainium dentro de Díptico. DIPPO mira qué emuladores tienes instalados,
 * cuáles usas (y cuánto), busca la versión más nueva en su página de publicaciones (GitHub o Forgejo), la baja y se la
 * entrega a Android para instalarla encima. Los que solo se publican en una tienda o en su web se revisan allá.
 */

/** De dónde sale cada emulador. [repo] es «dueño/proyecto»; [host] vacío = GitHub. Sin [repo], solo hay [web]. */
data class FuenteEmulador(
    val nombre: String,
    val paquetes: List<String>,
    val repo: String = "",
    val host: String = "",
    val web: String = "",
    val nota: String = "",
)

/** Lo que DIPPO sabe de un emulador instalado después de revisarlo. */
data class EmuRevisado(
    val nombre: String,
    val paquete: String,
    val version: String,
    val instaladoEn: Long,
    /** Consolas para las que es el emulador elegido, y cuánto se jugó en ellas (segundos). */
    val consolas: List<String> = emptyList(),
    val usoSeg: Long = 0L,
    /** Publicación más nueva encontrada (null = no se pudo revisar o no publica APK). */
    val ultima: String? = null,
    val url: String? = null,
    val bytes: Long = 0L,
    val notas: String = "",
    val nueva: Boolean = false,
    val pagina: String = "",
    /** Por qué no se puede actualizar desde aquí (tienda, web, sin APK…). */
    val aviso: String = "",
)

object EmuladoresAlDia {
    val FUENTES = listOf(
        FuenteEmulador("Eden", listOf("dev.eden.eden_emulator", "dev.legacy.eden_emulator"), "eden-emu/eden", "git.eden-emu.dev", "https://eden-emu.dev"),
        FuenteEmulador("NovaS", listOf("io.github.nova5emu.novas"), "nova5emu/novas", web = "https://github.com/nova5emu/novas/releases"),
        FuenteEmulador("Azahar", listOf("org.azahar_emu.azahar", "io.github.lime3ds.android"), "azahar-emu/azahar", web = "https://azahar-emu.org"),
        FuenteEmulador("Mandarine", listOf("io.github.mandarine3ds.mandarine"), "mandarine3ds/mandarine"),
        FuenteEmulador("Pandroid", listOf("com.panda3ds.pandroid"), "wheremyfoodat/Panda3DS"),
        FuenteEmulador("melonDS", listOf("me.magnum.melonds"), "rafaelvcaetano/melonDS-android"),
        FuenteEmulador("Vita3K", listOf("org.vita3k.emulator"), "Vita3K/Vita3K-Android"),
        FuenteEmulador("ARMSX2", listOf("com.armsx2", "com.nanodata.armsx2", "come.nanodata.armsx2", "com.nanodata.armsx"), "ARMSX2/ARMSX2"),
        FuenteEmulador("Flycast", listOf("com.flycast.emulator"), "flyinghead/flycast"),
        FuenteEmulador("Cemu", listOf("info.cemu.cemu", "info.cemu.Cemu"), "SSimco/Cemu"),
        FuenteEmulador("aPS3e", listOf("aenu.aps3e"), "aenu1/aps3e"),
        FuenteEmulador("Mupen64Plus FZ", listOf("org.mupen64plusae.v3.fzurita", "org.mupen64plusae.v3.alpha"), "mupen64plus-ae/mupen64plus-ae"),
        FuenteEmulador("Winlator", listOf("com.winlator", "com.winlator.vanilla"), "brunodev85/winlator"),
        FuenteEmulador("Winlator Cmod", listOf("com.winlator.cmod", "com.cmodded.winlator"), "coffincolors/winlator"),
        FuenteEmulador("GameNative", listOf("app.gamenative"), "utkarshdalal/GameNative"),
        FuenteEmulador("EKA2L1", listOf("com.github.eka2l1"), "EKA2L1/EKA2L1"),
        FuenteEmulador("Ruffle", listOf("rs.ruffle"), "ruffle-rs/ruffle-android"),
        FuenteEmulador("Gahvoria", listOf("io.github.nikalbazu24.gahvoria"), "Nikalbazu24/gahvoria", nota = "Fan games de RPG Maker y Pokémon Essentials"),
        FuenteEmulador("J2ME Loader", listOf("ru.playsoftware.j2meloader"), "nikita36078/J2ME-Loader"),
        FuenteEmulador("Citron", listOf("org.citron.citron_emu"), web = "https://citron-emu.org", nota = "Se publica en su web"),
        FuenteEmulador("Kenji-NX", listOf("org.kenjinx.android"), web = "https://git.ryujinx.app/kenji-nx", nota = "Se publica en su web"),
        FuenteEmulador("Dolphin", listOf("org.dolphinemu.dolphinemu", "org.dolphinemu.mmjr"), web = "https://dolphin-emu.org/download/", nota = "Se actualiza desde Play Store o su web"),
        FuenteEmulador("PPSSPP", listOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold"), web = "https://www.ppsspp.org/download/", nota = "Se actualiza desde Play Store o su web"),
        FuenteEmulador("DuckStation", listOf("com.github.stenzek.duckstation"), web = "https://www.duckstation.org/android/", nota = "Se actualiza desde Play Store"),
        FuenteEmulador("RetroArch", listOf("com.retroarch.aarch64", "com.retroarch", "com.retroarch.ra32"), web = "https://www.retroarch.com/?page=platforms", nota = "Se actualiza desde Play Store o su web"),
        FuenteEmulador("NetherSX2 / AetherSX2", listOf("xyz.aethersx2.android", "xyz.aethersx2.cturnip", "xyz.aethersx2.tturnip"), web = "https://github.com/Trixarian/NetherSX2-classic", nota = "Ya no recibe versiones nuevas: ARMSX2 es su sucesor"),
        FuenteEmulador("DraStic", listOf("com.dsemu.drastic"), web = "https://play.google.com/store/apps/details?id=com.dsemu.drastic", nota = "Se actualiza desde Play Store"),
        FuenteEmulador("Yaba Sanshiro 2", listOf("org.devmiyax.yabasanshioro2", "org.devmiyax.yabasanshioro2.pro"), web = "https://www.uoyabause.org", nota = "Se actualiza desde Play Store"),
        FuenteEmulador("Redream", listOf("io.recompiled.redream"), web = "https://redream.io/download", nota = "Se actualiza desde Play Store o su web"),
        FuenteEmulador("MAME4droid", listOf("com.seleuco.mame4d2024", "com.seleuco.mame4droid"), web = "https://github.com/seleuco/MAME4droid-2024", nota = "Se actualiza desde Play Store"),
        FuenteEmulador("ScummVM", listOf("org.scummvm.scummvm"), web = "https://www.scummvm.org/downloads/", nota = "Se actualiza desde Play Store o su web"),
        FuenteEmulador("Banana Native", listOf("dev.bananative.android"), nota = "Aún no tiene página pública"),
    ) + Nativos.PORTS.map { p ->
        // Los ports nativos también se actualizan: su repo sale del enlace de GitHub.
        FuenteEmulador(p.nombre, listOf(p.paquete), p.enlace.removePrefix("https://github.com/").trimEnd('/'), web = p.enlace)
    }

    /** La fuente de [paquete] (por prefijo, para cubrir variantes .nightly, .debug…). */
    fun fuenteDe(paquete: String): FuenteEmulador? =
        FUENTES.firstOrNull { f -> f.paquetes.any { paquete == it || paquete.startsWith("$it.") } }

    /** Los números de una versión («v0.9.1-rc2» → [0, 9, 1, 2]); vacío si no tiene números. */
    fun numeros(v: String): List<Int> = Regex("""\d+""").findAll(v.substringAfterLast('/')).mapNotNull { it.value.toIntOrNull() }.take(5).toList()

    /**
     * ¿La publicación [etiqueta] (del [publicada]) es más nueva que lo instalado ([instalada], instalado el [instaladoEn])?
     * Compara los números de versión; si no se pueden comparar (nombres distintos, compilaciones por fecha), manda la fecha.
     */
    fun esMasNueva(instalada: String, etiqueta: String, instaladoEn: Long, publicada: Long): Boolean {
        val a = numeros(instalada)
        val b = numeros(etiqueta)
        // Una versión con fecha (20261105) contra una normal no se comparan por número.
        val comparables = a.size >= 2 && b.size >= 2 && (a[0] < 1000) == (b[0] < 1000)
        if (comparables) {
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return y > x
            }
            return false
        }
        // 12 horas de margen: la publicación tiene que ser claramente posterior a cuando lo instalaste.
        return publicada > 0 && instaladoEn > 0 && publicada > instaladoEn + 12 * 3_600_000L
    }

    /** Elige el APK que sirve en la Thor (arm64, o el universal) entre los de una publicación. */
    fun elegirApk(nombres: List<String>, paquete: String): String? {
        val apks = nombres.filter { it.endsWith(".apk", ignoreCase = true) }
        if (apks.isEmpty()) return null
        val nocturna = paquete.endsWith(".nightly")
        val utiles = apks.filterNot { n ->
            val x = n.lowercase()
            "x86" in x || "armeabi" in x || "v7a" in x || "debug" in x || ("nightly" in x) != nocturna && apks.size > 1
        }.ifEmpty { apks }
        return utiles.maxByOrNull { n ->
            val x = n.lowercase()
            (if ("arm64" in x || "v8a" in x || "aarch64" in x) 3 else 0) + (if ("universal" in x) 2 else 0) +
                (if ("legacy" in x) (if ("legacy" in paquete) 4 else -4) else 0) + (if ("optimized" in x || "pgo" in x) 1 else 0)
        }
    }

    /** La publicación más nueva con APK (o null). Usa la API de GitHub o la de Forgejo, que tienen la misma forma. */
    fun buscarPublicacion(f: FuenteEmulador, paquete: String): Triple<JsonPub, String, Long>? {
        if (f.repo.isBlank()) return null
        val api = if (f.host.isBlank()) "https://api.github.com/repos/${f.repo}/releases?per_page=6"
        else "https://${f.host}/api/v1/repos/${f.repo}/releases?limit=6"
        val c = URL(api).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        c.setRequestProperty("Accept", "application/json")
        c.setRequestProperty("User-Agent", "Diptico")
        val texto = try { if (c.responseCode != 200) return null; c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
        val lista = JSONArray(texto)
        val nocturna = paquete.endsWith(".nightly")
        val pubs = (0 until lista.length()).map { lista.getJSONObject(it) }.filterNot { it.optBoolean("draft") }
        // Las estables primero (salvo que tengas la nocturna); si no hay estables con APK, la preliminar más nueva.
        val orden = if (nocturna) pubs else pubs.sortedBy { it.optBoolean("prerelease") }
        for (r in orden) {
            val assets = r.optJSONArray("assets") ?: continue
            val nombres = (0 until assets.length()).map { assets.getJSONObject(it) }
            val elegido = elegirApk(nombres.map { it.optString("name") }, paquete) ?: continue
            val a = nombres.first { it.optString("name") == elegido }
            val pub = JsonPub(
                r.optString("tag_name").ifBlank { r.optString("name") },
                fecha(r.optString("published_at")),
                r.optString("body").lineSequence().map { it.trim().trimStart('#', '*', '-', ' ') }.filter { it.isNotBlank() }.take(4).joinToString("\n"),
            )
            return Triple(pub, a.optString("browser_download_url"), a.optLong("size"))
        }
        return null
    }

    data class JsonPub(val etiqueta: String, val fecha: Long, val notas: String)

    private fun fecha(iso: String): Long = try { java.time.Instant.parse(iso).toEpochMilli() } catch (_: Exception) { 0L }

    /** Versión y fecha de instalación de [paquete] (null si no está). */
    fun instalado(ctx: Context, paquete: String): Pair<String, Long>? = try {
        val pi = ctx.packageManager.getPackageInfo(paquete, 0)
        (pi.versionName ?: "?") to pi.lastUpdateTime
    } catch (_: Exception) { null }

    /** ¿El APK bajado es de [paquete] y viene firmado igual que el instalado? (si no, Android no lo deja instalar encima). */
    fun mismaFirma(ctx: Context, apk: File, paquete: String): Boolean? {
        if (Build.VERSION.SDK_INT < 28) return null
        return try {
            val pm = ctx.packageManager
            val nuevo = pm.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNING_CERTIFICATES) ?: return null
            if (nuevo.packageName != paquete) return false
            val a = pm.getPackageInfo(paquete, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet()
            val b = nuevo.signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet()
            if (a == null || b == null) null else a == b
        } catch (_: Exception) { null }
    }

    /** Lo que dice DIPPO después de revisar. */
    fun comentario(lista: List<EmuRevisado>): String {
        if (lista.isEmpty()) return "No encontré emuladores instalados que conozca. Cuando instales uno, aparece aquí solo."
        val nuevas = lista.filter { it.nueva }
        val usado = lista.filter { it.usoSeg > 0 }.maxByOrNull { it.usoSeg }
        return buildString {
            append("Revisé tus ${lista.size} emuladores. ")
            append(when (nuevas.size) {
                0 -> "Todos los que puedo revisar están al día."
                1 -> "${nuevas[0].nombre} tiene versión nueva."
                else -> "${nuevas.size} tienen versión nueva: ${nuevas.take(3).joinToString(", ") { it.nombre }}${if (nuevas.size > 3) "…" else ""}."
            })
            if (usado != null) append(" El que más usas es ${usado.nombre} (${usado.consolas.size} ${if (usado.consolas.size == 1) "consola" else "consolas"}, ${usado.usoSeg / 3600} h).")
        }
    }
}

// ---------- En Díptico ----------

/** Cuántas veces al día se revisa sola (como mínimo cada 20 h). */
private const val CADA_MS = 20 * 3_600_000L

/** Arma la lista de emuladores instalados que se conocen, con su uso (sin internet). */
internal fun Diptico.emuladoresInstalados(e: Estado): List<EmuRevisado> {
    // Qué emulador abre cada consola con juegos, y cuánto se jugó en ella.
    val uso = HashMap<String, Pair<MutableList<String>, Long>>()
    for (c in e.consolasConJuegos) {
        val pkg = runCatching { Lanzador.elegida(app, c, e.emuladores[c.id])?.paquete }.getOrNull() ?: continue
        val seg = e.juegosPorConsola[c.id].orEmpty().sumOf { e.datos[it.clave]?.tiempo?.toLong() ?: 0L }
        val (l, s) = uso.getOrPut(pkg) { mutableListOf<String>() to 0L }
        l += c.nombre
        uso[pkg] = l to (s + seg)
    }
    val vistos = HashSet<String>()
    val lista = mutableListOf<EmuRevisado>()
    for (f in EmuladoresAlDia.FUENTES) for (base in f.paquetes) {
        for (pkg in listOf(base, "$base.nightly")) {
            if (!vistos.add(pkg)) continue
            val (v, cuando) = EmuladoresAlDia.instalado(app, pkg) ?: continue
            val (cs, seg) = uso[pkg] ?: (emptyList<String>() to 0L)
            lista += EmuRevisado(
                f.nombre + if (pkg.endsWith(".nightly")) " (nocturna)" else "", pkg, v, cuando, cs, seg,
                pagina = f.web.ifBlank { if (f.repo.isNotBlank()) "https://${f.host.ifBlank { "github.com" }}/${f.repo}/releases" else "" },
                aviso = if (f.repo.isBlank()) f.nota.ifBlank { "Se actualiza desde su página" } else "",
            )
        }
    }
    return lista
}

/** Orden para mostrar: los que tienen versión nueva, luego los que usas, luego el resto. */
internal fun ordenEmus(l: List<EmuRevisado>) = l.sortedWith(compareBy({ !it.nueva }, { -it.usoSeg }, { it.nombre.lowercase() }))

/** DIPPO revisa todos los emuladores instalados (con internet). [silencioso] = la revisión automática diaria. */
fun Diptico.revisarEmuladores(silencioso: Boolean = false) {
    if (estado.value.extra.emusRevisando) return
    actualizar { it.copy(extra = it.extra.copy(emusRevisando = true, emus = ordenEmus(emuladoresInstalados(it)))) }
    scope.launch {
        val base = estado.value.extra.emus
        val revisados = withContext(Dispatchers.IO) {
            base.map { em ->
                val f = EmuladoresAlDia.fuenteDe(em.paquete) ?: return@map em
                if (f.repo.isBlank()) return@map em
                val r = runCatching { EmuladoresAlDia.buscarPublicacion(f, em.paquete) }.getOrNull()
                    ?: return@map em.copy(aviso = "No encontré su APK publicado: revisa su página")
                val (pub, url, bytes) = r
                em.copy(
                    ultima = pub.etiqueta, url = url, bytes = bytes, notas = pub.notas,
                    nueva = EmuladoresAlDia.esMasNueva(em.version, pub.etiqueta, em.instaladoEn, pub.fecha),
                )
            }
        }
        almacen.guardarTexto("emusRevisado", System.currentTimeMillis().toString())
        actualizar { it.copy(extra = it.extra.copy(emusRevisando = false, emus = ordenEmus(revisados), emusRevisado = System.currentTimeMillis())) }
        val nuevas = revisados.filter { it.nueva }
        // En la campana de arriba queda el aviso hasta que se actualicen (o dejen de tener versión nueva).
        if (nuevas.isEmpty()) quitarAvisoDippo("emus")
        else avisoDippo(
            "emus", if (nuevas.size == 1) "${nuevas[0].nombre} tiene versión nueva" else "${nuevas.size} emuladores tienen versión nueva",
            nuevas.take(4).joinToString(" · ") { "${it.nombre} ${it.ultima}" },
        ) { abrirNativos(pestana = 2) }
        if (!silencioso) avisar(EmuladoresAlDia.comentario(revisados).substringBefore(" El que"))
        else if (nuevas.isNotEmpty()) cuandoHayaLugar("emus-nuevos") {
            mostrarDialogo(
                Dialogo("emus-nuevos", "DIPPO revisó tus emuladores", EmuladoresAlDia.comentario(revisados), Icono.EMULADOR,
                    lineas = nuevas.take(4).map { "${it.nombre}: ${it.version} → ${it.ultima}" }, si = "Ver y actualizar", no = "Después"),
            ) { abrirNativos(pestana = 2) }
        }
    }
}

/** Revisión automática: una vez cada ~20 h, un rato después de abrir Díptico (si está encendida). */
fun Diptico.vigilarEmuladores() {
    scope.launch {
        delay(90_000)
        val ultima = almacen.texto("emusRevisado").toLongOrNull() ?: 0L
        if (estado.value.extra.emusAuto && System.currentTimeMillis() - ultima > CADA_MS) revisarEmuladores(silencioso = true)
    }
}

/** Baja la versión nueva de [paquete], revisa que sea el mismo emulador con la misma firma y se la pasa a Android. */
fun Diptico.actualizarEmulador(paquete: String) {
    val e = estado.value
    if (e.extra.emusDescarga != null) { avisar("Espera que termine la descarga de ahora"); return }
    val em = e.extra.emus.firstOrNull { it.paquete == paquete } ?: return
    val url = em.url ?: return
    if (!Actualizador.puedeInstalar(app)) {
        mostrarDialogo(Dialogo("permiso-instalar-emus", "Falta un permiso",
            "Para instalar emuladores, Android tiene que dejar que Díptico instale apps. Actívalo y vuelve a tocar «Actualizar».",
            Icono.CLAVE, si = "Abrir el permiso", no = "Ahora no")) { Actualizador.pedirPermisoInstalar(app) }
        return
    }
    actualizar { it.copy(extra = it.extra.copy(emusDescarga = paquete, emusAvance = 0f)) }
    scope.launch {
        val destino = File(File(app.cacheDir, "emuladores").apply { mkdirs() }, "$paquete.apk")
        val apk = withContext(Dispatchers.IO) {
            runCatching {
                Actualizador.descargarA(url, destino, em.bytes) { a ->
                    // Solo se avisa cada 5 %, para no redibujar todo con cada pedacito.
                    if ((a * 20).toInt() != (estado.value.extra.emusAvance * 20).toInt()) actualizar { it.copy(extra = it.extra.copy(emusAvance = a)) }
                }
            }.getOrNull()
        }
        if (apk == null) {
            actualizar { it.copy(extra = it.extra.copy(emusDescarga = null)) }
            avisar("No pude bajar ${em.nombre}: revisa tu internet")
            return@launch
        }
        val firma = withContext(Dispatchers.IO) { EmuladoresAlDia.mismaFirma(app, apk, paquete) }
        if (firma == false) {
            actualizar { it.copy(extra = it.extra.copy(emusDescarga = null)) }
            mostrarDialogo(Dialogo("firma-distinta", "${em.nombre} viene firmado distinto",
                "Tu ${em.nombre} se instaló desde otra fuente (por ejemplo Play Store), y Android no deja poner encima una versión firmada por otro. " +
                    "Para cambiarte a la de su página: guarda tus partidas, desinstálalo y vuelve a tocar «Actualizar» (quedará como instalación nueva).",
                Icono.CLAVE, si = "Abrir su página", no = "Entendido")) { if (em.pagina.isNotBlank()) abrirWeb(em.pagina) }
            apk.delete()
            return@launch
        }
        actualizar { it.copy(extra = it.extra.copy(emusAvance = 1f)) }
        try {
            withContext(Dispatchers.IO) { Actualizador.instalarApk(app, apk, paquete) }
        } catch (ex: Exception) {
            actualizar { it.copy(extra = it.extra.copy(emusDescarga = null)) }
            avisar("Android no aceptó la instalación de ${em.nombre}")
        }
    }
}

/** Android terminó de instalar un emulador: se marca al día. */
fun Diptico.emuladorInstalado(paquete: String) {
    Lanzador.olvidar()
    val (v, cuando) = EmuladoresAlDia.instalado(app, paquete) ?: ("?" to System.currentTimeMillis())
    actualizar { st ->
        st.copy(extra = st.extra.copy(emusDescarga = null, emus = ordenEmus(st.extra.emus.map {
            if (it.paquete == paquete) it.copy(version = v, instaladoEn = cuando, nueva = false) else it
        })))
    }
    val nombre = estado.value.extra.emus.firstOrNull { it.paquete == paquete }?.nombre ?: "El emulador"
    avisar("$nombre quedó al día ($v)")
    // Si ya no queda ninguno con versión nueva, el aviso de la campana se va.
    if (estado.value.extra.emus.none { it.nueva }) quitarAvisoDippo("emus")
    File(File(app.cacheDir, "emuladores"), "$paquete.apk").delete()
}

/** Android no lo instaló: se explica por qué en palabras simples. */
fun Diptico.emuladorNoInstalado(paquete: String, estadoAndroid: Int, motivo: String?) {
    actualizar { it.copy(extra = it.extra.copy(emusDescarga = null)) }
    val nombre = estado.value.extra.emus.firstOrNull { it.paquete == paquete }?.nombre ?: "El emulador"
    val texto = when (estadoAndroid) {
        PackageInstaller.STATUS_FAILURE_ABORTED -> "Cancelaste la instalación de $nombre"
        PackageInstaller.STATUS_FAILURE_CONFLICT -> "$nombre viene firmado distinto al que tienes: desinstálalo primero (guarda tus partidas)"
        PackageInstaller.STATUS_FAILURE_STORAGE -> "No hay espacio para instalar $nombre"
        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "Esta versión de $nombre no es compatible con tu consola"
        else -> "No se pudo instalar $nombre" + (motivo?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: "")
    }
    avisar(texto)
}

/** Filas de la pestaña «Emuladores» del centro (la primera es DIPPO con su resumen). */
internal fun Diptico.entradasEmuladores(e: Estado): List<EntradaNativa> {
    val x = e.extra
    val lista = mutableListOf<EntradaNativa>()
    val cuando = if (x.emusRevisado == 0L) "Aún no reviso" else "Revisé " + cuandoFueTexto(x.emusRevisado)
    lista += EntradaNativa(
        "dippo-emus", "DIPPO revisa tus emuladores",
        if (x.emusRevisando) "Revisando…" else "$cuando · A: revisar ahora", "DIPPO",
        accion = if (x.emusRevisando) "" else "Revisar ahora", lista = x.emus.none { it.nueva } && x.emusRevisado > 0,
        ayuda = if (x.emus.isEmpty()) "Toca «Revisar ahora»: veo qué emuladores tienes, cuáles usas y si hay versión nueva en su página."
        else EmuladoresAlDia.comentario(x.emus),
        consejos = listOf(
            "Android pide confirmar cada instalación (la primera vez por emulador).",
            "Si un emulador vino de Play Store, su versión de GitHub viene firmada distinto: hay que desinstalarlo antes.",
            "Guarda tus partidas antes de actualizar un emulador en beta.",
        ),
    )
    for (em in x.emus) {
        val bajando = x.emusDescarga == em.paquete
        val uso = if (em.consolas.isNotEmpty()) "Lo usas para ${em.consolas.take(3).joinToString(", ")}" + (if (em.usoSeg >= 3600) " · ${em.usoSeg / 3600} h" else "") else "No lo usas para ninguna consola"
        val detalle = when {
            bajando -> "Bajando… ${(x.emusAvance * 100).toInt()} %"
            em.nueva -> "${em.version} → ${em.ultima} · A: actualizar"
            em.ultima != null -> "${em.version} · al día"
            em.aviso.isNotBlank() -> "${em.version} · ${em.aviso}"
            x.emusRevisando -> "${em.version} · revisando…"
            else -> "${em.version} · sin revisar"
        }
        lista += EntradaNativa(
            "emu:${em.paquete}", em.nombre, detalle, if (em.nueva) "Con versión nueva" else if (em.consolas.isNotEmpty()) "Los que usas" else "Otros instalados",
            em.paquete, true,
            accion = when { bajando -> ""; em.nueva -> "Actualizar"; em.pagina.isNotBlank() && em.ultima == null -> "Abrir su página"; else -> "Abrir" },
            lista = em.ultima != null && !em.nueva,
            ayuda = buildString {
                append("Instalado: ${em.version}. ")
                if (em.ultima != null) append("Más nueva en su página: ${em.ultima}. ")
                append(uso).append('.')
            },
            consejos = listOfNotNull(
                em.notas.takeIf { it.isNotBlank() }?.let { "Novedades: " + it.replace("\n", " · ").take(220) },
                em.aviso.takeIf { it.isNotBlank() },
                if (em.bytes > 0) "Descarga de ${em.bytes / 1_048_576} MB" else null,
            ),
        )
    }
    return lista
}

/** «hace 3 h», «ayer», «hace 2 días». */
internal fun cuandoFueTexto(ms: Long): String {
    val min = (System.currentTimeMillis() - ms) / 60_000
    return when {
        min < 2 -> "recién"
        min < 60 -> "hace $min min"
        min < 24 * 60 -> "hace ${min / 60} h"
        min < 48 * 60 -> "ayer"
        else -> "hace ${min / (24 * 60)} días"
    }
}

/** A sobre una fila de la pestaña de emuladores. */
internal fun Diptico.usarEntradaEmulador(id: String) {
    if (id == "dippo-emus") { revisarEmuladores(); return }
    val pkg = id.removePrefix("emu:")
    val em = estado.value.extra.emus.firstOrNull { it.paquete == pkg } ?: return
    when {
        estado.value.extra.emusDescarga == pkg -> Unit
        em.nueva -> actualizarEmulador(pkg)
        em.ultima == null && em.pagina.isNotBlank() -> abrirWeb(em.pagina)
        else -> abrirPaqueteAqui(pkg, estado.value.extra.nativosEn)
    }
}


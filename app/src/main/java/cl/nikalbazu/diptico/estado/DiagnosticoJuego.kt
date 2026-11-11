package cl.nikalbazu.diptico.estado

import android.content.Context
import android.provider.DocumentsContract
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.emu.Lanzador
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * «Por qué falló un juego»: si el emulador se cierra a los pocos segundos de abrir un juego, Díptico no lo cuenta como
 * partida y te dice qué es lo más probable (BIOS, llaves, formato, archivo dañado, permisos o el emulador), con un botón
 * para probar con otro emulador.
 */

/** Menos de esto (en segundos) entre abrir el juego y volver a Díptico = el juego no llegó a abrir. */
const val CIERRE_RAPIDO_SEG = 20L

object Diagnostico {
    /** Consolas cuyo emulador casi siempre pide la BIOS (o el firmware) original. */
    private val BIOS = mapOf(
        "psx" to "PlayStation: los emuladores como DuckStation piden la BIOS (por ejemplo scph5501.bin) en su carpeta de BIOS.",
        "ps2" to "PS2: NetherSX2 / AetherSX2 necesitan la BIOS de PS2; elígela dentro del emulador la primera vez.",
        "segacd" to "Sega CD: pide las BIOS bios_CD_U.bin, bios_CD_E.bin o bios_CD_J.bin en la carpeta system de RetroArch.",
        "saturn" to "Saturn: Yaba Sanshiro y Beetle Saturn piden la BIOS de Saturn (sega_101.bin / mpr-17933.bin).",
        "pcenginecd" to "PC Engine CD: pide syscard3.pce en la carpeta system de RetroArch.",
        "tg-cd" to "TurboGrafx-CD: pide syscard3.pce en la carpeta system de RetroArch.",
        "neogeo" to "Neo Geo: el archivo neogeo.zip tiene que estar junto a tus juegos o en system.",
        "nds" to "DS: si usas un núcleo que no trae BIOS propia, pide bios7.bin, bios9.bin y firmware.bin.",
        "dreamcast" to "Dreamcast: Flycast funciona sin BIOS en muchos juegos, pero algunos piden dc_boot.bin.",
        "3do" to "3DO: pide la BIOS panafz10.bin en la carpeta system de RetroArch.",
        "atari5200" to "Atari 5200: pide 5200.rom en la carpeta system.",
        "fds" to "Famicom Disk System: pide disksys.rom en la carpeta system.",
        "ps3" to "PS3: RPCS3 / aPS3e necesitan el firmware de PS3 instalado dentro del emulador.",
    )

    /** Formatos que los emuladores de esa consola no suelen abrir. */
    private val FORMATO_RARO = mapOf(
        "ps2" to setOf("zip", "7z", "rar"),
        "psp" to setOf("zip", "7z", "rar", "chd"),
        "switch" to setOf("zip", "7z", "rar"),
        "switch2" to setOf("zip", "7z", "rar"),
        "n3ds" to setOf("zip", "7z", "rar"),
        "gc" to setOf("zip", "7z", "rar"),
        "wii" to setOf("zip", "7z", "rar"),
        "psvita" to setOf("zip", "7z", "rar"),
    )

    /**
     * Las causas probables, de la más a la menos probable. [bytes] es el tamaño del archivo (−1 si no se sabe);
     * [emulador] es el nombre del emulador con que se abrió; [veces] cuántas veces seguidas se cerró enseguida.
     */
    fun causas(juego: Juego, consola: Consola, emulador: String?, bytes: Long, veces: Int): List<String> {
        val l = mutableListOf<String>()
        val tipo = juego.tipo
        if (bytes in 0L..1023L && !juego.esCarpeta) l += "El archivo pesa casi nada (${bytes} bytes): puede estar incompleto o dañado. Vuelve a copiarlo."
        if (tipo in setOf("rar", "part", "crdownload", "tmp")) l += "El archivo .$tipo no es un juego listo: termina de descargarlo o descomprímelo primero."
        FORMATO_RARO[consola.id]?.takeIf { tipo in it }?.let { l += "${consola.nombre} casi nunca abre juegos .$tipo: descomprímelo y deja el archivo del juego tal cual." }
        when (consola.id) {
            "switch", "switch2" -> when {
                tipo == "nup" && emulador?.contains("NovaS") != true && emulador?.contains("Banana") != true ->
                    l += "Los .nup se abren con NovaS o Banana Native: elige uno de esos para este juego."
                tipo == "nup" && emulador?.contains("Banana") == true ->
                    l += "Banana Native necesita «Acceso a todos los archivos» y que el juego sea uno de los que convierte."
                tipo != "nup" -> l += "Switch: faltan las llaves (prod.keys) o el firmware dentro del emulador, o el juego necesita una actualización."
            }
        }
        BIOS[consola.id]?.let { l += it }
        if (consola.id == "switch2" || consola.id == "switch") l += "Si pasa con todos los juegos, prueba el driver Turnip del emulador (en sus ajustes de gráficos)."
        l += "El emulador no tiene permiso para leer la carpeta de juegos: ábrelo y elige la carpeta dentro de él."
        if (veces >= 2) l += "Ya se cerró $veces veces seguidas: lo más probable es que este emulador no se lleve bien con este juego. Prueba otro."
        return l.distinct().take(5)
    }
}

/** Tamaño del archivo del juego (−1 si Android no lo dice). */
internal fun tamanoArchivo(ctx: Context, juego: Juego): Long = try {
    if (juego.uri.scheme == "file") java.io.File(juego.uri.path ?: "").length()
    else ctx.contentResolver.query(juego.uri, arrayOf(DocumentsContract.Document.COLUMN_SIZE), null, null, null)
        ?.use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else -1L } ?: -1L
} catch (_: Exception) { -1L }

/** Cierres rápidos seguidos por juego (se olvidan al cerrar Díptico). */
private val cierresRapidos = HashMap<String, Int>()

/**
 * Se llama al terminar una sesión. Si duró menos de [CIERRE_RAPIDO_SEG] y volviste a Díptico solo (no por cerrar tú el juego
 * desde Díptico), explica qué pudo pasar y ofrece probar con otro emulador.
 */
internal fun Diptico.revisarCierreRapido(clave: String, duracionSeg: Long) {
    if (duracionSeg >= CIERRE_RAPIDO_SEG) { cierresRapidos.remove(clave); return }
    val e = estado.value
    val juego = e.juegosPorClave[clave] ?: return
    if (juego.uri.scheme == "app") return
    val consola = Consolas.porId(juego.consolaId) ?: return
    val veces = (cierresRapidos[clave] ?: 0) + 1
    cierresRapidos[clave] = veces
    val archivo = archivoParaJugar(juego, e)
    val preferido = e.datos[clave]?.emulador ?: Nativos.recomendado(app, juego) ?: e.emuladores[consola.id]
    scope.launch {
        val bytes = withContext(Dispatchers.IO) { tamanoArchivo(app, archivo) }
        val emulador = withContext(Dispatchers.IO) { runCatching { Lanzador.elegida(app, consola, preferido, archivo.tipo)?.nombre }.getOrNull() }
        val causas = Diagnostico.causas(archivo, consola, emulador, bytes, veces)
        val otros = withContext(Dispatchers.IO) { runCatching { Lanzador.opciones(app, consola, archivo.tipo).distinctBy { it.id } }.getOrDefault(emptyList()) }
        val hayOtro = otros.size > 1
        cuandoHayaLugar("cierre-rapido") {
            mostrarDialogo(
                Dialogo(
                    "cierre-rapido", "${juego.titulo} se cerró enseguida",
                    (if (emulador != null) "Se abrió con $emulador y volvió a Díptico a los $duracionSeg s. " else "") + "Lo más probable:",
                    Icono.EMULADOR, lineas = causas,
                    si = if (hayOtro) "Probar con otro emulador" else "Ver emuladores", no = "Cerrar",
                ),
            ) {
                if (hayOtro) {
                    // El siguiente emulador distinto del que falló (solo para este juego).
                    val actual = otros.indexOfFirst { it.nombre == emulador }
                    val siguiente = otros[(actual + 1).mod(otros.size)]
                    cambiarDatos(clave) { it.copy(emulador = siguiente.id) }
                    val nuevo = siguiente.nombre
                    avisar("Ahora ${juego.titulo} se abre con ${nuevo ?: "otro emulador"}")
                    actualizar { it.copy(juegoSel = clave) }
                    jugar()
                } else abrirMenu(Seccion.EMULADORES)
            }
        }
    }
}

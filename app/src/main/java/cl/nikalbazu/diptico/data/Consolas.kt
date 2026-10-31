package cl.nikalbazu.diptico.data

import androidx.compose.ui.graphics.Color

/** Formato físico de cada consola: define el ícono de la cinta de consolas. */
enum class Medio { CARTUCHO_ANCHO, CARTUCHO_N64, CD, CARTUCHO_CHICO, MINIDISCO, TARJETA, DVD, BLURAY, UMD, TODOS }

/** Detalle de plástico que lleva la caja del juego (bisagra, ventana, franja...). */
enum class Banda { NINGUNA, BISAGRA, LATERAL_CLARA, ARRIBA_CLARA, ARRIBA_OSCURA, ABAJO_CLARA }

/** Tamaño de la caja en el estante, en unidades de diseño. */
data class FormaCaja(val ancho: Float, val alto: Float, val radio: Float, val banda: Banda)

/**
 * Cómo se entrega el juego al emulador:
 * DATA = URI del archivo como dato del intent; EXTRA = URI en un extra;
 * RUTA = ruta de archivo real (/storage/...) en un extra;
 * INYECTAR = el contenido del archivo (por ejemplo un .ps4 con "CUSA00900") en un extra.
 */
enum class ModoRom { DATA, EXTRA, RUTA, INYECTAR }

/**
 * Cómo abrir un juego con un emulador. Los datos vienen de las órdenes que usa ES-DE en Android.
 * [actividad] puede empezar con "." (relativa al paquete), contener "{pkg}" o ser completa.
 */
data class Emulador(
    val nombre: String,
    val paquetes: List<String>,
    val actividad: String,
    val accion: String? = null,
    val categoria: String? = null,
    val modo: ModoRom = ModoRom.DATA,
    val extraRom: String? = null,
    val extrasBool: Map<String, Boolean> = emptyMap(),
    val limpiarTarea: Boolean = false,
    /** Si no es null, solo sirve para estos tipos de archivo ("carpeta" = juego en carpeta). */
    val soloPara: Set<String>? = null,
) {
    fun sirvePara(tipo: String): Boolean = soloPara == null || tipo in soloPara

    fun componente(paquete: String): String = when {
        actividad.startsWith(".") -> paquete + actividad
        actividad.contains("{pkg}") -> actividad.replace("{pkg}", paquete)
        else -> actividad
    }
}

data class Consola(
    val id: String,
    val nombre: String,
    val nombreLargo: String,
    val carpetas: List<String>,
    val extensiones: Set<String>,
    val medio: Medio,
    val color: Color,
    val caja: FormaCaja,
    val emuladores: List<Emulador>,
    /** El juego usa las dos pantallas (DS): no se atenúa la de abajo. */
    val dosPantallas: Boolean = false,
    /** Las subcarpetas también son juegos (PS3 en formato carpeta). */
    val carpetasSonJuegos: Boolean = false,
)

object Consolas {
    private const val VIEW = "android.intent.action.VIEW"
    private const val MAIN = "android.intent.action.MAIN"

    private fun ext(vararg e: String) = e.toSet()

    val todas: List<Consola> = listOf(
        Consola(
            id = "snes", nombre = "SNES", nombreLargo = "SUPER NINTENDO",
            carpetas = listOf("snes", "sfc", "supernintendo", "superfamicom"),
            extensiones = ext("bin", "bml", "bs", "bsx", "dx2", "fig", "gd3", "gd7", "mgd", "sfc", "smc", "st", "swc", "7z", "zip"),
            medio = Medio.CARTUCHO_ANCHO, color = Color(0xFF7A5CE8),
            caja = FormaCaja(128f, 92f, 6f, Banda.NINGUNA),
            emuladores = listOf(
                Emulador("Snes9x EX+", listOf("com.explusalpha.Snes9xPlus"), "com.imagine.BaseActivity", accion = VIEW),
            ),
        ),
        Consola(
            id = "n64", nombre = "N64", nombreLargo = "NINTENDO 64",
            carpetas = listOf("n64", "nintendo64"),
            extensiones = ext("app", "bin", "d64", "n64", "ndd", "u1", "v64", "z64", "7z", "zip"),
            medio = Medio.CARTUCHO_N64, color = Color(0xFF3E6FD8),
            caja = FormaCaja(128f, 92f, 6f, Banda.ABAJO_CLARA),
            emuladores = listOf(
                Emulador(
                    "Mupen64Plus FZ",
                    listOf("org.mupen64plusae.v3.fzurita.pro", "org.mupen64plusae.v3.fzurita", "org.mupen64plusae.v3.fzurita.amazon"),
                    "paulscode.android.mupen64plusae.SplashActivity", accion = VIEW,
                ),
                Emulador("Mupen64Plus AE", listOf("org.mupen64plusae.v3.alpha"), "paulscode.android.mupen64plusae.SplashActivity", accion = VIEW),
            ),
        ),
        Consola(
            id = "psx", nombre = "PS1", nombreLargo = "PLAYSTATION",
            carpetas = listOf("psx", "ps1", "playstation"),
            extensiones = ext("bin", "cbn", "ccd", "chd", "cue", "ecm", "exe", "img", "iso", "m3u", "mdf", "mds", "pbp", "toc", "z", "znx", "7z", "zip"),
            medio = Medio.CD, color = Color(0xFF8C93A8),
            caja = FormaCaja(104f, 104f, 4f, Banda.BISAGRA),
            emuladores = listOf(
                Emulador(
                    "DuckStation", listOf("com.github.stenzek.duckstation"), ".EmulationActivity",
                    modo = ModoRom.EXTRA, extraRom = "bootPath", extrasBool = mapOf("resumeState" to false), limpiarTarea = true,
                ),
            ),
        ),
        Consola(
            id = "gba", nombre = "GBA", nombreLargo = "GAME BOY ADVANCE",
            carpetas = listOf("gba", "gameboyadvance"),
            extensiones = ext("agb", "bin", "cgb", "dmg", "gb", "gba", "gbc", "gbx", "sgb", "7z", "zip"),
            medio = Medio.CARTUCHO_CHICO, color = Color(0xFF5A4BD6),
            caja = FormaCaja(92f, 98f, 6f, Banda.LATERAL_CLARA),
            emuladores = listOf(
                Emulador(
                    "Pizza Boy GBA", listOf("it.dbtecno.pizzaboygbapro", "it.dbtecno.pizzaboygba"), "{pkg}.MainActivity",
                    modo = ModoRom.EXTRA, extraRom = "rom_uri", limpiarTarea = true,
                ),
            ),
        ),
        Consola(
            id = "gc", nombre = "GC", nombreLargo = "GAMECUBE",
            carpetas = listOf("gc", "gamecube", "ngc"),
            extensiones = ext("ciso", "dol", "elf", "gcm", "gcz", "iso", "m3u", "rvz", "tgc", "wbfs", "wia", "7z", "zip"),
            medio = Medio.MINIDISCO, color = Color(0xFF6E4BC9),
            caja = FormaCaja(90f, 124f, 8f, Banda.ARRIBA_CLARA),
            emuladores = listOf(
                Emulador(
                    "Dolphin", listOf("org.dolphinemu.dolphinemu"), ".ui.main.TvMainActivity",
                    accion = MAIN, categoria = "android.intent.category.LEANBACK_LAUNCHER",
                    modo = ModoRom.EXTRA, extraRom = "AutoStartFile",
                ),
                Emulador(
                    "Dolphin MMJR2", listOf("org.dolphinemu.mmjr"), "org.dolphinemu.dolphinemu.ui.main.MainActivity",
                    accion = VIEW, modo = ModoRom.EXTRA, extraRom = "AutoStartFile",
                ),
            ),
        ),
        Consola(
            id = "nds", nombre = "NDS", nombreLargo = "NINTENDO DS",
            carpetas = listOf("nds", "ds", "nintendods"),
            extensiones = ext("app", "bin", "dsi", "ids", "nds", "7z", "zip"),
            medio = Medio.TARJETA, color = Color(0xFF5E6576),
            caja = FormaCaja(96f, 108f, 9f, Banda.ARRIBA_CLARA),
            dosPantallas = true,
            emuladores = listOf(
                Emulador(
                    "melonDS", listOf("me.magnum.melonds"), ".ui.emulator.EmulatorActivity",
                    accion = "me.magnum.melonds.LAUNCH_ROM", modo = ModoRom.EXTRA, extraRom = "uri",
                ),
                Emulador(
                    "melonDS (doble pantalla)", listOf("me.magnum.melondualds"), "me.magnum.melonds.ui.emulator.EmulatorActivity",
                    accion = "me.magnum.melonds.LAUNCH_ROM", modo = ModoRom.EXTRA, extraRom = "uri",
                ),
                Emulador(
                    "melonDS Nightly", listOf("me.magnum.melonds.nightly"), "me.magnum.melonds.ui.emulator.EmulatorActivity",
                    accion = "me.magnum.melonds.nightly.LAUNCH_ROM", modo = ModoRom.EXTRA, extraRom = "uri",
                ),
                Emulador("DraStic", listOf("com.dsemu.drastic"), ".DraSticActivity", limpiarTarea = true),
            ),
        ),
        Consola(
            id = "ps2", nombre = "PS2", nombreLargo = "PLAYSTATION 2",
            carpetas = listOf("ps2", "playstation2"),
            extensiones = ext("bin", "chd", "ciso", "cso", "dump", "elf", "gz", "m3u", "mdf", "img", "iso", "isz", "nrg"),
            medio = Medio.DVD, color = Color(0xFF2E4FB8),
            caja = FormaCaja(84f, 120f, 6f, Banda.ARRIBA_OSCURA),
            emuladores = listOf(
                Emulador(
                    "NetherSX2", listOf("xyz.aethersx2.android"), ".EmulationActivity",
                    accion = MAIN, modo = ModoRom.EXTRA, extraRom = "bootPath", limpiarTarea = true,
                ),
            ),
        ),
        Consola(
            id = "ps3", nombre = "PS3", nombreLargo = "PLAYSTATION 3",
            carpetas = listOf("ps3", "playstation3"),
            extensiones = ext("iso", "ps3"),
            medio = Medio.BLURAY, color = Color(0xFF3A3F55),
            caja = FormaCaja(88f, 112f, 6f, Banda.ARRIBA_OSCURA),
            carpetasSonJuegos = true,
            emuladores = listOf(
                // Archivo .ps3 con el código del juego adentro (por ejemplo BLUS30443).
                Emulador(
                    "ARMSX3", listOf("com.armsx3", "com.armsx3.play"), "com.armsx2.Main",
                    accion = VIEW, modo = ModoRom.INYECTAR, extraRom = "title_id", soloPara = setOf("ps3"),
                ),
                // ISO o carpeta del juego.
                Emulador(
                    "ARMSX3", listOf("com.armsx3", "com.armsx3.play"), "com.armsx2.Main",
                    modo = ModoRom.RUTA, extraRom = "path", soloPara = setOf("iso", "carpeta"),
                ),
                Emulador(
                    "aPS3e", listOf("aenu.aps3e.premium", "aenu.aps3e"), "aenu.aps3e.EmulatorActivity",
                    accion = "aenu.intent.action.APS3E", modo = ModoRom.EXTRA, extraRom = "iso_uri", soloPara = setOf("iso"),
                ),
                Emulador(
                    "aPS3e", listOf("aenu.aps3e.premium", "aenu.aps3e"), "aenu.aps3e.EmulatorActivity",
                    accion = "aenu.intent.action.APS3E", modo = ModoRom.RUTA, extraRom = "game_dir", soloPara = setOf("carpeta"),
                ),
            ),
        ),
        Consola(
            id = "ps4", nombre = "PS4", nombreLargo = "PLAYSTATION 4",
            carpetas = listOf("ps4", "playstation4"),
            extensiones = ext("ps4"),
            medio = Medio.BLURAY, color = Color(0xFF1F3A8C),
            caja = FormaCaja(88f, 112f, 6f, Banda.ARRIBA_OSCURA),
            emuladores = listOf(
                // Archivo .ps4 con el código del juego ya importado en Bachata S4 (por ejemplo CUSA00900).
                Emulador(
                    "Bachata S4", listOf("com.bachatas4.android", "com.bachatas4.android.github"), "com.bachatas4.android.DirectLaunchActivity",
                    modo = ModoRom.INYECTAR, extraRom = "game_id",
                ),
            ),
        ),
        Consola(
            id = "psp", nombre = "PSP", nombreLargo = "PSP",
            carpetas = listOf("psp"),
            extensiones = ext("chd", "cso", "elf", "iso", "pbp", "prx", "7z", "zip"),
            medio = Medio.UMD, color = Color(0xFF4A4F60),
            caja = FormaCaja(78f, 130f, 12f, Banda.ARRIBA_OSCURA),
            emuladores = listOf(
                Emulador(
                    "PPSSPP", listOf("org.ppsspp.ppssppgold", "org.ppsspp.ppsspp"), "org.ppsspp.ppsspp.PpssppActivity",
                    accion = VIEW, categoria = "android.intent.category.DEFAULT",
                ),
            ),
        ),
    )

    private val porNombreCarpeta: Map<String, Consola> by lazy {
        todas.flatMap { c -> c.carpetas.map { it to c } }.toMap()
    }

    fun porCarpeta(nombre: String): Consola? = porNombreCarpeta[nombre.lowercase().replace(" ", "").trim()]

    fun porId(id: String): Consola? = todas.firstOrNull { it.id == id }

    fun orden(id: String): Int = todas.indexOfFirst { it.id == id }.let { if (it < 0) 99 else it }
}

package cl.nikalbazu.diptico.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

/** Formato físico del juego: decide el ícono de la consola. */
enum class Medio { CARTUCHO_ANCHO, CARTUCHO_N64, CD, CARTUCHO_CHICO, MINIDISCO, TARJETA, DVD, BLURAY, UMD, APP, MOVIL, ARCADE, DISQUETE, TODOS }

/** Detalle de plástico que lleva la caja del juego (bisagra, ventana, franja...). */
enum class Banda { NINGUNA, BISAGRA, LATERAL_CLARA, ARRIBA_CLARA, ARRIBA_OSCURA, ABAJO_CLARA, ARRIBA_ROJA }

/** Proporciones de la caja del juego, en unidades de diseño. */
data class FormaCaja(val ancho: Float, val alto: Float, val radio: Float, val banda: Banda)

/**
 * Una forma de abrir los juegos de una consola, tal como la escribe ES-DE para Android.
 * Ejemplo: "%EMULATOR_EDEN% %ACTION%=android.nfc.action.TECH_DISCOVERED %DATA%=%ROMPROVIDER%".
 */
data class Comando(val etiqueta: String, val linea: String) {
    /** Nombre de la regla del emulador (RETROARCH, EDEN, DOLPHIN...), o null si abre una app de Android. */
    val emulador: String? = Regex("%EMULATOR_([A-Z0-9_+\\-]+)%").find(linea)?.groupValues?.get(1)

    /** El archivo del juego trae adentro el nombre de una app (ports y juegos de Android). */
    val abreApp: Boolean = linea.startsWith("%ANDROIDAPP%")

    /** Núcleo de RetroArch, si usa uno. */
    val nucleo: String? = Regex("/cores/([A-Za-z0-9_]+)_libretro_android\\.so").find(linea)?.groupValues?.get(1)

    /** Nombre para mostrar: "RetroArch · Snes9x - Current" o "Eden". */
    val nombre: String
        get() = when {
            emulador == "RETROARCH" -> "RetroArch · $etiqueta"
            else -> etiqueta.removeSuffix(" (Standalone)").ifEmpty { emulador ?: "App" }
        }
}

data class Consola(
    val id: String,
    /** Nombre corto para la pestaña (SNES, PS2, Switch...). */
    val nombre: String,
    val nombreLargo: String,
    /** Nombre completo para leer cómodo: "Nintendo Switch", "Sony PlayStation 2"... */
    val bonito: String,
    val carpetas: List<String>,
    val extensiones: Set<String>,
    val medio: Medio,
    val color: Color,
    val caja: FormaCaja,
    val comandos: List<Comando>,
    /** El juego usa las dos pantallas (DS, 3DS): no se atenúa la de abajo. */
    val dosPantallas: Boolean = false,
    /** Las subcarpetas también son juegos (PS3, Wii U, Xbox 360 en formato carpeta). */
    val carpetasSonJuegos: Boolean = false,
    /** Nombre del sistema en libretro, para descargar portadas e íconos. */
    val libretro: String? = null,
    /** Nombre del ícono en los paquetes de íconos de RetroArch. */
    val icono: String? = null,
) {
    /** Los ports son apps de Android, no archivos. */
    val esPorts: Boolean get() = id == Consolas.PORTS

    /** Juegos instalados en Android (Genshin, Minecraft...). */
    val esAndroid: Boolean get() = id == Consolas.ANDROID

    /** Abre apps en vez de usar emuladores. */
    val sinEmulador: Boolean get() = esPorts || esAndroid

    /** El emulador que recomendamos instalar si no hay ninguno. */
    val sugerido: String get() = comandos.firstOrNull { !it.abreApp }?.nombre ?: "un emulador"
}

/** Lo que Díptico sabe dibujar de cada consola conocida. */
private data class Visual(
    val corto: String,
    val medio: Medio,
    val color: Long,
    val caja: FormaCaja? = null,
    val dos: Boolean = false,
    val carpetasJuegos: Boolean = false,
    val alias: List<String> = emptyList(),
    val bonito: String? = null,
)

private val CAJA_SNES = FormaCaja(128f, 92f, 6f, Banda.NINGUNA)
private val CAJA_CARTUCHO = FormaCaja(116f, 96f, 6f, Banda.ABAJO_CLARA)
private val CAJA_CHICA = FormaCaja(92f, 98f, 6f, Banda.LATERAL_CLARA)
private val CAJA_CD = FormaCaja(104f, 104f, 4f, Banda.BISAGRA)
private val CAJA_DVD = FormaCaja(84f, 120f, 6f, Banda.ARRIBA_OSCURA)
private val CAJA_DS = FormaCaja(96f, 108f, 9f, Banda.ARRIBA_CLARA)
private val CAJA_SWITCH = FormaCaja(80f, 128f, 7f, Banda.ARRIBA_ROJA)
private val CAJA_PC = FormaCaja(96f, 120f, 4f, Banda.LATERAL_CLARA)
private val CAJA_APP = FormaCaja(104f, 104f, 26f, Banda.NINGUNA)

private val VISUALES: Map<String, Visual> = mapOf(
    "switch" to Visual("Switch", Medio.TARJETA, 0xFFE60012, CAJA_SWITCH, alias = listOf("nsw", "nintendoswitch", "nintendo switch")),
    // Switch 2: hoy en Android solo corren sus juegos compatibles con Switch 1 (con Eden, Citron, Kenji-NX…);
    // cuando salga un emulador de Switch 2, Díptico ya tiene su consola lista.
    "switch2" to Visual("Switch 2", Medio.TARJETA, 0xFF00C3E3, CAJA_SWITCH, alias = listOf("ns2", "nswitch2", "nintendoswitch2", "nintendo switch 2"), bonito = "Nintendo Switch 2"),
    "ps2" to Visual("PS2", Medio.DVD, 0xFF2A4BA0, CAJA_DVD),
    "gc" to Visual("GC", Medio.MINIDISCO, 0xFF6A5ACD, FormaCaja(90f, 124f, 8f, Banda.ARRIBA_CLARA), alias = listOf("gamecube", "ngc")),
    "wii" to Visual("Wii", Medio.DVD, 0xFF9AA4B5, FormaCaja(86f, 122f, 8f, Banda.ARRIBA_CLARA)),
    "wiiu" to Visual("Wii U", Medio.BLURAY, 0xFF1C9FD8, FormaCaja(86f, 122f, 8f, Banda.ARRIBA_CLARA), carpetasJuegos = true),
    "psp" to Visual("PSP", Medio.UMD, 0xFF4A4F60, FormaCaja(78f, 130f, 12f, Banda.ARRIBA_OSCURA)),
    "nds" to Visual("NDS", Medio.TARJETA, 0xFF8D99AE, CAJA_DS, dos = true),
    "n3ds" to Visual("3DS", Medio.TARJETA, 0xFFD12C3A, CAJA_DS, dos = true, alias = listOf("3ds")),
    "gba" to Visual("GBA", Medio.CARTUCHO_CHICO, 0xFF5B3FD9, CAJA_CHICA),
    "gbc" to Visual("GBC", Medio.CARTUCHO_CHICO, 0xFF2EC4A6, CAJA_CHICA),
    "gb" to Visual("GB", Medio.CARTUCHO_CHICO, 0xFF8BAC0F, CAJA_CHICA),
    "snes" to Visual("SNES", Medio.CARTUCHO_ANCHO, 0xFF7A5CE8, CAJA_SNES, alias = listOf("supernintendo")),
    "sfc" to Visual("SFC", Medio.CARTUCHO_ANCHO, 0xFFD94F4F, CAJA_SNES, alias = listOf("superfamicom")),
    "snesna" to Visual("SNES NA", Medio.CARTUCHO_ANCHO, 0xFF7A5CE8, CAJA_SNES),
    "nes" to Visual("NES", Medio.CARTUCHO_ANCHO, 0xFFC7323B, CAJA_CARTUCHO),
    "famicom" to Visual("FC", Medio.CARTUCHO_ANCHO, 0xFFB22234, CAJA_CARTUCHO),
    "fds" to Visual("FDS", Medio.DISQUETE, 0xFFE8B31C, CAJA_CHICA),
    "n64" to Visual("N64", Medio.CARTUCHO_N64, 0xFF2F80ED, FormaCaja(128f, 92f, 6f, Banda.ABAJO_CLARA)),
    "virtualboy" to Visual("VB", Medio.CARTUCHO_CHICO, 0xFFC62828, CAJA_CHICA),
    "psx" to Visual("PS1", Medio.CD, 0xFF9B9BA8, CAJA_CD, alias = listOf("ps1", "playstation")),
    "ps3" to Visual("PS3", Medio.BLURAY, 0xFF1D1D24, FormaCaja(88f, 112f, 6f, Banda.ARRIBA_OSCURA), carpetasJuegos = true),
    "ps4" to Visual("PS4", Medio.BLURAY, 0xFF003791, FormaCaja(88f, 112f, 6f, Banda.ARRIBA_OSCURA)),
    "psvita" to Visual("Vita", Medio.TARJETA, 0xFF2D5BD7, FormaCaja(84f, 110f, 8f, Banda.ARRIBA_OSCURA)),
    "xbox" to Visual("Xbox", Medio.DVD, 0xFF107C10, FormaCaja(88f, 124f, 6f, Banda.ARRIBA_OSCURA)),
    "xbox360" to Visual("X360", Medio.DVD, 0xFF5DC21E, FormaCaja(88f, 124f, 6f, Banda.ARRIBA_CLARA), carpetasJuegos = true),
    "xboxone" to Visual("XOne", Medio.BLURAY, 0xFF0E7A0D, FormaCaja(88f, 124f, 6f, Banda.ARRIBA_OSCURA)),
    "dreamcast" to Visual("DC", Medio.CD, 0xFFF26D21, CAJA_CD),
    "saturn" to Visual("Saturn", Medio.CD, 0xFF4A4A6A, FormaCaja(100f, 118f, 4f, Banda.LATERAL_CLARA)),
    "saturnjp" to Visual("Saturn JP", Medio.CD, 0xFF4A4A6A, FormaCaja(100f, 118f, 4f, Banda.LATERAL_CLARA)),
    "genesis" to Visual("Genesis", Medio.CARTUCHO_ANCHO, 0xFF1E1E1E, FormaCaja(92f, 128f, 6f, Banda.ARRIBA_OSCURA)),
    "megadrive" to Visual("MD", Medio.CARTUCHO_ANCHO, 0xFF1E3A8A, FormaCaja(92f, 128f, 6f, Banda.ARRIBA_OSCURA)),
    "megadrivejp" to Visual("MD JP", Medio.CARTUCHO_ANCHO, 0xFF1E3A8A, FormaCaja(92f, 128f, 6f, Banda.ARRIBA_OSCURA)),
    "mastersystem" to Visual("SMS", Medio.CARTUCHO_ANCHO, 0xFF2B2B2B, CAJA_CARTUCHO),
    "gamegear" to Visual("GG", Medio.CARTUCHO_CHICO, 0xFF333333, CAJA_CHICA),
    "segacd" to Visual("Sega CD", Medio.CD, 0xFF222244, CAJA_CD),
    "megacd" to Visual("Mega CD", Medio.CD, 0xFF222244, CAJA_CD),
    "sega32x" to Visual("32X", Medio.CARTUCHO_ANCHO, 0xFFB71C1C, CAJA_CARTUCHO),
    "pcengine" to Visual("PCE", Medio.TARJETA, 0xFFF5F5F5, CAJA_CHICA),
    "tg16" to Visual("TG16", Medio.TARJETA, 0xFFFF6F00, CAJA_CHICA),
    "pcenginecd" to Visual("PCE CD", Medio.CD, 0xFFF57C00, CAJA_CD),
    "neogeo" to Visual("Neo Geo", Medio.ARCADE, 0xFFD4AF37, CAJA_DVD),
    "ngp" to Visual("NGP", Medio.CARTUCHO_CHICO, 0xFF455A64, CAJA_CHICA),
    "ngpc" to Visual("NGPC", Medio.CARTUCHO_CHICO, 0xFF00897B, CAJA_CHICA),
    "wonderswan" to Visual("WS", Medio.CARTUCHO_CHICO, 0xFF607D8B, CAJA_CHICA),
    "wonderswancolor" to Visual("WSC", Medio.CARTUCHO_CHICO, 0xFF3F51B5, CAJA_CHICA),
    "atari2600" to Visual("2600", Medio.CARTUCHO_ANCHO, 0xFF8D6E63, CAJA_CARTUCHO),
    "atari7800" to Visual("7800", Medio.CARTUCHO_ANCHO, 0xFF6D4C41, CAJA_CARTUCHO),
    "atarilynx" to Visual("Lynx", Medio.CARTUCHO_CHICO, 0xFFF9A825, CAJA_CHICA),
    "atarijaguar" to Visual("Jaguar", Medio.CARTUCHO_ANCHO, 0xFFC62828, CAJA_CARTUCHO),
    "arcade" to Visual("Arcade", Medio.ARCADE, 0xFFFF5D8F, CAJA_DVD),
    "mame" to Visual("MAME", Medio.ARCADE, 0xFF7B61FF, CAJA_DVD),
    "fbneo" to Visual("FBNeo", Medio.ARCADE, 0xFFEF6C00, CAJA_DVD),
    "cps1" to Visual("CPS1", Medio.ARCADE, 0xFF1565C0, CAJA_DVD),
    "cps2" to Visual("CPS2", Medio.ARCADE, 0xFF0D47A1, CAJA_DVD),
    "cps3" to Visual("CPS3", Medio.ARCADE, 0xFF283593, CAJA_DVD),
    "naomi" to Visual("Naomi", Medio.ARCADE, 0xFF00ACC1, CAJA_DVD),
    "atomiswave" to Visual("AW", Medio.ARCADE, 0xFF00897B, CAJA_DVD),
    "dos" to Visual("DOS", Medio.DISQUETE, 0xFF546E7A, CAJA_PC),
    "pc" to Visual("PC", Medio.DISQUETE, 0xFF37474F, CAJA_PC),
    "windows" to Visual("Windows", Medio.DISQUETE, 0xFF0078D4, CAJA_PC),
    "amiga" to Visual("Amiga", Medio.DISQUETE, 0xFFD32F2F, CAJA_PC),
    "c64" to Visual("C64", Medio.DISQUETE, 0xFF6D4C41, CAJA_PC),
    "msx" to Visual("MSX", Medio.CARTUCHO_ANCHO, 0xFF5E35B1, CAJA_CARTUCHO),
    "zxspectrum" to Visual("ZX", Medio.DISQUETE, 0xFF212121, CAJA_PC),
    "scummvm" to Visual("ScummVM", Medio.DISQUETE, 0xFF43A047, CAJA_PC),
    "3do" to Visual("3DO", Medio.CD, 0xFF9E9E9E, CAJA_CD),
    "steam" to Visual("Steam", Medio.APP, 0xFF1B2838, CAJA_APP),
    // Fan games de RPG Maker XP/VX/VX Ace (Pokémon Essentials y otros), abiertos con Essentia.
    // Cada subcarpeta con Game.ini es un juego; también sirven los accesos .essentia que crea Essentia.
    "rpgmaker" to Visual(
        "RPG Maker", Medio.DISQUETE, 0xFFE0452B, CAJA_PC, carpetasJuegos = true,
        alias = listOf("fangames", "fan games", "fangame", "pokemonfangames", "fangamespokemon", "rpgmakerxp", "rpgmakervx", "rpgmakervxace", "rmxp", "essentia"),
        bonito = "Fan games (RPG Maker)",
    ),
    "ports" to Visual("Ports", Medio.APP, 0xFF2EC4A6, CAJA_APP, bonito = "Ports"),
    "androidgames" to Visual("Android", Medio.MOVIL, 0xFF3DDC84, CAJA_APP, bonito = "Juegos de Android"),
)

private val BONITOS = mapOf(
    "snes" to "Super Nintendo", "sfc" to "Super Famicom", "snesna" to "Super Nintendo", "psx" to "PlayStation",
    "ps2" to "PlayStation 2", "ps3" to "PlayStation 3", "ps4" to "PlayStation 4", "psp" to "PSP", "psvita" to "PS Vita",
    "gba" to "Game Boy Advance", "gbc" to "Game Boy Color", "gb" to "Game Boy", "nes" to "NES", "mame" to "MAME",
    "gc" to "GameCube", "n3ds" to "Nintendo 3DS", "xbox" to "Xbox", "xbox360" to "Xbox 360", "xboxone" to "Xbox One",
)

/** Otros nombres de carpeta comunes (de otros frontends y de cómo la gente las nombra), para reconocer más consolas. */
private val ALIAS = mapOf(
    "psx" to listOf("ps1", "playstation", "playstation1"), "ps2" to listOf("playstation2"), "ps3" to listOf("playstation3"),
    "ps4" to listOf("playstation4"), "psp" to listOf("playstationportable"), "psvita" to listOf("vita", "psv"),
    "n3ds" to listOf("3ds", "nintendo3ds"), "nds" to listOf("ds", "nintendods"), "gba" to listOf("gameboyadvance"),
    "gbc" to listOf("gameboycolor"), "gb" to listOf("gameboy"), "snes" to listOf("supernintendo"),
    "n64" to listOf("nintendo64"), "gc" to listOf("gamecube"), "wiiu" to listOf("wii u"), "dreamcast" to listOf("dc"),
    "genesis" to listOf("sega genesis"), "megadrive" to listOf("md", "sega megadrive"), "mastersystem" to listOf("sms"),
    "gamegear" to listOf("gg"), "pcengine" to listOf("pce"), "neogeo" to listOf("neo geo"), "xbox360" to listOf("x360"),
    "saturn" to listOf("segasaturn"), "atari2600" to listOf("2600"),
)

/** Orden de las pestañas: primero las más usadas en una consola portátil. */
private val ORDEN = listOf(
    "switch2", "switch", "ps2", "gc", "wii", "wiiu", "psp", "psvita", "n3ds", "nds", "gba", "gbc", "gb", "snes", "sfc", "nes", "famicom",
    "n64", "psx", "ps3", "ps4", "xbox", "xbox360", "dreamcast", "saturn", "genesis", "megadrive", "mastersystem", "gamegear",
    "segacd", "sega32x", "pcengine", "tg16", "neogeo", "arcade", "mame", "fbneo", "cps1", "cps2", "cps3", "dos", "scummvm", "rpgmaker",
)

/** Todas las consolas que Díptico sabe abrir: las mismas que ES-DE para Android. */
object Consolas {
    const val PORTS = "ports"
    const val ANDROID = "androidgames"
    const val RPGMAKER = "rpgmaker"

    /** Dónde descargar Essentia, el emulador de fan games de RPG Maker. */
    const val URL_ESSENTIA = "https://github.com/Nikalbazu24/essentia/releases"

    /** Reglas de ES-DE: nombre del emulador -> "paquete/actividad" en orden de preferencia. */
    var reglas: Map<String, List<String>> = emptyMap()
        private set

    var todas: List<Consola> = basicas()
        private set

    /** Las consolas tal como vienen, antes de los cambios de la persona (nombre y color propios). */
    private var originales: List<Consola>? = null

    /** Aplica el nombre y el color que la persona eligió para cada consola. [propios]: id -> (nombre, color 0xAARRGGBB o 0). */
    fun personalizar(propios: Map<String, Pair<String?, Long>>) {
        val base = originales ?: todas.also { originales = it }
        todas = base.map { c ->
            val (n, col) = propios[c.id] ?: return@map c
            var r = c
            if (!n.isNullOrBlank()) r = r.copy(nombre = n, nombreLargo = n, bonito = n)
            if (col != 0L) r = r.copy(color = androidx.compose.ui.graphics.Color(col))
            r
        }
    }

    private var cargado = false

    /** Lee el catálogo (assets/sistemas.json, generado desde ES-DE). */
    fun cargar(ctx: Context) {
        if (cargado) return
        try {
            val texto = ctx.assets.open("sistemas.json").bufferedReader().use { it.readText() }
            val o = JSONObject(texto)
            val r = o.getJSONObject("reglas")
            reglas = r.keys().asSequence().associateWith { k ->
                val a = r.getJSONArray(k)
                (0 until a.length()).map { a.getString(it) }
            }
            val s = o.getJSONArray("sistemas")
            val lista = ArrayList<Consola>(s.length() + 1)
            for (i in 0 until s.length()) {
                val j = s.getJSONObject(i)
                val id = j.getString("id")
                // ES-DE separa "androidapps", "androidgames" y "android"; Díptico junta los juegos en uno solo.
                if (id == "android" || id == "androidapps") continue
                val ext = j.getJSONArray("ext").let { a -> (0 until a.length()).map { a.getString(it) }.filter { it.isNotEmpty() }.toSet() }
                val cmds = j.getJSONArray("cmds").let { a ->
                    (0 until a.length()).map { a.getJSONArray(it).let { c -> Comando(c.getString(0), c.getString(1)) } }
                }
                lista += crear(
                    id, j.getString("nombre"), ext, cmds,
                    j.optString("libretro", "").takeIf { it.isNotEmpty() && it != "null" },
                    j.optString("icono", "").takeIf { it.isNotEmpty() && it != "null" },
                )
            }
            originales = null
            todas = lista.sortedWith(compareBy<Consola>({ ORDEN.indexOf(it.id).let { p -> if (p < 0) 999 else p } }, { it.esPorts || it.esAndroid }, { it.bonito }))
                .let { l -> l.filter { !it.sinEmulador } + l.filter { it.sinEmulador } }
            cargado = true
            indice = null
        } catch (e: Exception) {
            // Si algo falla, quedan las consolas básicas para no romper nada.
        }
    }

    private fun crear(id: String, completo: String, ext: Set<String>, cmds: List<Comando>, libretro: String?, icono: String?): Consola {
        val v = VISUALES[id]
        val medio = v?.medio ?: adivinarMedio(ext)
        val caja = v?.caja ?: when (medio) {
            Medio.CD -> CAJA_CD
            Medio.DVD, Medio.BLURAY, Medio.ARCADE -> CAJA_DVD
            Medio.DISQUETE -> CAJA_PC
            Medio.APP, Medio.MOVIL -> CAJA_APP
            Medio.CARTUCHO_CHICO, Medio.TARJETA -> CAJA_CHICA
            else -> CAJA_CARTUCHO
        }
        val bonito = v?.bonito ?: BONITOS[id] ?: completo
        val corto = v?.corto ?: cortoDe(completo)
        return Consola(
            id = id, nombre = corto, nombreLargo = bonito.uppercase(), bonito = bonito,
            carpetas = (listOf(id) + (v?.alias ?: emptyList()) + (ALIAS[id] ?: emptyList())).distinct(),
            extensiones = ext, medio = medio,
            color = Color(v?.color ?: colorDe(id)),
            caja = caja, comandos = cmds, dosPantallas = v?.dos ?: false,
            carpetasSonJuegos = v?.carpetasJuegos ?: false,
            libretro = libretro, icono = icono,
        )
    }

    private fun adivinarMedio(ext: Set<String>): Medio = when {
        ext.any { it in setOf("cue", "chd", "gdi", "cdi", "ccd", "m3u") } -> Medio.CD
        ext.any { it in setOf("iso", "rvz", "wbfs", "wua") } -> Medio.DVD
        ext.any { it in setOf("adf", "dsk", "d64", "exe", "bat", "com", "hdf", "tap", "tzx", "st", "msa") } -> Medio.DISQUETE
        ext == setOf("app") -> Medio.APP
        else -> Medio.CARTUCHO_CHICO
    }

    private fun cortoDe(completo: String): String {
        val sinMarca = completo.split(' ').let { p -> if (p.size > 1) p.drop(1).joinToString(" ") else completo }
        return if (sinMarca.length <= 10) sinMarca else sinMarca.split(' ').joinToString("") { it.take(1) }.take(6).uppercase()
    }

    private fun colorDe(id: String): Long {
        val paleta = listOf(0xFF6B4EFF, 0xFFFF7A59, 0xFF3BA8FF, 0xFFFF5D8F, 0xFF2EC4A6, 0xFFFFB020, 0xFF8E6CF0, 0xFF00ACC1, 0xFFE57373)
        return paleta[Math.floorMod(id.hashCode(), paleta.size)]
    }

    /** Consolas mínimas, por si el catálogo todavía no se cargó (por ejemplo en las capturas). */
    private fun basicas(): List<Consola> = listOf("switch", "ps2", "gc", "psp", "nds", "gba", "snes", "n64", "psx", "ps3", "ps4", "xbox", PORTS, ANDROID).map { id ->
        val v = VISUALES[id]!!
        crear(id, v.bonito ?: v.corto, emptySet(), emptyList(), null, null)
    }

    private var indice: Map<String, Consola>? = null

    private fun porNombreCarpeta(): Map<String, Consola> =
        // Primero los otros nombres y después los nombres propios (así un nombre propio nunca queda tapado por un alias).
        indice ?: (todas.flatMap { c -> c.carpetas.drop(1).map { it.lowercase().replace(" ", "") to c } } +
            todas.map { c -> c.id.lowercase() to c }).toMap().also { indice = it }

    fun porCarpeta(nombre: String): Consola? = porNombreCarpeta()[nombre.lowercase().replace(" ", "").trim()]

    fun porId(id: String): Consola? = todas.firstOrNull { it.id == id }

    fun orden(id: String): Int = todas.indexOfFirst { it.id == id }.let { if (it < 0) 9999 else it }
}

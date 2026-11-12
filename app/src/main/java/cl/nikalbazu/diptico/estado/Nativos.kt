package cl.nikalbazu.diptico.estado

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.emu.Lanzador

/*
 * Switch 2 y ports nativos: lo que Díptico sabe de NovaS (emulador de Switch 2, derivado de Eden), de Banana Native
 * (convierte juegos de Switch 2 en ports nativos de Android) y de los ports nativos conocidos (recomps y decomps).
 * El «Centro de Switch 2 y ports» (ui/CentroNativos.kt) muestra todo esto con lo que tienes instalado.
 */

/** Un port nativo de Android: el juego que porta, de qué consola viene, su paquete y dónde conseguirlo. */
data class PortNativo(val nombre: String, val original: String, val origen: String, val paquete: String, val enlace: String)

/** Una fila del centro: lo que se ve y qué hace A. */
data class EntradaNativa(
    val id: String,
    val titulo: String,
    val detalle: String,
    val grupo: String,
    /** Paquete de la app (para su ícono y saber si está instalada). */
    val paquete: String? = null,
    val instalada: Boolean = false,
    /** Lo que dice el botón A. */
    val accion: String = "",
    /** Explicación larga para la otra pantalla. */
    val ayuda: String = "",
    val consejos: List<String> = emptyList(),
    /** Verde: listo para jugar o en uso. */
    val lista: Boolean = false,
)

object Nativos {
    const val NOVAS = "io.github.nova5emu.novas"
    const val BANANA = "dev.bananative.android"
    /** Los nombres de las órdenes en sistemas.json (son también lo que se guarda como emulador elegido). */
    const val ORDEN_NOVAS = "NovaS (Standalone)"
    const val ORDEN_BANANA = "Banana Native (Port nativo)"
    const val WEB_NOVAS = "https://github.com/nova5emu/novas/releases"

    /** Los juegos de Switch 2 que Banana Native ya convierte completos (código del juego → nombre). */
    val JUEGOS_BANANA = listOf(
        "0400C3F00006E000" to "Mario Kart World",
        "040074A01BF12000" to "Donkey Kong Bananza",
        "0400380020056000" to "Splatoon Raiders",
        "0400A3A01EE4C000" to "Pokémon Pokopia",
        "0400486024DEC000" to "The Legend of Zelda: Ocarina of Time",
    )

    /** Ports nativos conocidos (recomps, decomps y ports de PC). Necesitan tu copia del juego original. */
    val PORTS = listOf(
        PortNativo("Ship of Harkinian", "Zelda: Ocarina of Time", "Nintendo 64", "com.waterdish.shipwright", "https://github.com/Waterdish/Shipwright-Android"),
        PortNativo("2 Ship 2 Harkinian", "Zelda: Majora's Mask", "Nintendo 64", "org.linkzenic.twoship", "https://github.com/linkzenic/2ship2harkinian-Android"),
        PortNativo("Ghostship", "Super Mario 64", "Nintendo 64", "com.izzy2lost.ghostship", "https://github.com/izzy2lost/Ghostship"),
        PortNativo("Starship", "Star Fox 64", "Nintendo 64", "com.izzy2lost.starship", "https://github.com/izzy2lost/Starship"),
        PortNativo("SpaghettiKart", "Mario Kart 64", "Nintendo 64", "com.izzy2lost.spaghettikart", "https://github.com/izzy2lost/SpaghettiKart"),
        PortNativo("BattleShip", "Super Smash Bros.", "Nintendo 64", "com.jrickey.battleship", "https://github.com/JRickey/BattleShip"),
        PortNativo("Perfect Dark", "Perfect Dark", "Nintendo 64", "com.izzy2lost.perfectdark", "https://github.com/izzy2lost/perfect_dark"),
        PortNativo("Banjo Recomp", "Banjo-Kazooie", "Nintendo 64", "com.aure.banjorecomp", "https://github.com/AurelioB/BanjoRecomp-Android"),
        PortNativo("Goemon 64 Recomp", "Mystical Ninja Starring Goemon", "Nintendo 64", "com.ogdanimal.goemon64", "https://github.com/ogdanimal/Goemon64Recomp-Android"),
        PortNativo("Harvest Moon 64 Recomp", "Harvest Moon 64", "Nintendo 64", "com.igawa6.harvestmoon64", "https://github.com/igawa6/HarvestMoon64Recomp"),
        PortNativo("Castlevania LoD Recomp", "Castlevania: Legacy of Darkness", "Nintendo 64", "com.sergiomanzur.cvlodrecomp", "https://github.com/sergiomanzur/cvlod_recomp"),
        PortNativo("Dusklight", "Zelda: Twilight Princess", "GameCube", "com.twilitrealm.dusklight", "https://github.com/TwilitRealm/dusklight"),
        PortNativo("Dusklight DS", "Zelda: Twilight Princess (dos pantallas)", "GameCube", "com.igawa6.dusklight", "https://github.com/igawa6/dusklight"),
        PortNativo("KartPad", "Mario Kart Wii", "Wii", "com.chrissotraidis.kartpad", "https://github.com/chrissotraidis/kartpad"),
        PortNativo("Zelda 3", "Zelda: A Link to the Past", "Super Nintendo", "com.samyost1.zelda3android", "https://github.com/samyost1/zelda3-android"),
        PortNativo("Super Metroid", "Super Metroid", "Super Nintendo", "com.raekwon1603.supermetroid", "https://github.com/Raekwon1603/super_metroid-android"),
        PortNativo("The Minish Cap", "Zelda: The Minish Cap", "Game Boy Advance", "com.samyost1.tmcandroid", "https://github.com/samyost1/tmc-android"),
        PortNativo("Gen 1 Recomp", "Pokémon Rojo y Azul", "Game Boy", "com.bryanthaboi.pokemonredblue", "https://github.com/bryanthaboi/pokemon-gen1-recomp-project"),
        PortNativo("Gen 2 Recomped", "Pokémon Oro y Plata", "Game Boy Color", "com.underdecoded.gen2recomped", "https://github.com/UNDERdecoded/Gen2Recomped"),
        PortNativo("Crash Bandicoot", "Crash Bandicoot", "PlayStation", "com.matteo842.crashbandicoot", "https://github.com/Matteo842/CrashBandicoot-Launcher"),
        PortNativo("CTR Native", "Crash Team Racing", "PlayStation", "com.simon358.ctrnative", "https://github.com/Simon358/ctr-native-android"),
        PortNativo("Symphony Recomp", "Castlevania: Symphony of the Night", "PlayStation", "com.sergiomanzur.sotnrecomp", "https://github.com/sergiomanzur/SymphonyRecomp"),
        PortNativo("Silent Hill", "Silent Hill", "PlayStation", "com.slickamogus.silenthill", "https://github.com/SlickAmogus/silent-hill-decomp"),
        PortNativo("GoldenEye Recomp", "GoldenEye 007 (XBLA)", "Xbox 360", "com.jeffory.goldeneyerecomp", "https://github.com/jeffory/GoldenEye-Recomp"),
        PortNativo("Unleashed Recomp", "Sonic Unleashed", "Xbox 360", "com.sansnope.unleashedrecomp", "https://github.com/SansNope/UnleashedRecomp-Android"),
        PortNativo("Skate 3 Mobile", "Skate 3", "Xbox 360", "com.buku313.skate3mobile", "https://github.com/Buku313/Skate3-Mobile"),
        PortNativo("Hit and Run", "The Simpsons: Hit & Run", "PC", "com.carlox33.hitandrun", "https://github.com/Carlox33/The-Simpsons-Hit-and-Run-Android"),
        PortNativo("DevilutionX", "Diablo", "PC", "org.diasurgical.devilutionx", "https://github.com/diasurgical/DevilutionX"),
        PortNativo("OpenMW", "Morrowind", "PC", "com.xyzz.openmw", "https://github.com/xyzz/openmw-android"),
        PortNativo("OpenMW DS", "Morrowind (dos pantallas)", "PC", "com.joshdaniels.openmwds", "https://github.com/Josh-Daniels/OpenMW-DS"),
    )

    val paquetesPorts: List<String> = PORTS.map { it.paquete }

    private fun normal(s: String) = Busqueda.normalizar(s).replace(Regex("[^a-z0-9]"), "")

    /** El juego de Banana Native que es [titulo], si lo es (por nombre: los archivos .nup no traen su código a la vista). */
    fun juegoBanana(titulo: String): String? {
        val t = normal(titulo)
        if (t.isEmpty()) return null
        return JUEGOS_BANANA.firstOrNull { (id, nombre) ->
            val n = normal(nombre)
            t.contains(n) || n.contains(t) && t.length >= 8 || t.contains(id.lowercase()) ||
                // «Pokopia» y «Bananza» se reconocen solos aunque el archivo tenga otro nombre largo.
                (nombre.contains("Pokopia") && "pokopia" in t) || (nombre.contains("Bananza") && "bananza" in t)
        }?.second
    }

    /**
     * El emulador que conviene para [juego] si nadie eligió uno a mano: los juegos de Switch 2 que Banana Native convierte
     * van con Banana Native (corren nativos, mucho más fluidos) si está instalada.
     */
    fun recomendado(ctx: Context, juego: Juego): String? {
        if (juego.consolaId != "switch2") return null
        if (juegoBanana(juego.titulo) == null || !Lanzador.instalado(ctx, BANANA)) return null
        return ORDEN_BANANA
    }

    /** Lo que importa del equipo para Switch 2. */
    data class Equipo(val android: Int, val ramGb: Float, val adreno: Boolean, val chip: String) {
        val androidOk get() = android >= 33
        val ramOk get() = ramGb >= 11.5f
        val listo get() = androidOk && ramOk && adreno
    }

    fun equipo(ctx: Context): Equipo {
        val mem = ActivityManager.MemoryInfo()
        try { ctx.getSystemService(ActivityManager::class.java)?.getMemoryInfo(mem) } catch (_: Exception) {}
        val chip = if (Build.VERSION.SDK_INT >= 31) "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}".trim() else Build.HARDWARE
        val qualcomm = listOf(chip, Build.HARDWARE, Build.BOARD).any { s ->
            val x = s.lowercase()
            "qcom" in x || "qti" in x || "qualcomm" in x || x.startsWith("sm") || "kalama" in x || "pineapple" in x || "sun" == x
        }
        return Equipo(Build.VERSION.SDK_INT, mem.totalMem / 1_073_741_824f, qualcomm, chip.ifBlank { Build.HARDWARE })
    }
}

// ---------- El centro (estado y acciones) ----------

/** Pestañas del centro: 0 Switch 2, 1 Ports nativos, 2 Emuladores al día. */
const val PESTANAS_NATIVOS = 3

fun Diptico.abrirNativos(pantalla: Int = android.view.Display.DEFAULT_DISPLAY, pestana: Int = 0) {
    Lanzador.olvidar()
    actualizar { it.copy(panel = Panel.NINGUNO, extra = it.extra.copy(nativosEn = pantalla, nativosPestana = pestana.coerceIn(0, PESTANAS_NATIVOS - 1), nativosSel = 0, perfilEn = -1, amigosEn = -1, anioEn = -1)) }
    if (pestana == 2 && estado.value.extra.emusRevisado == 0L) revisarEmuladores()
}

fun Diptico.cerrarNativos() {
    if (estado.value.extra.nativosEn >= 0) actualizar { it.copy(extra = it.extra.copy(nativosEn = -1)) }
}

fun Diptico.pestanaNativos(delta: Int) {
    val x = estado.value.extra
    val n = (x.nativosPestana + delta).mod(PESTANAS_NATIVOS)
    actualizar { it.copy(extra = it.extra.copy(nativosPestana = n, nativosSel = 0)) }
    if (n == 2 && x.emusRevisado == 0L) revisarEmuladores()
    cl.nikalbazu.diptico.audio.Sonidos.mover()
}

fun Diptico.moverNativos(delta: Int) {
    val total = entradasNativos(estado.value).size
    if (total == 0) return
    val x = estado.value.extra
    val n = (x.nativosSel + delta).coerceIn(0, total - 1)
    if (n == x.nativosSel) { cl.nikalbazu.diptico.audio.Sonidos.borde(); return }
    actualizar { it.copy(extra = it.extra.copy(nativosSel = n)) }
    cl.nikalbazu.diptico.audio.Sonidos.mover()
}

fun Diptico.elegirNativo(i: Int) {
    if (estado.value.extra.nativosSel == i) usarNativo(i) else actualizar { it.copy(extra = it.extra.copy(nativosSel = i)) }
}

/** Tus juegos de Switch 2 en la biblioteca. */
internal fun juegosSwitch2(e: Estado): List<Juego> = e.juegosPorConsola["switch2"].orEmpty()

/** Las filas de la pestaña abierta. */
fun Diptico.entradasNativos(e: Estado): List<EntradaNativa> {
    val ctx = app
    if (e.extra.nativosPestana == 2) return entradasEmuladores(e)
    return if (e.extra.nativosPestana == 0) {
        val eq = Nativos.equipo(ctx)
        val novas = Lanzador.instalado(ctx, Nativos.NOVAS)
        val banana = Lanzador.instalado(ctx, Nativos.BANANA)
        val elegido = e.emuladores["switch2"]
        val propios = juegosSwitch2(e)
        val lista = mutableListOf<EntradaNativa>()
        lista += EntradaNativa(
            "equipo", "Tu consola", detalle2(eq) + if (eq.listo) " · lista para Switch 2" else " · puede costarle", "Tu equipo",
            lista = eq.listo,
            ayuda = "Para Switch 2 hace falta Android 13 o más, 12 GB de RAM (mejor 16) y gráficos Adreno (Snapdragon). La AYN Thor cumple con todo.",
            consejos = listOf(
                (if (eq.androidOk) "✓" else "✗") + " Android ${androidNombre(eq.android)}",
                (if (eq.ramOk) "✓" else "✗") + " Memoria: %.0f GB".format(eq.ramGb),
                (if (eq.adreno) "✓" else "✗") + " Gráficos: ${eq.chip.ifBlank { "desconocido" }}",
            ),
        )
        lista += EntradaNativa(
            "novas", "NovaS (beta)", when {
                !novas -> "No está instalado · A: conseguirlo"
                elegido == Nativos.ORDEN_NOVAS || elegido == null -> "Instalado · emulador de tus juegos de Switch 2"
                else -> "Instalado · A: usarlo para Switch 2"
            }, "Emulador", Nativos.NOVAS, novas,
            accion = if (!novas) "Conseguir" else if (elegido == Nativos.ORDEN_NOVAS) "Abrir NovaS" else "Usar para Switch 2",
            lista = novas,
            ayuda = "Emulador de Switch 2 (y Switch 1), hecho sobre Eden. Abre juegos .nup, .nsp y .xci. Los .nup ya vienen descifrados: no piden llaves ni firmware.",
            consejos = listOf(
                "Usa el driver Turnip que trae NovaS: el de fábrica cerraba algunos juegos.",
                "Pon tus juegos de Switch 2 en una carpeta propia y elígela en Díptico como carpeta de Switch 2.",
                "Es una beta: guarda seguido dentro del juego.",
            ),
        )
        lista += EntradaNativa(
            "banana", "Banana Native", when {
                !banana -> "No está instalado · A: conseguirlo"
                else -> "Instalado · ports nativos de ${Nativos.JUEGOS_BANANA.size} juegos de Switch 2"
            }, "Ports de Switch 2", Nativos.BANANA, banana,
            accion = if (!banana) "Conseguir" else "Abrir Banana Native", lista = banana,
            ayuda = "No emula: convierte cada juego en un programa nativo de Android, así corre mucho más fluido. Díptico lo usa solo para los juegos que soporta, y los demás siguen con NovaS.",
            consejos = listOf(
                "Dale «Acceso a todos los archivos» en Ajustes de Android › Apps › Banana Native.",
                "El juego tiene que ser un archivo .nup (con el nombre en minúsculas: .nup).",
                "La primera vez prepara los sombreados: déjalo terminar, después carga rápido.",
            ),
        )
        for ((id, nombre) in Nativos.JUEGOS_BANANA) {
            val tuyo = propios.firstOrNull { Nativos.juegoBanana(it.titulo) == nombre }
            lista += EntradaNativa(
                "banana:$id", nombre, when {
                    tuyo != null && banana -> "En tu biblioteca · A: jugar como port nativo"
                    tuyo != null -> "En tu biblioteca · instala Banana Native para el port nativo"
                    else -> "No está en tu biblioteca"
                }, "Juegos con port nativo", Nativos.BANANA, banana && tuyo != null,
                accion = if (tuyo != null) "Jugar" else "", lista = banana && tuyo != null,
                ayuda = "Código del juego: $id. Banana Native lo convierte completo.",
            )
        }
        lista
    } else {
        Nativos.PORTS.map { p ->
            val si = Lanzador.instalado(ctx, p.paquete)
            EntradaNativa(
                "port:${p.paquete}", p.nombre, "${p.original} · " + (if (si) "instalado · A: jugar" else "A: conseguirlo"),
                p.origen, p.paquete, si, accion = if (si) "Jugar" else "Conseguir", lista = si,
                ayuda = "Port nativo de ${p.original} (${p.origen}). No es un emulador: el juego corre como app de Android. " +
                    "Necesita tu copia del juego original (la app te dice qué archivo pide la primera vez). Al instalarlo aparece solo en tu carpeta Ports.",
                consejos = listOf("Se baja de ${p.enlace.removePrefix("https://")}"),
            )
        }.sortedBy { !it.instalada } // Los instalados primero; el resto en el orden del catálogo (los más conocidos arriba).
    }
}

private fun androidNombre(api: Int) = when {
    api >= 36 -> "16"
    api == 35 -> "15"
    api == 34 -> "14"
    api == 33 -> "13"
    api >= 31 -> "12"
    else -> "11 o menos"
}

private fun detalle2(eq: Nativos.Equipo) = "Android ${androidNombre(eq.android)} · %.0f GB · ${if (eq.adreno) "Adreno" else "otra GPU"}".format(eq.ramGb)

/** A sobre una fila: jugar, abrir, usar o conseguir. */
fun Diptico.usarNativo(i: Int = estado.value.extra.nativosSel) {
    val e = estado.value
    val f = entradasNativos(e).getOrNull(i) ?: return
    when {
        f.id == "dippo-emus" || f.id.startsWith("emu:") -> usarEntradaEmulador(f.id)
        f.id == "equipo" -> avisar(f.ayuda)
        f.id == "novas" -> when {
            !f.instalada -> abrirWeb(Nativos.WEB_NOVAS)
            e.emuladores["switch2"] == Nativos.ORDEN_NOVAS -> abrirPaqueteAqui(Nativos.NOVAS, e.extra.nativosEn)
            else -> { elegirEmuladorConsola("switch2", Nativos.ORDEN_NOVAS); avisar("Tus juegos de Switch 2 se abren con NovaS") }
        }
        f.id == "banana" -> if (!f.instalada) avisar("Pide el APK de Banana Native a quien te lo compartió: aún no tiene página pública")
            else abrirPaqueteAqui(Nativos.BANANA, e.extra.nativosEn)
        f.id.startsWith("banana:") -> {
            val nombre = f.titulo
            val juego = juegosSwitch2(e).firstOrNull { Nativos.juegoBanana(it.titulo) == nombre }
            if (juego == null) { avisar("$nombre no está en tu biblioteca. Pon su .nup en tu carpeta de Switch 2."); return }
            cerrarNativos()
            actualizar { it.copy(consolaSel = "switch2", juegoSel = juego.clave, enSistemas = false) }
            jugar()
        }
        f.id.startsWith("port:") -> {
            val p = Nativos.PORTS.firstOrNull { it.paquete == f.paquete } ?: return
            if (f.instalada) abrirPaqueteAqui(p.paquete, e.extra.nativosEn) else abrirWeb(p.enlace)
        }
    }
}

/** Fija el emulador de una consola (lo mismo que elegirlo en Ajustes › Emuladores). */
fun Diptico.elegirEmuladorConsola(consolaId: String, orden: String) {
    val m = estado.value.emuladores + (consolaId to orden)
    almacen.guardarEmuladores(m)
    actualizar { it.copy(emuladores = m) }
}

/** ¿Tiene algo que mostrar el centro en esta consola? (para ofrecerlo desde la carpeta de Switch 2 o de Ports). */
fun esConsolaNativa(consolaId: String) = consolaId == "switch2" || consolaId == Consolas.PORTS

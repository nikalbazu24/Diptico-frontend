package cl.nikalbazu.diptico.estado

import android.net.Uri
import cl.nikalbazu.diptico.audio.Sonidos
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas

/*
 * Lo que va detrás de cada consola en el inicio de Colección viva: su juego emblema (el más jugado), un juego que
 * elijas, una imagen de internet (la «imagen grande» de la consola) o la consola misma, como antes.
 */

/** El fondo ya resuelto: la imagen (null = se dibuja la consola) y de qué juego es, para nombrarlo. */
data class FondoConsola(val imagen: Uri?, val juego: String?, val modo: String)

val MODOS_FONDO = listOf("emblema", "juego", "imagen", "consola")
/** «god of war» → «God Of War» (para mostrar el nombre del clásico). */
fun tituloBonito(t: String) = t.split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

fun nombreModoFondo(m: String) = when (m) {
    "emblema" -> "Su emblema"; "juego" -> "Un juego"; "imagen" -> "De internet"; else -> "La consola"
}

/** El modo que manda para esta consola: el suyo o el de Ajustes. */
fun modoFondo(e: Estado, id: String): String {
    val f = e.consolasPropias[id]?.fondo ?: e.extra.fondoConsolas
    return if (f.startsWith("juego:")) "juego" else f
}

private fun imagenDe(j: Juego): Uri? = j.arte ?: j.portada?.takeIf { it.scheme != "app" }

/** Los juegos de la consola que tienen imagen para ir de fondo, del más jugado al menos. */
fun juegosParaFondo(e: Estado, id: String): List<Juego> =
    e.juegosPorConsola[id].orEmpty()
        .filter { e.datos[it.clave]?.oculto != true && imagenDe(it) != null }
        .sortedByDescending { e.datos[it.clave]?.tiempo ?: 0L }

/**
 * Los juegos que hicieron historia en cada consola, del más emblemático al menos (God of War en PS2, Ocarina of Time en
 * N64…). El emblema de una consola es el primero de esta lista que tengas en tu biblioteca; si no tienes ninguno, se usa
 * tu más jugado. Los nombres se comparan sin tildes ni signos, así «God of War II» también cuenta.
 */
val EMBLEMAS_CLASICOS: Map<String, List<String>> = mapOf(
    "nes" to listOf("super mario bros 3", "the legend of zelda", "super mario bros", "metroid", "mega man 2", "castlevania", "contra"),
    "snes" to listOf("super mario world", "the legend of zelda a link to the past", "super metroid", "chrono trigger", "donkey kong country", "street fighter ii", "final fantasy vi", "super mario kart"),
    "n64" to listOf("the legend of zelda ocarina of time", "super mario 64", "goldeneye 007", "mario kart 64", "super smash bros", "banjo kazooie", "majoras mask"),
    "gc" to listOf("super smash bros melee", "the legend of zelda the wind waker", "metroid prime", "super mario sunshine", "resident evil 4", "mario kart double dash"),
    "wii" to listOf("super mario galaxy", "wii sports", "mario kart wii", "the legend of zelda twilight princess", "super smash bros brawl", "xenoblade"),
    "wiiu" to listOf("the legend of zelda breath of the wild", "mario kart 8", "splatoon", "super mario 3d world", "xenoblade chronicles x"),
    "switch" to listOf("the legend of zelda breath of the wild", "super mario odyssey", "tears of the kingdom", "mario kart 8 deluxe", "animal crossing", "super smash bros ultimate", "metroid dread"),
    "switch2" to listOf("mario kart world", "donkey kong bananza"),
    "gb" to listOf("pokemon red", "pokemon blue", "tetris", "the legend of zelda links awakening", "super mario land", "kirbys dream land"),
    "gbc" to listOf("pokemon gold", "pokemon silver", "pokemon crystal", "links awakening dx", "oracle of seasons", "oracle of ages"),
    "gba" to listOf("pokemon emerald", "pokemon ruby", "pokemon sapphire", "pokemon fire red", "metroid fusion", "metroid zero mission", "the minish cap", "advance wars", "fire emblem", "golden sun"),
    "nds" to listOf("new super mario bros", "pokemon heartgold", "pokemon soulsilver", "pokemon black", "pokemon platinum", "mario kart ds", "professor layton", "phoenix wright"),
    "n3ds" to listOf("pokemon x", "pokemon y", "a link between worlds", "fire emblem awakening", "animal crossing new leaf", "super mario 3d land", "monster hunter"),
    "psx" to listOf("final fantasy vii", "metal gear solid", "crash bandicoot", "spyro", "resident evil 2", "castlevania symphony of the night", "gran turismo", "silent hill", "tekken 3"),
    "ps2" to listOf("god of war", "shadow of the colossus", "grand theft auto san andreas", "metal gear solid 3", "kingdom hearts", "final fantasy x", "ico", "devil may cry", "okami", "gran turismo 4"),
    "ps3" to listOf("the last of us", "uncharted 2", "god of war iii", "red dead redemption", "demons souls", "metal gear solid 4"),
    "psp" to listOf("god of war chains of olympus", "monster hunter freedom unite", "crisis core", "daxter", "patapon", "lumines", "persona 3 portable", "ghost of sparta"),
    "psvita" to listOf("persona 4 golden", "gravity rush", "uncharted golden abyss", "tearaway"),
    "dreamcast" to listOf("sonic adventure", "shenmue", "jet set radio", "soulcalibur", "crazy taxi", "skies of arcadia"),
    "saturn" to listOf("nights into dreams", "panzer dragoon saga", "virtua fighter 2", "guardian heroes"),
    "genesis" to listOf("sonic the hedgehog 2", "sonic the hedgehog", "streets of rage 2", "gunstar heroes", "phantasy star iv", "sonic 3"),
    "megadrive" to listOf("sonic the hedgehog 2", "sonic the hedgehog", "streets of rage 2", "gunstar heroes", "sonic 3"),
    "mastersystem" to listOf("alex kidd in miracle world", "sonic the hedgehog", "phantasy star", "wonder boy"),
    "gamegear" to listOf("sonic the hedgehog", "shinobi", "columns"),
    "pcengine" to listOf("bonks adventure", "castlevania rondo of blood", "ys book", "bomberman"),
    "tg16" to listOf("bonks adventure", "bomberman", "ys book"),
    "neogeo" to listOf("metal slug", "the king of fighters 98", "samurai shodown", "garou"),
    "arcade" to listOf("street fighter ii", "metal slug", "pac man", "the king of fighters", "marvel vs capcom"),
    "mame" to listOf("street fighter ii", "metal slug", "pac man", "the king of fighters"),
    "fbneo" to listOf("metal slug", "street fighter ii", "the king of fighters 98"),
    "xbox" to listOf("halo combat evolved", "halo 2", "fable", "ninja gaiden"),
    "xbox360" to listOf("halo 3", "gears of war", "mass effect", "forza horizon"),
    "atari2600" to listOf("pitfall", "space invaders", "adventure", "pac man"),
    "virtualboy" to listOf("mario tennis", "wario land"),
    "ngpc" to listOf("snk vs capcom", "sonic the hedgehog pocket adventure"),
    "wonderswancolor" to listOf("final fantasy", "gunpey"),
)

/** El nombre del juego que hizo historia en la consola (para buscar su imagen aunque no lo tengas). */
fun emblemaClasico(id: String): String? = clasicosDe(id)?.firstOrNull()

/** Consolas con otro nombre (versión japonesa o americana) usan la lista de su hermana. */
private val HERMANAS = mapOf("sfc" to "snes", "snesna" to "snes", "famicom" to "nes", "fds" to "nes", "megadrivejp" to "megadrive",
    "saturnjp" to "saturn", "pcenginecd" to "pcengine", "wonderswan" to "wonderswancolor", "ngp" to "ngpc", "cps1" to "arcade", "cps2" to "arcade", "cps3" to "arcade")
fun clasicosDe(id: String): List<String>? = EMBLEMAS_CLASICOS[id] ?: HERMANAS[id]?.let { EMBLEMAS_CLASICOS[it] }

/** Tu juego que coincide con un emblema histórico de la consola (el más emblemático que tengas), con imagen. */
fun emblemaHistorico(e: Estado, id: String): Juego? {
    val lista = clasicosDe(id) ?: return null
    val tuyos = juegosParaFondo(e, id).map { it to Busqueda.normalizar(it.titulo) }
    for (nombre in lista) {
        val n = Busqueda.normalizar(nombre)
        tuyos.firstOrNull { (_, t) -> t.startsWith(n) || t.contains(" $n") || t.contains(n) }?.let { return it.first }
    }
    return null
}

/** El juego emblema: el que hizo historia en la consola (si lo tienes); si no, tu más jugado; si no, el primero con imagen. */
fun juegoEmblema(e: Estado, id: String): Juego? {
    emblemaHistorico(e, id)?.let { return it }
    val l = juegosParaFondo(e, id)
    return l.firstOrNull { (e.datos[it.clave]?.tiempo ?: 0L) > 0L } ?: l.firstOrNull { it.arte != null } ?: l.firstOrNull()
}

fun fondoDeConsola(e: Estado, c: Consola?): FondoConsola {
    c ?: return FondoConsola(null, null, "consola")
    val propio = e.consolasPropias[c.id]
    val f = propio?.fondo ?: e.extra.fondoConsolas
    return when {
        f == "consola" -> FondoConsola(null, null, "consola")
        f == "imagen" && propio?.hero != null -> FondoConsola(Uri.parse(propio.hero), emblemaClasico(c.id)?.let { tituloBonito(it) }, "imagen")
        f.startsWith("juego:") -> {
            val j = e.juegosPorConsola[c.id].orEmpty().firstOrNull { it.clave == f.removePrefix("juego:") } ?: juegoEmblema(e, c.id)
            FondoConsola(j?.let(::imagenDe), j?.titulo, "juego")
        }
        else -> juegoEmblema(e, c.id).let { FondoConsola(it?.let(::imagenDe), it?.titulo, if (it != null && it == emblemaHistorico(e, c.id)) "historico" else "emblema") }
    }
}

fun Diptico.fijarFondoConsola(id: String, valor: String?) {
    cambiarConsolaPropia(id) { it.copy(fondo = valor) }
    Sonidos.mover()
}

/** Ajustes › Estilo: lo que va detrás de todas las consolas (cada una puede cambiarlo en su configuración). */
fun Diptico.elegirFondoConsolas(v: String) {
    almacen.guardarTexto("fondoConsolas", v)
    actualizar { it.copy(extra = it.extra.copy(fondoConsolas = v)) }
}

/** Busca una imagen grande para el fondo: arte de sus juegos en tus raspadores (ScreenScraper, TheGamesDB, IGDB…) y de la consola. */
fun Diptico.buscarFondoConsola(id: String) {
    val e = estado.value
    val juego = (e.consolasPropias[id]?.fondo?.takeIf { it.startsWith("juego:") }?.removePrefix("juego:")
        ?.let { k -> e.juegosPorConsola[id].orEmpty().firstOrNull { it.clave == k } }) ?: juegoEmblema(e, id)
        ?: e.juegosPorConsola[id].orEmpty().firstOrNull()
    val consola = Consolas.porId(id) ?: return
    val clasico = if (emblemaHistorico(e, id) == null) emblemaClasico(id) else null
    lanzarBusquedaImagenes("c_hero", clasico ?: juego?.titulo ?: consola.bonito) {
        // Si no tienes el juego que hizo historia en la consola, igual se busca su imagen por nombre (SteamGridDB).
        val deClasico = clasico?.let { runCatching { cl.nikalbazu.diptico.data.Descargas.candidatosConsola(consola, "c_hero", it, e.claveSgdb, null) }.getOrDefault(emptyList()) }.orEmpty()
        val deJuego = juego?.let { j ->
            val crudo = todosCrudos().firstOrNull { it.clave == j.clave } ?: j
            runCatching { cl.nikalbazu.diptico.data.Descargas.candidatos(app, crudo, consola, "arte", j.titulo, e.claveSgdb, null, e.raspadores) }.getOrDefault(emptyList())
        }.orEmpty()
        val deConsola = runCatching { cl.nikalbazu.diptico.data.Descargas.candidatosConsola(consola, "c_hero", consola.bonito, e.claveSgdb, null) }.getOrDefault(emptyList())
        deClasico + deJuego + deConsola
    }
}

/** Las opciones del apartado «Fondo» en la configuración de la consola. */
internal fun Diptico.opcionesFondoConsola(e: Estado, c: Consola): List<Opcion> {
    val id = c.id
    val modo = modoFondo(e, id)
    val i = MODOS_FONDO.indexOf(modo).coerceAtLeast(0)
    fun poner(m: String) {
        when (m) {
            "juego" -> fijarFondoConsola(id, "juego:" + (juegoEmblema(e, id)?.clave ?: ""))
            "imagen" -> { fijarFondoConsola(id, "imagen"); if (e.consolasPropias[id]?.hero == null) buscarFondoConsola(id) }
            else -> fijarFondoConsola(id, m)
        }
    }
    val fondo = fondoDeConsola(e, c)
    val lista = mutableListOf(
        Opcion(
            "Fondo", nombreModoFondo(modo), Icono.PORTADA,
            "Lo que se ve de fondo en el inicio. Su emblema (el juego que hizo historia en esa consola, si lo tienes; si no, tu más jugado), uno que elijas, una imagen de internet o la consola sola.",
            menos = { poner(MODOS_FONDO[(i - 1).mod(MODOS_FONDO.size)]) },
        ) { poner(MODOS_FONDO[(i + 1).mod(MODOS_FONDO.size)]) },
    )
    val juegos = juegosParaFondo(e, id)
    if (modo == "emblema" || modo == "juego") {
        val actual = juegos.indexOfFirst { it.titulo == fondo.juego }.coerceAtLeast(0)
        fun ir(d: Int) { juegos.getOrNull((actual + d).mod(juegos.size.coerceAtLeast(1)))?.let { fijarFondoConsola(id, "juego:" + it.clave) } }
        lista += Opcion(
            "Juego", fondo.juego ?: "Sin imagen", Icono.MANDO,
            if (juegos.isEmpty()) "Ningún juego de esta consola tiene imagen todavía. Busca una con «Internet»."
            else "◀ ▶ cambia entre tus juegos de esta consola (${juegos.size} con imagen), del más jugado al menos.",
            menos = { ir(-1) },
        ) { ir(1) }
    }
    lista += Opcion(
        "Internet", if (e.consolasPropias[id]?.hero != null) "Propia ✓" else "Buscar", Icono.DESCARGA,
        "Busca imágenes grandes del juego del fondo en tus fuentes (ScreenScraper, TheGamesDB, IGDB, SteamGridDB) y de la consola. La que elijas queda de fondo.",
    ) { fijarFondoConsola(id, "imagen"); buscarFondoConsola(id) }
    if (e.consolasPropias[id]?.fondo != null) lista += Opcion("Como en Ajustes", icono = Icono.BORRAR,
        ayuda = "Vuelve a lo que eliges para todas las consolas en Ajustes › Estilo.") { fijarFondoConsola(id, null) }
    return lista.map { it.copy(grupo = "Fondo") }
}

package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.emu.App
import cl.nikalbazu.diptico.emu.Lanzador

/*
 * Elegir el emulador de una consola o de un juego desde una lista: los que Díptico conoce y, por si el tuyo no
 * aparece, cualquier app instalada (se le pasa el juego como «abrir archivo», como hacen ARMSX2, NetherSX2 y casi todos).
 */

internal data class EleccionEmu(val id: String?, val nombre: String)

/** Lista para elegir: los conocidos instalados primero, después «Otra app: …» con el resto de tus apps. */
private fun Diptico.eleccionesEmulador(c: Consola, tipo: String?, conConsola: Boolean): List<EleccionEmu> {
    val conocidos = Lanzador.opciones(app, c, tipo).distinctBy { it.id }
    val paquetes = conocidos.mapNotNull { it.paquete }.toSet()
    val otras = appsInstaladasRapido().filter { it.paquete !in paquetes && it.paquete != app.packageName }
        .sortedWith(compareByDescending<App> { pareceEmulador(it.nombre, it.paquete) }.thenBy { it.nombre.lowercase() })
    return buildList {
        if (conConsola) add(EleccionEmu(null, "El de la consola"))
        conocidos.forEach { add(EleccionEmu(it.id, it.nombre)) }
        otras.forEach { add(EleccionEmu("app:${it.paquete}", "Otra app: ${it.nombre}")) }
    }
}

/** Las apps con cara de emulador van primero en la lista de «Otra app». */
private fun pareceEmulador(nombre: String, paquete: String): Boolean {
    val t = (nombre + " " + paquete).lowercase()
    return listOf("emu", "sx2", "ps2", "psx", "retro", "dolphin", "citra", "azahar", "yuzu", "eden", "ppsspp", "drastic", "melon", "duck", "mupen", "snes", "nes", "vita", "flycast", "redream", "aethersx", "nether", "arms", "lime", "sudachi", "citron", "winlator", "gamehub")
        .any { it in t }
}

/** Opción de Ajustes › Juegos y emuladores para una consola, con menú desplegable. */
internal fun Diptico.opcionEmuladorConsola(c: Consola): Opcion {
    val lista = eleccionesEmulador(c, null, conConsola = false)
    val actual = Lanzador.elegida(app, c, estado.value.emuladores[c.id])
    val i = lista.indexOfFirst { it.id == actual?.id }.coerceAtLeast(0)
    return Opcion(
        c.bonito,
        actual?.nombre ?: "Falta: ${c.sugerido}",
        Icono.EMULADOR,
        "Con qué se abren los juegos de ${c.nombre}. Si tu emulador no aparece entre los conocidos, búscalo abajo como «Otra app».",
    ) {}.conLista(lista.map { it.nombre }, i) { k ->
        val id = lista.getOrNull(k)?.id ?: return@conLista
        val m = estado.value.emuladores + (c.id to id)
        almacen.guardarEmuladores(m)
        actualizar { it.copy(emuladores = m) }
        avisar("${c.nombre} se abrirá con ${lista[k].nombre.removePrefix("Otra app: ")}")
    }
}

/** Opción del emulador de un solo juego (en sus opciones y en Ajustes). [cache] evita rearmar la lista para cada juego. */
internal fun Diptico.opcionEmuladorJuego(
    juego: Juego, c: Consola, titulo: String = "Emulador",
    cache: MutableMap<String, List<EleccionEmu>>? = null,
): Opcion {
    val lista = cache?.getOrPut(c.id + "|" + juego.tipo) { eleccionesEmulador(c, juego.tipo, conConsola = true) }
        ?: eleccionesEmulador(c, juego.tipo, conConsola = true)
    val propio = estado.value.datos[juego.clave]?.emulador
    val i = lista.indexOfFirst { it.id == propio }.coerceAtLeast(0)
    val nombre = if (propio == null) "El de la consola" else
        (Lanzador.elegida(app, c, propio, juego.tipo)?.takeIf { it.id == propio }?.nombre ?: lista.getOrNull(i)?.nombre ?: "El de la consola")
    return Opcion(
        titulo, nombre, Icono.EMULADOR,
        "Con qué se abre solo ${juego.titulo}. «El de la consola» usa el que elegiste para ${c.nombre}. Si tu emulador no aparece, elige «Otra app».",
    ) {}.conLista(lista.map { it.nombre }, i) { k ->
        val id = lista.getOrNull(k)?.id
        cambiarDatos(juego.clave) { it.copy(emulador = id) }
    }
}

const val TITULO_EMU_CONSOLAS = "Emulador de cada consola"
const val TITULO_EMU_JUEGOS = "Emulador de cada juego"

/**
 * Editor de emuladores en Ajustes › Juegos y emuladores: dos filas que abren su lista.
 * «De cada consola»: todas tus consolas con su desplegable. «De cada juego»: tus juegos ordenados por consola,
 * cada uno con «El de la consola», los emuladores conocidos o cualquier app instalada (por si Díptico no conoce el tuyo).
 */
internal fun Diptico.opcionesEditorEmuladores(e: Estado): List<Opcion> {
    val conJuegos = e.juegos.map { it.consolaId }.toSet()
    val consolas = Consolas.todas.filter { !it.sinEmulador && (it.id in conJuegos || it.id in e.emuladores) }
        .ifEmpty { e.consolasEmu.ifEmpty { Consolas.todas.filter { !it.sinEmulador }.take(20) } }
    val porConsola = consolas.map { opcionEmuladorConsola(it).copy(grupo = "Tus consolas") }
    val cache = HashMap<String, List<EleccionEmu>>()
    val porJuego = e.juegos.asSequence()
        .mapNotNull { j -> Consolas.porId(j.consolaId)?.takeIf { !it.sinEmulador }?.let { j to it } }
        .sortedWith(compareBy({ it.second.nombre }, { it.first.titulo.lowercase() }))
        .take(400)
        .map { (j, c) -> opcionEmuladorJuego(j, c, j.titulo, cache).copy(grupo = c.nombre) }
        .toList()
    val propios = e.datos.count { it.value.emulador != null }
    return buildList {
        add(Opcion(
            TITULO_EMU_CONSOLAS, "${consolas.size} consolas ›", Icono.EMULADOR,
            "Elige con qué se abren los juegos de cada consola. Si tu emulador no aparece entre los conocidos (por ejemplo una versión nueva de ARMSX2), " +
                "búscalo más abajo en la lista como «Otra app»: Díptico le pasa el juego como abrir archivo.",
            sub = porConsola,
        ) { abrirSubOpciones(TITULO_EMU_CONSOLAS) })
        if (porJuego.isNotEmpty()) add(Opcion(
            TITULO_EMU_JUEGOS, if (propios > 0) "$propios con emulador propio ›" else "${porJuego.size} juegos ›", Icono.MANDO,
            "Un emulador distinto solo para un juego (por ejemplo uno de PS2 que anda mejor en otro). «El de la consola» vuelve al de siempre.",
            sub = porJuego,
        ) { abrirSubOpciones(TITULO_EMU_JUEGOS) })
    }
}

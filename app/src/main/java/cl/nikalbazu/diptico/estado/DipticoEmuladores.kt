package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.emu.App
import cl.nikalbazu.diptico.emu.Lanzador

/*
 * Elegir el emulador de una consola o de un juego desde una lista: los que Díptico conoce y, por si el tuyo no
 * aparece, cualquier app instalada (se le pasa el juego como «abrir archivo», como hacen ARMSX2, NetherSX2 y casi todos).
 */

private data class EleccionEmu(val id: String?, val nombre: String)

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
    ).conLista(lista.map { it.nombre }, i) { k ->
        val id = lista.getOrNull(k)?.id ?: return@conLista
        val m = estado.value.emuladores + (c.id to id)
        almacen.guardarEmuladores(m)
        actualizar { it.copy(emuladores = m) }
        avisar("${c.nombre} se abrirá con ${lista[k].nombre.removePrefix("Otra app: ")}")
    }
}

/** Opción del emulador de un solo juego (en sus opciones y en Ajustes, para el juego elegido). */
internal fun Diptico.opcionEmuladorJuego(juego: Juego, c: Consola, titulo: String = "Emulador"): Opcion {
    val lista = eleccionesEmulador(c, juego.tipo, conConsola = true)
    val propio = estado.value.datos[juego.clave]?.emulador
    val i = lista.indexOfFirst { it.id == propio }.coerceAtLeast(0)
    val nombre = if (propio == null) "El de la consola" else
        (Lanzador.elegida(app, c, propio, juego.tipo)?.takeIf { it.id == propio }?.nombre ?: lista.getOrNull(i)?.nombre ?: "El de la consola")
    return Opcion(
        titulo, nombre, Icono.EMULADOR,
        "Con qué se abre solo ${juego.titulo}. «El de la consola» usa el que elegiste para ${c.nombre}. Si tu emulador no aparece, elige «Otra app».",
    ).conLista(lista.map { it.nombre }, i) { k ->
        val id = lista.getOrNull(k)?.id
        cambiarDatos(juego.clave) { it.copy(emulador = id) }
    }
}

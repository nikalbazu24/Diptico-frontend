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
fun nombreModoFondo(m: String) = when (m) {
    "emblema" -> "Su juego más jugado"; "juego" -> "Un juego que elijas"; "imagen" -> "Imagen de internet"; else -> "La consola"
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

/** El juego emblema: el más jugado con imagen (si no has jugado ninguno, el primero que tenga imagen grande). */
fun juegoEmblema(e: Estado, id: String): Juego? {
    val l = juegosParaFondo(e, id)
    return l.firstOrNull { (e.datos[it.clave]?.tiempo ?: 0L) > 0L } ?: l.firstOrNull { it.arte != null } ?: l.firstOrNull()
}

fun fondoDeConsola(e: Estado, c: Consola?): FondoConsola {
    c ?: return FondoConsola(null, null, "consola")
    val propio = e.consolasPropias[c.id]
    val f = propio?.fondo ?: e.extra.fondoConsolas
    return when {
        f == "consola" -> FondoConsola(null, null, "consola")
        f == "imagen" && propio?.hero != null -> FondoConsola(Uri.parse(propio.hero), null, "imagen")
        f.startsWith("juego:") -> {
            val j = e.juegosPorConsola[c.id].orEmpty().firstOrNull { it.clave == f.removePrefix("juego:") } ?: juegoEmblema(e, c.id)
            FondoConsola(j?.let(::imagenDe), j?.titulo, "juego")
        }
        else -> juegoEmblema(e, c.id).let { FondoConsola(it?.let(::imagenDe), it?.titulo, "emblema") }
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
    lanzarBusquedaImagenes("c_hero", juego?.titulo ?: consola.bonito) {
        val deJuego = juego?.let { j ->
            val crudo = todosCrudos().firstOrNull { it.clave == j.clave } ?: j
            runCatching { cl.nikalbazu.diptico.data.Descargas.candidatos(app, crudo, consola, "arte", j.titulo, e.claveSgdb, null, e.raspadores) }.getOrDefault(emptyList())
        }.orEmpty()
        val deConsola = runCatching { cl.nikalbazu.diptico.data.Descargas.candidatosConsola(consola, "c_hero", consola.bonito, e.claveSgdb, null) }.getOrDefault(emptyList())
        deJuego + deConsola
    }
}

/** Las opciones del apartado «Fondo» en la configuración de la consola. */
internal fun Diptico.opcionesFondoConsola(e: Estado, c: Consola): List<Opcion> {
    val id = c.id
    val modo = modoFondo(e, id)
    val i = MODOS_FONDO.indexOf(modo).coerceAtLeast(0)
    fun poner(m: String) = when (m) {
        "juego" -> fijarFondoConsola(id, "juego:" + (juegoEmblema(e, id)?.clave ?: ""))
        "imagen" -> { fijarFondoConsola(id, "imagen"); if (e.consolasPropias[id]?.hero == null) buscarFondoConsola(id) }
        else -> fijarFondoConsola(id, m)
    }
    val fondo = fondoDeConsola(e, c)
    val lista = mutableListOf(
        Opcion(
            "Detrás de la consola", nombreModoFondo(modo), Icono.PORTADA,
            "Lo que se ve de fondo en el inicio. Su juego más jugado, uno que elijas, una imagen de internet o la consola sola.",
            menos = { poner(MODOS_FONDO[(i - 1).mod(MODOS_FONDO.size)]) },
        ) { poner(MODOS_FONDO[(i + 1).mod(MODOS_FONDO.size)]) },
    )
    val juegos = juegosParaFondo(e, id)
    if (modo == "emblema" || modo == "juego") {
        val actual = juegos.indexOfFirst { it.titulo == fondo.juego }.coerceAtLeast(0)
        fun ir(d: Int) { juegos.getOrNull((actual + d).mod(juegos.size.coerceAtLeast(1)))?.let { fijarFondoConsola(id, "juego:" + it.clave) } }
        lista += Opcion(
            "Juego del fondo", fondo.juego ?: "Ninguno con imagen", Icono.MANDO,
            if (juegos.isEmpty()) "Ningún juego de esta consola tiene imagen todavía. Busca una con «Buscar imagen en internet»."
            else "◀ ▶ cambia entre tus juegos de esta consola (${juegos.size} con imagen), del más jugado al menos.",
            menos = { ir(-1) },
        ) { ir(1) }
    }
    lista += Opcion(
        "Buscar imagen en internet", if (e.consolasPropias[id]?.hero != null) "Propia ✓" else "Buscar", Icono.DESCARGA,
        "Busca imágenes grandes del juego del fondo en tus fuentes (ScreenScraper, TheGamesDB, IGDB, SteamGridDB) y de la consola. La que elijas queda de fondo.",
    ) { fijarFondoConsola(id, "imagen"); buscarFondoConsola(id) }
    if (e.consolasPropias[id]?.fondo != null) lista += Opcion("Usar lo de Ajustes", icono = Icono.BORRAR,
        ayuda = "Vuelve a lo que eliges para todas las consolas en Ajustes › Estilo.") { fijarFondoConsola(id, null) }
    return lista.map { it.copy(grupo = "Fondo") }
}

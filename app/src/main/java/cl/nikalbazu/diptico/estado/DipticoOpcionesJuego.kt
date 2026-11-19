package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.data.Raspadores
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Ventana del juego (Y o mantener A), rediseñada: sin Jugar ni Ficha (ya están en la vitrina), con «Mi paso» destacado,
 * cuatro grupos con submenús y «Mover de lugar». Las opciones de siempre se reparten por su título.
 */

/** Los grupos de la ventana del juego (solo los que tienen algo), con sus opciones. */
fun Diptico.gruposJuego(): List<Pair<Opcion, List<Opcion>>> {
    val e = estado.value
    val juego = e.seleccionado ?: return emptyList()
    val consola = cl.nikalbazu.diptico.data.Consolas.porId(juego.consolaId)
    val todas = filtrarPorEquipo(opcionesJuegoPlanas())
    fun de(vararg inicios: String) = todas.filter { o -> inicios.any { o.titulo.startsWith(it) } }

    val personalizar = buildList {
        addAll(de("Personalizar").map { it.copy(titulo = "Título, logo y música") })
        addAll(de("Imágenes"))
        // Acceso directo desde los ajustes del juego, además del grupo «Emulador y versión».
        consola?.takeIf { !it.sinEmulador }?.let { add(opcionEmuladorJuego(juego, it, "Emulador de este juego")) }
        add(Opcion("Lugar en la vitrina", if (e.extra.disposiciones.containsKey(juego.clave)) "A tu gusto ✓" else "Mover logo, imagen y caja", Icono.PALETA,
            "En una cuadrícula mueves a gusto el logo, la imagen grande de fondo y la caja 3D de este juego. Arriba ves cómo queda.") { abrirEditorVitrina(juego.clave) })
        add(Opcion("Estudio de Cajas", if (e.extra.caras[juego.clave]?.tiene == true) "Tiene caja" else "Con la portada", Icono.PORTADA,
            "La caja 3D a tu gusto: caras con su procedencia, forma, color, material y variantes. Si no está la original, DIPPO la arma con una foto tuya.") { abrirCajaJuego(juego.clave) })
        addAll(de("Imagen en el inicio", "Cajas de", "Imagen en las cajas"))
    }
    val partida = de("Copias de partida", "Estado en mi cola", "Priorizar en la cola")
    val emulador = de("Emulador", "Versión del juego", "Abre en", "Pantalla compañera", "Lupa en este juego")
    val mis = de("Favorito", "DIPPO", "Capturas del juego", "Acceso directo", "Colecciones", "Ocultar este juego", "Mostrar este juego", "Desinstalar")

    val avance = todas.firstOrNull { it.titulo.startsWith("Estado en mi cola") }?.valor.orEmpty()
    val emu = todas.firstOrNull { it.titulo.startsWith("Emulador") }?.valor
    return listOf(
        Opcion("Personalizar", "Nombre, imágenes y caja ›", Icono.PALETA, "Nombre · Imágenes · Lugar en la vitrina · Estudio de Cajas") {} to personalizar,
        Opcion("Partida", if (avance.isNotBlank()) "$avance ›" else "Copias y cola ›", Icono.COPIAR, "Copias de partida · Estado en mi cola") {} to partida,
        Opcion("Emulador y versión", if (!emu.isNullOrBlank()) "$emu ›" else "Pantalla y versión ›", Icono.EMULADOR,
            if (consola?.sinEmulador == true) "Pantalla en que abre · Pantalla compañera" else "Emulador · Versión · Pantalla en que abre") {} to emulador,
        Opcion("Mis cosas", "Favorito, capturas y más ›", Icono.CORAZON, "Favorito · Capturas · Preguntar a DIPPO · Colecciones") {} to mis,
    ).filter { it.second.isNotEmpty() }
}

/** La lista de arriba de la ventana: Mi paso, los grupos y Mover de lugar (en ese orden). */
fun Diptico.opcionesRapido(): List<Opcion> {
    val todas = opcionesJuegoPlanas()
    if (todas.isEmpty()) return emptyList()
    val paso = todas.firstOrNull { it.titulo == "Mi paso" }
    val mover = todas.firstOrNull { it.titulo == "Mover de lugar" }
    val grupos = gruposJuego().mapIndexed { k, (g, _) -> g.copy(accion = { abrirGrupoRapido(k) }) }
    return listOfNotNull(paso) + grupos + listOfNotNull(mover)
}

/** Las opciones del grupo [k] (índice entre los grupos que se ven). */
fun Diptico.opcionesGrupoRapido(k: Int): List<Opcion> = gruposJuego().getOrNull(k)?.second.orEmpty()

fun Diptico.abrirGrupoRapido(k: Int) {
    actualizar { it.copy(extra = it.extra.copy(rapidoGrupo = k, rapidoSel = 0)) }
}

/** B dentro de un grupo vuelve a la ventana. true = había un grupo abierto. */
fun Diptico.cerrarGrupoRapido(): Boolean {
    if (estado.value.extra.rapidoGrupo < 0) return false
    actualizar { it.copy(extra = it.extra.copy(rapidoGrupo = -1, rapidoSel = 0)) }
    return true
}

/** Toca (o elige) la opción [i] del grupo abierto. */
fun Diptico.elegirSubRapido(i: Int) {
    actualizar { it.copy(extra = it.extra.copy(rapidoSel = i)) }
    aceptar()
}

/**
 * Cruceta en la ventana: arriba «Mi paso», al medio los grupos en dos columnas y abajo «Mover de lugar».
 * Dentro de un grupo abierto solo se sube y se baja.
 */
fun Diptico.moverRapido(dx: Int, dy: Int) {
    val e = estado.value
    if (e.extra.rapidoGrupo >= 0) {
        val n = opcionesGrupoRapido(e.extra.rapidoGrupo).size
        if (n == 0 || dy == 0) return
        val s = (e.extra.rapidoSel + dy).coerceIn(0, n - 1)
        if (s != e.extra.rapidoSel) actualizar { it.copy(extra = it.extra.copy(rapidoSel = s)) }
        return
    }
    val total = opcionesRapido().size
    if (total == 0) return
    val g = (total - 2).coerceAtLeast(0) // cuántos grupos hay: índices 1..g
    val i = e.indice
    val n = when {
        // «Mi paso» (0) o «Mover de lugar» (último).
        i == 0 -> if (dy > 0) (if (g > 0) 1 else total - 1) else 0
        i == total - 1 && total > 1 -> if (dy < 0) g.coerceAtLeast(0) else i
        // Grupos: dos columnas.
        dy > 0 -> if (i + 2 <= g) i + 2 else total - 1
        dy < 0 -> if (i - 2 >= 1) i - 2 else 0
        dx > 0 -> if ((i - 1) % 2 == 0 && i + 1 <= g) i + 1 else i
        dx < 0 -> if ((i - 1) % 2 == 1) i - 1 else i
        else -> i
    }
    if (n != i) actualizar { it.copy(indice = n) }
}

// ---------- Caja 3D del juego: dónde se busca y cómo crearla con DIPPO ----------

fun Diptico.abrirCajaJuego(clave: String) {
    // La ventana del juego se cierra y el Estudio de Cajas la reemplaza.
    actualizar { it.copy(panel = Panel.NINGUNO, extra = it.extra.copy(rapidoGrupo = -1, rapidoSel = 0, cajaJuego = clave, cajaFuentes = listOf("", "", ""), cajaSel = 0,
        estudioTab = 0, estudioCara = 0, estudioVista = 0, estudioAbierta = false)) }
    // Con la caja ya guardada no hace falta salir a internet al abrir (se busca con «Buscar arte»).
    if (estado.value.extra.caras[clave]?.tiene != true) buscarCajaPantalla(clave)
}

fun Diptico.cerrarCajaJuego() {
    actualizar { it.copy(extra = it.extra.copy(cajaJuego = "", cajaFuentes = listOf("", "", ""), cajaSel = 0)) }
}

private fun Diptico.fuentesCaja(a: String, b: String, c: String) {
    actualizar { it.copy(extra = it.extra.copy(cajaFuentes = listOf(a, b, c))) }
}

/**
 * Busca la caja mostrando el estado de cada fuente: comunidad → ScreenScraper → TheGamesDB.
 * Al abrir la pantalla ([explicito] false) no reemplaza una caja armada por la persona; con «Buscar de nuevo», sí.
 */
fun Diptico.buscarCajaPantalla(clave: String, explicito: Boolean = false) {
    val e = estado.value
    val j = e.juegosPorClave[clave] ?: return
    val c = e.raspadores
    val ss = c.screenscraper
    val tg = c.conTgdb
    scope.launch {
        fuentesCaja("buscando", "", "")
        // La persona abrió la pantalla de la caja: mira la comunidad, pero no reemplaza sola la caja que ella armó.
        val propia = !explicito && estado.value.extra.caras[clave]?.propia == true
        val enComunidad = !propia && runCatching { cajaDeLaComunidad(clave, respetarPropia = !explicito) }.getOrDefault(false)
        if (enComunidad) { fuentesCaja("hallada", "", ""); return@launch }
        fuentesCaja(if (CajasComunidad.disponible) "no" else "sin", if (ss && !propia) "buscando" else "sin", if (tg && !propia) "buscando" else "sin")
        if ((!ss && !tg) || propia) return@launch
        val hallada = cajaDeInternet(clave, explicito = explicito)
        fuentesCaja(
            if (CajasComunidad.disponible) "no" else "sin",
            if (!ss) "sin" else if (hallada) "hallada" else "no",
            if (!tg) "sin" else if (hallada) "hallada" else "no",
        )
    }
}

// ---------- Vitrina PS5: tamaño de la caja, lugar y logo (ajustables, se guardan) ----------






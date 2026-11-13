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
        add(Opcion("Caja 3D", if (e.extra.caras[juego.clave]?.let { it.frente != null || it.reverso != null } == true) "Tiene caja" else "Con la portada", Icono.PORTADA,
            "Busca la caja original del juego. Si no está, DIPPO la arma con una foto tuya y la puedes compartir con la comunidad.") { abrirCajaJuego(juego.clave) })
        add(Opcion("Lugar en la vitrina", if (e.extra.disposiciones.containsKey(juego.clave)) "A tu gusto ✓" else "Mover logo, imagen y caja", Icono.PALETA,
            "En una cuadrícula mueves a gusto el logo, la imagen grande de fondo y la caja 3D de este juego. Arriba ves cómo queda.") { abrirEditorVitrina(juego.clave) })
        addAll(de("Imágenes").map { it.copy(titulo = "Portada de internet") })
        addAll(de("Personalizar").map { it.copy(titulo = "Título, logo y música") })
        addAll(de("Imagen en el inicio", "Cajas de", "Imagen en las cajas"))
    }
    val partida = de("Copias de partida", "Estado en mi cola", "Priorizar en la cola")
    val emulador = de("Emulador", "Versión del juego", "Abre en", "Pantalla compañera", "Lupa en este juego")
    val mis = de("Favorito", "DIPPO", "Capturas del juego", "Acceso directo", "Colecciones", "Ocultar este juego", "Mostrar este juego", "Desinstalar")

    val avance = todas.firstOrNull { it.titulo.startsWith("Estado en mi cola") }?.valor.orEmpty()
    val emu = todas.firstOrNull { it.titulo == "Emulador" }?.valor
    return listOf(
        Opcion("Personalizar", "Caja, portada y logo ›", Icono.PALETA, "Caja 3D · Portada · Imagen grande · Logo") {} to personalizar,
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
    // La ventana del juego se cierra y la pantalla de la caja la reemplaza.
    actualizar { it.copy(panel = Panel.NINGUNO, extra = it.extra.copy(rapidoGrupo = -1, rapidoSel = 0, cajaJuego = clave, cajaFuentes = listOf("", "", ""), cajaSel = 0)) }
    buscarCajaPantalla(clave)
}

fun Diptico.cerrarCajaJuego() {
    actualizar { it.copy(extra = it.extra.copy(cajaJuego = "", cajaFuentes = listOf("", "", ""), cajaSel = 0)) }
}

private fun Diptico.fuentesCaja(a: String, b: String, c: String) {
    actualizar { it.copy(extra = it.extra.copy(cajaFuentes = listOf(a, b, c))) }
}

/** Busca la caja mostrando el estado de cada fuente: comunidad → ScreenScraper → TheGamesDB. */
fun Diptico.buscarCajaPantalla(clave: String) {
    val e = estado.value
    val j = e.juegosPorClave[clave] ?: return
    val c = e.raspadores
    val ss = c.screenscraper
    val tg = c.conTgdb
    scope.launch {
        fuentesCaja("buscando", "", "")
        val enComunidad = runCatching { cajaDeLaComunidad(clave) }.getOrDefault(false)
        if (enComunidad) { fuentesCaja("hallada", "", ""); return@launch }
        fuentesCaja(if (CajasComunidad.disponible) "no" else "sin", if (ss) "buscando" else "sin", if (tg) "buscando" else "sin")
        if (!ss && !tg) return@launch
        val r = withContext(Dispatchers.IO) { runCatching { Raspadores.caras(c, j.titulo, j.archivo, j.consolaId) }.getOrNull() }
        val hallada = r != null && (r.frente != null || r.reverso != null)
        val g = CarasGuardadas(r?.reverso, r?.lomo, System.currentTimeMillis(), r?.frente)
        actualizar { it.copy(extra = it.extra.copy(caras = it.extra.caras + (clave to g))) }
        almacen.guardarTexto("carasCaja2", CarasGuardadas.escribir(estado.value.extra.caras))
        fuentesCaja(
            if (CajasComunidad.disponible) "no" else "sin",
            if (!ss) "sin" else if (hallada) "hallada" else "no",
            if (!tg) "sin" else if (hallada) "hallada" else "no",
        )
    }
}

/** Las acciones de la pantalla de la caja, en el orden de sus filas. */
fun Diptico.usarCajaJuego(fila: Int = estado.value.extra.cajaSel) {
    val clave = estado.value.extra.cajaJuego.takeIf { it.isNotEmpty() } ?: return
    actualizar { it.copy(extra = it.extra.copy(cajaSel = fila)) }
    when (fila) {
        0 -> armarCajaConImagen(clave)
        1 -> buscarCajaPantalla(clave)
        2 -> { quitarCajaPropia(clave); cerrarCajaJuego() }
        else -> cerrarCajaJuego()
    }
}

fun Diptico.moverCajaJuego(d: Int) {
    actualizar { it.copy(extra = it.extra.copy(cajaSel = (it.extra.cajaSel + d).coerceIn(0, 3))) }
}

// ---------- Vitrina PS5: tamaño de la caja, lugar y logo (ajustables, se guardan) ----------

fun Diptico.cambiarCajaTam(d: Float) {
    val v = (estado.value.extra.cajaTam + d).coerceIn(0.7f, 1.4f)
    almacen.guardarTexto("cajaTam", v.toString())
    actualizar { it.copy(extra = it.extra.copy(cajaTam = v)) }
}

fun Diptico.cambiarCajaCorrida(d: Float) {
    val v = (estado.value.extra.cajaCorrida + d).coerceIn(0f, 0.3f)
    almacen.guardarTexto("cajaCorrida", v.toString())
    actualizar { it.copy(extra = it.extra.copy(cajaCorrida = v)) }
}

fun Diptico.cambiarLogoTam(d: Float) {
    val v = (estado.value.extra.logoTam + d).coerceIn(0.7f, 1.6f)
    almacen.guardarTexto("logoTam", v.toString())
    actualizar { it.copy(extra = it.extra.copy(logoTam = v)) }
}

fun Diptico.reiniciarCajaVitrina() {
    listOf("cajaTam", "cajaCorrida", "logoTam").forEach { almacen.guardarTexto(it, if (it == "cajaCorrida") "0" else "1") }
    actualizar { it.copy(extra = it.extra.copy(cajaTam = 1f, cajaCorrida = 0f, logoTam = 1f)) }
}

package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.data.Consolas

/*
 * «Partida en curso» con una sola pantalla: si vuelves a Díptico con un juego abierto (botón Inicio), la partida no se
 * da por terminada. Díptico muestra a pantalla completa lo que en una consola de dos pantallas va en la de abajo:
 * volver al juego, tu paso, DIPPO y las herramientas. La partida termina cuando tú lo dices o al abrir otro juego.
 */

/** No hay otra pantalla libre para acompañar el juego. */
val Estado.sinCompanera: Boolean get() = unaPantalla || !dosDisponibles

/** ¿Se ve «Partida en curso» ahora? */
val Estado.partidaEnPausa: Boolean get() = sesion != null && extra.partidaPausa > 0L && sinCompanera

/** Botones de la pantalla, en el orden de la cruceta: 0 = Volver al juego y luego las herramientas. */
val HERRAMIENTAS_PARTIDA = listOf("Pista", "Mi paso", "Guías y trucos", "Mapa", "Hablar con DIPPO", "Ficha del juego")

fun Diptico.pausarPartidaUnica() {
    actualizar { if (it.extra.partidaPausa > 0L) it else it.copy(panel = Panel.NINGUNO, extra = it.extra.copy(partidaPausa = System.currentTimeMillis(), partidaSel = 0)) }
}

fun Diptico.volverAlJuegoUnica() {
    val e = estado.value
    val s = e.sesion ?: return
    val j = e.juegosPorClave[s.clave] ?: run { terminarPartidaUnica(); return }
    val c = Consolas.porId(j.consolaId) ?: run { terminarPartidaUnica(); return }
    // El rato en Díptico no cuenta como tiempo de juego: el inicio de la sesión se corre lo que duró la pausa.
    val pausa = e.extra.partidaPausa
    val corrida = if (pausa > 0L) s.copy(inicio = s.inicio + (System.currentTimeMillis() - pausa)) else s
    actualizar { it.copy(sesion = corrida, extra = it.extra.copy(partidaPausa = 0L, partidaSel = 0)) }
    volverALaPartida(j, c, corrida)
}

/** Termina la partida: cuenta el tiempo hasta que volviste a Díptico (no el rato que estuviste aquí). */
fun Diptico.terminarPartidaUnica() {
    val pausa = estado.value.extra.partidaPausa
    cerrarSesion(preguntar = true, fin = pausa)
}

fun Diptico.moverPartidaUnica(paso: Int) {
    actualizar { it.copy(extra = it.extra.copy(partidaSel = (it.extra.partidaSel + paso).coerceIn(0, HERRAMIENTAS_PARTIDA.size))) }
}

fun Diptico.usarPartidaUnica(i: Int = estado.value.extra.partidaSel) {
    val s = estado.value.sesion ?: return
    when (i) {
        0 -> volverAlJuegoUnica()
        1 -> { prepararAyuda(); ejecutarAccion(AccionChat("bisa_pista", s.clave, "")) }
        2 -> abrirPaso()
        3 -> abrirFichaDe(s.clave, 2)
        4 -> abrirMapa(s.clave)
        5 -> prepararAyuda()
        else -> abrirFichaDe(s.clave, 0)
    }
}

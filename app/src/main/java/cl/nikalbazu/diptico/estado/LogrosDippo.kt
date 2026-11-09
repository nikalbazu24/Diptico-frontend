package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.data.Consolas

/**
 * Los logros de DIPPO: los suyos, aparte de RetroAchievements. Premian jugar y también conversar con él.
 * Cada logro mira un número (horas, partidas, preguntas…) y se gana al llegar a su [meta].
 */
object LogrosDippo {
    /** Lo que DIPPO va contando aparte de tus datos de juego: preguntas, voz, pistas, ideas, temas y días jugados. */
    data class Contadores(val numeros: Map<String, Int> = emptyMap(), val dias: Set<String> = emptySet()) {
        fun n(k: String) = numeros[k] ?: 0
    }

    data class Logro(val id: String, val titulo: String, val descripcion: String, val icono: Icono, val meta: Int, val valor: (Estado, Contadores) -> Int)

    private fun horas(e: Estado) = (e.datos.values.sumOf { it.tiempo.toLong() } / 3600).toInt()
    private fun partidas(e: Estado) = e.datos.values.sumOf { it.sesiones }
    private fun terminados(e: Estado) = e.datos.values.count { it.avance == AvanceJuego.TERMINADO.name }
    private fun jugados(e: Estado) = e.datos.values.count { it.tiempo > 0 }
    private fun consolas(e: Estado): Int {
        val porClave = e.juegosPorClave
        return e.datos.filter { it.value.tiempo > 0 }.keys.mapNotNull { porClave[it]?.consolaId }.filter { Consolas.porId(it) != null }.toSet().size
    }

    /** Días seguidos jugando hasta hoy (o hasta ayer, si hoy todavía no juegas). */
    fun racha(dias: Set<String>): Int {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT)
        val c = java.util.Calendar.getInstance()
        if (f.format(c.time) !in dias) c.add(java.util.Calendar.DAY_OF_YEAR, -1)
        var n = 0
        while (f.format(c.time) in dias) { n++; c.add(java.util.Calendar.DAY_OF_YEAR, -1) }
        return n
    }

    val LISTA: List<Logro> = listOf(
        Logro("hola", "¡Hola, DIPPO!", "Abre a DIPPO por primera vez.", Icono.BISA, 1) { _, c -> c.n("abrir") },
        Logro("amigos", "Buenos amigos", "Hazle 10 preguntas a DIPPO.", Icono.BISA, 10) { _, c -> c.n("preguntas") },
        Logro("confidentes", "Confidentes", "Hazle 50 preguntas a DIPPO.", Icono.CORAZON, 50) { _, c -> c.n("preguntas") },
        Logro("voz", "Te escucho", "Háblale a DIPPO 5 veces con tu voz.", Icono.MICROFONO, 5) { _, c -> c.n("voz") },
        Logro("pistas", "Sin spoilers", "Pídele 3 pistas a DIPPO.", Icono.INFO, 3) { _, c -> c.n("pistas") },
        Logro("ideas", "Le hice caso", "Acepta 5 ideas de DIPPO (su globito o sus botones).", Icono.CHISPA, 5) { _, c -> c.n("ideas") },
        Logro("primera", "¡A jugar!", "Juega tu primera partida con Díptico.", Icono.JUGAR, 1) { e, _ -> partidas(e) },
        Logro("cien", "Maratón", "Juega 100 partidas.", Icono.MANDO, 100) { e, _ -> partidas(e) },
        Logro("diez_h", "10 horas juntos", "Suma 10 horas jugando.", Icono.RELOJ, 10) { e, _ -> horas(e) },
        Logro("cien_h", "100 horas juntos", "Suma 100 horas jugando.", Icono.RELOJ, 100) { e, _ -> horas(e) },
        Logro("final", "Créditos finales", "Marca un juego como terminado.", Icono.TROFEO, 1) { e, _ -> terminados(e) },
        Logro("finales", "Coleccionista de finales", "Termina 10 juegos.", Icono.TROFEO, 10) { e, _ -> terminados(e) },
        Logro("explorador", "Explorador", "Juega 20 juegos distintos.", Icono.MUNDO, 20) { e, _ -> jugados(e) },
        Logro("trotamundos", "Trotamundos", "Juega en 5 consolas distintas.", Icono.PANTALLAS, 5) { e, _ -> consolas(e) },
        Logro("racha3", "Racha de 3 días", "Juega 3 días seguidos.", Icono.ESTRELLA, 3) { _, c -> racha(c.dias) },
        Logro("racha7", "Semana perfecta", "Juega 7 días seguidos.", Icono.ESTRELLA, 7) { _, c -> racha(c.dias) },
        Logro("buho", "Búho nocturno", "Juega entre la 1 y las 5 de la mañana.", Icono.RELOJ, 1) { _, c -> c.n("noche") },
        Logro("madrugador", "Madrugador", "Juega entre las 5 y las 7 de la mañana.", Icono.RELOJ, 1) { _, c -> c.n("madrugada") },
        Logro("memoria", "Buena memoria", "Anota tu próximo paso en 5 juegos.", Icono.LAPIZ, 5) { e, _ -> e.datos.values.count { it.paso.isNotBlank() } },
        Logro("corazon", "Favoritos del corazón", "Marca 10 juegos como favoritos.", Icono.CORAZON, 10) { e, _ -> e.datos.values.count { it.favorito } },
        Logro("look", "Cambio de look", "Cambia de tema 3 veces.", Icono.PALETA, 3) { _, c -> c.n("temas") },
    )

    /** Cada logro con su avance (sin pasarse de la meta). */
    fun progreso(e: Estado, c: Contadores): List<Pair<Logro, Int>> = LISTA.map { it to it.valor(e, c).coerceAtMost(it.meta) }

    fun leerNumeros(s: String): Map<String, Int> = s.split(';').mapNotNull { p ->
        val (k, v) = p.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
        v.toIntOrNull()?.let { k to it }
    }.toMap()

    fun escribirNumeros(m: Map<String, Int>) = m.entries.joinToString(";") { "${it.key}=${it.value}" }
}

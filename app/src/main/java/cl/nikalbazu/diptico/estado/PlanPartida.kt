package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.data.Juego

enum class VistaPartida(val titulo: String, val icono: Icono) {
    CLASICA("Portada", Icono.PORTADA),
    BITACORA("Bitácora", Icono.LAPIZ),
    HERRAMIENTAS("Herramientas", Icono.AJUSTES),
    LIGERA("Ligera", Icono.NEGRO);

    companion object {
        fun leer(valor: String?) = entries.firstOrNull { it.name == valor } ?: CLASICA
    }
}

enum class AvanceJuego(val titulo: String) {
    SIN_ESTADO("Sin estado"), PENDIENTE("Pendiente"), JUGANDO("Jugando"), TERMINADO("Terminado");

    companion object {
        fun leer(valor: String?) = entries.firstOrNull { it.name == valor } ?: SIN_ESTADO
    }
}

internal object PlanPartida {
    fun cola(e: Estado): List<Juego> = e.juegos.asSequence()
        .filter { e.datos[it.clave]?.let { d -> !d.oculto && AvanceJuego.leer(d.avance) in setOf(AvanceJuego.PENDIENTE, AvanceJuego.JUGANDO) } == true }
        .sortedBy { e.datos[it.clave]?.puestoCola ?: Long.MAX_VALUE }
        .take(30).toList()

    fun breves(e: Estado): List<Juego> = e.juegos.asSequence().mapNotNull { juego ->
        val d = e.datos[juego.clave] ?: return@mapNotNull null
        if (d.oculto || d.sesiones < 2 || d.avance == AvanceJuego.TERMINADO.name) return@mapNotNull null
        val duracion = d.tiempo / d.sesiones
        if (duracion in 1..1_200L) juego to duracion else null
    }.sortedBy { it.second }.take(8).map { it.first }.toList()
}

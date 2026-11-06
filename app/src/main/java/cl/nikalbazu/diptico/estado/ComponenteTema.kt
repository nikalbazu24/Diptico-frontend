package cl.nikalbazu.diptico.estado

/** Cada elección sustituye un grupo de atributos de Mi tema. */
internal enum class ComponenteTema(val titulo: String) {
    COLORES("Colores"),
    FONDO("Fondo"),
    TIPOGRAFIA("Tipografía"),
    FORMAS("Formas y acabado"),
    DECORACION("Decoración"),
    EFECTOS("Efectos de sonido"),
    MUSICA("Música");

    fun combinar(destino: Tema, fuente: Tema): Tema = when (this) {
        COLORES -> destino.copy(acento = fuente.acento, acento2 = fuente.acento2, acento3 = fuente.acento3, tinta = fuente.tinta)
        FONDO -> destino.copy(base = fuente.base, fondo = fuente.fondo, trama = fuente.trama)
        TIPOGRAFIA -> destino.copy(fuente = fuente.fuente)
        FORMAS -> destino.copy(forma = fuente.forma, acabado = fuente.acabado)
        DECORACION -> destino.copy(decor = fuente.decor)
        EFECTOS -> destino.copy(sonidos = fuente.sonidos, tonoSonidos = fuente.tonoSonidos)
        MUSICA -> destino.copy(musica = fuente.musica, pistaBase = Diptico.pistaIncluidaDeTema(fuente))
    }
}

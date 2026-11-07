package cl.nikalbazu.diptico.estado

enum class ZonaGlass(val clave: String, val nombre: String) {
    CARPETAS("glassCarpetas", "carpetas"), WIDGETS("glassWidgets", "widgets")
}

/** Intensidades independientes; los temas no sustituyen estas preferencias. */
data class NivelesGlass(val carpetas: Int = 70, val widgets: Int = 55) {
    fun para(zona: ZonaGlass) = if (zona == ZonaGlass.CARPETAS) carpetas else widgets
    fun cambiar(zona: ZonaGlass, valor: Int): NivelesGlass =
        if (zona == ZonaGlass.CARPETAS) copy(carpetas = valor.coerceIn(0, 100)) else copy(widgets = valor.coerceIn(0, 100))
}

data class RangoOpcion(val valor: Int, val alCambiar: (Int) -> Unit)

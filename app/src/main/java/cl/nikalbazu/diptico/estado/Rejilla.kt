package cl.nikalbazu.diptico.estado

/** Distribución determinista: nunca oculta un widget por reducir las casillas; agrega páginas si hacen falta. */
internal object Rejilla {
    data class Resultado(val widgets: List<Widget>, val paginas: Int)
    fun distribuir(widgets: List<Widget>, columnas: Int, filas: Int, paginas: Int): Resultado {
        val cols = columnas.coerceIn(4, 8); val rows = filas.coerceIn(3, 8)
        var total = paginas.coerceAtLeast(1)
        val ocupadas = mutableSetOf<Triple<Int, Int, Int>>()
        fun cabe(p: Int, x: Int, y: Int, an: Int, al: Int) = x >= 0 && y >= 0 && x + an <= cols && y + al <= rows &&
            (x until x + an).all { cx -> (y until y + al).all { cy -> Triple(p, cx, cy) !in ocupadas } }
        val nuevos = widgets.map { w ->
            if (!w.visible) return@map w.copy(an = w.an.coerceIn(1, cols), al = w.al.coerceIn(1, rows), x = 0, y = 0)
            val an = w.an.coerceIn(1, cols); val al = w.al.coerceIn(1, rows)
            var destino: Triple<Int, Int, Int>? = null
            if (w.pagina in 0 until total && cabe(w.pagina, w.x, w.y, an, al)) destino = Triple(w.pagina, w.x, w.y)
            if (destino == null) buscar@ for (p in 0 until total) for (y in 0..rows - al) for (x in 0..cols - an) {
                if (cabe(p, x, y, an, al)) { destino = Triple(p, x, y); break@buscar }
            }
            val d = destino ?: Triple(total++, 0, 0)
            for (x in d.second until d.second + an) for (y in d.third until d.third + al) ocupadas += Triple(d.first, x, y)
            w.copy(x = d.second, y = d.third, an = an, al = al, pagina = d.first)
        }
        return Resultado(nuevos, total)
    }
}

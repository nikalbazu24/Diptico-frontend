package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.data.Biblioteca
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego

/**
 * El resumen de «Volver a buscar juegos»: cuántos hay, cuántos son nuevos, cuáles ya no aparecen (sus datos se
 * guardan igual: tiempo, favoritos, cajas y personalizaciones vuelven si el archivo vuelve) y qué se dejó fuera.
 */
internal fun Diptico.mostrarInformeEscaneo(
    informe: Biblioteca.Informe, antes: List<Juego>, ahora: List<Juego>, fallas: Int, carpetas: Int,
) {
    val claves = antes.mapTo(HashSet()) { it.clave }
    val nuevas = ahora.mapTo(HashSet()) { it.clave }
    val nuevos = ahora.count { it.clave !in claves }
    val faltan = antes.count { it.clave !in nuevas }
    val texto = Informes.resumen(ahora.size, nuevos, ahora.size - nuevos, faltan, informe, fallas, carpetas)
    mostrarDialogo(Dialogo(
        "informe-escaneo", "Encontré ${ahora.size} juegos", texto, Icono.BUSCAR,
        lineas = Informes.lineas(informe), si = "Listo", no = "Cerrar", lineasTexto = 10,
    )) {}
}

/** Textos del informe (aparte, para poder probarlos sin el estado). */
object Informes {
    fun resumen(total: Int, nuevos: Int, yaEstaban: Int, faltan: Int, inf: Biblioteca.Informe, fallas: Int, carpetas: Int): String = buildString {
        append(if (nuevos == 0) "Ninguno nuevo" else "$nuevos ${if (nuevos == 1) "nuevo" else "nuevos"}")
        append(" · $yaEstaban ya estaban")
        if (faltan > 0) append(" · $faltan ${if (faltan == 1) "ya no está" else "ya no están"} en la carpeta (su tiempo, favoritos y cajas se guardan por si vuelve${if (faltan == 1) "" else "n"})")
        append(".")
        val consolas = inf.porConsola.filterValues { it > 0 }.entries.sortedByDescending { it.value }.take(4)
            .joinToString(", ") { "${Consolas.porId(it.key)?.bonito ?: it.key} ${it.value}" }
        if (consolas.isNotEmpty()) append("\n").append(consolas)
        if (inf.enSubcarpetas > 0) append("\n${inf.enSubcarpetas} estaban dentro de su propia carpeta.")
        if (inf.juntados > 0) append("\nJunté ${inf.juntados} ${if (inf.juntados == 1) "versión repetida" else "versiones repetidas"} (quedan en la ficha del juego).")
        if (fallas > 0) append("\nNo pude leer $fallas de tus $carpetas carpetas: elígelas otra vez en Carpetas de juegos.")
        if (inf.sinConsola.isNotEmpty()) append("\nCarpetas sin nombre de consola: ${inf.sinConsola.take(4).joinToString()}. Renómbralas (por ejemplo «ps2») o elige su consola en Carpetas de juegos.")
        if (inf.totalOmitidos > 0) append("\nDejé fuera ${inf.totalOmitidos} ${if (inf.totalOmitidos == 1) "archivo" else "archivos"}:")
    }

    fun lineas(inf: Biblioteca.Informe): List<String> = inf.omitidos.map { (archivo, motivo) -> "$archivo: $motivo" } +
        if (inf.totalOmitidos > inf.omitidos.size) listOf("y ${inf.totalOmitidos - inf.omitidos.size} más") else emptyList()
}

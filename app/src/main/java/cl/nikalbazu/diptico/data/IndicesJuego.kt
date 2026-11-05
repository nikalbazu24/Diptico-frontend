package cl.nikalbazu.diptico.data

import java.io.InputStream
import java.util.Locale

internal object IndicesJuego {
    private val cue = Regex("""^\s*FILE\s+(?:"([^"]+)"|(\S+))""", RegexOption.IGNORE_CASE)
    private val gdi = Regex("""^\s*\d+\s+\d+\s+\d+\s+\d+\s+("[^"]+"|\S+)""")

    fun leer(entrada: InputStream): String? {
        val bytes = ByteArray(65_537)
        var total = 0
        while (total < bytes.size) {
            val n = entrada.read(bytes, total, bytes.size - total)
            if (n < 0) return bytes.decodeToString(0, total)
            total += n
        }
        return null
    }

    fun referencias(texto: String, extension: String): List<String> {
        val lineas = texto.removePrefix("\uFEFF").lineSequence()
        val nombres = when (extension.lowercase(Locale.ROOT)) {
            "cue" -> lineas.mapNotNull { cue.find(it)?.let { m -> m.groupValues[1].ifBlank { m.groupValues[2] } } }
            "gdi" -> lineas.drop(1).mapNotNull { gdi.find(it)?.groupValues?.get(1)?.removeSurrounding("\"") }
            "m3u" -> lineas.map { it.trim().removeSurrounding("\"") }.filter { !it.startsWith("#") }
            else -> emptySequence()
        }
        return nombres.mapNotNull { nombre ->
            val ruta = nombre.trim().replace('\\', '/')
            if (ruta.startsWith('/') || ':' in ruta || '\u0000' in ruta) return@mapNotNull null
            val partes = ruta.split('/').filter { it.isNotBlank() && it != "." }
            if (partes.isEmpty() || ".." in partes) null else partes.joinToString("/")
        }.distinct().take(256).toList()
    }
}

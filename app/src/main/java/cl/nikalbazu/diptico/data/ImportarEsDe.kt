package cl.nikalbazu.diptico.data

import android.util.Xml
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale
import org.xmlpull.v1.XmlPullParser

internal object ImportarEsDe {
    data class Entrada(val archivo: String, val titulo: String, val favorito: Boolean)
    data class Cambio(val juego: Juego, val titulo: String?, val favorito: Boolean)
    data class Previa(val cambios: List<Cambio>, val sinCoincidencia: Int)

    fun leer(entrada: InputStream): List<Entrada> {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = entrada.read(buffer)
            if (n < 0) break
            require(bytes.size() + n <= 8 * 1024 * 1024)
            bytes.write(buffer, 0, n)
        }
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        parser.setInput(bytes.toByteArray().inputStream(), null)
        require(parser.nextTag() == XmlPullParser.START_TAG && parser.name == "gameList")
        val juegos = ArrayList<Entrada>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG || parser.depth != 2 || parser.name != "game") continue
            var ruta = ""
            var titulo = ""
            var favorito = false
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.END_TAG && parser.depth == 2) break
                if (parser.eventType != XmlPullParser.START_TAG || parser.depth != 3) continue
                when (parser.name) {
                    "path" -> ruta = parser.nextText().trim().replace('\\', '/').substringAfterLast('/')
                    "name" -> titulo = parser.nextText().trim().take(180)
                    "favorite" -> favorito = parser.nextText().trim().equals("true", ignoreCase = true)
                }
            }
            if (ruta.isNotBlank()) juegos += Entrada(ruta, titulo, favorito)
        }
        return juegos
    }

    fun comparar(entradas: List<Entrada>, biblioteca: List<Juego>, datos: Map<String, DatosJuego>): Previa {
        val archivos = biblioteca.groupBy { it.archivo.lowercase(Locale.ROOT) }
        var sinCoincidencia = 0
        val cambios = entradas.distinctBy { it.archivo.lowercase(Locale.ROOT) }.mapNotNull { entrada ->
            val juego = archivos[entrada.archivo.lowercase(Locale.ROOT)]?.singleOrNull()
            if (juego == null) {
                sinCoincidencia++
                return@mapNotNull null
            }
            val d = datos[juego.clave]
            val titulo = entrada.titulo.takeIf { it.isNotBlank() && it != juego.titulo && d?.titulo.isNullOrBlank() }
            val favorito = entrada.favorito && d?.favorito != true
            if (titulo == null && !favorito) null else Cambio(juego, titulo, favorito)
        }
        return Previa(cambios, sinCoincidencia)
    }
}

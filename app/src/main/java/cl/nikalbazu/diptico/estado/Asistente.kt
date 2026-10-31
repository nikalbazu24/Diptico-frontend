package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.data.Consolas
import java.text.SimpleDateFormat
import java.util.Date

/** Algo que el asistente propone hacer: un botón debajo de su respuesta. */
data class AccionChat(val tipo: String, val valor: String, val etiqueta: String)

/** Un mensaje del chat: "yo" (la persona) o "ia". */
data class MensajeChat(val rol: String, val texto: String, val acciones: List<AccionChat> = emptyList())

/**
 * El asistente de Díptico. Corre en la nube (Claude, con la clave de la persona),
 * así que no gasta memoria ni batería de la consola: solo manda un texto corto y recibe la respuesta.
 */
object Asistente {
    /** Juegos que se mandan como contexto (por número, para que la IA proponga botones). */
    private var indice: List<String> = emptyList()

    val sugerencias = listOf(
        "¿Qué juego me recomiendas hoy?",
        "Quiero algo corto para 20 minutos",
        "¿Cuál es mi juego más jugado?",
        "Cámbiame a un tema de Halloween",
        "Dame un truco para el juego elegido",
        "¿Qué juegos clásicos me faltan?",
    )

    /** Instrucciones con lo que hay en la biblioteca, en pocas líneas. */
    fun sistema(e: Estado): String {
        val juegos = e.juegos.filter { e.datos[it.clave]?.oculto != true }
        val jugados = e.datos.entries.filter { it.value.tiempo > 0 }
        val recientes = e.datos.entries.filter { it.value.ultimo > 1 }.sortedByDescending { it.value.ultimo }.take(6).map { it.key }
        val masJugados = jugados.sortedByDescending { it.value.tiempo }.take(6).map { it.key }
        val favoritos = e.datos.filter { it.value.favorito }.keys.take(10)
        // Lista de juegos: primero los conocidos (recientes, favoritos, más jugados), después el resto.
        val prioridad = (listOfNotNull(e.seleccionado?.clave) + recientes + favoritos + masJugados).distinct()
        val resto = juegos.map { it.clave }.filter { it !in prioridad }.shuffled(java.util.Random(juegos.size.toLong())).take(260)
        indice = (prioridad + resto).filter { c -> juegos.any { it.clave == c } }
        fun linea(i: Int, clave: String): String {
            val j = juegos.first { it.clave == clave }
            val d = e.datos[clave]
            val c = Consolas.porId(j.consolaId)?.nombre ?: j.consolaId
            val extra = buildString {
                if (d != null && d.tiempo > 0) append(", ${d.tiempo / 3600}h${(d.tiempo % 3600) / 60}m jugadas")
                if (d?.favorito == true) append(", favorito")
                if (d?.paso?.isNotBlank() == true) append(", próximo paso: \"${d.paso.take(60)}\"")
            }
            return "#$i ${j.titulo} ($c$extra)"
        }
        val lista = indice.mapIndexed { i, c -> linea(i, c) }.joinToString("\n")
        val porConsola = juegos.groupBy { it.consolaId }.entries.sortedByDescending { it.value.size }
            .joinToString(", ") { (c, l) -> "${Consolas.porId(c)?.nombre ?: c}: ${l.size}" }
        val temas = Diptico.todosLosTemas(e).take(40).joinToString(", ") { "${it.id}=${it.nombre}" }
        val idioma = Idioma.nombreParaIA(e.idioma)
        return """
            Eres el asistente de Díptico, un launcher de juegos para la consola portátil de dos pantallas AYN Thor.
            Responde SIEMPRE en $idioma, breve (2 a 6 oraciones), cálido y concreto. Nada de markdown pesado: texto simple, como mucho guiones.
            Hoy es ${SimpleDateFormat("EEEE d 'de' MMMM yyyy", java.util.Locale("es")).format(Date())}.
            Sabes mucho de videojuegos: trucos, guías, historia, cuánto duran, a quién le gustan.

            Puedes proponer acciones que la persona activa con un botón. Escríbelas al final, cada una en su propia línea, con este formato exacto:
            [[jugar:#N|texto del botón]]  abre el juego número N de la lista
            [[ver:#N|texto]]              selecciona el juego N en el estante
            [[ficha:#N|texto]]            abre la historia, guías y logros del juego N
            [[tema:ID|texto]]             cambia el tema (IDs abajo)
            [[vista:estante|texto]] o [[vista:cuadricula|texto]] o [[vista:lista|texto]]
            [[buscar:palabra|texto]]      busca en la biblioteca
            [[menu:SECCION|texto]]        abre el menú en PANTALLAS, INICIO, ESTANTE, TEMAS, APARIENCIA, MUSICA, EMULADORES, PORTADAS, BIBLIOTECA, LOGROS, IDIOMA o SISTEMA
            Propón como mucho 3 acciones y solo si ayudan. Nunca inventes números de juego: usa solo los de la lista.
            Si te preguntan por un juego que no está en la lista, puedes hablar de él pero di que no está en su biblioteca.

            Biblioteca: ${juegos.size} juegos. Por consola: $porConsola.
            Juego elegido ahora: ${e.seleccionado?.let { s -> "#${indice.indexOf(s.clave)} ${s.titulo}" } ?: "ninguno"}.
            Tema actual: ${cl.nikalbazu.diptico.ui.Estilo.tema.nombre}. Vista: ${e.vista}.
            Temas disponibles: $temas.

            Juegos (lista parcial si son muchos):
            $lista
        """.trimIndent()
    }

    private val ACCION = Regex("""\[\[(\w+):([^|\]]+)\|([^\]]+)]]""")

    /** Separa el texto de la respuesta y los botones que propone. */
    fun interpretar(respuesta: String): MensajeChat {
        val acciones = ACCION.findAll(respuesta).mapNotNull { m ->
            val (tipo, valor, etiqueta) = m.destructured
            val v = valor.trim()
            val real = if (tipo in setOf("jugar", "ver", "ficha")) {
                val n = v.removePrefix("#").toIntOrNull() ?: return@mapNotNull null
                indice.getOrNull(n) ?: return@mapNotNull null
            } else v
            AccionChat(tipo, real, etiqueta.trim())
        }.take(3).toList()
        val texto = respuesta.replace(ACCION, "").replace(Regex("\n{3,}"), "\n\n").replace("**", "").trim()
        return MensajeChat("ia", texto, acciones)
    }
}

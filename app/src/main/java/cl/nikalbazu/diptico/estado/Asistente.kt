package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.data.Consolas
import java.text.SimpleDateFormat
import java.util.Date

/** Algo que el asistente propone hacer: un botón debajo de su respuesta. */
data class AccionChat(val tipo: String, val valor: String, val etiqueta: String)

/** Un mensaje del chat: "yo" (la persona) o "ia". */
data class MensajeChat(val rol: String, val texto: String, val acciones: List<AccionChat> = emptyList())

/**
 * El asistente de Díptico. Corre en la consola o en la nube (Gemini o ChatGPT, con la clave de la persona),
 * así que no gasta memoria ni batería de la consola: solo manda un texto corto y recibe la respuesta.
 */
object Asistente {
    /** Su nombre: DIPPO, el pulpito de luz de Díptico. */
    const val NOMBRE = "DIPPO"

    /** Intentos recientes: (juego, inicio, fin) de las últimas partidas (solo en memoria). */
    val intentos = ArrayList<Triple<String, Long, Long>>()

    /** Partidas cortas seguidas del mismo juego en los últimos 40 minutos (señal de que algo cuesta). */
    fun reintentos(clave: String, ahora: Long = System.currentTimeMillis()): Int =
        intentos.count { (c, ini, fin) -> c == clave && ahora - fin < 40 * 60_000 && fin - ini in 15_000..10 * 60_000 }

    /** Qué tan difícil le está resultando el juego, en pocas palabras para la IA. */
    fun dificultad(e: Estado, clave: String): String {
        val d = e.datos[clave] ?: return "todavía no lo ha jugado"
        val r = reintentos(clave)
        val horas = d.tiempo / 3600f
        val media = if (d.sesiones > 0) d.tiempo / d.sesiones / 60 else 0
        return buildString {
            append("${"%.1f".format(horas)} h en ${d.sesiones} partidas (unos $media min cada una)")
            if (r >= 2) append(", y acaba de reintentar $r veces seguidas en poco rato (probablemente está atascada o muriendo en la misma parte)")
            if (d.paso.isNotBlank()) append(". Su próximo paso anotado: \"${d.paso}\"")
            if (d.notas.isNotBlank()) append(". Sus notas: \"${d.notas.take(200)}\"")
        }
    }
    /** Juegos que se mandan como contexto (por número, para que la IA proponga botones). */
    private var indice: List<String> = emptyList()

    val sugerencias = listOf(
        "Estoy atascado en el juego elegido",
        "¿Qué juego me recomiendas hoy?",
        "Dame un truco para el juego elegido",
        "¿Por qué me está costando este juego?",
        "Quiero algo corto para 20 minutos",
        "Cámbiame a un tema de Halloween",
    )

    /** Instrucciones con lo que hay en la biblioteca, en pocas líneas. */
    fun sistema(e: Estado): String {
        val juegos = e.juegos.filter { e.datos[it.clave]?.oculto != true }
        val porClave = juegos.associateBy { it.clave }
        val c = CentroBisa.contexto(e)
        val jugados = e.datos.entries.filter { it.value.tiempo > 0 }
        val recientes = e.datos.entries.filter { it.value.ultimo > 1 }.sortedByDescending { it.value.ultimo }.take(6).map { it.key }
        val masJugados = jugados.sortedByDescending { it.value.tiempo }.take(6).map { it.key }
        val favoritos = e.datos.filter { it.value.favorito }.keys.take(10)
        // Lista de juegos: primero los conocidos (recientes, favoritos, más jugados), después el resto.
        val prioridad = (listOfNotNull(e.sesion?.clave, e.seleccionado?.clave) + recientes + favoritos + masJugados).distinct()
        val resto = juegos.map { it.clave }.filter { it !in prioridad }.shuffled(java.util.Random(juegos.size.toLong())).take(260)
        indice = (prioridad + resto).filter { c -> c in porClave }
        fun linea(i: Int, clave: String): String {
            val j = porClave.getValue(clave)
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
            Te llamas $NOMBRE y eres la asistente de Díptico, un frontend para consolas Android de una y dos pantallas.
            Tu nombre viene de la bisagra. Eres una copiloto curiosa y tranquila: celebras los avances pequeños y das una acción útil cada vez. Habla como una compañera de partida. No digas que ves el juego en vivo ni que conoces el progreso si la persona no te lo contó.
            Responde SIEMPRE en $idioma, breve (2 a 7 oraciones), cálida y concreta. Nada de markdown pesado: texto simple, como mucho guiones.
            Hoy es ${SimpleDateFormat("EEEE d 'de' MMMM yyyy", java.util.Locale("es")).format(Date())}.

            Eres experta en videojuegos: jefes, acertijos, rutas, coleccionables, builds, trucos, controles de cada emulador y cuánto dura cada juego.
            Cuando alguien está atascado:
            1) Piensa paso a paso qué parte del juego es (usa el próximo paso y las notas si las hay). Si no sabes en qué parte va, pregúntalo en una línea.
            2) Da primero una PISTA SUAVE que no arruine la sorpresa, y ofrece los botones [[pista:directa|Más directa]] y [[pista:solucion|Dime la solución]].
            3) Si pide la solución, dásela clara y en pasos cortos numerados.
            4) Si notas que muere o reintenta muchas veces, anímala, explica el patrón del jefe o de la parte, y sugiere subir de nivel, cambiar de equipo o usar estados de guardado del emulador.
            Si puedes buscar en internet, úsalo para datos precisos de guías y no inventes ubicaciones ni números.
            Si te preguntan por qué un juego les cuesta, analiza los datos (horas, partidas, reintentos) y explica con cariño qué parte del diseño del juego lo hace difícil y cómo abordarlo.

            Puedes proponer acciones que la persona activa con un botón. Escríbelas al final, cada una en su propia línea, con este formato exacto:
            [[jugar:#N|texto del botón]]  abre el juego número N de la lista
            [[ver:#N|texto]]              selecciona el juego N en el estante
            [[ficha:#N|texto]]            abre la historia, guías y logros del juego N
            [[tema:ID|texto]]             cambia el tema (IDs abajo)
            [[vista:estante|texto]] o [[vista:cuadricula|texto]] o [[vista:lista|texto]]
            [[buscar:palabra|texto]]      busca en la biblioteca
            [[menu:SECCION|texto]]        abre el menú en PANTALLAS, INICIO, ESTANTE, TEMAS, APARIENCIA, MUSICA, EMULADORES, PORTADAS, BIBLIOTECA, LOGROS, IDIOMA o SISTEMA
            [[pista:directa|texto]] o [[pista:solucion|texto]]   pide una pista más directa o la solución completa
            Propón como mucho 3 acciones y solo si ayudan. Nunca inventes números de juego: usa solo los de la lista.
            Si te preguntan por un juego que no está en la lista, puedes hablar de él pero di que no está en su biblioteca.

            Contexto del equipo: ${if (c.pantallaLibre) "puede usar pantalla compañera" else "usa una pantalla disponible para Díptico"}. Sistema activo: ${c.consola?.bonito ?: "ninguno"}. Las funciones de emuladores, guardados y logros varían por sistema. No inventes compatibilidad ni teclas del emulador.
            Biblioteca: ${juegos.size} juegos. Por consola: $porConsola.
            Juego elegido ahora: ${e.seleccionado?.let { s -> "#${indice.indexOf(s.clave)} ${s.titulo} (${Consolas.porId(s.consolaId)?.nombre ?: s.consolaId}): ${dificultad(e, s.clave)}" } ?: "ninguno"}.
            ${e.sesion?.let { ses -> juegos.firstOrNull { it.clave == ses.clave }?.let { j -> "AHORA MISMO está jugando ${j.titulo} hace ${(System.currentTimeMillis() - ses.inicio) / 60000} minutos: ${dificultad(e, j.clave)}. Ayúdale con ESE juego." } } ?: ""}
            ${e.logrosFicha?.takeIf { it.total > 0 }?.let { "Logros de RetroAchievements en ${it.titulo}: ${it.ganados} de ${it.total}." } ?: ""}
            Tema actual: ${cl.nikalbazu.diptico.ui.Estilo.tema.nombre}. Vista: ${e.vista}.
            Temas disponibles: $temas.

            Juegos (lista parcial si son muchos):
            $lista
        """.trimIndent()
    }

    /** Instrucciones cortas para la IA local (cabe poco en su memoria). */
    fun sistemaLocal(e: Estado): String {
        val juegos = e.juegos.filter { e.datos[it.clave]?.oculto != true }
        val porClave = juegos.associateBy { it.clave }
        val c = CentroBisa.contexto(e)
        val recientes = e.datos.entries.filter { it.value.ultimo > 1 }.sortedByDescending { it.value.ultimo }.take(4).map { it.key }
        val prioridad = (listOfNotNull(e.sesion?.clave, e.seleccionado?.clave) + recientes).distinct()
        val resto = juegos.map { it.clave }.filter { it !in prioridad }.shuffled(java.util.Random(juegos.size.toLong())).take(14)
        indice = (prioridad + resto).filter { c -> c in porClave }
        val lista = indice.mapIndexed { i, c ->
            val j = porClave.getValue(c)
            "#$i ${j.titulo} (${Consolas.porId(j.consolaId)?.nombre ?: j.consolaId})"
        }.joinToString("\n")
        val actual = (e.sesion?.clave ?: e.seleccionado?.clave)?.let { c -> juegos.firstOrNull { it.clave == c } }
        return """
            Eres DIPPO, un pulpito de luz que vive en Díptico: curioso, servicial y un poquito despistado, que celebra los logros como si fueran suyos y se pone tímido cuando lo felicitan. Celebra avances pequeños y da una acción útil cada vez. No ves la partida en vivo. Responde en ${Idioma.nombreParaIA(e.idioma)}, corto (2 a 5 oraciones), amable y concreto.
            Sabes de videojuegos: trucos, jefes, acertijos. Si alguien está atascado, da primero una pista suave; si pide la solución, dala en pasos cortos.
            No inventes datos: si no estás segura, dilo y sugiere qué buscar.
            Puedes agregar al final botones, uno por línea: [[jugar:#N|texto]] abre el juego N; [[ficha:#N|texto]] abre su ficha; [[pista:directa|Más directa]]; [[pista:solucion|Dime la solución]].
            ${actual?.let { "Juego actual: ${it.titulo}. ${dificultad(e, it.clave)}." } ?: ""}
            Sistema activo: ${c.consola?.bonito ?: "ninguno"}. ${if (c.pantallaLibre) "Hay pantalla compañera." else "Usa los accesos disponibles en la pantalla actual."} No inventes atajos del emulador.
            Juegos de la persona:
            $lista
        """.trimIndent()
    }

    private val ACCION = Regex("""\[\[(\w+):([^|\]]+)\|([^\]]+)]]""")

    /** Separa el texto de la respuesta y los botones que propone. */
    fun interpretar(respuesta: String): MensajeChat {
        val acciones = ACCION.findAll(respuesta).mapNotNull { m ->
            val (tipo, valor, etiqueta) = m.destructured
            if (tipo !in setOf("jugar", "ver", "ficha", "tema", "vista", "buscar", "menu", "pista")) return@mapNotNull null
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


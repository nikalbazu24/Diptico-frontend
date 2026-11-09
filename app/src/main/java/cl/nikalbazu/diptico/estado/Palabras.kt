package cl.nikalbazu.diptico.estado

import java.text.Normalizer
import java.util.Locale

/**
 * Las palabras que sugiere el teclado de Díptico (como la franja de Gboard): completa la palabra que estás
 * escribiendo, le pone las tildes que faltan y conoce los nombres de tus juegos.
 */
object Palabras {
    /** Palabras comunes del español, de la más usada a la menos, más las de videojuegos y de Díptico. */
    private val COMUNES = """
        de la que el en y a los se del las un por con no una su para es al lo como más pero sus le ya o este sí porque esta
        entre cuando muy sin sobre también me hasta hay donde quien desde todo nos durante todos uno les ni contra otros ese
        eso ante ellos e esto mí antes algunos qué unos yo otro otras otra él tanto esa estos mucho quienes nada muchos cual
        poco ella estar estas algunas algo nosotros mi mis tú te ti tu tus ellas nosotras vosotros vosotras os mío mía míos
        mías tuyo tuya tuyos tuyas suyo suya suyos suyas nuestro nuestra nuestros nuestras vuestro vuestra vuestros vuestras
        esos esas estoy estás está estamos están esté estés estemos estén estaré estarás estará estuve estaba estábamos
        hola gracias favor bien mal bueno buena buenos buenas día días noche noches tarde tardes hoy mañana ayer ahora
        después luego siempre nunca vez veces aquí allá allí así cómo cuándo dónde cuánto cuánta cuántos por qué quién
        hacer tengo tienes tiene tenemos tienen tener puedo puedes puede podemos pueden poder quiero quieres quiere queremos
        sé sabes sabe saber voy vas va vamos van ir hago haces hace hacemos hacen dime dame dar decir ver veo ves mira mirar
        necesito necesitas ayuda ayudar ayúdame explica explícame recomienda recomiéndame recomiendas busca buscar encontrar
        encuentro pasar paso pasé pasa pasos seguir sigo sigue continuar empezar empiezo terminar terminé jugar juego juegos
        jugando jugué partida partidas nivel niveles mundo mundos mapa jefe jefes final enemigo enemigos arma armas espada
        escudo llave llaves puerta templo castillo cueva bosque zona misión misiones objetivo pista pistas truco trucos
        secreto secretos coleccionables logro logros punto puntos vida vidas magia poder poderes objeto objetos tienda
        dinero monedas cofre cofres guardar guardado estado emulador consola consolas pantalla pantallas mando botón botones
        tema temas música sonido carpeta carpetas portada portadas biblioteca favorito favoritos corto largo difícil fácil
        atascado atascada cuesta morir muero muere muerto derrotar vencer ganar perder perdí gané historia personaje
        personajes jugador jugadores dos uno tres cuatro cinco diez veinte minutos hora horas rato primero primera segundo
        segunda último última nuevo nueva viejo clásico clásicos parecido parecidos similar similares mejor mejores peor
        rápido lento grande pequeño fuerte débil alto bajo arriba abajo izquierda derecha norte sur este oeste dentro fuera
        cerca lejos agua fuego tierra aire hielo rayo luz oscuridad sombra cielo mar río montaña isla ciudad pueblo casa
        Nintendo PlayStation Sega Game Boy Advance DS Switch Zelda Mario Pokémon Sonic Kirby Metroid Castlevania Final Fantasy
        quiero algo para con amigos solo sola acción aventura carreras plataformas lucha rol estrategia puzle terror deportes
        Halloween Navidad verano invierno otoño primavera oscuro claro color colores rojo azul verde amarillo morado naranjo
        rosado negro blanco gris cambiar cambia cambiame cámbiame pon ponme abre abrir cierra cerrar mostrar muestra quita
        también todavía aún casi solo sólo cada mismo misma mientras entonces aunque sino según incluso además bastante
    """.trimIndent().split(Regex("\\s+")).filter { it.isNotBlank() }.distinct()

    /** Frases para empezar según lo que se escribe, cuando aún no hay nada. */
    fun inicios(para: String): List<String> = when (para) {
        "asistente" -> listOf("¿Cómo", "Dame", "¿Qué")
        "paso" -> listOf("Buscar", "Derrotar", "Ir")
        "notas" -> listOf("El", "La", "Para")
        else -> emptyList()
    }

    fun normal(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)

    private var clavePropias = -1
    private var propias: List<String> = emptyList()

    /** Las palabras de los títulos de tus juegos (se arman una vez por biblioteca). */
    private fun palabrasDe(titulos: List<String>): List<String> {
        if (titulos.size != clavePropias) {
            propias = titulos.flatMap { it.split(Regex("[^\\p{L}\\p{N}']+")) }.filter { it.length >= 3 }
                .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
            clavePropias = titulos.size
        }
        return propias
    }

    /** La palabra que se está escribiendo justo antes del cursor. */
    fun actual(antes: String): String = antes.takeLastWhile { it.isLetterOrDigit() || it == '\'' }

    /**
     * Hasta [cuantas] sugerencias para lo escrito [antes] del cursor. Al buscar un juego ([titulosEnteros]),
     * también propone títulos completos.
     */
    fun sugerir(antes: String, para: String, titulos: List<String>, titulosEnteros: Boolean, cuantas: Int = 3): List<String> {
        if (titulosEnteros && antes.isNotBlank()) {
            val n = normal(antes.trim())
            val enteros = titulos.asSequence().filter { normal(it).contains(n) && !it.equals(antes.trim(), true) }
                .sortedBy { if (normal(it).startsWith(n)) 0 else 1 }.distinct().take(cuantas).toList()
            if (enteros.isNotEmpty()) return enteros
        }
        val palabra = actual(antes)
        if (palabra.isEmpty()) return if (antes.isBlank() || antes.trimEnd().endsWith(".")) inicios(para) else emptyList()
        val n = normal(palabra)
        val mayuscula = palabra.first().isUpperCase()
        val candidatas = (palabrasDe(titulos) + COMUNES).asSequence()
            .filter { normal(it).startsWith(n) && it != palabra }
            // Primero la misma palabra con sus tildes («cancion» → «canción»), luego las más cortas y usadas.
            .sortedWith(compareBy({ if (normal(it) == n) 0 else 1 }, { if (it.length - palabra.length <= 4) 0 else 1 }))
            .distinctBy { normal(it) }.take(cuantas).toList()
        return candidatas.map { s -> if (mayuscula && s.first().isLowerCase()) s.replaceFirstChar { it.uppercase() } else s }
    }

    /** La otra letra de una tecla (manteniéndola o con Y): tildes, ñ y números de la fila de arriba. */
    fun variante(t: String): String? {
        val base = t.lowercase(Locale.ROOT)
        val v = when (base) {
            "a" -> "á"; "e" -> "é"; "i" -> "í"; "o" -> "ó"; "u" -> "ú"; "n" -> "ñ"; "ñ" -> "n"
            "q" -> "1"; "w" -> "2"; "r" -> "4"; "t" -> "5"; "y" -> "6"; "p" -> "0"
            "," -> ";"; "." -> "…"; "?" -> "¿"; "!" -> "¡"
            else -> null
        } ?: return null
        return if (t != base) v.uppercase(Locale.ROOT) else v
    }

    /** Lo que se muestra chiquito en la esquina de la tecla. */
    fun pista(t: String): String? = when (t.lowercase(Locale.ROOT)) {
        "q" -> "1"; "w" -> "2"; "r" -> "4"; "t" -> "5"; "y" -> "6"; "p" -> "0"
        "a", "e", "i", "o", "u" -> "´"
        else -> null
    }
}

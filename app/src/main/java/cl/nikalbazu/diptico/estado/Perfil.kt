package cl.nikalbazu.diptico.estado

import org.json.JSONObject

/**
 * Tu perfil en Díptico: foto o personaje, color y algunos datos para que la consola sea más tuya.
 * El nombre es el de siempre («Tu nombre», que DIPPO ya usa para saludarte).
 */
data class Perfil(
    /** Cómo te dicen los amigos (aparece como @apodo). */
    val apodo: String = "",
    /** Fecha de nacimiento, «aaaa-mm-dd» (vacía = sin fecha). */
    val nacimiento: String = "",
    /** Tu foto (dirección de una imagen). Si está vacía se usa el personaje. */
    val foto: String = "",
    /** El personaje de Díptico que te representa (ver [PERSONAJES]). */
    val personaje: String = "gato",
    /** El color de tu perfil (ARGB). */
    val color: Long = 0xFFFF7A59,
    /** La consola favorita (id), o vacío para que se note la más jugada. */
    val consolaFav: String = "",
    /** Desde cuándo usas Díptico (milisegundos). */
    val desde: Long = 0L,
    /** Mostrar tus horas jugadas en la tarjeta. */
    val mostrarHoras: Boolean = true,
    /** Usar la foto de tu cuenta de Discord (si está conectada) en vez de tu foto o personaje. */
    val fotoDiscord: Boolean = false,
    /** El marco de la foto (ver [MARCOS]). */
    val marco: String = "estela",
    /** El título bajo tu nombre (ver [TITULOS]). */
    val titulo: String = "",
) {
    fun json(): String = JSONObject()
        .put("apodo", apodo).put("nacimiento", nacimiento).put("foto", foto).put("personaje", personaje)
        .put("color", color).put("consolaFav", consolaFav).put("desde", desde).put("mostrarHoras", mostrarHoras).put("fotoDiscord", fotoDiscord).put("marco", marco).put("titulo", titulo)
        .toString()

    /** La edad cumplida hoy, o null si no hay fecha. */
    fun edad(): Int? {
        val p = nacimiento.split("-").mapNotNull { it.toIntOrNull() }
        if (p.size != 3) return null
        val hoy = java.util.Calendar.getInstance()
        var e = hoy.get(java.util.Calendar.YEAR) - p[0]
        val mes = hoy.get(java.util.Calendar.MONTH) + 1
        if (mes < p[1] || (mes == p[1] && hoy.get(java.util.Calendar.DAY_OF_MONTH) < p[2])) e--
        return e.takeIf { it in 0..120 }
    }

    /** «12 de marzo de 1990», o vacío. */
    fun nacimientoBonito(): String {
        val p = nacimiento.split("-").mapNotNull { it.toIntOrNull() }
        if (p.size != 3) return ""
        return "${p[2]} de ${MESES.getOrElse(p[1] - 1) { "" }} de ${p[0]}"
    }

    /** Hoy es tu cumpleaños. */
    fun cumpleHoy(): Boolean {
        val p = nacimiento.split("-").mapNotNull { it.toIntOrNull() }
        if (p.size != 3) return false
        val hoy = java.util.Calendar.getInstance()
        return hoy.get(java.util.Calendar.MONTH) + 1 == p[1] && hoy.get(java.util.Calendar.DAY_OF_MONTH) == p[2]
    }

    companion object {
        val MESES = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")

        /** Los colores a elegir para el perfil. */
        val COLORES: List<Pair<String, Long>> = listOf(
            "Coral" to 0xFFFF7A59, "Discord" to 0xFF5865F2, "Menta" to 0xFF23C486, "Rosa" to 0xFFE64FA0,
            "Sol" to 0xFFFFC94D, "Cielo" to 0xFF38BDF8, "Uva" to 0xFF9B6BFF, "Lima" to 0xFFA3E635,
        )

        fun leer(texto: String): Perfil {
            if (texto.isBlank()) return Perfil(desde = System.currentTimeMillis())
            return try {
                val o = JSONObject(texto)
                Perfil(
                    apodo = o.optString("apodo"), nacimiento = o.optString("nacimiento"), foto = o.optString("foto"),
                    personaje = o.optString("personaje", "gato").ifBlank { "gato" },
                    color = o.optLong("color", 0xFFFF7A59), consolaFav = o.optString("consolaFav"),
                    desde = o.optLong("desde", 0L).takeIf { it > 0 } ?: System.currentTimeMillis(),
                    mostrarHoras = o.optBoolean("mostrarHoras", true),
                    fotoDiscord = o.optBoolean("fotoDiscord", false),
                    marco = o.optString("marco", "estela").ifBlank { "estela" },
                    titulo = o.optString("titulo"),
                )
            } catch (_: Exception) { Perfil(desde = System.currentTimeMillis()) }
        }

        /**
         * Lo que se escribió como fecha: «12/03/1990», «12-3-1990» o «12 3 1990» → «1990-03-12».
         * Devuelve null si no es una fecha real; una cadena vacía borra la fecha.
         */
        fun fecha(texto: String): String? {
            val t = texto.trim()
            if (t.isEmpty()) return ""
            val p = t.split(Regex("[/\\-. ]+")).mapNotNull { it.toIntOrNull() }
            if (p.size != 3) return null
            val (d, m, a) = p
            if (a !in 1900..java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) || m !in 1..12) return null
            val c = java.util.Calendar.getInstance().apply { clear(); set(a, m - 1, 1) }
            if (d !in 1..c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)) return null
            return "%04d-%02d-%02d".format(a, m, d)
        }
    }
}

/** Los personajes que se pueden elegir como foto de perfil (id y nombre). Todos son originales de Díptico. */
val PERSONAJES: List<Pair<String, String>> = listOf(
    "gato" to "Misu", "robot" to "Robotín", "bicho" to "Moquito", "dragon" to "Dragoncito",
    "fantasma" to "Fantasmita", "caballero" to "Caballerito", "zorro" to "Zorrito", "pixel" to "Pixelito",
)

/** Un marco para la foto. Si tiene [logro], se desbloquea al ganar ese logro de DIPPO. */
data class MarcoPerfil(val id: String, val nombre: String, val logro: String?, val detalle: String)

/** Los marcos de la foto de perfil: los dos primeros son libres, el resto se gana con logros de DIPPO. */
val MARCOS: List<MarcoPerfil> = listOf(
    MarcoPerfil("estela", "Estela", null, "Un cometa de luz que da vueltas sin parar."),
    MarcoPerfil("doble", "Doble estela", null, "Dos cometas que se persiguen."),
    MarcoPerfil("chispas", "Chispas", "cien", "Luciérnagas que orbitan tu foto."),
    MarcoPerfil("corazon", "Latido", "corazon30", "Un aro rosado que late como corazón."),
    MarcoPerfil("llama", "Llama", "racha14", "Fuego que corre alrededor del aro."),
    MarcoPerfil("noche", "Noche estrellada", "finales25", "Un aro de medianoche con estrellitas."),
    MarcoPerfil("aurora", "Aurora", "explorador50", "Colores de aurora que fluyen."),
    MarcoPerfil("arcoiris", "Arcoíris", "consolas8", "Todos los colores girando."),
    MarcoPerfil("oro", "Oro", "quinientas_h", "Un aro dorado con un destello que pasa."),
)

/** Un título que se muestra bajo tu nombre. Se desbloquea con un logro de DIPPO. */
data class TituloPerfil(val id: String, val nombre: String, val logro: String?)

val TITULOS: List<TituloPerfil> = listOf(
    TituloPerfil("", "Sin título", null),
    TituloPerfil("novato", "Recién llegado", "hola"),
    TituloPerfil("maraton", "Maratonista", "trescientas"),
    TituloPerfil("coleccionista", "Coleccionista", "corazon30"),
    TituloPerfil("constante", "Constante", "racha30"),
    TituloPerfil("explorador", "Explorador de consolas", "consolas8"),
    TituloPerfil("finalista", "Cazador de finales", "finales25"),
    TituloPerfil("amigo", "Mejor amigo de DIPPO", "pregunton"),
    TituloPerfil("leyenda", "Leyenda", "quinientas_h"),
)

fun tituloDe(id: String) = TITULOS.firstOrNull { it.id == id }?.takeIf { it.id.isNotEmpty() }?.nombre.orEmpty()

fun marcoDe(id: String) = MARCOS.firstOrNull { it.id == id } ?: MARCOS.first()

fun nombrePersonaje(id: String) = if (id == INICIAL) "Mi inicial" else PERSONAJES.firstOrNull { it.first == id }?.second ?: "Misu"

/** El «personaje» que en vez de un dibujo muestra la primera letra de tu nombre. */
const val INICIAL = "inicial"

/**
 * Editando el perfil (en la pantalla de abajo, con la tarjeta arriba): un borrador que solo se guarda con A · Guardar.
 * [fila]/[col] = lo elegido con el mando (ver [FilasEdicion]).
 */
data class EdicionPerfil(
    val perfil: Perfil,
    val nombre: String,
    val presencia: Boolean,
    val saludo: Boolean,
    val fila: Int = 0,
    val col: Int = 0,
)

/** Las filas del editor, en orden, para moverse con el mando. */
object FilasEdicion {
    const val FOTO = 0
    const val PERSONAJES_1 = 1
    const val PERSONAJES_2 = 2
    const val NOMBRE = 3
    const val APODO = 4
    const val FECHA = 5
    const val CONSOLA = 6
    const val COLOR = 7
    const val MARCO = 8
    const val PRESENCIA = 9
    const val SALUDO = 10
    const val HORAS = 11
    const val BOTONES = 12
    const val TOTAL = 13

    /** Las fuentes de la foto: personaje, Discord (si hay cuenta con foto), galería, inicial. */
    fun fuentes(conDiscord: Boolean): List<String> = listOfNotNull("personaje", if (conDiscord) "discord" else null, "galeria", INICIAL)

    /** Cuántas cosas hay en la fila (para ← →). */
    fun ancho(fila: Int, conDiscord: Boolean): Int = when (fila) {
        FOTO -> fuentes(conDiscord).size
        PERSONAJES_1, PERSONAJES_2 -> 4
        COLOR -> Perfil.COLORES.size
        MARCO -> MARCOS.size
        BOTONES -> 2
        else -> 1
    }

    /** Las filas que se ven (sin Discord no hay «Jugando a…»). */
    fun visible(fila: Int, conKitDiscord: Boolean) = fila != PRESENCIA || conKitDiscord
}

/** Lo que muestra la tarjeta del perfil, ya calculado. */
data class ResumenPerfil(
    val segundos: Long,
    val masJugado: cl.nikalbazu.diptico.data.Juego?,
    val masJugadoSeg: Long,
    val racha: Int,
    val logros: Int,
    val logrosTotal: Int,
    val favoritos: List<cl.nikalbazu.diptico.data.Juego>,
    val juegosJugados: Int,
    val consola: String,
)

package cl.nikalbazu.diptico.estado

/*
 * Díptico según el equipo: al abrir detecta si es una consola de dos pantallas, una portátil de una, un teléfono,
 * una tablet o una tele, y con eso decide qué inicio mostrar y qué opciones tienen sentido.
 */

/** En la tele (detectada, elegida en «Tu equipo» o conectada por HDMI con el modo sala). */
val Estado.enLaTele: Boolean get() = extra.equipoTipo == "tele" || sobremesa

/** El equipo tiene de verdad una segunda pantalla (aunque ahora se use una sola). */
/** Solo una consola de dos pantallas de verdad (la Thor): una tele conectada por HDMI no cuenta como segunda pantalla. */
val Estado.tieneDosPantallas: Boolean get() = extra.equipoTipo == "dual"

val TIPOS_EQUIPO = listOf("auto", "dual", "portatil", "telefono", "tablet", "tele")

fun nombreTipoEquipo(t: String) = when (t) {
    "auto" -> "Detectar solo"
    "dual" -> "Consola de dos pantallas"
    "portatil" -> "Consola portátil (una pantalla)"
    "telefono" -> "Teléfono"
    "tablet" -> "Tablet"
    else -> "Tele"
}

/** Opciones que solo sirven con dos pantallas: en un equipo de una no se muestran. */
private val SOLO_DOS = setOf(
    "Tamaño en la pantalla de abajo", "Modo de pantallas", "Atenuación mientras juegas", "Intercambiar pantallas",
    "Los juegos se abren en", "Pantalla compañera", "Abrir en otra pantalla", "Intercambiar", "Abre en",
)

fun soloDosPantallas(o: Opcion): Boolean = o.titulo in SOLO_DOS || (o.titulo == "Teclado" && o.valor.contains("otra pantalla"))

/** Quita de la lista lo que no sirve en este equipo. */
fun Diptico.filtrarPorEquipo(l: List<Opcion>): List<Opcion> {
    val e = estado.value
    if (e.tieneDosPantallas) return l
    return l.filterNot(::soloDosPantallas)
}

/** Los nombres de las opciones de una sección que se ocultan porque este equipo tiene una sola pantalla. */
fun Diptico.ocultasEnEquipo(s: Seccion): List<String> {
    if (estado.value.tieneDosPantallas) return emptyList()
    val partes = GRUPOS_AJUSTES[s] ?: listOf(s)
    return partes.flatMap { runCatching { armarOpciones(it) }.getOrDefault(emptyList()) }
        .filter(::soloDosPantallas).map { it.titulo }.distinct()
}

/** Nombre corto del equipo para la cabecera de Ajustes: «AYN Odin 2 Portal · una pantalla». */
fun Diptico.etiquetaEquipo(): String {
    val e = estado.value
    val nombre = e.extra.equipoNombre.ifBlank { nombreTipoEquipo(e.extra.equipoTipo) }
    return nombre + " · " + when {
        e.enLaTele -> "en la tele"
        e.tieneDosPantallas -> "dos pantallas"
        else -> "una pantalla"
    }
}

fun Diptico.elegirTipoEquipo(t: String) {
    almacen.guardarTexto("equipoTipo", t)
    val tipo = if (t == "auto") equipo().tipo else t
    actualizar { it.copy(extra = it.extra.copy(equipoTipo = tipo)) }
    optimizarParaEquipo(silencioso = true)
    avisar("Listo: Díptico acomodado como ${nombreTipoEquipo(tipo).lowercase()}")
}

/** DIPPO movido con el dedo: cuánto se corrió de su lugar (en píxeles), por pantalla y lugar. */
fun Diptico.dippoCorrido(lugar: String): Pair<Float, Float> =
    almacen.texto("dippoPos_$lugar").split(",").takeIf { it.size == 2 }?.let { (it[0].toFloatOrNull() ?: 0f) to (it[1].toFloatOrNull() ?: 0f) } ?: (0f to 0f)

fun Diptico.guardarDippoCorrido(lugar: String, x: Float, y: Float) {
    almacen.guardarTexto("dippoPos_$lugar", "$x,$y")
}

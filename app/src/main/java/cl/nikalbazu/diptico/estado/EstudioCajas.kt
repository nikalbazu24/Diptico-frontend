package cl.nikalbazu.diptico.estado

import android.net.Uri
import cl.nikalbazu.diptico.audio.Sonidos
import cl.nikalbazu.diptico.data.Consolas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estudio de Cajas: dentro de Personalizar juego, la caja 3D se arma a gusto. Caras (frente, lomo, reverso, disco)
 * con su procedencia, forma física (plantillas con medidas de cajas reales, sin logos ni arte de marcas), acabado
 * (color y material) y variantes guardadas. Todo vive en el mismo registro de la caja (`CarasGuardadas`), así viaja
 * en el respaldo y queda en el equipo para siempre. Las variantes son entradas aparte con la clave del juego + `#vN`.
 */
object EstudioCajas {
    /** Una forma física: [estuche] es el nombre de `ui.Estuche`; [aspecto] = ancho / alto del frente. */
    data class Plantilla(val id: String, val nombre: String, val estuche: String, val aspecto: Float)

    val PLANTILLAS = listOf(
        Plantilla("ps2", "PS2 DVD", "DVD", 0.71f),
        Plantilla("ps1", "PS1 jewel", "JEWEL", 1.12f),
        Plantilla("bluray", "PS3/4/5 Blu-ray", "BLURAY", 0.8f),
        Plantilla("umd", "PSP UMD", "UMD", 0.6f),
        Plantilla("switch", "Switch delgada", "TARJETA", 0.62f),
        Plantilla("ds", "DS / 3DS", "TARJETA", 0.91f),
        Plantilla("gc", "GameCube", "DVD", 0.72f),
        Plantilla("wii", "Wii / Wii U", "BLURAY", 0.71f),
        Plantilla("carton", "Cartón SNES/N64", "CARTON_FRANJA", 1.38f),
        Plantilla("gba", "Cartón GBA/GBC", "CARTON_PORTATIL", 1.0f),
        Plantilla("pc", "PC clásica", "CAJA_PC", 0.8f),
        Plantilla("android", "Android", "NINGUNO", 1.0f),
    )

    fun plantilla(id: String?): Plantilla? = PLANTILLAS.firstOrNull { it.id == id }

    /** Materiales, con nombre propio de Díptico. */
    val MATERIALES = listOf(
        "brillante" to "Brillo de vitrina",
        "mate" to "Mate suave",
        "carton" to "Cartón de época",
        "translucido" to "Cristal de humo",
    )

    fun nombreMaterial(id: String?) = MATERIALES.firstOrNull { it.first == id }?.second ?: MATERIALES.first().second

    /** Colores del estuche (null = el de la consola). */
    val COLORES: List<Pair<String, Long?>> = listOf(
        "De la consola" to null,
        "Noche" to 0xFF16161CL,
        "Azul profundo" to 0xFF1B2F7AL,
        "Hielo" to 0xFFEDEDF3L,
        "Rojo cereza" to 0xFFC2352BL,
        "Verde menta" to 0xFF23A884L,
        "Ámbar" to 0xFFE89B2EL,
    )

    const val MAX_VARIANTES = 6
    private const val SEP = "#v"

    fun claveVariante(clave: String, n: Int) = "$clave$SEP$n"
    fun esVariante(k: String) = SEP in k

    fun variantes(caras: Map<String, CarasGuardadas>, clave: String): List<Pair<String, CarasGuardadas>> =
        caras.filterKeys { it.startsWith(clave + SEP) }.toList().sortedBy { it.first.substringAfterLast(SEP).toIntOrNull() ?: 0 }

    /** ¿Dos cajas son la misma (sin contar nombre ni fecha)? */
    fun misma(a: CarasGuardadas?, b: CarasGuardadas?): Boolean =
        a?.copy(nombre = null, buscado = 0L) == b?.copy(nombre = null, buscado = 0L)

    /** Las pestañas y cuántas columnas tiene cada una (para moverse con la cruceta). */
    val PESTANAS = listOf("Caras", "Forma", "Acabado", "Variantes")
    fun columnas(tab: Int) = when (tab) { 0 -> 4; 1 -> 4; 2 -> 7; else -> 1 }

    /** Cuántas casillas tiene la pestaña. Caras: 4 caras + 4 acciones. Forma: «De la consola» + plantillas. */
    fun total(tab: Int, nVariantes: Int) = when (tab) {
        0 -> 8
        1 -> PLANTILLAS.size + 1
        2 -> COLORES.size + MATERIALES.size
        else -> nVariantes + 2
    }

    val CARAS = listOf("Frente", "Lomo", "Reverso", "Disco")

    /** De dónde salió cada cara, en palabras simples. */
    fun procedencia(g: CarasGuardadas?, cara: Int): String {
        val valor = when (cara) { 0 -> g?.frente; 1 -> g?.lomo; 2 -> g?.reverso; else -> g?.disco }
        if (valor == null) return when (cara) { 0 -> "La portada"; 3 -> "Disco genérico"; else -> "Lo arma DIPPO" }
        return when (g?.origen) {
            "propia" -> "Tu imagen"
            "comunidad" -> "Comunidad Díptico"
            "internet" -> "ScreenScraper o TheGamesDB"
            else -> "Guardada en el equipo"
        }
    }

    /** Lo que dice DIPPO arriba del Estudio: le da voz a la pantalla y explica qué hacer. */
    fun comentario(g: CarasGuardadas?, tab: Int, nVariantes: Int): String = when (tab) {
        0 -> when {
            g?.frente != null && g.reverso != null && g.propia -> "La armaste tú y nadie la cambia sin preguntarte. ¡Quedó de colección!"
            g?.frente != null && g.reverso != null -> "Frente y reverso de verdad. Esta caja ya es de colección."
            g?.tiene == true -> "Tengo parte de la caja real; lo que falta lo armo yo con la ficha del juego."
            else -> "Por ahora uso la portada. Busca la caja o pásame una foto y la armo."
        }
        1 -> "Medidas de cajas reales, sin logos de nadie. Elige la que más te recuerde a tu estante."
        2 -> "«Brillo de vitrina» refleja la luz cuando giras la caja. «Cartón de época» huele a tienda de los 90."
        else -> if (nVariantes == 0) "Guarda versiones: la de tu país, la de coleccionista o la tuya. Cambias entre ellas cuando quieras."
        else "Tienes $nVariantes ${if (nVariantes == 1) "variante guardada" else "variantes guardadas"}. Al cambiar, guardo la de ahora para que no se pierda."
    }
}

private fun Diptico.cajaActual(): Pair<String, CarasGuardadas?>? {
    val clave = estado.value.extra.cajaJuego.takeIf { it.isNotEmpty() } ?: return null
    return clave to estado.value.extra.caras[clave]
}

/** Cambia algo del estilo de la caja (forma, color o material) y lo deja guardado. */
private fun Diptico.cambiarEstilo(f: (CarasGuardadas) -> CarasGuardadas) {
    val (clave, g) = cajaActual() ?: return
    val base = g ?: CarasGuardadas(null, null, 0L)
    val nuevo = f(base)
    guardarCaras(clave, if (!nuevo.tiene && !nuevo.conEstilo) null else nuevo)
}

fun Diptico.pestanaEstudio(d: Int) {
    val n = EstudioCajas.PESTANAS.size
    actualizar { it.copy(extra = it.extra.copy(estudioTab = (it.extra.estudioTab + d).mod(n), cajaSel = 0)) }
    Sonidos.pagina()
}

fun Diptico.elegirPestanaCajas(t: Int) {
    actualizar { it.copy(extra = it.extra.copy(estudioTab = t.coerceIn(0, EstudioCajas.PESTANAS.size - 1), cajaSel = 0)) }
}

/** Cruceta dentro de la pestaña: [dx] de a uno, [dy] de a una fila. */
fun Diptico.moverEstudio(dx: Int, dy: Int) {
    val e = estado.value
    val clave = e.extra.cajaJuego
    val tab = e.extra.estudioTab
    val total = EstudioCajas.total(tab, EstudioCajas.variantes(e.extra.caras, clave).size)
    val sel = (e.extra.cajaSel + dx + dy * EstudioCajas.columnas(tab)).coerceIn(0, total - 1)
    if (sel != e.extra.cajaSel) Sonidos.mover() else Sonidos.borde()
    actualizar { it.copy(extra = it.extra.copy(cajaSel = sel)) }
}

/** Cómo se ve la caja arriba: 0 girando, 1 frente, 2 lomo, 3 reverso. */
fun Diptico.vistaEstudio(v: Int) {
    actualizar { it.copy(extra = it.extra.copy(estudioVista = v.coerceIn(0, 3), estudioAbierta = false)) }
}

fun Diptico.siguienteVistaEstudio() = vistaEstudio((estado.value.extra.estudioVista + 1) % 4)

fun Diptico.alternarAperturaEstudio() {
    actualizar { it.copy(extra = it.extra.copy(estudioAbierta = !it.extra.estudioAbierta)) }
    Sonidos.abrir()
}

/** A en el Estudio (o el dedo sobre una casilla): hace lo de la casilla [sel] de la pestaña abierta. */
fun Diptico.usarEstudio(sel: Int = estado.value.extra.cajaSel) {
    val e = estado.value
    val clave = e.extra.cajaJuego.takeIf { it.isNotEmpty() } ?: return
    actualizar { it.copy(extra = it.extra.copy(cajaSel = sel)) }
    when (e.extra.estudioTab) {
        0 -> when (sel) {
            in 0..3 -> {
                // Elegir una cara gira la caja para mostrarla (el disco abre la tapa).
                val vista = when (sel) { 0 -> 1; 1 -> 2; 2 -> 3; else -> 1 }
                actualizar { it.copy(extra = it.extra.copy(estudioCara = sel, estudioVista = vista, estudioAbierta = sel == 3)) }
                Sonidos.aceptar()
            }
            4 -> buscarCajaPantalla(clave, explicito = true)
            5 -> { avisar("Elige la imagen para el ${EstudioCajas.CARAS[e.extra.estudioCara].lowercase()}"); pedirImagen("cara:${e.extra.estudioCara}:$clave") }
            6 -> restaurarCara(clave, e.extra.estudioCara)
            else -> armarCajaConImagen(clave)
        }
        1 -> {
            val id = if (sel == 0) null else EstudioCajas.PLANTILLAS.getOrNull(sel - 1)?.id
            cambiarEstilo { it.copy(plantilla = id) }
            Sonidos.aceptar()
        }
        2 -> {
            val nc = EstudioCajas.COLORES.size
            if (sel < nc) cambiarEstilo { it.copy(color = EstudioCajas.COLORES[sel].second) }
            else EstudioCajas.MATERIALES.getOrNull(sel - nc)?.let { (id, _) -> cambiarEstilo { it.copy(material = if (id == "brillante") null else id) } }
            Sonidos.aceptar()
        }
        else -> {
            val lista = EstudioCajas.variantes(e.extra.caras, clave)
            when {
                sel < lista.size -> usarVariante(clave, lista[sel].first)
                sel == lista.size -> guardarVariante(clave)
                else -> quitarCajaPropia(clave)
            }
        }
    }
}

/** Vuelve una cara a la de Díptico (portada o la que arma DIPPO). La imagen solo se borra si nadie más la usa. */
fun Diptico.restaurarCara(clave: String, cara: Int) {
    val g = estado.value.extra.caras[clave] ?: run { avisar("Esa cara ya es la de Díptico"); return }
    val nuevo = when (cara) { 0 -> g.copy(frente = null); 1 -> g.copy(lomo = null); 2 -> g.copy(reverso = null); else -> g.copy(disco = null) }
    guardarCaras(clave, if (!nuevo.tiene && !nuevo.conEstilo) null else nuevo)
    scope.launch(Dispatchers.IO) { CajasLocales.borrarSinUso(app, g, estado.value.extra.caras.values) }
    avisar("Listo: el ${EstudioCajas.CARAS[cara].lowercase()} volvió al de Díptico")
}

/** Una cara desde una imagen del equipo: se guarda reducida en los archivos de Díptico y la caja pasa a ser tuya. */
internal fun Diptico.caraDesdeImagen(clave: String, cara: Int, uri: Uri) {
    val j = estado.value.juegosPorClave[clave] ?: return
    val llave = CajasComunidad.clave(j.titulo, j.consolaId)
    scope.launch {
        val f = withContext(Dispatchers.IO) {
            val dir = CajasComunidad.carpeta(app, "$llave-p${System.currentTimeMillis() % 1_000_000}")
            runCatching { CajasComunidad.guardarCara(app, uri, java.io.File(dir, "${EstudioCajas.CARAS[cara].lowercase()}.jpg")) }.getOrNull()
        }
        if (f == null) { avisar("No pude leer esa imagen. Prueba con otra"); return@launch }
        val antes = estado.value.extra.caras[clave]
        val u = Uri.fromFile(f).toString()
        val base = antes ?: CarasGuardadas(null, null, System.currentTimeMillis())
        val nuevo = when (cara) { 0 -> base.copy(frente = u); 1 -> base.copy(lomo = u); 2 -> base.copy(reverso = u); else -> base.copy(disco = u) }
            .copy(origen = "propia", buscado = System.currentTimeMillis())
        guardarCaras(clave, nuevo)
        withContext(Dispatchers.IO) { CajasLocales.borrarSinUso(app, antes, estado.value.extra.caras.values) }
        sumarDippo("caja")
        avisar("¡Listo! El ${EstudioCajas.CARAS[cara].lowercase()} quedó con tu imagen")
    }
}

/** Guarda la caja de ahora como variante (máximo 6: la más vieja deja su lugar). */
fun Diptico.guardarVariante(clave: String) {
    val e = estado.value
    val g = e.extra.caras[clave]
    if (g == null || (!g.tiene && !g.conEstilo)) { avisar("Primero dale algo a la caja: una imagen, una forma o un color"); return }
    val lista = EstudioCajas.variantes(e.extra.caras, clave)
    if (lista.any { EstudioCajas.misma(it.second, g) }) { avisar("Esa caja ya está guardada como variante"); return }
    val usados = lista.mapNotNull { it.first.substringAfterLast("#v").toIntOrNull() }.toSet()
    val n = (1..EstudioCajas.MAX_VARIANTES).firstOrNull { it !in usados }
    val k = if (n != null) EstudioCajas.claveVariante(clave, n) else lista.minByOrNull { it.second.buscado }!!.first
    val nombre = when (g.origen) {
        "propia" -> "Mi caja"; "comunidad" -> "De la comunidad"; "internet" -> "Original"; else -> "Variante"
    } + " " + (n ?: k.substringAfterLast("#v"))
    val viejo = e.extra.caras[k]
    guardarCaras(k, g.copy(nombre = nombre, buscado = System.currentTimeMillis()))
    if (viejo != null) scope.launch(Dispatchers.IO) { CajasLocales.borrarSinUso(app, viejo, estado.value.extra.caras.values) }
    avisar("Guardé «$nombre». Puedes volver a ella cuando quieras")
    Sonidos.aceptar()
}

/** Usa una variante. Si la caja de ahora no estaba guardada, antes la guarda (así nunca se pierde). */
fun Diptico.usarVariante(clave: String, k: String) {
    val e = estado.value
    val v = e.extra.caras[k] ?: return
    val actual = e.extra.caras[clave]
    val lista = EstudioCajas.variantes(e.extra.caras, clave)
    if (actual != null && (actual.tiene || actual.conEstilo) && lista.none { EstudioCajas.misma(it.second, actual) }) guardarVariante(clave)
    guardarCaras(clave, v.copy(nombre = null))
    avisar("Ahora usas «${v.nombre ?: "la variante"}»")
    Sonidos.aceptar()
}


/** Quita una variante guardada (la caja en uso no cambia). Sus imágenes se borran solo si nadie más las usa. */
fun Diptico.quitarVariante(k: String) {
    val v = estado.value.extra.caras[k] ?: return
    guardarCaras(k, null)
    scope.launch(Dispatchers.IO) { CajasLocales.borrarSinUso(app, v, estado.value.extra.caras.values) }
    val clave = estado.value.extra.cajaJuego
    val n = EstudioCajas.variantes(estado.value.extra.caras, clave).size
    actualizar { it.copy(extra = it.extra.copy(cajaSel = it.extra.cajaSel.coerceAtMost(n + 1))) }
    avisar("Quité «${v.nombre ?: "la variante"}»")
}

/** X en el Estudio: en Variantes, sobre una variante, la quita; en lo demás abre o cierra la caja. */
fun Diptico.xEstudio() {
    val e = estado.value
    val lista = EstudioCajas.variantes(e.extra.caras, e.extra.cajaJuego)
    if (e.extra.estudioTab == 3 && e.extra.cajaSel < lista.size) quitarVariante(lista[e.extra.cajaSel].first)
    else alternarAperturaEstudio()
}

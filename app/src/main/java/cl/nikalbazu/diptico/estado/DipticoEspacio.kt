package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.audio.Sonidos

/**
 * «Tu espacio» (Widgets 3.0, boceto 10a–10d): la página de widgets del diseño Colección viva.
 * Empieza vacía; DIPPO propone tres widgets según cómo usas la consola; L2 pasa del inicio a Tu espacio y de vuelta;
 * cada página puede medir hasta 3 pantallas de ancho y 3 de alto (se desliza para ver el resto).
 */

/** No hay ningún widget puesto en ninguna página. */
fun Estado.espacioVacio(): Boolean = widgets.none { it.visible }

/** Una vez: con el diseño nuevo Tu espacio parte vacío (los widgets quedan guardados, se agregan desde la tienda). */
internal fun Diptico.vaciarEspacioUnaVez() {
    if (almacen.bool("espacioVacio1", false)) return
    almacen.guardarBool("espacioVacio1", true)
    if (!estado.value.extra.coleccionViva) return
    val l = estado.value.widgets.map { it.copy(visible = false) }
    almacen.guardarTexto("widgets", escribirWidgets(l))
    actualizar { it.copy(widgets = l, paginaInicio = 0) }
}

/** Los tres que DIPPO propone, con el porqué (según cómo se usa la consola). */
fun Diptico.sugeridosEspacio(e: Estado = estado.value): List<Pair<String, String>> {
    val semana = runCatching { TuAno.ultimosDias(7).sumOf { it.second } }.getOrDefault(0L)
    val horas = semana / 3600
    val jugoAlgo = e.datos.values.any { it.tiempo > 0L }
    val hora = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return listOf(
        if (jugoAlgo) "proximaPartida" to "Para volver a tu juego en un toque" else "dippoPropone" to "Para elegir qué jugar",
        "tuSemana" to if (horas > 0) "Esta semana llevas $horas h" else "Para ver cuánto juegas",
        if (hora < 13) "clima" to "Sueles abrir Díptico de día" else "recientes" to "Tus últimos juegos a mano",
    )
}

/** «Agregar los 3»: pone los sugeridos en Tu espacio y lo guarda. */
fun Diptico.agregarSugeridos() {
    sugeridosEspacio().forEach { (tipo, _) ->
        val i = estado.value.widgets.indexOfFirst { it.tipo == tipo }
        if (i >= 0 && !estado.value.widgets[i].visible) alternarWidget(i)
    }
    guardarWidgets(estado.value.widgets)
    Sonidos.aceptar()
    avisar("Listo: ya tienes tres widgets")
}


/** Una página más en Tu espacio (también sin entrar al editor). */
fun Diptico.agregarPaginaEspacio() {
    val e = estado.value
    if (e.paginas >= 12) { avisar("Ya tienes 12 páginas"); return }
    val paginas = e.paginas + 1
    almacen.guardarTexto("paginas", paginas.toString())
    actualizar { it.copy(paginas = paginas, paginaInicio = paginas - 1, widgetSel = -1, burbuja = null, celdaCatalogo = null) }
    avisar("Página ${paginas} lista")
}

/** Quita la página en la que estás si no tiene widgets. */
fun Diptico.quitarPaginaEspacio() {
    val e = estado.value
    if (e.paginas <= 1) return
    val pag = e.paginaInicio
    if (e.widgets.any { it.visible && it.pagina == pag }) { avisar("Primero saca los widgets de esta página"); return }
    val l = e.widgets.map { if (it.pagina > pag) it.copy(pagina = it.pagina - 1) else it }
    val paginas = e.paginas - 1
    almacen.guardarTexto("paginas", paginas.toString())
    almacen.guardarTexto("widgets", escribirWidgets(l))
    actualizar { it.copy(paginas = paginas, widgets = l, paginaInicio = pag.coerceAtMost(paginas - 1)) }
}

/** Agranda o achica la página: cuántas pantallas de ancho y de alto mide (1 a 3). */
fun Diptico.cambiarTamPagina(dAncho: Int, dAlto: Int) {
    val e = estado.value
    val an = (e.extra.paginaAncho + dAncho).coerceIn(1, 3)
    val al = (e.extra.paginaAlto + dAlto).coerceIn(1, 3)
    if (an == e.extra.paginaAncho && al == e.extra.paginaAlto) return
    almacen.guardarTexto("paginaAncho", an.toString())
    almacen.guardarTexto("paginaAlto", al.toString())
    Widgets.COLUMNAS = e.gridCols * an
    Widgets.FILAS = e.gridFilas * al
    // Al achicar, lo que quedó afuera se acomoda adentro sin montarse.
    val l = reacomodar(e.widgets.map { encajar(it) }, e.paginas)
    almacen.guardarTexto("widgets", escribirWidgets(l))
    actualizar { it.copy(widgets = l, extra = it.extra.copy(paginaAncho = an, paginaAlto = al)) }
}

fun nombreTamPagina(an: Int, al: Int): String = when {
    an == 1 && al == 1 -> "Una pantalla"
    al == 1 -> "$an pantallas de ancho"
    an == 1 -> "$al pantallas de alto"
    else -> "$an × $al pantallas"
}

/**
 * L2 y R2 recorren una sola tira: Inicio · página 1 · página 2 · …  (L2 hacia la izquierda, R2 hacia la derecha).
 * Desde la última página R2 vuelve al inicio; con una sola página, los dos gatillos alternan inicio y Tu espacio.
 */
fun Diptico.pasoEspacio(d: Int) {
    val e = estado.value
    if (e.inicio != "widgets") {
        almacen.guardarTexto("inicio", "widgets")
        actualizar { it.copy(inicio = "widgets", widgetFoco = -1, paginaInicio = if (d > 0) 0 else (it.paginas - 1).coerceAtLeast(0)) }
    } else {
        val p = e.paginaInicio + d
        if (p < 0 || p >= e.paginas) {
            almacen.guardarTexto("inicio", "heroe")
            actualizar { it.copy(inicio = "heroe", widgetFoco = -1, extra = it.extra.copy(tiendaLado = false, galeria = false)) }
        }
        else irAPagina(p)
    }
    Sonidos.mover()
}

/** Cuántos widgets hay en toda la Galería. */
fun totalGaleria(): Int = Widgets.porDefecto.map { it.tipo }.filter { it != "imagen" && it != "video" }.distinct().size

/** Los widgets que muestra la tienda de Tu espacio en una categoría (sin los que necesitan un archivo). */
fun tiposTiendaEspacio(cat: Int): List<String> {
    val nombre = CATEGORIAS_WIDGETS.getOrNull(cat) ?: CATEGORIAS_WIDGETS.first()
    val todos = Widgets.porDefecto.map { it.tipo }.filter { it != "imagen" && it != "video" }.distinct()
    return if (nombre == "Para ti") PARA_TI.filter { it in todos } else todos.filter { categoriaWidget(it) == nombre }
}

fun Diptico.categoriaEspacio(d: Int) {
    val n = CATEGORIAS_WIDGETS.size
    val c = (estado.value.extra.tiendaCat + d).mod(n)
    actualizar { it.copy(extra = it.extra.copy(tiendaCat = c, espacioSel = 0)) }
    Sonidos.mover()
}

fun Diptico.elegirCategoriaEspacio(c: Int) = actualizar { it.copy(extra = it.extra.copy(tiendaCat = c, espacioSel = 0)) }

/** Cruceta en la tienda de Tu espacio (grilla de [columnas]). */
fun Diptico.moverTiendaEspacio(dx: Int, dy: Int, columnas: Int = 2) {
    val e = estado.value
    val total = tiposTiendaEspacio(e.extra.tiendaCat).size
    if (total == 0) return
    val n = (e.extra.espacioSel + dx + dy * columnas).coerceIn(0, total - 1)
    if (n != e.extra.espacioSel) { actualizar { it.copy(extra = it.extra.copy(espacioSel = n)) }; Sonidos.mover() }
}

/** A en la tienda: si el widget ya está, salta a su página; si no, lo pone en el primer lugar libre. */
fun Diptico.ponerEnEspacio(tipo: String) {
    val e = estado.value
    val i = e.widgets.indexOfFirst { it.tipo == tipo }
    if (i < 0) return
    val w = e.widgets[i]
    if (w.visible) { irAPagina(w.pagina); avisar("${Widgets.nombre(tipo)} ya está en Tu espacio"); return }
    alternarWidget(i)
    guardarWidgets(estado.value.widgets)
    val ahora = estado.value.widgets.getOrNull(i)
    if (ahora?.visible == true) {
        irAPagina(ahora.pagina)
        Sonidos.aceptar()
        avisar("${Widgets.nombre(tipo)} quedó en Tu espacio")
    }
}



/** Personalizar consola › «Ver sus juegos». */
fun Diptico.verJuegosConsola(id: String) {
    cerrarPanel()
    actualizar { it.copy(consolaSel = id) }
    entrarSistema()
}

/** Personalizar consola › «Uno al azar»: abre un juego cualquiera de esa consola. */
fun Diptico.azarDeConsola(id: String) {
    val j = estado.value.juegosPorConsola[id].orEmpty().randomOrNull() ?: run { avisar("Esta consola no tiene juegos"); return }
    jugarDesdeWidget(j.clave)
}

/** L1/R1 en Personalizar consola: salta a la primera opción de la sección siguiente o anterior. */
fun Diptico.seccionEditarConsola(d: Int) {
    val e = estado.value
    val l = opcionesEditarConsola(e)
    val grupos = l.map { it.grupo ?: "" }.distinct()
    val actual = grupos.indexOf(l.getOrNull(e.indice)?.grupo ?: "").coerceAtLeast(0)
    val g = grupos.getOrNull((actual + d).mod(grupos.size)) ?: return
    val i = l.indexOfFirst { (it.grupo ?: "") == g }.coerceAtLeast(0)
    actualizar { it.copy(indice = i) }
    Sonidos.mover()
}

/**
 * Mantener presionado un widget: entra al modo edición con ESE widget elegido y abre su menú de edición
 * en la otra pantalla (con una pantalla, en el panel del editor). «Listo» lo cierra todo.
 */
fun Diptico.editarEsteWidget(tipo: String?) {
    val e = estado.value
    val i = e.widgets.indexOfFirst { it.tipo == tipo && it.visible }
    if (i < 0) { editarWidgets(); return }
    if (e.widgets[i].pagina != e.paginaInicio) actualizar { it.copy(paginaInicio = e.widgets[i].pagina) }
    abrirAjustesWidget(i)
    cl.nikalbazu.diptico.audio.Vibracion.elegir()
}

/** L1/R1 en el menú de un widget: la pestaña anterior o siguiente. */
fun Diptico.pasoPestanaWidget(d: Int) {
    val e = estado.value
    val n = controlesEstudio(e).getOrNull(1)?.elecciones?.size ?: return
    if (n <= 0) return
    elegirGrupoEditorWidget((e.navegacion.grupoEditorWidget + d).mod(n))
    Sonidos.mover()
}

// ———————————————— Galería de widgets (su propia pantalla, boceto 12a–12c) ————————————————

fun Diptico.abrirGaleriaWidgets() {
    // La Galería tiene prioridad sobre el widget que estaba enfocado.
    actualizar { it.copy(widgetFoco = -1,
        extra = it.extra.copy(galeria = true, tiendaLado = it.unaPantalla, espacioSel = 0, galeriaTam = 0)) }
    Sonidos.aceptar()
}

fun Diptico.cerrarGaleriaWidgets() {
    actualizar { it.copy(extra = it.extra.copy(galeria = false, tiendaLado = false)) }
    Sonidos.atras()
}

/** Los tamaños que se ofrecen para un widget: como viene y los de siempre, sin repetir y que quepan en la página. */
fun tamanosGaleria(e: Estado, tipo: String): List<Pair<Int, Int>> {
    val w = e.widgets.firstOrNull { it.tipo == tipo }
    val base = listOfNotNull(w?.let { it.an to it.al }) + listOf(1 to 1, 2 to 1, 2 to 2, 3 to 2)
    return base.distinct().filter { (an, al) -> an <= Widgets.COLUMNAS && al <= Widgets.FILAS }
}

fun Diptico.cambiarTamGaleria(d: Int) {
    val e = estado.value
    val tipo = tiposTiendaEspacio(e.extra.tiendaCat).getOrNull(e.extra.espacioSel) ?: return
    val n = tamanosGaleria(e, tipo).size.coerceAtLeast(1)
    actualizar { it.copy(extra = it.extra.copy(galeriaTam = (it.extra.galeriaTam + d).mod(n))) }
    Sonidos.mover()
}

fun Diptico.elegirTamGaleria(i: Int) = actualizar { it.copy(extra = it.extra.copy(galeriaTam = i)) }

/** A en la Galería: agrega el widget elegido con el tamaño elegido (si ya está, salta a su página). */
fun Diptico.agregarDesdeGaleria() {
    val e = estado.value
    val tipo = tiposTiendaEspacio(e.extra.tiendaCat).getOrNull(e.extra.espacioSel) ?: return
    val i = e.widgets.indexOfFirst { it.tipo == tipo }
    if (i < 0) return
    val w = e.widgets[i]
    if (!w.visible) {
        val (an, al) = tamanosGaleria(e, tipo).getOrNull(e.extra.galeriaTam) ?: (w.an to w.al)
        actualizar { s -> s.copy(widgets = s.widgets.toMutableList().also { it[i] = w.copy(an = an, al = al) }) }
    }
    ponerEnEspacio(tipo)
}


// ── Tu espacio abajo · modo edición (boceto S221ptEpUYaNtWFkc4kbQ9): cambia la parte del medio, no abre nada encima. ──

/** Las pestañas del modo edición. */
val PESTANAS_ESPACIO = listOf("Mover", "Tamaño", "Páginas", "Fondo")

/** Fondos listos para Tu espacio (degradados oscuros del boceto; el último es negro). */
val FONDOS_ESPACIO: List<List<Long>> = listOf(
    listOf(0xFF1B1F4A, 0xFF0B0C16), listOf(0xFF3B1D5A, 0xFF0B0C16), listOf(0xFF0F3B3A, 0xFF0B0C16),
    listOf(0xFF5A3A14, 0xFF0B0C16), listOf(0xFF40101C, 0xFF0B0C16), listOf(0xFF000000, 0xFF000000),
)

/** Entra al modo edición de Tu espacio. Si estabas mirando el inicio, va a la primera página de widgets. */
fun Diptico.editarEspacio(pestana: Int = 0) {
    almacen.guardarTexto("inicio", "widgets")
    actualizar { it.copy(inicio = "widgets", editandoWidgets = true, panel = Panel.NINGUNO, estudioMover = false, widgetFoco = -1,
        burbuja = null, celdaCatalogo = null, extra = it.extra.copy(espacioEdita = pestana.coerceIn(0, PESTANAS_ESPACIO.size - 1), galeria = false)) }
    actualizar { it.copy(widgetSel = widgetsDePagina(it).firstOrNull() ?: -1) }
    Sonidos.aceptar()
}

/** B: guarda y vuelve a Tu espacio. */
fun Diptico.terminarEspacio() {
    almacen.guardarTexto("widgets", escribirWidgets(estado.value.widgets))
    actualizar { it.copy(editandoWidgets = false, estudioMover = false, burbuja = null, celdaCatalogo = null, extra = it.extra.copy(espacioEdita = -1)) }
    Sonidos.atras()
}

fun Diptico.elegirPestanaEspacio(i: Int) {
    actualizar { it.copy(extra = it.extra.copy(espacioEdita = i.mod(PESTANAS_ESPACIO.size))) }
    Sonidos.mover()
}

fun Diptico.pestanaEspacio(d: Int) = elegirPestanaEspacio(estado.value.extra.espacioEdita + d)

/** L1/R1 en «Mover»: otro widget de la misma página. */
fun Diptico.otroWidgetEspacio(d: Int) {
    val l = widgetsDePagina()
    if (l.isEmpty()) return
    val pos = l.indexOf(estado.value.widgetSel)
    actualizar { it.copy(widgetSel = l[(pos + d).mod(l.size)]) }
    Sonidos.mover()
}

/** Agranda (d > 0) o achica el widget elegido: primero a lo ancho, después a lo alto, para que crezca parejo. */
fun Diptico.crecerWidgetEspacio(d: Int) {
    val e = estado.value
    val w = e.widgets.getOrNull(e.widgetSel)?.takeIf { it.visible } ?: return
    val (an, al) = if (d > 0) { if (w.an <= w.al) w.an + 1 to w.al else w.an to w.al + 1 }
        else { if (w.an > w.al) w.an - 1 to w.al else w.an to (w.al - 1) }
    if (an < 1 || al < 1) return
    redimensionarWidget(e.widgetSel, an, al)
    Sonidos.mover()
}

/** Corre la página que estás mirando un lugar antes (d < 0) o después, con sus widgets. El inicio siempre va primero. */
fun Diptico.moverPaginaEspacio(d: Int) {
    val e = estado.value
    val p = e.paginaInicio
    val q = p + d
    if (q < 0 || q >= e.paginas) return
    val l = e.widgets.map { when (it.pagina) { p -> it.copy(pagina = q); q -> it.copy(pagina = p); else -> it } }
    almacen.guardarTexto("widgets", escribirWidgets(l))
    actualizar { it.copy(widgets = l, paginaInicio = q) }
    Sonidos.mover()
}

/** Elige el fondo de Tu espacio: −1 = como viene; si eliges uno listo, se quita tu imagen. */
fun Diptico.elegirFondoEspacio(i: Int) {
    val v = i.coerceIn(-1, FONDOS_ESPACIO.size - 1)
    almacen.guardarTexto("fondoEspacio", v.toString())
    if (v >= 0 && estado.value.fondoInicio != null) {
        almacen.guardarTexto("fondoInicio", "")
        actualizar { it.copy(fondoInicio = null) }
    }
    actualizar { it.copy(extra = it.extra.copy(fondoEspacio = v)) }
    Sonidos.mover()
}

fun Diptico.quitarImagenEspacio() {
    almacen.guardarTexto("fondoInicio", "")
    actualizar { it.copy(fondoInicio = null) }
}

/** Cruceta en el modo edición, según la pestaña. */
fun Diptico.crucetaEspacio(dx: Int, dy: Int) {
    val e = estado.value
    when (e.extra.espacioEdita) {
        0 -> moverWidgetSel(dx, dy)
        1 -> cambiarTamPagina(dx, dy)
        2 -> if (dx != 0) moverPaginaEspacio(dx)
        3 -> if (dx != 0 || dy != 0) elegirFondoEspacio((e.extra.fondoEspacio + dx + dy * 3).coerceIn(-1, FONDOS_ESPACIO.size - 1))
    }
}

/** A en el modo edición: agranda el widget, agrega una página o pide tu imagen de fondo. */
fun Diptico.aEspacio() = when (estado.value.extra.espacioEdita) {
    0 -> crecerWidgetEspacio(1)
    2 -> agregarPaginaEspacio()
    3 -> pedirArchivoPara("fondoInicio", "image/*")
    else -> Unit
}

/** X en el modo edición: achica el widget o quita la página (si está vacía). */
fun Diptico.xEspacio() = when (estado.value.extra.espacioEdita) {
    0 -> crecerWidgetEspacio(-1)
    2 -> quitarPaginaEspacio()
    else -> Unit
}

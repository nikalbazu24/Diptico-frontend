package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.estado.Diptico.Control
import cl.nikalbazu.diptico.estado.Diptico.InfoBateria
import cl.nikalbazu.diptico.estado.Diptico.ItemGaleria
import cl.nikalbazu.diptico.estado.Diptico.LanzadorPC
import cl.nikalbazu.diptico.estado.Diptico.OpcionBurbuja
import cl.nikalbazu.diptico.estado.Diptico.PorBorrar
import cl.nikalbazu.diptico.estado.Diptico.Regreso
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.os.SystemClock
import android.view.Display
import cl.nikalbazu.diptico.audio.Aviso
import cl.nikalbazu.diptico.audio.Avisos
import cl.nikalbazu.diptico.audio.Medios
import cl.nikalbazu.diptico.audio.Musica
import cl.nikalbazu.diptico.audio.Sonidos
import cl.nikalbazu.diptico.audio.Vibracion
import cl.nikalbazu.diptico.data.Almacen
import cl.nikalbazu.diptico.data.Biblioteca
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.data.Descargas
import cl.nikalbazu.diptico.data.FichaInfo
import cl.nikalbazu.diptico.data.Fichas
import cl.nikalbazu.diptico.data.CarpetaJuegos
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.data.MediosJuego
import cl.nikalbazu.diptico.emu.App
import cl.nikalbazu.diptico.emu.Lanzador
import cl.nikalbazu.diptico.emu.Pantallas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Burbuja para agregar widgets: funciones de Diptico movidas aquí desde Diptico.kt para que ese archivo no siga creciendo.
 * Se llaman igual que antes (Diptico.x()); fuera del paquete estado hay que importar cl.nikalbazu.diptico.estado.*.
 */

/** Lo que se puede agregar: los widgets que no están a la vista, más imagen/GIF y video. */
fun Diptico.opcionesBurbuja(e: Estado = estado.value): List<OpcionBurbuja> =
    e.widgets.withIndex().filter { !it.value.visible && !it.value.tipo.startsWith("imagen") && !it.value.tipo.startsWith("video") }
        .map { (k, w) -> OpcionBurbuja(Widgets.nombre(w.tipo), Widgets.icono(w.tipo), k) } +
        OpcionBurbuja("Imagen o GIF", Icono.PORTADA, -1, "imagen") + OpcionBurbuja("Video corto", Icono.JUGAR, -1, "video") +
        OpcionBurbuja("Acceso a un juego", Icono.CONTINUAR, -1, "atajo") + OpcionBurbuja("Colección nueva", Icono.CARPETA, -1, "coleccion")


/** Abre la burbuja en la celda (x, y): con el dedo (manteniendo) o con el control. */
fun Diptico.abrirBurbuja(x: Int, y: Int) {
    val e = estado.value
    if (!e.editandoWidgets) return
    val celda = x.coerceIn(0, Widgets.COLUMNAS - 1) to y.coerceIn(0, Widgets.FILAS - 1)
    if (!e.unaPantalla) abrirCatalogoWidgets(celda)
    else actualizar { it.copy(burbuja = celda, burbujaSel = 0, estudioMover = false) }
    Sonidos.abrir()
}


/** La burbuja junto al widget elegido (con el control). */
fun Diptico.abrirBurbujaAqui() {
    val e = estado.value
    val w = e.widgets.getOrNull(e.widgetSel)?.takeIf { it.visible && it.pagina == e.paginaInicio }
    abrirBurbuja(w?.x ?: 0, w?.y ?: 0)
}


fun Diptico.cerrarBurbuja() {
    if (estado.value.burbuja == null) return
    actualizar { it.copy(burbuja = null) }
    Sonidos.atras()
}


fun Diptico.moverBurbuja(d: Int) {
    val n = opcionesBurbuja().size
    if (n == 0) return
    actualizar { it.copy(burbujaSel = (it.burbujaSel + d).mod(n)) }
}


/** Agrega lo elegido de la burbuja en su celda; si ahí hay otro widget, se corre para dejarle espacio. */
fun Diptico.elegirBurbuja(k: Int = estado.value.burbujaSel) {
    val e = estado.value
    val (cx, cy) = e.burbuja ?: return
    val o = opcionesBurbuja(e).getOrNull(k) ?: return
    actualizar { it.copy(burbuja = null) }
    when (o.especial) {
        "imagen" -> { celdaPendiente = cx to cy; pedirArchivoPara("widgetImagen", "image/*"); return }
        "video" -> { celdaPendiente = cx to cy; pedirArchivoPara("widgetVideo", "video/*"); return }
        "atajo" -> {
            val j = e.seleccionado ?: run { avisar("Primero elige un juego en el estante"); return }
            celdaPendiente = cx to cy; crearAtajo(j.clave); return
        }
        "coleccion" -> { celdaPendiente = cx to cy; abrirTexto("coleccion_nueva"); return }
    }
    ponerEnCelda(o.indice, cx, cy)
}


internal fun Diptico.ponerEnCelda(i: Int, cx: Int, cy: Int) {
    val e = estado.value
    val w = e.widgets.getOrNull(i) ?: return
    val destino = encajar(w.copy(visible = true, x = cx, y = cy, pagina = e.paginaInicio))
    val l = e.widgets.toMutableList()
    when {
        !choca(destino, i) -> { l[i] = destino; guardarWidgets(l) }
        else -> {
            val antes = estado.value.widgetSel
            actualizar { it.copy(widgetSel = i) }
            val empujada = empujando(i, destino)
            if (empujada != null) guardarWidgets(empujada.mapIndexed { k, x -> if (k == i) destino else x })
            else { actualizar { it.copy(widgetSel = antes) }; alternarWidget(i) }
        }
    }
    if (estado.value.widgets.getOrNull(i)?.visible == true) {
        actualizar { it.copy(widgetSel = i) }
        Sonidos.soltar()
        avisar("${Widgets.nombre(w.tipo)} agregado")
    }
}


/** Guarda la vista previa del tamaño al soltar la manija. */
fun Diptico.redimensionarWidget(i: Int, an: Int, al: Int) {
    val w = estado.value.widgets.getOrNull(i)?.takeIf { it.visible } ?: return
    val c = w.copy(an = an.coerceIn(1, (Widgets.COLUMNAS - w.x).coerceAtLeast(1)),
        al = al.coerceIn(1, (Widgets.FILAS - w.y).coerceAtLeast(1)))
    if (!ubicarWidget(i, c)) avisar("No hay espacio para estirarlo tanto")
}


fun Diptico.abrirCatalogoWidgets(celda: Pair<Int, Int>? = null) {
    if (!estado.value.editandoWidgets) editarWidgets()
    actualizar { it.copy(panel = Panel.ESTUDIO, estudioPestana = 1, indice = 0,
        estudioZona = "panel", estudioMover = false, burbuja = null, celdaCatalogo = celda) }
}


fun Diptico.cambiarPestanaEstudio(d: Int) = actualizar {
    it.copy(estudioPestana = (it.estudioPestana + d).mod(PESTANAS_ESTUDIO.size), indice = 0, estudioMover = false, celdaCatalogo = null)
}


fun Diptico.elegirPestanaEstudio(i: Int) = actualizar { it.copy(estudioPestana = i, indice = 0, estudioMover = false, celdaCatalogo = if (i == 1) it.celdaCatalogo else null) }


fun Diptico.alternarMoverWidget() {
    val v = !estado.value.estudioMover
    actualizar { it.copy(estudioMover = v) }
    if (v) Sonidos.levantar() else Sonidos.soltar()
    if (v) avisar("Cruceta mueve el widget · L1 R1 ancho · L2 R2 alto · A o B para terminar")
}


internal fun <T> Diptico.ciclar(l: List<T>, v: T, d: Int): T = l[(l.indexOf(v).coerceAtLeast(0) + d).mod(l.size)]


/** Pone el widget elegido en una forma lista (si choca, busca dónde cabe en su página). */
internal fun Diptico.ponerForma(i: Int, an: Int, al: Int) {
    val e = estado.value
    val w = e.widgets.getOrNull(i) ?: return
    if (an > Widgets.COLUMNAS || al > Widgets.FILAS) { avisar("No cabe en la grilla: agrega columnas o filas en la pestaña Página"); return }
    val c = encajar(w.copy(an = an, al = al))
    if (!choca(c, i)) { cambiarWidget(i) { c }; return }
    for (y in 0..Widgets.FILAS - al) for (x in 0..Widgets.COLUMNAS - an) {
        val d = c.copy(x = x, y = y)
        if (!choca(d, i)) { cambiarWidget(i) { d }; return }
    }
    avisar("No cabe en esta página: achica otro widget o cámbialo de página")
}


/** Los controles de la pestaña abierta del estudio. */
fun Diptico.controlesEstudio(e: Estado = estado.value): List<Control> = when (e.estudioPestana) {
    1 -> controlesAgregar(e).filter { c -> e.navegacion.buscarWidgets.isBlank() ||
        (c.titulo + " " + c.ayuda).contains(e.navegacion.buscarWidgets, ignoreCase = true) }
    2 -> controlesPagina(e)
    else -> controlesWidget(e).let { controles ->
        if (controles.size <= 1) controles else organizarControlesWidget(e, controles)
    }
}


fun Diptico.elegirGrupoEditorWidget(grupo: Int) = actualizar {
    it.copy(navegacion = it.navegacion.copy(grupoEditorWidget = grupo.coerceIn(0, 2)), indice = 1)
}


fun Diptico.buscarEnCatalogoWidgets(texto: String) = actualizar {
    it.copy(navegacion = it.navegacion.copy(buscarWidgets = texto.take(60)), indice = 0)
}


internal fun Diptico.organizarControlesWidget(e: Estado, controles: List<Control>): List<Control> {
    val grupo = e.navegacion.grupoEditorWidget
    val grupos = listOf("Posición y tamaño", "Aspecto", "Más")
    val selector = Control("Personalizar", "chips", grupo = "",
        elecciones = listOf("Tamaño", "Diseño", "Más ajustes"), elegida = grupo,
        elegir = { elegirGrupoEditorWidget(it) }) { elegirGrupoEditorWidget((grupo + 1).mod(3)) }
    return listOf(controles.first(), selector) + controles.drop(1).filter { it.grupo == grupos[grupo] }
}


internal fun Diptico.controlesWidget(e: Estado): List<Control> {
    val i = e.widgetSel
    val w = e.widgets.getOrNull(i)?.takeIf { it.visible } ?: return listOf(
        Control("No hay widgets a la vista", "accion", "Agregar", icono = Icono.APPS) { elegirPestanaEstudio(1) },
    )
    val formas = Widgets.FORMAS.filter { (_, f) -> f.first <= Widgets.COLUMNAS && f.second <= Widgets.FILAS }
    val colores = listOf(0L) + Temas.MUESTRAS
    val l = mutableListOf<Control>()
    l += Control(
        "Widget elegido", "pasos", Widgets.nombre(w.tipo), "Elegido", Widgets.icono(w.tipo),
        ayuda = "También lo eliges con la cruceta directo en tu inicio (B para volver allá).",
        menos = { siguienteWidget(-1) },
    ) { siguienteWidget(1) }
    l += Control(
        if (e.estudioMover) "Moviendo: usa la cruceta" else "Mover", "accion",
        if (e.estudioMover) "Terminar" else "A", "Posición y tamaño", Icono.MANDO,
        ayuda = "Si lo pones sobre otro, el otro se corre. Con el dedo: arrástralo, y estíralo desde su esquina.",
    ) { alternarMoverWidget() }
    l += Control(
        "Forma", "formas", Widgets.nombreTamano(w.an, w.al), "Posición y tamaño",
        elecciones = formas.map { it.first }, formas = formas.map { it.second },
        elegida = formas.indexOfFirst { it.second == (w.an to w.al) },
        menos = { tamanoWidgetSel(-1) }, elegir = { k -> formas.getOrNull(k)?.second?.let { (an, al) -> ponerForma(i, an, al) } },
    ) { tamanoWidgetSel(1) }
    l += Control("Ancho", "pasos", "${w.an} de ${Widgets.COLUMNAS}", "Posición y tamaño", Icono.FORMA, menos = { cambiarTamWidget(-1, 0) }) { cambiarTamWidget(1, 0) }
    l += Control("Alto", "pasos", "${w.al} de ${Widgets.FILAS}", "Posición y tamaño", Icono.FORMA, menos = { cambiarTamWidget(0, -1) }) { cambiarTamWidget(0, 1) }
    l += Control("Página", "pasos", "${w.pagina + 1} de ${e.paginas}", "Posición y tamaño", Icono.VISTA, menos = { moverWidgetPagina(i, -1) }) { moverWidgetPagina(i, 1) }
    val variantes = Widgets.variantes(w.tipo)
    if (variantes.size > 1) l += Control(
        "Diseño", "chips", variantes.getOrElse(w.variante) { variantes[0] }, "Aspecto", elecciones = variantes, elegida = w.variante.coerceIn(0, variantes.size - 1),
        elegir = { k -> cambiarWidget(i) { it.copy(variante = k.coerceIn(0, variantes.size - 1)) } },
    ) { cambiarWidget(i) { it.copy(variante = (it.variante + 1).mod(variantes.size)) } }
    l += Control(
        "Estilo", "estilos", Widgets.nombreEstilo(w.estilo), "Aspecto",
        elecciones = Widgets.ESTILOS.map { Widgets.nombreEstilo(it) }, elegida = Widgets.ESTILOS.indexOf(w.estilo),
        elegir = { k -> cambiarWidget(i) { it.copy(estilo = Widgets.ESTILOS[k.coerceIn(0, Widgets.ESTILOS.size - 1)]) } },
    ) { cambiarWidget(i) { it.copy(estilo = ciclar(Widgets.ESTILOS, it.estilo, 1)) } }
    l += Control(
        "Color", "colores", if (w.color == 0L) "Del tema" else "Propio", "Aspecto", colores = colores, elegida = colores.indexOf(w.color).coerceAtLeast(0),
        elegir = { k -> cambiarWidget(i) { it.copy(color = colores[k.coerceIn(0, colores.size - 1)]) } },
    ) { cambiarWidget(i) { it.copy(color = ciclar(colores, it.color, 1)) } }
    l += Control(
        "Fondo", "chips", "${(w.opacidad * 100).toInt()}%", "Aspecto",
        elecciones = OPACIDADES.map { if (it == 0f) "Sin fondo" else "${(it * 100).toInt()}%" }, elegida = OPACIDADES.indexOf(w.opacidad),
        elegir = { k -> cambiarWidget(i) { it.copy(opacidad = OPACIDADES[k.coerceIn(0, OPACIDADES.size - 1)]) } },
    ) { cambiarWidget(i) { it.copy(opacidad = ciclar(OPACIDADES, it.opacidad, 1)) } }
    l += Control(
        "Esquinas", "chips", "", "Aspecto", elecciones = listOf("Rectas", "Suaves", "Redondas", "Muy redondas"), elegida = RADIOS.indexOf(w.radio),
        elegir = { k -> cambiarWidget(i) { it.copy(radio = RADIOS[k.coerceIn(0, RADIOS.size - 1)]) } },
    ) { cambiarWidget(i) { it.copy(radio = ciclar(RADIOS, it.radio, 1)) } }
    l += Control(
        "Tamaño del contenido", "chips", "", "Aspecto", elecciones = listOf("Chico", "Normal", "Grande", "Enorme"), elegida = ESCALAS.indexOf(w.escala),
        elegir = { k -> cambiarWidget(i) { it.copy(escala = ESCALAS[k.coerceIn(0, ESCALAS.size - 1)]) } },
    ) { cambiarWidget(i) { it.copy(escala = ciclar(ESCALAS, it.escala, 1)) } }
    l += Control("Copiar el estilo a todos", "accion", "Aplicar", "Más", Icono.PALETA, "Todos los widgets quedan con este estilo, color, fondo y esquinas.") {
        val base = estado.value.widgets[i]
        guardarWidgets(estado.value.widgets.map { it.copy(estilo = base.estilo, color = base.color, opacidad = base.opacidad, radio = base.radio) })
        avisar("Estilo copiado a todos los widgets")
    }
    l += Control("Quitar este widget", "accion", "Quitar", "Más", Icono.BORRAR) { quitarWidget(i) }
    return l
}


internal fun Diptico.controlesAgregar(e: Estado): List<Control> {
    val tipos = e.widgets.withIndex().filter { (!it.value.tipo.startsWith("imagen") && !it.value.tipo.startsWith("video")) || it.value.visible }
    return tipos.map { (k, w) ->
        Control(
            Widgets.nombre(w.tipo), "catalogo", if (w.visible) "En tu inicio ✓" else "Agregar", if (w.visible) "En tu inicio" else "Para agregar",
            Widgets.icono(w.tipo), ayuda = Widgets.descripcion(w.tipo), extra = w.tipo,
        ) {
            if (w.visible) {
                actualizar { it.copy(widgetSel = k, estudioPestana = 0, indice = 0, paginaInicio = w.pagina, celdaCatalogo = null) }
            } else if (e.celdaCatalogo != null) {
                val (columna, fila) = e.celdaCatalogo
                actualizar { it.copy(celdaCatalogo = null) }
                ponerEnCelda(k, columna, fila)
            } else {
                alternarWidget(k)
                if (estado.value.widgets[k].visible) {
                    actualizar { it.copy(widgetSel = k) }
                    avisar("${Widgets.nombre(w.tipo)} agregado")
                }
            }
        }
    }.sortedBy { if (it.valor == "Agregar") 0 else 1 } + Control(
        "Imagen o GIF propio", "catalogo", "Elegir", "Para agregar", Icono.PORTADA,
        ayuda = "Una foto o un GIF animado de tu consola como widget.", extra = "imagen",
    ) { celdaPendiente = e.celdaCatalogo; pedirArchivoPara("widgetImagen", "image/*") } + Control(
        "Video corto", "catalogo", "Elegir", "Para agregar", Icono.JUGAR,
        ayuda = "Un clip corto (mp4) que se repite en silencio. Se pausa solo mientras juegas.", extra = "video",
    ) { celdaPendiente = e.celdaCatalogo; pedirArchivoPara("widgetVideo", "video/*") } + listOf(
        Control("Acceso a un juego", "catalogo", "Agregar", "Para agregar", Icono.CONTINUAR,
            ayuda = "Usa el juego elegido en el estante.", extra = "atajo") {
            val juego = estado.value.seleccionado
            if (juego == null) avisar("Primero elige un juego en el estante") else {
                celdaPendiente = e.celdaCatalogo
                crearAtajo(juego.clave)
                celdaPendiente = null
            }
        },
        Control("Colección nueva", "catalogo", "Crear", "Para agregar", Icono.CARPETA,
            ayuda = "Crea una colección y añádela a tu inicio.", extra = "coleccion") {
            celdaPendiente = e.celdaCatalogo
            abrirTexto("coleccion_nueva")
        },
    )
}


internal fun Diptico.controlesPagina(e: Estado): List<Control> = listOf(
    Control("Columnas", "pasos", "${e.gridCols}", "Grilla", Icono.APPS, "De 3 a 10.", menos = { cambiarGrilla(-1, 0, 0) }) { cambiarGrilla(1, 0, 0) },
    Control("Filas", "pasos", "${e.gridFilas}", "Grilla", Icono.APPS, "De 2 a 8. Con más de 4, el inicio se desplaza hacia abajo.", menos = { cambiarGrilla(0, -1, 0) }) { cambiarGrilla(0, 1, 0) },
    Control("Páginas", "pasos", "${e.paginas}", "Grilla", Icono.VISTA, "Cambia de página deslizando o con L2 y R2.", menos = { cambiarGrilla(0, 0, -1) }) { cambiarGrilla(0, 0, 1) },
    Control(
        "Separación", "chips", "", "Grilla", elecciones = listOf("Juntos", "Normal", "Amplia"), elegida = listOf(4, 10, 16).indexOf(e.huecoInicio),
        elegir = { k -> listOf(4, 10, 16).getOrNull(k)?.let { v -> almacen.guardarTexto("huecoInicio", v.toString()); actualizar { it.copy(huecoInicio = v) } } },
    ) { cambiarHueco(1) },
    Control("Saludo y fecha arriba", "interruptor", if (e.saludoInicio) "Sí" else "No", "Encabezado", Icono.TEXTO) {
        val v = !estado.value.saludoInicio
        almacen.guardarBool("saludoInicio", v)
        actualizar { it.copy(saludoInicio = v) }
    },
    Control("Fondo del inicio", "accion", if (e.fondoInicio != null) "Propio ✓" else "Arte del juego", "Fondo", Icono.PORTADA, "Una imagen o un GIF animado detrás de tus widgets.") {
        pedirArchivoPara("fondoInicio", "image/*")
    },
    Control("Quitar fondo propio", "accion", if (e.fondoInicio != null) "Quitar" else "—", "Fondo", Icono.BORRAR) {
        almacen.guardarTexto("fondoInicio", "")
        actualizar { it.copy(fondoInicio = null) }
    },
    Control("Volver a como venía", "accion", "Restablecer", "Fondo", Icono.BORRAR, "Deja los widgets y la grilla como vienen de fábrica.") {
        guardarGrilla(6, 4, 1)
        guardarWidgets(Widgets.porDefecto)
        actualizar { it.copy(widgetSel = 0, huecoInicio = 10) }
        almacen.guardarTexto("huecoInicio", "10")
        avisar("Inicio como al principio")
    },
)


internal fun Diptico.cambiarGrilla(dc: Int, df: Int, dp: Int) {
    val e = estado.value
    val c = (e.gridCols + dc).coerceIn(3, 10)
    val f = (e.gridFilas + df).coerceIn(2, 8)
    val pg = (e.paginas + dp).coerceIn(1, 6)
    if (c == e.gridCols && f == e.gridFilas && pg == e.paginas) return
    guardarGrilla(c, f, pg)
}


/** ← y → en el estudio. */
fun Diptico.cambiarControl(d: Int) {
    val c = controlesEstudio().getOrNull(estado.value.indice) ?: return
    when {
        d < 0 && c.menos != null -> c.menos.invoke()
        c.elegir != null && c.tipo != "widgets" && c.tipo != "formas" -> {
            val n = if (c.tipo == "colores") c.colores.size else c.elecciones.size
            if (n > 0) c.elegir.invoke((c.elegida + d).coerceIn(0, n - 1))
        }
        d > 0 -> c.mas()
    }
}


/** A en el estudio. */
fun Diptico.aceptarControl() {
    val c = controlesEstudio().getOrNull(estado.value.indice) ?: return
    when (c.tipo) {
        "accion", "interruptor", "catalogo" -> c.mas()
        else -> cambiarControl(1)
    }
}



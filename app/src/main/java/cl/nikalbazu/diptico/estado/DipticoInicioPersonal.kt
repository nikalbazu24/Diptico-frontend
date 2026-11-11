package cl.nikalbazu.diptico.estado

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
 * Inicio personalizado: funciones de Diptico movidas aquí desde Diptico.kt para que ese archivo no siga creciendo.
 * Se llaman igual que antes (Diptico.x()); fuera del paquete estado hay que importar cl.nikalbazu.diptico.estado.*.
 */


internal fun Diptico.opcionesInicio(e: Estado): List<Opcion> = listOf(
    Opcion("Usar el inicio nuevo", "Seguir jugando, progreso, música y recientes", Icono.APPS,
        "Ordena tus widgets con el diseño nuevo. Tu distribución de ahora queda guardada y vuelves a ella en Temas y decoraciones › Estilos guardados.") { aplicarInicioNuevo() },
    Opcion("Clip al retomar", if (e.clipRetoma) "Activado" else "Desactivado", Icono.CONTINUAR, "En dos pantallas: pide el permiso de grabación de Android al abrir un juego y guarda solo los últimos 6 segundos, sin audio. La lupa tiene prioridad. Puede afectar al rendimiento; se desactiva aquí.") { alternarClipRetoma() },
    Opcion("Preparar clip de esta partida", "Pedir permiso de Android", Icono.CONTINUAR, "Después de bloquear la consola Android puede finalizar la captura. Este botón prepara una nueva para la siguiente retoma.", apagada = e.sesion == null || !e.clipRetoma) { pedirPermisoRetoma(false) },
    Opcion("Retomar después de", "${e.esperaRetoma} segundos", Icono.RELOJ, "Tiempo mínimo con la pantalla apagada para mostrar el recordatorio al abrir la consola.") { cambiarEsperaRetoma() },
    Opcion(
        "Al abrir Díptico", nombreArranque(e.arranque), Icono.INTRO,
        "Con qué parte Díptico cada vez que se abre: como lo dejaste, las carátulas, tus widgets, tus consolas o directo en tu último juego.",
        menos = { cambiarArranque(-1) },
    ) { cambiarArranque(1) },
    Opcion(
        "Bienvenida al encender", nombreBienvenida(e.bienvenidaModo), Icono.CHISPA,
        "Al prender la consola: un saludo con la hora, tu último juego para seguir y DIPPO. Si estabas jugando, un recordatorio de dónde ibas. Nada se detiene mientras tanto.",
    ) { cambiarBienvenidaModo() },
    Opcion(
        "Continuación sobre el juego", if (cl.nikalbazu.diptico.bisa.Cartel.puede(app)) "Lista ✓" else "Dar permiso", Icono.CONTINUAR,
        "Con una pantalla, al prender la consola en medio de un juego aparece encima una tarjeta con el juego, tu tiempo y tu próximo paso. Android pide permiso para mostrarse sobre otras apps.",
    ) {
        if (cl.nikalbazu.diptico.bisa.Cartel.puede(app)) avisar("La continuación ya está lista")
        else try {
            app.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${app.packageName}")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { avisar("No pude abrir el permiso") }
    },
    Opcion(
        "Barra de accesos", if (e.barraFija) "Siempre visible" else "Se esconde sola", Icono.APPS,
        "Siempre visible: la barra queda fija. Se esconde sola: desaparece a los 5 segundos para que luzca el juego, y vuelve deslizando hacia arriba desde el borde de abajo, tocando la pantalla o con B.",
    ) { cambiarBarraFija(!estado.value.barraFija) },
    Opcion("Tu nombre", e.nombre.ifBlank { "Sin nombre" }, Icono.TEXTO, "Para que DIPPO te salude por tu nombre.") { abrirTexto("nombre", estado.value.nombre) },
    Opcion("Preguntar dónde quedaste", if (e.preguntarQuedaste) "Sí" else "No", Icono.LAPIZ,
        "Al terminar de jugar, una tarjeta para anotar dónde quedaste: escribiendo, con una nota de voz o marcando en el mapa del juego. Siempre se puede pasar de largo.") {
        val v = !estado.value.preguntarQuedaste
        almacen.guardarBool("preguntarQuedaste", v)
        actualizar { it.copy(preguntarQuedaste = v) }
    },
    Opcion("Imagen grande de los juegos", nombreHeroe(e.imagenHeroe), Icono.PORTADA,
        "Qué imagen se ve en grande en el inicio: la imagen grande (hero) o la portada. Cada juego puede tener la suya (ventana del juego, Y).") {
        val v = if (estado.value.imagenHeroe == "portada") "arte" else "portada"
        almacen.guardarTexto("imagenHeroe", v)
        actualizar { it.copy(imagenHeroe = v) }
    },
    Opcion(
        "Estilo del inicio", if (e.inicio == "widgets") "Con widgets" else "Carátulas", Icono.APPS,
        "Carátulas: el arte del juego ocupa la pantalla. Con widgets: tu inicio con reloj, música, recientes y más. " +
            "Se cambia rápido con Select.",
    ) { alternarInicio() },
    Opcion(
        "Estudio de widgets", "${e.widgets.count { it.visible }} a la vista", Icono.LAPIZ,
        "Todo en un lugar: tamaño, forma, estilo, color, diseño y página de cada widget. También manteniendo apretado el inicio.",
    ) { editarWidgets() },
    Opcion("Agregar widgets", icono = Icono.APPS, ayuda = "El catálogo de widgets: reloj, música, recientes, DIPPO y más.") { abrirCatalogoWidgets() },
    Opcion("Volver al orden original", icono = Icono.BORRAR, ayuda = "Deja los widgets como vienen de fábrica.") {
        guardarWidgets(Widgets.porDefecto)
        avisar("Widgets como al principio")
    },
) + opcionesGrilla(e) + listOf(
    Opcion(
        "Permiso para el widget de música", if (Medios.tienePermiso(app)) "Listo ✓" else "Dar permiso", Icono.MUSICA,
        "Para mostrar y controlar lo que suena en Spotify, YouTube Music u otra app, Android pide activar el \"acceso a notificaciones\" de Díptico.",
    ) { if (!Medios.tienePermiso(app)) Medios.pedirPermiso(app) else avisar("El widget de música ya tiene permiso") },
)


internal fun Diptico.cambiarArranque(d: Int) {
    val i = ARRANQUES.indexOf(estado.value.arranque).coerceAtLeast(0)
    val v = ARRANQUES[Math.floorMod(i + d, ARRANQUES.size)]
    almacen.guardarTexto("arranque", v)
    actualizar { it.copy(arranque = v) }
    avisar("Al abrir: ${nombreArranque(v)}")
}


/** Select: cambia entre el inicio con widgets y el de carátulas. */
/** Abre un widget por su tipo (al tocarlo). */
fun Diptico.abrirWidgetTipo(tipo: String) {
    val i = estado.value.widgets.indexOfFirst { it.tipo == tipo }
    if (i >= 0) abrirWidget(i)
}


/** Deja el inicio en la portada ("heroe") o en los widgets (al deslizar la pantalla de arriba). */
fun Diptico.fijarInicio(v: String) {
    if (estado.value.inicio == v || v !in setOf("heroe", "widgets")) return
    almacen.guardarTexto("inicio", v)
    actualizar { it.copy(inicio = v, widgetFoco = -1) }
}


fun Diptico.alternarInicio() {
    actualizar { it.copy(widgetFoco = -1) }
    val v = if (estado.value.inicio == "widgets") "heroe" else "widgets"
    almacen.guardarTexto("inicio", v)
    actualizar { it.copy(inicio = v, editandoWidgets = false, avisoInicio = if (v == "widgets") System.currentTimeMillis() else 0L) }
    Sonidos.deslizar()
}


internal fun Diptico.guardarWidgets(l: List<Widget>) {
    almacen.guardarTexto("widgets", escribirWidgets(l))
    actualizar { it.copy(widgets = l) }
}


/** Entra al modo de acomodar widgets (en la pantalla del inicio). */
fun Diptico.editarWidgets() {
    almacen.guardarTexto("inicio", "widgets")
    val primero = widgetsDePagina().firstOrNull() ?: -1
    actualizar { it.copy(inicio = "widgets", editandoWidgets = true, widgetSel = primero, panel = Panel.ESTUDIO, estudioPestana = 0, estudioMover = false, indice = 0, widgetFoco = -1, estudioZona = "grilla", burbuja = null, celdaCatalogo = null) }
}


fun Diptico.terminarEdicion() {
    almacen.guardarTexto("widgets", escribirWidgets(estado.value.widgets))
    actualizar { it.copy(editandoWidgets = false, estudioMover = false, burbuja = null, celdaCatalogo = null,
        panel = if (it.panel == Panel.ESTUDIO) Panel.NINGUNO else it.panel) }
    avisar("Inicio guardado")
}


internal fun Diptico.cambiarWidgetYEmpujar(i: Int, f: (Widget) -> Widget) {
    cambiarWidget(i, f)
    aplicarEmpuje()
}


internal fun Diptico.cambiarWidget(i: Int, f: (Widget) -> Widget) {
    val l = estado.value.widgets.toMutableList()
    val w = l.getOrNull(i) ?: return
    l[i] = f(w)
    actualizar { it.copy(widgets = l) }
}


/** ¿El widget [w] (en la posición [i] de la lista) se monta sobre otro de su página? */
internal fun Diptico.choca(w: Widget, i: Int, l: List<Widget> = estado.value.widgets): Boolean = l.withIndex().any { (k, o) ->
    k != i && o.visible && o.pagina == w.pagina &&
        w.x < o.x + o.an && o.x < w.x + w.an && w.y < o.y + o.al && o.y < w.y + w.al
}


internal fun Diptico.encajar(w: Widget): Widget {
    val an = w.an.coerceIn(1, Widgets.COLUMNAS)
    val al = w.al.coerceIn(1, Widgets.FILAS)
    return w.copy(an = an, al = al, x = w.x.coerceIn(0, Widgets.COLUMNAS - an), y = w.y.coerceIn(0, Widgets.FILAS - al))
}


fun Diptico.moverWidgetSel(dx: Int, dy: Int) {
    val e = estado.value
    val i = e.widgetSel
    cambiarWidget(i) { w ->
        val nx = w.x + dx
        when {
            // Al salir por un costado pasa a la página de al lado (si allá hay espacio).
            nx < 0 && w.pagina > 0 -> encajar(w.copy(pagina = w.pagina - 1, x = Widgets.COLUMNAS - w.an)).takeIf { !choca(it, i) } ?: w
            nx + w.an > Widgets.COLUMNAS && w.pagina < e.paginas - 1 -> encajar(w.copy(pagina = w.pagina + 1, x = 0)).takeIf { !choca(it, i) } ?: w
            else -> {
                // Avanza una celda; si ahí hay otro widget, ese se corre para dejarle espacio.
                val c = encajar(w.copy(x = w.x + dx, y = w.y + dy))
                if (c.x == w.x && c.y == w.y) w
                else if (!choca(c, i)) c
                else {
                    val l = empujando(i, c)
                    if (l != null) { pendienteEmpuje = l; c } else {
                        // Si no hay cómo correrlo, salta por encima hasta el primer lugar libre.
                        var paso = 2
                        var r: Widget? = null
                        while (paso <= maxOf(Widgets.COLUMNAS, Widgets.FILAS)) {
                            val c2 = encajar(w.copy(x = w.x + dx * paso, y = w.y + dy * paso))
                            if (c2.x == w.x && c2.y == w.y) break
                            if (!choca(c2, i)) { r = c2; break }
                            paso++
                        }
                        r ?: w
                    }
                }
            }
        }
    }
    aplicarEmpuje()
    estado.value.widgets.getOrNull(e.widgetSel)?.let { w -> if (w.pagina != e.paginaInicio) actualizar { it.copy(paginaInicio = w.pagina) } }
}


/** Un solo cambio de estado para mover o estirar y reubicar a los vecinos. */
internal fun Diptico.ubicarWidget(i: Int, destino: Widget): Boolean {
    val e = estado.value
    val antes = e.widgets.getOrNull(i) ?: return false
    val c = encajar(destino)
    if (c == antes) return true
    val nueva = empujando(i, c) ?: return false
    almacen.guardarTexto("widgets", escribirWidgets(nueva))
    actualizar { it.copy(widgets = nueva, widgetSel = i, paginaInicio = c.pagina) }
    return true
}


fun Diptico.ponerWidget(i: Int, x: Int, y: Int, pagina: Int) {
    val e = estado.value
    val w = e.widgets.getOrNull(i)?.takeIf { it.visible } ?: return
    val destino = encajar(w.copy(x = x, y = y, pagina = pagina.coerceIn(0, e.paginas - 1)))
    if (ubicarWidget(i, destino)) return
    // Si el punto de caída está ocupado, busca el hueco más cercano de la página destino.
    if (destino.pagina != w.pagina) {
        val libre = (0..Widgets.FILAS - w.al).asSequence().flatMap { fila ->
            (0..Widgets.COLUMNAS - w.an).asSequence().map { columna -> destino.copy(x = columna, y = fila) }
        }.filterNot { choca(it, i, e.widgets) }
            .minByOrNull { kotlin.math.abs(it.x - destino.x) + kotlin.math.abs(it.y - destino.y) }
        if (libre != null && ubicarWidget(i, libre)) return
    }
    avisar("No hay espacio. El widget sigue en su lugar.")
}


internal fun Diptico.aplicarEmpuje() {
    val l = pendienteEmpuje ?: return
    pendienteEmpuje = null
    val i = estado.value.widgetSel
    val actual = estado.value.widgets
    // El elegido queda como quedó; los demás toman su lugar nuevo.
    actualizar { it.copy(widgets = l.mapIndexed { k, w -> if (k == i) actual.getOrElse(k) { w } else w }) }
    Sonidos.soltar()
}


/**
 * Pone el widget [i] en [destino] y corre a los que tapa: primero al lugar que dejó libre (así se intercambian),
 * y si no cabe ahí, al hueco libre más cercano de la misma página. Devuelve la lista nueva o null si no hay espacio.
 */
internal fun Diptico.empujando(i: Int, destino: Widget): List<Widget>? {
    val l = estado.value.widgets.toMutableList()
    val antes = l.getOrNull(i) ?: return null
    l[i] = destino
    val tapados = l.indices.filter { k -> k != i && l[k].visible && l[k].pagina == destino.pagina && solapan(l[k], destino) }
    for (k in tapados) {
        val o = l[k]
        val candidatos = buildList {
            if (antes.pagina == destino.pagina) {
                add(o.copy(x = antes.x, y = antes.y))
                add(o.copy(x = antes.x + antes.an - o.an, y = antes.y))
                add(o.copy(x = antes.x, y = antes.y + antes.al - o.al))
            }
            for (yy in 0..Widgets.FILAS - o.al) for (xx in 0..Widgets.COLUMNAS - o.an) add(o.copy(x = xx, y = yy))
        }.map { encajar(it) }
        val inicioCandidatos = if (antes.pagina == destino.pagina) 3 else 0
        val elegido = candidatos.take(inicioCandidatos).firstOrNull { !choca(it, k, l) }
            ?: candidatos.drop(inicioCandidatos).sortedBy { kotlin.math.abs(it.x - o.x) + kotlin.math.abs(it.y - o.y) }.firstOrNull { !choca(it, k, l) }
            ?: return null
        l[k] = elegido
    }
    return l
}


internal fun Diptico.solapan(a: Widget, b: Widget) = a.x < b.x + b.an && b.x < a.x + a.an && a.y < b.y + b.al && b.y < a.y + a.al


/** Cambia el ancho (L1/R1) o el alto (L2/R2) del widget elegido; si choca, corre a los otros. */
fun Diptico.cambiarTamWidget(dAn: Int, dAl: Int) {
    val i = estado.value.widgetSel
    cambiarWidgetYEmpujar(i) { w ->
        val an = (w.an + dAn).coerceIn(1, Widgets.COLUMNAS)
        val al = (w.al + dAl).coerceIn(1, Widgets.FILAS)
        // Si no cabe a la derecha o abajo, se corre hacia la izquierda o arriba.
        val c = w.copy(an = an, al = al, x = w.x.coerceAtMost(Widgets.COLUMNAS - an), y = w.y.coerceAtMost(Widgets.FILAS - al))
        if (!choca(c, i)) c
        else empujando(i, c)?.let { l -> pendienteEmpuje = l; c } ?: run { avisar("No cabe: no hay cómo correr los otros widgets"); w }
    }
}


fun Diptico.tamanoWidgetSel(delta: Int) = cambiarWidget(estado.value.widgetSel) { w ->
    val t = Widgets.TAMANOS
    val i = estado.value.widgetSel
    val actual = t.indexOfFirst { it.first == w.an && it.second == w.al }
    var k = actual
    var r = w
    repeat(t.size) {
        k = (k + delta).mod(t.size)
        val (an, al) = t[k]
        val c = encajar(w.copy(an = an, al = al))
        if (!choca(c, i)) { r = c; return@cambiarWidget r }
    }
    r
}


/** Cambia el estilo del widget elegido: tarjeta, vidrio, color, contorno o sin fondo. */
fun Diptico.cambiarEstiloWidget(i: Int = estado.value.widgetSel) {
    cambiarWidget(i) { w -> w.copy(estilo = Widgets.ESTILOS[(Widgets.ESTILOS.indexOf(w.estilo) + 1).mod(Widgets.ESTILOS.size)]) }
    estado.value.widgets.getOrNull(i)?.let { avisar("Estilo: ${Widgets.nombreEstilo(it.estilo)}") }
}


fun Diptico.siguienteWidget(delta: Int) {
    val l = estado.value.widgets
    val visibles = l.indices.filter { l[it].visible }
    if (visibles.isEmpty()) return
    val pos = visibles.indexOf(estado.value.widgetSel)
    actualizar { it.copy(widgetSel = visibles[(pos + delta).mod(visibles.size)]) }
}


fun Diptico.elegirWidget(i: Int) = actualizar { it.copy(widgetSel = i) }


/** Muestra u oculta un widget. Al mostrarlo, lo pone en el primer hueco libre. */
internal fun Diptico.alternarWidget(i: Int) {
    val l = estado.value.widgets
    val w = l.getOrNull(i) ?: return
    if (w.visible) {
        cambiarWidget(i) { it.copy(visible = false) }
    } else {
        // Busca un hueco en la página actual (achicándolo si hace falta) y si no, en las otras páginas.
        val e = estado.value
        var puesto: Widget? = null
        val paginas = listOf(e.paginaInicio) + (0 until e.paginas).filter { it != e.paginaInicio }
        busqueda@ for (pag in paginas) for ((an, al) in listOf(w.an to w.al, 2 to 2, 2 to 1, 1 to 1)) {
            for (y in 0..Widgets.FILAS - al) for (x in 0..Widgets.COLUMNAS - an) {
                val c = w.copy(visible = true, x = x, y = y, an = an, al = al, pagina = pag)
                if (!choca(c, i, l)) { puesto = c; break@busqueda }
            }
        }
        if (puesto == null) { avisar("No hay espacio. Agrega una página o achica otro widget."); return }
        cambiarWidget(i) { puesto }
        if (puesto.pagina != e.paginaInicio) actualizar { it.copy(paginaInicio = puesto.pagina) }
    }
    almacen.guardarTexto("widgets", escribirWidgets(estado.value.widgets))
}

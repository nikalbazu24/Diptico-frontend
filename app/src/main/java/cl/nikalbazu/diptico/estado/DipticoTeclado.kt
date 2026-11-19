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
 * Teclado de Díptico: funciones de Diptico movidas aquí desde Diptico.kt para que ese archivo no siga creciendo.
 * Se llaman igual que antes (Diptico.x()); fuera del paquete estado hay que importar cl.nikalbazu.diptico.estado.*.
 */


/**
 * Teclado estilo Gboard. Fila 0: la franja de sugerencias (o las herramientas, con ⋯). Luego las letras,
 * con ⇧ y ⌫ a los lados de la tercera fila, y abajo ?123, coma, espacio, punto y Listo.
 */
fun Diptico.filasTeclado(e: Estado): List<List<String>> {
    val franja = if (e.extra.tecladoEdicion) listOf("⋯") + EDICION
    else listOf("⋯") + sugerenciasTeclado(e).map { "»$it" } + listOf("Voz")
    val m = e.mayus && !e.simbolos
    fun fila(t: String) = t.map { c -> if (m) c.uppercase() else c.toString() }
    val cuerpo = if (e.simbolos) listOf(
        fila("1234567890"), fila("@#$%&-+()/"), listOf("¿", "¡", "*", "\"", "'", ":", ";", "!", "?", "⌫"),
    ) else listOf(
        fila("qwertyuiop"), fila("asdfghjklñ"), listOf("⇧") + fila("zxcvbnm") + listOf("⌫"),
    )
    return listOf(franja) + cuerpo + listOf(listOf(if (e.simbolos) "abc" else "?123", ",", "␣", ".", "✓"))
}


/** Las palabras que propone la franja del teclado para lo que se está escribiendo. */
fun Diptico.sugerenciasTeclado(e: Estado): List<String> {
    if (e.panel == Panel.CLAVE && (e.textoPara == "sgdb" || e.textoPara.startsWith("ia_clave") || e.textoPara.startsWith("ra_") || e.textoPara == "tema_enlace")) return emptyList()
    val c = cursorDe(e)
    val para = if (e.panel == Panel.PASO) "paso" else e.textoPara
    val buscar = para == "buscar_juego" || para == "titulo" || para == "buscar_imagen"
    return Palabras.sugerir(e.borrador.substring(0, c), para, e.juegos.map { it.titulo }, buscar)
}


/** Pone la sugerencia en lugar de la palabra que se estaba escribiendo (o del texto entero, si es un título). */
fun Diptico.aceptarSugerencia(s: String) {
    val e = estado.value
    val c = cursorDe(e)
    val antes = e.borrador.substring(0, c)
    val despues = e.borrador.substring(c)
    val buscar = e.textoPara == "buscar_juego" || e.textoPara == "titulo" || e.textoPara == "buscar_imagen"
    val base = when {
        buscar && s.contains(' ') || buscar && Palabras.normal(s).contains(Palabras.normal(antes.trim())) && antes.trim().contains(' ') -> ""
        else -> antes.dropLast(Palabras.actual(antes).length)
    }
    val espacio = if (buscar && base.isEmpty()) "" else " "
    val nuevo = base + s + espacio
    cambiarBorrador(nuevo + despues)
    actualizar { it.copy(cursor = if (despues.isEmpty()) -1 else nuevo.length, ancla = null, mayus = it.extra.mayusFija) }
}


/** Borra la palabra antes del cursor (mantener ⌫). */
fun Diptico.borrarPalabra() {
    val e = estado.value
    val c = cursorDe(e)
    if (c == 0) return
    val antes = e.borrador.substring(0, c)
    val sinEspacios = antes.trimEnd()
    val corte = sinEspacios.length - Palabras.actual(sinEspacios).length.coerceAtLeast(if (sinEspacios.isEmpty()) 0 else 1)
    val nuevo = antes.substring(0, corte.coerceIn(0, antes.length))
    cambiarBorrador(nuevo + e.borrador.substring(c))
    actualizar { it.copy(cursor = if (c >= e.borrador.length) -1 else nuevo.length, ancla = null, mayus = nuevo.isBlank()) }
}


/** ⇧: un toque, la siguiente letra en mayúscula; dos toques seguidos, mayúsculas fijas. */
internal fun Diptico.pulsarShift() {
    val e = estado.value
    val ahora = System.currentTimeMillis()
    when {
        e.extra.mayusFija -> actualizar { it.copy(mayus = false, extra = it.extra.copy(mayusFija = false)) }
        e.mayus && ahora - ultimoShift < 450 -> actualizar { it.copy(mayus = true, extra = it.extra.copy(mayusFija = true)) }
        else -> actualizar { it.copy(mayus = !it.mayus) }
    }
    ultimoShift = ahora
}


/** La tecla elegida con la cruceta, con su otra letra (Y): tildes y números. */
fun Diptico.varianteElegida() {
    val e = estado.value
    val t = filasTeclado(e).getOrNull(e.teclaFila)?.getOrNull(e.teclaCol) ?: return
    val v = Palabras.variante(t)
    if (v != null) escribirLetra(v) else pulsarValor("⇧")
}


/** Escribe una letra y suelta la mayúscula de una sola vez. */
fun Diptico.escribirLetra(t: String) {
    val e = estado.value
    escribir(t, quitarMayus = e.mayus && !e.simbolos && !e.extra.mayusFija && t != "." && t != "?" && t != "!")
}


fun Diptico.moverTecla(dx: Int, dy: Int) {
    val e = estado.value
    val filas = filasTeclado(e)
    var f = e.teclaFila.coerceIn(0, filas.size - 1)
    var c = e.teclaCol
    if (dy != 0) {
        val nf = (f + dy).coerceIn(0, filas.size - 1)
        if (nf != f) {
            val a = filas[f].size - 1
            val b = filas[nf].size - 1
            c = if (a <= 0) 0 else Math.round(c * b / a.toFloat())
            f = nf
        }
    }
    if (dx != 0) c = (c + dx).mod(filas[f].size)
    actualizar { it.copy(teclaFila = f, teclaCol = c.coerceIn(0, filas[f].size - 1), extra = if (it.extra.tecladoDedo) it.extra.copy(tecladoDedo = false) else it.extra) }
}


/** Aprieta una tecla (con A sobre la elegida, o tocándola). */
/**
 * Una tecla del teclado de Díptico. [dedo]: se tocó en la pantalla; el foco del mando no salta a esa tecla (antes
 * quedaba marcada en morado y parecía pegada) y se esconde hasta que se vuelva a usar la cruceta.
 */
fun Diptico.pulsarTecla(fila: Int = estado.value.teclaFila, col: Int = estado.value.teclaCol, dedo: Boolean = false) {
    val e = estado.value
    val t = filasTeclado(e).getOrNull(fila)?.getOrNull(col) ?: return
    if (dedo) { if (!e.extra.tecladoDedo) actualizar { it.copy(extra = it.extra.copy(tecladoDedo = true)) } }
    else if (fila != e.teclaFila || col != e.teclaCol || e.extra.tecladoDedo)
        actualizar { it.copy(teclaFila = fila, teclaCol = col, extra = it.extra.copy(tecladoDedo = false)) }
    pulsarValor(t)
    Sonidos.mover()
}


/** Hace lo de una tecla según su etiqueta. */
fun Diptico.pulsarValor(t: String) {
    val e = estado.value
    when (t) {
        "⇧" -> pulsarShift()
        "#", "?123", "abc" -> actualizar { it.copy(simbolos = !it.simbolos) }
        "⋯" -> actualizar { it.copy(extra = it.extra.copy(tecladoEdicion = !it.extra.tecladoEdicion)) }
        "␣" -> {
            // Dos espacios seguidos ponen un punto, como en Gboard.
            val c = cursorDe(e)
            val antes = e.borrador.substring(0, c)
            if (antes.length >= 2 && antes.last() == ' ' && antes[antes.length - 2].isLetterOrDigit()) {
                borrarLetra(); escribir(". ")
            } else escribir(" ")
        }
        "⌫" -> borrarLetra()
        "✓" -> terminarTexto()
        "Voz" -> dictar("teclado")
        "◀" -> moverCursor(-1)
        "▶" -> moverCursor(1)
        "Selec." -> alternarSeleccion()
        "Todo" -> actualizar { it.copy(ancla = 0, cursor = it.borrador.length) }
        "Copiar" -> copiar(false)
        "Cortar" -> copiar(true)
        "Pegar" -> pegar()
        else -> if (t.startsWith("»")) aceptarSugerencia(t.drop(1)) else escribirLetra(t)
    }
}


internal fun Diptico.cursorDe(e: Estado) = if (e.cursor < 0 || e.cursor > e.borrador.length) e.borrador.length else e.cursor


/** Lo seleccionado, como rango (vacío si no hay selección). */
fun Diptico.seleccion(e: Estado): IntRange {
    val c = cursorDe(e)
    val a = e.ancla?.coerceIn(0, e.borrador.length) ?: return IntRange.EMPTY
    return minOf(a, c) until maxOf(a, c)
}


/**
 * Escribe [t] donde está el cursor (o en lugar de lo seleccionado). Todo en un solo cambio de estado: con varios
 * seguidos, cada letra redibujaba la pantalla tres veces y al escribir rápido se notaba el retraso.
 */
fun Diptico.escribir(t: String, quitarMayus: Boolean = false) {
    actualizar { e ->
        val sel = seleccion(e)
        val c = cursorDe(e)
        val (antes, despues) = if (!sel.isEmpty()) e.borrador.substring(0, sel.first) to e.borrador.substring(sel.last + 1)
        else e.borrador.substring(0, c) to e.borrador.substring(c)
        val texto = (antes + t + despues).take(maxBorrador(e))
        val nuevo = (antes.length + t.length).coerceAtMost(texto.length)
        // Después de un punto, la siguiente va con mayúscula.
        val punto = t.trimEnd().let { it.endsWith(".") || it.endsWith("?") || it.endsWith("!") }
        e.copy(
            borrador = texto, cursor = if (despues.isEmpty() || nuevo >= texto.length) -1 else nuevo, ancla = null,
            mayus = when { punto -> true; quitarMayus -> false; else -> e.mayus },
        )
    }
}

/** Pone el cursor donde se tocó el texto ([i] = posición en el borrador). Con [extender], selecciona hasta ahí. */
fun Diptico.ponerCursor(i: Int, extender: Boolean = false) {
    actualizar { e ->
        val c = i.coerceIn(0, e.borrador.length)
        e.copy(cursor = if (c >= e.borrador.length) -1 else c, ancla = if (extender) e.ancla ?: cursorDe(e) else null)
    }
}

/** Selecciona del carácter [desde] al [hasta] (arrastrar el dedo sobre el texto). */
fun Diptico.seleccionarTexto(desde: Int, hasta: Int) {
    actualizar { e ->
        val a = desde.coerceIn(0, e.borrador.length)
        val c = hasta.coerceIn(0, e.borrador.length)
        e.copy(ancla = if (a == c) null else a, cursor = if (c >= e.borrador.length) -1 else c)
    }
}

/** Selecciona la palabra que está en [i] (doble toque sobre el texto). */
fun Diptico.seleccionarPalabra(i: Int) {
    val b = estado.value.borrador
    if (b.isEmpty()) return
    val k = i.coerceIn(0, b.length - 1)
    if (!b[k].isLetterOrDigit()) { ponerCursor(i); return }
    var a = k; var z = k
    while (a > 0 && b[a - 1].isLetterOrDigit()) a--
    while (z < b.length && b[z].isLetterOrDigit()) z++
    seleccionarTexto(a, z)
}


fun Diptico.borrarLetra() {
    val e = estado.value
    val sel = seleccion(e)
    if (!sel.isEmpty()) {
        cambiarBorrador(e.borrador.removeRange(sel.first, sel.last + 1))
        actualizar { it.copy(cursor = sel.first, ancla = null) }
    } else {
        val c = cursorDe(e)
        if (c > 0) {
            cambiarBorrador(e.borrador.removeRange(c - 1, c))
            actualizar { it.copy(cursor = if (c >= e.borrador.length) -1 else c - 1) }
        }
    }
    if (estado.value.borrador.isEmpty()) actualizar { it.copy(mayus = true, cursor = -1, ancla = null) }
}


fun Diptico.moverCursor(d: Int) {
    val e = estado.value
    val c = (cursorDe(e) + d).coerceIn(0, e.borrador.length)
    actualizar { it.copy(cursor = c) }
}


/** Empieza (o termina) a seleccionar desde donde está el cursor. */
fun Diptico.alternarSeleccion() {
    val e = estado.value
    actualizar { it.copy(ancla = if (e.ancla == null) cursorDe(e) else null) }
    if (e.ancla == null) avisar("Mueve el cursor para seleccionar")
}


internal fun Diptico.portapapeles() = app.getSystemService(android.content.ClipboardManager::class.java)


/** Copia lo seleccionado (o todo si no hay selección). Con [cortar], además lo borra. */
fun Diptico.copiar(cortar: Boolean) {
    val e = estado.value
    val sel = seleccion(e)
    val texto = if (sel.isEmpty()) e.borrador else e.borrador.substring(sel.first, sel.last + 1)
    if (texto.isEmpty()) { avisar("No hay nada que copiar"); return }
    try { portapapeles()?.setPrimaryClip(android.content.ClipData.newPlainText("Díptico", texto)) } catch (_: Exception) {}
    if (cortar) {
        if (sel.isEmpty()) { cambiarBorrador(""); actualizar { it.copy(cursor = -1, ancla = null) } } else borrarLetra()
    } else {
        actualizar { it.copy(ancla = null) }
    }
    avisar(if (cortar) "Cortado" else "Copiado")
}


fun Diptico.pegar() {
    val t = try { portapapeles()?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(app)?.toString() } catch (_: Exception) { null }
    if (t.isNullOrEmpty()) { avisar("No hay nada para pegar"); return }
    escribir(t.trim())
}


fun Diptico.terminarTexto() {
    if (estado.value.panel == Panel.PASO) guardarPaso() else guardarClave()
}


/** Cuántas letras admite lo que se está escribiendo. */
internal fun maxBorrador(e: Estado): Int = when {
    e.panel != Panel.CLAVE -> 160
    e.textoPara == "notas" -> 2000
    e.textoPara == "tema_enlace" -> 600
    e.textoPara == "discord_webhook" -> 300
    e.textoPara == "titulo" -> 80
    else -> 200
}

fun Diptico.cambiarBorrador(texto: String) {
    actualizar { it.copy(borrador = texto.take(maxBorrador(it))) }
}


fun Diptico.guardarPaso() {
    val e = estado.value
    val clave = e.seleccionado?.clave ?: return
    val datos = e.datos.toMutableMap()
    datos[clave] = (datos[clave] ?: DatosJuego()).copy(paso = e.borrador.trim())
    almacen.guardarDatos(datos)
    actualizar { it.copy(datos = datos, panel = Panel.NINGUNO, pregunta = false) }
    avisar("Próximo paso guardado")
}


fun Diptico.guardarClave() {
    when (estado.value.textoPara) {
        "sgdb" -> Unit
        else -> {
            guardarTextoPanel()
            return
        }
    }
    val clave = estado.value.borrador.trim()
    if (clave.isEmpty()) {
        almacen.claveSgdb = ""
        actualizar { it.copy(claveSgdb = "") }
        cerrarPanel()
        avisar("Clave de SteamGridDB quitada")
        return
    }
    avisar("Probando la clave…")
    scope.launch {
        val ok = withContext(Dispatchers.IO) { try { Descargas.probarSgdb(clave) } catch (e: Exception) { false } }
        if (ok) {
            almacen.claveSgdb = clave
            actualizar { it.copy(claveSgdb = clave) }
            cerrarPanel()
            avisar("¡Clave conectada! Ya puedo bajar logos y arte grande")
        } else {
            avisar("La clave no funcionó. Revísala o prueba tu conexión.")
        }
    }
}


/** Arriba/abajo: en el menú se mueve dentro de la columna que tiene el foco. */
fun Diptico.moverIndice(delta: Int) {
    val antes = estado.value
    moverIndiceSolo(delta)
    val ahora = estado.value
    // Si no se movió nada, llegaste al borde: suena distinto.
    if (delta != 0 && ahora.indice == antes.indice && ahora.seccion == antes.seccion && ahora.panel == antes.panel && ahora.widgetSel == antes.widgetSel) Sonidos.borde()
}


internal fun Diptico.moverIndiceSolo(delta: Int) {
    val e = estado.value
    if (e.panel in setOf(Panel.MENU, Panel.RAPIDO) && e.desplegable >= 0) {
        val total = opcionDesplegada(e)?.alternativas?.size ?: 0
        if (total > 0) actualizar { it.copy(extra = it.extra.copy(desplegableSel = (it.desplegableSel + delta).coerceIn(0, total - 1))) }
        return
    }
    subOpciones(e)?.let { sub ->
        if (sub.isNotEmpty()) actualizar { it.copy(extra = it.extra.copy(subSel = (it.extra.subSel + delta).coerceIn(0, sub.size - 1))) }
        return
    }
    if (!e.configurado && e.panel == Panel.NINGUNO) {
        val total = opcionesTuto().size
        if (total == 0) return
        val n = (e.indice + delta).coerceIn(0, total - 1)
        if (n == e.indice) return
        actualizar { it.copy(indice = n) }
        vistaPrevia(e.pasoTuto, n)
        return
    }
    when (e.panel) {
        Panel.MENU -> if (!e.enOpciones) {
            saltarSeccion(delta, envolver = false)
        } else {
            val total = opcionesSeccion(e.seccion).size
            if (total > 0) actualizar { it.copy(indice = (it.indice + delta + total) % total) }
        }
        Panel.WIDGET -> {
            val total = opcionesWidget(e).size
            if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
        }
        Panel.RAPIDO -> {
            if (estado.value.extra.rapidoGrupo >= 0) moverRapido(0, delta)
            else {
                val total = opcionesRapido().size
                if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
            }
        }
        Panel.ASISTENTE -> {
            val total = elementosAsistente().size
            if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
        }
        Panel.APPS -> {
            val total = appsVisibles(e).size
            if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
        }
        Panel.EDITAR_CONSOLA -> {
            val total = opcionesEditarConsola().size
            if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
        }
        Panel.ESTUDIO -> {
            val total = controlesEstudio(e).size
            if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
        }
        Panel.GALERIA -> {
            val total = itemsGaleria(e).size
            if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
        }
        Panel.IMAGENES -> {
            val total = candidatosVisibles(e).size
            if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
        }
        Panel.BUSCAR -> {
            val total = e.visibles.size
            if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
        }
        Panel.CENTRO -> {
            val total = if (e.cortina == "avisos") Avisos.lista.value.size else opcionesCentro().size
            if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
        }
        Panel.CARPETAS -> {
            val total = if (e.consolaPara >= 0) consolasElegibles().size + 1 else filasCarpetas()
            actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
        }
        Panel.PORTS -> {
            val total = e.apps.size
            if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
        }
        Panel.FICHA -> {
            val total = when {
                e.pestanaFicha == 4 -> e.logrosFicha?.logros?.size ?: 0
                e.pestanaFicha >= 2 -> opcionesFicha().size
                else -> 40
            }
            if (total > 0) actualizar { it.copy(indice = (it.indice + delta).coerceIn(0, total - 1)) }
        }
        else -> Unit
    }
}


/** Izquierda/derecha en el menú: cambia de columna. */
fun Diptico.columnaMenu(aOpciones: Boolean) {
    actualizar { it.copy(enOpciones = aOpciones, indice = if (aOpciones) 0 else it.indice, extra = it.extra.copy(desplegable = -1)) }
}


fun Diptico.elegirSeccion(s: Seccion) {
    actualizar { it.copy(seccion = s, enOpciones = true, indice = 0, extra = it.extra.copy(desplegable = -1)) }
}

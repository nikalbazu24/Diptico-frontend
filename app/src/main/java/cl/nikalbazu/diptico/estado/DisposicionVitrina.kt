package cl.nikalbazu.diptico.estado

/*
 * Lugar en la vitrina (Ficha › Personalizar): en una cuadrícula de 9 × 5 se mueven a gusto el logo, la imagen grande
 * de fondo y la caja 3D de cada juego, con su tamaño, y se elige si va la caja y si el fondo va difuminado.
 * Reemplaza las pastillas «Tamaño / Mover / Logo» que estaban sobre la vitrina.
 */

/** Una pieza de la vitrina: columna, fila (de la cuadrícula) y tamaño (1 = normal). */
data class PiezaVitrina(val c: Int, val f: Int, val t: Float = 1f)

data class Disposicion(
    val logo: PiezaVitrina = PiezaVitrina(2, 2),
    val caja: PiezaVitrina = PiezaVitrina(7, 2),
    val fondo: PiezaVitrina = PiezaVitrina(4, 2),
    val conCaja: Boolean = true,
    val difuminado: Boolean = true,
) {
    fun pieza(k: Int) = when (k) { 0 -> logo; 1 -> caja; else -> fondo }
    fun con(k: Int, p: PiezaVitrina) = when (k) { 0 -> copy(logo = p); 1 -> copy(caja = p); else -> copy(fondo = p) }

    companion object {
        const val COLUMNAS = 9
        const val FILAS = 5
        val NOMBRES = listOf("Logo", "Caja 3D", "Imagen grande")
        /** Centro de la celda como fracción del ancho / alto de la pantalla. */
        fun fx(p: PiezaVitrina) = (p.c + 0.5f) / COLUMNAS
        fun fy(p: PiezaVitrina) = (p.f + 0.5f) / FILAS
        fun minimo(k: Int) = if (k == 2) 1f else 0.6f
        fun maximo(k: Int) = if (k == 2) 1.6f else 1.6f

        private fun PiezaVitrina.texto() = "$c,$f,$t"
        private fun pieza(s: String): PiezaVitrina? = s.split(",").takeIf { it.size == 3 }?.let {
            PiezaVitrina(it[0].toIntOrNull()?.coerceIn(0, COLUMNAS - 1) ?: return null, it[1].toIntOrNull()?.coerceIn(0, FILAS - 1) ?: return null, it[2].toFloatOrNull() ?: 1f)
        }

        /** Una línea por juego: clave<TAB>logo<TAB>caja<TAB>fondo<TAB>conCaja<TAB>difuminado. */
        fun escribir(m: Map<String, Disposicion>) = m.entries.joinToString("\n") { (k, d) ->
            listOf(k, d.logo.texto(), d.caja.texto(), d.fondo.texto(), if (d.conCaja) "1" else "0", if (d.difuminado) "1" else "0").joinToString("\t")
        }

        fun leer(texto: String): Map<String, Disposicion> = texto.lineSequence().mapNotNull { l ->
            val p = l.split("\t")
            if (p.size < 6) return@mapNotNull null
            p[0] to Disposicion(pieza(p[1]) ?: return@mapNotNull null, pieza(p[2]) ?: return@mapNotNull null, pieza(p[3]) ?: return@mapNotNull null, p[4] == "1", p[5] == "1")
        }.toMap()
    }
}

/** El borrador mientras se edita: [original] para «Volver a como estaba» y [sel] = pieza elegida (0 logo, 1 caja, 2 fondo). */
data class EdicionVitrina(val clave: String, val disp: Disposicion, val original: Disposicion, val sel: Int = 1)

/** Lo que se usa al no haber nada guardado para el juego: lo de siempre, con los tamaños generales que ya tenías. */
fun Diptico.disposicionDe(clave: String): Disposicion {
    val e = estado.value
    return e.extra.disposiciones[clave] ?: Disposicion(
        logo = PiezaVitrina(2, 2, e.extra.logoTam), caja = PiezaVitrina(7, 2, e.extra.cajaTam), difuminado = e.extra.fondoDifuminado,
    )
}

fun Diptico.abrirEditorVitrina(clave: String = estado.value.seleccionado?.clave.orEmpty()) {
    if (clave.isEmpty()) return
    val d = disposicionDe(clave)
    actualizar { it.copy(panel = Panel.NINGUNO, extra = it.extra.copy(rapidoGrupo = -1, rapidoSel = 0, edicionVitrina = EdicionVitrina(clave, d, d))) }
}

private fun Diptico.cambiarEdicion(f: (EdicionVitrina) -> EdicionVitrina) {
    actualizar { s: Estado ->
        val actual = s.extra.edicionVitrina
        if (actual == null) s else s.copy(extra = s.extra.copy(edicionVitrina = f(actual)))
    }
}

fun Diptico.elegirPiezaVitrina(k: Int) {
    cambiarEdicion { ed: EdicionVitrina -> ed.copy(sel = ((k % 3) + 3) % 3) }
}

fun Diptico.siguientePiezaVitrina(d: Int) {
    cambiarEdicion { ed: EdicionVitrina -> ed.copy(sel = ((ed.sel + d) % 3 + 3) % 3) }
}

/** Toca una celda: la pieza elegida va ahí. */
fun Diptico.ponerPiezaVitrina(c: Int, f: Int) { cambiarEdicion { ed: EdicionVitrina ->
    val p = ed.disp.pieza(ed.sel)
    ed.copy(disp = ed.disp.con(ed.sel, p.copy(c = c.coerceIn(0, Disposicion.COLUMNAS - 1), f = f.coerceIn(0, Disposicion.FILAS - 1))))
} }

/** Cruceta: corre la pieza elegida una celda. */
fun Diptico.moverPiezaVitrina(dc: Int, df: Int) {
    val ed = estado.value.extra.edicionVitrina ?: return
    val p = ed.disp.pieza(ed.sel)
    ponerPiezaVitrina(p.c + dc, p.f + df)
}

fun Diptico.tamPiezaVitrina(d: Float) { cambiarEdicion { ed: EdicionVitrina ->
    val p = ed.disp.pieza(ed.sel)
    val t = (Math.round((p.t + d) * 10f) / 10f).coerceIn(Disposicion.minimo(ed.sel), Disposicion.maximo(ed.sel))
    ed.copy(disp = ed.disp.con(ed.sel, p.copy(t = t)))
} }

fun Diptico.alternarCajaVitrina() { cambiarEdicion { ed: EdicionVitrina -> ed.copy(disp = ed.disp.copy(conCaja = !ed.disp.conCaja)) } }
fun Diptico.alternarDifuminadoVitrina() { cambiarEdicion { ed: EdicionVitrina -> ed.copy(disp = ed.disp.copy(difuminado = !ed.disp.difuminado)) } }
fun Diptico.volverDisposicionOriginal() { cambiarEdicion { ed: EdicionVitrina -> ed.copy(disp = ed.original) } }
fun Diptico.disposicionDeFabrica() { cambiarEdicion { ed: EdicionVitrina -> ed.copy(disp = Disposicion()) } }

/** «Listo»: guarda lo de este juego y cierra el editor. */
fun Diptico.guardarEditorVitrina() {
    val ed = estado.value.extra.edicionVitrina ?: return
    val m = estado.value.extra.disposiciones + (ed.clave to ed.disp)
    almacen.guardarTexto("vitrinaLugar", Disposicion.escribir(m))
    actualizar { it.copy(extra = it.extra.copy(disposiciones = m, edicionVitrina = null)) }
}

/** Para ver la vitrina mientras se edita sin tener la portada PS5: la deja puesta. */
fun Diptico.usarPortadaPs5() {
    almacen.guardarTexto("portadaArriba", "ps5")
    actualizar { it.copy(extra = it.extra.copy(portada = "ps5")) }
    if (estado.value.vista != "iconos") { almacen.guardarTexto("vista", "iconos"); actualizar { it.copy(vista = "iconos") } }
}

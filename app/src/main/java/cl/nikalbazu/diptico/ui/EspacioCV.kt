package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.estado.*

/*
 * Tu espacio (Widgets 3.0, boceto 10a–10d) con Colección viva:
 *  - arriba, tus páginas de widgets (empiezan vacías, DIPPO propone tres);
 *  - abajo, los accesos (Editar, Buscar, Fondo), las páginas y su tamaño, y la tienda por categorías;
 *  - con una pantalla, la tienda entra por el costado.
 * L2 y R2 pasan del inicio a Tu espacio y de vuelta (ya no hay botón grande de volver).
 */

/** Encabezado de Tu espacio (arriba): Inicio | Tu espacio, Editar, la tienda (solo con una pantalla), la hora y tu foto. */
@Composable
internal fun EncabezadoEspacioCV(e: Estado) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    @Composable
    fun Pildora(t: String, on: Boolean, desc: String? = null, accion: (() -> Unit)?) {
        Box(Modifier.clip(RoundedCornerShape(50)).background(if (on) p.tinta else p.tinta.copy(alpha = 0.08f))
            .border(esc.d(1), p.tinta.copy(alpha = if (on) 0f else 0.14f), RoundedCornerShape(50))
            .then(if (accion != null) Modifier.clickable(onClickLabel = desc, onClick = accion) else Modifier).padding(horizontal = esc.d(14), vertical = esc.d(7))) {
            Texto(t, 13.5f, color = if (on) p.fondo else p.tinta, lineas = 1)
        }
    }
    Row(Modifier.fillMaxWidth().testTag("encabezado-espacio"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
        // Las dos páginas de la tira, con la pista de los gatillos.
        Row(Modifier.clip(RoundedCornerShape(50)).background(p.tinta.copy(alpha = 0.06f)).padding(esc.d(3)), horizontalArrangement = Arrangement.spacedBy(esc.d(2))) {
            Pildora("Inicio", false, "Volver al inicio") { Diptico.fijarInicio("heroe") }
            Pildora("Tu espacio", true, "Página siguiente", if (e.paginas > 1) ({ Diptico.irAPagina((e.paginaInicio + 1) % e.paginas) }) else null)
        }
        Texto("L2 · R2", 11.5f, color = p.tenue, lineas = 1, traducir = false, modifier = Modifier.padding(start = esc.d(4)))
        if (e.paginas > 1) Texto("${e.paginaInicio + 1}/${e.paginas}", 12f, color = p.tenue, lineas = 1, traducir = false)
        Spacer(Modifier.weight(1f))
        if (!e.espacioVacio()) Pildora("Editar", false) { Diptico.editarWidgets() }
        Pildora(if (e.extra.galeria) "Cerrar la Galería" else "Galería de widgets", e.extra.galeria) {
            if (e.extra.galeria) Diptico.cerrarGaleriaWidgets() else Diptico.abrirGaleriaWidgets()
        }
        PerfilArriba(50f, conReloj = true)
    }
}

/** Tu espacio vacío: DIPPO invita y propone tres widgets según cómo usas la consola. */
@Composable
internal fun EspacioVacioCV(e: Estado, modifier: Modifier = Modifier) {
    val sugeridos = Diptico.sugeridosEspacio(e)
    LienzoBoceto(1920f, 900f, modifier.testTag("espacio-vacio"), zoom = if (e.unaPantalla) ZOOM_UNA else ZOOM_ARRIBA) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        Column(
            Modifier.align(Alignment.Center).width(esc.d(1180)).vidrio(44f, fuerte = true).padding(horizontal = esc.d(48), vertical = esc.d(36)),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(18)),
        ) {
            cl.nikalbazu.diptico.bisa.AvatarBisa(118f * esc.factor)
            TB("Este espacio es tuyo", 46f, titulo = true)
            TB("Empieza vacío. Según cómo usas tu consola, te propongo estos tres:", 24f, color = col.sub, lineas = 2)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                sugeridos.forEach { (tipo, porque) ->
                    Column(
                        Modifier.weight(1f).height(esc.d(160)).clip(RoundedCornerShape(esc.d(28))).background(col.texto.copy(alpha = .06f))
                            .border(1.dp, col.borde, RoundedCornerShape(esc.d(28))).clickable { Diptico.ponerEnEspacio(tipo) }.padding(esc.d(18)),
                        verticalArrangement = Arrangement.spacedBy(esc.d(8)),
                    ) {
                        Box(Modifier.size(esc.d(52)).clip(RoundedCornerShape(esc.d(16))).background(col.acento.copy(alpha = .35f)), contentAlignment = Alignment.Center) {
                            IconoDiptico(Widgets.icono(tipo), col.texto, 34f)
                        }
                        TB(Widgets.nombre(tipo), 23f, peso = FontWeight.ExtraBold)
                        TB(porque, 18f, color = col.sub, lineas = 2, traducir = false)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                BotonTecla("A", "Agregar los 3", { Diptico.agregarSugeridos() }, alto = 72f, principal = true, letra = 24f)
                BotonTecla("Y", "Abrir la Galería", { Diptico.abrirGaleriaWidgets() }, alto = 72f, letra = 24f)
            }
        }
    }
}

/**
 * La otra pantalla mientras estás en Tu espacio (boceto 12c): la tira de páginas, el acceso grande a la Galería de
 * widgets y los accesos de siempre. La Galería ya no va mezclada aquí: tiene su propia pantalla ([GaleriaAbajoCV]).
 */
@Composable
fun EspacioAbajoCV(e: Estado) {
    if (e.extra.galeria) { GaleriaAbajoCV(e); return }
    LienzoBoceto(1240f, 1080f, Modifier.testTag("espacio-abajo"), fondo = LocalColeccion.current.fondo, zoom = ZOOM_ABAJO) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(40), vertical = esc.d(28)), verticalArrangement = Arrangement.spacedBy(esc.d(18))) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                TB("Tu espacio", 44f, titulo = true)
                TB(nombreTamPagina(e.extra.paginaAncho, e.extra.paginaAlto), 19f, color = col.sub, modifier = Modifier.padding(bottom = esc.d(6)))
            }
            TiraPaginas(e, Modifier.fillMaxWidth().height(esc.d(140)))
            // El acceso grande a la Galería.
            Row(
                Modifier.fillMaxWidth().weight(1.2f).clip(RoundedCornerShape(esc.d(44)))
                    .background(Brush.linearGradient(listOf(col.acento, col.acento.copy(alpha = .55f), col.fondo)))
                    .border(1.dp, col.borde, RoundedCornerShape(esc.d(44)))
                    .clickable(onClickLabel = "Abrir la Galería de widgets") { Diptico.abrirGaleriaWidgets() }
                    .testTag("abrir-galeria").padding(horizontal = esc.d(36)),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(28)),
            ) {
                MosaicoGaleria(Modifier.size(esc.d(170), esc.d(120)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    TB("Galería de widgets", 40f, titulo = true)
                    TB("${totalGaleria()} widgets para tu espacio. Lo que eliges va a la página que estás mirando.", 20f, color = col.texto.copy(alpha = .85f), lineas = 2)
                }
                PistaTecla("Y", "Abrir")
            }
            Row(Modifier.fillMaxWidth().weight(.8f), horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                AccesoEspacio("Editar", "Mover, estirar y cambiar cada widget", "X", Modifier.weight(1f)) { Diptico.editarWidgets() }
                TamanoPagina(e, Modifier.weight(1f))
                AccesoEspacio("Fondo", "Una imagen tuya detrás", null, Modifier.weight(1f)) { Diptico.pedirArchivoPara("fondoInicio", "image/*") }
            }
            Row(
                Modifier.fillMaxWidth().height(esc.d(100)).vidrio(32f).padding(horizontal = esc.d(22)),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(18)),
            ) {
                cl.nikalbazu.diptico.bisa.AvatarBisa(64f * esc.factor)
                TB(ideaEspacio(e), 20f, peso = FontWeight.ExtraBold, lineas = 2, modifier = Modifier.weight(1f))
            }
            PistasFinas("L2 R2" to "Página", "Y" to "Galería", "X" to "Editar", "B" to "Volver al inicio")
        }
    }
}

/** Lo que DIPPO dice abajo según la página que estás mirando. */
private fun ideaEspacio(e: Estado): String = when {
    e.espacioVacio() -> "Tu espacio está vacío: A agrega los tres que te propongo, o abre la Galería con Y."
    e.widgets.none { it.visible && it.pagina == e.paginaInicio } -> "Esta página está vacía. Abre la Galería y elige uno."
    else -> "Mantén presionado un widget para cambiar lo que muestra."
}

/** El dibujito del acceso: unos widgets de colores acomodados. */
@Composable
private fun MosaicoGaleria(modifier: Modifier) {
    val col = LocalColeccion.current
    androidx.compose.foundation.Canvas(modifier) {
        val g = size.width * .05f
        val cw = (size.width - 2 * g) / 3f; val ch = (size.height - g) / 2f
        val r = androidx.compose.ui.geometry.CornerRadius(cw * .22f)
        fun caja(x: Int, y: Int, an: Int, c: Color) = drawRoundRect(c, androidx.compose.ui.geometry.Offset(x * (cw + g), y * (ch + g)),
            androidx.compose.ui.geometry.Size(an * cw + (an - 1) * g, ch), r)
        caja(0, 0, 2, col.texto.copy(alpha = .9f)); caja(2, 0, 1, Coral); caja(0, 1, 1, Color(0xFF2EC4A6)); caja(1, 1, 2, col.texto.copy(alpha = .35f))
    }
}

/** Las páginas en fila: el inicio, cada página de widgets y «+ Página» (L2/R2 pasan de una a otra). */
@Composable
private fun TiraPaginas(e: Estado, modifier: Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Row(modifier.vidrio(36f).padding(horizontal = esc.d(18), vertical = esc.d(16)), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
        PistaTecla("L2", "")
        Row(Modifier.weight(1f).fillMaxHeight().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
            val paginas = listOf("Inicio" to "tu colección") + (0 until e.paginas).map { p ->
                val n = e.widgets.count { it.visible && it.pagina == p }
                "Pág ${p + 1}" to (if (n == 0) "vacía" else "$n widgets")
            }
            paginas.forEachIndexed { k, (t, nota) ->
                val on = k == e.paginaInicio + 1
                Column(
                    Modifier.width(esc.d(170)).fillMaxHeight().clip(RoundedCornerShape(esc.d(26)))
                        .background(if (on) col.elegido else col.texto.copy(alpha = .05f))
                        .border(1.dp, if (on) Color.Transparent else col.borde, RoundedCornerShape(esc.d(26)))
                        .clickable { if (k == 0) Diptico.fijarInicio("heroe") else Diptico.irAPagina(k - 1) },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    TB(t, 23f, titulo = true, color = if (on) col.enElegido else col.texto)
                    TB(nota, 15f, color = if (on) col.enElegido.copy(alpha = .7f) else col.sub, peso = FontWeight.ExtraBold)
                }
            }
            if (e.paginas < 12) Column(
                Modifier.width(esc.d(120)).fillMaxHeight().clip(RoundedCornerShape(esc.d(26)))
                    .border(2.dp, col.texto.copy(alpha = .35f), RoundedCornerShape(esc.d(26)))
                    .clickable(onClickLabel = "Agregar página") { Diptico.agregarPaginaEspacio() },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) { TB("+", 34f, titulo = true); TB("Página", 15f, peso = FontWeight.ExtraBold) }
        }
        PistaTecla("R2", "")
    }
}

/** El tamaño de la página que estás mirando: ancho y alto en pantallas (1 a 3), y quitarla. */
@Composable
private fun TamanoPagina(e: Estado, modifier: Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    @Composable
    fun Mini(t: String, desc: String, activo: Boolean, accion: () -> Unit) {
        Box(Modifier.size(esc.d(46)).clip(CircleShape).background(col.texto.copy(alpha = if (activo) .12f else .04f))
            .clickable(enabled = activo, onClickLabel = desc, onClick = accion), contentAlignment = Alignment.Center) {
            TB(t, 22f, color = if (activo) col.texto else col.tenue, peso = FontWeight.ExtraBold, traducir = false)
        }
    }
    Column(modifier.fillMaxHeight().vidrio(34f).padding(horizontal = esc.d(18), vertical = esc.d(14)), verticalArrangement = Arrangement.spacedBy(esc.d(8), Alignment.CenterVertically)) {
        TB("Tamaño", 26f, titulo = true)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
            TB("Ancho ${e.extra.paginaAncho}", 17f, color = col.sub, peso = FontWeight.ExtraBold, modifier = Modifier.weight(1f), traducir = false)
            Mini("−", "Menos ancho", e.extra.paginaAncho > 1) { Diptico.cambiarTamPagina(-1, 0) }
            Mini("+", "Más ancho", e.extra.paginaAncho < 3) { Diptico.cambiarTamPagina(1, 0) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
            TB("Alto ${e.extra.paginaAlto}", 17f, color = col.sub, peso = FontWeight.ExtraBold, modifier = Modifier.weight(1f), traducir = false)
            Mini("−", "Menos alto", e.extra.paginaAlto > 1) { Diptico.cambiarTamPagina(0, -1) }
            Mini("+", "Más alto", e.extra.paginaAlto < 3) { Diptico.cambiarTamPagina(0, 1) }
        }
        if (e.paginas > 1) TB("Quitar esta página", 15f, color = Color(0xFFFF8A8A), peso = FontWeight.ExtraBold,
            modifier = Modifier.clickable { Diptico.quitarPaginaEspacio() })
    }
}

/** Galería de widgets abajo (boceto 12b): su propia pantalla, con categorías y los widgets en grande. */
@Composable
fun GaleriaAbajoCV(e: Estado) {
    LienzoBoceto(1240f, 1080f, Modifier.testTag("galeria-abajo"), fondo = LocalColeccion.current.fondo, zoom = ZOOM_ABAJO) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(40), vertical = esc.d(28)), verticalArrangement = Arrangement.spacedBy(esc.d(16))) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                BotonRedondo(Icono.CERRAR, 64f, { Diptico.cerrarGaleriaWidgets() }, descripcion = "Volver a Tu espacio")
                TB("Galería de widgets", 44f, titulo = true, modifier = Modifier.weight(1f))
                TB("Va a: Pág ${e.paginaInicio + 1}", 20f, color = col.sub, peso = FontWeight.ExtraBold)
            }
            ListaGaleria(e, Modifier.fillMaxWidth().weight(1f), columnas = if (e.unaPantalla) 1 else 2)
            PistasFinas("L1 R1" to "Categoría", "X" to "Tamaño", "A" to "Agregar", "B" to "Volver a Tu espacio")
        }
    }
}

/** Con una pantalla: la Galería entra por el costado de Tu espacio. */
@Composable
internal fun TiendaLadoCV(e: Estado, modifier: Modifier) {
    LienzoBoceto(760f, 1080f, modifier.testTag("tienda-lado"), zoom = ZOOM_UNA) {
        val esc = LocalEscala.current
        Column(Modifier.fillMaxSize().padding(esc.d(18)).vidrio(40f, fuerte = true).padding(esc.d(24)), verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                BotonRedondo(Icono.CERRAR, 56f, { Diptico.cerrarGaleriaWidgets() }, descripcion = "Cerrar la Galería")
                TB("Galería de widgets", 36f, titulo = true)
            }
            ListaGaleria(e, Modifier.fillMaxWidth().weight(1f), columnas = 1)
            TamanosGaleria(e)
        }
    }
}

/** Los tamaños para el widget elegido (X los pasa). */
@Composable
private fun TamanosGaleria(e: Estado) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val tipo = tiposTiendaEspacio(e.extra.tiendaCat).getOrNull(e.extra.espacioSel) ?: return
    val tams = tamanosGaleria(e, tipo)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
        TB("Tamaño", 20f, color = col.sub, peso = FontWeight.ExtraBold)
        tams.forEachIndexed { i, (an, al) ->
            val on = i == e.extra.galeriaTam.coerceIn(0, tams.size - 1)
            Box(Modifier.height(esc.d(54)).clip(CircleShape).background(if (on) col.elegido else col.texto.copy(alpha = .08f))
                .clickable { Diptico.elegirTamGaleria(i) }.padding(horizontal = esc.d(20)), contentAlignment = Alignment.Center) {
                TB("$an × $al", 20f, color = if (on) col.enElegido else col.texto, peso = FontWeight.ExtraBold, traducir = false)
            }
        }
    }
}

/**
 * Galería arriba (boceto 12a), con dos pantallas: el widget elegido en grande (de verdad, con tus datos) en el tamaño
 * que elegiste, y un mapa chico de la página que muestra dónde va a quedar.
 */
@Composable
fun GaleriaArribaCV(e: Estado, modifier: Modifier = Modifier) {
    val escApp = LocalEscala.current
    val tipos = tiposTiendaEspacio(e.extra.tiendaCat)
    val tipo = tipos.getOrNull(e.extra.espacioSel) ?: return
    val tams = tamanosGaleria(e, tipo)
    val (an, al) = tams.getOrNull(e.extra.galeriaTam.coerceIn(0, (tams.size - 1).coerceAtLeast(0))) ?: (2 to 2)
    val w = e.widgets.firstOrNull { it.tipo == tipo }
    LienzoBoceto(1920f, 900f, modifier.testTag("galeria-arriba"), zoom = ZOOM_ARRIBA) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        Row(Modifier.fillMaxSize().padding(horizontal = esc.d(80), vertical = esc.d(30)), horizontalArrangement = Arrangement.spacedBy(esc.d(60))) {
            Column(Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(18))) {
                TB("GALERÍA DE WIDGETS · ${CATEGORIAS_WIDGETS.getOrElse(e.extra.tiendaCat) { "" }.uppercase()}", 18f, color = col.sub, peso = FontWeight.ExtraBold, espaciado = 3f)
                TB(Widgets.nombre(tipo), 66f, titulo = true)
                TB(Widgets.descripcion(tipo), 23f, color = col.sub, lineas = 2)
                // El widget de verdad, en el tamaño elegido.
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    val unidad = 150f
                    Box(
                        Modifier.size(esc.d(unidad * an.coerceAtMost(4)), esc.d(unidad * al.coerceAtMost(3)))
                            .clip(RoundedCornerShape(esc.d(34))).vidrio(34f, fuerte = true),
                    ) {
                        if (w != null) androidx.compose.runtime.CompositionLocalProvider(LocalEscala provides escApp) {
                            ContenidoWidget(e, w.copy(an = an, al = al), an, al)
                        }
                    }
                }
                TamanosGaleria(e)
            }
            Column(Modifier.weight(.9f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(18))) {
                Column(Modifier.fillMaxWidth().weight(1f).vidrio(40f).padding(esc.d(28)), verticalArrangement = Arrangement.spacedBy(esc.d(16))) {
                    TB(if (w?.visible == true) "Ya está en la Pág ${w.pagina + 1}" else "Va a la Pág ${e.paginaInicio + 1}", 32f, titulo = true)
                    MapaPagina(e, tipo, an, al, Modifier.fillMaxWidth().weight(1f))
                    if (w?.visible != true && !cabeEnPagina(e, tipo, an, al)) TB("En esta página no cabe de ese tamaño: va a otra página.", 18f, color = Color(0xFFFFD29A), peso = FontWeight.ExtraBold, lineas = 2)
                    TB("Después lo mueves o estiras desde Editar.", 18f, color = col.sub)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    BotonTecla("A", if (w?.visible == true) "Ir a su página" else "Agregar a mi espacio", { Diptico.agregarDesdeGaleria() }, alto = 80f, principal = true, letra = 26f)
                }
            }
        }
    }
}

/** ¿Cabe el widget de [an]×[al] en algún hueco de la página que estás mirando? */
private fun cabeEnPagina(e: Estado, tipo: String, an: Int, al: Int): Boolean {
    val cols = Widgets.COLUMNAS.coerceAtLeast(1); val filas = Widgets.FILAS.coerceAtLeast(1)
    val enPagina = e.widgets.filter { it.visible && it.pagina == e.paginaInicio && it.tipo != tipo }
    for (y in 0..filas - al) for (x in 0..cols - an) {
        if (enPagina.none { o -> x < o.x + o.an && o.x < x + an && y < o.y + o.al && o.y < y + al }) return true
    }
    return false
}

/** El mapa de la página: los widgets que ya están y, punteado, dónde quedaría el nuevo. */
@Composable
private fun MapaPagina(e: Estado, tipo: String, an: Int, al: Int, modifier: Modifier) {
    val col = LocalColeccion.current
    val cols = Widgets.COLUMNAS.coerceAtLeast(1); val filas = Widgets.FILAS.coerceAtLeast(1)
    val enPagina = e.widgets.filter { it.visible && it.pagina == e.paginaInicio && it.tipo != tipo }
    // El primer hueco donde cabe (igual que al agregarlo).
    val hueco = run {
        for (y in 0..filas - al) for (x in 0..cols - an) {
            val libre = enPagina.none { o -> x < o.x + o.an && o.x < x + an && y < o.y + o.al && o.y < y + al }
            if (libre) return@run x to y
        }
        null
    }
    androidx.compose.foundation.Canvas(modifier) {
        val g = 8f
        val cw = (size.width - g * (cols - 1)) / cols; val ch = (size.height - g * (filas - 1)) / filas
        fun rect(x: Int, y: Int, w: Int, h: Int) = androidx.compose.ui.geometry.Offset(x * (cw + g), y * (ch + g)) to
            androidx.compose.ui.geometry.Size(w * cw + (w - 1) * g, h * ch + (h - 1) * g)
        val r = androidx.compose.ui.geometry.CornerRadius(14f)
        for (o in enPagina) { val (p, s) = rect(o.x, o.y, o.an.coerceAtMost(cols - o.x), o.al.coerceAtMost(filas - o.y)); drawRoundRect(col.texto.copy(alpha = .16f), p, s, r) }
        if (hueco != null) {
            val (p, s) = rect(hueco.first, hueco.second, an, al)
            drawRoundRect(col.acento.copy(alpha = .35f), p, s, r)
            drawRoundRect(col.texto, p, s, r, style = androidx.compose.ui.graphics.drawscope.Stroke(4f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
        }
    }
}

@Composable
private fun AccesoEspacio(t: String, nota: String, tecla: String?, modifier: Modifier, accion: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Column(modifier.fillMaxHeight().vidrio(34f).clickable(onClick = accion).padding(horizontal = esc.d(22)),
        verticalArrangement = Arrangement.spacedBy(esc.d(8), Alignment.CenterVertically)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
            if (tecla != null) PistaTecla(tecla, "")
            TB(t, 28f, titulo = true)
        }
        TB(nota, 17f, color = col.sub, lineas = 2)
    }
}

/** La lista de la Galería: categorías (L1/R1) y widgets (cruceta, A agrega). Sin entrar al editor. */
@Composable
private fun ListaGaleria(e: Estado, modifier: Modifier, columnas: Int) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val tipos = tiposTiendaEspacio(e.extra.tiendaCat)
    val sel = e.extra.espacioSel.coerceIn(0, (tipos.size - 1).coerceAtLeast(0))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(esc.d(16))) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
            CATEGORIAS_WIDGETS.forEachIndexed { i, n ->
                val on = i == e.extra.tiendaCat
                Box(Modifier.height(esc.d(54)).clip(CircleShape).background(if (on) col.elegido else col.texto.copy(alpha = .06f))
                    .border(1.dp, col.borde, CircleShape).clickable { Diptico.elegirCategoriaEspacio(i) }.padding(horizontal = esc.d(22)),
                    contentAlignment = Alignment.Center) { TB(n, 20f, color = if (on) col.enElegido else col.texto, peso = FontWeight.ExtraBold) }
            }
        }
        val grilla = rememberLazyGridState()
        LaunchedEffect(sel, e.extra.tiendaCat) { if (tipos.isNotEmpty()) grilla.animateScrollToItem((sel - columnas).coerceAtLeast(0)) }
        LazyVerticalGrid(GridCells.Fixed(columnas), Modifier.fillMaxWidth().weight(1f), state = grilla,
            verticalArrangement = Arrangement.spacedBy(esc.d(14)), horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
            items(tipos.size) { k ->
                val tipo = tipos[k]
                val on = k == sel
                val puesto = e.widgets.any { it.tipo == tipo && it.visible }
                val w = e.widgets.firstOrNull { it.tipo == tipo }
                Row(
                    Modifier.height(esc.d(170)).clip(RoundedCornerShape(esc.d(28)))
                        .background(if (on) col.texto.copy(alpha = .10f) else col.texto.copy(alpha = .04f))
                        .border(if (on) 3.dp else 1.dp, if (on) col.texto else col.borde, RoundedCornerShape(esc.d(28)))
                        .clickable { if (on) Diptico.agregarDesdeGaleria() else Diptico.actualizar { it.copy(extra = it.extra.copy(espacioSel = k, galeriaTam = 0)) } }
                        .padding(esc.d(16)),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16)),
                ) {
                    Box(Modifier.size(esc.d(120)).clip(RoundedCornerShape(esc.d(24))).background(col.acento.copy(alpha = .30f)), contentAlignment = Alignment.Center) {
                        IconoDiptico(Widgets.icono(tipo), col.texto, 56f, acento = Coral)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        TB(Widgets.nombre(tipo), 24f, peso = FontWeight.ExtraBold, lineas = 1)
                        TB(Widgets.descripcion(tipo), 17f, color = col.sub, lineas = 2)
                        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8)), verticalAlignment = Alignment.CenterVertically) {
                            if (w != null) Box(Modifier.clip(CircleShape).background(col.texto.copy(alpha = .08f)).padding(horizontal = esc.d(12), vertical = esc.d(4))) {
                                TB("${w.an} × ${w.al}", 15f, peso = FontWeight.ExtraBold, traducir = false)
                            }
                            if (puesto) TB("En Tu espacio ✓", 15f, color = Color(0xFF7FE0A8), peso = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

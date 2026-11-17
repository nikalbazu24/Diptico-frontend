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
        if (e.unaPantalla) Pildora(if (e.extra.tiendaLado) "Cerrar tienda" else "Tienda", e.extra.tiendaLado) { Diptico.alternarTiendaLado() }
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
                if (e.unaPantalla) BotonTecla("X", "Elegir en la tienda", { Diptico.alternarTiendaLado() }, alto = 72f, letra = 24f)
            }
            if (!e.unaPantalla) TB("La tienda completa está en la otra pantalla.", 19f, color = col.tenue)
        }
    }
}

/** La otra pantalla mientras estás en Tu espacio: accesos, páginas y la tienda (sin botón grande de volver: L2/R2 o B). */
@Composable
fun EspacioAbajoCV(e: Estado) {
    LienzoBoceto(1240f, 1080f, Modifier.testTag("espacio-abajo"), fondo = LocalColeccion.current.fondo, zoom = ZOOM_ABAJO) {
        val esc = LocalEscala.current
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(40), vertical = esc.d(30)), verticalArrangement = Arrangement.spacedBy(esc.d(18))) {
            Row(Modifier.fillMaxWidth().height(esc.d(130)), horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                AccesoEspacio("Editar", Icono.LAPIZ, Modifier.weight(1f)) { Diptico.editarWidgets() }
                AccesoEspacio("Buscar", Icono.BUSCAR, Modifier.weight(1f)) { Diptico.abrirBuscar() }
                AccesoEspacio("Fondo", Icono.PORTADA, Modifier.weight(1f)) { Diptico.pedirArchivoPara("fondoInicio", "image/*") }
            }
            PaginasEspacio(e, Modifier.fillMaxWidth().height(esc.d(150)))
            TiendaEspacioCV(e, Modifier.fillMaxWidth().weight(1f), columnas = 2)
        }
    }
}

/** Con una pantalla: la tienda al costado de Tu espacio. */
@Composable
internal fun TiendaLadoCV(e: Estado, modifier: Modifier) {
    LienzoBoceto(760f, 1080f, modifier.testTag("tienda-lado"), zoom = ZOOM_UNA) {
        val esc = LocalEscala.current
        Column(Modifier.fillMaxSize().padding(esc.d(18)).vidrio(40f, fuerte = true).padding(esc.d(24)), verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
            PaginasEspacio(e, Modifier.fillMaxWidth().height(esc.d(150)))
            TiendaEspacioCV(e, Modifier.fillMaxWidth().weight(1f), columnas = 1, conMarco = false)
        }
    }
}

@Composable
private fun AccesoEspacio(t: String, icono: Icono, modifier: Modifier, accion: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Column(modifier.fillMaxHeight().vidrio(34f).clickable(onClick = accion), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(esc.d(10), Alignment.CenterVertically)) {
        IconoDiptico(icono, col.texto, 44f, acento = Coral)
        TB(t, 21f, peso = FontWeight.ExtraBold)
    }
}

/** Las páginas de Tu espacio: cuál ves, agregar o quitar una, y cuánto mide (hasta 3 pantallas de ancho y de alto). */
@Composable
private fun PaginasEspacio(e: Estado, modifier: Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    @Composable
    fun Mini(t: String, desc: String, activo: Boolean = true, accion: () -> Unit) {
        Box(Modifier.size(esc.d(50)).clip(CircleShape).background(col.texto.copy(alpha = if (activo) .10f else .04f))
            .clickable(enabled = activo, onClickLabel = desc, onClick = accion), contentAlignment = Alignment.Center) {
            TB(t, 22f, color = if (activo) col.texto else col.tenue, peso = FontWeight.ExtraBold, traducir = false)
        }
    }
    Column(modifier.vidrio(34f).padding(horizontal = esc.d(20), vertical = esc.d(14)), verticalArrangement = Arrangement.spacedBy(esc.d(10), Alignment.CenterVertically)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
            TB("Página ${e.paginaInicio + 1} de ${e.paginas}", 22f, titulo = true, modifier = Modifier.weight(1f))
            Mini("‹", "Página anterior", e.paginaInicio > 0) { Diptico.cambiarPagina(-1) }
            Mini("›", "Página siguiente", e.paginaInicio < e.paginas - 1) { Diptico.cambiarPagina(1) }
            Mini("+", "Agregar página", e.paginas < 12) { Diptico.agregarPaginaEspacio() }
            Mini("−", "Quitar esta página", e.paginas > 1) { Diptico.quitarPaginaEspacio() }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
            TB(nombreTamPagina(e.extra.paginaAncho, e.extra.paginaAlto), 18f, color = col.sub, modifier = Modifier.weight(1f))
            TB("Ancho", 16f, color = col.tenue)
            Mini("−", "Menos ancho", e.extra.paginaAncho > 1) { Diptico.cambiarTamPagina(-1, 0) }
            Mini("+", "Más ancho", e.extra.paginaAncho < 3) { Diptico.cambiarTamPagina(1, 0) }
            TB("Alto", 16f, color = col.tenue)
            Mini("−", "Menos alto", e.extra.paginaAlto > 1) { Diptico.cambiarTamPagina(0, -1) }
            Mini("+", "Más alto", e.extra.paginaAlto < 3) { Diptico.cambiarTamPagina(0, 1) }
        }
    }
}

/** La tienda de Tu espacio: categorías (L1/R1) y widgets (cruceta, A agrega). Sin entrar al editor. */
@Composable
private fun TiendaEspacioCV(e: Estado, modifier: Modifier, columnas: Int, conMarco: Boolean = true) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val tipos = tiposTiendaEspacio(e.extra.tiendaCat)
    val sel = e.extra.espacioSel.coerceIn(0, (tipos.size - 1).coerceAtLeast(0))
    Column(modifier.then(if (conMarco) Modifier.vidrio(40f).padding(esc.d(26)) else Modifier), verticalArrangement = Arrangement.spacedBy(esc.d(16))) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
            TB("Tienda de widgets", 38f, titulo = true)
            TB("${tipos.size} en esta categoría", 18f, color = col.sub)
        }
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
                        .clickable { if (on) Diptico.ponerEnEspacio(tipo) else Diptico.actualizar { it.copy(extra = it.extra.copy(espacioSel = k)) } }
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
        PistasFinas("L1 R1" to "Categoría", "A" to "Agregar", "Y" to "Tus widgets", "L2 R2" to "Inicio")
    }
}

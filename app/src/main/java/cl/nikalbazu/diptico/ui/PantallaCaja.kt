package cl.nikalbazu.diptico.ui

import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import cl.nikalbazu.diptico.bisa.AvatarBisa
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.*

private val Verde = Color(0xFF23C486)
private val Ambar = Color(0xFFFFB454)

/** Las acciones de la pestaña Caras (casillas 4 a 7). */
private val ACCIONES_CARA = listOf(
    Triple("Buscar arte", "Comunidad, ScreenScraper y TheGamesDB", Icono.BUSCAR),
    Triple("Importar imagen", "Una foto o escaneo para esta cara", Icono.CARPETA),
    Triple("Restaurar cara", "Vuelve a la de Díptico", Icono.BORRAR),
    Triple("Armar con una foto", "DIPPO corta la caja abierta", Icono.BISA),
)

/**
 * Estudio de Cajas: la caja 3D de un juego, a gusto. Con dos pantallas, arriba la caja se inspecciona (gira, se ve de
 * frente, de lomo, de reverso o se abre) y abajo están las pestañas Caras, Forma, Acabado y Variantes. Con una
 * pantalla, caja y controles van lado a lado. Todo lo que se cambia se ve al tiro y queda guardado en el equipo.
 */
@Composable
fun PantallaCaja(e: Estado, rol: Rol) {
    val clave = e.extra.cajaJuego
    if (clave.isEmpty()) return
    val j = e.juegosPorClave[clave] ?: return
    val c = Consolas.porId(j.consolaId) ?: return
    val esc = LocalEscala.current
    val dividido = !e.unaPantalla && e.dosDisponibles
    if (rol == Rol.INFERIOR && e.unaPantalla) return
    val muestraCaja = rol == Rol.SUPERIOR || rol == Rol.UNICA || (rol == Rol.INFERIOR && !dividido)
    val muestraControles = rol == Rol.INFERIOR || rol == Rol.UNICA
    val g = e.extra.caras[clave]
    Box(
        Modifier.fillMaxSize().testTag("pantalla-caja")
            .background(Brush.radialGradient(listOf(lerpColor(c.color, Color(0xFF12101F), 0.72f), Color(0xFF080912)), radius = 1600f))
            .clickable(remember { MutableInteractionSource() }, indication = null) { },
    ) {
        Row(Modifier.fillMaxSize().padding(esc.d(16)), horizontalArrangement = Arrangement.spacedBy(esc.d(16)), verticalAlignment = Alignment.CenterVertically) {
            if (muestraCaja) {
                Box(Modifier.weight(if (muestraControles) 0.85f else 1f).fillMaxHeight()) {
                    InspeccionCaja(e, j, c, g, grande = !muestraControles)
                }
            }
            if (muestraControles) {
                if (!muestraCaja && coleccionVivaActiva(e)) Box(Modifier.weight(1f).fillMaxHeight()) { PanelEstudioCV(e, clave, g) }
                else Column(Modifier.weight(if (muestraCaja) 1.15f else 1f).widthIn(max = esc.d(640)).fillMaxHeight()) {
                    PanelCajas(e, clave, g)
                }
            }
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)

/** La caja grande que se inspecciona: gira sola o se queda de frente, de lomo o de reverso; con X se abre. */
@Composable
private fun InspeccionCaja(e: Estado, j: cl.nikalbazu.diptico.data.Juego, c: cl.nikalbazu.diptico.data.Consola, g: CarasGuardadas?, grande: Boolean) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val vista = e.extra.estudioVista
    val gira = rememberInfiniteTransition(label = "estudio-gira").animateFloat(
        -34f, 34f, infiniteRepeatable(tween(5600, easing = LinearEasing), RepeatMode.Reverse), label = "y",
    )
    val fijo by animateFloatAsState(when (vista) { 2 -> 82f; 3 -> 180f; else -> 26f }, spring(dampingRatio = 0.8f, stiffness = 120f), label = "giro")
    val abierta by animateFloatAsState(if (e.extra.estudioAbierta) 1f else 0f, tween(620), label = "tapa")
    val caja: @Composable (Modifier) -> Unit = { m ->
        Caja3D(
            datosCaja(e, j, c), j, c, m,
            giroY = { if (vista == 0) gira.value else fijo }, giroX = { -7f }, caras = g, apertura = { abierta },
        )
    }
    val pastillas: @Composable (Modifier) -> Unit = { m ->
        Row(m.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
            listOf("Girar", "Frente", "Lomo", "Reverso").forEachIndexed { i, t ->
                Pastilla(t, vista == i && !e.extra.estudioAbierta, grande) { Diptico.vistaEstudio(i) }
            }
            Pastilla(if (e.extra.estudioAbierta) "Cerrar" else "Abrir", e.extra.estudioAbierta, grande) { Diptico.alternarAperturaEstudio() }
        }
    }
    if (grande) {
        // Con la pantalla entera para la caja: los datos a la izquierda y la caja a la derecha, sin taparse.
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(0.34f).fillMaxHeight().padding(esc.d(6)), verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
                Etiqueta(c.bonito, Coral, 11f)
                Etiqueta("Estudio de Cajas", p.tenue, 10f)
                Texto(j.titulo, 24f, lineas = 3)
                Detalle(
                    (EstudioCajas.plantilla(g?.plantilla)?.nombre ?: "Forma de la consola") + " · " + EstudioCajas.nombreMaterial(g?.material),
                    12.5f, lineas = 2,
                )
                if (g?.tiene == true && g.caras().none(CajasLocales::esRemota)) SelloGuardada()
                Spacer(Modifier.weight(1f))
                Detalle("Select: cambiar vista\nX: abrir la caja\nY: jugar ya", 11.5f, color = p.tenue)
            }
            Column(Modifier.weight(0.66f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                caja(Modifier.weight(1f).fillMaxWidth())
                pastillas(Modifier.padding(bottom = esc.d(4)))
            }
        }
    } else {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            caja(Modifier.weight(1f).fillMaxWidth())
            pastillas(Modifier.padding(bottom = esc.d(2)))
        }
    }
}

/** El sello que dice que la caja ya no depende de internet. */
@Composable
private fun SelloGuardada() {
    val esc = LocalEscala.current
    Row(
        Modifier.clip(CircleShape).background(Verde.copy(alpha = 0.16f)).border(esc.d(1), Verde.copy(alpha = 0.5f), CircleShape)
            .padding(horizontal = esc.d(10), vertical = esc.d(3)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconoDiptico(Icono.CHECK, Color(0xFF7BE7B9), 12f)
        Spacer(Modifier.width(esc.d(5)))
        Texto("Guardada en el equipo · se ve sin internet", 11f, color = Color(0xFF7BE7B9), lineas = 1)
    }
}

@Composable
private fun Pastilla(texto: String, activa: Boolean, grande: Boolean, alTocar: () -> Unit) {
    val esc = LocalEscala.current
    Box(
        Modifier.clip(CircleShape).then(if (activa) Modifier.fondoBoton(Uva) else Modifier.background(Color(0xBF141422)))
            .border(esc.d(1), if (activa) Color.White else Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable { alTocar() }.padding(horizontal = esc.d(if (grande) 12 else 9), vertical = esc.d(if (grande) 6 else 5)),
    ) { Texto(texto, if (grande) 12f else 10.5f, color = if (activa) SobreUva else Color(0xFFD9D7EA), lineas = 1) }
}

/** El panel de abajo (o de la derecha): DIPPO, pestañas y lo de cada pestaña. */
@Composable
private fun PanelCajas(e: Estado, clave: String, g: CarasGuardadas?) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val tab = e.extra.estudioTab
    val variantes = EstudioCajas.variantes(e.extra.caras, clave)
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        // DIPPO le da voz al Estudio: cuenta qué tiene la caja y qué se puede hacer.
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarBisa(34f, cara = if (g?.frente != null && g.reverso != null) CaraDippo.FELIZ else CaraDippo.NORMAL)
            Spacer(Modifier.width(esc.d(8)))
            Column(Modifier.weight(1f)) {
                Texto("Estudio de Cajas", 17f, lineas = 1)
                Detalle(EstudioCajas.comentario(g, tab, variantes.size), 11.5f, lineas = 2)
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(12))).background(p.fondo2.copy(alpha = 0.8f)).padding(esc.d(3)),
            horizontalArrangement = Arrangement.spacedBy(esc.d(3)),
        ) {
            EstudioCajas.PESTANAS.forEachIndexed { i, t ->
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(esc.d(9))).then(if (tab == i) Modifier.fondoBoton(Uva) else Modifier)
                        .clickable { Diptico.elegirPestanaCajas(i) }.padding(vertical = esc.d(7)),
                    contentAlignment = Alignment.Center,
                ) { Texto(t, 13f, color = if (tab == i) SobreUva else p.tenue, lineas = 1) }
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
            when (tab) {
                0 -> PestanaCaras(e, g)
                1 -> PestanaForma(e, g)
                2 -> PestanaAcabado(e, g)
                else -> PestanaVariantes(e, g, variantes)
            }
        }
        Pistas("L1/R1" to "Pestaña", "A" to "Elegir", "Y" to "Jugar", "B" to "Volver")
    }
}

/** Una casilla del Estudio: [foco] = donde está el mando; [elegida] = lo que está en uso. */
@Composable
private fun Modifier.casilla(foco: Boolean, elegida: Boolean, alTocar: () -> Unit): Modifier {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val forma = RoundedCornerShape(esc.d(12))
    return this.clip(forma)
        .background(if (elegida) Uva.copy(alpha = 0.22f) else p.fondo2.copy(alpha = 0.75f))
        .border(esc.d(if (foco) 2.5f else 1f), if (foco) Color.White else if (elegida) Uva else Color.White.copy(alpha = 0.06f), forma)
        .clickable { alTocar() }
}

@Composable
private fun PestanaCaras(e: Estado, g: CarasGuardadas?) {
    val esc = LocalEscala.current
    val sel = e.extra.cajaSel
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
        EstudioCajas.CARAS.forEachIndexed { i, nombre ->
            val valor = when (i) { 0 -> g?.frente; 1 -> g?.lomo; 2 -> g?.reverso; else -> g?.disco }
            Column(Modifier.weight(1f).casilla(sel == i, e.extra.estudioCara == i) { Diptico.usarEstudio(i) }.padding(esc.d(7)), horizontalAlignment = Alignment.CenterHorizontally) {
                MiniCara(valor, if (i == 0) e.juegosPorClave[e.extra.cajaJuego]?.portada else null, i == 3)
                Spacer(Modifier.height(esc.d(5)))
                Texto(nombre, 13f, lineas = 1)
                Detalle(EstudioCajas.procedencia(g, i), 10.5f, lineas = 2)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
        ACCIONES_CARA.forEachIndexed { k, (t, d, ic) ->
            val i = 4 + k
            val foco = sel == i
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(esc.d(12)))
                    .then(if (foco) Modifier.fondoBoton(Uva) else Modifier.background(Color(0xFF24253C)))
                    .clickable { Diptico.usarEstudio(i) }.padding(horizontal = esc.d(8), vertical = esc.d(7)),
            ) {
                IconoDiptico(ic, if (foco) SobreUva else Color(0xFFF2F1FA), 16f, acento = if (foco) SobreUva else Coral)
                Spacer(Modifier.height(esc.d(3)))
                Texto(t, 12f, color = if (foco) SobreUva else Color(0xFFF2F1FA), lineas = 1)
                Detalle(d, 9.5f, color = if (foco) SobreUva.copy(alpha = .8f) else LocalPaleta.current.tenue, lineas = 2)
            }
        }
    }
    val f = e.extra.cajaFuentes
    if (f.any { it.isNotEmpty() }) Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
        FuenteCaja("Comunidad", f.getOrElse(0) { "" }, Modifier.weight(1f))
        FuenteCaja("ScreenScraper", f.getOrElse(1) { "" }, Modifier.weight(1f))
        FuenteCaja("TheGamesDB", f.getOrElse(2) { "" }, Modifier.weight(1f))
    }
}

/** La miniatura de una cara (o su lugar vacío). */
@Composable
private fun MiniCara(valor: String?, respaldo: Uri?, disco: Boolean) {
    val esc = LocalEscala.current
    val img = rememberImagen((valor?.let { Uri.parse(it) }) ?: respaldo, 240)
    Box(
        Modifier.height(esc.d(78)).aspectRatio(if (disco) 1f else 0.75f).clip(if (disco) CircleShape else RoundedCornerShape(esc.d(6)))
            .background(Brush.linearGradient(listOf(Color(0xFF2A2547), Color(0xFF151526)))),
        contentAlignment = Alignment.Center,
    ) {
        if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else IconoDiptico(if (disco) Icono.JUGAR else Icono.BISA, Color.White.copy(alpha = 0.35f), 20f)
    }
}

@Composable
private fun PestanaForma(e: Estado, g: CarasGuardadas?) {
    val esc = LocalEscala.current
    val sel = e.extra.cajaSel
    val elementos = listOf<Pair<String, Float?>>("De la consola" to null) + EstudioCajas.PLANTILLAS.map { it.nombre to it.aspecto }
    val actual = EstudioCajas.PLANTILLAS.indexOfFirst { it.id == g?.plantilla } + 1
    elementos.chunked(4).forEachIndexed { fila, grupo ->
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
            grupo.forEachIndexed { k, (nombre, aspecto) ->
                val i = fila * 4 + k
                Column(
                    Modifier.weight(1f).casilla(sel == i, actual == i) { Diptico.usarEstudio(i) }.padding(esc.d(7)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.height(esc.d(40)), contentAlignment = Alignment.Center) {
                        val a = (aspecto ?: 0.75f).coerceIn(0.5f, 1.5f)
                        Box(
                            Modifier.height(esc.d(if (a > 1f) 40f / a else 40f)).aspectRatio(a).clip(RoundedCornerShape(esc.d(3)))
                                .background(if (aspecto == null) Coral.copy(alpha = 0.6f) else Color(0xFF3B4C9C)),
                        )
                    }
                    Spacer(Modifier.height(esc.d(4)))
                    Texto(nombre, 11f, lineas = 2)
                }
            }
            repeat(4 - grupo.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun PestanaAcabado(e: Estado, g: CarasGuardadas?) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val sel = e.extra.cajaSel
    val nc = EstudioCajas.COLORES.size
    Etiqueta("Color del estuche", p.tenue, 11f)
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10)), verticalAlignment = Alignment.CenterVertically) {
        EstudioCajas.COLORES.forEachIndexed { i, (_, color) ->
            val elegido = g?.color == color
            val consola = e.juegosPorClave[e.extra.cajaJuego]?.consolaId?.let { Consolas.porId(it)?.color } ?: Coral
            Box(
                Modifier.size(esc.d(36)).clip(CircleShape).background(color?.let { Color(it) } ?: consola)
                    .border(esc.d(if (sel == i) 3f else if (elegido) 2f else 1f), if (sel == i) Color.White else if (elegido) Uva else Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable { Diptico.usarEstudio(i) },
            )
        }
    }
    Detalle(EstudioCajas.COLORES.getOrNull(sel)?.first ?: EstudioCajas.COLORES.firstOrNull { it.second == g?.color }?.first.orEmpty(), 11.5f, lineas = 1)
    Etiqueta("Material", p.tenue, 11f)
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
        EstudioCajas.MATERIALES.forEachIndexed { k, (id, nombre) ->
            val i = nc + k
            val elegido = (g?.material ?: "brillante") == id
            Box(
                Modifier.weight(1f).casilla(sel == i, elegido) { Diptico.usarEstudio(i) }.padding(vertical = esc.d(9), horizontal = esc.d(6)),
                contentAlignment = Alignment.Center,
            ) { Texto(nombre, 12f, lineas = 2) }
        }
    }
    Detalle("Los efectos de la vitrina siguen el ajuste de fluidez: en modo Ahorro la caja no lleva reflejos.", 10.5f, lineas = 2)
}

@Composable
private fun PestanaVariantes(e: Estado, g: CarasGuardadas?, variantes: List<Pair<String, CarasGuardadas>>) {
    val esc = LocalEscala.current
    val sel = e.extra.cajaSel
    val portada = e.juegosPorClave[e.extra.cajaJuego]?.portada
    variantes.forEachIndexed { i, (k, v) ->
        val enUso = EstudioCajas.misma(v, g)
        Row(
            Modifier.fillMaxWidth().casilla(sel == i, enUso) { Diptico.usarEstudio(i) }.padding(esc.d(8)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.height(esc.d(44)).width(esc.d(34)).clip(RoundedCornerShape(esc.d(4)))) { MiniCara(v.frente, portada, false) }
            Spacer(Modifier.width(esc.d(10)))
            Column(Modifier.weight(1f)) {
                Texto(v.nombre ?: "Variante", 13.5f, lineas = 1)
                Detalle(
                    (EstudioCajas.plantilla(v.plantilla)?.nombre ?: "Forma de la consola") + " · " + EstudioCajas.nombreMaterial(v.material),
                    10.5f, lineas = 1,
                )
            }
            if (enUso) Texto("En uso", 11.5f, color = Color(0xFF7BE7B9), lineas = 1)
            Spacer(Modifier.width(esc.d(8)))
            // Quitar esta variante (X con el mando sobre la fila).
            Box(
                Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable { Diptico.quitarVariante(k) }
                    .padding(horizontal = esc.d(10), vertical = esc.d(5)),
            ) { Texto(if (sel == i) "X Quitar" else "Quitar", 11f, color = Color(0xFFD9D7EA), lineas = 1) }
        }
    }
    val n = variantes.size
    listOf(
        Triple("Guardar como variante nueva", "Hasta ${EstudioCajas.MAX_VARIANTES}: la más vieja deja su lugar", Icono.ESTRELLA),
        Triple("Restaurar la caja completa", "Vuelve a la portada; tus variantes se quedan", Icono.BORRAR),
    ).forEachIndexed { k, (t, d, ic) ->
        val i = n + k
        val foco = sel == i
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(12)))
                .then(if (foco) Modifier.fondoBoton(Uva) else Modifier.background(Color(0xFF24253C)))
                .clickable { Diptico.usarEstudio(i) }.padding(horizontal = esc.d(12), vertical = esc.d(8)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconoDiptico(ic, if (foco) SobreUva else Color(0xFFF2F1FA), 18f, acento = if (foco) SobreUva else Coral)
            Spacer(Modifier.width(esc.d(10)))
            Column {
                Texto(t, 13f, color = if (foco) SobreUva else Color(0xFFF2F1FA), lineas = 1)
                Detalle(d, 10.5f, color = if (foco) SobreUva.copy(alpha = .8f) else LocalPaleta.current.tenue, lineas = 1)
            }
        }
    }
}

@Composable
private fun FuenteCaja(nombre: String, estado: String, modifier: Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val (texto, color) = when (estado) {
        "buscando" -> "Buscando…" to Ambar
        "hallada" -> "¡La encontré!" to Verde
        "no" -> "No está" to p.tenue
        "sin" -> "No conectada" to p.tenue
        else -> "—" to p.tenue
    }
    Column(
        modifier.clip(RoundedCornerShape(esc.d(12))).background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = esc.d(10), vertical = esc.d(6)),
    ) {
        Texto(nombre, 12.5f, lineas = 1, traducir = false)
        Texto(texto, 11.5f, color = color, lineas = 1)
    }
}


/**
 * Estudio de Cajas en la pantalla de abajo con el diseño Colección viva (boceto 4): pestañas grandes con L1/R1,
 * las cuatro caras como tarjetas con su imagen y de dónde viene, las acciones de la cara elegida, las fuentes
 * y un pie con botones grandes. Forma, Acabado y Variantes usan sus casillas de siempre, agrandadas.
 */
@Composable
private fun PanelEstudioCV(e: Estado, clave: String, g: CarasGuardadas?) {
    val base = LocalEscala.current
    val tab = e.extra.estudioTab
    val sel = e.extra.cajaSel
    val variantes = EstudioCajas.variantes(e.extra.caras, clave)
    // Más chico que el resto de la pantalla de abajo: aquí hay mucho que mostrar a la vez.
    LienzoBoceto(1240f, 1080f, zoom = 1.08f) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(32), vertical = esc.d(20)), verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
            // Pestañas
            Row(Modifier.fillMaxWidth().height(esc.d(68)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                TeclaHombro("L1") { Diptico.pestanaEstudio(-1) }
                Row(Modifier.weight(1f).fillMaxHeight().vidrio(42f).padding(esc.d(6)), horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    EstudioCajas.PESTANAS.forEachIndexed { i, t ->
                        Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(if (tab == i) col.texto else Color.Transparent)
                            .clickable { Diptico.elegirPestanaCajas(i) }, contentAlignment = Alignment.Center) {
                            TB(t, 21f, color = if (tab == i) col.enElegido else col.sub, peso = FontWeight.ExtraBold)
                        }
                    }
                }
                TeclaHombro("R1") { Diptico.pestanaEstudio(1) }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(esc.d(16))) {
                if (tab == 0) {
                    // DIPPO cuenta qué tiene la caja.
                    // DIPPO chiquito, en una línea, para que las caras tengan el espacio.
                    Row(Modifier.fillMaxWidth().vidrio(28f).padding(horizontal = esc.d(12), vertical = esc.d(6)),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                        AvatarBisa(44f, cara = if (g?.frente != null && g.reverso != null) CaraDippo.FELIZ else CaraDippo.NORMAL)
                        TB(EstudioCajas.comentario(g, tab, variantes.size), 17f, color = col.sub, lineas = 2)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                        EstudioCajas.CARAS.forEachIndexed { i, nombre ->
                            val valor = when (i) { 0 -> g?.frente; 1 -> g?.lomo; 2 -> g?.reverso; else -> g?.disco }
                            val foco = sel == i
                            val falta = valor == null && i != 0
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(esc.d(24)))
                                    .background(if (foco) col.acento.copy(alpha = .2f) else Color.White.copy(alpha = .06f))
                                    .border(if (foco) 3.dp else 1.dp, if (foco) col.texto else Color.White.copy(alpha = .1f), RoundedCornerShape(esc.d(24)))
                                    .clickable { Diptico.usarEstudio(i) }.padding(esc.d(10)),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(5)),
                            ) {
                                val img = rememberImagen((valor?.let { Uri.parse(it) }) ?: if (i == 0) e.juegosPorClave[clave]?.portada else null, 300)
                                Box(
                                    Modifier.height(esc.d(128)).aspectRatio(if (i == 3) 1f else 0.74f).clip(if (i == 3) CircleShape else RoundedCornerShape(esc.d(8)))
                                        .background(Brush.linearGradient(listOf(Color(0xFF2A2547), Color(0xFF151526))))
                                        .then(if (falta) Modifier.border(2.dp, Color.White.copy(alpha = .35f), if (i == 3) CircleShape else RoundedCornerShape(esc.d(8))) else Modifier),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    else TB("+", 48f, color = col.tenue)
                                }
                                TB(nombre, 22f, titulo = true)
                                TB(EstudioCajas.procedencia(g, i), 15f, color = if (falta) Color(0xFFFFB48F) else Color(0xFF9DB6FF), peso = FontWeight.Bold, lineas = 2)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                        ACCIONES_CARA.forEachIndexed { k, (t, d, ic) ->
                            val i = 4 + k
                            val foco = sel == i
                            Row(
                                Modifier.weight(1f).height(esc.d(70)).clip(RoundedCornerShape(esc.d(35)))
                                    .background(if (foco) col.texto else Color.White.copy(alpha = .07f))
                                    .border(1.dp, Color.White.copy(alpha = .14f), RoundedCornerShape(esc.d(35)))
                                    .clickable { Diptico.usarEstudio(i) }.padding(horizontal = esc.d(12)),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8)),
                            ) {
                                IconoDiptico(ic, if (foco) col.enElegido else col.texto, 32f, acento = Coral)
                                TB(t, 16f, color = if (foco) col.enElegido else col.texto, peso = FontWeight.ExtraBold, lineas = 2)
                            }
                        }
                    }
                    val f = e.extra.cajaFuentes
                    if (f.any { it.isNotEmpty() }) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                        listOf("Comunidad", "ScreenScraper", "TheGamesDB").forEachIndexed { i, n ->
                            val (texto, color) = when (f.getOrElse(i) { "" }) {
                                "buscando" -> "Buscando…" to Ambar
                                "hallada" -> "¡La encontré!" to Verde
                                "no" -> "No está" to col.tenue
                                "sin" -> "No conectada" to col.tenue
                                else -> "—" to col.tenue
                            }
                            Column(Modifier.weight(1f).vidrio(20f).padding(esc.d(10))) {
                                TB(n, 17f, peso = FontWeight.ExtraBold, traducir = false)
                                TB(texto, 16f, color = color, peso = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Las casillas de siempre, al tamaño de esta pantalla.
                    CompositionLocalProvider(LocalEscala provides Escala(base.factor * 1.2f, base.ancho, base.texto)) {
                        Column(verticalArrangement = Arrangement.spacedBy(base.d(8))) {
                            when (tab) {
                                1 -> PestanaForma(e, g)
                                2 -> PestanaAcabado(e, g)
                                else -> PestanaVariantes(e, g, variantes)
                            }
                        }
                    }
                }
            }
            // Pie: botones grandes.
            Row(Modifier.fillMaxWidth().height(esc.d(72)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                BotonTecla("Y", "Jugar", principal = true, alto = 64f, letra = 22f, onClick = { val k = clave; Diptico.cerrarCajaJuego(); Diptico.elegirJuego(k); Diptico.jugar() })
                BotonTecla("Select", "Ver de otro lado", alto = 64f, letra = 22f, onClick = { Diptico.siguienteVistaEstudio() })
                Spacer(Modifier.weight(1f))
                BotonTecla("B", "Volver", alto = 64f, letra = 22f, onClick = { Diptico.cerrarCajaJuego() })
            }
        }
    }
}

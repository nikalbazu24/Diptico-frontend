package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Medio
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Opcion
import cl.nikalbazu.diptico.estado.Panel
import kotlinx.coroutines.delay

/** Pantalla de abajo: consolas, estante de juegos y barra de acciones. */
@Composable
fun PantallaInferior(e: Estado, miDisplay: Int) {
    val p = LocalPaleta.current
    Box(Modifier.fillMaxSize().background(p.fondo2)) {
        val s = e.sesion
        when {
            !e.configurado -> Configuracion(e)
            s != null && s.displayId != miDisplay && !s.dosPantallas -> EnJuego(e)
            else -> Column(Modifier.fillMaxSize()) {
                Cinta(e)
                Box(Modifier.padding(horizontal = LocalEscala.current.d(20)).fillMaxWidth().height(LocalEscala.current.d(1)).background(p.linea))
                Estante(e)
                Info(e)
                Spacer(Modifier.weight(1f))
                Dock(e)
            }
        }
        when (e.panel) {
            Panel.MENU -> PanelLista(titulo = e.seleccionado?.titulo ?: "", opciones = Diptico.opcionesMenu(), indice = e.indice)
            Panel.AJUSTES -> PanelLista(titulo = "Ajustes", opciones = Diptico.opcionesAjustes(), indice = e.indice)
            Panel.PASO -> PanelPaso(e)
            Panel.NINGUNO -> Unit
        }
        Aviso(e.aviso, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Cinta(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Row(
        Modifier.fillMaxWidth().padding(start = esc.d(12), end = esc.d(12), top = esc.d(10)),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (id in e.pestanas) {
            val c = Consolas.porId(id)
            val activa = id == e.consolaSel
            val escala by animateFloatAsState(if (activa) 1.15f else 1f, spring(dampingRatio = 0.5f), label = "icono")
            Column(
                Modifier.width(esc.d(56)).clickable { Diptico.elegirConsola(id) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.height(esc.d(46)), contentAlignment = Alignment.Center) {
                    IconoMedio(
                        c?.medio ?: Medio.TODOS, c?.color ?: Uva, p.fondo2,
                        modifier = Modifier.graphicsLayer { scaleX = escala; scaleY = escala },
                        tam = 42f,
                    )
                }
                Texto(c?.nombre ?: "Todo", 10f, color = if (activa) p.tinta else p.tenue, familia = Nunito, peso = FontWeight.ExtraBold)
                Spacer(Modifier.height(esc.d(5)))
                val ancho by animateDpAsState(if (activa) esc.d(24) else esc.d(0), label = "barra")
                Box(Modifier.width(ancho).height(esc.d(3)).clip(RoundedCornerShape(50)).background(Coral))
            }
        }
    }
}

@Composable
private fun Estante(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val lista = e.visibles
    val estado = rememberLazyListState()
    LaunchedEffect(e.indiceSel, e.consolaSel, lista.size) {
        if (lista.isNotEmpty()) estado.animateScrollToItem((e.indiceSel - 1).coerceAtLeast(0))
    }
    Box(Modifier.fillMaxWidth().height(esc.d(236))) {
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(38)).fillMaxWidth().height(esc.d(3)).background(p.estante),
        )
        LazyRow(
            state = estado,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(esc.d(18)),
            verticalAlignment = Alignment.Bottom,
            contentPadding = PaddingValues(horizontal = esc.d(20)),
        ) {
            itemsIndexed(lista, key = { _, j -> j.clave }) { i, j ->
                val c = Consolas.porId(j.consolaId) ?: return@itemsIndexed
                val elegido = i == e.indiceSel
                val alza by animateFloatAsState(if (elegido) -14f else 0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow), label = "alza")
                val tam by animateFloatAsState(if (elegido) 1.06f else 1f, spring(dampingRatio = 0.5f), label = "tam")
                val u = with(LocalDensity.current) { esc.d(1f).toPx() }
                val color = colorDe(j.clave)
                val forma = RoundedCornerShape(esc.d(c.caja.radio))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { if (elegido) Diptico.jugar() else Diptico.elegirJuego(j.clave) },
                ) {
                    Box(
                        Modifier
                            .graphicsLayer { translationY = alza * u; scaleX = tam; scaleY = tam }
                            .then(
                                if (elegido) Modifier
                                    .shadow(esc.d(18), forma, ambientColor = color, spotColor = color)
                                    .border(esc.d(3), Color.White, forma)
                                else Modifier,
                            ),
                    ) { CajaJuego(j, c) }
                    Box(
                        Modifier.padding(top = esc.d(8)).width(esc.d(c.caja.ancho)).height(esc.d(30)).clip(forma)
                            .background(Brush.verticalGradient(listOf(color.copy(alpha = if (elegido) 0.33f else 0.18f), Color.Transparent))),
                    )
                }
            }
        }
    }
}

@Composable
private fun Info(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val j = e.seleccionado ?: return
    val c = Consolas.porId(j.consolaId)
    Row(Modifier.fillMaxWidth().padding(start = esc.d(22), end = esc.d(22), top = esc.d(4)), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Texto(j.titulo, 24f, lineas = 1)
            Texto(
                "${c?.nombreLargo ?: ""} · ${e.indiceSel + 1} de ${e.visibles.size}",
                12f, color = p.tenue, familia = Nunito, peso = FontWeight.ExtraBold,
            )
        }
        Texto("L1 · R1 cambian de consola", 12f, color = p.tenue, familia = Nunito, peso = FontWeight.ExtraBold)
    }
}

@Composable
private fun Dock(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column {
        Box(Modifier.fillMaxWidth().height(esc.d(1)).background(p.linea))
        Row(Modifier.fillMaxWidth().padding(horizontal = esc.d(12)).height(esc.d(62))) {
            ItemDock(Icons.Filled.PlayArrow, "Continuar", p.acentoTexto) { Diptico.continuar() }
            ItemDock(Icons.Filled.Refresh, "Sorpréndeme") { Diptico.sorprendeme() }
            ItemDock(Icons.Filled.Edit, "Próximo paso") { Diptico.abrirPaso() }
            ItemDock(null, if (e.oscuro) "Claro" else "Oscuro", glifo = "◐") { Diptico.alternarTema() }
            ItemDock(Icons.Filled.Settings, "Ajustes") { Diptico.abrirAjustes() }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ItemDock(
    icono: ImageVector?,
    texto: String,
    color: Color = LocalPaleta.current.tinta,
    glifo: String? = null,
    accion: () -> Unit,
) {
    val esc = LocalEscala.current
    Column(
        Modifier.weight(1f).fillMaxSize().clickable { accion() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icono != null) Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(esc.d(22)))
        else Texto(glifo ?: "", 18f, color = color)
        Spacer(Modifier.height(esc.d(3)))
        Texto(texto, 11f, color = color, familia = Nunito, peso = FontWeight.ExtraBold)
    }
}

/** Mientras juegas arriba: la pantalla de abajo se atenúa y muestra el juego. */
@Composable
private fun EnJuego(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val s = e.sesion ?: return
    val juego = e.juegos.firstOrNull { it.clave == s.clave }
    val c = juego?.let { Consolas.porId(it.consolaId) }
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(s.inicio) {
        while (true) {
            ahora = System.currentTimeMillis()
            delay(1000)
        }
    }
    val seg = (ahora - s.inicio) / 1000
    val reloj = "${seg / 60}:${(seg % 60).toString().padStart(2, '0')}"
    val color = juego?.let { colorDe(it.clave) } ?: Uva

    Box(
        Modifier.fillMaxSize().background(Color(0xFF07060C)).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
        ) { if (!e.despierta) Diptico.despertar() },
    ) {
        if (!e.despierta) {
            Box(
                Modifier.align(Alignment.Center).size(esc.d(460)).background(
                    Brush.radialGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)),
                ),
            )
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Texto(c?.nombreLargo ?: "", 11f, color = color, familia = Nunito, peso = FontWeight.ExtraBold, espaciado = 3f)
                Spacer(Modifier.height(esc.d(12)))
                Texto(juego?.titulo ?: "Jugando", 52f, color = Color.White, lineas = 2, interlineado = 56f, alinear = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = esc.d(24), end = esc.d(24), bottom = esc.d(58))) {
                Texto("Sesión · $reloj", 14f, color = Color(0xFF8E8AAE))
            }
            Texto(
                "Toca o aprieta un botón para abrir el compañero de partida", 12f, color = Color(0xFF8E8AAE),
                familia = Nunito, peso = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = esc.d(24)),
            )
        } else {
            Column(Modifier.fillMaxSize().background(p.fondo2).padding(esc.d(20)), verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(esc.d(54)).clip(RoundedCornerShape(esc.d(16))).background(color))
                    Spacer(Modifier.width(esc.d(12)))
                    Column(Modifier.weight(1f)) {
                        Texto(juego?.titulo ?: "", 20f, lineas = 1)
                        Texto("Sesión de $reloj", 12f, color = p.tenue, familia = Nunito, peso = FontWeight.ExtraBold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                    BotonGrande("Anotar próximo paso", Coral, Tinta, Modifier.weight(1f)) { Diptico.abrirPaso() }
                    BotonGrande("Atenuar", Sol, Tinta, Modifier.weight(1f)) { Diptico.atenuar() }
                }
                BotonGrande("Terminé de jugar", Uva, Color.White, Modifier.fillMaxWidth()) { Diptico.cerrarSesion(preguntar = true) }
                Spacer(Modifier.weight(1f))
                Texto("Se vuelve a atenuar sola en unos segundos.", 12f, color = p.tenue, familia = Nunito, peso = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun BotonGrande(texto: String, fondo: Color, tinta: Color, modifier: Modifier = Modifier, accion: () -> Unit) {
    val esc = LocalEscala.current
    Box(
        modifier.height(esc.d(70)).clip(RoundedCornerShape(esc.d(22))).background(fondo).clickable { accion() },
        contentAlignment = Alignment.Center,
    ) { Texto(texto, 17f, color = tinta) }
}

/** Primer arranque: elegir carpeta y revisar emuladores. */
@Composable
private fun Configuracion(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column(Modifier.fillMaxSize().padding(esc.d(22)), verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
        when {
            !e.tieneCarpeta -> {
                Texto("¡Hola! Soy Díptico", 26f)
                Texto(
                    "Elige la carpeta donde tienes tus juegos. Adentro debe haber una carpeta por consola: " +
                        "snes, n64, psx, gba, gc, nds, ps2, ps3, ps4 o psp (igual que en ES-DE).",
                    14f, color = p.tenue, familia = Nunito, interlineado = 21f,
                )
                Spacer(Modifier.weight(1f))
                BotonGrande("A · Elegir carpeta de juegos", Uva, Color.White, Modifier.fillMaxWidth()) { Diptico.accionConfiguracion() }
            }
            e.escaneando && e.juegos.isEmpty() -> {
                Texto("Buscando tus juegos…", 26f)
                Texto("Esto puede tardar un poco la primera vez.", 14f, color = p.tenue, familia = Nunito)
            }
            e.juegos.isEmpty() -> {
                Texto("No encontré juegos", 26f)
                Texto(
                    "Revisa que adentro haya carpetas como snes, psx o nds con tus juegos, y elige la carpeta de nuevo.",
                    14f, color = p.tenue, familia = Nunito, interlineado = 21f,
                )
                Spacer(Modifier.weight(1f))
                BotonGrande("A · Elegir otra carpeta", Uva, Color.White, Modifier.fillMaxWidth()) { Diptico.accionConfiguracion() }
            }
            else -> {
                Texto("Encontré ${e.juegos.size} juegos", 26f)
                Texto("Así se abrirá cada consola:", 13f, color = p.tenue, familia = Nunito, peso = FontWeight.ExtraBold)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    itemsIndexed(e.consolasConJuegos, key = { _, c -> c.id }) { _, c ->
                        val listo = Diptico.emuladorListo(c)
                        Row(
                            Modifier.fillMaxWidth().height(esc.d(42)).clip(RoundedCornerShape(esc.d(14))).background(p.superficie.copy(alpha = 0.6f))
                                .padding(horizontal = esc.d(12)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconoMedio(c.medio, c.color, p.fondo2, tam = 26f)
                            Spacer(Modifier.width(esc.d(10)))
                            Texto(c.nombre, 13f, color = p.tenue, familia = Nunito, peso = FontWeight.ExtraBold, modifier = Modifier.width(esc.d(48)))
                            Texto(
                                if (listo) Diptico.nombreEmulador(c) else "Instala ${c.emuladores.first().nombre}",
                                15f, modifier = Modifier.weight(1f), lineas = 1,
                            )
                            Box(
                                Modifier.clip(RoundedCornerShape(50)).background(if (listo) Sol.copy(alpha = 0.18f) else Coral)
                                    .padding(horizontal = esc.d(10), vertical = esc.d(4)),
                            ) { Texto(if (listo) "Listo" else "Falta", 12f, color = if (listo) Sol else Tinta, familia = Nunito, peso = FontWeight.ExtraBold) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                    BotonGrande(
                        if (e.tieneMedia) "Imágenes ES-DE ✓" else "Imágenes de ES-DE",
                        p.superficie, p.tinta, Modifier.weight(1f),
                    ) { Diptico.pedirCarpeta?.invoke(false) }
                    BotonGrande("A · ¡Empezar!", Uva, Color.White, Modifier.weight(1.4f)) { Diptico.terminarConfiguracion() }
                }
            }
        }
    }
}

@Composable
private fun Fondo(contenido: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
        ) { Diptico.cerrarPanel() },
        contentAlignment = Alignment.Center,
    ) { contenido() }
}

@Composable
private fun PanelLista(titulo: String, opciones: List<Opcion>, indice: Int) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val lista = rememberLazyListState()
    LaunchedEffect(indice) { if (opciones.isNotEmpty()) lista.animateScrollToItem((indice - 2).coerceAtLeast(0)) }
    Fondo {
        Column(
            Modifier.width(esc.d(540)).clip(RoundedCornerShape(esc.d(26))).background(p.superficie)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(esc.d(10)),
        ) {
            Texto(titulo, 20f, lineas = 1, modifier = Modifier.padding(start = esc.d(12), top = esc.d(8), bottom = esc.d(8)))
            LazyColumn(state = lista, modifier = Modifier.height(esc.d(380)), verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
                itemsIndexed(opciones) { i, o ->
                    val activa = i == indice
                    Row(
                        Modifier.fillMaxWidth().height(esc.d(50)).clip(RoundedCornerShape(esc.d(16)))
                            .background(if (activa) Uva else Color.Transparent)
                            .clickable { Diptico.elegirIndice(i) }
                            .padding(horizontal = esc.d(14)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Texto(o.titulo, 16f, color = if (activa) Color.White else p.tinta, modifier = Modifier.weight(1f), lineas = 1)
                        if (o.valor.isNotEmpty()) {
                            Texto(o.valor, 13f, color = if (activa) Color.White else p.tenue, familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1)
                        }
                    }
                }
            }
            Texto(
                "A elegir · B cerrar", 12f, color = p.tenue, familia = Nunito, peso = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = esc.d(12), top = esc.d(6), bottom = esc.d(4)),
            )
        }
    }
}

@Composable
private fun PanelPaso(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        try { foco.requestFocus() } catch (_: Exception) {}
    }
    Fondo {
        Column(
            Modifier.width(esc.d(560)).clip(RoundedCornerShape(esc.d(26))).background(p.superficie)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(esc.d(20)),
            verticalArrangement = Arrangement.spacedBy(esc.d(12)),
        ) {
            Texto(if (e.pregunta) "¿Dónde quedaste?" else "Tu próximo paso", 22f)
            Texto(e.seleccionado?.titulo ?: "", 13f, color = p.tenue, familia = Nunito, peso = FontWeight.ExtraBold, lineas = 1)
            val densidad = LocalDensity.current
            BasicTextField(
                value = e.borrador,
                onValueChange = { Diptico.cambiarBorrador(it) },
                textStyle = TextStyle(color = p.tinta, fontSize = esc.s(18), fontFamily = Fredoka, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(Uva),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { Diptico.guardarPaso() }),
                modifier = Modifier.fillMaxWidth().focusRequester(foco)
                    .clip(RoundedCornerShape(esc.d(18))).background(p.fondo2)
                    .border(esc.d(2), p.linea, RoundedCornerShape(esc.d(18)))
                    .padding(esc.d(14)).height(esc.d(70)),
                decorationBox = { campo ->
                    Box {
                        if (e.borrador.isEmpty()) Texto("Ej: me falta la llave del jefe en el templo", 18f, color = p.tenue, peso = FontWeight.Medium)
                        campo()
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                BotonGrande("B · Cancelar", p.fondo2, p.tinta, Modifier.weight(1f)) { Diptico.cerrarPanel() }
                BotonGrande("Guardar", Uva, Color.White, Modifier.weight(1.4f)) { Diptico.guardarPaso() }
            }
        }
    }
}

@Composable
private fun Aviso(texto: String?, modifier: Modifier) {
    val esc = LocalEscala.current
    AnimatedVisibility(
        visible = texto != null,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier.padding(bottom = esc.d(76)),
    ) {
        Box(
            Modifier.padding(horizontal = esc.d(24)).clip(RoundedCornerShape(esc.d(18))).background(Tinta.copy(alpha = 0.94f))
                .padding(horizontal = esc.d(18), vertical = esc.d(12)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(esc.d(9)).clip(CircleShape).background(Coral))
                Spacer(Modifier.width(esc.d(10)))
                Texto(texto ?: "", 14f, color = Color.White, familia = Nunito, peso = FontWeight.ExtraBold)
            }
        }
    }
}

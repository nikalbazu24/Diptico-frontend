package cl.nikalbazu.diptico.ui


import cl.nikalbazu.diptico.estado.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.LogrosDippo
import cl.nikalbazu.diptico.estado.Perfil
import cl.nikalbazu.diptico.estado.Seccion
import cl.nikalbazu.diptico.estado.tituloDe
import cl.nikalbazu.diptico.estado.ResumenPerfil
import cl.nikalbazu.diptico.estado.SocialDiscord
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val TextoClaro = Color(0xFFF4F5FB)
private val TextoSuave = Color(0xFFA5A8BD)

/**
 * Tu foto en una esquina: al tocarla se abre tu perfil, y la tarjeta crece desde ahí mismo.
 * Guarda dónde está (como fracción de la pantalla) para que la animación salga de este punto.
 */
@Composable
fun BotonPerfil(tam: Float = 26f) {
    val vista = LocalView.current
    val pantalla = pantallaActual()
    val esc = LocalEscala.current
    val pf by remember { Diptico.estado.map { it.extra.perfil }.distinctUntilChanged() }.collectAsState(Diptico.estado.value.extra.perfil)
    val cuenta by remember { Diptico.estado.map { it.extra.discordUsuario.isNotBlank() }.distinctUntilChanged() }.collectAsState(Diptico.estado.value.extra.discordUsuario.isNotBlank())
    val conexion by SocialDiscord.conexion.collectAsState()
    // Centro de la foto en la ventana (fracción del ancho y del alto); no se lee al dibujar, así que no recompone nada.
    val centro = remember { floatArrayOf(0.92f, 0.07f) }
    val insignia = tam * 0.46f
    Box(Modifier.padding(end = esc.d(insignia * 0.25f), bottom = esc.d(insignia * 0.2f))) {
        Box(
            Modifier
                .onGloballyPositioned { c ->
                    val p = c.positionInWindow()
                    val w = vista.rootView.width.coerceAtLeast(1)
                    val h = vista.rootView.height.coerceAtLeast(1)
                    centro[0] = (p.x + c.size.width / 2f) / w
                    centro[1] = (p.y + c.size.height / 2f) / h
                }
                .clip(CircleShape)
                .clickable { Diptico.abrirPerfil(pantalla, centro[0], centro[1]) }
                .testTag("boton-perfil"),
        ) { FotoPerfil(pf, tam) }
        // La insignia de Discord: un toque abre la ventana de amigos. El puntito verde dice si estás conectado.
        val listo = cuenta && (!SocialDiscord.disponible || conexion == "listo")
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = esc.d(insignia * 0.25f), y = esc.d(insignia * 0.2f)).size(esc.d(insignia))
                .clip(CircleShape).background(Color(0xFF1E1F24)).padding(esc.d(insignia * 0.1f))
                .clickable { Diptico.abrirAmigos(pantalla) }
                .testTag("boton-discord"),
            contentAlignment = Alignment.Center,
        ) { MarcaDiscord(insignia * 0.8f, listo) }
    }
}

/**
 * Tu perfil en grande en la pantalla de arriba, como en iiSU: tu nombre y tu título junto a una foto grande.
 * Tocar el nombre o la foto abre tu tarjeta; la insignia de Discord abre tus amigos.
 */
@Composable
fun PerfilArriba(tam: Float = 54f, conReloj: Boolean = false) {
    val esc = LocalEscala.current
    val pantalla = pantallaActual()
    val nombre by remember { Diptico.estado.map { it.nombre }.distinctUntilChanged() }.collectAsState(Diptico.estado.value.nombre)
    val pf by remember { Diptico.estado.map { it.extra.perfil }.distinctUntilChanged() }.collectAsState(Diptico.estado.value.extra.perfil)
    val titulo = tituloDe(pf.titulo)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("perfil-arriba")) {
        if (conReloj) { RelojBateria(); Spacer(Modifier.width(esc.d(6))) }
        Column(
            Modifier.clip(RoundedCornerShape(50)).clickable { Diptico.abrirPerfil(pantalla, 0.9f, 0.08f) }
                .padding(horizontal = esc.d(10), vertical = esc.d(4)),
            horizontalAlignment = Alignment.End,
        ) {
            Texto(nombre.ifBlank { "Tu perfil" }, 18f, peso = FontWeight.ExtraBold, sombra = true, lineas = 1, traducir = nombre.isBlank())
            Texto(titulo.ifEmpty { "Toca para ver tu perfil" }, 11.5f, sombra = true, lineas = 1, color = LocalPaleta.current.tinta.copy(alpha = 0.75f))
        }
        BotonPerfil(tam)
    }
}

/**
 * Hora y batería juntas en una sola píldora oscura y suave, para ir al lado de tu foto arriba: con fondo propio se leen
 * sobre cualquier arte y quedan alineadas con la foto en vez de flotar sueltas. Tocar la hora cambia su tamaño.
 */
@Composable
fun RelojBateria(modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val hora = rememberHora()
    val bateria = rememberBateria()
    val cargando = rememberCargando()
    val tono = when { cargando -> Color(0xFF38BDF8); bateria in 0..15 -> Color(0xFFF87171); else -> Color.White }
    val forma = RoundedCornerShape(50)
    Row(
        modifier.testTag("reloj-bateria").clip(forma).background(Color.Black.copy(alpha = 0.34f))
            .border(esc.d(1), Color.White.copy(alpha = 0.16f), forma)
            .clickable { Diptico.alternarReloj() }
            .padding(horizontal = esc.d(13), vertical = esc.d(4)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Texto(hora, 14f, color = Color.White, peso = FontWeight.ExtraBold, traducir = false, lineas = 1)
        if (bateria >= 0) {
            Box(Modifier.padding(horizontal = esc.d(10)).width(esc.d(1)).height(esc.d(12)).background(Color.White.copy(alpha = 0.28f)))
            Box(Modifier.size(esc.d(18), esc.d(9)).border(esc.d(1.4f), Color.White.copy(alpha = 0.75f), RoundedCornerShape(esc.d(2.5f))).padding(esc.d(1.6f))) {
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth((bateria / 100f).coerceIn(0.08f, 1f))
                        .clip(RoundedCornerShape(esc.d(1.2f))).background(tono),
                )
            }
            Spacer(Modifier.width(esc.d(6)))
            Texto("$bateria%", 12.5f, color = if (tono == Color.White) Color.White.copy(alpha = 0.9f) else tono, traducir = false, lineas = 1)
        }
    }
}

/**
 * La tarjeta de tu perfil. Crece desde tu foto con un resorte suave, sobre el fondo difuminado (lo desenfoca [Raiz]),
 * y se cierra encogiéndose de vuelta hacia ella. B o tocar afuera la cierra; A abre los ajustes del perfil.
 */
@Composable
fun PanelPerfil(e: Estado, miPantalla: Int, rol: Rol = Rol.SUPERIOR) {
    val ed = e.extra.edicionPerfil
    val dividida = ed != null && Diptico.edicionDividida(e)
    val abierto = when {
        // Escribiendo un dato (teclado arriba): la tarjeta se aparta para no taparlo.
        ed != null && e.panel != cl.nikalbazu.diptico.estado.Panel.NINGUNO -> false
        // Editando con dos pantallas: la tarjeta va siempre arriba (el editor está abajo).
        dividida -> rol == Rol.SUPERIOR
        // Editando en una pantalla: el editor la cubre entera.
        ed != null -> false
        else -> e.extra.perfilEn == miPantalla
    }
    // Si la tarjeta se abrió abajo, al editar se queda arriba también después de guardar.
    LaunchedEffect(dividida, rol, miPantalla) { if (dividida && rol == Rol.SUPERIOR && e.extra.perfilEn != miPantalla) Diptico.moverPerfilA(miPantalla) }
    val animar = LocalMovimiento.current
    val t by animateFloatAsState(
        if (abierto) 1f else 0f,
        if (!animar) snap() else if (abierto) spring(dampingRatio = 0.8f, stiffness = 300f) else tween(210, easing = FastOutLinearInEasing),
        label = "perfil",
    )
    if (!abierto && t < 0.01f) return
    val esc = LocalEscala.current
    val ox = e.extra.perfilOx
    val oy = e.extra.perfilOy
    // Mientras editas, la tarjeta muestra el borrador: ves cómo queda antes de guardar.
    val ev = remember(e, ed) { if (ed != null) e.copy(nombre = ed.nombre, extra = e.extra.copy(perfil = ed.perfil)) else e }
    val color = Color(ev.extra.perfil.color)
    Box(Modifier.fillMaxSize()) {
        // Oscurece un poco más lo de atrás (el desenfoque lo pone Raiz).
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = t.coerceIn(0f, 1f) }.background(Color(0x66080912))
                .clickable(remember { MutableInteractionSource() }, indication = null) { Diptico.cerrarPerfil() },
        )
        // Todo el lienzo se escala desde el punto de la foto: así la tarjeta sale exactamente de ahí.
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    val s = 0.04f + 0.96f * t
                    scaleX = s; scaleY = s
                    alpha = (t * 2.2f).coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(ox, oy)
                }
                .padding(horizontal = esc.d(if (esc.angosta) 12 else 40), vertical = esc.d(if (esc.angosta) 12 else 26)),
        ) {
            val forma = RoundedCornerShape(esc.d(28))
            Box(
                Modifier.fillMaxSize().clip(forma)
                    .background(Brush.linearGradient(listOf(Color(0xF0171A32), Color(0xF00C0D1A))))
                    .background(Brush.radialGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent), center = Offset(0f, 0f), radius = 1100f))
                    .border(esc.d(1.5f), color.copy(alpha = 0.55f), forma)
                    .clickable(remember { MutableInteractionSource() }, indication = null) {}
                    .testTag("panel-perfil"),
            ) { ContenidoPerfil(ev) }
        }
    }
}

@Composable
private fun ContenidoPerfil(e: Estado) {
    val esc = LocalEscala.current
    val pf = e.extra.perfil
    val color = Color(pf.color)
    val r = remember(e.datos, e.juegos, pf, e.consolasConJuegos) { Diptico.resumenPerfil(e) }
    val nombre = e.nombre.ifBlank { pf.apodo.ifBlank { "Jugador" } }
    val angosta = esc.angosta
    Column(Modifier.fillMaxSize().padding(esc.d(if (angosta) 16 else 22))) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (angosta) {
                // Pantalla casi cuadrada: arriba tu foto y tu nombre, y abajo las pastillas y los números (sin favoritos, para que quepa todo).
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                    FotoPerfil(pf, 78f)
                    Column(Modifier.weight(1f)) { Encabezado(e, nombre, 26f) }
                }
                Spacer(Modifier.height(esc.d(8)))
                Fichas(e, r, color)
                Spacer(Modifier.height(esc.d(10)))
                Numeros(e, r, color, conFavoritos = false)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(26))) {
                    Column(Modifier.width(esc.d(196)), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        FotoPerfil(pf, 104f)
                        Encabezado(e, nombre, 30f)
                        Fichas(e, r, color)
                    }
                    Column(Modifier.weight(1f)) { Numeros(e, r, color, conFavoritos = true) }
                }
            }
        }
        Spacer(Modifier.height(esc.d(10)))
        if (e.extra.edicionPerfil != null) {
            // Editando abajo: aquí solo se avisa, y los botones guardan o cancelan igual que allá.
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10)), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.1f)).padding(horizontal = esc.d(14), vertical = esc.d(7))) {
                    Texto("✎ Editando en la pantalla de abajo", 13.5f, color = TextoClaro, lineas = 1)
                }
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(color).clickable { Diptico.guardarEdicionPerfil() }
                        .padding(horizontal = esc.d(16), vertical = esc.d(7)),
                ) { Texto("A · Guardar", 13.5f, color = if (color.luminance2() > 0.55f) Color(0xFF14101A) else Color.White, lineas = 1) }
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.1f)).clickable { Diptico.cancelarEdicionPerfil() }
                        .padding(horizontal = esc.d(16), vertical = esc.d(7)),
                ) { Texto("B · Cancelar", 13.5f, color = TextoClaro, lineas = 1) }
            }
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(color).clickable { Diptico.editarPerfil() }
                    .padding(horizontal = esc.d(16), vertical = esc.d(7)),
            ) { Texto("A · Editar perfil", 13.5f, color = if (color.luminance2() > 0.55f) Color(0xFF14101A) else Color.White, lineas = 1) }
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.1f)).clickable { Diptico.abrirTuAno(e.extra.perfilEn) }
                    .padding(horizontal = esc.d(16), vertical = esc.d(7)),
            ) { Texto("Y · Tu año", 13.5f, color = TextoClaro, lineas = 1) }
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.1f)).clickable { Diptico.cerrarPerfil() }
                    .padding(horizontal = esc.d(16), vertical = esc.d(7)),
            ) { Texto("B · Cerrar", 13.5f, color = TextoClaro, lineas = 1) }
        }
    }
}

private fun Color.luminance2(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

@Composable
private fun Encabezado(e: Estado, nombre: String, tam: Float) {
    val pf = e.extra.perfil
    val desde = remember(pf.desde) { java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(pf.desde)) }
    Column(verticalArrangement = Arrangement.spacedBy(LocalEscala.current.d(2))) {
        Texto(nombre, tam, color = TextoClaro, peso = FontWeight.Black, lineas = 1, traducir = false)
        tituloDe(pf.titulo).takeIf { it.isNotBlank() }?.let { Texto("★ $it", 13.5f, color = Sol, peso = FontWeight.Bold, lineas = 1, traducir = false) }
        Texto(
            (if (pf.apodo.isNotBlank()) "@${pf.apodo} · " else "") + "desde $desde",
            14f, color = TextoSuave, lineas = 1, traducir = false,
        )
    }
}

/** Las pastillas bajo tu nombre: Discord, tu consola favorita y tu edad o cumpleaños. */
@Composable
private fun Fichas(e: Estado, r: ResumenPerfil, color: Color) {
    val esc = LocalEscala.current
    val pf = e.extra.perfil
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
        if (pf.cumpleHoy()) Pastilla(color.copy(alpha = 0.3f), color) { Texto("🎂 ¡Hoy es tu cumpleaños${pf.edad()?.let { " · $it" } ?: ""}!", 12.5f, color = TextoClaro, lineas = 2, traducir = false) }
        if (e.extra.discordUsuario.isNotBlank()) Pastilla(Color(0x385865F2), Color(0xFF23A55A)) { Texto("Discord: ${e.extra.discordUsuario}", 12.5f, color = TextoClaro, lineas = 1, traducir = false) }
        if (r.consola.isNotBlank()) Pastilla(Color.White.copy(alpha = 0.08f), color) { Texto("Consola favorita: ${r.consola}", 12.5f, color = TextoClaro, lineas = 1, traducir = false) }
        pf.edad()?.takeIf { !pf.cumpleHoy() }?.let { Pastilla(Color.White.copy(alpha = 0.08f), color) { Texto("$it años", 12.5f, color = TextoClaro, lineas = 1, traducir = false) } }
    }
}

@Composable
private fun Pastilla(fondo: Color, punto: Color, contenido: @Composable () -> Unit) {
    val esc = LocalEscala.current
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(fondo).padding(horizontal = esc.d(11), vertical = esc.d(4)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(7)),
    ) {
        Box(Modifier.size(esc.d(8)).clip(CircleShape).background(punto))
        contenido()
    }
}

/** Tus números (horas, juego más jugado, racha, logros) y tus juegos favoritos. */
@Composable
private fun Numeros(e: Estado, r: ResumenPerfil, color: Color, conFavoritos: Boolean) {
    val esc = LocalEscala.current
    val pf = e.extra.perfil
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
            Dato("Horas jugadas", if (pf.mostrarHoras) horas(r.segundos) else "Oculto", if (pf.mostrarHoras) "${r.juegosJugados} juegos distintos" else "Se cambia en tu perfil", color, Modifier.weight(1f))
            Dato("Más jugado", r.masJugado?.titulo ?: "Aún nada", r.masJugado?.let { if (pf.mostrarHoras) horas(r.masJugadoSeg) else "" }.orEmpty(), color, Modifier.weight(1f), traducirValor = r.masJugado == null)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
            Dato("Racha", if (r.racha == 1) "1 día" else "${r.racha} días", if (r.racha > 0) "jugando seguido" else "juega hoy para empezar", color, Modifier.weight(1f))
            LogrosDippoTile(r, color, Modifier.weight(1f))
        }
        LogrosRATile(e, color)
        if (conFavoritos) {
            Texto("MIS FAVORITOS", 12f, color = TextoSuave, espaciado = 1.6f, lineas = 1)
            if (r.favoritos.isEmpty()) Detalle("Marca juegos como favoritos y aparecen aquí.", 12.5f, color = TextoSuave, lineas = 2)
            else Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                for (j in r.favoritos) Box(
                    Modifier.height(esc.d(48)).aspectRatio(0.78f).clip(RoundedCornerShape(esc.d(10))).background(Color.White.copy(alpha = 0.06f)),
                ) { ArteJuego(j, preferirArte = false, maxLado = 200, modifier = Modifier.fillMaxSize()) }
            }
        }
    }
}

/** Tus logros de DIPPO: cuántos llevas y los últimos trofeos que ganaste. */
@Composable
private fun LogrosDippoTile(r: ResumenPerfil, color: Color, modifier: Modifier) {
    val esc = LocalEscala.current
    val ganados = remember(r.logros) { Diptico.idsLogrosDippo() }
    val lista = remember(ganados) { LogrosDippo.LISTA.filter { it.id in ganados }.takeLast(5) }
    Column(
        modifier.clip(RoundedCornerShape(esc.d(16))).background(Color.White.copy(alpha = 0.07f))
            .clickable { Diptico.cerrarPerfil(); Diptico.abrirGaleria("logros_dippo") }
            .padding(horizontal = esc.d(12), vertical = esc.d(5)),
    ) {
        Texto("Logros de DIPPO", 11.5f, color = TextoSuave, lineas = 1)
        Texto("${r.logros} de ${r.logrosTotal}", 19f, color = TextoClaro, peso = FontWeight.Black, lineas = 1, traducir = false)
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(4)), modifier = Modifier.height(esc.d(16))) {
            if (lista.isEmpty()) Texto("Aún sin trofeos", 11.5f, color = color, lineas = 1)
            else for (l in lista) IconoDiptico(l.icono, Sol, 15f)
        }
    }
}

/** Tus logros de RetroAchievements: usuario, puntos y las insignias más recientes. */
@Composable
private fun LogrosRATile(e: Estado, color: Color) {
    val esc = LocalEscala.current
    val ra = e.perfilRA
    LaunchedEffect(e.usuarioRA) { if (ra == null && e.usuarioRA.isNotBlank()) Diptico.cargarPerfilRA() }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(16))).background(Color.White.copy(alpha = 0.07f))
            .clickable { Diptico.cerrarPerfil(); Diptico.abrirMenu(Seccion.G_CUENTAS) }
            .padding(horizontal = esc.d(12), vertical = esc.d(5)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10)),
    ) {
        IconoDiptico(Icono.TROFEO, Sol, 22f)
        Column(Modifier.weight(1f)) {
            Texto("RetroAchievements", 11.5f, color = TextoSuave, lineas = 1, traducir = false)
            when {
                e.usuarioRA.isBlank() -> Texto("Conéctalo en tu perfil", 13.5f, color = TextoClaro, lineas = 1)
                ra == null -> Texto("Cargando…", 13.5f, color = TextoClaro, lineas = 1)
                else -> Texto("${ra.usuario} · ${ra.puntos} pts", 15f, color = TextoClaro, peso = FontWeight.Black, lineas = 1, traducir = false)
            }
        }
        if (ra != null) Row(horizontalArrangement = Arrangement.spacedBy(esc.d(4))) {
            for ((_, l) in ra.recientes.take(4)) {
                val img = rememberImagen(android.net.Uri.parse(l.imagen), 96)
                Box(Modifier.size(esc.d(26)).clip(RoundedCornerShape(esc.d(6))).background(Color.White.copy(alpha = 0.08f))) {
                    if (img != null) Image(img, null, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, sub: String, color: Color, modifier: Modifier, traducirValor: Boolean = false) {
    val esc = LocalEscala.current
    Column(
        modifier.clip(RoundedCornerShape(esc.d(16))).background(Color.White.copy(alpha = 0.07f)).padding(horizontal = esc.d(12), vertical = esc.d(5)),
        verticalArrangement = Arrangement.spacedBy(esc.d(0)),
    ) {
        Texto(etiqueta, 11.5f, color = TextoSuave, lineas = 1)
        Texto(valor, 17.5f, color = TextoClaro, peso = FontWeight.Black, lineas = 1, traducir = traducirValor || valor == "Oculto")
        if (sub.isNotBlank()) Texto(sub, 11.5f, color = color, lineas = 1, traducir = sub == "jugando seguido" || sub == "juega hoy para empezar" || sub == "Se cambia en tu perfil")
    }
}

private fun horas(seg: Long): String = when {
    seg >= 36_000 -> "${seg / 3600} h"
    seg >= 3600 -> "%.1f h".format(java.util.Locale.ROOT, seg / 3600.0)
    else -> "${seg / 60} min"
}

/** Una vista chica de tu perfil, para el panel de la derecha de los ajustes. */
@Composable
internal fun PreviaPerfil(e: Estado) {
    val esc = LocalEscala.current
    val pf: Perfil = e.extra.perfil
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(pf.color).copy(alpha = 0.3f), Color.Transparent), radius = 700f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
            FotoPerfil(pf, 110f)
            Texto(e.nombre.ifBlank { "Tu nombre" }, 24f, lineas = 1, traducir = e.nombre.isBlank())
            if (pf.apodo.isNotBlank()) Detalle("@${pf.apodo}", 14f, lineas = 1)
            if (pf.nacimientoBonito().isNotBlank()) Detalle(pf.nacimientoBonito(), 13f, lineas = 1)
        }
    }
}



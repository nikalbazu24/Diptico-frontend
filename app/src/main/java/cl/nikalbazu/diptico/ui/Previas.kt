package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Opcion
import cl.nikalbazu.diptico.estado.Panel
import cl.nikalbazu.diptico.estado.Seccion

/*
 * Vistas previas de los ajustes: para cada opción del menú, la otra pantalla muestra en miniatura y en vivo
 * la parte de Díptico que cambia (la pantalla de arriba, la de abajo, las dos, un tema, una canción, la IA…).
 */

/** Medidas de diseño de cada pantalla de la Thor (en unidades del lienzo). */
private const val ARRIBA_AN = 711f
private const val ARRIBA_AL = 400f
private const val ABAJO_AN = 472f
private const val ABAJO_AL = 411f

/**
 * Dibuja [contenido] como si fuera una pantalla completa de [anchoU] x [altoU] y lo achica para que quepa.
 * No se puede tocar: es solo para mirar.
 */
@Composable
fun MiniPantalla(anchoU: Float, altoU: Float, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    val esc = LocalEscala.current
    val dens = LocalDensity.current
    val p = LocalPaleta.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val wPx = with(dens) { esc.d(anchoU).roundToPx() }.coerceAtLeast(1)
        val hPx = with(dens) { esc.d(altoU).roundToPx() }.coerceAtLeast(1)
        val escala = minOf(constraints.maxWidth / wPx.toFloat(), constraints.maxHeight / hPx.toFloat())
        Box(
            Modifier.layout { medible, _ ->
                val pl = medible.measure(Constraints.fixed(wPx, hPx))
                layout((wPx * escala).toInt(), (hPx * escala).toInt()) {
                    pl.placeWithLayer(0, 0) { scaleX = escala; scaleY = escala; transformOrigin = TransformOrigin(0f, 0f) }
                }
            }.clip(RoundedCornerShape(esc.d(10))).border(esc.d(1.5f), p.linea, RoundedCornerShape(esc.d(10))),
        ) {
            CompositionLocalProvider(LocalEscala provides Escala(esc.factor, anchoU, esc.texto)) {
                Box(Modifier.fillMaxSize().background(p.fondo)) {
                    contenido()
                    // Capa que se traga los toques: la vista previa no se puede usar.
                    Box(Modifier.fillMaxSize().sinCerrar())
                }
            }
        }
    }
}

/** El estado "limpio" para dibujar pantallas de muestra (sin el menú abierto encima, para no dibujarse a sí mismo). */
private fun limpio(e: Estado) = e.copy(
    panel = Panel.NINGUNO, enOpciones = false, editandoWidgets = false, bienvenida = 0L, continuacion = 0L,
    relojGrande = false, protector = false, aviso = null, intro = false,
)

/** Para "Lo esencial" y la búsqueda: la sección de donde viene cada opción. */
private fun seccionDe(o: Opcion?, s: Seccion): Seccion {
    if (o == null) return s
    // En «Más ajustes», cada opción es una sección: se muestra la vista previa de esa sección.
    if (s == Seccion.MAS) return Seccion.values().firstOrNull { it.titulo == o.titulo } ?: Seccion.SISTEMA
    if (s != Seccion.ESENCIAL) return s
    return Seccion.values().firstOrNull { sec -> sec != Seccion.ESENCIAL && sec != Seccion.MAS && !Diptico.esGrupoAjustes(sec) && Diptico.opcionesSeccion(sec).any { it.titulo == o.titulo } } ?: s
}

/** La vista previa de la opción [o] de la sección [s]. Siempre dibuja algo útil, aunque no haya opción elegida. */
@Composable
fun PreviaAjuste(e: Estado, s: Seccion, o: Opcion?, modifier: Modifier = Modifier) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val sec = seccionDe(o, s)
    val t = o?.titulo.orEmpty()
    val ev = limpio(e)
    Column(modifier) {
        Etiqueta("Vista previa", p.tenue, 11f)
        Spacer(Modifier.height(esc.d(6)))
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when (sec) {
                Seccion.TEMAS -> {
                    val tema = Diptico.todosLosTemas(e).firstOrNull { it.nombre == t }
                    if (tema != null) Marco { PreviaTema(tema, e.oscuro) }
                    else MiniPantalla(ARRIBA_AN, ARRIBA_AL, Modifier.fillMaxSize()) { PantallaSuperior(ev) }
                }
                Seccion.DECORACIONES -> Marco { PreviaTema(Estilo.tema, e.oscuro) }
                Seccion.APARIENCIA -> when {
                    t.startsWith("Efecto") || t == "Modo" -> Marco { PreviaBotonesGrande(e) }
                    t.contains("cono") -> MiniPantalla(ABAJO_AN, ABAJO_AL, Modifier.fillMaxSize()) { PantallaInferior(ev.copy(vistaConsolas = "arriba"), 1) }
                    else -> MiniPantalla(ARRIBA_AN, ARRIBA_AL, Modifier.fillMaxSize()) { PantallaSuperior(ev) }
                }
                Seccion.INICIO -> when {
                    t.startsWith("Bienvenida") || t == "Tu nombre" ->
                        MiniPantalla(ARRIBA_AN, ARRIBA_AL, Modifier.fillMaxSize()) { Bienvenida(ev.copy(bienvenida = 1L, bienvenidaBreve = e.bienvenidaModo == "breve", ausencia = 2 * 3_600_000L), Rol.SUPERIOR) }
                    t.contains("widget", true) || ev.inicio == "widgets" ->
                        MiniPantalla(ARRIBA_AN, ARRIBA_AL, Modifier.fillMaxSize()) { PantallaInicio(ev.copy(inicio = "widgets")) }
                    else -> MiniPantalla(ARRIBA_AN, ARRIBA_AL, Modifier.fillMaxSize()) { PantallaSuperior(ev) }
                }
                Seccion.ESTANTE, Seccion.PORTADAS -> MiniPantalla(ABAJO_AN, ABAJO_AL, Modifier.fillMaxSize()) { PantallaInferior(ev, 1) }
                Seccion.MUSICA -> Marco { PreviaMusica(e) }
                Seccion.CONSOLAS -> {
                    val c = Consolas.todas.firstOrNull { it.bonito == t }
                    MiniPantalla(ARRIBA_AN, ARRIBA_AL, Modifier.fillMaxSize()) { VistaSistema(ev.copy(consolaSel = c?.id ?: ev.consolaSel)) }
                }
                Seccion.ASISTENTE -> when {
                    t == "IA de DIPPO" -> MiniPantalla(ARRIBA_AN, ARRIBA_AL, Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize().padding(esc.d(24)), contentAlignment = Alignment.Center) { ExplicaIA(Diptico.motorEfectivo(e).let { if (it == "ninguno") "auto" else it }) } }
                    t.startsWith("Clave de") -> {
                        val prov = if (t.contains("Gemini")) "gemini" else "openai"
                        MiniPantalla(ARRIBA_AN, ARRIBA_AL, Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize().padding(esc.d(24)), contentAlignment = Alignment.Center) { ExplicaIA(prov) } }
                    }
                    t == "IA en tu consola" || t.contains("modelo") -> MiniPantalla(ARRIBA_AN, ARRIBA_AL, Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize().padding(esc.d(24)), contentAlignment = Alignment.Center) { ExplicaIA("local") } }
                    else -> MiniPantalla(ABAJO_AN, ABAJO_AL, Modifier.fillMaxSize()) { PanelAsistente(ev.copy(panel = Panel.ASISTENTE)) }
                }
                Seccion.LOGROS -> MiniPantalla(ABAJO_AN, ABAJO_AL, Modifier.fillMaxSize()) { PanelEstadisticas(ev) }
                Seccion.BIBLIOTECA -> MiniPantalla(ABAJO_AN, ABAJO_AL, Modifier.fillMaxSize()) { PanelCarpetas(ev.copy(panel = Panel.CARPETAS)) }
                Seccion.EMULADORES -> Marco { PreviaEmulador(t, o?.valor.orEmpty()) }
                Seccion.PANTALLAS -> PreviaThor(ev, t)
                Seccion.CONSOLA -> when {
                    t.contains("inicio", true) -> PreviaThor(ev, t)
                    else -> MiniPantalla(ARRIBA_AN, ARRIBA_AL, Modifier.fillMaxSize()) { PanelCentro(ev.copy(panel = Panel.CENTRO)) }
                }
                Seccion.SOBREMESA -> PreviaTele(ev)
                Seccion.CONTROLES -> Marco { PreviaBotonesExtra(ev) }
                Seccion.RENDIMIENTO -> Marco { PreviaTurbo(ev) }
                Seccion.IDIOMA -> Marco { PreviaIdioma(t) }
                Seccion.SISTEMA, Seccion.ESENCIAL, Seccion.MAS, Seccion.G_ESTILO, Seccion.G_TEMAS, Seccion.G_WIDGETS, Seccion.G_BIBLIOTECA, Seccion.G_JUEGOS, Seccion.G_CONTROLES, Seccion.G_SONIDO, Seccion.G_SISTEMA -> Marco { PreviaAcerca() }
            }
        }
    }
}

/** Un marco redondeado para las vistas previas dibujadas a mano. */
@Composable
private fun Marco(contenido: @Composable () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Box(
        Modifier.fillMaxSize().clip(RoundedCornerShape(esc.d(18))).border(esc.d(2), p.linea, RoundedCornerShape(esc.d(18))),
    ) { contenido() }
}

/**
 * Las dos pantallas de la consola, una sobre otra, con lo que muestra cada una.
 * Para "Atenuación" la de abajo se ve oscurecida como quedará mientras juegas.
 */
@Composable
private fun PreviaThor(e: Estado, titulo: String) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val jugando = titulo.startsWith("Atenuación")
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        // Pantalla de arriba
        MiniPantalla(ARRIBA_AN, ARRIBA_AL, Modifier.fillMaxWidth().weight(1.05f)) {
            if (jugando) Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF2B5876), Color(0xFF4E4376)))), contentAlignment = Alignment.Center) {
                Texto("Tu juego", 40f, color = Color.White)
            } else PantallaSuperior(e)
        }
        // Bisagra
        Box(Modifier.padding(vertical = esc.d(3)).width(esc.d(60)).height(esc.d(5)).clip(RoundedCornerShape(50)).background(p.linea))
        // Pantalla de abajo
        MiniPantalla(ABAJO_AN, ABAJO_AL, Modifier.fillMaxWidth().weight(1f)) {
            when {
                e.unaPantalla -> Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    Texto(if (e.modoPantallas == "una_libre") "Libre para otras apps" else "En negro", 26f, color = Color(0xFF55526F))
                }
                jugando -> Box(Modifier.fillMaxSize()) {
                    PantallaInferior(e.copy(sesion = cl.nikalbazu.diptico.estado.Sesion(e.seleccionado?.clave ?: "", System.currentTimeMillis() - 600_000, 0, false), despierta = true), 1)
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = (1f - e.atenuacion.coerceIn(0f, 1f)) * 0.92f)))
                }
                else -> PantallaInferior(e, 1)
            }
        }
    }
}

@Composable
private fun PreviaBotonesGrande(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column(
        Modifier.fillMaxSize().background(p.fondo2).padding(esc.d(18)),
        verticalArrangement = Arrangement.spacedBy(esc.d(14), Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.clip(RoundedCornerShape(50)).fondoBoton(Uva).padding(horizontal = esc.d(30), vertical = esc.d(12))) { Texto("▶  Jugar", 18f, color = SobreUva) }
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
            listOf(Uva, Coral, Sol).forEach { c -> Box(Modifier.size(esc.d(54), esc.d(36)).clip(RoundedCornerShape(esc.d(12))).fondoBoton(c)) }
        }
        Detalle("Efecto: ${Diptico.nombreEfecto(e.efecto)} · ${if (e.oscuro) "Oscuro" else "Claro"}", 13f, lineas = 1)
    }
}

/** Una consola y el emulador con que se abre. */
@Composable
private fun PreviaEmulador(titulo: String, valor: String) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val c = Consolas.todas.firstOrNull { titulo.contains(it.bonito) || titulo.contains(it.nombre) }
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf((c?.color ?: Uva).copy(alpha = 0.3f), p.fondo2))).padding(esc.d(18)),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        IconoConsola(c, 64f, p.fondo2)
        Spacer(Modifier.height(esc.d(10)))
        Texto(c?.bonito ?: titulo, 20f, lineas = 1)
        Spacer(Modifier.height(esc.d(10)))
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(p.superficie).padding(horizontal = esc.d(14), vertical = esc.d(7)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconoDiptico(Icono.EMULADOR, p.tinta, 18f, acento = Coral)
            Spacer(Modifier.width(esc.d(8)))
            Texto(valor.ifBlank { "Emulador" }, 14f, lineas = 1)
        }
    }
}

/** Cómo te habla Díptico en ese idioma. */
@Composable
private fun PreviaIdioma(titulo: String) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val cod = cl.nikalbazu.diptico.estado.Idioma.IDIOMAS.firstOrNull { it.second == titulo }?.first ?: cl.nikalbazu.diptico.estado.Idioma.actual
    val (hola, jugar) = when (cod) {
        "en" -> "Hi! Shall we play?" to "Play"
        "pt" -> "Oi! Vamos jogar?" to "Jogar"
        "zh" -> "你好！来玩吧？" to "开始游戏"
        "ja" -> "こんにちは！遊ぼう？" to "プレイ"
        else -> "¡Hola! ¿Jugamos?" to "Jugar"
    }
    Column(
        Modifier.fillMaxSize().background(p.fondo2).padding(esc.d(20)),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(12), Alignment.CenterVertically),
    ) {
        cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(54).value)
        Texto(hola, 22f, lineas = 1, traducir = false)
        Box(Modifier.clip(RoundedCornerShape(50)).fondoBoton(Uva).padding(horizontal = esc.d(22), vertical = esc.d(8))) { Texto("▶  $jugar", 15f, color = SobreUva, traducir = false) }
        Detalle(cl.nikalbazu.diptico.estado.Idioma.ayuda(cod), 12.5f, lineas = 1)
    }
}

/** Díptico y su versión. */
@Composable
private fun PreviaAcerca() {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Uva.copy(alpha = 0.25f), p.fondo2))).padding(esc.d(20)),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(10), Alignment.CenterVertically),
    ) {
        Logo(24f)
        Texto("Versión ${Diptico.VERSION}", 16f, lineas = 1)
        Detalle("Hecho con cariño para la AYN Thor", 12.5f, lineas = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
            listOf(Uva, Coral, Sol, Cielo).forEach { Box(Modifier.size(esc.d(10)).clip(CircleShape).background(it)) }
        }
    }
}


/** La tele con Díptico adentro: como se verá en modo sobremesa (tamaño y margen incluidos). */
@Composable
private fun PreviaTele(e: Estado) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(esc.d(10))).background(Color(0xFF0B0B0F))
                .border(esc.d(5), Color(0xFF26252E), RoundedCornerShape(esc.d(10))).padding(esc.d(5)),
        ) {
            val m = e.sobremesaMargen / 100f
            Box(Modifier.fillMaxSize().padding(horizontal = esc.d(220f * m), vertical = esc.d(124f * m))) {
                MiniPantalla(ARRIBA_AN / e.sobremesaTam, ARRIBA_AL / e.sobremesaTam, Modifier.fillMaxSize()) {
                    PantallaUnica(e.copy(modoPantallas = "una"))
                }
            }
        }
        // El pie de la tele.
        Box(Modifier.width(esc.d(70)).height(esc.d(6)).background(Color(0xFF26252E)))
        Box(Modifier.width(esc.d(130)).height(esc.d(5)).clip(RoundedCornerShape(50)).background(Color(0xFF26252E)))
        Spacer(Modifier.height(esc.d(8)))
        Detalle(if (e.sobremesa) "Activo ahora" + (e.teleConectada?.let { " · $it" } ?: "") else "Se activa al conectar la tele", 12f, color = p.tenue, lineas = 1)
    }
}

/** La consola vista desde atrás, con sus botones extra y lo que hace cada uno. */
@Composable
private fun PreviaBotonesExtra(e: Estado) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    Column(Modifier.fillMaxSize().padding(esc.d(14)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Etiqueta("Vista de atrás", p.tenue, 10.5f)
        Spacer(Modifier.height(esc.d(8)))
        Box(
            Modifier.fillMaxWidth(0.92f).aspectRatio(2.3f).clip(RoundedCornerShape(esc.d(40))).background(p.superficie)
                .border(esc.d(2), p.linea, RoundedCornerShape(esc.d(40))),
        ) {
            // Gatillos arriba.
            Row(Modifier.fillMaxWidth().padding(horizontal = esc.d(26)), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.width(esc.d(54)).height(esc.d(12)).clip(RoundedCornerShape(bottomEnd = esc.d(6), bottomStart = esc.d(6))).background(p.linea))
                Box(Modifier.width(esc.d(54)).height(esc.d(12)).clip(RoundedCornerShape(bottomEnd = esc.d(6), bottomStart = esc.d(6))).background(p.linea))
            }
            // Los botones de atrás.
            Row(Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = esc.d(20)), horizontalArrangement = Arrangement.SpaceBetween) {
                for (b in e.botonesExtra.take(2)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.width(esc.d(46)).height(esc.d(30)).clip(RoundedCornerShape(esc.d(10))).fondoBoton(Uva),
                            contentAlignment = Alignment.Center,
                        ) { Texto(b.nombre, 13f, color = SobreUva, lineas = 1, traducir = false) }
                        Spacer(Modifier.height(esc.d(4)))
                        Detalle(cl.nikalbazu.diptico.estado.BotonExtra.nombreAccion(b.accion), 10.5f, lineas = 2, modifier = Modifier.widthIn(max = esc.d(110)))
                    }
                }
            }
        }
    }
}


/** Un medidor de memoria: cuánto está libre ahora y lo que liberó el último turbo. */
@Composable
private fun PreviaTurbo(e: Estado) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val libres = remember { cl.nikalbazu.diptico.estado.Turbo.memoriaLibreMb(ctx) }
    val total = remember { cl.nikalbazu.diptico.estado.Turbo.memoriaTotalMb(ctx).coerceAtLeast(1) }
    val f = (libres.toFloat() / total).coerceIn(0f, 1f)
    Column(Modifier.fillMaxSize().padding(esc.d(16)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(esc.d(140)), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val g = size.minDimension * 0.1f
                val st = androidx.compose.ui.graphics.drawscope.Stroke(g, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                val o = androidx.compose.ui.geometry.Offset(g / 2, g / 2)
                val t = androidx.compose.ui.geometry.Size(size.width - g, size.height - g)
                drawArc(p.linea, 135f, 270f, false, o, t, style = st)
                drawArc(Brush.sweepGradient(listOf(Coral, Sol, Uva)), 135f, 270f * f, false, o, t, style = st)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Texto("${(f * 100).toInt()}%", 26f, lineas = 1, traducir = false)
                Detalle("libre", 11f, lineas = 1)
            }
        }
        Spacer(Modifier.height(esc.d(8)))
        Texto("$libres de $total MB", 14f, lineas = 1, traducir = false)
        if (e.turboUltimo.isNotBlank()) Detalle("Último turbo: ${e.turboUltimo}", 11.5f, lineas = 2)
        Spacer(Modifier.height(esc.d(6)))
        Box(Modifier.clip(RoundedCornerShape(50)).background(if (e.turbo) Coral else p.linea).padding(horizontal = esc.d(12), vertical = esc.d(4))) {
            Texto(if (e.turbo) "Turbo activado" else "Turbo apagado", 12f, color = Color.White, lineas = 1)
        }
    }
}


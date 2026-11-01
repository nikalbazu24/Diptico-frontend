package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.MapaJuego
import cl.nikalbazu.diptico.estado.Notas

/**
 * "¿Dónde quedaste?": al terminar de jugar (o desde la ventana del juego), cuatro formas rápidas:
 * escribirlo, decirlo en una nota de voz, marcarlo en el mapa del juego, o pasar de largo.
 */
@Composable
fun TarjetaQuedaste(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val j = e.seleccionado ?: return
    val d = e.datos[j.clave]
    val tieneVoz = j.clave in e.notasVoz
    val mapa = e.mapas[j.clave]
    val entra = remember { androidx.compose.animation.core.Animatable(0.9f) }
    LaunchedEffect(Unit) { entra.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 400f)) }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(interactionSource = null, indication = null) { Diptico.cerrarQuedaste() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.graphicsLayer { scaleX = entra.value; scaleY = entra.value; alpha = ((entra.value - 0.9f) / 0.1f).coerceIn(0f, 1f) }
                .widthIn(max = esc.d(560)).fillMaxWidth(0.94f).clip(RoundedCornerShape(esc.d(28))).background(p.fondo2)
                .border(esc.d(1.5f), Coral.copy(alpha = 0.4f), RoundedCornerShape(esc.d(28))).sinCerrar().padding(esc.d(18)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(esc.d(46)).clip(RoundedCornerShape(esc.d(10)))) { ArteJuego(j, preferirArte = false, maxLado = 160, modifier = Modifier.fillMaxSize()) }
                Spacer(Modifier.width(esc.d(12)))
                Column(Modifier.weight(1f)) {
                    Etiqueta(if (e.quedastePregunta) "Terminaste de jugar" else "Tu próximo paso", Coral, 11f)
                    Texto("¿Dónde quedaste?", 21f, lineas = 1)
                    Detalle(j.titulo, 12.5f, lineas = 1)
                }
            }
            if (!d?.paso.isNullOrBlank()) {
                Spacer(Modifier.height(esc.d(8)))
                Row(Modifier.clip(RoundedCornerShape(esc.d(12))).background(p.superficie.copy(alpha = 0.6f)).padding(horizontal = esc.d(10), vertical = esc.d(6)), verticalAlignment = Alignment.CenterVertically) {
                    Detalle("Antes anotaste:", 11.5f, lineas = 1)
                    Spacer(Modifier.width(esc.d(6)))
                    Texto(d?.paso.orEmpty(), 12.5f, lineas = 1, traducir = false)
                }
            }
            Spacer(Modifier.height(esc.d(12)))
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                val grab = e.grabando > 0L
                FormaQuedaste(Icono.LAPIZ, "Escribir", "Una nota corta", e.quedasteSel == 0, Uva, Modifier.weight(1f)) { Diptico.elegirQuedaste(0) }
                FormaQuedaste(
                    Icono.VOLUMEN, if (grab) "Grabando" else "Nota de voz",
                    if (grab) "A para terminar" else if (tieneVoz) "Reemplaza la anterior" else "Hasta 1 minuto",
                    e.quedasteSel == 1, Coral, Modifier.weight(1f), grabando = if (grab) e.grabando else 0L,
                ) { Diptico.elegirQuedaste(1) }
                FormaQuedaste(Icono.MUNDO, "En el mapa", if (mapa != null) "Mover el pin" else "Elige la imagen", e.quedasteSel == 2, Color(0xFF2EC4A6), Modifier.weight(1f)) { Diptico.elegirQuedaste(2) }
                FormaQuedaste(Icono.CONTINUAR, "Pasar de largo", "Ahora no", e.quedasteSel == 3, p.tenue, Modifier.weight(1f)) { Diptico.elegirQuedaste(3) }
            }
            Spacer(Modifier.height(esc.d(12)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pistas(
                    "← →" to "Elegir", "A" to "Usar",
                    *(if (tieneVoz) arrayOf("Y" to "Escuchar") else emptyArray<Pair<String, String>>()),
                    *(if (e.quedastePregunta) arrayOf("X" to "No preguntar más") else emptyArray<Pair<String, String>>()),
                )
            }
        }
    }
}

@Composable
private fun FormaQuedaste(icono: Icono, titulo: String, sub: String, elegida: Boolean, color: Color, modifier: Modifier, grabando: Long = 0L, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val k by animateFloatAsState(if (elegida) 1f else 0f, spring(dampingRatio = 0.6f), label = "forma")
    val pulso = rememberInfiniteTransition(label = "pulso")
    val r by pulso.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "r")
    var ahora by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(grabando) { while (grabando > 0L) { ahora = System.currentTimeMillis(); kotlinx.coroutines.delay(250) } }
    Column(
        modifier.graphicsLayer { scaleX = 1f + 0.04f * k; scaleY = 1f + 0.04f * k }.clip(RoundedCornerShape(esc.d(18)))
            .background(if (elegida) color.copy(alpha = 0.2f) else p.superficie.copy(alpha = 0.7f))
            .border(esc.d(if (elegida) 2f else 1f), if (elegida) color else p.linea, RoundedCornerShape(esc.d(18)))
            .clickable { alPulsar() }.padding(vertical = esc.d(12), horizontal = esc.d(6)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(esc.d(40)).graphicsLayer { if (grabando > 0L) { scaleX = r; scaleY = r } }.clip(CircleShape)
                .background(if (grabando > 0L) Color(0xFFE53935) else color.copy(alpha = if (elegida) 1f else 0.25f)),
            contentAlignment = Alignment.Center,
        ) { IconoDiptico(icono, Color.White, 20f) }
        Spacer(Modifier.height(esc.d(6)))
        Texto(
            if (grabando > 0L) formatoSeg((ahora - grabando) / 1000) else titulo, 13.5f, lineas = 1, alinear = TextAlign.Center,
            color = if (grabando > 0L) Color(0xFFE53935) else p.tinta, traducir = grabando == 0L,
        )
        Detalle(sub, 10.5f, lineas = 2, modifier = Modifier.padding(horizontal = esc.d(2)))
    }
}

private fun formatoSeg(s: Long) = "${s / 60}:${(s % 60).toString().padStart(2, '0')}"

/**
 * Marcar en el mapa: tu imagen del mapa del juego, con un pin que se mueve con la cruceta (o tocando).
 * A guarda, Y cambia la imagen, X quita el mapa, B cancela.
 */
@Composable
fun MarcarMapa(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val clave = e.marcandoMapa ?: return
    val m = e.mapas[clave] ?: return
    val titulo = e.juegos.firstOrNull { it.clave == clave }?.titulo ?: ""
    Box(Modifier.fillMaxSize().background(Color(0xF00B0A12)).sinCerrar()) {
        Column(Modifier.fillMaxSize().padding(esc.d(14))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.MUNDO, Color(0xFF2EC4A6), 20f)
                Spacer(Modifier.width(esc.d(8)))
                Column(Modifier.weight(1f)) {
                    Texto("Marca dónde quedaste", 18f, color = Color.White, lineas = 1)
                    Detalle(titulo, 12f, color = Color.White.copy(alpha = 0.6f), lineas = 1)
                }
            }
            Spacer(Modifier.height(esc.d(8)))
            MapaConPin(m.copy(x = e.pinX, y = e.pinY), Modifier.weight(1f).fillMaxWidth(), editable = true, maxLado = 1600)
            Spacer(Modifier.height(esc.d(8)))
            Pistas("✥" to "Mover", "A" to "Guardar", "Y" to "Otra imagen", "X" to "Quitar", "B" to "Cancelar")
        }
    }
}

/** La imagen del mapa entera (sin recortar) y el pin donde quedaste. [editable]: tocar mueve el pin. */
@Composable
fun MapaConPin(m: MapaJuego, modifier: Modifier, editable: Boolean = false, maxLado: Int = 600) {
    val esc = LocalEscala.current
    val img = rememberImagen(android.net.Uri.parse(m.uri), maxLado)
    val pulso = rememberInfiniteTransition(label = "pin")
    val anillo by pulso.animateFloat(0f, 1f, infiniteRepeatable(tween(1400)), label = "anillo")
    BoxWithConstraints(modifier.clip(RoundedCornerShape(esc.d(14))).background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
        if (img == null) { Girador(22f); return@BoxWithConstraints }
        // Dónde queda la imagen ajustada dentro del espacio (para poner el pin en el lugar justo).
        val ancho = maxWidth.value
        val alto = maxHeight.value
        val r = img.width.toFloat() / img.height.coerceAtLeast(1)
        val (iw, ih) = if (ancho / alto > r) (alto * r) to alto else ancho to (ancho / r)
        Box(
            Modifier.size(androidx.compose.ui.unit.Dp(iw), androidx.compose.ui.unit.Dp(ih))
                .then(if (editable) Modifier.pointerInput(iw, ih) {
                    detectTapGestures { o -> Diptico.ponerPin(o.x / size.width, o.y / size.height) }
                } else Modifier),
        ) {
            Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            val tam = if (editable) 34f else 18f
            Canvas(Modifier.fillMaxSize()) {
                val c = androidx.compose.ui.geometry.Offset(size.width * m.x, size.height * m.y)
                val u = esc.d(1f).toPx()
                drawCircle(Color(0xFFFF5D8F).copy(alpha = 0.5f * (1f - anillo)), radius = tam * u * (0.4f + anillo), center = c, style = Stroke(2.5f * u))
            }
            Box(
                Modifier.offset(androidx.compose.ui.unit.Dp(iw * m.x - tam / 2 * esc.factor), androidx.compose.ui.unit.Dp(ih * m.y - tam / 2 * esc.factor))
                    .size(esc.d(tam)).clip(CircleShape).background(Color.White).padding(esc.d(tam * 0.14f)).clip(CircleShape).background(Coral),
                contentAlignment = Alignment.Center,
            ) { if (editable) IconoDiptico(Icono.JUGAR, Color.White, tam * 0.4f) }
        }
    }
}

/**
 * Lo que dejaste anotado de un juego, además del texto: la nota de voz (para escucharla) y el mapa con el pin.
 * Va en la bienvenida, en la continuación y en "Tu próximo paso".
 */
@Composable
fun RecuerdosJuego(e: Estado, clave: String, mapaAlto: Float = 90f) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val voz = clave in e.notasVoz
    val mapa = e.mapas[clave]
    if (!voz && mapa == null) return
    val sonando by Notas.sonando.collectAsState()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
        if (voz) {
            val activa = sonando == clave
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(if (activa) Coral else p.superficie).clickable { Diptico.escucharNota(clave) }
                    .padding(horizontal = esc.d(12), vertical = esc.d(7)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconoDiptico(if (activa) Icono.CERRAR else Icono.JUGAR, if (activa) Color.White else Coral, 14f)
                Spacer(Modifier.width(esc.d(6)))
                Texto(if (activa) "Escuchando…" else "Tu nota de voz", 12.5f, color = if (activa) Color.White else p.tinta, lineas = 1)
            }
        }
        if (mapa != null) {
            MapaConPin(mapa, Modifier.size(esc.d(mapaAlto * 1.5f), esc.d(mapaAlto)).clickable { Diptico.abrirMapa(clave) })
        }
    }
}

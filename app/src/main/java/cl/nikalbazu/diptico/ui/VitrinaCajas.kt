package cl.nikalbazu.diptico.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/** Solo para las capturas de prueba: dibuja la vitrina sin el giro 3D (para comparar). */
internal var vitrinaSinGiro = false

/** La portada de arriba: "cajas" (la vitrina con las cajas de los juegos) o "arte" (el arte del juego en grande). */
val Estado.portadaCajas: Boolean get() = extra.portada == "cajas"

/**
 * La vitrina de cajas: el arte del juego elegido de fondo (difuminado, con la luz del color de su consola), su caja
 * grande al centro y las de al lado asomándose inclinadas, como en un estante. Al cambiar de juego las cajas se
 * deslizan con un resorte suave. Tocar la caja del centro abre el juego; tocar una de al lado la elige.
 */
@Composable
fun VitrinaCajas(e: Estado, juego: Juego) {
    val esc = LocalEscala.current
    val consola = Consolas.porId(juego.consolaId)
    val color = consola?.color ?: Uva
    val lista = e.visibles
    val indice = lista.indexOfFirst { it.clave == juego.clave }.coerceAtLeast(0)
    // La posición del estante es continua: así las cajas se deslizan entre juego y juego en vez de saltar.
    val pos = remember { Animatable(indice.toFloat()) }
    LaunchedEffect(indice, lista.size) {
        if (abs(pos.value - indice) > 6f) pos.snapTo(indice.toFloat())
        pos.animateTo(indice.toFloat(), spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))
    }
    // La vitrina siempre es oscura (las cajas lucen sobre la penumbra), así que la letra va clara en cualquier tema.
    val paleta = LocalPaleta.current
    CompositionLocalProvider(LocalPaleta provides paleta.copy(tinta = Color.White, tenue = Color.White.copy(alpha = 0.66f))) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF09080D)).testTag("vitrina-cajas")) {
            val ancho = maxWidth.value
            val alto = maxHeight.value
            // 1. Fondo: el arte del juego difuminado y oscurecido, con la luz de su consola desde abajo.
            Crossfade(juego, animationSpec = tween(if (Estilo.efectosCompletos) 450 else 0), label = "fondo-cajas") { j ->
                // Una versión chiquita del arte, estirada: queda suave como desenfocada sin el costo de un desenfoque en vivo.
                ArteJuego(
                    j, preferirArte = true, maxLado = 72,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.55f; scaleX = 1.1f; scaleY = 1.1f },
                )
            }
            // La luz de la consola sube desde abajo, como el foco de una vitrina.
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0.35f to Color.Transparent, 1f to color.copy(alpha = 0.45f),
            )))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.5f), 0.3f to Color.Transparent, 0.62f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.88f),
            )))

            // 2. Las cajas: la elegida al centro y hasta dos a cada lado.
            // Cada caja con la forma de su consola (la vitrina ignora la forma «para todas» del estante).
            if (consola != null && lista.isNotEmpty()) CompositionLocalProvider(LocalFormas provides e.formas) {
                // Qué cajas se dibujan depende del juego elegido (no de cada cuadro de la animación): así no se recompone todo
                // mientras se deslizan; el deslizamiento se lee solo al dibujar.
                val desde = (indice - 4).coerceAtLeast(0)
                val hasta = (indice + 4).coerceAtMost(lista.lastIndex)
                val altoCaja = alto * 0.5f
                val paso1 = ancho * 0.27f
                val paso2 = ancho * 0.12f
                Box(Modifier.fillMaxSize().padding(bottom = (alto * 0.12f).dp), contentAlignment = Alignment.Center) {
                    // De la más lejana a la más cercana: la del centro se dibuja al final, encima de todas.
                    val orden = (desde..hasta).sortedByDescending { abs(it - indice) }
                    for (i in orden) {
                        val j = lista[i]
                        key(j.clave) { CajaVitrina(j, i, pos, juego.clave, altoCaja, paso1, paso2, ancho) }
                    }
                }
            }

            // 3. Arriba: la consola a la izquierda; hora y batería a la derecha.
            Column(Modifier.align(Alignment.TopStart).padding(start = esc.d(26), top = esc.d(18))) {
                Texto((consola?.nombre ?: "").uppercase(), 16f, color = Color.White, peso = FontWeight.ExtraBold, espaciado = 1f, lineas = 1, traducir = false)
                Texto("${indice + 1} de ${lista.size}", 10.5f, color = Color.White.copy(alpha = 0.6f), lineas = 1, traducir = false, espaciado = 1.2f)
            }
            Box(Modifier.align(Alignment.TopEnd).padding(end = esc.d(22), top = esc.d(18))) { EstadoChico() }

            // 4. Abajo: el nombre del juego, una línea de datos y los botones (también se tocan).
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = esc.d(14), start = esc.d(24), end = esc.d(24)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(esc.d(4)),
            ) {
                Crossfade(juego, animationSpec = tween(260), label = "nombre-caja") { j ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Texto(j.titulo, 22f, color = Color.White, lineas = 1, traducir = false, alinear = TextAlign.Center)
                        val seg = e.datos[j.clave]?.tiempo?.toLong() ?: 0L
                        val linea = listOfNotNull(
                            consola?.bonito,
                            if (seg > 0) formatoTiempo(seg) + " " + cl.nikalbazu.diptico.estado.Idioma.t("jugadas") else null,
                            e.datos[j.clave]?.favorito?.takeIf { it }?.let { "♥" },
                        ).joinToString(" · ")
                        Texto(linea, 11.5f, color = Color.White.copy(alpha = 0.66f), lineas = 1, traducir = false, alinear = TextAlign.Center)
                    }
                }
                Row(Modifier.padding(top = esc.d(8)), horizontalArrangement = Arrangement.spacedBy(esc.d(10)), verticalAlignment = Alignment.CenterVertically) {
                    val jugado = (e.datos[juego.clave]?.tiempo?.toLong() ?: 0L) > 0L
                    BotonVitrina("A", if (jugado) "Continuar" else "Jugar", lleno = true) { Diptico.jugar() }
                    BotonVitrina("X", "Ficha", lleno = false) { Diptico.abrirFicha() }
                    BotonVitrina("Y", "DIPPO", lleno = false) { Diptico.abrirAsistente() }
                }
            }
        }
    }
}

/** Una caja del estante en el lugar [i]; [pos] es la posición animada del estante (se lee solo al dibujar). */
@Composable
private fun CajaVitrina(j: Juego, i: Int, pos: Animatable<Float, *>, elegida: String, altoCaja: Float, paso1: Float, paso2: Float, ancho: Float) {
    val esc = LocalEscala.current
    val c = Consolas.porId(j.consolaId) ?: return
    val base = cajaDe(c)
    val escala = minOf(altoCaja / base.alto, (ancho * 0.3f) / base.ancho) / esc.factor
    val sombra = esc.d(10)
    Box(
        Modifier
            .offset {
                val r = i - pos.value
                val ar = abs(r)
                val dx = (if (r < 0) -1f else 1f) * (ar.coerceAtMost(1f) * paso1 + (ar - 1f).coerceAtLeast(0f) * paso2)
                IntOffset((dx * density).roundToInt(), 0)
            }
            .graphicsLayer {
                val r = i - pos.value
                val ar = abs(r)
                val cerca = ar.coerceAtMost(1f)
                val lejos = (ar - 1f).coerceAtLeast(0f)
                if (!vitrinaSinGiro) {
                    rotationY = -r.coerceIn(-1f, 1f) * 34f
                    cameraDistance = 12f * density
                }
                val s = 1f - 0.26f * cerca - 0.1f * lejos
                scaleX = s; scaleY = s
                alpha = if (ar > 2.6f) 0f else (1f - 0.15f * cerca - 0.55f * lejos).coerceIn(0f, 1f)
                shadowElevation = sombra.toPx() * (1f - 0.5f * cerca)
                shape = RoundedCornerShape(esc.d(6))
                clip = true
            }
            .clickable(remember { MutableInteractionSource() }, indication = null) {
                if (j.clave == elegida) Diptico.jugar() else Diptico.elegirJuego(j.clave)
            },
    ) {
        CajaJuego(j, c, escala = escala, estuche = true)
        // Las de los costados quedan en penumbra para que mande la del centro.
        Box(Modifier.matchParentSize().drawBehind {
            val ar = abs(i - pos.value)
            val a = 0.38f * ar.coerceAtMost(1f) + 0.2f * (ar - 1f).coerceAtLeast(0f)
            if (a > 0.01f) drawRect(Color.Black.copy(alpha = a))
        })
    }
}

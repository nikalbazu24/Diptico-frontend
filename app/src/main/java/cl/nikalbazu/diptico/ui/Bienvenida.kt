package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Idioma

/** Saludo según la hora, con el nombre si lo hay. */
private fun saludoCon(nombre: String): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val base = when (h) { in 5..11 -> "Buenos días"; in 12..19 -> "Buenas tardes"; else -> "Buenas noches" }
    // Se traduce el saludo antes de sumarle el nombre (con el nombre ya no calza con la traducción).
    return if (nombre.isBlank()) base else "${Idioma.t(base)}, $nombre"
}

/** Cuánto tiempo estuviste fuera, dicho en palabras. */
private fun ausenciaTexto(ms: Long): String {
    val min = ms / 60_000
    return when {
        ms <= 0L -> "¡Qué bueno verte!"
        min < 60 -> "Volviste después de $min min"
        min < 60 * 24 -> "Volviste después de ${min / 60} h"
        else -> "Volviste después de ${min / (60 * 24)} días"
    }
}

/** Lo que dice Bisa al recibirte (cambia según la hora y lo que falta por hacer). */
private fun fraseBisa(e: Estado, tieneJuego: Boolean): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val frases = buildList {
        if (tieneJuego) { add("¿Seguimos donde quedamos?"); add("Tu partida te está esperando.") }
        add("¡Te estaba esperando!")
        if (h >= 23 || h < 5) add("Una partidita corta y a dormir, ¿ya?")
        if (h in 5..10) add("Buen día para jugar algo nuevo.")
        e.fiesta?.let { add(it.tema.mensaje.ifBlank { "¡Hoy es ${it.nombre}!" }) }
    }
    return frases[((System.currentTimeMillis() / 60_000) % frases.size).toInt()]
}

/**
 * Bienvenida al encender la consola: ocupa la pantalla principal unos segundos con un saludo, la hora,
 * el último juego para seguir y Bisa. Todo lo de fondo sigue funcionando. Cualquier botón la cierra; A sigue jugando.
 */
@Composable
fun Bienvenida(e: Estado, rol: Rol) {
    if (e.bienvenidaBreve) {
        if (rol != Rol.INFERIOR || e.unaPantalla) BienvenidaBreve(e)
        return
    }
    if (rol == Rol.INFERIOR && !e.unaPantalla) BienvenidaAbajo(e) else BienvenidaCompleta(e)
}

/** Aparece en cascada: cada parte entra un poquito después de la anterior. */
@Composable
private fun rememberEntrada(): Animatable<Float, *> {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
    return a
}

private fun Modifier.entra(t: Float, desde: Float): Modifier = this.graphicsLayer {
    val k = ((t - desde) / 0.35f).coerceIn(0f, 1f)
    alpha = k
    translationY = (1f - k) * 26f * density
}

@Composable
private fun BienvenidaCompleta(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val j = remember(e.bienvenida) { Diptico.ultimoJugado(e) }
    val entrada = rememberEntrada()
    val t = entrada.value
    val inf = rememberInfiniteTransition(label = "bienvenida")
    val deriva by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "deriva")
    val cuenta = remember(e.bienvenida) { Animatable(1f) }
    LaunchedEffect(e.bienvenida) { cuenta.animateTo(0f, tween(10_000, easing = LinearEasing)) }
    Box(
        Modifier.fillMaxSize().background(p.fondo).clickable(interactionSource = null, indication = null) { Diptico.cerrarBienvenida() },
    ) {
        // Fondo: el arte del último juego, difuminado, con luces suaves que se mueven.
        if (j != null) {
            ArteJuego(
                j, preferirArte = true, maxLado = 900,
                modifier = Modifier.fillMaxSize().then(if (Estilo.efectosCompletos) Modifier.blur(30.dp) else Modifier)
                    .graphicsLayer { alpha = 0.4f * t; scaleX = 1.1f + deriva * 0.05f; scaleY = 1.1f + deriva * 0.05f },
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to p.fondo, 0.55f to p.fondo.copy(alpha = 0.75f), 1f to p.fondo.copy(alpha = 0.35f))))
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(Uva.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(200f + deriva * 600f, 120f + deriva * 200f), radius = 900f,
                ),
            ),
        )
        // Luces de colores que flotan despacio, cada una a su ritmo.
        LucesFlotantes(t)
        Row(Modifier.fillMaxSize().padding(horizontal = esc.d(40), vertical = esc.d(30)), verticalAlignment = Alignment.CenterVertically) {
            // Izquierda: saludo, hora y datos.
            Column(Modifier.weight(1f)) {
                Etiqueta(Idioma.fecha("EEEEdMMMM").replaceFirstChar { it.uppercase() }, Coral, 13f, Modifier.entra(t, 0f))
                Spacer(Modifier.height(esc.d(6)))
                TextoPorPalabras(saludoCon(e.nombre), 34f, t, 0.06f)
                RelojQueCae(rememberHora(), 76f, p.tinta.copy(alpha = 0.92f))
                Spacer(Modifier.height(esc.d(8)))
                Row(Modifier.entra(t, 0.26f), horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                    val b = rememberBateria()
                    Chip(Icono.BATERIA, if (b >= 0) "$b%" else "—")
                    Chip(Icono.RELOJ, ausenciaTexto(e.ausencia))
                    val avisos = cl.nikalbazu.diptico.audio.Avisos.lista.value.size
                    if (avisos > 0) Chip(Icono.CAMPANA, "$avisos")
                }
            }
            Spacer(Modifier.width(esc.d(24)))
            // Derecha: seguir jugando.
            if (j != null) {
                val c = Consolas.porId(j.consolaId)
                val paso = e.datos[j.clave]?.paso.orEmpty()
                val gira = remember(e.bienvenida) { Animatable(1f) }
                LaunchedEffect(e.bienvenida) { kotlinx.coroutines.delay(380); gira.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.62f, stiffness = 140f)) }
                Column(
                    Modifier.widthIn(max = esc.d(300))
                        .graphicsLayer {
                            rotationY = 38f * gira.value; translationX = gira.value * 140f * density
                            alpha = (1f - gira.value * 1.2f).coerceIn(0f, 1f); cameraDistance = 14f * density
                        }
                        .clip(RoundedCornerShape(esc.d(26)))
                        .background(p.superficie.copy(alpha = 0.85f)).border(esc.d(1), Color.White.copy(alpha = 0.08f), RoundedCornerShape(esc.d(26)))
                        .clickable { Diptico.seguirDesdeBienvenida() }.padding(esc.d(18)),
                ) {
                    Etiqueta("Seguir jugando", p.tenue, 11f)
                    Spacer(Modifier.height(esc.d(10)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (c != null) CajaJuego(j, c, escala = minOf(110f / cajaDe(c).alto, 1f))
                        Spacer(Modifier.width(esc.d(14)))
                        Column(Modifier.weight(1f)) {
                            Etiqueta(c?.nombre ?: "", Coral, 10.5f)
                            Texto(j.titulo, 19f, lineas = 3, interlineado = 22f)
                        }
                    }
                    if (paso.isNotBlank()) {
                        Spacer(Modifier.height(esc.d(10)))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(esc.d(7)).clip(CircleShape).background(Sol))
                            Spacer(Modifier.width(esc.d(8)))
                            Detalle(paso, 13f, color = p.tinta, lineas = 2)
                        }
                    }
                    // La nota de voz y el mapa con el pin, si los dejaste.
                    if (j.clave in e.notasVoz || e.mapas.containsKey(j.clave)) {
                        Spacer(Modifier.height(esc.d(10)))
                        RecuerdosJuego(e, j.clave, 64f)
                    }
                    Spacer(Modifier.height(esc.d(12)))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).fondoBoton(Uva).padding(vertical = esc.d(9)),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(esc.d(22)).clip(CircleShape).background(SobreUva), contentAlignment = Alignment.Center) { Texto("A", 12f, color = Uva, traducir = false) }
                        Spacer(Modifier.width(esc.d(8)))
                        Texto("Continuar", 15f, color = SobreUva, lineas = 1)
                    }
                }
            }
        }
        // Bisa abajo a la izquierda con su frase.
        Row(Modifier.align(Alignment.BottomStart).padding(start = esc.d(36), bottom = esc.d(22)).entra(t, 0.5f), verticalAlignment = Alignment.CenterVertically) {
            cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(42).value)
            Spacer(Modifier.width(esc.d(10)))
            Box(Modifier.clip(RoundedCornerShape(esc.d(16))).background(p.superficie.copy(alpha = 0.9f)).padding(horizontal = esc.d(14), vertical = esc.d(8))) {
                TextoQueSeEscribe(remember(e.bienvenida) { fraseBisa(e, j != null) }, 14f, demora = 900)
            }
        }
        Row(Modifier.align(Alignment.BottomEnd).padding(end = esc.d(36), bottom = esc.d(26)).entra(t, 0.6f)) {
            Pistas(*(if (j != null) arrayOf("A" to "Continuar", "B" to "Explorar") else arrayOf("B" to "Explorar")))
        }
        // Una línea fina que se acorta: cuando se acaba, la bienvenida se va sola.
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(cuenta.value).height(esc.d(3)).background(Brush.horizontalGradient(listOf(Uva, Coral))))
    }
}

@Composable
private fun Chip(icono: Icono, texto: String) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(p.superficie.copy(alpha = 0.8f)).padding(horizontal = esc.d(11), vertical = esc.d(6)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconoDiptico(icono, p.tinta, 15f, acento = Coral)
        Spacer(Modifier.width(esc.d(6)))
        Texto(texto, 12.5f, lineas = 1, familia = Nunito)
    }
}

/** En la pantalla de abajo, durante la bienvenida: dos botones grandes para tocar. */
@Composable
private fun BienvenidaAbajo(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val j = remember(e.bienvenida) { Diptico.ultimoJugado(e) }
    val t = rememberEntrada().value
    Box(Modifier.fillMaxSize().background(p.fondo2).clickable(interactionSource = null, indication = null) { Diptico.cerrarBienvenida() }) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Coral.copy(alpha = 0.18f), Color.Transparent), radius = 700f)))
        Column(
            Modifier.fillMaxSize().padding(esc.d(22)),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(70).value, Modifier.entra(t, 0f))
            Spacer(Modifier.height(esc.d(10)))
            Texto(if (e.nombre.isBlank()) "¡Hola de nuevo!" else "¡Hola de nuevo, ${e.nombre}!", 22f, lineas = 1, modifier = Modifier.entra(t, 0.1f))
            Detalle(ausenciaTexto(e.ausencia), 13.5f, lineas = 1, modifier = Modifier.entra(t, 0.18f))
            Spacer(Modifier.height(esc.d(18)))
            if (j != null) {
                Row(
                    Modifier.fillMaxWidth().entra(t, 0.3f).clip(RoundedCornerShape(esc.d(20))).fondoBoton(Uva)
                        .clickable { Diptico.seguirDesdeBienvenida() }.padding(esc.d(14)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(esc.d(34)).clip(CircleShape).background(SobreUva), contentAlignment = Alignment.Center) {
                        IconoDiptico(Icono.JUGAR, Uva, 16f)
                    }
                    Spacer(Modifier.width(esc.d(12)))
                    Column(Modifier.weight(1f)) {
                        Etiqueta("Continuar", SobreUva.copy(alpha = 0.8f), 10.5f)
                        Texto(j.titulo, 16f, color = SobreUva, lineas = 1)
                    }
                }
                Spacer(Modifier.height(esc.d(8)))
            }
            Box(
                Modifier.fillMaxWidth().entra(t, 0.4f).clip(RoundedCornerShape(esc.d(20))).background(p.superficie)
                    .clickable { Diptico.cerrarBienvenida() }.padding(vertical = esc.d(14)),
                contentAlignment = Alignment.Center,
            ) { Texto("Explorar mis juegos", 15f, lineas = 1) }
        }
    }
}

/** La versión breve: un cartelito arriba que se va solo y no interrumpe nada. */
@Composable
private fun BienvenidaBreve(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val t = rememberEntrada().value
    Box(Modifier.fillMaxWidth().padding(top = esc.d(14)), contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier.graphicsLayer { alpha = (t * 3f).coerceAtMost(1f); translationY = (1f - (t * 3f).coerceAtMost(1f)) * -30f * density }
                .clip(RoundedCornerShape(50)).background(p.superficie.copy(alpha = 0.96f))
                .border(esc.d(1), Uva.copy(alpha = 0.4f), RoundedCornerShape(50))
                .clickable { Diptico.cerrarBienvenida() }
                .padding(start = esc.d(6), end = esc.d(18), top = esc.d(6), bottom = esc.d(6)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(32).value)
            Spacer(Modifier.width(esc.d(10)))
            Texto(saludoCon(e.nombre), 15f, lineas = 1)
            Spacer(Modifier.width(esc.d(10)))
            Texto(rememberHora(), 15f, color = p.acentoTexto, lineas = 1, traducir = false)
        }
    }
}

/**
 * Continuación: encendiste la consola en medio de un juego. En la pantalla libre aparece un recordatorio grande
 * de qué jugabas, cuánto llevas y tu próximo paso (encima del juego va un cartel chico).
 */
@Composable
fun Continuacion(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val s = e.sesion ?: return
    val j = e.juegos.firstOrNull { it.clave == s.clave } ?: return
    val c = Consolas.porId(j.consolaId)
    val t = rememberEntrada().value
    val minutos = ((System.currentTimeMillis() - s.inicio) / 60_000).coerceAtLeast(0)
    val paso = e.datos[s.clave]?.paso.orEmpty()
    Box(Modifier.fillMaxSize().background(p.fondo).clickable(interactionSource = null, indication = null) {
        Diptico.actualizar { it.copy(continuacion = 0L) }
    }) {
        ArteJuego(j, preferirArte = true, maxLado = 700, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.22f * t })
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, p.fondo))))
        Column(
            Modifier.align(Alignment.Center).padding(esc.d(24)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Etiqueta("Sigues jugando", Coral, 12f, Modifier.entra(t, 0f))
            Spacer(Modifier.height(esc.d(10)))
            if (c != null) Box(Modifier.entra(t, 0.08f)) { CajaJuego(j, c, escala = minOf(120f / cajaDe(c).alto, 1f)) }
            Spacer(Modifier.height(esc.d(12)))
            Texto(j.titulo, 22f, lineas = 2, alinear = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.entra(t, 0.16f))
            Detalle(if (minutos > 0) "Llevas $minutos min en esta partida" else "Recién empezaste esta partida", 13.5f, lineas = 1, modifier = Modifier.entra(t, 0.22f))
            if (paso.isNotBlank()) {
                Spacer(Modifier.height(esc.d(12)))
                Row(
                    Modifier.entra(t, 0.3f).clip(RoundedCornerShape(esc.d(16))).background(p.superficie).padding(horizontal = esc.d(14), vertical = esc.d(10)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconoDiptico(Icono.LAPIZ, Coral, 16f)
                    Spacer(Modifier.width(esc.d(8)))
                    Texto(paso, 14f, lineas = 2)
                }
            }
            if (s.clave in e.notasVoz || e.mapas.containsKey(s.clave)) {
                Spacer(Modifier.height(esc.d(12)))
                Box(Modifier.entra(t, 0.36f)) { RecuerdosJuego(e, s.clave, 96f) }
            }
        }
    }
}


/** Las palabras aparecen una tras otra, subiendo un poquito. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun TextoPorPalabras(texto: String, tam: Float, t: Float, desde: Float) {
    val palabras = Idioma.t(texto).split(' ')
    androidx.compose.foundation.layout.FlowRow {
        palabras.forEachIndexed { i, w ->
            Texto(w + " ", tam, lineas = 1, interlineado = tam * 1.12f, traducir = false, modifier = Modifier.entra(t, desde + i * 0.06f))
        }
    }
}

/** El reloj: cada dígito cae con un rebote al aparecer, y cuando cambia el minuto el dígito nuevo baja. */
@Composable
private fun RelojQueCae(hora: String, tam: Float, color: Color) {
    Row {
        hora.forEachIndexed { i, ch ->
            val cae = remember { Animatable(-1f) }
            LaunchedEffect(Unit) { kotlinx.coroutines.delay(160L + i * 70L); cae.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 260f)) }
            androidx.compose.animation.AnimatedContent(
                targetState = ch,
                transitionSpec = {
                    (androidx.compose.animation.slideInVertically(tween(380)) { -it } + androidx.compose.animation.fadeIn(tween(380))) togetherWith
                        (androidx.compose.animation.slideOutVertically(tween(300)) { it } + androidx.compose.animation.fadeOut(tween(300)))
                },
                label = "digito",
                modifier = Modifier.graphicsLayer { translationY = cae.value * tam * 1.4f * density; alpha = (1f + cae.value).coerceIn(0f, 1f) },
            ) { c -> Texto(c.toString(), tam, color = color, lineas = 1, interlineado = tam * 1.05f, traducir = false) }
        }
    }
}

/** Texto que se va escribiendo letra por letra (como si Bisa lo tecleara). */
@Composable
private fun TextoQueSeEscribe(texto: String, tam: Float, demora: Long = 0) {
    val texto = Idioma.t(texto)
    var n by androidx.compose.runtime.remember(texto) { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(texto) {
        kotlinx.coroutines.delay(demora)
        while (n < texto.length) { n++; kotlinx.coroutines.delay(28) }
    }
    Texto(texto.take(n) + if (n < texto.length) "▍" else "", tam, lineas = 2, traducir = false)
}

/** Tres luces suaves que flotan por el fondo. */
@Composable
private fun LucesFlotantes(t: Float) {
    val inf = rememberInfiniteTransition(label = "luces")
    val a by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = t }) {
        val w = size.width; val h = size.height
        drawCircle(Brush.radialGradient(listOf(Coral.copy(alpha = 0.22f), Color.Transparent), center = Offset(w * (0.75f - a * 0.2f), h * (0.25f + b * 0.2f)), radius = h * 0.5f), h * 0.5f, Offset(w * (0.75f - a * 0.2f), h * (0.25f + b * 0.2f)))
        drawCircle(Brush.radialGradient(listOf(Sol.copy(alpha = 0.14f), Color.Transparent), center = Offset(w * (0.2f + b * 0.25f), h * (0.85f - a * 0.2f)), radius = h * 0.42f), h * 0.42f, Offset(w * (0.2f + b * 0.25f), h * (0.85f - a * 0.2f)))
        drawCircle(Brush.radialGradient(listOf(Uva.copy(alpha = 0.18f), Color.Transparent), center = Offset(w * (0.5f + a * 0.15f), h * (0.55f - b * 0.15f)), radius = h * 0.35f), h * 0.35f, Offset(w * (0.5f + a * 0.15f), h * (0.55f - b * 0.15f)))
    }
}

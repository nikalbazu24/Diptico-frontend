package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Panel

/** Pantalla del juego: el juego elegido ocupa todo, con su información encima. Se adapta a pantalla ancha o angosta. */
@Composable
fun PantallaSuperior(e: Estado) {
    if (e.panel == Panel.MENU) {
        AyudaMenu(e)
        return
    }
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val angosta = esc.angosta
    val margen = if (angosta) 22f else 32f
    Box(Modifier.fillMaxSize().background(p.fondo)) {
        val juego = e.seleccionado
        if (juego == null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Logo(26f)
                Spacer(Modifier.height(esc.d(14)))
                Texto(if (e.escaneando) "Buscando tus juegos…" else "No encontré juegos en tu carpeta", 19f, color = p.tenue)
                if (!e.escaneando) {
                    Spacer(Modifier.height(esc.d(6)))
                    Detalle("Abre el menú con Start › Biblioteca para elegirla.", 15f)
                }
            }
            return@Box
        }
        Heroe(juego, if (angosta) 1f else 0.8f)
        Box(
            Modifier.fillMaxSize().background(
                if (angosta) Brush.verticalGradient(0f to p.fondo.copy(alpha = 0.35f), 0.45f to p.fondo.copy(alpha = 0.7f), 1f to p.fondo)
                else Brush.horizontalGradient(0f to p.fondo, 0.26f to p.fondo, 0.74f to Color.Transparent),
            ),
        )
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(esc.d(180))
                .background(Brush.verticalGradient(listOf(Color.Transparent, p.fondo))),
        )

        BarraSuperior(Modifier.align(Alignment.TopStart), inicio = margen)

        AnimatedContent(
            targetState = juego,
            transitionSpec = { (fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 10 }) togetherWith fadeOut(tween(180)) },
            label = "info",
            modifier = Modifier.padding(start = esc.d(margen), end = esc.d(margen), top = esc.d(58))
                .widthIn(max = esc.d(if (angosta) 460f else 400f)),
        ) { j -> InfoJuego(e, j, if (angosta) 32f else 38f) }

        Row(
            Modifier.align(Alignment.BottomStart).padding(start = esc.d(margen), bottom = esc.d(22)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(esc.d(16)),
        ) {
            BotonJugar()
            if (!angosta) Pista("Y", "Próximo paso")
            Pista("Start", "Menú")
        }
        if (e.sesion != null) {
            Etiqueta(
                "Jugando ahora", Coral, 13f,
                Modifier.align(Alignment.BottomEnd).padding(end = esc.d(margen), bottom = esc.d(34)),
            )
        }
    }
}

/** El arte del juego, a la derecha (o de fondo en pantallas angostas), con un cruce suave al cambiar. */
@Composable
fun Heroe(juego: Juego, ancho: Float) {
    Crossfade(targetState = juego, animationSpec = tween(650), label = "arte", modifier = Modifier.fillMaxSize()) { j ->
        Box(Modifier.fillMaxSize()) {
            ArteJuego(j, preferirArte = true, maxLado = 1600, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(ancho))
        }
    }
}

/** Consola, título (o logo), tiempo jugado y el próximo paso. */
@Composable
fun InfoJuego(e: Estado, j: Juego, tamTitulo: Float, conPaso: Boolean = true) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val c = Consolas.porId(j.consolaId)
    val datos = e.datos[j.clave]
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        if (c != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoMedio(c.medio, c.color, p.fondo, tam = 22f)
                Spacer(Modifier.width(esc.d(8)))
                Etiqueta(c.nombreLargo, p.tenue, 12.5f)
            }
        }
        val logo = rememberImagen(j.logo, 700)
        if (logo != null) {
            Image(
                logo, contentDescription = j.titulo, contentScale = ContentScale.Fit, alignment = Alignment.CenterStart,
                modifier = Modifier.heightIn(max = esc.d(tamTitulo * 2.4f)).widthIn(max = esc.d(320)),
            )
        } else {
            Texto(j.titulo, tamTitulo, lineas = 3, interlineado = tamTitulo * 1.06f, sombra = true)
        }
        Detalle(
            if ((datos?.tiempo ?: 0L) > 0) "${formatoTiempo(datos!!.tiempo)} jugadas" else "Aún sin jugar con Díptico",
            14f,
        )
        if (conPaso) {
            Spacer(Modifier.height(esc.d(2)))
            Row {
                Box(Modifier.padding(top = esc.d(6)).size(esc.d(9)).clip(CircleShape).background(Coral))
                Spacer(Modifier.width(esc.d(9)))
                val paso = datos?.paso.orEmpty()
                Column(Modifier.clickable { Diptico.abrirPaso() }) {
                    Texto("Tu próximo paso", 14f, color = p.acentoTexto)
                    Texto(
                        paso.ifEmpty { "Aún no anotas dónde quedaste. Aprieta Y para hacerlo." },
                        17f, peso = FontWeight.SemiBold, lineas = 3, interlineado = 22f,
                        color = if (paso.isEmpty()) p.tenue else p.tinta,
                    )
                }
            }
        }
    }
}

@Composable
fun BotonJugar() {
    val esc = LocalEscala.current
    Row(
        Modifier
            .shadow(esc.d(6), RoundedCornerShape(50), spotColor = Uva)
            .clip(RoundedCornerShape(50))
            .background(Uva)
            .clickable { Diptico.jugar() }
            .padding(start = esc.d(6), end = esc.d(20), top = esc.d(6), bottom = esc.d(6)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(esc.d(34)).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Texto("A", 15f, color = Uva)
        }
        Spacer(Modifier.width(esc.d(10)))
        Texto("¡Jugar!", 18f, color = Color.White)
    }
}

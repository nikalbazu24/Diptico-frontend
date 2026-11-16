package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import cl.nikalbazu.diptico.estado.*
import cl.nikalbazu.diptico.estado.Tema
import androidx.compose.foundation.shape.RoundedCornerShape as Redondeada

/**
 * Vista previa de un tema en cinco pantallas de Díptico (Inicio, Biblioteca, Widgets, Menús y Ficha), pintada con su
 * fondo, su trama, la forma de sus tarjetas, cómo marca lo elegido, su letra y su decoración.
 * Sirve para mirar antes de elegir; para verlo en las pantallas de verdad está «Probar 30 s».
 */
@Composable
fun PreviaTemaVista(t: Tema, oscuro: Boolean, vista: Int, animar: Boolean = false) {
    val pal = remember(t, oscuro) { paletaDe(t, oscuro) }
    val esc = LocalEscala.current
    CompositionLocalProvider(LocalPaleta provides pal) {
        Box(Modifier.fillMaxSize().background(pal.fondo)) {
            TramaTema(t)
            if (t.decor != "nada") Decoracion(t, animada = animar)
            Box(Modifier.fillMaxSize().padding(esc.d(12))) {
                when (vista) {
                    1 -> PreviaBiblioteca(t, pal)
                    2 -> PreviaWidgets(t, pal)
                    3 -> PreviaMenus(t, pal)
                    4 -> PreviaFicha(t, pal)
                    else -> PreviaInicio(t, pal)
                }
            }
        }
    }
}

private fun formaDe(t: Tema, dp: Dp): Shape = formaTema(dp, 0, Redondeada(dp), t.forma)

/** Una tarjeta del tema, elegida o no. */
@Composable
private fun Modifier.tarjeta(t: Tema, pal: Paleta, elegida: Boolean, radio: Dp): Modifier {
    val forma = formaDe(t, radio)
    val a = Color(t.acento)
    val base = this.clip(forma)
    return if (elegida) {
        if (t.forma in FORMAS_CON_ELEGIDO) base.fondoElegidoTema(a, t.forma)
        else base.background(Brush.linearGradient(listOf(a, lerp(a, Color(t.acento2), if (t.acabado == "liso") 0f else 1f))))
    } else base.background(pal.superficie).border(LocalEscala.current.d(1), pal.linea.copy(alpha = 0.5f), forma)
}

private fun sobre(c: Color) = if (c.luminance() > 0.55f) Tinta else Color.White

@Composable
private fun Titulo(t: Tema, texto: String, tam: Float, color: Color, modifier: Modifier = Modifier) =
    Texto(texto, tam, modifier, color = color, familia = fuenteTema(t.fuente), lineas = 1, traducir = false)

private val ARTES = listOf(0xFF1D3A8F to 0xFF3B2A7A, 0xFF8F3A1D to 0xFF3B1A2A, 0xFF1D8F6A to 0xFF16304A,
    0xFF8F7A1D to 0xFF4A2A16, 0xFF5A1D8F to 0xFF1A1640, 0xFF1D6A8F to 0xFF103040)

@Composable
private fun Arte(i: Int, modifier: Modifier) {
    val (a, b) = ARTES[i % ARTES.size]
    Box(modifier.background(Brush.linearGradient(listOf(Color(a), Color(b)))))
}

@Composable
private fun BoxScope.PreviaInicio(t: Tema, pal: Paleta) {
    val esc = LocalEscala.current
    val a = Color(t.acento)
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
            Detalle("PLAYSTATION 2", 8.5f, color = a, lineas = 1)
            Titulo(t, "Marea Lunar", 20f, pal.tinta)
            Detalle("12 h jugadas · vas en el 68 %", 9.5f, lineas = 1)
            Box(Modifier.tarjeta(t, pal, true, esc.d(14)).padding(horizontal = esc.d(12), vertical = esc.d(5))) {
                Titulo(t, "A · Continuar", 11f, sobre(a))
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                repeat(5) { i ->
                    Box(Modifier.size(esc.d(if (i == 0) 40 else 34)).tarjeta(t, pal, i == 0, esc.d(9)).padding(esc.d(3))) {
                        Arte(i, Modifier.fillMaxSize().clip(formaDe(t, esc.d(7))))
                    }
                }
            }
        }
        Box(Modifier.width(esc.d(78)).fillMaxHeight(0.72f).align(Alignment.Top)
            .clip(formaDe(t, esc.d(6))).border(esc.d(3), pal.superficie, formaDe(t, esc.d(6)))) {
            Arte(0, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun BoxScope.PreviaBiblioteca(t: Tema, pal: Paleta) {
    val esc = LocalEscala.current
    val consolas = listOf("PS2" to 0xFF2A4BA0, "Super Nintendo" to 0xFF7B5CC0, "Game Boy Advance" to 0xFF4A3FA8, "Switch" to 0xFFD23A2E,
        "PSP" to 0xFF333A48, "Nintendo DS" to 0xFF9AA0AA, "GameCube" to 0xFF5A4BB8, "Android" to 0xFF23A884)
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(7))) {
        Titulo(t, "Tu biblioteca", 15f, pal.tinta)
        consolas.chunked(4).forEachIndexed { fila, grupo ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(7))) {
                grupo.forEachIndexed { i, (nombre, color) ->
                    val elegida = fila == 0 && i == 0
                    Column(Modifier.weight(1f).fillMaxHeight().tarjeta(t, pal, elegida, esc.d(10)).padding(esc.d(6)),
                        verticalArrangement = Arrangement.spacedBy(esc.d(3))) {
                        Box(Modifier.size(esc.d(12)).clip(CircleShape).background(Color(color)))
                        Spacer(Modifier.weight(1f))
                        Titulo(t, nombre, 8.5f, if (elegida) sobre(Color(t.acento)) else pal.tinta)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.PreviaWidgets(t: Tema, pal: Paleta) {
    val esc = LocalEscala.current
    val a = Color(t.acento)
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(7))) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(7))) {
            Column(Modifier.weight(1f).fillMaxHeight().tarjeta(t, pal, false, esc.d(12)).padding(esc.d(8))) {
                Detalle("Domingo", 8.5f, lineas = 1)
                Titulo(t, "23:02", 22f, pal.tinta)
                Detalle("Batería 74 %", 8.5f, lineas = 1)
            }
            Column(Modifier.weight(1f).fillMaxHeight().tarjeta(t, pal, false, esc.d(12)).padding(esc.d(8)),
                verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
                Detalle("Tu progreso", 8.5f, lineas = 1)
                Titulo(t, "Marea Lunar", 10f, pal.tinta)
                Box(Modifier.fillMaxWidth().height(esc.d(5)).clip(CircleShape).background(pal.linea)) {
                    Box(Modifier.fillMaxWidth(0.68f).fillMaxHeight().background(a))
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight().tarjeta(t, pal, false, esc.d(12)).padding(esc.d(8))) {
                Detalle("DIPPO dice", 8.5f, lineas = 1)
                Detalle("¿Seguimos con el faro?", 9.5f, color = pal.tinta, lineas = 3)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(7))) {
            repeat(5) { i -> Box(Modifier.size(esc.d(30)).tarjeta(t, pal, i == 1, esc.d(9))) }
        }
    }
}

@Composable
private fun BoxScope.PreviaMenus(t: Tema, pal: Paleta) {
    val esc = LocalEscala.current
    val a = Color(t.acento)
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
        Column(Modifier.width(esc.d(84)), verticalArrangement = Arrangement.spacedBy(esc.d(4))) {
            Titulo(t, "Ajustes", 13f, pal.tinta)
            listOf("Estilo", "Biblioteca", "Juegos", "DIPPO", "Cuentas").forEachIndexed { i, n ->
                Box(if (i == 0) Modifier.fillMaxWidth().tarjeta(t, pal, true, esc.d(8)).padding(esc.d(5)) else Modifier.padding(esc.d(5))) {
                    Titulo(t, n, 9.5f, if (i == 0) sobre(a) else pal.tenue)
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
            listOf("Tienda de temas" to t.nombre, "Modo" to "Noche", "Escala" to "100 %", "Efectos" to "Equilibrado").forEachIndexed { i, (n, v) ->
                Row(Modifier.fillMaxWidth().tarjeta(t, pal, i == 0, esc.d(9)).padding(horizontal = esc.d(8), vertical = esc.d(6)),
                    verticalAlignment = Alignment.CenterVertically) {
                    Texto(n, 9.5f, Modifier.weight(1f), color = if (i == 0) sobre(a) else pal.tinta, lineas = 1, traducir = false, familia = NunitoBase)
                    Texto(v, 9f, color = if (i == 0) sobre(a) else pal.tenue, lineas = 1, traducir = false, familia = NunitoBase)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.PreviaFicha(t: Tema, pal: Paleta) {
    val esc = LocalEscala.current
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(7))) {
        Box(Modifier.fillMaxWidth().weight(0.9f).clip(formaDe(t, esc.d(10)))) {
            Arte(0, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
            Titulo(t, "Marea Lunar", 18f, Color.White, Modifier.align(Alignment.BottomStart).padding(esc.d(8)))
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(7))) {
            listOf("Tu progreso" to "12 h · 68 %", "De qué trata" to "Un faro y una marea", "Guías" to "3 guías").forEachIndexed { i, (n, d) ->
                Column(Modifier.weight(1f).fillMaxHeight().tarjeta(t, pal, i == 0, esc.d(10)).padding(esc.d(7))) {
                    Titulo(t, n, 9.5f, if (i == 0) sobre(Color(t.acento)) else pal.tinta)
                    Detalle(d, 8.5f, color = if (i == 0) sobre(Color(t.acento)).copy(alpha = 0.8f) else pal.tenue, lineas = 2)
                }
            }
        }
    }
}

/**
 * La otra pantalla de la tienda de temas: pestañas de vista (L2/R2), la previa grande, el nombre, lo que cambia
 * y los tres botones (A usar, Y probar 30 s, X personalizar).
 */
@Composable
fun VistaTiendaTema(e: Estado, item: Diptico.ItemGaleria, compacta: Boolean = false) {
    val tema = item.tema ?: return
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val probando = e.extra.temaPrueba == tema.id
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(5))) {
            VISTAS_PREVIA_TEMA.forEachIndexed { i, n ->
                val activa = i == e.extra.previaTema
                Box(Modifier.clip(CircleShape).background(if (activa) Uva else p.superficie)
                    .clickable { Diptico.fijarPreviaTema(i) }.padding(horizontal = esc.d(10), vertical = esc.d(4))) {
                    Texto(n, 11f, color = if (activa) SobreUva else p.tinta, lineas = 1)
                }
            }
            if (!compacta) {
                Spacer(Modifier.weight(1f))
                Detalle("L2 / R2 cambian la vista", 10.5f, lineas = 1)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().clip(Redondeada(esc.d(16)))) {
            PreviaTemaVista(tema, e.oscuro, e.extra.previaTema, animar = Estilo.efectosCompletos)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Texto(item.nombre, if (compacta) 15f else 20f, lineas = 1, familia = fuenteTema(tema.fuente))
                if (item.detalle.isNotBlank()) Detalle(item.detalle, if (compacta) 10.5f else 12f, lineas = if (compacta) 2 else 3)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
            BotonTienda("A", if (item.enUso) "En uso ✓" else "Usar", true, Modifier.weight(1f)) { Diptico.aceptar() }
            BotonTienda("Y", if (probando) "Quitar prueba" else "Probar 30 s", false, Modifier.weight(1f)) { Diptico.probarTema(tema.id) }
            BotonTienda("X", "Personalizar", false, Modifier.weight(1f)) { Diptico.personalizarTemaElegido() }
        }
    }
}

@Composable
private fun RowScope.BotonTienda(boton: String, texto: String, principal: Boolean, modifier: Modifier, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Row(modifier.clip(RoundedCornerShape(esc.d(12))).then(if (principal) Modifier.fondoBoton(Uva) else Modifier.background(p.superficie))
        .clickable(onClick = alPulsar).padding(vertical = esc.d(8), horizontal = esc.d(8)),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Texto("$boton · $texto", 12f, color = if (principal) SobreUva else p.tinta, lineas = 1)
    }
}

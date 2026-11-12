package cl.nikalbazu.diptico.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.*

/** Cuántas filas de acción tiene la pantalla de la caja (ver `usarCajaJuego`). */
private val ACCIONES = listOf(
    Triple("Elegir una foto y que DIPPO la arme", "Caja abierta (reverso, lomo y frente) o solo el frente", Icono.PORTADA),
    Triple("Buscar otra vez", "Comunidad, ScreenScraper y TheGamesDB", Icono.BUSCAR),
    Triple("Usar solo la portada", "Quita la caja propia o encontrada", Icono.BORRAR),
    Triple("Listo", "Volver", Icono.CHECK),
)

private val Verde = Color(0xFF23C486)
private val Ambar = Color(0xFFFFB454)

/**
 * La pantalla de la caja 3D de un juego: dónde se busca (comunidad, ScreenScraper, TheGamesDB) y, si no aparece,
 * cómo crearla con DIPPO desde una foto. Con dos pantallas, arriba gira la caja y abajo van las fuentes y las acciones.
 */
@Composable
fun PantallaCaja(e: Estado, rol: Rol) {
    val clave = e.extra.cajaJuego
    if (clave.isEmpty()) return
    val j = e.juegosPorClave[clave] ?: return
    val c = Consolas.porId(j.consolaId) ?: return
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val dividido = !e.unaPantalla && e.dosDisponibles
    val muestraCaja = rol == Rol.SUPERIOR || rol == Rol.UNICA || (rol == Rol.INFERIOR && !dividido)
    val muestraControles = rol == Rol.INFERIOR || rol == Rol.UNICA
    if (rol == Rol.INFERIOR && e.unaPantalla) return
    Box(
        Modifier.fillMaxSize().testTag("pantalla-caja").background(Color(0xF2080912))
            .clickable(remember { MutableInteractionSource() }, indication = null) { },
    ) {
        Row(Modifier.fillMaxSize().padding(esc.d(18)), horizontalArrangement = Arrangement.spacedBy(esc.d(18)), verticalAlignment = Alignment.CenterVertically) {
            if (muestraCaja) {
                Box(Modifier.weight(if (muestraControles) 0.9f else 1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    val giro = rememberInfiniteTransition(label = "caja-gira").animateFloat(
                        -38f, 38f, infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Reverse), label = "y",
                    )
                    Caja3D(
                        datosCaja(e, j, c), j, c, Modifier.fillMaxSize(0.92f),
                        giroY = { giro.value }, giroX = { -8f }, caras = e.extra.caras[clave],
                    )
                }
            }
            if (muestraControles) {
                Column(Modifier.weight(if (muestraCaja) 1.1f else 1f).widthIn(max = esc.d(520)).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    Etiqueta(c.bonito, Coral, 11f)
                    Texto(j.titulo, 20f, lineas = 1)
                    Spacer(Modifier.height(esc.d(2)))
                    Detalle(resumenCaja(e, clave), 12.5f, lineas = 2)
                    Spacer(Modifier.height(esc.d(10)))
                    Etiqueta("¿Dónde la busco?", p.tenue, 11f)
                    Spacer(Modifier.height(esc.d(4)))
                    val f = e.extra.cajaFuentes
                    FuenteCaja("Comunidad de Díptico", "Cajas que armó otra gente", f.getOrElse(0) { "" })
                    FuenteCaja("ScreenScraper", "Escaneos de cajas reales", f.getOrElse(1) { "" })
                    FuenteCaja("TheGamesDB", "Frente y reverso", f.getOrElse(2) { "" })
                    Spacer(Modifier.height(esc.d(8)))
                    Etiqueta("Si no la encuentro, la creamos con DIPPO", Coral, 11f)
                    Spacer(Modifier.height(esc.d(4)))
                    Column(verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
                        ACCIONES.forEachIndexed { i, (t, d, ic) ->
                            val act = e.extra.cajaSel == i
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(14)))
                                    .then(if (act) Modifier.fondoBoton(Uva) else Modifier.background(p.fondo2.copy(alpha = 0.7f)))
                                    .clickable { Diptico.usarCajaJuego(i) }.padding(horizontal = esc.d(12), vertical = esc.d(7)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconoDiptico(ic, if (act) SobreUva else p.tinta, 20f, acento = if (act) SobreUva else Coral)
                                Spacer(Modifier.width(esc.d(10)))
                                Column(Modifier.weight(1f)) {
                                    Texto(t, 13.5f, color = if (act) SobreUva else p.tinta, lineas = 1)
                                    Detalle(d, 11f, color = if (act) SobreUva.copy(alpha = .8f) else p.tenue, lineas = 1)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(esc.d(6)))
                    Pistas("A" to "Elegir", "Cruceta" to "Moverte", "B" to "Volver")
                }
            }
        }
    }
}

private fun resumenCaja(e: Estado, clave: String): String {
    val g = e.extra.caras[clave]
    return when {
        g == null -> "Todavía no hay caja guardada: se ve con la portada."
        g.frente != null && g.reverso != null -> "Tiene frente, lomo y reverso reales."
        g.frente != null || g.reverso != null -> "Tiene parte de la caja. El resto lo arma DIPPO con la ficha."
        else -> "No se encontró caja: se ve con la portada."
    }
}

@Composable
private fun FuenteCaja(nombre: String, detalle: String, estado: String) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val (texto, color) = when (estado) {
        "buscando" -> "Buscando…" to Ambar
        "hallada" -> "¡La encontré!" to Verde
        "no" -> "No está" to p.tenue
        "sin" -> "No conectada" to p.tenue
        else -> "" to p.tenue
    }
    Row(Modifier.fillMaxWidth().padding(vertical = esc.d(2)), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Texto(nombre, 13f, lineas = 1)
            Detalle(detalle, 11f, lineas = 1)
        }
        if (texto.isNotEmpty()) Texto(texto, 12.5f, color = color, lineas = 1)
    }
}

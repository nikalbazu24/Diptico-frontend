package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono

/** Un único propietario por pantalla: los paneles y sus transiciones quedan debajo. */
internal val LocalBarraSistema = staticCompositionLocalOf { false }
internal const val ALTO_BARRA_SISTEMA = 26f

@Composable
internal fun BarraSistema(e: Estado, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val p = LocalPaleta.current
    val hora = rememberHora()
    val bateria = rememberBateria()
    val cargando = rememberCargando()
    Row(modifier.fillMaxWidth().height(esc.d(ALTO_BARRA_SISTEMA)).background(p.fondo2)
        .padding(horizontal = esc.d(14)).testTag("barra-sistema"), verticalAlignment = Alignment.CenterVertically) {
        Logo(10f)
        Spacer(Modifier.width(esc.d(10)))
        PildoraTrabajos(e.trabajos)
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(esc.d(22)).clickable(onClickLabel = "Abrir notificaciones") { Diptico.abrirCentro("avisos") }, contentAlignment = Alignment.Center) {
            IconoDiptico(Icono.CAMPANA, p.tenue, 12f)
        }
        Spacer(Modifier.width(esc.d(8)))
        Texto(hora, 11.5f, traducir = false, lineas = 1,
            modifier = Modifier.testTag("hora-sistema").clickable(onClickLabel = "Abrir reloj") { Diptico.alternarReloj() })
        Spacer(Modifier.width(esc.d(12)))
        Row(Modifier.testTag("bateria-sistema").semantics {
            contentDescription = if (bateria >= 0) "Batería $bateria por ciento" else "Batería no disponible"
        }, verticalAlignment = Alignment.CenterVertically) {
            IconoDiptico(Icono.BATERIA, if (bateria in 0..15) Coral else p.tinta, 13f, acento = if (cargando) Sol else p.tenue)
            Spacer(Modifier.width(esc.d(4)))
            Texto(if (bateria >= 0) "$bateria%" else "—", 11.5f, traducir = false, lineas = 1)
        }
    }
}

package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.VistaPartida

@Composable
internal fun FilaVistaPartida(vista: VistaPartida, acento: Color, elegir: (VistaPartida) -> Unit) {
    val esc = LocalEscala.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(5))) {
        VistaPartida.entries.forEach { opcion ->
            val activa = opcion == vista
            val forma = RoundedCornerShape(esc.d(12))
            Box(Modifier.weight(1f).clip(forma)
                .background(if (activa) acento.copy(alpha = .24f) else Color.White.copy(alpha = .07f))
                .then(if (activa) Modifier.border(esc.d(1), acento, forma) else Modifier)
                .clickable { elegir(opcion) }.padding(vertical = esc.d(8)),
                contentAlignment = Alignment.Center) {
                Texto(opcion.titulo, 11.5f, color = Color.White, lineas = 1)
            }
        }
    }
}

@Composable
internal fun MesaPartida(e: Estado, clave: String, vista: VistaPartida, modifier: Modifier) {
    val esc = LocalEscala.current
    val tenue = Color(0xFFC1BBD6)
    when (vista) {
        VistaPartida.BITACORA -> Column(modifier, verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
            val mapa = e.mapas[clave]
            if (mapa != null) MapaConPin(mapa, Modifier.weight(1f).fillMaxWidth(), animar = false)
            else Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AccionMesa("Elegir mapa", Icono.MUNDO, Modifier) { Diptico.abrirMapa(clave) }
            }
            val paso = e.datos[clave]?.paso.orEmpty()
            Row(Modifier.fillMaxWidth().padding(bottom = esc.d(6)), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                Column(Modifier.weight(1f)) {
                    Etiqueta("Tu próximo paso", tenue, 10f)
                    Texto(paso.ifBlank { "Todavía no anotaste un paso" }, 14f, lineas = 2, color = Color.White)
                }
                if (mapa != null) AccionMesa("Marcar mapa", Icono.MUNDO, Modifier) { Diptico.abrirMapa(clave) }
            }
        }
        VistaPartida.HERRAMIENTAS -> Column(modifier, verticalArrangement = Arrangement.Center) {
            Etiqueta("Herramientas de sesión", tenue, 12f)
            Spacer(Modifier.height(esc.d(8)))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                val temporizador = if (e.temporizadorFin > 0L) "Detener aviso" else "Aviso en ${e.temporizadorMin} min"
                AccionMesa(temporizador, Icono.RELOJ, Modifier.weight(1f)) { Diptico.alternarTemporizador() }
                val crono = if (e.cronoInicio > 0L) "Pausar cronómetro" else "Iniciar cronómetro"
                AccionMesa(crono, Icono.RELOJ, Modifier.weight(1f)) { Diptico.alternarCrono() }
            }
            Spacer(Modifier.height(esc.d(6)))
            Detalle("Los avisos y el cronómetro se comparten con los widgets de Inicio.", 11f, color = tenue, lineas = 2)
        }
        else -> Box(modifier, contentAlignment = Alignment.Center) {
            val titulo = e.juegos.firstOrNull { it.clave == clave }?.titulo ?: "En partida"
            Texto(titulo, 19f, color = tenue, lineas = 2)
        }
    }
}

@Composable
private fun AccionMesa(titulo: String, icono: Icono, modifier: Modifier, accion: () -> Unit) {
    val esc = LocalEscala.current
    Row(modifier.clip(RoundedCornerShape(esc.d(12))).background(Color.White.copy(alpha = .10f))
        .clickable(onClick = accion).padding(horizontal = esc.d(10), vertical = esc.d(9)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
        IconoDiptico(icono, Coral, 15f)
        Texto(titulo, 12f, color = Color.White, lineas = 1)
    }
}

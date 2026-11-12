package cl.nikalbazu.diptico.ui

import cl.nikalbazu.diptico.estado.*

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
private fun AccionMesa(titulo: String, icono: Icono, modifier: Modifier, accion: () -> Unit) {
    val esc = LocalEscala.current
    Row(modifier.clip(RoundedCornerShape(esc.d(12))).background(Color.White.copy(alpha = .10f))
        .clickable(onClick = accion).padding(horizontal = esc.d(10), vertical = esc.d(9)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
        IconoDiptico(icono, Coral, 15f)
        Texto(titulo, 12f, color = Color.White, lineas = 1)
    }
}

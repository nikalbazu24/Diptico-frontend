package cl.nikalbazu.diptico.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Panel

/**
 * Dock de apps en la pantalla de abajo: una manija discreta en el borde elegido que, al tocarla, despliega tus
 * apps fijadas (de 3 a 6) y un acceso al cajón con todas las apps. Se cierra solo al abrir una app.
 */
@Composable
fun BoxScope.DockApps(e: Estado) {
    val x = e.extra
    if (!x.dock || e.sesion != null || e.panel != Panel.NINGUNO || !e.configurado) return
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val mov = ajusteMovimiento()
    val apps = e.appsFijadas.take(x.dockCantidad)
    val icono = when (x.dockTam) { "chico" -> 34f; "grande" -> 54f; else -> 44f }
    val alinear = when (x.dockLado) { "izquierda" -> Alignment.CenterStart; "derecha" -> Alignment.CenterEnd; else -> Alignment.BottomCenter }
    val vertical = x.dockLado != "abajo"
    Box(Modifier.align(alinear).padding(esc.d(8))) {
        // La manija: siempre a mano, casi invisible.
        AnimatedVisibility(!x.dockAbierto, enter = entradaFlotante(mov), exit = salidaFlotante(mov)) {
            Box(
                Modifier.clip(RoundedCornerShape(50)).clickable { Diptico.abrirDock(true) }
                    .padding(esc.d(10)),
            ) {
                Box(Modifier.size(if (vertical) esc.d(5) else esc.d(44), if (vertical) esc.d(44) else esc.d(5))
                    .clip(RoundedCornerShape(50)).background(p.tinta.copy(alpha = 0.3f)))
            }
        }
        AnimatedVisibility(x.dockAbierto, enter = entradaFlotante(mov), exit = salidaFlotante(mov)) {
            val forma = RoundedCornerShape(esc.d(26))
            val contenido: @Composable () -> Unit = {
                apps.forEach { pkg ->
                    Box(
                        Modifier.clip(RoundedCornerShape(esc.d(14))).clickable { Diptico.abrirDock(false); Diptico.abrirPaquete(pkg) }
                            .padding(esc.d(4)),
                    ) { IconoApp(pkg, icono) }
                }
                // Cajón: todas las apps.
                Box(
                    Modifier.size(esc.d(icono + 8f)).clip(CircleShape).background(p.superficie)
                        .clickable { Diptico.abrirDock(false); Diptico.abrirApps() },
                    contentAlignment = Alignment.Center,
                ) { IconoDiptico(Icono.APPS, p.tinta, icono * 0.45f) }
                Box(
                    Modifier.size(esc.d(28)).clip(CircleShape).clickable { Diptico.abrirDock(false) },
                    contentAlignment = Alignment.Center,
                ) { IconoDiptico(Icono.CERRAR, p.tenue, 13f) }
            }
            val base = Modifier.shadow(esc.d(14), forma).clip(forma).fondoBarra("difusa")
                .border(esc.d(1), Color.White.copy(alpha = 0.1f), forma).padding(esc.d(8))
            if (vertical) Column(base, verticalArrangement = Arrangement.spacedBy(esc.d(6)), horizontalAlignment = Alignment.CenterHorizontally) { contenido() }
            else Row(base, horizontalArrangement = Arrangement.spacedBy(esc.d(6)), verticalAlignment = Alignment.CenterVertically) { contenido() }
        }
    }
}

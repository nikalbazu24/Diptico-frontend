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
import androidx.compose.ui.platform.testTag
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

/** Máximo de apps en el dock: cinco, para que quepan a la vista sin tapar nada. */
const val MAX_DOCK = 5

/**
 * Dock de apps fijo: hasta cinco de tus apps fijadas, siempre a la vista en la pantalla de abajo (también mientras
 * juegas), y al final el cajón con todas. La app se abre en la misma pantalla donde la tocaste, así el juego sigue
 * en la otra. Sin apps fijadas muestra un botón para elegirlas.
 */
@Composable
fun FilaDock(e: Estado, tam: Float, modifier: Modifier = Modifier, conCajon: Boolean = true, flotante: Boolean = false) {
    if (!e.extra.dock) return
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val vista = androidx.compose.ui.platform.LocalView.current
    val apps = e.appsFijadas.take(MAX_DOCK)
    Row(modifier.testTag("dock-apps"), horizontalArrangement = Arrangement.spacedBy(esc.d(4)), verticalAlignment = Alignment.CenterVertically) {
        apps.forEach { pkg ->
            Box(
                Modifier.then(if (flotante) Modifier.shadow(esc.d(8), RoundedCornerShape(esc.d(12)), clip = false) else Modifier)
                    .clip(RoundedCornerShape(esc.d(12))).clickable { Diptico.abrirPaqueteAqui(pkg, vista.display?.displayId) }
                    .padding(esc.d(3)),
            ) { IconoApp(pkg, tam, desnudo = true) }
        }
        if (apps.isEmpty()) Row(
            Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.07f))
                .clickable { Diptico.abrirApps(); Diptico.avisar("Fija hasta 5 apps con Y y quedan en tu dock") }
                .padding(horizontal = esc.d(10), vertical = esc.d(6)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconoDiptico(Icono.APPS, p.tinta, 13f)
            androidx.compose.foundation.layout.Spacer(Modifier.size(esc.d(6)))
            Texto("Fija tus apps", 12f, color = p.tinta, lineas = 1)
        } else if (conCajon) Box(
            Modifier.size(esc.d(tam + 6f)).then(if (flotante) Modifier.shadow(esc.d(6), CircleShape, clip = false) else Modifier)
                .clip(CircleShape).background(p.tinta.copy(alpha = 0.10f))
                .clickable { Diptico.abrirApps() },
            contentAlignment = Alignment.Center,
        ) { IconoDiptico(Icono.APPS, p.tinta, tam * 0.45f) }
    }
}

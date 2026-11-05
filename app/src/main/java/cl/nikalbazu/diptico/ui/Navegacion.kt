package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import cl.nikalbazu.diptico.estado.*

fun Modifier.pellizcoWidgets(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var zoom = 1f; var multiple = false
        while (true) {
            val ev = awaitPointerEvent(PointerEventPass.Initial)
            val dedos = ev.changes.count { it.pressed }
            if (dedos == 0) break
            if (dedos >= 2) { multiple = true; zoom *= ev.calculateZoom(); ev.changes.forEach { it.consume() } }
        }
        if (multiple && zoom > 1.18f) Diptico.cambiarDensidadWidgets(true)
        else if (multiple && zoom < .85f) Diptico.cambiarDensidadWidgets(false)
    }
}

@Composable
fun MenuApp(e: Estado) {
    val p = LocalPaleta.current; val esc = LocalEscala.current
    val app = e.appsTodas.firstOrNull { it.paquete == e.appsMenu }
    val opciones = Diptico.opcionesApp(e)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .65f)).clickable { Diptico.cerrarOpcionesApp() }, contentAlignment = Alignment.Center) {
        Column(Modifier.padding(esc.d(16)).widthIn(max = esc.d(420)).fillMaxWidth().heightIn(max = esc.d(360))
            .clip(RoundedCornerShape(esc.d(24))).background(p.fondo2).sinCerrar().padding(esc.d(16))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                app?.let { IconoApp(it.paquete, 32f); Spacer(Modifier.width(esc.d(10))) }
                Texto(app?.nombre ?: "Opciones de app", 19f, lineas = 1, modifier = Modifier.weight(1f))
                Box(Modifier.clickable { Diptico.cerrarOpcionesApp() }.padding(esc.d(8))) { IconoDiptico(Icono.CERRAR, p.tinta, 18f) }
            }
            Spacer(Modifier.height(esc.d(10)))
            LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
                itemsIndexed(opciones) { i, o ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(14)))
                        .background(if (i == e.appsMenuIndice) Uva.copy(alpha = .25f) else p.superficie)
                        .border(esc.d(if (i == e.appsMenuIndice) 2 else 0), if (i == e.appsMenuIndice) Uva else Color.Transparent, RoundedCornerShape(esc.d(14)))
                        .clickable { o.accion() }.padding(esc.d(12)), verticalAlignment = Alignment.CenterVertically) {
                        o.icono?.let { IconoDiptico(it, if (o.titulo == "Desinstalar") Coral else p.tinta, 18f); Spacer(Modifier.width(esc.d(10))) }
                        Texto(o.titulo, 14f, lineas = 1)
                    }
                }
            }
            Spacer(Modifier.height(esc.d(8))); Pistas("✥" to "Elegir", "A" to "Aceptar", "B" to "Cerrar")
        }
    }
}

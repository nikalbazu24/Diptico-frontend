package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.estado.Diptico.ItemGaleria
import cl.nikalbazu.diptico.ui.WIDGETS_VIVOS
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** La pestaña «Widgets» de la tienda: los widgets nuevos, marcados si ya están en tu inicio. */
internal fun Diptico.itemsTiendaWidgets(e: Estado): List<ItemGaleria> = WIDGETS_VIVOS.map { tipo ->
    val puesto = e.widgets.any { it.tipo == tipo && it.visible }
    ItemGaleria(tipo, Widgets.nombre(tipo), Widgets.descripcion(tipo) + if (puesto) " · Ya está en tu inicio" else "", puesto) {
        ponerWidgetDesdeTienda(tipo)
    }
}

/** A en la tienda: pone el widget en el inicio (en el primer hueco) o lo quita si ya estaba. */
fun Diptico.ponerWidgetDesdeTienda(tipo: String) {
    val e = estado.value
    val i = e.widgets.indexOfFirst { it.tipo == tipo }
    if (i < 0) return
    val estaba = e.widgets[i].visible
    alternarWidget(i)
    val ahora = estado.value.widgets.getOrNull(i)?.visible == true
    if (ahora != estaba) avisar(if (ahora) "${Widgets.nombre(tipo)} quedó en tu inicio" else "${Widgets.nombre(tipo)} salió de tu inicio")
}

/** Widget «Radio del tema»: la música de Díptico se calla por una hora y después vuelve sola. */
fun Diptico.callarMusicaUnaHora() {
    val hasta = System.currentTimeMillis() + 3_600_000L
    actualizar { it.copy(extra = it.extra.copy(musicaCalla = hasta)) }
    avisar("Música en silencio por una hora")
    scope.launch {
        delay(3_600_000L)
        if (estado.value.extra.musicaCalla == hasta) actualizar { it.copy(extra = it.extra.copy(musicaCalla = 0L)) }
    }
}

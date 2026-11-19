package cl.nikalbazu.diptico.ui

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Anclas reales del recorrido. Guardamos píxeles de ventana (no porcentajes del boceto):
 * así el marco sigue a cada componente después del zoom, animaciones y cambio de pantalla.
 */
internal object AnclasRecorrido {
    private data class Registro(val dueno: Any, val bounds: Rect)
    private val medidas = mutableStateMapOf<Pair<Int, String>, Registro>()

    fun buscar(pantalla: Int, nombre: String): Rect? = medidas[pantalla to nombre]?.bounds

    fun actualizar(clave: Pair<Int, String>, dueno: Any, bounds: Rect) {
        val previo = medidas[clave]
        if (previo?.dueno !== dueno || previo.bounds != bounds) medidas[clave] = Registro(dueno, bounds)
    }

    fun retirar(clave: Pair<Int, String>, dueno: Any) {
        if (medidas[clave]?.dueno === dueno) medidas.remove(clave)
    }
}

/** No cambia el tamaño, toque, dibujo ni posición del componente señalado. */
internal fun Modifier.anclaRecorrido(nombre: String): Modifier = composed {
    val pantalla = pantallaActual()
    val clave = pantalla to nombre
    val dueno = remember { Any() }
    DisposableEffect(clave) { onDispose { AnclasRecorrido.retirar(clave, dueno) } }
    this.onGloballyPositioned { coordenadas ->
        if (coordenadas.isAttached) AnclasRecorrido.actualizar(clave, dueno, coordenadas.boundsInWindow())
    }
}

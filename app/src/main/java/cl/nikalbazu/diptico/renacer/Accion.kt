package cl.nikalbazu.diptico.renacer

import android.net.Uri

/** Contrato entre las pantallas y el coordinador. También sirve para probar la UI sin Android. */
sealed interface Accion {
    data class Ir(val lugar: Lugar) : Accion
    data class Consola(val id: String?) : Accion
    data class Consulta(val texto: String) : Accion
    data class Filtrar(val filtro: Filtro) : Accion
    data class Seleccionar(val clave: String) : Accion
    data class Abrir(val ventana: Ventana?) : Accion
    data class Favorito(val clave: String) : Accion
    data class Ocultar(val clave: String) : Accion
    data class Paso(val clave: String, val texto: String) : Accion
    data class Emulador(val clave: String, val id: String?) : Accion
    data class Jugar(val clave: String, val pantalla: Int) : Accion
    data class Estilo(val ambiente: Ambiente) : Accion
    data class Calendario(val activo: Boolean) : Accion
    data class Movimiento(val activo: Boolean) : Accion
    data class DosPantallas(val activo: Boolean) : Accion
    data class QuitarCarpeta(val uri: Uri) : Accion
    data class AsignarConsola(val uri: Uri, val id: String?) : Accion
    data class AgregarApp(val paquete: String) : Accion
    data class BuscarArte(val clave: String) : Accion
    data class Imagen(val clave: String) : Accion
    data object Carpeta : Accion
    data object Recargar : Accion
    data object Apps : Accion
    data object Exportar : Accion
    data object Importar : Accion
    data object InicioAndroid : Accion
    data object CerrarAviso : Accion
    data object Limpiar : Accion
    data object Volver : Accion
    data object Lista : Accion
}

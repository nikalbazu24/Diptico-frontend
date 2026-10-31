package cl.nikalbazu.diptico.data

import android.net.Uri

/** Un juego encontrado en la carpeta del usuario. */
data class Juego(
    /** Identificador estable: "consola/archivo". */
    val clave: String,
    val consolaId: String,
    val titulo: String,
    val archivo: String,
    /** URI del archivo dentro de la carpeta elegida (con permiso persistente). */
    val uri: Uri,
    val docId: String,
    val portada: Uri? = null,
    val arte: Uri? = null,
    /** Logo del juego (descargado), para la pantalla atenuada y el inicio. */
    val logo: Uri? = null,
    /** El juego es una carpeta (PS3), no un archivo. */
    val esCarpeta: Boolean = false,
) {
    /** Tipo para elegir emulador: la extensión del archivo o "carpeta". */
    val tipo: String
        get() = if (esCarpeta) "carpeta" else archivo.substringAfterLast('.', "").lowercase()
}

/** Lo que Díptico recuerda de cada juego. */
data class DatosJuego(
    /** Segundos jugados en total. */
    val tiempo: Long = 0L,
    /** "Tu próximo paso", escrito por la persona. */
    val paso: String = "",
    /** Última vez que se jugó (milisegundos). */
    val ultimo: Long = 0L,
    /** "arriba", "abajo" o null para usar el ajuste general. */
    val pantalla: String? = null,
)

/** Imágenes y título descargados de internet para un juego (rutas dentro de la app). */
data class MediosJuego(
    val portada: String? = null,
    val arte: String? = null,
    val logo: String? = null,
    val titulo: String? = null,
)

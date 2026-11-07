package cl.nikalbazu.diptico.renacer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import cl.nikalbazu.diptico.data.Almacen
import cl.nikalbazu.diptico.data.Biblioteca
import cl.nikalbazu.diptico.data.CarpetaJuegos
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.emu.Lanzador
import kotlinx.coroutines.CancellationException

/** Solo acceso a archivos y Android. El escaneo no decide la navegación. */
internal class BibliotecaRenacer(private val contexto: Context) {
    val almacen = Almacen(contexto)
    private var archivos = emptyList<Juego>()

    data class Lectura(val juegos: List<Juego>, val fallas: List<String>)

    fun cargar(): List<Juego> {
        Consolas.cargar(contexto)
        val raices = almacen.carpetas().mapTo(HashSet()) { it.uri }
        archivos = almacen.biblioteca().filter { raiz(it) in raices }
        return presentar()
    }

    fun escanear(carpetas: List<CarpetaJuegos>): Lectura {
        val previos = archivos.groupBy(::raiz)
        val fallas = mutableListOf<String>()
        val encontrados = carpetas.flatMap { carpeta ->
            try {
                Biblioteca.escanear(contexto, carpeta.uri, almacen.media, carpeta.consola)
            } catch (cancelado: CancellationException) {
                throw cancelado
            } catch (_: Exception) {
                fallas += Biblioteca.nombreCarpeta(carpeta.uri)
                previos[carpeta.uri].orEmpty()
            }
        }.distinctBy { it.clave }.sortedWith(compareBy({ Consolas.orden(it.consolaId) }, { it.titulo.lowercase() }))
        archivos = encontrados
        almacen.guardarBiblioteca(encontrados)
        return Lectura(presentar(), fallas)
    }

    private fun presentar(): List<Juego> {
        val datos = almacen.datos()
        val medios = almacen.medios()
        val propios = archivos.map { juego ->
            val dato = datos[juego.clave]
            val medio = medios[juego.clave]
            juego.copy(
                titulo = dato?.titulo ?: medio?.titulo ?: juego.titulo,
                portada = dato?.portada?.let(Uri::parse) ?: juego.portada ?: medio?.portada?.let { Uri.fromFile(java.io.File(it)) },
                arte = dato?.arte?.let(Uri::parse) ?: juego.arte ?: medio?.arte?.let { Uri.fromFile(java.io.File(it)) },
                logo = dato?.logo?.let(Uri::parse) ?: juego.logo ?: medio?.logo?.let { Uri.fromFile(java.io.File(it)) },
            )
        }
        val apps = (almacen.android.orEmpty() + almacen.ports).distinct().mapNotNull { paquete ->
            val nombre = Lanzador.nombreApp(contexto, paquete) ?: return@mapNotNull null
            val uri = Uri.parse("app://$paquete")
            Juego("${Consolas.ANDROID}/$paquete", Consolas.ANDROID, nombre, paquete, uri, paquete, portada = uri)
        }
        return (propios + apps).distinctBy { it.clave }
    }

    companion object {
        fun raiz(juego: Juego): Uri? = try {
            DocumentsContract.buildTreeDocumentUri(juego.uri.authority, DocumentsContract.getTreeDocumentId(juego.uri))
        } catch (_: IllegalArgumentException) { null }
    }
}

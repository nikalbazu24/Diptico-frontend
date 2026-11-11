package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.estado.Diptico.Control
import cl.nikalbazu.diptico.estado.Diptico.InfoBateria
import cl.nikalbazu.diptico.estado.Diptico.ItemGaleria
import cl.nikalbazu.diptico.estado.Diptico.LanzadorPC
import cl.nikalbazu.diptico.estado.Diptico.OpcionBurbuja
import cl.nikalbazu.diptico.estado.Diptico.PorBorrar
import cl.nikalbazu.diptico.estado.Diptico.Regreso
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.os.SystemClock
import android.view.Display
import cl.nikalbazu.diptico.audio.Aviso
import cl.nikalbazu.diptico.audio.Avisos
import cl.nikalbazu.diptico.audio.Medios
import cl.nikalbazu.diptico.audio.Musica
import cl.nikalbazu.diptico.audio.Sonidos
import cl.nikalbazu.diptico.audio.Vibracion
import cl.nikalbazu.diptico.data.Almacen
import cl.nikalbazu.diptico.data.Biblioteca
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.data.Descargas
import cl.nikalbazu.diptico.data.FichaInfo
import cl.nikalbazu.diptico.data.Fichas
import cl.nikalbazu.diptico.data.CarpetaJuegos
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.data.MediosJuego
import cl.nikalbazu.diptico.emu.App
import cl.nikalbazu.diptico.emu.Lanzador
import cl.nikalbazu.diptico.emu.Pantallas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Personalización y perfiles visuales: funciones de Diptico movidas aquí desde Diptico.kt para que ese archivo no siga creciendo.
 * Se llaman igual que antes (Diptico.x()); fuera del paquete estado hay que importar cl.nikalbazu.diptico.estado.*.
 */

fun Diptico.cancelarArchivoVisual() { imagenVisual = null }


fun Diptico.guardarExperiencia(v: Experiencia) {
    almacen.guardarTexto("experiencia", v.json(local = true).toString())
    actualizar { it.copy(experiencia = v) }
    cl.nikalbazu.diptico.ui.Estilo.efecto = if (v.vidrio) "vidrio" else estado.value.efecto
    aplicarTema()
}


internal fun Diptico.capturarPerfil(): Experiencia = estado.value.let { e -> e.experiencia.copy(
    posiciones = e.widgets.map { w -> PosicionVisual(w.tipo, w.x, w.y, w.an, w.al, w.pagina, w.visible,
        w.estilo, w.color, w.radio, w.opacidad, w.variante, w.escala) },
    columnas = e.gridCols, filas = e.gridFilas, paginas = e.paginas,
    inicioPerfil = e.inicio, vistaPerfil = e.vista, sistemasPerfil = e.vistaConsolas,
) }


internal fun Diptico.aplicarPerfil(v: Experiencia) {
    guardarExperiencia(v.copy(posiciones = emptyList()))
    if (v.posiciones.isNotEmpty()) {
        val posiciones = v.posiciones.associateBy { it.tipo }
        val widgets = estado.value.widgets.map { w ->
            val p = posiciones[w.tipo] ?: return@map w
            w.copy(x = p.x, y = p.y, an = p.an, al = p.al, pagina = p.pagina, visible = p.visible,
                estilo = p.estilo ?: w.estilo, color = p.color ?: w.color, radio = p.radio ?: w.radio,
                opacidad = p.opacidad ?: w.opacidad, variante = p.variante ?: w.variante, escala = p.escala ?: w.escala)
        }
        val cols = v.columnas.coerceIn(4, 8)
        val filas = v.filas.coerceIn(3, 8)
        val r = Rejilla.distribuir(widgets, cols, filas, v.paginas)
        almacen.guardarTexto("gridCols", cols.toString())
        almacen.guardarTexto("gridFilas", filas.toString())
        almacen.guardarTexto("paginas", r.paginas.toString())
        Widgets.COLUMNAS = cols
        Widgets.FILAS = filas
        guardarWidgets(r.widgets)
        actualizar { it.copy(gridCols = cols, gridFilas = filas, paginas = r.paginas, paginaInicio = 0) }
    }
    v.inicioPerfil?.let { inicio ->
        almacen.guardarTexto("inicio", inicio)
        actualizar { it.copy(inicio = inicio) }
    }
    v.vistaPerfil?.takeIf { it in VISTAS }?.let { vista ->
        almacen.guardarTexto("vista", vista)
        actualizar { it.copy(vista = vista) }
    }
    v.sistemasPerfil?.let { elegirVistaConsolas(it) }
}


internal fun Diptico.plantillaVisual(nombre: String) {
    val e = estado.value
    val elegidos = when (nombre) {
        "Jugar" -> setOf("juego", "reloj", "proximaPartida", "tengo20")
        "Explorar" -> setOf("buscar", "recientes", "colaJuegos", "favoritos")
        else -> setOf("juego", "reloj")
    }
    val widgets = Widgets.porDefecto.map { w -> w.copy(visible = w.tipo in elegidos) }
    val r = Rejilla.distribuir(widgets, 6, 4, 1)
    val plantilla = e.experiencia.copy(
        vidrio = nombre != "Ligero", movimiento = nombre != "Ligero",
        posiciones = r.widgets.map { PosicionVisual(it.tipo, it.x, it.y, it.an, it.al, it.pagina, it.visible) },
        columnas = 6, filas = 4, paginas = r.paginas, inicioPerfil = "widgets",
        vistaPerfil = if (nombre == "Ligero") "lista" else "cuadricula",
        sistemasPerfil = if (nombre == "Explorar") "carpetas" else "arriba",
        vistaPartida = when (nombre) {
            "Jugar" -> VistaPartida.BITACORA.name
            "Ligero" -> VistaPartida.LIGERA.name
            else -> VistaPartida.CLASICA.name
        },
    )
    mostrarDialogo(Dialogo("plantilla-visual", "Aplicar $nombre", "Cambia la distribución del inicio y el estilo. Conserva las personalizaciones de cada juego.",
        Icono.PALETA, si = "Aplicar", no = "Cancelar")) {
        almacen.guardarTexto("perfilVisualAnterior", capturarPerfil().json(local = true).toString())
        aplicarPerfil(plantilla)
    }
}


fun Diptico.recibirPerfilVisual(texto: String) {
    try {
        val v = Experiencia.importar(texto)
        mostrarDialogo(Dialogo("perfil_visual", "Importar perfil visual", "Universo: ${if (v.universo) "sí" else "no"} · Vidrio: ${if (v.vidrio) "sí" else "no"}. Tus colecciones y juegos se conservan.", Icono.PALETA)) {
            aplicarPerfil(v.copy(colecciones = estado.value.experiencia.colecciones))
        }
    } catch (_: Exception) { avisar("Ese archivo no es un perfil visual compatible") }
}


fun Diptico.abrirColeccionVisual(id: String) {
    if (id !in estado.value.colecciones) return
    abrirWidgetTipo("coleccion-$id")
    actualizar { it.copy(coleccionVisual = id) }
}


fun Diptico.fijarFicha() {
    val e = estado.value
    if (e.unaPantalla || e.sesion != null) return
    actualizar { it.copy(fichaFijada = if (it.fichaFijada != null) null else it.seleccionado?.clave) }
}


internal fun Diptico.vigilarUniverso() {
    scope.launch {
        estado.map { e ->
            if (!e.experiencia.universo) return@map null
            val c = e.coleccionVisual?.let { e.experiencia.colecciones[it] }
            val j = e.sesion?.let { e.juegosPorClave[it.clave] } ?: e.seleccionado
            c?.color to (j?.arte ?: j?.portada)
        }.distinctUntilChanged().collectLatest { fuente ->
            // Ignora el color de una portada anterior si ya te moviste a otro juego.
            val color = if (fuente == null) 0L else fuente.first ?: withContext(Dispatchers.IO) {
                cl.nikalbazu.diptico.ui.ColorUniverso.leer(app, fuente.second)
            }
            if (color != estado.value.colorUniverso) {
                actualizar { it.copy(colorUniverso = color) }
                aplicarTema()
            }
        }
    }
}


internal fun Diptico.opcionesPersonalizacion(e: Estado): List<Opcion> = buildList {
    val v = e.experiencia
    add(Opcion("Colores del juego · Universo", if (v.universo) "Sí" else "No", Icono.MUNDO, "Colores tomados de la imagen del juego, compartidos por ambas pantallas. Sin imagen se conserva el tema.") { guardarExperiencia(v.copy(universo = !v.universo)) })
    add(Opcion("Movimiento ambiental", if (v.movimiento) "Suave" else "Reducido", Icono.ESTRELLA, "Apaga el movimiento ambiental y el zoom lento; en partida se mantiene estático.") {
        guardarExperiencia(v.copy(movimiento = !v.movimiento))
    })
    if (almacen.texto("perfilVisualAnterior").isNotBlank()) {
        add(Opcion("Volver a mi distribución anterior", icono = Icono.CONTINUAR) {
            aplicarPerfil(Experiencia.leer(almacen.texto("perfilVisualAnterior")))
            almacen.guardarTexto("perfilVisualAnterior", "")
        })
    }
    for (n in 1..3) {
        val id = "Perfil $n"
        add(Opcion("Guardar $id", if (id in e.perfilesVisuales) "Reemplazar" else "Vacío", Icono.COPIAR) {
            val perfiles = estado.value.perfilesVisuales + (id to capturarPerfil())
            almacen.guardarTexto("perfilesVisuales", org.json.JSONObject().apply { perfiles.forEach { (k, p) -> put(k, p.json(true)) } }.toString())
            actualizar { it.copy(perfilesVisuales = perfiles) }; avisar("$id guardado")
        })
        if (id in e.perfilesVisuales) add(Opcion("Aplicar $id", icono = Icono.CHECK) { aplicarPerfil(e.perfilesVisuales.getValue(id)) })
    }
    add(Opcion("Compartir perfil visual", "Archivo JSON", Icono.WEB, "Comparte la composición, el estilo de widgets y las vistas. Los archivos locales, las claves y la biblioteca quedan en tu equipo.") { exportarVisual?.invoke(capturarPerfil().json().toString(2)) })
    add(Opcion("Importar perfil visual", "Con vista previa y confirmación", Icono.DESCARGA) { importarVisual?.invoke() })
    e.colecciones.forEach { (id, c) ->
        val p = v.colecciones[id] ?: PresentacionColeccion()
        fun guardar(nuevo: PresentacionColeccion) = guardarExperiencia(estado.value.experiencia.let { it.copy(colecciones = it.colecciones + (id to nuevo)) })
        add(Opcion("${c.nombre} · abrir", "Presentación", Icono.CARPETA) { abrirColeccionVisual(id) })
        add(Opcion("${c.nombre} · color", "Cambiar", Icono.PALETA) { val l = Temas.MUESTRAS; guardar(p.copy(color = l[(l.indexOf(p.color) + 1).mod(l.size)])) })
        add(Opcion("${c.nombre} · disposición", p.vista, Icono.VISTA) { guardar(p.copy(vista = if (p.vista == "mosaico") "galeria" else "mosaico")) })
        listOf("fondo", "logo", "musica").forEach { tipo ->
            add(Opcion("${c.nombre} · $tipo", "Elegir archivo", if (tipo == "musica") Icono.MUSICA else Icono.PORTADA) { imagenVisual = id to tipo; pedirArchivo?.invoke(if (tipo == "musica") "audio/*" else "image/*") })
        }
        add(Opcion("${c.nombre} · restaurar diseño", icono = Icono.CONTINUAR) { guardarExperiencia(v.copy(colecciones = v.colecciones - id)) })
    }
}

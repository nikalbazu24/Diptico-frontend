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
 * Temas ----------: funciones de Diptico movidas aquí desde Diptico.kt para que ese archivo no siga creciendo.
 * Se llaman igual que antes (Diptico.x()); fuera del paquete estado hay que importar cl.nikalbazu.diptico.estado.*.
 */


internal fun Diptico.temasPersonales(e: Estado): List<Tema> {
    val temaAnterior = Temas.incluidos.firstOrNull { it.id == e.tema && Temas.iniciales.none { t -> t.id == it.id } }
    return (e.temasPropios + listOfNotNull(temaAnterior)).distinctBy { it.id }
}


fun Diptico.todosLosTemas(e: Estado = estado.value): List<Tema> = Temas.iniciales + temasPersonales(e) + temasExclusivosGanados()


/** Los temas exclusivos cuyo logro de DIPPO ya ganaste. */
fun Diptico.temasExclusivosGanados(): List<Tema> {
    val hechos = hechosDippo()
    return Temas.exclusivos.filter { it.logro in hechos }.map { it.tema }
}


internal fun Diptico.conAdorno(e: Estado, tema: Tema): Tema {
    val decor = e.adornosPorTema[tema.id]?.takeIf { it in Temas.DECORACIONES } ?: tema.decor
    val intensidad = e.intensidadPorTema[tema.id]?.toIntOrNull()?.takeIf { it in 1..3 } ?: tema.intensidadDecoracion
    return if (decor == tema.decor && intensidad == tema.intensidadDecoracion) tema
    else tema.copy(decor = decor, intensidadDecoracion = intensidad)
}


internal fun Diptico.temasDecoracion(e: Estado): List<Tema> = when (e.galeriaPestana) {
    1 -> catalogoOficial.orEmpty().map { it.tema }
    2 -> Temas.fiestas.map { it.tema }
    else -> todosLosTemas(e)
}


internal fun Diptico.itemsDecoraciones(e: Estado): List<ItemGaleria> = buildList {
    temasDecoracion(e).forEachIndexed { i, tema ->
        val abierto = e.decoracionAbierta == tema.id
        val activo = conAdorno(e, tema)
        val detalle = if (activo.decor == "nada") Temas.nombreDecor(activo.decor)
        else "${Temas.nombreDecor(activo.decor)} · ${Temas.intensidadNombre(activo.intensidadDecoracion)}"
        add(ItemGaleria("decor-tema:${tema.id}", tema.nombre, detalle, abierto, activo) {
            actualizar { it.copy(decoracionAbierta = if (abierto) null else tema.id, indice = i) }
        })
        if (abierto) {
            val estilos = (Temas.decoracionesPara(tema) + activo.decor).distinct()
            estilos.forEach { decor ->
                add(ItemGaleria("decor-tipo:$decor", Temas.nombreDecor(decor), "Tipo de decoración",
                    activo.decor == decor, activo.copy(decor = decor)) {
                    guardarDecoracion(tema.id, decor, e.intensidadPorTema[tema.id]?.toIntOrNull())
                })
            }
            for (nivel in 1..3) {
                add(ItemGaleria("decor-intensidad:$nivel", Temas.intensidadNombre(nivel), "Intensidad",
                    activo.intensidadDecoracion == nivel, activo.copy(intensidadDecoracion = nivel)) {
                    guardarDecoracion(tema.id, e.adornosPorTema[tema.id], nivel)
                })
            }
            add(ItemGaleria("decor-restaurar", "Restaurar decoración", "Valores originales del tema", false, tema) {
                guardarDecoracion(tema.id, null, null)
            })
        }
    }
}


internal fun Diptico.guardarDecoracion(id: String, decor: String?, nivel: Int?) {
    val e = estado.value
    val estilos = if (decor == null) e.adornosPorTema - id else e.adornosPorTema + (id to decor)
    val intensidades = if (nivel == null) e.intensidadPorTema - id else e.intensidadPorTema + (id to nivel.toString())
    if (estilos == e.adornosPorTema && intensidades == e.intensidadPorTema) return
    almacen.guardarTexto("adornosPorTema", escribirMapa(estilos))
    almacen.guardarTexto("intensidadPorTema", escribirMapa(intensidades))
    actualizar { it.copy(adornosPorTema = estilos, intensidadPorTema = intensidades) }
    aplicarTema()
}


/** Aplica el tema elegido, o el de la fiesta de hoy si está activado. */
fun Diptico.aplicarTema() {
    val e = estado.value
    val fiesta = if (e.temaFestivo) Temas.deHoy() else null
    val elegido = todosLosTemas(e).firstOrNull { it.id == e.tema }
        ?: Temas.fiestas.firstOrNull { it.tema.id == e.tema }?.tema
        ?: Temas.incluidos[0]
    val baseVisual = conAdorno(e, fiesta?.tema ?: elegido)
    // El color de la consola elegida tiñe el acento, pero las decoraciones del tema se quedan siempre (antes se apagaban
    // al entrar a una consola con color propio, y por eso solo se veían en algunas carpetas).
    val activo = if (e.experiencia.universo && e.colorUniverso != 0L) baseVisual.copy(acento = e.colorUniverso, acento2 = e.colorUniverso) else baseVisual
    cl.nikalbazu.diptico.ui.Estilo.tema = activo
    cl.nikalbazu.diptico.ui.Estilo.materialInterfaz = e.experiencia.materialInterfaz
    cl.nikalbazu.diptico.ui.Estilo.efecto = activo.acabado.ifBlank { if (e.experiencia.vidrio) "vidrio" else e.efecto }
    aplicarFluidez()
    Sonidos.cambiarJuego(activo.sonidos, activo.tonoSonidos)
    val pista = musicaDeTema(activo)
    if (fiesta?.id != e.fiesta?.id || pista != e.pistaTema) actualizar { it.copy(fiesta = fiesta, pistaTema = pista) }
}


fun Diptico.elegirTema(id: String) {
    if (id != estado.value.tema) sumarDippo("temas")
    almacen.guardarTexto("tema", id)
    actualizar { it.copy(tema = id) }
    aplicarTema()
    val e = estado.value
    if (e.fiesta != null) avisar("Hoy manda el tema de ${e.fiesta.nombre}. Apaga \"Fechas especiales\" para usar este.")
}


internal fun Diptico.guardarTemasPropios(l: List<Tema>) {
    almacen.guardarTexto("temasPropios", escribirTemas(l))
    actualizar { it.copy(temasPropios = l) }
}


internal fun Diptico.temaParaMezclar(e: Estado): Tema {
    e.temasPropios.firstOrNull { it.id == "mio" }?.let { return it }
    val elegido = todosLosTemas(e).firstOrNull { it.id == e.tema } ?: Temas.iniciales.first()
    return ComponenteTema.MUSICA.combinar(
        conAdorno(e, elegido).copy(id = "mio", nombre = "Mi tema", propio = true, descripcion = ""),
        elegido,
    )
}


internal fun Diptico.editarMiTema(f: (Tema) -> Tema) {
    val e = estado.value
    val nuevo = f(temaParaMezclar(e))
    guardarTemasPropios(listOf(nuevo) + e.temasPropios.filter { it.id != "mio" })
    if (e.tema != "mio") elegirTema("mio") else aplicarTema()
}


internal fun Diptico.importarTemas(leer: () -> List<TemaComunidad>) {
    if (trabajosMapa.containsKey("catalogo_temas")) return
    trabajo("catalogo_temas", "Leyendo el tema")
    scope.launch {
        val resultado = withContext(Dispatchers.IO) { runCatching(leer) }
        terminarTrabajo("catalogo_temas")
        resultado.onSuccess { temas ->
            if (temas.size == 1) confirmarTemaImportado(temas.first())
            else {
                actualizar { it.copy(catalogoTemas = temas) }
                abrirGaleria("importar_temas")
            }
        }.onFailure { avisar(it.message ?: "No pude leer el tema.") }
    }
}


internal fun Diptico.confirmarTemaImportado(entrada: TemaComunidad) {
    val tema = entrada.tema
    val detalle = tema.descripcion.ifBlank { "Tema de ${entrada.autor.ifBlank { "la comunidad" }}" }
    mostrarDialogo(Dialogo("instalar_tema", "Instalar ${tema.nombre}", detalle, Icono.PALETA,
        si = "Instalar y usar", no = "Cancelar")) { instalarTemaComunidad(entrada) }
}


internal fun Diptico.instalarTemaComunidad(entrada: TemaComunidad) {
    if (trabajosMapa.containsKey("instalar_tema")) return
    trabajo("instalar_tema", "Instalando ${entrada.tema.nombre}")
    scope.launch {
        val resultado = withContext(Dispatchers.IO) { runCatching { TiendaTemas.instalar(app, entrada) } }
        terminarTrabajo("instalar_tema")
        resultado.onSuccess { tema ->
            guardarTemasPropios(listOf(tema) + estado.value.temasPropios.filterNot { it.id == tema.id })
            elegirTema(tema.id)
            val actual = estado.value
            if (actual.panel == Panel.GALERIA && actual.galeria == "importar_temas") abrirGaleria("temas", pestana = 1)
            avisar(if (actual.fiesta != null) "${tema.nombre} instalado" else "${tema.nombre} instalado y aplicado")
        }.onFailure { avisar(it.message ?: "No pude instalar este tema.") }
    }
}


internal fun Diptico.opcionesTemas(e: Estado): List<Opcion> {
    val lista = mutableListOf<Opcion>()
    val prox = Temas.proxima()
    lista += Opcion(
        "Fechas especiales", if (e.temaFestivo) "Sí" else "No", Icono.MUNDO,
        "Cambia el tema solo en más de 20 fiestas del mundo: Año Nuevo Lunar, Holi, Diwali, Halloween, Día de Muertos, " +
            "Fiestas Patrias, Navidad y más." + (prox?.let { " La próxima: ${it.first.nombre} (${it.first.donde})." } ?: ""),
    ) {
        val v = !estado.value.temaFestivo
        almacen.guardarBool("temaFestivo2", v)
        actualizar { it.copy(temaFestivo = v) }
        aplicarTema()
        estado.value.fiesta?.let { f -> avisar(f.tema.mensaje.ifBlank { "¡Hoy es ${f.nombre}!" }) }
    }
    e.fiesta?.let { f ->
        lista += Opcion("Hoy: ${f.nombre}", f.donde, Icono.ESTRELLA, f.tema.mensaje) { avisar(f.tema.mensaje.ifBlank { f.nombre }) }
    }
    lista.add(
        0,
        Opcion(
            "Elegir tema", cl.nikalbazu.diptico.ui.Estilo.tema.nombre, Icono.PALETA,
            "Cinco temas incluidos y los que hayas instalado. Cada tema trae una explicación y vista previa.",
        ) { abrirGaleria("temas") },
    )
    lista.add(
        1,
        Opcion(
            "Mis colores", "Toda la interfaz", Icono.PALETA,
            "Elige tú el color principal, el secundario, los detalles, el fondo y las letras de todo Díptico.",
        ) { abrirGaleria("colores") },
    )
    lista.add(0, Opcion("Tienda de temas", "${TiendaTemas.TOTAL_OFICIAL} temas", Icono.PALETA,
        "Explora e instala temas dentro de Díptico.") { abrirTiendaTemas() })
    lista.add(2, Opcion("Mezclar mi tema", "Temas instalados", Icono.PALETA,
        "Elige de qué tema viene cada elemento. Cada cambio se guarda en Mi tema.") { abrirGaleria("mezclar_tema") })
    val mio = e.temasPropios.firstOrNull { it.id == "mio" }
    lista += Opcion("Mi tema · fondo", when (mio?.base) { "oscuro" -> "Oscuro"; "claro" -> "Claro"; "oled" -> "Negro puro"; else -> "Según el modo" }, Icono.TEMA, ayuda = "El fondo de tu tema propio: oscuro, claro, negro puro (ideal para OLED) o según el modo.") {
        editarMiTema { it.copy(base = when (it.base) { "auto" -> "oscuro"; "oscuro" -> "oled"; "oled" -> "claro"; else -> "auto" }) }
    }
    return lista
}

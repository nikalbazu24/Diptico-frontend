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
 * Menú principal: funciones de Diptico movidas aquí desde Diptico.kt para que ese archivo no siga creciendo.
 * Se llaman igual que antes (Diptico.x()); fuera del paquete estado hay que importar cl.nikalbazu.diptico.estado.*.
 */

fun Diptico.esGrupoAjustes(s: Seccion) = s in GRUPOS_AJUSTES


/** El menú ahora muestra solo las seis secciones nuevas; las de antes siguen abriéndose por dentro y con Buscar. */
@Suppress("UNUSED_PARAMETER")
fun Diptico.seccionesMenu(simple: Boolean = estado.value.menuSimple): List<Seccion> = GRUPOS_AJUSTES.keys.toList()


/** L1/R1 y la cruceta en la columna de secciones: la anterior o la siguiente de las que se ven. */
fun Diptico.saltarSeccion(delta: Int, envolver: Boolean) {
    val e = estado.value
    val s = seccionesMenu()
    // Una sección avanzada abierta desde «Más ajustes» cuenta como «Más ajustes».
    val i = s.indexOf(e.seccion).takeIf { it >= 0 } ?: s.indexOf(Seccion.MAS).coerceAtLeast(0)
    val n = if (envolver) (i + delta + s.size) % s.size else (i + delta).coerceIn(0, s.size - 1)
    if (envolver) elegirSeccion(s[n]) else actualizar { it.copy(seccion = s[n], indice = 0) }
}


fun Diptico.cambiarMenuSimple(v: Boolean) {
    almacen.guardarBool("menuSimple", v)
    val visibles = seccionesMenu(v)
    actualizar { it.copy(menuSimple = v, seccion = if (it.seccion in visibles) it.seccion else Seccion.ESENCIAL, indice = 0) }
    avisar(if (v) "Menú simple: lo avanzado está en «Más ajustes»" else "Menú completo: todas las secciones a la vista")
}


internal fun Diptico.opcionMenuSimple(e: Estado) = Opcion(
    "Menú simple", if (e.menuSimple) "Sí" else "No", Icono.VISTA,
    "Sí: solo las secciones de todos los días y las avanzadas juntas en «Más ajustes». No: todas las secciones en la columna. Buscar encuentra cualquier ajuste igual.",
) { cambiarMenuSimple(!estado.value.menuSimple) }


/** «Más ajustes»: cada sección avanzada con lo que trae, para abrirla de un toque. */
internal fun Diptico.opcionesMas(e: Estado): List<Opcion> {
    val lista = mutableListOf(opcionMenuSimple(e))
    for (s in Seccion.values()) {
        if (s in SECCIONES_SIMPLES || s == Seccion.MAS || esGrupoAjustes(s)) continue
        lista += Opcion(s.titulo, "", s.icono, s.ayuda) { actualizar { it.copy(seccion = s, enOpciones = true, indice = 0) } }
    }
    return lista
}


/** Las opciones de una sección. Se arman una vez por cada cambio de estado (las dos pantallas las comparten). */
/** Subtítulo que agrupa las opciones largas de algunas secciones (así es más fácil encontrar algo). "" = sin grupo. */
/** El subtítulo de [o] en la sección [s]: el que trae la opción, o el que se deduce de su título. */
fun Diptico.grupoOpcion(s: Seccion, o: Opcion): String = o.grupo ?: grupoOpcion(s, o.titulo)


fun Diptico.grupoOpcion(s: Seccion, t: String): String = when (s) {
    Seccion.BIBLIOTECA -> when {
        t == "Agregar un juego de Steam" || t.startsWith("Agregar: ") || estado.value.juegosPC.any { it.titulo == t } ||
            LANZADORES_PC.any { it.nombre == t } -> "Juegos de PC"
        else -> "Tus juegos"
    }
    Seccion.RENDIMIENTO -> if (t == "Optimizar ahora" || t == "Memoria disponible") "Ahora" else "Al jugar"
    Seccion.CONTROLES -> if (t == "Sticks") "Sticks" else if (t.startsWith("Botón") || t.startsWith("Aprender")) "Botones extra" else "Más"
    Seccion.SOBREMESA -> if (t == "Cuándo se activa" || t == "Tele conectada") "Cuándo" else "En la tele"
    Seccion.ASISTENTE -> when {
        t == "Hablar con DIPPO" || t.startsWith("Logros") || t.startsWith("Premios") -> "DIPPO"
        t.startsWith("Clave de") || t.contains("IA") || t.contains("modelo", true) || t.contains("conversación", true) || t.contains("voz", true) -> "Su cerebro (IA)"
        t.contains("juegas") || t.contains("emperatura") || t.contains("onsejo") -> "Mientras juegas"
        t.contains("Aviso") -> "Avisos"
        else -> "Dónde aparece"
    }
    Seccion.TEMAS -> when {
        t.startsWith("Mi tema") || t == "Mezclar mi tema" -> "Mi tema"
        t.contains("IA") -> "Con IA"
        else -> "Elegir"
    }
    Seccion.INICIO -> when {
        t == "Al abrir Díptico" || t.startsWith("Bienvenida") || t.startsWith("Continuación") || t == "Tu nombre" -> "Al encender"
        t.startsWith("Preguntar dónde") -> "Al terminar de jugar"
        t.startsWith("Imagen grande") || t == "Barra de accesos" -> "Pantalla principal"
        t == "Estilo del inicio" || t.contains("widget", true) && !t.startsWith("Permiso") -> "Pantalla principal"
        else -> "Más"
    }
    Seccion.APARIENCIA -> when {
        t == "Modo" || t.startsWith("Efecto") || t == "Fluidez" -> "Estilo"
        t.contains("protector", true) -> "Protector de pantalla"
        t.startsWith("Zoom") || t.contains("nimación") -> "Detalles"
        else -> "Más opciones"
    }
    Seccion.MUSICA -> if (t.startsWith("Sonidos") || t.startsWith("Juego de sonidos")) "Sonidos" else "Música de fondo"
    Seccion.ESTANTE -> when {
        t.endsWith("carpetas") -> "Carpetas"
        t.startsWith("Cajas") || t.startsWith("Forma") || t.startsWith("Tamaño") || t.startsWith("Imagen") -> "Cajas"
        else -> "Qué ves y en qué orden"
    }
    Seccion.PORTADAS -> when {
        t.startsWith("Reconocer") || t == "Títulos de libretro" -> "Nombres de tus juegos"
        t.startsWith("Descarga") || t.startsWith("Buscar de nuevo") || t.startsWith("Imágenes de") || t.startsWith("Borrar") -> "Imágenes"
        else -> "Cuentas para más imágenes"
    }
    Seccion.SISTEMA -> when {
        t.startsWith("Respald") || t.contains("respaldo", true) || t.startsWith("Cambiar dónde") || t.startsWith("Elegir dónde") -> "Respaldo"
        t.startsWith("Descargar e instalar") || t.contains("actualizaciones") || t.startsWith("Avisarme") -> "Actualizaciones"
        t.startsWith("Reportar") || t.startsWith("Ver las novedades") || t.startsWith("Recorrido") || t.startsWith("Repetir") -> "Ayuda"
        else -> "Díptico"
    }
    Seccion.PANTALLAS -> when {
        t == "Tu equipo" -> "Tu equipo"
        t.startsWith("Tamaño") || t.contains("letra", true) -> "Tamaño"
        t.startsWith("Teclado") -> "Escribir"
        else -> "Pantallas"
    }
    else -> ""
}


fun Diptico.opcionesSeccion(s: Seccion): List<Opcion> {
    val e = estado.value
    // Moverse por la lista (indice) no cambia las opciones: no se vuelven a armar en cada paso.
    val clave = claveMenu(e)
    val c = cacheOpciones?.takeIf { it.first === e || it.first == clave }?.second ?: HashMap<Seccion, List<Opcion>>().also { cacheOpciones = clave to it }
    return c.getOrPut(s) { condensar(s, armarOpciones(s)) }
}


/** Lo que cambia las opciones del menú (moverse por las listas no las cambia). */
fun Diptico.claveMenu(e: Estado): Estado = e.copy(indice = 0, enOpciones = false, extra = e.extra.copy(subSel = 0, subOpcion = -1, desplegableSel = 0))


/**
 * Ajustes condensados: en una sección larga, cada grupo muestra sus 3 primeras opciones y el resto queda
 * en una fila «Más opciones…» que se abre en una ventana emergente. Con una búsqueda se ve todo.
 */
internal fun Diptico.condensar(s: Seccion, l: List<Opcion>): List<Opcion> {
    if (l.any { it.sub != null }) return l
    if (!esGrupoAjustes(s) || estado.value.buscarAjuste.isNotBlank() || l.size <= 12) return l
    val salida = mutableListOf<Opcion>()
    var i = 0
    while (i < l.size) {
        val g = grupoOpcion(s, l[i])
        var j = i
        while (j < l.size && grupoOpcion(s, l[j]) == g) j++
        val grupo = l.subList(i, j)
        if (grupo.size > 4) {
            salida += grupo.take(3).map { it.copy(grupo = g) }
            val resto = grupo.drop(3)
            val titulo = if (g.isBlank()) "Más opciones" else "Más opciones de ${g.lowercase()}"
            salida += Opcion(
                titulo, "${resto.size} ›", Icono.MENU,
                resto.take(4).joinToString(", ") { Idioma.t(it.titulo) } + if (resto.size > 4) "…" else "",
                grupo = g, sub = resto,
            ) { abrirSubOpciones(titulo) }
        } else salida += grupo.map { it.copy(grupo = g) }
        i = j
    }
    return salida
}


fun Diptico.abrirSubOpciones(titulo: String) {
    val e = estado.value
    val i = opcionesSeccion(e.seccion).indexOfFirst { it.titulo == titulo && it.sub != null }
    if (i < 0) return
    actualizar { it.copy(indice = i, extra = it.extra.copy(subOpcion = i, subSel = 0)) }
}


fun Diptico.cerrarSubOpciones() {
    if (estado.value.extra.subOpcion >= 0) actualizar { it.copy(extra = it.extra.copy(subOpcion = -1, subSel = 0)) }
}


/** Las opciones de la ventana abierta (null = no hay ventana). */
fun Diptico.subOpciones(e: Estado): List<Opcion>? =
    if (e.panel == Panel.MENU && e.extra.subOpcion >= 0) opcionesSeccion(e.seccion).getOrNull(e.extra.subOpcion)?.sub else null


/** La opción que se explica arriba: la marcada en la ventana, o la de la lista. */
fun Diptico.opcionEnFoco(e: Estado): Opcion? =
    subOpciones(e)?.getOrNull(e.extra.subSel) ?: if (e.enOpciones) opcionesSeccion(e.seccion).getOrNull(e.indice) else null


/** Usa la opción [i] de la ventana (A o →; con [menos], ←). Si abre otra cosa, la ventana se cierra. */
fun Diptico.usarSubOpcion(i: Int = estado.value.extra.subSel, menos: Boolean = false) {
    val e = estado.value
    val o = subOpciones(e)?.getOrNull(i) ?: return
    if (i != e.extra.subSel) actualizar { it.copy(extra = it.extra.copy(subSel = i)) }
    // Una opción con lista (íconos, vistas…) abre su menú desplegable también dentro del submenú, en vez de pasar a la siguiente.
    if (!menos && o.alternativas != null && o.elegir != null) { abrirDesplegable(i, o.elegida); return }
    if (menos) (o.menos ?: return)() else desdeMenu(o.accion)
    if (estado.value.panel != Panel.MENU) actualizar { it.copy(extra = it.extra.copy(subOpcion = -1, subSel = 0)) }
}


/** Ajustes que más se usan, sacados de todas las secciones. O, si se buscó algo, lo que coincide. */
internal fun Diptico.opcionesEsenciales(e: Estado): List<Opcion> {
    val todas = Seccion.values().filter { it != Seccion.ESENCIAL && it != Seccion.MAS && !esGrupoAjustes(it) }.flatMap { s -> armarOpciones(s).map { s to it } }
    if (e.buscarAjuste.isNotBlank()) {
        val q = Busqueda.normalizar(e.buscarAjuste)
        val res = todas.filter { (s, o) ->
            listOf(o.titulo, o.ayuda, s.titulo, Idioma.t(o.titulo), Idioma.t(s.titulo)).any { Busqueda.normalizar(it).contains(q) }
        }.map { (s, o) -> o.copy(valor = o.valor.ifEmpty { s.titulo }) }
        return listOf(Opcion("Quitar la búsqueda", "\"${e.buscarAjuste}\" · ${res.size}", Icono.CERRAR) { actualizar { it.copy(buscarAjuste = "", indice = 0) } }) + res
    }
    val elegidos = listOf(
        "Tienda de temas", "Modo", "Tamaño de letra", "Modo de pantallas", "Intercambiar pantallas", "Teclado", "Música de fondo",
        "Sonidos al moverte", "Fechas especiales", "Efecto de los botones", "Fluidez", "DIPPO mientras juegas",
    )
    val lista = mutableListOf(
        Opcion("Buscar un ajuste", "Y", Icono.BUSCAR, "Escribe una palabra (música, tema, teclado…) y te muestro dónde está, aunque esté en una sección avanzada.") { abrirTexto("buscar_ajuste") },
        opcionMenuSimple(e),
        Opcion("Idioma", (Idioma.IDIOMAS.firstOrNull { it.first == e.idioma }?.second ?: e.idioma), Icono.IDIOMA) { abrirMenu(Seccion.IDIOMA) },
        Opcion("Acomodar el inicio", icono = Icono.EDITAR, ayuda = "Mueve, agranda y cambia el estilo de tus widgets. También manteniendo apretado el inicio.") { cerrarPanel(); editarWidgets() },
        Opcion("Hablar con DIPPO", icono = Icono.BISA) { abrirAsistente() },
    )
    for (t in elegidos) todas.firstOrNull { it.second.titulo == t }?.let { lista += it.second }
    return lista
}


internal fun Diptico.armarOpciones(s: Seccion): List<Opcion> {
    GRUPOS_AJUSTES[s]?.let { partes -> return submenus(s, partes) }
    val e = estado.value
    val lista = mutableListOf<Opcion>()
    when (s) {
        Seccion.G_PERFIL, Seccion.G_ESTILO, Seccion.G_TEMAS, Seccion.G_WIDGETS, Seccion.G_BIBLIOTECA, Seccion.G_JUEGOS, Seccion.G_CUENTAS, Seccion.G_CONTROLES, Seccion.G_SONIDO, Seccion.G_DIPPO, Seccion.G_SISTEMA -> Unit
        Seccion.PERFIL -> { lista += opcionesPerfil(e); lista += opcionesDiscord(e) }
        Seccion.ESTANTE -> lista += opcionesEstante(e)
        Seccion.CONSOLA -> lista += opcionesConsola(e)
        Seccion.SOBREMESA -> lista += opcionesSobremesa(e)
        Seccion.CONTROLES -> lista += opcionesBotones(e)
        Seccion.RENDIMIENTO -> lista += opcionesTurbo(e)
        Seccion.CONSOLAS -> {
            lista += opcionesIconos(e).first()
            for (c in e.consolasConJuegos) {
                val n = e.juegosPorConsola[c.id].orEmpty().size
                val propia = e.consolasPropias[c.id]?.vacia == false || e.iconosPropios.containsKey(c.id)
                lista += Opcion(
                    c.bonito, if (propia) "Personalizada ✓" else if (n == 1) "1 juego" else "$n juegos", Icono.MANDO,
                    "Cambia su nombre, color, ícono, imagen grande, logo, música, cajas y emulador.",
                ) { abrirEditarConsola(c.id, desdeMenu = true) }
            }
            if (lista.isEmpty()) lista += Opcion("Aún no hay consolas", "Elige tu carpeta de juegos", Icono.CARPETA, ayuda = "Primero elige la carpeta donde guardas tus juegos y aquí aparecerán tus consolas.") { abrirMenu(Seccion.BIBLIOTECA) }
        }
        Seccion.PANTALLAS -> {
            val q = equipo()
            lista += Opcion(
                "Tu equipo", q.nombre, Icono.MANDO,
                "${q.tipoBonito} · ${q.ramGb} GB de memoria · potencia ${q.gama}. Elige esto para que Díptico acomode solo la distribución, las vistas y los efectos para este equipo.",
            ) { optimizarParaEquipo() }
            lista += Opcion(
                "Tamaño en la pantalla de arriba", nombreZoom(e.zoomArriba), Icono.ARRIBA,
                "Automático ajusta todo según la forma de la pantalla. Compacto muestra más cosas; grande, todo más grande.",
            ) { ciclarZoom(true) }.conLista(ZOOMS.map(::nombreZoom), ZOOMS.indexOfFirst { kotlin.math.abs(it - e.zoomArriba) < 0.01f }) { fijarZoom(true, ZOOMS[it]) }
            if (e.dosDisponibles) lista += Opcion(
                "Tamaño en la pantalla de abajo", nombreZoom(e.zoomAbajo), Icono.ABAJO,
                "La pantalla de abajo de la Thor es casi cuadrada: en automático todo se acomoda a esa forma.",
            ) { ciclarZoom(false) }.conLista(ZOOMS.map(::nombreZoom), ZOOMS.indexOfFirst { kotlin.math.abs(it - e.zoomAbajo) < 0.01f }) { fijarZoom(false, ZOOMS[it]) }
            lista += opcionLista(
                if (e.dosDisponibles && !e.unaPantalla) "Barras de la pantalla de arriba" else "Barras de la pantalla", Icono.ARRIBA,
                "Cómo se ven la barra de la hora y la de accesos: sólida (con el color del tema), difusa (esmerilada sobre la imagen) o transparente (casi sin fondo).",
                ESTILOS_BARRA, ESTILOS_BARRA.map(::nombreEstiloBarra), e.estiloBarraArriba, grupo = "Barras",
            ) { fijarEstiloBarra(true, it) }
            lista += Opcion(
                "Modo de pantallas", if (e.dosDisponibles) nombreModo(e.modoPantallas) else "Una (no hay otra)", Icono.PANTALLAS,
                "Dos pantallas: arriba el juego y abajo el estante. Una: todo en la pantalla principal, como en una tele o una Odin.",
                apagada = !e.dosDisponibles,
            ) {
                if (!estado.value.dosDisponibles) {
                    avisar("Este equipo tiene una sola pantalla")
                } else {
                    val siguiente = when (estado.value.modoPantallas) { "dos" -> "una_negra"; "una_negra" -> "una_libre"; else -> "dos" }
                    elegirModo(siguiente)
                }
            }
            lista += Opcion(
                "Atenuación mientras juegas", nombreAtenuacion(e.atenuacion), Icono.NEGRO,
                "Qué tan oscura queda la otra pantalla mientras juegas. Se atenúa sola 5 segundos después de abrir el juego; tocarla la despierta.",
                apagada = e.unaPantalla,
            ) {
                val l = NIVELES_ATENUACION
                val v = l[(l.indexOfFirst { kotlin.math.abs(it - estado.value.atenuacion) < 0.005f } + 1).mod(l.size)]
                almacen.guardarTexto("atenuacion", v.toString())
                actualizar { it.copy(atenuacion = v) }
            }
            lista += Opcion(
                "Teclado", if (e.tecladoPropio) "Díptico, en la otra pantalla" else "El de Android", Icono.TEXTO,
                "Con dos pantallas, el teclado de Díptico aparece en la pantalla contraria para que veas lo que escribes. Se usa con la cruceta o tocando.",
                apagada = e.unaPantalla,
            ) {
                val v = !estado.value.tecladoPropio
                almacen.guardarBool("tecladoPropio", v)
                actualizar { it.copy(tecladoPropio = v) }
            }
            lista += Opcion(
                "Intercambiar pantallas", if (e.intercambiar) "Sí" else "No", Icono.INTERCAMBIAR,
                "Pone el estante arriba y el juego elegido abajo. También está en la barra de abajo.",
                apagada = e.unaPantalla,
            ) { intercambiar() }
            lista += Opcion(
                "Los juegos se abren en", if (e.pantallaJuegos == "abajo") "Abajo" else "Arriba", Icono.JUGAR,
                "La pantalla donde se abre el emulador al apretar A. Para un juego en especial, mantén A sobre él.",
                apagada = e.unaPantalla,
            ) {
                if (estado.value.unaPantalla) {
                    avisar("Con una pantalla, los juegos se abren en la principal")
                } else {
                    val v = if (estado.value.pantallaJuegos == "abajo") "arriba" else "abajo"
                    almacen.pantallaJuegos = v
                    actualizar { it.copy(pantallaJuegos = v) }
                }
            }
            lista += Opcion(
                "Tamaño de letra", nombreTam(e.tamTexto), Icono.TEXTO,
                "Agranda todas las letras de Díptico. Se nota al instante.",
            ) {
                val i = tamanos.indexOfFirst { kotlin.math.abs(it - estado.value.tamTexto) < 0.01f }
                fijarTam(tamanos[(i + 1) % tamanos.size])
            }
            lista += Opcion(
                "Alto contraste", if (e.extra.altoContraste) "Sí" else "No", Icono.TEMA,
                "Las letras chicas y los bordes se ven más marcados, para leer mejor con poca luz o con la vista cansada.",
            ) {
                val v = !estado.value.extra.altoContraste
                almacen.guardarBool("altoContraste", v)
                actualizar { it.copy(extra = it.extra.copy(altoContraste = v)) }
                aplicarFluidez()
            }
        }
        Seccion.INICIO -> { lista += opcionesInicio(e); lista += opcionesDock(e) }
        Seccion.TEMAS -> {
            lista += opcionesTemas(e)
            // Los estilos guardados viven con los temas (antes alargaban «Pantalla y estilo»).
            lista += opcionesPersonalizacion(e).map { it.copy(grupo = it.grupo ?: "Estilos guardados y colecciones") }
        }
        Seccion.DECORACIONES -> {
            lista += Opcion("Configurar por tema", icono = Icono.PALETA,
                ayuda = "Despliega un tema para elegir su adorno y la intensidad.") { abrirDecoraciones() }
            lista += Opcion("Animar decoraciones", if (e.decoraciones) "Sí" else "No", Icono.ESTRELLA,
                ayuda = "Enciende o apaga los adornos animados de todos los temas.") {
                val activar = !estado.value.decoraciones
                almacen.guardarBool("decoraciones", activar)
                actualizar { it.copy(decoraciones = activar) }
                aplicarTema()
            }
        }
        Seccion.LOGROS -> {
            lista += opcionesDiscord(e)
            lista += opcionesLogros(e).map { if (it.grupo.isNullOrBlank()) it.copy(grupo = "RetroAchievements") else it }
            lista += (listOf(Opcion(
                "Clave de SteamGridDB", if (e.claveSgdb.isNotBlank()) "Conectada ✓" else "Opcional", Icono.CLAVE,
                "Gratis en steamgriddb.com: entra, ve a Preferencias › API y copia tu clave. Trae arte grande y logos.",
            ) { abrirClave() }) + opcionesRaspadores(e)).map { if (it.grupo.isNullOrBlank()) it.copy(grupo = "Portadas e imágenes") else it }
        }
        Seccion.ESENCIAL -> lista += opcionesEsenciales(e)
        Seccion.MAS -> lista += opcionesMas(e)
        Seccion.ASISTENTE -> {
            lista += Opcion("Hablar con DIPPO", icono = Icono.BISA, ayuda = "DIPPO es tu asistente: te recomienda juegos de tu biblioteca, te ayuda cuando te atascas y puede cambiar el tema o la vista por ti.") { abrirAsistente() }
            lista += Opcion(
                "Logros de DIPPO", "${logrosDippoGanados()} de ${LogrosDippo.LISTA.size}", Icono.TROFEO,
                "Los logros propios de DIPPO: por jugar, por terminar juegos, por rachas de días y por conversar con él. Cuando ganas uno, DIPPO lo celebra.",
            ) { abrirGaleria("logros_dippo") }
            lista += Opcion(
                "DIPPO en el inicio", if (e.extra.dippoInicio) "Sí" else "No", Icono.BISA,
                "DIPPO se queda a la vista en el inicio y te saluda al llegar. Tócalo para hablarle; mantenlo apretado para dictarle.",
            ) {
                val v = !estado.value.extra.dippoInicio
                almacen.guardarBool("dippoInicio", v)
                actualizar { it.copy(extra = it.extra.copy(dippoInicio = v)) }
            }
            lista += Opcion(
                "Saludo e ideas al llegar", if (e.extra.dippoSaludo) "Sí" else "No", Icono.BISA,
                "Cuando vuelves al inicio, DIPPO te saluda con un globito y te propone algo: seguir tu partida, volver a un juego olvidado o elegir por ti. Toca el globito para hacerlo.",
            ) {
                val v = !estado.value.extra.dippoSaludo
                almacen.guardarBool("dippoSaludo", v)
                actualizar { it.copy(extra = it.extra.copy(dippoSaludo = v)) }
            }
            lista += Opcion(
                "Trucos y datos escondidos", if (e.extra.dippoDatos) "Sí" else "No", Icono.CHISPA,
                "De vez en cuando DIPPO te cuenta, en su globito, una función de Díptico que quizás no conocías. Desliza el globito para sacarlo.",
            ) {
                val v = !estado.value.extra.dippoDatos
                almacen.guardarBool("dippoDatos", v)
                actualizar { it.copy(extra = it.extra.copy(dippoDatos = v)) }
            }
            lista += Opcion(
                "Avisos de DIPPO en la campana", if (e.extra.dippoAvisos) "Sí" else "No", Icono.CAMPANA,
                "Calor, batería baja y versiones nuevas (de Díptico y de tus emuladores) también quedan guardados en tus notificaciones, arriba, para verlos cuando quieras.",
            ) {
                val v = !estado.value.extra.dippoAvisos
                almacen.guardarBool("dippoAvisos", v)
                actualizar { it.copy(extra = it.extra.copy(dippoAvisos = v)) }
                if (!v) Avisos.lista.value.filter { it.esDippo }.forEach { Avisos.descartar(it) }
            }
            lista += Opcion(
                "Temperatura y consejos al jugar", if (e.extra.termica) "Sí" else "No", Icono.BATERIA,
                "Mientras juegas, la pantalla compañera muestra qué tan caliente está la consola (verde a rojo) y DIPPO te aconseja si se calienta, si queda poca batería o si llevas mucho rato. Solo informa: Android no deja controlar el ventilador.",
            ) {
                val v = !estado.value.extra.termica
                almacen.guardarBool("termica", v)
                actualizar { it.copy(extra = it.extra.copy(termica = v)) }
            }
            if (e.extra.termica) lista += Opcion(
                "Sensores de temperatura", try { Termica.resumenSensores(Termica.leer(app)) } catch (_: Exception) { "Sin datos" }, Icono.INFO,
                "Qué temperaturas deja leer este equipo. La batería casi siempre se ve; el procesador, solo si Android lo permite. Con lo que haya, DIPPO te avisa.",
            ) {}
            lista += Opcion(
                "Responder con voz", if (e.extra.dippoVoz) "Sí" else "No", Icono.MICROFONO,
                "Cuando le hablas a DIPPO (mantenlo apretado o toca Hablar), te responde en voz alta. Así no tienes que escribir ni leer.",
            ) {
                val v = !estado.value.extra.dippoVoz
                almacen.guardarBool("dippoVoz", v)
                actualizar { it.copy(extra = it.extra.copy(dippoVoz = v)) }
                if (!v) cl.nikalbazu.diptico.audio.VozDippo.callar()
            }
            lista += opcionLista(
                "Colores de DIPPO", Icono.PALETA,
                "Según la hora es cálido de día y azulado desde las 8 de la noche. También puedes dejarlo siempre de día o siempre de noche.",
                COLORES_DIPPO, COLORES_DIPPO.map(::nombreColorDippo), e.extra.dippoColor,
            ) { v ->
                almacen.guardarTexto("dippoColor", v)
                actualizar { it.copy(extra = it.extra.copy(dippoColor = v)) }
            }
            lista += Opcion(
                "DIPPO mientras juegas", if (e.bisaEnJuego) "Sí" else "No", Icono.MANDO,
                "Mientras juegas deja un aviso para pedir ayuda, y si reintentas varias veces seguidas la misma partida te ofrece una pista. Se abre en la pantalla libre.",
            ) {
                val v = !estado.value.bisaEnJuego
                almacen.guardarBool("bisaEnJuego", v)
                actualizar { it.copy(bisaEnJuego = v) }
            }
            lista += Opcion(
                "Burbuja flotante", if (cl.nikalbazu.diptico.bisa.BurbujaBisa.puedeFlotar(app)) "Activada ✓" else "Dar permiso", Icono.BISA,
                "Para juegos que usan las dos pantallas (DS, 3DS) o el modo una pantalla: DIPPO aparece como una burbuja encima del juego, con su botón para volver.",
            ) { if (cl.nikalbazu.diptico.bisa.BurbujaBisa.puedeFlotar(app)) avisar("La burbuja ya tiene permiso") else cl.nikalbazu.diptico.bisa.BurbujaBisa.pedirPermisoFlotante(app) }
            lista += Opcion(
                "IA de DIPPO", textoMotor(e), Icono.CHISPA,
                "Su cerebro: la IA de tu consola (gratis, sin internet), Gemini (Google, con nivel gratis) o ChatGPT. Cada persona usa su propia clave.",
            ) { abrirGaleria("ia") }
            lista += Opcion(
                "Buscar guías en internet", if (e.bisaWeb) "Sí" else "No", Icono.WEB,
                "Con Gemini o ChatGPT, DIPPO busca en internet para darte datos exactos de cada juego. La IA de tu consola no busca.",
            ) {
                val v = !estado.value.bisaWeb
                almacen.guardarBool("bisaWeb", v)
                actualizar { it.copy(bisaWeb = v) }
            }
            lista += Opcion(
                "IA en tu consola",
                when { e.iaLocalLista -> "Lista ✓ · ${IALocal.NOMBRE_MODELO}"; trabajosMapa.containsKey("ialocal") -> "Bajando…"; else -> "Bajar (1,6 GB)" },
                Icono.DESCARGA,
                "Un modelo de IA libre (${IALocal.NOMBRE_MODELO}) que corre en tu consola. Se baja una vez; después DIPPO funciona sin internet y sin gastar créditos, aunque compartas Díptico.",
            ) { if (estado.value.iaLocalLista) avisar("La IA local ya está lista") else descargarIALocal() }
            for (prov in IA.PROVEEDORES) {
                val clave = claveDe(prov, e)
                lista += Opcion(
                    "Clave de ${IA.nombre(prov)}" + if (prov == "gemini") " (gratis)" else "",
                    if (clave.isBlank()) "Pegar" else "Guardada ✓", Icono.CLAVE,
                    "Se saca en ${IA.dondeClave(prov)}. Solo se guarda en tu consola." + if (prov == "gemini") " Con tu cuenta de Google es gratis (con un límite diario)." else " Usa el saldo de tu cuenta.",
                ) { abrirTexto(claveTexto(prov), clave) }
            }
            lista += Opcion(
                "Usar un modelo propio", icono = Icono.CARPETA,
                ayuda = "Si tienes otro modelo en formato .task de MediaPipe (por ejemplo Gemma), elígelo y DIPPO lo usará.",
            ) { pedirArchivoPara("modeloIA", "*/*") }
            if (e.iaLocalLista) lista += Opcion("Borrar la IA local", "Libera 1,6 GB", Icono.BORRAR, ayuda = "Borra el modelo de IA que se descargó a la consola. DIPPO sigue funcionando con lo básico o con la nube.") {
                IALocal.borrar(app)
                actualizar { it.copy(iaLocalLista = false) }
                avisar("IA local borrada")
            }
            lista += Opcion("Borrar la conversación", if (e.chat.isEmpty()) "Vacía" else "${e.chat.size} mensajes", Icono.BORRAR, ayuda = "Borra lo que hablaste con DIPPO. No toca tus ajustes ni tus juegos.") { borrarChat(); avisar("Conversación borrada") }
        }
        Seccion.IDIOMA -> Idioma.IDIOMAS.forEach { (cod, nombre) ->
            lista += Opcion(nombre, if (e.idioma == cod) "En uso ✓" else "", Icono.IDIOMA, Idioma.ayuda(cod)) { cambiarIdioma(cod) }
        }
        Seccion.APARIENCIA -> {
            lista += opcionLista(
                "Animaciones", Icono.CHISPA,
                "Cómo se mueven el menú, las ventanas y las carpetas al abrirse y cerrarse. Apagadas: todo aparece al instante. Intensas: con un leve rebote.",
                listOf("apagadas", "suaves", "normales", "intensas"), listOf("Apagadas", "Suaves", "Normales", "Intensas"),
                e.extra.animaciones, grupo = "Movimiento",
            ) { v -> almacen.guardarTexto("animaciones", v); actualizar { it.copy(extra = it.extra.copy(animaciones = v)) } }
            lista += opcionLista(
                "Portada de arriba", Icono.TEMA,
                "Vitrina de cajas: la caja del juego al centro, con las de al lado asomándose inclinadas. PS5 con caja: el arte del juego de fondo, su logo e información a la izquierda y su caja 3D flotando (abajo quedan los íconos para elegir). Arte del juego: la imagen en grande con su información.",
                listOf("cajas", "ps5", "arte"), listOf("Vitrina de cajas", "PS5 con caja", "Arte del juego"), e.extra.portada, grupo = "Movimiento",
            ) { v ->
                almacen.guardarTexto("portadaArriba", v)
                actualizar { it.copy(extra = it.extra.copy(portada = v)) }
                // Con la PS5 arriba, abajo va el selector de íconos (se puede cambiar después en «Vista de juegos»).
                if (v == "ps5" && estado.value.vista != "iconos") { almacen.guardarTexto("vista", "iconos"); actualizar { it.copy(vista = "iconos") } }
            }
            lista += Opcion(
                "Fondo difuminado con la caja 3D", if (e.extra.fondoDifuminado) "Sí" else "No", Icono.PORTADA,
                "Aplica un leve desenfoque al arte de fondo cuando arriba se ve la caja 3D, para que la caja destaque. Apágalo si prefieres el arte nítido.",
                grupo = "Movimiento",
            ) {
                val v = !estado.value.extra.fondoDifuminado
                almacen.guardarBool("fondoDifuminado", v)
                actualizar { it.copy(extra = it.extra.copy(fondoDifuminado = v)) }
            }
            lista += Opcion(
                "Logo de la consola en las cajas", if (e.extra.bandaCaja) "Sí" else "No", Icono.PORTADA,
                "Una franja arriba del frente con el logo de la consola, como en las cajas de PS2 o Switch. Usa el logo que elijas para la consola (Editar consola › Logo); si no hay, su nombre. Apágalo si tus portadas ya traen la franja.",
                grupo = "Movimiento",
            ) {
                val v = !estado.value.extra.bandaCaja
                almacen.guardarBool("bandaCaja", v)
                actualizar { it.copy(extra = it.extra.copy(bandaCaja = v)) }
            }
            lista += Opcion(
                "Pantalla de carga", if (e.extra.cargaJuego) "Sí" else "No", Icono.JUGAR,
                "Al abrir un juego, la pantalla se tiñe del color de su consola, aparece su caja y se funde a negro antes de empezar. Si la apagas, el juego se abre al instante.",
                grupo = "Movimiento",
            ) {
                val v = !estado.value.extra.cargaJuego
                almacen.guardarBool("cargaJuego", v)
                actualizar { it.copy(extra = it.extra.copy(cargaJuego = v)) }
            }
            lista += Opcion("Modo", if (e.oscuro) "Oscuro" else "Claro", Icono.TEMA, "El oscuro ahorra batería en pantallas OLED. Algunos temas usan siempre el suyo.") { alternarTema() }
            lista += Opcion(
                "Fluidez", nombreFluidez(e.fluidez) + if (e.fluidez == "auto") " (${nombreFluidez(e.fluidezDetectada).lowercase()})" else "", Icono.CHISPA,
                "Automática elige según tu consola. Alta usa la máxima tasa de refresco y todos los efectos; ahorro apaga difuminados y decoraciones para ir más liviano.",
            ) {
                val l = listOf("auto", "alta", "equilibrada", "ahorro")
                val v = l[(l.indexOf(estado.value.fluidez) + 1).mod(l.size)]
                almacen.guardarTexto("fluidez", v)
                actualizar { it.copy(fluidez = v) }
                aplicarFluidez()
            }.let { o ->
                val l = listOf("auto", "alta", "equilibrada", "ahorro")
                o.conLista(l.map(::nombreFluidez), l.indexOf(e.fluidez)) { n ->
                    almacen.guardarTexto("fluidez", l[n])
                    actualizar { it.copy(fluidez = l[n]) }
                    aplicarFluidez()
                }
            }
            lista += Opcion(
                "Medidor de fluidez", if (e.extra.medidorFps) "Sí" else "No", Icono.CHISPA,
                "Muestra en una esquina de cada pantalla cuántos cuadros por segundo dibuja Díptico y el cuadro más lento. " +
                    "Sirve para ver qué pantalla o qué ajuste lo pone lento. Mientras está encendido gasta un poco más de batería.",
            ) {
                val v = !estado.value.extra.medidorFps
                almacen.guardarBool("medidorFps", v)
                actualizar { it.copy(extra = it.extra.copy(medidorFps = v)) }
            }
            lista += Opcion(
                "Enviar informe de fluidez", if (Fluidez.cuantas() > 0) "${Fluidez.cuantas()} s medidos" else "Enciende el medidor", Icono.CHISPA,
                "Con el medidor encendido, Díptico anota en qué pantalla y en qué vista se pone lento. Úsalo un rato (carpetas, vitrina, ajustes) y envía el informe: así se arregla justo lo que va lento en tu consola.",
            ) { enviarInformeFluidez() }
            lista += Opcion(
                "Protector de pantalla", if (e.protectorMin == 0) "Nunca" else "A los ${e.protectorMin} min", Icono.RELOJ,
                "Si no usas la consola, muestra tus juegos con las dos pantallas atenuadas. Cualquier botón lo cierra.",
            ) {
                val l = listOf(2, 5, 10, 20, 0)
                val v = l[(l.indexOf(estado.value.protectorMin) + 1).mod(l.size)]
                almacen.guardarTexto("protectorMin", v.toString())
                actualizar { it.copy(protectorMin = v) }
            }.let { o ->
                val l = listOf(2, 5, 10, 20, 0)
                o.conLista(l.map { if (it == 0) "Nunca" else "A los $it min" }, l.indexOf(e.protectorMin)) { n ->
                    almacen.guardarTexto("protectorMin", l[n].toString())
                    actualizar { it.copy(protectorMin = l[n]) }
                }
            }
            lista += opcionesIconos(e).map { it.copy(grupo = it.grupo ?: "Íconos de las consolas") }
            lista += Opcion(
                "Animación de entrada", if (e.mostrarIntro) "Sí" else "No", Icono.INTRO,
                "La animación con el logo de Díptico al encender.",
            ) {
                val v = !estado.value.mostrarIntro
                almacen.mostrarIntro = v
                actualizar { it.copy(mostrarIntro = v) }
            }
        }
        Seccion.MUSICA -> {
            lista += opcionLista(
                "Vibración", Icono.CHISPA,
                "Un toque de vibración al moverte y al elegir, para sentir cada paso. Suave, media o fuerte. Nunca vibra mientras juegas.",
                cl.nikalbazu.diptico.audio.Vibracion.NIVELES, cl.nikalbazu.diptico.audio.Vibracion.NIVELES.map(cl.nikalbazu.diptico.audio.Vibracion::nombre),
                e.vibracion, grupo = "Sonidos",
            ) { v ->
                almacen.guardarTexto("vibracion", v)
                cl.nikalbazu.diptico.audio.Vibracion.nivel = v
                actualizar { it.copy(extra = it.extra.copy(vibracion = v)) }
                cl.nikalbazu.diptico.audio.Vibracion.probar()
            }
            lista += Opcion(
                "Música de fondo", if (e.musica) "Sí" else "No", Icono.MUSICA,
                "Suena mientras eliges. Se calla al abrir un juego o si otra app pone música.",
            ) { fijarMusica(!estado.value.musica) }
            lista += Opcion(
                "Música según el tema", if (e.musicaTema) "Sí" else "No", Icono.PALETA,
                "Cada tema trae su música: Halloween suena a fantasmas, Navidad a cascabeles, Sakura a flauta japonesa… Ahora: ${nombrePista(e.pistaTema)}.",
            ) {
                val v = !estado.value.musicaTema
                almacen.guardarBool("musicaTema", v)
                actualizar { it.copy(musicaTema = v) }
            }
            val sonando = if (e.musicaTema && e.pista != "mia") e.pistaTema else e.pista
            lista.add(
                0,
                Opcion(
                    "Elegir canción", if (e.musica) nombrePista(sonando) else "Música apagada", Icono.MUSICA,
                    "Todas las canciones de Díptico en una lista, para escucharlas y elegir.",
                ) { abrirGaleria("musica") },
            )
            lista += Opcion(
                "Sonidos al moverte", if (e.sonidos) "Sí" else "No", Icono.VOLUMEN,
                "Un sonido suave al moverte, elegir, volver y abrir un juego.",
            ) {
                val v = !estado.value.sonidos
                almacen.guardarBool("sonidos", v)
                Sonidos.activo = v
                actualizar { it.copy(sonidos = v) }
                if (v) Sonidos.aceptar()
            }
            lista += Opcion(
                "Juego de sonidos", nombrePaqueteSonidos(e.paqueteSonidos), Icono.VOLUMEN,
                "Del tema: cada tema trae los suyos. Clásicos: campanitas suaves. Bloques: madera y pops. Retro: 8 bits. Burbuja: gotitas.",
                menos = { cambiarPaqueteSonidos(-1) },
            ) { cambiarPaqueteSonidos(1) }
            lista += Opcion("Volumen", nombreVolumen(e.volumen), Icono.VOLUMEN, "Qué tan fuerte suena la música de Díptico. Es independiente del volumen de tus juegos.") {
                val i = volumenes.indexOfFirst { kotlin.math.abs(it - estado.value.volumen) < 0.01f }
                val v = volumenes[(i + 1) % volumenes.size]
                almacen.volumen = v
                actualizar { it.copy(volumen = v) }
            }
            lista += Opcion(
                "Mi propia música", if (e.carpetaMusica != null) "Cambiar carpeta" else "Elegir carpeta", Icono.CARPETA,
                "Elige una carpeta con canciones (mp3, ogg, m4a, flac). Suenan en orden aleatorio.",
            ) { pedirCarpeta?.invoke(Carpeta.MUSICA) }
        }
        Seccion.EMULADORES -> {
            val nuevas = e.extra.emus.count { it.nueva }
            lista += Opcion(
                "Emuladores al día", if (nuevas > 0) "$nuevas con versión nueva" else if (e.extra.emusRevisado > 0) "Al día" else "Revisar",
                Icono.DESCARGA,
                "DIPPO revisa qué emuladores tienes, cuáles usas y si hay versión nueva en su página; los baja y Android los instala encima, como Obtainium.",
                grupo = "Emuladores al día",
            ) { cerrarPanel(); abrirNativos(pestana = 2) }
            lista += Opcion(
                "Revisar cada día", if (e.extra.emusAuto) "Sí" else "No", Icono.RELOJ,
                "Una vez al día, un rato después de abrir Díptico, DIPPO revisa solo y te avisa si hay algo nuevo.",
                grupo = "Emuladores al día",
            ) {
                val v = !estado.value.extra.emusAuto
                almacen.guardarBool("emusAuto", v)
                actualizar { it.copy(extra = it.extra.copy(emusAuto = v)) }
            }
            val consolas = e.consolasEmu.ifEmpty { Consolas.todas.filter { !it.sinEmulador }.take(20) }
            for (c in consolas) {
                lista += Opcion(
                    c.bonito,
                    if (emuladorListo(c)) nombreEmulador(c) else "Falta: ${c.sugerido}",
                    Icono.EMULADOR,
                    "Aprieta A para pasar al siguiente emulador instalado. Con RetroArch puedes elegir el núcleo; " +
                        "el núcleo debe estar descargado dentro de RetroArch.",
                ) { ciclarEmulador(c) }
            }
        }
        Seccion.PORTADAS -> {
            if (e.seleccionado != null) lista += Opcion("Buscar imágenes del juego", "Portadas por fuente", Icono.PORTADA,
                "Elige imágenes para el juego seleccionado. Libretro, Wikipedia y Steam para PC funcionan sin clave.") {
                abrirFicha(); buscarCandidatos("portada")
            }
            lista += Opcion(
                "Reconocer nombres de juegos", icono = Icono.TITULO,
                ayuda = "Busca el nombre oficial de cada juego en libretro (gratis). Se hace solo al buscar juegos nuevos.",
            ) { identificarNombres(avisarAlFinal = true) }
            lista += Opcion(
                "Reconocer nombres con IA", if (llaveNube(e) == null) "Necesita IA en la nube" else "Para los que faltan", Icono.CHISPA,
                "La IA reconoce los juegos que libretro no conoce (Switch, PS3, archivos con nombres raros) y les pone su nombre real.",
            ) { identificarConIA() }
            val d = e.descarga
            lista += Opcion(
                if (d != null) "Descargando…" else "Descargar lo que falta",
                if (d != null) "${d.hechos} de ${d.total}" else "${e.sinPortada} sin portada",
                Icono.DESCARGA,
                "Busca en libretro (gratis) y, si conectaste tu clave, en SteamGridDB. No toca tus propias imágenes.",
            ) { if (estado.value.descarga == null) descargarPortadas(todo = false) else cancelarDescarga() }
            lista += Opcion(
                "Buscar de nuevo todo", "Incluye los que ya tienen", Icono.BUSCAR,
                "Vuelve a buscar títulos, arte y logos para todos los juegos.",
            ) { if (estado.value.descarga == null) descargarPortadas(todo = true) }
            lista += Opcion(
                "Fuentes y claves", "En Cuentas y conexiones", Icono.CLAVE,
                "SteamGridDB, ScreenScraper, IGDB y las demás fuentes de imágenes están en Ajustes › Cuentas y conexiones.",
            ) { abrirMenu(Seccion.G_CUENTAS) }
            lista += Opcion(
                "Títulos de libretro", if (e.titulosBonitos) "Sí" else "No", Icono.TITULO,
                "Muestra el nombre oficial del juego en vez del nombre del archivo.",
            ) {
                val v = !estado.value.titulosBonitos
                almacen.titulosBonitos = v
                actualizar { it.copy(titulosBonitos = v) }
                publicarJuegos()
            }
            lista += Opcion(
                "Imágenes de ES-DE", if (e.tieneMedia) "Conectadas ✓" else "Opcional", Icono.CARPETA,
                "Si ya usas ES-DE, elige su carpeta downloaded_media y uso esas portadas.",
            ) { pedirCarpeta?.invoke(Carpeta.MEDIA) }
            lista += Opcion("Borrar lo descargado", icono = Icono.BORRAR, ayuda = "Libera espacio. Tus imágenes propias no se tocan.") {
                cancelarDescarga()
                scope.launch {
                    withContext(Dispatchers.IO) { Descargas.borrar(app) }
                    medios = emptyMap()
                    almacen.guardarMedios(medios)
                    publicarJuegos()
                    avisar("Listo, borré las imágenes descargadas")
                }
            }
        }
        Seccion.BIBLIOTECA -> {
            lista += Opcion("Importar lista de ES-DE", icono = Icono.DESCARGA,
                ayuda = "Elige gamelist.xml tras escanear tus juegos. Revisa los títulos y favoritos antes de importar.") {
                pedirArchivoPara("es-de", "*/*")
            }
            lista += Opcion(
                "Volver a buscar juegos", if (e.escaneando) "Buscando…" else "${e.juegos.size} juegos", Icono.BUSCAR,
                "Úsalo cuando agregues juegos nuevos a tu carpeta.",
            ) { escanear() }
            val ocultos = e.datos.count { it.value.oculto }
            lista += Opcion(
                "Juegos ocultos", if (ocultos == 0) "Ninguno" else "$ocultos", Icono.OCULTAR,
                "Los juegos que ocultaste desde sus opciones (Y). Desde aquí los ves y los vuelves a mostrar.",
            ) { abrirJuegosOcultos() }
            lista += Opcion(
                "Carpetas de juegos", "${almacen.carpetas().size} carpetas", Icono.CARPETA,
                "Agrega todas las que quieras (memoria interna, SD…). Si una carpeta no tiene el nombre de una consola, le eliges la consola.",
            ) { abrirCarpetas() }
            lista += Opcion(
                "Ports y apps", if (e.ports.isEmpty()) "Elegir" else "${e.ports.size} elegidos", Icono.APPS,
                "Los ports son juegos hechos para Android (Ship of Harkinian, los de Doom, Mario 64…). " +
                    "Márcalos y aparecen en su propio estante, con tiempo jugado y próximo paso.",
            ) { abrirPorts() }
            lista += Opcion(
                "Juegos de Android", if (e.android.isEmpty()) "Elegir" else "${e.android.size} juegos", Icono.MOVIL,
                "Genshin, Minecraft y cualquier juego instalado. Tienen su propio estante, ficha, guías y personalización.",
            ) { abrirPorts(Consolas.ANDROID) }
            lista += Opcion(
                "Buscar juegos de Android nuevos", icono = Icono.BUSCAR,
                ayuda = "Agrega solos los juegos que Android reconoce como juego.",
            ) { detectarAndroid() }
            lista += Opcion("Ir a lo último que jugaste", icono = Icono.CONTINUAR, ayuda = "Salta directo a tu último juego.") {
                cerrarPanel(); continuar()
            }
            lista += opcionesPC(e)
        }
        Seccion.SISTEMA -> {
            lista += opcionesRespaldo(e)
            lista += opcionesActualizar(e)
            lista += Opcion(
                "Reportar un problema", icono = Icono.INFO,
                ayuda = "Describe qué pasó y revisa el envío del diagnóstico al equipo de Díptico.",
            ) { prepararReporte() }
            lista += Opcion("Ver las novedades de esta versión", icono = Icono.CHISPA, ayuda = "El tour corto de lo nuevo, otra vez.") { cerrarPanel(); actualizar { it.copy(novedades = 0, recorrido = false, novedadesDesde = "") } }
            lista += Opcion("Recorrido por Díptico", icono = Icono.TUTORIAL, ayuda = "El paseo de la primera vez por todas las opciones y botones.") { verRecorrido() }
            lista += Opcion("Repetir el tutorial", icono = Icono.TUTORIAL, ayuda = "Vuelve a configurar todo paso a paso.") { reiniciarTutorial() }
            lista += Opcion(
                "Acerca de Díptico", "Versión $VERSION.${Actualizador.numeroActual(app)}", Icono.INFO,
                "Hecho para la AYN Thor y consolas de una pantalla. Abre los emuladores con las reglas de ES-DE.",
            ) { avisar("Díptico $VERSION · hecho con cariño para jugar") }
            lista += Opcion("Cerrar el menú", icono = Icono.CERRAR, ayuda = "También con B o Start.") { cerrarPanel() }
        }
    }
    // Las opciones de un mismo grupo quedan juntas, bajo un solo subtítulo (en el orden en que aparece cada grupo).
    if (s == Seccion.ESENCIAL || s == Seccion.MAS) return lista
    return lista.groupBy { grupoOpcion(s, it) }.values.flatten()
}


/** Partes que van enteras en un solo submenú (sus grupos internos son muy chicos para separarlos). */
private val PARTES_ENTERAS = setOf(Seccion.SOBREMESA, Seccion.RENDIMIENTO, Seccion.CONSOLAS, Seccion.IDIOMA, Seccion.CONSOLA, Seccion.DECORACIONES, Seccion.EMULADORES)

/** El nombre de un submenú: el del grupo, con nombres más claros para los genéricos. */
private fun nombreSubmenu(parte: Seccion, g: String, entera: Boolean): String = when {
    entera || g.isBlank() -> when (parte) { Seccion.RENDIMIENTO -> "Turbo al jugar"; Seccion.SOBREMESA -> "En la tele"; else -> parte.titulo }
    g == "Más" || g == "Más opciones" -> "Más de ${parte.titulo.lowercase()}"
    parte == Seccion.TEMAS && g == "Elegir" -> "Temas"
    parte == Seccion.TEMAS && g == "Con IA" -> "Temas con IA"
    parte == Seccion.APARIENCIA && g == "Detalles" -> "Animaciones y detalles"
    parte == Seccion.ESTANTE && g == "Qué ves y en qué orden" -> "Vistas y orden"
    parte == Seccion.ESTANTE && g == "Carpetas" -> "Carpetas de consolas"
    parte == Seccion.PANTALLAS && g == "Escribir" -> "Teclado"
    parte == Seccion.PORTADAS && g == "Imágenes" -> "Portadas e imágenes"
    parte == Seccion.SISTEMA && g == "Díptico" -> "Acerca de Díptico"
    else -> g
}

/**
 * Una sección de Ajustes armada con submenús: cada grupo de opciones es una fila «Nombre ›» que abre su ventana
 * (las opciones adentro, con su explicación y su vista previa). Un grupo de una sola opción va directo.
 * En DIPPO, hablar, los logros y los premios quedan a la vista al entrar.
 */
internal fun Diptico.submenus(s: Seccion, partes: List<Seccion>): List<Opcion> {
    // Todas las opciones de la sección, cada una con el nombre de su grupo (para los títulos dentro de «Más»).
    val todas = mutableListOf<Opcion>()
    for (parte in partes) {
        val entera = parte in PARTES_ENTERAS
        for (o in armarOpciones(parte)) todas += o.copy(grupo = nombreSubmenu(parte, if (entera) "" else grupoOpcion(parte, o), entera))
    }
    if (s == Seccion.G_DIPPO && todas.none { it.titulo.startsWith("Premios") }) {
        val i = todas.indexOfFirst { it.titulo.startsWith("Logros") }
        todas.add(if (i >= 0) i + 1 else 0, Opcion(
            "Premios de los logros", "Ver", Icono.TROFEO,
            "Los marcos, títulos y temas que regalan los logros de DIPPO. Los que ya ganaste se usan con un toque.", grupo = "DIPPO",
        ) { abrirGaleria("logros_dippo", 1) })
    }
    // Lo más usado de la sección va a la vista (en este orden); si faltan, se completa con la primera de cada grupo.
    val principales = mutableListOf<Opcion>()
    for (t in PRINCIPALES[s].orEmpty()) todas.firstOrNull { it.titulo == t && it !in principales }?.let { principales += it }
    for (o in todas) {
        if (principales.size >= MAX_PRINCIPALES) break
        if (o !in principales && principales.none { it.grupo == o.grupo }) principales += o
    }
    val resto = todas.filter { it !in principales }
    val salida = principales.map { it.copy(grupo = null) }.toMutableList()
    if (resto.isNotEmpty()) {
        val nombre = "Más de ${s.titulo.lowercase()}"
        val grupos = resto.mapNotNull { it.grupo }.distinct()
        salida += Opcion(
            nombre, "${resto.size} ›", Icono.MENU,
            "El resto de las opciones de ${s.titulo.lowercase()}, ordenadas por tema: " + grupos.take(6).joinToString(", ") { Idioma.t(it) } + if (grupos.size > 6) "…" else ".",
            sub = resto,
        ) { abrirSubOpciones(nombre) }
    }
    return salida
}

/** Cuántas opciones van a la vista en cada sección (el resto, en «Más de …»). */
private const val MAX_PRINCIPALES = 6

/** Lo más usado de cada sección de Ajustes, en el orden en que se muestra. */
private val PRINCIPALES: Map<Seccion, List<String>> = mapOf(
    Seccion.G_ESTILO to listOf("Tienda de temas", "Modo", "Íconos de consola", "Íconos en las carpetas", "Portada de arriba", "Medidor de fluidez"),
    Seccion.G_BIBLIOTECA to listOf("Vista de juegos", "Tamaño de los juegos", "Estilo de las carpetas", "Tono de las carpetas", "Ordenar carpetas", "Switch 2 y ports nativos"),
    Seccion.G_WIDGETS to listOf("Acomodar el inicio", "Agregar widgets", "Elegir las apps del dock", "Barra de abajo", "Preguntar dónde quedaste", "Usar el inicio nuevo"),
    Seccion.G_JUEGOS to listOf("Modo turbo al jugar", "Optimizar ahora"),
    Seccion.G_DIPPO to listOf("Hablar con DIPPO", "Logros de DIPPO", "Premios de los logros", "DIPPO en el inicio", "Avisos de DIPPO en la campana", "Temperatura y consejos al jugar"),
    Seccion.G_CONTROLES to listOf("Sticks", "Música de Díptico", "Volumen"),
    Seccion.G_CUENTAS to listOf("Iniciar sesión con Discord", "Tu cuenta de Discord", "Mostrar «Jugando a…» en Discord", "Conectar RetroAchievements", "Clave de SteamGridDB", "ScreenScraper"),
    Seccion.G_SISTEMA to listOf("Respaldar ahora", "Idioma", "Buscar actualizaciones", "Ver las novedades de esta versión", "Recorrido por Díptico", "Enviar informe de fluidez"),
)


/** Qué hace una opción y cómo se cambia: su ayuda, o una explicación armada con su valor si no tiene. */
fun explicacion(o: Opcion): String = o.ayuda.ifBlank {
    when {
        o.sub != null -> "Aquí están juntas: " + o.sub.take(5).joinToString(", ") { Idioma.t(it.titulo) } + "."
        o.alternativas != null -> "Elige entre: " + o.alternativas.take(5).joinToString(", ") { Idioma.t(it) } + ". Con A abres la lista y en la vista previa ves cómo queda."
        o.valor.isNotBlank() -> "Ahora: ${o.valor}. Con A la cambias (← y → también) y en la vista previa ves cómo queda."
        else -> "Con A la usas. En la vista previa ves qué cambia."
    }
}


/** La opción cuyo menú desplegable está abierto: la del submenú si hay uno abierto, si no la de la sección. */
fun Diptico.opcionDesplegada(e: Estado): Opcion? =
    if (e.extra.desplegable < 0) null
    else subOpciones(e)?.getOrNull(e.extra.desplegable) ?: opcionesSeccion(e.seccion).getOrNull(e.extra.desplegable)


/** Manda el informe de fluidez por el mismo camino que el reporte de fallas (con su confirmación). */
fun Diptico.enviarInformeFluidez() {
    if (Fluidez.cuantas() < 10) { avisar("Enciende el medidor de fluidez y usa Díptico un rato antes de enviar"); return }
    val equipo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.RELEASE}"
    val detalle = Fluidez.informe(equipo) + "\n\n" + Fallas.informe(app, "Datos de la versión", null)
    reportePendiente = Fallas.Reporte(detalle, false)
    confirmarReporte("Informe de fluidez (medidor de fps)")
}

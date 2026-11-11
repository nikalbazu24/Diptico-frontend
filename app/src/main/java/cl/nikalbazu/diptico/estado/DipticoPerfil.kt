package cl.nikalbazu.diptico.estado

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
 * Tu perfil, su editor y «Tu año»: funciones de [Diptico] separadas del archivo central para que sea más fácil de mantener.
 * Se llaman igual que antes (Diptico.editarPerfil(), etc.).
 */

// ---------- Tu perfil ----------

/** Lee tu perfil guardado; la primera vez lo crea (con la fecha de hoy como «desde»). */
internal fun Diptico.cargarPerfil(): Perfil {
    val t = almacen.texto("perfil")
    return Perfil.leer(t).also { if (t.isBlank()) almacen.guardarTexto("perfil", it.json()) }
}

/** Abre la tarjeta de tu perfil en [pantalla]; crece desde el punto ([ox], [oy]) = fracción del ancho y del alto donde está tu foto. */
fun Diptico.abrirPerfil(pantalla: Int = android.view.Display.DEFAULT_DISPLAY, ox: Float = 0.92f, oy: Float = 0.07f) {
    actualizar { it.copy(extra = it.extra.copy(perfilEn = pantalla, perfilOx = ox.coerceIn(0f, 1f), perfilOy = oy.coerceIn(0f, 1f), amigosEn = -1)) }
}

/** La tarjeta pasa a [pantalla] (al editar con dos pantallas, se queda arriba). */
fun Diptico.moverPerfilA(pantalla: Int) {
    if (estado.value.extra.perfilEn >= 0 && estado.value.extra.perfilEn != pantalla) actualizar { it.copy(extra = it.extra.copy(perfilEn = pantalla)) }
}

fun Diptico.cerrarPerfil() {
    if (estado.value.extra.perfilEn >= 0) actualizar { it.copy(extra = it.extra.copy(perfilEn = -1, edicionPerfil = null)) }
}

// ---------- Tu año ----------

/** Abre «Tu año» (el resumen de lo que jugaste) en [pantalla]; cierra el perfil y los amigos si estaban abiertos. */
fun Diptico.abrirTuAno(pantalla: Int = android.view.Display.DEFAULT_DISPLAY) {
    actualizar { it.copy(extra = it.extra.copy(anioEn = pantalla, perfilEn = -1, amigosEn = -1)) }
}

fun Diptico.cerrarTuAno() {
    if (estado.value.extra.anioEn >= 0) actualizar { it.copy(extra = it.extra.copy(anioEn = -1)) }
}

/** En diciembre, avisa una sola vez por año que «Tu año» ya está listo. */
internal fun Diptico.vigilarTuAno() {
    scope.launch {
        delay(20_000)
        while (true) {
            val c = java.util.Calendar.getInstance()
            val anio = c.get(java.util.Calendar.YEAR)
            if (TuAno.tocaAvisar(c.get(java.util.Calendar.MONTH) + 1, anio, almacen.texto("tuAnoAvisado")) && estado.value.configurado && estado.value.sesion == null) {
                almacen.guardarTexto("tuAnoAvisado", anio.toString())
                cuandoHayaLugar("tu-ano") { avisar("🎁 Tu año $anio con Díptico está listo: ábrelo desde tu perfil o en Ajustes"); Sonidos.aviso() }
            }
            delay(6 * 3_600_000L)
        }
    }
}

/** Con dos pantallas (y sin partida), el editor va abajo y la tarjeta arriba; si no, el editor ocupa la pantalla del perfil. */
fun Diptico.edicionDividida(e: Estado) = !e.unaPantalla && e.sesion == null

/** Abre el editor del perfil con un borrador de lo que tienes ahora; nada se guarda hasta A · Guardar. */
fun Diptico.editarPerfil() {
    val e = estado.value
    val x = e.extra
    val ed = EdicionPerfil(x.perfil, e.nombre, x.discordPresencia, x.dippoSaludo)
    val en = if (x.perfilEn >= 0) x.perfilEn else android.view.Display.DEFAULT_DISPLAY
    actualizar {
        it.copy(
            panel = if (it.panel == Panel.MENU || it.panel == Panel.GALERIA) Panel.NINGUNO else it.panel,
            extra = it.extra.copy(edicionPerfil = ed, perfilEn = en, perfilOx = 0.5f, perfilOy = 0.5f, amigosEn = -1, desplegable = -1),
        )
    }
}

/** B · Cancelar: se descarta el borrador y queda la tarjeta como estaba. */
fun Diptico.cancelarEdicionPerfil() {
    val ed = estado.value.extra.edicionPerfil ?: return
    val e = estado.value
    val hubo = ed.perfil != e.extra.perfil || ed.nombre != e.nombre || ed.presencia != e.extra.discordPresencia || ed.saludo != e.extra.dippoSaludo
    actualizar { it.copy(extra = it.extra.copy(edicionPerfil = null)) }
    if (hubo) avisar("Cambios descartados")
}

/** A · Guardar: el borrador pasa a ser tu perfil (nombre, foto, datos, color y privacidad). */
fun Diptico.guardarEdicionPerfil() {
    val e = estado.value
    val ed = e.extra.edicionPerfil ?: return
    if (ed.nombre != e.nombre) almacen.guardarTexto("nombre", ed.nombre)
    almacen.guardarTexto("perfil", ed.perfil.json())
    if (ed.presencia != e.extra.discordPresencia) {
        almacen.guardarBool("discordPresencia", ed.presencia)
        if (!ed.presencia) SocialDiscord.sinJuego()
    }
    if (ed.saludo != e.extra.dippoSaludo) almacen.guardarBool("dippoSaludo", ed.saludo)
    actualizar {
        it.copy(nombre = ed.nombre, extra = it.extra.copy(perfil = ed.perfil, discordPresencia = ed.presencia, dippoSaludo = ed.saludo, edicionPerfil = null))
    }
    Sonidos.aviso()
    avisar(if (ed.nombre.isNotBlank()) "¡Listo, ${ed.nombre}! Tu perfil quedó guardado" else "Tu perfil quedó guardado")
}

internal fun Diptico.cambiarEdicion(f: (EdicionPerfil) -> EdicionPerfil) =
    actualizar { s -> s.extra.edicionPerfil?.let { s.copy(extra = s.extra.copy(edicionPerfil = f(it))) } ?: s }

/** Con el mando: ↑ ↓ cambian de fila (saltando las que no se ven), ← → dentro de la fila; en «Consola favorita» ← → la cambian. */
fun Diptico.moverEdicionPerfil(df: Int, dc: Int) {
    val e = estado.value
    val ed = e.extra.edicionPerfil ?: return
    val conD = e.extra.discordAvatar.isNotBlank()
    if (df != 0) {
        var f = ed.fila
        do { f += df } while (f in 0 until FilasEdicion.TOTAL && !FilasEdicion.visible(f, SocialDiscord.disponible))
        if (f !in 0 until FilasEdicion.TOTAL) return
        cambiarEdicion { it.copy(fila = f, col = it.col.coerceIn(0, FilasEdicion.ancho(f, conD) - 1)) }
        Vibracion.mover()
        return
    }
    if (ed.fila == FilasEdicion.CONSOLA) { ciclarConsolaFav(dc); return }
    val n = FilasEdicion.ancho(ed.fila, conD)
    val c = (ed.col + dc).coerceIn(0, n - 1)
    if (c != ed.col) { cambiarEdicion { it.copy(col = c) }; Vibracion.mover() }
}

internal fun Diptico.ciclarConsolaFav(d: Int) {
    val ids = listOf("") + estado.value.consolasConJuegos.map { it.id }
    cambiarEdicion { ed ->
        val i = ids.indexOf(ed.perfil.consolaFav).coerceAtLeast(0)
        ed.copy(perfil = ed.perfil.copy(consolaFav = ids[(i + d).mod(ids.size)]))
    }
}

/** Usa lo que está en ([fila], [col]): con A o con un toque. */
fun Diptico.usarEdicionPerfil(fila: Int, col: Int = 0) {
    val e = estado.value
    val ed = e.extra.edicionPerfil ?: return
    cambiarEdicion { it.copy(fila = fila, col = col) }
    val p = ed.perfil
    when (fila) {
        FilasEdicion.FOTO -> when (FilasEdicion.fuentes(e.extra.discordAvatar.isNotBlank()).getOrNull(col)) {
            "personaje" -> cambiarEdicion { it.copy(perfil = p.copy(foto = "", fotoDiscord = false, personaje = if (p.personaje == INICIAL) "gato" else p.personaje)) }
            "discord" -> cambiarEdicion { it.copy(perfil = p.copy(fotoDiscord = true)) }
            "galeria" -> pedirImagen("perfilFoto")
            INICIAL -> cambiarEdicion { it.copy(perfil = p.copy(foto = "", fotoDiscord = false, personaje = INICIAL)) }
        }
        FilasEdicion.PERSONAJES_1, FilasEdicion.PERSONAJES_2 -> PERSONAJES.getOrNull((fila - 1) * 4 + col)?.let { (id, _) ->
            cambiarEdicion { it.copy(perfil = p.copy(personaje = id, foto = "", fotoDiscord = false)) }
        }
        FilasEdicion.NOMBRE -> abrirTexto("nombre", ed.nombre)
        FilasEdicion.APODO -> abrirTexto("perfil_apodo", p.apodo)
        FilasEdicion.FECHA -> abrirTexto("perfil_nacimiento", p.nacimiento.split("-").takeIf { it.size == 3 }?.reversed()?.joinToString("/").orEmpty())
        FilasEdicion.CONSOLA -> ciclarConsolaFav(1)
        FilasEdicion.COLOR -> Perfil.COLORES.getOrNull(col)?.let { (_, c) -> cambiarEdicion { it.copy(perfil = p.copy(color = c)) } }
        FilasEdicion.PRESENCIA -> cambiarEdicion { it.copy(presencia = !it.presencia) }
        FilasEdicion.SALUDO -> cambiarEdicion { it.copy(saludo = !it.saludo) }
        FilasEdicion.HORAS -> cambiarEdicion { it.copy(perfil = p.copy(mostrarHoras = !p.mostrarHoras)) }
        FilasEdicion.BOTONES -> if (col == 0) guardarEdicionPerfil() else cancelarEdicionPerfil()
    }
}

fun Diptico.guardarPerfil(nuevo: Perfil) {
    almacen.guardarTexto("perfil", nuevo.json())
    actualizar { it.copy(extra = it.extra.copy(perfil = nuevo)) }
}

/** Cambia el perfil; si estás en el editor, cambia el borrador (se guarda con A · Guardar). */
fun Diptico.cambiarPerfil(f: (Perfil) -> Perfil) {
    if (estado.value.extra.edicionPerfil != null) cambiarEdicion { it.copy(perfil = f(it.perfil)) }
    else guardarPerfil(f(estado.value.extra.perfil))
}

/** Lo que se muestra en la tarjeta: tus horas, tu juego más jugado, la racha, los logros de DIPPO y tus favoritos. */
fun Diptico.resumenPerfil(e: Estado): ResumenPerfil {
    val jugados = e.datos.filterValues { it.tiempo > 0 }
    val top = jugados.maxByOrNull { it.value.tiempo }
    val porClave = e.juegos.associateBy { it.clave }
    val favoritos = e.datos.filterValues { it.favorito }.keys.mapNotNull { porClave[it] }.take(4)
        .ifEmpty { jugados.entries.sortedByDescending { it.value.tiempo }.mapNotNull { porClave[it.key] }.take(4) }
    val porConsola = jugados.entries.groupBy { porClave[it.key]?.consolaId.orEmpty() }.filterKeys { it.isNotEmpty() }
        .mapValues { (_, v) -> v.sumOf { it.value.tiempo } }
    val idConsola = e.extra.perfil.consolaFav.ifBlank { porConsola.maxByOrNull { it.value }?.key.orEmpty() }
    return ResumenPerfil(
        segundos = jugados.values.sumOf { it.tiempo },
        masJugado = top?.let { porClave[it.key] }, masJugadoSeg = top?.value?.tiempo ?: 0L,
        racha = rachaDias(), logros = logrosDippoGanados(), logrosTotal = LogrosDippo.LISTA.size,
        favoritos = favoritos, juegosJugados = jugados.size,
        consola = if (idConsola.isEmpty()) "" else (e.consolasConJuegos.firstOrNull { it.id == idConsola }?.bonito ?: Consolas.porId(idConsola)?.bonito.orEmpty()),
    )
}

internal fun Diptico.opcionesPerfil(e: Estado): List<Opcion> {
    val pf = e.extra.perfil
    val consolas = e.consolasConJuegos
    val fechaEscrita = pf.nacimiento.split("-").takeIf { it.size == 3 }?.reversed()?.joinToString("/").orEmpty()
    val lista = mutableListOf(
        Opcion("Editar mi perfil", "Foto, personaje, datos y color", Icono.LAPIZ,
            "Abre el editor: con dos pantallas, abajo cambias tus datos y arriba ves tu tarjeta tal como va quedando. Se guarda con A.", grupo = "Tu perfil") {
            editarPerfil()
        },
        Opcion("Ver mi perfil", "Abrir mi tarjeta", Icono.PERFIL, "Tu tarjeta con tus horas, tu racha, tus logros y tus juegos favoritos.", grupo = "Tu perfil") {
            cerrarPanel(); abrirPerfil(android.view.Display.DEFAULT_DISPLAY, 0.5f, 0.14f)
        },
        Opcion("Tu año", "Ver mi resumen del año", Icono.ESTRELLA, "Tus horas, tus juegos más jugados, tu mejor mes y tu racha del año, con un comentario de DIPPO. En diciembre te aviso solo.", grupo = "Tu perfil") {
            cerrarPanel(); abrirTuAno(android.view.Display.DEFAULT_DISPLAY)
        },
        Opcion("Tu nombre", e.nombre.ifBlank { "Sin nombre" }, Icono.TEXTO, "Así te saluda DIPPO y así se ve en tu perfil.", grupo = "Tus datos") { abrirTexto("nombre", estado.value.nombre) },
        Opcion("Apodo", if (pf.apodo.isBlank()) "Sin apodo" else "@${pf.apodo}", Icono.TEXTO, "Cómo te dicen tus amigos. Se ve como @apodo.", grupo = "Tus datos") { abrirTexto("perfil_apodo", estado.value.extra.perfil.apodo) },
        Opcion("Fecha de nacimiento", pf.nacimientoBonito().ifBlank { "Sin fecha" }, Icono.RELOJ,
            "Escríbela así: 12/03/1990. DIPPO te felicita en tu cumpleaños, y más adelante servirá para las llamadas con amigos.", grupo = "Tus datos") { abrirTexto("perfil_nacimiento", fechaEscrita) },
        Opcion("Mi foto", if (pf.foto.isBlank()) "Usar un personaje" else "Cambiar mi foto", Icono.PORTADA, "Elige una imagen de tu consola. Si no, se usa el personaje.", grupo = "Tu imagen") { pedirImagen("perfilFoto") },
    )
    if (e.extra.discordAvatar.isNotBlank()) lista += Opcion("Usar la foto de Discord", if (pf.fotoDiscord) "Sí" else "No", Icono.MUNDO,
        "Tu foto de perfil de Discord se ve en vez de tu foto o personaje, en la barra y en tu tarjeta.", grupo = "Tu imagen") { cambiarPerfil { it.copy(fotoDiscord = !it.fotoDiscord) } }
    if (pf.foto.isNotBlank()) lista += Opcion("Quitar mi foto", "Volver al personaje", Icono.BORRAR, "Tu foto se quita de Díptico (la imagen original no se toca).", grupo = "Tu imagen") { cambiarPerfil { it.copy(foto = "") }; avisar("Foto quitada") }
    lista += Opcion("Mi personaje", nombrePersonaje(pf.personaje), Icono.BISA, "Ocho amigos de Díptico para elegir. Se ve cuando no tienes foto.", grupo = "Tu imagen") { abrirGaleria("personajes") }
    lista += Opcion("Marco de la foto", "${marcoDe(pf.marco).nombre} · ${marcosDesbloqueados()} de ${MARCOS.size}", Icono.ESTRELLA,
        "Un aro de luz que se mueve alrededor de tu foto. Los marcos nuevos se desbloquean con logros de DIPPO.", grupo = "Tu imagen") { abrirGaleria("marcos") }
    lista += Opcion("Título", tituloDe(pf.titulo).ifBlank { "Sin título" }, Icono.TROFEO,
        "Una frase bajo tu nombre en tu tarjeta. Se desbloquea con logros difíciles de DIPPO.", grupo = "Tu imagen") { abrirGaleria("titulos") }
    lista += opcionLista("Mi color", Icono.PALETA, "El color del borde de tu foto y de los detalles de tu tarjeta.",
        Perfil.COLORES.map { it.second.toString() }, Perfil.COLORES.map { it.first }, pf.color.toString(), grupo = "Tu imagen") { id -> cambiarPerfil { it.copy(color = id.toLong()) } }
    lista += opcionLista("Consola favorita", Icono.MANDO, "La que se muestra en tu tarjeta. «La más jugada» la elige sola según tus horas.",
        listOf("") + consolas.map { it.id }, listOf("La más jugada") + consolas.map { it.bonito }, pf.consolaFav, grupo = "Tu tarjeta") { id -> cambiarPerfil { it.copy(consolaFav = id) } }
    lista += Opcion("Mostrar mis horas", if (pf.mostrarHoras) "Sí" else "No", Icono.RELOJ, "Si lo apagas, tu tarjeta no muestra cuántas horas has jugado.", grupo = "Tu tarjeta") { cambiarPerfil { it.copy(mostrarHoras = !it.mostrarHoras) } }
    return lista
}

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
 * Widgets con vida: funciones de Diptico movidas aquí desde Diptico.kt para que ese archivo no siga creciendo.
 * Se llaman igual que antes (Diptico.x()); fuera del paquete estado hay que importar cl.nikalbazu.diptico.estado.*.
 */


/** Widgets visibles de la página actual del inicio (índices de la lista). */
internal fun Diptico.widgetsDePagina(e: Estado = estado.value) = e.widgets.indices.filter { e.widgets[it].visible && e.widgets[it].pagina == e.paginaInicio }


/** Entra a los widgets con la cruceta (desde el estante, con ↑): el foco va al de más abajo a la izquierda. */
fun Diptico.entrarWidgets(): Boolean {
    val e = estado.value
    if (e.inicio != "widgets" || e.editandoWidgets || e.eligiendoConsola) return false
    val l = widgetsDePagina(e)
    if (l.isEmpty()) return false
    val i = l.maxWithOrNull(compareBy<Int>({ e.widgets[it].y + e.widgets[it].al }, { -e.widgets[it].x })) ?: return false
    actualizar { it.copy(widgetFoco = i) }
    Sonidos.levantar()
    return true
}


fun Diptico.salirWidgets() {
    if (estado.value.widgetFoco < 0) return
    actualizar { it.copy(widgetFoco = -1) }
    Sonidos.soltar()
}


/** Mueve el foco al widget más cercano en esa dirección. Abajo del todo vuelve al estante; a los lados cambia de página. */
fun Diptico.moverFocoWidget(dx: Int, dy: Int) {
    val e = estado.value
    val w = e.widgets.getOrNull(e.widgetFoco) ?: return
    val siguiente = vecinoWidget(e, e.widgetFoco, dx, dy)
    when {
        siguiente != null -> actualizar { it.copy(widgetFoco = siguiente) }
        dy > 0 -> salirWidgets()
        dx != 0 && e.paginas > 1 && e.paginaInicio + dx in 0 until e.paginas -> {
            irAPagina(e.paginaInicio + dx)
            val l = widgetsDePagina()
            val f = (if (dx > 0) l.minByOrNull { estado.value.widgets[it].x * 10 + kotlin.math.abs(estado.value.widgets[it].y - w.y) }
            else l.maxByOrNull { estado.value.widgets[it].x * 10 - kotlin.math.abs(estado.value.widgets[it].y - w.y) })
            actualizar { it.copy(widgetFoco = f ?: -1) }
            Sonidos.pagina()
        }
        else -> Sonidos.borde()
    }
}


/** Con el foco en los widgets, L2/R2 cambian de página y el foco pasa al primero de la nueva. */
fun Diptico.paginaFocoWidget(d: Int) {
    val e = estado.value
    val n = e.paginaInicio + d
    if (n !in 0 until e.paginas) { Sonidos.borde(); return }
    irAPagina(n)
    val l = widgetsDePagina()
    actualizar { it.copy(widgetFoco = l.minByOrNull { k -> it.widgets[k].y * 100 + it.widgets[k].x } ?: -1) }
    Sonidos.pagina()
}


/** Abre el widget: unos van directo a su lugar (ficha, estadísticas, DIPPO…) y el resto muestra su propio contenido. */
fun Diptico.abrirWidget(i: Int = estado.value.widgetFoco) {
    val e = estado.value
    val w = e.widgets.getOrNull(i) ?: return
    when (w.tipo) {
        "juego" -> { abrirFicha(); return }
        "paso" -> { abrirQuedaste(); return }
        "accesos" -> { abrirCentro(); return }
        "apps" -> { abrirApps(); return }
        "buscar" -> { abrirBuscar(); return }
    }
    // Acceso directo: juega al tiro.
    if (w.tipo.startsWith("atajo-")) {
        val clave = w.tipo.removePrefix("atajo-")
        if (e.juegos.none { it.clave == clave }) { avisar("Ese juego ya no está en tu biblioteca"); return }
        elegirConsola("todo"); elegirJuego(clave); jugar(); return
    }
    if (w.tipo == "sugerencia" && e.sugeridas.isEmpty()) nuevasSugerencias()
    actualizar { it.copy(panel = Panel.WIDGET, widgetAbierto = i, widgetFoco = i, indice = 0) }
}


/** ← → dentro de un widget abierto: cambia las opciones con valor (minutos, pausas…). */
fun Diptico.ladoWidget(d: Int) {
    val o = opcionesWidget().getOrNull(estado.value.indice) ?: return
    if (o.menos == null) return
    if (d < 0) o.menos.invoke() else o.accion()
}


/** Tres juegos para descubrir (primero los que nunca has jugado). */
fun Diptico.nuevasSugerencias() {
    val e = estado.value
    val sinJugar = e.juegos.filter { (e.datos[it.clave]?.tiempo ?: 0L) == 0L }
    val l = (sinJugar.shuffled() + e.juegos.shuffled()).distinctBy { it.clave }.filter { it.clave !in e.sugeridas }.take(3)
        .ifEmpty { e.juegos.shuffled().take(3) }
    actualizar { it.copy(sugeridas = l.map { j -> j.clave }) }
}


fun Diptico.jugarDesdeWidget(clave: String) {
    cerrarPanel()
    actualizar { it.copy(widgetFoco = -1) }
    elegirConsola("todo")
    elegirJuego(clave)
    jugar()
}


internal fun Diptico.cambiarTemporizador(d: Int) {
    val l = MINUTOS_TEMPORIZADOR
    val i = l.indexOfFirst { it >= estado.value.temporizadorMin }.coerceAtLeast(0)
    val v = l[(i + d).coerceIn(0, l.size - 1)]
    almacen.guardarTexto("temporizadorMin", v.toString())
    actualizar { it.copy(temporizadorMin = v) }
}


fun Diptico.alternarTemporizador() {
    val e = estado.value
    if (e.temporizadorFin > 0L) {
        actualizar { it.copy(temporizadorFin = 0L) }
        Sonidos.interruptor(false)
        return
    }
    val fin = System.currentTimeMillis() + e.temporizadorMin * 60_000L
    actualizar { it.copy(temporizadorFin = fin) }
    Sonidos.interruptor(true)
    avisar("Temporizador de ${e.temporizadorMin} min. Te aviso aunque estés jugando.")
    scope.launch {
        while (estado.value.temporizadorFin == fin && System.currentTimeMillis() < fin) delay(1000)
        if (estado.value.temporizadorFin != fin) return@launch
        actualizar { it.copy(temporizadorFin = 0L) }
        Sonidos.aviso()
        val s = estado.value.sesion
        if (s != null) cl.nikalbazu.diptico.bisa.Cartel.mostrar(app, s.displayId, "Temporizador", "¡Se acabó el tiempo!", "${e.temporizadorMin} min")
        else avisar("¡Se acabó el tiempo! (${e.temporizadorMin} min)")
    }
}


fun Diptico.alternarCrono() {
    val e = estado.value
    val ahora = System.currentTimeMillis()
    if (e.cronoInicio > 0L) actualizar { it.copy(cronoInicio = 0L, cronoAcumulado = it.cronoAcumulado + (ahora - it.cronoInicio)) }
    else actualizar { it.copy(cronoInicio = ahora) }
    Sonidos.interruptor(e.cronoInicio == 0L)
}


fun Diptico.reiniciarCrono() = actualizar { it.copy(cronoInicio = 0L, cronoAcumulado = 0L) }


internal fun Diptico.cambiarPausa(d: Int) {
    val l = PAUSAS
    val v = l[(l.indexOf(estado.value.pausaCada).coerceAtLeast(0) + d).mod(l.size)]
    almacen.guardarTexto("pausaCada", v.toString())
    actualizar { it.copy(pausaCada = v) }
}


/** Mientras juegas: cada [Estado.pausaCada] minutos un cartel amable para descansar la vista y estirarse. */
internal fun Diptico.vigilarPausas() {
    scope.launch {
        var avisadas = 0L
        var sesion = 0L
        while (true) {
            delay(30_000)
            val e = estado.value
            val s = e.sesion
            if (s == null || e.pausaCada <= 0) { avisadas = 0; continue }
            if (s.inicio != sesion) { sesion = s.inicio; avisadas = 0 }
            val minutos = (System.currentTimeMillis() - s.inicio) / 60_000
            val toca = minutos / e.pausaCada
            if (toca > avisadas) {
                avisadas = toca
                cl.nikalbazu.diptico.bisa.Cartel.mostrar(
                    app, s.displayId, "DIPPO", "Llevas $minutos min jugando",
                    listOf("¿Una pausa? Mira a lo lejos unos segundos.", "Estira las manos y toma agua.", "Buen momento para guardar la partida.").random(),
                )
            }
        }
    }
}


fun Diptico.elegirVistaPartida(clave: String, vista: VistaPartida) {
    val datos = estado.value.datos.toMutableMap()
    datos[clave] = (datos[clave] ?: DatosJuego()).copy(vistaPartida = vista.name)
    almacen.guardarDatos(datos)
    actualizar { it.copy(datos = datos) }
    if (estado.value.sesion != null) despertar()
}


fun Diptico.cambiarAvance(clave: String) {
    cambiarDatos(clave) { d ->
        val actual = AvanceJuego.leer(d.avance)
        val siguiente = AvanceJuego.entries[(actual.ordinal + 1) % AvanceJuego.entries.size]
        d.copy(avance = siguiente.name, puestoCola = d.puestoCola ?: System.currentTimeMillis())
    }
}


fun Diptico.priorizarJuego(clave: String) {
    val primero = estado.value.datos.values.mapNotNull { it.puestoCola }.minOrNull() ?: 0L
    cambiarDatos(clave) { it.copy(puestoCola = (primero.coerceAtLeast(Long.MIN_VALUE + 1)) - 1) }
    avisar("Juego al comienzo de la cola")
}


internal fun Diptico.opcionesTemporizador(e: Estado) = listOf(
    Opcion("Temporizador", "${e.temporizadorMin} min", Icono.RELOJ, "Elige los minutos con ← →. Te aviso con un cartel aunque estés jugando.", menos = { cambiarTemporizador(-1) }) { cambiarTemporizador(1) },
    Opcion(if (e.temporizadorFin > 0L) "Detener el temporizador" else "Empezar el temporizador", icono = if (e.temporizadorFin > 0L) Icono.CERRAR else Icono.JUGAR, ayuda = "Un temporizador para avisarte cuando sea hora de dejar de jugar.") { alternarTemporizador() },
)


internal fun Diptico.opcionesCronometro(e: Estado) = listOf(
    Opcion(if (e.cronoInicio > 0L) "Pausar el cronómetro" else if (e.cronoAcumulado > 0L) "Seguir el cronómetro" else "Empezar el cronómetro", icono = Icono.RELOJ, ayuda = "Para medir una partida, un speedrun o lo que quieras.") { alternarCrono() },
    Opcion("Reiniciar el cronómetro", icono = Icono.BORRAR, apagada = e.cronoInicio == 0L && e.cronoAcumulado == 0L, ayuda = "Vuelve el cronómetro a cero.") { reiniciarCrono() },
)


/** Lo que se puede hacer dentro de cada widget abierto (la lista del panel). */
fun Diptico.opcionesWidget(e: Estado = estado.value): List<Opcion> {
    val w = e.widgets.getOrNull(e.widgetAbierto) ?: return emptyList()
    fun juegosComo(l: List<cl.nikalbazu.diptico.data.Juego>) = l.map { j ->
        val d = e.datos[j.clave]
        Opcion(j.titulo, (Consolas.porId(j.consolaId)?.nombre ?: "") + (d?.tiempo?.takeIf { it >= 60 }?.let { " · ${formatoTiempoCorto(it)}" } ?: ""), Icono.JUGAR, d?.paso.orEmpty()) { jugarDesdeWidget(j.clave) }
    }
    return when (w.tipo) {
        "proximaPartida" -> ultimoJugado(e)?.let { j ->
            juegosComo(listOf(j)) + Opcion("Ver o marcar el mapa", icono = Icono.MUNDO) { abrirMapa(j.clave) }
        } ?: listOf(Opcion("Tu próxima partida aparecerá aquí", icono = Icono.INFO, apagada = true) {})
        "colaJuegos" -> juegosComo(PlanPartida.cola(e)).ifEmpty {
            listOf(Opcion("Agrega juegos desde Opciones del juego", icono = Icono.INFO, apagada = true) {})
        }
        "tengo20" -> juegosComo(PlanPartida.breves(e)).ifEmpty {
            listOf(Opcion("Las sugerencias se basan en tus sesiones anteriores", icono = Icono.INFO, apagada = true) {})
        }
        "temporizador" -> opcionesTemporizador(e)
        "cronometro" -> opcionesCronometro(e)
        "reloj" -> opcionesTemporizador(e) + opcionesCronometro(e) + listOf(
            Opcion("Recordarme una pausa al jugar", if (e.pausaCada == 0) "No" else "Cada ${e.pausaCada} min", Icono.CORAZON, "Un cartel amable mientras juegas para descansar la vista y estirarte.", menos = { cambiarPausa(-1) }) { cambiarPausa(1) },
            Opcion("Reloj a pantalla completa", icono = Icono.RELOJ, ayuda = "Muestra la hora en grande en la pantalla de arriba.") { cerrarPanel(); alternarReloj() },
        )
        "musica" -> buildList {
            val m = cl.nikalbazu.diptico.audio.Medios.sonando.value
            if (!cl.nikalbazu.diptico.audio.Medios.tienePermiso(app)) add(Opcion("Dar permiso para ver la música", icono = Icono.CLAVE, ayuda = "Android pide permiso para que Díptico vea y controle lo que suena.") { cl.nikalbazu.diptico.audio.Medios.pedirPermiso(app) })
            if (m != null) {
                add(Opcion(if (m.reproduciendo) "Pausar" else "Reproducir", m.titulo, Icono.JUGAR) { cl.nikalbazu.diptico.audio.Medios.alternar() })
                add(Opcion("Siguiente canción", icono = Icono.CONTINUAR) { cl.nikalbazu.diptico.audio.Medios.siguiente() })
                add(Opcion("Canción anterior", icono = Icono.CONTINUAR) { cl.nikalbazu.diptico.audio.Medios.anterior() })
                add(Opcion("Abrir ${m.app}", icono = Icono.APPS) { cerrarPanel(); abrirPaquete(m.paquete) })
            }
            appsMusica().filter { it.paquete != m?.paquete }.forEach { a -> add(Opcion("Abrir ${a.nombre}", icono = Icono.MUSICA, paquete = a.paquete) { cerrarPanel(); abrirPaquete(a.paquete) }) }
            add(Opcion("Música de Díptico", if (e.musica) "Sí" else "No", Icono.MUSICA, "La música propia de cada tema.") { fijarMusica(!estado.value.musica) })
            add(Opcion("Elegir canción de Díptico", icono = Icono.MUSICA, ayuda = "Elige qué canción de fondo suena en Díptico.") { abrirGaleria("musica") })
        }
        "recientes" -> juegosComo(e.datos.entries.filter { it.value.ultimo > 0 }.sortedByDescending { it.value.ultimo }.mapNotNull { (k, _) -> e.juegosPorClave[k] }.take(12))
            .ifEmpty { listOf(Opcion("Todavía no juegas nada con Díptico", icono = Icono.INFO, apagada = true) {}) }
        "favoritos" -> juegosComo(e.juegos.filter { e.datos[it.clave]?.favorito == true })
            .ifEmpty { listOf(Opcion("Marca favoritos con Y sobre un juego", icono = Icono.CORAZON, apagada = true) {}) }
        "sugerencia" -> juegosComo(e.sugeridas.mapNotNull { k -> e.juegosPorClave[k] }) + listOf(
            Opcion("Otras sugerencias", icono = Icono.DADO, ayuda = "Te muestra otros juegos de tu biblioteca para jugar ahora.") { nuevasSugerencias() },
            Opcion("Pídele una recomendación a DIPPO", icono = Icono.BISA, ayuda = "DIPPO elige un juego para ti según lo que te gusta.") { abrirAsistente(); preguntar("¿Qué juego de mi colección me recomiendas para hoy? Dame 3 opciones y por qué.") },
        )
        "fiesta" -> buildList {
            e.fiesta?.let { f -> add(Opcion("Hoy: ${f.nombre}", f.donde, Icono.MUNDO, f.tema.mensaje) {}) }
            Temas.proximas(6).forEach { (f, d) -> add(Opcion(f.nombre, "${d.dayOfMonth}/${d.monthValue}", Icono.MUNDO, f.donde) { avisar("${f.nombre}: ${f.donde}") }) }
            add(Opcion("Temas de fiestas", if (e.temaFestivo) "Sí" else "No", Icono.TEMA, "Díptico se viste solo para cada celebración.") {
                val v = !estado.value.temaFestivo
                almacen.guardarBool("temaFestivo2", v)
                actualizar { it.copy(temaFestivo = v) }
                aplicarTema()
            })
        }
        "bateria" -> {
            val b = infoBateria()
            listOf(
                Opcion("Carga", if (b.nivel >= 0) "${b.nivel}%" else "—", Icono.BATERIA) {},
                Opcion("Estado", b.estado, Icono.BATERIA) {},
                Opcion("Temperatura", if (b.temperatura > 0f) "${"%.1f".format(b.temperatura)} °C" else "—", Icono.INFO, if (b.temperatura >= 42f) "Está tibia: una pausa le viene bien." else "") {},
                Opcion("Voltaje", if (b.voltaje > 0) "${"%.2f".format(b.voltaje / 1000f)} V" else "—", Icono.INFO) {},
                Opcion("Salud", b.salud, Icono.CORAZON) {},
                Opcion("Modo de efectos", nombreFluidez(e.fluidez), Icono.CHISPA, "En ahorro Díptico apaga difuminados y decoraciones para gastar menos.") {
                    val l = listOf("auto", "alta", "equilibrada", "ahorro")
                    val v = l[(l.indexOf(estado.value.fluidez) + 1).mod(l.size)]
                    almacen.guardarTexto("fluidez", v)
                    actualizar { it.copy(fluidez = v) }
                    aplicarFluidez()
                },
                Opcion("Ajustes de batería de Android", icono = Icono.AJUSTES, ayuda = "Abre los ajustes de batería de Android.") { cerrarPanel(); abrirAjusteAndroid(android.content.Intent.ACTION_POWER_USAGE_SUMMARY) },
            )
        }
        "tiempo", "estadisticas" -> {
            val top = e.datos.entries.filter { it.value.tiempo > 0 }.sortedByDescending { it.value.tiempo }.mapNotNull { (k, _) -> e.juegosPorClave[k] }.take(8)
            listOf(Opcion("Ver todas las estadísticas", icono = Icono.TROFEO, ayuda = "Horas por juego y por consola, partidas y rachas.") { abrirEstadisticas() }) + juegosComo(top)
        }
        "logros" -> if (e.usuarioRA.isBlank()) listOf(Opcion("Conectar RetroAchievements", icono = Icono.CLAVE, ayuda = "Tu usuario y tu clave web de retroachievements.org.") { abrirMenu(Seccion.LOGROS) })
        else listOf(
            Opcion("Ver mis logros", icono = Icono.TROFEO, ayuda = "Tus puntos, los últimos logros y los de cada juego.") { abrirEstadisticas() },
            Opcion("Ajustes de logros", icono = Icono.AJUSTES, ayuda = "Tu cuenta de RetroAchievements y cómo se ven tus logros.") { abrirMenu(Seccion.LOGROS) },
        )
        "consolas" -> e.consolasConJuegos.map { c ->
            Opcion(c.bonito, "${e.juegosPorConsola[c.id].orEmpty().size} juegos", Icono.BIBLIOTECA, ayuda = "Va a esta consola en el estante.") { cerrarPanel(); actualizar { it.copy(widgetFoco = -1) }; elegirConsola(c.id) }
        }
        "frase" -> listOf(
            Opcion("Otra frase", icono = Icono.CHISPA, ayuda = "Muestra otra frase en el widget.") { actualizar { it.copy(fraseExtra = it.fraseExtra + 1) } },
            Opcion("Pídele una a DIPPO", icono = Icono.BISA, ayuda = "DIPPO escribe una frase nueva para el widget.") { abrirAsistente(); preguntar("Dame una frase corta y simpática para empezar a jugar hoy.") },
        )
        "notificaciones" -> Avisos.lista.value.map { a -> Opcion(a.titulo.ifBlank { a.app }, a.app, Icono.CAMPANA, a.texto, paquete = a.paquete) { abrirAviso(a) } }
            .ifEmpty { listOf(Opcion("Todo tranquilo: no hay notificaciones", icono = Icono.CHECK, apagada = true) {}) }
        "asistente" -> buildList {
            val j = e.seleccionado
            add(Opcion("Conversar con DIPPO", icono = Icono.BISA, ayuda = "Pregúntale lo que quieras: trucos, qué jugar, o cambiar Díptico.") { abrirAsistente() })
            if (j != null) {
                add(Opcion("Un truco para ${j.titulo}", icono = Icono.CHISPA, ayuda = "Le pregunta a DIPPO por un truco para este juego.") { abrirAsistente(); preguntar("Dame un buen truco o consejo para ${j.titulo}.") })
                add(Opcion("¿De qué trata ${j.titulo}?", icono = Icono.TUTORIAL, ayuda = "DIPPO te cuenta de qué trata este juego, sin spoilers.") { abrirAsistente(); preguntar("Cuéntame en pocas líneas de qué trata ${j.titulo} y por qué vale la pena.") })
                e.datos[j.clave]?.paso?.takeIf { it.isNotBlank() }?.let { paso -> add(Opcion("Ayúdame a seguir donde quedé", paso.take(30), Icono.LAPIZ, ayuda = "DIPPO te ayuda con lo que anotaste como tu próximo paso.") { abrirAsistente(); preguntar("En ${j.titulo} anoté esto: \"$paso\". ¿Qué me conviene hacer ahora?") }) }
            }
            add(Opcion("¿Qué juego hoy?", icono = Icono.DADO, ayuda = "DIPPO te recomienda qué jugar hoy.") { abrirAsistente(); preguntar("¿Qué juego de mi colección me recomiendas para hoy? Dame 3 opciones y por qué.") })
            add(Opcion("Cámbiame el tema", icono = Icono.TEMA, ayuda = "DIPPO te cambia el tema de Díptico.") { abrirAsistente(); preguntar("Cámbiame el tema de Díptico por uno que me sorprenda.") })
            add(Opcion("Ayuda mientras juego", icono = Icono.BISA, ayuda = "DIPPO mira tu partida y te ayuda (con el botón de DIPPO en el juego).") { prepararAyuda() })
        }
        else -> if (w.tipo.startsWith("coleccion-")) {
            val id = w.tipo.removePrefix("coleccion-")
            val c = e.colecciones[id] ?: return emptyList()
            val j = e.seleccionado
            juegosComo(c.juegos.mapNotNull { k -> e.juegosPorClave[k] }) + listOfNotNull(
                j?.let { Opcion(if (it.clave in c.juegos) "Quitar ${it.titulo}" else "Agregar ${it.titulo}", icono = Icono.CARPETA, ayuda = "El juego elegido en el estante.") { alternarEnColeccion(id, it.clave) } },
                Opcion("Borrar esta colección", icono = Icono.BORRAR, ayuda = "Los juegos no se borran, solo la carpeta.") { borrarColeccion(id) },
            )
        } else if (w.tipo.startsWith("imagen")) listOf(
            Opcion("Cambiar la imagen o GIF", icono = Icono.PORTADA, ayuda = "Elige una imagen o GIF de tus archivos para este widget.") { pedirArchivoPara("widget:${w.tipo}", "image/*") },
            Opcion("Acomodar en el estudio", icono = Icono.APPS, ayuda = "Abre el estudio para mover, agrandar o cambiar el estilo de este widget.") { cerrarPanel(); editarWidgets(); elegirWidget(e.widgetAbierto) },
        ) else emptyList()
    }
}


fun Diptico.infoBateria(): InfoBateria {
    val i = app.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
    val nivel = i?.let { val l = it.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1); val s = it.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100); if (l >= 0) l * 100 / s.coerceAtLeast(1) else -1 } ?: -1
    val est = when (i?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)) {
        android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "Cargando"
        android.os.BatteryManager.BATTERY_STATUS_FULL -> "Llena"
        android.os.BatteryManager.BATTERY_STATUS_DISCHARGING, android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Usando la batería"
        else -> "—"
    }
    val salud = when (i?.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, -1)) {
        android.os.BatteryManager.BATTERY_HEALTH_GOOD -> "Buena"
        android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Muy caliente"
        android.os.BatteryManager.BATTERY_HEALTH_COLD -> "Muy fría"
        android.os.BatteryManager.BATTERY_HEALTH_DEAD -> "Gastada"
        else -> "—"
    }
    return InfoBateria(nivel, est, (i?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f, i?.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0) ?: 0, salud)
}

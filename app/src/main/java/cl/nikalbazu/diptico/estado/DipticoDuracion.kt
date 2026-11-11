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
 * Cuánto dura cada juego: funciones de Diptico movidas aquí desde Diptico.kt para que ese archivo no siga creciendo.
 * Se llaman igual que antes (Diptico.x()); fuera del paquete estado hay que importar cl.nikalbazu.diptico.estado.*.
 */

/** Busca cuánto dura el juego si todavía no se sabe (o si la última búsqueda falló hace más de una semana). */
fun Diptico.pedirDuracion(clave: String, alTerminar: ((CuantoDura.Duracion?) -> Unit)? = null) {
    val e = estado.value
    val j = e.juegosPorClave[clave] ?: return
    val ya = e.extra.duraciones[clave]
    if (ya != null && (ya.tiene || System.currentTimeMillis() - ya.buscado < 7 * 86_400_000L)) { alTerminar?.invoke(ya.takeIf { it.tiene }); return }
    if (!buscandoDuracion.add(clave)) return
    scope.launch {
        val d = withContext(Dispatchers.IO) { runCatching { CuantoDura.buscar(j.titulo) }.getOrNull() }
            ?: CuantoDura.Duracion(0f, 0f, 0f, System.currentTimeMillis())
        buscandoDuracion.remove(clave)
        actualizar { it.copy(extra = it.extra.copy(duraciones = it.extra.duraciones + (clave to d))) }
        almacen.guardarTexto("duraciones", CuantoDura.escribir(estado.value.extra.duraciones))
        alTerminar?.invoke(d.takeIf { it.tiene })
    }
}


/**
 * Busca el reverso y el lomo reales de la caja (solo con ScreenScraper o TheGamesDB conectados). Se pregunta una
 * vez por juego; si no aparece, se vuelve a intentar a la semana. Sin fuentes, la caja usa el reverso armado con la ficha.
 */
fun Diptico.pedirCaras(clave: String) {
    val e = estado.value
    val c = e.raspadores
    if (!c.screenscraper && !c.conTgdb) return
    val j = e.juegosPorClave[clave] ?: return
    val ya = e.extra.caras[clave]
    if (ya != null && ((ya.reverso != null && ya.frente != null) || System.currentTimeMillis() - ya.buscado < 2 * 86_400_000L)) return
    if (!buscandoCaras.add(clave)) return
    scope.launch {
        val r = withContext(Dispatchers.IO) { runCatching { cl.nikalbazu.diptico.data.Raspadores.caras(c, j.titulo, j.archivo, j.consolaId) }.getOrNull() }
        buscandoCaras.remove(clave)
        val g = CarasGuardadas(r?.reverso, r?.lomo, System.currentTimeMillis(), r?.frente)
        actualizar { it.copy(extra = it.extra.copy(caras = it.extra.caras + (clave to g))) }
        almacen.guardarTexto("carasCaja2", CarasGuardadas.escribir(estado.value.extra.caras))
    }
}


/** DIPPO responde cuánto dura el juego y cuánto te queda, sin IA. */
internal fun Diptico.responderDuracion(clave: String) {
    val j = estado.value.juegosPorClave[clave] ?: return
    actualizar { it.copy(pensando = true, pensandoQue = "Mirando HowLongToBeat…") }
    pedirDuracion(clave) { d ->
        actualizar { it.copy(pensando = false, pensandoQue = "") }
        val jugado = estado.value.datos[clave]?.tiempo?.toLong() ?: 0L
        val texto = if (d == null) "No encontré cuánto dura ${j.titulo} en HowLongToBeat. Puede que esté con otro nombre allá."
        else buildString {
            append("Según HowLongToBeat, ${j.titulo} dura")
            if (d.principal > 0f) append(" ~${CuantoDura.horasBonitas(d.principal)} la historia")
            if (d.extras > 0f) append(", ~${CuantoDura.horasBonitas(d.extras)} con extras")
            if (d.completo > 0f) append(" y ~${CuantoDura.horasBonitas(d.completo)} al 100 %")
            append(". ")
            if (jugado > 0) append("Llevas ${formatoTiempoCorto(jugado)}. ")
            CuantoDura.teQueda(d, jugado)?.let { append(it).append(".") }
        }
        respuestaBisa(MensajeChat("ia", texto, if (d == null && motorEfectivo() != "ninguno")
            listOf(AccionChat("preguntar", "¿Cuánto dura más o menos ${j.titulo}?", "Pregúntale a la IA")) else emptyList()))
    }
}


/**
 * Abre a DIPPO. Si es la primera vez o pasó un rato, él habla primero: te cuenta qué notó (lo último que
 * jugaste, tu nota, un juego olvidado) y te deja botones listos, así no tienes que escribir nada.
 */
fun Diptico.abrirAsistente() {
    val e = estado.value
    val ahora = System.currentTimeMillis()
    val fresco = e.chat.isEmpty() || ahora - ultimaCharla > 20 * 60_000L
    actualizar { it.copy(panel = Panel.ASISTENTE, indice = 0) }
    if (fresco && !e.pensando) respuestaBisa(CentroBisa.saludoInicial(estado.value))
    if (contadores().n("abrir") == 0) sumarDippo("abrir")
    ultimaCharla = ahora
}


internal fun Diptico.leerSiHablo(texto: String) {
    if (porVoz && estado.value.extra.dippoVoz && !texto.startsWith("⚠")) cl.nikalbazu.diptico.audio.VozDippo.decir(app, texto, estado.value.idioma)
    porVoz = false
}


fun Diptico.escribirAlAsistente() {
    abrirTexto("asistente")
}


fun Diptico.borrarChat() {
    actualizar { it.copy(chat = emptyList(), indice = 0) }
}


/** Lo que se puede elegir con la cruceta en el asistente: escribir, los botones de la última respuesta o las sugerencias. */
fun Diptico.elementosAsistente(e: Estado = estado.value): List<Opcion> = buildList {
    add(Opcion("Hablar", "voz", Icono.MICROFONO) { hablarConBisa() })
    add(Opcion("Escribir", "texto", Icono.TEXTO) { escribirAlAsistente() })
    val ultima = e.chat.lastOrNull()
    val acciones = if (ultima?.rol == "ia") ultima.acciones else emptyList()
    // Primero lo que propuso DIPPO, luego preguntas listas para tocar según lo que tienes delante, y al final los atajos.
    val conIA = motorEfectivo(e) != "ninguno"
    (acciones + CentroBisa.preguntasListas(e, conIA) + CentroBisa.acciones(e)).distinctBy { it.tipo to it.valor }
        .filterNot { a -> e.chat.any { it.rol == "yo" && it.texto == a.valor } }.forEach { a ->
        add(Opcion(a.etiqueta, a.tipo, Icono.CHISPA) { ejecutarAccion(a) })
    }
    if (!e.pensando && e.chat.any { it.rol == "yo" } && motorEfectivo(e) != "ninguno") {
        add(Opcion("Más detalles", "sugerencia", Icono.INFO) { preguntar("Dame más detalles, paso a paso.") })
        e.chat.lastOrNull { it.rol == "yo" }?.texto?.let { pregunta ->
            add(Opcion("Búscalo en internet", "sugerencia", Icono.WEB) { preguntar(pregunta, forzarWeb = true) })
        }
    }
}


fun Diptico.hablarConBisa() {
    if (estado.value.panel != Panel.ASISTENTE) abrirAsistente()
    dictar("bisa")
}


internal fun Diptico.respuestaBisa(mensaje: MensajeChat, pregunta: String? = null) {
    leerSiHablo(mensaje.texto)
    actualizar {
        val nuevo = if (pregunta == null) listOf(mensaje) else listOf(MensajeChat("yo", pregunta), mensaje)
        it.copy(chat = it.chat.takeLast(38) + nuevo, indice = if (mensaje.acciones.isEmpty()) 0 else 2)
    }
}


/** La clave guardada para una IA en la nube. */
fun Diptico.claveDe(p: String, e: Estado = estado.value) = when (p) { "gemini" -> e.claveGemini; "openai" -> e.claveOpenAI; else -> "" }


/** Qué IA usa DIPPO ahora: local (en la consola), gemini, openai o "ninguno". */
fun Diptico.motorEfectivo(e: Estado = estado.value): String = when (e.motorIA) {
    "local", "gemini", "openai" -> e.motorIA
    else -> when {
        IALocal.listo(app) -> "local"
        else -> IA.PROVEEDORES.firstOrNull { claveDe(it, e).isNotBlank() } ?: "ninguno"
    }
}


fun Diptico.nombreMotor(m: String) = when (m) {
    "local" -> "En tu consola"; "gemini" -> "Gemini"; "openai" -> "ChatGPT"; "ninguno" -> "Sin IA aún"; else -> "Automático"
}


/** Valor que se muestra en el menú: el motor elegido (y, en automático, cuál está usando). */
fun Diptico.textoMotor(e: Estado = estado.value): String = when {
    e.motorIA != "auto" -> nombreMotor(e.motorIA)
    motorEfectivo(e) == "ninguno" -> "Sin elegir"
    else -> "Auto · " + nombreMotor(motorEfectivo(e))
}


/** Una IA en la nube con clave, para temas y nombres: la de DIPPO si es de la nube, si no la primera con clave. */
fun Diptico.llaveNube(e: Estado = estado.value): Llave? {
    val m = motorEfectivo(e)
    if (m in IA.PROVEEDORES && claveDe(m, e).isNotBlank()) return Llave(m, claveDe(m, e))
    return IA.PROVEEDORES.firstOrNull { claveDe(it, e).isNotBlank() }?.let { Llave(it, claveDe(it, e)) }
}


/** Para algo que necesita IA en la nube y no hay ninguna clave: abre el selector de IA con una explicación. */
internal fun Diptico.pedirIANube(para: String) {
    abrirGaleria("ia")
    avisar("$para necesita una IA en la nube. Gemini tiene un nivel gratis: elígela y pega tu clave.")
}


fun Diptico.elegirMotor(m: String) {
    almacen.guardarTexto("motorIA", m)
    actualizar { it.copy(motorIA = m) }
}


/** Elegir una IA en el selector: si le falta algo (bajarla o la clave), lo pide. */
fun Diptico.elegirIA(m: String) {
    val e = estado.value
    when {
        m == "local" && !IALocal.listo(app) -> { elegirMotor("local"); descargarIALocal() }
        m in IA.PROVEEDORES && claveDe(m, e).isBlank() -> { elegirMotor(m); claveVieneDeGaleria = true; abrirTexto(claveTexto(m)) }
        else -> { elegirMotor(m); avisar("DIPPO ahora usa: ${nombreMotor(m)}") }
    }
}


fun Diptico.claveTexto(p: String) = if (p == "gemini") "ia_clave_gemini" else "ia_clave_openai"


fun Diptico.preguntar(texto: String, forzarWeb: Boolean = false) {
    val e = estado.value
    if (e.pensando) return
    if (!forzarWeb) {
        val local = CentroBisa.responder(e, texto) ?: AyudaLocal.responder(texto)?.let { MensajeChat("ia", it) }
        if (local != null) { respuestaBisa(local, texto); return }
    }
    val motor = motorEfectivo(e)
    if (motor == "ninguno" || (motor == "local" && !IALocal.listo(app))) {
        respuestaBisa(MensajeChat("ia", "Puedo ayudarte con tu biblioteca, tus notas y las funciones de Díptico. Para preguntas abiertas sobre guías o trucos, configura un motor.",
            if (e.sesion == null) listOf(AccionChat("bisa_motor", "", "Configurar motor")) else emptyList()), texto)
        return
    }
    if (motor in IA.PROVEEDORES && claveDe(motor, e).isBlank()) {
        claveParaAsistente = true; abrirTexto(claveTexto(motor))
        avisar("Para usar ${nombreMotor(motor)} pega tu clave, o elige otra IA en Menú › DIPPO.")
        return
    }
    sumarDippo("preguntas")
    val chat = e.chat.takeLast(38) + MensajeChat("yo", texto)
    // La respuesta aparece mientras se escribe (con la IA local).
    actualizar { it.copy(chat = chat, pensando = true, indice = 0) }
    scope.launch {
        val r = withContext(Dispatchers.IO) {
            try {
                val historia = chat.takeLast(12).filter { it.texto.isNotBlank() }.map { (if (it.rol == "yo") "user" to it.texto else "assistant" to it.texto) }
                if (motor == "local") {
                    // La IA de la consola no sabe buscar: Díptico busca por ella y le pasa lo encontrado.
                    val web = if (forzarWeb || (estado.value.bisaWeb && Web.conviene(texto))) {
                        actualizar { it.copy(pensandoQue = "Buscando en internet…") }
                        Web.comoContexto(Web.buscar(texto, estado.value.idioma)).also { actualizar { it.copy(pensandoQue = "Pensando…") } }
                    } else ""
                    val sistema = Asistente.sistemaLocal(e) + if (web.isNotBlank()) "\n\n$web" else ""
                    var ultimo = 0L
                    Result.success(
                        IALocal.responder(app, sistema, historia) { parcial ->
                            val ahora = System.currentTimeMillis()
                            if (ahora - ultimo > 120) {
                                ultimo = ahora
                                val visible = parcial.substringBefore("[[").trim()
                                actualizar { it.copy(chat = chat + MensajeChat("ia", visible)) }
                            }
                        },
                    )
                } else {
                    val sistema = Asistente.sistema(e)
                    if (forzarWeb || estado.value.bisaWeb) actualizar { it.copy(pensandoQue = "Pensando y buscando…") }
                    Result.success(IA.conversar(Llave(motor, claveDe(motor)), sistema, historia, 1200, listos = true, buscarWeb = forzarWeb || estado.value.bisaWeb))
                }
            } catch (ex: Exception) {
                Result.failure(ex)
            }
        }
        val msg = r.fold({ Asistente.interpretar(it) }, { MensajeChat("ia", "⚠ " + (it.message ?: "No pude hablar con la IA.")) })
        actualizar { it.copy(chat = chat + msg, pensando = false, pensandoQue = "", indice = if (msg.acciones.isNotEmpty()) 2 else 0) }
        leerSiHablo(msg.texto)
        ultimaCharla = System.currentTimeMillis()
    }
}


/** Baja la IA local (unos 1,6 GB). Se puede cortar y seguir después. */
fun Diptico.descargarIALocal() {
    if (IALocal.listo(app)) { avisar("La IA local ya está lista"); return }
    if (trabajosMapa.containsKey("ialocal")) return
    trabajo("ialocal", "Bajando la IA de DIPPO", IALocal.bajado(app) / IALocal.TAMANO.toFloat())
    avisar("Bajando la IA de DIPPO (1,6 GB). Puedes seguir usando Díptico.")
    scope.launch {
        val error = withContext(Dispatchers.IO) {
            IALocal.descargar(app) { a -> scope.launch { trabajo("ialocal", "Bajando la IA de DIPPO", a) } }
        }
        terminarTrabajo("ialocal")
        if (error != null) avisar(error) else {
            avisar("¡Listo! DIPPO ya piensa dentro de tu consola, sin internet")
            if (estado.value.motorIA != "local") elegirMotor("auto")
        }
        actualizar { it.copy(iaLocalLista = IALocal.listo(app)) }
    }
}


/** Las acciones sensibles al juego comprueban de nuevo el contexto al pulsarlas. */
fun Diptico.ejecutarAccion(a: AccionChat) {
    porVoz = false
    val e = estado.value
    val c = CentroBisa.contexto(e)
    if (e.pensando) return
    if (c.jugando && a.tipo in setOf("jugar", "ver", "ficha", "tema", "vista", "buscar", "menu",
            "bisa_biblioteca", "bisa_retomar", "bisa_sugerir", "bisa_logros", "bisa_motor", "bisa_apps")) {
        avisar("Esta acción está disponible al volver al inicio")
        return
    }
    when (a.tipo) {
        "bisa_biblioteca" -> abrirMenu(Seccion.BIBLIOTECA)
        "bisa_motor" -> abrirGaleria("ia")
        "bisa_apps" -> abrirApps()
        "bisa_sugerir" -> respuestaBisa(CentroBisa.sugerir(e))
        "bisa_retomar" -> ultimoJugado(e)?.takeUnless { e.datos[it.clave]?.oculto == true }
            ?.let { jugarDesdeWidget(it.clave) } ?: avisar("Aún no hay una partida reciente")
        "bisa_sistema" -> {
            val ayuda = CentroBisa.ayudaSistema(e, a.valor) ?: return
            val j = c.juego
            val consola = c.consola
            if (j == null || consola == null || consola.sinEmulador) respuestaBisa(ayuda)
            else scope.launch {
                val elegido = withContext(Dispatchers.IO) {
                    runCatching { Lanzador.elegida(app, consola,
                        e.datos[j.clave]?.emulador ?: e.emuladores[consola.id], archivoParaJugar(j, e).tipo)?.nombre }.getOrNull()
                }
                val actual = estado.value
                if (CentroBisa.contexto(actual).juego?.clave == j.clave && !actual.pensando) {
                    val detalle = elegido?.let { " Emulador seleccionado para abrirlo: $it." }
                        ?: " No encontré un emulador reconocido para ese archivo."
                    respuestaBisa(ayuda.copy(texto = ayuda.texto + detalle))
                }
            }
        }
        "bisa_logros" -> if (c.juego?.clave == a.valor) abrirFichaDe(a.valor, 4)
        "bisa_pista" -> { sumarDippo("pistas"); CentroBisa.prepararPista(e, a.valor)?.let { respuestaBisa(it) } }
        "bisa_dura" -> responderDuracion(a.valor)
        "bisa_logros_ver" -> abrirGaleria("logros_dippo")
        "bisa_logros_dippo" -> respuestaBisa(CentroBisa.contarLogros(progresoLogrosDippo(e)))
        "bisa_escribir" -> if (c.juego?.clave == a.valor) escribirAlAsistente()
        "bisa_pausa" -> if (c.jugando && c.juego?.clave == a.valor) {
            if (e.temporizadorFin > 0L) {
                val minutos = ((e.temporizadorFin - System.currentTimeMillis()).coerceAtLeast(0L) + 59_999) / 60_000
                respuestaBisa(MensajeChat("ia", "Tu aviso de pausa está activo. Faltan unos $minutos min."))
            } else alternarTemporizador()
        }
        "bisa_paso", "bisa_mapa" -> if (c.juego?.clave == a.valor && (!c.jugando || c.pantallaLibre)) {
            if (a.tipo == "bisa_mapa") abrirMapa(a.valor)
            else {
                pasoDeBisa = a.valor
                abrirTexto("bisa_paso", e.datos[a.valor]?.paso.orEmpty())
            }
        }
        "preguntar" -> preguntar(a.valor)
        "pista" -> preguntar(if (a.valor == "solucion") "Dime la solución completa, paso a paso." else "Dame una pista más directa.")
        "jugar", "ver", "ficha" -> {
            if (e.juegos.none { it.clave == a.valor } || e.datos[a.valor]?.oculto == true) { avisar("No encontré ese juego"); return }
            sumarDippo("ideas")
            if (a.tipo == "jugar") jugarDesdeWidget(a.valor)
            else {
                actualizar { it.copy(consolaSel = "todo", juegoSel = a.valor, panel = Panel.NINGUNO) }
                if (a.tipo == "ficha") abrirFicha()
            }
        }
        "tema" -> if (todosLosTemas().any { it.id == a.valor }) elegirTema(a.valor) else avisar("No encontré ese tema")
        "vista" -> if (a.valor in VISTAS) {
            almacen.guardarTexto("vista", a.valor)
            actualizar { it.copy(vista = a.valor, panel = Panel.NINGUNO) }
        }
        "buscar" -> actualizar { it.copy(panel = Panel.BUSCAR, busqueda = a.valor.take(40), indice = 0) }
        "menu" -> Seccion.values().firstOrNull { it.name.equals(a.valor, true) }?.let { abrirMenu(it) }
    }
}

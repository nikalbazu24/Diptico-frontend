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
 * Elegir una imagen de internet: funciones de Diptico movidas aquí desde Diptico.kt para que ese archivo no siga creciendo.
 * Se llaman igual que antes (Diptico.x()); fuera del paquete estado hay que importar cl.nikalbazu.diptico.estado.*.
 */


fun Diptico.nombreCandidato(t: String) = when (t) { "arte", "c_hero" -> "imagen grande"; "logo", "c_logo" -> "logo"; "c_icono", "c_portada" -> "ícono"; else -> "portada" }


/** Pestañas de la tienda según qué se busca (las consolas usan RetroArch en vez de libretro). */
fun Diptico.fuentesImagen(e: Estado = estado.value): List<String> {
    val carpeta = e.candidatoPara == "c_portada"
    if (!carpeta && e.candidatoPara.startsWith("c_")) return listOf("todas", "RetroArch", "SteamGridDB", "Wikipedia")
    val c = e.raspadores
    val consola = if (carpeta) juegoDeCarpetaParaArte(e, e.nombreImagen)?.consolaId else juegoFicha()?.consolaId
    val fuentes = if (carpeta) listOf("todas", "RetroArch") + FUENTES_IMAGEN.drop(1) else FUENTES_IMAGEN
    return fuentes.filter { f -> when (f) {
        "Steam" -> consola in setOf("steam", "windows", "pc")
        "ScreenScraper" -> c.screenscraper
        "IGDB" -> c.igdb
        "TheGamesDB" -> c.conTgdb
        "RAWG" -> c.conRawg
        else -> true
    } }
}


fun Diptico.nombreFuente(f: String) = if (f == "todas") "Todas" else f


/** Lo que se ve en la tienda: atajos (mis archivos, quitar, conectar SteamGridDB) y las imágenes de la fuente elegida. */
fun Diptico.candidatosVisibles(e: Estado = estado.value): List<cl.nikalbazu.diptico.data.ImagenWeb> {
    val j = juegoFicha()
    val d = j?.let { e.datos[it.clave] }
    val cp = e.consolasPropias[e.consolaEditando]
    val propia = when (e.candidatoPara) {
        "arte" -> d?.arte; "logo" -> d?.logo
        "c_hero" -> cp?.hero; "c_logo" -> cp?.logo; "c_icono" -> e.iconosPropios[e.consolaEditando]
        "c_portada" -> cp?.portadaCarpeta
        else -> d?.portada
    }
    val atajos = buildList {
        add(cl.nikalbazu.diptico.data.ImagenWeb("accion:archivos", "", "Mis archivos"))
        if (propia != null) add(cl.nikalbazu.diptico.data.ImagenWeb("accion:quitar", propia, "Quitar la propia"))
        if (e.claveSgdb.isBlank()) add(cl.nikalbazu.diptico.data.ImagenWeb("accion:sgdb", "", "Conectar SteamGridDB"))
        if (!e.candidatoPara.startsWith("c_") || e.candidatoPara == "c_portada") add(cl.nikalbazu.diptico.data.ImagenWeb("accion:fuentes", "", "Configurar fuentes"))
    }
    val lista = if (e.fuenteImagen == "todas") e.candidatos else e.candidatos.filter { it.fuente == e.fuenteImagen }
    return atajos + lista
}


fun Diptico.cambiarFuenteImagen(d: Int) {
    val l = fuentesImagen()
    actualizar { it.copy(fuenteImagen = l[(l.indexOf(it.fuenteImagen) + d).mod(l.size)], indice = 0) }
}


internal fun Diptico.juegoDeCarpetaParaArte(e: Estado, nombre: String?): Juego? {
    val juegos = if (e.consolaEditando == "todo") e.juegos else e.juegosPorConsola[e.consolaEditando].orEmpty()
    val consulta = nombre?.let(Busqueda::normalizar)
    if (!consulta.isNullOrEmpty()) {
        juegos.firstOrNull { Busqueda.normalizar(it.titulo) == consulta }?.let { return it }
    }
    return juegos.firstOrNull { e.datos[it.clave]?.oculto != true }
}


internal fun Diptico.lanzarBusquedaImagenes(
    tipo: String,
    titulo: String,
    iniciales: List<cl.nikalbazu.diptico.data.ImagenWeb> = emptyList(),
    buscar: () -> List<cl.nikalbazu.diptico.data.ImagenWeb>,
) {
    busquedaImagenes?.cancel()
    actualizar { it.copy(panel = Panel.IMAGENES, candidatos = iniciales, candidatoPara = tipo,
        buscandoCandidatos = true, indice = 0, nombreImagen = titulo, fuenteImagen = "todas") }
    busquedaImagenes = scope.launch {
        val lista = withContext(Dispatchers.IO) { runCatching(buscar).getOrDefault(emptyList()) }
        if (estado.value.panel == Panel.IMAGENES) {
            actualizar { it.copy(candidatos = (iniciales + lista).distinctBy { it.url }, buscandoCandidatos = false) }
        }
    }
}


fun Diptico.buscarCandidatos(tipo: String, nombre: String? = null) {
    val e = estado.value
    val consulta = nombre?.takeIf { it.isNotBlank() }
    if (tipo.startsWith("c_")) {
        val consola = Consolas.porId(e.consolaEditando)
        if (consola == null && tipo != "c_portada") return
        val titulo = consulta ?: consola?.bonito ?: "Todo"
        val juego = if (tipo == "c_portada") juegoDeCarpetaParaArte(e, consulta) else null
        val iniciales = if (tipo == "c_portada" && consola != null) {
            Descargas.candidatosConsola(consola, "c_icono", titulo, "", null)
        } else emptyList()
        lanzarBusquedaImagenes(tipo, titulo, iniciales) {
            val wiki = if (tipo == "c_icono" || consola == null && consulta == null) null
            else runCatching { Fichas.buscar(app, titulo, "") }.getOrNull()
            val plataforma = consola?.let { Descargas.candidatosConsola(it, tipo, titulo, e.claveSgdb, wiki) }
                ?: listOfNotNull(wiki?.principal)
            val sistema = juego?.let { Consolas.porId(it.consolaId) }
            if (juego == null || sistema == null) plataforma
            else {
                val nombreJuego = consulta ?: juego.titulo
                val muestra = if (Busqueda.normalizar(nombreJuego) == Busqueda.normalizar(juego.titulo)) juego
                else juego.copy(clave = "${juego.consolaId}/busqueda-icono", archivo = nombreJuego, titulo = nombreJuego)
                plataforma + runCatching {
                    Descargas.candidatos(app, muestra, sistema, "portada", nombreJuego, e.claveSgdb, null, e.raspadores)
                }.getOrDefault(emptyList())
            }
        }
        return
    }
    val j = juegoFicha() ?: return
    val crudo = todosCrudos().firstOrNull { it.clave == j.clave } ?: return
    val c = Consolas.porId(crudo.consolaId) ?: return
    val titulo = consulta ?: j.titulo
    lanzarBusquedaImagenes(tipo, titulo) {
        val wiki = if (nombre == null) e.ficha ?: Fichas.buscar(app, titulo, c.bonito) else Fichas.buscar(app, titulo, c.bonito)
        Descargas.candidatos(app, crudo, c, tipo, titulo, e.claveSgdb, wiki, e.raspadores)
    }
}


/** Usa la opción elegida de la tienda: la baja y la deja como imagen propia del juego (o hace el atajo). */
fun Diptico.usarCandidato(i: Int = estado.value.indice) {
    val e = estado.value
    val img = candidatosVisibles(e).getOrNull(i) ?: return
    if (e.candidatoPara.startsWith("c_")) { usarCandidatoConsola(img); return }
    val j = juegoFicha() ?: return
    val tipo = e.candidatoPara
    when (img.url) {
        "accion:archivos" -> { pedirImagen(tipo); return }
        "accion:fuentes" -> { abrirMenu(Seccion.PORTADAS); return }
        "accion:sgdb" -> { abrirTexto("sgdb", e.claveSgdb); return }
        "accion:quitar" -> {
            cambiarDatos(j.clave) { when (tipo) { "arte" -> it.copy(arte = null); "logo" -> it.copy(logo = null); else -> it.copy(portada = null) } }
            avisar("Listo, volvió la ${nombreCandidato(tipo)} original")
            actualizar { it.copy(indice = 0) }
            return
        }
    }
    avisar("Bajando la ${nombreCandidato(tipo)}…")
    scope.launch {
        val f = withContext(Dispatchers.IO) {
            if (img.url.startsWith("http")) try { Descargas.bajarElegida(app, j, tipo, img.url) } catch (ex: Exception) { null } else null
        }
        if (f == null) { avisar("No pude bajar esa imagen. Prueba otra."); return@launch }
        val u = Uri.fromFile(f).toString()
        cambiarDatos(j.clave) {
            when (tipo) { "arte" -> it.copy(arte = u); "logo" -> it.copy(logo = u); else -> it.copy(portada = u) }
        }
        avisar("¡Listo! Nueva ${nombreCandidato(tipo)} para ${j.titulo}")
    }
}


internal fun Diptico.cambiarDatos(clave: String, f: (DatosJuego) -> DatosJuego) {
    val datos = estado.value.datos.toMutableMap()
    datos[clave] = f(datos[clave] ?: DatosJuego())
    almacen.guardarDatos(datos)
    actualizar { it.copy(datos = datos) }
    publicarJuegos()
}


/** Oculta un juego del estante (o lo vuelve a mostrar). Los ocultos se ven en Buscar › filtro Ocultos. */
fun Diptico.alternarOculto(clave: String) {
    val oculto = estado.value.datos[clave]?.oculto != true
    val e = estado.value
    // Al ocultarlo, la selección pasa al juego vecino.
    val v = e.visibles
    val i = v.indexOfFirst { it.clave == clave }
    val vecino = if (oculto && e.filtro != "ocultos") (v.getOrNull(i + 1) ?: v.getOrNull(i - 1))?.clave else e.juegoSel
    cambiarDatos(clave) { it.copy(oculto = oculto) }
    actualizar { it.copy(juegoSel = vecino, panel = Panel.NINGUNO) }
    avisar(if (oculto) "Juego oculto. Lo ves en Buscar › Ocultos" else "El juego volvió al estante")
}


fun Diptico.alternarFavorito(clave: String) {
    cambiarDatos(clave) { it.copy(favorito = !it.favorito) }
    avisar(if (estado.value.datos[clave]?.favorito == true) "Agregado a favoritos" else "Quitado de favoritos")
}


/** La página donde se saca cada clave (para el botón "Sacar mi clave" y su código QR). */
fun Diptico.urlClave(para: String): String? = when (para) {
    "ras_igdbId", "ras_igdbSecreto" -> "https://dev.twitch.tv/console/apps/create"
    "ras_ssUsuario", "ras_ssClave" -> "https://www.screenscraper.fr/membreinscription.php"
    "ras_ssDevId", "ras_ssDevClave" -> "https://www.screenscraper.fr/forumsujets.php?frub=12&numpage=0"
    "ras_rawg" -> "https://rawg.io/apidocs"
    "ras_tgdb" -> "https://forums.thegamesdb.net/viewforum.php?f=10"
    "ia_clave_gemini", "gemini" -> "https://aistudio.google.com/apikey"
    "ia_clave_openai", "openai" -> "https://platform.openai.com/api-keys"
    "sgdb" -> "https://www.steamgriddb.com/profile/preferences/api"
    "ra_clave", "ra_usuario" -> "https://retroachievements.org/settings"
    "discord_webhook" -> Discord.AYUDA
    "discord_app", "discord_secreto" -> "https://discord.com/developers/applications"
    else -> null
}


/** Abre en el navegador la página para sacar la clave de [para]. */
fun Diptico.abrirPaginaClave(para: String) {
    val u = urlClave(para) ?: return
    avisar("Abriendo la página. Copia la clave y vuelve a Díptico para pegarla")
    abrirWeb(u)
}


fun Diptico.abrirWeb(url: String) {
    try {
        app.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        avisar("No encontré un navegador para abrir el enlace")
    }
}


/** Guarda lo escrito en el cuadro de texto según para qué era. */
internal fun Diptico.guardarTextoPanel() {
    val e = estado.value
    val t = e.borrador.trim()
    when (e.textoPara) {
        "reporte_fallas" -> { confirmarReporte(t); return }
        "titulo" -> e.fichaDe?.let { c -> cambiarDatos(c) { it.copy(titulo = t.ifEmpty { null }) } }
        "notas" -> e.fichaDe?.let { c -> cambiarDatos(c) { it.copy(notas = e.borrador) } }
        "buscar_ajuste" -> {
            actualizar { it.copy(buscarAjuste = t, panel = Panel.MENU, seccion = Seccion.ESENCIAL, enOpciones = true, indice = 0) }
            return
        }
        "buscar_imagen" -> {
            buscarCandidatos(e.candidatoPara, t)
            return
        }
        "coleccion_nueva" -> {
            crearColeccion(t.ifBlank { "Mi colección" }, e.seleccionado?.clave)
            return
        }
        "ras_igdbId", "ras_igdbSecreto", "ras_ssUsuario", "ras_ssClave", "ras_ssDevId", "ras_ssDevClave", "ras_rawg", "ras_tgdb" -> {
            guardarRaspador(e.textoPara.removePrefix("ras_"), e.borrador.trim())
            return
        }
        "buscar_juego" -> {
            actualizar { it.copy(panel = Panel.BUSCAR, busqueda = t.take(40), indice = 0) }
            return
        }
        "pc_steam" -> {
            actualizar { it.copy(panel = Panel.MENU, seccion = Seccion.BIBLIOTECA, enOpciones = true) }
            buscarEnSteam(t)
            return
        }
        "bisa_paso" -> {
            val clave = pasoDeBisa
            pasoDeBisa = null
            actualizar { it.copy(panel = Panel.ASISTENTE, indice = 0) }
            if (clave != null && CentroBisa.contexto(e).juego?.clave == clave) {
                val datos = estado.value.datos.toMutableMap()
                datos[clave] = (datos[clave] ?: DatosJuego()).copy(paso = t)
                almacen.guardarDatos(datos)
                actualizar { it.copy(datos = datos) }
                respuestaBisa(MensajeChat("ia", if (t.isBlank()) "Quité tu próximo paso." else "Guardé tu próximo paso: $t"))
            }
            return
        }
        "asistente" -> {
            actualizar { it.copy(panel = Panel.ASISTENTE, indice = 0) }
            if (t.isNotEmpty()) preguntar(t)
            return
        }
        "consola_nombre" -> {
            cambiarConsolaPropia(e.consolaEditando) { it.copy(nombre = t.take(30).ifBlank { null }) }
            actualizar { it.copy(panel = Panel.EDITAR_CONSOLA) }
            return
        }
        "perfil_apodo" -> {
            val apodo = t.removePrefix("@").replace(" ", "").take(20)
            cambiarPerfil { it.copy(apodo = apodo) }
            if (apodo.isNotEmpty()) avisar("Tu apodo es @$apodo")
        }
        "perfil_nacimiento" -> {
            val f = Perfil.fecha(t)
            if (f == null) { avisar("No entendí la fecha. Escríbela así: 12/03/1990"); return }
            cambiarPerfil { it.copy(nacimiento = f) }
            avisar(if (f.isEmpty()) "Fecha quitada" else "Fecha guardada")
        }
        "nombre" -> if (e.extra.edicionPerfil != null) cambiarEdicion { it.copy(nombre = t.take(24)) } else {
            almacen.guardarTexto("nombre", t.take(24))
            actualizar { it.copy(nombre = t.take(24)) }
            if (t.isNotBlank()) avisar("¡Hola, ${t.take(24)}!")
        }
        "ra_usuario" -> {
            almacen.guardarTexto("usuarioRA", t)
            actualizar { it.copy(usuarioRA = t, perfilRA = null) }
            if (t.isNotEmpty() && estado.value.claveRA.isBlank()) {
                abrirTexto("ra_clave")
                avisar("Ahora pega tu Web API Key")
                return
            }
            cargarPerfilRA(true)
        }
        "ra_clave" -> {
            almacen.guardarTexto("claveRA", t)
            actualizar { it.copy(claveRA = t, perfilRA = null) }
            cargarPerfilRA(true)
        }
        "ia_clave_gemini", "ia_clave_openai" -> {
            if (e.textoPara == "ia_clave_gemini") { almacen.guardarTexto("claveGemini", t); actualizar { it.copy(claveGemini = t) } }
            else { almacen.guardarTexto("claveOpenAI", t); actualizar { it.copy(claveOpenAI = t) } }
            if (claveVieneDeGaleria) {
                claveVieneDeGaleria = false
                abrirGaleria("ia")
                if (t.isNotEmpty()) avisar("Clave guardada. DIPPO ya puede pensar con ${nombreMotor(estado.value.motorIA)}.")
                return
            }
            if (t.isNotEmpty() && claveParaAsistente) {
                claveParaAsistente = false
                abrirTexto("asistente")
                avisar("Clave guardada. Ahora escribe tu pregunta.")
                return
            }
        }
        "discord_secreto" -> {
            val v = t.trim()
            almacen.guardarTexto("discordSecreto", v)
            cerrarPanel()
            if (v.isNotEmpty() && estado.value.extra.discordUsuario.isBlank()) iniciarDiscord()
            return
        }
        "discord_app" -> {
            val id = t.trim()
            if (id.isNotEmpty() && !Regex("[0-9]{15,22}").matches(id)) { avisar("El Application ID son solo números (15 a 22)"); return }
            almacen.guardarTexto("discordApp", id)
            actualizar { it.copy(extra = it.extra.copy(discordApp = id)) }
            cerrarPanel()
            if (id.isNotEmpty() && estado.value.extra.discordUsuario.isBlank()) iniciarDiscord()
            return
        }
        "discord_webhook" -> {
            val url = t.trim()
            if (url.isNotEmpty() && !Discord.valido(url)) { avisar("Ese enlace no es un webhook de Discord (empieza con https://discord.com/api/webhooks/)"); return }
            almacen.guardarTexto("discordWebhook", url)
            actualizar { it.copy(extra = it.extra.copy(discordWebhook = url)) }
            cerrarPanel()
            if (url.isNotEmpty()) probarDiscord()
            return
        }
        "tema_enlace" -> {
            cerrarPanel()
            if (t.isNotBlank()) importarTemas { TiendaTemas.catalogo(t) }
            return
        }
    }
    cerrarPanel()
}

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
 * Tutorial guiado: funciones de Diptico movidas aquí desde Diptico.kt para que ese archivo no siga creciendo.
 * Se llaman igual que antes (Diptico.x()); fuera del paquete estado hay que importar cl.nikalbazu.diptico.estado.*.
 */


/** La configuración de la primera vez: solo lo que hace falta para jugar. Lo demás tiene buenos valores y se cambia en Ajustes. */
fun Diptico.pasosTuto(e: Estado = estado.value): List<PasoTuto> = (
    // Tus pantallas (solo si el equipo tiene dos) · Tú · Tus juegos · Para jugar · Tus cuentas · Tu estilo · Listo.
    // Quien elige dos pantallas puede pasar después a una sola en Ajustes › Pantallas › Modo de pantallas.
    listOfNotNull(if (e.dosDisponibles) PasoTuto.PANTALLAS else null) +
        listOf(PasoTuto.BIENVENIDA, PasoTuto.CARPETA, PasoTuto.EMULADORES, PasoTuto.CUENTAS, PasoTuto.APARIENCIA, PasoTuto.LISTO)
)

/** Los nombres cortos de cada paso, para la fila de arriba. */
fun nombrePasoTuto(p: PasoTuto) = when (p) {
    PasoTuto.PANTALLAS -> "Tus pantallas"; PasoTuto.BIENVENIDA -> "Tú"; PasoTuto.CARPETA -> "Tus juegos"; PasoTuto.EMULADORES -> "Para jugar"
    PasoTuto.CUENTAS -> "Tus cuentas"; PasoTuto.APARIENCIA -> "Tu estilo"; PasoTuto.LISTO -> "Listo"; else -> p.name.lowercase()
}

/** Los temas que se ofrecen la primera vez (los cuatro primeros incluidos). */
internal val TEMAS_INICIALES: List<Tema> get() = Temas.incluidos



internal fun Diptico.irAPaso(p: PasoTuto) {
    actualizar { it.copy(pasoTuto = p, indice = 0, panel = Panel.NINGUNO) }
}

/** ↑↓ en la configuración: de fila en fila; el último lugar es «Siguiente». */
fun Diptico.moverConfig(d: Int) {
    val n = opcionesTuto().size
    actualizar { it.copy(indice = (it.indice + d).coerceIn(0, n)) }
    cl.nikalbazu.diptico.audio.Sonidos.mover()
}

/** ◀▶ en la configuración: cambia la opción de la fila (personaje, color, tema…) sin moverse de ella. */
fun Diptico.ladoConfig(d: Int) {
    val o = opcionesTuto().getOrNull(estado.value.indice) ?: return
    val menos = o.menos ?: return
    if (d < 0) menos() else o.accion()
    cl.nikalbazu.diptico.audio.Sonidos.mover()
}

/** Qué dice el botón fijo de abajo. */
fun Diptico.textoSiguiente(e: Estado): String = when (e.pasoTuto) {
    PasoTuto.LISTO -> "Empezar el recorrido"
    PasoTuto.CARPETA -> if (e.juegos.isEmpty()) "Seguir sin juegos" else "Siguiente"
    else -> "Siguiente"
}


fun Diptico.siguientePaso() {
    val pasos = pasosTuto()
    val e = estado.value
    // Al dejar «Tus juegos» con juegos encontrados, las portadas se bajan de fondo.
    if (e.pasoTuto == PasoTuto.CARPETA && e.juegos.isNotEmpty() && e.descarga == null && e.sinPortada > 0) descargarPortadas(todo = false)
    val i = pasos.indexOf(e.pasoTuto)
    if (i < 0 || i >= pasos.size - 1) terminarConfiguracion(recorrido = true) else irAPaso(pasos[i + 1])
    cl.nikalbazu.diptico.audio.Sonidos.aceptar()
}


fun Diptico.pasoAnterior() {
    val pasos = pasosTuto()
    val i = pasos.indexOf(estado.value.pasoTuto)
    if (i > 0) irAPaso(pasos[i - 1])
}


fun Diptico.marcarProbado(boton: String) {
    if (estado.value.pasoTuto == PasoTuto.CONTROLES) actualizar { it.copy(probados = it.probados + boton) }
}


fun Diptico.reiniciarTutorial() {
    almacen.configurado = false
    actualizar {
        it.copy(
            configurado = false, pasoTuto = PasoTuto.BIENVENIDA, indice = 0, panel = Panel.NINGUNO,
            probados = emptySet(), intercambiar = it.intercambiar,
        )
    }
}


fun Diptico.opcionesTuto(): List<Opcion> {
    val e = estado.value
    return when (e.pasoTuto) {
        PasoTuto.BIENVENIDA -> {
            val per = e.extra.perfil.personaje
            val ids = PERSONAJES.map { it.first } + INICIAL
            val k = ids.indexOf(per).coerceAtLeast(0)
            val colores = Perfil.COLORES
            val c = colores.indexOfFirst { it.second == e.extra.perfil.color }.coerceAtLeast(0)
            val conDiscord = e.extra.discordUsuario.isNotBlank()
            listOf(
                Opcion("Tu nombre", e.nombre.ifBlank { "Toca para escribir" }, Icono.TITULO) { abrirTexto("nombre", e.nombre) },
                Opcion("Tu personaje", nombrePersonaje(per), Icono.BISA,
                    menos = { cambiarPerfil { it.copy(personaje = ids[(k - 1).mod(ids.size)], foto = "", fotoDiscord = false) } },
                ) { cambiarPerfil { it.copy(personaje = ids[(k + 1).mod(ids.size)], foto = "", fotoDiscord = false) } },
                Opcion("Tu color", colores[c].first, Icono.PALETA,
                    menos = { cambiarPerfil { it.copy(color = colores[(c - 1).mod(colores.size)].second) } },
                ) { cambiarPerfil { it.copy(color = colores[(c + 1).mod(colores.size)].second) } },
                Opcion("Foto de Discord", when { e.extra.perfil.fotoDiscord -> "En uso ✓"; conDiscord -> "Usar la de ${e.extra.discordUsuario}"; else -> "Conectar (opcional)" }, Icono.PERFIL) {
                    if (!conDiscord) iniciarDiscord() else cambiarPerfil { it.copy(fotoDiscord = !it.fotoDiscord) }
                },
                Opcion("Idioma", (Idioma.IDIOMAS.firstOrNull { it.first == e.idioma }?.second ?: e.idioma), Icono.IDIOMA) {
                    val l = Idioma.IDIOMAS.map { it.first }
                    cambiarIdioma(l[(l.indexOf(estado.value.idioma) + 1).mod(l.size)])
                },
            )
        }
        PasoTuto.PANTALLAS -> listOf(
            Opcion(
                "Dos pantallas",
                if (e.dosDisponibles) "Arriba el juego, abajo eliges" else "No detecto una segunda pantalla",
                Icono.PANTALLAS, apagada = !e.dosDisponibles,
            ) {
                if (estado.value.dosDisponibles) {
                    elegirModo("dos"); siguientePaso()
                } else {
                    avisar("Este equipo tiene una sola pantalla. Elige \"Una pantalla\".")
                }
            },
            Opcion("Una pantalla", if (e.dosDisponibles) "La versión de una pantalla; la de abajo queda en negro" else "Todo en tu pantalla", Icono.UNA) {
                elegirModo("una_negra"); siguientePaso()
            },
            Opcion("Una pantalla, abajo libre", "Para usar otra app abajo", Icono.LIBRE, apagada = !e.dosDisponibles) {
                if (estado.value.dosDisponibles) {
                    elegirModo("una_libre"); siguientePaso()
                } else {
                    avisar("Este equipo tiene una sola pantalla. Elige \"Una pantalla\".")
                }
            },
        )
        PasoTuto.TEXTO -> listOf(
            Opcion("Normal", "Cabe más en la pantalla", Icono.TEXTO) { fijarTam(1f); siguientePaso() },
            Opcion("Grande", "Más fácil de leer", Icono.TEXTO) { fijarTam(1.15f); siguientePaso() },
            Opcion("Muy grande", "Para leer de lejos", Icono.TEXTO) { fijarTam(1.3f); siguientePaso() },
        )
        PasoTuto.DISPOSICION -> listOf(
            Opcion("Juego arriba, estante abajo", "Recomendado", Icono.ARRIBA) {
                almacen.intercambiar = false
                actualizar { it.copy(intercambiar = false) }
                siguientePaso()
            },
            Opcion("Estante arriba, juego abajo", "Lo puedes cambiar cuando quieras", Icono.INTERCAMBIAR) {
                almacen.intercambiar = true
                actualizar { it.copy(intercambiar = true) }
                siguientePaso()
            },
        )
        PasoTuto.JUEGOS -> listOf(
            Opcion("En la pantalla de arriba", "Recomendado: es la grande", Icono.ARRIBA) {
                almacen.pantallaJuegos = "arriba"
                actualizar { it.copy(pantallaJuegos = "arriba") }
                siguientePaso()
            },
            Opcion("En la pantalla de abajo", "Útil para juegos de una pantalla chica", Icono.ABAJO) {
                almacen.pantallaJuegos = "abajo"
                actualizar { it.copy(pantallaJuegos = "abajo") }
                siguientePaso()
            },
        )
        PasoTuto.CARPETA -> when {
            e.escaneando -> emptyList()
            else -> listOf(
                Opcion(if (e.tieneCarpeta) "Elegir otra carpeta" else "Elegir la carpeta de juegos", "La que tiene snes, ps2, gba…", Icono.CARPETA) { pedirCarpeta?.invoke(Carpeta.JUEGOS) },
            )
        }
        PasoTuto.EMULADORES -> e.consolasEmu.map { c ->
                Opcion(c.bonito, if (emuladorListo(c)) nombreEmulador(c) else "Falta: ${c.sugerido}", Icono.EMULADOR, activa = emuladorListo(c)) { ciclarEmulador(c) }
            }
        PasoTuto.CUENTAS -> {
            // Todo lo que se conecta, en un solo lugar y todo opcional: Discord, RetroAchievements, las fuentes de portadas y la IA.
            val x = e.extra
            val conDiscord = x.discordUsuario.isNotBlank()
            listOf(
                Opcion("Discord", if (conDiscord) x.discordUsuario + " ✓" else "Con un código QR", Icono.MUNDO,
                    "Tus amigos y tu foto de perfil. Aparece un código QR: lo escaneas con el teléfono. Nunca escribes tu contraseña aquí.") {
                    if (!estado.value.extra.discordUsuario.isNotBlank()) iniciarDiscord() else avisar("Discord ya está conectado")
                },
                Opcion("RetroAchievements · usuario", e.usuarioRA.ifBlank { "Opcional" }, Icono.TROFEO,
                    "Tu usuario de retroachievements.org, para ver tus logros en la ficha de cada juego.") { abrirTexto("ra_usuario", estado.value.usuarioRA) },
                Opcion("RetroAchievements · clave", if (e.claveRA.isBlank()) "Pegar" else "Guardada ✓", Icono.CLAVE,
                    "Tu «Web API Key», en retroachievements.org › Settings. Solo se guarda en tu consola.") { abrirTexto("ra_clave") },
                Opcion("SteamGridDB", if (e.claveSgdb.isNotBlank()) "Conectada ✓" else "Opcional", Icono.CLAVE,
                    "Arte grande y logos. Clave gratis en steamgriddb.com › Preferencias › API.") { abrirClave() },
            ) + opcionesRaspadores(e).map { it.copy(valor = if (it.valor == "✓") "Conectada ✓" else if (it.valor == "Añadir") "Opcional" else it.valor) } + listOf(
                Opcion("IA de DIPPO", textoMotor(e), Icono.CHISPA,
                    "Su cerebro: la IA de tu consola (gratis), Gemini o ChatGPT. Cada persona usa su propia clave.") { abrirGaleria("ia") },
            )
        }
        PasoTuto.PORTS -> listOf(
            Opcion(
                "Elegir mis ports",
                if (e.ports.isEmpty()) "Ship of Harkinian, Doom, Mario 64…" else "${e.ports.size} elegidos ✓",
                Icono.APPS,
            ) { abrirPorts() },
            Opcion(
                "Juegos de Android",
                if (e.android.isEmpty()) "Genshin, Minecraft y más" else "${e.android.size} encontrados ✓",
                Icono.MOVIL,
            ) { abrirPorts(Consolas.ANDROID) },
            Opcion(if (e.ports.isEmpty()) "Ahora no" else "Continuar", "Lo encuentras en el menú, en Biblioteca", Icono.CONTINUAR) {
                siguientePaso()
            },
        )
        PasoTuto.PORTADAS -> listOf(
            Opcion(
                if (e.descarga != null) "Descargando…" else "Descargar portadas ahora",
                when {
                    e.descarga != null -> "${e.descarga.hechos} de ${e.descarga.total} · sigue sola de fondo"
                    e.juegos.isEmpty() -> "Primero necesitas juegos"
                    else -> "Gratis, desde libretro · ${e.sinPortada} sin portada"
                },
                Icono.DESCARGA,
            ) {
                if (estado.value.juegos.isNotEmpty()) {
                    if (estado.value.descarga == null) descargarPortadas(todo = false)
                    siguientePaso()
                } else {
                    avisar("Primero elige tu carpeta de juegos")
                }
            },
            Opcion(
                "Clave de SteamGridDB",
                if (e.claveSgdb.isNotBlank()) "Conectada ✓ · logos y arte grande" else "Opcional · logos y arte grande",
                Icono.CLAVE,
            ) { abrirClave() },
            Opcion("Imágenes de ES-DE", if (e.tieneMedia) "Conectadas ✓" else "Si ya usas ES-DE", Icono.CARPETA) {
                pedirCarpeta?.invoke(Carpeta.MEDIA)
            },
            Opcion("Después", "Lo encuentras en el menú, en Portadas", Icono.CONTINUAR) { siguientePaso() },
        )
        PasoTuto.MUSICA -> Musica.pistas.take(3).map { p ->
            Opcion(p.nombre, p.estilo, Icono.MUSICA) { fijarPista(p.id); siguientePaso() }
        } + listOf(
            Opcion("Mezcla", "Las tres, una tras otra", Icono.ESTRELLA) { fijarPista("mezcla"); siguientePaso() },
            Opcion("Sin música", "Silencio total", Icono.SILENCIO) { fijarMusica(false); siguientePaso() },
            Opcion(
                "Mi propia música",
                if (e.carpetaMusica != null) "Carpeta elegida ✓" else "Elige una carpeta con canciones",
                Icono.CARPETA,
            ) {
                if (estado.value.carpetaMusica != null) {
                    fijarPista("mia"); siguientePaso()
                } else {
                    pedirCarpeta?.invoke(Carpeta.MUSICA)
                }
            },
        )
        PasoTuto.APARIENCIA -> {
            val temas = TEMAS_INICIALES
            val t = temas.indexOfFirst { it.id == e.tema }.coerceAtLeast(0)
            listOf(
                Opcion("Tema", temas.getOrNull(t)?.nombre ?: "", Icono.TEMA,
                    menos = { elegirTema(temas[(t - 1).mod(temas.size)].id) }) { elegirTema(temas[(t + 1).mod(temas.size)].id) },
                Opcion("Detrás de cada consola", if (e.extra.fondoConsolas == "consola") "La consola" else "Su juego emblema", Icono.PORTADA,
                    menos = { elegirFondoConsolas(if (estado.value.extra.fondoConsolas == "consola") "emblema" else "consola") },
                ) { elegirFondoConsolas(if (estado.value.extra.fondoConsolas == "consola") "emblema" else "consola") },
                Opcion("Música y sonidos", if (e.musica) "Encendidos" else "Apagados", Icono.MUSICA, activa = e.musica) { fijarMusica(!estado.value.musica) },
            )
        }
        PasoTuto.CONTROLES -> listOf(Opcion("Continuar", "Cuando quieras", Icono.CHECK) { siguientePaso() })
        PasoTuto.LISTO -> emptyList()
    }
}



internal fun Diptico.vistaPrevia(paso: PasoTuto, i: Int) {
    when (paso) {
        PasoTuto.APARIENCIA -> {
            fijarTema(i != 1)
            elegirTema(if (i == 2) "diptico_vidrio" else "diptico")
        }
        PasoTuto.TEXTO -> tamanos.getOrNull(i)?.let { fijarTam(it) }
        PasoTuto.MUSICA -> when (i) {
            in 0..3 -> {
                val p = ordenPistas[i]
                almacen.pista = p
                almacen.musica = true
                actualizar { it.copy(pista = p, musica = true) }
            }
            4 -> fijarMusica(false)
            else -> if (estado.value.carpetaMusica != null) {
                almacen.pista = "mia"
                actualizar { it.copy(pista = "mia", musica = true) }
            }
        }
        else -> Unit
    }
}

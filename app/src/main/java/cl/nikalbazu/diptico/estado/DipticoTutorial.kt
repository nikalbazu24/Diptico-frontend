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
fun Diptico.pasosTuto(e: Estado = estado.value): List<PasoTuto> = buildList {
    add(PasoTuto.BIENVENIDA)
    if (e.dosDisponibles) add(PasoTuto.PANTALLAS)
    add(PasoTuto.CARPETA)
    if (e.consolasConJuegos.any { !emuladorListo(it) }) add(PasoTuto.EMULADORES)
    add(PasoTuto.APARIENCIA)
    add(PasoTuto.LISTO)
}


internal fun Diptico.indiceInicial(p: PasoTuto, e: Estado): Int = when (p) {
    PasoTuto.PANTALLAS -> if (!e.modoElegido) (if (e.dosDisponibles) 0 else 1)
        else listOf("dos", "una_negra", "una_libre").indexOf(e.modoPantallas).coerceAtLeast(0)
    PasoTuto.TEXTO -> tamanos.indexOfFirst { kotlin.math.abs(it - e.tamTexto) < 0.01f }.coerceAtLeast(0)
    PasoTuto.DISPOSICION -> if (e.intercambiar) 1 else 0
    PasoTuto.JUEGOS -> if (e.pantallaJuegos == "abajo") 1 else 0
    PasoTuto.MUSICA -> if (!e.musica) 4 else ordenPistas.indexOf(e.pista).let { if (it < 0) 5 else it }
    PasoTuto.APARIENCIA -> if (e.tema == "diptico_vidrio") 2 else if (e.oscuro) 0 else 1
    else -> 0
}


internal fun Diptico.irAPaso(p: PasoTuto) {
    actualizar { it.copy(pasoTuto = p, indice = indiceInicial(p, it), panel = Panel.NINGUNO) }
}


fun Diptico.siguientePaso() {
    val pasos = pasosTuto()
    val i = pasos.indexOf(estado.value.pasoTuto)
    if (i < 0 || i >= pasos.size - 1) terminarConfiguracion() else irAPaso(pasos[i + 1])
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
        PasoTuto.BIENVENIDA -> listOf(
            Opcion("¡Comencemos!", "Toma unos 2 minutos", Icono.JUGAR) { siguientePaso() },
            Opcion("Idioma", (Idioma.IDIOMAS.firstOrNull { it.first == e.idioma }?.second ?: e.idioma), Icono.IDIOMA) {
                val l = Idioma.IDIOMAS.map { it.first }
                cambiarIdioma(l[(l.indexOf(estado.value.idioma) + 1).mod(l.size)])
            },
        )
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
            Opcion("Una pantalla", if (e.dosDisponibles) "La de abajo queda en negro" else "Todo en tu pantalla", Icono.UNA) {
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
            !e.tieneCarpeta -> listOf(
                Opcion("Elegir carpeta de juegos", "La que tiene snes, psx, nds…", Icono.CARPETA) { pedirCarpeta?.invoke(Carpeta.JUEGOS) },
                Opcion("Hacerlo después", "Puedes elegirla desde el menú", Icono.CONTINUAR) { siguientePaso() },
            )
            e.escaneando -> emptyList()
            e.juegos.isEmpty() -> listOf(
                Opcion("Elegir otra carpeta", icono = Icono.CARPETA) { pedirCarpeta?.invoke(Carpeta.JUEGOS) },
                Opcion("Continuar sin juegos", icono = Icono.CONTINUAR) { siguientePaso() },
            )
            else -> listOf(
                Opcion("Continuar", "${e.juegos.size} juegos encontrados · las portadas se bajan solas", Icono.CHECK) {
                    // Las portadas se bajan de fondo (antes era un paso aparte).
                    if (estado.value.descarga == null && estado.value.sinPortada > 0) descargarPortadas(todo = false)
                    siguientePaso()
                },
                Opcion("Elegir otra carpeta", icono = Icono.CARPETA) { pedirCarpeta?.invoke(Carpeta.JUEGOS) },
            )
        }
        PasoTuto.EMULADORES -> listOf(Opcion("Continuar", icono = Icono.CHECK) { siguientePaso() }) + e.consolasEmu.map { c ->
            Opcion(
                c.bonito,
                if (emuladorListo(c)) nombreEmulador(c) else "Falta: ${c.sugerido}",
                Icono.EMULADOR,
            ) { ciclarEmulador(c) }
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
        PasoTuto.APARIENCIA -> listOf(
            Opcion("Díptico de noche", "Estante y tonos oscuros", Icono.TEMA) { elegirAparienciaInicial(0); siguientePaso() },
            Opcion("Díptico de día", "Estante y tonos claros", Icono.TEMA) { elegirAparienciaInicial(1); siguientePaso() },
        )
        PasoTuto.CONTROLES -> listOf(Opcion("Continuar", "Cuando quieras", Icono.CHECK) { siguientePaso() })
        PasoTuto.LISTO -> listOf(
            Opcion("¡Empezar a jugar!", icono = Icono.JUGAR) { terminarConfiguracion() },
            Opcion("¿Cómo te llama DIPPO?", e.nombre.ifBlank { "Opcional" }, Icono.BISA) { abrirTexto("nombre", e.nombre) },
        )
    }
}


/** En algunos pasos el cambio se ve (y se oye) en vivo mientras eliges. */
internal fun Diptico.elegirAparienciaInicial(i: Int) {
    fijarTema(i != 1)
    elegirTema(if (i == 2) "diptico_vidrio" else "diptico")
    val inicio = if (i == 2) "widgets" else "heroe"
    almacen.guardarTexto("inicio", inicio)
    actualizar { it.copy(inicio = inicio) }
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

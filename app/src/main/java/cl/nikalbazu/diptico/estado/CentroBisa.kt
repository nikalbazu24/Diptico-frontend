package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.data.Logros
import java.text.Normalizer
import java.util.Locale

/** Contexto compartido por el chat, los accesos rápidos y la burbuja de partida. */
internal object CentroBisa {
    data class Contexto(val juego: Juego?, val consola: Consola?, val jugando: Boolean, val pantallaLibre: Boolean) {
        val titulo: String
            get() = when {
                juego == null -> "Tu biblioteca"
                consola == null -> juego.titulo
                else -> "${juego.titulo} · ${consola.nombre}"
            }
    }

    fun contexto(e: Estado): Contexto {
        val sesion = e.sesion
        val juego = if (sesion != null) e.juegos.firstOrNull { it.clave == sesion.clave } else e.seleccionado
        val consola = juego?.let { Consolas.porId(it.consolaId) }
        return Contexto(juego, consola, sesion != null, e.dosDisponibles && !e.unaPantalla && sesion?.dosPantallas != true)
    }

    fun acciones(e: Estado): List<AccionChat> {
        val c = contexto(e)
        val j = c.juego
        if (j == null) return if (c.jugando) emptyList() else listOf(
            AccionChat("bisa_biblioteca", "", "Revisar biblioteca"),
            AccionChat("bisa_retomar", "", "Retomar partida"),
            AccionChat("bisa_sugerir", "", "¿Qué juego hoy?"),
        )
        return buildList {
            if (!c.jugando || c.pantallaLibre) {
                add(AccionChat("bisa_paso", j.clave, "Mi próximo paso"))
                add(AccionChat("bisa_mapa", j.clave, "Mapa"))
            }
            add(AccionChat("bisa_sistema", j.clave, if (c.consola?.sinEmulador == true) "Ayuda de Android" else "Ayuda de ${c.consola?.nombre ?: "este sistema"}"))
            if (c.jugando) {
                add(AccionChat("bisa_pista", j.clave, "Pista sin spoilers"))
                add(AccionChat("bisa_pausa", j.clave, if (e.temporizadorFin > 0L) "Ver aviso de pausa" else "Pausa en ${e.temporizadorMin} min"))
            } else {
                add(AccionChat("bisa_retomar", "", "Retomar partida"))
                add(AccionChat("bisa_sugerir", "", "¿Qué juego hoy?"))
            }
        }
    }

    fun ayudaSistema(e: Estado, clave: String): MensajeChat? {
        val c = contexto(e)
        val j = c.juego?.takeIf { it.clave == clave } ?: return null
        val s = c.consola
        val texto = when {
            s?.sinEmulador == true -> "${j.titulo} se abre como una app de Android. Sus controles y partidas se configuran dentro del juego. Puedes revisar si sigue instalado en el cajón de apps."
            s?.dosPantallas == true -> "${s.bonito} usa las dos pantallas en Díptico. Configura el mando y la pantalla táctil en el emulador. Si tienes permiso de superposición, puedes abrir mi burbuja desde el aviso de partida."
            s?.id == Consolas.RPGMAKER -> "Los juegos de RPG Maker se abren con Essentia. Si uno falla, revisa que elegiste la carpeta del juego y que Essentia esté disponible. En la ficha de Díptico puedes guardar tus notas y el mapa."
            j.tipo == "m3u" || j.variantes.isNotEmpty() -> "Este juego tiene una lista de discos o archivos alternativos. Antes de jugar puedes elegir el archivo en Opciones del juego › Versión del juego. El emulador decide cómo leer sus discos y guardados."
            else -> "Para ${s?.bonito ?: j.consolaId}, revisa el emulador asignado al juego y la carpeta de archivos. Si el emulador pide una BIOS, configúrala dentro de él. Los botones de guardar estado dependen del emulador y su configuración."
        }
        val botones = buildList {
            if (!c.jugando) {
                if (s?.sinEmulador == true) add(AccionChat("bisa_apps", "", "Abrir cajón"))
                else add(AccionChat("menu", "EMULADORES", "Ver emuladores"))
                if (Logros.tieneLogros(j.consolaId)) add(AccionChat("bisa_logros", j.clave, "Ver logros"))
            }
            if (!c.jugando || c.pantallaLibre) add(AccionChat("bisa_paso", j.clave, "Mi próximo paso"))
        }
        return MensajeChat("ia", texto, botones)
    }

    fun prepararPista(e: Estado, clave: String): MensajeChat? {
        val j = contexto(e).juego?.takeIf { it.clave == clave } ?: return null
        val nota = e.datos[j.clave]?.paso.orEmpty().trim()
        val acciones = buildList {
            if (nota.isNotBlank()) add(AccionChat("preguntar",
                "Juego ${j.titulo}. Mi último objetivo anotado es «${nota.take(300)}». Dame una pista suave sin spoilers; pregunta si falta contexto.",
                "Usar mi nota"))
            add(AccionChat("bisa_escribir", j.clave, "Contar dónde estoy"))
        }
        val contexto = if (nota.isBlank()) "" else " Dejaste anotado: «${nota.take(140)}»."
        return MensajeChat("ia", "Dime el jefe, la zona o el objetivo de ${j.titulo} y partimos con una pista suave.$contexto", acciones)
    }

    fun responder(e: Estado, texto: String): MensajeChat? {
        val consulta = Normalizer.normalize(texto, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT).trim().trim('¿', '?', '¡', '!', '.')
        val c = contexto(e)
        val j = c.juego
        return when {
            j != null && consulta in setOf("pista", "una pista", "pista sin spoilers", "estoy atascado", "estoy atascada") ->
                prepararPista(e, j.clave)
            j != null && (consulta in setOf("mi proximo paso", "donde quede", "que deje anotado", "que estaba haciendo") ||
                consulta.startsWith("donde quede en ")) -> {
                val paso = e.datos[j.clave]?.paso.orEmpty()
                MensajeChat("ia", if (paso.isBlank()) "Todavía no hay un próximo paso para ${j.titulo}." else "En ${j.titulo} anotaste: $paso",
                    acciones(e).filter { it.tipo in setOf("bisa_paso", "bisa_mapa") })
            }
            j != null && consulta in setOf("que emulador uso", "ayuda de esta consola", "ayuda de este sistema", "mi juego no abre") -> ayudaSistema(e, j.clave)
            !c.jugando && consulta in setOf("retoma mi partida", "quiero retomar", "continuar mi partida") ->
                MensajeChat("ia", "Puedo abrir el último juego que registró una sesión.", listOf(AccionChat("bisa_retomar", "", "Retomar partida")))
            !c.jugando && (consulta in setOf("que juego hoy", "que juego me recomiendas hoy", "tengo 20 minutos") ||
                consulta.startsWith("quiero algo corto")) -> sugerir(e, consulta.contains("20") || consulta.contains("corto"))
            else -> null
        }
    }

    fun sugerir(e: Estado, breves: Boolean = false): MensajeChat {
        val candidatos = if (breves) PlanPartida.breves(e) else {
            val disponibles = e.juegos.filterNot { e.datos[it.clave]?.oculto == true }
            (PlanPartida.cola(e) + disponibles.filter { e.datos[it.clave]?.favorito == true } + disponibles).distinctBy { it.clave }
        }
        val opciones = candidatos.take(3).map { AccionChat("jugar", it.clave, it.titulo) }
        return MensajeChat("ia", when {
            opciones.isEmpty() && breves -> "Aún no hay suficientes sesiones breves registradas para recomendarte un juego de 20 minutos."
            opciones.isEmpty() -> "Agrega una carpeta de juegos para que pueda recomendarte algo."
            breves -> "Estos juegos suelen entrar en sesiones de hasta 20 minutos, según tus partidas anteriores."
            else -> "Te propongo estos juegos de tu biblioteca. Priorizo tu cola y tus favoritos."
        }, opciones)
    }
}

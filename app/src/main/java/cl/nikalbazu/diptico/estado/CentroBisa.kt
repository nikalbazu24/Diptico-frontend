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
    // ---------- DIPPO toma la iniciativa: habla primero y deja todo listo para tocar ----------

    private fun corto(t: String) = if (t.length <= 22) t else t.take(20).trimEnd() + "…"

    private fun hace(ms: Long): String {
        val dias = ms / 86_400_000L
        return when {
            dias <= 0L -> "hoy"
            dias == 1L -> "ayer"
            dias < 7L -> "hace $dias días"
            dias < 30L -> "hace ${dias / 7} semana${if (dias >= 14) "s" else ""}"
            else -> "hace más de un mes"
        }
    }

    private fun visibles(e: Estado) = e.juegos.filter { e.datos[it.clave]?.oculto != true }

    /** El último juego que jugaste. */
    fun ultimo(e: Estado): Juego? = visibles(e).filter { (e.datos[it.clave]?.ultimo ?: 0L) > 1L }
        .maxByOrNull { e.datos[it.clave]?.ultimo ?: 0L }

    /** Un juego que te gustaba (favorito o con harto tiempo) y que no tocas hace más de dos semanas. */
    fun olvidado(e: Estado, salvo: String?): Juego? {
        val ahora = System.currentTimeMillis()
        return visibles(e).asSequence().filter { it.clave != salvo }.mapNotNull { j ->
            val d = e.datos[j.clave] ?: return@mapNotNull null
            if (d.avance == AvanceJuego.TERMINADO.name || d.ultimo <= 1L || ahora - d.ultimo < 14 * 86_400_000L) return@mapNotNull null
            if (!d.favorito && d.tiempo < 2 * 3600) return@mapNotNull null
            j to (d.tiempo + if (d.favorito) 50 * 3600 else 0)
        }.maxByOrNull { it.second }?.first
    }

    /** Lo que DIPPO dice apenas lo abres: se fija en lo que estabas haciendo y te deja botones listos. */
    fun saludoInicial(e: Estado): MensajeChat {
        val c = contexto(e)
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val nombre = e.nombre.trim().takeIf { it.isNotEmpty() }?.let { ", $it" } ?: ""
        val hola = when (h) { in 6..11 -> "¡Buenos días$nombre!"; in 12..19 -> "¡Buenas tardes$nombre!"; else -> "¡Buenas noches$nombre!" }
        val j = c.juego
        if (c.jugando && j != null) {
            val min = (System.currentTimeMillis() - (e.sesion?.inicio ?: System.currentTimeMillis())) / 60_000
            val r = Asistente.reintentos(j.clave)
            val texto = when {
                r >= 2 -> "Llevas $r intentos seguidos en ${j.titulo}. ¿Te tiro una pista sin spoilers?"
                min >= 90 -> "Llevas ${min / 60} h y ${min % 60} min en ${j.titulo}. ¿Seguimos o te aviso para una pausa?"
                else -> "Aquí estoy, copiloto. Llevas $min min en ${j.titulo}. Toca lo que necesites."
            }
            return MensajeChat("ia", texto, acciones(e).filter { it.tipo in setOf("bisa_pista", "bisa_paso", "bisa_pausa") }.take(3))
        }
        val partes = mutableListOf(hola)
        val botones = mutableListOf<AccionChat>()
        val ult = ultimo(e)
        if (ult != null) {
            val d = e.datos[ult.clave]
            val paso = d?.paso.orEmpty().trim()
            partes += if (paso.isNotEmpty()) "La última vez jugaste ${ult.titulo} y anotaste: «${paso.take(90)}»."
            else "Lo último que jugaste fue ${ult.titulo}, ${hace(System.currentTimeMillis() - (d?.ultimo ?: 0L))}."
            botones += AccionChat("jugar", ult.clave, "Seguir con ${corto(ult.titulo)}")
        }
        val sel = e.seleccionado?.takeIf { it.clave != ult?.clave }
        val olv = olvidado(e, ult?.clave)
        when {
            olv != null -> {
                val dias = (System.currentTimeMillis() - (e.datos[olv.clave]?.ultimo ?: 0L)) / 86_400_000L
                partes += "Y hace $dias días que no le das a ${olv.titulo}… ¿una vueltita?"
                botones += AccionChat("ver", olv.clave, "Ver ${corto(olv.titulo)}")
            }
            ult == null -> {
                partes += "Todavía no te veo jugar nada por aquí. ¿Te elijo algo de tu biblioteca?"
                botones += AccionChat("bisa_sugerir", "", "Elige por mí")
            }
            else -> partes += "¿Seguimos o te sugiero otra cosa?"
        }
        if (sel != null) botones += AccionChat("preguntar", "Cuéntame en pocas líneas de qué trata ${sel.titulo}, sin spoilers, y cuánto dura más o menos.", "¿De qué trata ${corto(sel.titulo)}?")
        if (botones.none { it.tipo == "bisa_sugerir" }) botones += AccionChat("bisa_sugerir", "", "Otra idea")
        return MensajeChat("ia", partes.joinToString(" "), botones.take(4))
    }

    /** Una idea corta para el globito de DIPPO en el inicio (y qué hace al tocarla). */
    fun ideaInicio(e: Estado): Pair<String, AccionChat>? {
        val ult = ultimo(e)
        val olv = olvidado(e, ult?.clave)
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            ult != null && (e.datos[ult.clave]?.paso?.isNotBlank() == true) -> "¿Seguimos con ${corto(ult.titulo)}?" to AccionChat("jugar", ult.clave, "")
            olv != null && h in 10..22 -> "¿Y si vuelves a ${corto(olv.titulo)}?" to AccionChat("ver", olv.clave, "")
            ult != null -> "¿Otra partida de ${corto(ult.titulo)}?" to AccionChat("jugar", ult.clave, "")
            e.juegos.isNotEmpty() -> "¿Te elijo un juego?" to AccionChat("bisa_sugerir", "", "")
            else -> null
        }
    }

    /**
     * Preguntas listas para tocar según lo que tienes delante, para no escribir. [conIA]: si hay una IA que
     * pueda responder preguntas abiertas (si no, solo las que DIPPO resuelve solo).
     */
    fun preguntasListas(e: Estado, conIA: Boolean): List<AccionChat> {
        val c = contexto(e)
        val j = c.juego
        return buildList {
            if (j != null && c.jugando) {
                add(AccionChat("bisa_pista", j.clave, "Estoy atascado"))
                if (conIA) {
                    add(AccionChat("preguntar", "¿Cuáles son los controles básicos de ${j.titulo} y un truco útil para jugar mejor?", "Controles y trucos"))
                    add(AccionChat("preguntar", "Llevo ${(e.datos[j.clave]?.tiempo ?: 0) / 3600} horas en ${j.titulo}. ¿Cuánto dura en total y qué me queda más o menos, sin spoilers?", "¿Cuánto me falta?"))
                }
            } else if (j != null) {
                if (conIA) {
                    add(AccionChat("preguntar", "Cuéntame en pocas líneas de qué trata ${j.titulo}, sin spoilers, y cuánto dura más o menos.", "¿De qué trata?"))
                    add(AccionChat("preguntar", "Dame 3 trucos o secretos buenos de ${j.titulo}, cortitos y sin arruinar la historia.", "Trucos y secretos"))
                    add(AccionChat("preguntar", "¿Qué juegos de mi biblioteca se parecen a ${j.titulo}? Proponme hasta 3, con el botón para jugarlos.", "Parecidos en mi biblioteca"))
                }
                add(AccionChat("bisa_pista", j.clave, "Estoy atascado"))
            }
            if (!c.jugando) {
                if (conIA) add(AccionChat("preguntar", "Elige por mí un juego de mi biblioteca que me pueda gustar hoy, dime por qué en una línea y dame el botón para jugarlo.", "Sorpréndeme"))
                add(AccionChat("preguntar", "Quiero algo corto para 20 minutos", "Algo corto"))
                if (conIA) add(AccionChat("preguntar", "Elige un tema de Díptico que combine con hoy y dame el botón para ponerlo.", "Un tema para hoy"))
            }
        }
    }
}
}

package cl.nikalbazu.diptico.estado

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * El recorrido de la primera vez, paso a paso. Cada paso tiene un nombre ([PASOS]) y casi todos piden hacer algo de
 * verdad: el paso se da por cumplido cuando la interfaz cambió como se pidió ([cumplido]), sin importar si fue con
 * el mando, con el dedo o con el botón «Probar ahora». Hasta cumplirlo no se puede seguir.
 *
 * Empieza en los widgets, porque ahí abre Díptico.
 */
object RecorridoVivo {
    data class Reaccion(val que: String, val vez: Long = System.nanoTime())

    /** Los pasos en orden (van de la mano con [Novedades.RECORRIDO], que tiene sus textos). */
    val PASOS = listOf(
        "aJuego", "consola", "personalizar", "vistaCarpetas", "dippo", "abrirConsola", "vistaJuegos", "opcionesJuego",
        "volver", "aWidgets", "galeria", "editar", "apps", "buscar", "ajustes", "perfil", "fin",
    )

    fun idDe(k: Int): String = PASOS.getOrElse(k) { "fin" }
    fun indiceDe(id: String): Int = PASOS.indexOf(id)

    val reaccion = MutableStateFlow<Reaccion?>(null)

    fun avisar(que: String) { reaccion.value = Reaccion(que) }

    /** El reto del paso (null = solo leer y seguir). */
    fun retoDe(k: Int): String? = idDe(k).takeIf { it != "fin" }

    /** ¿Ya se hizo lo que pide el paso? Compara cómo estaba la interfaz al empezar el paso ([antes]) con cómo está [ahora]. */
    fun cumplido(id: String, antes: Estado, ahora: Estado): Boolean = when (id) {
        "aJuego" -> ahora.inicio != "widgets"
        "consola" -> ahora.consolaSel != antes.consolaSel
        "personalizar" -> ahora.panel == Panel.EDITAR_CONSOLA
        "vistaCarpetas" -> ahora.extra.vistaCarpetasCV != antes.extra.vistaCarpetasCV
        "abrirConsola" -> !ahora.enSistemas
        "vistaJuegos" -> ahora.extra.vistaColeccion != antes.extra.vistaColeccion
        "opcionesJuego" -> ahora.panel == Panel.RAPIDO
        "volver" -> ahora.enSistemas
        "aWidgets" -> ahora.inicio == "widgets"
        "galeria" -> ahora.extra.galeria
        "editar" -> ahora.editandoWidgets
        "apps" -> ahora.panel == Panel.APPS
        "buscar" -> ahora.panel == Panel.BUSCAR
        "ajustes" -> ahora.panel == Panel.MENU
        "perfil" -> ahora.extra.perfilEn >= 0
        else -> false // "dippo" se cumple al tocarlo (avisar("toque")); "fin" no pide nada.
    }

    @Volatile var hecho: Int = -1

    /** Se puede pasar al siguiente: el paso no pide nada, ya se cumplió, o no se puede hacer (biblioteca vacía). */
    fun puedeSeguir(k: Int): Boolean {
        if (retoDe(k) == null || hecho == k) return true
        val e = Diptico.estado.value
        return when (idDe(k)) {
            "abrirConsola", "vistaJuegos", "opcionesJuego" -> e.juegos.isEmpty()
            "volver" -> e.enSistemas
            else -> false
        }
    }

    /** La consola sobre la que actúan los pasos (la elegida, o la primera con juegos). */
    private fun consolaActual(e: Estado) = e.consolaSel.takeIf { it != "todo" } ?: e.pestanas.firstOrNull { it != "todo" }

    /**
     * Hace la acción REAL del paso: la usan el mando (desde Teclas) y el botón «Probar ahora» (para el dedo).
     * [d] es la dirección cuando el paso la tiene (L1 = −1, R1 = +1).
     */
    fun probar(k: Int, d: Int = 1) {
        val e = Diptico.estado.value
        if (!e.recorrido || e.novedades != k) return
        when (idDe(k)) {
            "aJuego" -> Diptico.fijarInicio("heroe")
            "consola" -> Diptico.cambiarConsola(d)
            "personalizar" -> consolaActual(e)?.let { id ->
                if (e.consolaSel != id) Diptico.elegirConsola(id)
                Diptico.abrirEditarConsola(id)
            }
            "vistaCarpetas" -> Diptico.siguienteVistaCarpetas()
            "dippo" -> avisar("toque")
            "abrirConsola" -> consolaActual(e)?.let { Diptico.entrarConsolaCV(it) }
            "vistaJuegos" -> Diptico.siguienteVistaColeccion()
            "opcionesJuego" -> if (e.seleccionado != null) Diptico.abrirRapido()
            "volver" -> { if (e.panel != Panel.NINGUNO) Diptico.cerrarPanel(); Diptico.volverASistemas() }
            "aWidgets" -> Diptico.fijarInicio("widgets")
            "galeria" -> Diptico.abrirGaleriaWidgets()
            "editar" -> { if (e.extra.galeria) Diptico.cerrarGaleriaWidgets(); Diptico.editarEspacio() }
            "apps" -> Diptico.abrirApps()
            "buscar" -> Diptico.abrirBuscar()
            "ajustes" -> Diptico.abrirMenu()
            "perfil" -> Diptico.abrirPerfil()
        }
    }

    /** Deja todo listo al pasar de un paso a otro: cierra lo que abrió el paso que termina y prepara el que sigue. */
    fun alCambiar(desde: Int, hacia: Int) {
        val e = Diptico.estado.value
        when (idDe(desde)) {
            "personalizar", "opcionesJuego", "apps", "buscar", "ajustes" -> if (e.panel != Panel.NINGUNO) Diptico.cerrarPanel()
            "galeria" -> if (e.extra.galeria) Diptico.cerrarGaleriaWidgets()
            "editar" -> if (e.editandoWidgets) Diptico.terminarEspacio()
            "perfil" -> if (e.extra.perfilEn >= 0) Diptico.cerrarPerfil()
        }
        val ahora = Diptico.estado.value
        when (idDe(hacia)) {
            // Los pasos de la biblioteca necesitan ver las consolas; los de widgets, los widgets.
            "consola", "personalizar", "vistaCarpetas", "abrirConsola" -> {
                if (ahora.inicio == "widgets") Diptico.fijarInicio("heroe")
                if (!Diptico.estado.value.enSistemas) Diptico.volverASistemas()
            }
            "galeria", "editar" -> if (ahora.inicio != "widgets") Diptico.fijarInicio("widgets")
        }
    }
}

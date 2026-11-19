package cl.nikalbazu.diptico.estado

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Lo que hace la persona durante el recorrido de DIPPO (botones que aprieta, toques a DIPPO), para que él reaccione.
 * [vez] cambia en cada reacción para que se repita aunque sea el mismo botón.
 */
object RecorridoVivo {
    data class Reaccion(val que: String, val vez: Long = System.nanoTime())

    val reaccion = MutableStateFlow<Reaccion?>(null)

    fun avisar(que: String) { reaccion.value = Reaccion(que) }

    /** Cada desafío está ligado a una transición comprobada de la interfaz, nunca a una tecla vacía. */
    fun retoDe(k: Int): String? = when (k) {
        1 -> "gatillo"
        2 -> "x"
        3, 11 -> "gatillo2"
        5 -> "select"
        6 -> "toque"
        7 -> "abrirCarpeta"
        8 -> "vistaJuego"
        9 -> "opcionesJuego"
        10 -> "volverCarpetas"
        12 -> "paginaWidget"
        13 -> "galeriaWidget"
        14 -> "editarWidget"
        15 -> "pestanaEditor"
        16 -> "apps"
        17 -> "buscar"
        18 -> "ajustes"
        19 -> "perfil"
        20 -> "emuladorJuego"
        21 -> "ajustesEmuladores"
        else -> null
    }

    @Volatile var hecho: Int = -1

    /** Los desafíos dependientes de una biblioteca vacía o de páginas inexistentes son opcionales. */
    fun puedeSeguir(k: Int): Boolean {
        if (retoDe(k) == null || hecho == k) return true
        val e = Diptico.estado.value
        return when (k) {
            7, 8, 9 -> e.juegos.isEmpty()
            10 -> e.enSistemas
            12 -> e.paginas < 2
            15 -> !e.editandoWidgets
            20 -> e.juegos.isEmpty()
            else -> false
        }
    }

    /**
     * Botón táctil del tour: la demostración modifica componentes reales, no una vista simulada.
     * El control físico invoca las mismas operaciones desde Teclas.
     */
    fun probar(k: Int) {
        val e = Diptico.estado.value
        if (!e.recorrido || e.novedades != k) return
        when (k) {
            1 -> {
                val antes = e.consolaSel
                Diptico.cambiarConsola(1)
                if (Diptico.estado.value.consolaSel != antes) avisar("gatillo")
            }
            2 -> {
                val id = e.consolaSel.takeIf { it != "todo" } ?: e.pestanas.firstOrNull { it != "todo" }
                if (id != null) {
                    Diptico.abrirEditarConsola(id)
                    if (Diptico.estado.value.panel == Panel.EDITAR_CONSOLA) avisar("x")
                }
            }
            3, 11 -> {
                Diptico.pasoEspacio(1)
                if (Diptico.estado.value.inicio == "widgets") avisar("gatillo2")
            }
            5 -> {
                val anterior = e.extra.vistaCarpetasCV
                Diptico.siguienteVistaCarpetas()
                if (Diptico.estado.value.extra.vistaCarpetasCV != anterior) avisar("select")
            }
            6 -> avisar("toque")
            7 -> {
                val id = e.consolaSel.takeIf { it != "todo" } ?: e.pestanas.firstOrNull { it != "todo" }
                if (id != null) {
                    Diptico.entrarConsolaCV(id)
                    if (!Diptico.estado.value.enSistemas) avisar("abrirCarpeta")
                }
            }
            8 -> {
                val antes = e.extra.vistaColeccion
                Diptico.elegirVistaColeccion(when (antes) {
                    "estante" -> "cuadricula"
                    "cuadricula" -> "lista"
                    else -> "estante"
                })
                if (Diptico.estado.value.extra.vistaColeccion != antes) avisar("vistaJuego")
            }
            9 -> if (e.seleccionado != null) {
                Diptico.abrirRapido()
                if (Diptico.estado.value.panel == Panel.RAPIDO) avisar("opcionesJuego")
            }
            10 -> {
                if (e.panel != Panel.NINGUNO) Diptico.cerrarPanel()
                Diptico.volverASistemas()
                if (Diptico.estado.value.enSistemas) avisar("volverCarpetas")
            }
            12 -> {
                val antes = e.paginaInicio
                Diptico.cambiarPagina(1)
                if (Diptico.estado.value.paginaInicio != antes) avisar("paginaWidget")
            }
            13 -> {
                Diptico.abrirGaleriaWidgets()
                if (Diptico.estado.value.extra.galeria) avisar("galeriaWidget")
            }
            14 -> {
                if (e.extra.galeria) Diptico.cerrarGaleriaWidgets()
                Diptico.editarEspacio()
                if (Diptico.estado.value.editandoWidgets) avisar("editarWidget")
            }
            15 -> if (e.editandoWidgets) {
                val antes = e.extra.espacioEdita
                Diptico.pestanaEspacio(1)
                if (Diptico.estado.value.extra.espacioEdita != antes) avisar("pestanaEditor")
            }
            16 -> { Diptico.abrirApps(); if (Diptico.estado.value.panel == Panel.APPS) avisar("apps") }
            17 -> { Diptico.abrirBuscar(); if (Diptico.estado.value.panel == Panel.BUSCAR) avisar("buscar") }
            18 -> { Diptico.abrirMenu(); if (Diptico.estado.value.panel == Panel.MENU) avisar("ajustes") }
            19 -> { Diptico.abrirPerfil(); if (Diptico.estado.value.extra.perfilEn >= 0) avisar("perfil") }
            20 -> if (e.seleccionado != null) {
                Diptico.fijarInicio("heroe")
                Diptico.abrirRapido()
                val g = Diptico.gruposJuego().indexOfFirst { it.first.titulo == "Emulador y versión" }
                if (g >= 0) {
                    Diptico.abrirGrupoRapido(g)
                    if (Diptico.estado.value.extra.rapidoGrupo >= 0) avisar("emuladorJuego")
                }
            }
            21 -> {
                Diptico.abrirMenu(Seccion.EMULADORES)
                if (Diptico.estado.value.panel == Panel.MENU) avisar("ajustesEmuladores")
            }
        }
    }
}

package cl.nikalbazu.diptico.estado

/*
 * Colección viva (docs/coleccion-viva.md): lo que cambia el rediseño en el estado.
 * Todo se guarda en el almacén para que quede igual al volver a abrir Díptico.
 */

/** Marca · año · generación, y un párrafo corto con la historia de cada consola (6a/6c). */
data class FichaConsola(val marca: String, val anio: Int, val generacion: String, val historia: String)

object HistoriaConsolas {
    private val fichas = mapOf(
        "ps2" to FichaConsola("SONY", 2000, "SEXTA GENERACIÓN", "Salió en marzo de 2000 con lector de DVD y compatible con los juegos de la primera PlayStation. Con más de 155 millones de unidades, es la consola más vendida de la historia."),
        "psx" to FichaConsola("SONY", 1994, "QUINTA GENERACIÓN", "La primera PlayStation llegó en 1994 con juegos en CD y gráficos en 3D. Convirtió a Sony en uno de los grandes de los videojuegos."),
        "ps3" to FichaConsola("SONY", 2006, "SÉPTIMA GENERACIÓN", "De 2006, trajo el Blu-ray, juego en línea gratis y su procesador Cell, famoso por lo difícil de programar."),
        "ps4" to FichaConsola("SONY", 2013, "OCTAVA GENERACIÓN", "Lanzada en 2013, con el botón Share para compartir capturas y videos. Vendió más de 117 millones de unidades."),
        "psp" to FichaConsola("SONY", 2004, "SÉPTIMA GENERACIÓN", "La primera portátil de Sony, de 2004, con discos UMD y una pantalla ancha que la volvió también reproductor de video."),
        "psvita" to FichaConsola("SONY", 2011, "OCTAVA GENERACIÓN", "De 2011, con pantalla OLED, dos sticks y un panel táctil atrás. Muy querida por sus juegos japoneses e independientes."),
        "snes" to FichaConsola("NINTENDO", 1990, "CUARTA GENERACIÓN", "Llegó a Japón en 1990 como Super Famicom. Sus chips de sonido y el Modo 7 le dieron a los 16 bits algunos de sus juegos más recordados."),
        "sfc" to FichaConsola("NINTENDO", 1990, "CUARTA GENERACIÓN", "La Super Famicom de Japón, de 1990: la misma Super Nintendo con otro diseño y muchos juegos que nunca salieron de allá."),
        "nes" to FichaConsola("NINTENDO", 1983, "TERCERA GENERACIÓN", "Nació en Japón en 1983 como Famicom y revivió los videojuegos en América después de la crisis de 1983."),
        "famicom" to FichaConsola("NINTENDO", 1983, "TERCERA GENERACIÓN", "La Family Computer de 1983, roja y blanca, con los mandos pegados a la consola."),
        "n64" to FichaConsola("NINTENDO", 1996, "QUINTA GENERACIÓN", "De 1996, popularizó el stick analógico y trajo cuatro puertos de mando de fábrica."),
        "gc" to FichaConsola("NINTENDO", 2001, "SEXTA GENERACIÓN", "De 2001, usaba miniDVD de 8 cm y tenía un asa atrás para llevarla."),
        "wii" to FichaConsola("NINTENDO", 2006, "SÉPTIMA GENERACIÓN", "De 2006, se jugaba moviendo el control; vendió más de 100 millones de unidades."),
        "wiiu" to FichaConsola("NINTENDO", 2012, "OCTAVA GENERACIÓN", "De 2012, con un mando que traía su propia pantalla: se podía jugar sin la tele."),
        "switch" to FichaConsola("NINTENDO", 2017, "OCTAVA GENERACIÓN", "Lanzada en 2017, se juega en la tele o en la mano y sus controles se separan para jugar de a dos."),
        "switch2" to FichaConsola("NINTENDO", 2025, "NOVENA GENERACIÓN", "La sucesora de Switch, de 2025: pantalla más grande, controles que se pegan con imanes y compatibilidad con los juegos de antes."),
        "gb" to FichaConsola("NINTENDO", 1989, "CUARTA GENERACIÓN", "La portátil de 1989 con pantalla verde y pilas que duraban horas. Tetris la hizo inolvidable."),
        "gbc" to FichaConsola("NINTENDO", 1998, "QUINTA GENERACIÓN", "De 1998, el Game Boy a color, compatible con los cartuchos del Game Boy original."),
        "gba" to FichaConsola("NINTENDO", 2001, "SEXTA GENERACIÓN", "La portátil de 32 bits de 2001 que también leía los cartuchos del Game Boy. En 2003 llegó el modelo SP, plegable y con luz."),
        "nds" to FichaConsola("NINTENDO", 2004, "SÉPTIMA GENERACIÓN", "De 2004, con dos pantallas y la de abajo táctil: la misma idea que hoy tiene tu Thor."),
        "n3ds" to FichaConsola("NINTENDO", 2011, "OCTAVA GENERACIÓN", "De 2011, con 3D sin lentes en la pantalla de arriba."),
        "virtualboy" to FichaConsola("NINTENDO", 1995, "QUINTA GENERACIÓN", "El visor de 1995 en rojo y negro: uno de los experimentos más raros de Nintendo."),
        "megadrive" to FichaConsola("SEGA", 1988, "CUARTA GENERACIÓN", "La Mega Drive de 1988, rival de la Super Nintendo, rápida como su mascota."),
        "genesis" to FichaConsola("SEGA", 1989, "CUARTA GENERACIÓN", "El nombre de la Mega Drive en Norteamérica, de 1989."),
        "mastersystem" to FichaConsola("SEGA", 1985, "TERCERA GENERACIÓN", "La consola de 8 bits de SEGA, de 1985, muy popular en Brasil y Europa."),
        "gamegear" to FichaConsola("SEGA", 1990, "CUARTA GENERACIÓN", "La portátil a color de SEGA, de 1990, con pantalla con luz y hambre de pilas."),
        "saturn" to FichaConsola("SEGA", 1994, "QUINTA GENERACIÓN", "De 1994, con dos procesadores y un catálogo de lucha y arcade muy querido."),
        "dreamcast" to FichaConsola("SEGA", 1998, "SEXTA GENERACIÓN", "La última consola de SEGA, de 1998, y una de las primeras con módem para jugar en línea."),
        "segacd" to FichaConsola("SEGA", 1991, "CUARTA GENERACIÓN", "El lector de CD de 1991 que se ponía debajo de la Mega Drive."),
        "sega32x" to FichaConsola("SEGA", 1994, "CUARTA GENERACIÓN", "El accesorio de 1994 que le daba 32 bits a la Mega Drive."),
        "xbox" to FichaConsola("MICROSOFT", 2001, "SEXTA GENERACIÓN", "La primera Xbox, de 2001, traía disco duro y Xbox Live para jugar en línea."),
        "xbox360" to FichaConsola("MICROSOFT", 2005, "SÉPTIMA GENERACIÓN", "De 2005, con logros, juego en línea y un catálogo enorme de juegos independientes."),
        "neogeo" to FichaConsola("SNK", 1990, "CUARTA GENERACIÓN", "La máquina de arcade de 1990 que también se vendía para la casa, con cartuchos gigantes."),
        "pcengine" to FichaConsola("NEC", 1987, "CUARTA GENERACIÓN", "La pequeña consola de NEC, de 1987, con juegos en tarjetas del tamaño de una tarjeta de crédito."),
        "atari2600" to FichaConsola("ATARI", 1977, "SEGUNDA GENERACIÓN", "De 1977, llevó los cartuchos y el joystick a millones de casas."),
        "arcade" to FichaConsola("ARCADE", 1978, "MÁQUINAS DE SALÓN", "Las máquinas de los salones de juegos: una moneda, tres vidas y la tabla de récords."),
        "dos" to FichaConsola("PC", 1981, "COMPUTADORES", "Los juegos de PC con MS-DOS, de los 80 y 90: aventuras, estrategia y los primeros juegos en 3D."),
        "ports" to FichaConsola("COMUNIDAD", 2020, "PORTS NATIVOS", "Juegos clásicos rehechos para correr directo en Android, sin emulador."),
        "androidgames" to FichaConsola("GOOGLE", 2008, "ANDROID", "Los juegos de Android que tienes instalados en tu consola."),
    )

    fun de(id: String): FichaConsola? = fichas[id] ?: fichas[id.removeSuffix("jp").removeSuffix("na")]
}

/** Créditos de los modelos 3D de las consolas que van en el equipo (se muestran en Ajustes y bajo cada consola). */
object Modelos3D {
    data class Modelo(val archivo: String, val nombre: String, val autor: String, val licencia: String)
    private val modelos = mapOf(
        "snes" to Modelo("snes", "Snes Reworked", "Manny Ruiz", "CC BY 4.0"),
        "gba" to Modelo("gba", "Gameboy Advance - Zelda Concept", "yassineCGI", "CC BY 4.0"),
        "switch" to Modelo("switch", "Nintendo Switch Console", "eltayerkebulan", "CC BY 4.0"),
        "psp" to Modelo("psp", "Sony PSP", "Ilya Ostrovsky", "CC BY 4.0"),
        "n64" to Modelo("n64", "Nintendo 64", "DevonCarlson", "CC BY-NC 4.0"),
        "gbc" to Modelo("gbc", "Game Boy Color", "MaximePages", "CC BY 4.0"),
        "gb" to Modelo("gb", "Gameboy", "danteizq", "CC BY 4.0"),
        "psx" to Modelo("psx", "Playstation 1", "Felipe Vallerino", "CC BY 4.0"),
        "switch2" to Modelo("switch2", "Nintendo Switch 2", "davibiem.1111", "CC BY 4.0"),
        "n3ds" to Modelo("n3ds", "Nintendo 3DS XL", "Joshua P.", "CC BY 4.0"),
        "xbox360" to Modelo("xbox360", "Classic Xbox 360 - White Edition", "Anand Stephen", "CC BY 4.0"),
        "androidgames" to Modelo("androidgames", "Android Logo (Animation)", "Anthony Yanez", "CC BY 4.0"),
        "ports" to Modelo("ports", "USB memory", "Marsoft", "CC BY 4.0"),
        "gc" to Modelo("gc", "Low poly Nintendo Gamecube", "LuisAR", "CC BY 4.0"),
        "wii" to Modelo("wii", "Nintendo Wii", "MacDrawz", "CC BY 4.0"),
        "ps3" to Modelo("ps3", "Playstation 3", "Smoggybeard", "CC BY-NC-SA 4.0"),
        "wiiu" to Modelo("wiiu", "Wii U GamePad controller & stand", "synthetic worlds", "CC BY 4.0"),
        "ps2" to Modelo("ps2", "classic ps2", "jei3323", "CC BY 4.0"),
        "ps4" to Modelo("ps4", "classic ps4", "jei3323", "CC BY 4.0"),
    )
    private val alias = mapOf("sfc" to "snes", "snesna" to "snes", "android" to "androidgames", "androidapps" to "androidgames",
        "nds" to "n3ds", "3ds" to "n3ds", "rpgmaker" to "ports", "gahvoria" to "ports", "gamecube" to "gc", "ngc" to "gc")
    fun idDe(id: String): String = alias[id] ?: id
    /**
     * Cuánto girar cada modelo para que parta mostrando su frente (cada autor lo armó mirando a un lado distinto;
     * revisado con `ps2work/frentes.py`, que los dibuja desde 0°, 90°, 180° y 270°).
     */
    private val frentes = mapOf("gba" to -90f, "n64" to 90f, "ps3" to 90f, "switch" to 180f, "wii" to -90f)
    fun frente(id: String): Float = frentes[idDe(id)] ?: 0f
    fun de(id: String): Modelo? = modelos[idDe(id)]
    fun credito(id: String): String? = de(id)?.let { "Modelo «${it.nombre}» de ${it.autor} · ${it.licencia}" }
    fun todos(): List<Pair<String, Modelo>> = modelos.toList()
}

fun Diptico.alternarColeccionViva(v: Boolean) {
    almacen.guardarBool("coleccionViva", v)
    if (v) almacen.guardarTexto("vistaConsolas", "carpetas")
    actualizar { if (v) it.copy(vistaConsolas = "carpetas", enSistemas = true, extra = it.extra.copy(coleccionViva = true)) else it.copy(extra = it.extra.copy(coleccionViva = false)) }
}

/** Toca la caja (o R3): gira al reverso y abajo pasa al carrete; de nuevo, vuelve. */
fun Diptico.girarCaja(v: Boolean = !estado.value.extra.cajaGirada) {
    actualizar { it.copy(extra = it.extra.copy(cajaGirada = v, carreteSel = 0)) }
}

fun Diptico.moverCarrete(d: Int, total: Int) {
    if (total <= 0) return
    actualizar { it.copy(extra = it.extra.copy(carreteSel = (it.extra.carreteSel + d).coerceIn(0, total - 1))) }
}

fun Diptico.elegirCarrete(i: Int) = actualizar { it.copy(extra = it.extra.copy(carreteSel = i)) }

fun Diptico.elegirVistaColeccion(v: String) {
    almacen.guardarTexto("vistaColeccion", v)
    // La cuadrícula y la lista usan la navegación en rejilla de siempre; el estante va de lado.
    val vista = when (v) { "cuadricula" -> "cuadricula"; "lista" -> "lista"; else -> "carrusel" }
    almacen.guardarTexto("vista", vista)
    actualizar { it.copy(vista = vista, extra = it.extra.copy(vistaColeccion = v)) }
}

val VISTAS_CARPETAS_CV = listOf("icono", "diptico", "repisa", "carpeta")
fun nombreVistaCarpetas(v: String) = when (v) { "icono" -> "Ícono"; "repisa" -> "Repisa"; "carpeta" -> "Carpeta"; else -> "Díptico" }

fun Diptico.elegirVistaCarpetas(v: String) {
    almacen.guardarTexto("vistaCarpetasCV", v)
    actualizar { it.copy(extra = it.extra.copy(vistaCarpetasCV = v)) }
}

fun Diptico.siguienteVistaCarpetas() {
    val i = VISTAS_CARPETAS_CV.indexOf(estado.value.extra.vistaCarpetasCV)
    elegirVistaCarpetas(VISTAS_CARPETAS_CV[(i + 1) % VISTAS_CARPETAS_CV.size])
}

/** Columnas de la rejilla de carpetas según la vista (para que la cruceta suba y baje bien). */
fun columnasCarpetasCV(v: String, unaPantalla: Boolean): Int = when (v) {
    "icono" -> -1
    "repisa" -> 1
    "carpeta" -> if (unaPantalla) 3 else 2
    else -> if (unaPantalla) 2 else 1
}

fun Diptico.elegirJuegosCarpeta(modo: String) {
    almacen.guardarTexto("juegosCarpeta", modo)
    actualizar { it.copy(extra = it.extra.copy(juegosCarpeta = modo)) }
}

fun Diptico.elegirCuantosCarpeta(n: Int) {
    almacen.guardarTexto("cuantosCarpeta", n.coerceIn(1, 5).toString())
    actualizar { it.copy(extra = it.extra.copy(cuantosCarpeta = n.coerceIn(1, 5))) }
}

/** Ícono de las carpetas: cuántas por fila (2–6), se ajusta pellizcando con dos dedos. */
fun Diptico.ajustarColumnasIcono(n: Int) {
    val v = n.coerceIn(2, 6)
    if (v == estado.value.extra.columnasIconoCV) return
    almacen.guardarTexto("columnasIconoCV", v.toString())
    actualizar { it.copy(extra = it.extra.copy(columnasIconoCV = v)) }
}

fun Diptico.alternarLogoConsola(id: String) {
    val s = estado.value.extra.logoConsolas.let { if (id in it) it - id else it + id }
    almacen.guardarTexto("logoConsolas", s.joinToString("|"))
    actualizar { it.copy(extra = it.extra.copy(logoConsolas = s)) }
}

fun Diptico.elegirPestanaCarpetas(i: Int) = actualizar { it.copy(extra = it.extra.copy(pestanaCarpetas = i)) }

fun Diptico.alternarEditarWidgetsCV() = actualizar { it.copy(extra = it.extra.copy(editandoWidgetsCV = !it.extra.editandoWidgetsCV)) }

/**
 * Los juegos que van junto al ícono de una carpeta: los últimos jugados, los favoritos o los que eligió la persona
 * (los favoritos de esa consola, si no eligió ninguno). Siempre hasta [cuantos].
 */
fun juegosJuntoAlIcono(e: Estado, consolaId: String, modo: String, cuantos: Int): List<cl.nikalbazu.diptico.data.Juego> {
    val todos = e.juegos.filter { it.consolaId == consolaId }
    val orden = when (modo) {
        "favoritos" -> todos.filter { e.datos[it.clave]?.favorito == true }.ifEmpty { todos }
        "elegidos" -> todos.filter { e.datos[it.clave]?.favorito == true }.ifEmpty { todos }
        else -> todos.sortedByDescending { e.datos[it.clave]?.ultimo ?: 0L }
    }
    return orden.take(cuantos.coerceIn(1, 5))
}

/** «Ícono de la carpeta» (6d): abre la búsqueda de imágenes con sus fuentes (SteamGridDB, ScreenScraper, libretro…). */
fun Diptico.cambiarIconoCarpeta(id: String) {
    actualizar { it.copy(consolaEditando = id) }
    buscarCandidatos("c_portada")
}

/** Entrar a la consola elegida (A en las carpetas), con el sonido de encendido de Díptico. */
fun Diptico.entrarConsolaCV(id: String) {
    if (estado.value.consolaSel != id) elegirConsola(id)
    cl.nikalbazu.diptico.audio.Sonidos.encendido(id)
    entrarSistema()
}

/** El último juego jugado de una consola (para «Y Seguir con…»). */
fun ultimoDeConsola(e: Estado, id: String): cl.nikalbazu.diptico.data.Juego? =
    e.juegos.filter { it.consolaId == id && (e.datos[it.clave]?.ultimo ?: 0L) > 1 }.maxByOrNull { e.datos[it.clave]!!.ultimo }

fun Diptico.seguirCon(j: cl.nikalbazu.diptico.data.Juego) {
    actualizar { it.copy(consolaSel = j.consolaId, juegoSel = j.clave, enSistemas = false) }
    jugar()
}

fun Diptico.quitarWidgetCV(id: String) {
    val s = estado.value.extra.widgetsCVOcultos + id
    almacen.guardarTexto("widgetsCVOcultos", s.joinToString("|"))
    actualizar { it.copy(extra = it.extra.copy(widgetsCVOcultos = s)) }
}

fun Diptico.ponerWidgetCV(id: String) {
    val s = estado.value.extra.widgetsCVOcultos - id
    almacen.guardarTexto("widgetsCVOcultos", s.joinToString("|"))
    actualizar { it.copy(extra = it.extra.copy(widgetsCVOcultos = s)) }
}

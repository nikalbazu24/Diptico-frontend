package cl.nikalbazu.diptico

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.emu.App
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Panel
import cl.nikalbazu.diptico.estado.PasoTuto
import cl.nikalbazu.diptico.estado.Seccion
import cl.nikalbazu.diptico.estado.Sesion
import cl.nikalbazu.diptico.ui.Estilo
import cl.nikalbazu.diptico.ui.Lienzo
import cl.nikalbazu.diptico.estado.Tema
import cl.nikalbazu.diptico.estado.Temas
import cl.nikalbazu.diptico.estado.Trabajo
import cl.nikalbazu.diptico.estado.Widget
import cl.nikalbazu.diptico.estado.Widgets
import cl.nikalbazu.diptico.data.FichaInfo
import cl.nikalbazu.diptico.ui.Raiz
import cl.nikalbazu.diptico.ui.Rol
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Fotos de cada pantalla con el tamaño real de la AYN Thor, para revisar el diseño sin tener la consola.
 * Arriba: 1920x1080 (731x411 dp). Abajo: 1240x1080 (472x411 dp).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = ARRIBA)
class Capturas {
    @get:Rule
    val regla = createComposeRule()

    private fun juego(id: String, consola: String, titulo: String) =
        Juego("$consola/$id", consola, titulo, "$titulo.zip", Uri.parse("content://prueba/$consola/$id"), "primary:Juegos/$consola/$id")

    private val juegos = listOf(
        juego("1", "snes", "Leyenda del Bosque"),
        juego("2", "gba", "Cohete Rojo Advance"),
        juego("3", "psx", "Bahía Neón"),
        juego("4", "nds", "Ruta Estelar DS"),
        juego("5", "ps2", "Caballero de Papel"),
        juego("6", "n64", "Pixel Kart 64"),
        juego("7", "snes", "Torre de Cristal"),
        juego("8", "psp", "Isla Tormenta"),
    )

    private fun base(): Estado = Estado(
        listo = true, configurado = true, tieneCarpeta = true, juegos = juegos, juegoSel = "snes/1",
        modoPantallas = "dos", modoElegido = true, dosDisponibles = true, intro = false, mostrarIntro = true,
        datos = mapOf("snes/1" to DatosJuego(tiempo = 3 * 3600 + 25 * 60, paso = "Buscar la llave del templo del agua", ultimo = 1L)),
    )

    private fun foto(nombre: String, e: Estado, rol: Rol, tema: Tema? = null, mi: Int = 0) {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(e)
        Estilo.tema = tema ?: Temas.incluidos[0]
        Estilo.decoraciones = true
        Estilo.efecto = e.efecto
        cl.nikalbazu.diptico.estado.Idioma.fijar(ApplicationProvider.getApplicationContext(), e.idioma)
        regla.mainClock.autoAdvance = false
        regla.setContent {
            Lienzo(e.oscuro, Modifier.fillMaxSize(), e.tamTexto) { Raiz(e, rol, mi) }
        }
        regla.mainClock.advanceTimeBy(1600)
        regla.onRoot().captureRoboImage("capturas/$nombre.png")
    }

    private fun tuto(paso: PasoTuto, indice: Int = 0) = base().copy(configurado = false, pasoTuto = paso, indice = indice, modoElegido = false, modoPantallas = "una_negra")

    // ---------- Tutorial (siempre en una pantalla) ----------

    @Test fun t01_tutorial_bienvenida() = foto("01_tutorial_bienvenida", tuto(PasoTuto.BIENVENIDA), Rol.UNICA)
    @Test fun t02_tutorial_pantallas() = foto("02_tutorial_pantallas", tuto(PasoTuto.PANTALLAS), Rol.UNICA)
    @Test fun t03_tutorial_pantallas_odin() = foto("03_tutorial_pantallas_una_sola", tuto(PasoTuto.PANTALLAS, 1).copy(dosDisponibles = false), Rol.UNICA)
    @Test fun t04_tutorial_letra() = foto("04_tutorial_letra_grande", tuto(PasoTuto.TEXTO, 1).copy(tamTexto = 1.15f), Rol.UNICA)
    @Test fun t05_tutorial_carpeta() = foto("05_tutorial_carpeta", tuto(PasoTuto.CARPETA).copy(tieneCarpeta = false, juegos = emptyList()), Rol.UNICA)
    @Test fun t06_tutorial_emuladores() = foto("06_tutorial_emuladores", tuto(PasoTuto.EMULADORES), Rol.UNICA)
    @Test fun t07_tutorial_portadas() = foto("07_tutorial_portadas", tuto(PasoTuto.PORTADAS), Rol.UNICA)
    @Test fun t08_tutorial_musica() = foto("08_tutorial_musica", tuto(PasoTuto.MUSICA, 1).copy(pista = "cartucho"), Rol.UNICA)
    @Test fun t09_tutorial_controles() = foto("09_tutorial_controles", tuto(PasoTuto.CONTROLES).copy(probados = setOf("cruceta", "Y")), Rol.UNICA)

    @Config(qualifiers = ABAJO)
    @Test fun t10_tutorial_abajo() = foto("10_tutorial_abajo_dos_pantallas", tuto(PasoTuto.TEXTO).copy(modoElegido = true, modoPantallas = "dos"), Rol.INFERIOR)

    // ---------- Inicio con dos pantallas ----------

    @Test fun t11_inicio_arriba() = foto("11_inicio_arriba", base(), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t12_inicio_abajo() = foto("12_inicio_abajo", base(), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t13_inicio_abajo_claro() = foto("13_inicio_abajo_claro", base().copy(oscuro = false, consolaSel = "snes"), Rol.INFERIOR)

    // ---------- Menú ----------

    @Config(qualifiers = ABAJO)
    @Test fun t14_menu_abajo() = foto("14_menu_abajo", base().copy(panel = Panel.MENU, seccion = Seccion.PANTALLAS, enOpciones = true, indice = 1), Rol.INFERIOR)

    @Test fun t15_menu_ayuda_arriba() = foto("15_menu_ayuda_arriba", base().copy(panel = Panel.MENU, seccion = Seccion.PANTALLAS, enOpciones = true, indice = 1), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t16_menu_portadas() = foto("16_menu_portadas", base().copy(panel = Panel.MENU, seccion = Seccion.PORTADAS, enOpciones = false), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t17_menu_musica() = foto("17_menu_musica", base().copy(panel = Panel.MENU, seccion = Seccion.MUSICA, enOpciones = true), Rol.INFERIOR)

    // ---------- Pantallas intercambiadas ----------

    @Test fun t18_intercambiado_arriba() = foto("18_intercambiado_estante_arriba", base().copy(intercambiar = true), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t19_intercambiado_abajo() = foto("19_intercambiado_juego_abajo", base().copy(intercambiar = true), Rol.SUPERIOR)

    // ---------- Una pantalla (Thor en modo una, Odin, tele) ----------

    @Test fun t20_una_pantalla() = foto("20_una_pantalla", base().copy(modoPantallas = "una_negra"), Rol.UNICA)

    @Test fun t21_una_pantalla_menu() = foto("21_una_pantalla_menu", base().copy(modoPantallas = "una_negra", panel = Panel.MENU, seccion = Seccion.EMULADORES, enOpciones = true), Rol.UNICA)

    // ---------- Otros ----------

    @Config(qualifiers = ABAJO)
    @Test fun t22_menu_rapido() = foto("22_menu_rapido", base().copy(panel = Panel.RAPIDO), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t23_jugando_atenuada() = foto(
        "23_jugando_abajo_atenuada", base().copy(sesion = Sesion("snes/1", System.currentTimeMillis() - 65_000, 99, false)), Rol.INFERIOR,
    )

    // ---------- Ports ----------

    private fun port(pkg: String, titulo: String) = Uri.parse("app://$pkg").let {
        Juego("ports/$pkg", "ports", titulo, pkg, it, pkg, portada = it)
    }

    private fun conPorts() = base().copy(
        juegos = juegos + listOf(port("com.ejemplo.harkinian", "Ship of Harkinian"), port("com.ejemplo.doom", "Doom Clásico"), port("com.ejemplo.sm64", "Mario 64 Port")),
        consolaSel = "ports", juegoSel = "ports/com.ejemplo.harkinian",
        ports = listOf("com.ejemplo.harkinian", "com.ejemplo.doom", "com.ejemplo.sm64"),
    )

    @Config(qualifiers = ABAJO)
    @Test fun t25_ports_estante() = foto("25_ports_estante", conPorts(), Rol.INFERIOR)

    @Test fun t26_ports_arriba() = foto("26_ports_arriba", conPorts(), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t27_ports_elegir() = foto(
        "27_ports_elegir",
        conPorts().copy(
            panel = Panel.PORTS, indice = 1,
            apps = listOf(
                App("com.ejemplo.harkinian", "Ship of Harkinian"), App("com.ejemplo.doom", "Doom Clásico"),
                App("com.ejemplo.sm64", "Mario 64 Port"), App("com.ejemplo.musica", "Mi reproductor"), App("com.ejemplo.notas", "Notas"),
            ),
        ),
        Rol.INFERIOR,
    )

    @Test fun t28_tutorial_ports() = foto("28_tutorial_ports", tuto(PasoTuto.PORTS), Rol.UNICA)

    // ---------- v0.3 ----------

    private val ficha = FichaInfo(
        "Leyenda del Bosque",
        "Leyenda del Bosque es un videojuego de acción y aventura de 1994. El jugador recorre un bosque encantado buscando las cinco llaves del templo para despertar al guardián.",
        "El juego nació como un proyecto de dos amigos que querían mezclar exploración libre con acertijos. El equipo dibujó más de 300 escenarios a mano antes de pasarlos a la consola.",
        "Cada zona del bosque cambia según la hora del día. Los acertijos usan la música: algunas puertas solo se abren tocando la melodía correcta.",
        "Fue muy bien recibido por su música y su mundo. Vendió más de un millón de copias.",
        "https://es.wikipedia.org/wiki/Ejemplo", "es",
    )

    @Test fun t29_inicio_widgets() = foto("29_inicio_widgets", base().copy(inicio = "widgets"), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t30_ficha_abajo() = foto("30_ficha_abajo", base().copy(panel = Panel.FICHA, fichaDe = "snes/1", ficha = ficha, pestanaFicha = 0), Rol.INFERIOR)

    @Test fun t31_ficha_arriba() = foto("31_ficha_lectura_arriba", base().copy(panel = Panel.FICHA, fichaDe = "snes/1", ficha = ficha, pestanaFicha = 1), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t32_ficha_personalizar() = foto("32_ficha_personalizar", base().copy(panel = Panel.FICHA, fichaDe = "snes/1", ficha = ficha, pestanaFicha = 3, indice = 1), Rol.INFERIOR)

    @Test fun t33_tema_halloween() = foto("33_tema_halloween_arriba", base(), Rol.SUPERIOR, Temas.fiestas.first { it.id == "halloween" }.tema)

    @Config(qualifiers = ABAJO)
    @Test fun t34_tema_halloween_abajo() = foto("34_tema_halloween_abajo", base(), Rol.INFERIOR, Temas.fiestas.first { it.id == "halloween" }.tema)

    @Test fun t35_reloj_grande() = foto("35_reloj_grande", base().copy(relojGrande = true), Rol.SUPERIOR)

    @Test fun t36_trabajos() = foto(
        "36_trabajos_de_fondo",
        base().copy(trabajos = listOf(Trabajo("portadas", "Portadas · Bahía Neón", 0.4f), Trabajo("iconos", "Bajando íconos"))),
        Rol.SUPERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t37_menu_temas() = foto("37_menu_temas", base().copy(panel = Panel.MENU, seccion = Seccion.TEMAS, enOpciones = true, indice = 2), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t38_consolas_nuevas() = foto(
        "38_consolas_nuevas_siglas",
        base().copy(
            juegos = juegos + listOf(juego("9", "switch", "Aventura en la Isla"), juego("10", "xbox360", "Motor Rugiente"), juego("11", "psvita", "Pequeño Planeta"), juego("12", "n3ds", "Granja Feliz 3D")),
            estiloIconos = "siglas",
        ),
        Rol.INFERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t39_tema_sakura_claro() = foto("39_tema_sakura_claro", base().copy(oscuro = false), Rol.INFERIOR, Temas.incluidos.first { it.id == "sakura" })

    @Config(qualifiers = ABAJO)
    @Test fun t40_editar_widgets() = foto("40_editar_widgets", base().copy(inicio = "widgets", editandoWidgets = true, panel = Panel.ESTUDIO, estudioPestana = 1, indice = 2), Rol.INFERIOR)

    // ---------- v0.4 ----------

    @Config(qualifiers = ABAJO)
    @Test fun t41_cuadricula() = foto("41_vista_cuadricula", base().copy(vista = "cuadricula"), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t42_lista() = foto("42_vista_lista", base().copy(vista = "lista", juegoSel = "psx/3"), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t43_buscar() = foto("43_buscar", base().copy(panel = Panel.BUSCAR, busqueda = "ro"), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t44_centro() = foto("44_centro_de_control", base().copy(panel = Panel.CENTRO, indice = 1), Rol.INFERIOR)

    @Test fun t45_editar_widgets() = foto("45_acomodar_widgets", base().copy(inicio = "widgets", editandoWidgets = true, widgetSel = 1), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t46_formas() = foto(
        "46_cajas_disco_y_poster",
        base().copy(formas = mapOf("snes" to cl.nikalbazu.diptico.estado.FormaCfg("disco"), "gba" to cl.nikalbazu.diptico.estado.FormaCfg("poster"), "psx" to cl.nikalbazu.diptico.estado.FormaCfg("horizontal"))),
        Rol.INFERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t47_carpetas() = foto(
        "47_carpetas",
        base().copy(
            panel = Panel.CARPETAS,
            carpetas = listOf(
                cl.nikalbazu.diptico.data.CarpetaJuegos(Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AROMs"), null),
                cl.nikalbazu.diptico.data.CarpetaJuegos(Uri.parse("content://com.android.externalstorage.documents/tree/1234-ABCD%3AJuegos%20Switch"), "switch"),
            ),
        ),
        Rol.INFERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t48_menu_estante() = foto("48_menu_estante", base().copy(panel = Panel.MENU, seccion = Seccion.ESTANTE, enOpciones = true, consolaSel = "snes"), Rol.INFERIOR)

    @Test fun t49_una_pantalla_cuadricula() = foto("49_una_pantalla_cuadricula", base().copy(modoPantallas = "una_negra", vista = "cuadricula"), Rol.UNICA)

    @Config(qualifiers = ABAJO)
    @Test fun t24_letra_muy_grande() = foto("24_abajo_letra_muy_grande", base().copy(tamTexto = 1.3f), Rol.INFERIOR)

    // ---------- v0.5 ----------

    private fun conHoras() = base().copy(
        datos = mapOf(
            "snes/1" to DatosJuego(tiempo = 12 * 3600 + 25 * 60, sesiones = 18, ultimo = 1_730_000_000_000L),
            "psx/3" to DatosJuego(tiempo = 7 * 3600 + 10 * 60, sesiones = 9, ultimo = 1_729_800_000_000L),
            "n64/6" to DatosJuego(tiempo = 3 * 3600, sesiones = 6, ultimo = 1_729_500_000_000L),
            "gba/2" to DatosJuego(tiempo = 95 * 60, sesiones = 4, ultimo = 1_729_000_000_000L),
            "ps2/5" to DatosJuego(tiempo = 40 * 60, sesiones = 2, ultimo = 1_728_000_000_000L),
        ),
    )

    @Config(qualifiers = ABAJO)
    @Test fun t50_estadisticas() = foto("50_estadisticas", conHoras().copy(panel = Panel.ESTADISTICAS), Rol.INFERIOR)

    @Test fun t51_protector() = foto("51_protector_de_pantalla", base().copy(protector = true), Rol.SUPERIOR)

    @Test fun t52_teclado_arriba() = foto("52_teclado_en_la_otra_pantalla", base().copy(panel = Panel.PASO, borrador = "Me falta la llave del ", teclaFila = 2, teclaCol = 3, mayus = false), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t53_paso_abajo() = foto("53_escribiendo_abajo", base().copy(panel = Panel.PASO, borrador = "Me falta la llave del "), Rol.INFERIOR)

    @Test fun t54_previa_tema() = foto(
        "54_menu_vista_previa_tema",
        base().copy(panel = Panel.MENU, seccion = Seccion.TEMAS, enOpciones = true, indice = 2 + Temas.incluidos.indexOfFirst { it.id == "chile" }),
        Rol.SUPERIOR,
    )

    @Test fun t55_previa_estante() = foto("55_menu_vista_previa_estante", base().copy(panel = Panel.MENU, seccion = Seccion.ESTANTE, enOpciones = true, vista = "cuadricula"), Rol.SUPERIOR)

    @Test fun t56_previa_inicio() = foto("56_menu_vista_previa_inicio", base().copy(panel = Panel.MENU, seccion = Seccion.INICIO, enOpciones = true), Rol.SUPERIOR)

    @Test fun t57_previa_botones() = foto("57_menu_vista_previa_botones", base().copy(panel = Panel.MENU, seccion = Seccion.APARIENCIA, enOpciones = true, indice = 1, efecto = "vidrio"), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t58_tema_chile() = foto("58_tema_chile", base(), Rol.INFERIOR, Temas.incluidos.first { it.id == "chile" })

    @Test fun t59_tema_cyber() = foto("59_tema_cyberpunk", base(), Rol.SUPERIOR, Temas.incluidos.first { it.id == "cyber" })

    @Test fun t60_inicio_paginas() = foto(
        "60_inicio_pagina_2",
        conHoras().copy(
            inicio = "widgets", paginas = 3, paginaInicio = 1,
            widgets = Widgets.porDefecto + listOf(
                Widget("estadisticas", 0, 0, 3, 2, pagina = 1),
                Widget("accesos", 3, 0, 3, 1, pagina = 1),
                Widget("notificaciones", 3, 1, 3, 2, pagina = 1),
                Widget("sugerencia", 0, 2, 3, 2, pagina = 1),
            ),
        ),
        Rol.SUPERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t61_centro_nuevo() = foto("61_centro_de_control", base().copy(panel = Panel.CENTRO, indice = 2), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t62_elegir_imagen() = foto("62_elegir_portada", base().copy(panel = Panel.IMAGENES, fichaDe = "snes/1", candidatoPara = "portada", buscandoCandidatos = true), Rol.INFERIOR)

    @Test fun t63_previa_musica() = foto("63_menu_vista_previa_musica", base().copy(panel = Panel.MENU, seccion = Seccion.MUSICA, enOpciones = true), Rol.SUPERIOR)

    // ---------- v0.6 ----------

    private val conversacion = listOf(
        cl.nikalbazu.diptico.estado.MensajeChat("yo", "¿Qué juego me recomiendas hoy?"),
        cl.nikalbazu.diptico.estado.MensajeChat(
            "ia",
            "Tienes Leyenda del Bosque a medio camino: te faltaba la llave del templo del agua, y llevas más de 3 horas. Si prefieres algo corto, Pixel Kart 64 es perfecto para un par de carreras.",
            listOf(
                cl.nikalbazu.diptico.estado.AccionChat("jugar", "snes/1", "Seguir Leyenda del Bosque"),
                cl.nikalbazu.diptico.estado.AccionChat("ver", "n64/6", "Ver Pixel Kart 64"),
            ),
        ),
    )

    @Config(qualifiers = ABAJO)
    @Test fun t64_asistente() = foto("64_asistente", base().copy(panel = Panel.ASISTENTE, chat = conversacion, indice = 1, claveIa = "x"), Rol.INFERIOR)

    @Test fun t65_asistente_arriba() = foto("65_asistente_lectura_arriba", base().copy(panel = Panel.ASISTENTE, chat = conversacion, indice = 1, claveIa = "x"), Rol.SUPERIOR)

    private fun logro(id: Int, t: String, d: String, p: Int, g: String = "", duro: Boolean = false) =
        cl.nikalbazu.diptico.data.Logro(id, t, d, p, "0000$id", g, duro)

    private val logros = cl.nikalbazu.diptico.data.LogrosJuego(
        1, "Leyenda del Bosque", "",
        listOf(
            logro(1, "Primeros pasos", "Sal de la aldea por primera vez.", 5, "2026-10-20"),
            logro(2, "Guardián del agua", "Vence al jefe del templo del agua.", 25, "2026-10-28", true),
            logro(3, "Coleccionista", "Encuentra las 50 semillas doradas.", 50),
            logro(4, "Sin un rasguño", "Termina un templo sin recibir daño.", 25),
            logro(5, "Leyenda", "Termina el juego.", 100),
        ),
    )

    @Config(qualifiers = ABAJO)
    @Test fun t66_logros_ficha() = foto(
        "66_logros_en_la_ficha",
        base().copy(panel = Panel.FICHA, fichaDe = "snes/1", ficha = ficha, pestanaFicha = 4, usuarioRA = "nika", claveRA = "x", logrosFicha = logros, indice = 1),
        Rol.INFERIOR,
    )

    @Test fun t67_logro_grande() = foto(
        "67_logro_grande_arriba",
        base().copy(panel = Panel.FICHA, fichaDe = "snes/1", ficha = ficha, pestanaFicha = 4, usuarioRA = "nika", claveRA = "x", logrosFicha = logros, indice = 1),
        Rol.SUPERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t68_menu_idioma() = foto("68_menu_idioma", base().copy(panel = Panel.MENU, seccion = Seccion.IDIOMA, enOpciones = true, indice = 1), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t69_ingles_abajo() = foto("69_ingles_abajo", base().copy(idioma = "en"), Rol.INFERIOR)

    @Test fun t70_ingles_menu() = foto("70_ingles_menu_arriba", base().copy(idioma = "en", panel = Panel.MENU, seccion = Seccion.APARIENCIA, enOpciones = true, indice = 2), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t71_japones_menu() = foto("71_japones_menu", base().copy(idioma = "ja", panel = Panel.MENU, seccion = Seccion.PANTALLAS, enOpciones = true), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t72_chino_estadisticas() = foto("72_chino_estadisticas", conHoras().copy(idioma = "zh", panel = Panel.ESTADISTICAS), Rol.INFERIOR)

    @Test fun t73_portugues_inicio() = foto("73_portugues_inicio_widgets", conHoras().copy(idioma = "pt", inicio = "widgets"), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t74_menu_logros() = foto("74_menu_logros", base().copy(panel = Panel.MENU, seccion = Seccion.LOGROS, enOpciones = true), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t75_menu_secciones() = foto("75_menu_secciones", base().copy(panel = Panel.MENU, seccion = Seccion.ASISTENTE, enOpciones = false), Rol.INFERIOR)

    // ---------- v0.7 ----------

    @Test fun t76_teclado_mitad() = foto(
        "76_teclado_a_media_pantalla",
        base().copy(panel = Panel.PASO, borrador = "Me falta la llave del templo", cursor = 22, ancla = 12, teclaFila = 0, teclaCol = 4),
        Rol.SUPERIOR,
    )

    @Test fun t77_editar_widgets() = foto(
        "77_acomodar_widgets_libre",
        base().copy(
            inicio = "widgets", editandoWidgets = true, widgetSel = 1,
            widgets = cl.nikalbazu.diptico.estado.Widgets.porDefecto.mapIndexed { i, w -> if (i == 1) w.copy(estilo = "vidrio") else if (i == 2) w.copy(estilo = "solido") else w },
        ),
        Rol.SUPERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t78_tienda_imagenes() = foto(
        "78_tienda_de_imagenes",
        base().copy(
            panel = Panel.IMAGENES, fichaDe = "snes/1", candidatoPara = "portada", nombreImagen = "Leyenda del Bosque",
            candidatos = (1..9).map { cl.nikalbazu.diptico.data.ImagenWeb("https://ejemplo/$it.png", "https://ejemplo/$it.png", if (it % 3 == 0) "libretro" else "SteamGridDB") },
        ),
        Rol.INFERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t79_menu_nuevo() = foto("79_menu_ajustes_nuevo", base().copy(panel = Panel.MENU, seccion = Seccion.ESENCIAL, enOpciones = false), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t80_menu_buscar() = foto("80_menu_buscar_ajuste", base().copy(panel = Panel.MENU, seccion = Seccion.ESENCIAL, enOpciones = true, buscarAjuste = "música", indice = 1), Rol.INFERIOR)

    @Test fun t81_caratulas() = foto("81_inicio_caratulas", conHoras().copy(datos = conHoras().datos + ("snes/1" to DatosJuego(tiempo = 3 * 3600, sesiones = 7, ultimo = System.currentTimeMillis() - 2 * 86_400_000L, paso = "Buscar la llave del templo del agua", favorito = true))), Rol.SUPERIOR)

    @Test fun t82_centro_arriba() = foto("82_centro_de_control_arriba", base().copy(panel = Panel.CENTRO, indice = 1), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t83_bisa_en_juego() = foto(
        "83_bisa_ayuda_mientras_juegas",
        base().copy(
            sesion = Sesion("snes/1", System.currentTimeMillis() - 600_000, 0, false), panel = Panel.ASISTENTE, claveIa = "x",
            chat = listOf(
                cl.nikalbazu.diptico.estado.MensajeChat(
                    "ia", "Veo que llevas varios intentos seguidos en Leyenda del Bosque. ¡Tranqui, nos pasa a todos! ¿Te doy una pista suave o prefieres la solución?",
                    listOf(
                        cl.nikalbazu.diptico.estado.AccionChat("preguntar", "x", "Dame una pista suave"),
                        cl.nikalbazu.diptico.estado.AccionChat("preguntar", "y", "Me matan siempre en lo mismo"),
                    ),
                ),
            ),
            indice = 1,
        ),
        Rol.INFERIOR, mi = 1,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t84_companero_juego() = foto(
        "84_companero_con_bisa_y_lupa",
        base().copy(sesion = Sesion("snes/1", System.currentTimeMillis() - 600_000, 0, false), despierta = true),
        Rol.INFERIOR, mi = 1,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t85_menu_rapido_juego() = foto("85_opciones_del_juego", base().copy(panel = Panel.RAPIDO), Rol.INFERIOR)

    // ---------- v0.8 ----------

    @Config(qualifiers = ABAJO)
    @Test fun t86_companero() = foto(
        "86_companero_de_partida",
        base().copy(sesion = Sesion("snes/1", System.currentTimeMillis() - 1_500_000, 0, false), despierta = true),
        Rol.INFERIOR, mi = 1,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t87_menu_juego() = foto("87_menu_del_juego", base().copy(panel = Panel.RAPIDO, indice = 6), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t88_galeria_temas() = foto("88_galeria_de_temas", base().copy(panel = Panel.GALERIA, galeria = "temas", indice = 2), Rol.INFERIOR)

    @Test fun t89_galeria_temas_arriba() = foto("89_galeria_temas_arriba", base().copy(panel = Panel.GALERIA, galeria = "temas", galeriaPestana = 1, indice = 3), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t90_mis_colores() = foto("90_mis_colores", base().copy(panel = Panel.GALERIA, galeria = "colores", indice = 9), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t91_galeria_musica() = foto("91_galeria_de_musica", base().copy(panel = Panel.GALERIA, galeria = "musica", indice = 1), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t92_ajustes_widget() = foto(
        "92_ajustes_del_widget",
        base().copy(inicio = "widgets", editandoWidgets = true, widgetSel = 0, panel = Panel.ESTUDIO, estudioPestana = 0, indice = 2),
        Rol.INFERIOR,
    )

    @Test fun t93_widgets_personalizados() = foto(
        "93_widgets_personalizados",
        base().copy(
            inicio = "widgets", huecoInicio = 16,
            widgets = cl.nikalbazu.diptico.estado.Widgets.porDefecto.mapIndexed { i, w ->
                when (i) {
                    0 -> w.copy(estilo = "solido", color = 0xFF2EC4A6, radio = 34)
                    1 -> w.copy(estilo = "vidrio", radio = 6)
                    2 -> w.copy(estilo = "contorno", color = 0xFFFF5D8F)
                    else -> w.copy(opacidad = 0.5f)
                }
            },
        ),
        Rol.SUPERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t94_menu_bisa() = foto("94_menu_bisa_ia_local", base().copy(panel = Panel.MENU, seccion = Seccion.ASISTENTE, enOpciones = true, indice = 1), Rol.INFERIOR)

    // ---------- v0.9 ----------

    private val bloques get() = Temas.incluidos.first { it.id == "bloques" }
    private fun conRecientes(e: Estado) = e.copy(
        datos = e.datos + mapOf(
            "gba/2" to DatosJuego(tiempo = 5400, ultimo = 5L, favorito = true), "psx/3" to DatosJuego(tiempo = 1800, ultimo = 4L),
            "n64/6" to DatosJuego(tiempo = 900, ultimo = 3L, favorito = true),
        ),
    )

    @Config(qualifiers = ABAJO)
    @Test fun t96_estudio_estilo() = foto(
        "96_estudio_widget_estilo",
        base().copy(inicio = "widgets", editandoWidgets = true, widgetSel = 1, panel = Panel.ESTUDIO, estudioPestana = 0, indice = 6),
        Rol.INFERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t97_estudio_pagina() = foto(
        "97_estudio_pagina",
        base().copy(inicio = "widgets", editandoWidgets = true, panel = Panel.ESTUDIO, estudioPestana = 2, indice = 3),
        Rol.INFERIOR,
    )

    @Test fun t98_widgets_disenos() = foto(
        "98_widgets_disenos_nuevos",
        conRecientes(base()).copy(
            inicio = "widgets", editandoWidgets = true, widgetSel = 1, panel = Panel.ESTUDIO,
            widgets = listOf(
                Widget("juego", 0, 0, 3, 2, variante = 1), Widget("reloj", 3, 0, 2, 2, variante = 1, estilo = "vidrio"),
                Widget("bateria", 5, 0, 1, 2, variante = 1), Widget("recientes", 0, 2, 3, 2, variante = 1),
                Widget("favoritos", 3, 2, 3, 2, escala = 1.15f, estilo = "solido", color = 0xFF2EC4A6),
            ) + Widgets.porDefecto.filter { it.tipo !in setOf("juego", "reloj", "bateria", "recientes", "favoritos") }.map { it.copy(visible = false) },
        ),
        Rol.SUPERIOR,
    )

    @Test fun t98b_widgets_disenos_listos() = foto(
        "98b_widgets_disenos_terminados",
        conRecientes(base()).copy(
            inicio = "widgets",
            widgets = listOf(
                Widget("juego", 0, 0, 3, 2, variante = 1), Widget("reloj", 3, 0, 2, 2, variante = 2, estilo = "sinfondo"),
                Widget("bateria", 5, 0, 1, 2, variante = 1), Widget("recientes", 0, 2, 3, 2, variante = 1),
                Widget("musica", 3, 2, 3, 2, estilo = "vidrio"),
            ) + Widgets.porDefecto.filter { it.tipo !in setOf("juego", "reloj", "bateria", "recientes", "musica") }.map { it.copy(visible = false) },
        ),
        Rol.SUPERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t99_galeria_ia() = foto("99_elegir_ia_de_bisa", base().copy(panel = Panel.GALERIA, galeria = "ia", indice = 2, claveIa = "x"), Rol.INFERIOR)

    @Test fun t100_galeria_ia_arriba() = foto("100_ia_explicacion_arriba", base().copy(panel = Panel.GALERIA, galeria = "ia", indice = 2), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t101_bloques_abajo() = foto("101_tema_bloques_abajo", base(), Rol.INFERIOR, bloques)

    @Test fun t102_bloques_arriba() = foto("102_tema_bloques_arriba", base(), Rol.SUPERIOR, bloques)

    @Config(qualifiers = ABAJO)
    @Test fun t103_bloques_menu() = foto("103_tema_bloques_menu", base().copy(panel = Panel.MENU, seccion = Seccion.TEMAS, enOpciones = true, indice = 0), Rol.INFERIOR, bloques)

    @Test fun t103b_bloques_widgets() = foto("103b_tema_bloques_widgets", conRecientes(base()).copy(inicio = "widgets"), Rol.SUPERIOR, bloques)

    @Config(qualifiers = ABAJO)
    @Test fun t104_consolas_costado() = foto("104_consolas_al_costado", base().copy(vistaConsolas = "lado", consolaSel = "snes"), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t105_consolas_rueda() = foto("105_consolas_rueda", base().copy(vistaConsolas = "rueda", consolaSel = "psx", vista = "cuadricula"), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t106_sistemas() = foto("106_sistemas_carrusel", base().copy(vistaConsolas = "sistemas", enSistemas = true, consolaSel = "snes"), Rol.INFERIOR)

    @Test fun t107_sistemas_arriba() = foto("107_sistemas_arriba", conRecientes(base()).copy(vistaConsolas = "sistemas", enSistemas = true, consolaSel = "snes"), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t108_carrusel_juegos() = foto("108_juegos_en_carrusel", base().copy(vista = "carrusel", juegoSel = "psx/3", vistaConsolas = "sistemas", enSistemas = false), Rol.INFERIOR)

    @Test fun t109_bienvenida() = foto(
        "109_bienvenida_arriba",
        conRecientes(base()).copy(bienvenida = 1L, ausencia = 2 * 3_600_000L, nombre = "Nico"), Rol.SUPERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t110_bienvenida_abajo() = foto("110_bienvenida_abajo", conRecientes(base()).copy(bienvenida = 1L, ausencia = 2 * 3_600_000L, nombre = "Nico"), Rol.INFERIOR, mi = 1)

    @Config(qualifiers = ABAJO)
    @Test fun t111_continuacion() = foto(
        "111_continuacion_al_encender",
        base().copy(sesion = Sesion("snes/1", System.currentTimeMillis() - 2_520_000, 0, false), continuacion = 1L, despierta = true),
        Rol.INFERIOR, mi = 1,
    )

    @Test fun t112_estudio_una_pantalla() = foto(
        "112_estudio_una_pantalla",
        base().copy(modoPantallas = "una_negra", inicio = "widgets", editandoWidgets = true, panel = Panel.ESTUDIO, estudioPestana = 0, indice = 2),
        Rol.UNICA,
    )

    @Test fun t113_bienvenida_breve() = foto("113_bienvenida_breve", base().copy(bienvenida = 1L, bienvenidaBreve = true, nombre = "Nico"), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t114_menu_inicio() = foto("114_menu_inicio_bienvenida", base().copy(panel = Panel.MENU, seccion = Seccion.INICIO, enOpciones = true, indice = 0), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t115_menu_consola() = foto("115_menu_consola_inicio", base().copy(panel = Panel.MENU, seccion = Seccion.CONSOLA, enOpciones = true, indice = 3, esInicio = false), Rol.INFERIOR)

    @Test fun t116_sistemas_una_pantalla() = foto("116_sistemas_una_pantalla", base().copy(modoPantallas = "una_negra", vistaConsolas = "sistemas", enSistemas = true, consolaSel = "gba"), Rol.UNICA)

    // ---------- 1.0 beta ----------

    @Test fun t117_previa_estante() = foto("117_previa_ajuste_estante", base().copy(panel = Panel.MENU, seccion = Seccion.ESTANTE, enOpciones = true, indice = 0), Rol.SUPERIOR)

    @Test fun t118_previa_atenuacion() = foto("118_previa_atenuacion", base().copy(panel = Panel.MENU, seccion = Seccion.PANTALLAS, enOpciones = true, indice = 3, atenuacion = 0.12f), Rol.SUPERIOR)

    @Test fun t119_previa_bienvenida() = foto("119_previa_bienvenida", conRecientes(base()).copy(panel = Panel.MENU, seccion = Seccion.INICIO, enOpciones = true, indice = 0, nombre = "Nico"), Rol.SUPERIOR)

    @Test fun t120_previa_clave_gemini() = foto("120_previa_clave_gemini", base().copy(panel = Panel.MENU, seccion = Seccion.ASISTENTE, enOpciones = true, indice = 6), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t121_menu_agrupado() = foto("121_menu_bisa_agrupado", base().copy(panel = Panel.MENU, seccion = Seccion.ASISTENTE, enOpciones = true, indice = 3), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t122_clave_con_qr() = foto("122_clave_gemini_con_qr", base().copy(panel = Panel.CLAVE, textoPara = "ia_clave_gemini", tecladoPropio = false), Rol.INFERIOR)

    private fun conConsolaPropia(e: Estado): Estado {
        val m = mapOf("snes" to cl.nikalbazu.diptico.estado.ConsolaPropia(nombre = "Super Famicom", color = 0xFFE63946))
        cl.nikalbazu.diptico.data.Consolas.personalizar(m.mapValues { (_, v) -> v.nombre to v.color })
        return e.copy(consolasPropias = m)
    }

    @Config(qualifiers = ABAJO)
    @Test fun t123_editar_consola() = foto("123_personalizar_consola", conConsolaPropia(base()).copy(panel = Panel.EDITAR_CONSOLA, consolaEditando = "snes", indice = 1), Rol.INFERIOR)

    @Test fun t124_editar_consola_arriba() = foto("124_personalizar_consola_arriba", conConsolaPropia(conRecientes(base())).copy(panel = Panel.EDITAR_CONSOLA, consolaEditando = "snes", consolaSel = "snes"), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t125_menu_consolas() = foto("125_menu_tus_consolas", conConsolaPropia(base()).copy(panel = Panel.MENU, seccion = Seccion.CONSOLAS, enOpciones = true, indice = 0), Rol.INFERIOR)

    @Test fun t126_menu_una_pantalla() = foto("126_menu_una_pantalla_con_previa", base().copy(modoPantallas = "una_negra", panel = Panel.MENU, seccion = Seccion.TEMAS, enOpciones = true, indice = 0), Rol.UNICA)

    @Config(qualifiers = ABAJO)
    @Test fun t127_sistemas_propios() = foto("127_sistemas_consola_propia", conConsolaPropia(base()).copy(vistaConsolas = "sistemas", enSistemas = true, consolaSel = "snes"), Rol.INFERIOR)

    @Test fun t128_galeria_ia_qr() = foto("128_ia_con_qr_arriba", base().copy(panel = Panel.GALERIA, galeria = "ia", indice = 3), Rol.SUPERIOR)

    private val appsEjemplo = listOf(
        App("com.android.chrome", "Chrome"), App("com.spotify.music", "Spotify"), App("com.retroarch", "RetroArch"),
        App("org.ppsspp.ppsspp", "PPSSPP"), App("com.mihoyo.genshin", "Genshin Impact", true), App("com.android.settings", "Ajustes"),
        App("com.google.android.youtube", "YouTube"), App("com.whatsapp", "WhatsApp"), App("com.mojang.minecraftpe", "Minecraft", true),
        App("com.android.documentsui", "Archivos"), App("me.magnum.melonds", "melonDS"), App("com.android.camera", "Cámara"),
    )

    @Config(qualifiers = ABAJO)
    @Test fun t129_cajon_apps() = foto("129_cajon_de_apps", base().copy(panel = Panel.APPS, appsTodas = appsEjemplo, appsFijadas = listOf("com.spotify.music", "com.retroarch"), indice = 4), Rol.INFERIOR)

    @Test fun t130_cajon_apps_arriba() = foto("130_app_elegida_arriba", base().copy(panel = Panel.APPS, appsTodas = appsEjemplo, indice = 4), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t131_barra_nueva() = foto("131_barra_de_accesos", base(), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t132_barra_bloques() = foto("132_barra_tema_bloques", base().copy(vista = "cuadricula"), Rol.INFERIOR, bloques)

    // ---------- 1.0 beta.2 ----------

    private fun conWidgets(e: Estado) = e.copy(
        inicio = "widgets",
        widgets = cl.nikalbazu.diptico.estado.Widgets.porDefecto.map {
            when (it.tipo) {
                "musica" -> it.copy(visible = false)
                "asistente" -> it.copy(visible = true, x = 0, y = 2, an = 3, al = 2)
                else -> it
            }
        },
    )

    @Test fun t133_widgets_foco() = foto("133_widgets_con_foco", conWidgets(conRecientes(base())).copy(widgetFoco = 1), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t134_widget_reloj_abierto() = foto(
        "134_widget_reloj_abierto",
        conWidgets(base()).copy(panel = Panel.WIDGET, widgetAbierto = 1, widgetFoco = 1, temporizadorFin = System.currentTimeMillis() + 7 * 60_000L, cronoAcumulado = 95_000L, pausaCada = 60),
        Rol.INFERIOR,
    )

    @Test fun t135_widget_reloj_arriba() = foto(
        "135_widget_reloj_arriba",
        conWidgets(base()).copy(panel = Panel.WIDGET, widgetAbierto = 1, temporizadorFin = System.currentTimeMillis() + 7 * 60_000L),
        Rol.SUPERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t136_widget_bisa_abierto() = foto("136_widget_bisa_accesos", conWidgets(base()).copy(panel = Panel.WIDGET, widgetAbierto = 16, indice = 1), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t137_widget_recientes() = foto("137_widget_recientes_abierto", conWidgets(conRecientes(base())).copy(panel = Panel.WIDGET, widgetAbierto = 3), Rol.INFERIOR)

    @Test fun t138_widget_recientes_arriba() = foto("138_widget_recientes_arriba", conWidgets(conRecientes(base())).copy(panel = Panel.WIDGET, widgetAbierto = 3, indice = 0), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t139_encuadre() = foto(
        "139_encuadrar_portada",
        base().copy(panel = Panel.ENCUADRE, encuadreDe = "snes/1|portada", encuadreBorrador = cl.nikalbazu.diptico.estado.Encuadre(0.3f, -0.2f, 1.4f)),
        Rol.INFERIOR,
    )

    @Test fun t140_encuadre_arriba() = foto("140_encuadrar_arriba", base().copy(panel = Panel.ENCUADRE, encuadreDe = "snes/1|arte"), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t141_musica_en_juego() {
        cl.nikalbazu.diptico.audio.Medios.fijarParaPrueba(cl.nikalbazu.diptico.audio.Sonando("Spotify", "Canción de prueba", "Artista", null, true, "com.spotify.music"))
        foto("141_musica_mientras_juegas", base().copy(sesion = Sesion("snes/1", System.currentTimeMillis() - 1_500_000, 0, false), despierta = true), Rol.INFERIOR, mi = 1)
        cl.nikalbazu.diptico.audio.Medios.fijarParaPrueba(null)
    }

    @Test fun t142_menu_sobremesa() = foto("142_menu_sobremesa_previa", base().copy(panel = Panel.MENU, seccion = Seccion.SOBREMESA, enOpciones = true, indice = 3, sobremesaMargen = 5), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t143_menu_sobremesa_abajo() = foto("143_menu_sobremesa", base().copy(panel = Panel.MENU, seccion = Seccion.SOBREMESA, enOpciones = true, indice = 0), Rol.INFERIOR)

    @Test fun t144_tele() = foto("144_en_la_tele", base().copy(sobremesaModo = "si", sobremesaTam = 1.2f), Rol.UNICA)

    @Test fun t145_menu_botones() = foto("145_menu_botones_extra", base().copy(panel = Panel.MENU, seccion = Seccion.CONTROLES, enOpciones = true, indice = 0), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t146_menu_pc() = foto(
        "146_menu_juegos_de_pc",
        base().copy(panel = Panel.MENU, seccion = Seccion.BIBLIOTECA, enOpciones = true, indice = 7, resultadosSteam = listOf(cl.nikalbazu.diptico.estado.JuegoPC("Hollow Knight", 367520))),
        Rol.INFERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t147_ordenando() = foto("147_moviendo_un_juego", base().copy(ordenando = true, orden = "propio"), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t148_tienda_consola() = foto(
        "148_tienda_imagenes_consola",
        base().copy(panel = Panel.IMAGENES, consolaEditando = "snes", candidatoPara = "c_icono", nombreImagen = "Super Nintendo"),
        Rol.INFERIOR,
    )

    @Test fun t149_iconos_neon() = foto("149_iconos_neon", base().copy(estiloIconos = "neon"), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t150_iconos_sellos() = foto("150_iconos_sellos", base().copy(estiloIconos = "solido", vistaConsolas = "lado"), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t151_temas_galeria() = foto("151_galeria_temas_sin_saltos", base().copy(panel = Panel.GALERIA, galeria = "temas", indice = 7), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t152_editar_consola_lugar() = foto("152_personalizar_consola_lugar", base().copy(panel = Panel.EDITAR_CONSOLA, consolaEditando = "gba", indice = 1), Rol.INFERIOR)

    @Test fun t153_widget_bisa_inicio() = foto("153_widget_bisa_en_el_inicio", conWidgets(conRecientes(base())), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t154_menu_sonidos() = foto("154_menu_juego_de_sonidos", base().copy(panel = Panel.MENU, seccion = Seccion.MUSICA, enOpciones = true, indice = 4, paqueteSonidos = "retro"), Rol.INFERIOR)

    // ---------- 1.0 beta.3 ----------

    private fun conWidgetsVariados(e: Estado) = conWidgets(e).copy(
        widgets = conWidgets(e).widgets.map {
            when (it.tipo) {
                "reloj" -> it.copy(x = 4, y = 0, an = 1, al = 1)
                "bateria" -> it.copy(visible = true, x = 5, y = 0, an = 1, al = 1)
                "tiempo" -> it.copy(visible = true, x = 4, y = 1, an = 1, al = 1)
                "favoritos" -> it.copy(visible = true, x = 5, y = 1, an = 1, al = 1)
                else -> it
            }
        },
    )

    @Test fun t155_widgets_1x1() = foto("155_widgets_de_1x1", conWidgetsVariados(conRecientes(base())), Rol.SUPERIOR)

    @Test fun t156_editor_grilla() = foto(
        "156_editor_eligiendo_con_cruceta",
        conWidgetsVariados(conRecientes(base())).copy(editandoWidgets = true, panel = Panel.ESTUDIO, widgetSel = 3, estudioZona = "grilla"),
        Rol.SUPERIOR,
    )

    @Test fun t157_burbuja() = foto(
        "157_burbuja_para_agregar",
        conWidgetsVariados(conRecientes(base())).copy(editandoWidgets = true, panel = Panel.ESTUDIO, widgetSel = 3, burbuja = 2 to 1, burbujaSel = 2),
        Rol.SUPERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t158_estudio_ordenado() = foto(
        "158_estudio_reordenado",
        conWidgetsVariados(conRecientes(base())).copy(editandoWidgets = true, panel = Panel.ESTUDIO, widgetSel = 1, estudioZona = "panel", indice = 2),
        Rol.INFERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t159_estudio_zona_grilla() = foto(
        "159_estudio_zona_grilla",
        conWidgetsVariados(conRecientes(base())).copy(editandoWidgets = true, panel = Panel.ESTUDIO, widgetSel = 1, estudioZona = "grilla"),
        Rol.INFERIOR,
    )

    @Test fun t160_widget_arriba() = foto(
        "160_widget_abierto_arriba",
        conWidgets(base()).copy(panel = Panel.WIDGET, widgetAbierto = 1, temporizadorFin = System.currentTimeMillis() + 5 * 60_000L),
        Rol.SUPERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t161_cajas() = foto("161_galeria_de_cajas", base().copy(panel = Panel.GALERIA, galeria = "cajas", galeriaConsola = "snes", indice = 4), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t162_lomos() = foto("162_vista_lomos", conRecientes(base()).copy(vista = "lomos"), Rol.INFERIOR)

    @Test fun t163_lomos_una() = foto("163_vista_lomos_una_pantalla", conRecientes(base()).copy(vista = "lomos", modoPantallas = "una_negra"), Rol.UNICA)

    @Test fun t164_turbo() = foto("164_menu_rendimiento", base().copy(panel = Panel.MENU, seccion = Seccion.RENDIMIENTO, enOpciones = true, indice = 0, turboUltimo = "2140 MB libres (+380 MB)"), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t165_turbo_abajo() = foto("165_rendimiento_ayuda", base().copy(panel = Panel.MENU, seccion = Seccion.RENDIMIENTO, enOpciones = true, indice = 0, turboUltimo = "2140 MB libres (+380 MB)"), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t166_tienda_cuadradas() = foto(
        "166_tienda_con_cuadradas",
        base().copy(
            panel = Panel.IMAGENES, fichaDe = "snes/1", candidatoPara = "portada",
            candidatos = listOf(
                cl.nikalbazu.diptico.data.ImagenWeb("https://x/1.png", "https://x/1.png", "SteamGridDB", ancho = 600, alto = 900),
                cl.nikalbazu.diptico.data.ImagenWeb("https://x/2.png", "https://x/2.png", "SteamGridDB", ancho = 1024, alto = 1024),
                cl.nikalbazu.diptico.data.ImagenWeb("https://x/3.png", "https://x/3.png", "SteamGridDB", ancho = 512, alto = 512),
            ),
        ),
        Rol.INFERIOR,
    )

    @Test fun t167_recordatorio() = foto("167_widgets_con_recordatorio", conWidgets(conRecientes(base())).copy(avisoInicio = 1L), Rol.SUPERIOR)

    // ---------- 1.0 beta.4: todos los widgets en todos los tamaños ----------

    private fun muestrario(nombre: String, an: Int, al: Int, oscuro: Boolean = true, foco: Int = -1) {
        val e = conRecientes(base()).copy(oscuro = oscuro, avisoInicio = 0L)
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(e)
        Estilo.tema = Temas.incluidos[0]
        Estilo.decoraciones = false
        cl.nikalbazu.diptico.audio.Medios.fijarParaPrueba(cl.nikalbazu.diptico.audio.Sonando("Spotify", "Canción de prueba con nombre largo", "Artista", null, true, "com.spotify.music"))
        regla.mainClock.autoAdvance = false
        regla.setContent { Lienzo(e.oscuro, Modifier.fillMaxSize(), e.tamTexto) { cl.nikalbazu.diptico.ui.MuestrarioWidgets(e, an, al, foco) } }
        regla.mainClock.advanceTimeBy(1600)
        regla.onRoot().captureRoboImage("capturas/$nombre.png")
        cl.nikalbazu.diptico.audio.Medios.fijarParaPrueba(null)
    }

    @Config(qualifiers = MUESTRA) @Test fun t170_m_1x1() = muestrario("170_muestra_1x1", 1, 1, foco = 7)
    @Config(qualifiers = MUESTRA) @Test fun t171_m_1x2() = muestrario("171_muestra_1x2", 1, 2)
    @Config(qualifiers = MUESTRA) @Test fun t172_m_2x1() = muestrario("172_muestra_2x1", 2, 1)
    @Config(qualifiers = MUESTRA) @Test fun t173_m_2x2() = muestrario("173_muestra_2x2", 2, 2)
    @Config(qualifiers = MUESTRA) @Test fun t174_m_3x1() = muestrario("174_muestra_3x1", 3, 1)
    @Config(qualifiers = MUESTRA) @Test fun t175_m_3x2() = muestrario("175_muestra_3x2", 3, 2)
    @Config(qualifiers = MUESTRA_ALTA) @Test fun t176_m_2x3() = muestrario("176_muestra_2x3", 2, 3)
    @Config(qualifiers = MUESTRA_ALTA) @Test fun t177_m_4x2() = muestrario("177_muestra_4x2", 4, 2)
    @Config(qualifiers = MUESTRA_ALTA) @Test fun t178_m_3x3() = muestrario("178_muestra_3x3", 3, 3)
    @Config(qualifiers = MUESTRA_ALTA) @Test fun t179_m_6x1() = muestrario("179_muestra_6x1", 6, 1)
    @Config(qualifiers = MUESTRA_ALTA) @Test fun t180_m_6x2() = muestrario("180_muestra_6x2", 6, 2)
    @Config(qualifiers = MUESTRA_ALTA) @Test fun t181_m_4x3() = muestrario("181_muestra_4x3", 4, 3)
    @Config(qualifiers = MUESTRA) @Test fun t182_m_1x1_claro() = muestrario("182_muestra_1x1_claro", 1, 1, oscuro = false, foco = 7)
    @Config(qualifiers = MUESTRA) @Test fun t183_m_2x1_claro() = muestrario("183_muestra_2x1_claro", 2, 1, oscuro = false)
    @Config(qualifiers = MUESTRA) @Test fun t184_m_3x2_claro() = muestrario("184_muestra_3x2_claro", 3, 2, oscuro = false)
    @Config(qualifiers = MUESTRA_ALTA) @Test fun t185_m_4x2_claro() = muestrario("185_muestra_4x2_claro", 4, 2, oscuro = false)

    /** Como en la foto de la consola: tema claro, widgets de 1×1 y la batería con el foco. */
    @Test fun t186_foco_claro() {
        val e = conWidgetsVariados(conRecientes(base())).copy(oscuro = false)
        foto("186_foco_en_tema_claro", e.copy(widgetFoco = e.widgets.indexOfFirst { it.tipo == "bateria" }), Rol.SUPERIOR)
    }

    /** Un widget grande (ancho completo) con el foco: el marco tiene que verse entero. */
    @Test fun t187_foco_grande() {
        val e = conWidgets(conRecientes(base()))
        val l = e.widgets.map { if (it.tipo == "juego") it.copy(x = 0, y = 0, an = 6, al = 2) else if (it.tipo == "reloj") it.copy(visible = false) else it }
        foto("187_foco_widget_grande", e.copy(widgets = l, widgetFoco = l.indexOfFirst { it.tipo == "juego" }), Rol.SUPERIOR)
    }

    // ---------- 1.0 beta.4: nuevas ventanas ----------

    @Config(qualifiers = ABAJO)
    @Test fun t188_donde() = foto("188_donde_lo_abro", conRecientes(base()).copy(donde = "juego", dondeSel = 1, dondeRecordar = true), Rol.INFERIOR)

    @Test fun t189_donde_arriba() = foto("189_donde_marco_arriba", conRecientes(base()).copy(donde = "juego", dondeSel = 0), Rol.SUPERIOR)

    @Test fun t190_tour() = foto("190_tour_novedades", base().copy(novedades = 2), Rol.SUPERIOR)

    @Test fun t191_tour_una() = foto("191_tour_una_pantalla", base().copy(novedades = 0, modoPantallas = "una_negra"), Rol.UNICA)

    @Config(qualifiers = ABAJO)
    @Test fun t192_tour_abajo() = foto("192_tour_abajo", base().copy(novedades = 2), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t193_quedaste() = foto("193_donde_quedaste", conRecientes(base()).copy(quedaste = true, quedastePregunta = true, quedasteSel = 1), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t194_grabando() = foto("194_grabando_nota", conRecientes(base()).copy(quedaste = true, quedasteSel = 1, grabando = System.currentTimeMillis() - 12_000), Rol.INFERIOR)

    @Test fun t195_quedaste_una() = foto("195_quedaste_una_pantalla", conRecientes(base()).copy(quedaste = true, quedastePregunta = true, modoPantallas = "una_negra"), Rol.UNICA)

    private fun lectura(nombre: String, tipo: String, e0: Estado = conWidgets(conRecientes(base()))) {
        val i = e0.widgets.indexOfFirst { it.tipo == tipo }
        foto(nombre, e0.copy(panel = Panel.WIDGET, widgetAbierto = i, widgetFoco = i, indice = 0), Rol.SUPERIOR)
    }

    @Test fun t196_l_tiempo() = lectura("196_abierto_tiempo", "tiempo")
    @Test fun t197_l_estadisticas() = lectura("197_abierto_estadisticas", "estadisticas")
    @Test fun t198_l_coleccion() = lectura("198_abierto_coleccion", "consolas")
    @Test fun t199_l_bateria() = lectura("199_abierto_bateria", "bateria")
    @Test fun t200_l_avisos() = lectura("200_abierto_notificaciones", "notificaciones")
    @Test fun t201_l_musica() {
        cl.nikalbazu.diptico.audio.Medios.fijarParaPrueba(cl.nikalbazu.diptico.audio.Sonando("Spotify", "Canción de prueba", "Artista", null, true, "com.spotify.music"))
        lectura("201_abierto_musica", "musica")
        cl.nikalbazu.diptico.audio.Medios.fijarParaPrueba(null)
    }
    @Test fun t202_l_frase() = lectura("202_abierto_frase", "frase")
    @Test fun t203_l_fiesta() = lectura("203_abierto_fiestas", "fiesta")
    @Test fun t204_l_logros() = lectura("204_abierto_logros", "logros")

    @Config(qualifiers = ABAJO)
    @Test fun t205_todos_poster() = foto("205_todos_en_poster", conRecientes(base()).copy(consolaSel = "todo", formaTodos = "poster"), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t206_todos_cuadradas() = foto("206_todos_cuadradas", conRecientes(base()).copy(consolaSel = "todo", formaTodos = "cuadrada", vista = "cuadricula"), Rol.INFERIOR)

    @Test fun t207_inicio_portada() = foto("207_inicio_con_portada", base().copy(imagenHeroe = "portada"), Rol.SUPERIOR)

    @Test fun t208_menu_actualizar() = foto(
        "208_menu_actualizar",
        base().copy(panel = Panel.MENU, seccion = Seccion.SISTEMA, enOpciones = true, indice = 0,
            actualizacion = cl.nikalbazu.diptico.estado.VersionNueva(99, "1.0-beta.99", "https://x", 28_000_000, "Novedades de prueba"), descargaAct = 0.42f),
        Rol.SUPERIOR,
    )

    @Test fun t209_menu_sticks() = foto("209_menu_controles_sticks", base().copy(panel = Panel.MENU, seccion = Seccion.CONTROLES, enOpciones = true, indice = 0), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t210_ventana_juego() = foto("210_ventana_juego_imagen_inicio", conRecientes(base()).copy(panel = Panel.RAPIDO, indice = 8), Rol.INFERIOR)

    // ---------- 1.0 beta.5 ----------

    @Config(qualifiers = ABAJO)
    @Test fun t211_bisa_nueva() = foto("211_bisa_nueva", base().copy(panel = Panel.ASISTENTE, chat = conversacion, indice = 0, claveIa = "x"), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t212_bisa_vacia() = foto("212_bisa_vacia", base().copy(panel = Panel.ASISTENTE, indice = 0, claveIa = "x"), Rol.INFERIOR)

    @Test fun t213_teclado_grande() = foto("213_teclado_grande", base().copy(panel = Panel.CLAVE, textoPara = "asistente", borrador = "¿Cómo paso el templo del agua", teclaFila = 2, teclaCol = 3), Rol.SUPERIOR)

    @Test fun t214_teclado_una() = foto("214_teclado_una_pantalla", base().copy(panel = Panel.CLAVE, textoPara = "buscar_juego", borrador = "zel", modoPantallas = "una_negra", teclaFila = 5, teclaCol = 2), Rol.UNICA)

    @Config(qualifiers = ABAJO)
    @Test fun t215_carpetas() = foto("215_vista_carpetas", conRecientes(base()).copy(vistaConsolas = "carpetas", enSistemas = true, consolaSel = "snes"), Rol.INFERIOR)

    @Test fun t216_carpetas_una() = foto("216_carpetas_una_pantalla", conRecientes(base()).copy(vistaConsolas = "carpetas", enSistemas = true, consolaSel = "gba", modoPantallas = "una_negra"), Rol.UNICA)

    @Test fun t217_centro_activos() = foto("217_centro_accesos_activos", base().copy(panel = Panel.CENTRO, indice = 0, turbo = true), Rol.SUPERIOR)

    @Test fun t218_bienvenida_nueva() = foto("218_bienvenida_animada", conRecientes(base()).copy(bienvenida = 1L, ausencia = 2 * 3_600_000L, nombre = "Nico"), Rol.SUPERIOR)

    @Test fun t219_colecciones() {
        val e = conWidgets(conRecientes(base()))
        val l = e.widgets.map { if (it.tipo == "asistente" || it.tipo == "recientes") it.copy(visible = false) else it } +
            listOf(
                Widget("coleccion-a", 0, 2, 3, 2), Widget("atajo-gba/2", 3, 2, 1, 2, estilo = "sinfondo"), Widget("atajo-psx/3", 4, 2, 1, 2, estilo = "sinfondo"),
            )
        foto(
            "219_colecciones_y_accesos", e.copy(widgets = l, colecciones = mapOf("a" to cl.nikalbazu.diptico.estado.Coleccion("RPG pendientes", listOf("snes/1", "gba/2", "nds/4", "ps2/5")))),
            Rol.SUPERIOR,
        )
    }

    @Test fun t220_portada_botones() = foto("220_portada_botones_chicos", conRecientes(base()), Rol.SUPERIOR)

    @Test fun t221_menu_equipo() = foto("221_menu_tu_equipo", base().copy(panel = Panel.MENU, seccion = Seccion.PANTALLAS, enOpciones = true, indice = 0), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t222_colecciones_galeria() = foto(
        "222_galeria_colecciones",
        base().copy(panel = Panel.GALERIA, galeria = "colecciones", colecciones = mapOf("a" to cl.nikalbazu.diptico.estado.Coleccion("RPG pendientes", listOf("snes/1")))),
        Rol.INFERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t223_carrusel() = foto("223_carrusel", conRecientes(base()).copy(vista = "carrusel", juegoSel = "psx/3"), Rol.INFERIOR)

    @Config(qualifiers = MUESTRA_ALTA)
    @Test fun t224_menu_fuentes() = foto(
        "224_menu_portadas_fuentes",
        base().copy(panel = Panel.MENU, seccion = Seccion.PORTADAS, enOpciones = true, indice = 5,
            raspadores = cl.nikalbazu.diptico.data.Raspadores.Claves(igdbId = "abc123", igdbSecreto = "xyz789", rawg = "k1")),
        Rol.SUPERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t225_clave_igdb() = foto("225_clave_igdb", base().copy(panel = Panel.CLAVE, textoPara = "ras_igdbId"), Rol.INFERIOR)

    @Test fun t226_recorrido_inicio() = foto("226_recorrido_1", base().copy(novedades = 0, recorrido = true), Rol.SUPERIOR)
    @Test fun t227_recorrido_paso() = foto("227_recorrido_paso", base().copy(novedades = 3, recorrido = true), Rol.SUPERIOR)
    @Test fun t228_recorrido_final() = foto("228_recorrido_final", base().copy(novedades = 10, recorrido = true), Rol.SUPERIOR)
    @Test fun t229_novedades_b6() = foto("229_novedades_beta6", base().copy(novedades = 1), Rol.SUPERIOR)

    @Test fun t230_novedades_juntas() = foto("230_novedades_juntas_inicio", base().copy(novedades = 0, novedadesDesde = "1.0-beta.3"), Rol.SUPERIOR)
    @Test fun t231_novedades_juntas_vieja() = foto("231_novedades_juntas_beta4", base().copy(novedades = 9, novedadesDesde = "1.0-beta.3"), Rol.SUPERIOR)

    // ---------- Fotos en inglés para el README (prefijo "readme_") ----------

    @Test fun r01_arriba() = foto("readme_01_top", base().copy(idioma = "en"), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun r02_abajo() = foto("readme_02_bottom", base().copy(idioma = "en"), Rol.INFERIOR)

    @Test fun r03_widgets() = foto("readme_03_widgets", conHoras().copy(idioma = "en", inicio = "widgets"), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun r04_quedaste() = foto("readme_04_where_you_left_off", conRecientes(base()).copy(idioma = "en", quedaste = true, quedastePregunta = true, quedasteSel = 1), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun r05_carpetas() = foto("readme_05_folders", conRecientes(base()).copy(idioma = "en", vistaConsolas = "carpetas", enSistemas = true, consolaSel = "snes"), Rol.INFERIOR)

    @Test fun r06_halloween() = foto("readme_06_theme_top", base().copy(idioma = "en"), Rol.SUPERIOR, Temas.fiestas.first { it.id == "halloween" }.tema)

    @Config(qualifiers = ABAJO)
    @Test fun r07_halloween_abajo() = foto("readme_07_theme_bottom", base().copy(idioma = "en"), Rol.INFERIOR, Temas.fiestas.first { it.id == "halloween" }.tema)

    @Test fun r08_bienvenida() = foto("readme_08_welcome_back", conRecientes(base()).copy(idioma = "en", bienvenida = 1L, ausencia = 2 * 3_600_000L, nombre = "Nico"), Rol.SUPERIOR)

    @Test fun r09_donde() = foto("readme_09_pick_screen", conRecientes(base()).copy(idioma = "en", donde = "juego", dondeSel = 0), Rol.SUPERIOR)

    @Test fun r10_centro() = foto("readme_10_control_center", base().copy(idioma = "en", panel = Panel.CENTRO, indice = 0, turbo = true), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun r11_carrusel() = foto("readme_11_carousel", conRecientes(base()).copy(idioma = "en", vista = "carrusel", juegoSel = "psx/3"), Rol.INFERIOR)

    @Test fun r12_recorrido() = foto("readme_12_tour", base().copy(idioma = "en", novedades = 2, recorrido = true), Rol.SUPERIOR)

    @Test fun t95_tema_mis_colores() = foto(
        "95_interfaz_con_mis_colores", base(), Rol.SUPERIOR,
        Tema("mio", "Mi tema", 0xFF2EC4A6, 0xFFFFC93C, 0xFFFF5D8F, propio = true, fondo = 0xFF10243A, tinta = 0xFFE8F4FF),
    )
}

const val ARRIBA = "w731dp-h411dp-land-420dpi"
const val ABAJO = "w472dp-h411dp-land-420dpi"
const val MUESTRA = "w1600dp-h1100dp-land-160dpi"
const val MUESTRA_ALTA = "w1600dp-h2600dp-port-160dpi"

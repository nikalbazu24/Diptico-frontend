package cl.nikalbazu.diptico

import cl.nikalbazu.diptico.estado.*

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import cl.nikalbazu.diptico.estado.EstadoExtra
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

    private fun foto(nombre: String, e: Estado, rol: Rol, tema: Tema? = null, mi: Int = 0, avance: Long = 1600) {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(e)
        Estilo.tema = tema ?: Temas.incluidos[0]
        Estilo.decoraciones = true
        Estilo.efecto = if (e.experiencia.vidrio) "vidrio" else e.efecto
        cl.nikalbazu.diptico.estado.Idioma.fijar(ApplicationProvider.getApplicationContext(), e.idioma)
        regla.mainClock.autoAdvance = false
        regla.setContent {
            Lienzo(e.oscuro, Modifier.fillMaxSize(), e.tamTexto) { Raiz(e, rol, mi) }
        }
        regla.mainClock.advanceTimeBy(avance)
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
    @Test fun t64_asistente() = foto("64_asistente", base().copy(panel = Panel.ASISTENTE, chat = conversacion, indice = 1, claveGemini = "x"), Rol.INFERIOR)

    @Test fun t65_asistente_arriba() = foto("65_asistente_lectura_arriba", base().copy(panel = Panel.ASISTENTE, chat = conversacion, indice = 1, claveGemini = "x"), Rol.SUPERIOR)

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
            sesion = Sesion("snes/1", System.currentTimeMillis() - 600_000, 0, false), panel = Panel.ASISTENTE, claveGemini = "x",
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
    @Test fun t99_galeria_ia() = foto("99_elegir_ia_de_bisa", base().copy(panel = Panel.GALERIA, galeria = "ia", indice = 2, claveGemini = "x"), Rol.INFERIOR)

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
    @Test fun t211_bisa_nueva() = foto("211_bisa_nueva", base().copy(panel = Panel.ASISTENTE, chat = conversacion, indice = 0, claveGemini = "x"), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t212_bisa_vacia() = foto("212_bisa_vacia", base().copy(panel = Panel.ASISTENTE, indice = 0, claveGemini = "x"), Rol.INFERIOR)

    @Test fun t213_teclado_grande() = foto("213_teclado_grande", base().copy(panel = Panel.CLAVE, textoPara = "asistente", borrador = "¿Cómo paso el templo del agua", teclaFila = 2, teclaCol = 3), Rol.SUPERIOR)

    @Test fun t214_teclado_una() = foto("214_teclado_una_pantalla", base().copy(panel = Panel.CLAVE, textoPara = "buscar_juego", borrador = "zel", modoPantallas = "una_negra", teclaFila = 5, teclaCol = 2), Rol.UNICA)

    @Config(qualifiers = ABAJO)
    @Test fun t215_carpetas() = foto("215_vista_carpetas", conRecientes(base()).copy(vistaConsolas = "carpetas", enSistemas = true, consolaSel = "snes"), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun visual_carpetas_horizontal() = foto("carpetas_horizontal_zoom", base().copy(
        vistaConsolas = "carpetas", enSistemas = true, consolaSel = "todo",
        carpetasHorizontal = true, tamCarpetas = 1.6f), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun visual_carpeta_ventana() = foto("carpetas_ventana_inferior", base().copy(
        vistaConsolas = "carpetas", vista = "cuadricula", consolaSel = "snes", modoCarpeta = "ventana"), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun visual_decoraciones_desplegadas() = foto("decoraciones_desplegadas_inferior", base().copy(
        panel = Panel.GALERIA, galeria = "decoraciones", decoracionAbierta = "bloques", indice = 3), Rol.INFERIOR)

    @Test fun visual_carpetas_una_horizontal() = foto("carpetas_una_horizontal_accesos", base().copy(
        dosDisponibles = false, vistaConsolas = "carpetas", enSistemas = true, carpetasHorizontal = true, tamCarpetas = 1.6f,
    ), Rol.UNICA)

    @Test fun visual_carpeta_una_completa() = foto("carpeta_una_pantalla_completa", base().copy(
        dosDisponibles = false, vistaConsolas = "carpetas", modoCarpeta = "completa", vista = "cuadricula", consolaSel = "snes",
    ), Rol.UNICA)

    @Test fun visual_tienda_una_pantalla() {
        val catalogo = cl.nikalbazu.diptico.estado.TiendaTemas.incluidos(ApplicationProvider.getApplicationContext())
        foto("tienda_una_pantalla_previa", base().copy(
            dosDisponibles = false, panel = Panel.GALERIA, galeria = "tienda_temas", galeriaPestana = 1, catalogoTemas = catalogo,
        ), Rol.UNICA)
    }

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
    @Test fun t228_recorrido_final() = foto("228_recorrido_final", base().copy(novedades = 5, recorrido = true), Rol.SUPERIOR)
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

    // ---------- 1.0 beta.7 ----------

    private val fanGames = listOf(
        juego("pa", "rpgmaker", "Pokémon Añil").copy(archivo = "Pokemon Anil", esCarpeta = true),
        juego("pz", "rpgmaker", "Pokémon Zeta").copy(archivo = "Pokemon Zeta", esCarpeta = true),
        juego("ri", "rpgmaker", "Relatos de Invierno").copy(archivo = "Relatos"),
    )

    @Config(qualifiers = ABAJO)
    @Test fun t232_rpgmaker_estante() = foto("232_rpgmaker_estante", base().copy(juegos = juegos + fanGames, consolaSel = "rpgmaker", juegoSel = "rpgmaker/pa"), Rol.INFERIOR)

    @Test fun t233_rpgmaker_arriba() = foto("233_rpgmaker_arriba", base().copy(juegos = juegos + fanGames, consolaSel = "rpgmaker", juegoSel = "rpgmaker/pa"), Rol.SUPERIOR)

    private val dialogoBorrar = cl.nikalbazu.diptico.estado.Dialogo(
        "desinstalar-1", "¿Desinstalar Bahía Neón?",
        "Se borrará de tu consola y liberar 612 MB. También sus imágenes, notas y notas de voz de Díptico. Tus partidas guardadas del emulador se conservan.",
        cl.nikalbazu.diptico.estado.Icono.BORRAR, lineas = listOf("Bahía Neón.cue", "Bahía Neón (Track 1).bin", "Bahía Neón (Track 2).bin"), si = "Seguir", no = "Cancelar", peligro = true,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t234_desinstalar_1() = foto("234_desinstalar_paso1", base().copy(dialogo = dialogoBorrar), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t235_desinstalar_2() = foto(
        "235_desinstalar_mantener",
        base().copy(dialogo = dialogoBorrar.copy(id = "desinstalar-2", titulo = "Última confirmación", texto = "Mantén A hasta que se llene la barra para borrar Bahía Neón. Esto no se puede deshacer.", lineas = emptyList(), si = "Borrar", mantener = true), dialogoMantener = 1L),
        Rol.INFERIOR,
    )

    @Config(qualifiers = ABAJO)
    @Test fun t236_reporte_falla() = foto(
        "236_reporte_falla",
        base().copy(dialogo = cl.nikalbazu.diptico.estado.Dialogo("falla", "Díptico se cerró de golpe", "La última vez Díptico se cerró solo. ¿Mandas el reporte? Se abre una página de GitHub con el informe listo (versión, equipo y el error, sin datos personales): solo cuenta qué estabas haciendo y envíalo.", cl.nikalbazu.diptico.estado.Icono.INFO, si = "Enviar reporte", no = "Ahora no")),
        Rol.INFERIOR,
    )

    @Test fun t237_cortina_ajustes() = foto("237_cortina_ajustes", base().copy(panel = Panel.CENTRO, cortina = "ajustes", indice = 1), Rol.SUPERIOR)

    @Test fun t238_cortina_avisos() = foto("238_cortina_notificaciones", base().copy(panel = Panel.CENTRO, cortina = "avisos"), Rol.SUPERIOR)

    @Config(qualifiers = MUESTRA_ALTA)
    @Test fun t239_menu_acerca() = foto(
        "239_menu_acerca_respaldo", base().copy(panel = Panel.MENU, seccion = Seccion.SISTEMA, enOpciones = true, indice = 0, respaldoNombre = "Diptico-respaldo-2026-11-03.zip", respaldoUltimo = 1L),
        Rol.SUPERIOR,
    )

    @Config(qualifiers = MUESTRA_ALTA)
    @Test fun t240_menu_inicio() = foto("240_menu_inicio_al_abrir", base().copy(panel = Panel.MENU, seccion = Seccion.INICIO, enOpciones = true, indice = 0, arranque = "ultimo"), Rol.SUPERIOR)

    @Test fun t241_novedades_b7() = foto("241_novedades_beta7", base().copy(novedades = 1, novedadesDesde = "1.0-beta.6"), Rol.SUPERIOR)

    @Test fun t242_atajos() = foto(
        "242_widgets_accesos_directos",
        base().copy(inicio = "widgets", widgets = listOf(
            cl.nikalbazu.diptico.estado.Widget("atajo-snes/1", 0, 0, 2, 2, variante = 0),
            cl.nikalbazu.diptico.estado.Widget("atajo-gba/2", 2, 0, 2, 2, variante = 1),
            cl.nikalbazu.diptico.estado.Widget("atajo-psx/3", 4, 0, 2, 2, variante = 2),
            cl.nikalbazu.diptico.estado.Widget("reloj", 0, 2, 3, 2),
        )),
        Rol.SUPERIOR,
    )

    // ---------- 1.0 beta.8 ----------
    @Test fun t243_barra_visible() = foto("243_inicio_barra_flotante", conRecientes(base()).copy(barra = true), Rol.SUPERIOR)
    @Test fun t244_barra_escondida() = foto("244_inicio_barra_escondida", conRecientes(base()).copy(barra = false), Rol.SUPERIOR)
    @Test fun t245_abajo_sin_dock() = foto("245_abajo_sin_barra", conRecientes(base()), Rol.INFERIOR)
    @Test fun t246_widgets_barra() = foto("246_widgets_con_barra", base().copy(inicio = "widgets", barra = true), Rol.SUPERIOR)
    @Test fun t247_discos() = foto("247_vista_discos", conRecientes(base()).copy(vistaConsolas = "discos", enSistemas = true, consolaSel = "snes"), Rol.INFERIOR)
    @Test fun t248_discos_arriba() = foto("248_discos_arriba", conRecientes(base()).copy(vistaConsolas = "discos", enSistemas = true, consolaSel = "snes"), Rol.SUPERIOR)
    @Test fun t249_discos_una() = foto("249_discos_una_pantalla", conRecientes(base()).copy(vistaConsolas = "discos", enSistemas = true, consolaSel = "gba", modoPantallas = "una_negra"), Rol.UNICA)
    @Test fun t251_menu_simple() = foto("251_menu_simple_mas_ajustes", base().copy(panel = Panel.MENU, seccion = Seccion.MAS, enOpciones = true, indice = 2, menuSimple = true), Rol.SUPERIOR)
    @Test fun t252_menu_simple_esencial() = foto("252_menu_simple_esencial", base().copy(panel = Panel.MENU, seccion = Seccion.ESENCIAL, enOpciones = false, menuSimple = true), Rol.SUPERIOR)
    @Test fun t253_menu_completo_grupos() = foto("253_menu_completo_apariencia", base().copy(panel = Panel.MENU, seccion = Seccion.APARIENCIA, enOpciones = true, indice = 0, menuSimple = false), Rol.SUPERIOR)
    @Test fun t254_menu_musica_grupos() = foto("254_menu_musica_agrupado", base().copy(panel = Panel.MENU, seccion = Seccion.MUSICA, enOpciones = true, indice = 3), Rol.SUPERIOR)
    @Test fun t255_menu_acerca_grupos() = foto("255_menu_acerca_agrupado", base().copy(panel = Panel.MENU, seccion = Seccion.SISTEMA, enOpciones = true, indice = 0), Rol.SUPERIOR)
    @Test fun t256_ventana_juego_ayuda() = foto("256_ventana_juego_con_ayuda", conRecientes(base()).copy(panel = Panel.RAPIDO, indice = 1), Rol.INFERIOR)
    @Test fun t257_novedades_b8() = foto("257_novedades_beta8", base().copy(novedades = 1, novedadesDesde = "1.0-beta.7"), Rol.SUPERIOR)
    // ---------- v1.2 · Orden y base ----------
    @Test fun t259_desplegable_glass() = foto("259_desplegable_glass", base().copy(panel = Panel.MENU, seccion = Seccion.APARIENCIA, enOpciones = true, indice = 0, extra = EstadoExtra(desplegable = 0, desplegableSel = 2)), Rol.SUPERIOR)
    @Test fun t260_desplegable_abajo() = foto("260_desplegable_abajo", base().copy(panel = Panel.MENU, seccion = Seccion.APARIENCIA, enOpciones = true, indice = 0, extra = EstadoExtra(desplegable = 0, desplegableSel = 1)), Rol.INFERIOR)
    @Test fun t261_barra_escondida() = foto("261_barra_escondida", conRecientes(base()).copy(barraFija = false, barra = false), Rol.SUPERIOR)
    @Test fun t262_barras_difusas_arriba() = foto("262_barras_difusas_arriba", conRecientes(base()).copy(extra = EstadoExtra(estiloBarraArriba = "difusa")), Rol.SUPERIOR)
    @Test fun t263_barras_transparentes_arriba() = foto("263_barras_transparentes_arriba", conRecientes(base()).copy(extra = EstadoExtra(estiloBarraArriba = "transparente")), Rol.SUPERIOR)
    @Test fun t264_galeria_accesos() = foto("264_accesos_cortina", base().copy(panel = Panel.GALERIA, galeria = "accesos_cortina", extra = EstadoExtra(accesosCentro = listOf("Turbo", "Wi-Fi", "DIPPO")), indice = 0), Rol.INFERIOR)
    @Test fun t265_cortina_elegida() = foto("265_cortina_con_accesos_elegidos", base().copy(panel = Panel.CENTRO, cortina = "ajustes", extra = EstadoExtra(accesosCentro = listOf("Turbo", "Wi-Fi", "DIPPO"))), Rol.SUPERIOR)
    @Test fun t266_menu_musica_vibracion() = foto("266_menu_musica_vibracion", base().copy(panel = Panel.MENU, seccion = Seccion.MUSICA, enOpciones = true, indice = 0), Rol.SUPERIOR)
    @Test fun t271_menu_apariencia_movimiento() = foto("271_menu_apariencia_movimiento", base().copy(panel = Panel.MENU, seccion = Seccion.APARIENCIA, enOpciones = true, indice = 0), Rol.SUPERIOR)
    @Test fun t272_barra_flotante_arriba() = foto("272_barra_flotante_arriba", base().copy(extra = EstadoExtra(coloresBarra = "vivos")), Rol.SUPERIOR)
    @Test fun t273_barra_flotante_neon() = foto("273_barra_flotante_neon_abajo", base().copy(extra = EstadoExtra(coloresBarra = "neon", estiloBarraAbajo = "difusa")), Rol.INFERIOR)
    @Test fun t274_barra_completa() = foto("274_barra_completa_pastel", base().copy(extra = EstadoExtra(formaBarra = "completa", coloresBarra = "pastel")), Rol.SUPERIOR)
    @Test fun t275_galeria_artes() = foto("275_galeria_artes", base().copy(panel = Panel.GALERIA, galeria = "artes", galeriaPestana = 1, indice = 0), Rol.INFERIOR)
    @Test fun t276_galeria_artes_grande() = foto("276_galeria_artes_en_grande", base().copy(panel = Panel.GALERIA, galeria = "artes", galeriaPestana = 1, indice = 0), Rol.SUPERIOR)
    @Test fun t277_menu_modo_dia_noche() = foto("277_menu_apariencia_modo", base().copy(panel = Panel.MENU, seccion = Seccion.APARIENCIA, enOpciones = true, indice = 3), Rol.SUPERIOR)
    @Test fun t278_mosaicos() = foto("278_vista_mosaicos", base().copy(vista = "mosaicos"), Rol.INFERIOR)
    @Test fun t279_mosaicos_segundo() = foto("279_vista_mosaicos_elegido", base().let { b -> b.copy(vista = "mosaicos", juegoSel = b.visibles[3].clave) }, Rol.INFERIOR)
    @Test fun t280_vitrina_arriba() = foto("280_vitrina_arriba", base().copy(vista = "mosaicos"), Rol.SUPERIOR)
    @Test fun t281_ajustes_nuevos() = foto("281_ajustes_seis_secciones", base().copy(vista = "mosaicos", panel = Panel.MENU, seccion = Seccion.G_ESTILO, enOpciones = true), Rol.SUPERIOR)
    @Test fun t282_iconos_desplegable() = foto("282_iconos_desplegable", base().copy(vista = "mosaicos", panel = Panel.MENU, seccion = Seccion.G_ESTILO, enOpciones = true, indice = 8, extra = EstadoExtra(desplegable = 8, desplegableSel = 1)), Rol.SUPERIOR)
    @Test fun t283_ajustes_widgets() = foto("283_ajustes_inicio_widgets", base().copy(vista = "mosaicos", panel = Panel.MENU, seccion = Seccion.G_WIDGETS, enOpciones = true), Rol.SUPERIOR)
    @Test fun t284_tienda_temas() = foto("284_tienda_temas", base().copy(panel = Panel.GALERIA, galeria = "tienda_temas"), Rol.SUPERIOR)
    @Test fun t285_dippo_novedades() = foto("285_dippo_bienvenida", base().copy(vista = "mosaicos", panel = Panel.MENU, seccion = Seccion.G_SISTEMA, enOpciones = true), Rol.SUPERIOR)
    @Test fun t286_dippo_grande() {
        regla.setContent { Lienzo(true, Modifier.fillMaxSize(), 1f) { androidx.compose.foundation.layout.Row {
            cl.nikalbazu.diptico.bisa.AvatarBisa(120f); cl.nikalbazu.diptico.bisa.AvatarBisa(120f, cara = cl.nikalbazu.diptico.ui.CaraDippo.FELIZ)
            cl.nikalbazu.diptico.bisa.AvatarBisa(120f, cara = cl.nikalbazu.diptico.ui.CaraDippo.PENSANDO); cl.nikalbazu.diptico.bisa.AvatarBisa(120f, cara = cl.nikalbazu.diptico.ui.CaraDippo.SORPRENDIDO)
        } } }
        regla.onRoot().captureRoboImage("capturas/286_dippo_avatar.png")
    }
    @Test fun t287_dippo_franja() = foto("287_dippo_franja_abajo", base().copy(vista = "mosaicos"), Rol.INFERIOR)
    @Test fun t288_dippo_una_pantalla() = foto("288_dippo_una_pantalla", base().copy(vista = "mosaicos", modoPantallas = "una_negra"), Rol.UNICA)
    @Test fun t289_dippo_ajustes() = foto("289_dippo_ajustes", base().copy(vista = "mosaicos", panel = Panel.MENU, seccion = Seccion.G_DIPPO, enOpciones = true), Rol.SUPERIOR)
    @Test fun t291_teclado_gboard() = foto("291_teclado_sugerencias", base().copy(panel = Panel.CLAVE, textoPara = "asistente", borrador = "Dame una pis", teclaFila = 2, teclaCol = 3), Rol.SUPERIOR)
    @Test fun t292_teclado_herramientas() = foto("292_teclado_herramientas", base().copy(panel = Panel.CLAVE, textoPara = "notas", borrador = "La llave está en el templo", teclaFila = 0, teclaCol = 3, extra = EstadoExtra(tecladoEdicion = true)), Rol.SUPERIOR)
    @Test fun t293_teclado_buscar() = foto("293_teclado_buscar_juego", base().copy(panel = Panel.CLAVE, textoPara = "buscar_juego", borrador = "ley", simbolos = false), Rol.INFERIOR)
    @Test fun t294_dippo_habla_primero() = foto("294_dippo_habla_primero", base().let { b -> b.copy(panel = Panel.ASISTENTE, chat = listOf(cl.nikalbazu.diptico.estado.CentroBisa.saludoInicial(b)), indice = 2) }, Rol.INFERIOR)
    @Test fun t295_logros_dippo() = foto("295_logros_dippo", base().copy(panel = Panel.GALERIA, galeria = "logros_dippo"), Rol.INFERIOR)
    @Test fun t296_vitrina_cuanto_dura() = foto("296_vitrina_cuanto_dura", base().let { b -> b.copy(vista = "mosaicos", extra = EstadoExtra(duraciones = mapOf(b.seleccionado!!.clave to cl.nikalbazu.diptico.estado.CuantoDura.Duracion(12f, 18.5f, 31f, 1L)))) }, Rol.SUPERIOR)
    @Test fun t297_dippo_celebra() = foto("297_dippo_celebra", base().copy(vista = "mosaicos", extra = EstadoExtra(dippoCelebra = System.currentTimeMillis(), dippoLogro = "Semana perfecta")), Rol.INFERIOR)
    @Test fun t299_logros_dippo_arriba() = foto("299_logros_dippo_arriba", base().copy(panel = Panel.GALERIA, galeria = "logros_dippo", indice = 0), Rol.SUPERIOR)
    @Test fun t300_widgets_inicio_nuevo() = foto("300_widgets_inicio_nuevo", base().let { b -> b.copy(vista = "mosaicos", inicio = "widgets", widgets = cl.nikalbazu.diptico.estado.Widgets.inicioNuevo, paginas = 1, extra = EstadoExtra(duraciones = mapOf((b.seleccionado?.clave ?: "") to cl.nikalbazu.diptico.estado.CuantoDura.Duracion(12f, 18f, 30f, 1L)))) }, Rol.SUPERIOR)
    @Test fun t301_franja_con_dock() = foto("301_franja_con_dock", base().copy(vista = "mosaicos", appsFijadas = listOf("com.android.settings", "com.android.chrome", "org.ppsspp.ppsspp")), Rol.INFERIOR)
    @Test fun t302_jugando_con_dock() = foto("302_jugando_con_dock", base().copy(appsFijadas = listOf("com.android.settings", "com.android.chrome"), sesion = Sesion("snes/1", System.currentTimeMillis() - 1_500_000, 0, false), despierta = true), Rol.INFERIOR, mi = 1)
    @Test fun t305_vitrina_cajas() = foto("305_vitrina_cajas", base().copy(vista = "mosaicos"), Rol.SUPERIOR)
    @Test fun t306_vitrina_cajas_psx() = foto("306_vitrina_cajas_psx", base().copy(vista = "mosaicos", juegoSel = "psx/3"), Rol.SUPERIOR)
    @Test fun t310_vitrina_sin_giro() { cl.nikalbazu.diptico.ui.vitrinaSinGiro = true; try { foto("310_vitrina_sin_giro", base().copy(vista = "mosaicos", juegoSel = "psx/3"), Rol.SUPERIOR) } finally { cl.nikalbazu.diptico.ui.vitrinaSinGiro = false } }
    private val juegosVarios = listOf(
        juego("1", "snes", "Leyenda del Bosque"), juego("2", "psx", "Bahía Neón"), juego("3", "ps2", "Caballero de Papel"),
        juego("4", "switch", "Isla Brillante"), juego("5", "nes", "Torre Pixel"), juego("6", "genesis", "Rayo Azul"),
        juego("7", "n64", "Pixel Kart 64"), juego("8", "psp", "Isla Tormenta"), juego("9", "gba", "Cohete Rojo"), juego("10", "dreamcast", "Ola Neón"),
    )
    @Test fun t311_estuches_ps2() = foto("311_estuches_ps2", base().copy(vista = "mosaicos", juegos = juegosVarios, juegoSel = "ps2/3"), Rol.SUPERIOR)
    @Test fun t312_estuches_nes() = foto("312_estuches_nes", base().copy(vista = "mosaicos", juegos = juegosVarios, juegoSel = "genesis/6"), Rol.SUPERIOR)
    @Test fun t313_estuches_psp() = foto("313_estuches_psp", base().copy(vista = "mosaicos", juegos = juegosVarios, juegoSel = "psp/8"), Rol.SUPERIOR)
    @Config(qualifiers = ABAJO)
    @Test fun t314_franja_dock_escondido() = foto("314_franja_dock_escondido", base().copy(vista = "mosaicos", appsFijadas = listOf("com.android.settings"), extra = EstadoExtra(dockVisible = false)), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t315_jugando_logo() = foto("315_jugando_logo", base().copy(sesion = Sesion("snes/1", System.currentTimeMillis() - 1_500_000, 0, false), despierta = true, extra = EstadoExtra(duraciones = mapOf("snes/1" to cl.nikalbazu.diptico.estado.CuantoDura.Duracion(12f, 18f, 30f, 1L)))), Rol.INFERIOR, mi = 1)
    @Test fun t316_ajustes_discord() = foto("316_ajustes_discord", base().copy(vista = "mosaicos", panel = Panel.MENU, seccion = Seccion.G_CUENTAS, enOpciones = true, extra = EstadoExtra(discordWebhook = "https://discord.com/api/webhooks/1/abc")), Rol.SUPERIOR)
    @Config(qualifiers = ABAJO)
    @Test fun t317_menu_intercambiado() = foto("317_menu_intercambiado", base().copy(vista = "mosaicos", intercambiar = true, panel = Panel.MENU), Rol.INFERIOR, mi = 0)
    @Test fun t318_discord_qr() = foto("318_discord_qr", base().copy(vista = "mosaicos", panel = Panel.MENU, seccion = Seccion.G_CUENTAS, enOpciones = true,
        extra = EstadoExtra(discordLogin = cl.nikalbazu.diptico.estado.LoginDiscord("ABCD-EFGH", "https://discord.com/activate", "https://discord.com/activate?user_code=ABCDEFGH", System.currentTimeMillis() + 240_000))), Rol.SUPERIOR)
    @Test fun t319_cuentas() = foto("319_cuentas", base().copy(vista = "mosaicos", panel = Panel.MENU, seccion = Seccion.G_CUENTAS, enOpciones = true,
        extra = EstadoExtra(discordUsuario = "Nico")), Rol.SUPERIOR)
    @Test fun t320_estuches_integrales() = foto("320_estuches_integrales", base().copy(vista = "mosaicos", juegos = juegosVarios, juegoSel = "snes/1"), Rol.SUPERIOR)
    private fun fotoGiro(nombre: String, grados: Float, sel: String) {
        cl.nikalbazu.diptico.ui.vitrinaSinGiro = true
        cl.nikalbazu.diptico.ui.vitrinaGiroPrueba = grados
        try {
            foto(nombre, base().copy(vista = "mosaicos", juegos = juegosVarios, juegoSel = sel,
                datos = mapOf(sel to DatosJuego(tiempo = 5 * 3600 + 12 * 60, ultimo = 1L)),
                extra = EstadoExtra(duraciones = mapOf(sel to cl.nikalbazu.diptico.estado.CuantoDura.Duracion(14f, 20f, 35f, 1L)))), Rol.SUPERIOR)
        } finally { cl.nikalbazu.diptico.ui.vitrinaSinGiro = false; cl.nikalbazu.diptico.ui.vitrinaGiroPrueba = 0f }
    }
    @Test fun t321_caja3d_frente() = fotoGiro("321_caja3d_frente", 0f, "ps2/3")
    @Test fun t322_caja3d_tres_cuartos() = fotoGiro("322_caja3d_tres_cuartos", 38f, "snes/1")
    @Test fun t323_caja3d_lomo() = fotoGiro("323_caja3d_lomo", 82f, "ps2/3")
    @Test fun t324_caja3d_reverso() = fotoGiro("324_caja3d_reverso", 180f, "snes/1")
    @Test fun t325_caja3d_reverso_dvd() = fotoGiro("325_caja3d_reverso_dvd", 160f, "genesis/6")
    @Config(qualifiers = ABAJO)
    @Test fun t326_clave_discord() = foto("326_clave_discord", base().copy(vista = "mosaicos", panel = Panel.CLAVE, textoPara = "discord_app"), Rol.INFERIOR, mi = 1)
    @Test fun t327_opciones_juego_bajando() = foto("327_opciones_juego_bajando", base().copy(vista = "mosaicos", panel = Panel.RAPIDO, indice = 9), Rol.INFERIOR, mi = 1)
    @Test fun t328_clave_secreta_discord() = foto("328_clave_secreta_discord", base().copy(vista = "mosaicos", panel = Panel.CLAVE, textoPara = "discord_secreto"), Rol.INFERIOR, mi = 1)
    @Test fun t329_estante_abajo() = foto("329_estante_abajo", base().copy(vista = "mosaicos", juegos = juegosVarios, juegoSel = "ps2/3"), Rol.INFERIOR, mi = 1)
    @Test fun t330_ficha_bajando() = foto("330_ficha_bajando", base().copy(vista = "mosaicos", panel = Panel.FICHA, fichaDe = "snes/1", pestanaFicha = 3, indice = 4), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t331_amigos_sin_conectar() = foto("331_amigos_sin_conectar", base().copy(vista = "mosaicos", extra = EstadoExtra(amigosEn = 1)), Rol.INFERIOR, mi = 1)
    @Test fun t332_amigos_conectado() {
        val s = cl.nikalbazu.diptico.estado.SocialDiscord
        s.disponible = true
        s.publicar(listOf(
            cl.nikalbazu.diptico.estado.AmigoDiscord("1", "Cata", estado = "jugando", actividad = "Leyenda del Bosque", detalle = "Templo del agua", enDiptico = true),
            cl.nikalbazu.diptico.estado.AmigoDiscord("2", "Benja", estado = "jugando", actividad = "Pixel Kart 64"),
            cl.nikalbazu.diptico.estado.AmigoDiscord("3", "Javi", estado = "conectado"),
            cl.nikalbazu.diptico.estado.AmigoDiscord("4", "Fran", estado = "ausente"),
            cl.nikalbazu.diptico.estado.AmigoDiscord("5", "Tomi", estado = "desconectado"),
        ))
        try { foto("332_amigos_conectado", base().copy(vista = "mosaicos", extra = EstadoExtra(amigosEn = 0, discordUsuario = "Nico")), Rol.SUPERIOR, mi = 0) }
        finally { s.disponible = false; s.publicar(emptyList()) }
    }
    @Test fun t333_discord_qr_con_kit() {
        val s = cl.nikalbazu.diptico.estado.SocialDiscord
        s.disponible = true
        try { foto("333_discord_qr_con_kit", base().copy(vista = "mosaicos", extra = EstadoExtra(discordLogin = cl.nikalbazu.diptico.estado.LoginDiscord(
            "ABCD-1234", "https://discord.com/activate", "https://discord.com/activate?user_code=ABCD1234", System.currentTimeMillis() + 300_000))), Rol.SUPERIOR) }
        finally { s.disponible = false }
    }
    // ---------- Perfil de usuario ----------
    private fun conPerfil(abierto: Int = -1, personaje: String = "gato", color: Long = 0xFFFF7A59, extra: EstadoExtra = EstadoExtra()): Estado = base().copy(
        vista = "mosaicos", nombre = "Nico", juegos = juegosVarios,
        datos = mapOf(
            "snes/1" to DatosJuego(tiempo = 84 * 3600L, ultimo = 1L, favorito = true),
            "ps2/3" to DatosJuego(tiempo = 51 * 3600L + 600, ultimo = 1L, favorito = true),
            "n64/7" to DatosJuego(tiempo = 38 * 3600L, ultimo = 1L, favorito = true),
            "genesis/6" to DatosJuego(tiempo = 12 * 3600L, ultimo = 1L),
        ),
        extra = extra.copy(
            perfilEn = abierto, perfilOx = 0.93f, perfilOy = 0.07f, discordUsuario = "Nico",
            perfil = cl.nikalbazu.diptico.estado.Perfil(apodo = "nikalbazu", nacimiento = "1992-03-12", personaje = personaje, color = color, desde = 1_793_000_000_000L),
        ),
    )
    @Test fun t334_perfil_tarjeta_arriba() = foto("334_perfil_tarjeta_arriba", conPerfil(abierto = 0), Rol.SUPERIOR)
    @Config(qualifiers = ABAJO)
    @Test fun t335_perfil_tarjeta_abajo() = foto("335_perfil_tarjeta_abajo", conPerfil(abierto = 1, personaje = "zorro", color = 0xFF23C486), Rol.INFERIOR, mi = 1)
    @Test fun t336_perfil_zoom_a_medias() {
        val cerrado = conPerfil(abierto = -1, personaje = "robot", color = 0xFF5865F2)
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(cerrado)
        Estilo.tema = Temas.incluidos[0]
        cl.nikalbazu.diptico.estado.Idioma.fijar(ApplicationProvider.getApplicationContext(), cerrado.idioma)
        val actual = androidx.compose.runtime.mutableStateOf(cerrado)
        regla.mainClock.autoAdvance = false
        regla.setContent { Lienzo(true, Modifier.fillMaxSize(), 1f) { Raiz(actual.value, Rol.SUPERIOR, 0) } }
        regla.mainClock.advanceTimeBy(1600)
        actual.value = cerrado.copy(extra = cerrado.extra.copy(perfilEn = 0))
        regla.mainClock.advanceTimeBy(140)
        regla.onRoot().captureRoboImage("capturas/336_perfil_zoom_a_medias.png")
    }
    @Config(qualifiers = ABAJO)
    @Test fun t341_perfil_galeria_personajes_abajo() = foto("341_perfil_galeria_personajes_abajo", conPerfil(personaje = "zorro", color = 0xFF9B6BFF).copy(panel = Panel.GALERIA, galeria = "personajes", indice = 2), Rol.INFERIOR, mi = 1)
    @Test fun t342_perfil_marcos_galeria() = foto("342_perfil_marcos_galeria", conPerfil(personaje = "gato", color = 0xFF9B6BFF).copy(panel = Panel.GALERIA, galeria = "marcos", indice = 3), Rol.SUPERIOR)
    @Test fun t343_perfil_marcos_hoja() {
        regla.setContent { Lienzo(true, Modifier.fillMaxSize(), 1f) { androidx.compose.foundation.layout.Column {
            for (fila in cl.nikalbazu.diptico.estado.MARCOS.chunked(5)) androidx.compose.foundation.layout.Row {
                for (m in fila) cl.nikalbazu.diptico.ui.FotoPerfil(cl.nikalbazu.diptico.estado.Perfil(personaje = "zorro", color = 0xFFFF7A59), 120f, marco = m.id)
            }
        } } }
        regla.mainClock.autoAdvance = false
        regla.mainClock.advanceTimeBy(900)
        regla.onRoot().captureRoboImage("capturas/343_perfil_marcos_hoja.png")
    }
    @Test fun t344_perfil_tarjeta_con_logros() {
        val ra = cl.nikalbazu.diptico.data.PerfilRA("nikalbazu", 1840, 5210, "", "2021", "",
            listOf("Chrono Trigger" to cl.nikalbazu.diptico.data.Logro(1, "Primera victoria", "", 5, "1", "2026-11-01", false)))
        foto("344_perfil_tarjeta_con_logros", conPerfil(abierto = 0).copy(perfilRA = ra, usuarioRA = "nikalbazu"), Rol.SUPERIOR)
    }
    @Config(qualifiers = ABAJO)
    @Test fun t345_abajo_franja_y_globito_dippo() = foto("345_abajo_franja_y_globito_dippo", conPerfil(personaje = "dragon", color = 0xFFE64FA0), Rol.INFERIOR, mi = 1)
    @Test fun t346_recorrido_dippo_una() = foto("346_recorrido_dippo_una", base().copy(novedades = 2, recorrido = true), Rol.UNICA)
    @Config(qualifiers = ABAJO)
    @Test fun t347_recorrido_dippo_abajo() = foto("347_recorrido_dippo_abajo", base().copy(novedades = 1, recorrido = true), Rol.INFERIOR, mi = 1)
    // Temperatura mientras juegas: caliente y enchufada, y muy caliente (la lectura se fuerza; en la captura no hay sensores).
    @Config(qualifiers = ABAJO)
    @Test fun t348_abajo_juego_caliente() {
        val T = cl.nikalbazu.diptico.estado.Termica
        T.pruebaLectura = cl.nikalbazu.diptico.estado.Termica.Lectura(43f, 0f, 2, Float.NaN, true, 55, cl.nikalbazu.diptico.estado.Termica.Nivel.CALIENTE)
        try {
            foto("348_abajo_juego_caliente", base().copy(sesion = Sesion("snes/1", System.currentTimeMillis() - 1_500_000, 0, false), despierta = true), Rol.INFERIOR, mi = 1)
        } finally { T.pruebaLectura = null }
    }
    @Config(qualifiers = ABAJO)
    @Test fun t349_abajo_juego_muy_caliente() {
        val T = cl.nikalbazu.diptico.estado.Termica
        T.pruebaLectura = cl.nikalbazu.diptico.estado.Termica.Lectura(45f, 0f, 3, Float.NaN, false, 40, cl.nikalbazu.diptico.estado.Termica.Nivel.MUY_CALIENTE)
        try {
            foto("349_abajo_juego_muy_caliente", base().copy(sesion = Sesion("snes/1", System.currentTimeMillis() - 1_500_000, 0, false), despierta = true), Rol.INFERIOR, mi = 1)
        } finally { T.pruebaLectura = null }
    }
    // Tu año (con partidas de ejemplo repartidas por los meses) y un logro de DIPPO celebrado en plena partida.
    @Test fun t350_tu_ano_arriba() {
        val anio = java.time.LocalDate.now().year
        fun f(mes: Int, clave: String, h: Double, n: Int) = cl.nikalbazu.diptico.estado.TuAno.Fila("$anio-%02d-10".format(mes), clave, (h * 3600).toLong(), n)
        cl.nikalbazu.diptico.estado.TuAno.pruebaFilas = listOf(
            f(1, "snes/1", 12.0, 9), f(2, "snes/1", 8.0, 6), f(2, "ps2/3", 15.0, 7), f(3, "ps2/3", 22.0, 10), f(4, "n64/7", 6.5, 5),
            f(5, "snes/1", 4.0, 3), f(6, "genesis/6", 3.0, 3), f(8, "ps2/3", 18.0, 8), f(9, "n64/7", 9.0, 6), f(10, "ps2/3", 14.0, 6), f(11, "snes/1", 5.0, 4),
        )
        try {
            val c = conPerfil()
            foto("350_tu_ano_arriba", c.copy(extra = c.extra.copy(anioEn = 0)), Rol.SUPERIOR)
        } finally { cl.nikalbazu.diptico.estado.TuAno.pruebaFilas = null }
    }
    @Config(qualifiers = ABAJO)
    @Test fun t351_abajo_logro_en_partida() = foto(
        "351_abajo_logro_en_partida",
        base().copy(
            sesion = Sesion("snes/1", System.currentTimeMillis() - 1_500_000, 0, false), despierta = true,
            extra = EstadoExtra(dippoCelebra = System.currentTimeMillis(), dippoLogro = "Maratón"),
        ),
        Rol.INFERIOR, mi = 1,
    )
    // ---------- Editar perfil (abajo) con la tarjeta arriba ----------
    private fun editando(fila: Int, col: Int, personaje: String = "zorro", color: Long = 0xFF23C486, una: Boolean = false): Estado {
        val e = conPerfil(abierto = 0)
        val ed = cl.nikalbazu.diptico.estado.EdicionPerfil(
            e.extra.perfil.copy(personaje = personaje, color = color), "Nico", presencia = true, saludo = true, fila = fila, col = col,
        )
        return e.copy(modoPantallas = if (una) "una_negra" else "dos", extra = e.extra.copy(edicionPerfil = ed))
    }
    @Config(qualifiers = ABAJO)
    @Test fun t352_editar_perfil_abajo() = foto("352_editar_perfil_abajo", editando(cl.nikalbazu.diptico.estado.FilasEdicion.PERSONAJES_2, 2), Rol.INFERIOR, mi = 1)
    @Test fun t353_editar_perfil_tarjeta_arriba() = foto("353_editar_perfil_tarjeta_arriba", editando(cl.nikalbazu.diptico.estado.FilasEdicion.PERSONAJES_2, 2), Rol.SUPERIOR)
    @Config(qualifiers = ABAJO)
    @Test fun t354_editar_perfil_color() = foto("354_editar_perfil_color", editando(cl.nikalbazu.diptico.estado.FilasEdicion.COLOR, 6, personaje = "inicial", color = 0xFF9B6BFF), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t355_editar_perfil_guardar() = foto("355_editar_perfil_guardar", editando(cl.nikalbazu.diptico.estado.FilasEdicion.BOTONES, 0, personaje = "pixel", color = 0xFFFFC94D), Rol.INFERIOR, mi = 1)
    @Test fun t356_editar_perfil_una_pantalla() = foto("356_editar_perfil_una_pantalla", editando(cl.nikalbazu.diptico.estado.FilasEdicion.NOMBRE, 0, una = true), Rol.UNICA)
    @Config(qualifiers = ABAJO)
    @Test fun t357_partidas_copias() = foto("357_partidas_copias", conPerfil().copy(panel = Panel.MENU, seccion = Seccion.SISTEMA, enOpciones = true, indice = 0,
        extra = conPerfil().extra.copy(carpetasPartidas = listOf("content://x/tree/saves"), medidorFps = true)), Rol.INFERIOR, mi = 1)
    @Test fun t358_alto_contraste() {
        cl.nikalbazu.diptico.ui.Estilo.contraste = true
        try { foto("358_alto_contraste", conPerfil().copy(panel = Panel.MENU, seccion = Seccion.G_ESTILO, enOpciones = true, indice = 1, extra = conPerfil().extra.copy(altoContraste = true)), Rol.SUPERIOR) }
        finally { cl.nikalbazu.diptico.ui.Estilo.contraste = false }
    }
    // ---------- Carpetas: ordenar y estilos ----------
    private fun carpetasCon(estilo: String, ordenando: Boolean = false, tomada: String? = null) = conRecientes(base()).copy(
        vistaConsolas = "carpetas", enSistemas = true, consolaSel = tomada ?: "snes",
        extra = EstadoExtra(estiloCarpetas = estilo, ordenandoCarpetas = ordenando, carpetaTomada = tomada),
    )
    @Config(qualifiers = ABAJO)
    @Test fun t359_carpetas_ordenando() = foto("359_carpetas_ordenando", carpetasCon("tarjeta", ordenando = true, tomada = "snes"), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t360_carpetas_consola() = foto("360_carpetas_consola", carpetasCon("consola"), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t361_carpetas_abanico() = foto("361_carpetas_abanico", carpetasCon("abanico"), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t362_carpetas_logo() = foto("362_carpetas_logo", carpetasCon("logo"), Rol.INFERIOR, mi = 1)
    // ---------- Ajustes condensados ----------
    private fun conSubOpciones(abierta: Boolean, sel: Int = 1): Estado {
        val e0 = conPerfil().copy(panel = Panel.MENU, seccion = Seccion.G_ESTILO, enOpciones = true, indice = 0)
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(e0)
        val i = Diptico.opcionesSeccion(Seccion.G_ESTILO).indexOfFirst { it.sub != null }.coerceAtLeast(0)
        return e0.copy(indice = i, extra = e0.extra.copy(subOpcion = if (abierta) i else -1, subSel = sel))
    }
    @Config(qualifiers = ABAJO)
    @Test fun t363_ajustes_condensados() = foto("363_ajustes_condensados", conSubOpciones(false), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t364_ajustes_ventana_mas() = foto("364_ajustes_ventana_mas", conSubOpciones(true), Rol.INFERIOR, mi = 1)
    @Test fun t365_ajustes_ventana_ayuda_arriba() = foto("365_ajustes_ventana_ayuda_arriba", conSubOpciones(true), Rol.SUPERIOR)
    // Barra de abajo flotante (sin fondo, encima de las carpetas) y escondida (solo la píldora).
    private fun conDock(abierta: Boolean) = carpetasCon("tarjeta").let { it.copy(vista = "mosaicos", appsFijadas = listOf("com.android.settings", "com.android.chrome"), extra = it.extra.copy(dockVisible = abierta)) }
    @Config(qualifiers = ABAJO)
    @Test fun t366_barra_flotante() = foto("366_barra_flotante", conDock(true), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t367_barra_escondida() = foto("367_barra_escondida", conDock(false), Rol.INFERIOR, mi = 1)
    // Centro de Switch 2 y ports: abajo la lista, arriba el detalle, y con una pantalla todo junto.
    private fun conNativos(pestana: Int, sel: Int = 0) = base().copy(vista = "mosaicos", extra = EstadoExtra(nativosEn = 0, nativosPestana = pestana, nativosSel = sel))
    @Config(qualifiers = ABAJO)
    @Test fun t368_nativos_abajo() = foto("368_nativos_abajo", conNativos(0, 1), Rol.INFERIOR, mi = 1)
    @Test fun t369_nativos_arriba() = foto("369_nativos_arriba", conNativos(0, 1), Rol.SUPERIOR)
    @Config(qualifiers = ABAJO)
    @Test fun t370_nativos_ports() = foto("370_nativos_ports", conNativos(1, 0), Rol.INFERIOR, mi = 1)
    @Test fun t371_nativos_una() = foto("371_nativos_una", conNativos(0, 2).copy(modoPantallas = "una_negra"), Rol.UNICA)
    // Temas: el calendario (cada fiesta con su estilo completo) y los exclusivos con candado.
    @Config(qualifiers = ABAJO)
    @Test fun t372_temas_calendario() = foto("372_temas_calendario", base().copy(panel = Panel.GALERIA, galeria = "temas", galeriaPestana = 1, indice = 1), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t373_temas_exclusivos() = foto("373_temas_exclusivos", base().copy(panel = Panel.GALERIA, galeria = "temas", galeriaPestana = 2, indice = 0), Rol.INFERIOR, mi = 1)
    @Test fun t374_tema_exclusivo_arriba() = foto("374_tema_exclusivo_aurora", base(), Rol.SUPERIOR, tema = Temas.exclusivos[0].tema)
    @Test fun t375_tema_fiesta_navidad() = foto("375_tema_fiesta_navidad", base(), Rol.SUPERIOR, tema = Temas.fiestas.first { it.id == "navidad" }.tema)
    // Emuladores al día: DIPPO arriba con su resumen, los que tienen versión nueva primero.
    private val emusPrueba = listOf(
        EmuRevisado("Eden", "dev.eden.eden_emulator", "0.8.2", 0, listOf("Switch"), 41 * 3600, ultima = "v0.9.0", bytes = 98_000_000, nueva = true, notas = "Mejoras en Vulkan\nArreglos de audio"),
        EmuRevisado("Azahar", "org.azahar_emu.azahar", "2124.1", 0, listOf("3DS"), 12 * 3600, ultima = "2124.1"),
        EmuRevisado("DuckStation", "com.github.stenzek.duckstation", "0.1-9384", 0, listOf("PlayStation"), 3 * 3600, aviso = "Se actualiza desde Play Store"),
    )
    private fun conEmus(sel: Int, descarga: String? = null) = base().copy(vista = "mosaicos", extra = EstadoExtra(nativosEn = 0, nativosPestana = 2, nativosSel = sel,
        emus = emusPrueba, emusRevisado = System.currentTimeMillis() - 3_600_000L, emusDescarga = descarga, emusAvance = 0.45f))
    @Config(qualifiers = ABAJO)
    @Test fun t377_emus_abajo() = foto("377_emus_abajo", conEmus(1), Rol.INFERIOR, mi = 1)
    @Test fun t378_emus_arriba() = foto("378_emus_arriba", conEmus(0), Rol.SUPERIOR)
    @Config(qualifiers = ABAJO)
    @Test fun t379_emus_bajando() = foto("379_emus_bajando", conEmus(1, "dev.eden.eden_emulator"), Rol.INFERIOR, mi = 1)
    // Premios de los logros de DIPPO y el perfil grande arriba.
    @Config(qualifiers = ABAJO)
    @Test fun t380_logros_premios() = foto("380_logros_premios", conPerfil().copy(panel = Panel.GALERIA, galeria = "logros_dippo", galeriaPestana = 1), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t381_logros_con_premio() = foto("381_logros_con_premio", conPerfil().copy(panel = Panel.GALERIA, galeria = "logros_dippo", galeriaPestana = 0, indice = 4), Rol.INFERIOR, mi = 1)
    @Test fun t382_perfil_arriba() = foto("382_perfil_arriba", conPerfil(), Rol.SUPERIOR)
    @Test fun t383_perfil_arriba_cajas() = foto("383_perfil_arriba_cajas", conPerfil().let { it.copy(extra = it.extra.copy(portada = "cajas")) }, Rol.SUPERIOR)
    // Abajo con la vitrina de cajas arriba: la fila de juegos centrada en lo que queda a la vista sobre la barra.
    @Config(qualifiers = ABAJO)
    @Test fun t384_juegos_centrados() = foto("384_juegos_centrados", base().copy(vista = "mosaicos", extra = EstadoExtra(portada = "cajas")), Rol.INFERIOR, mi = 1)
    // Íconos pixel de Switch, Switch 2 y Gahvoria (vista lado a lado con el estilo pixel).
    @Test fun t385_iconos_pixel() {
        regla.setContent { Lienzo(true, Modifier.fillMaxSize(), 1f) {
            androidx.compose.runtime.CompositionLocalProvider(cl.nikalbazu.diptico.ui.LocalIconos provides cl.nikalbazu.diptico.ui.IconosCfg(estilo = "pixel")) {
                androidx.compose.foundation.layout.Row(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(24f)), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(androidx.compose.ui.unit.Dp(28f))) {
                    for (id in listOf("switch", "switch2", "wiiu", "androidgames", "rpgmaker"))
                        cl.nikalbazu.diptico.ui.IconoConsola(cl.nikalbazu.diptico.data.Consolas.porId(id), 120f, androidx.compose.ui.graphics.Color(0xFF14121F))
                }
            }
        } }
        regla.onRoot().captureRoboImage("capturas/385_iconos_pixel.png")
    }
    // Pantalla de abajo: apps fijadas arriba, lupa y ajustes con pestaña semiredonda, íconos pixel dentro de las carpetas.
    @Config(qualifiers = ABAJO)
    @Test fun t388_dock_arriba() = foto("388_dock_arriba", carpetasCon("tarjeta").let { it.copy(estiloIconos = "pixel") }, Rol.INFERIOR, mi = 1)
    // Pantalla de arriba con el estilo Píxeles: el ícono grande de Switch, Switch 2, Wii U y 3DS.
    private fun arribaPixel(id: String) = carpetasCon("tarjeta").copy(consolaSel = id, estiloIconos = "pixel")
    @Test fun t389_arriba_pixel_switch() = foto("389_arriba_pixel_switch", arribaPixel("switch"), Rol.SUPERIOR)
    @Test fun t390_arriba_pixel_3ds() = foto("390_arriba_pixel_3ds", arribaPixel("n3ds"), Rol.SUPERIOR)
    @Test fun t391_arriba_pixel_wiiu() = foto("391_arriba_pixel_wiiu", arribaPixel("wiiu"), Rol.SUPERIOR)
    // Abajo: apps sin sombras y sin el título junto a la foto (el título solo va arriba).
    @Config(qualifiers = ABAJO)
    @Test fun t392_abajo_sin_titulo() = foto("392_abajo_sin_titulo", conPerfil().copy(vistaConsolas = "carpetas", enSistemas = true, consolaSel = "snes"), Rol.INFERIOR, mi = 1)
    // Estilo «Pixel art» de RetroArch (sin bajar): Switch, Wii U y Android caen a los sprites pixel, no a la insignia.
    @Config(qualifiers = ABAJO)
    @Test fun t393_pixel_art_pestanas() = foto("393_pixel_art_pestanas", base().copy(vistaConsolas = "arriba", estiloIconos = "dot-art", consolaSel = "switch"), Rol.INFERIOR, mi = 1)
    // Arriba con la vitrina de cajas: hora y batería a la izquierda, junto a la consola; tu foto sola a la derecha.
    @Test fun t394_vitrina_reloj_izquierda() = foto("394_vitrina_reloj_izquierda", conPerfil().let { it.copy(extra = it.extra.copy(portada = "cajas")) }, Rol.SUPERIOR)
    // Abajo: el juego elegido en el centro de lado a lado.
    @Config(qualifiers = ABAJO)
    @Test fun t395_juegos_al_centro() = foto("395_juegos_al_centro", base().copy(vista = "mosaicos", extra = EstadoExtra(portada = "cajas")), Rol.INFERIOR, mi = 1)
    // Todas las vistas con el diseño nuevo (abajo) y la vista Xbox de vuelta.
    @Config(qualifiers = ABAJO)
    @Test fun t396_vista_xbox() = foto("396_vista_xbox", base().copy(vista = "xbox", extra = EstadoExtra(portada = "cajas")), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t397_vista_galeria() = foto("397_vista_galeria", base().copy(vista = "cuadricula", extra = EstadoExtra(portada = "cajas")), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t398_vista_lista() = foto("398_vista_lista", base().copy(vista = "lista", extra = EstadoExtra(portada = "cajas")), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t399_vista_carrusel() = foto("399_vista_carrusel", base().copy(vista = "carrusel", extra = EstadoExtra(portada = "cajas")), Rol.INFERIOR, mi = 1)
    // Arriba con la vista Galería: la vitrina de cajas sigue (antes volvía el diseño viejo).
    @Test fun t400_galeria_arriba() = foto("400_galeria_arriba", conPerfil().let { it.copy(vista = "cuadricula", extra = it.extra.copy(portada = "cajas")) }, Rol.SUPERIOR)
    // Ajustes ordenados: ocho secciones con submenús; arriba la lista, abajo la explicación y la vista previa.
    @Test fun t401_ajustes_submenus() = foto("401_ajustes_submenus", conPerfil().copy(panel = Panel.MENU, seccion = Seccion.G_BIBLIOTECA, enOpciones = true, indice = 0), Rol.SUPERIOR)
    private fun submenuAbierto(): Estado {
        val e0 = conPerfil().copy(panel = Panel.MENU, seccion = Seccion.G_BIBLIOTECA, enOpciones = true, indice = 0)
        Diptico.iniciar(ApplicationProvider.getApplicationContext()); Diptico.fijarParaPrueba(e0)
        val i = Diptico.opcionesSeccion(Seccion.G_BIBLIOTECA).indexOfFirst { it.sub != null }.coerceAtLeast(0)
        return e0.copy(indice = i, extra = e0.extra.copy(subOpcion = i, subSel = 1))
    }
    @Test fun t402_ajustes_submenu_abierto() = foto("402_ajustes_submenu_abierto", submenuAbierto(), Rol.SUPERIOR)
    @Config(qualifiers = ABAJO)
    @Test fun t403_ajustes_explicacion_abajo() = foto("403_ajustes_explicacion_abajo", submenuAbierto(), Rol.INFERIOR, mi = 1)
    @Test fun t404_ajustes_dippo() = foto("404_ajustes_dippo", conPerfil().copy(panel = Panel.MENU, seccion = Seccion.G_DIPPO, enOpciones = true, indice = 1), Rol.SUPERIOR)
    // Arreglos: Xbox sin cortes, carrusel de sistemas con pistas sobre la barra, y la lista de íconos dentro del submenú.
    @Config(qualifiers = ABAJO)
    @Test fun t405_xbox_sin_cortes() = foto("405_xbox_sin_cortes", base().copy(vista = "xbox", extra = EstadoExtra(portada = "cajas")), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t406_sistemas_pistas() = foto("406_sistemas_pistas", base().copy(vistaConsolas = "sistemas", enSistemas = true), Rol.INFERIOR, mi = 1)
    private fun listaIconosEnSubmenu(): Estado {
        val e0 = conPerfil().copy(panel = Panel.MENU, seccion = Seccion.G_ESTILO, enOpciones = true, indice = 0)
        Diptico.iniciar(ApplicationProvider.getApplicationContext()); Diptico.fijarParaPrueba(e0)
        val l = Diptico.opcionesSeccion(Seccion.G_ESTILO)
        val i = l.indexOfFirst { o -> o.sub?.any { it.titulo == "Íconos de consola" } == true }.coerceAtLeast(0)
        val j = l.getOrNull(i)?.sub?.indexOfFirst { it.titulo == "Íconos de consola" }?.coerceAtLeast(0) ?: 0
        return e0.copy(indice = i, extra = e0.extra.copy(subOpcion = i, subSel = j, desplegable = j, desplegableSel = 5))
    }
    @Test fun t407_iconos_lista_en_submenu() = foto("407_iconos_lista_en_submenu", listaIconosEnSubmenu(), Rol.SUPERIOR)
    @Test fun t408_sistema_hora_logo() = foto("408_sistema_hora_logo", conPerfil().copy(vistaConsolas = "sistemas", enSistemas = true, consolaSel = "snes"), Rol.SUPERIOR)
    // Idea 9b: arriba «PS5 con caja» (arte, info a la izquierda, una caja flotando) y abajo solo los íconos.
    @Test fun t409_ps5_arriba() = foto("409_ps5_arriba", conPerfil().let { it.copy(vista = "iconos", extra = it.extra.copy(portada = "ps5")) }, Rol.SUPERIOR)
    @Config(qualifiers = ABAJO)
    @Test fun t410_iconos_abajo() = foto("410_iconos_abajo", base().copy(vista = "iconos", extra = EstadoExtra(portada = "ps5")), Rol.INFERIOR, mi = 1)
    // La caja se abre en la pantalla de carga.
    @Test fun t411_caja_abierta() = foto("411_caja_abierta", base().copy(vista = "iconos", extra = EstadoExtra(portada = "ps5", carga = cl.nikalbazu.diptico.estado.CargaJuego("snes/1", 0, System.currentTimeMillis() - 1350))), Rol.SUPERIOR, avance = 200)
    @Test fun t412_ajustes_b() = foto("412_ajustes_b", conPerfil().copy(panel = Panel.MENU, seccion = Seccion.G_BIBLIOTECA, enOpciones = true, indice = 0), Rol.SUPERIOR)
    // Ficha › Personalizar con sus apartados, centro de ports con su explicación, y la carga con el logo del juego.
    @Config(qualifiers = ABAJO)
    @Test fun t413_ficha_apartados() = foto("413_ficha_apartados", base().copy(panel = Panel.FICHA, fichaDe = "snes/1", pestanaFicha = 3, indice = 0), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t414_ports_explicados() = foto("414_ports_explicados", base().copy(extra = EstadoExtra(nativosEn = 1, nativosPestana = 1, nativosSel = 0)), Rol.INFERIOR, mi = 1)
    @Test fun t416_opciones_juego() = foto("416_opciones_juego", base().copy(panel = Panel.RAPIDO, indice = 1), Rol.INFERIOR, mi = 1)
    @Test fun t417_opciones_submenu() = foto("417_opciones_submenu", base().copy(panel = Panel.RAPIDO, indice = 1, extra = EstadoExtra(rapidoGrupo = 0, rapidoSel = 0)), Rol.INFERIOR, mi = 1)
    @Test fun t418_pantalla_caja_abajo() = foto("418_pantalla_caja_abajo", base().copy(extra = EstadoExtra(cajaJuego = "snes/1", cajaFuentes = listOf("no", "buscando", "sin"), cajaSel = 0)), Rol.INFERIOR, mi = 1)
    @Test fun t419_pantalla_caja_arriba() = foto("419_pantalla_caja_arriba", base().copy(extra = EstadoExtra(cajaJuego = "snes/1", cajaFuentes = listOf("no", "hallada", "sin"))), Rol.SUPERIOR)
    @Test fun t420_vitrina_pose() = foto("420_vitrina_pose", base().copy(vista = "iconos", extra = EstadoExtra(portada = "ps5", cajaTam = 1.15f, logoTam = 1.2f)), Rol.SUPERIOR)
    private val dispPrueba = Disposicion(logo = PiezaVitrina(4, 0, 0.9f), caja = PiezaVitrina(1, 2, 1.2f), fondo = PiezaVitrina(6, 3, 1.1f), conCaja = true, difuminado = true)
    @Test fun t421_editor_vitrina_abajo() = foto("421_editor_vitrina_abajo", base().copy(vista = "iconos", juegoSel = "snes/1", extra = EstadoExtra(portada = "ps5", edicionVitrina = EdicionVitrina("snes/1", dispPrueba, Disposicion(), sel = 1))), Rol.INFERIOR, mi = 1)
    @Test fun t422_editor_vitrina_arriba() = foto("422_editor_vitrina_arriba", base().copy(vista = "iconos", juegoSel = "snes/1", extra = EstadoExtra(portada = "ps5", edicionVitrina = EdicionVitrina("snes/1", dispPrueba, Disposicion(), sel = 1))), Rol.SUPERIOR)
    @Test fun t423_vitrina_a_tu_gusto() = foto("423_vitrina_a_tu_gusto", base().copy(vista = "iconos", juegoSel = "snes/1", extra = EstadoExtra(portada = "ps5", disposiciones = mapOf("snes/1" to dispPrueba.copy(conCaja = false, difuminado = false)))), Rol.SUPERIOR)
    @Test fun t415_ports_detalle() = foto("415_ports_detalle", base().copy(extra = EstadoExtra(nativosEn = 1, nativosPestana = 1, nativosSel = 0)), Rol.SUPERIOR)
    // Carpetas en modo oscuro (opción «Tono de las carpetas») con un tema claro, para ver que se fuerza.
    @Config(qualifiers = ABAJO)
    @Test fun t386_carpetas_oscuras() = foto("386_carpetas_oscuras", carpetasCon("consola").let { it.copy(extra = it.extra.copy(carpetasTono = "oscuro")) }, Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun t387_carpetas_oscuras_tarjeta() = foto("387_carpetas_oscuras_tarjeta", carpetasCon("tarjeta").let { it.copy(extra = it.extra.copy(carpetasTono = "oscuro")) }, Rol.INFERIOR, mi = 1)
    // Por qué falló un juego: el emulador se cerró enseguida y Díptico explica lo más probable.
    @Config(qualifiers = ABAJO)
    @Test fun t376_cierre_rapido() = foto("376_cierre_rapido", base().copy(dialogo = Dialogo("cierre-rapido", "Leyenda del Bosque se cerró enseguida",
        "Se abrió con DuckStation y volvió a Díptico a los 6 s. Lo más probable:", Icono.EMULADOR,
        lineas = listOf("PlayStation: los emuladores como DuckStation piden la BIOS (por ejemplo scph5501.bin) en su carpeta de BIOS.",
            "El emulador no tiene permiso para leer la carpeta de juegos: ábrelo y elige la carpeta dentro de él."),
        si = "Probar con otro emulador", no = "Cerrar")), Rol.INFERIOR, mi = 1)
    @Test fun t337_perfil_boton_esquina() = foto("337_perfil_boton_esquina", conPerfil(personaje = "dragon", color = 0xFFE64FA0), Rol.SUPERIOR)
    @Test fun t338_perfil_ajustes() = foto("338_perfil_ajustes", conPerfil().copy(panel = Panel.MENU, seccion = Seccion.G_CUENTAS, enOpciones = true, indice = 3), Rol.SUPERIOR)
    @Test fun t339_perfil_galeria_personajes() = foto("339_perfil_galeria_personajes", conPerfil(personaje = "fantasma").copy(panel = Panel.GALERIA, galeria = "personajes", indice = 4), Rol.SUPERIOR)
    @Test fun t340_perfil_personajes_grandes() {
        regla.setContent { Lienzo(true, Modifier.fillMaxSize(), 1f) { androidx.compose.foundation.layout.Column {
            for (fila in cl.nikalbazu.diptico.estado.PERSONAJES.chunked(4)) androidx.compose.foundation.layout.Row {
                for ((id, _) in fila) cl.nikalbazu.diptico.ui.FotoPerfil(
                    cl.nikalbazu.diptico.estado.Perfil(personaje = id, color = cl.nikalbazu.diptico.estado.Perfil.COLORES[cl.nikalbazu.diptico.estado.PERSONAJES.indexOfFirst { it.first == id }].second), 150f)
            }
        } } }
        regla.onRoot().captureRoboImage("capturas/340_personajes_grandes.png")
    }

    @Test fun t307_carga_arriba() = foto("307_carga_arriba", base().copy(vista = "mosaicos", extra = EstadoExtra(carga = cl.nikalbazu.diptico.estado.CargaJuego("snes/1", 0, System.currentTimeMillis()))), Rol.SUPERIOR, avance = 600)
    @Config(qualifiers = ABAJO)
    @Test fun t308_carga_abajo() = foto("308_carga_abajo", base().copy(vista = "mosaicos", extra = EstadoExtra(carga = cl.nikalbazu.diptico.estado.CargaJuego("snes/1", 0, System.currentTimeMillis()))), Rol.INFERIOR, mi = 1, avance = 600)
    @Test fun t309_carga_una() = foto("309_carga_una", base().copy(modoPantallas = "una_negra", extra = EstadoExtra(carga = cl.nikalbazu.diptico.estado.CargaJuego("psx/3", 0, System.currentTimeMillis())), juegoSel = "psx/3"), Rol.UNICA, avance = 600)
    @Test fun t303_portada_con_puntos() = foto("303_portada_con_puntos", base().copy(vista = "mosaicos"), Rol.SUPERIOR)
    @Test fun t304_jugando_nuevo_con_hltb() = foto("304_jugando_nuevo", base().copy(appsFijadas = listOf("com.android.settings", "com.android.chrome"), sesion = Sesion("snes/1", System.currentTimeMillis() - 1_500_000, 0, false), despierta = true, extra = EstadoExtra(duraciones = mapOf("snes/1" to cl.nikalbazu.diptico.estado.CuantoDura.Duracion(12f, 18f, 30f, 1L)))), Rol.INFERIOR, mi = 1)
    @Test fun t298_vistas_juegos() = foto("298_vistas_juegos", base().copy(panel = Panel.GALERIA, galeria = "vistas_juegos"), Rol.SUPERIOR)
    @Test fun t290_dippo_ajustes_colores() = foto("290_dippo_ajustes_colores", base().copy(vista = "mosaicos", panel = Panel.MENU, seccion = Seccion.G_DIPPO, enOpciones = true, indice = 3), Rol.SUPERIOR)

    @Test fun t258_menu_inicio_barra() = foto("258_menu_inicio_barra", base().copy(panel = Panel.MENU, seccion = Seccion.INICIO, enOpciones = true, indice = 4), Rol.SUPERIOR)

    @Test fun t95_tema_mis_colores() = foto(
        "95_interfaz_con_mis_colores", base(), Rol.SUPERIOR,
        Tema("mio", "Mi tema", 0xFF2EC4A6, 0xFFFFC93C, 0xFFFF5D8F, propio = true, fondo = 0xFF10243A, tinta = 0xFFE8F4FF),
    )
    @Config(qualifiers = ABAJO)

    @Test fun visual_tienda_temas() = foto("visual_tienda_temas", base().copy(panel = Panel.GALERIA, galeria = "temas", galeriaPestana = 3), Rol.INFERIOR, mi = 1)

    @Test fun visual_cristal_y_barra() = foto("visual_cristal_y_barra", base(), Rol.SUPERIOR, tema = Temas.incluidos.first { it.id == "diptico_vidrio" })

    @Test fun visual_catalogo_widgets_arriba() = foto("visual_catalogo_widgets_arriba",
        base().copy(inicio = "widgets", panel = Panel.ESTUDIO, editandoWidgets = true, estudioPestana = 1,
            estudioZona = "panel", celdaCatalogo = 2 to 1), Rol.SUPERIOR)

    @Test fun visual_estudio_arriba() = foto("visual_estudio_arriba", base().copy(panel = Panel.MENU, seccion = Seccion.APARIENCIA, enOpciones = true), Rol.SUPERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun visual_estudio_previa_abajo() = foto("visual_estudio_previa_abajo", base().copy(panel = Panel.MENU, seccion = Seccion.APARIENCIA, enOpciones = true, vistaConsolas = "carpetas"), Rol.INFERIOR, mi = 1)

    @Test fun visual_coleccion_arriba() = foto("visual_coleccion_arriba", base().copy(panel = Panel.WIDGET, coleccionVisual = "muestra", colecciones = mapOf("muestra" to cl.nikalbazu.diptico.estado.Coleccion("Aventuras", juegos.map { it.clave }))), Rol.SUPERIOR)

    @Test fun nav_barra_heroe() = foto("nav_barra_heroe", base().copy(barra = false), Rol.SUPERIOR)
    @Test fun nav_barra_widgets() = foto("nav_barra_widgets", base().copy(inicio = "widgets", barra = false), Rol.SUPERIOR)
    @Config(qualifiers = ABAJO)
    @Test fun nav_galeria_abajo() = foto("nav_galeria_abajo", base().copy(vista = "cuadricula"), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun nav_lista_abajo() = foto("nav_lista_abajo", base().copy(vista = "lista"), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun nav_carpetas_abajo() = foto("nav_carpetas_abajo", base().copy(vistaConsolas = "carpetas", enSistemas = true), Rol.INFERIOR, mi = 1)
    @Test fun nav_selector_arriba() = foto("nav_selector_arriba", base().copy(panel = Panel.GALERIA, galeria = "vistas_juegos"), Rol.SUPERIOR)
    @Config(qualifiers = ABAJO)
    @Test fun nav_selector_abajo() = foto("nav_selector_abajo", base().copy(panel = Panel.GALERIA, galeria = "vistas_juegos"), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun nav_menu_app() = foto("nav_menu_app", base().copy(navegacion = base().navegacion.copy(appsMenu = "com.test.app"), panel = Panel.APPS, appsTodas = listOf(App("com.test.app", "Mi aplicación", false))), Rol.INFERIOR, mi = 1)
    @Config(qualifiers = ABAJO)
    @Test fun nav_retoma_sin_clip() = foto("nav_retoma_sin_clip", base().copy(sesion = Sesion("snes/1", 1L, 0, false), continuacion = 1L), Rol.INFERIOR, mi = 1)

    @Config(qualifiers = ABAJO)
    @Test fun nav_menu_app_desinstalar() = foto("nav_menu_app_desinstalar", base().copy(navegacion = cl.nikalbazu.diptico.estado.NavegacionEstado(appsMenu = "com.test.app", appsMenuIndice = 5), panel = Panel.APPS, appsTodas = listOf(App("com.test.app", "Mi aplicación", false))), Rol.INFERIOR, mi = 1)

    @Config(qualifiers = ABAJO)
    @Test fun rc_mesa_bitacora() {
        val e = base()
        foto("rc_mesa_bitacora", e.copy(
            sesion = Sesion("snes/1", System.currentTimeMillis() - 180_000, 0, false), despierta = true,
            datos = e.datos.mapValues { it.value.copy(vistaPartida = "BITACORA") },
        ), Rol.INFERIOR, mi = 1)
    }

    @Config(qualifiers = ABAJO)
    @Test fun rc_mesa_herramientas() {
        val e = base()
        foto("rc_mesa_herramientas", e.copy(
            sesion = Sesion("snes/1", System.currentTimeMillis() - 180_000, 0, false), despierta = true,
            datos = e.datos.mapValues { it.value.copy(vistaPartida = "HERRAMIENTAS") },
        ), Rol.INFERIOR, mi = 1)
    }

    @Test fun rc_widgets_partida() {
        val e = base()
        foto("rc_widgets_partida", e.copy(inicio = "widgets", widgets = listOf(
            Widget("proximaPartida", 0, 0, 3, 2),
            Widget("colaJuegos", 3, 0, 3, 2),
            Widget("tengo20", 0, 2, 3, 1),
        ), datos = e.datos.mapValues { it.value.copy(avance = "JUGANDO", sesiones = 16) }), Rol.SUPERIOR)
    }

    @Config(qualifiers = ABAJO)
    @Test fun centro_bisa_en_partida() {
        val e = base()
        foto("centro_bisa_en_partida", e.copy(panel = Panel.ASISTENTE, despierta = true,
            sesion = Sesion("snes/1", System.currentTimeMillis() - 120_000, 0, false),
            chat = listOf(cl.nikalbazu.diptico.estado.MensajeChat("ia", "Aquí está tu próximo paso y las opciones para este sistema."))),
            Rol.INFERIOR, mi = 1)
    }

}

const val ARRIBA = "w731dp-h411dp-land-420dpi"
const val ABAJO = "w472dp-h411dp-land-420dpi"
const val MUESTRA = "w1600dp-h1100dp-land-160dpi"
const val MUESTRA_ALTA = "w1600dp-h2600dp-port-160dpi"



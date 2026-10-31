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

    private fun foto(nombre: String, e: Estado, rol: Rol, tema: Tema? = null) {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(e)
        Estilo.tema = tema ?: Temas.incluidos[0]
        Estilo.decoraciones = true
        regla.mainClock.autoAdvance = false
        regla.setContent {
            Lienzo(e.oscuro, Modifier.fillMaxSize(), e.tamTexto) { Raiz(e, rol, 0) }
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
    @Test fun t40_editar_widgets() = foto("40_editar_widgets", base().copy(panel = Panel.WIDGETS, indice = 2), Rol.INFERIOR)

    @Config(qualifiers = ABAJO)
    @Test fun t24_letra_muy_grande() = foto("24_abajo_letra_muy_grande", base().copy(tamTexto = 1.3f), Rol.INFERIOR)
}

const val ARRIBA = "w731dp-h411dp-land-420dpi"
const val ABAJO = "w472dp-h411dp-land-420dpi"

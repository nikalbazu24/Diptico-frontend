package cl.nikalbazu.diptico

import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.estado.*
import cl.nikalbazu.diptico.retoma.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavegacionTest {
    @Test fun pellizcarNoPierdeWidgetsNiLosSuperpone() {
        val originales = (0 until 60).map { i -> Widget("w$i", i % 6, i % 4, 2, 2, visible = i != 10) }
        for (cols in 4..8) for (filas in 3..8) {
            val r = Rejilla.distribuir(originales, cols, filas, 1)
            assertEquals(originales.count { it.visible }, r.widgets.count { it.visible })
            assertEquals(originales.map { it.tipo }, r.widgets.map { it.tipo })
            val celdas = mutableSetOf<Triple<Int, Int, Int>>()
            r.widgets.filter { it.visible }.forEach { w ->
                assertTrue(w.x >= 0 && w.x + w.an <= cols && w.y >= 0 && w.y + w.al <= filas)
                assertTrue(w.pagina in 0 until r.paginas)
                for (x in w.x until w.x + w.an) for (y in w.y until w.y + w.al) assertTrue(celdas.add(Triple(w.pagina, x, y)))
            }
        }
    }
    @Test fun clipEmpiezaEnFotogramaClaveYAcotaMemoria() {
        val b = BufferRetoma(ventanaUs = 2_000_000, limiteBytes = 80)
        b.agregar(PaqueteRetoma(ByteArray(5), 0, false, 0))
        assertTrue(b.copia().isEmpty())
        for (i in 0..100) b.agregar(PaqueteRetoma(ByteArray(5), i * 100_000L, i % 10 == 0, if(i % 10 == 0) 1 else 0))
        val clip = b.copia(); assertTrue(clip.first().clave)
        assertTrue(clip.sumOf { it.datos.size } <= 80)
        assertTrue(clip.last().tiempoUs - clip.first().tiempoUs <= 3_000_000)
    }
    @Test fun clipNoConservaPaquetesQueExcedenElLimite() {
        val b = BufferRetoma(limiteBytes = 10)
        b.agregar(PaqueteRetoma(ByteArray(20), 0, true, 1))
        assertTrue(b.copia().isEmpty())
    }
    @Test fun acordeAbreCajonYNoAbreMenuAlSoltarStart() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(navegacion = NavegacionEstado(atajoApps = "start_r3"), listo = true, configurado = true, intro = false))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_THUMBR))
        assertEquals(Panel.APPS, Diptico.estado.value.panel)
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_THUMBR))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_START))
        assertEquals(Panel.APPS, Diptico.estado.value.panel)
    }
    @Test fun atajoAprendidoConStartNoActivaElBotonSolo() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        val start = KeyEvent.KEYCODE_BUTTON_START
        val extra = KeyEvent.KEYCODE_BUTTON_12
        Diptico.fijarParaPrueba(Estado(
            listo = true, configurado = true, intro = false,
            botonesExtra = listOf(BotonExtra("Extra", extra, "apps", conStart = true)),
        ))
        toque(extra, 1000L)
        assertEquals(Panel.NINGUNO, Diptico.estado.value.panel)
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_DOWN, start))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_DOWN, extra))
        assertEquals(Panel.APPS, Diptico.estado.value.panel)
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_UP, start))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_UP, extra))
        assertEquals(Panel.APPS, Diptico.estado.value.panel)
    }

    @Test fun arribaRecorreMosaicoAunqueElInicioMuestreWidgets() {
        val juegos = listOf("snes", "gba", "psx", "nds").map { id ->
            cl.nikalbazu.diptico.data.Juego("$id/juego", id, "Juego", "juego.zip", android.net.Uri.parse("content://prueba/$id"), id)
        }
        val e = Estado(configurado = true, inicio = "widgets", modoPantallas = "dos", dosDisponibles = true,
            intro = false, vistaConsolas = "mosaico", enSistemas = true, juegos = juegos)
        val columnasPrevias = Teclas.carrilesCarpetas
        try {
            Teclas.carrilesCarpetas = 2
            Diptico.fijarParaPrueba(e.copy(consolaSel = e.pestanas[3]))
            toque(KeyEvent.KEYCODE_DPAD_UP, 1000L)
            assertEquals(e.pestanas[1], Diptico.estado.value.consolaSel)
            toque(KeyEvent.KEYCODE_DPAD_UP, 1200L)
            assertEquals(e.pestanas[1], Diptico.estado.value.consolaSel)
            assertEquals(-1, Diptico.estado.value.widgetFoco)
        } finally {
            Teclas.carrilesCarpetas = columnasPrevias
        }
    }

    @Test fun soloCincoTemasIncluidosYTodosTienenDescripcion() {
        val e = Estado(galeria = "temas")
        val incluidos = Diptico.itemsGaleria(e)
        assertEquals(5, incluidos.size)
        assertTrue(incluidos.all { it.detalle.isNotBlank() && it.tema != null })
        assertEquals(listOf("Incluidos", "Mis temas"), Diptico.pestanasGaleria("temas"))
        assertEquals(1, Diptico.itemsGaleria(e.copy(galeriaPestana = 1)).size)
    }

    @Test fun mezclarTemaConservaLasEleccionesDeCadaPestana() {
        val base = Temas.iniciales.first().copy(id = "mio", propio = true)
        val archivoMusical = "com_0123456789abcdef01234567.ogg"
        val fuente = base.copy(id = "com_0123456789abcdef01234567", fuente = "serif", sonidos = "retro",
            tonoSonidos = -3, musica = archivoMusical, pistaBase = "sakura")
        val conLetra = ComponenteTema.TIPOGRAFIA.combinar(base, fuente)
        val conEfectos = ComponenteTema.EFECTOS.combinar(conLetra, fuente)
        val mezcla = ComponenteTema.MUSICA.combinar(conEfectos, fuente)
        assertEquals(base.copy(fuente = "serif", sonidos = "retro", tonoSonidos = -3,
            musica = archivoMusical, pistaBase = "sakura"), mezcla)
        assertEquals(mezcla, Tema.deJson(mezcla.json()))
        assertEquals(0, Tema.deJson(base.json().apply { remove("tonoSonidos") }).tonoSonidos)
    }

    @Test fun carpetaTodoSePersonalizaConElMandoEnLasTresVistas() {
        for (vista in Diptico.VISTAS_CARPETAS) {
            Diptico.fijarParaPrueba(Estado(configurado = true, intro = false, vistaConsolas = vista,
                enSistemas = true, consolaSel = "todo"))
            toque(KeyEvent.KEYCODE_BUTTON_X, 1000L)
            assertEquals(Panel.EDITAR_CONSOLA, Diptico.estado.value.panel)
            assertEquals("todo", Diptico.estado.value.consolaEditando)
            assertEquals(listOf("Imagen del ícono de carpeta"), Diptico.opcionesEditarConsola().map { it.titulo })
        }
    }

    @Test fun tiendaLocalIncluyeTemasDeVideojuegosConRecursosPropios() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val temas = TiendaTemas.incluidos(ctx)
        assertEquals(TiendaTemas.TOTAL_OFICIAL, temas.size)
        assertEquals(temas.size, temas.mapNotNull { it.pistaLocal }.toSet().size)
        assertTrue(temas.all { it.tema.descripcion.isNotBlank() && it.categoria.isNotBlank() &&
            it.tema.fuente.isNotBlank() })
        val chile = temas.single { it.tema.nombre == "Chile" }
        assertEquals("chupallas", chile.tema.decor)
        val instalado = TiendaTemas.instalar(ctx, chile.copy(audio = null))
        assertNotNull(TiendaTemas.pista(ctx, instalado))
        assertNotNull(TiendaTemas.pista(ctx, ComponenteTema.MUSICA.combinar(Temas.iniciales.first().copy(id = "mio"), instalado)))
        val juegos = temas.filter { it.categoria == "Videojuegos" }
        assertEquals(6, juegos.size)
        assertEquals(6, juegos.map { it.tema.sonidos }.toSet().size)
        assertEquals(6, juegos.map { it.tema.decor }.toSet().size)
        for (entrada in juegos) {
            for (accion in listOf("mover", "aceptar", "atras", "jugar")) {
                val id = ctx.resources.getIdentifier("sfx_${entrada.tema.sonidos}_$accion", "raw", ctx.packageName)
                assertTrue(id != 0)
            }
            assertNotNull(TiendaTemas.pista(ctx, TiendaTemas.instalar(ctx, entrada)))
        }
        val tienda = Estado(galeria = "tienda_temas", catalogoTemas = temas, galeriaPestana = 1)
        assertEquals(juegos.map { it.tema.id }, Diptico.itemsGaleria(tienda).map { it.id })
    }

    @Test fun adornosPorDefectoConservanLasFechasDelCalendario() {
        val fiestas = Temas.fiestas.associateBy { it.tema.id }
        for (grupo in Temas.adornosFestivos) {
            assertTrue(fiestas.getValue(grupo.temaId).tema.decor in grupo.opciones)
            assertTrue(grupo.opciones.all { it in Temas.DECORACIONES })
        }
    }

    @Test fun salirDeOcultosConBRecuperaLaBibliotecaAntesYDespuesDeElegirUnResultado() {
        val visible = cl.nikalbazu.diptico.data.Juego("gba/visible", "gba", "Visible", "visible.gba",
            android.net.Uri.parse("content://prueba/visible"), "visible")
        val oculto = visible.copy(clave = "snes/oculto", consolaId = "snes", titulo = "Oculto")
        for (abrirResultado in listOf(false, true)) {
            Diptico.fijarParaPrueba(Estado(configurado = true, intro = false, juegos = listOf(visible, oculto),
                datos = mapOf(oculto.clave to DatosJuego(oculto = true),
                    visible.clave to DatosJuego(favorito = true)),
                filtro = "favoritos", consolaSel = "gba", juegoSel = visible.clave,
                vistaConsolas = "carpetas", enSistemas = true))
            Diptico.abrirJuegosOcultos()
            assertEquals(listOf(oculto.clave), Diptico.estado.value.visibles.map { it.clave })
            if (abrirResultado) Diptico.aceptar()
            toque(KeyEvent.KEYCODE_BUTTON_B, 1200L)
            val estado = Diptico.estado.value
            assertEquals("favoritos", estado.filtro)
            assertEquals("gba", estado.consolaSel)
            assertEquals(visible.clave, estado.seleccionado?.clave)
            assertTrue(estado.enSistemas)
            assertNull(estado.antesDeOcultos)
        }
    }

    @Test fun decoracionPorTemaSeDespliegaYConservaLaIntensidadAlCambiarElAdorno() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(configurado = true, intro = false, tema = "bloques", temaFestivo = false))
        Diptico.abrirDecoraciones()
        fun elegir(id: String) = Diptico.elegirIndice(Diptico.itemsGaleria().indexOfFirst { it.id == id })
        elegir("decor-tema:bloques")
        elegir("decor-intensidad:1")
        elegir("decor-tipo:monedas")
        assertEquals("monedas", Diptico.estado.value.adornosPorTema["bloques"])
        assertEquals("1", Diptico.estado.value.intensidadPorTema["bloques"])
        Diptico.cerrarPanel()
        assertNull(Diptico.estado.value.decoracionAbierta)
        assertEquals(Panel.GALERIA, Diptico.estado.value.panel)
    }

    @Test fun vistaCarpetasAdmiteVentanaYPantallaCompleta() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(configurado = true, intro = false, vistaConsolas = "carpetas", enSistemas = true))
        toque(KeyEvent.KEYCODE_BUTTON_A, 1200L)
        assertTrue(Diptico.estado.value.carpetaEnVentana)
        Diptico.alternarModoCarpeta()
        assertFalse(Diptico.estado.value.carpetaEnVentana)
        assertFalse(Diptico.estado.value.enSistemas)
        toque(KeyEvent.KEYCODE_BUTTON_B, 1400L)
        assertTrue(Diptico.estado.value.enSistemas)
        assertTrue(Diptico.fuentesImagen(Diptico.estado.value.copy(candidatoPara = "c_portada", consolaEditando = "todo"))
            .containsAll(listOf("RetroArch", "SteamGridDB", "libretro", "Wikipedia")))
    }

    @Test fun laCrucetaSigueElRecorridoHorizontalDeCarpetas() {
        val juegos = listOf("snes", "gba", "psx", "nds", "ps2").map { id ->
            cl.nikalbazu.diptico.data.Juego("$id/juego", id, "Juego", "juego.zip", android.net.Uri.parse("content://prueba/$id"), id)
        }
        val e = Estado(configurado = true, intro = false, vistaConsolas = "carpetas",
            enSistemas = true, carpetasHorizontal = true, juegos = juegos)
        val anteriores = Teclas.carrilesCarpetas
        try {
            Teclas.carrilesCarpetas = 2
            Diptico.fijarParaPrueba(e)
            toque(KeyEvent.KEYCODE_DPAD_DOWN, 1000L)
            assertEquals(e.pestanas[1], Diptico.estado.value.consolaSel)
            toque(KeyEvent.KEYCODE_DPAD_DOWN, 1100L)
            assertEquals(e.pestanas[1], Diptico.estado.value.consolaSel)
            toque(KeyEvent.KEYCODE_DPAD_RIGHT, 1200L)
            assertEquals(e.pestanas[3], Diptico.estado.value.consolaSel)
            toque(KeyEvent.KEYCODE_DPAD_UP, 1300L)
            assertEquals(e.pestanas[2], Diptico.estado.value.consolaSel)
            toque(KeyEvent.KEYCODE_DPAD_LEFT, 1400L)
            assertEquals(e.pestanas[0], Diptico.estado.value.consolaSel)
        } finally {
            Teclas.carrilesCarpetas = anteriores
        }
    }

    @Test fun buscarDesdeWidgetsAbreElJuegoEncontradoEnUnaPantalla() {
        val juego = cl.nikalbazu.diptico.data.Juego("snes/1", "snes", "Juego", "juego.sfc",
            android.net.Uri.parse("content://prueba/juego"), "juego")
        Diptico.fijarParaPrueba(Estado(configurado = true, intro = false, dosDisponibles = false,
            inicio = "widgets", vistaConsolas = "carpetas", enSistemas = true, juegos = listOf(juego)))
        Diptico.abrirBuscar()
        Diptico.aceptar()
        val e = Diptico.estado.value
        assertEquals(Panel.NINGUNO, e.panel)
        assertFalse(e.enSistemas)
        assertEquals("heroe", e.inicio)
        assertEquals(juego, e.seleccionado)
    }

    @Test fun novedadesNoRepitenIdentificadoresNiRevivenVersionesAntiguas() {
        val ids = Novedades.HISTORIAL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(listOf(Novedades.ID), Novedades.pendientes("franja-burbuja-2.0-51").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51"), Novedades.pendientes("franja-ajuste-2.0-50").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50"), Novedades.pendientes("franja-nueva-2.0-49").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49"), Novedades.pendientes("esquinas-2.0-48").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48"), Novedades.pendientes("esquinas-2.0-47").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47"), Novedades.pendientes("mas-rapido-2.0-46").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46"), Novedades.pendientes("biblioteca-ordenada-2.0-45").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45"), Novedades.pendientes("tu-ano-2.0-44").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44"), Novedades.pendientes("tu-ano-2.0-43").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43"), Novedades.pendientes("tu-ano-2.0-42").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42"), Novedades.pendientes("temperatura-2.0-41").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41"), Novedades.pendientes("dippo-vivo-2.0-40").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40"), Novedades.pendientes("dippo-guia-2.0-39").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39"), Novedades.pendientes("primera-vez-2.0-38").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38"), Novedades.pendientes("marcos-dippo-2.0-37").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37"), Novedades.pendientes("perfil-mando-2.0-36").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36"), Novedades.pendientes("perfil-2.0-35").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35"), Novedades.pendientes("discord-amigos-2.0-34").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34"), Novedades.pendientes("cajas-exactas-2.0-33").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33"), Novedades.pendientes("cajas-reales-2.0-32").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32"), Novedades.pendientes("cajas-3d-2.0-31").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31"), Novedades.pendientes("estuches-discord-2.0-30").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30"), Novedades.pendientes("cajas-carga-2.0-29").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29"), Novedades.pendientes("ficha-carpetas-2.0-28").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28"), Novedades.pendientes("jugando-2.0-27").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28", "jugando-2.0-27"), Novedades.pendientes("deslizar-dock-2.0-26").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28", "jugando-2.0-27", "deslizar-dock-2.0-26"), Novedades.pendientes("logros-dippo-2.0-25").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28", "jugando-2.0-27", "deslizar-dock-2.0-26", "logros-dippo-2.0-25"), Novedades.pendientes("dippo-listo-2.0-24").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28", "jugando-2.0-27", "deslizar-dock-2.0-26", "logros-dippo-2.0-25", "dippo-listo-2.0-24"), Novedades.pendientes("dippo-casa-2.0-23").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28", "jugando-2.0-27", "deslizar-dock-2.0-26", "logros-dippo-2.0-25", "dippo-listo-2.0-24", "dippo-casa-2.0-23"), Novedades.pendientes("dippo-2.0-22").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28", "jugando-2.0-27", "deslizar-dock-2.0-26", "logros-dippo-2.0-25", "dippo-listo-2.0-24", "dippo-casa-2.0-23", "dippo-2.0-22"), Novedades.pendientes("temas-aparte-2.0-21").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28", "jugando-2.0-27", "deslizar-dock-2.0-26", "logros-dippo-2.0-25", "dippo-listo-2.0-24", "dippo-casa-2.0-23", "dippo-2.0-22", "temas-aparte-2.0-21"), Novedades.pendientes("ajustes-orden-2.0-20").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28", "jugando-2.0-27", "deslizar-dock-2.0-26", "logros-dippo-2.0-25", "dippo-listo-2.0-24", "dippo-casa-2.0-23", "dippo-2.0-22", "temas-aparte-2.0-21", "ajustes-orden-2.0-20"), Novedades.pendientes("vitrina-ajustes-2.0-19").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28", "jugando-2.0-27", "deslizar-dock-2.0-26", "logros-dippo-2.0-25", "dippo-listo-2.0-24", "dippo-casa-2.0-23", "dippo-2.0-22", "temas-aparte-2.0-21", "ajustes-orden-2.0-20", "vitrina-ajustes-2.0-19"), Novedades.pendientes("franja-2.0-18").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28", "jugando-2.0-27", "deslizar-dock-2.0-26", "logros-dippo-2.0-25", "dippo-listo-2.0-24", "dippo-casa-2.0-23", "dippo-2.0-22", "temas-aparte-2.0-21", "ajustes-orden-2.0-20", "vitrina-ajustes-2.0-19", "franja-2.0-18"), Novedades.pendientes("mosaicos-2.0-16").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28", "jugando-2.0-27", "deslizar-dock-2.0-26", "logros-dippo-2.0-25", "dippo-listo-2.0-24", "dippo-casa-2.0-23", "dippo-2.0-22", "temas-aparte-2.0-21", "ajustes-orden-2.0-20", "vitrina-ajustes-2.0-19", "franja-2.0-18", "mosaicos-2.0-16"), Novedades.pendientes("movimiento-y-vistas-1.3-15").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28", "jugando-2.0-27", "deslizar-dock-2.0-26", "logros-dippo-2.0-25", "dippo-listo-2.0-24", "dippo-casa-2.0-23", "dippo-2.0-22", "temas-aparte-2.0-21", "ajustes-orden-2.0-20", "vitrina-ajustes-2.0-19", "franja-2.0-18", "mosaicos-2.0-16", "movimiento-y-vistas-1.3-15"), Novedades.pendientes("orden-y-base-1.2-13").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28", "jugando-2.0-27", "deslizar-dock-2.0-26", "logros-dippo-2.0-25", "dippo-listo-2.0-24", "dippo-casa-2.0-23", "dippo-2.0-22", "temas-aparte-2.0-21", "ajustes-orden-2.0-20", "vitrina-ajustes-2.0-19", "franja-2.0-18", "mosaicos-2.0-16", "movimiento-y-vistas-1.3-15", "orden-y-base-1.2-13"), Novedades.pendientes("arreglos-1.2-12").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28", "jugando-2.0-27", "deslizar-dock-2.0-26", "logros-dippo-2.0-25", "dippo-listo-2.0-24", "dippo-casa-2.0-23", "dippo-2.0-22", "temas-aparte-2.0-21", "ajustes-orden-2.0-20", "vitrina-ajustes-2.0-19", "franja-2.0-18", "mosaicos-2.0-16", "movimiento-y-vistas-1.3-15", "orden-y-base-1.2-13", "arreglos-1.2-12", "glass-y-densidades-153-11"), Novedades.pendientes("mantenimiento-y-estabilidad-10").map { it.id })
        assertEquals(listOf(Novedades.ID, "franja-burbuja-2.0-51", "franja-ajuste-2.0-50", "franja-nueva-2.0-49", "esquinas-2.0-48", "esquinas-2.0-47", "mas-rapido-2.0-46", "biblioteca-ordenada-2.0-45", "tu-ano-2.0-44", "tu-ano-2.0-43", "tu-ano-2.0-42", "temperatura-2.0-41", "dippo-vivo-2.0-40", "dippo-guia-2.0-39", "primera-vez-2.0-38", "marcos-dippo-2.0-37", "perfil-mando-2.0-36", "perfil-2.0-35", "discord-amigos-2.0-34", "cajas-exactas-2.0-33", "cajas-reales-2.0-32", "cajas-3d-2.0-31", "estuches-discord-2.0-30", "cajas-carga-2.0-29", "ficha-carpetas-2.0-28", "jugando-2.0-27", "deslizar-dock-2.0-26", "logros-dippo-2.0-25", "dippo-listo-2.0-24", "dippo-casa-2.0-23", "dippo-2.0-22", "temas-aparte-2.0-21", "ajustes-orden-2.0-20", "vitrina-ajustes-2.0-19", "franja-2.0-18", "mosaicos-2.0-16", "movimiento-y-vistas-1.3-15", "orden-y-base-1.2-13", "arreglos-1.2-12", "glass-y-densidades-153-11", "mantenimiento-y-estabilidad-10", "rendimiento-y-una-pantalla-9", "zoom-y-recorrido-carpetas-8"),
            Novedades.pendientes("desenfoque-carpetas-7").map { it.id })
    }

    @Test fun tiempoConConsolaCerradaNoCuentaComoTiempoJugado() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(listo = true, configurado = true, intro = false, bienvenidaModo = "no", sesion = Sesion("snes/a", 1000L, 0, false)))
        Diptico.alEncender(60_000L, true)
        assertEquals(61_000L, Diptico.estado.value.sesion!!.inicio)
    }

    private fun toque(tecla: Int, tiempo: Long) {
        Teclas.manejar(KeyEvent(tiempo, tiempo, KeyEvent.ACTION_DOWN, tecla, 0))
        Teclas.manejar(KeyEvent(tiempo, tiempo + 20, KeyEvent.ACTION_UP, tecla, 0))
    }

    @Test fun dobleStartAbreBisaSinActivarElMenu() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(listo = true, configurado = true, intro = false,
            navegacion = NavegacionEstado(atajoBisa = AtajoInicio.START_DOBLE.id)))
        toque(KeyEvent.KEYCODE_BUTTON_START, 1000L)
        assertEquals(Panel.NINGUNO, Diptico.estado.value.panel)
        toque(KeyEvent.KEYCODE_BUTTON_START, 1120L)
        assertEquals(Panel.ASISTENTE, Diptico.estado.value.panel)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
            .idleFor(java.time.Duration.ofMillis(350))
        assertEquals(Panel.ASISTENTE, Diptico.estado.value.panel)
    }

    @Test fun unaPulsacionStartSigueAbriendoElMenu() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(listo = true, configurado = true, intro = false))
        toque(KeyEvent.KEYCODE_BUTTON_START, 1000L)
        assertEquals(Panel.MENU, Diptico.estado.value.panel)
    }

    @Test fun mantenerStartAbreElCentroDeControl() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(listo = true, configurado = true, intro = false))
        val start = KeyEvent.KEYCODE_BUTTON_START
        Teclas.manejar(KeyEvent(1000L, 1000L, KeyEvent.ACTION_DOWN, start, 0))
        Teclas.manejar(KeyEvent(1000L, 1600L, KeyEvent.ACTION_DOWN, start, 1))
        Teclas.manejar(KeyEvent(1000L, 1620L, KeyEvent.ACTION_UP, start, 0))
        assertEquals(Panel.CENTRO, Diptico.estado.value.panel)
    }

    @Test fun permiteCambiarBisaAStartSelectSinUsarBotonesPrincipales() {
        Diptico.iniciar(ApplicationProvider.getApplicationContext())
        Diptico.fijarParaPrueba(Estado(listo = true, configurado = true, intro = false,
            navegacion = NavegacionEstado(atajoBisa = "start_select")))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_START))
        Teclas.manejar(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT))
        assertEquals(Panel.ASISTENTE, Diptico.estado.value.panel)
    }

}

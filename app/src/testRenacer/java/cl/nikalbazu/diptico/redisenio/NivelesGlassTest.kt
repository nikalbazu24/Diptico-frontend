package cl.nikalbazu.diptico.redisenio

import androidx.test.core.app.ApplicationProvider
import cl.nikalbazu.diptico.data.Almacen
import cl.nikalbazu.diptico.estado.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NivelesGlassTest {
    @Test fun lasDosIntensidadesPersistenYSeConservanAlCambiarDeTema() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        Diptico.iniciar(ctx)
        Diptico.app = ctx
        Diptico.almacen = Almacen(ctx)
        Diptico.fijarParaPrueba(Estado(listo = true, configurado = true, temaFestivo = false))
        Diptico.elegirNivelGlass(ZonaGlass.CARPETAS, 20)
        Diptico.elegirNivelGlass(ZonaGlass.WIDGETS, 90)
        assertEquals(NivelesGlass(20, 90), Almacen(ctx).nivelesGlass)
        Diptico.elegirTema(Temas.incluidos.first { it.id == "diptico_vidrio" }.id)
        Diptico.elegirMaterialInterfaz("original")
        Diptico.elegirMaterialInterfaz("glass")
        assertEquals(NivelesGlass(20, 90), Diptico.estado.value.nivelesGlass)
        Diptico.elegirNivelGlass(ZonaGlass.CARPETAS, -15)
        assertEquals(NivelesGlass(0, 90), Almacen(ctx).nivelesGlass)
        Diptico.elegirNivelGlass(ZonaGlass.WIDGETS, 140)
        assertEquals(NivelesGlass(0, 100), Almacen(ctx).nivelesGlass)
    }

    @Test fun elMandoYElDeslizadorCambianSoloElControlElegido() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        Diptico.iniciar(ctx)
        Diptico.app = ctx
        Diptico.almacen = Almacen(ctx)
        Diptico.fijarParaPrueba(Estado(listo = true, configurado = true, experiencia = Experiencia(nivelesGlass = NivelesGlass(50, 80))))
        val carpeta = Diptico.opcionesSeccion(Seccion.APARIENCIA).first { it.titulo == "Glass de carpetas" }
        requireNotNull(carpeta.menos).invoke()
        assertEquals(NivelesGlass(40, 80), Diptico.estado.value.nivelesGlass)
        carpeta.accion()
        assertEquals(NivelesGlass(50, 80), Diptico.estado.value.nivelesGlass)
        val widget = Diptico.opcionesSeccion(Seccion.TEMAS).first { it.titulo == "Glass de widgets" }
        requireNotNull(widget.rango).alCambiar(30)
        assertEquals(NivelesGlass(50, 30), Almacen(ctx).nivelesGlass)
    }
}

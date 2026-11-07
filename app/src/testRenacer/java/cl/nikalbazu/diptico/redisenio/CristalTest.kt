package cl.nikalbazu.diptico.redisenio

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import cl.nikalbazu.diptico.estado.Temas
import cl.nikalbazu.diptico.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CristalTest {
    private fun contraste(a: Color, b: Color): Float = (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)
    @Test fun elTextoConservaContrasteEnClaroOscuroYFondosExtremos() {
        val tema = Temas.incluidos.first { it.id == "diptico_vidrio" }
        listOf(false, true).forEach { oscuro ->
            val p = paletaDe(tema, oscuro)
            CapaCristal.entries.forEach { capa ->
                listOf(Color.Black, Color.White).forEach { debajo ->
                    val superficie = lerp(debajo, colorMaterialCristal(p, capa), densidadCristal(capa, true, false))
                    assertTrue("Texto en $capa", contraste(p.tinta, superficie) >= 4.5f)
                }
            }
            assertTrue(contraste(p.tenue, colorMaterialCristal(p, CapaCristal.CONTENIDO)) >= 4.5f)
            val acento = Color(tema.acento)
            val tinta = if (acento.luminance() > .179f) Color.Black else Color.White
            assertTrue(contraste(tinta, acento) >= 4.5f)
        }
    }
    @Test fun lasSuperficiesSeHacenSolidasConAhorroOContraste() {
        CapaCristal.entries.forEach { capa ->
            assertEquals(1f, densidadCristal(capa, false, false), 0f)
            assertEquals(1f, densidadCristal(capa, true, true), 0f)
            assertTrue(densidadCristal(capa, true, false, 1f) >= densidadCristal(capa, true, false))
        }
        assertTrue(densidadCristal(CapaCristal.PANEL, true, false) > densidadCristal(CapaCristal.CONTROL, true, false))
    }

    @Test fun todosLosNivelesConservanTextoLegibleYElCeroEsSolido() {
        val tema = Temas.incluidos.first { it.id == "diptico_vidrio" }
        listOf(false, true).forEach { oscuro ->
            val p = paletaDe(tema, oscuro)
            listOf(0, 25, 55, 75, 100).forEach { nivel ->
                listOf(CapaCristal.CARPETA, CapaCristal.WIDGET).forEach { capa ->
                    val densidad = densidadCristal(capa, true, false, nivel = nivel)
                    if (nivel == 0) assertEquals(1f, densidad, 0f)
                    assertEquals(1f, densidadCristal(capa, false, false, nivel = nivel), 0f)
                    assertEquals(1f, densidadCristal(capa, true, true, nivel = nivel), 0f)
                    listOf(Color.Black, Color.White).forEach { debajo ->
                        val superficie = lerp(debajo, colorMaterialCristal(p, capa), densidad)
                        assertTrue("Texto $capa $nivel", contraste(p.tinta, superficie) >= 4.5f)
                        if (capa == CapaCristal.WIDGET) assertTrue("Detalle $nivel", contraste(paletaLecturaCristal(p, nivel).tenue, superficie) >= 4.5f)
                    }
                }
            }
        }
    }
}

package cl.nikalbazu.diptico.redisenio

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import cl.nikalbazu.diptico.ui.DesenfoqueBitmap
import cl.nikalbazu.diptico.ui.FondosCristal
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [29])
class DesenfoqueTest {
    @Test fun suavizaLosDetallesSinDependerDelFiltroDeAndroid12() {
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (y in 0..63) for (x in 0..63) bmp.setPixel(x,y,if((x+y)%2==0) Color.WHITE else Color.BLACK)
        DesenfoqueBitmap(64,64,5).aplicar(bmp)
        val valores = IntArray(64*64).also { bmp.getPixels(it,0,64,0,0,64,64) }
        val tonos = valores.map { Color.red(it) }
        assertTrue(tonos.max() - tonos.min() < 50)
        assertTrue(valores.all { Color.alpha(it) == 255 })
    }

    @Test fun extiendeLosBordesYConservaUnColorUniformeInclusoEnUnPixel() {
        val bmp = Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.rgb(40,120,200))
        DesenfoqueBitmap(1,1,5).aplicar(bmp)
        assertEquals(Color.rgb(40,120,200),bmp.getPixel(0,0))
    }

    @Test fun elAlfaNoGeneraHalosNegrosEnLasImagenesTransparentes() {
        val bmp = Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888)
        for (y in 8..23) for (x in 8..23) bmp.setPixel(x,y,Color.RED)
        DesenfoqueBitmap(32,32,4).aplicar(bmp)
        val c = bmp.getPixel(8,16)
        assertTrue(Color.alpha(c) in 1..254)
        assertEquals(255,Color.red(c))
        assertEquals(0,Color.green(c))
    }

    @Test fun comparteLaTexturaSuaveSinModificarLaPortadaOriginal() {
        val bmp = Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        for (y in 0..127) for(x in 0..63) bmp.setPixel(x,y,Color.BLACK)
        val original = bmp.asImageBitmap()
        val suave = FondosCristal.preparar(original)
        assertSame(suave,FondosCristal.preparar(original))
        assertTrue(maxOf(suave.width,suave.height)<=96)
        assertEquals(Color.BLACK,bmp.getPixel(63,64))
        assertEquals(Color.WHITE,bmp.getPixel(64,64))
        assertTrue(Color.red(suave.asAndroidBitmap().getPixel(48,48)) in 1..254)
    }
}

package cl.nikalbazu.diptico.ui

import android.graphics.Matrix
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/** Si el vidrio actúa como lente (Menú › Apariencia › Efecto lente). Solo se nota con Android 13 o más nuevo. */
internal val LocalLenteVidrio = staticCompositionLocalOf { true }

/**
 * Vidrio «de verdad» con un sombreador de Android 13: lo que queda detrás se ve un poco ampliado y se curva
 * hacia los bordes (refracción), el canto separa levemente los colores (dispersión) y brilla (fresnel),
 * y una luz suave sigue el punto que tocas. En equipos anteriores se usa el vidrio de siempre.
 */
internal object LenteVidrio {
    val disponible: Boolean get() = Build.VERSION.SDK_INT >= 33

    private const val CODIGO = """
        uniform shader fondo;
        uniform float2 tam;
        uniform float radio;
        uniform float fuerza;
        uniform float2 luz;
        uniform half4 tinte;
        uniform float opacidad;

        float caja(float2 p, float2 b, float r) {
            float2 q = abs(p) - b + r;
            return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
        }

        half4 main(float2 c) {
            float2 centro = tam * 0.5;
            float d = caja(c - centro, centro, radio);
            float ancho = max(min(tam.x, tam.y) * 0.24, 1.0);
            float dentro = clamp(-d / ancho, 0.0, 1.0);
            float curva = (1.0 - dentro) * (1.0 - dentro);
            float2 dir = (c - centro) / max(length(c - centro), 0.001);
            // Refracción: el borde «tira» la imagen hacia adentro y todo se amplía apenas, como una lupa suave.
            float2 m = c - dir * curva * 16.0 * fuerza;
            m = centro + (m - centro) * (1.0 - 0.05 * fuerza);
            // Dispersión: rojo y azul se separan un poco solo en el canto.
            float sep = curva * 2.2 * fuerza;
            half4 ca = fondo.eval(m + dir * sep);
            half4 cb = fondo.eval(m);
            half4 cc = fondo.eval(m - dir * sep);
            half3 col = half3(ca.r, cb.g, cc.b);
            col = mix(col, tinte.rgb, half(opacidad));
            // Fresnel: el canto brilla más que el centro.
            col += half3(smoothstep(0.35, 1.0, 1.0 - dentro) * 0.22 * fuerza);
            // Luz que sigue el dedo (o la esquina de arriba si no tocas).
            float l = 1.0 - clamp(length(c - luz) / (max(tam.x, tam.y) * 0.55), 0.0, 1.0);
            col += half3(l * l * l * 0.22 * fuerza);
            return half4(col, 1.0);
        }
    """

    /** null si este equipo no logra compilar el sombreador: entonces se usa el vidrio de siempre. */
    @RequiresApi(33)
    fun nuevo(): RuntimeShader? = runCatching { RuntimeShader(CODIGO) }.getOrNull()

    /**
     * Prepara el sombreador para dibujar una superficie de [tam] con esquinas [radio], sobre [imagen] ubicada en
     * [destino] con tamaño [dibujo] (relativo a la superficie). [luz] en píxeles de la superficie.
     */
    @RequiresApi(33)
    fun preparar(
        s: RuntimeShader, imagen: ImageBitmap, destino: IntOffset, dibujo: IntSize, tam: Size, radio: Float,
        fuerza: Float, luz: Offset, tinte: Color, opacidad: Float,
    ) {
        val bmp = imagen.asAndroidBitmap()
        val fondo = android.graphics.BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply {
                setScale(dibujo.width.toFloat() / bmp.width.coerceAtLeast(1), dibujo.height.toFloat() / bmp.height.coerceAtLeast(1))
                postTranslate(destino.x.toFloat(), destino.y.toFloat())
            })
        }
        s.setInputShader("fondo", fondo)
        s.setFloatUniform("tam", tam.width, tam.height)
        s.setFloatUniform("radio", radio)
        s.setFloatUniform("fuerza", fuerza.coerceIn(0f, 1.5f))
        s.setFloatUniform("luz", luz.x, luz.y)
        s.setFloatUniform("tinte", tinte.red, tinte.green, tinte.blue, 1f)
        s.setFloatUniform("opacidad", opacidad.coerceIn(0f, 1f))
    }
}

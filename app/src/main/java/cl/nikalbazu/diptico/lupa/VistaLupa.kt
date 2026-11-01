package cl.nikalbazu.diptico.lupa

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView

/**
 * Muestra la copia de la pantalla de arriba. Con [zona] (en fracciones 0..1 de la pantalla de arriba)
 * agranda solo esa parte hasta llenar la vista, sin deformarla. Sin zona, muestra la pantalla entera.
 */
class VistaLupa(ctx: Context) : TextureView(ctx), TextureView.SurfaceTextureListener {
    var zona: RectF? = null
        set(v) { field = v; ajustar() }

    private var superficie: Surface? = null

    init {
        surfaceTextureListener = this
        isOpaque = true
    }

    /** Dónde queda la pantalla de arriba completa dentro de esta vista (cuando no hay zona). */
    fun rectoCompleto(): RectF {
        val w = LupaServicio.ancho.toFloat()
        val h = LupaServicio.alto.toFloat()
        val s = minOf(width / w, height / h)
        val ox = (width - w * s) / 2f
        val oy = (height - h * s) / 2f
        return RectF(ox, oy, ox + w * s, oy + h * s)
    }

    /** Si la lupa ya sabe el tamaño real de la pantalla de arriba, la textura lo usa (así no se deforma). */
    fun refrescar() {
        try { surfaceTexture?.setDefaultBufferSize(LupaServicio.ancho, LupaServicio.alto) } catch (_: Exception) {}
        ajustar()
    }

    private fun ajustar() {
        if (width == 0 || height == 0) return
        val w = LupaServicio.ancho.toFloat()
        val h = LupaServicio.alto.toFloat()
        val z = zona ?: RectF(0f, 0f, 1f, 1f)
        val rx = z.left * w
        val ry = z.top * h
        val rw = (z.width() * w).coerceAtLeast(8f)
        val rh = (z.height() * h).coerceAtLeast(8f)
        val s = minOf(width / rw, height / rh)
        val m = Matrix()
        // La textura viene estirada a la vista: primero se vuelve a píxeles de la pantalla de arriba…
        m.setScale(w / width, h / height)
        // …se lleva la zona al origen, se agranda y se centra.
        m.postTranslate(-rx, -ry)
        m.postScale(s, s)
        m.postTranslate((width - rw * s) / 2f, (height - rh * s) / 2f)
        setTransform(m)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ajustar()
    }

    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
        st.setDefaultBufferSize(LupaServicio.ancho, LupaServicio.alto)
        val s = Surface(st)
        superficie = s
        LupaServicio.ponerSuperficie(s)
        ajustar()
    }

    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) = ajustar()

    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
        LupaServicio.ponerSuperficie(null)
        superficie?.release()
        superficie = null
        return true
    }

    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
}

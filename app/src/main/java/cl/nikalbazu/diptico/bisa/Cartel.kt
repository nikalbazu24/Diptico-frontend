package cl.nikalbazu.diptico.bisa

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import cl.nikalbazu.diptico.R
import cl.nikalbazu.diptico.estado.Idioma

/**
 * Un cartel corto encima del juego ("Sigues jugando…"). No se puede tocar ni le quita el control al juego:
 * aparece arriba, se queda unos segundos y se va solo. Sin permiso para mostrarse encima, usa un aviso de Android.
 */
object Cartel {
    private val manejador = Handler(Looper.getMainLooper())
    private var actual: Pair<WindowManager, View>? = null

    fun mostrar(ctx: Context, displayId: Int, etiqueta: String, titulo: String, detalle: String) {
        manejador.post { poner(ctx.applicationContext, displayId, Idioma.t(etiqueta), titulo, Idioma.t(detalle)) }
    }

    private fun poner(app: Context, displayId: Int, etiqueta: String, titulo: String, detalle: String) {
        quitar()
        if (!Settings.canDrawOverlays(app)) {
            try { Toast.makeText(app, "▶ $etiqueta: $titulo", Toast.LENGTH_LONG).show() } catch (_: Exception) {}
            return
        }
        try {
            val dm = app.getSystemService(DisplayManager::class.java)
            val pantalla = dm?.getDisplay(displayId) ?: dm?.getDisplay(0) ?: return
            val c = if (Build.VERSION.SDK_INT >= 30) app.createDisplayContext(pantalla).createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
            else app.createDisplayContext(pantalla)
            val wm = c.getSystemService(WindowManager::class.java) ?: return
            val dp = c.resources.displayMetrics.density
            val titulos = try { ResourcesCompat.getFont(c, R.font.fredoka_bold) } catch (_: Exception) { null }
            val textos = try { ResourcesCompat.getFont(c, R.font.nunito_bold) } catch (_: Exception) { null }

            val caja = LinearLayout(c).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((14 * dp).toInt(), (10 * dp).toInt(), (20 * dp).toInt(), (10 * dp).toInt())
                background = GradientDrawable().apply {
                    cornerRadius = 22 * dp
                    setColor(0xEE15131F.toInt())
                    setStroke((1.5f * dp).toInt(), 0x556B4EFF)
                }
                elevation = 8 * dp
            }
            val boton = TextView(c).apply {
                text = "▶"
                setTextColor(0xFFFFFFFF.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFF6B4EFF.toInt()) }
            }
            caja.addView(boton, LinearLayout.LayoutParams((36 * dp).toInt(), (36 * dp).toInt()).apply { marginEnd = (12 * dp).toInt() })
            val columna = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
            fun linea(t: String, tam: Float, color: Int, letra: android.graphics.Typeface?) = TextView(c).apply {
                text = t
                setTextColor(color)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, tam)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                maxWidth = (460 * dp).toInt()
                if (letra != null) typeface = letra
            }
            columna.addView(linea(etiqueta.uppercase(Idioma.locale), 10.5f, 0xFFFF7A59.toInt(), textos))
            columna.addView(linea(titulo, 17f, 0xFFF2F0FF.toInt(), titulos))
            if (detalle.isNotBlank()) columna.addView(linea(detalle, 12.5f, 0xFFB2AED0.toInt(), textos))
            caja.addView(columna)

            val p = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (18 * dp).toInt()
                windowAnimations = android.R.style.Animation_Toast
            }
            caja.alpha = 0f
            caja.translationY = -20 * dp
            wm.addView(caja, p)
            caja.animate().alpha(1f).translationY(0f).setDuration(280).start()
            actual = wm to caja
            manejador.postDelayed({
                caja.animate().alpha(0f).translationY(-16 * dp).setDuration(260).withEndAction { quitar() }.start()
            }, 4_600)
        } catch (_: Exception) {
            quitar()
        }
    }

    private fun quitar() {
        val (wm, v) = actual ?: return
        actual = null
        try { wm.removeView(v) } catch (_: Exception) {}
    }
}

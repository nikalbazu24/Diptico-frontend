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

    /**
     * Continuación grande para consolas de una pantalla: una tarjeta al centro, encima del juego, con la portada,
     * el título, cuánto llevas y tu próximo paso. No recibe toques (el juego sigue igual) y se va sola.
     */
    fun continuacion(ctx: Context, displayId: Int, titulo: String, detalle: String, paso: String, portada: android.net.Uri?) {
        val app = ctx.applicationContext
        Thread {
            val img = portada?.let { u ->
                try {
                    app.contentResolver.openInputStream(u)?.use { st ->
                        val o = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
                        android.graphics.BitmapFactory.decodeStream(st, null, o)
                    }
                } catch (_: Exception) { null }
            }
            manejador.post { ponerGrande(app, displayId, titulo, Idioma.t(detalle), paso, img) }
        }.start()
    }

    private fun ponerGrande(app: Context, displayId: Int, titulo: String, detalle: String, paso: String, portada: android.graphics.Bitmap?) {
        quitar()
        if (!Settings.canDrawOverlays(app)) {
            try { Toast.makeText(app, "▶ ${Idioma.t("Sigues jugando")}: $titulo", Toast.LENGTH_LONG).show() } catch (_: Exception) {}
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

            // Fondo oscurecido de toda la pantalla, con la tarjeta al centro.
            val velo = android.widget.FrameLayout(c).apply { setBackgroundColor(0x99000000.toInt()) }
            val caja = LinearLayout(c).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((22 * dp).toInt(), (20 * dp).toInt(), (26 * dp).toInt(), (20 * dp).toInt())
                background = GradientDrawable().apply {
                    cornerRadius = 28 * dp
                    setColor(0xF215131F.toInt())
                    setStroke((2 * dp).toInt(), 0x886B4EFF.toInt())
                }
                elevation = 16 * dp
            }
            if (portada != null) {
                val iv = android.widget.ImageView(c).apply {
                    setImageBitmap(portada)
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    clipToOutline = true
                    outlineProvider = object : android.view.ViewOutlineProvider() {
                        override fun getOutline(v: View, o: android.graphics.Outline) { o.setRoundRect(0, 0, v.width, v.height, 14 * dp) }
                    }
                }
                caja.addView(iv, LinearLayout.LayoutParams((110 * dp).toInt(), (150 * dp).toInt()).apply { marginEnd = (20 * dp).toInt() })
            }
            val columna = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
            fun linea(t: String, tam: Float, color: Int, letra: android.graphics.Typeface?, lineas: Int = 1) = TextView(c).apply {
                text = t
                setTextColor(color)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, tam)
                maxLines = lineas
                ellipsize = android.text.TextUtils.TruncateAt.END
                maxWidth = (420 * dp).toInt()
                if (letra != null) typeface = letra
            }
            columna.addView(linea(Idioma.t("Sigues jugando").uppercase(Idioma.locale), 12f, 0xFFFF7A59.toInt(), textos))
            columna.addView(linea(titulo, 26f, 0xFFF2F0FF.toInt(), titulos, 2))
            if (detalle.isNotBlank()) columna.addView(linea(detalle, 14f, 0xFFB2AED0.toInt(), textos))
            if (paso.isNotBlank()) {
                val pasoV = linea("✎  $paso", 15f, 0xFFF2F0FF.toInt(), textos, 2).apply {
                    setPadding((12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
                    background = GradientDrawable().apply { cornerRadius = 14 * dp; setColor(0xFF24213A.toInt()) }
                }
                columna.addView(pasoV, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = (12 * dp).toInt() })
            }
            caja.addView(columna)
            velo.addView(caja, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.WRAP_CONTENT, android.widget.FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

            val p = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            )
            velo.alpha = 0f
            caja.scaleX = 0.92f; caja.scaleY = 0.92f
            wm.addView(velo, p)
            velo.animate().alpha(1f).setDuration(260).start()
            caja.animate().scaleX(1f).scaleY(1f).setDuration(320).start()
            actual = wm to velo
            manejador.postDelayed({
                velo.animate().alpha(0f).setDuration(300).withEndAction { quitar() }.start()
            }, 6_000)
        } catch (_: Exception) {
            quitar()
        }
    }

    /** Si Díptico puede mostrarse encima de los juegos (lo necesitan la continuación en una pantalla y la burbuja de DIPPO). */
    fun puede(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    private fun quitar() {
        val (wm, v) = actual ?: return
        actual = null
        try { wm.removeView(v) } catch (_: Exception) {}
    }
}

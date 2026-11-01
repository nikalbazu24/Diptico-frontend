package cl.nikalbazu.diptico

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.media.AudioManager
import cl.nikalbazu.diptico.audio.Medios
import cl.nikalbazu.diptico.emu.Lanzador
import cl.nikalbazu.diptico.estado.Carpeta
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Teclas

/** Lo común a las dos pantallas: pantalla completa, botones del control y selector de carpetas. */
abstract class BaseActivity : ComponentActivity() {

    private val elegirJuegos = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) Diptico.carpetaElegida(uri, Carpeta.JUEGOS)
    }

    private val elegirImagenes = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) Diptico.carpetaElegida(uri, Carpeta.MEDIA)
    }

    private val elegirMusica = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) Diptico.carpetaElegida(uri, Carpeta.MUSICA)
    }

    private val elegirArchivo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) Diptico.archivoElegido(uri)
    }

    // Pedirle a Android que Díptico sea la app de inicio. Si la persona no lo elige ahí, se abren los ajustes de inicio.
    private val serInicio = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        Diptico.revisarInicio()
        if (Diptico.estado.value.esInicio) Diptico.avisar("¡Listo! Díptico es tu inicio: el botón Inicio siempre te trae aquí")
    }

    override fun onStart() {
        super.onStart()
        Diptico.aLaVista(1)
    }

    override fun onStop() {
        Diptico.aLaVista(-1)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Diptico.iniciar(this)
        // Los botones de volumen controlan la música (también la de Díptico).
        volumeControlStream = AudioManager.STREAM_MUSIC
        pantallaCompleta()
    }

    override fun onResume() {
        super.onResume()
        Diptico.pedirCarpeta = { tipo ->
            try {
                when (tipo) {
                    Carpeta.JUEGOS -> elegirJuegos.launch(null)
                    Carpeta.MEDIA -> elegirImagenes.launch(null)
                    Carpeta.MUSICA -> elegirMusica.launch(null)
                }
            } catch (e: Exception) {
                Diptico.avisar("No pude abrir el selector de carpetas")
            }
        }
        Diptico.pedirArchivo = { tipo ->
            try {
                elegirArchivo.launch(arrayOf(tipo))
            } catch (e: Exception) {
                Diptico.avisar("No pude abrir el selector de archivos")
            }
        }
        Diptico.pedirSerInicio = {
            val pedido = cl.nikalbazu.diptico.estado.Inicio.pedido(this)
            try {
                if (pedido != null) serInicio.launch(pedido)
                else serInicio.launch(android.content.Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
            } catch (e: Exception) {
                Diptico.avisar("No pude abrir los ajustes de inicio")
            }
        }
        Diptico.revisarInicio()
        Lanzador.olvidar()
        Diptico.revisarPantallas()
        Diptico.aplicarTema()
        aplicarRefresco()
        Medios.revisar(this)
        Diptico.alVolver(miDisplay())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) pantallaCompleta()
    }

    fun miDisplay(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.displayId ?: 0
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.displayId
        }

    private fun pantallaCompleta() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /** Con fluidez alta usa la tasa de refresco más alta de la pantalla; en ahorro, 60 Hz. */
    private fun aplicarRefresco() {
        try {
            val d = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else null
            val modos = d?.supportedModes ?: return
            val actual = d.mode
            val mismos = modos.filter { it.physicalWidth == actual.physicalWidth && it.physicalHeight == actual.physicalHeight }
            val elegido = when (Diptico.fluidezEfectiva()) {
                "ahorro" -> mismos.minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }
                "alta" -> mismos.maxByOrNull { it.refreshRate }
                else -> null
            } ?: return
            val lp = window.attributes
            if (lp.preferredDisplayModeId != elegido.modeId) {
                lp.preferredDisplayModeId = elegido.modeId
                window.attributes = lp
            }
        } catch (_: Exception) {
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        // Un toque cierra el protector de pantalla sin hacer nada más.
        if (ev.action == android.view.MotionEvent.ACTION_DOWN && Diptico.actividad()) return true
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchGenericMotionEvent(ev: android.view.MotionEvent): Boolean {
        if (Diptico.estado.value.protector) {
            if (ev.action == android.view.MotionEvent.ACTION_MOVE) Diptico.actividad()
            return true
        }
        if (Teclas.movimiento(ev)) { Diptico.actividad(); return true }
        return super.dispatchGenericMotionEvent(ev)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(android.content.Intent.CATEGORY_HOME)) Diptico.alInicio()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val volumen = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!volumen && event.action == KeyEvent.ACTION_DOWN && Diptico.actividad()) return true
        if (!volumen && event.action == KeyEvent.ACTION_UP && Diptico.estado.value.protector) return true
        if (Teclas.manejar(event)) return true
        return super.dispatchKeyEvent(event)
    }
}

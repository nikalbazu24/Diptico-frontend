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
        Diptico.revisarPantallas()
        Diptico.aplicarTema()
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

    override fun dispatchGenericMotionEvent(ev: android.view.MotionEvent): Boolean {
        if (Teclas.movimiento(ev)) return true
        return super.dispatchGenericMotionEvent(ev)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(android.content.Intent.CATEGORY_HOME)) Diptico.alInicio()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (Teclas.manejar(event)) return true
        return super.dispatchKeyEvent(event)
    }
}

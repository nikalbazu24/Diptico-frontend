package cl.nikalbazu.diptico

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

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

    // Respaldo: crear el archivo (en Google Drive o donde se elija) y traer uno.
    private val crearRespaldo = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) Diptico.respaldoCreado(uri)
    }
    private val exportarPartida = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        Diptico.exportarPartidaElegida(uri)
    }
    private val traerRespaldo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) Diptico.respaldoElegido(uri)
    }

    private var perfilPorExportar: String? = null
    private val exportarPerfil = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val texto = perfilPorExportar
        perfilPorExportar = null
        if (uri != null && texto != null) lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { contentResolver.openOutputStream(uri)?.use { it.write(texto.toByteArray(Charsets.UTF_8)) } ?: error("Sin acceso") }.isSuccess }
            if (ok) {
                try { startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("application/json").putExtra(android.content.Intent.EXTRA_STREAM, uri).addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION), "Compartir perfil visual")) } catch (_: Exception) { Diptico.avisar("Perfil guardado") }
            } else Diptico.avisar("No pude guardar el perfil")
        }
    }
    private val importarPerfil = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lifecycleScope.launch {
            val texto = withContext(Dispatchers.IO) { runCatching {
                contentResolver.openInputStream(uri)?.use { entrada ->
                    val buffer = java.io.ByteArrayOutputStream()
                    val bloque = ByteArray(4096)
                    while (buffer.size() <= 32768) {
                        val n = entrada.read(bloque, 0, minOf(bloque.size, 32769 - buffer.size()))
                        if (n < 0) break
                        buffer.write(bloque, 0, n)
                    }
                    val bytes = buffer.toByteArray()
                    require(bytes.size <= 32768)
                    String(bytes, Charsets.UTF_8)
                }
            }.getOrNull() }
            if (texto != null) Diptico.recibirPerfilVisual(texto) else Diptico.avisar("No pude leer ese perfil visual")
        }
    }

    private val elegirArchivo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) Diptico.archivoElegido(uri) else Diptico.cancelarArchivoVisual()
    }

    // Dictado por voz: Android escucha y devuelve el texto (para DIPPO y el teclado).
    private val dictado = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val t = r.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (r.resultCode == RESULT_OK && t != null) Diptico.vozRecibida(t)
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
        if (intent?.getBooleanExtra("actualizar", false) == true) Diptico.abrirActualizacion()
        // Los botones de volumen controlan la música (también la de Díptico).
        volumeControlStream = AudioManager.STREAM_MUSIC
        pantallaCompleta()
    }

    override fun onResume() {
        super.onResume()
        Diptico.revisarActualizacionAlVolver()
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
        Diptico.exportarVisual = { texto -> perfilPorExportar = texto; exportarPerfil.launch("Diptico-perfil-visual.json") }
        Diptico.importarVisual = { importarPerfil.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
        Diptico.pedirArchivo = { tipo ->
            try {
                elegirArchivo.launch(arrayOf(tipo))
            } catch (e: Exception) {
                Diptico.avisar("No pude abrir el selector de archivos")
            }
        }
        Diptico.pedirCrearRespaldo = { nombre ->
            try { crearRespaldo.launch(nombre) } catch (_: Exception) { Diptico.avisar("No pude abrir el selector de archivos") }
        }
        Diptico.pedirExportarPartida = { nombre ->
            try { exportarPartida.launch(nombre) } catch (_: Exception) { Diptico.avisar("No pude abrir el selector de archivos") }
        }
        Diptico.pedirTraerRespaldo = {
            try { traerRespaldo.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) } catch (_: Exception) { Diptico.avisar("No pude abrir el selector de archivos") }
        }
        Diptico.pedirVoz = {
            val i = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toLanguageTag())
                .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, cl.nikalbazu.diptico.estado.Idioma.t("Habla, te escucho"))
            try { dictado.launch(i); true } catch (_: Exception) { false }
        }
        Diptico.pedirMicrofono = {
            try { requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 53) } catch (_: Exception) {}
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
        if (Diptico.estado.value.panel == cl.nikalbazu.diptico.estado.Panel.APPS) Diptico.cargarApps()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) pantallaCompleta() else Teclas.soltarTodo()
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
        if (intent.getBooleanExtra("actualizar", false)) Diptico.abrirActualizacion()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val volumen = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!volumen && event.action == KeyEvent.ACTION_DOWN && Diptico.actividad()) return true
        if (!volumen && event.action == KeyEvent.ACTION_UP && Diptico.estado.value.protector) return true
        if (Teclas.manejar(event)) return true
        return super.dispatchKeyEvent(event)
    }
}



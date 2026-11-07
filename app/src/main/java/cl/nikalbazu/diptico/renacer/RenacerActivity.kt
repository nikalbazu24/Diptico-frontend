package cl.nikalbazu.diptico.renacer

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cl.nikalbazu.diptico.DipticoApp
import cl.nikalbazu.diptico.emu.Pantallas
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Entrada de la reconstrucción. No arranca el estado ni los paneles del launcher anterior. */
open class RenacerActivity : ComponentActivity(), DisplayManager.DisplayListener {
    protected open val inferior = false
    private val control get() = (application as DipticoApp).renacer
    private var imagenPara: String? = null
    private val mando by lazy { MandoRenacer { super.dispatchKeyEvent(it) } }
    private val carpeta = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { it?.let(control::carpeta) }
    private val imagen = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val clave = imagenPara
        imagenPara = null
        if (uri != null && clave != null) control.imagen(clave, uri)
    }
    private val exportar = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { it?.let(control::exportar) }
    private val importar = registerForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(control::importar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagenPara = savedInstanceState?.getString("imagenPara")
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = volver()
        })
        setContent {
            val e by control.estado.collectAsState()
            val apps by control.apps.collectAsState()
            val arte by control.arte.collectAsState()
            val segunda = Pantallas.inferior(this)?.displayId
            val doble = e.usarDos && e.dosPantallas
            SalonRenacer(e, ::accion, companera = doble && !inferior,
                pantallaJuego = Display.DEFAULT_DISPLAY,
                pantallaAlterna = segunda?.takeIf { doble }, apps = apps, buscandoArte = arte)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                control.estado.map { it.usarDos to it.dosPantallas }.distinctUntilChanged().collect { (usar, hay) ->
                    if (inferior && (!usar || !hay)) finish()
                    else if (!inferior && usar && hay) abrirInferior()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        getSystemService(DisplayManager::class.java).registerDisplayListener(this, null)
        revisarPantallas()
    }
    override fun onStop() {
        getSystemService(DisplayManager::class.java).unregisterDisplayListener(this)
        super.onStop()
    }
    override fun onResume() {
        super.onResume()
        control.volverDePartida(pantalla())
        if (!inferior && control.estado.value.usarDos) abrirInferior()
    }
    override fun onPause() {
        mando.detener()
        control.pausa(pantalla())
        super.onPause()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("imagenPara", imagenPara)
        super.onSaveInstanceState(outState)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) control.ir(Lugar.JUGAR)
    }
    override fun onDisplayAdded(displayId: Int) = revisarPantallas()
    override fun onDisplayRemoved(displayId: Int) = revisarPantallas()
    override fun onDisplayChanged(displayId: Int) = revisarPantallas()
    private fun revisarPantallas() = control.pantallas(Pantallas.inferior(this) != null)
    private fun pantalla(): Int = if (android.os.Build.VERSION.SDK_INT >= 30) display?.displayId ?: 0 else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.displayId
    }
    private fun abrirInferior() {
        val display = Pantallas.inferior(this) ?: return
        if (RenacerInferiorActivity.viva) return
        try {
            startActivity(Intent(this, RenacerInferiorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                ActivityOptions.makeBasic().setLaunchDisplayId(display.displayId).toBundle())
        } catch (_: Exception) { control.pantallas(false); control.avisar("No pude abrir la segunda pantalla. Puedes seguir en esta.") }
    }

    private fun accion(a: Accion) {
        when (a) {
            is Accion.Ir -> control.ir(a.lugar)
            is Accion.Consola -> control.consola(a.id)
            is Accion.Consulta -> control.buscar(a.texto)
            is Accion.Filtrar -> control.filtrar(a.filtro)
            is Accion.Seleccionar -> control.seleccionar(a.clave)
            is Accion.Abrir -> control.abrir(a.ventana)
            is Accion.Favorito -> control.favorito(a.clave)
            is Accion.Ocultar -> control.ocultar(a.clave)
            is Accion.Paso -> control.paso(a.clave, a.texto)
            is Accion.Emulador -> control.emulador(a.clave, a.id)
            is Accion.Jugar -> control.jugar(a.clave, a.pantalla)
            is Accion.Estilo -> control.ambiente(a.ambiente)
            is Accion.Calendario -> control.calendario(a.activo)
            is Accion.Movimiento -> control.movimiento(a.activo)
            is Accion.DosPantallas -> control.usarDos(a.activo)
            is Accion.QuitarCarpeta -> control.quitarCarpeta(a.uri)
            is Accion.AsignarConsola -> control.asignarConsola(a.uri, a.id)
            is Accion.AgregarApp -> control.agregarApp(a.paquete)
            is Accion.BuscarArte -> control.buscarArte(a.clave)
            is Accion.Imagen -> { imagenPara = a.clave; imagen.launch(arrayOf("image/*")) }
            Accion.Carpeta -> carpeta.launch(null)
            Accion.Recargar -> control.escanear()
            Accion.Apps -> control.cargarApps()
            Accion.Exportar -> exportar.launch("Diptico-respaldo.zip")
            Accion.Importar -> importar.launch(arrayOf("application/zip", "application/octet-stream"))
            Accion.InicioAndroid -> startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
            Accion.CerrarAviso -> control.avisar(null)
            Accion.Limpiar -> control.limpiar()
            Accion.Volver -> control.volver()
            Accion.Lista -> control.lista()
        }
    }

    private fun tecladoVisible() = ViewCompat.getRootWindowInsets(window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
    private fun volver() {
        if (tecladoVisible()) WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
        else control.volver()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1 -> {
                if (event.action == KeyEvent.ACTION_UP && control.estado.value.ventana == null && !tecladoVisible()) {
                    val lugares = Lugar.entries
                    val paso = if (event.keyCode == KeyEvent.KEYCODE_BUTTON_L1) -1 else 1
                    control.ir(lugares[(lugares.indexOf(control.estado.value.lugar) + paso).mod(lugares.size)])
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_B -> { if (event.action == KeyEvent.ACTION_UP) volver(); return true }
            KeyEvent.KEYCODE_BUTTON_Y -> { if (event.action == KeyEvent.ACTION_UP) control.estado.value.protagonista?.let { control.abrir(Ventana.Juego(it.clave)) }; return true }
            KeyEvent.KEYCODE_BUTTON_A -> return super.dispatchKeyEvent(KeyEvent(event.downTime, event.eventTime, event.action,
                KeyEvent.KEYCODE_DPAD_CENTER, event.repeatCount, event.metaState, event.deviceId, event.scanCode, event.flags, event.source))
        }
        return super.dispatchKeyEvent(event)
    }
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean = mando.mover(event) || super.dispatchGenericMotionEvent(event)
}

class RenacerInferiorActivity : RenacerActivity() {
    override val inferior = true
    companion object { var viva = false; private set }
    override fun onCreate(savedInstanceState: Bundle?) { viva = true; super.onCreate(savedInstanceState) }
    override fun onDestroy() { viva = false; super.onDestroy() }
}

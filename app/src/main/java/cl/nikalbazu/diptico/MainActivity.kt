package cl.nikalbazu.diptico

import cl.nikalbazu.diptico.estado.*

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cl.nikalbazu.diptico.emu.Pantallas
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.ui.Intercambio
import cl.nikalbazu.diptico.ui.Lienzo
import cl.nikalbazu.diptico.ui.Raiz
import cl.nikalbazu.diptico.ui.Rol
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * La pantalla principal del equipo (la de arriba en la Thor).
 * El tutorial y el modo una pantalla usan una sola interfaz adaptada al tamaño del equipo.
 */
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val e by Diptico.estado.collectAsState()
            val mi = miDisplay()
            val t = e.tamTexto
            // En la tele: todo más grande y con margen para que no se corte nada en los bordes.
            val z = Diptico.zoomAutomatico(e) * (if (e.sobremesa) e.sobremesaTam else 1f)
            val margen = if (e.sobremesa) e.sobremesaMargen else 0
            Lienzo(e.oscuro, Modifier.fillMaxSize().margenPorcentaje(margen), t, z) {
                if (e.unaPantalla) Raiz(e, Rol.UNICA, mi)
                else Intercambio(Diptico.intercambioEfectivo(e), esArriba = true) { inter ->
                    if (inter) Raiz(e, Rol.INFERIOR, mi, rolIntro = Rol.SUPERIOR) else Raiz(e, Rol.SUPERIOR, mi)
                }
            }
        }
        // Si cambias a dos pantallas (o a "abajo en negro") se abre la de abajo.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    Diptico.estado.map { it.configurado }.distinctUntilChanged().collect { configurado ->
                        if (configurado) pedirPermisoAvisos()
                    }
                }
                launch {
                    // Protector de pantalla: la pantalla principal también se atenúa.
                    Diptico.estado.map { it.protector }.distinctUntilChanged().collect { p ->
                        val lp = window.attributes
                        lp.screenBrightness = if (p) 0.08f else android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        window.attributes = lp
                    }
                }
                Diptico.estado.map { it.modoPantallas to it.dosDisponibles }.distinctUntilChanged().collect { (modo, hayInferior) ->
                    if (hayInferior && modo != "una_libre") abrirPantallaInferior()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Diptico.estado.value.modoPantallas != "una_libre") abrirPantallaInferior()
    }

    private fun pedirPermisoAvisos() {
        if (android.os.Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        val prefs = getSharedPreferences("diptico_permisos", MODE_PRIVATE)
        if (prefs.getBoolean("avisos_pedidos", false)) return
        prefs.edit().putBoolean("avisos_pedidos", true).apply()
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 41)
    }

    private fun abrirPantallaInferior() {
        val inferior = Pantallas.inferior(this) ?: return
        if (InferiorActivity.viva) return
        val intent = Intent(this, InferiorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opciones = ActivityOptions.makeBasic().setLaunchDisplayId(inferior.displayId)
        try {
            startActivity(intent, opciones.toBundle())
        } catch (e: Exception) {
            Diptico.avisar("No pude abrir la pantalla de abajo")
        }
    }
}

/** Deja un margen (en % del ancho y del alto) alrededor: para teles que cortan los bordes de la imagen. */
fun Modifier.margenPorcentaje(pct: Int): Modifier = if (pct <= 0) this else this.layout { medible, c ->
    val px = (c.maxWidth * pct / 100f).toInt()
    val py = (c.maxHeight * pct / 100f).toInt()
    val pl = medible.measure(c.copy(minWidth = 0, minHeight = 0, maxWidth = (c.maxWidth - 2 * px).coerceAtLeast(0), maxHeight = (c.maxHeight - 2 * py).coerceAtLeast(0)))
    layout(c.maxWidth, c.maxHeight) { pl.place(px, py) }
}


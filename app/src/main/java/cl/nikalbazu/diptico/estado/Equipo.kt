package cl.nikalbazu.diptico.estado

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.InputDevice

/**
 * Qué equipo es: consola de dos pantallas, consola portátil, teléfono, tablet o tele.
 * Con eso Díptico elige la mejor distribución (una o dos pantallas, vista de juegos) y cuántos efectos usar.
 */
data class Equipo(
    val nombre: String,
    /** "dual", "portatil", "telefono", "tablet" o "tele". */
    val tipo: String,
    val ramGb: Int,
    /** "alta", "media" o "baja": cuánta potencia tiene (para los efectos). */
    val gama: String,
    val conMando: Boolean,
) {
    val tipoBonito: String get() = when (tipo) {
        "dual" -> "Consola de dos pantallas"; "portatil" -> "Consola portátil"; "telefono" -> "Teléfono"; "tablet" -> "Tablet"; else -> "Tele"
    }
}

object Equipos {
    /** Consolas conocidas por su marca o modelo (en minúsculas) -> nombre bonito. */
    private val CONOCIDAS = listOf(
        "thor" to "AYN Thor", "odin 2 portal" to "AYN Odin 2 Portal", "odin2 portal" to "AYN Odin 2 Portal", "odin 2 mini" to "AYN Odin 2 Mini",
        "odin2 mini" to "AYN Odin 2 Mini", "odin 2" to "AYN Odin 2", "odin2" to "AYN Odin 2", "odin" to "AYN Odin",
        "retroid pocket 5" to "Retroid Pocket 5", "retroid pocket mini" to "Retroid Pocket Mini", "retroid pocket 4" to "Retroid Pocket 4",
        "retroid pocket flip" to "Retroid Pocket Flip", "retroid" to "Retroid Pocket",
        "rg556" to "Anbernic RG556", "rg cube" to "Anbernic RG Cube", "rg406" to "Anbernic RG406", "rg405" to "Anbernic RG405", "anbernic" to "Anbernic",
        "pocket ds" to "AYANEO Pocket DS", "pocket s" to "AYANEO Pocket S", "pocket dmg" to "AYANEO Pocket DMG", "ayaneo" to "AYANEO",
        "g cloud" to "Logitech G Cloud", "razer edge" to "Razer Edge", "trimui" to "TrimUI", "powkiddy" to "Powkiddy",
    )

    fun detectar(ctx: Context, hayPantallaAbajo: Boolean): Equipo {
        val marca = "${Build.MANUFACTURER} ${Build.BRAND} ${Build.MODEL} ${Build.DEVICE} ${Build.PRODUCT}".lowercase()
        val conocida = CONOCIDAS.firstOrNull { (clave, _) -> clave in marca }?.second
        val am = ctx.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
        val ram = Math.round(mi.totalMem / (1024.0 * 1024 * 1024)).toInt()
        val soc = if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL.lowercase() else ""
        val potente = listOf("sm8550", "sm8650", "sm8750", "sm8475", "sm8450", "sm8635", "sm8735").any { it in soc }
        val gama = when {
            potente || ram >= 11 -> "alta"
            am?.isLowRamDevice == true || ram < 4 -> "baja"
            else -> "media"
        }
        // ¿Tiene controles propios (no un control conectado por Bluetooth)?
        val conMando = try {
            InputDevice.getDeviceIds().any { id ->
                val d = InputDevice.getDevice(id) ?: return@any false
                val s = d.sources
                (s and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD || s and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) && !d.isExternal
            }
        } catch (_: Exception) { false }
        val cfg = ctx.resources.configuration
        val tele = (cfg.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
        val pulgadas = ctx.resources.displayMetrics.let { m -> Math.hypot(m.widthPixels / m.xdpi.toDouble(), m.heightPixels / m.ydpi.toDouble()) }
        val tipo = when {
            tele -> "tele"
            hayPantallaAbajo -> "dual"
            conocida != null || conMando -> "portatil"
            cfg.smallestScreenWidthDp >= 600 || pulgadas >= 7.5 -> "tablet"
            else -> "telefono"
        }
        val nombre = conocida ?: "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".trim()
        return Equipo(nombre, tipo, ram, gama, conMando)
    }
}

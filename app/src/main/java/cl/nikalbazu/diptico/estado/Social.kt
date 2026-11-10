package cl.nikalbazu.diptico.estado

import android.app.Activity
import android.util.Log
import cl.nikalbazu.diptico.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Un amigo de Discord, como lo entrega el kit oficial de Discord (Social SDK). */
data class AmigoDiscord(
    val id: String,
    val nombre: String,
    val avatar: String? = null,
    /** "jugando", "conectado", "ausente", "ocupado" o "desconectado". */
    val estado: String = "desconectado",
    /** A qué juega, si está jugando (el nombre que muestra Discord). */
    val actividad: String? = null,
    /** Detalle de la actividad (nivel, modo, «en el menú»…). */
    val detalle: String? = null,
    /** Juega desde Díptico. */
    val enDiptico: Boolean = false,
)

/** Las funciones en C++ (app/src/main/cpp/puente_discord.cpp). Solo se llaman desde el hilo «discord». */
object PuenteDiscord {
    @JvmStatic external fun iniciar(app: Long): Boolean
    @JvmStatic external fun correr()
    @JvmStatic external fun permisos(): ByteArray
    @JvmStatic external fun autorizar()
    @JvmStatic external fun cancelar()
    @JvmStatic external fun usarToken(acceso: ByteArray)
    @JvmStatic external fun renovar(refresco: ByteArray)
    @JvmStatic external fun salir()
    @JvmStatic external fun presencia(titulo: ByteArray, consola: ByteArray, imagen: ByteArray, inicio: Long)
    @JvmStatic external fun sinPresencia()

    /** Lo llama el C++ (dentro de correr()) con lo que respondió Discord. */
    @JvmStatic fun evento(tipo: String, a: ByteArray, b: ByteArray, n: Long) {
        try { SocialDiscord.alEvento(tipo, String(a, Charsets.UTF_8), String(b, Charsets.UTF_8), n) }
        catch (ex: Exception) { Log.w("DipticoDiscord", "evento $tipo", ex) }
    }
}

/**
 * Lo social de Discord con su kit oficial (Social SDK): iniciar sesión, tus amigos y quién juega, y tu «Jugando a…».
 * Si esta versión no trae el kit ([disponible] = false), la ventana de amigos lo explica y queda el QR por internet.
 */
object SocialDiscord {
    private val _amigos = MutableStateFlow<List<AmigoDiscord>>(emptyList())
    val amigos: StateFlow<List<AmigoDiscord>> = _amigos

    /** "apagado", "conectando", "listo" o "error". */
    private val _conexion = MutableStateFlow("apagado")
    val conexion: StateFlow<String> = _conexion

    /** El kit de Discord está dentro de esta versión de Díptico (y se pudo cargar). */
    @Volatile var disponible: Boolean = BuildConfig.DISCORD_KIT

    private val hilo = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "discord").apply { isDaemon = true } }
    @Volatile private var iniciado = false
    @Volatile private var primeraVez = true

    fun publicar(lista: List<AmigoDiscord>) { _amigos.value = lista }

    /** Orden de la lista: primero los que juegan, luego conectados, ausentes y al final los desconectados. */
    fun ordenar(l: List<AmigoDiscord>): List<AmigoDiscord> = l.sortedWith(
        compareBy<AmigoDiscord> { when (it.estado) { "jugando" -> 0; "conectado" -> 1; "ocupado" -> 2; "ausente" -> 3; else -> 4 } }
            .thenBy { it.nombre.lowercase() },
    )

    /**
     * Cada pantalla de Díptico se presenta al kit cuando queda al frente: así la ventana de «Autorizar» se abre en la
     * pantalla que estás usando. La primera vez además reconecta la cuenta guardada.
     */
    fun alFrente(act: Activity) {
        if (!disponible) return
        try {
            Class.forName("com.discord.socialsdk.DiscordSocialSdkInit").getMethod("setEngineActivity", Activity::class.java).invoke(null, act)
        } catch (ex: Throwable) {
            Log.w("DipticoDiscord", "sin kit", ex); disponible = false; return
        }
        if (primeraVez) { primeraVez = false; Diptico.reconectarDiscord() }
    }

    /** Arranca el kit en su hilo (una vez) y lo hace correr cada 200 ms. */
    private fun enHilo(trabajo: () -> Unit) {
        if (!disponible) return
        hilo.execute {
            try {
                if (!iniciado) {
                    System.loadLibrary("diptico_discord")
                    val app = Diptico.appDiscord().toLongOrNull() ?: run { Diptico.errorDiscord("Falta el Application ID de Díptico"); return@execute }
                    iniciado = PuenteDiscord.iniciar(app)
                    if (!iniciado) return@execute
                    hilo.scheduleWithFixedDelay({ try { PuenteDiscord.correr() } catch (ex: Throwable) { Log.w("DipticoDiscord", "correr", ex) } },
                        0, 200, TimeUnit.MILLISECONDS)
                }
                trabajo()
            } catch (ex: Throwable) {
                Log.w("DipticoDiscord", "kit", ex)
                if (ex is UnsatisfiedLinkError) disponible = false
                Diptico.errorDiscord("El kit de Discord no arrancó (${ex.javaClass.simpleName})")
            }
        }
    }

    /** Los permisos que pide el kit (para el QR por internet). */
    fun permisos(): String? = if (!iniciado) null else try { String(PuenteDiscord.permisos(), Charsets.UTF_8) } catch (_: Throwable) { null }

    /** Inicia sesión en esta consola: se abre Discord, tocas «Autorizar» y vuelves solo. */
    fun conectarAqui() { _conexion.value = "conectando"; enHilo { PuenteDiscord.autorizar() } }

    fun cancelar() = enHilo { PuenteDiscord.cancelar() }

    fun usarToken(acceso: String) { _conexion.value = "conectando"; enHilo { PuenteDiscord.usarToken(acceso.toByteArray()) } }

    fun renovar(refresco: String) { _conexion.value = "conectando"; enHilo { PuenteDiscord.renovar(refresco.toByteArray()) } }

    fun salir() {
        _amigos.value = emptyList(); _conexion.value = "apagado"
        if (iniciado) enHilo { PuenteDiscord.salir() }
    }

    /** Tu perfil de Discord dice «Jugando a [titulo]» (en [consola]), con el tiempo desde [inicio]. */
    fun jugando(titulo: String, consola: String, imagen: String?, inicio: Long) {
        if (!iniciado) return
        val t = titulo.take(120).padEnd(2, ' ')
        val c = consola.take(120).let { if (it.length < 2) "" else it }
        val img = imagen?.takeIf { it.startsWith("https://") && it.length <= 300 }.orEmpty()
        enHilo { PuenteDiscord.presencia(t.toByteArray(), c.toByteArray(), img.toByteArray(), inicio) }
    }

    fun sinJuego() { if (iniciado) enHilo { PuenteDiscord.sinPresencia() } }

    internal fun alEvento(tipo: String, a: String, b: String, n: Long) {
        when (tipo) {
            "estado" -> _conexion.value = when (n.toInt()) { 3 -> "listo"; 0 -> if (_conexion.value == "error") "error" else "apagado"; else -> "conectando" }
            "token" -> Diptico.tokenDiscord(a, b, n)
            "yo" -> if (a.isNotBlank()) {
                val j = JSONObject(a)
                Diptico.cuentaDiscord(j.optString("nombre"), j.optString("avatar"))
            }
            "amigos" -> {
                val arr = JSONArray(a)
                publicar(List(arr.length()) { i ->
                    val j = arr.getJSONObject(i)
                    AmigoDiscord(
                        id = j.optString("id"), nombre = j.optString("nombre"),
                        avatar = j.optString("avatar").takeIf { it.isNotBlank() },
                        estado = j.optString("estado", "desconectado"),
                        actividad = j.optString("actividad").takeIf { it.isNotBlank() },
                        detalle = j.optString("detalle").takeIf { it.isNotBlank() },
                        enDiptico = j.optString("estado") == "jugando",
                    )
                })
            }
            "vence" -> Diptico.reconectarDiscord(forzarRenovar = true)
            "error" -> {
                Log.w("DipticoDiscord", "$b: $a ($n)")
                _conexion.value = "error"
                Diptico.errorDiscord(motivo(a, b))
            }
        }
    }

    private fun motivo(a: String, donde: String): String {
        val t = a.lowercase()
        return when {
            donde == "presencia" -> "Discord no aceptó el «Jugando a…»"
            "cancel" in t || "denied" in t -> "Cancelaste el permiso en Discord"
            "invalid_client" in t || "unauthorized" in t -> "Discord no aceptó la app: falta activar «Public Client» en su portal"
            "redirect" in t -> "Falta la dirección de vuelta (Redirect) en el portal de Discord"
            "invalid_grant" in t -> "Tu sesión de Discord venció: conéctate de nuevo"
            else -> "Discord: " + a.take(90).ifBlank { "no respondió" }
        }
    }
}

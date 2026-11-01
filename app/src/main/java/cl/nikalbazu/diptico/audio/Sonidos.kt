package cl.nikalbazu.diptico.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock

/** Sonidos cortos al moverte, elegir, volver y abrir juegos. Se pueden apagar en Menú › Música. */
object Sonidos {
    private var pool: SoundPool? = null
    private val ids = HashMap<String, Int>()
    private var ultimoMover = 0L

    @Volatile
    var activo = true

    @Volatile
    var volumen = 0.6f

    /** Juego de sonidos del tema ("" = los normales, "bloques" = madera y pops). */
    @Volatile
    var juego = ""

    /** Juego elegido en los ajustes: "tema" (el del tema), "clasico", "bloques", "retro" o "burbuja". */
    @Volatile
    var paquete = "tema"

    val PAQUETES = listOf("tema", "clasico", "bloques", "retro", "burbuja")

    private fun prefijo(): String = when (if (paquete == "tema") juego else paquete) {
        "bloques" -> "b_"; "retro" -> "r_"; "burbuja" -> "u_"; else -> ""
    }

    fun iniciar(ctx: Context) {
        if (pool != null) return
        val p = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build()
        val app = ctx.applicationContext
        // Todos los juegos tienen los mismos nombres: "" clásicos, "b_" bloques, "r_" retro, "u_" burbuja.
        val nombres = listOf("mover", "consola", "aceptar", "atras", "abrir", "jugar", "borde", "pagina", "deslizar", "tecla", "si", "no", "levantar", "soltar", "aviso")
        for (pre in listOf("", "b_", "r_", "u_")) for (n in nombres) {
            val r = app.resources.getIdentifier("sfx_$pre$n", "raw", app.packageName)
            if (r != 0) ids["$pre$n"] = p.load(app, r, 1)
        }
        pool = p
    }

    private var flujoMover = 0

    private fun tocar(nombre: String, tono: Float = 1f): Int {
        if (!activo) return 0
        val id = ids[prefijo() + nombre] ?: ids[nombre] ?: return 0
        return pool?.play(id, volumen, volumen, 1, 0, tono) ?: 0
    }

    fun mover() {
        // Si mantienes la cruceta no se amontonan.
        val ahora = SystemClock.uptimeMillis()
        if (ahora - ultimoMover < 45) return
        ultimoMover = ahora
        flujoMover = tocar("mover", 0.96f + (ahora % 7) * 0.012f)
    }

    fun consola() { tocar("consola") }
    fun aceptar() { tocar("aceptar") }
    fun atras() { tocar("atras") }
    fun abrir() { tocar("abrir") }
    fun jugar() { tocar("jugar") }

    private var ultimoBorde = 0L

    /** Llegaste al final de una lista (no se repite mientras mantienes la cruceta). */
    fun borde() {
        val ahora = SystemClock.uptimeMillis()
        if (ahora - ultimoBorde < 260) return
        ultimoBorde = ahora
        // En vez del "tic" de moverse, que no llevó a ningún lado.
        if (ahora - ultimoMover < 80 && flujoMover != 0) pool?.stop(flujoMover)
        tocar("borde")
    }
    fun pagina() { tocar("pagina") }
    fun deslizar() { tocar("deslizar") }
    fun tecla() {
        val ahora = SystemClock.uptimeMillis()
        tocar("tecla", 0.95f + (ahora % 5) * 0.025f)
        Unit
    }
    fun interruptor(encendido: Boolean) { tocar(if (encendido) "si" else "no") }
    fun levantar() { tocar("levantar") }
    fun soltar() { tocar("soltar") }
    fun aviso() { tocar("aviso") }

    /** Para escuchar un juego al elegirlo en los ajustes. */
    fun muestra() { aceptar() }
}

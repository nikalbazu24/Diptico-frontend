package cl.nikalbazu.diptico.estado

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import java.io.File

/**
 * Qué tan caliente está la consola mientras juegas, y qué consejo darle a la persona.
 *
 * Solo usa lecturas que Android deja a cualquier app (sin permisos especiales):
 *  - la temperatura de la batería,
 *  - el estado térmico que avisa el sistema (cuando él mismo empieza a bajarle potencia),
 *  - el «margen térmico» que calcula el sistema (1,0 = a punto de limitarse),
 *  - y, si el equipo lo permite, la temperatura del procesador (en varias consolas viene bloqueada: ahí se ignora).
 * No se puede cambiar el ventilador ni la potencia: eso pide permisos de sistema. Por eso aquí solo se informa y se aconseja.
 * Leer es barato, pero toca archivos: hacerlo siempre fuera del hilo principal.
 */
object Termica {
    enum class Nivel { FRESCA, TIBIA, CALIENTE, MUY_CALIENTE }

    data class Lectura(
        /** °C de la batería (0 si no se pudo leer). */
        val bateria: Float,
        /** °C del procesador o la GPU (0 si el sistema no deja leerlo). */
        val procesador: Float,
        /** Estado térmico del sistema: 0 normal … 6 apagado de emergencia; -1 si no se sabe. */
        val estadoSistema: Int,
        /** Margen térmico (1,0 = límite); NaN si no se sabe. */
        val margen: Float,
        val cargando: Boolean,
        val nivelBateria: Int,
        val nivel: Nivel,
    ) {
        /** El número que se muestra: la temperatura más alta que se pudo leer. */
        val grados: Float get() = maxOf(bateria, procesador)
    }

    /** Un consejo listo para mostrar: el texto (sin números, así se traduce) y cómo se siente DIPPO al decirlo. */
    data class Consejo(val texto: String, val animo: Animo)

    enum class Animo { CONTENTO, PENSANDO, PREOCUPADO }

    /** Solo para pruebas y capturas: si trae algo, se usa en vez de leer el equipo. */
    @Volatile var pruebaLectura: Lectura? = null

    fun leer(ctx: Context): Lectura {
        pruebaLectura?.let { return it }
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val bateria = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val l = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val escala = (i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100).coerceAtLeast(1)
        val nivelBat = if (l >= 0) l * 100 / escala else -1
        val estado = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val cargando = estado == BatteryManager.BATTERY_STATUS_CHARGING

        val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val sistema = try { pm?.currentThermalStatus ?: -1 } catch (_: Exception) { -1 }
        val margen = if (Build.VERSION.SDK_INT >= 30) {
            try { pm?.getThermalHeadroom(0) ?: Float.NaN } catch (_: Exception) { Float.NaN }
        } else Float.NaN
        val cpu = leerProcesador()
        return Lectura(bateria, cpu, sistema, margen, cargando, nivelBat, nivelDe(bateria, cpu, sistema, margen))
    }

    /** El nivel manda lo más alto que digan las lecturas (así un solo sensor alarmado basta para avisar). */
    private fun nivelDe(bateria: Float, cpu: Float, sistema: Int, margen: Float): Nivel {
        val porBateria = when {
            bateria >= 44f -> Nivel.MUY_CALIENTE
            bateria >= 41f -> Nivel.CALIENTE
            bateria >= 38f -> Nivel.TIBIA
            else -> Nivel.FRESCA
        }
        val porProcesador = when {
            cpu >= 85f -> Nivel.MUY_CALIENTE
            cpu >= 75f -> Nivel.CALIENTE
            cpu >= 65f -> Nivel.TIBIA
            else -> Nivel.FRESCA
        }
        val porSistema = when {
            sistema >= 3 -> Nivel.MUY_CALIENTE
            sistema == 2 -> Nivel.CALIENTE
            sistema == 1 -> Nivel.TIBIA
            else -> Nivel.FRESCA
        }
        val porMargen = when {
            margen.isNaN() -> Nivel.FRESCA
            margen >= 1f -> Nivel.MUY_CALIENTE
            margen >= 0.9f -> Nivel.CALIENTE
            margen >= 0.8f -> Nivel.TIBIA
            else -> Nivel.FRESCA
        }
        return listOf(porBateria, porProcesador, porSistema, porMargen).maxByOrNull { it.ordinal } ?: Nivel.FRESCA
    }

    /** Busca en los sensores del sistema el más caliente entre CPU, GPU y SoC. Si Android no deja leerlos, devuelve 0. */
    private fun leerProcesador(): Float {
        var mayor = 0f
        try {
            val zonas = File("/sys/class/thermal").listFiles { f -> f.name.startsWith("thermal_zone") } ?: return 0f
            for (z in zonas) {
                val tipo = try { File(z, "type").readText().trim().lowercase() } catch (_: Exception) { continue }
                if (!(tipo.contains("cpu") || tipo.contains("gpu") || tipo.contains("soc"))) continue
                val bruto = try { File(z, "temp").readText().trim().toFloat() } catch (_: Exception) { continue }
                val c = if (bruto > 1000f) bruto / 1000f else bruto
                if (c in 15f..120f && c > mayor) mayor = c
            }
        } catch (_: Exception) {
        }
        return mayor
    }

    /** Consejos de calma que se van turnando cuando no pasa nada raro. */
    private val TURNO = listOf(
        "Cada 30 minutos mira algo lejano por 20 segundos: tus ojos lo agradecen.",
        "¿Tomaste agua? Aprovecha un momento tranquilo del juego.",
        "Estira las manos y las muñecas entre partidas.",
        "Con el brillo más bajo la consola dura más y se calienta menos.",
        "Si el juego va a tirones, prueba bajar la resolución o los efectos en sus ajustes.",
    )

    /**
     * El consejo que toca ahora, o null si no hay nada de calor ni de batería que decir (ahí manda el de siempre).
     * Solo habla de lo físico; los avisos de reintentos y descansos los pone la pantalla.
     */
    fun consejoUrgente(l: Lectura?, exigente: Boolean = false): Consejo? {
        if (l == null) return null
        return when {
            l.nivel == Nivel.MUY_CALIENTE ->
                Consejo("Está muy caliente. Haz una pausa, baja el brillo y sácale la funda para que respire.", Animo.PREOCUPADO)
            l.nivel == Nivel.CALIENTE && exigente ->
                Consejo("Este emulador exige harto. Baja la resolución interna a 1x o 2x en sus ajustes y se va a enfriar.", Animo.PENSANDO)
            l.nivel == Nivel.CALIENTE && l.cargando ->
                Consejo("Jugar enchufado la calienta más. Desenchúfala un rato o apóyala en un lugar ventilado.", Animo.PENSANDO)
            l.nivel == Nivel.CALIENTE ->
                Consejo("Se está calentando. Baja un poco el brillo y déjala respirar.", Animo.PENSANDO)
            !l.cargando && l.nivelBateria in 1..15 ->
                Consejo("Te queda poca batería. Guarda tu partida y enchúfala pronto.", Animo.PREOCUPADO)
            else -> null
        }
    }

    /** Un consejo tranquilo del turno, que cambia cada vez que sube [bloque]. */
    fun consejoDeTurno(bloque: Int): String = TURNO[Math.floorMod(bloque, TURNO.size)]

    // ---------- Según el juego ----------

    /** Consolas cuyos emuladores le exigen harto al procesador (las que más calientan). */
    private val EXIGENTES = setOf("switch", "ps3", "ps2", "wiiu", "xbox", "xbox360", "psvita", "n3ds", "gc", "wii", "dreamcast", "saturn")

    fun exigente(consolaId: String?): Boolean = consolaId != null && consolaId.lowercase() in EXIGENTES

    /** Grados desde los que un juego queda marcado como «suele calentarla». */
    const val CALIENTA = 41f

    /** El punto más caliente al que llegó cada juego (clave → °C), guardado entre partidas. */
    private var picos: MutableMap<String, Float>? = null

    private fun cargarPicos(): MutableMap<String, Float> {
        picos?.let { return it }
        val m = HashMap<String, Float>()
        try {
            Diptico.almacen.texto("termicaPicos").split('\n').forEach { linea ->
                val i = linea.lastIndexOf('\t')
                if (i > 0) linea.substring(i + 1).toFloatOrNull()?.let { m[linea.substring(0, i)] = it }
            }
        } catch (_: Exception) {
        }
        picos = m
        return m
    }

    /** Lo más caliente que llegó este juego (0 si nunca se midió). */
    fun picoDe(clave: String): Float = cargarPicos()[clave] ?: 0f

    /** Anota la lectura de la partida: si es la más caliente de ese juego, se guarda. */
    fun registrar(clave: String, l: Lectura) {
        val g = l.grados
        if (g <= 0f) return
        val m = cargarPicos()
        if (g <= (m[clave] ?: 0f) + 0.4f) return
        m[clave] = g
        try {
            Diptico.almacen.guardarTexto("termicaPicos", m.entries.sortedByDescending { it.value }.take(300).joinToString("\n") { "${it.key}\t${"%.1f".format(java.util.Locale.US, it.value)}" })
        } catch (_: Exception) {
        }
    }

    /** Al empezar la partida: si el juego suele calentarla, un consejo para prevenir (sin esperar a que se caliente). */
    fun consejoAlEmpezar(clave: String, consolaId: String?): Consejo? {
        if (picoDe(clave) < CALIENTA) return null
        return if (exigente(consolaId))
            Consejo("Este juego suele calentarla. Juega sin la funda y, si puedes, baja la resolución interna del emulador.", Animo.PENSANDO)
        else Consejo("Este juego suele calentarla. Juega sin la funda y en un lugar ventilado.", Animo.PENSANDO)
    }

    /** Resumen para Ajustes: qué sensores deja leer este equipo (sirve para saber si la lectura es completa). */
    fun resumenSensores(l: Lectura): String = buildString {
        append(if (l.bateria > 0f) "Batería ${"%.0f".format(l.bateria)} °C" else "Batería sin dato")
        append(" · ")
        append(if (l.procesador > 0f) "Procesador ${"%.0f".format(l.procesador)} °C" else "Procesador bloqueado")
        if (l.estadoSistema >= 0) append(" · Sistema ${l.estadoSistema}/6")
    }
}

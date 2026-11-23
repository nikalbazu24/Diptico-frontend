package cl.nikalbazu.diptico.estado

import android.content.Context
import android.net.Uri
import cl.nikalbazu.diptico.data.DatosJuego
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.emu.Lanzador
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Apps amigas de Díptico: se muestran en la biblioteca, se abren como lanzador y comparten lo que saben.
 *
 * - PokeDaisy (com.pokedaisy.app, lidor30/pokedaisy): juegos de Game Boy, Color, Advance y DS con compañero en la otra
 *   pantalla. Entrada para lanzadores: `com.pokedaisy.app/.LaunchActivity` con VIEW + la ROM. Va primero en los juegos de
 *   Pokémon si está instalada.
 * - Peanuts Native (dev.nxnative.android.peanuts, igawa6/peanuts-native): juegos de Switch 1 convertidos a apps nativas
 *   (archivos .nup). Entrada fija `dev.peanuts.launch.LaunchActivity` con el extra `game` = ruta del .nup.
 * - Biomeria Launcher (com.prissma.launcher, Nikalbazu24/Prissma-Launcher): Minecraft Java, Bedrock y Rust. Comparte sus
 *   instancias por `content://com.prissma.launcher.provider/instances` (nombre, motor, versión, loader, horas y enlace
 *   `prissma://launch?instance_id=…`). Díptico las muestra como juegos de la carpeta «Minecraft», las abre con ese enlace
 *   (con `source_frontend` para que Biomeria sepa volver) y usa las horas de Biomeria si son más que las suyas.
 */
object Integraciones {
    const val POKEDAISY = "com.pokedaisy.app"
    const val PEANUTS = "dev.nxnative.android.peanuts"
    const val BIOMERIA = "com.prissma.launcher"

    /** Nombres de las órdenes en sistemas.json (son también lo que se guarda como emulador elegido). */
    const val ORDEN_POKEDAISY = "PokeDaisy (Standalone)"
    const val ORDEN_PEANUTS = "Peanuts Native (Port nativo)"

    const val CONSOLA_BIOMERIA = "biomeria"
    const val EXT_BIOMERIA = "biomeria"
    private val CATALOGO_BIOMERIA: Uri = Uri.parse("content://$BIOMERIA.provider/instances")

    /** Consolas que abre PokeDaisy. */
    private val CONSOLAS_POKEDAISY = setOf("gb", "gbc", "gba", "nds")

    /** Una instancia de Minecraft que Biomeria comparte. */
    data class Instancia(
        val id: String,
        val titulo: String,
        /** "java", "bedrock" o "rust". */
        val motor: String,
        val version: String,
        val loader: String,
        val segundos: Long,
        val enlace: String,
    ) {
        val clave: String get() = "$CONSOLA_BIOMERIA/$id"

        /** «Java 1.21.1 · Fabric» */
        val detalle: String
            get() = listOf(
                when (motor) { "java" -> "Java"; "bedrock" -> "Bedrock"; "rust" -> "Rust"; else -> motor.replaceFirstChar { it.uppercase() } } +
                    (if (version.isNotBlank()) " $version" else ""),
                loader.takeIf { it.isNotBlank() && !it.equals("vanilla", true) }?.replaceFirstChar { it.uppercase() },
            ).filterNotNull().joinToString(" · ")
    }

    /** Lo último que Biomeria contó (se refresca en segundo plano). */
    @Volatile var instancias: List<Instancia> = emptyList()
        internal set

    /** Las instancias de Biomeria, o null si no está instalada o no responde. */
    fun leerBiomeria(ctx: Context): List<Instancia>? {
        if (!Lanzador.instalado(ctx, BIOMERIA)) return null
        return try {
            ctx.contentResolver.query(CATALOGO_BIOMERIA, null, null, null, null)?.use { c ->
                fun col(n: String) = c.getColumnIndex(n)
                val iId = col("instance_id"); val iTit = col("title"); val iMot = col("engine"); val iVer = col("mc_version")
                val iLoa = col("loader"); val iSeg = col("playtime_seconds"); val iEnl = col("deep_link")
                if (iId < 0) return@use emptyList()
                buildList {
                    while (c.moveToNext()) {
                        val id = c.getString(iId)?.takeIf { it.isNotBlank() } ?: continue
                        add(
                            Instancia(
                                id = id,
                                titulo = (if (iTit >= 0) c.getString(iTit) else null)?.takeIf { it.isNotBlank() } ?: "Minecraft",
                                motor = if (iMot >= 0) c.getString(iMot).orEmpty() else "",
                                version = if (iVer >= 0) c.getString(iVer).orEmpty() else "",
                                loader = if (iLoa >= 0) c.getString(iLoa).orEmpty() else "",
                                segundos = if (iSeg >= 0) c.getLong(iSeg).coerceAtLeast(0) else 0L,
                                enlace = (if (iEnl >= 0) c.getString(iEnl) else null)?.takeIf { it.startsWith("prissma://") }
                                    ?: "prissma://launch?instance_id=${Uri.encode(id)}",
                            ),
                        )
                    }
                }
            } ?: emptyList()
        } catch (_: Exception) {
            // Versiones de Biomeria sin catálogo o que se cerraron: se muestra solo el acceso a la app.
            emptyList()
        }
    }

    /** Enlace con el que Biomeria abre la instancia y sabe que vino desde Díptico (para volver al terminar). */
    fun enlaceDesdeDiptico(ctx: Context, enlace: String): String =
        Uri.parse(enlace).buildUpon().appendQueryParameter("source_frontend", ctx.packageName).build().toString()

    /**
     * Los juegos de la carpeta «Minecraft»: una instancia = un juego. Cada uno es un archivo chico en filesDir/biomeria con
     * el enlace adentro (lo lee la orden `%DATA%=%INJECT%=%ROM%` de sistemas.json). Si Biomeria está instalada pero no
     * comparte instancias todavía, queda un solo juego que abre la app.
     */
    fun juegosBiomeria(ctx: Context): List<Juego> {
        if (!Lanzador.instalado(ctx, BIOMERIA)) return emptyList()
        val dir = java.io.File(ctx.filesDir, "biomeria").apply { mkdirs() }
        val lista = instancias.ifEmpty { listOf(Instancia("app", "Biomeria Launcher", "", "", "", 0L, "prissma://launch")) }
        val usados = HashSet<String>()
        val juegos = lista.mapNotNull { i ->
            try {
                val seguro = i.id.replace(Regex("[^A-Za-z0-9_-]"), "_").take(60)
                val nombre = "$seguro.$EXT_BIOMERIA"
                usados += nombre
                val f = java.io.File(dir, nombre)
                val enlace = enlaceDesdeDiptico(ctx, i.enlace)
                if (!f.exists() || f.readText().trim() != enlace) f.writeText(enlace)
                Juego(i.clave, CONSOLA_BIOMERIA, i.titulo, nombre, Uri.fromFile(f), "", null)
            } catch (_: Exception) { null }
        }
        dir.listFiles()?.filter { it.name !in usados }?.forEach { it.delete() }
        return juegos
    }

    /** La instancia de Biomeria detrás de un juego de Díptico. */
    fun instanciaDe(clave: String): Instancia? = instancias.firstOrNull { it.clave == clave }

    /**
     * El emulador que conviene si nadie eligió uno a mano: los juegos de Pokémon de GB/GBC/GBA/DS con PokeDaisy y los
     * .nup de Switch con Peanuts Native, si están instalados.
     */
    fun recomendado(ctx: Context, juego: Juego): String? = when {
        juego.consolaId in CONSOLAS_POKEDAISY && esPokemon(juego.titulo) && Lanzador.instalado(ctx, POKEDAISY) -> ORDEN_POKEDAISY
        juego.consolaId == "switch" && juego.tipo == "nup" && Lanzador.instalado(ctx, PEANUTS) -> ORDEN_PEANUTS
        else -> null
    }

    fun esPokemon(titulo: String): Boolean =
        Busqueda.normalizar(titulo).replace(Regex("[^a-z]"), "").contains("pokemon")
}

/** Pide a Biomeria sus instancias (en segundo plano) y, si cambiaron, actualiza la biblioteca y las horas jugadas. */
fun Diptico.refrescarBiomeria() {
    scope.launch {
        val leidas = withContext(Dispatchers.IO) { Integraciones.leerBiomeria(app) } ?: emptyList()
        if (leidas == Integraciones.instancias && estado.value.juegos.any { it.consolaId == Integraciones.CONSOLA_BIOMERIA } == Lanzador.instalado(app, Integraciones.BIOMERIA)) return@launch
        Integraciones.instancias = leidas
        // Horas compartidas: si Biomeria contó más tiempo (jugaste sin pasar por Díptico), Díptico usa ese.
        val datos = estado.value.datos.toMutableMap()
        var cambio = false
        for (i in leidas) {
            val d = datos[i.clave] ?: DatosJuego()
            if (i.segundos > d.tiempo) { datos[i.clave] = d.copy(tiempo = i.segundos); cambio = true }
        }
        if (cambio) {
            almacen.guardarDatos(datos)
            actualizar { it.copy(datos = datos) }
        }
        publicarJuegos()
    }
}

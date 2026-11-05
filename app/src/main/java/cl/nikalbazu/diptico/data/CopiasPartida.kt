package cl.nikalbazu.diptico.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.security.MessageDigest

/** Copias manuales de SRAM que el usuario selecciona. Nunca modifica el archivo del emulador. */
internal object CopiasPartida {
    private const val LIMITE = 16L * 1024 * 1024

    private fun carpeta(ctx: Context, clave: String): File {
        val id = MessageDigest.getInstance("SHA-256").digest(clave.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(ctx.filesDir, "copias-partida/$id")
    }

    fun listar(ctx: Context, clave: String): List<File> =
        carpeta(ctx, clave).listFiles()?.filter { it.isFile && it.extension == "srm" }
            ?.sortedByDescending { it.name }.orEmpty()

    @Synchronized
    fun guardar(ctx: Context, clave: String, uri: Uri) {
        val cr = ctx.contentResolver
        val nombre = cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
        require(nombre?.endsWith(".srm", ignoreCase = true) == true) { "Elige un archivo .srm de RetroArch" }
        val dir = carpeta(ctx, clave).apply { mkdirs() }
        val temporal = File.createTempFile("copia-", ".tmp", dir)
        try {
            val n = requireNotNull(cr.openInputStream(uri)).use { origen ->
                temporal.outputStream().use { destino ->
                    val bloque = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val leidos = origen.read(bloque)
                        if (leidos < 0) break
                        total += leidos
                        require(total <= LIMITE)
                        destino.write(bloque, 0, leidos)
                    }
                    total
                }
            }
            require(n > 0)
            val final = File(dir, "${System.currentTimeMillis()}-${java.util.UUID.randomUUID()}.srm")
            check(temporal.renameTo(final))
            listar(ctx, clave).drop(3).forEach { it.delete() }
        } finally {
            temporal.delete()
        }
    }
}

package cl.nikalbazu.diptico.estado

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * El dock de apps viene armado: las apps más útiles que ya están en el equipo (tienda, navegador, ajustes, archivos,
 * fotos o YouTube). Después se suman con «+» en el mismo dock y se sacan manteniéndolas presionadas.
 */
internal fun dockPredeterminado(ctx: Context): List<String> {
    val pm = ctx.packageManager
    fun abre(pkg: String) = runCatching { pm.getLaunchIntentForPackage(pkg) != null }.getOrDefault(false)
    fun porIntencion(i: Intent) = runCatching { pm.resolveActivity(i, 0)?.activityInfo?.packageName }.getOrNull()
        ?.takeIf { it != "android" && abre(it) }
    val navegador = porIntencion(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
    val candidatos = listOf(
        "com.android.vending", navegador, "com.android.settings",
        "com.google.android.youtube", "com.google.android.apps.photos", "com.google.android.documentsui", "com.android.documentsui",
        "com.spotify.music", "com.discord",
    )
    return candidatos.filterNotNull().filter { abre(it) }.distinct().take(4)
}

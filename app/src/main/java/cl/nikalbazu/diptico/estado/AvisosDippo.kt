package cl.nikalbazu.diptico.estado

import cl.nikalbazu.diptico.audio.Avisos

/*
 * Avisos de DIPPO en la campana de notificaciones de arriba: calor, batería baja, versiones nuevas…
 * Se guardan ahí para verlos cuando quieras (en los globitos duran poco) y se quitan solos cuando ya no aplican.
 */

/** Publica un aviso de DIPPO (si no los apagaste). Con la misma [clave] reemplaza al anterior. */
fun Diptico.avisoDippo(clave: String, titulo: String, texto: String, accion: (() -> Unit)? = null) {
    if (!estado.value.extra.dippoAvisos) return
    Avisos.publicarDippo(app, clave, titulo, texto, accion)
}

fun Diptico.quitarAvisoDippo(clave: String) = Avisos.retirarDippo(clave)

/** Aviso de calor según la lectura (la consola lo cuenta en la pantalla de abajo mientras juegas; esto queda en la campana). */
fun Diptico.avisoCalor(t: Termica.Lectura) {
    val muy = t.nivel == Termica.Nivel.MUY_CALIENTE
    val grados = if (t.bateria > 0f) " (${t.bateria.toInt()} °C)" else ""
    avisoDippo(
        "calor", if (muy) "La consola está muy caliente$grados" else "La consola se está calentando$grados",
        Termica.consejoUrgente(t)?.texto ?: "Déjala descansar un ratito, baja el brillo y sácale la funda para que respire.",
    )
}

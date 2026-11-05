package cl.nikalbazu.diptico.estado

/** Respuestas verificables del manual de Díptico, disponibles sin descargar un modelo. */
internal object AyudaLocal {
    fun responder(texto: String): String? {
        val consulta = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").lowercase(java.util.Locale.ROOT)
        if (listOf("como ", "donde ", "ayuda", "manual", "no abre", "no aparece").none { it in consulta }) return null
        return when {
            "widget" in consulta -> "Desde Inicio › Widgets, toca Editar. Mantén una casilla vacía para agregar un widget. Arrastra su manija para cambiar el tamaño y mantén el widget junto al borde para pasarlo a otra página."
            "respaldo" in consulta || "restaur" in consulta -> "En Menú › Acerca de, elige dónde guardar un respaldo o selecciona uno para recuperar tus ajustes y notas. Las partidas de los emuladores se manejan por separado."
            "pantalla" in consulta -> "En Menú › Pantallas puedes elegir el modo y qué lado usa cada parte. En las opciones de un juego, «Pantalla compañera» elige la vista que se mostrará en la pantalla libre."
            "es-de" in consulta || "gamelist" in consulta -> "Escanea tus juegos primero. Luego abre Menú › Biblioteca › Importar lista de ES-DE y elige el gamelist.xml. Verás una previa de títulos y favoritos."
            "emulador" in consulta || "no abre" in consulta -> "Revisa que el juego esté accesible en su carpeta y que tengas un emulador compatible. En Menú › Emuladores puedes revisar los instalados. Algunos sistemas requieren BIOS configurada dentro del emulador."
            "mapa" in consulta -> "En las opciones de un juego abre Mi paso y elige Mapa. Puedes elegir una imagen y marcar un punto. En dos pantallas, la vista Bitácora muestra tu mapa mientras juegas."
            else -> null
        }
    }
}

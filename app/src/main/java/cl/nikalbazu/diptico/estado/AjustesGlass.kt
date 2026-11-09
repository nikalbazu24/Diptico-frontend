package cl.nikalbazu.diptico.estado

/** Opciones del material fuera del controlador general de la biblioteca. */
internal fun opcionesGlass(e: Estado): List<Opcion> = listOf(
    Opcion("Modo Liquid Glass", when (e.experiencia.materialInterfaz) {
        "glass" -> "Activado"; "original" -> "Desactivado"; else -> "Según el tema"
    }, Icono.ESTRELLA, "Material translúcido con desenfoque, luz y bordes suaves. Conserva tu tema y su distribución.") {
        val materiales = listOf("glass", "original", "tema")
        val v = Diptico.estado.value.experiencia
        Diptico.guardarExperiencia(v.copy(materialInterfaz = materiales[(materiales.indexOf(v.materialInterfaz) + 1).mod(materiales.size)]))
    }.let { o ->
        val materiales = listOf("glass", "original", "tema")
        o.copy(alternativas = listOf("Activado", "Desactivado", "Según el tema"), elegida = materiales.indexOf(e.experiencia.materialInterfaz),
            elegir = { n -> Diptico.guardarExperiencia(Diptico.estado.value.experiencia.copy(materialInterfaz = materiales[n])) })
    },
) + ZonaGlass.entries.map { zona ->
    val nivel = e.nivelesGlass.para(zona)
    fun cambiar(valor: Int) {
        val v = Diptico.estado.value.experiencia
        Diptico.guardarExperiencia(v.copy(nivelesGlass = v.nivelesGlass.cambiar(zona, valor)))
    }
    Opcion("Glass de ${zona.nombre}", "$nivel %", Icono.ESTRELLA,
        "0 % sólido; 100 % máxima transparencia y profundidad. Ajuste independiente para ${zona.nombre}.",
        menos = { cambiar(Diptico.estado.value.nivelesGlass.para(zona) - 10) },
        rango = RangoOpcion(nivel) { cambiar(it) },
    ) { cambiar(Diptico.estado.value.nivelesGlass.para(zona) + 10) }
}

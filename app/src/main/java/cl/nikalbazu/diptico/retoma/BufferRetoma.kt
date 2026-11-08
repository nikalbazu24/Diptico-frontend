package cl.nikalbazu.diptico.retoma

/** Solo paquetes codificados de los últimos segundos; cada clip comienza en un fotograma completo. */
internal data class PaqueteRetoma(val datos: ByteArray, val tiempoUs: Long, val clave: Boolean, val flags: Int)
internal class BufferRetoma(private val ventanaUs: Long = 6_000_000L, private val limiteBytes: Int = 3 * 1024 * 1024) {
    private val paquetes = ArrayDeque<PaqueteRetoma>()
    private var bytes = 0
    fun agregar(p: PaqueteRetoma) {
        if (paquetes.isEmpty() && !p.clave) return
        paquetes.addLast(p); bytes += p.datos.size
        while (paquetes.size > 1) {
            // Índice del siguiente fotograma completo (sin copiar la lista en cada paquete).
            var siguiente = -1
            for (i in 1 until paquetes.size) if (paquetes[i].clave) { siguiente = i; break }
            if (siguiente < 0) break
            if (paquetes[siguiente].tiempoUs > p.tiempoUs - ventanaUs && bytes <= limiteBytes) break
            repeat(siguiente) { bytes -= paquetes.removeFirst().datos.size }
        }
        if (bytes > limiteBytes) limpiar()
    }
    fun copia(): List<PaqueteRetoma> = paquetes.toList()
    fun limpiar() { paquetes.clear(); bytes = 0 }
}

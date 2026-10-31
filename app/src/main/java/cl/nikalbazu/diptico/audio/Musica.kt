package cl.nikalbazu.diptico.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.provider.DocumentsContract
import cl.nikalbazu.diptico.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Música de fondo de Díptico.
 * Suena solo mientras Díptico está a la vista y no hay un juego abierto.
 * Si otra app pide el audio (Spotify, YouTube Music, un video), se calla sola.
 */
object Musica {
    data class Pista(val id: String, val nombre: String, val estilo: String, val recurso: Int)

    val pistas = listOf(
        Pista("bisagra", "Bisagra", "Lofi tranquilo", R.raw.diptico_bisagra),
        Pista("cartucho", "Cartucho", "Arcade alegre", R.raw.diptico_cartucho),
        Pista("medianoche", "Medianoche", "Ambiente suave", R.raw.diptico_medianoche),
        Pista("fantasma", "Fantasma", "Misterio de Halloween", R.raw.diptico_fantasma),
        Pista("nieve", "Nieve", "Cascabeles de Navidad", R.raw.diptico_nieve),
        Pista("sakura", "Sakura", "Flauta y koto japonés", R.raw.diptico_sakura),
        Pista("neon", "Neón", "Synthwave arcade", R.raw.diptico_neon),
        Pista("oceano", "Océano", "Olas y calma", R.raw.diptico_oceano),
        Pista("bosque", "Bosque", "Folk con flauta", R.raw.diptico_bosque),
        Pista("portatil", "Portátil", "Chiptune clásico", R.raw.diptico_portatil),
        Pista("fiesta", "Fiesta", "Carnaval y celebración", R.raw.diptico_fiesta),
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var app: Context
    private var mp: MediaPlayer? = null
    private var sonando: String? = null
    private var nivel = 0f
    private var volumenMax = 0.6f
    private var fundido: Job? = null
    private var debe = false
    private var focoPerdido = false
    private var focoPedido: AudioFocusRequest? = null
    private var cola: List<Uri> = emptyList()
    private var enCola = 0
    private var mezcla = 0

    /** Nombre de lo que está sonando ahora, para mostrarlo. */
    var titulo: String = ""
        private set

    fun iniciar(ctx: Context) {
        if (!::app.isInitialized) app = ctx.applicationContext
    }

    private val escuchaFoco = AudioManager.OnAudioFocusChangeListener { cambio ->
        scope.launch {
            when (cambio) {
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    focoPerdido = true
                    fundir(0f) { mp?.pause() }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> fundir(volumenMax * 0.3f)
                AudioManager.AUDIOFOCUS_GAIN -> {
                    focoPerdido = false
                    if (debe) {
                        mp?.start()
                        fundir(volumenMax)
                    }
                }
            }
        }
    }

    private fun pedirFoco(): Boolean {
        val am = app.getSystemService(AudioManager::class.java) ?: return true
        val req = focoPedido ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
            )
            .setOnAudioFocusChangeListener(escuchaFoco)
            .setWillPauseWhenDucked(false)
            .build().also { focoPedido = it }
        return am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun soltarFoco() {
        val am = app.getSystemService(AudioManager::class.java) ?: return
        focoPedido?.let { am.abandonAudioFocusRequest(it) }
    }

    /**
     * Díptico llama esto cada vez que cambia algo: si debe sonar, qué pista, a qué volumen.
     * [carpeta] es la carpeta de música propia (para la pista "mia").
     */
    fun sincronizar(sonar: Boolean, pista: String, volumen: Float, carpeta: Uri?) {
        if (!::app.isInitialized) return
        volumenMax = volumen.coerceIn(0f, 1f)
        val clave = if (pista == "mia") "mia:$carpeta" else pista
        if (!sonar) {
            if (debe) {
                debe = false
                fundir(0f) {
                    mp?.pause()
                    soltarFoco()
                }
            }
            return
        }
        debe = true
        if (sonando != clave || mp == null) {
            cambiarA(pista, clave, carpeta)
            return
        }
        if (focoPerdido) return
        if (mp?.isPlaying != true) {
            if (!pedirFoco()) return
            mp?.start()
        }
        fundir(volumenMax)
    }

    /** Escuchar una pista un momento (en el tutorial y en el menú). */
    fun probar(pista: String, volumen: Float, carpeta: Uri?) = sincronizar(true, pista, volumen, carpeta)

    private fun cambiarA(pista: String, clave: String, carpeta: Uri?) {
        fundido?.cancel()
        val anterior = mp
        sonando = clave
        scope.launch {
            // Se apaga lo anterior suave y entra lo nuevo.
            if (anterior != null) {
                var v = nivel
                while (v > 0f) {
                    v = (v - 0.06f).coerceAtLeast(0f)
                    try { anterior.setVolume(v, v) } catch (_: Exception) {}
                    delay(30)
                }
                try { anterior.release() } catch (_: Exception) {}
            }
            if (mp === anterior) mp = null
            nivel = 0f
            if (sonando != clave || !debe) return@launch
            val nuevo = when (pista) {
                "mia" -> {
                    cola = withContext(Dispatchers.IO) { listarCanciones(carpeta) }.shuffled()
                    enCola = 0
                    crearDeCola()
                }
                "mezcla" -> {
                    mezcla = 0
                    crearDeRecurso(pistas[0], enBucle = false)
                }
                else -> if (pista.startsWith("archivo:")) {
                    crearDeArchivo(Uri.parse(pista.removePrefix("archivo:"))) ?: crearDeRecurso(pistas[0], enBucle = true)
                } else {
                    crearDeRecurso(pistas.firstOrNull { it.id == pista } ?: pistas[0], enBucle = true)
                }
            }
            if (nuevo == null) return@launch
            mp = nuevo
            if (!pedirFoco()) {
                focoPerdido = true
                return@launch
            }
            focoPerdido = false
            nuevo.setVolume(0f, 0f)
            nuevo.start()
            fundir(volumenMax)
        }
    }

    private fun atributos() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private fun crearDeRecurso(p: Pista, enBucle: Boolean): MediaPlayer? = try {
        val afd = app.resources.openRawResourceFd(p.recurso)
        MediaPlayer().apply {
            setAudioAttributes(atributos())
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            isLooping = enBucle
            prepare()
            if (!enBucle) setOnCompletionListener { siguienteMezcla() }
        }.also { titulo = p.nombre }
    } catch (e: Exception) {
        null
    }

    /** La canción propia de un juego, en bucle. */
    private fun crearDeArchivo(uri: Uri): MediaPlayer? = try {
        MediaPlayer().apply {
            setAudioAttributes(atributos())
            setDataSource(app, uri)
            isLooping = true
            prepare()
        }.also { titulo = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Música del juego" }
    } catch (e: Exception) {
        null
    }

    private fun siguienteMezcla() {
        if (!sonando.equals("mezcla")) return
        mezcla = (mezcla + 1) % pistas.size
        val viejo = mp
        val nuevo = crearDeRecurso(pistas[mezcla], enBucle = false) ?: return
        mp = nuevo
        try { viejo?.release() } catch (_: Exception) {}
        nuevo.setVolume(nivel, nivel)
        if (debe && !focoPerdido) nuevo.start()
    }

    private fun crearDeCola(): MediaPlayer? {
        if (cola.isEmpty()) {
            // Sin canciones en la carpeta: vuelve a la música de Díptico.
            return crearDeRecurso(pistas[0], enBucle = true)
        }
        for (intento in cola.indices) {
            val uri = cola[(enCola + intento) % cola.size]
            try {
                val m = MediaPlayer().apply {
                    setAudioAttributes(atributos())
                    setDataSource(app, uri)
                    prepare()
                    setOnCompletionListener { siguienteDeCola() }
                }
                enCola = (enCola + intento) % cola.size
                titulo = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Tu música"
                return m
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun siguienteDeCola() {
        if (cola.isEmpty()) return
        enCola = (enCola + 1) % cola.size
        val viejo = mp
        val nuevo = crearDeCola() ?: return
        mp = nuevo
        try { viejo?.release() } catch (_: Exception) {}
        nuevo.setVolume(nivel, nivel)
        if (debe && !focoPerdido) nuevo.start()
    }

    /** Las canciones de la carpeta de música propia (mp3, ogg, m4a, flac, wav, opus). */
    fun listarCanciones(carpeta: Uri?): List<Uri> {
        if (carpeta == null) return emptyList()
        val salida = ArrayList<Uri>()
        val ext = setOf("mp3", "ogg", "m4a", "flac", "wav", "opus", "aac")
        try {
            val raiz = DocumentsContract.getTreeDocumentId(carpeta)
            val hijos = DocumentsContract.buildChildDocumentsUriUsingTree(carpeta, raiz)
            app.contentResolver.query(
                hijos,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val nombre = c.getString(1) ?: continue
                    if (nombre.substringAfterLast('.', "").lowercase() in ext) {
                        salida += DocumentsContract.buildDocumentUriUsingTree(carpeta, id)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return salida
    }

    private fun fundir(objetivo: Float, alTerminar: (() -> Unit)? = null) {
        fundido?.cancel()
        fundido = scope.launch {
            while (kotlin.math.abs(nivel - objetivo) > 0.01f) {
                nivel += (objetivo - nivel).coerceIn(-0.05f, 0.04f)
                try { mp?.setVolume(nivel, nivel) } catch (_: Exception) {}
                delay(40)
            }
            nivel = objetivo
            try { mp?.setVolume(nivel, nivel) } catch (_: Exception) {}
            alTerminar?.invoke()
        }
    }
}

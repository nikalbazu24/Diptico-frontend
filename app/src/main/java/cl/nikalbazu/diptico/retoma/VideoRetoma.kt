package cl.nikalbazu.diptico.retoma

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

/** TextureView permite componer el video detrás del texto y del desenfoque, sin otra pantalla virtual. */
@Composable
fun VideoRetoma(archivo: String, modifier: Modifier = Modifier, alFallar: () -> Unit = {}) {
    var reproductor by remember(archivo) { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(archivo) { onDispose { try { reproductor?.release() } catch (_: Exception) {}; reproductor = null } }
    AndroidView(modifier = modifier, factory = { ctx ->
        TextureView(ctx).apply {
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(textura: SurfaceTexture, w: Int, h: Int) {
                    if (!File(archivo).isFile) return
                    val mp = MediaPlayer(); reproductor = mp
                    val s = Surface(textura)
                    try {
                        mp.setSurface(s); s.release(); mp.setVolume(0f, 0f); mp.isLooping = false
                        mp.setDataSource(archivo)
                        mp.setOnPreparedListener { if (reproductor === it) it.start() }
                        mp.setOnErrorListener { _, _, _ -> alFallar(); true }
                        mp.prepareAsync()
                    } catch (_: Exception) { s.release(); mp.release(); reproductor = null; alFallar() }
                }
                override fun onSurfaceTextureSizeChanged(t: SurfaceTexture, w: Int, h: Int) {}
                override fun onSurfaceTextureDestroyed(t: SurfaceTexture): Boolean { try { reproductor?.release() } catch (_: Exception) {}; reproductor = null; return true }
                override fun onSurfaceTextureUpdated(t: SurfaceTexture) {}
            }
        }
    })
}

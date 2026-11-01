package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** Un código QR con [texto] (por ejemplo, la página para sacar una clave), para escanearlo con el teléfono. */
@Composable
fun CodigoQR(texto: String, tam: Float, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val matriz = remember(texto) {
        try { QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0)) } catch (_: Exception) { null }
    } ?: return
    Box(modifier.clip(RoundedCornerShape(esc.d(8))).background(Color.White).padding(esc.d(5))) {
        Canvas(Modifier.size(esc.d(tam))) {
            val n = matriz.width
            val c = size.width / n
            for (y in 0 until n) for (x in 0 until n) {
                if (matriz.get(x, y)) drawRect(Color.Black, Offset(x * c, y * c), Size(c + 0.5f, c + 0.5f))
            }
        }
    }
}

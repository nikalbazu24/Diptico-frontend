package cl.nikalbazu.diptico.renacer

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cl.nikalbazu.diptico.data.Biblioteca
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.emu.App
import cl.nikalbazu.diptico.emu.Lanzador
import cl.nikalbazu.diptico.ui.rememberImagen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun VentanasRenacer(e: Salon, enviar: (Accion) -> Unit, pantalla: Int, alterna: Int?, apps: List<App>, buscandoArte: String?) {
    val ventana = e.ventana ?: return
    Dialog(onDismissRequest = { enviar(Accion.Abrir(null)) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.93f).widthIn(max = 800.dp).fillMaxHeight(.9f), shape = RoundedCornerShape(24.dp)) {
            when (ventana) {
                is Ventana.Juego -> DetalleRenacer(e, ventana.clave, enviar, pantalla, alterna, buscandoArte)
                Ventana.Fuentes -> CarpetasRenacer(e, enviar)
                Ventana.Apps -> AppsRenacer(apps, enviar)
            }
        }
    }
}

@Composable
private fun TituloVentana(titulo: String, enviar: (Accion) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        TextButton(onClick = { enviar(Accion.Abrir(null)) }) { Text("Cerrar") }
    }
}

@Composable
private fun DetalleRenacer(e: Salon, clave: String, enviar: (Accion) -> Unit, pantalla: Int, alterna: Int?, buscandoArte: String?) {
    val juego = e.porClave[clave]
    if (juego == null) {
        MensajeVacio("Este juego ya no está aquí", "Revisa el acceso a su carpeta o vuelve a buscar tus juegos.") {
            Button(onClick = { enviar(Accion.Abrir(null)) }) { Text("Volver") }
        }
        return
    }
    val dato = e.datos[clave]
    val contexto = LocalContext.current
    val consola = Consolas.porId(juego.consolaId)
    val opciones by produceState(emptyList<Lanzador.Opcion>(), juego.clave) {
        value = withContext(Dispatchers.IO) { consola?.let { Lanzador.opciones(contexto, it, juego.tipo) }.orEmpty().distinctBy { it.id } }
    }
    var elegirEmulador by remember(clave) { mutableStateOf(false) }
    Column(Modifier.padding(22.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TituloVentana(juego.titulo, enviar)
        Eyebrow(consola?.bonito ?: juego.consolaId)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val imagen = rememberImagen(juego.portada, 500)
            Box(Modifier.width(110.dp).height(135.dp).clip(RoundedCornerShape(12.dp))) {
                if (imagen != null) Image(imagen, juego.titulo, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else Paisaje(juego, Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { enviar(Accion.Jugar(clave, pantalla)) }) { Text("Jugar") }
                if (alterna != null) OutlinedButton(onClick = { enviar(Accion.Jugar(clave, alterna)) }) { Text("Jugar en la otra pantalla") }
                TextButton(onClick = { enviar(Accion.Favorito(clave)) }) { Text(if (dato?.favorito == true) "Quitar de favoritos" else "Guardar en favoritos") }
            }
        }
        OutlinedTextField(dato?.paso.orEmpty(), { enviar(Accion.Paso(clave, it)) }, Modifier.fillMaxWidth(),
            label = { Text("Mi próximo paso") }, placeholder = { Text("¿Dónde quieres continuar?") }, minLines = 2, maxLines = 4)
        if (juego.uri.scheme != "app") Box {
            OutlinedButton(onClick = { elegirEmulador = true }) {
                Text("Emulador · " + (opciones.firstOrNull { it.id == dato?.emulador }?.nombre ?: "Automático"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(elegirEmulador, { elegirEmulador = false }) {
                DropdownMenuItem(text = { Text("Automático") }, onClick = { enviar(Accion.Emulador(clave, null)); elegirEmulador = false })
                opciones.forEach { opcion -> DropdownMenuItem(text = { Text(opcion.nombre) }, onClick = { enviar(Accion.Emulador(clave, opcion.id)); elegirEmulador = false }) }
            }
        }
        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { enviar(Accion.BuscarArte(clave)) }, enabled = buscandoArte == null) { Text(if (buscandoArte == clave) "Buscando arte…" else "Buscar portada") }
            TextButton(onClick = { enviar(Accion.Imagen(clave)) }) { Text("Elegir imagen") }
        }
        TextButton(onClick = { enviar(Accion.Ocultar(clave)) }) { Text(if (dato?.oculto == true) "Volver a mostrar" else "Ocultar de mi biblioteca") }
        val minutos = (dato?.tiempo ?: 0L) / 60
        if (minutos > 0) Text("${minutos / 60} h ${minutos % 60} min en tu biblioteca", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CarpetasRenacer(e: Salon, enviar: (Accion) -> Unit) {
    var asignando by remember { mutableStateOf<Uri?>(null) }
    var consulta by remember { mutableStateOf("") }
    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloVentana("De dónde vienen tus juegos", enviar)
        Text("Puedes sumar carpetas de la memoria interna o la SD. Si una carpeta tiene un nombre propio, elige su consola.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { enviar(Accion.Carpeta) }) { Text("Agregar carpeta") }
            OutlinedButton(onClick = { enviar(Accion.Recargar) }, enabled = !e.cargando) { Text(if (e.cargando) "Buscando…" else "Buscar otra vez") }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(e.carpetas, key = { it.uri.toString() }) { carpeta ->
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(Biblioteca.nombreCarpeta(carpeta.uri), style = MaterialTheme.typography.titleMedium)
                        Text(Consolas.porId(carpeta.consola.orEmpty())?.bonito ?: "Reconocer consolas automáticamente", style = MaterialTheme.typography.bodyMedium)
                        Row {
                            TextButton(onClick = { consulta = ""; asignando = carpeta.uri }) { Text("Elegir consola") }
                            TextButton(onClick = { enviar(Accion.QuitarCarpeta(carpeta.uri)) }) { Text("Quitar acceso") }
                        }
                    }
                }
            }
        }
    }
    val uri = asignando
    if (uri != null) AlertDialog(onDismissRequest = { asignando = null },
        title = { Text("Consola de esta carpeta") },
        text = {
            Column {
                OutlinedTextField(consulta, { consulta = it }, singleLine = true, placeholder = { Text("Buscar consola") })
                LazyColumn(Modifier.heightIn(max = 260.dp)) {
                    item { TextButton(onClick = { enviar(Accion.AsignarConsola(uri, null)); asignando = null }) { Text("Reconocer automáticamente") } }
                    items(Consolas.todas.filter { Salon.normalizar(it.bonito).contains(Salon.normalizar(consulta)) }, key = { it.id }) { consola ->
                        TextButton(onClick = { enviar(Accion.AsignarConsola(uri, consola.id)); asignando = null }) { Text(consola.bonito) }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { asignando = null }) { Text("Volver") } })
}

@Composable
private fun AppsRenacer(apps: List<App>, enviar: (Accion) -> Unit) {
    var consulta by remember { mutableStateOf("") }
    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloVentana("Juegos y apps de Android", enviar)
        Text("Elige una app para dejarla en tu biblioteca.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(consulta, { consulta = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Buscar una app") })
        LazyColumn(Modifier.weight(1f)) {
            items(apps.filter { Salon.normalizar(it.nombre).contains(Salon.normalizar(consulta)) }, key = { it.paquete }) { app ->
                TextButton(onClick = { enviar(Accion.AgregarApp(app.paquete)) }, modifier = Modifier.fillMaxWidth()) { Text(app.nombre, Modifier.fillMaxWidth()) }
            }
        }
    }
}

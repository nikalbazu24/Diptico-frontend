@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package cl.nikalbazu.diptico.renacer

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.data.Juego
import cl.nikalbazu.diptico.emu.App
import cl.nikalbazu.diptico.estado.Temas
import cl.nikalbazu.diptico.ui.rememberImagen

/** Una misma estructura adapta el salón a la consola, el teléfono y la pantalla compañera. */
@Composable
fun SalonRenacer(
    e: Salon,
    enviar: (Accion) -> Unit,
    modifier: Modifier = Modifier,
    companera: Boolean = false,
    pantallaJuego: Int = 0,
    pantallaAlterna: Int? = null,
    apps: List<App> = emptyList(),
    buscandoArte: String? = null,
) {
    TemaRenacer(e) {
        Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val ancho = maxWidth >= 700.dp
                val compacta = maxHeight < 500.dp
                val fondo = if (e.ventana != null && e.movimiento) Modifier.blur(12.dp) else Modifier
                Column(Modifier.fillMaxSize().then(fondo)) {
                    if (companera) {
                        VitrinaRenacer(e, enviar, pantallaJuego, Modifier.weight(1f))
                    } else if (e.partida != null) {
                        CompañeroRenacer(e, enviar, Modifier.weight(1f))
                    } else {
                        Row(Modifier.weight(1f)) {
                            if (ancho) Navegacion(e, enviar, vertical = true)
                            Column(Modifier.weight(1f)) {
                                if (!compacta || e.lugar == Lugar.JUGAR) Cabecera(e, enviar)
                                when {
                                    e.cargando && e.juegos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                                    e.juegos.isEmpty() && e.lugar != Lugar.AMBIENTE -> BienvenidaRenacer(e, enviar, Modifier.weight(1f))
                                    e.lugar == Lugar.JUGAR -> InicioRenacer(e, enviar, pantallaJuego, Modifier.weight(1f))
                                    e.lugar == Lugar.BIBLIOTECA -> BibliotecaSalon(e, enviar, Modifier.weight(1f))
                                    else -> AmbienteSalon(e, enviar, Modifier.weight(1f))
                                }
                            }
                        }
                        if (!ancho) Navegacion(e, enviar, vertical = false)
                    }
                    if (e.error != null) Surface(color = MaterialTheme.colorScheme.errorContainer) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(e.error, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { enviar(Accion.Recargar) }) { Text("Reintentar") }
                        }
                    }
                }
                if (e.aviso != null) Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(18.dp).widthIn(max = 600.dp),
                    action = { TextButton(onClick = { enviar(Accion.CerrarAviso) }) { Text("Entendido") } },
                ) { Text(e.aviso) }
                if (!companera) VentanasRenacer(e, enviar, pantallaJuego, pantallaAlterna, apps, buscandoArte)
            }
        }
    }
}

@Composable
private fun Cabecera(e: Salon, enviar: (Accion) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Marca(Modifier.size(22.dp, 25.dp))
        Spacer(Modifier.width(9.dp))
        Text("díptico", fontFamily = FontFamily.Serif, fontSize = 23.sp)
        Spacer(Modifier.weight(1f))
        if (e.cargando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = { enviar(Accion.Apps) }) { Text("Apps") }
    }
}

@Composable
private fun Navegacion(e: Salon, enviar: (Accion) -> Unit, vertical: Boolean) {
    val contenido: @Composable (Lugar) -> Unit = { lugar ->
        val activo = e.lugar == lugar
        val signo = when (lugar) { Lugar.JUGAR -> "01"; Lugar.BIBLIOTECA -> "02"; Lugar.AMBIENTE -> "03" }
        TextButton(onClick = { enviar(Accion.Ir(lugar)) },
            colors = ButtonDefaults.textButtonColors(contentColor = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant),
            shape = RoundedCornerShape(12.dp)) {
            Text("$signo  ${lugar.nombre}", style = MaterialTheme.typography.labelLarge)
        }
    }
    if (vertical) Column(Modifier.width(170.dp).fillMaxHeight().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(65.dp))
        Lugar.entries.forEach { contenido(it) }
        Spacer(Modifier.weight(1f))
        Eyebrow("RENACER")
        Text("Primer capítulo", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Eyebrow("L1 / R1 · CAMBIAR")
    } else Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        Lugar.entries.forEach { contenido(it) }
    }
}

@Composable
private fun BienvenidaRenacer(e: Salon, enviar: (Accion) -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Eyebrow("BIENVENIDO A TU DÍPTICO")
        Text("Hagámosle un lugar\na tus juegos.", style = MaterialTheme.typography.headlineLarge)
        Text("Conecta una carpeta y elige a qué volver. Tu biblioteca empieza con lo que ya tienes.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { enviar(Accion.Carpeta) }, enabled = !e.cargando) { Text("Agregar mis juegos") }
            OutlinedButton(onClick = { enviar(Accion.Apps) }) { Text("Juegos Android") }
        }
        if (e.carpetas.isNotEmpty()) TextButton(onClick = { enviar(Accion.Abrir(Ventana.Fuentes)) }) { Text("Revisar carpetas y elegir su consola") }
        HorizontalDivider()
        Text("¿Vienes de la beta?", style = MaterialTheme.typography.titleMedium)
        Text("Recupera tus favoritos, tiempos y notas desde un respaldo de Díptico. Después elige las carpetas de este equipo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { enviar(Accion.Importar) }) { Text("Traer mi respaldo") }
    }
}

@Composable
private fun InicioRenacer(e: Salon, enviar: (Accion) -> Unit, pantalla: Int, modifier: Modifier) {
    val juego = e.protagonista
    LazyColumn(modifier, contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Eyebrow(if (e.recientes.isEmpty()) "ELIGE TU PRIMER VIAJE" else "TU PRÓXIMA PARTIDA", Modifier.padding(bottom = 10.dp))
            if (e.dosPantallas && e.usarDos && juego != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(juego.titulo, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Button(onClick = { enviar(Accion.Jugar(juego.clave, pantalla)) }) { Text("Continuar") }
                }
                val paso = e.datos[juego.clave]?.paso.orEmpty()
                if (paso.isNotBlank()) Text(paso, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            } else HeroeRenacer(e, juego, enviar, pantalla, Modifier.fillMaxWidth().height(252.dp))
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (e.recientes.isNotEmpty()) "A donde quieres volver" else "Por descubrir", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { enviar(Accion.Consola("*")) }) { Text("Ver biblioteca") }
            }
            val sugerencias = e.recientes.ifEmpty { e.juegos.filterNot { e.datos[it.clave]?.oculto == true }.take(8) }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sugerencias, key = { it.clave }) { j -> JuegoMiniatura(j, e, enviar, Modifier.width(150.dp)) }
            }
        }
        item {
            val fiesta = remember(java.time.LocalDate.now()) { Temas.deHoy() }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Marca(Modifier.size(16.dp, 19.dp))
                Spacer(Modifier.width(10.dp))
                Text(fiesta?.let { "Hoy celebramos ${it.nombre}." } ?: "Un juego nuevo, o ese al que siempre vuelves.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
internal fun HeroeRenacer(e: Salon, juego: Juego?, enviar: (Accion) -> Unit, pantalla: Int, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(20.dp))) {
        Crossfade(juego, Modifier.fillMaxSize(), animationSpec = if (e.movimiento) tween(160) else snap(), label = "arteDelJuego") { actual ->
            Paisaje(actual, Modifier.fillMaxSize())
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF172D28), Color(0xED172D28), Color(0x35172D28)))))
        Column(Modifier.fillMaxHeight().fillMaxWidth(.82f).padding(22.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Eyebrow(juego?.let { Consolas.porId(it.consolaId)?.bonito } ?: "TU BIBLIOTECA", color = Color(0xFFD9DDCB))
            Text(juego?.titulo ?: "Tu próxima historia", color = Color(0xFFF8F0DE), style = MaterialTheme.typography.headlineLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val paso = juego?.let { e.datos[it.clave]?.paso }.orEmpty()
            if (paso.isNotBlank()) Text(paso, color = Color(0xFFD9DDCB), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            if (juego != null) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { enviar(Accion.Jugar(juego.clave, pantalla)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6BF7F), contentColor = Color(0xFF252B23))) {
                    Text(if ((e.datos[juego.clave]?.ultimo ?: 0L) > 0) "Continuar" else "Jugar")
                }
                TextButton(onClick = { enviar(Accion.Abrir(Ventana.Juego(juego.clave))) }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("Ver juego") }
            }
        }
    }
}

@Composable
private fun JuegoMiniatura(juego: Juego, e: Salon, enviar: (Accion) -> Unit, modifier: Modifier) {
    TarjetaEnfocable(modifier, e.elegido == juego.clave,
        seleccionar = { enviar(Accion.Seleccionar(juego.clave)) }, activar = { enviar(Accion.Abrir(Ventana.Juego(juego.clave))) }) {
        Column {
            val imagen = rememberImagen(juego.portada, 350)
            Box(Modifier.fillMaxWidth().height(105.dp).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                if (imagen != null) Image(imagen, juego.titulo, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Paisaje(juego, Modifier.fillMaxSize())
                if (e.datos[juego.clave]?.favorito == true) Text("♥", Modifier.align(Alignment.TopEnd).padding(8.dp), color = Color.White)
            }
            Column(Modifier.padding(11.dp)) {
                Text(juego.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Consolas.porId(juego.consolaId)?.nombre ?: juego.consolaId, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BibliotecaSalon(e: Salon, enviar: (Accion) -> Unit, modifier: Modifier) {
    Column(modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (e.consola != null) TextButton(onClick = { enviar(Accion.Consola(null)) }) { Text("←") }
            Text(e.consola?.let { Consolas.porId(it)?.bonito } ?: "Tu biblioteca", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = { enviar(Accion.Abrir(Ventana.Fuentes)) }) { Text("Carpetas") }
        }
        OutlinedTextField(e.consulta, { enviar(Accion.Consulta(it)) }, Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("Encuentra un juego") }, shape = RoundedCornerShape(14.dp),
            trailingIcon = { if (e.consulta.isNotEmpty()) TextButton(onClick = { enviar(Accion.Consulta("")) }) { Text("Borrar") } })
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            items(Filtro.entries) { filtro -> FilterChip(e.filtro == filtro, { enviar(Accion.Filtrar(filtro)) }, label = { Text(filtro.nombre) }) }
            item { TextButton(onClick = { enviar(Accion.Lista) }) { Text(if (e.lista) "Cuadrícula" else "Lista") } }
        }
        if (e.filtro != Filtro.TODOS || e.consulta.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (e.visibles.size == 1) "1 juego" else "${e.visibles.size} juegos", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { enviar(Accion.Limpiar) }) { Text("Limpiar filtros") }
        }
        when {
            e.consola == null && e.consulta.isEmpty() && e.filtro == Filtro.TODOS -> LazyVerticalGrid(
                GridCells.Adaptive(145.dp), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                item { TarjetaSistema("*", "Todos tus juegos", e.juegos.count { e.datos[it.clave]?.oculto != true }, e.protagonista, enviar) }
                items(e.porConsola.entries.toList(), key = { it.key }) { (id, juegos) ->
                    TarjetaSistema(id, Consolas.porId(id)?.nombre ?: id, juegos.size, juegos.firstOrNull(), enviar)
                }
            }
            e.visibles.isEmpty() -> MensajeVacio("Por aquí todavía nada", "Puedes limpiar la búsqueda o elegir otro filtro.", Modifier.weight(1f)) {
                Button(onClick = { enviar(Accion.Limpiar) }) { Text("Ver mis juegos") }
            }
            e.lista -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                items(e.visibles, key = { it.clave }) { j -> TarjetaEnfocable(Modifier.fillMaxWidth(), e.elegido == j.clave,
                    seleccionar = { enviar(Accion.Seleccionar(j.clave)) }, activar = { enviar(Accion.Abrir(Ventana.Juego(j.clave))) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(j.titulo, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Eyebrow(Consolas.porId(j.consolaId)?.nombre ?: j.consolaId)
                    }
                } }
            }
            else -> LazyVerticalGrid(GridCells.Adaptive(130.dp), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                items(e.visibles, key = { it.clave }) { j -> JuegoMiniatura(j, e, enviar, Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
private fun TarjetaSistema(id: String, nombre: String, cantidad: Int, juego: Juego?, enviar: (Accion) -> Unit) {
    TarjetaEnfocable(Modifier.fillMaxWidth(), seleccionar = { juego?.let { enviar(Accion.Seleccionar(it.clave)) } }, activar = { enviar(Accion.Consola(id)) }) {
        Column {
            Box(Modifier.fillMaxWidth().height(92.dp)) {
                Paisaje(juego, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color(0x550B211B)))
                Text(if (id == "*") "D /" else nombre, Modifier.align(Alignment.Center), color = Color(0xFFF8F0DE), fontFamily = FontFamily.Serif, fontSize = 26.sp)
            }
            Column(Modifier.padding(13.dp)) {
                Text(nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (cantidad == 1) "1 juego" else "$cantidad juegos", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AmbienteSalon(e: Salon, enviar: (Accion) -> Unit, modifier: Modifier) {
    LazyColumn(modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
        item { Text("Un lugar que se sienta tuyo.", style = MaterialTheme.typography.headlineMedium) }
        item { Text("Tres ambientes para empezar. La estructura se mantiene familiar cuando cambias de color.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(Ambiente.entries) { ambiente ->
            TarjetaEnfocable(Modifier.fillMaxWidth(), e.ambiente == ambiente, activar = { enviar(Accion.Estilo(ambiente)) }) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color(ambiente.fondo))) {
                        Marca(Modifier.size(18.dp, 22.dp).align(Alignment.Center), Color(ambiente.acento))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ambiente.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(ambiente.descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (e.ambiente == ambiente) Text("✓", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { Ajuste("Celebraciones", "Un acento de color en las fechas del mundo y de Chile.", e.calendario) { enviar(Accion.Calendario(it)) } }
        item { Ajuste("Movimiento", "Transiciones y desenfoque al abrir una ventana.", e.movimiento) { enviar(Accion.Movimiento(it)) } }
        if (e.dosPantallas) item { Ajuste("Dos pantallas", "El juego se luce arriba y lo eliges abajo.", e.usarDos) { enviar(Accion.DosPantallas(it)) } }
        item {
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { enviar(Accion.Exportar) }) { Text("Guardar respaldo") }
                TextButton(onClick = { enviar(Accion.Importar) }) { Text("Recuperar") }
            }
            TextButton(onClick = { enviar(Accion.InicioAndroid) }) { Text("Usar Díptico como inicio de Android") }
        }
    }
}

@Composable
private fun Ajuste(titulo: String, texto: String, activo: Boolean, cambiar: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 14.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(activo, cambiar)
    }
}

@Composable
private fun VitrinaRenacer(e: Salon, enviar: (Accion) -> Unit, pantalla: Int, modifier: Modifier) {
    Column(modifier.padding(26.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Marca(Modifier.size(25.dp, 28.dp))
            Text("díptico", Modifier.padding(start = 10.dp).weight(1f), fontFamily = FontFamily.Serif, fontSize = 25.sp)
            Eyebrow(if (e.partida == null) "UN MOMENTO PARA JUGAR" else "PARTIDA EN CURSO")
        }
        HeroeRenacer(e, e.protagonista, enviar, pantalla, Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun CompañeroRenacer(e: Salon, enviar: (Accion) -> Unit, modifier: Modifier) {
    val juego = e.partida?.clave?.let(e.porClave::get)
    Column(modifier.padding(26.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Eyebrow("CONTIGO EN LA PARTIDA")
        Text(juego?.titulo ?: "Tu partida", style = MaterialTheme.typography.headlineLarge)
        val clave = juego?.clave
        if (clave != null) {
            OutlinedTextField(e.datos[clave]?.paso.orEmpty(), { enviar(Accion.Paso(clave, it)) }, Modifier.fillMaxWidth(),
                label = { Text("Una nota para cuando vuelvas") }, minLines = 3, maxLines = 5)
            Text("Puedes dejar aquí una pista, una tarea o ese lugar al que quieres volver.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

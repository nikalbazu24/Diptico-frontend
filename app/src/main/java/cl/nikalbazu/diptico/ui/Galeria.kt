package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import cl.nikalbazu.diptico.estado.Diptico
import cl.nikalbazu.diptico.estado.Estado
import cl.nikalbazu.diptico.estado.Icono
import cl.nikalbazu.diptico.estado.Teclas

/** Galería de temas, canciones o colores: pestañas arriba (L1/R1) y todo a la vista para elegir. */
@Composable
fun PanelGaleria(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val items = remember(e) { Diptico.itemsGaleria(e) }
    val pestanas = Diptico.pestanasGaleria(e.galeria)
    val (titulo, icono) = when (e.galeria) {
        "temas" -> "Temas" to Icono.PALETA
        "colores" -> "Mis colores" to Icono.PALETA
        "ia" -> "IA de Bisa" to Icono.CHISPA
        else -> "Música" to Icono.MUSICA
    }
    Box(Modifier.fillMaxSize().background(p.fondo2).sinCerrar()) {
        Column(Modifier.fillMaxSize().padding(start = esc.d(14), end = esc.d(14), top = esc.d(12), bottom = esc.d(10))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(icono, p.tinta, 22f, acento = Coral)
                Spacer(Modifier.width(esc.d(10)))
                Texto(titulo, 20f, lineas = 1, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(esc.d(34)).clip(CircleShape).background(p.superficie).clickable { Diptico.cerrarPanel() },
                    contentAlignment = Alignment.Center,
                ) { IconoDiptico(Icono.CERRAR, p.tinta, 16f) }
            }
            if (pestanas.size > 1) {
                Spacer(Modifier.height(esc.d(8)))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    pestanas.forEachIndexed { i, t ->
                        val activa = i == e.galeriaPestana
                        Box(
                            Modifier.clip(RoundedCornerShape(50)).then(if (activa) Modifier.fondoBoton(Uva) else Modifier.background(p.superficie))
                                .clickable { Diptico.cambiarPestanaGaleria(i - e.galeriaPestana) }
                                .padding(horizontal = esc.d(12), vertical = esc.d(6)),
                        ) { Texto(t, 13f, color = if (activa) SobreUva else p.tinta, lineas = 1) }
                    }
                }
            }
            if (e.galeria == "colores") {
                Spacer(Modifier.height(esc.d(6)))
                Detalle("Lo que elijas se aplica al tiro a toda la interfaz (queda guardado como \"Mi tema\").", 12f, lineas = 2)
            }
            if (e.galeria == "ia") {
                Spacer(Modifier.height(esc.d(6)))
                Detalle("Cada persona usa su propia IA: nadie gasta los créditos de otro al compartir Díptico.", 12f, lineas = 2)
            }
            Spacer(Modifier.height(esc.d(10)))
            val cols = when (e.galeria) {
                "colores" -> if (esc.angosta) 8 else 12
                "temas" -> if (esc.angosta) 2 else 3
                else -> 1
            }
            SideEffect { Teclas.columnasGaleria = cols }
            val grilla = rememberLazyGridState()
            LaunchedEffect(e.indice, e.galeriaPestana) { if (items.isNotEmpty()) grilla.animateScrollToItem((e.indice - cols).coerceAtLeast(0)) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols), state = grilla, modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(esc.d(if (e.galeria == "colores") 6f else 8f)),
                verticalArrangement = Arrangement.spacedBy(esc.d(if (e.galeria == "colores") 6f else 8f)),
                contentPadding = PaddingValues(esc.d(3)),
            ) {
                itemsIndexed(items, key = { _, it -> it.id + it.nombre }) { i, it ->
                    val activo = i == e.indice
                    when (e.galeria) {
                        "colores" -> MuestraColor(it, activo) { Diptico.elegirIndice(i) }
                        "temas" -> TarjetaTema(it, activo) { Diptico.elegirIndice(i) }
                        "ia" -> FilaIA(it, activo) { Diptico.elegirIndice(i) }
                        else -> FilaCancion(it, activo) { Diptico.elegirIndice(i) }
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas(*(if (pestanas.size > 1) arrayOf("A" to "Elegir", "L1 R1" to "Pestaña", "B" to "Volver") else arrayOf("A" to "Elegir", "B" to "Volver")))
        }
    }
}

@Composable
private fun TarjetaTema(it: Diptico.ItemGaleria, activo: Boolean, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Column(
        Modifier.clip(RoundedCornerShape(esc.d(16))).background(p.superficie)
            .border(esc.d(if (activo) 3f else if (it.enUso) 2f else 0f), if (activo) Uva else if (it.enUso) Sol else Color.Transparent, RoundedCornerShape(esc.d(16)))
            .clickable { alPulsar() }.padding(esc.d(6)),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(esc.d(12)))) {
            val t = it.tema
            if (t != null) PreviaTema(t, true)
            else Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Uva, Coral))), contentAlignment = Alignment.Center) {
                IconoDiptico(Icono.CHISPA, Color.White, 30f)
            }
        }
        Spacer(Modifier.height(esc.d(5)))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Texto(it.nombre, 13f, lineas = 1, modifier = Modifier.weight(1f))
            if (it.enUso) Texto("✓", 13f, color = Sol, traducir = false)
        }
        if (it.detalle.isNotBlank()) Detalle(it.detalle, 11f, lineas = 1)
    }
}

@Composable
private fun MuestraColor(it: Diptico.ItemGaleria, activo: Boolean, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val c = if (it.color == 0L) null else Color(it.color)
    Box(
        Modifier.aspectRatio(1f).clip(RoundedCornerShape(esc.d(10)))
            .background(c ?: p.superficie)
            .border(esc.d(if (activo) 3f else 1f), if (activo) Uva else p.linea, RoundedCornerShape(esc.d(10)))
            .clickable { alPulsar() },
        contentAlignment = Alignment.Center,
    ) {
        if (c == null) Texto("Auto", 10f, lineas = 1)
        if (it.enUso) Texto("✓", 15f, color = if ((c ?: p.superficie).luminance() > 0.5f) Tinta else Color.White, traducir = false)
    }
}

/** Una IA para Bisa: su sello de color, nombre y en qué estado está. */
@Composable
private fun FilaIA(it: Diptico.ItemGaleria, activo: Boolean, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val (sello, letra) = selloIA(it.id)
    Row(
        Modifier.fillMaxWidth().height(esc.d(60)).clip(RoundedCornerShape(esc.d(16)))
            .then(if (activo) Modifier.fondoBoton(Uva) else Modifier.background(p.superficie))
            .border(esc.d(if (it.enUso && !activo) 2f else 0f), if (it.enUso && !activo) Sol else Color.Transparent, RoundedCornerShape(esc.d(16)))
            .clickable { alPulsar() }.padding(horizontal = esc.d(12)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(esc.d(38)).clip(RoundedCornerShape(esc.d(11))).background(sello), contentAlignment = Alignment.Center) {
            if (it.id == "local") cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(30).value)
            else Texto(letra, 17f, color = Color.White, lineas = 1, traducir = false)
        }
        Spacer(Modifier.width(esc.d(12)))
        Column(Modifier.weight(1f)) {
            Texto(it.nombre, 15.5f, color = if (activo) SobreUva else p.tinta, lineas = 1)
            Detalle(it.detalle, 11.5f, color = if (activo) SobreUva.copy(alpha = 0.85f) else p.tenue, lineas = 1)
        }
        if (it.enUso) Box(Modifier.clip(RoundedCornerShape(50)).background(Sol.copy(alpha = 0.25f)).padding(horizontal = esc.d(8), vertical = esc.d(3))) {
            Texto("En uso", 11f, color = if (activo) SobreUva else p.tinta, lineas = 1)
        }
    }
}

/** Color y letra de cada IA (sin logos de marcas: solo un sello simple). */
fun selloIA(id: String): Pair<Color, String> = when (id) {
    "gemini" -> Color(0xFF4F7DF3) to "G"
    "openai" -> Color(0xFF10A37F) to "C"
    "claude" -> Color(0xFFD97757) to "A"
    "local" -> Color(0xFF2A2440) to ""
    else -> Uva to "✦"
}

@Composable
private fun FilaCancion(it: Diptico.ItemGaleria, activo: Boolean, alPulsar: () -> Unit) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    Row(
        Modifier.fillMaxWidth().height(esc.d(56)).clip(RoundedCornerShape(esc.d(16)))
            .then(if (activo) Modifier.fondoBoton(Uva) else Modifier.background(p.superficie))
            .clickable { alPulsar() }.padding(horizontal = esc.d(12)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(esc.d(34)).clip(CircleShape).background(if (activo) SobreUva.copy(alpha = 0.2f) else Uva.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            IconoDiptico(Icono.MUSICA, if (activo) SobreUva else p.tinta, 18f, acento = if (activo) SobreUva else Coral)
        }
        Spacer(Modifier.width(esc.d(10)))
        Column(Modifier.weight(1f)) {
            Texto(it.nombre, 15f, color = if (activo) SobreUva else p.tinta, lineas = 1)
            if (it.detalle.isNotBlank()) Detalle(it.detalle, 12f, color = if (activo) SobreUva.copy(alpha = 0.8f) else p.tenue, lineas = 1)
        }
        if (it.enUso) Box(Modifier.clip(RoundedCornerShape(50)).background(Sol.copy(alpha = 0.25f)).padding(horizontal = esc.d(8), vertical = esc.d(3))) {
            Texto("Sonando", 11f, color = if (activo) SobreUva else p.tinta, lineas = 1)
        }
    }
}

/** En la otra pantalla: cómo queda el tema elegido, en grande (o lo que suena). */
@Composable
fun VistaGaleria(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val it = remember(e) { Diptico.itemsGaleria(e).getOrNull(e.indice) }
    Box(Modifier.fillMaxSize().background(p.fondo).padding(esc.d(30)), contentAlignment = Alignment.Center) {
        when {
            e.galeria == "temas" && it?.tema != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth(0.8f).aspectRatio(1.6f).clip(RoundedCornerShape(esc.d(26)))) { PreviaTema(it.tema, e.oscuro) }
                Spacer(Modifier.height(esc.d(12)))
                Texto(it.nombre, 26f, lineas = 1)
                if (it.detalle.isNotBlank()) Detalle(it.detalle, 15f, lineas = 1)
            }
            e.galeria == "ia" && it != null -> ExplicaIA(it.id)
            e.galeria == "colores" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth(0.8f).aspectRatio(1.6f).clip(RoundedCornerShape(esc.d(26)))) { PreviaTema(cl.nikalbazu.diptico.ui.Estilo.tema, e.oscuro) }
                Spacer(Modifier.height(esc.d(12)))
                Texto("Así se ve tu interfaz", 22f, lineas = 1)
                Detalle(Diptico.pestanasGaleria("colores").getOrNull(e.galeriaPestana) ?: "", 15f, lineas = 1)
            }
            else -> Box(Modifier.fillMaxWidth(0.7f).aspectRatio(1.6f).clip(RoundedCornerShape(esc.d(26)))) { PreviaMusica(e) }
        }
    }
}


/**
 * Ajustes del widget elegido: tamaño, estilo, color, fondo, esquinas y página. ← y → cambian cada valor.
 * Va a un costado para ver el widget cambiar en vivo.
 */
@Composable
fun PanelAjusteWidget(e: Estado) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val opciones = remember(e) { Diptico.opcionesWidget() }
    val w = e.widgets.getOrNull(e.widgetSel)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        Column(
            Modifier.fillMaxSize(if (esc.angosta) 1f else 0.5f).padding(esc.d(10)).clip(RoundedCornerShape(esc.d(22)))
                .background(p.fondo2.copy(alpha = 0.97f)).border(esc.d(1), p.linea, RoundedCornerShape(esc.d(22)))
                .sinCerrar().padding(esc.d(12)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDiptico(Icono.EDITAR, p.tinta, 18f, acento = Coral)
                Spacer(Modifier.width(esc.d(8)))
                Column(Modifier.weight(1f)) {
                    Texto("Ajustes del widget", 17f, lineas = 1)
                    Detalle(w?.let { cl.nikalbazu.diptico.estado.Widgets.nombre(it.tipo) } ?: "", 12f, lineas = 1)
                }
            }
            Spacer(Modifier.height(esc.d(8)))
            val lista = androidx.compose.foundation.lazy.rememberLazyListState()
            LaunchedEffect(e.indice) { lista.animateScrollToItem((e.indice - 2).coerceAtLeast(0)) }
            androidx.compose.foundation.lazy.LazyColumn(state = lista, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(5))) {
                items(opciones.size) { i ->
                    val o = opciones[i]
                    val activo = i == e.indice
                    Row(
                        Modifier.fillMaxWidth().height(esc.d(46)).clip(RoundedCornerShape(esc.d(14)))
                            .then(if (activo) Modifier.border(esc.d(2), Uva, RoundedCornerShape(esc.d(14))) else Modifier)
                            .background(p.superficie.copy(alpha = if (activo) 1f else 0.6f))
                            .clickable { Diptico.actualizar { it.copy(indice = i) } }.padding(horizontal = esc.d(10)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Texto(o.titulo, 13.5f, lineas = 1, modifier = Modifier.weight(1f))
                        if (o.menos != null) {
                            Box(Modifier.size(esc.d(28)).clip(CircleShape).background(p.fondo).clickable { Diptico.actualizar { it.copy(indice = i) }; o.menos?.invoke() }, contentAlignment = Alignment.Center) { Texto("‹", 16f, traducir = false) }
                            Box(Modifier.width(esc.d(96)), contentAlignment = Alignment.Center) { Texto(o.valor, 12.5f, color = p.acentoTexto, lineas = 1) }
                            Box(Modifier.size(esc.d(28)).clip(CircleShape).background(p.fondo).clickable { Diptico.actualizar { it.copy(indice = i) }; o.accion() }, contentAlignment = Alignment.Center) { Texto("›", 16f, traducir = false) }
                        } else {
                            if (o.valor.isNotEmpty()) Texto(o.valor, 12.5f, color = p.acentoTexto, lineas = 1)
                            Spacer(Modifier.width(esc.d(6)))
                            Box(Modifier.clip(RoundedCornerShape(50)).background(p.fondo).clickable { Diptico.actualizar { it.copy(indice = i) }; o.accion() }.padding(horizontal = esc.d(10), vertical = esc.d(4))) {
                                Texto("Cambiar", 11.5f, lineas = 1)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(esc.d(6)))
            Pistas("← →" to "Cambiar", "↑ ↓" to "Moverte", "B" to "Listo")
        }
    }
}

/** En la otra pantalla: qué es cada IA, lo bueno, lo no tan bueno y cómo conseguirla. */
@Composable
private fun ExplicaIA(id: String) {
    val p = LocalPaleta.current
    val esc = LocalEscala.current
    val (sello, letra) = selloIA(id)
    val (titulo, bueno, malo, como) = when (id) {
        "local" -> listOf(
            "En tu consola", "Gratis para siempre, sin internet y privada: nada sale de tu consola.",
            "Es más simple que las de la nube, no busca guías en internet y ocupa 1,6 GB.",
            "Elígela y se baja sola. Puedes seguir usando Díptico mientras baja.",
        )
        "gemini" -> listOf(
            "Gemini · Google", "Muy lista, rápida y con un nivel gratis: basta tu cuenta de Google. Puede buscar en internet.",
            "El nivel gratis tiene un límite de preguntas por día. Necesita internet.",
            "Entra a aistudio.google.com/apikey con tu cuenta de Google, toca \"Create API key\", cópiala y pégala aquí.",
        )
        "openai" -> listOf(
            "ChatGPT · OpenAI", "Muy lista y conocida. Puede buscar en internet.",
            "Se paga por uso con saldo en tu cuenta de OpenAI (la suscripción ChatGPT Plus no sirve para esto).",
            "Entra a platform.openai.com/api-keys, crea una clave, agrega saldo en Billing y pégala aquí.",
        )
        "claude" -> listOf(
            "Claude · Anthropic", "Muy lista para conversar y ayudarte paso a paso. Puede buscar en internet.",
            "Se paga por uso con saldo en tu cuenta de Anthropic.",
            "Entra a console.anthropic.com, ve a API Keys, crea una clave y pégala aquí.",
        )
        else -> listOf(
            "Automático", "Díptico elige solo: primero la IA de tu consola y, si no está, la primera IA de la nube con clave.",
            "Si no tienes ninguna lista, Bisa te pedirá elegir una.",
            "Ideal si compartes Díptico: cada persona usa lo que tenga.",
        )
    }
    Column(Modifier.fillMaxWidth(0.86f), verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(esc.d(60)).clip(RoundedCornerShape(esc.d(16))).background(sello), contentAlignment = Alignment.Center) {
                if (id == "local") cl.nikalbazu.diptico.bisa.AvatarBisa(esc.d(48).value)
                else Texto(letra, 28f, color = Color.White, lineas = 1, traducir = false)
            }
            Spacer(Modifier.width(esc.d(16)))
            Texto(titulo, 28f, lineas = 1)
        }
        @Composable
        fun Fila(icono: Icono, color: Color, etiqueta: String, texto: String) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(esc.d(16))).background(p.superficie).padding(esc.d(14)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(esc.d(36)).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    IconoDiptico(icono, color, 18f, acento = color)
                }
                Spacer(Modifier.width(esc.d(12)))
                Column(Modifier.weight(1f)) {
                    Etiqueta(etiqueta, color, 11f)
                    Detalle(texto, 14f, color = p.tinta, lineas = 3)
                }
            }
        }
        Fila(Icono.CHECK, Color(0xFF2EC4A6), "Lo bueno", bueno)
        Fila(Icono.INFO, Coral, "A tener en cuenta", malo)
        Fila(Icono.CLAVE, Sol, "Cómo tenerla", como)
    }
}

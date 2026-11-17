package cl.nikalbazu.diptico.ui

import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.luminance
import androidx.compose.animation.togetherWith
import cl.nikalbazu.diptico.data.Consola
import cl.nikalbazu.diptico.data.Consolas
import cl.nikalbazu.diptico.estado.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/*
 * Colección viva · carpetas de consolas (bocetos 6a, 6b y 6c; docs/coleccion-viva.md).
 */

// ———————————————————————————— la consola en 3D ————————————————————————————

/** En las pruebas (Robolectric) no hay GPU: ahí la consola se muestra con un marcador. */
private val SIN_GPU = android.os.Build.FINGERPRINT == "robolectric"

/**
 * La consola en 3D de verdad: el modelo .glb que mandó Nicolás (va en el equipo, sin internet) dibujado con Filament.
 * Se gira libre con el dedo hacia cualquier lado (y se acerca con dos dedos), sin saltos; si nadie la toca, gira sola despacio.
 */
@Composable
fun Consola3D(c: Consola, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val id = Modelos3D.idDe(c.id)
    val existe = remember(id) { runCatching { ctx.assets.list("consolas3d")?.contains("$id.glb") == true }.getOrDefault(false) }
    val icono: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { IconoConsola(c, 260f, c.color.copy(alpha = .22f)) }
    }
    if (SIN_GPU || !Motor3D.permitido(ctx)) { Box(modifier.testTag("consola-3d")) { icono() }; return }
    val girar = LocalMovimiento.current
    // El visor queda SIEMPRE montado (sacarlo y volver a ponerlo es lo más delicado del motor): una consola sin modelo
    // solo vacía el visor y se ve su ícono encima.
    Box(modifier.testTag("consola-3d")) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { cx -> Motor3D.visorPara(cx) ?: android.view.View(cx) },
            update = { (it as? VisorConsola)?.mostrar(if (existe) id else null, girar) },
            modifier = Modifier.fillMaxSize(),
            onRelease = { (it as? VisorConsola)?.pausar() },
        )
        if (!existe) icono()
    }
}

/**
 * Un solo motor de Filament para toda la app y un visor guardado por pantalla que cambia de modelo.
 * Seguro: antes de tocar el motor queda anotado «dibujando»; si la app se cae ahí (esas caídas no dejan informe),
 * al volver a abrirla el 3D queda en pausa en esta versión y se ven los íconos. Lo mismo si se cierra dos veces seguidas.
 */
internal object Motor3D {
    private const val PREFS = "motor3d"
    private var revisado = false
    private var bloqueado = false
    val engine: com.google.android.filament.Engine by lazy {
        com.google.android.filament.utils.Utils.init()
        com.google.android.filament.Engine.create()
    }
    private val visores = java.util.WeakHashMap<android.content.Context, VisorConsola>()
    private fun clave() = "pausa_" + cl.nikalbazu.diptico.BuildConfig.VERSION_NAME

    fun permitido(ctx: android.content.Context): Boolean {
        if (!revisado) {
            revisado = true
            val p = ctx.getSharedPreferences(PREFS, 0)
            if (p.getBoolean("dibujando", false)) p.edit().putBoolean(clave(), true).putBoolean("dibujando", false).commit()
            bloqueado = p.getBoolean(clave(), false)
        }
        return !bloqueado
    }
    fun marcar(ctx: android.content.Context, dibujando: Boolean) =
        ctx.getSharedPreferences(PREFS, 0).edit().putBoolean("dibujando", dibujando).commit()
    /** Igual que [marcar], sin esperar al disco (para el paso a primer plano y al fondo). */
    fun marcarLuego(ctx: android.content.Context, dibujando: Boolean) =
        ctx.getSharedPreferences(PREFS, 0).edit().putBoolean("dibujando", dibujando).apply()
    fun pausarPorSeguridad(ctx: android.content.Context) {
        ctx.getSharedPreferences(PREFS, 0).edit().putBoolean(clave(), true).putBoolean("dibujando", false).commit()
        bloqueado = true; revisado = true
    }
    fun reactivar(ctx: android.content.Context) { ctx.getSharedPreferences(PREFS, 0).edit().clear().commit(); bloqueado = false }

    fun visorPara(cx: android.content.Context): VisorConsola? {
        visores[cx]?.let { if (it.parent == null) return it }
        // Anotado ANTES de crear el motor: si se cae al crearlo, la próxima vez no se intenta.
        marcar(cx, true)
        val v = runCatching { VisorConsola(cx) }.getOrNull()
        marcar(cx, false)
        if (v == null) { pausarPorSeguridad(cx); return null }
        visores[cx] = v
        return v
    }
}

/**
 * El visor: fondo transparente, la consola siempre al centro y del mismo tamaño (el modelo se ajusta a un cubo).
 * El dedo la gira (horizontal = alrededor, vertical = inclinar, con tope); al soltarla sigue con la inercia, vuelve a su
 * inclinación con suavidad y a los 2,5 s gira sola despacio. Solo se dibuja cuando algo cambia (máx. 60 cuadros).
 */
internal class VisorConsola(ctx: android.content.Context) :
    android.view.TextureView(ctx), android.view.Choreographer.FrameCallback {
    private val engine = Motor3D.engine
    private val ui = com.google.android.filament.android.UiHelper(com.google.android.filament.android.UiHelper.ContextErrorPolicy.DONT_CHECK).apply { isOpaque = false }
    private val visor = com.google.android.filament.utils.ModelViewer(this, engine, ui)
    private var corriendo = false
    private var girar = true
    private var idActual: String? = null
    private var pedido = 0
    private var ultimoToque = 0L
    private var yaw = 25f
    private var pitch = 0f
    private var velYaw = 0f
    private var tocando = false
    private var lx = 0f
    private var ly = 0f
    private var cuadrosPendientes = 0
    private val base = FloatArray(16)
    private val m1 = FloatArray(16)
    private val m2 = FloatArray(16)
    private val fin = FloatArray(16)

    init {
        isOpaque = false
        visor.view.blendMode = com.google.android.filament.View.BlendMode.TRANSLUCENT
        visor.renderer.clearOptions = visor.renderer.clearOptions.apply { clear = true }
        visor.scene.skybox = null
        visor.scene.indirectLight = com.google.android.filament.IndirectLight.Builder()
            .irradiance(1, floatArrayOf(1f, 1f, 1f)).intensity(30_000f).build(engine)
        // Sin sombras ni efectos caros: es un adorno, tiene que ser liviano.
        visor.view.setShadowingEnabled(false)
        setOnTouchListener { v, ev ->
            ultimoToque = android.os.SystemClock.uptimeMillis()
            when (ev.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> { v.parent?.requestDisallowInterceptTouchEvent(true); tocando = true; velYaw = 0f; lx = ev.x; ly = ev.y }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val dx = ev.x - lx; val dy = ev.y - ly; lx = ev.x; ly = ev.y
                    val grados = 360f / (width.coerceAtLeast(1) * 1.1f)
                    yaw += dx * grados              // el dedo a la derecha la gira hacia la derecha
                    pitch = (pitch + dy * grados).coerceIn(-35f, 35f)
                    velYaw = dx * grados
                    pedirCuadros()
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> { tocando = false; pedirCuadros() }
            }
            true
        }
    }

    private fun pedirCuadros(n: Int = 2) { cuadrosPendientes = maxOf(cuadrosPendientes, n) }

    /** Cambia el modelo sin rehacer nada: el archivo se lee aparte y se carga en el hilo de la pantalla. */
    fun mostrar(id: String?, girarSolo: Boolean) {
        girar = girarSolo
        if (id == idActual) return
        idActual = id
        val n = ++pedido
        if (id == null) {
            // Consola sin modelo: se vacía el visor (se ve su ícono encima).
            post { if (n == pedido) runCatching { visor.destroyModel(); pedirCuadros(2) } }
            return
        }
        val app = context.applicationContext
        Thread {
            // Si se pasa de consola en consola rápido, solo se carga la última (cargar y soltar seguido es lo más delicado).
            Thread.sleep(280)
            if (n != pedido) return@Thread
            val bytes = runCatching { app.assets.open("consolas3d/$id.glb").use { it.readBytes() } }.getOrNull() ?: return@Thread
            post {
                if (n != pedido) return@post
                runCatching {
                    visor.destroyModel()
                    val buffer = java.nio.ByteBuffer.allocateDirect(bytes.size).order(java.nio.ByteOrder.nativeOrder()).put(bytes)
                    buffer.rewind()
                    visor.loadModelGlb(buffer)
                    visor.transformToUnitCube()
                    visor.asset?.let { a -> val tm = engine.transformManager; tm.getTransform(tm.getInstance(a.root), base) }
                    // Parte mostrando su frente, apenas girada (tres cuartos), como en el boceto.
                    yaw = Modelos3D.frente(id) + 25f; pitch = 0f; velYaw = 0f
                    aplicarGiro()
                    pedirCuadros(4)
                }
            }
        }.apply { isDaemon = true }.start()
    }

    /**
     * Gira la consola sobre SU centro. `transformToUnitCube` la deja centrada en (0, 0, −4) frente a la cámara: el giro
     * se arma como «llevar al origen → girar → devolver a su lugar». (Antes giraba alrededor de la cámara y se salía de la
     * pantalla o quedaba cortada: era el «desaparece» del video.)
     */
    private fun aplicarGiro() {
        val a = visor.asset ?: return
        val cx = base[12]; val cy = base[13]; val cz = base[14]
        android.opengl.Matrix.setRotateM(m1, 0, pitch, 1f, 0f, 0f)
        android.opengl.Matrix.setRotateM(m2, 0, yaw, 0f, 1f, 0f)
        android.opengl.Matrix.multiplyMM(fin, 0, m1, 0, m2, 0)          // R = Rx · Ry
        android.opengl.Matrix.setIdentityM(m1, 0)
        android.opengl.Matrix.translateM(m1, 0, -cx, -cy, -cz)
        android.opengl.Matrix.multiplyMM(m2, 0, m1, 0, base, 0)         // T(−c) · base: centrada en el origen
        android.opengl.Matrix.multiplyMM(m1, 0, fin, 0, m2, 0)          // R · …
        m1[12] += cx; m1[13] += cy; m1[14] += cz                        // T(c) · …
        val tm = engine.transformManager
        tm.setTransform(tm.getInstance(a.root), m1)
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); seguir() }
    override fun onDetachedFromWindow() { pausar(); super.onDetachedFromWindow() }
    override fun onWindowVisibilityChanged(v: Int) {
        super.onWindowVisibilityChanged(v)
        if (v == android.view.View.VISIBLE) seguir() else pausar()
    }

    private fun seguir() {
        if (corriendo || !isAttachedToWindow) return
        corriendo = true
        Motor3D.marcarLuego(context, true)
        pedirCuadros(4)
        android.view.Choreographer.getInstance().postFrameCallback(this)
    }

    fun pausar() {
        if (!corriendo) return
        corriendo = false
        android.view.Choreographer.getInstance().removeFrameCallback(this)
        Motor3D.marcarLuego(context, false)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!corriendo) return
        android.view.Choreographer.getInstance().postFrameCallback(this)
        if (visor.asset == null) return
        var cambio = false
        if (!tocando) {
            // Inercia al soltar, y la inclinación vuelve sola a su lugar.
            if (kotlin.math.abs(velYaw) > 0.05f) { yaw += velYaw; velYaw *= 0.92f; cambio = true } else velYaw = 0f
            if (kotlin.math.abs(pitch) > 0.1f) { pitch *= 0.9f; cambio = true } else if (pitch != 0f) { pitch = 0f; cambio = true }
            if (girar && velYaw == 0f && android.os.SystemClock.uptimeMillis() - ultimoToque > 2500) { yaw += 0.25f; cambio = true }
        }
        if (cambio) { aplicarGiro(); pedirCuadros(1) }
        if (cuadrosPendientes <= 0) return
        cuadrosPendientes--
        visor.render(frameTimeNanos)
    }
}

// ———————————————————————————— 6a · Thor arriba ————————————————————————————

@Composable
fun CarpetasArribaCV(e: Estado) {
    val c = Consolas.porId(e.consolaSel) ?: e.consolasConJuegos.firstOrNull()
    LienzoBoceto(1920f, 1080f, fondo = LocalColeccion.current.fondo, zoom = ZOOM_ARRIBA) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val color = c?.color ?: col.acento
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(color.copy(alpha = .38f), Color.Transparent), radius = 1600f)))
        FondoTemaCV()
        val d1 = duracionMovimiento(280); val d2 = duracionMovimiento(360); val d3 = duracionMovimiento(180); val d4 = duracionMovimiento(220)
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(64), vertical = esc.d(28))) {
            Row(Modifier.fillMaxWidth().height(esc.d(70)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                TB("TUS CONSOLAS", 20f, color = col.sub, peso = FontWeight.ExtraBold, espaciado = 3f)
                Spacer(Modifier.weight(1f))
                BotonWidgetsCV(); HoraBateria(60f, 24f); BotonPerfil(64f)
            }
            if (c == null) { TB("Aún no hay consolas", 64f, titulo = true); return@Column }
            // Cambio de consola: la de antes se aleja y la nueva llega (zoom out / zoom in).
            Box(Modifier.fillMaxWidth().weight(1f)) {
            androidx.compose.animation.AnimatedContent(
                targetState = c.id,
                transitionSpec = {
                    (androidx.compose.animation.fadeIn(tween(d1)) + androidx.compose.animation.scaleIn(tween(d2, easing = CurvaPosarse), initialScale = 0.86f)) togetherWith
                        (androidx.compose.animation.fadeOut(tween(d3)) + androidx.compose.animation.scaleOut(tween(d4), targetScale = 0.86f))
                }, label = "consola", modifier = Modifier.fillMaxSize(),
            ) { id ->
                val k = Consolas.porId(id) ?: return@AnimatedContent
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(esc.d(28))) {
                    Column(Modifier.weight(0.56f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(14), Alignment.CenterVertically)) {
                        InfoConsolaCV(e, k, tamNombre = 72f, lineasHistoria = 2)
                        PersonalizarConsolaCV(e, k)
                    }
                    Spacer(Modifier.weight(0.44f))
                }
            }
            // La consola 3D va fuera de la animación: un solo visor que cambia de modelo.
            Column(Modifier.fillMaxWidth(0.44f).fillMaxHeight().align(Alignment.CenterEnd), horizontalAlignment = Alignment.CenterHorizontally) {
                Consola3D(c, Modifier.fillMaxWidth().weight(1f))
                Modelos3D.credito(c.id)?.let { TB("$it · gírala con el dedo", 16f, color = col.tenue, lineas = 1, traducir = false) }
            }
            }
            // Acciones a la izquierda; a la derecha cómo cambiar de consola y de vista, en una línea fina.
            Row(Modifier.fillMaxWidth().height(esc.d(90)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
                AccionesConsolaCV(e, c, alto = 80f)
                Spacer(Modifier.weight(1f))
                PistasFinas("L1 R1" to "Otra consola", "Select" to "Vista", "Mantén Select" to "Cambiar pantallas")
            }
        }
    }
}


@Composable
fun InfoConsolaCV(e: Estado, c: Consola, tamNombre: Float, lineasHistoria: Int = 4) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val f = HistoriaConsolas.de(c.id)
    val juegos = e.juegosPorConsola[c.id].orEmpty()
    val horas = juegos.sumOf { e.datos[it.clave]?.tiempo ?: 0L }
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
        if (f != null) TB("${f.marca} · ${f.anio} · ${f.generacion}", 22f, color = androidx.compose.ui.graphics.lerp(c.color, Color.White, .55f), peso = FontWeight.ExtraBold, espaciado = 3f)
        val conLogo = c.id in e.extra.logoConsolas
        val logoUri = e.consolasPropias[c.id]?.logo?.let { android.net.Uri.parse(it) }
        val logo = if (conLogo) rememberImagen(logoUri, 900) else null
        if (conLogo && logo != null) androidx.compose.foundation.Image(logo, c.bonito, contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            alignment = Alignment.CenterStart, modifier = Modifier.height(esc.d(tamNombre * 1.3f)).fillMaxWidth())
        else if (conLogo) Box(Modifier.size(esc.d(620), esc.d(150)).border(3.dp, col.texto.copy(alpha = .3f), RoundedCornerShape(esc.d(24)))
            .clickable { Diptico.cambiarIconoCarpeta(c.id) }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TB("Logo de ${c.bonito}", 24f, peso = FontWeight.ExtraBold)
                TB("Elígelo en Ajustes › Tus consolas › Logo", 17f, color = col.tenue)
            }
        }
        else TB(e.consolasPropias[c.id]?.nombre ?: c.bonito, tamNombre, titulo = true, lineas = 2, interlineado = tamNombre * 0.95f, traducir = false)
        Box(Modifier.height(esc.d(48)).clip(RoundedCornerShape(50)).background(col.vidrio).border(1.dp, col.texto.copy(alpha = .18f), RoundedCornerShape(50))
            .clickable { Diptico.alternarLogoConsola(c.id) }.padding(horizontal = esc.d(20)), contentAlignment = Alignment.Center) {
            TB(if (conLogo) "Mostrar el nombre" else "Mostrar el logo", 19f, peso = FontWeight.ExtraBold)
        }
        if (f != null) TB(f.historia, 23f, color = Color(0xFFD3D5EA).let { if (col.texto.luminance() > .5f) it else col.sub }, peso = FontWeight.SemiBold, lineas = lineasHistoria, interlineado = 32f)
        Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
            ChipDato("${juegos.size} juegos", 21f)
            if (horas > 0) ChipDato("${formatoTiempo(horas)} jugadas", 21f)
        }
    }
}


/** Las 6 opciones de la consola (X): ícono, vista, color, emulador, fondo y música, nombre o logo. */
/**
 * Las opciones de la consola juntas en una sola tarjeta ordenada (antes eran seis casillas sueltas): un resumen de cómo
 * está cada cosa y un solo lugar para cambiarlas (X).
 */
@Composable
fun PersonalizarConsolaCV(e: Estado, c: Consola) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val cp = e.consolasPropias[c.id]
    val resumen = listOf(
        "Vista" to nombreVistaCarpetas(e.extra.vistaCarpetasCV),
        "Emulador" to Diptico.nombreEmulador(c),
        "Ícono" to if (cp?.portadaCarpeta != null) "Propio" else "Original",
    )
    Row(
        Modifier.fillMaxWidth().height(esc.d(132)).vidrio(30f, fuerte = true).clickable { Diptico.abrirEditarConsola(c.id) }
            .padding(horizontal = esc.d(22)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(20)),
    ) {
        Box(Modifier.size(esc.d(84)).clip(RoundedCornerShape(esc.d(24))).background(c.color.copy(alpha = .3f)), contentAlignment = Alignment.Center) {
            IconoDiptico(Icono.AJUSTES, col.texto, 44f, acento = Coral)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
            TB("Personalizar ${c.bonito}", 28f, titulo = true, lineas = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
                resumen.forEach { (n, v) ->
                    Row(Modifier.clip(RoundedCornerShape(50)).background(col.texto.copy(alpha = .08f)).padding(horizontal = esc.d(12), vertical = esc.d(5)),
                        horizontalArrangement = Arrangement.spacedBy(esc.d(5))) {
                        TB(n, 16f, color = col.sub, lineas = 1)
                        TB(v, 16f, peso = FontWeight.ExtraBold, lineas = 1, traducir = false)
                    }
                }
            }
        }
        Box(Modifier.size(esc.d(54)).clip(CircleShape).border(2.dp, col.texto, CircleShape), contentAlignment = Alignment.Center) { TB("X", 22f, peso = FontWeight.ExtraBold, traducir = false) }
    }
}

@Composable
fun OpcionesConsolaCV(e: Estado, c: Consola, columnas: Int, alto: Float = 78f) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val opciones = listOf(
        Triple("Ícono de la carpeta", if (e.consolasPropias[c.id]?.portadaCarpeta != null) "Propio ›" else "Elegir ›", { Diptico.cambiarIconoCarpeta(c.id) }),
        Triple("Vista", nombreVistaCarpetas(e.extra.vistaCarpetasCV), { Diptico.siguienteVistaCarpetas() }),
        Triple("Color", if ((e.consolasPropias[c.id]?.color ?: 0L) != 0L) "Propio" else "De la consola", { Diptico.abrirEditarConsola(c.id) }),
        Triple("Emulador", Diptico.nombreEmulador(c), { Diptico.abrirEditarConsola(c.id) }),
        Triple("Fondo y música", if (e.consolasPropias[c.id]?.musica != null) "Propios" else "Del tema", { Diptico.abrirEditarConsola(c.id) }),
        Triple("Nombre o logo", if (c.id in e.extra.logoConsolas) "Logo" else "Nombre", { Diptico.alternarLogoConsola(c.id) }),
    )
    Column(verticalArrangement = Arrangement.spacedBy(esc.d(10))) {
        TB("OPCIONES DE ESTA CONSOLA · X", 19f, color = Color(0xFF9A9CB8), peso = FontWeight.ExtraBold, espaciado = 2.3f)
        opciones.chunked(columnas).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                fila.forEach { (n, v, accion) ->
                    Column(Modifier.weight(1f).height(esc.d(alto)).vidrio(20f, fuerte = true).clickable { accion() }.padding(horizontal = esc.d(16)),
                        verticalArrangement = Arrangement.Center) {
                        TB(n, 18f, color = col.sub)
                        TB(v, 22f, peso = FontWeight.ExtraBold, traducir = false)
                    }
                }
                repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AccionesConsolaCV(e: Estado, c: Consola, alto: Float = 84f) {
    val esc = LocalEscala.current
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
        BotonTecla("A", "Entrar", { Diptico.entrarConsolaCV(c.id) }, alto = alto, principal = true, letra = 28f)
        ultimoDeConsola(e, c.id)?.let { j -> BotonTecla("Y", "Seguir con ${j.titulo}", { Diptico.seguirCon(j) }, alto = alto, letra = 24f) }
            ?: BotonTecla("Y", "Cambiar ícono", { Diptico.cambiarIconoCarpeta(c.id) }, alto = alto, letra = 24f)
    }
}

// ———————————————————————————— 6b · Thor abajo ————————————————————————————

@Composable
fun CarpetasAbajoCV(e: Estado) {
    LienzoBoceto(1240f, 1080f, fondo = LocalColeccion.current.fondo2, zoom = ZOOM_ABAJO) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        FondoTemaCV()
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(26), vertical = esc.d(20)), verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
            // Arriba: DIPPO · Consolas | Colecciones · las vistas (Ícono, Díptico, Repisa, Carpeta).
            Row(Modifier.fillMaxWidth().height(esc.d(76)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(10))) {
                if (e.extra.dippoInicio) Box(Modifier.size(esc.d(72)).clip(CircleShape).background(col.vidrio).clickable { Diptico.abrirAsistente() }, contentAlignment = Alignment.Center) {
                    cl.nikalbazu.diptico.bisa.AvatarBisa(52f * esc.factor)
                }
                listOf("Consolas", "Colecciones").forEachIndexed { i, n ->
                    val on = e.extra.pestanaCarpetas == i
                    Box(Modifier.height(esc.d(64)).clip(RoundedCornerShape(50)).background(if (on) col.elegido else col.vidrio)
                        .then(if (on) Modifier else Modifier.border(1.dp, col.borde, RoundedCornerShape(50)))
                        .clickable { Diptico.elegirPestanaCarpetas(i) }.padding(horizontal = esc.d(20)), contentAlignment = Alignment.Center) {
                        TB(n, 23f, color = if (on) col.enElegido else col.texto, peso = FontWeight.ExtraBold)
                    }
                }
                Spacer(Modifier.weight(1f))
                // El dock de apps a la medida de 5 apps (las vistas se cambian con Select, no hace falta otro botón).
                DockCV(e, 54f)
            }
            if (e.extra.pestanaCarpetas == 1) ColeccionesCV(e, Modifier.fillMaxWidth().weight(1f))
            else RejillaCarpetasCV(e, Modifier.fillMaxWidth().weight(1f), unaPantalla = false)
            PistasFinas("A" to "Entrar", "X" to "Opciones", "Select" to nombreVistaCarpetas(e.extra.vistaCarpetasCV), modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Colecciones: por ahora, tus favoritos y lo jugado hace poco como carpetas especiales. */
@Composable
private fun ColeccionesCV(e: Estado, modifier: Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val favoritos = e.juegos.filter { e.datos[it.clave]?.favorito == true }
    val recientes = e.juegos.filter { (e.datos[it.clave]?.ultimo ?: 0L) > 1 }.sortedByDescending { e.datos[it.clave]?.ultimo ?: 0L }.take(12)
    val sinJugar = e.juegos.filter { (e.datos[it.clave]?.tiempo ?: 0L) == 0L }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(esc.d(14))) {
        listOf(Triple("Favoritos", "♥", favoritos), Triple("Jugados hace poco", "⏱", recientes), Triple("Aún sin jugar", "✦", sinJugar)).forEach { (n, s, l) ->
            Row(Modifier.fillMaxWidth().height(esc.d(150)).vidrio(26f, fuerte = true).clickable {
                l.firstOrNull()?.let { Diptico.elegirJuego(it.clave); Diptico.entrarSistema() }
            }.padding(esc.d(16)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                Box(Modifier.size(esc.d(110)).clip(RoundedCornerShape(esc.d(26))).background(Brush.linearGradient(listOf(col.acento, Color(0xFF0D0F1C)))), contentAlignment = Alignment.Center) {
                    TB(s, 44f, color = Color.White, traducir = false)
                }
                Column(Modifier.weight(1f)) {
                    TB(n, 28f, titulo = true)
                    TB("${l.size} juegos", 20f, color = col.sub, traducir = false)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                    l.take(3).forEach { j -> Box(Modifier.size(esc.d(64), esc.d(90)).clip(RoundedCornerShape(esc.d(6)))) { ArteJuego(j, preferirArte = false, maxLado = 200, modifier = Modifier.fillMaxSize()) } }
                }
            }
        }
    }
}


@Composable
fun SelectorVistasCarpetas(e: Estado, alto: Float = 56f, letra: Float = 22f, modifier: Modifier = Modifier) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Row(modifier.clip(RoundedCornerShape(50)).background(col.vidrio).border(1.dp, col.borde, RoundedCornerShape(50)).padding(esc.d(6)),
        horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
        VISTAS_CARPETAS_CV.forEach { v ->
            val on = e.extra.vistaCarpetasCV == v
            Box((if (modifier != Modifier) Modifier.weight(1f) else Modifier).height(esc.d(alto)).clip(RoundedCornerShape(50)).background(if (on) col.elegido else Color.Transparent)
                .clickable { Diptico.elegirVistaCarpetas(v) }.padding(horizontal = esc.d(alto * 0.4f)), contentAlignment = Alignment.Center) {
                TB(nombreVistaCarpetas(v), letra, color = if (on) col.enElegido else col.texto, peso = FontWeight.ExtraBold)
            }
        }
    }
}

/** Las carpetas de las consolas en la vista elegida: Ícono, Díptico, Repisa o Carpeta. */
@Composable
fun RejillaCarpetasCV(e: Estado, modifier: Modifier, unaPantalla: Boolean) {
    val esc = LocalEscala.current
    val ids = e.pestanas.filter { it != "todo" }
    val sel = ids.indexOf(e.consolaSel).coerceAtLeast(0)
    val vista = e.extra.vistaCarpetasCV
    val columnas = columnasCarpetasCV(vista, unaPantalla).let { if (it < 0) e.extra.columnasIconoCV else it }
    androidx.compose.runtime.SideEffect { Teclas.carrilesCarpetas = columnas }
    val estado = rememberLazyGridState()
    LaunchedEffect(sel, vista) { estado.animateScrollToItem((sel - columnas).coerceAtLeast(0)) }
    // Pellizcar con dos dedos (vista Ícono): abrir agranda los íconos (menos por fila), cerrar los achica.
    val columnasAhora = androidx.compose.runtime.rememberUpdatedState(columnas)
    val pellizco = if (vista != "icono") Modifier else Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var acumulado = 1f
            while (true) {
                val ev = awaitPointerEvent()
                if (ev.changes.none { it.pressed }) break
                if (ev.changes.count { it.pressed } >= 2) {
                    acumulado *= ev.calculateZoom()
                    ev.changes.forEach { it.consume() }
                    if (acumulado > 1.18f) { Diptico.ajustarColumnasIcono(columnasAhora.value - 1); acumulado = 1f }
                    if (acumulado < 0.85f) { Diptico.ajustarColumnasIcono(columnasAhora.value + 1); acumulado = 1f }
                }
            }
        }
    }
    LazyVerticalGrid(GridCells.Fixed(columnas), modifier.then(pellizco), estado,
        horizontalArrangement = Arrangement.spacedBy(esc.d(if (vista == "icono") 10f else 24f)),
        verticalArrangement = Arrangement.spacedBy(esc.d(if (vista == "repisa") 12f else 22f)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = esc.d(10))) {
        itemsIndexed(ids, key = { _, id -> id }) { i, id ->
            val c = Consolas.porId(id) ?: return@itemsIndexed
            val elegida = i == sel
            val entrar = { if (elegida) Diptico.entrarConsolaCV(id) else Diptico.elegirConsola(id) }
            when (vista) {
                "icono" -> CarpetaIcono(e, c, elegida, unaPantalla, entrar)
                "repisa" -> CarpetaRepisa(e, c, elegida, unaPantalla, entrar)
                "carpeta" -> CarpetaClasica(e, c, elegida, unaPantalla, entrar)
                else -> CarpetaDiptico(e, c, elegida, unaPantalla, entrar)
            }
        }
    }
}

@Composable
private fun anilloCV(elegida: Boolean, forma: androidx.compose.ui.graphics.Shape): Modifier {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    return if (elegida) Modifier.border(esc.d(4), col.elegido, forma) else Modifier
}

/** El ícono de la carpeta: el propio (6d) o el degradado del color con la sigla. */
@Composable
private fun IconoCarpetaCV(e: Estado, c: Consola, modifier: Modifier, sigla: Float) {
    val propia = e.consolasPropias[c.id]?.portadaCarpeta
    Box(modifier.background(Brush.linearGradient(listOf(c.color, Color(0xFF0D0F1C)))), contentAlignment = Alignment.Center) {
        if (propia != null) PortadaCarpetaRuta(propia, c.id, Modifier.fillMaxSize(), maxLado = 360)
        else IconoConsola(c, sigla, Color.Transparent)
    }
}

@Composable
private fun CarpetaIcono(e: Estado, c: Consola, elegida: Boolean, una: Boolean, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val forma = RoundedCornerShape(24)
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
        IconoCarpetaCV(e, c, Modifier.fillMaxWidth(0.88f).aspectRatio(1f).graphicsLayer { scaleX = if (elegida) 1.05f else 1f; scaleY = scaleX }
            .clip(forma).then(anilloCV(elegida, forma)), 40f)
        TB(c.bonito, 20f, color = if (elegida) col.texto else col.sub)
    }
}

/** Los mini-juegos al lado del ícono (los últimos, los favoritos o los elegidos; 1 a 3). */
@Composable
private fun MiniJuegos(e: Estado, c: Consola, ancho: Float, alto: Float) {
    val esc = LocalEscala.current
    val juegos = juegosJuntoAlIcono(e, c.id, e.extra.juegosCarpeta, e.extra.cuantosCarpeta)
    Row(horizontalArrangement = Arrangement.spacedBy(esc.d(8))) {
        juegos.forEach { j ->
            Box(Modifier.size(esc.d(ancho), esc.d(alto)).clip(RoundedCornerShape(esc.d(5))).background(c.color.copy(alpha = .4f))) {
                ArteJuego(j, preferirArte = false, maxLado = 200, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun CarpetaDiptico(e: Estado, c: Consola, elegida: Boolean, una: Boolean, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val alto = if (una) 150f else 150f
    val forma = RoundedCornerShape(esc.d(26))
    Row(Modifier.fillMaxWidth().height(esc.d(alto)).clip(forma).background(col.vidrioFuerte).border(1.dp, col.borde, forma).then(anilloCV(elegida, forma)).clickable(onClick = onClick)) {
        IconoCarpetaCV(e, c, Modifier.width(esc.d(if (una) alto * 0.7f else alto)).fillMaxHeight(), 40f)
        Box(Modifier.width(esc.d(6)).fillMaxHeight().background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .35f), Color.White.copy(alpha = .08f)))))
        Column(Modifier.weight(1f).fillMaxHeight().padding(esc.d(16)), verticalArrangement = Arrangement.SpaceBetween) {
            MiniJuegos(e, c, 46f, 64f)
            Column {
                TB(c.bonito, 24f, titulo = true, peso = FontWeight.SemiBold)
                TB("${e.juegosPorConsola[c.id].orEmpty().size} juegos", 19f, color = col.sub)
            }
        }
    }
}

@Composable
private fun CarpetaRepisa(e: Estado, c: Consola, elegida: Boolean, una: Boolean, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val forma = RoundedCornerShape(esc.d(26))
    val juegos = e.juegosPorConsola[c.id].orEmpty()
    Row(Modifier.fillMaxWidth().height(esc.d(if (una) 160f else 190f)).clip(forma)
        .background(if (elegida) col.texto.copy(alpha = .08f) else Color.Transparent)
        .border(if (elegida) esc.d(3) else 1.dp, if (elegida) col.elegido else Color.Transparent, forma)
        .clickable(onClick = onClick).padding(horizontal = esc.d(24)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(20))) {
        Column(Modifier.width(esc.d(200)), verticalArrangement = Arrangement.spacedBy(esc.d(6))) {
            IconoCarpetaCV(e, c, Modifier.size(esc.d(56)).clip(RoundedCornerShape(esc.d(16))), 24f)
            TB(c.bonito, 24f, titulo = true, peso = FontWeight.SemiBold, lineas = 2, interlineado = 26f)
            TB("${juegos.size} juegos", 19f, color = col.sub)
        }
        // Los lomos de pie sobre la repisa (los primeros juegos de la consola, con su portada recortada).
        Box(Modifier.weight(1f).height(esc.d(if (una) 120f else 160f))) {
            Row(Modifier.fillMaxSize().padding(start = esc.d(10)), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                val lomos = juegosJuntoAlIcono(e, c.id, e.extra.juegosCarpeta, 3) + juegos.take(12)
                lomos.distinctBy { it.clave }.take(12).forEachIndexed { k, j ->
                    Box(Modifier.width(esc.d(30f + (k * 7) % 14)).fillMaxHeight(0.74f + ((k * 13) % 22) / 100f).clip(RoundedCornerShape(esc.d(4)))
                        .background(c.color.copy(alpha = .5f))) {
                        ArteJuego(j, preferirArte = false, maxLado = 120, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().offset(y = esc.d(10)).height(esc.d(10)).clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .22f), Color.White.copy(alpha = .04f)))))
        }
    }
}

@Composable
private fun CarpetaClasica(e: Estado, c: Consola, elegida: Boolean, una: Boolean, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    val alto = if (una) 170f else 220f
    val juegos = juegosJuntoAlIcono(e, c.id, e.extra.juegosCarpeta, e.extra.cuantosCarpeta)
    Box(Modifier.fillMaxWidth().height(esc.d(alto)).clickable(onClick = onClick)) {
        // La carpeta de atrás, los juegos asomando y el frente de vidrio.
        Box(Modifier.fillMaxSize().padding(start = esc.d(10), end = esc.d(10), top = esc.d(22)).clip(RoundedCornerShape(esc.d(24))).background(c.color.copy(alpha = .53f)))
        juegos.forEachIndexed { k, j ->
            val pw = if (una) 70f else 86f
            Box(Modifier.offset(esc.d(30f + k * (pw - 6f)), esc.d(4f - k * 2f)).size(esc.d(pw), esc.d(pw * 1.38f))
                .graphicsLayer { rotationZ = listOf(-7f, 5f, -3f, 6f, -4f)[k % 5] }.clip(RoundedCornerShape(esc.d(6)))) {
                ArteJuego(j, preferirArte = false, maxLado = 200, modifier = Modifier.fillMaxSize())
            }
        }
        val forma = RoundedCornerShape(esc.d(24))
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(esc.d(alto * 0.6f)).clip(forma).background(col.vidrioFuerte)
            .border(1.dp, Color.White.copy(alpha = .14f), forma).then(anilloCV(elegida, forma)).padding(esc.d(18)),
            verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(esc.d(14))) {
            IconoCarpetaCV(e, c, Modifier.size(esc.d(if (una) 52f else 60f)).clip(RoundedCornerShape(esc.d(16))), 22f)
            Column {
                TB(c.bonito, if (una) 23f else 26f, titulo = true, peso = FontWeight.SemiBold)
                TB("${e.juegosPorConsola[c.id].orEmpty().size} juegos", 19f, color = col.sub)
            }
        }
    }
}

// ———————————————————————————— 6c · una pantalla ————————————————————————————

@Composable
fun CarpetasUnicaCV(e: Estado) {
    val c = Consolas.porId(e.consolaSel) ?: e.consolasConJuegos.firstOrNull()
    LienzoBoceto(1920f, 1080f, fondo = LocalColeccion.current.fondo, zoom = ZOOM_UNA) {
        val esc = LocalEscala.current
        val col = LocalColeccion.current
        val color = c?.color ?: col.acento
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(color.copy(alpha = .38f), Color.Transparent), radius = 1500f)))
        FondoTemaCV()
        Column(Modifier.fillMaxSize().padding(horizontal = esc.d(40), vertical = esc.d(18)), verticalArrangement = Arrangement.spacedBy(esc.d(12))) {
            // Franja 1: vistas · hora, batería y perfil.
            Row(Modifier.fillMaxWidth().height(esc.d(70)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(12))) {
                DockCV(e, 52f)
                Spacer(Modifier.weight(1f))
                HoraBateria(56f, 22f); BotonPerfil(58f)
            }
            if (c == null) return@Column
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(esc.d(26))) {
                // Izquierda: la consola, su historia, sus opciones y A/Y.
                Column(Modifier.weight(0.5f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(12), Alignment.CenterVertically)) {
                    InfoConsolaCV(e, c, tamNombre = 60f, lineasHistoria = 2)
                    PersonalizarConsolaCV(e, c)
                    AccionesConsolaCV(e, c, alto = 64f)
                }
                // Derecha: la consola en 3D, qué juegos van junto al ícono y las carpetas.
                Column(Modifier.weight(0.5f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(esc.d(8))) {
                    Consola3D(c, Modifier.fillMaxWidth().weight(0.8f))
                    Modelos3D.credito(c.id)?.let { TB(it, 15f, color = col.tenue, lineas = 1, traducir = false) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(6))) {
                        TB("JUNTO AL ÍCONO", 15f, color = Color(0xFF9A9CB8), peso = FontWeight.ExtraBold, espaciado = 1.4f)
                        listOf("ultimos" to "Últimos", "favoritos" to "Favoritos", "elegidos" to "Elegidos").forEach { (m, n) ->
                            ChipElegir(n, e.extra.juegosCarpeta == m) { Diptico.elegirJuegosCarpeta(m) }
                        }
                        Spacer(Modifier.weight(1f))
                        (1..5).forEach { q -> ChipElegir("$q", e.extra.cuantosCarpeta == q) { Diptico.elegirCuantosCarpeta(q) } }
                    }
                    RejillaCarpetasCV(e, Modifier.fillMaxWidth().weight(1.2f), unaPantalla = true)
                }
            }
            // Abajo: DIPPO y el dock de apps.
            Row(Modifier.fillMaxWidth().height(esc.d(84)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(esc.d(16))) {
                if (e.extra.dippoInicio) DippoFrase("Elige una consola y aprieta A. Select cambia cómo se ven las carpetas; si lo mantienes, cambias de pantalla.", Modifier.weight(1f), cara = 58f, letra = 20f, maxAncho = 900f)
                else Spacer(Modifier.weight(1f))
                BotonRedondo(Icono.BUSCAR, 64f, { Diptico.abrirBuscar() }, descripcion = "Buscar")
                BotonRedondo(Icono.AJUSTES, 64f, { Diptico.abrirMenu() }, descripcion = "Ajustes")
            }
        }
    }
}


@Composable
private fun ChipElegir(texto: String, on: Boolean, onClick: () -> Unit) {
    val esc = LocalEscala.current
    val col = LocalColeccion.current
    Box(Modifier.height(esc.d(44)).clip(RoundedCornerShape(50)).background(if (on) col.elegido else col.vidrioFuerte).border(1.dp, col.borde, RoundedCornerShape(50))
        .clickable(onClick = onClick).padding(horizontal = esc.d(16)), contentAlignment = Alignment.Center) {
        TB(texto, 18f, color = if (on) col.enElegido else col.texto, peso = FontWeight.ExtraBold)
    }
}

package cl.nikalbazu.diptico.ui

import cl.nikalbazu.diptico.estado.Panel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import cl.nikalbazu.diptico.estado.Estado

/**
 * Lo que muestra una pantalla: el tutorial si falta configurar, o el inicio que le toca.
 * Encima van el reloj grande y la animación de entrada.
 */
@Composable
fun Raiz(e: Estado, rol: Rol, miPantalla: Int, rolIntro: Rol = rol) {
    val barra = e.configurado && !(rol == Rol.INFERIOR && (e.unaPantalla || e.vista == "mosaicos")) && !nuevoDisenoDosPantallas(e)
    val esc = LocalEscala.current
    CompositionLocalProvider(
        LocalBarraSistema provides barra,
        LocalIconos provides IconosCfg(e.estiloIconos, e.iconosPropios, e.iconosVersion),
        LocalFormas provides remember(e.formas, e.formaTodos, e.consolaSel) { cl.nikalbazu.diptico.estado.Diptico.formasVisibles(e) },
        LocalHeroes provides (e.heroes to e.imagenHeroe),
        LocalEncuadres provides cl.nikalbazu.diptico.estado.Diptico.encuadresVisibles(e),
        LocalZoomLento provides (e.zoomLento && e.experiencia.movimiento && e.sesion == null),
        LocalTamJuegos provides e.tamJuegos,
        LocalCristal provides recordarCristal(e),
        LocalMovimiento provides (e.experiencia.movimiento && e.extra.animaciones != "apagadas" && Estilo.efectosCompletos && android.animation.ValueAnimator.areAnimatorsEnabled()),
        LocalAjusteMovimiento provides AjusteMovimiento(
            intensidad = when (e.extra.animaciones) { "apagadas" -> 0f; "suaves" -> 0.6f; "intensas" -> 1.5f; else -> 1f },
            duracion = when (e.extra.velocidadAnim) { "lenta" -> 1.45f; "rapida" -> 0.65f; else -> 1f },
        ),
    ) {
        // El centro de control vive siempre en la pantalla de arriba (la principal), aunque se intercambien.
        val arribaFisica = miPantalla == android.view.Display.DEFAULT_DISPLAY &&
            (rol == Rol.UNICA || rol == (if (cl.nikalbazu.diptico.estado.Diptico.intercambioEfectivo(e)) Rol.INFERIOR else Rol.SUPERIOR))
        val centroAqui = e.panel == Panel.CENTRO && arribaFisica
        val tienda = e.panel == Panel.GALERIA && e.galeria == "tienda_temas"
        // Desenfoque detrás de lo que se abre encima: el centro de control, la bienvenida, el reloj grande o la continuación.
        val encima = centroAqui || e.dialogo != null || e.donde != null || e.quedaste || e.marcandoMapa != null || (e.novedades >= 0 && !e.intro) || (e.bienvenida > 0L && !e.intro) || e.relojGrande || (e.continuacion > 0L && e.sesion?.let { miPantalla != it.displayId } == true)
        val desenfoque by animateDpAsState(if (encima && Estilo.efectosCompletos) 18.dp else 0.dp, tween(300), label = "desenfoque")
        // Deslizar hacia abajo desde el borde de arriba: por la izquierda, notificaciones; por la derecha, ajustes rápidos.
        val libre by androidx.compose.runtime.rememberUpdatedState(e.configurado && e.panel == Panel.NINGUNO && e.dialogo == null && !e.intro)
        Box(Modifier.fillMaxSize().cortinaDesdeArriba { libre }) {
            Box(Modifier.fillMaxSize().padding(top = esc.d(if (barra && barraOcupaEspacio(e, miPantalla == android.view.Display.DEFAULT_DISPLAY)) ALTO_BARRA_SISTEMA else 0f))) {
                if (Estilo.tema.forma == "cristal" && Estilo.efectosCompletos && e.sesion == null &&
                    (e.panel in setOf(Panel.MENU, Panel.APPS, Panel.GALERIA, Panel.ESTUDIO) || (rol == Rol.INFERIOR && e.inicio != "widgets"))) {
                    e.seleccionado?.takeIf { (it.arte ?: it.portada)?.scheme?.let { s -> s != "app" } == true }?.let { juego ->
                        ArteJuego(juego, preferirArte = true, maxLado = 320,
                            modifier = Modifier.fillMaxSize().blur(24.dp).graphicsLayer { alpha = 0.24f })
                    }
                }
                Box(Modifier.fillMaxSize().then(if (desenfoque > 0.dp) Modifier.blur(desenfoque) else Modifier)) {
                    when {
                        !e.configurado -> if (rol == Rol.INFERIOR) TutorialAbajo(e) else Tutorial(e)
                        tienda && (arribaFisica || rol == Rol.UNICA) -> PanelGaleria(e, conPrevia = rol == Rol.UNICA)
                        tienda -> ControlesTiendaTemas(e)
                        rol == Rol.UNICA -> PantallaUnica(e)
                        rol == Rol.SUPERIOR -> if (e.fichaFijada != null && e.panel == Panel.NINGUNO && e.sesion == null) {
                            PantallaSuperior(e.copy(juegoSel = e.fichaFijada, consolaSel = "todo"))
                        } else PantallaSuperior(e)
                        else -> PantallaInferior(e, miPantalla)
                    }
                }
                AnimatedVisibility(visible = centroAqui, enter = fadeIn(tween(160)), exit = fadeOut(tween(200))) { PanelCentro(e) }
                // Mientras juegas: DIPPO se abre en la pantalla libre (la que no tiene el juego).
                val s = e.sesion
                val companeraLibre = s != null && !e.unaPantalla && !s.dosPantallas && miPantalla != s.displayId
                if (companeraLibre && e.panel == Panel.ASISTENTE) PanelAsistente(e)
                if (companeraLibre && rol == Rol.SUPERIOR && e.panel == Panel.CLAVE &&
                    e.textoPara in setOf("asistente", "bisa_paso")) PanelClave(e)
                AnimatedVisibility(
                    visible = e.relojGrande && rol != Rol.INFERIOR,
                    enter = fadeIn(tween(250)) + scaleIn(tween(320), initialScale = 0.9f),
                    exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 1.05f),
                ) { RelojGrande(e) }
                // Teclado de Díptico: en la pantalla contraria a la del texto.
                AnimatedVisibility(
                    visible = e.conTeclado && if (s != null && e.textoPara in setOf("asistente", "bisa_paso") && e.panel == Panel.CLAVE)
                        companeraLibre else (rol == Rol.SUPERIOR || rol == Rol.UNICA),
                    enter = fadeIn(tween(200)) + androidx.compose.animation.slideInVertically(tween(300)) { it / 3 },
                    exit = fadeOut(tween(160)),
                ) { TecladoDiptico(e) }
                if (e.protector && !(rol == Rol.INFERIOR && e.unaPantalla)) Protector(e, rol)
                // Bienvenida al encender (y el recordatorio si estabas jugando), encima de todo menos la animación de entrada.
                AnimatedVisibility(
                    visible = e.bienvenida > 0L && !e.intro && !(rol == Rol.INFERIOR && e.unaPantalla),
                    enter = fadeIn(tween(380)), exit = fadeOut(tween(420)),
                ) { Bienvenida(e, rol) }
                // ¿En qué pantalla lo abro?: la ventana va donde está el estante (abajo); arriba, un marco si se abrirá ahí.
                if (e.donde != null) {
                    val fisicaArriba = miPantalla == android.view.Display.DEFAULT_DISPLAY
                    if (rol != Rol.UNICA) MarcoDonde(if (fisicaArriba) e.dondeSel == 0 else e.dondeSel == 1)
                    if (rol == Rol.UNICA || rol == Rol.INFERIOR) VentanaDonde(e)
                }
                // ¿Dónde quedaste? y el mapa: en la pantalla del estante (la de abajo, o la única).
                val delEstante = rol == Rol.UNICA || rol == Rol.INFERIOR
                if (delEstante && e.quedaste && e.marcandoMapa == null) TarjetaQuedaste(e)
                if (delEstante && e.marcandoMapa != null) MarcarMapa(e)
                // Tour de novedades (una vez por versión): en la pantalla principal; la otra acompaña.
                AnimatedVisibility(
                    visible = e.novedades >= 0 && !e.intro,
                    enter = fadeIn(tween(300)) + scaleIn(tween(360), initialScale = 0.95f), exit = fadeOut(tween(260)),
                ) { if (rol == Rol.INFERIOR) NovedadesCompania(e) else TourNovedades(e) }
                val sc = e.sesion
                AnimatedVisibility(
                    visible = e.continuacion > 0L && sc != null && !sc.dosPantallas && miPantalla != sc.displayId && e.datos[sc.clave]?.lupa != true,
                    enter = fadeIn(tween(300)) + scaleIn(tween(360), initialScale = 0.94f), exit = fadeOut(tween(400)),
                ) { Continuacion(e) }
                // Ventana de pregunta (reporte de fallas, desinstalar…): en la pantalla del estante, encima de todo.
                if ((if (tienda) !arribaFisica || rol == Rol.UNICA else delEstante) && e.dialogo != null) VentanaDialogo(e)
                // En modo una pantalla, la de abajo no muestra la animación.
                val sinIntro = rol == Rol.INFERIOR && rolIntro == Rol.INFERIOR && e.unaPantalla
                if (e.intro && !sinIntro) Intro(rolIntro)
            }
            if (barra && !(e.relojGrande && rol != Rol.INFERIOR)) BarraSistema(e, Modifier.align(Alignment.TopCenter),
                estilo = if (miPantalla == android.view.Display.DEFAULT_DISPLAY) e.extra.estiloBarraArriba else e.extra.estiloBarraAbajo)
            // Abriendo un juego: la pantalla de carga tapa todo (también la barra) en las dos pantallas.
            if (e.extra.carga != null && !(rol == Rol.INFERIOR && e.unaPantalla)) PantallaCarga(e, miPantalla, rol == Rol.UNICA)
        }
    }
}

/**
 * Gestos de los bordes (sin quitarle los toques a lo de abajo): hacia abajo desde el borde de arriba, por la
 * izquierda las notificaciones y por la derecha los ajustes rápidos; hacia arriba desde el borde de abajo, la
 * barra flotante del inicio.
 */
private fun Modifier.cortinaDesdeArriba(puede: () -> Boolean): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val inicio = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (!puede()) return@awaitEachGesture
            val borde = 44.dp.toPx()
            val arriba = inicio.position.y <= borde
            // Desde el borde de abajo, hacia arriba: vuelve la barra flotante del inicio.
            val abajo = inicio.position.y >= size.height - borde
            if (!arriba && !abajo) return@awaitEachGesture
            val izquierda = inicio.position.x < size.width / 2f
            while (true) {
                val ev = awaitPointerEvent(PointerEventPass.Initial)
                val c = ev.changes.firstOrNull { it.id == inicio.id } ?: break
                if (!c.pressed) break
                val dy = c.position.y - inicio.position.y
                if (arriba && dy > 56.dp.toPx()) {
                    c.consume()
                    cl.nikalbazu.diptico.estado.Diptico.abrirCentro(if (izquierda) "avisos" else "ajustes")
                    break
                }
                if (abajo && dy < -56.dp.toPx()) {
                    c.consume()
                    cl.nikalbazu.diptico.estado.Diptico.mostrarBarra()
                    break
                }
            }
        }
    },
)



# Memoria del proyecto Díptico (para Claude)

Léeme al empezar una sesión nueva: con esto no hace falta releer conversaciones viejas. Reglas para todos los agentes en [AGENTS.md](AGENTS.md).

## Quién y cómo

- Dueño: Nicolás (Nikalbazu24), psicólogo escolar en Chile, no programa. Hablarle en español de Chile, cercano («hno»), sin jerga.
- Claude = arquitecto: decide, reparte tareas (issues con etiqueta `codex` o `gemini`), revisa PRs y capturas, entrega APKs.
- Codex hizo 31 cambios entre la 1.0 beta.8 (`bd95fe5`) y la v1.1-beta.178 (`68dd7f5`, 7 nov 2026).
- Bitácora (Claude Docs): https://claude.ai/artifact/MQBZXqQ8q4CcddJK4j1hBy · proyecto `a5336366-f22f-4e03-b553-91a7da20789c`,
  nodo `08c6a6b4-34e5`, enum Estado `de610ba8-1c71` (0 Pendiente, 1 En curso, 2 Hecho). Tabla de ideas `m4m0edz7a5a.1683`.

## Repositorio y publicación

- `Nikalbazu24/diptico-frontend` (público). Ramas `fix/**` compilan; `main` publica `v1.1-beta.<run>` firmada (dos APK).
- Cambios solo de `*.md`/`docs/` no compilan. El ID de `Novedades.kt` debe cambiar o la publicación falla.
- Las capturas (Roborazzi, `Capturas.kt`) quedan solo como artefacto (no descargable desde la sesión). Para verlas:
  empujar a `fix/revision-capturas` (sube el zip al borrador `revision-capturas`) y bajarlo con
  `gh api -H "Accept: application/octet-stream" repos/Nikalbazu24/diptico-frontend/releases/assets/<id>`.
- Sin Android SDK local ni acceso a maven/dl.google: no se compila en la sesión, solo en GitHub Actions.
- Commits terminan con las líneas `Co-Authored-By` / `Claude-Session` que indique el sistema.

## Estado al 8 nov 2026 (revisión de Claude)

Funciones nuevas de Codex: Liquid Glass (`ui/Cristal.kt`, `Desenfoque.kt`, `FondoVentanaCristal.kt`), tienda de 30 temas,
editor de widgets adaptable, Centro de Bisa, atajos Start+R3/L3, retoma con clip (`retoma/`), reportes a Drive
(`Fallas.kt` + `tools/reportes-drive.gs`, falta configurar `DIPTICO_REPORTES_URL`), respaldo validado, credenciales aparte,
importar gamelist de ES-DE, copias manuales de partidas, vistas Mosaico/Vitrina, selector de vistas con R3.

Pendientes encontrados (issues abiertos para Codex/Gemini):
- Caídas: `RetomaServicio` no llama `startForeground` antes de salir; «Ahora no» en el aviso de caída no borra el informe;
  `Teclas` deja teclas «presionadas» al perder el foco; permiso `REQUEST_DELETE_PACKAGES` sobra.
- Rendimiento Glass: cálculo sin `remember`, posición por cuadro, desenfoque en el hilo principal, portadas sin `HARDWARE`.
- Respaldo no lleva copias de partida ni música de temas instalados.
- `grupoOpcion` agrupa por texto del título (se rompe al renombrar; Glass cae bajo «Íconos de las consolas»).
- Íconos de carpetas usan motivos de franquicias (pokébolas, estrella de Mario, omega de God of War): reemplazar por originales.
- ~1.090 textos sin traducir; ~40 opciones sin `ayuda`; pistas de botones distintas entre pantallas.
- Menú recargado: Apariencia ~27 opciones, 8 ajustes que se pisan (Glass ×3, Efecto «vidrio», Fluidez, Movimiento…).
- Barra de accesos: quedó fija (`barraFija = true`), código de «se esconde sola» muerto. Decisión de Nicolás pendiente.
- `Diptico.kt` creció a 8.719 líneas.

## Preferencias de Nicolás

- Cada versión nueva va con un **reporte de cambios detallado** (qué se agregó, corrigió, cambió y quitó, y qué probar en la Thor).
- Sus listas de mejoras y errores se ordenan en la bitácora por versiones antes de repartirlas como issues.
- **Antes de cada instrucción, recomendarle qué modelo usar** (él lo cambia; Claude no puede): Opus para diseño,
  errores difíciles, rendimiento y versiones grandes; Sonnet para cambios acotados, textos, traducciones y bitácora;
  Haiku para preguntas rápidas. Decirlo en una línea al comienzo y esperar su visto bueno si conviene cambiar.

## Plan acordado (8 nov 2026, detalle en la bitácora)

- Errores primero: #29 juego se reinicia al reabrir la consola, #30 herramientas no pasan a la pantalla libre con pantallas
  intercambiadas, más #23–#28.
- v1.2 Orden y base: ajustes con menús desplegables, barra difusa/transparente/sólida por pantalla, accesos de la cortina
  editables, vibración con niveles, limpieza (dividir `Diptico.kt`).
- v1.3 Movimiento y vistas: transiciones con intensidad/velocidad (Nicolás debe describir los videos de X: no se abren aquí),
  cuadrícula solo íconos, cartuchos 3D genéricos, galería de artes y capturas, dock + cajón de apps, estilos de carpetas.
- v1.4 Mientras juegas: pantalla de abajo minimalista con menú desplegable, temperatura y consejos (no se puede cambiar el
  ventilador sin permisos de sistema), por qué falló un juego, respaldo de partidas, «Tu año» en diciembre.
- v1.5 DIPO (Bisa renombrada, personaje original, 3 propuestas), ventana de logros en la barra, más widgets.
- v1.6 Conectado: Discord (investigar kit social), descargas ordenadas solas, importar frontends, biblioteca inteligente,
  HowLongToBeat, repaso una pantalla/celular/TV, votación de la comunidad.
- Regla transversal: todo funciona con una pantalla, mando y dedo; lo vistoso se puede apagar.

## Hecho el 8 nov 2026 (v1.1-beta.182, por Claude: Codex sin cupo hasta ~12 nov)

- #23–#27, #29, #30 cerrados. Solo queda #28 (textos, para Gemini/Jules) y la decisión de la barra.
- Partida: `jugarComprobado` trae al frente si hay sesión del mismo juego (`volverALaPartida`) y relanza sin
  CLEAR_TASK/CLEAR_TOP si se cerró hace < 6 h (`ultimaPartida`). Pendiente que Nicolás confirme en la Thor.
- `Diptico.intercambioEfectivo(e)`: durante una partida, la pantalla libre muestra el estante con las herramientas.
- REQUEST_DELETE_PACKAGES se mantiene a propósito (ACTION_DELETE lo exige con targetSdk ≥ 28).
- Las portadas no usan HARDWARE con Glass a propósito: la captura del fondo dibuja en un lienzo de software.

## v1.1-beta.185 (8 nov 2026, «Orden y base», por Claude)

- `Estado` llegó al límite de 255 datos de Java: todo lo nuevo va en `EstadoExtra` (`e.extra`), con propiedades
  de extensión para leerlo (`e.desplegable`, `e.vibracion`…; en `ui/` usar `e.extra.x`).
- Menús desplegables: `Opcion.alternativas/elegida/elegir`, `Diptico.opcionLista(...)` y `Opcion.conLista(...)`.
- Barra de accesos fija u oculta (`barraFija`, `mostrarBarra` con 5 s), estilos de barra por pantalla
  (`fondoBarra`), accesos de la cortina (`accesosCentro`, galería `accesos_cortina`), `audio/Vibracion.kt`.
- Reportes: el receptor de Apps Script de Nicolás va por defecto en `app/build.gradle.kts` (el proxy no deja
  crear la variable de GitHub). Carpeta Drive «Díptico · Reportes de fallas» (id 1QkbVkAUc6Pg5eyE92BQ9usu33BoJ2gcJ).
- Pendiente v1.2: reordenar el menú (menos secciones, sin repetidos) y la limpieza de `Diptico.kt`.
- v1.3: referencia de Glass/transiciones = video de Nicolás (lente con refracción y dispersión, luz que sigue
  la selección, cambio de ambiente día/noche suave, interruptores-escena). Usar AGSL (Android 13) con respaldo.

## v1.3 «Movimiento y vistas» (8 nov 2026, por Claude, commit 0806726 en main)

- `ui/Movimiento.kt` (AjusteMovimiento, entradaVentana/Flotante/Carpeta), `ui/LenteVidrio.kt` (AGSL, `nuevo()` puede ser null),
  `ui/Interruptores.kt` (líquido y día/noche), paleta que se funde en `Tema.kt` (`paletaSuave`), `ui/DockApps.kt`,
  vistas `iconos` y `cartuchos`, galería `artes`, barra flotante (`extra.formaBarra`, `extra.coloresBarra`, `barraOcupaEspacio`).
- Rama de capturas: el commit a cherry-pickear ahora es `2718878` (no `ea478b4`). Traducciones siguientes: `extras15.tsv`.
- Falta de la 1.3: estilos de carpetas. Luego v1.2 pendiente: reordenar menú y dividir `Diptico.kt`.

## Errores de la beta.195 reportados por Nicolás (8 nov, 22:30) — hacer primero la próxima sesión

- La isla de la barra flotante se monta sobre los botones de la barra de accesos del inicio (arriba a la derecha) y
  en la pantalla de abajo pasa lo mismo: reservar espacio o mover los accesos cuando la barra es flotante.
- Todo se siente muy lento (sospechas: `paletaSuave` recompone todo, el lente AGSL por superficie, el escalonado del menú).
- La app se cierra al cambiar la vista de los juegos (R3 / galería `vistas_juegos`): revisar la vista `cartuchos`/`iconos`
  y la vista previa `MiniPantalla` con `PantallaInferior` (DockApps dentro de la previa). Pedir el reporte de Drive.

## Beta.197 (8 nov, 22:50): arreglos rápidos y lo que pidió Nicolás para la próxima sesión

- Hecho: la isla de arriba va centrada; abajo la barra reserva su espacio; lente apagado por defecto (`lente=false`) y se salta
  en lienzos sin aceleración (`isHardwareAccelerated`); se quitó el uso de `paletaSuave` en `Lienzo`.
- Pedido (23:16), en este orden:
  1. **Sigue muy lenta.** Medir antes de tocar: escalonado de filas del menú (`Animatable` por fila), `superficieCristal`,
     `rememberRed`/`PilaBateria`/`collectAsState` de avisos en cada barra, el dock y las vistas nuevas. Considerar volver
     a `formaBarra = "completa"` por defecto.
  2. **La barra flotante no combina:** en Ajustes se ve encima y queda mal; abajo «se ve horrible». Rediseñarla integrada
     (abajo quizá dentro de la fila de consolas o como pie fino), que combine con el tema y no ocupe tanta pantalla.
  3. **Carpetas:** al marcar una no se nota cuál está elegida; falta un brillo o halo claro en la elegida.
  4. **Carpetas con efecto Glass** como el resto de la app (`CarpetaBiblioteca`/`CarpetaVisual` en `ui/VistasConsolas.kt`).

## NUEVO RUMBO (9 nov 2026, 00:14) — manda sobre el plan por versiones

- Nicolás no está conforme con el rumbo. Desde ahora Claude decide como programador experto; él da ideas y aprueba diseños.
- Decisiones: no rehacer de cero (el motor sirve); rehacer la interfaz sobre la base actual. Congelar funciones nuevas
  (v1.4–1.6 y DIPO quedan guardadas). Un solo estilo (arte del juego protagonista + un vidrio oscuro sobrio), Apariencia de
  ~27 a ~8 opciones, velocidad como regla (efectos caros apagados), una pantalla a la vez, con boceto aprobado ANTES de programar,
  y partir `Diptico.kt` mientras se rehace cada pantalla.
- Referencias que le encantan: iiSU (dos pantallas como una sola consola), Cocoon (orden, acento por consola),
  ES-DE (elegir sistema con logo grande y luego sus juegos), PS5 (arte de fondo, fila de juegos, info + Jugar grande).
- Propuesta acordada: arriba la vitrina (arte a pantalla completa con degradado, logo/nombre, consola, tiempo, Continuar;
  hora y batería chicas integradas); abajo el control (fila de logos de consolas, fila de juegos tipo PS5 con borde de luz,
  franja de accesos Buscar/Apps/Ajustes); una pantalla = vitrina de fondo + controles abajo; Ajustes en ~6 secciones tipo PS5.
- PRÓXIMO PASO cuando Nicolás avise: boceto visual (Design artifact) de arriba, abajo y ajustes; afinarlo con él y recién ahí programar.
- Boceto (9 nov): https://claude.ai/artifact/42GUSEMnxQhs41gtjLiPAw (Arriba, Abajo, Ajustes en 6 secciones, Una pantalla). Esperando opinión de Nicolás.
- Paso 1 hecho (9 nov): vista `mosaicos` (ui/Mosaicos.kt, `Teclas.pasoMosaico`) por defecto; barra `completa` por defecto.
  Novedades: las versiones van en orden cronológico (la nueva AL FINAL de la lista). Siguiente: franja de abajo con
  Buscar/Apps/Ajustes + hora/batería y fila de consolas con halo; luego vitrina de arriba; luego ajustes en 6 secciones.
- Paso 2 hecho (9 nov): `FranjaAccesos` abajo (Buscar/Apps/Ajustes + hora/batería; la barra de la hora no se dibuja abajo con mosaicos), consola elegida con halo en `Cinta`. Siguiente: vitrina de arriba.
- Paso 3 hecho (9 nov): con dos pantallas y mosaicos, arriba no se dibujan `BarraSistema` ni `BarraFlotante`
  (`nuevoDisenoDosPantallas`); `BotonesVitrina` (A Jugar/Continuar, X Ficha). Ajustes en 6 secciones `G_*`
  (`GRUPOS_AJUSTES`, `armarOpciones` las arma juntando las de antes con su título como grupo). Liquid Glass apagado
  (`Estilo.glass = false`), sin opciones Glass ni lente (`LenteVidrio.kt` borrado). Pendiente: recortar opciones de
  «Pantalla y estilo» (aún larga) y seguir partiendo `Diptico.kt`.
- Paso 4 hecho (9 nov): `Opcion.muestrasIconos` (desplegable de íconos con muestra), `G_WIDGETS` («Inicio y widgets» = INICIO),
  importar/aportar temas en la cabecera de `tienda_temas` (`importarTemaArchivo`), plantillas borradas, clip de retoma
  apagado para todos (clave `clipRetoma2`). PENDIENTE PEDIDO: reinventar Bisa como **DIPPO** (doble p): boceto con 3
  propuestas de personaje original antes de programar. Luego: recortar Apariencia y partir `Diptico.kt`.
- Paso 5 (9 nov): `G_TEMAS` («Temas y decoraciones» = TEMAS + DECORACIONES); mosaicos con transición al cambiar de consola
  y resorte corto; menú sin animación por fila. DIPPO: 6 propuestas en el boceto (Alas, Chispa, Cartucho, Pixel, Cartucho
  con gorro, Disco); el cartucho gustó más pero ninguna lo convenció del todo. Pedirle referencias de estilo antes de otra ronda.
  Ronda 3 (12 ideas): le gustaron Pulpito, Fantasmita y Chispa. Ronda 4 en el boceto (Dippo4): A Pulpito fantasma, B Pulpito de luz, C Fantasmita de luz.
  ELEGIDO (9 nov 09:34): **Pulpito de luz** (naranjo cálido de día, azulado de noche; visor oscuro con ojos de luz celeste; puntas de tentáculos = lucecitas). Ficha en el boceto (Dippo5): pose saludando, caras pensando/celebrando/sorprendido/preocupado/durmiendo; personalidad curioso, servicial, algo despistado.
- DIPPO hecho (9 nov, beta tras la 217): `ui/Dippo.kt` (`dibujarDippo`, `CaraDippo`, `DippoDia`/`DippoNoche`), `AvatarBisa(cara=)`
  con luces que respiran y saludo cada ~9 s; `dibujarBisa` (ícono) delega en el pulpito. «Bisa»→«DIPPO» en textos y TSV
  (Novedades viejas sin tocar); accesos de la cortina migran "Bisa"→"DIPPO". Pendiente DIPPO: caras según contexto
  (pensando mientras responde, preocupado con batería baja, celebrando con logros, durmiendo de noche sin uso),
  su widget propio con menú de acciones y su lugar en la pantalla de abajo mientras juegas.
- DIPPO en casa (9 nov, beta.224): `ui/DippoInicio.kt` (`DippoInicio(e, tam)` con globito de saludo según la hora y caras
  por contexto: feliz al saludar, preocupado con batería ≤15 %, dormido de 1 a 5 AM). Va en `FranjaAccesos` (abajo, junto
  a la hora) y en `PantallaUnica` (esquina de arriba a la derecha). Se asoma en la `Intro` (`DippoIntro`, DURACION 3800).
  Sección propia `G_DIPPO` en Ajustes (antes ASISTENTE estaba en Sistema): nuevas `extra.dippoInicio`, `dippoSaludo`,
  `dippoColor` ("hora"/"dia"/"noche", `coloresDippoAhora(pref)`). Capturas t287–t290. Novedades `dippo-casa-2.0-23`.
  Pendiente: DIPPO pensando mientras responde, celebrando con logros, su menú de acciones y su lugar mientras juegas;
  los mosaicos se ven apretados en una pantalla (captura 288).
- DIPPO listo + teclado Gboard (9 nov, beta tras la 224, rama `fix/dippo-listo`):
  DIPPO habla primero al abrirlo (`CentroBisa.saludoInicial`, si el chat está vacío o pasaron 20 min) con botones;
  `CentroBisa.preguntasListas` (chips de un toque según contexto) + `ideaInicio` (globito Coral en el inicio que se
  toca para hacer la idea); la IA termina con botones `[[preguntar:…|…]]`; prompt de nube con personalidad del pulpito
  y «no le pidas que escriba»; `audio/VozDippo.kt` (TTS) responde en voz alta si la pregunta fue por voz (`extra.dippoVoz`).
  Teclado (`ui/Teclado.kt` + `estado/Palabras.kt`): fila 0 = franja (⋯ / sugerencias «»palabra» / Voz) o herramientas
  (`extra.tecladoEdicion`); ⇧ doble toque = `extra.mayusFija`; mantener letra = tilde/número (`Palabras.variante`), Y en
  mando hace lo mismo; mantener ⌫ = borrar palabra; doble espacio = punto. Mosaicos en una fila si el alto < 200 dp
  (`Teclas.mosaicosUnaFila`). Pendiente: DIPPO celebrando con logros (no hay evento de logro nuevo), su lugar mientras juegas.
- Limpieza + logros DIPPO + HowLongToBeat (9 nov, rama `fix/logros-dippo`): vistas de juegos solo mosaicos, cuadricula,
  lista, carrusel (las viejas caen en Mosaicos; `Lomos.kt`, Abanico y Cartuchos borrados); consolas: arriba, carpetas,
  sistemas (`vistaConsolasCompatible` migra); barra flotante borrada (`formaBarra` fijo "completa"); Apariencia sin
  velocidad/efecto/zoom lento/«ver ahora»; estilos guardados (`opcionesPersonalizacion`) pasaron a TEMAS.
  `estado/LogrosDippo.kt` (21 logros, contadores en almacén `dippoContadores`/`dippoDias`/`dippoLogros`,
  `Diptico.sumarDippo`, `revisarLogrosDippo`, `vigilarLogrosDippo` cada 30 s; celebra con `extra.dippoCelebra`).
  Galería `logros_dippo`. `estado/CuantoDura.kt` (HLTB como howlongtobeatpy: ruta del JS + /init + POST; respaldo DuckDuckGo),
  `extra.duraciones` (almacén `duraciones`), `Diptico.pedirDuracion`, `LineaDuracion` en la vitrina, chip `bisa_dura`.
  Siguiente: ficha del juego estilo PS5 (boceto primero), pantalla mientras juegas con DIPPO, partir `Diptico.kt`.
- Boceto (9 nov, 13:30, versión 14 del Design): tableros `Ficha.dc.html` (arriba: arte grande, chips de tiempo/logros, barra
  de historia con HLTB, próximo paso, A Continuar + Y Preguntar a DIPPO; abajo: tarjetas tipo PS5 Tu progreso/Logros/De qué
  trata/Guías con detalle y opciones al pie) y `Jugando.dc.html` (pantalla libre sin pestañas: DIPPO con globito, próximo
  paso con barra HLTB, botones Pista/Mi paso/Captura/Más y «Terminé por hoy»; a los 5 s se oscurece con DIPPO durmiendo y
  el reloj). Esperando la opinión de Nicolás antes de programar.
- Deslizar + dock (9 nov, tarde, rama `fix/deslizar-dock`): `ui/InicioDeslizable.kt` (HorizontalPager portada↔widgets
  arriba con puntitos; `Diptico.fijarInicio`; la página de al lado no se compone hasta deslizar). Una pantalla: transición
  horizontal. Dock fijo `FilaDock` (máx. 5 = `MAX_DOCK`, `extra.dock` por defecto true, clave `dock2`) en `FranjaAccesos`
  (Buscar/Ajustes quedan solo ícono) y en la pantalla compañera mientras juegas; `abrirPaqueteAqui` abre la app en la pantalla
  tocada. Widget nuevo `progreso` (HLTB + racha + próximo logro DIPPO), `Widgets.inicioNuevo` + opción «Usar el inicio
  nuevo» (respaldo en perfilVisualAnterior), hora y batería en el encabezado de widgets con el diseño nuevo.
- Mientras juegas rediseñado (9 nov, rama `fix/jugando`): `EnJuego` en `ui/PantallaInferior.kt` sin pestañas ni logo con −/+:
  línea fina (consola, título, ⏱ sesión, hora), `ConsejoDippo` (consejo según reintentos/temporizador/90 min/sin paso),
  `TuPartida` (paso + barra HLTB), `MusicaEnJuego`, `FilaDock`, botones Pista/Mi paso/Guías/Más; `MasHerramientas`
  (lupa, mapa, aviso, cronómetro, ficha, DIPPO, apps, apagar pantalla, y «Marcar como terminada» discreto). Sin botón
  Terminé: la sesión se cierra sola en `Diptico.alVolver` (Díptico vuelve al frente en la pantalla del juego).
  OJO: el 9 nov GitHub dejó de lanzar el workflow con push en `fix/jugando`; se lanzó a mano con
  `gh api -X POST repos/Nikalbazu24/diptico-frontend/actions/workflows/402353305/dispatches -f ref=<rama>`.
- 9 nov 16:30 (rama `fix/ficha-carpetas`, SIN COMPILAR/REVISAR aún: se acabó el cupo): pager con red de seguridad
  (gesto largo cambia de página) y umbral 25 %; `LogoEnPartida` (logo tipo Cocoon) en `EnJuego`; acceso marcado arriba con
  aire y borde; `EstadoChico` (hora/batería/avisos) en el encabezado de Ajustes sin barra; carpetas con halo de color y
  borde blanco, desenfoque + oscurecido detrás de la carpeta abierta; `FranjaAccesos` también en carpetas/sistemas;
  ficha: arriba `VitrinaFicha` (capturas + fotos + `ComentarioDippo`), abajo la ficha; chat: `CarpetaJuegosChat` bajo el
  mensaje de DIPPO cuando propone juegos. Novedades `ficha-carpetas-2.0-28`. Falta: traducir textos nuevos (extras15),
  capturas, publicar. Pendientes que pidió: Discord (Social SDK ya tiene Android; investigar), revisar bitácora (desactualizada
  desde la beta.185: actualizarla con 204–245), «Tu año» antes de diciembre, temperatura, por qué falló un juego, respaldo
  de partidas, logros RA con aviso al ganarlos.
- Cajas y carga (9 nov, noche, rama `fix/cajas-carga`, Opus): `ui/CargaJuego.kt` (`PantallaCarga`: color de consola, caja,
  «INICIANDO», consola, título, barrita, fundido a negro; B cancela) — `jugarComprobado(conCarga)` muestra `extra.carga`
  (`CargaJuego`) y lanza a los `CARGA_MS` (1250); `terminarCarga` la quita 1,2 s después; `Teclas` traga teclas durante la carga.
  Ajuste `extra.cargaJuego` (clave `cargaJuego`). `ui/VitrinaCajas.kt` (`VitrinaCajas`, `CajaVitrina`): portada de arriba con
  diseño nuevo, `extra.portada` "cajas"/"arte" (clave `portadaArriba`), forma real de cada consola (`LocalFormas provides e.formas`).
  OJO: en Robolectric con zIndex + translationX en graphicsLayer las vecinas no salían; quedó con offset + orden por cercanía + key.
  El giro rotationY no se ve en las capturas (sí en el equipo). Capturas t305–t310. Novedades `cajas-carga-2.0-29`.
  Pendiente: la «caja que se abre» con librito (opcional, apagable) para la ficha; Nicolás eligió color y forma por consola.
- Estuches + Discord (10 nov, rama `fix/cajas-unicas`): `ui/Estuches.kt` (`Estuche`, `estucheDe`, `CapaEstuche` con
  drawWithCache; `CajaJuego(estuche = true)` en vitrina y carga). Vitrina lee `pos` solo al dibujar (offset/graphicsLayer/
  drawBehind), fondo con arte de 72 px sin blur en vivo. Menú con pantallas intercambiadas: fondo opaco a pantalla completa
  (`Paneles`). Dock replegable `extra.dockVisible` (`BotonRepliegue`, `Diptico.alternarDockVisible`). En partida: logo
  primero y grande, `TuPartida` compacta. Discord por webhook (`estado/Discord.kt`, `extra.discordWebhook/Empezar/Terminar`,
  `avisarDiscord` al lanzar y en `cerrarSesion`, opciones en Seccion.LOGROS grupo «Discord», texto `discord_webhook` con QR).
  Rich Presence real necesita Discord Social SDK (no descargable desde la sesión). Novedades `estuches-discord-2.0-30`. Capturas t311–t317.
- Cajas 3D + cuentas (10 nov, rama `fix/cajas-3d`): estuche ALREDEDOR del arte (`MargenesEstuche`/`margenesDe`, la caja crece
  y el arte va en el hueco). Vitrina: la elegida flota (`ola`), se gira con el dedo (`giroY`/`giroX`, resorte al soltar),
  lomo que se asoma (2,5D) y reflejo; el respaldo del pager ignora arrastres consumidos. Decoraciones: ya no se apagan con
  el color de la consola (`aplicarTema`) ni al jugar (`aplicarFluidez`). Sección `G_CUENTAS` («Cuentas y conexiones» =
  Seccion.LOGROS: Discord + RA + SteamGridDB + raspadores). Discord con login por QR (flujo device de OAuth2:
  `DiscordCuenta`, `extra.discordLogin` → `PanelDiscord` en Raiz; tokens en credenciales). FALTA el Application ID:
  Nicolás debe crear la app en discord.com/developers (OAuth2 › Public Client ON) y pasar el ID → `DIPTICO_DISCORD_APP`
  en build.gradle (por defecto vacío). Presencia «Jugando a…» sigue pendiente del Social SDK. Novedades `cajas-3d-2.0-31`.

## Cajas reales + rapidez (10 nov, rama `fix/cajas-reales`, Opus)
- Nicolás: las cajas 3D de la beta.274 «feísimas»; quería cajas reales girables completas que al abrirse lleven a la carga y al juego.
- `ui/Caja3D.kt`: caja con perspectiva real en un Canvas (8 esquinas proyectadas, `Matrix.setPolyToPoly` por cara, luz por normal,
  sombra en el piso). Caras: frente = portada (si no, `FabricaCaras` arma una), reverso y lomo reales (`extra.caras`,
  `CarasGuardadas`, `Diptico.pedirCaras` con `Raspadores.caras`: ScreenScraper box-2D-back/side, TheGamesDB back) o armados
  con ficha guardada (`Fichas.guardada`), tu tiempo y HLTB. `grosorDe(estuche)` da el grosor real.
- Vitrina: giro libre en Y (dedo, stick derecho vía `GiroCaja` desde `Teclas.movimiento` si `vitrinaActiva`, botón «Girar»),
  se acomoda a frente/reverso al soltar; vecinas a 50° mostrando el lomo. `vitrinaGiroPrueba` para capturas t321–t325.
  `CargaJuego` usa la misma `Caja3D`.
- Discord: `PanelClave` tenía `discord_app`/`discord_webhook` cayendo en el texto de SteamGridDB (era el «me tira a steamgriddb»).
  App ID por defecto 1569677228269117580 en build.gradle; la opción «App de Díptico» solo aparece si falta o es propia.
  OJO: la guía de consolas de Discord sugiere Public Client apagado + client secret; probar si el device flow funciona sin secreto.
- Rendimiento: `desenfoqueAnimado {}` (Componentes.kt) en Raiz y VentanaCarpeta, `zoomLento` devuelve State, logo en partida
  lee el flotar al dibujar, `Estilo.decorQuieta` (decoraciones quietas con sesión). Pendiente: `Estado` entero pasa a todo
  (cada `actualizar` recompone mucho); medir en la Thor antes de partir eso.
- Novedades `cajas-reales-2.0-32`.
- Publicado como **v1.1-beta.285** (main a93292f). Además: franja de la consola arriba del frente (`franjaDe`, `InfoFrente`,
  `FabricaCaras.hacerFrente`; logo = `consolasPropias[id].logo` o el nombre), apagable con `extra.bandaCaja` (clave
  `bandaCaja`); fondo de la vitrina = portada del elegido a 40 px estirada. El «video de anoche» de Nicolás NO está en esta
  sesión (no se puede ver): pedirle que lo vuelva a adjuntar o lo describa para afinar fluidez y estilo.

## Beta.288 (10 nov, rama `fix/cajas-exactas`)
- Discord: el flujo de dispositivos exige Client ID + **Client Secret** con Basic auth (docs de consolas). La clave la pega
  Nicolás en Ajustes (`discord_secreto`, almacén credencial `discordSecreto`); `PERMISOS = "identify"`. Sin el secreto daba invalid_client.
- Cajas exactas: `Raspadores.caras` trae `frente` (TGDB front / SS box-2D) con filtro de plataforma (`PLATAFORMA_TGDB`);
  `CarasGuardadas.frente`, almacén `carasCaja2`. Con frente exacto no se agrega franja. DS/3DS: franja lateral (`franjaLateral`).
- Fondo de la vitrina: `Imagenes.desenfocada` (240 px + 3 pasadas de desenfoque de caja) + `rememberDesenfocada`.
- Con vitrina de cajas, `Mosaicos` va en una fila (`estante`), mismo orden que arriba (+1 por paso).
- `VentanaJuego` (opciones del juego): todo dentro del LazyColumn; `PanelFicha`: cabecera se esconde con indice > 0.
- Novedades `cajas-exactas-2.0-33`. Capturas t327–t330.

## Discord con el Social SDK (10 nov, rama `fix/discord-amigos`, publicado)
- La vía HTTP (device flow) falla aun con Client Secret («No pude conectar» = 401 sin desafío en Android) y no da amigos.
  Decisión: usar el **Social SDK** (`discord_partner_sdk.aar`, C++ por NDK/Prefab, licencia NO permite subirlo al repo
  público → guardarlo como asset de un release borrador y que el CI lo baje). Auth con Public Client sin secreto,
  amigos con `GetRelationships`, presencia «Jugando a…».
- Hecho en la rama: `estado/Social.kt` (`SocialDiscord`, `AmigoDiscord`), `ui/Amigos.kt` (`MarcaDiscord` dibujo propio,
  `BotonDiscord` en `EstadoChico` y `FranjaAccesos`, `PanelAmigos` desde el costado derecho, `extra.amigosEn`),
  Teclas (B cierra). Capturas t331–t332. Kit recibido de Nicolás el 10 nov.
- Kit integrado (10 nov, commit 3298dc9): `app/src/main/cpp/puente_discord.cpp` (JNI, textos como bytes UTF-8, todo en el
  hilo «discord» con `RunCallbacks` cada 200 ms) + `PuenteDiscord`/`SocialDiscord` en `estado/Social.kt` (`conexion`,
  `conectarAqui` = `Client::Authorize`, `usarToken`, `renovar`, `jugando`/`sinJuego`). `BaseActivity.onResume` →
  `SocialDiscord.alFrente` (setEngineActivity por reflexión; la 1.ª vez `Diptico.reconectarDiscord`). QR por HTTP sin
  secreto (Public Client) con permisos del kit (`PERMISOS_KIT`); su token pasa al kit. `extra.discordPresencia`.
  Solo se ve la actividad de amigos en Díptico (el kit no muestra otros juegos). Novedades `discord-amigos-2.0-34`, captura t333.
- El .aar NO va al repo (ni cifrado: lo bloquea la sesión). CI lo baja del borrador privado `kit-discord`
  (asset `discord_partner_sdk.aar`, job `apk` con contents: write); sin borrador compila sin kit (`BuildConfig.DISCORD_KIT`).
  Nicolás ya creó el borrador (11:24). Portal: activar Public Client + Redirect `discord-1569677228269117580:/authorize/callback`.


## Perfil de usuario (10 nov, rama `fix/perfil`, Sonnet)
- Pedido de Nicolás: perfil con foto y datos, acceso en una esquina y en Ajustes, fondo difuminado, personajes propios más finos y
  zoom desde el ícono. Boceto aprobado: https://claude.ai/artifact/AuoPvX9VZhuDYFPcMwcwao (versión 4). Un solo perfil; con fecha de nacimiento.
- `estado/Perfil.kt` (`Perfil` en JSON, clave `perfil`; `Perfil.fecha()` valida «12/03/1990»; `PERSONAJES` = 8 originales; `ResumenPerfil`).
  `EstadoExtra.perfil/perfilEn/perfilOx/perfilOy`. `Diptico.abrirPerfil(pantalla, ox, oy)`, `cerrarPerfil`, `editarPerfil` (→ Ajustes › Mi perfil),
  `cambiarPerfil{}`, `resumenPerfil(e)`, `opcionesPerfil`. Sección `G_PERFIL` (primera de Ajustes) que junta `PERFIL`. El nombre sigue siendo `e.nombre`.
- `ui/Personajes.kt` (`dibujarPersonaje(id)` en Canvas: Misu, Robotín, Moquito, Dragoncito, Fantasmita, Caballerito, Zorrito, Pixelito;
  `FotoPerfil(perfil, tam, personaje=)`). `ui/Perfil.kt`: `BotonPerfil` (guarda su centro en la ventana), `PanelPerfil` (tarjeta que se
  escala desde ese punto con `TransformOrigin`; Raiz desenfoca el fondo con `encima`), `PreviaPerfil`. Botón en `EstadoChico` y `FranjaAccesos`.
- Teclas: con la tarjeta abierta, B/Atrás cierra y A abre Ajustes › Mi perfil. DIPPO felicita el día del cumpleaños (`CentroBisa.saludoInicial`).
- Foto: `pedirImagen("perfilFoto")`. Galería `personajes`. Novedades `perfil-2.0-35` (la de Discord pasó a id literal). Capturas t334–t340.
- Pendiente: Start+A para abrir el perfil con el mando; el botón también en el encabezado del inicio con widgets; estadísticas «esta semana».

## Marcos, Discord y DIPPO arriba (10 nov, rama `fix/perfil-marcos`, Sonnet)
- Beta.299/301 ya publicadas (perfil + Start+A + botón en el encabezado de widgets).
- `Perfil.fotoDiscord` (usa `extra.discordAvatar`) y `Perfil.marco`; `MARCOS`/`MarcoPerfil` (en `estado/Perfil.kt`): estela y doble
  libres; chispas(hola), corazon(corazon), llama(racha3), noche(buho), aurora(explorador), arcoiris(trotamundos), oro(cien_h) con logro
  de DIPPO (`Diptico.marcoDesbloqueado`, `idsLogrosDippo`). Galería `marcos` con candado. `ui/Marcos.kt` (`dibujarMarco`, todo en el dibujo,
  `rememberInfiniteTransition` en `FotoPerfil`; quieto si `LocalMovimiento` es false).
- `BotonPerfil` ahora lleva la insignia de Discord (abre `PanelAmigos`, que tiene al pie «Mi perfil» y «Ajustes de Discord»); se quitó `BotonDiscord`
  de `EstadoChico` y `FranjaAccesos`. Ajustes de Discord viven en `Seccion.PERFIL` (ya no en LOGROS). Tamaños: franja 40, encabezados 32.
- Tarjeta de perfil: `LogrosDippoTile` y `LogrosRATile` (usa `e.perfilRA`).
- `ui/DippoBurbuja.kt`: globito de DIPPO sobre su cabeza en la capa de `Raiz` (`BurbujaDippoCapa`), se saca deslizando; `DippoInicio` solo publica
  el mensaje y la posición de su cabeza. `LocalPantallaId`/`pantallaActual()` en `Raiz.kt`.
- `FranjaAccesos`: grupo izquierdo con `horizontalScroll`, derecha fija. Novedades `marcos-dippo-2.0-37`. Capturas t342–t345.
- Pendiente (próxima semana): conexión completa de Discord (llamadas de voz/chat, Comms Access, puerta de edad con la fecha de nacimiento).

## Primera vez (10 nov, rama `fix/primera-vez`, Opus)
- `pasosTuto`: BIENVENIDA, PANTALLAS (solo con dos), CARPETA (al continuar baja portadas sola), EMULADORES (solo si faltan), APARIENCIA
  (noche/día, sin Cristal), LISTO. El enum `PasoTuto` sigue completo (lo usan capturas viejas).
- `Novedades.RECORRIDO` en 5 páginas al día. Novedades `primera-vez-2.0-38`.
- El cierre al terminar el tutorial = Android reinicia la app al darle el rol de inicio (no había reportes en Drive). `ofrecerInicio` ahora
  pregunta con `Dialogo("ser-inicio")` explicando el reinicio; guarda `pidioInicio` y `revisarPedidoInicio()` confirma al volver.
- Bienvenida al encender apagada por defecto (clave `bienvenidaModo2`, "no"): solo sale `Continuacion` si había partida en pausa.
- Tema de fechas apagado por defecto (clave `temaFestivo2`).
- `difuminar`/`ventanaEncima(e)` en `PantallaInferior.kt`: todo panel abierto (menos CENTRO/NINGUNO), amigos o perfil difuminan el estante;
  `Raiz.encima` suma `amigosEn` y `discordLogin`.

## DIPPO guía (10 nov, rama `fix/recorrido-dippo`, Opus)
- `ui/RecorridoDippo.kt`: con `e.recorrido`, `TourNovedades` dibuja a DIPPO grande (`AvatarBisa` con cara por paso) + globo con texto
  letra a letra (`TextoQueEscribeDippo`). Abajo `NovedadesCompania` dice «DIPPO te cuenta arriba». `RECORRIDO` = 6 páginas en su voz.
  `DippoInicio` no se dibuja con `novedades >= 0`.
- Trucos escondidos: `estado/DatosDippo.kt` (24), `Diptico.siguienteDatoDippo()` (almacén `dippoDato`, rota), fase 4 del globito en
  `DippoInicio` (a los 45 s y luego cada 4 min, 11 s visible, color Uva). Ajuste `extra.dippoDatos` (clave `dippoDatos`) en G_DIPPO.
- Novedades `dippo-guia-2.0-39`. Capturas t346–t347.
- Beta.317 publicó lo anterior. DIPPO vivo (rama `fix/dippo-vivo`): `poseDe(k)` (se corre/inclina hacia lo que explica), entrada desde el
  costado, salto por paso, vaivén, giro al tocarlo; `retoDe(k)` (paso 2: L1/R1; paso 3: tocarlo) → celebra y avanza solo.
  `estado/RecorridoVivo.kt` (Teclas avisa L1/R1/X/Y/L2/R2/sticks durante el recorrido; `fraseDe` explica cada botón). Novedades `dippo-vivo-2.0-40`.


## Temperatura y consejos (10 nov, rama `fix/temperatura`, Sonnet)
- `estado/Termica.kt`: `Termica.leer(ctx)` (temperatura de batería, `PowerManager.currentThermalStatus`, `getThermalHeadroom`, y CPU/GPU desde
  `/sys/class/thermal` si Android deja; todo sin permisos) → `Lectura` con `Nivel` FRESCA/TIBIA/CALIENTE/MUY_CALIENTE (manda el más alto);
  `consejoUrgente(l)` (calor, enchufada, batería ≤15 %) y `consejoDeTurno(n)` (5 consejos de cuidado, rotan). `pruebaLectura` fuerza una lectura en capturas/tests.
  No se puede cambiar el ventilador ni la potencia (permisos de sistema): solo informa y aconseja.
- `EnJuego` (`ui/PantallaInferior.kt`): lee cada 15 s en IO; chip de grados en la línea fina (verde/amarillo/naranjo/rojo, colores fijos);
  `ConsejoDippo` pone primero lo físico, luego lo de antes, y alterna con un consejo de cuidado cada ~40 s. Al pasar a «caliente» despierta la pantalla una vez.
- Ajuste `extra.termica` (clave `termica`, G_DIPPO › «Temperatura y consejos al jugar»). Novedades `temperatura-2.0-41`. Capturas t348–t349, test `TermicaTest`.
- Opus (misma rama): `Termica.registrar` guarda el pico por juego (almacén `termicaPicos`), `consejoAlEmpezar` (min < 3, pico ≥ 41 °C), `exigente(consolaId)` cambia el consejo de calor (bajar resolución interna), aviso en el inicio si ya viene caliente (`vigilarBateria`), opción «Sensores de temperatura» (`resumenSensores`) para ver qué deja leer la Thor.

## Tu año + logros en vivo (10 nov, rama `fix/tu-ano`, Sonnet)
- `estado/TuAno.kt`: historial de partidas (almacén `historialAnio`, líneas `fecha<TAB>clave<TAB>seg<TAB>partidas`, máx. 4000, se suma por día y juego;
  `TuAno.registrar` se llama en `cerrarSesion` con partidas de ≥15 s). `resumenDe(filas, anio)` (horas, partidas, días, racha máxima, top 5, meses, mejor mes,
  consola, desde cuándo) + `comentario(r)` de DIPPO + `tocaAvisar(mes, anio, yaAvisado)`. El historial parte en esta versión: lo anterior solo está en los totales de siempre.
  `pruebaFilas` fuerza datos en capturas.
- `ui/TuAno.kt` (`PanelTuAno`, tarjeta sobre el fondo difuminado), `extra.anioEn`, `Diptico.abrirTuAno/cerrarTuAno`. Se abre con Y desde la tarjeta de perfil, desde
  Ajustes › Tu perfil, y `vigilarTuAno` avisa una vez en diciembre. Teclas: B cierra. Raiz y `ventanaEncima` la cuentan como ventana (desenfoque).
- Logros en vivo: `revisarLogrosDippo` cuenta el tiempo y la partida en curso (antes solo se sumaban al cerrar la sesión); `EnJuego` muestra «Logro de DIPPO
  desbloqueado» 10 s con DIPPO celebrando y despierta la pantalla. Novedades `tu-ano-2.0-42`. Capturas t350–t351, test `TuAnoTest`.

## Biblioteca ordenada + perfil en la tarjeta (10 nov, v1.1-beta.341)
- Carpeta «Todos los juegos» apagada por defecto: `extra.carpetaTodos` (clave `carpetaTodos2`, opción en `opcionesEstante`); `pestanas`
  solo incluye "todo" si está activa (o si no hay consolas). `consolaDeCarpeta(e)` saca la selección de "todo" al volver a las carpetas.
  "todo" sigue existiendo como estado interno (saltos a un juego desde el inicio).
- Pistas de botones de `CarpetasConsolas` pasaron a `CabeceraCarpetas` (Biblioteca/N sistemas · pistas · Ordenar).
- Perfil: `extra.perfilEdita` (fila elegida); `editarPerfil` abre `EditorPerfil` dentro de `PanelPerfil`; `usarEdicionPerfil` cierra la
  tarjeta si la opción abre otro panel. Teclas: ↑↓ eligen, A/→ cambia, ← menos, B listo. Novedades `biblioteca-ordenada-2.0-45`.
- PENDIENTE (Opus): lentitud general (medir antes: `Estado` entero recompone todo); carpetas movibles a gusto y en widgets
  (colecciones) — boceto primero. Sin capturas revisadas de esta versión.
- v1.1-beta.343 (Opus): `opcionesSeccion` cachea por `e.copy(indice=0, enOpciones=false)` (antes se rearmaba todo el menú en cada paso)
  y `remember` de `ui/Menu.kt` igual; transiciones ×0,65 (`RAPIDEZ` en `ui/Movimiento.kt`). Novedades `mas-rapido-2.0-46`.
  Falta: rendimiento de la vitrina de cajas 3D (medir en la Thor) y carpetas movibles + carpeta como widget/colección (boceto primero).
- v1.1-beta.348: abajo, `CabeceraCarpetas` = `EsquinasArriba(e)` (DIPPO izq., hora/batería al centro, `BotonPerfil` der.; sin «Biblioteca»/«Ordenar»).
  `FranjaAccesos`: lupa izq., ajustes der., dock al centro en píldora `superficieApp(p.superficie)` + borde `p.linea`. Nicolás quiere
  DIPPO/perfil en la parte de arriba de la PANTALLA DE ABAJO (no en la barra de la pantalla superior). Novedades `esquinas-2.0-48`.
- beta.351/352: `FranjaAccesos` sin franja de color (degradado al `p.fondo`), `CirculoFranja` (lupa/ajustes) + píldora del dock, todo con
  `superficieApp(p.superficie)`. `EsquinasArriba`: DIPPO 52 a la izq.; a la der. foto · hora · batería. `extra.franjaAbajo` (clave `franjaAbajo`,
  opción «Barra de abajo» en `opcionesEstante`). Novedades `franja-ajuste-2.0-50`. Sin capturas revisadas.
- beta.35x «Barra con burbuja»: `IconoApp(desnudo = true)` (sin casilla/marco) en `FilaDock`. `FranjaAccesos` = Column: barrita (`dock-repliegue`,
  siempre visible, alterna `extra.dockVisible`) + `AnimatedVisibility` con entrada burbuja (spring + scaleIn desde abajo al centro) y salida hacia abajo;
  esconde TODA la barra (lupa, apps, ajustes). `BotonRepliegue` borrado. Novedades `franja-burbuja-2.0-51`. Sin capturas revisadas.

## Editar perfil abajo + tarjeta arriba (11 nov, rama `fix/editar-perfil`, Opus)
- Boceto: canvas 5041841b (`Editar.dc.html` abajo, `Main.dc.html` tarjeta), adaptado a horizontal (Nicolás lo aprobó).
- `EstadoExtra.edicionPerfil: EdicionPerfil?` (reemplaza `perfilEdita`): borrador con perfil, nombre, `presencia` (discordPresencia),
  `saludo` (dippoSaludo), `fila`/`col`. `FilasEdicion` (estado/Perfil.kt) da el orden: FOTO, PERSONAJES_1/2, NOMBRE, APODO, FECHA,
  CONSOLA, COLOR, PRESENCIA (solo con kit), SALUDO, HORAS, BOTONES.
- `Diptico.editarPerfil/cancelarEdicionPerfil/guardarEdicionPerfil/moverEdicionPerfil(df, dc)/usarEdicionPerfil(fila, col)`,
  `edicionDividida(e)` (= dos pantallas y sin partida), `moverPerfilA`. `cambiarPerfil` escribe en el borrador si hay edición
  (así la foto de la galería y el apodo/fecha del teclado van al borrador). Texto "nombre" con edición → borrador; `cerrarPanel`
  desde CLAVE vuelve al editor (panel NINGUNO).
- `ui/EditorPerfil.kt` (`EditorPerfilPantalla(e, rol, mi)` en Raiz tras `PanelPerfil(e, mi, rol)`): se ve con `panel == NINGUNO`;
  la tarjeta se aparta mientras se escribe. Tarjeta muestra el borrador y pastillas «Editando abajo · A Guardar · B Cancelar».
- Teclas: cruceta (al bajar, repite), A usa, Start guarda, B cancela. Foto «Mi inicial» = `personaje = INICIAL` (letra del nombre).
- Ajustes › Tu perfil: nueva primera opción «Editar mi perfil». Marco y título siguen solo en Ajustes (el boceto no los tiene).
- Del boceto NO se hizo «Mostrar mi perfil a mis amigos» (no hay nada detrás que lo use). Novedades `editar-perfil-2.0-52`. Capturas t352–t356.
- El empuje forzado a `fix/revision-capturas` lo bloquea la sesión: se fusiona la rama nueva encima (merge normal).

## Versión grande «Partidas a salvo» (11 nov, rama `fix/version-grande` → main, beta.375, Opus)
- Decisiones de Nicolás (11 nov): sí accesibilidad, medición, estilos de carpetas, respaldo de partidas; postergados Discord voz/chat,
  mapas web y compartir «Tu año»; NO perfil infantil; una versión = un tema grande; inicio NUEVO DESCARTADO (Claude decidió dejar el actual).
  Foto de perfil va en el inicio de ARRIBA (BotonPerfil en `BarraSuperior { }` de la portada con arte y de `VistaSistema`; la vitrina de cajas ya la tenía).
- Bocetos aprobados: https://claude.ai/artifact/AxJJLmHBzAeFiGTgnPngFD (carpetas modo edición + 4 estilos, ajustes condensados con demo arriba).
- `data/RespaldoPartidas.kt`: carpetas de partidas (SAF, `Carpeta.PARTIDAS`, `extra.carpetasPartidas`, clave `carpetasPartidas`),
  `respaldarPartida` en `cerrarSesion` (≥30 s): copia lo modificado desde el inicio de la partida (−1 min), 3 versiones por juego en
  filesDir/respaldo-partidas/<sha>/<fecha>/ con `.origen`; la más nueva va en el zip de Drive (≤48 MB, `paraRespaldo`). Restaurar desde
  opciones del juego › Copias de partida (escribe en el URI original con "wt").
- `ui/Medidor.kt` (`extra.medidorFps`, abajo a la derecha), alto contraste (`extra.altoContraste` → `Estilo.contraste` → `conContraste` en Lienzo).
- `estado/DipticoPerfil.kt` y `estado/DipticoCarpetas.kt`: funciones de Diptico como EXTENSIONES (`fun Diptico.x()`); fuera del paquete
  estado hay que importarlas (`import cl.nikalbazu.diptico.estado.*`). Seguir partiendo Diptico.kt así.
- Carpetas: `extra.ordenandoCarpetas/carpetaTomada/estiloCarpetas` (tarjeta/consola/abanico/logo, `ESTILOS_CARPETA`). Mantener A (repeatCount 1)
  o el dedo entra; A toma/suelta, cruceta mueve, X personaliza, B listo; arrastre con `detectDragGestures` en `CarpetasConsolas`.
- Ajustes condensados: `condensar(s, l)` en `opcionesSeccion` (secciones G_ con >12 opciones: 3 por grupo + fila «Más opciones de …» con
  `Opcion.sub`); `extra.subOpcion/subSel`, `VentanaSubOpciones` en Menu.kt, `Diptico.opcionEnFoco(e)` para la ayuda de arriba,
  `claveMenu(e)` como llave de caché. La búsqueda de ajustes ve todo.
- Novedades `partidas-a-salvo-2.0-53` (la 52 = editor de perfil, beta.362). Capturas t352–t365.

## Más fluido y flotante (11 nov, tarde, rama `fix/fluido-flotante`, Sonnet)
- `FranjaAccesos` (ui/Mosaicos.kt) sin píldora de fondo: `FilaDock(flotante = true)` da sombra a cada ícono, `CirculoFranja` es burbuja
  casi transparente con sombra y borde fino. Escondida = solo la barrita (`dock-repliegue`).
- Lentitud (sin poder medir en equipo, causas halladas leyendo): `VitrinaCajas` animaba las 7 `Caja3D` con `ola` (infinita) → ahora solo la
  caja elegida lee el balanceo; `Mosaico` ya no recibe `Estado` (juego, elegido, jugado) y no se recompone entero por cada cambio;
  el temblor de carpetas lee su `State` en `graphicsLayer`. Pedir a Nicolás las lecturas del Medidor de fluidez (inicio, carpetas, vitrina).
- `EsquinasArriba`: burbuja con el título (`tituloDe(e.extra.perfil.titulo)`) junto a la foto.
- Novedades `fluido-flotante-2.0-54` (la cadena de `NavegacionTest` ya incluye `partidas-a-salvo-2.0-53`).
- Pendiente Sonnet: «DIPPO aparece solo al hablar en Ajustes» (no quedó claro dónde sale hoy; preguntar a Nicolás), traducciones (#28),
  retoque de «Tu año» (`fix/tu-ano-retoque`). Temas propios/calendario = Opus.

## Switch 2 y ports nativos (11 nov, noche, rama `fix/switch2-ports`, Opus)
- APKs leídos (Nicolás los adjuntó): **NovaS 0.1.1** `io.github.nova5emu.novas` (derivado de Eden: `org.yuzu.yuzu_emu.activities.EmulationActivity`,
  TECH_DISCOVERED + content URI, Android 13+, 12 GB, Adreno; .nup sin llaves). **Banana Native 0.2.4** `dev.bananative.android` (nxnative: ports nativos de
  Switch 2; alias exportado `dev.bananative.android.Launch` → FrontendLaunch con extra `game` = ruta absoluta del .nup, o `title_id`; alias `.Open` VIEW .nup;
  pide «Acceso a todos los archivos»). Juegos con libnxlaunch: Mario Kart World 0400C3F00006E000, DK Bananza 040074A01BF12000, Splatoon Raiders
  0400380020056000, Pokopia 0400A3A01EE4C000, Zelda OoT S2 0400486024DEC000.
- `assets/sistemas.json` (editado a mano, OJO si se regenera con herramientas): reglas NOVAS y BANANA-NATIVE; switch2 ext +nup, órdenes NovaS y Banana primero;
  switch con NovaS al final.
- `estado/Nativos.kt`: catálogo (`JUEGOS_BANANA`, 30 `PORTS` con paquetes del pack Obtainium Recomp-Decomp), `recomendado(ctx, juego)` (Banana para esos
  5 si está instalada; va entre el emulador del juego y el de la consola en `jugarComprobado`/`volverALaPartida`), `equipo(ctx)`, extensiones
  `abrirNativos/cerrarNativos/pestanaNativos/moverNativos/usarNativo/entradasNativos`. Ports conocidos instalados entran solos a la carpeta Ports (`todosCrudos`).
- `ui/CentroNativos.kt` (`CentroNativos(e, rol)` en Raiz; `extra.nativosEn/nativosPestana/nativosSel`): dos pantallas = lista abajo + detalle arriba;
  una = lado a lado. Teclas: cruceta, L1/R1/←→ pestaña, A, B. Accesos: Ajustes › Biblioteca (primera opción) y pastilla en `CabeceraSistema` de switch2/ports.
- Barra de abajo: ahora superpuesta (`FranjaAccesos(e, Modifier.align(BottomCenter))` en PantallaInferior), `LocalAireAbajo` + `ALTO_FRANJA_*` para el relleno
  de las rejillas; lupa/ajustes/cajón sin círculo (ícono con sombra). Globito de DIPPO oculto con cualquier panel/ventana (`BurbujaDippoCapa(oculta=)`).
- Rendimiento: detrás del desenfoque `LocalMovimiento` = false (la vitrina deja de mecerse), `CarpetaBiblioteca` → `CarpetaCuerpo` sin Estado,
  trama de `ArteCarpeta` con un solo `drawPoints`. Novedades `switch2-ports-2.0-55`. Capturas t366–t371.

## Temas propios + Diptico.kt partido (11 nov, noche, rama `fix/temas-propios`, Opus)
- `estado/Temas.kt`: cada fiesta es tema completo (`ESTILO_FIESTA`: trama, acabado, forma, fondo; OJO va ANTES de `fiestas` porque el objeto se
  inicializa en orden, con `by lazy` también fallaba). Fechas nuevas: madre (2.º dom. mayo), padre (3.º dom. junio), profesor 16/10, libro 23/4,
  friki 25/5. `Temas.calendario()` = todas con su próxima fecha. `Temas.exclusivos` (8 `TemaExclusivo(logro, tema)` ids `x_*`, logros difíciles:
  racha14, racha30, trescientas, doscientas_h, quinientas_h, finales25, explorador50, consolas8); `todosLosTemas` suma `temasExclusivosGanados()`.
- Galería "temas": pestañas Incluidos, Calendario, Exclusivos (candado como los marcos), Mis temas (índice 3).
- Diptico.kt bajó de 10.090 a ~8.250 líneas: 109 funciones movidas como extensiones a `DipticoMenu.kt`, `DipticoTeclado.kt`, `DipticoImagenes.kt`,
  `DipticoInicioPersonal.kt`, `DipticoCentro.kt` (script en la sesión: mueve solo `fun`; las propiedades quedan en el objeto). Todo lo `private`
  del objeto pasó a `internal`. Los 60 archivos fuera de `estado` que usan Diptico importan `cl.nikalbazu.diptico.estado.*`.
- Traducciones: `herramientas/traducir/extras16.tsv` (siguiente: extras17). Novedades `temas-propios-2.0-56`. Capturas t372–t375.

## Por qué falló un juego + segunda tanda de partición (11 nov, noche, rama `fix/por-que-fallo`, Opus)
- `estado/DiagnosticoJuego.kt`: `Diagnostico.causas(juego, consola, emulador, bytes, veces)` (BIOS por consola, llaves de Switch, .nup con
  NovaS/Banana, formatos comprimidos, archivo de <1 KB, permiso del emulador, varias veces seguidas → otro emulador; máx. 5) y
  `Diptico.revisarCierreRapido(clave, seg)` llamado en `cerrarSesion(preguntar = true)`: si la sesión duró < `CIERRE_RAPIDO_SEG` (20 s)
  muestra `Dialogo("cierre-rapido")` con las causas y «Probar con otro emulador» (fija `datos.emulador` del juego al siguiente y relanza).
  Test `DiagnosticoTest`, captura t376. Novedades `por-que-fallo-2.0-57`.
- Diptico.kt bajó a ~5.800 líneas: diez archivos más (`DipticoTutorial`, `Paneles`, `Temas`, `Widgets`, `BurbujaWidgets`, `Respaldo`,
  `Archivos`, `Duracion`, `Desinstalar`, `Personalizacion`). OJO: las clases anidadas de Diptico (ItemGaleria, Control, OpcionBurbuja,
  InfoBateria, LanzadorPC, PorBorrar, Regreso) se importan como `cl.nikalbazu.diptico.estado.Diptico.X` en cada `Diptico*.kt`.
- Propuesta a Nicolás (pendiente de su sí): próxima versión grande «Emuladores al día», actualizador tipo Obtainium sobre `Actualizador.kt`
  (GitHub/Forgejo releases, APK arm64, PackageInstaller; firma distinta = desinstalar antes; los de Play Store solo se revisan).

## Emuladores al día (11 nov, noche, rama `fix/emuladores-al-dia`, Opus)
- `estado/EmuladoresAlDia.kt`: `FUENTES` (GitHub/Forgejo con APK; las de solo web abren su página; ports desde `Nativos.PORTS`), `EmuRevisado`,
  `revisarEmuladores(silencioso)`, `vigilarEmuladores` (90 s tras abrir, cada 20 h, `extra.emusAuto` clave `emusAuto`, `emusRevisado`),
  `actualizarEmulador(paquete)` (permiso de instalar → baja a cacheDir/emuladores → `mismaFirma` → `instalarApk`), `emuladorInstalado/NoInstalado`
  (los llama `ReceptorInstalacion` con el extra "paquete"). Firma distinta = `Dialogo("firma-distinta")` (desinstalar antes).
- Centro de Switch 2 y ports: 3.ª pestaña «Emuladores» (`PESTANAS_NATIVOS = 3`, `entradasEmuladores`, filas `dippo-emus` y `emu:<pkg>`).
  Ajustes › Juegos y emuladores › grupo «Emuladores al día». Test `EmuladoresAlDiaTest`.
- Premios: galería `logros_dippo` con pestañas Logros/Premios (`premiosDippo`, `premiosDeLogro` en DipticoPerfil.kt).
- Perfil fuera de Ajustes (`G_PERFIL` quitado de `GRUPOS_AJUSTES`; Discord en `G_CUENTAS`); se edita desde la tarjeta. `PerfilArriba(tam=54)`
  (nombre + título + foto grande, tipo iiSU) en `BarraSuperior(perfil=)` y en la vitrina de cajas; `EstadoChico(conFoto=)`.
- 30 funciones muertas borradas. Traducciones `extras17.tsv` (siguiente: extras18). Novedades `emuladores-al-dia-2.0-58`. Capturas t377–t383.
- Essentia fuera (11 nov, 23:00): fan games de RPG Maker con **Gahvoria** (`io.github.nikalbazu24.gahvoria`, repo Nikalbazu24/gahvoria). Regla
  `GAHVORIA` → `io.github.nikalbazu24.gahvoria.LaunchActivity` (VIEW + %ROMSAF% + extra `path`), ext `gahvoria`. Gahvoria aún NO tiene esa
  actividad: mientras no exista, `Lanzador` abre su biblioteca (`actividadExiste`). Falta agregar a Gahvoria la entrada para lanzadores
  (la sesión no tuvo permiso de escritura en su repo). Gahvoria también está en `EmuladoresAlDia.FUENTES`.
- Hora/batería arriba + juegos centrados (11 nov, 23:50, rama `fix/hora-centrado`, Sonnet): `RelojBateria()` (ui/Perfil.kt, píldora oscura con hora | pila | %)
  va dentro de `PerfilArriba(conReloj = true)` a la izquierda del nombre, centrada con la foto (vitrina de cajas y `BarraSuperior(perfil = true)`).
  `Mosaicos` resta `LocalAireAbajo` (la barra flotante) al alto y al centrado vertical: la fila queda al medio de lo visible. Captura t384.

## Carpetas y avisos (12 nov, rama `fix/hora-centrado`, Sonnet; Novedades `hora-iconos-2.0-59`)
- Hora/batería: `RelojBateria()` (píldora, `ui/Perfil.kt`) dentro de `PerfilArriba(conReloj = true)`. Juegos de abajo: `VistaJuegos` ya
  descuenta la barra (`padding(bottom = aire)`), así que `Mosaicos` NO resta `LocalAireAbajo` (antes se restaba dos veces); estante centrado con `(maxHeight - lado)/2`.
- Íconos pixel `ui/SpritesConsola.kt` (Switch, Switch 2, Gahvoria; 16×16 en texto) usados por `ConsolaPixel`.
- Avisos de DIPPO en la campana: `Avisos.publicarDippo/retirarDippo` (clave con prefijo `dippo:`), `Diptico.avisoDippo/avisoCalor` (`estado/AvisosDippo.kt`),
  `extra.dippoAvisos`; calor, batería baja, versión nueva de Díptico y de emuladores. Los textos dinámicos de estos avisos quedan solo en español.
- Carpetas: `extra.carpetasTono` ("auto"/"oscuro"/"claro") → `carpetasOscuras(tono)` → `ArteCarpeta(oscuro=)`. El estilo «consola» no usa ese dibujo.
- Arrastre de carpetas: gesto en la zona (no por carpeta) con `celdasEn` (bounds), carpeta fantasma sobre la zona y auto-scroll al acercarse al borde
  (`RejillaCentrada(estado=)` hoisteado). Sin probar en equipo (las capturas no muestran el arrastre). Traducciones `extras18.tsv`; siguiente: extras19.

## Apps arriba (12 nov, rama `fix/dock-arriba`, Sonnet; Novedades `dock-arriba-2.0-60`)
- Pedido de Nicolás (foto con líneas): el dock de apps fijadas va ARRIBA al medio de la pantalla de abajo (`EsquinasArriba`, entre DIPPO y la foto), solo en la lista de
  carpetas (`dockArriba(e)` en `ui/Mosaicos.kt`); en las demás vistas sigue abajo entre lupa y ajustes (`DockOApps`). `FranjaAccesos` ya no tiene barrita ni repliegue
  (`dockVisible` solo queda para otras pantallas); `ALTO_FRANJA_ABIERTA` = 56. Lupa y ajustes = pestañas semiredondas pegadas a las esquinas (`CirculoFranja(izquierda)`).
- Íconos pixel: Wii U y Android (`androidgames`) en `SpritesConsola.kt`; `ArteCarpeta` dibuja el sprite al centro si el estilo de íconos es «Píxeles» (`LocalIconos`).
  Traducciones `extras19.tsv` (siguiente: extras20). Capturas t388.

## Pixel y orden (12 nov, rama `fix/pixel-arriba`, Sonnet; Novedades `pixel-arriba-2.0-61`)
- Causa de «los íconos pixel no se ven arriba»: Nicolás usa el paquete **Pixel art** (`dot-art`, RetroArch, en Ajustes › Biblioteca › Íconos de consola),
  que no trae Switch/Switch 2/Wii U/Android/Gahvoria; `IconoConsola` los dibujaba como «insignia» (círculo con vector). Ahora `dot-art` sin PNG cae a
  `ConsolaPixel` (sprites de `SpritesConsola`) y `ArteCarpeta` también usa sprites con `dot-art`.
- Hora/batería (`RelojBateria`) pasó a la izquierda de la vitrina de cajas (junto al nombre de la consola) y de `BarraSuperior(perfil=true)`; `PerfilArriba()` sin reloj.
- `Mosaicos`: el elegido va centrado (relleno `aireLado` = (ancho − mosaico)/2 y `animateScrollToItem(i, 0)` dentro del `BoxWithConstraints`).
- `FilaDock`: sin sombras ni contornos. `EsquinasArriba` (abajo) sin burbuja de título; el título solo en `PerfilArriba` (arriba).
- Traducciones `extras20.tsv` (siguiente: extras21). Capturas t389–t395.

## Ajustes ordenados + vistas (12 nov, rama `fix/vistas-ajustes`, Opus; Novedades `ajustes-vistas-2.0-62`)
- Diseño nuevo en TODAS las vistas: `nuevoDisenoDosPantallas(e) = !unaPantalla && dosDisponibles` (ya no depende de `vista == "mosaicos"`);
  `Raiz`: sin barra vieja en la pantalla de abajo; `FranjaAccesos` en todas. Vista nueva `"xbox"` («Mosaicos Xbox», `esMosaico(vista)`):
  Mosaicos en dos filas con el primero grande aunque haya vitrina de cajas. Teclas usa `esMosaico`.
- Ajustes: 8 secciones (`GRUPOS_AJUSTES`: G_ESTILO=TEMAS+DECORACIONES+APARIENCIA+PANTALLAS, G_BIBLIOTECA, G_WIDGETS, G_JUEGOS, G_DIPPO,
  G_CONTROLES=CONTROLES+MUSICA «Controles y sonido», G_CUENTAS, G_SISTEMA; G_TEMAS/G_SONIDO quedan sin usar). `submenus(s, partes)` en
  DipticoMenu.kt: cada grupo (`grupoOpcion`) = fila «Nombre ›» con `sub` (ventana `VentanaSubOpciones`); grupo de 1 opción va directo;
  `PARTES_ENTERAS` van en un solo submenú; `nombreSubmenu` da nombres claros. DIPPO: Hablar, Logros y «Premios de los logros» directos.
  `condensar` no toca listas con submenús. `explicacion(o)` = ayuda o una explicación armada (todas las opciones explican algo).
  `Previas.seccionDe` busca la parte original de la opción (o de la 1.ª del submenú) para la vista previa.
- B vuelve a la ventana anterior: `Diptico.desdeMenu(accion)` (en `aceptar` y `usarSubOpcion`) apila `EnMenu` si la opción abrió otro panel;
  `cerrarPanel` llama `volverAlMenu()` (restaura sección, índice y subventana); `actualizar` vacía la pila al volver a NINGUNO.
- Vitrina de cajas: `RelojBateria` arriba del nombre de la consola (columna izquierda). Traducciones `extras21.tsv` (siguiente: extras22). Capturas t396–t404.
- Boceto de ajustes: https://claude.ai/artifact/Ezq1VDV9ZTwD2mf4johmxc (Nicolás pidió mantener el diseño actual: lista arriba, explicación abajo).

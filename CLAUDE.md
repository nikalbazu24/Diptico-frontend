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

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

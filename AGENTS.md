# Guía para agentes (Codex, Gemini y otros)

Díptico es un frontend Android de dos pantallas para la AYN Thor (también una pantalla, teléfonos y TV).
Kotlin + Jetpack Compose, paquete `cl.nikalbazu.diptico`. Lo usa gente adulta, con mando primero y tacto después.
El dueño (Nicolás) no programa: todo lo que llega a `main` tiene que funcionar sin que él toque código.

**Claude es el arquitecto del proyecto.** Reparte las tareas como issues de GitHub con la etiqueta `codex` o `gemini`
y revisa cada PR antes de que se una. Si una tarea no está en un issue, pregunta antes de hacerla.

## Reglas de trabajo

1. Una tarea = una rama `fix/<tema-corto>` desde `main` = un PR que cierra su issue (`Closes #N`). Nunca empujes a `main`.
2. Cambia solo lo que pide el issue. Si ves otro problema, anótalo en el PR («Visto, no tocado»), no lo arregles de paso.
3. Sin funciones nuevas ni opciones nuevas en el menú si el issue no las pide.
4. El PR describe en español simple qué cambió, cómo se probó y qué falta probar en la consola.

## Cómo está armado

- `estado/Diptico.kt`: estado único (`StateFlow<Estado>`) y casi toda la lógica. Es enorme: no lo hagas crecer; si una tarea
  agrega más de ~80 líneas, ponlas en un archivo nuevo de `estado/` y llámalo desde ahí.
- `estado/Teclas.kt`: todo el mando. Cualquier función nueva debe poder usarse con mando y con el dedo.
- `ui/`: pantallas en Compose. `Raiz.kt` arma las dos pantallas; `PantallaSuperior.kt` / `PantallaInferior.kt` / `PantallaUnica.kt`.
- Menú: `Seccion` (enum) y listas de `Opcion(titulo, valor, icono, ayuda)` en `Diptico.kt`. **Toda opción lleva `ayuda`**
  (qué hace, en una o dos frases simples).
- Respaldo: `estado/Respaldo.kt`. Si guardas un ajuste o archivo nuevo, revisa que entre en el respaldo.
- Imágenes: `ui/Imagenes.kt` (caché propia). No decodifiques imágenes grandes sin reducirlas.

## Lo que no se negocia

- Nada de disco, red ni decodificación de imágenes en el hilo principal. Usa `Dispatchers.IO`/`Default`.
- Nada de trabajo pesado por cuadro (en `draw`, `onGloballyPositioned`, recomposiciones): recuerda (`remember`) y mide.
- Sin claves, tokens ni direcciones privadas en el código. Cada persona pone sus propias claves.
- Sin permisos nuevos en el manifiesto salvo que el issue lo pida.
- Sin arte, nombres, logos ni motivos de franquicias (Pokémon, Mario, God of War, etc.). Todo dibujo es original.
- Textos de la interfaz en español de Chile, claros y cortos, sin tecnicismos («Glass», «JSON», «clip» → explicarlos).

## Antes de abrir el PR

1. Compila (`gradle :app:assembleCompletaDebug`) y corre las pruebas (`gradle :app:testCompletaDebugUnitTest`).
2. Si cambiaste una pantalla, agrega o actualiza su captura en `app/src/test/.../Capturas.kt`.
3. Textos nuevos: corre `python3 herramientas/textos.py`, agrega sus traducciones en un
   `herramientas/traducir/extrasNN.tsv` nuevo (es, en, pt, zh, ja separados por tabulador) y corre `python3 herramientas/traducir/armar.py`.
4. Si el cambio se va a publicar, cambia `ID` y las páginas de `estado/Novedades.kt` y actualiza `docs/novedades-beta.md`
   (la publicación en `main` falla si el ID no cambia).

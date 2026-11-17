# Imágenes de las consolas (assets/consolas/<id>.webp)

Fotos de estudio hechas desde modelos 3D de Sketchfab (créditos en `Modelos3D`), con fondo transparente.
Concepto: **pantalla apagada y colores originales**, sin arte de juegos (no se usan pantallas ni etiquetas con juegos reales).

- `tex.mjs sacar|poner <id>`: saca las texturas de `h/<id>.glb` a `tx/` y vuelve a ponerlas en `h2/<id>.glb`.
- `pintar.py`: ayudas para pintar (pantalla apagada con reflejo, etiquetas lisas, rampa de color).
  GBA: cuerpo al índigo original con una rampa por luminancia, calcomanías borradas con inpaint de OpenCV.
- `sinemis.mjs` quita la textura emisiva (ahí venían las pantallas encendidas de GBA y PSP);
  `sinnormal.mjs` quita relieve (la GBA traía emblemas en el mapa de normales); `quitar.mjs` borra mallas por nombre
  (pantallas superpuestas de la Game Boy, texto del robot Android).
- `consolas_png.py [ids]` (con `DIR=h2`) renderiza con model-viewer + Chrome (swiftshader) de frente con 25° de giro.
- La DS es una foto de Evan-Amos (Wikimedia Commons, dominio público).

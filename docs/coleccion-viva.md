# Colección viva · especificación visual (aprobada por Nicolás el 16 nov 2026)

Bocetos: https://claude.ai/artifact/VTCZof8gamwjrsQTPU1HKj (tableros 1–7). Esta página es la regla para programarlos.

## Base común
- Medidas en **píxeles del boceto**: Thor arriba y Odin 1920×1080, Thor abajo 1240×1080. En la app se escalan con
  `Boceto` (ui/coleccion/Boceto.kt): 1 px del boceto = `min(ancho/1920, alto/1080)` dp. Así se ve igual en cualquier densidad.
- Fondo `#07080f` / `#0b0c16`. Vidrio `rgba(18,20,36,.55)` con borde blanco 12 %. Texto `#F4F3FA`, secundario `#B9BBD6`,
  tenue `#8E90AE`. Elegido = relleno `#F4F3FA` con texto `#12131f`, o anillo blanco de 3–4 px con halo.
- Letras: títulos Fredoka 600–700, el resto Nunito 700–800. Con un tema, los colores, el radio y la letra del título salen del tema
  (`ColoresColeccion.delTema()`); con el tema oscuro por defecto son exactamente los de arriba.
- **Nada encima de nada**: cada parte vive en su franja. Toda la pantalla se usa. Lo vistoso se apaga con «Animaciones: apagadas».
- DIPPO = el pulpito de luz (`AvatarBisa`). Dock de apps = `FilaDock`.

## 1 · Thor arriba (dentro de una consola)
- Arte del juego a pantalla completa + velo: 90° de .92→.72 (34 %)→.1 (62 %)→0, y 0° de .85→0 (34 %).
- Barra (top 44, lados 96, alto 64): píldora consola (punto de color 16, nombre Fredoka 28, «3 de 42» 22) · a la derecha
  píldora hora | batería | % (26/24) · foto de perfil 72 con anillo blanco 3.
- Info (left 96, top 210, ancho 820, separación 22): «GÉNERO · AÑO» 24/800 espaciado .14em color claro de la consola;
  título Fredoka 700 118 (o el logo del juego); descripción 28 (máx 2–3 líneas); chips 23 (horas, avance, cuándo);
  barra de avance 8 px + «Tu próximo paso» 24.
- Acciones (left 96, bottom 92, alto 92): A Continuar (blanco), X Ficha, Y Editar caja (vidrio).
- Caja 3D (left 1240, top 210, 400×560) girada 24°, sin pedestal; sombra elíptica en el piso. Tocar o R3: la caja viaja al
  centro, crece ×1.34 y gira 180° (reverso); la info se desvanece; abajo pasa al **carrete** (2b).
- DIPPO abajo a la derecha (right 96, bottom 104): píldora con su cara 52 y una frase 23.

## 2 · Thor abajo
- Fila arriba (top 32, alto 76): DIPPO 76 · buscador «Buscar en tu colección» · Widgets · Apps.
- Consolas (top 136, alto 104): L1 · píldoras 76 con punto de color (elegida blanca) · R1.
- Estante 3D (top 280, alto 480): 5 cajas, elegida 250×350 al centro con anillo blanco, vecinas 200×280 giradas ±48° y al 78 %.
- Título del elegido Fredoka 44 + «3 de 42 · ◀ ▶ para recorrer · A jugar» 22.
- Barra (bottom 36, alto 96): selector Estante 3D | Cuadrícula | Lista · dock de 5 apps · Colecciones · Opciones · Ajustes.
- 2b Carrete (con la caja girada): imagen grande, fuentes ScreenScraper/SteamGridDB/TheGamesDB, miniaturas, pistas.

## 3 · Una pantalla (Odin 2 Portal)
- Franja 1 (88): consola · búsqueda, apps, widgets, ajustes · DIPPO, hora y batería.
- Franja 2: info a la izquierda, caja a la derecha. Franja 3: estante de cajas en una fila.
- 3b: reverso (zoom) + carrete en la misma pantalla; R3 gira.

## 4 · Estudio de Cajas y 5 · Ajustes
- Mismo vidrio y letras. Botones del Estudio de 112 px con letra 29–30. Ajustes: secciones · opciones · explicación y previa real.

## 6 · Carpetas
- 6a Thor arriba: «MARCA · AÑO · GENERACIÓN», nombre 116 (o el logo de la consola: «Mostrar el logo»), historia, chips,
  6 opciones (Ícono de la carpeta, Vista, Color, Emulador, Fondo y música, Nombre o logo), A Entrar / Y Seguir con…;
  a la derecha la consola en 3D (giro con el dedo y con inercia), L1 ◀ nombre ▶ R1 con zoom al cambiar, crédito del modelo,
  sonido de encendido original (no el de la consola real).
- 6b Thor abajo: Consolas | Colecciones · DIPPO con su frase · vistas Ícono | Díptico | Repisa | Carpeta · rejilla · pistas.
- 6c una pantalla: info y opciones a la izquierda; arriba a la derecha la consola 3D; debajo «Juegos junto al ícono»
  (Últimos jugados | Favoritos | Elegidos por mí · 1–3) y las carpetas de 6b; abajo DIPPO y el dock.
- 6d Ícono de la carpeta: pestañas SteamGridDB, ScreenScraper, libretro, Comunidad, Tu galería, Díptico la arma; candidatos; previa.
- 6e Carpeta abierta (una pantalla): barra · info del juego + A/X/R3 · caja 3D · fila de 8 juegos con el elegido grande.

## 7 · Widgets
- 7a: cuadrícula 6×3 con separación 20: Continuar (2×2), Tu semana (2×1), DIPPO (2×1), ¿Qué juego hoy?, Logro más cercano,
  Equipo, Reloj, Tus favoritos (3×1), Te extraña, Hace un año (2×1). «Editar widgets» muestra borde punteado y ✕.
  Todo toma colores, radio y letra del tema.
- 7b: tienda de widgets (2 columnas, agregar/quitar, tamaño, espacios usados). 7c: 7a con DIPPO y dock abajo.

## Modelos 3D de las consolas
- Se guardan en el equipo (sin internet) como giros pre-renderizados de 72 cuadros (`assets/consolas3d/<id>.webp`, 8×9,
  480×345), hechos desde los .glb que mandó Nicolás. Créditos y licencias en Ajustes › Acerca de.

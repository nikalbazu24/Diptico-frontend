# Referencias de las carpetas

Las carpetas del rediseño incluyen un collage vectorial con referencias a juegos de cada plataforma. El hardware se sitúa en el centro y el color distingue el sistema. Las imágenes descargadas o elegidas como portada y los íconos propios tienen prioridad.

`ui/ReferenciasCarpetas.kt` cubre los 197 identificadores del archivo `assets/sistemas.json`. Dos alias de Android se unifican al cargar el catálogo: los 195 sistemas visibles tienen una entrada. Las categorías de aplicaciones, medios y emuladores usan una marca de colección porque no representan una consola con un juego insignia. Las referencias son decisiones visuales editoriales, no una clasificación oficial de popularidad.

| Plataforma | Referencia visual |
|---|---|
| NES / SNES | Gorra y estrella de Mario |
| Nintendo 64 | Trifuerza de Ocarina of Time |
| GameCube | Vela de The Wind Waker |
| Wii / Wii U | Estrella de Galaxy y calamar de Splatoon |
| Switch / Switch 2 | Espada de Breath of the Wild y bandera de Mario Kart World |
| GB / GBC / GBA / DS | Bloques de Tetris y referencias a Pokémon |
| 3DS | Hoja de Animal Crossing |
| PlayStation | Espadón de Final Fantasy VII |
| PS2 / PSP / Vita | Omega, garras y televisión |
| Xbox | Casco y visor de Halo |
| Sega | Anillo, nave o luna según el sistema |
| Android | Estrella de viento de Genshin Impact |
| RPG Maker / EasyRPG | Luna de To the Moon / Yume Nikki |

Los demás equipos, motores, ordenadores y consolas virtuales tienen sus referencias en el mismo registro. La prueba de cobertura recorre el catálogo real y falla si se añade un sistema sin referencia. La lámina de capturas dibuja todos los íconos, incluidos los sistemas menos comunes.

Los contornos se preparan en la caché de dibujo y se escalan al tamaño de la carpeta. No hay descargas, imágenes generadas en cada celda ni animaciones permanentes. El registro vectorial contiene los nombres de los juegos y los motivos; todas las portadas se construyen con los dibujos propios de la app.

## Referencias para sistemas menos comunes

- [Catálogo de Vircon32](https://www.vircon32.com/games.html)
- [Arduboy y Micro City](https://www.arduboy.com/)
- [Tetris de Uzebox](https://uzebox.org/wiki/Tetris)
- [WASM-4 y Snake](https://wasm4.org/play/snake/)
- [LowRes NX](https://lowresnx.inutilis.com/)
- [ChaiLove y Floppy Bird](https://docs.libretro.com/library/chailove/)
- [Lutro y sus ejemplos](https://github.com/libretro/libretro-lutro)

## Revisión a partir de la referencia de Game Boy

Las portadas cambian a una composición con el hardware centrado en una placa de color, rodeado de varios motivos de juegos. El collage vectorial conserva la paleta de cada sistema y la nitidez al ampliar. Los dibujos dejan de ocupar el lugar del símbolo de consola.

Game Boy y Super Game Boy también usan una portada vectorial propia: consola centrada en verde oliva y motivos de sus juegos alrededor. No se incluye la imagen adjunta del usuario. Todo el catálogo utiliza ilustración vectorial de la app, con motivos relacionados agrupados por familia. Android y RPG Maker conservan cobertura. Las portadas e iconos personalizados mantienen prioridad. La selección utiliza un contorno fino y la cantidad de juegos se muestra en una cápsula independiente.

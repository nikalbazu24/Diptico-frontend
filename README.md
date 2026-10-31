# Díptico

Un launcher para la **AYN Thor** que usa las dos pantallas como un díptico: arriba, el juego elegido ocupa todo; abajo, eliges consola y juego en un estante con la forma real de cada caja.

## Qué hace la v0.2

- **Funciona con una o dos pantallas.** El tutorial parte siempre en una sola pantalla y ahí eliges el modo. Sirve igual en la AYN Thor, una Odin 2 Portal, una tele o un teléfono.
- **Letra más grande y adaptable.** Todo se dibuja en proporción a la pantalla, también con las pantallas intercambiadas. Hay tres tamaños de letra: normal, grande y muy grande.
- **Menú nuevo con secciones e íconos propios:** Pantallas, Apariencia, Música, Emuladores, Portadas, Biblioteca y Sistema. Con dos pantallas, la de arriba explica en grande la opción elegida.
- **Música de fondo propia**, con tres pistas originales (Bisagra, Cartucho y Medianoche), una mezcla o tu propia carpeta de canciones. Se calla sola al jugar o si pones Spotify o YouTube Music.
- **RetroArch con sus núcleos** para SNES, N64, PSX, GBA, GameCube, DS, PS2 y PSP, igual que en ES-DE.
- **Descarga de portadas, imágenes y títulos** desde libretro (gratis) y, con tu clave, logos y arte grande desde SteamGridDB. Se configura en el tutorial.
- **Estante de ports:** marca tus ports y juegos nativos de Android (Ship of Harkinian, Doom, Mario 64…) y aparecen en su propio estante, con tiempo jugado y próximo paso.
- **Intercambiar pantallas** desde la barra de abajo o con Select.
- Animación de entrada, próximo paso, tiempo jugado, "Continuar" y pantalla atenuada al jugar, como en la v0.1.

## Cómo instalarla

1. Desde la Thor, entra a la pestaña **Releases** de este repositorio.
2. Descarga el archivo `Diptico-v0.2.X.apk` más reciente y ábrelo para instalar.
3. Al abrir Díptico, elige tu carpeta de juegos.

Las versiones nuevas se instalan encima de la anterior, sin perder tus datos.

## Cómo ordenar tus juegos

Dentro de tu carpeta de juegos debe haber una carpeta por consola, igual que en ES-DE:

| Carpeta | Consola | Emuladores que Díptico sabe abrir |
|---|---|---|
| `snes` | Super Nintendo | Snes9x EX+, RetroArch |
| `n64` | Nintendo 64 | Mupen64Plus FZ / AE, RetroArch |
| `psx` | PlayStation | DuckStation, RetroArch |
| `gba` | Game Boy Advance | Pizza Boy GBA, RetroArch |
| `gc` | GameCube | Dolphin, Dolphin MMJR2, RetroArch |
| `nds` | Nintendo DS | melonDS, DraStic, RetroArch |
| `ps2` | PlayStation 2 | NetherSX2, RetroArch |
| `ps3` | PlayStation 3 | ARMSX3, aPS3e |
| `ps4` | PlayStation 4 | Bachata S4 |
| `psp` | PSP | PPSSPP, RetroArch |

### PS3 y PS4

- **PS3:** sirven archivos `.iso` y juegos en carpeta. Con ARMSX3 también puedes crear un archivo de texto `.ps3` que tenga adentro solo el código del juego, por ejemplo `BLUS30443`.
- **PS4:** primero importa el juego dentro de Bachata S4. Después crea en la carpeta `ps4` un archivo de texto, por ejemplo `Bloodborne.ps4`, con el código del juego adentro: `CUSA00900`.

Los dos emuladores son experimentales: algunos juegos andarán bien y otros no.

### Portadas

Díptico usa una imagen con el mismo nombre que el juego, ya sea al lado del juego o dentro de una carpeta `covers`. También puedes conectar la carpeta `downloaded_media` de ES-DE.

Para lo que falte, en **Menú › Portadas › Descargar lo que falta** Díptico busca en libretro-thumbnails (gratis, sin cuenta). Si además pegas tu clave de [SteamGridDB](https://www.steamgriddb.com) (Preferencias › API), también trae logos y arte grande. Si no encuentra nada, dibuja una portada de ejemplo.

### RetroArch

En **Menú › Emuladores** aprieta A sobre una consola para pasar por los emuladores instalados, incluido cada núcleo de RetroArch. El núcleo tiene que estar descargado dentro de RetroArch (Menú principal › Cargar núcleo › Descargar un núcleo).

## Botones

| Botón | Acción |
|---|---|
| ← → | Moverte por el estante |
| L1 / R1 | Cambiar de consola |
| L2 / R2 | Saltar de a 5 juegos |
| A | Jugar (mantén presionado para el menú rápido) |
| B | Volver o cerrar |
| Y | Anotar tu próximo paso |
| Select | Intercambiar pantallas |
| Start | Menú (en el menú: ← → cambian de columna, L1/R1 de sección) |

## Cómo se construye

Cada cambio en `main` hace que GitHub Actions compile el APK, saque fotos de todas las pantallas con el tamaño de la Thor y publique ambas cosas en Releases. Las órdenes para abrir cada emulador vienen de las reglas de ES-DE para Android.

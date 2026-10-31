# Díptico

Un launcher para la **AYN Thor** que usa las dos pantallas como un díptico: arriba, el juego elegido ocupa todo; abajo, eliges consola y juego en un estante con la forma real de cada caja.

## Qué hace la v0.1

- **Dos pantallas.** Arriba ves el arte del juego desvanecido, con su título y tu próximo paso. Abajo están las consolas, el estante de juegos y la barra de acciones.
- **Abre tus juegos con su emulador**, en la pantalla de arriba o en la de abajo.
- **Tu próximo paso.** Al volver de jugar, te pregunta dónde quedaste y te lo recuerda la próxima vez.
- **Tiempo jugado y "Continuar"** para volver a tu último juego.
- **Pantalla de abajo atenuada** con el nombre del juego mientras juegas arriba. Se despierta al tocarla.
- **Control completo con botones**, modo claro y oscuro, e intercambio de pantallas.

## Cómo instalarla

1. Desde la Thor, entra a la pestaña **Releases** de este repositorio.
2. Descarga el archivo `Diptico-v0.1.X.apk` más reciente y ábrelo para instalar.
3. Al abrir Díptico, elige tu carpeta de juegos.

Las versiones nuevas se instalan encima de la anterior, sin perder tus datos.

## Cómo ordenar tus juegos

Dentro de tu carpeta de juegos debe haber una carpeta por consola, igual que en ES-DE:

| Carpeta | Consola | Emuladores que Díptico sabe abrir |
|---|---|---|
| `snes` | Super Nintendo | Snes9x EX+ |
| `n64` | Nintendo 64 | Mupen64Plus FZ / AE |
| `psx` | PlayStation | DuckStation |
| `gba` | Game Boy Advance | Pizza Boy GBA |
| `gc` | GameCube | Dolphin, Dolphin MMJR2 |
| `nds` | Nintendo DS | melonDS, DraStic |
| `ps2` | PlayStation 2 | NetherSX2 |
| `ps3` | PlayStation 3 | ARMSX3, aPS3e |
| `ps4` | PlayStation 4 | Bachata S4 |
| `psp` | PSP | PPSSPP |

### PS3 y PS4

- **PS3:** sirven archivos `.iso` y juegos en carpeta. Con ARMSX3 también puedes crear un archivo de texto `.ps3` que tenga adentro solo el código del juego, por ejemplo `BLUS30443`.
- **PS4:** primero importa el juego dentro de Bachata S4. Después crea en la carpeta `ps4` un archivo de texto, por ejemplo `Bloodborne.ps4`, con el código del juego adentro: `CUSA00900`.

Los dos emuladores son experimentales: algunos juegos andarán bien y otros no.

### Portadas

Díptico usa una imagen con el mismo nombre que el juego, ya sea al lado del juego o dentro de una carpeta `covers`. También puedes conectar la carpeta `downloaded_media` de ES-DE desde Ajustes. Si no hay imagen, dibuja una portada de ejemplo.

## Botones

| Botón | Acción |
|---|---|
| ← → | Moverte por el estante |
| L1 / R1 | Cambiar de consola |
| L2 / R2 | Saltar de a 5 juegos |
| A | Jugar (mantén presionado para el menú rápido) |
| B | Volver o cerrar |
| Y | Anotar tu próximo paso |
| Start | Ajustes |

## Cómo se construye

Cada cambio en `main` hace que GitHub Actions compile el APK y publique una versión nueva en Releases. Las órdenes para abrir cada emulador vienen de las reglas de ES-DE para Android.

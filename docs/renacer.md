# Reconstrucción de Díptico

## La experiencia que queremos construir

Díptico es un lugar al que apetece volver para jugar. Su identidad nace de sus dos hojas. Una presenta el juego, la otra deja elegirlo. En un equipo de una pantalla, ambas conviven en el mismo espacio.

El recorrido principal empieza en la partida que quieres retomar. Tiene tres destinos estables.

| Destino | Lo que resuelve |
|---|---|
| Jugar | Retomar una partida, recordar el próximo paso y elegir algo de la biblioteca |
| Biblioteca | Encontrar juegos, entrar en sus consolas, ordenarlos y consultar sus opciones |
| Personalizar | Elegir cómo se siente Díptico y administrar sus preferencias |

La dirección visual se basa en una biblioteca de videojuegos para mando. El arte, los logos y las portadas del juego ocupan el centro. Las consolas tienen emblemas de hardware, el foco usa un marco visible y los accesos muestran los botones del control. La marca representa dos pantallas y sus botones.

Los 30 temas actuales de Díptico definen los colores, las fuentes, los marcos, la trama y las decoraciones de la nueva interfaz. El calendario automático aplica el tema completo de la celebración. Elegir un tema manual pausa el calendario hasta que la persona vuelva a activarlo.

### Referencias de la revisión visual

| Referencia | Criterio aplicado | Fuente oficial |
|---|---|---|
| iiSU | Arte e íconos con presencia, composición de una y dos pantallas | [Galería](https://iisu.network/gallery) |
| Cocoon | Cuadrícula de consolas y juegos y temas que transforman la interfaz | [Presentación](https://cocoon-shell.com/) |
| ES-DE | Navegación con mando y una estructura adaptable a temas distintos | [Frontend y temas](https://es-de.org/) |
| Daijishō | Identidad visual por plataforma y opciones agrupadas | [Repositorio oficial](https://github.com/TapiocaFox/Daijishou) |
| Beacon | Selección directa de juegos, portadas y metadatos | [Ficha del desarrollador](https://play.google.com/store/apps/details?id=com.radikal.gamelauncher) |

Estas referencias orientan la composición y los recorridos. Los recursos visuales y el código utilizados son los de Díptico.

El tono es cercano. La bienvenida pide agregar juegos y permite recuperar un respaldo. Los permisos aparecen al realizar la acción que los necesita. Al abrir una ficha se conserva el lugar, la consola, la búsqueda y el filtro. Siempre hay una salida visible de Ocultos.

## Primera entrega ejecutable

La variante `completaRenacer` instala `cl.nikalbazu.diptico.renacer`, con el nombre Díptico Renacer. Se puede instalar junto a la beta y probar con las mismas carpetas de juegos. El selector de Android concede los permisos a esta instalación.

Esta primera edición usa textos en español. Incluye la nueva entrada de la app, navegación, inicio, biblioteca por consolas, cuadrícula y lista, búsqueda, favoritos, juegos ocultos, apps de Android, asignación manual de consolas, selección de emulador, lanzamiento en una de las pantallas, próximo paso, arte local o descargado, el catálogo visual de temas y el calendario completo y respaldo portable. La segunda pantalla comparte selección y sesión con la principal.

Para recuperar datos de la beta, guarda un respaldo desde ella e impórtalo en Renacer. Se recuperan los datos de juegos que contiene ese respaldo. Las carpetas y recursos de este dispositivo se seleccionan desde Renacer. Las bibliotecas muy grandes, los emuladores, el cambio físico de pantalla y las sesiones necesitan prueba en hardware además de las pruebas automatizadas.

## Estructura del nuevo código

| Archivo | Responsabilidad |
|---|---|
| `Salon.kt` | Datos de la experiencia, destinos, filtros y reglas de regreso |
| `Accion.kt` | Acciones que la interfaz envía al coordinador |
| `BibliotecaRenacer.kt` | Lectura, caché y presentación de juegos y apps |
| `RenacerControl.kt` | Coordinar tareas, guardar cambios y mantener la sesión compartida |
| `RenacerActivity.kt` | Permisos, selectores de Android y pantallas físicas |
| `MandoRenacer.kt` | Repetición del stick mediante el foco nativo de Android |
| `Identidad.kt` | Selección de temas, calendario, formas y contraste |
| `Diseno.kt` | Paleta, tipografía, marca, foco y arte vectorial |
| `SalonUI.kt` | Inicio, biblioteca, ambiente y pantalla compañera |
| `Ventanas.kt` | Fichas y administración de fuentes |

El coordinador publica un estado inmutable. Las pantallas envían acciones y se dibujan desde ese estado. Las escrituras se serializan y las peticiones repetidas de escaneo se agrupan. Una configuración que cambia durante el escaneo vuelve a leerse antes de mostrar resultados. Los archivos de una carpeta que pierde acceso se conservan en la caché de esa carpeta.

Se aprovechan el catálogo ES-DE, los comandos de lanzamiento, la lectura de archivos, las imágenes y el formato de respaldo existentes como integraciones. La nueva entrada de la app usa su propio coordinador y pantallas. La variante Renacer tiene un flujo de pruebas y APK propio en GitHub Actions.

## Próximos hitos y condiciones para adoptarlos

| Hito | Trabajo | Condición de cierre |
|---|---|---|
| Identidad y recorrido principal | Probar esta edición en Thor y una pantalla, revisar tamaños, foco, regresos y primera configuración | Agregar una biblioteca, jugar y volver conservando la selección funciona con mando y toque |
| Biblioteca completa | Llevar discos y variantes, colecciones, carpetas ajustables, proveedores y personalización a la nueva estructura | Cada opción tiene un lugar definido y una prueba de su recorrido |
| Ambiente completo | Integrar temas de la tienda, recursos, calendario, decoraciones por tema, intensidad y editor | La tienda vive en Díptico y los temas respetan estructura y legibilidad |
| Compañero de partida | Integrar Bisa, bitácora, audio, logros, lupa y retoma en el nuevo modelo | Los permisos, la captura y el regreso funcionan en equipo real |
| Inicio personal | Reconstruir widgets y perfiles sobre el mismo sistema de composición y foco | Editar con mando y toque conserva el contenido y funciona en una pantalla |
| Accesibilidad e idiomas | Revisar escalado de letra, lector de pantalla, foco e idiomas mediante recursos de Android | Los recorridos completos conservan legibilidad y funcionan en cada idioma |
| Migración y publicación | Validar respaldos, recursos locales, actualizador y recuperación de fallos | Migración comprobada, pruebas completas y APK de distribución verificado |

Cada hito debe entregar un recorrido utilizable. La matriz de funciones del README de la beta sirve como inventario de migración. El código anterior se retira por componentes a medida que su reemplazo cubre el comportamiento y los datos necesarios.

## Validación de esta entrega

`RenacerTest` comprueba los regresos, las salidas de filtros, la búsqueda en otros idiomas y el comportamiento de la caché cuando una carpeta pierde acceso o se quita. `CapturasRenacer` verifica las acciones principales y genera imágenes en escritorio, pantalla inferior y teléfono.

Las capturas combinan datos de ejemplo, fondos vectoriales de reserva y medios de Super Mario World y Pokémon Emerald procedentes de libretro-thumbnails para revisar el espacio real de las portadas. Las imágenes de apoyo se guardan como recursos de las pruebas. Las capturas permiten revisar la interfaz. La fluidez y los emuladores se evalúan en dispositivos.

# Reconstrucción de Díptico

## La experiencia que queremos construir

Díptico es un lugar al que apetece volver para jugar. Su identidad nace de sus dos hojas. Una presenta el juego, la otra deja elegirlo. En un equipo de una pantalla, ambas conviven en el mismo espacio.

El recorrido principal empieza en la partida que quieres retomar. Tiene tres destinos estables.

| Destino | Lo que resuelve |
|---|---|
| Jugar | Retomar una partida, recordar el próximo paso y elegir algo de la biblioteca |
| Biblioteca | Encontrar juegos, entrar en sus consolas, ordenarlos y consultar sus opciones |
| Ambiente | Elegir cómo se siente Díptico y administrar sus preferencias |

La dirección visual de esta primera propuesta usa papel cálido, tinta, terracota y una bisagra como marca. Los títulos tienen una tipografía editorial. Los estados seleccionados y el foco del mando se distinguen con borde y color. Los fondos vectoriales son propios y sirven mientras falta arte de un juego. Las portadas del usuario toman su lugar cuando están disponibles.

El tono es cercano. La bienvenida pide agregar juegos y permite recuperar un respaldo. Los permisos aparecen al realizar la acción que los necesita. Al abrir una ficha se conserva el lugar, la consola, la búsqueda y el filtro. Siempre hay una salida visible de Ocultos.

## Primera entrega ejecutable

La variante `completaRenacer` instala `cl.nikalbazu.diptico.renacer`, con el nombre Díptico Renacer. Se puede instalar junto a la beta y probar con las mismas carpetas de juegos. El selector de Android concede los permisos a esta instalación.

Esta primera edición usa textos en español. Incluye la nueva entrada de la app, navegación, inicio, biblioteca por consolas, cuadrícula y lista, búsqueda, favoritos, juegos ocultos, apps de Android, asignación manual de consolas, selección de emulador, lanzamiento en una de las pantallas, próximo paso, arte local o descargado, tres ambientes, acentos del calendario y respaldo portable. La segunda pantalla comparte selección y sesión con la principal.

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

Los juegos de las capturas son ejemplos de diseño con nombres y paisajes originales. Las capturas permiten revisar la interfaz. La fluidez y los emuladores se evalúan en dispositivos.

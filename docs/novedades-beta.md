# Díptico 2.0.2 beta — widgets primero

- **Agregado · Díptico abre en tus widgets:** al encender se ven primero los widgets. Deslizando se llega al inicio con tu juego y, siguiendo, de vuelta a los widgets, sin fin y para los dos lados. El botón, R2/L2 y DIPPO mueven la misma cinta. Quien tenía otro arranque elegido pasa una vez a widgets (se cambia en Ajustes › Inicio › Al abrir). El modo seguro sigue abriendo en la portada.
- **Cambiado · Configuración de la primera vez:** todo en la pantalla principal, con «¡Bienvenido!», la fila de pasos con sus nombres, letras más grandes y los personajes dentro de su propia fila. Abajo solo el logo de Díptico animado. Con una pantalla, el logo va chico en la esquina.
- **Cambiado · Recorrido de DIPPO:** 17 pasos que parten en los widgets. Cada paso pide hacer algo de verdad (con el mando, el dedo o «Probar ahora») y no se sigue hasta hacerlo; B ya no lo cierra y se quitó «Saltar recorrido». Los textos caben enteros.
- **Corregido · Fluidez:** el deslizamiento widgets ↔ juego ya no rearma las páginas; la lista de fotos de consolas se lee una sola vez en vez de en cada cambio de consola (antes en el hilo principal).
- **Quitado:** las vistas previas de cada paso de la configuración (estorbaban y achicaban las letras).
- **Qué probar en la consola:** abrir Díptico (debe partir en widgets); deslizar muchas veces seguidas hacia ambos lados; L2/R2 seguidos rápido; «Volver a configurar» y recorrer todo el tour solo con el mando, y otra vez solo con el dedo; el tour en la Odin 2 Portal.

---

# Díptico 2.0.1 beta — emuladores más confiables

Arreglos de una revisión del lanzador de juegos y apps.

- **Tu elección manda:** el emulador elegido para una consola gana a las recomendaciones (Banana Native, PokeDaisy, Peanuts Native). El elegido para un juego sigue ganando a todo.
- **Emulador correcto:** Díptico solo ofrece un emulador si tiene de verdad la pantalla que abre juegos (y está abierta a otras apps); si no, prueba el siguiente. Si está instalado pero en una versión que no sirve, dice «actualiza X» en vez de «instala X». Las apps sin ícono ya no aparecen como «no instalada».
- **Cierre rápido:** si el emulador se cae en los primeros 4 segundos, Díptico lo vuelve a mirar a los 4,5 s y DIPPO explica qué pudo pasar (antes se ignoraba).
- **Carpetas de solo lectura:** primero se pide lectura y escritura, luego solo lectura y al final sin permisos.
- **Rutas:** en Descargas o la nube ya no se inventa una ruta falsa; para `%ROM%` se manda el documento.
- **Reglas:** se reconoce Play! (`%EMULATOR_PLAY!%`); melonDS deja de duplicar a WatermelonDS; se borró una regla de MAME4droid sin uso; ARMSX2 `com.nanodata.armsx2` primero; los paquetes «camuflados» (Genshin, PUBG, AnTuTu…) ya no esconden esos juegos reales de «Juegos de Android».
- **Emuladores al día:** Winlator Cmod ya no se confunde con Winlator; si el APK bajado es de otra app, no se instala ni se ofrece desinstalar nada.
- **Biomeria:** el enlace nunca lleva `source_frontend` dos veces y, si Biomeria no da permiso, queda anotado el motivo.
- **Qué probar en la consola:** elegir un emulador para PS2 y ver que se usa; un juego de una carpeta de solo lectura; cerrar un emulador a la fuerza recién abierto (debe salir el aviso de DIPPO); Emuladores al día con Winlator Cmod instalado.

---

# Díptico 2.0.1 beta — Peanuts Native a punto

Compatibilidad completa con **Peanuts Native** (juegos de Switch 1 convertidos en apps nativas, archivos `.nup`).

- **Se abre bien:** Díptico le pasa a Peanuts la ruta del `.nup` (la forma que su documentación recomienda) por su entrada para lanzadores `dev.peanuts.launch.LaunchActivity`.
- **También desde la carpeta de Switch 2:** un `.nup` cuyo nombre trae el código de un juego de Switch 1 (empieza con `0100`) se abre con Peanuts. Los de Switch 2 (`0400…`) siguen con NovaS o Banana Native, igual que antes.
- **Avisos claros antes de abrir:** si el archivo termina en `.NUP` con mayúsculas, o está en un lugar sin ruta (Descargas, la nube), Díptico explica qué hacer en vez de mandar una ruta que Peanuts no puede leer. La raíz «Documentos» del selector de Android ahora se traduce bien a su ruta (sirve para todos los emuladores).
- **Si el juego se cierra enseguida:** DIPPO recuerda darle a Peanuts «Acceso a todos los archivos» y terminar de prepararlo en su pantalla; si no la tienes, te sugiere instalarla.
- **En el centro de ports:** fila «Peanuts Native» en Ports nativos (instala desde GitHub o la abre). La carpeta de Switch tiene la pastilla «Peanuts y ports».
- **Emuladores al día:** Peanuts publica «v0.1.8-1» y la app dice «0.1.8»: ya no aparece siempre como desactualizada.
- **Qué probar en la consola:** un `.nup` de Switch 1 en la memoria interna y otro en la SD; Peanuts sin y con «Acceso a todos los archivos»; un `.nup` en la carpeta de Switch 2 con `[0100…]` en el nombre; Ports nativos › Peanuts Native › A.

---

# Díptico 2.0.1 beta — mejoras oficiales del Home y las carpetas

Esta actualización incorpora los ajustes probados en Díptico Pruebas a la **aplicación oficial**, manteniendo su identificador `cl.nikalbazu.diptico`, su firma y los datos de la instalación existente.

- **Home compacto en una pantalla:** selector de consolas centrado, carpeta activa más destacada, botón Entrar accesible y Dippo independiente.
- **Dock coherente:** apps fijadas en una barra vertical derecha, tanto en Home como dentro de una consola.
- **Perfil en la esquina:** Dippo, hora, batería y fotografía del usuario quedan fijos arriba a la derecha.
- **Caja 3D sin solapamientos:** contenedor limitado para que la caja no invada la barra superior; títulos y acciones Jugar, Ficha y Editar caja más compactos.
- **Carrusel de juegos:** una sola fila horizontal de carátulas dentro de las carpetas en el modo de una pantalla, conservando navegación con mando, Select y las demás vistas.
- **Recarga uniforme:** el botón de actualizar juegos tiene la misma apariencia que los demás botones.
- **Apps amigas:** Biomeria Launcher (tus instancias de Minecraft aparecen en la carpeta Minecraft, se abren desde Díptico, vuelves al salir y se suman sus horas), PokeDaisy (abre tus juegos de Pokémon de GB, GBC, GBA y DS) y Peanuts Native (abre los .nup de Switch). Las tres aparecen en Emuladores al día.
- **Compatibilidad:** se conservan las vistas de dos pantallas, la configuración de la APK oficial, el actualizador y la firma de publicación.
- **Validación:** pruebas de diseño automatizadas; se recomienda además revisar en una consola Android real, especialmente tamaño de portadas y navegación.

---

# Díptico 2.0.0 — lanzamiento oficial

Díptico 2.0.0 oficial reúne Colección viva, el Home compacto de una pantalla y la recarga directa de las carpetas de juegos. Se distribuye con el paquete y la firma originales y puede actualizar la instalación oficial existente.

- **Inicio renovado para una pantalla:** carpetas y consolas visibles, juego destacado, biblioteca accesible y navegación táctil o con mando.
- **Actualizar juegos desde el inicio (↻):** botón pequeño en una y dos pantallas, Colección viva, widgets y modo TV para volver a leer las carpetas y descubrir juegos nuevos sin reiniciar Díptico. Durante el escaneo aparece un indicador y al terminar se informa el resultado.
- **Vitrina y biblioteca renovadas:** arte del juego protagonista, cajas 3D y navegación adaptada a una o dos pantallas.
- **Colección viva:** representación de consolas, carátulas persistentes, personalización y nuevas vistas.
- **Tu espacio:** páginas de widgets editables con controles de mando o táctiles, tamaños y fondos.
- **DIPPO, ajustes y temas:** acceso al asistente, selección de temas, widgets y música viva.
- **Compatibilidad:** APK para consolas Android y variante móvil. Se conserva el identificador de aplicación y se utiliza la misma firma de distribución configurada en GitHub.
- **Antes de instalar:** conserva una copia de seguridad de tus ajustes y datos. La fluidez, el comportamiento de emuladores y el modo de dos pantallas deben verificarse en una consola real.

## Detalle de cambios incluidos

- **Cuatro temas nuevos que cambian todo (pestaña Nuevos de la tienda):** Recortes (cartón, cinta adhesiva, lo elegido con línea de tijera, letra hecha a mano y sonidos de papel), Acuario de DIPPO (rayos de luz de mar, burbujas, DIPPO nadando al fondo, tarjetas redondas como gotas y sonidos de burbuja), Salón Recreativo (luces de marquesina que corren, líneas de pantalla, bordes de neón, letra de 8 bits en los títulos y sonidos de ficha) y Constelación (cielo con estrellas unidas por líneas de luz, estrella fugaz, letra fina y campanitas de cristal). Cada uno trae su fondo, forma de tarjetas, manera de marcar lo elegido, letra, decoración y sonidos.
- **Tienda con vista previa en cinco pantallas:** al elegir un tema, la otra pantalla lo muestra en Inicio, Biblioteca, Widgets, Menús y Ficha. Se cambia con L2/R2 o tocando las pestañas.
- **Probar 30 s:** con Y el tema se pone de verdad en las dos pantallas por 30 segundos y después vuelve el tuyo solo. A lo deja puesto; Y otra vez quita la prueba.
- **Personalizar:** con X partes desde el tema elegido en «Mezclar mi tema» (fondo, colores, decoración, sonidos).
- **Temas remasterizados (pestaña Remasterizados):** nueve clásicos rehechos completos (fondo, forma, letra, sonidos y música), cada uno con algo vivo: Nieve (se junta abajo y en el borde de arriba mientras no tocas nada, se sacude al moverte y la pantalla se empaña al minuto), Lava (sube y brilla con la temperatura real; con «muy caliente» late en rojo), Bosque (hojas y rayos de día, naranjo al atardecer, luciérnagas de noche, según la hora real), Medianoche (la luna con la fase real de esta noche y estrellas fugaces), Océano (la marea sigue la batería; con poca asoma la arena y al cargar suben burbujas), Sakura (los pétalos se juntan si te quedas quieto y el viento los barre), Portátil clásica (pantalla verde con rejilla que se pone pálida con poca batería), Atardecer (el cielo y el sol o la luna siguen la hora real) y Fogata (crece con tu racha de días; sin racha quedan brasas). Lo especial se puede cambiar por la decoración simple en Decoraciones.
- **Widgets nuevos (pestaña Widgets de la tienda):** DIPPO te propone (qué jugar según la hora; de noche, algo corto), Te extraña (un juego que dejaste hace más de dos semanas, con tu paso), Tu semana (barras por día, mejor día y racha), Tu equipo (batería, temperatura y espacio libre), Un recuerdo (lo que jugabas hace una semana, un mes o un año) y Radio del tema (pausa, otra parte y silencio por 1 hora). A los pone o los quita del inicio; la otra pantalla muestra el widget con tus datos.
- **Música viva:** las 13 canciones de Díptico rehechas desde cero, cada una en 4 partes (Tema, Vuelta, Brillo y Respiro) y en versión de día y de noche. Se encadenan en otro orden cada vez, de noche (20:00 a 7:00) suenan más suaves, en Ajustes se calman, cada 6 a 9 minutos hay un silencio de 15 a 30 s y cada 20 minutos cambian de tono.
- **Quitado:** las 13 canciones antiguas en bucle (las reemplaza la música viva).
- **Cambiado:** la tienda de temas tiene ocho pestañas: Nuevos, Remasterizados, Widgets, Incluidos, Calendario, Exclusivos, Mis temas y Clásicos (los temas originales tal como eran).
- **Qué probar en la Thor:** Ajustes › Estilo › Elegir tema › pestaña Nuevos; recorrer los 4 temas mirando arriba; L2/R2 para cambiar la vista; Y para probar uno 30 s (fijarse que vuelva solo); A para dejarlo; oír los sonidos al moverte y al abrir un juego; ver que los títulos con letra de 8 bits no se corten. Remasterizados: poner Nieve y dejar la consola quieta 2 minutos (debe juntarse nieve y empañarse) y luego mover la cruceta; poner Océano con poca batería y enchufarla; Lava después de jugar un rato. Widgets: ponerlos desde la tienda y mirar el inicio de widgets. Música: dejarla sonar 10 minutos y notar que cambia de parte y hace un descanso; probar el ⏭ y el «1 h» del widget Radio.

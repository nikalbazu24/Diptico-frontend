# Díptico · Plan de calidad después de 2.0.0

Este plan ordena las recomendaciones recogidas al preparar Díptico 2.0.0. La prioridad es publicar una versión estable, no agrandar la lista de funciones antes de tener una base sólida.

## Etapa 0 — Correcciones indispensables de 2.0.0

- [x] En «Tu espacio», ambos joysticks navegan por los widgets o la Galería sin desplazar el inicio y las carpetas detrás.
- [x] En «Editar espacio», los sticks controlan la pestaña activa; el menú del widget recibe navegación de opciones.
- [x] Contorno de selección y borde punteado con el mismo radio de esquina, sin marco cuadrado arriba y redondeado abajo.
- [ ] Pruebas automatizadas, compilación y capturas de la nueva candidata.
- [ ] Prueba en AYN Thor con dos pantallas y en modo de una pantalla: entrar/salir con L2/R2, recorrer widgets y Galería, editar, arrastrar, cambiar tamaño y volver al inicio.
- [ ] Validar el APK firmado y la actualización sin pérdida de preferencias antes de publicar en `main`.

## Etapa 1 — Velocidad y estabilidad (2.0.x)

**Meta:** navegación consistente y sin cierres inesperados, primero en AYN Thor y luego en equipos de una pantalla como Odin 2 Portal.

- Medir tiempos de entrada a biblioteca, cambio de consola, apertura de cajas 3D y Galería; registrar la referencia antes de optimizar.
- Evitar decodificación de arte, consultas de almacenamiento y red en el hilo de dibujo; limitar recomposiciones innecesarias.
- Cachear materiales 3D e imágenes ya descargados y acotar cachés según memoria disponible.
- Añadir pruebas de navegación, gestos y regresiones de una/dos pantallas. Sin alterar la apariencia aprobada por cambios de rendimiento.
- Verificar que volver de un emulador, suspender o cambiar de pantalla no duplique tareas ni deje controles activos.

**Criterio de cierre:** sin regresiones de navegación, compilación firmada correcta y comprobación real en ambas configuraciones.

## Etapa 2 — Coherencia de la experiencia visual (2.1)

- Definir radios, bordes, márgenes, densidades, tamaños táctiles y focos del mando como tokens compartidos.
- Revisar Biblioteca, Tu espacio, Ajustes, Tienda, DIPPO y ventanas, con vistas consistentes para una o dos pantallas.
- Eliminar opciones duplicadas o sin efecto solo después de identificar su configuración guardada y asegurar migración.
- Aprobación de bocetos antes de modificar pantallas completas.

## Etapa 3 — Biblioteca y cajas 3D inteligentes (2.1)

- Verificar detección por consola, formatos y rutas reales; no confundir ausencia de carátula con juego faltante.
- Dar prioridad al arte local y guardar sin conexión lo que el usuario descargó o construyó.
- Evitar sobrescribir cajas personalizadas y ofrecer opciones de respaldo o restauración.
- Previsualizaciones ligeras y manejo claro de errores de proveedores de imágenes.

## Etapa 4 — DIPPO útil, privado y ligero (2.1+)

- Contexto local opcional: juegos instalados, sesiones recientes y ajustes de la consola.
- Recomendaciones breves y acciones asistidas, sin capturas de pantalla permanentes ni monitoreo oculto.
- Consentimiento explícito antes de enviar datos o capturas a servicios externos; modo totalmente desactivable.
- Reducir consumo de CPU/batería cuando DIPPO esté inactivo.

## Etapa 5 — Actualizador, firmas y respaldo (transversal)

- Mantener firma de distribución, nombre y código de versión coherentes entre APK y release.
- Mostrar claramente si la actualización es candidata, beta o estable y qué datos se preservan.
- Probar instalación sobre versión anterior, copia/restauración de preferencias y fallos de descarga.
- Nunca publicar en `main` si falla una prueba crítica o la firma.

**Regla de trabajo:** una mejora por rama y PR, con prueba y notas de cambio; no eliminar ramas de respaldo ni publicar una versión estable sin confirmación de pruebas.

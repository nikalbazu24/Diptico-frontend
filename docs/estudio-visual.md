# Actualización Estudio visual

Base: main tras la corrección de la lupa de la beta 88, confirmada en la AYN Thor por Nicolás.

- Universo del juego: muestra local de 48 px, caché y lectura fuera del hilo de interfaz. Conserva el tema si no hay imagen. No captura el juego ni requiere servicios de pago.
- Habitación 2D: estantes de consolas, colecciones y recuerdos basados en los juegos más jugados; pared y objetos elegibles. Entrada por tacto o control en consolas.
- Colecciones: color, fondo, logo, música y dos disposiciones. La música se detiene al jugar. Imágenes y audio locales con acceso persistente cuando Android lo permite.
- Gesto en tarjeta explícita: fija el arte y datos del juego arriba mientras se recorre el estante abajo. También disponible en el menú con el control. No mueve ventanas de juegos Android.
- Estudio: editor existente con vista previa; tres perfiles de efectos, decoración y geometría de widgets. Al importar se valida formato y versión, se acotan tamaños y se pide confirmación. Los archivos compartidos omiten rutas, claves, biblioteca y accesos personales. Los medios de las colecciones se configuran por dispositivo.

## Correcciones

AyudaMenu ya no usa el valor cambiante de la opción como clave de transición: durante la descarga se actualiza el porcentaje sin reiniciar el fundido de toda la pantalla inferior. InferiorActivity solo cambia FLAG_NOT_FOCUSABLE cuando corresponde un cambio real del modo lupa.

## Validación

GitHub Actions ejecuta pruebas obligatorias de perfiles antes de compilar ambas variantes. Se agregan capturas para habitación, estudio y colección. La fluidez, el parpadeo físico y el selector de audio/imagen deben comprobarse en la Thor. Los archivos de la lupa y la detección de pantallas de la beta 88 no cambian.

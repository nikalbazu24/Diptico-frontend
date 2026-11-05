# Navegación y retoma

Base: beta 93 confirmada por el usuario como instalada. No se modifica la resolución ni la superficie de captura de la lupa.

La barra permanece fija abajo en el arte, y pasa arriba al entrar a Widgets. Se eliminan de Widgets los accesos superiores duplicados. El cajón tiene menú contextual, orden persistente, movimiento con cruceta o posición táctil y desinstalación mediante Android. Se recarga al volver al frente para reflejar apps eliminadas.

El pellizco distribuye los widgets en una nueva grilla. No oculta widgets por falta de espacio: aumenta las páginas. La distribución se comprueba con múltiples densidades y widgets.

R3 abre un selector con las vistas a la vista y previa. Galería, lista y carpetas reciben tarjetas renovadas; vista, filtro y orden están en el estante.

## Clip opcional

RetomaServicio usa una única MediaProjection/VirtualDisplay propia con Surface de un codificador H.264 (máximo 640×360, 15 fps, 800 kb/s). Guarda en memoria un máximo de 3 MiB y unos 6 segundos; cada MP4 comienza en un fotograma clave. No captura audio ni micrófono. Guarda un único clip privado en caché al detenerse; no se sube, comparte ni respalda. Android pide permiso antes de empezar. No se graba a la vez que la lupa; esta tiene prioridad. La grabación se detiene al bloquear, salir de la partida, detener desde la notificación o apagar el ajuste. No se reutilizan tokens de captura tras bloquear: el usuario debe conceder de nuevo el permiso para preparar otra retoma. Sin clip válido se muestra el arte del juego, con el último paso.

La reproducción usa TextureView en la pantalla libre y se cierra al tocar o tras unos segundos. No reemplaza la partida ni intenta restaurar un estado del emulador. Con una sola pantalla se conserva el aviso existente con imagen.

## Validación

Pruebas de distribución sin pérdidas ni superposiciones, límites/recorte de paquetes del clip y acorde Start + Y. Capturas de barra, widgets, galería, lista, carpetas, selector, menú de app y retoma sin clip. La captura H.264, el cierre físico de la Thor y el rendimiento de Genshin requieren validación en el equipo: no quedan demostrados por las capturas ni por compilar.

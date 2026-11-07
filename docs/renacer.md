# Rediseño de Díptico desde su identidad anterior

La revisión parte de la beta anterior a Renacer. Recupera su composición, sus recorridos y sus funciones completas. La pantalla superior presenta el juego seleccionado. La inferior contiene las carpetas y la biblioteca. Los widgets siguen siendo una vista propia y la barra flotante reúne los accesos.

## Cambios de la revisión

| Elemento | Rediseño |
|---|---|
| Escena del juego | Arte a todo el lienzo, título o logo, portada completa, tiempo de juego y próximo paso |
| Carpetas | Íconos de consola con su color, carátula personalizada o descargada, foco visible y nombre bajo el ícono |
| Biblioteca | Cabecera breve, búsqueda y ajustes agrupados, fondo tenue que acompaña al juego |
| Controles | Dock flotante con Jugar, ficha, búsqueda, apps, widgets, Bisa y ajustes |
| Una pantalla | Escena y biblioteca conviven en dos zonas, adaptadas a horizontal y vertical |
| Dos pantallas | La selección, las carpetas y las funciones continúan en el modelo completo de Díptico |

Se conservan los gestos de tamaño y dirección de las carpetas, la ventana ampliable con desenfoque de fondo, las vistas de juegos, los proveedores de imágenes, los widgets y su editor, Bisa, la tienda de temas integrada, los recursos de los temas y las decoraciones con sus opciones. El calendario mundial y chileno mantiene el comportamiento de la beta.

## Implementación

`ui/Redisenio.kt` contiene la nueva composición y sus componentes. `LocalRedisenio` permite activar la revisión y capturar una comparación con la interfaz anterior. La variante `renacer` conserva el identificador `cl.nikalbazu.diptico.renacer`, con el nombre visible **Díptico · Rediseño**, y usa las actividades principales, el estado y las integraciones completas existentes.

La revisión retira el núcleo y las pantallas del primer prototipo Renacer. La limpieza se limita a ese prototipo descartado. El trabajo de separar el coordinador completo por responsabilidades continúa como una tarea independiente de estabilización.

La nueva composición se activa en la edición de prueba. La beta pública mantiene su diseño mientras se revisa esta propuesta. La pantalla inferior tiene afinidad de tarea propia del paquete para que ambas instalaciones puedan convivir. El actualizador de la beta queda desactivado en la edición de prueba para que no ofrezca un APK de otro paquete.

## Instalación y datos

El APK usa la firma estable de distribución y GitHub Actions verifica su certificado. Puede actualizar la revisión Renacer con firma estable. Quien conserve una primera Renacer con firma de depuración necesita guardar un respaldo antes de reinstalar esa edición.

La revisión usa el tutorial, el respaldo y los permisos de carpetas de la aplicación completa. Los favoritos, notas y tiempos persistidos en el formato compartido siguen disponibles. Las preferencias específicas del prototipo descartado no definen la disposición nueva. Para traer datos desde la beta se utiliza su respaldo y se seleccionan las carpetas del dispositivo.

## Verificación

`CapturasRedisenio` genera vistas de la escena anterior, la nueva escena, las carpetas, la carpeta abierta, una pantalla horizontal, teléfono vertical, widgets, Chile y Navidad. Usa medios de ejemplo de Super Mario World y Pokémon Emerald exclusivamente en recursos de pruebas, con procedencia documentada en el README de esos recursos.

Las pruebas de navegación, estabilidad, perfiles y edición de widgets de la aplicación completa también se ejecutan en la variante de revisión. Las capturas comprueban la distribución de la interfaz. El rendimiento con bibliotecas grandes, los proveedores externos, los emuladores y el cambio físico de pantallas requieren comprobación en dispositivos.

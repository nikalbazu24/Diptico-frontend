# Material y densidades sobre la beta 153

La base es `c850a9e4cb6b69ab1267c7b6d633781d8b98086b`, publicada como v1.1-beta.153. Esta rama no incorpora el rediseño de Prueba Renacer, su identidad ni su configuración de arranque.

## Uso

En Ajustes → Apariencia, Modo Liquid Glass permite activarlo, desactivarlo o seguir el tema. Glass de carpetas y Glass de widgets se regulan independientemente de 0 a 100 %. Se guardan dentro de Experiencia y se incluyen en perfiles visuales. Los perfiles anteriores reciben los valores predeterminados sin cambiar posiciones ni juegos.

La biblioteca conserva su orden personalizado; Ordenar permite elegir A → Z o restaurar las familias del catálogo. Mantener una carpeta abre su personalización. Las portadas personales tienen prioridad sobre el arte vectorial incluido; Game Boy usa una creación propia.

Junta o separa dos dedos sobre los juegos. La previa responde durante el gesto y al soltar encaja en Compacto (80 %), Equilibrado (100 %), Amplio (120 %) o Detalle (140 %). Mantiene la vista y la selección, incluidos Carrusel, Abanico y Lomos. Las carpetas tienen cinco paradas (65, 100, 130, 160 y 190 %). Un dedo sigue desplazando y una cancelación descarta la previa.

## Implementación

- `Densidades`: paradas y límites compartidos entre gesto, ajustes y mando.
- `AjusteDensidad`: detector único que consume los toques después del segundo dedo y guarda solo al finalizar.
- `VentanaCarpeta`: mantiene la biblioteca montada durante entrada y salida.
- `FondoVentanaCristal`: captura local reducida, sin incluir el panel. Sirve como fondo difuminado y muestra alineada bajo el material. En widgets se comparte la captura de su fondo, sin procesar una imagen por tarjeta.
- `Desenfoque`: filtro separable con bordes extendidos y alfa premultiplicado. Compatible con Android 10 y 11.
- `Cristal`: color, contraste, velo, borde óptico y una ampliación sutil en el contorno. Es una aproximación visual propia en Android; no usa el renderizador de iOS.
- `Movimiento`: permite desactivar las animaciones nuevas al reducir movimiento o usar Ahorro.

No se crean buffers por fotograma. Las capturas se renuevan al abrir, cambiar de tamaño, fondo o nivel. El texto y las acciones se dibujan después del material. El contraste alto y Ahorro usan superficies sólidas.

## Revisión

Pruebas de persistencia, perfiles antiguos, límites, cambio de densidad sin perder selección, orden de carpetas, gestos reales con dos dedos, cancelación y desplazamiento de un dedo. Capturas en claro y oscuro, Android 10 y 14 y recorrido horizontal. La compilación en la rama de revisión verifica la firma de distribución sin publicar una beta en main.

## Editor y contenido de widgets

El apartado Widget separa Tamaño, Diseño y Más ajustes. Conserva posición, tamaños listos y libres, página, variantes, estilo, color, opacidad, esquinas, escala de contenido, copia de estilo y quitar. El catálogo filtra por nombre y descripción, incluidas fotos, GIF y vídeos propios. En móviles hay una vista previa visible.

`WidgetAdaptable` elige distribución y escala según ancho y alto reales; no depende solo del número de casillas. `TextoWidget` mide antes de dibujar, ajusta tipografía y permite dos líneas en etiquetas. Si un texto completo sigue sin caber, muestra ↗ y se puede abrir una lectura desplazable; el original no se recorta ni se modifica. Mantiene todas las preferencias de cada widget.

## Hora y batería

`Raiz` reserva una franja fuera de los paneles y del desenfoque. `BarraSistema` es su único propietario por pantalla activa. Las antiguas cabeceras, el centro y la sesión acompañante evitan repetir indicadores. El reloj a pantalla completa conserva su propia presentación. La barra de estado no depende del scroll, de la carpeta ni de la vista de juegos. Los widgets de reloj y batería elegidos por la persona siguen siendo contenido personalizable.

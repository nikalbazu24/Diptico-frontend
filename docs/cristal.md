# Díptico Cristal

## Dirección visual

Cristal combina superficies de lectura serenas y una capa funcional con volumen. El juego conserva la prioridad. La barra flotante, las ventanas y la navegación reciben el tratamiento óptico; las portadas y el texto siguen en una capa sencilla.

## Referencias oficiales consultadas

| Fuente | Principio que orienta la adaptación |
|---|---|
| [Materiales de las Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/materials) | Separar controles del contenido, elegir la densidad por función y preservar legibilidad |
| [Color en Liquid Glass](https://developer.apple.com/design/human-interface-guidelines/color) | Reservar el tinte fuerte para acciones y estados relevantes |
| [Meet Liquid Glass, WWDC25](https://developer.apple.com/videos/play/wwdc2025/219/) | Bordes ópticos, luz, profundidad, respuesta a la interacción y movimiento reducido |
| [Platforms State of the Union, WWDC26](https://developer.apple.com/videos/play/wwdc2026/102/) | Difundir fondos complejos y combinar bordes oscuros con reflejos claros |

La aplicación usa componentes propios de Android y Compose. Las referencias orientan la jerarquía, las formas y el tratamiento de la luz. No se incorporan componentes, iconos ni código de las interfaces de Apple.

## Materiales de la app

| Capa | Tratamiento |
|---|---|
| Controles flotantes | Contorno iluminado, sombra, reflejo y textura ambiental suave |
| Ventanas y navegación | Material más denso, borde definido y lectura estable |
| Widgets y contenido | Superficies casi opacas y bordes discretos; las imágenes conservan su aspecto |
| Acción principal | Tinte azul con una única jerarquía de acción |
| Fondo | Luz ambiental azul, menta y coral en modo claro y oscuro |

El tema cambia paleta, tipografía, geometría y acabados en toda la interfaz. La selección de juegos mantiene la cabecera, las portadas y una acción Jugar. Sus opciones permanecen en el menú secundario.

## Adaptación técnica y límites

`ui/Cristal.kt` define el material compartido. Se reutiliza una textura del arte de hasta 96 píxeles de lado, con una ampliación local leve. La luminosidad y su variación se muestrean una vez al cambiar la imagen. La densidad aumenta cuando el fondo tiene más detalle. Los contornos y los degradados se preparan en la caché de dibujo; no hay una captura completa de la pantalla por control ni una animación ambiental permanente.

La luz de respuesta observa el toque sin consumirlo. La preferencia de movimiento de Díptico y las animaciones desactivadas en Android reducen esa respuesta. El perfil de ahorro quita el muestreo, las sombras y la transparencia. El ajuste de texto de alto contraste del sistema, o una escala de fuente muy grande, usa superficies opacas y bordes contrastados.

Este renderizado aproxima el volumen y el comportamiento visual. No reproduce la refracción física por píxel de iOS ni captura el contenido arbitrario que pasa detrás de cada control. La apariencia clara u oscura sigue el modo elegido en Díptico. El material mantiene la legibilidad aunque no haya imágenes disponibles.

## Comprobación

Las pruebas verifican el contraste del texto en claro y oscuro sobre fondos extremos, la densidad al reducir efectos y el recorrido de opciones con toque y regreso mediante mando. Las capturas incluyen escena, biblioteca, selección, widgets, ajustes, modo claro, ahorro, alto contraste y Android 10. La respuesta y el coste del efecto requieren comprobación en consolas reales.

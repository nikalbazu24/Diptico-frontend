# Temas de la comunidad para Díptico

Los 30 temas del catálogo se exploran e instalan en **Ajustes → Temas → Tienda de temas**. Para probar un aporte de la comunidad, abre **Importar tema** y selecciona el JSON, o usa **Importar por enlace**.

Díptico muestra el nombre, el autor y los colores antes de instalar. Los temas de la tienda incluyen su música y se instalan sin conexión. Los aportes con un enlace de audio descargan su pista al instalarse. Para escucharla, activa la música según el tema en los ajustes de Música.

En **Ajustes → Temas → Mezclar mi tema**, elige un tema instalado por cada elemento que quieras combinar. La mezcla se guarda como **Mi tema**.

## Publica un tema

Genera el archivo en el [Estudio de temas de Díptico](https://diptico-temas.nickalbazu.chatgpt.site) o escribe el JSON con esta estructura. Pruébalo con **Importar tema** y comparte su enlace o su contenido al proponerlo en GitHub.

```json
{
  "id": "aurora",
  "nombre": "Aurora",
  "autor": "Tu nombre",
  "acento": 4287264767,
  "acento2": 4294933081,
  "acento3": 4289527039,
  "base": "auto",
  "forma": "cristal",
  "decor": "nada",
  "audio": "musica/aurora.ogg"
}
```

- Usa un identificador estable con letras, números, guiones o guiones bajos.
- Los tres acentos son colores ARGB opacos. Escríbelos en decimal.
- `base` acepta `auto`, `claro`, `oscuro` u `oled`. `forma` acepta `cristal`, `pixel` o el valor vacío.
- `audio` es opcional. Acepta una ruta relativa al JSON o un enlace HTTPS directo a MP3, OGG, M4A o WAV de hasta 16 MB. Comparte música con autorización para distribuirla.
- Puedes publicar hasta 50 temas juntos en un JSON con la estructura `{"temas": [...]}`. La lista debe contener los objetos completos de cada tema.

Los campos opcionales `descripcion`, `trama`, `acabado` y `pistaBase` definen la explicación, la textura, el estilo de los botones y la música integrada. También puedes usar `recta`, `bisel` o `capsula` en `forma`.

- `fuente` acepta `sans`, `serif`, `mono` y `pixel`. Si se omite, se conserva la tipografía de Díptico.
- `sonidos` acepta `bloques`, `retro` o `burbuja`. `tonoSonidos` ajusta el tono del paquete en semitonos de −6 a +6.
- `decor` también admite `muerdago`, `campanas`, `chupallas`, `volantines`, `monedas`, `esferas`, `reliquias`, `orbitas`, `destellos` y `anillos`.
- `intensidadDecoracion` acepta 1 (baja), 2 (media) o 3 (alta).

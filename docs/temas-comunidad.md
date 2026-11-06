# Temas de la comunidad para Díptico

Abre **Ajustes → Temas → Tienda de temas → Abrir enlace**. Pega la dirección HTTPS al archivo JSON del tema. También sirven los enlaces de GitHub que muestran el archivo.

Díptico muestra el nombre, el autor y los colores antes de instalar. Si el tema incluye música, se descarga al instalar. Para escucharla, activa la música según el tema en los ajustes de Música.

## Publica un tema

Guarda un archivo llamado `tema.json` con esta estructura y publícalo en tu repositorio o sitio web.

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

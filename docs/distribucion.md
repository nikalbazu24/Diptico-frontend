# Distribución y migración de Díptico

Las compilaciones de rama y de main son artefactos de prueba. La publicación requiere ejecutar manualmente «Construir APK» desde main con «publicar» y una clave privada de distribución. El flujo publica únicamente después de verificar los dos APK con la huella configurada.

Configura en GitHub Actions los secretos `DIPTICO_KEYSTORE_BASE64`, `DIPTICO_STORE_PASSWORD`, `DIPTICO_KEY_ALIAS` y `DIPTICO_KEY_PASSWORD`. Configura `DIPTICO_CERT_SHA256` como variable con la huella SHA-256 del certificado, 64 caracteres hexadecimales en minúsculas sin separadores. Guarda la clave privada y su contraseña fuera del repositorio, con una copia de recuperación. Usa la misma firma en cada versión futura. La firma de pruebas no sirve para distribución.

## Antes de publicar el primer APK firmado

- Con una instalación existente de beta 99, guarda un respaldo de Díptico en una carpeta compartida fuera de los datos de la app. Guarda por separado cualquier partida cuyo emulador dependa de permisos o carpetas privadas.
- En un equipo de pruebas o emulador, verifica el ZIP y restaura una copia en una instalación con la firma nueva. Comprueba temas, widgets, notas de voz, colecciones y tiempos. Vuelve a seleccionar las carpetas de juegos y a conectar servicios opcionales con tu cuenta.
- La nueva firma requiere una migración desde el APK anterior. Android puede rechazar la instalación encima. Desinstala la beta anterior solo después de verificar que tienes una copia restaurable y que los archivos necesarios están fuera de sus datos privados.
- Los APK firmados usan el canal 1.1 beta. El actualizador de la beta 99 conserva su canal 1.0, de modo que no ofrece un APK que Android rechazaría por firma diferente.

Las copias nuevas excluyen credenciales y permisos de carpetas. Trata las copias de las betas antiguas como privadas: pueden contener claves. La restauración guarda una copia previa en el almacenamiento privado de Díptico antes de cambiar preferencias y notas.

## Pruebas del candidato en equipo real

Prueba la instalación limpia y la restauración con datos de beta 99. Comprueba una consola de dos pantallas, una de una pantalla y un teléfono. Recorre la biblioteca con mando y toque, y comprueba inicio, volver del emulador, suspensión, SD sin permiso, captura denegada, lupa y clip en la Thor, y audio durante una sesión exigente. Verifica una partida .srm guardando la copia manual, exportándola a otro archivo e importándola en RetroArch según la ubicación configurada en ese equipo. Los APK de Actions se firman para pruebas y pueden requerir una instalación separada de tu configuración habitual.

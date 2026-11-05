# Distribución y migración de Díptico

Cada cambio de la app que llega a `main` ejecuta las pruebas y genera los dos APK con la firma de distribución. GitHub verifica ambos certificados, sube los APK y publica una beta `1.1` que puede detectar el actualizador. Los cambios de documentación y las ramas `fix/**` no publican versiones.

Configura en GitHub Actions los secretos `DIPTICO_KEYSTORE_BASE64`, `DIPTICO_STORE_PASSWORD`, `DIPTICO_KEY_ALIAS` y `DIPTICO_KEY_PASSWORD`. Configura `DIPTICO_CERT_SHA256` como variable con la huella SHA-256 del certificado, 64 caracteres hexadecimales en minúsculas sin separadores. Guarda la clave privada y su contraseña fuera del repositorio, con una copia de recuperación. Usa la misma firma en cada versión futura. La firma de pruebas no sirve para distribución.

## Configuración inicial desde Windows

Haz este paso una vez, en un PC con Java, PowerShell y GitHub CLI autenticado con una cuenta que pueda configurar el repositorio. Si Díptico ya tiene una firma privada, usa esa clave y omite la generación.

```powershell
$repo = "nikalbazu24/Diptico-frontend"
$firmaDiptico = Join-Path $env:LOCALAPPDATA "DipticoFirma"
New-Item -ItemType Directory -Force $firmaDiptico | Out-Null
$claveDiptico = Join-Path $firmaDiptico "diptico.p12"
if (-not (Test-Path $claveDiptico)) {
    keytool -genkeypair -keystore $claveDiptico -storetype PKCS12 -alias diptico -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=Diptico Release"
    if ($LASTEXITCODE -ne 0) { throw "No se pudo crear la clave" }
}
$certDiptico = Join-Path $firmaDiptico "diptico.cer"
keytool -exportcert -keystore $claveDiptico -alias diptico -file $certDiptico
if ($LASTEXITCODE -ne 0) { throw "No se pudo obtener el certificado" }
$huellaDiptico = (Get-FileHash -Algorithm SHA256 $certDiptico).Hash.ToLowerInvariant()
$huellaDiptico
```

Comprueba los valores en **Settings → Secrets and variables → Actions** antes de cargar nada. Si ya hay una huella, debe coincidir. Si hay secretos de una instalación anterior, verifica primero de qué clave vienen.

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes($claveDiptico)) | gh secret set DIPTICO_KEYSTORE_BASE64 --repo $repo
gh secret set DIPTICO_STORE_PASSWORD --repo $repo
gh secret set DIPTICO_KEY_PASSWORD --repo $repo
"diptico" | gh secret set DIPTICO_KEY_ALIAS --repo $repo
gh variable set DIPTICO_CERT_SHA256 --body $huellaDiptico --repo $repo
```

GitHub CLI pide la contraseña en la terminal. En el PKCS12 generado arriba, usa la misma contraseña en ambos secretos. Ejecuta los comandos uno por uno y detente si falla alguno. Haz una copia privada del archivo `diptico.p12` y conserva su contraseña en un gestor. La huella y el certificado público sirven para verificar el APK. La clave y las contraseñas se mantienen fuera de Git y del chat.

## Probar y publicar el mismo APK firmado

- Para preparar un borrador firmado sin publicar, ejecuta «Construir APK» manualmente desde `main` con `preparar_firma`. Descarga ese APK para probarlo.
- Con una instalación existente de beta 99, guarda un respaldo de Díptico en una carpeta compartida fuera de los datos de la app. Guarda por separado cualquier partida cuyo emulador dependa de permisos o carpetas privadas.
- En un equipo de pruebas o emulador, verifica el ZIP y restaura una copia en una instalación con la firma nueva. Comprueba temas, widgets, notas de voz, colecciones y tiempos. Vuelve a seleccionar las carpetas de juegos y a conectar servicios opcionales con tu cuenta.
- La nueva firma requiere una migración desde el APK anterior. Android puede rechazar la instalación encima. Desinstala la beta anterior solo después de verificar que tienes una copia restaurable y que los archivos necesarios están fuera de sus datos privados.
- Los APK firmados usan el canal 1.1 beta. El actualizador de la beta 99 conserva su canal 1.0, de modo que no ofrece un APK que Android rechazaría por firma diferente.

Las copias nuevas excluyen credenciales y permisos de carpetas. Trata las copias de las betas antiguas como privadas: pueden contener claves. La restauración guarda una copia previa en el almacenamiento privado de Díptico antes de cambiar preferencias y notas.

## Pruebas del candidato en equipo real

Prueba la instalación limpia y la restauración con datos de beta 99. Comprueba una consola de dos pantallas, una de una pantalla y un teléfono. Recorre la biblioteca con mando y toque, y comprueba inicio, volver del emulador, suspensión, SD sin permiso, captura denegada, lupa y clip en la Thor, y audio durante una sesión exigente. Verifica una partida .srm guardando la copia manual, exportándola a otro archivo e importándola en RetroArch según la ubicación configurada en ese equipo. Los APK de compilaciones de rama se firman para pruebas y pueden requerir una instalación separada de tu configuración habitual.

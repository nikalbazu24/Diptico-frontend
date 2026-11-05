# Reportes de Díptico en Google Drive

Díptico comparte la descripción, versión, modelo, Android y diagnóstico técnico cuando la persona confirma el envío. El destino recibe el reporte en Drive. El APK solo contiene la dirección de la aplicación web, sin correos ni credenciales de Google.

## Configurar el receptor una vez

1. Crea una carpeta privada en tu Drive. Copia su ID, el fragmento que aparece después de `/folders/` en la dirección.
2. Crea un proyecto independiente en [Google Apps Script](https://script.google.com/). Pega el contenido de [reportes-drive.gs](../tools/reportes-drive.gs) en el editor y guarda.
3. En **Configuración del proyecto → Propiedades del script**, añade `CARPETA_REPORTES` con el ID de la carpeta.
4. Elige **Implementar → Nueva implementación → Aplicación web**. Elige **Ejecutar como yo** y permite el acceso a **Cualquier persona**. Autoriza el acceso a tu Drive desde tu cuenta. La carpeta debe seguir privada.
5. Copia el enlace terminado en `/exec`. En el repositorio de Díptico, abre **Settings → Secrets and variables → Actions → Variables** y crea la variable de repositorio `DIPTICO_REPORTES_URL` con ese enlace.
6. Ejecuta **Construir APK** desde `main` con `preparar_firma` para que los APK incluyan el destino. Con la beta instalada, usa **Menú → Acerca de → Reportar un problema** y comprueba que aparece el archivo en Drive.

El receptor solo recibe reportes y devuelve un acuse con el mismo identificador. Los reintentos con el mismo identificador no crean archivos repetidos. El límite diario de 120 reportes protege la cuota de Drive frente a abuso de la aplicación web pública. Si el receptor está desactivado, la app permite copiar el diagnóstico para compartirlo por otro medio.

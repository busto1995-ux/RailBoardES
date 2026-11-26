# RailBoard ES

Aplicación Android (Kotlin + Jetpack Compose + Material 3) de teleindicador ferroviario para España.

## Cómo generar el APK sin instalar Android Studio (GitHub Actions)

1. Crea un repositorio nuevo (puede ser privado) en GitHub.
2. Sube el contenido de esta carpeta al repositorio (asegúrate de incluir la carpeta oculta `.github/workflows/build-apk.yml`).
3. Ve a la pestaña **Actions** en GitHub y abre la ejecución **Build Android APK** (o pulsa **Run workflow**).
4. Al terminar (~2-3 minutos), descarga el artefacto **RailBoardES-debug-apk** desde la parte inferior de la ejecución, descomprímelo y pasa `app-debug.apk` al móvil.

## Cómo generar el APK en local (Android Studio)

1. Abre esta carpeta en Android Studio.
2. Espera a que sincronice Gradle.
3. Ve a **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. El archivo quedará en `app/build/outputs/apk/debug/app-debug.apk`.

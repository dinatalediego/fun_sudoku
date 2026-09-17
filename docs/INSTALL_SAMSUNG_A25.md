# Instalar Fun Sudoku en Samsung Galaxy A25

Esta guía sirve para instalar la versión de prueba generada por GitHub Actions antes de publicar la app en Google Play.

## 1. Descargar el APK

1. En el Samsung A25 abre Chrome.
2. Entra al repositorio `dinatalediego/fun_sudoku` en GitHub.
3. Abre la pestaña **Actions**.
4. Entra a la ejecución más reciente de **Android CI** que tenga un check verde.
5. Baja hasta **Artifacts**.
6. Descarga `fun-sudoku-debug-apk`.
7. Android descargará un archivo ZIP.

## 2. Extraerlo

1. Abre **Mis archivos** de Samsung.
2. Ve a **Descargas**.
3. Toca el ZIP descargado y elige **Extraer**.
4. Dentro aparecerá `app-debug.apk`.

## 3. Instalarlo

1. Toca `app-debug.apk`.
2. Si Android bloquea la instalación, entra en **Configuración > Instalar apps desconocidas** cuando el sistema te lleve allí.
3. Permite temporalmente instalaciones desde **Mis archivos** o desde la app con la que abriste el APK.
4. Vuelve atrás y pulsa **Instalar**.
5. Abre **Fun Sudoku**.

Android 8.0 o superior concede este permiso por fuente concreta, no de forma global. Puedes desactivarlo nuevamente después de instalar.

## 4. Qué verás

La app tiene navegación inferior con:

- **Jugar**: Sudoku clásico, notas, pistas, errores, pausa y cronómetro.
- **Mis juegos**: historial de partidas terminadas, récords por dificultad, promedio y partidas perfectas.
- **Perfil**: preparado para identidad y sincronización con Google.

## 5. Actualizaciones mientras usemos APK de desarrollo

Los APK debug de CI son adecuados para pruebas. Antes de usar actualizaciones automáticas o publicar en Google Play se configurará una clave de firma de release estable. Si una futura compilación de prueba no puede instalarse encima de la anterior por firma diferente, desinstala la versión de prueba y vuelve a instalar el APK nuevo. La publicación final en Google Play eliminará esta fricción.

## 6. Login con Google

El login se implementará con Google OAuth + Supabase Auth. Las partidas seguirán guardándose localmente primero y, una vez iniciada sesión, se sincronizarán con el usuario autenticado. Esto evita que jugar dependa de la conexión a internet.

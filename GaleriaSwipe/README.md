# Galería Swipe 📸

App de Android para limpiar tu galería estilo Tinder.

- 👈 **Desliza a la izquierda** → la foto va a la **papelera** (todavía NO se borra).
- 👉 **Desliza a la derecha** → la foto **se queda**.
- ↻ **Deshacer** → regresa la última decisión.
- 🗑️ **Papelera** (ícono arriba a la derecha) → revisa las fotos que mandaste ahí,
  recupera las que se fueron por error y toca **Confirmar y eliminar** para borrarlas de verdad.

Tu progreso se guarda: si cierras la app, sigue donde te quedaste.

## Cómo instalarla en tu celular

1. En GitHub abre la pestaña **Releases** del repositorio (o **Actions** → la última
   ejecución de "Compilar APK Android" → artefacto `GaleriaSwipe-apk`).
2. Descarga `GaleriaSwipe.apk` desde el celular.
3. Ábrelo. Android te pedirá permitir "instalar apps desconocidas" para tu navegador: acéptalo.
4. Abre **Galería Swipe** y dale acceso a tus fotos.

> En Android 11 o superior, al confirmar el borrado el sistema muestra un último aviso
> "¿Permitir que Galería Swipe elimine estas fotos?". Es normal: es la protección de Android.

## Compilar tú mismo

Con Android Studio: abre la carpeta `GaleriaSwipe` y dale ▶️ Run.
Desde terminal (con el SDK de Android instalado): `./gradlew assembleDebug`.

Requiere Android 8.0 o superior.

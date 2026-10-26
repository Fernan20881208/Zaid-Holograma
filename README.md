# Holograma Zaid

App Android nativa para ejecutar `krissgamervipHoloo.sh` con root, copiarlo a `/data/local/tmp/krissgamervipHoloo.sh`, aplicarle permisos 755 y mostrar registros/terminal verbose.

## Compilar

1. Abre la pestaña **Actions**.
2. Ejecuta **Build Holograma Zaid APK** mediante **Run workflow** (o realiza un push a `main`).
3. Descarga el artefacto `Holograma-Zaid-debug-APK` e instala `app-debug.apk`.

## Requisitos

- Android 8.0 o posterior.
- Root funcional por KernelSU, APatch o Magisk con permiso concedido a la app.
- El script original usa el paquete `com.dts.freefireth` y su ruta privada `.pdcache`; el script se integra sin alterar su lógica.

La terminal permite comandos arbitrarios como root. Úsala solo con comandos que entiendas. El APK generado es de depuración, no firmado para publicación.
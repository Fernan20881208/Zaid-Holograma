# Holograma Zaid

App Android nativa para ejecutar `krissgamervipHoloo.sh` con root, copiarlo a `/data/local/tmp/krissgamervipHoloo.sh`, aplicarle permisos 755 y mostrar registros/terminal verbose.

## Compilar

1. Abre la pestaña **Actions**.
2. Ejecuta **Build Holograma Zaid APK** mediante **Run workflow** (o realiza un push a `main`).
3. Descarga el artefacto `Holograma-Zaid-debug-APK` e instala `app-debug.apk`.

## Permisos y comprobación

- Android 8.0 o posterior.
- Root funcional por KernelSU, APatch o Magisk. Al abrir, autoriza Holograma Zaid en el gestor root.
- En Android 11 o posterior, pulsa **Conceder acceso a todos los archivos** y activa el permiso especial en Ajustes > Acceso a todos los archivos.
- Pulsa **Probar almacenamiento mediante root** para ver en Registros si `su` puede listar `/storage/emulated/0` y comprobar la ruta privada que usa el script.
- El permiso «Todos los archivos» de Android cubre el almacenamiento compartido, pero por sí solo no permite a una app normal leer los datos privados de otras apps. Las operaciones privadas del script se intentan desde `su` y dependen de que el gestor root las autorice y de las restricciones del sistema.
- El script original espera el paquete `com.dts.freefireth` y su archivo `.pdcache` en la ruta configurada.

La terminal permite comandos arbitrarios como root. Úsala solo con comandos que entiendas. El APK generado es de depuración, no firmado para publicación.
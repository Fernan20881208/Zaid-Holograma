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

## Descarga de archivo desde GitHub Releases

La pantalla Inicio incluye **Descargar y verificar APK**. Usa este enlace directo y estable del recurso publicado en la release `Release`:

`https://github.com/Fernan20881208/Zaid-Holograma/releases/download/Release/split_asset_pack_install_time.apk`

Descarga el archivo al espacio privado de la app, calcula SHA-256 y usa root para copiarlo a:

`/data/local/tmp/split_asset_pack_install_time.apk`

Después se verifica el tamaño y, cuando `sha256sum` está disponible, el SHA-256 del destino. El recurso publicado actualmente tiene un tamaño aproximado de 295 MiB, por lo que la app necesita espacio libre suficiente tanto para la copia temporal como para el archivo de destino. La operación no copia el archivo a la carpeta privada de Free Fire ni ejecuta scripts automáticamente. Si el recurso se elimina, cambia de nombre o se retira la release, la descarga fallará; los errores aparecen en **Registros**.

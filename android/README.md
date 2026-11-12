# Tokkip Health para Android

La limpieza del teléfono de la familia Tokkip: lo que Healthy PC hace en la PC,
en Android (pensado para el Pixel, sin root). Spanish first (Mexican, de tú),
English too.

Herramientas:

- **Restos**: datos que dejaron apps que ya no están.
- **Limpiador del sistema**: archivos sueltos según filtros.
- **Cachés de apps**: con Shizuku, o con accesibilidad (Tokkip pulsa "Borrar caché" por ti).
- **Apps**: verlas, congelarlas, quitarlas.
- **Almacenamiento**: qué ocupa tu espacio.
- **Duplicados**, **comprimir fotos y videos**, **deslizar para ordenar**.
- **Programar** limpiezas.

## De dónde viene

Es un fork de [SD Maid SE](https://github.com/d4rken-org/sdmaid-se) de
Matthias Urhahn (darken), bajo GPL-3.0 (ver `LICENSE`): **su motor, nuestra
cara**. El código de las herramientas sigue siendo el suyo; el nombre, el
diseño, Tokkip y el español son de Tokkip Health. Su mascota, iconos,
animaciones, textos de tienda y traducciones no vienen: la licencia de SD Maid
SE los excluye.

Basado en d4rken-org/sdmaid-se@e759023 (2.2.2-rc0).

## Acceso

En un Pixel sin root, Android no deja tocar los datos de otras apps. Tokkip
Health usa:

- **Shizuku** (recomendado): permisos de ADB para cachés y `Android/data`.
- **Accesibilidad**: si no hay Shizuku, abre Ajustes y pulsa "Borrar caché" app por app.

## Compilar

```
gradle -p android assembleRelease
```

El APK se firma con la llave de Tokkip desde los secretos del CI
(`TOKKIP_KEYSTORE_B64`, `TOKKIP_KEYSTORE_PASSWORD`, `TOKKIP_KEY_ALIAS`); sin
ellos sale firmado con una llave de prueba.

Las versiones se publican en Andford/tokkiphealth con la etiqueta
`android-vX.Y.Z`: la app solo busca esas.

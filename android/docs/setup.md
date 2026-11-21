# Configuración y permisos

Ningún permiso es obligatorio, pero con cada uno Tokkip Health ve y limpia más.
En un Pixel sin root, lo mejor es **Shizuku** más el **almacenamiento**.

<a id="shizuku"></a>
## Acceso ADB (Shizuku)

Shizuku le presta a Tokkip Health los permisos de ADB sin root. Con él puede
borrar las cachés de todas las apps de un jalón (sin abrir Ajustes app por app)
y entrar a `Android/data` y `Android/obb`.

1. Instala [Shizuku](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api).
2. En Ajustes del teléfono › Información del teléfono, toca 7 veces **Número de compilación** para activar las Opciones de desarrollador.
3. En Opciones de desarrollador, activa **Depuración inalámbrica**.
4. Abre Shizuku › **Iniciar mediante depuración inalámbrica** y sigue el emparejamiento.
5. Vuelve a Tokkip Health › Configuración › Acceso ADB, y dale permiso.

Al reiniciar el teléfono Shizuku se apaga: ábrelo y vuelve a iniciarlo.

<a id="porter"></a>
### Porter

Porter es otra app que da acceso ADB como Shizuku. Si tienes las dos, Tokkip
Health espera a la que esté instalada con prioridad. Con una basta.

<a id="root-access"></a>
## Acceso root

Solo sirve si tu teléfono tiene root (Magisk, KernelSU…). Si tú no lo
modificaste, no tiene root: elige "Mi teléfono no tiene root".

<a id="manage-storage"></a>
## Administrar el almacenamiento

Deja ver y borrar archivos en el almacenamiento público (`Download/`, `DCIM/`…).
Casi todas las herramientas lo necesitan.

<a id="storage-access-framework"></a>
## Acceso a carpetas

En algunas versiones de Android, Tokkip Health pide entrar a carpetas concretas.
La ventana del sistema casi siempre abre en la carpeta correcta: solo toca
**Usar esta carpeta** y **Permitir**.

<a id="saf-open_document_tree-activitynotfoundexception"></a>
### No se abre la ventana de carpetas

Falta o está desactivada la app **Archivos** del sistema (Documents UI). Revisa
en Ajustes › Apps › Mostrar apps del sistema que "Archivos" esté activada.

<a id="usage-statistics"></a>
## Estadísticas de uso

Deja ver cuánto ocupan las cachés de cada app. Hace falta para limpiar cachés
con la accesibilidad.

<a id="notifications"></a>
## Notificaciones

Para avisarte qué está haciendo, de lo programado y de cuando te quedas sin
espacio.

<a id="app-inventory"></a>
## Lista de apps

Tokkip Health necesita saber qué apps tienes para reconocer los restos de las
que ya no están. Busca el permiso "Lista de apps" o "Consultar todos los paquetes".

<a id="accessibility-service"></a>
## Servicio de accesibilidad

Si no tienes Shizuku ni root, Tokkip Health usa la accesibilidad para abrir los
Ajustes de cada app y tocar **Borrar caché** por ti. Siempre ves un aviso
mientras lo hace y puedes cancelarlo. No lo usa para nada más ni junta datos.

<a id="acs-appops-restrictions"></a>
### "Ajustes restringidos"

Desde Android 13, las apps que no vienen de Google Play no pueden activar la
accesibilidad hasta que lo permitas: Ajustes › Apps › Tokkip Health › menú ⋮
(arriba a la derecha) › **Permitir ajustes restringidos**. Luego vuelve a
intentarlo.

<a id="acs-advanced-protection"></a>
### Protección avanzada

Con la Protección avanzada de Android activada no se puede usar la
accesibilidad. Usa Shizuku: hace lo mismo y más rápido.

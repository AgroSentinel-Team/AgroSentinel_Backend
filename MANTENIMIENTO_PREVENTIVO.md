# Mantenimiento preventivo

## Modelo

`PreventiveMaintenance` referencia un activo, el técnico asignado y, al ejecutar, la identidad autenticada del técnico. Registra además el índice de atención (`LOW`, `MEDIUM`, `HIGH`) y, si la programación parte de una alerta, el contexto predictivo recibido. Sus tareas y evidencias se guardan en `PreventiveMaintenanceTask` y `PreventiveMaintenanceEvidence` con relaciones uno-a-muchos. `MaintenanceHistory` contiene una referencia única al mantenimiento preventivo y su estado, fechas, técnico, repuestos, detalle de ejecución y firma.

Cada programación crea inmediatamente una entrada `SCHEDULED` en `maintenance_history`, por lo que ya aparece en la hoja de vida sin confundirse con un trabajo realizado. Esta entrada conserva el índice de atención y, cuando aplica, el contexto predictivo. La ejecución actualiza esa misma entrada (sin duplicarla) con el resultado, tareas, repuestos, observaciones, próxima fecha recomendada, firma y evidencias; el historial cronológico conserva la fecha programada y la fecha efectiva por separado. La hoja de vida presenta entradas preventivas y correctivas con su estado operativo.

`Category.maintenanceFrequencyDays` configura el ciclo preventivo de cada tipo de activo; su valor inicial es 90 días. Hibernate aplica el cambio de esquema según `spring.jpa.hibernate.ddl-auto=update`.

## API y autorización

- `GET /api/preventive-maintenances`: administrador obtiene todos los planes; técnico obtiene únicamente sus asignaciones.
- `GET /api/preventive-maintenances/{id}`: consulta detalle según el mismo alcance.
- `POST /api/preventive-maintenances`: solo administrador; JSON con `assetId`, `technicianId`, `scheduledAt`, `attentionIndex` opcional (`LOW`, `MEDIUM`, `HIGH`) y `predictiveContext` opcional (máximo 5000 caracteres).
- `PUT /api/preventive-maintenances/{id}/execution`: solo el técnico asignado; `multipart/form-data` con `data` (JSON de tareas, repuestos, trabajo, observaciones, resultado, próxima fecha y confirmación) y hasta 10 imágenes `evidencias`.

La frecuencia de categoría también está disponible en `GET /api/categories`; los selectores reutilizan los directorios existentes `/api/assets`, `/api/categories` y `/api/users`. Las evidencias admiten JPG, PNG y WEBP hasta 10 MB por archivo y se guardan en `MAINTENANCE_EVIDENCE_LOCATION` (por defecto `uploads/maintenance-evidence`). Tanto la API como las rutas de descarga requieren autenticación de administrador o técnico.

## Grupos y mantenimiento correctivo

Los administradores gestionan grupos desde `/api/work-groups`: `POST` crea, `PUT /{id}` edita nombre, descripción, líder, miembros y roles concedidos; `POST` y `DELETE /{id}/members/{userId}` agregan o retiran miembros; `PATCH /{id}/roles` reemplaza los roles del grupo, y `DELETE /{id}` lo desactiva lógicamente. El líder siempre debe pertenecer al grupo y no se puede retirar sin asignar primero otro líder. Un usuario puede pertenecer a varios grupos. Los roles efectivos combinan el rol individual y los concedidos por todos sus grupos activos; los roles administrativos no se pueden conceder desde un grupo. El inicio de sesión conserva `rol` y devuelve también `roles`.

`GET /api/work-groups/options` entrega los grupos activos disponibles para seleccionar una zona. Al crear una falla, `groupId` identifica el grupo: el administrador debe enviarlo; los demás usuarios solo pueden escoger uno de sus grupos (si tienen uno solo, se infiere). La falla queda asignada automáticamente, de forma round-robin, a un miembro activo con rol efectivo `TECNICO`. `GET /api/failure-reports` lista las fallas propias, las asignadas al técnico autenticado o todas para administradores. `POST /api/failure-reports/{id}/start-review` registra el inicio de revisión y `PUT /api/failure-reports/{id}/intervention` guarda la intervención; ambos requieren al técnico asignado. El técnico puede consultar la ficha técnica y hoja de vida del activo asociado con `GET /api/failure-reports/{id}/asset` o `GET /api/assets/{id}/resume`.

La respuesta del reporte incluye equipo, marca, modelo, número de serie o código de inventario, grupo, técnico y fechas para autollenar la identificación del formato. El técnico puede editar esos datos al enviar la intervención. Diagnóstico, causa raíz, tareas, repuestos (código, descripción, marca y cantidad), minutos trabajados, pruebas, estado final, recomendaciones, pendientes, técnico y receptor quedan guardados como una entrada correctiva en `GET /api/assets/{id}/resume`. Un equipo operativo o con restricciones cierra el reporte como `RESUELTO`; si queda inoperativo esperando repuestos, se conserva como `ESPERA_REPUESTO`.

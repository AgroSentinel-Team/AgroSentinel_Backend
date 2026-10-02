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

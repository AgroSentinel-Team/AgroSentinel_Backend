# Carga local del entorno ilustrativo

El cargador toma las 44 fichas de equipos identificadas en el informe técnico adjunto y crea datos ilustrativos para recorrer los módulos de usuarios, activos, grupos, mantenimiento, fallas, análisis predictivo e inventario. Los historiales, reportes, lecturas de sensores y existencias generados no son intervenciones ni mediciones reales. Las imágenes incluidas son ilustraciones de categoría, no fotografías de los activos.

La carga está desactivada por defecto. En Docker, `AGROSENTINEL_DEMO_DATA_ENABLED=true` activa el perfil `demo-data`; en otros lanzamientos se debe activar ese perfil y `agrosentinel.demo-data.enabled=true`. Por defecto acepta únicamente PostgreSQL local (`localhost`, `127.0.0.1` o `::1`). Una carga remota requiere además habilitarla explícitamente y autorizar el host y el nombre exactos de la base.

## Ejecutarla en una base local

Desde la carpeta del backend, configura `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` para una base local vacía y ejecuta:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo-data" "-Dspring-boot.run.arguments=--agrosentinel.demo-data.enabled=true --spring.docker.compose.enabled=false"
```

La carga no elimina usuarios ni otros datos existentes. Crea o actualiza únicamente las cuentas listadas abajo y evita duplicar el catálogo cuando ya encuentra el código `25530003001`. El informe repite el código `228600105001` en fichas distintas; para respetar la restricción de unicidad del inventario, el código queda sin asignar a la motosierra STIHL hasta que se verifique. Para repetir la carga desde cero, usa una base local nueva; no borres tablas de una base con datos de operación.

## Carga remota de una sola vez

Solo después de confirmar el destino, usa las propiedades `agrosentinel.demo-data.allow-remote=true`, `agrosentinel.demo-data.allowed-remote-host` y `agrosentinel.demo-data.allowed-remote-database` con el host y la base exactos. La aplicación valida estos valores antes de conectarse. En una base remota, cada cuenta nueva recibe una contraseña distinta derivada con HMAC-SHA256 de una clave privada de al menos 32 bytes y su correo. La clave no se guarda en la base ni en el repositorio. Usa `tools/Get-DemoDataCredentials.ps1` localmente para derivar las credenciales; no habilites su emisión en logs de despliegue. Las contraseñas fijas de la tabla siguiente son exclusivamente para desarrollo local.

## Activar la carga durante el despliegue en Render

El `render.yaml` del frontend solo despliega el sitio estático. Para el servicio backend de Render, configura en su panel las variables:

| Variable | Valor |
|---|---|
| `AGROSENTINEL_DEMO_DATA_ENABLED` | `true` |
| `AGROSENTINEL_DEMO_DATA_ALLOW_REMOTE` | `true` |
| `AGROSENTINEL_DEMO_DATA_ALLOWED_REMOTE_HOST` | `aws-0-us-west-1.pooler.supabase.com` |
| `AGROSENTINEL_DEMO_DATA_ALLOWED_REMOTE_DATABASE` | `postgres` |
| `AGROSENTINEL_DEMO_DATA_PASSWORD_KEY` | Clave aleatoria de al menos 32 bytes, configurada como secreto |

Con el flag activo, el contenedor habilita el perfil `demo-data` y carga durante el arranque. La operación valida host y base antes de conectarse, no elimina tablas y se detiene si encuentra datos existentes sin el activo marcador. En reinicios o despliegues posteriores, si el marcador ya existe, no modifica usuarios ni vuelve a insertar registros.

Genera la clave localmente y guárdala en el panel de Render como secreto, sin compartirla ni agregarla al repositorio:

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Después del despliegue, deriva y guarda las credenciales localmente, fuera del repositorio:

```powershell
.\tools\Get-DemoDataCredentials.ps1 | Set-Content -Path "$env:USERPROFILE\demo-credentials.csv" -Encoding UTF8
```

## Cuentas locales

Se crean 1 administrador, 5 técnicos, 5 supervisores, 20 operarios de campo y 3 almacenistas. Los nombres son generados para la demostración.

| Grupo | Correo | Contraseña |
|---|---|---|
| Administrador | `admin1@gmail.com` | `admin1` |
| Técnicos | `tecnico1@gmail.com` a `tecnico5@gmail.com` | `tecnico1` a `tecnico5` |
| Supervisores | `supervisor1@gmail.com` a `supervisor5@gmail.com` | `supervisor1` a `supervisor5` |
| Operarios | `operario1@gmail.com` a `operario20@gmail.com` | `operario1` a `operario20` |
| Almacenistas | `almacenista1@gmail.com` a `almacenista3@gmail.com` | `almacenista1` a `almacenista3` |

Estas contraseñas son débiles y solo deben usarse en una base local aislada. No las despliegues ni reutilices en producción.

## Imágenes persistentes

En Docker Compose las evidencias de mantenimiento y las imágenes de activos usan volúmenes nombrados. En Render u otro proveedor con filesystem efímero, monta un disco persistente en `/app/uploads` y conserva `ASSET_IMAGE_LOCATION=/app/uploads/asset-images`, `MAINTENANCE_EVIDENCE_LOCATION=/app/uploads/maintenance-evidence` y `FAILURE_EVIDENCE_LOCATION=/app/uploads/failure-evidence`.

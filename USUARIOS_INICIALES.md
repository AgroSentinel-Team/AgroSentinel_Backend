# Usuarios iniciales de AgroSentinel

Este es el listado de las 34 cuentas definidas para la carga inicial. Su existencia en la base depende de que el cargador haya terminado correctamente durante el despliegue.

Las contraseñas no se incluyen en este archivo para evitar subir credenciales al repositorio. En modo remoto seguro se derivan de `AGROSENTINEL_DEMO_DATA_PASSWORD_KEY`. Para una instancia de pruebas se puede habilitar explícitamente `AGROSENTINEL_DEMO_DATA_ALLOW_WEAK_REMOTE_PASSWORDS=true`; esto establece contraseñas `tecnico1`, `supervisor1`, `operario1`, `almacenista1`, etc. No habilites esa opción en una instancia pública u operativa.

## Administrador

| Nombre | Correo | Rol |
|---|---|---|
| Administrador de AgroSentinel | `admin1@gmail.com` | Administrador |

## Técnicos

| Nombre | Correo | Rol |
|---|---|---|
| Juan David Rojas | `tecnico1@gmail.com` | Técnico |
| Camilo Andrés Pardo | `tecnico2@gmail.com` | Técnico |
| Santiago Muñoz | `tecnico3@gmail.com` | Técnico |
| Felipe Cárdenas | `tecnico4@gmail.com` | Técnico |
| Nicolás Vargas | `tecnico5@gmail.com` | Técnico |

## Supervisores

| Nombre | Correo | Rol |
|---|---|---|
| Laura Marcela Gómez | `supervisor1@gmail.com` | Supervisor |
| Diana Carolina Salazar | `supervisor2@gmail.com` | Supervisor |
| Andrés Felipe Torres | `supervisor3@gmail.com` | Supervisor |
| María Camila Peña | `supervisor4@gmail.com` | Supervisor |
| Jorge Esteban Molina | `supervisor5@gmail.com` | Supervisor |

## Operarios de campo

| Nombre | Correo | Rol |
|---|---|---|
| Carlos Eduardo Ramírez | `operario1@gmail.com` | Operario de campo |
| Miguel Ángel Ortiz | `operario2@gmail.com` | Operario de campo |
| José Manuel Rincón | `operario3@gmail.com` | Operario de campo |
| Luis Fernando Castro | `operario4@gmail.com` | Operario de campo |
| Daniel Alejandro Suárez | `operario5@gmail.com` | Operario de campo |
| Óscar Mauricio Díaz | `operario6@gmail.com` | Operario de campo |
| Diego Alejandro Reyes | `operario7@gmail.com` | Operario de campo |
| Julián Andrés Herrera | `operario8@gmail.com` | Operario de campo |
| Kevin Santiago León | `operario9@gmail.com` | Operario de campo |
| Brayan Steven Cruz | `operario10@gmail.com` | Operario de campo |
| Héctor Fabio Medina | `operario11@gmail.com` | Operario de campo |
| Jhon Jairo Castaño | `operario12@gmail.com` | Operario de campo |
| Andrés Mauricio Silva | `operario13@gmail.com` | Operario de campo |
| Wilson Javier Mora | `operario14@gmail.com` | Operario de campo |
| Sebastián Ospina | `operario15@gmail.com` | Operario de campo |
| Edwin Alexander Gil | `operario16@gmail.com` | Operario de campo |
| Mateo Nicolás Parra | `operario17@gmail.com` | Operario de campo |
| Cristian Camilo Rojas | `operario18@gmail.com` | Operario de campo |
| Javier Orlando Peña | `operario19@gmail.com` | Operario de campo |
| Álvaro José Trujillo | `operario20@gmail.com` | Operario de campo |

## Almacenistas

| Nombre | Correo | Rol |
|---|---|---|
| Mónica Patricia Arias | `almacenista1@gmail.com` | Almacenista |
| Paola Andrea Lozano | `almacenista2@gmail.com` | Almacenista |
| Ricardo Alonso Vera | `almacenista3@gmail.com` | Almacenista |

## Seguridad de las credenciales

La clave privada de `AGROSENTINEL_DEMO_DATA_PASSWORD_KEY` no debe incluirse en commits, documentos ni logs. Si se compartió o se guardó en un archivo versionado, considérala expuesta: genera una clave nueva, actualiza el secreto del servicio backend y evita reutilizar la anterior.

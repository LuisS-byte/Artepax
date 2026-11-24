# Base de demostración

`init.sql` inicializa una base vacía en PostgreSQL 18. Incluye estructura y catálogos, productos, decoraciones, asociaciones, parámetros y plantillas de demostración. La tabla de usuarios comienza vacía.

Docker ejecuta el script una vez al inicializar el volumen `pgdata`. Para una restauración manual en una base **desechable y vacía**:

```sh
psql -X -v ON_ERROR_STOP=1 -d artepax -f database/init.sql
psql -X -v ON_ERROR_STOP=1 -d artepax -f database/verify.sql
```

En Windows usa `psql.exe` en PATH o indica su ruta. Configura los parámetros de conexión del cliente antes de ejecutar los comandos. Corre `verify.sql` antes de crear usuarios o utilizar la demo.

## Crear el primer administrador en Compose

Instala BCrypt en un entorno virtual local y ejecuta el asistente. Introduce un correo y una contraseña privada de 12 a 72 bytes. El asistente calcula el hash localmente y usa la secuencia de la tabla junto con el rol `ADMIN` del catálogo.

| Linux o macOS | Windows PowerShell |
| --- | --- |
| `python3 -m venv .venv` | `python -m venv .venv` |
| `.venv/bin/python -m pip install bcrypt==4.3.0` | `.\.venv\Scripts\python.exe -m pip install bcrypt==4.3.0` |
| `.venv/bin/python database/bootstrap_admin.py` | `.\.venv\Scripts\python.exe database/bootstrap_admin.py` |

Usa `POST /auth/login` y pega el JWT en el botón **Authorize** de Swagger si el documento OpenAPI lo ofrece, o envíalo en la cabecera `Authorization: Bearer <TOKEN>`. El asistente requiere el servicio `db` de Compose en ejecución.

Consulta [el modelo de datos](../docs/database.md) y [la guía de restauración](../docs/deployment.md). Los dumps y respaldos privados se conservan fuera del repositorio.

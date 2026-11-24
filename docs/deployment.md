# Operación y despliegue

## Demo local

Sigue [la configuración de Compose](setup.md). El backend y Mailpit publican sus interfaces solo en loopback. La base se comunica por la red interna de Compose. Los volúmenes `pgdata` y `documents` conservan el contenido de PostgreSQL y los PDF. La aplicación ejecuta sus tareas programadas con los parámetros del seed y dirige el SMTP al buzón de captura.

- `docker compose ps` muestra los servicios.
- `docker compose logs app` permite revisar el arranque.
- `docker compose stop` detiene los contenedores y conserva los volúmenes.
- `docker compose up -d` vuelve a iniciar los servicios.
- Para probar desde cero, utiliza otra copia de trabajo y un nombre de proyecto Compose distinto. Ajusta los puertos si la primera instancia sigue activa.

## Respaldo y restauración

Detén escrituras antes de capturar PostgreSQL y los PDF. El siguiente ejemplo para Bash escribe un dump personalizado **fuera del repositorio**. En PowerShell 7.4 o posterior, usa la misma redirección binaria con una ruta de salida privada.

```sh
docker compose exec -T db sh -ec 'pg_dump -Fc -U "$POSTGRES_USER" -d "$POSTGRES_DB"' > ../artepax-private.dump
```

Guarda una copia privada del volumen de documentos junto con el dump y registra la revisión de la aplicación. Restaura primero en una **base vacía distinta** con `pg_restore --exit-on-error --no-owner --no-privileges -d <base_aislada> <dump>`. Valida el arranque y los datos antes de configurar el backend para usarla. `docker compose down` conserva los volúmenes. La variante con `--volumes` elimina los datos del proyecto Compose seleccionado.

## Arquitectura cloud propuesta

Una futura implementación puede usar un proxy HTTPS, contenedores privados y PostgreSQL gestionado con red restringida. Requeriría una solución de almacenamiento persistente compatible con los PDF, un gestor de secretos, un SMTP aprobado, registros centralizados, respaldos verificados y recuperación probada. Los costes quedan por estimar según proveedor, región y uso. La demo documentada es local.

Para revertir cambios del repositorio, crea commits de reversión en orden inverso con `git revert <sha>`. Una reversión de Git no restaura los datos de un volumen. Para el entorno, vuelve a la imagen anterior y utiliza el respaldo compatible si hubo cambios en el esquema.

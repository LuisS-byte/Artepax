# Configuración local

## Requisitos

Instala un JDK 21 y prepara una instancia PostgreSQL para desarrollo con una cuenta autorizada. El repositorio incluye Maven Wrapper. Docker se usa en la prueba de concurrencia basada en Testcontainers.

```sh
git clone https://github.com/LuisS-byte/Artepax.git
cd Artepax
```

**Bloqueo de arranque desde una base vacía.** La plantilla usa `spring.jpa.hibernate.ddl-auto=validate`. El repositorio revisado carece de scripts de inicialización y migraciones Flyway o Liquibase. Antes del arranque, consigue del equipo un esquema compatible con las entidades, las vistas `vista_stock_producto` y `vista_pagos_evento` y los datos de referencia que requieren los flujos, como estados de evento y reserva, roles y parámetros de configuración. La URL de la plantilla apunta a `jdbc:postgresql://localhost:5432/Artepax`.

## Variables del proceso

Usa [la plantilla de entorno](../.env.example) como referencia. Spring Boot no carga automáticamente el archivo `.env` en este proyecto. Inyecta los valores mediante el entorno de ejecución del IDE o de la terminal y evita escribir credenciales en comandos que queden en el historial.

| Variable | Uso | Plantilla |
| --- | --- | --- |
| `DB_USER` | Usuario PostgreSQL | Requerida |
| `DB_PASS` | Contraseña PostgreSQL | Requerida |
| `GOOGLE_CLIENT_ID` | Identificador de cliente para validar ID tokens | Requerida |
| `JWT_SECRET` | Clave de firma JWT | Requerida |
| `MAIL_USERNAME` | Cuenta SMTP y remitente | Requerida |
| `MAIL_PASSWORD` | Credencial SMTP | Requerida |
| `FRONTEND_URLS` | Orígenes permitidos por CORS | Opcional, con valores locales por defecto |

Compara tu configuración con [application.properties.example](../src/main/resources/application.properties.example). La revisión detectó que `application.properties` ya está versionado, aunque `.gitignore` enumera ese archivo. Conserva el archivo versionado y proporciona las credenciales mediante las variables externas. Para una personalización local puedes preparar una copia de la plantilla en una ubicación externa al repositorio y usarla como configuración adicional de Spring Boot. `app.mail.from-name` tiene un valor distinto en el archivo versionado y en la plantilla. Revisa ese ajuste si necesitas controlar el nombre visible del remitente.

## Arranque

| Entorno | Comando |
| --- | --- |
| Linux o macOS | `./mvnw spring-boot:run` |
| Windows PowerShell | `.\mvnw.cmd spring-boot:run` |

Con la configuración de referencia, el puerto es 8080 y puedes consultar [Swagger UI](http://localhost:8080/swagger-ui/index.html) o el [JSON OpenAPI](http://localhost:8080/v3/api-docs).

- Si falla la conexión a PostgreSQL, comprueba el servicio, el host, el puerto, el nombre de la base y la cuenta.
- Si la validación de esquema falla, compara las tablas, columnas y vistas con la versión del backend.
- Si falta un placeholder, revisa las variables del proceso que ejecuta Maven.
- Si la API inicia y el correo falla, verifica la configuración SMTP en el entorno de desarrollo. Un documento puede quedar registrado como `FALLIDO`.

La guía se verificó contra el repositorio. El arranque y el envío de correo quedan por comprobar en un entorno autorizado.

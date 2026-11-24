# Configuración local

## Inicio rápido con Docker Compose

Necesitas Docker con Compose v2 y, para crear el administrador, Python 3 con el paquete BCrypt. Desde la raíz del repositorio, copia la plantilla una sola vez:

| Sistema | Comando |
| --- | --- |
| Linux o macOS | `cp -n .env.example .env` |
| Windows PowerShell | `if (-not (Test-Path .env)) { Copy-Item .env.example .env }` |

Edita `.env` y completa `DB_PASS` y `JWT_SECRET` con valores distintos y privados. Si tienes OpenSSL, puedes generar una contraseña con `openssl rand -hex 24` y la clave JWT con `openssl rand -base64 32`. En Windows y Linux también puedes usar Python con `python -c "import secrets; print(secrets.token_hex(24))"` y `python -c "import base64,secrets; print(base64.b64encode(secrets.token_bytes(32)).decode())"`. Guarda los valores solo en tu archivo local y evita mostrarlos en capturas o logs.

```sh
docker compose config --quiet
docker compose up --build --wait --wait-timeout 240
```

Crea un administrador siguiendo [la guía de base de datos](../database/README.md). Abre [Swagger UI](http://localhost:8080/swagger-ui/index.html), [OpenAPI](http://localhost:8080/v3/api-docs) o [Mailpit](http://localhost:8025). Los puertos se pueden ajustar en `.env`. PostgreSQL queda disponible dentro de Compose y el SMTP de la demo apunta a Mailpit para capturar los mensajes localmente.

| Variable | Uso |
| --- | --- |
| `DB_USER`, `DB_PASS` | Cuenta y contraseña de la base de demo |
| `JWT_SECRET` | Clave Base64 de al menos 32 bytes antes de codificar |
| `GOOGLE_CLIENT_ID` | Audiencia de Google. El valor ficticio permite el login local |
| `FRONTEND_URLS` | Orígenes permitidos por CORS |
| `APP_PORT`, `MAILPIT_PORT` | Puertos locales del backend y del buzón de prueba |
| `DB_URL`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Ajustes cuando se ejecuta Java fuera de Compose |

## Ejecución directa con Maven

Usa Java 21, el Maven Wrapper y PostgreSQL 18. Restaura [el SQL de inicialización](../database/init.sql) en una base desechable vacía. El backend mantiene `spring.jpa.hibernate.ddl-auto=validate` y requiere las vistas, catálogos y restricciones que define el SQL. La URL JDBC por defecto es `jdbc:postgresql://localhost:5432/Artepax`. Puedes reemplazarla con `DB_URL`.

Inyecta las variables desde tu IDE o el entorno de la terminal. Compose lee `.env` para interpolar sus valores y Spring Boot usa el entorno del proceso cuando se inicia directamente. Para probar documentos fuera de Compose, configura un SMTP de captura con `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` y los ajustes de autenticación y TLS adecuados a ese servidor.

| Sistema | Comando |
| --- | --- |
| Linux o macOS | `bash ./mvnw spring-boot:run` |
| Windows PowerShell | `.\mvnw.cmd spring-boot:run` |

Consulta [operación y restauración](deployment.md) antes de usar una base que ya tenga datos.

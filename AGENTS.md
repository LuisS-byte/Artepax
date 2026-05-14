# Artepax

- Stack: single-module Spring Boot 3.5.14 on Java 21, built with the Maven wrapper in repo root. Use `./mvnw` on Unix-like shells or `./mvnw.cmd` on Windows.

## Commands

- Run the app: `./mvnw spring-boot:run`
- Run all tests: `./mvnw test`
- Run the only current test class: `./mvnw -Dtest=ArtepaxApplicationTests test`
- There is no repo-local lint, formatter, typecheck, CI workflow, Flyway, or Liquibase config to fall back on; Maven tests are the only checked-in automation.

## Startup And Test Preconditions

- `src/main/resources/application.properties` points to PostgreSQL at `jdbc:postgresql://localhost:5432/Artepax` and uses env placeholders for `DB_USER`, `DB_PASS`, `GOOGLE_CLIENT_ID`, `JWT_SECRET`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and optional `FRONTEND_URLS`.
- `src/main/resources/application.properties.example` is the checked-in reference for required config.
- Spring Boot is not configured here to import a root `.env` file. A verified `./mvnw -Dtest=ArtepaxApplicationTests test` run failed with `${DB_USER}` still unresolved, so export env vars in the shell before running Maven.
- `spring.jpa.hibernate.ddl-auto=validate` is enabled and there are no migration files in the repo. The schema must already exist and match the entities before the app or tests will start.

## Code Layout

- Main entrypoint: `src/main/java/com/catolica/Artepax/ArtepaxApplication.java`.
- The codebase is organized by feature under `src/main/java/com/catolica/Artepax`: `authentication`, `clientes`, `cronograma`, `documento`, `evento`, `inventario`, `notificacion`, `pago`, with shared cross-cutting code in `common`.
- If architecture is unclear, read feature service implementations before leaf entities; most business rules live there.

## Security And API Shape

- `authentication/security/SecurityConfig.java` only permits `/auth/**`, `/error`, `/swagger-ui/**`, `/swagger-ui.html`, and `/v3/api-docs/**`; everything else is authenticated.
- Observed controllers under `/api/**` use class-level `@PreAuthorize("hasRole('ADMIN')")`. Check the controller before assuming a route is public.
- Authentication is Google-login-to-JWT: `authentication/auth` handles Google auth, `authentication/security/JwtAuthenticationFilter.java` applies the token, and `authentication/user/UsuarioService.java` is the `UserDetailsService`.

## Business Logic Gotchas

- Scheduling is live. `ArtepaxApplication` enables scheduling, and `common/job/*` contains background jobs for reminders, overdue quotes, and automatic cancellation processing. Read those jobs before changing event, notification, or configuration flows.
- Event state transitions and their side effects are centralized in `evento/service/impl/EventoServiceImpl.java`; changing event status logic without reading that file is risky because it coordinates inventory and payment behavior.
- `documento/service/impl/DocumentoServiceImpl.java` intentionally persists a generated `Documento` even if email sending fails: `generarDocumento` uses `@Transactional(noRollbackFor = RuntimeException.class)` and records `FALLIDO` for retry instead of rolling back.

## Local Guidance

- Repo-local Java guidance lives in `.agents/skills/java-coding-standards/SKILL.md`, `.agents/skills/java-docs/SKILL.md`, and `.agents/skills/java-springboot/SKILL.md`. Follow those when editing Java code or reviewing API/service changes.

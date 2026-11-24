# Artepax

- Stack: single-module Spring Boot 3.5.14 on Java 21, built with the Maven wrapper in repo root. Use `./mvnw` on Unix-like shells or `./mvnw.cmd` on Windows.

## Commands

- Run the app: `./mvnw spring-boot:run`
- Run all tests: `./mvnw test`
- Run a specific test class: `./mvnw -Dtest=ClassName test`
- CI runs the Maven suite, Testcontainers integration tests and an isolated Compose smoke test.

## Startup And Test Preconditions

- `DB_URL` overrides the local PostgreSQL JDBC default; see `docs/setup.md` for required variables.
- `src/main/resources/application.properties` is tracked even though `.gitignore` lists it. `src/main/resources/application.properties.example` is the reference template. Supply local values through the process environment.
- `.env.example` documents the environment variables. Spring Boot does not automatically import a local `.env` file. Export or inject the variables before running Maven.
- `ddl-auto=validate` requires a compatible database. `database/init.sql` initializes a fresh PostgreSQL 18 database or an empty Compose volume.

## Test Profiles And Testcontainers

- Unit tests (plain JUnit 5 + Mockito with `@ExtendWith(MockitoExtension.class)`) do not need a database.
- `ArtepaxApplicationTests` is a plain `@SpringBootTest(contextLoads)` — it WILL fail without a running PostgreSQL and exported env vars.
- Integration tests follow the `*IT.java` naming convention and use Testcontainers (`@Testcontainers(disabledWithoutDocker = true)` + `@Container PostgreSQLContainer`). They skip gracefully when Docker is unavailable.
- The `test` profile (`src/test/resources/application-test.properties`) sets `ddl-auto=create-drop`, disables scheduling (`spring.task.scheduling.enabled=false`), and provides dummy credentials for Google, JWT, and email — so integration tests are self-contained.
- Run integration tests specifically with `./mvnw -Dtest="*IT" test` (Docker required).

## Code Layout

- Main entrypoint: `src/main/java/com/catolica/Artepax/ArtepaxApplication.java`. It enables scheduling and `@ConfigurationPropertiesScan`.
- The codebase is organized by feature under `src/main/java/com/catolica/Artepax`: `authentication`, `clientes`, `comunicacion`, `cronograma`, `documento`, `evento`, `inventario`, `notificacion`, `pago`, with shared cross-cutting code in `common`.
- If architecture is unclear, read feature service implementations before leaf entities; most business rules live there.
- `@ConfigurationProperties(prefix = "app.mail")` is used for type-safe property binding (see `comunicacion/config/MailProperties.java`).

## Security And API Shape

- `authentication/security/SecurityConfig.java` only permits `/auth/**`, `/error`, `/swagger-ui/**`, `/swagger-ui.html`, and `/v3/api-docs/**`; everything else is authenticated.
- Most `/api/**` controllers require `ADMIN` through `@PreAuthorize`. `GET /api/usuarios/me` requires authentication without a method-level role restriction.
- `authentication/auth` issues JWTs through local email/password login and Google ID token login, `authentication/security/JwtAuthenticationFilter.java` applies the token, and `authentication/user/UsuarioService.java` is the `UserDetailsService`.

## Business Logic Gotchas

- Scheduling is live. `ArtepaxApplication` enables scheduling, and `common/job/*` contains background jobs for reminders, overdue quotes, saldo-pendiente, and automatic cancellation processing. Read those jobs before changing event, notification, or configuration flows.
- Event state transitions and their side effects are centralized in `evento/service/impl/EventoServiceImpl.java`; changing event status logic without reading that file is risky because it coordinates inventory and payment behavior.
  - State flow: COTIZACION → PROGRAMADO → CONFIRMADO → EN_MONTAJE → (PENDIENTE_COBRO | FINALIZADO). CANCELACION_PENDIENTE is a reversible intermediate state.
  - PROGRAMADO → CONFIRMADO can be auto-triggered when the anticipo threshold is met after a payment is confirmed (see `PagoServiceImpl.verificarAnticipo`).
  - PENDIENTE_COBRO → FINALIZADO is auto-triggered after the last payment is confirmed when the event is fully paid (see `finalizarDesdeCobroCompleto`).
- `documento/service/impl/DocumentoServiceImpl.java` intentionally persists a generated `Documento` even if email sending fails: `generarDocumento` (and the other doc-generation methods) catch `RuntimeException` in a try-catch, set the document's state to `FALLIDO`, and return normally so the transaction commits. This allows retry via `reenviarDocumento` instead of rolling back.
- Runtime configuration is stored in the `configuracion` database table, not in property files. `ConfiguracionService` exposes typed accessors (`getString`, `getInt`, `getDecimal`, `getBoolean`) backed by an in-memory `ConcurrentHashMap` cache. Jobs read keys like `DIAS_RECORDATORIO_EVENTO`, `TIEMPO_GRACIA_CANCELACION_MINUTOS`, and `DIAS_AVISO_SALDO_PENDIENTE` from it. After updating a config value via the API, call `invalidarCache(clave)` or the cached stale value will persist.
- AOP-based auditing lives in `common/aspect/AuditoriaAspect.java`. Its pointcuts are tied to specific method signatures in `EventoServiceImpl`, `PagoServiceImpl`, and `ClienteServiceImpl`. Renaming those methods WILL break audit logging silently — the aspect catches and swallows its own errors.
- All feature exceptions extend `common/exception/ArtepaxException` (abstract, carries `HttpStatus` + `errorCode`). `GlobalExceptionHandler` (`@RestControllerAdvice`) handles them generically. When creating a new exception, extend `ArtepaxException` for consistent error responses.
- EventoStockHandler (`evento/service/handler/EventoStockHandler.java`) uses a two-phase stock model: reservations are soft (no stock subtracted, only an `ACTIVA` `EventoProducto` record created), consumption is hard (stock subtracted + `MovimientoInventario` recorded on `finalizarEvento`). On cancellation, reservations are freed by marking them `LIBERADA` — no stock math is involved since none was subtracted.
- Stock validation uses pessimistic locking (`SELECT ... FOR UPDATE`) on productos with IDs sorted to prevent deadlock. Availability formula: `cantidadTotal - sum(active_reservations)`.
- `pago/service/impl/PagoServiceImpl.java.confirmarPago()` calls `pagoRepository.flush()` before reading the database view `vista_pagos_evento`. Removing that `flush()` will cause stale view data within the same transaction.
- When an event is cancelled, `PagoCancelacionHandler` converts PAGADO payments to `DEVOLUCION_PENDIENTE` — this requires manual admin processing. PENDIENTE payments become `CANCELADO` with no financial impact.
- `inventario/producto/entity/StockProducto.java` is a read-only DB view (`vista_stock_producto`, `@Immutable`) that computes `cantidad_disponible = total - reservada` and `stock_bajo_alerta`. `MovimientoServiceImpl` and `DecoracionServiceImpl` validate stock against this view, while `EventoStockHandler` computes availability manually against `Producto` + `EventoProducto` in Java. Keep both in sync.
- `inventario/movimiento/service/impl/MovimientoServiceImpl.java` and `inventario/producto/service/impl/ProductoServiceImpl.java` use `jakarta.transaction.Transactional` instead of the Spring variant used by every other service. Be aware of this inconsistency when debugging transaction boundaries in those files.
- `inventario/movimiento/service/impl/MovimientoServiceImpl.registrarAjuste()` validates that the physical count is never lower than the sum of active reservations — this guard prevents inventory shrinkage from breaking event stock promises.

## Local Guidance

- Repo-local Java guidance lives in `.agents/skills/java-coding-standards/SKILL.md`, `.agents/skills/java-docs/SKILL.md`, and `.agents/skills/java-springboot/SKILL.md`. Follow those when editing Java code or reviewing API/service changes.

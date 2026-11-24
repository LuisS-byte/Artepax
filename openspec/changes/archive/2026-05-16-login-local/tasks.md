# Tasks: Login Local (email + password)

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~190 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | auto-chain |
| Chain strategy | not-needed |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: not-needed
400-line budget risk: Low

## Phase 1: Foundation — DTO, Repository, Config

- [x] 1.1 Create `auth/dto/LoginRequest.java`: DTO with `@NotBlank @Email String email`, `@NotBlank String password`, Lombok `@Data`.
- [x] 1.2 Add `@Bean PasswordEncoder` to `security/SecurityConfig.java`: `return new BCryptPasswordEncoder()`, imports from `org.springframework.security.crypto.password`.
- [x] 1.3 Add `Optional<Usuario> findByCorreoAndTipoAuth(String correo, String tipoAuth)` to `user/repository/UsuarioRepository.java`.

## Phase 2: Core — LocalAuthService

- [x] 2.1 Create `auth/service/LocalAuthService.java`: `@Service`, inject `UsuarioRepository`, `PasswordEncoder`, `JwtService`. Single `login(LoginRequest)` method: find by `correo + "LOCAL"` → check `estado` → check `rol == "ADMIN"` → BCrypt verify → `jwtService.generateToken()` → `AuthResponse`. Throws `ResponseStatusException` (401/403) matching GoogleAuthService pattern.

## Phase 3: Wiring — Controller Endpoint

- [x] 3.1 Modify `auth/controller/AuthController.java`: inject `LocalAuthService`, add `@PostMapping("/login") loginLocal(@Valid @RequestBody LoginRequest)` returning `ResponseEntity.ok(localAuthService.login(request))`. Import `jakarta.validation.Valid`.

## Phase 4: Testing — Unit Tests + Seed

- [x] 4.1 Create `auth/service/LocalAuthServiceTest.java` under `src/test/java/com/catolica/Artepax/authentication/`: `@ExtendWith(MockitoExtension.class)`, 7 scenarios per spec §7 — success, wrong password (401), unknown email (401), GOOGLE user (401), inactive user (403), non-ADMIN role (403), non-BCrypt hash (401). Mock `UsuarioRepository`, `PasswordEncoder`, `JwtService`. Assert exception type, status, and message.

**GOOGLE user test detail**: The test MUST verify that `findByCorreoAndTipoAuth(email, "LOCAL")` returns `Optional.empty()` for a GOOGLE user. The query itself is the filter — the test should NOT find the user first and then check tipoAuth. Mock the repository to return empty for the LOCAL-scoped query; this proves the query-level isolation.
- [x] 4.2 Create `db/seed/auth-seed.sql` with **two separate UPDATE statements** — DO NOT merge into a single rule:

```sql
-- 1. Corregir usuario mal clasificado (es Google, no LOCAL)
UPDATE usuario
SET usu_tipo_auth = 'GOOGLE'
WHERE usu_correo = 'usuario2@example.com'
  AND usu_password = 'GOOGLE_AUTH';

-- 2. Asegurar que usuarios LOCAL tengan BCrypt válido
-- (ejecutar solo para usuarios que realmente son LOCAL y usarán login local)
UPDATE usuario
SET usu_password = '<BCrypt_generado>'
WHERE usu_correo = '<admin-local>'
  AND usu_tipo_auth = 'LOCAL';
```

## Phase 5: Integration Verification

- [ ] 5.1 Manual verification per design §Testing Strategy: `POST /auth/login` with LOCAL credentials → 200 + JWT → `GET /api/usuarios/me` with `Authorization: Bearer <token>` → 200 + user schema. Verify Google login still works via `POST /auth/google`. Confirm `@PreAuthorize("hasRole('ADMIN')")` endpoints accept local-issued JWT.

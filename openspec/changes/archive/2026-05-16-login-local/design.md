# Design: Login Local (email + password)

## Technical Approach

New `LocalAuthService` mirrors `GoogleAuthService` pattern — validate → find user by `correo + tipoAuth='LOCAL'` → check estado/rol → verify BCrypt → generate JWT via existing `JwtService`. Wire into `AuthController` as new `POST /auth/login` endpoint. No modification to GoogleAuthService, JwtService, or JwtAuthenticationFilter.

## Architecture Decisions

| Decision | Choice | Rejected | Rationale |
|----------|--------|----------|-----------|
| Exception type | `ResponseStatusException` | `ArtepaxException` | GoogleAuthService already uses it; GlobalExceptionHandler handles it (line 48). Consistency within module. |
| PasswordEncoder bean location | `SecurityConfig` | Separate `AuthConfig` | Minimal diff; SecurityConfig already owns security beans. Single-class change. |
| Service pattern | New `LocalAuthService` | Modify `GoogleAuthService` to dual-mode | User requirement: don't touch GoogleAuthService. Separated services = cleaner rollback. |
| Repository query | Spring Data derived `findByCorreoAndTipoAuth` | `@Query` | Derived is sufficient; Spring Data auto-generates correct SQL from method name. |
| JWT reuse | Call existing `JwtService.generateToken(Usuario)` | Duplicate token logic | `Usuario` implements `UserDetails` — no adaptation needed. |

## Data Flow

```
POST /auth/login  {"email":"usuario1@example.com", "password":"..."}
       │
       ▼
AuthController.loginLocal(@Valid LoginRequest)
       │
       │  Bean Validation fails? → GlobalExceptionHandler → 400
       │
       ▼
LocalAuthService.login(request)
       │
  [1]  ├─► UsuarioRepository.findByCorreoAndTipoAuth(email, "LOCAL")
       │      └─ empty? → 401 "Credenciales inválidas"
       │
  [2]  ├─► usuario.getEstado() == false? → 403 "Usuario inactivo"
       │
  [3]  ├─► usuario.getRol() != "ADMIN"? → 403 "No tiene permisos"
       │
  [4]  ├─► passwordEncoder.matches(password, usuario.password)
       │      └─ false? → 401 "Credenciales inválidas"
       │
  [5]  └─► jwtService.generateToken(usuario) → AuthResponse(token)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `auth/dto/LoginRequest.java` | **Create** | DTO: `@NotBlank @Email String email`, `@NotBlank String password`. Lombok `@Data`. |
| `auth/service/LocalAuthService.java` | **Create** | `@Service`, injects `UsuarioRepository`, `PasswordEncoder`, `JwtService`. Single `login(LoginRequest)` method returning `AuthResponse`. |
| `auth/controller/AuthController.java` | **Modify** | Add `private final LocalAuthService localAuthService` field. Add `@PostMapping("/login") loginLocal(@Valid @RequestBody LoginRequest)`. |
| `security/SecurityConfig.java` | **Modify** | Add `@Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }`. |
| `user/repository/UsuarioRepository.java` | **Modify** | Add `Optional<Usuario> findByCorreoAndTipoAuth(String correo, String tipoAuth);`. |
| `auth/service/LocalAuthServiceTest.java` | **Create** | 7 test methods: success, wrong password, unknown email, GOOGLE user, inactive user, non-ADMIN role, **non-BCrypt password hash**. Mockito + JUnit 5. |
| `db/seed/auth-seed.sql` | **Create** | Seed correction: fix `usuario2@example.com` `usu_tipo_auth` to 'GOOGLE', add LOCAL test user with BCrypt hash. |

## Component Responsibilities

| Component | Responsibility |
|-----------|---------------|
| `LoginRequest` | Transport email + password from HTTP body; declare validation constraints. |
| `LocalAuthService` | Orchestrate LOCAL authentication: repository lookup, state checks, BCrypt verify, JWT issuance. |
| `AuthController` | Route `POST /auth/login` to `LocalAuthService`, `POST /auth/google` to `GoogleAuthService`. |
| `SecurityConfig` | Provide `PasswordEncoder` bean; `/auth/**` already `permitAll()`. |
| `UsuarioRepository` | `findByCorreoAndTipoAuth` filters by auth type — GOOGLE users never match local login. |
| `GlobalExceptionHandler` | Already handles `ResponseStatusException` + `MethodArgumentNotValidException` — zero changes. |

## Testing Strategy

| Scenario | Mock setup | Assertion |
|----------|-----------|-----------|
| Success | repo returns LOCAL user (estado=true, rol=ADMIN); encoder.matches=true; jwt returns "token" | `AuthResponse.token` equals "token" |
| Wrong password | repo returns LOCAL user; encoder.matches=false | `ResponseStatusException(401, "Credenciales inválidas")` |
| Unknown email | repo returns `Optional.empty()` | `ResponseStatusException(401, "Credenciales inválidas")` |
| GOOGLE user | repo returns `Optional.empty()` (type filter) | `ResponseStatusException(401, "Credenciales inválidas")` |
| Inactive user | repo returns LOCAL user (estado=false) | `ResponseStatusException(403, "Usuario inactivo")` |
| Non-ADMIN role | repo returns LOCAL user (rol=null or "USER") | `ResponseStatusException(403, "No tiene permisos")` |
| Non-BCrypt password hash | repo returns LOCAL user; password contains "GOOGLE_AUTH" | `passwordEncoder.matches()` returns false → `ResponseStatusException(401, "Credenciales inválidas")` |

All tests use `@ExtendWith(MockitoExtension.class)`, `@Mock` for dependencies, `@InjectMocks` for `LocalAuthService`.

### Integration Verification (manual)

1. `POST /auth/login` with valid LOCAL credentials → 200 + JWT
2. Use JWT in `Authorization: Bearer <token>` header → `GET /api/usuarios/me` → 200 + `{id, nombre, correo, telefono, rol}`
3. Same JWT → any `@PreAuthorize("hasRole('ADMIN')")` endpoint → authorized

## Rollout

No migration required. `POST /auth/login` is additive. Rollback: revert commit or remove endpoint + PasswordEncoder bean; Google login unaffected.

## Open Questions

- None. All design decisions resolved per user preferences and existing codebase patterns.

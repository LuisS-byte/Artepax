# Design: Admin User Management (admin-usuarios)

## Technical Approach

Extend `UsuarioController` with three ADMIN-only endpoints, backed by new methods in `UsuarioService` and two new repository queries. PasswordEncoder (BCrypt) already exists in `SecurityConfig`. DTOs in a new `dto/` subpackage under `authentication/user/`. GlobalExceptionHandler already handles `MethodArgumentNotValidException` (field validation → 400) and `ArtepaxException` — custom domain errors like self-deactivation extend `ArtepaxException` for consistent 400 responses.

## Architecture Decisions

| Decision | Choice | Rejected | Rationale |
|----------|--------|----------|-----------|
| Exception for business rules | `ArtepaxException` con `HttpStatus.BAD_REQUEST` | `ResponseStatusException` | `ArtepaxException` es abstracto y ya maneja `HttpStatus` + `errorCode`. Si el constructor existente no acepta `HttpStatus`, usar `ResponseStatusException` temporalmente para no modificar el sistema global de errores solo por este cambio. |
| DTO location | New `dto/` subpackage under `authentication/user/` | Keep in existing packages | Feature-based package structure per project convention; DTOs are always local, never shared across modules. |
| Password validation | JSR-380 `@Size(min=8, max=72)` en DTO | Manual check en service | Automatic 400 via `GlobalExceptionHandler.handleValidation` — zero code. Límite 72 por BCrypt (solo procesa 72 bytes). |
| Role lookup | `RolRepository.findById(Integer)` | Custom query by nombre | Spec pasa `rolId` (Integer PK, consistente con `Rol.idRol` que es `Integer` en la entidad). |
| Auto-deactivation guard | Service recibe `Authentication` (o `Usuario` autenticado) | Controller-level check | Service valúa self-deactivation comparando IDs; controller pasa `@AuthenticationPrincipal`. |
| `countByRolNombreAndEstadoTrue` | Preferir `@Query` explícita | Spring Data derived query que atraviesa `u.rol.nombre` | El derived query puede fallar si la navegación de relación no es directa. `@Query` con JOIN explícito es más estable. |

## Data Flow

```
POST /api/usuarios  {"nombre":"...", "correo":"...", "password":"...", "rolId":1}
       │
       ▼
UsuarioController.crearUsuario(@Valid @RequestBody UsuarioCreateRequest)
  [1]  ├─ @PreAuthorize → 403 if not ADMIN
       │
       ▼
UsuarioService.crearUsuarioLocal(request)
  [2]  ├─ usuarioRepository.existsByCorreoIgnoreCase(correo)  → true? → 400
  [3]  ├─ rolRepository.findById(rolId)                        → empty? → 400 "Rol no válido"
  [4]  ├─ BCrypt.encode(password)
  [5]  ├─ Usuario.builder().tipoAuth("LOCAL").estado(true)...
  [6]  └─ usuarioRepository.save(entity) → UsuarioResponseDTO
       │
       └─→ 201 Created

PATCH /api/usuarios/{id}/estado  {"activo":false}
       │
       ▼
UsuarioController.cambiarEstado(@PathVariable Long id,
    @Valid @RequestBody UsuarioEstadoRequest request,
    @AuthenticationPrincipal Usuario usuarioAutenticado)
  [1]  ├─ @PreAuthorize → 403 if not ADMIN
       │
       ▼
UsuarioService.cambiarEstado(id, request, usuarioAutenticado)
  [2]  ├─ usuarioRepository.findById(id)                    → empty? → 404
  [3]  ├─ usuarioAutenticado.idUsuario == id + desactivando → 400 "no puede desactivarse a sí mismo"
  [4]  ├─ target.rol.nombre == "ADMIN" && target.estado && desactivando?
  [5]  │    └─ countActivosByRolNombre("ADMIN") == 1?       → 400 "último ADMIN activo"
  [6]  ├─ target.setEstado(request.activo())
  [7]  └─ usuarioRepository.save(target) → UsuarioResponseDTO
       │
       └─→ 200 OK

GET /api/usuarios
       │
       ▼
UsuarioController.listarUsuarios()
  [1]  ├─ @PreAuthorize → 403 if not ADMIN
       │
       ▼
UsuarioService.listarUsuarios()
  [2]  ├─ usuarioRepository.findAll()
  [3]  └─ map to List<UsuarioResponseDTO> (no password field)
       │
       └─→ 200 OK
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `auth/user/dto/UsuarioCreateRequest.java` | **Create** | DTO: `@NotBlank String nombre`, `@NotBlank @Email String correo`, `@NotBlank @Size(min=8,max=72) String password`, `String telefono`, `@NotNull Integer rolId` |
| `auth/user/dto/UsuarioEstadoRequest.java` | **Create** | DTO: `Boolean activo` — wrapping primitive for explicit JSON |
| `auth/user/dto/UsuarioResponseDTO.java` | **Create** | Record: id, nombre, correo, telefono, tipoAuth, estado, **rolId** (Integer), rol (String), fechaCreacion. No password field. |
| `auth/user/repository/RolRepository.java` | **Create** | `JpaRepository<Rol, Integer>` — `findById(Integer)` inherited |
| `auth/user/repository/UsuarioRepository.java` | **Modify** | Add `existsByCorreoIgnoreCase(String)`, `countByRolNombreAndEstadoTrue(String rolNombre)` |
| `auth/user/service/UsuarioService.java` | **Modify** | Add `crearUsuarioLocal(UsuarioCreateRequest)`, `listarUsuarios()`, `cambiarEstado(Long, UsuarioEstadoRequest, Authentication)` |
| `auth/user/controller/UsuarioController.java` | **Modify** | Inject `UsuarioService`. Add `POST /api/usuarios` (@PreAuthorize), `GET /api/usuarios`, `PATCH /api/usuarios/{id}/estado`. Keep `GET /me` intact. |

## Component Responsibilities

| Component | Responsibility |
|-----------|---------------|
| `UsuarioCreateRequest` | Validate and transport create payload; `@Size` on password triggers Bean Validation. |
| `UsuarioEstadoRequest` | Transport `activo` boolean for state toggle. |
| `UsuarioResponseDTO` | Output projection — explicitly excludes password. Maps `Rol.nombre` to `String`. |
| `RolRepository` | Lookup `Rol` by PK — validate role exists before create. |
| `UsuarioRepository` | `existsByCorreoIgnoreCase` for duplicate guard; `countByRolNombreAndEstadoTrue` for last-ADMIN guard. |
| `UsuarioService` | Orchestrate create/list/state-change with business rules. Encodes password via injected `PasswordEncoder`. |
| `UsuarioController` | Route + `@PreAuthorize` + delegate to service. `@Valid` on request body. |
| `GlobalExceptionHandler` | Already handles `ArtepaxException` (all 400/404 errors), `MethodArgumentNotValidException` (field validation). Zero changes. |

## Interfaces / Contracts

```java
// Repositories
public interface RolRepository extends JpaRepository<Rol, Integer> {}

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByCorreoIgnoreCase(String correo);

    @Query("""
        SELECT COUNT(u) FROM Usuario u
        JOIN u.rol r WHERE r.nombre = :rolNombre
        AND u.estado = true
    """)
    long countActivosByRolNombre(@Param("rolNombre") String rolNombre);
}

// DTOs
public record UsuarioCreateRequest(
    @NotBlank String nombre,
    @NotBlank @Email String correo,
    @NotBlank @Size(min = 8, max = 72) String password,
    String telefono,
    @NotNull Integer rolId
) {}

public record UsuarioEstadoRequest(@NotNull Boolean activo) {}

public record UsuarioResponseDTO(
    Long id, String nombre, String correo,
    String telefono, String tipoAuth, Boolean estado,
    Integer rolId, String rol, LocalDateTime fechaCreacion
) {}

// Controller
@PatchMapping("/{id}/estado")
public ResponseEntity<UsuarioResponseDTO> cambiarEstado(
        @PathVariable Long id,
        @Valid @RequestBody UsuarioEstadoRequest request,
        @AuthenticationPrincipal Usuario usuarioAutenticado
) {
    return ResponseEntity.ok(usuarioService.cambiarEstado(id, request, usuarioAutenticado));
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit — Service | `crearUsuarioLocal` — success, duplicate email, invalid role | `@ExtendWith(MockitoExtension.class)`, mock repos + PasswordEncoder. Verify builder state. |
| Unit — Service | `cambiarEstado` — activation, deactivation, self-deactivation (400), last ADMIN (400), not found (404) | Mock `findById`, `countActivosByRolNombre`, `authentication.getName()`. Comparar IDs del target y del auth. |
| Unit — Service | `listarUsuarios` — returns DTOs, no password leaked | Mock `findAll`, verify mapped DTO fields. |
| Integration — Controller | Full request-response cycle for all 3 endpoints | `@WebMvcTest(UsuarioController.class)` with `@MockBean` services. Test `@PreAuthorize` by mocking auth. One class per scenario family. |
| Integration — End-to-End | Smoke test: create user → list includes it → toggle state → verify login | Manual via Swagger or curl. |

## Rollout

No migration required. All three endpoints are additive — no existing behavior changes. `GET /api/usuarios/me` is untouched. `@PreAuthorize` on new methods never fires for existing clients. Rollback: revert the commit.

## Open Questions

- None. All decisions resolved per spec and existing patterns.

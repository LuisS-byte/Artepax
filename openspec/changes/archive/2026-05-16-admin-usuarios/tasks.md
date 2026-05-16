# Tasks: Admin Usuarios

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~590 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (Foundation) → PR 2 (Service) → PR 3 (Controller + Tests) |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Base | Notes |
|------|------|-----------|------|-------|
| 1 | DTOs + Repository changes | PR 1 | feature/admin-usuarios | Foundation: types repos depend on. ~165 lines. |
| 2 | Service implementation + unit tests | PR 2 | PR 1 branch | Core business logic. ~290 lines. |
| 3 | Controller + integration tests | PR 3 | PR 2 branch | Routes + @PreAuthorize + @WebMvcTest. ~195 lines. |

## Phase 1: Foundation — DTOs y Repositorios (PR 1)

- [x] 1.1 Crear `auth/user/dto/UsuarioCreateRequest.java` — record con `@NotBlank`, `@Email`, `@Size(min=8,max=72)`, `@NotNull Integer rolId`
- [x] 1.2 Crear `auth/user/dto/UsuarioEstadoRequest.java` — record con `@NotNull Boolean activo`
- [x] 1.3 Crear `auth/user/dto/UsuarioResponseDTO.java` — record con `rolId` (Integer) + `rol` (String), sin password
- [x] 1.4 Crear `auth/user/repository/RolRepository.java` — `extends JpaRepository<Rol, Integer>`
- [x] 1.5 Modificar `auth/user/repository/UsuarioRepository.java` — agregar `existsByCorreoIgnoreCase` + `@Query countActivosByRolNombre`

## Phase 2: Core Implementation — Service (PR 2)

- [x] 2.1 Inyectar `RolRepository`, `PasswordEncoder` en `UsuarioService` como `private final`
- [x] 2.2 Implementar `crearUsuarioLocal(UsuarioCreateRequest)`: validar correo duplicado → BCrypt → persistir con `tipoAuth=LOCAL`, `estado=true` → return `UsuarioResponseDTO`
- [x] 2.3 Implementar `listarUsuarios()`: `findAll()` → mapear a `List<UsuarioResponseDTO>` (excluir password)
- [x] 2.4 Implementar `cambiarEstado(Long, UsuarioEstadoRequest, Usuario autenticado)`: validar existencia (404), autodesactivación (400), último ADMIN (400), toggle + save

## Phase 3: Rutas — Controller (PR 3)

- [x] 3.1 Inyectar `UsuarioService` en `UsuarioController`; agregar `@PreAuthorize("hasRole('ADMIN')")` a nivel de método (no clase, por compatibilidad con GET /me)
- [x] 3.2 Agregar `POST /api/usuarios` con `@Valid @RequestBody UsuarioCreateRequest` → 201
- [x] 3.3 Agregar `GET /api/usuarios` → `listarUsuarios()` → 200
- [x] 3.4 Agregar `PATCH /api/usuarios/{id}/estado` con `@AuthenticationPrincipal Usuario` → 200
- [x] 3.5 Verificar `GET /me` intacto (sin cambios ni rotura)

## Phase 4: Verificación — Tests (PR 3)

- [x] 4.1 Unit test: `crearUsuarioLocal` — éxito, correo duplicado (400), rol inexistente (400), password inválido
- [x] 4.2 Unit test: `listarUsuarios` — retorna DTOs sin password
- [x] 4.3 Unit test: `cambiarEstado` — activación, desactivación, autodesactivación (400), último ADMIN (400), usuario no encontrado (404)
- [x] 4.4 Controller test (standalone MockMvc) — verificar enrutamiento + validación en POST/PATCH
- [x] 4.5 Controller test — verificar GET /me preservado (directo + MockMvc sin auth). @PreAuthorize 403 requiere spring-security-test no disponible

## Dependencies

| Task | Depends On |
|------|-----------|
| 1.5 (Repos mod) | — |
| 1.1-1.4 (DTOs + RolRepo) | — |
| 2.2-2.4 (Service) | 1.1-1.5 |
| 3.2-3.5 (Controller) | 2.2-2.4 |
| 4.1-4.5 (Tests) | 3.2-3.5 |

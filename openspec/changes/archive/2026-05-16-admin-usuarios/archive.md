# Archive Report: admin-usuarios

**Archive date**: 2026-05-16
**Verification result**: PASS_WITH_MANUAL_VERIFICATION_PENDING
**Implemented**: ✅ Yes
**Tests**: 28/28 new, 205/205 total applicable

## Artifacts

| Artifact | File | Status |
|----------|------|--------|
| Proposal | `proposal.md` | ✅ Archived |
| Specification | `spec.md` | ✅ Archived |
| Design | `design.md` | ✅ Archived |
| Tasks | `tasks.md` | ✅ Archived (18/18 tasks complete) |
| Verify Report | `verify-report.md` | ✅ Archived |
| Contract (Frontend) | `contract.md` | ✅ Archived |

## Tasks Completed

All 18 tasks across 4 phases were completed:

### Phase 1: Foundation — DTOs y Repositorios (5/5)
- [x] 1.1 UsuarioCreateRequest record
- [x] 1.2 UsuarioEstadoRequest record
- [x] 1.3 UsuarioResponseDTO record (without password)
- [x] 1.4 RolRepository (JpaRepository<Rol, Integer>)
- [x] 1.5 UsuarioRepository: existsByCorreoIgnoreCase + @Query countActivosByRolNombre

### Phase 2: Core Implementation — Service (3/3)
- [x] 2.1 Inyectar RolRepository, PasswordEncoder
- [x] 2.2 Implementar crearUsuarioLocal: duplicate check → BCrypt → persist
- [x] 2.3 Implementar listarUsuarios: findAll → map DTO (no password)
- [x] 2.4 Implementar cambiarEstado: existence, self-deactivation, last-ADMIN guards

### Phase 3: Rutas — Controller (5/5)
- [x] 3.1 @PreAuthorize a nivel de método (compatible con GET /me)
- [x] 3.2 POST /api/usuarios → 201
- [x] 3.3 GET /api/usuarios → 200
- [x] 3.4 PATCH /api/usuarios/{id}/estado → 200
- [x] 3.5 GET /me intacto

### Phase 4: Verificación — Tests (5/5)
- [x] 4.1 Unit: crearUsuarioLocal — success, duplicate, invalid role, invalid password
- [x] 4.2 Unit: listarUsuarios — DTOs without password
- [x] 4.3 Unit: cambiarEstado — activation, deactivation, self-deactivation, last ADMIN, not found
- [x] 4.4 Controller test: routing + validation on POST/PATCH
- [x] 4.5 Controller test: GET /me preserved

## Minor Deviation

**ResponseStatusException** was used in `UsuarioService` instead of `ArtepaxException` because `ArtepaxException` is abstract (not directly instantiable). HTTP behavior preserved (400/404).

## Pendiente (post-archive)

Verificación manual con PostgreSQL + frontend:
- [ ] ADMIN crea usuario LOCAL → 201
- [ ] Nuevo usuario hace login → 200 + JWT
- [ ] JWT funciona en GET /api/usuarios/me
- [ ] Listar usuarios sin password
- [ ] Desactivar/activar usuario
- [ ] Autodesactivación rechazada
- [ ] Sin rol ADMIN → 403
- [ ] POST /auth/google sigue funcionando

## Traceability

- **Verification**: PASS_WITH_MANUAL_VERIFICATION_PENDING — structural validation complete, 205/205 tests passing
- **No delta specs**: the spec was a single file (not delta-per-domain), so no main spec merge was needed
- **Archive location**: `openspec/changes/archive/2026-05-16-admin-usuarios/`

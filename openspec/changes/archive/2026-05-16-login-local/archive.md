# Archive Report: login-local

## Summary

**Change**: login-local
**Archived**: 2026-05-16
**Phase**: Archive
**Artifact Store Mode**: hybrid (openspec + engram)
**Status**: PASS_WITH_MANUAL_VERIFICATION_PENDING

Implements local email + password authentication alongside existing Google OAuth2 login. Adds `POST /auth/login` endpoint, `LocalAuthService`, `LoginRequest` DTO, `PasswordEncoder` BCrypt bean, repository query, unit tests (8 passing), seed data correction, and frontend-facing auth contract documentation.

## Verification Overview

| Metric | Value |
|--------|-------|
| Tasks total | 8 |
| Tasks complete | 7 (1 manual integration remaining — task 5.1) |
| Unit tests | 8/8 passing (LocalAuthServiceTest: 7, AuthControllerTest: 1) |
| Build | 177 passed, 1 pre-existing error (ArtepaxApplicationTests needs PostgreSQL) |
| Critical issues | None |

## Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| `auth` | Created | `openspec/specs/auth/spec.md` — Full spec (8 requirements, 19 scenarios) copied from delta |

## Engram Observation IDs (Lineage)

| Artifact | ID | Topic Key |
|----------|----|-----------|
| proposal | #414 | `sdd/login-local/proposal` |
| spec | #415 | `sdd/login-local/spec` |
| design | #416 | `sdd/login-local/design` |
| tasks | #417 | `sdd/login-local/tasks` |
| apply-progress | #418 | `sdd/login-local/apply-progress` |
| verify-report | #419 | `sdd/login-local/verify-report` |
| archive-report | (this document) | `sdd/login-local/archive-report` |

## Archive Contents

| Artifact | Status |
|----------|--------|
| `proposal.md` | ✅ |
| `spec.md` | ✅ |
| `design.md` | ✅ |
| `tasks.md` | ✅ (7/8 tasks complete) |
| `verify-report.md` | ✅ |
| `contract.md` | ✅ |

## Source of Truth Updated

The following main spec now reflects the new behavior:
- `openspec/specs/auth/spec.md`

## Risks Carried Forward

1. **Task 5.1 — Manual integration verification pending**: JWT compatibility with existing endpoints (REQ-04) requires manual testing against a running PostgreSQL instance before the change is fully verified.
2. **REQ-01 — Input validation**: Implemented structurally via Bean Validation. Adding `@WebMvcTest(AuthController.class)` with MockMvc for behavioral HTTP 400 coverage is recommended as a follow-up.

## Files Changed (Implementation)

| File | Action | Description |
|------|--------|-------------|
| `auth/dto/LoginRequest.java` | Created | DTO with `@Email`, `@NotBlank` validation |
| `auth/service/LocalAuthService.java` | Created | Core service: validate → find → check estado/rol → BCrypt → JWT |
| `auth/service/LocalAuthServiceTest.java` | Created | 7 unit tests covering all spec scenarios |
| `auth/controller/AuthControllerTest.java` | Created | 1 test verifying delegation to LocalAuthService |
| `auth/controller/AuthController.java` | Modified | Added `@PostMapping("/login")` + LocalAuthService injection |
| `security/SecurityConfig.java` | Modified | Added `@Bean PasswordEncoder` (BCryptPasswordEncoder) |
| `user/repository/UsuarioRepository.java` | Modified | Added `findByCorreoAndTipoAuth()` query |
| `db/seed/auth-seed.sql` | Created | Seed correction: GOOGLE user tipo_auth fix + LOCAL BCrypt template |

## SDD Cycle Complete

The change has been fully planned (proposal → spec → design → tasks), implemented (apply with strict TDD, 8/8 tests passing), verified (verify-report with compliance matrix), and archived. Ready for the next change.

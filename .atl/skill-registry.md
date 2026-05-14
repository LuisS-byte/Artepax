# Skill Registry

**Delegator use only.** Any agent that launches sub-agents reads this registry to resolve compact rules, then injects them directly into sub-agent prompts. Sub-agents do NOT read this registry or individual SKILL.md files.

See `_shared/skill-resolver.md` for the full resolution protocol.

## User Skills

| Trigger | Skill | Path |
|---------|-------|------|
| Creating a pull request, opening a PR, preparing changes for review | branch-pr | ~/.config/opencode/skills/branch-pr/SKILL.md |
| Creating a GitHub issue, reporting a bug, requesting a feature | issue-creation | ~/.config/opencode/skills/issue-creation/SKILL.md |
| Writing Go tests, using teatest, adding test coverage | go-testing | ~/.config/opencode/skills/go-testing/SKILL.md |
| User says "judgment day", "judgment-day", "review adversarial", "dual review", "doble review", "juzgar", "que lo juzguen" | judgment-day | ~/.config/opencode/skills/judgment-day/SKILL.md |
| Creating a new skill, adding agent instructions, documenting patterns for AI | skill-creator | ~/.config/opencode/skills/skill-creator/SKILL.md |

## Compact Rules

Pre-digested rules per skill. Delegators copy matching blocks into sub-agent prompts as `## Project Standards (auto-resolved)`.

### java-coding-standards
- Prefer records and final fields for immutability; no setters on entities unless required
- Use Optional from find* methods; map/flatMap instead of get(); orElseThrow with domain exceptions
- Use unchecked exceptions for domain errors; wrap technical exceptions with context
- Favor constructor injection; declare dependencies as private final
- One public top-level type per file; keep methods short and focused
- JUnit 5 + AssertJ for fluent assertions; Mockito for mocking (no partial mocks)
- Avoid long parameter lists (use DTOs), deep nesting (early returns), magic numbers (constants)
- SLF4J parameterized logging: `log.info("fetch_user id={}", userId)`

### java-docs
- Document public/protected members with Javadoc; package-private for complex logic
- First sentence is the summary, ends with period
- Use @param (lowercase start, no period), @return, @throws, @see, @since, @version, @author
- Use {@code} for inline code, `<pre>{@code ... }</pre>` for code blocks
- Use {@inheritDoc} unless major behavior change (document the differences)

### java-springboot
- Organize code by feature/domain (e.g. `evento/`, `clientes/`), NOT by layer
- Constructor injection with `private final` fields; use `@Service`, `@Repository`, `@RestController`
- Use DTOs for API layer; never expose JPA entities directly to the client
- Use `@ConfigurationProperties` for type-safe config binding; `@Valid` for request validation
- Global `@ControllerAdvice` + `@ExceptionHandler` for consistent error responses
- `@Transactional` at the most granular service method level needed
- Test slices: `@WebMvcTest` for controllers, `@DataJpaTest` for repositories, `@SpringBootTest` for integration
- Spring Data JPA repositories extend `JpaRepository`; use `@Query` or Criteria API for complex queries

### branch-pr
- Every PR MUST link an approved issue (status:approved label) — no exceptions
- Exactly one `type:*` label required per PR
- Branch naming MUST match: `^(feat|fix|chore|docs|style|refactor|perf|test|build|ci|revert)/[a-z0-9._-]+$`
- Conventional commits required; no AI attribution (Co-Authored-By) in commits
- Run shellcheck on modified scripts before opening PR
- Never push to main/master directly; never force push to main

### issue-creation
- MUST use a template (bug report or feature request); blank issues disabled
- Every issue gets `status:needs-review` automatically on creation
- Maintainer MUST add `status:approved` before any PR can be opened
- Search existing issues for duplicates before creating; questions go to Discussions

### go-testing
- Use table-driven tests with `t.Run()` for multiple cases (name, input, expected, wantErr)
- Test Bubbletea models directly: `model.Update(msg)` and check resulting state
- Use golden files (testdata/*.golden) for complex output comparison
- Use `TestMain()` for shared setup/teardown across test files

### judgment-day
- Launch TWO sub-agents via `delegate` (async, parallel) — never sequential
- Each agent receives identical target but works independently (blind review)
- Orchestrator synthesizes: Confirmed (both found), Suspect (one found), Contradiction (they disagree)
- Judges MUST classify warnings: WARNING (real) = causes bug/data loss in production; WARNING (theoretical) = contrived scenario
- NEVER do the review yourself as the orchestrator — coordination only
- Resolve skills via Skill Resolver Protocol before launching judges

### skill-creator
- Create skills for repeated patterns, complex workflows, or project-specific conventions
- Don't create for trivial patterns, existing docs, or one-off tasks
- Structure: `skills/{name}/SKILL.md` + optional `assets/` and `references/`
- Frontmatter required: name, description (with Trigger:), license, metadata (author, version)
- Use `## When to Use` and `## Critical Patterns` sections; keep examples minimal

## Project Conventions

| File | Path | Notes |
|------|------|-------|
| AGENTS.md | AGENTS.md | Index — references files below |
| java-coding-standards | .agents/skills/java-coding-standards/SKILL.md | Referenced by AGENTS.md |
| java-docs | .agents/skills/java-docs/SKILL.md | Referenced by AGENTS.md |
| java-springboot | .agents/skills/java-springboot/SKILL.md | Referenced by AGENTS.md |

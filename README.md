# American Express AI Engineer Technical Assessment

This repository is an **incomplete RealWorld blogging API backend** built with
Kotlin and Ktor. **Article Search** is the feature implemented for this assessment
on `feat/article-search`, supported by automated verification and an auditable
record of AI-assisted engineering. The application is not production-ready.

## Article Search

```http
GET /api/articles/search?q=<term>
```

The endpoint is public and requires no authentication.

- `q` is trimmed. A missing, empty, or whitespace-only value returns **400**:
  `{"errors":{"q":["must not be blank"]}}`.
- Search performs case-insensitive, literal substring matching against **title OR
  body**. Description is not searched; a null title does not prevent body matches.
- SQL parameters are bound. `%`, `_`, backslashes, and quotes are literal
  characters, not wildcard syntax. Internal whitespace remains literal.
- Each matching article appears once, ordered by database ID, not relevance.
  All matches are returned; `limit` and `offset` are ignored.
- Unicode support is limited to lowercase matching, without accent folding,
  normalization, or full case folding.

Successful requests return **200** with the existing article-list JSON envelope.
`articlesCount` equals the number of returned articles. With no matches:

```json
{
  "articles": [],
  "articlesCount": 0
}
```

Each result uses the existing `Article` DTO: `slug`, `title`, `description`, `body`,
`tagList`, `createdAt`, `updatedAt`, `favorited`, `favoritesCount`, and `author`.
The new table persists only slug, title, description, and body; tags, dates,
favorite state/count, and author retain empty, null, or false/zero defaults.

## Architecture

| Component | Role |
| --- | --- |
| Kotlin / Ktor 1.2.3 | Application code, HTTP routing, validation, and JSON responses |
| Exposed 0.41.1 | Transactional database access and a parameterized SQL `LOCATE` search |
| H2 2.2.224 | In-memory application database and isolated test databases |
| Gradle 8.5 wrapper | Reproducible build, packaging, and JUnit 4 test tasks |
| GitHub Actions | Build/test matrix on Temurin JDK 17 and 21 |

The search path is `Router.kt` → `ArticleController.search` →
`ArticleRepository.search` → H2. Kodein wires dependencies; Jackson serializes DTOs.

## Build, test, and run

From the repository root on `feat/article-search`, select **JDK 17 or 21** and set
`JAVA_HOME` to that JDK's installation directory. Use the checked-in wrapper:

```sh
./gradlew clean build --no-daemon
./gradlew test --no-daemon --rerun-tasks
./gradlew run
```

`build` includes tests; the second command forces a fresh test run.
`run` starts the HTTP server at **http://localhost:8080**. Both `SERVER_PORT` in
[AppConfig.kt](src/main/kotlin/io/realworld/app/config/AppConfig.kt) and
[application.conf](src/main/resources/application.conf) specify 8080.
Stop the server with Ctrl+C.

In another terminal:

```sh
curl --get 'http://localhost:8080/api/articles/search' --data-urlencode 'q=kotlin'
```

A fresh server returns an empty list: article creation is unfinished and tests
seed H2 directly. Application data is not persisted across runs.

## Verification and CI

The standard suite reports **43 tests: 18 passing, 25 pre-existing ignored, zero
failures/errors**. The 18 active tests comprise 12 search repository/HTTP checks
and 6 user persistence compatibility checks. Search coverage includes validation,
title/body matching, literal SQL characters and injection-shaped input, empty
results, JSON serialization, stable ordering/counts, long/whitespace input, and
selected Unicode examples. Tests use real H2 and Ktor's test host; they do not
establish production socket limits or load capacity.

Two suites are explicitly excluded from the standard build/CI test count:

| Command | Purpose and recorded outcome |
| --- | --- |
| `./gradlew searchExploration --no-daemon` | 6 passing characterization checks for query-key/repeated-input behavior, large inputs/results, Unicode boundaries, resource observations, and ignored pagination. Measurements are not benchmarks. |
| `./gradlew upstreamDefectDiagnostics --no-daemon` | 3 intended-behavior assertions that currently fail for upstream follow lookup/unfollow defects. This task deliberately exits nonzero; passing compatibility tests do not certify those operations. |

Reports are written under `build/reports/tests/test/`,
`build/reports/tests/searchExploration/`, and
`build/reports/tests/upstreamDefectDiagnostics/`, respectively.

The active [Build and test workflow](.github/workflows/ci.yml) runs
`./gradlew build --no-daemon --stacktrace` on pushes, pull requests, and manual
dispatches, and uploads test reports. The [hosted run for revision
244ba8a](https://github.com/btarrant/kotlin-ktor-realworld-example-app/actions/runs/41879722970)
passed on **both JDK 17 and 21**. Separate local runs verified exploration and
known-defect diagnostics. The legacy `gradle.yml` workflow targets `master` and
does not validate feature-branch pushes.

## AI engineering evidence

The project-local [api-feature-verification Codex
skill](.agents/skills/api-feature-verification/SKILL.md), added after implementation,
standardizes contract/baseline review, test selection, report inspection, and
defect classification. From a Codex session rooted in this repository, invoke:

```text
Use $api-feature-verification to verify existing Article Search.
Inspect the API and persistence contracts, run relevant existing checks,
and report evidence and limitations. Do not modify or publish files.
```

The [skill verification report](docs/assessment-notes/skill-verification-report.md)
records two actual reuse executions against search and user persistence,
demonstrating explicit activation without claiming productivity improvements.

- [AGENT_WORKLOG.md](AGENT_WORKLOG.md): contributions, corrections, and verification summary.
- [Engineering journal](docs/assessment-notes/engineering-journal.md): chronological commands, failures, decisions, and results.
- [Implementation study guide](docs/assessment-notes/implementation-study-guide.md): implementation rationale, test design, and reviewer questions.

## Limitations and attribution

Upstream article CRUD, feeds, and related operations remain unfinished. Tests
seed articles directly, so they do not demonstrate a create-to-search API flow.
Search scans text and materializes unbounded results without pagination, a result
cap, or a full-text index. Broader RealWorld compatibility is incomplete: legacy
routes have inconsistent `/api` prefixes, the included API collection does not
pass, 25 legacy tests remain ignored, and follow diagnostics expose unresolved
upstream defects.

Derived from [Rudge's Kotlin/Ktor RealWorld example
application](https://github.com/Rudge/kotlin-ktor-realworld-example-app) and the
[RealWorld project](https://github.com/realworld-apps/realworld). Original licensing
and attribution are preserved in [LICENSE](LICENSE).

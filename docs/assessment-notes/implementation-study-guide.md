# Implementation study guide

## Architecture and scope

Upstream commit: `b9e9dec0ec03902073389edd251c4904ccbfef7a`.
`App.kt` starts `config/AppConfig.kt`; Kodein in `ModulesConfig.kt` wires controllers and repositories. `Router.kt` registers endpoints. Jackson serializes DTOs; Exposed uses H2 transactions. User/tag repositories establish the table-initialization convention.

The upstream article controller is a stub: no article repository/service/table existed, and methods return DTOs without responding. All five original controller test classes are ignored. Existing routes are unprefixed, despite article tests using `/api`. This work adds exactly `/api/articles/search`, without migrating old paths or implementing unrelated CRUD.

## Search contract

`GET /api/articles/search?q=<term>` is public. The controller trims the query. Missing, empty or whitespace-only input returns HTTP 400 with `{"errors":{"q":["must not be blank"]}}`, using the existing error envelope. Successful responses use `ArticlesDTO`: `articles` and `articlesCount`; no matches returns `[]` and `0`.

`ArticleRepository.kt` creates an `Articles` table and searches title OR body using lowercased SQL `LOCATE`, with a bound parameter. The query is literal substring matching: `%`, `_`, backslash and quotes are ordinary characters. A match in both fields yields one row. Null titles still allow body matches. Results are ordered by database ID, and count equals returned rows. Description is not searched. Query normalization uses `Locale.ROOT`; this is not linguistic full-text search or accent folding.

The minimal table stores slug, nullable title/description, and body. Other `Article` properties retain existing DTO defaults. There are no article relationships, author projection, tags or timestamps persisted by this new schema. The existing article creation endpoint remains a stub. A fresh application therefore returns no matches until rows are populated directly; tests use explicit SQL fixtures. This is a substantial upstream limitation, not a completed CRUD application.

## Verification

`ArticleRepositoryTest.kt` runs against actual isolated H2 databases. It covers title/body OR semantics, case-insensitive and partial matches, null title, description exclusion, duplicate prevention, field preservation, wildcard/backslash/quote/Unicode literals, injection-shaped input and zero matches.

`ArticleSearchTest.kt` boots the real `mainModule` using Ktor's matching-version test host. It checks routing, public access, query decoding/trimming, HTTP status and JSON envelope. No controller/repository mocks are used. Ktor's unused client-test fixture dependency is excluded because it pulls unavailable serialization artifacts.

Run `JAVA_HOME=<JDK home> ./gradlew clean build --no-daemon` separately for JDK 17 and 21. Read `build/test-results/test/TEST-*.xml` and `build/reports/tests/test/index.html`; do not infer success from compilation or ignored tests. See the journal for actual executions, failures and final counts.

## Focused user persistence regressions

`UserRepositoryCompatibilityTest.kt` exercises real Exposed 0.41.1/H2 2.2.224 persistence: creation and lookup, generated IDs, full/partial updates, composite-key rejection (SQLState 23505), multiple followers/followees, reciprocal relationships and stored pair direction. `UserRepositoryFixture.kt` creates a unique in-memory database per test and explicitly shuts it down/unregisters it afterward. Assertions compare independently read rows, rather than relying on the defective following lookup.

`UserFollowDiagnosticsTest.kt` separately asserts the intended behavior of following-status lookup, unfollow removal, and preservation of reciprocal/unrelated relationships. Source inspection confirmed two upstream predicate defects: `follow` stores `(target, actor)`, while lookup and unfollow use the reverse pair. Consequently these diagnostics are expected to fail until a separately authorized production fix; their assertions are not weakened or wrapped as expected exceptions.

The standard `test` task explicitly excludes the `KnownUpstreamDefect` JUnit category and prints a pointer to `./gradlew upstreamDefectDiagnostics`. That dedicated task runs all three diagnostics with ordinary failure reporting and nonzero exit on failure. It is deliberately separate from the passing CI lifecycle; no `@Ignore` or `ignoreFailures` is used for these new tests. Standard test counts therefore do not include the three diagnostics. The original 25 ignored tests remain unchanged.

Reproduce from repository root (select JDK home for each run):

```sh
JAVA_HOME=<JDK home> ./gradlew test --tests '*UserRepositoryCompatibilityTest' --no-daemon
JAVA_HOME=<JDK home> ./gradlew upstreamDefectDiagnostics --no-daemon
JAVA_HOME=<JDK home> ./gradlew build --no-daemon
```

Compatibility results appear in `build/reports/tests/test`; diagnostics have their own `build/reports/tests/upstreamDefectDiagnostics` report. Verified on Temurin 17.0.21 and 21.0.13: six compatibility tests pass; all three diagnostics fail with assertion failures and no setup errors. Default builds pass 14 active tests with 25 original skips; diagnostics are separate. Full command/outcome evidence is in the journal. These repository tests do not validate user HTTP/authentication behavior or repair upstream follow predicates.

## Build and CI

The original Gradle 4.10/Kotlin 1.3.+ build could not resolve Exposed 0.14.1. Initial repair to monolithic Exposed 0.17.14 compiled but failed at runtime against H2 2.2.224 (`JdbcConnection.getSession` linkage error). Exposed 0.41.1 core/dao/jdbc fixes that compatibility issue. Adaptations are limited to `dao.id` imports, explicit `Follows` primary key and the `eq` import required by `deleteWhere`.

Gradle 8.5 supports running on JDK 21; JDK 17 support starts at Gradle 7.3 ([official matrix](https://docs.gradle.org/current/userguide/compatibility.html)). Kotlin 2.0.21 supports the selected Gradle release ([Kotlin compatibility](https://kotlinlang.org/docs/gradle-configure-project.html)). Maven Central is the only repository; versions are pinned. Wrapper scripts/jar are regenerated, and the distribution SHA-256 comes from Gradle's published checksum.

No JVM 16 target existed upstream. Both Java and Kotlin now explicitly target 16 to investigate the requested compatibility scenario. This controls class-file format, not the JDK running Gradle. JDK 17/21 can execute this bytecode. It does not guarantee all dependencies or JDK APIs support running on Java 16; Java 16 runtime is not tested or required.

`.github/workflows/ci.yml` builds on Ubuntu with Temurin 17 and 21, `fail-fast: false`, read-only repository permissions, a 15-minute timeout and test-report artifacts. `gradle/actions/setup-gradle` supplies Gradle caching; no second overlapping cache is configured. Build includes tests. Push, pull request and manual triggers are configured. Hosted execution and cache hits require an approved push and remain distinct from local verification.

## Likely reviewer questions

- **Why add storage?** No article data source existed. A real Exposed table makes search meaningful and testable without inventing an in-memory production substitute.
- **Why no new service?** Search needs only request validation and one repository query. A pass-through service adds no useful behavior here.
- **Why LOCATE instead of LIKE?** Bound literal substring input avoids SQL wildcard semantics and escaping mistakes. This implementation is verified against H2, the configured database.
- **Why not enable all old tests?** They exercise unfinished features and inconsistent routes. They remain visibly skipped; claiming full application coverage would be misleading.
- **Can this scale?** No pagination, cap or full-text index was requested. The substring query scans text and returns all matches. Large datasets need explicit pagination/count semantics and a search index.
- **What remains unverified?** Hosted Actions execution/cache restoration, other databases/operating systems, broad Unicode collation semantics, and unrelated legacy endpoints. See final audit in the journal.
- **Why no Codex skill or extra agent?** The verification is project-specific and reproducible with two test classes and Gradle. Extra orchestration would not improve the evidence for this task.

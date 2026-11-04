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

## Expanded search verification

The assessment PDF requires Search over titles/bodies with the existing response envelope. It explicitly requests pagination for Popular Articles, but does not specify Search pagination, maximum query length, duplicate-q policy, accent folding, Unicode normalization, or latency targets. These are product decisions; tests must not create new semantics implicitly.

`ArticleSearchEdgeCasesTest.kt` adds release checks for multiple matches/count/order, a 2,048-character literal term without truncation, whitespace padding versus literal internal whitespace, and accented/Cyrillic/CJK/emoji matching with response-text preservation. Ordering is database-ID/insertion order, not alphabetical or relevance order. An isolated `ArticleSearchFixture.kt` shuts down and unregisters each H2 database.

`./gradlew searchExploration --no-daemon` separately runs five characterization tests: repeated/ambiguous q, very large queries, Unicode normalization/case boundaries, bounded large results/resource observations, and pagination parameters. The default test task and JDK matrix exclude this named JUnit category and announce the separate task. No time/memory thresholds or unsupported-contract assertions enter release CI. Existing known-defect diagnostics remain separately runnable.

Exploratory resource output reports repository duration, HTTP test-host duration, response UTF-8 bytes, and coarse heap snapshots/pool peaks for 100/1,000/5,000 rows. These are single-process observations affected by warmup, GC and retained data, not statistically reliable benchmarks, allocation profiling, throughput tests or capacity guarantees. The test host bypasses network parsing limits; use the journal's separate production-CIO socket observations for transport behavior. No result cap, pagination or index is silently introduced.

Verified local results on both JDKs: standard builds pass 18 active tests with 25 original skips; all six exploratory tests pass separately. Observations:

| Area | Evidence and limitation |
| --- | --- |
| Multiple matches/order | Exact count 3 and ID order repeated three times; both-field matches return once; description-only rows excluded |
| Large/whitespace input | 2,048-character literal and padded queries pass release checks; test host accepts megabyte inputs, but CIO rejects sampled request lines above its transport limit |
| Ambiguous q | First value wins, blank first yields 400; encoded duplicate names merge and uppercase Q is accepted case-insensitively by Ktor; duplicate-key policy remains unspecified by assessment |
| Unicode | Accented case, Cyrillic, CJK and emoji pass; no accent folding, canonical normalization or full case folding (sharp-s, dotted-I, sigma boundaries) |
| Resource observations | 5,000 rows/2,500 matches produce 3,001,425 bytes; JDK21 repository sample ~27ms, HTTP host ~154ms; warmup/GC prevent capacity conclusions |
| Pagination | 25 results returned with or without limit/offset, including malformed values; no pagination added |
| RealWorld collection | All 31 requests complete but return 404 at /api routes; 17 assertion and 12 script failures. Baseline routes lack /api. Unprefixed probes additionally show registration 500 and public articles 401; tags returns 200 |

No production change is justified solely to make exploratory probes pass. Before broader deployment, define duplicate-input, query-size and pagination/result-cap contracts and assess full-text indexing/concurrency under representative load. Do not claim full RealWorld compatibility or full Unicode case folding.

See the engineering journal for actual matrix outcomes, repeated-parameter policy observations, Unicode limitations, measured values and the included RealWorld collection result. The collection's default runner points at a public server; verification must explicitly use localhost. Failures in unfinished legacy routes do not establish a search regression. Commit `6a6f120` passed hosted JDK 17/21 builds in run [41771910410](https://github.com/btarrant/kotlin-ktor-realworld-example-app/actions/runs/41771910410); each report contained 18 passing tests and 25 original skips.

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
- **What remains unverified?** Cache restoration behavior, other databases, broad Unicode collation semantics, and unrelated legacy endpoints. Hosted JDK 17/21 execution succeeded as recorded above. See final audit in the journal.
- **Why add a skill afterward?** Initial verification used ordinary tests. The later `.agents/skills/api-feature-verification/SKILL.md` standardizes contract discovery, baseline comparison, test selection and evidence reporting across API and persistence targets. See [reuse validation](skill-verification-report.md) for actual executions and limitations.

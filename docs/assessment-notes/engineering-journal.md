# Engineering journal

Start: 2026-11-04T02:39:18.931761+00:00

- User requested article search, tests, JDK 17/21 CI, evidence and assessment documents. No final push or external submission without approval.
- First discovery command searched home too broadly and hit macOS privacy errors; corrected to a dedicated project directory. No source changes yet.
- Agent: primary Codex only; exact underlying model identifier unavailable. No delegation.

## 2026-11-04T02:39:58.101433+00:00 — inspection and baseline
- Cloned upstream b9e9dec0ec03902073389edd251c4904ccbfef7a. No AGENTS.md found in repository or checked parent locations.
- `./gradlew test` failed: no Java runtime. Gradle wrapper 4.10, Kotlin 1.3.+, Ktor 1.2.3, Exposed 0.14.1, H2 2.2.224; all five existing controller test classes ignored. Article CRUD has no service/table and returns unresponded DTOs. Router has no /api prefix.
- Plan: minimal Exposed article table/repository for search, controller responds using ArticlesDTO, exact public /api/articles/search route preserving old paths. No CRUD expansion. Repository fixtures seed tests.
- Gradle official compatibility matrix: Java 17 runtime needs Gradle >=7.3, Java 21 >=8.5. No JVM 16 target exists upstream. Will explicitly target JVM 16 bytecode and exercise it on 17/21; modern compiler needed. Sources: https://docs.gradle.org/current/userguide/compatibility.html and https://kotlinlang.org/docs/gradle-configure-project.html .
- GitHub connector has no fork capability; using gh for requested fork only. Final source push remains prohibited.

## 2026-11-04T02:40:54.690772+00:00 — environment corrections
- Fork created: https://github.com/btarrant/kotlin-ktor-realworld-example-app (upstream contents only). First gh command rejected unsupported --remote flag with repository argument; removed flag. Local feature branch created; no push.
- Mistake: downloaded ARM JDKs before reading uname result (x86_64). Corrected with x64 Temurin 17/21 downloads and isolated extraction directories. Toolchains and raw evidence live outside repository.

## 2026-11-04T02:41:58.468363+00:00 — baseline completed, build repair
- Both unchanged baseline runs on Temurin 17.0.21 and 21.0.13 failed resolving Exposed 0.14.1 (30s / 6s reported by Gradle). No tests ran. Raw logs: ../evidence/baseline17.log and baseline21.log. Corrected an initially guessed JAVA_HOME version using actual extracted directory.
- Maven Central metadata lists monolithic Exposed 0.17.10–0.17.14 only; choose 0.17.14 to preserve API shape. Remove jcenter/mavenLocal for reproducible dependency resolution. Upgrade Gradle to 8.5 and Kotlin to 2.0.21, replace removed testCompile and mainClassName. Align Java/Kotlin target 16.

## 2026-11-04T02:43:29.499559+00:00 — implementation and tests
- Added Exposed Articles schema (slug/title/description/body) and parameterized H2 LOCATE query over lowercased title OR body, ordered by ID. Unlike LIKE this does not need wildcard escaping. Inspected Exposed 0.17.14 source jar APIs directly.
- Controller trims q, returns 400 errors.q for missing/blank, otherwise ArticlesDTO with actual count. Explicit public /api/articles/search route avoids changing legacy routes or slug/auth behavior. Kodein wires repository.
- Added four repository tests and four Ktor test-host integration tests; dependency matches existing Ktor version, JUnit remains version 4. Tests use real isolated H2 databases, no mocks.
- Build repair run overlapped source edits, so is not treated as a clean pre-feature baseline. Started clean build on JDK 17 in ../evidence/feature17.log.
- CI matrix 17/21, setup-gradle cache, build task includes tests, always upload reports, read-only contents permissions. Official action examples consulted: https://github.com/gradle/actions/blob/main/setup-gradle/README.md .
- Skill evaluation: no reusable Codex skill justified for this single legacy project. Two ordinary test classes and reproducible Gradle commands provide verification without extra framework/instructions. No skill created.

## 2026-11-04T02:44:24.302976+00:00 — verification failures and corrections
- First feature clean build failed resolving obsolete kotlinx serialization 0.11.1 pulled through Ktor test-host client-test fixtures. Excluded only unused ktor-client-tests-jvm; server test engine compiles.
- Next run executed 33 tests: 8 failed, 25 skipped. XML inspection shows NoSuchMethodError JdbcConnection.getSession(): Exposed 0.17.14 incompatible with existing H2 2.2.224. Keeping H2, moving to Exposed 0.41.1 modular core/dao/jdbc. This requires dao.id imports and explicit Follows composite PrimaryKey.
- Generated Gradle 8.5 wrapper successfully (2s), including jar/scripts.

## 2026-11-04T02:46:06.749316+00:00 — JDK 17 verified
- Exposed modular upgrade required explicit SqlExpressionBuilder.eq import in existing deleteWhere. First compile identified this; fixed without changing behavior. Normalized regenerated gradlew.bat newlines after diff-check flagged CRLF.
- `JAVA_HOME=<Temurin 17.0.21> ./gradlew clean build`: SUCCESS, 11s. Independently parsed XML: 8 new tests passed, 0 failures/errors; 25 original tests skipped across 5 classes. XML copied to ../evidence/jdk17.
- `javap -verbose .../ArticleController.class` reports major version 60 (JVM 16).
- Started separate clean build --no-daemon on Temurin 21.0.13; no toolchain override, so tests run on selected JVM.
- Remaining compiler warning is pre-existing user/follow generic comparison, outside search scope.

## 2026-11-04T02:47:13.967675+00:00 — JDK 21 and independent checks
- `JAVA_HOME=<Temurin 21.0.13> ./gradlew clean build --no-daemon`: SUCCESS, 24s. Parsed XML: 33 total, 0 failures/errors, 25 skipped, hence 8 executed/passed. Copied XML to ../evidence/jdk21.
- `actionlint` 1.7.12 validates .github/workflows/ci.yml with exit 0 and no findings. `git diff --check` passes.
- Mutation experiment: temporarily replaced title OR body with AND; `./gradlew test --tests *ArticleRepositoryTest` executed four tests, three failed (17s). Restored original source and verified byte-for-byte with cmp; rerunning build to ensure final reports are green.
- Added .kotlin/ to gitignore for compiler-generated local state. No new application frameworks.

## 2026-11-04T02:48:06.957149+00:00 — final verification and assessment audit
- Elapsed since journal creation: 8.8 minutes (wall clock).
- Restored-source `./gradlew build --no-daemon` on JDK 21: SUCCESS, 14s. Independently parsed final XML again: 8 passed, 25 skipped, 0 failures/errors.
- Actual application smoke test: started `./gradlew run --no-daemon` on JDK 21, using production setup/CIO/Hikari/H2. Initial curl was too early and failed to connect; retried after log showed listening. `GET /api/articles/search?q=kotlin` returned 200 and `{"articles":[],"articlesCount":0}`; blank query returned 400 and errors.q. Saved headers/bodies in ../evidence/smoke-*.txt. Stopped server via Ctrl-C afterward.
- Reviewed final diff and untracked files. Wrapper regeneration accounts for most mechanical diff; no unrelated behavior refactoring. New repository only stores core article text; default metadata and absent CRUD are explicitly documented.

| Requirement | Evidence / status |
| --- | --- |
| Fork repository | https://github.com/btarrant/kotlin-ktor-realworld-example-app created; origin points to fork, upstream preserved |
| Inspect and baseline before source changes | Relevant architecture/tests/build read; both unchanged baseline runs failed unavailable Exposed artifact, no tests claimed |
| Immediate journal, ongoing evidence | Created before cloning/source work; records mistakes, changes, commands, timestamps and actual outcomes |
| Title/body search and existing envelopes | Router/controller/Exposed implementation; real DB and Ktor tests plus production empty-result smoke |
| Edge cases | Missing/blank q, case/partial matching, null title, literal metacharacters, injection-shaped text, no matches, OR deduplication |
| Tests executed independently checked | Both JDK clean builds; parsed XML; OR-to-AND mutation killed by 3 tests; restored build passed |
| JVM 16 compatibility | Explicit target 16; javap major 60; executed on JDK 17 and 21; Java 16 runtime not tested |
| CI JDK 17/21 and caching | Matrix workflow, setup-gradle cache, build/tests, report upload; actionlint passed |
| Actual hosted workflow | NOT RUN: final workflow/source never pushed because approval required; no CI/cache-hit claim |
| Reusable testing skill | Evaluated, rejected as unnecessary overhead for this project-specific verification |
| Worklog/study guide/walkthrough | All requested files present; recording URL placeholder remains |
| Publication boundary | No source push, PR or assessment submission. Local commit prepared for review only |

Unresolved risks: existing article CRUD cannot populate the new table; fresh production database is empty. Other article metadata uses DTO defaults. Search is unpaginated and scans text; only H2 and macOS local execution verified. 25 legacy tests remain skipped; unrelated endpoints have not been validated after the necessary dependency upgrade. Existing UserRepository generic warning remains. Hosted Linux CI and cache behavior await approval to publish. No walkthrough recording has been made.

Raw build logs, XML snapshots, downloaded tools and mutation backup are kept outside submission at `../evidence` and `../toolchains`; concise evidence is retained here. Improvements for a repeat: inspect host architecture before provisioning, check legacy dependency runtime compatibility early, and preserve numbered failure logs rather than reusing a log filename (the first feature dependency failure was captured in this journal but its raw log was overwritten).

## 2026-11-04T03:23:33.296972+00:00 — focused persistence regression work started
- Request: add H2 compatibility coverage without changing production behavior or dependencies. Inspected JUnit 4 @Before/real Exposed H2 conventions in ArticleRepositoryTest, UserRepository and UserService.
- Confirmed source predicates: follow stores (target, actor), but lookup checks (actor, target) for the service calling convention and unfollow deletes (actor, target). Compared original upstream in prior review; these predicates predate search. Tests will independently inspect stored rows.
- Decision: passing compatibility tests remain in normal test/CI. Known-defect tests assert intended behavior and remain failing in an explicitly named upstreamDefectDiagnostics task, isolated by JUnit Category. No @Ignore, expected-failure wrappers or ignoreFailures. Normal test task will visibly report the diagnostic exclusion.

## 2026-11-04T03:25:00.331411+00:00 — first targeted compatibility result
- Added shared JUnit 4 H2 fixture with unique in-memory database per test, explicit SHUTDOWN and Exposed closeAndUnregister cleanup; fixtures use persisted row pairs as independent oracle. No production/dependency changes.
- Command (repository cwd): `JAVA_HOME="$PWD/../toolchains/x64-17/jdk-17.0.21+9/Contents/Home" ./gradlew test --tests '*UserRepositoryCompatibilityTest' --no-daemon` — SUCCESS, 22s. XML independently parsed: 6 tests, 0 failures/errors/skips. Snapshot ../evidence/user-compat17/.
- Coverage: create/lookup field roundtrip and missing records; positive distinct generated identities matching reads; full update including lookup keys, stable identity and unrelated-user preservation; partial update; duplicate composite-key SQLState 23505 with original row intact; multiple followers/followees and reciprocal pairs stored in correct direction.
- Started `JAVA_HOME="$PWD/../toolchains/x64-17/jdk-17.0.21+9/Contents/Home" ./gradlew upstreamDefectDiagnostics --no-daemon`; results pending.

## 2026-11-04T03:25:37.388604+00:00 — upstream diagnostics confirmed on JDK 17
- Explicit diagnostics task FAILED as intended, 11s. Parsed XML: 3 tests, 3 assertion failures, 0 errors/skips. All fixtures ran successfully; failures demonstrate wrong relationship semantics rather than setup or linkage errors. Forward lookup returns false; unfollow leaves forward row; reciprocal-case unfollow removes the wrong row. Preserved XML under ../evidence/user-diagnostics17.
- The default lifecycle excludes this named category with a visible message; diagnostic task has its own report and normal failure exit. This is an explicit separation, not a claim of full passing coverage.
- No unexpected compatibility-test failures or production changes so far. One report-read command ran before the diagnostics XML existed and got a shell glob error; waited for task completion before inspection.
- Started targeted JDK 21 run with `JAVA_HOME="$PWD/../toolchains/x64-21/jdk-21.0.13+9/Contents/Home" ./gradlew test --tests '*UserRepositoryCompatibilityTest' --no-daemon`.

## 2026-11-04T03:26:29.607385+00:00 — JDK 21 targeted verification complete
- Targeted compatibility task SUCCESS, 16s; independently parsed snapshot: 6 passed, 0 failures/errors/skips. ../evidence/user-compat21/.
- `JAVA_HOME="$PWD/../toolchains/x64-21/jdk-21.0.13+9/Contents/Home" ./gradlew upstreamDefectDiagnostics --no-daemon` FAILED, 11s. XML confirms the same 3 assertion failures, 0 errors/skips; ../evidence/user-diagnostics21/. No fixture/setup failures and no fixes to production behavior.
- Both diagnostic runs use normal Gradle failure handling; no suppression/expected-failure assertion wrapper. Expected duplicate-key rejection in the compatibility suite is separately checked for exact exception class and SQLState, preserving the original relationship.
- Started relevant regression check: `JAVA_HOME="$PWD/../toolchains/x64-17/jdk-17.0.21+9/Contents/Home" ./gradlew build --no-daemon` to exercise all default tests together (including existing article search) and application packaging after new fixture lifecycle and test-task configuration.

## 2026-11-04T03:27:15.358331+00:00 — default regression build on JDK 17
- `JAVA_HOME="$PWD/../toolchains/x64-17/jdk-17.0.21+9/Contents/Home" ./gradlew build --no-daemon` SUCCESS, 19s. Independently parsed XML snapshot ../evidence/persistence-regression17/: 39 total, 25 original skips, 14 executed/passed, 0 failures/errors. Explicitly confirmed diagnostic class is absent from default report rather than counted as a passing/skipped test.
- `git diff --exit-code 52a05b1 -- src/main` and `git diff --check` passed: production source unchanged and no whitespace errors. No dependency versions changed.
- Started `JAVA_HOME="$PWD/../toolchains/x64-21/jdk-21.0.13+9/Contents/Home" ./gradlew build --no-daemon` for equivalent default regression verification.

## 2026-11-04T03:27:49.840436+00:00 — persistence follow-up complete
- JDK 21 default build SUCCESS, 20s. Independently parsed XML snapshot ../evidence/persistence-regression21/: 39 total, 25 legacy skips, 14 passed, 0 failures/errors. Diagnostics are separately reported and excluded with a visible task message. Existing 8 search tests and all 6 new compatibility tests execute together on both JDKs.
- Final targeted results per JDK: compatibility 6/6 passed; diagnostics 3/3 failed on intended assertions (0 errors/skips). Normal lifecycle passing does not certify correct follow lookup/unfollow behavior.
- Changed only build.gradle test-task configuration, three new test files, this journal, implementation study guide and concise worklog. No production source, dependencies, CI workflow or legacy @Ignore annotations changed. Changes remain uncommitted; no push/submission.
- Remaining risks: confirmed upstream reversed lookup/unfollow predicates; 25 original tests still ignored; no new user HTTP/authentication/tag/concurrency coverage. Existing EntityID/Long generic warning remains. Hosted CI not rerun because changes have not been published. No reusable skill or new dependency needed.

## 2026-11-04T03:33:16.777798+00:00 — publication review and hosted CI authorization
- User explicitly authorized committing reviewed persistence coverage, pushing only feat/article-search to the existing btarrant fork, and inspecting hosted JDK 17/21 results. No production changes, default-branch merge, PR or recruiter submission authorized. Stop and report cause before edits if hosted CI fails.
- Reviewed all seven pending paths: 6 compatibility tests, 3 category-isolated diagnostic tests, shared fixture, Gradle task changes and three relevant documentation files. No production/dependency changes; diff-check passed.
- Publication scan found no absolute personal home paths, private keys or GitHub/AWS credential patterns in tracked/candidate text files. Reviewed fixture passwords/example.com identities and inherited signing placeholder as example data; no new credentials or confidential material identified. Evidence/toolchains/generated outputs remain outside the tracked submission; only the required wrapper jar is tracked. Relative versioned toolchain paths in the journal document actual local commands and do not configure CI.
- `JAVA_HOME="$PWD/../toolchains/x64-21/jdk-21.0.13+9/Contents/Home" ./gradlew build --dry-run --no-daemon` completed successfully; task graph includes test/check/build and excludes upstreamDefectDiagnostics. ci.yml invokes build with a JDK 17/21 matrix, so inherits the same exclusion. Existing master-only gradle.yml does not trigger on this feature-branch push.
- Commit/push/hosted outcomes will be reported from actual GitHub results; no hosted success claimed at this point.

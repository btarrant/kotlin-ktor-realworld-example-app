# Agent Worklog — American Express AI Engineer Assessment

## 1. Approach and AI Agent Usage

I used OpenAI Codex as the primary coding agent to investigate, implement, test,
and verify Article Search in the existing Kotlin/Ktor RealWorld application. I
established requirements and scope, directed implementation review, requested
additional testing, and required reproducible evidence before accepting results.
Codex inspected the inherited architecture, implemented search with real
H2/Exposed persistence, repaired build compatibility, added tests, and configured
JDK 17/21 CI.

The initial model identifier was not exposed, and no subagents were used during
initial implementation. Follow-up persistence regression work used `gpt-6.1-sol`.
Verification used Gradle, JUnit, Ktor's test host, H2, and GitHub Actions; later
skill validation used two fresh Codex CLI sessions.

## 2. Representative Instructions

My documented instructions included:

- "Run baseline checks before modifying code."
- "Never fabricate evidence."
- "Never claim unexecuted tests passed."
- "Do not push the final submission or submit anything externally without my approval."

Subsequent direction expanded verification to edge cases, Unicode, long queries,
scalability observations, and persistence regression risks. I required the agent
to distinguish specified behavior from exploratory findings, existing defects
from regressions, and local results from hosted CI. Later publication occurred
under explicit authorization; pushing the feature branch did not authorize
assessment submission.

## 3. Engineering Decisions and Corrections

- **Establish the baseline.** The unchanged build could not resolve Exposed
  0.14.1 on either JDK; no tests executed, so this was not a passing baseline.
- **Check runtime compatibility.** The intermediate upgrade to Exposed 0.17.14
  compiled but failed against H2 2.2.224 with `JdbcConnection.getSession`
  linkage errors. Inspection of test XML led to compatible Exposed 0.41.1
  `core`/`dao`/`jdbc` modules and the required API adaptations. Compilation alone
  was insufficient evidence.
- **Isolate inherited defects.** Persistence checks exposed reversed follow
  lookup/unfollow predicates. Separate intended-behavior diagnostics preserve
  those failures without changing unrelated production behavior or presenting
  them as passing coverage.
- **Keep scope explicit.** Search uses bound, literal title-or-body matching.
  Unfinished CRUD and unspecified pagination semantics were not added to satisfy
  unrelated or exploratory checks.
- **Correct assumptions with execution.** A focused HTTP test disproved the
  assumption that uppercase `Q` was treated as missing: Ktor accepts it through
  case-insensitive parameter lookup. The characterization was corrected rather
  than changing production semantics. Earlier environment/path mistakes were
  corrected and retained in the journal.

## 4. Verification and Results

**Final recorded results on both JDK 17 and 21:**

| Suite | Result | Execution boundary |
| --- | --- | --- |
| Standard tests | **18 passed; zero failures/errors; 25 pre-existing ignored** | `./gradlew build --no-daemon`; 12 search tests and 6 persistence compatibility tests |
| Search exploration | **6 passed; zero failures/errors** | Separately invoked with `./gradlew searchExploration --no-daemon`; excluded from standard CI |
| Known-defect diagnostics | **3 assertion failures; zero setup errors** | Separately invoked with `./gradlew upstreamDefectDiagnostics --no-daemon`; excluded from standard CI |

The diagnostics assert intended follow/unfollow behavior and currently fail
because of known upstream defects. The task exits nonzero; assertions are not
weakened or wrapped as expected failures.

[Hosted GitHub Actions run 41879722970](https://github.com/btarrant/kotlin-ktor-realworld-example-app/actions/runs/41879722970)
passed both JDK 17 and 21 build jobs at revision `244ba8a`. This establishes
standard build/test success, not execution of exploration or diagnostics in CI.

I challenged the agent's completion claims by requiring executed checks and report
inspection. Verification went beyond build summaries: Codex independently parsed
JUnit XML, inspected persisted rows, and compared unchanged-source baselines.
During development, a deliberate **OR-to-AND search mutation caused three test
failures**; the original source was restored byte-for-byte and the build passed.
Production HTTP smoke checks also confirmed 200/empty results and 400/blank-query
validation, without claiming a populated create-to-search API flow.

## 5. Reusable AI Agent Skill

At my request, the project-local
[api-feature-verification skill](.agents/skills/api-feature-verification/SKILL.md)
was created **after the original Article Search implementation** as an additional
reusable engineering improvement. It standardizes requirements review, baseline
verification, test selection, edge-case identification, failure investigation,
and evidence reporting.

Two fresh Codex CLI sessions explicitly invoked `$api-feature-verification` and
read its `SKILL.md` before applying it:

- **Article Search:** 18 standard tests and 6 exploratory checks passed; 25 legacy
  tests remained ignored.
- **User persistence:** 6 compatibility tests passed. Known-defect diagnostics
  were inspected, not executed, in this reuse session.

The primary agent independently checked the resulting XML. Explicit activation
and reuse were demonstrated within this repository. Implicit activation,
portability to other frameworks, and productivity gains were not established.
The [skill verification report](docs/assessment-notes/skill-verification-report.md)
records prompts, commands, activation traces, results, and limitations.

## 6. Limitations and Future Improvements

Upstream article CRUD remains unfinished; populated searches require direct
database seeding, as used by the tests. The article table stores core text fields;
other response metadata retains DTO defaults. Results are unbounded, with no
pagination or full-text index. The 25 ignored legacy tests, follow/unfollow
defects, and inconsistent API route prefixes limit broader compatibility. The
included RealWorld collection failed against localhost; complete RealWorld
compatibility and production readiness are not claimed.

Before broader deployment, complete and verify the article lifecycle, resolve
inherited compatibility defects, and restore meaningful legacy coverage. Define
pagination, result caps, query-size and Unicode contracts, then evaluate indexing
and representative concurrent load. Improve future agent runs by checking runtime
dependency compatibility early and retaining distinct failure logs and report
snapshots. Exploratory timing and heap observations are not capacity guarantees.

## 7. Walkthrough Recording

Recording: [American Express AI Engineer assessment walkthrough](https://drive.google.com/file/d/1dKZSK3mfHC4Lz4rhNDoJ8_nRSLAsGBCF/view)

## 8. Supporting Documentation

- [Engineering journal](docs/assessment-notes/engineering-journal.md): chronological decisions, commands, failures, and evidence.
- [Implementation study guide](docs/assessment-notes/implementation-study-guide.md): architecture, rationale, verification details, and reviewer questions.
- [Walkthrough outline](docs/assessment-notes/walkthrough-outline.md): original recording plan; early publication/skill status reflects its drafting stage.
- [Skill verification report](docs/assessment-notes/skill-verification-report.md): actual activation and reuse evidence.

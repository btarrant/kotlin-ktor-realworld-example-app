# Reusable verification skill: validation report

Added after Article Search implementation, at revision `6a6f120`, to exercise the
same evidence workflow across HTTP search and persistence. No productivity or
time-saving claim is made.

## Activation and structure

`.agents/skills/api-feature-verification/SKILL.md` is a project-local Codex skill
with required name/description frontmatter. No optional metadata, scripts,
dependencies or AGENTS routing is needed. The installed skill-creator validator
returned `Skill is valid!` (PyYAML installed only in an external validation venv).
[Official discovery documentation](https://learn.chatgpt.com/docs/build-skills)
describes repository discovery and explicit `$` invocation.

Two fresh Codex CLI 0.166.0 executions received prompts naming only the skill,
without its file path or instructions. Both announced its use and successfully
read the project SKILL.md in trace `item_2`, then inspected different code/test
boundaries and executed checks. This demonstrates explicit invocation and loading;
unprompted implicit selection and UI picker visibility were not tested.

| Execution | Prompt excerpt | Observed application and evidence |
| --- | --- | --- |
| A: Article Search | `Use $api-feature-verification to verify existing Article Search.` | Inspected router/controller/repository, contract evidence, fixtures and category exclusions; ran fresh standard tests and separate exploration; parsed XML and compared prior reports. 18 standard passed, 25 legacy skipped; 6 exploratory passed; zero failures/errors. Search accounts for 12 of the 18 active standard tests. |
| B: User persistence | `Use $api-feature-verification to verify existing user creation, lookup, update and relationship persistence.` | Inspected identity/lookup/update and follow direction, fixture isolation, upstream history and diagnostics; executed 6 compatibility tests, all passed, zero skips/failures/errors. Known-defect diagnostics were reviewed, not executed or changed. |

## Reproduction and audit trail

Run from repository root in a fresh Codex session. Explicitly invoke
`$api-feature-verification` with either prompt above and constrain source edits and
publishing as needed. The skill selects checks from the project; it does not embed
these Gradle commands.

Actual commands selected by the two executions (with provided JDK 17 JAVA_HOME):

- A: `./gradlew test searchExploration --offline --no-daemon --rerun-tasks --console=plain`, plus external `--project-cache-dir` and `--init-script` flags to isolate build outputs.
- B: `./gradlew test --tests io.realworld.app.domain.repository.UserRepositoryCompatibilityTest --offline --no-daemon --no-build-cache --rerun-tasks --max-workers=2`, plus external cache/init-script flags and `-Pkotlin.compiler.execution.strategy=in-process`.

Raw traces, exact commands, reports and temporary build redirection scripts remain
outside submission in `../evidence/skill-target-{a,b}.jsonl`,
`article-search-verification-20261104T173748Z/` and `user-persistence-mzxqh9x_/`.
Execution IDs: A `01a1a7a2-9161-7760-8c2c-94ec39d3e9fc`;
B `01a1a7a4-4446-7a73-8292-a1b2e7184f7e`.
The primary agent independently parsed their XML and verified no source/test edits.

## Final local checks and limitations

Primary-agent `./gradlew build --no-daemon --rerun-tasks` passed on Temurin 17.0.21
and 21.0.13. Each report: 43 total, 18 passed, 25 original skips, zero failures or
errors. Exploration and known-defect diagnostic suites are excluded from standard
builds; the existing CI configuration remains unchanged. Logs/XML snapshots are
in `../evidence/skill-final{17,21}*`.

A's sandbox blocked Kotlin daemon marker creation; compilation fell back and the
build/tests succeeded. A also corrected a guessed historical report path before
comparison. These are environment/path issues, not passing feature assertions.
The outer CLI needed authorized escalation after sandbox initialization failed.

Both targets use one repository/framework and JDK 17 for the reuse executions;
portability to another stack, full production HTTP behavior and the quality of
future agent decisions are unproven. Static validation cannot prove workflow
correctness. Search still has unbounded results, ignored pagination and limited
Unicode folding. User follow lookup/unfollow still have upstream reversed
predicates, with three isolated intended-behavior diagnostics; six compatibility
passes do not certify those operations. Twenty-five legacy skips still limit
broader application coverage. No new benchmark or arbitrary CI threshold was added.

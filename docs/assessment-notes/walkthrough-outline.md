# Walkthrough outline (approximately 8 minutes)

1. **0:00–1:00 — Establish scope.** Show fork URL and local feature branch. Explain no final changes have been pushed. Show upstream article stubs and ignored original tests; distinguish the new search capability from missing CRUD.
2. **1:00–2:15 — Follow a request.** Open `web/Router.kt`, `web/controllers/ArticleController.kt`, and `domain/Article.kt` under `src/main/kotlin/io/realworld/app`. Explain exact `/api/articles/search` path, public access, trimmed q, 400 errors envelope and ArticlesDTO.
3. **2:15–3:30 — Explain persistence.** Open `domain/repository/ArticleRepository.kt` and `config/ModulesConfig.kt`. Show real H2 table, bound parameter, title OR body, literal LOCATE and deterministic ordering. State metadata/default and data-population limitations.
4. **3:30–5:00 — Demonstrate tests.** Open `ArticleRepositoryTest.kt` and `ArticleSearchTest.kt`. Run `./gradlew test --tests '*ArticleRepositoryTest' --tests '*ArticleSearchTest' --rerun-tasks` with a configured JDK. Show actual JUnit XML/HTML and current results; do not substitute compilation output for passing tests. Explain original skipped tests.
5. **5:00–6:15 — Build compatibility.** Show `build.gradle`, wrapper properties and journal. Explain unavailable Exposed 0.14.1, the observed H2 linkage failure, and final dependency repair. Distinguish target 16 bytecode from JDK 17/21 runtimes.
6. **6:15–7:15 — CI.** Open `.github/workflows/ci.yml`: matrix, cache, build/test task, reports. Show locally verified results recorded in the journal. Hosted run link remains pending approval to push; do not present a local run as GitHub Actions.
7. **7:15–8:00 — Agent accountability.** Open `AGENT_WORKLOG.md` and the final audit in `engineering-journal.md`. Explain actual mistakes, test-driven corrections, no extra agents/skill, limitations and next steps.

Recording link: **TODO — add walkthrough URL after recording.**

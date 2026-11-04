---
name: api-feature-verification
description: Verify API features and their persistence contracts using repository-native tests, real reports, and baseline comparisons. Use for endpoint implementation verification, regression reviews, and API compatibility assessments.
---

# API feature verification

Use this workflow for the requested endpoint or persistence boundary. Match effort to
its contract and risk; discover tools from the repository rather than assuming a
framework, language, database, or test runner.

## Workflow

1. Read applicable repository instructions, the requested API contract, routing,
   controller/service/persistence paths, existing tests, build manifests and CI.
   Identify response/error conventions, authorization boundaries, and incomplete
   requirements. Distinguish specified behavior from characterization of current
   behavior. Cite the sources that establish the contract.
2. Record the revision, working-tree state, runtime/tool versions and relevant
   baseline command/results before edits. Preserve user changes. If an unchanged
   baseline already has reliable evidence, identify its revision and limitations;
   rerun when evidence is stale or the environment differs.
3. Map behaviors to checks: happy paths, empty/missing/invalid and repeated inputs,
   boundary sizes, ordering/counts/pagination, Unicode/case, serialization,
   authentication/authorization and injection where relevant. Do not invent a
   contract for unspecified behavior; list the question or characterize it separately.
4. Select the smallest useful repository/unit, integration/HTTP and regression
   checks using existing fixtures and tools. Exercise observable behavior with
   deterministic isolated data and cleanup. Include persistence constraints or
   relationship direction when relevant. Avoid dependencies or mock layers that
   bypass the boundary being verified. Add tests only when useful and authorized.
5. Run selected checks with the actual wrapper/build commands and required runtime
   matrix. Retain commands, exit codes and report locations. Inspect test reports:
   count passed, failed, errors, skipped and excluded suites independently; build
   success or an up-to-date task alone does not prove tests executed. Force execution
   with the runner's supported option when fresh results are required.
6. Investigate failures against the baseline and unchanged code/history. Label
   introduced regressions, pre-existing defects, environmental failures and spec
   gaps with evidence. Keep intended-behavior diagnostics separate from release
   tests; never silently skip, weaken assertions or change unrelated production
   behavior to make a run pass. Stop scope expansion at the requested boundary.
7. Assess scalability from query shape, indexes, materialization, payload size and
   resource ownership. If useful, run bounded isolated exploration with dataset,
   environment and warmup context recorded. Measurements are observations, not
   benchmarks or guarantees. Keep exploratory/performance checks separate from
   standard CI when their contract is unsettled; invent no latency thresholds.
8. Report a compact behavior-to-test/evidence mapping, actual commands and counts,
   defects/limitations, missing coverage and justified follow-ups. Distinguish local
   execution from hosted CI and test-host HTTP from a production socket. Claim
   only observed results. Verification does not authorize publishing changes.

## Use

From a Codex session rooted in this repository, explicitly invoke
`$api-feature-verification` with a target and scope. Codex discovers this directory
under `.agents/skills`; a fresh session may be required after installation.
Discovery can also select the skill from its description. To validate activation,
retain a trace showing the agent reading this SKILL.md and applying it; merely
mentioning the skill or passing its instructions in a prompt is insufficient.

Example for future work (not an assertion that this endpoint exists):

> Use $api-feature-verification to verify Popular Articles against its supplied
> contract. Inspect sorting, ties, pagination, empty data and authorization, select
> repository and HTTP checks from this project, and report executed evidence and
> spec gaps. Do not add endpoint semantics or publish changes.

---
phase: quick-261125-bha
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - .github/workflows/ci.yml
  - docs/adr/0010-test-levels-as-jvm-test-suites.md
  - docs/tooling.md
autonomous: true
requirements: [CICD-01, CICD-03, INFRA-12]

estimate:
  tokens: 60000
  raw_tokens: 60000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "On every pull request and every push to main, CI runs `:build-logic:functionalTest` in a job `functional-test` that runs in parallel with `build`, and `ci-success` lists it in `needs`, so a failing TestKit test still fails the single required context `ci-success`"
    - "The Build job's Gradle step is `./gradlew build checkDependencyLicences -x integrationTest -x :build-logic:functionalTest --console=plain`: its dry-run task graph has no `:build-logic:functionalTest` and no `:build-logic:compileFunctionalTestKotlin`, and still has `checkDependencyLicences` of every Kotlin module (T-02-58 / T-02-32: CI names checkDependencyLicences)"
    - "Together the CI Gradle commands still are `check` (D-20): every task in the dry-run graph of `./gradlew check` appears in the union of the dry-run graphs of the `run: ./gradlew ...` lines of ci.yml"
    - "Locally nothing changes (D-19): `./gradlew check` and `./gradlew build` still run `:build-logic:functionalTest`, and build-logic/build.gradle.kts has no commit after a881aac"
    - "The `functional-test` job mirrors the `build` job's setup exactly: `permissions: contents: read`, `persist-credentials: false`, the same SHA-pinned checkout, setup-java, setup-node, mise-action (shfmt and taplo only) and setup-gradle (read-only cache off main, same encryption key), `MISE_DISABLE_TOOLS: java,node`, and on failure it uploads its test reports as `functional-test-reports`"
    - "ADR-0010 and docs/tooling.md describe the three parallel test jobs and the Build command as it now is"
    - "Every step of the CI `lint` job and the full `mise x -- ./gradlew build` are green on the final committed HEAD; zizmor ran online, or offline with the reason reported"
  artifacts:
    - path: ".github/workflows/ci.yml"
      provides: "functional-test job; Build step with -x :build-logic:functionalTest; ci-success needs functional-test"
      contains: "./gradlew :build-logic:functionalTest --console=plain"
    - path: "docs/adr/0010-test-levels-as-jvm-test-suites.md"
      provides: "D-20 bullet, Confirmation and the job-layout pros and cons describing the functional-test job"
      contains: "-x :build-logic:functionalTest"
    - path: "docs/tooling.md"
      provides: "fast-loop paragraph naming the TestKit functional tests and their CI job"
      contains: "TestKit functional tests of `build-logic`"
  key_links:
    - from: ".github/workflows/ci.yml"
      to: "build-logic/build.gradle.kts"
      via: "the task path `:build-logic:functionalTest` (JvmTestSuite `functionalTest` of the included build); Gradle fails with task-not-found if the suite is renamed, so the job cannot silently run nothing"
      pattern: ":build-logic:functionalTest"
    - from: ".github/rulesets/main.json"
      to: ".github/workflows/ci.yml"
      via: "required context `ci-success`, whose `needs` must list every other job of ci.yml, now including functional-test (ADR-0001)"
      pattern: "needs: \\["
---

<objective>
Speed up CI by moving the build logic's TestKit suite out of the `build` job into its own parallel job.

Measured on CI run 45507745588: the Build job took 13m35s, and `:build-logic:functionalTest` (about 230 TestKit tests,
run one fork at a time because every fixture includes the real build-logic) ran from 23:15:36 to 23:24:39, about
9 minutes of it. The other jobs take 2 to 4.5 minutes. After this plan the Build job should drop to about 4 to 5 minutes
and the new job takes about 10 minutes, so the critical path of `ci-success` shrinks by about 3 to 4 minutes.

This extends D-20 (Phase 2 CONTEXT: CI runs parallel test jobs aggregated by `ci-success`) at the author's request
with a third job; D-19 (`check` and `build` run every suite locally) stays exactly as it is, so
build-logic/build.gradle.kts is not touched.

Findings recorded at planning time (live observation at a881aac):
- Required status checks: `.github/rulesets/main.json` requires exactly `ci-success` and `governance-success`; ADR-0001
  says adding a job to ci.yml never changes the ruleset. scripts/apply-github-settings.sh and its test list no job
  names. No other workflow (codeql, governance, labels, scorecard, settings-drift) references CI jobs, and the
  governance shell tests use ci.yml only as a file name. So the only wiring is `ci-success`'s `needs`.
- Tools the suite needs (grep of build-logic/src/functionalTest): FormattingFunctionalTest formats a shell script with
  real shfmt, LineWidthFunctionalTest runs Prettier and taplo, ExecutableOnPathFunctionalTest resolves `node` from the
  test JVM's PATH and fails without it. So the new job needs setup-node and mise with `aqua:mvdan/sh aqua:tamasfe/taplo`,
  exactly as `build` has them.
- `-x :build-logic:functionalTest` works from the root build for the included build: the dry run of the Build command
  with it lists neither `:build-logic:functionalTest` nor `:build-logic:compileFunctionalTestKotlin` (2 without it,
  0 with it), and `./gradlew :build-logic:functionalTest --dry-run` from the root lists the suite. detekt of build-logic
  still analyses `src/functionalTest/kotlin` in the Build job (source set in `detekt { source.from(...) }`); the
  functional-test job compiles those sources.
- Docs that describe the CI jobs: docs/adr/0010-test-levels-as-jvm-test-suites.md (D-20 bullet line 55, Confirmation
  line 102, pros and cons line 124-127) and docs/tooling.md line 74-75. ADR-0010 is not on main yet (it ships with the
  Phase 2 PR #21), so it is edited in place, as quick task 261125-a09 did with ADR-0008. ADR-0012 line 69 and
  docs/tooling.md line 291 ("CI's `build` job names the task") stay true. ADR-0001 and ADR-0006 stay true.
  `.planning/**` is orchestrator-owned and is not edited by the executor (except the SUMMARY).

Decisions taken in this plan (planner discretion):
- Job id `functional-test`, display name `Functional tests (build-logic)`: the id mirrors the suite name the way
  `integration-test` mirrors `integrationTest`; the name says which build the suite belongs to.
- The job is placed directly after `integration-test` in ci.yml and listed right after `build` in `ci-success`'s
  `needs`; timeout 30 minutes like `build` and `integration-test`.
- Report upload path `**/build/reports/**`, retention 7 days, `if-no-files-found: ignore`, the same as the other
  test jobs.
- The ADR keeps its `date` (precedent: 261125-a09 edited ADR-0008 in place without changing it).

Governed paths (governance/governed-paths.txt): `.github/**` is governed, so `.github/workflows/ci.yml` is edited with
the Edit tool only, never with sed, shell redirection or `spotlessApply`. Inside the agent worktree the edit hook stays
silent; the merge hook asks the author once at cleanup. PR #21 already carries the `governance-approved` label.
Not governed: docs/adr/**, docs/tooling.md. Use the file tools for them too.
Out of scope (do not touch): build-logic/** (in particular build-logic/build.gradle.kts and GradleFixture.kt: the
parallelisation of the suite itself is a separate todo the orchestrator records), `.github/rulesets/**`,
scripts/apply-github-settings.sh, governance/**, `.planning/STATE.md`, ROADMAP.md.

Purpose: CICD-01 (CI builds and runs every test level on every push and PR, now faster), CICD-03 (the new job's
actions are SHA-pinned), INFRA-12 (the build logic's TestKit tests stay part of the CI checks).
Output: one ci.yml change, two doc corrections, all CI lint steps and the full build green.
</objective>

<execution_context>
@.claude/gsd-core/workflows/execute-plan.md
@.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md
@.github/workflows/ci.yml

Read only the parts named (line numbers verified at a881aac):
- .github/workflows/ci.yml: the whole file is small; the `build` job 21-82 (env 27-30, mise version 45-50, mise-action
  with its comment 52-57, setup-gradle 59-64, Build step comment 66-72 and run line 73, report upload 75-82), the
  `integration-test` job 84-120 (comment 84-86, upload 114-120), `ci-success` 388-397 (`needs` on 390).
- build-logic/build.gradle.kts 47-95 (the `functionalTest` suite, `maxParallelForks = 1`, `check` dependsOn) — read
  only, never edit.
- docs/adr/0010-test-levels-as-jvm-test-suites.md 53-58 (D-19 and D-20 bullets), 82-86 (Consequences), 97-102
  (Confirmation), 124-127 (pros and cons of the CI job layout).
- docs/tooling.md 74-75 (fast-loop paragraph).

Shell note: every `<automated>` gate below that splits a command line into words is wrapped in `bash -c`, because the
interactive shell on this machine is zsh, which does not word-split an unquoted variable. Gradle rejects a repeated
`--console` option, so the gates append only `--dry-run` to the command lines read from ci.yml (which already carry
`--console=plain`).
</context>

<tasks>

<task type="tracer">
  <name>Task 1: Run the build-logic TestKit suite in its own parallel CI job (one path: exclusion, job, aggregate)</name>
  <files>.github/workflows/ci.yml</files>
  <action>
Edit `.github/workflows/ci.yml` with the Edit tool only (governed path). Three edits, all in this one file, per D-20
(parallel CI test jobs aggregated by `ci-success`) while keeping D-19 (local `check` unchanged):

1. Build job, Gradle step. Change the run line to
   `./gradlew build checkDependencyLicences -x integrationTest -x :build-logic:functionalTest --console=plain`.
   `checkDependencyLicences` stays named on this line (T-02-58 mitigation). Rewrite the first two sentences of the
   step comment above it so they read, wrapped at 120 columns like the rest of the file: "Compile, unit and component
   tests (Testcontainers needs only the runner's Docker), Spotless, detekt, Dokka and the governance checks. The
   integration tests run in the parallel `integration-test` job and the TestKit tests of build-logic in the parallel
   `functional-test` job (D-20); locally, `check` still runs both (D-19). Build Scans are never published from CI."
   Keep the existing `checkDependencyLicences` paragraph of that comment unchanged. Leave the mise-action comment of
   the build job as it is (Spotless in `build` still needs shfmt and taplo).

2. New job `functional-test`, inserted after the `integration-test` job and before `compose-smoke`, with a comment
   block above it in the style of the `integration-test` comment, saying: the TestKit tests of build-logic run in
   their own job so that they run in parallel with `build`; the suite runs its fixtures one at a time because each
   includes the real build-logic, and at about nine minutes it was most of the `build` job; `check` still runs it
   locally (D-19). The job:
   - `name: Functional tests (build-logic)`, `runs-on: ubuntu-latest`, `timeout-minutes: 30`,
     `permissions:` with only `contents: read`;
   - job-level `env` with `MISE_DISABLE_TOOLS: java,node` and the same two-line comment as the build job's env;
   - steps, copied from the build job with identical `uses:` SHAs and version comments and identical `with:` values:
     checkout (`persist-credentials: false`), setup-java (`distribution: temurin`, `java-version-file: .java-version`),
     setup-node (`node-version-file: .nvmrc`), the "Read the mise version" step (id `mise-version`, same script),
     mise-action with `version: ${{ steps.mise-version.outputs.version }}` and `install_args: aqua:mvdan/sh aqua:tamasfe/taplo`,
     preceded by a one-line comment that the fixtures run Spotless with Prettier, taplo and shfmt and resolve node,
     taplo and shfmt from PATH; setup-gradle with `cache-provider: basic`,
     `cache-read-only: ${{ github.ref != 'refs/heads/main' }}`, `cache-encryption-key: ${{ secrets.GRADLE_ENCRYPTION_KEY }}`
     and the same "written from main only" comment;
   - a step `name: Functional tests` with `run: ./gradlew :build-logic:functionalTest --console=plain`;
   - a step `name: Upload functional test reports`, `if: failure()`, the same pinned `actions/upload-artifact` as the
     other jobs, with `name: functional-test-reports`, `path: "**/build/reports/**"`, `retention-days: 7`,
     `if-no-files-found: ignore`.
   Add no other step, no `fetch-depth`, no secret beyond the Gradle cache key, no `continue-on-error`.

3. `ci-success`: change `needs` to
   `[build, functional-test, lint, secrets, qodana, integration-test, compose-smoke, doctor, upstream-records]`.

Keep two-space YAML indentation and Prettier style (`printWidth` 120, `.prettierrc.yaml`); run
`mise x -- ./gradlew spotlessPrettierCheck` and, if it reports ci.yml, fix the reported lines by hand with the Edit
tool. Then commit: `ci: run the build-logic TestKit suite in a parallel functional-test job`.
  </action>
  <verify>
    <automated>mise x -- actionlint && mise x -- ./gradlew spotlessPrettierCheck --console=plain && bash -c 'l=$(sed -nE "s/^ +run: \.\/gradlew (build .*)$/\1/p" .github/workflows/ci.yml); read -r -a a <<< "$l"; o=$(mise x -- ./gradlew "${a[@]}" --dry-run </dev/null 2>&1); echo "$o" | grep -q "^BUILD SUCCESSFUL" && ! echo "$o" | grep -qE "^:build-logic:(functionalTest|compileFunctionalTestKotlin) SKIPPED$" && echo "$o" | grep -qE ":checkDependencyLicences SKIPPED$"' && bash -c 'l=$(sed -nE "s/^ +run: \.\/gradlew (:build-logic:functionalTest .*)$/\1/p" .github/workflows/ci.yml); test -n "$l"; read -r -a a <<< "$l"; mise x -- ./gradlew "${a[@]}" --dry-run </dev/null 2>&1 | grep -qE "^:build-logic:functionalTest SKIPPED$"' && bash -c 'n=$(sed -nE "s/^    needs: \[(.*)\]$/\1/p" .github/workflows/ci.yml | tr -d " " | tr , "\n" | sort); j=$(sed -nE "/^jobs:$/,$ s/^  ([a-z][a-z-]*):$/\1/p" .github/workflows/ci.yml | grep -vx ci-success | sort); test -n "$j" && test "$n" = "$j"' && bash -c 't() { mise x -- ./gradlew "$@" --dry-run </dev/null 2>&1 | sed -nE "s/^(:[^ ]+) SKIPPED$/\1/p" | sort -u; }; d=$(mktemp -d); mise x -- ./gradlew check --dry-run --console=plain </dev/null 2>&1 | sed -nE "s/^(:[^ ]+) SKIPPED$/\1/p" | sort -u > "$d/check"; : > "$d/ci"; while IFS= read -r l; do read -r -a a <<< "$l"; t "${a[@]}" >> "$d/ci"; done < <(sed -nE "s/^ +run: \.\/gradlew (.*)$/\1/p" .github/workflows/ci.yml); sort -u -o "$d/ci" "$d/ci"; test -s "$d/check"; ! comm -23 "$d/check" "$d/ci" | grep .'</automated>
    Baselines of the automated chain, measured at a881aac (value = what the named count printed):
    - Build run line read from ci.yml, dry run, lines matching `^:build-logic:(functionalTest|compileFunctionalTestKotlin) SKIPPED$` (second command) [HEAD=2 EXPECT==0 @a881aac]
    - same dry run, lines matching `:checkDependencyLicences SKIPPED$` [HEAD=9 EXPECT>=1 @a881aac GUARD MUTATION: removing the `courses.licences` apply line from build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts]
    - functional-test run line read from ci.yml, dry run lists `:build-logic:functionalTest` (third command) [HEAD=absent EXPECT==present @a881aac]
    - number of entries in `ci-success`'s `needs`, which the fourth command requires to equal the set of other job ids [HEAD=8 EXPECT==9 @a881aac]
    - tasks of the `check` dry-run graph missing from the union of the CI `run: ./gradlew` graphs (fifth command) [HEAD=0 EXPECT==0 @a881aac GUARD MUTATION: deleting the functional-test run line from ci.yml after adding the Build exclusion, which leaves the suite's tasks missing]
    Grep gates on ci.yml (comment lines filtered), run from the checkout root:
    - `grep -cE '^  functional-test:$' .github/workflows/ci.yml` [HEAD=0 EXPECT==1]
    - `grep -cE '^ +run: \./gradlew build checkDependencyLicences -x integrationTest -x :build-logic:functionalTest --console=plain$' .github/workflows/ci.yml` [HEAD=0 EXPECT==1]
    - `grep -cE '^ +run: \./gradlew :build-logic:functionalTest --console=plain$' .github/workflows/ci.yml` [HEAD=0 EXPECT==1]
    - `grep -cE '^    needs: \[build, functional-test, ' .github/workflows/ci.yml` [HEAD=0 EXPECT==1]
    - `grep -c 'name: functional-test-reports' .github/workflows/ci.yml` [HEAD=0 EXPECT==1]
    - `grep -vE '^\s*#' .github/workflows/ci.yml | grep -cF 'actions/setup-node@949feb2413d6458794dcd2491c4babbbce0c15c1'` [HEAD=3 EXPECT==4]
    - `grep -vE '^\s*#' .github/workflows/ci.yml | grep -cF 'jdx/mise-action@90f833d396d0cc3674b631d2d225e306476c303a'` [HEAD=4 EXPECT==5]
    - `grep -vE '^\s*#' .github/workflows/ci.yml | grep -cF 'install_args: aqua:mvdan/sh aqua:tamasfe/taplo'` [HEAD=1 EXPECT==2]
    - `grep -vE '^\s*#' .github/workflows/ci.yml | grep -cF 'persist-credentials: false'` [HEAD=8 EXPECT==9]
    - `grep -vE '^\s*#' .github/workflows/ci.yml | grep -cF '      contents: read'` [HEAD=8 EXPECT==9]
    - `grep -vE '^\s*#' .github/workflows/ci.yml | grep -cF 'MISE_DISABLE_TOOLS: java,node'` [HEAD=4 EXPECT==5]
    - `grep -vE '^\s*#' .github/workflows/ci.yml | grep -cF "cache-read-only: \${{ github.ref != 'refs/heads/main' }}"` [HEAD=4 EXPECT==5]
    - `grep -vE '^\s*#' .github/workflows/ci.yml | grep -cF 'actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9'` [HEAD=3 EXPECT==4]
    D-19 gates:
    - `mise x -- ./gradlew check --dry-run --console=plain </dev/null 2>&1 | grep -cE '^:build-logic:functionalTest SKIPPED$'` [HEAD=1 EXPECT==1 @a881aac GUARD MUTATION: deleting `tasks.named("check") { dependsOn("functionalTest") }` from build-logic/build.gradle.kts]
    - `git rev-list --count a881aac..HEAD -- build-logic/build.gradle.kts` [HEAD=0 EXPECT==0 @a881aac GUARD MUTATION: any commit that edits build-logic/build.gradle.kts]
  </verify>
  <done>ci.yml has the `functional-test` job with the build job's exact setup, the Build step excludes
`:build-logic:functionalTest` and still names `checkDependencyLicences`, `ci-success` needs the new job, every gate
above meets its expectation, and the change is one commit.</done>
</task>

<task type="auto">
  <name>Task 2: Describe the functional-test job in ADR-0010 and docs/tooling.md</name>
  <files>docs/adr/0010-test-levels-as-jvm-test-suites.md, docs/tooling.md</files>
  <action>
Edit with the Edit tool; wrap prose at 120 columns like the surrounding text; keep the ADR front matter (including its
`date`) unchanged.

docs/adr/0010-test-levels-as-jvm-test-suites.md, three places (per D-20, D-19):
1. Decision Outcome, the bold D-20 bullet (line 55-56): replace the whole bullet with one that reads: "**Parallel CI
   jobs (D-20).** `build` runs `./gradlew build checkDependencyLicences -x integrationTest -x :build-logic:functionalTest`,
   `integration-test` runs `./gradlew integrationTest` and `functional-test` runs `./gradlew :build-logic:functionalTest`;
   `ci-success` aggregates all three, so together they are `check`. The TestKit suite of `build-logic` got its own job
   on 2026-11-25: it runs its fixtures one at a time and took about 9 of the 13.5 minutes of the `build` job (CI run
   45507745588). Locally, `check` still runs it (D-19)."
2. Confirmation, the bullet about `ci.yml` (line 102): it reads that `ci.yml` has the three jobs and the `ci-success`
   aggregate, which makes all three required through ADR-0001.
3. Pros and cons, section "One CI job, or one job per suite" (line 126-127): keep the first sentence about a single
   job and a job per suite; replace the closing sentence about the Axon Server boundary with: "Parallel jobs split the
   cost where the cost is: at the Axon Server boundary, and at the serial TestKit suite of `build-logic`, which alone
   was about two thirds of a single `build` job."
Leave the Considered Options line and the Consequences bullets as they are (still true).

docs/tooling.md, fast-loop paragraph (line 74-75): keep the first sentence part about `./gradlew test` and
`./gradlew componentTest`; the rest reads that `./gradlew check` and `./gradlew build` run every suite, including the
integration tests and the TestKit functional tests of `build-logic` (CI runs each of these in a parallel job of its
own). Nothing else in docs/tooling.md changes.

Run `mise x -- ./gradlew checkAdrs spotlessPrettierCheck --console=plain`; for these two non-governed files a reported
Prettier diff may be fixed with the Edit tool (no `spotlessApply`, because it would also touch governed files).
Commit: `docs: describe the parallel functional-test CI job in ADR-0010 and docs/tooling.md`.
  </action>
  <verify>
    <automated>mise x -- ./gradlew checkAdrs spotlessPrettierCheck --console=plain</automated>
    Grep gates, run from the checkout root:
    - `grep -c -- '-x :build-logic:functionalTest' docs/adr/0010-test-levels-as-jvm-test-suites.md` [HEAD=0 EXPECT==1]
    - `grep -c 'functional-test' docs/adr/0010-test-levels-as-jvm-test-suites.md` [HEAD=0 EXPECT>=1]
    - `grep -c 'has the three jobs' docs/adr/0010-test-levels-as-jvm-test-suites.md` [HEAD=0 EXPECT==1]
    - `grep -ciE 'two (parallel )?(ci )?jobs|both required|aggregates both' docs/adr/0010-test-levels-as-jvm-test-suites.md` [HEAD=4 EXPECT==0]
    - `grep -c 'TestKit functional tests of `build-logic`' docs/tooling.md` [HEAD=0 EXPECT==1]
    - `grep -c 'CI runs those in their own job' docs/tooling.md` [HEAD=1 EXPECT==0]
  </verify>
  <done>ADR-0010 states the three parallel jobs, the current Build command, the reason and the measurement, and its
Confirmation and pros and cons agree; docs/tooling.md names the TestKit suite and its CI job; checkAdrs and the
Prettier check pass; one commit.</done>
</task>

<task type="auto">
  <name>Task 3: Run every CI lint-job step and the full build on the committed HEAD</name>
  <files>.github/workflows/ci.yml, docs/adr/0010-test-levels-as-jvm-test-suites.md, docs/tooling.md</files>
  <action>
Gate run only; the files above change only if a gate reports a problem in them (fix with the Edit tool, governed
ci.yml included, then commit as `fix: ...` naming the gate). From the worktree root:

1. Delete the untracked Swift PM cache directory `.build/` if it exists (a lefthook hook can create it); it must
   never be committed (`git ls-files .build` stays empty).
2. Run every `run:` step of the `lint` job of ci.yml (as of this branch), in a bash shell whose PATH has the mise
   tools (`eval "$(mise env -s bash)"`), each verbatim and in order: actionlint; zizmor `--no-progress
   .github/workflows` online with `GH_TOKEN` from `gh auth token` (if the sandbox refuses the token or the network,
   run `zizmor --offline --no-progress .github/workflows` instead and report that the online audits, impostor commits
   and ref confusion, were not run locally and are left to CI); shellcheck over `git ls-files -z '*.sh'`;
   editorconfig-checker; `reuse lint`; the line-ending renormalisation check on a temporary copy of the index (copy the
   step's script exactly, it never touches the real index); `renovate-config-validator --strict renovate.json5`;
   `docker compose config --quiet`; and every shell test from `git ls-files -z -- 'scripts/test-*.sh'
   'scripts/*/test-*.sh'`, each with bash.
3. Run the full `mise x -- ./gradlew build` (it includes `:build-logic:functionalTest` and the integration tests, so it
   takes about 15 minutes; run it in the background and wait for it). It must end with BUILD SUCCESSFUL.
4. Re-run the Task 1 automated gate once more on the final HEAD.

Record in the SUMMARY: each lint step with its exit status, whether zizmor ran online or offline and why, the build
duration and result, the Task 1 gate results, and the expected CI effect (Build job without the TestKit suite, new
`Functional tests (build-logic)` job). Qodana is not run locally: this plan changes no Kotlin and only mirrors YAML
that Qodana already accepts in ci.yml; CI runs it on push.
  </action>
  <verify>
    <automated>mise x -- actionlint && mise x -- editorconfig-checker && mise x -- reuse lint && mise x -- renovate-config-validator --strict renovate.json5 && docker compose config --quiet && test -z "$(git ls-files .build)" && mise x -- ./gradlew build --console=plain</automated>
    Plus the full lint list of the action (zizmor, shellcheck, renormalisation, shell tests), each recorded with its
    exit status in the SUMMARY. Baseline: every lint step and the full build were green at a881aac (quick task
    261125-a09). After the last commit, `git status --short --untracked-files=no` prints nothing.
  </verify>
  <done>Every lint-job step exits 0 (zizmor online, or offline with the reason recorded), the full build is green on
the final committed HEAD, `.build/` is not tracked, and the SUMMARY records each result.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| pull request -> CI runner | A pull request's tree runs the new job with a read-only token and a read-only Gradle cache |
| ci.yml -> required context | `ci-success` is the only CI context the `main` ruleset requires; a job it does not need is not enforced |
| governed file edit -> main | ci.yml is governed; the PR needs `governance-approved`, the merge hook asks the author |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-bha-01 | Elevation of Privilege | `ci-success` needs list | high | mitigate | Task 1 adds `functional-test` to `needs`; the needs gate fails when any job of ci.yml is missing from it, so TestKit failures still block merges |
| T-bha-02 | Tampering | coverage of `check` by CI commands | high | mitigate | Task 1 gate compares the dry-run graph of `check` with the union of the CI `run: ./gradlew` graphs and must find nothing missing; Gradle fails on an unknown task path, so a renamed suite cannot make the new job run nothing |
| T-bha-03 | Tampering | licence gate (T-02-32, T-02-58) | high | mitigate | `checkDependencyLicences` stays named on the Build run line; the Build-graph gate asserts the task is still in the graph |
| T-bha-04 | Tampering | Gradle cache of the new job | medium | mitigate | `cache-read-only` off main and the same encryption key, exactly as the other jobs; zizmor audits the workflow (cache poisoning, permissions, pins) |
| T-bha-05 | Information Disclosure | job token and uploaded reports | low | mitigate | `permissions: contents: read`, `persist-credentials: false`, SHA-pinned actions only; the reports hold TestKit output of temporary fixtures, no secret is passed to Gradle beyond setup-gradle's cache key, 7-day retention |
| T-bha-06 | Tampering | governed `.github/workflows/ci.yml` | low | mitigate | Edit tool only, no sed, redirection or spotlessApply; PR #21 carries `governance-approved`; the merge hook lists ci.yml for the author |
| T-bha-07 | Repudiation | ADR-0010 and docs/tooling.md drift from ci.yml | low | mitigate | Task 2 updates both with positive and negative grep gates |
| T-bha-SC | Tampering | npm/pip/cargo installs | high | mitigate | no package-manager install and no dependency change in this plan, so the package legitimacy gate has nothing to audit |
</threat_model>

<verification>
Run from the checkout root after all commits:
- the Task 1 automated gate passes (Build graph without the suite and with the licence check, the functional-test
  command graph, `needs` completeness, `check` fully covered by the CI commands)
- every `lint` job step listed in Task 3 exits 0
- `mise x -- ./gradlew build` succeeds
- `git diff --quiet a881aac -- build-logic .github/rulesets scripts governance .claude renovate.json5 gradle/libs.versions.toml`
  exits 0 (nothing outside the plan's files changed)
</verification>

<success_criteria>
- Two commits (ci.yml; ADR-0010 and docs/tooling.md), plus a gate-fix commit only if a gate needed one
- The SUMMARY quotes the gate results, the lint step results, zizmor online or offline with the reason, the build
  result and duration, and names the governed file `.github/workflows/ci.yml`
</success_criteria>

<orchestrator_notes>
1. Merge: `.github/workflows/ci.yml` is governed, so the merge hook asks the author once at cleanup. Read
   `git diff HEAD...<branch> -- .github/workflows/ci.yml` before the author confirms.
2. After the push, check the next CI run of PR #21: the `Build` job no longer runs `:build-logic:functionalTest` and
   should take about 4 to 5 minutes; `Functional tests (build-logic)` runs about 10 minutes and passes; `ci-success`
   waits for it. Its Gradle cache may be cold until the job has run once on main, because the cache is written from
   main only.
3. `.planning/STATE.md`, section `### Pending Todos`, add this bullet (exact text, one line):

```markdown
- [Before Phase 3, CI and local speed]: Parallelise the build-logic TestKit suite itself: switch the fixtures from `includeBuild` of the real build-logic (`GradleFixture.writeSettings`) to `withPluginClasspath()` or a pre-built plugin, then set `maxParallelForks > 1` on `functionalTest` in `build-logic/build.gradle.kts` (governed). Expected about 9 -> 3-4 minutes, locally and in the CI `functional-test` job (quick task 261125-bha moved the suite into that parallel job).
```

4. Optionally record under `### Decisions`: "- [Quick 261125-bha]: D-20 extended at the author's request: CI runs the
   build-logic TestKit suite in a third parallel job `functional-test` (needed by `ci-success`); the `build` job
   excludes it with `-x :build-logic:functionalTest` and still names `checkDependencyLicences`; local `check` is
   unchanged (D-19)". Add the usual row for 261125-bha to "Quick Tasks Completed".
</orchestrator_notes>

<output>
Create `.planning/quick/261125-bha-move-build-logic-functionaltest-into-its/261125-bha-SUMMARY.md` when done
</output>

---
phase: quick-261122-kqn
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - qodana.yaml
  - scripts/qodana.sh
  - .github/workflows/ci.yml
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/PrTemplateCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ToolingTableCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ConfigPolicyCheck.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ExecutableOnPathFunctionalTest.kt
  - renovate.json5
  - .github/labels.yml
  - docs/tooling.md
  - .planning/PROJECT.md
  - .planning/ROADMAP.md
autonomous: true
requirements: [CICD-01, CICD-04, INFRA-09, INFRA-14, INFRA-16, GH-11]

estimate:
  tokens: 110000
  raw_tokens: 110000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "scripts/qodana.sh, run with no argument, exports the committed HEAD and runs the digest-pinned Qodana image from qodana.yaml with exactly the flags CI uses; on the tree after this plan it exits 0 and the SARIF report holds 0 results"
    - "Before the Kotlin fixes the same run exits 255 and reports exactly the six known Kotlin findings: nothing from .editorconfig and nothing under any build output directory, so the exclusions and failThreshold 0 are proven to work"
    - "The CI workflow has a `qodana` job that runs scripts/qodana.sh on the checkout, restores a Qodana cache on every run and saves it from main only, and `ci-success` needs it, so any Qodana problem blocks merges to main"
    - "The Qodana image is pinned in exactly one place, the `linter:` line of qodana.yaml as tag@digest; Renovate extracts it with tag and digest, labels it `deps` and `deps:docker`, allows year.minor tags only, never automerges it, and its PR body reminds to move IntelliJ IDEA to the same year.minor"
    - "The six Kotlin findings are resolved without changing behaviour or messages: three collection builders, one `when` subject, one inlined test-helper parameter, and one `@Suppress` with a reason comment; ./gradlew build passes, including every governance TestKit test"
    - "docs/tooling.md lists Qodana and IntelliJ IDEA with their single version source and states the IDE parity rule and the local run; PROJECT.md records the decision including the cooldown exception; the ROADMAP Phase 12 research line no longer lists the Qodana topic"
    - "actionlint, zizmor, shellcheck, renovate-config-validator --strict, reuse lint and editorconfig-checker all pass"
  artifacts:
    - path: "qodana.yaml"
      provides: "Qodana configuration: the image pin, profile Default, failThreshold 0, build-output and EditorConfig exclusions"
      contains: "linter: jetbrains/qodana-jvm-community:2026.3@sha256:5420d961a4f3a850adc47fc6565916eb6fe558f3205f511ac7bc75b98812c37e"
    - path: "scripts/qodana.sh"
      provides: "The one way to run the gate, used by CI and locally"
      contains: "idea.headless.enable.statistics=false"
    - path: ".github/workflows/ci.yml"
      provides: "Required `qodana` job aggregated by ci-success"
      contains: "needs: [build, lint, secrets, qodana]"
    - path: "renovate.json5"
      provides: "Regex custom manager for the qodana.yaml linter tag and digest, docker label rule, Qodana parity rule"
      contains: "jetbrains/qodana-jvm-community"
    - path: ".github/labels.yml"
      provides: "Ecosystem label for container image updates"
      contains: "deps:docker"
    - path: "docs/tooling.md"
      provides: "Qodana and IntelliJ IDEA inventory rows, IDE parity rule, local run"
      contains: "scripts/qodana.sh"
    - path: ".planning/PROJECT.md"
      provides: "Key Decisions row for the Qodana gate"
      contains: "Qodana JVM Community"
  key_links:
    - from: ".github/workflows/ci.yml"
      to: "scripts/qodana.sh"
      via: "the qodana job's run step calls scripts/qodana.sh with the checkout directory and QODANA_WORK_DIR under runner.temp"
    - from: "scripts/qodana.sh"
      to: "qodana.yaml"
      via: "reads the `linter:` value and accepts only jetbrains/qodana-jvm-community:<year.minor>@sha256:<64 hex>"
    - from: "renovate.json5"
      to: "qodana.yaml"
      via: "regex custom manager with datasource docker captures depName, currentValue and currentDigest of the linter line"
    - from: ".github/workflows/ci.yml"
      to: ".github/workflows/ci.yml"
      via: "ci-success `needs` includes qodana, and its jq check fails on any result other than success"
---

<objective>
Adopt Qodana JVM Community as a required CI gate for the inspections that only IntelliJ IDEA reports (author decision 2026-11-22), starting at zero findings with no baseline.

Purpose: the compiler, detekt and ktfmt cannot see IDE inspections such as unstable API usage, collection-builder or `when`-subject suggestions. A Qodana run with profile `Default` on the image whose year.minor equals the author's IDE (IntelliJ IDEA 2026.3) reproduces exactly what the IDE shows, so the gate keeps the tree as clean as the IDE view.

Output: qodana.yaml, scripts/qodana.sh, a `qodana` job joined to `ci-success`, the 6 Kotlin findings resolved, the Renovate manager and rules, the `deps:docker` label, docs/tooling.md rows and section, the PROJECT.md decision row and the ROADMAP Phase 12 research line without the Qodana topic.

Design decisions taken in this plan (rationale for the executor and the SUMMARY):

- Image pin location. PROJECT.md's version decision puts in the Gradle catalog "everything Gradle consumes (incl. tools, container images)". Gradle does not consume the Qodana image, so the pin lives in Qodana's own native place, the `linter:` key of qodana.yaml, as `tag@digest`: the same pattern as mise.toml for CLI tools. scripts/qodana.sh reads it (CI and local), and a Renovate regex custom manager updates tag and digest together (INFRA-09, CICD-04). A planning probe on the 2026.3 image confirmed that the in-container CLI accepts a digest in `linter:` and prints "Loaded Qodana Configuration".
- Plain `docker run` through one script, not JetBrains/qodana-action. The action downloads a separate CLI binary, defaults to PR-only analysis, comments and annotations, and adds a second supply-chain artefact. A script gives full control over `--user`, the mounts and the statistics opt-out. It also lets the local run and CI run the identical command.
- Cooldown exception. The `2026.3` tag was last pushed 2026-11-19, under the seven-day `minimumReleaseAge` (CICD-04). The IDE parity rule wins. The author approved this exception on 2026-11-22, and it is recorded in the PROJECT.md row. Renovate keeps the cooldown for every later update.
- No SPDX line in qodana.yaml. Per 01-PATTERNS.md ("`.sh` files and YAML are covered by `REUSE.toml` aggregate annotations"), and like every YAML file in the repository (ci.yml, lefthook.yml, .prettierrc.yaml, labels.yml), the file starts with a descriptive comment. REUSE.toml's `**` annotation covers it. scripts/qodana.sh does get the Spotless-managed `# SPDX-License-Identifier: MIT` line 2.
- governance/governed-paths.txt stays unchanged, so checkClaudeAskRules needs no sync. qodana.yaml and scripts/qodana.sh configure a code-quality gate, like config/detekt/detekt.yml, which is outside the governance boundary. They cannot rewrite policy or stop a governance check. The `qodana` job and its place in `ci-success` live in `.github/**`, which is governed. The image a PR could select is limited to digests of the JetBrains image by the script's pattern check. The pull request will still need the `governance-approved` label because it touches `.github/**`, renovate.json5 and `build-logic/.../governance/**`. The author applies that label. Claude Code also asks before each edit of those governed files, and an answered prompt is expected, not a failure.
- Qodana writes `.idea/` into the directory it analyses, which the planning probe confirmed. The script therefore never analyses the developer's working tree. With no argument it exports HEAD, and it refuses an explicit directory that already contains `.idea/`.
</objective>

<execution_context>
@/Users/afanasyev/Development/axon5-dcb-courses/.claude/gsd-core/workflows/execute-plan.md
@/Users/afanasyev/Development/axon5-dcb-courses/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@/Users/afanasyev/Development/axon5-dcb-courses/.claude/worktrees/qodana-gate/.planning/STATE.md
@/Users/afanasyev/Development/axon5-dcb-courses/.claude/worktrees/qodana-gate/.claude/CLAUDE.md

Facts verified at planning time (2026-11-22, on the local 2026.3 image `sha256:5420d961…`):
- The in-container CLI is Qodana CLI 2026.3.442. The first argument must be the command `scan`, because the bare-flag syntax is deprecated and will be rejected. Useful flags: `--print-problems` and `--property=idea.headless.enable.statistics=false` (disables usage statistics). `--no-statistics` applies to clang and dotnet only.
- The image's `QodanaYamlConfig` knows `exclude`, `include`, `failThreshold`, `runPromoInspections` and `disableSanityInspections`. Its scope entries have `name` and `paths`. `failThreshold: 0` in qodana.yaml produced "Failure condition triggered" and exit code 255 with one problem. A clean run exits 0.
- The image runs fine with `--user "$(id -u):$(id -g)"`. `/root` is mode 777 and the entrypoint adds a passwd entry. The Gradle user home lives under `/data/cache/gradle`, the IDE caches under `/data/cache/idea`.
- colima shares only `$HOME` with its VM by default. A directory under `/var/folders` (mktemp) is mounted empty, and Qodana then reports "No files to check".
- Warm-cache run on the full tree: about 10 minutes. Cold run including the image pull: about 35 minutes. Run it in the background and wait for the completion notification, because a foreground Bash call is capped at 10 minutes.
- Parity-run findings (SARIF ruleId and location): EditorConfigOptionRedundancy at .editorconfig lines 20, 36, 43, 46, 50 and 54; SameParameterValue at ExecutableOnPathFunctionalTest.kt:231; CollectionConcatenationToBuildCollection at AskRulesCheck.kt:86, PrTemplateCheck.kt:46 and ToolingTableCheck.kt:45; IntroduceWhenSubject at ConfigPolicyCheck.kt:95; ConvertCallChainIntoSequence at ToolingTableCheck.kt:88.
- actions/cache v6.1.0 (published 2026-06-26, past the cooldown) resolves to commit `55cc8345863c7cc4c66a329aec7e433d2d1c52a9` (`gh api repos/actions/cache/commits/v6.1.0 --jq .sha`). Its restore action outputs `cache-hit` and `cache-primary-key`.
- `LOG_LEVEL=debug mise x -- renovate --platform=local --dry-run=extract` runs in this repository with exit 0 and prints every extracted dependency with `packageFile`, `depName`, `currentValue` and `currentDigest`.

Qodana work directory, outside the repository by design (colima shares only `$HOME`; its `cache/` is already warm): `/Users/afanasyev/.claude/jobs/00d88943/tmp/qodana`. Export it as `QODANA_WORK_DIR` for every local Qodana run in this plan.

mise: if mise reports the parent `mise.toml` as untrusted, prefix every `mise x -- …` command with `MISE_TRUSTED_CONFIG_PATHS="$(cd ../../.. && pwd)/mise.toml"`, which is the main checkout's `mise.toml` as seen from this worktree's root.

Git: work on the current branch `gsd/phase-01-qodana-gate` only. Do not create or switch branches, do not push, do not amend. Fix-ups are new commits.
</context>

<tasks>

<task type="tracer">
  <name>Task 1: Tracer — qodana.yaml, scripts/qodana.sh and the required CI job, proven by a real run that catches exactly the six Kotlin findings</name>
  <files>qodana.yaml, scripts/qodana.sh, .github/workflows/ci.yml</files>
  <precondition>The Docker engine (colima) answers `docker info`, and `docker image inspect jetbrains/qodana-jvm-community@sha256:5420d961a4f3a850adc47fc6565916eb6fe558f3205f511ac7bc75b98812c37e` succeeds. If the image is missing, the first run pulls about 4 GB.</precondition>
  <action>
1. Create `qodana.yaml` at the repository root. Indent with 2 spaces and keep lines within 120 columns. Write no SPDX line, as explained in the objective. The file starts with a comment block saying that it configures the required `qodana` CI gate, that scripts/qodana.sh runs it locally and in CI, and that docs/tooling.md explains the IDE parity rule. Keys, in this order, each with a short reason comment above it:
   - `version: "1.0"`
   - `linter: jetbrains/qodana-jvm-community:2026.3@sha256:5420d961a4f3a850adc47fc6565916eb6fe558f3205f511ac7bc75b98812c37e`. Write the value unquoted, on one line, because the script and the Renovate manager both parse exactly this form. The comment says this is the only place the image is pinned, that Renovate updates tag and digest together, and that the tag's year.minor must equal the IntelliJ IDEA release used for development.
   - `profile:` with `name: Default`. The comment says this is the IDE's own default profile, so Qodana reports what the IDE shows, while `qodana.recommended` hides IDE-default inspections.
   - `failThreshold: 0`. The comment says any problem fails the gate and there is no baseline.
   - `runPromoInspections: false`. The comment says only the profile's inspections run, for IDE parity.
   - `exclude:` with two entries.
     - `name: All` with `paths: [build, build-logic/build, platform/build, tooling/convention-sample/build, .gradle, build-logic/.gradle, .kotlin, build-logic/.kotlin]`. The comment says these are build output and Gradle-generated Kotlin DSL accessors (240 findings, none in our code), and that a new module adds its build directory here. The gate fails closed: a missing entry shows up as findings in generated code and never hides real ones.
     - `name: EditorConfigOptionRedundancy` with `paths: [.editorconfig]`. The comment says the repeated values are deliberate, because INFRA-14 requires `.editorconfig` to define settings for every file type explicitly.
   Do not edit `.editorconfig`.

2. Create `scripts/qodana.sh`, executable (`chmod +x`, so git records mode 100755), in the style of `scripts/format-staged.sh`:
   - Shebang `#!/usr/bin/env bash`. Line 2 is `# SPDX-License-Identifier: MIT`.
   - Then a header comment covering:
     - what it does: runs the Qodana gate exactly as CI does;
     - usage: `scripts/qodana.sh [project-dir]`;
     - with no argument: it exports the committed HEAD into `<work dir>/project` and analyses that, so uncommitted changes are not analysed. Qodana writes `.idea/` into the analysed directory, so the IDE's working tree is never used;
     - with an argument: it analyses that directory as it is and refuses one that already contains `.idea/`. CI passes its fresh checkout;
     - `QODANA_WORK_DIR`: defaults to `${XDG_CACHE_HOME:-$HOME/.cache}/axon5-dcb-courses/qodana` and holds `cache/`, `results/` and `project/`. On colima it must be under `$HOME`;
     - exit status: Qodana's own, 0 when clean and 255 when problems exceed `failThreshold`.
   - Behaviour, with `set -euo pipefail`:
     - Resolve the repository root from the script's own location.
     - Compute the work dir.
     - Choose the project dir. With no argument, run `rm -rf -- "<work dir>/project"`, recreate it, pipe `git -C <root> archive --format=tar HEAD` into `tar -x -C <work dir>/project`, and print to stderr which short HEAD is analysed. With one argument, resolve it to an absolute path and fail when `<dir>/.idea` exists. With more arguments, print usage and exit 2.
     - Fail when `<project dir>/qodana.yaml` is missing.
     - Read the linter with `sed -nE` on the `^linter:` line, taking the first whitespace-free token.
     - Validate it against a readonly pattern `^jetbrains/qodana-jvm-community:[0-9]{4}\.[0-9]+@sha256:[0-9a-f]{64}$`, used unquoted on the right of `=~`. On a mismatch print a fixed message, `qodana.yaml must pin linter as jetbrains/qodana-jvm-community:<year.minor>@sha256:<digest>`, to stderr and exit 1. Never echo the value read: in CI it is pull-request content, and echoing it could inject workflow commands.
     - Only after these checks, fail with a hint pointing to docs/tooling.md when `docker` is not on PATH.
     - `rm -rf` and recreate `<work dir>/results`, and create `<work dir>/cache`.
     - Finally `exec docker run --rm --user "$(id -u):$(id -g)" --volume "<project dir>:/data/project" --volume "<work dir>/cache:/data/cache" --volume "<work dir>/results:/data/results" "<image>" scan --print-problems --property=idea.headless.enable.statistics=false`.
     - Pass no `-e` variables and no tokens. There is no Qodana Cloud and nothing is uploaded.
   - Make every message go to stderr with a `qodana:` prefix.
   - The script must be shellcheck-clean and shfmt-formatted (`spotlessApply` formats it).

3. Edit `.github/workflows/ci.yml`, following its hardening conventions exactly.
   - Line 3 comment: say the workflow also runs the Qodana IDE inspections.
   - Add a job `qodana` (name `Qodana`) between `secrets` and `ci-success`, with `runs-on: ubuntu-latest`, `timeout-minutes: 60`, `permissions: contents: read`, and no `env` with secrets. Above the job, a comment says:
     - it runs the IntelliJ IDEA inspections of profile Default over the whole tree, and any problem fails it (qodana.yaml);
     - the image is pinned once, in qodana.yaml, and scripts/qodana.sh runs it exactly as a local run does;
     - the cache holds Qodana's IDE caches and the container's Gradle home and, like the Gradle cache, is written from main only.
   - Steps:
     a. `actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1` with `persist-credentials: false`.
     b. `id: qodana-cache` using `actions/cache/restore@55cc8345863c7cc4c66a329aec7e433d2d1c52a9 # v6.1.0`. Before using the SHA, re-confirm it with `mise x -- gh api repos/actions/cache/commits/v6.1.0 --jq .sha`. Set `path: ${{ runner.temp }}/qodana/cache`, `key: qodana-${{ runner.os }}-${{ hashFiles('qodana.yaml', 'gradle/libs.versions.toml', 'gradle/wrapper/gradle-wrapper.properties', '.java-version', '**/*.gradle.kts') }}` and `restore-keys: qodana-${{ runner.os }}-`.
     c. Name `Run Qodana`, with `env: QODANA_WORK_DIR: ${{ runner.temp }}/qodana` and `run: scripts/qodana.sh "${GITHUB_WORKSPACE}"`. No `${{ }}` inside the run body.
     d. `actions/cache/save@55cc8345863c7cc4c66a329aec7e433d2d1c52a9 # v6.1.0` with `if: github.ref == 'refs/heads/main' && steps.qodana-cache.outputs.cache-hit != 'true'`, the same `path`, and `key: ${{ steps.qodana-cache.outputs.cache-primary-key }}`.
     e. On `if: failure()`, `actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9 # v7.0.2` with name `qodana-results`, `path: ${{ runner.temp }}/qodana/results`, `retention-days: 7` and `if-no-files-found: ignore`.
   - Change `ci-success` to `needs: [build, lint, secrets, qodana]` and leave its aggregation step unchanged, so skipped, cancelled and failed all count as failure.

4. Run `mise x -- ./gradlew spotlessApply --console=plain`. Then run the static checks in `<verify>` and commit the task (`ci(qodana): …`). Then run the real gate in the background: `QODANA_WORK_DIR=/Users/afanasyev/.claude/jobs/00d88943/tmp/qodana scripts/qodana.sh > /Users/afanasyev/.claude/jobs/00d88943/tmp/qodana/task1.log 2>&1; echo "exit=$?" >> /Users/afanasyev/.claude/jobs/00d88943/tmp/qodana/task1.log`. Expect exit 255 and exactly the six Kotlin findings listed in `<context>`.
   - If a finding appears in a file this task created or changed, fix it in that file. If it is a schema warning on the optional `runPromoInspections` key, drop the key. Then re-commit and re-run.
   - If a finding appears anywhere else, or an EditorConfigOptionRedundancy finding or a path under a build directory shows up, stop and report it. That would mean the exclusions do not work as planned.
  </action>
  <verify>
    <automated>mise x -- shellcheck scripts/qodana.sh && mise x -- actionlint && mise x -- zizmor --offline --no-progress .github/workflows && test -x scripts/qodana.sh && test "$(grep -c 'needs: \[build, lint, secrets, qodana\]' .github/workflows/ci.yml)" -ge 1 && test "$(grep -c 'actions/cache/save@55cc8345863c7cc4c66a329aec7e433d2d1c52a9' .github/workflows/ci.yml)" -ge 1 && d="$HOME/.cache/qodana-guard-test" && rm -rf "$d" && mkdir -p "$d/bad" "$d/ide/.idea" && printf 'linter: jetbrains/qodana-jvm-community:latest\n' > "$d/bad/qodana.yaml" && cp qodana.yaml "$d/ide/qodana.yaml" && { scripts/qodana.sh "$d/bad" 2>"$d/bad.err"; test $? -eq 1; } && grep -q 'must pin linter' "$d/bad.err" && { scripts/qodana.sh "$d/ide" 2>"$d/ide.err"; test $? -ne 0; } && grep -q '.idea' "$d/ide.err" && rm -rf "$d"</automated>
    <automated>grep -q '^exit=255$' /Users/afanasyev/.claude/jobs/00d88943/tmp/qodana/task1.log && test "$(mise x -- jq '[.runs[].results[]] | length' /Users/afanasyev/.claude/jobs/00d88943/tmp/qodana/results/qodana.sarif.json)" -eq 6 && test "$(mise x -- jq '[.runs[].results[] | select(.ruleId == "EditorConfigOptionRedundancy" or (.locations[0].physicalLocation.artifactLocation.uri | test("(^|/)build/")))] | length' /Users/afanasyev/.claude/jobs/00d88943/tmp/qodana/results/qodana.sarif.json)" -eq 0</automated>
    Gate baselines: ci-success needs line [HEAD=0 EXPECT>=1]; actions/cache/save pin [HEAD=0 EXPECT>=1].
  </verify>
  <done>
    - qodana.yaml and an executable scripts/qodana.sh exist.
    - ci.yml has the hardened `qodana` job, and `ci-success` needs it.
    - actionlint, zizmor (offline) and shellcheck pass.
    - The script rejects an unpinned linter (exit 1, fixed message) and a directory with `.idea/` without calling Docker.
    - The real run on the HEAD export exits 255 with exactly 6 SARIF results, the six Kotlin findings, with none from .editorconfig and none under build output.
  </done>
</task>

<task type="auto">
  <name>Task 2: Resolve the six Kotlin findings and prove the gate green on the real tree</name>
  <files>build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/PrTemplateCheck.kt, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ToolingTableCheck.kt, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheck.kt, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ConfigPolicyCheck.kt, build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ExecutableOnPathFunctionalTest.kt</files>
  <action>
Keep behaviour, report text, problem ordering and messages byte-for-byte unchanged. The governance TestKit tests assert them. All four main files are governed paths, so Claude Code asks before each edit.

1. PrTemplateCheck.kt, `check()`, in the non-matching branch of `problems`. Replace the `+` concatenation with `buildList { … }`. Map the missing headings into it with `expected.filterNot { it in actual }.mapTo(this) { "missing heading: ## $it" }`, then `add` the `expected:` line and the `actual:   ` line, keeping the same strings and the three spaces after `actual:`. The `if` branch stays `emptyList()`.

2. ToolingTableCheck.kt, `check()`. Build `problems` with `buildList { … }`: the three `mapTo(this)` calls in the same order, with the same sorting and the same messages.

3. ToolingTableCheck.kt, `inventoryRows`. Keep the chain and add `@Suppress("ConvertCallChainIntoSequence")` on the function, below its KDoc. Put the reason as a `//` comment on the first line of the expression body, before `lines`. For example: a Markdown table has a few dozen rows, so a lazy sequence adds overhead and no benefit. Follow the convention that every `@Suppress` carries its reason. Do not place the comment above the file's first code line, because Spotless treats that as the licence header.

4. AskRulesCheck.kt, `ruleProblems`. Keep `expected` and `counts` as they are. Return `buildList { … }.sorted()` with the three `mapTo(this)` calls, for non-string rules, missing and duplicated, using the same filters and messages. This drops the three intermediate lists.

5. ConfigPolicyCheck.kt, `compareMaps`. Introduce the subject: `when (key) { !in actual -> …; !in expected -> …; else -> … }` with the same three branches in the same order.

6. ExecutableOnPathFunctionalTest.kt. The only two callers pass `"node"`. Rename the private helper to `realNode()` with no parameter. Use the literal `node` where the parameter was used, in both the file name and the `error(...)` message, so the message reads exactly as before for node. Update its KDoc to name `node` and keep the rest of the wording. Update both call sites to `${realNode()}`.

7. Run `mise x -- ./gradlew spotlessApply --console=plain`, then the `<verify>` build. Commit (`refactor(build-logic): …`). Then run the real gate in the background again: `QODANA_WORK_DIR=/Users/afanasyev/.claude/jobs/00d88943/tmp/qodana scripts/qodana.sh > /Users/afanasyev/.claude/jobs/00d88943/tmp/qodana/task2.log 2>&1; echo "exit=$?" >> /Users/afanasyev/.claude/jobs/00d88943/tmp/qodana/task2.log`. Expect exit 0 and 0 SARIF results. If the rewrite triggers a new inspection, fix it the same way, or suppress it with a reason only when the fix reads worse. Commit and re-run until clean.
  </action>
  <verify>
    <automated>mise x -- ./gradlew build --console=plain && test "$(grep -c 'buildList' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/PrTemplateCheck.kt)" -ge 1 && test "$(grep -c 'buildList' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ToolingTableCheck.kt)" -ge 1 && test "$(grep -c 'buildList' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheck.kt)" -ge 1 && test "$(grep -c 'when (key)' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ConfigPolicyCheck.kt)" -ge 1 && test "$(grep -c '@Suppress("ConvertCallChainIntoSequence")' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ToolingTableCheck.kt)" -ge 1 && test "$(grep -c 'realNode()' build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ExecutableOnPathFunctionalTest.kt)" -ge 3</automated>
    <automated>grep -q '^exit=0$' /Users/afanasyev/.claude/jobs/00d88943/tmp/qodana/task2.log && test "$(mise x -- jq '[.runs[].results[]] | length' /Users/afanasyev/.claude/jobs/00d88943/tmp/qodana/results/qodana.sarif.json)" -eq 0</automated>
    Gate baselines: buildList per file [HEAD=0 EXPECT>=1]; `when (key)` [HEAD=0 EXPECT>=1]; ConvertCallChainIntoSequence suppression [HEAD=0 EXPECT>=1]; `realNode()` lines [HEAD=0 EXPECT>=3].
  </verify>
  <done>
    - `./gradlew build` passes, covering compile with allWarningsAsErrors, detekt, Spotless, Dokka and every TestKit test including the governance and ExecutableOnPath tests.
    - The real Qodana run on the committed HEAD exits 0 with 0 SARIF results, so the gate starts at zero.
  </done>
</task>

<task type="auto">
  <name>Task 3: Renovate coverage, the deps:docker label, the parity-rule docs and the planning records</name>
  <files>renovate.json5, .github/labels.yml, docs/tooling.md, .planning/PROJECT.md, .planning/ROADMAP.md</files>
  <action>
1. renovate.json5, which is governed. Escape backslashes as the existing entries do.
   - Append to `customManagers` a regex manager:
     - `description`: "Qodana linter image (tag and digest) in qodana.yaml";
     - `managerFilePatterns`: one root-only pattern for `qodana.yaml`, in the same slash-delimited style as the others;
     - `matchStrings`: one pattern `linter: (?<depName>[^:\s]+):(?<currentValue>[^@\s]+)@(?<currentDigest>sha256:[a-f0-9]{64})`;
     - `datasourceTemplate`: "docker".
   - Add under the "Ecosystem labels" comment, after the npm rule: `description` "Label container image updates", `matchDatasources: ["docker"]`, `addLabels: ["deps:docker"]`.
   - Add one more rule at the end of `packageRules`, after the JDK rule, preceded by a comment that the Qodana tag must match the IntelliJ IDEA release (docs/tooling.md):
     - `description`: "Qodana linter image: year.minor tags only, with the IntelliJ IDEA parity reminder";
     - `matchDepNames: ["jetbrains/qodana-jvm-community"]`;
     - `allowedVersions`: a regex for a four-digit year, a dot and a minor;
     - one `prBodyNotes` entry, saying: the Qodana tag must equal the IntelliJ IDEA year.minor used for development (docs/tooling.md). If this PR changes the year.minor, update IntelliJ IDEA to the same release, run `scripts/qodana.sh`, and fix or justify every new finding in this PR before merging. A digest-only update needs no IDE change.
   - Add no automerge. The three D-12 automerge rules stay exactly as they are, and the top-level seven-day cooldown keeps applying.

2. `.github/labels.yml`, which is governed. After `deps:npm`, add `name: "deps:docker"`, `color: "0e8a16"`, `description: "A container image update."`. Quote it like its neighbours. Phase 2 images reuse this label.

3. docs/tooling.md:
   - In the Tools table, after the Docker-compatible engine row, add two five-column rows:
     - `Qodana JVM Community`: purpose "Runs the IntelliJ IDEA inspections (profile `Default`) as the required `qodana` CI gate"; version source `qodana.yaml (linter)`; installation "Container image by tag and digest, run by `scripts/qodana.sh` on the Docker-compatible engine"; used in `local, CI`.
     - `IntelliJ IDEA`: purpose "IDE; its year.minor must equal the Qodana image tag"; version source `qodana.yaml (linter year.minor)`; installation "Host installation (host prerequisite)"; used in `local`.
   - Change the Docker engine row's purpose so it says it also runs the local Qodana run now, besides tests and the local stack from Phase 2.
   - Write no version numbers, because the page states that versions are never written there.
   - Add a `## Qodana` section between "Planned tools" and "Notes", in short paragraphs or bullets, covering:
     - the gate runs every IntelliJ IDEA inspection of profile `Default` over the whole tree, fails on any problem (`failThreshold: 0`, no baseline), and is required through `ci-success`;
     - the IDE parity rule: the image tag's year.minor must equal the IntelliJ IDEA release used for development. An older image lacks inspections the IDE has, and only profile `Default` matches the IDE's defaults. When Renovate proposes a new year.minor, update the IDE in the same step; a digest-only update needs nothing;
     - the two exclusions and why. Build output and generated Kotlin DSL accessors are excluded, and a new module adds its build directory. EditorConfigOptionRedundancy is excluded for `.editorconfig` because of INFRA-14;
     - the policy is to fix a finding. When the fix reads worse, add `@Suppress("<InspectionId>")` with a reason comment;
     - local run: `scripts/qodana.sh` analyses the committed HEAD with the same image and flags as CI, in `QODANA_WORK_DIR`. The first run takes about half an hour (multi-GB image, Gradle and JDK downloads), later runs about ten minutes. On colima the work dir must be under `$HOME`.
   - Run `spotlessApply` so Prettier realigns the table.

4. .planning/PROJECT.md. Append one row at the end of the Key Decisions table, after the test-class-suffix row, with three cells.
   - Decision: Qodana JVM Community is a required CI gate (`qodana` job aggregated by `ci-success`) for the inspections only IntelliJ IDEA reports. It uses profile `Default` and `failThreshold: 0` with no baseline. The image is pinned once as tag + digest in `qodana.yaml` (`linter:`), run by `scripts/qodana.sh` locally and in CI, and updated by Renovate (year.minor tags only, manual merge). Its year.minor must equal the IntelliJ IDEA release used for development. `EditorConfigOptionRedundancy` is excluded for `.editorconfig` (INFRA-14), and build output is excluded.
   - Rationale: The compiler, detekt and ktfmt cannot see IDE inspections. The local evaluation found 12 findings in our code (6 fixed, 1 suppressed with a reason, 6 deliberate EditorConfig repetitions) and 240 only in generated build output, so the gate starts at zero without a baseline. Profile `Default` on the matching image reproduced exactly the IDE view, while `qodana.recommended` hid IDE-default inspections and image 2026.2 lacked one. The pin lives in qodana.yaml, not the catalog, because Gradle does not consume it. The 2026.3 image was adopted three days after its push, an exception to the seven-day cooldown approved by the author because IDE parity wins.
   - Outcome: `✓ 2026-11-22 (author decision)`.
   - Keep the table's pipe style, and do not touch other rows.

5. .planning/ROADMAP.md. In the Phase 12 `**Research**` line, delete the last topic (the comma before it and the whole Qodana clause) so the line ends with "open vs commercial tracing binding (prefer the open `SpanFactory` path)." Change nothing else.

6. Run `mise x -- ./gradlew spotlessApply --console=plain` and the full `<verify>`, then commit (`docs(qodana): …` or `build(deps): …` as fits; one commit is fine).
  </action>
  <verify>
    <automated>mise x -- renovate-config-validator --strict renovate.json5 && log="$HOME/.cache/qodana-renovate-extract.log" && LOG_LEVEL=debug mise x -- renovate --platform=local --dry-run=extract > "$log" 2>&1 && grep -q '"depName": "jetbrains/qodana-jvm-community"' "$log" && grep -q '"currentDigest": "sha256:5420d961a4f3a850adc47fc6565916eb6fe558f3205f511ac7bc75b98812c37e"' "$log" && grep -q '"currentValue": "2026.3"' "$log" && rm -f "$log" && test "$(grep -c '"deps:docker"' .github/labels.yml)" -ge 1 && test "$(grep -c 'scripts/qodana.sh' docs/tooling.md)" -ge 1 && test "$(grep -c 'Qodana JVM Community' .planning/PROJECT.md)" -ge 1 && test "$(grep -c 'IDE-only inspections' .planning/ROADMAP.md)" -eq 0 && test "$(grep -c 'prefer the open `SpanFactory` path)\.$' .planning/ROADMAP.md)" -ge 1</automated>
    <automated>mise x -- reuse lint && mise x -- editorconfig-checker && mise x -- actionlint && mise x -- zizmor --offline --no-progress .github/workflows && mise x -- ./gradlew build --console=plain</automated>
    Gate baselines: deps:docker label [HEAD=0 EXPECT>=1]; scripts/qodana.sh in docs/tooling.md [HEAD=0 EXPECT>=1]; decision row in PROJECT.md [HEAD=0 EXPECT>=1]; removed ROADMAP clause [HEAD=1 EXPECT==0]; SpanFactory line ending [HEAD=0 EXPECT>=1].
  </verify>
  <done>
    - Renovate's strict validator passes.
    - A local extract shows `jetbrains/qodana-jvm-community` from qodana.yaml with currentValue `2026.3` and the `sha256:5420d961…` digest.
    - `deps:docker` exists in .github/labels.yml.
    - docs/tooling.md documents Qodana, IntelliJ IDEA, the parity rule and the local run.
    - PROJECT.md has the decision row.
    - The ROADMAP Phase 12 research line ends at the SpanFactory topic.
    - reuse lint, editorconfig-checker, actionlint, zizmor and `./gradlew build` pass on the final tree.
  </done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| Pull request content → CI runner | qodana.yaml, scripts/qodana.sh and the analysed sources come from an untrusted PR and run inside the `qodana` job |
| Docker Hub → runner | The Qodana image, about 4 GB, is pulled and executed with the checkout mounted |
| GitHub Actions cache → PR jobs | A cache saved by one run is restored by later runs on main and on PR branches |
| Qodana container → network | The IDE inside the container may contact JetBrains services |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-kqn-01 | Tampering | qodana.yaml `linter:` read by scripts/qodana.sh | medium | mitigate | The script accepts only `^jetbrains/qodana-jvm-community:[0-9]{4}\.[0-9]+@sha256:[0-9a-f]{64}$`. The digest pins the image content, and Renovate is the only regular writer of that line |
| T-kqn-02 | Tampering | Qodana cache in actions/cache | medium | mitigate | `actions/cache/restore` on every run, and `actions/cache/save` only when `github.ref == 'refs/heads/main'`, so a PR can never poison what main restores. This is the same rule as the Gradle cache |
| T-kqn-03 | Information disclosure | `qodana` job token and secrets | medium | mitigate | Job `permissions: contents: read`, `persist-credentials: false`, no secrets in the job, `docker run` with no `-e` variables and no QODANA_TOKEN, and no cloud upload |
| T-kqn-04 | Information disclosure | Usage statistics from the container | low | mitigate | `--property=idea.headless.enable.statistics=false` is set in scripts/qodana.sh for every run |
| T-kqn-05 | Tampering | Workflow-command injection through logged PR content | low | mitigate | The linter value goes from the PR file into the script, never into `${{ }}` in a run body. On a mismatch the script prints a fixed message and never echoes the value |
| T-kqn-06 | Elevation of privilege | Container writes into the workspace as root | low | mitigate | `--user "$(id -u):$(id -g)"`, so the container runs as the runner user and every file in the mounted dirs belongs to that user |
| T-kqn-07 | Repudiation | Gate weakened through qodana.yaml (exclusions, failThreshold) | low | accept | qodana.yaml is a quality-tool config like config/detekt/detekt.yml, outside the governance boundary by design. The job and its place in ci-success are in governed `.github/**`, and the maintainer reviews every PR |
| T-kqn-08 | Denial of service | Long cold runs or a hung analysis | low | mitigate | `timeout-minutes: 60` on the job, and the restored cache keeps warm runs at about 10 minutes |
| T-kqn-09 | Tampering | Freshly pushed 2026.3 image adopted under the seven-day cooldown | low | accept | Author-approved IDE-parity exception, recorded in PROJECT.md. The image is the official JetBrains repository pinned by digest, and every later update waits for the cooldown |
| T-kqn-SC | Tampering | npm/pip/cargo installs | low | accept | No package-manager installs in this plan. The only new third-party artefacts are the digest-pinned Qodana image and actions/cache pinned by commit SHA, which is past the cooldown and checked by zizmor's online audits in CI |
</threat_model>

<verification>
- Task 1's real run fails (exit 255) on exactly the six known Kotlin findings. Task 2's real run passes (exit 0, 0 SARIF results). Together they show that the gate catches findings and starts at zero.
- actionlint, `zizmor --offline`, shellcheck, `renovate-config-validator --strict`, the Renovate local extract, `reuse lint`, `editorconfig-checker` and `./gradlew build` pass on the final tree.
- `git status --short` is clean after the last commit, on branch `gsd/phase-01-qodana-gate`, with nothing pushed.
</verification>

<success_criteria>
- A PR with any Qodana problem fails the `qodana` job and therefore `ci-success`, the required check.
- The Qodana image is pinned once (qodana.yaml) and Renovate updates its tag and digest together, with the IntelliJ IDEA parity reminder.
- The tree has zero Qodana findings with profile `Default` on image 2026.3, which matches the author's IDE view.
- docs/tooling.md, PROJECT.md and ROADMAP.md reflect the adoption.
</success_criteria>

<output>
Create `.planning/quick/261122-kqn-adopt-qodana-as-a-required-ci-gate-for-i/261122-kqn-SUMMARY.md` when done. Include:
- run times and exit codes of both real runs;
- the size of the local Qodana cache (`du -sh` of the work dir's `cache/`) as an estimate of the CI cache;
- any additional finding met and how it was resolved;
- the note that the PR needs the `governance-approved` label.
</output>

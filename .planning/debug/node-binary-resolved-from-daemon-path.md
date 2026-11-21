---
status: diagnosed
trigger: "node-binary-resolved-from-daemon-path: A Node switch on PATH is seen by the build: verifyNodeVersion checks, and Prettier runs with, the node binary on the current build's PATH, so the Prettier cache is invalidated on a Node major change even with a warm Gradle daemon."
created: 2026-11-21T17:46:51Z
updated: 2026-11-21T18:55:00Z
---

## Current Focus

hypothesis: H1 - In a warm daemon, Gradle updates System.getenv() and the native environ per build, but the JDK resolves a bare command name (no '/') against `parentPathv`, a native copy of PATH cached once when java.lang.ProcessImpl is initialised (first child process launched in the daemon JVM). So `commandLine("node", ...)` finds node on that stale PATH, while `sh -c 'command -v tool'` (lookup done by the shell using the child env, which is the updated PATH) and Spotless's own PATH walk over System.getenv("PATH") see the client's PATH.
test: Isolated scratch Gradle project (/tmp/path-exp, own GRADLE_USER_HOME, own daemon) with two shim dirs binA/binB each holding `probe-tool` printing A or B. Task action prints System.getenv("PATH") head, runs bare `probe-tool`, bare `probe-tool` with explicit environment("PATH", System.getenv("PATH")), `sh -c probe-tool`, and the absolute path resolved manually from System.getenv("PATH"). Daemon 1: PATH_A then PATH_B. Daemon 2: PATH_A `help` only, then PATH_B probe (distinguishes daemon-start PATH vs first-process-launch PATH).
expecting: Daemon 1 second build: getenv=B, bare=A, bare+explicit env=A, sh=B, manual=B. Falsified if bare=B in the warm daemon.
result: H1 confirmed in the scratch experiment and in the real worktree; a second contributing cause (Spotless npm-first node selection) confirmed in the real worktree.
next_action: Return ROOT CAUSE FOUND to the caller (goal find_root_cause_only); no code change.
reasoning_checkpoint:
  hypothesis: "verifyNodeVersion checks a different node than the build's PATH names because the JDK resolves the bare name `node` against a PATH cached at daemon start; Prettier runs yet another node because Spotless picks node beside the first npm on the live PATH"
  confirming_evidence:
    - "Scratch daemon: getenv PATH head binB, bare exec -> A, bare exec with explicit environment PATH -> A, absolute path from getenv -> B (same pid)"
    - "Real worktree warm daemon pid 55618: shim node never called by verifyNodeVersion; with --no-daemon it is; with npm beside the shim, Prettier calls the shim (serve.js) while verifyNodeVersion does not, in the same build"
  falsification_test: "A warm-daemon build whose bare `node` exec resolves the newly prepended shim would refute (1); Prettier invoking the PATH-first node when no npm sits beside it would refute (2) - neither observed"
  fix_rationale: "Resolving the absolute path from System.getenv(\"PATH\") bypasses parentPathv; feeding the same path to Spotless nodeExecutable/npmExecutable removes the second, divergent resolution rule"
  blind_spots: "Not tested on Linux CI (same JDK code path, setup-node puts node and npm in one dir); Windows PATHEXT not considered (no Windows support); TestKit daemon behaviour inferred from the same Gradle mechanism, not run"
  candidate_causes:
    - "code: bare-name exec in VerifyNodeVersion (confirmed)"
    - "code/third-party config: Spotless npm-first node resolution with no nodeExecutable configured (confirmed)"
    - "environment: Gradle failing to update daemon env (eliminated - System.getenv and child env are updated)"
  and_gate: "yes - (1) breaks the guard in a warm daemon, (2) breaks 'Prettier runs the verified node' even without a daemon; the truth requires fixing both"
mode: find_root_cause_only
bug_class: Bohrbug (deterministic given daemon state: warm daemon + changed PATH always reproduces)

## Symptoms

expected: After a green `./gradlew spotlessCheck`, switching the Node version on PATH makes verifyNodeVersion see the new Node (and fail against .nvmrc, or record the new major so spotlessPrettier re-runs), and Spotless Prettier runs with that same node binary.
actual: With a warm Gradle daemon, a different `node` placed first on the build's PATH was NOT seen. `.nvmrc=22` plus a PATH shim `node` that answers `--version` with `v22.0.0` (and execs the real Node 24 otherwise) still failed with "Node.js 24 is active, but .nvmrc requires 22". The same run with `--no-daemon` passed verifyNodeVersion and re-ran spotlessPrettier. taplo and shfmt shims WERE picked up with a warm daemon (resolved via `providers.exec { commandLine("sh", "-c", "command -v <tool>") }`), while VerifyNodeVersion calls `execOperations.exec { commandLine("node", "--version") }` with a bare command name.
errors: "Node.js 24 is active, but .nvmrc requires 22. Run `mise install` locally, or use setup-node with `node-version-file: .nvmrc` in CI." (verifyNodeVersion, warm daemon)
reproduction: UAT test 2. Shim dir with a `node` script printing `v22.0.0` for `--version`, otherwise exec the real mise node; `.nvmrc`=22; `mise x -- sh -c 'PATH="<shim>:$PATH" exec ./gradlew spotlessCheck --console=plain'` with a warm daemon (fails), then with `--no-daemon` (passes, spotlessPrettier re-executes). Restore `.nvmrc` to 24.
started: Discovered during Phase 01 UAT on 2026-11-22.
author_direction: "Do not rely on exec's own PATH lookup; resolve the full path of the binary from System.getenv(\"PATH\") and exec that absolute path."

## Eliminated

- hypothesis: The warm daemon keeps its start-up environment, so System.getenv("PATH") and the child environment are stale (the JDK 9 era JDK-8193288 behaviour)
  evidence: Gradle 9.9.2 ApplyClientEnvironmentVariables + ReflectiveEnvironment update System.getenv() per build; the scratch probe printed getenv PATH head binB in the warm daemon and `sh -c probe-tool` (child env) ran B. Only the JDK's native bare-name lookup is stale.
  timestamp: 2026-11-21T18:20:00Z

## Evidence

- timestamp: 2026-11-21T17:46:51Z
  checked: Knowledge base (.planning/debug/knowledge-base.md) and MemPalace
  found: No knowledge base exists yet (first debug session in this repository); no prior match
  implication: Proceed with open investigation

- timestamp: 2026-11-21T17:55:00Z
  checked: build-logic/src/main (grep for exec/commandLine/ProcessBuilder/getenv)
  found: Exactly three process launches in main sources. (1) VerifyNodeVersion.kt:58-59 `execOperations.exec { commandLine("node", "--version") }` (bare name, task action). (2) courses.root.gradle.kts:59-67 `pathOf(tool)` = `providers.exec { commandLine("sh", "-c", "command -v $tool") }` (configuration time; bare `sh`, but the lookup of the tool happens inside the shell). (3) VerifyFormatterBinary.kt:65-66 `commandLine(path, "--version")` with the absolute path from pathOf. No System.getenv / providers.environmentVariable anywhere in build-logic main.
  implication: VerifyNodeVersion is the only bare-name exec of a project tool; `sh` in pathOf is also a bare name but resolves to /bin/sh on every PATH.

- timestamp: 2026-11-21T18:00:00Z
  checked: Spotless versions (gradle/libs.versions.toml spotless=8.11.0 -> spotless-lib 4.11.0) and sources NpmPathResolver, NpmExecutableResolver, FileFinder, NodeExecutableResolver, StandardNpmProcessFactory, FormatExtension.NpmStepConfig
  found: With no explicit executables, Spotless resolves NPM first, not node: system property `npm.exec`, then env NVM_BIN (buggy: uses the variable NAME as a path), NVM_SYMLINK, NODE_PATH (parent of node_modules), then each entry of `System.getenv("PATH")` - first dir containing an executable `npm`. Node is then `<dir of that npm>/node` (NodeExecutableResolver.tryFindNextTo), never `node` on PATH. The npm process is launched with the ABSOLUTE npm path and child env PATH = `<node dir>:` + System.getenv("PATH"), and package.json `start` = `node serve.js`, so Prettier's server runs the node next to the first npm. Resolution is lazy (NpmFormatterStepLocations.npmExecutable() called at npm install/serve time, i.e. task execution), nothing about node is in the step state/fingerprint. NpmStepConfig exposes `npmExecutable(Object)` and `nodeExecutable(Object)` (and npmrc); with only nodeExecutable set, npm is taken from the same dir if executable, else falls back to the PATH search.
  implication: Prettier's node is chosen by a different rule (npm-first, from the live System.getenv("PATH")) than verifyNodeVersion's node (bare `node` resolved by the JDK). They can diverge even without a daemon (a PATH dir with node but no npm, NODE_PATH/NVM_SYMLINK set, npm.exec set).

- timestamp: 2026-11-21T18:05:00Z
  checked: Gradle 9.9.2 daemon classes (javap): ApplyClientEnvironmentVariables, AbstractProcessEnvironment, ReflectiveEnvironment
  found: Per build, ApplyClientEnvironmentVariables.doBuild calls processEnvironment.maybeSetEnvironment(build.getParameters().getEnvVariables()) and restores the old env after the build. AbstractProcessEnvironment.setEnvironmentVariable does setNativeEnvironmentVariable (native setenv) AND ReflectiveEnvironment.setenv, which writes into the backing field `m` of the unmodifiable System.getenv() map.
  implication: In a warm daemon, System.getenv("PATH") and ProcessBuilder.environment() (cloned from the same JDK map) should carry the client's PATH. The stale lookup must come from somewhere else.

- timestamp: 2026-11-21T18:08:00Z
  checked: JDK 25 java.lang.ProcessImpl (src.zip) and OpenJDK native sources (childproc.h/childproc.c via web; JDK-6413313)
  found: ProcessImpl has `static { init(); }` -> native Java_java_lang_ProcessImpl_init sets `parentPathv = effectivePathv(env)`, the "cached and split version of the JDK's effective PATH" (childproc.h; native putenv("PATH=...") explicitly unsupported). JDK_execvpe searches parentPathv for a file name without '/', by design using the PARENT's PATH, not the child's (JDK-6413313).
  implication: Bare-name lookup uses the PATH captured when ProcessImpl was first initialised in the daemon JVM; Gradle's per-build setenv/reflection cannot refresh it. Needs experimental confirmation.

- timestamp: 2026-11-21T18:20:00Z
  checked: Scratch experiment /tmp/path-exp (Gradle 9.9.2 distribution, -g /tmp/path-exp/gh, JDK 25, --configuration-cache). Shims binA/probe-tool -> "A", binB/probe-tool -> "B". PATH_A = binA:binB:/usr/bin:/bin:..., PATH_B = binB:binA:...
  found: |
    Daemon 1 (pid 95338): build 1 PATH_A -> all probes A. Build 2 PATH_B, SAME daemon pid:
      getenv.PATH.head=[/tmp/path-exp/binB]
      bare commandLine(probe-tool) -> A
      bare + environment("PATH", System.getenv("PATH")) -> A
      sh -c probe-tool -> B
      manual resolve from System.getenv("PATH") = /tmp/path-exp/binB/probe-tool; absolute path -> B
    Daemon 2 (pid 95485): build 1 PATH_A `help` only (no task process), build 2 PATH_B probe -> bare still A, getenv B, sh B, absolute B.
  implication: H1 CONFIRMED. System.getenv("PATH") in a task action of a warm daemon DOES reflect the client's PATH; the ExecSpec child environment carries it too; but the JDK resolves a bare executable name against the PATH cached at daemon start (parentPathv), even when the child's PATH is set explicitly. A shell started by bare `sh` looks the tool up with the child env PATH, hence the correct result. Resolving the absolute path from System.getenv("PATH") and exec-ing it gives the client's binary.

- timestamp: 2026-11-21T18:25:00Z
  checked: Configuration-cache behaviour in the same scratch daemon
  found: |
    Lazy providers.exec(sh -c command -v) wired into a task Property without .get(): evaluated at execution time on every build, CC entry reused across PATH_A/PATH_B, value follows the client PATH.
    Eager providers.exec(...).get() at configuration time (the pathOf pattern, which the root plugin realises with taploPath.get()/shfmtPath.get()): CC is re-checked by re-running the process; PATH_A->PATH_B: "configuration cache cannot be reused because output of the external process 'sh' has changed"; an unrelated PATH change that does not change the output (PATH_A + /tmp/path-exp/binC, after PATH_A) also only misses when the output differs, and an identical output reuses the entry.
    System.getenv("PATH") at configuration time: "configuration cache cannot be reused because environment variable 'PATH' has changed" for PATH_A -> PATH_A+binC, although the resolved binary is identical (over-invalidation on any PATH edit).
    System.getenv("PATH") inside the task action: not a CC input; CC entry reused while the action sees each build's PATH.
  implication: Q3/Q4 answered. pathOf follows the build's PATH and invalidates CC only when the resolved path changes. Reading PATH at configuration time would invalidate CC on every PATH difference (mise activation order, IDE vs terminal), so for verifyNodeVersion (outputs.upToDateWhen { false }) the lookup belongs in the task action.

- timestamp: 2026-11-21T18:35:00Z
  checked: ValueSource<String, None> whose obtain() walks System.getenv("PATH") for the tool, realised with .get() at configuration time (scratch project, same daemon)
  found: PATH_A -> entry stored; PATH_A+binC (same resolved file) -> "Configuration cache entry reused"; PATH_B -> "configuration cache cannot be reused because a build logic input of type 'ToolOnPath' has changed", value /tmp/path-exp/binB/probe-tool (while bare exec in the same build still ran A).
  implication: A ValueSource is the CC-precise way to resolve a binary from System.getenv("PATH") at configuration time (needed for Spotless nodeExecutable/npmExecutable, which project.file() resolves eagerly in createStep): it invalidates only when the resolved path changes, unlike a direct System.getenv("PATH") read.

- timestamp: 2026-11-21T18:45:00Z
  checked: Real worktree, logging shim /tmp/node-shim/node (appends args to /tmp/node-shim-log/calls.log, then execs the real mise node 24.22.3, so .nvmrc stays 24 and is never edited). Daemon warmed with `mise x -- ./gradlew help`, then `mise x -- sh -c 'PATH="/tmp/node-shim:$PATH" exec ./gradlew verifyNodeVersion spotlessPrettier --rerun --console=plain --info'`
  found: |
    Run 1, warm daemon pid 55618 (299th build, uptime 12 h), shim dir holds only `node`: verifyNodeVersion and spotlessPrettier both executed (server started), calls.log EMPTY -> verifyNodeVersion ran the daemon-start node, Prettier ran the node next to the mise npm.
    Run 2, same command with --no-daemon (single-use daemon pid 97308 forked with the client env): calls.log = "args=--version" only -> verifyNodeVersion used the shim, Prettier's server did NOT (it ran /Users/afanasyev/.local/share/mise/installs/node/24.22.3/bin/node, next to npm).
    Run 3, warm daemon pid 55618 (300th build), shim dir additionally holds `npm` -> symlink to the mise npm: calls.log = "args=serve.js --node-server-instance-id=..." only -> in ONE build, Prettier ran the shim node from the client PATH while verifyNodeVersion ran the stale node.
  implication: Confirms both defects in the project itself: (a) verifyNodeVersion's bare `node` follows the daemon's start PATH; (b) Prettier follows a different rule (first `npm` on the live System.getenv("PATH"), node beside it), so the guard and the formatter can check/run different binaries in either direction, with or without a daemon.

- timestamp: 2026-11-21T18:50:00Z
  checked: build-logic/src/functionalTest FormattingFunctionalTest + GradleFixture
  found: The Node tests only vary `.nvmrc` ("1"), never the node on PATH. PATH manipulation (GradleRunner.withEnvironment via runWithout/runAndFailWithout) exists only for taplo/shfmt, whose lookup runs inside `sh -c command -v` and therefore works in a reused TestKit daemon.
  implication: Why not caught: no test switched node on PATH between two builds of one daemon, and nothing asserted that Prettier runs the verified binary.

## Resolution

root_cause: |
  Two contributing causes (AND-gate: either alone breaks the "same node, from the build's PATH" truth).
  (1) VerifyNodeVersion.kt:58-59 launches `commandLine("node", "--version")` by bare name. The JDK (ProcessImpl/childproc JDK_execvpe) resolves a name without '/' against `parentPathv`, a native copy of PATH cached once when java.lang.ProcessImpl is initialised in the daemon JVM, by design the parent's PATH and not the child's (JDK-6413313). Gradle 9.9.2's ApplyClientEnvironmentVariables updates the native environ and the System.getenv() map per build, but cannot refresh parentPathv, so in a warm daemon `node` is looked up on the PATH of the client that started the daemon, even though System.getenv("PATH") inside the task action and the child's own PATH carry the current client's PATH. --no-daemon forks a fresh single-use daemon, which is why it "works".
  (2) Spotless 8.11.0 (spotless-lib 4.11.0) Prettier with no explicit executables does not use `node` on PATH at all: NpmPathResolver finds the first executable `npm` (npm.exec system property, NVM_BIN [broken: uses the variable name], NVM_SYMLINK, NODE_PATH, then each System.getenv("PATH") entry) and runs `<that dir>/node` (the npm process gets PATH=<node dir>:$PATH and starts `node serve.js`). It reads the live, per-build PATH, so it follows the client's PATH in a warm daemon, but by an npm-first rule that differs from verifyNodeVersion's node-first rule. The guard and the formatter can therefore check and run different binaries, with or without a daemon.
fix: (diagnose-only; not applied)
verification: (diagnose-only)
files_changed: []

---
phase: 02-runtime-infrastructure-dcb-spikes
reviewed: 2026-11-24T00:00:00Z
depth: standard
files_reviewed: 23
files_reviewed_list:
  - .claude/CLAUDE.md
  - .claude/settings.json
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersionsFunctionalTest.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt
  - docs/adr/0003-governance-gate-trust-boundary.md
  - docs/adr/0004-gsd-configuration.md
  - docs/adr/0015-governance-hooks-for-worktrees-and-merges.md
  - docs/tooling.md
  - docs/upstream/0001-decision-capture-extension-point.md
  - docs/upstream/0002-conflict-diagnosis-completion-exception.md
  - docs/upstream/0003-remote-conflict-classification.md
  - docs/upstream/0004-tag-resolver-value-classes.md
  - docs/upstream/0007-shared-testcontainer-latest-image.md
  - governance/governed-paths.txt
  - renovate.json5
  - scripts/doctor.sh
  - scripts/governance/ask-governed-edit.sh
  - scripts/governance/ask-governed-merge.sh
  - scripts/governance/check-pr.sh
  - scripts/governance/test-ask-governed-edit.sh
  - scripts/governance/test-ask-governed-merge.sh
  - scripts/governance/test-check-pr.sh
  - scripts/test-doctor.sh
findings:
  critical: 1
  warning: 6
  info: 0
  total: 7
  necessity: 0
status: issues_found
---

# Phase 2: Code Review Report (incremental re-review, second pass)

**Reviewed:** 2026-11-24
**Depth:** standard
**Files Reviewed:** 23
**Status:** issues_found

## Summary

This pass covers the changes since 03358fd: the fixes for WR-07..WR-10 (quick task 261124-c1g), the agent-worktree
exemption of the edit hook (261124-dvp), the Renovate manager and never-exempt rule for the GSD pin (261124-eum), and the
widened governed list (261124-gju, 261124-hbh). No structural findings block was provided, and the necessity reference was
not applied (no finding qualified).

The WR-07..WR-10 fixes hold for the exact forms they name: I re-ran `git mer''ge`, `git m\erge`, a heredoc whose delimiter
never comes, `x=$((1<<2))` and `echo ... | bash` against the real hook, and each asks. The `check-pr.sh` never-exempt
logic, the Renovate cooldown and GSD rules, and the rewritten `ComposeVersionsFunctionalTest` (it now owns its image set,
so a real catalog entry cannot break it) have no defect that I could demonstrate. IN-06, IN-07 and IN-08 of the previous
review stay open per `02-REVIEW-DISPOSITION.md` and are not repeated.

The weaknesses are in the merge hook, and they matter more than before. Since 261124-dvp the edit hook is silent inside
an agent worktree, and ADR-0015 calls the merge hook "the single confirmation point" for that work. I ran the real
`ask-governed-merge.sh` against a throwaway repository in which branch `feat` changes `.github/workflows/ci.yml`. Several
merge commands that fall inside the forms the header says it analyses print nothing, one of them a very common idiom.

## Critical Issues

### CR-01: `bash -euo pipefail -c '...'` hides the script from the merge hook

**File:** `scripts/governance/ask-governed-merge.sh:811-849`
**Issue:** `analyze_shell` skips the value of an option only when the word is exactly `-o`, `+o`, `-O` or `+O`
(line 820). In a cluster such as `-eo`, `-euo` or `-xeuo`, `o` is the last flag and takes the next word (`pipefail`) as
its value, but the hook reads `pipefail` as the script operand, stops the option loop before it reaches `-c`, and never
analyses the script. Long options that take a value (`--rcfile <file>`, `--init-file <file>`) have the same defect,
because every `--*` word advances by one. I ran the hook with `branch feat` changing a governed file:
```
bash -euo pipefail -c 'git merge feat'      -> no output (bash runs the merge)
bash -eo pipefail -c 'git merge feat'       -> no output
bash --rcfile /dev/null -c 'git merge feat' -> no output
bash -o pipefail -c 'git merge feat'        -> asks (only the exact `-o` is understood)
```
`bash -euo pipefail -c` is the usual strict-mode spelling, so this is not an exotic form. The same function serves `sudo`,
`env -S`, `mise x --` and heredoc bodies, so every route to a shell inherits it. The hook is the only prompt for
agent-worktree work since the 2026-11-24 amendment, and its header lists `bash -c` among the forms it covers.
`test-ask-governed-merge.sh` has no case for a cluster ending in `o`.
**Fix:** Parse the shell options the way bash does. A short-option cluster that contains `o` or `O` takes the next word
as a value (and a cluster that contains `c` still marks a script); `--rcfile` and `--init-file` take a value in both
spellings:
```bash
    --rcfile | --init-file) index=$((index + 2)) ;;
    --*) index=$((index + 1)) ;;
    -* | +*)
      [[ $word == -*c* ]] && has_script=1
      [[ $word == -*s* ]] && has_stdin_option=1
      if [[ $word == -*[oO]* || $word == +*[oO]* ]]; then
        index=$((index + 2))
      else
        index=$((index + 1))
      fi
      ;;
```
Handle `-c` before the `o` test, because the loop must still find `-c` after the value. Add `bash -euo pipefail -c`,
`bash -eo pipefail -c`, `sh -eu -c`, `bash --rcfile x -c` and `bash --init-file x -c` to the test file. When a word cannot
be classified, ask when the command mentions a merge instead of dropping the shell.

## Warnings

### WR-01: Option forms of `git`, `sudo`, `env` and a `GIT_DIR` prefix make the hook read the wrong repository or none

**File:** `scripts/governance/ask-governed-merge.sh:668-694` (git options), `948-1000` (`env` and `sudo`)
**Issue:** The merge is checked in `cwd` plus the directory changes the hook understands. Several spellings that move
the repository, or hide the subcommand, are not understood, and none of them asks. With the session in a repository where
the merge is clean (`repo2`), and the governed change in `repo`:
```
git --attr-source HEAD merge feat                   -> no output  (real git accepts the separate-word form)
sudo --chdir=/path/to/repo git merge feat           -> no output  (only the exact `--chdir` clears cwd)
sudo -D/path/to/repo git merge feat                 -> no output
env --chdir=/path/to/repo git merge feat            -> no output
env -C/path/to/repo git merge feat                  -> no output
GIT_DIR=/path/to/repo/.git GIT_WORK_TREE=/path/to/repo git merge feat -> no output
```
For `--attr-source` the hook takes `HEAD` as the git subcommand and returns. For the others it analyses the merge in the
session's repository and finds nothing, although the command merges in another one. The spelled-out variants
`sudo -D <dir>` and `env -C <dir>` do ask ("cannot tell which repository"), so the forms that were handled prove the
intent. ADR-0015 lists none of these as residual risks.
**Fix:** In `analyze_git` add `--attr-source` to the options that take a value (`-c | --namespace | --config-env |
--super-prefix | --attr-source`). In the `env` and `sudo` loops clear `cwd` for `-C*`, `--chdir=*`, `-D*` and
`--chdir=*` as well, with the glob patterns rather than the exact words. In `analyze_words`, treat an assignment to
`GIT_DIR`, `GIT_WORK_TREE`, `GIT_COMMON_DIR` or `GIT_CEILING_DIRECTORIES` in front of git as an unknown repository
(`repository_known=0` or `cwd=''`), so a command that mentions a merge asks. Add each form to
`test-ask-governed-merge.sh`.

### WR-02: Brace expansion assembles a merge command without a trace

**File:** `scripts/governance/ask-governed-merge.sh:55-69` (early probe), `1034-1041` (command dispatch)
**Issue:** `git {merge,feat}` and `{git,merge,feat}` expand to `git merge feat` in bash and zsh. The hook reads the first
as a program `git` with the subcommand word `{merge,feat}`, which is neither `merge` nor dynamic, and the second as an
unknown program; neither asks. I ran both and the hook printed nothing, while `bash -c 'echo {git,merge,feat}'` prints
`git merge feat`. The words appear literally in the command text, so the early probe does not hide them; the lexer simply
does not model brace expansion. ADR-0015 residual risks name parameter expansion and ANSI-C escapes as assembling forms
and do not name this one.
**Fix:** Mark a word that holds an unquoted `{...,...}` (a brace and a comma between them) as dynamic in `lex_command`,
so that `analyze_git` ("git runs a subcommand given by a shell expansion") and the program check ask when the command
mentions a merge:
```bash
'{') [[ $text == *'{'*','*'}'* ]] && dynamic=1; word+=$c; in_word=1; i=$((i + 1)) ;;
```
(or test `$word =~ \{[^}]*,[^}]*\}` in `lex_end_word`). Add `git {merge,feat}` and `{git,merge,feat}` to the tests, or
list brace expansion in ADR-0015's residual risks.

### WR-03: Refs that an earlier part of the same command moves are read before the command runs

**File:** `scripts/governance/ask-governed-merge.sh:567-598` (`check_merge`), `1130-1133`
**Issue:** `check_merge` resolves the ref and `HEAD` in the repository as it is when the hook runs. A command that moves a
ref or `HEAD` first and then merges is judged against the old state. I ran, with a branch `tmp` that pointed at `main`:
```
git branch -f tmp feat && git merge tmp                       -> no output
git checkout -B tmp feat && git checkout main && git merge tmp -> no output
```
Both merge a governed change. The same holds for `git fetch origin && git merge origin/main` once `origin/main` exists
(it names the pre-fetch commit), and for `reset`, `update-ref` and `switch -C` before a merge. The executor flow that
the ADR expects ("an executor that merges the base branch into its own worktree") is the `fetch` case. Pre-execution
analysis cannot know the new state, and ADR-0015 does not say so.
**Fix:** When one command holds a merge and also a git subcommand that moves refs or `HEAD` (`fetch`, `pull`, `branch`
with `-f`/`-M`/`-C`, `update-ref`, `checkout`/`switch` with `-b`/`-B`/`-c`/`-C`, `reset`, `symbolic-ref`, `tag -f`), add a
problem ("a ref the merge uses may change before it runs") so that the hook asks, or record the limit in ADR-0015's
residual risks next to `pull` and `rebase`.

### WR-04: The agent worktree exemption does not hold for the `**/` governed entries

**File:** `scripts/governance/ask-governed-edit.sh:228-238`, with `governance/governed-paths.txt` (new entries
`**/.editorconfig`, `**/.gitattributes`, `**/REUSE.toml`, `**/.shellcheckrc`, `**/shellcheckrc`) and
`.claude/settings.json:58-71`
**Issue:** `check_candidate` matches the path against every root in `ROOTS`, including the main checkout, and skips a root
only when that root is itself the agent root. A file inside an agent worktree is also below the main root, and relative
to it the path is `.claude/worktrees/agent-x/<file>`, which a `**/<name>` entry matches. I created an agent worktree and
ran the hook:
```
<main>/.claude/worktrees/agent-abc/.github/workflows/ci.yml  -> silent
<main>/.claude/worktrees/agent-abc/.editorconfig             -> ask (entry `**/.editorconfig`)
<main>/.claude/worktrees/agent-abc/sub/.gitattributes        -> ask (entry `**/.gitattributes`)
<main>/.claude/worktrees/agent-abc/REUSE.toml                -> ask (entry `**/REUSE.toml`)
```
The `Edit(/**/.editorconfig)` style ask rules anchor at the session root and reach the worktree the same way, so the
rules prompt even when the hook is silent. The entries date from the same day as the exemption (dead089 and 2a4927f against
7b332d0), and `test-ask-governed-edit.sh` has a `**/.editorconfig` entry but no case that places it in an agent worktree.
This contradicts ADR-0015 ("no per-edit ask inside an agent worktree"), the ADR's live check, ADR-0004's statement that
"the edit rule does not match" there, and `.claude/CLAUDE.md`. The prompts are harmless for safety, but they are the
per-edit prompts the author asked to remove.
**Fix:** In the hook, skip a root in the first loop when the candidate lies below an agent root (reuse `is_agent_root`
over `AGENT_ROOTS` for the candidate, not for the root). The ask rules cannot be narrowed without breaking the
one-rule-per-entry check, so state that residual in ADR-0015 and ADR-0004 (the four `**/` names still prompt in an agent
worktree), or move those names to root and explicit-directory entries. Add the missing hook case.

### WR-05: `doctor` builds a copy-paste install command from the unvalidated pin

**File:** `scripts/doctor.sh:358-361, 379-381, 429`
**Issue:** `pinned` is the first line of `governance/gsd-version` with whitespace removed, and `install_fix` embeds it as
`npx -y @opengsd/gsd-core@$pinned --claude --local`, which doctor prints as the fix for four failures. The value is never
checked against a version pattern (the Renovate manager only matches `\d+.\d+.\d+`, but the file can hold anything). An
empty or blank pin yields `npx -y @opengsd/gsd-core@ --claude --local`, an install with no version at all, which is the
opposite of what the pin is for. Any other text is pasted into a shell command the maintainer is told to run. The file is
governed, and the output is only a suggestion, so the damage needs a reviewer to miss the file; the unpinned command is
the more likely failure.
**Fix:** Validate right after reading the file and fail with its own message when the pin is not a bare version:
```bash
if [[ ! $pinned =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  report fail gsd "governance/gsd-version must hold one version such as 1.2.3, not '$pinned'" \
    "write the version of the installed GSD into governance/gsd-version"
  return
fi
```
Add a case with an empty file and one with `1.0.0; echo x` to `test-doctor.sh`.

### WR-06: UP-0001 and UP-0004 still say the contribution is undecided

**File:** `docs/upstream/0001-decision-capture-extension-point.md:114`, `docs/upstream/0004-tag-resolver-value-classes.md:99`
**Issue:** The records are the repository's public source of truth. Both end with "Open decision for the author ... This repository has not decided whether to
contribute". Commit 6169e7d (after the records were edited in c0db516) records in `STATE.md` that the author decided on
2026-11-24 to contribute both pull requests, #5326 first as an opt-in decorator definition, then #5328. The records now
state the opposite of the project's decision, and UP-0001 adds that "the 5.5.0 milestone may move" until the repository
decides.
**Fix:** Replace both paragraphs with the decision: the author will contribute after the Phase 2 pull request, in the
order STATE.md gives, and the record is updated when a pull request merges. Keep the `updated:` date current.

---

_Reviewed: 2026-11-24_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_

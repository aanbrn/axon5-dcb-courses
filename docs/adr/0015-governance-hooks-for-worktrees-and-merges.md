---
status: "accepted"
date: "2026-11-23"
decision-makers: ["repository author (debug session ask-rules-skip-worktree-agents)"]
---

# ADR-0015: Governance hooks guard governed paths in every worktree and at merge time

## Context and Problem Statement

AIDEV-08 requires the author to confirm every agent edit of a governed path. Phase 1 implemented it as one
`Edit(/<entry>)` ask rule per entry of `governance/governed-paths.txt` in `.claude/settings.json`, kept in step by
`checkClaudeAskRules`.

GSD dispatches executors as subagents with `isolation: worktree`, which work under `.claude/worktrees/<name>/`. In the
quick tasks 261123-mub and 261123-rzt such executors edited governed files (the governance package,
`courses.root.gradle.kts`, `governance/governed-paths.txt`, `.claude/settings.json`) and nobody was asked. A debug
session found two independent gaps:

- **Anchoring.** Claude Code resolves a `/`-anchored rule from the project settings against the session's primary
  working directory, the directory the session started in. A subagent inherits the parent's rules with that anchor, so
  `Edit(/mise/**)` means `<main checkout>/mise/**` and never matches `<main checkout>/.claude/worktrees/agent-x/mise/**`.
  Live probes on Claude Code 2.1.330 confirmed it: auto mode accepted a worktree subagent's Write to its own
  `mise/ask-rule-probe.txt`, the same rule asked for the file in the main checkout, and the path-independent
  `Bash(*config-set*)` rule asked inside the worktree subagent. Asks raised in background subagents reach the author.
- **Coverage.** Edit rules see neither shell writes (a redirect into a governed path in the main checkout was accepted
  without an ask) nor the `git merge` with which `worktree.cleanup-wave` brings executor branches into the main checkout.
  The transcripts show executors writing governed files with `sed -i`, `perl -pi`, heredocs and Python, which auto mode's
  preference for shell edits encourages.

Both gaps must close for AIDEV-08 to hold for delegated work.

## Decision Drivers

- The author is asked before a governed path changes, in whichever worktree the agent works.
- Governed content that reaches a worktree through any write channel is caught before it enters the main checkout.
- One source of truth: the hooks read `governance/governed-paths.txt` with the grammar the pull request gate and
  `checkClaudeAskRules` use.
- Fail closed: a hook that cannot decide asks, and a hook that cannot start blocks.
- `check` verifies the setup, as it verifies the ask rules.
- Ordinary work stays fast and free of prompts.

## Considered Options

- Worktree twins of every ask rule, `Edit(/.claude/worktrees/*/<entry>)`.
- A PreToolUse hook for the file tools that matches the edited path against every worktree root (chosen).
- A merge gate: a PreToolUse hook for Bash that asks before a merge that brings changes to governed paths (chosen, in the
  strict form), or a lighter rule in `.claude/CLAUDE.md` that the orchestrator checks the branch and asks before it
  merges.
- Coverage of shell writes at write time: heuristics over shell commands, or a sandbox `denyWrite` on governed paths.
- Process only: plans that touch governed paths run inline in the main session.

## Decision Outcome

Chosen options: the edit hook and the strict merge gate, because together they close both gaps. The edit hook restores
the edit-level prompt of AIDEV-08 in every worktree, and the merge hook catches every other write channel at the point
where worktree work enters the main checkout.

### The hooks

- **`scripts/governance/ask-governed-edit.sh`** (matcher `Edit|Write|MultiEdit|NotebookEdit`). It takes the absolute
  path of the call, normalised twice (with `.` and `..` applied, and with the symbolic links of its longest existing
  prefix and of its final component resolved), relative to the session root, the main worktree root, every worktree that
  `git worktree list` reports and any `.claude/worktrees/<name>/` below the session or main root. When a relative path
  matches a governed entry, ignoring ASCII case as the default macOS file system does, it asks and names the entry and
  the worktree.
- **`scripts/governance/ask-governed-merge.sh`** (matcher `Bash`). A command that mentions neither `merge` nor
  `cleanup-wave` leaves at once, once quotes, backslashes, `$` and line continuations are removed from the text (so
  `git mer''ge` and `git mer\<newline>ge` count as mentions); the test is linear, because bash pattern substitution on a
  long command is quadratic in bash 3.2 and would run into the hook's timeout. Otherwise a small lexer finds
  `git ... merge <ref>...`, also with `-C`, after `cd`, in lists, pipelines, subshells, command substitutions, `bash -c`,
  `eval`, heredocs and here-strings fed to a shell, behind `env` (also `env -S`), `sudo` and `doas`, behind programs that
  take git or `bash -c` among their arguments (`mise x -- git merge`, `stdbuf`, `caffeinate`, `script`), and GSD's
  `worktree.cleanup-wave --manifest <file>`, which merges every branch of the manifest. `<<` inside `$((...))` or
  `((...))` is arithmetic, not a heredoc. For each merged ref the hook lists the files changed since the merge base with
  the target's `HEAD` (`git diff --no-renames --name-only HEAD...<ref>`, so a file moved out of a governed path counts)
  and asks when one is governed, listing them. `git merge --abort` and `--quit` pass. It also asks when a shell reads its
  script from standard input (a pipe, `/dev/stdin`, a process substitution, a heredoc fed to another command), when a
  script, eval text or program comes from a shell expansion, or when the lexing is left incomplete (an unterminated
  quote, substitution or heredoc), if the command mentions a merge anywhere.
- **Shared code.** `governed-paths.sh` holds the grammar and the glob matcher and is now also sourced by `check-pr.sh`;
  `hook-common.sh` holds the hook protocol. [ADR-0003](0003-governance-gate-trust-boundary.md) does not require the gate to
  be self-contained: the library lies under the same governed `scripts/governance/**` and comes from the same tree.
  `GovernedPaths` in build-logic mirrors both the grammar and the matcher.
- **Protocol.** A hook prints one JSON object with `permissionDecision: "ask"`, or nothing, and exits 0. It never returns
  `allow`, so it only adds prompts; when hooks disagree, Claude Code lets `ask` win over `allow`. An EXIT trap turns any
  unexpected failure into an ask, a missing `jq` gets a fixed JSON answer, and an input, list, ref, manifest or directory
  that cannot be resolved is an ask with the reason. The scripts run under bash 3.2, the first bash on the macOS `PATH`.
- **Registration.** `.claude/settings.json` registers each hook in exec form, `"command": "bash"` with
  `"args": ["${CLAUDE_PROJECT_DIR}/scripts/governance/<hook>.sh"]`, and with `"onFailure": "block"`, so a hook that cannot
  start or times out blocks the call instead of letting it through. `${CLAUDE_PROJECT_DIR}` is the directory the session
  started in and does not follow a worktree, so the scripts and the governed list always come from the session's own
  tree, never from a worktree an agent edits. The project ask rules stay: they guard the session's own tree on their own.

### Consequences

- Good, because a worktree agent's edit of a governed path prompts the author again, in auto mode and in background
  subagents: a hook's ask goes to the permission prompt, not to the auto mode classifier.
- Good, because governed content that shell commands, scripts or git wrote in a worktree is caught when it is merged
  through `git merge` or `worktree.cleanup-wave`, with the governed files listed in the prompt. That holds for the forms
  the hook analyses or, failing that, asks about; the forms under Residual risks stay outside.
- Good, because the hooks fail closed, and `checkClaudeAskRules` fails the build when a registration, a script or its
  governance is missing.
- Bad, because every Edit and Write pays for a hook run, about 80 ms, and every Bash command for a short one; a command
  that mentions `merge` pays for the full check.
- Bad, because a merge the hook cannot resolve, such as `git merge "$BRANCH"`, asks even when it brings nothing
  governed, and an executor that merges the base branch into its own worktree is asked when the base changed governed
  paths.
- Bad, because failing closed also asks for some commands that only mention a merge next to a script the hook cannot
  read (a pipe into a shell, `eval "$x"`), and an argument list that names git and merge, such as `echo git merge x`, is
  checked as a merge.
- Bad, because shell writes in a worktree are still not prompted when they happen, only when they are merged.

### Residual risks

- The merge hook does not see other integration channels: `git pull`, `cherry-pick`, `rebase`, `reset`,
  `checkout <ref> -- <path>`, `apply` and `am`, git aliases, and programs that run git themselves, except the wrappers
  and the argument lists it analyses (see The hooks). The pull request gate
  (`governance-success`) and the maintainer's review stay the control there, as ADR-0003 records.
- The hook never sees a merge word or git subcommand that the command text does not mention: one assembled by a
  parameter expansion or a command substitution (`git mer${E}ge`) or by ANSI-C escapes (`$'...'` with a hex or octal
  escape) leaves at the early probe.
- A script file that a shell or `source` runs counts with the programs that run git themselves: the hook does not read
  the file.
- A shell that reads standard input behind a program the hook does not know as a wrapper (for example
  `echo ... | stdbuf bash`) is not asked about.
- A shell write by the main session into a governed path of its own tree is not prompted. That is unchanged since
  Phase 1, and the same review covers it.
- `disableAllHooks` in the untracked `.claude/settings.local.json` or in the user settings switches the hooks off;
  `checkClaudeAskRules` checks only the tracked project settings.
- The hooks are a guard rail, not a security boundary. A confirmed edit of the hook scripts, or a session started
  outside the repository root, where the project settings do not load, is beyond them.

### Confirmation

- `checkClaudeAskRules` (`AskRulesCheck` with `GovernanceHooks`) checks, besides the ask rules, that
  `.claude/settings.json` registers each hook exactly once with its matcher, command, args and `onFailure: "block"`,
  without `if` or `async`, that `disableAllHooks` is not true, and that both scripts exist and are governed paths. Its
  TestKit tests are in `AskRulesCheckHooksFunctionalTest`.
- `scripts/governance/test-ask-governed-edit.sh` and `test-ask-governed-merge.sh` run the real hooks against throwaway
  repositories with linked worktrees and branches; CI runs every `scripts/*/test-*.sh`.
- Live check: with the hooks registered, a worktree subagent's Write to a governed path of its worktree must ask, which
  repeats the debug session's probe that showed the gap.

## More Information

- The debug session `ask-rules-skip-worktree-agents` under `.planning/debug/` holds the evidence, the probes and the
  eliminated hypotheses.
- Claude Code documentation: hooks reference (Hook locations, Matcher patterns, Exit code output, Block the action when a
  hook fails, Reference scripts by path, PreToolUse decision control, PreToolUse `ask` decisions in auto mode) and
  permissions (how path patterns match).
- Revisit when Claude Code anchors project rules at a subagent's worktree, which would make the edit hook redundant, or
  when GSD brings executor work into the main checkout by a means other than `git merge`.

#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Claude Code PreToolUse hook for Edit, Write, MultiEdit and NotebookEdit (ADR-0015, AIDEV-08).
#
# The project ask rules `Edit(/<entry>)` anchor at the directory the session started in, so they never match a file in
# another worktree of the repository, such as the worktree under .claude/worktrees/<name>/ of a subagent dispatched
# with `isolation: worktree`. This hook asks the author before every edit whose file, taken relative to any worktree
# root of the repository, is a governed path. Without a governed match it prints nothing and leaves the call to the
# normal permission flow; it never allows a call.
#
# Input: the PreToolUse JSON on standard input, with the absolute path in tool_input.file_path (tool_input.notebook_path
# for NotebookEdit) and cwd. The governed list is ${CLAUDE_PROJECT_DIR}/governance/governed-paths.txt, the copy in the
# tree the session started in, which is where the project settings and their ask rules come from; without
# CLAUDE_PROJECT_DIR the hook uses the repository that contains it.
#
# The roots a path is taken relative to:
#   - the session root and the main worktree root;
#   - every worktree that `git worktree list` reports;
#   - <session root or main worktree root>/.claude/worktrees/<name>/, registered as a worktree or not.
# The path is checked twice: as written with `.` and `..` applied, and with the symbolic links of its longest existing
# prefix and of its final component resolved. The comparison ignores ASCII case, as the default macOS file system does.
# Any problem, such as missing jq, unreadable input, an unusable governed list or a failing git, is answered with "ask".
set -uo pipefail
export LC_ALL=C
unset CDPATH

# The answer when the shared scripts cannot be loaded; hook-common.sh defines the same text for later failures.
LOAD_FAILURE_ASK='{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"ask",'
LOAD_FAILURE_ASK+='"permissionDecisionReason":"Governance hook: the hook could not load its shared scripts, so it '
LOAD_FAILURE_ASK+='asks. Confirm that this call changes no governed path (governance/governed-paths.txt)."}}'
readonly LOAD_FAILURE_ASK

SCRIPT_DIR=$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P) || SCRIPT_DIR=''
# shellcheck source=/dev/null
if [[ -z $SCRIPT_DIR ]] || ! source "$SCRIPT_DIR/hook-common.sh" || ! source "$SCRIPT_DIR/governed-paths.sh"; then
  printf '%s\n' "$LOAD_FAILURE_ASK"
  exit 0
fi
install_fail_closed

# The most symbolic links followed for one path before the hook gives up and asks.
readonly MAX_LINK_HOPS=40
# Where Claude Code creates the worktrees of subagents and of `claude --worktree`, below a root.
readonly CLAUDE_WORKTREES=/.claude/worktrees/

# physical_path <variable> <absolute path>: stores the path with the symbolic links of its final component and of its
# longest existing directory prefix resolved, as the operating system would follow them, and the rest of the path
# appended through lexical_path. Fails when the links do not settle or the prefix cannot be entered.
physical_path() {
  local __path=$2 __target __directory __tail='' __hops=0
  while [[ -L $__path ]]; do
    __hops=$((__hops + 1))
    [[ $__hops -le $MAX_LINK_HOPS ]] || return 1
    __target=$(readlink -- "$__path") || return 1
    if [[ $__target == /* ]]; then
      __path=$__target
    else
      __path=${__path%/*}/$__target
    fi
  done
  __directory=$__path
  while [[ ! -d $__directory ]]; do
    __tail=/${__directory##*/}$__tail
    __directory=${__directory%/*}
    [[ -n $__directory ]] || __directory=/
  done
  __directory=$(cd -P -- "$__directory" 2>/dev/null && pwd -P) || return 1
  lexical_path "$1" "$__directory$__tail"
}

# add_root <path>: adds the path to ROOTS as written and resolved; an empty path adds nothing.
add_root() {
  local resolved
  [[ -n $1 ]] || return 0
  lexical_path resolved "$1"
  ROOTS+=("$resolved")
  if physical_path resolved "$1"; then
    ROOTS+=("$resolved")
  fi
}

# match_relative <relative path> <root>: asks the author when the relative path is governed.
match_relative() {
  local entry=''
  if match_governed entry "$1"; then
    ask_author "$1 in $2 is a governed path (entry \`$entry\` of governance/governed-paths.txt). \
Edits to governed paths need the author's confirmation (AIDEV-08)."
  fi
}

command -v jq >/dev/null 2>&1 || ask_author "jq is not on PATH, so the hook cannot read the call."
read_hook_input
input_string FILE_PATH '.tool_input.file_path // .tool_input.notebook_path' ||
  ask_author "the file path in the tool input is not a usable string."
input_string CWD '.cwd' || ask_author "the cwd in the hook input is not a usable string."
[[ -n $FILE_PATH ]] || ask_author "the tool input names no file path."
if [[ $FILE_PATH != /* ]]; then
  [[ $CWD == /* ]] || ask_author "the file path $FILE_PATH is relative and the hook input has no absolute cwd."
  FILE_PATH=$CWD/$FILE_PATH
fi

load_governed_globs "$SCRIPT_DIR"

COMMON_DIR=$(git -C "$SESSION_ROOT" rev-parse --path-format=absolute --git-common-dir 2>&1) ||
  ask_author "git cannot read the repository at $SESSION_ROOT: $COMMON_DIR"
MAIN_ROOT=''
if [[ $COMMON_DIR == */.git ]]; then
  MAIN_ROOT=${COMMON_DIR%/.git}
fi
# `-z` ends every field with a NUL, which the command substitution would drop, so it becomes \001 first.
WORKTREE_LIST=$(git -C "$SESSION_ROOT" worktree list --porcelain -z 2>&1 | tr '\0' '\001') ||
  ask_author "git cannot list the worktrees of $SESSION_ROOT: $WORKTREE_LIST"

ROOTS=()
add_root "$SESSION_ROOT"
add_root "$MAIN_ROOT"
# The roots that hold the .claude/worktrees/ directory of Claude Code, before the worktrees are added.
BASES=("${ROOTS[@]}")
rest=$WORKTREE_LIST
while [[ $rest == *$'\001'* ]]; do
  field=${rest%%$'\001'*}
  rest=${rest#*$'\001'}
  if [[ $field == 'worktree '* ]]; then
    add_root "${field#worktree }"
  fi
done

CANDIDATES=()
lexical_path candidate "$FILE_PATH"
CANDIDATES+=("$candidate")
if physical_path candidate "$FILE_PATH"; then
  CANDIDATES+=("$candidate")
else
  ask_author "the symbolic links of $FILE_PATH cannot be resolved."
fi

# check_candidate <absolute path>: asks the author when the path, taken relative to a root, is governed. The prefix
# test compares folded case; the relative path keeps the spelling of the candidate.
check_candidate() {
  local candidate=$1 folded_candidate='' folded='' root base relative
  fold_case folded_candidate "$candidate"
  for root in "${ROOTS[@]}"; do
    fold_case folded "$root"
    if [[ $folded != / && $folded_candidate == "$folded"/* ]]; then
      match_relative "${candidate:${#root}+1}" "$root"
    fi
  done
  for base in "${BASES[@]}"; do
    fold_case folded "$base"
    if [[ $folded != / && $folded_candidate == "$folded$CLAUDE_WORKTREES"*/* ]]; then
      relative=${candidate:${#base}+${#CLAUDE_WORKTREES}}
      match_relative "${relative#*/}" "$base$CLAUDE_WORKTREES${relative%%/*}"
    fi
  done
}

for candidate in "${CANDIDATES[@]}"; do
  check_candidate "$candidate"
done

no_decision

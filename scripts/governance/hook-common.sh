# SPDX-License-Identifier: MIT
# shellcheck shell=bash
# The Claude Code hook protocol shared by ask-governed-edit.sh and ask-governed-merge.sh (ADR-0015).
#
# A governance hook answers a PreToolUse call in one of two ways and exits 0 in both:
#   - ask_author <reason>: prints one JSON object with permissionDecision "ask", which sends the call to the author's
#     permission prompt in every permission mode, auto mode included;
#   - no_decision: prints nothing, so the normal permission flow decides.
# It never returns "allow". It fails closed: install_fail_closed makes any exit that did not go through one of the two
# functions, such as an unset variable or a failing command, an "ask" as well. Claude Code treats any other exit code,
# or a hook that cannot start or times out, as a non-blocking error unless the registration sets onFailure to "block",
# which .claude/settings.json does (checked by checkClaudeAskRules).
#
# Callers export LC_ALL=C and source governed-paths.sh first. Everything runs under bash 3.2, the bash macOS ships,
# because Claude Code starts the hooks with the first bash on PATH.

# Set by ask_author and no_decision, read by the EXIT trap.
HOOK_DECIDED=0

# The answer when jq, which writes the JSON, is unavailable or fails. It is a fixed string, so it needs no escaping.
HOOK_STATIC_ASK='{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"ask",'
HOOK_STATIC_ASK+='"permissionDecisionReason":"Governance hook: the hook could not finish its check, so it asks. '
HOOK_STATIC_ASK+='Confirm that this call changes no governed path (governance/governed-paths.txt)."}}'
readonly HOOK_STATIC_ASK

# The jq filter behind input_string: an absent or null value reads as the empty string, any other non-string value and
# a string with a NUL byte fail, and a sentinel protects trailing newlines from the command substitution.
readonly HOOK_STRING_FILTER='// "" | if type == "string" and ((explode | any(. == 0)) | not) then . + "x"
  else error("unusable") end'

# ask_author <reason>: answers "ask" with the reason, which the permission prompt shows, and exits 0.
ask_author() {
  HOOK_DECIDED=1
  if command -v jq >/dev/null 2>&1 &&
    jq -cn --arg reason "Governance hook: $1" \
      '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "ask",
        permissionDecisionReason: $reason}}' 2>/dev/null; then
    exit 0
  fi
  printf '%s\n' "$HOOK_STATIC_ASK"
  exit 0
}

# no_decision: leaves the call to the normal permission flow and exits 0.
no_decision() {
  HOOK_DECIDED=1
  exit 0
}

hook_on_exit() {
  local status=$?
  if [[ $HOOK_DECIDED -eq 0 ]]; then
    ask_author "the hook stopped before it decided (exit status $status), so it asks. Confirm that this call \
changes no governed path."
  fi
}

# install_fail_closed: turns every undecided exit into an "ask".
install_fail_closed() {
  trap hook_on_exit EXIT
}

# read_hook_input [text]: stores the PreToolUse JSON in HOOK_INPUT, the given text or else standard input, and checks
# that it is a JSON object.
read_hook_input() {
  if [[ $# -gt 0 ]]; then
    HOOK_INPUT=$1
  else
    HOOK_INPUT=$(cat) || ask_author "the hook could not read its input."
  fi
  jq -e 'type == "object"' >/dev/null 2>&1 <<<"$HOOK_INPUT" || ask_author "the hook input is not a JSON object."
}

# input_string <variable> <jq path>: stores the string at the path of HOOK_INPUT, or the empty string when the path is
# absent or null. Any other value, and a string that holds a NUL byte, fails.
input_string() {
  local __value
  __value=$(jq -r "($2) $HOOK_STRING_FILTER" 2>/dev/null <<<"$HOOK_INPUT") || return 1
  printf -v "$1" '%s' "${__value%x}"
}

# load_governed_globs <hook directory>: sets SESSION_ROOT to CLAUDE_PROJECT_DIR, the directory the session started in,
# or without it to the repository that contains the hook, and reads SESSION_ROOT/governance/governed-paths.txt into
# GLOBS and, folded to lower case, FOLDED_GLOBS. The list comes from the session's tree, where the project settings
# and their ask rules come from, never from a worktree an agent edits.
load_governed_globs() {
  local text glob folded=''
  SESSION_ROOT=${CLAUDE_PROJECT_DIR:-}
  if [[ -z $SESSION_ROOT ]]; then
    SESSION_ROOT=$(cd -P -- "$1/../.." && pwd -P) || ask_author "cannot find the repository of the hook."
  fi
  [[ $SESSION_ROOT == /* ]] || ask_author "CLAUDE_PROJECT_DIR is not an absolute path: $SESSION_ROOT"
  text=$(governed_globs "$SESSION_ROOT/governance/governed-paths.txt" 2>&1) ||
    ask_author "the governed paths list cannot be used: $text"
  GLOBS=()
  FOLDED_GLOBS=()
  while IFS= read -r glob; do
    GLOBS+=("$glob")
    fold_case folded "$glob"
    FOLDED_GLOBS+=("$folded")
  done <<<"$text"
}

# The functions below store their result in the variable the caller names. Their own locals start with `__`, so they
# never shadow that variable through bash's dynamic scoping.

# match_governed <variable> <relative path>: stores the governed entry the path matches, compared in folded ASCII case,
# in the variable unless its name is empty, or fails when the path matches no entry.
match_governed() {
  local __folded='' __index
  fold_case __folded "$2"
  for ((__index = 0; __index < ${#FOLDED_GLOBS[@]}; __index++)); do
    if glob_match "$__folded" "${FOLDED_GLOBS[__index]}"; then
      if [[ -n $1 ]]; then
        printf -v "$1" '%s' "${GLOBS[__index]}"
      fi
      return 0
    fi
  done
  return 1
}

# fold_case <variable> <text>: stores the text with ASCII upper case letters folded to lower case. Only the comparison
# folds, so a reason still shows the path as written.
fold_case() {
  local __text=$2 __upper=ABCDEFGHIJKLMNOPQRSTUVWXYZ __lower=abcdefghijklmnopqrstuvwxyz __index
  for ((__index = 0; __index < 26; __index++)); do
    __text=${__text//${__upper:__index:1}/${__lower:__index:1}}
  done
  printf -v "$1" '%s' "$__text"
}

# lexical_path <variable> <absolute path>: stores the path with empty and `.` components removed and each `..` applied
# to the component before it, without looking at the file system. `..` at the root stays at the root.
lexical_path() {
  local __rest=$2 __component __result=''
  local -a __stack=()
  while [[ -n $__rest ]]; do
    __component=${__rest%%/*}
    if [[ $__rest == */* ]]; then
      __rest=${__rest#*/}
    else
      __rest=''
    fi
    case $__component in
    '' | .) ;;
    ..)
      if [[ ${#__stack[@]} -gt 0 ]]; then
        unset '__stack[${#__stack[@]}-1]'
      fi
      ;;
    *) __stack+=("$__component") ;;
    esac
  done
  for __component in ${__stack[@]+"${__stack[@]}"}; do
    __result+=/$__component
  done
  printf -v "$1" '%s' "${__result:-/}"
}

# join_path <variable> <base> <path>: stores the path, made absolute against the base when it is relative, through
# lexical_path. A leading `~` stands for HOME. An empty base means "unknown" and gives an empty result for a relative
# path.
join_path() {
  local __base=$2 __path=$3
  if [[ $__path == \~ || $__path == \~/* ]]; then
    __path=${HOME:-}${__path#\~}
  fi
  if [[ $__path != /* ]]; then
    if [[ -z $__base ]]; then
      printf -v "$1" '%s' ''
      return 0
    fi
    __path=$__base/$__path
  fi
  lexical_path "$1" "$__path"
}

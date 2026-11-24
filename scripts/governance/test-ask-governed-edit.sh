#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Self-contained tests for ask-governed-edit.sh: every case runs the real hook in a clean environment against a
# throwaway repository with linked worktrees, and asserts that it either asks with a matching reason or stays silent.
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
HOOK="$SCRIPT_DIR/ask-governed-edit.sh"
BASH_BIN=$(command -v bash)

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

passed=0
failed=0

# Git runs without the user's or the system's configuration, so a local setting cannot change a case.
export GIT_CONFIG_NOSYSTEM=1 GIT_CONFIG_GLOBAL=/dev/null
git_quiet() {
  git -c user.name=test -c user.email=test@example.invalid -c init.defaultBranch=main "$@" >/dev/null 2>&1
}

# The repository the cases edit: a governed list, a few files, two linked worktrees (one under .claude/worktrees/, one
# outside the repository) and symbolic links into governed paths.
REPO="$TMP/repo"
mkdir -p "$REPO/governance" "$REPO/docs"
cat >"$REPO/governance/governed-paths.txt" <<'EOF'
# Test list.
governance/**
mise.toml
mise/**
.claude/settings.json
CODEOWNERS
EOF
echo 'tools' >"$REPO/mise.toml"
echo 'docs' >"$REPO/docs/readme.md"
git_quiet init "$REPO"
git_quiet -C "$REPO" add -A
git_quiet -C "$REPO" commit -m initial
git_quiet -C "$REPO" worktree add "$REPO/.claude/worktrees/agent-a" -b worktree-agent-a
git_quiet -C "$REPO" worktree add "$TMP/elsewhere" -b elsewhere
ln -s governance "$REPO/linked-dir"
ln -s ../mise.toml "$REPO/docs/alias.toml"
AGENT="$REPO/.claude/worktrees/agent-a"

# edit_input <tool> <path> [cwd]: the PreToolUse input for a file tool call.
edit_input() {
  local tool=$1 path=$2 cwd=${3:-$REPO}
  if [[ $tool == NotebookEdit ]]; then
    jq -cn --arg t "$tool" --arg p "$path" --arg c "$cwd" \
      '{session_id: "s", hook_event_name: "PreToolUse", tool_name: $t,
        tool_input: {notebook_path: $p, new_source: "x"}, cwd: $c}'
  else
    jq -cn --arg t "$tool" --arg p "$path" --arg c "$cwd" \
      '{session_id: "s", hook_event_name: "PreToolUse", tool_name: $t,
        tool_input: {file_path: $p, content: "x"}, cwd: $c}'
  fi
}

# check <name> <ask|none> <reason regex> <exit code> <output>: records one result.
check() {
  local name=$1 expectation=$2 reason=$3 code=$4 output=$5 ok=0
  if [[ $code -eq 0 && $output != *allow* ]]; then
    case $expectation in
    ask)
      if jq -e --arg pattern "$reason" '
          .hookSpecificOutput.hookEventName == "PreToolUse"
          and .hookSpecificOutput.permissionDecision == "ask"
          and (.hookSpecificOutput.permissionDecisionReason | test($pattern))' >/dev/null 2>&1 <<<"$output"; then
        ok=1
      fi
      ;;
    none) [[ -z $output ]] && ok=1 ;;
    esac
  fi
  if [[ $ok -eq 1 ]]; then
    passed=$((passed + 1))
    echo "ok   - $name"
  else
    failed=$((failed + 1))
    echo "FAIL - $name (exit $code, expected exit 0 and $expectation; reason pattern: $reason)"
    while IFS= read -r line; do
      echo "       | $line"
    done <<<"$output"
  fi
}

# run_case <name> <ask|none> <reason regex> <input JSON> [VAR=value ...]: runs the hook with CLAUDE_PROJECT_DIR set to
# the test repository unless a case names it again.
run_case() {
  local name=$1 expectation=$2 reason=$3 input=$4
  shift 4
  local output code=0
  output=$(env -i PATH="$PATH" HOME="$TMP" GIT_CONFIG_NOSYSTEM=1 GIT_CONFIG_GLOBAL=/dev/null \
    CLAUDE_PROJECT_DIR="$REPO" "$@" "$BASH_BIN" "$HOOK" <<<"$input" 2>/dev/null) || code=$?
  check "$name" "$expectation" "$reason" "$code" "$output"
}

# --- the session's own tree ---------------------------------------------------------------------------------------

run_case "a governed file in the session root asks and names the entry" ask "mise\\.toml.*entry \`mise\\.toml\`" \
  "$(edit_input Edit "$REPO/mise.toml")"
run_case "a new file under a governed directory asks" ask "governance/new\\.json.*\`governance/\\*\\*\`" \
  "$(edit_input Write "$REPO/governance/new.json")"
run_case "a file that is not governed passes silently" none '' "$(edit_input Edit "$REPO/docs/readme.md")"
run_case "a file outside every worktree passes silently" none '' "$(edit_input Write "$TMP/outside/mise.toml")"
run_case "a governed name in a subdirectory is not governed" none '' \
  "$(edit_input Write "$REPO/docs/mise.toml")"

# --- other worktrees (the gap the project ask rules leave) ----------------------------------------------------------

run_case "a governed file in an agent worktree under .claude/worktrees asks" ask \
  'governance/governed-paths\.txt in .*/agent-a is a governed path' \
  "$(edit_input Edit "$AGENT/governance/governed-paths.txt" "$AGENT")"
run_case "the settings file of an agent worktree asks" ask '\.claude/settings\.json' \
  "$(edit_input Write "$AGENT/.claude/settings.json" "$AGENT")"
run_case "a governed file in a worktree outside the repository asks" ask 'mise/config\.toml' \
  "$(edit_input Write "$TMP/elsewhere/mise/config.toml" "$TMP/elsewhere")"
run_case "a governed file under an unregistered .claude/worktrees directory asks" ask 'CODEOWNERS.*ghost' \
  "$(edit_input Write "$REPO/.claude/worktrees/ghost/CODEOWNERS")"
run_case "a file that is not governed in an agent worktree passes silently" none '' \
  "$(edit_input Edit "$AGENT/docs/readme.md" "$AGENT")"
run_case "a session started inside a worktree still guards the main checkout" ask 'mise\.toml' \
  "$(edit_input Edit "$REPO/mise.toml" "$AGENT")" CLAUDE_PROJECT_DIR="$AGENT"

# --- path spellings --------------------------------------------------------------------------------------------------

run_case "a path through .. into a governed directory asks" ask 'governance/x\.json' \
  "$(edit_input Write "$REPO/docs/../governance/x.json")"
run_case "a path with . and doubled slashes asks" ask 'mise\.toml' "$(edit_input Edit "$REPO/./docs/..//mise.toml")"
run_case "a symbolic link to a governed directory asks" ask 'governance/new\.json' \
  "$(edit_input Write "$REPO/linked-dir/new.json")"
run_case "a symbolic link to a governed file asks" ask 'mise\.toml' "$(edit_input Edit "$REPO/docs/alias.toml")"
run_case "upper case spelling of a governed file asks" ask 'MISE\.TOML' "$(edit_input Write "$REPO/MISE.TOML")"
run_case "mixed case spelling of a governed directory asks" ask 'Governance/new\.json' \
  "$(edit_input Write "$REPO/Governance/new.json")"
run_case "a relative path resolves against the cwd" ask 'governance/x\.json' \
  "$(edit_input Write governance/x.json "$REPO")"

# --- tools ------------------------------------------------------------------------------------------------------------

run_case "MultiEdit on a governed file asks" ask 'mise\.toml' "$(edit_input MultiEdit "$AGENT/mise.toml" "$AGENT")"
run_case "NotebookEdit reads notebook_path and asks" ask 'governance/a\.ipynb' \
  "$(edit_input NotebookEdit "$AGENT/governance/a.ipynb" "$AGENT")"

# --- the governed list and its location ---------------------------------------------------------------------------

run_case "without CLAUDE_PROJECT_DIR the hook uses the repository that contains it" ask \
  'scripts/governance/ask-governed-edit\.sh' "$(edit_input Edit "$HOOK")" CLAUDE_PROJECT_DIR=
BROKEN="$TMP/broken"
mkdir -p "$BROKEN/governance"
printf 'mise.toml\n\xc2\xa0.mcp.json\n' >"$BROKEN/governance/governed-paths.txt"
git_quiet init "$BROKEN"
run_case "an unusable governed list asks and names the offending line" ask \
  'cannot be used.*line 2: entry contains a character outside printable ASCII' \
  "$(edit_input Edit "$BROKEN/docs/a.md" "$BROKEN")" CLAUDE_PROJECT_DIR="$BROKEN"
run_case "a missing governed list asks" ask 'governed paths file not found' \
  "$(edit_input Edit "$TMP/outside/a.md")" CLAUDE_PROJECT_DIR="$TMP/outside"
NOT_A_REPO="$TMP/not-a-repo"
mkdir -p "$NOT_A_REPO/governance"
echo 'mise.toml' >"$NOT_A_REPO/governance/governed-paths.txt"
run_case "a session root that is not a git repository asks" ask 'git cannot read the repository' \
  "$(edit_input Edit "$NOT_A_REPO/docs/a.md")" CLAUDE_PROJECT_DIR="$NOT_A_REPO"
run_case "a relative CLAUDE_PROJECT_DIR asks" ask 'not an absolute path' \
  "$(edit_input Edit "$REPO/docs/readme.md")" CLAUDE_PROJECT_DIR=repo

# --- malformed input fails closed ---------------------------------------------------------------------------------

run_case "input that is not JSON asks" ask 'not a JSON object' '{"tool_name": "Edit", '
run_case "a JSON array instead of an object asks" ask 'not a JSON object' '[1, 2]'
run_case "a missing file path asks" ask 'names no file path' \
  '{"tool_name": "Write", "tool_input": {"content": "x"}, "cwd": "/"}'
run_case "a file path that is not a string asks" ask 'not a usable string' \
  '{"tool_name": "Write", "tool_input": {"file_path": 7}, "cwd": "/"}'
run_case "a file path with a NUL byte asks" ask 'not a usable string' \
  '{"tool_name": "Write", "tool_input": {"file_path": "/a\u0000b"}, "cwd": "/"}'
run_case "a relative file path without a cwd asks" ask 'no absolute cwd' \
  '{"tool_name": "Write", "tool_input": {"file_path": "mise.toml"}}'

LOOP="$REPO/docs/loop"
ln -s loop "$LOOP"
run_case "a symbolic link loop asks" ask 'cannot be resolved' "$(edit_input Write "$LOOP")"
rm -f "$LOOP"

# A PATH with the commands the hook needs, without jq: the hook still answers, with the static reason.
NO_JQ="$TMP/no-jq"
mkdir -p "$NO_JQ"
for tool in cat git tr readlink dirname; do
  ln -s "$(command -v "$tool")" "$NO_JQ/$tool"
done
run_case "without jq the hook asks with the static reason" ask 'could not finish its check' \
  "$(edit_input Edit "$REPO/docs/readme.md")" PATH="$NO_JQ"

# The hook alone, without the shared scripts next to it.
LONELY="$TMP/lonely"
mkdir -p "$LONELY"
cp "$HOOK" "$LONELY/"
lonely_output=$(env -i PATH="$PATH" CLAUDE_PROJECT_DIR="$REPO" "$BASH_BIN" "$LONELY/ask-governed-edit.sh" \
  <<<"$(edit_input Edit "$REPO/docs/readme.md")" 2>/dev/null) && lonely_code=0 || lonely_code=$?
check "a hook without its shared scripts asks" ask 'could not load its shared scripts' "$lonely_code" "$lonely_output"

# --- the fail-closed protocol of hook-common.sh -------------------------------------------------------------------

# fail_closed_case <name> <reason regex> <script>: runs the script after loading hook-common.sh and installing the
# fail-closed trap.
fail_closed_case() {
  local name=$1 reason=$2 script=$3 output code=0
  output=$(env -i PATH="$PATH" LC_ALL=C "$BASH_BIN" -uc \
    "source \"$SCRIPT_DIR/hook-common.sh\"; install_fail_closed; $script" 2>/dev/null) || code=$?
  check "$name" ask "$reason" "$code" "$output"
}
fail_closed_case "an unexpected exit becomes an ask that names the status" 'exit status 3' 'exit 3'
fail_closed_case "an unset variable becomes an ask" 'stopped before it decided' "echo \"\$UNSET_VARIABLE\""
fail_closed_case "a reason with quotes and backslashes stays valid JSON" 'say "hi" \\ there' \
  'ask_author "say \"hi\" \\ there"'

echo "$passed passed, $failed failed"
[[ $failed -eq 0 ]]

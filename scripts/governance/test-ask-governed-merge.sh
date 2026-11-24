#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Self-contained tests for ask-governed-merge.sh: every case runs the real hook in a clean environment on a Bash command
# against a throwaway repository whose branches change governed or ungoverned files, and asserts that it either asks
# with a matching reason or stays silent.
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
HOOK="$SCRIPT_DIR/ask-governed-merge.sh"
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

REPO="$TMP/repo"
mkdir -p "$REPO/governance" "$REPO/docs"
cat >"$REPO/governance/governed-paths.txt" <<'EOF'
# Test list.
governance/**
mise.toml
.github/**
EOF
echo 'tools' >"$REPO/mise.toml"
echo 'docs' >"$REPO/docs/readme.md"
echo '{}' >"$REPO/governance/policy.json"
git_quiet init "$REPO"
git_quiet -C "$REPO" add -A
git_quiet -C "$REPO" commit -m initial

# branch_from <branch> <parent> <change>...: creates the branch on a commit after the parent without touching any
# working tree. A change is `add=<path>` (the file gets new content), `remove=<path>` or `move=<from>:<to>` (the content
# moves unchanged, so git sees a rename).
branch_from() {
  local branch=$1 parent=$2 change blob tree commit index="$TMP/index"
  shift 2
  rm -f "$index"
  GIT_INDEX_FILE="$index" git -C "$REPO" read-tree "$parent"
  for change in "$@"; do
    case $change in
    add=*)
      blob=$(echo "$branch" | git -C "$REPO" hash-object -w --stdin)
      GIT_INDEX_FILE="$index" git -C "$REPO" update-index --add --cacheinfo "100644,$blob,${change#add=}"
      ;;
    remove=*) GIT_INDEX_FILE="$index" git -C "$REPO" update-index --force-remove "${change#remove=}" ;;
    move=*)
      change=${change#move=}
      blob=$(git -C "$REPO" rev-parse "$parent:${change%%:*}")
      GIT_INDEX_FILE="$index" git -C "$REPO" update-index --force-remove "${change%%:*}"
      GIT_INDEX_FILE="$index" git -C "$REPO" update-index --add --cacheinfo "100644,$blob,${change#*:}"
      ;;
    esac
  done
  tree=$(GIT_INDEX_FILE="$index" git -C "$REPO" write-tree)
  commit=$(git -c user.name=test -c user.email=test@example.invalid -C "$REPO" commit-tree "$tree" -p "$parent" \
    -m "$branch")
  git -C "$REPO" branch "$branch" "$commit"
}

branch_from governed main add=mise.toml
branch_from docs-only main add=docs/readme.md
branch_from rename-out main move=governance/policy.json:docs/policy.json
branch_from upper-case main add=MISE.TOML
many=()
for n in $(seq 1 25); do
  many+=("add=.github/workflows/w$n.yml")
done
branch_from many main "${many[@]}"
git -C "$REPO" branch merged-already main
unrelated_tree=$(echo x | git -C "$REPO" hash-object -w --stdin)
unrelated_tree=$(printf '100644 blob %s\tx\n' "$unrelated_tree" | git -C "$REPO" mktree)
unrelated=$(git -c user.name=test -c user.email=test@example.invalid -C "$REPO" commit-tree "$unrelated_tree" -m x)
git -C "$REPO" branch unrelated "$unrelated"

# An agent worktree with a governed change, merged back by GSD's cleanup-wave.
AGENT="$REPO/.claude/worktrees/agent-a"
branch_from worktree-agent-a main add=governance/new.json
git_quiet -C "$REPO" worktree add "$AGENT" worktree-agent-a
manifest() {
  jq -n --arg b "$2" --arg p "$AGENT" \
    '{orchestrator_root: "x", worktrees: [{agent_id: "a", worktree_path: $p, branch: $b, expected_base: "main"}]}' \
    >"$TMP/$1"
  echo "$TMP/$1"
}
GOVERNED_MANIFEST=$(manifest governed-manifest.json worktree-agent-a)
DOCS_MANIFEST=$(manifest docs-manifest.json docs-only)
echo '{"worktrees": [' >"$TMP/malformed-manifest.json"

# bash_input <command> [cwd]: the PreToolUse input for a Bash call.
bash_input() {
  jq -cn --arg c "$1" --arg d "${2:-$REPO}" \
    '{session_id: "s", hook_event_name: "PreToolUse", tool_name: "Bash",
      tool_input: {command: $c, description: "test"}, cwd: $d}'
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

# run_raw <name> <ask|none> <reason regex> <input> [VAR=value ...]: runs the hook on the input as given.
run_raw() {
  local name=$1 expectation=$2 reason=$3 input=$4
  shift 4
  local output code=0
  output=$(env -i PATH="$PATH" HOME="$TMP" GIT_CONFIG_NOSYSTEM=1 GIT_CONFIG_GLOBAL=/dev/null \
    CLAUDE_PROJECT_DIR="$REPO" "$@" "$BASH_BIN" "$HOOK" <<<"$input" 2>/dev/null) || code=$?
  check "$name" "$expectation" "$reason" "$code" "$output"
}

# run_case <name> <ask|none> <reason regex> <command> [cwd]: runs the hook on a Bash call.
run_case() {
  run_raw "$1" "$2" "$3" "$(bash_input "$4" "${5:-$REPO}")"
}

# The whole reason for a merge of the branch `governed` alone: one file and no problem.
ONLY_MISE='^Governance hook: this command merges changes to governed paths: mise\.toml \(from governed\)\. '
ONLY_MISE+='Merges of governed paths need the author.s confirmation \(AIDEV-08\)\.$'

# --- commands without a merge -------------------------------------------------------------------------------------

run_case "an ordinary command passes silently" none '' 'ls -la'
run_case "git merge-base is not a merge" none '' 'git merge-base HEAD governed && echo ok'
run_case "echo merge is not a merge" none '' 'echo merge'
run_case "a commit message that names a merge is not a merge" none '' 'git commit -m "git merge governed"'
run_case "a quoted heredoc that names a merge is not a merge" none '' \
  $'git commit -F - <<\'EOF\'\ngit merge governed\nEOF\necho done'
run_case "a comment that names a merge is not a merge" none '' $'ls # git merge governed'
run_case "grep for cleanup-wave is not a merge" none '' 'grep -n cleanup-wave worktree-safety.cjs'
run_case "cleanup-wave without --manifest merges nothing" none '' 'gsd-tools query worktree.cleanup-wave'

# --- git merge ------------------------------------------------------------------------------------------------------

run_case "merging a branch that changes a governed file asks and lists it" ask "$ONLY_MISE" 'git merge governed'
run_case "merging a branch that changes no governed file passes silently" none '' 'git merge docs-only'
run_case "merging a branch that is already merged passes silently" none '' 'git merge merged-already'
run_case "merge options with values are not taken for refs" none '' \
  'git merge --no-ff --no-edit -m "merge governed" -X theirs -s ort docs-only'
run_case "a cluster of short options ending in m takes the next word" none '' 'git merge -qm governed docs-only'
run_case "git merge --abort passes silently" none '' 'git merge --abort'
run_case "git merge --continue without a merge in progress passes silently" none '' 'git merge --continue'
run_case "a redirection is not taken for a ref" ask "$ONLY_MISE" 'git merge governed 2>&1 | tail -5'
run_case "every ref of an octopus merge is checked" ask "$ONLY_MISE" 'git merge docs-only governed'
run_case "a file renamed out of a governed path asks" ask 'governance/policy\.json \(from rename-out\)' \
  'git merge rename-out'
run_case "a governed path in another letter case asks" ask 'MISE\.TOML \(from upper-case\)' 'git merge upper-case'
run_case "a long list of governed files is cut short" ask 'w4\.yml \(from many\), and 5 more\.' 'git merge many'

# --- where the merge runs -------------------------------------------------------------------------------------------

run_case "git -C names the repository" ask "$ONLY_MISE" "git -C '$REPO' merge governed" "$TMP"
run_case "cd before the merge names the repository" ask "$ONLY_MISE" "cd '$REPO' && git merge governed" "$TMP"
run_case "a subshell with cd names the repository" ask "$ONLY_MISE" "(cd '$REPO'; git merge governed)" "$TMP"
run_case "a merge from a directory outside any repository asks" ask 'cannot find the git repository' \
  'git merge governed' "$TMP"
run_case "a merge in an agent worktree is checked against that worktree" ask 'mise\.toml \(from governed\)' \
  'git merge governed' "$AGENT"

# --- wrappers and nesting -----------------------------------------------------------------------------------------

run_case "env with assignments in front of git asks" ask "$ONLY_MISE" 'env GIT_EDITOR=true git merge governed'
run_case "an assignment in front of git asks" ask "$ONLY_MISE" 'GIT_MERGE_AUTOEDIT=no git merge governed'
run_case "command and timeout in front of git ask" ask "$ONLY_MISE" 'command timeout 30 git merge governed'
run_case "git under if asks" ask "$ONLY_MISE" 'if git merge governed; then echo ok; fi'
run_case "a negated git merge asks" ask "$ONLY_MISE" '! git merge governed'
run_case "git after && in a list asks" ask "$ONLY_MISE" 'git fetch -q . && git merge governed'
run_case "a command substitution inside double quotes asks" ask "$ONLY_MISE" "echo \"\$(git merge governed)\""
run_case "bash -c asks" ask "$ONLY_MISE" "bash -lc 'git merge governed'"
run_case "eval asks" ask "$ONLY_MISE" 'eval "git merge governed"'
run_case "a heredoc fed to a shell asks" ask "$ONLY_MISE" $'bash <<\'EOF\'\ngit merge governed\nEOF'
run_case "a merge after a heredoc is still checked" ask "$ONLY_MISE" $'cat <<\'EOF\'\ntext\nEOF\ngit merge governed'
run_case "a merge after a heredoc with tabs stripped is still checked" ask "$ONLY_MISE" \
  $'cat <<-EOF\n\ttext\n\tEOF\ngit merge governed'
run_case "a heredoc that expands a merge asks" ask 'heredoc expands a command substitution' \
  $'cat <<EOF\n$(git merge governed)\nEOF'

# --- merges the hook cannot resolve ---------------------------------------------------------------------------------

run_case "a ref from a shell variable asks" ask "the merged ref '\\\$BRANCH': it holds a shell expansion" \
  "git merge \"\$BRANCH\""
run_case "a ref that does not exist asks" ask "cannot resolve the merged ref 'does-not-exist'" \
  'git merge does-not-exist'
run_case "a merge without a ref and without an upstream asks" ask "cannot resolve the merged ref '@\\{upstream\\}'" \
  'git merge'
run_case "an unrelated history asks" ask "cannot list what 'unrelated' changes" 'git merge unrelated'
run_case "a repository from a shell variable asks" ask 'cannot tell which repository' "git -C \"\$X\" merge docs-only"
run_case "--git-dir asks" ask 'cannot tell which repository' 'git --git-dir=.git merge docs-only'
run_case "refs from xargs ask" ask 'refs from xargs' 'xargs git merge <branches.txt'
run_case "a program named by a variable with merge asks" ask 'named by a shell expansion' "\$GIT merge governed"
run_case "an unterminated quote with a merge asks" ask 'leaves quoting' 'git merge "governed'

# --- GSD cleanup-wave ---------------------------------------------------------------------------------------------

GSD='node .claude/gsd-core/bin/gsd-tools.cjs'
run_case "cleanup-wave of a branch with a governed change asks" ask 'governance/new\.json \(from worktree-agent-a\)' \
  "$GSD query worktree.cleanup-wave --manifest $GOVERNED_MANIFEST 2>&1 | tail -20"
run_case "cleanup-wave of a branch without a governed change passes silently" none '' \
  "$GSD query worktree.cleanup-wave --manifest $DOCS_MANIFEST"
run_case "the non-query form of cleanup-wave asks" ask 'governance/new\.json' \
  "$GSD worktree cleanup-wave --manifest $GOVERNED_MANIFEST"
run_case "a relative manifest resolves against the cwd" ask 'governance/new\.json' \
  "$GSD query worktree.cleanup-wave --manifest ../governed-manifest.json"
run_case "a manifest from a shell variable asks" ask 'cannot resolve the manifest' \
  "$GSD query worktree.cleanup-wave --manifest \"\$WAVE_WORKTREE_MANIFEST\" || exit 1"
run_case "a missing manifest asks" ask 'cannot read the worktree.cleanup-wave manifest' \
  "$GSD query worktree.cleanup-wave --manifest $TMP/missing.json"
run_case "a malformed manifest asks" ask 'cannot read the worktree.cleanup-wave manifest' \
  "$GSD query worktree.cleanup-wave --manifest $TMP/malformed-manifest.json"

# --- malformed input and missing tools fail closed ----------------------------------------------------------------

run_raw "input that is not JSON but mentions merge asks" ask 'not a JSON object' '{"command": "git merge x"'
run_raw "a command that is not a string asks" ask 'not a usable string' \
  '{"tool_name": "Bash", "tool_input": {"command": ["git", "merge"]}, "cwd": "/"}'
run_raw "an input without a command passes silently" none '' \
  '{"tool_name": "Bash", "tool_input": {"description": "merge"}, "cwd": "/"}'
run_raw "a unicode escape takes the full check" ask "$ONLY_MISE" \
  "$(bash_input 'git merge governed' | sed 's/merge/\\u006derge/')"

NO_JQ="$TMP/no-jq"
mkdir -p "$NO_JQ"
for tool in cat git tr dirname; do
  ln -s "$(command -v "$tool")" "$NO_JQ/$tool"
done
run_raw "without jq a merge command asks with the static reason" ask 'could not finish its check' \
  "$(bash_input 'git merge governed')" PATH="$NO_JQ"
run_raw "without jq an ordinary command passes silently" none '' "$(bash_input 'ls -la')" PATH="$NO_JQ"

LONELY="$TMP/lonely"
mkdir -p "$LONELY"
cp "$HOOK" "$LONELY/"
lonely_output=$(env -i PATH="$PATH" CLAUDE_PROJECT_DIR="$REPO" "$BASH_BIN" "$LONELY/ask-governed-merge.sh" \
  <<<"$(bash_input 'git merge governed')" 2>/dev/null) && lonely_code=0 || lonely_code=$?
check "a hook without its shared scripts asks on a merge" ask 'could not read its input or load its shared scripts' \
  "$lonely_code" "$lonely_output"
lonely_output=$(env -i PATH="$PATH" CLAUDE_PROJECT_DIR="$REPO" "$BASH_BIN" "$LONELY/ask-governed-merge.sh" \
  <<<"$(bash_input 'ls -la')" 2>/dev/null) && lonely_code=0 || lonely_code=$?
check "a hook without its shared scripts passes an ordinary command" none '' "$lonely_code" "$lonely_output"

run_raw "a missing governed list asks" ask 'governed paths file not found' \
  "$(bash_input 'git merge governed')" CLAUDE_PROJECT_DIR="$TMP"

echo "$passed passed, $failed failed"
[[ $failed -eq 0 ]]

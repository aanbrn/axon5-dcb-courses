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
run_case "a merge word that quotes split in a commit message is not a merge" none '' "git commit -m \"mer''ge\""
run_case "a left shift and a right shift in arithmetic expansion are not heredocs" none '' \
  $'echo $((1<<2)) $((8>>1))'
run_case "a heredoc without a merge is not an open heredoc" none '' $'cat <<NEVER\ntext'
run_case "an arithmetic expansion next to merge-base is not a merge" none '' \
  $'x=$((1<<2))\ngit merge-base HEAD governed'

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
run_case "git after && in a list asks" ask "$ONLY_MISE" 'git status && git merge governed'
run_case "git fetch before a merge asks about the files and about the moved ref" ask \
  'mise\.toml \(from governed\).*git fetch .*may move a ref' 'git fetch -q . && git merge governed'
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

# --- a merge word that quoting, backslashes or continuations split --------------------------------------------------

run_case "quotes inside the merge word ask" ask "$ONLY_MISE" "git mer''ge governed"
run_case "a backslash inside the merge word asks" ask "$ONLY_MISE" 'git m\erge governed'
run_case "double quotes inside the merge word ask" ask "$ONLY_MISE" 'git "mer"ge governed'
run_case "a line continuation inside the merge word asks" ask "$ONLY_MISE" $'git mer\\\nge governed'
run_case "an ANSI-C quote inside the merge word asks" ask 'subcommand given by a shell expansion' \
  "git m\$'e'rge governed"

# --- a ref that an earlier part of the same command moves -----------------------------------------------------------
# The hook checks the refs as they are now, so a command that merges and also moves a ref or HEAD asks, in either order.

run_case "git branch -f before a merge asks" ask 'may move a ref' \
  'git branch -f merged-already governed && git merge merged-already'
run_case "git checkout -B and checkout before a merge ask" ask 'may move a ref' \
  'git checkout -B merged-already governed && git checkout main && git merge merged-already'
run_case "git switch before a merge asks" ask 'may move a ref' 'git switch main && git merge docs-only'
run_case "git pull before a merge asks" ask 'may move a ref' 'git pull && git merge docs-only'
run_case "git fetch behind mise x -- before a merge asks" ask 'may move a ref' \
  'mise x -- git fetch && git merge docs-only'
run_case "git branch -d after a merge passes silently" none '' 'git merge docs-only && git branch -d docs-only'
run_case "git stash around a merge passes silently" none '' 'git stash && git merge docs-only && git stash pop'
run_case "git commit before a merge passes silently" none '' 'git commit -m wip && git merge docs-only'
run_case "a ref mover without a merge passes silently" none '' 'git fetch -q . && git merge-base HEAD governed'

# --- the reason lists every problem and file once -------------------------------------------------------------------

REPEATED=''
for n in $(seq 1 30); do
  REPEATED+="\$R merge x; "
done
ONE_PROBLEM='^Governance hook: the hook could not check every merge in this command '
ONE_PROBLEM+="\\(the command runs a program named by a shell expansion: \\\$R\\), so it asks\\. "
ONE_PROBLEM+='Confirm that the command merges no governed path\.$'
run_case "thirty identical problems are one problem" ask "$ONE_PROBLEM" "$REPEATED"
DISTINCT=''
for n in $(seq 1 12); do
  DISTINCT+="\$R$n merge x; "
done
run_case "problems are capped at ten with a count of the rest" ask "\\\$R10; and 2 more\\), so it asks\\." "$DISTINCT"
run_case "a governed file merged twice is listed once" ask "$ONLY_MISE" 'git merge governed; git merge governed'

# --- arithmetic is not a heredoc ----------------------------------------------------------------------------------

run_case "a left shift in an arithmetic expansion does not swallow the next line" ask "$ONLY_MISE" \
  $'x=$((1<<2))\ngit merge governed'
run_case "a left shift in an arithmetic command does not swallow the next line" ask "$ONLY_MISE" \
  $'((x = 1<<2))\ngit merge governed'
run_case "a later line equal to the would-be delimiter does not hide the merge" ask "$ONLY_MISE" \
  $'x=$((1<<true))\ngit merge governed\ntrue'
run_case "a left shift in a C-style for loop does not swallow the next line" ask "$ONLY_MISE" \
  $'for ((i = 1<<2; i < 9; i++)); do :; done\ngit merge governed'
run_case "a left shift in arithmetic inside double quotes does not swallow the next line" ask "$ONLY_MISE" \
  $'echo "$((1<<2))"\ngit merge governed'
run_case "a heredoc whose delimiter never comes leaves the lexing incomplete" ask \
  'leaves quoting, a substitution or a heredoc open' $'cat <<NEVER\ngit merge governed'
run_case "a command substitution inside arithmetic is still analysed" ask "$ONLY_MISE" \
  $'echo $(( $(git merge governed) ))'
run_case "a subshell that looks like an arithmetic expansion leaves the lexing incomplete" ask \
  'leaves quoting, a substitution or a heredoc open' $'echo $((git merge governed); true)'

# --- scripts handed to a shell ------------------------------------------------------------------------------------

run_case "a here-string fed to a shell is analysed" ask "$ONLY_MISE" 'bash <<< "git merge governed"'
run_case "a here-string from a shell variable fed to a shell asks" ask 'shell expansion' \
  $'S="git merge governed"; bash <<< "$S"'
run_case "a script piped into bash asks" ask 'standard input' 'echo "git merge governed" | bash'
run_case "a script piped into sh -s asks" ask 'standard input' "printf 'git merge governed' | sh -s"
run_case "a heredoc fed to another command and piped into a shell asks" ask 'standard input' \
  $'cat <<EOF | bash\ngit merge governed\nEOF'
run_case "bash /dev/stdin asks" ask 'standard input' 'echo "git merge governed" | bash /dev/stdin'
run_case "a process substitution handed to bash asks" ask 'standard input' 'bash <(echo "git merge governed")'
run_case "source of standard input asks" ask 'standard input' 'echo "git merge governed" | source /dev/stdin'
run_case "source of a process substitution asks" ask 'standard input' 'source <(echo "git merge governed")'
run_case "a heredoc behind env and a shell asks" ask "$ONLY_MISE|standard input" \
  $'env bash <<\'EOF\'\ngit merge governed\nEOF'
run_case "a heredoc in front of a shell word asks" ask "$ONLY_MISE|standard input" \
  $'<<\'EOF\' bash\ngit merge governed\nEOF'
run_case "a script from a shell variable handed to bash -c asks" ask 'shell expansion' \
  $'s="git merge governed"; bash -c "$s"'
run_case "a script from a shell variable handed to eval asks" ask 'shell expansion' \
  $'s="git merge governed"; eval "$s"'
run_case "a script from a command substitution handed to bash -c asks" ask 'shell expansion' \
  $'echo "git merge governed" | bash -c "$(cat)"'
run_case "a program named by a variable that holds a merge asks" ask 'named by a shell expansion' \
  $'CMD="git merge governed"; $CMD'
run_case "a script file run by a shell is not standard input" none '' \
  'bash scripts/governance/test-ask-governed-merge.sh'
run_case "command -v looks a name up and runs nothing" none '' \
  'command -v bash >/dev/null && git merge-base HEAD governed'
run_case "a shell whose script is a file next to merge-base passes silently" none '' \
  'bash -c "git merge-base HEAD governed"'

# --- options of a shell before its script -------------------------------------------------------------------------
# bash takes the words after -o and +o (and after -O and +O) as option names, so `-euo pipefail` is one cluster with a
# value, and --rcfile and --init-file take a file; the script of `-c` comes after all of them.

run_case "bash -euo pipefail -c asks" ask "$ONLY_MISE" "bash -euo pipefail -c 'git merge governed'"
run_case "bash -eo pipefail -c asks" ask "$ONLY_MISE" "bash -eo pipefail -c 'git merge governed'"
run_case "sh -eu -c asks" ask "$ONLY_MISE" "sh -eu -c 'git merge governed'"
run_case "bash --rcfile with a file before -c asks" ask "$ONLY_MISE" \
  "bash --rcfile /dev/null -c 'git merge governed'"
run_case "bash --init-file with a file before -c asks" ask "$ONLY_MISE" \
  "bash --init-file /dev/null -c 'git merge governed'"
run_case "bash -oo with two option names before -c asks" ask "$ONLY_MISE" \
  "bash -oo pipefail errexit -c 'git merge governed'"
run_case "bash +eo pipefail -c asks" ask "$ONLY_MISE" "bash +eo pipefail -c 'git merge governed'"
run_case "sudo bash -euo pipefail -c asks" ask "$ONLY_MISE" "sudo bash -euo pipefail -c 'git merge governed'"
run_case "env -S with bash -euo pipefail -c asks" ask "$ONLY_MISE" \
  $'env -S "bash -euo pipefail -c \'git merge governed\'"'
run_case "mise x -- bash -eo pipefail -c asks" ask "$ONLY_MISE" "mise x -- bash -eo pipefail -c 'git merge governed'"
run_case "a script behind strict-mode options is analysed, not blanket-asked" none '' \
  "bash -euo pipefail -c 'git merge docs-only'"
run_case "an unknown long option of a shell asks" ask 'unknown option' \
  "bash --unknown-option -c 'git merge docs-only'"
DYNAMIC_SHELL_WORD='the script or the options of bash come from a shell expansion'
run_case "an option word from a shell expansion asks and names the word" ask \
  "$DYNAMIC_SHELL_WORD: [\$]OPTS" "bash \$OPTS -c 'git merge docs-only'"
run_case "a script file from a shell variable asks and names the word" ask \
  "$DYNAMIC_SHELL_WORD: [\$]SCRIPT" "bash \$SCRIPT; git merge docs-only"
run_case "a script file from a quoted expansion asks, whatever follows it" ask \
  "$DYNAMIC_SHELL_WORD" "bash \"\$HOME/run.sh\" && git merge docs-only"
run_case "a literal script file next to a docs-only merge passes silently" none '' \
  'bash run.sh && git merge docs-only'

# --- wrappers that run another command ----------------------------------------------------------------------------

run_case "sudo in front of git asks" ask "$ONLY_MISE" 'sudo git merge governed'
run_case "sudo -u in front of git asks" ask "$ONLY_MISE" 'sudo -u root git merge governed'
run_case "doas in front of git asks" ask "$ONLY_MISE" 'doas git merge governed'
run_case "doas -u in front of git asks" ask "$ONLY_MISE" 'doas -u root git merge governed'
run_case "sudo bash -c asks" ask "$ONLY_MISE" "sudo bash -c 'git merge governed'"
run_case "sudo -s without a command reads standard input" ask 'standard input' \
  'echo "git merge governed" | sudo -s'
run_case "env -S asks" ask "$ONLY_MISE" 'env -S "git merge governed"'
run_case "env -S with an attached value asks" ask "$ONLY_MISE" "env -S'git merge governed'"
run_case "env --split-string asks" ask "$ONLY_MISE" "env --split-string='git merge governed'"
run_case "env -S with a value from a shell variable asks" ask 'shell expansion' \
  $'s="git merge governed"; env -S "$s"'
run_case "mise x -- in front of git asks" ask "$ONLY_MISE" 'mise x -- git merge governed'
run_case "stdbuf in front of git asks" ask "$ONLY_MISE" 'stdbuf -oL git merge governed'
run_case "caffeinate in front of git asks" ask "$ONLY_MISE" 'caffeinate git merge governed'
run_case "script in front of git asks" ask "$ONLY_MISE" 'script -q /dev/null git merge governed'
run_case "mise x -- bash -c asks" ask "$ONLY_MISE" "mise x -- bash -c 'git merge governed'"
run_case "a program with a git merge that changes no governed file passes silently" none '' \
  'mise x -- git merge docs-only'
run_case "git log --merges piped to head is not a merge" none '' 'git log --merges --oneline | head -5'
run_case "grep for a merge word is not a merge" none '' 'echo merge | grep merge'

# --- options of git and of the wrappers that change the repository or hide the command --------------------------------

run_case "git --attr-source takes a value before the subcommand" ask "$ONLY_MISE" 'git --attr-source HEAD merge governed'
run_case "git --attr-source before a merge without a governed change passes silently" none '' \
  'git --attr-source HEAD merge docs-only'
run_case "sudo --chdir= makes the repository unknown" ask 'cannot tell which repository' \
  "sudo --chdir=$TMP git merge docs-only"
run_case "sudo -D with an attached directory makes the repository unknown" ask 'cannot tell which repository' \
  "sudo -D$TMP git merge docs-only"
run_case "env --chdir= makes the repository unknown" ask 'cannot tell which repository' \
  "env --chdir=$TMP git merge docs-only"
run_case "env -C with an attached directory makes the repository unknown" ask 'cannot tell which repository' \
  "env -C$TMP git merge docs-only"
run_case "an env cluster that holds C makes the repository unknown" ask 'cannot tell which repository' \
  "env -iC $TMP git merge docs-only"
run_case "an env cluster that holds S with an attached script is analysed" ask "$ONLY_MISE" \
  "env -iS'git merge governed'"
run_case "a sudo cluster with a value letter is analysed" ask "$ONLY_MISE" 'sudo -iu root git merge governed'
run_case "GIT_DIR and GIT_WORK_TREE in front of git ask" ask 'cannot tell which repository' \
  "GIT_DIR=$TMP/x/.git GIT_WORK_TREE=$TMP/x git merge docs-only"
run_case "an exported GIT_DIR asks" ask 'cannot tell which repository' \
  "export GIT_DIR=$TMP/x/.git; git merge docs-only"
run_case "GIT_WORK_TREE after env asks" ask 'cannot tell which repository' \
  "env GIT_WORK_TREE=$TMP/x git merge docs-only"
run_case "another GIT_ variable in front of a merge passes silently" none '' 'GIT_EDITOR=true git merge docs-only'

# --- brace expansion is a shell expansion ---------------------------------------------------------------------------

run_case "a subcommand from a brace expansion asks" ask 'subcommand given by a shell expansion' 'git {merge,governed}'
run_case "a command from a brace expansion asks" ask 'named by a shell expansion' '{git,merge,governed}'
run_case "a ref from a brace expansion asks" ask 'holds a shell expansion' 'git merge {governed,docs-only}'
run_case "a brace expansion behind a wrapper program asks" ask 'shell expansion' 'mise x -- {git,merge,governed}'
run_case "a quoted brace expression is no expansion" none '' '"{git,merge,governed}"'
run_case "a group command is no brace expansion" ask "$ONLY_MISE" '{ git merge governed; }'
run_case "a brace expansion in an unrelated command next to a merge passes silently" none '' \
  'echo {a,b} && git merge docs-only'

# --- GSD cleanup-wave ---------------------------------------------------------------------------------------------

GSD='node .claude/gsd-core/bin/gsd-tools.cjs'
run_case "cleanup-wave of a branch with a governed change asks" ask 'governance/new\.json \(from worktree-agent-a\)' \
  "$GSD query worktree.cleanup-wave --manifest $GOVERNED_MANIFEST 2>&1 | tail -20"
run_case "cleanup-wave of a branch without a governed change passes silently" none '' \
  "$GSD query worktree.cleanup-wave --manifest $DOCS_MANIFEST"
run_case "the non-query form of cleanup-wave asks" ask 'governance/new\.json' \
  "$GSD worktree cleanup-wave --manifest $GOVERNED_MANIFEST"
run_case "quotes inside the cleanup-wave word ask" ask 'governance/new\.json' \
  "$GSD query worktree.cleanup''-wave --manifest $GOVERNED_MANIFEST"
run_case "a backslash before the n of cleanup-wave asks" ask 'governance/new\.json' \
  "$GSD query worktree.clea\\nup-wave --manifest $GOVERNED_MANIFEST"
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

# A command of about 150 KB that holds quotes and dollar signs but never mentions a merge must pass the early probe in
# linear time: bash pattern substitution on a text this long takes minutes in bash 3.2, and the hook's timeout blocks the
# call. The command goes through a file, because one argument may not exceed 128 KB on Linux.
BIG_COMMAND="$TMP/big-command.txt"
BIG_LINE="say \"hi\" it's \$HOME and \$(date) then some filler words to reach a realistic line length for a script"
{
  echo "cat >$TMP/out.txt <<'EOF'"
  for ((n = 0; n < 1600; n++)); do
    echo "$BIG_LINE"
  done
  echo EOF
} >"$BIG_COMMAND"
BIG_INPUT=$(jq -cn --rawfile c "$BIG_COMMAND" --arg d "$REPO" \
  '{session_id: "s", hook_event_name: "PreToolUse", tool_name: "Bash",
    tool_input: {command: $c, description: "test"}, cwd: $d}')
big_started=$SECONDS
run_raw "a command of 150 KB that never mentions a merge passes silently" none '' "$BIG_INPUT"
big_elapsed=$((SECONDS - big_started))
if [[ $big_elapsed -lt 10 ]]; then
  passed=$((passed + 1))
  echo "ok   - the 150 KB command was judged in $big_elapsed s, under the limit of 10 s"
else
  failed=$((failed + 1))
  echo "FAIL - the 150 KB command took $big_elapsed s, the limit is 10 s"
fi

NO_JQ="$TMP/no-jq"
mkdir -p "$NO_JQ"
for tool in cat git tr dirname sed; do
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

#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Self-contained tests for list-pr-files.sh. A fake `gh` placed first on PATH logs its arguments and
# replays a canned answer, so every case shows what the listing writes, and above all what it never
# writes, for each way the files API can go wrong. The chain cases run the real gate on the path the
# listing produced, which is how the workflow wires the two scripts together.
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
LIST="$SCRIPT_DIR/list-pr-files.sh"
CHECK="$SCRIPT_DIR/check-pr.sh"
GOVERNED="$SCRIPT_DIR/../../governance/governed-paths.txt"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/bin" "$TMP/fixtures"

# The fake prints the file named by FAKE_GH_STDOUT when it is set and exits with FAKE_GH_EXIT
# (default 0), after logging its arguments as one line.
cat >"$TMP/bin/gh" <<'FAKE'
#!/usr/bin/env bash
set -u
echo "$*" >>"$FAKE_GH_LOG"
if [[ -n ${FAKE_GH_STDOUT:-} ]]; then
  cat "$FAKE_GH_STDOUT"
fi
exit "${FAKE_GH_EXIT:-0}"
FAKE
chmod +x "$TMP/bin/gh"

passed=0
failed=0
cases=0
CASE_DIR=''
OUT=''
CODE=0
PROBLEMS=''

# fixture <name>: stores standard input as $TMP/fixtures/<name> and prints the path.
fixture() {
  cat >"$TMP/fixtures/$1"
  echo "$TMP/fixtures/$1"
}

# run_list [VAR=value | !VAR ...]: runs the real listing script in a clean environment and a fresh
# case directory. The defaults describe a valid call; a case overrides a variable by naming it again
# and removes one with a leading "!". Sets CASE_DIR, OUT (combined output) and CODE.
run_list() {
  cases=$((cases + 1))
  CASE_DIR="$TMP/case-$cases"
  mkdir -p "$CASE_DIR"
  PROBLEMS=''
  local -a assignments=(
    "PR_BASE_REPO=owner/repo"
    "PR_NUMBER=7"
    "PR_FILES_JSON=$CASE_DIR/pr-files.json"
    "FAKE_GH_LOG=$CASE_DIR/gh.log"
  )
  local arg name item
  local -a kept=()
  for arg in "$@"; do
    if [[ $arg == '!'* ]]; then
      name=${arg#!}
      kept=()
      for item in "${assignments[@]}"; do
        [[ $item == "$name="* ]] || kept+=("$item")
      done
      assignments=("${kept[@]}")
    else
      assignments+=("$arg")
    fi
  done
  CODE=0
  OUT=$(env -i PATH="$TMP/bin:$PATH" "${assignments[@]}" bash "$LIST" 2>&1) || CODE=$?
}

# run_gate <files json path> [VAR=value ...]: runs the real gate with the labels and branch of a
# plain maintainer pull request. Sets OUT and CODE.
run_gate() {
  local files=$1
  shift
  CODE=0
  OUT=$(env -i PATH="$PATH" GOVERNED_PATHS_FILE="$GOVERNED" PR_FILES_JSON="$files" \
    PR_LABELS_JSON='[]' PR_AUTHOR=maintainer PR_HEAD_REF=gsd/phase-01-repository-build-ci-foundation \
    PR_HEAD_REPO=owner/repo PR_BASE_REPO=owner/repo "$@" bash "$CHECK" 2>&1) || CODE=$?
}

problem() {
  PROBLEMS="$PROBLEMS $1;"
}

# expect_exit_and_output <expected exit code> <output regex>: checks the last run.
expect_exit_and_output() {
  [[ $CODE -eq $1 ]] || problem "exit $CODE, expected $1"
  grep -Eq -- "$2" <<<"$OUT" || problem "output lacks /$2/"
}

# expect_destination <absent | JSON>: the listing destination does not exist, or holds that JSON.
expect_destination() {
  local dest="$CASE_DIR/pr-files.json"
  if [[ $1 == absent ]]; then
    [[ ! -e $dest ]] || problem "destination exists"
  elif [[ ! -f $dest ]]; then
    problem "destination is missing"
  else
    jq -e --argjson want "$1" '. == $want' "$dest" >/dev/null 2>&1 || problem "destination differs"
  fi
}

# expect_call_count <n>: number of `gh` invocations of the last listing run.
expect_call_count() {
  local actual=0
  [[ ! -f $CASE_DIR/gh.log ]] || actual=$(wc -l <"$CASE_DIR/gh.log" | tr -d ' ')
  [[ $actual -eq $1 ]] || problem "gh called $actual times, expected $1"
}

# report <name>: prints the verdict of the case and counts it.
report() {
  if [[ -z $PROBLEMS ]]; then
    passed=$((passed + 1))
    echo "ok   - $1"
  else
    failed=$((failed + 1))
    echo "FAIL - $1 (exit $CODE;$PROBLEMS)"
    local line
    while IFS= read -r line; do
      echo "       | $line"
    done <<<"$OUT"
  fi
}

ONE_ENTRY='[{"filename": "docs/tooling.md", "status": "modified"}]'
page_one=$(fixture page-one <<<"$ONE_ENTRY")
workflow_page=$(
  fixture workflow-page <<'JSON'
[{"filename": ".github/workflows/ci.yml", "status": "modified"}]
JSON
)

# --- tracer: one path from the files API to the gate -------------------------------------------

# (a) a single page with a single entry.
run_list FAKE_GH_STDOUT="$page_one"
expect_exit_and_output 0 "listed 1 changed files"
expect_destination "$ONE_ENTRY"
report "a one-entry page is listed and written to the destination"

# (b) the CR-01 reproduction: gh succeeds and prints nothing.
run_list
expect_exit_and_output 1 "gh printed nothing"
expect_destination absent
report "empty gh output with a zero exit status fails the listing"

# (c) chain: the listed governed file reaches the gate, which names it.
run_list FAKE_GH_STDOUT="$workflow_page"
expect_exit_and_output 0 "listed 1 changed files"
list_dest="$CASE_DIR/pr-files.json"
run_gate "$list_dest"
expect_exit_and_output 1 "\.github/workflows/ci\.yml"
report "chain: a listed governed file without the label fails the gate"

# (d) chain: a failed listing leaves nothing for the gate to read, and the gate fails closed.
run_list
expect_exit_and_output 1 "gh printed nothing"
run_gate "$CASE_DIR/pr-files.json"
expect_exit_and_output 1 "PR_FILES_JSON does not name a readable file"
report "chain: the gate fails closed on the destination of a failed listing"

# (e) one call, with the paginated files endpoint.
run_list FAKE_GH_STDOUT="$page_one"
expect_call_count 1
grep -Fq -- "api --paginate repos/owner/repo/pulls/7/files?per_page=100" "$CASE_DIR/gh.log" ||
  problem "call is not the paginated files endpoint"
report "the listing makes one paginated call to the pull request files endpoint"

echo "$passed passed, $failed failed"
[[ $failed -eq 0 ]]

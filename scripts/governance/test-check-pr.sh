#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Self-contained tests for check-pr.sh: every case builds its inputs in a temporary directory, runs
# the real script in a clean environment and asserts the exit code and a key output line.
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
CHECK="$SCRIPT_DIR/check-pr.sh"
GOVERNED="$SCRIPT_DIR/../../governance/governed-paths.txt"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

passed=0
failed=0

# run_case <name> <expected exit code> <expected output regex> [VAR=value ...]
# Runs check-pr.sh with only the given variables set and checks exit code and combined output.
run_case() {
  local name=$1 expected_code=$2 expected_output=$3
  shift 3
  local output code=0
  output=$(env -i PATH="$PATH" GOVERNED_PATHS_FILE="$GOVERNED" "$@" bash "$CHECK" 2>&1) || code=$?
  if [[ $code -eq $expected_code ]] && grep -Eq -- "$expected_output" <<<"$output"; then
    passed=$((passed + 1))
    echo "ok   - $name"
  else
    failed=$((failed + 1))
    echo "FAIL - $name (exit $code, expected $expected_code; output pattern: $expected_output)"
    while IFS= read -r line; do
      echo "       | $line"
    done <<<"$output"
  fi
}

# files_json <name>: stores the JSON read from standard input as $TMP/<name>.json, prints its path.
files_json() {
  cat >"$TMP/$1.json"
  echo "$TMP/$1.json"
}

LABEL_NONE='[]'
LABEL_APPROVED='["governance-approved"]'

# --- label gate ---------------------------------------------------------------------------------

docs_only=$(
  files_json docs-only <<'JSON'
[{"filename": "docs/tooling.md", "status": "modified"}]
JSON
)
run_case "a change to a non-governed file passes" 0 "no governed file is touched" \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE"

workflow_change=$(
  files_json workflow-change <<'JSON'
[{"filename": ".github/workflows/ci.yml", "status": "modified"}]
JSON
)
run_case "a governed change without the label fails and names the file" 1 \
  "\.github/workflows/ci\.yml" \
  PR_FILES_JSON="$workflow_change" PR_LABELS_JSON="$LABEL_NONE"
run_case "a governed change with the label passes" 0 "carries 'governance-approved'" \
  PR_FILES_JSON="$workflow_change" PR_LABELS_JSON="$LABEL_APPROVED"
run_case "an unrelated label does not unlock a governed change" 1 "governance-approved" \
  PR_FILES_JSON="$workflow_change" PR_LABELS_JSON='["documentation"]'

rename_out=$(
  files_json rename-out <<'JSON'
[{"filename": "docs/x.json", "previous_filename": "governance/x.json", "status": "renamed"}]
JSON
)
run_case "a rename out of a governed path without the label fails" 1 "governance/x\.json" \
  PR_FILES_JSON="$rename_out" PR_LABELS_JSON="$LABEL_NONE"

empty_list=$(
  files_json empty <<'JSON'
[]
JSON
)
run_case "an empty files list passes" 0 "no governed file is touched" \
  PR_FILES_JSON="$empty_list" PR_LABELS_JSON="$LABEL_NONE"

run_case "a missing PR_FILES_JSON fails closed" 1 "PR_FILES_JSON" \
  PR_LABELS_JSON="$LABEL_NONE"
run_case "a PR_FILES_JSON naming a missing file fails closed" 1 "PR_FILES_JSON" \
  PR_FILES_JSON="$TMP/does-not-exist.json" PR_LABELS_JSON="$LABEL_NONE"

malformed=$(
  files_json malformed <<'JSON'
[{"filename": "docs/a.md"
JSON
)
run_case "malformed files JSON fails closed" 1 "malformed" \
  PR_FILES_JSON="$malformed" PR_LABELS_JSON="$LABEL_NONE"

not_an_array=$(
  files_json not-an-array <<'JSON'
{"message": "Not Found"}
JSON
)
run_case "a files JSON that is not an array fails closed" 1 "malformed" \
  PR_FILES_JSON="$not_an_array" PR_LABELS_JSON="$LABEL_NONE"

run_case "malformed label JSON fails closed" 1 "label list is malformed" \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON='{"not": "a list"}'

spaced=$(
  files_json spaced <<'JSON'
[{"filename": "docs/my notes.md", "status": "added"}]
JSON
)
run_case "a path with a space under docs passes" 0 "no governed file is touched" \
  PR_FILES_JSON="$spaced" PR_LABELS_JSON="$LABEL_NONE"

unicode=$(
  files_json unicode <<'JSON'
[{"filename": ".github/ünïcode.yml", "status": "added"}]
JSON
)
run_case "a non-ASCII file under .github without the label fails" 1 "ünïcode" \
  PR_FILES_JSON="$unicode" PR_LABELS_JSON="$LABEL_NONE"

echo "$passed passed, $failed failed"
[[ $failed -eq 0 ]]

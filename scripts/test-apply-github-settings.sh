#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Self-contained tests for apply-github-settings.sh. A fake `gh` placed first on PATH serves fixture
# JSON per endpoint from a temporary directory and logs `METHOD ENDPOINT` for every call, so the
# tests prove what the script reads, what it writes and, above all, what it never writes.
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
APPLY="$SCRIPT_DIR/apply-github-settings.sh"
RULESET="$SCRIPT_DIR/../.github/rulesets/main.json"
SETTINGS="$SCRIPT_DIR/../.github/repository-settings.json"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
LOG="$TMP/calls.log"
BODIES="$LOG.bodies"
mkdir -p "$TMP/bin"

# The fake serves, for `gh api METHOD ENDPOINT`, the file named after the endpoint with `/` turned
# into `__`: its content, an empty success (HTTP 204), `.404` or `.500` marker files for errors.
# Once any non-GET call was logged, reads come from the `after` directory when it exists, which
# lets an apply test show the state the writes would have produced.
cat >"$TMP/bin/gh" <<'FAKE'
#!/usr/bin/env bash
set -u
if [[ ${1:-} == repo ]]; then
  echo '{"nameWithOwner":"owner/repo"}'
  exit 0
fi
shift
method=GET endpoint='' input=''
while [[ $# -gt 0 ]]; do
  case $1 in
    -X | --method) method=$2; shift 2 ;;
    --input) input=$2; shift 2 ;;
    -*) shift ;;
    *) endpoint=$1; shift ;;
  esac
done
echo "$method $endpoint" >>"$FAKE_GH_LOG"
if [[ $input == - ]]; then
  { printf '%s %s ' "$method" "$endpoint"; cat; echo; } >>"$FAKE_GH_LOG.bodies"
fi
if [[ $method != GET ]]; then
  echo '{}'
  exit 0
fi
dir=$FAKE_GH_FIXTURES
if grep -qv '^GET ' "$FAKE_GH_LOG" && [[ -d $dir/after ]]; then
  dir=$dir/after
fi
name=${endpoint//\//__}
if [[ -f $dir/$name.404 ]]; then
  echo '{"message":"Not Found"}'
  echo "gh: Not Found (HTTP 404)" >&2
  exit 1
fi
if [[ -f $dir/$name.500 ]]; then
  echo "gh: Server Error (HTTP 500)" >&2
  exit 1
fi
if [[ -f $dir/$name ]]; then
  cat "$dir/$name"
  exit 0
fi
echo "gh: no fixture for $endpoint (HTTP 599)" >&2
exit 1
FAKE
chmod +x "$TMP/bin/gh"

passed=0
failed=0
OUT=''
CODE=0

# make_state <dir> [repo jq filter] [ruleset jq filter or "absent"] [alerts on|off|error]
#           [private reporting true|false] [automated fixes true|false]
# Writes fixtures for a repository that already matches the declared settings, then applies the
# filters to introduce differences. The live ruleset carries server-only fields, reversed lists and
# an extra parameter, so a match proves the normalisation and the projection.
make_state() {
  local dir=$1 repo_filter=${2:-.} ruleset_filter=${3:-.} alerts=${4:-on} pvr=${5:-true}
  local asf=${6:-false} base=owner__repo
  mkdir -p "$dir"
  jq '.repository + {id: 1, full_name: "owner/repo", security_and_analysis: {
        secret_scanning: {status: "enabled"},
        secret_scanning_push_protection: {status: "enabled"},
        dependabot_security_updates: {status: "disabled"}}}' "$SETTINGS" |
    jq "$repo_filter" >"$dir/repos__$base"
  if [[ $ruleset_filter == absent ]]; then
    echo '[]' >"$dir/repos__$base""__rulesets"
  else
    echo '[{"id": 7, "name": "other", "source_type": "Repository"},
          {"id": 42, "name": "main", "source_type": "Repository", "enforcement": "active"}]' \
      >"$dir/repos__$base""__rulesets"
    jq '. + {id: 42, node_id: "RRS_x", source_type: "Repository", source: "owner/repo",
             created_at: "2026-01-01T00:00:00Z", updated_at: "2026-01-02T00:00:00Z",
             _links: {}, current_user_can_bypass: "always"}
        | .rules |= (reverse | map(if .type == "pull_request"
                                   then .parameters += {required_reviewers: []} else . end))' \
      "$RULESET" | jq "$ruleset_filter" >"$dir/repos__$base""__rulesets__42"
  fi
  case $alerts in
  on) : >"$dir/repos__$base""__vulnerability-alerts" ;;
  off) : >"$dir/repos__$base""__vulnerability-alerts.404" ;;
  error) : >"$dir/repos__$base""__vulnerability-alerts.500" ;;
  *)
    echo "bad alerts state: $alerts" >&2
    exit 2
    ;;
  esac
  echo "{\"enabled\": $pvr}" >"$dir/repos__$base""__private-vulnerability-reporting"
  echo "{\"enabled\": $asf, \"paused\": false}" >"$dir/repos__$base""__automated-security-fixes"
}

# run_apply <fixture dir> args...: runs the script against the fake gh; sets OUT and CODE.
run_apply() {
  local dir=$1
  shift
  : >"$LOG"
  : >"$BODIES"
  CODE=0
  OUT=$(env -u GITHUB_REPOSITORY PATH="$TMP/bin:$PATH" FAKE_GH_FIXTURES="$dir" FAKE_GH_LOG="$LOG" \
    bash "$APPLY" "$@" 2>&1) || CODE=$?
}

pass() {
  passed=$((passed + 1))
  echo "ok   - $1"
}

fail() {
  failed=$((failed + 1))
  echo "FAIL - $1"
  if [[ -n ${2:-} ]]; then echo "       $2"; fi
  local line
  while IFS= read -r line; do
    echo "       | $line"
  done <<<"$OUT"
  echo "       calls:"
  while IFS= read -r line; do
    echo "       | $line"
  done <"$LOG"
}

# assert_code <name> <expected exit code>
assert_code() {
  if [[ $CODE -eq $2 ]]; then pass "$1: exit $2"; else fail "$1: exit $CODE, expected $2"; fi
}

# assert_out <name> <regex that the output must match>
assert_out() {
  if grep -Eq -- "$2" <<<"$OUT"; then pass "$1: output matches $2"; else fail "$1: output lacks $2"; fi
}

# assert_no_out <name> <regex that the output must not match>
assert_no_out() {
  if grep -Eq -- "$2" <<<"$OUT"; then fail "$1: output unexpectedly matches $2"; else pass "$1: output lacks $2"; fi
}

# assert_writes <name> <expected non-GET calls, newline separated, in order; empty for none>
assert_writes() {
  local actual
  actual=$(grep -v '^GET ' "$LOG" || true)
  if [[ $actual == "$2" ]]; then
    pass "$1: writes are exactly [${2//$'\n'/; }]"
  else
    fail "$1: writes [${actual//$'\n'/; }], expected [${2//$'\n'/; }]"
  fi
}

# assert_called <name> <exact logged call>
assert_called() {
  if grep -qxF -- "$2" "$LOG"; then pass "$1: called $2"; else fail "$1: not called: $2"; fi
}

# assert_not_called <name> <exact logged call>
assert_not_called() {
  if grep -qxF -- "$2" "$LOG"; then fail "$1: unexpectedly called $2"; else pass "$1: never called $2"; fi
}

# assert_body <name> <jq expression that must be true for the last logged JSON body of a call>
# The expression runs over the body of the first call whose logged line starts with the prefix.
assert_body() {
  local prefix=$2 expr=$3 body
  body=$(grep -F -- "$prefix " "$BODIES" | head -n 1 | sed "s|^$prefix ||" || true)
  if [[ -n $body ]] && jq -e "$expr" >/dev/null 2>&1 <<<"$body"; then
    pass "$1: body of $prefix satisfies $expr"
  else
    fail "$1: body [$body] of $prefix does not satisfy $expr"
  fi
}

R=repos/owner/repo
n=0
fresh() {
  n=$((n + 1))
  FX="$TMP/fx/$n"
  mkdir -p "$FX"
}

# --- idempotency and read-only modes -------------------------------------------------------------

fresh
make_state "$FX"
run_apply "$FX" --check --repo owner/repo
assert_code "matching state, --check" 0
assert_out "matching state, --check" "no drift"
assert_writes "matching state, --check" ""

fresh
make_state "$FX"
run_apply "$FX" --apply --repo owner/repo
assert_code "matching state, --apply" 0
assert_out "matching state, --apply" "nothing to apply"
assert_out "matching state, --apply" "RepositoryRole 5 mode=pull_request"
assert_writes "matching state, --apply is idempotent" ""

fresh
make_state "$FX"
run_apply "$FX" --repo owner/repo
assert_code "matching state, default mode" 0
assert_out "matching state, default mode" "mode: dry-run"
assert_out "matching state, default mode" "0 delta\(s\); nothing written"

# --- the live starting point of this repository: nothing declared yet ----------------------------

old_repo='. + {allow_merge_commit: true, allow_rebase_merge: true, delete_branch_on_merge: false,
  allow_auto_merge: false, allow_update_branch: false,
  squash_merge_commit_title: "COMMIT_OR_PR_TITLE", squash_merge_commit_message: "COMMIT_MESSAGES"}'

fresh
make_state "$FX" "$old_repo" absent off false false
run_apply "$FX" --dry-run --repo owner/repo
assert_code "starting point, --dry-run" 0
assert_out "starting point, --dry-run" "ruleset main: create"
assert_out "starting point, --dry-run" "repository.allow_merge_commit: true -> false"
assert_out "starting point, --dry-run" "repository.allow_rebase_merge: true -> false"
assert_out "starting point, --dry-run" "repository.squash_merge_commit_title: COMMIT_OR_PR_TITLE -> PR_TITLE"
assert_out "starting point, --dry-run" "repository.squash_merge_commit_message: COMMIT_MESSAGES -> PR_BODY"
assert_out "starting point, --dry-run" "repository.delete_branch_on_merge: false -> true"
assert_out "starting point, --dry-run" "repository.allow_auto_merge: false -> true"
assert_out "starting point, --dry-run" "repository.allow_update_branch: false -> true"
assert_out "starting point, --dry-run" "security.vulnerability_alerts: false -> true"
assert_out "starting point, --dry-run" "security.private_vulnerability_reporting: false -> true"
assert_no_out "starting point, --dry-run" "security.automated_security_fixes"
assert_no_out "starting point, --dry-run" "security.secret_scanning"
assert_writes "starting point, --dry-run" ""

run_apply "$FX" --check --repo owner/repo
assert_code "starting point, --check" 1
assert_out "starting point, --check" "ruleset main: create"
assert_writes "starting point, --check" ""

fresh
make_state "$FX" "$old_repo" absent off false false
make_state "$FX/after"
run_apply "$FX" --apply --repo owner/repo
assert_code "starting point, --apply" 0
assert_called "starting point, --apply" "PATCH $R"
assert_called "starting point, --apply" "POST $R/rulesets"
assert_called "starting point, --apply" "PUT $R/vulnerability-alerts"
assert_called "starting point, --apply" "PUT $R/private-vulnerability-reporting"
assert_not_called "starting point, --apply" "DELETE $R/automated-security-fixes"
assert_not_called "starting point, --apply" "PUT $R/rulesets/42"
assert_out "starting point, --apply" "applied; the live settings match"
assert_out "starting point, --apply" "RepositoryRole 5 mode=pull_request"
assert_body "starting point, --apply patches only differences" "PATCH $R" \
  '(keys | sort) == ["allow_auto_merge","allow_merge_commit","allow_rebase_merge","allow_update_branch","delete_branch_on_merge","squash_merge_commit_message","squash_merge_commit_title"]
   and .allow_merge_commit == false and .squash_merge_commit_title == "PR_TITLE"'

# --- scoped runs ---------------------------------------------------------------------------------

fresh
make_state "$FX" "$old_repo" absent off false false
make_state "$FX/after" . absent off false false
run_apply "$FX" --apply --only repository --repo owner/repo
assert_code "--apply --only repository" 0
assert_writes "--apply --only repository" "PATCH $R"
assert_not_called "--apply --only repository" "GET $R/rulesets"
assert_not_called "--apply --only repository" "GET $R/vulnerability-alerts"
assert_no_out "--apply --only repository" "ruleset main"

fresh
make_state "$FX" "$old_repo" absent off false false
run_apply "$FX" --check --only ruleset --only security --repo owner/repo
assert_code "--check with two --only sections" 1
assert_out "--check with two --only sections" "ruleset main: create"
assert_out "--check with two --only sections" "security.vulnerability_alerts"
assert_no_out "--check with two --only sections" "repository\."

fresh
make_state "$FX" "$old_repo"
run_apply "$FX" --check --only ruleset --repo owner/repo
assert_code "--check --only ruleset ignores repository drift" 0
assert_out "--check --only ruleset ignores repository drift" "no drift"

fresh
make_state "$FX" "$old_repo" absent off false false
run_apply "$FX" --check --only repository --repo owner/repo
assert_code "--check --only repository ignores ruleset and security drift" 1
assert_no_out "--check --only repository ignores ruleset and security drift" "ruleset main|security\."

# --- ruleset comparison --------------------------------------------------------------------------

fresh
make_state "$FX" . '.enforcement = "disabled"'
make_state "$FX/after"
run_apply "$FX" --apply --repo owner/repo
assert_code "ruleset enforcement drift, --apply" 0
assert_writes "ruleset enforcement drift, --apply" "PUT $R/rulesets/42"

fresh
make_state "$FX" . '.rules |= map(if .type == "required_status_checks"
  then .parameters.required_status_checks |= map(select(.context != "governance-success")) else . end)'
run_apply "$FX" --check --repo owner/repo
assert_code "a dropped required context is drift" 1
assert_out "a dropped required context is drift" "ruleset main: update"
assert_out "a dropped required context is drift" "governance-success"

fresh
make_state "$FX" . '.rules |= map(if .type == "required_status_checks"
  then .parameters.required_status_checks |= map(.integration_id = 1) else . end)'
run_apply "$FX" --check --repo owner/repo
assert_code "required contexts bound to another app are drift" 1
assert_out "required contexts bound to another app are drift" "ruleset main: update"

fresh
make_state "$FX" . '.rules |= map(if .type == "pull_request"
  then .parameters.allowed_merge_methods = ["squash", "merge"] else . end)'
run_apply "$FX" --check --repo owner/repo
assert_code "an extra merge method is drift" 1

fresh
make_state "$FX" . '.bypass_actors = []'
run_apply "$FX" --check --repo owner/repo
assert_code "a missing bypass actor is drift" 1

fresh
make_state "$FX" . '.rules |= map(select(.type != "non_fast_forward"))'
run_apply "$FX" --check --repo owner/repo
assert_code "a missing rule is drift" 1

fresh
make_state "$FX" . '.rules |= (. + [{"type": "creation"}])'
run_apply "$FX" --check --repo owner/repo
assert_code "an extra rule is drift" 1

fresh
make_state "$FX"
echo '[{"id": 9, "name": "main", "source_type": "Organization"}]' >"$FX/repos__owner__repo__rulesets"
run_apply "$FX" --check --only ruleset --repo owner/repo
assert_code "an organisation ruleset of the same name does not count" 1
assert_out "an organisation ruleset of the same name does not count" "ruleset main: create"

# --- security toggles and secret scanning ---------------------------------------------------------

fresh
make_state "$FX" . . on true true
make_state "$FX/after"
run_apply "$FX" --apply --repo owner/repo
assert_code "automated security fixes enabled, --apply" 0
assert_writes "automated security fixes enabled, --apply" "DELETE $R/automated-security-fixes"

fresh
make_state "$FX" '.security_and_analysis.secret_scanning_push_protection.status = "disabled"'
make_state "$FX/after"
run_apply "$FX" --apply --repo owner/repo
assert_code "push protection off, --apply" 0
assert_out "push protection off, --apply" "security.secret_scanning_push_protection: disabled -> enabled"
assert_writes "push protection off, --apply" "PATCH $R"
assert_body "push protection off, --apply" "PATCH $R" \
  '.security_and_analysis.secret_scanning_push_protection.status == "enabled"
   and (.security_and_analysis | has("secret_scanning") | not)'

fresh
make_state "$FX" . . error
run_apply "$FX" --check --repo owner/repo
assert_code "a server error on the alerts probe is not read as disabled" 1
assert_out "a server error on the alerts probe is not read as disabled" "HTTP 500"
assert_no_out "a server error on the alerts probe is not read as disabled" "no drift|vulnerability_alerts: false"

# --- failures never read as an empty state -------------------------------------------------------

fresh
make_state "$FX"
rm "$FX/repos__owner__repo__rulesets"
run_apply "$FX" --check --repo owner/repo
assert_code "a failed ruleset list is an error, not an absent ruleset" 1
assert_no_out "a failed ruleset list is an error, not an absent ruleset" "ruleset main: create"
assert_writes "a failed ruleset list is an error, not an absent ruleset" ""

fresh
make_state "$FX" "$old_repo" absent off false false
run_apply "$FX" --apply --repo owner/repo
assert_code "--apply fails when a delta remains after the writes" 1
assert_out "--apply fails when a delta remains after the writes" "deltas remain after apply"

# --- usage ---------------------------------------------------------------------------------------

fresh
make_state "$FX"
run_apply "$FX" --frobnicate
assert_code "unknown flag" 2
assert_out "unknown flag" "unknown argument: --frobnicate"
assert_writes "unknown flag" ""

run_apply "$FX" --only cluster
assert_code "unknown --only value" 2
assert_out "unknown --only value" "unknown section for --only: cluster"

run_apply "$FX" --only
assert_code "--only without a value" 2

run_apply "$FX" --apply --check
assert_code "two modes" 2
assert_out "two modes" "only one of"

run_apply "$FX" --repo "owner/repo; rm -rf /"
assert_code "malformed --repo" 2

run_apply "$FX" --help
assert_code "--help" 0
assert_out "--help" "Usage: apply-github-settings.sh"

echo
echo "$passed passed, $failed failed"
[[ $failed -eq 0 ]]

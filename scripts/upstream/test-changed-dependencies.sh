#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Self-contained tests for changed-dependencies.sh: every case copies a baseline git repository into a
# temporary directory, changes and commits some files, runs the real script from inside that repository
# and asserts its exit code, its summary, and the workflow commands on standard output.
set -euo pipefail

# Git behaves the same on every machine: no global or system configuration, hooks or signing.
export GIT_CONFIG_NOSYSTEM=1 GIT_CONFIG_GLOBAL=/dev/null

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
SCRIPT="$SCRIPT_DIR/changed-dependencies.sh"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
BASELINE="$TMP/baseline"

passed=0
failed=0

# --- assertion helpers --------------------------------------------------------------------------

# expect <name> <command...>: passes when the command succeeds; a failure prints the last run's output.
expect() {
  local name=$1
  shift
  if "$@"; then
    passed=$((passed + 1))
    echo "ok   - $name"
  else
    failed=$((failed + 1))
    echo "FAIL - $name (exit $CODE)"
    while IFS= read -r line; do
      echo "       | $line"
    done <<<"stdout and stderr:"$'\n'"$OUT"$'\n'"summary:"$'\n'"$SUMMARY"
  fi
}

contains() { grep -Fq -- "$2" <<<"$1"; }
absent() { ! grep -Fq -- "$2" <<<"$1"; }
matches() { grep -Eq -- "$2" <<<"$1"; }
no_line_matching() { ! grep -Eq -- "$2" <<<"$1"; }
exit_code_is() { [[ $CODE -eq $1 ]]; }
# line_count_is <text> <regex> <count>: exactly <count> lines of the text match the regex.
line_count_is() { [[ $(grep -Ec -- "$2" <<<"$1") -eq $3 ]]; }

# A backtick, kept in a variable so that Markdown code spans can sit inside double-quoted strings.
BT='`'

# --- fixture ------------------------------------------------------------------------------------

SHA_A=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
SHA_B=bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
SHA_C=cccccccccccccccccccccccccccccccccccccccc

# write_record <dir> <number> <product> <status> <title> <removal criteria>
write_record() {
  local dir=$1 number=$2 product=$3 status=$4 title=$5 criteria=$6
  cat >"$dir/docs/upstream/$number-record.md" <<EOF
---
id: "UP-$number"
product: "$product"
product-version: "1.0.0"
status: "$status"
workaround: true
upstream: []
reported: null
updated: "2026-11-23"
---

# UP-$number: $title

## Symptom

Something fails.

## Removal criteria

$criteria

## How to verify removal

Run the check.
EOF
}

git_in() {
  local dir=$1
  shift
  git -C "$dir" "$@"
}

commit_all() {
  git_in "$1" add -A
  git_in "$1" commit -q -m "$2"
}

# replace_in <file> <old> <new>: replaces every occurrence of a literal string. The pattern is quoted to
# stay literal; the replacement stays unquoted because bash 3.2 (macOS) would keep quotes written there.
replace_in() {
  local content new=$3
  content=$(<"$1")
  printf '%s\n' "${content//"$2"/$new}" >"$1"
}

build_baseline() {
  local dir=$BASELINE
  mkdir -p "$dir/gradle/wrapper" "$dir/.github/workflows" "$dir/docs/upstream"
  git_in "$dir" init -q -b main
  git_in "$dir" config user.name test
  git_in "$dir" config user.email test@example.com
  git_in "$dir" config commit.gpgsign false

  cat >"$dir/gradle/libs.versions.toml" <<'TOML'
[versions]
kotlin = "2.4.21"
# Shared by the Axon Framework and Axoniq Framework BOMs.
axon = "5.4.2"

[libraries]
axon-bom = { module = "org.axonframework:axon-framework-bom", version.ref = "axon" }
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version = "2.4.21" }
TOML

  cat >"$dir/mise.toml" <<'TOML'
min_version = "2026.11.11"

[settings]
locked = true

[tools]
jq = "1.8.2" # trailing comment
"npm:renovate" = { version = "44.252.8", trust_policy_excludes = ["@yarnpkg/libzip@3.2.2"] }
"aqua:mvdan/sh" = "3.14.1"
TOML

  printf 'distributionUrl=https\\://services.gradle.org/distributions/gradle-9.9.2-bin.zip\n' \
    >"$dir/gradle/wrapper/gradle-wrapper.properties"
  printf '25\n' >"$dir/.java-version"
  printf '24\n' >"$dir/.nvmrc"
  printf '# readme\n' >"$dir/README.md"

  cat >"$dir/.github/workflows/ci.yml" <<YAML
name: CI
on: push
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@$SHA_A # v7.0.1
        with:
          persist-credentials: false
      - uses: gradle/actions/setup-gradle@$SHA_A # v6.4.1
      - uses: ./.github/actions/local
      - run: echo "uses: example/not-an-action@$SHA_A"
YAML

  write_record "$dir" 0001 axon open "Axon defect" \
    $'Upgrade to an Axon release that contains the fix.\nThen delete the decorator.'
  write_record "$dir" 0002 "npm:renovate" reported "Renovate defect" "A Renovate release without the libzip pin."
  write_record "$dir" 0003 axon workaround-removed "Axon defect, already removed" "Met long ago."
  write_record "$dir" 0004 "action:actions/checkout" open "Checkout defect" "A checkout release with the fix."
  write_record "$dir" 0005 gradle open "Gradle defect" "A Gradle release with the fix."
  write_record "$dir" 0006 java wont-fix "Java defect" "Never."
  write_record "$dir" 0007 node fixed-upstream "Node defect" "Drop the flag."
  commit_all "$dir" baseline
}

# new_case: a fresh copy of the baseline in CASE_DIR, with BASE_SHA set to its commit.
new_case() {
  CASE_DIR=$(mktemp -d "$TMP/case.XXXXXX")
  cp -R "$BASELINE/." "$CASE_DIR"
  BASE_SHA=$(git_in "$CASE_DIR" rev-parse HEAD)
}

# run_advisory <args...>: runs the script in CASE_DIR with a summary file; sets CODE, OUT and SUMMARY.
run_advisory() {
  local summary_file="$CASE_DIR.summary"
  : >"$summary_file"
  CODE=0
  OUT=$(cd "$CASE_DIR" && env -i PATH="$PATH" HOME="$TMP" GITHUB_STEP_SUMMARY="$summary_file" \
    bash "$SCRIPT" "$@" 2>&1) || CODE=$?
  SUMMARY=$(<"$summary_file")
}

# run_advisory_stdout <args...>: the same without a summary file, so the report goes to standard output.
run_advisory_stdout() {
  CODE=0
  SUMMARY=''
  OUT=$(cd "$CASE_DIR" && env -i PATH="$PATH" HOME="$TMP" bash "$SCRIPT" "$@" 2>&1) || CODE=$?
}

build_baseline

# --- version catalog ----------------------------------------------------------------------------

new_case
replace_in "$CASE_DIR/gradle/libs.versions.toml" 'axon = "5.4.2"' 'axon = "5.5.0"'
commit_all "$CASE_DIR" "bump axon"
run_advisory "$BASE_SHA" HEAD
expect "an axon bump exits 0" exit_code_is 0
expect "an axon bump names the open axon record" contains "$SUMMARY" "### UP-0001: Axon defect"
expect "an axon bump prints the removal criteria" contains "$SUMMARY" "> Upgrade to an Axon release that contains the fix."
expect "an axon bump prints every line of the removal criteria" contains "$SUMMARY" "> Then delete the decorator."
expect "an axon bump prints the status" contains "$SUMMARY" "- Status: ${BT}open${BT}"
expect "an axon bump names the changed key" contains "$SUMMARY" "Changed keys: ${BT}axon${BT}"
expect "an axon bump skips the workaround-removed axon record" absent "$SUMMARY" "UP-0003"
expect "an axon bump skips records of other products" absent "$SUMMARY" "UP-0002"
expect "an axon bump emits a warning annotation" matches "$OUT" '^::warning title=Upstream record UP-0001::Axon defect'
expect "the warning points at the record file" contains "$OUT" "docs/upstream/0001-record.md"

new_case
replace_in "$CASE_DIR/gradle/libs.versions.toml" 'kotlin-stdlib" version = "2.4.21"' 'kotlin-stdlib" version = "2.5.0"'
replace_in "$CASE_DIR/gradle/libs.versions.toml" 'version.ref = "axon" }' 'version.ref = "axon" } # moved'
commit_all "$CASE_DIR" "change libraries only"
run_advisory "$BASE_SHA" HEAD
expect "a change outside [versions] matches nothing" contains "$SUMMARY" "No upstream records affected."
expect "a change outside [versions] emits no warning" absent "$OUT" "::warning"

new_case
replace_in "$CASE_DIR/gradle/libs.versions.toml" 'axon = "5.4.2"' 'axon = "5.4.2" # pinned'
commit_all "$CASE_DIR" "comment only"
run_advisory "$BASE_SHA" HEAD
expect "a trailing comment change is not a version change" contains "$SUMMARY" "No upstream records affected."

new_case
content=$(grep -v '^axon = "5.4.2"$' "$CASE_DIR/gradle/libs.versions.toml")
printf '%s\n' "$content" >"$CASE_DIR/gradle/libs.versions.toml"
commit_all "$CASE_DIR" "remove the axon key"
run_advisory "$BASE_SHA" HEAD
expect "a removed version key counts as a change" contains "$SUMMARY" "### UP-0001: Axon defect"

# --- mise ---------------------------------------------------------------------------------------

new_case
replace_in "$CASE_DIR/mise.toml" '44.252.8' '44.260.0'
commit_all "$CASE_DIR" "bump renovate"
run_advisory "$BASE_SHA" HEAD
expect "a quoted mise key bump exits 0" exit_code_is 0
expect "a quoted mise key bump names the renovate record" contains "$SUMMARY" "### UP-0002: Renovate defect"
expect "a quoted mise key bump names the unquoted key" contains "$SUMMARY" "Changed keys: ${BT}npm:renovate${BT}"
expect "a reported record is matched" contains "$SUMMARY" "- Status: ${BT}reported${BT}"
expect "a mise bump skips axon records" absent "$SUMMARY" "UP-0001"

new_case
replace_in "$CASE_DIR/mise.toml" 'jq = "1.8.2" # trailing comment' 'jq = "1.8.2" # another comment'
replace_in "$CASE_DIR/mise.toml" 'locked = true' 'locked = false'
commit_all "$CASE_DIR" "comment and settings"
run_advisory "$BASE_SHA" HEAD
expect "a comment or [settings] change matches nothing" contains "$SUMMARY" "No upstream records affected."

# --- single-value files -------------------------------------------------------------------------

new_case
replace_in "$CASE_DIR/gradle/wrapper/gradle-wrapper.properties" '9.9.2' '9.9.3'
commit_all "$CASE_DIR" "bump the wrapper"
run_advisory "$BASE_SHA" HEAD
expect "a wrapper bump names the gradle record" contains "$SUMMARY" "### UP-0005: Gradle defect"
expect "a wrapper bump names no unrelated record" absent "$SUMMARY" "UP-0001"
expect "a wrapper bump names no other product" absent "$SUMMARY" "UP-0004"

new_case
printf '26\n' >"$CASE_DIR/.java-version"
commit_all "$CASE_DIR" "bump java"
run_advisory "$BASE_SHA" HEAD
expect "a wont-fix record never produces a prompt" contains "$SUMMARY" "No upstream records affected."
expect "the java key is still reported as changed" contains "$SUMMARY" "Changed keys: ${BT}java${BT}"

new_case
printf '26\n' >"$CASE_DIR/.nvmrc"
commit_all "$CASE_DIR" "bump node"
run_advisory "$BASE_SHA" HEAD
expect "a fixed-upstream record is matched through .nvmrc" contains "$SUMMARY" "### UP-0007: Node defect"

# --- workflow actions ---------------------------------------------------------------------------

new_case
replace_in "$CASE_DIR/.github/workflows/ci.yml" "actions/checkout@$SHA_A # v7.0.1" "actions/checkout@$SHA_B # v7.0.2"
commit_all "$CASE_DIR" "bump checkout"
run_advisory "$BASE_SHA" HEAD
expect "an action SHA change matches an action: product" contains "$SUMMARY" "### UP-0004: Checkout defect"
expect "an action SHA change names the action key" contains "$SUMMARY" "Changed keys: ${BT}action:actions/checkout${BT}"
expect "an action SHA change matches only that action" absent "$SUMMARY" "UP-0005"

new_case
replace_in "$CASE_DIR/.github/workflows/ci.yml" "gradle/actions/setup-gradle@$SHA_A" "gradle/actions/setup-gradle@$SHA_C"
commit_all "$CASE_DIR" "bump setup-gradle"
run_advisory "$BASE_SHA" HEAD
expect "an action without a record matches nothing" contains "$SUMMARY" "No upstream records affected."
expect "the sub-path action is keyed by owner and repository" contains "$SUMMARY" "Changed keys: ${BT}action:gradle/actions${BT}"

new_case
replace_in "$CASE_DIR/.github/workflows/ci.yml" "example/not-an-action@$SHA_A" "example/not-an-action@$SHA_B"
replace_in "$CASE_DIR/.github/workflows/ci.yml" "./.github/actions/local" "./.github/actions/other"
commit_all "$CASE_DIR" "change text that is no remote action"
run_advisory "$BASE_SHA" HEAD
expect "run text and local actions are not actions" contains "$SUMMARY" "No dependency version changed."

# --- nothing relevant ---------------------------------------------------------------------------

new_case
printf '# readme\n\nmore\n' >"$CASE_DIR/README.md"
commit_all "$CASE_DIR" "docs"
run_advisory "$BASE_SHA" HEAD
expect "no relevant change reports no affected records" contains "$SUMMARY" "No upstream records affected."
expect "no relevant change says no version changed" contains "$SUMMARY" "No dependency version changed."
expect "no relevant change exits 0" exit_code_is 0
expect "no relevant change emits no annotation" absent "$OUT" "::"

new_case
run_advisory "$BASE_SHA"
expect "the head revision defaults to HEAD" contains "$SUMMARY" "No upstream records affected."

new_case
git_in "$CASE_DIR" checkout -q -b feature
replace_in "$CASE_DIR/gradle/libs.versions.toml" 'axon = "5.4.2"' 'axon = "5.5.0"'
commit_all "$CASE_DIR" "bump axon on a branch"
git_in "$CASE_DIR" checkout -q main
run_advisory "$BASE_SHA" feature
expect "a named head revision is compared instead of the checkout" contains "$SUMMARY" "### UP-0001: Axon defect"

# --- several changes at once --------------------------------------------------------------------

new_case
replace_in "$CASE_DIR/gradle/libs.versions.toml" 'axon = "5.4.2"' 'axon = "5.5.0"'
replace_in "$CASE_DIR/mise.toml" '44.252.8' '44.260.0'
printf '26\n' >"$CASE_DIR/.nvmrc"
commit_all "$CASE_DIR" "several bumps"
run_advisory "$BASE_SHA" HEAD
expect "several changes name each matching record once" line_count_is "$SUMMARY" '^### UP-' 3
expect "several changes emit one warning per record" line_count_is "$OUT" '^::warning ' 3

# --- workflow-command safety (T-02-42) ----------------------------------------------------------

new_case
write_record "$CASE_DIR" 0008 injectme open 'boom %0A::error::injected' \
  $'First line.\n::error::from the removal criteria\n::set-output name=x::y\n%0A::error::again'
commit_all "$CASE_DIR" "add the injection record"
BASE_SHA=$(git_in "$CASE_DIR" rev-parse HEAD)
replace_in "$CASE_DIR/gradle/libs.versions.toml" 'axon = "5.4.2"' 'axon = "5.4.2"
injectme = "2"'
commit_all "$CASE_DIR" "add injectme"
run_advisory "$BASE_SHA" HEAD
expect "an added version key matches its record" contains "$SUMMARY" "### UP-0008: boom %0A::error::injected"
expect "a title with %0A produces no ::error:: line" no_line_matching "$OUT" '^::error::'
expect "the percent sign of the title is escaped" contains "$OUT" 'boom %250A::error::injected'
expect "the job summary has no line that is a workflow command" no_line_matching "$SUMMARY" '^::'
expect "removal criteria are quoted, never a bare command" contains "$SUMMARY" "> ::error::from the removal criteria"
run_advisory_stdout "$BASE_SHA" HEAD
expect "standard output without a summary file has no ::error:: line" no_line_matching "$OUT" '^::error::'
expect "standard output without a summary file has no ::set-output line" no_line_matching "$OUT" '^::set-output'
expect "the only command line on standard output is the warning" line_count_is "$OUT" '^::' 1

new_case
write_record "$CASE_DIR" 0009 escapeme open "Plain title" "Criteria."
replace_in "$CASE_DIR/docs/upstream/0009-record.md" 'id: "UP-0009"' 'id: "UP-0009,x:y"'
replace_in "$CASE_DIR/docs/upstream/0009-record.md" 'product: "escapeme"' 'product: "axon"'
commit_all "$CASE_DIR" "record with awkward id"
BASE_SHA=$(git_in "$CASE_DIR" rev-parse HEAD)
replace_in "$CASE_DIR/gradle/libs.versions.toml" 'axon = "5.4.2"' 'axon = "5.5.0"'
commit_all "$CASE_DIR" "bump axon"
run_advisory "$BASE_SHA" HEAD
expect "a comma and a colon in the id are escaped in the title property" contains "$OUT" 'title=Upstream record UP-0009%2Cx%3Ay::'

# --- no usable base -----------------------------------------------------------------------------

new_case
run_advisory 0000000000000000000000000000000000000000 HEAD
expect "an all-zero base is skipped with exit 0" exit_code_is 0
expect "an all-zero base says so" contains "$SUMMARY" "No base commit; upstream advisory skipped."

new_case
run_advisory "" HEAD
expect "an empty base is skipped with exit 0" exit_code_is 0
expect "an empty base says so" contains "$SUMMARY" "No base commit; upstream advisory skipped."

new_case
run_advisory 1234567890123456789012345678901234567890 HEAD
expect "a base missing from the checkout is skipped with exit 0" exit_code_is 0
expect "a base missing from the checkout says so" contains "$SUMMARY" "upstream advisory skipped"

# --- usage and parsing errors -------------------------------------------------------------------

new_case
run_advisory
expect "no argument is a usage error" exit_code_is 2
expect "the usage message names the arguments" contains "$OUT" "usage: changed-dependencies.sh <base-sha>"

new_case
run_advisory "--help"
expect "an option-like base is a usage error" exit_code_is 2

new_case
run_advisory "$BASE_SHA" no-such-ref
expect "an unresolvable head revision is an error" exit_code_is 2
expect "the error names the head revision" contains "$OUT" "no-such-ref"

new_case
printf 'no front matter here\n' >"$CASE_DIR/docs/upstream/0010-broken.md"
commit_all "$CASE_DIR" "broken record"
run_advisory "$BASE_SHA" HEAD
expect "a record without readable front matter is an error" exit_code_is 2
expect "the error names the record" contains "$OUT" "docs/upstream/0010-broken.md"

echo
echo "$passed passed, $failed failed"
[[ $failed -eq 0 ]]

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

# Defaults for a same-repository pull request from a human on a GSD phase branch; a case overrides
# any of them by naming the variable again.
DEFAULT_ENV=(
  PR_AUTHOR=maintainer
  PR_HEAD_REF=gsd/phase-01-repository-build-ci-foundation
  PR_HEAD_REPO=owner/repo
  PR_BASE_REPO=owner/repo
)

# run_case <name> <expected exit code> <expected output regex> [VAR=value ...]
# Runs check-pr.sh with only the default and the given variables set and checks exit code and
# combined output.
run_case() {
  local name=$1 expected_code=$2 expected_output=$3
  shift 3
  local output code=0
  output=$(env -i PATH="$PATH" GOVERNED_PATHS_FILE="$GOVERNED" "${DEFAULT_ENV[@]}" "$@" \
    bash "$CHECK" 2>&1) || code=$?
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

# --- fixtures for the Renovate exemption ---------------------------------------------------------

# patch_file <name>: stores standard input as $TMP/<name>.patch and prints the path. A patch is the
# `patch` field of the files API: hunks only, with no file header lines. A context line starts with
# one space, which would trip the indentation checks on this file, so fixtures write it as "~".
patch_file() {
  sed 's/^~/ /' >"$TMP/$1.patch"
  echo "$TMP/$1.patch"
}

# entry <filename> <status> [patch file]: one element of the files API list; without a patch file
# the `patch` field is absent, as the API does for binary or very large diffs.
entry() {
  if [[ $# -ge 3 ]]; then
    jq -n --arg f "$1" --arg s "$2" --arg p "$(cat "$3")" '{filename: $f, status: $s, patch: $p}'
  else
    jq -n --arg f "$1" --arg s "$2" '{filename: $f, status: $s}'
  fi
}

# files_of <name> <entry>...: stores the entries as a JSON array and prints its path.
files_of() {
  local name=$1
  shift
  printf '%s\n' "$@" | jq -s '.' >"$TMP/$name.json"
  echo "$TMP/$name.json"
}

BOT='renovate[bot]'
SHA_OLD=3d3c42e5aac5ba805825da76410c181273ba90b1
SHA_NEW=9f8e7d6c5b4a39281706f5e4d3c2b1a098765432

workflow_bump=$(
  patch_file workflow-bump <<EOF
@@ -22,7 +22,7 @@ jobs:
~    timeout-minutes: 5
~    steps:
-      - uses: actions/checkout@$SHA_OLD # v7.0.1
+      - uses: actions/checkout@$SHA_NEW # v7.0.2
~        with:
~          persist-credentials: false
EOF
)
workflow_bump_files=$(files_of workflow-bump "$(entry .github/workflows/ci.yml modified "$workflow_bump")")

mise_bump=$(
  patch_file mise-bump <<'EOF'
@@ -15,7 +15,7 @@ locked = true
~
~[tools]
-gitleaks = "8.30.1"
+gitleaks = "8.30.2"
~"aqua:rhysd/actionlint" = "1.7.12"
EOF
)
mise_bump_files=$(files_of mise-bump "$(entry mise.toml modified "$mise_bump")")

# A real `mise lock jq` run after moving jq to its previous patch release in a scratch copy of the
# repository; it contains only public release checksums and URLs.
real_lock=$(
  patch_file real-lock <<'EOF'
@@ -226,32 +226,32 @@
~url = "https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.5.1%2B2/OpenJDK25U-jdk_x64_mac_hotspot_25.0.5.1_2.tar.gz"
~
~[[tools.jq]]
-version = "1.8.2"
+version = "1.8.1"
~backend = "aqua:jqlang/jq"
-specifiers = ["1.8.2"]
+specifiers = ["1.8.1"]
~
~[tools.jq."platforms.linux-arm64"]
-checksum = "sha256:8b85c817833814ddca00a144c33705546355afccf0cf39b188f3cdb48b852309"
-url = "https://github.com/jqlang/jq/releases/download/jq-1.8.2/jq-linux-arm64"
-url_api = "https://api.github.com/repos/jqlang/jq/releases/assets/453012756"
+checksum = "sha256:6bc62f25981328edd3cfcfe6fe51b073f2d7e7710d7ef7fcdac28d4e384fc3d4"
+url = "https://github.com/jqlang/jq/releases/download/jq-1.8.1/jq-linux-arm64"
+url_api = "https://api.github.com/repos/jqlang/jq/releases/assets/268981481"
~provenance = "github-attestations"
~
~[tools.jq."platforms.linux-x64"]
-checksum = "sha256:b1c22172dd303f3be49e935aa56aa48a8b7a46e0bc838b4997d3bb451495870f"
-url = "https://github.com/jqlang/jq/releases/download/jq-1.8.2/jq-linux-amd64"
-url_api = "https://api.github.com/repos/jqlang/jq/releases/assets/453012752"
+checksum = "sha256:020468de7539ce70ef1bceaf7cde2e8c4f2ca6c3afb84642aabc5c97d9fc2a0d"
+url = "https://github.com/jqlang/jq/releases/download/jq-1.8.1/jq-linux-amd64"
+url_api = "https://api.github.com/repos/jqlang/jq/releases/assets/268981484"
~provenance = "github-attestations"
~
~[tools.jq."platforms.macos-arm64"]
-checksum = "sha256:2d75340ba57a4b4b4c8708a21c2dc8e958a48aaa8bba13b27f77f6e4c0eca07e"
-url = "https://github.com/jqlang/jq/releases/download/jq-1.8.2/jq-macos-arm64"
-url_api = "https://api.github.com/repos/jqlang/jq/releases/assets/453012783"
+checksum = "sha256:a9fe3ea2f86dfc72f6728417521ec9067b343277152b114f4e98d8cb0e263603"
+url = "https://github.com/jqlang/jq/releases/download/jq-1.8.1/jq-macos-arm64"
+url_api = "https://api.github.com/repos/jqlang/jq/releases/assets/268981506"
~provenance = "github-attestations"
~
~[tools.jq."platforms.macos-x64"]
-checksum = "sha256:e94b266e3c26690550006abe63152b782280f4e14374accdf04cbde844f00bc0"
-url = "https://github.com/jqlang/jq/releases/download/jq-1.8.2/jq-macos-amd64"
-url_api = "https://api.github.com/repos/jqlang/jq/releases/assets/453012782"
+checksum = "sha256:e80dbe0d2a2597e3c11c404f03337b981d74b4a8504b70586c354b7697a7c27f"
+url = "https://github.com/jqlang/jq/releases/download/jq-1.8.1/jq-macos-amd64"
+url_api = "https://api.github.com/repos/jqlang/jq/releases/assets/268981505"
~provenance = "github-attestations"
~
~[[tools.lefthook]]
EOF
)
real_lock_toml=$(
  patch_file real-lock-toml <<'EOF'
@@ -30,5 +30,5 @@
~# transitively; that release was published manually by the Yarn lead maintainer without provenance
~# (3.2.0 had it), which trips trustPolicy=no-downgrade. Remove once renovate no longer pulls it.
~"npm:renovate" = { version = "44.239.1", trust_policy_excludes = ["@yarnpkg/libzip@3.2.2"] }
-jq = "1.8.2"
+jq = "1.8.1"
~gh = "2.104.0"
EOF
)
real_lock_files=$(files_of real-lock \
  "$(entry mise.toml modified "$real_lock_toml")" "$(entry mise.lock modified "$real_lock")")

new_tool=$(
  patch_file new-tool <<'EOF'
@@ -30,5 +30,6 @@
~jq = "1.8.2"
~gh = "2.104.0"
+evil-tool = "1.0.0"
EOF
)
new_tool_files=$(files_of new-tool "$(entry mise.toml modified "$new_tool")")

timeout_change=$(
  patch_file timeout-change <<'EOF'
@@ -10,7 +10,7 @@ jobs:
~  governance:
-    timeout-minutes: 30
+    timeout-minutes: 60
EOF
)
timeout_files=$(files_of timeout "$(entry .github/workflows/ci.yml modified "$timeout_change")")

# A removed line that begins with two dashes shows up as "---" in a patch. It must still count as a
# removal, so it cannot hide next to a version bump.
dashes_hidden=$(
  patch_file dashes-hidden <<'EOF'
@@ -10,5 +10,5 @@
-gitleaks = "8.30.1"
+gitleaks = "8.30.2"
--- guard = "removed"
EOF
)
dashes_files=$(files_of dashes "$(entry mise.toml modified "$dashes_hidden")")

mixed_files=$(files_of mixed \
  "$(entry mise.toml modified "$mise_bump")" "$(entry .github/workflows/ci.yml modified "$timeout_change")")
added_status_files=$(files_of added-status "$(entry mise.toml added "$mise_bump")")
no_patch_files=$(files_of no-patch "$(entry mise.toml modified)")
empty_patch=$(patch_file empty-patch </dev/null)
empty_patch_files=$(files_of empty-patch "$(entry mise.toml modified "$empty_patch")")

# --- Renovate version-only exemption ------------------------------------------------------------

run_case "renovate moving an action SHA and its version comment passes without the label" 0 \
  "version-only.*\.github/workflows/ci\.yml" \
  PR_FILES_JSON="$workflow_bump_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/actions-checkout
run_case "renovate bumping a tool version in mise.toml passes without the label" 0 \
  "version-only.*mise\.toml" \
  PR_FILES_JSON="$mise_bump_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/mise
run_case "renovate bumping a tool through a real mise lock diff passes without the label" 0 \
  "version-only.*mise\.lock" \
  PR_FILES_JSON="$real_lock_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/mise
run_case "renovate adding a new tool line fails without the label" 1 "mise\.toml" \
  PR_FILES_JSON="$new_tool_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/mise
run_case "renovate changing a non-version token fails without the label" 1 \
  "\.github/workflows/ci\.yml" \
  PR_FILES_JSON="$timeout_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/actions-checkout
run_case "renovate hiding a removed line that starts with dashes fails" 1 "mise\.toml" \
  PR_FILES_JSON="$dashes_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/mise
run_case "one non-version governed file spoils the exemption for the whole pull request" 1 \
  "\.github/workflows/ci\.yml" \
  PR_FILES_JSON="$mixed_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/mise
run_case "a human author with a version-only diff still needs the label" 1 "mise\.toml" \
  PR_FILES_JSON="$mise_bump_files" PR_LABELS_JSON="$LABEL_NONE" PR_AUTHOR=maintainer
run_case "an author that only looks like renovate is never exempt" 1 "mise\.toml" \
  PR_FILES_JSON="$mise_bump_files" PR_LABELS_JSON="$LABEL_NONE" PR_AUTHOR='renovate-bot'
run_case "a governed file added by renovate is not exempt" 1 "mise\.toml" \
  PR_FILES_JSON="$added_status_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/mise
run_case "a governed file without a patch is not exempt" 1 "mise\.toml" \
  PR_FILES_JSON="$no_patch_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/mise
run_case "a governed file with an empty patch is not exempt" 1 "mise\.toml" \
  PR_FILES_JSON="$empty_patch_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/mise
run_case "renovate with the label passes even for a non-version change" 0 \
  "carries 'governance-approved'" \
  PR_FILES_JSON="$new_tool_files" PR_LABELS_JSON="$LABEL_APPROVED" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/mise

# --- files API limit ----------------------------------------------------------------------------

# files_many <name> <count>: stores a list of <count> non-governed entries as $TMP/<name>.json and
# prints its path.
files_many() {
  jq -n --argjson count "$2" \
    '[range($count) | {filename: "docs/f\(.).md", status: "added"}]' >"$TMP/$1.json"
  echo "$TMP/$1.json"
}

# One governed glob keeps the per-file loop short for thousands of entries.
limit_globs="$TMP/limit-globs.txt"
echo 'governance/**' >"$limit_globs"

many_2999=$(files_many many-2999 2999)
many_3000=$(files_many many-3000 3000)
many_3001=$(files_many many-3001 3001)

run_case "2999 entries are evaluated file by file and pass" 0 "no governed file is touched" \
  GOVERNED_PATHS_FILE="$limit_globs" PR_FILES_JSON="$many_2999" PR_LABELS_JSON="$LABEL_NONE"
run_case "3000 entries without the label fail and name the count" 1 "3000 entries" \
  GOVERNED_PATHS_FILE="$limit_globs" PR_FILES_JSON="$many_3000" PR_LABELS_JSON="$LABEL_NONE"
run_case "3000 entries without the label are told to add the label" 1 \
  "add the 'governance-approved' label" \
  GOVERNED_PATHS_FILE="$limit_globs" PR_FILES_JSON="$many_3000" PR_LABELS_JSON="$LABEL_NONE"
run_case "3001 entries without the label fail and name the count" 1 "3001 entries" \
  GOVERNED_PATHS_FILE="$limit_globs" PR_FILES_JSON="$many_3001" PR_LABELS_JSON="$LABEL_NONE"
run_case "3000 entries with the label pass" 0 "carries 'governance-approved'" \
  GOVERNED_PATHS_FILE="$limit_globs" PR_FILES_JSON="$many_3000" PR_LABELS_JSON="$LABEL_APPROVED"
run_case "3000 entries from renovate never take the version-only exemption" 1 "files API limit" \
  GOVERNED_PATHS_FILE="$limit_globs" PR_FILES_JSON="$many_3000" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/mise

# --- governed paths grammar (WR-04) -------------------------------------------------------------
# The same case table runs against GovernedPaths.parse in build-logic (GovernedPathsFormatFunctionalTest).

# governed_file <name> <text>: stores `printf %b` of the text as $TMP/<name>.txt and prints its path,
# so a case can spell tabs, carriage returns and raw bytes as escapes.
governed_file() {
  printf '%b' "$2" >"$TMP/$1.txt"
  echo "$TMP/$1.txt"
}

# touch_json <name> <filename>: stores a one-file list as $TMP/<name>.json and prints its path.
touch_json() {
  jq -n --arg f "$2" '[{filename: $f, status: "modified"}]' >"$TMP/$1.json"
  echo "$TMP/$1.json"
}

padded=$(governed_file padded '  .mcp.json  \n')
touches_mcp=$(touch_json touches-mcp .mcp.json)
run_case "G1 a governed entry padded with spaces still governs its path" 1 "\.mcp\.json" \
  GOVERNED_PATHS_FILE="$padded" PR_FILES_JSON="$touches_mcp" PR_LABELS_JSON="$LABEL_NONE"

crlf=$(governed_file crlf '\tmise.toml\r\n')
touches_mise=$(touch_json touches-mise mise.toml)
run_case "G2 a tab-indented entry with a CRLF ending still governs its path" 1 "mise\.toml" \
  GOVERNED_PATHS_FILE="$crlf" PR_FILES_JSON="$touches_mise" PR_LABELS_JSON="$LABEL_NONE"

# run_order_case <name> <first> <second> [VAR=value ...]
# Like run_case, but expects exit 1 and the two texts in the combined output, <first> before <second>.
run_order_case() {
  local name=$1 first=$2 second=$3
  shift 3
  local output code=0
  output=$(env -i PATH="$PATH" GOVERNED_PATHS_FILE="$GOVERNED" "${DEFAULT_ENV[@]}" "$@" \
    bash "$CHECK" 2>&1) || code=$?
  if [[ $code -eq 1 && $output == *"$first"*"$second"* ]]; then
    passed=$((passed + 1))
    echo "ok   - $name"
  else
    failed=$((failed + 1))
    echo "FAIL - $name (exit $code, expected 1; '$first' before '$second')"
    while IFS= read -r line; do
      echo "       | $line"
    done <<<"$output"
  fi
}

NOT_PRINTABLE="entry contains a character outside printable ASCII"

comments=$(governed_file comments '# a comment\n   # indented comment\nmise.toml\n')
touches_comment=$(touch_json touches-comment '# a comment')
run_case "G3 a comment line governs nothing" 0 "no governed file is touched" \
  GOVERNED_PATHS_FILE="$comments" PR_FILES_JSON="$touches_comment" PR_LABELS_JSON="$LABEL_NONE"

inner_space=$(governed_file inner-space 'docs/my notes.md\n')
touches_notes=$(touch_json touches-notes 'docs/my notes.md')
run_case "G4 an entry keeps its inner space" 1 "docs/my notes\.md" \
  GOVERNED_PATHS_FILE="$inner_space" PR_FILES_JSON="$touches_notes" PR_LABELS_JSON="$LABEL_NONE"

nbsp=$(governed_file nbsp 'mise.toml\n\xc2\xa0.mcp.json\n')
run_case "G5 a non-breaking space fails closed naming the line" 1 "line 2: $NOT_PRINTABLE" \
  GOVERNED_PATHS_FILE="$nbsp" PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE"

two_bad=$(governed_file two-bad \
  'a\n\xc2\xa0.mcp.json\nb\nc\nd\ne\nf\ng\nh\nmise\t.toml\n')
run_order_case "G6 every offending line is reported in ascending line order" \
  "line 2:" "line 10:" \
  GOVERNED_PATHS_FILE="$two_bad" PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE"

only_comments=$(governed_file only-comments '# nothing governed\n\n   \n')
run_case "G7 a file with only comments and blank lines lists no governed path" 1 \
  "lists no governed path" \
  GOVERNED_PATHS_FILE="$only_comments" PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE"

bom=$(governed_file bom '\xef\xbb\xbfmise.toml\n')
run_case "G8 a byte order mark fails closed naming the first line" 1 "line 1: $NOT_PRINTABLE" \
  GOVERNED_PATHS_FILE="$bom" PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE"

nul=$(governed_file nul 'mi\x00se.toml\n')
run_case "G8b a NUL byte fails closed instead of being dropped by the reader" 1 \
  "line 1: $NOT_PRINTABLE" \
  GOVERNED_PATHS_FILE="$nul" PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE"

inner_cr=$(governed_file inner-cr 'mise.toml\r.mcp.json\n')
run_case "G9 an inner carriage return fails closed instead of splitting the entry" 1 \
  "line 1: $NOT_PRINTABLE" \
  GOVERNED_PATHS_FILE="$inner_cr" PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE"

# --- head branch naming -------------------------------------------------------------------------

run_case "a same-repository branch outside the allowed names fails" 1 "feature/x" \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE" PR_HEAD_REF=feature/x
run_case "a GSD phase branch with a long slug passes" 0 "branch name" \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE" \
  PR_HEAD_REF=gsd/phase-01-repository-build-ci-foundation
run_case "a GSD decimal phase branch passes" 0 "branch name" \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE" PR_HEAD_REF=gsd/phase-2.1-x
run_case "gsd/phase without number and slug fails" 1 "gsd/phase" \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE" PR_HEAD_REF=gsd/phase
run_case "gsd/phase-x without a number fails" 1 "gsd/phase-x" \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE" PR_HEAD_REF=gsd/phase-x
run_case "gsd/quick-x fails" 1 "gsd/quick-x" \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE" PR_HEAD_REF=gsd/quick-x
run_case "a renovate branch passes" 0 "branch name" \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE" PR_HEAD_REF=renovate/gradle
run_case "a release-please branch passes" 0 "branch name" \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE" \
  PR_HEAD_REF=release-please--branches--main
run_case "an empty head ref on a same-repository pull request fails closed" 1 "head ref" \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE" PR_HEAD_REF=
run_case "a look-alike Unicode branch name never matches" 1 "gsd/phase-01-ünï" \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE" PR_HEAD_REF='gsd/phase-01-ünï'
run_case "a fork pull request is not subject to the naming rule" 0 "fork" \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE" \
  PR_HEAD_REF=patch-1 PR_HEAD_REPO=contributor/repo
run_case "a fork pull request touching a governed file still needs the label" 1 \
  "\.github/workflows/ci\.yml" \
  PR_FILES_JSON="$workflow_change" PR_LABELS_JSON="$LABEL_NONE" \
  PR_HEAD_REF=patch-1 PR_HEAD_REPO=contributor/repo
run_case "a missing PR_BASE_REPO fails closed" 1 "PR_BASE_REPO" \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE" PR_BASE_REPO=

echo "$passed passed, $failed failed"
[[ $failed -eq 0 ]]

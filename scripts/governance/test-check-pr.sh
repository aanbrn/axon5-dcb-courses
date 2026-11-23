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

# renamed_entry <filename> <previous filename> [patch file]: a `renamed` element of the files API
# list, as GitHub reports a file that moved and changed in one pull request.
renamed_entry() {
  if [[ $# -ge 3 ]]; then
    jq -n --arg f "$1" --arg p "$2" --arg d "$(cat "$3")" \
      '{filename: $f, previous_filename: $p, status: "renamed", patch: $d}'
  else
    jq -n --arg f "$1" --arg p "$2" '{filename: $f, previous_filename: $p, status: "renamed"}'
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

# --- fixtures for mise sidecar lockfiles ---------------------------------------------------------

# mise keeps the dependency lock of an npm or pipx tool under .mise/locks/<tool>/<version>~<hash>/,
# so every version bump moves the files to a new directory and GitHub reports them as renamed.
SIDECAR_OLD=.mise/locks/npm-renovate/44.252.7~aa673d8d
SIDECAR_NEW=.mise/locks/npm-renovate/44.252.8~aa673d8d

# The real diffs of Renovate PR #13 (npm:renovate 44.252.7 to 44.252.8). aube-lock.yaml keeps its
# first three hunks; every line is public npm metadata.
sidecar_aube=$(
  patch_file sidecar-aube <<'EOF'
@@ -9,8 +9,8 @@ importers:
~  .:
~    dependencies:
~      renovate:
-        specifier: 44.252.7
-        version: 44.252.7(supports-color@7.2.0)(typanion@3.14.0)
+        specifier: 44.252.8
+        version: 44.252.8(supports-color@7.2.0)(typanion@3.14.0)
~
~packages:
~
@@ -446,8 +446,8 @@ packages:
~      '@opentelemetry/api':
~        optional: true
~
-  '@renovatebot/base-image@13.172.4':
-    resolution: {integrity: sha512-7lMTTuVNCBqSayRa4vNWfhu+owpy9DsxGTuJOIsNBoeIwHqMiJRSQXG5AILHS3Nloab7ohTSTjamsZQp/qeqIQ==}
+  '@renovatebot/base-image@13.172.5':
+    resolution: {integrity: sha512-Q3Q9W8XIw22xVfhGXJ28FhSfoMOkCfdaNCrBCo2ctENbTF2cljd+7XsJE49LELXdzivbBT56t/D0dIM8IgQbmA==}
~    engines: {node: '>=24.10.0'}
~    peerDependencies:
~      zod: ^4.0.0
@@ -1919,8 +1919,8 @@ packages:
~  remark@15.0.1:
~    resolution: {integrity: sha512-Eht5w30ruCXgFmxVUSlNWQ9iiimq07URKeFS3hNc8cUWy1llX4KDWfyEDZRycMc+znsN9Ux5/tJ/BFdgdOwA3A==}
~
-  renovate@44.252.7:
-    resolution: {integrity: sha512-VpbKHbzmXc6Y1ebftn00X9wmm3WWMrGZBuIZQkaaM8HFkyyP0421gSKLOdn5UfKyK1gL3apf8aZfaNKy+SHeMw==}
+  renovate@44.252.8:
+    resolution: {integrity: sha512-4UdKNLUgtG2xbPZdB1ugnkH7ztPMIshAOMLqepXO2pBIcQp+U8I5/BxypCH4ykubCxhVepZ0UMhDDyc1Cle6cQ==}
~    engines: {node: ^24.11.0 || ^26.13.0, pnpm: ^11.0.0}
~    hasBin: true
~
EOF
)
sidecar_package=$(
  patch_file sidecar-package <<'EOF'
@@ -2,6 +2,6 @@
~  "name": "mise-npm-install",
~  "private": true,
~  "dependencies": {
-    "renovate": "44.252.7"
+    "renovate": "44.252.8"
~  }
~}
EOF
)
sidecar_lock=$(
  patch_file sidecar-lock <<'EOF'
@@ -305,10 +305,10 @@
~url = "https://nodejs.org/dist/v26.13.1/node-v26.13.1-darwin-x64.tar.gz"
~
~[[tools."npm:renovate"]]
-version = "44.252.7"
-aube = { path = ".mise/locks/npm-renovate/44.252.7~aa673d8d", digest = "sha256:8f2f65fcb2cc08c993bb95b23ec952f2602b3fe131e0dcc5890f710890d3ecc0" }
+version = "44.252.8"
+aube = { path = ".mise/locks/npm-renovate/44.252.8~aa673d8d", digest = "sha256:adefe5520c0a0dca6acb6c9b0d000c5e693a32a4832fbfad15ab22e1637cdd35" }
~backend = "npm:renovate"
-specifiers = ["44.252.7"]
+specifiers = ["44.252.8"]
~
EOF
)
sidecar_toml=$(
  patch_file sidecar-toml <<'EOF'
@@ -34,6 +34,6 @@ uv = "0.13.8"
~# (3.2.0 had it), which trips trustPolicy=no-downgrade. Remove once renovate no longer pulls it.
-"npm:renovate" = { version = "44.252.7", trust_policy_excludes = ["@yarnpkg/libzip@3.2.2"] }
+"npm:renovate" = { version = "44.252.8", trust_policy_excludes = ["@yarnpkg/libzip@3.2.2"] }
~jq = "1.8.2"
EOF
)
pr13_files=$(files_of pr13 \
  "$(renamed_entry "$SIDECAR_NEW/aube-lock.yaml" "$SIDECAR_OLD/aube-lock.yaml" "$sidecar_aube")" \
  "$(renamed_entry "$SIDECAR_NEW/package.json" "$SIDECAR_OLD/package.json" "$sidecar_package")" \
  "$(entry mise.lock modified "$sidecar_lock")" "$(entry mise.toml modified "$sidecar_toml")")

# A version bump that also pulls in a dependency the lock did not have before.
sidecar_new_dep=$(
  patch_file sidecar-new-dep <<'EOF'
@@ -1919,8 +1919,11 @@ packages:
-  renovate@44.252.7:
-    resolution: {integrity: sha512-VpbKHbzmXc6Y1ebftn00X9wmm3WWMrGZBuIZQkaaM8HFkyyP0421gSKLOdn5UfKyK1gL3apf8aZfaNKy+SHeMw==}
+  renovate@44.252.8:
+    resolution: {integrity: sha512-4UdKNLUgtG2xbPZdB1ugnkH7ztPMIshAOMLqepXO2pBIcQp+U8I5/BxypCH4ykubCxhVepZ0UMhDDyc1Cle6cQ==}
+
+  evil-postinstall@1.0.0:
+    resolution: {integrity: sha512-Eht5w30ruCXgFmxVUSlNWQ9iiimq07URKeFS3hNc8cUWy1llX4KDWfyEDZRycMc+znsN9Ux5/tJ/BFdgdOwA3A==}
EOF
)
new_dep_files=$(files_of sidecar-new-dep \
  "$(renamed_entry "$SIDECAR_NEW/aube-lock.yaml" "$SIDECAR_OLD/aube-lock.yaml" "$sidecar_new_dep")")

# A pipx tool lock written by uv: versions, sha256 hashes, upload times and the PyPI path segments
# change with the release; nothing else does.
uv_lock=$(
  patch_file uv-lock <<'EOF'
@@ -40,10 +40,10 @@
~[[package]]
~name = "reuse"
-version = "6.3.0"
-source = { registry = "https://pypi.org/simple" }
-sdist = { url = "https://files.pythonhosted.org/packages/4b/1a/0c2e6d1f5a9e8b7c3d2f1e0a9b8c7d6e5f4a3b2c1d0e9f8a7b6c5d4e3f2a1b0c/reuse-6.3.0.tar.gz", hash = "sha256:0c2e6d1f5a9e8b7c3d2f1e0a9b8c7d6e5f4a3b2c1d0e9f8a7b6c5d4e3f2a1b0c", size = 389514, upload-time = "2026-10-01T09:12:44.512Z" }
+version = "6.3.1"
+source = { registry = "https://pypi.org/simple" }
+sdist = { url = "https://files.pythonhosted.org/packages/9e/77/5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c/reuse-6.3.1.tar.gz", hash = "sha256:5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c", size = 390022, upload-time = "2026-11-12T17:03:07Z" }
~
EOF
)
UV_OLD=.mise/locks/pipx-reuse/6.3.0~068c1fb0
UV_NEW=.mise/locks/pipx-reuse/6.3.1~068c1fb0
uv_files=$(files_of uv-lock "$(renamed_entry "$UV_NEW/uv.lock" "$UV_OLD/uv.lock" "$uv_lock")")

other_tool_files=$(files_of sidecar-other-tool \
  "$(renamed_entry .mise/locks/npm-evil/44.252.8~aa673d8d/package.json "$SIDECAR_OLD/package.json" "$sidecar_package")")
other_hash_files=$(files_of sidecar-other-hash \
  "$(renamed_entry .mise/locks/npm-renovate/44.252.8~bb000000/package.json "$SIDECAR_OLD/package.json" "$sidecar_package")")
out_of_locks_files=$(files_of sidecar-out-of-locks \
  "$(renamed_entry mise/locks/npm-renovate/44.252.8~aa673d8d/package.json "$SIDECAR_OLD/package.json" "$sidecar_package")")
renamed_no_patch_files=$(files_of sidecar-no-patch \
  "$(renamed_entry "$SIDECAR_NEW/package.json" "$SIDECAR_OLD/package.json")")
renamed_governed_files=$(files_of renamed-governed \
  "$(renamed_entry mise.toml mise.toml.old "$mise_bump")")

# --- Renovate exemption for mise sidecar lockfiles ------------------------------------------------

run_case "renovate PR #13 moving the npm sidecar lock to its new version passes without the label" 0 \
  "version-only.*$SIDECAR_NEW/aube-lock\.yaml" \
  PR_FILES_JSON="$pr13_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/dev-tools
run_case "renovate PR #13 is exempt as a whole, sidecar files and mise.lock alike" 0 \
  "every governed change is a version-only" \
  PR_FILES_JSON="$pr13_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/dev-tools
run_case "renovate moving a uv sidecar lock to its new version passes without the label" 0 \
  "version-only.*$UV_NEW/uv\.lock" \
  PR_FILES_JSON="$uv_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/dev-tools
run_case "a sidecar bump that adds a new dependency fails without the label" 1 \
  "$SIDECAR_NEW/aube-lock\.yaml" \
  PR_FILES_JSON="$new_dep_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/dev-tools
run_case "a sidecar moved to another tool's directory is not exempt" 1 "npm-evil" \
  PR_FILES_JSON="$other_tool_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/dev-tools
run_case "a sidecar moved to a directory with another hash is not exempt" 1 "bb000000" \
  PR_FILES_JSON="$other_hash_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/dev-tools
run_case "a sidecar moved out of .mise/locks is not exempt" 1 "$SIDECAR_OLD/package\.json" \
  PR_FILES_JSON="$out_of_locks_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/dev-tools
run_case "a renamed sidecar without a patch is not exempt" 1 "$SIDECAR_NEW/package\.json" \
  PR_FILES_JSON="$renamed_no_patch_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/dev-tools
run_case "a rename of a governed file outside .mise/locks is still never exempt" 1 "mise\.toml" \
  PR_FILES_JSON="$renamed_governed_files" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/mise
run_case "a human author moving a sidecar lock still needs the label" 1 \
  "$SIDECAR_NEW/package\.json" \
  PR_FILES_JSON="$pr13_files" PR_LABELS_JSON="$LABEL_NONE" PR_AUTHOR=maintainer

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

# --- reading the changed files list (WR-05) -----------------------------------------------------
# The gate must either evaluate every entry or fail; it never passes a list it did not read.

# A NUL cannot travel in the NUL-delimited record stream, so jq aborts part-way through the list.
# The NUL is the JSON escape \u0000, so the fixture file itself holds no raw NUL byte.
nul_patch=$(
  files_json nul-patch <<'JSON'
[
{"filename": "docs/tooling.md", "status": "modified"},
{"filename": ".github/workflows/ci.yml", "status": "modified", "patch": "@@ -1 +1 @@\n-a\n+b\u0000c"}
]
JSON
)
run_case "R1 a NUL in a governed entry's patch fails the gate instead of dropping the entry" 1 \
  "could not be read: jq exited with status" \
  PR_FILES_JSON="$nul_patch" PR_LABELS_JSON="$LABEL_NONE"

# The same NUL in the only entry, which is not governed: an unreadable entry fails the gate even
# when it is not governed, because nothing after it can be trusted.
nul_only_patch=$(
  files_json nul-only-patch <<'JSON'
[{"filename": "docs/tooling.md", "status": "modified", "patch": "@@ -1 +1 @@\n-a\n+b\u0000c"}]
JSON
)
run_case "R2 a NUL in the patch of the only, ungoverned entry fails the gate" 1 \
  "could not be read: jq exited with status" \
  PR_FILES_JSON="$nul_only_patch" PR_LABELS_JSON="$LABEL_NONE"

nul_filename=$(
  files_json nul-filename <<'JSON'
[{"filename": "docs/a\u0000b.md", "status": "added"}]
JSON
)
run_case "R3 a NUL in a filename fails the gate" 1 "could not be read: jq exited with status" \
  PR_FILES_JSON="$nul_filename" PR_LABELS_JSON="$LABEL_NONE"

nul_previous=$(
  files_json nul-previous <<'JSON'
[{"filename": "docs/new.md", "previous_filename": "docs/old\u0000.md", "status": "renamed"}]
JSON
)
run_case "R4 a NUL in a previous_filename fails the gate" 1 \
  "could not be read: jq exited with status" \
  PR_FILES_JSON="$nul_previous" PR_LABELS_JSON="$LABEL_NONE"

run_case "R5 the label never approves an unreadable list" 1 \
  "could not be read: jq exited with status" \
  PR_FILES_JSON="$nul_patch" PR_LABELS_JSON="$LABEL_APPROVED"
run_case "R6 the renovate exemption is never consulted for an unreadable list" 1 \
  "could not be read: jq exited with status" \
  PR_FILES_JSON="$nul_patch" PR_LABELS_JSON="$LABEL_NONE" \
  PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/mise

# A jq that misbehaves on demand, so each guard is proven on its own. fake_jq writes a wrapper named
# jq and prints its directory; a case puts that directory first on PATH. Every call that does not
# stream records (validation, `length`, the label checks) goes to the real jq.
#   FAKE_JQ_REAL  absolute path of the real jq
#   FAKE_JQ_MODE  pass | exit-after-output | drop-entries | torn-record
#   FAKE_JQ_LOG   optional file that receives a listing of the temporary directory
REAL_JQ=$(command -v jq)

fake_jq() {
  local dir="$TMP/fake-jq"
  mkdir -p "$dir"
  cat >"$dir/jq" <<'WRAPPER'
#!/usr/bin/env bash
streams_records=0
for arg in "$@"; do
  [[ $arg == --raw-output0 ]] && streams_records=1
done
if [[ $streams_records -eq 0 ]]; then
  exec "$FAKE_JQ_REAL" "$@"
fi
if [[ -n ${FAKE_JQ_LOG:-} ]]; then
  ls -A "${TMPDIR:-/tmp}" >>"$FAKE_JQ_LOG"
fi
case ${FAKE_JQ_MODE:-pass} in
pass)
  exec "$FAKE_JQ_REAL" "$@"
  ;;
exit-after-output)
  "$FAKE_JQ_REAL" "$@"
  exit 3
  ;;
drop-entries)
  printf '%s\0' docs/tooling.md '' modified 0 ''
  ;;
torn-record)
  printf '%s\0' docs/tooling.md '' modified
  ;;
esac
WRAPPER
  chmod +x "$dir/jq"
  echo "$dir"
}

# run_tmpdir_case <name> <expected exit code> <expected output regex> <tmpdir> <log> [VAR=value ...]
# Like run_case, but the gate gets its own empty temporary directory. The case passes only when the
# gate exits as expected, the records file existed in that directory during the run (the fake jq
# logs a listing, so the case cannot pass vacuously) and nothing is left in the directory afterwards.
run_tmpdir_case() {
  local name=$1 expected_code=$2 expected_output=$3 tmpdir=$4 log=$5
  shift 5
  mkdir -p "$tmpdir"
  : >"$log"
  local output code=0 leftover during
  output=$(env -i PATH="$PATH" GOVERNED_PATHS_FILE="$GOVERNED" "${DEFAULT_ENV[@]}" \
    TMPDIR="$tmpdir" FAKE_JQ_LOG="$log" "$@" bash "$CHECK" 2>&1) || code=$?
  leftover=$(find "$tmpdir" -mindepth 1 -print)
  during=$(grep -c 'check-pr-records\.' "$log" || true)
  if [[ $code -eq $expected_code && $during -ge 1 && -z $leftover ]] &&
    grep -Eq -- "$expected_output" <<<"$output"; then
    passed=$((passed + 1))
    echo "ok   - $name"
  else
    failed=$((failed + 1))
    echo "FAIL - $name (exit $code, expected $expected_code; records file seen during run: $during;" \
      "left behind: ${leftover:-nothing}; output pattern: $expected_output)"
    while IFS= read -r line; do
      echo "       | $line"
    done <<<"$output"
  fi
}

fake_dir=$(fake_jq)

run_case "R7 a jq that exits non-zero after writing every record fails on its status alone" 1 \
  "jq exited with status 3" \
  PATH="$fake_dir:$PATH" FAKE_JQ_REAL="$REAL_JQ" FAKE_JQ_MODE=exit-after-output \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE"

two_entries=$(
  files_json two-entries <<'JSON'
[
{"filename": "docs/tooling.md", "status": "modified"},
{"filename": ".github/workflows/ci.yml", "status": "modified"}
]
JSON
)
run_case "R8 a jq that exits 0 one record short fails on the completeness check alone" 1 \
  "1 of 2 entries were evaluated" \
  PATH="$fake_dir:$PATH" FAKE_JQ_REAL="$REAL_JQ" FAKE_JQ_MODE=drop-entries \
  PR_FILES_JSON="$two_entries" PR_LABELS_JSON="$LABEL_NONE"
run_case "R9 a torn record fails the completeness check" 1 "0 of 1 entries were evaluated" \
  PATH="$fake_dir:$PATH" FAKE_JQ_REAL="$REAL_JQ" FAKE_JQ_MODE=torn-record \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE"

run_tmpdir_case "R10 no records file is left behind when the gate fails" 1 \
  "jq exited with status 3" "$TMP/r10-tmp" "$TMP/r10.log" \
  PATH="$fake_dir:$PATH" FAKE_JQ_REAL="$REAL_JQ" FAKE_JQ_MODE=exit-after-output \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE"
run_tmpdir_case "R11 no records file is left behind when the gate passes" 0 \
  "no governed file is touched" "$TMP/r11-tmp" "$TMP/r11.log" \
  PATH="$fake_dir:$PATH" FAKE_JQ_REAL="$REAL_JQ" FAKE_JQ_MODE=pass \
  PR_FILES_JSON="$docs_only" PR_LABELS_JSON="$LABEL_NONE"

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

# --- governed boundary (WR-01) ------------------------------------------------------------------
# These cases run against the real governance/governed-paths.txt, so they also prove that the
# commented file is read as plan 01-16's grammar defines.

# boundary_case <path> <governed|ungoverned>
# A one-file pull request touching <path> by a human, without the label: it must be blocked when the
# path is governed and pass when it is not.
boundary_case() {
  local path=$1 expectation=$2 files
  files=$(touch_json "boundary-$((passed + failed))" "$path")
  case "$expectation" in
  governed)
    run_case "boundary: $path is governed and needs the label" 1 "touches governed files" \
      PR_FILES_JSON="$files" PR_LABELS_JSON="$LABEL_NONE"
    ;;
  ungoverned)
    run_case "boundary: $path is ungoverned and passes without the label" 0 \
      "no governed file is touched" PR_FILES_JSON="$files" PR_LABELS_JSON="$LABEL_NONE"
    ;;
  *)
    echo "boundary_case: unknown expectation '$expectation'" >&2
    exit 2
    ;;
  esac
}

boundary_case build-logic/src/main/kotlin/courses.governance.gradle.kts governed
boundary_case \
  build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ConfigPolicyCheck.kt governed
boundary_case scripts/apply-github-settings.sh governed
boundary_case build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts ungoverned

# The rest of the enforcement boundary: CODEOWNERS location, root build configuration, the plugin
# build, the root and settings convention plugins and the launcher.
boundary_case docs/CODEOWNERS governed
boundary_case build.gradle.kts governed
boundary_case build.gradle governed
boundary_case settings.gradle.kts governed
boundary_case settings.gradle governed
boundary_case gradle.properties governed
boundary_case buildSrc/build.gradle.kts governed
boundary_case build-logic/build.gradle.kts governed
boundary_case build-logic/build.gradle governed
boundary_case build-logic/settings.gradle.kts governed
boundary_case build-logic/settings.gradle governed
boundary_case build-logic/src/main/kotlin/courses.root.gradle.kts governed
boundary_case build-logic/src/main/kotlin/courses.settings.settings.gradle.kts governed
boundary_case gradlew governed
boundary_case gradle/wrapper/gradle-wrapper.jar governed
boundary_case gradle/gradle-daemon-jvm.properties governed

# Deliberately outside the boundary, for the reasons recorded in governance/governed-paths.txt.
boundary_case \
  build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheckFunctionalTest.kt \
  ungoverned
boundary_case gradle/libs.versions.toml ungoverned
boundary_case gradlew.bat ungoverned
boundary_case scripts/test-apply-github-settings.sh ungoverned
boundary_case backend/contracts/kernel/build.gradle.kts ungoverned

# Alternate Renovate config names and mise files (WR-07).
boundary_case renovate.json governed
boundary_case renovate.jsonc governed
boundary_case '.mise/locks/pipx-reuse/6.3.0~068c1fb0/uv.lock' governed
boundary_case .gitlab/renovate.json governed
boundary_case .gitlab/renovate.jsonc governed
boundary_case .gitlab/renovate.json5 governed
boundary_case .renovaterc governed
boundary_case .renovaterc.json governed
boundary_case .renovaterc.jsonc governed
boundary_case .renovaterc.json5 governed
boundary_case package.json governed
boundary_case .mise.toml governed
boundary_case mise.local.toml governed
boundary_case .mise.local.toml governed
boundary_case mise/config.toml governed
boundary_case .mise/config.toml governed
boundary_case .mise/config.local.toml governed
boundary_case .mise/conf.d/tools.toml governed
boundary_case .config/mise.toml governed
boundary_case .config/mise.local.toml governed
boundary_case .config/mise/config.toml governed
boundary_case .tool-versions governed
boundary_case .miserc.toml governed
boundary_case .miserc.local.toml governed
boundary_case .config/miserc.toml governed

# Recorded exclusions, each explained in governance/governed-paths.txt: an environment file, the
# lockfile of an alternate mise config, a mise config below the root and a nested package.json.
boundary_case mise.ci.toml ungoverned
boundary_case mise.local.lock ungoverned
boundary_case backend/contracts/kernel/mise.toml ungoverned
boundary_case frontend/package.json ungoverned

# A root renovate.json beats the governed renovate.json5, and the file does not exist yet: adding it
# must still need the label.
run_case "a pull request adding renovate.json without the label fails" 1 "renovate\.json" \
  PR_FILES_JSON="$(files_of adds-renovate-json "$(entry renovate.json added)")" \
  PR_LABELS_JSON="$LABEL_NONE"

# The exemption is not extended: a bot adding a sidecar lockfile adds files, so it needs the label.
# This pins the accepted cost of governing .mise/locks/**.
run_case "renovate adding a mise sidecar lockfile needs the label" 1 "\.mise/locks/npm-renovate" \
  PR_FILES_JSON="$(files_of adds-sidecar \
    "$(entry '.mise/locks/npm-renovate/44.240.0~0123abcd/package.json' added)")" \
  PR_LABELS_JSON="$LABEL_NONE" PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/mise

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

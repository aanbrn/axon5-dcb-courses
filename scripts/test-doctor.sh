#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Self-contained tests for doctor.sh. Every case runs the script against a temporary repository tree and a
# fake HOME, with fake `mise`, `java` and `docker` commands first on a PATH that holds nothing else but
# symlinks to the few real tools the script needs. So the tests prove what the script reports, how it
# reports a missing prerequisite, and that it never changes anything it reads.
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
DOCTOR="$SCRIPT_DIR/doctor.sh"
BASH_BIN=$(command -v bash)

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

# The real tools doctor.sh may call. They are linked into one directory so that PATH can be restricted to it:
# a `java` or `docker` that the machine has must never leak into a case that expects it to be absent.
SYSBIN="$TMP/sysbin"
mkdir -p "$SYSBIN"
for tool in bash env awk sed sort grep cat dirname basename jq paste tr head tail cut find git shasum sha256sum wc uname; do
  if command -v "$tool" >/dev/null 2>&1; then ln -s "$(command -v "$tool")" "$SYSBIN/$tool"; fi
done

passed=0
failed=0
OUT=''
CODE=0

# The tool list `mise ls --current --json` prints for a healthy installation.
HEALTHY_TOOLS='{
  "jq": [{"version": "1.8.2", "requested_version": "1.8.2", "installed": true, "active": true}],
  "shfmt": [{"version": "3.14.1", "requested_version": "3.14.1", "installed": true, "active": true}]
}'

# make_env <dir>: a repository tree with mise.toml and a copy of doctor.sh, an empty fake HOME and fake commands.
# Every fake is reached through the file or fixture named below; remove a fake to simulate a missing command.
make_env() {
  local dir=$1
  mkdir -p "$dir/repo/scripts" "$dir/home" "$dir/bin"
  cp "$DOCTOR" "$dir/repo/scripts/doctor.sh"
  printf 'min_version = "2026.11.11"\n\n[tools]\njq = "1.8.2"\nshfmt = "3.14.1"\n' >"$dir/repo/mise.toml"
  printf '%s\n' "$HEALTHY_TOOLS" >"$dir/mise-tools.json"

  cat >"$dir/bin/mise" <<'FAKE'
#!/usr/bin/env bash
case "$*" in
--version) echo "2026.11.12 macos-arm64 (2026-11-21)" ;;
"ls --current --json") cat "$FAKE_DIR/mise-tools.json" ;;
"which jq") command -v jq ;;
*) exit 1 ;;
esac
FAKE

  cat >"$dir/bin/java" <<'FAKE'
#!/usr/bin/env bash
exit 0
FAKE

  # The context comes from `docker-context`; `docker-down` makes `docker info` fail like a stopped engine.
  echo colima >"$dir/docker-context"
  cat >"$dir/bin/docker" <<'FAKE'
#!/usr/bin/env bash
case "$*" in
"context show") cat "$FAKE_DIR/docker-context" ;;
"info --format {{.ServerVersion}}")
  [[ -e $FAKE_DIR/docker-down ]] && exit 1
  echo "29.0.0"
  ;;
"info --format {{.OperatingSystem}}") echo "Ubuntu 24.04" ;;
*) exit 1 ;;
esac
FAKE
  chmod +x "$dir/bin/mise" "$dir/bin/java" "$dir/bin/docker"
}

# run_doctor <env dir> args...: runs the copy of doctor.sh in the tree; sets OUT and CODE. The environment is
# empty but for HOME, PATH and what the script is told, so nothing of the machine's setup influences a case.
# Set EXTRA_ENV to a space-separated list of NAME=value pairs to add to it.
run_doctor() {
  local dir=$1
  shift
  CODE=0
  # shellcheck disable=SC2086 # EXTRA_ENV is a word list of NAME=value pairs on purpose
  OUT=$(env -i HOME="$dir/home" PATH="$dir/bin:$SYSBIN" FAKE_DIR="$dir" DOCTOR_ROOT="$dir/repo" \
    DOCTOR_SKIP_GRADLE=1 ${EXTRA_ENV:-} "$BASH_BIN" "$dir/repo/scripts/doctor.sh" "$@" 2>&1) || CODE=$?
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

# assert_equals <name> <expected> <actual>
assert_equals() {
  if [[ $2 == "$3" ]]; then pass "$1"; else fail "$1" "expected [$2], got [$3]"; fi
}

# tree_hash <dir>: one digest over the path and content of every file below the directory.
tree_hash() {
  (cd "$1" && find . -type f -print0 | sort -z | xargs -0 shasum -a 256 | shasum -a 256)
}

n=0
fresh() {
  n=$((n + 1))
  ENV="$TMP/env$n"
  make_env "$ENV"
  EXTRA_ENV=''
}

# --- usage ---------------------------------------------------------------------------------------

fresh
run_doctor "$ENV" --help
assert_code "--help" 0
assert_out "--help" "--ci"
assert_out "--help" "Usage: doctor.sh"

run_doctor "$ENV" --frobnicate
assert_code "unknown flag" 2
assert_out "unknown flag" "unknown argument: --frobnicate"

# --- every prerequisite present --------------------------------------------------------------------

fresh
run_doctor "$ENV"
assert_code "healthy tree" 0
assert_out "healthy tree" "^ok  mise: 2026\\.11\\.12 satisfies min_version 2026\\.11\\.11"
assert_out "healthy tree" "^ok  tools: all 2 pinned tools are installed and active"
assert_out "healthy tree" "^ok  java: "
assert_out "healthy tree" "^ok  docker: colima reachable through context colima, server 29\\.0\\.0"
assert_out "healthy tree" "^warn  gradle: skipped"
assert_no_out "healthy tree" "^fail  "
assert_out "healthy tree" "^summary: [0-9]+ ok, [0-9]+ warn, 0 fail, [0-9]+ skip"

# --- a missing docker ------------------------------------------------------------------------------

fresh
rm "$ENV/bin/docker"
run_doctor "$ENV"
assert_code "docker missing" 1
assert_out "docker missing" "^fail  docker: the docker CLI is not on PATH"
assert_out "docker missing" "^      fix: colima: brew install docker colima && colima start"
assert_out "docker missing" "^ok  mise: "

# A stopped engine is a failure that names the engine's own start command.
fresh
: >"$ENV/docker-down"
run_doctor "$ENV"
assert_code "docker stopped" 1
assert_out "docker stopped" "^fail  docker: the engine of context colima does not answer"
assert_out "docker stopped" "^      fix: colima start"

# --- a missing Java runtime ------------------------------------------------------------------------

fresh
rm "$ENV/bin/java"
run_doctor "$ENV"
assert_code "java missing" 1
assert_out "java missing" "^fail  java: no Java runtime"
assert_out "java missing" "^      fix: mise install"
assert_out "java missing" "^warn  gradle: skipped: ./gradlew doctor needs a Java runtime"

# JAVA_HOME alone is enough, with no java on PATH.
fresh
rm "$ENV/bin/java"
mkdir -p "$ENV/jdk/bin"
cp "$ENV/bin/docker" "$ENV/jdk/bin/java"
EXTRA_ENV="JAVA_HOME=$ENV/jdk"
run_doctor "$ENV"
assert_code "JAVA_HOME only" 0
assert_out "JAVA_HOME only" "^ok  java: a Java runtime is available through JAVA_HOME"

# --- mise ------------------------------------------------------------------------------------------

fresh
printf 'min_version = "2027.1.1"\n' >"$ENV/repo/mise.toml"
run_doctor "$ENV"
assert_code "mise too old" 1
assert_out "mise too old" "^fail  mise: 2026\\.11\\.12 is older than min_version 2027\\.1\\.1"

fresh
printf '%s\n' '{"jq": [{"version": "1.8.2", "requested_version": "1.8.2", "installed": true, "active": true}],
  "taplo": [{"version": "0.10.0", "requested_version": "0.10.0", "installed": false, "active": true}]}' \
  >"$ENV/mise-tools.json"
run_doctor "$ENV"
assert_code "tool not installed" 1
assert_out "tool not installed" "^fail  tools: not installed or not active: taplo@0\\.10\\.0"
assert_out "tool not installed" "^      fix: mise install"

# --- nothing is changed, and a second run says the same ----------------------------------------------

fresh
before=$(tree_hash "$ENV")
run_doctor "$ENV"
first_out=$OUT
first_code=$CODE
run_doctor "$ENV"
assert_equals "second run prints the same output" "$first_out" "$OUT"
assert_equals "second run exits the same" "$first_code" "$CODE"
assert_equals "the tree is unchanged after two runs" "$before" "$(tree_hash "$ENV")"

echo
echo "$passed passed, $failed failed"
[[ $failed -eq 0 ]]

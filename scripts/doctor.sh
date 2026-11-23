#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Diagnoses the development environment and says how to fix every problem (TOOL-03, D-27). It runs every
# check, prints each as ok, warn, fail or skip with the exact fix command, exits 1 when any check fails,
# and changes nothing: no install, update, start, pull or write. The fix lines are suggestions for you.
#
# Which checks live where (D-48, one implementation per check):
#   - This script runs only what must work before Gradle can start (mise and its tools, a Java runtime
#     for the Gradle wrapper, the Docker engine) and the local-only checks Gradle does not own.
#   - Everything else runs as `./gradlew doctor`: the Node major (verifyNodeVersion), the formatter
#     binaries (verifyShfmt, verifyTaplo), the daemon JVM criteria (checkDaemonJvm) and Testcontainers
#     connectivity (testcontainersProbe). The last check below runs it, so each failure carries the
#     fix hint of the task that found it.
#
# Usage: doctor.sh [--ci] [--help]
#   --ci    skip the checks that only make sense on a developer machine (GSD, MCP servers, the
#           Platform token, colima and the Claude Code version); CI runs this mode
#   --help  print this text
# Environment (for the shell tests): DOCTOR_ROOT is the repository root (default: the parent of
# this script's directory), DOCTOR_SKIP_GRADLE=1 skips the delegation to Gradle.
# Exit codes: 0 no check failed (warnings are allowed), 1 a check failed, 2 usage error.
set -euo pipefail

# Byte-wise comparison and stable sorting regardless of the caller's locale.
export LC_ALL=C

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
readonly SCRIPT_DIR
ROOT=${DOCTOR_ROOT:-$(cd "$SCRIPT_DIR/.." && pwd)}
readonly ROOT

CI_MODE=false
JAVA_OK=false
COUNT_OK=0
COUNT_WARN=0
COUNT_FAIL=0
COUNT_SKIP=0

usage() {
  sed -n '2,/^set -euo/p' "${BASH_SOURCE[0]}" | sed -e '$d' -e '1d' -e 's/^# \{0,1\}//'
}

# report <ok|warn|fail|skip> <check> <detail> [fix...]
# Prints `<status>  <check>: <detail>` and one indented `fix:` line per fix, and counts the status.
report() {
  local status=$1 check=$2 detail=$3 fix
  shift 3
  printf '%s  %s: %s\n' "$status" "$check" "$detail"
  for fix in "$@"; do
    printf '      fix: %s\n' "$fix"
  done
  case $status in
  ok) COUNT_OK=$((COUNT_OK + 1)) ;;
  warn) COUNT_WARN=$((COUNT_WARN + 1)) ;;
  fail) COUNT_FAIL=$((COUNT_FAIL + 1)) ;;
  skip) COUNT_SKIP=$((COUNT_SKIP + 1)) ;;
  *)
    echo "doctor.sh: internal error: unknown status $status" >&2
    exit 2
    ;;
  esac
}

has() {
  command -v "$1" >/dev/null 2>&1
}

# The path of jq: the one on PATH, else the one mise has pinned. Fails when neither exists.
find_jq() {
  if has jq; then
    command -v jq
  elif has mise; then
    mise which jq 2>/dev/null
  else
    return 1
  fi
}

# mise: present, and at least the min_version of mise.toml (the only place that version lives).
check_mise() {
  if ! has mise; then
    report fail mise "not found on PATH" \
      "install mise (brew install mise, or https://mise.jdx.dev/getting-started.html), then run: mise install"
    return
  fi
  local minimum installed lowest
  minimum=$(sed -nE 's/^min_version[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$ROOT/mise.toml" 2>/dev/null | sed -n 1p)
  installed=$(mise --version 2>/dev/null | awk 'NR == 1 { print $1 }')
  if [[ -z $installed ]]; then
    report fail mise "cannot read the installed version from 'mise --version'" "brew upgrade mise, or: mise self-update"
    return
  fi
  if [[ -z $minimum ]]; then
    report ok mise "$installed (mise.toml sets no min_version)"
    return
  fi
  lowest=$(printf '%s\n%s\n' "$minimum" "$installed" | sort -V | sed -n 1p)
  if [[ $lowest == "$minimum" ]]; then
    report ok mise "$installed satisfies min_version $minimum"
  else
    report fail mise "$installed is older than min_version $minimum from mise.toml" \
      "brew upgrade mise, or: mise self-update"
  fi
}

# tools: mise's own installation state. Every tool the project pins is installed and active. The
# binary checks of the formatters and of Node are Gradle's (verifyShfmt, verifyTaplo, verifyNodeVersion).
check_tools() {
  if ! has mise; then
    report skip tools "needs mise, see the mise check"
    return
  fi
  local jq json total missing
  if ! jq=$(find_jq); then
    report fail tools "jq is needed to read the tool list of mise and is not installed" "mise install"
    return
  fi
  if ! json=$(cd "$ROOT" && mise ls --current --json 2>/dev/null); then
    report fail tools "'mise ls --current --json' failed in the repository" "mise install"
    return
  fi
  total=$("$jq" -r '[.[][]] | length' <<<"$json")
  # shellcheck disable=SC2016 # a jq program, not shell
  missing=$("$jq" -r '
    to_entries[] | .key as $tool | .value[]
    | select(((.installed // false) and (.active // false)) | not)
    | "\($tool)@\(.requested_version // .version // "?")"' <<<"$json" | paste -sd, - | sed 's/,/, /g')
  if [[ -n $missing ]]; then
    report fail tools "not installed or not active: $missing" "mise install"
  else
    report ok tools "all $total pinned tools are installed and active"
  fi
}

# java: a Java runtime that can start the Gradle wrapper. Presence only: the daemon JVM criteria
# (checkDaemonJvm, run by ./gradlew doctor) select the real JDK, so no version is compared here.
check_java() {
  if [[ -n ${JAVA_HOME:-} && -x $JAVA_HOME/bin/java ]]; then
    JAVA_OK=true
    report ok java "a Java runtime is available through JAVA_HOME to start the Gradle wrapper"
  elif has java; then
    JAVA_OK=true
    report ok java "a Java runtime is on PATH to start the Gradle wrapper"
  else
    local fix="mise install"
    if $CI_MODE; then
      fix="add actions/setup-java with java-version-file: .java-version before this step"
    fi
    report fail java "no Java runtime to start the Gradle wrapper (JAVA_HOME is unset or invalid, no java on PATH)" "$fix"
  fi
}

# docker_field <Go template field of `docker info`>: the field's value, empty when it cannot be read.
docker_field() {
  docker info --format "{{.$1}}" 2>/dev/null || true
}

# docker: the engine answers, and which engine it is (colima, Docker Desktop or another).
check_docker() {
  if ! has docker; then
    if $CI_MODE; then
      report fail docker "the docker CLI is not on PATH" "run this job on an ubuntu runner, which ships Docker"
    else
      report fail docker "the docker CLI is not on PATH" \
        "colima: brew install docker colima && colima start" \
        "Docker Desktop: install it from https://www.docker.com/products/docker-desktop and start it"
    fi
    return
  fi
  local context version operating_system engine
  context=$(docker context show 2>/dev/null || true)
  if ! version=$(docker info --format '{{.ServerVersion}}' 2>/dev/null) || [[ -z $version ]]; then
    case $context in
    colima*) report fail docker "the engine of context ${context} does not answer" "colima start" ;;
    desktop*) report fail docker "the engine of context ${context} does not answer" "start Docker Desktop" ;;
    *)
      report fail docker "no engine answers in context '${context:-unknown}'" \
        "colima start (colima), or start Docker Desktop; list the contexts with: docker context ls"
      ;;
    esac
    return
  fi
  operating_system=$(docker_field OperatingSystem)
  case $context in
  colima*) engine=colima ;;
  desktop*) engine="Docker Desktop" ;;
  *) if [[ $operating_system == *"Docker Desktop"* ]]; then engine="Docker Desktop"; else engine="engine"; fi ;;
  esac
  report ok docker "$engine reachable through context ${context:-unknown}, server $version"
}

# gradle: the JVM-side checks, run by Gradle itself. `--continue` runs every independent check and
# reports each failure with the fix its task names. mise puts the pinned Node and formatter binaries
# on the build's PATH, the same way CI and the lefthook hooks run Gradle.
check_gradle() {
  if [[ $JAVA_OK != true ]]; then
    report warn gradle "skipped: ./gradlew doctor needs a Java runtime, see the java check" "mise install"
    return
  fi
  if [[ ${DOCTOR_SKIP_GRADLE:-} == 1 ]]; then
    report warn gradle "skipped: DOCTOR_SKIP_GRADLE=1 (the shell tests set it)"
    return
  fi
  local -a gradle=("$ROOT/gradlew")
  if has mise; then
    gradle=(mise x -- "$ROOT/gradlew")
  fi
  if (cd "$ROOT" && "${gradle[@]}" doctor --continue --console=plain); then
    report ok gradle "every JVM-side check passed"
  else
    report fail gradle "./gradlew doctor reported failures above" \
      "read the failing task's message; it names its fix, then run this script again"
  fi
}

parse_arguments() {
  local argument
  for argument in "$@"; do
    case $argument in
    --ci) CI_MODE=true ;;
    --help | -h)
      usage
      exit 0
      ;;
    *)
      echo "doctor.sh: unknown argument: $argument" >&2
      usage >&2
      exit 2
      ;;
    esac
  done
}

main() {
  parse_arguments "$@"
  check_mise
  check_tools
  check_java
  check_docker
  check_gradle
  printf 'summary: %d ok, %d warn, %d fail, %d skip\n' "$COUNT_OK" "$COUNT_WARN" "$COUNT_FAIL" "$COUNT_SKIP"
  [[ $COUNT_FAIL -eq 0 ]]
}

main "$@"

#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Diagnoses the development environment and says how to fix every problem (TOOL-03, D-27). It runs every
# check, prints each as ok, warn, fail or skip with the exact fix command, exits 1 when any check fails,
# and changes nothing: no install, update, start, pull or write. The fix lines are suggestions for you.
#
# Which checks live where (D-48, one implementation per check):
#   - This script runs only what must work before Gradle can start (mise and its tools, a Java runtime
#     for the Gradle wrapper, the Docker engine and its image store) and what Gradle does not own: the
#     Testcontainers settings, the pinned GSD installation (AIDEV-07), the global GSD defaults, the MCP
#     servers, the Platform token, colima, the Claude Code version, the project's Claude Code plugins (each
#     one enabled in `.claude/settings.json` has an install for this clone), the git version the governance
#     hooks need, the tracked `.claude` files, and the upstream-removal reminder (every docs/upstream record that
#     is fixed upstream while its workaround markers remain, with its removal criteria).
#   - Everything else runs as `./gradlew doctor`: the Node major (verifyNodeVersion), the formatter
#     binaries (verifyShfmt, verifyTaplo), the daemon JVM criteria (checkDaemonJvm) and Testcontainers
#     connectivity (testcontainersProbe). The last check below runs it, so each failure carries the
#     fix hint of the task that found it.
#
# Usage: doctor.sh [--ci] [--help]
#   --ci    skip the checks that only make sense on a developer machine (gsd, gsd-defaults, mcp,
#           platform-token, colima, claude-code, claude-plugins and git); CI runs this mode
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
# Set by check_docker: whether the engine answered, the name of the docker context in use and the engine's server version.
DOCKER_OK=false
DOCKER_CONTEXT=''
DOCKER_SERVER_VERSION=''
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

# local_only <check>: in --ci mode reports skip for a check that only makes sense on a developer machine and succeeds,
# so that the caller returns (`local_only gsd && return`). Outside --ci it fails and the check runs.
local_only() {
  if $CI_MODE; then
    report skip "$1" "local-only check, skipped by --ci"
    return 0
  fi
  return 1
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
  # A missing mise.toml is restored from git; one that exists but cannot be read keeps its content and needs its mode
  # changed. The first sed fails on either, which pipefail would turn into the end of the run.
  if [[ ! -e $ROOT/mise.toml ]]; then
    report fail mise "mise.toml cannot be read from the repository root, so its min_version cannot be checked" \
      "restore it: git restore mise.toml"
    return
  fi
  if ! minimum=$(sed -nE 's/^min_version[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$ROOT/mise.toml" 2>/dev/null |
    sed -n 1p); then
    report fail mise "mise.toml cannot be read from the repository root, so its min_version cannot be checked" \
      "chmod u+r mise.toml"
    return
  fi
  installed=$(mise --version 2>/dev/null | awk 'NR == 1 { print $1 }') || installed=''
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
  # The tool list must be an object whose values are arrays of objects, or the program below cannot run on it.
  # shellcheck disable=SC2016 # a jq program, not shell
  if ! total=$("$jq" -r 'if type == "object" and all(.[]; type == "array" and all(.[]; type == "object"))
    then [.[][]] | length else error("not a tool list") end' <<<"$json" 2>/dev/null) ||
    ! missing=$("$jq" -r '
      to_entries[] | .key as $tool | .value[]
      | select(((.installed // false) and (.active // false)) | not)
      | "\($tool)@\(.requested_version // .version // "?")"' <<<"$json" 2>/dev/null | paste -sd, - | sed 's/,/, /g'); then
    report fail tools "cannot read the tool list that 'mise ls --current --json' printed (not a JSON object of tool lists)" \
      "mise install" \
      "if the output is still not a JSON object of tool lists: brew upgrade mise, or: mise self-update"
    return
  fi
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
  DOCKER_CONTEXT=$context
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
  DOCKER_OK=true
  DOCKER_SERVER_VERSION=$version
  report ok docker "$engine reachable through context ${context:-unknown}, server $version"
}

# The lowest Docker engine that exports the Paketo noble run image on the containerd image store (docs/tooling.md,
# UP-0012). A requirement, not a workaround: the UP-0012 record is workaround-removed, so no UPSTREAM marker here.
readonly CONTAINERD_STORE_MIN_DOCKER=29.9.0

# image-store: which image store the engine uses. Both work: the classic store is the one on which the Testcontainers and
# Paketo setup was verified, and the containerd store needs Docker 29.9.0 or newer, because older engines cannot export
# the Paketo noble run image (docs/tooling.md, UP-0012). So below that, or on a version that cannot be compared, the
# check warns. A `-` or `+` suffix of the server version (29.10.0+azure-1, 29.9.0-rc.1) is dropped before comparing.
# The containerd store reports the driver `overlayfs` and a snapshotter entry in the driver status. The server version
# comes from check_docker, which only sets DOCKER_OK with a non-empty one.
check_image_store() {
  if [[ $DOCKER_OK != true ]]; then
    report skip image-store "the engine does not answer, see the docker check"
    return
  fi
  local driver status version=$DOCKER_SERVER_VERSION core lowest
  driver=$(docker_field Driver)
  status=$(docker info --format '{{json .DriverStatus}}' 2>/dev/null || true)
  if [[ $driver != overlayfs && $status != *io.containerd.snapshotter* ]]; then
    report ok image-store "the classic image store is active on Docker $version (driver ${driver:-unknown})"
    return
  fi
  core=${version%%[-+]*}
  if [[ ! $core =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    report warn image-store \
      "the containerd image store is active on Docker '$version', a version that cannot be compared with its minimum $CONTAINERD_STORE_MIN_DOCKER" \
      "upgrade the Docker engine to $CONTAINERD_STORE_MIN_DOCKER or newer (docker version shows it), see docs/tooling.md" \
      "or use the classic image store, colima: colima stop && colima start --edit, then set docker.features.containerd-snapshotter: false" \
      "or use the classic image store, Docker Desktop: Settings > General, turn off 'Use containerd for pulling and storing images'"
    return
  fi
  lowest=$(printf '%s\n%s\n' "$CONTAINERD_STORE_MIN_DOCKER" "$core" | sort -V | sed -n 1p)
  if [[ $lowest == "$CONTAINERD_STORE_MIN_DOCKER" ]]; then
    report ok image-store "the containerd image store is active on Docker $version (driver ${driver:-unknown}), which meets its minimum $CONTAINERD_STORE_MIN_DOCKER"
  else
    report warn image-store \
      "the containerd image store is active on Docker $version, which is older than $CONTAINERD_STORE_MIN_DOCKER and cannot export the Paketo noble run image (UP-0012)" \
      "upgrade the Docker engine to $CONTAINERD_STORE_MIN_DOCKER or newer (docker version shows it), see docs/tooling.md" \
      "or use the classic image store, colima: colima stop && colima start --edit, then set docker.features.containerd-snapshotter: false" \
      "or use the classic image store, Docker Desktop: Settings > General, turn off 'Use containerd for pulling and storing images'"
  fi
}

# The awk program that reads the facts of a colima.yaml that this check needs: the number of entries in
# `network.dns`. A comment line is ignored.
read -r -d '' COLIMA_AWK <<'AWK' || true
/^[[:space:]]*#/ { next }
/^[^[:space:]]/ { top = $0; sub(/:.*/, "", top); in_dns = 0; next }
top == "network" && /^  dns:/ {
  rest = $0
  sub(/^  dns:[[:space:]]*/, "", rest)
  if (rest ~ /^\[/) {
    gsub(/[][[:space:]]/, "", rest)
    if (rest != "") dns++
    in_dns = 0
  } else {
    in_dns = 1
  }
  next
}
top == "network" && in_dns && /^    -/ { dns++; next }
top == "network" && /^  [^[:space:]-]/ { in_dns = 0 }
END { printf "%d\n", dns }
AWK
readonly COLIMA_AWK

# UPSTREAM: UP-0011 a colima profile without explicit resolvers once stalled Go DNS lookups for about 10 s each
# (AAAA suspected); a clean profile did not reproduce it, so the cause is not established.
# colima (local only): the setting of the colima VM that the image pulls and the Testcontainers runs depend on: an
# explicit DNS list, the UP-0011 workaround kept until the cause is known. The image store is the image-store check's.
check_colima() {
  local_only colima && return
  case $DOCKER_CONTEXT in
  colima*) ;;
  *)
    report skip colima "the engine is not colima"
    return
    ;;
  esac
  local profile=${DOCKER_CONTEXT#colima} file dns
  profile=${profile#-}
  file=${HOME:-}/.colima/${profile:-default}/colima.yaml
  # A file that exists but cannot be read is the same finding as a missing one: awk would end the run under pipefail.
  if [[ ! -f $file ]] || ! dns=$(awk "$COLIMA_AWK" "$file" 2>/dev/null); then
    report warn colima "cannot read the colima configuration ~/.colima/${profile:-default}/colima.yaml" \
      "colima start --edit"
    return
  fi
  if [[ $dns -eq 0 ]]; then
    report warn colima "network.dns is empty, so the default DNS forwarder can stall image pulls" \
      "colima stop && colima start --dns 8.8.8.8 --dns 1.1.1.1 (or list servers under network.dns with: colima start --edit)"
  else
    report ok colima "network.dns lists $dns servers"
  fi
}

# testcontainers-config: how Testcontainers finds the engine and whether its Ryuk reaper runs. Only key names and the
# scheme of DOCKER_HOST are printed, never the values of the properties file. Real connectivity is the testcontainers
# check of ./gradlew doctor.
check_testcontainers_config() {
  local properties=${HOME:-}/.testcontainers.properties host_state keys='' keys_state ryuk_disabled=false
  if [[ -n ${DOCKER_HOST:-} ]]; then
    host_state="DOCKER_HOST set (${DOCKER_HOST%%://*})"
  else
    host_state="DOCKER_HOST unset"
  fi
  if [[ -f $properties ]]; then
    keys=$(grep -E '^[[:space:]]*(tc\.host|docker\.host)[[:space:]]*=' "$properties" |
      sed -E 's/^[[:space:]]*(tc\.host|docker\.host).*/\1/' | sort -u | paste -sd, - | sed 's/,/ and /' || true)
    if grep -Eq '^[[:space:]]*ryuk\.disabled[[:space:]]*=[[:space:]]*true' "$properties"; then
      ryuk_disabled=true
    fi
  fi
  if [[ ${TESTCONTAINERS_RYUK_DISABLED:-} == true ]]; then
    ryuk_disabled=true
  fi
  if [[ -n $keys ]]; then
    keys_state="$keys set"
  else
    keys_state="no tc.host or docker.host"
  fi
  if [[ $ryuk_disabled == true ]]; then
    report warn testcontainers-config "Ryuk is disabled, so containers of a crashed test run are never removed" \
      "unset TESTCONTAINERS_RYUK_DISABLED and remove ryuk.disabled from ~/.testcontainers.properties"
  elif [[ $DOCKER_CONTEXT == colima* && -z ${DOCKER_HOST:-} && -z $keys ]]; then
    report warn testcontainers-config "colima: no DOCKER_HOST, tc.host or docker.host, so Testcontainers cannot find the engine" \
      "add the line tc.host=unix:///var/run/docker.sock to ~/.testcontainers.properties" \
      "or export DOCKER_HOST=unix://\$HOME/.colima/default/docker.sock and TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock"
  else
    report ok testcontainers-config "$host_state, $keys_state, Ryuk enabled"
  fi
}

# The files under .claude/ of a GSD installation that its manifest does not list. The installer leaves them out.
readonly GSD_UNLISTED_ALLOWED=(hooks/package.json scripts/changeset/README.md)
# The directories under .claude/ that GSD owns. Anything else there (skills, settings) is not GSD's.
readonly GSD_DIRECTORIES=(gsd-core agents commands hooks scripts)

# gsd (local only, AIDEV-07): the pin is a plain version (validated before any command is built from it), the installed
# GSD is the pinned version and every file is byte for byte what the manifest records. A modified hook changes what
# agents do, so it is called out as a hook. Files that the manifest does not list under GSD's directories are reported as
# unexpected. A manifest that cannot be read is reported with the same reinstall fix. Nothing is repaired: the fix
# reinstalls the pinned version after you have looked at the difference.
check_gsd() {
  local_only gsd && return
  local claude=$ROOT/.claude pinned_file=$ROOT/governance/gsd-version manifest pinned installed jq install_fix
  manifest=$claude/gsd-file-manifest.json
  if [[ ! -f $pinned_file ]]; then
    report fail gsd "governance/gsd-version is missing" "write the version of the installed GSD into governance/gsd-version"
    return
  fi
  if ! pinned=$(sed -n 1p "$pinned_file" 2>/dev/null | tr -d '[:space:]'); then
    report fail gsd "governance/gsd-version cannot be read" "chmod u+r governance/gsd-version"
    return
  fi
  # The pin goes into a command that a maintainer copies and runs, so it must be a plain version (the shape that the
  # Renovate custom manager matches). The message does not repeat it: it can hold anything a file can.
  if [[ ! $pinned =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    report fail gsd "governance/gsd-version must hold one version such as 1.2.3" \
      "write the version of the installed GSD into governance/gsd-version"
    return
  fi
  install_fix="npx -y @opengsd/gsd-core@$pinned --claude --local"
  if [[ ! -f $manifest ]]; then
    report fail gsd ".claude/gsd-file-manifest.json is missing, so GSD is not installed" "$install_fix"
    return
  fi
  if ! jq=$(find_jq); then
    report fail gsd "jq is needed to read the GSD manifest and is not installed" "mise install"
    return
  fi
  local -a hasher
  if has sha256sum; then
    hasher=(sha256sum)
  elif has shasum; then
    hasher=(shasum -a 256)
  else
    report fail gsd "neither sha256sum nor shasum is installed, so the files cannot be verified" "install coreutils or perl"
    return
  fi

  # One guarded read that also checks the shape: the manifest is an object with an object of files. That is what
  # makes the later reads of `.files` safe, which would otherwise end the run under set -e.
  if ! installed=$("$jq" -r 'if type == "object" and (.files | type) == "object" then .version // empty
    else error("not a GSD manifest") end' "$manifest" 2>/dev/null); then
    report fail gsd ".claude/gsd-file-manifest.json cannot be read as a GSD manifest (not JSON, or no object of files)" \
      "$install_fix"
    return
  fi
  if [[ $installed != "$pinned" ]]; then
    report fail gsd "installed GSD ${installed:-unknown} differs from governance/gsd-version $pinned" \
      "$install_fix"
  fi

  # `hash  path` lines of what the manifest expects and of what is on disk.
  local expected actual listed on_disk problems total
  expected=$("$jq" -r '.files | to_entries[] | "\(.value)  \(.key)"' "$manifest")
  actual=$(cd "$claude" && "$jq" -r '.files | keys[]' "$manifest" |
    while IFS= read -r path; do
      if [[ -f $path ]]; then printf '%s\0' "$path"; fi
    done | xargs -0 "${hasher[@]}" 2>/dev/null || true)
  problems=$(awk '
    function path_of(line) { return substr(line, index(line, "  ") + 2) }
    function hash_of(line) { return substr(line, 1, index(line, "  ") - 1) }
    FNR == NR { want[path_of($0)] = hash_of($0); next }
    { got[path_of($0)] = hash_of($0) }
    END {
      for (p in want) {
        if (!(p in got)) print "missing: " p
        else if (got[p] != want[p]) print (p ~ /^hooks\// ? "modified hook: " : "modified: ") p
      }
    }' <(printf '%s\n' "$expected") <(printf '%s\n' "$actual") | sort)

  listed=$("$jq" -r '.files | keys[]' "$manifest" | sort)
  on_disk=$(
    cd "$claude"
    for directory in "${GSD_DIRECTORIES[@]}"; do
      if [[ -d $directory ]]; then find "$directory" -type f; fi
    done | sort
  )
  local unexpected
  unexpected=$(comm -13 <(printf '%s\n' "$listed") <(printf '%s\n' "$on_disk") |
    grep -vxF -f <(printf '%s\n' "${GSD_UNLISTED_ALLOWED[@]}") | grep -v -e '^$' -e '/\.DS_Store$' |
    sed 's/^/unexpected: /' || true)
  problems=$(printf '%s\n%s\n' "$problems" "$unexpected" | grep -v '^$' || true)

  if [[ -z $problems ]]; then
    if [[ $installed == "$pinned" ]]; then
      total=$("$jq" -r '.files | length' "$manifest")
      report ok gsd "version $installed, $total files match the manifest, no unexpected files"
    fi
    return
  fi
  local line count=0 shown=0 limit=25
  count=$(printf '%s\n' "$problems" | wc -l | tr -d '[:space:]')
  while IFS= read -r line; do
    shown=$((shown + 1))
    if [[ $shown -le $limit ]]; then
      if [[ $shown -eq $count || $shown -eq $limit ]]; then
        report fail gsd "$line" "$install_fix"
      else
        report fail gsd "$line"
      fi
    fi
  done <<<"$problems"
  if [[ $count -gt $limit ]]; then
    report fail gsd "and $((count - limit)) more problems"
  fi
}

# gsd-defaults (local only): ~/.gsd/defaults.json is merged into GSD's configuration by some resolvers, so a key in it
# can change how GSD behaves in this project. Only key names are listed, never values.
check_gsd_defaults() {
  local_only gsd-defaults && return
  local file=${HOME:-}/.gsd/defaults.json name="\$HOME/.gsd/defaults.json" keys jq
  if [[ ! -f $file ]]; then
    report ok gsd-defaults "no $name"
    return
  fi
  if ! jq=$(find_jq); then
    report warn gsd-defaults "jq is needed to read $name and is not installed" "mise install"
    return
  fi
  if ! keys=$("$jq" -r 'if type == "object" then keys | join(", ") else error("not an object") end' "$file" 2>/dev/null); then
    report warn gsd-defaults "$name is not a JSON object" "repair the file or remove it"
    return
  fi
  if [[ -z $keys ]]; then
    report ok gsd-defaults "$name sets no keys"
  else
    report warn gsd-defaults "$name sets keys that GSD can merge into this project's configuration: $keys (names only)" \
      "review $name and delete the keys this project must not inherit"
  fi
}

# mcp (local only, AIDEV-04): the binary of every server in .mcp.json resolves. A server started through
# `mise x -- <binary>` is resolved with `mise which`, any other command through PATH. A server without a `command` (an
# HTTP or SSE server) must have a url and then has no binary to resolve; its url can carry a credential and never leaves
# jq. A server with neither a command nor a url can never start: a defect of the committed .mcp.json, so the check fails,
# while an unresolved binary of a well-formed server is a warning that `mise install` fixes.
check_mcp() {
  local_only mcp && return
  local file=$ROOT/.mcp.json jq servers kind name binary total=0 remote=0 unresolved='' missing=''
  if [[ ! -f $file ]]; then
    report ok mcp "no .mcp.json, so no project MCP servers"
    return
  fi
  if ! jq=$(find_jq); then
    report warn mcp "jq is needed to read .mcp.json and is not installed" "mise install"
    return
  fi
  # The shape check: an object whose `mcpServers`, when present, is an object of server objects. Anything else would end
  # the run under set -e (or, for an array of servers, be counted as zero servers).
  # Every server is one line of three tab-separated fields: its kind (mise, path, none or missing), its name and the
  # binary to resolve. `none` is a server with a url and no command, `missing` one with neither. No field is ever empty,
  # because `read` with a tab as separator would drop an empty field and shift the ones behind it; a server that has
  # nothing to resolve gets `-` as its binary, and the url never leaves jq.
  # shellcheck disable=SC2016 # a jq program, not shell
  if ! servers=$("$jq" -r '
    def text: if type == "string" and . != "" then . else null end;
    if type == "object" and ((.mcpServers // {}) | type == "object" and all(.[]; type == "object")) then
      .mcpServers // {} | to_entries[]
      | ((.value.args // []) | if type == "array" then . else [] end) as $args
      | (.value.command | text) as $command
      | (.key | text // "(unnamed)") as $name
      | if $command == null then (if (.value.url | text) != null then ["none", $name, "-"] else ["missing", $name, "-"] end)
        elif $command == "mise" and ($args[0] == "x" or $args[0] == "exec") and $args[1] == "--"
            and ($args[2] | text) != null then ["mise", $name, $args[2]]
        else ["path", $name, $command] end | @tsv
    else error("not MCP server configuration") end' "$file" 2>/dev/null); then
    report warn mcp ".mcp.json cannot be read as MCP server configuration (not JSON, or not an object of server objects)" \
      "repair .mcp.json, or restore the committed version: git restore .mcp.json"
    return
  fi
  while IFS=$'\t' read -r kind name binary; do
    [[ -n $kind ]] || continue
    total=$((total + 1))
    case $kind in
    none) remote=$((remote + 1)) ;;
    missing) missing+="${missing:+, }$name" ;;
    mise)
      if ! has mise || ! (cd "$ROOT" && mise which "$binary" >/dev/null 2>&1); then
        unresolved+="${unresolved:+, }$name ($binary)"
      fi
      ;;
    *)
      if ! has "$binary"; then
        unresolved+="${unresolved:+, }$name ($binary)"
      fi
      ;;
    esac
  done <<<"$servers"
  if [[ -n $missing ]]; then
    local detail="servers in .mcp.json with neither a command nor a url: $missing"
    local restore="give each of them a command, or a url for an HTTP or SSE server, or restore the committed version: git restore .mcp.json"
    if [[ -n $unresolved ]]; then
      report fail mcp "$detail; servers whose binary cannot be resolved: $unresolved" "$restore" "mise install"
    else
      report fail mcp "$detail" "$restore"
    fi
  elif [[ -n $unresolved ]]; then
    report warn mcp "servers in .mcp.json whose binary cannot be resolved: $unresolved" "mise install"
  elif [[ $remote -gt 0 ]]; then
    report ok mcp "all $total servers in .mcp.json resolve through mise or PATH, or have no command to resolve ($remote)"
  else
    report ok mcp "all $total servers in .mcp.json resolve through mise or PATH"
  fi
}

# The awk program that classifies the Platform token in a dotenv file as `set`, `empty` or `absent`. It prints that one
# word and nothing else, so the value can never reach the output. The last assignment of the key wins, as in dotenv.
read -r -d '' TOKEN_AWK <<'AWK' || true
/^[[:space:]]*(export[[:space:]]+)?AXONIQ_PLATFORM_AUTHENTICATION[[:space:]]*=/ {
  value = substr($0, index($0, "=") + 1)
  sub(/^[[:space:]]+/, "", value)
  sub(/[[:space:]]+$/, "", value)
  state = (value == "" || value == "\"\"" || value == "\047\047") ? "empty" : "set"
}
END { print (state == "" ? "absent" : state) }
AWK
readonly TOKEN_AWK

# UPSTREAM: UP-0006 Axon Server treats an empty token as configured and its endpoints die; this check is the workaround.
# platform-token (local only, D-47): an empty definition makes Axon Server start in a zombie state, so it fails; no
# definition only means the 12-hour window, so it warns. Only the state is tested, never the value.
check_platform_token() {
  local_only platform-token && return
  local dotenv=$ROOT/.env state=absent
  if [[ -f $dotenv ]] && ! state=$(awk "$TOKEN_AWK" "$dotenv" 2>/dev/null); then
    report warn platform-token ".env exists but cannot be read, so the state of AXONIQ_PLATFORM_AUTHENTICATION is unknown" \
      "chmod u+r .env"
    return
  fi
  case $state in
  set) report ok platform-token "AXONIQ_PLATFORM_AUTHENTICATION is set in .env (the value is not shown)" ;;
  empty)
    report fail platform-token \
      "AXONIQ_PLATFORM_AUTHENTICATION is defined but empty, which leaves Axon Server running with dead endpoints" \
      "remove the line or set <environment-id>:<token>"
    ;;
  *)
    report warn platform-token \
      "AXONIQ_PLATFORM_AUTHENTICATION is not set, so Axon Server stops itself about 12 hours after it starts" \
      "to lift the limit, set AXONIQ_PLATFORM_AUTHENTICATION=<environment-id>:<token> in .env (see .env.example)"
    ;;
  esac
}

# The only place the minimum Claude Code version lives: marketplaces pinned by commit SHA need it.
readonly CLAUDE_CODE_MIN_VERSION=2.1.324

# claude-code (local only, AIDEV-04): the installed Claude Code can pin marketplaces by commit SHA.
check_claude_code() {
  local_only claude-code && return
  if ! has claude; then
    report warn claude-code "the claude CLI is not on PATH" "brew install --cask claude-code@latest"
    return
  fi
  local installed lowest
  installed=$(claude --version 2>/dev/null | awk 'NR == 1 { print $1 }') || installed=''
  if [[ -z $installed ]]; then
    report warn claude-code "cannot read the version from 'claude --version'" "brew upgrade --cask claude-code@latest"
    return
  fi
  lowest=$(printf '%s\n%s\n' "$CLAUDE_CODE_MIN_VERSION" "$installed" | sort -V | sed -n 1p)
  if [[ $lowest == "$CLAUDE_CODE_MIN_VERSION" ]]; then
    report ok claude-code "$installed meets the minimum $CLAUDE_CODE_MIN_VERSION for marketplaces pinned by commit SHA"
  else
    report warn claude-code "$installed is older than $CLAUDE_CODE_MIN_VERSION, which marketplaces pinned by commit SHA need" \
      "brew upgrade --cask claude-code@latest (the stable cask lags behind; claude-code@latest has the minimum)"
  fi
}

# resolve_dir <path>: the physical path of an existing directory (symlinks resolved), else the text as it was written.
resolve_dir() {
  if [[ -d $1 ]]; then
    (cd "$1" && pwd -P)
  else
    printf '%s\n' "$1"
  fi
}

# claude-plugins (local only, G-02-2): every plugin that .claude/settings.json enables has an install record usable from
# this checkout. The settings only enable the plugins and register their marketplaces: Claude Code never installs a plugin
# that only repository settings enable when its marketplace is a remote source, so each contributor installs both once per
# clone with `claude plugin install <id> --scope project`. A project-scope record is keyed by the path of the checkout it was
# made in, and a session in a linked git worktree resolves to the record of the main checkout (observed 2026-11-25, see
# docs/tooling.md), so the paths that count are this checkout and the main worktree of its repository. A user-scope record
# counts everywhere. The check reads Claude Code's records file and runs no `claude plugin` command: those can write
# Claude Code's state, and doctor changes nothing. Only plugin ids and fixed messages are printed, never a record field.
check_claude_plugins() {
  local_only claude-plugins && return
  if ! has claude; then
    report skip claude-plugins "the claude CLI is not on PATH, see the claude-code check"
    return
  fi
  local jq settings=$ROOT/.claude/settings.json
  if ! jq=$(find_jq); then
    report warn claude-plugins "jq is needed to read the plugin install records and is not installed" "mise install"
    return
  fi
  local enabled
  if ! enabled=$("$jq" -r '(.enabledPlugins // {}) | if type == "object" then to_entries[] | select(.value == true) | .key
    else empty end' "$settings" 2>/dev/null | sort); then
    report warn claude-plugins ".claude/settings.json cannot be read as JSON, so the enabled plugins are unknown" \
      "restore it: git restore .claude/settings.json"
    return
  fi
  if [[ -z $enabled ]]; then
    report ok claude-plugins "no plugin is enabled in .claude/settings.json"
    return
  fi

  # An id is printed and put into a command that a contributor copies, so it must be a plain <plugin>@<marketplace>.
  # A key that is not is counted and never echoed: a committed key can carry terminal control characters.
  local id malformed=0 ids=()
  while IFS= read -r id; do
    if [[ $id =~ ^[A-Za-z0-9][A-Za-z0-9._-]*@[A-Za-z0-9][A-Za-z0-9._-]*$ ]]; then
      ids+=("$id")
    else
      malformed=$((malformed + 1))
    fi
  done <<<"$enabled"
  if ((malformed > 0)); then
    local noun=entry verb=is
    if ((malformed > 1)); then
      noun=entries
      verb=are
    fi
    report fail claude-plugins \
      "$malformed enabledPlugins $noun in .claude/settings.json $verb not of the form <plugin>@<marketplace>" \
      "correct the key in .claude/settings.json"
    return
  fi

  local records=${CLAUDE_CONFIG_DIR:-$HOME/.claude}/plugins/installed_plugins.json
  # An absent file is a Claude Code that has installed nothing. One that cannot be read, is not JSON or is not format
  # version 2 is Claude Code's own format changing, which is one warning for the whole check.
  if [[ -e $records ]] &&
    ! "$jq" -e '.version == 2 and (.plugins | type) == "object"' "$records" >/dev/null 2>&1; then
    report warn claude-plugins "the install records of Claude Code cannot be read, so the installs are unknown" \
      "claude plugin list --json shows the install state"
    return
  fi

  local -a candidates=("$(resolve_dir "$ROOT")")
  local common
  if has git && common=$(git -C "$ROOT" rev-parse --path-format=absolute --git-common-dir 2>/dev/null) &&
    [[ $common == */.git ]]; then
    candidates+=("$(resolve_dir "${common%/.git}")")
  fi

  local scope path candidate installed missing=0
  for id in "${ids[@]}"; do
    installed=false
    if [[ -e $records ]]; then
      # shellcheck disable=SC2016 # $id in the jq program is a jq variable bound by --arg
      while IFS=$'\t' read -r scope path; do
        if [[ $scope == user ]]; then
          installed=true
        elif [[ $scope == project || $scope == local ]]; then
          path=$(resolve_dir "$path")
          for candidate in "${candidates[@]}"; do
            if [[ $path == "$candidate" ]]; then installed=true; fi
          done
        fi
      done < <("$jq" -r --arg id "$id" '(.plugins[$id] // []) | .[] | [.scope, (.projectPath // "")] | @tsv' "$records")
    fi
    if ! $installed; then
      missing=$((missing + 1))
      report warn claude-plugins "$id is enabled in .claude/settings.json but not installed for this clone" \
        "claude plugin install $id --scope project (once per clone, in the main checkout)" \
        "then run /reload-plugins in Claude Code"
    fi
  done
  if ((missing == 0)); then
    local list phrase="enabled plugins are"
    list=$(printf '%s, ' "${ids[@]}")
    if ((${#ids[@]} == 1)); then
      phrase="enabled plugin is"
    fi
    report ok claude-plugins "${#ids[@]} $phrase installed for this clone (${list%, })"
  fi
}

# The only place the minimum git version lives. It is the git release the governance hooks need (ADR-0015):
# scripts/governance/ask-governed-edit.sh calls `git worktree list --porcelain -z` (git 2.36) and
# `git rev-parse --path-format=absolute` (git 2.31). On an older git the edit hook fails closed and asks for every
# edit. docs/tooling.md points here instead of repeating the number.
readonly GIT_MIN_VERSION=2.36.0

# git (local only, AIDEV-08): the installed git supports the calls of the governance hooks. A missing git, an older
# git and a version that cannot be read all fail, because the hooks then ask for every edit. A `git` that fails
# outright, like the macOS /usr/bin/git stub without the Command Line Tools, also reads as a version that cannot
# be read.
check_git() {
  local_only git && return
  if ! has git; then
    report fail git "the git CLI is not on PATH" \
      "brew install git (or install git with your package manager), then check that it comes first on PATH"
    return
  fi
  local installed lowest
  installed=$(git --version 2>/dev/null | awk 'NR == 1 && $1 == "git" && $2 == "version" { print $3 }') || installed=''
  if [[ ! $installed =~ ^[0-9]+\.[0-9]+ ]]; then
    report fail git "cannot read the version from 'git --version'" \
      "brew install git (Homebrew's git must come before /usr/bin on PATH), or upgrade git with your package manager" \
      "xcode-select --install (on macOS, /usr/bin/git is a stub until the Xcode Command Line Tools are installed)"
    return
  fi
  lowest=$(printf '%s\n%s\n' "$GIT_MIN_VERSION" "$installed" | sort -V | sed -n 1p)
  if [[ $lowest == "$GIT_MIN_VERSION" ]]; then
    report ok git "$installed meets the minimum $GIT_MIN_VERSION that the governance hooks need"
  else
    report fail git "$installed is older than $GIT_MIN_VERSION, which the governance hooks need (ADR-0015)" \
      "brew install git (Homebrew's git must come before /usr/bin on PATH), or upgrade git with your package manager"
  fi
}

# claude-tracking (AIDEV-03, also in --ci): the only tracked content under .claude/ is the project-owned settings.json
# and CLAUDE.md and the spike-findings skills (D-39). The GSD installation and local settings stay untracked.
check_claude_tracking() {
  if ! has git || ! git -C "$ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    report skip claude-tracking "the repository is not a git work tree"
    return
  fi
  local tracked unexpected path count
  tracked=$(git -C "$ROOT" ls-files -- .claude)
  unexpected=$(printf '%s\n' "$tracked" |
    grep -vE '^$|^\.claude/(settings\.json|CLAUDE\.md|skills/spike-findings-[^/]+/.*)$' || true)
  if [[ -z $unexpected ]]; then
    count=$(printf '%s\n' "$tracked" | grep -c . || true)
    report ok claude-tracking "only project-owned files are tracked under .claude ($count files)"
    return
  fi
  while IFS= read -r path; do
    report fail claude-tracking "$path is tracked, but only settings.json, CLAUDE.md and skills/spike-findings-* may be (AIDEV-03)" \
      "git rm --cached $path"
  done <<<"$unexpected"
}

# The word that opens a workaround marker. It is kept in a variable because written out after a comment opener it would
# itself be a marker, and `checkUpstreamRecords` would demand a record for it.
readonly MARKER_WORD=UPSTREAM

# The files that can hold a workaround marker, as git pathspecs: the scope of the `checkUpstreamRecords` Gradle task
# (the upstream record check of build-logic), limited to the files git tracks.
readonly -a MARKER_PATHSPECS=(
  '*.kt' '*.kts' '*.sh' '*.yaml' '*.yml' '*.toml' '*.json' '*.json5' '*.md' '*.properties' '*.xml' '.env.example' NOTICE
  ':(exclude)docs/upstream' ':(exclude)docs/templates' ':(exclude).planning' ':(exclude).claude'
  ':(exclude)build-logic/src/functionalTest'
)

# front_matter_value <file> <key>: the value of a `key: value` line of the front matter, between the first two `---`
# lines, without its quotes or a carriage return. Prints nothing when there is no such line.
front_matter_value() {
  awk -v key="$2" '
    { sub(/\r$/, "") }
    NR == 1 { if ($0 == "---") { inside = 1; next } else { exit } }
    inside && $0 == "---" { exit }
    inside && index($0, key ": ") == 1 {
      value = substr($0, length(key) + 3)
      gsub(/^"|"$/, "", value)
      print value
      exit
    }' "$1"
}

# removal_criteria <file>: the non-blank lines under `## Removal criteria` up to the next heading, joined into one line.
removal_criteria() {
  awk '
    { sub(/\r$/, "") }
    /^## Removal criteria[[:space:]]*$/ { inside = 1; next }
    inside && /^#/ { exit }
    inside && $0 !~ /^[[:space:]]*$/ { text = text (text == "" ? "" : " ") $0 }
    END { print text }' "$1"
}

# workaround_markers <id>: the `path:line` of every tracked workaround marker of the record, comma-separated; nothing
# when there is none. The pattern mirrors the marker rule of the upstream record check (a comment opener, the marker
# word, a colon and the ID as a whole word); `checkUpstreamRecords` stays the authority on marker consistency.
workaround_markers() {
  local pattern hits
  pattern="(^|[[:space:]])(//|/\\*|\\*|#|<!--|--)[[:space:]]*$MARKER_WORD: $1([^0-9A-Za-z_]|\$)"
  hits=$(git -C "$ROOT" grep -n -E -e "$pattern" -- "${MARKER_PATHSPECS[@]}" || true)
  if [[ -n $hits ]]; then
    printf '%s\n' "$hits" | cut -d: -f1,2 | paste -sd, - | sed 's/,/, /g'
  fi
}

# upstream-removal (also in --ci): every docs/upstream record whose status is fixed-upstream and whose workaround
# markers still exist in a tracked file is listed with its removal criteria, whatever its product. The dependency
# update advisory only matches records whose product has a pinned key, so it cannot surface these. A warning never
# fails the script. Record text is collapsed to one line and printed after a fixed prefix, so a record can never
# start a GitHub workflow command in the CI log.
check_upstream_removal() {
  if ! has git || ! git -C "$ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    report skip upstream-removal "not a git checkout, so the markers cannot be listed"
    return
  fi
  local record id locations warned=false
  for record in "$ROOT"/docs/upstream/[0-9][0-9][0-9][0-9]-*.md; do
    [[ -f $record && $(front_matter_value "$record" status) == fixed-upstream ]] || continue
    id=$(front_matter_value "$record" id)
    [[ $id =~ ^UP-[0-9]{4}$ ]] || continue
    locations=$(workaround_markers "$id")
    [[ -n $locations ]] || continue
    warned=true
    report warn upstream-removal "$id is fixed upstream, but its workaround markers remain: $locations" \
      "removal criteria (${record#"$ROOT"/}): $(removal_criteria "$record")" \
      "when they hold, follow How to verify removal in that record, then remove the markers and set its status to workaround-removed"
  done
  if [[ $warned == false ]]; then
    report ok upstream-removal "no fixed-upstream record has workaround markers left"
  fi
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
  check_image_store
  check_colima
  check_testcontainers_config
  check_gsd
  check_gsd_defaults
  check_mcp
  check_platform_token
  check_claude_code
  check_claude_plugins
  check_git
  check_claude_tracking
  check_upstream_removal
  check_gradle
  printf 'summary: %d ok, %d warn, %d fail, %d skip\n' "$COUNT_OK" "$COUNT_WARN" "$COUNT_FAIL" "$COUNT_SKIP"
  [[ $COUNT_FAIL -eq 0 ]]
}

main "$@"

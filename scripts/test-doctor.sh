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
REAL_GIT=$(command -v git)

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

# The real tools doctor.sh may call. They are linked into one directory so that PATH can be restricted to it:
# a `java` or `docker` that the machine has must never leak into a case that expects it to be absent.
SYSBIN="$TMP/sysbin"
mkdir -p "$SYSBIN"
for tool in bash env awk sed sort grep cat dirname basename jq paste tr head tail cut find git shasum sha256sum \
  comm xargs wc uname; do
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

  # `mise-version-fails` makes `mise --version` exit 1 without a word.
  cat >"$dir/bin/mise" <<'FAKE'
#!/usr/bin/env bash
case "$*" in
--version)
  [[ -e $FAKE_DIR/mise-version-fails ]] && exit 1
  echo "2026.11.12 macos-arm64 (2026-11-21)"
  ;;
"ls --current --json") cat "$FAKE_DIR/mise-tools.json" ;;
"which jq") command -v jq ;;
"which "*)
  # The binaries mise can resolve are listed, one per line, in `mise-binaries`.
  grep -qxF -- "$2" "$FAKE_DIR/mise-binaries" && echo "/fake/mise/bin/$2"
  ;;
*) exit 1 ;;
esac
FAKE

  cat >"$dir/bin/java" <<'FAKE'
#!/usr/bin/env bash
exit 0
FAKE

  # The context comes from `docker-context` and the server version from `docker-server-version`; `docker-down` makes
  # `docker info` fail like a stopped engine.
  echo colima >"$dir/docker-context"
  echo "29.0.0" >"$dir/docker-server-version"
  cat >"$dir/bin/docker" <<'FAKE'
#!/usr/bin/env bash
case "$*" in
"context show") cat "$FAKE_DIR/docker-context" ;;
"info --format {{.ServerVersion}}")
  [[ -e $FAKE_DIR/docker-down ]] && exit 1
  cat "$FAKE_DIR/docker-server-version"
  ;;
"info --format {{.OperatingSystem}}") echo "Ubuntu 24.04" ;;
"info --format {{.Driver}}") cat "$FAKE_DIR/docker-driver" ;;
"info --format {{json .DriverStatus}}") cat "$FAKE_DIR/docker-driver-status" ;;
*) exit 1 ;;
esac
FAKE
  echo overlay2 >"$dir/docker-driver"
  echo '[["Backing Filesystem","extfs"]]' >"$dir/docker-driver-status"

  # `claude --version` prints the version in `claude-version`; `claude-fails` makes every call exit 1.
  echo "2.1.330" >"$dir/claude-version"
  cat >"$dir/bin/claude" <<'FAKE'
#!/usr/bin/env bash
[[ -e $FAKE_DIR/claude-fails ]] && exit 1
echo "$(cat "$FAKE_DIR/claude-version") (Claude Code)"
FAKE
  # `git --version` prints the line in `git-version`; every other call goes to the real git (the tracking and
  # upstream-removal checks run `git -C ...`), whose path is kept in `real-git`. `git-broken` makes every call
  # fail like the macOS `/usr/bin/git` shim when the Xcode Command Line Tools are missing.
  echo "git version 2.47.1 (Apple Git-154)" >"$dir/git-version"
  echo "$REAL_GIT" >"$dir/real-git"
  cat >"$dir/bin/git" <<'FAKE'
#!/usr/bin/env bash
if [[ -e $FAKE_DIR/git-broken ]]; then
  echo "xcode-select: note: No developer tools were found, requesting install." >&2
  exit 1
fi
if [[ ${1:-} == --version ]]; then
  cat "$FAKE_DIR/git-version"
  exit 0
fi
exec "$(cat "$FAKE_DIR/real-git")" "$@"
FAKE
  chmod +x "$dir/bin/mise" "$dir/bin/java" "$dir/bin/docker" "$dir/bin/claude" "$dir/bin/git"
  printf 'jq\ncontext7-mcp\nplaywright-mcp\n' >"$dir/mise-binaries"
  make_local_state "$dir"
}

# make_local_state <dir>: everything the local-only checks read, in a healthy state: a GSD installation that matches
# its manifest, the pinned version, `.mcp.json`, a `.env` with a token, a colima configuration, Testcontainers
# settings, install records for the two plugins that `.claude/settings.json` enables, and a git repository that tracks
# only the allowed `.claude` files.
make_local_state() {
  local dir=$1 repo=$1/repo
  mkdir -p "$repo/governance" "$repo/.claude/hooks" "$repo/.claude/gsd-core" "$repo/.claude/agents" \
    "$repo/.claude/scripts/changeset" "$dir/home/.colima/default"
  echo "guard" >"$repo/.claude/hooks/gsd-guard.js"
  echo "workflow" >"$repo/.claude/gsd-core/workflow.md"
  echo "executor" >"$repo/.claude/agents/gsd-executor.md"
  # Two files the installer leaves out of its manifest.
  echo "{}" >"$repo/.claude/hooks/package.json"
  echo "changeset" >"$repo/.claude/scripts/changeset/README.md"
  write_manifest "$dir" 1.22.0
  printf '1.22.0\n' >"$repo/governance/gsd-version"

  printf '{"mcpServers": {"context7": {"command": "mise", "args": ["x", "--", "context7-mcp"]},
    "playwright": {"command": "mise", "args": ["x", "--", "playwright-mcp", "--isolated"]}}}\n' >"$repo/.mcp.json"
  printf 'AXONIQ_PLATFORM_AUTHENTICATION=secret-sentinel-value\n' >"$repo/.env"

  printf 'network:\n  dns:\n    - 100.64.0.1\n    - 8.8.4.4\n  dnsHosts:\n    host.docker.internal: host.lima.internal\ndocker:\n  features:\n    containerd-snapshotter: false\n' \
    >"$dir/home/.colima/default/colima.yaml"
  printf 'tc.host=unix:///var/run/docker.sock\n' >"$dir/home/.testcontainers.properties"

  git -C "$repo" init -q
  printf '{"enabledPlugins": {"axoniq-app-development@axoniq": true, "gradle-skills@gradle-skills": true}}\n' \
    >"$repo/.claude/settings.json"
  write_install_records "$dir/home/.claude" project "$repo" axoniq-app-development@axoniq gradle-skills@gradle-skills
  echo '# project' >"$repo/.claude/CLAUDE.md"
  git -C "$repo" add .claude/settings.json .claude/CLAUDE.md
}

# install_records_json <scope> <projectPath> <plugin id>...: prints Claude Code's `installed_plugins.json` (format
# version 2) with one record per id. A `user` record carries no projectPath, as in Claude Code's own file.
install_records_json() {
  local scope=$1 path=$2
  shift 2
  printf '%s\n' "$@" | jq -Rn --arg scope "$scope" --arg path "$path" \
    '{version: 2, plugins: ([inputs | {(.): [{scope: $scope, installPath: "/fake/plugin-cache", version: "1.0.0",
      gitCommitSha: "0123456789abcdef"} + (if $scope == "user" then {} else {projectPath: $path} end)]}] | add // {})}'
}

# write_install_records <claude config dir> <scope> <projectPath> <plugin id>...: writes the install records file
# under the configuration directory, replacing the one that is there.
write_install_records() {
  local config=$1
  shift
  mkdir -p "$config/plugins"
  install_records_json "$@" >"$config/plugins/installed_plugins.json"
}

# add_install_records <claude config dir> <scope> <projectPath> <plugin id>...: adds records to the existing file.
add_install_records() {
  local file=$1/plugins/installed_plugins.json
  shift
  jq -s '{version: 2, plugins: (.[0].plugins + .[1].plugins)}' "$file" <(install_records_json "$@") >"$file.new"
  mv "$file.new" "$file"
}

# write_manifest <dir> <version>: writes `.claude/gsd-file-manifest.json` over the listed GSD files as they are now.
write_manifest() {
  local claude=$1/repo/.claude file
  for file in hooks/gsd-guard.js gsd-core/workflow.md agents/gsd-executor.md; do
    printf '%s  %s\n' "$(shasum -a 256 <"$claude/$file" | cut -d' ' -f1)" "$file"
  done | jq -Rn --arg version "$2" \
    '{version: $version, manifestVersion: 2, files: ([inputs | split("  ") | {(.[1]): .[0]}] | add)}' \
    >"$claude/gsd-file-manifest.json"
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
  (cd "$1" && find . -path ./repo/.git -prune -o -type f -print0 | sort -z | xargs -0 shasum -a 256 | shasum -a 256)
}

n=0
fresh() {
  n=$((n + 1))
  ENV="$TMP/env$n"
  make_env "$ENV"
  EXTRA_ENV=''
}

# make_unreadable <file>: sets the mode of the file to 000. Succeeds only when that really makes it unreadable: root
# reads a mode-000 file, so there the mode is restored and the helper fails, and the case prints a skip line.
make_unreadable() {
  chmod 000 "$1"
  if cat "$1" >/dev/null 2>&1; then
    chmod 644 "$1"
    return 1
  fi
}

# skip_unreadable <case name>: the skip line of a case that cannot make a file unreadable. It is not counted.
skip_unreadable() {
  echo "skip - $1: root can read a mode-000 file"
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

fresh
touch "$ENV/mise-version-fails"
run_doctor "$ENV"
assert_code "mise --version fails" 1
assert_out "mise --version fails" "^fail  mise: cannot read the installed version from 'mise --version'"
assert_out "mise --version fails" "^summary: "

# A tool list that is not a JSON object of tool lists is reported (the review's WR-01 class: jq died under set -e).
for list in 'not json' '{"jq": {"version": "1.8.2"}}'; do
  fresh
  printf '%s\n' "$list" >"$ENV/mise-tools.json"
  run_doctor "$ENV"
  assert_code "unreadable tool list [$list]" 1
  assert_out "unreadable tool list [$list]" \
    "^fail  tools: cannot read the tool list that 'mise ls --current --json' printed"
  assert_out "unreadable tool list [$list]" "^      fix: mise install"
  assert_no_out "unreadable tool list [$list]" "jq: "
  assert_out "unreadable tool list [$list]" "^ok  java: "
  assert_out "unreadable tool list [$list]" "^summary: "
done

# Without mise.toml the min_version cannot be read: that is a failure with a fix, not a silent end of the run.
fresh
rm "$ENV/repo/mise.toml"
run_doctor "$ENV"
assert_code "no mise.toml" 1
assert_out "no mise.toml" "^fail  mise: mise\\.toml cannot be read"
assert_out "no mise.toml" "^      fix: restore it: git restore mise\\.toml"
assert_out "no mise.toml" "^ok  tools: "
assert_out "no mise.toml" "^summary: "

# A mise.toml that exists but cannot be read needs a different fix than a missing one: restoring would overwrite it.
fresh
if make_unreadable "$ENV/repo/mise.toml"; then
  run_doctor "$ENV"
  chmod 644 "$ENV/repo/mise.toml"
  assert_code "unreadable mise.toml" 1
  assert_out "unreadable mise.toml" "^fail  mise: mise\\.toml cannot be read"
  assert_out "unreadable mise.toml" "^      fix: chmod u\\+r mise\\.toml"
  assert_no_out "unreadable mise.toml" "git restore mise\\.toml"
  assert_out "unreadable mise.toml" "^ok  tools: "
  assert_out "unreadable mise.toml" "^summary: "
else
  skip_unreadable "unreadable mise.toml"
fi

# --- the local-only checks of a healthy tree ----------------------------------------------------------

fresh
run_doctor "$ENV"
assert_code "healthy local state" 0
assert_out "healthy local state" "^ok  image-store: "
assert_out "healthy local state" "^ok  colima: "
assert_out "healthy local state" "^ok  testcontainers-config: DOCKER_HOST unset, tc\\.host set, Ryuk enabled"
assert_out "healthy local state" "^ok  gsd: version 1\\.22\\.0, 3 files match the manifest"
assert_out "healthy local state" "^ok  gsd-defaults: "
assert_out "healthy local state" "^ok  mcp: all 2 servers in \\.mcp\\.json resolve through mise"
assert_out "healthy local state" "^ok  platform-token: "
assert_out "healthy local state" "^ok  claude-code: 2\\.1\\.330"
assert_out "healthy local state" "^ok  git: 2\\.47\\.1 meets the minimum 2\\.36\\.0"
assert_out "healthy local state" "^ok  claude-plugins: 2 enabled plugins are installed for this clone \\(axoniq-app-development@axoniq, gradle-skills@gradle-skills\\)"
assert_out "healthy local state" "^ok  claude-tracking: "
assert_no_out "healthy local state" "^(warn|fail)  (image-store|colima|gsd|gsd-defaults|mcp|platform-token|claude-code|claude-plugins|git|claude-tracking):"

# --- GSD integrity ---------------------------------------------------------------------------------

# A modified hook fails, names its path and says it is a hook.
fresh
echo "tampered" >"$ENV/repo/.claude/hooks/gsd-guard.js"
run_doctor "$ENV"
assert_code "modified hook" 1
assert_out "modified hook" "^fail  gsd: modified hook: hooks/gsd-guard\\.js"
assert_out "modified hook" "^      fix: npx -y @opengsd/gsd-core@1\\.22\\.0 --claude --local"

# A modified file that is not a hook is not called a hook.
fresh
echo "tampered" >"$ENV/repo/.claude/gsd-core/workflow.md"
run_doctor "$ENV"
assert_code "modified file" 1
assert_out "modified file" "^fail  gsd: modified: gsd-core/workflow\\.md"
assert_no_out "modified file" "modified hook"

fresh
rm "$ENV/repo/.claude/agents/gsd-executor.md"
run_doctor "$ENV"
assert_code "missing file" 1
assert_out "missing file" "^fail  gsd: missing: agents/gsd-executor\\.md"

# An unlisted file under a GSD directory is unexpected, and the two unlisted files of the installer are not.
fresh
echo "evil" >"$ENV/repo/.claude/hooks/evil.js"
run_doctor "$ENV"
assert_code "unexpected file" 1
assert_out "unexpected file" "^fail  gsd: unexpected: hooks/evil\\.js"
assert_no_out "unexpected file" "hooks/package\\.json|scripts/changeset/README\\.md"

# The tracked D-39 skill directory is outside GSD's scope.
fresh
mkdir -p "$ENV/repo/.claude/skills/spike-findings-x"
echo "skill" >"$ENV/repo/.claude/skills/spike-findings-x/SKILL.md"
run_doctor "$ENV"
assert_code "skills are not GSD files" 0

fresh
printf '1.23.0\n' >"$ENV/repo/governance/gsd-version"
run_doctor "$ENV"
assert_code "version differs" 1
assert_out "version differs" "^fail  gsd: installed GSD 1\\.22\\.0 differs from governance/gsd-version 1\\.23\\.0"
assert_out "version differs" "^      fix: npx -y @opengsd/gsd-core@1\\.23\\.0 --claude --local"

fresh
printf '\n' >"$ENV/repo/governance/gsd-version"
run_doctor "$ENV"
assert_code "empty pin" 1
assert_out "empty pin" "^fail  gsd: governance/gsd-version must hold one version such as 1\\.2\\.3"
assert_no_out "empty pin" "@opengsd/gsd-core@ "

fresh
printf '1.0.0; echo x\n' >"$ENV/repo/governance/gsd-version"
run_doctor "$ENV"
assert_code "garbage pin" 1
assert_out "garbage pin" "^fail  gsd: governance/gsd-version must hold one version such as 1\\.2\\.3"
assert_no_out "garbage pin" "echo ?x"

fresh
rm "$ENV/repo/.claude/gsd-file-manifest.json"
run_doctor "$ENV"
assert_code "no manifest" 1
assert_out "no manifest" "^fail  gsd: .claude/gsd-file-manifest\\.json is missing"
assert_out "no manifest" "^      fix: npx -y @opengsd/gsd-core@1\\.22\\.0 --claude --local"

# A manifest that is not JSON, or that holds no object of files, is reported with the reinstall fix, and the later
# checks and the summary still run (the review's WR-01: jq died under set -e). The shape cases also cover the later
# reads of `.files`, which a manifest without an object of files would have ended.
for content in '{' '[]' '{"version": "1.22.0"}' '{"version": "1.22.0", "files": []}'; do
  fresh
  printf '%s\n' "$content" >"$ENV/repo/.claude/gsd-file-manifest.json"
  run_doctor "$ENV"
  assert_code "unreadable manifest [$content]" 1
  assert_out "unreadable manifest [$content]" \
    "^fail  gsd: \\.claude/gsd-file-manifest\\.json cannot be read as a GSD manifest"
  assert_out "unreadable manifest [$content]" "^      fix: npx -y @opengsd/gsd-core@1\\.22\\.0 --claude --local"
  assert_out "unreadable manifest [$content]" "^ok  mcp: "
  assert_out "unreadable manifest [$content]" "^summary: "
  assert_no_out "unreadable manifest [$content]" "jq: "
  assert_no_out "unreadable manifest [$content]" "^fail  gsd: (unexpected|missing|modified)"
done

# A pin that cannot be read is a failure with a fix (skipped where root reads a mode-000 file).
fresh
if make_unreadable "$ENV/repo/governance/gsd-version"; then
  run_doctor "$ENV"
  chmod 644 "$ENV/repo/governance/gsd-version"
  assert_code "unreadable pin" 1
  assert_out "unreadable pin" "^fail  gsd: governance/gsd-version cannot be read"
  assert_out "unreadable pin" "^      fix: chmod u\\+r governance/gsd-version"
  assert_out "unreadable pin" "^ok  gsd-defaults: "
  assert_out "unreadable pin" "^summary: "
else
  skip_unreadable "unreadable pin"
fi

# --- ~/.gsd/defaults.json --------------------------------------------------------------------------

fresh
mkdir -p "$ENV/home/.gsd"
echo '{"runtime": "value-sentinel-1", "resolve_model_ids": "value-sentinel-2"}' >"$ENV/home/.gsd/defaults.json"
run_doctor "$ENV"
assert_code "leaking defaults" 0
assert_out "leaking defaults" "^warn  gsd-defaults: .*resolve_model_ids, runtime"
assert_no_out "leaking defaults" "value-sentinel"

# --- MCP servers -----------------------------------------------------------------------------------

fresh
printf 'jq\nplaywright-mcp\n' >"$ENV/mise-binaries"
run_doctor "$ENV"
assert_code "unresolved MCP binary" 0
assert_out "unresolved MCP binary" "^warn  mcp: .*context7.*context7-mcp"
assert_out "unresolved MCP binary" "^      fix: mise install"

# A server without `command` (an HTTP or SSE server with a url) has no binary to resolve: it is no unresolved binary, and
# its url, which can carry a credential, never reaches the output (WR-01 of the Phase 2 review).
fresh
printf '{"mcpServers": {"context7": {"command": "mise", "args": ["x", "--", "context7-mcp"]},
  "playwright": {"command": "mise", "args": ["x", "--", "playwright-mcp"]},
  "remote": {"type": "http", "url": "https://example.invalid/mcp?token=secret-value"}}}\n' >"$ENV/repo/.mcp.json"
run_doctor "$ENV"
assert_code "url-only MCP server with stdio servers" 0
assert_out "url-only MCP server with stdio servers" "^ok  mcp: "
assert_no_out "url-only MCP server with stdio servers" "^warn  mcp:"
assert_no_out "url-only MCP server with stdio servers" "secret-value"

fresh
printf '{"mcpServers": {"remote": {"type": "sse", "url": "https://example.invalid/sse?token=secret-value"}}}\n' \
  >"$ENV/repo/.mcp.json"
run_doctor "$ENV"
assert_code "only a url-only MCP server" 0
assert_out "only a url-only MCP server" "^ok  mcp: "
assert_no_out "only a url-only MCP server" "^warn  mcp:"
assert_no_out "only a url-only MCP server" "secret-value|example\\.invalid"

# A url-only server in front of an unresolved one must not shift the fields of the next line.
fresh
printf '{"mcpServers": {"remote": {"type": "http", "url": "https://example.invalid/mcp"},
  "context7": {"command": "mise", "args": ["x", "--", "missing-mcp"]}}}\n' >"$ENV/repo/.mcp.json"
run_doctor "$ENV"
assert_out "url-only MCP server before an unresolved one" "^warn  mcp: .*context7 \\(missing-mcp\\)"
assert_no_out "url-only MCP server before an unresolved one" "remote"

# Only a server with a url has no command to resolve. One with neither a command nor a url can never start, so the check
# fails (IN-01 of the Phase 2 review).
for content in '{"mcpServers": {"broken": {}}}' '{"mcpServers": {"broken": {"command": ""}}}' \
  '{"mcpServers": {"broken": {"command": 5}}}' '{"mcpServers": {"broken": {"type": "http", "url": ""}}}'; do
  fresh
  printf '%s\n' "$content" >"$ENV/repo/.mcp.json"
  run_doctor "$ENV"
  assert_code "MCP server without command and url [$content]" 1
  assert_out "MCP server without command and url [$content]" \
    "^fail  mcp: servers in \\.mcp\\.json with neither a command nor a url: broken"
  assert_out "MCP server without command and url [$content]" "^      fix: .*git restore \\.mcp\\.json"
  assert_no_out "MCP server without command and url [$content]" "^ok  mcp:"
  assert_out "MCP server without command and url [$content]" "^summary: "
done

fresh
printf '{"mcpServers": {"remote": {"type": "http", "url": "https://example.invalid/mcp?token=secret-value"},
  "broken": {},
  "context7": {"command": "mise", "args": ["x", "--", "missing-mcp"]}}}\n' >"$ENV/repo/.mcp.json"
run_doctor "$ENV"
assert_code "MCP server without command and url next to an unresolved one" 1
assert_out "MCP server without command and url next to an unresolved one" "^fail  mcp: .*broken.*context7 \\(missing-mcp\\)"
assert_out "MCP server without command and url next to an unresolved one" "^      fix: .*git restore \\.mcp\\.json"
assert_out "MCP server without command and url next to an unresolved one" "^      fix: mise install"
assert_no_out "MCP server without command and url next to an unresolved one" "secret-value|example\\.invalid"

# `mise x --` without a binary argument resolves `mise` itself.
fresh
printf '{"mcpServers": {"bare": {"command": "mise", "args": ["x", "--"]}}}\n' >"$ENV/repo/.mcp.json"
run_doctor "$ENV"
assert_code "mise x -- without a binary" 0
assert_out "mise x -- without a binary" "^ok  mcp: "
assert_no_out "mise x -- without a binary" "^warn  mcp:"

# A command that cannot be found keeps its real name in the warning.
fresh
printf '{"mcpServers": {"plain": {"command": "no-such-server-binary"}}}\n' >"$ENV/repo/.mcp.json"
run_doctor "$ENV"
assert_out "unresolved stdio binary" "^warn  mcp: .*plain \\(no-such-server-binary\\)"

# A .mcp.json that is not JSON, or not an object of server objects, is a warning with a fix (the review's WR-01 class).
for content in '{' '{"mcpServers": {"context7": 1}}' '{"mcpServers": []}'; do
  fresh
  printf '%s\n' "$content" >"$ENV/repo/.mcp.json"
  run_doctor "$ENV"
  assert_code "unreadable .mcp.json [$content]" 0
  assert_out "unreadable .mcp.json [$content]" "^warn  mcp: \\.mcp\\.json cannot be read as MCP server configuration"
  assert_out "unreadable .mcp.json [$content]" "^      fix: .*git restore \\.mcp\\.json"
  assert_out "unreadable .mcp.json [$content]" "^ok  platform-token: "
  assert_out "unreadable .mcp.json [$content]" "^summary: "
  assert_no_out "unreadable .mcp.json [$content]" "^ok  mcp:"
  assert_no_out "unreadable .mcp.json [$content]" "jq: "
done

# --- the Platform token ----------------------------------------------------------------------------

fresh
run_doctor "$ENV"
assert_out "token set" "^ok  platform-token: AXONIQ_PLATFORM_AUTHENTICATION is set"
assert_no_out "token set" "secret-sentinel-value"
assert_no_out "token set" "secret"

fresh
rm "$ENV/repo/.env"
run_doctor "$ENV"
assert_code "no .env" 0
assert_out "no .env" "^warn  platform-token: .*12 hours"

fresh
printf '# AXONIQ_PLATFORM_AUTHENTICATION=secret-sentinel-value\nOTHER=1\n' >"$ENV/repo/.env"
run_doctor "$ENV"
assert_code "commented token" 0
assert_out "commented token" "^warn  platform-token: .*12 hours"
assert_no_out "commented token" "secret-sentinel-value"

for empty in 'AXONIQ_PLATFORM_AUTHENTICATION=' 'AXONIQ_PLATFORM_AUTHENTICATION=""' "AXONIQ_PLATFORM_AUTHENTICATION=''" \
  'AXONIQ_PLATFORM_AUTHENTICATION=   '; do
  fresh
  printf '%s\n' "$empty" >"$ENV/repo/.env"
  run_doctor "$ENV"
  assert_code "empty token [$empty]" 1
  assert_out "empty token [$empty]" "^fail  platform-token: AXONIQ_PLATFORM_AUTHENTICATION is defined but empty"
  assert_out "empty token [$empty]" "^      fix: remove the line or set <environment-id>:<token>"
done

# A .env that exists but cannot be read is a warning with a fix, and its content is never shown.
fresh
if make_unreadable "$ENV/repo/.env"; then
  run_doctor "$ENV"
  chmod 644 "$ENV/repo/.env"
  assert_code "unreadable .env" 0
  assert_out "unreadable .env" "^warn  platform-token: \\.env exists but cannot be read"
  assert_out "unreadable .env" "^      fix: chmod u\\+r \\.env"
  assert_out "unreadable .env" "^summary: "
  assert_no_out "unreadable .env" "secret-sentinel-value"
else
  skip_unreadable "unreadable .env"
fi

# --- colima and the image store --------------------------------------------------------------------

fresh
printf 'network:\n  dns: []\ndocker:\n  features:\n    containerd-snapshotter: false\n' \
  >"$ENV/home/.colima/default/colima.yaml"
run_doctor "$ENV"
assert_code "colima without dns" 0
assert_out "colima without dns" "^warn  colima: network\\.dns is empty"
assert_out "colima without dns" "^      fix: colima stop && colima start --dns "

fresh
printf 'network:\n  dns:\n    - 8.8.8.8\ndocker:\n  features:\n    containerd-snapshotter: true\n' \
  >"$ENV/home/.colima/default/colima.yaml"
run_doctor "$ENV"
assert_code "colima with the containerd snapshotter" 0
assert_out "colima with the containerd snapshotter" "^ok  colima: network\\.dns lists 1 servers"
assert_no_out "colima with the containerd snapshotter" "^warn  colima:"
assert_no_out "colima with the containerd snapshotter" "snapshotter"

fresh
if make_unreadable "$ENV/home/.colima/default/colima.yaml"; then
  run_doctor "$ENV"
  chmod 644 "$ENV/home/.colima/default/colima.yaml"
  assert_code "unreadable colima configuration" 0
  assert_out "unreadable colima configuration" "^warn  colima: cannot read the colima configuration"
  assert_out "unreadable colima configuration" "^summary: "
else
  skip_unreadable "unreadable colima configuration"
fi

# The containerd image store needs Docker 29.9.0 or newer (docs/tooling.md, UP-0012); below that, or on a version that
# cannot be compared, the check warns (WR-01 of the Phase 2 review). A `-` or `+` suffix is dropped before comparing.
use_containerd_store() {
  echo overlayfs >"$ENV/docker-driver"
  echo '[["driver-type","io.containerd.snapshotter.v1"]]' >"$ENV/docker-driver-status"
}

for version in 29.8.2 28.5.2; do
  fresh
  echo "$version" >"$ENV/docker-server-version"
  use_containerd_store
  run_doctor "$ENV"
  assert_code "containerd image store on Docker $version" 0
  assert_out "containerd image store on Docker $version" \
    "^warn  image-store: the containerd image store is active on Docker ${version//./\\.}, which is older than 29\\.9\\.0"
  assert_out "containerd image store on Docker $version" "^      fix: upgrade the Docker engine to 29\\.9\\.0 or newer"
  assert_out "containerd image store on Docker $version" \
    "^      fix: or use the classic image store, colima: .*containerd-snapshotter: false"
  assert_no_out "containerd image store on Docker $version" "^ok  image-store:"
done

fresh
echo 29.9.0 >"$ENV/docker-server-version"
use_containerd_store
run_doctor "$ENV"
assert_code "containerd image store on the minimum" 0
assert_out "containerd image store on the minimum" \
  "^ok  image-store: the containerd image store is active on Docker 29\\.9\\.0 \\(driver overlayfs\\), which meets its minimum 29\\.9\\.0"
assert_no_out "containerd image store on the minimum" "^warn  image-store:"

# As text, 29.10.0 sorts before 29.9.0; the comparison is numeric.
fresh
echo 29.10.0 >"$ENV/docker-server-version"
use_containerd_store
run_doctor "$ENV"
assert_code "containerd image store on a two-digit minor" 0
assert_out "containerd image store on a two-digit minor" "^ok  image-store: the containerd image store is active on Docker 29\\.10\\.0 "
assert_no_out "containerd image store on a two-digit minor" "^warn  image-store:"

fresh
echo 29.10.0+azure-1 >"$ENV/docker-server-version"
use_containerd_store
run_doctor "$ENV"
assert_code "containerd image store on a suffixed version" 0
assert_out "containerd image store on a suffixed version" "^ok  image-store: .*on Docker 29\\.10\\.0\\+azure-1 "
assert_no_out "containerd image store on a suffixed version" "^warn  image-store:"

fresh
echo dev >"$ENV/docker-server-version"
use_containerd_store
run_doctor "$ENV"
assert_code "containerd image store on an uncomparable version" 0
assert_out "containerd image store on an uncomparable version" \
  "^warn  image-store: the containerd image store is active on Docker 'dev', a version that cannot be compared with its minimum 29\\.9\\.0"
assert_out "containerd image store on an uncomparable version" "^      fix: upgrade the Docker engine to 29\\.9\\.0 or newer"
assert_no_out "containerd image store on an uncomparable version" "^ok  image-store:"
assert_out "containerd image store on an uncomparable version" "^summary: "

fresh
echo 29.5.2 >"$ENV/docker-server-version"
run_doctor "$ENV"
assert_code "classic image store" 0
assert_out "classic image store" "^ok  image-store: the classic image store is active on Docker 29\\.5\\.2 \\(driver overlay2\\)"
assert_no_out "classic image store" "^warn  image-store:"

# Another engine than colima has no colima configuration to check.
fresh
echo desktop-linux >"$ENV/docker-context"
rm -r "$ENV/home/.colima"
run_doctor "$ENV"
assert_code "Docker Desktop" 0
assert_out "Docker Desktop" "^ok  docker: Docker Desktop reachable through context desktop-linux"
assert_out "Docker Desktop" "^skip  colima: the engine is not colima"

# --- Testcontainers settings -----------------------------------------------------------------------

fresh
EXTRA_ENV="TESTCONTAINERS_RYUK_DISABLED=true"
run_doctor "$ENV"
assert_code "Ryuk disabled" 0
assert_out "Ryuk disabled" "^warn  testcontainers-config: Ryuk is disabled"

fresh
rm "$ENV/home/.testcontainers.properties"
run_doctor "$ENV"
assert_code "colima without a Testcontainers host" 0
assert_out "colima without a Testcontainers host" "^warn  testcontainers-config: .*no DOCKER_HOST"

fresh
EXTRA_ENV="DOCKER_HOST=unix:///Users/someone/.colima/default/docker.sock"
run_doctor "$ENV"
assert_out "DOCKER_HOST set" "^ok  testcontainers-config: DOCKER_HOST set \\(unix\\), tc\\.host set, Ryuk enabled"

# --- the Claude Code version -----------------------------------------------------------------------

fresh
echo "2.1.323" >"$ENV/claude-version"
run_doctor "$ENV"
assert_code "Claude Code too old" 0
assert_out "Claude Code too old" "^warn  claude-code: 2\\.1\\.323 is older than 2\\.1\\.324"
assert_out "Claude Code too old" "^      fix: brew upgrade --cask claude-code@latest"

fresh
rm "$ENV/bin/claude"
run_doctor "$ENV"
assert_out "Claude Code missing" "^warn  claude-code: the claude CLI is not on PATH"

fresh
touch "$ENV/claude-fails"
run_doctor "$ENV"
assert_code "claude --version fails" 0
assert_out "claude --version fails" "^warn  claude-code: cannot read the version from 'claude --version'"
assert_out "claude --version fails" "^summary: "

# --- the project's Claude Code plugins (G-02-2) -------------------------------------------------------
# `.claude/settings.json` enables the plugins, but Claude Code installs none of them for a clone by itself: each
# contributor installs them once per clone at project scope. The healthy fixture has both enabled and installed.

PLUGIN_A=axoniq-app-development@axoniq
PLUGIN_B=gradle-skills@gradle-skills
RECORDS="home/.claude/plugins/installed_plugins.json"

fresh
printf '{}\n' >"$ENV/repo/.claude/settings.json"
run_doctor "$ENV"
assert_code "no plugin enabled" 0
assert_out "no plugin enabled" "^ok  claude-plugins: no plugin is enabled in \\.claude/settings\\.json"

fresh
printf '{"enabledPlugins": {"%s": false}}\n' "$PLUGIN_A" >"$ENV/repo/.claude/settings.json"
rm "$ENV/$RECORDS"
run_doctor "$ENV"
assert_out "only disabled plugins" "^ok  claude-plugins: no plugin is enabled in \\.claude/settings\\.json"

# No install record at all: one warning per enabled plugin, each with the install command, and exit 0.
fresh
rm "$ENV/$RECORDS"
run_doctor "$ENV"
assert_code "no install records" 0
assert_out "no install records" "^warn  claude-plugins: $PLUGIN_A is enabled in \\.claude/settings\\.json but not installed for this clone"
assert_out "no install records" "^warn  claude-plugins: $PLUGIN_B is enabled in \\.claude/settings\\.json but not installed for this clone"
assert_out "no install records" "^      fix: claude plugin install $PLUGIN_A --scope project \\(once per clone, in the main checkout\\)"
assert_out "no install records" "^      fix: claude plugin install $PLUGIN_B --scope project \\(once per clone, in the main checkout\\)"
assert_out "no install records" "^      fix: then run /reload-plugins in Claude Code"
assert_no_out "no install records" "^ok  claude-plugins: "

# A record of another project does not count; the plugin with a record for this clone is not reported. The path of
# the foreign record, like every other record field, never reaches the output.
fresh
write_install_records "$ENV/home/.claude" project "$ENV/foreign-project-sentinel" "$PLUGIN_A"
add_install_records "$ENV/home/.claude" project "$ENV/repo" "$PLUGIN_B"
run_doctor "$ENV"
assert_code "foreign project record" 0
assert_out "foreign project record" "^warn  claude-plugins: $PLUGIN_A is enabled in \\.claude/settings\\.json but not installed for this clone"
assert_no_out "foreign project record" "claude-plugins: $PLUGIN_B"
assert_no_out "foreign project record" "foreign-project-sentinel|fake/plugin-cache|0123456789abcdef"

# A user-scope record counts for every clone.
fresh
write_install_records "$ENV/home/.claude" user "" "$PLUGIN_A" "$PLUGIN_B"
run_doctor "$ENV"
assert_out "user scope record" "^ok  claude-plugins: 2 enabled plugins are installed for this clone"

# The records are read from CLAUDE_CONFIG_DIR when it is set, and only from there.
fresh
mkdir -p "$ENV/config"
mv "$ENV/home/.claude/plugins" "$ENV/config/plugins"
EXTRA_ENV="CLAUDE_CONFIG_DIR=$ENV/config"
run_doctor "$ENV"
assert_out "CLAUDE_CONFIG_DIR with the records" "^ok  claude-plugins: 2 enabled plugins are installed for this clone"

fresh
mkdir -p "$ENV/config"
EXTRA_ENV="CLAUDE_CONFIG_DIR=$ENV/config"
run_doctor "$ENV"
assert_out "CLAUDE_CONFIG_DIR without the records" "^warn  claude-plugins: $PLUGIN_A is enabled"

# A session in a linked worktree uses the install record of the main checkout (observed 2026-11-25).
fresh
git -C "$ENV/repo" -c user.name=test -c user.email=test@example.invalid -c commit.gpgsign=false \
  -c core.hooksPath=/dev/null commit -q -m fixture
git -C "$ENV/repo" worktree add -q --detach "$ENV/worktree"
EXTRA_ENV="DOCTOR_ROOT=$ENV/worktree"
run_doctor "$ENV"
assert_out "linked worktree" "^ok  claude-plugins: 2 enabled plugins are installed for this clone"

# Paths compare after symlinks are resolved, on both sides.
fresh
ln -s "$ENV/repo" "$ENV/link"
write_install_records "$ENV/home/.claude" project "$ENV/link" "$PLUGIN_A" "$PLUGIN_B"
run_doctor "$ENV"
assert_out "record through a symlink" "^ok  claude-plugins: 2 enabled plugins are installed for this clone"

fresh
ln -s "$ENV/repo" "$ENV/link"
EXTRA_ENV="DOCTOR_ROOT=$ENV/link"
run_doctor "$ENV"
assert_out "root through a symlink" "^ok  claude-plugins: 2 enabled plugins are installed for this clone"

# A key that is no plugin id fails the check and is never echoed, nor reaches a command.
fresh
# shellcheck disable=SC2016 # the $( is text of the settings file on purpose, it must never be expanded
printf '{"enabledPlugins": {"evil\\u001b[31m@m": true, "x$(touch %s/pwned)@m": true, "%s": true}}\n' \
  "$ENV" "$PLUGIN_B" >"$ENV/repo/.claude/settings.json"
run_doctor "$ENV"
assert_code "malformed plugin key" 1
assert_out "malformed plugin key" "^fail  claude-plugins: 2 enabledPlugins entries in \\.claude/settings\\.json are not of the form"
assert_out "malformed plugin key" "^      fix: correct the key in \\.claude/settings\\.json"
assert_no_out "malformed plugin key" "evil|touch|pwned"
assert_equals "malformed plugin key runs nothing" "absent" "$([[ -e $ENV/pwned ]] && echo present || echo absent)"

# Records that cannot be read are one warning for the whole check, with a manual way to look.
for content in 'not json' '{"version": 1, "plugins": {}}' '{"version": 2}'; do
  fresh
  printf '%s\n' "$content" >"$ENV/$RECORDS"
  run_doctor "$ENV"
  assert_code "unreadable install records [$content]" 0
  assert_out "unreadable install records [$content]" "^warn  claude-plugins: the install records of Claude Code cannot be read"
  assert_out "unreadable install records [$content]" "^      fix: claude plugin list --json shows the install state"
  assert_no_out "unreadable install records [$content]" "jq: |not installed for this clone"
done

fresh
if make_unreadable "$ENV/$RECORDS"; then
  run_doctor "$ENV"
  assert_out "install records mode 000" "^warn  claude-plugins: the install records of Claude Code cannot be read"
  chmod 644 "$ENV/$RECORDS"
else
  skip_unreadable "install records mode 000"
fi

# The committed settings are expected; nothing is guessed when they are missing or not JSON.
fresh
rm "$ENV/repo/.claude/settings.json"
run_doctor "$ENV"
assert_code "settings missing" 0
assert_out "settings missing" "^warn  claude-plugins: \\.claude/settings\\.json cannot be read as JSON"

fresh
printf 'not json\n' >"$ENV/repo/.claude/settings.json"
run_doctor "$ENV"
assert_out "settings not JSON" "^warn  claude-plugins: \\.claude/settings\\.json cannot be read as JSON"
assert_no_out "settings not JSON" "jq: "

# Without jq the records cannot be read: a warning with the mise fix, not a crash.
fresh
mkdir -p "$ENV/nojq"
for tool in "$SYSBIN"/*; do
  [[ ${tool##*/} == jq ]] || ln -s "$(readlink "$tool")" "$ENV/nojq/${tool##*/}"
done
EXTRA_ENV="PATH=$ENV/bin:$ENV/nojq"
run_doctor "$ENV"
assert_out "jq missing" "^warn  claude-plugins: jq is needed to read the plugin install records and is not installed"
assert_out "jq missing" "^      fix: mise install"

fresh
rm "$ENV/bin/claude"
run_doctor "$ENV"
assert_out "claude missing" "^skip  claude-plugins: the claude CLI is not on PATH, see the claude-code check"

# --- the git version the governance hooks need (ADR-0015) ---------------------------------------------

fresh
echo "git version 2.35.8" >"$ENV/git-version"
run_doctor "$ENV"
assert_code "git too old" 1
assert_out "git too old" "^fail  git: 2\\.35\\.8 is older than 2\\.36\\.0"
assert_out "git too old" "^      fix: brew install git"

fresh
echo "git version 2.36.0" >"$ENV/git-version"
run_doctor "$ENV"
assert_code "git at the minimum" 0
assert_out "git at the minimum" "^ok  git: 2\\.36\\.0 meets the minimum 2\\.36\\.0"

fresh
echo "git version unknown" >"$ENV/git-version"
run_doctor "$ENV"
assert_code "git version unreadable" 1
assert_out "git version unreadable" "^fail  git: cannot read the version from 'git --version'"
assert_out "git version unreadable" "^      fix: brew install git"

# The macOS /usr/bin/git shim without the Command Line Tools fails every call. Doctor reports it and still runs
# the later checks and prints the summary instead of dying silently under `set -e`.
fresh
touch "$ENV/git-broken"
run_doctor "$ENV"
assert_code "git shim fails" 1
assert_out "git shim fails" "^fail  git: cannot read the version from 'git --version'"
assert_out "git shim fails" "^      fix: brew install git"
assert_out "git shim fails" "^      fix: xcode-select --install"
assert_out "git shim fails" "^skip  claude-tracking: "
assert_out "git shim fails" "^summary: "

# --- tracked .claude files (AIDEV-03) --------------------------------------------------------------

fresh
mkdir -p "$ENV/repo/.claude/agents"
echo "x" >"$ENV/repo/.claude/agents/x.md"
git -C "$ENV/repo" add -f .claude/agents/x.md
run_doctor "$ENV"
assert_code "tracked agent file" 1
assert_out "tracked agent file" "^fail  claude-tracking: .claude/agents/x\\.md is tracked"
assert_out "tracked agent file" "^      fix: git rm --cached .claude/agents/x\\.md"

fresh
mkdir -p "$ENV/repo/.claude/skills/spike-findings-x"
echo "skill" >"$ENV/repo/.claude/skills/spike-findings-x/SKILL.md"
git -C "$ENV/repo" add -f .claude/skills/spike-findings-x/SKILL.md
run_doctor "$ENV"
assert_code "tracked spike skill" 0
assert_out "tracked spike skill" "^ok  claude-tracking: "

# --- upstream-removal ----------------------------------------------------------------------------

# The marker word lives in a variable: written out after a comment opener, it would itself be a marker, and the
# upstream record check would report it for a record that does not exist.
MARK=UPSTREAM

# add_record <env dir> <number> <status>: a tracked upstream record UP-<number> with two lines of removal criteria.
add_record() {
  local dir=$1 number=$2 status=$3 record
  record="$dir/repo/docs/upstream/$number-sample.md"
  mkdir -p "$dir/repo/docs/upstream"
  printf -- '---\nid: "UP-%s"\nproduct: "docker"\nstatus: "%s"\n---\n\n# UP-%s: Sample\n\n## Symptom\n\nIt fails.\n\n' \
    "$number" "$status" "$number" >"$record"
  printf '## Removal criteria\n\nThe fixed release is out.\nThe workaround is no longer needed.\n\n' >>"$record"
  printf '## How to verify removal\n\nRun it.\n' >>"$record"
  git -C "$dir/repo" add "docs/upstream/$number-sample.md"
}

# add_marker <env dir> <path> <line text>: a tracked file that holds the line.
add_marker() {
  mkdir -p "$(dirname "$1/repo/$2")"
  printf '%s\n' "$3" >"$1/repo/$2"
  git -C "$1/repo" add "$2"
}

fresh
run_doctor "$ENV"
assert_code "no record" 0
assert_out "no record" "^ok  upstream-removal: "

fresh
add_record "$ENV" 0042 fixed-upstream
add_marker "$ENV" scripts/sample.sh "# $MARK: UP-0042 sample workaround"
run_doctor "$ENV"
assert_code "fixed-upstream record with a marker" 0
assert_out "fixed-upstream record with a marker" "^warn  upstream-removal: UP-0042 .*scripts/sample\\.sh:1"
assert_out "fixed-upstream record with a marker" \
  "^      fix: removal criteria \\(docs/upstream/0042-sample\\.md\\): The fixed release is out\\. The workaround is no longer needed\\.$"
assert_no_out "fixed-upstream record with a marker" "^ok  upstream-removal: "
run_doctor "$ENV" --ci
assert_code "fixed-upstream record with a marker, --ci" 0
assert_out "fixed-upstream record with a marker, --ci" "^warn  upstream-removal: UP-0042 .*scripts/sample\\.sh:1"

fresh
add_record "$ENV" 0042 open
add_marker "$ENV" scripts/sample.sh "# $MARK: UP-0042 sample workaround"
run_doctor "$ENV"
assert_out "open record with a marker" "^ok  upstream-removal: "
assert_no_out "open record with a marker" "^warn  upstream-removal: "

fresh
add_record "$ENV" 0042 fixed-upstream
add_marker "$ENV" docs/notes.md "See $MARK: UP-0042 in prose, and UP-0042 alone."
run_doctor "$ENV"
assert_out "an ID in prose" "^ok  upstream-removal: "

fresh
add_record "$ENV" 0042 fixed-upstream
add_marker "$ENV" docs/templates/sample.md "<!-- $MARK: UP-0042 template example -->"
run_doctor "$ENV"
assert_out "a marker under docs/templates" "^ok  upstream-removal: "

fresh
add_record "$ENV" 0042 fixed-upstream
add_marker "$ENV" scripts/sample.sh "# $MARK: UP-00421 another record"
run_doctor "$ENV"
assert_out "a longer ID" "^ok  upstream-removal: "

fresh
add_record "$ENV" 0042 fixed-upstream
printf '# %s: UP-0042 sample workaround\n' "$MARK" >"$ENV/repo/untracked.sh"
run_doctor "$ENV"
assert_out "an untracked file" "^ok  upstream-removal: "

# --- --ci -------------------------------------------------------------------------------------------

# The local-only checks are skipped, so a tree without any GSD, token, MCP or colima state passes, and the
# tracking check still runs.
fresh
rm -r "$ENV/repo/.claude/gsd-file-manifest.json" "$ENV/repo/.env" "$ENV/home/.colima" "$ENV/bin/claude"
mkdir -p "$ENV/home/.gsd"
echo '{"runtime": "x"}' >"$ENV/home/.gsd/defaults.json"
printf 'jq\n' >"$ENV/mise-binaries"
run_doctor "$ENV" --ci
assert_code "--ci" 0
for check in gsd gsd-defaults mcp platform-token colima claude-code claude-plugins git; do
  assert_out "--ci skips $check" "^skip  $check: "
done
assert_out "--ci still checks the tracked files" "^ok  claude-tracking: "
assert_out "--ci still checks docker" "^ok  docker: "
assert_no_out "--ci" "^(warn|fail)  (mise|tools|java|docker|image-store|testcontainers-config|claude-tracking):"

fresh
echo "x" >"$ENV/repo/.claude/agents/x.md"
git -C "$ENV/repo" add -f .claude/agents/x.md
run_doctor "$ENV" --ci
assert_code "--ci tracked agent file" 1
assert_out "--ci tracked agent file" "^fail  claude-tracking: "

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

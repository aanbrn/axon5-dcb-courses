#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Self-contained tests for retry-on-rate-limit.sh. Every case runs the real wrapper around a fake command whose
# output and exit status per run are scripted, and proves four things: a command that fails with Gradle's HTTP 429
# message is run exactly once more and the second result is final; any other failure, and any success, is never
# retried and keeps the command's own exit status; the arguments reach the command unchanged and every retry is
# announced with a fixed text; the command sees the caller's environment unchanged, its locale included.
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
WRAPPER="$SCRIPT_DIR/retry-on-rate-limit.sh"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

passed=0
failed=0

# The 429 line exactly as Gradle 9.9.2 prints it (see .planning/debug/compose-smoke-ci-failure.md).
RATE_LIMIT_LINE='Received status code 429 from server: Too Many Requests'
RETRY_MARKER='retry-on-rate-limit: Maven Central answered HTTP 429'
WARNING_PREFIX='::warning title=Maven Central rate limit::'

# --- fake commands -------------------------------------------------------------------------------

# fake-command: counts its runs in $FAKE_DIR/count and, for run N, prints $FAKE_DIR/run.N.stdout to standard output
# and $FAKE_DIR/run.N.stderr to standard error, then exits with the number in $FAKE_DIR/run.N.status (default 0). It
# also records the LC_ALL each run sees in $FAKE_DIR/run.N.lc_all.
cat >"$TMP/fake-command" <<'FAKE'
#!/usr/bin/env bash
set -euo pipefail
count=0
if [[ -f "$FAKE_DIR/count" ]]; then
  count=$(<"$FAKE_DIR/count")
fi
count=$((count + 1))
echo "$count" >"$FAKE_DIR/count"
printf '%s\n' "${LC_ALL-<unset>}" >"$FAKE_DIR/run.$count.lc_all"
if [[ -f "$FAKE_DIR/run.$count.stdout" ]]; then
  cat "$FAKE_DIR/run.$count.stdout"
fi
if [[ -f "$FAKE_DIR/run.$count.stderr" ]]; then
  cat "$FAKE_DIR/run.$count.stderr" >&2
fi
status=0
if [[ -f "$FAKE_DIR/run.$count.status" ]]; then
  status=$(<"$FAKE_DIR/run.$count.status")
fi
exit "$status"
FAKE

# fake-args: writes the arguments it received, one per line, to $FAKE_DIR/args.
cat >"$TMP/fake-args" <<'FAKE'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$@" >"$FAKE_DIR/args"
FAKE
chmod +x "$TMP/fake-command" "$TMP/fake-args"

# --- assertion helpers ---------------------------------------------------------------------------

# expect <name> <command...>: passes when the command succeeds; a failure prints the last run's output.
expect() {
  local name=$1
  shift
  if "$@"; then
    passed=$((passed + 1))
    echo "ok   - $name"
  else
    failed=$((failed + 1))
    echo "FAIL - $name (exit $CODE, $(runs) run(s))"
    while IFS= read -r line; do
      echo "       | $line"
    done <<<"$OUT"
  fi
}

contains() { grep -Fq -- "$1" <<<"$OUT"; }
absent() { ! grep -Fq -- "$1" <<<"$OUT"; }
exit_code_is() { [[ $CODE -eq $1 ]]; }
runs_is() { [[ $(runs) -eq $1 ]]; }
# line_count_is <fixed-string-prefix> <count>: exactly <count> lines of the output start with the prefix.
line_count_is() { [[ $(grep -c "^$1" <<<"$OUT" || true) -eq $2 ]]; }

# --- scenario helpers ----------------------------------------------------------------------------

CASE=0
FAKE_DIR=''

# new_case: a fresh scenario directory, so one case never sees another's counter or scripted runs.
new_case() {
  CASE=$((CASE + 1))
  FAKE_DIR="$TMP/case-$CASE"
  mkdir -p "$FAKE_DIR"
}

# script_run <n> <status> [stdout|stderr <text>]: scripts run <n> of the fake command.
script_run() {
  local n=$1 status=$2 stream=${3:-} text=${4:-}
  echo "$status" >"$FAKE_DIR/run.$n.status"
  if [[ -n $stream ]]; then
    echo "$text" >"$FAKE_DIR/run.$n.$stream"
  fi
}

runs() {
  if [[ -f "$FAKE_DIR/count" ]]; then
    cat "$FAKE_DIR/count"
  else
    echo 0
  fi
}

# run_wrapper <delay> <github-actions> <command...>: runs the wrapper with the given delay; github-actions is `true`
# to run as a workflow step and anything else to run with the variable unset, so the cases behave the same on a
# runner. Sets OUT (both streams, interleaved as a log would show them) and CODE.
run_wrapper() {
  local delay=$1 actions=$2
  shift 2
  local -a runner=(env -u GITHUB_ACTIONS)
  if [[ $actions == true ]]; then
    runner=(env GITHUB_ACTIONS=true)
  fi
  CODE=0
  OUT=$("${runner[@]}" FAKE_DIR="$FAKE_DIR" RATE_LIMIT_RETRY_DELAY_SECONDS="$delay" "$WRAPPER" "$@" 2>&1) || CODE=$?
}

# --- cases ---------------------------------------------------------------------------------------

# 1. A command that succeeds runs once, its output is streamed and nothing is announced.
new_case
script_run 1 0 stdout 'BUILD SUCCESSFUL in 4s'
run_wrapper 0 true "$TMP/fake-command"
expect "success: exits 0" exit_code_is 0
expect "success: runs the command once" runs_is 1
expect "success: streams the command output" contains 'BUILD SUCCESSFUL in 4s'
expect "success: prints no retry line" absent "$RETRY_MARKER"
expect "success: prints no warning annotation" absent "$WARNING_PREFIX"

# 2. A failure without the 429 message is never retried and keeps its exit status.
new_case
script_run 1 3 stderr 'FAILURE: Build failed with an exception.'
run_wrapper 0 true "$TMP/fake-command"
expect "other failure: keeps the exit status 3" exit_code_is 3
expect "other failure: runs the command once" runs_is 1
expect "other failure: streams the command output" contains 'FAILURE: Build failed with an exception.'
expect "other failure: prints no retry line" absent "$RETRY_MARKER"

# 3. A 429 on the first run and success on the second: two runs, exit 0, one announcement naming the delay.
new_case
script_run 1 1 stdout "$RATE_LIMIT_LINE"
script_run 2 0 stdout 'BUILD SUCCESSFUL in 1m 2s'
run_wrapper 0 true "$TMP/fake-command"
expect "429 then success: exits 0" exit_code_is 0
expect "429 then success: runs the command twice" runs_is 2
expect "429 then success: streams the output of both runs" contains 'BUILD SUCCESSFUL in 1m 2s'
expect "429 then success: prints one warning annotation" line_count_is "$WARNING_PREFIX" 1
expect "429 then success: the annotation names the delay" contains "${WARNING_PREFIX}$RETRY_MARKER (Too Many Requests); running the command once more in 0 s"

# 4. A 429 on both runs: exactly two runs, the second run's status is final.
new_case
script_run 1 1 stdout "$RATE_LIMIT_LINE"
script_run 2 4 stdout "$RATE_LIMIT_LINE"
script_run 3 0
run_wrapper 0 true "$TMP/fake-command"
expect "429 twice: exits with the status of the second run" exit_code_is 4
expect "429 twice: never runs a third time" runs_is 2
expect "429 twice: announces the retry once" line_count_is "$WARNING_PREFIX" 1

# 5. A 429 followed by a different failure: the different failure is final.
new_case
script_run 1 1 stdout "$RATE_LIMIT_LINE"
script_run 2 5 stderr 'FAILURE: Build failed with an exception.'
run_wrapper 0 true "$TMP/fake-command"
expect "429 then other failure: exits with the status of the second run" exit_code_is 5
expect "429 then other failure: runs the command twice" runs_is 2

# 6. The 429 message on standard error alone is detected: the wrapper merges both streams.
new_case
script_run 1 1 stderr "$RATE_LIMIT_LINE"
script_run 2 0 stdout 'BUILD SUCCESSFUL in 2s'
run_wrapper 0 true "$TMP/fake-command"
expect "429 on stderr: exits 0" exit_code_is 0
expect "429 on stderr: runs the command twice" runs_is 2

# 7. Arguments reach the command unchanged: spaces, glob characters, a variable reference, a quote and an empty word.
new_case
run_wrapper 0 true "$TMP/fake-args" 'a b' '*' "\$HOME" "it's" '' '--tests' '*Conditional*'
printf '%s\n' 'a b' '*' "\$HOME" "it's" '' '--tests' '*Conditional*' >"$FAKE_DIR/expected-args"
expect "arguments: exits 0" exit_code_is 0
expect "arguments: reach the command unchanged" cmp -s "$FAKE_DIR/args" "$FAKE_DIR/expected-args"

# 8. Usage errors exit 2 before any command runs.
new_case
run_wrapper 0 true
expect "no command: exits 2" exit_code_is 2
expect "no command: prints the usage line" contains 'usage: retry-on-rate-limit.sh <command>'

for bad_delay in abc -1 1.5 '1 2'; do
  new_case
  script_run 1 0 stdout 'must not run'
  run_wrapper "$bad_delay" true "$TMP/fake-command"
  expect "delay '$bad_delay': exits 2" exit_code_is 2
  expect "delay '$bad_delay': never runs the command" runs_is 0
  expect "delay '$bad_delay': names the variable" contains 'RATE_LIMIT_RETRY_DELAY_SECONDS'
done

# 9. Outside a workflow the retry still happens and is announced in plain text, without an annotation.
new_case
script_run 1 1 stdout "$RATE_LIMIT_LINE"
script_run 2 0 stdout 'BUILD SUCCESSFUL in 3s'
run_wrapper 0 false "$TMP/fake-command"
expect "outside Actions: exits 0" exit_code_is 0
expect "outside Actions: runs the command twice" runs_is 2
expect "outside Actions: prints a plain retry line" contains "$RETRY_MARKER"
expect "outside Actions: prints no warning annotation" absent '::warning'

# 10. The delay is honoured and shown.
new_case
script_run 1 1 stdout "$RATE_LIMIT_LINE"
script_run 2 0
SECONDS=0
run_wrapper 1 false "$TMP/fake-command"
expect "delay 1: waits at least a second" [ "$SECONDS" -ge 1 ]
expect "delay 1: shows the delay" contains 'running the command once more in 1 s'

# 11. Without RATE_LIMIT_RETRY_DELAY_SECONDS the delay is 60 seconds. A fake sleep records its argument, so the
# case does not wait.
new_case
mkdir -p "$FAKE_DIR/bin"
cat >"$FAKE_DIR/bin/sleep" <<'FAKE'
#!/usr/bin/env bash
echo "$1" >"$FAKE_DIR/slept"
FAKE
chmod +x "$FAKE_DIR/bin/sleep"
script_run 1 1 stdout "$RATE_LIMIT_LINE"
script_run 2 0
CODE=0
OUT=$(env -u GITHUB_ACTIONS -u RATE_LIMIT_RETRY_DELAY_SECONDS FAKE_DIR="$FAKE_DIR" PATH="$FAKE_DIR/bin:$PATH" "$WRAPPER" "$TMP/fake-command" 2>&1) || CODE=$?
expect "default delay: exits 0" exit_code_is 0
expect "default delay: shows 60 s" contains 'running the command once more in 60 s'
expect "default delay: sleeps 60 seconds" [ "$(cat "$FAKE_DIR/slept" 2>/dev/null || echo none)" = 60 ]

# 12. The announcement text is fixed: command output never reaches an annotation of the wrapper's title.
new_case
printf '%s\n' "$RATE_LIMIT_LINE" '::error::injected%0A::warning::injected' >"$FAKE_DIR/run.1.stdout"
echo 1 >"$FAKE_DIR/run.1.status"
script_run 2 0
run_wrapper 0 true "$TMP/fake-command"
expect "fixed text: only the wrapper's own warning carries its title" line_count_is "$WARNING_PREFIX" 1
expect "fixed text: runs the command twice" runs_is 2

# 13. The wrapped command runs in the caller's locale in both runs (review WR-01). Unset is what a GitHub runner has
# (LANG only). POSIX exists on every system and differs from `C` as a string, so an overwrite shows.
for caller_lc_all in '<unset>' POSIX; do
  new_case
  script_run 1 1 stdout "$RATE_LIMIT_LINE"
  script_run 2 0 stdout 'BUILD SUCCESSFUL in 5s'
  locale_env=(LC_ALL="$caller_lc_all")
  if [[ $caller_lc_all == '<unset>' ]]; then
    locale_env=(-u LC_ALL)
  fi
  CODE=0
  OUT=$(env -u GITHUB_ACTIONS "${locale_env[@]}" FAKE_DIR="$FAKE_DIR" RATE_LIMIT_RETRY_DELAY_SECONDS=0 "$WRAPPER" "$TMP/fake-command" 2>&1) || CODE=$?
  expect "caller LC_ALL $caller_lc_all: exits 0" exit_code_is 0
  expect "caller LC_ALL $caller_lc_all: runs the command twice" runs_is 2
  expect "caller LC_ALL $caller_lc_all: the first run sees the caller's LC_ALL" [ "$(cat "$FAKE_DIR/run.1.lc_all" 2>/dev/null || echo missing)" = "$caller_lc_all" ]
  expect "caller LC_ALL $caller_lc_all: the second run sees the caller's LC_ALL" [ "$(cat "$FAKE_DIR/run.2.lc_all" 2>/dev/null || echo missing)" = "$caller_lc_all" ]
done

echo
echo "$passed passed, $failed failed"
[[ $failed -eq 0 ]]

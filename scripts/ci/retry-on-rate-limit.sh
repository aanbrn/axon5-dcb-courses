#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Runs a command and, when it fails with Gradle's HTTP 429 message, waits and runs the same command exactly once
# more. The result of the second run is the result of this script.
#
# Why. Maven Central answers 429 (Too Many Requests) to a runner's egress IP when the window of its consumption
# limit is exhausted. Gradle 9.9.2 already retries a 429 inside the request (NetworkingIssueVerifier treats 408
# and 429 as transient and NetworkOperationBackOffAndRetry makes three attempts, with a back-off that starts at
# one second and doubles, about three seconds in all), which is far shorter than the window. Gradle then disables
# the repository for the rest of the build ("Repository ... is disabled due to earlier error") and does not
# continue with the next repository unless that repository sets allowInsecureContinueWhenDisabled, an option
# the Gradle user guide names insecure (Graph resolution, Repository disabling). So a transient 429 fails the
# job although nothing in the repository is wrong. A pull-request run of a job that main does not run yet starts
# with an empty Gradle cache, because the cache is written from main only, and downloads the whole classpath.
# A delayed rerun of the step is the smallest mechanism that turns that failure into a wait.
#
# Gradle prints the same message whichever host answered (Maven Central, the CI mirror that ADR-0016 puts in front
# of it, or the Gradle Plugin Portal), so neither the match nor the announcement names a host.
#
# What it never does: retry a failure whose output lacks the 429 message, run the command a third time, change
# a dependency source or an option of Gradle, change the command's environment (its locale included), or evaluate
# or re-quote the arguments. A test, compile or check failure therefore fails at once with its own exit status.
#
# Usage: retry-on-rate-limit.sh <command> [<argument>...]
#
# The command runs with standard error merged into standard output, streamed live and copied to a temporary
# log that is searched for the message once the command has failed. The announcement of a retry is a fixed
# text: nothing from the log reaches it, so the log cannot inject a workflow command.
#
# Environment:
#   RATE_LIMIT_RETRY_DELAY_SECONDS  seconds to wait before the rerun, a non-negative integer, default 60
#                                   (set only by the tests, which use 0)
#   GITHUB_ACTIONS                  when `true`, a retry also prints a warning annotation
#
# Exit codes: the command's own status (of the second run after a retry); 2 for a usage error.
set -euo pipefail

# The line Gradle prints for the failed request, for example
# "Received status code 429 from server: Too Many Requests".
readonly RATE_LIMIT_MESSAGE='Received status code 429 from server'

usage() {
  echo "usage: retry-on-rate-limit.sh <command> [<argument>...]" >&2
  exit 2
}

[[ $# -ge 1 ]] || usage

delay=${RATE_LIMIT_RETRY_DELAY_SECONDS:-60}
if ! [[ $delay =~ ^[0-9]+$ ]]; then
  printf 'retry-on-rate-limit: RATE_LIMIT_RETRY_DELAY_SECONDS must be a non-negative integer, got %q\n' "$delay" >&2
  exit 2
fi

log=$(mktemp)
trap 'rm -f "$log"' EXIT

# First run. The status is the command's, not tee's.
status=0
"$@" 2>&1 | tee "$log" || status=${PIPESTATUS[0]}

if [[ $status -eq 0 ]]; then
  exit 0
fi

# The C locale applies to this grep alone, so the match is byte-wise whatever the caller's locale. Exporting it would
# put both runs of the command, Gradle included, into the C locale (console and file-name encodings, the default
# Locale), unlike a local run.
if ! LC_ALL=C grep -aqF -- "$RATE_LIMIT_MESSAGE" "$log"; then
  exit "$status"
fi

message="retry-on-rate-limit: a server answered HTTP 429 (Too Many Requests); running the command once more in ${delay} s"
echo "$message"
if [[ ${GITHUB_ACTIONS:-} == true ]]; then
  echo "::warning title=Dependency download rate limit::$message"
fi

sleep "$delay"

# Second run: the last one, whatever it returns.
status=0
"$@" || status=$?
exit "$status"

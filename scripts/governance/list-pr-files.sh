#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Lists the files of a pull request for the governance gate, and fails closed. The gate decides on a
# complete list only, so an unusable answer from the files API is an error here and never an empty
# list: a failing `gh`, an error body, a broken pagination, no output at all, or JSON that does not
# combine into one array leaves no destination file behind. The result is renamed into place only
# after it has been validated.
# All inputs arrive through environment variables, never arguments:
#   PR_BASE_REPO   owner/name of the repository the pull request targets
#   PR_NUMBER      pull request number
#   PR_FILES_JSON  destination path of the JSON array, written only on success
#   GH_TOKEN       token read by `gh`
set -euo pipefail

# Byte-wise comparison: look-alike Unicode characters never match an ASCII pattern.
export LC_ALL=C

readonly PR_NUMBER_PATTERN='^[1-9][0-9]*$'
readonly REPO_PATTERN='^[A-Za-z0-9-]+/[A-Za-z0-9._-]+$'

# Temporary files live next to the destination. The paths are globals, not locals of `main`, so the
# EXIT trap still sees them after `main` has returned.
RAW_FILE=''
COMBINED_FILE=''

die() {
  echo "governance: $*" >&2
  exit 1
}

cleanup() {
  [[ -z $RAW_FILE ]] || rm -f -- "$RAW_FILE"
  [[ -z $COMBINED_FILE ]] || rm -f -- "$COMBINED_FILE"
}

# The values reach the API path, so they are checked before any `gh` call. A rejected value is not
# printed back: only the variable name is.
validate_inputs() {
  [[ -n ${PR_FILES_JSON:-} ]] || die "PR_FILES_JSON is not set"
  [[ -d $(dirname -- "$PR_FILES_JSON") ]] ||
    die "PR_FILES_JSON must name a file in an existing directory"
  [[ ${PR_NUMBER:-} =~ $PR_NUMBER_PATTERN ]] ||
    die "PR_NUMBER must be a positive integer without leading zeros"
  [[ ${PR_BASE_REPO:-} =~ $REPO_PATTERN ]] || die "PR_BASE_REPO must be owner/name"
  local repo_name=${PR_BASE_REPO#*/}
  [[ $repo_name != . && $repo_name != .. ]] || die "PR_BASE_REPO must be owner/name"
}

main() {
  validate_inputs
  # A list left behind by an earlier run must never be read as the list of this one.
  rm -f -- "$PR_FILES_JSON"

  trap cleanup EXIT
  # Same directory as the destination, so the final rename stays on one file system and is atomic.
  RAW_FILE=$(mktemp "${PR_FILES_JSON}.XXXXXX")
  COMBINED_FILE=$(mktemp "${PR_FILES_JSON}.XXXXXX")

  # A plain command, not a pipeline or a substitution: its exit status is the one that counts. An
  # HTTP error prints a body and a broken pagination prints some pages; both die with the file.
  local gh_status=0
  gh api --paginate "repos/${PR_BASE_REPO}/pulls/${PR_NUMBER}/files?per_page=100" >"$RAW_FILE" ||
    gh_status=$?
  [[ $gh_status -eq 0 ]] || die "listing the pull request files failed: gh exited with status $gh_status"
  [[ -s $RAW_FILE ]] ||
    die "gh printed nothing for the pull request files; an empty output is never read as an empty list"

  jq -s 'add' "$RAW_FILE" >"$COMBINED_FILE" ||
    die "the files API output is not valid JSON"
  jq -e 'type == "array"' "$COMBINED_FILE" >/dev/null 2>&1 ||
    die "the files API output did not combine into one JSON array"

  mv -f -- "$COMBINED_FILE" "$PR_FILES_JSON"
  echo "governance: listed $(jq 'length' "$PR_FILES_JSON") changed files"
}

main "$@"

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

die() {
  echo "governance: $*" >&2
  exit 1
}

main() {
  local raw='' combined='' gh_status=0
  trap 'rm -f -- "${raw:-}" "${combined:-}"' EXIT
  # Next to the destination, so the final rename stays on one file system and is atomic.
  raw=$(mktemp "${PR_FILES_JSON}.XXXXXX")
  combined=$(mktemp "${PR_FILES_JSON}.XXXXXX")

  # A plain command, not a pipeline or a substitution: its exit status is the one that counts. An
  # HTTP error prints a body and a broken pagination prints some pages; both die with the file.
  gh api --paginate "repos/${PR_BASE_REPO}/pulls/${PR_NUMBER}/files?per_page=100" >"$raw" ||
    gh_status=$?
  [[ $gh_status -eq 0 ]] || die "listing the pull request files failed: gh exited with status $gh_status"
  [[ -s $raw ]] ||
    die "gh printed nothing for the pull request files; an empty output is never read as an empty list"

  jq -s 'add' "$raw" >"$combined" ||
    die "the files API output is not valid JSON"
  jq -e 'type == "array"' "$combined" >/dev/null 2>&1 ||
    die "the files API output did not combine into one JSON array"

  mv -f -- "$combined" "$PR_FILES_JSON"
  echo "governance: listed $(jq 'length' "$PR_FILES_JSON") changed files"
}

main "$@"

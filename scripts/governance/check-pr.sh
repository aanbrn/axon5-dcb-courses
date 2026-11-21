#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Pull request governance gate: a PR that touches a governed file needs the maintainer's
# `governance-approved` label. All inputs arrive through environment variables, never arguments:
#   PR_FILES_JSON        path to the JSON array returned by the pull request files API
#   PR_LABELS_JSON       JSON array of label names on the pull request
#   GOVERNED_PATHS_FILE  list of governed path globs (default: governance/governed-paths.txt)
# PR data is read with jq only and is never evaluated by the shell.
set -euo pipefail

# Byte-wise comparison: look-alike Unicode characters never match an ASCII pattern.
export LC_ALL=C

readonly APPROVAL_LABEL="governance-approved"

die() {
  echo "governance: $*" >&2
  exit 1
}

# Succeeds when the path matches the glob. `**` matches any characters including `/`; every other
# character is literal, so a plain name matches exactly.
glob_match() {
  local path=$1 glob=$2
  if [[ $glob != *'**'* ]]; then
    [[ $path == "$glob" ]]
    return
  fi
  local -a parts=()
  local rest=$glob
  while [[ $rest == *'**'* ]]; do
    parts+=("${rest%%'**'*}")
    rest=${rest#*'**'}
  done
  parts+=("$rest")
  local last_index=$((${#parts[@]} - 1))
  local first=${parts[0]} last=${parts[last_index]} i
  [[ $path == "$first"* ]] || return 1
  path=${path#"$first"}
  for ((i = 1; i < last_index; i++)); do
    [[ $path == *"${parts[i]}"* ]] || return 1
    path=${path#*"${parts[i]}"}
  done
  [[ $path == *"$last" ]]
}

# Succeeds when the path matches any governed glob.
is_governed() {
  local path=$1 glob
  for glob in "${GLOBS[@]}"; do
    if glob_match "$path" "$glob"; then
      return 0
    fi
  done
  return 1
}

load_globs() {
  local file=$1 line
  [[ -f $file ]] || die "governed paths file not found: $file"
  GLOBS=()
  while IFS= read -r line || [[ -n $line ]]; do
    [[ -n $line ]] && GLOBS+=("$line")
  done <"$file"
  [[ ${#GLOBS[@]} -gt 0 ]] || die "governed paths file is empty: $file"
}

validate_inputs() {
  [[ -n ${PR_FILES_JSON:-} && -f ${PR_FILES_JSON} ]] || die "PR_FILES_JSON does not name a readable file"
  jq -e 'type == "array" and all(.[]; type == "object" and (.filename | type) == "string")' \
    "$PR_FILES_JSON" >/dev/null 2>&1 ||
    die "the changed files list is malformed: expected a JSON array of objects with a filename"
  [[ -n ${PR_LABELS_JSON:-} ]] || die "PR_LABELS_JSON is not set"
  printf '%s' "$PR_LABELS_JSON" | jq -e 'type == "array" and all(.[]; type == "string")' >/dev/null 2>&1 ||
    die "the label list is malformed: expected a JSON array of strings"
}

has_label() {
  printf '%s' "$PR_LABELS_JSON" | jq -e --arg label "$1" 'index($label) != null' >/dev/null
}

main() {
  local governed_file=${GOVERNED_PATHS_FILE:-governance/governed-paths.txt}
  load_globs "$governed_file"
  validate_inputs

  local -a touched=()
  local path
  while IFS= read -r -d '' path; do
    if is_governed "$path"; then
      touched+=("$path")
    fi
  done < <(jq --raw-output0 '.[] | .filename, (.previous_filename // empty)' "$PR_FILES_JSON")

  if [[ ${#touched[@]} -eq 0 ]]; then
    echo "governance: no governed file is touched"
    return 0
  fi

  if has_label "$APPROVAL_LABEL"; then
    echo "governance: governed files are touched and the pull request carries '$APPROVAL_LABEL'"
    return 0
  fi

  {
    echo "governance: this pull request touches governed files:"
    printf '  %s\n' "${touched[@]}"
    echo "governance: ask a maintainer to add the '$APPROVAL_LABEL' label, then this check re-runs."
  } >&2
  return 1
}

main "$@"

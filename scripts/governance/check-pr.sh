#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Pull request governance gate. Two rules, both decided here so CI and the tests run the same code:
#   1. A PR that touches a governed file needs the maintainer's `governance-approved` label. The
#      only exemption is a Renovate bump whose every governed diff changes versions, commit SHAs or
#      digests and nothing else. A mise sidecar lockfile under `.mise/locks/` whose directory name
#      carries the tool version counts as modified, not renamed, when its old and new paths differ
#      only in that version. A list at the files API limit (3000 entries) may hide a governed
#      file, so it needs the label and is never exempt. A list that cannot be read completely fails,
#      with or without the label. The paths in NEVER_EXEMPT_PATHS (the GSD version pin) never take the
#      exemption, because a new GSD release changes hooks that execute in the maintainer's session.
#   2. A PR from a branch of this repository must come from a GSD phase branch or a bot branch.
# All inputs arrive through environment variables, never arguments:
#   PR_FILES_JSON        path to the JSON array returned by the pull request files API
#   PR_LABELS_JSON       JSON array of label names on the pull request
#   PR_AUTHOR            login of the pull request author
#   PR_HEAD_REF          head branch name
#   PR_HEAD_REPO         full name of the repository the head branch lives in
#   PR_BASE_REPO         full name of the repository the pull request targets
#   GOVERNED_PATHS_FILE  list of governed path globs (default: governance/governed-paths.txt)
# PR data is read with jq only and is never evaluated by the shell. The governed paths grammar and the glob matcher come
# from governed-paths.sh next to this script, which the Claude Code hooks share.
set -euo pipefail

# Byte-wise comparison: look-alike Unicode characters never match an ASCII pattern.
export LC_ALL=C

# shellcheck source=/dev/null
source "$(dirname -- "${BASH_SOURCE[0]}")/governed-paths.sh"

readonly APPROVAL_LABEL="governance-approved"
readonly BOT_AUTHOR="renovate[bot]"
# Governed paths that never take the renovate[bot] exemption, compared byte for byte as whole paths. The GSD pin
# selects the release whose hooks and agents run in the maintainer's Claude Code session, so its bump is reviewed like
# code (docs/tooling.md, GSD updates; ADR-0003).
readonly NEVER_EXEMPT_PATHS=(governance/gsd-version)
readonly PHASE_BRANCH_PATTERN='^gsd/phase-[0-9]+(\.[0-9]+)?-[a-z0-9][a-z0-9-]*$'
readonly RENOVATE_BRANCH_PATTERN='^renovate/.+$'
readonly RELEASE_BRANCH_PATTERN='^release-please--.+$'
# The pull request files API returns at most 3000 files and truncates silently. Files come back in
# path order, so governed paths such as `mise.toml`, `renovate.json5` and `governance/**` fall off
# first: a list this long may hide a governed file.
readonly FILES_API_LIMIT=3000

# Temporary file holding the per-file records. It is a script-level global, not a local of
# `check_governed_files`, so the EXIT trap still sees it after the function has returned.
RECORDS_FILE=''

die() {
  echo "governance: $*" >&2
  exit 1
}

cleanup() {
  [[ -z $RECORDS_FILE ]] || rm -f -- "$RECORDS_FILE"
}

# Succeeds when the path matches any governed glob.
is_governed() {
  first_governed_glob "$1" "${GLOBS[@]}" >/dev/null
}

# Reads the governed path globs into GLOBS, through the grammar in governed-paths.sh. A missing or unusable file has
# already been reported on standard error, so the gate fails closed.
load_globs() {
  local text glob
  text=$(governed_globs "$1") || exit 1
  GLOBS=()
  while IFS= read -r glob; do
    GLOBS+=("$glob")
  done <<<"$text"
}

validate_inputs() {
  [[ -n ${PR_FILES_JSON:-} && -f ${PR_FILES_JSON} ]] || die "PR_FILES_JSON does not name a readable file"
  jq -e 'type == "array" and all(.[]; type == "object" and (.filename | type) == "string")' \
    "$PR_FILES_JSON" >/dev/null 2>&1 ||
    die "the changed files list is malformed: expected a JSON array of objects with a filename"
  [[ -n ${PR_LABELS_JSON:-} ]] || die "PR_LABELS_JSON is not set"
  printf '%s' "$PR_LABELS_JSON" | jq -e 'type == "array" and all(.[]; type == "string")' >/dev/null 2>&1 ||
    die "the label list is malformed: expected a JSON array of strings"
  [[ -n ${PR_BASE_REPO:-} ]] || die "PR_BASE_REPO is not set"
}

has_label() {
  printf '%s' "$PR_LABELS_JSON" | jq -e --arg label "$1" 'index($label) != null' >/dev/null
}

# Succeeds when the path equals an entry of NEVER_EXEMPT_PATHS exactly (no glob matching); fails for an empty path.
is_never_exempt() {
  local entry
  [[ -n ${1:-} ]] || return 1
  for entry in "${NEVER_EXEMPT_PATHS[@]}"; do
    [[ $1 == "$entry" ]] && return 0
  done
  return 1
}

# Reads a diff on standard input and writes each line as a version-independent token stream: commit
# SHAs, digests and version numbers collapse to placeholders. With "lock" as the first argument the
# volatile fields of mise.lock and of the sidecar lockfiles under .mise/locks collapse too: sizes,
# GitHub release asset ids, npm integrity hashes, uv upload times and the two-character PyPI file
# path segments. The lock rules run first, so the generic rules never split an integrity hash. This
# is the only place that decides what "version-only" means.
normalize() {
  local mode=${1:-plain}
  local -a program=()
  if [[ $mode == lock ]]; then
    program+=(
      -e 's#sha512-[A-Za-z0-9+/]{20,}={0,2}#<INTEGRITY>#g'
      -e 's/[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\.[0-9]+)?Z/<TIME>/g'
      -e 's#(/packages/)[0-9a-f]{2}/[0-9a-f]{2}/#\1<H>/<H>/#g'
      -e 's/size = [0-9]+/size = <N>/g'
      -e 's#(releases/assets/)[0-9]+#\1<ID>#g'
    )
  fi
  program+=(
    -e 's/sha256:[0-9a-fA-F]{64}/<DIGEST>/g'
    -e 's/(^|[^0-9a-fA-F])[0-9a-fA-F]{64}($|[^0-9a-fA-F])/\1<DIGEST>\2/g'
    -e 's/(^|[^0-9a-fA-F])[0-9a-fA-F]{40}($|[^0-9a-fA-F])/\1<SHA>\2/g'
    -e 's/v?[0-9]+(\.[0-9]+)+([-+][0-9A-Za-z.]+)?/<VER>/g'
  )
  sed -E "${program[@]}"
}

# Succeeds for a file under .mise/locks/ that mise moved to a new versioned directory: both paths lie
# under .mise/locks/ and are equal once their versions are normalised, for example
# `.mise/locks/npm-renovate/44.252.7~aa673d8d/aube-lock.yaml` and the same path with 44.252.8.
is_sidecar_version_move() {
  local previous=$1 filename=$2
  [[ $previous == .mise/locks/* && $filename == .mise/locks/* ]] || return 1
  [[ $(normalize <<<"$previous") == "$(normalize <<<"$filename")" ]]
}

# Prints the lines of a patch that start with the given sign ("-" or "+"), without the sign. A files
# API patch holds hunks only, so everything before the first hunk header is skipped and every later
# line counts, including a removed line that itself starts with dashes.
changed_lines() {
  local patch=$1 sign=$2 in_hunk=0 line
  while IFS= read -r line || [[ -n $line ]]; do
    if [[ $line == '@@'* ]]; then
      in_hunk=1
    elif [[ $in_hunk -eq 1 && ${line:0:1} == "$sign" ]]; then
      printf '%s\n' "${line:1}"
    fi
  done <<<"$patch"
}

# Succeeds when a patch changes at least one line and the removed and added lines are equal after
# normalisation (as sorted multisets, so a moved or duplicated line does not hide a change).
is_version_only() {
  local patch=$1 mode=$2 removed added
  removed=$(changed_lines "$patch" - | normalize "$mode" | sort)
  added=$(changed_lines "$patch" + | normalize "$mode" | sort)
  [[ -n $removed || -n $added ]] && [[ $removed == "$added" ]]
}

# Rule 2: a pull request from a branch of this repository must use an allowed branch name.
check_branch_name() {
  if [[ ${PR_HEAD_REPO:-} != "$PR_BASE_REPO" ]]; then
    echo "governance: fork pull request, the branch naming rule does not apply"
    return 0
  fi
  local ref=${PR_HEAD_REF:-}
  if [[ -z $ref ]]; then
    echo "governance: the head ref of a same-repository pull request is empty" >&2
    return 1
  fi
  if [[ $ref =~ $PHASE_BRANCH_PATTERN || $ref =~ $RENOVATE_BRANCH_PATTERN ||
    $ref =~ $RELEASE_BRANCH_PATTERN ]]; then
    echo "governance: head branch name '$ref' is allowed"
    return 0
  fi
  {
    echo "governance: head branch name '$ref' is not allowed for a pull request from this repository."
    echo "governance: use gsd/phase-<number>-<slug>, renovate/* or release-please--*."
  } >&2
  return 1
}

# A list at the files API limit may hide a governed file, so it needs the label like any governed
# change, and the Renovate exemption is never consulted for it.
check_files_api_limit() {
  local count=$1
  if has_label "$APPROVAL_LABEL"; then
    echo "governance: the changed files list has $count entries, the files API limit;" \
      "the pull request carries '$APPROVAL_LABEL'"
    return 0
  fi
  {
    echo "governance: the changed files list has $count entries, reaches the files API limit of" \
      "$FILES_API_LIMIT and may hide governed files."
    echo "governance: review the full diff and add the '$APPROVAL_LABEL' label, or split the pull request."
  } >&2
  return 1
}

# Rule 1: label gate with the narrow Renovate exemption.
check_governed_files() {
  local count
  # Explicit exit: errexit is suspended inside a function called from an `||` list.
  count=$(jq 'length' "$PR_FILES_JSON") || return 1
  if [[ $count -ge $FILES_API_LIMIT ]]; then
    check_files_api_limit "$count"
    return
  fi

  # The records go to a file so that jq's exit status can be read before any entry is evaluated.
  # The status of a process substitution is lost and `set -e` does not see it, so a jq failure
  # part-way through the list used to end the loop quietly and drop the remaining entries (WR-05).
  # Errexit is suspended in this function because `main` calls it from an `||` list, so every
  # status is handled explicitly.
  RECORDS_FILE=$(mktemp "${TMPDIR:-/tmp}/check-pr-records.XXXXXX") || return 1
  local jq_status=0
  jq --raw-output0 '.[] | (
      .filename, (.previous_filename // ""), (.status // ""),
      (if (.patch | type) == "string" then "1" else "0" end), (.patch // "")
    )' "$PR_FILES_JSON" >"$RECORDS_FILE" || jq_status=$?
  if [[ $jq_status -ne 0 ]]; then
    echo "governance: the changed files list could not be read: jq exited with status $jq_status" >&2
    return 1
  fi

  local -a touched=() blocked=() exempt=() never_exempt=()
  local filename previous status has_patch patch is_touched is_exempt mode seen=0 hit
  while IFS= read -r -d '' filename &&
    IFS= read -r -d '' previous &&
    IFS= read -r -d '' status &&
    IFS= read -r -d '' has_patch &&
    IFS= read -r -d '' patch; do
    seen=$((seen + 1))
    is_touched=0
    if is_governed "$filename"; then
      is_touched=1
      touched+=("$filename")
    fi
    if [[ -n $previous ]] && is_governed "$previous"; then
      is_touched=1
      touched+=("$previous")
    fi
    [[ $is_touched -eq 1 ]] || continue

    hit=0
    if [[ ${PR_AUTHOR:-} == "$BOT_AUTHOR" ]]; then
      if is_never_exempt "$filename"; then
        never_exempt+=("$filename")
        hit=1
      fi
      if is_never_exempt "$previous"; then
        never_exempt+=("$previous")
        hit=1
      fi
    fi

    is_exempt=0
    if [[ $hit -eq 0 && ${PR_AUTHOR:-} == "$BOT_AUTHOR" && $has_patch == 1 ]] &&
      { [[ $status == modified && -z $previous ]] ||
        { [[ $status == renamed ]] && is_sidecar_version_move "$previous" "$filename"; }; }; then
      mode=plain
      [[ $filename == mise.lock || $filename == .mise/locks/* ]] && mode=lock
      if is_version_only "$patch" "$mode"; then
        is_exempt=1
      fi
    fi
    if [[ $is_exempt -eq 1 ]]; then
      exempt+=("$filename")
    else
      blocked+=("$filename")
      [[ -n $previous ]] && blocked+=("$previous")
    fi
  done <"$RECORDS_FILE"

  # A second guard, independent of jq's status: it catches a producer that exits 0 but writes fewer
  # records than the list has entries, or a torn record. Both sides count JSON array entries.
  if [[ $seen -ne $count ]]; then
    echo "governance: the changed files list could not be read completely:" \
      "$seen of $count entries were evaluated" >&2
    return 1
  fi

  if [[ ${#touched[@]} -eq 0 ]]; then
    echo "governance: no governed file is touched"
    return 0
  fi
  if has_label "$APPROVAL_LABEL"; then
    echo "governance: governed files are touched and the pull request carries '$APPROVAL_LABEL'"
    return 0
  fi
  if [[ ${#blocked[@]} -eq 0 ]]; then
    printf 'governance: version-only governed file: %s\n' "${exempt[@]}"
    echo "governance: every governed change is a version-only $BOT_AUTHOR bump, no label needed"
    return 0
  fi
  {
    echo "governance: this pull request touches governed files:"
    printf '  %s\n' "${blocked[@]}"
    if [[ ${#never_exempt[@]} -gt 0 ]]; then
      local path
      for path in "${never_exempt[@]}"; do
        echo "governance: never exempt: $path" \
          "(a GSD release replaces the hooks that run in the maintainer's Claude Code session)"
      done
    fi
    echo "governance: ask a maintainer to add the '$APPROVAL_LABEL' label, then this check re-runs."
  } >&2
  return 1
}

main() {
  trap cleanup EXIT
  load_globs "${GOVERNED_PATHS_FILE:-governance/governed-paths.txt}"
  validate_inputs

  local status=0
  check_branch_name || status=1
  check_governed_files || status=1
  return "$status"
}

main "$@"

#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Staged-file adapter for the Spotless IDE hook, called by the lefthook `format` job.
# Usage: scripts/format-staged.sh <repository-relative path>...
# Formats only the given files, through the same Spotless configuration that CI checks, so the
# result of the hook and of `./gradlew spotlessCheck` can never disagree. Paths that no longer
# exist (deletions) and documents owned by GSD (`.planning/` and `.claude/`, except the one
# project-owned `.claude/settings.json`) are skipped. A file no Spotless format targets is ignored
# by the IDE hook. Run it through `mise x --` so Node, shfmt and taplo from `mise.toml` are on PATH.
set -euo pipefail

readonly CLAUDE_SETTINGS=".claude/settings.json"

root="$(git rev-parse --show-toplevel)"
absolute_paths=()

for path in "$@"; do
  case "${path}" in
  "${CLAUDE_SETTINGS}") ;;
  .planning/* | .claude/*) continue ;;
  *,*)
    # The IDE hook takes a comma-separated list, so a comma inside a path cannot be expressed.
    echo "format-staged: cannot format a path that contains a comma: ${path}" >&2
    exit 1
    ;;
  esac
  [[ -f "${root}/${path}" ]] || continue
  absolute_paths+=("${root}/${path}")
done

if [[ ${#absolute_paths[@]} -eq 0 ]]; then
  exit 0
fi

paths_csv="$(
  IFS=,
  echo "${absolute_paths[*]}"
)"
cd "${root}"
./gradlew --quiet spotlessApply "-PspotlessIdeHook=${paths_csv}"

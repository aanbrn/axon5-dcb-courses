#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Runs the Qodana gate exactly as the CI `qodana` job does: the IntelliJ IDEA inspections of the
# profile in qodana.yaml, from the digest-pinned image named there, with nothing uploaded anywhere.
# Usage: scripts/qodana.sh [project-dir]
# With no argument it exports the committed HEAD into <work dir>/project and analyses that, so
# uncommitted changes are not analysed. Qodana writes `.idea/` into the directory it analyses, so the
# IDE's working tree is never used. With an argument it analyses that directory as it is and refuses
# one that already contains `.idea/`; CI passes its fresh checkout.
# QODANA_WORK_DIR defaults to ${XDG_CACHE_HOME:-$HOME/.cache}/axon5-dcb-courses/qodana and holds
# `cache/`, `results/` and `project/`. On colima it must be under $HOME, the only directory the VM
# shares. The exit status is Qodana's own: 0 when clean, 255 when the problems exceed `failThreshold`.
set -euo pipefail

# The one accepted form of the `linter:` value in qodana.yaml; it comes from pull request content.
readonly LINTER_PATTERN='^jetbrains/qodana-jvm-community:[0-9]{4}\.[0-9]+@sha256:[0-9a-f]{64}$'

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
work_dir="${QODANA_WORK_DIR:-${XDG_CACHE_HOME:-${HOME}/.cache}/axon5-dcb-courses/qodana}"

if [[ $# -gt 1 ]]; then
  echo "qodana: usage: scripts/qodana.sh [project-dir]" >&2
  exit 2
fi

if [[ $# -eq 0 ]]; then
  project_dir="${work_dir}/project"
  rm -rf -- "${project_dir}"
  mkdir -p "${project_dir}"
  git -C "${root}" archive --format=tar HEAD | tar -x -C "${project_dir}"
  echo "qodana: analysing the committed HEAD $(git -C "${root}" rev-parse --short HEAD)" >&2
else
  project_dir="$(cd "$1" && pwd)"
  if [[ -e "${project_dir}/.idea" ]]; then
    echo "qodana: refusing to analyse a directory that already contains .idea/" >&2
    exit 1
  fi
fi

if [[ ! -f "${project_dir}/qodana.yaml" ]]; then
  echo "qodana: qodana.yaml is missing from the project directory" >&2
  exit 1
fi

linter="$(sed -nE 's/^linter:[[:space:]]+([^[:space:]]+).*/\1/p' "${project_dir}/qodana.yaml" | head -n 1)"
if ! [[ "${linter}" =~ ${LINTER_PATTERN} ]]; then
  echo "qodana: qodana.yaml must pin linter as jetbrains/qodana-jvm-community:<year.minor>@sha256:<digest>" >&2
  exit 1
fi

if ! command -v docker >/dev/null; then
  echo "qodana: docker is not on PATH; install a Docker-compatible engine (see docs/tooling.md)" >&2
  exit 1
fi

rm -rf -- "${work_dir}/results"
mkdir -p "${work_dir}/results" "${work_dir}/cache"

# No -e variables and no token are passed: there is no Qodana Cloud and nothing is uploaded.
exec docker run --rm \
  --user "$(id -u):$(id -g)" \
  --volume "${project_dir}:/data/project" \
  --volume "${work_dir}/cache:/data/cache" \
  --volume "${work_dir}/results:/data/results" \
  "${linter}" \
  scan --print-problems --property=idea.headless.enable.statistics=false

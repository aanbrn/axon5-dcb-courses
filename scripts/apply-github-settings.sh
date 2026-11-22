#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Applies the declared GitHub repository settings and the `main` ruleset, or reports how the live
# repository differs from them (D-15). The desired state lives in `.github/rulesets/main.json` and
# `.github/repository-settings.json`; this script is the only tool that writes it to GitHub.
#
# Modes (exactly one; the default writes nothing):
#   --dry-run   print every delta, exit 0 (default)
#   --check     print every delta, exit 1 when there is any (read-only drift check)
#   --apply     change only what differs, re-read, fail when a delta remains, print bypass actors
# Options:
#   --repo owner/name   target repository (default: $GITHUB_REPOSITORY, else the current checkout)
#   --only SECTION      limit the comparison and the writes to ruleset, repository or security;
#                       repeatable, default is all three
# Exit codes: 0 success or no drift, 1 drift (--check) or failure, 2 usage error.
#
# Reads use `gh api` GET calls piped to `jq`; every write sits in apply_changes, which only the
# --apply mode reaches.
set -euo pipefail

# Byte-wise comparison and stable sorting regardless of the caller's locale.
export LC_ALL=C

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
readonly SCRIPT_DIR
readonly RULESET_FILE="$SCRIPT_DIR/../.github/rulesets/main.json"
readonly SETTINGS_FILE="$SCRIPT_DIR/../.github/repository-settings.json"

# jq helpers. `norm` drops the fields the server adds and sorts every list whose order carries no
# meaning, so two equal rulesets compare equal. `proj` keeps only the keys the desired document
# declares: the server also returns defaults and read-only fields this repository never sets.
JQ_LIB=''
read -r -d '' JQ_LIB <<'JQ' || true
def sort_params:
  (if has("required_status_checks") then .required_status_checks |= sort_by(.context) else . end)
  | (if has("allowed_merge_methods") then .allowed_merge_methods |= sort else . end);
def norm:
  del(.id, .node_id, ._links, .created_at, .updated_at, .source, .source_type,
    .current_user_can_bypass)
  | (if has("rules") then
    .rules |= (map(if has("parameters") then .parameters |= sort_params else . end)
      | sort_by(.type))
  else . end)
  | (if has("bypass_actors") then .bypass_actors |= sort_by(.actor_type, .actor_id) else . end);
def proj($d):
  if ($d | type) == "object" and type == "object" then
    . as $c | reduce ($d | keys_unsorted[]) as $k ({};
      if $c | has($k) then .[$k] = ($c[$k] | proj($d[$k])) else . end)
  elif ($d | type) == "array" and type == "array" and length == ($d | length) then
    [range(0; length) as $i | .[$i] | proj($d[$i])]
  else . end;
JQ
readonly JQ_LIB

usage() {
  cat <<'EOF'
Usage: apply-github-settings.sh [--dry-run | --check | --apply] [--repo owner/name]
                                [--only ruleset|repository|security]...

  --dry-run  print the deltas between the declared and the live settings; write nothing (default)
  --check    like --dry-run, but exit 1 when any delta exists
  --apply    change only what differs, then verify and print the ruleset bypass actors
  --repo     target repository (default: $GITHUB_REPOSITORY, else the current checkout)
  --only     limit the sections compared and applied; repeatable (default: all three)
EOF
}

usage_error() {
  echo "apply-github-settings: $*" >&2
  usage >&2
  exit 2
}

die() {
  echo "apply-github-settings: $*" >&2
  exit 1
}

MODE=''
REPO=''
ONLY=''

add_only() {
  case $1 in
  ruleset | repository | security) ONLY="$ONLY $1" ;;
  *) usage_error "unknown section for --only: $1 (expected ruleset, repository or security)" ;;
  esac
}

set_mode() {
  if [[ -n $MODE && $MODE != "$1" ]]; then
    usage_error "only one of --dry-run, --check and --apply may be given"
  fi
  MODE=$1
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case $1 in
    --dry-run | --check | --apply)
      set_mode "${1#--}"
      shift
      ;;
    --repo | --only)
      [[ $# -ge 2 ]] || usage_error "$1 needs a value"
      if [[ $1 == --repo ]]; then REPO=$2; else add_only "$2"; fi
      shift 2
      ;;
    --repo=*)
      REPO=${1#--repo=}
      shift
      ;;
    --only=*)
      add_only "${1#--only=}"
      shift
      ;;
    -h | --help)
      usage
      exit 0
      ;;
    *) usage_error "unknown argument: $1" ;;
    esac
  done
  MODE=${MODE:-dry-run}
}

# Succeeds when the section takes part in this run.
selected() {
  [[ -z $ONLY || " $ONLY " == *" $1 "* ]]
}

sections_label() {
  if [[ -z $ONLY ]]; then
    echo "ruleset repository security"
  else
    echo "${ONLY# }"
  fi
}

resolve_repo() {
  if [[ -z $REPO ]]; then
    REPO=${GITHUB_REPOSITORY:-}
  fi
  if [[ -z $REPO ]]; then
    REPO=$(gh repo view --json nameWithOwner | jq -r '.nameWithOwner') ||
      die "cannot determine the repository; pass --repo owner/name"
  fi
  [[ $REPO =~ ^[A-Za-z0-9._-]+/[A-Za-z0-9._-]+$ ]] || usage_error "invalid --repo value: $REPO"
}

api_get() {
  gh api "$1" || die "GET $1 failed"
}

# Prints true or false for an endpoint that answers 2xx when the feature is on and HTTP 404 when
# it is off (Dependabot alerts); any other failure aborts instead of reading as "off".
probe_status() {
  local out rc=0
  out=$(gh api "repos/$REPO/$1" 2>&1) || rc=$?
  if [[ $rc -eq 0 ]]; then
    echo true
  elif [[ $out == *"HTTP 404"* ]]; then
    echo false
  else
    die "GET repos/$REPO/$1 failed: $out"
  fi
}

# Prints the `enabled` flag of an endpoint that answers {"enabled": bool}.
probe_enabled() {
  api_get "repos/$REPO/$1" | jq -r '.enabled'
}

# --- delta computation --------------------------------------------------------------------------

QUIET=0
DELTA_COUNT=0
RULESET_ACTION=none
RULESET_ID=''
REPO_PATCH='{}'
SA_PATCH='{}'
TOGGLES=''
REPO_JSON=''

emit() {
  DELTA_COUNT=$((DELTA_COUNT + 1))
  if [[ $QUIET -eq 0 ]]; then
    echo "$1"
  fi
}

emit_lines() {
  local line
  while IFS= read -r line; do
    if [[ -n $line ]]; then
      emit "$line"
    fi
  done <<<"$1"
}

ruleset_id() {
  local list
  # A separate assignment, so a failed GET aborts instead of reading as "no ruleset". The explicit
  # exit matters: this function runs inside a command substitution, which does not inherit -e.
  list=$(api_get "repos/$REPO/rulesets") || exit 1
  jq -r --arg name "$1" \
    '[.[] | select(.name == $name and ((.source_type // "Repository") == "Repository"))]
      | first | .id // empty' <<<"$list"
}

check_ruleset() {
  local name desired current projected
  name=$(jq -r '.name' "$RULESET_FILE")
  RULESET_ID=$(ruleset_id "$name")
  if [[ -z $RULESET_ID ]]; then
    RULESET_ACTION=create
    emit "ruleset $name: create"
    return 0
  fi
  desired=$(jq -S "$JQ_LIB"' norm' "$RULESET_FILE")
  current=$(api_get "repos/$REPO/rulesets/$RULESET_ID")
  projected=$(jq -S --argjson d "$desired" "$JQ_LIB"' norm | proj($d)' <<<"$current")
  if [[ $projected != "$desired" ]]; then
    RULESET_ACTION=update
    emit "ruleset $name: update"
    if [[ $QUIET -eq 0 ]]; then
      diff -U1 --label live --label declared <(printf '%s\n' "$projected") \
        <(printf '%s\n' "$desired") | tail -n +3 | sed 's/^/    /' || true
    fi
  fi
}

check_repository() {
  local desired
  desired=$(jq -c '.repository' "$SETTINGS_FILE")
  REPO_PATCH=$(jq -c --argjson cur "$REPO_JSON" \
    'with_entries(select(.value != $cur[.key]))' <<<"$desired")
  emit_lines "$(jq -r --argjson cur "$REPO_JSON" \
    'to_entries[] | select(.value != $cur[.key])
      | "repository.\(.key): \($cur[.key]) -> \(.value)"' <<<"$desired")"
}

check_security() {
  local key want have
  for key in secret_scanning secret_scanning_push_protection; do
    want=$(jq -r --arg k "$key" '.security[$k]' "$SETTINGS_FILE")
    have=$(jq -r --arg k "$key" '.security_and_analysis[$k].status // "unknown"' <<<"$REPO_JSON")
    if [[ $have != "$want" ]]; then
      emit "security.$key: $have -> $want"
      SA_PATCH=$(jq -c --arg k "$key" --arg w "$want" '. + {($k): {status: $w}}' <<<"$SA_PATCH")
    fi
  done
  for key in vulnerability_alerts private_vulnerability_reporting automated_security_fixes; do
    want=$(jq -r --arg k "$key" '.security[$k]' "$SETTINGS_FILE")
    case $key in
    vulnerability_alerts) have=$(probe_status "${key//_/-}") ;;
    *) have=$(probe_enabled "${key//_/-}") ;;
    esac
    if [[ $have != "$want" ]]; then
      emit "security.$key: $have -> $want"
      TOGGLES="$TOGGLES $key=$want"
    fi
  done
}

compute_deltas() {
  DELTA_COUNT=0
  RULESET_ACTION=none
  RULESET_ID=''
  REPO_PATCH='{}'
  SA_PATCH='{}'
  TOGGLES=''
  if selected repository || selected security; then
    REPO_JSON=$(api_get "repos/$REPO")
  fi
  if selected ruleset; then check_ruleset; fi
  if selected repository; then check_repository; fi
  if selected security; then check_security; fi
}

# --- writes (reached only through --apply) ------------------------------------------------------

apply_changes() {
  local pair key want
  case $RULESET_ACTION in
  create)
    echo "applying: create ruleset"
    gh api -X POST "repos/$REPO/rulesets" --input "$RULESET_FILE" >/dev/null
    ;;
  update)
    echo "applying: update ruleset $RULESET_ID"
    gh api -X PUT "repos/$REPO/rulesets/$RULESET_ID" --input "$RULESET_FILE" >/dev/null
    ;;
  *) ;;
  esac
  if [[ $REPO_PATCH != '{}' ]]; then
    echo "applying: update repository settings"
    printf '%s' "$REPO_PATCH" | gh api -X PATCH "repos/$REPO" --input - >/dev/null
  fi
  if [[ $SA_PATCH != '{}' ]]; then
    echo "applying: update secret scanning settings"
    jq -c '{security_and_analysis: .}' <<<"$SA_PATCH" |
      gh api -X PATCH "repos/$REPO" --input - >/dev/null
  fi
  for pair in $TOGGLES; do
    key=${pair%%=*}
    want=${pair#*=}
    echo "applying: $key -> $want"
    if [[ $want == true ]]; then
      gh api -X PUT "repos/$REPO/${key//_/-}" >/dev/null
    else
      gh api -X DELETE "repos/$REPO/${key//_/-}" >/dev/null
    fi
  done
}

print_bypass_actors() {
  local name id
  name=$(jq -r '.name' "$RULESET_FILE")
  id=$(ruleset_id "$name")
  [[ -n $id ]] || die "ruleset $name not found after apply"
  echo "ruleset $name bypass actors (confirm the repository admin role):"
  api_get "repos/$REPO/rulesets/$id" |
    jq -r '(.bypass_actors // [])[] | "  \(.actor_type) \(.actor_id) mode=\(.bypass_mode)"'
}

# --- main ---------------------------------------------------------------------------------------

main() {
  parse_args "$@"
  command -v gh >/dev/null 2>&1 || die "gh is required"
  command -v jq >/dev/null 2>&1 || die "jq is required"
  resolve_repo
  echo "repository: $REPO, mode: $MODE, sections: $(sections_label)"
  compute_deltas
  case $MODE in
  dry-run)
    echo "$DELTA_COUNT delta(s); nothing written (dry-run)"
    ;;
  check)
    if [[ $DELTA_COUNT -gt 0 ]]; then
      echo "drift: $DELTA_COUNT delta(s) between the declared and the live settings" >&2
      exit 1
    fi
    echo "no drift"
    ;;
  apply)
    if [[ $DELTA_COUNT -eq 0 ]]; then
      echo "nothing to apply"
    else
      apply_changes
      QUIET=1
      compute_deltas
      if [[ $DELTA_COUNT -gt 0 ]]; then
        QUIET=0
        echo "deltas remain after apply:" >&2
        compute_deltas >&2
        exit 1
      fi
      echo "applied; the live settings match the declared ones"
    fi
    if selected ruleset; then print_bypass_actors; fi
    ;;
  *) die "unreachable mode: $MODE" ;;
  esac
}

main "$@"

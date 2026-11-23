#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Dependency-update advisory (UPS-04, D-32): when a change moves a pinned version, lists the upstream
# records (docs/upstream/*.md) whose workaround that version might make removable, with their removal
# criteria, in the job summary and as warning annotations. Advisory only: it never fails on a match.
#
# Usage: changed-dependencies.sh <base-sha> [<head-ref>]   (head-ref defaults to HEAD)
#
# Changed keys, each compared between the base and the head revision (so a moved line or a comment
# change is not a change):
#   <key>             a changed, added or removed key of [versions] in gradle/libs.versions.toml
#   <key>             a changed, added or removed key of [tools] in mise.toml (quotes removed)
#   gradle            gradle/wrapper/gradle-wrapper.properties differs
#   java              .java-version differs
#   node              .nvmrc differs
#   action:<o>/<r>    the set of `uses: <o>/<r>@<ref>` lines under .github/workflows/ differs for <o>/<r>
#
# A record is reported when its front matter `status` is open, reported or fixed-upstream and its
# `product` equals a changed key. Records are read from the head revision. Products without a pinned
# key (colima, GSD) never match.
#
# Record text is repository-controlled. It reaches only the Markdown summary, where every line carries a
# prefix (`### `, `- `, `> `) so that none can begin a workflow command, and the warning annotation,
# where `%`, CR and LF are escaped (and `:` and `,` in the title property) so a record cannot inject
# annotations or commands.
#
# A missing, empty or all-zero base (a new branch, workflow_dispatch) or a base that is not in the
# checkout (a force push) skips the advisory with exit 0. Exit codes: 0 success or skipped, 2 usage
# error or an unreadable record (the fail-closed parsing the governance check shares).
set -euo pipefail

# Byte-wise comparison and stable sorting regardless of the caller's locale.
export LC_ALL=C

readonly STATUS_PATTERN='^(open|reported|fixed-upstream)$'

usage() {
  echo "usage: changed-dependencies.sh <base-sha> [<head-ref>]" >&2
  exit 2
}

# GitHub workflow-command escaping: `data` for the message, `property` additionally for a property value.
# The replacements sit in variables because quoting inside ${var//x/y} differs between bash versions.
escape_data() {
  local value=$1 percent='%' cr=$'\r' lf=$'\n'
  value=${value//$percent/%25}
  value=${value//$cr/%0D}
  value=${value//$lf/%0A}
  printf '%s' "$value"
}

escape_property() {
  local value colon=':' comma=','
  value=$(escape_data "$1")
  value=${value//$colon/%3A}
  value=${value//$comma/%2C}
  printf '%s' "$value"
}

# code_span <text>: the text as a Markdown code span, with backticks replaced so it cannot end the span.
code_span() {
  local value=$1 backtick='`' quote="'"
  value=${value//$backtick/$quote}
  printf '%s%s%s' "$backtick" "$value" "$backtick"
}

# Shared awk helpers: trim, and cut a trailing `# comment` that sits outside quotes.
AWK_LIB=''
read -r -d '' AWK_LIB <<'AWK' || true
function trim(s) {
  sub(/^[[:space:]]+/, "", s)
  sub(/[[:space:]]+$/, "", s)
  return s
}
function strip_comment(s,    i, c, q) {
  q = ""
  for (i = 1; i <= length(s); i++) {
    c = substr(s, i, 1)
    if (q == "") {
      if (c == "\"" || c == "\047") {
        q = c
      } else if (c == "#") {
        return trim(substr(s, 1, i - 1))
      }
    } else if (c == q) {
      q = ""
    }
  }
  return trim(s)
}
AWK
readonly AWK_LIB

# Prints `<tag> TAB <key> TAB <value>` for every `key = value` line inside [<section>] of a TOML file.
AWK_TOML=''
read -r -d '' AWK_TOML <<'AWK' || true
{
  sub(/\r$/, "")
  line = strip_comment($0)
}
line == "" { next }
line ~ /^\[/ {
  in_section = (line == "[" section "]")
  next
}
!in_section { next }
{
  first = substr(line, 1, 1)
  if (first == "\"" || first == "\047") {
    rest = substr(line, 2)
    end = index(rest, first)
    if (end == 0) next
    key = substr(rest, 1, end - 1)
    after = trim(substr(rest, end + 1))
    if (substr(after, 1, 1) != "=") next
    value = trim(substr(after, 2))
  } else {
    eq = index(line, "=")
    if (eq == 0) next
    key = trim(substr(line, 1, eq - 1))
    value = trim(substr(line, eq + 1))
  }
  gsub(/\t/, " ", value)
  print tag "\t" key "\t" value
}
AWK
readonly AWK_TOML

# Reads tagged pairs (B before, H after) and prints every key that is added, removed or has another value.
AWK_PAIR_DIFF=''
read -r -d '' AWK_PAIR_DIFF <<'AWK' || true
$1 == "B" { before[$2] = $3; seen[$2] = 1 }
$1 == "H" { after[$2] = $3; seen[$2] = 1 }
END {
  for (key in seen) {
    if (!(key in before) || !(key in after) || before[key] != after[key]) print key
  }
}
AWK
readonly AWK_PAIR_DIFF

# Prints `<owner>/<repo>@<ref>` for every remote `uses:` line of a workflow file.
AWK_USES=''
read -r -d '' AWK_USES <<'AWK' || true
{
  sub(/\r$/, "")
  if (!match($0, /^[[:space:]]*(-[[:space:]]+)?uses:[[:space:]]*/)) next
  rest = substr($0, RLENGTH + 1)
  sub(/^["\047]/, "", rest)
  at = index(rest, "@")
  if (at == 0) next
  path = substr(rest, 1, at - 1)
  ref = substr(rest, at + 1)
  sub(/["\047#[:space:]].*$/, "", ref)
  if (path ~ /^\./ || path ~ /^docker:/) next
  if (split(path, parts, "/") < 2) next
  print parts[1] "/" parts[2] "@" ref
}
AWK
readonly AWK_USES

# Prints id, product, status and title on four lines, then the Removal criteria section without its
# leading and trailing blank lines. Prints nothing when the front matter is missing or unterminated.
AWK_RECORD=''
read -r -d '' AWK_RECORD <<'AWK' || true
function scalar(v) {
  v = trim(v)
  if (substr(v, 1, 1) == "\"") {
    v = substr(v, 2)
    end = index(v, "\"")
    return end == 0 ? v : substr(v, 1, end - 1)
  }
  return strip_comment(v)
}
{ sub(/\r$/, "") }
NR == 1 {
  if ($0 != "---") exit
  state = 1
  next
}
state == 1 {
  if ($0 == "---") {
    state = 2
    next
  }
  if (match($0, /^[A-Za-z][A-Za-z0-9_-]*:/)) {
    name = substr($0, 1, RLENGTH - 1)
    value = scalar(substr($0, RLENGTH + 1))
    if (name == "id") id = value
    else if (name == "product") product = value
    else if (name == "status") status = value
  }
  next
}
state == 2 {
  if (title == "" && $0 ~ /^# UP-[0-9][0-9][0-9][0-9]:[[:space:]]+/) {
    title = $0
    sub(/^# UP-[0-9][0-9][0-9][0-9]:[[:space:]]+/, "", title)
    title = trim(title)
    next
  }
  if ($0 ~ /^##[[:space:]]+Removal criteria[[:space:]]*$/) {
    in_criteria = 1
    next
  }
  if ($0 ~ /^##?[[:space:]]/) in_criteria = 0
  if (in_criteria) lines[++count] = $0
}
END {
  if (state != 2) exit
  print id
  print product
  print status
  print title
  first = 1
  while (first <= count && trim(lines[first]) == "") first++
  last = count
  while (last >= first && trim(lines[last]) == "") last--
  for (i = first; i <= last; i++) print lines[i]
}
AWK
readonly AWK_RECORD

# tagged_pairs <tag> <rev> <path> <section>: the key/value pairs of one TOML section at one revision.
tagged_pairs() {
  { git show "$2:$3" 2>/dev/null || true; } |
    awk -v tag="$1" -v section="$4" "${AWK_LIB}
${AWK_TOML}"
}

# toml_changed <path> <section>: the keys of the section that differ between base and head.
toml_changed() {
  {
    tagged_pairs B "$BASE_SHA" "$1" "$2"
    tagged_pairs H "$HEAD_SHA" "$1" "$2"
  } | awk -F '\t' "${AWK_PAIR_DIFF}"
}

# file_changed <path>: succeeds when the file differs between base and head (or exists in only one).
file_changed() {
  ! git diff --quiet "$BASE_SHA" "$HEAD_SHA" -- "$1"
}

# workflow_uses <rev>: every remote `uses:` reference of the workflows at the revision.
workflow_uses() {
  local path
  git ls-tree -r --name-only "$1" -- .github/workflows | while IFS= read -r path; do
    case "$path" in
    *.yml | *.yaml) git show "$1:$path" | awk "${AWK_USES}" ;;
    *) ;;
    esac
  done
}

# action_changed: `action:<owner>/<repo>` for every action whose set of pinned references differs.
action_changed() {
  {
    workflow_uses "$BASE_SHA" | sort -u | sed 's/^/B\t/'
    workflow_uses "$HEAD_SHA" | sort -u | sed 's/^/H\t/'
  } | awk -F '\t' '
    $1 == "B" { before[$2] = 1; seen[$2] = 1 }
    $1 == "H" { after[$2] = 1; seen[$2] = 1 }
    END {
      for (pin in seen) {
        if (!(pin in before) || !(pin in after)) {
          split(pin, parts, "@")
          print "action:" parts[1]
        }
      }
    }'
}

collect_changed_keys() {
  toml_changed gradle/libs.versions.toml versions
  toml_changed mise.toml tools
  if file_changed gradle/wrapper/gradle-wrapper.properties; then echo gradle; fi
  if file_changed .java-version; then echo java; fi
  if file_changed .nvmrc; then echo node; fi
  action_changed
}

# --- arguments ----------------------------------------------------------------------------------

[[ $# -ge 1 && $# -le 2 ]] || usage
BASE=$1
HEAD_REF=${2:-HEAD}
[[ $BASE != -* && $HEAD_REF != -* ]] || usage

REPO_ROOT=$(git rev-parse --show-toplevel)
cd "$REPO_ROOT"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
REPORT="$TMP/report.md"
WARNINGS="$TMP/warnings.txt"
: >"$REPORT"
: >"$WARNINGS"

# Appends the Markdown report to the job summary, or prints it when there is none, then prints the
# warning annotations. Every exit path goes through here.
finish() {
  if [[ -n ${GITHUB_STEP_SUMMARY:-} ]]; then
    cat "$REPORT" >>"$GITHUB_STEP_SUMMARY"
  else
    cat "$REPORT"
  fi
  cat "$WARNINGS"
  exit 0
}

# A one-line status for the job log; without a summary file the report itself goes to standard output.
say() {
  if [[ -n ${GITHUB_STEP_SUMMARY:-} ]]; then
    echo "$1"
  fi
}

skip() {
  printf '## Upstream records advisory\n\n%s\n' "$1" >>"$REPORT"
  say "$1"
  finish
}

if [[ -z $BASE || $BASE =~ ^0+$ ]]; then
  skip "No base commit; upstream advisory skipped."
fi
BASE_SHA=$(git rev-parse --verify --quiet "${BASE}^{commit}") ||
  skip "Base commit is not available in this checkout; upstream advisory skipped."
HEAD_SHA=$(git rev-parse --verify --quiet "${HEAD_REF}^{commit}") ||
  {
    echo "changed-dependencies.sh: cannot resolve the head revision '${HEAD_REF}'" >&2
    exit 2
  }

# --- changed keys -------------------------------------------------------------------------------

CHANGED=$(collect_changed_keys | sort -u)

{
  printf '## Upstream records advisory\n\n'
  if [[ -z $CHANGED ]]; then
    printf 'No dependency version changed.\n\n'
  else
    keys=''
    while IFS= read -r key; do
      keys+="$(code_span "$key"), "
    done <<<"$CHANGED"
    printf 'Changed keys: %s\n\n' "${keys%, }"
  fi
} >>"$REPORT"

# --- records ------------------------------------------------------------------------------------

matched=0
while IFS= read -r record_path; do
  [[ -n $record_path ]] || continue
  parsed=$(git show "${HEAD_SHA}:${record_path}" | awk "${AWK_LIB}
${AWK_RECORD}")
  id=$(printf '%s\n' "$parsed" | sed -n 1p)
  product=$(printf '%s\n' "$parsed" | sed -n 2p)
  status=$(printf '%s\n' "$parsed" | sed -n 3p)
  title=$(printf '%s\n' "$parsed" | sed -n 4p)
  if [[ -z $id || -z $product || -z $status ]]; then
    echo "changed-dependencies.sh: ${record_path} has no readable front matter with id, product and status" >&2
    exit 2
  fi
  [[ $status =~ $STATUS_PATTERN ]] || continue
  grep -Fxq -- "$product" <<<"$CHANGED" || continue

  matched=$((matched + 1))
  criteria=$(printf '%s\n' "$parsed" | sed -n '5,$p')
  {
    printf '### %s: %s\n\n' "$id" "$title"
    printf -- '- Product: %s\n' "$(code_span "$product")"
    printf -- '- Status: %s\n' "$(code_span "$status")"
    printf -- '- Record: %s\n\n' "$(code_span "$record_path")"
    printf 'Removal criteria:\n\n'
    if [[ -n $criteria ]]; then
      while IFS= read -r line; do
        printf '> %s\n' "$line"
      done <<<"$criteria"
    else
      printf '> (the record has no removal criteria text)\n'
    fi
    printf '\n'
  } >>"$REPORT"
  printf '::warning title=%s::%s\n' \
    "$(escape_property "Upstream record ${id}")" \
    "$(escape_data "${title} — check the removal criteria in ${record_path}")" >>"$WARNINGS"
done < <(git ls-tree --name-only "$HEAD_SHA" -- docs/upstream/ | grep -E '^docs/upstream/[^/]+\.md$' || true)

if [[ $matched -eq 0 ]]; then
  printf 'No upstream records affected.\n' >>"$REPORT"
  say "No upstream records affected."
else
  say "${matched} upstream record(s) affected; see the summary for their removal criteria."
fi
finish

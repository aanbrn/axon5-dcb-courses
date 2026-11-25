# SPDX-License-Identifier: MIT
# shellcheck shell=bash
# The grammar of governance/governed-paths.txt and the glob matcher, in one place for every shell consumer: the pull
# request gate (check-pr.sh) and the Claude Code hooks (ask-governed-edit.sh, ask-governed-merge.sh). Source this file;
# it defines functions only and keeps no state. The grammar is shared with `GovernedPaths` in build-logic's governance
# package, which backs `checkClaudeAskRules`; both must change together (WR-04).
#
# Callers export LC_ALL=C first, so every comparison is byte-wise and a look-alike Unicode character never matches an
# ASCII pattern. The functions run under bash 3.2, the bash macOS ships, because Claude Code starts the hooks with the
# first bash on PATH.

# governed_globs <file>: prints the entries of a governed paths file, one per line, and succeeds. The grammar:
#   1. Lines are split on LF only.
#   2. Leading and trailing space, tab and carriage return are stripped and nothing else
#      (`[:space:]` would also strip vertical tab and form feed).
#   3. Empty lines and lines starting with `#` are ignored.
#   4. Every other entry must be printable ASCII (0x20 to 0x7E), checked byte-wise under LC_ALL=C.
#      A NUL byte, which `read` would silently drop, is turned into a non-printable byte first.
#   5. A file without entries is unusable.
# For a missing or unusable file nothing goes to standard output: every problem goes to standard error, offending lines
# in ascending order, and the function fails.
governed_globs() {
  local file=$1 line number=0 entries='' failed=0
  if [[ ${LC_ALL:-} != C ]]; then
    echo "governance: reading the governed paths needs LC_ALL=C" >&2
    return 1
  fi
  if [[ ! -f $file ]]; then
    echo "governance: governed paths file not found: $file" >&2
    return 1
  fi
  while IFS= read -r line || [[ -n $line ]]; do
    number=$((number + 1))
    while [[ $line == [$' \t\r']* ]]; do
      line=${line#?}
    done
    while [[ $line == *[$' \t\r'] ]]; do
      line=${line%?}
    done
    if [[ -z $line || $line == '#'* ]]; then
      continue
    fi
    if [[ $line == *[![:print:]]* ]]; then
      echo "governance: $file line $number: entry contains a character outside printable ASCII" >&2
      failed=1
    else
      entries+=$line$'\n'
    fi
  done < <(tr '\0' '\001' <"$file")
  [[ $failed -eq 0 ]] || return 1
  if [[ -z $entries ]]; then
    echo "governance: governed paths file lists no governed path: $file" >&2
    return 1
  fi
  printf '%s' "$entries"
}

# glob_match <path> <glob>: succeeds when the path matches the glob. `**` matches any characters including `/`; every
# other character is literal, so a plain name matches exactly.
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

# first_governed_glob <path> <glob>...: prints the first glob the path matches and succeeds, or fails when it matches
# none.
first_governed_glob() {
  local path=$1 glob
  shift
  for glob in "$@"; do
    if glob_match "$path" "$glob"; then
      printf '%s\n' "$glob"
      return 0
    fi
  done
  return 1
}

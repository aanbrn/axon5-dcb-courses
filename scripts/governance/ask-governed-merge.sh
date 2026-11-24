#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Claude Code PreToolUse hook for Bash (ADR-0015, AIDEV-08): asks the author before a merge that brings changes to
# governed paths into a worktree.
#
# Ask rules and the edit hook see only the file tools, and the edit hook does not ask in an agent worktree. Work that
# reaches a governed file in an agent worktree, through the file tools or a shell command (sed, a heredoc, a script),
# enters the main checkout through a merge, and this hook stands at that point: it reads the Bash command and asks when
# the command merges a ref that changes a governed path since its merge base with the target's HEAD. It recognises
#   - `git ... merge <ref>...`, with git's global options such as `-C <dir>` and `--attr-source <tree>`, inside
#     pipelines, lists, subshells, command substitutions, `bash -c` (behind option clusters such as `-euo pipefail` and
#     options with a value such as `--rcfile <file>`), `eval`, heredocs and here-strings fed to a shell, after `cd`,
#     `env` (also `env -S`), `sudo`, `doas` and similar wrappers with their option clusters, and behind programs that
#     take git or `bash -c` among their arguments (`mise x -- git merge`, `stdbuf`, `caffeinate`, `script`);
#   - `worktree.cleanup-wave --manifest <file>` of GSD, which merges every branch of the manifest's `worktrees` list.
# `git merge --abort` and `--quit` merge nothing. When the hook cannot resolve a merge, because the ref, the manifest or
# the directory comes from a shell expansion (an unquoted brace expansion such as `{a,b}` counts as one), does not
# exist or cannot be read, it asks as well. So it does when the directory is changed by `env -C`, `--chdir` or
# `sudo -D`, or when the command sets GIT_DIR, GIT_WORK_TREE, GIT_COMMON_DIR or GIT_CEILING_DIRECTORIES anywhere, so
# that the repository is unknown. It also asks when the command mentions a merge anywhere and a shell reads its script
# from standard input (a pipe, `/dev/stdin`, a process substitution, a heredoc fed to another command), a shell option
# is unknown or comes from a shell expansion, or a script, eval text or program comes from a shell expansion, because
# that text is not visible to the hook. `<<` in arithmetic is no heredoc. It never allows a call; a command without a
# merge gets no output.
#
# The governed list is ${CLAUDE_PROJECT_DIR}/governance/governed-paths.txt, as for the edit hook. The hook runs on every
# Bash command, so a command that does not mention `merge` or `cleanup-wave` leaves before anything else is loaded. It
# looks for the words as written and again with quotes, backslashes, `$` and line continuations removed, so a word that
# the shell assembles (`git mer''ge`) takes the full check; the early probe below says how.
# A command that merges and also runs a git subcommand that can move a ref or HEAD (fetch, pull, checkout, switch, reset,
# rebase, a forced branch or tag and the like) asks too, because the refs are checked as they are before the command.
# Outside its view on purpose (ADR-0015): `git pull`, cherry-pick, rebase, reset and checkout of paths without a merge
# in the same command, git aliases, script files that a shell or `source` runs, programs that run git themselves, a
# merge word that a parameter expansion, a brace expansion inside one word (`mer{g,}e`) or ANSI-C escapes assemble, a
# merge option value that splits into several words, and a shell that reads standard input behind a program the hook
# does not know as a wrapper; the pull request gate and the maintainer's review stay the control there.
set -uo pipefail
export LC_ALL=C
unset CDPATH

# The answers before the shared scripts are loaded; hook-common.sh defines the same text for later failures.
EARLY_FAILURE_ASK='{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"ask",'
EARLY_FAILURE_ASK+='"permissionDecisionReason":"Governance hook: the hook could not read its input or load its shared '
EARLY_FAILURE_ASK+='scripts, so it asks. Confirm that this command merges no governed path '
EARLY_FAILURE_ASK+='(governance/governed-paths.txt)."}}'
readonly EARLY_FAILURE_ASK

INPUT=$(cat) || {
  printf '%s\n' "$EARLY_FAILURE_ASK"
  exit 0
}
# The early probe: a command that cannot mention a merge leaves here, before the shared scripts are loaded. JSON keeps
# letters unescaped, so the words merge and cleanup-wave appear as written, unless a \u escape hides them (full check) or
# the shell assembles them, which quotes, backslashes, `$` and line continuations can do (`git mer''ge`, `git m\erge`,
# `git mer\<newline>ge`). Without any of these three characters in the raw input the command has no shell quoting at all
# (a quote that belongs to the command appears as \", which holds a backslash), so it leaves at once. Otherwise one sed
# and one tr drop a line continuation (the JSON text backslash backslash backslash n) and then every quote, backslash and
# `$`, and the words are searched again. Deleting characters can only join text, and neither word holds a deleted
# character, so the filter can only add full checks. A failing filter takes the full check, too. The test stays linear
# on purpose: bash pattern substitution on the whole input is quadratic in bash 3.2 and would run into the hook's
# timeout, which blocks the call.
case $INPUT in
*merge* | *cleanup-wave* | *'\u'*) ;;
*)
  case $INPUT in
  *"'"* | *\\* | *'$'*) ;;
  *) exit 0 ;;
  esac
  if PROBE=$(printf '%s' "$INPUT" | sed 's/\\\\\\n//g' | tr -d '\047\042\134\044'); then
    case $PROBE in
    *merge* | *cleanup-wave*) ;;
    *) exit 0 ;;
    esac
  fi
  ;;
esac

SCRIPT_DIR=$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P) || SCRIPT_DIR=''
# shellcheck source=/dev/null
if [[ -z $SCRIPT_DIR ]] || ! source "$SCRIPT_DIR/hook-common.sh" || ! source "$SCRIPT_DIR/governed-paths.sh"; then
  printf '%s\n' "$EARLY_FAILURE_ASK"
  exit 0
fi
install_fail_closed

# How deep `bash -c`, `eval` and heredocs fed to a shell may nest before the hook gives up and asks.
readonly MAX_DEPTH=3
# How many governed files a reason lists before it says how many more there are.
readonly MAX_LISTED=20
# How many distinct problems a reason lists before it says how many more there are.
readonly MAX_PROBLEMS=10
readonly ASSIGNMENT_PATTERN='^[A-Za-z_][A-Za-z0-9_]*='
readonly DIGITS_PATTERN='^[0-9]+$'
# The first word of a simple command after which `((` starts an arithmetic command (or none: `((i++))`, `for ((...))`).
readonly ARITHMETIC_COMMAND_PATTERN='^(if|while|until|elif|then|do|else|for|!|\{)?$'
# Unquoted text that bash expands as a brace expansion: braces around a comma or a `..`, as in `{a,b}` and `{1..3}`.
readonly BRACE_EXPANSION_PATTERN='[{][^{}]*(,|\.\.)[^{}]*[}]'
# An assignment or a bare name of a variable that moves git to another repository, which the hook cannot follow.
readonly GIT_REPOSITORY_VARIABLE_PATTERN='^(GIT_DIR|GIT_WORK_TREE|GIT_COMMON_DIR|GIT_CEILING_DIRECTORIES)(=|$)'

# What the analysis found: governed files a merge brings in, and merges it could not check.
HITS=()
PROBLEMS=()
# 1 when the analysis met a merge it could check or fail to check (not `--abort` and `--quit`), and 1 when the command
# sets one of the variables of GIT_REPOSITORY_VARIABLE_PATTERN anywhere. The two are combined in main, regardless of
# order: a variable exported in one segment, or in a nested `bash -c`, reaches merges anywhere after it.
MERGE_SEEN=0
GIT_REPOSITORY_ENV=0
# The first git subcommand of the command that can move a ref or HEAD (see ref_mover_name), or empty.
REF_MOVER=''
# 1 when the whole command mentions a merge (see mentions_merge). A script, eval text, program or git subcommand that
# comes from a shell expansion, or from standard input, is text from elsewhere in the command, so such a source matters
# when the command mentions a merge anywhere.
COMMAND_MENTIONS_MERGE=0

# --- lexer ----------------------------------------------------------------------------------------------------------
# lex_command <text> splits a shell command into words without running anything, close enough to bash to find the
# simple commands it runs. It sets
#   TOKENS          each word with its quotes removed;
#   TOKEN_SEGMENT   the number of the simple command the word belongs to;
#   TOKEN_DYNAMIC   1 when the word holds an expansion the hook cannot evaluate ($NAME, $(...), `...`, $'...', or an
#                   unquoted brace expansion such as {a,b} and {1..3}, which bash turns into several words);
#   TOKEN_REDIRECT  1 when the word is the target of a redirection, 2 when it is the text of a here-string;
#   HEREDOC_SHELL_SEGMENTS  the numbers of the simple commands that run a shell on a heredoc;
#   TOKEN_DEPTH     how many command substitutions enclose the word;
#   NESTED_SCRIPTS  the bodies of heredocs fed to a shell, to be analysed like the command;
#   LEX_INCOMPLETE  1 when quoting, a substitution or an arithmetic is left open, an arithmetic closes with a single `)`,
#                   or a heredoc does not get its delimiter line;
#   LEX_HEREDOC_SUBSTITUTION  1 when the body of a heredoc with an unquoted delimiter, which the shell expands, holds a
#                   command substitution and mentions a merge.
# An unquoted `<<` is a heredoc except in arithmetic: inside `$((...))`, and inside `((...))` at the start of a command
# (also after `if`, `while`, `for` and the like), `<` and `>` are operators, so the text there registers no redirect or
# heredoc and is dropped when the arithmetic closes; only a command substitution inside it is read as a command. A
# heredoc whose delimiter line never comes makes the lexing incomplete, as does an arithmetic that closes with a single
# `)` (bash reads it as a subshell in a subshell) or that never closes.
# The text of a command substitution is a simple command of its own; the word that holds it continues afterwards as a
# dynamic word. The words of one simple command are contiguous among the words of the same TOKEN_DEPTH. The helpers
# lex_* share the state of lex_command through bash's dynamic scoping. Runs of ordinary characters are taken in one
# step, because every character access costs a pass over the text in bash.

lex_end_word() {
  if [[ $in_word -eq 1 ]]; then
    # `shadow` holds the unquoted text of the word, which is all that brace expansion looks at.
    if [[ $shadow == *'{'* && $shadow =~ $BRACE_EXPANSION_PATTERN ]]; then
      dynamic=1
    fi
    TOKENS+=("$word")
    TOKEN_SEGMENT+=("$segment")
    TOKEN_DYNAMIC+=("$dynamic")
    TOKEN_REDIRECT+=("$redirect")
    TOKEN_DEPTH+=($((${#contexts[@]} - 1)))
    if [[ $redirect -eq 0 && -z $segment_command && ! $word =~ $ASSIGNMENT_PATTERN ]]; then
      segment_command=${word##*/}
    fi
    redirect=0
  fi
  word=''
  shadow=''
  in_word=0
  dynamic=0
}

lex_end_segment() {
  lex_end_word
  segment_count=$((segment_count + 1))
  segment=$segment_count
  segment_command=''
  redirect=0
}

# lex_open_substitution <S|B>: starts a command substitution, $(...) or `...`, inside the current word.
lex_open_substitution() {
  saved_words+=("$word")
  saved_segments+=("$segment")
  saved_commands+=("$segment_command")
  word=''
  in_word=0
  dynamic=0
  lex_end_segment
  contexts+=("$1")
  depths+=(0)
}

lex_close_substitution() {
  local top=$((${#contexts[@]} - 1)) saved=$((${#saved_words[@]} - 1))
  lex_end_word
  unset 'contexts[top]' 'depths[top]'
  word=${saved_words[saved]}
  segment=${saved_segments[saved]}
  segment_command=${saved_commands[saved]}
  unset 'saved_words[saved]' 'saved_segments[saved]' 'saved_commands[saved]'
  in_word=1
  dynamic=1
}

# lex_open_dollar_paren: starts what `$(` at i opens: an arithmetic expansion for `$((`, whose text `<` and `>` are
# operators in, or a command substitution. Moves i past the opening.
lex_open_dollar_paren() {
  if [[ ${text:i+2:1} == '(' ]]; then
    lex_open_substitution A
    i=$((i + 3))
  else
    lex_open_substitution S
    i=$((i + 2))
  fi
}

# lex_close_arithmetic: ends an arithmetic expansion or command at the closing parenthesis at i. Bash closes it with
# `))`; a single `)` at its outer level means the text was a subshell in a subshell, which the lexer did not read as
# commands, so the lexing is incomplete. The text of the arithmetic is dropped; a command substitution inside it was
# opened as one and is analysed. An arithmetic expansion continues the word that holds it as a dynamic part, and an
# arithmetic command leaves no word.
lex_close_arithmetic() {
  local kind=${contexts[${#contexts[@]} - 1]}
  if [[ ${text:i+1:1} == ')' ]]; then
    i=$((i + 2))
  else
    LEX_INCOMPLETE=1
    i=$((i + 1))
  fi
  word=''
  in_word=0
  dynamic=0
  lex_close_substitution
  if [[ $kind == C ]]; then
    word=''
    shadow=''
    in_word=0
    dynamic=0
  fi
}

# lex_quoted <ansi>: reads a single-quoted string that starts at i; with ansi=1 a backslash escapes the next character,
# as in $'...', and the word becomes dynamic because the escapes are not decoded.
lex_quoted() {
  local ansi=$1 c rest
  i=$((i + 1))
  in_word=1
  if [[ $ansi -eq 0 ]]; then
    rest=${text:i}
    if [[ $rest == *"'"* ]]; then
      rest=${rest%%"'"*}
      word+=$rest
      i=$((i + ${#rest} + 1))
      return 0
    fi
    word+=$rest
    i=$length
    LEX_INCOMPLETE=1
    return 0
  fi
  while ((i < length)); do
    c=${text:i:1}
    if [[ $c == "'" ]]; then
      i=$((i + 1))
      return 0
    fi
    if [[ $ansi -eq 1 && $c == "\\" ]]; then
      word+=${text:i:2}
      dynamic=1
      i=$((i + 2))
      continue
    fi
    word+=$c
    i=$((i + 1))
  done
  LEX_INCOMPLETE=1
}

# lex_heredoc_delimiter: reads the delimiter word of a heredoc that starts at i, with its quotes removed, and whether it
# was quoted, which keeps the shell from expanding the body.
lex_heredoc_delimiter() {
  local c delimiter='' quoted=0
  while [[ ${text:i:1} == ' ' || ${text:i:1} == $'\t' ]]; do
    i=$((i + 1))
  done
  while ((i < length)); do
    c=${text:i:1}
    case $c in
    ' ' | $'\t' | $'\n' | ';' | '&' | '|' | '<' | '>' | '(' | ')') break ;;
    "'" | '"')
      quoted=1
      i=$((i + 1))
      while ((i < length)) && [[ ${text:i:1} != "$c" ]]; do
        delimiter+=${text:i:1}
        i=$((i + 1))
      done
      i=$((i + 1))
      ;;
    "\\")
      quoted=1
      delimiter+=${text:i+1:1}
      i=$((i + 2))
      ;;
    *)
      delimiter+=$c
      i=$((i + 1))
      ;;
    esac
  done
  if [[ -z $delimiter ]]; then
    LEX_INCOMPLETE=1
  fi
  heredoc_delimiters+=("$delimiter")
  heredoc_quoted+=("$quoted")
}

# lex_redirect: reads a redirection operator at i. A word of digits right before it is a file descriptor, and the word
# after it is a target, not an argument. A heredoc is registered so that its body is skipped at the next newline.
lex_redirect() {
  if [[ $in_word -eq 1 && $dynamic -eq 0 && $word =~ $DIGITS_PATTERN ]]; then
    word=''
    shadow=''
    in_word=0
  else
    lex_end_word
  fi
  if [[ ${text:i:3} == '<<<' ]]; then
    i=$((i + 3))
    redirect=2
  elif [[ ${text:i:2} == '<<' ]]; then
    i=$((i + 2))
    if [[ ${text:i:1} == '-' ]]; then
      heredoc_strips+=(1)
      i=$((i + 1))
    else
      heredoc_strips+=(0)
    fi
    case $segment_command in
    bash | sh | zsh | dash | ksh)
      heredoc_shells+=(1)
      HEREDOC_SHELL_SEGMENTS+=("$segment")
      ;;
    *) heredoc_shells+=(0) ;;
    esac
    lex_heredoc_delimiter
  else
    i=$((i + 1))
    while [[ ${text:i:1} == '>' || ${text:i:1} == '&' || ${text:i:1} == '|' ]]; do
      i=$((i + 1))
    done
    redirect=1
  fi
}

# lex_heredoc_bodies: after a newline, skips the bodies of the heredocs registered on the line before and keeps those
# fed to a shell in NESTED_SCRIPTS.
lex_heredoc_bodies() {
  local index line body rest candidate found
  for ((index = 0; index < ${#heredoc_delimiters[@]}; index++)); do
    body=''
    found=0
    while ((i < length)); do
      rest=${text:i}
      line=${rest%%$'\n'*}
      i=$((i + ${#line} + 1))
      candidate=$line
      if [[ ${heredoc_strips[index]} -eq 1 ]]; then
        while [[ $candidate == $'\t'* ]]; do
          candidate=${candidate#?}
        done
      fi
      if [[ $candidate == "${heredoc_delimiters[index]}" ]]; then
        found=1
        break
      fi
      body+=$line$'\n'
    done
    # The text ended before the delimiter line. Bash would take the rest as the body, but a `<<` that never gets its
    # delimiter is more likely one the lexer misread, and then the rest holds commands it did not see.
    if [[ $found -eq 0 ]]; then
      LEX_INCOMPLETE=1
    fi
    if [[ ${heredoc_shells[index]} -eq 1 ]]; then
      NESTED_SCRIPTS+=("$body")
    elif [[ ${heredoc_quoted[index]} -eq 0 && ($body == *"\$("* || $body == *"\`"*) ]] && mentions_merge "$body"; then
      LEX_HEREDOC_SUBSTITUTION=1
    fi
  done
  heredoc_delimiters=()
  heredoc_quoted=()
  heredoc_strips=()
  heredoc_shells=()
}

lex_command() {
  local text=$1 length=${#1} i=0 c next context top rest chunk
  local word='' shadow='' in_word=0 dynamic=0 redirect=0 segment=0 segment_count=0 segment_command=''
  local -a contexts=(U) depths=(0) saved_words=() saved_segments=() saved_commands=()
  local -a heredoc_delimiters=() heredoc_quoted=() heredoc_strips=() heredoc_shells=()
  TOKENS=()
  TOKEN_SEGMENT=()
  TOKEN_DYNAMIC=()
  TOKEN_REDIRECT=()
  TOKEN_DEPTH=()
  NESTED_SCRIPTS=()
  HEREDOC_SHELL_SEGMENTS=()
  LEX_INCOMPLETE=0
  LEX_HEREDOC_SUBSTITUTION=0
  while ((i < length)); do
    c=${text:i:1}
    top=$((${#contexts[@]} - 1))
    context=${contexts[top]}
    if [[ $context == D ]]; then
      case $c in
      '"')
        unset 'contexts[top]' 'depths[top]'
        i=$((i + 1))
        ;;
      "\\")
        next=${text:i+1:1}
        case $next in
        '$' | '`' | '"' | "\\") word+=$next ;;
        $'\n') ;;
        *) word+="\\$next" ;;
        esac
        i=$((i + 2))
        ;;
      '$')
        if [[ ${text:i+1:1} == '(' ]]; then
          lex_open_dollar_paren
        else
          word+='$'
          dynamic=1
          i=$((i + 1))
        fi
        ;;
      '`')
        lex_open_substitution B
        i=$((i + 1))
        ;;
      *)
        rest=${text:i}
        chunk=${rest%%[\"\$\`\\]*}
        word+=$chunk
        i=$((i + ${#chunk}))
        ;;
      esac
      continue
    fi
    if [[ $context == A || $context == C ]]; then
      # Arithmetic: `<` and `>` are operators, so nothing here is a redirection or a heredoc. Only a command
      # substitution, which runs, is opened; the rest is dropped when the arithmetic closes.
      case $c in
      '(')
        depths[top]=$((depths[top] + 1))
        i=$((i + 1))
        ;;
      ')')
        if [[ ${depths[top]} -gt 0 ]]; then
          depths[top]=$((depths[top] - 1))
          i=$((i + 1))
        else
          lex_close_arithmetic
        fi
        ;;
      '$')
        if [[ ${text:i+1:1} == '(' ]]; then
          lex_open_dollar_paren
        else
          i=$((i + 1))
        fi
        ;;
      '`')
        lex_open_substitution B
        i=$((i + 1))
        ;;
      "\\") i=$((i + 2)) ;;
      *)
        rest=${text:i}
        chunk=${rest%%[\(\)\$\`\\]*}
        i=$((i + ${#chunk}))
        ;;
      esac
      continue
    fi
    case $c in
    ' ' | $'\t')
      lex_end_word
      i=$((i + 1))
      ;;
    $'\n')
      lex_end_segment
      i=$((i + 1))
      lex_heredoc_bodies
      ;;
    ';' | '|')
      lex_end_segment
      i=$((i + 1))
      ;;
    '&')
      if [[ ${text:i+1:1} == '>' ]]; then
        lex_end_word
        i=$((i + 1))
        lex_redirect
      else
        lex_end_segment
        i=$((i + 1))
      fi
      ;;
    '(')
      if [[ $in_word -eq 0 && ${text:i+1:1} == '(' && $segment_command =~ $ARITHMETIC_COMMAND_PATTERN ]]; then
        lex_open_substitution C
        i=$((i + 2))
      else
        if [[ $context == S ]]; then
          depths[top]=$((depths[top] + 1))
        fi
        lex_end_segment
        i=$((i + 1))
      fi
      ;;
    ')')
      if [[ $context == S && ${depths[top]} -eq 0 ]]; then
        lex_close_substitution
      else
        if [[ $context == S ]]; then
          depths[top]=$((depths[top] - 1))
        fi
        lex_end_segment
      fi
      i=$((i + 1))
      ;;
    '`')
      if [[ $context == B ]]; then
        lex_close_substitution
      else
        lex_open_substitution B
      fi
      i=$((i + 1))
      ;;
    '<' | '>') lex_redirect ;;
    "'") lex_quoted 0 ;;
    '"')
      contexts+=(D)
      depths+=(0)
      in_word=1
      i=$((i + 1))
      ;;
    "\\")
      next=${text:i+1:1}
      if [[ $next != $'\n' ]]; then
        word+=$next
        in_word=1
      fi
      i=$((i + 2))
      ;;
    '$')
      next=${text:i+1:1}
      if [[ $next == '(' ]]; then
        lex_open_dollar_paren
      elif [[ $next == "'" ]]; then
        i=$((i + 1))
        dynamic=1
        lex_quoted 1
      else
        word+='$'
        dynamic=1
        in_word=1
        i=$((i + 1))
      fi
      ;;
    '#')
      if [[ $in_word -eq 0 ]]; then
        rest=${text:i}
        rest=${rest%%$'\n'*}
        i=$((i + ${#rest}))
      else
        word+=$c
        shadow+=$c
        i=$((i + 1))
      fi
      ;;
    *)
      rest=${text:i}
      chunk=${rest%%[[:space:]\;\&\|\(\)\<\>\'\"\$\`\\]*}
      # A character that ends a run without being special, such as a carriage return, is taken alone.
      [[ -n $chunk ]] || chunk=$c
      word+=$chunk
      shadow+=$chunk
      in_word=1
      i=$((i + ${#chunk}))
      ;;
    esac
  done
  lex_end_segment
  # A heredoc registered on the last line has no body and no delimiter line.
  if [[ ${#contexts[@]} -ne 1 || ${#heredoc_delimiters[@]} -gt 0 ]]; then
    LEX_INCOMPLETE=1
  fi
}

# --- analysis ---------------------------------------------------------------------------------------------------------

# check_merge <directory> <ref> <optional>: records the governed files the ref changes since its merge base with HEAD
# of the repository that contains the directory. A ref that does not resolve is a problem, unless it is optional.
check_merge() {
  local directory=$1 ref=$2 optional=$3 top changed rest item
  if [[ $ref == -* ]]; then
    PROBLEMS+=("cannot resolve the merged ref '$ref'")
    return 0
  fi
  if ! top=$(git -C "$directory" rev-parse --show-toplevel 2>/dev/null); then
    PROBLEMS+=("cannot find the git repository of $directory")
    return 0
  fi
  if ! git -C "$top" rev-parse --verify --quiet "$ref^{commit}" >/dev/null 2>&1; then
    if [[ $optional -eq 0 ]]; then
      PROBLEMS+=("cannot resolve the merged ref '$ref' in $top")
    fi
    return 0
  fi
  # `-z` ends every name with a NUL, which the command substitution would drop, so it becomes \001 first. Renames are
  # split into a deletion and an addition, so a file moved out of a governed path still counts.
  if ! changed=$(git -C "$top" diff --no-ext-diff --no-renames --name-only -z "HEAD...$ref" 2>/dev/null |
    tr '\0' '\001'); then
    PROBLEMS+=("cannot list what '$ref' changes since its merge base with HEAD in $top")
    return 0
  fi
  rest=$changed
  while [[ $rest == *$'\001'* ]]; do
    item=${rest%%$'\001'*}
    rest=${rest#*$'\001'}
    if match_governed '' "$item"; then
      HITS+=("$item (from $ref)")
    fi
  done
}

# mentions_merge <word>...: succeeds when a word mentions a merge, so that an unresolvable command around it matters.
# Text that holds quotes, backslashes, `$` or newlines is searched again with these characters removed, because a shell
# joins `mer''ge` and `mer\<newline>ge` to a merge word. The search is linear (one tr, no pattern substitution), and a
# failing tr counts as a mention.
mentions_merge() {
  local word plain
  for word in "$@"; do
    case $word in
    *merge* | *cleanup-wave*) return 0 ;;
    *"'"* | *'"'* | *\\* | *'$'* | *$'\n'*)
      plain=$(printf '%s' "$word" | tr -d '\047\042\134\044\012') || return 0
      case $plain in
      *merge* | *cleanup-wave*) return 0 ;;
      esac
      ;;
    esac
  done
  return 1
}

# The analysis of one simple command reads WORDS and WORD_DYNAMIC from analyze_words and the current directory `cwd`
# of analyze_command, which `cd` updates; an empty `cwd` means it is unknown.

analyze_cleanup_wave() {
  local index manifest='' manifest_dynamic=0 base=$cwd base_dynamic=0 found=0 path branches branch
  for ((index = 0; index < ${#WORDS[@]}; index++)); do
    case ${WORDS[index]} in
    worktree.cleanup-wave) found=1 ;;
    cleanup-wave) [[ $index -eq 0 || ${WORDS[index - 1]} != worktree ]] || found=1 ;;
    --manifest)
      manifest=${WORDS[index + 1]:-}
      manifest_dynamic=${WORD_DYNAMIC[index + 1]:-0}
      ;;
    --manifest=*)
      manifest=${WORDS[index]#--manifest=}
      manifest_dynamic=${WORD_DYNAMIC[index]}
      ;;
    --cwd)
      join_path base "$cwd" "${WORDS[index + 1]:-}"
      base_dynamic=${WORD_DYNAMIC[index + 1]:-0}
      ;;
    --cwd=*)
      join_path base "$cwd" "${WORDS[index]#--cwd=}"
      base_dynamic=${WORD_DYNAMIC[index]}
      ;;
    esac
  done
  # Without --manifest, cleanup-wave refuses to run and merges nothing.
  [[ $found -eq 1 && -n $manifest ]] || return 0
  MERGE_SEEN=1
  if [[ $manifest_dynamic -eq 1 || $base_dynamic -eq 1 || -z $base ]]; then
    PROBLEMS+=("cannot resolve the manifest of worktree.cleanup-wave: $manifest")
    return 0
  fi
  join_path path "$base" "$manifest"
  if ! branches=$(jq -r '(if type == "array" then . else .worktrees end)
      | if type == "array" then .[] else error("no worktrees list") end | objects | .branch | strings' \
    "$path" 2>/dev/null); then
    PROBLEMS+=("cannot read the worktree.cleanup-wave manifest $path")
    return 0
  fi
  while IFS= read -r branch; do
    if [[ -n $branch ]]; then
      check_merge "$base" "$branch" 0
    fi
  done <<<"$branches"
}

# ref_mover_name <index of a git subcommand>: sets REF_MOVER to the subcommand (with its qualifier) when it can move a ref
# or HEAD to a commit that is not a descendant of the current one, so that a merge in the same command may see another
# state than the hook checks: fetch, pull, checkout, switch, reset, rebase, update-ref, symbolic-ref, replace, bisect,
# filter-branch and remote always; branch and tag with a force option (branch also -M and -C, which overwrite); `stash
# branch`; `worktree add`. Left out on purpose: commit, revert, cherry-pick, am and merge, which only add commits on top
# of HEAD and keep every merge base; stash push and pop; and `branch -d` and `tag -d`, after which a merge of that name
# either fails or resolves the same ref the hook resolves. It reads WORDS of analyze_git.
ref_mover_name() {
  local index=$1 subcommand=${WORDS[$1]} word letters=f position
  case $subcommand in
  fetch | pull | checkout | switch | reset | rebase | update-ref | symbolic-ref | replace | bisect | filter-branch | \
    remote)
    REF_MOVER=$subcommand
    ;;
  stash)
    if [[ ${WORDS[index + 1]:-} == branch ]]; then
      REF_MOVER='stash branch'
    fi
    ;;
  worktree)
    if [[ ${WORDS[index + 1]:-} == add ]]; then
      REF_MOVER='worktree add'
    fi
    ;;
  branch | tag)
    if [[ $subcommand == branch ]]; then
      letters=fMC
    fi
    for ((position = index + 1; position < ${#WORDS[@]}; position++)); do
      word=${WORDS[position]}
      case $word in
      --force)
        REF_MOVER="$subcommand --force"
        return 0
        ;;
      --*) ;;
      -?*)
        if [[ $word == -*[$letters]* ]]; then
          REF_MOVER="$subcommand $word"
          return 0
        fi
        ;;
      esac
    done
    ;;
  esac
}

# analyze_git <index of git>: finds `merge` after git's global options and checks every ref it merges.
analyze_git() {
  local index=$(($1 + 1)) count=${#WORDS[@]} directory=$cwd repository_known=1 word cluster
  local abort=0 continue_merge=0 options_done=0 ref_index
  local -a refs=() ref_dynamic=()
  while ((index < count)); do
    word=${WORDS[index]}
    case $word in
    -C)
      if [[ ${WORD_DYNAMIC[index + 1]:-0} -eq 1 ]]; then
        directory=''
      else
        join_path directory "$directory" "${WORDS[index + 1]:-}"
      fi
      index=$((index + 2))
      ;;
    -c | --namespace | --config-env | --super-prefix | --attr-source) index=$((index + 2)) ;;
    --git-dir | --work-tree)
      repository_known=0
      index=$((index + 2))
      ;;
    --git-dir=* | --work-tree=*)
      repository_known=0
      index=$((index + 1))
      ;;
    -*) index=$((index + 1)) ;;
    *) break ;;
    esac
  done
  ((index < count)) || return 0
  if [[ ${WORD_DYNAMIC[index]} -eq 1 ]]; then
    if [[ $COMMAND_MENTIONS_MERGE -eq 1 ]]; then
      PROBLEMS+=("git runs a subcommand given by a shell expansion: ${WORDS[index]}")
    fi
    return 0
  fi
  if [[ -z $REF_MOVER ]]; then
    ref_mover_name "$index"
  fi
  [[ ${WORDS[index]} == merge ]] || return 0
  index=$((index + 1))
  while ((index < count)); do
    word=${WORDS[index]}
    if [[ $options_done -eq 1 || $word != -* || ${WORD_DYNAMIC[index]} -eq 1 ]]; then
      refs+=("$word")
      ref_dynamic+=("${WORD_DYNAMIC[index]}")
      index=$((index + 1))
      continue
    fi
    case $word in
    --) options_done=1 ;;
    --abort | --quit) abort=1 ;;
    --continue) continue_merge=1 ;;
    -m | --message | -F | --file | -s | --strategy | -X | --strategy-option | --into-name | --cleanup)
      index=$((index + 1))
      ;;
    --*) ;;
    *)
      # A cluster of short options: -S takes the rest as its key; m, F, s and X take the rest or the next word.
      cluster=${word#-}
      while [[ -n $cluster ]]; do
        case ${cluster:0:1} in
        S) break ;;
        m | F | s | X)
          if [[ ${#cluster} -eq 1 ]]; then
            index=$((index + 1))
          fi
          break
          ;;
        esac
        cluster=${cluster:1}
      done
      ;;
    esac
    index=$((index + 1))
  done
  [[ $abort -eq 0 ]] || return 0
  MERGE_SEEN=1
  if [[ $ARGUMENTS_FROM_STDIN -eq 1 ]]; then
    PROBLEMS+=("git merge takes further refs from xargs")
    return 0
  fi
  if [[ $repository_known -eq 0 || -z $directory ]]; then
    PROBLEMS+=("cannot tell which repository git merge runs in")
    return 0
  fi
  if [[ $continue_merge -eq 1 ]]; then
    check_merge "$directory" MERGE_HEAD 1
    return 0
  fi
  if [[ ${#refs[@]} -eq 0 ]]; then
    check_merge "$directory" '@{upstream}' 0
    return 0
  fi
  for ((ref_index = 0; ref_index < ${#refs[@]}; ref_index++)); do
    if [[ ${ref_dynamic[ref_index]} -eq 1 ]]; then
      PROBLEMS+=("cannot resolve the merged ref '${refs[ref_index]}': it holds a shell expansion")
    else
      check_merge "$directory" "${refs[ref_index]}" 0
    fi
  done
}

# analyze_stdin_script: a shell or `source` reads its script from standard input. A heredoc that the lexer registered for
# this very shell was analysed with the command already. A here-string is the script itself and is analysed here. Any
# other source of standard input (a pipe, a process substitution, a heredoc fed through a wrapper) cannot be read, which
# matters when the command mentions a merge anywhere. It reads the current segment of analyze_command.
analyze_stdin_script() {
  local index shell_heredoc=0 here_index=-1 here_string
  for index in ${heredoc_segments[@]+"${heredoc_segments[@]}"}; do
    if [[ $index == "$segment" ]]; then
      shell_heredoc=1
    fi
  done
  [[ $shell_heredoc -eq 0 ]] || return 0
  for ((index = 0; index < ${#herestring_segments[@]}; index++)); do
    if [[ ${herestring_segments[index]} == "$segment" ]]; then
      here_index=$index
    fi
  done
  if [[ $here_index -ge 0 ]]; then
    here_string=${herestring_words[here_index]}
    if [[ ${herestring_dynamic[here_index]} -eq 1 ]]; then
      if [[ $COMMAND_MENTIONS_MERGE -eq 1 ]]; then
        PROBLEMS+=("cannot read a shell script that holds a shell expansion: $here_string")
      fi
    else
      analyze_command "$here_string" "$cwd" $((DEPTH + 1))
    fi
    return 0
  fi
  if [[ $COMMAND_MENTIONS_MERGE -eq 1 ]]; then
    PROBLEMS+=("a shell reads its script from standard input, which the hook cannot read")
  fi
}

# is_stdin_operand <word>: succeeds for the file names that stand for standard input.
is_stdin_operand() {
  case $1 in
  /dev/stdin | /dev/fd/[0-9]*) return 0 ;;
  esac
  return 1
}

# analyze_shell <index of the shell> [embedded]: analyses the script of `bash -c <script>` and similar shells. Without
# -c the shell reads standard input when it has option -s, no operand, or /dev/stdin as the operand; a script file is
# not analysed and stays with the programs that run git themselves (ADR-0015). A shell that only appears among the
# arguments of another program (embedded=1) is not asked for its standard input.
analyze_shell() {
  local index=$(($1 + 1)) embedded=${2:-0} count=${#WORDS[@]} word has_script=0 has_stdin_option=0 letters
  # The options end at the first word that is none: `--` and a lone `-` end them too. bash takes a value for each o and
  # O of a short cluster (`-euo pipefail`, `-oo pipefail errexit`, `+eo pipefail`), and for --rcfile and --init-file.
  while ((index < count)); do
    word=${WORDS[index]}
    if [[ ${WORD_DYNAMIC[index]} -eq 1 ]]; then
      # A word from an expansion may be an option, an option value or the script, so the hook cannot tell the script.
      if [[ $COMMAND_MENTIONS_MERGE -eq 1 ]]; then
        if [[ $has_script -eq 1 ]]; then
          PROBLEMS+=("cannot read a shell script that holds a shell expansion: $word")
        else
          PROBLEMS+=("cannot tell where the options of ${WORDS[$1]} end: they come from a shell expansion")
        fi
      fi
      return 0
    fi
    case $word in
    -- | -)
      index=$((index + 1))
      break
      ;;
    --rcfile | --init-file) index=$((index + 2)) ;;
    --login | --noprofile | --norc | --posix | --restricted | --verbose | --version | --help | --debug | --debugger | \
      --dump-strings | --dump-po-strings | --pretty-print | --noediting | --wordexp | --protected | --rcfile=* | \
      --init-file=*)
      index=$((index + 1))
      ;;
    --*)
      if [[ $COMMAND_MENTIONS_MERGE -eq 1 ]]; then
        PROBLEMS+=("cannot tell where the options of ${WORDS[$1]} end: unknown option $word")
      fi
      return 0
      ;;
    -* | +*)
      if [[ $word == -*c* ]]; then
        has_script=1
      fi
      if [[ $word == -*s* ]]; then
        has_stdin_option=1
      fi
      letters=${word//[!oO]/}
      index=$((index + 1 + ${#letters}))
      ;;
    *) break ;;
    esac
  done
  if [[ $has_script -eq 1 ]]; then
    ((index < count)) || return 0
    if [[ ${WORD_DYNAMIC[index]} -eq 1 ]]; then
      if [[ $COMMAND_MENTIONS_MERGE -eq 1 ]]; then
        PROBLEMS+=("cannot read a shell script that holds a shell expansion: ${WORDS[index]}")
      fi
      return 0
    fi
    analyze_command "${WORDS[index]}" "$cwd" $((DEPTH + 1))
    return 0
  fi
  [[ $embedded -eq 0 ]] || return 0
  if [[ $has_stdin_option -eq 1 ]] || ((index >= count)) || is_stdin_operand "${WORDS[index]}"; then
    analyze_stdin_script
  fi
}

# analyze_source <index of source>: `source` and `.` read a script file; standard input or a process substitution (no
# operand is left of it) is a script the hook cannot read.
analyze_source() {
  local index=$(($1 + 1)) count=${#WORDS[@]}
  while ((index < count)) && [[ ${WORDS[index]} == -?* ]]; do
    index=$((index + 1))
  done
  if ((index >= count)) || is_stdin_operand "${WORDS[index]}"; then
    analyze_stdin_script
  fi
}

# analyze_eval <index of eval>: analyses the words eval joins into a command.
analyze_eval() {
  local index=$(($1 + 1)) script=''
  for (( ; index < ${#WORDS[@]}; index++)); do
    if [[ ${WORD_DYNAMIC[index]} -eq 1 ]]; then
      if [[ $COMMAND_MENTIONS_MERGE -eq 1 ]]; then
        PROBLEMS+=("cannot read an eval command that holds a shell expansion")
      fi
      return 0
    fi
    script+=" ${WORDS[index]}"
  done
  analyze_command "$script" "$cwd" $((DEPTH + 1))
}

# analyze_env_split <value> <dynamic> <index of the first word after the value>: analyses the script that `env -S`,
# `-Svalue`, `-iSvalue`, `--split-string value` or `--split-string=value` builds from the value (read by
# read_wrapper_option) and the words after it.
analyze_env_split() {
  local script=$1 dynamic=$2 next=$3 count=${#WORDS[@]}
  for (( ; next < count; next++)); do
    script+=" ${WORDS[next]}"
    if [[ ${WORD_DYNAMIC[next]} -eq 1 ]]; then
      dynamic=1
    fi
  done
  if [[ $dynamic -eq 1 ]]; then
    if [[ $COMMAND_MENTIONS_MERGE -eq 1 ]]; then
      PROBLEMS+=("cannot read an env -S command that holds a shell expansion: $script")
    fi
    return 0
  fi
  analyze_command "$script" "$cwd" $((DEPTH + 1))
}

# analyze_embedded <index of the program>: a program that is none of those analysed above may run git or a shell with
# its arguments (`mise x -- git merge`, `stdbuf`, `caffeinate`, `script`, `watch`). The first argument whose basename is
# git is analysed as git, and the first shell as `bash -c`. An argument list that merely names git and merge, such as
# `echo git merge x`, is checked as well, which only adds prompts.
analyze_embedded() {
  local index count=${#WORDS[@]} git_index=-1 shell_index=-1
  for ((index = $1 + 1; index < count; index++)); do
    if [[ ${WORD_DYNAMIC[index]} -eq 1 ]]; then
      # A brace expansion such as {git,merge,x} or an expansion that holds the word git can name git or a merge.
      if [[ $COMMAND_MENTIONS_MERGE -eq 1 ]]; then
        if mentions_merge "${WORDS[index]}" || [[ ${WORDS[index]} == *git* ]]; then
          PROBLEMS+=("a program's argument holds a shell expansion that may name git or a merge: ${WORDS[index]}")
        fi
      fi
      continue
    fi
    case ${WORDS[index]##*/} in
    git) [[ $git_index -ge 0 ]] || git_index=$index ;;
    bash | sh | zsh | dash | ksh) [[ $shell_index -ge 0 ]] || shell_index=$index ;;
    esac
  done
  if [[ $git_index -ge 0 ]]; then
    analyze_git "$git_index"
  fi
  if [[ $shell_index -ge 0 ]]; then
    analyze_shell "$shell_index" 1
  fi
}

# read_wrapper_option <env|sudo|doas> <index of an option word>: reads one option of env, sudo or doas from WORDS, which
# may be a long option, or a cluster of short options in which the first letter that takes a value takes the rest of the
# word, or the next word when nothing is left. It sets
#   OPTION_NEXT         the index of the first word after the option and its value;
#   OPTION_CLEARS_CWD   1 when the option changes the directory the command runs in (env -C and --chdir, sudo -D and
#                       --chdir), which makes the repository unknown;
#   OPTION_INTERACTIVE  1 for the sudo and doas options that start a shell (-s, -i, --shell, --login);
#   OPTION_SPLIT        1 for env -S and --split-string, with OPTION_SPLIT_VALUE and OPTION_SPLIT_DYNAMIC.
read_wrapper_option() {
  local kind=$1 index=$2 word=${WORDS[$2]} cluster letter values dynamic
  OPTION_NEXT=$((index + 1))
  OPTION_CLEARS_CWD=0
  OPTION_INTERACTIVE=0
  OPTION_SPLIT=0
  OPTION_SPLIT_VALUE=''
  OPTION_SPLIT_DYNAMIC=0
  if [[ $word == --* ]]; then
    case $word in
    --chdir=*)
      [[ $kind == doas ]] || OPTION_CLEARS_CWD=1
      ;;
    --split-string=*)
      if [[ $kind == env ]]; then
        OPTION_SPLIT=1
        OPTION_SPLIT_VALUE=${word#--split-string=}
        OPTION_SPLIT_DYNAMIC=${WORD_DYNAMIC[index]}
      fi
      ;;
    --*=*) ;;
    --chdir)
      [[ $kind == doas ]] || OPTION_CLEARS_CWD=1
      OPTION_NEXT=$((index + 2))
      ;;
    --split-string)
      if [[ $kind == env ]]; then
        OPTION_SPLIT=1
        OPTION_SPLIT_VALUE=${WORDS[index + 1]:-}
        OPTION_SPLIT_DYNAMIC=${WORD_DYNAMIC[index + 1]:-0}
        OPTION_NEXT=$((index + 2))
      fi
      ;;
    --shell | --login) OPTION_INTERACTIVE=1 ;;
    *)
      # The long options that take a value; sudo's may also be written --option=value, which the case above took.
      case $kind in
      env) values=' --unset --argv0 ' ;;
      sudo)
        values=' --user --group --host --prompt --role --type --other-user --close-from '
        ;;
      *) values=' ' ;;
      esac
      if [[ $values == *" $word "* ]]; then
        OPTION_NEXT=$((index + 2))
      fi
      ;;
    esac
    return 0
  fi
  case $kind in
  env) values=CuPaSLU ;;
  sudo) values=ughpCDRrTtU ;;
  *) values=uC ;;
  esac
  cluster=${word#-}
  while [[ -n $cluster ]]; do
    letter=${cluster:0:1}
    cluster=${cluster:1}
    if [[ $kind != env && ($letter == s || $letter == i) ]]; then
      OPTION_INTERACTIVE=1
    fi
    if [[ $values != *"$letter"* ]]; then
      continue
    fi
    if [[ $letter == C && $kind == env ]] || [[ $letter == D && $kind == sudo ]]; then
      OPTION_CLEARS_CWD=1
    fi
    if [[ -n $cluster ]]; then
      dynamic=${WORD_DYNAMIC[index]}
    else
      cluster=${WORDS[index + 1]:-}
      dynamic=${WORD_DYNAMIC[index + 1]:-0}
      OPTION_NEXT=$((index + 2))
    fi
    if [[ $kind == env && $letter == S ]]; then
      OPTION_SPLIT=1
      OPTION_SPLIT_VALUE=$cluster
      OPTION_SPLIT_DYNAMIC=$dynamic
    fi
    return 0
  done
}

# analyze_words: analyses the simple command in SEGMENT_WORDS and SEGMENT_DYNAMIC.
analyze_words() {
  local -a WORDS=("${SEGMENT_WORDS[@]}") WORD_DYNAMIC=("${SEGMENT_DYNAMIC[@]}")
  local count=${#SEGMENT_WORDS[@]} index=0 wrapper interactive_shell
  local ARGUMENTS_FROM_STDIN=0
  # Reserved words, assignments and wrappers in front of the program that runs. An assignment may hold an expansion
  # (`x=$((1<<2))`), which is no program.
  while ((index < count)); do
    if [[ ${WORDS[index]} =~ $ASSIGNMENT_PATTERN ]]; then
      if [[ ${WORDS[index]} =~ $GIT_REPOSITORY_VARIABLE_PATTERN ]]; then
        GIT_REPOSITORY_ENV=1
      fi
      index=$((index + 1))
      continue
    fi
    wrapper=${WORDS[index]##*/}
    case $wrapper in
    '!' | '{' | '}' | if | then | else | elif | fi | do | done | while | until) index=$((index + 1)) ;;
    env)
      index=$((index + 1))
      while ((index < count)); do
        case ${WORDS[index]} in
        --)
          index=$((index + 1))
          break
          ;;
        -*)
          read_wrapper_option env "$index"
          if [[ $OPTION_SPLIT -eq 1 ]]; then
            analyze_env_split "$OPTION_SPLIT_VALUE" "$OPTION_SPLIT_DYNAMIC" "$OPTION_NEXT"
            return 0
          fi
          if [[ $OPTION_CLEARS_CWD -eq 1 ]]; then
            cwd=''
          fi
          index=$OPTION_NEXT
          ;;
        *) break ;;
        esac
      done
      ;;
    sudo | doas)
      index=$((index + 1))
      interactive_shell=0
      while ((index < count)); do
        case ${WORDS[index]} in
        --)
          index=$((index + 1))
          break
          ;;
        -*)
          read_wrapper_option "$wrapper" "$index"
          if [[ $OPTION_INTERACTIVE -eq 1 ]]; then
            interactive_shell=1
          fi
          if [[ $OPTION_CLEARS_CWD -eq 1 ]]; then
            cwd=''
          fi
          index=$OPTION_NEXT
          ;;
        *) break ;;
        esac
      done
      # `sudo -s` and `doas -s` without a command start a shell that reads standard input.
      if ((index >= count)) && [[ $interactive_shell -eq 1 ]]; then
        analyze_stdin_script
      fi
      ;;
    command | builtin | exec | nohup | time | nice | timeout | xargs)
      if [[ $wrapper == xargs ]]; then
        ARGUMENTS_FROM_STDIN=1
      fi
      index=$((index + 1))
      # The options of these wrappers that take a value: nice -n, timeout -s and -k, xargs -n -s -I -L -P -d -E -a.
      while ((index < count)); do
        case ${WORDS[index]} in
        -n | -s | -k | -I | -L | -P | -d | -E | -a) index=$((index + 2)) ;;
        -*[vV]*)
          # `command -v` and `command -V` look a name up and run nothing.
          [[ $wrapper != command ]] || return 0
          index=$((index + 1))
          ;;
        -*) index=$((index + 1)) ;;
        *) break ;;
        esac
      done
      if [[ $wrapper == timeout ]]; then
        index=$((index + 1))
      fi
      ;;
    *) break ;;
    esac
  done
  ((index < count)) || return 0
  analyze_cleanup_wave
  if [[ ${WORD_DYNAMIC[index]} -eq 1 ]]; then
    if [[ $COMMAND_MENTIONS_MERGE -eq 1 ]]; then
      PROBLEMS+=("the command runs a program named by a shell expansion: ${WORDS[index]}")
    fi
    return 0
  fi
  case ${WORDS[index]##*/} in
  cd | pushd) change_directory "$index" ;;
  git) analyze_git "$index" ;;
  bash | sh | zsh | dash | ksh) analyze_shell "$index" ;;
  eval) analyze_eval "$index" ;;
  source | .) analyze_source "$index" ;;
  export | declare | typeset | local | readonly)
    # These change the environment of the commands after them.
    for ((index = index + 1; index < count; index++)); do
      if [[ ${WORDS[index]} =~ $GIT_REPOSITORY_VARIABLE_PATTERN ]]; then
        GIT_REPOSITORY_ENV=1
      fi
    done
    ;;
  *) analyze_embedded "$index" ;;
  esac
}

# change_directory <index of cd>: follows `cd` and `pushd` in `cwd`; a target the hook cannot evaluate makes it unknown.
change_directory() {
  local index=$(($1 + 1)) count=${#WORDS[@]}
  while ((index < count)) && [[ ${WORDS[index]} == -?* ]]; do
    index=$((index + 1))
  done
  if ((index >= count)); then
    cwd=${HOME:-}
  elif [[ ${WORDS[index]} == - || ${WORD_DYNAMIC[index]} -eq 1 ]]; then
    cwd=''
  else
    join_path cwd "$cwd" "${WORDS[index]}"
  fi
}

# analyze_command <text> <cwd> <depth>: analyses every simple command of the text in order.
analyze_command() {
  local cwd=$2 DEPTH=$3 depth deepest=0 index segment script
  local -a order=() nested=()
  if ((DEPTH > MAX_DEPTH)); then
    PROBLEMS+=("the command nests shells more than $MAX_DEPTH levels deep")
    return 0
  fi
  lex_command "$1"
  if [[ $LEX_INCOMPLETE -eq 1 ]] && mentions_merge "$1"; then
    PROBLEMS+=("the command leaves quoting, a substitution or a heredoc open")
  fi
  if [[ $LEX_HEREDOC_SUBSTITUTION -eq 1 ]]; then
    PROBLEMS+=("a heredoc expands a command substitution that mentions a merge")
  fi
  local -a tokens=(${TOKENS[@]+"${TOKENS[@]}"}) token_segment=(${TOKEN_SEGMENT[@]+"${TOKEN_SEGMENT[@]}"})
  local -a token_dynamic=(${TOKEN_DYNAMIC[@]+"${TOKEN_DYNAMIC[@]}"}) token_depth=(${TOKEN_DEPTH[@]+"${TOKEN_DEPTH[@]}"})
  local -a token_redirect=(${TOKEN_REDIRECT[@]+"${TOKEN_REDIRECT[@]}"})
  # What analyze_stdin_script needs: the segments that run a shell on a heredoc, and the here-strings with their segment.
  local -a heredoc_segments=(${HEREDOC_SHELL_SEGMENTS[@]+"${HEREDOC_SHELL_SEGMENTS[@]}"})
  local -a herestring_segments=() herestring_words=() herestring_dynamic=()
  nested=(${NESTED_SCRIPTS[@]+"${NESTED_SCRIPTS[@]}"})
  # The words in order of depth, which keeps every simple command in one run, and then run by run.
  for ((index = 0; index < ${#tokens[@]}; index++)); do
    if ((token_depth[index] > deepest)); then
      deepest=${token_depth[index]}
    fi
  done
  for ((depth = 0; depth <= deepest; depth++)); do
    for ((index = 0; index < ${#tokens[@]}; index++)); do
      if ((token_depth[index] == depth)); then
        order+=("$index")
      fi
    done
  done
  segment=-1
  SEGMENT_WORDS=()
  SEGMENT_DYNAMIC=()
  for index in ${order[@]+"${order[@]}"} end; do
    if [[ $index == end || ${token_segment[index]} -ne $segment ]]; then
      if [[ ${#SEGMENT_WORDS[@]} -gt 0 ]]; then
        analyze_words
      fi
      SEGMENT_WORDS=()
      SEGMENT_DYNAMIC=()
      [[ $index != end ]] || break
      segment=${token_segment[index]}
    fi
    if [[ ${token_redirect[index]} -eq 0 ]]; then
      SEGMENT_WORDS+=("${tokens[index]}")
      SEGMENT_DYNAMIC+=("${token_dynamic[index]}")
    elif [[ ${token_redirect[index]} -eq 2 ]]; then
      herestring_segments+=("${token_segment[index]}")
      herestring_words+=("${tokens[index]}")
      herestring_dynamic+=("${token_dynamic[index]}")
    fi
  done
  for script in ${nested[@]+"${nested[@]}"}; do
    analyze_command "$script" "$cwd" $((DEPTH + 1))
  done
}

# --- main -------------------------------------------------------------------------------------------------------------

# unique_messages <message>...: sets UNIQUE_MESSAGES to the messages without repeats, in first-seen order. One printf, tr
# and awk do it in linear time, which bash pattern substitution would not on a long message (see the early probe); a
# newline inside a message becomes a space so that a message stays one line. If the pipeline fails, the messages stay as
# they are: more text, never less. It needs at least one message.
unique_messages() {
  local lines line
  UNIQUE_MESSAGES=()
  if ! lines=$(printf '%s\0' "$@" | tr '\n\0' ' \n' | awk '!seen[$0]++'); then
    UNIQUE_MESSAGES=("$@")
    return 0
  fi
  while IFS= read -r line; do
    UNIQUE_MESSAGES+=("$line")
  done <<<"$lines"
}

command -v jq >/dev/null 2>&1 || ask_author "jq is not on PATH, so the hook cannot read the command."
read_hook_input "$INPUT"
input_string COMMAND '.tool_input.command' || ask_author "the command in the tool input is not a usable string."
input_string CWD '.cwd' || ask_author "the cwd in the hook input is not a usable string."
[[ $CWD == /* ]] || CWD=''
load_governed_globs "$SCRIPT_DIR"

if mentions_merge "$COMMAND"; then
  COMMAND_MENTIONS_MERGE=1
fi
analyze_command "$COMMAND" "$CWD" 0
if [[ $MERGE_SEEN -eq 1 && $GIT_REPOSITORY_ENV -eq 1 ]]; then
  PROBLEMS+=("the command sets GIT_DIR, GIT_WORK_TREE, GIT_COMMON_DIR or GIT_CEILING_DIRECTORIES, so the hook cannot \
tell which repository git merge runs in")
fi
# The order of the analysis follows the depth of substitutions, not the text, so the rule ignores the order on purpose.
if [[ $MERGE_SEEN -eq 1 && -n $REF_MOVER ]]; then
  PROBLEMS+=("git $REF_MOVER in the same command may move a ref or HEAD that the merge uses before it runs, and the \
hook checks the current state")
fi

if [[ ${#HITS[@]} -eq 0 && ${#PROBLEMS[@]} -eq 0 ]]; then
  no_decision
fi
REASON=''
if [[ ${#HITS[@]} -gt 0 ]]; then
  unique_messages "${HITS[@]}"
  HITS=("${UNIQUE_MESSAGES[@]}")
  LISTED=''
  for ((index = 0; index < ${#HITS[@]} && index < MAX_LISTED; index++)); do
    LISTED+="${LISTED:+, }${HITS[index]}"
  done
  if [[ ${#HITS[@]} -gt $MAX_LISTED ]]; then
    LISTED+=", and $((${#HITS[@]} - MAX_LISTED)) more"
  fi
  REASON="this command merges changes to governed paths: $LISTED. Merges of governed paths need the author's \
confirmation (AIDEV-08)."
fi
if [[ ${#PROBLEMS[@]} -gt 0 ]]; then
  unique_messages "${PROBLEMS[@]}"
  PROBLEMS=("${UNIQUE_MESSAGES[@]}")
  PROBLEM_TEXT=''
  for ((index = 0; index < ${#PROBLEMS[@]} && index < MAX_PROBLEMS; index++)); do
    PROBLEM_TEXT+="${PROBLEM_TEXT:+; }${PROBLEMS[index]}"
  done
  if [[ ${#PROBLEMS[@]} -gt $MAX_PROBLEMS ]]; then
    PROBLEM_TEXT+="; and $((${#PROBLEMS[@]} - MAX_PROBLEMS)) more"
  fi
  REASON+="${REASON:+ }the hook could not check every merge in this command ($PROBLEM_TEXT), so it asks. Confirm \
that the command merges no governed path."
fi
ask_author "$REASON"

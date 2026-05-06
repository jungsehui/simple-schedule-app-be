#!/usr/bin/env bash
# .claude/hooks/check-bash-safety.sh
#
# Block obviously dangerous bash commands.
# Reads JSON tool input from stdin (Claude Code PreToolUse:Bash hook).
#
# Exit codes:
#   0 — allow
#   2 — block with reason

set -uo pipefail

input="$(cat)"
cmd=$(printf '%s' "$input" | sed -n 's/.*"command"[[:space:]]*:[[:space:]]*"\(.*\)"[[:space:]]*}*[[:space:]]*$/\1/p' | head -1)

if [[ -z "$cmd" ]]; then
  exit 0
fi

block() {
  cat >&2 <<EOF
🚫 BLOCKED: Dangerous bash command detected.

  Command: $cmd
  Reason : $1

If you really need this, ask the user explicitly. See .claude/FORBIDDEN.md §5.
EOF
  exit 2
}

# rm -rf on root / home only (allow rm -rf /tmp/foo, build/, etc.)
if echo "$cmd" | grep -qE 'rm[[:space:]]+-rf?[[:space:]]+(/$|/[[:space:]]|~$|~[[:space:]]|~/$|\$HOME$|\$HOME/$)'; then
  block "rm -rf on root / home"
fi

# git push --force on main/master
if echo "$cmd" | grep -qE 'git[[:space:]]+push.*--force.*\b(main|master)\b'; then
  block "git push --force on main/master"
fi
if echo "$cmd" | grep -qE 'git[[:space:]]+push.*-f[[:space:]]+.*\b(main|master)\b'; then
  block "git push -f on main/master"
fi

# git reset --hard (without explicit confirmation)
if echo "$cmd" | grep -qE 'git[[:space:]]+reset[[:space:]]+--hard'; then
  if ! echo "$cmd" | grep -qE 'OMC_CONFIRM_DESTRUCTIVE=1'; then
    block "git reset --hard (set OMC_CONFIRM_DESTRUCTIVE=1 to override)"
  fi
fi

# docker compose down -v (volumes deleted)
if echo "$cmd" | grep -qE 'docker[[:space:]]+compose[[:space:]]+down[[:space:]]+.*-v'; then
  if ! echo "$cmd" | grep -qE 'OMC_CONFIRM_DESTRUCTIVE=1'; then
    block "docker compose down -v deletes DB volumes (set OMC_CONFIRM_DESTRUCTIVE=1 to override)"
  fi
fi

# git commit --no-verify (skips hooks)
if echo "$cmd" | grep -qE 'git[[:space:]]+commit.*--no-verify'; then
  block "--no-verify bypasses commit hooks"
fi

exit 0

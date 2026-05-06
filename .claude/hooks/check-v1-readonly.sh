#!/usr/bin/env bash
# .claude/hooks/check-v1-readonly.sh
#
# Prevents modifications to v1 NestJS code (~/Work/geek-chat/geek-chat-server/)
# and v1 web frontend (~/Work/geek-chat/geek-chat-web/).
# Reads JSON tool input from stdin (Claude Code PreToolUse:Edit/Write hook).

set -uo pipefail

input="$(cat)"
file_path=$(printf '%s' "$input" | sed -n 's/.*"file_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)

if [[ -z "$file_path" ]]; then
  exit 0
fi

# Resolve ~
expanded=$(eval echo "$file_path")

if [[ "$expanded" == "$HOME/Work/geek-chat/geek-chat-server/"* ]] || \
   [[ "$expanded" == "$HOME/Work/geek-chat/geek-chat-web/"* ]]; then
  cat >&2 <<EOF
🚫 BLOCKED: v1 code is read-only.

  File: $expanded

The v1 NestJS server and v1 Expo Web frontend are reference-only.
v1 OAuth code is the porting source for v2 — do not modify v1.

If you really need to change v1 (rare), ask the user explicitly first.
See .claude/FORBIDDEN.md §2.
EOF
  exit 2
fi

exit 0

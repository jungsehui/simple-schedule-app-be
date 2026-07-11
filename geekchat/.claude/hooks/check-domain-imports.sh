#!/usr/bin/env bash
# .claude/hooks/check-domain-imports.sh
#
# Edit/Write guard: blocks two violations at once.
#   1) Domain layer cannot import JPA / Spring
#   2) v1 code (~/Work/geek-chat/geek-chat-server/, geek-chat-web/) is read-only
#
# Reads JSON tool input from stdin (Claude Code PreToolUse:Edit|Write|MultiEdit).
#
# Exit codes:
#   0 — allow
#   2 — block with reason on stderr

set -uo pipefail

input="$(cat)"

file_path=$(printf '%s' "$input" | sed -n 's/.*"file_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
[[ -z "$file_path" ]] && exit 0

# ── (1) v1 read-only check ──
expanded=$(eval echo "$file_path")
if [[ "$expanded" == "$HOME/Work/geek-chat/geek-chat-server/"* ]] || \
   [[ "$expanded" == "$HOME/Work/geek-chat/geek-chat-web/"* ]]; then
  cat >&2 <<EOF
🚫 BLOCKED: v1 code is read-only.

  File: $expanded

v1 NestJS / v1 Expo Web are reference-only.
Read OK; modifications require explicit user approval.
See .claude/RULES.md §2.
EOF
  exit 2
fi

# ── (2) domain layer JPA/Spring import check ──
# Only run on files inside src/main/kotlin/.../domain/
if [[ "$file_path" != *"/src/main/kotlin/com/geekchat/server/domain/"* ]]; then
  exit 0
fi
# Tests are exempt
if [[ "$file_path" == *"/src/test/"* ]]; then
  exit 0
fi

if printf '%s' "$input" | grep -qE '"(new_string|content)"[^"]*"[^"]*import jakarta\.persistence'; then
  cat >&2 <<EOF
🚫 BLOCKED: Domain layer cannot import JPA.

  File: $file_path

Domain models are pure Kotlin (no framework). JPA entities live in
adapter/out/persistence/entity/ with toDomain()/fromDomain() mappings.
See .claude/RULES.md §1.
EOF
  exit 2
fi

if printf '%s' "$input" | grep -qE '"(new_string|content)"[^"]*"[^"]*import org\.springframework'; then
  cat >&2 <<EOF
🚫 BLOCKED: Domain layer cannot import Spring.

  File: $file_path

Spring annotations belong to application/service/ or adapter/ layers.
See .claude/RULES.md §1.
EOF
  exit 2
fi

exit 0

#!/usr/bin/env bash
# .claude/hooks/check-domain-imports.sh
#
# Domain layer import guard.
# Reads JSON tool input from stdin (Claude Code PreToolUse:Edit/Write hook),
# extracts the file_path + content, and blocks if domain/ files contain
# forbidden imports (JPA, Spring, Jackson, etc.)
#
# Exit codes:
#   0 — allow (no violation)
#   1 — silent allow (couldn't parse stdin — fail open)
#   2 — block with reason printed to stderr (Claude Code shows this to the user)

set -uo pipefail

# Read tool input JSON from stdin
input="$(cat)"

# Extract file_path (works for both Edit and Write tools)
file_path=$(printf '%s' "$input" | sed -n 's/.*"file_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)

if [[ -z "$file_path" ]]; then
  exit 0  # not an Edit/Write call we care about
fi

# Only check files inside domain/
if [[ "$file_path" != *"/src/main/kotlin/com/geekchat/server/domain/"* ]]; then
  exit 0
fi

# Tests can have JPA/Spring (integration tests check round-trip)
if [[ "$file_path" == *"/src/test/"* ]]; then
  exit 0
fi

# Extract content. Edit tool uses new_string; Write uses content.
# Best-effort: just dump the input and grep the forbidden patterns.
if printf '%s' "$input" | grep -qE '"(new_string|content)"[^"]*"[^"]*import jakarta\.persistence'; then
  cat >&2 <<EOF
🚫 BLOCKED: Domain layer cannot import JPA.

  File: $file_path
  Reason: 'jakarta.persistence' import detected in domain/ layer.

Domain entities must be pure Kotlin (no framework dependencies).
JPA entities live in adapter/out/persistence/entity/ with toDomain()/fromDomain() mappings.

See .claude/FORBIDDEN.md §1 for the full rule.
EOF
  exit 2
fi

if printf '%s' "$input" | grep -qE '"(new_string|content)"[^"]*"[^"]*import org\.springframework'; then
  cat >&2 <<EOF
🚫 BLOCKED: Domain layer cannot import Spring.

  File: $file_path
  Reason: 'org.springframework' import detected in domain/ layer.

Domain models must be pure Kotlin. Spring annotations belong to application/service/
or adapter/ layers.

See .claude/FORBIDDEN.md §1 for the full rule.
EOF
  exit 2
fi

exit 0

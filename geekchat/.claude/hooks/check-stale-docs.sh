#!/usr/bin/env bash
# .claude/hooks/check-stale-docs.sh
#
# Light-weight stale-doc warning for the Stop hook.
# Compares last-commit code changes vs docs/AGENTS.md changes.
# Always exits 0 (advisory only — never block session end).

set -uo pipefail

# Skip if no recent commits
last_commit=$(git log -1 --format=%H 2>/dev/null) || exit 0
[[ -z "$last_commit" ]] && exit 0

# Files changed in last commit
changed=$(git diff-tree --no-commit-id --name-only -r "$last_commit" 2>/dev/null) || exit 0
[[ -z "$changed" ]] && exit 0

code_changed=$(printf '%s\n' "$changed" | grep -cE '\.(kt|java|kts)$' || true)
docs_changed=$(printf '%s\n' "$changed" | grep -cE '\.md$' || true)
domain_event=$(printf '%s\n' "$changed" | grep -cE 'domain/event/.*\.kt$' || true)
controller=$(printf '%s\n' "$changed" | grep -cE 'adapter/in/web/.*Controller\.kt$' || true)
ws_handler=$(printf '%s\n' "$changed" | grep -cE 'ChatWebSocketHandler|ChatEventListener' || true)

warn=()
if [[ $controller -gt 0 ]]; then
  if ! printf '%s\n' "$changed" | grep -q 'docs/API.md'; then
    warn+=("REST controller changed but docs/API.md not updated")
  fi
fi
if [[ $domain_event -gt 0 || $ws_handler -gt 0 ]]; then
  if ! printf '%s\n' "$changed" | grep -q 'docs/WEBSOCKET.md'; then
    warn+=("WS handler/event changed but docs/WEBSOCKET.md not updated")
  fi
fi
if [[ $code_changed -gt 5 && $docs_changed -eq 0 ]]; then
  warn+=("$code_changed code files changed but no docs updated — consider /refresh-context")
fi

if [[ ${#warn[@]} -gt 0 ]]; then
  cat >&2 <<EOF
⚠️  Stale-doc warning (advisory):
EOF
  for w in "${warn[@]}"; do
    echo "  • $w" >&2
  done
  echo "  → Run: /refresh-context" >&2
fi

exit 0

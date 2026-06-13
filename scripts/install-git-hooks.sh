#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOOK="$ROOT/.githooks/prepare-commit-msg"
TARGET="$ROOT/.git/hooks/prepare-commit-msg"

chmod +x "$HOOK"
mkdir -p "$(dirname "$TARGET")"
cp "$HOOK" "$TARGET"
chmod +x "$TARGET"
echo "Installed prepare-commit-msg hook (strips Cursor co-author lines)."

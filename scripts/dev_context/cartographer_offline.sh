#!/usr/bin/env bash
set -euo pipefail
if [ "$#" -ne 3 ]; then
  echo "USAGE: cartographer_offline.sh CARTOGRAPHER_CHECKOUT ANYTV_CHECKOUT OUTPUT_OUTSIDE_TREE" >&2
  exit 2
fi
cart=$(realpath -m "$1")
source_root=$(realpath -m "$2")
out=$(realpath -m "$3")
case "$out/" in
  "$source_root/"*) echo "REFUSED: output inside AnyTV source tree" >&2; exit 3;;
esac
case "$out/" in
  "$cart/"*) echo "REFUSED: output inside Cartographer checkout" >&2; exit 3;;
esac
if ! command -v bun >/dev/null 2>&1; then
  echo "BLOCKED: bun executable not available in isolated executor" >&2
  exit 4
fi
if [ ! -f "$cart/src/cli/index.ts" ] || [ ! -f "$cart/bun.lock" ]; then
  echo "BLOCKED: Cartographer source/lockfile missing (pin upstream commit)" >&2
  exit 5
fi
if [ ! -d "$source_root/.git" ] && ! git -C "$source_root" rev-parse --git-dir >/dev/null 2>&1; then
  echo "BLOCKED: AnyTV source must be a Git checkout at a verified SHA" >&2
  exit 6
fi
if [ -d "$out" ] && [ -n "$(ls -A "$out")" ]; then
  echo "BLOCKED: output directory must start empty" >&2
  exit 7
fi
mkdir -p "$out"
(cd "$cart" && bun install --frozen-lockfile)
(cd "$cart" && bun run src/cli/index.ts index --root "$source_root" --out "$out")
(cd "$cart" && bun run src/cli/index.ts verify --root "$source_root" --out "$out" --fresh)
echo "INDEX_VERIFIED: $out"

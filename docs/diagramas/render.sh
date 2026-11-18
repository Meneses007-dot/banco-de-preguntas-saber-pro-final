#!/usr/bin/env bash
# Renders every diagram source in docs/diagramas to SVG and PNG through Kroki.
#   *.puml -> C4-PlantUML (/plantuml)
#   *.mmd  -> Mermaid      (/mermaid)
# Usage: bash docs/diagramas/render.sh
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
status=0

render() {
  local source="$1" dialect="$2"
  local base
  base="$(basename "$source")"
  base="${base%.*}"
  for format in svg png; do
    local target="$DIR/$base.$format"
    local code
    code=$(curl -s -o "$target" -w "%{http_code}" \
      -X POST "https://kroki.io/$dialect/$format" \
      -H "Content-Type: text/plain" \
      --data-binary "@$source")
    if [ "$code" != "200" ]; then
      echo "ERROR $base.$format -> HTTP $code" >&2
      status=1
    else
      echo "OK    $base.$format ($(wc -c < "$target") bytes)"
    fi
  done
}

for source in "$DIR"/*.puml; do
  [ -e "$source" ] && render "$source" plantuml
done
for source in "$DIR"/*.mmd; do
  [ -e "$source" ] && render "$source" mermaid
done

exit "$status"

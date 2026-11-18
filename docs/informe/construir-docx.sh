#!/usr/bin/env bash
# Builds the official Word document in two passes so the table of contents gets
# real page numbers (keeping the pagination stable between passes).
#
# Usage: bash docs/informe/construir-docx.sh
# Requirements: python-docx, LibreOffice (soffice), poppler-utils (pdftotext).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
INFORME_DIR="$ROOT/docs/informe"
WORK="${TMPDIR:-/tmp}/saberpro-informe"
PYTHON="${PYTHON:-python3}"
mkdir -p "$WORK"

echo "== 1/4 Documento preliminar (marcadores de pagina vacios) =="
"$PYTHON" "$INFORME_DIR/generar-docx.py"

echo "== 2/4 Render preliminar a PDF =="
soffice --headless --convert-to pdf --outdir "$WORK" \
  "$INFORME_DIR/Formato-Documento.docx" >/dev/null 2>&1

echo "== 3/4 Calculo de paginas por encabezado =="
"$PYTHON" "$INFORME_DIR/calcular-paginas.py" \
  "$WORK/Formato-Documento.pdf" "$WORK/paginas.json"

echo "== 4/4 Documento definitivo con tabla de contenido paginada =="
PAGES_JSON="$WORK/paginas.json" "$PYTHON" "$INFORME_DIR/generar-docx.py"
soffice --headless --convert-to pdf --outdir "$WORK" \
  "$INFORME_DIR/Formato-Documento.docx" >/dev/null 2>&1

pdfinfo "$WORK/Formato-Documento.pdf" | grep Pages || true
echo "OK $INFORME_DIR/Formato-Documento.docx"

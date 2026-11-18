#!/usr/bin/env python3
"""Computes the page number of every heading of the report from a rendered PDF.

Usage:
    python3 docs/informe/calcular-paginas.py <pdf> <pages.json>

The page numbers are relative to the first body page (the one that contains the
introduction), which is the page numbered 1 in the document.
"""

from __future__ import annotations

import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
INFORME = ROOT / "docs" / "informe" / "informe-tecnico.md"


def normalize(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def headings() -> list[str]:
    result: list[str] = []
    for line in INFORME.read_text(encoding="utf-8").splitlines():
        match = re.match(r"^#{2,3}\s+(.*)$", line.strip())
        if not match:
            continue
        text = match.group(1).replace("`", "").replace("**", "").strip()
        if text.startswith("Entrega Final Integradora"):
            continue
        result.append(text)
    return result


def main() -> None:
    pdf = Path(sys.argv[1])
    output = Path(sys.argv[2])
    raw = subprocess.run(
        ["pdftotext", "-layout", str(pdf), "-"],
        check=True, capture_output=True, text=True).stdout
    pages = [normalize(page) for page in raw.split("\f") if page.strip()]

    body_start = next(
        index for index, page in enumerate(pages)
        if "El examen Saber PRO" in page and "1. Introducción" in page
    )

    result: dict[str, str] = {}
    missing: list[str] = []
    for heading in headings():
        target = normalize(heading)
        found = None
        for index in range(body_start, len(pages)):
            if target in pages[index]:
                found = index
                break
        if found is None:
            missing.append(heading)
        else:
            result[heading] = str(found - body_start + 1)

    output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"OK {output} ({len(result)} encabezados)")
    if missing:
        print("WARN sin pagina:", *missing, sep="\n  ")


if __name__ == "__main__":
    main()

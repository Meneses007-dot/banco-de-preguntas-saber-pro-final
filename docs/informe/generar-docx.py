#!/usr/bin/env python3
"""Builds docs/informe/Formato-Documento.docx from informe-tecnico.md.

The official institutional template (Lisw2) is used as the base document: its cover
pages, section setup, fonts and paragraph styles are preserved. The instructional
body of the template is replaced with the real report content.

Usage:
    python3 docs/informe/generar-docx.py
Requirements:
    pip install python-docx
"""

from __future__ import annotations

import json
import os
import re
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK, WD_TAB_ALIGNMENT, WD_TAB_LEADER
from docx.image.image import Image as DocxImage
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

ROOT = Path(__file__).resolve().parents[2]
INFORME = ROOT / "docs" / "informe" / "informe-tecnico.md"
TEMPLATE = ROOT / "docs" / "informe" / "plantilla" / "Formato-Documento.docx"
OUTPUT = ROOT / "docs" / "informe" / "Formato-Documento.docx"

COURSE = "Ingeniería de Software II"
TITLE = "Banco de Preguntas Saber PRO"
SUBTITLE = "Entrega Final Integradora — DDD, Arquitectura Hexagonal y Docker"
STUDENTS = [
    "Nombre del estudiante 1 — Código",
    "Nombre del estudiante 2 — Código",
]

FIGURES: list[str] = []
TABLES: list[str] = []

ACCENTS = {
    "DDD": "Domain-Driven Design",
    "VO": "Value Object",
    "REST": "Representational State Transfer",
    "AMQP": "Advanced Message Queuing Protocol",
    "DLQ": "Dead Letter Queue",
    "DLX": "Dead Letter Exchange",
    "JPA": "Java Persistence API",
    "JAR": "Java ARchive",
    "E2E": "End-to-end",
    "C4": "Modelo C4 de arquitectura",
    "HU": "Historia de Usuario",
    "SOLID": "Single responsibility, Open/closed, Liskov, Interface segregation, Dependency inversion",
    "UML": "Unified Modeling Language",
}


# --------------------------------------------------------------------------- #
# Low level helpers
# --------------------------------------------------------------------------- #
def set_text_keep_format(paragraph, text: str) -> None:
    """Replaces the text of a paragraph without losing its run/paragraph format."""
    if not paragraph.runs:
        paragraph.add_run(text)
        return
    paragraph.runs[0].text = text
    for run in paragraph.runs[1:]:
        run.text = ""


def replace_paragraph_content(paragraph, text: str) -> None:
    """Removes runs, fields and hyperlinks but keeps the paragraph style."""
    for child in list(paragraph._p):
        if child.tag == qn("w:pPr"):
            continue
        paragraph._p.remove(child)
    paragraph.add_run(text)


def shade(paragraph, fill: str = "F2F2F2") -> None:
    p_pr = paragraph._p.get_or_add_pPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), fill)
    p_pr.append(shd)


def add_page_break(document) -> None:
    paragraph = document.add_paragraph()
    paragraph.add_run().add_break(WD_BREAK.PAGE)


def add_toc(document) -> None:
    paragraph = document.add_paragraph()
    run = paragraph.add_run()
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    begin.set(qn("w:dirty"), "true")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = r' TOC \o "1-2" \h \z \u '
    separate = OxmlElement("w:fldChar")
    separate.set(qn("w:fldCharType"), "separate")
    placeholder = OxmlElement("w:t")
    placeholder.text = "Actualice el campo (F9) para generar la tabla de contenido."
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    run._r.append(begin)
    run._r.append(instr)
    run._r.append(separate)
    run._r.append(placeholder)
    run._r.append(end)


INLINE_BOLD = re.compile(r"\*\*(.+?)\*\*")
INLINE_CODE = re.compile(r"`([^`]+)`")
MD_LINK = re.compile(r"\[([^\]]+)\]\(([^)]+)\)")
ITALIC = re.compile(r"(?<![\w*])\*([^*\n]+)\*(?![\w*])")
FIGURE = re.compile(r"<!--\s*figura:(.+?)\s*\|\s*(.+?)\s*-->")
TABLE_CAPTION = re.compile(r"<!--\s*tabla:(.+?)\s*-->")


def add_inline(paragraph, text: str) -> None:
    """Adds runs honouring **bold**, `code`, *italic* and [text](url)."""
    text = MD_LINK.sub(lambda m: f"{m.group(1)} ({m.group(2)})", text)

    tokens: list[tuple[str, str]] = []
    position = 0
    pattern = re.compile(r"(\*\*.+?\*\*|`[^`]+`|(?<![\w*])\*[^*\n]+\*(?![\w*]))")
    for match in pattern.finditer(text):
        if match.start() > position:
            tokens.append(("plain", text[position:match.start()]))
        chunk = match.group(0)
        if chunk.startswith("**"):
            tokens.append(("bold", chunk[2:-2]))
        elif chunk.startswith("`"):
            tokens.append(("code", chunk[1:-1]))
        else:
            tokens.append(("italic", chunk[1:-1]))
        position = match.end()
    if position < len(text):
        tokens.append(("plain", text[position:]))

    for kind, value in tokens:
        run = paragraph.add_run(value)
        if kind == "bold":
            run.bold = True
        elif kind == "code":
            run.font.name = "Consolas"
            run.font.size = Pt(10)
        elif kind == "italic":
            run.italic = True


def configure_styles(document) -> None:
    normal = document.styles["Normal"]
    normal.font.name = "Arial"
    normal.font.size = Pt(11)
    r_pr = normal.element.get_or_add_rPr()
    r_pr.rFonts.set(qn("w:eastAsia"), "Arial")
    fmt = normal.paragraph_format
    fmt.line_spacing = 1.3
    fmt.space_after = Pt(6)
    fmt.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY

    for name, size in (("Heading 1", 14), ("Heading 2", 12), ("Heading 3", 11)):
        style = document.styles[name]
        style.font.name = "Arial"
        style.font.size = Pt(size)
        style.font.bold = True
        style.font.color.rgb = RGBColor(0x1F, 0x3B, 0x57)
        style.paragraph_format.space_before = Pt(12)
        style.paragraph_format.space_after = Pt(6)
        style.paragraph_format.keep_with_next = True


# --------------------------------------------------------------------------- #
# Markdown parsing
# --------------------------------------------------------------------------- #
def read_blocks(markdown: str) -> list[tuple[str, object]]:
    """Turns the markdown subset used by the report into typed blocks."""
    blocks: list[tuple[str, object]] = []
    lines = markdown.splitlines()
    index = 0
    paragraph_buffer: list[str] = []

    def flush_paragraph() -> None:
        nonlocal paragraph_buffer
        if paragraph_buffer:
            blocks.append(("paragraph", " ".join(paragraph_buffer).strip()))
            paragraph_buffer = []

    while index < len(lines):
        line = lines[index]
        stripped = line.strip()

        if stripped.startswith("```"):
            flush_paragraph()
            index += 1
            code_lines: list[str] = []
            while index < len(lines) and not lines[index].strip().startswith("```"):
                code_lines.append(lines[index])
                index += 1
            blocks.append(("code", "\n".join(code_lines)))
            index += 1
            continue

        figure = FIGURE.search(stripped)
        if figure:
            flush_paragraph()
            blocks.append(("figure", (figure.group(1), figure.group(2))))
            index += 1
            continue

        table_caption = TABLE_CAPTION.search(stripped)
        if table_caption:
            flush_paragraph()
            blocks.append(("table_caption", table_caption.group(1).strip()))
            index += 1
            continue

        if stripped.startswith("<!--") or stripped == "---" or not stripped:
            flush_paragraph()
            index += 1
            continue

        if stripped.startswith("#"):
            flush_paragraph()
            level = len(stripped) - len(stripped.lstrip("#"))
            text = stripped[level:].strip()
            blocks.append(("heading", (level, text)))
            index += 1
            continue

        if stripped.startswith("|"):
            flush_paragraph()
            table_lines: list[str] = []
            while index < len(lines) and lines[index].strip().startswith("|"):
                table_lines.append(lines[index].strip())
                index += 1
            rows = []
            for row_index, row in enumerate(table_lines):
                cells = [cell.strip() for cell in row.strip("|").split("|")]
                if row_index == 1 and all(re.fullmatch(r":?-{2,}:?", c) for c in cells):
                    continue
                rows.append(cells)
            blocks.append(("table", rows))
            continue

        if stripped.startswith("- "):
            flush_paragraph()
            blocks.append(("bullet", stripped[2:].strip()))
            index += 1
            continue

        numbered = re.match(r"^\d+\.\s+(.*)$", stripped)
        if numbered:
            flush_paragraph()
            blocks.append(("number", numbered.group(1)))
            index += 1
            continue

        if stripped.startswith("> "):
            flush_paragraph()
            blocks.append(("quote", stripped[2:].strip()))
            index += 1
            continue

        # A list item wrapped over several source lines keeps its indentation and
        # must stay inside the item (otherwise the bold markers get truncated).
        if (
            not paragraph_buffer
            and blocks
            and blocks[-1][0] in ("bullet", "number", "quote")
            and line[:1] in (" ", "\t")
        ):
            kind, value = blocks[-1]
            blocks[-1] = (kind, f"{value} {stripped}")
            index += 1
            continue

        paragraph_buffer.append(stripped)
        index += 1

    flush_paragraph()
    return blocks


# --------------------------------------------------------------------------- #
# Document building
# --------------------------------------------------------------------------- #
def remove_template_toc(document) -> None:
    """The template ships a cached TOC (w:sdt). It is removed so only our field remains."""
    body = document.element.body
    for child in list(body):
        if child.tag == qn("w:sdt") and "TOC" in "".join(child.itertext()):
            body.remove(child)


def fix_headers(document, text: str) -> None:
    """Replaces the template's header placeholder on the front matter and body pages."""
    for section in document.sections[1:]:
        paragraph = section.header.paragraphs[0]
        paragraph.text = f"{text}\t"
        for run in paragraph.runs:
            run.font.size = Pt(8)
            run.font.name = "Arial"


def add_student_line(paragraph, text: str) -> None:
    """Adds a second student name on a new line, reusing the paragraph format."""
    first = paragraph.runs[0]
    first.add_break()
    run = paragraph.add_run(text)
    run.bold = first.bold
    run.font.size = first.font.size
    run.font.name = first.font.name


def fill_cover(document) -> None:
    paragraphs = document.paragraphs
    set_text_keep_format(paragraphs[0], TITLE)
    set_text_keep_format(paragraphs[2], STUDENTS[0])
    add_student_line(paragraphs[2], STUDENTS[1])
    set_text_keep_format(paragraphs[3], SUBTITLE)
    set_text_keep_format(paragraphs[14], "Universidad del Cauca")
    set_text_keep_format(paragraphs[15], "Facultad de Ingeniería Electrónica y Telecomunicaciones")
    set_text_keep_format(paragraphs[16], "Departamento de Sistemas")
    set_text_keep_format(paragraphs[17], COURSE)
    set_text_keep_format(paragraphs[18], "Popayán, 2026")
    set_text_keep_format(paragraphs[20], STUDENTS[0])
    add_student_line(paragraphs[20], STUDENTS[1])
    set_text_keep_format(paragraphs[25], TITLE)
    set_text_keep_format(paragraphs[30], f"{SUBTITLE} presentada en el curso de {COURSE}")
    set_text_keep_format(paragraphs[33], "Estudiantes del:")
    set_text_keep_format(paragraphs[34], "Programa de Ingeniería de Sistemas")
    set_text_keep_format(paragraphs[42], "Popayán")
    set_text_keep_format(paragraphs[43], "2026")

    # The second student line grows each cover by one line; the spare empty
    # paragraph that follows it is removed so the cover still fits one page.
    for paragraph in (paragraphs[8], paragraphs[24]):
        if not paragraph.text.strip():
            paragraph._p.getparent().remove(paragraph._p)


def fill_index_lists(document, figure_captions: list[str], table_captions: list[str]) -> None:
    paragraphs = document.paragraphs
    figure_heading = next(i for i, p in enumerate(paragraphs) if p.text.strip() == "Lista de Figuras")
    table_heading = next(i for i, p in enumerate(paragraphs) if p.text.strip() == "Lista de Tablas")
    acronym_heading = next(i for i, p in enumerate(paragraphs) if p.text.strip() == "Lista de Siglas")

    def rewrite_entries(heading_index: int, entries: list[str]) -> None:
        # The first entry follows the heading plus one blank paragraph.
        cursor = heading_index + 2
        template_style = paragraphs[cursor].style.name
        original: list = []
        probe = cursor
        while probe < len(paragraphs) and paragraphs[probe].style.name == template_style:
            original.append(paragraphs[probe])
            probe += 1
        anchor = paragraphs[probe]
        for offset, entry in enumerate(entries):
            if offset < len(original):
                replace_paragraph_content(original[offset], entry)
            else:
                anchor.insert_paragraph_before(entry, style=template_style)

    rewrite_entries(figure_heading, figure_captions)
    rewrite_entries(table_heading, table_captions)

    acronym_start = acronym_heading + 2
    acronyms = [f"{key}: {value}" for key, value in ACCENTS.items()]
    anchor = paragraphs[acronym_start]
    for acronym in acronyms:
        anchor.insert_paragraph_before(acronym, style="Normal")


def add_page_break_before(paragraph) -> None:
    new_paragraph = paragraph.insert_paragraph_before("")
    new_paragraph.add_run().add_break(WD_BREAK.PAGE)


def add_toc_to_paragraph(paragraph) -> None:
    run = paragraph.add_run()
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    begin.set(qn("w:dirty"), "true")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = r' TOC \o "1-2" \h \z \u '
    separate = OxmlElement("w:fldChar")
    separate.set(qn("w:fldCharType"), "separate")
    placeholder = OxmlElement("w:t")
    placeholder.text = "Actualice el campo (F9) para generar la tabla de contenido."
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    for element in (begin, instr, separate, placeholder, end):
        run._r.append(element)


def delete_body_from(document, marker: str) -> None:
    """Deletes every body element after the marker paragraph (tables and sdt included)."""
    body = document.element.body
    children = list(body)
    start = None
    for index, child in enumerate(children):
        if child.tag == qn("w:p") and marker in "".join(child.itertext()):
            start = index
            break
    if start is None:
        raise ValueError(f"marker not found: {marker}")
    for child in children[start:]:
        if child.tag == qn("w:sectPr"):
            continue
        body.remove(child)


def trim_front_matter(document) -> None:
    """Removes the extra empty paragraphs that would render a blank page."""
    body = document.element.body
    children = list(body)
    figure_index = next(
        index for index, child in enumerate(children)
        if child.tag == qn("w:p") and "Lista de Figuras" in "".join(child.itertext())
    )
    section_index = 0
    for index, child in enumerate(children[:figure_index]):
        if child.tag != qn("w:p"):
            continue
        p_pr = child.find(qn("w:pPr"))
        if p_pr is not None and p_pr.find(qn("w:sectPr")) is not None:
            section_index = index
            break
    for child in children[section_index + 1:figure_index]:
        if child.tag == qn("w:p") and not "".join(child.itertext()).strip():
            body.remove(child)


def add_static_toc(document, entries: list[tuple[int, str]], pages: dict[str, str]) -> list:
    elements = []
    for level, text in entries:
        style = "toc 1" if level == 1 else "toc 2"
        paragraph = document.add_paragraph(style=style)
        paragraph.paragraph_format.space_before = Pt(0)
        paragraph.paragraph_format.space_after = Pt(1)
        paragraph.paragraph_format.line_spacing = 1.15
        paragraph.paragraph_format.tab_stops.add_tab_stop(
            Cm(15.5), WD_TAB_ALIGNMENT.RIGHT, WD_TAB_LEADER.DOTS)
        run = paragraph.add_run(f"{text}\t{pages.get(text, '')}")
        run.font.size = Pt(10)
        elements.append(paragraph._p)
    elements.append(document.add_paragraph()._p)
    return elements


def move_to_front_matter(document, elements: list) -> None:
    """Moves the table of contents into the roman-numbered front matter section.

    The TOC is built at the end of the document (the body section) because that
    is where python-docx appends content. Inserting its paragraphs right before
    the section break that closes the front matter keeps the roman pagination of
    the first pages and lets the body restart at page 1, so the printed page
    numbers match the ones computed for the TOC.
    """
    body = document.element.body
    breaks = []
    for child in body:
        if child.tag != qn("w:p"):
            continue
        p_pr = child.find(qn("w:pPr"))
        if p_pr is not None and p_pr.find(qn("w:sectPr")) is not None:
            breaks.append(child)
    if len(breaks) < 2:
        raise ValueError("front matter section break not found")
    front_matter_break = breaks[1]
    for element in elements:
        body.remove(element)
        front_matter_break.addprevious(element)


def add_table(document, rows: list[list[str]]) -> None:
    if not rows:
        return
    columns = max(len(row) for row in rows)
    table = document.add_table(rows=0, cols=columns)
    table.style = "Table Grid"
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    layout = OxmlElement("w:tblLayout")
    layout.set(qn("w:type"), "fixed")
    table._tbl.tblPr.append(layout)

    # Column widths follow the text they hold: each column gets at least the
    # width of its longest unbreakable word, and the remaining space is shared
    # proportionally to the total content. When the minimums do not fit, the
    # table font is reduced one step instead of breaking identifiers in half.
    char_width = 0.175  # cm per character, refined per font size below
    padding = 0.55

    def longest_word(value: str) -> int:
        parts = [part for part in re.split(r"[\s/\\]", value) if part]
        return max((len(part) for part in parts), default=1)

    usable = 15.4
    font_sizes = ((9, 0.19), (8, 0.17), (7.5, 0.16))
    for font_size, char_width in font_sizes:
        naturals = [
            max(len(row[column]) if column < len(row) else 0 for row in rows) * char_width + padding
            for column in range(columns)
        ]
        minimums = [
            max(longest_word(row[column] if column < len(row) else "")
                for row in rows) * char_width + padding
            for column in range(columns)
        ]
        if sum(minimums) <= usable:
            break

    if sum(naturals) <= usable:
        scale = usable / sum(naturals)
        widths_cm = [natural * scale for natural in naturals]
    else:
        remaining = usable - sum(minimums)
        if remaining >= 0:
            extra_demand = [max(natural - minimum, 0.01) for natural, minimum in zip(naturals, minimums)]
            widths_cm = [
                minimum + remaining * extra / sum(extra_demand)
                for minimum, extra in zip(minimums, extra_demand)
            ]
        else:
            scale = usable / sum(minimums)
            widths_cm = [minimum * scale for minimum in minimums]
    widths = [int(Cm(width)) for width in widths_cm]
    for column, width in zip(table.columns, widths):
        column.width = width

    for row_index, row in enumerate(rows):
        table_row = table.add_row()
        properties = table_row._tr.get_or_add_trPr()
        properties.append(OxmlElement("w:cantSplit"))
        if row_index == 0:
            properties.append(OxmlElement("w:tblHeader"))
        cells = table_row.cells
        for column in range(columns):
            value = row[column] if column < len(row) else ""
            cell = cells[column]
            cell.width = widths[column]
            cell.text = ""
            paragraph = cell.paragraphs[0]
            paragraph.alignment = WD_ALIGN_PARAGRAPH.LEFT
            paragraph.paragraph_format.space_after = Pt(2)
            paragraph.paragraph_format.line_spacing = 1.0
            if row_index < len(rows) - 1:
                paragraph.paragraph_format.keep_with_next = True
            add_inline(paragraph, value)
            for run in paragraph.runs:
                run.font.size = Pt(font_size)
                if row_index == 0:
                    run.bold = True

    document.add_paragraph()


def add_code(document, code: str) -> None:
    lines = code.splitlines() or [""]
    longest = max(len(line) for line in lines)
    available_pt = 420.0  # usable text width of the report body
    size = min(8.5, max(7.0, available_pt / (0.62 * max(longest, 1))))
    for line in lines:
        paragraph = document.add_paragraph()
        paragraph.paragraph_format.space_after = Pt(0)
        paragraph.paragraph_format.line_spacing = 1.0
        paragraph.paragraph_format.left_indent = Cm(0.4)
        run = paragraph.add_run(line if line else " ")
        run.font.name = "Consolas"
        run.font.size = Pt(size)
        shade(paragraph)
    spacer = document.add_paragraph()
    spacer.paragraph_format.space_before = Pt(0)
    spacer.paragraph_format.space_after = Pt(0)
    spacer.paragraph_format.line_spacing = 1.0
    spacer.add_run(" ").font.size = Pt(3)


def add_figure(document, relative_path: str, caption: str) -> None:
    image_path = ROOT / relative_path
    if not image_path.exists():
        raise FileNotFoundError(image_path)
    image = DocxImage.from_file(str(image_path))
    max_width, max_height = Cm(14.5), Cm(16.5)
    scale = min(1.0, max_width / image.width, max_height / image.height)
    paragraph = document.add_paragraph()
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    paragraph.paragraph_format.space_before = Pt(6)
    paragraph.paragraph_format.keep_with_next = True
    paragraph.add_run().add_picture(
        str(image_path), width=int(image.width * scale), height=int(image.height * scale))

    caption_paragraph = document.add_paragraph()
    caption_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    caption_paragraph.paragraph_format.space_after = Pt(12)
    caption_paragraph.style = document.styles["Normal"]
    run = caption_paragraph.add_run(caption)
    run.italic = True
    run.font.size = Pt(9)


def build(markdown: str) -> None:
    blocks = read_blocks(markdown)
    figure_captions = [value[1] for kind, value in blocks if kind == "figure"]
    table_captions = [value for kind, value in blocks if kind == "table_caption"]

    document = Document(str(TEMPLATE))
    configure_styles(document)
    remove_template_toc(document)
    fix_headers(document, TITLE + " — Entrega Final")
    fill_cover(document)
    fill_index_lists(document, figure_captions, table_captions)
    trim_front_matter(document)
    delete_body_from(document, "Título del documento")

    toc_entries = [
        (level - 1, text.replace("`", "").replace("**", ""))
        for kind, value in blocks
        if kind == "heading"
        for level, text in [value]
        if level in (2, 3) and not text.startswith("Entrega Final Integradora")
    ]
    page_map: dict[str, str] = {}
    pages_file = os.environ.get("PAGES_JSON")
    if pages_file and Path(pages_file).exists():
        page_map = json.loads(Path(pages_file).read_text(encoding="utf-8"))

    page_break_paragraph = document.add_paragraph()
    page_break_paragraph.add_run().add_break(WD_BREAK.PAGE)
    document.add_heading("Contenido", level=1)
    toc_elements = [page_break_paragraph._p, document.paragraphs[-1]._p]
    toc_elements += add_static_toc(document, toc_entries, page_map)
    move_to_front_matter(document, toc_elements)

    number_counter = 0
    for kind, value in blocks:
        if kind == "heading":
            number_counter = 0
            level, text = value
            if level == 1:
                continue  # document title lives on the cover
            text = text.replace("`", "").replace("**", "")
            if level == 2 and text.startswith("Entrega Final Integradora"):
                subtitle = document.add_paragraph()
                subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
                run = subtitle.add_run(text)
                run.bold = True
                run.font.size = Pt(12)
                continue
            document.add_heading(text, level=min(level - 1, 3))
        elif kind == "paragraph":
            paragraph = document.add_paragraph()
            # Justified text looks broken when a line holds a very long token
            # (URLs, environment variables, file names), so those go left.
            if re.match(r"^\[\d+\]", value) or re.search(r"\S{28,}", value):
                paragraph.alignment = WD_ALIGN_PARAGRAPH.LEFT
            add_inline(paragraph, value)
        elif kind == "bullet":
            paragraph = document.add_paragraph()
            paragraph.paragraph_format.left_indent = Cm(0.8)
            paragraph.paragraph_format.first_line_indent = Cm(-0.4)
            paragraph.paragraph_format.space_after = Pt(2)
            paragraph.add_run("•  ")
            add_inline(paragraph, value)
            number_counter = 0
        elif kind == "number":
            number_counter += 1
            paragraph = document.add_paragraph()
            paragraph.paragraph_format.left_indent = Cm(0.8)
            paragraph.paragraph_format.first_line_indent = Cm(-0.4)
            paragraph.paragraph_format.space_after = Pt(2)
            paragraph.add_run(f"{number_counter}.  ")
            add_inline(paragraph, value)
        elif kind == "quote":
            paragraph = document.add_paragraph()
            paragraph.paragraph_format.left_indent = Cm(0.8)
            run = paragraph.add_run(value)
            run.italic = True
        elif kind == "code":
            add_code(document, value)
        elif kind == "table_caption":
            caption_paragraph = document.add_paragraph()
            caption_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
            caption_paragraph.paragraph_format.space_before = Pt(6)
            caption_paragraph.paragraph_format.space_after = Pt(3)
            caption_paragraph.paragraph_format.keep_with_next = True
            run = caption_paragraph.add_run(value)
            run.italic = True
            run.font.size = Pt(9)
        elif kind == "table":
            add_table(document, value)
        elif kind == "figure":
            path, caption = value
            add_figure(document, path, caption)

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    document.save(str(OUTPUT))
    print(f"OK {OUTPUT.relative_to(ROOT)} ({OUTPUT.stat().st_size} bytes)")
    print(f"   figuras: {len(figure_captions)} | tablas: {len(table_captions)}")


if __name__ == "__main__":
    build(INFORME.read_text(encoding="utf-8"))

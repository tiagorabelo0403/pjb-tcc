from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

LINE_MARKER = re.compile(
    r"//\s*(todo|fixme|hack|xxx|workaround|temporary|temporario)\b",
    re.IGNORECASE,
)

LINE_REDUNDANTE = re.compile(
    r"//\s*(método|metodo|classe|função|funcao|ajuste|corrige|correção|correcao)\w*\b",
    re.IGNORECASE,
)

COMMENT_BLOCK = re.compile(r"/\*(?:(?!\*/)[\s\S])*?\*/")
BLOCK_MARKER_UPPER = re.compile(r"\b(TODO|FIXME|HACK|XXX|WORKAROUND)\b")
BLOCK_MARKER_LABELLED = re.compile(
    r"\b(todo|fixme|hack|xxx|workaround|temporario|temporary)\s*:", re.IGNORECASE
)

ALLOWED_DIR_PARTS = {"target", "build", "out", ".git"}


def ignored(path: Path) -> bool:
    return bool(set(path.relative_to(ROOT).parts) & ALLOWED_DIR_PARTS)


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def block_violation(text: str) -> bool:
    for block in COMMENT_BLOCK.finditer(text):
        corpo = block.group(0)
        if BLOCK_MARKER_UPPER.search(corpo) or BLOCK_MARKER_LABELLED.search(corpo):
            return True
    return False


def main() -> int:
    findings = []
    for path in ROOT.rglob("*.java"):
        if ignored(path):
            continue
        text = path.read_text(encoding="utf-8", errors="ignore")
        if LINE_MARKER.search(text) or LINE_REDUNDANTE.search(text) or block_violation(text):
            findings.append(relative(path))
    result = {"forbiddenCommentFindings": findings}
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if findings else 0


if __name__ == "__main__":
    raise SystemExit(main())

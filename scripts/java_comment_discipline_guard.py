from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FORBIDDEN = re.compile(r"//\s*(todo|fixme|hack|temporary|temporario|workaround|método|metodo|classe|função|funcao|ajuste|corrige|correção|correcao)", re.IGNORECASE)
BLOCK_FORBIDDEN = re.compile(r"/\*.*?(todo|fixme|hack|temporary|temporario|workaround).*?\*/", re.IGNORECASE | re.DOTALL)
ALLOWED_DIR_PARTS = {"target", "build", "out", ".git"}


def ignored(path: Path) -> bool:
    return bool(set(path.relative_to(ROOT).parts) & ALLOWED_DIR_PARTS)


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def main() -> int:
    findings = []
    for path in ROOT.rglob("*.java"):
        if ignored(path):
            continue
        text = path.read_text(encoding="utf-8", errors="ignore")
        if FORBIDDEN.search(text) or BLOCK_FORBIDDEN.search(text):
            findings.append(relative(path))
    result = {"forbiddenCommentFindings": findings}
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if findings else 0


if __name__ == "__main__":
    raise SystemExit(main())

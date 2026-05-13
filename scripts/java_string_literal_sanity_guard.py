#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
SCAN_ROOTS = (
    ROOT / "pjb-api" / "src" / "main" / "java",
    ROOT / "pjb-api" / "src" / "test" / "java",
    ROOT / "pjb-core" / "src" / "main" / "java",
    ROOT / "pjb-core" / "src" / "test" / "java",
)


def scan_file(path: Path) -> list[str]:
    issues: list[str] = []
    in_block_comment = False
    in_text_block = False
    try:
        lines = path.read_text(encoding="utf-8", errors="ignore").splitlines()
    except OSError as exc:
        return [f"{path.relative_to(ROOT)}: unable to read file: {exc}"]
    for line_number, line in enumerate(lines, start=1):
        i = 0
        in_string = False
        in_char = False
        escaped = False
        while i < len(line):
            current = line[i]
            nxt = line[i + 1] if i + 1 < len(line) else ""
            if in_text_block:
                if line.startswith('"""', i):
                    in_text_block = False
                    i += 3
                    continue
                i += 1
                continue
            if in_block_comment:
                if current == "*" and nxt == "/":
                    in_block_comment = False
                    i += 2
                    continue
                i += 1
                continue
            if in_char:
                if escaped:
                    escaped = False
                elif current == "\\":
                    escaped = True
                elif current == "'":
                    in_char = False
                i += 1
                continue
            if in_string:
                if escaped:
                    escaped = False
                elif current == "\\":
                    escaped = True
                elif current == '"':
                    in_string = False
                i += 1
                continue
            if current == "/" and nxt == "/":
                break
            if current == "/" and nxt == "*":
                in_block_comment = True
                i += 2
                continue
            if line.startswith('"""', i):
                in_text_block = True
                i += 3
                continue
            if current == "'":
                in_char = True
                escaped = False
                i += 1
                continue
            if current == '"':
                in_string = True
                escaped = False
                i += 1
                continue
            i += 1
        if in_string:
            issues.append(f"{path.relative_to(ROOT)}:{line_number}: ordinary string literal crosses line boundary")
    return issues


def main() -> int:
    issues: list[str] = []
    for root in SCAN_ROOTS:
        if not root.exists():
            continue
        for path in root.rglob("*.java"):
            issues.extend(scan_file(path))
    if issues:
        print("java_string_literal_sanity_guard failed")
        for issue in issues:
            print(f" - {issue}")
        return 1
    print("java_string_literal_sanity_guard: OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BANNED_DIRS = {"target", "build", "out", "outcheck", "__pycache__", ".pytest_cache", ".mypy_cache", ".gradle"}
BANNED_SUFFIXES = {".class", ".pyc", ".pyo", ".log", ".tmp", ".bak", ".orig", ".rej"}
ALLOWED_ROOT_MARKDOWN = {"README.md"}
IGNORED_ROOTS = {".git"}


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def ignored(path: Path) -> bool:
    parts = set(path.relative_to(ROOT).parts)
    return bool(parts & IGNORED_ROOTS)


def main() -> int:
    banned_directories = []
    banned_files = []
    loose_root_markdown = []
    for path in ROOT.rglob("*"):
        if ignored(path):
            continue
        if path.is_dir() and path.name in BANNED_DIRS:
            banned_directories.append(relative(path))
            continue
        if path.is_file() and path.suffix.lower() in BANNED_SUFFIXES:
            banned_files.append(relative(path))
    for path in ROOT.glob("*.md"):
        if path.name not in ALLOWED_ROOT_MARKDOWN:
            loose_root_markdown.append(relative(path))
    result = {
        "bannedDirectories": banned_directories,
        "bannedFiles": banned_files,
        "looseRootMarkdown": loose_root_markdown,
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if any(result.values()) else 0


if __name__ == "__main__":
    raise SystemExit(main())

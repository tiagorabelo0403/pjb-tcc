from __future__ import annotations

import json

from versionados import arquivos_versionados

BANNED_DIRS = {"target", "build", "out", "outcheck", "__pycache__", ".pytest_cache", ".mypy_cache", ".gradle"}
BANNED_SUFFIXES = {".class", ".pyc", ".pyo", ".log", ".tmp", ".bak", ".orig", ".rej"}
ALLOWED_ROOT_MARKDOWN = {"README.md", "README.en.md"}


def main() -> int:
    versionados = arquivos_versionados()

    banned_files = sorted(
        caminho
        for caminho in versionados
        if any(caminho.lower().endswith(sufixo) for sufixo in BANNED_SUFFIXES)
    )
    banned_directories = sorted(
        {
            "/".join(caminho.split("/")[: indice + 1])
            for caminho in versionados
            for indice, segmento in enumerate(caminho.split("/")[:-1])
            if segmento in BANNED_DIRS
        }
    )
    loose_root_markdown = sorted(
        caminho
        for caminho in versionados
        if "/" not in caminho and caminho.endswith(".md") and caminho not in ALLOWED_ROOT_MARKDOWN
    )

    result = {
        "arquivosVersionadosExaminados": len(versionados),
        "bannedDirectories": banned_directories,
        "bannedFiles": banned_files,
        "looseRootMarkdown": loose_root_markdown,
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if banned_directories or banned_files or loose_root_markdown else 0


if __name__ == "__main__":
    raise SystemExit(main())

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
README = ROOT / "README.md"
PATH_PATTERN = re.compile(r"(?:^|[\s`])((?:docs|scripts|config|infra|tooling|pjb-api|pjb-core)/(?:[A-Za-z0-9_.@=+:-]+/)*[A-Za-z0-9_.@=+:-]+)(?:[\s`.,)]|$)")
BANNED_WORDS = ["round", "rodada"]


def existing(path: str) -> bool:
    return (ROOT / path).exists()


def main() -> int:
    if not README.exists():
        print(json.dumps({"missingReadme": True}, ensure_ascii=False, indent=2))
        return 1
    text = README.read_text(encoding="utf-8")
    lower = text.lower()
    banned = [word for word in BANNED_WORDS if word in lower]
    paths = sorted(set(match.group(1).rstrip(".") for match in PATH_PATTERN.finditer(text)))
    missing_paths = [path for path in paths if not existing(path)]
    result = {
        "bannedWords": banned,
        "referencedPaths": paths,
        "missingPaths": missing_paths,
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if banned or missing_paths else 0


if __name__ == "__main__":
    raise SystemExit(main())

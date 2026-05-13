#!/usr/bin/env python3
from __future__ import annotations

from collections import defaultdict
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
MIGRATION_DIR = ROOT / "pjb-api" / "src" / "main" / "resources" / "db" / "migration"
PATTERN = re.compile(r"^(V\d+)__.+\.sql$")


def main() -> int:
    versions: dict[str, list[str]] = defaultdict(list)
    if not MIGRATION_DIR.exists():
        print("FLYWAY MIGRATION VERSION GUARD: FAIL")
        print(f" - missing migration directory: {MIGRATION_DIR.relative_to(ROOT)}")
        return 1

    for path in sorted(MIGRATION_DIR.glob("V*__*.sql")):
        match = PATTERN.match(path.name)
        if not match:
            continue
        versions[match.group(1)].append(path.name)

    duplicates = {version: names for version, names in versions.items() if len(names) > 1}
    if duplicates:
        print("FLYWAY MIGRATION VERSION GUARD: FAIL")
        for version, names in sorted(duplicates.items()):
            print(f" - duplicate version {version}: {', '.join(names)}")
        return 1

    print("FLYWAY MIGRATION VERSION GUARD: OK")
    print(f"validated migrations: {MIGRATION_DIR.relative_to(ROOT)}")
    print(f"total versions: {len(versions)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "pjb-api" / "src" / "main" / "resources"
MANIFEST = RES / "catalog" / "legal_ai_catalog_manifest_2026.json"


def main() -> int:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    errors: list[str] = []
    validated: list[str] = []
    for item in manifest.get("requiredResources", []):
        rel = item.get("path", "")
        top_level = item.get("topLevel", "")
        required_fields = item.get("requiredFields", [])
        path = RES / rel
        if not path.exists():
            errors.append(f"missing resource: {rel}")
            continue
        try:
            payload = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            errors.append(f"invalid json: {rel}: {exc}")
            continue
        if top_level == "ARRAY" and not isinstance(payload, list):
            errors.append(f"expected ARRAY: {rel}")
            continue
        if top_level == "OBJECT" and not isinstance(payload, dict):
            errors.append(f"expected OBJECT: {rel}")
            continue
        if isinstance(payload, list):
            for index, entry in enumerate(payload):
                if not isinstance(entry, dict):
                    errors.append(f"non-object entry: {rel}[{index}]")
                    continue
                for field in required_fields:
                    value = entry.get(field)
                    if value in (None, ""):
                        errors.append(f"missing field: {rel}[{index}].{field}")
        else:
            for field in required_fields:
                if field not in payload:
                    errors.append(f"missing field: {rel}.{field}")
        validated.append(rel)
    if errors:
        print("LEGAL KNOWLEDGE CATALOG GUARD: FAIL")
        for item in errors:
            print(f" - {item}")
        return 1
    print("LEGAL KNOWLEDGE CATALOG GUARD: OK")
    print(f"validated resources: {len(validated)}")
    for item in validated:
        print(f" - {item}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

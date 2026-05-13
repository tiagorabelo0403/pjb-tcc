from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "pjb-api" / "src" / "main" / "java" / "com" / "tcc" / "pjb" / "backend" / "controller" / "processual" / "comunicacao" / "institutional"
LEGACY = "NationalCommunicationInstitutionalHttpRoutes"
CANONICAL_REQUEST_MAPPING = "@RequestMapping(InstitutionalApiRoutes.CANONICAL_BASE)"
CANONICAL_IMPORT = "import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;"


def main() -> int:
    legacy_imports = []
    weak_mappings = []
    missing_imports = []
    for path in sorted(BASE.rglob("*.java")):
        content = path.read_text(encoding="utf-8")
        rel = path.relative_to(ROOT).as_posix()
        if LEGACY in content:
            legacy_imports.append(rel)
        if "@RequestMapping" in content and CANONICAL_REQUEST_MAPPING not in content:
            weak_mappings.append(rel)
        if "@RequestMapping" in content and CANONICAL_IMPORT not in content:
            missing_imports.append(rel)
    result = {
        "legacyRouteReferences": legacy_imports,
        "weakMappings": weak_mappings,
        "missingCanonicalImports": missing_imports,
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if any(result.values()) else 0


if __name__ == "__main__":
    raise SystemExit(main())

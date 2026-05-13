from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOC = ROOT / "docs" / "product" / "NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_MATRIX.md"
INDEX = ROOT / "docs" / "product" / "NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_INDEX.json"
CATALOG = ROOT / "pjb-api" / "src" / "main" / "java" / "com" / "tcc" / "pjb" / "backend" / "core" / "plataforma" / "substituicao" / "domain" / "PjbSubstituicaoNacionalCapabilityCatalog.java"
EXPECTED_SYSTEMS = ["PJe", "PJe 2.x", "e-SAJ", "eproc", "Creta", "Projudi"]
EXPECTED_PATHS = [
    "core.plataforma.substituicao",
    "integration.judicial",
    "integration.mni",
    "core.procedural",
    "core.processo",
    "core.kernel.recursal",
    "core.comunicacao.institucional",
    "service.secretariat",
    "core.security",
    "core.lgpd",
    "core.observability",
]


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8") if path.exists() else ""


def main() -> int:
    missing = []
    doc = read(DOC)
    catalog = read(CATALOG)
    if not DOC.exists():
        missing.append(str(DOC.relative_to(ROOT)))
    if not INDEX.exists():
        missing.append(str(INDEX.relative_to(ROOT)))
    if not CATALOG.exists():
        missing.append(str(CATALOG.relative_to(ROOT)))
    for item in EXPECTED_SYSTEMS:
        if item not in doc:
            missing.append(f"document-system:{item}")
    for item in EXPECTED_PATHS:
        if item not in doc:
            missing.append(f"document-path:{item}")
    for item in ["PJE", "PJE_2X", "ESAJ", "EPROC", "CRETA", "PROJUDI"]:
        if item not in catalog:
            missing.append(f"catalog-system:{item}")
    index_status = "missing"
    if INDEX.exists():
        data = json.loads(INDEX.read_text(encoding="utf-8"))
        index_status = "ok" if data.get("legacySystems") == EXPECTED_SYSTEMS else "invalid"
        if index_status != "ok":
            missing.append("index-legacySystems")
    result = {
        "document": str(DOC.relative_to(ROOT)),
        "index": str(INDEX.relative_to(ROOT)),
        "catalog": str(CATALOG.relative_to(ROOT)),
        "indexStatus": index_status,
        "missing": missing,
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if missing else 0


if __name__ == "__main__":
    raise SystemExit(main())

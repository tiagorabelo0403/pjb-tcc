from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = [
    "docs/product/TRIBUNAL_PRODUCTION_READINESS.md",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/substituicao/readiness/PjbTribunalReadinessStatus.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/substituicao/readiness/PjbTribunalReadinessCapability.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/substituicao/readiness/PjbTribunalReadinessSnapshot.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/substituicao/readiness/PjbTribunalProductionReadinessService.java",
]
TOKENS = [
    "READY_FOR_PRODUCTION",
    "BLOCKED_BY_CONNECTOR",
    "BLOCKED_BY_MIGRATION",
    "PjbSubstituicaoNacionalCapabilityCatalog",
]


def main() -> int:
    missing = [path for path in REQUIRED if not (ROOT / path).exists()]
    service = ROOT / REQUIRED[-1]
    token_findings = []
    if service.exists():
        text = service.read_text(encoding="utf-8")
        token_findings = [token for token in TOKENS if token not in text]
    result = {"missing": missing, "missingTokens": token_findings}
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if missing or token_findings else 0


if __name__ == "__main__":
    raise SystemExit(main())

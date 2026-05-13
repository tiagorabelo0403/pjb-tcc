from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = [
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/security/accesskey/PjbProcessAccessKeyPolicy.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/security/accesskey/PjbProcessAccessKeyGrant.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/observability/unavailability/PjbDeadlineImpactAssessmentService.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/observability/unavailability/PjbSystemUnavailabilityEvent.java",
    "docs/product/PUBLIC_PORTAL_CAPABILITY_SPEC.md",
    "docs/product/MIGRATION_AND_INTEROPERABILITY_STRATEGY.md",
]
TOKENS = {
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/security/accesskey/PjbProcessAccessKeyPolicy.java": ["revoked", "expired", "sealedCase", "MAXIMUM_VALIDITY"],
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/observability/unavailability/PjbDeadlineImpactAssessmentService.java": ["MINIMUM_EXTERNAL_OUTAGE", "nextBusinessDay", "America/Fortaleza"],
}


def main() -> int:
    missing = [path for path in REQUIRED if not (ROOT / path).exists()]
    missing_tokens = []
    for path, tokens in TOKENS.items():
        candidate = ROOT / path
        text = candidate.read_text(encoding="utf-8") if candidate.exists() else ""
        for token in tokens:
            if token not in text:
                missing_tokens.append(f"{path}:{token}")
    result = {"missing": missing, "missingTokens": missing_tokens}
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if missing or missing_tokens else 0


if __name__ == "__main__":
    raise SystemExit(main())

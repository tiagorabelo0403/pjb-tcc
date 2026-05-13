from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REQUIRED_PATHS = [
    "docs/product/JUDICIAL_INNOVATION_PART_TWO.md",
    "pjb-api/src/main/java/com/tcc/pjb/backend/service/audiencia/digital/PjbDigitalHearingOrchestrator.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice/PjbDigitalJusticeUnitPlanner.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/service/offline/continuity/PjbOfflineContinuityPolicy.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/procedural/atermacao/PjbAtermacaoGuidedIntakeService.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/service/jurisprudencia/awareness/PjbPrecedentAwarenessEngine.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/kernel/advisory/PjbSettlementGovernanceLens.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/frontend/accessibility/PjbAccessToJusticeScoreService.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/observability/procedural/PjbProceduralObservabilityService.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/service/api/PjbJudicialServiceMarketplaceGovernance.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/substituicao/parity/PjbLegacyParityTestKit.java",
]
FORBIDDEN_ROOTS = [
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/processo2",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/secretaria2",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/marketplace2",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/legado2",
]
FORBIDDEN_TOKENS = ["TODO", "FIXME", "HACK", "workaround"]


def main() -> int:
    missing = [path for path in REQUIRED_PATHS if not (ROOT / path).exists()]
    duplicate_roots = [path for path in FORBIDDEN_ROOTS if (ROOT / path).exists()]
    token_hits = []
    for path in REQUIRED_PATHS:
        full = ROOT / path
        if full.suffix == ".java" and full.exists():
            content = full.read_text(encoding="utf-8")
            for token in FORBIDDEN_TOKENS:
                if token in content:
                    token_hits.append(f"{path}:{token}")
    result = {
        "missingInnovationPartTwoArtifacts": missing,
        "duplicateInnovationPartTwoRoots": duplicate_roots,
        "forbiddenTokenHits": token_hits,
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if missing or duplicate_roots or token_hits else 0


if __name__ == "__main__":
    raise SystemExit(main())

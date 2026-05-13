from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REQUIRED_PATHS = [
    "docs/product/JUDICIAL_INNOVATION_BLUEPRINT.md",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/governance/changeimpact/PjbChangeImpactSimulator.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/health/PjbProcessHealthRadar.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/peticionamento/blackbox/PjbProtocolBlackBoxService.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/frontend/publicaccess/PjbPlainLanguageTimelineService.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/frontend/publicaccess/PjbPublicDocumentTrustCenter.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/observability/unavailability/PjbUnavailabilityCertificateAssembler.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/distribuicao/explainable/PjbExplainableDistributionEngine.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/service/secretariat/autopilot/PjbSecretariatAutopilotService.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/migracao/intelligence/PjbLegacyMigrationIntelligenceService.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/kernel/twin/PjbCourtDigitalTwinSimulationService.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/frontend/virtualcounter/PjbSmartVirtualCounterService.java",
]
FORBIDDEN_DUPLICATE_ROOTS = [
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/innovation",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/novo",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/substituicao2",
]


def main() -> int:
    missing = [path for path in REQUIRED_PATHS if not (ROOT / path).exists()]
    duplicate_roots = [path for path in FORBIDDEN_DUPLICATE_ROOTS if (ROOT / path).exists()]
    result = {"missingInnovationArtifacts": missing, "duplicateInnovationRoots": duplicate_roots}
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if missing or duplicate_roots else 0


if __name__ == "__main__":
    raise SystemExit(main())

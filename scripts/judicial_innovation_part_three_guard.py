from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REQUIRED_PATHS = [
    "docs/product/JUDICIAL_INNOVATION_PART_THREE.md",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice/PjbJuizadoAdjuntoNucleoStage.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice/PjbJuizadoAdjuntoNucleoStageCatalog.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice/PjbJuizadoAdjuntoNucleoOptionService.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice/PjbJuizadoAdjuntoPublicGuidance.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice/PjbJuizadoAdjuntoPjeProtocolBridge.java",
]
REQUIRED_SNIPPETS = {
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice/PjbJuizadoAdjuntoNucleoOptionService.java": [
        "MENCAO_NA_PETICAO_INICIAL_NAO_SUBSTITUI_OPCAO_NO_CADASTRO",
        "SEM_OPCAO_NO_CADASTRO_PROCESSO_SEGUE_NA_VARA_COMUM",
        "NAO_HA_REDISTRIBUICAO_AUTOMATICA_PARA_O_NUCLEO",
        "OPCAO_IMUTAVEL_APOS_DISTRIBUICAO",
    ],
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice/PjbJuizadoAdjuntoNucleoStageCatalog.java": [
        "1ª e 2ª Vara Cível da Comarca de Morada Nova",
        "LocalDate.of(2026, 5, 18)",
        "LocalDate.of(2026, 5, 29)",
    ],
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice/PjbDigitalJusticeUnitPlanner.java": [
        "assessJuizadoAdjunto",
        "PJE_NAO_HABILITADO_COMO_SISTEMA_DE_TRAMITACAO",
        "OPCAO_NO_CADASTRO_DE_PROTOCOLO_NAO_HABILITADA",
    ],
}
FORBIDDEN_ROOTS = [
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/juizado40",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/nucleo40",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/pje2",
]


def main() -> int:
    missing = [path for path in REQUIRED_PATHS if not (ROOT / path).exists()]
    missing_snippets = []
    for path, snippets in REQUIRED_SNIPPETS.items():
        content = (ROOT / path).read_text(encoding="utf-8") if (ROOT / path).exists() else ""
        for snippet in snippets:
            if snippet not in content:
                missing_snippets.append(f"{path}:{snippet}")
    duplicate_roots = [path for path in FORBIDDEN_ROOTS if (ROOT / path).exists()]
    result = {
        "missingInnovationPartThreeArtifacts": missing,
        "missingInnovationPartThreeSnippets": missing_snippets,
        "duplicateInnovationPartThreeRoots": duplicate_roots,
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if missing or missing_snippets or duplicate_roots else 0


if __name__ == "__main__":
    raise SystemExit(main())

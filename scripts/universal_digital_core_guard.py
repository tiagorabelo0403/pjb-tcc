from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = [
    "docs/product/UNIVERSAL_DIGITAL_CORE_ZERO_ERROR_TRIAGE.md",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice/PjbDigitalCoreRitoKind.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice/PjbUniversalDigitalCoreRouter.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice/PjbRitoContext.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice/PjbContextualPanelPolicy.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/preflight/PjbZeroErrorTriageService.java",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/distribuicao/PjbDistribuicaoStrategyResolver.java",
]
FORBIDDEN = [
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/juizado",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/juizados",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/python",
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/microservice/ocr",
]
SNIPPETS = {
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/preflight/PjbZeroErrorTriageService.java": [
        "INCOMPATIBILIDADE_SUMARISSIMA_PERICIA_COMPLEXA",
        "COMPETENCIA_TERRITORIAL_POTENCIALMENTE_INCOMPATIVEL",
        "CUSTAS_INICIAIS_BLOQUEADAS_NO_JUIZADO",
    ],
    "pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice/PjbContextualPanelPolicy.java": [
        "CITACAO_POR_EDITAL_PADRAO",
        "GUIA_CUSTAS_PRIMEIRO_GRAU",
        "VALIDAR_COMPATIBILIDADE_SUMARISSIMA_ANTES_DA_DISTRIBUICAO",
    ],
}


def main() -> int:
    missing = [path for path in REQUIRED if not (ROOT / path).exists()]
    forbidden = [path for path in FORBIDDEN if (ROOT / path).exists()]
    missing_snippets = []
    for path, snippets in SNIPPETS.items():
        content = (ROOT / path).read_text(encoding="utf-8") if (ROOT / path).exists() else ""
        for snippet in snippets:
            if snippet not in content:
                missing_snippets.append(f"{path}:{snippet}")
    result = {"missing": missing, "forbidden": forbidden, "missingSnippets": missing_snippets}
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if missing or forbidden or missing_snippets else 0


if __name__ == "__main__":
    raise SystemExit(main())

from __future__ import annotations

from pathlib import Path


def _detect_root() -> Path:
    current = Path(__file__).resolve()
    for candidate in current.parents:
        if (candidate / "pom.xml").exists() and (candidate / "pjb-api").exists():
            return candidate
    raise RuntimeError("Não foi possível localizar a raiz do repositório do PJB.")


ROOT = _detect_root()
APP_MODULE = ROOT / "pjb-api"
APP_ROOT = APP_MODULE if (APP_MODULE / "src" / "main" / "java").exists() else ROOT
SRC_MAIN = APP_ROOT / "src" / "main" / "java"
SRC_TEST = APP_ROOT / "src" / "test" / "java"
RES_MAIN = APP_ROOT / "src" / "main" / "resources"
RES_TEST = APP_ROOT / "src" / "test" / "resources"
CORE_MAIN = ROOT / "pjb-core" / "src" / "main" / "java"


def resolve_candidate(candidate: str) -> Path:
    normalized = candidate.lstrip("./")
    direct = ROOT / normalized
    if direct.exists():
        return direct
    if normalized.startswith("src/"):
        nested = APP_MODULE / normalized
        if nested.exists():
            return nested
    return direct

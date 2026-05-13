from __future__ import annotations

import sys
from pathlib import Path

_TOOLING_SCRIPTS = Path(__file__).resolve().parents[1] / "tooling" / "python" / "scripts"
if str(_TOOLING_SCRIPTS) not in sys.path:
    sys.path.insert(0, str(_TOOLING_SCRIPTS))

from project_roots import APP_MODULE, APP_ROOT, CORE_MAIN, RES_MAIN, RES_TEST, ROOT, SRC_MAIN, SRC_TEST, resolve_candidate

__all__ = [
    "APP_MODULE",
    "APP_ROOT",
    "CORE_MAIN",
    "RES_MAIN",
    "RES_TEST",
    "ROOT",
    "SRC_MAIN",
    "SRC_TEST",
    "resolve_candidate",
]

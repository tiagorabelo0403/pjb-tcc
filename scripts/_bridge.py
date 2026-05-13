from __future__ import annotations

import runpy
import sys
from pathlib import Path


def run(script_name: str) -> None:
    root = Path(__file__).resolve().parents[1]
    tooling_root = root / "tooling" / "python"
    tooling_scripts = tooling_root / "scripts"
    target = tooling_scripts / script_name
    if not target.exists():
        raise SystemExit(f"Python tooling script not found: {target}")
    sys.path.insert(0, str(tooling_scripts))
    sys.path.insert(0, str(tooling_root))
    runpy.run_path(str(target), run_name="__main__")

from __future__ import annotations

import sys
import warnings
from pathlib import Path

from project_roots import ROOT

SCRIPT_ROOTS = (ROOT / 'tooling' / 'python' / 'scripts', ROOT / 'scripts')


def findings() -> list[tuple[str, str]]:
    found: list[tuple[str, str]] = []
    for root in SCRIPT_ROOTS:
        if not root.exists():
            continue
        for path in sorted(root.rglob('*.py')):
            source = path.read_text(encoding='utf-8', errors='ignore')
            with warnings.catch_warnings(record=True) as captured:
                warnings.simplefilter('always')
                try:
                    compile(source, str(path), 'exec')
                except SyntaxError as error:
                    found.append((path.relative_to(ROOT).as_posix(), f'SyntaxError: {error}'))
                    continue
                for item in captured:
                    if issubclass(item.category, SyntaxWarning):
                        found.append((path.relative_to(ROOT).as_posix(), str(item.message)))
    return found


def main() -> int:
    found = findings()
    scanned = sum(len(list(root.rglob('*.py'))) for root in SCRIPT_ROOTS if root.exists())
    if not found:
        print(f'PYTHON SYNTAX WARNING GUARD: OK ({scanned} scripts varridos)')
        return 0
    print('PYTHON SYNTAX WARNING GUARD: FAIL', file=sys.stderr)
    for path, message in found:
        print(f' - {path}: {message}', file=sys.stderr)
    return 1


if __name__ == '__main__':
    raise SystemExit(main())

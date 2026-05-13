from __future__ import annotations

import json
import re
from pathlib import Path

from project_roots import ROOT

SRC = ROOT / 'pjb-api' / 'src' / 'main' / 'java' / 'com' / 'tcc' / 'pjb' / 'backend'
REPORT_JSON = ROOT / 'docs' / 'reports' / 'constructor_injection_guard.json'
REPORT_MD = ROOT / 'docs' / 'reports' / 'constructor_injection_guard.md'

TYPE_THRESHOLD = 12
HOTSPOT_THRESHOLD = 16
LINES_THRESHOLD = 900


def count_lines(path: Path) -> int:
    return path.read_text(encoding='utf-8', errors='ignore').count('\n') + 1


def class_name(path: Path) -> str:
    return path.stem


def constructor_signature(text: str, type_name: str) -> str | None:
    pattern = re.compile(r'public\s+' + re.escape(type_name) + r'\s*\((.*?)\)\s*\{', re.DOTALL)
    match = pattern.search(text)
    if not match:
        return None
    return match.group(1).strip()


def normalize_parameters(signature: str | None) -> list[str]:
    if not signature:
        return []
    pieces = []
    depth = 0
    current = []
    for char in signature:
        if char == '<':
            depth += 1
        elif char == '>':
            depth = max(0, depth - 1)
        elif char == ',' and depth == 0:
            value = ''.join(current).strip()
            if value:
                pieces.append(value)
            current = []
            continue
        current.append(char)
    value = ''.join(current).strip()
    if value:
        pieces.append(value)
    return pieces


def parameter_type(parameter: str) -> str:
    tokens = [token for token in parameter.replace('\n', ' ').split(' ') if token]
    if len(tokens) < 2:
        return parameter.strip()
    return ' '.join(tokens[:-1]).strip()


def classify(path: Path) -> str:
    name = path.name
    if name.endswith('Controller.java'):
        return 'controller'
    if name.endswith('FacadeService.java'):
        return 'facade_service'
    if name.endswith('Service.java'):
        return 'service'
    if name.endswith('Engine.java'):
        return 'engine'
    return 'other'


def main() -> None:
    entries = []
    for path in sorted(SRC.rglob('*.java')):
        text = path.read_text(encoding='utf-8', errors='ignore')
        signature = constructor_signature(text, class_name(path))
        if signature is None:
            continue
        parameters = normalize_parameters(signature)
        types = [parameter_type(parameter) for parameter in parameters]
        entries.append({
            'file': str(path.relative_to(ROOT)),
            'className': class_name(path),
            'kind': classify(path),
            'lineCount': count_lines(path),
            'constructorParameterCount': len(parameters),
            'constructorParameterTypes': types,
        })

    by_constructor = sorted(entries, key=lambda item: (-item['constructorParameterCount'], -item['lineCount'], item['file']))
    hotspots = [
        entry for entry in by_constructor
        if entry['constructorParameterCount'] >= HOTSPOT_THRESHOLD
        or (entry['constructorParameterCount'] >= TYPE_THRESHOLD and entry['lineCount'] >= LINES_THRESHOLD)
    ]

    report = {
        'base': str(SRC),
        'thresholds': {
            'constructorParameterCount': TYPE_THRESHOLD,
            'hotspotParameterCount': HOTSPOT_THRESHOLD,
            'lineCount': LINES_THRESHOLD,
        },
        'totalConstructors': len(entries),
        'hotspotCount': len(hotspots),
        'topConstructors': by_constructor[:40],
        'hotspots': hotspots[:40],
        'recommendedActions': [
            'Extrair collaborators especializados quando facades/services ultrapassarem 12 dependências com mais de 900 linhas.',
            'Concentrar gateways documentais, drafting e projections em assemblers dedicados.',
            'Travar regressão com testes de arquitetura focados nos hotspots mais críticos.'
        ]
    }
    REPORT_JSON.parent.mkdir(parents=True, exist_ok=True)
    REPORT_JSON.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')

    lines = [
        '# Constructor Injection Guard',
        '',
        f"- Base analisada: `{SRC}`",
        f"- Construtores analisados: **{len(entries)}**",
        f"- Hotspots detectados: **{len(hotspots)}**",
        '',
        '## Hotspots',
        ''
    ]
    if hotspots:
        for entry in hotspots[:20]:
            lines.append(
                f"- `{entry['file']}` -> {entry['constructorParameterCount']} dependências, {entry['lineCount']} linhas ({entry['kind']})"
            )
    else:
        lines.append('- Nenhum hotspot acima dos limiares configurados.')
    lines.extend(['', '## Ações recomendadas', ''])
    for action in report['recommendedActions']:
        lines.append(f'- {action}')
    REPORT_MD.write_text('\n'.join(lines) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()

from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path

from project_roots import ROOT

BASE = ROOT / 'pjb-api' / 'src' / 'main' / 'java' / 'com' / 'tcc' / 'pjb' / 'backend'
REPORT_JSON = ROOT / 'docs' / 'reports' / 'config_taxonomy_guard.json'
REPORT_MD = ROOT / 'docs' / 'reports' / 'config_taxonomy_guard.md'
CANONICAL_ROOT = 'configs'
LEGACY_ROOTS = ('config', 'configuracao')


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--fail-on-legacy', action='store_true')
    args = parser.parse_args()

    java_files = sorted(BASE.rglob('*.java'))
    root_counts: Counter[str] = Counter()
    legacy_files: list[dict[str, str]] = []
    canonical_files: list[dict[str, str]] = []

    for path in java_files:
        rel = path.relative_to(BASE)
        root = rel.parts[0]
        root_counts[root] += 1
        entry = {
            'path': str(path.relative_to(ROOT)),
            'package': '.'.join(rel.with_suffix('').parts[:-1]),
            'root': root,
        }
        if root == CANONICAL_ROOT:
            canonical_files.append(entry)
        if root in LEGACY_ROOTS:
            legacy_files.append(entry)

    report = {
        'base': str(BASE.relative_to(ROOT)),
        'canonicalRoot': CANONICAL_ROOT,
        'legacyRoots': list(LEGACY_ROOTS),
        'rootCounts': dict(sorted(root_counts.items())),
        'totals': {
            'javaFiles': len(java_files),
            'canonicalFiles': len(canonical_files),
            'legacyFiles': len(legacy_files),
        },
        'legacyFiles': legacy_files[:50],
        'recommendedActions': [
            'Usar `configs` como raiz canônica para configurações Spring e infraestrutura do runtime.',
            'Evitar novos arquivos Java em `config` e `configuracao` para impedir deriva semântica.',
            'Reservar `config` de nível de repositório apenas para toolchain e análise estática fora da árvore Java.',
        ],
    }

    REPORT_JSON.parent.mkdir(parents=True, exist_ok=True)
    REPORT_JSON.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')

    lines = [
        '# Config Taxonomy Guard',
        '',
        f'- Base analisada: `{report["base"]}`',
        f'- Raiz canônica: `{CANONICAL_ROOT}`',
        f'- Arquivos Java analisados: **{report["totals"]["javaFiles"]}**',
        f'- Arquivos na raiz canônica: **{report["totals"]["canonicalFiles"]}**',
        f'- Arquivos em raízes legadas: **{report["totals"]["legacyFiles"]}**',
        '',
        '## Contagem por raiz',
        '',
    ]
    for root, count in sorted(root_counts.items()):
        if root in (CANONICAL_ROOT, *LEGACY_ROOTS):
            marker = ' (canônica)' if root == CANONICAL_ROOT else ' (legada)'
            lines.append(f'- `{root}` -> {count} arquivos{marker}')
    if report['totals']['legacyFiles']:
        lines.extend(['', '## Arquivos ainda em raízes legadas', ''])
        for item in legacy_files[:20]:
            lines.append(f"- `{item['path']}`")
    else:
        lines.extend(['', '## Arquivos ainda em raízes legadas', '', '- Nenhum arquivo Java remanescente nas raízes legadas.'])
    lines.extend(['', '## Diretriz', ''])
    for action in report['recommendedActions']:
        lines.append(f'- {action}')
    REPORT_MD.write_text('\n'.join(lines) + '\n', encoding='utf-8')

    if args.fail_on_legacy and report['totals']['legacyFiles']:
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

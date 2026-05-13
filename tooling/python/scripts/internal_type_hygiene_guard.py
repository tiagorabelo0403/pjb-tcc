from pathlib import Path
import json
import re

BASE = Path('pjb-api/src/main/java/com/tcc/pjb/backend')
REPORT_JSON = Path('docs/reports/internal_type_hygiene_guard.json')
REPORT_MD = Path('docs/reports/internal_type_hygiene_guard.md')
THRESHOLD = 900
pattern = re.compile(r'\b(private|protected|public)?\s*(static\s+)?(final\s+)?(sealed\s+)?(record|enum|class|interface)\s+([A-Za-z0-9_]+)')

results = []
for path in sorted(BASE.rglob('*.java')):
    lines = path.read_text(encoding='utf-8').splitlines()
    if len(lines) < THRESHOLD:
        continue
    nested = []
    for idx, line in enumerate(lines, 1):
        if idx <= 40:
            continue
        stripped = line.strip()
        m = pattern.search(stripped)
        if not m:
            continue
        kind = m.group(5)
        name = m.group(6)
        if kind == 'class' and name == path.stem and not line.startswith((' ', '\t')):
            continue
        nested.append({'line': idx, 'kind': kind, 'name': name, 'declaration': line.strip()})
    if nested:
        results.append({'file': str(path), 'lines': len(lines), 'nestedTypes': nested})

summary = {
    'filesScanned': sum(1 for _ in BASE.rglob('*.java')),
    'thresholdLines': THRESHOLD,
    'filesFlagged': len(results),
    'totalNestedTypes': sum(len(item['nestedTypes']) for item in results),
}
REPORT_JSON.write_text(json.dumps({'summary': summary, 'flaggedFiles': results}, indent=2), encoding='utf-8')
md = [
    '# Internal Type Hygiene Guard',
    '',
    f"- Base analisada: `{BASE}`",
    f"- Arquivos Java: **{summary['filesScanned']}**",
    f"- Threshold de tamanho: **{THRESHOLD} linhas**",
    f"- Arquivos sinalizados: **{summary['filesFlagged']}**",
    f"- Tipos internos detectados: **{summary['totalNestedTypes']}**",
    '',
    '## Arquivos sinalizados',
    ''
]
if not results:
    md.append('- Nenhum arquivo acima do threshold com tipos internos detectados.')
else:
    for item in results[:40]:
        md.append(f"- `{item['file']}` -> {item['lines']} linhas / {len(item['nestedTypes'])} tipos internos")
        for nested in item['nestedTypes'][:8]:
            md.append(f"  - L{nested['line']} `{nested['kind']} {nested['name']}`")
REPORT_MD.write_text('\n'.join(md) + '\n', encoding='utf-8')

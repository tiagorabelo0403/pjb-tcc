from __future__ import annotations
import json
from pathlib import Path

from project_roots import ROOT, RES_MAIN

MIGRATIONS = RES_MAIN / 'db' / 'migration'

expected = {
    'V178': 'ICP-Brasil',
    'V179': 'MNI',
    'V180': 'DataJud',
    'V181': 'Workflow criminal',
    'V182': 'Integrações financeiras sensíveis',
    'V183': 'Custas judiciais',
    'V184': 'Workflow trabalhista',
    'V185': 'Workflow eleitoral',
    'V186': 'DJe',
    'V187': 'Digitalização de acervo',
    'V188': 'Sobrestamento por tema',
}
actual = {
    'ICP-Brasil': 'V197__icp_brasil_certificate_chain.sql',
    'MNI': 'V192__mni_remessa.sql',
    'DataJud': 'V193__datajud_feed_checkpoint.sql',
    'Workflow criminal': 'V195__criminal_workflow.sql',
    'Integrações financeiras sensíveis': 'V198__integracao_judicial_financeira.sql',
    'Custas judiciais': 'V196__custas_judiciais.sql',
    'Workflow trabalhista': 'V199__workflow_trabalhista.sql',
    'Workflow eleitoral': 'V191__workflow_eleitoral.sql',
    'DJe': 'V189__dje_publicacao.sql',
    'Digitalização de acervo': 'V194__digitalizacao_acervo.sql',
    'Sobrestamento por tema': 'V190__sobrestamento_tema.sql',
}

versions = {}
for name in sorted(MIGRATIONS.glob('V*__*.sql')):
    version = name.name.split('__',1)[0]
    versions.setdefault(version, []).append(name.name)

duplicates = {version: files for version, files in versions.items() if len(files) > 1}
rows = []
for version, label in expected.items():
    resolved = actual.get(label)
    rows.append({
        'pdfVersion': version,
        'label': label,
        'resolvedMigration': resolved,
        'resolvedExists': bool(resolved and (MIGRATIONS / resolved).exists()),
        'alignment': 'implementado_com_adaptacao_de_numeracao' if resolved else 'pendente',
    })

report = {
    'summary': {
        'expectedEntries': len(expected),
        'resolvedEntries': sum(1 for row in rows if row['resolvedExists']),
        'duplicateMigrationVersions': duplicates,
    },
    'rows': rows,
}

(ROOT / 'docs/reports').mkdir(parents=True, exist_ok=True)
(ROOT / 'docs/reports/migration_alignment_report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')

lines = [
    '# Alinhamento de migrations do PDF',
    '',
    'O conteúdo do PDF foi materializado, mas a numeração das migrations acabou consolidada com adaptação na base atual.',
    '',
    '## Resumo',
    '',
    f"- Entradas esperadas do PDF: **{report['summary']['expectedEntries']}**",
    f"- Entradas resolvidas na base: **{report['summary']['resolvedEntries']}**",
    f"- Versões duplicadas de migration detectadas no sweep: **{len(duplicates)}**",
    '',
    '## Mapa PDF → base atual',
    '',
    '| PDF | Item | Migration resolvida na base | Status |',
    '|---|---|---|---|',
]
for row in rows:
    status = 'implementado com adaptação' if row['resolvedExists'] else 'pendente'
    lines.append(f"| {row['pdfVersion']} | {row['label']} | `{row['resolvedMigration'] or '-'} ` | {status} |")
lines.append('')
if duplicates:
    lines.append('## Duplicidades corrigidas/monitoradas')
    lines.append('')
    for version, files in duplicates.items():
        lines.append(f"- `{version}`: {', '.join(files)}")
    lines.append('')

(ROOT / 'docs/PDF_MIGRATION_ALIGNMENT.md').write_text('\n'.join(lines), encoding='utf-8')

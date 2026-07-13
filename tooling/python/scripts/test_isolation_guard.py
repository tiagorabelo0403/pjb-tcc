from __future__ import annotations

import json
import re
from pathlib import Path

from project_roots import ROOT, SRC_TEST

REPORT_JSON = ROOT / 'docs' / 'reports' / 'test_isolation_guard.json'
REPORT_MD = ROOT / 'docs' / 'reports' / 'test_isolation_guard.md'

DIRECT_BASE_PATTERN = re.compile(r'extends\s+PjbIntegrationTestBase\b')
CLEANUP_ANNOTATION_PATTERN = re.compile(r'@(Transactional|BeforeEach|AfterEach|Sql)\b')


def class_name(path: Path) -> str:
    return path.stem


def main() -> None:
    findings = []
    total_it_classes = 0

    for path in sorted(SRC_TEST.rglob('*IT.java')):
        total_it_classes += 1
        text = path.read_text(encoding='utf-8', errors='ignore')
        if not DIRECT_BASE_PATTERN.search(text):
            continue
        if CLEANUP_ANNOTATION_PATTERN.search(text):
            continue
        findings.append({
            'file': str(path.relative_to(ROOT)),
            'className': class_name(path),
            'reason': 'extends PjbIntegrationTestBase diretamente, sem @Transactional/@BeforeEach/@AfterEach/@Sql na classe.',
        })

    findings.sort(key=lambda item: item['file'])

    report = {
        'base': str(SRC_TEST),
        'totalItClasses': total_it_classes,
        'findingCount': len(findings),
        'findings': findings,
        'recommendedActions': [
            'Estender PjbFlowItBase (ou PjbH2ItBase/PjbTransactionalRepositoryItBase) em vez de PjbIntegrationTestBase direto quando o teste persiste dado real sem @Transactional.',
            'Se PjbIntegrationTestBase direto for realmente necessário, adicionar @BeforeEach/@AfterEach de limpeza própria ou @Sql de setup/teardown.',
            'Ver debt_pjbflowitbase_cleanup_only_beforeeach na memória do projeto: PjbFlowItBase só limpa @BeforeEach, nunca depois do último teste da classe — mesmo migrando pra lá, resíduo entre classes ainda é possível.',
        ],
    }

    REPORT_JSON.parent.mkdir(parents=True, exist_ok=True)
    REPORT_JSON.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')

    lines = [
        '# Test Isolation Guard',
        '',
        f"- Base analisada: `{SRC_TEST}`",
        f"- Classes *IT.java analisadas: **{total_it_classes}**",
        f"- Sem isolamento entre testes: **{len(findings)}**",
        '',
        '## Classes sem isolamento',
        '',
    ]
    if findings:
        for entry in findings:
            lines.append(f"- `{entry['file']}` — {entry['reason']}")
    else:
        lines.append('- Nenhuma classe *IT.java sem isolamento detectada.')
    lines.extend(['', '## Ações recomendadas', ''])
    for action in report['recommendedActions']:
        lines.append(f'- {action}')
    REPORT_MD.write_text('\n'.join(lines) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()

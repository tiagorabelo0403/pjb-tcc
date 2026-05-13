from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

from project_roots import ROOT

SRC = ROOT / 'pjb-api' / 'src' / 'main' / 'java'
REPORT_JSON = ROOT / 'docs' / 'reports' / 'transactional_hotspot_guard.json'
REPORT_MD = ROOT / 'docs' / 'reports' / 'transactional_hotspot_guard.md'

TX_METHOD_RE = re.compile(r'(?P<annotations>(?:\s*@[^\n]+\n)+)\s*public\s+[^{]+\{', re.MULTILINE)
BUDGET_RE = re.compile(r'@PjbTransactionalBudget(?:\([^)]*\))?')
SUSPICIOUS_TOKENS = (
    'findAll(',
    'saveAll(',
    'outboxPublisher.enqueue(',
    'appendSafely(',
    'processUnified(',
    'iaOrchestrator.',
    'for (',
    '.stream()',
)
HOTSPOT_PACKAGES = (
    'core/transito',
    'integration/judicial/financeiro',
    'integration/datajud/feed',
    'service/security/govbr',
)


def extract_method_name(block: str) -> str:
    signature = block.split('{', 1)[0]
    tail = signature.split('public', 1)[-1].strip()
    return tail.split('(', 1)[0].split()[-1]


def in_hotspot(path: Path) -> bool:
    posix = path.as_posix()
    return any(pkg in posix for pkg in HOTSPOT_PACKAGES)


def extract_block(text: str, start: int) -> str:
    brace_start = text.find('{', start)
    if brace_start == -1:
        return text[start:]
    depth = 0
    for index in range(brace_start, len(text)):
        char = text[index]
        if char == '{':
            depth += 1
        elif char == '}':
            depth -= 1
            if depth == 0:
                return text[start:index + 1]
    return text[start:]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--fail-on-findings', action='store_true')
    parser.add_argument('--fail-on-missing-budgets', action='store_true')
    args = parser.parse_args()

    findings: list[dict[str, object]] = []
    missing_budgets: list[dict[str, object]] = []
    scanned = 0

    for path in SRC.rglob('*.java'):
        text = path.read_text(encoding='utf-8', errors='ignore')
        scanned += 1
        for match in TX_METHOD_RE.finditer(text):
            annotations = match.group('annotations')
            if '@Transactional' not in annotations:
                continue
            block = extract_block(text, match.start())
            method = extract_method_name(block)
            tokens = [token for token in SUSPICIOUS_TOKENS if token in block]
            if tokens:
                findings.append({
                    'file': str(path.relative_to(ROOT)),
                    'method': method,
                    'signals': tokens,
                })
            if in_hotspot(path) and not BUDGET_RE.search(annotations):
                missing_budgets.append({
                    'file': str(path.relative_to(ROOT)),
                    'method': method,
                })

    findings.sort(key=lambda item: (len(item['signals']), item['file'], item['method']), reverse=True)
    missing_budgets.sort(key=lambda item: (item['file'], item['method']))
    report = {
        'scannedFiles': scanned,
        'hotspots': findings,
        'hotspotCount': len(findings),
        'missingBudgets': missing_budgets,
        'missingBudgetCount': len(missing_budgets),
    }
    REPORT_JSON.parent.mkdir(parents=True, exist_ok=True)
    REPORT_JSON.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')

    lines = [
        '# Transactional Hotspot Guard',
        '',
        f'Arquivos varridos: {scanned}',
        f'Hotspots: {len(findings)}',
        f'Métodos hotspot sem budget: {len(missing_budgets)}',
        '',
    ]
    for item in findings[:100]:
        lines.append(f"- `{item['file']}` :: `{item['method']}` :: {', '.join(item['signals'])}")
    if missing_budgets:
        lines.extend(['', '## Métodos hotspot sem budget', ''])
        for item in missing_budgets[:100]:
            lines.append(f"- `{item['file']}` :: `{item['method']}`")
    REPORT_MD.write_text('\n'.join(lines) + '\n', encoding='utf-8')

    if args.fail_on_findings and (findings or missing_budgets):
        return 1
    if args.fail_on_missing_budgets and missing_budgets:
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

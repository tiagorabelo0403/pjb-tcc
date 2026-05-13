from __future__ import annotations

from dataclasses import dataclass, asdict
from pathlib import Path
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
TEST_ROOT = ROOT / 'pjb-api' / 'src' / 'test' / 'java'
REPORT_JSON = ROOT / 'docs' / 'reports' / 'test_drift_scan_batch4.json'
REPORT_MD = ROOT / 'docs' / 'reports' / 'test_drift_scan_batch4.md'

@dataclass(frozen=True)
class Finding:
    category: str
    file: str
    detail: str


def java_files() -> list[Path]:
    return sorted(TEST_ROOT.rglob('*.java'))


def imports_of(text: str) -> set[str]:
    return {line.strip() for line in text.splitlines() if line.startswith('import ')}


def add_missing_import_findings(path: Path, text: str) -> list[Finding]:
    imports = imports_of(text)
    findings: list[Finding] = []
    checks = [
        ('assertThat', 'import static org.assertj.core.api.Assertions.assertThat;'),
        ('assertThatThrownBy', 'import static org.assertj.core.api.Assertions.assertThatThrownBy;'),
        ('when', 'import static org.mockito.Mockito.when;'),
        ('verify', 'import static org.mockito.Mockito.verify;'),
        ('mock', 'import static org.mockito.Mockito.mock;'),
        ('any', 'import static org.mockito.ArgumentMatchers.any;'),
        ('eq', 'import static org.mockito.ArgumentMatchers.eq;'),
        ('never', 'import static org.mockito.Mockito.never;'),
    ]
    declared_methods = set(re.findall(r'\b(?:void|int|long|boolean|String|var|[A-Z]\w*(?:<[^>]+>)?)\s+(\w+)\s*\(', text))
    for symbol, import_stmt in checks:
        if symbol in declared_methods:
            continue
        if re.search(rf'(?<![\w.]){re.escape(symbol)}\s*\(', text):
            if f'Mockito.{symbol}(' in text:
                continue
            if import_stmt not in imports:
                if symbol in {'when', 'verify', 'mock', 'never'} and 'import static org.mockito.Mockito.*;' in imports:
                    continue
                if symbol in {'any', 'eq'} and 'import static org.mockito.ArgumentMatchers.*;' in imports:
                    continue
                if symbol in {'assertThat', 'assertThatThrownBy'} and 'import static org.assertj.core.api.Assertions.*;' in imports:
                    continue
                findings.append(Finding('missing_static_import', str(path.relative_to(ROOT)), symbol))
    return findings


def add_legacy_pattern_findings(path: Path, text: str) -> list[Finding]:
    findings: list[Finding] = []
    if ('PublicPageResolveResponse' in text or 'resolvePublicPage' in text) and re.search(r'\.(pageId|processoId|numeroUnificado|publicActKind|texto)\(\)', text):
        findings.append(Finding('legacy_public_page_accessor', str(path.relative_to(ROOT)), 'PublicPageResolveResponse likely migrated to bean getters'))
    if 'GuardRailSnapshot' in text and '.verdict()' in text:
        findings.append(Finding('legacy_guardrail_accessor', str(path.relative_to(ROOT)), 'GuardRailSnapshot no longer exposes verdict()'))
    if 'QualifiedDocumentSignatureEnvelopeService' in text and 'OfficeProcessWorkspaceScopeService' in text:
        if 'ObjectProvider<OfficeProcessWorkspaceScopeService>' not in text and 'getBeanProvider(OfficeProcessWorkspaceScopeService.class)' not in text and 'getBeanProvider(com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService.class)' not in text:
            findings.append(Finding('object_provider_mismatch', str(path.relative_to(ROOT)), 'QualifiedDocumentSignatureEnvelopeService expects ObjectProvider<OfficeProcessWorkspaceScopeService>'))
    if 'OfflineApplicationService' in text and 'new OfflineApplicationService(' in text and 'PjbPlatformMessageCatalog' not in text:
        findings.append(Finding('constructor_drift_catalog', str(path.relative_to(ROOT)), 'OfflineApplicationService expects PjbPlatformMessageCatalog'))
    return findings


def scan() -> list[Finding]:
    findings: list[Finding] = []
    for path in java_files():
        text = path.read_text(encoding='utf-8', errors='ignore')
        findings.extend(add_missing_import_findings(path, text))
        findings.extend(add_legacy_pattern_findings(path, text))
    return findings


def write_reports(findings: list[Finding]) -> None:
    REPORT_JSON.write_text(json.dumps([asdict(f) for f in findings], indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    grouped: dict[str, list[Finding]] = {}
    for finding in findings:
        grouped.setdefault(finding.category, []).append(finding)
    lines = ['# Test Drift Scan — Batch 4', '', f'Total de achados: {len(findings)}', '']
    for category in sorted(grouped):
        lines.append(f'## {category}')
        for finding in sorted(grouped[category], key=lambda item: item.file):
            lines.append(f'- `{finding.file}` — {finding.detail}')
        lines.append('')
    if not findings:
        lines.append('Nenhum achado heurístico encontrado.')
    REPORT_MD.write_text('\n'.join(lines) + '\n', encoding='utf-8')


def main() -> int:
    findings = scan()
    write_reports(findings)
    if findings:
        for finding in findings:
            print(f'[{finding.category}] {finding.file} :: {finding.detail}')
        return 1
    print('test_drift_guard.py: nenhum achado heurístico')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

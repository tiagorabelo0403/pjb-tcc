from __future__ import annotations

import json
import re
import sys

from project_roots import ROOT, SRC_TEST

REPORT_JSON = ROOT / 'docs' / 'reports' / 'test_isolation_guard.json'
REPORT_MD = ROOT / 'docs' / 'reports' / 'test_isolation_guard.md'

BASES = {'PjbIntegrationTestBase', 'PjbFlowItBase', 'PjbTransactionalRepositoryItBase', 'PjbH2ItBase'}
DIRECT_BASE = re.compile(r'class\s+(\w+)\s+extends\s+PjbIntegrationTestBase\b')
FLOW_BASE = re.compile(r'class\s+(\w+)\s+extends\s+PjbFlowItBase\b')
TEST_METHOD = re.compile(r'^\s*@Test\b', re.M)
METHOD_SIGNATURE = re.compile(r'\bvoid\s+\w+\s*\(')
PRESERVED_CATALOG_WRITE = re.compile(
    r'(?i:insert\s+into\s+(tb_tribunal|tb_comarca|tb_jurisdicao_territorial\w*)\b)'
    r'|\b(tribunalRepository|comarcaRepository|jurisdicao\w*Repository|repository)\.save\w*\(\s*new\s+(Tribunal|Comarca|JurisdicaoTerritorial\w*)\(')
AFTER_EACH_METHOD = re.compile(r'@AfterEach\s+(?:\w+\s+)*void\s+\w+\s*\(\s*\)\s*\{(?P<body>.*?)\n    \}', re.S)
DELETE_CALL = re.compile(r'(?i:\bdelete\s+from\b)|\.delete\w*\(')


def class_annotations(text: str, class_start: int) -> str:
    collected = []
    for line in reversed(text[:class_start].rstrip().split('\n')):
        stripped = line.strip()
        if not stripped:
            continue
        if stripped.startswith(('@', ')', '"', '}')) or stripped.endswith((',', '(', '{')):
            collected.append(stripped)
            continue
        break
    return ' '.join(reversed(collected))


def rolls_back_every_test(text: str, class_start: int) -> bool:
    if re.search(r'@Transactional\b', class_annotations(text, class_start)):
        return True
    starts = [match.start() for match in TEST_METHOD.finditer(text)]
    if not starts:
        return False
    for start in starts:
        signature = METHOD_SIGNATURE.search(text, start)
        if signature is None or '@Transactional' not in text[start:signature.start()]:
            return False
    return True


def deletes_after_each(text: str) -> bool:
    return any(DELETE_CALL.search(match.group('body')) for match in AFTER_EACH_METHOD.finditer(text))


def main() -> int:
    scanned = 0
    rollback = []
    flow = []
    findings = []

    for path in sorted(SRC_TEST.rglob('*.java')):
        if path.stem in BASES:
            continue
        text = path.read_text(encoding='utf-8', errors='ignore')
        direct = DIRECT_BASE.search(text)
        on_flow = FLOW_BASE.search(text)
        if not direct and not on_flow:
            continue
        scanned += 1
        relative = str(path.relative_to(ROOT))
        if direct:
            if rolls_back_every_test(text, direct.start()):
                rollback.append(relative)
            else:
                findings.append({'file': relative,
                                 'reason': 'estende PjbIntegrationTestBase diretamente sem rollback em todos os testes; '
                                           'estenda PjbFlowItBase ou marque a classe ou cada @Test com @Transactional.'})
            continue
        flow.append(relative)
        if PRESERVED_CATALOG_WRITE.search(text) and not deletes_after_each(text):
            findings.append({'file': relative,
                             'reason': 'grava em catalogo que o TRUNCATE do PjbFlowItBase preserva '
                                       '(tb_tribunal, tb_comarca, tb_jurisdicao_territorial*) sem apagar em @AfterEach.'})

    report = {
        'scanned': scanned,
        'isolatedByFlowBase': len(flow),
        'rollbackEveryTest': len(rollback),
        'findingCount': len(findings),
        'findings': findings,
    }
    REPORT_JSON.parent.mkdir(parents=True, exist_ok=True)
    REPORT_JSON.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    lines = ['# Test Isolation Guard', '',
             f'- Classes de integracao analisadas: **{scanned}**',
             f'- Isoladas pelo PjbFlowItBase: **{len(flow)}**',
             f'- PjbIntegrationTestBase direto com rollback em todo teste: **{len(rollback)}**',
             f'- Achados: **{len(findings)}**', '']
    lines += [f"- `{entry['file']}` — {entry['reason']}" for entry in findings]
    REPORT_MD.write_text('\n'.join(lines) + '\n', encoding='utf-8')

    if scanned == 0 or not flow:
        print('TEST ISOLATION GUARD: FAIL')
        print('Nenhuma classe de integracao sobre as bases conhecidas foi encontrada; o guard esta olhando o lugar errado.')
        return 1
    if findings:
        print('TEST ISOLATION GUARD: FAIL')
        print('A ordem das classes de integracao muda a cada checkout; estado deixado por uma quebra outra:')
        for entry in findings:
            print(f" - {entry['file']}: {entry['reason']}")
        return 1
    print(f'TEST ISOLATION GUARD: OK ({scanned} classes; {len(flow)} no PjbFlowItBase, '
          f'{len(rollback)} com rollback em todo teste)')
    return 0


if __name__ == '__main__':
    sys.exit(main())

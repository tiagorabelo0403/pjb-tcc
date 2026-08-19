from __future__ import annotations

import json
import re
from pathlib import Path

from project_roots import ROOT, SRC_MAIN, CORE_MAIN

REPORT_JSON = ROOT / 'docs' / 'reports' / 'hibernate_filter_definition_guard.json'
REPORT_MD = ROOT / 'docs' / 'reports' / 'hibernate_filter_definition_guard.md'

SCAN_ROOTS = [root for root in (SRC_MAIN, CORE_MAIN) if root.exists()]

FILTER_DEF_NAME_PATTERN = re.compile(r'@FilterDef\s*\(\s*name\s*=\s*"([^"]+)"')
FILTER_NAME_PATTERN = re.compile(r'@Filter\s*\(\s*name\s*=\s*"([^"]+)"')
CONDITION_PATTERN = re.compile(
    r'condition\s*=\s*((?:"(?:\\.|[^"\\])*"\s*(?:\+\s*)?)+)'
)
STRING_LITERAL_PATTERN = re.compile(r'"((?:\\.|[^"\\])*)"')

CLASS_HEADER_PATTERN = re.compile(
    r'((?:@[A-Za-z][A-Za-z0-9_.]*(?:\([^)]*(?:\([^)]*\)[^)]*)*\))?\s*)*)'
    r'(?:public\s+)?(?:abstract\s+|final\s+)?class\s+([A-Z][A-Za-z0-9_]*)\b'
)
CONDITIONAL_ON_BEAN_PATTERN = re.compile(r'@Conditional(?:OnBean|OnMissingBean)\s*\(([^)]*)\)')
RISKY_TYPE_PATTERN = re.compile(r'\b(?:EntityManager|EntityManagerFactory|\w+Repository)\b')


def strip_comments(text: str) -> str:
    without_block = re.sub(r'/\*.*?\*/', '', text, flags=re.DOTALL)
    return re.sub(r'//[^\n]*', '', without_block)


def line_of(text: str, index: int) -> int:
    return text.count('\n', 0, index) + 1


def condition_paren_balance(raw_value: str) -> tuple[int, int]:
    literal = ''.join(STRING_LITERAL_PATTERN.findall(raw_value))
    literal = literal.replace('\\"', '"')
    return literal.count('('), literal.count(')')


def scan_filter_defs_and_conditions(paths: list[Path]) -> tuple[set[str], list[dict], list[dict]]:
    defined_names: set[str] = set()
    filter_usages: list[dict] = []
    condition_findings: list[dict] = []

    for path in paths:
        raw = path.read_text(encoding='utf-8', errors='ignore')
        text = strip_comments(raw)
        rel = str(path.relative_to(ROOT))

        for match in FILTER_DEF_NAME_PATTERN.finditer(text):
            defined_names.add(match.group(1))

        for match in FILTER_NAME_PATTERN.finditer(text):
            filter_usages.append({
                'file': rel,
                'line': line_of(text, match.start()),
                'name': match.group(1),
            })

        for match in CONDITION_PATTERN.finditer(text):
            opens, closes = condition_paren_balance(match.group(1))
            if opens != closes:
                condition_findings.append({
                    'file': rel,
                    'line': line_of(text, match.start()),
                    'opens': opens,
                    'closes': closes,
                })

    return defined_names, filter_usages, condition_findings


def scan_conditional_on_bean_on_component(paths: list[Path]) -> list[dict]:
    findings: list[dict] = []
    for path in paths:
        raw = path.read_text(encoding='utf-8', errors='ignore')
        text = strip_comments(raw)
        rel = str(path.relative_to(ROOT))

        match = CLASS_HEADER_PATTERN.search(text)
        if not match:
            continue
        annotations_block = match.group(1) or ''
        class_name = match.group(2)

        is_stereotype = bool(re.search(r'@(Component|Service|Repository|RestController|Controller)\b', annotations_block))
        is_configuration = '@Configuration' in annotations_block
        if not is_stereotype or is_configuration:
            continue

        conditional_match = CONDITIONAL_ON_BEAN_PATTERN.search(annotations_block)
        if not conditional_match:
            continue

        referenced = conditional_match.group(1)
        risky_types = RISKY_TYPE_PATTERN.findall(referenced)
        if not risky_types:
            continue

        findings.append({
            'file': rel,
            'className': class_name,
            'riskyTypes': sorted(set(risky_types)),
            'reason': (
                '@ConditionalOnBean/@ConditionalOnMissingBean no nivel da classe, numa classe '
                '@Component/@Service/@Repository (nao @Configuration), referenciando um tipo de '
                'infraestrutura JPA/Spring Data (EntityManager ou *Repository). Essa condicao e '
                'avaliada durante o component-scan, antes desses beans existirem, e resolve falso '
                'sempre — o bean anotado nunca e criado, sem erro nenhum (ver '
                'EquipeSwitchInterceptor, D-equipe-switch-interceptor-noop-quatro-bugs-empilhados '
                'em docs/quality/DEBT_LOG.md). Mover para um metodo @Bean dentro de uma classe '
                '@Configuration, onde a condicao e avaliada tarde o suficiente.'
            ),
        })
    return findings


def main() -> None:
    java_files = sorted(
        path
        for root in SCAN_ROOTS
        for path in root.rglob('*.java')
    )

    defined_names, filter_usages, condition_findings = scan_filter_defs_and_conditions(java_files)
    conditional_bean_findings = scan_conditional_on_bean_on_component(java_files)

    missing_filter_def_findings = [
        usage for usage in filter_usages if usage['name'] not in defined_names
    ]

    report = {
        'filesScanned': len(java_files),
        'filterDefNamesFound': sorted(defined_names),
        'missingFilterDefFindings': missing_filter_def_findings,
        'unbalancedConditionFindings': condition_findings,
        'conditionalOnBeanOnComponentFindings': conditional_bean_findings,
    }

    REPORT_JSON.parent.mkdir(parents=True, exist_ok=True)
    REPORT_JSON.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')

    total_findings = (
        len(missing_filter_def_findings)
        + len(condition_findings)
        + len(conditional_bean_findings)
    )

    lines = [
        '# Hibernate Filter Definition Guard',
        '',
        f'- Arquivos Java escaneados: **{len(java_files)}**',
        f'- Nomes de @FilterDef encontrados: {", ".join(sorted(defined_names)) or "(nenhum)"}',
        f'- Total de achados: **{total_findings}**',
        '',
        '## @Filter sem @FilterDef correspondente',
        '',
    ]
    if missing_filter_def_findings:
        for f in missing_filter_def_findings:
            lines.append(f"- `{f['file']}:{f['line']}` — @Filter(name = \"{f['name']}\") sem @FilterDef correspondente em lugar nenhum do codigo")
    else:
        lines.append('- Nenhum achado.')

    lines.extend(['', '## @Filter com condition SQL de parenteses desbalanceados', ''])
    if condition_findings:
        for f in condition_findings:
            lines.append(f"- `{f['file']}:{f['line']}` — {f['opens']} abertos vs {f['closes']} fechados")
    else:
        lines.append('- Nenhum achado.')

    lines.extend(['', '## @ConditionalOnBean/@ConditionalOnMissingBean arriscado em @Component', ''])
    if conditional_bean_findings:
        for f in conditional_bean_findings:
            lines.append(f"- `{f['file']}` — `{f['className']}` referencia {', '.join(f['riskyTypes'])}")
    else:
        lines.append('- Nenhum achado.')

    REPORT_MD.write_text('\n'.join(lines) + '\n', encoding='utf-8')

    if total_findings:
        raise SystemExit(1)


if __name__ == '__main__':
    main()

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

from java_source import (
    count_parameters,
    matching_brace,
    matching_paren,
    strip_comments_and_strings,
)
from project_roots import CORE_MAIN, ROOT, SRC_MAIN

SOURCE_ROOTS = (SRC_MAIN, CORE_MAIN)
BUDGET_FILE = ROOT / 'config' / 'constructor-dependency-budget.json'
REPORT_JSON = ROOT / 'docs' / 'reports' / 'constructor_injection_guard.json'
REPORT_MD = ROOT / 'docs' / 'reports' / 'constructor_injection_guard.md'

CEILING = 8
MAX_BUDGET_ENTRIES = 178

STEREOTYPES = ('Component', 'Service', 'Repository', 'Configuration', 'Controller',
               'RestController', 'ControllerAdvice', 'RestControllerAdvice')

TYPE_DECLARATION = re.compile(
    r'((?:@[A-Za-z][\w.]*(?:\([^)]*\))?\s+)+)'
    r'(?:(?:public|protected|private|static|final|abstract|sealed|non-sealed)\s+)*'
    r'(class|record)\s+([A-Z]\w*)',
)
FIELD_DECLARATION = re.compile(
    r'^[ \t]*((?:@[A-Za-z][\w.]*(?:\([^)]*\))?\s+)*)'
    r'(?:(?:public|protected|private)\s+)?(static\s+)?(final\s+)?(?:volatile\s+|transient\s+)?'
    r'[\w.$]+(?:\s*<[^;=]*>)?(?:\s*\[\s*\])*\s+\w+\s*(=[^;]*)?;',
    re.MULTILINE,
)
BEAN_METHOD = re.compile(
    r'@Bean(?:\([^)]*\))?\s+(?:@[A-Za-z][\w.]*(?:\([^)]*\))?\s+)*'
    r'(?:(?:public|protected|private|static|final)\s+)*'
    r'[\w.$<>,\[\]\s]+?\s+(\w+)\s*\(',
)


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def annotations_of(block: str) -> set[str]:
    return {name.split('.')[-1] for name in re.findall(r'@([A-Za-z][\w.]*)', block)}


def type_body(text: str, declaration_end: int) -> tuple[str, str]:
    header_end = declaration_end
    if text[header_end:header_end + 1] == '(':
        header_end = matching_paren(text, header_end) + 1
    brace = text.find('{', header_end)
    if brace == -1:
        return text[declaration_end:], ''
    end = matching_brace(text, brace)
    return text[declaration_end:brace], text[brace + 1:end if end != -1 else len(text)]


def strip_nested_bodies(body: str) -> str:
    result = []
    depth = 0
    for char in body:
        if char == '{':
            depth += 1
            continue
        if char == '}':
            depth = max(0, depth - 1)
            continue
        if depth == 0:
            result.append(char)
    return ''.join(result)


def direct_fields(body: str) -> list[dict[str, bool]]:
    fields = []
    for match in FIELD_DECLARATION.finditer(strip_nested_bodies(body)):
        fields.append({
            'static': bool(match.group(2)),
            'final': bool(match.group(3)),
            'initialized': bool(match.group(4)),
            'nonNull': 'NonNull' in annotations_of(match.group(1)),
        })
    return fields


def explicit_constructors(body: str, type_name: str) -> list[int]:
    header = re.compile(
        r'(?:^|[;}{\s])(?:@[A-Za-z][\w.]*(?:\([^)]*\))?\s+)*'
        r'(?:(?:public|protected|private)\s+)?(?:<[^>]+>\s*)?'
        + re.escape(type_name) + r'\s*\(',
    )
    counts = []
    for match in header.finditer(body):
        open_paren = body.rfind('(', 0, match.end())
        close_paren = matching_paren(body, open_paren)
        if close_paren == -1:
            continue
        following = body[close_paren + 1:close_paren + 40].lstrip()
        if not (following.startswith('{') or following.startswith('throws')):
            continue
        counts.append(count_parameters(body[open_paren + 1:close_paren]))
    return counts


def lombok_constructor_size(annotations: set[str], body: str) -> int | None:
    fields = [field for field in direct_fields(body) if not field['static']]
    if 'AllArgsConstructor' in annotations:
        return sum(1 for field in fields if not (field['final'] and field['initialized']))
    if 'RequiredArgsConstructor' in annotations:
        return sum(1 for field in fields
                   if not field['initialized'] and (field['final'] or field['nonNull']))
    return None


def scan_file(path: Path) -> list[dict[str, object]]:
    text = strip_comments_and_strings(path.read_text(encoding='utf-8', errors='ignore'))
    entries: list[dict[str, object]] = []
    for match in TYPE_DECLARATION.finditer(text):
        annotations = annotations_of(match.group(1))
        if not annotations.intersection(STEREOTYPES):
            continue
        kind, name = match.group(2), match.group(3)
        header, body = type_body(text, match.end())
        key = relative(path) if name == path.stem else f'{relative(path)}#{name}'
        if kind == 'record':
            open_paren = header.find('(')
            close_paren = matching_paren(header, open_paren) if open_paren != -1 else -1
            size = count_parameters(header[open_paren + 1:close_paren]) if close_paren != -1 else 0
            entries.append({'key': key, 'count': size, 'origin': 'record'})
            continue
        explicit = explicit_constructors(body, name)
        if explicit:
            entries.append({'key': key, 'count': max(explicit), 'origin': 'construtor'})
            continue
        lombok = lombok_constructor_size(annotations, body)
        if lombok is not None:
            entries.append({'key': key, 'count': lombok, 'origin': 'lombok'})
    for match in BEAN_METHOD.finditer(text):
        open_paren = text.find('(', match.end() - 1)
        close_paren = matching_paren(text, open_paren)
        if close_paren == -1:
            continue
        entries.append({
            'key': f'{relative(path)}::{match.group(1)}',
            'count': count_parameters(text[open_paren + 1:close_paren]),
            'origin': 'metodo @Bean',
        })
    return entries


def scan() -> dict[str, dict[str, object]]:
    measured: dict[str, dict[str, object]] = {}
    for root in SOURCE_ROOTS:
        if not root.exists():
            continue
        for path in sorted(root.rglob('*.java')):
            for entry in scan_file(path):
                measured[str(entry['key'])] = entry
    return measured


def load_budget() -> dict[str, int]:
    if not BUDGET_FILE.exists():
        return {}
    return json.loads(BUDGET_FILE.read_text(encoding='utf-8'))


def save_budget(budget: dict[str, int]) -> None:
    BUDGET_FILE.write_text(json.dumps(dict(sorted(budget.items())), ensure_ascii=False, indent=2) + '\n',
                           encoding='utf-8')


def tighten(budget: dict[str, int], measured: dict[str, dict[str, object]]) -> dict[str, int]:
    tightened = {}
    for key, allowed in budget.items():
        entry = measured.get(key)
        if entry is None or int(entry['count']) <= CEILING:
            continue
        tightened[key] = min(allowed, int(entry['count']))
    return tightened


def evaluate(budget: dict[str, int], measured: dict[str, dict[str, object]]) -> list[str]:
    violations = []
    for key, entry in sorted(measured.items()):
        count = int(entry['count'])
        if count <= CEILING:
            continue
        allowed = budget.get(key)
        if allowed is None:
            violations.append(f'{key}: {count} dependências ({entry["origin"]}), teto {CEILING}, sem orçamento')
        elif count > allowed:
            violations.append(f'{key}: {count} dependências ({entry["origin"]}), orçamento {allowed}')
    for key, allowed in sorted(budget.items()):
        entry = measured.get(key)
        if entry is None:
            violations.append(f'{key}: orçamento para ponto de injeção que não existe mais')
            continue
        count = int(entry['count'])
        if count <= CEILING:
            violations.append(f'{key}: {count} dependências, dentro do teto, orçamento obsoleto')
        elif count < allowed:
            violations.append(f'{key}: {count} dependências, orçamento {allowed} ficou folgado')
    if len(budget) > MAX_BUDGET_ENTRIES:
        violations.append(f'orçamento com {len(budget)} entradas, acima do limite {MAX_BUDGET_ENTRIES}')
    return violations


def write_reports(measured: dict[str, dict[str, object]], budget: dict[str, int], violations: list[str]) -> None:
    acima = sorted(((int(entry['count']), key, str(entry['origin'])) for key, entry in measured.items()
                    if int(entry['count']) > CEILING), reverse=True)
    report = {
        'ceiling': CEILING,
        'maxBudgetEntries': MAX_BUDGET_ENTRIES,
        'measuredInjectionPoints': len(measured),
        'aboveCeiling': len(acima),
        'tenOrMore': sum(1 for entry in measured.values() if int(entry['count']) >= 10),
        'budgetEntries': len(budget),
        'violations': violations,
        'entries': [{'key': key, 'count': count, 'origin': origin} for count, key, origin in acima],
    }
    REPORT_JSON.parent.mkdir(parents=True, exist_ok=True)
    REPORT_JSON.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    lines = [
        '# Constructor Injection Guard',
        '',
        f'- Pontos de injeção medidos: **{len(measured)}**',
        f'- Teto por ponto de injeção: **{CEILING}**',
        f'- Acima do teto, com orçamento congelado: **{len(acima)}**',
        f"- Com 10 ou mais dependências: **{report['tenOrMore']}**",
        f'- Violações: **{len(violations)}**',
        '',
        '## Acima do teto',
        '',
    ]
    lines.extend(f'- `{key}` -> {count} ({origin})' for count, key, origin in acima[:40])
    REPORT_MD.write_text('\n'.join(lines) + '\n', encoding='utf-8')


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--tighten', action='store_true')
    args = parser.parse_args()

    measured = scan()
    budget = load_budget()
    if args.tighten:
        budget = tighten(budget, measured)
        save_budget(budget)

    violations = evaluate(budget, measured)
    write_reports(measured, budget, violations)

    print(f'Pontos de injeção medidos: {len(measured)}')
    print(f'Acima do teto {CEILING}: {sum(1 for entry in measured.values() if int(entry["count"]) > CEILING)}')
    for violation in violations:
        print(f'VIOLACAO {violation}', file=sys.stderr)
    return 1 if violations else 0


if __name__ == '__main__':
    raise SystemExit(main())

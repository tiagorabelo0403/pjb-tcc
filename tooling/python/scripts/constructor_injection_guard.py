from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

from project_roots import CORE_MAIN, ROOT, SRC_MAIN

SOURCE_ROOTS = (SRC_MAIN, CORE_MAIN)
BUDGET_FILE = ROOT / 'config' / 'constructor-dependency-budget.json'
REPORT_JSON = ROOT / 'docs' / 'reports' / 'constructor_injection_guard.json'
REPORT_MD = ROOT / 'docs' / 'reports' / 'constructor_injection_guard.md'

CEILING = 8

STEREOTYPE = re.compile(
    r'^\s*@(?:Service|Component|Controller|RestController|Repository|Configuration'
    r'|ControllerAdvice|RestControllerAdvice)\b',
    re.MULTILINE,
)
FIELD_AUTOWIRED = re.compile(
    r'@Autowired(?:\([^)]*\))?\s+(?:@\w+(?:\([^)]*\))?\s+)*'
    r'(?:(?:private|protected|public|static|final|transient|volatile)\s+)*'
    r'[\w<>\[\],.?\s]+?\s+\w+\s*(?:=[^;]*)?;'
)


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def line_of(text: str, offset: int) -> int:
    return text.count('\n', 0, offset) + 1


def count_parameters(signature: str) -> int:
    depth = 0
    count = 0
    current = ''
    for char in signature:
        if char in '<(':
            depth += 1
        elif char in '>)':
            depth -= 1
        elif char == ',' and depth == 0:
            if current.strip():
                count += 1
            current = ''
            continue
        current += char
    return count + (1 if current.strip() else 0)


def widest_constructor(text: str, type_name: str) -> int | None:
    header = re.compile(
        r'^[ \t]*(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:public|protected|private)\s+)?'
        + re.escape(type_name) + r'\s*\(',
        re.MULTILINE,
    )
    widest = None
    for match in header.finditer(text):
        start = match.end()
        depth = 1
        end = start
        while depth and end < len(text):
            if text[end] == '(':
                depth += 1
            elif text[end] == ')':
                depth -= 1
            end += 1
        count = count_parameters(text[start:end - 1])
        widest = count if widest is None else max(widest, count)
    return widest


def scan() -> tuple[dict[str, int], list[dict[str, object]], int]:
    beans: dict[str, int] = {}
    field_injections: list[dict[str, object]] = []
    total = 0
    for root in SOURCE_ROOTS:
        if not root.exists():
            continue
        for path in sorted(root.rglob('*.java')):
            text = path.read_text(encoding='utf-8', errors='ignore')
            for match in FIELD_AUTOWIRED.finditer(text):
                field_injections.append({'file': relative(path), 'line': line_of(text, match.start())})
            if not STEREOTYPE.search(text):
                continue
            count = widest_constructor(text, path.stem)
            if count is None:
                continue
            total += 1
            beans[relative(path)] = count
    return beans, field_injections, total


def load_budget() -> dict[str, int]:
    if not BUDGET_FILE.exists():
        return {}
    return json.loads(BUDGET_FILE.read_text(encoding='utf-8'))


def save_budget(budget: dict[str, int]) -> None:
    ordered = dict(sorted(budget.items()))
    BUDGET_FILE.write_text(json.dumps(ordered, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def tighten(budget: dict[str, int], beans: dict[str, int]) -> dict[str, int]:
    tightened: dict[str, int] = {}
    for file, allowed in budget.items():
        current = beans.get(file)
        if current is None or current <= CEILING:
            continue
        tightened[file] = min(allowed, current)
    return tightened


def evaluate(budget: dict[str, int], beans: dict[str, int]) -> list[str]:
    violations: list[str] = []
    for file, count in sorted(beans.items()):
        if count <= CEILING:
            continue
        allowed = budget.get(file)
        if allowed is None:
            violations.append(f'{file}: {count} dependências no construtor, teto {CEILING}, sem orçamento')
        elif count > allowed:
            violations.append(f'{file}: {count} dependências no construtor, orçamento {allowed}')
    for file, allowed in sorted(budget.items()):
        current = beans.get(file)
        if current is None:
            violations.append(f'{file}: orçamento para bean que não existe mais')
        elif current < allowed:
            violations.append(f'{file}: {current} dependências, orçamento {allowed} ficou folgado')
        elif current <= CEILING:
            violations.append(f'{file}: {current} dependências, dentro do teto, orçamento obsoleto')
    return violations


def write_reports(beans: dict[str, int], budget: dict[str, int], field_injections: list[dict[str, object]],
                  violations: list[str], total: int) -> None:
    over = sorted(((count, file) for file, count in beans.items() if count > CEILING), reverse=True)
    report = {
        'ceiling': CEILING,
        'beansWithConstructor': total,
        'beansAboveCeiling': len(over),
        'beansWithTenOrMore': sum(1 for count in beans.values() if count >= 10),
        'budgetEntries': len(budget),
        'fieldInjections': field_injections,
        'violations': violations,
        'aboveCeiling': [{'file': file, 'constructorParameterCount': count} for count, file in over],
    }
    REPORT_JSON.parent.mkdir(parents=True, exist_ok=True)
    REPORT_JSON.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    lines = [
        '# Constructor Injection Guard',
        '',
        f'- Beans Spring com construtor: **{total}**',
        f'- Teto de dependências por construtor: **{CEILING}**',
        f'- Beans acima do teto (com orçamento congelado): **{len(over)}**',
        f"- Beans com 10 ou mais dependências: **{report['beansWithTenOrMore']}**",
        f'- `@Autowired` em field: **{len(field_injections)}**',
        f'- Violações: **{len(violations)}**',
        '',
        '## Acima do teto',
        '',
    ]
    lines.extend(f'- `{file}` -> {count}' for count, file in over[:40])
    REPORT_MD.write_text('\n'.join(lines) + '\n', encoding='utf-8')


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--tighten', action='store_true')
    args = parser.parse_args()

    beans, field_injections, total = scan()
    budget = load_budget()
    if args.tighten:
        budget = tighten(budget, beans)
        save_budget(budget)

    violations = [f"{item['file']}:{item['line']}: @Autowired em field" for item in field_injections]
    violations.extend(evaluate(budget, beans))
    write_reports(beans, budget, field_injections, violations, total)

    print(f'Beans com construtor: {total}')
    print(f'Acima do teto {CEILING}: {sum(1 for count in beans.values() if count > CEILING)}')
    print(f'@Autowired em field: {len(field_injections)}')
    for violation in violations:
        print(f'VIOLACAO {violation}', file=sys.stderr)
    return 1 if violations else 0


if __name__ == '__main__':
    raise SystemExit(main())

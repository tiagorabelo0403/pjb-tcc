from __future__ import annotations

import json
import re
from pathlib import Path

from project_roots import ROOT

SRC = ROOT / 'pjb-api' / 'src' / 'main' / 'java' / 'com' / 'tcc' / 'pjb' / 'backend'
REPORT_JSON = ROOT / 'docs' / 'reports' / 'spring_ambiguous_constructor_guard.json'
REPORT_MD = ROOT / 'docs' / 'reports' / 'spring_ambiguous_constructor_guard.md'

STEREOTYPE_PATTERN = re.compile(r'^\s*@(Component|Service|Repository|Configuration)\b', re.MULTILINE)
CLASS_PATTERN = re.compile(r'^\s*public\s+(?:abstract\s+|final\s+|sealed\s+|non-sealed\s+)?class\s+([A-Z][A-Za-z0-9_]*)\b', re.MULTILINE)


def strip_comments_and_strings(text: str) -> str:
    without_block = re.sub(r'/\*.*?\*/', '', text, flags=re.DOTALL)
    without_line = re.sub(r'//[^\n]*', '', without_block)
    without_strings = re.sub(r'"(?:\\.|[^"\\])*"', '""', without_line)
    return without_strings


def top_level_class(text: str) -> str | None:
    match = CLASS_PATTERN.search(text)
    return match.group(1) if match else None


def is_spring_stereotype(text: str) -> bool:
    return STEREOTYPE_PATTERN.search(text) is not None


def _find_matching_brace(text: str, open_index: int) -> int:
    depth = 0
    i = open_index
    while i < len(text):
        char = text[i]
        if char == '{':
            depth += 1
        elif char == '}':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def _delegates_to_this(body: str) -> bool:
    stripped = body.strip()
    return bool(re.match(r'this\s*\(', stripped))


def constructor_hits(text: str, class_name: str) -> list[dict[str, object]]:
    pattern = re.compile(
        r'((?:@[A-Za-z][A-Za-z0-9_.]*(?:\([^)]*\))?\s+)*)'
        r'(public\s+|protected\s+)?'
        + re.escape(class_name) + r'\s*\([^)]*\)\s*\{',
        re.MULTILINE,
    )
    hits = []
    for match in pattern.finditer(text):
        annotations_block = match.group(1) or ''
        visibility = (match.group(2) or '').strip()
        if visibility == 'protected':
            continue
        open_brace = match.end() - 1
        close_brace = _find_matching_brace(text, open_brace)
        body = text[open_brace + 1:close_brace] if close_brace > open_brace else ''
        if _delegates_to_this(body):
            continue
        line_no = text.count('\n', 0, match.start()) + 1
        has_autowired = bool(re.search(r'@(Autowired|Inject)\b', annotations_block))
        hits.append({
            'line': line_no,
            'visibility': visibility or 'package-private',
            'hasAutowired': has_autowired,
        })
    return hits


def main() -> None:
    findings = []
    stereotype_files_scanned = 0

    for path in sorted(SRC.rglob('*.java')):
        raw = path.read_text(encoding='utf-8', errors='ignore')
        stripped = strip_comments_and_strings(raw)
        if not is_spring_stereotype(stripped):
            continue
        stereotype_files_scanned += 1
        class_name = top_level_class(stripped)
        if class_name is None:
            continue
        ctors = constructor_hits(stripped, class_name)
        if len(ctors) < 2:
            continue
        if any(ctor['hasAutowired'] for ctor in ctors):
            continue
        findings.append({
            'file': str(path.relative_to(ROOT)),
            'className': class_name,
            'constructorCount': len(ctors),
            'constructors': ctors,
            'reason': 'Classe com estereotipo Spring possui multiplos construtores publicos/package-private e nenhum marca @Autowired ou @Inject; container caira no default constructor inexistente e a falha aparece apenas em runtime.',
        })

    findings.sort(key=lambda item: item['file'])

    report = {
        'base': str(SRC),
        'stereotypeFilesScanned': stereotype_files_scanned,
        'findingCount': len(findings),
        'findings': findings,
        'recommendedActions': [
            'Preferir construtor unico com dependencias reais (Spring escolhe automaticamente desde Spring 4).',
            'Se um segundo construtor existe apenas para teste, exponha a logica testada via metodo static ou reduza a visibilidade para private; nao deixe visivel ao Spring.',
            'Como ultimo recurso, marcar o construtor de producao com @Autowired explicito para desambiguar.',
        ],
    }

    REPORT_JSON.parent.mkdir(parents=True, exist_ok=True)
    REPORT_JSON.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')

    lines = [
        '# Spring Ambiguous Constructor Guard',
        '',
        f'- Base analisada: `{SRC}`',
        f'- Arquivos com estereotipo Spring escaneados: **{stereotype_files_scanned}**',
        f'- Classes com ambiguidade de construtor: **{len(findings)}**',
        '',
        '## Achados',
        '',
    ]
    if findings:
        for entry in findings:
            ctor_lines = ', '.join(str(c['line']) for c in entry['constructors'])
            lines.append(f"- `{entry['file']}` — `{entry['className']}` ({entry['constructorCount']} construtores nas linhas {ctor_lines})")
    else:
        lines.append('- Nenhuma classe com ambiguidade de construtor detectada.')
    lines.extend(['', '## Acoes recomendadas', ''])
    for action in report['recommendedActions']:
        lines.append(f'- {action}')
    REPORT_MD.write_text('\n'.join(lines) + '\n', encoding='utf-8')

    if findings:
        raise SystemExit(1)


if __name__ == '__main__':
    main()

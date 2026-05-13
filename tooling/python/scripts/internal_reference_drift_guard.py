from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import json
import re

ROOTS = [
    Path('pjb-api/src/main/java'),
    Path('pjb-api/src/test/java'),
    Path('pjb-core/src/main/java'),
    Path('pjb-core/src/test/java'),
]
SCAN_ROOTS = [Path('pjb-api/src/main/java'), Path('pjb-api/src/test/java')]
REPORT_JSON = Path('docs/reports/internal_reference_drift_guard.json')
REPORT_MD = Path('docs/reports/internal_reference_drift_guard.md')
INTERNAL_PREFIX = 'com.tcc.pjb.backend'
PACKAGE_RE = re.compile(r'^\s*package\s+([a-zA-Z0-9_.]+)\s*;', re.M)
TYPE_RE = re.compile(r'\b(record|enum|class|interface)\s+([A-Z][A-Za-z0-9_]*)\b')
IMPORT_RE = re.compile(r'^\s*import\s+(com\.tcc\.pjb\.backend[\w.]+);\s*$', re.M)
REFERENCE_RE = re.compile(r'com\.tcc\.pjb\.backend(?:\.[A-Za-z_][A-Za-z0-9_]*)+')
STRING_RE = re.compile(r'"(?:\\.|[^"\\])*"', re.S)
BLOCK_COMMENT_RE = re.compile(r'/\*.*?\*/', re.S)
LINE_COMMENT_RE = re.compile(r'//.*')


@dataclass(frozen=True)
class DriftOccurrence:
    file: str
    line: int
    reference: str
    kind: str


def build_known_types() -> set[str]:
    known: set[str] = set()
    for root in ROOTS:
        if not root.exists():
            continue
        for path in root.rglob('*.java'):
            text = path.read_text(encoding='utf-8', errors='ignore')
            package_match = PACKAGE_RE.search(text)
            if not package_match:
                continue
            package_name = package_match.group(1)
            type_names = [name for _, name in TYPE_RE.findall(text)]
            if not type_names:
                continue
            top_level = type_names[0]
            known.add(f'{package_name}.{top_level}')
            for nested in type_names[1:]:
                known.add(f'{package_name}.{top_level}.{nested}')
    return known


def strip_comments_and_strings(text: str) -> str:
    without_block_comments = BLOCK_COMMENT_RE.sub('', text)
    without_line_comments = '\n'.join(LINE_COMMENT_RE.sub('', line) for line in without_block_comments.splitlines())
    return STRING_RE.sub('""', without_line_comments)


def line_number_for(text: str, index: int) -> int:
    return text.count('\n', 0, index) + 1


def scan(known_types: set[str]) -> list[DriftOccurrence]:
    occurrences: list[DriftOccurrence] = []
    for root in SCAN_ROOTS:
        if not root.exists():
            continue
        for path in root.rglob('*.java'):
            raw_text = path.read_text(encoding='utf-8', errors='ignore')
            scrubbed = strip_comments_and_strings(raw_text)
            for match in IMPORT_RE.finditer(scrubbed):
                reference = match.group(1)
                if reference not in known_types:
                    occurrences.append(DriftOccurrence(str(path), line_number_for(scrubbed, match.start()), reference, 'import'))
            for match in REFERENCE_RE.finditer(scrubbed):
                reference = match.group(0)
                final_segment = reference.rsplit('.', 1)[-1]
                if not final_segment or not final_segment[0].isupper() or final_segment.isupper():
                    continue
                if reference not in known_types:
                    occurrences.append(DriftOccurrence(str(path), line_number_for(scrubbed, match.start()), reference, 'reference'))
    # deduplicate deterministicly
    unique = {(item.file, item.line, item.reference, item.kind): item for item in occurrences}
    return sorted(unique.values(), key=lambda item: (item.file, item.line, item.kind, item.reference))


def write_reports(known_types: set[str], occurrences: list[DriftOccurrence]) -> None:
    summary = {
        'scanRoots': [str(root) for root in SCAN_ROOTS],
        'knownInternalTypes': len(known_types),
        'filesScanned': sum(1 for root in SCAN_ROOTS if root.exists() for _ in root.rglob('*.java')),
        'unresolvedOccurrences': len(occurrences),
        'unresolvedImports': sum(1 for item in occurrences if item.kind == 'import'),
        'unresolvedReferences': sum(1 for item in occurrences if item.kind == 'reference'),
    }
    payload = {
        'summary': summary,
        'occurrences': [item.__dict__ for item in occurrences],
    }
    REPORT_JSON.parent.mkdir(parents=True, exist_ok=True)
    REPORT_JSON.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding='utf-8')

    md_lines = [
        '# Internal Reference Drift Guard',
        '',
        f"- Tipos internos conhecidos: **{summary['knownInternalTypes']}**",
        f"- Arquivos varridos: **{summary['filesScanned']}**",
        f"- Ocorrências não resolvidas: **{summary['unresolvedOccurrences']}**",
        f"- Imports não resolvidos: **{summary['unresolvedImports']}**",
        f"- Referências não resolvidas: **{summary['unresolvedReferences']}**",
        '',
        '## Ocorrências',
        '',
    ]
    if not occurrences:
        md_lines.append('- Nenhuma referência interna fora de strings/comentários ficou não resolvida.')
    else:
        for item in occurrences[:200]:
            md_lines.append(f"- `{item.file}:L{item.line}` [{item.kind}] `{item.reference}`")
    REPORT_MD.write_text('\n'.join(md_lines) + '\n', encoding='utf-8')


def main() -> int:
    known_types = build_known_types()
    occurrences = scan(known_types)
    write_reports(known_types, occurrences)
    if occurrences:
        for item in occurrences[:50]:
            print(f'{item.file}:{item.line} [{item.kind}] {item.reference}')
        return 1
    print('OK - no unresolved internal references outside strings/comments')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
